package com.example.project_findtutor

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

class StudentProfileFragment : Fragment(R.layout.fragment_student_profile) {

    lateinit var auth: FirebaseAuth
    lateinit var db: DatabaseReference
    lateinit var tvName: TextView
    lateinit var tvEmail: TextView
    lateinit var tvPhoneNumber: TextView
    lateinit var btnEditProfile: Button
    lateinit var btnChangePassword: Button
    lateinit var btnDeleteAccount: Button
    lateinit var btnReportProblem: Button
    lateinit var btnLogout: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvPhoneNumber = view.findViewById(R.id.tvPhoneNumber)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnChangePassword = view.findViewById<Button>(R.id.btnChangePassword)
        btnDeleteAccount = view.findViewById<Button>(R.id.btnDeleteAccount)
        btnReportProblem = view.findViewById<Button>(R.id.btnReportProblem)
        btnLogout = view.findViewById<Button>(R.id.btnLogout)

        loadProfile()

        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), StudentEditProfileActivity::class.java))
//            Toast.makeText(requireContext(),"Edit Profile", Toast.LENGTH_SHORT).show()
        }

        btnChangePassword.setOnClickListener {
            Toast.makeText(requireContext(),"Change Password", Toast.LENGTH_SHORT).show()
        }

        btnDeleteAccount.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Account").setMessage("Are you sure you want to delete your account?\nThis action cannot be undone.")
                .setPositiveButton("Yes") { _, _ ->
                    deleteAccount()
                }
                .setNegativeButton("No", null).setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }

        btnReportProblem.setOnClickListener {
            showReportProblemDialog()
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(),"Logged out", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }

    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    fun loadProfile(){
        val userId = auth.currentUser?.uid
        if(userId == null){
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        db.child("Students").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").value.toString()
                        val email = snapshot.child("email").value.toString()
                        val phoneNumber = snapshot.child("phoneNumber").value.toString()

                        tvName.text = "Name: $name"
                        tvEmail.text = "Email: $email"
                        tvPhoneNumber.text = "Phone Number: $phoneNumber"
                    } else {
                        if (isAdded) {
                            Toast.makeText(requireContext(),"Student id not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load date", Toast.LENGTH_SHORT).show()
                }
            })
    }

    fun deleteAccount(){
        val user= auth.currentUser
        val userId = user?.uid

        if(userId == null || user == null){
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        db.child("Students").child(userId).removeValue()
        db.child("Users").child(userId).removeValue()

        user.delete().addOnSuccessListener {
            Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Please re-log in before deleting your account", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showReportProblemDialog() {
        val problemInput = EditText(requireContext()).apply {
            hint = "Describe your problem"
            minLines = 4
            setSingleLine(false)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setPadding(24, 16, 24, 16)
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 0)
            addView(problemInput)
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Report a Problem")
            .setMessage("Tell us what issue you are facing.")
            .setView(container)
            .setPositiveButton("Submit", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val description = problemInput.text.toString().trim()

                if (description.isEmpty()) {
                    problemInput.error = "Please describe the problem"
                    return@setOnClickListener
                }

                submitProblemReport(description)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun submitProblemReport(description: String) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val reportRef = db.child("ProblemReports").push()
        val reportId = reportRef.key ?: return

        val name = tvName.text.toString()
            .replace("Name:", "")
            .trim()

        val email = tvEmail.text.toString()
            .replace("Email:", "")
            .trim()
            .ifEmpty { currentUser.email.orEmpty() }

        val phoneNumber = tvPhoneNumber.text.toString()
            .replace("Phone Number:", "")
            .trim()

        val reportData = ProblemReport(
            reportId = reportId,
            userId = currentUser.uid,
            userRole = "tutor",
            userName = name,
            userEmail = email,
            userPhoneNumber = phoneNumber,
            description = description,
            status = "pending",
            createdAt = ServerValue.TIMESTAMP
        )

        reportRef.setValue(reportData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Problem reported successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    requireContext(),
                    "Failed to report problem: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

}