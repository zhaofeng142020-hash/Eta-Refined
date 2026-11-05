# Eta-Refined 发布与上游同步手册

本手册面向重构版 **Eta-Refined**，说明签名密钥、版本发布（正式版 / nightly）、
SemVer 与版本号规则，以及从上游 `Mangi-11/Eta` 同步更新的操作流程。
相关自动化见：

- 发布流水线：[`release.yml`](./workflows/release.yml)（正式版 / nightly 均由它负责）
- 上游同步：[`sync-upstream.yml`](./workflows/sync-upstream.yml)
- PR 检查：[`pr-check.yml`](./workflows/pr-check.yml)
- 旧版发布流水线 `android-release.yml` 已退役，发布统一走 `release.yml`（避免同一 tag 重复建 Release）；历史说明见 [`RELEASING.md`](./RELEASING.md)。

---

## 1. 签名 Secrets

发布证书与密码**绝不提交到 Git**。在仓库
`Settings > Secrets and variables > Actions` 中配置以下 4 个 Secret（新前缀 `ETA_REFINED_`）：

| Secret 名称 | 说明 |
| --- | --- |
| `ETA_REFINED_KEYSTORE_BASE64` | KeyStore（`.jks`）文件的 Base64 文本 |
| `ETA_REFINED_STORE_PASSWORD` | KeyStore 密码 |
| `ETA_REFINED_KEY_ALIAS` | 密钥别名（key alias） |
| `ETA_REFINED_KEY_PASSWORD` | 密钥密码 |

> 兼容性：`release.yml` 通过 `||` 回退，**新前缀优先、旧名兜底**。若仓库里仍是原有的
> `ETA_RELEASE_KEYSTORE_BASE64` / `ETA_RELEASE_STORE_PASSWORD` / `ETA_RELEASE_KEY_ALIAS` /
> `ETA_RELEASE_KEY_PASSWORD`，无需立刻改名即可工作；但推荐逐步迁移到 `ETA_REFINED_*`。
> 注意 Gradle（`app/build.gradle.kts`）读取的是 `ETA_RELEASE_*` **环境变量**，工作流内部
> 已负责把 Secret 映射到对应 env 变量名，无需在 Gradle 侧改动。

> 优雅降级：全新仓库首次推送 `main` 时即使**尚未配置**签名 Secrets，`release.yml` 也**不会失败**：
> 它会先无条件跑单元测试，检测到缺少 Secrets 时输出 `ready=false` 并以 `::notice::` 跳过发布，
> 任务保持成功（`main` 不变红）。一旦配置了上述 Secrets（新前缀或兼容旧名），
> `tag` / `main` / 手动触发三条路径即自动发布。

### 1.1 生成 KeyStore

```bash
keytool -genkeypair \
  -keystore Eta-Refined-release.jks \
  -alias eta-refined \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storetype PKCS12
```

> 记下 KeyStore 密码、alias 与 key 密码；`-alias` 的值即 `ETA_REFINED_KEY_ALIAS`。
> 该证书用于后续所有版本覆盖升级，请离线妥善备份，**丢失后已安装用户无法覆盖升级**。

### 1.2 上传为 GitHub Secrets

macOS / Linux 可先把证书编码为单行 Base64，再写入 Secret：

```bash
# 生成 Base64（去掉换行）
base64 < Eta-Refined-release.jks | tr -d '\n' > keystore.b64

# 用 GitHub CLI 写入（密码类建议回车后按提示输入，避免留在命令历史里）
gh secret set ETA_REFINED_KEYSTORE_BASE64 < keystore.b64
gh secret set ETA_REFINED_STORE_PASSWORD
gh secret set ETA_REFINED_KEY_ALIAS
gh secret set ETA_REFINED_KEY_PASSWORD
```

Windows PowerShell 生成 Base64：

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("Eta-Refined-release.jks")) |
  Set-Content -NoNewline keystore.b64
