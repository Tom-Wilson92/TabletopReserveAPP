package com.example.tabletopreserve

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: NotificationsAdapter
    private val notifications = mutableListOf<Map<String, Any>>()

    // Tag for logging
    companion object {
        private const val TAG = "NotificationsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        // Initialize views
        recyclerView = view.findViewById(R.id.notifications_recycler_view)
        emptyView = view.findViewById(R.id.empty_view)

        // Set up RecyclerView
        adapter = NotificationsAdapter(notifications) { notification ->
            onNotificationClick(notification)
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Load notifications
        loadNotifications()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }

    private fun loadNotifications() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            showEmptyState(getString(R.string.login_to_view_notifications))
            return
        }

        // Show loading state
        if (notifications.isEmpty()) {
            emptyView.text = getString(R.string.loading_notifications)
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting to load notifications for user: $userId")

                // Get notifications - now we're getting ALL notifications
                // without filtering by followed shops
                val db = FirebaseFirestore.getInstance()

                // Query all notifications, ordered by creation date (newest first)
                val notificationsQuery = db.collection("Notifications")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(20)

                Log.d(TAG, "Executing notification query")
                val notificationsSnapshot = notificationsQuery.get().await()
                Log.d(TAG, "Query returned ${notificationsSnapshot.size()} notifications")

                // Clear and update the list
                notifications.clear()

                notificationsSnapshot.documents.forEach { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["id"] = doc.id
                    notifications.add(data)
                    Log.d(TAG, "Added notification: ${doc.id}, title: ${data["title"]}")
                }

                // Update UI
                if (notifications.isEmpty()) {
                    Log.d(TAG, "No notifications found, showing empty state")
                    showEmptyState(getString(R.string.no_notifications))
                } else {
                    Log.d(TAG, "Showing ${notifications.size} notifications")
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading notifications", e)
                showEmptyState(getString(R.string.error_loading_notifications))
            }
        }
    }

    private fun showEmptyState(message: String) {
        emptyView.text = message
        emptyView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun onNotificationClick(notification: Map<String, Any>) {
        // Track notification open
        val notificationId = notification["id"] as? String ?: return
        lifecycleScope.launch {
            try {
                NotificationManager.trackNotificationOpen(notificationId)
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking notification open", e)
            }
        }

        // Handle navigation based on notification type
        val shopId = notification["shopId"] as? String ?: return
        val type = notification["type"] as? String ?: "update"

        when (type) {
            "event" -> {
                // Navigate to shop events
                val intent = Intent(context, ShopDetailActivity::class.java).apply {
                    putExtra("shop_id", shopId)
                    putExtra("open_tab", "events")
                }
                startActivity(intent)
            }
            "promo" -> {
                // Navigate to shop details
                val intent = Intent(context, ShopDetailActivity::class.java).apply {
                    putExtra("shop_id", shopId)
                }
                startActivity(intent)
            }
            else -> {
                // Navigate to shop details
                val intent = Intent(context, ShopDetailActivity::class.java).apply {
                    putExtra("shop_id", shopId)
                }
                startActivity(intent)
            }
        }
    }

    private class NotificationsAdapter(
        private val notifications: List<Map<String, Any>>,
        private val onItemClick: (Map<String, Any>) -> Unit
    ) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.notification_title)
            val message: TextView = view.findViewById(R.id.notification_message)
            val shopName: TextView = view.findViewById(R.id.shop_name)
            val timestamp: TextView = view.findViewById(R.id.notification_time)
            val typeIcon: View = view.findViewById(R.id.notification_type_icon)
            val typeText: TextView = view.findViewById(R.id.notification_type_text)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val notification = notifications[position]

            // Set basic notification info
            holder.title.text = notification["title"] as? String ?: "Notification"
            holder.message.text = notification["message"] as? String ?: ""
            holder.shopName.text = notification["shopName"] as? String ?: "Shop"

            // Format timestamp
            val timestamp = notification["createdAt"] as? com.google.firebase.Timestamp
            val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            holder.timestamp.text = if (timestamp != null) {
                dateFormat.format(timestamp.toDate())
            } else {
                "Recently"
            }

            // Set notification type
            val type = notification["type"] as? String ?: "update"

            // Set icon and text based on type
            when (type) {
                "event" -> {
                    holder.typeIcon.setBackgroundResource(R.drawable.ic_event_background)
                    holder.typeText.text = holder.itemView.context.getString(R.string.notification_type_event)
                }
                "promo" -> {
                    holder.typeIcon.setBackgroundResource(R.drawable.ic_promo_background)
                    holder.typeText.text = holder.itemView.context.getString(R.string.notification_type_promo)
                }
                else -> {
                    holder.typeIcon.setBackgroundResource(R.drawable.ic_update_background)
                    holder.typeText.text = holder.itemView.context.getString(R.string.notification_type_update)
                }
            }

            // Set click listener
            holder.itemView.setOnClickListener {
                onItemClick(notification)
            }
        }

        override fun getItemCount() = notifications.size
    }
}