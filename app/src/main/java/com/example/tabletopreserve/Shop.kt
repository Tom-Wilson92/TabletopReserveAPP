package com.example.tabletopreserve.models

import java.io.Serializable
import java.util.Date

data class Shop(
    val id: String = "",
    val storeName: String = "",
    val ownerName: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val county: String = "",
    val postCode: String = "",
    val phoneNumber: String = "",
    val role: String = "shop",
    val isApproved: Boolean = false,
    val registrationStatus: String = "pending", // "pending", "approved", "rejected"
    val description: String = "",
    val shopType: String = "", // "game-store", "dedicated-tables", "cafe"
    val website: String = "",
    val socialMedia: String = "",
    val amenities: Map<String, Boolean> = mapOf(
        "wifi" to false,
        "food" to false,
        "drinks" to false,
        "parking" to false,
        "accessible" to false,
        "gameLibrary" to false
    ),
    val paymentMethods: Map<String, Boolean> = mapOf(
        "cash" to false,
        "credit" to false,
        "debit" to false,
        "mobile" to false
    ),
    val specialty: String = "",
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val approvedAt: Date? = null,
    val approvedBy: String = "",
    val rejectedAt: Date? = null,
    val rejectedBy: String = ""
) : Serializable