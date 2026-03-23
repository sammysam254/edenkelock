package com.eden.mkopa

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                // Start sync worker
                SyncWorker.schedule(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            try {
                // Check if PIN has been completed
                val prefs = context.getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
                val pinCompleted = prefs.getBoolean("pin_completed", false)
                
                // Launch appropriate activity
                val launchIntent = if (pinCompleted) {
                    Intent(context, MainActivity::class.java)
                } else {
                    Intent(context, PinEntryActivity::class.java)
                }
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
