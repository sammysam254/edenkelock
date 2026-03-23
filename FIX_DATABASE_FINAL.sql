-- SAFE DATABASE SETUP - No Foreign Key Constraints
-- This script avoids foreign key issues and focuses on core functionality

-- Step 1: Create admins table (drop and recreate to avoid conflicts)
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
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true
);

-- Step 2: Add missing columns to devices table
ALTER TABLE devices ADD COLUMN IF NOT EXISTS pin_hash TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS is_locked BOOLEAN DEFAULT false;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS token TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS must_change_pin BOOLEAN DEFAULT false;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT false;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

-- Step 3: Create security violations table
DROP TABLE IF EXISTS security_violations;
CREATE TABLE security_violations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT,
    customer_phone TEXT,
    violation_type TEXT NOT NULL,
    violation_details TEXT,
    ip_address TEXT,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    admin_notified BOOLEAN DEFAULT false,
    notification_sent_at TIMESTAMP WITH TIME ZONE
);

-- Step 4: Create device logs table
DROP TABLE IF EXISTS device_logs;
CREATE TABLE device_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL,
    action TEXT NOT NULL,
    performed_by_email TEXT,
    old_values JSONB,
    new_values JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Step 5: Create notifications table
DROP TABLE IF EXISTS notifications;
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT,
    customer_phone TEXT,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    notification_type TEXT DEFAULT 'warning',
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    delivered BOOLEAN DEFAULT false,
    read_at TIMESTAMP WITH TIME ZONE
);

-- Step 6: Create payments table (simplified)
DROP TABLE IF EXISTS payments;
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT,
    customer_phone TEXT NOT NULL,
    amount NUMERIC NOT NULL,
    payment_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    payment_method TEXT,
    reference_number TEXT,
    recorded_by_email TEXT
);

-- Step 7: Set default PIN (1234) for all existing devices
UPDATE devices 
SET pin_hash = 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3',
    must_change_pin = true
WHERE customer_phone IS NOT NULL AND (pin_hash IS NULL OR pin_hash = '');

-- Step 8: Create super admin user
INSERT INTO admins (username, password_hash, role, full_name, email, must_change_password)
VALUES (
    'sammyselth260@gmail.com',
    '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
    'super_admin',
    'System Administrator',
    'sammyselth260@gmail.com',
    true
) ON CONFLICT (username) DO UPDATE 
SET password_hash = '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
    role = 'super_admin',
    must_change_password = true;

-- Step 9: Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_devices_customer_phone ON devices(customer_phone);
CREATE INDEX IF NOT EXISTS idx_devices_device_id ON devices(device_id);
CREATE INDEX IF NOT EXISTS idx_devices_is_deleted ON devices(is_deleted);
CREATE INDEX IF NOT EXISTS idx_security_violations_device_id ON security_violations(device_id);
CREATE INDEX IF NOT EXISTS idx_security_violations_created_at ON security_violations(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_device_id ON notifications(device_id);
CREATE INDEX IF NOT EXISTS idx_device_logs_device_id ON device_logs(device_id);
CREATE INDEX IF NOT EXISTS idx_admins_username ON admins(username);
CREATE INDEX IF NOT EXISTS idx_admins_email ON admins(email);

-- Step 10: Enable Row Level Security
ALTER TABLE admins ENABLE ROW LEVEL SECURITY;
ALTER TABLE devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE security_violations ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;

-- Step 11: Create RLS policies (basic - allow all for now)
DROP POLICY IF EXISTS "Allow all operations on admins" ON admins;
CREATE POLICY "Allow all operations on admins" ON admins FOR ALL USING (true);

DROP POLICY IF EXISTS "Allow all operations on devices" ON devices;
CREATE POLICY "Allow all operations on devices" ON devices FOR ALL USING (true);

DROP POLICY IF EXISTS "Allow all operations on security_violations" ON security_violations;
CREATE POLICY "Allow all operations on security_violations" ON security_violations FOR ALL USING (true);

DROP POLICY IF EXISTS "Allow all operations on device_logs" ON device_logs;
CREATE POLICY "Allow all operations on device_logs" ON device_logs FOR ALL USING (true);

DROP POLICY IF EXISTS "Allow all operations on notifications" ON notifications;
CREATE POLICY "Allow all operations on notifications" ON notifications FOR ALL USING (true);

DROP POLICY IF EXISTS "Allow all operations on payments" ON payments;
CREATE POLICY "Allow all operations on payments" ON payments FOR ALL USING (true);

-- Step 12: Verification queries
SELECT 'Database setup completed successfully!' as status;

SELECT 'Active devices ready for login:' as info, COUNT(*) as count 
FROM devices 
WHERE customer_phone IS NOT NULL AND pin_hash IS NOT NULL AND (is_deleted = false OR is_deleted IS NULL);

SELECT 'Super admin created:' as info, username, email, role, must_change_password
FROM admins 
WHERE username = 'sammyselth260@gmail.com';

-- Step 13: Show sample devices
SELECT 'Sample devices:' as info;
SELECT device_id, customer_name, customer_phone, 
       CASE WHEN pin_hash IS NOT NULL THEN 'Has PIN (1234)' ELSE 'No PIN' END as auth_status,
       status, 
       CASE WHEN is_locked = true THEN 'LOCKED' ELSE 'UNLOCKED' END as lock_status
FROM devices 
WHERE customer_phone IS NOT NULL AND (is_deleted = false OR is_deleted IS NULL)
LIMIT 5;

-- Step 14: Display login credentials
SELECT '=== LOGIN CREDENTIALS ===' as info;
SELECT 'Admin Login URL: https://eden-mkopa.onrender.com/login' as credential
UNION ALL SELECT 'Username: sammyselth260@gmail.com'
UNION ALL SELECT 'Password: admin123 (MUST CHANGE ON FIRST LOGIN)'
UNION ALL SELECT ''
UNION ALL SELECT 'Customer Login (Android App):'
UNION ALL SELECT 'Phone: Any phone number from devices table'
UNION ALL SELECT 'PIN: 1234 (MUST CHANGE ON FIRST LOGIN)';

SELECT '=== FEATURES ENABLED ===' as info;
SELECT '✅ Admin dashboard with device management' as feature
UNION ALL SELECT '✅ Customer PIN management'
UNION ALL SELECT '✅ Security violation monitoring'
UNION ALL SELECT '✅ Device locking/unlocking'
UNION ALL SELECT '✅ Factory reset protection logging'
UNION ALL SELECT '✅ Payment tracking'
UNION ALL SELECT '✅ Comprehensive audit trail';