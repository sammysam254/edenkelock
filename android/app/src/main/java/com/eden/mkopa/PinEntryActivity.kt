package com.eden.mkopa

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class PinEntryActivity : AppCompatActivity() {
    
    private lateinit var returningUserLayout: LinearLayout
    private lateinit var newUserLayout: LinearLayout
    private lateinit var phoneInput: EditText
    private lateinit var pinBox1: EditText
    private lateinit var pinBox2: EditText
    private lateinit var pinBox3: EditText
    private lateinit var pinBox4: EditText
    private lateinit var newPhoneInput: EditText
    private lateinit var errorText: TextView
    private lateinit var titleText: TextView
    
    private val BASE_URL = "https://eden-mkopa.onrender.com"
    private val TAG = "PinEntryActivity"
    private val PERMISSION_REQUEST_CODE = 1001
    
    private var deviceFingerprint: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_entry)
        
        // Initialize views
        returningUserLayout = findViewById(R.id.returningUserLayout)
        newUserLayout = findViewById(R.id.newUserLayout)
        phoneInput = findViewById(R.id.phoneInput)
        pinBox1 = findViewById(R.id.pinBox1)
        pinBox2 = findViewById(R.id.pinBox2)
        pinBox3 = findViewById(R.id.pinBox3)
        pinBox4 = findViewById(R.id.pinBox4)
        newPhoneInput = findViewById(R.id.newPhoneInput)
        errorText = findViewById(R.id.errorText)
        titleText = findViewById(R.id.titleText)
        
        // Generate device fingerprint
        generateDeviceFingerprint()
        
        // Try auto-login first
        tryAutoLogin()
    }
    
    @SuppressLint("HardwareIds")
    private fun generateDeviceFingerprint() {
        try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            val imei = if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    telephonyManager.imei ?: "unknown"
                } else {
                    @Suppress("DEPRECATION")
                    telephonyManager.deviceId ?: "unknown"
                }
            } else {
                "no_permission"
            }
            
            val serial = Build.SERIAL ?: "unknown"
            val model = Build.MODEL ?: "unknown"
            val brand = Build.BRAND ?: "unknown"
            
            // Create unique device fingerprint
            deviceFingerprint = "${imei}_${serial}_${model}_${brand}".replace(" ", "_")
            
            Log.d(TAG, "Device fingerprint generated: ${deviceFingerprint?.take(10)}...")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate device fingerprint", e)
            deviceFingerprint = "fallback_${Build.MODEL}_${Build.BRAND}".replace(" ", "_")
        }
    }
    
    private fun tryAutoLogin() {
        val persistentToken = getPersistentToken()
        
        if (persistentToken != null && deviceFingerprint != null) {
            Log.d(TAG, "Attempting auto-login with persistent token")
            
            errorText.visibility = View.VISIBLE
            errorText.text = "Checking saved login..."
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL("$BASE_URL/api/auth/device-auto-login")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true
                    
                    val jsonBody = JSONObject()
                    jsonBody.put("device_fingerprint", deviceFingerprint)
                    jsonBody.put("persistent_token", persistentToken)
                    
                    connection.outputStream.write(jsonBody.toString().toByteArray())
                    
                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().readText()
                        val jsonResponse = JSONObject(response)
                        
                        withContext(Dispatchers.Main) {
                            if (jsonResponse.getBoolean("success")) {
                                Log.d(TAG, "Auto-login successful")
                                
                                // Save customer info
                                val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
                                prefs.edit()
                                    .putBoolean("pin_completed", true)
                                    .putString("customer_phone", jsonResponse.getString("customer_phone"))
                                    .putString("customer_id", jsonResponse.getString("customer_id"))
                                    .putString("device_id", jsonResponse.getString("device_id"))
                                    .apply()
                                
                                // Go directly to MainActivity
                                val intent = Intent(this@PinEntryActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                                return@withContext
                            } else {
                                Log.w(TAG, "Auto-login failed: ${jsonResponse.optString("error")}")
                                showManualLogin()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Log.w(TAG, "Auto-login request failed: $responseCode")
                            showManualLogin()
                        }
                    }
                    
                    connection.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Auto-login error", e)
                    withContext(Dispatchers.Main) {
                        showManualLogin()
                    }
                }
            }
        } else {
            Log.d(TAG, "No persistent token found, showing manual login")
            showManualLogin()
        }
    }
    
    private fun showManualLogin() {
        errorText.visibility = View.GONE
        
        // Check if user has logged in before (fallback to SharedPreferences)
        val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        val savedPhone = prefs.getString("customer_phone", null)
        
        if (savedPhone != null) {
            // Returning user - show phone + PIN on same page
            showReturningUserLogin(savedPhone)
        } else {
            // New user - show phone number entry first
            showNewUserPhoneEntry()
        }
    }
    
    private fun getPersistentToken(): String? {
        return try {
            // Try to read from external storage first (survives app updates)
            val externalDir = getExternalFilesDir(null)
            if (externalDir != null) {
                val tokenFile = File(externalDir, "eden_persistent_token.txt")
                if (tokenFile.exists()) {
                    val token = tokenFile.readText().trim()
                    Log.d(TAG, "Persistent token found in external storage")
                    return token
                }
            }
            
            // Fallback to SharedPreferences
            val prefs = getSharedPreferences("eden_persistent", Context.MODE_PRIVATE)
            val token = prefs.getString("persistent_token", null)
            if (token != null) {
                Log.d(TAG, "Persistent token found in SharedPreferences")
            }
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get persistent token", e)
            null
        }
    }
    
    private fun savePersistentToken(token: String) {
        try {
            // Save to external storage (survives app updates)
            val externalDir = getExternalFilesDir(null)
            if (externalDir != null) {
                val tokenFile = File(externalDir, "eden_persistent_token.txt")
                tokenFile.writeText(token)
                Log.d(TAG, "Persistent token saved to external storage")
            }
            
            // Also save to SharedPreferences as backup
            val prefs = getSharedPreferences("eden_persistent", Context.MODE_PRIVATE)
            prefs.edit().putString("persistent_token", token).apply()
            Log.d(TAG, "Persistent token saved to SharedPreferences")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save persistent token", e)
        }
    }
    
    private fun showReturningUserLogin(savedPhone: String) {
        returningUserLayout.visibility = View.VISIBLE
        newUserLayout.visibility = View.GONE
        titleText.text = "Welcome Back!"
        
        // Pre-fill phone number but make it editable
        phoneInput.setText(savedPhone)
        phoneInput.isEnabled = true  // Allow editing
        phoneInput.setSelection(phoneInput.text.length)  // Cursor at end
        
        // Setup PIN boxes
        setupPinBoxes()
        
        // Focus on first PIN box
        pinBox1.requestFocus()
        pinBox1.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(pinBox1, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
        
        // Add phone number formatting for returning users
        phoneInput.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                
                isFormatting = true
                val formatted = formatPhoneForDisplay(s.toString())
                if (formatted != s.toString()) {
                    phoneInput.setText(formatted)
                    phoneInput.setSelection(formatted.length)
                }
                isFormatting = false
            }
        })
        
        // Add "Not you?" button functionality
        findViewById<TextView>(R.id.notYouText).setOnClickListener {
            // Clear saved data and show new user flow
            val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            
            // Clear persistent token as well
            try {
                val externalDir = getExternalFilesDir(null)
                if (externalDir != null) {
                    val tokenFile = File(externalDir, "eden_persistent_token.txt")
                    if (tokenFile.exists()) {
                        tokenFile.delete()
                    }
                }
                val persistentPrefs = getSharedPreferences("eden_persistent", Context.MODE_PRIVATE)
                persistentPrefs.edit().clear().apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear persistent token", e)
            }
            
            showNewUserPhoneEntry()
        }
    }
    
    private fun showNewUserPhoneEntry() {
        returningUserLayout.visibility = View.GONE
        newUserLayout.visibility = View.VISIBLE
        titleText.text = "Enter Your Phone Number"
        
        // Clear any previous error messages
        errorText.visibility = View.GONE
        
        // Clear the input field
        newPhoneInput.text.clear()
        
        newPhoneInput.requestFocus()
        newPhoneInput.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(newPhoneInput, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
        
        newPhoneInput.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                
                isFormatting = true
                val formatted = formatPhoneForDisplay(s.toString())
                if (formatted != s.toString()) {
                    newPhoneInput.setText(formatted)
                    newPhoneInput.setSelection(formatted.length)
                }
                isFormatting = false
                
                // Clear any previous error messages when user types
                errorText.visibility = View.GONE
                
                // Only check when we have a complete 10-digit number (07xxxxxxxx)
                val phone = formatted
                if (phone.length == 10 && phone.startsWith("07")) {
                    // Wait a moment to avoid too many API calls while typing
                    newPhoneInput.removeCallbacks(checkPhoneRunnable)
                    newPhoneInput.postDelayed(checkPhoneRunnable, 1000) // Increased delay for better UX
                }
            }
        })
    }
    
    // Runnable to check phone number after user stops typing
    private val checkPhoneRunnable = Runnable {
        val phoneDisplay = newPhoneInput.text.toString()
        if (phoneDisplay.length == 10 && phoneDisplay.startsWith("07")) {
            val phoneForServer = formatPhoneForServer(phoneDisplay)
            checkPhoneNumber(phoneForServer, phoneDisplay)
        }
    }
    
    /**
     * Show customer registration screen for enrolled but unregistered customers
     */
    private fun showCustomerRegistration(phoneForServer: String, phoneDisplay: String, customerName: String) {
        // Hide other layouts
        returningUserLayout.visibility = View.GONE
        newUserLayout.visibility = View.GONE
        
        // Create registration dialog
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Complete Registration")
        builder.setMessage("Welcome $customerName!\n\nYour device is enrolled. Please set a 4-digit PIN to complete registration and activate your device.")
        builder.setCancelable(false)
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_pin, null)
        builder.setView(dialogView)
        
        val newPin1 = dialogView.findViewById<EditText>(R.id.newPinBox1)
        val newPin2 = dialogView.findViewById<EditText>(R.id.newPinBox2)
        val newPin3 = dialogView.findViewById<EditText>(R.id.newPinBox3)
        val newPin4 = dialogView.findViewById<EditText>(R.id.newPinBox4)
        val confirmPin1 = dialogView.findViewById<EditText>(R.id.confirmPinBox1)
        val confirmPin2 = dialogView.findViewById<EditText>(R.id.confirmPinBox2)
        val confirmPin3 = dialogView.findViewById<EditText>(R.id.confirmPinBox3)
        val confirmPin4 = dialogView.findViewById<EditText>(R.id.confirmPinBox4)
        
        // Setup PIN box navigation
        setupPinBoxNavigation(listOf(newPin1, newPin2, newPin3, newPin4))
        setupPinBoxNavigation(listOf(confirmPin1, confirmPin2, confirmPin3, confirmPin4))
        
        builder.setPositiveButton("Register") { _, _ ->
            val newPin = newPin1.text.toString() + newPin2.text.toString() + 
                        newPin3.text.toString() + newPin4.text.toString()
            val confirmPin = confirmPin1.text.toString() + confirmPin2.text.toString() + 
                            confirmPin3.text.toString() + confirmPin4.text.toString()
            
            if (newPin.length != 4 || confirmPin.length != 4) {
                Toast.makeText(this, "Please enter 4 digits for both PINs", Toast.LENGTH_SHORT).show()
                showCustomerRegistration(phoneForServer, phoneDisplay, customerName) // Show dialog again
                return@setPositiveButton
            }
            
            if (newPin != confirmPin) {
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show()
                showCustomerRegistration(phoneForServer, phoneDisplay, customerName) // Show dialog again
                return@setPositiveButton
            }
            
            registerCustomer(phoneForServer, newPin)
        }
        
        builder.setNegativeButton("Cancel") { _, _ ->
            showNewUserPhoneEntry()
        }
        
        val dialog = builder.create()
        dialog.show()
        
        // Focus on first new PIN box
        newPin1.requestFocus()
        newPin1.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(newPin1, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }
    
    /**
     * Register customer with their chosen PIN
     */
    private fun registerCustomer(phone: String, pin: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/customer/register")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                val jsonBody = JSONObject()
                jsonBody.put("phone_number", phone)
                jsonBody.put("pin", pin)
                jsonBody.put("confirm_pin", pin)
                
                connection.outputStream.write(jsonBody.toString().toByteArray())
                
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    
                    withContext(Dispatchers.Main) {
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(this@PinEntryActivity, "Registration successful! Welcome to Eden.", Toast.LENGTH_LONG).show()
                            
                            // Save registration data and proceed to MainActivity
                            val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putBoolean("pin_completed", true)
                                .putString("customer_phone", phone)
                                .putString("customer_id", jsonResponse.getString("customer_id"))
                                .putString("device_id", jsonResponse.getString("device_id"))
                                .apply()
                            
                            // Save persistent token if provided
                            val token = jsonResponse.optString("token")
                            if (token.isNotEmpty()) {
                                savePersistentToken(token)
                            }
                            
                            val intent = Intent(this@PinEntryActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@PinEntryActivity, "Registration failed: ${jsonResponse.optString("error")}", Toast.LENGTH_LONG).show()
                            showNewUserPhoneEntry()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PinEntryActivity, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show()
                        showNewUserPhoneEntry()
                    }
                }
                
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PinEntryActivity, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                    showNewUserPhoneEntry()
                }
            }
        }
    }

    /**
     * Format phone number for display (07 format)
     * Improved to handle various input formats and ensure 07 display
     */
    private fun formatPhoneForDisplay(phone: String): String {
        if (phone.isEmpty()) return phone
        
        // Remove all non-digits
        val digits = phone.replace(Regex("[^0-9]"), "")
        
        return when {
            // If starts with 254, convert to 07 format
            digits.startsWith("254") && digits.length >= 12 -> "0${digits.substring(3, 12)}"
            // If starts with 07, keep as is but limit to 10 digits
            digits.startsWith("07") -> digits.take(10)
            // If starts with 7 (without 0), add 0 and limit to 10 digits
            digits.startsWith("7") && digits.length >= 9 -> "0${digits.take(9)}"
            // If starts with 0 but not 07, try to format correctly
            digits.startsWith("0") && digits.length >= 10 -> {
                if (digits.startsWith("07")) digits.take(10) else "07${digits.substring(1).take(8)}"
            }
            // For other cases, try to create valid 07 format
            digits.length >= 9 -> "07${digits.take(8)}"
            // If less than 9 digits, just add 0 prefix if needed
            digits.length > 0 -> {
                if (digits.startsWith("0")) digits else "0$digits"
            }
            else -> phone
        }
    }
    
    /**
     * Convert 07 format to +254 format for server submission
     * Improved to handle edge cases and ensure correct +254 format
     */
    private fun formatPhoneForServer(phone: String): String {
        if (phone.isEmpty()) return phone
        
        // Remove all non-digits
        val digits = phone.replace(Regex("[^0-9]"), "")
        
        return when {
            // Already starts with 254
            digits.startsWith("254") && digits.length >= 12 -> "+${digits.take(12)}"
            // Starts with 07 - convert to +254
            digits.startsWith("07") && digits.length >= 10 -> "+254${digits.substring(1, 10)}"
            // Starts with 0 and has enough digits
            digits.startsWith("0") && digits.length >= 10 -> "+254${digits.substring(1, 10)}"
            // Starts with 7 (without 0)
            digits.startsWith("7") && digits.length >= 9 -> "+254${digits.take(9)}"
            // Default case - assume it's a 9-digit number without country code
            digits.length >= 9 -> "+254${digits.take(9)}"
            else -> phone
        }
    }
    
    private fun checkPhoneNumber(phoneForServer: String, phoneDisplay: String) {
        errorText.visibility = View.VISIBLE
        errorText.text = "Verifying phone number..."
        
        Log.d(TAG, "Checking phone: $phoneForServer (display: $phoneDisplay)")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/customer/check-phone")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                val jsonBody = JSONObject()
                jsonBody.put("phone_number", phoneForServer)
                
                connection.outputStream.write(jsonBody.toString().toByteArray())
                
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    
                    withContext(Dispatchers.Main) {
                        val enrolled = jsonResponse.optBoolean("enrolled", false)
                        val registered = jsonResponse.optBoolean("registered", false)
                        val action = jsonResponse.optString("action", "contact_support")
                        val customerName = jsonResponse.optString("customer_name", "")
                        
                        when (action) {
                            "login" -> {
                                // Customer is enrolled and registered - show login
                                val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
                                prefs.edit().putString("customer_phone", phoneForServer).apply()
                                
                                errorText.visibility = View.VISIBLE
                                errorText.text = "Account found! Please enter your PIN."
                                
                                // Small delay for better UX, then show login
                                newPhoneInput.postDelayed({
                                    showReturningUserLogin(phoneDisplay) // Show 07 format for display
                                }, 1000)
                            }
                            "register" -> {
                                // Customer is enrolled but not registered - show registration
                                errorText.visibility = View.VISIBLE
                                errorText.text = "Device enrolled! Setting up your account..."
                                
                                // Small delay for better UX, then show registration
                                newPhoneInput.postDelayed({
                                    showCustomerRegistration(phoneForServer, phoneDisplay, customerName)
                                }, 1000)
                            }
                            else -> {
                                // Not enrolled
                                errorText.visibility = View.VISIBLE
                                errorText.text = "Phone number not enrolled. Please contact support to enroll your device first."
                                // Don't clear the input, let user try again or contact support
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        errorText.visibility = View.VISIBLE
                        errorText.text = "Error checking account. Please try again."
                    }
                }
                
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    errorText.visibility = View.VISIBLE
                    errorText.text = "Network error. Please check your connection."
                }
            }
        }
    }
    
    private fun setupPinBoxes() {
        val boxes = listOf(pinBox1, pinBox2, pinBox3, pinBox4)
        
        boxes.forEachIndexed { index, box ->
            box.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val text = s.toString()
                    
                    if (text.length == 1) {
                        val heartbeat = AnimationUtils.loadAnimation(this@PinEntryActivity, R.anim.heartbeat)
                        box.startAnimation(heartbeat)
                        
                        if (index < 3) {
                            boxes[index + 1].requestFocus()
                        } else {
                            // All boxes filled, verify PIN
                            verifyPin()
                        }
                    } else if (text.isEmpty() && index > 0) {
                        boxes[index - 1].requestFocus()
                    }
                }
            })
        }
    }
    
    private fun verifyPin() {
        val rawPhone = phoneInput.text.toString()
        val phone = formatPhoneForServer(rawPhone)  // Ensure consistent formatting
        val pin = pinBox1.text.toString() + 
                  pinBox2.text.toString() + 
                  pinBox3.text.toString() + 
                  pinBox4.text.toString()
        
        if (pin.length != 4) {
            showError("Please enter 4 digits")
            return
        }
        
        if (deviceFingerprint == null) {
            showError("Device fingerprint not available")
            return
        }
        
        Log.d(TAG, "Attempting login with phone: $phone (formatted from: $rawPhone)")
        
        errorText.visibility = View.VISIBLE
        errorText.text = "Verifying..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/auth/device-login")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                val jsonBody = JSONObject()
                jsonBody.put("device_fingerprint", deviceFingerprint)
                jsonBody.put("phone_number", phone)
                jsonBody.put("pin", pin)
                
                connection.outputStream.write(jsonBody.toString().toByteArray())
                
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    
                    withContext(Dispatchers.Main) {
                        if (jsonResponse.getBoolean("success")) {
                            // Save persistent token
                            val persistentToken = jsonResponse.getString("persistent_token")
                            savePersistentToken(persistentToken)
                            
                            Log.d(TAG, "Login successful with persistent token")
                            
                            // Check if PIN must be changed
                            val mustChangePin = jsonResponse.optBoolean("must_change_pin", false)
                            
                            if (mustChangePin) {
                                // Show PIN change dialog
                                showChangePinDialog(phone)
                                return@withContext
                            }
                            
                            // Check if we need to verify loan balance
                            val checkBalanceAfterLogin = intent.getBooleanExtra("check_balance_after_login", false)
                            val factoryResetRecovery = intent.getBooleanExtra("factory_reset_recovery", false)
                            
                            if (checkBalanceAfterLogin || factoryResetRecovery) {
                                // Check loan balance immediately
                                checkLoanBalanceAndProceed(phone)
                            } else {
                                // Normal login - save state and proceed
                                val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
                                prefs.edit()
                                    .putBoolean("pin_completed", true)
                                    .putString("customer_phone", phone)
                                    .putString("customer_id", jsonResponse.getString("customer_id"))
                                    .putString("device_id", jsonResponse.getString("device_id"))
                                    .apply()
                                
                                val intent = Intent(this@PinEntryActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            showError("Invalid PIN. Please try again.")
                            clearPinBoxes()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showError("Login failed. Please try again.")
                        clearPinBoxes()
                    }
                }
                
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showError("Network error. Please check your connection.")
                }
            }
        }
    }
    
    private fun showError(message: String) {
        errorText.visibility = View.VISIBLE
        errorText.text = message
        
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
        returningUserLayout.startAnimation(shake)
    }
    
    private fun clearPinBoxes() {
        pinBox1.text.clear()
        pinBox2.text.clear()
        pinBox3.text.clear()
        pinBox4.text.clear()
        pinBox1.requestFocus()
    }
    
    private fun showChangePinDialog(phone: String) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Change Your PIN")
        builder.setMessage("You must change your default PIN before continuing.")
        builder.setCancelable(false)
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_pin, null)
        builder.setView(dialogView)
        
        val newPin1 = dialogView.findViewById<EditText>(R.id.newPinBox1)
        val newPin2 = dialogView.findViewById<EditText>(R.id.newPinBox2)
        val newPin3 = dialogView.findViewById<EditText>(R.id.newPinBox3)
        val newPin4 = dialogView.findViewById<EditText>(R.id.newPinBox4)
        val confirmPin1 = dialogView.findViewById<EditText>(R.id.confirmPinBox1)
        val confirmPin2 = dialogView.findViewById<EditText>(R.id.confirmPinBox2)
        val confirmPin3 = dialogView.findViewById<EditText>(R.id.confirmPinBox3)
        val confirmPin4 = dialogView.findViewById<EditText>(R.id.confirmPinBox4)
        
        // Setup PIN box navigation for new PIN
        setupPinBoxNavigation(listOf(newPin1, newPin2, newPin3, newPin4))
        setupPinBoxNavigation(listOf(confirmPin1, confirmPin2, confirmPin3, confirmPin4))
        
        builder.setPositiveButton("Change PIN") { _, _ ->
            val newPin = newPin1.text.toString() + newPin2.text.toString() + 
                        newPin3.text.toString() + newPin4.text.toString()
            val confirmPin = confirmPin1.text.toString() + confirmPin2.text.toString() + 
                            confirmPin3.text.toString() + confirmPin4.text.toString()
            
            if (newPin.length != 4 || confirmPin.length != 4) {
                Toast.makeText(this, "Please enter 4 digits for both PINs", Toast.LENGTH_SHORT).show()
                showChangePinDialog(phone) // Show dialog again
                return@setPositiveButton
            }
            
            if (newPin != confirmPin) {
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show()
                showChangePinDialog(phone) // Show dialog again
                return@setPositiveButton
            }
            
            changePin(phone, newPin)
        }
        
        val dialog = builder.create()
        dialog.show()
        
        // Focus on first new PIN box
        newPin1.requestFocus()
        newPin1.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(newPin1, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }
    
    private fun setupPinBoxNavigation(boxes: List<EditText>) {
        boxes.forEachIndexed { index, box ->
            box.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val text = s.toString()
                    
                    if (text.length == 1) {
                        if (index < 3) {
                            boxes[index + 1].requestFocus()
                        }
                    } else if (text.isEmpty() && index > 0) {
                        boxes[index - 1].requestFocus()
                    }
                }
            })
        }
    }
    
    private fun changePin(phone: String, newPin: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/customer/set-pin")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                val jsonBody = JSONObject()
                jsonBody.put("phone_number", phone)
                jsonBody.put("pin", newPin)
                
                connection.outputStream.write(jsonBody.toString().toByteArray())
                
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    
                    withContext(Dispatchers.Main) {
                        if (jsonResponse.getBoolean("success")) {
                            Toast.makeText(this@PinEntryActivity, "PIN changed successfully!", Toast.LENGTH_SHORT).show()
                            
                            // Save login state and proceed to MainActivity
                            val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putBoolean("pin_completed", true)
                                .putString("customer_phone", phone)
                                .apply()
                            
                            val intent = Intent(this@PinEntryActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@PinEntryActivity, "Failed to change PIN: ${jsonResponse.optString("error")}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@PinEntryActivity, "Failed to change PIN. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
                
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PinEntryActivity, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun checkLoanBalanceAndProceed(phone: String) {
        errorText.visibility = View.VISIBLE
        errorText.text = "Checking loan balance..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/customer/dashboard?phone=${android.net.Uri.encode(phone)}")
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
                                errorText.text = "Outstanding balance detected - Locking device..."
                                
                                val lockIntent = Intent(this@PinEntryActivity, LockScreenActivity::class.java)
                                lockIntent.putExtra("lock_reason", "OUTSTANDING_BALANCE")
                                lockIntent.putExtra("balance_amount", loanBalance)
                                lockIntent.putExtra("customer_phone", phone)
                                lockIntent.putExtra("website_url", BASE_URL)
                                lockIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(lockIntent)
                                finish()
                            } else {
                                // Loan paid - save state and proceed
                                errorText.text = "Loan verified - Access granted"
                                
                                val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
                                prefs.edit()
                                    .putBoolean("pin_completed", true)
                                    .putString("customer_phone", phone)
                                    .apply()
                                
                                val intent = Intent(this@PinEntryActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            showError("Unable to verify loan status. Please try again.")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showError("Network error. Please check connection and try again.")
                    }
                }
                
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showError("Error checking loan balance. Please try again.")
                }
            }
        }
    }
    
    override fun onBackPressed() {
        // Disable back button
    }
}
