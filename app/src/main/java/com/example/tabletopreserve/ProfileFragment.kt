package com.example.tabletopreserve

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var editProfileButton: Button
    private lateinit var logoutButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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
        profileImage = view.findViewById(R.id.profile_image)
        nameText = view.findViewById(R.id.name_text)
        emailText = view.findViewById(R.id.email_text)
        editProfileButton = view.findViewById(R.id.edit_profile_button)
        logoutButton = view.findViewById(R.id.logout_button)

        // Set up button click listeners
        editProfileButton.setOnClickListener {
            // TODO: Navigate to edit profile screen
            // For now, just show a message
            // Toast.makeText(context, "Edit Profile clicked", Toast.LENGTH_SHORT).show()
        }

        logoutButton.setOnClickListener {
            // Sign out user
            auth.signOut()

            // Redirect to login activity
            // Uncomment this after creating LoginActivity
            // val intent = Intent(context, LoginActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // startActivity(intent)
        }

        // Load user data
        loadUserData()

        return view
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Set email
            emailText.text = currentUser.email

            // Get additional user data from Firestore
            db.collection("Users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Get user name
                        val name = document.getString("name")
                        if (!name.isNullOrEmpty()) {
                            nameText.text = name
                        } else {
                            nameText.text = "User"
                        }

                        // TODO: Load profile image if available
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle failure
                    nameText.text = "User"
                }
        } else {
            // User not logged in, redirect to login
            // Uncomment this after creating LoginActivity
            // val intent = Intent(context, LoginActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // startActivity(intent)
        }
    }
}