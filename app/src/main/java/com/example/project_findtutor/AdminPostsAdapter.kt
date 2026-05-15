package com.example.project_findtutor

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.util.Locale

class AdminPostsAdapter (private val onCloseClick: (Post) -> Unit,private val onHideClick: (Post) -> Unit, private val onRemoveClick: (Post) -> Unit, private val onStatusMenuClick: (Post, String) -> Unit
) : RecyclerView.Adapter<AdminPostsAdapter.AdminPostViewHolder>() {

    private val posts = mutableListOf<Post>()

    fun submitList(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminPostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_post, parent, false)

        return AdminPostViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminPostViewHolder, position: Int) {
        holder.bind(
            post = posts[position],
            onCloseClick = onCloseClick,
            onHideClick = onHideClick,
            onRemoveClick = onRemoveClick,
            onStatusMenuClick = onStatusMenuClick
        )
    }

    override fun getItemCount(): Int = posts.size

    class AdminPostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvPostTitle: TextView = itemView.findViewById(R.id.tvPostTitle)
        private val tvPostSubjects: TextView = itemView.findViewById(R.id.tvPostSubjects)
        private val tvPostStatus: TextView = itemView.findViewById(R.id.tvPostStatus)
        private val btnMorePostOptions: TextView = itemView.findViewById(R.id.btnMorePostOptions)

        private val tvPostSalary: TextView = itemView.findViewById(R.id.tvPostSalary)
        private val tvPostLocation: TextView = itemView.findViewById(R.id.tvPostLocation)
        private val tvPostStudentClass: TextView = itemView.findViewById(R.id.tvPostStudentClass)
        private val tvPostSchedule: TextView = itemView.findViewById(R.id.tvPostSchedule)
        private val tvPostStudentGender: TextView = itemView.findViewById(R.id.tvPostStudentGender)
        private val tvPostPreferredTutorGender: TextView =
            itemView.findViewById(R.id.tvPostPreferredTutorGender)
        private val tvPostDescription: TextView = itemView.findViewById(R.id.tvPostDescription)

        private val btnClosePost: MaterialButton = itemView.findViewById(R.id.btnClosePost)
        private val btnHidePost: MaterialButton = itemView.findViewById(R.id.btnHidePost)
        private val btnRemovePost: MaterialButton = itemView.findViewById(R.id.btnRemovePost)

        fun bind(
            post: Post,
            onCloseClick: (Post) -> Unit,
            onHideClick: (Post) -> Unit,
            onRemoveClick: (Post) -> Unit,
            onStatusMenuClick: (Post, String) -> Unit
        ) {
            tvPostTitle.text = post.title.ifBlank { "Untitled Post" }
            tvPostSubjects.text = post.subjects.ifBlank { "Subject not provided" }

            tvPostSalary.text = if (post.salary > 0) {
                "${post.salary} BDT"
            } else {
                "Salary not set"
            }

            tvPostLocation.text = post.location.ifBlank { "Location not set" }
            tvPostStudentClass.text = post.studentClass.ifBlank { "Class not set" }

            tvPostSchedule.text = buildScheduleText(post)
            tvPostStudentGender.text = post.studentGender.ifBlank { "Not set" }
            tvPostPreferredTutorGender.text = getPreferredTutorGender(post).ifBlank { "Any" }
            tvPostDescription.text = post.description.ifBlank { "No description provided" }

            val status = normalizeStatus(post.status)
            tvPostStatus.text = displayStatus(status)
            styleStatusBadge(tvPostStatus, status)

            btnClosePost.setOnClickListener {
                onCloseClick(post)
            }

            btnHidePost.setOnClickListener {
                onHideClick(post)
            }

            btnRemovePost.setOnClickListener {
                onRemoveClick(post)
            }

            btnMorePostOptions.setOnClickListener {
                showPostMenu(post, it, onStatusMenuClick)
            }
        }

        private fun showPostMenu(
            post: Post,
            anchor: View,
            onStatusMenuClick: (Post, String) -> Unit
        ) {
            val popup = PopupMenu(anchor.context, anchor)

            popup.menu.add("Mark Open")
            popup.menu.add("Close Post")
            popup.menu.add("Hide Post")
            popup.menu.add("Remove Post")

            popup.setOnMenuItemClickListener { item ->
                when (item.title.toString()) {
                    "Mark Open" -> onStatusMenuClick(post, "open")
                    "Close Post" -> onStatusMenuClick(post, "closed")
                    "Hide Post" -> onStatusMenuClick(post, "hidden")
                    "Remove Post" -> onStatusMenuClick(post, "removed")
                }
                true
            }

            popup.show()
        }

        private fun buildScheduleText(post: Post): String {
            val daysText = if (post.days > 0) {
                "${post.days} days"
            } else {
                "Days not set"
            }

            val timeText = post.time.ifBlank { "Time not set" }

            return "$daysText, $timeText"
        }

        private fun getPreferredTutorGender(post: Post): String {
            return try {
                val field = post.javaClass.getDeclaredField("preferredTutorGender")
                field.isAccessible = true
                field.get(post)?.toString().orEmpty()
            } catch (e: Exception) {
                try {
                    val field = post.javaClass.getDeclaredField("tutorGender")
                    field.isAccessible = true
                    field.get(post)?.toString().orEmpty()
                } catch (e: Exception) {
                    ""
                }
            }
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

        private fun styleStatusBadge(textView: TextView, status: String) {
            val textColor: String
            val bgColor: String

            when (status) {
                "open" -> {
                    textColor = "#166534"
                    bgColor = "#DCFCE7"
                }

                "hidden" -> {
                    textColor = "#92400E"
                    bgColor = "#FEF3C7"
                }

                "closed" -> {
                    textColor = "#374151"
                    bgColor = "#E5E7EB"
                }

                "removed" -> {
                    textColor = "#991B1B"
                    bgColor = "#FEE2E2"
                }

                else -> {
                    textColor = "#374151"
                    bgColor = "#E5E7EB"
                }
            }

            textView.setTextColor(Color.parseColor(textColor))
            textView.background = roundedBackground(bgColor, 11)
        }

        private fun roundedBackground(color: String, radiusDp: Int): GradientDrawable {
            val density = itemView.resources.displayMetrics.density

            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor(color))
                cornerRadius = radiusDp * density
            }
        }
    }
}