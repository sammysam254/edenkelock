# User Manual

## For Super Administrators

### Initial Setup
1. Login to Supabase and add your email to `super_admins` table
2. Access the dashboard at your Vercel URL
3. Create administrator accounts for field agents

### Managing Administrators
- View all administrators in the system
- Create new administrator accounts
- Assign regions and branches
- Deactivate administrators if needed

### Viewing Reports
- Total devices enrolled
- Total revenue collected
- Payment trends
- Device lock status overview

## For Administrators

### Enrolling a Customer

1. Navigate to "Customers" section
2. Click "Add New Customer"
3. Fill in customer details:
   - Full name
   - Phone number
   - National ID
   - Address
4. Click "Save"

### Enrolling a Device

1. Navigate to "Devices" section
2. Click "Enroll New Device"
3. Fill in device details:
   - Select customer
   - Enter IMEI number
   - Select device model
   - Enter device price
   - Set down payment amount
   - Set daily payment amount
   - Set payment period (days)
4. Click "Generate QR Code"
5. Save the QR code image

### Device Provisioning

1. Factory reset the Android device
2. During setup, when prompted, scan the QR code
3. Device will download and install the DPC app
4. Device will be locked until first payment
5. Give device to customer

### Processing Payments

#### M-Pesa Payment
1. Customer makes M-Pesa payment
2. Navigate to "Payments" section
3. Click "Process Payment"
4. Select device
5. Enter payment details:
   - Amount
   - M-Pesa receipt code
   - M-Pesa phone number
6. Click "Submit"
7. Device will unlock on next sync (within 15 minutes)

#### Cash Payment
1. Collect cash from customer
2. Navigate to "Payments" section
3. Select payment method: "Cash"
4. Enter amount and notes
5. Click "Submit"

### Checking Device Status

1. Navigate to "Devices" section
2. View device card showing:
   - Lock status (locked/unlocked)
   - Remaining balance
   - Last sync time
   - Payment history

## For Customers

### Making Payments

#### M-Pesa
1. Go to M-Pesa menu
2. Select "Lipa na M-Pesa"
3. Select "Pay Bill"
4. Enter business number: [YOUR_PAYBILL]
5. Enter account number: Your device code (e.g., DEV000001)
6. Enter amount
7. Enter M-Pesa PIN
8. Confirm payment
9. Device will unlock within 15 minutes

#### Crypto Payment
1. Open your crypto wallet
2. Send tokens to your device wallet address
3. Device will unlock automatically within 30 minutes

### Device Lock Screen

If your device is locked:
- You will see a red lock screen
- Message: "Please make your payment to unlock this device"
- Contact support if you've already paid

### Checking Balance

Contact your administrator to check:
- Remaining loan balance
- Payment history
- Next payment due date

## Troubleshooting

### Device Not Unlocking After Payment

1. Check if payment was processed successfully
2. Wait 15 minutes for device to sync
3. Ensure device has internet connection
4. Contact administrator to verify payment

### Device Stuck on Lock Screen

1. Ensure internet connection is active
2. Wait for automatic sync (every 15 minutes)
3. Contact administrator to check device status
4. Administrator can manually trigger unlock

### QR Code Not Scanning

1. Ensure device is factory reset
2. During setup, look for "Scan QR code" option
3. If option doesn't appear, device may not support provisioning
4. Contact support for manual enrollment

### Lost Device

1. Contact administrator immediately
2. Administrator can mark device as "defaulted"
3. Device will remain locked
4. Report to authorities if stolen

## Support

For technical support:
- Email: support@edenservices.ke
- Phone: +254700000000
- Hours: Monday-Friday, 8AM-6PM EAT
