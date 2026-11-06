# 远程主机（Remote host）

> ⚠️ **实验性功能 · 仅「协议实现 + 可配置」 · 未做真机联调**
>
> 本轮交付的是「远程主机」的**协议层实现、可配置的设置界面与文档**：可以在 App 内配置直连或 Relay 主机、查看连接状态与错误建议。**它不代表现在就能连通**：
> - **PC 端前提未就绪时功能不可用**（见下文）；本机维护环境**没有 Android 真机 / 模拟器**，PC 端也**未运行** `dsh web` 与 `ds-harness-remote` Host 插件；
> - 因此远程连接**未做真机联调，不宣称已连通**。正确性依靠协议源码逐行核对 + 不依赖真机的单元测试 + CI 编译，而非实机验证。
>
> 完整的范围、错误码与「未实现」清单见下文；常见问题见 [FAQ.md](FAQ.md)。

## 这是什么

Eta-Refined 通常在本机（手机）内运行 Agent Runtime。远程主机是一项**并列新增**的能力：把手机上的会话接到运行在**电脑 / 服务器上的 the agent harness（DSH / `dsh web`）**引擎。它与本机 Eta Agent **并存、可随时切换**——不选远程主机时仍使用本机 Agent，原有业务功能（设备工具、Hook、系统入口等）不受影响。

本轮提供两种连接方式（与确认的范围一致：**方案 A 直连 `dsh web` HTTP + Relay-only 零依赖骨架**），且**不新增任何 Gradle 依赖**（HTTP/WS 复用现有 OkHttp，加密用 Android 平台能力，凭据用 Android Keystore）。

## 开始之前：PC 端前提（未就绪时功能不可用）

> **远程主机不是「装了 App 就能用」的功能**。手机端只是客户端，必须先在 PC / 服务器端把引擎跑起来。下列任一前提未满足，连接都会失败（状态卡会给出对应错误码）。

### 直连（方案 A）前提

1. PC 已安装 Node.js 并全局安装 DSH：`npm i -g @deepseek-ai/dsh`。
2. PC 上运行 `dsh web`，终端会打印一条**带 token 的 URL**，形如 `http://192.168.1.5:3080/?token=…`（默认端口 `3080`）。
3. 手机能访问该地址：同一局域网，或通过端口转发 / 反向代理可达；PC 防火墙放行对应端口；公网地址须 HTTPS（私网 host 才允许 HTTP）。
4. 该 token URL 仍然有效（重启 `dsh web` 会更换 token）。

### Relay 前提

1. PC 端 the agent harness 已安装并启用 **`ds-harness-remote` Host 插件**（0.6.x 系，需声明 `harness.remote.v3`），且 Host 处于**在线**状态。
2. 手机与 PC **使用同一账号**登录到 Relay Server。
3. Relay Server 可达：默认第三方托管 `https://dsh.r2049.cn`，或你自建的 Server。
4. 首次连接需要**核对主机身份指纹**（见下文）。

> 目前本机维护环境**未运行**上述任何 PC 端组件，也未安装 Host 插件；因此这两种方式在本轮都**无法实测连通**。

## 两种连接方式

### 方式一：直连 `dsh web`（方案 A / DIRECT）

1. 在 PC 上执行 `npm i -g @deepseek-ai/dsh` 并运行 `dsh web`。
2. 复制终端打印的**带 token 的完整 URL**（例如 `http://192.168.1.5:3080/?token=…`）。
3. 手机打开「设置 → 远程主机 → 直连主机」，填入该 URL（App 拆分为**地址**与 **launch token**），保存后点「测试连接」。
4. 状态卡显示「已连接」后，可在首页主机切换胶囊选择该主机进入远程会话。

协议侧（供排障参考）：用该 token 换取 `dsh-auth-*` 会话 Cookie（绑定 host:port，默认 30 天），随后通过 `POST /api/<endpoint>` 发一元请求、通过 `WS /api/remote.mux` 接收流式事件；请求带正确 `Host`、不发 `Origin`。token / Cookie 失效会报 `AUTH_INVALID` 或 `TOKEN_EXPIRED`，重新复制 `dsh web` 打印的 URL 即可。

### 方式二：Relay 中继（RELAY，relay-only 零依赖骨架）

1. 在 PC 端 the agent harness 安装 `ds-harness-remote` Host 插件并登录同一账号。
2. 手机在「设置 → 远程主机 → Relay」用**同一账号**登录（登录方式由 Server 提供）。
3. 从**在线主机列表**选择目标 PC；**首次连接核对身份指纹**（形如 `ABCD EF01 2345`，48 bit TOFU），确认是本人主机后「信任并连接」。
4. 一次只连接一台远程主机；切换到另一台会断开上一台。

