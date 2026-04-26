package com.example.project_findtutor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class StudentPostAdapter(val postList: List<Post>) : RecyclerView.Adapter<StudentPostAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvJobId: TextView = itemView.findViewById(R.id.tvJobId)
        val tvTitle: TextView = itemView.findViewById(R.id.tvJobTitle)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvSubjects: TextView = itemView.findViewById(R.id.tvSubjects)
        val tvSalary: TextView = itemView.findViewById(R.id.tvSalary)
        val tvDate: TextView = itemView.findViewById(R.id.tvPostedDate)
        val tvPreferredTutor: TextView = itemView.findViewById(R.id.tvPreferredTutor)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)



    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student_post, parent, false)
        return PostViewHolder(view)
    }

    override fun getItemCount(): Int =postList.size

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {

        val post = postList[position]
        holder.tvJobId.text = "Job ID\n${post.jobId}"
        holder.tvTitle.text = post.title
        holder.tvLocation.text = post.location
        holder.tvSubjects.text = post.subjects
        holder.tvSalary.text = "${post.salary} BDT"
        holder.tvDate.text = post.postedDate
        holder.tvPreferredTutor.text = "${post.tutorGender} tutor preferred"

        holder.btnEdit.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Edit button clicked", Toast.LENGTH_SHORT).show()
        }

        holder.btnDelete.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Delete button clicked", Toast.LENGTH_SHORT).show()
        }
    }
}