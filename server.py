import os
from flask import Flask, jsonify, render_template, request
from flask_cors import CORS
from supabase import create_client
import logging
import hashlib
import secrets

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__, template_folder='templates', static_folder='static')
app.secret_key = os.getenv("SECRET_KEY", "eden-secret-key")
CORS(app)

SUPABASE_URL = os.getenv("SUPABASE_URL", "https://fvkjeteywfcppbtovbiv.supabase.co")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_KEY", os.getenv("SUPABASE_ANON_KEY", ""))

try:
# Helper functions
def hash_password(password):
    return hashlib.sha256(password.encode()).hexdigest()

def generate_token():
    return secrets.token_urlsafe(32)

def format_phone_number(phone):
    """Convert phone numbers starting with 07 to international format +254"""
    if not phone:
        return phone
    
    # Remove any spaces, dashes, or special characters
    phone = ''.join(filter(str.isdigit, phone))
    
    # If starts with 07, convert to +254
    if phone.startswith('07'):
        return '+254' + phone[1:]
    
    # If starts with 7 (without 0), add +254
    if phone.startswith('7') and len(phone) == 9:
        return '+254' + phone
    
    # If starts with 254, add +
    if phone.startswith('254'):
        return '+' + phone
    
    # If already starts with +254, return as is
    if phone.startswith('+254'):
        return phone
    
    # Otherwise return as is
    return phone
    if not supabase or not token:
        return False
    try:
        session = supabase.table("admin_sessions").select("admin_id").eq("token", token).execute()
        if not session.data:
            return False
        admin = supabase.table("admins").select("role").eq("id", session.data[0]["admin_id"]).execute()
        return admin.data and admin.data[0]["role"] == "super_admin"
    except:
        return False

@app.route("/")
def home():
    return render_template('index.html')

@app.route("/login")
def login_page():
    return render_template('login.html')

@app.route("/dashboard")
def dashboard():
    return render_template('dashboard.html')

@app.route("/admin")
def admin():
    return render_template('admin.html')

@app.route("/administrator")
def administrator():
    return render_template('admin.html')

@app.route("/super-admin")
def super_admin():
    return render_template('super_admin.html')

@app.route("/api/health")
def health():
    return jsonify({"status": "healthy"})

@app.route("/api/auth/login", methods=["POST"])
def login():
    if not supabase:
        return jsonify({"error": "Database not configured"}), 500
    data = request.json
    password_hash = hashlib.sha256(data.get("password").encode()).hexdigest()
    try:
        result = supabase.table("admins").select("*").eq("email", data.get("email")).eq("password_hash", password_hash).execute()
        if result.data and len(result.data) > 0:
            user = result.data[0]
            token = secrets.token_urlsafe(32)
            supabase.table("admin_sessions").insert({"admin_id": user["id"], "token": token}).execute()
            return jsonify({"success": True, "token": token, "user": {"id": user["id"], "email": user["email"], "role": user["role"], "full_name": user.get("full_name")}})
        return jsonify({"success": False, "error": "Invalid credentials"}), 401
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/api/auth/create-admin", methods=["POST"])
def create_admin():
    if not supabase:
        return jsonify({"error": "Database not configured"}), 500
    token = request.headers.get("Authorization", "").replace("Bearer ", "")
    if not verify_super_admin(token):
        return jsonify({"error": "Unauthorized"}), 403
    data = request.json
    password_hash = hashlib.sha256(data.get("password").encode()).hexdigest()
    try:
        result = supabase.table("admins").insert({"email": data.get("email"), "password_hash": password_hash, "full_name": data.get("full_name"), "role": "admin"}).execute()
        return jsonify({"success": True, "data": result.data})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/api/auth/admins", methods=["GET"])
def get_admins():
    if not supabase:
        return jsonify([])
    token = request.headers.get("Authorization", "").replace("Bearer ", "")
    if not verify_super_admin(token):
        return jsonify({"error": "Unauthorized"}), 403
    try:
        result = supabase.table("admins").select("id,email,full_name,role,created_at").execute()
        return jsonify(result.data)
    except Exception as e:
        return jsonify([])

@app.route("/api/stats")
def stats():
    if not supabase:
        return jsonify({"totalDevices": 0, "activeDevices": 0, "totalCustomers": 0, "totalRevenue": 0})
    try:
        devices = supabase.table("devices").select("*", count="exact").execute()
        customers = supabase.table("customers").select("*", count="exact").execute()
        payments = supabase.table("payment_transactions").select("amount").execute()
        total_revenue = sum(float(p["amount"]) for p in payments.data) if payments.data else 0
        active_devices = len([d for d in devices.data if d["status"] == "active"]) if devices.data else 0
        return jsonify({"totalDevices": devices.count or 0, "activeDevices": active_devices, "totalCustomers": customers.count or 0, "totalRevenue": total_revenue})
    except:
        return jsonify({"totalDevices": 0, "activeDevices": 0, "totalCustomers": 0, "totalRevenue": 0})

@app.route("/api/devices/enroll", methods=["POST"])
def enroll_device():
    if not supabase:
        return jsonify({"error": "Database not configured"}), 500
    token = request.headers.get("Authorization", "").replace("Bearer ", "")
    if not token:
        return jsonify({"error": "Unauthorized"}), 403
    try:
        session = supabase.table("admin_sessions").select("admin_id").eq("token", token).execute()
        if not session.data:
            return jsonify({"error": "Unauthorized"}), 403
        data = request.json
        serial = data.get("serial_number", "")
        device_id = f"DEV{serial[-6:]}" if len(serial) >= 6 else f"DEV{serial}"
        national_id = data.get("national_id", "")
        customer_id = f"CUST{national_id[-6:]}" if len(national_id) >= 6 else f"CUST{national_id}"
        device_data = {
            "device_id": device_id,
            "serial_number": data.get("serial_number"),
            "customer_id": customer_id,
            "national_id": data.get("national_id"),
            "customer_name": data.get("customer_name"),
            "customer_phone": data.get("customer_phone"),
            "total_amount": data.get("total_amount"),
            "amount_paid": data.get("amount_paid", 0),
            "id_front_url": data.get("id_front", ""),
            "id_back_url": data.get("id_back", ""),
            "passport_photo_url": data.get("passport_photo", ""),
            "status": "active"
        }
        result = supabase.table("devices").insert(device_data).execute()
        return jsonify({"success": True, "device_id": device_id, "customer_id": customer_id, "data": result.data})
    except Exception as e:
        logger.error(f"Enrollment error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/api/devices", methods=["GET"])
