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

@app.route("/login")
def login():
    return render_template("login.html")

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
        "version_code": 3,
        "version_name": "1.2.0",
        "download_url": f"{request.host_url}download/eden.apk",
        "force_update": False
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
