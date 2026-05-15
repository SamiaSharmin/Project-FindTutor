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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminManageUsersFragment : Fragment(R.layout.fragment_admin_manage_users) {

    private lateinit var db: DatabaseReference

    private lateinit var rvAdminUsers: RecyclerView
    private lateinit var etSearchUsers: EditText
    private lateinit var tvUserResultCount: TextView
    private lateinit var emptyUsersLayout: LinearLayout
    private lateinit var progressUsers: ProgressBar
    private lateinit var tvEmptyUsers: TextView

    private lateinit var chipAllUsers: MaterialCardView
    private lateinit var chipStudents: MaterialCardView
    private lateinit var chipTutors: MaterialCardView
    private lateinit var chipSuspended: MaterialCardView
    private lateinit var chipUnverified: MaterialCardView
    private lateinit var tvChipAllUsers: TextView
    private lateinit var tvChipStudents: TextView
    private lateinit var tvChipTutors: TextView
    private lateinit var tvChipSuspended: TextView
    private lateinit var tvChipUnverified: TextView
    private lateinit var adapter: AdminUsersAdapter

    private val allUsers = mutableListOf<AdminUser>()

    private var selectedFilter = UserFilter.ALL
    private var searchQuery = ""
    private var usersListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance().reference

        rvAdminUsers = view.findViewById(R.id.rvAdminUsers)
        etSearchUsers = view.findViewById(R.id.etSearchUsers)
        tvUserResultCount = view.findViewById(R.id.tvUserResultCount)
        emptyUsersLayout = view.findViewById(R.id.emptyUsersLayout)
        progressUsers = view.findViewById(R.id.progressUsers)
        tvEmptyUsers = view.findViewById(R.id.tvEmptyUsers)

        chipAllUsers = view.findViewById(R.id.chipAllUsers)
        chipStudents = view.findViewById(R.id.chipStudentUsers)
        chipTutors = view.findViewById(R.id.chipTutorUsers)
        chipSuspended = view.findViewById(R.id.chipSuspended)
        chipUnverified = view.findViewById(R.id.chipUnverified)

        tvChipAllUsers = view.findViewById(R.id.tvChipAllUsers)
        tvChipStudents = view.findViewById(R.id.tvChipStudentUsers)
        tvChipTutors = view.findViewById(R.id.tvChipTutorUsers)
        tvChipSuspended = view.findViewById(R.id.tvChipSuspended)
        tvChipUnverified = view.findViewById(R.id.tvChipUnverified)


        requireActivity().findViewById<TextView>(R.id.tvAdminDashboardTitle).text = "View User"

        setupRecyclerView()
        setupSearch()
        setupFilterChips()
        loadUsers()
    }

    private fun setupRecyclerView() {
        adapter = AdminUsersAdapter(
            onUserClick = { user ->
                openUserDetail(user)
            },
            onMoreClick = { user, anchor ->
                showUserActionMenu(user, anchor)
            }
        )

        rvAdminUsers.layoutManager = LinearLayoutManager(requireContext())
        rvAdminUsers.adapter = adapter
    }

    private fun openUserDetail(user: AdminUser) {
        requireActivity()
            .findViewById<TextView>(R.id.tvAdminDashboardTitle)
            .text = "User Detail"

        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.adminFragmentContainer,
                AdminUserDetailFragment.newInstance(
                    user.userId,
                    user.profileId,
                    user.role
                )
            )
            .addToBackStack(null)
            .commit()
    }

    private fun setupSearch() {
        etSearchUsers.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim().orEmpty()
                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilterChips() {
        chipAllUsers.setOnClickListener {
            selectedFilter = UserFilter.ALL
            updateChipUi()
            applyFilters()
        }

        chipStudents.setOnClickListener {
            selectedFilter = UserFilter.STUDENTS
            updateChipUi()
            applyFilters()
        }

        chipTutors.setOnClickListener {
            selectedFilter = UserFilter.TUTORS
            updateChipUi()
            applyFilters()
        }

        chipSuspended.setOnClickListener {
            selectedFilter = UserFilter.SUSPENDED
            updateChipUi()
            applyFilters()
        }

        chipUnverified.setOnClickListener {
            selectedFilter = UserFilter.UNVERIFIED
            updateChipUi()
            applyFilters()
        }

        updateChipUi()
    }

    private fun loadUsers() {
        showLoadingState()

        usersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsers.clear()
                allUsers.addAll(buildUsersFromSnapshot(snapshot))

                updateChipLabels()
                applyFilters()
            }

            override fun onCancelled(error: DatabaseError) {
                progressUsers.visibility = View.GONE
                tvEmptyUsers.text = "Failed to load users"

                Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        db.addValueEventListener(usersListener as ValueEventListener)
    }

    private fun buildUsersFromSnapshot(root: DataSnapshot): List<AdminUser> {
        val result = mutableListOf<AdminUser>()

        val usersRoot = root.child("Users")
        val studentsRoot = root.child("Students")
        val tutorsRoot = root.child("Tutors")

        val addedUserIds = mutableSetOf<String>()
        val addedProfiles = mutableSetOf<String>()

        for (userSnap in usersRoot.children) {
            val userId = firstNonBlank(
                userSnap.getStringValue("userId"),
                userSnap.key.orEmpty()
            )

            var role = userSnap.getStringValue("role").lowercase(Locale.ROOT)

            if (role == "admin") {
                continue
            }

            if (role.isBlank()) {
                role = inferRole(userId, studentsRoot, tutorsRoot)
            }

            val profileSnap = when (role) {
                "student" -> findProfileSnapshot(studentsRoot, userId)
                "tutor" -> findProfileSnapshot(tutorsRoot, userId)
                else -> null
            }

            val profileId = profileSnap?.key.orEmpty()
            result.add(
                createAdminUser(
                    userSnap = userSnap,
                    profileSnap = profileSnap,
                    userId = userId,
                    profileId = if (profileId.isNotBlank()) profileId else userId,
                    role = if (role.isBlank()) "user" else role
                )
            )

            addedUserIds.add(userId)

            if (profileId.isNotBlank()) {
                addedProfiles.add("$role:$profileId")
            }
        }

        for (studentSnap in studentsRoot.children) {
            val profileId = studentSnap.key.orEmpty()
            val userId = firstNonBlank(
                studentSnap.getStringValue("userId"),
                profileId
            )

            if (addedUserIds.contains(userId) || addedProfiles.contains("student:$profileId")) {
                continue
            }

            result.add(
                createAdminUser(
                    userSnap = null,
                    profileSnap = studentSnap,
                    userId = userId,
                    profileId = profileId,
                    role = "student"
                )
            )
        }

        for (tutorSnap in tutorsRoot.children) {
            val profileId = tutorSnap.key.orEmpty()
            val userId = firstNonBlank(tutorSnap.getStringValue("userId"), profileId)

            if (addedUserIds.contains(userId) || addedProfiles.contains("tutor:$profileId")) {
                continue
            }

            result.add(createAdminUser(userSnap = null, profileSnap = tutorSnap, userId = userId, profileId = profileId, role = "tutor"))
        }

        return result.sortedWith(compareBy<AdminUser> { it.role }.thenBy { it.name.lowercase(Locale.ROOT) })
    }

    private fun createAdminUser(userSnap: DataSnapshot?, profileSnap: DataSnapshot?, userId: String, profileId: String, role: String): AdminUser {
        val normalizedRole = role.lowercase(Locale.ROOT)

        val name = firstNonBlank(
            profileSnap?.getStringValue("name").orEmpty(),
            userSnap?.getStringValue("name").orEmpty(),
            "Unnamed User"
        )

        val email = firstNonBlank(
            profileSnap?.getStringValue("email").orEmpty(),
            userSnap?.getStringValue("email").orEmpty(),
            "No email"
        )

        val phone = firstNonBlank(
            profileSnap?.getStringValue("phoneNumber").orEmpty(),
            profileSnap?.getStringValue("phone number").orEmpty(),
            profileSnap?.getStringValue("phone").orEmpty(),
            userSnap?.getStringValue("phoneNumber").orEmpty(),
            userSnap?.getStringValue("phone number").orEmpty(),
            userSnap?.getStringValue("phone").orEmpty()
        )

        val area = firstNonBlank(
            profileSnap?.getStringValue("area").orEmpty(),
            profileSnap?.getStringValue("location").orEmpty(),
            profileSnap?.getStringValue("address").orEmpty(),
            userSnap?.getStringValue("area").orEmpty(),
            userSnap?.getStringValue("location").orEmpty(),
            "Area not set"
        )

        val status = normalizeStatus(
            firstNonBlank(
                userSnap?.getStringValue("status").orEmpty(),
                profileSnap?.getStringValue("status").orEmpty(),
                "active"
            )
        )

        val verificationStatus = normalizeVerificationStatus(
            firstNonBlank(
                userSnap?.getStringValue("verificationStatus").orEmpty(),
                profileSnap?.getStringValue("verificationStatus").orEmpty(),
                if (normalizedRole == "tutor") "unverified" else "verified"
            )
        )

        val rating = firstPositiveFloat(profileSnap?.getFloatValue("rating") ?: 0f,
            userSnap?.getFloatValue("rating") ?: 0f
        )

        val registeredAt = readRegistrationText(userSnap, profileSnap)

        return AdminUser(
            userId = userId,
            profileId = profileId,
            role = normalizedRole,
            name = name,
            email = email,
            phone = phone,
            area = area,
            rating = rating,
            status = status,
            verificationStatus = verificationStatus,
            registeredAt = registeredAt
        )
    }

    private fun applyFilters() {
        val query = searchQuery.lowercase(Locale.ROOT)

        val filteredUsers = allUsers.filter { user ->
            matchesSelectedFilter(user) && matchesSearchQuery(user, query)
        }

        adapter.submitList(filteredUsers)

        progressUsers.visibility = View.GONE
        tvUserResultCount.text = "Showing ${formatNumber(filteredUsers.size)} of ${formatNumber(allUsers.size)} results"

        if (filteredUsers.isEmpty()) {
            emptyUsersLayout.visibility = View.VISIBLE
            tvEmptyUsers.text = if (allUsers.isEmpty()) {
                "No users found"
            } else {
                "No matching users found"
            }
        } else {
            emptyUsersLayout.visibility = View.GONE
        }
    }

    private fun matchesSelectedFilter(user: AdminUser): Boolean {
        return when (selectedFilter) {
            UserFilter.ALL -> true
            UserFilter.STUDENTS -> user.role == "student"
            UserFilter.TUTORS -> user.role == "tutor"
            UserFilter.SUSPENDED -> user.status == "suspended"
            UserFilter.UNVERIFIED -> user.verificationStatus != "verified"
        }
    }

    private fun matchesSearchQuery(user: AdminUser, query: String): Boolean {
        if (query.isBlank()) return true

        return user.name.lowercase(Locale.ROOT).contains(query) ||
                user.email.lowercase(Locale.ROOT).contains(query) ||
                user.phone.lowercase(Locale.ROOT).contains(query) ||
                user.area.lowercase(Locale.ROOT).contains(query) ||
                user.role.lowercase(Locale.ROOT).contains(query) ||
                user.status.lowercase(Locale.ROOT).contains(query) ||
                user.verificationStatus.lowercase(Locale.ROOT).contains(query)
    }

    private fun showUserActionMenu(user: AdminUser, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)

        popup.menu.add(0, ACTION_ACTIVATE, 0, "Activate")
        popup.menu.add(0, ACTION_DEACTIVATE, 1, "Deactivate")
        popup.menu.add(0, ACTION_SUSPEND, 2, "Suspend")
        popup.menu.add(0, ACTION_RESTORE, 3, "Restore")

        if (user.role == "tutor") {
            popup.menu.add(0, ACTION_VERIFY, 4, "Mark Verified")
            popup.menu.add(0, ACTION_UNVERIFY, 5, "Mark Unverified")
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                ACTION_ACTIVATE -> {
                    updateUserStatus(user, "active")
                    true
                }

                ACTION_DEACTIVATE -> {
                    updateUserStatus(user, "deactivated")
                    true
                }

                ACTION_SUSPEND -> {
                    updateUserStatus(user, "suspended")
                    true
                }

                ACTION_RESTORE -> {
                    updateUserStatus(user, "active")
                    true
                }

                ACTION_VERIFY -> {
                    updateTutorVerification(user, "verified")
                    true
                }

                ACTION_UNVERIFY -> {
                    updateTutorVerification(user, "unverified")
                    true
                }

                else -> false
            }
        }

        popup.show()
    }

    private fun updateUserStatus(user: AdminUser, newStatus: String) {
        val updates = hashMapOf<String, Any?>()

        updates["Users/${user.userId}/status"] = newStatus

        when (user.role) {
            "student" -> updates["Students/${user.profileId}/status"] = newStatus
            "tutor" -> updates["Tutors/${user.profileId}/status"] = newStatus
        }

        val auditId = db.child("AuditLogs").push().key
        if (auditId != null) {
            updates["AuditLogs/$auditId"] = mapOf(
                "action" to "USER_STATUS_CHANGED",
                "targetUserId" to user.userId,
                "targetProfileId" to user.profileId,
                "targetRole" to user.role,
                "oldStatus" to user.status,
                "newStatus" to newStatus,
                "timestamp" to ServerValue.TIMESTAMP
            )
        }

        db.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "User status updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Failed to update status",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateTutorVerification(user: AdminUser, newVerificationStatus: String) {
        if (user.role != "tutor") return

        val updates = hashMapOf<String, Any?>()

        updates["Users/${user.userId}/verificationStatus"] = newVerificationStatus
        updates["Tutors/${user.profileId}/verificationStatus"] = newVerificationStatus

        val auditId = db.child("AuditLogs").push().key
        if (auditId != null) {
            updates["AuditLogs/$auditId"] = mapOf(
                "action" to "TUTOR_VERIFICATION_CHANGED",
                "targetUserId" to user.userId,
                "targetProfileId" to user.profileId,
                "oldVerificationStatus" to user.verificationStatus,
                "newVerificationStatus" to newVerificationStatus,
                "timestamp" to ServerValue.TIMESTAMP
            )
        }

        db.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Tutor verification updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Failed to update verification",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateChipLabels() {
        tvChipAllUsers.text = "All Users (${formatNumber(allUsers.size)})"
        tvChipStudents.text = "Students (${formatNumber(allUsers.count { it.role == "student" })})"
        tvChipTutors.text = "Tutors (${formatNumber(allUsers.count { it.role == "tutor" })})"
        tvChipSuspended.text = "Suspended (${formatNumber(allUsers.count { it.status == "suspended" })})"
        tvChipUnverified.text =
            "Unverified (${formatNumber(allUsers.count { it.verificationStatus != "verified" })})"
    }

    private fun updateChipUi() {
        setChipState(chipAllUsers, tvChipAllUsers, selectedFilter == UserFilter.ALL)
        setChipState(chipStudents, tvChipStudents, selectedFilter == UserFilter.STUDENTS)
        setChipState(chipTutors, tvChipTutors, selectedFilter == UserFilter.TUTORS)
        setChipState(chipSuspended, tvChipSuspended, selectedFilter == UserFilter.SUSPENDED)
        setChipState(chipUnverified, tvChipUnverified, selectedFilter == UserFilter.UNVERIFIED)
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
        emptyUsersLayout.visibility = View.VISIBLE
        progressUsers.visibility = View.VISIBLE
        tvEmptyUsers.text = "Loading users"
    }

    private fun findProfileSnapshot(parent: DataSnapshot, userId: String): DataSnapshot? {
        val directChild = parent.child(userId)
        if (directChild.exists()) {
            return directChild
        }

        return parent.children.firstOrNull { child ->
            child.getStringValue("userId") == userId
        }
    }

    private fun inferRole(userId: String, studentsRoot: DataSnapshot, tutorsRoot: DataSnapshot): String {
        return when {
            findProfileSnapshot(studentsRoot, userId) != null -> "student"
            findProfileSnapshot(tutorsRoot, userId) != null -> "tutor"
            else -> "user"
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

    private fun normalizeVerificationStatus(status: String): String {
        return when (status.trim().lowercase(Locale.ROOT)) {
            "", "verified", "approved" -> "verified"
            "pending" -> "pending"
            "unverified", "not verified", "not_verified", "rejected" -> "unverified"
            else -> status.trim().lowercase(Locale.ROOT)
        }
    }

    private fun readRegistrationText(userSnap: DataSnapshot?, profileSnap: DataSnapshot?): String {
        val rawValue = firstNonNull(
            userSnap?.child("registeredAt")?.value,
            userSnap?.child("registrationDate")?.value,
            userSnap?.child("createdAt")?.value,
            userSnap?.child("timestamp")?.value,
            profileSnap?.child("registeredAt")?.value,
            profileSnap?.child("registrationDate")?.value,
            profileSnap?.child("createdAt")?.value,
            profileSnap?.child("timestamp")?.value
        )

        return formatDateValue(rawValue) ?: "Joined date unavailable"
    }

    private fun formatDateValue(value: Any?): String? {
        if (value == null) return null

        val timestamp = when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            is Float -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }

        if (timestamp != null && timestamp > 0L) {
            val millis = if (timestamp < 10_000_000_000L) timestamp * 1000 else timestamp
            return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(millis))
        }

        val textValue = value.toString().trim()
        return textValue.ifBlank { null }
    }

    private fun firstNonBlank(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun firstNonNull(vararg values: Any?): Any? {
        return values.firstOrNull { it != null }
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

    private fun formatNumber(value: Int): String {
        return NumberFormat.getIntegerInstance(Locale.US).format(value)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        usersListener?.let {
            db.removeEventListener(it)
        }

        usersListener = null
        super.onDestroyView()
    }

    private enum class UserFilter {
        ALL,
        STUDENTS,
        TUTORS,
        SUSPENDED,
        UNVERIFIED
    }


    companion object {
        private const val ACTION_ACTIVATE = 1
        private const val ACTION_DEACTIVATE = 2
        private const val ACTION_SUSPEND = 3
        private const val ACTION_RESTORE = 4
        private const val ACTION_VERIFY = 5
        private const val ACTION_UNVERIFY = 6
    }
}