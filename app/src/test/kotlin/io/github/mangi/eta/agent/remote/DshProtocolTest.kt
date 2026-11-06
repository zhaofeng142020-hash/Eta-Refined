/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 远程协议单元测试：帧解析 fail-closed、大小上限、URL 规范化、Cookie 绑定、RPC 信封、错误码映射。
 * 不依赖网络与真机（Robolectric 提供 android.util.Base64）。
 */
package io.github.mangi.eta.agent.remote

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DshProtocolTest {

    @Test
    fun parseValidControlFrame() {
        val text = DshProtocol.encodeControlFrame("ping", JSONObject())
        val frame = DshProtocol.parseControlFrame(text)
        assertEquals(1, DshProtocol.PROTOCOL_VERSION)
        assertEquals("ping", frame.type)
        assertTrue(frame.id.isNotEmpty())
        assertTrue(frame.timestamp > 0)
        assertNotNull(frame.payload)
    }

    @Test
    fun unknownFrameTypeFailsClosed() {
        val frame = JSONObject()
            .put("v", 1).put("id", "x").put("type", "unknown.type")
            .put("timestamp", 1).put("payload", JSONObject())
        assertInvalid { DshProtocol.parseControlFrame(frame.toString()) }
    }

    @Test
    fun unknownEnvelopeFieldFailsClosed() {
        val frame = JSONObject()
            .put("v", 1).put("id", "x").put("type", "ping")
            .put("timestamp", 1).put("payload", JSONObject())
            .put("extra", "leak")
        assertInvalid { DshProtocol.parseControlFrame(frame.toString()) }
    }

    @Test
    fun wrongVersionFailsClosed() {
        val frame = JSONObject()
            .put("v", 2).put("id", "x").put("type", "ping")
            .put("timestamp", 1).put("payload", JSONObject())
        assertInvalid { DshProtocol.parseControlFrame(frame.toString()) }
    }

    @Test
    fun oversizedFrameFailsClosed() {
        val payload = JSONObject().put("blob", "x".repeat(DshProtocol.MAX_CONTROL_FRAME_BYTES))
        assertInvalid { DshProtocol.parseControlFrame(
            JSONObject().put("v", 1).put("id", "x").put("type", "ping")
                .put("timestamp", 1).put("payload", payload).toString(),
        ) }
    }

    @Test
    fun normalizeServerUrlRequiresHttpsForPublicHost() {
        assertEquals("https://example.com", DshProtocol.normalizeServerUrl("example.com/"))
        // 公网显式写 http 会被拒绝（不静默升级为 https，避免误导用户以为已启用 TLS）
        try {
            DshProtocol.normalizeServerUrl("http://example.com:8443")
            fail("公网 HTTP 应被拒绝")
        } catch (_: IllegalArgumentException) { }
        try {
            DshProtocol.normalizeServerUrl("http://public.example.com")
            fail("公网 HTTP 应被拒绝")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun normalizeServerUrlAllowsHttpForPrivateHosts() {
        assertEquals("http://192.168.1.5:3080", DshProtocol.normalizeServerUrl("http://192.168.1.5:3080"))
        assertEquals("http://10.0.2.2:3080", DshProtocol.normalizeServerUrl("http://10.0.2.2:3080"))
        // 无 scheme 时默认补 https（对回环地址同样有效）
        assertEquals("https://localhost:3080", DshProtocol.normalizeServerUrl("localhost:3080"))
    }

    @Test
    fun controlSocketUrlUsesWss() {
        assertEquals("wss://dsh.r2049.cn/ws/v1/connect", DshProtocol.controlSocketUrl("https://dsh.r2049.cn"))
        assertEquals("ws://localhost:8080/ws/v1/connect", DshProtocol.controlSocketUrl("http://localhost:8080"))
    }

    @Test
    fun parseLaunchUrlExtractsToken() {
        val line = "dsh web: http://127.0.0.1:3080/?token=abc123"
        val result = DshProtocol.parseLaunchUrl(line)
        assertNotNull(result)
        assertEquals("http://127.0.0.1:3080", result!!.first)
        assertEquals("abc123", result.second)
    }

    @Test
    fun cookieNameIsAuthorityBound() {
        val a = DshProtocol.cookieName("127.0.0.1:3080")
        val b = DshProtocol.cookieName("127.0.0.1:3081")
        assertTrue(a.startsWith("dsh-auth-"))
        assertTrue(a != b)
    }

    @Test
    fun rpcEnvelopeParsesValueAndError() {
        val ok = DshProtocol.parseRpcResponse("{\"ok\":true,\"value\":{\"sessions\":[]}}")
        assertNotNull(ok.optJSONArray("sessions"))
        try {
            DshProtocol.parseRpcResponse(
                "{\"ok\":false,\"error\":{\"code\":\"HOST_OFFLINE\",\"message\":\"offline\",\"retryable\":true}}",
            )
            fail("错误信封应抛出 RemoteException")
        } catch (error: RemoteException) {
            assertEquals(RemoteErrorCodes.HOST_OFFLINE, error.code)
            assertTrue(error.retryable)
        }
    }

    @Test
    fun relayAllowlistRejectsPrivateBridgeEndpoints() {
        assertTrue("session/list" in DshProtocol.RELAY_ALLOWED_ENDPOINTS)
        assertTrue("dsh-go/v1/inventory" !in DshProtocol.RELAY_ALLOWED_ENDPOINTS)
    }

    @Test
    fun errorHintsAreActionable() {
        assertTrue(remoteErrorHint(RemoteErrorCodes.PEER_IDENTITY_MISMATCH).contains("指纹"))
        assertTrue(remoteErrorHint(RemoteErrorCodes.HARNESS_VERSION_INCOMPATIBLE).contains("升级"))
        assertTrue(remoteErrorHint("UNKNOWN_CODE").isNotEmpty())
    }

    private fun assertInvalid(block: () -> Unit) {
        try {
            block()
            fail("应抛出 RemoteException/IllegalArgumentException")
        } catch (error: RemoteException) {
            assertEquals(RemoteErrorCodes.INVALID_MESSAGE, error.code)
        } catch (_: IllegalArgumentException) { }
    }
}
