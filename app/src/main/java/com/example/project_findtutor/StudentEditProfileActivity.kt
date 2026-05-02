package com.example.project_findtutor

import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class StudentEditProfileActivity : AppCompatActivity() {

    lateinit var etStudentName: EditText
    lateinit var etPhoneNumber: EditText
    lateinit var btnSave: Button
    lateinit var auth: FirebaseAuth
    lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_student_edit_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etStudentName = findViewById(R.id.etStudentName)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        btnSave = findViewById(R.id.btnSave)

        auth = FirebaseAuth.getInstance()
        loadProfile()

        btnSave.setOnClickListener {
            updateProfile()
        }
    }

    fun loadProfile() {
        val userId = auth.currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("Students").child(userId).get()
            .addOnSuccessListener { snapshot ->
                etStudentName.setText(snapshot.child("name").value.toString() ?: "")
                etPhoneNumber.setText(snapshot.child("phoneNumber").value.toString() ?: "")
            }
    }

    fun updateProfile(){
        val userId = auth.currentUser?.uid ?: return
        val newName = etStudentName.text.toString().trim()
        val newPhoneNumber = etPhoneNumber.text.toString().trim()

        if (newName.isEmpty() || newPhoneNumber.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPhoneNumber.length != 11) {
            Toast.makeText(this, "Phone number must be 11 digits", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseDatabase.getInstance().reference

        val updates = mapOf(
            "name" to newName,
            "phoneNumber" to newPhoneNumber
        )

        db.child("Students").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                db.child("Users").child(userId).child("name").setValue(newName)
                updateNameAndPhoneEverywhere(newName,newPhoneNumber)
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }

    }

    fun updateNameAndPhoneEverywhere(newName: String, newPhoneNumber: String) {
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseDatabase.getInstance().reference

        db.child("Meetings").get().addOnSuccessListener { snapshot ->
            for (meeting in snapshot.children) {
                val studentId = meeting.child("studentId").value.toString()
                if (studentId == userId) {
                    meeting.ref.child("studentName").setValue(newName)
                    meeting.ref.child("studentPhoneNumber").setValue(newPhoneNumber)
                }
            }
        }
    }
}