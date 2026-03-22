package com.eden.mkopa

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SetupWizardActivity : AppCompatActivity() {
    
    private var tapCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var welcomeText: TextView
    private lateinit var instructionText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_wizard)
        
        welcomeText = findViewById(R.id.welcomeText)
        instructionText = findViewById(R.id.instructionText)
        
        // Check if already device owner
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        
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
                instructionText.text = "Tap 2 more times..."
                instructionText.visibility = View.VISIBLE
            }
            2 -> {
                instructionText.text = "Tap 1 more time..."
            }
            3 -> {
                instructionText.text = "Activating Device Owner..."
                activateDeviceOwner()
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
    
    private fun activateDeviceOwner() {
        try {
            val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
            
            // This requires the device to be factory reset and the app to be installed
            // via adb with device owner provisioning
            // Command: adb shell dpm set-device-owner com.eden.mkopa/.DeviceAdminReceiver
            
            if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
                setupDeviceOwner()
                Toast.makeText(this, "Device Owner Activated!", Toast.LENGTH_LONG).show()
                
                handler.postDelayed({
                    startMainActivity()
                }, 2000)
            } else {
                // Show instructions for manual activation
                instructionText.text = """
                    Device Owner Setup Required
                    
                    Connect device to computer and run:
                    adb shell dpm set-device-owner com.eden.mkopa/.DeviceAdminReceiver
                    
                    Then restart this app.
                """.trimIndent()
                
                Toast.makeText(this, "Manual setup required via ADB", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupDeviceOwner() {
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        
        try {
            // Block uninstall
            devicePolicyManager.setUninstallBlocked(adminComponent, packageName, true)
            
            // Add user restrictions
            devicePolicyManager.addUserRestriction(adminComponent, "no_factory_reset")
            devicePolicyManager.addUserRestriction(adminComponent, "no_safe_boot")
            devicePolicyManager.addUserRestriction(adminComponent, "no_debugging_features")
            devicePolicyManager.addUserRestriction(adminComponent, "no_usb_file_transfer")
            
            // Set lock task packages
            devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf(packageName))
            
            // Enable factory reset protection
            devicePolicyManager.setFactoryResetProtectionPolicy(
                adminComponent,
                android.app.admin.FactoryResetProtectionPolicy.Builder()
                    .setFactoryResetProtectionEnabled(true)
                    .setFactoryResetProtectionAccounts(listOf())
                    .build()
            )
            
            // Save setup complete flag
            val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("device_owner_setup", true).apply()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
