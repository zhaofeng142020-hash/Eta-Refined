#!/usr/bin/env pwsh
# =============================================================================
# Eta-Refined GitHub 引导脚本
# -----------------------------------------------------------------------------
# 作用：在提供有效 PAT 的前提下，一键完成：
#   1) 创建公开仓库 <Owner>/Eta-Refined（重名则回退 Eta-Refined-UI）
#   2) 把本地 main 推送到该仓库（upstream 仍指向 Mangi-11/Eta）
#   3) 写入 4 个发布签名 Secrets（读取 ../secrets）
#   4) 可选：创建首个 v* 标签以触发一次 GitHub Release
#
# 前置：
#   $env:GH_TOKEN = "ghp_xxx"   # 需 repo + workflow 权限（细粒度：Contents/Actions/Issues/Pull requests/Administration 读写）
#   若使用 gh CLI 且已 gh auth login，也可省略 GH_TOKEN。
#   secrets/ 目录由 create-release-keystore.ps1 生成（keystore.b64 + keystore-info.txt）。
#
# 用法：
#   pwsh ./scripts/bootstrap-github.ps1 -Owner zhaofeng142020-hash
#   pwsh ./scripts/bootstrap-github.ps1 -Owner <you> -CreateFirstTag
# =============================================================================
param(
    [Parameter(Mandatory = $true)][string]$Owner,
    [string]$Repo = 'Eta-Refined',
    [string]$FallbackRepo = 'Eta-Refined-UI',
    [string]$GhPath = 'gh',
    [string]$SecretsDir = (Resolve-Path (Join-Path $PSScriptRoot '..\secrets') -ErrorAction SilentlyContinue),
    [switch]$CreateFirstTag,
    [string]$FirstTag = 'v3.4.0'
)

$ErrorActionPreference = 'Stop'
$env:GIT_CONFIG_GLOBAL = if ($env:GIT_CONFIG_GLOBAL) { $env:GIT_CONFIG_GLOBAL } else { (Join-Path $PSScriptRoot '..\..\tools\gitconfig') }

function Invoke-Git { & git @args; if ($LASTEXITCODE -ne 0) { throw "git $($args -join ' ') 失败" } }

if (-not $env:GH_TOKEN -and -not $env:GITHUB_TOKEN) {
    Write-Warning "未检测到 GH_TOKEN / GITHUB_TOKEN。将尝试使用已登录的 gh CLI；若未登录请先设置 GH_TOKEN。"
}

Push-Location (Join-Path $PSScriptRoot '..')
try {
    # 0) 确保工作区干净
    $dirty = (git status --porcelain)
    if ($dirty) { throw "工作区有未提交改动，请先提交后再运行。`n$dirty" }

    # 1) 确定仓库名（重名回退）
    $finalRepo = $Repo
    & $GhPath repo view "$Owner/$Repo" *> $null
    if ($LASTEXITCODE -eq 0) {
        Write-Warning "仓库 $Owner/$Repo 已存在，改用备用名 $FallbackRepo。"
        $finalRepo = $FallbackRepo
        & $GhPath repo view "$Owner/$FallbackRepo" *> $null
        if ($LASTEXITCODE -eq 0) { throw "备用名 $Owner/$FallbackRepo 也已存在，请手动指定。" }
    }
    $full = "$Owner/$finalRepo"

    # 2) 创建公开仓库（空仓库，不初始化）
    & $GhPath repo create $full --public --description "Eta-Refined — 基于 Mangi-11/Eta 重构 UI/UX 的 Android 系统级 AI Agent"
    if ($LASTEXITCODE -ne 0) { throw "创建仓库 $full 失败。" }

    # 3) 规范化本地分支为 main 并设置 origin（upstream 保持 Mangi-11/Eta）
    Invoke-Git branch -M main
    Invoke-Git remote remove origin 2>$null
    Invoke-Git remote add origin "https://github.com/$full.git"
    if (-not ((git remote) -contains 'upstream')) {
        Invoke-Git remote add upstream "https://github.com/Mangi-11/Eta.git"
    }

    # 4) 推送 main 与 tags
    Invoke-Git push -u origin main --tags

    # 5) 写入发布签名 Secrets（若已生成）
    if ($SecretsDir -and (Test-Path (Join-Path $SecretsDir 'keystore.b64'))) {
        $b64 = Get-Content (Join-Path $SecretsDir 'keystore.b64') -Raw
        $info = @{}
        Get-Content (Join-Path $SecretsDir 'keystore-info.txt') | ForEach-Object {
            $kv = $_ -split '=', 2; if ($kv.Count -eq 2) { $info[$kv[0]] = $kv[1] }
        }
        & $GhPath secret set ETA_REFINED_KEYSTORE_BASE64 --repo $full --body $b64
        & $GhPath secret set ETA_REFINED_STORE_PASSWORD --repo $full --body $info['storePassword']
        & $GhPath secret set ETA_REFINED_KEY_ALIAS      --repo $full --body $info['alias']
        & $GhPath secret set ETA_REFINED_KEY_PASSWORD   --repo $full --body $info['keyPassword']
        Write-Host "已写入 4 个签名 Secrets。" -ForegroundColor Green
    } else {
        Write-Warning "未找到 secrets/keystore.b64，跳过 Secrets 配置（release 工作流将优雅降级、不发布）。"
    }

    # 6) 可选：创建首个标签触发 Release
    if ($CreateFirstTag) {
        Invoke-Git tag $FirstTag
        Invoke-Git push origin $FirstTag
        Write-Host "已推送标签 $FirstTag，将触发 Release 工作流。" -ForegroundColor Green
    }

    Write-Host ""
    Write-Host "完成：https://github.com/$full" -ForegroundColor Cyan
    Write-Host "Actions: https://github.com/$full/actions" -ForegroundColor Cyan
}
finally {
    Pop-Location
}
