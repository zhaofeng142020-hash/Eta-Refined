/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 远程主机领域模型与错误类型。
 * 协议事实参考 dsh-Go（AGPL-3.0）与 ds-harness-remote（MIT）的公开协议，Kotlin 原创实现，不复制源码。
 */
package io.github.mangi.eta.agent.remote

enum class RemoteHostKind { DIRECT, RELAY }

data class RemoteHostConfig(
    val id: String,
    val kind: RemoteHostKind,
    val displayName: String,
    val baseUrl: String = "",
    val serverUrl: String = "",
    val enabled: Boolean = true,
)

object RemoteHostDefaults {
    const val RELAY_SERVER_URL = "https://dsh.r2049.cn"
    const val DIRECT_DEFAULT_PORT = 3080
}

enum class RemoteLinkState { IDLE, CONNECTING, CONNECTED, ERROR }

data class RemoteLinkStatus(
    val hostId: String,
    val state: RemoteLinkState,
    val transport: String = "",
    val detail: String = "",
    val retryable: Boolean = false,
    val latencyMs: Long? = null,
)

data class RemoteSessionSummary(val id: String, val title: String, val updatedAt: Long)

/** 稳定错误码：与 dsh-Go 的 RemoteApiException / HostLinkError 对齐，便于逐条翻译成中文可操作文案。 */
class RemoteException(
    val code: String,
    message: String,
    val retryable: Boolean = false,
) : Exception(message)

object RemoteErrorCodes {
    const val HOST_OFFLINE = "HOST_OFFLINE"
    const val AUTH_INVALID = "AUTH_INVALID"
    const val TOKEN_EXPIRED = "TOKEN_EXPIRED"
    const val PEER_IDENTITY_MISMATCH = "PEER_IDENTITY_MISMATCH"
    const val HARNESS_VERSION_INCOMPATIBLE = "HARNESS_VERSION_INCOMPATIBLE"
    const val CONNECTION_REPLACED = "CONNECTION_REPLACED"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val CONNECTION_FAILED = "CONNECTION_FAILED"
    const val INVALID_MESSAGE = "INVALID_MESSAGE"
    const val UNSUPPORTED_CRYPTO = "UNSUPPORTED_CRYPTO"
    const val METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED"
}

/** 把稳定错误码翻译成可操作的中文提示；UI 直接展示，不需要再解析异常文本。 */
fun remoteErrorHint(code: String): String = when (code) {
    RemoteErrorCodes.HOST_OFFLINE -> "主机不在线，请确认电脑端 dsh web 或远程插件正在运行后重试。"
    RemoteErrorCodes.AUTH_INVALID -> "登录凭据无效，请在设置中重新登录远程主机。"
    RemoteErrorCodes.TOKEN_EXPIRED -> "登录已过期，请重新登录远程主机。"
    RemoteErrorCodes.PEER_IDENTITY_MISMATCH -> "主机身份指纹与之前记录的不一致，可能为中间人攻击或主机重装；请在电脑端核对指纹后再连接。"
    RemoteErrorCodes.HARNESS_VERSION_INCOMPATIBLE -> "电脑端 DSH 版本不兼容（需要支持 harness.remote.v3），请升级 dsh 后重试。"
    RemoteErrorCodes.CONNECTION_REPLACED -> "该账号的另一台设备已连接这台主机，连接被替换；请关闭其他设备后重试。"
    RemoteErrorCodes.RATE_LIMITED -> "请求过于频繁，请稍后再试。"
    RemoteErrorCodes.CONNECTION_FAILED -> "无法连接远程主机，请检查地址、端口与网络后重试。"
    RemoteErrorCodes.INVALID_MESSAGE -> "主机返回了无法解析的数据，请确认主机版本与协议一致。"
    RemoteErrorCodes.UNSUPPORTED_CRYPTO -> "当前设备不支持远程连接所需的加密算法（X25519 / ChaCha20-Poly1305）。"
    RemoteErrorCodes.METHOD_NOT_ALLOWED -> "远程主机不支持该操作（远程隧道只开放固定的 API 集合）。"
    else -> "远程连接失败，请检查配置后重试。"
}
