# Eden Authentication Setup

## Step 1: Create Admin Tables in Supabase

Go to your Supabase SQL Editor and run:

```sql
-- Admins table
CREATE TABLE IF NOT EXISTS admins (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50) DEFAULT 'admin',
    created_at TIMESTAMP DEFAULT NOW()
);

-- Admin sessions table
CREATE TABLE IF NOT EXISTS admin_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admin_id UUID REFERENCES admins(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
```

## Step 2: Initialize Super Admin

After deployment, make a POST request to initialize the super admin:

```bash
curl -X POST https://eden-mkopa.onrender.com/api/auth/init-super-admin
```

## Step 3: Login as Super Admin

1. Go to: https://eden-mkopa.onrender.com/login
2. Email: sammyseth260@gmail.com
3. Password: 58369234

## Features

### Super Admin Can:
- Create administrator accounts
- Delete administrators
- Enroll new devices
- View all devices and stats
- Lock/unlock devices

### Administrators Can:
- View devices
- Lock/unlock devices
- View payment history
- Manage customers

## API Endpoints

### Authentication
- POST `/api/auth/login` - Login
- POST `/api/auth/create-admin` - Create admin (super admin only)
- GET `/api/auth/admins` - List admins (super admin only)
- DELETE `/api/auth/admins/:id` - Delete admin (super admin only)
- POST `/api/auth/init-super-admin` - Initialize super admin (one-time)

### Devices
- POST `/api/devices/enroll` - Enroll device (super admin only)
- GET `/api/devices` - List all devices
- GET `/api/devices/:id` - Get device details
- POST `/api/devices/:id/lock` - Lock device
- POST `/api/devices/:id/unlock` - Unlock device
- GET `/api/devices/:id/status` - Check device status

### Payments
- POST `/api/payments` - Record payment
- GET `/api/payments/:device_id` - Get payment history
