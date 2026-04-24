package com.example.project_findtutor

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    lateinit var auth: FirebaseAuth
    lateinit var db: DatabaseReference
    lateinit var tvName: TextView
    lateinit var tvRating: TextView
    lateinit var tvQualification: TextView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference
        tvName = view.findViewById(R.id.tvName)
        tvRating = view.findViewById(R.id.tvRating)
        tvQualification = view.findViewById(R.id.tvQualification)

        val userId = auth.currentUser?.uid
        if(userId == null){
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        db.child("Users").child("Tutors").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if(snapshot.exists()){
                        val name = snapshot.child("name").value.toString()?:"N/A"
                        val rating = snapshot.child("rating").value.toString()?:"0.0"
                        val qualification = snapshot.child("qualification").value.toString()?:"N/A"

                        tvName.text = name
                        tvRating.text = rating
                        tvQualification.text = qualification

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load date", Toast.LENGTH_SHORT).show()
                }

            })
    }
}