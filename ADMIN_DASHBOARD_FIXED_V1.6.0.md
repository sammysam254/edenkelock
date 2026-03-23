# Admin Dashboard Fixed + App v1.6.0 Built

## Issues Fixed

### 1. Admin Dashboard Logout Issue
**Problem**: Admin was being logged out immediately after login
**Root Cause**: Dashboard was checking for `eden_user` in localStorage, but login only stores `eden_token`, `eden_role`, and `eden_admin_id`

**Fixed**:
- Updated `templates/admin.html` authentication check
- Updated `templates/super_admin.html` authentication check  
- Fixed logout functions to clear correct localStorage items

### 2. Authentication Flow
**Before**:
```javascript
// Login stored these:
localStorage.setItem('eden_token', data.token);
localStorage.setItem('eden_role', data.role);
localStorage.setItem('eden_admin_id', data.admin_id);

// But dashboard checked for:
const user = JSON.parse(localStorage.getItem('eden_user') || '{}');
if (!token || user.role !== 'admin') // FAILED - user was empty
```

**After**:
```javascript
// Dashboard now checks:
const token = localStorage.getItem('eden_token');
const role = localStorage.getItem('eden_role');
if (!token || (role !== 'admin' && role !== 'super_admin')) // WORKS
```

## New Android App Build - v1.6.0

### Features Added:
1. **Force PIN Change on First Login**
   - Customers with default PIN (1234) must change it
   - PIN change dialog appears automatically
   - New PIN validation (4 digits, confirmation match)
   - Cannot proceed without changing PIN

2. **Improved Login Flow**
   - Returning users: Phone + PIN on same page
   - New users: Phone first, then system checks account
   - "Not you?" button to switch accounts
   - Better error handling and user feedback

3. **Enhanced Security**
   - Default PINs cannot be used permanently
   - Secure PIN storage with SHA-256 hashing
   - Session token management

### Build Details:
- **Version Code**: 7
- **Version Name**: 1.6.0
- **Build Status**: ✅ SUCCESS
- **APK Location**: `app/eden-v1.6.0.apk` and `app/eden.apk`
- **Build Time**: ~39 seconds

## Testing Checklist

### Admin Dashboard:
- [x] Login with sammyselth260@gmail.com / admin123
- [x] Should redirect to password change pa