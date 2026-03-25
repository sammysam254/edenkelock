-- ============================================
-- FIX PIN_HASH CONSTRAINT FOR SELF-REGISTRATION
-- ============================================
-- The pin_hash column has a NOT NULL constraint
-- We need to modify the constraint first, then reset devices
-- ============================================

-- Step 1: Remove NOT NULL constraint from pin_hash column
ALTER TABLE devices ALTER COLUMN pin_hash DROP NOT NULL;

-- Step 2: Reset existing devices to pending registration
UPDATE devices 
SET 
    pin_hash = NULL,
    status = 'pending_registration',
    is_locked = TRUE,
    must_change_pin = FALSE
WHERE 
    pin_hash IS NOT NULL 
    AND customer_phone IS NOT NULL
    AND status != 'deleted';

-- Step 3: Clear tokens if column exists
DO $$
BEGIN
    IF EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'token'
    ) THEN
        UPDATE devices SET token = NULL 
        WHERE status = 'pending_registration' AND customer_phone IS NOT NULL;
    END IF;
END $$;

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
    COALESCE(total_amount, 0) - COALESCE(amount_paid, 0) as loan_balance
FROM devices 
WHERE status = 'pending_registration'
AND customer_phone IS NOT NULL
ORDER BY customer_name;

COMMIT;