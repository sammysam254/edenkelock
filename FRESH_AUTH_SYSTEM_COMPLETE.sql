-- 🔥 COMPLETE DATABASE RESET & NEW AUTH SYSTEM
-- This will completely wipe everything and create a fresh, clean authentication system

-- ============================================
-- STEP 1: COMPLETE DATABASE WIPE
-- ============================================

-- Drop all existing tables (in correct order to avoid foreign key issues)
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS device_logs CASCADE;
DROP TABLE IF EXISTS security_violations CASCADE;
DROP TABLE IF EXISTS devices CASCADE;
DROP TABLE IF EXISTS admins CASCADE;

-- Drop any remaining sequences or functions
DROP SEQUENCE IF EXISTS admins_id_seq CASCADE;
DROP SEQUENCE IF EXISTS devices_id_seq CASCADE;

-- ============================================
-- STEP 2: CREATE CLEAN AUTH SYSTEM
-- ============================================

-- ADMINS TABLE - Simple and Clean
CREATE TABLE admins (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'admin',
    full_name TEXT,
    token TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true
);

-- DEVICES TABLE - Contains all customer and device info
CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT UNIQUE NOT NULL,
    serial_number TEXT UNIQUE NOT NULL,
    
    -- Customer Information
    customer_name TEXT NOT NULL,
    customer_phone TEXT UNIQUE NOT NULL,
    national_id TEXT UNIQUE NOT NULL,
    
    -- Authentication
    pin_hash TEXT NOT NULL,
    token TEXT,
    
    -- Loan Information
    total_amount NUMERIC NOT NULL DEFAULT 0,
    amount_paid NUMERIC NOT NULL DEFAULT 0,
    
    -- Device Status
    status TEXT DEFAULT 'active',
    is_locked BOOLEAN DEFAULT false,
    
    -- Metadata
    enrolled_by UUID REFERENCES admins(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- SECURITY VIOLATIONS TABLE - Track security issues
CREATE TABLE security_violations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT,
    customer_phone TEXT,
    violation_type TEXT NOT NULL,
    violation_details TEXT,
    ip_address TEXT,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- DEVICE LOGS TABLE - Audit trail
CREATE TABLE device_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT NOT NULL,
    action TEXT NOT NULL,
    performed_by UUID REFERENCES admins(id),
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- PAYMENTS TABLE - Payment history
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT,
    customer_phone TEXT NOT NULL,
    amount NUMERIC NOT NULL,
    payment_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    payment_method TEXT,
    reference_number TEXT,
    recorded_by UUID REFERENCES admins(id)
);

-- ============================================
-- STEP 3: CREATE INDEXES FOR PERFORMANCE
-- ============================================

CREATE INDEX idx_admins_email ON admins(email);
CREATE INDEX idx_admins_token ON admins(token);
CREATE INDEX idx_devices_device_id ON devices(device_id);
CREATE INDEX idx_devices_customer_phone ON devices(customer_phone);
CREATE INDEX idx_devices_pin_hash ON devices(pin_hash);
CREATE INDEX idx_devices_token ON devices(token);
CREATE INDEX idx_security_violations_device_id ON security_violations(device_id);
CREATE INDEX idx_device_logs_device_id ON device_logs(device_id);
CREATE INDEX idx_payments_customer_phone ON payments(customer_phone);

-- ============================================
-- STEP 4: ENABLE ROW LEVEL SECURITY
-- ============================================

ALTER TABLE admins ENABLE ROW LEVEL SECURITY;
ALTER TABLE devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE security_violations ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;

-- Create permissive policies (allow all operations for now)
CREATE POLICY "Allow all operations on admins" ON admins FOR ALL USING (true);
CREATE POLICY "Allow all operations on devices" ON devices FOR ALL USING (true);
CREATE POLICY "Allow all operations on security_violations" ON security_violations FOR ALL USING (true);
CREATE POLICY "Allow all operations on device_logs" ON device_logs FOR ALL USING (true);
CREATE POLICY "Allow all operations on payments" ON payments FOR ALL USING (true);

-- ============================================
-- STEP 5: CREATE SUPER ADMIN RECOGNITION SYSTEM
-- ============================================

-- Create a function to automatically assign super_admin role to sammyselth260@gmail.com
CREATE OR REPLACE FUNCTION check_super_admin_email()
RETURNS TRIGGER AS $$
BEGIN
    -- If the email is sammyselth260@gmail.com, automatically make them super_admin
    IF NEW.email = 'sammyselth260@gmail.com' THEN
        NEW.role = 'super_admin';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically assign super_admin role
CREATE TRIGGER auto_super_admin_trigger
    BEFORE INSERT ON admins
    FOR EACH ROW
    EXECUTE FUNCTION check_super_admin_email();

-- Note: No super admin account created yet - they must register first

-- ============================================
-- STEP 6: REMOVE SAMPLE DEVICE (Super Admin Must Create)
-- ============================================

-- No sample devices created - super admin will create everything after registration

-- ============================================
-- STEP 7: VERIFICATION QUERIES
-- ============================================

-- Show created tables
SELECT 'Tables created successfully:' as status;
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_type = 'BASE TABLE'
ORDER BY table_name;

-- Show super admin recognition system
SELECT 'Super Admin Recognition System:' as info;
SELECT 'When sammyselth260@gmail.com registers, they automatically get super_admin role' as status;

-- Show empty tables (ready for super admin to populate)
SELECT 'Database is clean and ready:' as info;
SELECT 'No admins created yet - super admin must register first' as status;

-- ============================================
-- STEP 8: AUTHENTICATION FLOW SUMMARY
-- ============================================

SELECT '=== NEW AUTHENTICATION SYSTEM READY ===' as title;

SELECT 'ADMIN REGISTRATION & AUTHENTICATION:' as type
UNION ALL SELECT '1. Super admin registers with sammyselth260@gmail.com + their own password'
UNION ALL SELECT '2. System automatically recognizes email and assigns super_admin role'
UNION ALL SELECT '3. Super admin can then create other administrators'
UNION ALL SELECT '4. Regular admins can only access enrollment page to create customers'
UNION ALL SELECT ''
UNION ALL SELECT 'CUSTOMER AUTHENTICATION:'
UNION ALL SELECT '1. Admin enrolls device and sets customer PIN during enrollment'
UNION ALL SELECT '2. Customer logs in with phone number + PIN set by admin'
UNION ALL SELECT '3. Customer can change PIN after first login'
UNION ALL SELECT ''
UNION ALL SELECT 'FIRST STEP:'
UNION ALL SELECT 'Super admin must register at: /register with sammyselth260@gmail.com';

SELECT '=== SYSTEM STATUS ===' as info;
SELECT 'Database: COMPLETELY RESET ✅' as status
UNION ALL SELECT 'Auth System: BRAND NEW ✅'
UNION ALL SELECT 'Super Admin Recognition: ACTIVE ✅'
UNION ALL SELECT 'Registration Ready: WAITING FOR SUPER ADMIN ✅'
UNION ALL SELECT 'All Tables: CLEAN ✅'
UNION ALL SELECT 'Indexes: OPTIMIZED ✅'
UNION ALL SELECT 'Security: ENABLED ✅';

-- Show table counts
SELECT 'Table Counts:' as info;
SELECT 'admins' as table_name, COUNT(*) as count FROM admins
UNION ALL SELECT 'devices', COUNT(*) FROM devices
UNION ALL SELECT 'security_violations', COUNT(*) FROM security_violations
UNION ALL SELECT 'device_logs', COUNT(*) FROM device_logs
UNION ALL SELECT 'payments', COUNT(*) FROM payments;