-- ============================================
-- COMPLETE PAYMENT SYSTEM FIX
-- ============================================
-- Run this ONCE to fix all payment system and device locking issues
-- ============================================

BEGIN;

-- 1. Add device locking columns
DO $$
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns WHERE table_name = 'devices' AND column_name = 'is_locked') THEN
        ALTER TABLE devices ADD COLUMN is_locked BOOLEAN DEFAULT TRUE;
        RAISE NOTICE '✓ Added is_locked column';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns WHERE table_name = 'devices' AND column_name = 'lock_reason') THEN
        ALTER TABLE devices ADD COLUMN lock_reason TEXT;
        RAISE NOTICE '✓ Added lock_reason column';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns WHERE table_name = 'devices' AND column_name = 'locked_by') THEN
        ALTER TABLE devices ADD COLUMN locked_by TEXT;
        RAISE NOTICE '✓ Added locked_by column';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns WHERE table_name = 'devices' AND column_name = 'locked_at') THEN
        ALTER TABLE devices ADD COLUMN locked_at TIMESTAMP WITH TIME ZONE;
        RAISE NOTICE '✓ Added locked_at column';
    END IF;
END $$;

-- 2. Add payment system columns
DO $$
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns WHERE table_name = 'devices' AND column_name = 'daily_payment_amount') THEN
        ALTER TABLE devices ADD COLUMN daily_payment_amount DECIMAL(10,2) DEFAULT 80.00;
        RAISE NOTICE '✓ Added daily_payment_amount column';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns WHERE table_name = 'devices' AND column_name = 'downpayment') THEN
        ALTER TABLE devices ADD COLUMN downpayment DECIMAL(10,2) DEFAULT 0;
        RAISE NOTICE '✓ Added downpayment column';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns WHERE table_name = 'devices' AND column_name = 'payment_balance') THEN
        ALTER TABLE devices ADD COLUMN payment_balance DECIMAL(10,2) DEFAULT 0;
        RAISE NOTICE '✓ Added payment_balance column';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns WHERE table_name = 'devices' AND column_name = 'last_payment_date') THEN
        ALTER TABLE devices ADD COLUMN last_payment_date TIMESTAMP WITH TIME ZONE;
        RAISE NOTICE '✓ Added last_payment_date column';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns WHERE table_name = 'devices' AND column_name = 'last_payment_amount') THEN
        ALTER TABLE devices ADD COLUMN last_payment_amount DECIMAL(10,2) DEFAULT 0;
        RAISE NOTICE '✓ Added last_payment_amount column';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns WHERE table_name = 'devices' AND column_name = 'unlocked_at') THEN
        ALTER TABLE devices ADD COLUMN unlocked_at TIMESTAMP WITH TIME ZONE;
        RAISE NOTICE '✓ Added unlocked_at column';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns WHERE table_name = 'devices' AND column_name = 'unlock_until') THEN
        ALTER TABLE devices ADD COLUMN unlock_until TIMESTAMP WITH TIME ZONE;
        RAISE NOTICE '✓ Added unlock_until column';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns WHERE table_name = 'devices' AND column_name = 'trial_period') THEN
        ALTER TABLE devices ADD COLUMN trial_period BOOLEAN DEFAULT FALSE;
        RAISE NOTICE '✓ Added trial_period column';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns WHERE table_name = 'devices' AND column_name = 'manual_unlock_requested') THEN
        ALTER TABLE devices ADD COLUMN manual_unlock_requested BOOLEAN DEFAULT FALSE;
        RAISE NOTICE '✓ Added manual_unlock_requested column';
    END IF;
END $$;

-- 3. Add admin token column
DO $$
BEGIN
    IF NOT EXISTS (SELECT column_name FROM information_schema.columns WHERE table_name = 'admins' AND column_name = 'token') THEN
        ALTER TABLE admins ADD COLUMN token TEXT;
        RAISE NOTICE '✓ Added token column to admins table';
    END IF;
END $$;

-- 4. Create payments table if it doesn't exist
CREATE TABLE IF NOT EXISTS payments (
    id SERIAL PRIMARY KEY,
    customer_phone TEXT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_date TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    payment_method TEXT DEFAULT 'mobile_money',
    reference TEXT,
    unlock_type TEXT DEFAULT 'none',
    unlock_hours DECIMAL(5,2) DEFAULT 0,
    device_unlocked BOOLEAN DEFAULT FALSE,
    status TEXT DEFAULT 'completed',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 5. Create indexes
CREATE INDEX IF NOT EXISTS idx_payments_customer_phone ON payments(customer_phone);
CREATE INDEX IF NOT EXISTS idx_payments_date ON payments(payment_date DESC);
CREATE INDEX IF NOT EXISTS idx_devices_unlock_until ON devices(unlock_until);
CREATE INDEX IF NOT EXISTS idx_devices_customer_phone ON devices(customer_phone);

-- 6. Set default values for existing devices
UPDATE devices 
SET daily_payment_amount = 80.00 
WHERE daily_payment_amount IS NULL OR daily_payment_amount = 0;

UPDATE devices 
SET is_locked = CASE 
    WHEN status = 'locked' THEN TRUE 
    WHEN status = 'active' THEN FALSE
    ELSE TRUE 
END
WHERE is_locked IS NULL;

UPDATE devices 
SET downpayment = amount_paid
WHERE downpayment IS NULL OR downpayment = 0;

UPDATE devices 
SET payment_balance = 0
WHERE payment_balance IS NULL;

-- 7. Show verification
SELECT 
    '📋 DEVICE COLUMNS VERIFICATION' as info,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'devices'
AND column_name IN (
    'is_locked', 'lock_reason', 'locked_by', 'locked_at',
    'daily_payment_amount', 'downpayment', 'payment_balance',
    'last_payment_date', 'last_payment_amount', 'unlocked_at',
    'unlock_until', 'trial_period', 'manual_unlock_requested'
)
ORDER BY column_name;

SELECT 
    '📊 CURRENT DEVICE STATUS' as info,
    device_id,
    customer_name,
    daily_payment_amount,
    downpayment,
    payment_balance,
    is_locked,
    status
FROM devices 
ORDER BY created_at DESC
LIMIT 5;

COMMIT;

-- Success message
SELECT '🎉 COMPLETE PAYMENT SYSTEM FIX APPLIED SUCCESSFULLY!' as message;