package com.example.project_findtutor

data class Meeting(
    var meetingId: String = "",
    var jobId: Int = 0,
    var studentId: String = "",
    var tutorId: String="",
    var date: String = "",
    var time: String = "",
    var location: String = "",
    var status: String = "pending",
    var createdAt:Long = 0
)
