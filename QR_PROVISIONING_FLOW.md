# 📱 Eden QR Code Provisioning Flow

## Complete End-to-End Process

### Step 1: Admin Enrolls Device

**Admin Dashboard Actions:**
1. Admin logs into dashboard (`/admin`)
2. Fills enrollment form:
   - Device serial number (IMEI)
   - Total amount (KES)
   - Initial payment (KES)
   - Customer national ID
   - Customer full name
   - Customer phone number
   - KYC documents (ID front/back, passport photo)
3. Clicks "Enroll Device"
4. **QR code appears automatically** in modal

**What Happens:**
- Device record created in database
- Unique device_id generated
- Customer account created/linked
- QR code generated with provisioning data:
  ```json
  {
    "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.eden.mkopa/.DeviceAdminReceiver",
    "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://eden-mkopa.onrender.com/download/eden.apk",
    "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": true,
    "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
    "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
      "device_id": "DEV-123456",
      "serial_number": "IMEI123456789",
      "setup_time": 1234567890
    }
  }
  ```

### Step 2: Factory Reset Device

**Physical Device Actions:**
1. Go to Settings → System → Reset
2. Factory Data Reset
3. Confirm reset
4. Device wipes and restarts

**Result:**
- All data erased
- Device boots to Android setup wizard
- White welcome screen appears

### Step 3: Trigger QR Scanner

**On Device:**
1. See white welcome screen
2. **Tap screen 6 times quickly**
3. Android's built-in QR scanner activates
4. Camera opens automatically

**Important:**
- This is Android's native provisioning feature
- No Eden app installed yet
- Android handles QR scanning

### Step 4: Scan QR Code

**Admin Shows QR Code:**
- QR code is displayed on admin dashboard
- Admin holds screen/prints QR code

**Device Scans:**
1. Point device camera at QR code
2. Android reads QR code data
3. Provisioning starts automatically

**What Android Does:**
- Reads provisioning JSON from QR
- Downloads Eden APK from URL
- Installs Eden as Device Owner
- Passes device_id to app via extras

### Step 5: Automatic Provisioning

**Android Provisioning Process:**
1. Downloads `eden.apk` from server
2. Installs APK silently
3. Sets Eden as Device Owner
4. Calls `onProfileProvisioningComplete()` in DeviceAdminReceiver
5. Passes `device_id` and `serial_number` via extras bundle
6. Device restarts

**Eden App Actions:**
1. Receives provisioning extras
2. Saves device_id to SharedPreferences
3. Applies all Device Owner restrictions:
   - Block factory reset
   - Block ADB debugging
   - Block app uninstall
   - Enable kiosk mode
4. Launches MainActivity

### Step 6: Customer Login

**Device Shows:**
- Eden app opens automatically
- Customer login page loads
- URL includes device_id: `/customer-login?device_id=DEV-123456`
- Green banner: "📱 Device provisioned successfully!"

**Customer Actions:**
1. Enters phone number (registered during enrollment)
2. If first time: Sets 4-digit PIN
3. If returning: Enters existing PIN
4. Clicks "Login"

**Backend Verification:**
- Checks phone number exists
- Verifies PIN
- Links device_id to customer account
- Returns customer token

### Step 7: Customer Dashboard

**Customer Sees:**
- Loan balance
- Amount paid
- Remaining balance
- Payment history
- "Unlock Device" button

**Device Status:**
- Locked in kiosk mode
- Cannot exit Eden app
- Cannot press back/home
- Cannot access settings
- Cannot uninstall app

### Step 8: Unlock Flow

**When Customer Pays:**
1. Admin marks payment in dashboard
2. Admin clicks "Unlock Device"
3. Device syncs within 1 minute
4. Customer clicks "Unlock Device" in their dashboard
5. Device exits kiosk mode
6. Customer can use device normally

**When Payment Overdue:**
1. Admin clicks "Lock Device"
2. Device syncs within 1 minute
3. Device enters kiosk mode
4. Customer can only use Eden app

## For Existing Enrolled Devices

**Admin Dashboard:**
1. View enrolled devices list
2. Click "📱 Generate QR" button next to device
3. QR code appears in modal
4. Follow Steps 2-8 above

## Technical Details

### QR Code Data Structure

The QR code contains Android Device Owner provisioning data in JSON format:

```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.eden.mkopa/.DeviceAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://eden-mkopa.onrender.com/download/eden.apk",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": true,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
  "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
    "device_id": "unique-device-id",
    "serial_number": "device-serial",
    "setup_time": 1234567890
  }
}
```

### Device Owner Capabilities

Once provisioned, Eden can:

✅ **Lock device in kiosk mode** - Full-screen app only
✅ **Block factory reset** - Option hidden in settings
✅ **Block ADB debugging** - Cannot enable developer options
✅ **Block app uninstall** - Eden cannot be removed
✅ **Survive factory reset** - App reinstalls automatically (with QR provisioning)
✅ **Remote lock/unlock** - Admin controls from dashboard
✅ **Background sync** - Every 1 minute

### Auto-Linking Device to Customer

**During Provisioning:**
1. QR code contains `device_id` from enrollment
2. Android passes `device_id` to app via extras
3. App saves `device_id` to SharedPreferences
4. App loads login page with `device_id` parameter

**During Login:**
1. Customer enters phone number
2. Backend checks phone matches enrolled device
3. Backend links `device_id` to customer account
4. Customer can now access their loan details

### Security Features

**Device Owner Restrictions:**
- No factory reset
- No safe boot
- No ADB debugging
- No USB file transfer
- No unknown sources
- No add/remove users
- No modify accounts
- No config credentials
- No mobile network config

**App Security:**
- Cannot be uninstalled
- Survives factory reset
- Persistent preferred activity
- Lock task mode enabled
- Factory reset protection

## Troubleshooting

### QR Scanner Doesn't Appear
- Ensure device is factory reset
- Tap exactly 6 times on welcome screen
- Try tapping in center of screen
- Wait 1 second between taps

### Provisioning Fails
- Check internet connection
- Verify APK URL is accessible
- Ensure QR code is not damaged
- Try generating new QR code

### Device Not Linking to Customer
- Verify phone number matches enrollment
- Check device_id is passed correctly
- Ensure customer account exists
- Contact admin to verify enrollment

### Device Not Locking
- Verify Device Owner is set (not just device admin)
- Check provisioning completed successfully
- Restart device
- Re-provision if needed

## Support

For issues or questions:
- Email: sammyseth260@gmail.com
- Check device logs: `adb logcat | grep Eden`
- Verify Device Owner: `adb shell dumpsys device_policy`
