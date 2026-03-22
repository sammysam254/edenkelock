-- Initialize Super Admin for Eden
-- Run this in Supabase SQL Editor

-- Insert super admin with email: sammyseth260@gmail.com and password: 58369234
-- Password is hashed using SHA256

INSERT INTO admins (email, password_hash, full_name, role)
VALUES (
    'sammyseth260@gmail.com',
    '5ba3e91e5a1c15b76194ca105cb345523bb7cdac33a708d4491484eb4a1c6b9f',
    'Super Administrator',
    'super_admin'
)
ON CONFLICT (email) DO NOTHING;

-- Verify the super admin was created
SELECT id, email, full_name, role, created_at 
FROM admins 
WHERE email = 'sammyseth260@gmail.com';
