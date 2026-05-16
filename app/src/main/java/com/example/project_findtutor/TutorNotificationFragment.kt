package com.example.project_findtutor

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TutorNotificationFragment : Fragment(R.layout.fragment_tutor_notification) {

    lateinit var recyclerView: RecyclerView
    lateinit var tvEmpty: TextView
    lateinit var auth: FirebaseAuth
    lateinit var db: DatabaseReference
    private lateinit var adapter: TutorNotificationAdapter
    val meetingList = mutableListOf<Meeting>()
    private val notificationList = mutableListOf<NotificationModel>()

    private var meetingsListener: ValueEventListener? = null
    private var notificationsListener: ValueEventListener? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvNotifications)
        tvEmpty = view.findViewById(R.id.tvNoNotification)
        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference

//        adapter = TutorMeetingAdapter(meetingList){meeting ->
//            showMeetingDetailsDialog(meeting)
//        }
        adapter = TutorNotificationAdapter { meeting ->
            showMeetingDetailsDialog(meeting)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadMeetings()
        loadAdminNotifications()
        updateBadgeCount()

    }

    fun loadMeetings(){
        val tutorId = auth.currentUser?.uid ?: return

        meetingsListener?.let {
            db.child("Meetings").removeEventListener(it)
        }

        meetingsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                meetingList.clear()

                for (data in snapshot.children) {
                    val meeting = data.getValue(Meeting::class.java)
                    if (meeting != null) {
                        meeting.meetingId = data.key.orEmpty()
                        meetingList.add(meeting)
                    }
                }

                refreshNotificationList()
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                Toast.makeText(requireContext(), "Failed to load meetings", Toast.LENGTH_SHORT).show()
            }
        }

        db.child("Meetings")
            .orderByChild("tutorId")
            .equalTo(tutorId)
            .addValueEventListener(meetingsListener as ValueEventListener)
    }

    private fun loadAdminNotifications() {
        val tutorId = auth.currentUser?.uid ?: return

        notificationsListener?.let {
            db.child("Notifications").child(tutorId).removeEventListener(it)
        }

        notificationsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                notificationList.clear()

                for (data in snapshot.children) {
                    val notification = data.getValue(NotificationModel::class.java)
                    if (notification != null) {
                        notificationList.add(notification)
                    }
                }

                refreshNotificationList()
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                Toast.makeText(requireContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show()
            }
        }

        db.child("Notifications")
            .child(tutorId)
            .addValueEventListener(notificationsListener as ValueEventListener)
    }

    private fun refreshNotificationList() {
        val combinedList = mutableListOf<TutorNotificationListItem>()

        combinedList.addAll(meetingList.map { TutorNotificationListItem.MeetingItem(it) })
        combinedList.addAll(notificationList.map { TutorNotificationListItem.NotificationItem(it) })

        combinedList.sortByDescending { item ->
            when (item) {
                is TutorNotificationListItem.MeetingItem -> item.meeting.createdAt
                is TutorNotificationListItem.NotificationItem -> item.notification.timestamp
            }
        }

        adapter.submitList(combinedList)

        if (combinedList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateBadgeCount() {
        val tutorId = auth.currentUser?.uid ?: return

        db.child("Notifications").child(tutorId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                var unreadCount = 0
                for (data in snapshot.children) {
                    val notification = data.getValue(NotificationModel::class.java)
                    if (notification != null && !notification.isRead) {
                        unreadCount++
                    }
                }

                (activity as? TutorDashboard)?.updateBadge(unreadCount)
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                Toast.makeText(requireContext(), "Failed to load notification count", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun showMeetingDetailsDialog(meeting: Meeting){
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_meeting_details, null)

        val tvStudentName = view.findViewById<TextView>(R.id.tvStudentName)
        val tvPhoneNumber = view.findViewById<TextView>(R.id.tvPhoneNumber)
        val tvLocation = view.findViewById<TextView>(R.id.tvLocation)
        val tvDateTime = view.findViewById<TextView>(R.id.tvDateTime)
        val btnAccept = view.findViewById<Button>(R.id.btnAccept)
        val btnReject = view.findViewById<Button>(R.id.btnReject)

        tvStudentName.text = "Name: ${meeting.studentName}"
        tvPhoneNumber.text = "Phone Number: ${meeting.studentPhoneNumber}"
        tvLocation.text = "Location: ${meeting.location}"
        tvDateTime.text = "Date: ${meeting.date} at ${meeting.time}"

        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()

        btnAccept.setOnClickListener {
            updateMeetingAndNotify(meeting, "accepted")
            dialog.dismiss()
        }

        btnReject.setOnClickListener {
            updateMeetingAndNotify(meeting, "rejected")
            dialog.dismiss()
        }

        dialog.show()

    }

    fun updateMeetingAndNotify(meeting: Meeting, status:String) {
        db.child("Meetings").child(meeting.meetingId).child("status").setValue(status)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Meeting $status", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update meeting status", Toast.LENGTH_SHORT).show()
            }

        db.child("Tutors").child(meeting.tutorId).child("name").get().addOnSuccessListener { snapshot ->
            val tutorName = snapshot.value.toString() ?: "Unknown"
            val notificationRef = db.child("Notifications").child(meeting.studentId)
            val notificationId = notificationRef.push().key ?: return@addOnSuccessListener

            val message = if (status == "accepted") {
                "Meeting request is Accepted by $tutorName"
            } else {
                "Meeting request is Rejected by $tutorName"
            }

            val notification = NotificationModel(
                meeting.jobId,
                meeting.tutorId,
                tutorName,
                message,
                "meeting_status",
                System.currentTimeMillis(),
                false
            )

            notificationRef.child(notificationId).setValue(notification)
        }
    }

    override fun onDestroyView() {
        meetingsListener?.let {
            db.child("Meetings").removeEventListener(it)
        }

        val tutorId = auth.currentUser?.uid
        if (tutorId != null) {
            notificationsListener?.let {
                db.child("Notifications").child(tutorId).removeEventListener(it)
            }
        }

        meetingsListener = null
        notificationsListener = null

        super.onDestroyView()
    }

}