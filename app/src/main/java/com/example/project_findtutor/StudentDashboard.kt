package com.example.project_findtutor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class StudentDashboard : AppCompatActivity() {

    lateinit var bottomNav: BottomNavigationView
    lateinit var auth: FirebaseAuth
    lateinit var db: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_student_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference
        bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        loadFragment(StudentDashboardFragment())

        bottomNav.setOnItemSelectedListener {
            when(it.itemId){
                R.id.nav_home -> {
                    loadFragment(StudentDashboardFragment())
                    true
                }
                R.id.nav_addPost-> {
                    startActivity(Intent(this, CreatePostActivity::class.java))
                    true
                }
                R.id.nav_notification -> {
                    loadFragment(StudentNotificationFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(StudentProfileFragment())
                    true
                }
                else -> false
            }
        }

    }

    fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()
    }

    fun updateBadge(count:Int){
        val badge = bottomNav.getOrCreateBadge(R.id.nav_notification)
        if(count>0){
            badge.isVisible = true
            badge.number = count
        }else{
            badge.isVisible = false
        }

    }
}