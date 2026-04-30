package com.example.project_findtutor

data class NotificationModel(
    val jobId: Int = 0,
    val tutorId: String = "",
    val tutorName: String = "",
    var message: String = "",
    val type: String = "",
    val timestamp: Long = 0L,
    val isRead:Boolean = false
)
