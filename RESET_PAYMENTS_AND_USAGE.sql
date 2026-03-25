-- ============================================
-- RESET PAYMENTS AND USAGE DAYS
-- ============================================
-- This script resets all payment balances and usage days to zero
-- so customers start fresh with the new payment system
-- 
-- WHAT THIS DOES:
-- 1. Resets payment_balance to 0 (no active payment days)
-- 2. Keeps downpayment intact (it's a deposit, not for unlock)
-- 3. Resets unlock_until to NULL (no active unlock period)
-- 4. Keeps amount_paid and total_amount (loan tracking)
-- 5. Sets trial_period to NULL (trial expired)
-- 6. Locks all devices (requires payment to unlock)
-- 7. Clears last payment tracking
-- 8. Ensures daily_payment_amount is set (default 80 KES)
--
-- RUN THIS IN SUPABASE SQL EDITOR
-- ============================================

BEGIN;

-- Step 1: Reset payment balances and usage days
UPDATE devices
SET 
    payment_balance = 0,                    -- No active payment balance
    unlock_until = NULL,                    -- No active unlock period
    trial_period = NULL,                    -- Trial period expired
    last_payment_date = NULL,               -- Clear last payment
    last_payment_amount = 0,                -- Clear last payment amount
    status = 'locked',                      -- Lock all devices
    is_locked = TRUE                        -- Ensure locked flag is set
WHERE 
    device_id IS NOT NULL;                  -- All devices

-- Step 2: Ensure all devices have daily_payment_amount set
-- Default to 80 KES if not set
UPDATE devices
SET 
    daily_payment_amount = 80
WHERE 
    daily_payment_amount IS NULL 
    OR daily_payment_amount = 0;

-- Step 3: Clear lock reasons (not admin locked, just payment required)
UPDATE devices
SET 
    lock_reason = 'PAYMENT_REQUIRED',
    locked_by = NULL,
    locked_at = NULL
WHERE 
    device_id IS NOT NULL;

-- Step 4: Verify the reset
SELECT 
    device_id,
    customer_name,
    customer_phone,
    total_amount,
    amount_paid,
    downpayment,
    payment_balance,
    daily_payment_amount,
    unlock_until,
    status,
    is_locked,
    lock_reason
FROM devices
ORDER BY created_at DESC;

COMMIT;

-- ============================================
-- VERIFICATION QUERIES
-- ============================================

-- Check all devices are locked and reset
SELECT 
    COUNT(*) as total_devices,
    COUNT(CASE WHEN is_locked = TRUE THEN 1 END) as locked_devices,
    COUNT(CASE WHEN payment_balance = 0 THEN 1 END) as zero_balance_devices,
    COUNT(CASE WHEN unlock_until IS NULL THEN 1 END) as no_unlock_period,
    AVG(daily_payment_amount) as avg_daily_payment
FROM devices;

-- Show summary of each device
SELECT 
    device_id,
    customer_name,
    customer_phone,
    CONCAT('KES ', total_amount) as loan_amount,
    CONCAT('KES ', amount_paid) as total_paid_to_loan,
    CONCAT('KES ', downpayment) as deposit,
    CONCAT('KES ', payment_balance) as active_payment_balance,
    CONCAT('KES ', daily_payment_amount) as daily_payment,
    CASE 
        WHEN is_locked THEN '🔒 LOCKED'
        ELSE '🔓 UNLOCKED'
    END as device_status,
    lock_reason
FROM devices
ORDER BY created_at DESC;

-- ============================================
-- NOTES FOR ADMIN
-- ============================================
-- 
-- After running this script:
-- 
-- 1. ALL DEVICES ARE LOCKED
--    - Customers must make payment to unlock
--    - Trial period has expired
-- 
-- 2. PAYMENT SYSTEM READY
--    - Full payment (e.g., KES 80) = 24 hours unlock
--    - Partial payment (e.g., KES 50) = 12 hours unlock
--    - Multi-day payment (e.g., KES 160) = 48 hours unlock
-- 
-- 3. LOAN TRACKING INTACT
--    - total_amount: Original loan amount
--    - amount_paid: Total paid toward loan
--    - downpayment: Initial deposit (not for unlock)
-- 
-- 4. DAILY PAYMENT AMOUNTS
--    - All devices default to KES 80 daily payment
--    - Admin can change per device in enrollment
-- 
-- 5. CUSTOMER ACTIONS REQUIRED
--    - Customers must make payment to unlock device
--    - Payment goes to payment_balance first
--    - When payment_balance used up, device locks again
--    - Payments also reduce total loan (amount_paid increases)
-- 
-- ============================================
