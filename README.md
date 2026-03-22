# Eden M-Kopa Style Device Financing System

Complete "Lipa Polepole" (Pay-as-you-go) Android device-locking system with Web3 blockchain integration.

## 🎯 System Components

1. **Android DPC App** - Device Policy Controller with kiosk mode
2. **Web Dashboard** - Multi-tenant admin interface (Super Admin + Administrators)
3. **Python Backend** - Web3 blockchain listener on Render
4. **Supabase Database** - PostgreSQL with RLS

## 📁 Project Structure

```
eden-mkopa/
├── android/                          # Android DPC App (Kotlin)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/eden/mkopa/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── LockScreenActivity.kt
│   │   │   │   ├── DeviceAdminReceiver.kt
│   │   │   │   ├── SyncService.kt
│   │   │   │   ├── BootReceiver.kt
│   │   │   │   └── ApiService.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   │   └── xml/
│   │   │   └── AndroidManifest.xml
│   │   ├── build.gradle
│   │   └── proguard-rules.pro
│   ├── build.gradle
│   ├── settings.gradle
│   └── gradle.properties
│
├── dashboard/                        # Next.js Admin Dashboard
│   ├── app/
│   │   ├── api/devices/route.ts
│   │   ├── devices/page.tsx
│   │   ├── page.tsx
│   │   ├── layout.tsx
│   │   └── globals.css
│   ├── components/
│   │   └── DeviceCard.tsx
│   ├── lib/
│   │   └── supabase.ts
│   ├── package.json
│   ├── tsconfig.json
│   ├── tailwind.config.ts
│   ├── next.config.js
│   ├── postcss.config.js
│   ├── vercel.json
│   └── .env.local.example
│
├── backend/                          # Python Web3 Listener
│   ├── main.py
│   ├── requirements.txt
│   ├── Dockerfile
│   ├── render.yaml
│   └── .env.example
│
├── database/                         # Supabase SQL Schema
│   ├── schema.sql
│   └── rls_policies.sql
│
├── provisioning/                     # QR Code Generation
│   ├── generate_qr.py
│   └── requirements.txt
│
├── docs/                            # Documentation
│   ├── ARCHITECTURE.md
│   ├── DEPLOYMENT.md
│   ├── API.md
│   ├── USER_MANUAL.md
│   └── SECURITY.md
│
├── README.md
├── LICENSE
├── CONTRIBUTING.md
└── .gitignore
```

## 🚀 Quick Deploy to Render

### ⚡ One-Click Deployment (10 Minutes)

Your app is **pre-configured** with Supabase credentials and ready to deploy!

1. **Push to GitHub**:
```bash
git init
git add .
git commit -m "Deploy Eden M-Kopa"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/eden-mkopa.git
git push -u origin main
```

2. **Deploy to Render**:
   - Go to [Render Dashboard](https://dashboard.render.com/)
   - Click "New +" → "Blueprint"
   - Connect your GitHub repository
   - Add environment variable: `SUPABASE_SERVICE_KEY` (get from Supabase Settings → API)
   - Click "Apply"
   - ⏰ Wait 5-10 minutes

3. **Done!** 🎉 Your app is live at: `https://eden-mkopa.onrender.com`

📖 **Detailed Guide**: See [RENDER_DEPLOYMENT.md](RENDER_DEPLOYMENT.md)

### 🎨 What You Get

- ✨ Beautiful gradient dashboard with animations
- 📊 Real-time analytics and stats
- 🔒 Device lock/unlock management
- 📱 Responsive mobile-friendly design
- 🚀 Backend API + Frontend in one service
- 💰 Free tier available (no credit card needed)

### 📋 Pre-Deployment Checklist

- [x] Supabase credentials configured
- [x] Beautiful UI implemented
- [x] Unified server ready
- [x] Docker configuration complete
- [ ] Run database schema in Supabase
- [ ] Get service role key
- [ ] Push to GitHub
- [ ] Deploy to Render

## 🚀 Quick Start (Local Development)

### Automated Setup (Recommended)

**Windows:**
```bash
scripts\setup.bat
```

**Mac/Linux:**
```bash
chmod +x scripts/setup.sh
./scripts/setup.sh
```

### Manual Setup

1. **Database Setup (Supabase)**
   ```bash
   # Create Supabase project
   # Run database/schema.sql in SQL Editor
   # Run database/rls_policies.sql in SQL Editor
   ```

2. **Dashboard Setup**
   ```bash
   cd dashboard
   npm install
   cp .env.local.example .env.local
   # Edit .env.local with Supabase credentials
   npm run dev
   ```

3. **Backend Setup**
   ```bash
   cd backend
   python -m venv venv
   source venv/bin/activate  # Windows: venv\Scripts\activate
   pip install -r requirements.txt
   cp .env.example .env
   # Edit .env with credentials
   python main.py
   ```

4. **Android App**
   ```bash
   # Open android/ folder in Android Studio
   # Update ApiService.kt with Supabase credentials
   # Build APK: Build → Build APK
   ```

See [QUICKSTART.md](docs/QUICKSTART.md) for detailed 30-minute setup guide.

## 📊 Features

- ✅ Multi-tenant role-based access (Super Admin, Administrators, Customers)
- ✅ Device enrollment via QR code provisioning
- ✅ Automatic device lock/unlock based on payment status
- ✅ Web3 blockchain payment detection
- ✅ M-Pesa payment integration
- ✅ Real-time device synchronization
- ✅ Comprehensive reporting and analytics
- ✅ Hardened security (Factory reset disabled, ADB blocked)

## 🔐 Security

- Device Owner enforcement
- Factory reset disabled
- Safe boot disabled
- ADB/Developer options blocked
- Kiosk mode lock
- Row Level Security (RLS) in database

## 📖 Documentation

See `/docs` folder for detailed documentation:
- [ARCHITECTURE.md](docs/ARCHITECTURE.md) - System design and data flow
- [DEPLOYMENT.md](docs/DEPLOYMENT.md) - Step-by-step deployment guide
- [API.md](docs/API.md) - REST API endpoints and examples
- [USER_MANUAL.md](docs/USER_MANUAL.md) - User guides for all roles
- [SECURITY.md](docs/SECURITY.md) - Security measures and best practices

## 🛠️ Technology Stack

- **Frontend**: Next.js 14, TypeScript, Tailwind CSS, Supabase JS
- **Backend**: Python 3.11, Web3.py, Flask
- **Mobile**: Kotlin, Android DPC, WorkManager, Retrofit
- **Database**: PostgreSQL (Supabase) with RLS
- **Deployment**: Vercel (Dashboard), Render (Backend)
- **Blockchain**: Web3 payment detection

## 📚 Additional Resources

- [Quick Start Guide](docs/QUICKSTART.md) - Get running in 30 minutes
- [FAQ](docs/FAQ.md) - Frequently asked questions
- [Contributing](CONTRIBUTING.md) - How to contribute

## 🤝 Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## 📄 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

## 📞 Support

- **Email**: support@edenservices.ke
- **GitHub Issues**: For bug reports and feature requests
- **Documentation**: See `/docs` folder

## ⚠️ Disclaimer

This system is provided as-is for educational and commercial use. Ensure compliance with local laws and regulations regarding device financing and consumer protection. Always disclose device lock terms to customers before purchase.

## 🌟 Acknowledgments

Inspired by M-Kopa's innovative device financing model. Built with modern web and mobile technologies to democratize access to device financing systems.
