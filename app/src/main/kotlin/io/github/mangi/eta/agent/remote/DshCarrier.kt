/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 统一传输接缝：对齐 dsh-Go 的 DshCarrier / DshMuxLink 抽象，本机与远程共用同一套会话 API。
 */
package io.github.mangi.eta.agent.remote

import org.json.JSONObject

interface DshLink {
    fun open(streamId: String, endpoint: String, payload: JSONObject)
    fun cancel(streamId: String)
    fun close()
}

interface DshCarrier {
    val kind: String
    val hostVersion: String?
    fun allowsEndpoint(endpoint: String): Boolean
    suspend fun call(endpoint: String, args: Map<String, Any?>): JSONObject
    fun openLink(onFrame: (JSONObject) -> Unit, onLost: (Throwable) -> Unit): DshLink
    fun close()
}
