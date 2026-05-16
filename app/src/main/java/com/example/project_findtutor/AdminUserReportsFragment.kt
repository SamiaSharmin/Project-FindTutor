package com.example.project_findtutor

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class AdminUserReportsFragment : Fragment(R.layout.fragment_admin_user_reports) {


    private val reportsNode = "ProblemReports"

    private lateinit var db: DatabaseReference

    private lateinit var tvReportsTotal: TextView
    private lateinit var tvRefreshReports: TextView
    private lateinit var etSearchReports: EditText
    private lateinit var tvReportResultCount: TextView

    private lateinit var rvAdminReports: RecyclerView
    private lateinit var emptyReportsLayout: LinearLayout
    private lateinit var progressReports: ProgressBar
    private lateinit var tvEmptyReports: TextView

    private lateinit var chipAllReports: MaterialCardView
    private lateinit var chipPendingReports: MaterialCardView
    private lateinit var chipInReviewReports: MaterialCardView
    private lateinit var chipResolvedReports: MaterialCardView
    private lateinit var chipDismissedReports: MaterialCardView

    private lateinit var tvChipAllReports: TextView
    private lateinit var tvChipPendingReports: TextView
    private lateinit var tvChipInReviewReports: TextView
    private lateinit var tvChipResolvedReports: TextView
    private lateinit var tvChipDismissedReports: TextView

    private lateinit var adapter: AdminReportsAdapter

    private val allReports = mutableListOf<ProblemReport>()
    private val allReviews = mutableListOf<Review>()
    private val reviewStatusMap = mutableMapOf<String, String>()
    private val allItems = mutableListOf<AdminSupportItem>()

    private var selectedFilter = ReportFilter.ALL
    private var searchQuery = ""
    private var reportsListener: ValueEventListener? = null
    private var reviewsListener: ValueEventListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseDatabase.getInstance().reference

        requireActivity()
            .findViewById<TextView>(R.id.tvAdminDashboardTitle)
            .text = "Reports"

        tvReportsTotal = view.findViewById(R.id.tvReportsTotal)
        tvRefreshReports = view.findViewById(R.id.tvRefreshReports)
        etSearchReports = view.findViewById(R.id.etSearchReports)
        tvReportResultCount = view.findViewById(R.id.tvReportResultCount)

        rvAdminReports = view.findViewById(R.id.rvAdminReports)
        emptyReportsLayout = view.findViewById(R.id.emptyReportsLayout)
        progressReports = view.findViewById(R.id.progressReports)
        tvEmptyReports = view.findViewById(R.id.tvEmptyReports)

        chipAllReports = view.findViewById(R.id.chipAllReports)
        chipPendingReports = view.findViewById(R.id.chipPendingReports)
        chipInReviewReports = view.findViewById(R.id.chipInReviewReports)
        chipResolvedReports = view.findViewById(R.id.chipResolvedReports)
        chipDismissedReports = view.findViewById(R.id.chipDismissedReports)

        tvChipAllReports = view.findViewById(R.id.tvChipAllReports)
        tvChipPendingReports = view.findViewById(R.id.tvChipPendingReports)
        tvChipInReviewReports = view.findViewById(R.id.tvChipInReviewReports)
        tvChipResolvedReports = view.findViewById(R.id.tvChipResolvedReports)
        tvChipDismissedReports = view.findViewById(R.id.tvChipDismissedReports)
        setupRecyclerView()
        setupSearch()
        setupFilterChips()
        setupRefreshButton()

        loadReviews()
        loadReports()
    }

    private fun setupRecyclerView() {
        adapter = AdminReportsAdapter(
            onReportStatusChangeClick = { report, newStatus ->
                updateReportStatus(report, newStatus)
            },
            onReviewStatusChangeClick = { review, newStatus ->
                updateReviewStatus(review, newStatus)
            }
        )

        rvAdminReports.layoutManager = LinearLayoutManager(requireContext())
        rvAdminReports.adapter = adapter
    }

    private fun setupSearch() {
        etSearchReports.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim().orEmpty()
                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilterChips() {
        chipAllReports.setOnClickListener {
            selectedFilter = ReportFilter.ALL
            updateChipUi()
            applyFilters()
        }

        chipPendingReports.setOnClickListener {
            selectedFilter = ReportFilter.PENDING
            updateChipUi()
            applyFilters()
        }

        chipInReviewReports.setOnClickListener {
            selectedFilter = ReportFilter.IN_REVIEW
            updateChipUi()
            applyFilters()
        }

        chipResolvedReports.setOnClickListener {
            selectedFilter = ReportFilter.RESOLVED
            updateChipUi()
            applyFilters()
        }

        chipDismissedReports.setOnClickListener {
            selectedFilter = ReportFilter.DISMISSED
            updateChipUi()
            applyFilters()
        }

        updateChipUi()
    }

    private fun setupRefreshButton() {
        tvRefreshReports.setOnClickListener {
            loadReviews()
            loadReports()
        }
    }

    private fun loadReports() {
        showLoadingState()

        reportsListener?.let {
            db.child(reportsNode).removeEventListener(it)
        }

        reportsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allReports.clear()

                for (reportSnapshot in snapshot.children) {
                    allReports.add(parseReport(reportSnapshot))
                }
                rebuildSupportItems()
            }

            override fun onCancelled(error: DatabaseError) {
                progressReports.visibility = View.GONE
                tvEmptyReports.text = "Failed to load reports"

                Toast.makeText(
                    requireContext(),
                    "Database error: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        db.child(reportsNode).addValueEventListener(reportsListener as ValueEventListener)
    }

    private fun loadReviews() {
        reviewsListener?.let {
            db.child("Reviews").removeEventListener(it)
        }

        reviewsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allReviews.clear()
                reviewStatusMap.clear()

                for (reviewSnapshot in snapshot.children) {
                    val review = parseReview(reviewSnapshot)

                    if (review.reviewText.isNotBlank()) {
                        allReviews.add(review)

                        val moderationStatus = firstNonBlank(
                            reviewSnapshot.getStringValue("moderationStatus"),
                            reviewSnapshot.getStringValue("status"),
                            "pending"
                        )

                        reviewStatusMap[review.reviewId] = normalizeStatus(moderationStatus)
                    }
                }

                rebuildSupportItems()

                allReviews.forEach { review ->
                    if (review.studentName.isBlank()) {
                        fetchStudentNameForReview(
                            studentId = review.studentId,
                            reviewId = review.reviewId
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load reviews: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        db.child("Reviews").addValueEventListener(reviewsListener as ValueEventListener)
    }

    private fun rebuildSupportItems() {
        allItems.clear()

        val reportItems = allReports.map { report ->
            AdminSupportItem.ReportItem(report)
        }

        val reviewItems = allReviews.map { review ->
            AdminSupportItem.ReviewItem(
                review = review,
                moderationStatus = reviewStatusMap[review.reviewId] ?: "pending"
            )
        }

        allItems.addAll(reportItems)
        allItems.addAll(reviewItems)

        allItems.sortByDescending { item ->
            when (item) {
                is AdminSupportItem.ReportItem -> createdAtToLong(item.report.createdAt)
                is AdminSupportItem.ReviewItem -> item.review.timestamp
            }
        }

        updateChipLabels()
        applyFilters()
    }

    private fun parseReport(snapshot: DataSnapshot): ProblemReport {
        return ProblemReport(
            reportId = firstNonBlank(
                snapshot.getStringValue("reportId"),
                snapshot.key.orEmpty()
            ),
            userId = snapshot.getStringValue("userId"),
            userRole = snapshot.getStringValue("userRole"),
            userName = snapshot.getStringValue("userName"),
            userEmail = snapshot.getStringValue("userEmail"),
            userPhoneNumber = firstNonBlank(
                snapshot.getStringValue("userPhoneNumber"),
                snapshot.getStringValue("phoneNumber"),
                snapshot.getStringValue("phone")
            ),
            description = snapshot.getStringValue("description"),
            status = normalizeStatus(snapshot.getStringValue("status")),
            createdAt = snapshot.child("createdAt").value
        )
    }

    private fun parseReview(snapshot: DataSnapshot): Review {
        return Review(
            reviewId = snapshot.key.orEmpty(),
            studentId = snapshot.getStringValue("studentId"),
            studentName = snapshot.getStringValue("studentName"),
            tutorId = snapshot.getStringValue("tutorId"),
            meetingId = snapshot.getStringValue("meetingId"),
            rating = snapshot.getFloatValue("rating"),
            reviewText = firstNonBlank(
                snapshot.getStringValue("reviewText"),
                snapshot.getStringValue("comment")
            ),
            timestamp = snapshot.getLongValue("timestamp")
        )
    }

    private fun fetchStudentNameForReview(studentId: String, reviewId: String) {
        if (studentId.isBlank() || reviewId.isBlank()) return

        db.child("Students").child(studentId).get()
            .addOnSuccessListener { studentSnapshot ->
                val directName = studentSnapshot.getStringValue("name")

                if (directName.isNotBlank()) {
                    updateReviewStudentName(reviewId, directName)
                } else {
                    fetchStudentNameByUserId(studentId, reviewId)
                }
            }
            .addOnFailureListener {
                fetchStudentNameByUserId(studentId, reviewId)
            }
    }

    private fun fetchStudentNameByUserId(userId: String, reviewId: String) {
        db.child("Students")
            .orderByChild("userId")
            .equalTo(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                for (studentSnapshot in snapshot.children) {
                    val studentName = studentSnapshot.getStringValue("name")

                    if (studentName.isNotBlank()) {
                        updateReviewStudentName(reviewId, studentName)
                        return@addOnSuccessListener
                    }
                }

                fetchStudentNameFromUsers(userId, reviewId)
            }
            .addOnFailureListener {
                fetchStudentNameFromUsers(userId, reviewId)
            }
    }

    private fun fetchStudentNameFromUsers(userId: String, reviewId: String) {
        db.child("Users").child(userId).get()
            .addOnSuccessListener { userSnapshot ->
                val userName = userSnapshot.getStringValue("name")

                if (userName.isNotBlank()) {
                    updateReviewStudentName(reviewId, userName)
                }
            }
    }

    private fun updateReviewStudentName(reviewId: String, studentName: String) {
        val review = allReviews.firstOrNull { it.reviewId == reviewId } ?: return

        if (review.studentName == studentName) return

        review.studentName = studentName

        rebuildSupportItems()
    }

    private fun applyFilters() {
        val query = searchQuery.lowercase(Locale.ROOT)

        val filteredItems = allItems.filter { item ->
            matchesSelectedFilter(item) && matchesSearchQuery(item, query)
        }

        adapter.submitList(filteredItems)

        progressReports.visibility = View.GONE

        tvReportsTotal.text = "${formatNumber(allItems.size)} Total"
        tvReportResultCount.text =
            "Showing ${formatNumber(filteredItems.size)} of ${formatNumber(allItems.size)} results"

        if (filteredItems.isEmpty()) {
            emptyReportsLayout.visibility = View.VISIBLE
            tvEmptyReports.text = if (allItems.isEmpty()) {
                "No reports or reviews found"
            } else {
                "No matching records found"
            }
        } else {
            emptyReportsLayout.visibility = View.GONE
        }
    }

    private fun matchesSelectedFilter(item: AdminSupportItem): Boolean {
        val itemStatus = when (item) {
            is AdminSupportItem.ReportItem -> normalizeStatus(item.report.status)
            is AdminSupportItem.ReviewItem -> normalizeStatus(item.moderationStatus)
        }

        return when (selectedFilter) {
            ReportFilter.ALL -> true
            ReportFilter.PENDING -> itemStatus == "pending"
            ReportFilter.IN_REVIEW -> itemStatus == "in_review"
            ReportFilter.RESOLVED -> itemStatus == "resolved"
            ReportFilter.DISMISSED -> itemStatus == "dismissed"
        }
    }

    private fun matchesSearchQuery(item: AdminSupportItem, query: String): Boolean {
        if (query.isBlank()) return true

        return when (item) {
            is AdminSupportItem.ReportItem -> {
                val report = item.report

                report.userName.lowercase(Locale.ROOT).contains(query) ||
                        report.userEmail.lowercase(Locale.ROOT).contains(query) ||
                        report.userPhoneNumber.lowercase(Locale.ROOT).contains(query) ||
                        report.userRole.lowercase(Locale.ROOT).contains(query) ||
                        report.description.lowercase(Locale.ROOT).contains(query) ||
                        report.status.lowercase(Locale.ROOT).contains(query)
            }

            is AdminSupportItem.ReviewItem -> {
                val review = item.review

                review.studentName.lowercase(Locale.ROOT).contains(query) ||
                        review.studentId.lowercase(Locale.ROOT).contains(query) ||
                        review.tutorId.lowercase(Locale.ROOT).contains(query) ||
                        review.meetingId.lowercase(Locale.ROOT).contains(query) ||
                        review.reviewText.lowercase(Locale.ROOT).contains(query) ||
                        item.moderationStatus.lowercase(Locale.ROOT).contains(query)
            }
        }
    }

    private fun updateReportStatus(report: ProblemReport, newStatus: String) {
        val normalizedStatus = normalizeStatus(newStatus)
        val oldStatus = normalizeStatus(report.status)

        if (report.reportId.isBlank()) {
            Toast.makeText(requireContext(), "Report ID missing", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = hashMapOf<String, Any?>()

        updates["$reportsNode/${report.reportId}/status"] = normalizedStatus

        val auditId = db.child("AuditLogs").push().key
        if (auditId != null) {
            updates["AuditLogs/$auditId"] = mapOf(
                "action" to "REPORT_STATUS_CHANGED",
                "reportId" to report.reportId,
                "reporterUserId" to report.userId,
                "reporterRole" to report.userRole,
                "oldStatus" to oldStatus,
                "newStatus" to normalizedStatus,
                "timestamp" to ServerValue.TIMESTAMP
            )
        }

        db.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Report marked as ${displayStatus(normalizedStatus)}",
                    Toast.LENGTH_SHORT
                ).show()

                if (normalizedStatus == "in_review" && oldStatus != "in_review") {
                    sendReportInvestigationNotification(report)
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Failed to update report",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateReviewStatus(review: Review, newStatus: String) {
        val normalizedStatus = normalizeStatus(newStatus)
        val oldStatus = normalizeStatus(reviewStatusMap[review.reviewId] ?: "pending")

        if (review.reviewId.isBlank()) {
            Toast.makeText(requireContext(), "Review ID missing", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = hashMapOf<String, Any?>()

        updates["Reviews/${review.reviewId}/moderationStatus"] = normalizedStatus

        val auditId = db.child("AuditLogs").push().key
        if (auditId != null) {
            updates["AuditLogs/$auditId"] = mapOf(
                "action" to "REVIEW_MODERATION_STATUS_CHANGED",
                "reviewId" to review.reviewId,
                "studentId" to review.studentId,
                "tutorId" to review.tutorId,
                "oldStatus" to oldStatus,
                "newStatus" to normalizedStatus,
                "timestamp" to ServerValue.TIMESTAMP
            )
        }

        db.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Review marked as ${displayStatus(normalizedStatus)}",
                    Toast.LENGTH_SHORT
                ).show()

                if (normalizedStatus == "in_review" && oldStatus != "in_review") {
                    sendReviewInvestigationNotification(review)
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Failed to update review",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun sendReportInvestigationNotification(report: ProblemReport) {
        val targetUserId = report.userId.trim()

        if (targetUserId.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Cannot send notification: user ID missing",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val notification = NotificationModel(
            jobId = 0,
            tutorId = "",
            tutorName = "",
            message = "Your report is now under investigation by the admin team.",
            type = "report_investigation_started",
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        db.child("Notifications")
            .child(targetUserId)
            .push()
            .setValue(notification)
    }

    private fun sendReviewInvestigationNotification(review: Review) {
        resolveStudentUserIdForReview(review) { targetUserId ->
            if (targetUserId.isBlank()) {
                Toast.makeText(
                    requireContext(),
                    "Cannot send notification: student user ID missing",
                    Toast.LENGTH_SHORT
                ).show()
                return@resolveStudentUserIdForReview
            }

            val notification = NotificationModel(
                jobId = 0,
                tutorId = review.tutorId,
                tutorName = "",
                message = "Your review is now under investigation by the admin team.",
                type = "review_investigation_started",
                timestamp = System.currentTimeMillis(),
                isRead = false
            )

            db.child("Notifications").child(targetUserId).push().setValue(notification)
        }
    }

    private fun resolveStudentUserIdForReview(
        review: Review,
        onResult: (String) -> Unit
    ) {
        val studentId = review.studentId.trim()

        if (studentId.isBlank()) {
            onResult("")
            return
        }

        db.child("Students").child(studentId).get()
            .addOnSuccessListener { studentSnapshot ->
                val userIdFromStudent = studentSnapshot.getStringValue("userId")

                if (userIdFromStudent.isNotBlank()) {
                    onResult(userIdFromStudent)
                } else {
                    checkIfStudentIdIsUserId(studentId, onResult)
                }
            }
            .addOnFailureListener {
                checkIfStudentIdIsUserId(studentId, onResult)
            }
    }

    private fun checkIfStudentIdIsUserId(
        studentId: String,
        onResult: (String) -> Unit
    ) {
        db.child("Users").child(studentId).get()
            .addOnSuccessListener { userSnapshot ->
                if (userSnapshot.exists()) {
                    onResult(studentId)
                } else {
                    findStudentUserIdByProfileUserId(studentId, onResult)
                }
            }
            .addOnFailureListener {
                findStudentUserIdByProfileUserId(studentId, onResult)
            }
    }

    private fun findStudentUserIdByProfileUserId(
        possibleUserId: String,
        onResult: (String) -> Unit
    ) {
        db.child("Students")
            .orderByChild("userId")
            .equalTo(possibleUserId)
            .get()
            .addOnSuccessListener { snapshot ->
                for (studentSnapshot in snapshot.children) {
                    val userId = studentSnapshot.getStringValue("userId")
                    if (userId.isNotBlank()) {
                        onResult(userId)
                        return@addOnSuccessListener
                    }
                }

                onResult("")
            }
            .addOnFailureListener {
                onResult("")
            }
    }


    private fun updateChipLabels() {
        tvChipAllReports.text = "All (${formatNumber(allItems.size)})"
        tvChipPendingReports.text =
            "Pending (${formatNumber(countItemsByStatus("pending"))})"
        tvChipInReviewReports.text =
            "In Review (${formatNumber(countItemsByStatus("in_review"))})"
        tvChipResolvedReports.text =
            "Resolved (${formatNumber(countItemsByStatus("resolved"))})"
        tvChipDismissedReports.text =
            "Dismissed (${formatNumber(countItemsByStatus("dismissed"))})"
    }

    private fun countItemsByStatus(status: String): Int {
        return allItems.count { item ->
            when (item) {
                is AdminSupportItem.ReportItem -> normalizeStatus(item.report.status) == status
                is AdminSupportItem.ReviewItem -> normalizeStatus(item.moderationStatus) == status
            }
        }
    }

    private fun updateChipUi() {
        setChipState(chipAllReports, tvChipAllReports, selectedFilter == ReportFilter.ALL)
        setChipState(chipPendingReports, tvChipPendingReports, selectedFilter == ReportFilter.PENDING)
        setChipState(chipInReviewReports, tvChipInReviewReports, selectedFilter == ReportFilter.IN_REVIEW)
        setChipState(chipResolvedReports, tvChipResolvedReports, selectedFilter == ReportFilter.RESOLVED)
        setChipState(chipDismissedReports, tvChipDismissedReports, selectedFilter == ReportFilter.DISMISSED)
    }

    private fun setChipState(card: MaterialCardView, textView: TextView, selected: Boolean) {
        if (selected) {
            card.setCardBackgroundColor(Color.parseColor("#17C964"))
            card.strokeWidth = 0
            textView.setTextColor(Color.WHITE)
        } else {
            card.setCardBackgroundColor(Color.WHITE)
            card.strokeWidth = dp(1)
            card.strokeColor = Color.parseColor("#D7DCE3")
            textView.setTextColor(Color.parseColor("#111827"))
        }
    }

    private fun showLoadingState() {
        emptyReportsLayout.visibility = View.VISIBLE
        progressReports.visibility = View.VISIBLE
        tvEmptyReports.text = "Loading reports and reviews"
    }

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

    private fun createdAtToLong(value: Any?): Long {
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            is Float -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    private fun firstNonBlank(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun DataSnapshot.getStringValue(key: String): String {
        return child(key).value?.toString()?.trim().orEmpty()
    }

    private fun DataSnapshot.getLongValue(key: String): Long {
        val value = child(key).value ?: return 0L

        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            is Float -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    private fun DataSnapshot.getFloatValue(key: String): Float {
        val value = child(key).value ?: return 0f

        return when (value) {
            is Float -> value
            is Double -> value.toFloat()
            is Long -> value.toFloat()
            is Int -> value.toFloat()
            is String -> value.toFloatOrNull() ?: 0f
            else -> 0f
        }
    }

    private fun formatNumber(value: Int): String {
        return NumberFormat.getIntegerInstance(Locale.US).format(value)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        reportsListener?.let {
            db.child(reportsNode).removeEventListener(it)
        }

        reviewsListener?.let {
            db.child("Reviews").removeEventListener(it)
        }

        reportsListener = null
        reviewsListener = null

        super.onDestroyView()
    }

    private enum class ReportFilter {
        ALL,
        PENDING,
        IN_REVIEW,
        RESOLVED,
        DISMISSED
    }

}