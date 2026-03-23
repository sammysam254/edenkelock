package com.eden.mkopa

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("EdenBoot", "Device boot completed - starting security enforcement")
            
            // CRITICAL: Start protection service immediately
            startProtectionService(context)
            
            // Re-enforce all security restrictions
            enforceMaximumSecurity(context)
            
            try {
                // Start sync worker
                SyncWorker.schedule(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            try {
                // Check device status and launch appropriate activity
                val prefs = context.getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
                val isLocked = prefs.getBoolean("is_locked", false)
                
                if (isLocked) {
                    // Device is locked - show lock screen
                    val lockIntent = Intent(context, LockScreenActivity::class.java)
                    lockIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(lockIntent)
                    Log.i("EdenBoot", "Device is locked - showing lock screen")
                } else {
                    // Start device setup to check loan balance
                    val setupIntent = Intent(context, DeviceOwnerSetupActivity::class.java)
                    setupIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(setupIntent)
                    Log.i("EdenBoot", "Starting device setup to check loan balance")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun startProtectionService(context: Context) {
        try {
            val serviceIntent = Intent(context, FactoryResetProtectionService::class.java)
            context.startForegroundService(serviceIntent)
            Log.i("EdenBoot", "Factory reset protection service started")
        } catch (e: Exception) {
            Log.e("EdenBoot", "Failed to start protection service", e)
        }
    }
    
    private fun enforceMaximumSecurity(context: Context) {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, DeviceAdminReceiver::class.java)
        
        if (!devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
            Log.w("EdenBoot", "Not device owner - cannot enforce security")
            return
        }
        
        try {
            // CRITICAL: Re-apply factory reset protection after boot
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
            
            // Block uninstall
            devicePolicyManager.setUninstallBlocked(adminComponent, context.packageName, true)
            
            // Hide settings
            devicePolicyManager.setApplicationHidden(adminComponent, "com.android.settings", true)
            
            // Kiosk mode
            devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
            
            Log.i("EdenBoot", "Maximum security enforced after boot")
            
        } catch (e: Exception) {
            Log.e("EdenBoot", "Failed to enforce security", e)
        }
    }
}
