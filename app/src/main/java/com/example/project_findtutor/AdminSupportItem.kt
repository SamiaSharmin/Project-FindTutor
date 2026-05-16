package com.example.project_findtutor

sealed class AdminSupportItem {
    data class ReportItem(
        val report: ProblemReport
    ) : AdminSupportItem()

    data class ReviewItem(
        val review: Review,
        val moderationStatus: String = "pending"
    ) : AdminSupportItem()
}