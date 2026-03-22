-- Fix RLS Policies for Eden
-- Run this in Supabase SQL Editor

-- ============================================
-- DISABLE RLS ON ALL TABLES
-- ============================================
-- We're disabling RLS because the app uses service role key
-- and handles authentication in the application layer

ALTER TABLE IF EXISTS admins DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS admin_sessions DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS devices DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS payment_transactions DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS customer_accounts DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS customer_sessions DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS super_admins DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS administrators DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS customers DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS device_models DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS activity_logs DISABLE ROW LEVEL SECURITY;

-- ============================================
-- DROP ALL EXISTING POLICIES
-- ============================================
DO $$ 
DECLARE
    r RECORD;
BEGIN
    FOR r IN (SELECT schemaname, tablename, policyname 
              FROM pg_policies 
              WHERE schemaname = 'public') 
    LOOP
        EXECUTE 'DROP POLICY IF EXISTS ' || quote_ident(r.policyname) || 
                ' ON ' || quote_ident(r.schemaname) || '.' || quote_ident(r.tablename);
    END LOOP;
END $$;

-- ============================================
-- VERIFY RLS IS DISABLED
-- ============================================
SELECT 
    tablename,
    CASE 
        WHEN rowsecurity THEN 'ENABLED' 
        ELSE 'DISABLED' 
    END as rls_status
FROM pg_tables t
JOIN pg_class c ON t.tablename = c.relname
WHERE schemaname = 'public'
AND tablename IN ('admins', 'admin_sessions', 'devices', 'payment_transactions', 
                  'customer_accounts', 'customer_sessions', 'super_admins', 
                  'administrators', 'customers')
ORDER BY tablename;
