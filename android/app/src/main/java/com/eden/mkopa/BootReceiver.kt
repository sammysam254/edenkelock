package com.eden.mkopa

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start sync service
            val serviceIntent = Intent(context, SyncService::class.java)
            context.startService(serviceIntent)
            
            // Check lock status and show lock screen if needed
            val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
            val isLocked = prefs.getBoolean("is_locked", true)
            
            if (isLocked) {
                LockScreenActivity.show(context)
            }
        }
    }
}
