/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 统一空状态：图 / 标题 / 说明 / 可选行动按钮，全局一致的形态与留白。
 * 同时支持「文字按钮」与「自定义 action slot」，便于既有 ListEmptyState 平滑委托。
 */
package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.ui.theme2.MistShapes
import io.github.mangi.eta.ui.theme2.MistSpace
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MistEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = MistSpace.xxl, vertical = MistSpace.xl),
        verticalArrangement = Arrangement.spacedBy(MistSpace.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon?.let {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MistShapes.lg)
                    .background(colors.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = colors.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.height(MistSpace.xs))
        }
        Text(
            text = title,
            style = if (summary != null) MiuixTheme.textStyles.body1 else MiuixTheme.textStyles.body2,
            fontWeight = if (summary != null) FontWeight.Medium else null,
            color = if (summary != null) colors.onSurface else colors.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
        )
        summary?.let {
            Text(
                text = it,
                style = MiuixTheme.textStyles.body2,
                color = colors.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
            )
        }
        when {
            action != null -> {
                Spacer(Modifier.height(MistSpace.sm))
                action()
            }
            actionLabel != null && onAction != null -> {
                Spacer(Modifier.height(MistSpace.sm))
                TextButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}
