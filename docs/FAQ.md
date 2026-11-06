# 常见问题（FAQ）

本文汇总 Eta-Refined 使用中最常见的问题。若问题不在此列，请到仓库 Issues 反馈。

## 安装与升级

### Q1：安装失败 / 提示“应用未安装”怎么办？

请依次排查：

1. **系统版本与架构**：Eta-Refined 要求 **Android 13 及以上、ARM64（arm64-v8a）** 设备。32 位 ARM、x86 或 x86_64 不受支持。
2. **存储空间**：确认剩余空间足够（含安装与解压）。
3. **来源安装权限**：首次安装需允许“安装未知来源应用”。
4. **旧版 Eta-Refined 包名不同**：`v3.4.0-refined1` 及更早版本使用 `io.github.mangi.eta`，从 `v3.4.0-refined2` 起改为 `io.github.mangi.eta.refined`，两者是不同应用（见下方「与原版共存说明」）。

### Q2：会和原版 Eta 冲突吗？提示 `INSTALL_FAILED_UPDATE_INCOMPATIBLE` 怎么办？

**从 `v3.4.0-refined2` 起不会冲突**。Eta-Refined 使用独立签名证书，且 `applicationId` 已改为 `io.github.mangi.eta.refined`（原版为 `io.github.mangi.eta`）。包名不同 = 两个独立应用，可以**同时安装、数据互不干扰、互不覆盖**。直接安装即可，无需卸载原版。

签名冲突只可能出现在以下场景：

1. **从 `v3.4.0-refined1` 或更早版本升级**：旧版包名是 `io.github.mangi.eta`，新版是 `io.github.mangi.eta.refined`，属于「安装一个新应用」而非覆盖升级。旧版会保留在设备上（可自行卸载），其数据**不会自动迁移**到新版——请先在旧版中导出备份，安装新版后导入恢复。
2. **安装了别人用其他证书签名的同包名 APK**（例如自行构建的 `refined` 版本）：此时因签名不同无法覆盖，需先卸载那个包再安装。

> 若你需要从 Eta-Refined **回退到原版**，由于包名不同，原版本来就可以直接安装/保留，无需卸载 Eta-Refined；见 Q13。

### Q3：如何升级到新版本？

