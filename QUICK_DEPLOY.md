# ⚡ Quick Deploy Reference

## 📊 Render Configuration Chart

| Setting | Value |
|---------|-------|
| **Environment** | `Docker` |
| **Branch** | `main` |
| **Dockerfile Path** | `./Dockerfile` |
| **Auto-Deploy** | `Yes` |

---

## 🔧 Build & Start Commands

### If Using Docker (Recommended)
```
Environment: Docker
Dockerfile Path: ./Dockerfile
```
✅ That's it! Docker handles everything.

### If NOT Using Docker
```bash
# Build Command
npm install --prefix dashboard && npm run build --prefix dashboard && pip install -r requirements.txt

# Start Command
gunicorn --bind 0.0.0.0:$PORT --workers 2 --threads 4 --timeout 120 server:app
```

---

## 🔑 Environment Variables

### Copy-Paste Ready

```env
SUPABASE_URL=https://fvkjeteywfcppbtovbiv.supabase.co
SUPABASE_SERVICE_KEY=GET_FROM_SUPABASE_SETTINGS_API
```

### Where to Add
1. Render Dashboard
2. Your Service
3. Environment tab
4. Add each variable

---

## 📋 Deployment Checklist

### Before Deploy
- [x] Code on GitHub: ✅ https://github.com/sammysam254/edenkelock
- [ ] Supabase schema applied
- [ ] Service key obtained

### During Deploy
- [ ] Render account created
- [ ] Repository connected
- [ ] Environment variables added
- [ ] Deploy clicked

### After Deploy
- [ ] Health check passes
- [ ] Dashboard loads
- [ ] Admin user created
- [ ] Login successful

---

## 🎯 3-Step Deploy

### Step 1: Database (2 min)
```
1. Go to Supabase SQL Editor
2. Run database/schema.sql
3. Run database/rls_policies.sql
4. Get service key from Settings → API
```

### Step 2: Render (3 min)
```
1. Go to dashboard.render.com
2. New + → Blueprint
3. Connect sammysam254/edenkelock
4. Add SUPABASE_SERVICE_KEY
5. Click Apply
```

### Step 3: Admin (2 min)
```
1. Supabase → super_admins → Insert
2. Supabase → Authentication → Add user
3. Login to your dashboard
```

**Total Time: 7 minutes** ⏱️

---

## 🌐 URLs

| Service | URL |
|---------|-----|
| **GitHub** | https://github.com/sammysam254/edenkelock |
| **Render** | https://dashboard.render.com/ |
| **Supabase** | https://fvkjeteywfcppbtovbiv.supabase.co |
| **Your App** | https://eden-mkopa.onrender.com (after deploy) |

---

## 🧪 Test Commands

### Health Check
```bash
curl https://your-app.onrender.com/api/health
```

### Stats API
```bash
curl https://your-app.onrender.com/api/stats
```

### Dashboard
```
Open in browser: https://your-app.onrender.com
```

---

## 💰 Cost

| Plan | Cost | Best For |
|------|------|----------|
| **Free** | $0/month | Testing |
| **Starter** | $7/month | Production |
| **Pro** | $25/month | Scale |

---

## 🆘 Quick Fixes

### Build Fails
```
✅ Use Docker environment
✅ Check all files in GitHub
✅ Verify Dockerfile exists
```

### Can't Connect
```
✅ Add SUPABASE_SERVICE_KEY
✅ Check service key is correct
✅ Verify database schema applied
```

### Dashboard Errors
```
✅ Run schema.sql in Supabase
✅ Run rls_policies.sql in Supabase
✅ Create super admin user
```

---

## 📱 What You Get

✨ Beautiful gradient dashboard
📊 Real-time analytics
🔒 Device lock/unlock
👥 Customer management
💰 Payment processing
📈 Reporting & insights
🔐 Secure authentication
📱 Mobile responsive

---

## 🚀 Deploy Now!

1. **Render**: https://dashboard.render.com/
2. **Blueprint** → Connect `sammysam254/edenkelock`
3. **Add** `SUPABASE_SERVICE_KEY`
4. **Deploy** and wait 5-10 minutes
5. **Done!** 🎉

---

**Need Help?** See [RENDER_SETUP.md](RENDER_SETUP.md) for detailed guide.
