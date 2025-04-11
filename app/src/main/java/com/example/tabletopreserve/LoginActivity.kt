package com.example.tabletopreserve

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerTextView: TextView
    private lateinit var forgotPasswordTextView: TextView
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val TAG = "LoginActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI components
        emailEditText = findViewById(R.id.email_edit_text)
        passwordEditText = findViewById(R.id.password_edit_text)
        loginButton = findViewById(R.id.login_button)
        registerTextView = findViewById(R.id.register_text_view)
        forgotPasswordTextView = findViewById(R.id.forgot_password_text_view)
        progressBar = findViewById(R.id.progress_bar)

        // Set click listeners
        loginButton.setOnClickListener {
            loginWithEmailPassword()
        }

        registerTextView.setOnClickListener {
            // Navigate to register activity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        forgotPasswordTextView.setOnClickListener {
            // Show password reset dialog
            showPasswordResetDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Check if this user is a store owner
            db.collection("Stores")
                .document(userId)
                .get()
                .addOnSuccessListener { storeDocument ->
                    if (storeDocument.exists()) {
                        // This is a store owner, sign them out
                        auth.signOut()

                        // Show alert dialog instead of toast
                        AlertDialog.Builder(this)
                            .setTitle("Access Denied")
                            .setMessage("This app is for customers only. Please use the shop owner web portal at tabletop-reserve.web.app to manage your store.")
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        // Regular user, proceed normally
                        registerFcmToken()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    // Error occurred, let's proceed anyway
                    registerFcmToken()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
        }
    }

    private fun loginWithEmailPassword() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        // Validate inputs
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            emailEditText.requestFocus()
            return
        }

        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            passwordEditText.requestFocus()
            return
        }

        // Show progress bar
        progressBar.visibility = View.VISIBLE

        // Authenticate with Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "signInWithEmail:success")
                    val userId = auth.currentUser?.uid ?: ""

                    // Check in the Stores collection if this user ID is a shop
                    db.collection("Stores")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { storeDocument ->
                            progressBar.visibility = View.GONE

                            if (storeDocument.exists()) {
                                // This user ID is in the Stores collection
                                auth.signOut()

                                // Show alert dialog instead of toast
                                AlertDialog.Builder(this)
                                    .setTitle("Access Denied")
                                    .setMessage("This app is for customers only. Please use the shop owner web portal at tabletop-reserve.web.app to manage your store.")
                                    .setPositiveButton("OK", null)
                                    .show()
                            } else {
                                // Not a store owner, proceed as a regular user
                                registerFcmToken()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            progressBar.visibility = View.GONE
                            Log.w(TAG, "Error checking in Stores collection", e)

                            // In case of error with the check, let user proceed
                            registerFcmToken()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                } else {
                    // Hide progress bar
                    progressBar.visibility = View.GONE

                    // Sign in fails
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        baseContext, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun showPasswordResetDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password_reset, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.reset_email_edit_text)

        // Pre-fill the email if it's already entered in the login form
        if (this.emailEditText.text.toString().trim().isNotEmpty()) {
            emailEditText.setText(this.emailEditText.text.toString().trim())
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setView(dialogView)
            .setPositiveButton("Send Reset Link", null) // We'll set this up below
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Override the positive button click to prevent dialog dismissal on error
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                emailEditText.error = "Please enter your email address"
                return@setOnClickListener
            }

            // Show progress
            progressBar.visibility = View.VISIBLE

            // Send password reset email
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    progressBar.visibility = View.GONE

                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Password reset email sent to $email",
                            Toast.LENGTH_LONG
                        ).show()
                        dialog.dismiss()
                    } else {
                        // Show error
                        emailEditText.error = "Error: ${task.exception?.message}"
                    }
                }
        }
    }

    fun registerFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                val tokenData = mapOf("fcmToken" to token)

                FirebaseFirestore.getInstance().collection("Users").document(currentUser.uid)
                    .set(tokenData, SetOptions.merge())
                    .addOnSuccessListener { Log.d(TAG, "FCM Token saved successfully") }
                    .addOnFailureListener { e -> Log.e(TAG, "Failed to save FCM Token", e) }
            }
        }
    }
}