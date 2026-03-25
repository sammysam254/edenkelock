-- ============================================
-- FIX REGISTRATION COLUMNS
-- ============================================
-- Add missing columns needed for customer registration
-- ============================================

-- Add missing columns if they don't exist
DO $$ 
BEGIN
    -- Add pin_hash column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'pin_hash'
    ) THEN
        ALTER TABLE devices ADD COLUMN pin_hash TEXT;
        RAISE NOTICE 'Added pin_hash column';
    END IF;

    -- Add must_change_pin column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'must_change_pin'
    ) THEN
        ALTER TABLE devices ADD COLUMN must_change_pin BOOLEAN DEFAULT FALSE;
        RAISE NOTICE 'Added must_change_pin column';
    END IF;

    -- Add is_locked column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'is_locked'
    ) THEN
        ALTER TABLE devices ADD COLUMN is_locked BOOLEAN DEFAULT TRUE;
        RAISE NOTICE 'Added is_locked column';
    END IF;

    -- Add enrolled_by column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'enrolled_by'
    ) THEN
        ALTER TABLE devices ADD COLUMN enrolled_by TEXT;
        RAISE NOTICE 'Added enrolled_by column';
    END IF;

    -- Add imei column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'imei'
    ) THEN
        ALTER TABLE devices ADD COLUMN imei TEXT;
        RAISE NOTICE 'Added imei column';
    END IF;

    -- Add registered_at column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'registered_at'
    ) THEN
        ALTER TABLE devices ADD COLUMN registered_at TIMESTAMP WITH TIME ZONE;
        RAISE NOTICE 'Added registered_at column';
    END IF;

    -- Add last_login column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'last_login'
    ) THEN
        ALTER TABLE devices ADD COLUMN last_login TIMESTAMP WITH TIME ZONE;
        RAISE NOTICE 'Added last_login column';
    END IF;

    -- Add device_fingerprint column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'device_fingerprint'
    ) THEN
        ALTER TABLE devices ADD COLUMN device_fingerprint TEXT;
        RAISE NOTICE 'Added device_fingerprint column';
    END IF;

    -- Add token column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'token'
    ) THEN
        ALTER TABLE devices ADD COLUMN token TEXT;
        RAISE NOTICE 'Added token column';
    END IF;

    -- Add lock_reason column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'lock_reason'
    ) THEN
        ALTER TABLE devices ADD COLUMN lock_reason TEXT;
        RAISE NOTICE 'Added lock_reason column';
    END IF;

    -- Add locked_by column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'locked_by'
    ) THEN
        ALTER TABLE devices ADD COLUMN locked_by TEXT;
        RAISE NOTICE 'Added locked_by column';
    END IF;

    -- Add locked_at column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'locked_at'
    ) THEN
        ALTER TABLE devices ADD COLUMN locked_at TIMESTAMP WITH TIME ZONE;
        RAISE NOTICE 'Added locked_at column';
    END IF;
END $$;

-- Remove NOT NULL constraint from pin_hash if it exists
DO $$
BEGIN
    ALTER TABLE devices ALTER COLUMN pin_hash DROP NOT NULL;
    RAISE NOTICE 'Removed NOT NULL constraint from pin_hash';
EXCEPTION
    WHEN OTHERS THEN
        RAISE NOTICE 'pin_hash column does not have NOT NULL constraint or does not exist';
END $$;

-- Add missing columns to admins table if they don't exist
DO $$ 
BEGIN
    -- Add token column to admins table if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'admins' 
        AND column_name = 'token'
    ) THEN
        ALTER TABLE admins ADD COLUMN token TEXT;
        RAISE NOTICE 'Added token column to admins table';
    END IF;

    -- Add last_login column to admins table if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'admins' 
        AND column_name = 'last_login'
    ) THEN
        ALTER TABLE admins ADD COLUMN last_login TIMESTAMP WITH TIME ZONE;
        RAISE NOTICE 'Added last_login column to admins table';
    END IF;
END $$;

-- Show current table structure
SELECT 
    'COLUMN VERIFICATION' as info,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'devices'
AND column_name IN ('pin_hash', 'is_locked', 'registered_at', 'last_login', 'device_fingerprint', 'token', 'must_change_pin', 'enrolled_by', 'imei', 'lock_reason', 'locked_by', 'locked_at')
ORDER BY column_name;

COMMIT;