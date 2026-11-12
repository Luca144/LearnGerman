# Builds a debug APK and uploads it to the "dev" GitHub release, so the phone can
# download it from one fixed link. Usage:  .\deploy.ps1
#
# Phone link: https://github.com/Luca144/LearnGerman/releases/download/dev/wortlinge.apk

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# Terminals opened before the JDK was installed don't see JAVA_HOME yet: read it from the saved
# user settings, or fall back to the newest Microsoft JDK on this PC.
if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = [Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
}
if (-not $env:JAVA_HOME) {
    $jdk = Get-ChildItem "C:\Program Files\Microsoft" -Directory -Filter "jdk-*" -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($jdk) { $env:JAVA_HOME = $jdk.FullName }
}
if (-not $env:JAVA_HOME) { throw "No Java found. Install it with: winget install Microsoft.OpenJDK.21" }

$repo = "Luca144/LearnGerman"
$tag = "dev"

# Minutes since 1 Jan 2026: always rising, so every build installs over the last one.
$versionCode = [int](((Get-Date).ToUniversalTime() - [datetime]"2026-01-01").TotalMinutes)

Write-Host "Building Wortlinge, build $versionCode ..." -ForegroundColor Cyan
& .\gradlew.bat assembleDebug "-PversionCode=$versionCode" --console=plain -q
if ($LASTEXITCODE -ne 0) { throw "Build failed." }

$apk = Join-Path $env:TEMP "wortlinge.apk"
Copy-Item "app\build\outputs\apk\debug\app-debug.apk" $apk -Force

$commit = (git log -1 --format="%h %s")
$dirty = if (git status --porcelain) { " (plus uncommitted changes)" } else { "" }
$notes = "Test build $versionCode from $commit$dirty, uploaded $(Get-Date -Format 'yyyy-MM-dd HH:mm').`n`nOn the phone, download wortlinge.apk below and open it to install."

$existing = gh release list --repo $repo --json tagName --jq ".[].tagName"
if (-not ($existing -contains $tag)) {
    gh release create $tag --repo $repo --target main --prerelease --title "Wortlinge test build" --notes $notes
    if ($LASTEXITCODE -ne 0) { throw "Could not create the release." }
} else {
    gh release edit $tag --repo $repo --notes $notes | Out-Null
}

gh release upload $tag $apk --repo $repo --clobber
if ($LASTEXITCODE -ne 0) { throw "Upload failed." }

Write-Host ""
Write-Host "Done. Build $versionCode is online:" -ForegroundColor Green
Write-Host "  https://github.com/$repo/releases/download/$tag/wortlinge.apk"
