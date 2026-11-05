/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 友好错误卡：统一呈现「发生了什么 / 为什么 / 怎么修」，并提供重试与关闭操作。
 * 用于替代零散、只报错不给出路的 Toast。接入方式见 docs/REFACTORING.md。
 */
package io.github.mangi.eta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.ui.theme2.MistGlassTier
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * @param title 一句话说明发生了什么，例如「模型连接失败」。
 * @param message 具体错误信息（可包含服务端返回文本）。
 * @param fixHint 可操作的建议，例如「检查 API Key 是否正确」。
 * @param onRetry 重试回调，为空则不显示重试按钮。
 * @param onDismiss 关闭回调，为空则不显示关闭按钮。
 */
@Composable
internal fun MistErrorCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    fixHint: String? = null,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "重试",
    onDismiss: (() -> Unit)? = null,
    dismissLabel: String = "知道了",
) {
    // 使用当前 colorScheme 的 error / onError：自动适配应用强制明暗与 Monet 动态取色，
    // 避免固定语义色与主题前景色混搭导致对比度不足。
    val colors = MiuixTheme.colorScheme
    val danger = colors.error
    MistGlassSurface(tier = MistGlassTier.card, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            // 左侧警示竖条，替代图标以降低依赖。
            Box(
                Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .background(danger, RoundedCornerShape(2.dp)),
            )
            Column(
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    color = danger,
                )
                Text(
                    text = message,
                    style = MiuixTheme.textStyles.body2,
                    color = colors.onSurfaceVariantSummary,
                )
                fixHint?.let {
                    Text(
                        text = "建议：$it",
                        style = MiuixTheme.textStyles.body2,
                        color = colors.onSurfaceVariantSummary,
                    )
                }
                if (onRetry != null || onDismiss != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        onRetry?.let {
                            TextButton(
                                text = retryLabel,
                                onClick = it,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.textButtonColorsPrimary(
                                    color = danger,
                                    textColor = colors.onError,
                                ),
                            )
                        }
                        onDismiss?.let {
                            TextButton(
                                text = dismissLabel,
                                onClick = it,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
