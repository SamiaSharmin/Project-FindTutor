package com.example.project_findtutor

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class StudentDashboardFragment : Fragment(R.layout.fragment_student_dashboard) {

    lateinit var auth: FirebaseAuth
    lateinit var db: DatabaseReference
    lateinit var tvName: TextView
    lateinit var tvRating: TextView
    lateinit var rvPost: RecyclerView
    val postList = mutableListOf<Post>()


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
        rvPost = view.findViewById(R.id.rvPosts)
        rvPost.layoutManager= LinearLayoutManager(requireContext())
        rvPost.adapter = StudentPostAdapter(postList)


        loadStudentInfo()
        loadPost()
    }

    fun loadStudentInfo(){
        val userId = auth.currentUser?.uid?:return
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
        }

        db.child("Students").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    val name = snapshot.child("name").getValue(String::class.java)
                    val rating = snapshot.child("rating").getValue(Int::class.java)

                    tvName.text = name
                    tvRating.text = "Rating:${rating?:0}"
                }else{
                    println("DEBUG: Student info not found")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun loadPost(){
        val userId = FirebaseAuth.getInstance().currentUser?.uid?:return

        db.child("Posts").orderByChild("userId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    postList.clear()
                    for (data in snapshot.children) {
                        val post = data.getValue(Post::class.java)
                        if (post != null) {
                            postList.add(post)
                        }
                    }
                    rvPost.adapter?.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
                }
            })
    }

}