package com.eden.mkopa

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.work.*
import java.util.concurrent.TimeUnit

class SyncService : Service() {
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scheduleSyncWork()
        return START_STICKY
    }
    
    private fun scheduleSyncWork() {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "device_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}

class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    
    override fun doWork(): Result {
        // Sync device status with backend
        val prefs = applicationContext.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        val deviceCode = prefs.getString("device_code", "") ?: ""
        
        if (deviceCode.isEmpty()) return Result.failure()
        
        // TODO: API call to check lock status
        // If payment received, unlock device
        
        return Result.success()
    }
}
