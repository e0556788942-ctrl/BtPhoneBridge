$ErrorActionPreference = "Stop"

$scriptPath = Join-Path $PSScriptRoot "analyze_m393_bt_report.ps1"
$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("PhoneLink-Analyzer-Test-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tempRoot | Out-Null

function Assert-Contains {
    param([string]$Text, [string]$Expected)
    if (-not $Text.Contains($Expected)) {
        throw "Expected summary to contain: $Expected`nActual summary:`n$Text"
    }
}

try {
    $positivePath = Join-Path $tempRoot "positive.txt"
    $positiveOutput = Join-Path $tempRoot "positive-summary.txt"
    @(
        "PhoneLink M393 - read-only ADB diagnostics",
        "=== Candidate system Bluetooth packages ===",
        "com.vendor.bluetooth",
        "",
        "=== Bluetooth HFP role system properties ===",
        "bluetooth.profile.hfp.hf.enabled = true",
        "bluetooth.profile.hfp.ag.enabled = true",
        "",
        "=== Package details: com.vendor.bluetooth ===",
        "Package [com.vendor.bluetooth]",
        "HeadsetClientService enabled=true",
        "",
        "=== Bluetooth manager ===",
        "state: ON",
        "Profile: HEADSET_CLIENT enabled=true",
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
    Assert-Contains $positive "HFP HF role property: נמצא ערך true/1 למאפיין HFP HF"
    Assert-Contains $positive "HFP AG role property: נמצא ערך true/1 למאפיין HFP AG"
    Assert-Contains $positive "HFP Client indication: נמצא ערך true/enabled במקטע Bluetooth manager"
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
        "=== Bluetooth HFP role system properties ===",
        "bluetooth.profile.hfp.hf.enabled = false",
        "bluetooth.profile.hfp.ag.enabled = true",
        "",
        "=== Package details: com.android.bluetooth ===",
        "Package [com.android.bluetooth]",
        "",
        "=== Bluetooth manager ===",
        "state: OFF",
        "Profile: HEADSET_CLIENT enabled=false",
        "",
        "=== Audio policy ===",
        "No SCO devices in this filtered report"
    ) | Set-Content -LiteralPath $negativePath -Encoding UTF8
    $negative = (& $scriptPath -ReportPath $negativePath) -join "`n"
    Assert-Contains $negative "Bluetooth: OFF"
    Assert-Contains $negative "HeadsetClientService: לא נמצא"
    Assert-Contains $negative "לא נמצא בדוח המסונן"
    Assert-Contains $negative "HFP HF role property: נמצא ערך false/0 למאפיין HFP HF"
    Assert-Contains $negative "HFP AG role property: נמצא ערך true/1 למאפיין HFP AG"
    Assert-Contains $negative "HFP Client indication: נמצא ערך false/disabled במקטע Bluetooth manager"
    Assert-Contains $negative "אין מספיק ראיות"
    if ($negative -match "נמצאו ערכים סותרים") { throw "A disabled-only profile line was incorrectly classified as conflicting evidence." }

    # A disabled service component must suppress the aggregate positive hint.
    $serviceDisabledPath = Join-Path $tempRoot "service-disabled.txt"
    @(
        "PhoneLink M393 - read-only ADB diagnostics",
        "=== Candidate system Bluetooth packages ===",
        "com.vendor.bluetooth",
        "",
        "=== Package details: com.vendor.bluetooth ===",
        "Package [com.vendor.bluetooth]",
        "HeadsetClientService enabled=false",
        "",
        "=== Bluetooth manager ===",
        "state: ON",
        "Profile: HEADSET_CLIENT enabled=true",
        "",
        "=== Audio policy ===",
        "AUDIO_DEVICE_OUT_BLUETOOTH_SCO",
        "AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET"
    ) | Set-Content -LiteralPath $serviceDisabledPath -Encoding UTF8
    $serviceDisabled = (& $scriptPath -ReportPath $serviceDisabledPath) -join "`n"
    Assert-Contains $serviceDisabled "HeadsetClientService: נמצא אזכור שירות"
    Assert-Contains $serviceDisabled "enabled=false/disabled"
    Assert-Contains $serviceDisabled "HFP Client indication: נמצא ערך true/enabled במקטע Bluetooth manager"
    Assert-Contains $serviceDisabled "אין מספיק ראיות"
    if ($serviceDisabled -match "קיימים רמזים מערכתיים חיוביים") {
        throw "An explicitly disabled HeadsetClientService was incorrectly summarized as positive."
    }

    # A component/service mention must not become HFP profile evidence.
    $serviceOnlyPath = Join-Path $tempRoot "service-only.txt"
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
        "",
        "=== Audio policy ===",
        "AUDIO_DEVICE_OUT_BLUETOOTH_SCO",
        "AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET"
    ) | Set-Content -LiteralPath $serviceOnlyPath -Encoding UTF8
    $serviceOnly = (& $scriptPath -ReportPath $serviceOnlyPath) -join "`n"
    Assert-Contains $serviceOnly "HeadsetClientService: נמצא אזכור שירות"
    Assert-Contains $serviceOnly "HFP Client indication: לא נמצא אזכור HFP Client במקטע Bluetooth manager"
    Assert-Contains $serviceOnly "אין מספיק ראיות"
    if ($serviceOnly -match "קיימים רמזים מערכתיים חיוביים") {
        throw "HeadsetClientService enabled=true in package details was incorrectly treated as profile evidence."
    }

    # A profile name without an explicit enabled value must not be treated as positive evidence.
    $unknownPath = Join-Path $tempRoot "unknown.txt"
    @(
        "PhoneLink M393 - read-only ADB diagnostics",
        "=== Candidate system Bluetooth packages ===",
        "com.android.bluetooth",
        "",
        "=== Package details: com.android.bluetooth ===",
        "Package [com.android.bluetooth]",
        "HeadsetClientService enabled=true",
        "",
        "=== Bluetooth manager ===",
        "state: ON",
        "Profile: HEADSET_CLIENT",
        "",
        "=== Audio policy ===",
        "AUDIO_DEVICE_OUT_BLUETOOTH_SCO",
        "AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET"
    ) | Set-Content -LiteralPath $unknownPath -Encoding UTF8
    $unknown = (& $scriptPath -ReportPath $unknownPath) -join [Environment]::NewLine
    Assert-Contains $unknown "נמצא אזכור HFP Client ללא ערך enabled מפורש במקטע Bluetooth manager"
    Assert-Contains $unknown "אין מספיק ראיות"
    if ($unknown -match "קיימים רמזים מערכתיים חיוביים") {
        throw "A profile mention without an enabled value was incorrectly scored as positive evidence."
    }

    # Bare status words without a boolean value are ambiguous, not positive evidence.
    $bareFlagPath = Join-Path $tempRoot "bare-flags.txt"
    @(
        "PhoneLink M393 - read-only ADB diagnostics",
        "=== Candidate system Bluetooth packages ===",
        "com.android.bluetooth",
        "",
        "=== Bluetooth manager ===",
        "state: ON",
        "Profile: HEADSET_CLIENT enabled",
        "Profile: HEADSET_CLIENT supported",
        "",
        "=== Audio policy ===",
        "AUDIO_DEVICE_OUT_BLUETOOTH_SCO",
        "AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET"
    ) | Set-Content -LiteralPath $bareFlagPath -Encoding UTF8
    $bareFlags = (& $scriptPath -ReportPath $bareFlagPath) -join [Environment]::NewLine
    Assert-Contains $bareFlags "נמצא אזכור HFP Client ללא ערך enabled מפורש במקטע Bluetooth manager"
    Assert-Contains $bareFlags "אין מספיק ראיות"
    if ($bareFlags -match "קיימים רמזים מערכתיים חיוביים") {
        throw "Bare enabled/supported status words were incorrectly treated as affirmative boolean evidence."
    }

    # Contradictory Bluetooth ON/OFF evidence must not be reduced to ON.
    $stateConflictPath = Join-Path $tempRoot "bluetooth-state-conflict.txt"
    @(
        "PhoneLink M393 - read-only ADB diagnostics",
        "=== Candidate system Bluetooth packages ===",
        "com.android.bluetooth",
        "",
        "=== Package details: com.android.bluetooth ===",
        "Package [com.android.bluetooth]",
        "HeadsetClientService enabled=true",
        "",
        "=== Bluetooth manager ===",
        "state: ON",
        "state: OFF",
        "Profile: HEADSET_CLIENT enabled=true",
        "",
        "=== Audio policy ===",
        "AUDIO_DEVICE_OUT_BLUETOOTH_SCO",
        "AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET"
    ) | Set-Content -LiteralPath $stateConflictPath -Encoding UTF8
    $stateConflict = (& $scriptPath -ReportPath $stateConflictPath) -join [Environment]::NewLine
    Assert-Contains $stateConflict "Bluetooth: CONFLICT"
    Assert-Contains $stateConflict "אין מספיק ראיות"
    if ($stateConflict -match "קיימים רמזים מערכתיים חיוביים") {
        throw "Contradictory Bluetooth ON/OFF states were incorrectly summarized as positive."
    }

    # Contradictory flags must be reported as mixed evidence, not a positive result.
    $mixedPath = Join-Path $tempRoot "mixed.txt"
    @(
        "PhoneLink M393 - read-only ADB diagnostics",
        "=== Candidate system Bluetooth packages ===",
        "com.android.bluetooth",
        "",
        "=== Package details: com.android.bluetooth ===",
        "Package [com.android.bluetooth]",
        "HeadsetClientService enabled=true",
        "",
        "=== Bluetooth manager ===",
        "state: ON",
        "Profile: HEADSET_CLIENT enabled=true",
        "profile_supported_hfpclient=false",
        "",
        "=== Audio policy ===",
        "AUDIO_DEVICE_OUT_BLUETOOTH_SCO",
        "AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET"
    ) | Set-Content -LiteralPath $mixedPath -Encoding UTF8
    $mixed = (& $scriptPath -ReportPath $mixedPath) -join [Environment]::NewLine
    Assert-Contains $mixed "נמצאו ערכים סותרים של enabled/disabled"
    Assert-Contains $mixed "אין מספיק ראיות"
    if ($mixed -match "קיימים רמזים מערכתיים חיוביים") {
        throw "Contradictory HFP client flags were incorrectly scored as positive evidence."
    }

    # Contradictory vendor role property values must remain inconclusive.
    $roleConflictPath = Join-Path $tempRoot "hfp-role-property-conflict.txt"
    @(
        "PhoneLink M393 - read-only ADB diagnostics",
        "=== Candidate system Bluetooth packages ===",
        "com.vendor.bluetooth",
        "",
        "=== Bluetooth HFP role system properties ===",
        "bluetooth.profile.hfp.hf.enabled = true",
        "bluetooth.profile.hfp.hf.enabled = false",
        "bluetooth.profile.hfp.ag.enabled = true",
        "",
        "=== Package details: com.vendor.bluetooth ===",
        "Package [com.vendor.bluetooth]",
        "HeadsetClientService enabled=true",
        "",
        "=== Bluetooth manager ===",
        "state: ON",
        "Profile: HEADSET_CLIENT enabled=true",
        "",
        "=== Audio policy ===",
        "AUDIO_DEVICE_OUT_BLUETOOTH_SCO",
        "AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET"
    ) | Set-Content -LiteralPath $roleConflictPath -Encoding UTF8
    $roleConflict = (& $scriptPath -ReportPath $roleConflictPath) -join [Environment]::NewLine
    Assert-Contains $roleConflict "HFP HF role property: נמצאו ערכים סותרים"
    Assert-Contains $roleConflict "אין מספיק ראיות"
    if ($roleConflict -match "קיימים רמזים מערכתיים חיוביים") {
        throw "Contradictory HFP HF sysprop values were incorrectly summarized as positive."
    }

    # A missing sysprop is unknown, never an implicit false or positive.
    Assert-Contains $unknown "HFP HF role property: המאפיין לא נחשף"
    Assert-Contains $unknown "HFP AG role property: המאפיין לא נחשף"

    Write-Host "Analyzer tests passed: HFP HF/AG role properties positive, negative, unknown and conflicting; existing Bluetooth/HFP evidence cases."
}
finally {
    if (Test-Path -LiteralPath $tempRoot) { Remove-Item -LiteralPath $tempRoot -Recurse -Force }
}
