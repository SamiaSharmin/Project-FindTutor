package com.example.project_findtutor

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminNotificationFragment : Fragment(R.layout.fragment_admin_notification) {
    private lateinit var db: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoAdminNotification: TextView
    private lateinit var tvUnreadAdminNotificationCount: TextView

    private val notificationList = mutableListOf<AdminNotification>()
    private lateinit var adapter: AdminNotificationAdapter
    private var notificationsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseDatabase.getInstance().reference
        recyclerView = view.findViewById(R.id.rvAdminNotifications)
        tvNoAdminNotification = view.findViewById(R.id.tvNoAdminNotification)
        tvUnreadAdminNotificationCount = view.findViewById(R.id.tvUnreadAdminNotificationCount)

        requireActivity().findViewById<TextView>(R.id.tvAdminDashboardTitle).text = "Admin Notifications"

        adapter = AdminNotificationAdapter(notificationList) { notification ->
            markNotificationAsRead(notification)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        loadAdminNotifications()
    }

    private fun loadAdminNotifications() {
        notificationsListener?.let {
            db.child(AdminNotificationHelper.NODE_ADMIN_NOTIFICATIONS).removeEventListener(it)
        }

        notificationsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                notificationList.clear()

                for (data in snapshot.children) {
                    val notification = data.getValue(AdminNotification::class.java)
                    if (notification != null) {
                        if (notification.notificationId.isEmpty()) {
                            notification.notificationId = data.key.orEmpty()
                        }
                        notificationList.add(notification)
                    }
                }

                notificationList.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()
                updateEmptyState()
                updateUnreadCount()
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                Toast.makeText(requireContext(), "Failed to load admin notifications: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        db.child(AdminNotificationHelper.NODE_ADMIN_NOTIFICATIONS)
            .addValueEventListener(notificationsListener as ValueEventListener)
    }

    private fun markNotificationAsRead(notification: AdminNotification) {
        if (notification.notificationId.isEmpty()) return

        db.child(AdminNotificationHelper.NODE_ADMIN_NOTIFICATIONS)
            .child(notification.notificationId)
            .child("isRead")
            .setValue(true)
            .addOnFailureListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to update notification", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateEmptyState() {
        if (notificationList.isEmpty()) {
            tvNoAdminNotification.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvNoAdminNotification.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateUnreadCount() {
        val unreadCount = notificationList.count { !it.isRead }
        tvUnreadAdminNotificationCount.text = "Unread: $unreadCount"
    }

    override fun onDestroyView() {
        notificationsListener?.let {
            db.child(AdminNotificationHelper.NODE_ADMIN_NOTIFICATIONS).removeEventListener(it)
        }
        notificationsListener = null
        super.onDestroyView()
    }

}