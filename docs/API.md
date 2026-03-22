# API Documentation

## Authentication

All API requests require authentication via Supabase Auth.

### Headers
```
Authorization: Bearer <supabase-jwt-token>
apikey: <supabase-anon-key>
```

## Endpoints

### Devices

#### Get All Devices
```
GET /rest/v1/devices
```

Response:
```json
[
  {
    "id": "uuid",
    "device_code": "DEV000001",
    "imei": "123456789012345",
    "customer_id": "uuid",
    "device_model": "Samsung Galaxy A14",
    "loan_balance": 12000.00,
    "is_locked": true,
    "status": "active"
  }
]
```

#### Create Device
```
POST /rest/v1/devices
```

Body:
```json
{
  "imei": "123456789012345",
  "customer_id": "uuid",
  "device_model": "Samsung Galaxy A14",
  "device_price": 15000.00,
  "down_payment": 3000.00,
  "daily_payment": 100.00,
  "payment_period_days": 120
}
```

#### Update Device Lock Status
```
PATCH /rest/v1/devices?id=eq.<device-id>
```

Body:
```json
{
  "is_locked": false,
  "last_sync": "2024-01-01T12:00:00Z"
}
```

### Customers

#### Get All Customers
```
GET /rest/v1/customers
```

#### Create Customer
```
POST /rest/v1/customers
```

Body:
```json
{
  "full_name": "John Doe",
  "phone": "+254712345678",
  "national_id": "12345678",
  "address": "Nairobi, Kenya",
  "enrolled_by": "admin-uuid"
}
```

### Payments

#### Get Payment History
```
GET /rest/v1/payment_transactions?device_id=eq.<device-id>
```

#### Process Payment
```
POST /rest/v1/payment_transactions
```

Body:
```json
{
  "device_id": "uuid",
  "customer_id": "uuid",
  "amount": 100.00,
  "payment_method": "mpesa",
  "mpesa_receipt": "ABC123XYZ",
  "mpesa_phone": "+254712345678",
  "processed_by": "admin-uuid"
}
```

### Administrators

#### Create Administrator
```
POST /rest/v1/administrators
```

Body:
```json
{
  "email": "admin@example.com",
  "full_name": "Admin Name",
  "phone": "+254712345678",
  "created_by": "super-admin-uuid",
  "region": "Nairobi",
  "branch": "CBD"
}
```

## Device Sync API

### Check Lock Status
```
GET /rest/v1/devices?device_code=eq.<device-code>&select=is_locked,loan_balance,status
```

Response:
```json
[
  {
    "is_locked": false,
    "loan_balance": 5000.00,
    "status": "active"
  }
]
```

### Update Last Sync
```
PATCH /rest/v1/devices?device_code=eq.<device-code>
```

Body:
```json
{
  "last_sync": "2024-01-01T12:00:00Z"
}
```

## Error Responses

### 401 Unauthorized
```json
{
  "message": "Invalid token"
}
```

### 403 Forbidden
```json
{
  "message": "Insufficient permissions"
}
```

### 400 Bad Request
```json
{
  "message": "Invalid request body",
  "details": "Missing required field: customer_id"
}
```

## Rate Limits

- 100 requests per minute per user
- 1000 requests per hour per user

## Webhooks

### Payment Received
Triggered when a crypto payment is detected by the backend listener.

Payload:
```json
{
  "event": "payment.received",
  "device_code": "DEV000001",
  "amount": 100.00,
  "transaction_hash": "0x...",
  "timestamp": "2024-01-01T12:00:00Z"
}
```
