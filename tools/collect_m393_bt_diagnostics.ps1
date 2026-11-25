param(
    [string]$Serial = "",
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "adb לא נמצא ב-PATH. התקן Android Platform Tools ופתח PowerShell חדש."
}

if ([string]::IsNullOrWhiteSpace($Serial)) {
    $deviceLines = @(& adb devices 2>&1 | Select-Object -Skip 1 | Where-Object { $_ -match "^\S+\s+device$" })
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
    param([Parameter(Mandatory = $true)][string[]]$CommandArgs)
    $result = & adb @adbPrefix shell @CommandArgs 2>&1
    $exitCode = $LASTEXITCODE
    $text = ($result | Out-String).TrimEnd()
    if ($exitCode -ne 0) {
        return ($text + [Environment]::NewLine + "[remote command exit code: $exitCode]")
    }
    return $text
}

function Add-FilteredSection {
    param(
        [string]$Title,
        [string]$Text,
        [string]$Pattern
    )
    $sections.Add("=== $Title ===")
    if ([string]::IsNullOrWhiteSpace($Text)) {
        $sections.Add("(no output)")
        return
    }
    $matches = @($Text -split "\r?\n" | Select-String -Pattern $Pattern | ForEach-Object { $_.Line })
    if ($matches.Count -eq 0) {
        $sections.Add("(no matching lines; command may be unsupported or output format may differ)")
    } else {
        foreach ($match in $matches) { $sections.Add($match) }
    }
}

$sections = New-Object System.Collections.Generic.List[string]
$sections.Add("PhoneLink M393 - read-only ADB diagnostics")
$sections.Add("Collected: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
$sections.Add("This script does not change device settings, install software, or record audio.")
$sections.Add("ADB serial is not written to this report.")
$sections.Add("")

$sections.Add("=== Build properties ===")
foreach ($prop in @(
    "ro.product.model",
    "ro.product.manufacturer",
    "ro.product.device",
    "ro.build.version.release",
    "ro.build.version.sdk",
    "ro.build.version.security_patch"
)) {
    $value = Get-AdbShellText -CommandArgs @("getprop", $prop)
    $sections.Add("$prop = $value")
}
$sections.Add("")

# These AOSP sysprops are vendor-overlay inputs read by Bluetooth during initialization.
# Empty output means absent/not exposed; it must not be interpreted as false.
$sections.Add("=== Bluetooth HFP role system properties ===")
foreach ($prop in @(
    "bluetooth.profile.hfp.hf.enabled",
    "bluetooth.profile.hfp.ag.enabled"
)) {
    $value = Get-AdbShellText -CommandArgs @("getprop", $prop)
    if ([string]::IsNullOrWhiteSpace($value)) { $value = "(empty or not exposed)" }
    $sections.Add("$prop = $value")
}
$sections.Add("")

# Discover vendor-specific Bluetooth package names instead of assuming AOSP's package name.
$packageList = Get-AdbShellText -CommandArgs @("cmd", "package", "list", "packages", "-s")
if ($packageList -match "(?i)unknown command|can't find service|not found") {
    $packageList = Get-AdbShellText -CommandArgs @("pm", "list", "packages", "-s")
}
$bluetoothPackages = @(
    $packageList -split "\r?\n" |
        ForEach-Object {
            if ($_ -match "^package:([A-Za-z0-9_.]+)$") { $Matches[1] }
        } |
        Where-Object { $_ -match "(?i)bluetooth|(^|[._])bt([._]|$)|btservice|bt_stack" } |
        Sort-Object -Unique
)
if ($bluetoothPackages -notcontains "com.android.bluetooth") {
    $bluetoothPackages += "com.android.bluetooth"
}
$bluetoothPackages = @($bluetoothPackages | Sort-Object -Unique)

$sections.Add("=== Candidate system Bluetooth packages ===")
if ($bluetoothPackages.Count -eq 0) {
    $sections.Add("(none discovered)")
} else {
    foreach ($packageName in $bluetoothPackages) { $sections.Add($packageName) }
}
$sections.Add("")

foreach ($packageName in $bluetoothPackages) {
    $packageDump = Get-AdbShellText -CommandArgs @("dumpsys", "package", $packageName)
    Add-FilteredSection -Title "Package details: $packageName" -Text $packageDump -Pattern "Package \[|versionName=|versionCode=|HeadsetClientService|HeadsetService|AdapterService|A2dpService|ProfileService|enabled=|disabledComponents|enabledComponents|BLUETOOTH_PRIVILEGED|BLUETOOTH_CONNECT|Services:"
}

$bluetoothManager = Get-AdbShellText -CommandArgs @("dumpsys", "bluetooth_manager")
Add-FilteredSection -Title "Bluetooth manager" -Text $bluetoothManager -Pattern "(?i)state:|enabled|profile|headset|client|a2dp|connected|active|adapter|uuid|hfp|sco|bluetooth"

$activityServices = Get-AdbShellText -CommandArgs @("dumpsys", "activity", "services")
Add-FilteredSection -Title "Running activity services related to Bluetooth/audio" -Text $activityServices -Pattern "(?i)HeadsetClientService|HeadsetService|AdapterService|A2dpService|Bluetooth|Audio"

$audio = Get-AdbShellText -CommandArgs @("dumpsys", "audio")
Add-FilteredSection -Title "Audio service" -Text $audio -Pattern "(?i)Communication|SCO|Bluetooth|preferred|route|device|mode|AudioDevice|Strategy"

$audioPolicy = Get-AdbShellText -CommandArgs @("dumpsys", "media.audio_policy")
Add-FilteredSection -Title "Audio policy" -Text $audioPolicy -Pattern "(?i)bluetooth|sco|communication|device|strategy|route|input|output"

$services = Get-AdbShellText -CommandArgs @("service", "list")
Add-FilteredSection -Title "Registered binder services mentioning Bluetooth/audio" -Text $services -Pattern "(?i)bluetooth|audio"

$report = ($sections -join [Environment]::NewLine) -replace "(?i)(?:[0-9a-f]{2}:){5}[0-9a-f]{2}", "XX:XX:XX:XX:XX:XX"
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path (Get-Location) ("PhoneLink_M393_ADB_{0}.txt" -f (Get-Date -Format "yyyyMMdd_HHmmss"))
}
Set-Content -LiteralPath $OutputPath -Value $report -Encoding UTF8
Write-Host "דוח נוצר: $OutputPath"
Write-Host "כתובות MAC הוסתרו אוטומטית. מזהה ADB אינו נכתב לדוח. לפני שיתוף, בדוק שאין שמות התקנים או פרטי build שאינך רוצה למסור."
