package com.eden.mkopa

import android.Manifest
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SplashActivity : AppCompatActivity() {
    
    private lateinit var logoImageView: ImageView
    private lateinit var statusText: TextView
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private var isDeviceOwner = false
    private var deviceImei: String? = null
    
    private val BASE_URL = "https://eden-mkopa.onrender.com"
    private val TAG = "SplashActivity"
    private val PERMISSION_REQUEST_CODE = 1001
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make fullscreen with green background
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        setContentView(R.layout.activity_splash)
        
        // Initialize views
        logoImageView = findViewById(R.id.logoImageView)
        statusText = findViewById(R.id.statusText)
        
        // Initialize device management
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        isDeviceOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
        
        Log.d(TAG, "SplashActivity started. Is Device Owner: $isDeviceOwner")
        
        if (isDeviceOwner) {
            // Show Eden logo and green background for device owner
            showDeviceOwnerSplash()
        } else {
            // Regular startup
            showRegularSplash()
        }
    }
    
    private fun showDeviceOwnerSplash() {
        statusText.text = "Eden Device Protection Active"
        
        // Get device IMEI for tracking
        getDeviceImei()
        
        // Start initialization sequence
        Handler(Looper.getMainLooper()).postDelayed({
            initializeDeviceOwnerFeatures()
        }, 2000)
    }
    
    private fun showRegularSplash() {
        statusText.text = "Starting Eden..."
        
        Handler(Looper.getMainLooper()).postDelayed({
            proceedToLogin()
        }, 2000)
    }
    
    @SuppressLint("HardwareIds")
    private fun getDeviceImei() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
                
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_PHONE_STATE),
                    PERMISSION_REQUEST_CODE
                )
                return
            }
            
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            deviceImei = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephonyManager.imei
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.deviceId
            }
            
            Log.d(TAG, "Device IMEI obtained: ${deviceImei?.take(4)}****")
            
            // Store IMEI in preferences for tracking
            val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("device_imei", deviceImei).apply()
            
            // Report IMEI to server for tracking
            reportDeviceImei()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get device IMEI", e)
        }
    }
    
    private fun reportDeviceImei() {
        if (deviceImei == null) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/device/report-imei")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                val jsonBody = JSONObject()
                jsonBody.put("imei", deviceImei)
                jsonBody.put("device_model", Build.MODEL)
                jsonBody.put("device_brand", Build.BRAND)
                jsonBody.put("android_version", Build.VERSION.RELEASE)
                jsonBody.put("app_version", "1.8.4")
                
                connection.outputStream.write(jsonBody.toString().toByteArray())
                
                val responseCode = connection.responseCode
                Log.d(TAG, "IMEI report response: $responseCode")
                
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report IMEI", e)
            }
        }
    }
    
    private fun initializeDeviceOwnerFeatures() {
        statusText.text = "Initializing Device Protection..."
        
        try {
            // Enable maximum security restrictions
            enableMaximumSecurity()
            
            // Start protection services
            startProtectionServices()
            
            // Check for factory reset recovery
            checkFactoryResetRecovery()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize device owner features", e)
        }
        
        Handler(Looper.getMainLooper()).postDelayed({
            proceedToLogin()
        }, 3000)
    }
    
    private fun enableMaximumSecurity() {
        try {
            // Add all security restrictions
            devicePolicyManager.addUserRestriction(adminComponent, "no_factory_reset")
            devicePolicyManager.addUserRestriction(adminComponent, "no_safe_boot")
            devicePolicyManager.addUserRestriction(adminComponent, "no_debugging_features")
            devicePolicyManager.addUserRestriction(adminComponent, "no_usb_file_transfer")
            devicePolicyManager.addUserRestriction(adminComponent, "no_install_unknown_sources")
            devicePolicyManager.addUserRestriction(adminComponent, "no_uninstall_apps")
            
            // Block uninstall of this app
            devicePolicyManager.setUninstallBlocked(adminComponent, packageName, true)
            
            // Hide settings app
            devicePolicyManager.setApplicationHidden(adminComponent, "com.android.settings", true)
            
            // Set kiosk mode packages
            devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf(packageName))
            
            Log.d(TAG, "Maximum security restrictions enabled")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable security restrictions", e)
        }
    }
    
    private fun startProtectionServices() {
        try {
            // Start factory reset protection service
            val protectionIntent = Intent(this, FactoryResetProtectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(protectionIntent)
            } else {
                startService(protectionIntent)
            }
            
            // Start lock monitor service
            val lockMonitorIntent = Intent(this, LockMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(lockMonitorIntent)
            } else {
                startService(lockMonitorIntent)
            }
            
            Log.d(TAG, "Protection services started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start protection services", e)
        }
    }
    
    private fun checkFactoryResetRecovery() {
        val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        val lastBootTime = prefs.getLong("last_boot_time", 0)
        val currentBootTime = System.currentTimeMillis()
        
        // If significant time gap, might be factory reset recovery
        if (lastBootTime > 0 && (currentBootTime - lastBootTime) > 86400000) { // 24 hours
            Log.w(TAG, "Potential factory reset detected - checking IMEI lock status")
            checkImeiLockStatus()
        }
        
        prefs.edit().putLong("last_boot_time", currentBootTime).apply()
    }
    
    private fun checkImeiLockStatus() {
        if (deviceImei == null) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/device/check-imei-lock?imei=$deviceImei")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    
                    withContext(Dispatchers.Main) {
                        if (jsonResponse.getBoolean("is_locked")) {
                            // Device is locked by IMEI - show lock screen immediately
                            val lockIntent = Intent(this@SplashActivity, LockScreenActivity::class.java)
                            lockIntent.putExtra("lock_reason", "IMEI_LOCKED")
                            lockIntent.putExtra("imei", deviceImei)
                            lockIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(lockIntent)
                            finish()
                            return@withContext
                        }
                    }
                }
                
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check IMEI lock status", e)
            }
        }
    }
    
    private fun proceedToLogin() {
        val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        val pinCompleted = prefs.getBoolean("pin_completed", false)
        
        val intent = if (pinCompleted) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, PinEntryActivity::class.java)
        }
        
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getDeviceImei()
            } else {
                Log.w(TAG, "Phone state permission denied")
            }
        }
    }
}