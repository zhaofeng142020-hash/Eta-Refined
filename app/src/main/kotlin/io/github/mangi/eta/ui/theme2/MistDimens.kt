/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * Mist Glass 尺寸 / 圆角 / 动效 / 玻璃材质令牌。
 * 数值参考 dsh-Go 公开设计语言的量化指标（不复制代码）。
 */
package io.github.mangi.eta.ui.theme2

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** 间距阶梯（4 的倍数为主）。 */
object MistSpace {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    /** 页面水平留白。 */
    val gutter = 16.dp
    /** 同一轮内部竖向间距。 */
    val inTurn = 8.dp
    /** 换轮之间竖向间距。 */
    val betweenTurns = 24.dp
    /** 正文最大阅读宽度（居中）。 */
    val readingWidth = 720.dp
}

/**
 * 圆角五档 + 胶囊。
 * Compose 无原生连续曲率（Squircle）API，此处用标准圆角近似；如需更强曲率可后续以
 * `RoundedCornerShape(percent = …)` 或自定义 Shape 逼近。
 */
object MistShapes {
    /** 各档半径的 Dp 值，供需要 Dp 参数的组件（如 squircleSurface）直接消费。 */
    val xsRadius = 6.dp
    val smRadius = 10.dp
    val mdRadius = 14.dp
    val lgRadius = 20.dp
    val xlRadius = 28.dp

    val xs = RoundedCornerShape(xsRadius)     // 行内代码、角标、进度端点
    val sm = RoundedCornerShape(smRadius)    // Chip、小按钮、菜单高亮
    val md = RoundedCornerShape(mdRadius)    // 卡片、代码井、输入框、主按钮
    val lg = RoundedCornerShape(lgRadius)    // 用户气泡、审批卡、设置分组卡
    val xl = RoundedCornerShape(xlRadius)    // composer 输入条、底部弹层、对话框
    val pill = RoundedCornerShape(percent = 50) // 顶栏胶囊、模型胶囊、发送键
}

/** 动效时长（毫秒）与曲线。 */
object MistMotion {
    const val pressMs = 90
    const val quickMs = 160
    const val standardMs = 240
    const val collapseMs = 280
    const val emphasizedMs = 340
    const val sheetInMs = 380
    const val sheetOutMs = 260
    const val pageMs = 300
    const val breathMs = 2400

    /** 与 DSH Web 同族的 cubic-bezier(.4, 0, .2, 1)。 */
    val std: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    val enter: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val exit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val glide: Easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    val sheetRise: Easing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1f)

    /** 按压缩放比例。 */
    const val pressScale = 0.97f

    /** 列表首次出现逐项错开的延时（毫秒）与只错开的前 N 项。 */
    const val staggerMs = 30
    const val staggerLimit = 8
}

/**
 * 玻璃材质四档。
 *
 * 说明：Compose 没有与 Flutter BackdropFilter 等价的「只模糊背景、不模糊内容」的简单
 * Modifier（`Modifier.blur` 会连同内容一起模糊）。因此 Eta-Refined 用「半透明填充 +
 * 发丝描边 + 零 elevation」近似玻璃观感，由 `MistGlassSurface` 消费下面两个透明度。
 * 不声明无法兑现的模糊半径，避免令牌与实现不一致。
 */
enum class MistGlassTier(
    /** 填充透明度（0–1），按浅色 / 深色分别取值。 */
    val fillAlphaLight: Float,
    val fillAlphaDark: Float,
) {
    /** 顶栏 / 输入条 / 底栏：浮在滚动内容之上。 */
    chrome(fillAlphaLight = 0.70f, fillAlphaDark = 0.72f),

    /** 底部弹层 / 菜单 / 抽屉。 */
    sheet(fillAlphaLight = 0.82f, fillAlphaDark = 0.86f),

    /** 设置卡 / 审批卡：静态背景。 */
    card(fillAlphaLight = 0.62f, fillAlphaDark = 0.05f),

    /** 胶囊 / chip：滚动列表小件。 */
    pill(fillAlphaLight = 0.55f, fillAlphaDark = 0.06f),
}
