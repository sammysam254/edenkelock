package com.eden.mkopa

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class MainActivityNew : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_new)
        
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, DeviceAdminReceiver::class.java)
        
        setupToolbar()
        setupNavigationDrawer()
        
        // Check if device owner
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            setupDeviceOwner()
            startLockTask()
        }
        
        loadDashboardData()
    }
    
    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
    }
    
    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener(this)
        
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }
    
    private fun setupDeviceOwner() {
        try {
            devicePolicyManager.addUserRestriction(adminComponent, "no_factory_reset")
            devicePolicyManager.addUserRestriction(adminComponent, "no_safe_boot")
            devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf(packageName))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun loadDashboardData() {
        // Load device status
        val deviceStatusText = findViewById<TextView>(R.id.deviceStatusText)
        deviceStatusText.text = "Active"
        
        // Load payment info
        val balanceAmount = findViewById<TextView>(R.id.balanceAmount)
        balanceAmount.text = "$0.00"
        
        val nextPaymentDate = findViewById<TextView>(R.id.nextPaymentDate)
        nextPaymentDate.text = "Apr 1, 2026"
    }
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // Already on home
            }
            R.id.nav_payments -> {
                // Navigate to payments
            }
            R.id.nav_device -> {
                // Navigate to device info
            }
            R.id.nav_support -> {
                // Navigate to support
            }
            R.id.nav_settings -> {
                // Navigate to settings
            }
        }
        
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            // Don't allow back in kiosk mode
        }
    }
}
