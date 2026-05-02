package com.example.project_findtutor

data class Post(
    var postId: String="",
    val jobId: Int = 0,
    val userId: String = "",
    val title: String = "",
    val location: String = "",
    val studentClass: String = "",
    val time: String = "",
    val subjects: String = "",
    val salary: Int = 0,
    val days: Int = 0,
    val studentGender: String = "",
    val tutorGender: String = "",
    val description: String = "",
    val postedDate: String =""
)
