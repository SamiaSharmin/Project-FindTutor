package com.example.project_findtutor

data class Tutor(
    var userId: String,
    var name: String, val email: String,
    var phoneNumber: String,
    var qualification: String,
    var preferedAreas: String = "",
    var rating: Float = 0f,
    var totalReview: Int = 0
)
