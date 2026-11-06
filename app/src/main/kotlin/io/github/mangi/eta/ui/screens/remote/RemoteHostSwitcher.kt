/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 首页主机切换胶囊：本机 Eta Agent / 已保存远程主机 + 连接状态。
 * 只依赖契约类型与现有 Mist Glass / Miuix 组件；本文件同时放置三个共享文案映射。
 */
package io.github.mangi.eta.ui.screens.remote

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.R
import io.github.mangi.eta.agent.remote.RemoteHostConfig
import io.github.mangi.eta.agent.remote.RemoteHostKind
import io.github.mangi.eta.agent.remote.RemoteLinkState
import io.github.mangi.eta.agent.remote.RemoteLinkStatus
import io.github.mangi.eta.ui.components.MistGlassSurface
import io.github.mangi.eta.ui.theme2.MistGlassTier
import io.github.mangi.eta.ui.theme2.MistSpace
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun remoteLinkStateLabel(state: RemoteLinkState): String = when (state) {
    RemoteLinkState.IDLE -> stringResource(R.string.remote_link_state_idle)
    RemoteLinkState.CONNECTING -> stringResource(R.string.remote_link_state_connecting)
    RemoteLinkState.CONNECTED -> stringResource(R.string.remote_link_state_connected)
    RemoteLinkState.ERROR -> stringResource(R.string.remote_link_state_error)
}

@Composable
internal fun remoteTransportLabel(transport: String): String = when (transport) {
    "http" -> stringResource(R.string.remote_link_transport_http)
    "relay" -> stringResource(R.string.remote_link_transport_relay)
    else -> stringResource(R.string.remote_link_transport_unknown)
}

@Composable
internal fun remoteHostKindLabel(kind: RemoteHostKind): String = when (kind) {
    RemoteHostKind.DIRECT -> stringResource(R.string.remote_host_kind_direct)
    RemoteHostKind.RELAY -> stringResource(R.string.remote_host_kind_relay)
}

/**
 * @param activeHostId null 表示使用本机 Eta Agent。
 * @param status 来自 RemoteLinkStore.status；为空时按「未连接」显示。
 * @param onSelect 传 null 切回本机，传非空 id 切到对应远程主机。
 */
@Composable
internal fun RemoteHostSwitcher(
    hosts: List<RemoteHostConfig>,
    activeHostId: String?,
    status: RemoteLinkStatus?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val active = hosts.firstOrNull { it.id == activeHostId }
    val currentLabel = active?.displayName ?: stringResource(R.string.remote_switcher_local)
    val stateLabel = remoteLinkStateLabel(status?.state ?: RemoteLinkState.IDLE)

    MistGlassSurface(
        tier = MistGlassTier.pill,
        modifier = modifier.padding(horizontal = MistSpace.md, vertical = MistSpace.sm),
    ) {
        Row(
            modifier = Modifier
                .clickable { showPicker = true }
                .padding(horizontal = MistSpace.lg, vertical = MistSpace.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MistSpace.sm),
        ) {
            Icon(
                imageVector = Icons.Rounded.Smartphone,
                contentDescription = stringResource(R.string.remote_switcher_cd),
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.primary,
            )
            Text(
                text = currentLabel,
                style = MiuixTheme.textStyles.body1,
            )
            Text(
                text = stateLabel,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }

    WindowDialog(
        show = showPicker,
        title = stringResource(R.string.remote_switcher_pick_title),
        onDismissRequest = { showPicker = false },
    ) {
        Card(modifier = Modifier.padding(bottom = MistSpace.md)) {
            ArrowPreference(
                title = stringResource(R.string.remote_switcher_local),
                summary = if (activeHostId == null) {
                    stringResource(R.string.remote_switcher_active_tag)
                } else {
                    null
                },
                onClick = {
                    onSelect(null)
                    showPicker = false
                },
            )
            hosts.forEach { host ->
                ArrowPreference(
                    title = host.displayName,
                    summary = if (host.id == activeHostId) {
                        stringResource(R.string.remote_switcher_active_tag)
                    } else {
                        stringResource(
                            R.string.remote_host_row_summary,
                            remoteHostKindLabel(host.kind),
                            if (host.kind == RemoteHostKind.DIRECT) host.baseUrl else host.serverUrl,
                        )
                    },
                    onClick = {
                        onSelect(host.id)
                        showPicker = false
                    },
                )
            }
        }
    }
}
