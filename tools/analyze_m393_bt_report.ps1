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

$bluetoothState = "לא ידוע"
$managerText = $managerLines -join "`n"
if ($managerText -match '(?i)\bstate\s*:\s*ON\b|\bBluetooth\s+(?:enabled|מופעל)\s*[:=]\s*(?:true|1|on)\b') {
    $bluetoothState = "ON"
} elseif ($managerText -match '(?i)\bstate\s*:\s*OFF\b|\bBluetooth\s+(?:disabled|כבוי)\s*[:=]\s*(?:false|0|off)\b') {
    $bluetoothState = "OFF"
}

$serviceLines = @($packageDetailLines | Where-Object { $_ -match '(?i)HeadsetClientService' })
$serviceFinding = "לא נמצא בקטעי package dump שנאספו (היעדר אינו הוכחה שאין תמיכה)"
if ($serviceLines.Count -gt 0) {
    $enabledText = "מצב enabled לא צוין בשורה"
    if ($serviceLines -match '(?i)\benabled\s*[:=]\s*true\b') { $enabledText = "בשורה מופיע enabled=true (הצהרה בלבד)" }
    elseif ($serviceLines -match '(?i)\benabled\s*[:=]\s*false\b') { $enabledText = "בשורה מופיע enabled=false (הצהרה בלבד)" }
    $serviceFinding = "נמצא אזכור שירות · $enabledText"
}

$clientProfileMention = @($managerLines | Where-Object { $_ -match '(?i)\bHEADSET_CLIENT\b|\bHFP_CLIENT\b|HFP\s+(?:HF|CLIENT)' }).Count -gt 0
$scoOut = @($audioPolicyLines | Where-Object { $_ -match '(?i)AUDIO_DEVICE_OUT_BLUETOOTH_SCO|TYPE_BLUETOOTH_SCO|SCO.*(?:output|sink)|Bluetooth.*SCO.*(?:output|sink)' }).Count -gt 0
$scoIn = @($audioPolicyLines | Where-Object { $_ -match '(?i)AUDIO_DEVICE_IN_BLUETOOTH_SCO|SCO.*(?:input|source)|Bluetooth.*SCO.*(?:input|source)' }).Count -gt 0

$summary = New-Object System.Collections.Generic.List[string]
$summary.Add("PhoneLink — סיכום דוח ADB (ניתוח לא מקוון)")
$summary.Add("Bluetooth: $bluetoothState")
$summary.Add("חבילות Bluetooth מועמדות: " + $(if ($packages.Count) { $packages -join ", " } else { "לא נמצאו בקטע הדוח" }))
$summary.Add("HeadsetClientService: $serviceFinding")
$summary.Add("אזכור HEADSET_CLIENT/HFP Client ב-Bluetooth manager: " + $(if ($clientProfileMention) { "נמצא (טקסט דוח בלבד)" } else { "לא נמצא בדוח המסונן (לא מכריע)" }))
$summary.Add("מדיניות SCO יציאה: " + $(if ($scoOut) { "נמצא בקטע Audio policy" } else { "לא נמצא בדוח המסונן (לא מכריע)" }))
$summary.Add("מדיניות SCO כניסה: " + $(if ($scoIn) { "נמצא בקטע Audio policy" } else { "לא נמצא בדוח המסונן (לא מכריע)" }))
if ($bluetoothState -eq "ON" -and $serviceLines.Count -gt 0 -and $clientProfileMention -and $scoOut -and $scoIn) {
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
