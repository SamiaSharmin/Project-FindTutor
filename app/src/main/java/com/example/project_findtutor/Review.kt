package com.example.project_findtutor

data class Review(
    var reviewId: String ="",
    var studentId: String = "",
    var studentName: String = "",
    var tutorId: String = "",
    var meetingId: String = "",
    var rating: Float = 0f,
    var reviewText: String = "",
    var timestamp: Long = 0L
)
