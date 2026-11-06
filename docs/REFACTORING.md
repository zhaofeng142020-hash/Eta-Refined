# 重构说明与差异

本文说明 **Eta-Refined** 相对上游 [Mangi-11/Eta](https://github.com/Mangi-11/Eta) 做了哪些改动、为什么这样改、迁移成本与风险，以及如何保证功能不回归、如何控制与上游同步时的冲突。

> 一句话：**Eta-Refined 只重做 UI/UX 与工程化，不更换技术栈、不改业务逻辑。** 全部设备工具、Agent Runtime、系统适配与安全边界都继承自上游。

> ⚠️ **阅读前请注意**：本文同时包含**设计目标 / 迁移计划**与**当前已实现状态**，请勿把规划误读为已完成。当前状态：配色令牌层已全面生效，首启向导与统一错误卡已接线；圆角 / 间距 / 字号 / 动效令牌已在聊天输入条、工具标签、空状态、错误卡与首启向导等场景落地，其余现有界面仍在分批迁移中。详见文末第 6 节「当前实现状态（如实说明）」。

## 1. 技术栈与方案对照

| 维度 | 原版 Eta | Eta-Refined 新方案 |
| --- | --- | --- |
| 语言 / 运行时 | Kotlin + Jetpack Compose（Android） | 不变：Kotlin + Jetpack Compose |
| 构建体系 | Gradle（AGP + KSP） | 不变：Gradle |
| 视觉皮肤 | 以 Miuix 组件库提供的主题与视觉样式为主要外观 | 以 **Compose 自研的 Mist Glass 设计令牌层**承载视觉皮肤：**配色令牌已全面生效**（替换 Miuix 默认调色板），其余视觉参数在部分界面落地、分批迁移中 |
| 控件行为 | Miuix 组件行为与动画 | 保留既有控件行为与交互语义；视觉参数**已部分**改由 Mist Glass 令牌驱动（输入条 / 工具标签 / 空状态 / 错误卡 / 首启向导），其余界面分批迁移中 |
| 业务逻辑 | Agent Runtime、工具、系统适配 | **完全不变**（仅 UI 层改动） |
| 许可证 | PolyForm Noncommercial 1.0.0 | 不变：PolyForm Noncommercial 1.0.0 |

### 更换理由

- **统一视觉语言**：把散落在各处的颜色、圆角、间距、字号与动效收敛为一套设计令牌，避免“每个页面各写一套数值”。
- **降低对视觉皮肤库的绑定**：业务与系统能力本就与 UI 库无关，把“皮肤”抽成自研令牌层后，升级依赖或替换控件时不必整体重做界面。
- **可测试、可评审**：设计令牌是纯数据（`Color`、`Dp`、字号、时长、档位枚举），可单测、可在 PR 中逐项对比，比“截图对不齐”更容易治理。
- **多形态适配**：通过响应式断点与玻璃档位，让手机、折叠屏与外接大屏共用一套界面语义。

### 迁移成本与风险

| 项 | 评估 | 缓解措施 |
| --- | --- | --- |
| 工作量 | 中：规划范围为 `ui/theme2/`、`ui/onboarding/`、`ui/components/` 与核心 Screen | 按“令牌 → 组件 → 页面”分批迁移，逐屏验收（配色令牌、首启向导、错误卡与部分高频组件已落地，其余界面继续分批迁移） |
| 回归风险 | 中低：业务逻辑未改动，主要是样式与布局回归 | 保留原有交互语义与状态机；逐屏对比，覆盖深色/浅色与断点 |
| 依赖风险 | 低：同一 Compose 技术栈，仅替换视觉层 | 不改依赖坐标与构建脚本 |
| 上游同步成本 | 中：`ui/components/` 会与上游产生冲突 | 用同步保留区与冲突 PR 流程隔离（见第 5 节） |

## 2. Mist Glass 设计语言要点

Mist Glass 是一套基于“雾面玻璃 + 柔和分层”的 Compose 设计令牌层，规则如下。

### 颜色令牌

- 语义化分层：`background / surface / surfaceRaised / outline / onSurface / onSurfaceVariant / accent / danger / warning / success`。
- 玻璃层使用半透明表面色 + 发丝描边近似，**不启用真背景模糊**，**不直接写死十六进制**，页面只引用语义令牌。
- 强调色用于主操作与选中态；状态色（错误、警告、成功）保持独立语义，不参与品牌强调色。

### 圆角

统一使用 **6 / 10 / 14 / 20 / 28（dp）** 五档，外加**胶囊（`CircleShape` / 50% 圆角）**：

| 档位 | 用途 |
| --- | --- |
| 6 dp | 小徽标、标签、输入内嵌元素 |
| 10 dp | 小卡片、控件内层容器 |
| 14 dp | 常规卡片、列表项 |
| 20 dp | 大卡片、分组容器 |
| 28 dp | 弹层、抽屉、首启向导面板 |
| 胶囊 | 胶囊按钮、状态条、筛选 Chip |

### 间距

所有间距为 **4 的倍数**：`4 / 8 / 12 / 16 / 20 / 24 / 32 / 40`。页面外边距、卡片内边距、元素间隙均从该序列取值，禁止随意插入非 4 倍数值。

### 字号层级

| 层级 | 典型用途 |
| --- | --- |
| Display | 首启向导主标题、空状态大标题 |
| Headline | 页面标题 |
| Title | 卡片标题、分组标题 |
| Body | 正文、对话内容 |
| Label | 按钮、标签、辅助说明 |
| Caption | 时间戳、次要元信息 |

字号与行高成对定义，禁止只改字号不改行高。

### 动效时长

| 类型 | 时长 |
| --- | --- |
| 微交互（按压、选中） | ~100–150 ms |
| 常规过渡（展开、淡入） | ~200–300 ms |
| 页面级过渡（面板、向导） | ~300–450 ms |

统一使用缓动曲线（进入减速、退出加速），并遵循系统“减少动画”无障碍设置。

### 玻璃四档

玻璃效果按强度分为四档，由页面按层级选择，避免叠加过多模糊造成性能与可读性问题：

| 档位 | 说明 | 典型场景 |
| --- | --- | --- |
| Glass 0（无玻璃） | 纯色表面 | 正文列表、长文本阅读 |
| Glass 1（轻） | 低不透明度叠加 + 发丝描边 | 顶栏、底栏 |
| Glass 2（中） | 中等不透明度半透明表面 + 描边 | 卡片、分组容器 |
| Glass 3（重） | 高对比半透明表面 + 高光描边 | 弹层、向导面板、悬浮操作 |

> **实现说明**：当前玻璃效果以**半透明填充 + 发丝描边近似**，**并非真背景模糊**；上表四档是设计档位定义，用于约束不透明度、描边与层级，不代表已实现真实模糊。无法兑现的 `blurRadiusDp` 字段已从 `MistDimens.kt` 删除，相关注释已修正。

### 响应式断点

| 断点 | 宽度 | 布局策略 |
| --- | --- | --- |
| compact | < 600 dp | 单列、底部导航、全屏面板 |
| medium | 600–840 dp | 双栏或加宽单列、侧边辅助面板 |
| expanded | ≥ 840 dp | 多栏、常驻侧栏、内容最大宽度约束 |

同一页面在三档断点下共享同一套令牌与组件，仅调整布局骨架，不另写一套视觉。

## 3. 目录变更清单

### 新增目录

| 目录 | 说明 |
| --- | --- |
| `app/src/main/kotlin/io/github/mangi/eta/ui/theme2/` | Mist Glass 设计令牌层：颜色、圆角、间距、字号、动效、玻璃档位与响应式断点 |
| `app/src/main/kotlin/io/github/mangi/eta/ui/onboarding/` | 首启配置向导：模型选择、API Key / Codex / Grok 登录、权限说明与完成状态（**已接入启动路径**：首次启动标记 + 条件渲染 + 目标路由 + 可点击「跳过」） |

> 上表为新增目录。当前状态：`ui/theme2/` 的**配色令牌已全面接入** `AgentAppTheme`，尺寸 / 字号 / 动效 / 形状令牌已在输入条、工具标签、空状态、错误卡与首启向导等场景落地；`ui/onboarding/` 的向导**已接入启动路径**。其余现有界面仍在分批迁移中，详见文末第 6 节「当前实现状态（如实说明）」。

### 修改目录 / 文件

| 路径 | 改动 |
| --- | --- |
| `.../ui/components/` | **部分落地**：聊天输入条（`MistShapes.xl` 28dp）、ToolChip（`sm` 圆角 + 间距令牌）、空状态（`ListEmptyState` 委托 `MistEmptyState`）、错误卡、首启向导进度条（消费 `MistMotion` / `MistShapes`）已引用令牌；其余组件按「令牌 → 组件 → 页面」**分批迁移中**，保留原组件 API 与交互行为 |
| 核心 Screen（`.../ui/SettingsScreen.kt`、`.../ui/app/AgentAppShell.kt` 等） | **分批迁移中**：`SettingsScreen`、聊天气泡、顶栏等现有界面**尚未迁移**，后续逐步接入新令牌与响应式骨架，统一页面容器与间距 |
| `.../ui/app/AgentAppTheme.kt` | **已接入**：Mist Glass **配色令牌**入口，作为全局主题的事实源 |

> 改动范围仅限 UI 视觉与布局层（含后续规划）。`ui/markdown/`、`ui/layout/` 等既有能力继续复用，不改变其对外行为。

### 未改动（保证功能不回归）

以下目录与模块**保持上游原样**，不在本次重构范围内：

- `agent/`（Agent Runtime、工具、Provider、认证等）
- `hook/`（Xposed / Hook 相关）
- `core/`
- `systemizer/`
- `data/`
- `config/`
- `EtaApp`（应用入口装配）
- **全部测试**（单元测试与相关测试代码）

## 4. 功能不回归保证

1. **边界清晰**：改动集中在 `ui/theme2/`、`ui/onboarding/`、`AgentAppTheme.kt` 与部分高频 `ui/components/`（输入条、工具标签、空状态、错误卡）；核心 Screen 与其它现有界面为**后续分批迁移目标**。业务与服务层始终不改动。
2. **接口稳定（迁移原则）**：后续替换视觉参数时保持组件对外 API 与回调语义不变，避免调用方大改。
3. **行为优先**：交互状态机、权限检查、工具调用链路沿用上游实现；重构不引入新的业务分支。
4. **验证方式**：以“同一操作在改版前后结果一致”为验收标准，逐屏比对浅色/深色与三档断点；现有测试保持通过。
5. **可回退**：如出现无法接受的回归，可按上游同步策略回退对应提交，保留重构区的独立提交边界。

## 5. 上游同步与冲突控制策略

- **同步源唯一**：只同步 [Mangi-11/Eta](https://github.com/Mangi-11/Eta)，不涉及任何设计参考项目。
- **目标分支**：上游功能更新经 Pull Request 合入 `main`。
- **重构保留区**：`ui/theme2/`、`ui/onboarding/`、`README.md`、`docs/` 为本仓库的“重构保留区”。上游同步在这些路径发生冲突时，**优先保留 Eta-Refined 的实现**，再人工决定是否吸收上游变更。
- **冲突处理**：自动同步遇到冲突时创建 Issue 并开启冲突 PR，标注冲突文件与原因，由维护者裁决，**不静默覆盖**。
- **质量门禁**：同步 PR 必须通过构建与测试才可合并，失败不合并。

详细的同步频率、工作流与保留区清单见 [UPSTREAM_SYNC.md](UPSTREAM_SYNC.md)。

## 6. 当前实现状态（如实说明）

> 本节如实记录当前实现的接线与落地情况，区分「已接入 / 部分落地 / 分批迁移中」，避免把规划误读为已完成。

- **已真实完成**：
  - **配色令牌层**：`MistColors` / `RefinedUi` 已接入 `AgentAppTheme`，替换 Miuix 默认调色板，**颜色令牌全面生效**。
  - **首启配置向导**：已接入启动路径 —— `MainActivity` 条件渲染、`OnboardingState`（SharedPreferences 首次启动标记）、`AgentAppRoot` 目标路由导航（模型 / 外观 / 权限三步「去配置」直达），且「跳过」可点击。
- **部分落地（分批迁移中）**：
  - **设计令牌**：圆角 / 间距 / 字号 / 动效令牌已落地到**聊天输入条**（`MistShapes.xl` 28dp）、**ToolChip**（`sm` 圆角 + 间距令牌）、**空状态**（`ListEmptyState` 委托 `MistEmptyState`）、**错误卡**与**首启向导**（进度条动效消费 `MistMotion` / `MistShapes`）。**其余现有界面（`SettingsScreen`、聊天气泡、顶栏等）尚未迁移，仍为分批迁移中**。
  - **统一错误提示**：已新增 `MistErrorCard`（标题 + 修复建议 + 重试）并**接入会话加载失败场景**；全仓其余约 **35 处** `Toast.makeText` **尚未收敛，逐步迁移中**。
- **玻璃材质说明**：当前以**半透明填充 + 发丝描边近似**，**非真背景模糊**；无法兑现的 `blurRadiusDp` 已从 `MistDimens.kt` 删除，四档玻璃为设计档位定义（见第 2 节）。
- **功能不回归**：上游 302 个单元测试全部保留并通过；业务逻辑实现（Agent Runtime、设备工具、Hook 行为逻辑）未改语义。为支持独立包名，仅把 6 处「自身包名常量」由硬编码改为 `BuildConfig.REFINED_APPLICATION_ID`，并把 Manifest 权限/authority 改为 `${applicationId}` 占位符——这些是身份标识而非业务行为。
- **派生版身份适配（URL 常量 + 独立包名）**：
  - `data/update/AppUpdateChecker.kt`（应用内更新检查）与 `ui/SettingsScreen.kt`（关于页「反馈问题 / 项目主页」）原先指向 `Mangi-11/Eta`，已改为 Eta-Refined 仓库，避免推荐签名不同的上游 APK。
  - 从 `v3.4.0-refined2` 起通过 `applicationIdSuffix = ".refined"` 把最终 `applicationId` 改为 `io.github.mangi.eta.refined`（`namespace` 保持不变，Kotlin 源码目录、`R` 类均不用动）。改动位置：`app/build.gradle.kts`（suffix + `buildConfigField REFINED_APPLICATION_ID` + `versionCode/versionName`）、`AndroidManifest.xml`（权限名与 Provider authority 改用 `${applicationId}` 占位符，14 处相对组件类名展开为完整类名）、`core/ModuleConfig.kt`、`agent/accessibility/AccessibilityProtectionProtocol.kt`、`agent/runtime/AgentRuntimeWire.kt`、`agent/runtime/EntrySurfaceGuard.kt`、`agent/virtualdisplay/CanvasRoutingPolicy.kt`、`hook/system/AccessibilityServiceEnforcer.kt`（6 处硬编码包名改为 `BuildConfig.REFINED_APPLICATION_ID`）。
  - 效果：配合独立签名证书，Eta-Refined **可与原版 Eta 同时安装、数据隔离、互不覆盖**；安装不会替换原版。已列入[保留区清单](UPSTREAM_SYNC.md#6-重构保留区清单)，上游同步时需人工保留。
  - 代价：LSPosed 按包名识别模块，需在 LSPosed 中启用新模块并重新勾选作用域；默认数字助理、无障碍服务等系统级授权需针对新包名重新选择；`v3.4.0-refined1` 及更早版本的用户升级时属于全新安装，数据需导出/导入迁移。详见 [FAQ.md](FAQ.md)。
- **属于事实的部分**：来源声明、CI / 发布 / 上游同步机制（见 [UPSTREAM_SYNC.md](UPSTREAM_SYNC.md)）、文档与许可证均按仓库现状如实描述。
- **本轮新增：远程主机（协议实现 + 可配置，实验性）**：在不改动本机 Agent Runtime / 设备工具 / Hook 等既有业务逻辑的前提下，并列新增「远程主机」能力 —— `agent/remote/`、`data/remote/` 的协议与存储层，`ui/screens/remote/` 的设置页 / 主机切换胶囊 / 远程会话页，以及 `ui/navigation/AppRoute.kt`、`ui/app/AgentAppRoot.kt`、`ui/SettingsScreen.kt` 的**追加式**接线与三套 `values*/strings.xml` 文案；文档新增 [REMOTE_HOST.md](REMOTE_HOST.md) 并同步 README / FAQ / 上游同步保留区清单。其中 `agent/`、`data/` 下的新目录为**并列新增**，第 3 节「未改动」结论针对既有文件，二者不矛盾。本机 Eta Agent 与远程主机**并存、可切换**（未选远程主机时仍用本机 Agent）；两种连接方式为直连 `dsh web` HTTP 与 Relay（默认第三方托管 Server `https://dsh.r2049.cn`，可关闭或改自建地址）。**本轮仅交付协议实现 + 可配置，未做真机联调**，详见 [REMOTE_HOST.md](REMOTE_HOST.md)。

### 已知限制（如实记录）

1. **国际化缺口**：新增 UI（首启向导、错误卡）中的中文文案目前为 Kotlin 硬编码字符串，未加入 `values-en` / `values-zh-rTW` 的 `strings.xml`。在英文或繁体系统语言下，这些新界面仍显示简体中文；上游既有的三语言支持不受影响。后续应把这些文案迁移到 string 资源。
2. **向导不含输入控件**：首启向导是「说明 + 去配置跳转」的引导页，API Key / 模型选择仍在「设置 → 模型提供商」完成（与 dsh-Go 的「边解包边设置」思路一致），向导本身不提供输入框。
3. **玻璃材质为近似**：Compose 无 Flutter `BackdropFilter` 的等价物，用半透明填充 + 发丝描边实现，不是真背景模糊（见上文）。
4. **未新增单元测试**：新增的 9 个 Kotlin 组件文件目前没有配套单元测试；现有 302 个上游测试全部保留并通过。
5. **质量门禁的取舍**：仓库 ruleset 要求「必须经 PR 合入 main」，但**未启用必需状态检查**——因为 `GITHUB_TOKEN` 创建的上游同步 PR 不会自动触发 `PR Check`，启用会令同步 PR 永久卡住。上游同步工作流在**合并前的同步分支上**已跑 `lintDebug` / `testDebugUnitTest` / `assembleDebug`，失败时不开合并 PR；PR 页面上的检查需维护者手动 re-run（工作流会主动核对并留言提示）。
6. **共存模式需重新配置系统级能力**：LSPosed 按包名识别模块，改包名后需启用新模块并重新勾选作用域；默认数字助理、无障碍服务、通知使用权等也需针对新包名重新授予。若两版模块同时启用，Hook 可能重复加载，建议只启用一个。
7. **旧版升级不迁移数据**：`v3.4.0-refined1` 及更早版本使用旧包名 `io.github.mangi.eta`，升级到 `refined2` 属于全新安装，旧版数据不会自动迁移，需导出/导入备份。
8. **未做真机共存验证**：本机无 Android 设备/模拟器，「两版可同时安装」的结论基于包名、签名、权限名与 Provider authority 的静态核查（无冲突项），以及 CI 构建通过，未做实机安装验证。
9. **未验证项：远程连接未做真机联调**：本机无 Android 设备 / 模拟器，PC 端也未运行 `dsh web` 与 `ds-harness-remote` Host 插件，因此远程主机（直连与 Relay）**仅完成协议实现、单测与 CI 编译，未做真机联调，不宣称已连通**；WebRTC / LAN / P2P / TURN、SSH 主机、`/dsh-go/v1/*` 私有桥、DS Bot、工作区上传等均**未实现**。详见 [REMOTE_HOST.md](REMOTE_HOST.md)。

因此，本文第 1–4 节中「替换视觉皮肤」「统一视觉语言」「逐屏验收」等表述应理解为**设计目标与分批迁移计划**，而非「全部界面已完成」。
