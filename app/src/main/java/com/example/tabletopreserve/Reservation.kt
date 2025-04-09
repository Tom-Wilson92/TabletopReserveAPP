package com.example.tabletopreserve.models

import java.io.Serializable
import java.util.Date

data class Reservation(
    val id: String = "",
    val shopId: String = "",
    val tableId: String = "",
    val tableNumber: Int = 0,
    val reservationTime: Date? = null,
    val duration: Int = 0, // in hours
    val status: String = "pending", // "pending", "confirmed", "completed", "cancelled"
    val customerName: String = "",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val partySize: Int = 1,
    val notes: String = "",
    val shopNotes: String = "",
    val createdAt: Date? = null,
    val createdBy: String = "",
    val updatedAt: Date? = null,
    val confirmedAt: Date? = null,
    val confirmedBy: String = "",
    val completedAt: Date? = null,
    val completedBy: String = "",
    val cancelledAt: Date? = null,
    val cancelledBy: String = "",
    val cancellationReason: String = ""
) : Serializable