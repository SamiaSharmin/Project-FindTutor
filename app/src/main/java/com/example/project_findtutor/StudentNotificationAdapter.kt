package com.example.project_findtutor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
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
            showTutorDetailsDialog(holder, item)
            //Toast.makeText(holder.itemView.context, "View Profile", Toast.LENGTH_SHORT).show()
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

    fun showTutorDetailsDialog(holder: ViewHolder, item: NotificationModel) {
        val context = holder.itemView.context

        if (item.tutorId.isBlank()) {
            Toast.makeText(context, "Tutor ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseDatabase.getInstance()
            .getReference("Tutors")
            .child(item.tutorId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    Toast.makeText(context, "Tutor details not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val name = getSnapshotText(snapshot, "name")
                    .ifBlank { item.tutorName.ifBlank { "Not provided" } }

                val email = getSnapshotText(snapshot, "email")
                val phoneNumber = getSnapshotText(snapshot, "phoneNumber", "phone", "mobile")
                val gender = getSnapshotText(snapshot, "gender")
                val subject = getSnapshotText(snapshot, "subject", "subjects", "preferredSubject")
                val qualification = getSnapshotText(snapshot, "qualification", "education")
                val experience = getSnapshotText(snapshot, "experience")
                val location = getSnapshotText(snapshot, "location", "address", "area")
                val bio = getSnapshotText(snapshot, "bio", "about")

                val message = buildString {
                    append("Name: ${name.ifBlank { "Not provided" }}\n\n")
                    append("Email: ${email.ifBlank { "Not provided" }}\n\n")
                    append("Phone Number: ${phoneNumber.ifBlank { "Not provided" }}\n\n")

                    if (gender.isNotBlank()) {
                        append("Gender: $gender\n\n")
                    }

                    if (subject.isNotBlank()) {
                        append("Subject: $subject\n\n")
                    }

                    if (qualification.isNotBlank()) {
                        append("Qualification: $qualification\n\n")
                    }

                    if (experience.isNotBlank()) {
                        append("Experience: $experience\n\n")
                    }

                    if (location.isNotBlank()) {
                        append("Location: $location\n\n")
                    }

                    if (bio.isNotBlank()) {
                        append("About: $bio\n\n")
                    }

                    append("Interested Job ID: ${item.jobId}")
                }

                AlertDialog.Builder(context)
                    .setTitle("Tutor Details")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Failed to load tutor details: ${exception.message}", Toast.LENGTH_SHORT).show()
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
