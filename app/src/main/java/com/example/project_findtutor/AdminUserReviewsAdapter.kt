package com.example.project_findtutor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminUserReviewsAdapter: RecyclerView.Adapter<AdminUserReviewsAdapter.ReviewViewHolder>() {

    private val reviews = mutableListOf<Review>()

    fun submitList(newReviews: List<Review>) {
        reviews.clear()
        reviews.addAll(newReviews)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user_review, parent, false)

        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount(): Int = reviews.size

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvReviewRating: TextView = itemView.findViewById(R.id.tvReviewRating)
        private val tvReviewDate: TextView = itemView.findViewById(R.id.tvReviewDate)
        private val tvReviewComment: TextView = itemView.findViewById(R.id.tvReviewComment)
        private val tvReviewStudentId: TextView = itemView.findViewById(R.id.tvReviewStudentId)
        private val tvReviewTutorId: TextView = itemView.findViewById(R.id.tvReviewTutorId)

        fun bind(review: Review) {
            tvReviewRating.text = "Rating ${String.format(Locale.US, "%.1f", review.rating)}"
            tvReviewDate.text = formatDate(review.timestamp)
            tvReviewComment.text = review.reviewText.ifBlank { "No review text provided" }
            tvReviewStudentId.text = review.studentId
            tvReviewTutorId.text = review.tutorId
        }

        private fun formatDate(timestamp: Long): String {
            if (timestamp <= 0L) return "Date unavailable"

            val millis = if (timestamp < 10_000_000_000L) {
                timestamp * 1000
            } else {
                timestamp
            }

            return SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(millis))
        }
    }
}