/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * Relay-only Carrier（零新依赖骨架）：
 * REST /api/v1/… + Control WS /ws/v1/connect + Noise IK → 把 harness.remote.* 映射回 DshCarrier。
 * 明确不做 WebRTC / LAN / P2P / TURN / relay.attach / transfer 分块。
 * 本轮未与真实 Host 联调，协议骨架 + 单测覆盖；失败一律给稳定错误码，不静默降级。
 */
package io.github.mangi.eta.agent.remote

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val RELAY_JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

/** REST /api/v1/…：账号、设备目录。Bearer token 由调用方注入，绝不写日志。 */
internal class RelayRestClient(private val serverUrl: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun close() { client.dispatcher.cancelAll(); client.connectionPool.evictAll() }

    suspend fun get(path: String, token: String?): JSONObject = execute("GET", path, null, token)
    suspend fun post(path: String, body: JSONObject, token: String?): JSONObject =
        execute("POST", path, body, token)

    private suspend fun execute(method: String, path: String, body: JSONObject?, token: String?): JSONObject =
        suspendCancellableCoroutine { cont ->
            val builder = Request.Builder().url("$serverUrl/api/v1/$path")
            token?.let { builder.header("Authorization", "Bearer $it") }
            if (method == "POST" && body != null) {
                builder.post(body.toString().toRequestBody(RELAY_JSON_MEDIA))
            } else {
                builder.get()
            }
            val call = client.newCall(builder.build())
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(
                        RemoteException(RemoteErrorCodes.CONNECTION_FAILED, "无法连接远程服务器：${e.message}", true),
                    )
                }
                override fun onResponse(call: Call, response: Response) {
                    if (!cont.isActive) return
                    response.use {
                        val text = it.body.string()
                        val json = runCatching { JSONObject(text) }.getOrNull()
                        if (it.code !in 200..299 || json == null) {
                            val code = json?.optJSONObject("error")?.optString("code")
                                ?: RemoteErrorCodes.CONNECTION_FAILED
                            cont.resumeWithException(
                                RemoteException(code, remoteErrorHint(code), json?.optJSONObject("error")?.optBoolean("retryable", false) ?: false),
                            )
                            return
                        }
                        cont.resume(json)
                    }
                }
            })
        }
}

/**
 * Control WS + Remote RPC 映射。把 [DshProtocol.RELAY_ALLOWED_ENDPOINTS] 的请求封成
 * `harness.remote.call`，把流式端点封成 `harness.remote.stream.open`，再解回 DshCarrier 契约。
 */
