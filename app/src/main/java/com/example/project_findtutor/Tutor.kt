package com.example.project_findtutor

data class Tutor(
    val userId: String,
    val name: String, val email: String,
    val phoneNumber: String,
    val qualification: String,
    val preferedAreas: String = "",
    val rating: Double
)
