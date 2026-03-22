# 🔒 Eden Device Locking - Setup Guide

## ⚠️ CRITICAL: Why Device Isn't Locking

The Eden app **CANNOT** lock the device after regular installation!

### The Problem
Android security prevents regular apps from:
- ❌ Locking device in kiosk mode
- ❌ Blocking factory reset
- ❌ Disabling ADB
- ❌ Preventing uninstallation
- ❌ Disabling back/home buttons

### The Solution
The app MUST be set as **Device Owner** during initial device setup.

## 🚀 Quick Setup (2 Methods)

### Method 1: QR Code Provisioning (Production)

**For Customer Devices:**

1. **Enroll Device in Dashboard**
   - Admin/Super Admin logs into dashboard
   - Fills out enrollment form with customer details
   - Uploads KYC documents (ID front/back, passport photo)
   - Submits enrollment
   - **QR code appears automatically after enrollment**

2. **Factory Reset Device**
   ```
   Settings → System → Reset → Factory Data Reset
   ```

3. **Start Setup Wizard**
   - Device boots to welcome screen
   - **Tap screen 6 times quickly**
   - Android's built-in QR scanner appears

4. **Scan QR Code from Dashboard**
   - Point device camera at QR code on admin dashboard
   - Device scans QR code automatically
   - Eden app downloads automatically from server
   - Device Owner is set automatically
   - Device restarts (1-2 minutes)

5. **Done!**
   - Device is now locked
   - Eden app opens in kiosk mode
   - Customer can only use Eden app
   - All restrictions active

**For Existing Devices:**
- Click "Generate QR" button next to device in dashboard
- Follow steps 2-5 above

### Method 2: ADB Command (Testing)

**For Development/Testing:**

1. **Factory Reset Device**
   - Don't add Google account
   - Skip all setup steps

2. **Enable USB Debugging**
   ```
   Settings → About → Tap Build Number 7 times
   Settings → Developer Options → USB Debugging ON
   ```

3. **Connect to Computer**
   ```bash
   # Check connection
   adb devices
   
   # Install APK
   adb install app/eden.apk
   
   # Set as Device Owner
   adb shell dpm set-device-owner com.eden.mkopa/.DeviceAdminReceiver
   
   # Launch app
   adb shell am start -n com.eden.mkopa/.MainActivity
   ```

4. **Done!**
   - Device is now locked
   - Kiosk mode active

## ✅ Verify Device Owner Status

```bash
adb shell dumpsys device_policy | grep "Device Owner"
```

Should show:
```
Device Owner: ComponentInfo{com.eden.mkopa/com.eden.mkopa.DeviceAdminReceiver}
```

## 🔐 What Happens After Setup

Once Device Owner is set:

✅ **Kiosk Mode** - App fills entire screen
✅ **No Back Button** - Back button disabled
✅ **No Home Button** - Home button disabled  
✅ **No Factory Reset** - Option hidden in settings
✅ **No ADB** - Debugging blocked
✅ **No Uninstall** - App cannot be removed
✅ **Survives Reset** - App reinstalls after factory reset
✅ **Remote Lock** - Admin can lock/unlock from dashboard
✅ **Customer Login** - Must login to use device

## 🎯 Testing Device Locking

After Device Owner setup:

1. Open Eden app
2. Try pressing **Back** - Should not work
3. Try pressing **Home** - Should not work
4. Try opening **Settings** - Should not be able to
5. Try **Factory Reset** - Option should be hidden
6. Try **Uninstalling** - Should fail

If any of these work, Device Owner is NOT set correctly.

## 🏭 Production Workflow

**For Each Customer Device:**

1. Admin logs into dashboard
2. Enrolls device with customer details and KYC documents
3. QR code appears automatically after enrollment
4. Factory reset the device
5. During setup wizard, tap screen 6 times
6. Android's QR scanner appears
7. Scan QR code from dashboard
8. Device auto-configures and provisions
9. Device restarts with Eden as Device Owner
10. Hand device to customer
11. Customer logs in with phone + PIN
12. Device is locked and ready

**For Existing Enrolled Devices:**
1. Click "Generate QR" button in dashboard
2. Factory reset device
3. Follow steps 5-9 above

## 🐛 Troubleshooting

### "Device doesn't lock after install"
**Cause:** Device Owner not set
**Fix:** Must factory reset and use QR provisioning or ADB method

### "Can still press back/home button"
**Cause:** Device Owner not set (regular device admin is not enough)
**Fix:** Factory reset and provision correctly

### "Can factory reset from settings"
**Cause:** Device Owner not set
**Fix:** Use QR provisioning or ADB method

### "Can uninstall the app"
**Cause:** Device Owner not set
**Fix:** Device Owner apps cannot be uninstalled

### "ADB command fails"
**Error:** `Not allowed to set the device owner because...`
**Causes:**
- Google account is added (must skip during setup)
- Device has multiple users
- Another device owner exists
**Fix:** Factory reset without adding Google account

## 📱 Customer Experience

**After Device Owner Setup:**

1. Device boots → Eden app opens automatically
2. Customer sees login screen
3. Enters phone number + PIN
4. Sees loan balance and payment info
5. Can click "Unlock Device" button
6. Device unlocks (exits kiosk mode)
7. Customer can use device normally
8. If payment overdue, admin locks device remotely
9. Device locks within 1 minute
10. Customer must pay to unlock

## 🔧 Admin Controls

**From Dashboard:**

- **Lock Device** - Locks device remotely (1 min delay)
- **Unlock Device** - Unlocks device remotely (1 min delay)
- **View Status** - See if device is locked/unlocked
- **Track Payments** - Monitor payment history
- **Generate QR** - Create provisioning QR for new devices

## 📋 Deployment Checklist

Before deploying to customers:

- [ ] Device is factory reset
- [ ] QR code provisioning tested
- [ ] Device locks after provisioning
- [ ] Back/Home buttons disabled
- [ ] Factory reset blocked
- [ ] App cannot be uninstalled
- [ ] Customer login works
- [ ] Unlock button works
- [ ] Admin lock/unlock works
- [ ] Background sync running (1 min interval)

## 🆘 Support

**If device locking doesn't work:**

1. Check Device Owner status with ADB
2. If not Device Owner, factory reset and provision again
3. Regular installation will NEVER enable locking
4. Device Owner setup is MANDATORY

**Contact:** sammyseth260@gmail.com
