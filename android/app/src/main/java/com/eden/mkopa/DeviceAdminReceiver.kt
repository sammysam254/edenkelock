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
            // Block uninstall
            devicePolicyManager.setUninstallBlocked(adminComponent, context.packageName, true)
            
            // Add critical user restrictions
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_ADD_USER)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_REMOVE_USER)
            
            // Set lock task packages (kiosk mode)
            devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
            
            // Enable factory reset protection
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
            
            Toast.makeText(context, "Device Owner Setup Complete", Toast.LENGTH_LONG).show()
            
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
