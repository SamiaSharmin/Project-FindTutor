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

class StudentNotificationAdapter(
    private val list: List<NotificationModel>,
    private val onSetMeetingClick: (Int, String) -> Unit,
    private val onReviewClick: (NotificationModel) -> Unit
) : RecyclerView.Adapter<StudentNotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val btnViewTutorDetails: Button = view.findViewById(R.id.btnViewTutorDetails)
        val btnSetMeeting: Button = view.findViewById(R.id.btnSetMeeting)
        val btnReview: Button = view.findViewById(R.id.btnReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student_notification, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val effectiveType = getEffectiveType(item)

        holder.tvTime.text = formatTime(item.timestamp)
        holder.tvMessage.text = getNotificationMessage(item, effectiveType)

        holder.btnViewTutorDetails.visibility = View.GONE
        holder.btnSetMeeting.visibility = View.GONE
        holder.btnReview.visibility = View.GONE

        holder.btnViewTutorDetails.setOnClickListener(null)
        holder.btnSetMeeting.setOnClickListener(null)
        holder.btnReview.setOnClickListener(null)

        when (effectiveType) {
            "interest" -> bindInterestNotification(holder, item)
            "meeting_completed" -> bindMeetingCompletedNotification(holder, item)
        }
    }

    private fun getEffectiveType(item: NotificationModel): String {
        /*
         * This fixes old Firebase notifications that were saved like:
         * jobId, tutorId, tutorName, timestamp, isRead
         * but without type = "interest".
         */
        return if (
            item.type.isBlank() &&
            item.jobId != 0 &&
            item.tutorId.isNotBlank()
        ) {
            "interest"
        } else {
            item.type
        }
    }

    private fun getNotificationMessage(item: NotificationModel, effectiveType: String): String {
        return when (effectiveType) {
            "interest" -> {
                val tutorName = item.tutorName.ifBlank { "A tutor" }
                "$tutorName is interested in your job (ID: ${item.jobId})"
            }

            "meeting_status" -> {
                item.message.ifBlank { "Your meeting status has been updated." }
            }

            "meeting_completed" -> {
                val tutorName = item.tutorName.ifBlank { "your tutor" }
                "Meeting with $tutorName completed. Please rate your tutor."
            }

            "report_investigation_started" -> {
                item.message.ifBlank {
                    "Your report is now under investigation by the admin team."
                }
            }

            "review_investigation_started" -> {
                item.message.ifBlank {
                    "Your review is now under investigation by the admin team."
                }
            }

            else -> {
                item.message.ifBlank { "You have a new notification." }
            }
        }
    }

//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val item = list[position]
//
//        holder.tvTime.text = formatTime(item.timestamp)
//        holder.tvMessage.text = when (item.type) {
//            "meeting_status" -> item.message
//            "interest" -> "${item.tutorName} is interested in your job (ID: ${item.jobId})"
//            "meeting_completed" -> "Meeting with ${item.tutorName}. Please rate your tutor."
//            "report_investigation_started" -> item.message.ifBlank { "Your report is now under investigation by the admin team." }
//            "review_investigation_started" -> item.message.ifBlank { "Your review is now under investigation by the admin team." }
//            else -> item.message.ifBlank { "You have a new notification." }
//        }
//
//        holder.btnViewTutorDetails.visibility = View.GONE
//        holder.btnSetMeeting.visibility = View.GONE
//        holder.btnReview.visibility = View.GONE
//
//        holder.btnViewTutorDetails.setOnClickListener(null)
//        holder.btnSetMeeting.setOnClickListener(null)
//        holder.btnReview.setOnClickListener(null)
//
//        when (item.type) {
//            "interest" -> bindInterestNotification(holder, item)
//            "meeting_completed" -> bindMeetingCompletedNotification(holder, item)
//        }
//    }

    private fun bindInterestNotification(holder: ViewHolder, item: NotificationModel) {
        val studentId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        holder.btnViewTutorDetails.visibility = View.VISIBLE
        holder.btnViewTutorDetails.setOnClickListener {
            Toast.makeText(holder.itemView.context, "View Profile", Toast.LENGTH_SHORT).show()
        }

        FirebaseDatabase.getInstance()
            .getReference("Meetings")
            .get()
            .addOnSuccessListener { snapshot ->
                if (holder.bindingAdapterPosition == RecyclerView.NO_POSITION) return@addOnSuccessListener

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

                when (meetingFound?.status?.lowercase(Locale.ROOT)) {
                    null, "rejected" -> {
                        holder.btnSetMeeting.visibility = View.VISIBLE
                        holder.btnSetMeeting.setOnClickListener {
                            onSetMeetingClick(item.jobId, item.tutorId)
                        }
                    }

                    "accepted" -> {
                        holder.btnReview.visibility = View.VISIBLE
                        holder.btnReview.setOnClickListener {
                            onReviewClick(item)
                        }
                    }

                    else -> {
                        holder.btnSetMeeting.visibility = View.GONE
                        holder.btnReview.visibility = View.GONE
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(holder.itemView.context, "Failed to check meeting status", Toast.LENGTH_SHORT).show()
            }
    }

    private fun bindMeetingCompletedNotification(holder: ViewHolder, item: NotificationModel) {
        holder.btnReview.visibility = View.VISIBLE
        holder.btnReview.setOnClickListener {
            onReviewClick(item)
        }
    }

    private fun formatTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""

        val date = Date(timestamp)
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return format.format(date)
    }
}

