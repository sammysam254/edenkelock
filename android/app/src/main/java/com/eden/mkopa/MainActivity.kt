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
        
        try {
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
                try {
                    setupDeviceOwner()
                    
                    // Start background sync
                    try {
                        SyncWorker.schedule(this)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    // Start lock monitor service for instant locking
                    try {
                        val lockMonitorIntent = Intent(this, LockMonitorService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(lockMonitorIntent)
                        } else {
                            startService(lockMonitorIntent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Continue even if service fails to start
                    }
                    
                    // Start in kiosk mode immediately
                    try {
                        startLockTask()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Continue even if kiosk mode fails
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Continue even if setup fails
                }
            }
            
            // Load customer login page
            loadCustomerDashboard()
        } catch (e: Exception) {
            e.printStackTrace()
            // If anything fails, show error and try to continue
            try {
                android.widget.Toast.makeText(this, "Starting app...", android.widget.Toast.LENGTH_SHORT).show()
            } catch (te: Exception) {
                te.printStackTrace()
            }
        }
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
        try {
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
                        try {
                            if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
                                stopLockTask()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }, "AndroidInterface")
            
            webView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    try {
                        progressBar.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    try {
                        progressBar.visibility = View.GONE
                        swipeRefresh.isRefreshing = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }
                
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    try {
                        progressBar.visibility = View.GONE
                        swipeRefresh.isRefreshing = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            webView.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    try {
                        progressBar.progress = newProgress
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to basic URL
            try {
                webView.loadUrl("$BASE_URL/customer-login")
            } catch (we: Exception) {
                we.printStackTrace()
            }
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
