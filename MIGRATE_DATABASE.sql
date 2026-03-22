-- Eden Database Migration
-- Run this in Supabase SQL Editor to add missing columns

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- ADD MISSING COLUMNS TO DEVICES TABLE
-- ============================================
ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_id TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS customer_id TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS serial_number TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS national_id TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS customer_name TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS customer_phone TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS total_amount DECIMAL(10, 2) DEFAULT 0;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS amount_paid DECIMAL(10, 2) DEFAULT 0;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'locked';
ALTER TABLE devices ADD COLUMN IF NOT EXISTS id_front_url TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS id_back_url TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS passport_photo_url TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();
ALTER TABLE devices ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW();

-- Add constraint for status if not exists
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'devices_status_check'
    ) THEN
        ALTER TABLE devices ADD CONSTRAINT devices_status_check 
        CHECK (status IN ('active', 'locked', 'paid_off'));
    END IF;
END $$;

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

-- ============================================
-- CREATE ADMIN SESSIONS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS admin_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admin_id UUID REFERENCES admins(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- ============================================
-- CREATE PAYMENT TRANSACTIONS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID,
    customer_id TEXT,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method TEXT DEFAULT 'mpesa',
    status TEXT DEFAULT 'completed',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

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

-- ============================================
-- CREATE CUSTOMER SESSIONS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS customer_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone_number VARCHAR(50) NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- ============================================
-- CREATE INDEXES
-- ============================================
CREATE INDEX IF NOT EXISTS idx_devices_customer_phone ON devices(customer_phone);
CREATE INDEX IF NOT EXISTS idx_devices_device_id ON devices(device_id);
CREATE INDEX IF NOT EXISTS idx_devices_status ON devices(status);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_device_id ON payment_transactions(device_id);
CREATE INDEX IF NOT EXISTS idx_admins_email ON admins(email);
CREATE INDEX IF NOT EXISTS idx_customer_accounts_phone ON customer_accounts(phone_number);

-- ============================================
-- INSERT/UPDATE SUPER ADMIN
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
-- VERIFY SETUP
-- ============================================
SELECT 'Super Admin Created:' as status, email, full_name, role 
FROM admins 
WHERE email = 'sammyseth260@gmail.com';

SELECT 'Devices Table Columns:' as status, column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'devices' 
ORDER BY ordinal_position;
