package com.eden.mkopa

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.webkit.*
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    
    private val BASE_URL = "https://eden-mkopa.onrender.com"
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        
        setupWebView()
        setupSwipeRefresh()
        
        // Check if device owner
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            setupDeviceOwner()
            // Start background sync
            SyncWorker.schedule(this)
            // Start lock monitor service for instant locking
            val lockMonitorIntent = Intent(this, LockMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(lockMonitorIntent)
            } else {
                startService(lockMonitorIntent)
            }
            // Start in kiosk mode immediately
            startLockTask()
        } else {
            // Show setup instructions
            val intent = Intent(this, DeviceOwnerSetupActivity::class.java)
            startActivity(intent)
            // Don't finish - let user come back after setup
        }
        
        // Load customer login page
        loadCustomerDashboard()
    }
    
    private fun requestDeviceAdmin() {
        try {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Eden requires device admin to secure your financed device"
            )
            startActivityForResult(intent, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                // Device admin granted, but we need Device Owner for full control
                // Show message to user
            }
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
            setSupportZoom(true)
            builtInZoomControls = false
        }
        
        // Add JavaScript interface for unlock functionality
        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun unlockDevice() {
                runOnUiThread {
                    if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
                        stopLockTask()
                    }
                }
            }
        }, "AndroidInterface")
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
            
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
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
    
    private fun setupDeviceOwner() {
        try {
            // Set user restrictions
            devicePolicyManager.addUserRestriction(adminComponent, "no_factory_reset")
            devicePolicyManager.addUserRestriction(adminComponent, "no_safe_boot")
            
            // Set lock task packages
            devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf(packageName))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun loadCustomerDashboard() {
        val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val deviceId = prefs.getString("device_id", null)
        val serialNumber = prefs.getString("serial_number", null)
        
        if (isLoggedIn) {
            // Load dashboard with device info
            var url = "$BASE_URL/dashboard"
            if (deviceId != null) {
                url += "?device_id=$deviceId"
            }
            webView.loadUrl(url)
        } else {
            // Load login with device info for auto-linking
            var url = "$BASE_URL/customer-login"
            if (deviceId != null) {
                url += "?device_id=$deviceId"
            }
            if (serialNumber != null) {
                url += if (deviceId != null) "&serial_number=$serialNumber" else "?serial_number=$serialNumber"
            }
            webView.loadUrl(url)
        }
    }
    
    override fun onBackPressed() {
        // Disable back button - app is in kiosk mode
        // Do nothing
    }
    
    override fun onResume() {
        super.onResume()
        webView.onResume()
    }
    
    override fun onPause() {
        super.onPause()
        webView.onPause()
    }
}
