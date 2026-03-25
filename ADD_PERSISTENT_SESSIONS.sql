-- Add persistent session tables for authentication that survives app updates and browser restarts

-- Check if persistent_sessions table exists, if not create it
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'persistent_sessions') THEN
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

-- Check if admin_sessions table exists, if not create it
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'admin_sessions') THEN
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

-- Add device_fingerprint column to devices table if it doesn't exist
ALTER TABLE devices ADD COLUMN IF NOT EXISTS device_fingerprint TEXT;

-- Add indexes for performance (only if they don't exist)
DO $$ 
BEGIN
    -- Indexes for persistent_sessions
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_persistent_sessions_device_fingerprint') THEN
        CREATE INDEX idx_persistent_sessions_device_fingerprint ON persistent_sessions(device_fingerprint);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_persistent_sessions_token') THEN
        CREATE INDEX idx_persistent_sessions_token ON persistent_sessions(persistent_token);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_persistent_sessions_active') THEN
        CREATE INDEX idx_persistent_sessions_active ON persistent_sessions(is_active);
    END IF;
    
    -- Indexes for admin_sessions
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_admin_sessions_admin_id') THEN
        CREATE INDEX idx_admin_sessions_admin_id ON admin_sessions(admin_id);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_admin_sessions_token') THEN
        CREATE INDEX idx_admin_sessions_token ON admin_sessions(persistent_token);
    END IF;
    
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_admin_sessions_active') THEN
        CREATE INDEX idx_admin_sessions_active ON admin_sessions(is_active);
    END IF;
    
    -- Index for devices fingerprint
    IF NOT EXISTS (SELECT FROM pg_indexes WHERE indexname = 'idx_devices_fingerprint') THEN
        CREATE INDEX idx_devices_fingerprint ON devices(device_fingerprint);
    END IF;
END $$;

-- Enable RLS on new tables (only if tables exist)
DO $$ 
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'persistent_sessions') THEN
        ALTER TABLE persistent_sessions ENABLE ROW LEVEL SECURITY;
    END IF;
    
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'admin_sessions') THEN
        ALTER TABLE admin_sessions ENABLE ROW LEVEL SECURITY;
    END IF;
END $$;

-- Create policies (drop existing ones first to avoid conflicts)
DO $$ 
BEGIN
    -- Drop existing policies if they exist
    DROP POLICY IF EXISTS "Allow all operations on persistent_sessions" ON persistent_sessions;
    DROP POLICY IF EXISTS "Allow all operations on admin_sessions" ON admin_sessions;
    
    -- Create new policies
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'persistent_sessions') THEN
        CREATE POLICY "Allow all operations on persistent_sessions" ON persistent_sessions FOR ALL USING (true);
    END IF;
    
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'admin_sessions') THEN
        CREATE POLICY "Allow all operations on admin_sessions" ON admin_sessions FOR ALL USING (true);
    END IF;
END $$;

-- Clean up expired sessions (optional - run periodically)
-- DELETE FROM persistent_sessions WHERE expires_at < NOW() OR is_active = false;
-- DELETE FROM admin_sessions WHERE expires_at < NOW() OR is_active = false;

-- Verify tables were created
SELECT 'Persistent session tables setup completed successfully' as status;

-- Show table counts
DO $$ 
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'persistent_sessions') THEN
        RAISE NOTICE 'persistent_sessions table exists with % rows', (SELECT COUNT(*) FROM persistent_sessions);
    END IF;
    
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'admin_sessions') THEN
        RAISE NOTICE 'admin_sessions table exists with % rows', (SELECT COUNT(*) FROM admin_sessions);
    END IF;
END $$;

-- Show table structure for verification
SELECT 
    table_name, 
    column_name, 
    data_type, 
    is_nullable
FROM information_schema.columns 
WHERE table_name IN ('persistent_sessions', 'admin_sessions')
ORDER BY table_name, ordinal_position;