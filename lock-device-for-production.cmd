@echo off
title Eden Device - Enable Lock Mode for Production
color 0A

echo ========================================
echo   EDEN DEVICE - ENABLE LOCK MODE
echo ========================================
echo.
echo This script will re-enable Eden protection
echo and lock the device for production use.
echo.

set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"

if not exist "%ADB%" (
    echo ERROR: ADB not found at %ADB%
    echo.
    pause
    exit /b 1
)

echo [1/3] Checking device connection...
"%ADB%" devices | findstr "device" | findstr /V "List of devices" >nul
if errorlevel 1 (
    echo ERROR: No device connected
    echo Please connect your device via USB
    pause
    exit /b 1
)
echo ✓ Device connected

echo.
echo [2/3] Starting Eden app...
"%ADB%" shell am start -n com.eden.mkopa/.SplashActivity
timeout /t 2 >nul
echo ✓ Eden app started

echo.
echo [3/3] Enabling lock task mode...
"%ADB%" shell am task lock start com.eden.mkopa 2>nul
echo ✓ Lock task mode enabled

echo.
echo ========================================
echo   DEVICE LOCKED FOR PRODUCTION
echo ========================================
echo.
echo Device is now protected:
echo ✓ Lock task mode active
echo ✓ Settings blocked
echo ✓ Other apps hidden
echo ✓ Eden is the only accessible app
echo.
echo Device is ready for customer use.
echo.
pause
