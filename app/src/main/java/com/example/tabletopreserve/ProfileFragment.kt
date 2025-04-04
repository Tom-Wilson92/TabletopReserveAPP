package com.example.tabletopreserve

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.tabletopreserve.models.UserProfile

class ProfileFragment : Fragment() {

    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var phoneText: TextView
    private lateinit var favoriteGamesText: TextView
    private lateinit var editProfileButton: Button
    private lateinit var logoutButton: Button
    private lateinit var settingsNotifications: TextView
    private lateinit var settingsPrivacy: TextView
    private lateinit var settingsHelp: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userProfile: UserProfile? = null

    companion object {
        private const val TAG = "ProfileFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        nameText = view.findViewById(R.id.name_text)
        emailText = view.findViewById(R.id.email_text)
        phoneText = view.findViewById(R.id.phone_text)
        favoriteGamesText = view.findViewById(R.id.favorite_games_text)
        editProfileButton = view.findViewById(R.id.edit_profile_button)
        logoutButton = view.findViewById(R.id.logout_button)

        // Settings options
        settingsNotifications = view.findViewById(R.id.settings_notifications)
        settingsPrivacy = view.findViewById(R.id.settings_privacy)
        settingsHelp = view.findViewById(R.id.settings_help)

        // Set up button click listeners
        editProfileButton.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            // Sign out user
            auth.signOut()

            // Redirect to login activity
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Set up settings click listeners
        setupSettingsClickListeners()

        return view
    }

    private fun setupSettingsClickListeners() {
        settingsNotifications.setOnClickListener {
            val intent = Intent(requireContext(), NotificationsActivity::class.java)
            startActivity(intent)
        }

        settingsPrivacy.setOnClickListener {
            val intent = Intent(requireContext(), PrivacyActivity::class.java)
            startActivity(intent)
        }

        settingsHelp.setOnClickListener {
            val intent = Intent(requireContext(), HelpSupportActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload user data every time the fragment becomes visible
        loadUserData()
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            Log.d(TAG, "Current user: ${currentUser.email}")

            // Always set the email directly from Firebase Auth
            emailText.text = currentUser.email ?: "No email available"

            // Get user data from Firestore
            db.collection("Users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        try {
                            userProfile = document.toObject(UserProfile::class.java)
                            userProfile?.let { profile ->
                                // Update UI with user profile data
                                nameText.text = profile.name.ifEmpty { "User" }

                                // Display phone if available
                                phoneText.text = profile.phoneNumber.ifEmpty { "Not provided" }

                                // Display favorite games if available
                                if (profile.favoriteGames.isNotEmpty()) {
                                    favoriteGamesText.text = profile.favoriteGames.joinToString(", ")
                                } else {
                                    favoriteGamesText.text = "None specified"
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing user data", e)
                            nameText.text = "User"
                        }
                    } else {
                        Log.d(TAG, "User document does not exist")
                        nameText.text = "User"
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle failure
                    Log.e(TAG, "Error loading user data", exception)
                    nameText.text = "User"
                }
        } else {
            // User not logged in, redirect to login
            Log.d(TAG, "No user logged in")
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}