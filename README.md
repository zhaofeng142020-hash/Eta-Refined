<h1 align="center">Eta-Refined</h1>

<p align="center"><strong>简体中文</strong> | <a href="README_EN.md">English</a></p>

<p align="center"><strong>基于 Mangi-11/Eta 重构界面与体验的 Android 系统级 AI 助手</strong></p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-13%2B%20%7C%20arm64-3DDC84?logo=android&logoColor=white" alt="Android 13+ | arm64">
  <img src="https://img.shields.io/badge/License-PolyForm%20Noncommercial%201.0.0-1677FF" alt="PolyForm Noncommercial 1.0.0">
  <img src="https://img.shields.io/badge/Star-Eta--Refined-1677FF?logo=github" alt="Star Eta-Refined">
</p>

> **来源声明**
>
> 本仓库 **fork / 派生自 [Mangi-11/Eta](https://github.com/Mangi-11/Eta)**，原作者为 **Mangi-11（蛮吉）**，原仓库地址：<https://github.com/Mangi-11/Eta>。
>
> **Eta-Refined 仅重做 UI/UX 与工程化（设计语言、首启向导、错误提示、文档与发布流程），完整保留原版业务功能。** 它不改变 Agent Runtime、设备工具、系统适配等核心实现逻辑。原项目与衍生版均采用 [PolyForm Noncommercial License 1.0.0](LICENSE)；再次分发时请遵守其 Required Notice 要求（见[许可证](#许可证)）。

Eta 是为手机和移动设备设计的 AI Agent，结合了 [Codex](https://openai.com/codex/) 这类 Coding Agent 自主规划与执行任务的能力，以及[豆包手机助手](https://o.doubao.com/)所展示的 GUI Agent 跨应用操作方式。它可以处理文件、执行命令、编写代码，也能深入 Android 系统与厂商应用，直接获取个人上下文、调用系统与一方应用的原生接口，目标是提供真正的**系统级 AI 助手体验**。

支持 **Android 13 及以上版本的 ARM64 设备**，App 本体不限手机品牌，基础功能无需 Root。Root 和 LSPosed 可进一步开放深层个人上下文、一方应用接口与系统助手入口，具体能力取决于授权和 ROM 适配。

[下载 APK](https://github.com/zhaofeng142020-hash/Eta-Refined/releases) · [快速开始](#快速开始) · [重构说明](#重构说明与差异) · [常见问题](docs/FAQ.md)

## 快速开始

1. **下载**：前往 [Releases](https://github.com/zhaofeng142020-hash/Eta-Refined/releases) 下载 `Eta-Refined` 的 APK。
2. **安装**：在 Android 13+ ARM64 设备上安装 APK（首次安装建议允许来源安装）。
3. **配置并对话**：首次启动进入**配置向导**，按引导前往「模型提供商」填写 **API Key**（或登录 **Codex / Grok** 订阅账号）并选择默认模型，即可开始对话；也可跳过向导，稍后在「设置 → 模型提供商」中配置。

> 执行任务需要模型支持 **Tool Calling**，理解图片还需要模型支持图片输入。没有配置模型前，聊天与系统助手入口无法工作。

## 能做什么

Eta-Refined 沿用原版 Eta 的能力集合，下面按“执行工具”“上下文与扩展”“Agent Runtime”与“系统入口”分层说明。

### 执行工具

- **系统与一方应用操作**：直接调用 Android API 与一方应用接口，读写日历日程（单条或批量创建、修改、删除）、管理闹钟与计时器、创建和回收系统便签；控制手电筒、亮度、息屏时间、响铃与勿扰、护眼与深色模式、音量、媒体播放，以及 Wi‑Fi、蓝牙、移动数据、个人热点、NFC、定位和省电模式。也可查询应用版本、安装来源与权限，停止或冻结应用，在 Root 授权下筛选系统日志。设置、开关和一方应用写操作都会读回实际状态，未确认时明确报告，不盲目重复执行。
- **GUI Agent 与后台虚拟屏**：结合无障碍 UI 树、控件定位与按需截图，执行点击、长按、滑动、滚动和文本输入；动作成功后自动附带新界面的节点快照，截图按原始尺寸与像素传递，不在上传前缩放或转 JPEG。支持物理主屏与后台虚拟屏智能分流：目标应用已在主屏前台时直接原地操作；跨应用任务在具备 Root 或 LSPosed 时默认于后台受信任虚拟屏静默执行，不打断用户当前主屏操作。虚拟屏运行期间顶部胶囊可展开实时取景，模型也可调用 `present_to_user` 将虚拟屏内的应用原位无缝移交回物理主屏。
- **运行通知**：Android 16 及以上且支持 Live Updates 的系统可在状态栏胶囊与锁屏显示 Agent 任务状态，纯后台任务同样适用；具体展示取决于系统支持和通知设置，部分系统映射到流体云或超级岛。
- **网页搜索与内置浏览器**：提供轻量的公开网页搜索与静态正文分页读取，可在设置中切换内置默认、DeepSeek Search / Fetch 或自定义 HTTP 接口；动态 JavaScript 页面通过内置 WebView 加载，支持读取正文、操作 DOM 与截图，用户可随时打开同一浏览器会话接管。
- **终端**：Android user/root Shell、Alpine / Debian Linux 与脚本执行，支持会话、异步后台命令和常驻守护任务；返回实际 Shell、执行身份与可解析命令的信息。
- **文件工具**：读取、写入、精确编辑、元数据查询、目录分页、路径匹配、文本搜索与本机图片读取。文本读取按 UTF-8 字节边界续读，编辑检查旧内容，搜索结果说明实际范围与未完成部分。

### 上下文与扩展

- **个人上下文**：通过结构化只读工具按需获取本机与厂商 ROM 数据。普通授权下可读取时间与时区、最近位置、设备环境、当前通知与最近 7 天通知历史、最近打开应用与前台使用时长，以及日历日程。Root 授权下进一步开放通讯录、通话记录、短信与验证码提取、相册图片（支持按文件名，或通过内容索引按图片 OCR 文字、画面描述与标签检索）、音频与录音、共享文件与下载记录、输入法剪贴板历史、Health Connect 健康汇总、已保存的 Wi‑Fi 密码及微信/QQ 聊天图片缓存。在 ColorOS 上，还可通过小布记忆进程内 Hook 桥与系统 DMP 索引直接检索小布记忆、便签、通话与普通录音摘要，以及账单与收支汇总、航班/火车/酒店行程、外卖快递订单、待办线索、生活事件和收藏地点。
- **长期记忆**：使用本机 `MEMORY.md` 保存跨对话背景，核心内容按预算加入上下文，其余按需读取；支持编辑、清空和关闭。
- **Subagent**：支持在独立子上下文中派发并并发运行多个 Subagent（内置 `explore`、`coder`、`plan`），可单独配置轻量/高速模型并在聊天时间线中实时检视内部步骤。
- **Skills**：按需加载任务方法、参考资料与脚本资源，支持公开 GitHub 仓库安装和本地 ZIP 导入；安装不会执行脚本或开启额外权限。
- **MCP**：通过 Streamable HTTP 连接远程工具，支持 Bearer Token；工具逐项启用，与本机工具共同参与任务。

### Agent Runtime 与可选角色

Agent Runtime 运行在 Eta App 内，来自聊天页面和系统助手的请求共用同一个 Agent Loop。模型通过 Tool Calling 选择工具，执行结果回到上下文，再决定下一步。Runtime 同时管理流式事件、steering、取消和增量 transcript。会话与结果在本机归档；中断后尝试恢复已有记录，不自动重放操作。

侧边栏的“角色”可管理、导入和导出兼容酒馆的 PNG / JSON 角色卡，并从角色开始对话。角色会话仍能调用 Eta 的手机工具，支持用户人设、内嵌世界书和独立剧情记忆；普通对话及系统助手入口保持默认 Eta。

## 系统助手入口

Eta 自身实现了完整的 Android 原生数字助理服务（`VoiceInteractionService`），可作为系统默认数字助理运行，长按电源键直接唤醒 Eta 本体执行任务：

- **Eta 原生数字助理**：从电源键直接唤起 Eta 本体全局浮窗，唤醒时由系统直接提供当前屏幕截图与应用内容。支持语音提问、切换键盘输入或点击建议提问，上拉浮窗即可回到 Eta 主界面接管当前会话。语音识别与语音播报可独立配置。
- **长按电源键路由**：在设置中选择长按电源键唤起 Eta 本体、Gemini 或系统默认助手。
- **小布 / 超级小爱接管**：习惯使用厂商原有入口时，也可保留小布或超级小爱的界面，将请求交由 Eta 本体与自选模型处理。
- **Gemini 与一圈即搜**：在受支持的系统上补齐 Gemini 系统助手能力并解锁一圈即搜。

在锁死默认数字助理或电源键逻辑的 ROM 上，电源键路由与厂商助手接管需要 LSPosed 与对应系统支持，部分功能另需 Root。

## 模型与 BYOK

使用 Eta 的 AI 功能支持自备模型服务的 **API Key（BYOK）**，也支持 **Codex（ChatGPT）与 Grok 订阅账号登录**。内置 OpenAI、Anthropic、阿里百炼、DeepSeek、Kimi、MiMo、MiniMax、StepFun、硅基流动、OpenRouter 和 xAI 等提供商配置，也可添加自定义服务。

内置 OpenAI 与 xAI 提供商可在填写 API Key 和订阅账号登录之间切换：Codex 通过 PKCE 授权，Grok 通过设备码授权，均由系统浏览器完成登录。OAuth 凭据经 Android Keystore 加密保存在本机不可备份目录，不进入跨进程通信或备份文件。Provider 层支持 OpenAI-compatible Chat Completions、Responses API、Anthropic Messages 和 Gemini 原生 Interactions API，包括 SSE、Tool Calling、图片输入与推理内容。

## 权限与数据边界

系统工具、敏感读取、敏感操作、终端与文件、网页浏览、记忆均有独立开关，当前默认开启。Runtime 在执行前重新检查权限，撤权或断连不会覆盖已保存的配置。

- **能力边界**：普通设备可使用聊天、网页搜索与内置浏览器、记忆、Skills、MCP、普通终端与私有工作区；日历读写、GUI 和本机通知/位置/使用情况读取按需授权。Root 设备进一步开放后台虚拟屏执行、系统设置与开关修改、应用管理、受保护文件、深度个人数据检索与一方应用原生操作。LSPosed 与适配 ROM 开放厂商助手接管、系统快捷入口与 Google 能力增强，部分功能另需 Root。
- **数据去向**：任务所需的对话、图片和工具结果会发送给配置的模型服务；本地 Runtime 不代表本地推理。自定义 HTTP 地址会明文传输 API Key 与请求内容。
- **本机记录**：敏感工具及 MCP 的原始参数、结果不写入持久会话，模型回复仍会保存。通知历史在授权后保存最近 7 天、最多 1000 条；MCP 认证令牌、网页工具独立密钥与提供商 OAuth 凭据均通过 Android Keystore 加密保存。
- **运行边界**：任务可停止或接管。后台运行受 Android 与厂商进程管理影响，强停或重启后需手动启动；系统与应用更新也可能需要重新适配 Hook。

完整条件与验证范围见[设备支持说明](docs/ROOTLESS_SUPPORT.md)。

## 重构说明与差异

Eta-Refined 与原版 Eta 的差异集中在**视觉与交互层**，不改动业务逻辑与系统适配：

- **Mist Glass 设计令牌层（自研）**：在 Kotlin + Jetpack Compose 技术栈内引入统一的设计令牌层，**颜色令牌已全面生效**并替换 Miuix 默认调色板；圆角 / 间距 / 字号 / 动效已在聊天输入条、工具标签、空状态、错误卡与首启向导等部分界面落地，其余界面分批迁移中，形成可复用、可测试的设计系统。
- **首启配置向导**：新增首次启动引导流程并**已接入启动路径**（首次启动标记 + 条件渲染 + 模型 / 外观 / 权限三步「去配置」直达 + 可点击「跳过」），把“模型选择 → API Key / Codex / Grok 登录 → 权限说明”收敛到一条可完成的路径，降低上手门槛。
- **统一错误提示（逐步迁移）**：已引入统一错误卡（标题 + 修复建议 + 重试）并接入会话加载失败场景；其余约 20+ 处 Toast 尚未收敛，逐步迁移中。
- **文档重构**：重写 README、补充 FAQ、重构说明与上游同步说明，面向用户与协作者分层组织。
- **工程化**：保留原构建体系，新增设计密码、发布与同步工作流的说明文档。

设计令牌细节（颜色、圆角 6/10/14/20/28+胶囊、4 倍数间距、字号层级、动效时长、玻璃四档、响应式断点 compact<600 / medium 600–840 / expanded≥840）、迁移成本与风险、功能不回归保证，详见 **[docs/REFACTORING.md](docs/REFACTORING.md)**。

## 上游同步

Eta-Refined 的功能更新跟随上游 **[Mangi-11/Eta](https://github.com/Mangi-11/Eta)**：

- **每日自动同步**：定时任务每日从上游拉取功能更新，经 Pull Request 合入 `main`。
- **冲突不静默覆盖**：发生冲突时自动创建 Issue 并开启冲突 PR，标注冲突文件与原因，由维护者裁决；本仓库保留区（`ui/theme2/`、`ui/onboarding/`、`README.md`、`docs/`）优先保留重构成果。
- **质量门禁**：构建或测试失败不合并。

同步源与流程细节见 **[docs/UPSTREAM_SYNC.md](docs/UPSTREAM_SYNC.md)**。上游同步仅涉及 Mangi-11/Eta，绝不涉及任何设计参考项目。

## 发布

- **正式发布**：推送 `v*` 标签自动触发正式版构建与发布流程。
- **每夜构建（nightly）**：合并到 `main` 后产出的自动构建，用于持续验证。
- **手动触发**：支持在 Actions 中手动运行构建。

发布流程、签名配置与版本号规则详见 **[.github/RELEASING-REFINED.md](.github/RELEASING-REFINED.md)**。

## 常见问题

安装失败、签名冲突、API Key 配置、Root / LSPosed 权限、上游同步、如何升级与如何回退原版等问题，见 **[docs/FAQ.md](docs/FAQ.md)**。

## 深入了解

- [重构说明与差异](docs/REFACTORING.md)：技术栈取舍、Mist Glass 设计令牌、目录变更与风险控制。
- [上游同步说明](docs/UPSTREAM_SYNC.md)：同步源、分支策略、冲突处理与保留区。
- [常见问题](docs/FAQ.md)：安装、配置、权限、升级与回退。
- [设备支持与权限边界](docs/ROOTLESS_SUPPORT.md)：普通设备、Root、文件工作区与后台运行。
- [技术实现](docs/TECHNICAL.md)：设备工具、数据检索、浏览器、终端与系统集成。
- [Agent Runtime](docs/AGENT_RUNTIME.md)：Agent Loop、Provider、steering、transcript 与结果恢复。
- [第三方声明](docs/THIRD_PARTY_NOTICES.md)：随包分发的第三方组件与许可证。

## 参考与致谢

- **[Mangi-11/Eta](https://github.com/Mangi-11/Eta)**：本项目的上游与来源，原作者 Mangi-11（蛮吉），Eta-Refined 的全部业务能力来自该项目。
- **[dsh-Go](https://github.com/ccch1mneyyy/dsh-Go)**：UI/UX 设计语言参考（仅参考设计思路，不复制代码；该项目采用 AGPL-3.0）。
- [Pi Coding Agent](https://github.com/earendil-works/pi)：Eta Agent Runtime 的核心参考。
- [OmniBot](https://github.com/omnimind-ai/OmniBot)：Android AI Agent 方向的参考项目。
- [libxposed API](https://github.com/libxposed/api)：现代 Xposed API。
- [Miuix](https://github.com/compose-miuix-ui/miuix)：原版界面组件库。
- 完整的第三方组件致谢见 **[ATTRIBUTION.md](ATTRIBUTION.md)** 与 [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md)。

## Star History

<p align="center">
  <a href="https://www.star-history.com/#zhaofeng142020-hash/Eta-Refined&amp;Date">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=zhaofeng142020-hash/Eta-Refined&amp;type=Date&amp;theme=dark">
      <img src="https://api.star-history.com/svg?repos=zhaofeng142020-hash/Eta-Refined&amp;type=Date" alt="Eta-Refined Star History" width="800">
    </picture>
  </a>
</p>

## 许可证

Eta-Refined 与原版 Eta 一样，采用 **[PolyForm Noncommercial License 1.0.0](LICENSE)**。该许可证允许为**非商业目的**复制、分发本软件（含修改版与派生作品），但必须随附许可证文本或链接，以及 LICENSE 中列出的 `Required Notice` 版权声明。任何商业使用（贩卖、收费代装、商业集成等）不在授权范围内，需另行获得[原作者](https://github.com/Mangi-11)授权。完整条款以 [LICENSE](LICENSE) 为准。

原始版权归原作者所有：

```text
Required Notice: Copyright © 2026 蛮吉 (Mangi-11).
```

重新分发本软件或其衍生作品时，必须随附本许可证条款（或许可证 URL）以及上述 `Required Notice` 文本。原始版权、派生说明与第三方组件声明见 [ATTRIBUTION.md](ATTRIBUTION.md) 与 [NOTICE](NOTICE)。

<sub>社区：<a href="https://linux.do">LINUX DO</a></sub>
