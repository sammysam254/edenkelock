@echo off
echo ========================================
echo Eden Android APK - SDK Setup
echo ========================================
echo.

REM Check if Java is installed
echo [1/4] Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo.
    echo Please install Java JDK 17 from:
    echo https://adoptium.net/temurin/releases/?version=17
    echo.
    echo After installation, restart this script.
    pause
    exit /b 1
)
echo Java is installed ✓
echo.

REM Check if Android SDK is installed
echo [2/4] Checking Android SDK...
if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
    echo Android SDK found at: %ANDROID_HOME% ✓
    goto :sdk_found
)

if exist "%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
    set "ANDROID_HOME=%USERPROFILE%\AppData\Local\Android\Sdk"
    echo Android SDK found at: %ANDROID_HOME% ✓
    goto :sdk_found
)

if exist "C:\Android\Sdk\platform-tools\adb.exe" (
    set "ANDROID_HOME=C:\Android\Sdk"
    echo Android SDK found at: %ANDROID_HOME% ✓
    goto :sdk_found
)

echo.
echo ERROR: Android SDK not found!
echo.
echo You have 2 options:
echo.
echo OPTION 1 - Install Android Studio (Recommended, ~1GB download)
echo   1. Download from: https://developer.android.com/studio
echo   2. Install with default settings
echo   3. Open Android Studio
echo   4. Go to: Tools ^> SDK Manager
echo   5. Install "Android SDK Platform 34"
echo   6. Restart this script
echo.
echo OPTION 2 - Install Command Line Tools Only (~200MB)
echo   1. Download from: https://developer.android.com/studio#command-tools
echo   2. Extract to: C:\Android\Sdk\cmdline-tools\latest
echo   3. Open Command Prompt as Administrator
echo   4. Run: C:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
echo   5. Restart this script
echo.
echo OPTION 3 - Use Online Build Service (No SDK needed)
echo   We can use GitHub Actions or other CI/CD to build the APK
echo.
pause
exit /b 1

:sdk_found
echo [3/4] Creating local.properties...
echo sdk.dir=%ANDROID_HOME:\=\\% > local.properties
echo Created local.properties ✓
echo.

echo [4/4] Setup complete!
echo.
echo You can now build the APK using:
echo   build-apk.bat
echo.
pause
