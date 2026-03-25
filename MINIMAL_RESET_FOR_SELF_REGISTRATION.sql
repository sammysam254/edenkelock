-- ============================================
-- MINIMAL RESET FOR SELF-REGISTRATION
-- ============================================
-- This script safely resets existing devices using only core columns
-- that definitely exist in your devices table
-- ============================================

-- Step 1: Show current state before changes
SELECT 
    'BEFORE RESET' as status,
    COUNT(*) as total_devices,
    COUNT(CASE WHEN pin_hash IS NOT NULL THEN 1 END) as devices_with_pins,
    COUNT(CASE WHEN status = 'active' THEN 1 END) as active_devices,
    COUNT(CASE WHEN is_locked = TRUE THEN 1 END) as locked_devices
FROM devices 
WHERE customer_phone IS NOT NULL;

-- Step 2: Reset devices - using only essential columns
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

-- Step 3: Clear tokens if column exists (safe approach)
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

-- Step 4: Show results after reset
SELECT 
    'AFTER RESET' as status,
    COUNT(*) as total_devices,
    COUNT(CASE WHEN pin_hash IS NOT NULL THEN 1 END) as devices_with_pins,
    COUNT(CASE WHEN status = 'pending_registration' THEN 1 END) as pending_registration,
    COUNT(CASE WHEN is_locked = TRUE THEN 1 END) as locked_devices
FROM devices 
WHERE customer_phone IS NOT NULL;

-- Step 5: List customers who need to re-register
SELECT 
    'CUSTOMERS TO CONTACT' as info,
    customer_name,
    customer_phone,
    device_id,
    COALESCE(total_amount, 0) as total_amount,
    COALESCE(amount_paid, 0) as amount_paid,
    COALESCE(total_amount, 0) - COALESCE(amount_paid, 0) as loan_balance
FROM devices 
WHERE status = 'pending_registration'
AND customer_phone IS NOT NULL
ORDER BY customer_name;

-- Step 6: Verification checks
SELECT 'VERIFICATION: Devices with PINs in pending status (should be 0)' as check_name, COUNT(*) as count
FROM devices WHERE status = 'pending_registration' AND pin_hash IS NOT NULL
UNION ALL
SELECT 'VERIFICATION: Unlocked devices in pending status (should be 0)' as check_name, COUNT(*) as count
FROM devices WHERE status = 'pending_registration' AND is_locked = FALSE
UNION ALL
SELECT 'VERIFICATION: Devices ready for registration' as check_name, COUNT(*) as count
FROM devices WHERE status = 'pending_registration' AND pin_hash IS NULL AND customer_phone IS NOT NULL;

-- Step 7: Generate customer SMS messages
SELECT 
    customer_name,
    customer_phone,
    device_id,
    CONCAT(
        'Dear ', COALESCE(customer_name, 'Customer'), ', Eden upgraded! Re-register: 1) Download Eden v1.9.1 2) Enter phone: ', 
        customer_phone, ' 3) Set 4-digit PIN 4) Activate device. Balance: KES ', 
        COALESCE(total_amount, 0) - COALESCE(amount_paid, 0), '. Complete in 7 days. Download: https://eden-mkopa.onrender.com'
    ) as sms_message
FROM devices 
WHERE status = 'pending_registration'
AND customer_phone IS NOT NULL
ORDER BY customer_name;

COMMIT;

-- ============================================
-- SUMMARY REPORT
-- ============================================
SELECT 
    'MIGRATION COMPLETE' as status,
    COUNT(*) as devices_reset,
    COUNT(DISTINCT customer_phone) as customers_affected,
    SUM(COALESCE(total_amount, 0) - COALESCE(amount_paid, 0)) as total_outstanding_balance
FROM devices 
WHERE status = 'pending_registration'
AND customer_phone IS NOT NULL;

-- ============================================
-- NEXT STEPS
-- ============================================
/*
✅ MIGRATION COMPLETED SUCCESSFULLY

IMMEDIATE ACTIONS:
1. Contact all customers listed above with SMS messages
2. Deploy Eden app v1.9.1 
3. Monitor registration progress in admin dashboard
4. Provide customer support for registration issues

CUSTOMER INSTRUCTIONS:
- Download Eden app v1.9.1
- Enter registered phone number
- Set own secure 4-digit PIN  
- Device activates immediately

ADMIN CHANGES:
- No PIN field in enrollment forms
- Customers now control their own PINs
- Faster, more secure enrollment process

SUCCESS METRICS TO TRACK:
- Registration completion rate (target: 95% in 7 days)
- Customer support tickets (should decrease)
- Login success rate (should increase)
- Customer satisfaction (should improve)
*/