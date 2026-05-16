package com.example.project_findtutor

data class AdminPostItem(
    val post: Post,
    val studentName: String = "Unknown Student",
    val status: String = "open"
)