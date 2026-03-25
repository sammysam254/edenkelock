-- ============================================
-- SIMPLE RESET FOR SELF-REGISTRATION
-- ============================================
-- This script safely resets existing devices for customer self-registration
-- Only uses columns that definitely exist in the devices table
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

-- Step 2: Reset devices that have PINs to pending registration
UPDATE devices 
SET 
    pin_hash = NULL,
    status = 'pending_registration',
    is_locked = TRUE,
    must_change_pin = FALSE,
    token = NULL,
    last_login = NULL
WHERE 
    pin_hash IS NOT NULL 
    AND customer_phone IS NOT NULL
    AND status != 'deleted';

-- Step 3: Show results after reset
SELECT 
    'AFTER RESET' as status,
    COUNT(*) as total_devices,
    COUNT(CASE WHEN pin_hash IS NOT NULL THEN 1 END) as devices_with_pins,
    COUNT(CASE WHEN status = 'pending_registration' THEN 1 END) as pending_registration,
    COUNT(CASE WHEN is_locked = TRUE THEN 1 END) as locked_devices
FROM devices 
WHERE customer_phone IS NOT NULL;

-- Step 4: List customers who need to re-register
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

-- Step 5: Verification - these should all return 0
SELECT 'VERIFICATION: Devices with PINs in pending status (should be 0)' as check_name, COUNT(*) as count
FROM devices WHERE status = 'pending_registration' AND pin_hash IS NOT NULL
UNION ALL
SELECT 'VERIFICATION: Unlocked devices in pending status (should be 0)' as check_name, COUNT(*) as count
FROM devices WHERE status = 'pending_registration' AND is_locked = FALSE
UNION ALL
SELECT 'VERIFICATION: Devices ready for registration' as check_name, COUNT(*) as count
FROM devices WHERE status = 'pending_registration' AND pin_hash IS NULL AND customer_phone IS NOT NULL;

-- Step 6: Customer communication data
SELECT 
    customer_name,
    customer_phone,
    device_id,
    CONCAT(
        'Dear ', COALESCE(customer_name, 'Customer'), ',\n\n',
        'Eden system upgraded! Please re-register:\n',
        '1. Download Eden app v1.9.1\n',
        '2. Enter phone: ', customer_phone, '\n',
        '3. Set your own 4-digit PIN\n',
        '4. Device activates immediately\n\n',
        'Loan unchanged: KES ', COALESCE(total_amount, 0) - COALESCE(amount_paid, 0), ' balance\n\n',
        'Complete within 7 days.\n',
        'Download: https://eden-mkopa.onrender.com\n\n',
        'Eden Support'
    ) as sms_message
FROM devices 
WHERE status = 'pending_registration'
AND customer_phone IS NOT NULL
ORDER BY customer_name;

COMMIT;

-- ============================================
-- SUMMARY
-- ============================================
/*
WHAT THIS SCRIPT DID:
✅ Cleared all PINs from existing devices
✅ Set status to 'pending_registration' 
✅ Locked all devices until registration
✅ Cleared authentication tokens
✅ Preserved all customer and loan data

NEXT STEPS:
1. Contact all customers listed above
2. Provide them with registration instructions
3. Monitor registration progress in admin panel
4. Deploy Eden app v1.9.1

CUSTOMER INSTRUCTIONS:
- Download Eden app v1.9.1
- Enter their registered phone number
- Set their own secure 4-digit PIN
- Device will activate immediately
*/