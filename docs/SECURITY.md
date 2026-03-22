# Security Guidelines

## Overview

This document outlines the security measures implemented in the Eden M-Kopa system.

## Database Security

### Row Level Security (RLS)
- All tables have RLS enabled
- Super Admins have full access
- Administrators can only access their enrolled devices/customers
- Devices can only read their own record
- Service role for backend automation

### Authentication
- Supabase Auth with JWT tokens
- Token expiration: 1 hour
- Refresh tokens: 30 days
- Password requirements: min 8 characters

### API Keys
- Anon key: Public, used by dashboard and devices
- Service role key: Private, backend only
- Never commit keys to version control

## Android Device Security

### Device Owner Mode
- Highest privilege level on Android
- Cannot be removed without factory reset
- Factory reset is disabled by the app

### Restrictions Enforced
- Factory reset disabled
- Safe boot disabled
- ADB/Developer options blocked
- USB debugging disabled
- Installation from unknown sources blocked
- User account management disabled

### Kiosk Mode
- Lock task mode enabled
- Device locked to specific apps
- Home button disabled when locked
- Recent apps disabled when locked

### Data Protection
- Device code stored in encrypted SharedPreferences
- API keys obfuscated in ProGuard
- HTTPS only for all API calls

## Backend Security

### Web3 Listener
- Service role key stored in environment variables
- Read-only access to blockchain
- Write-only access to payment_transactions table
- Transaction validation before processing

### API Endpoints
- Rate limiting: 100 req/min per IP
- Input validation on all endpoints
- SQL injection prevention (Supabase handles this)
- CORS configured for dashboard domain only

## Dashboard Security

### Authentication
- Email/password authentication
- Multi-factor authentication (optional)
- Session timeout: 1 hour
- Automatic logout on inactivity

### Authorization
- Role-based access control
- Super Admin: Full access
- Administrator: Limited to own devices/customers
- No public access without authentication

### Data Handling
- No sensitive data in localStorage
- Tokens stored in httpOnly cookies
- HTTPS enforced in production
- CSP headers configured

## Best Practices

### For Developers
1. Never commit `.env` files
2. Use environment variables for all secrets
3. Keep dependencies updated
4. Run security audits regularly
5. Use HTTPS in all environments

### For Administrators
1. Use strong passwords (min 12 characters)
2. Enable 2FA if available
3. Don't share login credentials
4. Log out after each session
5. Report suspicious activity immediately

### For Deployment
1. Use separate environments (dev, staging, prod)
2. Rotate API keys every 90 days
3. Monitor logs for suspicious activity
4. Keep backups of database
5. Test disaster recovery procedures

## Incident Response

### If Device is Compromised
1. Mark device as "defaulted" in dashboard
2. Device will remain locked
3. Investigate how compromise occurred
4. Update security measures if needed

### If Admin Account is Compromised
1. Immediately disable the account
2. Rotate all API keys
3. Audit all actions by that account
4. Reset password and re-enable with 2FA

### If Database is Compromised
1. Immediately rotate all keys
2. Review RLS policies
3. Audit all data access
4. Notify affected users
5. Implement additional security measures

## Compliance

### Data Protection
- Customer data encrypted at rest
- PII handled according to local regulations
- Data retention policy: 7 years
- Right to deletion honored within 30 days

### Audit Trail
- All actions logged in activity_logs table
- Logs retained for 1 year
- Regular security audits conducted
- Penetration testing annually

## Reporting Security Issues

If you discover a security vulnerability:
1. Do NOT disclose publicly
2. Email: security@edenservices.ke
3. Include detailed description
4. We will respond within 48 hours
5. Fix will be deployed within 7 days

## Security Checklist

Before going to production:

- [ ] All RLS policies tested
- [ ] API keys rotated
- [ ] HTTPS enforced everywhere
- [ ] Rate limiting configured
- [ ] Monitoring and alerts set up
- [ ] Backup and recovery tested
- [ ] Security audit completed
- [ ] Incident response plan documented
- [ ] Team trained on security procedures
