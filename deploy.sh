#!/bin/bash

echo "🚀 Deploying Eden M-Kopa to GitHub..."

# Initialize git if not already initialized
if [ ! -d .git ]; then
    echo "📦 Initializing git repository..."
    git init
fi

# Add all files
echo "📝 Adding files..."
git add .

# Commit
echo "💾 Committing changes..."
git commit -m "Complete Eden M-Kopa device financing system

- Beautiful gradient dashboard with Next.js
- Python Flask backend with Web3 integration
- Android DPC app for device locking
- Supabase database with RLS
- Complete documentation
- Ready for Render deployment
- Pre-configured with Supabase credentials"

# Add remote
echo "🔗 Adding remote repository..."
git remote remove origin 2>/dev/null
git remote add origin https://github.com/sammysam254/edenkelock.git

# Set main branch
echo "🌿 Setting main branch..."
git branch -M main

# Push to GitHub
echo "⬆️ Pushing to GitHub..."
git push -u origin main --force

echo ""
echo "✅ Successfully pushed to GitHub!"
echo ""
echo "🎯 Next Steps:"
echo "1. Go to https://dashboard.render.com/"
echo "2. Click 'New +' → 'Blueprint'"
echo "3. Connect repository: sammysam254/edenkelock"
echo "4. Add SUPABASE_SERVICE_KEY environment variable"
echo "5. Click 'Apply' and wait 5-10 minutes"
echo ""
echo "📖 See RENDER_DEPLOYMENT.md for detailed instructions"
echo ""
echo "🎉 Your code is now on GitHub!"
echo "   https://github.com/sammysam254/edenkelock"
