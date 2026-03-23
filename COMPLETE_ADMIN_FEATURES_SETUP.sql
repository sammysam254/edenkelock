-- Complete Admin Features Setup - Advanced Management System

-- Step 1: Drop existing admins table and recreate with advanced features
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
    created_by UUID,
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
ALTER TABLE devices ADD COLUMN IF NOT EXISTS deleted_by UUID;

-- Step 3: Create security violations table for factory reset attempts
CREATE TABLE IF NOT EXISTS security_violations (
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

-- Step 4: Create device management logs
CREATE TABLE IF NOT EXISTS device_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL,
    action TEXT NOT NULL,
    performed_by UUID,
    old_values JSONB,
    new_values JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Step 5: Create notifications table
CREATE TABLE IF NOT EXISTS notifications (
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

-- Step 6: Set default PIN (1234) for all existing devices with phone numbers
UPDATE devices 
SET pin_hash = 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3',
    must_change_pin = true
WHERE customer_phone IS NOT NULL AND (pin_hash IS NULL OR pin_hash = '');

-- Step 7: Create super admin user
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
    role = 'super_admin';

-- Step 8: Create payments table if needed
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT,
    customer_phone TEXT NOT NULL,
    amount NUMERIC NOT NULL,
    payment_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    payment_method TEXT,
    reference_number TEXT,
    recorded_by UUID
);

-- Step 9: Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_devices_customer_phone ON devices(customer_phone);
CREATE INDEX IF NOT EXISTS idx_devices_device_id ON devices(device_id);
CREATE INDEX IF NOT EXISTS idx_devices_is_deleted ON devices(is_deleted);
CREATE INDEX IF NOT EXISTS idx_security_violations_device_id ON security_violations(device_id);
CREATE INDEX IF NOT EXISTS idx_security_violations_created_at ON security_violations(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_device_id ON notifications(device_id);
CREATE INDEX IF NOT EXISTS idx_device_logs_device_id ON device_logs(device_id);

-- Step 10: Add foreign key constraints after creating tables (with proper error handling)
DO $$
BEGIN
    -- Add foreign key for admins.created_by
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_admins_created_by'
    ) THEN
        ALTER TABLE admins ADD CONSTRAINT fk_admins_created_by 
            FOREIGN KEY (created_by) REFERENCES admins(admin_id);
    END IF;

    -- Add foreign key for devices.deleted_by
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_devices_deleted_by'
    ) THEN
        ALTER TABLE devices ADD CONSTRAINT fk_devices_deleted_by 
            FOREIGN KEY (deleted_by) REFERENCES admins(admin_id);
    END IF;

    -- Add foreign key for device_logs.performed_by
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_device_logs_performed_by'
    ) THEN
        ALTER TABLE device_logs ADD CONSTRAINT fk_device_logs_performed_by 
            FOREIGN KEY (performed_by) REFERENCES admins(admin_id);
    END IF;

    -- Add foreign key for payments.recorded_by
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_payments_recorded_by'
    ) THEN
        ALTER TABLE payments ADD CONSTRAINT fk_payments_recorded_by 
            FOREIGN KEY (recorded_by) REFERENCES admins(admin_id);
    END IF;
END $$;

-- Step 11: Verify setup
SELECT 'Active devices ready for login:' as info, COUNT(*) as count 
FROM devices 
WHERE customer_phone IS NOT NULL AND pin_hash IS NOT NULL AND is_deleted = false;

SELECT 'Super admin created:' as info, username, email, role 
FROM admins 
WHERE username = 'sammyselth260@gmail.com';

-- Step 12: Show sample active devices
SELECT device_id, customer_name, customer_phone, 
       CASE WHEN pin_hash IS NOT NULL THEN 'Has PIN' ELSE 'No PIN' END as auth_status,
       status, is_locked, is_deleted
FROM devices 
WHERE customer_phone IS NOT NULL AND is_deleted = false
LIMIT 5;

-- Step 13: Display credentials and features
SELECT '=== ADMIN FEATURES ENABLED ===' as info;
SELECT '✅ Manual loan balance updates' as feature
UNION ALL SELECT '✅ Device deletion with auto app uninstall'
UNION ALL SELECT '✅ Factory reset attempt monitoring'
UNION ALL SELECT '✅ Security violation notifications'
UNION ALL SELECT '✅ Admin role promotion system'
UNION ALL SELECT '✅ Comprehensive device logging'
UNION ALL SELECT '✅ Restricted admin dashboard';

SELECT '=== LOGIN CREDENTIALS ===' as info;
SELECT 'Super Admin:' as type, 'sammyselth260@gmail.com' as username, 'admin123' as password
UNION ALL
SELECT 'Customers:', 'Phone from devices table', 'PIN: 1234 (must change on first login)';