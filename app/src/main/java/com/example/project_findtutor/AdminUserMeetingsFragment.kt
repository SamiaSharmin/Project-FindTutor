package com.example.project_findtutor

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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

class AdminUserMeetingsFragment : Fragment(R.layout.fragment_admin_user_meetings) {

    private lateinit var db: DatabaseReference
    private lateinit var tvMeetingsTotal: TextView
    private lateinit var tvRefreshMeetings: TextView
    private lateinit var tvMeetingResultCount: TextView
    private lateinit var rvAdminMeetings: RecyclerView
    private lateinit var emptyMeetingsLayout: LinearLayout
    private lateinit var progressMeetings: ProgressBar
    private lateinit var tvEmptyMeetings: TextView
    private lateinit var chipAllMeetings: MaterialCardView
    private lateinit var chipPendingMeetings: MaterialCardView
    private lateinit var chipConfirmedMeetings: MaterialCardView
    private lateinit var chipCompletedMeetings: MaterialCardView
    private lateinit var tvChipAllMeetings: TextView
    private lateinit var tvChipPendingMeetings: TextView
    private lateinit var tvChipConfirmedMeetings: TextView
    private lateinit var tvChipCompletedMeetings: TextView
    private lateinit var adapter: AdminMeetingsAdapter
    private val allMeetings = mutableListOf<Meeting>()

    private var selectedFilter = MeetingFilter.ALL
    private var meetingsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseDatabase.getInstance().reference

        requireActivity().findViewById<TextView>(R.id.tvAdminDashboardTitle).text = "Meeting"

        tvMeetingsTotal = view.findViewById(R.id.tvMeetingsTotal)
        tvRefreshMeetings = view.findViewById(R.id.tvRefreshMeetings)
        tvMeetingResultCount = view.findViewById(R.id.tvMeetingResultCount)

        rvAdminMeetings = view.findViewById(R.id.rvAdminMeetings)
        emptyMeetingsLayout = view.findViewById(R.id.emptyMeetingsLayout)
        progressMeetings = view.findViewById(R.id.progressMeetings)
        tvEmptyMeetings = view.findViewById(R.id.tvEmptyMeetings)

        chipAllMeetings = view.findViewById(R.id.chipAllMeetings)
        chipPendingMeetings = view.findViewById(R.id.chipPendingMeetings)
        chipConfirmedMeetings = view.findViewById(R.id.chipConfirmedMeetings)
        chipCompletedMeetings = view.findViewById(R.id.chipCompletedMeetings)

        tvChipAllMeetings = view.findViewById(R.id.tvChipAllMeetings)
        tvChipPendingMeetings = view.findViewById(R.id.tvChipPendingMeetings)
        tvChipConfirmedMeetings = view.findViewById(R.id.tvChipConfirmedMeetings)
        tvChipCompletedMeetings = view.findViewById(R.id.tvChipCompletedMeetings)

