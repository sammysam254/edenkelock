package com.eden.mkopa

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class DeviceOwnerSetupActivity : AppCompatActivity() {
    
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var webView: WebView
    private lateinit var downloadButton: Button
    private lateinit var continueButton: Button
    
    private val BASE_URL = "https://eden-mkopa.onrender.com"
    private var isFactoryResetRecovery = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_owner_setup)
        
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        webView = findViewById(R.id.webView)
        downloadButton = findViewById(R.id.downloadButton)
        continueButton = findViewById(R.id.continueButton)
        
        // Check if this is a factory reset recovery
        checkFactoryResetRecovery()
    }
    
    private fun checkFactoryResetRecovery() {
        val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        val deviceOwnerSetup = prefs.getBoolean("device_owner_setup", false)
        val factoryResetProtection = prefs.getBoolean("factory_reset_protection_enabled", false)
        
        // If device owner was setup but protection flag is missing, this is likely a factory reset recovery
        if (!deviceOwnerSetup || !factoryResetProtection) {
            isFactoryResetRecovery = true
            showFactoryResetRecovery()
        } else {
            // Normal device owner setup - check loan balance
            checkLoanBalance()
        }
    }
    
    private fun showFactoryResetRecovery() {
        statusText.text = "⚠️ UNAUTHORIZED FACTORY RESET DETECTED"
        progressBar.visibility = View.GONE
        
        // Show website to download app
        webView.visibility = View.VISIBLE
        downloadButton.visibility = View.VISIBLE
        
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url?.contains("eden.apk") == true) {
                    // APK download detected
                    downloadButton.text = "✅ APK Downloaded - Continue Setup"
                    downloadButton.setBackgroundColor(getColor(android.R.color.holo_green_dark))
                    continueButton.visibility = View.VISIBLE
                    return false
                }
                return false
            }
        }
        
        // Load Eden website
        webView.loadUrl(BASE_URL)
        
        downloadButton.setOnClickListener {
            // Open APK download directly
            val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse("$BASE_URL/download/eden.apk"))
            startActivity(downloadIntent)
        }
        
        continueButton.setOnClickListener {
            // After APK download, proceed to login
            webView.visibility = View.GONE
            downloadButton.visibility = View.GONE
            continueButton.visibility = View.GONE
            
            statusText.text = "✅ App Downloaded - Please Login to Continue"
            
            // Start PIN entry for customer login
            val intent = Intent(this, PinEntryActivity::class.java)
            intent.putExtra("factory_reset_recovery", true)
            startActivity(intent)
            finish()
        }
    }
    
    private fun checkLoanBalance() {
        statusText.text = "🔍 Checking loan balance..."
        progressBar.visibility = View.VISIBLE
        
        val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        val customerPhone = prefs.getString("customer_phone", null)
        
        if (customerPhone == null) {
            // No customer logged in - go to PIN entry
            startPinEntry()
            return
        }
        
        // Check loan balance via API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/customer/dashboard?phone=${Uri.encode(customerPhone)}")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    
                    withContext(Dispatchers.Main) {
                        if (jsonResponse.getBoolean("success")) {
                            val customer = jsonResponse.getJSONObject("customer")
                            val loanBalance = customer.getDouble("loan_balance")
                            
                            if (loanBalance > 0) {
                                // Outstanding balance - LOCK DEVICE IMMEDIATELY
                                lockDeviceForNonPayment(loanBalance, customerPhone)
                            } else {
                                // Loan paid - allow access
                                statusText.text = "✅ Loan Paid - Access Granted"
                                Handler(Looper.getMainLooper()).postDelayed({
                                    startMainActivity()
                                }, 2000)
                            }
                        } else {
                            // Customer not found - go to login
                            startPinEntry()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        startPinEntry()
                    }
                }
                
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    startPinEntry()
                }
            }
        }
    }
    
    private fun lockDeviceForNonPayment(balance: Double, phone: String) {
        statusText.text = "🔒 DEVICE LOCKED - Outstanding Balance: KES ${balance.toInt()}"
        progressBar.visibility = View.GONE
        
        // Show lock message with payment instructions
        val lockIntent = Intent(this, LockScreenActivity::class.java)
        lockIntent.putExtra("lock_reason", "OUTSTANDING_BALANCE")
        lockIntent.putExtra("balance_amount", balance)
        lockIntent.putExtra("customer_phone", phone)
        lockIntent.putExtra("website_url", BASE_URL)
        lockIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(lockIntent)
        finish()
    }
    
    private fun startPinEntry() {
        statusText.text = "🔐 Please Login to Continue"
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, PinEntryActivity::class.java)
            intent.putExtra("check_balance_after_login", true)
            startActivity(intent)
            finish()
        }, 1500)
    }
    
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    override fun onBackPressed() {
        // Disable back button during setup
    }
}
