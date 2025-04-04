package com.example.tabletopreserve

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.tabletopreserve.models.NotificationPreferences
import com.example.tabletopreserve.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var favoriteGamesEditText: EditText
    private lateinit var reservationNotificationsSwitch: SwitchCompat
    private lateinit var promotionalNotificationsSwitch: SwitchCompat
    private lateinit var saveButton: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userProfile: UserProfile? = null

    companion object {
        private const val TAG = "EditProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI components
        nameEditText = findViewById(R.id.name_edit_text)
        emailEditText = findViewById(R.id.email_edit_text)
        phoneEditText = findViewById(R.id.phone_edit_text)
        favoriteGamesEditText = findViewById(R.id.favorite_games_edit_text)
        reservationNotificationsSwitch = findViewById(R.id.reservation_notifications_switch)
        promotionalNotificationsSwitch = findViewById(R.id.promotional_notifications_switch)
        saveButton = findViewById(R.id.save_profile_button)
        progressBar = findViewById(R.id.progress_bar)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"

        // Set click listener on the email field to explain why it can't be changed
        emailEditText.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Email Cannot Be Changed")
                .setMessage("For security reasons, email addresses cannot be changed directly. This is your login identifier in the system.")
                .setPositiveButton("OK", null)
                .show()
        }

        // Load user data
        loadUserProfile()

        // Set up save button listener
        saveButton.setOnClickListener {
            saveUserProfile()
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User not logged in, redirect back
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        showLoading(true)

        // Get user data from Firestore
        db.collection("Users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)

                if (document.exists()) {
                    try {
                        // Try to convert to UserProfile
                        userProfile = document.toObject(UserProfile::class.java)?.copy(id = document.id)
                            ?: createNewUserProfile(currentUser.uid, currentUser.email ?: "")

                        // Fill UI with user data
                        populateUI()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user data", e)

                        // Create a new user profile with basic data
                        userProfile = createNewUserProfile(currentUser.uid, currentUser.email ?: "")
                        populateUI()
                    }
                } else {
                    // User document doesn't exist, create a new one
                    userProfile = createNewUserProfile(currentUser.uid, currentUser.email ?: "")
                    populateUI()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "Error loading user profile", e)
                Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createNewUserProfile(userId: String, email: String): UserProfile {
        return UserProfile(
            id = userId,
            email = email,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun populateUI() {
        // Always get the email from the current user for the most up-to-date value
        val email = auth.currentUser?.email ?: ""
        emailEditText.setText(email)

        userProfile?.let { profile ->
            nameEditText.setText(profile.name)
            phoneEditText.setText(profile.phoneNumber)

            // Join favorite games list with commas
            favoriteGamesEditText.setText(profile.favoriteGames.joinToString(", "))

            // Set notification switches
            reservationNotificationsSwitch.isChecked = profile.notificationPreferences.reservationReminders
            promotionalNotificationsSwitch.isChecked = profile.notificationPreferences.promotionalOffers
        }
    }

    private fun saveUserProfile() {
        val currentUser = auth.currentUser ?: return

        // Get updated values from UI
        val name = nameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()
        val gamesText = favoriteGamesEditText.text.toString().trim()
        val favoriteGames = if (gamesText.isNotEmpty()) {
            gamesText.split(",").map { it.trim() }
        } else {
            listOf()
        }

        val notificationPrefs = NotificationPreferences(
            reservationReminders = reservationNotificationsSwitch.isChecked,
            promotionalOffers = promotionalNotificationsSwitch.isChecked
        )

        // Validate inputs
        if (name.isEmpty()) {
            nameEditText.error = "Name is required"
            nameEditText.requestFocus()
            return
        }

        showLoading(true)

        // Update the user profile
        userProfile?.let { profile ->
            profile.name = name
            profile.phoneNumber = phone
            profile.favoriteGames = favoriteGames
            profile.notificationPreferences = notificationPrefs

            // Make sure we always use the current email from Firebase Auth
            profile.email = currentUser.email ?: profile.email

            // Save to Firestore
            db.collection("Users").document(currentUser.uid)
                .set(profile)
                .addOnSuccessListener {
                    showLoading(false)
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    finish() // Go back to profile screen
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Log.e(TAG, "Error updating user profile", e)
                    Toast.makeText(this, "Error saving profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        saveButton.isEnabled = !isLoading
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed() // Handle the back button in the action bar
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}