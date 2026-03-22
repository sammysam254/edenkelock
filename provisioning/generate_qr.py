#!/usr/bin/env python3
"""
QR Code Generator for Android Device Provisioning
Generates QR codes for zero-touch enrollment
"""

import json
import qrcode
import sys

def generate_provisioning_qr(device_code, admin_email, wifi_ssid="", wifi_password=""):
    """
    Generate a QR code for device provisioning
    """
    
    # Provisioning payload
    payload = {
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": 
            "com.eden.mkopa/.DeviceAdminReceiver",
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION":
            "https://your-server.com/eden-mkopa.apk",
        "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
            "device_code": device_code,
            "admin_email": admin_email
        }
    }
    
    # Add WiFi if provided
    if wifi_ssid:
        payload["android.app.extra.PROVISIONING_WIFI_SSID"] = wifi_ssid
        payload["android.app.extra.PROVISIONING_WIFI_PASSWORD"] = wifi_password
        payload["android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE"] = "WPA"
    
    # Convert to JSON
    qr_data = json.dumps(payload)
    
    # Generate QR code
    qr = qrcode.QRCode(version=1, box_size=10, border=5)
    qr.add_data(qr_data)
    qr.make(fit=True)
    
    img = qr.make_image(fill_color="black", back_color="white")
    
    # Save QR code
    filename = f"qr_{device_code}.png"
    img.save(filename)
    
    print(f"QR code generated: {filename}")
    return filename

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python generate_qr.py <device_code> <admin_email> [wifi_ssid] [wifi_password]")
        sys.exit(1)
    
    device_code = sys.argv[1]
    admin_email = sys.argv[2]
    wifi_ssid = sys.argv[3] if len(sys.argv) > 3 else ""
    wifi_password = sys.argv[4] if len(sys.argv) > 4 else ""
    
    generate_provisioning_qr(device_code, admin_email, wifi_ssid, wifi_password)
