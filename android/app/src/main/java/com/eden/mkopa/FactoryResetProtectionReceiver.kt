package com.eden.mkopa

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log

class FactoryResetProtectionReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        // ============================================
        // FRP DISABLED FOR TESTING
        // TODO: Enable for production by uncommenting below
        // ============================================
        
        Log.i("EdenSecurity", "FRP Receiver triggered but DISABLED for testing: ${intent.action}")
        return
        
        /* COMMENTED OUT FOR TESTING - ENABLE FOR PRODUCTION
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i("EdenSecurity", "Boot completed - enforcing security")
                enforceMaximumSecurity(context)
                startProtectionService(context)
            }
            
            "android.intent.action.MASTER_CLEAR" -> {
                Log.w("EdenSecurity", "BLOCKING MASTER CLEAR ATTEMPT!")
                abortBroadcast()
                // Show lock screen instead
                showLockScreen(context)
            }
            
            "android.intent.action.FACTORY_RESET" -> {
                Log.w("EdenSecurity", "BLOCKING FACTORY RESET ATTEMPT!")
                abortBroadcast()
                // Show lock screen instead
                showLockScreen(context)
            }
            
            Intent.ACTION_PACKAGE_REMOVED -> {
                val packageName = intent.data?.schemeSpecificPart
                if (packageName == context.packageName) {
                    Log.w("EdenSecurity", "BLOCKING EDEN UNINSTALL ATTEMPT!")
                    // Immediately reinstall or show error
                    showLockScreen(context)
                }
            }
        }
        */
    }
    
    private fun enforceMaximumSecurity(context: Context) {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, DeviceAdminReceiver::class.java)
        
        if (!devicePolicyManager.isDeviceOwnerApp(context.packageName)) {
            return
        }
        
        try {
            // Re-apply all critical restrictions after boot
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_USB_FILE_TRANSFER)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_APPS)
            
            // Ensure Eden cannot be uninstalled
            devicePolicyManager.setUninstallBlocked(adminComponent, context.packageName, true)
            
            // Hide settings app
            devicePolicyManager.setApplicationHidden(adminComponent, "com.android.settings", true)
            
            // Ensure kiosk mode
            devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
            
            Log.i("EdenSecurity", "Maximum security re-enforced after boot")
            
        } catch (e: Exception) {
            Log.e("EdenSecurity", "Failed to re-enforce security after boot", e)
        }
    }
    
    private fun startProtectionService(context: Context) {
        try {
            val serviceIntent = Intent(context, FactoryResetProtectionService::class.java)
            context.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            Log.e("EdenSecurity", "Failed to start protection service", e)
        }
    }
    
    private fun showLockScreen(context: Context) {
        try {
            val lockIntent = Intent(context, LockScreenActivity::class.java)
            lockIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            lockIntent.putExtra("lock_reason", "SECURITY_VIOLATION")
            context.startActivity(lockIntent)
        } catch (e: Exception) {
            Log.e("EdenSecurity", "Failed to show lock screen", e)
        }
    }
}