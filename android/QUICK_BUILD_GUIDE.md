# Quick Build Guide - Eden Android APK

## Current Status
✅ Java 17 installed and verified
❌ Android SDK not installed (required for building)

## Choose Your Build Method

### Method 1: Local Build (Requires Android SDK)

#### Step 1: Install Android SDK
You have 2 options:

**Option A: Android Studio (Recommended - ~1GB)**
1. Download: https://developer.android.com/studio
2. Install with default settings
3. Open Android Studio → Tools → SDK Manager
4. Install "Android SDK Platform 34" and "Android SDK Build-Tools 34"
5. SDK will be at: `C:\Users\[YourUsername]\AppData\Local\Android\Sdk`

**Option B: Command Line Tools Only (~200MB)**
1. Download: https://developer.android.com/studio#command-tools
2. Extract to: `C:\Android\Sdk\cmdline-tools\latest`
3. Open Command Prompt as Administrator
4. Run:
   ```
   C:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   ```

#### Step 2: Run Setup Script
```bash
cd android
setup-sdk.bat
```

#### Step 3: Build APK
```bash
build-apk.bat
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk`

---

### Method 2: Online Build (No SDK Required) ⭐ EASIEST

Use GitHub Actions to build the APK in the cloud:

#### Step 1: Push code to GitHub
```bash
git add .
git commit -m "Add Android app"
git push
```

#### Step 2: Enable GitHub Actions
1. Go to your GitHub repository
2. Click "Actions" tab
3. The workflow will run automatically

#### Step 3: Download APK
1. Wait for build to complete (~5 minutes)
2. Click on the workflow run
3. Download the APK artifact

I can create the GitHub Actions workflow file for you if you choose this method.

---

### Method 3: Use Pre-built APK Service

Services like:
- **Appetize.io** - Build and test in browser
- **Bitrise** - CI/CD for mobile apps
- **CircleCI** - Free tier available

---

## Recommended Approach

For fastest results: **Method 2 (GitHub Actions)**
- No local setup required
- Builds in ~5 minutes
- Free for public repositories
- Automatic builds on every push

For development: **Method 1 (Android Studio)**
- Full IDE features
- Instant builds after first setup
- Better for debugging

## What's Next?

After building the APK:
1. Copy to `static/apk/eden.apk`
2. Deploy to server
3. Test device locking features
4. Configure device as Device Owner

## Need Help?

Choose your preferred method and I'll guide you through it step by step.
