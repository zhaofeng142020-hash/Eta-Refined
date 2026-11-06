/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * DSH 远程协议常量与编解码（Control 帧 v1 / RPC 信封 / DSHF 分片 / URL 规范化）。
 * 协议事实参考 dsh-Go packages/dsh_client 与 packages/dsh_remote 的公开实现，Kotlin 原创，不复制源码。
 */
package io.github.mangi.eta.agent.remote

import org.json.JSONObject
import java.util.Base64
import java.util.Locale

object DshProtocol {
    const val PROTOCOL_VERSION = 1
    const val MAX_CONTROL_FRAME_BYTES = 64 * 1024
    const val MAX_RELAY_FRAME_BYTES = 1024 * 1024
    const val MAX_CAPABILITY_ENTRIES = 128
    const val DSHF_THRESHOLD_BYTES = 48 * 1024
    const val DSHF_MAX_BYTES = 4 * 1024 * 1024
    const val NOISE_PROTOCOL = "Noise_IK_25519_ChaChaPoly_SHA256"
    const val HARNESS_REMOTE_V3 = "harness.remote.v3"
    const val REMOTE_RPC_DESCRIBE = "harness.transport.describe"
    const val REMOTE_RPC_CALL = "harness.remote.call"
    const val REMOTE_RPC_STREAM_OPEN = "harness.remote.stream.open"
    const val REMOTE_RPC_STREAM_CLOSE = "harness.remote.stream.close"

    /** Control 帧白名单；未知类型必须 fail-closed。本轮 relay-only 不含 signal.* / transport.selected。 */
    val CONTROL_FRAME_TYPES = setOf(
        "hello", "hello.ack", "connect.request", "connect.incoming",
        "connect.accepted", "connect.rejected", "secure.handshake", "relay",
        "ping", "pong", "error",
    )
    private val ENVELOPE_FIELDS = setOf("v", "id", "type", "timestamp", "payload")

    /** 远程隧道只允许固定端点集合，避免把本机专用 API 发到隧道上。 */
    val RELAY_ALLOWED_ENDPOINTS = setOf(
        "session/list", "session/create", "session/prompt", "session/cancel",
        "session/page", "session/follow", "session/projections", "session/control",
        "session/modelCatalog", "session/selectModel", "session/rename",
        "session/updateQueue", "\$events", "\$events/result",
        REMOTE_RPC_DESCRIBE, REMOTE_RPC_CALL, REMOTE_RPC_STREAM_OPEN, REMOTE_RPC_STREAM_CLOSE,
    )

