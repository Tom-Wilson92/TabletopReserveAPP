package com.example.tabletopreserve

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date
import java.util.HashMap

class ReservationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var filterTabLayout: TabLayout
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val bookings = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: BookingAdapter
    private var isPastBookings = false
    private var currentFilterTab = 0 // 0 = All, 1 = Tables, 2 = Events
    private var isInitialLoad = true

    companion object {
        private const val TAG = "ReservationsFragment"
        private const val TYPE_TABLE = "table"
        private const val TYPE_EVENT = "event"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_reservations, container, false)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize views
        recyclerView = view.findViewById(R.id.reservations_recycler_view)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyView = view.findViewById(R.id.empty_view)
        tabLayout = view.findViewById(R.id.tab_layout)
        filterTabLayout = view.findViewById(R.id.booking_type_tabs)

        // Set up RecyclerView
        adapter = BookingAdapter(
            bookings,
            { booking -> showBookingDetails(booking) },
            { booking -> showCancelDialog(booking) }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Set up tab selection listener for upcoming/past
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isPastBookings = tab?.position == 1
                loadBookings()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Add booking type selector (All, Tables, Events)
        filterTabLayout.addTab(filterTabLayout.newTab().setText("All"))
        filterTabLayout.addTab(filterTabLayout.newTab().setText("Tables"))
        filterTabLayout.addTab(filterTabLayout.newTab().setText("Events"))

        // Set up filter tab selection listener
        filterTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilterTab = tab?.position ?: 0
                loadBookings()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Load upcoming bookings by default - will happen in onResume
        return view
    }

    override fun onResume() {
        super.onResume()
        // Load bookings when the fragment becomes visible
        loadBookings()
    }

    private fun loadBookings() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User not logged in
            emptyView.text = "Please login to view your bookings"
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            return
        }

        Log.d(TAG, "Loading bookings for user: ${currentUser.uid}")

        progressBar.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        recyclerView.visibility = View.GONE

        // Get current time
        val now = Date()

        // Clear existing bookings
        bookings.clear()

        // Make sure to notify the adapter that data was cleared
        adapter.notifyDataSetChanged()

        // List to track loading state
        val loadingTasks = mutableListOf<Boolean>()

        // Table reservations and event bookings will be loaded in parallel
        // We'll add them both to the bookings list and then sort by date at the end

        // Load table reservations if filter is All or Tables
        if (currentFilterTab == 0 || currentFilterTab == 1) {
            loadingTasks.add(true)
            loadTableReservations(currentUser.uid, now) { loaded ->
                val index = loadingTasks.indexOf(true)
                if (index >= 0) {
                    loadingTasks[index] = false
                    checkAndDisplayBookings(loadingTasks)
                }
            }
        }

        // Load event bookings if filter is All or Events
        if (currentFilterTab == 0 || currentFilterTab == 2) {
            loadingTasks.add(true)
            loadEventBookings(currentUser.uid, now) { loaded ->
                val index = loadingTasks.indexOf(true)
                if (index >= 0) {
                    loadingTasks[index] = false
                    checkAndDisplayBookings(loadingTasks)
                }
            }
        }

        // If no loading tasks were added (shouldn't happen), update UI
        if (loadingTasks.isEmpty()) {
            checkAndDisplayBookings(loadingTasks)
        }
    }

    private fun checkAndDisplayBookings(loadingTasks: List<Boolean>) {
        Log.d(TAG, "Checking and displaying bookings. Loading tasks: $loadingTasks")
        // If all loading tasks are complete, we can display the results
        if (!loadingTasks.contains(true)) {
            progressBar.visibility = View.GONE

            if (bookings.isEmpty()) {
                // No bookings found
                emptyView.text = if (isPastBookings)
                    "No past bookings" else "No upcoming bookings"
                if (currentFilterTab == 1) emptyView.text = "${emptyView.text} for tables"
                if (currentFilterTab == 2) emptyView.text = "${emptyView.text} for events"

                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE

                Log.d(TAG, "No bookings to display")
            } else {
                // Bookings found
                emptyView.visibility = View.GONE

                // Sort bookings by date
                val sortedBookings = bookings.sortedWith(compareBy { booking ->
                    val timestamp = when (booking["bookingType"]) {
                        TYPE_TABLE -> booking["reservationTime"] as? com.google.firebase.Timestamp
                        TYPE_EVENT -> booking["eventDate"] as? com.google.firebase.Timestamp
                        else -> null
                    }
                    val date = timestamp?.toDate()

                    // Log for debugging
                    Log.d(TAG, "Sorting booking: ${booking["id"]}, Date: $date")

                    // Return date or min/max value based on sort direction
                    if (isPastBookings) {
                        date ?: Date(0) // Oldest possible date for null dates in past view
                    } else {
                        date ?: Date(Long.MAX_VALUE) // Future date for null dates in upcoming view
                    }
                })

                // Apply proper sorting order
                val finalSortedBookings = if (isPastBookings) {
                    // Reverse order for past bookings to show most recent first
                    sortedBookings.reversed()
                } else {
                    // Keep ascending order for upcoming bookings
                    sortedBookings
                }

                // Update the list and adapter
                bookings.clear()
                bookings.addAll(finalSortedBookings)

                Log.d(TAG, "Displaying ${bookings.size} bookings")

                // Show the RecyclerView
                recyclerView.visibility = View.VISIBLE

                // Notify adapter of data changes
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun loadTableReservations(userId: String, now: Date, callback: (Boolean) -> Unit) {
        // Create the base query for table reservations
        var query = db.collection("Reservations")
            .whereEqualTo("userId", userId)

        Log.d(TAG, "Querying table reservations with userId: $userId")

        // Add time filter based on selected tab
        if (isPastBookings) {
            query = query.whereLessThan("reservationTime", now)
                .orderBy("reservationTime", Query.Direction.DESCENDING)
            Log.d(TAG, "Filtering for past reservations")
        } else {
            query = query.whereGreaterThanOrEqualTo("reservationTime", now)
                .orderBy("reservationTime", Query.Direction.ASCENDING)
            Log.d(TAG, "Filtering for upcoming reservations")
        }

        // Execute the query ONCE
        query.get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Table reservations query returned ${documents.size()} documents")

                // Process each reservation separately to fetch shop names
                if (!documents.isEmpty) {
                    val reservationCount = documents.size()
                    var processedCount = 0

                    // Process each table reservation
                    for (document in documents) {
                        val reservationData = document.data.toMutableMap()
                        reservationData["id"] = document.id
                        reservationData["bookingType"] = TYPE_TABLE

                        // Get shop name for this reservation
                        val shopId = reservationData["shopId"] as? String ?: ""
                        if (shopId.isNotEmpty()) {
                            db.collection("Stores").document(shopId).get()
                                .addOnSuccessListener { shopDoc ->
                                    val shopName = if (shopDoc.exists())
                                        shopDoc.getString("storeName") ?: "Unknown Shop"
                                    else
                                        "Unknown Shop"

                                    reservationData["shopName"] = shopName

                                    // Add to bookings list
                                    bookings.add(reservationData)

                                    // Track processed documents
                                    processedCount++

                                    // If all processed, call the callback
                                    if (processedCount >= reservationCount) {
                                        callback(true)
                                    } else {
                                        // Update the adapter without waiting for all to complete
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error loading shop info", e)
                                    reservationData["shopName"] = "Unknown Shop"
                                    bookings.add(reservationData)

                                    // Track processed documents
                                    processedCount++

                                    // If all processed, call the callback
                                    if (processedCount >= reservationCount) {
                                        callback(true)
                                    } else {
                                        // Update the adapter without waiting for all to complete
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                        } else {
                            reservationData["shopName"] = "Unknown Shop"
                            bookings.add(reservationData)

                            // Track processed documents
                            processedCount++

                            // If all processed, call the callback
                            if (processedCount >= reservationCount) {
                                callback(true)
                            } else {
                                // Update the adapter without waiting for all to complete
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                } else {
                    // No reservations found, call the callback immediately
                    callback(true)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error loading table reservations", exception)
                callback(true) // Mark as completed even if it failed
            }
    }

    private fun loadEventBookings(userId: String, now: Date, callback: (Boolean) -> Unit) {
        // Create the base query for event bookings
        var query = db.collection("EventBookings")
            .whereEqualTo("userId", userId)

        Log.d(TAG, "Querying event bookings with userId: $userId")

        // Add time filter based on selected tab
        if (isPastBookings) {
            query = query.whereLessThan("eventDate", now)
                .orderBy("eventDate", Query.Direction.DESCENDING)
            Log.d(TAG, "Filtering for past event bookings")
        } else {
            query = query.whereGreaterThanOrEqualTo("eventDate", now)
                .orderBy("eventDate", Query.Direction.ASCENDING)
            Log.d(TAG, "Filtering for upcoming event bookings")
        }

        // Execute the query
        query.get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Event bookings query returned ${documents.size()} documents")

                if (!documents.isEmpty) {
                    val bookingCount = documents.size()
                    var processedCount = 0

                    // Process each event booking
                    for (document in documents) {
                        val bookingData = document.data.toMutableMap()
                        bookingData["id"] = document.id
                        bookingData["bookingType"] = TYPE_EVENT

                        // The shop name might be stored directly in the booking
                        // or we might need to fetch it
                        val shopName = bookingData["shopName"] as? String
                        if (shopName == null) {
                            val shopId = bookingData["shopId"] as? String ?: ""
                            if (shopId.isNotEmpty()) {
                                db.collection("Stores").document(shopId).get()
                                    .addOnSuccessListener { shopDoc ->
                                        val name = if (shopDoc.exists())
                                            shopDoc.getString("storeName") ?: "Unknown Shop"
                                        else
                                            "Unknown Shop"

                                        bookingData["shopName"] = name
                                        bookings.add(bookingData)

                                        // Track processed documents
                                        processedCount++

                                        // If all processed, call the callback
                                        if (processedCount >= bookingCount) {
                                            callback(true)
                                        } else {
                                            // Update the adapter without waiting for all to complete
                                            adapter.notifyDataSetChanged()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Error loading shop info", e)
                                        bookingData["shopName"] = "Unknown Shop"
                                        bookings.add(bookingData)

                                        // Track processed documents
                                        processedCount++

                                        // If all processed, call the callback
                                        if (processedCount >= bookingCount) {
                                            callback(true)
                                        } else {
                                            // Update the adapter without waiting for all to complete
                                            adapter.notifyDataSetChanged()
                                        }
                                    }
                            } else {
                                bookingData["shopName"] = "Unknown Shop"
                                bookings.add(bookingData)

                                // Track processed documents
                                processedCount++

                                // If all processed, call the callback
                                if (processedCount >= bookingCount) {
                                    callback(true)
                                } else {
                                    // Update the adapter without waiting for all to complete
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        } else {
                            bookings.add(bookingData)

                            // Track processed documents
                            processedCount++

                            // If all processed, call the callback
                            if (processedCount >= bookingCount) {
                                callback(true)
                            } else {
                                // Update the adapter without waiting for all to complete
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                } else {
                    // No event bookings found, call the callback immediately
                    callback(true)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error loading event bookings", exception)
                callback(true) // Mark as completed even if it failed
            }
    }

    private fun showBookingDetails(booking: Map<String, Any>) {
        val bookingType = booking["bookingType"] as? String ?: TYPE_TABLE

        if (bookingType == TYPE_TABLE) {
            showTableReservationDetails(booking)
        } else {
            showEventBookingDetails(booking)
        }
    }

    private fun showTableReservationDetails(reservation: Map<String, Any>) {
        // Get details from reservation
        val shopName = reservation["shopName"] as? String ?: "Unknown Shop"
        val tableNumber = reservation["tableNumber"] as? Long ?: 0
        val status = reservation["status"] as? String ?: "pending"
        val partySize = reservation["partySize"] as? Long ?: 1
        val notes = reservation["notes"] as? String ?: ""
        val timestamp = reservation["reservationTime"] as? com.google.firebase.Timestamp
        val reservationDate = timestamp?.toDate() ?: Date()

        // Format date and time
        val dateFormat =
            java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())

        val formattedDate = dateFormat.format(reservationDate)
        val formattedTime = timeFormat.format(reservationDate)

        // Duration and end time
        val duration = reservation["duration"] as? Long ?: 2
        val endTime = Date(reservationDate.time + (duration * 60 * 60 * 1000))
        val formattedEndTime = timeFormat.format(endTime)

        // Create details string
        val details = StringBuilder()
        details.append("Table Reservation\n\n")
        details.append("Shop: $shopName\n\n")
        details.append("Date: $formattedDate\n")
        details.append("Time: $formattedTime - $formattedEndTime\n")
        details.append("Table: $tableNumber\n")
        details.append("Party Size: $partySize people\n")
        details.append("Status: $status\n")

        if (notes.isNotEmpty()) {
            details.append("\nNotes: $notes\n")
        }

        // Show dialog with details
        AlertDialog.Builder(requireContext())
            .setTitle("Reservation Details")
            .setMessage(details.toString())
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showEventBookingDetails(booking: Map<String, Any>) {
        // Get details from event booking
        val shopName = booking["shopName"] as? String ?: "Unknown Shop"
        val eventName = booking["eventName"] as? String ?: "Unknown Event"
        val status = booking["status"] as? String ?: "confirmed"
        val participants = booking["participants"] as? Long ?: 1
        val notes = booking["notes"] as? String ?: ""
        val timestamp = booking["eventDate"] as? com.google.firebase.Timestamp
        val eventDate = timestamp?.toDate() ?: Date()

        // Format date and time
        val dateFormat =
            java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault())
        val formattedDate = dateFormat.format(eventDate)

        // Get time string if available
        val timeString = booking["eventTimeString"] as? String ?: ""

        // Create details string
        val details = StringBuilder()
        details.append("Event Booking\n\n")
        details.append("Event: $eventName\n")
        details.append("Shop: $shopName\n\n")
        details.append("Date: $formattedDate\n")
        if (timeString.isNotEmpty()) {
            details.append("Time: $timeString\n")
        }
        details.append("Participants: $participants\n")
        details.append("Status: $status\n")

        if (notes.isNotEmpty()) {
            details.append("\nNotes: $notes\n")
        }

        // Show dialog with details
        AlertDialog.Builder(requireContext())
            .setTitle("Event Booking Details")
            .setMessage(details.toString())
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showCancelDialog(booking: Map<String, Any>) {
        val bookingId = booking["id"] as? String ?: return
        val bookingType = booking["bookingType"] as? String ?: TYPE_TABLE

        // Different messages based on booking type
        val message = if (bookingType == TYPE_TABLE) {
            "Are you sure you want to cancel this table reservation?"
        } else {
            "Are you sure you want to cancel this event booking?"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Booking")
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                cancelBooking(bookingId, bookingType)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelBooking(bookingId: String, bookingType: String) {
        progressBar.visibility = View.VISIBLE

        val updates = HashMap<String, Any>()
        updates["status"] = "cancelled"
        updates["cancelledAt"] = com.google.firebase.Timestamp.now()
        updates["cancelledBy"] = auth.currentUser?.uid ?: ""

        // Use different collections based on booking type
        val collection = if (bookingType == TYPE_TABLE) "Reservations" else "EventBookings"

        db.collection(collection).document(bookingId)
            .update(updates)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                val message = if (bookingType == TYPE_TABLE)
                    "Reservation cancelled" else "Event booking cancelled"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                loadBookings()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error cancelling booking", e)
            }
    }
}