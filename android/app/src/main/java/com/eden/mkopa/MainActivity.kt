package com.eden.mkopa

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
        
        // Check if device owner and setup
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            setupDeviceOwner()
            // Start background sync
            SyncWorker.schedule(this)
            // Start in kiosk mode immediately
            startLockTask()
        }
        
        // Load customer login page
        loadCustomerDashboard()
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
        
        if (isLoggedIn) {
            webView.loadUrl("$BASE_URL/dashboard")
        } else {
            webView.loadUrl("$BASE_URL/customer-login")
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
