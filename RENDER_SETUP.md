# 🚀 Render Deployment - Quick Reference

## Render Configuration

### Build Command
```bash
npm install --prefix dashboard && npm run build --prefix dashboard && pip install -r requirements.txt
```

### Start Command
```bash
gunicorn --bind 0.0.0.0:$PORT --workers 2 --threads 4 --timeout 120 server:app
```

### Docker Command (Alternative)
If using Docker:
```
Docker
```

---

## Environment Variables (.env)

Copy these to Render Dashboard → Environment Variables:

```env
# Required - Supabase Configuration
SUPABASE_URL=https://fvkjeteywfcppbtovbiv.supabase.co
SUPABASE_SERVICE_KEY=your-service-role-key-here

# Optional - Web3 Blockchain (for crypto payments)
RPC_URL=
CONTRACT_ADDRESS=
POLL_INTERVAL=30

# Auto-set by Render
PORT=10000
```

---

## Step-by-Step Render Setup

### Option 1: Using Blueprint (Recommended - Easiest)

1. Go to https://dashboard.render.com/
2. Click **"New +"** → **"Blueprint"**
3. Connect repository: `sammysam254/edenkelock`
4. Render detects `render.yaml` automatically
5. Add environment variable:
   - Key: `SUPABASE_SERVICE_KEY`
   - Value: (get from Supabase Settings → API)
6. Click **"Apply"**
7. Wait 5-10 minutes ⏰

### Option 2: Manual Web Service

1. Go to https://dashboard.render.com/
2. Click **"New +"** → **"Web Service"**
3. Connect repository: `sammysam254/edenkelock`
4. Configure:

| Setting | Value |
|---------|-------|
| **Name** | `eden-mkopa` |
| **Environment** | `Docker` |
| **Region** | Choose closest to you |
| **Branch** | `main` |
| **Root Directory** | (leave empty) |
| **Dockerfile Path** | `./Dockerfile` |

5. Add Environment Variables (see table below)
6. Click **"Create Web Service"**
7. Wait 5-10 minutes ⏰

---

## Environment Variables Table

| Variable | Required | Value | Description |
|----------|----------|-------|-------------|
| `SUPABASE_URL` | ✅ Yes | `https://fvkjeteywfcppbtovbiv.supabase.co` | Your Supabase project URL |
| `SUPABASE_SERVICE_KEY` | ✅ Yes | `eyJhbG...` (get from Supabase) | Service role key (keep secret!) |
| `RPC_URL` | ❌ No | (empty) | Web3 RPC endpoint (optional) |
| `CONTRACT_ADDRESS` | ❌ No | (empty) | Token contract address (optional) |
| `POLL_INTERVAL` | ❌ No | `30` | Blockchain polling interval |
| `PORT` | 🔄 Auto | `10000` | Render sets this automatically |

---

## How to Get SUPABASE_SERVICE_KEY

1. Go to https://supabase.com/dashboard
2. Select your project
3. Click **Settings** (gear icon)
4. Click **API**
5. Scroll to **Project API keys**
6. Copy the **`service_role`** key (NOT the anon key)
7. Paste into Render environment variables

⚠️ **Important**: Keep this key secret! Never commit it to GitHub.

---

## Deployment Methods Comparison

### Method 1: Blueprint (render.yaml)
✅ Easiest - one click
✅ Auto-configured
✅ Just add service key
⏱️ 5 minutes

### Method 2: Docker
✅ Reliable
✅ Consistent builds
✅ Production-ready
⏱️ 7 minutes

### Method 3: Manual (without Docker)
❌ More complex
❌ Manual configuration
⏱️ 10 minutes

**Recommendation**: Use Blueprint (Method 1)

---

## After Deployment

### 1. Get Your URL
Render will give you a URL like:
```
https://eden-mkopa.onrender.com
```

### 2. Test Health Endpoint
```bash
curl https://your-app.onrender.com/api/health
```

