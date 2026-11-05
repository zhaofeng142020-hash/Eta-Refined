/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 列表空态：委托给 MistEmptyState，使所有既有调用方自动获得统一的 Mist Glass 形态与留白。
 * 签名保持不变，避免改动上游调用点。
 */
package io.github.mangi.eta.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 列表空态：有说明时标题用正文色作为主信息、说明退为次级；只有一句话时整体弱化，
 * 避免一条“暂无内容”在页面里显得比真实内容还醒目。
 */
@Composable
internal fun ListEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
) {
    MistEmptyState(
        title = title,
        modifier = modifier,
        summary = summary,
        icon = icon,
        action = action,
    )
}
