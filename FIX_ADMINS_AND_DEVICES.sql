-- Complete fix for admins and devices tables

-- Step 1: Drop existing admins table and recreate with correct schema
DROP TABLE IF EXISTS admins CASCADE;

CREATE TABLE admins (
    admin_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT DEFAULT 'admin',
    full_name TEXT,
    email TEXT,
    token TEXT,
    must_change_password BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Step 2: Add missing columns to devices table
ALTER TABLE devices ADD COLUMN IF NOT EXISTS pin_hash TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS is_locked BOOLEAN DEFAULT false;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS token TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS must_change_pin BOOLEAN DEFAULT false;

-- Step 3: Set default PIN (1234) for all existing devices with phone numbers
-- Mark them to change PIN on first login
UPDATE devices 
SET pin_hash = 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3',
    must_change_pin = true
WHERE customer_phone IS NOT NULL AND (pin_hash IS NULL OR pin_hash = '');

-- Step 4: Create admin user
-- Username: sammyselth260@gmail.com
-- Password: admin123 (must change on first login)
INSERT INTO admins (username, password_hash, role, full_name, email, must_change_password)
VALUES (
    'sammyselth260@gmail.com',
    '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
    'super_admin',
    'System Administrator',
    'sammyselth260@gmail.com',
    true
);

-- Step 5: Create payments table if it doesn't exist
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_phone TEXT NOT NULL,
    amount NUMERIC NOT NULL,
    payment_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    payment_method TEXT,
    reference_number TEXT
);

-- Step 6: Verify setup
SELECT '=== SETUP COMPLETE ===' as status;

SELECT 'Devices with authentication:' as info, COUNT(*) as count 
FROM devices 
WHERE customer_phone IS NOT NULL AND pin_hash IS NOT NULL;

SELECT 'Admin user created:' as info, username, email, role 
FROM admins;

SELECT 'Sample devices:' as info;
SELECT device_id, customer_name, customer_phone, 
       CASE WHEN pin_hash IS NOT NULL THEN 'Has PIN' ELSE 'No PIN' END as auth_status,
       status, is_locked
FROM devices 
WHERE customer_phone IS NOT NULL
LIMIT 5;

-- Step 7: Display login credentials
SELECT '=== LOGIN CREDENTIALS ===' as info;
SELECT 'Admin Login:' as type, 'sammyselth260@gmail.com' as username, 'admin123' as password
UNION ALL
SELECT 'Customer Login:', 'Phone from devices table', 'PIN: 1234';
