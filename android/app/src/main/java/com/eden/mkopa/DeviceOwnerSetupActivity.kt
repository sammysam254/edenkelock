package com.eden.mkopa

import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DeviceOwnerSetupActivity : AppCompatActivity() {
    
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_owner_setup)
        
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        
        val statusText = findViewById<TextView>(R.id.statusText)
        val instructionsText = findViewById<TextView>(R.id.instructionsText)
        val commandText = findViewById<TextView>(R.id.commandText)
        val copyButton = findViewById<Button>(R.id.copyCommandButton)
        val checkButton = findViewById<Button>(R.id.checkStatusButton)
        
        // Check current status
        updateStatus(statusText)
        
        // Setup instructions
        instructionsText.text = """
            To enable device locking, Eden must be set as Device Owner.
            
            Choose one of these methods:
            
            ═══════════════════════════════════════
            METHOD 1: QR Code Provisioning (Recommended)
            ═══════════════════════════════════════
            
            1. Install this app on the device
            2. Factory reset the device
            3. During setup, tap screen 6 times
            4. QR scanner appears
            5. Admin scans QR from dashboard
            6. Device auto-configures as Device Owner
            7. Done! Device locks automatically
            
            ═══════════════════════════════════════
            METHOD 2: ADB Command (For Testing)
            ═══════════════════════════════════════
            
            1. Enable USB Debugging:
               Settings → About Phone → Tap Build Number 7 times
               Settings → Developer Options → USB Debugging ON
            
            2. Connect device to computer via USB
            
            3. Copy the command below
            
            4. Open Command Prompt/Terminal on computer
            
            5. Paste and run the command
            
            6. Click "Check Status" below
            
            After setup with either method, device will lock automatically.
        """.trimIndent()
        
        // ADB command
        val adbCommand = "adb shell dpm set-device-owner com.eden.mkopa/.DeviceAdminReceiver"
        commandText.text = adbCommand
        
        // Copy button
        copyButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ADB Command", adbCommand)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        
        // Check status button
        checkButton.setOnClickListener {
            updateStatus(statusText)
            
            if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
                Toast.makeText(this, "✓ Device Owner setup complete! Restarting app...", Toast.LENGTH_LONG).show()
                
                // Apply restrictions
                try {
                    devicePolicyManager.addUserRestriction(adminComponent, "no_factory_reset")
                    devicePolicyManager.addUserRestriction(adminComponent, "no_safe_boot")
                    devicePolicyManager.addUserRestriction(adminComponent, "no_debugging_features")
                    devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf(packageName))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Restart to main activity
                android.os.Handler().postDelayed({
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                }, 2000)
            } else {
                Toast.makeText(this, "Device Owner not set yet. Follow the steps above.", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateStatus(statusText: TextView) {
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            statusText.text = "✓ STATUS: Device Owner (Device locking enabled)"
            statusText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else if (devicePolicyManager.isAdminActive(adminComponent)) {
            statusText.text = "⚠ STATUS: Device Admin (Device locking NOT enabled)"
            statusText.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
        } else {
            statusText.text = "✗ STATUS: Not configured (Device locking NOT enabled)"
            statusText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        }
    }
}
