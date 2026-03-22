from flask import Flask, request, jsonify, render_template, send_from_directory, redirect
from flask_cors import CORS
import os
import hashlib
import secrets
from datetime import datetime, timedelta
from supabase import create_client, Client
from dotenv import load_dotenv

load_dotenv()

app = Flask(__name__)
CORS(app)

# Supabase configuration
SUPABASE_URL = os.getenv("SUPABASE_URL", "https://fvkjeteywfcppbtovbiv.supabase.co")
SUPABASE_KEY = os.getenv("SUPABASE_KEY", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZ2a2pldGV5d2ZjcHBidG92Yml2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQxOTU2NzEsImV4cCI6MjA4OTc3MTY3MX0.5pOcpCSWn98Vvmq4IBQkWWv-nvvA6zbeUZXjSQ3cfC0")
supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

# Helper functions
def hash_password(password):
    return hashlib.sha256(password.encode()).hexdigest()

def generate_token():
    return secrets.token_urlsafe(32)

def verify_admin_token(token):
    try:
        result = supabase.table("admin_sessions").select("*").eq("token", token).execute()
        if result.data and len(result.data) > 0:
            session = result.data[0]
            admin_result = supabase.table("admins").select("*").eq("id", session["admin_id"]).execute()
            if admin_result.data and len(admin_result.data) > 0:
                return admin_result.data[0]
        return None
    except Exception as e:
        print(f"Token verification error: {e}")
        return None

def verify_customer_token(token):
    try:
        result = supabase.table("customer_sessions").select("*").eq("token", token).execute()
        if result.data and len(result.data) > 0:
            return result.data[0]
        return None
    except Exception as e:
        print(f"Customer token verification error: {e}")
        return None

# ============================================
# TEMPLATE ROUTES
# ============================================

@app.route("/")
def index():
    return render_template("index.html")

@app.route("/login")
def login_page():
    return render_template("login.html")

@app.route("/super-admin")
def super_admin_page():
    return render_template("super_admin.html")

@app.route("/admin")
def admin_page():
    return render_template("admin.html")

@app.route("/customer-login")
def customer_login_page():
    return render_template("customer_login.html")

@app.route("/dashboard")
def dashboard_page():
    return render_template("dashboard.html")

@app.route("/static/<path:path>")
def send_static(path):
    return send_from_directory("static", path)

# ============================================
# AUTHENTICATION ENDPOINTS
# ============================================

@app.route("/api/auth/login", methods=["POST"])
def admin_login():
    try:
        data = request.json
        email = data.get("email")
        password = data.get("password")
        
        if not email or not password:
            return jsonify({"success": False, "error": "Email and password required"}), 400
        
        password_hash = hash_password(password)
        
        result = supabase.table("admins").select("*").eq("email", email).eq("password_hash", password_hash).execute()
        
        if not result.data or len(result.data) == 0:
            return jsonify({"success": False, "error": "Invalid credentials"}), 401
        
        admin = result.data[0]
        token = generate_token()
        
        supabase.table("admin_sessions").insert({
            "admin_id": admin["id"],
            "token": token
        }).execute()
        
        return jsonify({
            "success": True,
            "token": token,
            "user": {
                "id": admin["id"],
                "email": admin["email"],
                "full_name": admin.get("full_name"),
                "role": admin.get("role", "admin")
            }
        })
    except Exception as e:
        print(f"Login error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/auth/create-admin", methods=["POST"])
def create_admin():
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin or admin.get("role") != "super_admin":
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        data = request.json
        email = data.get("email")
        password = data.get("password")
        full_name = data.get("full_name")
        
        if not email or not password:
            return jsonify({"success": False, "error": "Email and password required"}), 400
        
        password_hash = hash_password(password)
        
        result = supabase.table("admins").insert({
            "email": email,
            "password_hash": password_hash,
            "full_name": full_name,
            "role": "admin"
        }).execute()
        
        return jsonify({"success": True, "admin": result.data[0]})
    except Exception as e:
        print(f"Create admin error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/auth/admins", methods=["GET"])
def get_admins():
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin or admin.get("role") != "super_admin":
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        result = supabase.table("admins").select("id, email, full_name, role, created_at").execute()
        return jsonify(result.data)
    except Exception as e:
        print(f"Get admins error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/auth/admins/<admin_id>", methods=["DELETE"])
def delete_admin(admin_id):
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin or admin.get("role") != "super_admin":
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        supabase.table("admins").delete().eq("id", admin_id).execute()
        return jsonify({"success": True})
    except Exception as e:
        print(f"Delete admin error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

# ============================================
# CUSTOMER AUTHENTICATION ENDPOINTS
# ============================================

@app.route("/api/customer/check-phone", methods=["POST"])
def check_phone():
    try:
        data = request.json
        phone_number = data.get("phone_number")
        
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

@app.route("/api/customer/set-pin", methods=["POST"])
def set_pin():
    try:
        data = request.json
        phone_number = data.get("phone_number")
        pin = data.get("pin")
        
        if not phone_number or not pin:
            return jsonify({"success": False, "error": "Phone and PIN required"}), 400
        
        if len(pin) != 4:
            return jsonify({"success": False, "error": "PIN must be 4 digits"}), 400
        
        pin_hash = hash_password(pin)
        
        account_result = supabase.table("customer_accounts").select("*").eq("phone_number", phone_number).execute()
        
        if account_result.data and len(account_result.data) > 0:
            supabase.table("customer_accounts").update({
                "pin_hash": pin_hash,
                "is_pin_set": True
            }).eq("phone_number", phone_number).execute()
        else:
            supabase.table("customer_accounts").insert({
                "phone_number": phone_number,
                "pin_hash": pin_hash,
                "is_pin_set": True
            }).execute()
        
        return jsonify({"success": True})
    except Exception as e:
        print(f"Set PIN error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/customer/login", methods=["POST"])
def customer_login():
    try:
        data = request.json
        phone_number = data.get("phone_number")
        pin = data.get("pin")
        
        if not phone_number or not pin:
            return jsonify({"success": False, "error": "Phone and PIN required"}), 400
        
        pin_hash = hash_password(pin)
        
        account_result = supabase.table("customer_accounts").select("*").eq("phone_number", phone_number).eq("pin_hash", pin_hash).execute()
        
        if not account_result.data or len(account_result.data) == 0:
            return jsonify({"success": False, "error": "Invalid PIN"}), 401
        
        token = generate_token()
        
        supabase.table("customer_sessions").insert({
            "phone_number": phone_number,
            "token": token
        }).execute()
        
        return jsonify({"success": True, "token": token})
    except Exception as e:
        print(f"Customer login error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/customer/dashboard", methods=["GET"])
def customer_dashboard():
    try:
        phone = request.args.get("phone")
        
        if not phone:
            return jsonify({"error": "Phone number required"}), 400
        
        device_result = supabase.table("devices").select("*").eq("customer_phone", phone).execute()
        
        if not device_result.data or len(device_result.data) == 0:
            return jsonify({"error": "No device found"}), 404
        
        device = device_result.data[0]
        
        return jsonify({"device": device})
    except Exception as e:
        print(f"Customer dashboard error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/api/customer/payments", methods=["GET"])
def customer_payments():
    try:
        phone = request.args.get("phone")
        
        if not phone:
            return jsonify([])
        
        device_result = supabase.table("devices").select("id").eq("customer_phone", phone).execute()
        
        if not device_result.data or len(device_result.data) == 0:
            return jsonify([])
        
        device_id = device_result.data[0]["id"]
        
        payments_result = supabase.table("payment_transactions").select("*").eq("device_id", device_id).order("created_at", desc=True).execute()
        
        return jsonify(payments_result.data)
    except Exception as e:
        print(f"Customer payments error: {e}")
        return jsonify({"error": str(e)}), 500

# ============================================
# DEVICE ENDPOINTS
# ============================================

@app.route("/api/devices", methods=["GET"])
def get_devices():
    try:
        result = supabase.table("devices").select("*").order("created_at", desc=True).execute()
        return jsonify(result.data)
    except Exception as e:
        print(f"Get devices error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/api/devices/enroll", methods=["POST"])
def enroll_device():
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        data = request.json
        serial_number = data.get("serial_number")
        national_id = data.get("national_id")
        customer_name = data.get("customer_name")
        customer_phone = data.get("customer_phone")
        total_amount = data.get("total_amount")
        amount_paid = data.get("amount_paid", 0)
        id_front = data.get("id_front")
        id_back = data.get("id_back")
        passport_photo = data.get("passport_photo")
        
        if not all([serial_number, national_id, customer_name, customer_phone, total_amount]):
            return jsonify({"success": False, "error": "Missing required fields"}), 400
        
        device_id = f"DEV-{serial_number[-8:]}"
        customer_id = f"CUST-{national_id[-6:]}"
        
        device_data = {
            "device_id": device_id,
            "customer_id": customer_id,
            "serial_number": serial_number,
            "national_id": national_id,
            "customer_name": customer_name,
            "customer_phone": customer_phone,
            "total_amount": total_amount,
            "amount_paid": amount_paid,
            "status": "active" if amount_paid >= total_amount else "locked",
            "id_front_url": id_front,
            "id_back_url": id_back,
            "passport_photo_url": passport_photo
        }
        
        result = supabase.table("devices").insert(device_data).execute()
        
        account_check = supabase.table("customer_accounts").select("*").eq("phone_number", customer_phone).execute()
        if not account_check.data or len(account_check.data) == 0:
            supabase.table("customer_accounts").insert({
                "phone_number": customer_phone,
                "is_pin_set": False
            }).execute()
        
        return jsonify({"success": True, "device_id": device_id, "device": result.data[0]})
    except Exception as e:
        print(f"Enroll device error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/devices/<device_id>/status", methods=["GET"])
def get_device_status(device_id):
    try:
        result = supabase.table("devices").select("*").eq("device_id", device_id).execute()
        
        if not result.data or len(result.data) == 0:
            return jsonify({"error": "Device not found"}), 404
        
        device = result.data[0]
        balance = device["total_amount"] - device["amount_paid"]
        
        return jsonify({
            "device_id": device["device_id"],
            "status": device["status"],
            "is_locked": balance > 0,
            "balance": balance,
            "total_amount": device["total_amount"],
            "amount_paid": device["amount_paid"]
        })
    except Exception as e:
        print(f"Get device status error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/api/devices/<device_id>/lock", methods=["POST"])
def lock_device(device_id):
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        supabase.table("devices").update({"status": "locked"}).eq("device_id", device_id).execute()
        
        return jsonify({"success": True, "message": "Device locked"})
    except Exception as e:
        print(f"Lock device error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

@app.route("/api/devices/<device_id>/unlock", methods=["POST"])
def unlock_device(device_id):
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        supabase.table("devices").update({"status": "active"}).eq("device_id", device_id).execute()
        
        return jsonify({"success": True, "message": "Device unlocked"})
    except Exception as e:
        print(f"Unlock device error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

# ============================================
# PAYMENT ENDPOINTS
# ============================================

@app.route("/api/payments", methods=["POST"])
def record_payment():
    try:
        token = request.headers.get("Authorization", "").replace("Bearer ", "")
        admin = verify_admin_token(token)
        
        if not admin:
            return jsonify({"success": False, "error": "Unauthorized"}), 403
        
        data = request.json
        device_id = data.get("device_id")
        amount = data.get("amount")
        
        if not device_id or not amount:
            return jsonify({"success": False, "error": "Device ID and amount required"}), 400
        
        device_result = supabase.table("devices").select("*").eq("device_id", device_id).execute()
        
        if not device_result.data or len(device_result.data) == 0:
            return jsonify({"success": False, "error": "Device not found"}), 404
        
        device = device_result.data[0]
        new_amount_paid = device["amount_paid"] + amount
        new_status = "active" if new_amount_paid >= device["total_amount"] else device["status"]
        
        supabase.table("devices").update({
            "amount_paid": new_amount_paid,
            "status": new_status
        }).eq("device_id", device_id).execute()
        
        supabase.table("payment_transactions").insert({
            "device_id": device["id"],
            "customer_id": device["customer_id"],
            "amount": amount,
            "payment_method": data.get("payment_method", "mpesa"),
            "status": "completed"
        }).execute()
        
        return jsonify({"success": True, "new_balance": device["total_amount"] - new_amount_paid})
    except Exception as e:
        print(f"Record payment error: {e}")
        return jsonify({"success": False, "error": str(e)}), 500

# ============================================
# STATS ENDPOINT
# ============================================

@app.route("/api/stats", methods=["GET"])
def get_stats():
    try:
        devices_result = supabase.table("devices").select("*").execute()
        devices = devices_result.data
        
        total_devices = len(devices)
        active_devices = len([d for d in devices if d.get("status") == "active"])
        total_customers = len(set([d.get("customer_id") for d in devices if d.get("customer_id")]))
        total_revenue = sum([d.get("amount_paid", 0) for d in devices])
        
        return jsonify({
            "totalDevices": total_devices,
            "activeDevices": active_devices,
            "totalCustomers": total_customers,
            "totalRevenue": total_revenue
        })
    except Exception as e:
        print(f"Get stats error: {e}")
        return jsonify({
            "totalDevices": 0,
            "activeDevices": 0,
            "totalCustomers": 0,
            "totalRevenue": 0
        })


# ============================================
# APK DOWNLOAD ENDPOINT
# ============================================

@app.route("/download/eden.apk", methods=["GET"])
def download_apk():
    """Public endpoint to download Eden APK for factory reset recovery"""
    try:
        # Try static/apk folder first
        apk_path = os.path.join(os.getcwd(), "static", "apk", "eden.apk")
        
        # If not in static, try android build folder
        if not os.path.exists(apk_path):
            apk_path = os.path.join(os.getcwd(), "android", "app", "build", "outputs", "apk", "release", "app-release.apk")
        
        # If release doesn't exist, try debug
        if not os.path.exists(apk_path):
            apk_path = os.path.join(os.getcwd(), "android", "app", "build", "outputs", "apk", "debug", "app-debug.apk")
        
        # If still doesn't exist, return error
        if not os.path.exists(apk_path):
            return jsonify({"error": "APK not found. Please build and copy APK to static/apk/eden.apk"}), 404
        
        return send_from_directory(
            os.path.dirname(apk_path),
            os.path.basename(apk_path),
            as_attachment=True,
            download_name="eden.apk",
            mimetype="application/vnd.android.package-archive"
        )
    except Exception as e:
        print(f"APK download error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/app", methods=["GET"])
def app_redirect():
    """Redirect /app to APK download"""
    return redirect("/download/eden.apk")
# ============================================
# HEALTH CHECK
# ============================================

@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "healthy", "timestamp": datetime.now().isoformat()})

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 10000))
    app.run(host="0.0.0.0", port=port, debug=False)





