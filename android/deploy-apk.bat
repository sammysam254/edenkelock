@echo off
echo ========================================
echo Deploying Eden APK to Server
echo ========================================
echo.

REM Check if APK exists
if not exist "..\static\apk\eden.apk" (
    echo ERROR: APK not found!
    echo Please build the APK first using: build-apk.bat
    pause
    exit /b 1
)

echo APK found: static/apk/eden.apk
echo.

REM Get APK size
for %%A in ("..\static\apk\eden.apk") do set size=%%~zA
set /a sizeMB=%size%/1048576
echo Size: %sizeMB% MB
echo.

echo Deploying to server...
echo.

cd ..

REM Add to git
git add static/apk/eden.apk
git add android/

REM Commit
git commit -m "Deploy Eden Android APK v1.0"

REM Push to GitHub (triggers Render deployment)
git push

echo.
echo ========================================
echo Deployment Complete!
echo ========================================
echo.
echo The APK will be available at:
echo https://eden-mkopa.onrender.com/download/eden.apk
echo.
echo Wait 2-3 minutes for Render to deploy.
echo.
pause
