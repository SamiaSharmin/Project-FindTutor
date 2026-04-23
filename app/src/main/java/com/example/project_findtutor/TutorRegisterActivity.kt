package com.example.project_findtutor

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.FirebaseDatabase

class TutorRegisterActivity : AppCompatActivity() {

    lateinit var name: EditText
    lateinit var email: EditText
    lateinit var password: EditText
    lateinit var confirmPassword: EditText
    lateinit var phoneNumber: EditText
    lateinit var qualification: EditText
    lateinit var preferredAreas: EditText
    lateinit var btnTutorRegister: Button
    lateinit var tvTutorLogin: Button

    lateinit var auth : FirebaseAuth
    val db = FirebaseDatabase.getInstance().reference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tutor_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        name = findViewById<EditText>(R.id.etTutorName)
        email = findViewById<EditText>(R.id.etTutorEmail)
        password = findViewById<EditText>(R.id.etTutorPassword)
        confirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        phoneNumber = findViewById<EditText>(R.id.etPhoneNumber)
        qualification = findViewById<EditText>(R.id.etQualification)
        preferredAreas = findViewById<EditText>(R.id.etPreferedAreas)
        btnTutorRegister = findViewById<Button>(R.id.btnTutorRegister)

        btnTutorRegister.setOnClickListener {

            val nameStr = name.text.toString().trim()
            val emailStr = email.text.toString().trim()
            val passwordStr = password.text.toString().trim()
            val confirmPasswordStr = confirmPassword.text.toString().trim()
            val phoneNumberStr = phoneNumber.text.toString().trim()
            val qualificationStr = qualification.text.toString().trim()
            val preferredAreasStr = preferredAreas.text.toString().trim()


            if(nameStr.isEmpty()){
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                name.error = "Please enter your name"
                return@setOnClickListener
            }

            if(emailStr.isEmpty()){
                email.error = "Please enter a valid email"
                return@setOnClickListener
            }

            if(passwordStr.isEmpty()){
                password.error = "Please enter a valid password"
                return@setOnClickListener
            }
            if(passwordStr.length <8){
                password.error = "Password should be at least 8 characters"
                return@setOnClickListener
            }

            if(confirmPasswordStr != passwordStr){
                confirmPassword.error = "Password does not match"
                return@setOnClickListener
            }

            if(phoneNumberStr.isEmpty() || phoneNumberStr.length != 11){
                phoneNumber.error = "Please enter a valid phone number"
                return@setOnClickListener
            }

            if(qualificationStr.isEmpty()){
                qualification.error = "Please enter your qualification"
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(emailStr, passwordStr).addOnSuccessListener {
                val userId = it.user!!.uid
                val tutor = Tutor(userId, nameStr, emailStr, phoneNumberStr, qualificationStr, preferredAreasStr, 0.0)
                val user = User(userId, nameStr, emailStr, "tutor")
                db.child("Users").child(userId).setValue(user)
                db.child("Tutors").child(userId).setValue(tutor)
                Toast.makeText(this, "Tutor registered successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }



        }

    }


}