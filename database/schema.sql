-- Eden M-Kopa Database Schema
-- Run this in Supabase SQL Editor

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- SUPER ADMINS TABLE
-- ============================================
CREATE TABLE super_admins (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    phone TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================
-- ADMINISTRATORS TABLE
-- ============================================
CREATE TABLE administrators (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admin_code TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    phone TEXT NOT NULL,
    created_by UUID REFERENCES super_admins(id),
    region TEXT,
    branch TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================
-- CUSTOMERS TABLE
-- ============================================
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_code TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    phone TEXT NOT NULL,
    national_id TEXT UNIQUE NOT NULL,
    address TEXT,
    enrolled_by UUID REFERENCES administrators(id),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================
-- DEVICES TABLE
-- ============================================
CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_code TEXT UNIQUE NOT NULL,
    imei TEXT UNIQUE NOT NULL,
    customer_id UUID REFERENCES customers(id),
    enrolled_by UUID REFERENCES administrators(id),
    wallet_address TEXT,
    device_model TEXT NOT NULL,
    device_price DECIMAL(10, 2) NOT NULL,
    down_payment DECIMAL(10, 2) NOT NULL DEFAULT 0,
    loan_total DECIMAL(10, 2) NOT NULL,
    loan_balance DECIMAL(10, 2) NOT NULL,
    daily_payment DECIMAL(10, 2) NOT NULL,
    total_paid DECIMAL(10, 2) DEFAULT 0,
    payment_period_days INTEGER NOT NULL,
    next_payment_due DATE,
    days_overdue INTEGER DEFAULT 0,
    is_locked BOOLEAN DEFAULT true,
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'paid_off', 'defaulted', 'retired')),
    last_sync TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================
-- PAYMENT TRANSACTIONS TABLE
-- ============================================
CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_code TEXT UNIQUE NOT NULL,
    device_id UUID REFERENCES devices(id),
    customer_id UUID REFERENCES customers(id),
    amount DECIMAL(10, 2) NOT NULL,
    payment_method TEXT NOT NULL CHECK (payment_method IN ('mpesa', 'cash', 'bank', 'crypto')),
    mpesa_receipt TEXT,
    mpesa_phone TEXT,
    wallet_transaction_hash TEXT,
    status TEXT DEFAULT 'completed' CHECK (status IN ('pending', 'completed', 'failed', 'reversed')),
    processed_by UUID REFERENCES administrators(id),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================