def get_devices():
    if not supabase:
@app.route("/api/customer/check-phone", methods=["POST"])
def check_phone():
    try:
        data = request.json
        phone_number = format_phone_number(data.get("phone_number"))
        
        if not phone_number:
            return jsonify({"exists": False, "error": "Phone number required"}), 400
        
        device_result = supabase.table("devices").select("*").eq("customer_phone", phone_number).execute()
        
        if not device_result.data or len(device_result.data) == 0:
            return jsonify({"exists": False})
        
        account_result = supabase.table("customer_accounts").select("*").eq("phone_number", phone_number).execute()
        
        has_pin = False
        if account_result.data and len(account_result.data) > 0:
            has_pin = account_result.data[0].get("is_pin_set", False)
        
        return jsonify({"exists": True, "has_pin": has_pin})
    except Exception as e:
        print(f"Check phone error: {e}")
        return jsonify({"exists": False, "error": str(e)}), 500
        result = supabase.table("devices").update({"status": "locked"}).eq("device_id", device_id).execute()
@app.route("/api/customer/set-pin", methods=["POST"])
def set_pin():
    try:
        data = request.json
        phone_number = format_phone_number(data.get("phone_number"))
        pin = data.get("pin")
    if not supabase:
        return jsonify({"error": "Database not configured"}), 500
    try:
        result = supabase.table("devices").update({"status": "active"}).eq("device_id", device_id).execute()
        return jsonify({"success": True, "message": "Device unlocked"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/api/payments", methods=["POST"])
def record_payment():
    if not supabase:
        return jsonify({"error": "Database not configured"}), 500
    try:
        data = request.json
        result = supabase.table("payment_transactions").insert({"device_id": data.get("device_id"), "amount": data.get("amount"), "payment_method": data.get("payment_method", "manual")}).execute()
        device = supabase.table("devices").select("amount_paid").eq("device_id", data.get("device_id")).execute()
        if device.data:
            new_amount = float(device.data[0]["amount_paid"]) + float(data.get("amount"))
            supabase.table("devices").update({"amount_paid": new_amount}).eq("device_id", data.get("device_id")).execute()
        return jsonify({"success": True})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/api/payments/<device_id>", methods=["GET"])
def get_payments(device_id):
    if not supabase:
        return jsonify([])
    try:
@app.route("/api/customer/login", methods=["POST"])
def customer_login():
    try:
        data = request.json
        phone_number = format_phone_number(data.get("phone_number"))
        pin = data.get("pin")
    port = int(os.getenv("PORT", "10000"))
    app.run(host="0.0.0.0", port=port)
@app.route("/app", methods=["GET"])
def app_redirect():
    """Redirect /app to APK download"""
    return redirect("/download/eden.apk")

@app.route("/app/<path:filename>", methods=["GET"])
def serve_app_folder(filename):
    """Serve files from app folder"""
    try:
        app_folder = os.path.join(os.getcwd(), "app")
        if not os.path.exists(app_folder):
            return jsonify({"error": "App folder not found"}), 404
        
        return send_from_directory(
            app_folder,
            filename,
            as_attachment=True if filename.endswith('.apk') else False,
            mimetype="application/vnd.android.package-archive" if filename.endswith('.apk') else None
        )
    except Exception as e:
        print(f"App folder serve error: {e}")
@app.route("/api/customer/dashboard", methods=["GET"])
def customer_dashboard():
    try:
        phone = format_phone_number(request.args.get("phone"))
        
        if not phone:
            return jsonify({"error": "Phone number required"}), 400
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        # Update both status and is_locked for instant unlock
        supabase.table("devices").update({
            "status": "active",
            "is_locked": False
        }).eq("device_id", device_id).execute()
        
        return jsonify({"success": True, "message": "Device unlocked", "is_locked": False})
    except Exception as e:
        print(f"Unlock device error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500        data = request.json
        serial_number = data.get("serial_number")
@app.route("/api/customer/payments", methods=["GET"])
def customer_payments():
    try:
        phone = format_phone_number(request.args.get("phone"))
        
        if not phone:
            return jsonify([])_back")
        passport_photo = data.get("passport_photo")@app.route("/api/devices/<device_id>/lock", methods=["POST"])
def lock_device(device_id):
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        # Update both status and is_locked for instant locking
        supabase.table("devices").update({
            "status": "locked",
            "is_locked": True
        }).eq("device_id", device_id).execute()
        
        return jsonify({"success": True, "message": "Device locked", "is_locked": True})
    except Exception as e:
        print(f"Lock device error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500# ============================================
# APP UPDATE ENDPOINT
# ============================================

@app.route("/api/app/version", methods=["GET"])
def get_app_version():
    """Return current app version for OTA updates"""
    return jsonify({
        "version_code": 2,  # Increment this with each release
        "version_name": "1.1.0",
        "download_url": f"{request.host_url}download/eden.apk",
        "force_update": False
    })

# ============================================
# APK DOWNLOAD ENDPOINT
# ============================================