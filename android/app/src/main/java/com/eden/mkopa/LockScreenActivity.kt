package com.eden.mkopa

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LockScreenActivity : AppCompatActivity() {
    
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var statusText: TextView
    private lateinit var balanceText: TextView
    private lateinit var refreshButton: Button
    
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.eden.mkopa.UNLOCK_DEVICE") {
                unlockDevice()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make this activity full screen and prevent exit
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        
        setContentView(R.layout.activity_lock_screen)
        
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        
        statusText = findViewById(R.id.statusText)
        balanceText = findViewById(R.id.balanceText)
        refreshButton = findViewById(R.id.refreshButton)
        
        // Register unlock receiver
        val filter = IntentFilter("com.eden.mkopa.UNLOCK_DEVICE")
        registerReceiver(unlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        
        // Start lock task mode (Kiosk mode)
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            startLockTask()
        }
        
        refreshButton.setOnClickListener {
            checkDeviceStatus()
        }
        
        loadDeviceInfo()
        checkDeviceStatus()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun loadDeviceInfo() {
        val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        val phone = prefs.getString("customer_phone", "Unknown")
        val deviceId = prefs.getString("device_id", "Unknown")
        
        statusText.text = "DEVICE LOCKED\nPayment Required"
        balanceText.text = "Device: $deviceId\nPhone: $phone"
    }
    
    private fun checkDeviceStatus() {
        refreshButton.isEnabled = false
        refreshButton.text = "Checking..."
        
        val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", null)
        
        if (deviceId == null) {
            refreshButton.isEnabled = true
            refreshButton.text = "Refresh Status"
            return
        }
        
        val apiService = ApiService.create()
        apiService.getDeviceStatus(deviceId).enqueue(object : Callback<DeviceStatus> {
            override fun onResponse(call: Call<DeviceStatus>, response: Response<DeviceStatus>) {
                refreshButton.isEnabled = true
                refreshButton.text = "Refresh Status"
                
                if (response.isSuccessful) {
                    val status = response.body()
                    status?.let {
                        if (!it.is_locked && it.status == "active") {
                            // Device is unlocked, exit lock screen
                            unlockDevice()
                        } else {
                            // Still locked
                            balanceText.text = "Balance: KES ${it.balance}\nTotal: KES ${it.total_amount}\nPaid: KES ${it.amount_paid}"
                        }
                    }
                } else {
                    balanceText.text = "Error checking status. Please try again."
                }
            }
            
            override fun onFailure(call: Call<DeviceStatus>, t: Throwable) {
                refreshButton.isEnabled = true
                refreshButton.text = "Refresh Status"
                balanceText.text = "Network error. Please check connection."
            }
        })
    }
    
    private fun unlockDevice() {
        // Stop lock task mode
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            stopLockTask()
        }
        
        // Save unlock status
        val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_locked", false).apply()
        
        // Return to main activity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onBackPressed() {
        // Disable back button when locked
    }
    
    override fun onPause() {
        super.onPause()
        // Bring activity back to front if locked
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.moveTaskToFront(taskId, 0)
    }
    
    companion object {
        fun show(context: Context) {
            val intent = Intent(context, LockScreenActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }
}
