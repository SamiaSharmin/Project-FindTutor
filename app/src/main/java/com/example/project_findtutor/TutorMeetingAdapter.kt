package com.example.project_findtutor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class TutorMeetingAdapter(val list: List<Meeting>, val onViewDetailsClick: (Meeting) -> Unit)
    : RecyclerView.Adapter<TutorMeetingAdapter.ViewHolder>(){

    inner class ViewHolder(view: View):RecyclerView.ViewHolder(view){
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
        val tvViewMeetingDetails = view.findViewById<TextView>(R.id.tvViewMeetingDetails)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meeting_notification, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val meeting = list[position]
        holder.tvStatus.text = meeting.status
        holder.tvMessage.text = "${meeting.studentName} wants to set a meeting"

        holder.tvViewMeetingDetails.setOnClickListener {
            onViewDetailsClick(meeting)
        }
    }

}