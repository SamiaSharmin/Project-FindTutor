package com.example.project_findtutor

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminReportsAdapter(private val onReportStatusChangeClick: (ProblemReport, String) -> Unit,
                          private val onReviewStatusChangeClick: (Review, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<AdminSupportItem>()

    fun submitList(newItems: List<AdminSupportItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AdminSupportItem.ReportItem -> VIEW_TYPE_REPORT
            is AdminSupportItem.ReviewItem -> VIEW_TYPE_REVIEW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_REPORT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_admin_report, parent, false)

            ReportViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_admin_review, parent, false)

            ReviewViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AdminSupportItem.ReportItem -> {
                (holder as ReportViewHolder).bind(
                    report = item.report,
                    onReportStatusChangeClick = onReportStatusChangeClick
                )
            }

            is AdminSupportItem.ReviewItem -> {
                (holder as ReviewViewHolder).bind(
                    review = item.review,
                    moderationStatus = item.moderationStatus,
                    onReviewStatusChangeClick = onReviewStatusChangeClick
                )
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imgReportUserAvatar: ImageView =
            itemView.findViewById(R.id.imgReportUserAvatar)

        private val tvReportUserRole: TextView =
            itemView.findViewById(R.id.tvReportUserRole)

        private val tvReportUserName: TextView =
            itemView.findViewById(R.id.tvReportUserName)

        private val tvReportUserContact: TextView =
            itemView.findViewById(R.id.tvReportUserContact)

        private val tvReportStatus: TextView =
            itemView.findViewById(R.id.tvReportStatus)

        private val btnMoreReportOptions: TextView =
            itemView.findViewById(R.id.btnMoreReportOptions)

        private val tvReportDescription: TextView =
            itemView.findViewById(R.id.tvReportDescription)

        private val tvReportCreatedAt: TextView =
            itemView.findViewById(R.id.tvReportCreatedAt)

        private val btnDismissReport: MaterialButton =
            itemView.findViewById(R.id.btnDismissReport)

        private val btnInvestigateReport: MaterialButton =
            itemView.findViewById(R.id.btnInvestigateReport)

        fun bind(
            report: ProblemReport,
            onReportStatusChangeClick: (ProblemReport, String) -> Unit
        ) {
            imgReportUserAvatar.setImageResource(R.drawable.ic_admin_profile)

            tvReportUserRole.text = displayReporterRole(report.userRole)
            tvReportUserName.text = report.userName.ifBlank { "Unknown User" }
            tvReportUserContact.text = buildContactText(report)
            tvReportDescription.text = report.description.ifBlank { "No description provided" }

            val normalizedStatus = normalizeStatus(report.status)
            tvReportStatus.text = displayStatus(normalizedStatus)
            styleStatusBadge(tvReportStatus, normalizedStatus)

            tvReportCreatedAt.text = formatCreatedAt(report.createdAt)

            btnDismissReport.setOnClickListener {
                onReportStatusChangeClick(report, "dismissed")
            }

            btnInvestigateReport.setOnClickListener {
                onReportStatusChangeClick(report, "in_review")
            }

            btnMoreReportOptions.setOnClickListener {
                showReportMenu(report, it, onReportStatusChangeClick)
            }
        }

        private fun showReportMenu(
            report: ProblemReport,
            anchor: View,
            onReportStatusChangeClick: (ProblemReport, String) -> Unit
        ) {
            val popup = PopupMenu(anchor.context, anchor)

            popup.menu.add("Mark Pending")
            popup.menu.add("Mark In Review")
            popup.menu.add("Mark Resolved")
            popup.menu.add("Dismiss Report")

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title.toString()) {
                    "Mark Pending" -> onReportStatusChangeClick(report, "pending")
                    "Mark In Review" -> onReportStatusChangeClick(report, "in_review")
                    "Mark Resolved" -> onReportStatusChangeClick(report, "resolved")
                    "Dismiss Report" -> onReportStatusChangeClick(report, "dismissed")
                }
                true
            }

            popup.show()
        }

        private fun displayReporterRole(role: String): String {
            return when (role.trim().lowercase(Locale.ROOT)) {
                "student" -> "Reporter: Student"
                "tutor" -> "Reporter: Tutor"
                else -> "Reporter"
            }
        }

        private fun buildContactText(report: ProblemReport): String {
            val parts = mutableListOf<String>()

            if (report.userEmail.isNotBlank()) {
                parts.add(report.userEmail)
            }

            if (report.userPhoneNumber.isNotBlank()) {
                parts.add(report.userPhoneNumber)
            }

            return if (parts.isEmpty()) {
                "No contact information"
            } else {
                parts.joinToString("  |  ")
            }
        }
    }

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imgReviewUserAvatar: ImageView =
            itemView.findViewById(R.id.imgReviewUserAvatar)

        private val tvReviewerRole: TextView =
            itemView.findViewById(R.id.tvReviewerRole)

        private val tvReviewUserName: TextView =
            itemView.findViewById(R.id.tvReviewUserName)

        private val tvReviewUserContact: TextView =
            itemView.findViewById(R.id.tvReviewUserContact)

        private val tvReviewStatus: TextView =
            itemView.findViewById(R.id.tvReviewStatus)

        private val btnMoreReviewOptions: TextView =
            itemView.findViewById(R.id.btnMoreReviewOptions)

        private val tvReviewItemTutor: TextView =
            itemView.findViewById(R.id.tvReviewItemTutor)

        private val tvReviewDescription: TextView =
            itemView.findViewById(R.id.tvReviewDescription)

        private val tvReviewCreatedAt: TextView =
            itemView.findViewById(R.id.tvReviewCreatedAt)

        private val btnDismissReview: MaterialButton =
            itemView.findViewById(R.id.btnDismissReview)

        private val btnInvestigateReview: MaterialButton =
            itemView.findViewById(R.id.btnInvestigateReview)

        fun bind(
            review: Review,
            moderationStatus: String,
            onReviewStatusChangeClick: (Review, String) -> Unit
        ) {
            imgReviewUserAvatar.setImageResource(R.drawable.ic_admin_profile)

            tvReviewerRole.text = "Reviewer: Student"
            tvReviewUserName.text = review.studentName.ifBlank { "Unknown Student" }

            tvReviewUserContact.text = if (review.studentId.isNotBlank()) {
                "Student ID: ${review.studentId}"
            } else {
                "Student ID unavailable"
            }

            tvReviewItemTutor.text = if (review.tutorId.isNotBlank()) {
                "Tutor ID: ${review.tutorId}"
            } else {
                "Tutor ID unavailable"
            }

            tvReviewDescription.text = review.reviewText.ifBlank { "No review text provided" }

            val normalizedStatus = normalizeStatus(moderationStatus)
            tvReviewStatus.text = displayStatus(normalizedStatus)
            styleStatusBadge(tvReviewStatus, normalizedStatus)

            tvReviewCreatedAt.text = formatTimestamp(review.timestamp)

            btnDismissReview.setOnClickListener {
                onReviewStatusChangeClick(review, "dismissed")
            }

            btnInvestigateReview.setOnClickListener {
                onReviewStatusChangeClick(review, "in_review")
            }

            btnMoreReviewOptions.setOnClickListener {
                showReviewMenu(review, it, onReviewStatusChangeClick)
            }
        }

        private fun showReviewMenu(
            review: Review,
            anchor: View,
            onReviewStatusChangeClick: (Review, String) -> Unit
        ) {
            val popup = PopupMenu(anchor.context, anchor)

            popup.menu.add("Mark Pending")
            popup.menu.add("Mark In Review")
            popup.menu.add("Mark Resolved")
            popup.menu.add("Dismiss Review")

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title.toString()) {
                    "Mark Pending" -> onReviewStatusChangeClick(review, "pending")
                    "Mark In Review" -> onReviewStatusChangeClick(review, "in_review")
                    "Mark Resolved" -> onReviewStatusChangeClick(review, "resolved")
                    "Dismiss Review" -> onReviewStatusChangeClick(review, "dismissed")
                }
                true
            }

            popup.show()
        }
    }

    companion object {
        private const val VIEW_TYPE_REPORT = 1
        private const val VIEW_TYPE_REVIEW = 2

        private fun normalizeStatus(status: String): String {
            return when (status.trim().lowercase(Locale.ROOT)) {
                "", "pending", "new" -> "pending"
                "in_review", "in review", "reviewing", "investigating" -> "in_review"
                "resolved", "solved", "complete", "completed" -> "resolved"
                "dismissed", "rejected", "closed" -> "dismissed"
                else -> status.trim().lowercase(Locale.ROOT)
            }
        }

        private fun displayStatus(status: String): String {
            return when (status) {
                "pending" -> "Pending"
                "in_review" -> "In Review"
                "resolved" -> "Resolved"
                "dismissed" -> "Dismissed"
                else -> status.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }
            }
        }

        private fun styleStatusBadge(textView: TextView, status: String) {
            val textColor: String
            val bgColor: String

            when (status) {
                "pending" -> {
                    textColor = "#92400E"
                    bgColor = "#FEF3C7"
                }

                "in_review" -> {
                    textColor = "#1D4ED8"
                    bgColor = "#DBEAFE"
                }

                "resolved" -> {
                    textColor = "#166534"
                    bgColor = "#DCFCE7"
                }

                "dismissed" -> {
                    textColor = "#374151"
                    bgColor = "#E5E7EB"
                }

                else -> {
                    textColor = "#374151"
                    bgColor = "#E5E7EB"
                }
            }

            textView.setTextColor(Color.parseColor(textColor))
            textView.background = roundedBackground(textView, bgColor, 11)
        }

        private fun roundedBackground(view: View, color: String, radiusDp: Int): GradientDrawable {
            val density = view.resources.displayMetrics.density

            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor(color))
                cornerRadius = radiusDp * density
            }
        }

        private fun formatCreatedAt(value: Any?): String {
            val timestamp = when (value) {
                is Long -> value
                is Int -> value.toLong()
                is Double -> value.toLong()
                is Float -> value.toLong()
                is String -> value.toLongOrNull()
                else -> null
            } ?: return "Date unavailable"

            return formatTimestamp(timestamp)
        }

        private fun formatTimestamp(timestamp: Long): String {
            if (timestamp <= 0L) {
                return "Date unavailable"
            }

            val millis = if (timestamp < 10_000_000_000L) {
                timestamp * 1000
            } else {
                timestamp
            }

            return SimpleDateFormat("MMM dd, hh:mm a", Locale.US).format(Date(millis))
        }
    }
}