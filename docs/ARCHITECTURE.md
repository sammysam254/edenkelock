# System Architecture

## Overview

Eden M-Kopa is a device financing system that locks Android devices until payments are made. It consists of four main components working together.

## Components

### 1. Android DPC App
- Device Policy Controller with Device Owner privileges
- Enforces kiosk mode and device restrictions
- Syncs with backend every 15 minutes
- Locks/unlocks device based on payment status
- Prevents factory reset, safe boot, and ADB access

### 2. Web Dashboard (Next.js)
- Multi-tenant admin interface
- Role-based access control (Super Admin, Administrators)
- Device enrollment and management
- Payment processing
- Analytics and reporting
- QR code generation for provisioning

### 3. Python Backend (Render)
- Web3 blockchain listener
- Monitors crypto payments to device wallet addresses
- Automatically updates payment records in Supabase
- Triggers device unlock when payment received

### 4. Supabase Database
- PostgreSQL with Row Level Security
- Stores all system data
- Provides REST API for all components
- Real-time subscriptions for live updates

## Data Flow

### Device Enrollment
1. Administrator creates customer in dashboard
2. Dashboard generates device record with unique code
3. Dashboard generates QR code with provisioning data
4. Device scans QR during factory reset setup
5. Device downloads and installs DPC app
6. Device becomes locked until first payment

### Payment Processing

#### M-Pesa Payment
1. Customer makes M-Pesa payment
2. Administrator enters payment in dashboard
3. Dashboard creates payment transaction
4. Database trigger updates device balance
5. If balance paid, device is unlocked
6. Device syncs and removes lock screen

#### Crypto Payment
1. Customer sends tokens to device wallet address
2. Backend listener detects blockchain transaction
3. Backend creates payment record in Supabase
4. Database trigger updates device balance
5. Device syncs and unlocks automatically

### Device Sync
1. Device wakes up every 15 minutes (WorkManager)
2. Queries Supabase for lock status
3. Updates local lock state
4. Shows/hides lock screen accordingly
5. Reports last sync timestamp

## Security Model

### Database (RLS Policies)
- Super Admins: Full access to all data
- Administrators: Access only to their enrolled devices/customers
- Devices: Read-only access to own record
- Service Role: Backend can write payments

### Android Device
- Device Owner mode (highest privilege)
- Factory reset protection enabled
- Safe boot disabled
- ADB/Developer options blocked
- Kiosk mode (lock task)
- User restrictions enforced

### API Authentication
- Supabase JWT tokens
- Row Level Security enforcement
- Service role key for backend only
- Anon key for dashboard/devices

## Technology Stack

### Frontend
- Next.js 14 (App Router)
- TypeScript
- Tailwind CSS
- Supabase JS Client
- Recharts for analytics

### Backend
- Python 3.11
- Web3.py for blockchain
- Supabase Python Client
- Flask (optional REST endpoints)

### Mobile
- Kotlin
- Android Device Policy Controller
- WorkManager for background sync
- Retrofit for API calls

### Database
- PostgreSQL (Supabase)
- Row Level Security
- Triggers and Functions
- Real-time subscriptions

## Scalability

- Supabase handles up to 500GB database
- Backend can be scaled horizontally on Render
- Dashboard is serverless on Vercel (auto-scaling)
- Android devices sync independently

## Monitoring

- Supabase dashboard for database metrics
- Render logs for backend errors
- Vercel analytics for dashboard performance
- Device sync timestamps for health checks

## Future Enhancements

- SMS notifications for payment reminders
- Mobile app for customers to check balance
- Biometric unlock for temporary access
- GPS tracking integration
- Automated payment reminders
- Multi-currency support
