-- Check what columns exist in each table
-- Run this first to see the actual schema

-- Check admins table structure
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'admins'
ORDER BY ordinal_position;

-- Check customers table structure
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'customers'
ORDER BY ordinal_position;

-- Check devices table structure
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'devices'
ORDER BY ordinal_position;

-- Check payments table structure
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'payments'
ORDER BY ordinal_position;
