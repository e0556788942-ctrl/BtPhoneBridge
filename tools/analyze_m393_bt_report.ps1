param(
    [Parameter(Mandatory = $true)]
    [string]$ReportPath,
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $ReportPath -PathType Leaf)) {
    throw "דוח ADB לא נמצא: $ReportPath"
}
$lines = Get-Content -LiteralPath $ReportPath
if ($lines.Count -eq 0) { throw "דוח ADB ריק." }

function Get-SectionLines {
    param([string]$SectionName)
    $inside = $false
    $result = New-Object System.Collections.Generic.List[string]
    foreach ($line in $lines) {
        if ($line -match '^===\s*(.*?)\s*===$') {
            $header = $Matches[1]
            if ($inside) { break }
            if ($header -eq $SectionName) { $inside = $true }
            continue
        }
        if ($inside) { $result.Add($line) }
    }
    return $result.ToArray()
}

$managerLines = @(Get-SectionLines -SectionName "Bluetooth manager")
$audioPolicyLines = @(Get-SectionLines -SectionName "Audio policy")
$candidateLines = @(Get-SectionLines -SectionName "Candidate system Bluetooth packages")

$packageDetailLines = New-Object System.Collections.Generic.List[string]
$insidePackage = $false
foreach ($line in $lines) {
    if ($line -match '^===\s*(.*?)\s*===$') {
        $insidePackage = $Matches[1] -match '^Package details:'
        continue
    }
    if ($insidePackage) { $packageDetailLines.Add($line) }
}

$packages = @($candidateLines | ForEach-Object { $_.Trim() } |
    Where-Object { $_ -match '^[A-Za-z0-9_.]+$' } | Sort-Object -Unique)

$stateOnEvidence = @($managerLines | Where-Object { $_ -match '(?i)\bstate\s*:\s*ON\b|\bBluetooth\s+(?:enabled|מופעל)\s*[:=]\s*(?:true|1|on)\b' })
$stateOffEvidence = @($managerLines | Where-Object { $_ -match '(?i)\bstate\s*:\s*OFF\b|\bBluetooth\s+(?:disabled|כבוי)\s*[:=]\s*(?:false|0|off)\b' })
$bluetoothState = "לא ידוע"
if ($stateOnEvidence.Count -gt 0 -and $stateOffEvidence.Count -gt 0) {
    $bluetoothState = "CONFLICT"
} elseif ($stateOnEvidence.Count -gt 0) {
    $bluetoothState = "ON"
} elseif ($stateOffEvidence.Count -gt 0) {
    $bluetoothState = "OFF"
}

$serviceLines = @($packageDetailLines | Where-Object { $_ -match '(?i)HeadsetClientService' })
$serviceEnabledEvidence = @($serviceLines | Where-Object { $_ -match '(?i)\benabled\s*[:=]\s*true\b' })
$serviceDisabledEvidence = @($serviceLines | Where-Object { $_ -match '(?i)\benabled\s*[:=]\s*false\b|\bdisabled\b' })
$serviceEnabledHint = $serviceEnabledEvidence.Count -gt 0 -and $serviceDisabledEvidence.Count -eq 0
$serviceFinding = "לא נמצא בקטעי package dump שנאספו (היעדר אינו הוכחה שאין תמיכה)"
if ($serviceLines.Count -gt 0) {
    if ($serviceEnabledEvidence.Count -gt 0 -and $serviceDisabledEvidence.Count -gt 0) { $enabledText = "נמצאו שורות רכיב סותרות (רמז בלבד)" }
    elseif ($serviceEnabledEvidence.Count -gt 0) { $enabledText = "בשורה מופיע enabled=true (רכיב בלבד)" }
    elseif ($serviceDisabledEvidence.Count -gt 0) { $enabledText = "בשורה מופיע enabled=false/disabled (רכיב בלבד)" }
    else { $enabledText = "מצב enabled לא צוין בשורה" }
    $serviceFinding = "נמצא אזכור שירות · $enabledText · אינו מוכיח תפקיד HFP Client פעיל"
}

