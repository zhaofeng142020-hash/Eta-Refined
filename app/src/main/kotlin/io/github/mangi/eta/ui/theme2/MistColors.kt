/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * “Mist Glass（雾蓝玻璃）”设计令牌层。
 *
 * 设计语言参考自 ccch1mneyyy/dsh-Go（AGPL-3.0）的公开 UI 观感，仅借鉴其设计语言、
 * 交互模式与量化 token（颜色、圆角、间距、字级、动效、玻璃材质），不复制其任何源代码。
 * 本文件为原创 Kotlin/Compose 实现，映射到 Miuix 的 Colors 角色上，从而使整个既有
 * UI（361 处 MiuixTheme.colorScheme.* 调用）无需逐文件改动即可获得新的视觉语言。
 *
 * 接入点见 ui/app/AgentAppTheme.kt：当 RefinedUi.USE_MIST_GLASS 为真且用户未启用
 * Money 动态取色时，用本文件的 mistGlassColors() 覆盖 Miuix 默认色板。
 */
package io.github.mangi.eta.ui.theme2

import androidx.compose.ui.graphics.Color
import io.github.mangi.eta.data.model.AppearanceAccentColor
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/** 语义状态色（不随主色转动，保证危险/成功/警告始终可辨）。 */
object MistSemantic {
    val success = Color(0xFF337250)
    val successContainer = Color(0xFFD9F0E1)
    val warning = Color(0xFF936123)
    val warningContainer = Color(0xFFF9E9CE)
    val danger = Color(0xFFA54743)
    val dangerContainer = Color(0xFFFFE2E0)
    val info = Color(0xFF2971A7)
    val infoContainer = Color(0xFFDBECFC)

    fun successFor(isDark: Boolean) = if (isDark) Color(0xFF8FC7A8) else success
    fun warningFor(isDark: Boolean) = if (isDark) Color(0xFFD9B273) else warning
    fun dangerFor(isDark: Boolean) = if (isDark) Color(0xFFE39A96) else danger
    fun infoFor(isDark: Boolean) = if (isDark) Color(0xFF8DBEDD) else info
}

/** 浅色「晨雾蓝」色板原始 hex。 */
private object MistLight {
    val pageOpaque = Color(0xFFF0F4FD)
    val surfaceLowest = Color(0xFFFBFCFF)
    val surfaceLow = Color(0xFFF6F9FE)
    val container = Color(0xFFEEF1F9)
    val containerHigh = Color(0xFFE7EBF4)
    val containerHighest = Color(0xFFDFE4EE)
    val textPrimary = Color(0xFF1E232E)
    val textSecondary = Color(0xFF4F5562)
    val textTertiary = Color(0xFF646974)
    val textDisabled = Color(0xFFA1A5AC)
    val outline = Color(0xFF7E838D)
    val hairline = Color(0xFFDCE0E8)
    val primaryContainer = Color(0xFFDCE6FA)
    val onPrimaryContainer = Color(0xFF1E232E)
    val secondary = Color(0xFFE6E6E6)
    val error = Color(0xFFA54743)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFFFE2E0)
    val onErrorContainer = Color(0xFF410002)
    val tertiaryContainer = Color(0xFFECE5FB)
    val onTertiaryContainer = Color(0xFF6B578F)
    val windowDimming = Color(0x520B111F) // #0B111F 32%
}

/** 深色「深海石板」色板原始 hex（非纯黑）。 */
private object MistDark {
    val pageOpaque = Color(0xFF1C1F28)
    val surfaceLowest = Color(0xFF14171E)
    val surfaceLow = Color(0xFF232730)
    val container = Color(0xFF232730)
    val containerHigh = Color(0xFF272C36)
    val containerHighest = Color(0xFF2D323C)
    val textPrimary = Color(0xFFE6E9F0)
    val textSecondary = Color(0xFFB6BBC5)
    val textTertiary = Color(0xFF9A9EA9)
    val textDisabled = Color(0xFF656971)
    val outline = Color(0xFF737781)
    val hairline = Color(0xFF343841)
    val primaryContainer = Color(0xFF2A3550)
    val onPrimaryContainer = Color(0xFFDCE6FA)
    val secondary = Color(0xFF505050)
    val error = Color(0xFFF2B8B5)
    val onError = Color(0xFF601410)
    val errorContainer = Color(0xFF8C1D18)
    val onErrorContainer = Color(0xFFF9DEDC)
    val tertiaryContainer = Color(0xFF2B2A40)
    val onTertiaryContainer = Color(0xFFB8ABD2)
    val windowDimming = Color(0x7A000000) // 纯黑 48%
}

/** 强调色种子：dsh-Go 的六套预设被映射到 Eta 现有的 8 种强调色枚举上。 */
private fun accentSeed(accent: AppearanceAccentColor, isDark: Boolean): Color = when (accent) {
    AppearanceAccentColor.SYSTEM, AppearanceAccentColor.BLUE ->
        if (isDark) Color(0xFF97B1F0) else Color(0xFF4B67B4) // mist 雾蓝
    AppearanceAccentColor.PURPLE ->
        if (isDark) Color(0xFFB8ABD2) else Color(0xFF726291) // dusk 暮紫
    AppearanceAccentColor.PINK ->
        if (isDark) Color(0xFFD9A6B4) else Color(0xFF8D5B6B)
    AppearanceAccentColor.RED ->
        if (isDark) Color(0xFFE39A96) else Color(0xFFA54743)
    AppearanceAccentColor.ORANGE ->
        if (isDark) Color(0xFFCCAC87) else Color(0xFF8D6636) // amber 琥珀
    AppearanceAccentColor.YELLOW ->
        if (isDark) Color(0xFFC9BA7A) else Color(0xFF7A6A2E)
    AppearanceAccentColor.GREEN ->
        if (isDark) Color(0xFF8DBEB7) else Color(0xFF34766F) // turquoise 松石
    AppearanceAccentColor.TEAL ->
        if (isDark) Color(0xFF8CB9D7) else Color(0xFF236185) // daiqing 黛青
}

