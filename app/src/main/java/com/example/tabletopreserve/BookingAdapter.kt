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

class BookingAdapter(
    private val bookings: List<Map<String, Any>>,
    private val onViewDetails: (Map<String, Any>) -> Unit,
    private val onCancel: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    companion object {
        private const val TYPE_TABLE = "table"
        private const val TYPE_EVENT = "event"
    }

    class BookingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val shopNameText: TextView = view.findViewById(R.id.shop_name)
        val bookingTypeText: TextView = view.findViewById(R.id.booking_type)
        val statusText: TextView = view.findViewById(R.id.reservation_status)
        val dateText: TextView = view.findViewById(R.id.reservation_date)
        val timeText: TextView = view.findViewById(R.id.reservation_time)
        val detailsText: TextView = view.findViewById(R.id.table_details)
        val viewDetailsButton: Button = view.findViewById(R.id.view_details_button)
        val cancelButton: Button = view.findViewById(R.id.cancel_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reservation, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        val bookingType = booking["bookingType"] as? String ?: TYPE_TABLE

        // Extract common data
        val shopName = booking["shopName"] as? String ?: "Unknown Shop"
        val status = if (bookingType == TYPE_TABLE) {
            booking["status"] as? String ?: "pending"
        } else {
            booking["status"] as? String ?: "confirmed"
        }

        // Get and format booking date
        val timestamp = if (bookingType == TYPE_TABLE) {
            booking["reservationTime"] as? Timestamp
        } else {
            booking["eventDate"] as? Timestamp
        }
        val bookingDate = timestamp?.toDate() ?: Date()

        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(bookingDate)

        // Set base text for all bookings
        holder.shopNameText.text = shopName
        holder.dateText.text = formattedDate

        // Set booking type indicator
        if (bookingType == TYPE_TABLE) {
            holder.bookingTypeText.text = "Table"
            holder.bookingTypeText.setBackgroundResource(R.color.purple_500)

            // Set table-specific details
            val tableNumber = booking["tableNumber"] as? Long ?: 0
            val partySize = booking["partySize"] as? Long ?: 1
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val formattedTime = timeFormat.format(bookingDate)

            // Calculate end time based on duration
            val duration = booking["duration"] as? Long ?: 2
            val endTime = Date(bookingDate.time + (duration * 60 * 60 * 1000))
            val formattedEndTime = timeFormat.format(endTime)

            holder.timeText.text = "$formattedTime - $formattedEndTime"
            holder.detailsText.text = "Table $tableNumber • Party of $partySize"
        } else {
            holder.bookingTypeText.text = "Event"
            holder.bookingTypeText.setBackgroundResource(R.color.teal_700)

            // Set event-specific details
            val eventName = booking["eventName"] as? String ?: "Unknown Event"
            val participants = booking["participants"] as? Long ?: 1

            // Get event time string if available
            val timeString = booking["eventTimeString"] as? String ?: ""
            holder.timeText.text = if (timeString.isNotEmpty()) timeString else "Time not specified"

            holder.detailsText.text = "$eventName • $participants participants"
        }

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
            onViewDetails(booking)
        }

        holder.cancelButton.setOnClickListener {
            onCancel(booking)
        }

        // Hide cancel button if booking is already cancelled or completed
        if (status == "cancelled" || status == "completed") {
            holder.cancelButton.visibility = View.GONE
        } else {
            holder.cancelButton.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = bookings.size
}