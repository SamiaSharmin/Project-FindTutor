package com.example.project_findtutor

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class AdminMeetingsAdapter ( private val onStatusChangeClick: (Meeting, String) -> Unit
) : RecyclerView.Adapter<AdminMeetingsAdapter.AdminMeetingViewHolder>(){

    private val meetings = mutableListOf<Meeting>()
    private val tutorNamesByMeetingId = mutableMapOf<String, String>()

    fun submitList(newMeetings: List<Meeting>) {
        meetings.clear()
        meetings.addAll(newMeetings)
        notifyDataSetChanged()
    }

    fun setTutorName(meetingId: String, tutorName: String) {
        tutorNamesByMeetingId[meetingId] = tutorName

        val index = meetings.indexOfFirst { it.meetingId == meetingId }
        if (index != -1) {
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminMeetingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_meeting, parent, false)

        return AdminMeetingViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminMeetingViewHolder, position: Int) {
        val meeting = meetings[position]
        val tutorName = tutorNamesByMeetingId[meeting.meetingId].orEmpty()
        holder.bind(meeting, tutorName, onStatusChangeClick)
    }

    override fun getItemCount(): Int {
        return meetings.size
    }

    class AdminMeetingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvMeetingStudentName: TextView =
            itemView.findViewById(R.id.tvMeetingStudentName)
        private val tvMeetingTutorName: TextView = itemView.findViewById(R.id.tvMeetingTutorName)
        private val tvMeetingStatus: TextView = itemView.findViewById(R.id.tvMeetingStatus)
        private val btnMoreMeetingOptions: TextView =
            itemView.findViewById(R.id.btnMoreMeetingOptions)

        private val tvMeetingJobId: TextView = itemView.findViewById(R.id.tvMeetingJobId)
        private val tvMeetingStudentPhone: TextView =
            itemView.findViewById(R.id.tvMeetingStudentPhone)
        private val tvMeetingDate: TextView = itemView.findViewById(R.id.tvMeetingDate)
        private val tvMeetingTime: TextView = itemView.findViewById(R.id.tvMeetingTime)
        private val tvMeetingLocation: TextView = itemView.findViewById(R.id.tvMeetingLocation)

        private val tvMeetingReviewSubmitted: TextView =
            itemView.findViewById(R.id.tvMeetingReviewSubmitted)
        private val tvMeetingReviewRating: TextView =
            itemView.findViewById(R.id.tvMeetingReviewRating)

        fun bind(
            meeting: Meeting,
            tutorName: String,
            onStatusChangeClick: (Meeting, String) -> Unit
        ) {
            tvMeetingStudentName.text =
                "Student: ${meeting.studentName.ifBlank { "Unknown Student" }}"

            tvMeetingTutorName.text = if (tutorName.isNotBlank()) {
                "Tutor: $tutorName"
            } else {
                "Tutor ID: ${meeting.tutorId.ifBlank { "Not available" }}"
            }

            tvMeetingJobId.text = if (meeting.jobId > 0) {
                meeting.jobId.toString()
            } else {
                "Not available"
            }

            tvMeetingStudentPhone.text =
                meeting.studentPhoneNumber.ifBlank { "Not available" }

            tvMeetingDate.text = meeting.date.ifBlank { "Date not set" }
            tvMeetingTime.text = meeting.time.ifBlank { "Time not set" }
            tvMeetingLocation.text = meeting.location.ifBlank { "Location not set" }

            val normalizedStatus = normalizeStatus(meeting.status)
            tvMeetingStatus.text = displayStatus(normalizedStatus)
            styleStatusBadge(tvMeetingStatus, normalizedStatus)

            if (meeting.reviewSubmitted) {
                tvMeetingReviewSubmitted.text = "Review Submitted"
                tvMeetingReviewRating.visibility = View.VISIBLE
                tvMeetingReviewRating.text =
                    String.format(Locale.US, "%.1f", meeting.reviewRating)
            } else {
                tvMeetingReviewSubmitted.text = "No Review"
                tvMeetingReviewRating.visibility = View.GONE
            }

            btnMoreMeetingOptions.setOnClickListener {
                showStatusMenu(meeting, it, onStatusChangeClick)
            }

        }

        private fun showStatusMenu(
            meeting: Meeting,
            anchor: View,
            onStatusChangeClick: (Meeting, String) -> Unit
        ) {
            val popup = PopupMenu(anchor.context, anchor)

            popup.menu.add("Mark Pending")
            popup.menu.add("Mark Confirmed")
            popup.menu.add("Mark Completed")
            popup.menu.add("Mark Cancelled")

            popup.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Mark Pending" -> onStatusChangeClick(meeting, "pending")
                    "Mark Confirmed" -> onStatusChangeClick(meeting, "confirmed")
                    "Mark Completed" -> onStatusChangeClick(meeting, "completed")
                    "Mark Cancelled" -> onStatusChangeClick(meeting, "cancelled")
                }
                true
            }

            popup.show()
        }

        private fun normalizeStatus(status: String): String {
            return when (status.trim().lowercase(Locale.ROOT)) {
                "", "pending" -> "pending"
                "confirmed", "accepted", "approved" -> "confirmed"
                "completed", "complete", "done" -> "completed"
                "cancelled", "canceled", "rejected" -> "cancelled"
                else -> status.trim().lowercase(Locale.ROOT)
            }
        }
        private fun displayStatus(status: String): String {
            return when (status) {
                "pending" -> "Pending"
                "confirmed" -> "Confirmed"
                "completed" -> "Completed"
                "cancelled" -> "Cancelled"
                else -> status.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }
            }
        }

        private fun styleStatusBadge(textView: TextView, status: String) {
            val textColor: String
            val bgColor: String

            when (status) {
                "pending" -> {
                    textColor = "#92400E"
                    bgColor = "#FEF3C7"
                }

                "confirmed" -> {
                    textColor = "#166534"
                    bgColor = "#DCFCE7"
                }

                "completed" -> {
                    textColor = "#1D4ED8"
                    bgColor = "#DBEAFE"
                }

                "cancelled" -> {
                    textColor = "#991B1B"
                    bgColor = "#FEE2E2"
                }

                else -> {
                    textColor = "#374151"
                    bgColor = "#E5E7EB"
                }
            }

            textView.setTextColor(Color.parseColor(textColor))
            textView.background = roundedBackground(bgColor, 11)
        }

        private fun roundedBackground(color: String, radiusDp: Int): GradientDrawable {
            val density = itemView.resources.displayMetrics.density

            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor(color))
                cornerRadius = radiusDp * density
            }
        }
    }
}