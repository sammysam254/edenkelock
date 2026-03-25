-- FINAL PERSISTENT AUTHENTICATION FIX
-- This script safely adds persistent authentication without errors

-- Step 1: Add device_fingerprint to devices table
ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_fingerprint TEXT;

-- Step 2: Create persistent_sessions table if it doesn't exist
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

-- Step 3: Create admin_sessions table if it doesn't exist
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

-- Step 4: Add foreign key constraint to admin_sessions if it doesn't exist
DO $$ 
BEGIN
    -- Check if foreign key constraint exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'admin_sessions_admin_id_fkey' 
        AND table_name = 'admin_sessions'
    ) THEN
        -- Add foreign key constraint
        ALTER TABLE admin_sessions ADD CONSTRAINT admin_sessions_admin_id_fkey 
        FOREIGN KEY (admin_id) REFERENCES admins(id);
    END IF;
END $$;

-- Step 5: Add unique constraints if they don't exist
DO $$ 
BEGIN
    -- Add unique constraint to persistent_sessions.persistent_token
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'persistent_sessions_persistent_token_key' 
        AND table_name = 'persistent_sessions'
    ) THEN
        ALTER TABLE persistent_sessions ADD CONSTRAINT persistent_sessions_persistent_token_key 
        UNIQUE (persistent_token);
    END IF;
    
    -- Add unique constraint to admin_sessions.persistent_token
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'admin_sessions_persistent_token_key' 
        AND table_name = 'admin_sessions'
    ) THEN
        ALTER TABLE admin_sessions ADD CONSTRAINT admin_sessions_persistent_token_key 
        UNIQUE (persistent_token);
    END IF;
END $$;

-- Step 6: Create indexes safely
CREATE INDEX IF NOT EXISTS idx_devices_fingerprint ON devices(device_fingerprint);
CREATE INDEX IF NOT EXISTS idx_persistent_sessions_device_fingerprint ON persistent_sessions(device_fingerprint);
CREATE INDEX IF NOT EXISTS idx_persistent_sessions_token ON persistent_sessions(persistent_token);
CREATE INDEX IF NOT EXISTS idx_persistent_sessions_active ON persistent_sessions(is_active);
CREATE INDEX IF NOT EXISTS idx_admin_sessions_admin_id ON admin_sessions(admin_id);
CREATE INDEX IF NOT EXISTS idx_admin_sessions_token ON admin_sessions(persistent_token);
CREATE INDEX IF NOT EXISTS idx_admin_sessions_active ON admin_sessions(is_active);

-- Step 7: Enable RLS and create policies
ALTER TABLE persistent_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE admin_sessions ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if they exist (to avoid conflicts)
DROP POLICY IF EXISTS "Allow all operations on persistent_sessions" ON persistent_sessions;
DROP POLICY IF EXISTS "Allow all operations on admin_sessions" ON admin_sessions;

-- Create new policies
CREATE POLICY "Allow all operations on persistent_sessions" ON persistent_sessions FOR ALL USING (true);
CREATE POLICY "Allow all operations on admin_sessions" ON admin_sessions FOR ALL USING (true);

-- Step 8: Verification
SELECT 'Persistent authentication setup completed successfully!' as status;

-- Show table structure
SELECT 
    table_name,
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns 
WHERE table_name IN ('persistent_sessions', 'admin_sessions')
AND table_schema = 'public'
ORDER BY table_name, ordinal_position;

-- Show table counts
SELECT 
    'persistent_sessions' as table_name, 
    COUNT(*) as row_count 
FROM persistent_sessions
UNION ALL
SELECT 
    'admin_sessions' as table_name, 
    COUNT(*) as row_count 
FROM admin_sessions
UNION ALL
SELECT 
    'devices (with fingerprint)' as table_name, 
    COUNT(*) as row_count 
FROM devices 
WHERE device_fingerprint IS NOT NULL;