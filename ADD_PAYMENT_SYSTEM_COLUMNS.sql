-- ============================================
-- ADD PAYMENT SYSTEM COLUMNS
-- ============================================
-- Add columns needed for the payment system with auto-unlock
-- Includes daily payment logic with multi-day unlock capability
-- ============================================

BEGIN;

-- Add payment-related columns to devices table
DO $$
BEGIN
    -- Add daily_payment_amount column
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'daily_payment_amount'
    ) THEN
        ALTER TABLE devices ADD COLUMN daily_payment_amount DECIMAL(10,2) DEFAULT 80.00;
        RAISE NOTICE '✓ Added daily_payment_amount column';
    ELSE
        RAISE NOTICE '✓ daily_payment_amount column already exists';
    END IF;
END $$;

DO $$
BEGIN
    -- Add last_payment_date column
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'last_payment_date'
    ) THEN
        ALTER TABLE devices ADD COLUMN last_payment_date TIMESTAMP WITH TIME ZONE;
        RAISE NOTICE '✓ Added last_payment_date column';
    ELSE
        RAISE NOTICE '✓ last_payment_date column already exists';
    END IF;
END $$;

DO $$
BEGIN
    -- Add last_payment_amount column
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'last_payment_amount'
    ) THEN
        ALTER TABLE devices ADD COLUMN last_payment_amount DECIMAL(10,2) DEFAULT 0;
        RAISE NOTICE '✓ Added last_payment_amount column';
    ELSE
        RAISE NOTICE '✓ last_payment_amount column already exists';
    END IF;
END $$;

DO $$
BEGIN
    -- Add unlocked_at column
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'unlocked_at'
    ) THEN
        ALTER TABLE devices ADD COLUMN unlocked_at TIMESTAMP WITH TIME ZONE;
        RAISE NOTICE '✓ Added unlocked_at column';
    ELSE
        RAISE NOTICE '✓ unlocked_at column already exists';
    END IF;
END $$;

DO $$
BEGIN
    -- Add manual_unlock_requested column
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'manual_unlock_requested'
    ) THEN
        ALTER TABLE devices ADD COLUMN manual_unlock_requested BOOLEAN DEFAULT FALSE;
        RAISE NOTICE '✓ Added manual_unlock_requested column';
    ELSE
        RAISE NOTICE '✓ manual_unlock_requested column already exists';
    END IF;
END $$;

DO $$
BEGIN
    -- Add payment_balance column (tracks prepaid days)
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'payment_balance'
    ) THEN
        ALTER TABLE devices ADD COLUMN payment_balance DECIMAL(10,2) DEFAULT 0;
        RAISE NOTICE '✓ Added payment_balance column';
    ELSE
        RAISE NOTICE '✓ payment_balance column already exists';
    END IF;
END $$;

DO $$
BEGIN
    -- Add unlock_until column (tracks when device should lock again)
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'unlock_until'
    ) THEN
        ALTER TABLE devices ADD COLUMN unlock_until TIMESTAMP WITH TIME ZONE;
        RAISE NOTICE '✓ Added unlock_until column';
    ELSE
        RAISE NOTICE '✓ unlock_until column already exists';
    END IF;
END $$;

DO $$
BEGIN
    -- Add trial_period column
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'trial_period'
    ) THEN
        ALTER TABLE devices ADD COLUMN trial_period BOOLEAN DEFAULT FALSE;
        RAISE NOTICE '✓ Added trial_period column';
    ELSE
        RAISE NOTICE '✓ trial_period column already exists';
    END IF;
END $$;

DO $$
BEGIN
    -- Add downpayment column (separate from payment_balance)
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'devices' 
        AND column_name = 'downpayment'
    ) THEN
        ALTER TABLE devices ADD COLUMN downpayment DECIMAL(10,2) DEFAULT 0;
        RAISE NOTICE '✓ Added downpayment column';
    ELSE
        RAISE NOTICE '✓ downpayment column already exists';
    END IF;
END $$;

-- Update payments table to include new unlock tracking columns
DO $$
BEGIN
    -- Add unlock_type column to payments table
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'payments' 
        AND column_name = 'unlock_type'
    ) THEN
        ALTER TABLE payments ADD COLUMN unlock_type TEXT DEFAULT 'none';
        RAISE NOTICE '✓ Added unlock_type column to payments table';
    ELSE
        RAISE NOTICE '✓ unlock_type column already exists in payments table';
    END IF;
END $$;

DO $$
BEGIN
    -- Add unlock_hours column to payments table
    IF NOT EXISTS (
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'payments' 
        AND column_name = 'unlock_hours'
    ) THEN
        ALTER TABLE payments ADD COLUMN unlock_hours DECIMAL(5,2) DEFAULT 0;
        RAISE NOTICE '✓ Added unlock_hours column to payments table';
    ELSE
        RAISE NOTICE '✓ unlock_hours column already exists in payments table';
    END IF;
END $$;

-- Create payments table if it doesn't exist
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

-- Create indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_payments_customer_phone ON payments(customer_phone);
CREATE INDEX IF NOT EXISTS idx_payments_date ON payments(payment_date DESC);
CREATE INDEX IF NOT EXISTS idx_devices_unlock_until ON devices(unlock_until);

-- Set default daily payment amount for existing devices (KES 80)
UPDATE devices 
SET daily_payment_amount = 80.00 
WHERE daily_payment_amount IS NULL OR daily_payment_amount = 0;

-- Initialize payment_balance for existing devices based on current amount_paid
UPDATE devices 
SET payment_balance = CASE 
    WHEN daily_payment_amount > 0 THEN 
        FLOOR((amount_paid / daily_payment_amount)) * daily_payment_amount
    ELSE 0 
END
WHERE payment_balance IS NULL OR payment_balance = 0;

-- Set unlock_until for devices with sufficient payment balance
UPDATE devices 
SET unlock_until = CASE 
    WHEN payment_balance >= daily_payment_amount THEN 
        NOW() + INTERVAL '1 day' * FLOOR(payment_balance / daily_payment_amount)
    ELSE NULL 
END,
is_locked = CASE 
    WHEN payment_balance >= daily_payment_amount THEN FALSE 
    ELSE TRUE 
END,
status = CASE 
    WHEN payment_balance >= daily_payment_amount THEN 'active' 
    ELSE 'locked' 
END
WHERE unlock_until IS NULL;

-- Show verification of payment columns
SELECT 
    '📋 PAYMENT SYSTEM COLUMNS VERIFICATION' as info,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'devices'
AND column_name IN ('daily_payment_amount', 'last_payment_date', 'last_payment_amount', 'unlocked_at', 'manual_unlock_requested', 'payment_balance', 'unlock_until')
ORDER BY column_name;

-- Show payments table structure
SELECT 
    '📋 PAYMENTS TABLE VERIFICATION' as info,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'payments'
ORDER BY ordinal_position;

-- Show current device payment status
SELECT 
    '📊 CURRENT DEVICE PAYMENT STATUS' as info,
    device_id,
    customer_name,
    total_amount,
    amount_paid,
    daily_payment_amount,
    payment_balance,
    unlock_until,
    is_locked,
    status
FROM devices 
ORDER BY device_id;

COMMIT;