/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * Mist Glass 玻璃表面组件：以四档材质建立层次（不靠阴影，靠半透明填充 + 发丝描边）。
 * 为兼容性与性能，默认不启用真模糊，改用半透明填充近似；低端机同样安全。
 */
package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.ui.theme2.MistGlassTier
import io.github.mangi.eta.ui.theme2.MistShapes
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 每档材质默认对应的圆角。 */
private fun MistGlassTier.defaultShape(): Shape = when (this) {
    MistGlassTier.chrome -> MistShapes.xl
    MistGlassTier.sheet -> MistShapes.xl
    MistGlassTier.card -> MistShapes.lg
    MistGlassTier.pill -> MistShapes.pill
}

/**
 * Mist Glass 表面容器。
 *
 * @param tier 材质档位，决定填充透明度。
 * @param shape 圆角，默认按档位取值。
 */
@Composable
internal fun MistGlassSurface(
    tier: MistGlassTier,
    modifier: Modifier = Modifier,
    shape: Shape = tier.defaultShape(),
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val colors = MiuixTheme.colorScheme
    val fillAlpha = if (dark) tier.fillAlphaDark else tier.fillAlphaLight
    val fill = colors.surface.copy(alpha = fillAlpha)
    Box(
        modifier = modifier
            .clip(shape)
            .background(fill, shape)
            .border(width = 1.dp, color = colors.dividerLine, shape = shape),
        content = content,
    )
}
