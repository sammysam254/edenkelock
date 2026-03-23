-- Simple fix - just add authentication columns and create admin

-- 1. Add PIN column to devices table
ALTER TABLE devices ADD COLUMN IF NOT EXISTS pin_hash TEXT;

-- 2. Add is_locked column to devices table  
ALTER TABLE devices ADD COLUMN IF NOT EXISTS is_locked BOOLEAN DEFAULT false;

-- 3. Add token column to devices table for session management
ALTER TABLE devices ADD COLUMN IF NOT EXISTS token TEXT;

-- 4. Set PIN for all existing devices (PIN: 1234)
UPDATE devices 
SET pin_hash = 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3'
WHERE customer_phone IS NOT NULL AND (pin_hash IS NULL OR pin_hash = '');

-- 5. Create admins table if it doesn't exist
CREATE TABLE IF NOT EXISTS admins (
    admin_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT DEFAULT 'admin',
    full_name TEXT,
    email TEXT,
    token TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 6. Create admin user (Password: admin123)
INSERT INTO admins (username, password_hash, role, full_name, email)
VALUES (
    'sammyselth260@gmail.com',
    '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
    'super_admin',
    'System Administrator',
    'sammyselth260@gmail.com'
) ON CONFLICT (username) DO UPDATE 
SET password_hash = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9';

-- 7. Create payments table if needed
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_phone TEXT NOT NULL,
    amount NUMERIC NOT NULL,
    payment_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    payment_method TEXT,
    reference_number TEXT
);

-- 8. Verify setup
SELECT 'Devices ready for login:' as info, COUNT(*) as count 
FROM devices 
WHERE customer_phone IS NOT NULL AND pin_hash IS NOT NULL;

SELECT 'Admin created:' as info, username, email, role 
FROM admins 
WHERE username = 'sammyselth260@gmail.com';

-- 9. Show sample devices with authentication
SELECT device_id, customer_name, customer_phone, 
       CASE WHEN pin_hash IS NOT NULL THEN 'Has PIN' ELSE 'No PIN' END as auth_status,
       status, is_locked
FROM devices 
WHERE customer_phone IS NOT NULL
LIMIT 5;

-- 10. Display credentials
SELECT 
    '=== LOGIN CREDENTIALS ===' as message,
    '' as detail
UNION ALL
SELECT 
    'Admin:',
    'sammyselth260@gmail.com / admin123'
UNION ALL
SELECT 
    'Customers:',
    'Use phone number from devices / PIN: 1234';
