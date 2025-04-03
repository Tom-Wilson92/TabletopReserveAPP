package com.example.tabletopreserve.models

import java.io.Serializable
import java.util.Date
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class Table(
    val id: String = "",

    @get:PropertyName("shopId")
    @set:PropertyName("shopId")
    var shopId: String = "",

    @get:PropertyName("tableNumber")
    @set:PropertyName("tableNumber")
    var tableNumber: Int = 0,

    @get:PropertyName("capacity")
    @set:PropertyName("capacity")
    var capacity: Int = 0,

    @get:PropertyName("tableType")
    @set:PropertyName("tableType")
    var tableType: String = "",

    @get:PropertyName("description")
    @set:PropertyName("description")
    var description: String = "",

    @get:PropertyName("specializedGame")
    @set:PropertyName("specializedGame")
    var specializedGame: String = "",

    @get:PropertyName("features")
    @set:PropertyName("features")
    var features: List<String> = listOf(),

    @get:PropertyName("hourlyRate")
    @set:PropertyName("hourlyRate")
    var hourlyRate: Double? = null,

    @get:PropertyName("minBookingHours")
    @set:PropertyName("minBookingHours")
    var minBookingHours: Int = 1,

    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null,

    @get:PropertyName("updatedAt")
    @set:PropertyName("updatedAt")
    var updatedAt: Timestamp? = null
) : Serializable {
    // Empty constructor required for Firestore
    constructor() : this(id = "")
}