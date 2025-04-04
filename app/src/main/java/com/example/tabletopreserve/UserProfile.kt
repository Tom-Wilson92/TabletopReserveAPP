package com.example.tabletopreserve.models

import java.io.Serializable

data class UserProfile(
    val id: String = "",
    var name: String = "",
    var email: String = "",
    var phoneNumber: String = "",
    var favoriteGames: List<String> = listOf(),
    var notificationPreferences: NotificationPreferences = NotificationPreferences(),
    val createdAt: Long = 0
) : Serializable {
    // Empty constructor for Firestore
    constructor() : this(id = "")
}

data class NotificationPreferences(
    var reservationReminders: Boolean = true,
    var promotionalOffers: Boolean = false
) : Serializable {
    // Empty constructor for Firestore
    constructor() : this(true, false)
}