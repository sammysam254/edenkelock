package com.eden.mkopa

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        
        // Check if device owner
        if (!devicePolicyManager.isDeviceOwnerApp(packageName)) {
            // Show provisioning instructions
            showProvisioningInstructions()
        } else {
            setupDeviceOwner()
            checkLockStatus()
        }
    }
    
    private fun setupDeviceOwner() {
        // Disable factory reset
        devicePolicyManager.setFactoryResetProtectionPolicy(adminComponent, null)
        
        // Set user restrictions
        devicePolicyManager.addUserRestriction(adminComponent, "no_factory_reset")
        devicePolicyManager.addUserRestriction(adminComponent, "no_safe_boot")
        devicePolicyManager.addUserRestriction(adminComponent, "no_debugging_features")
        
        // Lock task mode (kiosk)
        devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf(packageName))
    }
    
    private fun checkLockStatus() {
        val prefs = getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        val isLocked = prefs.getBoolean("is_locked", true)
        
        if (isLocked) {
            LockScreenActivity.show(this)
        }
    }
    
    private fun showProvisioningInstructions() {
        // Show QR code scanning instructions
    }
}
