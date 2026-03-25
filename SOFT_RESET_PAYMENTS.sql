-- ============================================
-- SOFT RESET - PAYMENT BALANCE ONLY
-- ============================================
-- This is a gentler reset that only clears active payment balance
-- but keeps all other data intact (loan payments, history, etc.)
-- 
-- WHAT THIS DOES:
-- 1. Resets payment_balance to 0 (clears active unlock days)
-- 2. Clears unlock_until (removes active unlock period)
-- 3. Keeps ALL other data intact:
--    - Downpayment preserved
--    - Amount paid to loan preserved
--    - Total loan amount preserved
--    - Customer info preserved
--    - Device history preserved
-- 4. Locks devices that have no payment balance
-- 5. Sets daily_payment_amount if missing
--
-- USE THIS IF: You want to reset usage but keep payment history
-- RUN THIS IN SUPABASE SQL EDITOR
-- ============================================

BEGIN;

-- Step 1: Reset only payment balance and unlock period
UPDATE devices
SET 
    payment_balance = 0,                    -- Clear active payment balance
    unlock_until = NULL                     -- Clear unlock period
WHERE 
    device_id IS NOT NULL;

-- Step 2: Lock devices that have zero payment balance
UPDATE devices
SET 
    status = 'locked',
    is_locked = TRUE,
    lock_reason = 'PAYMENT_REQUIRED'
WHERE 
    payment_balance = 0 
    OR unlock_until IS NULL 
    OR unlock_until < NOW();

-- Step 3: Ensure daily_payment_amount is set (default 80 KES)
UPDATE devices
SET 
    daily_payment_amount = 80
WHERE 
    daily_payment_amount IS NULL 
    OR daily_payment_amount = 0;

-- Step 4: Verify the changes
SELECT 
    device_id,
    customer_name,
    customer_phone,
    CONCAT('KES ', total_amount) as loan_amount,
    CONCAT('KES ', amount_paid) as paid_to_loan,
    CONCAT('KES ', downpayment) as deposit,
    CONCAT('KES ', payment_balance) as active_balance,
    CONCAT('KES ', daily_payment_amount) as daily_payment,
    CASE 
        WHEN is_locked THEN '🔒 LOCKED - Payment Required'
        ELSE '🔓 UNLOCKED'
    END as status,
    unlock_until
FROM devices
ORDER BY created_at DESC;

COMMIT;

-- ============================================
-- SUMMARY
-- ============================================
SELECT 
    COUNT(*) as total_devices,
    COUNT(CASE WHEN is_locked = TRUE THEN 1 END) as locked_devices,
    COUNT(CASE WHEN payment_balance = 0 THEN 1 END) as zero_balance,
    SUM(amount_paid) as total_loan_payments_preserved,
    SUM(downpayment) as total_deposits_preserved
FROM devices;
