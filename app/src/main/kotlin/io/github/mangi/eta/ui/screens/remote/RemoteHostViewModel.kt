/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 远程主机 UI 状态桥接：只调用 agent.remote 契约 A 暴露的 API，不持有协议层实现细节。
 * 凭据不在本层落地或上传；launchToken 由设置页显式调用 repo.saveLaunchToken 写入 Keystore。
 */
package io.github.mangi.eta.ui.screens.remote

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.mangi.eta.agent.remote.DshClient
import io.github.mangi.eta.agent.remote.DshClientFactory
import io.github.mangi.eta.agent.remote.RemoteException
import io.github.mangi.eta.agent.remote.RemoteHostConfig
import io.github.mangi.eta.agent.remote.RemoteLinkStore
import io.github.mangi.eta.agent.remote.RemoteSessionSummary
import io.github.mangi.eta.data.remote.RemoteHostRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI 侧错误模型（非协议类型，仅用于渲染 MistErrorCard）。 */
internal data class RemoteUiError(
    val code: String,
    val message: String,
    val retryable: Boolean,
)

/**
 * 远程会话状态所有者。连接、会话列表、流式事件、模型目录都经此类桥接；
 * [RemoteLinkStore] 的全局连接状态仍由 UI 直接 collect，不在此复制。
 */
internal class RemoteHostViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val EVENT_LIMIT = 200
    }

    val repo = RemoteHostRepository(application)

    private var client: DshClient? = null
    private var followHandle: AutoCloseable? = null

    private val _sessions = MutableStateFlow(emptyList<RemoteSessionSummary>())
    val sessions: StateFlow<List<RemoteSessionSummary>> = _sessions

    private val _events = MutableStateFlow(emptyList<String>())
    val events: StateFlow<List<String>> = _events

    private val _models = MutableStateFlow(emptyList<String>())
    val models: StateFlow<List<String>> = _models

    private val _activeSessionId = MutableStateFlow("")
    val activeSessionId: StateFlow<String> = _activeSessionId

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _error = MutableStateFlow<RemoteUiError?>(null)
    val error: StateFlow<RemoteUiError?> = _error

    /** 按当前激活主机建立连接；已连接则复用，不会重复上传任何凭据。 */
    fun connect(initialSessionId: String = "") {
        if (client != null) {
            if (initialSessionId.isNotBlank()) openSession(initialSessionId)
            return
        }
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            val config = withContext(Dispatchers.IO) { resolveActiveConfig() }
            if (config == null) {
                _busy.value = false
                return@launch
            }
            runCatching {
                withContext(Dispatchers.IO) { DshClientFactory.connect(config) }
            }.onSuccess { connected ->
                client = connected
                RemoteLinkStore.refresh(config.id)
                loadSessions(connected)
                loadModels(connected)
                if (initialSessionId.isNotBlank()) openSession(initialSessionId)
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                _error.value = throwable.toUiError()
            }
            _busy.value = false
        }
    }

    private suspend fun resolveActiveConfig(): RemoteHostConfig? {
        val activeId = repo.activeHostId() ?: return null
        return repo.hosts().first().firstOrNull { it.id == activeId && it.enabled }
    }

    private suspend fun loadSessions(current: DshClient) {
        runCatching {
            withContext(Dispatchers.IO) { current.listSessions() }
        }.onSuccess { list ->
            _sessions.value = list
            _error.value = null
        }.onFailure { throwable ->
            if (throwable is CancellationException) throw throwable
            _error.value = throwable.toUiError()
        }
    }

    private suspend fun loadModels(current: DshClient) {
        // 模型目录是非关键路径，失败不覆盖主错误状态。
        runCatching {
            withContext(Dispatchers.IO) { current.modelCatalog() }
        }.onSuccess { _models.value = it }
    }

    fun refreshSessions() {
        val current = client
        if (current == null) {
            // 断开后再次刷新即重新建连，避免 UI 进入无法恢复的空状态。
            connect()
            return
        }
        viewModelScope.launch {
            _busy.value = true
            loadSessions(current)
            _busy.value = false
        }
    }

    fun createSession() {
        val current = client ?: return
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                withContext(Dispatchers.IO) { current.createSession(null) }
            }.onSuccess { summary ->
                openSession(summary.id)
                loadSessions(current)
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                _error.value = throwable.toUiError()
            }
            _busy.value = false
        }
    }

    fun openSession(sessionId: String) {
        val current = client ?: return
        runCatching { followHandle?.close() }
        followHandle = null
        _activeSessionId.value = sessionId
        _events.value = emptyList()
        followHandle = current.followEvents(sessionId) { event ->
            _events.value = (_events.value + event).takeLast(EVENT_LIMIT)
        }
    }

    fun sendPrompt(text: String) {
        val current = client ?: return
        val sessionId = _activeSessionId.value
        if (sessionId.isBlank() || text.isBlank()) return
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                withContext(Dispatchers.IO) { current.prompt(sessionId, text) }
            }.onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                _error.value = throwable.toUiError()
            }
            _busy.value = false
        }
    }

    fun cancelRun() {
        val current = client ?: return
        val sessionId = _activeSessionId.value
        if (sessionId.isBlank()) return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { current.cancel(sessionId) }
            }
        }
    }

    fun selectModel(modelId: String) {
        val current = client ?: return
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { current.selectModel(modelId) }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun disconnect() {
        runCatching { followHandle?.close() }
        followHandle = null
        runCatching { client?.close() }
        client = null
        _sessions.value = emptyList()
        _events.value = emptyList()
        _models.value = emptyList()
        _activeSessionId.value = ""
        RemoteLinkStore.disconnect()
    }

    private fun Throwable.toUiError(): RemoteUiError = when (this) {
        is RemoteException -> RemoteUiError(code, message.orEmpty(), retryable)
        else -> RemoteUiError("CONNECTION_FAILED", message.orEmpty(), true)
    }

    override fun onCleared() {
        runCatching { followHandle?.close() }
        runCatching { client?.close() }
        super.onCleared()
    }
}
