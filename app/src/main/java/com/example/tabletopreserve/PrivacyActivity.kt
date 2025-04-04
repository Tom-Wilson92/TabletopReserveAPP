package com.example.tabletopreserve

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PrivacyActivity : AppCompatActivity() {

    private lateinit var locationSharingSwitch: SwitchCompat
    private lateinit var dataCollectionSwitch: SwitchCompat
    private lateinit var deleteAccountButton: Button
    private lateinit var privacyPolicyText: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var accountDeletionService: AccountDeletionService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        accountDeletionService = AccountDeletionService()

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Privacy Settings"

        // Initialize UI components
        locationSharingSwitch = findViewById(R.id.location_sharing_switch)
        dataCollectionSwitch = findViewById(R.id.data_collection_switch)
        deleteAccountButton = findViewById(R.id.delete_account_button)
        privacyPolicyText = findViewById(R.id.privacy_policy_text)
        progressBar = findViewById(R.id.progress_bar)

        // Set up click listeners
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Location sharing toggle
        locationSharingSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Here you would typically save this preference to user settings
            Toast.makeText(
                this,
                "Location sharing ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Data collection toggle
        dataCollectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Here you would typically save this preference to user settings
            Toast.makeText(
                this,
                "Data collection for app improvement ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Delete account button
        deleteAccountButton.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }

        // Privacy policy text click
        privacyPolicyText.setOnClickListener {
            // Show the privacy policy in a dialog
            showPrivacyPolicyDialog()
        }
    }

    private fun showDeleteAccountConfirmationDialog() {
        // Get the user's email for confirmation
        val userEmail = auth.currentUser?.email ?: "your account"

        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete $userEmail? This action cannot be undone and all your data will be lost, including:\n\n• Your profile information\n• All reservations\n• Saved preferences\n\nTo confirm, please type DELETE")
            .setView(R.layout.dialog_delete_confirmation)
            .setPositiveButton("Delete Account") { dialog, _ ->
                // Get the confirmation text from the dialog
                val confirmationEditText = (dialog as AlertDialog).findViewById<android.widget.EditText>(R.id.confirmation_edit_text)
                val confirmationText = confirmationEditText?.text.toString()

                if (confirmationText.equals("DELETE", ignoreCase = true)) {
                    // Proceed with account deletion
                    deleteUserAccount()
                } else {
                    Toast.makeText(this, "Confirmation text didn't match. Account not deleted.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUserAccount() {
        showLoading(true)

        lifecycleScope.launch {
            accountDeletionService.deleteUserAccount(
                onSuccess = {
                    showLoading(false)
                    Toast.makeText(this@PrivacyActivity, "Your account has been deleted", Toast.LENGTH_SHORT).show()

                    // Return to login screen
                    val intent = Intent(this@PrivacyActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finishAffinity() // Close all activities
                },
                onError = { error ->
                    showLoading(false)
                    Toast.makeText(this@PrivacyActivity, "Error deleting account: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun showPrivacyPolicyDialog() {
        val policyText = """
            Privacy Policy for Tabletop Reserve
            
            1. Information We Collect
            We collect information you provide directly to us, such as your name, email address, and phone number.
            
            2. How We Use Your Information
            We use your information to provide and improve our service, process reservations, and communicate with you.
            
            3. Information Sharing
            We do not sell or share your personal information with third parties except as described in this policy.
            
            4. Your Choices
            You can update your account information or notification preferences at any time through your profile settings.
            
            5. Data Security
            We implement reasonable measures to help protect your personal information.
            
            6. Changes to This Policy
            We may update this privacy policy from time to time. We will notify you of any changes by posting the new policy on this page.
            
            Last updated: April 2025
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Privacy Policy")
            .setMessage(policyText)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        deleteAccountButton.isEnabled = !isLoading
        locationSharingSwitch.isEnabled = !isLoading
        dataCollectionSwitch.isEnabled = !isLoading
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed() // Handle the back button
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}