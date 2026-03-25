@echo off
title Eden Device - Exit Lock Mode for Testing
color 0E

echo ========================================
echo   EDEN DEVICE - EXIT LOCK MODE
echo ========================================
echo.
echo This script will temporarily exit lock task mode
echo so you can access Settings and other apps for testing.
echo.

set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"

if not exist "%ADB%" (
    echo ERROR: ADB not found at %ADB%
    echo.
    pause
    exit /b 1
)

echo [1/4] Checking device connection...
"%ADB%" devices | findstr "device" | findstr /V "List of devices" >nul
if errorlevel 1 (
    echo ERROR: No device connected
    echo Please connect your device via USB
    pause
    exit /b 1
)
echo ✓ Device connected

echo.
echo [2/4] Stopping lock task mode...
"%ADB%" shell am task lock stop 2>nul
echo ✓ Lock task mode stopped

echo.
echo [3/4] Stopping Eden app...
"%ADB%" shell am force-stop com.eden.mkopa
echo ✓ Eden app stopped

echo.
echo [4/4] Opening Settings...
"%ADB%" shell am start -a android.settings.SETTINGS
echo ✓ Settings opened

echo.
echo ========================================
echo   DEVICE UNLOCKED FOR TESTING
echo ========================================
echo.
echo You can now:
echo - Access Settings
echo - Use other apps
echo - Configure the device
echo.
echo To re-enable Eden protection:
echo 1. Open Eden app manually
echo 2. Or restart the device
echo.
echo IMPORTANT: Device is now unprotected!
echo Re-enable Eden when done testing.
echo.
pause
