package com.example.project_findtutor

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
                    recyclerView.adapter = TutorPostAdapter(postList){post->
                        markInterested(post)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()

                }
            })
    }

    fun markInterested(post:Post){
        val tutorId = auth.currentUser?.uid ?: return
        db.child("interests").child(post.jobId.toString()).child(tutorId)
            .setValue(true).addOnSuccessListener {
                Toast.makeText(requireContext(), "Interested marked", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to mark interested", Toast.LENGTH_SHORT).show()
            }
    }

}