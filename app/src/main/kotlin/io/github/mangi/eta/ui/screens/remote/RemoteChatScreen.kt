/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 远程会话列表 + 流式聊天页：复用现有 Miuix 列表/输入视觉语言，错误用 MistErrorCard、空态用 MistEmptyState。
 * 状态桥接统一交给 RemoteHostViewModel；连接状态仍直接 collect RemoteLinkStore.status。
 */
package io.github.mangi.eta.ui.screens.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.mangi.eta.R
import io.github.mangi.eta.agent.remote.RemoteLinkState
import io.github.mangi.eta.agent.remote.RemoteLinkStore
import io.github.mangi.eta.ui.components.MistEmptyState
import io.github.mangi.eta.ui.components.MistErrorCard
import io.github.mangi.eta.ui.components.MiuixScaffoldPage
import io.github.mangi.eta.ui.theme2.MistSpace
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * @param sessionId 初始选中的远程会话；为空表示只展示会话列表，由用户手动选择。
 */
@Composable
internal fun RemoteChatScreen(
    sessionId: String,
    onBack: () -> Unit,
) {
    val viewModel = viewModel<RemoteHostViewModel>()
    val sessions by viewModel.sessions.collectAsState()
    val events by viewModel.events.collectAsState()
    val models by viewModel.models.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val error by viewModel.error.collectAsState()
    val status by RemoteLinkStore.status.collectAsState()
    val genericError = stringResource(R.string.remote_error_generic)

    var draft by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.connect(sessionId)
    }

    MiuixScaffoldPage(
        title = stringResource(R.string.remote_chat_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = { viewModel.refreshSessions() }) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.remote_chat_refresh),
                )
            }
        },
    ) {
        // ── 连接状态与快捷操作 ──────────────────────────────────────────
        item(key = "remote_chat_status") {
            SmallTitle(stringResource(R.string.remote_link_status_title))
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                val link = status
                Column(
                    modifier = Modifier.padding(MistSpace.lg),
                    verticalArrangement = Arrangement.spacedBy(MistSpace.xs),
                ) {
                    Text(
                        text = stringResource(
                            R.string.remote_link_state_label,
                            remoteLinkStateLabel(link?.state ?: RemoteLinkState.IDLE),
                        ),
                        style = MiuixTheme.textStyles.body2,
                    )
                    if (link != null) {
                        Text(
                            text = stringResource(
                                R.string.remote_link_transport_label,
                                remoteTransportLabel(link.transport),
                            ),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        // 契约：latencyMs 未测量为 null，UI 不得显示 0。
                        link.latencyMs?.let { latency ->
                            Text(
                                text = stringResource(R.string.remote_link_latency_label, latency),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        if (link.detail.isNotBlank()) {
                            Text(
                                text = link.detail,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                    if (busy) {
                        CircularProgressIndicator(size = 20.dp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(MistSpace.sm)) {
                        TextButton(
                            text = stringResource(R.string.remote_chat_refresh),
                            onClick = { viewModel.refreshSessions() },
                        )
                        TextButton(
                            text = stringResource(R.string.remote_chat_disconnect),
                            onClick = { viewModel.disconnect() },
                        )
                    }
                }
            }
        }

        // ── ViewModel 级错误 ────────────────────────────────────────────
        val currentError = error
        if (currentError != null) {
            item(key = "remote_chat_error") {
                MistErrorCard(
                    title = stringResource(R.string.remote_error_title),
                    message = currentError.message.ifBlank { genericError },
                    fixHint = stringResource(R.string.remote_error_fix_hint),
                    onRetry = {
                        viewModel.clearError()
                        viewModel.refreshSessions()
                    },
                    retryLabel = stringResource(R.string.remote_action_retry),
                    onDismiss = { viewModel.clearError() },
                    dismissLabel = stringResource(R.string.remote_action_close),
                )
            }
        }

        // ── 远程会话列表 ────────────────────────────────────────────────
        item(key = "remote_chat_sessions") {
            SmallTitle(stringResource(R.string.remote_chat_sessions))
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                if (sessions.isEmpty()) {
                    MistEmptyState(
                        title = stringResource(R.string.remote_chat_sessions_empty),
                        summary = stringResource(R.string.remote_chat_sessions_empty_summary),
                        icon = Icons.Rounded.ChatBubble,
                        action = {
                            TextButton(
                                text = stringResource(R.string.remote_chat_new_session),
                                onClick = { viewModel.createSession() },
                            )
                        },
                    )
                } else {
                    sessions.forEach { summary ->
                        ArrowPreference(
                            title = summary.title.ifBlank { summary.id },
                            summary = if (summary.id == activeSessionId) {
                                stringResource(R.string.remote_switcher_active_tag)
                            } else {
                                null
                            },
                            onClick = { viewModel.openSession(summary.id) },
                        )
                    }
                    TextButton(
                        text = stringResource(R.string.remote_chat_new_session),
                        onClick = { viewModel.createSession() },
                        modifier = Modifier.padding(horizontal = MistSpace.lg),
                    )
                }
            }
        }

        // ── 可用模型 ────────────────────────────────────────────────────
        if (models.isNotEmpty()) {
            item(key = "remote_chat_models") {
                SmallTitle(stringResource(R.string.remote_chat_models))
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    models.forEach { modelId ->
                        ArrowPreference(
                            title = modelId,
                            onClick = { viewModel.selectModel(modelId) },
                        )
                    }
                }
            }
        }

        // ── 流式事件 ────────────────────────────────────────────────────
        item(key = "remote_chat_events") {
            SmallTitle(stringResource(R.string.remote_chat_events_title))
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                when {
                    activeSessionId.isBlank() -> MistEmptyState(
                        title = stringResource(R.string.remote_chat_pick_session),
                        icon = Icons.Rounded.ChatBubble,
                    )
                    events.isEmpty() -> MistEmptyState(
                        title = stringResource(R.string.remote_chat_events_empty),
                        icon = Icons.Rounded.ChatBubble,
                    )
                    else -> Column(
                        modifier = Modifier.padding(MistSpace.lg),
                        verticalArrangement = Arrangement.spacedBy(MistSpace.xs),
                    ) {
                        events.forEach { event ->
                            Text(
                                text = event,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
            }
        }

        // ── 输入条 ──────────────────────────────────────────────────────
        item(key = "remote_chat_composer") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                Column(
                    modifier = Modifier.padding(MistSpace.lg),
                    verticalArrangement = Arrangement.spacedBy(MistSpace.sm),
                ) {
                    TextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = stringResource(R.string.remote_chat_input_label),
                        singleLine = true,
                        enabled = activeSessionId.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(MistSpace.sm)) {
                        TextButton(
                            text = stringResource(R.string.remote_chat_send),
                            enabled = draft.isNotBlank() && activeSessionId.isNotBlank() && !busy,
                            onClick = {
                                viewModel.sendPrompt(draft)
                                draft = ""
                            },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            text = stringResource(R.string.remote_chat_cancel),
                            onClick = { viewModel.cancelRun() },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (activeSessionId.isBlank()) {
                        Text(
                            text = stringResource(R.string.remote_chat_no_session),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}
