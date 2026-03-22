#!/bin/bash

# Eden M-Kopa Setup Script
# This script helps set up the development environment

echo "🚀 Eden M-Kopa Setup Script"
echo "================================"

# Check prerequisites
echo "Checking prerequisites..."

# Check Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js 18+ first."
    exit 1
fi
echo "✅ Node.js $(node --version)"

# Check Python
if ! command -v python3 &> /dev/null; then
    echo "❌ Python is not installed. Please install Python 3.11+ first."
    exit 1
fi
echo "✅ Python $(python3 --version)"

# Check npm
if ! command -v npm &> /dev/null; then
    echo "❌ npm is not installed."
    exit 1
fi
echo "✅ npm $(npm --version)"

echo ""
echo "Setting up Dashboard..."
cd dashboard
if [ ! -f ".env.local" ]; then
    cp .env.local.example .env.local
    echo "⚠️  Please edit dashboard/.env.local with your Supabase credentials"
fi
npm install
echo "✅ Dashboard dependencies installed"
cd ..

echo ""
echo "Setting up Backend..."
cd backend
if [ ! -f ".env" ]; then
    cp .env.example .env
    echo "⚠️  Please edit backend/.env with your credentials"
fi
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
echo "✅ Backend dependencies installed"
cd ..

echo ""
echo "Setting up Provisioning..."
cd provisioning
pip install -r requirements.txt
echo "✅ Provisioning dependencies installed"
cd ..

echo ""
echo "================================"
echo "✅ Setup complete!"
echo ""
echo "Next steps:"
echo "1. Set up your Supabase project and run SQL scripts in database/"
echo "2. Edit dashboard/.env.local with your Supabase credentials"
echo "3. Edit backend/.env with your credentials"
echo "4. Run 'cd dashboard && npm run dev' to start the dashboard"
echo "5. Run 'cd backend && python main.py' to start the backend"
echo ""
echo "For Android app, open the android/ folder in Android Studio"
echo ""
echo "See docs/DEPLOYMENT.md for detailed deployment instructions"
