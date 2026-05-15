package com.example.project_findtutor

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class AdminUsersAdapter(private val onUserClick: (AdminUser) -> Unit,private val onMoreClick: (AdminUser, View) -> Unit)
    :RecyclerView.Adapter<AdminUsersAdapter.AdminUserViewHolder>() {
    private val users = mutableListOf<AdminUser>()

    fun submitList(newUsers: List<AdminUser>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminUserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)

        return AdminUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminUserViewHolder, position: Int) {
        holder.bind(users[position],onUserClick, onMoreClick)
    }

    override fun getItemCount(): Int = users.size

    class AdminUserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cbSelectUser: CheckBox = itemView.findViewById(R.id.cbSelectUser)
        private val imgUserAvatar: ImageView = itemView.findViewById(R.id.imgUserAvatar)
        private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        private val tvUserStatus: TextView = itemView.findViewById(R.id.tvUserStatus)
        private val btnMoreUserOptions: TextView = itemView.findViewById(R.id.btnMoreUserOptions)
        private val tvUserRole: TextView = itemView.findViewById(R.id.tvUserRole)
        private val tvUserRating: TextView = itemView.findViewById(R.id.tvUserRating)
        private val tvUserEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        private val tvUserMeta: TextView = itemView.findViewById(R.id.tvUserMeta)

        fun bind(user: AdminUser,onUserClick: (AdminUser) -> Unit, onMoreClick: (AdminUser, View) -> Unit) {
            cbSelectUser.isChecked = false

            imgUserAvatar.setImageResource(R.drawable.ic_admin_profile)

            tvUserName.text = user.name
            tvUserStatus.text = getDisplayStatus(user.status)
            tvUserRole.text = getDisplayRole(user.role)
            tvUserEmail.text = user.email
            tvUserMeta.text = buildMetaText(user)

            if (user.rating > 0f) {
                tvUserRating.visibility = View.VISIBLE
                tvUserRating.text = "Rating ${String.format(Locale.US, "%.1f", user.rating)}"
            } else {
                tvUserRating.visibility = View.GONE
            }

            styleStatusBadge(tvUserStatus, user.status)
            styleRoleBadge(tvUserRole, user.role)
            styleRatingBadge(tvUserRating)

            btnMoreUserOptions.setOnClickListener {
                onMoreClick(user, it)
            }

            itemView.setOnClickListener {
                onUserClick(user)
            }
        }

        private fun buildMetaText(user: AdminUser): String {
            val parts = mutableListOf<String>()

            if (user.area.isNotBlank()) {
                parts.add(user.area)
            }

            if (user.phone.isNotBlank()) {
                parts.add(user.phone)
            }

            parts.add("Joined ${user.registeredAt}")

            return parts.joinToString("   ")
        }

        private fun getDisplayRole(role: String): String {
            return when (role) {
                "student" -> "Student"
                "tutor" -> "Tutor"
                else -> "User"
            }
        }

        private fun getDisplayStatus(status: String): String {
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

        private fun styleStatusBadge(textView: TextView, status: String) {
            val textColor: String
            val bgColor: String

            when (status) {
                "active" -> {
                    textColor = "#166534"
                    bgColor = "#DCFCE7"
                }

                "suspended" -> {
                    textColor = "#991B1B"
                    bgColor = "#FEE2E2"
                }

                "deactivated" -> {
                    textColor = "#374151"
                    bgColor = "#E5E7EB"
                }

                "pending" -> {
                    textColor = "#92400E"
                    bgColor = "#FEF3C7"
                }

                else -> {
                    textColor = "#374151"
                    bgColor = "#E5E7EB"
                }
            }

            textView.setTextColor(Color.parseColor(textColor))
            textView.background = roundedBackground(bgColor, 11)
        }

        private fun styleRoleBadge(textView: TextView, role: String) {
            val bgColor = when (role) {
                "student" -> "#FDBA3B"
                "tutor" -> "#17C964"
                else -> "#6B7280"
            }

            textView.setTextColor(Color.WHITE)
            textView.background = roundedBackground(bgColor, 9)
        }

        private fun styleRatingBadge(textView: TextView) {
            textView.setTextColor(Color.parseColor("#92400E"))
            textView.background = roundedBackground("#FEF3C7", 9)
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