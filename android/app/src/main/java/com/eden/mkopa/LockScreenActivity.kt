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
        try {
            val filter = IntentFilter("com.eden.mkopa.UNLOCK_DEVICE")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(unlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(unlockReceiver, filter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // APPLY COMPLETE LOCKDOWN - BLOCKS EVERYTHING INCLUDING CALLS
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            DeviceAdminReceiver.applyCompleteLockdown(this)
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
        
        // Get lock reason from intent
        val lockReason = intent.getStringExtra("lock_reason") ?: "DEVICE_LOCKED"
        val balanceAmount = intent.getDoubleExtra("balance_amount", 0.0)
        
        when (lockReason) {
            "OUTSTANDING_BALANCE" -> {
                statusText.text = "🔒 DEVICE COMPLETELY LOCKED\nOutstanding Balance"
                balanceText.text = "Amount Due: KES ${balanceAmount.toInt()}\nDevice: $deviceId\nPhone: $phone\n\n⚠️ ALL FUNCTIONS BLOCKED:\n• Phone calls disabled\n• SMS disabled\n• All apps hidden\n• Settings blocked\n\nContact admin or make payment to unlock"
            }
            "ADMIN_LOCKED" -> {
                statusText.text = "🔒 DEVICE COMPLETELY LOCKED\nBy Administrator"
                balanceText.text = "Device: $deviceId\nPhone: $phone\n\n⚠️ ALL FUNCTIONS BLOCKED:\n• Phone calls disabled\n• SMS disabled\n• All apps hidden\n• Settings blocked\n\nContact administrator to unlock"
            }
            else -> {
                statusText.text = "🔒 DEVICE COMPLETELY LOCKED\nPayment Required"
                balanceText.text = "Device: $deviceId\nPhone: $phone\n\n⚠️ COMPLETE LOCKDOWN ACTIVE:\n• No calls allowed\n• No messaging\n• Only Eden app accessible\n• All system functions blocked"
            }
        }
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
                            // Still locked - update balance info
                            val currentText = balanceText.text.toString()
                            val newBalanceInfo = "Balance: KES ${it.balance}\nTotal: KES ${it.total_amount}\nPaid: KES ${it.amount_paid}"
                            
                            // Keep the lockdown warning but update balance
                            if (currentText.contains("ALL FUNCTIONS BLOCKED")) {
                                val parts = currentText.split("⚠️")
                                if (parts.size > 1) {
                                    balanceText.text = parts[0] + newBalanceInfo + "\n\n⚠️" + parts[1]
                                } else {
                                    balanceText.text = newBalanceInfo + "\n\n⚠️ COMPLETE LOCKDOWN ACTIVE"
                                }
                            } else {
                                balanceText.text = newBalanceInfo
                            }
                        }
                    }
                } else {
                    balanceText.text = "Error checking status. Please try again.\n\n⚠️ Device remains locked until payment confirmed."
                }
            }
            
            override fun onFailure(call: Call<DeviceStatus>, t: Throwable) {
                refreshButton.isEnabled = true
                refreshButton.text = "Refresh Status"
                balanceText.text = "Network error. Please check connection.\n\n⚠️ Device remains locked until payment confirmed."
            }
        })
    }
    
    private fun unlockDevice() {
        // REMOVE COMPLETE LOCKDOWN - RESTORE ALL FUNCTIONS
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            DeviceAdminReceiver.removeCompleteLockdown(this)
            stopLockTask()
        }
        
        // Save unlock status
        val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_locked", false)
            putBoolean("complete_lockdown_active", false)
            apply()
        }
        
        // Return to main activity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onBackPressed() {
        // Disable back button when locked - NO ESCAPE
    }
    
    override fun onPause() {
        super.onPause()
        // Bring activity back to front if locked - CANNOT MINIMIZE
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.moveTaskToFront(taskId, 0)
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Prevent user from leaving this activity
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.moveTaskToFront(taskId, 0)
    }
    
    companion object {
        fun show(context: Context, lockReason: String = "DEVICE_LOCKED", balanceAmount: Double = 0.0) {
            val intent = Intent(context, LockScreenActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra("lock_reason", lockReason)
            intent.putExtra("balance_amount", balanceAmount)
            context.startActivity(intent)
        }
    }
}