    /** 把用户输入规范化为不含尾斜杠的 origin；公网强制 HTTPS，私网/回环允许 HTTP。 */
    fun normalizeServerUrl(input: String): String {
        var text = input.trim().trimEnd('/')
        require(text.isNotEmpty()) { "服务器地址不能为空" }
        if (!Regex("^[a-z][a-z\\d+.-]*://", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            text = "https://$text"
        }
        val uri = java.net.URI(text)
        require(uri.userInfo.isNullOrEmpty() && uri.query.isNullOrEmpty() && uri.fragment.isNullOrEmpty()) {
            "服务器地址必须是不含路径、查询与账号信息的 origin"
        }
        require(uri.path.isNullOrEmpty() || uri.path == "/") { "服务器地址必须是不含路径的 origin" }
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        require(scheme == "https" || (scheme == "http" && isPrivateHost(uri.host.orEmpty()))) {
            "公网服务器必须使用 HTTPS（HTTP 仅允许局域网/回环地址）"
        }
        val port = if (uri.port > 0) ":${uri.port}" else ""
        return "$scheme://${uri.host}$port"
    }

    fun controlSocketUrl(server: String): String {
        val uri = java.net.URI(server)
        val scheme = if (uri.scheme == "https") "wss" else "ws"
        val port = if (uri.port > 0) ":${uri.port}" else ""
        return "$scheme://${uri.host}$port/ws/v1/connect"
    }

    /** 与 dsh-Go server_api.dart 的 _isPrivateHost 一致：私网/回环/.local/.test/.localhost 允许 HTTP。 */
    fun isPrivateHost(host: String): Boolean {
        if (host == "localhost" || host == "127.0.0.1" || host == "::1" || host == "[::1]" || host == "10.0.2.2") return true
        if (host.endsWith(".local") || host.endsWith(".test") || host.endsWith(".localhost")) return true
        val parts = host.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        val (a, b) = parts
        return a == 10 || a == 127 ||
            (a == 172 && b in 16..31) ||
            (a == 192 && b == 168) ||
            (a == 169 && b == 254) ||
            (a == 100 && b in 64..127)
    }

    /** 从 `dsh web: http://…/?token=…` 输出行解析启动 URL。 */
    fun parseLaunchUrl(line: String): Pair<String, String>? {
        val match = Regex("https?://\\S+").find(line) ?: return null
        val uri = runCatching { java.net.URI(match.value) }.getOrNull() ?: return null
        val token = (uri.query ?: "").split("&")
            .firstOrNull { it.startsWith("token=") }
            ?.substringAfter("token=")
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        val port = if (uri.port > 0) ":${uri.port}" else ""
        val base = "${uri.scheme}://${uri.host}$port"
        return base to token
    }

    /** Cookie 名与 authority 绑定：`dsh-auth-<base64url(sha256(authority))>`。 */
    fun cookieName(authority: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(authority.toByteArray(Charsets.UTF_8))
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(digest)
        return "dsh-auth-$encoded"
    }

    /** 解析 Control 帧；任何不符合 v1 契约的输入一律抛 INVALID_MESSAGE（fail-closed）。 */
    fun parseControlFrame(text: String): ControlFrame {
        val bytes = text.toByteArray(Charsets.UTF_8).size
        if (!(bytes <= MAX_RELAY_FRAME_BYTES)) throw RemoteException(RemoteErrorCodes.INVALID_MESSAGE, "Control 帧超过 1 MiB 上限")
        val map = runCatching { JSONObject(text) }.getOrElse {
            throw RemoteException(RemoteErrorCodes.INVALID_MESSAGE, "Control 帧不是合法 JSON")
        }
        for (key in map.keys()) {
            if (!(key in ENVELOPE_FIELDS)) throw RemoteException(RemoteErrorCodes.INVALID_MESSAGE, "Control 帧含未知封套字段 $key")
        }
        if (!(map.optInt("v", -1) == PROTOCOL_VERSION)) throw RemoteException(RemoteErrorCodes.INVALID_MESSAGE, "不支持的 Control 协议版本")
        val id = map.optString("id")
        if (id.isEmpty()) throw RemoteException(RemoteErrorCodes.INVALID_MESSAGE, "Control 帧缺少 id")
        val type = map.optString("type")
        if (!(type in CONTROL_FRAME_TYPES)) throw RemoteException(RemoteErrorCodes.INVALID_MESSAGE, "未知 Control 帧类型 $type")
        if (!(map.optLong("timestamp", -1L) > 0)) throw RemoteException(RemoteErrorCodes.INVALID_MESSAGE, "Control 帧 timestamp 非法")
        val payload = map.optJSONObject("payload")
            ?: throw RemoteException(RemoteErrorCodes.INVALID_MESSAGE, "Control 帧缺少 payload")
        val limit = if (type == "relay") MAX_RELAY_FRAME_BYTES else MAX_CONTROL_FRAME_BYTES
        if (!(bytes <= limit)) throw RemoteException(RemoteErrorCodes.INVALID_MESSAGE, "Control 帧超过 $type 的大小上限")
        return ControlFrame(id, type, map.optLong("timestamp"), payload)
    }

    fun encodeControlFrame(type: String, payload: JSONObject): String {
        if (!(type in CONTROL_FRAME_TYPES)) throw RemoteException(RemoteErrorCodes.INVALID_MESSAGE, "未知 Control 帧类型 $type")
        val frame = JSONObject()
            .put("v", PROTOCOL_VERSION)
            .put("id", java.util.UUID.randomUUID().toString())
            .put("type", type)
            .put("timestamp", System.currentTimeMillis())
            .put("payload", payload)
        val text = frame.toString()
        // 编码后同样自校验，保证不会发出不合规帧。
        parseControlFrame(text)
        return text
    }

    /** RPC 响应信封：{ok:true,value:…} 或 {ok:false,error:{code,message,retryable}}。 */
    fun parseRpcResponse(text: String): JSONObject {
        val json = runCatching { JSONObject(text) }.getOrElse {
            throw RemoteException(RemoteErrorCodes.INVALID_MESSAGE, "RPC 响应不是合法 JSON")
        }
        if (json.optBoolean("ok", false)) {
            return json.optJSONObject("value") ?: JSONObject()
        }
        val error = json.optJSONObject("error")
        val code = error?.optString("code")?.takeIf { it.isNotEmpty() } ?: RemoteErrorCodes.CONNECTION_FAILED
        val message = error?.optString("message")?.takeIf { it.isNotEmpty() } ?: remoteErrorHint(code)
        throw RemoteException(code, message, error?.optBoolean("retryable", false) ?: false)
    }

    fun rpcRequest(endpoint: String, args: Map<String, Any?>): JSONObject {
        val root = JSONObject()
        args.forEach { (key, value) -> root.put(key, value ?: JSONObject.NULL) }
        return JSONObject().put("_request", root).put("rpcId", java.util.UUID.randomUUID().toString())
    }
}

data class ControlFrame(
    val id: String,
    val type: String,
    val timestamp: Long,
    val payload: JSONObject,
)
