package com.example.project_findtutor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeetingReminderAdapter(val list: List<Meeting>, val onClick:(Meeting)->Unit)
    : RecyclerView.Adapter<MeetingReminderAdapter.ViewHolder>(){

    class ViewHolder(view: View):RecyclerView.ViewHolder(view){
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvText: TextView = view.findViewById(R.id.tvText)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvView: TextView = view.findViewById(R.id.tvView)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meeting_reminder, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val meeting = list[position]
        val status = getStatus(meeting)
        holder.tvStatus.text = status
        holder.tvTime.text = meeting.time
        holder.tvText.text = "Meeting with ${meeting.studentName}"
        holder.tvDate.text = meeting.date

        when (status) {
            "TODAY" -> holder.tvStatus.setTextColor(Color.parseColor("#FF9800")) // orange
            "UPCOMING" -> holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")) // green
            "PAST" -> holder.tvStatus.setTextColor(Color.GRAY)
            else -> holder.tvStatus.setTextColor(Color.parseColor("#999999"))
        }

        holder.tvView.setOnClickListener {
            onClick(meeting)
        }

    }

    fun getStatus(meeting: Meeting): String{
        return try {
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            val meetingDateTime = format.parse("${meeting.date} ${meeting.time}")
            val now = Date()

            if (meeting.status != "accepted") return "PENDING"

            val diff = meetingDateTime!!.time - now.time

            return when {
                diff < 0 -> "PAST"
                diff < 24 * 60 * 60 * 1000 -> "TODAY"
                else -> "UPCOMING"
            }

        } catch (e: Exception) {
            "UNKNOWN"
        }
    }
}