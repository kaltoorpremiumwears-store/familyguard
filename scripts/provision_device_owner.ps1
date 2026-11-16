# FamilyGuard - Android Enterprise Device Owner Setup Script (PowerShell)
# This script configures the Child App as Device Owner (DO) for testing and development.
# Requirements:
# 1. Android device connected via USB or Wi-Fi with USB Debugging enabled.
# 2. No Google accounts or user profiles on the device (Settings -> Accounts -> Remove Accounts).

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   FamilyGuard - Device Owner Provisioning Utility         " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Check ADB connection
Write-Host "`n[1/4] Checking connected Android devices..." -ForegroundColor Yellow
$devices = adb devices
Write-Host $devices

if ($devices -notmatch "\tdevice") {
    Write-Host "[ERROR] No authorized Android device detected. Enable USB debugging and reconnect." -ForegroundColor Red
    Exit 1
}

# 2. Check existing accounts
Write-Host "`n[2/4] Verifying device accounts status..." -ForegroundColor Yellow
$accounts = adb shell dumpsys account | Select-String "Account {"
if ($accounts) {
    Write-Host "[WARNING] Existing user accounts detected on device!" -ForegroundColor Yellow
    Write-Host "Android requires all accounts to be removed before granting Device Owner." -ForegroundColor Yellow
    Write-Host "Please open Settings -> Accounts on the device and remove all Google/Samsung accounts." -ForegroundColor Yellow
    $confirm = Read-Host "Have you removed all accounts? (Y/N)"
    if ($confirm -ne "Y" -and $confirm -ne "y") {
        Exit 1
    }
}

# 3. Check if Child APK is installed
Write-Host "`n[3/4] Checking if Child App package is installed..." -ForegroundColor Yellow
$installed = adb shell pm list packages org.familyguard.child
if (-not $installed) {
    Write-Host "[INFO] Package org.familyguard.child not found. Installing..." -ForegroundColor Yellow
    if (Test-Path "child_app\app\build\outputs\apk\debug\app-debug.apk") {
        adb install -r child_app\app\build\outputs\apk\debug\app-debug.apk
    } else {
        Write-Host "[NOTE] Please install the child app APK first using Android Studio or adb install." -ForegroundColor Yellow
    }
}

# 4. Set Device Owner
Write-Host "`n[4/4] Setting Device Owner privilege..." -ForegroundColor Yellow
$result = adb shell dpm set-device-owner org.familyguard.child/.dpc.ParentalDpcReceiver
Write-Host $result

if ($result -match "Success") {
    Write-Host "`n[SUCCESS] FamilyGuard Child App is now provisioned as Device Owner (DPC)!" -ForegroundColor Green
    Write-Host "Tamper-proof app suspension and enterprise controls are now active." -ForegroundColor Green
} else {
    Write-Host "`n[TROUBLESHOOTING]" -ForegroundColor Yellow
    Write-Host "- If 'already some accounts', remove accounts or wipe device."
    Write-Host "- If 'already some users', ensure only primary User 0 exists."
}
