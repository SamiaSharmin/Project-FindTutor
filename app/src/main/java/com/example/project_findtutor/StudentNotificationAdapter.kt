package com.example.project_findtutor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentNotificationAdapter(val list: List<NotificationModel>, val onSetMeetingClick: (Int, String) -> Unit): RecyclerView.Adapter<StudentNotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View): RecyclerView.ViewHolder(view){

        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val btnViewTutorDetails = view.findViewById<Button>(R.id.btnViewTutorDetails)
        val btnSetMeeting = view.findViewById<Button>(R.id.btnSetMeeting)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student_notification, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvTime.text = formatTime(item.timestamp)
        holder.tvMessage.text = "${item.tutorName} is interested in your job (ID: ${item.jobId})"

        holder.btnViewTutorDetails.setOnClickListener {
            Toast.makeText(holder.itemView.context, "View Profile", Toast.LENGTH_SHORT).show()
        }

        holder.btnSetMeeting.setOnClickListener {
//            Toast.makeText(holder.itemView.context, "Set Meeting", Toast.LENGTH_SHORT).show()
            onSetMeetingClick(item.jobId, item.tutorId)
        }
    }

    fun formatTime(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return format.format(date)
    }

}