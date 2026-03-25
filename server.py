import os
from flask import Flask, jsonify, render_template, request, send_from_directory
from flask_cors import CORS
from supabase import create_client
import logging
import hashlib
import secrets
from datetime import datetime

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__, template_folder='templates', static_folder='static')
app.secret_key = os.getenv("SECRET_KEY", "eden-secret-key")

# Configure CORS to allow all origins
CORS(app, resources={
    r"/*": {
        "origins": "*",
        "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
        "allow_headers": ["Content-Type", "Authorization"]
    }
})

SUPABASE_URL = os.getenv("SUPABASE_URL", "https://fvkjeteywfcppbtovbiv.supabase.co")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_KEY", os.getenv("SUPABASE_ANON_KEY", ""))

try:
    supabase = create_client(SUPABASE_URL, SUPABASE_KEY)
    logger.info("Supabase client initialized successfully")
except Exception as e:
    logger.error(f"Failed to initialize Supabase client: {e}")
    supabase = None

# Helper functions
def hash_password(password):
    return hashlib.sha256(password.encode()).hexdigest()

def generate_token():
    return secrets.token_urlsafe(32)

def format_phone_number(phone):
    """Convert phone numbers to consistent +254 format"""
    if not phone:
        return phone
    
    # Convert to string and strip whitespace
    phone = str(phone).strip()
    
    # Remove all non-digits first
    digits = ''.join(filter(str.isdigit, phone))
    
    # Handle different input formats
    if not digits:
        return phone  # Return original if no digits found
    
    # Format based on digit patterns
    if digits.startswith("254"):
        # Already in 254 format
        if len(digits) >= 12:
            return f"+{digits[:12]}"  # Limit to 12 digits (254 + 9 digits)
        else:
            return f"+{digits}"
    elif digits.startswith("07"):
        # Kenyan format starting with 07
        if len(digits) >= 10:
            return f"+254{digits[1:10]}"  # Remove the 0, take next 9 digits
        else:
            return f"+254{digits[1:]}"
    elif digits.startswith("7") and len(digits) >= 9:
        # Format starting with 7 (without 0)
        return f"+254{digits[:9]}"  # Take first 9 digits
    elif len(digits) >= 9:
        # Assume it's a Kenyan number without country code
        return f"+254{digits[:9]}"
    else:
        # Too short, return with + if it looks like it should have one
        if phone.startswith("+"):
            return phone
        else:
            return f"+254{digits}"

def verify_admin_token(token):
    """Verify admin authentication token"""
    if not token:
        return None
    
    try:
        response = supabase.table("admins").select("*").eq("token", token).execute()
        if response.data and len(response.data) > 0:
            return response.data[0]
        return None
    except Exception as e:
        logger.error(f"Token verification error: {e}")
        return None

# ============================================
# WEB ROUTES
# ============================================

@app.route("/")
def index():
    return render_template("index.html")

@app.route("/register")
def register_page():
    return render_template("register.html")

@app.route("/debug")
def debug_page():
    return render_template("debug.html")

@app.route("/login")
def login():
    return render_template("login.html")

@app.route("/api/register", methods=["POST"])
def api_register():
    try:
        if supabase is None:
            return jsonify({"success": False, "error": "Database connection not available"}), 500
        
        data = request.json
        if not data:
            return jsonify({"success": False, "error": "No data provided"}), 400
            
        email = data.get("email")
        password = data.get("password")
        full_name = data.get("full_name")
        
        if not email or not password:
            return jsonify({"success": False, "error": "Email and password required"}), 400
        
        if len(password) < 6:
            return jsonify({"success": False, "error": "Password must be at least 6 characters"}), 400
        
        logger.info(f"Registration attempt for email: {email}")
        
        # Check if admin already exists
        existing_admin = supabase.table("admins").select("*").eq("email", email).execute()
        if existing_admin.data and len(existing_admin.data) > 0:
            return jsonify({"success": False, "error": "Account already exists"}), 400
        
        password_hash = hash_password(password)
        
        # Create admin account (trigger will automatically assign super_admin role to sammyselth260@gmail.com)
        admin_data = {
            "email": email,
            "password_hash": password_hash,
            "full_name": full_name,
            "role": "admin"  # Will be overridden by trigger for sammyselth260@gmail.com
        }
        
        response = supabase.table("admins").insert(admin_data).execute()
        
        if response.data and len(response.data) > 0:
            admin = response.data[0]
            logger.info(f"Registration successful for: {email} with role: {admin['role']}")
            
            return jsonify({
                "success": True,
                "message": "Account created successfully",
                "role": admin["role"],
                "is_super_admin": admin["role"] == "super_admin"
            })
        else:
            return jsonify({"success": False, "error": "Failed to create account"}), 500
            
    except Exception as e:
        logger.error(f"Registration error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/dashboard")
def dashboard():
    return render_template("dashboard.html")

@app.route("/admin")
def admin():
    return render_template("admin.html")

@app.route("/super-admin")
def super_admin():
    return render_template("super_admin.html")

@app.route("/customer-login")
def customer_login():
    return render_template("customer_login.html")

@app.route("/customer-dashboard")
def customer_dashboard_page():
    return render_template("customer_dashboard.html")

@app.route("/change-password")
def change_password_page():
    return render_template("change_password.html")

@app.route("/change-pin")
def change_pin_page():
    return render_template("change_pin.html")

# ============================================
# API ROUTES - AUTHENTICATION
# ============================================

@app.route("/api/login", methods=["POST"])
def api_login():
    try:
        # Check if supabase is initialized
        if supabase is None:
            logger.error("Supabase client not initialized")
            return jsonify({"success": False, "error": "Database connection not available"}), 500
        
        data = request.json
        if not data:
            logger.error("No data provided in login request")
            return jsonify({"success": False, "error": "No data provided"}), 400
            
        username = data.get("username")
        password = data.get("password")
        
        if not username or not password:
            logger.error("Missing username or password")
            return jsonify({"success": False, "error": "Username and password required"}), 400
        
        logger.info(f"Login attempt for username: {username}")
        
        password_hash = hash_password(password)
        logger.info(f"Password hash generated for: {username}")
        
        try:
            # Query using email instead of username for new auth system
            response = supabase.table("admins").select("*").eq("email", username).eq("password_hash", password_hash).execute()
            logger.info(f"Database query executed for: {username}")
            
            if response.data and len(response.data) > 0:
                admin = response.data[0]
                token = generate_token()
                
                # Update last login and token (handle missing columns gracefully)
                update_data = {"token": token}
                
                try:
                    supabase.table("admins").select("last_login").limit(1).execute()
                    update_data["last_login"] = "now()"
                except:
                    logger.info("last_login column not found in admins table, skipping")
                
                supabase.table("admins").update(update_data).eq("id", admin["id"]).execute()
                
                logger.info(f"Login successful for: {username}")
                
                # Check if admin must change password (always false for new system)
                must_change = admin.get("must_change_password", False)
                
                return jsonify({
                    "success": True,
                    "token": token,
                    "role": admin["role"],
                    "admin_id": admin["id"],
                    "must_change_password": must_change
                })
            else:
                logger.warning(f"Invalid credentials for: {username}")
                return jsonify({"success": False, "error": "Invalid credentials"}), 401
                
        except Exception as db_error:
            logger.error(f"Database error during login: {db_error}")
            # Check if it's a table doesn't exist error
            if "relation \"admins\" does not exist" in str(db_error):
                return jsonify({
                    "success": False, 
                    "error": "Database not set up. Please run the database setup script first."
                }), 500
            return jsonify({"success": False, "error": f"Database error: {str(db_error)}"}), 500
            
    except Exception as e:
        logger.error(f"Login error: {e}")
        return jsonify({"success": False, "error": f"Server error: {str(e)}"}), 500

# ============================================
# API ROUTES - DEVICES
# ============================================

@app.route("/api/devices", methods=["GET"])
def get_devices():
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            logger.warning("Unauthorized devices access attempt")
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        logger.info(f"Admin {admin.get('email', 'unknown')} requesting devices list")
        
        # Get devices with error handling for missing columns
        try:
            response = supabase.table("devices").select("*").execute()
            devices = response.data if response.data else []
            
            # Ensure all devices have required fields with defaults
            processed_devices = []
            for device in devices:
                processed_device = {
                    "device_id": device.get("device_id", "Unknown"),
                    "customer_id": device.get("customer_id", device.get("national_id", "Unknown")),
                    "customer_name": device.get("customer_name", "Unknown"),
                    "customer_phone": device.get("customer_phone", "Unknown"),
                    "total_amount": float(device.get("total_amount", 0)),
                    "daily_payment_amount": float(device.get("daily_payment_amount", 80)),
                    "amount_paid": float(device.get("amount_paid", 0)),
                    "downpayment": float(device.get("downpayment", 0)),
                    "payment_balance": float(device.get("payment_balance", 0)),
                    "status": device.get("status", "unknown"),
                    "is_locked": device.get("is_locked", True),
                    "created_at": device.get("created_at", ""),
                    "serial_number": device.get("serial_number", ""),
                    "imei": device.get("imei", ""),
                    "last_payment_date": device.get("last_payment_date"),
                    "last_payment_amount": float(device.get("last_payment_amount", 0)),
                    "unlock_until": device.get("unlock_until"),
                    "trial_period": device.get("trial_period", False)
                }
                processed_devices.append(processed_device)
            
            logger.info(f"Successfully retrieved {len(processed_devices)} devices")
            return jsonify(processed_devices)
            
        except Exception as db_error:
            logger.error(f"Database error retrieving devices: {db_error}")
            return jsonify({"success": False, "error": f"Database error: {str(db_error)}"}), 500
        
    except Exception as e:
        logger.error(f"Get devices error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/devices/<device_id>", methods=["GET"])
def get_device(device_id):
    try:
        response = supabase.table("devices").select("*").eq("device_id", device_id).execute()
        
        if response.data and len(response.data) > 0:
            return jsonify(response.data[0])
        else:
            return jsonify({"error": "Device not found"}), 404
    except Exception as e:
        logger.error(f"Get device error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/api/devices/<device_id>/status", methods=["GET"])
