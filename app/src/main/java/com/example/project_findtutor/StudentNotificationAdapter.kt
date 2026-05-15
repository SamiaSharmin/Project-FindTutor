package com.example.project_findtutor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentNotificationAdapter(val list: List<NotificationModel>, val onSetMeetingClick: (Int, String) -> Unit, val onReviewClick: (NotificationModel)->Unit)
    : RecyclerView.Adapter<StudentNotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val btnViewTutorDetails = view.findViewById<Button>(R.id.btnViewTutorDetails)
        val btnSetMeeting = view.findViewById<Button>(R.id.btnSetMeeting)
        val btnReview = view.findViewById<Button>(R.id.btnReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_notification, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvTime.text = formatTime(item.timestamp)

        holder.tvMessage.text = when (item.type) {
            "meeting_status" -> item.message
            "interest" -> "${item.tutorName} is interested in your job (ID: ${item.jobId})"
            "meeting_completed" -> "Meeting with ${item.tutorName}.Please rate your tutor."
            else -> item.message
        }

        holder.btnViewTutorDetails.visibility = View.GONE
        holder.btnSetMeeting.visibility = View.GONE
        holder.btnReview.visibility = View.GONE

        val auth = FirebaseAuth.getInstance()
        val studentId = auth.currentUser?.uid ?: return

        holder.btnViewTutorDetails.visibility = View.VISIBLE

        holder.btnViewTutorDetails.setOnClickListener {
            Toast.makeText(
                holder.itemView.context,
                "View Profile",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (item.type == "meeting_completed") {
            holder.btnReview.visibility = View.VISIBLE

            holder.btnReview.setOnClickListener {
                onReviewClick(item)
            }

            return
        }

        if (item.type == "interest") {

            FirebaseDatabase.getInstance()
                .getReference("Meetings")
                .get()
                .addOnSuccessListener { snapshot ->

                    var meetingFound: Meeting? = null

                    for (data in snapshot.children) {

                        val meeting = data.getValue(Meeting::class.java)

                        if (
                            meeting != null &&
                            meeting.studentId == studentId &&
                            meeting.tutorId == item.tutorId &&
                            meeting.jobId == item.jobId
                        ) {
                            meetingFound = meeting
                            break
                        }
                    }

                    if (meetingFound == null) {

                        holder.btnSetMeeting.visibility = View.VISIBLE

                    } else {

                        when (meetingFound.status.lowercase()) {

                            "pending" -> {
                                holder.btnSetMeeting.visibility = View.GONE
                            }

                            "accepted" -> {
                                holder.btnReview.visibility = View.VISIBLE

                                holder.btnReview.setOnClickListener {
                                    onReviewClick(item)
                                }

                            }

                            "rejected" -> {
                                holder.btnSetMeeting.visibility = View.VISIBLE
                            }
                        }
                    }
                }

            holder.btnSetMeeting.setOnClickListener {
                onSetMeetingClick(item.jobId, item.tutorId)
            }
        }

//        if (item.type == "interest") {
//
//            holder.btnViewTutorDetails.visibility = View.VISIBLE
//
//            val auth = FirebaseAuth.getInstance()
//            val studentId = auth.currentUser?.uid ?: return
//
//            FirebaseDatabase.getInstance().getReference("Meetings").orderByChild("jobId").equalTo(item.jobId.toDouble()).get()
//                .addOnSuccessListener { snapshot ->
//                    var meetingExists = false
//                    var acceptedMeeting = false
//
//                    for (data in snapshot.children) {
//
//                        val meeting = data.getValue(Meeting::class.java)
//
//                        if (meeting != null &&
//                            meeting.studentId == studentId &&
//                            meeting.tutorId == item.tutorId
//                        ) {
//
//                            meetingExists = true
//
//                            if (meeting.status == "accepted") {
//                                acceptedMeeting = true
//                            }
//
//                            break
//                        }
//                    }
//
//                    when {
//                        acceptedMeeting -> {
//                            holder.btnSetMeeting.visibility = View.GONE
//                            holder.btnReview.visibility = View.VISIBLE
//                        }
//
//                        meetingExists -> {
//                            holder.btnSetMeeting.visibility = View.GONE
//                        }
//
//                        else -> {
//                            holder.btnSetMeeting.visibility = View.VISIBLE
//                        }
//                    }
//                }
//
//            holder.btnViewTutorDetails.setOnClickListener {
//                Toast.makeText(
//                    holder.itemView.context,
//                    "View Profile",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//
//            holder.btnSetMeeting.setOnClickListener {
//                onSetMeetingClick(item.jobId, item.tutorId)
//            }
    }

//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val item = list[position]
//        holder.tvTime.text = formatTime(item.timestamp)
//        holder.tvMessage.text = when(item.type){
//            "meeting_status" -> item.message
//            "interest" -> "${item.tutorName} is interested in your job (ID: ${item.jobId})"
//            "meeting_completed" -> "Meeting with ${item.tutorName}.Please rate your tutor."
//            else -> item.message
//        }
//
//        holder.btnViewTutorDetails.visibility = View.GONE
//        holder.btnSetMeeting.visibility = View.GONE
//        holder.btnReview.visibility = View.GONE
//
//        when(item.type){
//            "interest" -> {
//                holder.btnViewTutorDetails.visibility = View.VISIBLE
//                holder.btnSetMeeting.visibility = View.VISIBLE
//
//                holder.btnViewTutorDetails.setOnClickListener {
//                    Toast.makeText(holder.itemView.context, "View Profile", Toast.LENGTH_SHORT).show()
//                }
//                holder.btnSetMeeting.setOnClickListener {
//                    onSetMeetingClick(item.jobId, item.tutorId)
//                }
//            }
//            "meeting_completed" -> {
//                holder.btnReview.visibility = View.VISIBLE
//                holder.btnReview.setOnClickListener {
//                    onReviewClick(item)
//                }
//            }
//
//        }
//
////        if(item.type == "meeting_status"){
////            holder.btnSetMeeting.visibility= View.GONE
////            holder.btnViewTutorDetails.visibility= View.GONE
////        }else {
////            holder.btnSetMeeting.visibility = View.VISIBLE
////            holder.btnViewTutorDetails.visibility = View.VISIBLE
////
////            holder.btnViewTutorDetails.setOnClickListener {
////                Toast.makeText(holder.itemView.context, "View Profile", Toast.LENGTH_SHORT).show()
////            }
////
////            holder.btnSetMeeting.setOnClickListener {
//////            Toast.makeText(holder.itemView.context, "Set Meeting", Toast.LENGTH_SHORT).show()
////                onSetMeetingClick(item.jobId, item.tutorId)
////            }
////        }
//      }

    fun formatTime(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return format.format(date)
    }

}