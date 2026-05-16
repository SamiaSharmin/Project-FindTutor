package com.example.project_findtutor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TutorNotificationAdapter(
    private val onViewMeetingDetailsClick: (Meeting) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<TutorNotificationListItem>()

    fun submitList(newItems: List<TutorNotificationListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TutorNotificationListItem.MeetingItem -> VIEW_TYPE_MEETING
            is TutorNotificationListItem.NotificationItem -> VIEW_TYPE_NOTIFICATION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_MEETING) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_meeting_notification, parent, false)
            MeetingViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_student_notification, parent, false)
            NotificationViewHolder(view)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TutorNotificationListItem.MeetingItem -> {
                (holder as MeetingViewHolder).bind(item.meeting, onViewMeetingDetailsClick)
            }

            is TutorNotificationListItem.NotificationItem -> {
                (holder as NotificationViewHolder).bind(item.notification)
            }
        }
    }

    class MeetingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvViewMeetingDetails: TextView = view.findViewById(R.id.tvViewMeetingDetails)

        fun bind(meeting: Meeting, onViewMeetingDetailsClick: (Meeting) -> Unit) {
            tvStatus.text = meeting.status
            tvMessage.text = "${meeting.studentName.ifBlank { "A student" }} wants to set a meeting"

            tvViewMeetingDetails.visibility = View.VISIBLE
            tvViewMeetingDetails.setOnClickListener {
                onViewMeetingDetailsClick(meeting)
            }
        }
    }

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)
        private val btnViewTutorDetails: Button = view.findViewById(R.id.btnViewTutorDetails)
        private val btnSetMeeting: Button = view.findViewById(R.id.btnSetMeeting)
        private val btnReview: Button = view.findViewById(R.id.btnReview)

        fun bind(notification: NotificationModel) {
            tvTime.text = formatTime(notification.timestamp)
            tvMessage.text = notification.message.ifBlank {
                when (notification.type) {
                    "report_investigation_started" -> "Your report is now under investigation by the admin team."
                    "review_investigation_started" -> "Your review is now under investigation by the admin team."
                    else -> "You have a new notification."
                }
            }

            btnViewTutorDetails.visibility = View.GONE
            btnSetMeeting.visibility = View.GONE
            btnReview.visibility = View.GONE
        }

        private fun formatTime(timestamp: Long): String {
            if (timestamp <= 0L) return ""

            val date = Date(timestamp)
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            return format.format(date)
        }
    }

    companion object {
        private const val VIEW_TYPE_MEETING = 1
        private const val VIEW_TYPE_NOTIFICATION = 2
    }
}
