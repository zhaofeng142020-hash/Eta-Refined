/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * Eta-Refined 全局开关与运行时能力探测。
 */
package io.github.mangi.eta.ui.theme2

/**
 * 重构层总开关。
 *
 * - [USE_MIST_GLASS] 为 false 时，应用完全回退到原 Miuix 主题，保证「一键回退」。
 * - 关闭方法：把此常量改为 false 并重新构建；或后续接入设置项读取该标志。
 */
object RefinedUi {
    /** Mist Glass 设计语言总开关。 */
    const val USE_MIST_GLASS: Boolean = true

    /**
     * 是否允许玻璃真模糊。低端机 / 用户关闭模糊时返回 false，
     * 玻璃表面将回退为不透明面色填充（性能友好且不影响布局）。
     */
    fun glassBlurAllowed(blurEnabledByUser: Boolean, performanceLimited: Boolean): Boolean =
        USE_MIST_GLASS && blurEnabledByUser && !performanceLimited
}