```

---

## 2. 版本号规则

`versionCode` 与 `versionName` 都在 `app/build.gradle.kts` 的 `defaultConfig` 中手动维护：

- **versionCode**：`yyyyMMddNN`
  - `yyyyMMdd` 为发版日期，`NN` 为当日序号，从 `01` 起（`01`–`99`）。
  - 必须**严格递增**，否则已安装用户无法覆盖升级。日期编码不会超过 Play 上限（约 21 亿）。
  - 例：`2026103101` = 2026-10-31 当天第 1 个版本；同日再发第二个为 `2026103102`。
- **versionName**：遵循 [SemVer](#4-semver-规范)，当前示例 `3.4.0`。

发布前务必先改这两个值，再打 tag；`release.yml` 会校验 **tag 名 == `v<versionName>`**，
不一致直接失败，避免标签与安装包版本错位。

---

## 3. 发布流程

### 3.1 正式版（tag 触发）

1. 更新 `app/build.gradle.kts` 的 `versionCode`（`yyyyMMddNN`）与 `versionName`（如 `3.5.0`）。
2. 提交并合并到 `main`，确认 `PR Check` 通过。
3. 打标签并推送（标签必须为 `v<versionName>`）：

   ```bash
   git tag v3.5.0
   git push origin v3.5.0
   ```

4. 推送 tag 后，`Release` 工作流自动执行：
   - 还原签名证书 → `:app:testDebugUnitTest` → `:app:assembleRelease`
   - `apksigner verify` 校验签名
   - 校验 tag 与 `output-metadata.json` 的 `versionName` 一致
   - `gh release create v3.5.0 ... --verify-tag --generate-notes`，上传 `Eta-v3.5.0.apk`
5. 在仓库 `Releases` 中检查生成的正式 Release：确认说明、附件名 `Eta-v3.5.0.apk`。

> 全流程任一环节失败（测试、构建、签名、版本校验）都会**中止，不发布任何 Release**。

### 3.2 nightly / 预发布（main 推送或手动触发）

- **main 推送**：每次向 `main` 推送都会生成预发布
  `v<versionName>-nightly.<shortsha>`（`--prerelease`），例如 `v3.4.0-nightly.a1b2c3d`。
- **手动触发**：`Actions > Release > Run workflow`，可选输入 `version` 覆盖标签中的版本名
  （如填 `3.4.0`，省略前导 `v`），未填则用 Gradle 的 `versionName`；行为与 main 推送一致，
  仍为预发布。
- nightly 的 Release 会标注为 `Pre-release`，**不会**出现在 `releases/latest`，
  应用内正式更新检查不会读到它。
- 同一 `versionName` 下 nightly 以短提交号区分；正式发版请按 3.1 递增版本并打 tag。

### 3.3 手动触发说明

`workflow_dispatch` 适合在没有新提交时重跑发布，或补齐某个 nightly 产物。
它**不会**创建正式版；正式版只认 `v*` tag。

---

## 4. SemVer 规范

使用 `MAJOR.MINOR.PATCH`：

- **MAJOR**：存在不兼容的接口/行为变更（如配置格式、数据库 schema 不兼容升级）。
- **MINOR**：向下兼容的新功能。
- **PATCH**：向下兼容的缺陷修复。

约定：

- 正式版的 `versionName` 只写 `X.Y.Z`，**不带**前缀 `v`（`v` 只出现在 git tag / Release 名上）。
- 预发布标识（如 `-nightly.<sha>`）只存在于 **Release 标签**层，不写入 `versionName`，
  以免影响正式版的版本比较。
- 已发布的 `X.Y.Z` 不可复用；修复必须递增 `PATCH`。
- `versionCode` 的日期编码与 `versionName` 的语义版本相互独立，但都必须单调递增。

---

## 5. 上游同步（`sync-upstream.yml`）

`sync-upstream.yml` 每天 UTC 20:00 或手动运行，仅从 **`Mangi-11/Eta`** 同步 `main`，
**绝不涉及 dsh-Go 或其它仓库**。无冲突时开 PR 等人工 review；有冲突或构建失败时开
Issue 通知并**不合并**。

### 5.1 触发与产物

- **无冲突**：推 `sync/upstream-<date>` 分支 → 开 PR（标题
  `chore(sync): 同步上游 Mangi-11/Eta 更新 <date>`，正文列出新提交）→ **不自动合并**。
- **有冲突**：记录冲突文件 → `git merge --abort` → 开 Issue（标签 `upstream-sync`、`conflict`）
  → 另推 `[CONFLICT]` 说明分支并开 PR。
- **合并后构建失败**：开 Issue 通知，不推送、不开合并 PR。
- 由 `GITHUB_TOKEN` 创建的 PR 可能不会自动触发 `PR Check`，合并前请手动 re-run。

### 5.2 演练步骤（本地预演同步）

无冲突的正常路径：

```bash
# 1) 添加上游（仅 Mangi-11/Eta）
git remote add upstream https://github.com/Mangi-11/Eta.git   # 已有则 set-url

# 2) 拉取上游 main
git fetch upstream main

# 3) 基于当前 main 建同步分支
git switch main
git pull --ff-only origin main
git switch -c sync/upstream-$(date +%Y%m%d)

# 4) 合并（不要用 -X ours/theirs）
git merge upstream/main --no-edit

# 5) 本地验证
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug

# 6) 推送并开 PR（交给人工 review，不自动合并）
git push --set-upstream origin "sync/upstream-$(date +%Y%m%d)"
gh pr create --base main \
  --title "chore(sync): 同步上游 Mangi-11/Eta 更新 $(date +%Y%m%d)" \
  --body "上游同步，等待人工 review + CI。" \
  --label upstream-sync
```

冲突演练（重点是**不覆盖重构**）：

```bash
git fetch upstream main
git switch -c sync/upstream-$(date +%Y%m%d)
git merge upstream/main            # 出现冲突

# 列出冲突文件，逐个手工合并，保留 Eta-Refined 的重构改动
git diff --name-only --diff-filter=U

# 若想放弃本次尝试：
git merge --abort

# 解决后：
git add -A
git commit -m "chore(sync): 手工解决上游冲突"
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

> **保留区清单**（冲突时优先保留重构实现，逐文件人工合并）：
> `ui/theme2/`、`ui/onboarding/`、`README.md`、`README_EN.md`、`docs/`、
> `app/build.gradle.kts`、`.github/`。清单随重构进度维护。
> **严禁** `-X ours` / `-X theirs` 或整文件静默覆盖上游/本地任一方的改动。

### 5.3 冲突后的人工收尾

当 Issue 被自动创建时：

1. 按 Issue 中的冲突文件清单在本地复现合并。
2. 逐文件解决，保留重构改动；必要时与上游沟通取舍。
3. 本地测试通过后，手动推送分支并开 PR（流程同 5.2）。
4. 在 Issue 中记录处理结论并关闭。
