# Eden APK Download Folder

This folder contains the Eden APK for public download.

## Purpose
When a device is factory reset, it will automatically download and install the Eden APK from:
`https://eden-mkopa.onrender.com/download/eden.apk`

## Setup
1. Build the APK: `cd android && build-apk.bat`
2. Copy the APK here: `cp android/app/build/outputs/apk/release/app-release.apk static/apk/eden.apk`
3. The APK will be served at `/download/eden.apk`

## Auto-Install After Factory Reset
The device owner provisioning ensures that after any factory reset:
1. Device boots up
2. Eden APK downloads from this URL
3. APK auto-installs
4. Device locks automatically
5. Customer logs in with phone + PIN
6. Loan balance restored
7. Customer continues paying
