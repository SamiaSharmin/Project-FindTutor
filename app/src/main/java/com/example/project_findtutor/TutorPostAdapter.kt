package com.example.project_findtutor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class TutorPostAdapter(private val list: List<Post>, private val onInterestedClick: (Post) -> Unit)
    : RecyclerView.Adapter<TutorPostAdapter.ViewHolder>(){

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val jobId: TextView = view.findViewById(R.id.tvJobId)
        val title: TextView = view.findViewById(R.id.tvJobTitle)
        val location: TextView = view.findViewById(R.id.tvLocation)
        val subjects: TextView = view.findViewById(R.id.tvSubjects)
        val salary: TextView = view.findViewById(R.id.tvSalary)
        val date : TextView = view.findViewById(R.id.tvPostedDate)
        val btnInterested: Button = view.findViewById(R.id.btnInterested)
        val btnDetails: Button = view.findViewById(R.id.btnDetails)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_job_post, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = list[position]

        holder.jobId.text = "Job ID: ${post.jobId}"
        holder.title.text = post.title
        holder.location.text = post.location
        holder.subjects.text = post.subjects
        holder.salary.text = "${post.salary} BDT"
        holder.date.text = post.postedDate

        holder.btnInterested.setOnClickListener {
            onInterestedClick(post)
        }

        holder.btnDetails.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Details button clicked", Toast.LENGTH_SHORT).show()
        }
    }
}