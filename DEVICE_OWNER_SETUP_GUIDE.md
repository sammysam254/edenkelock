# 🔧 Device Owner Setup Guide - Eden v1.8.1

## ✅ SYSTEM READY FOR DEVICE OWNER SETUP

**Status**: 🚀 **ALL SYSTEMS READY**  
**APK Version**: v1.8.1 (versionCode 10)  
**Server**: All routes working  
**Database**: Ready for setup  

---

## 📋 PREREQUISITES CHECKLIST

### Before We Start:
- ✅ **APK Built**: v1.8.1 ready for download
- ✅ **Server Live**: https://eden-mkopa.onrender.com
- ✅ **Routes Fixed**: /login, /register, all API endpoints working
- ✅ **ADB Ready**: We'll set up ADB together
- ✅ **Device Ready**: Android device for testing

---

## 🛠️ ADB SETUP PROCESS

### Step 1: Install ADB Tools
```bash
# Download Android SDK Platform Tools
# Extract to a folder (e.g., C:\adb)
# Add to PATH environment variable
```

### Step 2: Enable Developer Options on Device
1. Go to **Settings** → **About Phone**
2. Tap **Build Number** 7 times
3. Go back to **Settings** → **Developer Options**
4. Enable **USB Debugging**
5. Enable **OEM Unlocking** (if available)

### Step 3: Connect Device and Verify ADB
```bash
# Connect device via USB
adb devices
# Should show your device listed
```

### Step 4: Factory Reset Device (Required for Device Owner)
```bash
# Device must be factory reset to set up Device Owner
adb reboot recovery
# Or manually factory reset through settings
```

---

## 🔐 DEVICE OWNER SETUP COMMANDS

### Method 1: Direct ADB Command (Preferred)
```bash
# After factory reset, during initial setup
# Skip Google account setup if possible
adb shell dpm set-device-owner com.eden.mkopa/.DeviceAdminReceiver
```

### Method 2: QR Code Provisioning (Alternative)
```bash
# Generate QR code with provisioning data
# Scan during device setup wizard
```

### Method 3: Manual Installation + Owner Setup
```bash
# Install APK first
adb install app/eden-v1.8.1.apk

# Then set as device owner
adb shell dpm set-device-owner com.eden.mkopa/.DeviceAdminReceiver
```

---

## 🎯 VERIFICATION COMMANDS

### Check Device Owner Status:
```bash
adb shell dpm list-owners
# Should show: com.eden.mkopa/.DeviceAdminReceiver
```

### Check App Installation:
```bash
adb shell pm list packages | grep eden
# Should show: package:com.eden.mkopa
```

### Check Device Admin Status:
```bash
adb shell dpm list-device-admins
# Should show Eden app as active admin
```

---

## 🔒 TEST SECURITY FEATURES

### Test Factory Reset Protection:
```bash
# Try to access recovery mode
adb reboot recovery
# Should be blocked or limited

# Try factory reset via settings
# Settings app should be hidden/restricted
```

### Test App Uninstall Protection:
```bash
# Try to uninstall Eden app
adb uninstall com.eden.mkopa
# Should fail with "Device owner cannot be uninstalled"
```

### Test Device Restrictions:
```bash
# Check active restrictions
adb shell dpm get-restrictions
# Should show comprehensive restrictions
```

---

## 📱 APK DOWNLOAD & INSTALLATION

### Download Latest APK:
- **Direct URL**: https://eden-mkopa.onrender.com/download/eden.apk
- **Version**: v1.8.1 (versionCode 10)
- **Size**: ~5.25MB

### Installation Commands:
```bash
# Download APK to computer
curl -O https://eden-mkopa.onrender.com/download/eden.apk

# Install via ADB
adb install eden.apk

# Or install and grant permissions
adb install -g eden.apk
```

---

## 🌐 SYSTEM VERIFICATION

### Test Web Access:
- **Homepage**: https://eden-mkopa.onrender.com ✅
- **Login**: https://eden-mkopa.onrender.com/login ✅
- **Register**: https://eden-mkopa.onrender.com/register ✅
- **APK Download**: https://eden-mkopa.onrender.com/download/eden.apk ✅

### Test Database Setup:
1. Run `FRESH_AUTH_SYSTEM_COMPLETE.sql` in Supabase
2. Register super admin at `/register`
3. Create test customer via admin dashboard
4. Test customer login on device

---

## 🚨 TROUBLESHOOTING

### Device Owner Setup Fails:
```bash
# Check if device is properly reset
adb shell getprop ro.setupwizard.mode
# Should show setup is active

# Check for existing device owners
adb shell dpm list-owners
# Should be empty before setup

# Remove existing device admin if needed
adb shell dpm remove-active-admin com.eden.mkopa/.DeviceAdminReceiver
```

### ADB Connection Issues:
```bash
# Restart ADB server
adb kill-server
adb start-server

# Check USB debugging is enabled
adb devices
# Should show device as "device" not "unauthorized"
```

### App Installation Issues:
```bash
# Enable unknown sources
adb shell settings put secure install_non_market_apps 1

# Check available storage
adb shell df /data

# Force install
adb install -r -d eden.apk
```

---

## 🎉 SUCCESS INDICATORS

### Device Owner Setup Successful:
- ✅ `adb shell dpm list-owners` shows Eden app
- ✅ Settings app is hidden/restricted
- ✅ Factory reset options are blocked
- ✅ App cannot be uninstalled
- ✅ Device restrictions are active

### App Functionality Working:
- ✅ Customer can login with phone + PIN
- ✅ Device locks when balance outstanding
- ✅ Security violations are logged
- ✅ Factory reset attempts are blocked
- ✅ All protection services running

---

## 📞 READY TO PROCEED

**Everything is now ready for device owner setup via ADB!**

### What We Have:
- ✅ **APK v1.8.1** - Built and ready
- ✅ **Server Live** - All routes working
- ✅ **Database Schema** - Ready for setup
- ✅ **Security Features** - Maximum protection active
- ✅ **ADB Commands** - Ready to execute

### Next Steps:
1. **Set up ADB** on your computer
2. **Factory reset** the test device
3. **Run device owner commands** together
4. **Test all security features**
5. **Verify factory reset protection**

**Let's set up device ownership together via ADB!**

---

*Device Owner Setup Guide for Eden v1.8.1*  
*Ready for maximum security device financing deployment*