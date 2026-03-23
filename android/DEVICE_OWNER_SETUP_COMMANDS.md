# Device Owner Setup - Complete Command Reference

This document contains all the commands used to set up device owner mode for the Eden app.

## Prerequisites

1. Factory reset Android device
2. Do NOT add any Google or Samsung accounts during setup
3. Enable USB debugging in Developer Options
4. Install ADB Platform Tools on your computer

## Step 1: Install ADB Platform Tools (Windows)

```powershell
# Download ADB Platform Tools
$url = "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
$output = "$env:TEMP\platform-tools.zip"
$extractPath = "C:\platform-tools"

Write-Host "Downloading ADB Platform Tools..." -ForegroundColor Green
Invoke-WebRequest -Uri $url -OutFile $output

Write-Host "Extracting to C:\platform-tools..." -ForegroundColor Green
Expand-Archive -Path $output -DestinationPath "C:\" -Force

Write-Host "Cleaning up..." -ForegroundColor Green
Remove-Item $output

Write-Host "`nADB installed successfully at: C:\platform-tools" -ForegroundColor Green
```

## Step 2: Add ADB to PATH (Current Session)

```powershell
$env:Path += ";C:\platform-tools"
```

## Step 3: Test ADB Installation

```powershell
adb version
```

Expected output:
```
Android Debug Bridge version 1.0.41
Version 36.0.2-14143358
Installed as C:\platform-tools\adb.exe
```

## Step 4: Connect Device and Authorize

```powershell
# Check connected devices
adb devices
```

If device shows as "unauthorized":
1. Look at your device screen for "Allow USB debugging?" prompt
2. Tap "Allow" (optionally check "Always allow from this computer")

If prompt doesn't appear:
```powershell
# Restart ADB server
adb kill-server
Start-Sleep -Seconds 2
adb devices
```

Expected output when authorized:
```
List of devices attached
R8YWA0A09JW     device
```

## Step 5: Remove All Accounts (If Any)

```powershell
# Check for accounts
adb shell dumpsys account | Select-String -Pattern "Accounts:"
```

Expected output:
```
Accounts: 0
```

If accounts exist, remove them manually on the device:
- Settings → Accounts → Remove all accounts

## Step 6: Reboot Device (If Needed)

```powershell
# Reboot to clear account cache
adb reboot
```

Wait 30-60 seconds for device to restart, then verify connection:
```powershell
adb devices
```

## Step 7: Install Eden APK

```powershell
# Navigate to project directory
cd C:\lockd

# Install APK
adb install -r app\eden.apk
```

Expected output:
```
Performing Streamed Install
Success
```

## Step 8: Set Device Owner

```powershell
adb shell dpm set-device-owner com.eden.mkopa/.DeviceAdminReceiver
```

Expected output:
```
Success: Device owner set to package com.eden.mkopa/.DeviceAdminReceiver
Active admin set to component com.eden.mkopa/.DeviceAdminReceiver
```

## Step 9: Launch Eden App

```powershell
adb shell am start -n com.eden.mkopa/.PinEntryActivity
```

## Step 10: Verify Device Owner Status

```powershell
adb shell dumpsys device_policy | Select-String -Pattern "Device Owner"
```

Expected output:
```
Device Owner:
  Device Owner Type: 0
```

## Troubleshooting

### Error: "Not allowed to set device owner because there are already some accounts"

**Solution:**
1. Remove all accounts from Settings → Accounts
2. Reboot the device: `adb reboot`
3. Wait for device to restart
4. Retry Step 8

### Error: "Device unauthorized"

**Solution:**
1. Check device screen for USB debugging prompt
2. Restart ADB: `adb kill-server; adb devices`
3. Unplug and replug USB cable
4. Toggle USB debugging OFF and ON in Developer Options

### Error: "ADB not found"

**Solution:**
1. Verify ADB is installed: `Test-Path C:\platform-tools\adb.exe`
2. Add to PATH: `$env:Path += ";C:\platform-tools"`
3. Restart PowerShell

### Device has multiple users

**Solution:**
Device owner can only be set on devices with a single user. Factory reset and don't add additional users.

## Security Notes

- After device owner is set, the device is locked in kiosk mode
- Back and Home buttons are disabled
- Factory reset is blocked
- ADB will be blocked after reboot (for security)
- Only the Eden app can unlock the device

## Default PIN

The default PIN for the app is: **1234**

This should be changed in production by modifying `PinEntryActivity.kt`:
```kotlin
private val correctPin = "1234" // Change this
```

## Uninstalling Device Owner (For Testing)

```powershell
# Remove device owner
adb shell dpm remove-active-admin com.eden.mkopa/.DeviceAdminReceiver

# Uninstall app
adb uninstall com.eden.mkopa
```

## Production Deployment

For production deployment:
1. Change the default PIN
2. Configure backend API URL
3. Test all locking mechanisms
4. Verify payment sync functionality
5. Test lock screen behavior

## Support

For issues or questions, contact the Eden development team.
