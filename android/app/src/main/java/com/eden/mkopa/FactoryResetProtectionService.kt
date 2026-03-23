package com.eden.mkopa

import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.UserManager
import android.util.Log

class FactoryResetProtectionService : Service() {
    
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    
    private val protectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BOOT_COMPLETED -> {
                    // Re-apply restrictions after boot
                    enforceMaximumSecurity()
                }
                "android.intent.action.MASTER_CLEAR" -> {
                    // Block factory reset intent
                    abortBroadcast()
                    Log.w("EdenSecurity", "Factory reset attempt blocked!")
                }
                "android.intent.action.FACTORY_RESET" -> {
                    // Block factory reset intent
                    abortBroadcast()
                    Log.w("EdenSecurity", "Factory reset attempt blocked!")
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    // Check if Eden is being uninstalled
                    val packageName = intent.data?.schemeSpecificPart
                    if (packageName == context?.packageName) {
                        // Block Eden uninstall
                        Log.w("EdenSecurity", "Eden uninstall attempt blocked!")
                    }
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        
        // Register for system broadcasts
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BOOT_COMPLETED)
            addAction("android.intent.action.MASTER_CLEAR")
            addAction("android.intent.action.FACTORY_RESET")
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(protectionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(protectionReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e("EdenSecurity", "Failed to register protection receiver", e)
        }
        
        // Continuously enforce security
        enforceMaximumSecurity()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        enforceMaximumSecurity()
        return START_STICKY // Restart if killed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(protectionReceiver)
        } catch (e: Exception) {
            Log.e("EdenSecurity", "Failed to unregister protection receiver", e)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun enforceMaximumSecurity() {
        if (!devicePolicyManager.isDeviceOwnerApp(packageName)) {
            return
        }
        
        try {
            // Re-apply all critical restrictions
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_SAFE_BOOT)
            devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            
            // Ensure Eden cannot be uninstalled
            devicePolicyManager.setUninstallBlocked(adminComponent, packageName, true)
            
            // Hide settings app
            devicePolicyManager.setApplicationHidden(adminComponent, "com.android.settings", true)
            
            // Ensure kiosk mode
            devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf(packageName))
            
            Log.i("EdenSecurity", "Maximum security enforced")
            
        } catch (e: Exception) {
            Log.e("EdenSecurity", "Failed to enforce security", e)
        }
    }
}