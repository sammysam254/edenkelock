# Eden Device Setup Tool Guide

## Overview
The Eden Device Setup Tool (`eden-device-setup.cmd`) is an automated Windows command-line utility that provisions Android devices for Eden M-Kopa device ownership and installs the Eden app.

## Features
- ✅ Automatic ADB detection and device connection
- ✅ Interactive authorization handling
- ✅ Account conflict detection and guidance
- ✅ Automated device owner provisioning
- ✅ Eden app installation and configuration
- ✅ Security policy application
- ✅ Comprehensive error handling and troubleshooting

## Prerequisites

### System Requirements
- Windows computer with Command Prompt
- Android SDK Platform Tools (ADB) installed
- USB cable for device connection

### Device Requirements
- Android device (Android 6.0+ recommended)
- Factory reset recommended for best results
- USB Debugging enabled
- Developer Options unlocked

## Installation

### 1. Download ADB (if not installed)
1. Download Android SDK Platform Tools from: https://developer.android.com/studio/releases/platform-tools
2. Extract to a folder (e.g., `C:\platform-tools`)
3. Add the folder to your system PATH:
   - Open System Properties → Advanced → Environment Variables
   - Edit PATH variable and add the platform-tools folder
   - Restart Command Prompt

### 2. Download Setup Tool
1. Visit the Eden dashboard homepage
2. Click "🔧 Device Setup Tool" button
3. Save `eden-device-setup.cmd` to your computer
4. Ensure `eden.apk` is in the `app\` folder (download from dashboard)

## Usage Instructions

### Step 1: Prepare Device
1. **Factory Reset** (recommended):
   - Go to Settings → System → Reset → Factory Reset
   - Complete the initial setup but don't add Google accounts

2. **Enable Developer Options**:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Developer Options will appear in Settings

3. **Enable USB Debugging**:
   - Go to Settings → Developer Options
   - Enable "USB Debugging"
   - Connect device to computer via USB

### Step 2: Run Setup Tool
1. Right-click `eden-device-setup.cmd`
2. Select "Run as administrator" (recommended)
3. Follow the interactive prompts

### Step 3: Interactive Process
The tool will guide you through 8 steps:

1. **ADB Check** - Verifies ADB installation
2. **Device Connection** - Detects connected device
3. **ADB Authorization** - Handles USB debugging authorization
4. **Account Check** - Warns about existing accounts
5. **Device Owner Setup** - Establishes Eden as device owner
6. **App Installation** - Installs Eden M-Kopa app
7. **Permission Configuration** - Sets up required permissions
8. **Final Setup** - Applies security policies and starts app

## Troubleshooting

### Common Issues

#### "ADB not found"
- Install Android SDK Platform Tools
- Add platform-tools folder to system PATH
- Restart Command Prompt

#### "No devices detected"
- Check USB cable connection
- Enable USB Debugging in Developer Options
- Unlock device screen
- Try different USB port/cable

#### "Authorization failed"
- Look for "Allow USB debugging?" popup on device
- Check "Always allow from this computer"
- Tap "OK" or "Allow"
- If popup doesn't appear, disconnect and reconnect USB

#### "Device owner setup failed"
- Perform complete factory reset
- Remove all Google/Samsung accounts
- Ensure device is not enrolled in any MDM
- Try setup immediately after factory reset

#### "App installation failed"
- Ensure `eden.apk` is in the `app\` folder
- Check if device has sufficient storage
- Verify APK is not corrupted (re-download if needed)

### Advanced Troubleshooting

#### Multiple Devices
- Connect only one device at a time
- Disconnect other Android devices/emulators

#### Existing Device Owner
If error mentions "already has device owner":
```cmd
adb shell dpm remove-active-admin com.android.deviceowner/.DeviceAdminReceiver
```

#### Manual Commands
If automatic setup fails, you can run commands manually:
```cmd
# Check device connection
adb devices

# Set device owner
adb shell dpm set-device-owner com.eden.mkopa/.DeviceAdminReceiver

# Install app
adb install -r app\eden.apk

# Grant permissions
adb shell pm grant com.eden.mkopa android.permission.WRITE_EXTERNAL_STORAGE
```

## Post-Setup Process

After successful setup:

1. **Device Activation**:
   - Eden app will start automatically
   - Customer enters their phone number
   - System verifies enrollment status
   - Customer sets 4-digit PIN
   - Device becomes active and ready

2. **Security Features Active**:
   - Device owner permissions established
   - Factory reset protection enabled
   - App installation restrictions applied
   - Remote management capabilities active

3. **Customer Experience**:
   - Clean mobile-optimized dashboard
   - Loan balance and payment tracking
   - Secure PIN-based authentication
   - Automatic device locking for overdue payments

## Support

For technical support:
- Contact Eden M-Kopa technical team
- Check device logs: `adb logcat | findstr Eden`
- Verify setup: Visit `/debug` page on Eden dashboard

## Security Notes

⚠️ **Important Security Information**:
- Device becomes managed by Eden M-Kopa system
- Factory reset protection is automatically enabled
- Only authorized apps can be installed
- Device can be remotely locked/unlocked
- Customer data is protected and encrypted

This tool is designed for authorized Eden M-Kopa device provisioning only.