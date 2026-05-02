package com.example.project_findtutor

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    lateinit var auth: FirebaseAuth
    lateinit var db: DatabaseReference
    lateinit var tvName: TextView
    lateinit var tvRating: TextView
    lateinit var tvQualification: TextView
    val  reminderList = mutableListOf<Meeting>()
    lateinit var rvReminders: RecyclerView

    lateinit var adapter: MeetingReminderAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseDatabase.getInstance().reference
        tvName = view.findViewById(R.id.tvName)
        tvRating = view.findViewById(R.id.tvRating)
        tvQualification = view.findViewById(R.id.tvQualification)
        rvReminders = view.findViewById(R.id.rvReminders)
        rvReminders.layoutManager = LinearLayoutManager(requireContext())

        adapter= MeetingReminderAdapter(reminderList){
            showMeetingDetails(it)
        }
        rvReminders.adapter = adapter

        loadReminders()

        val userId = auth.currentUser?.uid
        if(userId == null){
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        db.child("Tutors").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if(snapshot.exists()){
                        val name = snapshot.child("name").value.toString()?:"N/A"
                        val rating = snapshot.child("rating").value.toString()
                        val qualification = snapshot.child("qualification").value.toString()?:"N/A"

                        tvName.text = name
                        tvRating.text = rating
                        tvQualification.text = qualification

                    }else{
                        if(isAdded){
                            Toast.makeText(requireContext(),"Tutor id not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load date", Toast.LENGTH_SHORT).show()
                }

            })
    }

    fun loadReminders(){
        val tutorId = auth.currentUser?.uid?:return

        db.child("Meetings").orderByChild("tutorId").equalTo(tutorId)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    reminderList.clear()
                    for(data in snapshot.children){
                        val meeting = data.getValue(Meeting::class.java)
                        if(meeting != null && meeting.status == "accepted"){
                            meeting.meetingId = data.key?:""
                            reminderList.add(meeting)
                        }
                    }
                    reminderList.sortBy { parseDateTime(it) }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load reminders", Toast.LENGTH_SHORT).show()
                }

            })
    }

    fun parseDateTime(meeting: Meeting): Long{
        return try {
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val date = format.parse("${meeting.date} ${meeting.time}")
            date?.time ?: Long.MAX_VALUE
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }

    fun showMeetingDetails(meeting: Meeting) {
        val message = """
            Student: ${meeting.studentName}
            Phone: ${meeting.studentPhoneNumber}
            
            Date: ${meeting.date}
            Time: ${meeting.time}
            Location: ${meeting.location}
            
            Status: ${meeting.status.uppercase()}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Meeting Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}