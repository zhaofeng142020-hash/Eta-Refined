/*
 * Copyright 2026, Eta-Refined contributors.
 * SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
 *
 * 首启配置向导（Mist Glass 风格）：把「下载→配置→可用」压缩为几步。
 * 纯 UI 组件，通过回调把动作交给宿主（打开 Provider / 外观 / 权限设置并最终 onFinish）。
 * 接入方式见 docs/REFACTORING.md「首启向导接入」。
 */
package io.github.mangi.eta.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.mangi.eta.ui.theme2.MistMotion
import io.github.mangi.eta.ui.theme2.MistShapes
import io.github.mangi.eta.ui.theme2.MistSpace
import io.github.mangi.eta.ui.theme2.MistType
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 向导一步的静态描述。 */
private data class OnboardingStep(
    val eyebrow: String,
    val title: String,
    val body: String,
    val actionLabel: String? = null,
)

private val steps = listOf(
    OnboardingStep(
        eyebrow = "欢迎使用 Eta-Refined",
        title = "把 Agent 装进手机",
        body = "Eta 是一个系统级 Android AI Agent，能读写文件、执行命令、操作界内外应用。接下来 3 步完成配置即可开始对话。",
    ),
    OnboardingStep(
        eyebrow = "第 1 步 / 模型",
        title = "配置模型提供商",
        body = "填入自备模型的 API Key（BYOK），或登录 Codex / Grok 订阅账号。执行任务需要模型支持 Tool Calling。",
        actionLabel = "去配置模型提供商",
    ),
    OnboardingStep(
        eyebrow = "第 2 步 / 外观",
        title = "选择外观",
        body = "默认「雾蓝」低饱和护眼配色并跟随系统明暗。可随时在设置中切换强调色、圆角风格与动效强度。",
        actionLabel = "去调整外观",
    ),
    OnboardingStep(
        eyebrow = "第 3 步 / 权限",
        title = "按需授予权限",
        body = "GUI Agent 需要无障碍服务；日历、通知、位置等按任务授予。普通设备即可使用核心能力，Root / LSPosed 可解锁更多系统操作。",
        actionLabel = "去授权",
    ),
    OnboardingStep(
        eyebrow = "准备就绪",
        title = "开始使用 Eta",
        body = "配置完成。之后的常见操作都集中在首页与设置，随时可回到本向导或在设置中调整。",
    ),
)

/**
 * 首启配置向导。
 *
 * @param onFinish 完成引导。
 * @param onOpenProviderSettings 跳转模型提供商设置。
 * @param onOpenAppearanceSettings 跳转外观设置。
 * @param onRequestPermissions 跳转权限授予。
 * @param onSkip 跳过（使用默认配置）。
 */
@Composable
fun MistOnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenProviderSettings: () -> Unit = {},
    onOpenAppearanceSettings: () -> Unit = {},
    onRequestPermissions: () -> Unit = {},
    onSkip: (() -> Unit)? = null,
) {
    var index by remember { mutableIntStateOf(0) }
    val step = steps[index]
    val colors = MiuixTheme.colorScheme
    val isLast = index == steps.lastIndex

    // 进度条：使用 Mist Glass 动效令牌，切换步骤时给出即时视觉反馈。
    val progress by animateFloatAsState(
        targetValue = (index + 1f) / steps.size,
        animationSpec = tween(durationMillis = MistMotion.standardMs, easing = MistMotion.std),
    )

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = MistSpace.xxl, vertical = MistSpace.xxxl),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MistSpace.lg)) {
            // 字标
            Text(
                text = "Eta",
                style = MistType.wordmark,
                color = colors.primary,
            )
            Spacer(Modifier.height(MistSpace.xxxl))
            Text(
                text = step.eyebrow,
                style = MistType.eyebrow,
                color = colors.onSurfaceVariantSummary,
            )
            Text(
                text = step.title,
                style = MistType.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
            Text(
                text = step.body,
                style = MistType.bodyLarge,
                color = colors.onSurfaceVariantSummary,
            )
            step.actionLabel?.let { label ->
                val handler = when (index) {
                    1 -> onOpenProviderSettings
                    2 -> onOpenAppearanceSettings
                    else -> onRequestPermissions
                }
                Spacer(Modifier.height(MistSpace.xs))
                TextButton(
                    text = label,
                    onClick = handler,
                    colors = ButtonDefaults.textButtonColorsPrimary(color = colors.primary),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(MistSpace.lg)) {
            // 细线进度条：层次靠细线与材质，不使用阴影。
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(MistSpace.xxs)
                    .clip(MistShapes.xs)
                    .background(colors.outline.copy(alpha = 0.35f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(MistSpace.xxs)
                        .clip(MistShapes.xs)
                        .background(colors.primary),
                )
            }
            // 进度点
            Row(horizontalArrangement = Arrangement.spacedBy(MistSpace.sm)) {
                steps.indices.forEach { i ->
                    Box(
                        Modifier
                            .size(if (i == index) 9.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (i == index) colors.primary else colors.outline),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MistSpace.md), verticalAlignment = Alignment.CenterVertically) {
                if (index > 0) {
                    TextButton(
                        text = "上一步",
                        onClick = { index -= 1 },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.width(MistSpace.xs))
                TextButton(
                    text = if (isLast) "开始使用" else "下一步",
                    onClick = {
                        if (isLast) onFinish() else index += 1
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(color = colors.primary),
                )
            }
            onSkip?.let { skip ->
                TextButton(
                    text = "跳过，使用默认配置",
                    onClick = skip,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
