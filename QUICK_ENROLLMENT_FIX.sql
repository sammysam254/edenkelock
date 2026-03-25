-- QUICK FIX: Add only the essential columns for device enrollment to work

-- These are the columns causing the PGRST204 error
ALTER TABLE devices ADD COLUMN IF NOT EXISTS id_front_url TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN IF NOT EXISTS id_back_url TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN IF NOT EXISTS passport_photo_url TEXT DEFAULT '';
ALTER TABLE devices ADD COLUMN IF NOT EXISTS imei TEXT;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS enrolled_by UUID;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS must_change_pin BOOLEAN DEFAULT true;

-- Verify the fix
SELECT 'Device enrollment columns added successfully!' as status;