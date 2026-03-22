-- Initialize Super Admin for Eden
-- Run this in Supabase SQL Editor

-- Step 1: Create admins table if it doesn't exist
CREATE TABLE IF NOT EXISTS admins (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50) DEFAULT 'admin',
    created_at TIMESTAMP DEFAULT NOW()
);

-- Step 2: Create admin sessions table if it doesn't exist
CREATE TABLE IF NOT EXISTS admin_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admin_id UUID REFERENCES admins(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Step 3: Insert super admin with email: sammyseth260@gmail.com and password: 58369234
INSERT INTO admins (email, password_hash, full_name, role)
VALUES (
    'sammyseth260@gmail.com',
    '5ba3e91e5a1c15b76194ca105cb345523bb7cdac33a708d4491484eb4a1c6b9f',
    'Super Administrator',
    'super_admin'
)
ON CONFLICT (email) DO NOTHING;

-- Step 4: Verify the super admin was created
SELECT id, email, full_name, role, created_at 
FROM admins 
WHERE email = 'sammyseth260@gmail.com';
