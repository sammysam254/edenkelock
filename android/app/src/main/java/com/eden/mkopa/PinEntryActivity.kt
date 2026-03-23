package com.eden.mkopa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        
        // Check if user has logged in before
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
        
        errorText.visibility = View.VISIBLE
        errorText.text = "Verifying..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/customer/login")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                val jsonBody = JSONObject()
                jsonBody.put("phone_number", phone)
                jsonBody.put("pin", pin)
                
                connection.outputStream.write(jsonBody.toString().toByteArray())
                
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    
                    withContext(Dispatchers.Main) {
                        if (jsonResponse.getBoolean("success")) {
                            // Login successful
                            val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putBoolean("pin_completed", true)
                                .putString("customer_phone", phone)
                                .apply()
                            
                            // Go to MainActivity
                            val intent = Intent(this@PinEntryActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
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
    
    override fun onBackPressed() {
        // Disable back button
    }
}
