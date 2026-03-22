package com.eden.mkopa

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.widget.Toast

class DeviceAdminReceiver : DeviceAdminReceiver() {
    
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Eden Device Admin Enabled", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Eden Device Admin Disabled", Toast.LENGTH_SHORT).show()
    }
    
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, DeviceAdminReceiver::class.java)
        
        // Extract provisioning extras if available (device_id, serial_number from QR code)
        // Note: Extras may not be present if using minimal QR code format
        val adminExtras = intent.getBundleExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)
        val deviceId = adminExtras?.getString("device_id")
        val serialNumber = adminExtras?.getString("serial_number")
        
        // Save device info if available
        val prefs = context.getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (deviceId != null) putString("device_id", deviceId)
            if (serialNumber != null) putString("serial_number", serialNumber)
            putLong("provisioning_time", System.currentTimeMillis())
            putBoolean("provisioned_via_qr", true)
            apply()
        }
        
        // Apply all restrictions
        setupDeviceOwner(context, devicePolicyManager, adminComponent)
        
        // Start main activity
        val launchIntent = Intent(context, MainActivity::class.java)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }
    
    private fun setupDeviceOwner(
        context: Context,
        devicePolicyManager: DevicePolicyManager,
        adminComponent: ComponentName
    ) {
        try {
            // Block uninstall - CANNOT REMOVE APP
            devicePolicyManager.setUninstallBlocked(adminComponent, context.packageName, true)
            
            // Add ALL critical user restrictions
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_REMOVE_USER)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_MODIFY_ACCOUNTS)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_CREDENTIALS)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_REMOVE_MANAGED_PROFILE)
            
            // Hide settings options
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_UNMUTE_MICROPHONE)
            
            // Set lock task packages (kiosk mode)
            devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
            
            // Enable factory reset protection - SURVIVES FACTORY RESET
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                devicePolicyManager.setFactoryResetProtectionPolicy(
                    adminComponent,
                    android.app.admin.FactoryResetProtectionPolicy.Builder()
                        .setFactoryResetProtectionEnabled(true)
                        .setFactoryResetProtectionAccounts(listOf())
                        .build()
                )
            }
            
            // Set persistent preferred activities (make Eden the default launcher)
            devicePolicyManager.clearPackagePersistentPreferredActivities(adminComponent, context.packageName)
            
            // Save APK download URL for factory reset recovery
            val prefs = context.getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("apk_download_url", "https://eden-mkopa.onrender.com/download/eden.apk")
                putBoolean("device_owner_setup", true)
                apply()
            }
            
            Toast.makeText(context, "Device Owner Setup Complete - Factory Reset Blocked", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Setup Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Toast.makeText(context, "Device Locked", Toast.LENGTH_SHORT).show()
    }
    
    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Toast.makeText(context, "Device Unlocked", Toast.LENGTH_SHORT).show()
    }
}
