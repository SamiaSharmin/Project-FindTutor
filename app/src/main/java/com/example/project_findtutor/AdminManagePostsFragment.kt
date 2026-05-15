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

class AdminManagePostsFragment : Fragment(R.layout.fragment_admin_manage_posts) {
    private lateinit var db: DatabaseReference

    private lateinit var rvAdminPosts: RecyclerView
    private lateinit var etSearchPosts: EditText
    private lateinit var tvPostsTotal: TextView
    private lateinit var tvRefreshPosts: TextView
    private lateinit var tvPostResultCount: TextView
    private lateinit var emptyPostsLayout: LinearLayout
    private lateinit var progressPosts: ProgressBar
    private lateinit var tvEmptyPosts: TextView

    private lateinit var chipAllPosts: MaterialCardView
    private lateinit var chipOpenPosts: MaterialCardView
    private lateinit var chipHiddenPosts: MaterialCardView
    private lateinit var chipClosedPosts: MaterialCardView
    private lateinit var chipRemovedPosts: MaterialCardView

    private lateinit var tvChipAllPosts: TextView
    private lateinit var tvChipOpenPosts: TextView
    private lateinit var tvChipHiddenPosts: TextView
    private lateinit var tvChipClosedPosts: TextView
    private lateinit var tvChipRemovedPosts: TextView

    private lateinit var adapter: AdminPostsAdapter

    private val allPosts = mutableListOf<Post>()

    private var selectedFilter = PostFilter.ALL
    private var searchQuery = ""
    private var postsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseDatabase.getInstance().reference

        requireActivity()
            .findViewById<TextView>(R.id.tvAdminDashboardTitle)
            .text = "Posts"

        rvAdminPosts = view.findViewById(R.id.rvAdminPosts)
        etSearchPosts = view.findViewById(R.id.etSearchPosts)
        tvPostsTotal = view.findViewById(R.id.tvPostsTotal)
        tvRefreshPosts = view.findViewById(R.id.tvRefreshPosts)
        tvPostResultCount = view.findViewById(R.id.tvPostResultCount)
        emptyPostsLayout = view.findViewById(R.id.emptyPostsLayout)
        progressPosts = view.findViewById(R.id.progressPosts)
        tvEmptyPosts = view.findViewById(R.id.tvEmptyPosts)

        chipAllPosts = view.findViewById(R.id.chipAllPosts)
        chipOpenPosts = view.findViewById(R.id.chipOpenPosts)
        chipHiddenPosts = view.findViewById(R.id.chipHiddenPosts)
        chipClosedPosts = view.findViewById(R.id.chipClosedPosts)
        chipRemovedPosts = view.findViewById(R.id.chipRemovedPosts)

        tvChipAllPosts = view.findViewById(R.id.tvChipAllPosts)
        tvChipOpenPosts = view.findViewById(R.id.tvChipOpenPosts)
        tvChipHiddenPosts = view.findViewById(R.id.tvChipHiddenPosts)
        tvChipClosedPosts = view.findViewById(R.id.tvChipClosedPosts)
        tvChipRemovedPosts = view.findViewById(R.id.tvChipRemovedPosts)

