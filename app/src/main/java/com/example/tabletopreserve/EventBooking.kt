package com.example.tabletopreserve

import java.io.Serializable
import java.util.Date

data class EventBooking(
    val id: String = "",
    val eventId: String = "",
    val shopId: String = "",
    val eventName: String = "",
    val eventDate: Date? = null,
    val status: String = "confirmed", // "confirmed", "completed", "cancelled"
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val participants: Int = 1,
    val notes: String = "",
    val createdAt: Date? = null,
    val createdBy: String = "",
    val userId: String = "",
    val cancelledAt: Date? = null,
    val cancelledBy: String = "",
    val completedAt: Date? = null
) : Serializable