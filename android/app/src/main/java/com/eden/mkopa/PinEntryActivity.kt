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
import androidx.appcompat.app.AppCompatActivity

class PinEntryActivity : AppCompatActivity() {
    
    private var pinCode = ""
    private val correctPin = "1234" // Default PIN
    
    private lateinit var phoneNumberLayout: LinearLayout
    private lateinit var pinLayout: LinearLayout
    private lateinit var phoneInput: EditText
    private lateinit var pinBox1: EditText
    private lateinit var pinBox2: EditText
    private lateinit var pinBox3: EditText
    private lateinit var pinBox4: EditText
    private lateinit var errorText: TextView
    private lateinit var titleText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_pin_entry)
            
            // Initialize views
            phoneNumberLayout = findViewById(R.id.phoneNumberLayout)
            pinLayout = findViewById(R.id.pinLayout)
            phoneInput = findViewById(R.id.phoneInput)
            pinBox1 = findViewById(R.id.pinBox1)
            pinBox2 = findViewById(R.id.pinBox2)
            pinBox3 = findViewById(R.id.pinBox3)
            pinBox4 = findViewById(R.id.pinBox4)
            errorText = findViewById(R.id.errorText)
            titleText = findViewById(R.id.titleText)
            
            // Check if phone number is already saved
            val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
            val savedPhone = prefs.getString("customer_phone", null)
            
            if (savedPhone != null) {
                // Phone already saved, show PIN entry
                showPinEntry()
            } else {
                // Show phone number entry first
                showPhoneEntry()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            // If anything fails, go to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
    
    private fun showPhoneEntry() {
        phoneNumberLayout.visibility = View.VISIBLE
        pinLayout.visibility = View.GONE
        titleText.text = "Enter Your Phone Number"
        
        // Auto-focus and show keyboard
        phoneInput.requestFocus()
        phoneInput.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(phoneInput, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
        
        phoneInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val phone = s.toString()
                if (phone.length >= 10) {
                    // Save phone and move to PIN
                    val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("customer_phone", phone).apply()
                    showPinEntry()
                }
            }
        })
    }
    
    private fun showPinEntry() {
        phoneNumberLayout.visibility = View.GONE
        pinLayout.visibility = View.VISIBLE
        titleText.text = "Enter Your PIN"
        
        setupPinBoxes()
        
        // Auto-focus first box and show keyboard
        pinBox1.requestFocus()
        pinBox1.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(pinBox1, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
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
                        // Animate box
                        val heartbeat = AnimationUtils.loadAnimation(this@PinEntryActivity, R.anim.heartbeat)
                        box.startAnimation(heartbeat)
                        
                        // Move to next box
                        if (index < 3) {
                            boxes[index + 1].requestFocus()
                        } else {
                            // All boxes filled, check PIN
                            checkPin()
                        }
                    } else if (text.isEmpty() && index > 0) {
                        // Move back to previous box on delete
                        boxes[index - 1].requestFocus()
                    }
                }
            })
        }
    }
    
    private fun checkPin() {
        val enteredPin = pinBox1.text.toString() + 
                        pinBox2.text.toString() + 
                        pinBox3.text.toString() + 
                        pinBox4.text.toString()
        
        // Accept any 4-digit PIN for now (bypass PIN check)
        if (enteredPin.length == 4) {
            // Go to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // Not enough digits
            errorText.visibility = View.VISIBLE
            errorText.text = "Please enter 4 digits"
            
            val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
            pinLayout.startAnimation(shake)
            
            // Clear boxes after animation
            pinLayout.postDelayed({
                pinBox1.text.clear()
                pinBox2.text.clear()
                pinBox3.text.clear()
                pinBox4.text.clear()
                pinBox1.requestFocus()
                errorText.visibility = View.GONE
            }, 500)
        }
    }
    
    override fun onBackPressed() {
        // Disable back button in kiosk mode
    }
}