//class StudentNotificationAdapter(val list: List<NotificationModel>, val onSetMeetingClick: (Int, String) -> Unit, val onReviewClick: (NotificationModel)->Unit)
//    : RecyclerView.Adapter<StudentNotificationAdapter.ViewHolder>() {
//
//    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//
//        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
//        val tvTime = view.findViewById<TextView>(R.id.tvTime)
//        val btnViewTutorDetails = view.findViewById<Button>(R.id.btnViewTutorDetails)
//        val btnSetMeeting = view.findViewById<Button>(R.id.btnSetMeeting)
//        val btnReview = view.findViewById<Button>(R.id.btnReview)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_student_notification, parent, false)
//        return ViewHolder(view)
//    }
//
//    override fun getItemCount(): Int = list.size
//
//    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val item = list[position]
//        holder.tvTime.text = formatTime(item.timestamp)
//
//        holder.tvMessage.text = when (item.type) {
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
//        val auth = FirebaseAuth.getInstance()
//        val studentId = auth.currentUser?.uid ?: return
//
//        holder.btnViewTutorDetails.visibility = View.VISIBLE
//
//        holder.btnViewTutorDetails.setOnClickListener {
//            Toast.makeText(
//                holder.itemView.context,
//                "View Profile",
//                Toast.LENGTH_SHORT
//            ).show()
//        }
//
//        if (item.type == "meeting_completed") {
//            holder.btnReview.visibility = View.VISIBLE
//
//            holder.btnReview.setOnClickListener {
//                onReviewClick(item)
//            }
//
//            return
//        }
//
//        if (item.type == "interest") {
//
//            FirebaseDatabase.getInstance()
//                .getReference("Meetings")
//                .get()
//                .addOnSuccessListener { snapshot ->
//
//                    var meetingFound: Meeting? = null
//
//                    for (data in snapshot.children) {
//
//                        val meeting = data.getValue(Meeting::class.java)
//
//                        if (
//                            meeting != null &&
//                            meeting.studentId == studentId &&
//                            meeting.tutorId == item.tutorId &&
//                            meeting.jobId == item.jobId
//                        ) {
//                            meetingFound = meeting
//                            break
//                        }
//                    }
//
//                    if (meetingFound == null) {
//
//                        holder.btnSetMeeting.visibility = View.VISIBLE
//
//                    } else {
//
//                        when (meetingFound.status.lowercase()) {
//
//                            "pending" -> {
//                                holder.btnSetMeeting.visibility = View.GONE
//                            }
//
//                            "accepted" -> {
//                                holder.btnReview.visibility = View.VISIBLE
//
//                                holder.btnReview.setOnClickListener {
//                                    onReviewClick(item)
//                                }
//
//                            }
//
//                            "rejected" -> {
//                                holder.btnSetMeeting.visibility = View.VISIBLE
//                            }
//                        }
//                    }
//                }
//
//            holder.btnSetMeeting.setOnClickListener {
//                onSetMeetingClick(item.jobId, item.tutorId)
//            }
//        }
//    }
//
//    fun formatTime(timestamp: Long): String {
//        val date = Date(timestamp)
//        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
//        return format.format(date)
//    }
//
//}