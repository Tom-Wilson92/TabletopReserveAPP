package com.example.tabletopreserve

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.tabletopreserve.models.NotificationPreferences
import com.example.tabletopreserve.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsActivity : AppCompatActivity() {

    private lateinit var reservationReminderSwitch: SwitchCompat
    private lateinit var promotionalOffersSwitch: SwitchCompat
    private lateinit var eventUpdatesSwitch: SwitchCompat

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userProfile: UserProfile? = null

    companion object {
        private const val TAG = "NotificationsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notification Settings"

        // Initialize UI components
        reservationReminderSwitch = findViewById(R.id.reservation_reminders_switch)
        promotionalOffersSwitch = findViewById(R.id.promotional_offers_switch)
        eventUpdatesSwitch = findViewById(R.id.event_updates_switch)

        // Load user notification preferences
        loadNotificationPreferences()

        // Set switch change listeners to save preferences immediately
        setupSwitchListeners()
    }

    private fun loadNotificationPreferences() {
        val currentUser = auth.currentUser ?: return

        db.collection("Users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        userProfile = document.toObject(UserProfile::class.java)
                        userProfile?.let { profile ->
                            // Set switches based on saved preferences
                            reservationReminderSwitch.isChecked = profile.notificationPreferences.reservationReminders
                            promotionalOffersSwitch.isChecked = profile.notificationPreferences.promotionalOffers

                            // For event updates, default to true if not defined
                            eventUpdatesSwitch.isChecked = true
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user data", e)
                        // Set default values
                        setDefaultPreferences()
                    }
                } else {
                    // User document doesn't exist, use defaults
                    setDefaultPreferences()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading user profile", e)
                // Set default values
                setDefaultPreferences()
            }
    }

    private fun setDefaultPreferences() {
        reservationReminderSwitch.isChecked = true
        promotionalOffersSwitch.isChecked = false
        eventUpdatesSwitch.isChecked = true
    }

    private fun setupSwitchListeners() {
        // Save preferences when any switch is toggled
        val switchChangeListener = { _: Boolean ->
            saveNotificationPreferences()
        }

        reservationReminderSwitch.setOnCheckedChangeListener { _, isChecked -> switchChangeListener(isChecked) }
        promotionalOffersSwitch.setOnCheckedChangeListener { _, isChecked -> switchChangeListener(isChecked) }
        eventUpdatesSwitch.setOnCheckedChangeListener { _, isChecked -> switchChangeListener(isChecked) }
    }

    private fun saveNotificationPreferences() {
        val currentUser = auth.currentUser ?: return

        // Get current preferences from UI
        val preferences = NotificationPreferences(
            reservationReminders = reservationReminderSwitch.isChecked,
            promotionalOffers = promotionalOffersSwitch.isChecked
        )

        // Create or update user profile
        if (userProfile == null) {
            userProfile = UserProfile(
                id = currentUser.uid,
                email = currentUser.email ?: "",
                notificationPreferences = preferences,
                createdAt = System.currentTimeMillis()
            )
        } else {
            userProfile?.notificationPreferences = preferences
        }

        // Save to Firestore
        userProfile?.let { profile ->
            db.collection("Users").document(currentUser.uid)
                .set(profile)
                .addOnSuccessListener {
                    Log.d(TAG, "Notification preferences saved successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving notification preferences", e)
                    Toast.makeText(this, "Error saving preferences", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed() // Handle the back button
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}