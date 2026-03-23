package com.eden.mkopa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PinEntryActivity : AppCompatActivity() {
    
    private var pinCode = ""
    private val correctPin = "1234" // Default PIN, should be configurable
    
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var phoneInputLayout: LinearLayout
    private lateinit var phoneInput: EditText
    private lateinit var continueButton: Button
    private lateinit var pinDisplayLayout: LinearLayout
    private lateinit var pinBoxes: List<EditText>
    private lateinit var errorText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_entry)
        
        titleText = findViewById(R.id.titleText)
        subtitleText = findViewById(R.id.subtitleText)
        phoneInputLayout = findViewById(R.id.phoneInputLayout)
        phoneInput = findViewById(R.id.phoneInput)
        continueButton = findViewById(R.id.continueButton)
        pinDisplayLayout = findViewById(R.id.pinDisplayLayout)
        errorText = findViewById(R.id.errorText)
        
        pinBoxes = listOf(
            findViewById(R.id.pinBox1),
            findViewById(R.id.pinBox2),
            findViewById(R.id.pinBox3),
            findViewById(R.id.pinBox4)
        )
        
        checkPhoneNumber()
        
        continueButton.setOnClickListener {
            savePhoneNumber()
        }
        
        setupPinBoxes()
    }
    
    private fun checkPhoneNumber() {
        val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        val savedPhone = prefs.getString("device_phone", null)
        
        if (savedPhone == null) {
            // First time - show phone input
            showPhoneInput()
        } else {
            // Phone saved - show PIN input
            showPinInput()
        }
    }
    
    private fun showPhoneInput() {
        titleText.text = "Enter Phone Number"
        subtitleText.text = "Enter your phone number to continue"
        phoneInputLayout.visibility = View.VISIBLE
        pinDisplayLayout.visibility = View.GONE
        
        // Auto-open keyboard
        phoneInput.requestFocus()
        phoneInput.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(phoneInput, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }
    
    private fun showPinInput() {
        titleText.text = "Enter PIN"
        subtitleText.text = "Enter your 4-digit PIN to continue"
        phoneInputLayout.visibility = View.GONE
        pinDisplayLayout.visibility = View.VISIBLE
        
        // Auto-open keyboard on first box
        pinBoxes[0].requestFocus()
        pinBoxes[0].postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(pinBoxes[0], InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }
    
    private fun savePhoneNumber() {
        val phone = phoneInput.text.toString().trim()
        
        if (phone.isEmpty()) {
            errorText.text = "Please enter your phone number"
            errorText.visibility = View.VISIBLE
            return
        }
        
        if (phone.length < 10) {
            errorText.text = "Please enter a valid phone number"
            errorText.visibility = View.VISIBLE
            return
        }
        
        // Format phone number
        val formattedPhone = formatPhoneNumber(phone)
        
        // Save phone number
        val prefs = getSharedPreferences("eden_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("device_phone", formattedPhone).apply()
        
        // Show PIN input
        errorText.visibility = View.GONE
        showPinInput()
    }
    
    private fun formatPhoneNumber(phone: String): String {
        var cleaned = phone.replace(Regex("[^0-9]"), "")
        
        return when {
            cleaned.startsWith("07") -> "+254${cleaned.substring(1)}"
            cleaned.startsWith("7") && cleaned.length == 9 -> "+254$cleaned"
            cleaned.startsWith("254") -> "+$cleaned"
            cleaned.startsWith("+254") -> cleaned
            else -> cleaned
        }
    }
    
    private fun setupPinBoxes() {
        pinBoxes.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1) {
                        // Animate heartbeat
                        val anim = AnimationUtils.loadAnimation(this@PinEntryActivity, R.anim.heartbeat)
                        editText.startAnimation(anim)
                        
                        // Move to next box
                        if (index < pinBoxes.size - 1) {
                            pinBoxes[index + 1].requestFocus()
                        } else {
                            // All boxes filled, verify PIN
                            verifyPin()
                        }
                    } else if (s?.isEmpty() == true && index > 0) {
                        // Move to previous box on delete
                        pinBoxes[index - 1].requestFocus()
                    }
                }
                
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }
    
    private fun verifyPin() {
        val enteredPin = pinBoxes.joinToString("") { it.text.toString() }
        
        if (enteredPin == correctPin) {
            // PIN correct, proceed to main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // PIN incorrect
            errorText.text = "Incorrect PIN. Please try again."
            errorText.visibility = View.VISIBLE
            
            // Clear all boxes
            pinBoxes.forEach { it.text.clear() }
            pinBoxes[0].requestFocus()
            
            // Shake animation
            pinBoxes.forEach { box ->
                val shakeAnim = AnimationUtils.loadAnimation(this, R.anim.shake)
                box.startAnimation(shakeAnim)
            }
        }
    }
    
    override fun onBackPressed() {
        // Disable back button in kiosk mode
    }
}
