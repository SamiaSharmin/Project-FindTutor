package com.example.project_findtutor

data class Student(
    val userId: String,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val rating: Double = 0.0
)
