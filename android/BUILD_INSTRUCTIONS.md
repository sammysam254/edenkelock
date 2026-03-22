# Eden Android APK Build Instructions

## Prerequisites

### 1. Install Java JDK 17
- Download from: https://adoptium.net/
- Install and add to PATH
- Verify: `java -version`

### 2. Install Android Studio (Recommended) OR Android SDK
- **Option A: Android Studio** (Easiest)
  - Download: https://developer.android.com/studio
  - Install with default settings
  - Open Android Studio → SDK Manager → Install SDK 34

- **Option B: Command Line Tools Only**
  - Download: https://developer.android.com/studio#command-tools
  - Extract to `C:\Android\sdk`
  - Set environment variable: `ANDROID_HOME=C:\Android\sdk`

## Build Methods

### Method 1: Using Build Script (Easiest)
```bash
cd android
build-apk.bat
```

### Method 2: Using Gradle Directly
```bash
cd android

# For debug APK (faster, for testing)
gradlew.bat assembleDebug

# For release APK (optimized, for production)
gradlew.bat assembleRelease
```

### Method 3: Using Android Studio
1. Open Android Studio
2. File → Open → Select `android` folder
3. Wait for Gradle sync
4. Build → Build Bundle(s) / APK(s) → Build APK(s)

## Output Locations

- **Debug APK**: `android/app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `android/app/build/outputs/apk/release/app-release.apk`

## Signing Release APK (For Production)

### 1. Generate Keystore
```bash
keytool -genkey -v -keystore eden-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias eden
```

### 2. Update `android/app/build.gradle`
Add signing config:
```gradle
android {
    signingConfigs {
        release {
            storeFile file("eden-release-key.jks")
            storePassword "your-password"
            keyAlias "eden"
            keyPassword "your-password"
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### 3. Build Signed APK
```bash
gradlew.bat assembleRelease
```

## Troubleshooting

### Error: "JAVA_HOME is not set"
- Install Java JDK 17
- Set environment variable: `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.x.x`
- Add to PATH: `%JAVA_HOME%\bin`

### Error: "Android SDK not found"
- Install Android Studio OR
- Download command line tools
- Set `ANDROID_HOME` environment variable

### Error: "Gradle sync failed"
- Delete `.gradle` folder in android directory
- Run: `gradlew.bat clean`
- Try again

### Error: "Build failed with compilation errors"
- Check `android/app/build/outputs/logs/` for details
- Ensure all Kotlin files are properly formatted
- Run: `gradlew.bat clean build`

## Testing the APK

### Install on Device
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Install on Emulator
1. Open Android Studio
2. Tools → Device Manager
3. Create/Start emulator
4. Drag APK file onto emulator

## App Features

- Customer login with phone + PIN
- Loan balance display
- Payment history
- Device status monitoring
- Background sync every 15 minutes
- Device locking when payment overdue
- Swipe to refresh
- Beautiful loading animations

## Next Steps

1. Build the APK
2. Test on a device
3. Configure device as Device Owner (for locking features)
4. Deploy to customer devices

## Support

For issues, check:
- Build logs in `android/app/build/outputs/logs/`
- Gradle console output
- Android Studio Logcat
