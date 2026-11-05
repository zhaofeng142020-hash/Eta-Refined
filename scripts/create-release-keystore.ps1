#!/usr/bin/env pwsh
# =============================================================================
# 生成 Eta-Refined 发布签名密钥（PKCS12 / .p12），并输出 base64 供 GitHub Secrets 使用。
# -----------------------------------------------------------------------------
# 输出到仓库外的 secrets/ 目录（不会提交到 git）：
#   eta-refined.p12      发布证书
#   keystore.b64         base64 文本（-> ETA_REFINED_KEYSTORE_BASE64）
#   keystore-info.txt    alias / 密码（-> ETA_REFINED_KEY_ALIAS / *_PASSWORD）
#
# 依赖：keytool（随 JDK 提供；Android 构建环境通常已有 JDK）。
# 若本机无 keytool，可使用仓库所有者已生成的 secrets（见交付说明）。
# =============================================================================
param(
    [string]$OutDir = (Join-Path $PSScriptRoot '..\secrets'),
    [string]$Alias = 'eta-refined',
    [string]$DistinguishedName = 'CN=Eta-Refined, O=Eta-Refined, C=CN',
    [int]$ValidityDays = 3650
)

$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$p12 = Join-Path $OutDir 'eta-refined.p12'

# 生成随机强密码（避免硬编码）
$bytes = New-Object 'System.Byte[]' 18
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$pw = 'EtaRefined-' + ([Convert]::ToBase64String($bytes) -replace '[+/=]', '') + '!7'

& keytool -genkeypair -v `
    -keystore $p12 -storetype PKCS12 `
    -alias $Alias -keyalg RSA -keysize 2048 -validity $ValidityDays `
    -dname $DistinguishedName `
    -storepass $pw -keypass $pw
if ($LASTEXITCODE -ne 0) { throw "keytool 生成失败（请确认已安装 JDK 且 keytool 在 PATH）。" }

$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($p12))
Set-Content -Path (Join-Path $OutDir 'keystore.b64') -Value $b64 -NoNewline
@"
alias=$Alias
storePassword=$pw
keyPassword=$pw
storeFile=$p12
"@ | Set-Content -Path (Join-Path $OutDir 'keystore-info.txt')

Write-Host "已生成：$p12" -ForegroundColor Green
Write-Host "请勿提交 secrets/ 目录；用 scripts/bootstrap-github.ps1 写入 GitHub Secrets 即可。" -ForegroundColor Yellow
