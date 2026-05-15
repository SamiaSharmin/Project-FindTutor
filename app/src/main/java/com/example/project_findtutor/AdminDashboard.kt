package com.example.project_findtutor

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminDashboard : AppCompatActivity() {
    lateinit var bottomNav: BottomNavigationView
    lateinit var tvAdminDashboardTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvAdminDashboardTitle = findViewById(R.id.tvAdminDashboardTitle)
        bottomNav = findViewById<BottomNavigationView>(R.id.adminBottomNavigation)
        loadFragment(AdminDashboardFragment())

        setupHeaderActions()
        setupBottomNavigation()

        if (savedInstanceState == null) {
            tvAdminDashboardTitle.text = "Admin Dashboard"
            loadFragment(AdminDashboardFragment())
        }
    }

    fun setupBottomNavigation(){
        bottomNav.setOnItemSelectedListener {
            when(it.itemId){
                R.id.navAdminDashboard -> {
                    tvAdminDashboardTitle.text = "Admin Dashboard"
                    loadFragment(AdminDashboardFragment())
                    true
                }
                R.id.navAdminUsers -> {
                    tvAdminDashboardTitle.text = "View Users"
                    loadFragment(AdminManageUsersFragment())
                    true
                }
                R.id.navAdminMeetings -> {
                    tvAdminDashboardTitle.text = "Meetings"
                    loadFragment(AdminUserMeetingsFragment())
                    true
                }
                R.id.navAdminPosts -> {
                    tvAdminDashboardTitle.text = "Posts"
                    loadFragment(AdminManagePostsFragment())
                    true
                }
                R.id.navAdminReports -> {
                    tvAdminDashboardTitle.text = "Reports"
                    loadFragment(AdminUserReportsFragment())
                    true
                }
                else -> false
            }
        }
    }

    fun loadFragment(fragment: androidx.fragment.app.Fragment){
        supportFragmentManager.beginTransaction().replace(R.id.adminFragmentContainer, fragment).commit()
    }

    private fun setupHeaderActions() {
        findViewById<View>(R.id.btnAdminNotifications).setOnClickListener {
            Toast.makeText(this, "Admin notifications will be added later", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnAdminSettings).setOnClickListener {
            Toast.makeText(this, "Admin settings will be added later", Toast.LENGTH_SHORT).show()
        }
    }
}
