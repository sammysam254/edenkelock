# Eden Device Owner Setup - CRITICAL

## ⚠️ IMPORTANT: Device Owner is REQUIRED

The Eden app CANNOT lock the device, block factory reset, or disable ADB without being set as **Device Owner**.

Regular app installation will NOT work for device locking!

## Why Device Owner is Required

Android security prevents regular apps from:
- Locking the device in kiosk mode
- Blocking factory reset
- Disabling ADB debugging
- Preventing app uninstallation
- Surviving factory reset

Only a **Device Owner** app has these powers.

## How to Set Up Device Owner

### Method 1: QR Code Provisioning (Recommended)

1. **Factory Reset the Device**
   - Go to Settings → System → Reset → Factory Reset
   - Complete the reset

2. **During Initial Setup**
   - When you see the welcome screen
   - Tap the screen 6 times quickly
   - A QR code scanner will appear

3. **Generate QR Code**
   - Admin logs into Eden dashboard
   - Goes to device enrollment
   - System generates QR code with device ID

4. **Scan QR Code**
   - Device scans the QR code
   - Eden app downloads and installs automatically
   - Device Owner is set automatically
   - All restrictions are applied immediately

5. **Device is Now Locked**
   - App starts in kiosk mode
   - Back/Home buttons disabled
   - Factory reset blocked
   - ADB disabled
   - App cannot be uninstalled

### Method 2: ADB Command (For Testing)

**Requirements:**
- Device must be factory reset
- No Google account added
- USB debugging enabled (will be disabled after)
- ADB installed on computer

**Steps:**

1. Factory reset device
2. Skip all setup steps (don't add Google account)
3. Enable USB debugging
4. Connect device to computer
5. Install Eden APK:
   ```
   adb install eden.apk
   ```
6. Set as Device Owner:
   ```
   adb shell dpm set-device-owner com.eden.mkopa/.DeviceAdminReceiver
   ```
7. Launch app:
   ```
   adb shell am start -n com.eden.mkopa/.MainActivity
   ```
8. Device is now locked

## Verification

After setup, verify Device Owner status:

```bash
adb shell dumpsys device_policy | grep "Device Owner"
```

Should show: `Device Owner: ComponentInfo{com.eden.mkopa/com.eden.mkopa.DeviceAdminReceiver}`

## What Happens After Device Owner Setup

✅ App starts in kiosk mode automatically
✅ Back and Home buttons are disabled
✅ Factory reset option is hidden
✅ ADB debugging is blocked
✅ App cannot be uninstalled
✅ Device survives factory reset (app reinstalls)
✅ Customer must login to use device
✅ Admin can lock/unlock remotely

## Common Issues

### "App doesn't lock the device"
- Device Owner is NOT set
- Follow setup steps above

### "Can still press back/home button"
- Device Owner is NOT set
- Regular device admin is NOT enough

### "Can factory reset from settings"
- Device Owner is NOT set
- Must use QR provisioning or ADB method

### "Can uninstall the app"
- Device Owner is NOT set
- Device Owner apps cannot be uninstalled

## Testing Device Locking

1. Set up Device Owner (QR or ADB method)
2. Open Eden app
3. App should start in kiosk mode immediately
4. Try pressing back button - should not work
5. Try pressing home button - should not work
6. Try going to settings - should not be able to
7. Device is locked to Eden app only

## Production Deployment

For customer devices:

1. Factory reset all devices
2. Use QR code provisioning during setup
3. Admin scans QR from dashboard
4. Device auto-configures as Device Owner
5. Customer receives locked device
6. Customer logs in with phone + PIN
7. Device remains locked until loan paid

## Security Notes

- Device Owner cannot be removed without factory reset
- Even after factory reset, app reinstalls automatically
- Only way to remove: Factory reset + don't scan QR code
- Admin has full control over device
- Customer cannot bypass restrictions

## Support

If device locking doesn't work:
1. Verify Device Owner status with ADB command
2. If not Device Owner, factory reset and provision again
3. Regular installation will NEVER enable device locking
