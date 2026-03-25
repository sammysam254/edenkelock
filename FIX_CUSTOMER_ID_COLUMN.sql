-- Fix missing customer_id column in devices table
-- This script adds the customer_id column and populates it with national_id values

-- Add customer_id column if it doesn't exist
ALTER TABLE devices ADD COLUMN IF NOT EXISTS customer_id TEXT;

-- Populate customer_id with national_id values for existing records
UPDATE devices SET customer_id = national_id WHERE customer_id IS NULL;

-- Make customer_id NOT NULL after populating
ALTER TABLE devices ALTER COLUMN customer_id SET NOT NULL;

-- Add index for customer_id
CREATE INDEX IF NOT EXISTS idx_devices_customer_id ON devices(customer_id);

-- Verify the fix
SELECT 'customer_id column added successfully' as status;
SELECT COUNT(*) as total_devices, COUNT(customer_id) as devices_with_customer_id FROM devices;

-- Show sample data to verify
SELECT device_id, customer_id, national_id, customer_name, customer_phone 
FROM devices 
LIMIT 5;