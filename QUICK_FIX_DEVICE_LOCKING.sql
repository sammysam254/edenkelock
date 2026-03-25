-- ============================================
-- QUICK FIX FOR DEVICE LOCKING ISSUES
-- ============================================
-- Run this in Supabase SQL Editor to fix remote device locking
-- ============================================

-- Add missing columns needed for device locking
BEGIN;

-- Add is_locked column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'is_locked'
    ) THEN
        ALTER TABLE devices ADD COLUMN is_locked BOOLEAN DEFAULT TRUE;
        RAISE NOTICE '✓ Added is_locked column';
    ELSE
        RAISE NOTICE '✓ is_locked column already exists';
    END IF;
END $$;

-- Add lock_reason column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'lock_reason'
    ) THEN
        ALTER TABLE devices ADD COLUMN lock_reason TEXT;
        RAISE NOTICE '✓ Added lock_reason column';
    ELSE
        RAISE NOTICE '✓ lock_reason column already exists';
    END IF;
END $$;

-- Add locked_by column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'locked_by'
    ) THEN
        ALTER TABLE devices ADD COLUMN locked_by TEXT;
        RAISE NOTICE '✓ Added locked_by column';
    ELSE
        RAISE NOTICE '✓ locked_by column already exists';
    END IF;
END $$;

-- Add locked_at column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'locked_at'
    ) THEN
        ALTER TABLE devices ADD COLUMN locked_at TIMESTAMP WITH TIME ZONE;
        RAISE NOTICE '✓ Added locked_at column';
    ELSE
        RAISE NOTICE '✓ locked_at column already exists';
    END IF;
END $$;

-- Add token column to admins table if it doesn't exist (for authentication)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'admins' 
        AND column_name = 'token'
    ) THEN
        ALTER TABLE admins ADD COLUMN token TEXT;
        RAISE NOTICE '✓ Added token column to admins table';
    ELSE
        RAISE NOTICE '✓ token column already exists in admins table';
    END IF;
END $$;

-- Update existing devices to have proper default values
UPDATE devices 
SET is_locked = CASE 
    WHEN status = 'locked' THEN TRUE 
    ELSE FALSE 
END
WHERE is_locked IS NULL;

-- Show verification of columns
SELECT 
    '📋 DEVICE LOCKING COLUMNS VERIFICATION' as info,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'devices'
AND column_name IN ('is_locked', 'lock_reason', 'locked_by', 'locked_at')
ORDER BY column_name;

COMMIT;