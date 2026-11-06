/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 直连 Carrier：GET /?token= 换会话 Cookie → POST /api/<endpoint> + WS /api/remote.mux。
 * 协议事实参考 dsh-Go packages/dsh_client（AGPL-3.0），Kotlin 原创实现。
 */
package io.github.mangi.eta.agent.remote

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
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

internal class DshHttpCarrier(
    private val baseUrl: String,
    private val launchToken: String,
) : DshCarrier {
    override val kind = "http"
    override val hostVersion: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    @Volatile
    private var cookieHeader: String? = null

    private val authority: String = run {
        val uri = java.net.URI(baseUrl)
        val port = if (uri.port > 0) ":${uri.port}" else ""
        "${uri.host}$port"
    }

    /** 用 launch token 换会话 Cookie；Cookie 绑定 authority，30 天内复用，401 时重新登录。 */
    suspend fun login() {
        val request = Request.Builder()
            .url("$baseUrl/?token=$launchToken")
            .header("Host", authority)
            .get()
            .build()
        val response = execute(request)
        response.use {
            if (it.code !in 200..299) {
                throw RemoteException(RemoteErrorCodes.AUTH_INVALID, "启动令牌无效或已过期，请从 dsh web 重新复制带 token 的 URL")
            }
            val setCookie = it.headers("Set-Cookie").firstOrNull()
                ?: throw RemoteException(RemoteErrorCodes.INVALID_MESSAGE, "主机未返回会话 Cookie，请确认 dsh web 版本")
            cookieHeader = setCookie.substringBefore(';').trim()
        }
    }

    override fun allowsEndpoint(endpoint: String) = true

    override suspend fun call(endpoint: String, args: Map<String, Any?>): JSONObject {
        ensureLoggedIn()
        val body = DshProtocol.rpcRequest(endpoint, args).toString().toRequestBody(JSON_MEDIA)
        val request = Request.Builder()
            .url("$baseUrl/api/$endpoint")
            .header("Host", authority)
            .header("Cookie", cookieHeader.orEmpty())
            .post(body)
            .build()
        val response = execute(request)
        return response.use {
            if (it.code == 401) {
                cookieHeader = null
                throw RemoteException(RemoteErrorCodes.TOKEN_EXPIRED, "会话已过期，请重新连接远程主机")
            }
            if (it.code !in 200..299) {
                throw RemoteException(RemoteErrorCodes.CONNECTION_FAILED, "主机返回 HTTP ${it.code}")
            }
            val text = it.body.string()
            DshProtocol.parseRpcResponse(text)
        }
    }

    override fun openLink(onFrame: (JSONObject) -> Unit, onLost: (Throwable) -> Unit): DshLink {
        val request = Request.Builder()
            .url("$baseUrl/api/remote.mux")
            .header("Host", authority)
            .header("Cookie", cookieHeader.orEmpty())
            .build()
        val socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val frame = runCatching { JSONObject(text) }.getOrNull() ?: return
                onFrame(frame)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                response?.close()
                onLost(t)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onLost(IOException("mux closed: $code $reason"))
            }
        })
        return object : DshLink {
            override fun open(streamId: String, endpoint: String, payload: JSONObject) {
                val frame = JSONObject()
                    .put("type", "open")
                    .put("streamId", streamId)
                    .put("endpoint", endpoint)
                    .put("payload", payload)
                socket.send(frame.toString())
            }
            override fun cancel(streamId: String) {
                socket.send(JSONObject().put("type", "cancel").put("streamId", streamId).toString())
            }
            override fun close() { socket.cancel() }
        }
    }

    override fun close() { client.dispatcher.cancelAll(); client.connectionPool.evictAll() }

    private suspend fun ensureLoggedIn() {
        if (cookieHeader == null) login()
    }

    private suspend fun execute(request: Request): Response = suspendCancellableCoroutine { cont ->
        val call = client.newCall(request)
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(
                    RemoteException(RemoteErrorCodes.CONNECTION_FAILED, "无法连接远程主机：${e.message}", retryable = true),
                )
            }
            override fun onResponse(call: Call, response: Response) {
                if (cont.isActive) cont.resume(response)
            }
        })
    }
}