def get_device_status(device_id):
    try:
        response = supabase.table("devices").select("is_locked, status").eq("device_id", device_id).execute()
        
        if response.data and len(response.data) > 0:
            return jsonify(response.data[0])
        else:
            return jsonify({"is_locked": False, "status": "unknown"}), 404
    except Exception as e:
        logger.error(f"Get device status error: {e}")
        return jsonify({"is_locked": False, "status": "error"}), 500

@app.route("/api/devices/<device_id>/lock", methods=["POST"])
def lock_device(device_id):
    """Lock a device remotely with complete lockdown"""
    try:
        logger.info(f"Lock device request for device_id: {device_id}")
        
        # Get and verify admin token
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        logger.info(f"Admin token received: {token[:10] if token else 'None'}...")
        
        admin = verify_admin_token(token)
        
        if not admin:
            logger.warning(f"Unauthorized lock attempt for device {device_id}")
            return jsonify({"success": False, "error": "Unauthorized access"}), 403
        
        logger.info(f"Admin verified: {admin.get('email', 'unknown')}")
        
        # Get request data
        data = request.json or {}
        lock_reason = data.get("lock_reason", "ADMIN_LOCKED")
        
        logger.info(f"Locking device {device_id} with reason: {lock_reason}")
        
        # Check if device exists first
        try:
            device_check = supabase.table("devices").select("device_id, status").eq("device_id", device_id).execute()
            if not device_check.data:
                logger.error(f"Device {device_id} not found")
                return jsonify({"success": False, "error": "Device not found"}), 404
        except Exception as check_error:
            logger.error(f"Error checking device existence: {check_error}")
            return jsonify({"success": False, "error": "Database error"}), 500
        
        # Update device status in database with bulletproof error handling
        # Try with all columns first, then fallback to minimal columns
        logger.info(f"Attempting to lock device {device_id}")
        
        update_response = None
        
        # Attempt 1: Try with all columns
        try:
            update_data = {
                "status": "locked",
                "is_locked": True,
                "lock_reason": lock_reason,
                "locked_by": str(admin.get("id", "unknown")),
                "locked_at": datetime.now().isoformat()
            }
            logger.info(f"Attempt 1: Updating with all columns: {update_data}")
            update_response = supabase.table("devices").update(update_data).eq("device_id", device_id).execute()
            logger.info(f"Attempt 1: Success with all columns")
        except Exception as e1:
            logger.warning(f"Attempt 1 failed (missing columns): {e1}")
            
            # Attempt 2: Try with is_locked column
            try:
                update_data = {
                    "status": "locked",
                    "is_locked": True
                }
                logger.info(f"Attempt 2: Updating with is_locked: {update_data}")
                update_response = supabase.table("devices").update(update_data).eq("device_id", device_id).execute()
                logger.info(f"Attempt 2: Success with is_locked")
            except Exception as e2:
                logger.warning(f"Attempt 2 failed (is_locked missing): {e2}")
                
                # Attempt 3: Try with only status column (absolute minimum)
                try:
                    update_data = {
                        "status": "locked"
                    }
                    logger.info(f"Attempt 3: Updating with status only: {update_data}")
                    update_response = supabase.table("devices").update(update_data).eq("device_id", device_id).execute()
                    logger.info(f"Attempt 3: Success with status only")
                except Exception as e3:
                    logger.error(f"Attempt 3 failed (all attempts exhausted): {e3}")
                    return jsonify({
                        "success": False, 
                        "error": "Database schema missing required columns. Please run COMPLETE_PAYMENT_SYSTEM_FIX.sql"
                    }), 500
        
        try:
            
            if not update_response.data:
                logger.error(f"Failed to update device {device_id} in database - no data returned")
                return jsonify({"success": False, "error": "Failed to update device status"}), 500
            
            logger.info(f"Device {device_id} successfully locked in database")
            
        except Exception as update_error:
            logger.error(f"Database update error for device {device_id}: {update_error}")
            return jsonify({"success": False, "error": f"Database update failed: {str(update_error)}"}), 500
        
        # Send complete lockdown command to device (optional, don't fail if this fails)
        try:
            send_device_notification(
                device_id, 
                None,  # Will be filled from device record
                "🔒 DEVICE LOCKED - COMPLETE LOCKDOWN", 
                f"""
DEVICE COMPLETELY LOCKED BY ADMINISTRATOR

⚠️ ALL FUNCTIONS DISABLED:
• Phone calls blocked
• SMS messaging blocked  
• All apps hidden except Eden
• Settings access blocked
• System functions disabled

Reason: {lock_reason}
Locked by: Admin
Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

Contact administrator to unlock this device.
                """.strip(),
                "admin_lock"
            )
            logger.info(f"Lock notification sent to device {device_id}")
        except Exception as notification_error:
            logger.warning(f"Failed to send lock notification to device {device_id}: {notification_error}")
            # Continue even if notification fails
        
        return jsonify({
            "success": True, 
            "message": "Device locked with complete lockdown", 
            "is_locked": True,
            "lockdown_level": "COMPLETE",
            "device_id": device_id,
            "locked_by": admin.get("email", "unknown"),
            "lock_reason": lock_reason
        })
        
    except Exception as e:
        logger.error(f"Lock device error for {device_id}: {e}")
        import traceback
        logger.error(f"Lock device traceback: {traceback.format_exc()}")
        return jsonify({"success": False, "error": f"Lock device failed: {str(e)}"}), 500

