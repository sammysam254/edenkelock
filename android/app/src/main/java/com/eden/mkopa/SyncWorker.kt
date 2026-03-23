package com.eden.mkopa

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.work.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.net.URL
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
                    val wasLocked = prefs.getBoolean("is_locked", false)
                    prefs.edit().putBoolean("is_locked", it.is_locked).apply()
                    
                    // If device is locked, show lock screen IMMEDIATELY
                    if (it.is_locked) {
                        val intent = Intent(applicationContext, LockScreenActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        applicationContext.startActivity(intent)
                    } else if (wasLocked && !it.is_locked) {
                        // Device was unlocked, close lock screen
                        val intent = Intent("com.eden.mkopa.UNLOCK_DEVICE")
                        applicationContext.sendBroadcast(intent)
                    }
                    
                    // Check for app updates
                    checkForUpdates()
                }
            }
            
            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
    
    private fun checkForUpdates() {
        try {
            val currentVersion = applicationContext.packageManager
                .getPackageInfo(applicationContext.packageName, 0).versionCode
            
            val apiService = ApiService.create()
            val response = apiService.checkAppVersion().execute()
            
            if (response.isSuccessful) {
                val versionInfo = response.body()
                versionInfo?.let {
                    if (it.version_code > currentVersion) {
                        // Download and install update
                        downloadAndInstallUpdate(it.download_url)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun downloadAndInstallUpdate(downloadUrl: String) {
        try {
            val url = URL(downloadUrl)
            val connection = url.openConnection()
            connection.connect()
            
            val file = File(applicationContext.cacheDir, "eden_update.apk")
            val output = FileOutputStream(file)
            val input = connection.getInputStream()
            
            input.copyTo(output)
            output.close()
            input.close()
            
            // Install APK silently (requires device owner)
            installApkSilently(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun installApkSilently(apkFile: File) {
        try {
            val packageInstaller = applicationContext.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            
            val output = session.openWrite("eden", 0, -1)
            val input = apkFile.inputStream()
            input.copyTo(output)
            session.fsync(output)
            output.close()
            input.close()
            
            val intent = Intent(applicationContext, applicationContext.javaClass)
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                applicationContext, sessionId, intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    android.app.PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }
            )
            
            session.commit(pendingIntent.intentSender)
            session.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES  // Check every 15 minutes (minimum for PeriodicWork)
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "device_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest)
        }
    }
}
