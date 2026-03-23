-- Check what columns exist in the admins table
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'admins'
ORDER BY ordinal_position;