Expected response:
```json
{
  "status": "healthy",
  "supabase": "connected",
  "web3": "not configured"
}
```

### 3. Visit Dashboard
Open your Render URL in browser - you should see the beautiful gradient dashboard!

### 4. Create Admin User

In Supabase:
1. **Table Editor** → `super_admins` → **Insert row**:
   ```
   email: your-email@example.com
   full_name: Your Name
   phone: +254712345678
   is_active: true
   ```

2. **Authentication** → **Add user**:
   ```
   Email: your-email@example.com
   Password: (create strong password)
   ```

3. Login to your dashboard!

---

## Troubleshooting

### Build Fails

**Error**: "npm install failed"
- Check `dashboard/package.json` exists
- Verify Node.js version in Dockerfile

**Error**: "pip install failed"
- Check `requirements.txt` exists
- Verify Python version in Dockerfile

**Solution**: Use Docker environment (most reliable)

### Service Won't Start

**Error**: "Module not found"
- Ensure all files are pushed to GitHub
- Check Dockerfile copies all necessary files

**Error**: "Port already in use"
- Don't override PORT in code
- Render sets PORT automatically

### Dashboard Shows Errors

**Error**: "Failed to fetch"
- Check `SUPABASE_URL` is correct
- Verify `SUPABASE_SERVICE_KEY` is set
- Ensure database schema is applied

**Error**: "No data showing"
- Run `database/schema.sql` in Supabase
- Run `database/rls_policies.sql` in Supabase
- Create super admin user

---

## Performance Tips

### Free Tier
- Service spins down after 15 min inactivity
- First request takes 30-60 seconds
- Perfect for testing

### Paid Tier ($7/month)
- Always-on (no spin-down)
- Faster response times
- Better for production

---

## Quick Commands

### View Logs
```bash
# In Render Dashboard
Click your service → Logs tab
```

### Redeploy
```bash
# Push to GitHub
git add .
git commit -m "Update"
git push

# Render auto-deploys
```

### Manual Deploy
```
Render Dashboard → Your Service → Manual Deploy → Deploy latest commit
```

---

## Cost Estimate

| Tier | Cost | Features |
|------|------|----------|
| **Free** | $0/month | 750 hours, spins down after 15 min |
| **Starter** | $7/month | Always-on, 512MB RAM |
| **Standard** | $25/month | 2GB RAM, better performance |

**Recommendation**: Start with Free tier, upgrade when needed.

---

## Complete Environment Variables Example

Copy this to Render:

```env
SUPABASE_URL=https://fvkjeteywfcppbtovbiv.supabase.co
SUPABASE_SERVICE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ2a2pldGV5d2ZjcHBidG92Yml2Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTY3NDE5NTY3MSwiZXhwIjoxOTg5NzcxNjcxfQ.YOUR_ACTUAL_SERVICE_KEY_HERE
RPC_URL=
CONTRACT_ADDRESS=
POLL_INTERVAL=30
```

⚠️ Replace `YOUR_ACTUAL_SERVICE_KEY_HERE` with your real service key!

---

## Success Checklist

- [ ] Code pushed to GitHub
- [ ] Render account created
- [ ] Repository connected to Render
- [ ] Environment variables added
- [ ] Service deployed successfully
- [ ] Health endpoint returns "healthy"
- [ ] Dashboard loads in browser
- [ ] Database schema applied in Supabase
- [ ] RLS policies applied in Supabase
- [ ] Super admin user created
- [ ] Successfully logged in

---

## 🎉 You're Live!

Once deployed, your Eden M-Kopa system is live and ready to:
- ✅ Enroll devices
- ✅ Manage customers
- ✅ Process payments
- ✅ Monitor analytics
- ✅ Lock/unlock devices

**Your URL**: https://eden-mkopa.onrender.com (or similar)

---

## Support

- **GitHub**: https://github.com/sammysam254/edenkelock
- **Issues**: https://github.com/sammysam254/edenkelock/issues
- **Email**: support@edenservices.ke
