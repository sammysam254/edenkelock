-- Clean up persistent authentication tables and add proper structure

-- Step 1: Clean up existing admin_sessions data that has invalid foreign keys
DELETE FROM admin_sessions 
WHERE admin_id NOT IN (SELECT id FROM admins);

-- Step 2: Add device_fingerprint to devices table
ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_fingerprint TEXT;

-- Step 3: Add missing columns to existing tables (if they exist)
-- For persistent_sessions
DO $$ 
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'persistent_sessions') THEN
        -- Add missing columns if they don't exist
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
    ELSE
        -- Create table if it doesn't exist
        CREATE TABLE persistent_sessions (
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
    END IF;
END $$;

-- For admin_sessions
DO $$ 
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'admin_sessions') THEN
        -- Add missing columns if they don't exist
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
    ELSE
        -- Create table if it doesn't exist
        CREATE TABLE admin_sessions (
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
    END IF;
END $$;

-- Step 4: Add foreign key constraint safely (after cleaning data)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'admin_sessions_admin_id_fkey' 
        AND table_name = 'admin_sessions'
    ) THEN
        ALTER TABLE admin_sessions ADD CONSTRAINT admin_sessions_admin_id_fkey 
        FOREIGN KEY (admin_id) REFERENCES admins(id);
    END IF;
END $$;

-- Step 5: Create indexes
CREATE INDEX IF NOT EXISTS idx_devices_fingerprint ON devices(device_fingerprint);
CREATE INDEX IF NOT EXISTS idx_persistent_sessions_device_fingerprint ON persistent_sessions(device_fingerprint);
CREATE INDEX IF NOT EXISTS idx_persistent_sessions_token ON persistent_sessions(persistent_token);
CREATE INDEX IF NOT EXISTS idx_admin_sessions_admin_id ON admin_sessions(admin_id);
CREATE INDEX IF NOT EXISTS idx_admin_sessions_token ON admin_sessions(persistent_token);

-- Step 6: Enable RLS and create policies
ALTER TABLE persistent_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE admin_sessions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Allow all operations on persistent_sessions" ON persistent_sessions;
DROP POLICY IF EXISTS "Allow all operations on admin_sessions" ON admin_sessions;

CREATE POLICY "Allow all operations on persistent_sessions" ON persistent_sessions FOR ALL USING (true);
CREATE POLICY "Allow all operations on admin_sessions" ON admin_sessions FOR ALL USING (true);

-- Verification
SELECT 'Persistent authentication cleaned and setup successfully!' as status;