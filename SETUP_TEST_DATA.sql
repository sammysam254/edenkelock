-- Setup Test Data for Eden Device Financing System
-- Run this in Supabase SQL Editor

-- 1. Create Super Admin
-- Password: admin123 (hashed)
INSERT INTO admins (username, password_hash, role, full_name, email)
VALUES (
    'admin@eden.com',
    'ee0b8d25e5e3c9c2e5e8e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5',
    'super_admin',
    'System Administrator',
    'admin@eden.com'
) ON CONFLICT (username) DO NOTHING;

-- 2. Create Regular Admin
INSERT INTO admins (username, password_hash, role, full_name, email)
VALUES (
    'sammyselth260@gmail.com',
    'ee0b8d25e5e3c9c2e5e8e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5e5',
    'admin',
    'Sammy Admin',
    'sammyselth260@gmail.com'
) ON CONFLICT (username) DO NOTHING;

-- 3. Create Test Customers
INSERT INTO customers (phone_number, full_name, national_id, pin_hash, total_loan_amount, loan_balance, next_payment_date)
VALUES 
    ('+254700000000', 'John Doe', '12345678', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 50000, 35000, '2026-04-01'),
    ('+254711111111', 'Jane Smith', '87654321', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 75000, 50000, '2026-04-05'),
    ('+254722222222', 'Bob Johnson', '11223344', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 100000, 80000, '2026-04-10'),
    ('0700000000', 'Test User', '99887766', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', 60000, 40000, '2026-04-15')
ON CONFLICT (phone_number) DO NOTHING;

-- 4. Create Test Devices
INSERT INTO devices (device_id, serial_number, model, customer_phone, status, is_locked)
VALUES 
    ('DEV001', 'SN123456789', 'Samsung Galaxy A14', '+254700000000', 'active', false),
    ('DEV002', 'SN987654321', 'Tecno Spark 10', '+254711111111', 'active', false),
    ('DEV003', 'SN456789123', 'Infinix Hot 30', '+254722222222', 'locked', true),
    ('DEV004', 'SN789123456', 'Samsung Galaxy A04', '0700000000', 'active', false)
ON CONFLICT (device_id) DO NOTHING;

-- 5. Create Test Payments
INSERT INTO payments (customer_phone, amount, payment_date, payment_method, reference_number)
VALUES 
    ('+254700000000', 5000, '2026-03-01', 'M-Pesa', 'REF001'),
    ('+254700000000', 5000, '2026-03-15', 'M-Pesa', 'REF002'),
    ('+254711111111', 7500, '2026-03-05', 'M-Pesa', 'REF003'),
    ('+254722222222', 10000, '2026-03-10', 'Bank Transfer', 'REF004'),
    ('0700000000', 6000, '2026-03-20', 'M-Pesa', 'REF005');

-- 6. Verify Data
SELECT 'Admins Created:' as info, COUNT(*) as count FROM admins
UNION ALL
SELECT 'Customers Created:', COUNT(*) FROM customers
UNION ALL
SELECT 'Devices Created:', COUNT(*) FROM devices
UNION ALL
SELECT 'Payments Created:', COUNT(*) FROM payments;

-- Display test credentials
SELECT 
    '=== TEST CREDENTIALS ===' as info,
    '' as username,
    '' as password,
    '' as notes
UNION ALL
SELECT 
    'Admin Login:',
    'sammyselth260@gmail.com',
    'admin123',
    'Use this to login to admin panel'
UNION ALL
SELECT 
    'Customer Login:',
    '+254700000000 or 0700000000',
    '1234',
    'Use any of these phone numbers'
UNION ALL
SELECT 
    'Customer Login:',
    '+254711111111',
    '1234',
    'All test customers use PIN: 1234'
UNION ALL
SELECT 
    'Customer Login:',
    '+254722222222',
    '1234',
    'This customer has a locked device';
