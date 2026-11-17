param(
    [string]$Serial = "",
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "adb לא נמצא ב-PATH. התקן Android Platform Tools ופתח PowerShell חדש."
}

if ([string]::IsNullOrWhiteSpace($Serial)) {
    $deviceLines = @(& adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "^\S+\s+device$" })
    if ($deviceLines.Count -ne 1) {
        throw "נדרש מכשיר אחד שמופיע כ-device ב-adb devices. נמצאו $($deviceLines.Count). חבר את ה-M393 ואשר USB debugging; אם יש כמה מכשירים, העבר -Serial."
    }
    $Serial = ($deviceLines[0] -split "\s+")[0]
}

$adbPrefix = @("-s", $Serial)
$state = (& adb @adbPrefix get-state 2>&1 | Out-String).Trim()
if ($state -ne "device") {
    throw "המכשיר אינו זמין או ש-USB debugging לא אושר. adb get-state: $state"
}

function Get-AdbShellText {
    param([string]$Command)
    $result = & adb @adbPrefix shell $Command 2>&1
    return ($result | Out-String).TrimEnd()
}

$sections = New-Object System.Collections.Generic.List[string]
$sections.Add("PhoneLink M393 - read-only ADB diagnostics")
$sections.Add("Collected: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
$sections.Add("This script does not change device settings or install software.")
$sections.Add("")
$sections.Add("=== Build ===")
foreach ($prop in @("ro.product.model", "ro.product.manufacturer", "ro.build.version.release", "ro.build.version.sdk", "ro.build.version.security_patch")) {
    $sections.Add("$prop = $(Get-AdbShellText "getprop $prop")")
}
$sections.Add("")
$sections.Add("=== Bluetooth package: selected PackageManager lines ===")
$pkg = Get-AdbShellText "dumpsys package com.android.bluetooth"
$sections.Add(($pkg -split "\r?\n" | Select-String -Pattern "Package \[|versionName=|versionCode=|HeadsetClientService|headsetclient|enabled=|disabledComponents|enabledComponents|BLUETOOTH_PRIVILEGED|BLUETOOTH_CONNECT" | ForEach-Object { $_.Line }) -join [Environment]::NewLine)
$sections.Add("")
$sections.Add("=== Bluetooth manager: selected lines ===")
$bt = Get-AdbShellText "dumpsys bluetooth_manager"
$sections.Add(($bt -split "\r?\n" | Select-String -Pattern "state:|enabled|profile|headset|client|a2dp|connected|active|adapter|uuid|hfp|sco|Bluetooth" | ForEach-Object { $_.Line }) -join [Environment]::NewLine)
$sections.Add("")
$sections.Add("=== Audio service: selected lines ===")
$audio = Get-AdbShellText "dumpsys audio"
$sections.Add(($audio -split "\r?\n" | Select-String -Pattern "Communication|communication|SCO|Bluetooth|preferred|route|device|mode|AudioDevice|Strategy" | ForEach-Object { $_.Line }) -join [Environment]::NewLine)
$sections.Add("")
$sections.Add("=== Registered services mentioning Bluetooth/audio ===")
$services = Get-AdbShellText "service list"
$sections.Add(($services -split "\r?\n" | Select-String -Pattern "bluetooth|audio" | ForEach-Object { $_.Line }) -join [Environment]::NewLine)

$report = ($sections -join [Environment]::NewLine) -replace "(?i)(?:[0-9a-f]{2}:){5}[0-9a-f]{2}", "XX:XX:XX:XX:XX:XX"
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path (Get-Location) ("PhoneLink_M393_ADB_{0}.txt" -f (Get-Date -Format "yyyyMMdd_HHmmss"))
}
Set-Content -LiteralPath $OutputPath -Value $report -Encoding UTF8
Write-Host "דוח נוצר: $OutputPath"
Write-Host "כתובות MAC הוסתרו אוטומטית. לפני שיתוף, בדוק שאין בדוח שמות מכשירים או פרטים אישיים שאינך רוצה למסור."
