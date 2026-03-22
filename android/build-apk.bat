@echo off
echo ========================================
echo Building Eden Android APK
echo ========================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java JDK 17 or higher
    echo Download from: https://adoptium.net/
    pause
    exit /b 1
)

echo Java found!
echo.

REM Check if local.properties exists
if not exist "local.properties" (
    echo ERROR: local.properties not found!
    echo.
    echo Please run setup-sdk.bat first to configure Android SDK
    echo.
    pause
    exit /b 1
)

echo Android SDK configured!
echo.

echo Building APK...
echo.

REM Clean build
call gradlew.bat clean

REM Build debug APK (faster, for testing)
call gradlew.bat assembleDebug

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo Debug APK location:
    echo app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo To build release APK, run:
    echo gradlew.bat assembleRelease
    echo.
) else (
    echo.
    echo ========================================
    echo BUILD FAILED!
    echo ========================================
    echo.
    echo Please check the error messages above
    echo.
)

pause
