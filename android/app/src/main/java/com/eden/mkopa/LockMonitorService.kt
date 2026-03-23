package com.eden.mkopa

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class LockMonitorService : Service() {
    
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    
    private val checkLockStatusRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                checkLockStatus()
                handler.postDelayed(this, 2000) // Check every 2 seconds
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        handler.post(checkLockStatusRunnable)
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(checkLockStatusRunnable)
    }
    
    private fun checkLockStatus() {
        val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", null) ?: return
        
        Thread {
            try {
                val apiService = ApiService.create()
                val response = apiService.getDeviceStatus(deviceId).execute()
                
                if (response.isSuccessful) {
                    val deviceStatus = response.body()
                    deviceStatus?.let {
                        val wasLocked = prefs.getBoolean("is_locked", false)
                        prefs.edit().putBoolean("is_locked", it.is_locked).apply()
                        
                        // If device should be locked, lock it IMMEDIATELY
                        if (it.is_locked && !wasLocked) {
                            handler.post {
                                val lockIntent = Intent(this@LockMonitorService, LockScreenActivity::class.java)
                                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                startActivity(lockIntent)
                            }
                        }
                        
                        if (!it.is_locked && wasLocked) {
                            // Device unlocked, send broadcast
                            val unlockIntent = Intent("com.eden.mkopa.UNLOCK_DEVICE")
                            sendBroadcast(unlockIntent)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "eden_monitor",
                "Eden Device Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Monitors device lock status"
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "eden_monitor")
            .setContentTitle("Eden")
            .setContentText("Device monitoring active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
