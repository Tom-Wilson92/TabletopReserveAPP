package com.example.tabletopreserve

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class ReservationAdapter(
    private val reservations: List<Map<String, Any>>,
    private val onViewDetails: (Map<String, Any>) -> Unit,
    private val onCancel: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder>() {

    class ReservationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val shopNameText: TextView = view.findViewById(R.id.shop_name)
        val statusText: TextView = view.findViewById(R.id.reservation_status)
        val dateText: TextView = view.findViewById(R.id.reservation_date)
        val timeText: TextView = view.findViewById(R.id.reservation_time)
        val tableDetailsText: TextView = view.findViewById(R.id.table_details)
        val viewDetailsButton: Button = view.findViewById(R.id.view_details_button)
        val cancelButton: Button = view.findViewById(R.id.cancel_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reservation, parent, false)
        return ReservationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
        val reservation = reservations[position]

        // Extract reservation data
        val shopName = reservation["shopName"] as? String ?: "Unknown Shop"
        val tableNumber = reservation["tableNumber"] as? Long ?: 0
        val status = reservation["status"] as? String ?: "pending"
        val partySize = reservation["partySize"] as? Long ?: 1

        // Get and format reservation time
        val timestamp = reservation["reservationTime"] as? Timestamp
        val reservationDate = timestamp?.toDate() ?: Date()

        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        val formattedDate = dateFormat.format(reservationDate)
        val formattedTime = timeFormat.format(reservationDate)

        // Get duration
        val duration = reservation["duration"] as? Long ?: 2

        // Calculate end time
        val endTime = Date(reservationDate.time + (duration * 60 * 60 * 1000))
        val formattedEndTime = timeFormat.format(endTime)

        // Set text to views
        holder.shopNameText.text = shopName
        holder.dateText.text = formattedDate
        holder.timeText.text = "$formattedTime - $formattedEndTime"
        holder.tableDetailsText.text = "Table $tableNumber • Party of $partySize"

        // Set status with appropriate styling
        holder.statusText.text = when (status) {
            "confirmed" -> "Confirmed"
            "completed" -> "Completed"
            "cancelled" -> "Cancelled"
            else -> "Pending"
        }

        // Set status background color
        val statusBgResource = when (status) {
            "confirmed" -> android.R.color.holo_green_light
            "completed" -> android.R.color.holo_green_dark
            "cancelled" -> android.R.color.holo_red_light
            else -> android.R.color.holo_orange_light
        }
        holder.statusText.setBackgroundResource(statusBgResource)

        // Set click listeners
        holder.viewDetailsButton.setOnClickListener {
            onViewDetails(reservation)
        }

        holder.cancelButton.setOnClickListener {
            onCancel(reservation)
        }

        // Hide cancel button if reservation is already cancelled or completed
        if (status == "cancelled" || status == "completed") {
            holder.cancelButton.visibility = View.GONE
        } else {
            holder.cancelButton.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = reservations.size
}