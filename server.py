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
    supabase = create_client(SUPABASE_URL, SUPABASE_KEY) if SUPABASE_KEY else None
except Exception as e:
    logger.warning(f"Supabase init failed: {e}")
    supabase = None

def verify_super_admin(token):
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
    if not verify_super_admin(token):
        return jsonify({"error": "Unauthorized"}), 403
    try:
        data = request.json
        result = supabase.table("devices").insert({"device_id": data.get("device_id"), "customer_id": data.get("customer_id"), "total_amount": data.get("total_amount"), "amount_paid": data.get("amount_paid", 0), "status": "active"}).execute()
        return jsonify({"success": True, "data": result.data})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/api/devices", methods=["GET"])
def get_devices():
    if not supabase:
        return jsonify([])
    try:
        result = supabase.table("devices").select("*").execute()
        return jsonify(result.data)
    except:
        return jsonify([])

@app.route("/api/devices/<device_id>", methods=["GET"])
def get_device(device_id):
    if not supabase:
        return jsonify({"error": "Not found"}), 404
    try:
        result = supabase.table("devices").select("*").eq("device_id", device_id).execute()
        if result.data:
            return jsonify(result.data[0])
        return jsonify({"error": "Device not found"}), 404
    except:
        return jsonify({"error": "Not found"}), 404

@app.route("/api/devices/<device_id>/lock", methods=["POST"])
def lock_device(device_id):
    if not supabase:
        return jsonify({"error": "Database not configured"}), 500
    try:
        result = supabase.table("devices").update({"status": "locked"}).eq("device_id", device_id).execute()
        return jsonify({"success": True, "message": "Device locked"})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/api/devices/<device_id>/unlock", methods=["POST"])
def unlock_device(device_id):
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
        result = supabase.table("payment_transactions").select("*").eq("device_id", device_id).order("created_at", desc=True).execute()
        return jsonify(result.data)
    except:
        return jsonify([])

if __name__ == "__main__":
    port = int(os.getenv("PORT", "10000"))
    app.run(host="0.0.0.0", port=port)
