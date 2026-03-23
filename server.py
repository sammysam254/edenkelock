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
    return render_template("login.html")

# ============================================
# API ROUTES - AUTHENTICATION
# ============================================

@app.route("/api/login", methods=["POST"])
def api_login():
    try:
        data = request.json
        username = data.get("username")
        password = data.get("password")
        
        if not username or not password:
            return jsonify({"success": False, "error": "Username and password required"}), 400
        
        password_hash = hash_password(password)
        
        response = supabase.table("admins").select("*").eq("username", username).eq("password_hash", password_hash).execute()
        
        if response.data and len(response.data) > 0:
            admin = response.data[0]
            token = generate_token()
            
            supabase.table("admins").update({"token": token}).eq("admin_id", admin["admin_id"]).execute()
            
            return jsonify({
                "success": True,
                "token": token,
                "role": admin["role"],
                "admin_id": admin["admin_id"]
            })
        else:
            return jsonify({"success": False, "error": "Invalid credentials"}), 401
            
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
        
        response = supabase.table("customers").select("*").execute()
        return jsonify(response.data)
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
