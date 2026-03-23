-- Check and Fix Existing Data in Supabase
-- Run these queries one by one in Supabase SQL Editor

-- 1. Check existing admins
SELECT 'Existing Admins:' as info;
SELECT admin_id, username, email, role, created_at FROM admins;

-- 2. Check existing customers
SELECT 'Existing Customers:' as info;
SELECT customer_id, phone_number, full_name, pin_hash, total_loan_amount, loan_balance FROM customers;

-- 3. Check existing devices
SELECT 'Existing Devices:' as info;
SELECT device_id, serial_number, customer_phone, status, is_locked FROM devices;

-- 4. Set PIN for all customers (PIN: 1234)
-- Hash of "1234" = a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3
UPDATE customers 
SET pin_hash = 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3'
WHERE pin_hash IS NULL OR pin_hash = '';

SELECT 'Updated customers with PIN' as info, COUNT(*) as count 
FROM customers 
WHERE pin_hash = 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3';

-- 5. Set password for admin (Password: admin123)
-- Hash of "admin123" = 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
UPDATE admins 
SET password_hash = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9'
WHERE username = 'sammyselth260@gmail.com';

-- If admin doesn't exist, create it
INSERT INTO admins (username, password_hash, role, full_name, email)
VALUES (
    'sammyselth260@gmail.com',
    '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
    'super_admin',
    'Sammy Admin',
    'sammyselth260@gmail.com'
) ON CONFLICT (username) DO UPDATE 
SET password_hash = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9';

-- 6. Verify the updates
SELECT 'Admin ready for login:' as info;
SELECT username, email, role FROM admins WHERE username = 'sammyselth260@gmail.com';

SELECT 'Customers ready for login:' as info;
SELECT phone_number, full_name, 
       CASE WHEN pin_hash IS NOT NULL THEN 'Has PIN' ELSE 'No PIN' END as pin_status
FROM customers;

-- 7. Show login credentials
SELECT 
    '=== LOGIN CREDENTIALS ===' as info,
    '' as username,
    '' as password
UNION ALL
SELECT 
    'Admin:',
    'sammyselth260@gmail.com',
    'admin123'
UNION ALL
SELECT 
    'All Customers:',
    'Use their phone number',
    'PIN: 1234';
