/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 远程主机设置页：本机/远程主机切换、连接状态卡、Relay（第三方服务器）与直连主机配置。
 * 只调用契约 A 暴露的 API；不修改任何既有逻辑，本文件为纯新增。
 */
package io.github.mangi.eta.ui.screens.remote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import io.github.mangi.eta.agent.remote.RemoteHostConfig
import io.github.mangi.eta.agent.remote.RemoteHostDefaults
import io.github.mangi.eta.agent.remote.RemoteHostKind
import io.github.mangi.eta.agent.remote.RemoteLinkState
import io.github.mangi.eta.agent.remote.RemoteLinkStore
import io.github.mangi.eta.data.remote.RemoteHostRepository
import io.github.mangi.eta.ui.components.MistEmptyState
import io.github.mangi.eta.ui.components.MistErrorCard
import io.github.mangi.eta.ui.components.MiuixDialogActions
import io.github.mangi.eta.ui.components.MiuixScaffoldPage
import io.github.mangi.eta.ui.theme2.MistSpace
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import java.util.UUID

@Composable
internal fun RemoteHostSettingsScreen(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 契约 UI 调用示例（必须照抄这个形状）：仓库由 UI 持有，状态直接 collect。
    val repo = remember { RemoteHostRepository(context) }
    val hosts by repo.hosts().collectAsState(initial = emptyList())
    val status by RemoteLinkStore.status.collectAsState()
    val relayEnabled by repo.relayServerEnabled().collectAsState(initial = true)
    val relayUrl by repo.relayServerUrl()
        .collectAsState(initial = RemoteHostDefaults.RELAY_SERVER_URL)

    var activeHostId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(hosts) {
        activeHostId = repo.activeHostId()
    }

    var relayUrlDraft by remember(relayUrl) { mutableStateOf(relayUrl) }
    var editing by remember { mutableStateOf<RemoteHostConfig?>(null) }
    var showForm by remember { mutableStateOf(false) }

    MiuixScaffoldPage(
        title = stringResource(R.string.remote_hosts_title),
        onBack = onBack,
    ) {
        // ── 当前主机切换胶囊 ────────────────────────────────────────────
        item(key = "remote_switcher") {
            RemoteHostSwitcher(
                hosts = hosts.filter { it.kind == RemoteHostKind.DIRECT || relayEnabled },
                activeHostId = activeHostId,
                status = status,
                onSelect = { id ->
                    scope.launch {
                        repo.setActiveHostId(id)
                        activeHostId = id
                    }
                },
            )
        }

        // ── 第三方服务器显著声明 ────────────────────────────────────────
        item(key = "remote_third_party_notice") {
            SmallTitle(stringResource(R.string.remote_relay_notice_title))
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                Column(modifier = Modifier.padding(MistSpace.lg)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MiuixTheme.colorScheme.error,
                        )
                        Spacer(Modifier.width(MistSpace.sm))
                        Text(
                            text = stringResource(R.string.remote_relay_third_party_notice),
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(MistSpace.sm))
                    // 凭据绝不静默上传：仅在用户显式输入并点保存时经 repo.saveLaunchToken 写入本机 Keystore。
                    Text(
                        text = stringResource(R.string.remote_relay_credentials_notice),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }

        // ── 连接状态卡 ──────────────────────────────────────────────────
        item(key = "remote_link_status") {
            SmallTitle(stringResource(R.string.remote_link_status_title))
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                val link = status
                if (link == null) {
                    Text(
                        text = stringResource(R.string.remote_link_no_status),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(MistSpace.lg),
                    )
                } else {
                    val fallbackError = stringResource(R.string.remote_error_generic)
                    Column(
                        modifier = Modifier.padding(MistSpace.lg),
                        verticalArrangement = Arrangement.spacedBy(MistSpace.xs),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.remote_link_state_label,
                                remoteLinkStateLabel(link.state),
                            ),
                            style = MiuixTheme.textStyles.body2,
                        )
                        Text(
                            text = stringResource(
                                R.string.remote_link_transport_label,
                                remoteTransportLabel(link.transport),
                            ),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        // 契约：latencyMs 未测量为 null，UI 不得显示 0；仅在非 null 时渲染。
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
                        if (link.state == RemoteLinkState.ERROR) {
                            MistErrorCard(
                                title = stringResource(R.string.remote_error_title),
                                message = link.detail.ifBlank { fallbackError },
                                fixHint = stringResource(R.string.remote_error_fix_hint),
                                onRetry = { RemoteLinkStore.refresh(link.hostId) },
                                retryLabel = stringResource(R.string.remote_action_retry),
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(MistSpace.sm)) {
                            TextButton(
                                text = stringResource(R.string.remote_link_refresh),
                                onClick = { RemoteLinkStore.refresh(link.hostId) },
                            )
                            TextButton(
                                text = stringResource(R.string.remote_link_disconnect),
                                onClick = { RemoteLinkStore.disconnect() },
                            )
                        }
                    }
                }
            }
        }

        // ── Relay（第三方托管服务）开关与 Server 地址 ────────────────────
        item(key = "remote_relay") {
            SmallTitle(stringResource(R.string.remote_relay_group))
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                // 开关旁必须显著标注：默认连第三方托管服务，可关闭或改为自建地址。
                SwitchPreference(
                    title = stringResource(R.string.remote_relay_enable),
                    summary = stringResource(R.string.remote_relay_third_party_notice),
                    checked = relayEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { repo.setRelayServerEnabled(enabled) }
                    },
                )
                if (relayEnabled) {
                    TextField(
                        value = relayUrlDraft,
                        onValueChange = { relayUrlDraft = it },
                        label = stringResource(R.string.remote_relay_server_url),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MistSpace.lg),
                    )
                    Text(
                        text = stringResource(
                            R.string.remote_relay_server_hint,
                            RemoteHostDefaults.RELAY_SERVER_URL,
                        ),
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(horizontal = MistSpace.lg),
                    )
                    TextButton(
                        text = stringResource(R.string.remote_relay_save_url),
                        onClick = {
                            scope.launch { repo.setRelayServerUrl(relayUrlDraft.trim()) }
                        },
                        modifier = Modifier.padding(horizontal = MistSpace.lg),
                    )
                }
            }
        }

        // ── 已保存主机（直连 / Relay） ──────────────────────────────────
        item(key = "remote_hosts") {
            SmallTitle(stringResource(R.string.remote_host_list_title))
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                val visible = hosts.filter {
                    it.kind == RemoteHostKind.DIRECT || relayEnabled
                }
                if (visible.isEmpty()) {
                    MistEmptyState(
                        title = stringResource(R.string.remote_host_empty),
                        summary = stringResource(R.string.remote_host_empty_summary),
                        icon = Icons.Rounded.Hub,
                        action = {
                            TextButton(
                                text = stringResource(R.string.remote_host_add),
                                onClick = {
                                    editing = null
                                    showForm = true
                                },
                            )
                        },
                    )
                } else {
                    visible.forEach { host ->
                        ArrowPreference(
                            title = host.displayName,
                            summary = stringResource(
                                R.string.remote_host_row_summary,
                                remoteHostKindLabel(host.kind),
                                if (host.kind == RemoteHostKind.DIRECT) host.baseUrl else host.serverUrl,
                            ),
                            onClick = {
                                editing = host
                                showForm = true
                            },
                        )
                    }
                    TextButton(
                        text = stringResource(R.string.remote_host_add),
                        onClick = {
                            editing = null
                            showForm = true
                        },
                        modifier = Modifier.padding(horizontal = MistSpace.lg),
                    )
                }
                ArrowPreference(
                    title = stringResource(R.string.remote_host_open_chat),
                    summary = stringResource(R.string.remote_host_open_chat_summary),
                    onClick = { onOpenChat("") },
                )
            }
        }
    }

    if (showForm) {
        RemoteHostFormDialog(
            initial = editing,
            relayServerUrl = relayUrl,
            onDismiss = { showForm = false },
            onSave = { config, token ->
                scope.launch {
                    repo.upsert(config)
                    // 仅在用户显式输入并点保存时写入本机 Keystore，绝不静默上传凭据。
                    if (token.isNotBlank()) {
                        repo.saveLaunchToken(config.id, token)
                    }
                    showForm = false
                }
            },
            onActivate = { id ->
                scope.launch {
                    repo.setActiveHostId(id)
                    activeHostId = id
                }
                showForm = false
            },
            onRemove = { id ->
                scope.launch {
                    repo.remove(id)
                    repo.clearCredentials(id)
                    if (activeHostId == id) {
                        repo.setActiveHostId(null)
                        activeHostId = null
                    }
                    showForm = false
                }
            },
        )
    }
}

