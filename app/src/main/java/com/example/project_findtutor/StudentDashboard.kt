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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class StudentDashboard : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var db: DatabaseReference
    lateinit var tvStudentName: TextView
    lateinit var btnPreviousPost: Button
    lateinit var btnCreatePost: Button
    lateinit var btnLogout: ImageButton
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
        tvStudentName = findViewById<TextView>(R.id.tvStudentName)
        btnPreviousPost = findViewById<Button>(R.id.btnPreviousPost)
        btnCreatePost = findViewById<Button>(R.id.btnCreatePost)
        btnLogout = findViewById<ImageButton>(R.id.btnLogout)

        loadStudentData()

        btnCreatePost.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    fun loadStudentData(){

        val user = auth.currentUser
        if(user!=null){
            val userId = user.uid
            db.child("Students").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.exists()){
                            val name = snapshot.child("name").value.toString()
                            tvStudentName.text = "Welcome, $name"
                        }else{
                            tvStudentName.text = "Welcome, Student"

                        }}
                    override fun onCancelled(error: DatabaseError) {
                        tvStudentName.text = "Error loading name"

                    }
                })
        }

    }
}