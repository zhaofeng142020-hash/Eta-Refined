/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 工具标签：圆角与内边距改用 Mist Glass 令牌（sm 档 10dp / 8dp / 4dp）。
 */
package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import io.github.mangi.eta.ui.theme2.MistShapes
import io.github.mangi.eta.ui.theme2.MistSpace
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ToolChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(MistShapes.sm)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = MistSpace.sm, vertical = MistSpace.xs),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
