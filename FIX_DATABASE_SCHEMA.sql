-- Fix Database Schema for Eden
-- Your current schema only has a 'devices' table
-- We need to add columns for authentication

-- 1. Add PIN column to devices table for customer authentication
ALTER TABLE devices ADD COLUMN IF NOT EXISTS pin_hash TEXT;

-- 2. Add is_locked column if it doesn't exist
ALTER TABLE devices ADD COLUMN IF NOT EXISTS is_locked BOOLEAN DEFAULT false;

-- 3. Create admins table for admin authentication
CREATE TABLE IF NOT EXISTS admins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT DEFAULT 'admin',
    full_name TEXT,
    email TEXT,
    token TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. Create customers table for better data organization
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id TEXT UNIQUE,
    phone_number TEXT UNIQUE NOT NULL,
    full_name TEXT,
    national_id TEXT,
    pin_hash TEXT,
    total_loan_amount NUMERIC DEFAULT 0,
    loan_balance NUMERIC DEFAULT 0,
    next_payment_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. Create payments table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_phone TEXT NOT NULL,
    amount NUMERIC NOT NULL,
    payment_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    payment_method TEXT,
    reference_number TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 6. Migrate existing data from devices to customers
INSERT INTO customers (customer_id, phone_number, full_name, national_id, total_loan_amount, loan_balance)
SELECT 
    customer_id,
    customer_phone,
    customer_name,
    national_id,
    total_amount,
    total_amount - COALESCE(amount_paid, 0)
FROM devices
WHERE customer_phone IS NOT NULL 
  AND customer_phone != ''
  AND NOT EXISTS (
      SELECT 1 FROM customers WHERE customers.phone_number = devices.customer_phone
  );

-- 7. Set PIN for all customers (PIN: 1234)
-- Hash of "1234" = a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3
UPDATE customers 
SET pin_hash = 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3'
WHERE pin_hash IS NULL;

UPDATE devices
SET pin_hash = 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3'
WHERE pin_hash IS NULL;

-- 8. Create admin user
-- Password: admin123
-- Hash: 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
INSERT INTO admins (username, password_hash, role, full_name, email)
VALUES (
    'sammyselth260@gmail.com',
    '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
    'super_admin',
    'System Administrator',
    'sammyselth260@gmail.com'
) ON CONFLICT (username) DO UPDATE 
SET password_hash = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9';

-- 9. Verify the setup
SELECT 'Devices with customers:' as info, COUNT(*) as count FROM devices WHERE customer_phone IS NOT NULL;
SELECT 'Customers created:' as info, COUNT(*) as count FROM customers;
SELECT 'Admins created:' as info, COUNT(*) as count FROM admins;

-- 10. Show sample data
SELECT 'Sample Devices:' as info;
SELECT device_id, customer_name, customer_phone, status, is_locked FROM devices LIMIT 5;

SELECT 'Sample Customers:' as info;
SELECT phone_number, full_name, 
       CASE WHEN pin_hash IS NOT NULL THEN 'Has PIN' ELSE 'No PIN' END as pin_status,
       loan_balance
FROM customers LIMIT 5;

-- 11. Display login credentials
SELECT 
    '=== LOGIN CREDENTIALS ===' as info,
    '' as username,
    '' as password
UNION ALL
SELECT 
    'Admin Login:',
    'sammyselth260@gmail.com',
    'admin123'
UNION ALL
SELECT 
    'Customer Login:',
    'Use phone number from devices table',
    'PIN: 1234';
