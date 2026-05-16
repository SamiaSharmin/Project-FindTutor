package com.example.project_findtutor

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class StudentNotificationFragment : Fragment(R.layout.fragment_student_notification) {

    lateinit var auth: FirebaseAuth
    lateinit var db: DatabaseReference
    lateinit var recyclerView: RecyclerView
    lateinit var tvEmpty: TextView

    val list = mutableListOf<NotificationModel>()
    private var notificationsListener: ValueEventListener? = null
    private var badgeListener: ValueEventListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvNotifications)
        tvEmpty = view.findViewById(R.id.tvNoNotification)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference

        recyclerView.adapter = StudentNotificationAdapter(
            list = list,
            onSetMeetingClick = { jobId, tutorId ->
                showMeetingDialog(jobId, tutorId)
            },
            onReviewClick = { notification ->
                showReviewDialog(notification)
            }
        )

        loadNotifications()
        updateBadgeCount()
    }

    fun loadNotifications(){
        val userId = auth.currentUser?.uid ?: return

        notificationsListener?.let {
            db.child("Notifications").child(userId).removeEventListener(it)
        }

        notificationsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                list.clear()

                for (data in snapshot.children) {
                    val notification = data.getValue(NotificationModel::class.java)
                    if (notification != null) {
                        list.add(notification)
                    }
                }

                list.sortByDescending { it.timestamp }
                recyclerView.adapter?.notifyDataSetChanged()

                if (list.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                Toast.makeText(requireContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show()
            }
        }

        db.child("Notifications").child(userId)
            .addValueEventListener(notificationsListener as ValueEventListener)
    }

    fun updateBadgeCount(){

        val studentId = auth.currentUser?.uid ?: return

        badgeListener?.let {
            db.child("Notifications").child(studentId).removeEventListener(it)
        }

        badgeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                var count = 0
                for (data in snapshot.children) {
                    val notification = data.getValue(NotificationModel::class.java)
                    if (notification != null && !notification.isRead) {
                        count++
                    }
                }

                (activity as? StudentDashboard)?.updateBadge(count)
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                Toast.makeText(requireContext(), "Failed to load notification count", Toast.LENGTH_SHORT).show()
            }
        }

        db.child("Notifications").child(studentId)
            .addValueEventListener(badgeListener as ValueEventListener)
    }

    fun showMeetingDialog(jobId:Int, tutorId:String){
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_set_meeting, null)

        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val btnPickDate = view.findViewById<Button>(R.id.btnPickDate)
        val btnPickTime = view.findViewById<Button>(R.id.btnPickTime)

        val calendar = Calendar.getInstance()
        var selectedDate = ""
        var selectedTime = ""

        btnPickDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDate = "$day/${month + 1}/$year"
                    btnPickDate.text = selectedDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnPickTime.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    selectedTime = String.format("%02d:%02d", hour, minute)
                    btnPickTime.text = selectedTime
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Set Meeting")
            .setView(view)
            .setPositiveButton("Confirm") { _, _ ->
                val location = etLocation.text.toString().trim()
                if (location.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                saveMeeting(jobId, tutorId, selectedDate, selectedTime, location)
            }
            .setNegativeButton("Cancel", null)
            .show()

    }

    fun saveMeeting(jobId: Int, tutorId: String, date:String, time:String, location:String){
        val meetingRef = db.child("Meetings")
        val meetingId = meetingRef.push().key ?: return
        val studentId = auth.currentUser?.uid ?: return

        db.child("Students").child(studentId).get()
            .addOnSuccessListener { snapshot ->
                val studentName = snapshot.child("name").value?.toString() ?: "Unknown"
                val studentPhoneNumber = snapshot.child("phoneNumber").value?.toString().orEmpty()

                val meeting = Meeting(
                    meetingId = meetingId,
                    jobId = jobId,
                    studentId = studentId,
                    studentName = studentName,
                    studentPhoneNumber = studentPhoneNumber,
                    tutorId = tutorId,
                    date = date,
                    time = time,
                    location = location,
                    status = "pending",
                    createdAt = System.currentTimeMillis()
                )

                meetingRef.child(meetingId).setValue(meeting)
                    .addOnSuccessListener {
                        AdminNotificationHelper.sendAdminNotification(
                            db = db,
                            title = "New meeting request",
                            message = "$studentName set a meeting request for Job ID $jobId with tutor ID $tutorId.",
                            type = AdminNotificationHelper.TYPE_MEETING_SET,
                            userId = studentId,
                            userRole = "student",
                            userName = studentName,
                            relatedId = meetingId,
                            relatedNode = "Meetings"
                        )

                        Toast.makeText(requireContext(), "Meeting set", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to set meeting", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load student data", Toast.LENGTH_SHORT).show()
            }

    }

    fun showReviewDialog(notification: NotificationModel) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_review, null)
        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar)
        val etComment = view.findViewById<EditText>(R.id.etComment)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val dialog = AlertDialog.Builder(requireContext()).setTitle("Review").setView(view).create()

        btnCancel.setOnClickListener { dialog.dismiss()  }

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating
            val comment = etComment.text.toString().trim()
            if(rating == 0f || comment.isEmpty()){
                Toast.makeText(requireContext(), "Please give a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveReview(notification.tutorId, rating, comment)
            dialog.dismiss()
        }
        dialog.show()

    }

    fun saveReview(tutorId: String, rating: Float, comment: String) {
        val studentId = auth.currentUser?.uid ?: return

        db.child("Students").child(studentId).child("name").get()
            .addOnSuccessListener { snapshot ->
                val studentName = snapshot.value?.toString().orEmpty()
                val reviewRef = db.child("Reviews").push()
                val reviewId = reviewRef.key ?: return@addOnSuccessListener

                val review = mapOf(
                    "studentId" to studentId,
                    "studentName" to studentName,
                    "tutorId" to tutorId,
                    "rating" to rating,
                    "reviewText" to comment,
                    "comment" to comment,
                    "moderationStatus" to "pending",
                    "timestamp" to System.currentTimeMillis()
                )

                reviewRef.setValue(review)
                    .addOnSuccessListener {
                        AdminNotificationHelper.sendAdminNotification(
                            db = db,
                            title = "New review submitted",
                            message = "$studentName gave a $rating star review to tutor ID $tutorId.",
                            type = AdminNotificationHelper.TYPE_REVIEW_GIVEN,
                            userId = studentId,
                            userRole = "student",
                            userName = studentName,
                            relatedId = reviewId,
                            relatedNode = "Reviews"
                        )

                        updateTutorRating(tutorId, rating)
                        Toast.makeText(requireContext(), "Review submitted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to submit review", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load student data", Toast.LENGTH_SHORT).show()
            }

    }

    fun updateTutorRating(tutorId: String,newRating: Float) {
        val tutorRef = db.child("Tutors").child(tutorId)

        tutorRef.get()
            .addOnSuccessListener { snapshot ->
                val currentRating = snapshot.child("rating").value.toString().toFloatOrNull() ?: 0f
                val totalReviews = snapshot.child("totalReviews").value.toString().toIntOrNull() ?: 0
                val updatedTotalReviews = totalReviews + 1
                val updatedRating = ((currentRating * totalReviews) + newRating) / updatedTotalReviews

                val updates = mapOf(
                    "rating" to updatedRating,
                    "totalReviews" to updatedTotalReviews
                )

                tutorRef.updateChildren(updates)
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to update tutor rating", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load tutor data", Toast.LENGTH_SHORT).show()
            }

    }

    override fun onDestroyView() {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            notificationsListener?.let {
                db.child("Notifications").child(userId).removeEventListener(it)
            }
            badgeListener?.let {
                db.child("Notifications").child(userId).removeEventListener(it)
            }
        }

        notificationsListener = null
        badgeListener = null

        super.onDestroyView()
    }




}