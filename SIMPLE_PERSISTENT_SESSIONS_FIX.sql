-- Simple fix for persistent sessions - handles existing tables gracefully

-- Add device_fingerprint column to devices table if it doesn't exist
ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_fingerprint TEXT;

-- Add index for device fingerprint if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_devices_fingerprint') THEN
        CREATE INDEX idx_devices_fingerprint ON devices(device_fingerprint);
    END IF;
END $$;

-- If persistent_sessions table already exists, just add missing columns
DO $$ 
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'persistent_sessions') THEN
        -- Table exists, check and add missing columns
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'persistent_sessions' AND column_name = 'device_fingerprint') THEN
            ALTER TABLE persistent_sessions ADD COLUMN device_fingerprint TEXT;
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'persistent_sessions' AND column_name = 'customer_phone') THEN
            ALTER TABLE persistent_sessions ADD COLUMN customer_phone TEXT;
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'persistent_sessions' AND column_name = 'device_id') THEN
            ALTER TABLE persistent_sessions ADD COLUMN device_id TEXT;
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'persistent_sessions' AND column_name = 'persistent_token') THEN
            ALTER TABLE persistent_sessions ADD COLUMN persistent_token TEXT UNIQUE;
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'persistent_sessions' AND column_name = 'expires_at') THEN
            ALTER TABLE persistent_sessions ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'persistent_sessions' AND column_name = 'is_active') THEN
            ALTER TABLE persistent_sessions ADD COLUMN is_active BOOLEAN DEFAULT true;
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'persistent_sessions' AND column_name = 'last_accessed') THEN
            ALTER TABLE persistent_sessions ADD COLUMN last_accessed TIMESTAMP WITH TIME ZONE DEFAULT NOW();
        END IF;
    ELSE
        -- Table doesn't exist, create it
        CREATE TABLE persistent_sessions (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            device_fingerprint TEXT NOT NULL,
            customer_phone TEXT NOT NULL,
            device_id TEXT,
            persistent_token TEXT UNIQUE NOT NULL,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
            last_accessed TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
            expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
            is_active BOOLEAN DEFAULT true
        );
    END IF;
END $$;

-- If admin_sessions table already exists, just add missing columns
DO $$ 
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'admin_sessions') THEN
        -- Table exists, check and add missing columns
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'admin_sessions' AND column_name = 'admin_id') THEN
            ALTER TABLE admin_sessions ADD COLUMN admin_id UUID REFERENCES admins(id);
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'admin_sessions' AND column_name = 'email') THEN
            ALTER TABLE admin_sessions ADD COLUMN email TEXT;
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'admin_sessions' AND column_name = 'browser_fingerprint') THEN
            ALTER TABLE admin_sessions ADD COLUMN browser_fingerprint TEXT;
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'admin_sessions' AND column_name = 'persistent_token') THEN
            ALTER TABLE admin_sessions ADD COLUMN persistent_token TEXT UNIQUE;
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'admin_sessions' AND column_name = 'expires_at') THEN
            ALTER TABLE admin_sessions ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'admin_sessions' AND column_name = 'is_active') THEN
            ALTER TABLE admin_sessions ADD COLUMN is_active BOOLEAN DEFAULT true;
        END IF;
        
        IF NOT EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'admin_sessions' AND column_name = 'last_accessed') THEN
            ALTER TABLE admin_sessions ADD COLUMN last_accessed TIMESTAMP WITH TIME ZONE DEFAULT NOW();
        END IF;
    ELSE
        -- Table doesn't exist, create it
        CREATE TABLE admin_sessions (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            admin_id UUID REFERENCES admins(id) NOT NULL,
            email TEXT NOT NULL,
            browser_fingerprint TEXT,
            persistent_token TEXT UNIQUE NOT NULL,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
            last_accessed TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
            expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
            is_active BOOLEAN DEFAULT true
        );
    END IF;
END $$;

-- Add indexes safely
DO $$ 
BEGIN
    -- Persistent sessions indexes
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_persistent_sessions_device_fingerprint') THEN
        CREATE INDEX idx_persistent_sessions_device_fingerprint ON persistent_sessions(device_fingerprint);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_persistent_sessions_token') THEN
        CREATE INDEX idx_persistent_sessions_token ON persistent_sessions(persistent_token);
    END IF;
    
    -- Admin sessions indexes
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_admin_sessions_admin_id') THEN
        CREATE INDEX idx_admin_sessions_admin_id ON admin_sessions(admin_id);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_admin_sessions_token') THEN
        CREATE INDEX idx_admin_sessions_token ON admin_sessions(persistent_token);
    END IF;
END $$;

-- Enable RLS and create policies safely
DO $$ 
BEGIN
    -- Enable RLS on persistent_sessions
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'persistent_sessions') THEN
        ALTER TABLE persistent_sessions ENABLE ROW LEVEL SECURITY;
        
        -- Drop existing policy if it exists
        DROP POLICY IF EXISTS "Allow all operations on persistent_sessions" ON persistent_sessions;
        
        -- Create new policy
        CREATE POLICY "Allow all operations on persistent_sessions" ON persistent_sessions FOR ALL USING (true);
    END IF;
    
    -- Enable RLS on admin_sessions
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'admin_sessions') THEN
        ALTER TABLE admin_sessions ENABLE ROW LEVEL SECURITY;
        
        -- Drop existing policy if it exists
        DROP POLICY IF EXISTS "Allow all operations on admin_sessions" ON admin_sessions;
        
        -- Create new policy
        CREATE POLICY "Allow all operations on admin_sessions" ON admin_sessions FOR ALL USING (true);
    END IF;
END $$;

-- Verification
SELECT 'Persistent sessions setup completed successfully!' as status;

-- Show table info
SELECT 
    t.table_name,
    COUNT(c.column_name) as column_count
FROM information_schema.tables t
LEFT JOIN information_schema.columns c ON t.table_name = c.table_name
WHERE t.table_name IN ('persistent_sessions', 'admin_sessions', 'devices')
AND t.table_schema = 'public'
GROUP BY t.table_name
ORDER BY t.table_name;