-- DEVICE MODELS TABLE
-- ============================================
CREATE TABLE device_models (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================
-- ACTIVITY LOGS TABLE
-- ============================================
CREATE TABLE activity_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID,
    user_type TEXT CHECK (user_type IN ('super_admin', 'administrator', 'device')),
    action TEXT NOT NULL,
    entity_type TEXT,
    entity_id UUID,
    details JSONB,
    ip_address TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================
-- FUNCTIONS
-- ============================================

-- Function to generate customer code
CREATE OR REPLACE FUNCTION generate_customer_code()
RETURNS TRIGGER AS $$
BEGIN
    NEW.customer_code := 'CUST' || LPAD(NEXTVAL('customer_code_seq')::TEXT, 6, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to generate device code
CREATE OR REPLACE FUNCTION generate_device_code()
RETURNS TRIGGER AS $$
BEGIN
    NEW.device_code := 'DEV' || LPAD(NEXTVAL('device_code_seq')::TEXT, 6, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to generate admin code
CREATE OR REPLACE FUNCTION generate_admin_code()
RETURNS TRIGGER AS $$
BEGIN
    NEW.admin_code := 'ADM' || LPAD(NEXTVAL('admin_code_seq')::TEXT, 6, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to generate transaction code
CREATE OR REPLACE FUNCTION generate_transaction_code()
RETURNS TRIGGER AS $$
BEGIN
    NEW.transaction_code := 'TXN' || TO_CHAR(NOW(), 'YYYYMMDD') || LPAD(NEXTVAL('transaction_code_seq')::TEXT, 6, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to update device balance and lock status
CREATE OR REPLACE FUNCTION update_device_on_payment()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'completed' THEN
        UPDATE devices
        SET 
            loan_balance = loan_balance - NEW.amount,
            total_paid = total_paid + NEW.amount,
            is_locked = CASE WHEN (loan_balance - NEW.amount) <= 0 THEN false ELSE true END,
            status = CASE WHEN (loan_balance - NEW.amount) <= 0 THEN 'paid_off' ELSE status END,
            updated_at = NOW()
        WHERE id = NEW.device_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to update timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- SEQUENCES
-- ============================================
CREATE SEQUENCE customer_code_seq START 1;
CREATE SEQUENCE device_code_seq START 1;
CREATE SEQUENCE admin_code_seq START 1;
CREATE SEQUENCE transaction_code_seq START 1;

-- ============================================
-- TRIGGERS
-- ============================================

-- Customer code generation
CREATE TRIGGER trigger_generate_customer_code
    BEFORE INSERT ON customers
    FOR EACH ROW
    EXECUTE FUNCTION generate_customer_code();

-- Device code generation
CREATE TRIGGER trigger_generate_device_code
    BEFORE INSERT ON devices
    FOR EACH ROW
    EXECUTE FUNCTION generate_device_code();

-- Admin code generation
CREATE TRIGGER trigger_generate_admin_code
    BEFORE INSERT ON administrators
    FOR EACH ROW
    EXECUTE FUNCTION generate_admin_code();

-- Transaction code generation
CREATE TRIGGER trigger_generate_transaction_code
    BEFORE INSERT ON payment_transactions
    FOR EACH ROW
    EXECUTE FUNCTION generate_transaction_code();

-- Update device on payment
CREATE TRIGGER trigger_update_device_on_payment
    AFTER INSERT ON payment_transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_device_on_payment();

-- Update timestamps
CREATE TRIGGER trigger_update_super_admins_timestamp
    BEFORE UPDATE ON super_admins
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_update_administrators_timestamp
    BEFORE UPDATE ON administrators
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_update_customers_timestamp
    BEFORE UPDATE ON customers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_update_devices_timestamp
    BEFORE UPDATE ON devices
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- INDEXES
-- ============================================
CREATE INDEX idx_devices_customer_id ON devices(customer_id);
CREATE INDEX idx_devices_enrolled_by ON devices(enrolled_by);
CREATE INDEX idx_devices_is_locked ON devices(is_locked);
CREATE INDEX idx_devices_status ON devices(status);
CREATE INDEX idx_devices_wallet_address ON devices(wallet_address);
CREATE INDEX idx_payment_transactions_device_id ON payment_transactions(device_id);
CREATE INDEX idx_payment_transactions_customer_id ON payment_transactions(customer_id);
CREATE INDEX idx_customers_enrolled_by ON customers(enrolled_by);
CREATE INDEX idx_administrators_created_by ON administrators(created_by);

-- ============================================
-- SAMPLE DATA (Optional)
-- ============================================

-- Insert sample device models
INSERT INTO device_models (name, price, category) VALUES
('Samsung Galaxy A14', 15000, 'smartphone'),
('Tecno Spark 10', 12000, 'smartphone'),
('Infinix Hot 30', 13500, 'smartphone'),
('Nokia G21', 14000, 'smartphone');

-- Insert first super admin (Update with your email)
INSERT INTO super_admins (email, full_name, phone) VALUES
('admin@edenservices.ke', 'System Administrator', '+254700000000');

COMMENT ON TABLE super_admins IS 'System owners who manage administrators';
COMMENT ON TABLE administrators IS 'Field agents who enroll customers and process payments';
COMMENT ON TABLE customers IS 'Device buyers';
COMMENT ON TABLE devices IS 'Enrolled devices with payment tracking';
COMMENT ON TABLE payment_transactions IS 'All payment records';

-- Admins table
CREATE TABLE IF NOT EXISTS admins (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50) DEFAULT 'admin',
    created_at TIMESTAMP DEFAULT NOW()
);

-- Admin sessions table
CREATE TABLE IF NOT EXISTS admin_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admin_id UUID REFERENCES admins(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Update devices table to include serial number and customer details
ALTER TABLE devices ADD COLUMN IF NOT EXISTS serial_number VARCHAR(255);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS customer_name VARCHAR(255);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(50);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS national_id VARCHAR(50);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS id_front_url TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS id_back_url TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS passport_photo_url TEXT;
