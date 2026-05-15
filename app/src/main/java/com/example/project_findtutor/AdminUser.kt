package com.example.project_findtutor

data class AdminUser(
    val userId: String = "",
    val profileId: String = "",
    val role: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val area: String = "",
    val rating: Float = 0f,
    val status: String = "active",
    val verificationStatus: String = "verified",
    val registeredAt: String = "Joined date unavailable"
)
