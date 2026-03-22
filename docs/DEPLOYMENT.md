# Deployment Guide

## Prerequisites

- Supabase account
- Render account (for backend)
- Vercel account (for dashboard)
- Android Studio
- Python 3.11+
- Node.js 18+

## Step 1: Database Setup

1. Create a new Supabase project
2. Run the SQL scripts in order:
   ```bash
   # In Supabase SQL Editor
   # 1. Run database/schema.sql
   # 2. Run database/rls_policies.sql
   ```

3. Get your Supabase credentials:
   - Project URL
   - Anon key
   - Service role key

## Step 2: Backend Deployment (Render)

1. Create a new Web Service on Render
2. Connect your GitHub repository
3. Configure:
   - Build Command: `pip install -r requirements.txt`
   - Start Command: `python main.py`
   
4. Add environment variables:
   ```
   SUPABASE_URL=your-supabase-url
   SUPABASE_SERVICE_KEY=your-service-key
   RPC_URL=your-web3-rpc-url
   CONTRACT_ADDRESS=your-token-contract
   POLL_INTERVAL=30
   ```

5. Deploy

## Step 3: Dashboard Deployment (Vercel)

1. Install dependencies:
   ```bash
   cd dashboard
   npm install
   ```

2. Create `.env.local`:
   ```
   NEXT_PUBLIC_SUPABASE_URL=your-supabase-url
   NEXT_PUBLIC_SUPABASE_ANON_KEY=your-anon-key
   ```

3. Deploy to Vercel:
   ```bash
   npm run build
   vercel deploy --prod
   ```

## Step 4: Android App Build

1. Open project in Android Studio
2. Update `build.gradle` with your signing config
3. Build release APK:
   ```bash
   cd android
   ./gradlew assembleRelease
   ```

4. Upload APK to a public URL (e.g., Firebase Hosting, S3)
5. Update the APK URL in `provisioning/generate_qr.py`

## Step 5: Device Provisioning

1. Generate QR codes for devices:
   ```bash
   cd provisioning
   pip install -r requirements.txt
   python generate_qr.py DEV000001 admin@example.com
   ```

2. Factory reset the Android device
3. During setup, scan the QR code when prompted
4. Device will download and install the DPC app
5. Device will be locked until first payment

## Step 6: Testing

1. Create a super admin in Supabase
2. Login to dashboard
3. Create an administrator
4. Enroll a test device
5. Process a test payment
6. Verify device unlocks

## Security Checklist

- [ ] RLS policies enabled on all tables
- [ ] Service role key kept secure
- [ ] HTTPS enabled on all endpoints
- [ ] Factory reset disabled on devices
- [ ] ADB disabled on devices
- [ ] Safe boot disabled on devices

## Monitoring

- Check Render logs for backend errors
- Monitor Supabase dashboard for database issues
- Use Vercel analytics for dashboard performance
- Set up alerts for payment processing failures

## Support

For issues, contact: support@edenservices.ke
