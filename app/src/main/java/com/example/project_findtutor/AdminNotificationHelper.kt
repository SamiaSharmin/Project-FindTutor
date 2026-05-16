package com.example.project_findtutor

import com.google.firebase.database.DatabaseReference

object AdminNotificationHelper {
    const val NODE_ADMIN_NOTIFICATIONS = "AdminNotifications"

    const val TYPE_NEW_USER = "new_user"
    const val TYPE_NEW_POST = "new_post"
    const val TYPE_MEETING_SET = "meeting_set"
    const val TYPE_REVIEW_GIVEN = "review_given"
    const val TYPE_REPORT_GIVEN = "report_given"

    fun sendAdminNotification(
        db: DatabaseReference,
        title: String,
        message: String,
        type: String,
        userId: String = "",
        userRole: String = "",
        userName: String = "",
        relatedId: String = "",
        relatedNode: String = ""
    ) {
        val notificationRef = db.child(NODE_ADMIN_NOTIFICATIONS).push()
        val notificationId = notificationRef.key ?: return

        val notification = AdminNotification(
            notificationId = notificationId,
            title = title,
            message = message,
            type = type,
            userId = userId,
            userRole = userRole,
            userName = userName,
            relatedId = relatedId,
            relatedNode = relatedNode,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        notificationRef.setValue(notification)
    }

}