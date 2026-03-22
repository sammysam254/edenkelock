package com.eden.mkopa

import android.content.Context
import android.content.Intent
import androidx.work.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    
    override fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", null)
        
        if (deviceId == null) {
            return Result.success()
        }
        
        try {
            val apiService = ApiService.create()
            val response = apiService.getDeviceStatus(deviceId).execute()
            
            if (response.isSuccessful) {
                val deviceStatus = response.body()
                deviceStatus?.let {
                    // Save lock status
                    prefs.edit().putBoolean("is_locked", it.is_locked).apply()
                    
                    // If device is locked, show lock screen
                    if (it.is_locked) {
                        val intent = Intent(applicationContext, LockScreenActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        applicationContext.startActivity(intent)
                    }
                }
            }
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
    
    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "device_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
