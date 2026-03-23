# Quick Setup Guide - Eden Device Financing

## Problem: No Users in Database

If you're getting "account not found" errors, it means the database is empty. Follow these steps:

## Step 1: Setup Database Tables

1. Go to Supabase Dashboard: https://supabase.com/dashboard
2. Select your project
3. Click "SQL Editor" in the left sidebar
4. Run the complete database setup:

```sql
-- Copy and paste the contents of COMPLETE_DATABASE_SETUP.sql
-- This creates all tables with proper structure
```

## Step 2: Add Test Data

1. Still in SQL Editor
2. Run the test data script: