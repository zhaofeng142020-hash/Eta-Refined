/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 远程会话门面 + 连接状态存储 + 工厂。UI 只依赖本文件与 RemoteModels 的公开 API。
 */
package io.github.mangi.eta.agent.remote

import io.github.mangi.eta.data.remote.RemoteHostRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

/** 远程会话 API：两种 Carrier 共用同一套调用。 */
class DshClient internal constructor(private val carrier: DshCarrier) {

    suspend fun listSessions(): List<RemoteSessionSummary> {
        val value = carrier.call("session/list", emptyMap())
        val array = value.optJSONArray("sessions") ?: value.optJSONArray("items")
            ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            RemoteSessionSummary(
                id = o.optString("id"),
                title = o.optString("title"),
                updatedAt = o.optLong("updatedAt", 0L),
            )
        }.filter { it.id.isNotEmpty() }
    }

    suspend fun createSession(title: String? = null): RemoteSessionSummary {
        val args = if (title == null) emptyMap() else mapOf("title" to title)
        val value = carrier.call("session/create", args)
        return RemoteSessionSummary(
            id = value.optString("id"),
            title = value.optString("title", title.orEmpty()),
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun prompt(sessionId: String, text: String, images: List<ByteArray> = emptyList()) {
        val args = mutableMapOf<String, Any?>("sessionId" to sessionId, "text" to text)
        // 图片以 base64 传入（与 dsh-Go PromptImage 的 mediaType/data 结构对齐）；本轮仅占位传递。
        if (images.isNotEmpty()) {
            args["images"] = images.map { JSONObject().put("type", "image").put("mediaType", "image/png").put("data", it) }
        }
        carrier.call("session/prompt", args)
    }

    suspend fun cancel(sessionId: String) {
        carrier.call("session/cancel", mapOf("sessionId" to sessionId))
    }

    suspend fun modelCatalog(): List<String> {
        val value = carrier.call("session/modelCatalog", emptyMap())
        val array = value.optJSONArray("models") ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optString(it, null) }
    }

    suspend fun selectModel(modelId: String) {
        carrier.call("session/selectModel", mapOf("modelId" to modelId))
    }

    /** 订阅会话事件流；返回 AutoCloseable 便于 UI 在 dispose 时取消。 */
    fun followEvents(sessionId: String, onEvent: (String) -> Unit): AutoCloseable {
        val streamId = java.util.UUID.randomUUID().toString()
        val link = carrier.openLink(
            onFrame = { frame ->
                if (frame.optString("streamId") == streamId && frame.optString("type") == "item") {
                    onEvent(frame.optJSONObject("value")?.toString() ?: frame.optString("value"))
                }
            },
            onLost = { /* 状态由 RemoteLinkStore 统一标注 */ },
        )
        link.open(streamId, "session/follow", JSONObject().put("sessionId", sessionId))
        return AutoCloseable { link.cancel(streamId); link.close() }
    }

    fun close() = carrier.close()
}

object DshClientFactory {
    suspend fun connect(config: RemoteHostConfig): DshClient {
        val repo = RemoteHostRepository.get()
        return when (config.kind) {
            RemoteHostKind.DIRECT -> {
                val token = repo.credential(config.id, "launch_token")
                    ?: throw RemoteException(RemoteErrorCodes.AUTH_INVALID, "请先填写 dsh web 启动时打印的带 token 的 URL")
                val carrier = DshHttpCarrier(config.baseUrl, token)
                carrier.login()
                DshClient(carrier)
            }
            RemoteHostKind.RELAY -> {
                // 全局开关是一刀切：关闭后所有 RELAY 主机都不可用（避免静默连第三方）。
                val enabled = repo.relayServerEnabled().first()
                if (!enabled) {
                    throw RemoteException(RemoteErrorCodes.HOST_OFFLINE, "第三方远程服务器已在设置中关闭，请先开启")
                }
                // 以「全局 Server 地址」为准；config.serverUrl 仅作为 UI 展示/编辑时的镜像值。
                val server = DshProtocol.normalizeServerUrl(repo.relayServerUrl().first())
                val accessToken = repo.credential(config.id, "access_token")
                    ?: throw RemoteException(RemoteErrorCodes.AUTH_INVALID, "请先在设置中登录远程主机")
                val clientId = repo.credential(config.id, "client_id") ?: java.util.UUID.randomUUID().toString()
                repo.saveCredential(config.id, "client_id", clientId)
                val hostDeviceId = repo.credential(config.id, "host_device_id")
                    ?: throw RemoteException(RemoteErrorCodes.HOST_OFFLINE, "尚未选择电脑端主机，请在远程主机设置中登录后选择")
                val carrier = RelayCarrier(server, clientId, hostDeviceId, accessToken)
                carrier.connect()
                DshClient(carrier)
            }
        }
    }
}

/** 连接状态：UI 通过它渲染状态卡、触发连接与断开。 */
object RemoteLinkStore {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val _status = MutableStateFlow<RemoteLinkStatus?>(null)
    val status: StateFlow<RemoteLinkStatus?> = _status

    @Volatile
    private var client: DshClient? = null

    fun activeClient(): DshClient? = client

    fun refresh(hostId: String) {
        scope.launch {
            _status.value = RemoteLinkStatus(hostId, RemoteLinkState.CONNECTING, detail = "正在连接远程主机…")
            try {
                val config = RemoteHostRepository.get().host(hostId)
                    ?: throw RemoteException(RemoteErrorCodes.HOST_OFFLINE, "远程主机配置不存在")
                client?.close()
                client = DshClientFactory.connect(config)
                _status.value = RemoteLinkStatus(
                    hostId, RemoteLinkState.CONNECTED,
                    transport = if (config.kind == RemoteHostKind.DIRECT) "http" else "relay",
                    detail = "已连接",
                )
            } catch (error: RemoteException) {
                _status.value = RemoteLinkStatus(
                    hostId, RemoteLinkState.ERROR,
                    detail = remoteErrorHint(error.code),
                    retryable = error.retryable,
                )
            } catch (error: Exception) {
                _status.value = RemoteLinkStatus(
                    hostId, RemoteLinkState.ERROR,
                    detail = remoteErrorHint(RemoteErrorCodes.CONNECTION_FAILED),
                    retryable = true,
                )
            }
        }
    }

    fun disconnect() {
        client?.close()
        client = null
        _status.value = _status.value?.copy(state = RemoteLinkState.IDLE, detail = "已断开")
    }
}
