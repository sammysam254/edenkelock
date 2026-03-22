package com.eden.mkopa

import android.app.Service
import android.content.Intent
import android.os.IBinder

class SyncService : Service() {
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Schedule sync work using SyncWorker
        SyncWorker.schedule(this)
        return START_STICKY
    }
}
