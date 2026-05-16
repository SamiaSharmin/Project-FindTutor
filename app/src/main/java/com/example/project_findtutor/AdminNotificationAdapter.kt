package com.example.project_findtutor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminNotificationAdapter (
    private val notifications: MutableList<AdminNotification>,
    private val onMarkReadClick: (AdminNotification) -> Unit
) : RecyclerView.Adapter<AdminNotificationAdapter.AdminNotificationViewHolder>(){
    inner class AdminNotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardAdminNotification: MaterialCardView = itemView.findViewById(R.id.cardAdminNotification)
        val tvNotificationType: TextView = itemView.findViewById(R.id.tvNotificationType)
        val tvNotificationTitle: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        val tvNotificationMessage: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        val tvNotificationMeta: TextView = itemView.findViewById(R.id.tvNotificationMeta)
        val tvNotificationTime: TextView = itemView.findViewById(R.id.tvNotificationTime)
        val tvReadStatus: TextView = itemView.findViewById(R.id.tvReadStatus)
        val btnMarkRead: MaterialButton = itemView.findViewById(R.id.btnMarkRead)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminNotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_notification, parent, false)
        return AdminNotificationViewHolder(view)
    }

    override fun getItemCount(): Int = notifications.size

    override fun onBindViewHolder(holder: AdminNotificationViewHolder, position: Int) {
        val notification = notifications[position]

        holder.tvNotificationType.text = formatType(notification.type)
        holder.tvNotificationTitle.text = notification.title.ifEmpty { "Admin Notification" }
        holder.tvNotificationMessage.text = notification.message
        holder.tvNotificationTime.text = formatTime(notification.timestamp)

        val metaParts = mutableListOf<String>()
        if (notification.userName.isNotEmpty()) metaParts.add(notification.userName)
        if (notification.userRole.isNotEmpty()) metaParts.add(notification.userRole.replaceFirstChar { it.uppercase() })
        if (notification.relatedId.isNotEmpty()) metaParts.add("ID: ${notification.relatedId}")
        holder.tvNotificationMeta.text = if (metaParts.isEmpty()) "System event" else metaParts.joinToString("  •  ")

        if (notification.isRead) {
            holder.tvReadStatus.text = "Read"
            holder.tvReadStatus.setTextColor(0xFF6B7280.toInt())
            holder.btnMarkRead.visibility = View.GONE
            holder.cardAdminNotification.strokeColor = 0xFFE5E7EB.toInt()
            holder.cardAdminNotification.strokeWidth = 1
        } else {
            holder.tvReadStatus.text = "Unread"
            holder.tvReadStatus.setTextColor(0xFF17C964.toInt())
            holder.btnMarkRead.visibility = View.VISIBLE
            holder.cardAdminNotification.strokeColor = 0xFF17C964.toInt()
            holder.cardAdminNotification.strokeWidth = 2
        }

        holder.btnMarkRead.setOnClickListener {
            onMarkReadClick(notification)
        }
    }

    private fun formatType(type: String): String {
        return when (type) {
            AdminNotificationHelper.TYPE_NEW_USER -> "NEW USER"
            AdminNotificationHelper.TYPE_NEW_POST -> "NEW POST"
            AdminNotificationHelper.TYPE_MEETING_SET -> "MEETING"
            AdminNotificationHelper.TYPE_REVIEW_GIVEN -> "REVIEW"
            AdminNotificationHelper.TYPE_REPORT_GIVEN -> "REPORT"
            else -> "NOTIFICATION"
        }
    }

    private fun formatTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

}