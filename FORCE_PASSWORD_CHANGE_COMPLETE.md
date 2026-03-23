# Force Password/PIN Change Implementation - Complete

## Overview
Added functionality to force both admin users and customers to change their default passwords/PINs on first login.

## Database Changes

### Updated `FIX_ADMINS_AND_DEVICES.sql`:
1. Added `must_change_password` column to `admins` table
2. Added `must_change_pin` column to `devices` table
3. Set `must_change_password = true` for default admin user
4. Set `must_change_pin = true` for all devices with default PIN (1234)

## Backend Changes (server.py)

### Updated Login Endpoints:
1. **Admin Login** (`/api/login`):
   - Returns `must_change_password` flag in response
   - Redirects to password change page if required

2. **Customer Login** (`/api/customer/login`):
   - Returns `must_change_pin` flag in response
   - Triggers PIN change dialog if required

### New API Endpoints:
1. **`/api/admin/change-password`** (POST):
   - Changes admin password
   - Sets `must_change_password = false`
   - Requires minimum 6 characters

2. **Updated `/api/customer/set-pin`** (POST):
   - Changes customer PIN
   - Sets `must_change_pin = false`
   - Requires exactly 4 digits

### New Web Routes:
- `/change-password` - Admin password change page
- `/change-pin` - Customer PIN change page (web version)

## Frontend Changes

### Web Interface:
1. **`templates/login.html`**:
   - Checks `must_change_password` flag
   - Redirects to `/change-password` if required

2. **`templates/change_password.html`** (NEW):
   - Admin password change form
   - Password confirmation validation
   - Minimum 6 character requirement

3. **`templates/change_pin.html`** (NEW):
   - Customer PIN change form (web version)
   - 4-digit PIN validation
   - PIN confirmation

### Android App:
1. **`PinEntryActivity.kt`**:
   - Checks `must_change_pin` flag from login response
   - Shows PIN change dialog if required
   - Validates new PIN (4 digits, confirmation match)

2. **`dialog_change_pin.xml`** (NEW):
   - PIN change dialog layout
   - Two sets of 4 PIN boxes (new + confirm)
   - Proper navigation between boxes

## User Flow

### Admin Login:
1. Admin enters default credentials (sammyselth260@gmail.com / admin123)
2. System detects `must_change_password = true`
3. Redirects to password change page
4. Admin must enter new password (min 6 chars) + confirmation
5. After successful change, proceeds to admin dashboard
6. Future logins use new password

### Customer Login (Android):
1. Customer enters phone + default PIN (1234)
2. System detects `must_change_pin = true`
3. Shows PIN change dialog
4. Customer enters new 4-digit PIN + confirmation
5. After successful change, proceeds to customer dashboard
6. Future logins use new PIN

## Security Features
- Default credentials cannot be used permanently
- Password/PIN change is mandatory before accessing main features
- Proper validation (length, confirmation match)
- Secure password hashing (SHA-256)
- Session tokens invalidated appropriately

## Testing Steps

### 1. Run Database Script:
```sql
-- Run FIX_ADMINS_AND_DEVICES.sql in Supabase
```

### 2. Test Admin Flow:
- Go to `/login`
- Use: sammyselth260@gmail.com / admin123
- Should redirect to password change page
- Set new password, verify redirect to dashboard

### 3. Test Customer Flow:
- Open Android app
- Use any phone number from devices table + PIN 1234
- Should show PIN change dialog
- Set new PIN, verify access to dashboard

### 4. Test Subsequent Logins:
- Both admin and customers should use new credentials
- No more forced password/PIN changes

## Files Modified
- `FIX_ADMINS_AND_DEVICES.sql` - Database schema updates
- `server.py` - Login logic and new endpoints
- `templates/login.html` - Password change detection
- `templates/change_password.html` - NEW admin password change
- `templates/change_pin.html` - NEW customer PIN change (web)
- `android/app/src/main/java/com/eden/mkopa/PinEntryActivity.kt` - PIN change logic
- `android/app/src/main/res/layout/dialog_change_pin.xml` - NEW PIN change dialog

## Default Credentials (First Time Only)
- **Admin**: sammyselth260@gmail.com / admin123
- **Customers**: Phone from devices table / PIN 1234

After first login, users must set their own secure credentials.