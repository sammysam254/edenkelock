-- ============================================
-- RESET EXISTING DEVICES FOR SELF-REGISTRATION
-- ============================================
-- This script resets all existing enrolled devices to allow customers
-- to register themselves with their own PINs using the new flow.
-- 
-- WHAT THIS DOES:
-- 1. Clears all existing PINs from enrolled devices
-- 2. Sets status to "pending_registration" 
-- 3. Locks devices until customer completes registration
-- 4. Preserves all customer and device information
-- 5. Allows customers to set their own PINs during registration
--
-- CUSTOMER INSTRUCTIONS AFTER RUNNING THIS:
-- Tell all existing customers to:
-- 1. Download the latest Eden app (v1.9.1)
-- 2. Enter their registered phone number
-- 3. Set their own 4-digit PIN
-- 4. Device will be activated after registration
-- ============================================

-- Step 1: Reset all existing devices to pending registration status
UPDATE devices 
SET 
    pin_hash = NULL,                    -- Clear existing PIN
    status = 'pending_registration',    -- Require registration
    is_locked = TRUE,                   -- Lock until registration
    must_change_pin = FALSE,            -- Customer sets own PIN
    token = NULL,                       -- Clear auth tokens
    last_login = NULL                   -- Clear login history
WHERE 
    pin_hash IS NOT NULL                -- Only update devices that had PINs
    AND status != 'deleted'             -- Don't touch deleted devices
    AND customer_phone IS NOT NULL;     -- Only devices with phone numbers

-- Step 2: Clear any persistent sessions for these devices (if table exists)
-- (This ensures customers must register again)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'persistent_sessions') THEN
        UPDATE persistent_sessions 
        SET is_active = FALSE 
        WHERE device_id IN (
            SELECT device_id 
            FROM devices 
            WHERE status = 'pending_registration'
        );
    END IF;
END $$;

-- Step 3: Add a note to device logs about the reset (if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'device_logs') THEN
        INSERT INTO device_logs (device_id, action, old_values, new_values)
        SELECT 
            device_id,
            'RESET_FOR_SELF_REGISTRATION',
            '{"old_status": "active", "had_pin": "true"}',
            '{"new_status": "pending_registration", "pin_cleared": "true", "reason": "Migration to self-registration system"}'
        FROM devices 
        WHERE status = 'pending_registration'
        AND customer_phone IS NOT NULL;
    END IF;
END $$;

-- Step 4: Show summary of affected devices
SELECT 
    COUNT(*) as total_devices_reset,
    COUNT(DISTINCT customer_phone) as unique_customers_affected,
    MIN(created_at) as oldest_device,
    MAX(created_at) as newest_device
FROM devices 
WHERE status = 'pending_registration'
AND customer_phone IS NOT NULL;

-- Step 5: List all customers who need to re-register
SELECT 
    customer_name,
    customer_phone,
    device_id,
    total_amount,
    amount_paid,
    (total_amount - amount_paid) as loan_balance
FROM devices 
WHERE status = 'pending_registration'
AND customer_phone IS NOT NULL
ORDER BY customer_name;

-- ============================================
-- VERIFICATION QUERIES
-- ============================================

-- Verify no devices have PINs in pending_registration status
SELECT 
    'VERIFICATION: Devices with PINs in pending status (should be 0)' as check_name,
    COUNT(*) as count
FROM devices 
WHERE status = 'pending_registration' 
AND pin_hash IS NOT NULL;

-- Verify all pending devices are locked
SELECT 
    'VERIFICATION: Unlocked devices in pending status (should be 0)' as check_name,
    COUNT(*) as count
FROM devices 
WHERE status = 'pending_registration' 
AND is_locked = FALSE;

-- Show devices ready for customer registration
SELECT 
    'VERIFICATION: Devices ready for customer registration' as check_name,
    COUNT(*) as count
FROM devices 
WHERE status = 'pending_registration' 
AND pin_hash IS NULL 
AND is_locked = TRUE 
AND customer_phone IS NOT NULL;

-- ============================================
-- CUSTOMER COMMUNICATION TEMPLATE
-- ============================================

-- Use this information to contact customers:
SELECT 
    CONCAT(
        'Dear ', customer_name, ',\n\n',
        'We have upgraded our Eden system to improve security and user experience.\n\n',
        'IMPORTANT: You need to re-register in the Eden app:\n',
        '1. Download the latest Eden app (v1.9.1)\n',
        '2. Enter your phone number: ', customer_phone, '\n',
        '3. Set your own secure 4-digit PIN\n',
        '4. Your device will be activated immediately\n\n',
        'Your loan information remains unchanged:\n',
        '- Total Amount: KES ', CAST(total_amount as TEXT), '\n',
        '- Amount Paid: KES ', CAST(amount_paid as TEXT), '\n',
        '- Balance: KES ', CAST((total_amount - amount_paid) as TEXT), '\n\n',
        'Please complete registration within 7 days.\n\n',
        'Thank you,\nEden Support Team'
    ) as customer_message
FROM devices 
WHERE status = 'pending_registration'
AND customer_phone IS NOT NULL
ORDER BY customer_name;

-- ============================================
-- ADMIN DASHBOARD UPDATE
-- ============================================

-- Update admin dashboard to show pending registrations
SELECT 
    'ADMIN SUMMARY' as report_type,
    COUNT(*) as devices_awaiting_registration,
    SUM(total_amount - amount_paid) as total_outstanding_balance,
    COUNT(DISTINCT customer_phone) as customers_to_contact
FROM devices 
WHERE status = 'pending_registration'
AND customer_phone IS NOT NULL;

COMMIT;

-- ============================================
-- POST-EXECUTION NOTES
-- ============================================
/*
AFTER RUNNING THIS SCRIPT:

1. CUSTOMER COMMUNICATION:
   - Send SMS/call to all affected customers
   - Explain the new registration process
   - Provide download link for Eden app v1.9.1
   - Set deadline for registration (e.g., 7 days)

2. ADMIN ACTIONS:
   - Monitor registration progress in admin dashboard
   - Follow up with customers who don't register
   - Provide support for registration issues

3. TECHNICAL VERIFICATION:
   - Test the registration flow with a sample customer
   - Verify devices unlock after successful registration
   - Check that loan balances are preserved

4. ROLLBACK PLAN (if needed):
   - Keep backup of original pin_hash values
   - Can restore specific customers if needed
   - Monitor for any issues in first 24 hours

5. SUCCESS METRICS:
   - Track registration completion rate
   - Monitor customer satisfaction
   - Verify security improvements
*/