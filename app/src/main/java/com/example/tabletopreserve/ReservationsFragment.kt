package com.example.tabletopreserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class ReservationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

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

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Load reservations
        loadReservations()

        return view
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

        // Get current time
        val now = Date()

        // Query user's reservations from Firestore
        db.collection("Reservations")
            .whereEqualTo("userId", currentUser.uid)
            .whereGreaterThanOrEqualTo("reservationTime", now)
            .orderBy("reservationTime")
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE

                if (documents.isEmpty) {
                    // No reservations found
                    emptyView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    // Reservations found
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE

                    // TODO: Parse reservations and set adapter
                    // This will be implemented when you create the ReservationAdapter

                    // For now, just show a placeholder
                    emptyView.text = "Found ${documents.size()} upcoming reservations"
                    emptyView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                emptyView.text = "Error loading reservations: ${exception.message}"
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
    }
}