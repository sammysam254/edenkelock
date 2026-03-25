-- Fix Device Enrollment - Add ALL Missing Columns to devices table

-- Add missing columns for device enrollment
ALTER TABLE devices ADD COLUMN IF NOT EXISTS id_front_url TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS id_back_url TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS passport_photo_url TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS imei TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS enrolled_by UUID;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS must_change_pin BOOLEAN DEFAULT false;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_fingerprint TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS lock_reason TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS locked_by UUID;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT false;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS deleted_by UUID;

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_devices_imei ON devices(imei);
CREATE INDEX IF NOT EXISTS idx_devices_enrolled_by ON devices(enrolled_by);
CREATE INDEX IF NOT EXISTS idx_devices_device_fingerprint ON devices(device_fingerprint);
CREATE INDEX IF NOT EXISTS idx_devices_customer_phone ON devices(customer_phone);
CREATE INDEX IF NOT EXISTS idx_devices_device_id ON devices(device_id);

-- Verification query
SELECT 
    column_name, 
    data_type, 
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'devices' 
ORDER BY ordinal_position;