/** 强调色上的前景色：浅色种子配白字，深色种子配深墨字（保证对比度）。 */
private fun onAccent(isDark: Boolean): Color =
    if (isDark) Color(0xFF10141C) else Color(0xFFFFFFFF)

/**
 * 生成浅色 Mist Glass 色板，映射到 Miuix [Colors] 的语义角色。
 * 未显式覆盖的角色沿用 Miuix 默认值，避免破坏既有组件行为。
 */
fun mistLightColors(accent: AppearanceAccentColor): Colors {
    val primary = accentSeed(accent, isDark = false)
    return lightColorScheme(
        primary = primary,
        onPrimary = onAccent(false),
        primaryVariant = primary,
        onPrimaryVariant = onAccent(false),
        primaryContainer = MistLight.primaryContainer,
        onPrimaryContainer = MistLight.onPrimaryContainer,
        secondary = MistLight.secondary,
        onSecondary = MistLight.textPrimary,
        secondaryVariant = MistLight.container,
        onSecondaryVariant = MistLight.textPrimary,
        secondaryContainer = MistLight.container,
        onSecondaryContainer = MistLight.textSecondary,
        secondaryContainerVariant = MistLight.containerHigh,
        onSecondaryContainerVariant = MistLight.textTertiary,
        tertiaryContainer = MistLight.tertiaryContainer,
        onTertiaryContainer = MistLight.onTertiaryContainer,
        tertiaryContainerVariant = MistLight.container,
        background = MistLight.pageOpaque,
        onBackground = MistLight.textPrimary,
        onBackgroundVariant = MistLight.textSecondary,
        surface = MistLight.pageOpaque,
        onSurface = MistLight.textPrimary,
        surfaceVariant = MistLight.surfaceLowest,
        onSurfaceSecondary = MistLight.textSecondary,
        onSurfaceVariantSummary = MistLight.textSecondary,
        onSurfaceVariantActions = MistLight.textTertiary,
        disabledOnSurface = MistLight.textDisabled,
        disabledOnSecondaryVariant = MistLight.textDisabled,
        surfaceContainer = MistLight.surfaceLowest,
        onSurfaceContainer = MistLight.textPrimary,
        onSurfaceContainerVariant = MistLight.textTertiary,
        surfaceContainerHigh = MistLight.container,
        onSurfaceContainerHigh = MistLight.textTertiary,
        surfaceContainerHighest = MistLight.containerHigh,
        onSurfaceContainerHighest = MistLight.textPrimary,
        outline = MistLight.outline,
        dividerLine = MistLight.hairline,
        error = MistLight.error,
        onError = MistLight.onError,
        errorContainer = MistLight.errorContainer,
        onErrorContainer = MistLight.onErrorContainer,
        windowDimming = MistLight.windowDimming,
    )
}

/** 生成深色 Mist Glass 色板。 */
fun mistDarkColors(accent: AppearanceAccentColor): Colors {
    val primary = accentSeed(accent, isDark = true)
    return darkColorScheme(
        primary = primary,
        onPrimary = onAccent(true),
        primaryVariant = primary,
        onPrimaryVariant = onAccent(true),
        primaryContainer = MistDark.primaryContainer,
        onPrimaryContainer = MistDark.onPrimaryContainer,
        secondary = MistDark.secondary,
        onSecondary = Color(0xFFF2F2F2),
        secondaryVariant = MistDark.container,
        onSecondaryVariant = MistDark.textPrimary,
        secondaryContainer = MistDark.container,
        onSecondaryContainer = MistDark.textSecondary,
        secondaryContainerVariant = MistDark.containerHigh,
        onSecondaryContainerVariant = MistDark.textTertiary,
        tertiaryContainer = MistDark.tertiaryContainer,
        onTertiaryContainer = MistDark.onTertiaryContainer,
        tertiaryContainerVariant = MistDark.container,
        background = MistDark.pageOpaque,
        onBackground = MistDark.textPrimary,
        onBackgroundVariant = MistDark.textSecondary,
        surface = MistDark.pageOpaque,
        onSurface = MistDark.textPrimary,
        surfaceVariant = MistDark.surfaceLow,
        onSurfaceSecondary = MistDark.textSecondary,
        onSurfaceVariantSummary = MistDark.textSecondary,
        onSurfaceVariantActions = MistDark.textTertiary,
        disabledOnSurface = MistDark.textDisabled,
        disabledOnSecondaryVariant = MistDark.textDisabled,
        surfaceContainer = MistDark.surfaceLow,
        onSurfaceContainer = MistDark.textPrimary,
        onSurfaceContainerVariant = MistDark.textTertiary,
        surfaceContainerHigh = MistDark.containerHigh,
        onSurfaceContainerHigh = MistDark.textTertiary,
        surfaceContainerHighest = MistDark.containerHighest,
        onSurfaceContainerHighest = MistDark.textPrimary,
        outline = MistDark.outline,
        dividerLine = MistDark.hairline,
        error = MistDark.error,
        onError = MistDark.onError,
        errorContainer = MistDark.errorContainer,
        onErrorContainer = MistDark.onErrorContainer,
        windowDimming = MistDark.windowDimming,
    )
}

/** 统一入口：按明暗与强调色生成 Mist Glass [Colors]。 */
fun mistGlassColors(isDark: Boolean, accent: AppearanceAccentColor): Colors =
    if (isDark) mistDarkColors(accent) else mistLightColors(accent)
