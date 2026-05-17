package com.example.project_findtutor

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TutorPostListFragment : Fragment(R.layout.fragment_tutor_post_list) {

    lateinit var auth: FirebaseAuth
    lateinit var db: DatabaseReference
    lateinit var recyclerView: RecyclerView

    val postList = mutableListOf<Post>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference
        recyclerView = view.findViewById(R.id.recyclerPosts)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadPosts()

    }

    fun loadPosts(){
        db.child("Posts")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    postList.clear()
                    for (data in snapshot.children) {
                        val post = data.getValue(Post::class.java)
                        if(post != null) {
                            postList.add(post)
                        }
                    }
                    recyclerView.adapter = TutorPostAdapter( postList,
                        onInterestedClick = { post ->
                            markInterested(post)
                        },
                        onDetailsClick = { post ->
                            showStudentAndPostDetails(post)
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()

                }
            })
    }

    fun markInterested(post: Post) {
        val tutorId = auth.currentUser?.uid ?: return
        val studentId = post.userId

        if (studentId.isBlank()) {
            Toast.makeText(requireContext(), "Student ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        db.child("Tutors").child(tutorId).get()
            .addOnSuccessListener { tutorSnapshot ->

                val tutorName = tutorSnapshot.child("name").value?.toString().orEmpty()
                    .ifBlank { "A tutor" }

                val interestRef = db.child("interests")
                    .child(post.jobId.toString())
                    .child(tutorId)

                interestRef.get()
                    .addOnSuccessListener { interestSnapshot ->

                        if (interestSnapshot.exists()) {
                            Toast.makeText(
                                requireContext(),
                                "Already marked interested",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@addOnSuccessListener
                        }

                        interestRef.setValue(true)
                            .addOnSuccessListener {

                                val notification = mapOf(
                                    "jobId" to post.jobId,
                                    "tutorId" to tutorId,
                                    "tutorName" to tutorName,
                                    "message" to "$tutorName is interested in your job (ID: ${post.jobId})",
                                    "type" to "interest",
                                    "timestamp" to System.currentTimeMillis(),
                                    "isRead" to false
                                )

                                db.child("Notifications")
                                    .child(studentId)
                                    .push()
                                    .setValue(notification)
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(), "Interested sent", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(requireContext(), "Failed to send notification", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Failed to mark interested", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to check interest status", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load tutor data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showStudentAndPostDetails(post: Post) {
        val studentId = post.userId

        if (studentId.isBlank()) {
            Toast.makeText(requireContext(), "Student ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        db.child("Students").child(studentId).get()
            .addOnSuccessListener { snapshot ->

                val studentName = getSnapshotText(snapshot, "name")
                val studentPhone = getSnapshotText(snapshot, "phoneNumber", "phone", "mobile")
                val studentLocation = getSnapshotText(snapshot, "location", "address", "area")

                val detailsMessage = buildString {
                    append("Student Information\n")
                    append("-------------------------\n")
                    append("Name: ${studentName.ifBlank { "Not provided" }}\n")
                    append("Phone Number: ${studentPhone.ifBlank { "Not provided" }}\n")

                    if (studentLocation.isNotBlank()) {
                        append("Location: $studentLocation\n")
                    }

                    append("\nPost Details\n")
                    append("-------------------------\n")
                    append("Job ID: ${post.jobId}\n")
                    append("Title: ${post.title}\n")
                    append("Subjects: ${post.subjects}\n")
                    append("Location: ${post.location}\n")
                    append("Student Class: ${post.studentClass}\n")
                    append("Salary: ${post.salary} BDT\n")
                    append("Description: ${post.description}\n")
                    append("Student Gender: ${post.studentGender}\n")
                    append("Prefered Gender: ${post.tutorGender}\n")
                    append("Posted Date: ${post.postedDate}\n")
                }

                AlertDialog.Builder(requireContext())
                    .setTitle("Job Details")
                    .setMessage(detailsMessage)
                    .setPositiveButton("OK", null)
                    .show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    requireContext(),
                    "Failed to load details: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun getSnapshotText(snapshot: DataSnapshot, vararg keys: String): String {
        for (key in keys) {
            val value = snapshot.child(key).value
            if (value != null && value.toString().isNotBlank()) {
                return value.toString()
            }
        }
        return ""
    }

}