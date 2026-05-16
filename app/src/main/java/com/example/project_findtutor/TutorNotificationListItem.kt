package com.example.project_findtutor

sealed class TutorNotificationListItem {
    data class MeetingItem(
        val meeting: Meeting
    ) : TutorNotificationListItem()

    data class NotificationItem(
        val notification: NotificationModel
    ) : TutorNotificationListItem()
}