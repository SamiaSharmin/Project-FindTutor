package com.example.project_findtutor

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class ForgetPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etForgotEmail: EditText
    private lateinit var btnResetPassword: Button
    private lateinit var tvBackToLogin: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forget_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        etForgotEmail = findViewById(R.id.etForgotEmail)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        btnResetPassword.setOnClickListener {
            sendPasswordResetEmail()
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    fun sendPasswordResetEmail() {
        val email = etForgotEmail.text.toString().trim()

        if (email.isEmpty()) {
            etForgotEmail.error = "Email is required"
            etForgotEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etForgotEmail.error = "Enter a valid email"
            etForgotEmail.requestFocus()
            return
        }

        btnResetPassword.isEnabled = false
        btnResetPassword.text = "Sending..."

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(this, "Password reset email sent. Please check your inbox.", Toast.LENGTH_LONG).show()

                btnResetPassword.isEnabled = true
                btnResetPassword.text = "Send Reset Link"

                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                btnResetPassword.isEnabled = true
                btnResetPassword.text = "Send Reset Link"

                Toast.makeText(this, "Failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }
}