/**
 * 新增 / 编辑主机表单。凭据字段只在本机 Keystore 落地（见 onSave），不做任何静默上传。
 */
@Composable
private fun RemoteHostFormDialog(
    initial: RemoteHostConfig?,
    relayServerUrl: String,
    onDismiss: () -> Unit,
    onSave: (RemoteHostConfig, String) -> Unit,
    onActivate: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    val editId = initial?.id
    var name by remember { mutableStateOf(initial?.displayName.orEmpty()) }
    var useRelay by remember { mutableStateOf(initial?.kind == RemoteHostKind.RELAY) }
    var baseUrl by remember { mutableStateOf(initial?.baseUrl.orEmpty()) }
    var serverUrl by remember {
        mutableStateOf(initial?.serverUrl?.takeIf { it.isNotBlank() } ?: relayServerUrl)
    }
    var token by remember { mutableStateOf("") }

    WindowDialog(
        show = true,
        title = stringResource(
            if (editId == null) R.string.remote_host_add else R.string.remote_host_edit,
        ),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MistSpace.md),
        ) {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.remote_host_name),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SwitchPreference(
                title = stringResource(R.string.remote_host_use_relay),
                summary = stringResource(
                    if (useRelay) R.string.remote_host_kind_relay else R.string.remote_host_kind_direct,
                ),
                checked = useRelay,
                onCheckedChange = { useRelay = it },
            )
            if (useRelay) {
                TextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = stringResource(R.string.remote_host_server_url),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(
                        R.string.remote_host_server_url_hint,
                        RemoteHostDefaults.RELAY_SERVER_URL,
                    ),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            } else {
                TextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = stringResource(R.string.remote_host_base_url),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(
                        R.string.remote_host_base_url_hint,
                        RemoteHostDefaults.DIRECT_DEFAULT_PORT,
                    ),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                TextField(
                    value = token,
                    onValueChange = { token = it },
                    label = stringResource(R.string.remote_host_launch_token),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.remote_host_launch_token_hint),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Text(
                    text = stringResource(R.string.remote_relay_credentials_notice),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }

            if (editId != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(MistSpace.sm)) {
                    TextButton(
                        text = stringResource(R.string.remote_host_set_active),
                        onClick = { onActivate(editId) },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = stringResource(R.string.remote_host_remove),
                        onClick = { onRemove(editId) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            MiuixDialogActions(
                confirmText = stringResource(R.string.remote_host_save),
                confirmEnabled = name.isNotBlank(),
                onCancel = onDismiss,
                onConfirm = {
                    val config = RemoteHostConfig(
                        id = editId ?: UUID.randomUUID().toString(),
                        kind = if (useRelay) RemoteHostKind.RELAY else RemoteHostKind.DIRECT,
                        displayName = name.trim(),
                        baseUrl = if (useRelay) "" else baseUrl.trim(),
                        serverUrl = if (useRelay) {
                            serverUrl.trim().ifBlank { relayServerUrl }
                        } else {
                            ""
                        },
                        enabled = initial?.enabled ?: true,
                    )
                    onSave(config, token)
                },
                modifier = Modifier.padding(top = MistSpace.sm),
            )
        }
    }
}
