package com.example.project_findtutor

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class AdminUserDetailFragment : Fragment(R.layout.fragment_admin_user_detail) {

    private lateinit var db: DatabaseReference
    private lateinit var tvDetailUserName: TextView
    private lateinit var tvDetailUserRole: TextView
    private lateinit var tvDetailUserStatus: TextView
    private lateinit var tvDetailUserEmail: TextView
    private lateinit var tvDetailUserPhone: TextView
    private lateinit var tvDetailUserQualification: TextView
    private lateinit var tvDetailUserPreferredAreas: TextView
    private lateinit var tvDetailUserRating: TextView
    private lateinit var layoutTutorQualification: LinearLayout
    private lateinit var layoutTutorPreferredAreas: LinearLayout
    private lateinit var btnActivateUser: MaterialButton
    private lateinit var btnSuspendUser: MaterialButton
    private lateinit var btnDeactivateUser: MaterialButton
    private lateinit var btnRestoreUser: MaterialButton
    private lateinit var btnSaveUserStatus: MaterialButton

    private lateinit var rvUserReviews: RecyclerView
    private lateinit var layoutNoReviews: LinearLayout
    private lateinit var tvReviewCount: TextView

    private lateinit var reviewsAdapter: AdminUserReviewsAdapter
    private var userId: String = ""
    private var profileId: String = ""
    private var role: String = ""
    private var currentStatus: String = "active"
    private var selectedStatus: String = "active"
    private var profileListener: ValueEventListener? = null
    private var reviewsListener: ValueEventListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseDatabase.getInstance().reference

        userId = arguments?.getString(ARG_USER_ID).orEmpty()
        profileId = arguments?.getString(ARG_PROFILE_ID).orEmpty()
        role = arguments?.getString(ARG_ROLE).orEmpty().lowercase(Locale.ROOT)

        tvDetailUserName = view.findViewById(R.id.tvDetailUserName)
        tvDetailUserRole = view.findViewById(R.id.tvDetailUserRole)
        tvDetailUserStatus = view.findViewById(R.id.tvDetailUserStatus)
        tvDetailUserEmail = view.findViewById(R.id.tvDetailUserEmail)
        tvDetailUserPhone = view.findViewById(R.id.tvDetailUserPhone)
        tvDetailUserQualification = view.findViewById(R.id.tvDetailUserQualification)
        tvDetailUserPreferredAreas = view.findViewById(R.id.tvDetailUserPreferredAreas)
        tvDetailUserRating = view.findViewById(R.id.tvDetailUserRating)

        layoutTutorQualification = view.findViewById(R.id.layoutTutorQualification)
        layoutTutorPreferredAreas = view.findViewById(R.id.layoutTutorPreferredAreas)

        btnActivateUser = view.findViewById(R.id.btnActivateUser)
        btnSuspendUser = view.findViewById(R.id.btnSuspendUser)
        btnDeactivateUser = view.findViewById(R.id.btnDeactivateUser)
        btnRestoreUser = view.findViewById(R.id.btnRestoreUser)
        btnSaveUserStatus = view.findViewById(R.id.btnSaveUserStatus)

        rvUserReviews = view.findViewById(R.id.rvUserReviews)
        layoutNoReviews = view.findViewById(R.id.layoutNoReviews)
        tvReviewCount = view.findViewById(R.id.tvReviewCount)

        requireActivity()
            .findViewById<TextView>(R.id.tvAdminDashboardTitle)
            .text = "User Detail"

        setupReviewsRecyclerView()
        setupStatusButtons()

        loadUserProfile()
        loadUserReviews()
    }

    private fun setupReviewsRecyclerView() {
        reviewsAdapter = AdminUserReviewsAdapter()

        rvUserReviews.layoutManager = LinearLayoutManager(requireContext())
        rvUserReviews.adapter = reviewsAdapter
        rvUserReviews.isNestedScrollingEnabled = false
    }

    private fun setupStatusButtons() {
        btnActivateUser.setOnClickListener {
            selectedStatus = "active"
            updateSelectedStatusButtons()
        }

        btnSuspendUser.setOnClickListener {
            selectedStatus = "suspended"
            updateSelectedStatusButtons()
        }

        btnDeactivateUser.setOnClickListener {
            selectedStatus = "deactivated"
            updateSelectedStatusButtons()
        }

        btnRestoreUser.setOnClickListener {
            selectedStatus = "active"
            updateSelectedStatusButtons()
        }

        btnSaveUserStatus.setOnClickListener {
            updateUserStatus()
        }
    }

    private fun loadUserProfile() {
        val profileRef = when (role) {
            "student" -> db.child("Students").child(profileId)
            "tutor" -> db.child("Tutors").child(profileId)
            else -> db.child("Users").child(userId)
        }

        profileListener = object : ValueEventListener {
            override fun onDataChange(profileSnapshot: DataSnapshot) {
                db.child("Users").child(userId).get()
                    .addOnSuccessListener { userSnapshot ->
                        bindUserProfile(userSnapshot, profileSnapshot)
                    }
                    .addOnFailureListener {
                        bindUserProfile(null, profileSnapshot)
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load profile: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        profileRef.addValueEventListener(profileListener as ValueEventListener)
    }

    private fun bindUserProfile(userSnapshot: DataSnapshot?, profileSnapshot: DataSnapshot) {
        val name = firstNonBlank(
            profileSnapshot.getStringValue("name"),
            userSnapshot?.getStringValue("name").orEmpty(),
            "Unnamed User"
        )

        val email = firstNonBlank(
            profileSnapshot.getStringValue("email"),
            userSnapshot?.getStringValue("email").orEmpty(),
            "No email"
        )

        val phone = firstNonBlank(
            profileSnapshot.getStringValue("phoneNumber"),
            profileSnapshot.getStringValue("phone number"),
            profileSnapshot.getStringValue("phone"),
            userSnapshot?.getStringValue("phoneNumber").orEmpty(),
            userSnapshot?.getStringValue("phone number").orEmpty(),
            userSnapshot?.getStringValue("phone").orEmpty(),
            "No phone number"
        )

        val qualification = firstNonBlank(
            profileSnapshot.getStringValue("qualification"),
            "Not available"
        )

        val preferredAreas = firstNonBlank(
            profileSnapshot.getStringValue("preferredAreas"),
            profileSnapshot.getStringValue("preferedAreas"),
            "Not available"
        )

        val rating = firstPositiveFloat(
            profileSnapshot.getFloatValue("rating"),
            userSnapshot?.getFloatValue("rating") ?: 0f
        )

        currentStatus = normalizeStatus(
            firstNonBlank(
                userSnapshot?.getStringValue("status").orEmpty(),
                profileSnapshot.getStringValue("status"),
                "active"
            )
        )

        selectedStatus = currentStatus

        tvDetailUserName.text = name
        tvDetailUserEmail.text = email
        tvDetailUserPhone.text = phone
        tvDetailUserRole.text = displayRole(role)
        tvDetailUserStatus.text = displayStatus(currentStatus)
        tvDetailUserRating.text = if (rating > 0f) {
            String.format(Locale.US, "%.1f", rating)
        } else {
            "No rating yet"
        }

        if (role == "tutor") {
            layoutTutorQualification.visibility = View.VISIBLE
            layoutTutorPreferredAreas.visibility = View.VISIBLE
            tvDetailUserQualification.text = qualification
            tvDetailUserPreferredAreas.text = preferredAreas
        } else {
            layoutTutorQualification.visibility = View.GONE
            layoutTutorPreferredAreas.visibility = View.GONE
        }

        updateStatusUi()
        updateSelectedStatusButtons()
    }

    private fun loadUserReviews() {
        val ref = db.child("Reviews")

        reviewsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reviews = mutableListOf<Review>()

                for (reviewSnapshot in snapshot.children) {
                    val review = parseReview(reviewSnapshot)

                    val matchesUser = when (role) {
                        "student" -> review.studentId == profileId || review.studentId == userId
                        "tutor" -> review.tutorId == profileId || review.tutorId == userId
                        else -> false
                    }

                    if (matchesUser) {
                        reviews.add(review)
                    }
                }

                val sortedReviews = reviews.sortedByDescending { it.timestamp }

                reviewsAdapter.submitList(sortedReviews)
                tvReviewCount.text = "${sortedReviews.size} reviews"

                layoutNoReviews.visibility = if (sortedReviews.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
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
        ref.addValueEventListener(reviewsListener as ValueEventListener)
    }

    private fun parseReview(snapshot: DataSnapshot): Review {
        return Review(
            reviewId = snapshot.key.orEmpty(),
            studentId = snapshot.getStringValue("studentId"),
            tutorId = snapshot.getStringValue("tutorId"),
            rating = snapshot.getFloatValue("rating"),
            reviewText = snapshot.getStringValue("comment"),
            timestamp = snapshot.getLongValue("timestamp")
        )
    }

    private fun updateUserStatus() {
        val updates = hashMapOf<String, Any?>()

        updates["Users/$userId/status"] = selectedStatus

        when (role) {
            "student" -> updates["Students/$profileId/status"] = selectedStatus
            "tutor" -> updates["Tutors/$profileId/status"] = selectedStatus
        }

        val auditId = db.child("AuditLogs").push().key
        if (auditId != null) {
            updates["AuditLogs/$auditId"] = mapOf(
                "action" to "USER_STATUS_CHANGED",
                "targetUserId" to userId,
                "targetProfileId" to profileId,
                "targetRole" to role,
                "oldStatus" to currentStatus,
                "newStatus" to selectedStatus,
                "timestamp" to ServerValue.TIMESTAMP
            )
        }

        db.updateChildren(updates)
            .addOnSuccessListener {
                currentStatus = selectedStatus
                tvDetailUserStatus.text = displayStatus(currentStatus)
                updateStatusUi()
                Toast.makeText(requireContext(), "User status updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(requireContext(), error.message ?: "Failed to update user status", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateStatusUi() {
        when (currentStatus) {
            "active" -> {
                tvDetailUserStatus.setTextColor(Color.parseColor("#166534"))
                tvDetailUserStatus.setBackgroundResource(R.drawable.bg_badge_green)
            }

            "suspended" -> {
                tvDetailUserStatus.setTextColor(Color.parseColor("#991B1B"))
                tvDetailUserStatus.setBackgroundColor(Color.parseColor("#FEE2E2"))
            }

            "deactivated" -> {
                tvDetailUserStatus.setTextColor(Color.parseColor("#374151"))
                tvDetailUserStatus.setBackgroundColor(Color.parseColor("#E5E7EB"))
            }

            else -> {
                tvDetailUserStatus.setTextColor(Color.parseColor("#374151"))
                tvDetailUserStatus.setBackgroundColor(Color.parseColor("#E5E7EB"))
            }
        }
    }

    private fun updateSelectedStatusButtons() {
        resetButton(btnActivateUser, "#17C964", "#CDEFD9")
        resetButton(btnSuspendUser, "#DC2626", "#F5CCCC")
        resetButton(btnDeactivateUser, "#374151", "#D1D5DB")
        resetButton(btnRestoreUser, "#374151", "#D1D5DB")

        when (selectedStatus) {
            "active" -> {
                selectButton(btnActivateUser)
                selectButton(btnRestoreUser)
            }

            "suspended" -> selectButton(btnSuspendUser)
            "deactivated" -> selectButton(btnDeactivateUser)
        }
    }

    private fun resetButton(button: MaterialButton, textColor: String, strokeColor: String) {
        button.setTextColor(Color.parseColor(textColor))
        button.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
        button.strokeColor = ColorStateList.valueOf(Color.parseColor(strokeColor))
    }

    private fun selectButton(button: MaterialButton) {
        button.setTextColor(Color.WHITE)
        button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#17C964"))
        button.strokeColor = ColorStateList.valueOf(Color.parseColor("#17C964"))
    }

    private fun displayRole(role: String): String {
        return when (role) {
            "student" -> "Student"
            "tutor" -> "Tutor"
            else -> "User"
        }
    }

    private fun displayStatus(status: String): String {
        return when (status) {
            "active" -> "Active"
            "deactivated" -> "Deactivated"
            "suspended" -> "Suspended"
            "pending" -> "Pending"
            else -> status.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
        }
    }

    private fun normalizeStatus(status: String): String {
        return when (status.trim().lowercase(Locale.ROOT)) {
            "", "active", "activated" -> "active"
            "inactive", "deactive", "deactivated", "disabled" -> "deactivated"
            "suspended", "blocked", "banned" -> "suspended"
            "pending" -> "pending"
            else -> status.trim().lowercase(Locale.ROOT)
        }
    }

    private fun firstNonBlank(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun firstPositiveFloat(vararg values: Float): Float {
        return values.firstOrNull { it > 0f } ?: 0f
    }

    private fun DataSnapshot.getStringValue(key: String): String {
        return child(key).value?.toString()?.trim().orEmpty()
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

    override fun onDestroyView() {
        profileListener?.let { listener ->
            val ref = when (role) {
                "student" -> db.child("Students").child(profileId)
                "tutor" -> db.child("Tutors").child(profileId)
                else -> db.child("Users").child(userId)
            }

            ref.removeEventListener(listener)
        }

        reviewsListener?.let { listener ->
            db.child("Reviews").removeEventListener(listener)
        }

        profileListener = null
        reviewsListener = null

        super.onDestroyView()
    }


    companion object {

        private const val ARG_USER_ID = "userId"
        private const val ARG_PROFILE_ID = "profileId"
        private const val ARG_ROLE = "role"

        fun newInstance(userId: String, profileId: String, role: String): AdminUserDetailFragment {
            val fragment = AdminUserDetailFragment()

            val bundle = Bundle()
            bundle.putString(ARG_USER_ID, userId)
            bundle.putString(ARG_PROFILE_ID, profileId)
            bundle.putString(ARG_ROLE, role)

            fragment.arguments = bundle
            return fragment
        }

    }
}