/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * Mist Glass 字号层级令牌（14 档）。供新组件（错误卡、空状态、首启向导等）使用。
 * 既有界面仍走 Miuix 的 TextStyles，二者视觉一致即可渐进迁移。
 */
package io.github.mangi.eta.ui.theme2

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object MistType {
    /** 欢迎语、安装完成大字。 */
    val displaySmall = TextStyle(fontSize = 28.sp, lineHeight = 35.sp, fontWeight = FontWeight.Light)
    /** 页面标题。 */
    val headlineMedium = TextStyle(fontSize = 22.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold)
    /** 对话框标题。 */
    val headlineSmall = TextStyle(fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold)
    /** 弹层标题、顶栏会话名。 */
    val titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold)
    /** 卡片 / 列表标题。 */
    val titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold)
    /** 工具行名、分组名。 */
    val titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    /** 聊天正文。 */
    val bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal)
    /** 界面正文。 */
    val bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)
    /** 说明、元信息。 */
    val bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
    /** 按钮。 */
    val labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    /** Chip、胶囊。 */
    val labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
    /** 时间戳、角标。 */
    val labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Normal)
    /** 分区小标题（小写字距）。 */
    val eyebrow = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
    )
    /** 「Eta」字标。 */
    val wordmark = TextStyle(
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
    )
}
