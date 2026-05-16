package com.example.project_findtutor

data class ProblemReport(
    var reportId: String = "",
    var userId: String = "",
    var userRole: String = "",
    var userName: String = "",
    var userEmail: String = "",
    var userPhoneNumber: String = "",
    var description: String = "",
    var status: String = "pending",
    var createdAt: Any? = null
)
