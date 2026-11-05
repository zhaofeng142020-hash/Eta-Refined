/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 首启向导完成状态。全新安装时显示向导；完成后持久化标记，后续启动不再显示。
 * 使用独立的 SharedPreferences 文件，避免与上游配置中枢耦合，也便于未来清除向导状态。
 */
package io.github.mangi.eta.ui.onboarding

import android.content.Context

internal object OnboardingState {
    private const val PREF_FILE = "eta_refined_onboarding"
    private const val KEY_COMPLETED = "onboarding_completed"

    /** 是否已走过（或跳过）首启向导。默认 false = 全新安装。 */
    fun isCompleted(context: Context): Boolean =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)

    /** 标记向导已完成 / 已跳过。 */
    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, true)
            .apply()
    }

    /** 供设置页「重新查看新手引导」使用：清除标记后下次回到首页会再次显示。 */
    fun reset(context: Context) {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_COMPLETED)
            .apply()
    }
}
