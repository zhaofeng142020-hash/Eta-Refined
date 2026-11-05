/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 应用实际解析出的明暗状态。不能用 isSystemInDarkTheme() 代替：Eta 支持在设置里
 * 强制浅色/深色，此时系统主题与应用主题可能相反，玻璃透明度与语义色会取错档位。
 */
package io.github.mangi.eta.ui.theme2

import androidx.compose.runtime.staticCompositionLocalOf

/** 由 AgentAppTheme 按 AppearanceSettings.themeMode 解析后提供。默认跟随系统（false = 浅色）。 */
val LocalMistIsDark = staticCompositionLocalOf { false }
