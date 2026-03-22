# Frequently Asked Questions

## General

### What is Eden M-Kopa?
Eden M-Kopa is a device financing system that allows customers to purchase Android devices on credit with automatic payment enforcement through device locking.

### How does it work?
Devices are locked until payments are made. When a customer makes a payment (via M-Pesa or crypto), the device automatically unlocks. If payments stop, the device locks again.

### Is this legal?
Yes, as long as customers agree to the terms before purchase. The device lock is disclosed upfront and customers sign a financing agreement.

## Technical

### What Android versions are supported?
Android 8.0 (API 26) and above. Device Owner mode requires Android 5.0+, but we recommend 8.0+ for best compatibility.

### Can customers factory reset the device?
No. Factory reset is disabled by the Device Policy Controller. This prevents customers from bypassing the lock.

### What happens if the device is offline?
The device will remain in its last known state (locked or unlocked) until it connects to the internet and syncs with the backend.

### How often does the device sync?
Every 15 minutes using Android WorkManager. This ensures timely unlocking after payment.

### Can the system work without blockchain?
Yes. The Web3 listener is optional. You can use only M-Pesa or cash payments processed through the dashboard.

## Deployment

### What are the hosting costs?
- Supabase: Free tier supports up to 500MB database
- Vercel: Free tier for dashboard
- Render: Free tier for backend (with limitations)
- Total: Can start with $0/month, scale as needed

### How do I get devices enrolled?
1. Generate QR code in dashboard
2. Factory reset device
3. Scan QR during setup
4. Device downloads and installs DPC app
5. Device is locked until first payment

### Can I use my own payment gateway?
Yes. The system is designed to be flexible. You can integrate any payment method by adding it to the payment_transactions table.

### How do I update the Android app?
You can push updates through the Device Policy Controller. The app can auto-update from your server.

## Security

### Is customer data secure?
Yes. All data is encrypted at rest in Supabase. Row Level Security ensures users can only access their own data.

### Can customers bypass the lock?
No. The app has Device Owner privileges, which is the highest level on Android. Factory reset, safe boot, and ADB are all disabled.

### What if a device is stolen?
Mark it as "defaulted" in the dashboard. The device will remain locked and unusable.

### How are API keys protected?
API keys are stored in environment variables and never committed to version control. The Android app uses ProGuard to obfuscate keys.

## Business

### How much should I charge for devices?
This depends on your market. Typical markup is 20-30% above retail price to cover financing costs and risk.

### What's a reasonable payment period?
Most device financing is 3-6 months (90-180 days). Longer periods increase default risk.

### How do I handle defaults?
After a certain number of missed payments (e.g., 30 days), mark the device as "defaulted". The device remains locked and you can pursue legal action.

### Can I offer insurance?
Yes. You can add insurance as an optional add-on to the financing agreement.

## Troubleshooting

### Device won't unlock after payment
1. Check if payment was recorded in dashboard
2. Verify device has internet connection
3. Wait 15 minutes for sync
4. Check device last_sync timestamp
5. Manually trigger sync if needed

### QR code provisioning not working
1. Ensure device is factory reset
2. Check if device supports QR provisioning (most modern devices do)
3. Verify QR code contains correct data
4. Try manual provisioning via ADB

### Dashboard not loading data
1. Check Supabase credentials in .env.local
2. Verify RLS policies are set up correctly
3. Check browser console for errors
4. Ensure user is authenticated

### Backend not detecting payments
1. Check RPC_URL is correct
2. Verify CONTRACT_ADDRESS is correct
3. Check backend logs for errors
4. Ensure Supabase credentials are correct

## Support

### Where can I get help?
- Email: support@edenservices.ke
- GitHub Issues: For bug reports
- Documentation: See docs/ folder

### Can I hire you to set this up?
Contact us at support@edenservices.ke for consulting services.

### Is there a hosted version?
Not currently. This is a self-hosted solution. You deploy it on your own infrastructure.

### Can I white-label this?
Yes. The system is open source (MIT License). You can rebrand and customize it as needed.
