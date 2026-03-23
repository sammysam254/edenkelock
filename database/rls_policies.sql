-- Eden M-Kopa Row Level Security Policies
-- Run this AFTER schema.sql in Supabase SQL Editor

-- ============================================
-- ENABLE RLS ON ALL TABLES
-- ============================================
ALTER TABLE super_admins ENABLE ROW LEVEL SECURITY;
ALTER TABLE administrators ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_models ENABLE ROW LEVEL SECURITY;
ALTER TABLE activity_logs ENABLE ROW LEVEL SECURITY;

-- ============================================
-- SUPER ADMINS POLICIES
-- ============================================
-- Super admins can view all super admins
CREATE POLICY "Super admins can view all super admins"
    ON super_admins FOR SELECT
    USING (auth.jwt() ->> 'email' IN (SELECT email FROM super_admins WHERE is_active = true));

-- Super admins can update super admins
CREATE POLICY "Super admins can update super admins"
    ON super_admins FOR UPDATE
    USING (auth.jwt() ->> 'email' IN (SELECT email FROM super_admins WHERE is_active = true));

-- Super admins can insert super admins
CREATE POLICY "Super admins can insert super admins"
    ON super_admins FOR INSERT
    WITH CHECK (auth.jwt() ->> 'email' IN (SELECT email FROM super_admins WHERE is_active = true));

-- ============================================
-- ADMINISTRATORS POLICIES
-- ============================================
-- Super admins can view all administrators
CREATE POLICY "Super admins can view all administrators"
    ON administrators FOR SELECT
    USING (auth.jwt() ->> 'email' IN (SELECT email FROM super_admins WHERE is_active = true));

-- Administrators can view their own record
CREATE POLICY "Administrators can view own record"
    ON administrators FOR SELECT
    USING (email = auth.jwt() ->> 'email' AND is_active = true);

-- Super admins can manage administrators
CREATE POLICY "Super admins can insert administrators"
    ON administrators FOR INSERT
    WITH CHECK (auth.jwt() ->> 'email' IN (SELECT email FROM super_admins WHERE is_active = true));

CREATE POLICY "Super admins can update administrators"
    ON administrators FOR UPDATE
    USING (auth.jwt() ->> 'email' IN (SELECT email FROM super_admins WHERE is_active = true));

-- ============================================
-- CUSTOMERS POLICIES
-- ============================================
-- Super admins can view all customers
CREATE POLICY "Super admins can view all customers"
    ON customers FOR SELECT
    USING (auth.jwt() ->> 'email' IN (SELECT email FROM super_admins WHERE is_active = true));

-- Administrators can view customers they enrolled
CREATE POLICY "Administrators can view their customers"
    ON customers FOR SELECT
    USING (
        enrolled_by IN (
            SELECT id FROM administrators 
            WHERE email = auth.jwt() ->> 'email' AND is_active = true
        )
    );

-- Administrators can insert customers
CREATE POLICY "Administrators can insert customers"
    ON customers FOR INSERT
    WITH CHECK (
        enrolled_by IN (
            SELECT id FROM administrators 
            WHERE email = auth.jwt() ->> 'email' AND is_active = true
        )
    );

-- Administrators can update their customers
CREATE POLICY "Administrators can update their customers"
    ON customers FOR UPDATE
    USING (
        enrolled_by IN (
            SELECT id FROM administrators 
            WHERE email = auth.jwt() ->> 'email' AND is_active = true
        )
    );

-- ============================================
-- DEVICES POLICIES
-- ============================================
-- Super admins can view all devices
CREATE POLICY "Super admins can view all devices"
    ON devices FOR SELECT
    USING (auth.jwt() ->> 'email' IN (SELECT email FROM super_admins WHERE is_active = true));

-- Administrators can view devices they enrolled
CREATE POLICY "Administrators can view their devices"
    ON devices FOR SELECT
    USING (
        enrolled_by IN (
            SELECT id FROM administrators 
            WHERE email = auth.jwt() ->> 'email' AND is_active = true
        )
    );

-- Devices can view their own record (for Android app sync)
CREATE POLICY "Devices can view own record"
    ON devices FOR SELECT
    USING (device_code = auth.jwt() ->> 'device_code');

-- Administrators can insert devices
CREATE POLICY "Administrators can insert devices"
    ON devices FOR INSERT
    WITH CHECK (
        enrolled_by IN (
            SELECT id FROM administrators 
            WHERE email = auth.jwt() ->> 'email' AND is_active = true
        )
    );

-- Administrators and devices can update devices
CREATE POLICY "Administrators can update their devices"
    ON devices FOR UPDATE
    USING (
        enrolled_by IN (
            SELECT id FROM administrators 
            WHERE email = auth.jwt() ->> 'email' AND is_active = true
        )
    );

CREATE POLICY "Devices can update own record"
    ON devices FOR UPDATE
    USING (device_code = auth.jwt() ->> 'device_code');

-- ============================================
-- PAYMENT TRANSACTIONS POLICIES
-- ============================================
-- Super admins can view all transactions
CREATE POLICY "Super admins can view all transactions"
    ON payment_transactions FOR SELECT
    USING (auth.jwt() ->> 'email' IN (SELECT email FROM super_admins WHERE is_active = true));

-- Administrators can view transactions for their devices
CREATE POLICY "Administrators can view their transactions"
    ON payment_transactions FOR SELECT
    USING (
        device_id IN (
            SELECT id FROM devices 
            WHERE enrolled_by IN (
                SELECT id FROM administrators 
                WHERE email = auth.jwt() ->> 'email' AND is_active = true
            )
        )
    );

-- Administrators can insert transactions
CREATE POLICY "Administrators can insert transactions"
    ON payment_transactions FOR INSERT
    WITH CHECK (
        processed_by IN (
            SELECT id FROM administrators 
            WHERE email = auth.jwt() ->> 'email' AND is_active = true
        )
    );

-- Backend service can insert transactions (for Web3 listener)
CREATE POLICY "Service role can insert transactions"
    ON payment_transactions FOR INSERT
    WITH CHECK (auth.jwt() ->> 'role' = 'service_role');

-- ============================================
-- DEVICE MODELS POLICIES
-- ============================================
-- Everyone can view active device models
CREATE POLICY "Anyone can view active device models"
    ON device_models FOR SELECT
    USING (is_active = true);

-- Super admins can manage device models
CREATE POLICY "Super admins can manage device models"
    ON device_models FOR ALL
    USING (auth.jwt() ->> 'email' IN (SELECT email FROM super_admins WHERE is_active = true));

-- ============================================
-- ACTIVITY LOGS POLICIES
-- ============================================
-- Super admins can view all logs
CREATE POLICY "Super admins can view all logs"
    ON activity_logs FOR SELECT
    USING (auth.jwt() ->> 'email' IN (SELECT email FROM super_admins WHERE is_active = true));

-- Administrators can view their own logs
CREATE POLICY "Administrators can view own logs"
    ON activity_logs FOR SELECT
    USING (
        user_id IN (
            SELECT id FROM administrators 
            WHERE email = auth.jwt() ->> 'email' AND is_active = true
        )
    );

-- Anyone authenticated can insert logs
CREATE POLICY "Authenticated users can insert logs"
    ON activity_logs FOR INSERT
    WITH CHECK (auth.role() = 'authenticated' OR auth.jwt() ->> 'role' = 'service_role');

-- ============================================
-- GRANT PERMISSIONS
-- ============================================
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL TABLES IN SCHEMA public TO service_role;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO authenticated;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO authenticated, service_role;
