# Render Deployment Guide

## 🚀 Deploy Eden M-Kopa to Render in 10 Minutes

This guide will help you deploy the complete Eden M-Kopa system (frontend + backend) as a single web service on Render.

## Prerequisites

1. [Render account](https://render.com/) (free tier works)
2. [GitHub account](https://github.com/)
3. Supabase project set up with database schema

## Step 1: Prepare Your Repository

1. Push your code to GitHub:
```bash
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/your-username/eden-mkopa.git
git push -u origin main
```

## Step 2: Get Supabase Service Key

1. Go to your Supabase project
2. Click Settings → API
3. Copy the `service_role` key (NOT the anon key)
4. Keep it safe - you'll need it for Render

## Step 3: Deploy to Render

### Option A: Using render.yaml (Recommended)

1. Go to [Render Dashboard](https://dashboard.render.com/)
2. Click "New +" → "Blueprint"
3. Connect your GitHub repository
4. Render will detect `render.yaml` automatically
5. Add environment variable:
   - Key: `SUPABASE_SERVICE_KEY`
   - Value: Your service role key from Step 2
6. Click "Apply"
7. Wait 5-10 minutes for build to complete

### Option B: Manual Setup

1. Go to [Render Dashboard](https://dashboard.render.com/)
2. Click "New +" → "Web Service"
3. Connect your GitHub repository
4. Configure:
   - **Name**: eden-mkopa
   - **Environment**: Docker
   - **Region**: Choose closest to you
   - **Branch**: main
   - **Dockerfile Path**: ./Dockerfile
   - **Plan**: Free

5. Add Environment Variables:
   ```
   SUPABASE_URL=https://fvkjeteywfcppbtovbiv.supabase.co
   SUPABASE_SERVICE_KEY=your-service-role-key-here
   RPC_URL=(optional - for crypto payments)
   CONTRACT_ADDRESS=(optional - for crypto payments)
   POLL_INTERVAL=30
   PORT=10000
   ```

6. Click "Create Web Service"
7. Wait for deployment (5-10 minutes)

## Step 4: Verify Deployment

1. Once deployed, Render will give you a URL like:
   `https://eden-mkopa.onrender.com`

2. Visit the URL - you should see the beautiful dashboard

3. Test the health endpoint:
   `https://eden-mkopa.onrender.com/api/health`

   Should return:
   ```json
   {
     "status": "healthy",
     "supabase": "connected",
     "web3": "not configured"
   }
   ```

## Step 5: Create First Admin User

1. Go to Supabase → Table Editor → `super_admins`
2. Insert new row:
   - email: your-email@example.com
   - full_name: Your Name
   - phone: +254712345678
   - is_active: true

3. Go to Supabase → Authentication → Add User
   - Email: same as above
   - Password: create a strong password

## Step 6: Login to Dashboard

1. Visit your Render URL
2. Login with your credentials
3. You should see the dashboard with stats

## Troubleshooting

### Build Fails

**Error**: "npm install failed"
- Check that `dashboard/package.json` exists
- Verify Node.js version in Dockerfile

**Error**: "pip install failed"
- Check that `requirements.txt` exists
- Verify Python version in Dockerfile

### Dashboard Shows Errors

**Error**: "Failed to fetch"
- Check SUPABASE_URL is correct
- Verify SUPABASE_SERVICE_KEY is set
- Check Supabase RLS policies are applied

**Error**: "No data showing"
- Ensure database schema is applied
- Check that super admin user exists
- Verify RLS policies allow access

### Service Won't Start

**Error**: "Port already in use"
- Render automatically sets PORT=10000
- Don't override this in your code

**Error**: "Module not found"
- Check all dependencies are in requirements.txt
- Verify Dockerfile copies all necessary files

## Performance Optimization

### Free Tier Limitations
- Service spins down after 15 minutes of inactivity
- First request after spin-down takes 30-60 seconds
- 750 hours/month free (enough for 24/7 operation)

### Upgrade to Paid Tier ($7/month)
- Always-on service (no spin-down)
- Faster builds
- More memory and CPU
- Custom domains

## Monitoring

### View Logs
1. Go to Render Dashboard
2. Click your service
3. Click "Logs" tab
4. Monitor for errors

### Check Metrics
1. Go to Render Dashboard
2. Click your service
3. Click "Metrics" tab
4. View CPU, memory, and request stats

## Custom Domain (Optional)

1. Go to Render Dashboard
2. Click your service
3. Click "Settings" → "Custom Domain"
4. Add your domain (e.g., app.yourdomain.com)
5. Update DNS records as instructed
6. Wait for SSL certificate (automatic)

## Environment Variables Reference

| Variable | Required | Description |
|----------|----------|-------------|
| SUPABASE_URL | Yes | Your Supabase project URL |
| SUPABASE_SERVICE_KEY | Yes | Service role key (keep secret!) |
| RPC_URL | No | Web3 RPC endpoint for crypto payments |
| CONTRACT_ADDRESS | No | Token contract address |
| POLL_INTERVAL | No | Blockchain polling interval (default: 30) |
| PORT | Auto | Render sets this automatically |

## Security Checklist

- [ ] SUPABASE_SERVICE_KEY is kept secret
- [ ] RLS policies are applied in database
- [ ] Super admin user created
- [ ] HTTPS is enabled (automatic on Render)
- [ ] Environment variables are not in code
- [ ] Logs don't expose sensitive data

## Cost Estimate

### Free Tier
- Web Service: Free (750 hours/month)
- Supabase: Free (500MB database)
- **Total: $0/month**

### Paid Tier
- Web Service: $7/month (always-on)
- Supabase Pro: $25/month (8GB database)
- **Total: $32/month**

## Next Steps

1. ✅ Service deployed and running
2. ✅ Dashboard accessible
3. ✅ Admin user created
4. 📱 Build Android app
5. 📊 Start enrolling devices
6. 💰 Process payments

## Support

- **Render Docs**: https://render.com/docs
- **Supabase Docs**: https://supabase.com/docs
- **GitHub Issues**: Report bugs
- **Email**: support@edenservices.ke

## Useful Commands

### View Logs
```bash
# Install Render CLI
npm install -g @render/cli

# Login
render login

# View logs
render logs -s eden-mkopa
```

### Redeploy
```bash
# Push to GitHub
git add .
git commit -m "Update"
git push

# Render auto-deploys on push
```

### Manual Deploy
1. Go to Render Dashboard
2. Click your service
3. Click "Manual Deploy" → "Deploy latest commit"

---

🎉 **Congratulations!** Your Eden M-Kopa system is now live on Render!

Visit your URL and start managing device financing operations.
