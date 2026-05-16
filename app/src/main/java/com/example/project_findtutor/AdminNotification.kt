package com.example.project_findtutor

data class AdminNotification(
    var notificationId: String = "",
    var title: String = "",
    var message: String = "",
    var type: String = "",
    var userId: String = "",
    var userRole: String = "",
    var userName: String = "",
    var relatedId: String = "",
    var relatedNode: String = "",
    var timestamp: Long = 0L,
    var isRead: Boolean = false

)
