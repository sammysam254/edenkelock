# Quick Start Guide

Get Eden M-Kopa running in 30 minutes.

## Prerequisites

- [Node.js 18+](https://nodejs.org/)
- [Python 3.11+](https://www.python.org/)
- [Supabase account](https://supabase.com/)
- [Render account](https://render.com/) (optional, for backend)
- [Vercel account](https://vercel.com/) (optional, for dashboard)
- [Android Studio](https://developer.android.com/studio) (for Android app)

## Step 1: Clone Repository

```bash
git clone https://github.com/your-username/eden-mkopa.git
cd eden-mkopa
```

## Step 2: Database Setup (5 minutes)

1. Create a new project on [Supabase](https://supabase.com/)
2. Go to SQL Editor
3. Copy and paste `database/schema.sql` → Run
4. Copy and paste `database/rls_policies.sql` → Run
5. Note your credentials:
   - Project URL: `https://xxx.supabase.co`
   - Anon key: `eyJxxx...`
   - Service role key: `eyJxxx...`

## Step 3: Dashboard Setup (5 minutes)

```bash
cd dashboard
npm install
cp .env.local.example .env.local
```

Edit `.env.local`:
```env
NEXT_PUBLIC_SUPABASE_URL=https://your-project.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=your-anon-key
```

Start dashboard:
```bash
npm run dev
```

Visit: http://localhost:3000

## Step 4: Backend Setup (5 minutes)

```bash
cd backend
python -m venv venv

# On Windows:
venv\Scripts\activate

# On Mac/Linux:
source venv/bin/activate

pip install -r requirements.txt
cp .env.example .env
```

Edit `.env`:
```env
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_SERVICE_KEY=your-service-role-key
RPC_URL=https://mainnet.infura.io/v3/your-key
CONTRACT_ADDRESS=0x...
POLL_INTERVAL=30
```

Start backend:
```bash
python main.py
```

## Step 5: Create First Admin (2 minutes)

1. Go to Supabase → Table Editor → `super_admins`
2. Insert new row:
   - email: your-email@example.com
   - full_name: Your Name
   - phone: +254712345678
   - is_active: true

3. Go to Supabase → Authentication → Add User
   - Email: same as above
   - Password: create a password

## Step 6: Test Dashboard (3 minutes)

1. Visit http://localhost:3000
2. Login with your credentials
3. You should see the dashboard
4. Create a test administrator
5. Create a test customer
6. Create a test device

## Step 7: Android App (10 minutes)

1. Open `android/` folder in Android Studio
2. Wait for Gradle sync
3. Update `ApiService.kt`:
   ```kotlin
   private const val BASE_URL = "https://your-project.supabase.co/"
   private const val API_KEY = "your-anon-key"
   ```
4. Build APK: Build → Build Bundle(s) / APK(s) → Build APK(s)
5. Upload APK to a public URL
6. Update URL in `provisioning/generate_qr.py`

## Step 8: Generate QR Code (2 minutes)

```bash
cd provisioning
pip install -r requirements.txt
python generate_qr.py DEV000001 admin@example.com
```

This creates `qr_DEV000001.png`

## Step 9: Test Device Enrollment (5 minutes)

1. Factory reset an Android device (or use emulator)
2. During setup, scan the QR code
3. Device will download and install the app
4. Device will show lock screen
5. Process a test payment in dashboard
6. Wait 15 minutes for device to sync
7. Device should unlock

## Troubleshooting

### Dashboard won't start
- Check Node.js version: `node --version` (should be 18+)
- Delete `node_modules` and run `npm install` again
- Check `.env.local` has correct Supabase credentials

### Backend won't start
- Check Python version: `python --version` (should be 3.11+)
- Activate virtual environment first
- Check `.env` has correct credentials

### Device won't provision
- Ensure device is factory reset
- Check if device supports QR provisioning
- Verify APK URL is accessible
- Try manual provisioning via ADB

### Device won't unlock
- Check payment was recorded in dashboard
- Verify device has internet connection
- Check device `last_sync` timestamp
- Wait 15 minutes for automatic sync

## Next Steps

- Read [DEPLOYMENT.md](DEPLOYMENT.md) for production deployment
- Read [USER_MANUAL.md](USER_MANUAL.md) for detailed usage
- Read [SECURITY.md](SECURITY.md) for security best practices
- Read [API.md](API.md) for API documentation

## Production Deployment

### Dashboard (Vercel)
```bash
cd dashboard
vercel deploy --prod
```

### Backend (Render)
1. Create new Web Service on Render
2. Connect GitHub repo
3. Add environment variables
4. Deploy

### Android App
1. Build release APK with signing
2. Upload to hosting (Firebase, S3, etc.)
3. Update QR code generator with APK URL
4. Generate QR codes for devices

## Support

Need help? 
- Email: support@edenservices.ke
- GitHub Issues: Report bugs
- Documentation: See docs/ folder
