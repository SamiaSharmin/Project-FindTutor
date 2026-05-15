package com.example.project_findtutor

import android.os.Bundle
import android.telephony.PhoneNumberUtils.formatNumber
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminDashboardFragment : Fragment(R.layout.fragment_admin_dashboard) {

    lateinit var db: DatabaseReference
    lateinit var tvActivePostsValue: TextView
    lateinit var tvPendingTutorsValue: TextView
    lateinit var tvMeetingsTodayValue: TextView
    lateinit var tvPlatformRevenueValue: TextView
    private val firebaseListeners = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseDatabase.getInstance().reference

        tvActivePostsValue = view.findViewById(R.id.tvActivePostsValue)
        tvPendingTutorsValue = view.findViewById(R.id.tvPendingTutorsValue)
        tvMeetingsTodayValue = view.findViewById(R.id.tvMeetingsTodayValue)
        tvPlatformRevenueValue = view.findViewById(R.id.tvPlatformRevenueValue)

        requireActivity()
            .findViewById<TextView>(R.id.tvAdminDashboardTitle)
            .text = "Admin Dashboard"

        setupQuickActions(view)
        setupLiveFeed(view)
        loadDashboardMetrics()
    }

    private fun setupQuickActions(view: View) {
        view.findViewById<View>(R.id.actionVerifyTutors).setOnClickListener {
            selectBottomNavItem(R.id.navAdminUsers)
        }

        view.findViewById<View>(R.id.actionUserReport).setOnClickListener {
            selectBottomNavItem(R.id.navAdminReports)
        }

        view.findViewById<View>(R.id.actionManagePosts).setOnClickListener {
            selectBottomNavItem(R.id.navAdminPosts)
        }
    }

    private fun setupLiveFeed(view: View) {
        view.findViewById<View>(R.id.cardLiveFeed).setOnClickListener {
            Toast.makeText(requireContext(), "Live feed will be added later", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectBottomNavItem(itemId: Int) {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.adminBottomNavigation)
        bottomNav.selectedItemId = itemId
    }

    private fun loadDashboardMetrics() {
        loadActivePostsCount()
        loadPendingTutorsCount()
        loadMeetingsTodayCount()

        tvPlatformRevenueValue.text = "$0"
    }

    private fun loadActivePostsCount() {
        val ref = db.child("Posts")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val activePosts = snapshot.children.count { post ->
                    isPostOpen(post)
                }

                tvActivePostsValue.text = formatNumber(activePosts)
            }

            override fun onCancelled(error: DatabaseError) {
                showDatabaseError(error)
            }
        }

        attachListener(ref, listener)
    }

    private fun loadPendingTutorsCount() {
        val ref = db.child("Tutors")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pendingTutors = snapshot.children.count { tutor ->
                    isTutorPending(tutor)
                }

                tvPendingTutorsValue.text = formatNumber(pendingTutors)
            }

            override fun onCancelled(error: DatabaseError) {
                showDatabaseError(error)
            }
        }

        attachListener(ref, listener)
    }

    private fun loadMeetingsTodayCount() {
        val ref = db.child("Meetings")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val meetingsToday = snapshot.children.count { meeting ->
                    isMeetingToday(meeting)
                }

                tvMeetingsTodayValue.text = formatNumber(meetingsToday)
            }

            override fun onCancelled(error: DatabaseError) {
                showDatabaseError(error)
            }
        }

        attachListener(ref, listener)
    }

    private fun isPostOpen(post: DataSnapshot): Boolean {
        val status = post.child("status").getValue(String::class.java)?.trim()?.lowercase(Locale.ROOT)

        if (status.isNullOrEmpty()) return true

        return status !in setOf(
            "closed",
            "hidden",
            "removed",
            "deleted",
            "inactive"
        )
    }

    private fun isTutorPending(tutor: DataSnapshot): Boolean {
        val verificationStatus = tutor.child("verificationStatus").getValue(String::class.java)?.trim()?.lowercase(Locale.ROOT)

        val status = tutor.child("status").getValue(String::class.java)?.trim()?.lowercase(Locale.ROOT)

        // When verificationStatus exists, use it.
        if (!verificationStatus.isNullOrEmpty()) {
            return verificationStatus in setOf(
                "pending",
                "unverified",
                "not_verified",
                "not verified"
            )
        }

        if (!status.isNullOrEmpty()) {
            return status in setOf(
                "pending",
                "unverified",
                "not_verified",
                "not verified"
            )
        }

        return true
    }

    private fun isMeetingToday(meeting: DataSnapshot): Boolean {
        val meetingDate = meeting.child("date")
            .getValue(String::class.java)
            ?.trim()
            ?: return false

        return getTodayDateFormats().any { today ->
            meetingDate == today || meetingDate.startsWith(today)
        }
    }

    private fun getTodayDateFormats(): Set<String> {
        val today = Calendar.getInstance().time

        return setOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(today),
            SimpleDateFormat("dd/MM/yyyy", Locale.US).format(today),
            SimpleDateFormat("dd-MM-yyyy", Locale.US).format(today),
            SimpleDateFormat("MM/dd/yyyy", Locale.US).format(today)
        )
    }

    private fun attachListener(ref: DatabaseReference, listener: ValueEventListener) {
        ref.addValueEventListener(listener)
        firebaseListeners.add(ref to listener)
    }

    private fun formatNumber(value: Int): String {
        return NumberFormat.getIntegerInstance(Locale.US).format(value)
    }

    private fun showDatabaseError(error: DatabaseError) {
        if (isAdded) {
            Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        firebaseListeners.forEach { pair ->
            pair.first.removeEventListener(pair.second)
        }
        firebaseListeners.clear()
        super.onDestroyView()
    }

}