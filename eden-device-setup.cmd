@echo off
setlocal enabledelayedexpansion
title Eden M-Kopa Device Provisioning Setup
color 0A

echo.
echo ========================================
echo    EDEN M-KOPA DEVICE SETUP TOOL
echo ========================================
echo.
echo This tool will automatically provision your Android device
echo for Eden M-Kopa device ownership and install the app.
echo.
echo REQUIREMENTS:
echo - Android device with USB debugging enabled
echo - USB cable connected to computer
echo - Device must be factory reset (recommended)
echo.

:CHECK_ADB
echo [1/8] Checking ADB installation...
adb version >nul 2>&1
if errorlevel 1 (
    echo ERROR: ADB not found in system PATH
    echo.
    echo Please install Android SDK Platform Tools:
    echo 1. Download from: https://developer.android.com/studio/releases/platform-tools
    echo 2. Extract to a folder
    echo 3. Add the folder to your system PATH
    echo 4. Restart this script
    echo.
    pause
    exit /b 1
)
echo ✓ ADB found and ready

:DEVICE_CONNECTION
echo.
echo [2/8] Checking device connection...
echo.
echo Please ensure your Android device is:
echo - Connected via USB cable
echo - USB Debugging is enabled in Developer Options
echo - Screen is unlocked
echo.
pause

adb devices > temp_devices.txt 2>&1
findstr /C:"device" temp_devices.txt | findstr /V /C:"List of devices" > connected_devices.txt

set device_count=0
for /f %%i in (connected_devices.txt) do set /a device_count+=1

if %device_count%==0 (
    echo.
    echo ❌ No devices detected
    echo.
    echo TROUBLESHOOTING:
    echo 1. Check USB cable connection
    echo 2. Enable Developer Options: Settings > About Phone > Tap Build Number 7 times
    echo 3. Enable USB Debugging: Settings > Developer Options > USB Debugging
    echo 4. Unlock device screen
    echo.
    set /p retry="Try again? (y/n): "
    if /i "!retry!"=="y" goto DEVICE_CONNECTION
    exit /b 1
)

if %device_count% gtr 1 (
    echo.
    echo ⚠️  Multiple devices detected. Please connect only one device.
    echo.
    pause
    goto DEVICE_CONNECTION
)

echo ✓ Device detected and connected

:ADB_AUTHORIZATION
echo.
echo [3/8] Checking ADB authorization...

adb shell echo "test" >nul 2>&1
if errorlevel 1 (
    echo.
    echo ⚠️  ADB authorization required
    echo.
    echo PLEASE CHECK YOUR DEVICE:
    echo A popup should appear asking "Allow USB debugging?"
    echo.
    set /p auth_visible="Can you see the authorization popup on your device? (y/n): "
    
    if /i "!auth_visible!"=="n" (
        echo.
        echo MANUAL STEPS:
        echo 1. Disconnect and reconnect USB cable
        echo 2. Make sure device screen is unlocked
        echo 3. Look for "Allow USB debugging?" popup
        echo 4. Check "Always allow from this computer"
        echo 5. Tap "OK" or "Allow"
        echo.
        set /p auth_done="Have you completed these steps? (y/n): "
        if /i "!auth_done!"=="n" (
            echo Please complete the authorization and restart this script.
            pause
            exit /b 1
        )
    ) else (
        echo.
        echo Please:
        echo 1. Check "Always allow from this computer"
        echo 2. Tap "OK" or "Allow"
        echo.
        pause
    )
    
    echo Verifying authorization...
    timeout /t 3 >nul
    adb shell echo "test" >nul 2>&1
    if errorlevel 1 (
        echo ❌ Authorization failed. Please restart the script and try again.
        pause
        exit /b 1
    )
)

echo ✓ ADB authorized successfully

:CHECK_ACCOUNTS
echo.
echo [4/8] Checking device accounts...

adb shell dumpsys account | findstr "Account {" > temp_accounts.txt 2>nul
set account_count=0
for /f %%i in (temp_accounts.txt) do set /a account_count+=1

if %account_count% gtr 0 (
    echo.
    echo ⚠️  Google/Samsung accounts detected on device
    echo.
    echo For proper device ownership setup, accounts should be removed:
    echo 1. Go to Settings > Accounts
    echo 2. Remove all Google/Samsung accounts
    echo 3. Or perform a factory reset for clean setup
    echo.
    echo IMPORTANT: Device ownership works best on factory reset devices
    echo.
    set /p continue_anyway="Continue anyway? (y/n): "
    if /i "!continue_anyway!"=="n" (
        echo.
        echo Please remove accounts or factory reset, then restart this script.
        pause
        exit /b 1
    )
    echo ⚠️  Continuing with existing accounts (may cause issues)
) else (
    echo ✓ No conflicting accounts found
)