# Only the Bluetooth-manager section may provide profile evidence.
# Package/component mentions (e.g. HeadsetClientService enabled=true) are not profile evidence.
$clientProfileToken = '(?i)\bHEADSET_CLIENT\b|\bHFP_CLIENT\b|profile_supported_hfpclient|bluetooth\.profile\.hfp\.hf\.enabled|hfp_client_enabled'
$clientProfileLines = @($managerLines | Where-Object { $_ -match $clientProfileToken })
$clientEnabledFlag = '(?i)\b(?:enabled|supported|active|available|profile_supported_hfpclient|hfp_client_enabled)\s*[:=]\s*(?:true|1)\b|\b(?:HEADSET_CLIENT|HFP_CLIENT)\s*[:=]\s*(?:true|1)\b'
$clientDisabledFlag = '(?i)[:=]\s*(?:false|0)\b|\b(?:disabled|unsupported|not\s+supported)\b'
$clientEnabledEvidence = @($clientProfileLines | Where-Object { $_ -match $clientEnabledFlag })
$clientDisabledEvidence = @($clientProfileLines | Where-Object { $_ -match $clientDisabledFlag })
$clientProfilePositive = $clientEnabledEvidence.Count -gt 0 -and $clientDisabledEvidence.Count -eq 0
$clientProfileFinding = "לא נמצא אזכור HFP Client במקטע Bluetooth manager (לא מכריע)"
if ($clientEnabledEvidence.Count -gt 0 -and $clientDisabledEvidence.Count -gt 0) {
    $clientProfileFinding = "נמצאו ערכים סותרים של enabled/disabled (טקסט דוח בלבד; לא מכריע)"
} elseif ($clientEnabledEvidence.Count -gt 0) {
    $clientProfileFinding = "נמצא ערך true/enabled במקטע Bluetooth manager (רמז חיובי בלבד; טקסט דוח)"
} elseif ($clientDisabledEvidence.Count -gt 0) {
    $clientProfileFinding = "נמצא ערך false/disabled במקטע Bluetooth manager (רמז שלילי בלבד; טקסט דוח)"
} elseif ($clientProfileLines.Count -gt 0) {
    $clientProfileFinding = "נמצא אזכור HFP Client ללא ערך enabled מפורש במקטע Bluetooth manager (לא ראיה להפעלה)"
}
$scoOut = @($audioPolicyLines | Where-Object { $_ -match '(?i)AUDIO_DEVICE_OUT_BLUETOOTH_SCO|TYPE_BLUETOOTH_SCO|SCO.*(?:output|sink)|Bluetooth.*SCO.*(?:output|sink)' }).Count -gt 0
$scoIn = @($audioPolicyLines | Where-Object { $_ -match '(?i)AUDIO_DEVICE_IN_BLUETOOTH_SCO|SCO.*(?:input|source)|Bluetooth.*SCO.*(?:input|source)' }).Count -gt 0

$summary = New-Object System.Collections.Generic.List[string]
$summary.Add("PhoneLink — סיכום דוח ADB (ניתוח לא מקוון)")
$summary.Add("Bluetooth: $bluetoothState")
$summary.Add("חבילות Bluetooth מועמדות: " + $(if ($packages.Count) { $packages -join ", " } else { "לא נמצאו בקטע הדוח" }))
$summary.Add("HeadsetClientService: $serviceFinding")
$summary.Add("HFP Client indication: $clientProfileFinding")
$summary.Add("מדיניות SCO יציאה: " + $(if ($scoOut) { "נמצא בקטע Audio policy" } else { "לא נמצא בדוח המסונן (לא מכריע)" }))
$summary.Add("מדיניות SCO כניסה: " + $(if ($scoIn) { "נמצא בקטע Audio policy" } else { "לא נמצא בדוח המסונן (לא מכריע)" }))
if ($bluetoothState -eq "ON" -and $serviceEnabledHint -and $clientProfilePositive -and $scoOut -and $scoIn) {
    $summary.Add("הערכת סיכום: קיימים רמזים מערכתיים חיוביים; נדרשת בדיקת חיבור HFP Client ושמע פיזית.")
} else {
    $summary.Add("הערכת סיכום: אין מספיק ראיות בדוח המסונן; נדרשת בדיקת מכשיר/יצרן.")
}
$summary.Add("מגבלה: הדוח המסונן אינו מוכיח ש-HFP Client פעיל, שני קישורי HFP פועלים יחד, או ששיחת טלפון מגושרת לאוזניות.")
$summary.Add("הכלי פועל על קובץ בלבד; הוא אינו מפעיל ADB ואינו משנה את הנגן.")

$output = $summary -join [Environment]::NewLine
if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
    Set-Content -LiteralPath $OutputPath -Value $output -Encoding UTF8
    Write-Host "סיכום נוצר: $OutputPath"
}
Write-Output $output
