#!/bin/bash

echo "🚀 Building Eden M-Kopa..."

# Install Node dependencies and build dashboard
echo "📦 Building dashboard..."
cd dashboard
npm install
npm run build
cd ..

# Install Python dependencies
echo "🐍 Installing Python dependencies..."
pip install -r requirements.txt

echo "✅ Build complete!"
