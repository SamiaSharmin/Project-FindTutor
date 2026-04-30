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

        loadNotifications()
        updateBadgeCount()
    }

    fun loadNotifications(){
        val userId = auth.currentUser?.uid?:return

        db.child("Notifications").child(userId).addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()
                if(snapshot.exists()){
                    for(data in snapshot.children){
                        val notification = data.getValue(NotificationModel::class.java)
                        if(notification != null){
                            list.add(notification)
                        }

                    }

                    if(list.isEmpty()){
                        tvEmpty.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }else{
                        tvEmpty.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.adapter = StudentNotificationAdapter(list){jobId, tutorId->
                            showMeetingDialog(jobId, tutorId)
                        }
                    }
                    recyclerView.adapter = StudentNotificationAdapter(list){jobId, tutorId->
                        showMeetingDialog(jobId, tutorId)
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun updateBadgeCount(){
        val studentId = auth.currentUser?.uid?:return

        db.child("Notifications").child(studentId).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                var count = 0
                for(data in snapshot.children){
                    val notification = data.getValue(NotificationModel::class.java)
                    if(notification != null && !notification.isRead){
                        count++
                    }
                }
                (activity as StudentDashboard).updateBadge(count)

            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show()
            }
        })
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

            DatePickerDialog(requireContext(), { _, year, month, day ->
                selectedDate = "$day/${month+1}/$year"
                btnPickDate.text = selectedDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnPickTime.setOnClickListener {

            TimePickerDialog(requireContext(), { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                btnPickTime.text = selectedTime
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }

        AlertDialog.Builder(requireContext()).setTitle("Set Meeting").setView(view)
            .setPositiveButton("Confirm"){_,_->
                val location = etLocation.text.toString().trim()
                if(location.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()){
                    Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                saveMeeting(jobId, tutorId, selectedDate, selectedTime, location)
            }
            .setNegativeButton("Cancel", null).show()

    }

    fun saveMeeting(jobId: Int, tutorId: String, date:String, time:String, location:String){
        db= FirebaseDatabase.getInstance().getReference("Meetings")
        val meetingId = db.push().key?:return

        val studentId = auth.currentUser?.uid?:return

        val meetingRef = FirebaseDatabase.getInstance().getReference("Meetings")

        FirebaseDatabase.getInstance().getReference("Students").child(studentId).child("name")
            .get().addOnSuccessListener { snapshot ->
                val studentName = snapshot.value.toString()?:"Unknown"
                val studentPhoneNumber = auth.currentUser?.phoneNumber?:""

                val meeting = Meeting(meetingId, jobId, studentId, studentName,studentPhoneNumber, tutorId, date, time, location,"pending", System.currentTimeMillis())

                meetingRef.child(meetingId).setValue(meeting)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Meeting set", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to set meeting", Toast.LENGTH_SHORT).show()
                    }
            }

//        val meeting = Meeting(meetingId, jobId, studentId, tutorId, date, time, location,"pending", System.currentTimeMillis())
//
//        db.child(meetingId).setValue(meeting)
//            .addOnSuccessListener {
//                Toast.makeText(requireContext(), "Meeting set", Toast.LENGTH_SHORT).show()
//            }
//            .addOnFailureListener {
//                Toast.makeText(requireContext(), "Failed to set meeting", Toast.LENGTH_SHORT).show()
//            }
    }


}