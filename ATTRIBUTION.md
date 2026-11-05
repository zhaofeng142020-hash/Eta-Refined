# 来源与第三方致谢

本文记录 Eta-Refined 的原始来源、版权归属，以及随项目分发或参考的第三方组件与许可证。更细粒度的随包许可证全文见 [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md) 与 `app/src/main/assets/licenses/`。

## 1. 原版来源与版权

Eta-Refined **fork / 派生自 [Mangi-11/Eta](https://github.com/Mangi-11/Eta)**。

- **原作者**：Mangi-11（蛮吉）
- **原仓库**：<https://github.com/Mangi-11/Eta>
- **原始版权**：`Copyright © 2026 蛮吉 (Mangi-11).`
- **原始许可证**：PolyForm Noncommercial License 1.0.0

Eta-Refined 仅重做 **UI/UX 与工程化**（Mist Glass 设计令牌层、首启配置向导、统一错误提示、文档与发布流程），**业务功能与核心实现继承自原版**。除 UI/UX 与工程化改动外，Eta-Refined 不对原版功能主张新的作者身份。

## 2. 本项目的派生说明

- 本仓库是 Eta 的非官方派生版本，与原作者不构成隶属、赞助或背书关系。
- 派生版本的界面设计语言参考了 dsh-Go 项目（见第 3 节），**仅参考设计思路，不复制其代码**。
- 派生版本沿用原项目许可证（PolyForm Noncommercial 1.0.0），并保留原始版权与 Required Notice。

## 3. 设计参考（仅参考，不复制代码）

| 项目 | 作者 / 来源 | 许可证 | 说明 |
| --- | --- | --- | --- |
| [dsh-Go](https://github.com/ccch1mneyyy/dsh-Go) | ccch1mneyyy | AGPL-3.0 | UI/UX 设计语言参考。**仅参考配色、层级与交互思路，不复制任何源代码**；Eta-Refined 不同步、不合并该项目代码。 |

## 4. 第三方组件致谢

下列组件在 Eta-Refined 中被使用或因参考而致谢。具体版本以 `gradle/libs.versions.toml` 为准。

### Miuix

- **来源**：<https://github.com/compose-miuix-ui/miuix>
- **许可证**：Apache License 2.0
- **说明**：原版 Eta 的界面组件库。Eta-Refined 以自研 Mist Glass 令牌层替换其视觉皮肤，控件行为与既有依赖仍保留其许可权利。

### JetBrains Mono

- **来源**：<https://github.com/JetBrains/JetBrainsMono>
- **许可证**：SIL Open Font License 1.1（OFL）
- **版权**：The JetBrains Mono Project Authors
- **说明**：终端界面使用的无连字版本（Regular、Bold），以字体资源随 APK 分发。

### intellij-markdown

- **来源**：<https://github.com/JetBrains/markdown>（`org.jetbrains:markdown`）
- **许可证**：Apache License 2.0
- **说明**：GFM 解析器；聊天渲染层由 `ui/markdown` 自建，按块冻结并接入逐字显现。

### libxposed API

- **来源**：<https://github.com/libxposed/api>
- **许可证**：Apache License 2.0
- **说明**：现代 Xposed API，用于模块与系统 Hook 集成。

### 其他随包第三方组件

| 组件 | 来源 | 许可证 |
| --- | --- | --- |
| PRoot / talloc / libandroid-shmem | termux 等 | GPL-2.0-or-later / LGPL-3.0-or-later / BSD-3-Clause（组合 PRoot 可执行程序按 GPL-3.0-or-later 分发） |
| jsoup | <https://jsoup.org/> | MIT |
| Nimbus JOSE + JWT | Connect2id Ltd. | Apache License 2.0 |
| DexKit | <https://github.com/LuckyPray/DexKit> | Apache License 2.0 / LGPL v3（`Core/`） |
| Material Icons | AndroidX / Google | Apache License 2.0 |
| Lobe Icons | LobeHub | MIT |
| Android Hidden API Bypass | LSPosed | Apache License 2.0 |
| models.dev 模型目录 | models.dev | MIT |

OpenAI、Anthropic、Claude、阿里云百炼、Qwen、DeepSeek、Kimi、Xiaomi MiMo、MiniMax、StepFun、Z.ai、ChatGLM、Gemini、Gemma、Grok、Meta、Mistral、豆包、混元、Yi、硅基流动和 OpenRouter 等名称、图标及品牌标识归各自权利人所有；本项目展示这些图标仅用于准确标识用户正在配置的模型服务，不代表任何赞助、认可或合作关系。

> 上述第三方组件各自保留其原始许可证权利，**不受本项目非商业许可证的附加限制**。完整许可证文本见 [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md)。

## 5. 许可证与 Required Notice

Eta-Refined 采用 **[PolyForm Noncommercial License 1.0.0](LICENSE)**（<https://polyformproject.org/licenses/noncommercial/1.0.0>）。

依据该许可证的 Notices 条款，任何从本仓库获得软件副本的人都必须同时获得许可证条款（或上述 URL）以及下列 `Required Notice` 纯文本：

```text
Required Notice: Copyright © 2026 蛮吉 (Mangi-11).
```

详见 [NOTICE](NOTICE) 与 [LICENSE](LICENSE)。
