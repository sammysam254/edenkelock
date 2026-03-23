import os
from flask import Flask, jsonify, render_template, request, send_from_directory
from flask_cors import CORS
from supabase import create_client
import logging
import hashlib
import secrets

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
    """Convert phone numbers starting with 07 to international format +254"""
    if not phone:
        return phone
    
    phone = str(phone).strip()
    
    # Remove any spaces, dashes, or parentheses
    phone = phone.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
    
    # If starts with 07, replace with +254
    if phone.startswith("07"):
        phone = "+254" + phone[1:]
    # If starts with 7 (without 0), add +254
    elif phone.startswith("7") and len(phone) == 9:
        phone = "+254" + phone
    # If starts with 254, add +
    elif phone.startswith("254"):
        phone = "+" + phone
    # If doesn't start with +, assume it needs +254
    elif not phone.startswith("+"):
        phone = "+254" + phone
    
    return phone

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
            return jsonify({"success": False, "error": "No data provided"}), 400
            
        username = data.get("username")
        password = data.get("password")
        
        if not username or not password:
            return jsonify({"success": False, "error": "Username and password required"}), 400
        
        logger.info(f"Login attempt for username: {username}")
        
        password_hash = hash_password(password)
        
        try:
            response = supabase.table("admins").select("*").eq("username", username).eq("password_hash", password_hash).execute()
            
            if response.data and len(response.data) > 0:
                admin = response.data[0]
                token = generate_token()
                
                supabase.table("admins").update({"token": token}).eq("admin_id", admin["admin_id"]).execute()
                
                logger.info(f"Login successful for: {username}")
                
                # Check if admin must change password
                must_change = admin.get("must_change_password", False)
                
                return jsonify({
                    "success": True,
                    "token": token,
                    "role": admin["role"],
                    "admin_id": admin["admin_id"],
                    "must_change_password": must_change
                })
            else:
                logger.warning(f"Invalid credentials for: {username}")
                return jsonify({"success": False, "error": "Invalid credentials"}), 401
        except Exception as db_error:
            logger.error(f"Database error during login: {db_error}")
            return jsonify({"success": False, "error": "Database error"}), 500
            
    except Exception as e:
        logger.error(f"Login error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

# ============================================
# API ROUTES - DEVICES
# ============================================

@app.route("/api/devices", methods=["GET"])
def get_devices():
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        response = supabase.table("devices").select("*").execute()
        return jsonify(response.data)
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
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        supabase.table("devices").update({
            "status": "locked",
            "is_locked": True
        }).eq("device_id", device_id).execute()
        
        return jsonify({"success": True, "message": "Device locked", "is_locked": True})
    except Exception as e:
        logger.error(f"Lock device error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/devices/<device_id>/unlock", methods=["POST"])
def unlock_device(device_id):
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        supabase.table("devices").update({
            "status": "active",
            "is_locked": False
        }).eq("device_id", device_id).execute()
        
        return jsonify({"success": True, "message": "Device unlocked", "is_locked": False})
    except Exception as e:
        logger.error(f"Unlock device error: {e}")
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
        log_device_action(device_id, "LOAN_BALANCE_UPDATE", admin["admin_id"], {
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
            "deleted_by": admin["admin_id"],
            "status": "deleted"
        }).eq("device_id", device_id).execute()
        
        # Log the deletion
        log_device_action(device_id, "DEVICE_DELETED", admin["admin_id"], {
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
        }).eq("admin_id", target_admin_id).execute()
        
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
            "username": username,
            "password_hash": password_hash,
            "role": "admin",
            "full_name": full_name,
            "email": email,
            "created_by": admin["admin_id"],
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
# API ROUTES - DEVICE ENROLLMENT
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
        required_fields = ["serial_number", "national_id", "customer_name", "customer_phone", "total_amount"]
        for field in required_fields:
            if not data.get(field):
                return jsonify({"success": False, "error": f"Missing required field: {field}"}), 400
        
        # Generate device ID
        device_id = f"EDN{data['serial_number'][-6:].upper()}"
        
        # Format phone number
        customer_phone = format_phone_number(data["customer_phone"])
        
        # Admin sets customer default password (PIN)
        default_pin = data.get("default_pin", "1234")  # Admin can set custom PIN
        if len(default_pin) != 4 or not default_pin.isdigit():
            return jsonify({"success": False, "error": "Default PIN must be exactly 4 digits"}), 400
        
        pin_hash = hash_password(default_pin)
        
        # Create device record
        device_data = {
            "device_id": device_id,
            "customer_id": data["national_id"],
            "serial_number": data["serial_number"],
            "national_id": data["national_id"],
            "customer_name": data["customer_name"],
            "customer_phone": customer_phone,
            "total_amount": float(data["total_amount"]),
            "amount_paid": float(data.get("amount_paid", 0)),
            "status": "active",
            "pin_hash": pin_hash,
            "must_change_pin": True,  # Force PIN change on first login
            "is_locked": False,
            "id_front_url": data.get("id_front", ""),
            "id_back_url": data.get("id_back", ""),
            "passport_photo_url": data.get("passport_photo", "")
        }
        
        response = supabase.table("devices").insert(device_data).execute()
        
        if response.data:
            return jsonify({
                "success": True,
                "device_id": device_id,
                "customer_phone": customer_phone,
                "default_pin": default_pin,
                "message": f"Device enrolled successfully. Customer can login with phone {customer_phone} and PIN {default_pin}"
            })
        else:
            return jsonify({"success": False, "error": "Failed to enroll device"}), 500
            
    except Exception as e:
        logger.error(f"Device enrollment error: {e}")
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

@app.route("/api/customer/check-phone", methods=["POST"])
def check_customer_phone():
    try:
        data = request.json
        phone = format_phone_number(data.get("phone_number"))
        
        if not phone:
            return jsonify({"exists": False})
        
        # Query devices table instead of customers
        response = supabase.table("devices").select("*").eq("customer_phone", phone).execute()
        
        if response.data and len(response.data) > 0:
            device = response.data[0]
            return jsonify({
                "exists": True,
                "has_pin": device.get("pin_hash") is not None
            })
        else:
            return jsonify({"exists": False})
    except Exception as e:
        logger.error(f"Check phone error: {e}")
        return jsonify({"exists": False, "error": str(e)}), 500

@app.route("/api/customer/login", methods=["POST"])
def customer_login_api():
    try:
        data = request.json
        phone = format_phone_number(data.get("phone_number"))
        pin = data.get("pin")
        
        if not phone or not pin:
            return jsonify({"success": False, "error": "Phone and PIN required"}), 400
        
        pin_hash = hash_password(pin)
        
        # Query devices table instead of customers
        response = supabase.table("devices").select("*").eq("customer_phone", phone).eq("pin_hash", pin_hash).execute()
        
        if response.data and len(response.data) > 0:
            device = response.data[0]
            token = generate_token()
            
            # Store token in device record
            supabase.table("devices").update({"token": token}).eq("id", device["id"]).execute()
            
            # Check if customer must change PIN
            must_change = device.get("must_change_pin", False)
            
            return jsonify({
                "success": True,
                "token": token,
                "customer_id": device["customer_id"],
                "device_id": device["device_id"],
                "must_change_pin": must_change
            })
        else:
            return jsonify({"success": False, "error": "Invalid credentials"}), 401
            
    except Exception as e:
        logger.error(f"Customer login error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

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
        }).eq("admin_id", admin["admin_id"]).execute()
        
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
        "version_code": 9,
        "version_name": "1.8.0",
        "download_url": f"{request.host_url}download/eden.apk",
        "force_update": True,
        "security_level": "MAXIMUM",
        "factory_reset_protection": True,
        "features": [
            "Maximum Factory Reset Protection",
            "New Authentication System",
            "Admin Registration Flow", 
            "Enhanced Security Monitoring",
            "Automatic Loan Balance Verification",
            "Persistent Device Protection"
        ],
        "changelog": "Updated for new authentication system with admin registration flow and enhanced security"
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

# ============================================
# HEALTH CHECK
# ============================================

@app.route("/health")
def health():
    from datetime import datetime
    return jsonify({"status": "healthy", "timestamp": str(datetime.now())})

if __name__ == "__main__":
    port = int(os.getenv("PORT", 10000))
    app.run(host="0.0.0.0", port=port, debug=False)
