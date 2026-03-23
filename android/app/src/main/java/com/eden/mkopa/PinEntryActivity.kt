package com.eden.mkopa

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PinEntryActivity : AppCompatActivity() {
    
    private var pinCode = ""
    private val correctPin = "1234" // Default PIN, should be configurable
    
    private lateinit var pinDots: List<View>
    private lateinit var errorText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_entry)
        
        pinDots = listOf(
            findViewById(R.id.pinDot1),
            findViewById(R.id.pinDot2),
            findViewById(R.id.pinDot3),
            findViewById(R.id.pinDot4)
        )
        
        errorText = findViewById(R.id.errorText)
        
        setupNumberPad()
    }
    
    private fun setupNumberPad() {
        val buttons = listOf(
            findViewById<Button>(R.id.btn0) to "0",
            findViewById<Button>(R.id.btn1) to "1",
            findViewById<Button>(R.id.btn2) to "2",
            findViewById<Button>(R.id.btn3) to "3",
            findViewById<Button>(R.id.btn4) to "4",
            findViewById<Button>(R.id.btn5) to "5",
            findViewById<Button>(R.id.btn6) to "6",
            findViewById<Button>(R.id.btn7) to "7",
            findViewById<Button>(R.id.btn8) to "8",
            findViewById<Button>(R.id.btn9) to "9"
        )
        
        buttons.forEach { (button, digit) ->
            button.setOnClickListener {
                addDigit(digit)
            }
        }
        
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            removeDigit()
        }
    }
    
    private fun addDigit(digit: String) {
        if (pinCode.length < 4) {
            pinCode += digit
            updatePinDisplay()
            
            if (pinCode.length == 4) {
                verifyPin()
            }
        }
    }
    
    private fun removeDigit() {
        if (pinCode.isNotEmpty()) {
            pinCode = pinCode.dropLast(1)
            updatePinDisplay()
            errorText.visibility = View.GONE
        }
    }
    
    private fun updatePinDisplay() {
        pinDots.forEachIndexed { index, dot ->
            if (index < pinCode.length) {
                dot.setBackgroundResource(R.drawable.pin_dot_filled)
            } else {
                dot.setBackgroundResource(R.drawable.pin_dot)
            }
        }
    }
    
    private fun verifyPin() {
        if (pinCode == correctPin) {
            // PIN correct, proceed to main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // PIN incorrect
            errorText.text = "Incorrect PIN. Please try again."
            errorText.visibility = View.VISIBLE
            pinCode = ""
            updatePinDisplay()
        }
    }
    
    override fun onBackPressed() {
        // Disable back button in kiosk mode
    }
}
