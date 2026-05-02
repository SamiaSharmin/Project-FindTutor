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
    val meetingList = mutableListOf<Meeting>()
    lateinit var adapter: TutorMeetingAdapter


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

        adapter = TutorMeetingAdapter(meetingList){meeting ->
            showMeetingDetailsDialog(meeting)
        }

        recyclerView.adapter = adapter

        loadMeetings()

    }

    fun loadMeetings(){
        val tutorId = auth.currentUser?.uid?:return

        db.child("Meetings").orderByChild("tutorId").equalTo(tutorId).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(!isAdded)return
                meetingList.clear()
                if(snapshot.exists()) {
                    for (data in snapshot.children) {

                        val meeting = data.getValue(Meeting::class.java)
                        if (meeting != null) {
                            meeting.meetingId=data.key?:""
                            meetingList.add(meeting)
                        }
                    }
                }

                if (meetingList.isEmpty()){
                    tvEmpty.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }else{
                    tvEmpty.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load meetings", Toast.LENGTH_SHORT).show()
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

}