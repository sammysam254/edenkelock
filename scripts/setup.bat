@echo off
REM Eden M-Kopa Setup Script for Windows
REM This script helps set up the development environment

echo 🚀 Eden M-Kopa Setup Script
echo ================================

echo Checking prerequisites...

REM Check Node.js
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Node.js is not installed. Please install Node.js 18+ first.
    exit /b 1
)
echo ✅ Node.js installed

REM Check Python
where python >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Python is not installed. Please install Python 3.11+ first.
    exit /b 1
)
echo ✅ Python installed

REM Check npm
where npm >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ npm is not installed.
    exit /b 1
)
echo ✅ npm installed

echo.
echo Setting up Dashboard...
cd dashboard
if not exist ".env.local" (
    copy .env.local.example .env.local
    echo ⚠️  Please edit dashboard\.env.local with your Supabase credentials
)
call npm install
echo ✅ Dashboard dependencies installed
cd ..

echo.
echo Setting up Backend...
cd backend
if not exist ".env" (
    copy .env.example .env
    echo ⚠️  Please edit backend\.env with your credentials
)
python -m venv venv
call venv\Scripts\activate
pip install -r requirements.txt
echo ✅ Backend dependencies installed
cd ..

echo.
echo Setting up Provisioning...
cd provisioning
pip install -r requirements.txt
echo ✅ Provisioning dependencies installed
cd ..

echo.
echo ================================
echo ✅ Setup complete!
echo.
echo Next steps:
echo 1. Set up your Supabase project and run SQL scripts in database/
echo 2. Edit dashboard\.env.local with your Supabase credentials
echo 3. Edit backend\.env with your credentials
echo 4. Run 'cd dashboard && npm run dev' to start the dashboard
echo 5. Run 'cd backend && python main.py' to start the backend
echo.
echo For Android app, open the android/ folder in Android Studio
echo.
echo See docs\DEPLOYMENT.md for detailed deployment instructions

pause
