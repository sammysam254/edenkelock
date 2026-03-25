package com.eden.mkopa

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.*
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val handler = Handler(Looper.getMainLooper())
    
    private val BASE_URL = "https://eden-mkopa.onrender.com"
    private val TAG = "MainActivity"
    
    // Device management components (nullable to handle failures gracefully)
    private var devicePolicyManager: DevicePolicyManager? = null
    private var adminComponent: ComponentName? = null
    private var isDeviceOwner = false
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        Log.d(TAG, "MainActivity onCreate started")
        
        // Initialize views first - this must succeed
        try {
            webView = findViewById(R.id.webView)
            progressBar = findViewById(R.id.progressBar)
            swipeRefresh = findViewById(R.id.swipeRefresh)
            Log.d(TAG, "Views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL: Failed to initialize views", e)
            Toast.makeText(this, "App initialization failed", Toast.LENGTH_LONG).show()
            return
        }
        
        // Setup WebView - this must succeed
        try {
            setupWebView()
            Log.d(TAG, "WebView setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup WebView", e)
        }
        
        // Setup SwipeRefresh
        try {
            setupSwipeRefresh()
            Log.d(TAG, "SwipeRefresh setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup SwipeRefresh", e)
        }
        
        // Load dashboard immediately - don't wait for device features
        try {
            loadCustomerDashboard()
            Log.d(TAG, "Dashboard loading started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load dashboard", e)
            webView.loadUrl("$BASE_URL/customer-login")
        }
        
        // Initialize device management features in background with delays
        // This happens AFTER the UI is ready and won't block the app
        initializeDeviceFeaturesSequentially()
    }
    
    private fun initializeDeviceFeaturesSequentially() {
        Log.d(TAG, "Starting sequential device feature initialization")
        
        // Step 1: Initialize Device Policy Manager (after 5 seconds)
        handler.postDelayed({
            try {
                Log.d(TAG, "Step 1: Initializing Device Policy Manager")
                devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
                
                if (devicePolicyManager != null && adminComponent != null) {
                    isDeviceOwner = devicePolicyManager!!.isDeviceOwnerApp(packageName)
                    Log.d(TAG, "Device Policy Manager initialized. Is Device Owner: $isDeviceOwner")
                    
                    if (isDeviceOwner) {
                        // Step 2: Setup device restrictions (after 1 more minute)
                        handler.postDelayed({
                            setupDeviceRestrictions()
                        }, 60000) // 1 minute delay
                    }
                } else {
                    Log.w(TAG, "Device Policy Manager not available")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Device Policy Manager", e)
            }
        }, 5000) // 5 second initial delay
    }
    
    private fun setupDeviceRestrictions() {
        try {
            Log.d(TAG, "Step 2: Setting up device restrictions")
            
            if (devicePolicyManager == null || adminComponent == null || !isDeviceOwner) {
                Log.w(TAG, "Cannot setup restrictions - not device owner")
                return
            }
            
            // Add user restrictions
            try {
                devicePolicyManager!!.addUserRestriction(adminComponent!!, "no_factory_reset")
                devicePolicyManager!!.addUserRestriction(adminComponent!!, "no_safe_boot")
                Log.d(TAG, "User restrictions added successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add user restrictions", e)
            }
            
            // Set lock task packages
            try {
                devicePolicyManager!!.setLockTaskPackages(adminComponent!!, arrayOf(packageName))
                Log.d(TAG, "Lock task packages configured")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set lock task packages", e)
            }
            
            // Step 3: Start background services (after 1 more minute)
            handler.postDelayed({
                startBackgroundServices()
            }, 60000) // 1 minute delay
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup device restrictions", e)
        }
    }
    
    private fun startBackgroundServices() {
        try {
            Log.d(TAG, "Step 3: Starting background services")
            
            // Start SyncWorker
            try {
                SyncWorker.schedule(this)
                Log.d(TAG, "SyncWorker scheduled successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule SyncWorker", e)
            }
            
            // Start LockMonitorService
            try {
                val lockMonitorIntent = Intent(this, LockMonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(lockMonitorIntent)
                } else {
                    startService(lockMonitorIntent)
                }
                Log.d(TAG, "LockMonitorService started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start LockMonitorService", e)
            }
            
            // Step 4: Enter kiosk mode (after 1 more minute)
            handler.postDelayed({
                enterKioskMode()
            }, 60000) // 1 minute delay
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start background services", e)
        }
    }
    
    private fun enterKioskMode() {
        try {
            Log.d(TAG, "Step 4: Entering kiosk mode")
            
            if (devicePolicyManager == null || !isDeviceOwner) {
                Log.w(TAG, "Cannot enter kiosk mode - not device owner")
                return
            }
            
            // Whitelist this app for lock task mode first
            try {
                devicePolicyManager?.setLockTaskPackages(adminComponent, arrayOf(packageName))
                Log.d(TAG, "App whitelisted for lock task mode")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to whitelist app for lock task", e)
            }
            
            startLockTask()
            Log.d(TAG, "Kiosk mode activated successfully")
            Toast.makeText(this, "Device secured in kiosk mode", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enter kiosk mode", e)
        }
    }
    
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        
        // Add JavaScript interface for unlock functionality
        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun unlockDevice() {
                runOnUiThread {
                    try {
                        if (isDeviceOwner && devicePolicyManager != null) {
                            stopLockTask()
                            Log.d(TAG, "Device unlocked via JavaScript interface")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to unlock device", e)
                    }
                }
            }
        }, "AndroidInterface")
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
            
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                Log.e(TAG, "WebView error: ${error?.description}")
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
            }
        }
    }
    
    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(
            resources.getColor(android.R.color.holo_green_dark, null),
            resources.getColor(android.R.color.holo_green_light, null)
        )
        
        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
    }
    
    private fun loadCustomerDashboard() {
        val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        val customerPhone = prefs.getString("customer_phone", null)
        val deviceId = prefs.getString("device_id", null)
        val serialNumber = prefs.getString("serial_number", null)
        
        // Always load customer dashboard
        var url = "$BASE_URL/customer-dashboard"
        
        // Add phone as query parameter if available
        val params = mutableListOf<String>()
        if (customerPhone != null) {
            params.add("phone=$customerPhone")
            // Save phone to localStorage via JavaScript
            webView.evaluateJavascript(
                "localStorage.setItem('eden_customer_phone', '$customerPhone');",
                null
            )
        }
        if (deviceId != null) {
            params.add("device_id=$deviceId")
            webView.evaluateJavascript(
                "localStorage.setItem('eden_device_id', '$deviceId');",
                null
            )
        }
        if (serialNumber != null) params.add("serial_number=$serialNumber")
        
        if (params.isNotEmpty()) {
            url += "?" + params.joinToString("&")
        }
        
        Log.d(TAG, "Loading customer dashboard: $url")
        webView.loadUrl(url)
    }
    
    override fun onBackPressed() {
        if (isDeviceOwner) {
            // In kiosk mode, disable back button
            Log.d(TAG, "Back button disabled in kiosk mode")
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onResume() {
        super.onResume()
        webView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        webView.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up handlers
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "MainActivity destroyed")
    }
}
