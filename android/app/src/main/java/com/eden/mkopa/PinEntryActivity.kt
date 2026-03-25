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
        
        // Pre-fill phone number
        phoneInput.setText(savedPhone)
        phoneInput.isEnabled = false
        
        // Setup PIN boxes
        setupPinBoxes()
        
        // Focus on first PIN box
        pinBox1.requestFocus()
        pinBox1.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(pinBox1, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
        
        // Add "Not you?" button functionality
        findViewById<TextView>(R.id.notYouText).setOnClickListener {
            // Clear saved data and show new user flow
            val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            showNewUserPhoneEntry()
        }
    }
    
    private fun showNewUserPhoneEntry() {
        returningUserLayout.visibility = View.GONE
        newUserLayout.visibility = View.VISIBLE
        titleText.text = "Enter Your Phone Number"
        
        newPhoneInput.requestFocus()
        newPhoneInput.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(newPhoneInput, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
        
        newPhoneInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val phone = s.toString()
                if (phone.length >= 10) {
                    // Check if account exists
                    checkPhoneNumber(phone)
                }
            }
        })
    }
    
    private fun checkPhoneNumber(phone: String) {
        errorText.visibility = View.VISIBLE
        errorText.text = "Checking account..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/customer/check-phone")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                val jsonBody = JSONObject()
                jsonBody.put("phone_number", phone)
                
                connection.outputStream.write(jsonBody.toString().toByteArray())
                
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    
                    withContext(Dispatchers.Main) {
                        if (jsonResponse.getBoolean("exists")) {
                            // Account exists - save phone and show PIN entry
                            val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("customer_phone", phone).apply()
                            
                            errorText.visibility = View.GONE
                            showReturningUserLogin(phone)
                        } else {
                            // Account doesn't exist
                            errorText.visibility = View.VISIBLE
                            errorText.text = "Account not found. Please contact support."
                            newPhoneInput.text.clear()
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
        val phone = phoneInput.text.toString()
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
