$ErrorActionPreference = "Stop"
$script:adbCalls = New-Object System.Collections.Generic.List[string]

function adb {
    $adbArgs = @($args)
    $global:LASTEXITCODE = 0
    $script:adbCalls.Add(($adbArgs -join " "))

    if ($adbArgs.Count -gt 0 -and $adbArgs[0] -eq "-s") {
        $adbArgs = @($adbArgs | Select-Object -Skip 2)
    }

    if ($adbArgs.Count -eq 1 -and $adbArgs[0] -eq "devices") {
        Write-Output "List of devices attached"
        Write-Output "TEST-DEVICE device"
        return
    }
    if ($adbArgs.Count -eq 1 -and $adbArgs[0] -eq "get-state") {
        Write-Output "device"
        return
    }
    if ($adbArgs.Count -ge 2 -and $adbArgs[0] -eq "shell") {
        $remote = @($adbArgs | Select-Object -Skip 1)
        $key = $remote -join " "
        switch -Exact ($key) {
            "getprop ro.product.model" { Write-Output "M393"; return }
            "getprop ro.product.manufacturer" { Write-Output "ExampleVendor"; return }
            "getprop ro.product.device" { Write-Output "m393"; return }
            "getprop ro.build.version.release" { Write-Output "13"; return }
            "getprop ro.build.version.sdk" { Write-Output "33"; return }
            "getprop ro.build.version.security_patch" { Write-Output "2026-10-01"; return }
            "cmd package list packages -s" {
                Write-Output "package:com.vendor.bluetooth"
                Write-Output "package:com.vendor.settings"
                return
            }
            "pm list packages -s" { Write-Output "package:com.android.bluetooth"; return }
            "dumpsys package com.vendor.bluetooth" {
                Write-Output "Package [com.vendor.bluetooth]"
                Write-Output "versionName=13.2"
                Write-Output "HeadsetClientService enabled=true"
                Write-Output "BLUETOOTH_PRIVILEGED"
                return
            }
            "dumpsys package com.android.bluetooth" {
                Write-Output "Unable to find package: com.android.bluetooth"
                return
            }
            "dumpsys bluetooth_manager" {
                Write-Output "state: ON"
                Write-Output "Profile: HEADSET_CLIENT"
                Write-Output "device=AA:BB:CC:DD:EE:FF"
                return
            }
            "dumpsys activity services" {
                Write-Output "ServiceRecord{123 com.vendor.bluetooth/.hfpclient.HeadsetClientService}"
                return
            }
            "dumpsys audio" {
                Write-Output "mMode=MODE_NORMAL"
                Write-Output "Bluetooth SCO device available"
                return
            }
            "dumpsys media.audio_policy" {
                Write-Output "AUDIO_DEVICE_OUT_BLUETOOTH_SCO"
                Write-Output "AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET"
                return
            }
            "service list" {
                Write-Output "3 bluetooth_manager: [android.bluetooth.IBluetoothManager]"
                Write-Output "4 audio: [android.media.IAudioService]"
                return
            }
        }
        $global:LASTEXITCODE = 1
        Write-Output "Unexpected mocked shell command: $key"
        return
    }

    $global:LASTEXITCODE = 1
    Write-Output "Unexpected mocked ADB command: $($adbArgs -join ' ')"
}

$scriptPath = Join-Path $PSScriptRoot "collect_m393_bt_diagnostics.ps1"
$outputPath = Join-Path ([System.IO.Path]::GetTempPath()) ("PhoneLink-ADB-Test-" + [guid]::NewGuid().ToString("N") + ".txt")

try {
    & $scriptPath -OutputPath $outputPath
    if (-not (Test-Path -LiteralPath $outputPath)) { throw "Collector did not create its report." }

    $report = Get-Content -LiteralPath $outputPath -Raw
    foreach ($required in @(
        "M393",
        "com.vendor.bluetooth",
        "HeadsetClientService",
        "dumpsys activity services",
        "Audio policy",
        "XX:XX:XX:XX:XX:XX",
        "ADB serial is not written"
    )) {
        if (-not $report.Contains($required)) { throw "Expected report content was missing: $required" }
    }

    if ($report.Contains("AA:BB:CC:DD:EE:FF")) { throw "A Bluetooth MAC address was not redacted." }
    if ($report.Contains("TEST-DEVICE")) { throw "The ADB serial leaked into the report." }

    $unexpected = @($script:adbCalls | Where-Object {
        $_ -match "(?i)\b(install|uninstall|setprop|settings\s+put|am\s+(start|broadcast)|pm\s+(enable|disable|grant|revoke))\b"
    })
    if ($unexpected.Count -gt 0) {
        throw "The collector issued a command outside the read-only allowlist: $($unexpected -join '; ')"
    }

    Write-Host "ADB collector mock test passed: vendor package discovery, service/audio sections, MAC redaction, serial exclusion, and read-only command set."
}
finally {
    if (Test-Path -LiteralPath $outputPath) {
        Remove-Item -LiteralPath $outputPath -Force
    }
}
