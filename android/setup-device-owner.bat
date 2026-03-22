@echo off
echo ========================================
echo Eden Device Owner Setup
echo ========================================
echo.
echo This script will set up Eden as Device Owner
echo.
echo REQUIREMENTS:
echo 1. Device must be factory reset
echo 2. No Google account added
echo 3. USB debugging enabled
echo 4. Device connected via USB
echo.
pause

echo.
echo Step 1: Checking ADB connection...
adb devices
if %errorlevel% neq 0 (
    echo ERROR: ADB not found or device not connected
    pause
    exit /b 1
)

echo.
echo Step 2: Installing Eden APK...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %errorlevel% neq 0 (
    echo ERROR: Failed to install APK
    pause
    exit /b 1
)

echo.
echo Step 3: Setting Eden as Device Owner...
adb shell dpm set-device-owner com.eden.mkopa/.DeviceAdminReceiver
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Failed to set device owner
    echo.
    echo Common issues:
    echo - Device not factory reset
    echo - Google account already added
    echo - Another device owner already set
    echo - USB debugging not enabled
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo SUCCESS!
echo ========================================
echo.
echo Eden is now Device Owner
echo.
echo The device will now:
echo - Block factory reset
echo - Block ADB access
echo - Block uninstall
echo - Survive factory reset
echo.
echo Starting Eden app...
adb shell am start -n com.eden.mkopa/.MainActivity

echo.
echo Setup complete!
pause