- 从 [Releases](https://github.com/zhaofeng142020-hash/Eta-Refined/releases) 下载新的 Eta-Refined APK，直接覆盖安装即可（**相同包名 + 相同签名**时保留数据）。
- 若是从 `v3.4.0-refined1` 及更早版本升级，包名已变化，属于全新安装，数据需导出/导入迁移（见 Q2）。
- 升级前建议先导出备份，以防异常。

### 与原版共存说明（包名、签名与系统级配置）

从 `v3.4.0-refined2` 起：

- **包名**：`io.github.mangi.eta.refined`（通过 `applicationIdSuffix = ".refined"` 实现），Kotlin `namespace` 仍为 `io.github.mangi.eta`，源码与资源不受影响。
- **签名**：使用本仓库新建的独立发布证书，与原版不同。
- **效果**：可与原版 Eta 同时安装，两者数据目录、SharedPreferences、数据库完全隔离；安装 Eta-Refined **不会替换或清除原版**。
- **需要重新配置的系统级能力**（因为它们按包名识别应用）：
  - **LSPosed / Xposed**：LSPosed 按包名识别模块，需在 LSPosed 中启用 **Eta-Refined** 这个新模块并重新勾选作用域。若同时启用原版模块，两套 Hook 可能重复加载，**建议只启用一个**。
  - **系统默认数字助理 / 长按电源键路由**：需在系统设置中重新选择 Eta-Refined 作为默认数字助理。
  - **无障碍服务、通知使用权、电池优化白名单**等：需在系统设置中针对新包名重新授予（见 Q14）。
  - **应用内更新检查**：已指向本仓库 Releases，不会推荐原版 APK。
- **避免版本混淆**：`versionName` 带 `-refinedN` 后缀，与原版区分；请始终以本仓库 [Releases](https://github.com/zhaofeng142020-hash/Eta-Refined/releases) 作为下载来源。

## 模型与 API Key

### Q4：第一次启动怎么配置模型？

首次启动会进入**配置向导**：

1. 选择模型提供商（内置 OpenAI、Anthropic、阿里百炼、DeepSeek、Kimi、MiMo、MiniMax、StepFun、硅基流动、OpenRouter、xAI 等，也可自定义）。
2. 填写该服务的 **API Key**；或选择 **Codex（ChatGPT）/ Grok 订阅账号登录**（系统浏览器完成授权）。
3. 选择默认模型并保存，即可开始对话。

> 执行任务需要模型支持 **Tool Calling**；理解图片还需要模型支持**图片输入**。

### Q5：API Key 填了但仍然报错？

- 检查 Key 是否正确、是否已过期或被撤销。
- 检查服务地址（自定义服务需确认 Endpoint 与请求头）。
- 检查网络与“局域网访问”权限（访问局域网服务时需要）。
- “测试连接”使用**尚未保存**的配置，保存后请再发一条消息验证。
- 若提供商未返回输入用量，自动压缩可能不触发，可手动压缩。

### Q6：API Key 会被上传或备份吗？

- 任务所需的对话、图片和工具结果会发送给**你配置的模型服务**；本地 Runtime 不代表本地推理。
- 自定义 HTTP 地址会**明文**传输 API Key 与请求内容，请仅使用可信端点。
- MCP 认证令牌、网页工具独立密钥与提供商 OAuth 凭据通过 **Android Keystore 加密**保存在本机不可备份目录；整体备份包含 API Key，但**不包含**订阅登录的 OAuth 令牌。

## 权限：Root 与 LSPosed

### Q7：必须 Root 吗？

**不需要。** 普通设备即可使用聊天、网页搜索与内置浏览器、记忆、Skills、MCP、普通终端与私有工作区，以及按需授权的日历读写、GUI、通知/位置/使用情况读取。

### Q8：Root 能多做什么？

Root 进一步开放：后台虚拟屏执行、系统设置与开关修改、应用管理、受保护文件、深度个人数据检索（通讯录、短信、通话记录、相册、健康等），以及一方应用（闹钟、便签等）原生操作和 Root Shell / chroot。部分数据还取决于 ROM 适配。

### Q9：LSPosed 有什么用？为什么开了框架通信仍不生效？

LSPosed 用于厂商助手接管（小布 / 超级小爱）、系统快捷入口（电源键、一圈即搜）与 Google 能力增强，部分功能另需 Root。

“框架通信”只表示收到框架服务 Binder，**不代表** LSPosed 管理器中的 Eta 模块已开启或 Hook 已生效。请同时确认：

1. LSPosed 管理器中 Eta 模块已启用；
2. 作用域包含目标应用/系统；
3. 目标进程已重启加载 Hook；
4. ROM 与系统版本受支持。

### Q10：为什么某些功能在“普通设备”下不可用？

系统工具、敏感读取、敏感操作、终端与文件、网页浏览、记忆均有独立开关，且受 Android 版本、授权和 ROM 限制。工具页默认显示“当前设备”，可查看“全部能力”了解额外介绍与实际条件；查看不会申请权限，也不会扩大模型权限。详见[设备支持说明](ROOTLESS_SUPPORT.md)。

## 上游同步

### Q11：Eta-Refined 与上游 Eta 是什么关系？会跟进更新吗？

Eta-Refined **派生自 [Mangi-11/Eta](https://github.com/Mangi-11/Eta)**，原作者 Mangi-11（蛮吉）。重构版仅重做 UI/UX 与工程化，业务功能来自上游。

上游功能更新通过**每日自动同步 + 手动触发**跟进，经 PR 合入 `main`；发生冲突时自动创建 Issue 与冲突 PR 并标注原因，不静默覆盖。详见 [UPSTREAM_SYNC.md](UPSTREAM_SYNC.md)。

### Q12：同步会引入 dsh-Go 的代码吗？

**不会。** 同步源**只有 Mangi-11/Eta**。dsh-Go 仅作为 UI/UX 设计语言参考（AGPL-3.0），不复制代码、不同步、不合并。

## 回退

### Q13：如何回退到原版 Eta？

从 `v3.4.0-refined2` 起，Eta-Refined 与原版**包名不同，可以共存**，因此回退**无需卸载 Eta-Refined**：

1. 在 Eta-Refined 中**导出备份**（对话、模型配置、记忆、角色等）——若原版仍在设备上，其数据本来就一直保留，未受影响。
2. 从 [Mangi-11/Eta Releases](https://github.com/Mangi-11/Eta/releases) 下载原版 APK 并安装（若原版仍在设备上，直接使用即可）。
3. 在系统设置中把默认数字助理、无障碍服务、LSPosed 作用域等切回原版。
4. 可自行卸载 Eta-Refined；卸载只会清除**重构版本身**的数据，不会影响原版。

> 若你使用的是 `v3.4.0-refined1` 或更早版本（包名与原版相同），仍需先卸载重构版才能装回原版；建议直接升级到 `refined2` 及以后版本以摆脱这个限制。

### Q14：回退后数据还在吗？

两版数据目录完全隔离：卸载 Eta-Refined 只清除**重构版**的数据，原版数据不受影响。但若你要把重构版的对话/配置带过去，务必先导出备份再导入。系统级授权（无障碍、通知使用权、LSPosed 作用域、默认数字助理）是按包名记录的，切换版本后需要重新授予。

## 远程主机（实验性）

> 远程主机本轮为**协议实现 + 可配置**，PC 端前提未就绪时不可用，且**未做真机联调、不宣称已连通**。配置步骤、错误码与「未实现」清单见 [REMOTE_HOST.md](REMOTE_HOST.md)；背景见 [UPSTREAM_SYNC.md](UPSTREAM_SYNC.md) 的保留区清单。

### Q17：远程主机连不上 / 一直「连接中」或报错，怎么办？

按 PC 端前提逐项排查（详见 [REMOTE_HOST.md](REMOTE_HOST.md)）：

1. **直连**：PC 是否已全局安装 `@deepseek-ai/dsh` 并运行 `dsh web`；填入的**带 token URL** 是否正确、未过期；手机与 PC 是否网络可达（同一局域网 / 端口转发 / 反向代理），PC 防火墙是否放行端口（默认 3080）。
2. **Relay**：PC 端 the agent harness 是否已安装并启用 `ds-harness-remote` Host 插件、Host 是否在线；手机与 PC 是否用**同一账号**登录；默认第三方 Server `https://dsh.r2049.cn`（或自建地址）是否可达。
3. 看状态卡给出的**错误码**与可操作建议（见[错误码表](REMOTE_HOST.md)）再点「重试」：`HOST_OFFLINE` / `CONNECTION_FAILED` 多为 PC 端或网络未就绪；`AUTH_INVALID` / `TOKEN_EXPIRED` 多为登录或 token 失效，重新登录、或重新复制 `dsh web` 打印的 URL 即可。
4. **一次只连一台远程主机**：同一账号的其它连接会抢占（`CONNECTION_REPLACED`），先断开别处再重试。

### Q18：提示「身份指纹不匹配」（`PEER_IDENTITY_MISMATCH`）是什么？

这是 Relay 的 **Host 静态公钥 pin（TOFU）** 保护：首次连接时核对并记住主机指纹（形如 `ABCD EF01 2345`，48 bit），之后若指纹变化会**在建立任何连接之前直接拒绝**，防止中间人。

- 先确认 PC 端是否**重装了 the agent harness / Host 插件、换了设备或换了账号**——这会产生新密钥，属正常。
- 若确认是本人主机，在「设置 → 远程主机」中**忘记 / 重置该主机的信任**后重新连接并**重新核对指纹**。
- 若无法确认，**不要连接**：可能有人冒充该主机。

### Q19：提示「版本不兼容」（`HARNESS_VERSION_INCOMPATIBLE`）怎么办？

协议要求 PC 端 Host 在安全通道建立后的第一条请求 `harness.transport.describe` 中声明 `harness.remote.v3`（DSH 0.2.x Session V3），否则拒绝连接。请把 PC 端 the agent harness 与 `ds-harness-remote` Host 插件升级到支持 `harness.remote.v3` 的版本（0.6.x 系 Host 插件）后重试；旧版本无法兼容。详见 [REMOTE_HOST.md](REMOTE_HOST.md)。

### Q20：如何关闭第三方托管 Server `https://dsh.r2049.cn`？

Relay 默认连接的 `https://dsh.r2049.cn` 是**第三方托管服务**（ds-harness-remote，MIT），不是 Eta-Refined 自有服务。在「设置 → 远程主机」中可：

1. **关闭 Relay 开关**：关闭后不再连接任何 Relay Server，仅保留直连方式（或只用本机 Eta Agent）。
2. **改为自建 Server 地址**：填自己的裸 origin（公网须 HTTPS，私网 host 才允许 HTTP）。
3. **不添加任何远程主机**：默认使用本机 Eta Agent，不连接任何第三方 Server。

关闭或改地址后，已保存主机与凭据不会被上传；凭据（token / Cookie / accessToken / refreshToken / X25519 私钥）只存本机 Android Keystore、不写日志。

### Q21：新增的远程主机功能会影响「与原版 Eta 共存」吗？

**不受影响，口径与上文「与原版共存说明」一致。** 远程主机只是 Eta-Refined App **内部新增的设置页、会话与协议层**：

- 从 `v3.4.0-refined2` 起 Eta-Refined 使用独立包名 `io.github.mangi.eta.refined` 与独立签名，可与原版 Eta **同时安装、数据隔离、互不覆盖**；本功能**不改动**包名、签名、Provider authority 或任何系统级授权配置。
- 远程主机列表、连接状态与凭据保存在 **Eta-Refined 自己的数据目录**（凭据经 Android Keystore 加密），与原版 Eta 互不相干；卸载 Eta-Refined 只清除重构版数据，原版不受影响。
- 因此 LSPosed 作用域、默认数字助理、无障碍服务等系统级能力仍按上文「换包名后需重新配置」的既有说明处理，远程主机不会引入新的共存冲突。

## 其他

### Q15：后台任务被杀了怎么办？

后台运行受 Android 与厂商进程管理影响；强停或重启后需手动启动任务。系统与应用更新也可能需要重新适配 Hook。可在系统设置中为 Eta 关闭电池优化、允许后台运行以提升稳定性。

### Q16：为什么有些操作“没有生效”？

设置、开关和一方应用写操作都会**读回实际状态**；未确认时会明确报告，不会盲目重复执行。请根据提示检查授权、ROM 适配或目标应用状态。
