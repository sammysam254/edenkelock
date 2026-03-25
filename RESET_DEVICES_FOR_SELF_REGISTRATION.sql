-- ============================================
-- RESET EXISTING DEVICES FOR SELF-REGISTRATION
-- ============================================
-- This script resets existing enrolled devices to allow
-- customers to register themselves with their own PINs
-- ============================================

-- Step 1: Add missing columns if they don't exist
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
    END IF;

    -- Add must_change_pin column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'must_change_pin'
    ) THEN
        ALTER TABLE devices ADD COLUMN must_change_pin BOOLEAN DEFAULT FALSE;
    END IF;

    -- Add is_locked column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'is_locked'
    ) THEN
        ALTER TABLE devices ADD COLUMN is_locked BOOLEAN DEFAULT TRUE;
    END IF;

    -- Add enrolled_by column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'enrolled_by'
    ) THEN
        ALTER TABLE devices ADD COLUMN enrolled_by TEXT;
    END IF;

    -- Add imei column if it doesn't exist
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'imei'
    ) THEN
        ALTER TABLE devices ADD COLUMN imei TEXT;
    END IF;
END $$;

-- Step 2: Remove NOT NULL constraint from pin_hash if it exists
ALTER TABLE devices ALTER COLUMN pin_hash DROP NOT NULL;

-- Step 3: Reset existing devices to pending registration
UPDATE devices 
SET 
    pin_hash = NULL,
    status = 'pending_registration',
    is_locked = TRUE,
    must_change_pin = FALSE
WHERE 
    customer_phone IS NOT NULL
    AND status != 'deleted'
    AND status != 'pending_registration';

-- Step 4: Show results
SELECT 
    'MIGRATION COMPLETE' as status,
    COUNT(*) as devices_reset,
    COUNT(DISTINCT customer_phone) as customers_affected
FROM devices 
WHERE status = 'pending_registration'
AND customer_phone IS NOT NULL;

-- Step 5: List customers for communication
SELECT 
    customer_name,
    customer_phone,
    device_id,
    COALESCE(total_amount, 0) - COALESCE(amount_paid, 0) as loan_balance,
    status
FROM devices 
WHERE status = 'pending_registration'
AND customer_phone IS NOT NULL
ORDER BY customer_name;

COMMIT;