package com.eden.mkopa

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject

class SetupWizardActivity : AppCompatActivity() {
    
    private var tapCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var welcomeText: TextView
    private lateinit var instructionText: TextView
    private lateinit var qrCodeImage: ImageView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_wizard)
        
        welcomeText = findViewById(R.id.welcomeText)
        instructionText = findViewById(R.id.instructionText)
        qrCodeImage = findViewById(R.id.qrCodeImage)
        
        // Check if already device owner
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            // Already setup, go to main activity
            startMainActivity()
            return
        }
        
        // Setup tap listener
        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnClickListener {
            handleTap()
        }
    }
    
    private fun handleTap() {
        tapCount++
        
        when (tapCount) {
            1 -> {
                instructionText.text = "Tap 2 more times to show QR code..."
                instructionText.visibility = View.VISIBLE
            }
            2 -> {
                instructionText.text = "Tap 1 more time..."
            }
            3 -> {
                showQRCode()
            }
        }
        
        // Reset tap count after 3 seconds
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            if (tapCount < 3) {
                tapCount = 0
                instructionText.visibility = View.GONE
            }
        }, 3000)
    }
    
    private fun showQRCode() {
        try {
            // Generate provisioning QR code
            val provisioningData = JSONObject().apply {
                put("android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME", 
                    "com.eden.mkopa/.DeviceAdminReceiver")
                put("android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION",
                    "https://eden-mkopa.onrender.com/download/eden.apk")
                put("android.app.extra.PROVISIONING_SKIP_ENCRYPTION", true)
                put("android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED", true)
                put("android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE", JSONObject().apply {
                    put("device_id", android.os.Build.SERIAL)
                    put("setup_time", System.currentTimeMillis())
                })
            }
            
            val qrContent = provisioningData.toString()
            val qrBitmap = generateQRCode(qrContent, 512, 512)
            
            qrCodeImage.setImageBitmap(qrBitmap)
            qrCodeImage.visibility = View.VISIBLE
            welcomeText.visibility = View.GONE
            instructionText.text = """
                Administrator: Scan this QR code
                
                1. Open device enrollment app
                2. Scan this QR code
                3. Device will provision automatically
                
                After provisioning, device will:
                • Block factory reset
                • Block ADB access
                • Block uninstall
                • Survive factory reset
            """.trimIndent()
            
            Toast.makeText(this, "QR Code Ready - Administrator should scan now", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error generating QR code: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun generateQRCode(content: String, width: Int, height: Int): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }
    
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onBackPressed() {
        // Disable back button during setup
    }
}