@app.route("/api/devices/<device_id>/unlock", methods=["POST"])
def unlock_device(device_id):
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        # Bulletproof unlock with fallback attempts
        logger.info(f"Attempting to unlock device {device_id}")
        
        update_response = None
        
        # Attempt 1: Try with all columns
        try:
            update_data = {
                "status": "active",
                "is_locked": False,
                "lock_reason": None,
                "locked_by": None,
                "locked_at": None
            }
            logger.info(f"Attempt 1: Unlocking with all columns")
            update_response = supabase.table("devices").update(update_data).eq("device_id", device_id).execute()
            logger.info(f"Attempt 1: Success with all columns")
        except Exception as e1:
            logger.warning(f"Attempt 1 failed: {e1}")
            
            # Attempt 2: Try with is_locked column
            try:
                update_data = {
                    "status": "active",
                    "is_locked": False
                }
                logger.info(f"Attempt 2: Unlocking with is_locked")
                update_response = supabase.table("devices").update(update_data).eq("device_id", device_id).execute()
                logger.info(f"Attempt 2: Success with is_locked")
            except Exception as e2:
                logger.warning(f"Attempt 2 failed: {e2}")
                
                # Attempt 3: Try with only status column
                try:
                    update_data = {
                        "status": "active"
                    }
                    logger.info(f"Attempt 3: Unlocking with status only")
                    update_response = supabase.table("devices").update(update_data).eq("device_id", device_id).execute()
                    logger.info(f"Attempt 3: Success with status only")
                except Exception as e3:
                    logger.error(f"All unlock attempts failed: {e3}")
                    return jsonify({
                        "success": False, 
                        "error": "Database schema missing required columns. Please run COMPLETE_PAYMENT_SYSTEM_FIX.sql"
                    }), 500
        
        # Send unlock command to device
        try:
            send_device_notification(
                device_id,
                None,  # Will be filled from device record
                "🔓 DEVICE UNLOCKED - ACCESS RESTORED",
                f"""
DEVICE UNLOCKED BY ADMINISTRATOR

✅ ALL FUNCTIONS RESTORED:
• Phone calls enabled
• SMS messaging enabled
• All apps accessible
• Settings access restored
• System functions enabled

Unlocked by: Admin
Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

Your device is now fully functional.
            """.strip(),
                "admin_unlock"
            )
        except Exception as notif_error:
            logger.warning(f"Failed to send unlock notification: {notif_error}")
        
        return jsonify({
            "success": True, 
            "message": "Device unlocked - all functions restored", 
            "is_locked": False,
            "lockdown_level": "NONE"
        })
    except Exception as e:
        logger.error(f"Unlock device error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

# ============================================
# PAYMENT SYSTEM WITH AUTO-UNLOCK
# ============================================

@app.route("/api/customer/make-payment", methods=["POST"])
def make_payment():
    """Customer makes a payment with new logic: downpayment as deposit, partial payments for 12h unlock"""
    try:
        data = request.json
        phone = format_phone_number(data.get("phone_number"))
        payment_amount = float(data.get("payment_amount", 0))
        
        if not phone or payment_amount <= 0:
            return jsonify({"success": False, "error": "Phone number and valid payment amount required"}), 400
        
        logger.info(f"Processing payment: {payment_amount} for phone {phone}")
        
        # Get device record
        device_response = supabase.table("devices").select("*").eq("customer_phone", phone).execute()
        
        if not device_response.data:
            return jsonify({"success": False, "error": "Device not found"}), 404
        
        device = device_response.data[0]
        
        # Calculate current amounts
        total_amount = float(device.get("total_amount", 0))
        current_paid = float(device.get("amount_paid", 0))  # This includes downpayment (deposit)
        daily_payment = float(device.get("daily_payment_amount", 80))
        current_payment_balance = float(device.get("payment_balance", 0))  # Only actual payments, not downpayment
        downpayment = float(device.get("downpayment", 0))  # Separate downpayment tracking
        
        if daily_payment <= 0:
            return jsonify({"success": False, "error": "Daily payment amount not set. Contact administrator."}), 400
        
        # Add payment to totals (this goes to loan repayment)
        new_amount_paid = current_paid + payment_amount
        remaining_loan_balance = total_amount - new_amount_paid
        
        # Add to payment balance (this is for device unlock, separate from downpayment)
        new_payment_balance = current_payment_balance + payment_amount
        
        # Calculate unlock logic
        half_daily_plus_10 = (daily_payment / 2) + 10
        unlock_type = "none"
        unlock_duration_hours = 0
        unlock_until = None
        
        if payment_amount >= daily_payment:
            # Full daily payment = full day unlock
            days_purchased = payment_amount / daily_payment
            unlock_type = "full_days"
            unlock_duration_hours = days_purchased * 24
        elif payment_amount >= half_daily_plus_10:
            # Partial payment (half + 10) = 12 hours unlock
            unlock_type = "partial_12h"
            unlock_duration_hours = 12
        
        should_unlock = unlock_duration_hours > 0
        
        # Calculate unlock_until timestamp
        if should_unlock:
            from datetime import datetime, timedelta
            unlock_until = datetime.now() + timedelta(hours=unlock_duration_hours)
        
        # Update device with new payment
        update_data = {
            "amount_paid": new_amount_paid,
            "payment_balance": new_payment_balance,
            "last_payment_date": "now()",
            "last_payment_amount": payment_amount
        }
        
        # If payment is sufficient, unlock the device
        if should_unlock:
            update_data.update({
                "status": "active",
                "is_locked": False,
                "lock_reason": None,
                "unlocked_at": "now()",
                "unlock_until": unlock_until.isoformat() if unlock_until else None
            })
        
        # Record the payment in payments table
        try:
            payment_record = {
                "customer_phone": phone,
                "amount": payment_amount,
                "payment_date": "now()",
                "payment_method": data.get("payment_method", "mobile_money"),
                "reference": data.get("reference", f"PAY_{int(time.time())}"),
                "unlock_type": unlock_type,
                "unlock_hours": unlock_duration_hours,
                "device_unlocked": should_unlock,
                "status": "completed"
            }
            supabase.table("payments").insert(payment_record).execute()
            logger.info(f"Payment recorded in payments table")
        except Exception as payment_error:
            logger.warning(f"Could not record in payments table: {payment_error}")
            # Continue even if payments table doesn't exist
        
        # Update device record
        supabase.table("devices").update(update_data).eq("customer_phone", phone).execute()
        
        # Send unlock notification if device should be unlocked
        unlock_message = ""
        if should_unlock:
            if unlock_type == "full_days":
                days = int(unlock_duration_hours / 24)
                hours = int(unlock_duration_hours % 24)
                if days > 0:
                    unlock_message = f"Device unlocked for {days} day{'s' if days != 1 else ''}"
                    if hours > 0:
                        unlock_message += f" and {hours} hour{'s' if hours != 1 else ''}"
                else:
                    unlock_message = f"Device unlocked for {hours} hour{'s' if hours != 1 else ''}"
            else:  # partial_12h
                unlock_message = "Device unlocked for 12 hours (partial payment)"
            
            try:
                send_device_notification(
                    device.get("device_id"),
                    phone,
                    "💰 PAYMENT RECEIVED - DEVICE UNLOCKED",
                    f"""
PAYMENT SUCCESSFULLY PROCESSED

💰 Payment: KES {payment_amount:,.2f}
⏰ Unlock Duration: {unlock_message}
💳 Total Paid: KES {new_amount_paid:,.2f}
💵 Loan Remaining: KES {remaining_loan_balance:,.2f}
💰 Payment Balance: KES {new_payment_balance:,.2f}

✅ {unlock_message.upper()}
Your device is now functional.

Thank you for your payment!
                    """.strip(),
                    "payment_unlock"
                )
            except Exception as notification_error:
                logger.warning(f"Failed to send unlock notification: {notification_error}")
        else:
            unlock_message = f"Payment received but insufficient for unlock. Need KES {half_daily_plus_10:,.2f} minimum."
        
        return jsonify({
            "success": True,
            "message": "Payment processed successfully",
            "payment_amount": payment_amount,
            "unlock_type": unlock_type,
            "unlock_hours": unlock_duration_hours,
            "total_paid": new_amount_paid,
            "remaining_loan_balance": remaining_loan_balance,
            "payment_balance": new_payment_balance,
            "device_unlocked": should_unlock,
            "unlock_message": unlock_message,
            "daily_payment_amount": daily_payment,
            "minimum_partial_payment": half_daily_plus_10,
            "unlock_until": unlock_until.isoformat() if unlock_until else None
        })
        
    except Exception as e:
        logger.error(f"Payment processing error: {e}")
        return jsonify({"success": False, "error": f"Payment failed: {str(e)}"}), 500

@app.route("/api/customer/unlock-now", methods=["POST"])
def unlock_now():
    """Customer manually requests device unlock after payment"""
    try:
        data = request.json
        phone = format_phone_number(data.get("phone_number"))
        
        if not phone:
            return jsonify({"success": False, "error": "Phone number required"}), 400
        
        logger.info(f"Manual unlock request for phone {phone}")
        
        # Get device record
        device_response = supabase.table("devices").select("*").eq("customer_phone", phone).execute()
        
        if not device_response.data:
            return jsonify({"success": False, "error": "Device not found"}), 404
        
        device = device_response.data[0]
        
        # Calculate payment status
        total_amount = float(device.get("total_amount", 0))
        amount_paid = float(device.get("amount_paid", 0))
        daily_payment = float(device.get("daily_payment_amount", 80))
        payment_balance = float(device.get("payment_balance", 0))
        remaining_loan_balance = total_amount - amount_paid
        
        # Check if customer has sufficient payment balance for at least one day
        days_available = payment_balance / daily_payment if daily_payment > 0 else 0
        
        if days_available < 1.0:
            return jsonify({
                "success": False,
                "error": f"Insufficient payment balance. Need KES {daily_payment:,.2f} for one day unlock.",
                "payment_balance": payment_balance,
                "daily_payment_required": daily_payment,
                "days_available": round(days_available, 1),
                "remaining_loan_balance": remaining_loan_balance
            }), 400
        
        # Check if device is already unlocked
        if not device.get("is_locked", True):
            return jsonify({
                "success": True,
                "message": "Device is already unlocked",
                "device_status": "unlocked",
                "days_remaining": int(days_available)
            })
        
        # Calculate unlock duration
        from datetime import datetime, timedelta
        unlock_until = datetime.now() + timedelta(days=int(days_available))
        
        # Unlock the device
        supabase.table("devices").update({
            "status": "active",
            "is_locked": False,
            "lock_reason": None,
            "unlocked_at": "now()",
            "unlock_until": unlock_until.isoformat(),
            "manual_unlock_requested": True
        }).eq("customer_phone", phone).execute()
        
        # Send unlock notification
        days_remaining = int(days_available)
        hours_remaining = int((days_available - days_remaining) * 24)
        
        unlock_duration = f"{days_remaining} day{'s' if days_remaining != 1 else ''}"
        if hours_remaining > 0:
            unlock_duration += f" and {hours_remaining} hour{'s' if hours_remaining != 1 else ''}"
        
        try:
            send_device_notification(
                device.get("device_id"),
                phone,
                "🔓 MANUAL UNLOCK - DEVICE ACTIVATED",
                f"""
DEVICE MANUALLY UNLOCKED

✅ Status: Payment verified
💰 Payment Balance: KES {payment_balance:,.2f}
📅 Unlocked for: {unlock_duration}
💵 Loan Remaining: KES {remaining_loan_balance:,.2f}

Your device is now fully functional.
                """.strip(),
                "manual_unlock"
            )
        except Exception as notification_error:
            logger.warning(f"Failed to send unlock notification: {notification_error}")
        
        return jsonify({
            "success": True,
            "message": f"Device unlocked for {unlock_duration}",
            "payment_balance": payment_balance,
            "days_unlocked": int(days_available),
            "remaining_loan_balance": remaining_loan_balance,
            "device_status": "unlocked",
            "unlock_until": unlock_until.isoformat()
        })
        
    except Exception as e:
        logger.error(f"Manual unlock error: {e}")
        return jsonify({"success": False, "error": f"Unlock failed: {str(e)}"}), 500

@app.route("/api/customer/payment-status", methods=["GET"])
def payment_status():
    """Get customer payment status and device unlock eligibility"""
    try:
        phone = format_phone_number(request.args.get("phone"))
        
        if not phone:
            return jsonify({"success": False, "error": "Phone number required"}), 400
        
        # Get device record
        device_response = supabase.table("devices").select("*").eq("customer_phone", phone).execute()
        
        if not device_response.data:
            return jsonify({"success": False, "error": "Device not found"}), 404
        
        device = device_response.data[0]
        
        # Calculate payment status
        total_amount = float(device.get("total_amount", 0))
        amount_paid = float(device.get("amount_paid", 0))
        daily_payment = float(device.get("daily_payment_amount", 80))
        payment_balance = float(device.get("payment_balance", 0))
        remaining_loan_balance = total_amount - amount_paid
        
        # Calculate unlock eligibility
        days_available = payment_balance / daily_payment if daily_payment > 0 else 0
        can_unlock = days_available >= 1.0
        is_locked = device.get("is_locked", True)
        
        # Check if unlock period has expired
        unlock_until = device.get("unlock_until")
        unlock_expired = False
        if unlock_until:
            from datetime import datetime
            try:
                unlock_until_dt = datetime.fromisoformat(unlock_until.replace('Z', '+00:00'))
                unlock_expired = datetime.now() > unlock_until_dt
            except:
                unlock_expired = False
        
        return jsonify({
            "success": True,
            "total_amount": total_amount,
            "amount_paid": amount_paid,
            "remaining_loan_balance": remaining_loan_balance,
            "daily_payment_amount": daily_payment,
            "payment_balance": payment_balance,
            "days_available": round(days_available, 1),
            "can_unlock": can_unlock,
            "is_locked": is_locked,
            "device_status": device.get("status", "unknown"),
            "last_payment_date": device.get("last_payment_date"),
            "last_payment_amount": device.get("last_payment_amount", 0),
            "unlock_until": unlock_until,
            "unlock_expired": unlock_expired
        })
        
    except Exception as e:
        logger.error(f"Payment status error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/customer/payment-history", methods=["GET"])
def payment_history():
    """Get customer's last 10 payment transactions"""
    try:
        phone = format_phone_number(request.args.get("phone"))
        
        if not phone:
            return jsonify({"success": False, "error": "Phone number required"}), 400
        
        # Get last 10 payments for this customer
        try:
            payments_response = supabase.table("payments").select("*").eq("customer_phone", phone).order("payment_date", desc=True).limit(10).execute()
            payments = payments_response.data if payments_response.data else []
            
            # Format payment data for display
            formatted_payments = []
            for payment in payments:
                formatted_payment = {
                    "id": payment.get("id"),
                    "amount": float(payment.get("amount", 0)),
                    "payment_date": payment.get("payment_date"),
                    "payment_method": payment.get("payment_method", "mobile_money"),
                    "reference": payment.get("reference", ""),
                    "unlock_type": payment.get("unlock_type", "none"),
                    "unlock_hours": float(payment.get("unlock_hours", 0)),
                    "device_unlocked": payment.get("device_unlocked", False),
                    "status": payment.get("status", "completed")
                }
                formatted_payments.append(formatted_payment)
            
            return jsonify({
                "success": True,
                "payments": formatted_payments,
                "total_payments": len(formatted_payments)
            })
            
        except Exception as payments_error:
            logger.warning(f"Could not fetch payments: {payments_error}")
            return jsonify({
                "success": True,
                "payments": [],
                "total_payments": 0,
                "message": "Payment history not available"
            })
        
    except Exception as e:
        logger.error(f"Payment history error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

# ============================================
# API ROUTES - ADVANCED ADMIN FEATURES
# ============================================

@app.route("/api/admin/update-loan-balance", methods=["POST"])
def update_loan_balance():
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        data = request.json
        device_id = data.get("device_id")
        new_total = data.get("total_amount")
        new_paid = data.get("amount_paid")
        
        if not device_id or new_total is None or new_paid is None:
            return jsonify({"success": False, "error": "Missing required fields"}), 400
        
        # Get current values for logging
        current_device = supabase.table("devices").select("*").eq("device_id", device_id).execute()
        if not current_device.data:
            return jsonify({"success": False, "error": "Device not found"}), 404
        
        old_values = current_device.data[0]
        
        # Update loan balance
        update_data = {
            "total_amount": float(new_total),
            "amount_paid": float(new_paid),
            "updated_at": "now()"
        }
        
        supabase.table("devices").update(update_data).eq("device_id", device_id).execute()
        
        # Log the action
        log_device_action(device_id, "LOAN_BALANCE_UPDATE", admin["id"], {
            "old_total": old_values.get("total_amount"),
            "old_paid": old_values.get("amount_paid"),
            "new_total": new_total,
            "new_paid": new_paid
        })
        
        return jsonify({"success": True, "message": "Loan balance updated successfully"})
        
    except Exception as e:
        logger.error(f"Update loan balance error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/admin/delete-device", methods=["POST"])
def delete_device():
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        data = request.json
        device_id = data.get("device_id")
        
        if not device_id:
            return jsonify({"success": False, "error": "Device ID required"}), 400
        
        # Get device info before deletion
        device_response = supabase.table("devices").select("*").eq("device_id", device_id).execute()
        if not device_response.data:
            return jsonify({"success": False, "error": "Device not found"}), 404
        
        device = device_response.data[0]
        
        # Mark device as deleted (soft delete)
        supabase.table("devices").update({
            "is_deleted": True,
            "deleted_at": "now()",
            "deleted_by": admin["id"],
            "status": "deleted"
        }).eq("device_id", device_id).execute()
        
        # Log the deletion
        log_device_action(device_id, "DEVICE_DELETED", admin["id"], {
            "customer_name": device.get("customer_name"),
            "customer_phone": device.get("customer_phone"),
            "reason": "Admin deletion"
        })
        
        # Send uninstall command to device (this will trigger app removal)
        send_device_notification(device_id, device.get("customer_phone"), 
                                "Device Removed", 
                                "This device has been removed from the system. The Eden app will be uninstalled automatically.",
                                "system")
        
        return jsonify({"success": True, "message": "Device deleted successfully. App will be uninstalled from device."})
        
    except Exception as e:
        logger.error(f"Delete device error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/admin/security-violations", methods=["GET"])
def get_security_violations():
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        # Get recent security violations
        violations = supabase.table("security_violations").select("*").order("created_at", desc=True).limit(50).execute()
        
        return jsonify({"success": True, "violations": violations.data})
        
    except Exception as e:
        logger.error(f"Get security violations error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/admin/send-security-warning", methods=["POST"])
def send_security_warning():
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        data = request.json
        device_id = data.get("device_id")
        customer_phone = data.get("customer_phone")
        
        if not device_id and not customer_phone:
            return jsonify({"success": False, "error": "Device ID or customer phone required"}), 400
        
        # Send security warning notification
        title = "⚠️ SECURITY VIOLATION DETECTED"
        message = """
ILLEGAL ACTIVITY DETECTED

We have detected unauthorized attempts to factory reset or tamper with this device. 

⚠️ WARNING: Such activities are ILLEGAL and may result in:
• Criminal charges for device tampering
• Immediate legal action
• Device confiscation
• Additional penalties

This device is legally protected under our financing agreement. Any attempts to bypass security measures violate the terms of service and applicable laws.

If you have payment issues, contact our support team immediately instead of attempting to tamper with the device.

STOP IMMEDIATELY - You are being monitored.
        """.strip()
        
        send_device_notification(device_id, customer_phone, title, message, "security_warning")
        
        # Mark violation as notified
        if device_id:
            supabase.table("security_violations").update({
                "admin_notified": True,
                "notification_sent_at": "now()"
            }).eq("device_id", device_id).execute()
        
        return jsonify({"success": True, "message": "Security warning sent successfully"})
        
    except Exception as e:
        logger.error(f"Send security warning error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/admin/promote-admin", methods=["POST"])
def promote_admin():
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin or admin["role"] != "super_admin":
            return jsonify({"success": False, "error": "Only super admins can promote users"}), 403
        
        data = request.json
        target_admin_id = data.get("admin_id")
        new_role = data.get("new_role")
        
        if not target_admin_id or not new_role:
            return jsonify({"success": False, "error": "Admin ID and new role required"}), 400
        
        if new_role not in ["admin", "super_admin"]:
            return jsonify({"success": False, "error": "Invalid role"}), 400
        
        # Update admin role
        supabase.table("admins").update({
            "role": new_role
        }).eq("id", target_admin_id).execute()
        
        return jsonify({"success": True, "message": f"Admin promoted to {new_role} successfully"})
        
    except Exception as e:
        logger.error(f"Promote admin error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/admin/create-admin", methods=["POST"])
def create_admin():
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin or admin["role"] != "super_admin":
            return jsonify({"success": False, "error": "Only super admins can create admins"}), 403
        
        data = request.json
        username = data.get("username")
        password = data.get("password")
        full_name = data.get("full_name")
        email = data.get("email")
        
        if not username or not password:
            return jsonify({"success": False, "error": "Username and password required"}), 400
        
        password_hash = hash_password(password)
        
        # Create new admin
        new_admin = {
            "email": email,
            "password_hash": password_hash,
            "role": "admin",
            "full_name": full_name,
            "created_by": admin["id"],
            "must_change_password": True
        }
        
        response = supabase.table("admins").insert(new_admin).execute()
        
        if response.data:
            return jsonify({"success": True, "message": "Admin created successfully"})
        else:
            return jsonify({"success": False, "error": "Failed to create admin"}), 500
        
    except Exception as e:
        logger.error(f"Create admin error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

# Helper functions
def log_device_action(device_id, action, admin_id, details):
    try:
        log_entry = {
            "device_id": device_id,
            "action": action,
            "performed_by": admin_id,
            "new_values": details
        }
        supabase.table("device_logs").insert(log_entry).execute()
    except Exception as e:
        logger.error(f"Failed to log device action: {e}")

def send_device_notification(device_id, customer_phone, title, message, notification_type):
    try:
        notification = {
            "device_id": device_id,
            "customer_phone": customer_phone,
            "title": title,
            "message": message,
            "notification_type": notification_type
        }
        supabase.table("notifications").insert(notification).execute()
    except Exception as e:
        logger.error(f"Failed to send notification: {e}")

@app.route("/api/report-security-violation", methods=["POST"])
def report_security_violation():
    try:
        data = request.json
        device_id = data.get("device_id")
        customer_phone = data.get("customer_phone")
        violation_type = data.get("violation_type", "FACTORY_RESET_ATTEMPT")
        violation_details = data.get("details", "")
        
        # Get client IP and user agent
        ip_address = request.environ.get('HTTP_X_FORWARDED_FOR', request.environ.get('REMOTE_ADDR', ''))
        user_agent = request.headers.get('User-Agent', '')
        
        # Record security violation
        violation = {
            "device_id": device_id,
            "customer_phone": customer_phone,
            "violation_type": violation_type,
            "violation_details": violation_details,
            "ip_address": ip_address,
            "user_agent": user_agent
        }
        
        supabase.table("security_violations").insert(violation).execute()
        
        return jsonify({"success": True, "message": "Security violation reported"})
        
    except Exception as e:
        logger.error(f"Report security violation error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

# ============================================
# API ROUTES - SECURITY LOGGING
# ============================================

@app.route("/api/security/log-violation", methods=["POST"])
def log_security_violation():
    try:
        data = request.json
        
        violation_data = {
            "device_id": data.get("device_id"),
            "customer_phone": data.get("customer_phone"),
            "violation_type": data.get("violation_type", "UNKNOWN"),
            "violation_details": data.get("violation_details"),
            "ip_address": request.remote_addr,
            "user_agent": request.headers.get("User-Agent")
        }
        
        response = supabase.table("security_violations").insert(violation_data).execute()
        
        if response.data:
            logger.warning(f"Security violation logged: {violation_data['violation_type']} for device {violation_data['device_id']}")
            return jsonify({"success": True, "violation_id": response.data[0]["id"]})
        else:
            return jsonify({"success": False, "error": "Failed to log violation"}), 500
            
    except Exception as e:
        logger.error(f"Security logging error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/device/log-event", methods=["POST"])
def log_device_event():
    try:
        data = request.json
        
        log_data = {
            "device_id": data.get("device_id"),
            "action": data.get("action"),
            "old_values": data.get("old_values"),
            "new_values": data.get("new_values")
        }
        
        response = supabase.table("device_logs").insert(log_data).execute()
        
        if response.data:
            return jsonify({"success": True, "log_id": response.data[0]["id"]})
        else:
            return jsonify({"success": False, "error": "Failed to log event"}), 500
            
    except Exception as e:
        logger.error(f"Device logging error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

# Duplicate function removed - keeping the first definition

# ============================================
# API ROUTES - PERSISTENT AUTHENTICATION
# ============================================

@app.route("/api/auth/device-login", methods=["POST"])
def device_persistent_login():
    """Device-based persistent login that survives app updates"""
    try:
        data = request.json
        device_fingerprint = data.get("device_fingerprint")  # IMEI + Serial + Model
        phone = format_phone_number(data.get("phone_number"))
        pin = data.get("pin")
        
        if not device_fingerprint or not phone or not pin:
            return jsonify({"success": False, "error": "Device fingerprint, phone and PIN required"}), 400
        
        pin_hash = hash_password(pin)
        
        # Find device by phone and validate PIN
        response = supabase.table("devices").select("*").eq("customer_phone", phone).execute()
        
        if not response.data or len(response.data) == 0:
            return jsonify({"success": False, "error": "Account not found"}), 404
        
        device = response.data[0]
        
        if device.get("pin_hash") != pin_hash:
            return jsonify({"success": False, "error": "Invalid PIN"}), 401
        
        if device.get("is_locked", False):
            return jsonify({"success": False, "error": "Device is locked"}), 403
        
        # Create persistent session
        persistent_token = generate_token()
        
        # Try to store persistent session (fallback if table doesn't exist)
        try:
            session_data = {
                "device_fingerprint": device_fingerprint,
                "customer_phone": phone,
                "device_id": device["device_id"],
                "persistent_token": persistent_token,
                "created_at": "now()",
                "expires_at": "now() + interval '90 days'",  # 90 day expiry
                "is_active": True
            }
            supabase.table("persistent_sessions").insert(session_data).execute()
            logger.info(f"Persistent session created for device: {device['device_id']}")
        except Exception as session_error:
            logger.warning(f"Failed to create persistent session (table may not exist): {session_error}")
            # Continue without persistent session for now
        
        # Update device with last login (handle missing columns gracefully)
        update_data = {"token": persistent_token}
        
        # Add optional columns if they exist
        try:
            supabase.table("devices").select("last_login").limit(1).execute()
            update_data["last_login"] = "now()"
        except:
            logger.info("last_login column not found, skipping")
        
        try:
            supabase.table("devices").select("device_fingerprint").limit(1).execute()
            update_data["device_fingerprint"] = device_fingerprint
        except:
            logger.info("device_fingerprint column not found, skipping")
        
        supabase.table("devices").update(update_data).eq("id", device["id"]).execute()
        
        return jsonify({
            "success": True,
            "persistent_token": persistent_token,
            "customer_id": device.get("customer_id"),
            "device_id": device["device_id"],
            "customer_name": device.get("customer_name"),
            "expires_in_days": 90
        })
        
    except Exception as e:
        logger.error(f"Device persistent login error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/auth/device-auto-login", methods=["POST"])
def device_auto_login():
    """Auto-login using device fingerprint and persistent token"""
    try:
        data = request.json
        device_fingerprint = data.get("device_fingerprint")
        persistent_token = data.get("persistent_token")
        
        if not device_fingerprint or not persistent_token:
            return jsonify({"success": False, "error": "Device fingerprint and token required"}), 400
        
        # Try to check persistent session (fallback if table doesn't exist)
        try:
            session_response = supabase.table("persistent_sessions").select("*").eq("device_fingerprint", device_fingerprint).eq("persistent_token", persistent_token).eq("is_active", True).execute()
            
            if not session_response.data or len(session_response.data) == 0:
                return jsonify({"success": False, "error": "No valid session found"}), 404
            
            session = session_response.data[0]
            
            # Check if session is expired
            from datetime import datetime, timezone
            expires_at = datetime.fromisoformat(session["expires_at"].replace('Z', '+00:00'))
            if expires_at < datetime.now(timezone.utc):
                # Session expired - deactivate it
                supabase.table("persistent_sessions").update({"is_active": False}).eq("id", session["id"]).execute()
                return jsonify({"success": False, "error": "Session expired"}), 401
            
            # Get device info
            device_response = supabase.table("devices").select("*").eq("customer_phone", session["customer_phone"]).execute()
            
            if not device_response.data or len(device_response.data) == 0:
                return jsonify({"success": False, "error": "Device not found"}), 404
            
            device = device_response.data[0]
            
            if device.get("is_locked", False):
                return jsonify({"success": False, "error": "Device is locked"}), 403
            
            # Update last access
            supabase.table("persistent_sessions").update({"last_accessed": "now()"}).eq("id", session["id"]).execute()
            supabase.table("devices").update({"last_login": "now()"}).eq("id", device["id"]).execute()
            
            return jsonify({
                "success": True,
                "customer_id": device.get("customer_id"),
                "device_id": device["device_id"],
                "customer_name": device.get("customer_name"),
                "customer_phone": device.get("customer_phone"),
                "session_valid": True
            })
            
        except Exception as session_error:
            logger.warning(f"Persistent session check failed (table may not exist): {session_error}")
            
            # Fallback: Check device by token and fingerprint directly
            device_response = supabase.table("devices").select("*").eq("token", persistent_token).eq("device_fingerprint", device_fingerprint).execute()
            
            if not device_response.data or len(device_response.data) == 0:
                return jsonify({"success": False, "error": "No valid session found"}), 404
            
            device = device_response.data[0]
            
            if device.get("is_locked", False):
                return jsonify({"success": False, "error": "Device is locked"}), 403
            
            # Update last login
            supabase.table("devices").update({"last_login": "now()"}).eq("id", device["id"]).execute()
            
            return jsonify({
                "success": True,
                "customer_id": device.get("customer_id"),
                "device_id": device["device_id"],
                "customer_name": device.get("customer_name"),
                "customer_phone": device.get("customer_phone"),
                "session_valid": True
            })
        
    except Exception as e:
        logger.error(f"Device auto-login error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500
        
    except Exception as e:
        logger.error(f"Device auto-login error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/auth/admin-persistent-login", methods=["POST"])
def admin_persistent_login():
    """Admin persistent login that survives browser sessions"""
    try:
        data = request.json
        email = data.get("email")
        password = data.get("password")
        browser_fingerprint = data.get("browser_fingerprint", "")
        
        if not email or not password:
            return jsonify({"success": False, "error": "Email and password required"}), 400
        
        password_hash = hash_password(password)
        
        # Authenticate admin
        response = supabase.table("admins").select("*").eq("email", email).eq("password_hash", password_hash).execute()
        
        if not response.data or len(response.data) == 0:
            return jsonify({"success": False, "error": "Invalid credentials"}), 401
        
        admin = response.data[0]
        
        # Create persistent session
        persistent_token = generate_token()
        
        # Try to store persistent admin session (fallback if table doesn't exist)
        try:
            session_data = {
                "admin_id": admin["id"],
                "email": email,
                "browser_fingerprint": browser_fingerprint,
                "persistent_token": persistent_token,
                "created_at": "now()",
                "expires_at": "now() + interval '30 days'",  # 30 day expiry for admins
                "is_active": True
            }
            supabase.table("admin_sessions").insert(session_data).execute()
            logger.info(f"Persistent session created for admin: {email}")
        except Exception as session_error:
            logger.warning(f"Failed to create persistent session (table may not exist): {session_error}")
            # Continue without persistent session for now
        
        # Update admin last login
        supabase.table("admins").update({
            "token": persistent_token,
            "last_login": "now()"
        }).eq("id", admin["id"]).execute()
        
        return jsonify({
            "success": True,
            "persistent_token": persistent_token,
            "role": admin["role"],
            "admin_id": admin["id"],
            "email": admin["email"],
            "expires_in_days": 30,
            "must_change_password": admin.get("must_change_password", False)
        })
        
    except Exception as e:
        logger.error(f"Admin persistent login error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/auth/admin-auto-login", methods=["POST"])
def admin_auto_login():
    """Auto-login admin using persistent token"""
    try:
        data = request.json
        persistent_token = data.get("persistent_token")
        browser_fingerprint = data.get("browser_fingerprint", "")
        
        if not persistent_token:
            return jsonify({"success": False, "error": "Persistent token required"}), 400
        
        # Try to check persistent session (fallback if table doesn't exist)
        try:
            session_response = supabase.table("admin_sessions").select("*").eq("persistent_token", persistent_token).eq("is_active", True).execute()
            
            if not session_response.data or len(session_response.data) == 0:
                return jsonify({"success": False, "error": "No valid session found"}), 404
            
            session = session_response.data[0]
            
            # Check if session is expired
            from datetime import datetime, timezone
            expires_at = datetime.fromisoformat(session["expires_at"].replace('Z', '+00:00'))
            if expires_at < datetime.now(timezone.utc):
                # Session expired - deactivate it
                supabase.table("admin_sessions").update({"is_active": False}).eq("id", session["id"]).execute()
                return jsonify({"success": False, "error": "Session expired"}), 401
            
            # Get admin info
            admin_response = supabase.table("admins").select("*").eq("id", session["admin_id"]).execute()
            
            if not admin_response.data or len(admin_response.data) == 0:
                return jsonify({"success": False, "error": "Admin not found"}), 404
            
            admin = admin_response.data[0]
            
            # Update last access
            supabase.table("admin_sessions").update({"last_accessed": "now()"}).eq("id", session["id"]).execute()
            supabase.table("admins").update({"last_login": "now()"}).eq("id", admin["id"]).execute()
            
            return jsonify({
                "success": True,
                "role": admin["role"],
                "admin_id": admin["id"],
                "email": admin["email"],
                "session_valid": True,
                "must_change_password": admin.get("must_change_password", False)
            })
            
        except Exception as session_error:
            logger.warning(f"Persistent session check failed (table may not exist): {session_error}")
            
            # Fallback: Check admin by token directly
            admin_response = supabase.table("admins").select("*").eq("token", persistent_token).execute()
            
            if not admin_response.data or len(admin_response.data) == 0:
                return jsonify({"success": False, "error": "No valid session found"}), 404
            
            admin = admin_response.data[0]
            
            # Update last login
            supabase.table("admins").update({"last_login": "now()"}).eq("id", admin["id"]).execute()
            
            return jsonify({
                "success": True,
                "role": admin["role"],
                "admin_id": admin["id"],
                "email": admin["email"],
                "session_valid": True,
                "must_change_password": admin.get("must_change_password", False)
            })
        
    except Exception as e:
        logger.error(f"Admin auto-login error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

# ============================================
# API ROUTES - IMEI TRACKING & DEVICE LOCKING
# ============================================

@app.route("/api/device/report-imei", methods=["POST"])
def report_device_imei():
    """Report device IMEI for tracking and security"""
    try:
        data = request.json
        imei = data.get("imei")
        device_model = data.get("device_model")
        device_brand = data.get("device_brand")
        android_version = data.get("android_version")
        app_version = data.get("app_version")
        
        if not imei:
            return jsonify({"success": False, "error": "IMEI required"}), 400
        
        # Get client IP
        ip_address = request.environ.get('HTTP_X_FORWARDED_FOR', request.environ.get('REMOTE_ADDR', ''))
        
        # Check if device exists by IMEI
        existing_device = supabase.table("devices").select("*").eq("imei", imei).execute()
        
        if existing_device.data and len(existing_device.data) > 0:
            # Update existing device
            supabase.table("devices").update({
                "device_model": device_model,
                "device_brand": device_brand,
                "android_version": android_version,
                "app_version": app_version,
                "last_seen": "now()",
                "ip_address": ip_address
            }).eq("imei", imei).execute()
            
            logger.info(f"IMEI updated: {imei[:4]}****")
        else:
            # Log unregistered device
            logger.warning(f"Unregistered device IMEI reported: {imei[:4]}****")
            
            # Store in security violations for tracking
            violation_data = {
                "device_id": None,
                "customer_phone": None,
                "violation_type": "UNREGISTERED_DEVICE",
                "violation_details": f"Unregistered device with IMEI {imei[:4]}**** reported",
                "ip_address": ip_address,
                "user_agent": request.headers.get("User-Agent")
            }
            supabase.table("security_violations").insert(violation_data).execute()
        
        return jsonify({"success": True, "message": "IMEI reported successfully"})
        
    except Exception as e:
        logger.error(f"Report IMEI error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/device/check-imei-lock", methods=["GET"])
def check_imei_lock():
    """Check if device is locked by IMEI"""
    try:
        imei = request.args.get("imei")
        
        if not imei:
            return jsonify({"is_locked": False, "error": "IMEI required"}), 400
        
        # Check if device exists and is locked
        response = supabase.table("devices").select("*").eq("imei", imei).execute()
        
        if response.data and len(response.data) > 0:
            device = response.data[0]
            is_locked = device.get("is_locked", False)
            
            # Check loan balance - if outstanding, device should be locked
            total_amount = float(device.get("total_amount", 0))
            amount_paid = float(device.get("amount_paid", 0))
            loan_balance = total_amount - amount_paid
            
            if loan_balance > 0:
                is_locked = True
                # Update device lock status
                supabase.table("devices").update({
                    "is_locked": True,
                    "status": "locked"
                }).eq("imei", imei).execute()
            
            return jsonify({
                "is_locked": is_locked,
                "device_id": device["device_id"],
                "customer_phone": device["customer_phone"],
                "loan_balance": loan_balance,
                "lock_reason": "OUTSTANDING_BALANCE" if loan_balance > 0 else None
            })
        else:
            # Unknown IMEI - lock by default for security
            logger.warning(f"Unknown IMEI lock check: {imei[:4]}****")
            return jsonify({
                "is_locked": True,
                "lock_reason": "UNREGISTERED_DEVICE"
            })
        
    except Exception as e:
        logger.error(f"Check IMEI lock error: {e}")
        return jsonify({"is_locked": True, "error": str(e)}), 500

@app.route("/api/admin/lock-device-by-imei", methods=["POST"])
def lock_device_by_imei():
    """Admin endpoint to lock device by IMEI"""
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        data = request.json
        imei = data.get("imei")
        lock_reason = data.get("lock_reason", "ADMIN_LOCK")
        
        if not imei:
            return jsonify({"success": False, "error": "IMEI required"}), 400
        
        # Find and lock device by IMEI
        response = supabase.table("devices").select("*").eq("imei", imei).execute()
        
        if response.data and len(response.data) > 0:
            device = response.data[0]
            
            # Lock the device
            supabase.table("devices").update({
                "is_locked": True,
                "status": "locked",
                "lock_reason": lock_reason,
                "locked_by": admin["id"],
                "locked_at": "now()"
            }).eq("imei", imei).execute()
            
            # Log the action
            log_device_action(device["device_id"], "DEVICE_LOCKED_BY_IMEI", admin["id"], {
                "imei": imei[:4] + "****",
                "lock_reason": lock_reason
            })
            
            return jsonify({
                "success": True,
                "message": f"Device locked successfully",
                "device_id": device["device_id"]
            })
        else:
            return jsonify({"success": False, "error": "Device not found"}), 404
        
    except Exception as e:
        logger.error(f"Lock device by IMEI error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

# ============================================
# API ROUTES - DEVICE ENROLLMENT (UPDATED)
# ============================================

@app.route("/api/devices/enroll", methods=["POST"])
def enroll_device():
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        data = request.json
        
        # Required fields
        required_fields = ["serial_number", "national_id", "customer_name", "customer_phone", "total_amount", "daily_payment_amount"]
        for field in required_fields:
            if not data.get(field):
                return jsonify({"success": False, "error": f"Missing required field: {field}"}), 400
        
        # Generate device ID
        device_id = f"EDN{data['serial_number'][-6:].upper()}"
        
        # Format phone number
        customer_phone = format_phone_number(data["customer_phone"])
        
        # Get downpayment (initial payment) - this is separate from daily payments
        downpayment = float(data.get("amount_paid", 0))
        
        # Get IMEI if provided
        imei = data.get("imei")
        
        # Calculate 3-hour trial period from enrollment
        from datetime import datetime, timedelta
        trial_until = datetime.now() + timedelta(hours=3)
        
        # Create device record - start with essential fields only
        device_data = {
            "device_id": device_id,
            "customer_id": data["national_id"],
            "serial_number": data["serial_number"],
            "national_id": data["national_id"],
            "customer_name": data["customer_name"],
            "customer_phone": customer_phone,
            "total_amount": float(data["total_amount"]),
            "daily_payment_amount": float(data["daily_payment_amount"]),
            "amount_paid": downpayment,  # This is the downpayment (deposit)
            "downpayment": downpayment,  # Track downpayment separately
            "payment_balance": 0,  # No payment balance yet (only actual payments count)
            "status": "pending_registration",  # Customer must register first
            "pin_hash": None,  # No PIN until customer registers
            "is_locked": False,  # Unlocked for 3-hour trial
            "must_change_pin": False,  # Customer sets their own PIN
            "unlock_until": trial_until.isoformat(),  # 3-hour trial period
            "trial_period": True  # Mark as trial period
        }
        
        # Add optional fields only if they exist in the schema
        try:
            # Test if optional columns exist by checking schema
            optional_fields = {
                "enrolled_by": admin["id"],
                "id_front_url": data.get("id_front", ""),
                "id_back_url": data.get("id_back", ""),
                "passport_photo_url": data.get("passport_photo", ""),
                "imei": imei
            }
            
            # Add optional fields to device_data
            device_data.update(optional_fields)
            logger.info(f"Using full device enrollment with all fields")
            
        except Exception as schema_error:
            logger.warning(f"Some optional columns may not exist, using basic enrollment: {schema_error}")
            # Continue with basic fields only
        
        response = supabase.table("devices").insert(device_data).execute()
        
        if response.data:
            return jsonify({
                "success": True,
                "device_id": device_id,
                "customer_phone": customer_phone,
                "imei_tracking": "enabled" if imei else "disabled",
                "message": f"Device enrolled successfully. Customer must register in the app using phone {customer_phone}",
                "important": f"⚠️ CUSTOMER REGISTRATION REQUIRED:\nPhone: {customer_phone}\n\nCustomer must download the Eden app and register with this phone number to set their PIN and activate the device.",
                "status": "pending_registration"
            })
        else:
            return jsonify({"success": False, "error": "Failed to enroll device"}), 500
            
    except Exception as e:
        logger.error(f"Device enrollment error: {e}")
        
        # If it's a column not found error, provide specific guidance
        if "could not find" in str(e).lower() and "column" in str(e).lower():
            return jsonify({
                "success": False, 
                "error": "Database schema needs update. Please run the enrollment fix SQL script.",
                "fix_required": "Run QUICK_ENROLLMENT_FIX.sql in your database"
            }), 500
        
        return jsonify({"success": False, "error": str(e)}), 500

# ============================================
# API ROUTES - CUSTOMERS
# ============================================

@app.route("/api/customers", methods=["GET"])
def get_customers():
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        # Get all devices with customer info
        response = supabase.table("devices").select("*").execute()
        
        # Transform device data to customer format
        customers = []
        for device in response.data:
            if device.get("customer_phone"):
                total_amount = float(device.get("total_amount", 0))
                amount_paid = float(device.get("amount_paid", 0))
                customers.append({
                    "customer_id": device.get("customer_id"),
                    "phone_number": device.get("customer_phone"),
                    "full_name": device.get("customer_name"),
                    "national_id": device.get("national_id"),
                    "total_loan_amount": total_amount,
                    "amount_paid": amount_paid,
                    "loan_balance": total_amount - amount_paid,
                    "device_id": device.get("device_id"),
                    "status": device.get("status")
                })
        
        return jsonify(customers)
    except Exception as e:
        logger.error(f"Get customers error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/customer/payments", methods=["GET"])
def customer_payments():
    try:
        phone = format_phone_number(request.args.get("phone"))
        
        if not phone:
            return jsonify([])
        
        response = supabase.table("payments").select("*").eq("customer_phone", phone).order("payment_date", desc=True).execute()
        return jsonify(response.data)
    except Exception as e:
        logger.error(f"Customer payments error: {e}")
        return jsonify([]), 500

@app.route("/api/customer/register", methods=["POST"])
def customer_register():
    """Customer registration - check if phone is enrolled and set PIN"""
    try:
        data = request.json
        if not data:
            logger.error("Customer registration: No data provided")
            return jsonify({"success": False, "error": "No data provided"}), 400
        
        raw_phone = data.get("phone_number")
        pin = data.get("pin")
        confirm_pin = data.get("confirm_pin")
        
        logger.info(f"Registration attempt - Phone: {raw_phone}, PIN length: {len(pin) if pin else 0}")
        
        if not raw_phone or not pin or not confirm_pin:
            logger.error("Customer registration: Missing required fields")
            return jsonify({"success": False, "error": "Phone number, PIN, and PIN confirmation required"}), 400
        
        if len(pin) != 4 or not pin.isdigit():
            logger.error(f"Customer registration: Invalid PIN format - Length: {len(pin)}, IsDigit: {pin.isdigit()}")
            return jsonify({"success": False, "error": "PIN must be exactly 4 digits"}), 400
        
        if pin != confirm_pin:
            logger.error("Customer registration: PINs do not match")
            return jsonify({"success": False, "error": "PINs do not match"}), 400
        
        # Format phone number consistently
        phone = format_phone_number(raw_phone)
        logger.info(f"Customer registration attempt - Raw: {raw_phone}, Formatted: {phone}")
        
        # Check if phone number is enrolled for a device
        response = supabase.table("devices").select("*").eq("customer_phone", phone).execute()
        
        if not response.data or len(response.data) == 0:
            logger.warning(f"Registration failed - phone not enrolled: {phone}")
            return jsonify({
                "success": False, 
                "error": "Phone number not found. Please contact support to enroll your device first."
            }), 404
        
        device = response.data[0]
        logger.info(f"Found device for registration: {device.get('device_id', 'unknown')}")
        
        # Check if customer has already registered
        if device.get("pin_hash") is not None:
            logger.warning(f"Registration failed - customer already registered: {phone}")
            return jsonify({
                "success": False,
                "error": "Account already registered. Please use the login option."
            }), 400
        
        # Hash the PIN and update device
        pin_hash = hash_password(pin)
        logger.info(f"Generated PIN hash for device: {device.get('device_id', 'unknown')}")
        
        # Prepare update data with only columns that exist
        update_data = {
            "pin_hash": pin_hash,
            "status": "active",
            "is_locked": False
        }
        
        # Add optional columns if they exist
        try:
            # Check if registered_at column exists
            supabase.table("devices").select("registered_at").limit(1).execute()
            update_data["registered_at"] = "now()"
            logger.info("Added registered_at to update")
        except Exception as e:
            logger.info(f"registered_at column not found: {e}")
        
        # Update device with customer's PIN
        logger.info(f"Updating device with data: {update_data}")
        update_response = supabase.table("devices").update(update_data).eq("id", device["id"]).execute()
        
        if not update_response.data:
            logger.error(f"Failed to update device during registration: {phone} - Response: {update_response}")
            return jsonify({"success": False, "error": "Registration failed. Please try again."}), 500
        
        logger.info(f"Device updated successfully: {device.get('device_id', 'unknown')}")
        
        # Generate token for immediate login
        token = generate_token()
        token_update = {"token": token}
        
        # Add last_login if column exists
        try:
            supabase.table("devices").select("last_login").limit(1).execute()
            token_update["last_login"] = "now()"
            logger.info("Added last_login to token update")
        except Exception as e:
            logger.info(f"last_login column not found: {e}")
        
        logger.info(f"Updating device with token: {token_update}")
        token_response = supabase.table("devices").update(token_update).eq("id", device["id"]).execute()
        
        if not token_response.data:
            logger.warning(f"Failed to update token, but registration succeeded: {phone}")
        
        logger.info(f"Customer registration successful for phone: {phone}")
        
        return jsonify({
            "success": True,
            "message": "Registration successful! You can now use the app.",
            "token": token,
            "customer_id": device.get("customer_id", device.get("national_id")),
            "device_id": device["device_id"],
            "customer_name": device.get("customer_name"),
            "phone": phone
        })
        
    except Exception as e:
        logger.error(f"Customer registration error: {e}")
        import traceback
        logger.error(f"Registration traceback: {traceback.format_exc()}")
        return jsonify({"success": False, "error": f"Registration failed: {str(e)}"}), 500

@app.route("/api/customer/check-phone", methods=["POST"])
def check_customer_phone():
    try:
        data = request.json
        phone = format_phone_number(data.get("phone_number"))
        
        if not phone:
            return jsonify({"exists": False, "enrolled": False})
        
        # Query devices table to check enrollment and registration status
        response = supabase.table("devices").select("*").eq("customer_phone", phone).execute()
        
        if response.data and len(response.data) > 0:
            device = response.data[0]
            has_pin = device.get("pin_hash") is not None
            
            return jsonify({
                "exists": True,
                "enrolled": True,
                "registered": has_pin,
                "has_pin": has_pin,
                "status": device.get("status", "unknown"),
                "customer_name": device.get("customer_name"),
                "action": "login" if has_pin else "register"
            })
        else:
            return jsonify({
                "exists": False,
                "enrolled": False,
                "registered": False,
                "action": "contact_support"
            })
    except Exception as e:
        logger.error(f"Check phone error: {e}")
        return jsonify({"exists": False, "enrolled": False, "error": str(e)}), 500

@app.route("/api/admin/reset-customer-pin", methods=["POST"])
def reset_customer_pin():
    """Admin endpoint to reset a customer's PIN"""
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        data = request.json
        phone = format_phone_number(data.get("phone_number", ""))
        new_pin = data.get("new_pin", "1234")
        
        if not phone:
            return jsonify({"success": False, "error": "Phone number required"}), 400
        
        if len(new_pin) != 4 or not new_pin.isdigit():
            return jsonify({"success": False, "error": "PIN must be exactly 4 digits"}), 400
        
        # Hash the new PIN
        pin_hash = hash_password(new_pin)
        
        # Update the device record
        response = supabase.table("devices").update({
            "pin_hash": pin_hash,
            "must_change_pin": True
        }).eq("customer_phone", phone).execute()
        
        if response.data:
            logger.info(f"PIN reset for customer: {phone} by admin: {admin['email']}")
            return jsonify({
                "success": True,
                "message": f"PIN reset successfully for {phone}",
                "new_pin": new_pin,
                "customer_phone": phone
            })
        else:
            return jsonify({"success": False, "error": "Customer not found"}), 404
        
    except Exception as e:
        logger.error(f"Reset customer PIN error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/debug/device-pin", methods=["POST"])
def debug_device_pin():
    """Debug endpoint to check device PIN information"""
    try:
        data = request.json
        phone = format_phone_number(data.get("phone_number", ""))
        
        if not phone:
            return jsonify({"error": "Phone number required"}), 400
        
        # Get device info
        response = supabase.table("devices").select("customer_phone, pin_hash, must_change_pin, enrolled_by, created_at").eq("customer_phone", phone).execute()
        
        if not response.data:
            return jsonify({"error": "Device not found"}), 404
        
        device = response.data[0]
        
        # Test common PINs
        common_pins = ["1234", "0000", "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999"]
        matching_pin = None
        
        stored_hash = device.get("pin_hash")
        if stored_hash:
            for test_pin in common_pins:
                if hash_password(test_pin) == stored_hash:
                    matching_pin = test_pin
                    break
        
        return jsonify({
            "phone": phone,
            "has_pin_hash": bool(stored_hash),
            "pin_hash_preview": stored_hash[:10] + "..." if stored_hash else None,
            "must_change_pin": device.get("must_change_pin", False),
            "enrolled_by": device.get("enrolled_by"),
            "created_at": device.get("created_at"),
            "matching_common_pin": matching_pin,
            "suggested_action": "Use PIN: " + matching_pin if matching_pin else "Contact admin for correct PIN"
        })
        
    except Exception as e:
        logger.error(f"Debug device PIN error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/api/customer/login", methods=["POST"])
def customer_login_api():
    try:
        if supabase is None:
            logger.error("Supabase client not initialized")
            return jsonify({"success": False, "error": "Database connection not available"}), 500
        
        data = request.json
        if not data:
            logger.error("No data provided in customer login request")
            return jsonify({"success": False, "error": "No data provided"}), 400
            
        raw_phone = data.get("phone_number")
        pin = data.get("pin")
        
        if not raw_phone or not pin:
            logger.error(f"Missing phone or PIN. Phone: {raw_phone}, PIN: {'*' * len(pin) if pin else 'None'}")
            return jsonify({"success": False, "error": "Phone and PIN required"}), 400
        
        # Format phone number consistently
        phone = format_phone_number(raw_phone)
        logger.info(f"Customer login attempt - Raw: {raw_phone}, Formatted: {phone}")
        
        if len(pin) != 4 or not pin.isdigit():
            logger.error(f"Invalid PIN format for phone: {phone}")
            return jsonify({"success": False, "error": "PIN must be exactly 4 digits"}), 400
        
        pin_hash = hash_password(pin)
        logger.info(f"PIN hash generated for: {phone}")
        
        try:
            # Query devices table for customer authentication
            response = supabase.table("devices").select("*").eq("customer_phone", phone).execute()
            logger.info(f"Database query executed for phone: {phone}, found {len(response.data) if response.data else 0} records")
            
            if not response.data or len(response.data) == 0:
                logger.warning(f"No device found for phone: {phone}")
                
                # Try alternative phone formats for debugging
                alt_formats = [
                    raw_phone,
                    raw_phone.replace("+254", "0"),
                    f"0{raw_phone.replace('+254', '')}" if raw_phone.startswith("+254") else raw_phone
                ]
                
                for alt_phone in alt_formats:
                    if alt_phone != phone:
                        alt_response = supabase.table("devices").select("customer_phone").eq("customer_phone", alt_phone).execute()
                        if alt_response.data:
                            logger.info(f"Found device with alternative format: {alt_phone}")
                            break
                
                return jsonify({"success": False, "error": "Account not found. Please contact support."}), 404
            
            device = response.data[0]
            logger.info(f"Device found for phone: {phone}, checking PIN...")
            
            # Check PIN hash
            stored_pin_hash = device.get("pin_hash")
            if stored_pin_hash != pin_hash:
                logger.warning(f"Invalid PIN for phone: {phone}")
                logger.debug(f"Stored hash: {stored_pin_hash[:10] if stored_pin_hash else 'None'}..., Provided hash: {pin_hash[:10]}...")
                
                # Check if this might be a default PIN issue
                default_pins = ["1234", "0000"]
                suggested_pin = None
                for test_pin in default_pins:
                    if hash_password(test_pin) == stored_pin_hash:
                        suggested_pin = test_pin
                        break
                
                error_msg = "Invalid PIN. Please try again."
                if suggested_pin:
                    error_msg = f"Invalid PIN. Try using the default PIN: {suggested_pin}"
                elif device.get("must_change_pin", False):
                    error_msg = "Invalid PIN. Please use the PIN provided by the administrator during device enrollment."
                
                return jsonify({"success": False, "error": error_msg}), 401
            
            # Check if device is locked
            if device.get("is_locked", False):
                logger.warning(f"Device is locked for phone: {phone}")
                return jsonify({"success": False, "error": "Device is locked. Please contact support."}), 403
            
            # Generate and store token
            token = generate_token()
            supabase.table("devices").update({
                "token": token,
                "last_login": "now()"
            }).eq("id", device["id"]).execute()
            
            logger.info(f"Customer login successful for phone: {phone}")
            
            # Check if customer must change PIN
            must_change = device.get("must_change_pin", False)
            
            return jsonify({
                "success": True,
                "token": token,
                "customer_id": device.get("customer_id", device.get("national_id")),
                "device_id": device["device_id"],
                "customer_name": device.get("customer_name"),
                "must_change_pin": must_change
            })
            
        except Exception as db_error:
            logger.error(f"Database error during customer login: {db_error}")
            if "relation \"devices\" does not exist" in str(db_error):
                return jsonify({
                    "success": False, 
                    "error": "Database not set up. Please contact support."
                }), 500
            return jsonify({"success": False, "error": "Database error. Please try again."}), 500
            
    except Exception as e:
        logger.error(f"Customer login error: {e}")
        return jsonify({"success": False, "error": "Server error. Please try again."}), 500

@app.route("/api/customer/set-pin", methods=["POST"])
def set_customer_pin():
    try:
        data = request.json
        phone = format_phone_number(data.get("phone_number"))
        pin = data.get("pin")
        
        if not phone or not pin:
            return jsonify({"success": False, "error": "Phone and PIN required"}), 400
        
        if len(pin) != 4:
            return jsonify({"success": False, "error": "PIN must be 4 digits"}), 400
        
        pin_hash = hash_password(pin)
        
        # Update devices table instead of customers
        supabase.table("devices").update({
            "pin_hash": pin_hash,
            "must_change_pin": False
        }).eq("customer_phone", phone).execute()
        
        return jsonify({"success": True})
    except Exception as e:
        logger.error(f"Set PIN error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/admin/change-password", methods=["POST"])
def change_admin_password():
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        data = request.json
        new_password = data.get("new_password")
        
        if not new_password or len(new_password) < 6:
            return jsonify({"success": False, "error": "Password must be at least 6 characters"}), 400
        
        new_password_hash = hash_password(new_password)
        
        supabase.table("admins").update({
            "password_hash": new_password_hash,
            "must_change_password": False
        }).eq("id", admin["id"]).execute()
        
        return jsonify({"success": True})
    except Exception as e:
        logger.error(f"Change password error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/customer/dashboard", methods=["GET"])
def customer_dashboard_api():
    try:
        phone = format_phone_number(request.args.get("phone"))
        
        if not phone:
            return jsonify({"success": False, "error": "Phone required"}), 400
        
        # Get device data (which contains all customer info)
        device_response = supabase.table("devices").select("*").eq("customer_phone", phone).execute()
        
        if not device_response.data or len(device_response.data) == 0:
            return jsonify({"success": False, "error": "Customer not found"}), 404
        
        device = device_response.data[0]
        
        # Calculate loan balance
        total_amount = float(device.get("total_amount", 0))
        amount_paid = float(device.get("amount_paid", 0))
        loan_balance = total_amount - amount_paid
        
        # Build customer object from device data
        customer = {
            "customer_id": device.get("customer_id"),
            "phone_number": device.get("customer_phone"),
            "full_name": device.get("customer_name"),
            "national_id": device.get("national_id"),
            "total_loan_amount": total_amount,
            "amount_paid": amount_paid,
            "loan_balance": loan_balance,
            "next_payment_date": None  # Can be added later if needed
        }
        
        # Get payment history
        try:
            payments_response = supabase.table("payments").select("*").eq("customer_phone", phone).order("payment_date", desc=True).limit(10).execute()
            payments = payments_response.data if payments_response.data else []
        except Exception as e:
            logger.warning(f"Failed to fetch payments: {e}")
            payments = []
        
        return jsonify({
            "success": True,
            "customer": customer,
            "device": device,
            "payments": payments
        })
    except Exception as e:
        logger.error(f"Customer dashboard error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

# ============================================
# APP UPDATE ENDPOINT
# ============================================

@app.route("/api/app/version", methods=["GET"])
def get_app_version():
    """Return current app version for OTA updates"""
    return jsonify({
        "version_code": 23,
        "version_name": "1.9.5",
        "download_url": f"{request.host_url}download/eden.apk",
        "force_update": True,
        "security_level": "MAXIMUM",
        "factory_reset_protection": True,
        "customer_self_registration": True,
        "improved_phone_flow": True,
        "registration_fixes": True,
        "mobile_dashboard": True,
        "payment_system": True,
        "multi_day_unlock": True,
        "features": [
            "💳 Complete Payment System with Auto-Unlock",
            "📅 Multi-day unlock capability (pay 160 = 2 days unlock)",
            "💰 Payment balance tracking and prepaid system",
            "🔓 Manual unlock requests from customer app",
            "⏰ Automatic device locking when payment expires",
            "📊 Real-time payment status and days available",
            "🎯 Daily payment amount configuration per device",
            "💸 KES 80 default daily payment for existing devices",
            "🔄 Loading animations for all payment actions",
            "Clean mobile-friendly customer dashboard",
            "Compact card layout optimized for mobile",
            "Properly sized buttons and touch targets",
            "Enhanced device locking with better error handling",
            "Fixed customer registration issues",
            "Enhanced error handling and logging",
            "Database column compatibility fixes",
            "Improved phone number entry flow",
            "Better 07 format display with +254 server submission",
            "Enhanced phone number validation",
            "Customer self-registration",
            "No default PINs",
            "Immediate device activation",
            "Complete lockdown protection",
            "Persistent authentication",
            "Eden Logo Boot Screen for Device Owner",
            "Fixed Customer Login Authentication",
            "IMEI Tracking & Device Locking",
            "Enhanced Device Admin Capabilities",
            "Automatic Factory Reset Recovery",
            "Maximum Security Restrictions",
            "Admin Remote Device Control"
        ],
        "changelog": "Eden v1.9.5: Complete Payment System - Customers can now make payments directly in the app with automatic device unlocking. Multi-day unlock capability allows paying for multiple days at once (e.g., pay KES 160 for 2 days unlock). Real-time payment balance tracking, manual unlock requests, and automatic expiry management. Enhanced mobile dashboard with loading animations and improved user experience."
    })

# ============================================
# APK DOWNLOAD ENDPOINT
# ============================================

@app.route("/download/eden.apk")
def download_apk():
    """Serve the APK file for download"""
    try:
        return send_from_directory("app", "eden.apk", as_attachment=True)
    except Exception as e:
        logger.error(f"APK download error: {e}")
        return jsonify({"error": "APK not found"}), 404

@app.route("/download/eden-device-setup.cmd")
def download_setup_tool():
    """Serve the device setup CMD file for download"""
    try:
        return send_from_directory(".", "eden-device-setup.cmd", as_attachment=True)
    except Exception as e:
        logger.error(f"Setup tool download error: {e}")
        return jsonify({"error": "Setup tool not found"}), 404

# ============================================
# HEALTH CHECK
# ============================================

@app.route("/health")
def health():
    from datetime import datetime
    return jsonify({"status": "healthy", "timestamp": str(datetime.now())})

@app.route("/api/setup-check")
def setup_check():
    """Check if database is properly set up"""
    try:
        if supabase is None:
            return jsonify({
                "setup_complete": False,
                "error": "Supabase client not initialized",
                "next_step": "Check environment variables"
            })
        
        # Check if admins table exists
        try:
            response = supabase.table("admins").select("count", count="exact").execute()
            admin_count = response.count if response.count is not None else 0
            
            # Check if devices table exists
            devices_response = supabase.table("devices").select("count", count="exact").execute()
            device_count = devices_response.count if devices_response.count is not None else 0
            
            return jsonify({
                "setup_complete": True,
                "admin_count": admin_count,
                "device_count": device_count,
                "message": "Database is set up and ready",
                "next_step": "Register super admin at /register" if admin_count == 0 else "System ready for use"
            })
            
        except Exception as db_error:
            if "relation \"admins\" does not exist" in str(db_error):
                return jsonify({
                    "setup_complete": False,
                    "error": "Database tables not created",
                    "next_step": "Run FRESH_AUTH_SYSTEM_COMPLETE.sql in Supabase SQL Editor"
                })
            else:
                return jsonify({
                    "setup_complete": False,
                    "error": f"Database error: {str(db_error)}",
                    "next_step": "Check database connection and permissions"
                })
                
    except Exception as e:
        return jsonify({
            "setup_complete": False,
            "error": f"Server error: {str(e)}",
            "next_step": "Check server logs and configuration"
        })

if __name__ == "__main__":
    port = int(os.getenv("PORT", 10000))
    app.run(host="0.0.0.0", port=port, debug=False)
