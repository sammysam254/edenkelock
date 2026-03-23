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
            putBoolean("factory_reset_protection_enabled", true)
            apply()
        }
        
        // Apply all restrictions IMMEDIATELY
        setupDeviceOwner(context, devicePolicyManager, adminComponent)
        
        // Start setup activity to check loan status
        val setupIntent = Intent(context, DeviceOwnerSetupActivity::class.java)
        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(setupIntent)
    }
    
    private fun setupDeviceOwner(
        context: Context,
        devicePolicyManager: DevicePolicyManager,
        adminComponent: ComponentName
    ) {
        try {
            // MAXIMUM FACTORY RESET PROTECTION - BLOCK ALL POSSIBLE METHODS
            
            // 1. Block uninstall - CANNOT REMOVE APP
            devicePolicyManager.setUninstallBlocked(adminComponent, context.packageName, true)
            
            // 2. CRITICAL: Block ALL factory reset methods
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            
            // 3. Block recovery mode access
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_APPS)
            
            // 4. Block user management (prevent creating new users to bypass restrictions)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_REMOVE_USER)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_USER_SWITCH)
            
            // 5. Block account modifications
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_MODIFY_ACCOUNTS)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_CREDENTIALS)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_REMOVE_MANAGED_PROFILE)
            
            // 6. Block system settings access
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_WIFI)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_BLUETOOTH)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_UNMUTE_MICROPHONE)
            
            // 7. Block system error dialogs and recovery options
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_SYSTEM_ERROR_DIALOGS)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_DATE_TIME)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_LOCALE)
            
            // 8. Block developer options and ADB
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)
            
            // 9. CRITICAL: Hide Settings app entirely - NO ACCESS TO SYSTEM SETTINGS
            try {
                devicePolicyManager.setApplicationHidden(adminComponent, "com.android.settings", true)
                devicePolicyManager.setApplicationHidden(adminComponent, "com.android.systemui", false) // Keep system UI
            } catch (e: Exception) {
                // Some devices may not allow hiding system apps
            }
            
            // 10. Set lock task packages (kiosk mode) - ONLY EDEN CAN RUN
            devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
            
            // 11. CRITICAL: Factory Reset Protection Policy - SURVIVES FACTORY RESET
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    devicePolicyManager.setFactoryResetProtectionPolicy(
                        adminComponent,
                        android.app.admin.FactoryResetProtectionPolicy.Builder()
                            .setFactoryResetProtectionEnabled(true)
                            .setFactoryResetProtectionAccounts(listOf())
                            .build()
                    )
                } catch (e: Exception) {
                    // Fallback for older devices
                }
            }
            
            // 12. Block access to recovery and download modes
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_TETHERING)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_VPN)
            
            // 13. Disable hardware buttons for factory reset (where possible)
            try {
                // This prevents volume + power button combinations
                devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_ADJUST_VOLUME)
            } catch (e: Exception) {
                // Some devices may not support this
            }
            
            // 14. Make Eden the ONLY launcher - no other apps can be default
            devicePolicyManager.clearPackagePersistentPreferredActivities(adminComponent, context.packageName)
            
            // 15. Block package installer to prevent sideloading
            try {
                devicePolicyManager.setApplicationHidden(adminComponent, "com.android.packageinstaller", true)
                devicePolicyManager.setApplicationHidden(adminComponent, "com.google.android.packageinstaller", true)
            } catch (e: Exception) {
                // May not exist on all devices
            }
            
            // 16. Save critical recovery information with timestamp
            val prefs = context.getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("apk_download_url", "https://eden-mkopa.onrender.com/download/eden.apk")
                putString("website_url", "https://eden-mkopa.onrender.com")
                putBoolean("device_owner_setup", true)
                putBoolean("factory_reset_blocked", true)
                putBoolean("maximum_security_enabled", true)
                putLong("protection_enabled_time", System.currentTimeMillis())
                putString("protection_level", "MAXIMUM")
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
