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
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val reservations = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: ReservationAdapter
    private var isPastReservations = false

    companion object {
        private const val TAG = "ReservationsFragment"
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

        // Set up RecyclerView
        adapter = ReservationAdapter(
            reservations,
            { reservation -> showReservationDetails(reservation) },
            { reservation -> showCancelDialog(reservation) }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Set up tab selection listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isPastReservations = tab?.position == 1
                loadReservations()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Load upcoming reservations by default
        loadReservations()

        return view
    }

    override fun onResume() {
        super.onResume()
        // Reload reservations when the fragment becomes visible again
        loadReservations()
    }

    private fun loadReservations() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User not logged in
            emptyView.text = "Please login to view your reservations"
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            return
        }

        progressBar.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        recyclerView.visibility = View.GONE

        // Get current time
        val now = Date()

        // Create the base query
        var query = db.collection("Reservations")
            .whereEqualTo("userId", currentUser.uid)

        // Add time filter based on selected tab
        if (isPastReservations) {
            // Past reservations (before now)
            query = query.whereLessThan("reservationTime", now)
                .orderBy("reservationTime", Query.Direction.DESCENDING)
        } else {
            // Upcoming reservations (after now)
            query = query.whereGreaterThanOrEqualTo("reservationTime", now)
                .orderBy("reservationTime", Query.Direction.ASCENDING)
        }

        // Execute the query
        query.get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE

                if (documents.isEmpty()) {
                    // No reservations found
                    emptyView.text = if (isPastReservations)
                        "No past reservations" else "No upcoming reservations"
                    emptyView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    // Reservations found
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE

                    // Clear existing data
                    reservations.clear()

                    // Reset the temporary storage and counter each time we load
                    val tempReservations = mutableListOf<Map<String, Any>>()
                    var loadedCount = 0

                    // Parse reservations data from Firestore
                    for (document in documents) {
                        val reservationData = document.data.toMutableMap()
                        reservationData["id"] = document.id

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
                                    tempReservations.add(reservationData)

                                    // Increment counter and check if all loaded
                                    loadedCount++
                                    if (loadedCount == documents.size()) {
                                        // Sort the reservations by date
                                        val sortedReservations = if (isPastReservations) {
                                            tempReservations.sortedByDescending {
                                                (it["reservationTime"] as? com.google.firebase.Timestamp)?.toDate()
                                            }
                                        } else {
                                            tempReservations.sortedBy {
                                                (it["reservationTime"] as? com.google.firebase.Timestamp)?.toDate()
                                            }
                                        }

                                        // Clear and update the reservations list in one go
                                        reservations.clear()
                                        reservations.addAll(sortedReservations)
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error loading shop info", e)
                                    reservationData["shopName"] = "Unknown Shop"
                                    tempReservations.add(reservationData)

                                    // Increment counter and check if all loaded
                                    loadedCount++
                                    if (loadedCount == documents.size()) {
                                        reservations.clear()
                                        reservations.addAll(tempReservations)
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                        } else {
                            reservationData["shopName"] = "Unknown Shop"
                            tempReservations.add(reservationData)

                            // Increment counter and check if all loaded
                            loadedCount++
                            if (loadedCount == documents.size()) {
                                reservations.clear()
                                reservations.addAll(tempReservations)
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                emptyView.text = "Error loading reservations: ${exception.message}"
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                Log.e(TAG, "Error loading reservations", exception)
            }
    }

    private fun showReservationDetails(reservation: Map<String, Any>) {
        // Get details from reservation
        val shopName = reservation["shopName"] as? String ?: "Unknown Shop"
        val tableNumber = reservation["tableNumber"] as? Long ?: 0
        val status = reservation["status"] as? String ?: "pending"
        val partySize = reservation["partySize"] as? Long ?: 1
        val notes = reservation["notes"] as? String ?: ""
        val timestamp = reservation["reservationTime"] as? com.google.firebase.Timestamp
        val reservationDate = timestamp?.toDate() ?: Date()

        // Format date and time
        val dateFormat = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())

        val formattedDate = dateFormat.format(reservationDate)
        val formattedTime = timeFormat.format(reservationDate)

        // Duration and end time
        val duration = reservation["duration"] as? Long ?: 2
        val endTime = Date(reservationDate.time + (duration * 60 * 60 * 1000))
        val formattedEndTime = timeFormat.format(endTime)

        // Create details string
        val details = StringBuilder()
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

    private fun showCancelDialog(reservation: Map<String, Any>) {
        val reservationId = reservation["id"] as? String ?: return

        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Reservation")
            .setMessage("Are you sure you want to cancel this reservation?")
            .setPositiveButton("Yes") { _, _ ->
                cancelReservation(reservationId)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelReservation(reservationId: String) {
        progressBar.visibility = View.VISIBLE


        val updates = HashMap<String, Any>()
        updates["status"] = "cancelled"
        updates["cancelledAt"] = com.google.firebase.Timestamp.now()
        updates["cancelledBy"] = auth.currentUser?.uid ?: ""

        db.collection("Reservations").document(reservationId)
            .update(updates)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Reservation cancelled", Toast.LENGTH_SHORT).show()
                loadReservations()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error cancelling reservation", e)
            }
    }
}