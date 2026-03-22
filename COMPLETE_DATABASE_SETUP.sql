-- COMPLETE DATABASE SETUP FOR EDEN
-- Run this ENTIRE script in Supabase SQL Editor
-- This will fix ALL issues once and for all

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- DROP AND RECREATE DEVICES TABLE
-- ============================================
DROP TABLE IF EXISTS devices CASCADE;

CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id TEXT,
    customer_id TEXT,
    serial_number TEXT,
    national_id TEXT,
    customer_name TEXT,
    customer_phone TEXT,
    total_amount DECIMAL(10, 2) DEFAULT 0,
    amount_paid DECIMAL(10, 2) DEFAULT 0,
    status TEXT DEFAULT 'locked',
    id_front_url TEXT,
    id_back_url TEXT,
    passport_photo_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Disable RLS on devices
ALTER TABLE devices DISABLE ROW LEVEL SECURITY;

-- ============================================
-- DROP AND RECREATE PAYMENT_TRANSACTIONS TABLE
-- ============================================
DROP TABLE IF EXISTS payment_transactions CASCADE;

CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id TEXT,
    customer_id TEXT,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method TEXT DEFAULT 'mpesa',
    status TEXT DEFAULT 'completed',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Disable RLS on payment_transactions
ALTER TABLE payment_transactions DISABLE ROW LEVEL SECURITY;

-- ============================================
-- CREATE ADMINS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS admins (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50) DEFAULT 'admin',
    created_at TIMESTAMP DEFAULT NOW()
);

-- Disable RLS on admins
ALTER TABLE admins DISABLE ROW LEVEL SECURITY;

-- ============================================
-- CREATE ADMIN SESSIONS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS admin_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admin_id UUID REFERENCES admins(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Disable RLS on admin_sessions
ALTER TABLE admin_sessions DISABLE ROW LEVEL SECURITY;

-- ============================================
-- CREATE CUSTOMER ACCOUNTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS customer_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone_number VARCHAR(50) UNIQUE NOT NULL,
    pin_hash VARCHAR(255),
    is_pin_set BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Disable RLS on customer_accounts
ALTER TABLE customer_accounts DISABLE ROW LEVEL SECURITY;

-- ============================================
-- CREATE CUSTOMER SESSIONS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS customer_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone_number VARCHAR(50) NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Disable RLS on customer_sessions
ALTER TABLE customer_sessions DISABLE ROW LEVEL SECURITY;

-- ============================================
-- CREATE INDEXES
-- ============================================
DROP INDEX IF EXISTS idx_devices_customer_phone;
DROP INDEX IF EXISTS idx_devices_device_id;
DROP INDEX IF EXISTS idx_devices_status;
DROP INDEX IF EXISTS idx_payment_transactions_device_id;
DROP INDEX IF EXISTS idx_admins_email;
DROP INDEX IF EXISTS idx_customer_accounts_phone;

CREATE INDEX idx_devices_customer_phone ON devices(customer_phone);
CREATE INDEX idx_devices_device_id ON devices(device_id);
CREATE INDEX idx_devices_status ON devices(status);
CREATE INDEX idx_payment_transactions_device_id ON payment_transactions(device_id);
CREATE INDEX idx_admins_email ON admins(email);
CREATE INDEX idx_customer_accounts_phone ON customer_accounts(phone_number);

-- ============================================
-- INSERT SUPER ADMIN
-- ============================================
-- Email: sammyseth260@gmail.com
-- Password: 58369234
-- Password Hash: 5ba3e91e5a1c15b76194ca105cb345523bb7cdac33a708d4491484eb4a13e6ed

INSERT INTO admins (email, password_hash, full_name, role)
VALUES (
    'sammyseth260@gmail.com',
    '5ba3e91e5a1c15b76194ca105cb345523bb7cdac33a708d4491484eb4a13e6ed',
    'Super Administrator',
    'super_admin'
)
ON CONFLICT (email) DO UPDATE 
SET password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role;

-- ============================================
-- DISABLE RLS ON ALL OTHER TABLES (if they exist)
-- ============================================
ALTER TABLE IF EXISTS super_admins DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS administrators DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS customers DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS device_models DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS activity_logs DISABLE ROW LEVEL SECURITY;

-- ============================================
-- DROP ALL POLICIES
-- ============================================
DO $$ 
DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT schemaname, tablename, policyname 
              FROM pg_policies 
              WHERE schemaname = 'public') 
    LOOP
        EXECUTE 'DROP POLICY IF EXISTS ' || quote_ident(r.policyname) || 
                ' ON ' || quote_ident(r.schemaname) || '.' || quote_ident(r.tablename);
    END LOOP;
END $$;

-- ============================================
-- VERIFICATION
-- ============================================
SELECT 'Super Admin:' as info, email, full_name, role 
FROM admins 
WHERE email = 'sammyseth260@gmail.com';

SELECT 'Devices Table Structure:' as info, column_name, data_type, is_nullable
FROM information_schema.columns 
WHERE table_name = 'devices'
ORDER BY ordinal_position;

SELECT 'RLS Status:' as info, tablename,
    CASE WHEN rowsecurity THEN 'ENABLED' ELSE 'DISABLED' END as rls_status
FROM pg_tables t
JOIN pg_class c ON t.tablename = c.relname
WHERE schemaname = 'public'
AND tablename IN ('admins', 'devices', 'payment_transactions', 'customer_accounts')
ORDER BY tablename;
