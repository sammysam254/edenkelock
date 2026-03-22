import os
import subprocess
import threading
from flask import Flask, jsonify, request
from flask_cors import CORS
from supabase import create_client
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

SUPABASE_URL = os.getenv("SUPABASE_URL", "https://fvkjeteywfcppbtovbiv.supabase.co")
SUPABASE_KEY = os.getenv("SUPABASE_SERVICE_KEY", "")
supabase = create_client(SUPABASE_URL, SUPABASE_KEY) if SUPABASE_KEY else None

def start_nextjs():
    try:
        logger.info("Starting Next.js...")
        subprocess.run(["npm", "start"], cwd="dashboard", env={**os.environ, "PORT": "3000"})
    except Exception as e:
        logger.error(f"Next.js error: {e}")

threading.Thread(target=start_nextjs, daemon=True).start()

@app.route("/api/health")
def health():
    return jsonify({"status": "healthy", "supabase": "connected" if supabase else "not configured"})

@app.route("/api/stats")
def stats():
    if not supabase:
        return jsonify({"error": "Supabase not configured"}), 500
    try:
        devices = supabase.table("devices").select("*", count="exact").execute()
        customers = supabase.table("customers").select("*", count="exact").execute()
        payments = supabase.table("payment_transactions").select("amount").execute()
        total_revenue = sum(float(p["amount"]) for p in payments.data) if payments.data else 0
        active_devices = len([d for d in devices.data if d["status"] == "active"]) if devices.data else 0
        return jsonify({
            "totalDevices": devices.count or 0,
            "activeDevices": active_devices,
            "totalCustomers": customers.count or 0,
            "totalRevenue": total_revenue
        })
    except Exception as e:
        logger.error(f"Error: {e}")
        return jsonify({"error": str(e)}), 500

@app.route("/", defaults={"path": ""})
@app.route("/<path:path>")
def proxy(path):
    import requests
    try:
        url = f"http://localhost:3000/{path}"
        if request.query_string:
            url += f"?{request.query_string.decode()}"
        resp = requests.get(url, timeout=30)
        return resp.content, resp.status_code, dict(resp.headers)
    except:
        return "Starting...", 503

if __name__ == "__main__":
    port = int(os.getenv("PORT", "10000"))
    app.run(host="0.0.0.0", port=port)