协议侧（供排障参考）：REST `/api/v1/*`（Bearer accessToken）完成账号 / 设备目录，Control WebSocket `/ws/v1/connect` 建立连接，经 `Noise_IK_25519_ChaChaPoly_SHA256` 端到端加密后，把 `harness.remote.call` / `harness.remote.stream.*` 映射回与直连相同的会话 / 消息 / 事件接口。Server 要求安全通道建立后的第一条请求 `harness.transport.describe` 声明 `harness.remote.v3`，否则报 `HARNESS_VERSION_INCOMPATIBLE`。

> Relay 本轮为 **relay-only 骨架**：只走 Server 中继，**不尝试** WebRTC / LAN / P2P / TURN 直连（见[未实现 / 留作后续](#未实现--留作后续)）。

## 本机 Agent 与远程主机并存、切换

- 首页顶部的**主机切换胶囊**在「本机 Eta Agent」与已保存的远程主机之间切换；**不选远程主机（默认）即使用本机 Agent**。
- 两者是并列关系，远程主机**不修改、不替换**本机 Agent Runtime、设备工具、Provider、Hook 与系统入口。
- 可开启「连接上次使用的主机」；也可随时切回本机。远程主机配置与凭据只属于 Eta-Refined 自己的数据，与原版 Eta 互不影响（见 [FAQ.md](FAQ.md)）。

## 第三方托管 Server 声明（重要）

> ⚠️ **Relay 默认连接的是第三方托管服务，不是 Eta-Refined 自有服务。**

- 默认 Server：**`https://dsh.r2049.cn`**，由 [ds-harness-remote](https://github.com/liguobao/ds-harness-remote)（MIT，作者 liguobao / 李国宝）项目提供并托管；Eta-Refined 只是实现该协议的**客户端**，不运营、不控制该 Server。
- **可关闭**：在「设置 → 远程主机」关闭 Relay 开关后，App 不再连接任何 Relay Server（可继续用直连或仅用本机 Agent）。
- **可改自建地址**：在同一处把 Server 地址改为你自己的裸 origin（公网须 HTTPS，私网 host 才允许 HTTP）。
- **不静默上传凭据**：默认不会在用户未配置 / 未登录的情况下上传任何凭据；只有你主动添加 Relay 主机并登录时，才会与所选 Server 交换账号与设备信息。

## 凭据与数据边界

- 所有凭据 —— 直连的 **launch token / `dsh-auth-*` Cookie**，Relay 的 **accessToken / refreshToken / 设备 X25519 私钥 / 主机公钥 pin** —— **只存本机 Android Keystore**（加密后落盘），**不写日志、不回显完整值、不进入备份、不通过 IPC 明文传递**。
- 主机列表、启用状态等活动配置存本机（DataStore / Room）；切换主机不改本机业务数据。
- 连接状态卡会显示网络路径（`http` / `relay`）与 RTT；**未测量到延迟时显示为空，不显示 0**。
- 与第三方 Server 的账号 / 设备目录交互仅在 Relay 方式、且你主动登录后发生；任务内容仍发送给你在远程引擎侧配置的模型服务。

## 错误码与处理建议

连接状态卡会给出中文错误说明与可操作建议，并标注是否可「重试」。稳定错误码如下：

| 错误码 | 含义 | 建议操作 |
| --- | --- | --- |
| `HOST_OFFLINE` | Relay 目标主机当前不在线 / 设备目录中不可用 | 确认 PC 端 Host 插件已启动并登录、Host 在线；恢复后重试。 |
| `AUTH_INVALID` | 鉴权无效：accessToken 无效、设备未授权或直连 Cookie 被拒 | 重新登录同一账号 / 重新注册设备；直连时重新复制 `dsh web` 打印的带 token URL。 |
| `TOKEN_EXPIRED` | token 过期：直连 Cookie（默认 30 天）或 Relay accessToken / launch token 失效 | 直连重新运行 `dsh web` 并更新 URL；Relay 重新登录触发 token 轮换（旧 refresh token 复用会失效）。 |
| `PEER_IDENTITY_MISMATCH` | 主机静态公钥指纹与首次信任的 pin 不一致（TOFU，连 socket 之前即拒绝） | 确认 PC 是否重装引擎 / Host 插件、换设备或换账号；确属本人主机则在设置中「忘记 / 重置该主机信任」后重新核对指纹；无法确认时**不要连接**（可能中间人）。 |
| `HARNESS_VERSION_INCOMPATIBLE` | 目标 Host 未声明 `harness.remote.v3`（DSH 0.2.x Session V3） | 把 PC 端 the agent harness 与 `ds-harness-remote` Host 插件升级到支持 `harness.remote.v3` 的版本后重试；旧版不兼容。 |
| `CONNECTION_REPLACED` | 同一客户端设备被另一条连接抢占（Server 规定每设备同时仅一条 Host 连接，WS close 4003） | 断开其它设备上同账号 / 同主机的连接，或等待旧连接超时；本 App 一次只连一台远程主机。 |
| `RATE_LIMITED` | Server 限流 / 请求或重连过于频繁 | 稍后重试，降低重连与轮询频率，避免多设备同时抢连。 |
| `CONNECTION_FAILED` | 通用连接失败：网络不可达、Server 不可达、握手 / 登录超时、地址或端口错误 | 检查网络、Server 地址与端口、PC 端前提是否就绪；查看状态卡「详情」后重试；必要时切换直连 / Relay 方式。 |

## 未实现 / 留作后续

为避免夸大，以下能力**本轮均未实现**（协议骨架按 relay-only 取舍，未做完整 DSH Remote 移植）：

- **WebRTC / LAN / P2P / TURN 直连**、ICE 候选选路与 `transport.relay.attach` 独立 Relay node data WS（本轮 Relay 只走 Server 中继）；
- **SSH 主机**（通过 SSH 启动远端 `dsh web` 并做本地端口转发）；
- **`/dsh-go/v1/*` 私有桥**（dsh-go-bridge 的原生插件协议、插件面板、会话永久删除等）；
- **DS Bot**（`/api/bot`、`/api/bot/feed` SSE 及远程 `dsBot/*`）；
- **工作区上传 / 大包分块**（`workspace.upload.*`、`fileUploads/upload`、`harness.remote.transfer.*`）、**RTCH 分块**、网页版内嵌、对话日志导出等远程隧道附加能力。

## 本轮交付范围与未联调声明

- **已交付**：远程主机协议层（`agent/remote/`、`data/remote/`）、设置 / 切换 / 远程会话 UI（`ui/screens/remote/` 及导航与设置页的追加式接线、三套 `values*/strings.xml`）、错误码到中文建议的映射、文档与上游同步保留区清单；以及不依赖真机的协议单元测试。
- **未交付 / 未验证**：本机无 Android 设备 / 模拟器、PC 端未运行 `dsh web` 与 Host 插件 ⇒ **远程连接未做真机联调，不宣称已连通**；端到端加密握手、Cookie 换发、流式事件与断线重连仅经过源码核对、单测与 CI 编译验证。
- 因此「远程主机」当前定位为**协议实现 + 可配置**，请在具备 PC 端环境后自行验证，并把结果反馈到仓库 Issues。

## 许可与致谢

- **本仓库**：Eta-Refined 仍采用 [PolyForm Noncommercial License 1.0.0](../LICENSE)，派生自 [Mangi-11/Eta](https://github.com/Mangi-11/Eta)。
- **dsh-Go（[ccch1mneyyy/dsh-Go](https://github.com/ccch1mneyyy/dsh-Go)，AGPL-3.0）**：仅作为**设计与协议事实的参考**，Kotlin 实现为原创，**未复制任何 Dart 源码**；其整体 AGPL-3.0 许可不因本参考而改变本仓库许可。
- **ds-harness-remote（[liguobao/ds-harness-remote](https://github.com/liguobao/ds-harness-remote)，MIT，作者李国宝 / liguobao）**：REST `/api/v1/*`、Control WS `/ws/v1/connect`、`Noise_IK_25519_ChaChaPoly_SHA256`、`harness.remote.v3`、`harness.remote.*` 等协议事实来自该项目，特此致谢；本项目仅实现其客户端侧，未复制其源码。
- 完整第三方组件清单见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) 与 [ATTRIBUTION.md](../ATTRIBUTION.md)。

## 相关文档

- [常见问题](FAQ.md)：远程连接连不上、指纹不匹配、版本不兼容、关闭第三方 Server、与原版共存。
- [重构说明与差异](REFACTORING.md)：本轮新增范围与未验证项。
- [上游同步说明](UPSTREAM_SYNC.md)：远程主机相关保留区清单。
- [README](../README.md) / [README_EN](../README_EN.md)：项目总览。
