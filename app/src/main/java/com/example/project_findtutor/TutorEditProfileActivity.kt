package com.example.project_findtutor

import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class TutorEditProfileActivity : AppCompatActivity() {

    lateinit var etTutorName: EditText
    lateinit var etPhoneNumber: EditText
    lateinit var etQualification: EditText
    lateinit var etPreferredAreas: EditText
    lateinit var btnSave: Button

    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tutor_edit_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etTutorName = findViewById(R.id.etTutorName)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etQualification = findViewById(R.id.etQualification)
        etPreferredAreas = findViewById(R.id.etPreferredAreas)
        btnSave = findViewById(R.id.btnSave)

        auth = FirebaseAuth.getInstance()

        loadProfile()

        btnSave.setOnClickListener {
            updateProfile()
        }

    }

    fun loadProfile(){
        val userId = auth.currentUser?.uid?:return

        FirebaseDatabase.getInstance().getReference("Tutors").child(userId).get()
            .addOnSuccessListener { snapshot ->
                etTutorName.setText(snapshot.child("name").value.toString()?:"")
                etPhoneNumber.setText(snapshot.child("phoneNumber").value.toString()?:"")
                etQualification.setText(snapshot.child("qualification").value.toString()?:"")
                etPreferredAreas.setText(snapshot.child("preferedAreas").value.toString()?:"")

            }
    }

    fun updateProfile(){
        val userId = auth.currentUser?.uid?:return
        val newName  = etTutorName.text.toString().trim()
        val newPhoneNumber = etPhoneNumber.text.toString().trim()
        val newQualification = etQualification.text.toString().trim()
        val newPreferredAreas = etPreferredAreas.text.toString().trim()
        val db = FirebaseDatabase.getInstance().reference


        val updates = mapOf(
            "name" to newName,
            "phoneNumber" to newPhoneNumber,
            "qualification" to newQualification,
            "preferedAreas" to newPreferredAreas
        )

        db.child("Tutors").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                db.child("Users").child(userId).child("name").setValue(newName)
                updateNameEverywhere(newName)
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }

    }

    fun updateNameEverywhere(newName: String) {
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseDatabase.getInstance().reference

        db.child("Notifications").get().addOnSuccessListener { snapshot ->
            for (userNode in snapshot.children){
                for(notif in userNode.children){
                    val tutorId = notif.child("tutorId").value.toString()

                    if(tutorId == userId){
                        notif.ref.child("tutorName").setValue(newName)
                    }
                }
            }
        }

    }
}