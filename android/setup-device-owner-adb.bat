@echo off
echo ========================================
echo Eden Device Owner Setup via ADB
echo ========================================
echo.
echo REQUIREMENTS:
echo 1. Device must be factory reset
echo 2. No Google account added
echo 3. USB debugging enabled
echo 4. Device connected via USB
echo.
pause

echo.
echo [1/4] Checking ADB connection...
adb devices
if %errorlevel% neq 0 (
    echo ERROR: ADB not found or device not connected
    pause
    exit /b 1
)

echo.
echo [2/4] Installing Eden APK...
adb install -r ..\app\eden.apk
if %errorlevel% neq 0 (
    echo ERROR: Failed to install APK
    pause
    exit /b 1
)

echo.
echo [3/4] Setting Eden as Device Owner...
adb shell dpm set-device-owner com.eden.mkopa/.DeviceAdminReceiver
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Failed to set Device Owner
    echo.
    echo Common reasons:
    echo - Google account is added (must factory reset without adding account)
    echo - Device has multiple users
    echo - Another device owner already exists
    echo.
    pause
    exit /b 1
)

echo.
echo [4/4] Launching Eden app...
adb shell am start -n com.eden.mkopa/.MainActivity

echo.
echo ========================================
echo SUCCESS! Device Owner Setup Complete
echo ========================================
echo.
echo The device is now locked in kiosk mode.
echo.
echo Verification:
adb shell dumpsys device_policy | findstr "Device Owner"
echo.
echo The device should now:
echo - Start in kiosk mode
echo - Block back/home buttons
echo - Block factory reset
echo - Block ADB (after reboot)
echo - Prevent app uninstall
echo.
pause
