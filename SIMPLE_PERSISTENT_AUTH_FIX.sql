-- Simple fix for persistent authentication without foreign key constraints

-- Step 1: Clean up any invalid admin_sessions data
DELETE FROM admin_sessions WHERE admin_id IS NOT NULL AND admin_id NOT IN (SELECT id FROM admins);

-- Step 2: Add device_fingerprint to devices table if not exists
ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_fingerprint TEXT;

-- Step 3: Create persistent_sessions table if not exists
CREATE TABLE IF NOT EXISTS persistent_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_fingerprint TEXT,
    customer_phone TEXT,
    device_id TEXT,
    persistent_token TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_accessed TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true
);

-- Step 4: Create admin_sessions table if not exists (without foreign key)
CREATE TABLE IF NOT EXISTS admin_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID,
    email TEXT,
    browser_fingerprint TEXT,
    persistent_token TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_accessed TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true
);

-- Step 5: Add missing columns to existing tables
DO $$ 
BEGIN
    -- Add columns to persistent_sessions if they don't exist
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'persistent_sessions' AND column_name = 'device_fingerprint') THEN
        ALTER TABLE persistent_sessions ADD COLUMN device_fingerprint TEXT;
    END IF;
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'persistent_sessions' AND column_name = 'customer_phone') THEN
        ALTER TABLE persistent_sessions ADD COLUMN customer_phone TEXT;
    END IF;
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'persistent_sessions' AND column_name = 'device_id') THEN
        ALTER TABLE persistent_sessions ADD COLUMN device_id TEXT;
    END IF;
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'persistent_sessions' AND column_name = 'expires_at') THEN
        ALTER TABLE persistent_sessions ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;
    END IF;
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'persistent_sessions' AND column_name = 'is_active') THEN
        ALTER TABLE persistent_sessions ADD COLUMN is_active BOOLEAN DEFAULT true;
    END IF;
    
    -- Add columns to admin_sessions if they don't exist
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'admin_sessions' AND column_name = 'email') THEN
        ALTER TABLE admin_sessions ADD COLUMN email TEXT;
    END IF;
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'admin_sessions' AND column_name = 'browser_fingerprint') THEN
        ALTER TABLE admin_sessions ADD COLUMN browser_fingerprint TEXT;
    END IF;
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'admin_sessions' AND column_name = 'expires_at') THEN
        ALTER TABLE admin_sessions ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;
    END IF;
    IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'admin_sessions' AND column_name = 'is_active') THEN
        ALTER TABLE admin_sessions ADD COLUMN is_active BOOLEAN DEFAULT true;
    END IF;
END $$;

-- Step 6: Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_devices_fingerprint ON devices(device_fingerprint);
CREATE INDEX IF NOT EXISTS idx_persistent_sessions_device_fingerprint ON persistent_sessions(device_fingerprint);
CREATE INDEX IF NOT EXISTS idx_persistent_sessions_token ON persistent_sessions(persistent_token);
CREATE INDEX IF NOT EXISTS idx_admin_sessions_admin_id ON admin_sessions(admin_id);
CREATE INDEX IF NOT EXISTS idx_admin_sessions_token ON admin_sessions(persistent_token);

-- Step 7: Enable RLS and create policies
ALTER TABLE persistent_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE admin_sessions ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Allow all operations on persistent_sessions" ON persistent_sessions;
DROP POLICY IF EXISTS "Allow all operations on admin_sessions" ON admin_sessions;

-- Create new policies
CREATE POLICY "Allow all operations on persistent_sessions" ON persistent_sessions FOR ALL USING (true);
CREATE POLICY "Allow all operations on admin_sessions" ON admin_sessions FOR ALL USING (true);

-- Verification
SELECT 'Persistent authentication setup completed successfully!' as status;