        setupRecyclerView()
        setupSearch()
        setupFilterChips()
        setupRefresh()
        loadPosts()
    }

    private fun setupRecyclerView() {
        adapter = AdminPostsAdapter(
            onCloseClick = { post ->
                updatePostStatus(post, "closed")
            },
            onHideClick = { post ->
                updatePostStatus(post, "hidden")
            },
            onRemoveClick = { post ->
                updatePostStatus(post, "removed")
            },
            onStatusMenuClick = { post, status ->
                updatePostStatus(post, status)
            }
        )

        rvAdminPosts.layoutManager = LinearLayoutManager(requireContext())
        rvAdminPosts.adapter = adapter
    }

    private fun setupSearch() {
        etSearchPosts.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim().orEmpty()
                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilterChips() {
        chipAllPosts.setOnClickListener {
            selectedFilter = PostFilter.ALL
            updateChipUi()
            applyFilters()
        }

        chipOpenPosts.setOnClickListener {
            selectedFilter = PostFilter.OPEN
            updateChipUi()
            applyFilters()
        }

        chipHiddenPosts.setOnClickListener {
            selectedFilter = PostFilter.HIDDEN
            updateChipUi()
            applyFilters()
        }

        chipClosedPosts.setOnClickListener {
            selectedFilter = PostFilter.CLOSED
            updateChipUi()
            applyFilters()
        }

        chipRemovedPosts.setOnClickListener {
            selectedFilter = PostFilter.REMOVED
            updateChipUi()
            applyFilters()
        }

        updateChipUi()
    }

    private fun setupRefresh() {
        tvRefreshPosts.setOnClickListener {
            loadPosts()
        }
    }

    private fun loadPosts() {
        showLoadingState()

        postsListener?.let {
            db.child("Posts").removeEventListener(it)
        }

        postsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allPosts.clear()

                for (postSnapshot in snapshot.children) {
                    allPosts.add(parsePost(postSnapshot))
                }

                allPosts.sortByDescending { it.jobId }

                updateChipLabels()
                applyFilters()
            }

            override fun onCancelled(error: DatabaseError) {
                progressPosts.visibility = View.GONE
                tvEmptyPosts.text = "Failed to load posts"

                Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        db.child("Posts").addValueEventListener(postsListener as ValueEventListener)
    }

    private fun parsePost(snapshot: DataSnapshot): Post {
        return Post(
            postId = snapshot.key.orEmpty(),
            jobId = snapshot.getIntValue("jobId"),
            userId = snapshot.getStringValue("userId"),
            title = snapshot.getStringValue("title"),
            location = snapshot.getStringValue("location"),
            studentClass = snapshot.getStringValue("studentClass"),
            time = snapshot.getStringValue("time"),
            subjects = snapshot.getStringValue("subjects"),
            salary = snapshot.getIntValue("salary"),
            days = snapshot.getIntValue("days"),
            studentGender = snapshot.getStringValue("studentGender"),
            tutorGender = firstNonBlank(
                snapshot.getStringValue("preferredTutorGender"),
                snapshot.getStringValue("tutorGender")
            ),
            description = snapshot.getStringValue("description"),
            postedDate = snapshot.getStringValue("postedDate"),
            status = normalizeStatus(snapshot.getStringValue("status"))
        )
    }

    private fun applyFilters() {
        val query = searchQuery.lowercase(Locale.ROOT)

        val filteredPosts = allPosts.filter { post ->
            matchesSelectedFilter(post) && matchesSearchQuery(post, query)
        }

        adapter.submitList(filteredPosts)

        progressPosts.visibility = View.GONE

        tvPostsTotal.text = "${formatNumber(allPosts.size)} Total"
        tvPostResultCount.text =
            "Showing ${formatNumber(filteredPosts.size)} of ${formatNumber(allPosts.size)} results"

        if (filteredPosts.isEmpty()) {
            emptyPostsLayout.visibility = View.VISIBLE
            tvEmptyPosts.text = if (allPosts.isEmpty()) {
                "No posts found"
            } else {
                "No matching posts found"
            }
        } else {
            emptyPostsLayout.visibility = View.GONE
        }
    }

    private fun matchesSelectedFilter(post: Post): Boolean {
        val status = normalizeStatus(post.status)

        return when (selectedFilter) {
            PostFilter.ALL -> true
            PostFilter.OPEN -> status == "open"
            PostFilter.HIDDEN -> status == "hidden"
            PostFilter.CLOSED -> status == "closed"
            PostFilter.REMOVED -> status == "removed"
        }
    }

    private fun matchesSearchQuery(post: Post, query: String): Boolean {
        if (query.isBlank()) return true

        return post.title.lowercase(Locale.ROOT).contains(query) ||
                post.subjects.lowercase(Locale.ROOT).contains(query) ||
                post.location.lowercase(Locale.ROOT).contains(query) ||
                post.studentClass.lowercase(Locale.ROOT).contains(query) ||
                post.studentGender.lowercase(Locale.ROOT).contains(query) ||
                post.tutorGender.lowercase(Locale.ROOT).contains(query) ||
                post.description.lowercase(Locale.ROOT).contains(query) ||
                post.userId.lowercase(Locale.ROOT).contains(query) ||
                post.jobId.toString().contains(query)
    }

    private fun updatePostStatus(post: Post, newStatus: String) {
        val normalizedStatus = normalizeStatus(newStatus)

        val updates = hashMapOf<String, Any?>()
        updates["Posts/${post.postId}/status"] = normalizedStatus

        val auditId = db.child("AuditLogs").push().key
        if (auditId != null) {
            updates["AuditLogs/$auditId"] = mapOf(
                "action" to "POST_STATUS_CHANGED",
                "postId" to post.postId,
                "jobId" to post.jobId,
                "userId" to post.userId,
                "oldStatus" to post.status,
                "newStatus" to normalizedStatus,
                "timestamp" to ServerValue.TIMESTAMP
            )
        }

        db.updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Post marked as ${displayStatus(normalizedStatus)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "Failed to update post",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateChipLabels() {
        tvChipAllPosts.text = "All Posts (${formatNumber(allPosts.size)})"
        tvChipOpenPosts.text =
            "Open (${formatNumber(allPosts.count { normalizeStatus(it.status) == "open" })})"
        tvChipHiddenPosts.text =
            "Hidden (${formatNumber(allPosts.count { normalizeStatus(it.status) == "hidden" })})"
        tvChipClosedPosts.text =
            "Closed (${formatNumber(allPosts.count { normalizeStatus(it.status) == "closed" })})"
        tvChipRemovedPosts.text =
            "Removed (${formatNumber(allPosts.count { normalizeStatus(it.status) == "removed" })})"
    }

    private fun updateChipUi() {
        setChipState(chipAllPosts, tvChipAllPosts, selectedFilter == PostFilter.ALL)
        setChipState(chipOpenPosts, tvChipOpenPosts, selectedFilter == PostFilter.OPEN)
        setChipState(chipHiddenPosts, tvChipHiddenPosts, selectedFilter == PostFilter.HIDDEN)
        setChipState(chipClosedPosts, tvChipClosedPosts, selectedFilter == PostFilter.CLOSED)
        setChipState(chipRemovedPosts, tvChipRemovedPosts, selectedFilter == PostFilter.REMOVED)
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
        emptyPostsLayout.visibility = View.VISIBLE
        progressPosts.visibility = View.VISIBLE
        tvEmptyPosts.text = "Loading posts"
    }

    private fun normalizeStatus(status: String): String {
        return when (status.trim().lowercase(Locale.ROOT)) {
            "", "active", "opened" -> "open"
            "open" -> "open"
            "closed" -> "closed"
            "hidden" -> "hidden"
            "removed", "deleted" -> "removed"
            else -> status.trim().lowercase(Locale.ROOT)
        }
    }

    private fun displayStatus(status: String): String {
        return when (status) {
            "open" -> "Open"
            "closed" -> "Closed"
            "hidden" -> "Hidden"
            "removed" -> "Removed"
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

    private fun formatNumber(value: Int): String {
        return NumberFormat.getIntegerInstance(Locale.US).format(value)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        postsListener?.let {
            db.child("Posts").removeEventListener(it)
        }

        postsListener = null
        super.onDestroyView()
    }

    private enum class PostFilter {
        ALL,
        OPEN,
        HIDDEN,
        CLOSED,
        REMOVED
    }
}