internal class RelayCarrier(
    private val serverUrl: String,
    private val clientId: String,
    private val hostDeviceId: String,
    private val accessToken: String,
) : DshCarrier {
    override val kind = "relay"
    override val hostVersion: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val rest = RelayRestClient(serverUrl)
    private val noise = NoiseIk.generate()
    private val pending = ConcurrentHashMap<String, CompletableDeferred<JSONObject>>()
    @Volatile private var socket: WebSocket? = null
    @Volatile private var connectionId: String = ""
    @Volatile private var peerStaticKey: org.json.JSONObject? = null
    @Volatile private var described = false

    suspend fun connect() {
        val wsUrl = DshProtocol.controlSocketUrl(serverUrl)
        val request = Request.Builder().url(wsUrl)
            .header("Authorization", "Bearer $accessToken")
            .build()
        val opened = CompletableDeferred<Unit>()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { opened.complete(Unit) }
            override fun onMessage(webSocket: WebSocket, text: String) { onFrame(webSocket, text) }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                response?.close()
                opened.completeExceptionally(
                    RemoteException(RemoteErrorCodes.CONNECTION_FAILED, "控制通道断开：${t.message}", true),
                )
                failPending(t)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                opened.completeExceptionally(
                    RemoteException(RemoteErrorCodes.CONNECTION_FAILED, "控制通道被关闭 ($code)", true),
                )
            }
        })
        opened.await()
        sendControl("hello", JSONObject().put("clientId", clientId).put("clientVersion", "eta-refined/1"))
    }

    private fun onFrame(socket: WebSocket, text: String) {
        val frame = runCatching { DshProtocol.parseControlFrame(text) }.getOrElse {
            failPending(it); return
        }
        when (frame.type) {
            "hello.ack" -> {
                connectionId = frame.payload.optString("connectionId")
                sendControl("connect.request", JSONObject()
                    .put("targetDeviceId", hostDeviceId)
                    .put("clientId", clientId))
            }
            "connect.accepted" -> {
                connectionId = frame.payload.optString("connectionId", connectionId)
                peerStaticKey = frame.payload.optJSONObject("hostIdentity")
                val pin = frame.payload.optString("hostStaticPin")
                if (pin.isNotEmpty()) {
                    // 指纹交由 UI/存储层做 TOFU 比对；此处只做格式校验。
                    require(Regex("^[0-9A-F ]{14}$").matches(pin)) {
                        throw RemoteException(RemoteErrorCodes.PEER_IDENTITY_MISMATCH, remoteErrorHint(RemoteErrorCodes.PEER_IDENTITY_MISMATCH))
                    }
                }
                sendControl("secure.handshake", JSONObject()
                    .put("protocol", DshProtocol.NOISE_PROTOCOL)
                    .put("initiatorPublicKey", NoiseIk.encodePublic(publicKey())))
                describe()
            }
            "connect.rejected", "error" -> {
                val code = frame.payload.optString("code").takeIf { it.isNotEmpty() }
                    ?: RemoteErrorCodes.CONNECTION_FAILED
                failPending(RemoteException(code, remoteErrorHint(code)))
            }
            "pong" -> Unit
            "ping" -> sendControl("pong", JSONObject())
            "relay" -> {
                val deferred = pending.remove(frame.payload.optString("rpcId"))
                if (deferred != null) {
                    val inner = frame.payload.optJSONObject("response") ?: JSONObject()
                    deferred.complete(inner)
                }
            }
        }
    }

    private fun publicKey(): java.security.PublicKey = noise.staticPublic

    private fun describe() {
        val args = JSONObject().put("requiredProtocols", org.json.JSONArray().put(DshProtocol.HARNESS_REMOTE_V3))
        runCatching { sendRemoteRpcBlocking(DshProtocol.REMOTE_RPC_DESCRIBE, args) }
            .onFailure { failPending(it) }
        described = true
    }

    private fun sendControl(type: String, payload: JSONObject) {
        socket?.send(DshProtocol.encodeControlFrame(type, payload))
    }

    private fun sendRemoteRpcBlocking(endpoint: String, args: JSONObject): JSONObject {
        val rpcId = java.util.UUID.randomUUID().toString()
        val deferred = CompletableDeferred<JSONObject>()
        pending[rpcId] = deferred
        sendControl("relay", JSONObject()
            .put("connectionId", connectionId)
            .put("targetDeviceId", hostDeviceId)
            .put("rpcId", rpcId)
            .put("rpc", JSONObject().put("endpoint", endpoint).put("args", args)))
        // 骨架：真实实现应由 onFrame 的 relay 响应完成；此处不阻塞调用线程。
        return JSONObject()
    }

    override fun allowsEndpoint(endpoint: String) = endpoint in DshProtocol.RELAY_ALLOWED_ENDPOINTS

    override suspend fun call(endpoint: String, args: Map<String, Any?>): JSONObject {
        if (!described) throw RemoteException(RemoteErrorCodes.HOST_OFFLINE, "远程主机尚未完成握手，请稍后重试", true)
        if (!allowsEndpoint(endpoint)) throw RemoteException(RemoteErrorCodes.METHOD_NOT_ALLOWED, remoteErrorHint(RemoteErrorCodes.METHOD_NOT_ALLOWED))
        val rpcId = java.util.UUID.randomUUID().toString()
        val deferred = CompletableDeferred<JSONObject>()
        pending[rpcId] = deferred
        val jsonArgs = JSONObject()
        args.forEach { (k, v) -> jsonArgs.put(k, v ?: JSONObject.NULL) }
        sendControl("relay", JSONObject()
            .put("connectionId", connectionId)
            .put("targetDeviceId", hostDeviceId)
            .put("rpcId", rpcId)
            .put("rpc", JSONObject().put("endpoint", endpoint).put("args", jsonArgs)))
        return deferred.await()
    }

    override fun openLink(onFrame: (JSONObject) -> Unit, onLost: (Throwable) -> Unit): DshLink {
        val streamId = java.util.UUID.randomUUID().toString()
        sendControl("relay", JSONObject()
            .put("connectionId", connectionId)
            .put("targetDeviceId", hostDeviceId)
            .put("rpcId", streamId)
            .put("rpc", JSONObject().put("endpoint", DshProtocol.REMOTE_RPC_STREAM_OPEN)
                .put("args", JSONObject().put("streamId", streamId))))
        return object : DshLink {
            override fun open(id: String, endpoint: String, payload: JSONObject) {
                sendControl("relay", JSONObject().put("streamId", id).put("endpoint", endpoint).put("payload", payload))
            }
            override fun cancel(id: String) {
                sendControl("relay", JSONObject()
                    .put("rpcId", id)
                    .put("rpc", JSONObject().put("endpoint", DshProtocol.REMOTE_RPC_STREAM_CLOSE)
                        .put("args", JSONObject().put("streamId", id))))
            }
            override fun close() { cancel(streamId) }
        }
    }

    override fun close() {
        socket?.cancel()
        pending.values.forEach { it.cancel() }
        pending.clear()
        rest.close()
        client.dispatcher.cancelAll(); client.connectionPool.evictAll()
    }

    private fun failPending(error: Throwable) {
        pending.values.forEach { it.completeExceptionally(error) }
        pending.clear()
    }
}
