$ErrorActionPreference = "Stop"

$scriptPath = Join-Path $PSScriptRoot "analyze_m393_bt_report.ps1"
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("PhoneLink-Analyzer-Test-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tempRoot | Out-Null

function Assert-Contains {
    param([string]$Text, [string]$Expected)
    if (-not $Text.Contains($Expected)) { throw "Expected summary to contain: $Expected" }
}

try {
    $positivePath = Join-Path $tempRoot "positive.txt"
    $positiveOutput = Join-Path $tempRoot "positive-summary.txt"
    @(
        "PhoneLink M393 - read-only ADB diagnostics",
        "=== Candidate system Bluetooth packages ===",
        "com.vendor.bluetooth",
        "",
        "=== Package details: com.vendor.bluetooth ===",
        "Package [com.vendor.bluetooth]",
        "HeadsetClientService enabled=true",
        "",
        "=== Bluetooth manager ===",
        "state: ON",
        "Profile: HEADSET_CLIENT",
        "",
        "=== Audio policy ===",
        "AUDIO_DEVICE_OUT_BLUETOOTH_SCO",
        "AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET"
    ) | Set-Content -LiteralPath $positivePath -Encoding UTF8
    $positive = (& $scriptPath -ReportPath $positivePath -OutputPath $positiveOutput) -join "`n"
    Assert-Contains $positive "Bluetooth: ON"
    Assert-Contains $positive "com.vendor.bluetooth"
    Assert-Contains $positive "HeadsetClientService: נמצא"
    Assert-Contains $positive "enabled=true"
    Assert-Contains $positive "נמצא (טקסט דוח בלבד)"
    Assert-Contains $positive "מדיניות SCO יציאה: נמצא"
    Assert-Contains $positive "מדיניות SCO כניסה: נמצא"
    Assert-Contains $positive "אינו מוכיח"
    if (-not (Test-Path -LiteralPath $positiveOutput)) { throw "Summary output file was not created." }

    $negativePath = Join-Path $tempRoot "negative.txt"
    @(
        "PhoneLink M393 - read-only ADB diagnostics",
        "=== Candidate system Bluetooth packages ===",
        "com.android.bluetooth",
        "",
        "=== Package details: com.android.bluetooth ===",
        "Package [com.android.bluetooth]",
        "",
        "=== Bluetooth manager ===",
        "state: OFF",
        "",
        "=== Audio policy ===",
        "No SCO devices in this filtered report"
    ) | Set-Content -LiteralPath $negativePath -Encoding UTF8
    $negative = (& $scriptPath -ReportPath $negativePath) -join "`n"
    Assert-Contains $negative "Bluetooth: OFF"
    Assert-Contains $negative "HeadsetClientService: לא נמצא"
    Assert-Contains $negative "לא נמצא בדוח המסונן"
    Assert-Contains $negative "אין מספיק ראיות"

    Write-Host "Analyzer tests passed: positive and negative evidence, conservative interpretation, and output-file generation."
}
finally {
    if (Test-Path -LiteralPath $tempRoot) { Remove-Item -LiteralPath $tempRoot -Recurse -Force }
}