:DEVICE_OWNER_SETUP
echo.
echo [5/8] Setting up device ownership...
echo.
echo This will make Eden the device owner with full control.
echo The device will be managed and secured by Eden M-Kopa.
echo.

echo Removing existing device owner...
adb shell dpm remove-active-admin com.eden.mkopa/.DeviceAdminReceiver >nul 2>&1

echo Setting Eden as device owner...
adb shell dpm set-device-owner com.eden.mkopa/.DeviceAdminReceiver 2>error.txt

if errorlevel 1 (
    echo.
    echo ❌ Device owner setup failed
    echo.
    type error.txt 2>nul
    echo.
    echo COMMON SOLUTIONS:
    echo 1. Factory reset the device completely
    echo 2. Remove all Google/Samsung accounts
    echo 3. Ensure device is not enrolled in any MDM
    echo 4. Try setup immediately after factory reset
    echo.
    echo If error mentions "already has device owner", run:
    echo adb shell dpm remove-active-admin com.android.deviceowner/.DeviceAdminReceiver
    echo.
    pause
    exit /b 1
)

echo ✓ Device ownership established successfully

:INSTALL_APP
echo.
echo [6/8] Installing Eden M-Kopa app...

if not exist "app\eden.apk" (
    echo.
    echo ❌ Eden APK not found
    echo.
    echo Please ensure eden.apk is in the app\ folder
    echo You can download it from the Eden dashboard
    echo.
    pause
    exit /b 1
)

echo Installing APK...
adb install -r "app\eden.apk" 2>install_error.txt

if errorlevel 1 (
    echo.
    echo ❌ App installation failed
    echo.
    type install_error.txt 2>nul
    echo.
    echo Please check the error above and try again
    pause
    exit /b 1
)

echo ✓ Eden app installed successfully

:CONFIGURE_PERMISSIONS
echo.
echo [7/8] Configuring app permissions...

echo Granting device admin permissions...
adb shell dpm set-active-admin com.eden.mkopa/.DeviceAdminReceiver >nul 2>&1

echo Granting system permissions...
adb shell pm grant com.eden.mkopa android.permission.WRITE_EXTERNAL_STORAGE >nul 2>&1
adb shell pm grant com.eden.mkopa android.permission.READ_EXTERNAL_STORAGE >nul 2>&1
adb shell pm grant com.eden.mkopa android.permission.READ_PHONE_STATE >nul 2>&1
adb shell pm grant com.eden.mkopa android.permission.ACCESS_NETWORK_STATE >nul 2>&1
adb shell pm grant com.eden.mkopa android.permission.INTERNET >nul 2>&1

echo Setting app as launcher...
adb shell cmd package set-home-activity com.eden.mkopa/.MainActivity >nul 2>&1

echo ✓ Permissions configured

:FINAL_SETUP
echo.
echo [8/8] Finalizing setup...

echo Starting Eden app...
adb shell am start -n com.eden.mkopa/.SplashActivity >nul 2>&1

echo Applying security policies...
adb shell dpm set-user-restriction com.eden.mkopa/.DeviceAdminReceiver no_install_unknown_sources true >nul 2>&1
adb shell dpm set-user-restriction com.eden.mkopa/.DeviceAdminReceiver no_uninstall_apps true >nul 2>&1

echo.
echo ========================================
echo        SETUP COMPLETED SUCCESSFULLY!
echo ========================================
echo.
echo ✓ Device ownership established
echo ✓ Eden M-Kopa app installed
echo ✓ Security policies applied
echo ✓ Device is now managed by Eden
echo.
echo NEXT STEPS:
echo 1. The Eden app should now be running on your device
echo 2. Customer will enter their phone number to register
echo 3. System will verify enrollment status
echo 4. Customer will set their 4-digit PIN
echo 5. Device will be activated and ready for use
echo.
echo IMPORTANT NOTES:
echo - Device is now locked to Eden M-Kopa system
echo - Factory reset protection is active
echo - Only authorized apps can be installed
echo - Device will be remotely manageable
echo.
echo For support, contact Eden M-Kopa technical team.
echo.

:CLEANUP
del temp_devices.txt >nul 2>&1
del connected_devices.txt >nul 2>&1
del temp_accounts.txt >nul 2>&1
del error.txt >nul 2>&1
del install_error.txt >nul 2>&1

echo Setup completed at %date% %time%
echo.
pause