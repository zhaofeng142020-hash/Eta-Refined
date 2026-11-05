# Eta-Refined 开源与发布操作说明

本目录脚本用于把本地重构结果开源到 GitHub 并启用自动发布。仓库当前已完成全部代码与 CI 改动，**只差一次带凭据的推送**。

## 0. 一次性准备：生成发布签名密钥

```powershell
pwsh ./scripts/create-release-keystore.ps1
# 产物在仓库外的 ../secrets/：eta-refined.p12、keystore.b64、keystore-info.txt
```
> 若已由维护者生成，可直接使用其 `secrets/` 目录。**secrets/ 已被 .gitignore 忽略，绝不入库。**

## 1. 创建仓库并推送（需 PAT）

PAT 需具备 `repo` + `workflow`（细粒度：Contents / Actions / Issues / Pull requests / Administration 读写）。
推荐用已登录的 `gh`：

```powershell
$env:GH_TOKEN = "ghp_xxx"
pwsh ./scripts/bootstrap-github.ps1 -Owner zhaofeng142020-hash
```

脚本会：创建公开仓库 `Eta-Refined`（重名自动回退 `Eta-Refined-UI`）→ 推送 `main` → 写 4 个签名 Secrets。

## 2. 触发首个 Release

```powershell
pwsh ./scripts/bootstrap-github.ps1 -Owner zhaofeng142020-hash -CreateFirstTag
# 或在仓库页 Releases/Tags 手动推送 v3.4.0 标签
```
推送 `v*` 标签 → `release.yml` 自动构建并发布正式 Release；`main` 分支推送 → nightly 预发布；`Actions → Release → Run workflow` 可手动触发。

## 3. 手动等效命令（不想用脚本时）

```bash
# 1) 建仓（GitHub 网页或 CLI）
gh repo create <owner>/Eta-Refined --public --description "Eta-Refined"
# 2) 关联远端（origin=新仓库，upstream=上游）
git remote remove origin
git remote add origin https://github.com/<owner>/Eta-Refined.git
git remote add upstream https://github.com/Mangi-11/Eta.git
git branch -M main
git push -u origin main --tags
# 3) 写入 4 个 Secrets（值见 secrets/keystore-info.txt 与 keystore.b64）
gh secret set ETA_REFINED_KEYSTORE_BASE64 --repo <owner>/Eta-Refined --body (cat secrets/keystore.b64)
gh secret set ETA_REFINED_STORE_PASSWORD  --repo <owner>/Eta-Refined --body "<storePassword>"
gh secret set ETA_REFINED_KEY_ALIAS       --repo <owner>/Eta-Refined --body "eta-refined"
gh secret set ETA_REFINED_KEY_PASSWORD    --repo <owner>/Eta-Refined --body "<keyPassword>"
# 4) 首发
git tag v3.4.0 && git push origin v3.4.0
```

## 4. 上游同步演练

- 触发：每天 UTC 20:00 自动，或 `Actions → Sync Upstream → Run workflow`。
- 无冲突 → 自动开 PR（不自动合并）；有冲突 → 自动建 Issue + `[CONFLICT]` PR，绝不静默覆盖。
- 手动演练：在仓库 `Actions` 手动运行一次，检查生成的 PR/Issue。

## 5. Secrets 一览

| Secret | 说明 |
| --- | --- |
| `ETA_REFINED_KEYSTORE_BASE64` | 发布证书 base64 |
| `ETA_REFINED_STORE_PASSWORD` | KeyStore 密码 |
| `ETA_REFINED_KEY_ALIAS` | 密钥别名（默认 `eta-refined`） |
| `ETA_REFINED_KEY_PASSWORD` | 密钥密码 |

> 兼容旧名 `ETA_RELEASE_*`：`release.yml` 用 `||` 回退，配置任一即可。
