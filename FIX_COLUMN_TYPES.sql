-- Fix Column Types for Eden
-- Run this in Supabase SQL Editor

-- ============================================
-- FIX DEVICES TABLE COLUMN TYPES
-- ============================================

-- Drop the customer_id column if it's UUID type and recreate as TEXT
ALTER TABLE devices DROP COLUMN IF EXISTS customer_id CASCADE;
ALTER TABLE devices ADD COLUMN customer_id TEXT;

-- Ensure all other columns are correct types
ALTER TABLE devices ALTER COLUMN device_id TYPE TEXT;
ALTER TABLE devices ALTER COLUMN serial_number TYPE TEXT;
ALTER TABLE devices ALTER COLUMN national_id TYPE TEXT;
ALTER TABLE devices ALTER COLUMN customer_name TYPE TEXT;
ALTER TABLE devices ALTER COLUMN customer_phone TYPE TEXT;
ALTER TABLE devices ALTER COLUMN id_front_url TYPE TEXT;
ALTER TABLE devices ALTER COLUMN id_back_url TYPE TEXT;
ALTER TABLE devices ALTER COLUMN passport_photo_url TYPE TEXT;

-- ============================================
-- FIX PAYMENT_TRANSACTIONS TABLE
-- ============================================
ALTER TABLE payment_transactions DROP COLUMN IF EXISTS device_id CASCADE;
ALTER TABLE payment_transactions ADD COLUMN device_id TEXT;

ALTER TABLE payment_transactions DROP COLUMN IF EXISTS customer_id CASCADE;
ALTER TABLE payment_transactions ADD COLUMN customer_id TEXT;

-- ============================================
-- RECREATE INDEXES
-- ============================================
DROP INDEX IF EXISTS idx_devices_customer_phone;
DROP INDEX IF EXISTS idx_devices_device_id;
DROP INDEX IF EXISTS idx_devices_status;
DROP INDEX IF EXISTS idx_payment_transactions_device_id;

CREATE INDEX idx_devices_customer_phone ON devices(customer_phone);
CREATE INDEX idx_devices_device_id ON devices(device_id);
CREATE INDEX idx_devices_status ON devices(status);
CREATE INDEX idx_payment_transactions_device_id ON payment_transactions(device_id);

-- ============================================
-- VERIFY COLUMN TYPES
-- ============================================
SELECT 
    column_name, 
    data_type,
    character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'devices'
AND column_name IN ('device_id', 'customer_id', 'serial_number', 'national_id', 
                    'customer_name', 'customer_phone')
ORDER BY ordinal_position;