        setupRecyclerView()
        setupFilterChips()
        setupRefreshButton()
        loadMeetings()
    }

    private fun setupRecyclerView() {
        adapter = AdminMeetingsAdapter { meeting, newStatus ->
            updateMeetingStatus(meeting, newStatus)
        }

        rvAdminMeetings.layoutManager = LinearLayoutManager(requireContext())
        rvAdminMeetings.adapter = adapter
    }

    private fun setupFilterChips() {
        chipAllMeetings.setOnClickListener {
            selectedFilter = MeetingFilter.ALL
            updateChipUi()
            applyFilters()
        }

        chipPendingMeetings.setOnClickListener {
            selectedFilter = MeetingFilter.PENDING
            updateChipUi()
            applyFilters()
        }

        chipConfirmedMeetings.setOnClickListener {
            selectedFilter = MeetingFilter.CONFIRMED
            updateChipUi()
            applyFilters()
        }

        chipCompletedMeetings.setOnClickListener {
            selectedFilter = MeetingFilter.COMPLETED
            updateChipUi()
            applyFilters()
        }

        updateChipUi()
    }

    private fun setupRefreshButton() {
        tvRefreshMeetings.setOnClickListener {
            loadMeetings()
        }
    }

    private fun loadMeetings() {
        showLoadingState()

        meetingsListener?.let {
            db.child("Meetings").removeEventListener(it)
        }

        meetingsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allMeetings.clear()

                for (meetingSnapshot in snapshot.children) {
                    allMeetings.add(parseMeeting(meetingSnapshot))
                }

                allMeetings.sortWith(
                    compareByDescending<Meeting> { it.createdAt }.thenByDescending { it.date }
                )

                updateChipLabels()
                applyFilters()

                allMeetings.forEach { meeting ->
                    fetchTutorNameForMeeting(
                        tutorId = meeting.tutorId,
                        meetingId = meeting.meetingId
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressMeetings.visibility = View.GONE
                tvEmptyMeetings.text = "Failed to load meetings"

                Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        db.child("Meetings").addValueEventListener(meetingsListener as ValueEventListener)
    }

    private fun parseMeeting(snapshot: DataSnapshot): Meeting {
        return Meeting(
            meetingId = firstNonBlank(
                snapshot.getStringValue("meetingId"),
                snapshot.key.orEmpty()
            ),
            jobId = snapshot.getIntValue("jobId"),
            studentId = snapshot.getStringValue("studentId"),
            studentName = snapshot.getStringValue("studentName"),
            studentPhoneNumber = firstNonBlank(
                snapshot.getStringValue("studentPhoneNumber"),
                snapshot.getStringValue("studentPhone"),
                snapshot.getStringValue("phoneNumber")
            ),
            tutorId = snapshot.getStringValue("tutorId"),
            date = snapshot.getStringValue("date"),
            time = snapshot.getStringValue("time"),
            location = snapshot.getStringValue("location"),
            status = normalizeStatus(snapshot.getStringValue("status")),
            createdAt = snapshot.getLongValue("createdAt"),
            reviewSubmitted = snapshot.getBooleanValue("reviewSubmitted"),
            reviewRating = snapshot.getFloatValue("reviewRating"),
            reviewText = snapshot.getStringValue("reviewText")
        )
    }

    private fun applyFilters() {
        val filteredMeetings = allMeetings.filter { meeting ->
            matchesSelectedFilter(meeting)
        }

        adapter.submitList(filteredMeetings)

        progressMeetings.visibility = View.GONE

        tvMeetingsTotal.text = "${formatNumber(allMeetings.size)} Total"
        tvMeetingResultCount.text =
            "Showing ${formatNumber(filteredMeetings.size)} of ${formatNumber(allMeetings.size)} results"

        if (filteredMeetings.isEmpty()) {
            emptyMeetingsLayout.visibility = View.VISIBLE
            tvEmptyMeetings.text = if (allMeetings.isEmpty()) {
                "No meetings found"
            } else {
                "No matching meetings found"
            }
        } else {
            emptyMeetingsLayout.visibility = View.GONE
        }
    }

    private fun matchesSelectedFilter(meeting: Meeting): Boolean {
        val status = normalizeStatus(meeting.status)

        return when (selectedFilter) {
            MeetingFilter.ALL -> true
            MeetingFilter.PENDING -> status == "pending"
            MeetingFilter.CONFIRMED -> status == "confirmed"
            MeetingFilter.COMPLETED -> status == "completed"
        }
    }

    private fun fetchTutorNameForMeeting(tutorId: String, meetingId: String) {
        if (tutorId.isBlank() || meetingId.isBlank()) return

        db.child("Tutors").child(tutorId).get()
            .addOnSuccessListener { tutorSnapshot ->
                val tutorName = tutorSnapshot.getStringValue("name")

                if (tutorName.isNotBlank()) {
                    adapter.setTutorName(meetingId, tutorName)
                } else {
                    fetchTutorNameByUserId(tutorId, meetingId)
                }
            }
            .addOnFailureListener {
                fetchTutorNameByUserId(tutorId, meetingId)
            }
    }

    private fun fetchTutorNameByUserId(tutorId: String, meetingId: String) {
        db.child("Tutors")
            .orderByChild("userId")
            .equalTo(tutorId)
            .get()
            .addOnSuccessListener { snapshot ->
                for (tutorSnapshot in snapshot.children) {
                    val tutorName = tutorSnapshot.getStringValue("name")
                    if (tutorName.isNotBlank()) {
                        adapter.setTutorName(meetingId, tutorName)
                        break
                    }
                }
            }
    }

    private fun updateMeetingStatus(meeting: Meeting, newStatus: String) {
        val normalizedStatus = normalizeStatus(newStatus)

        val updates = hashMapOf<String, Any?>()
        updates["Meetings/${meeting.meetingId}/status"] = normalizedStatus

        val auditId = db.child("AuditLogs").push().key
        if (auditId != null) {
            updates["AuditLogs/$auditId"] = mapOf(
                "action" to "MEETING_STATUS_CHANGED",
                "meetingId" to meeting.meetingId,
                "jobId" to meeting.jobId,
                "studentId" to meeting.studentId,
                "tutorId" to meeting.tutorId,
                "oldStatus" to meeting.status,
                "newStatus" to normalizedStatus,
                "timestamp" to ServerValue.TIMESTAMP
            )
        }

        db.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Meeting marked as ${displayStatus(normalizedStatus)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Failed to update meeting",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateChipLabels() {
        tvChipAllMeetings.text = "All (${formatNumber(allMeetings.size)})"
        tvChipPendingMeetings.text =
            "Pending (${formatNumber(allMeetings.count { normalizeStatus(it.status) == "pending" })})"
        tvChipConfirmedMeetings.text =
            "Confirmed (${formatNumber(allMeetings.count { normalizeStatus(it.status) == "confirmed" })})"
        tvChipCompletedMeetings.text =
            "Completed (${formatNumber(allMeetings.count { normalizeStatus(it.status) == "completed" })})"
    }

    private fun updateChipUi() {
        setChipState(chipAllMeetings, tvChipAllMeetings, selectedFilter == MeetingFilter.ALL)
        setChipState(chipPendingMeetings, tvChipPendingMeetings, selectedFilter == MeetingFilter.PENDING)
        setChipState(chipConfirmedMeetings, tvChipConfirmedMeetings, selectedFilter == MeetingFilter.CONFIRMED)
        setChipState(chipCompletedMeetings, tvChipCompletedMeetings, selectedFilter == MeetingFilter.COMPLETED)
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
        emptyMeetingsLayout.visibility = View.VISIBLE
        progressMeetings.visibility = View.VISIBLE
        tvEmptyMeetings.text = "Loading meetings"
    }

    private fun normalizeStatus(status: String): String {
        return when (status.trim().lowercase(Locale.ROOT)) {
            "", "pending" -> "pending"
            "confirmed", "accepted", "approved" -> "confirmed"
            "completed", "complete", "done" -> "completed"
            "cancelled", "canceled", "rejected" -> "cancelled"
            else -> status.trim().lowercase(Locale.ROOT)
        }
    }

    private fun displayStatus(status: String): String {
        return when (status) {
            "pending" -> "Pending"
            "confirmed" -> "Confirmed"
            "completed" -> "Completed"
            "cancelled" -> "Cancelled"
            else -> status.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
        }
    }

    private fun firstNonBlank(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun DataSnapshot.getStringValue(key: String): String {
        return child(key).value?.toString()?.trim().orEmpty()
    }

    private fun DataSnapshot.getIntValue(key: String): Int {
        val value = child(key).value ?: return 0

        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Double -> value.toInt()
            is Float -> value.toInt()
            is String -> value.toIntOrNull() ?: 0
            else -> 0
        }
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

    private fun DataSnapshot.getBooleanValue(key: String): Boolean {
        val value = child(key).value ?: return false

        return when (value) {
            is Boolean -> value
            is String -> value.equals("true", ignoreCase = true)
            is Long -> value == 1L
            is Int -> value == 1
            else -> false
        }
    }

    private fun formatNumber(value: Int): String {
        return NumberFormat.getIntegerInstance(Locale.US).format(value)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        meetingsListener?.let {
            db.child("Meetings").removeEventListener(it)
        }

        meetingsListener = null
        super.onDestroyView()
    }

    private enum class MeetingFilter {
        ALL,
        PENDING,
        CONFIRMED,
        COMPLETED
    }

}