# Customer Login Fixed - All APIs Updated

## Problem
Customer login was failing with "account not found" errors because the code was trying to query a `customers` table that doesn't exist in the database. All customer data is actually stored in the `devices` table.

## Solution
Updated all customer-related API endpoints in `server.py` to query the `devices` table instead of the non-existent `customers` table.

## Changes Made

### 1. Updated `/api/customer/check-phone` endpoint
- Now queries `devices` table by `customer_phone` column
- Checks for `pin_hash` in device record

### 2. Updated `/api/customer/login` endpoint
- Authenticates against `devices.pin_hash` and `devices.customer_phone`
- Stores session token in `devices.token` column
- Returns device_id along with customer_id

### 3. Updated `/api/customer/dashboard` endpoint
- Fetches all data from `devices` table
- Maps device fields to customer format:
  - `devices.customer_phone` → `customer.phone_number`
  - `devices.customer_name` → `customer.full_name`
  - `devices.total_amount` → `customer.total_loan_amount`
  - `devices.amount_paid` → `customer.amount_paid`
  - Calculates `loan_balance` as (total_amount - amount_paid)

### 4. Updated `/api/customer/set-pin` endpoint
- Updates PIN in `devices` table instead of customers table

### 5. Updated `/api/customers` endpoint (for admin)
- Fetches from `devices` table
- Transforms device records to customer format for admin dashboard

## Database Changes Required

Run `SIMPLE_FIX.sql` in Supabase SQL Editor to:
1. Add `pin_hash` column to devices table
2. Add `is_locked` column to devices table
3. Add `token` column to devices table (for session management)
4. Set default PIN (1234) for all existing devices
5. Create `admins` table with proper schema (admin_id as primary key)
6. Create admin user: sammyselth260@gmail.com / admin123
7. Create `payments` table if needed

## Testing

After running the SQL script:

### Admin Login
- URL: https://your-app.com/login
- Username: sammyselth260@gmail.com
- Password: admin123

### Customer Login (Android App)
- Phone: Any phone number from devices table
- PIN: 1234 (default for all existing customers)

## Next Steps

1. Run `SIMPLE_FIX.sql` in Supabase SQL Editor
2. Test admin login at /login
3. Test customer login in Android app
4. Customers should now be able to:
   - Enter phone number
   - System checks if account exists
   - Enter PIN (1234)
   - See their dashboard with loan details

## Files Modified
- `server.py` - All customer API endpoints updated
- `SIMPLE_FIX.sql` - Added token column and fixed admin_id column name
