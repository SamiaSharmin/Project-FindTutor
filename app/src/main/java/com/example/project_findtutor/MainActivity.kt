package com.example.project_findtutor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    lateinit var etEmail: EditText
    lateinit var etPassword: EditText
    lateinit var btnLogin: Button
    lateinit var btnSignup: Button
    lateinit var btnForgotPass: Button
    lateinit var auth: FirebaseAuth
    lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference


        etEmail = findViewById<EditText>(R.id.etEmail)
        etPassword = findViewById<EditText>(R.id.etPassword)
        btnLogin = findViewById<Button>(R.id.btnLogin)
        btnSignup = findViewById<Button>(R.id.btnSignup)
        btnForgotPass = findViewById<Button>(R.id.btnForgotPass)

        btnSignup.setOnClickListener {
            val intent = Intent(this, RoleRegisterActivity::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            loginUser()
        }

    }

    fun loginUser(){
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if(email.isEmpty()){
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return
        }

        if(password.isEmpty()){
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val userId = auth.currentUser!!.uid
                    database.child("Users").child(userId).child("role")
                        .addListenerForSingleValueEvent(object: ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if(snapshot.exists()){
                                    val role = snapshot.value.toString()
                                    when(role){
                                        "student"->{
                                            startActivity(Intent(this@MainActivity, StudentDashboard::class.java))
                                        }

                                        "tutor"->{
                                            startActivity(Intent(this@MainActivity, TutorDashboard::class.java))
                                        }

                                        "admin"->{
                                            startActivity(Intent(this@MainActivity, AdminDashboard::class.java))
                                        }
                                        else ->{
                                            Toast.makeText(this@MainActivity, "Role not registered", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    finish()
                                }else {
                                    Toast.makeText(this@MainActivity, "Account not registered", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {

                                Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                }else{
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}