package com.example.tabletopreserve

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventBookingActivity : AppCompatActivity() {

    private lateinit var shopNameTextView: TextView
    private lateinit var eventNameTextView: TextView
    private lateinit var eventDateTextView: TextView
    private lateinit var eventTimeTextView: TextView
    private lateinit var nameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var notesEditText: EditText
    private lateinit var participantsEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var shopId: String = ""
    private var shopName: String = ""
    private var eventId: String = ""
    private var eventName: String = ""
    private var eventDate: Date? = null
    private var eventTimeString: String = ""

    companion object {
        private const val TAG = "EventBookingActivity"
        const val EXTRA_SHOP_ID = "extra_shop_id"
        const val EXTRA_SHOP_NAME = "extra_shop_name"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_EVENT_NAME = "extra_event_name"
        const val EXTRA_EVENT_DATE = "extra_event_date"
        const val EXTRA_EVENT_TIME = "extra_event_time"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_booking)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Get extras from intent
        shopId = intent.getStringExtra(EXTRA_SHOP_ID) ?: ""
        shopName = intent.getStringExtra(EXTRA_SHOP_NAME) ?: ""
        eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: ""
        eventName = intent.getStringExtra(EXTRA_EVENT_NAME) ?: ""
        eventDate = intent.getSerializableExtra(EXTRA_EVENT_DATE) as? Date
        eventTimeString = intent.getStringExtra(EXTRA_EVENT_TIME) ?: ""

        if (shopId.isEmpty() || eventId.isEmpty()) {
            Toast.makeText(this, "Event information is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        shopNameTextView = findViewById(R.id.shop_name)
        eventNameTextView = findViewById(R.id.event_name)
        eventDateTextView = findViewById(R.id.event_date)
        eventTimeTextView = findViewById(R.id.event_time)
        nameEditText = findViewById(R.id.customer_name)
        phoneEditText = findViewById(R.id.customer_phone)
        emailEditText = findViewById(R.id.customer_email)
        notesEditText = findViewById(R.id.notes)
        participantsEditText = findViewById(R.id.participants)
        submitButton = findViewById(R.id.submit_button)
        progressBar = findViewById(R.id.progress_bar)

        // Set event info
        shopNameTextView.text = shopName
        eventNameTextView.text = eventName

        // Format and set date
        if (eventDate != null) {
            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            eventDateTextView.text = dateFormat.format(eventDate!!)
        } else {
            eventDateTextView.text = "Date not available"
        }

        // Set time
        eventTimeTextView.text = eventTimeString

        // Set up action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Book Event"
        }

        // Set current user's info if logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            emailEditText.setText(currentUser.email)

            // Try to get user's display name if available
            if (!currentUser.displayName.isNullOrEmpty()) {
                nameEditText.setText(currentUser.displayName)
            }

            // Get user data from Firestore
            db.collection("Users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Get name and phone if available
                        document.getString("name")?.let { name ->
                            if (name.isNotEmpty()) {
                                nameEditText.setText(name)
                            }
                        }

                        document.getString("phoneNumber")?.let { phone ->
                            if (phone.isNotEmpty()) {
                                phoneEditText.setText(phone)
                            }
                        }
                    }
                }
        }

        // Set click listener for submit button
        submitButton.setOnClickListener { submitEventBooking() }
    }

    private fun submitEventBooking() {
        // Validate inputs
        val participants = participantsEditText.text.toString().toIntOrNull() ?: 0
        val customerName = nameEditText.text.toString().trim()
        val customerPhone = phoneEditText.text.toString().trim()
        val customerEmail = emailEditText.text.toString().trim()
        val notes = notesEditText.text.toString().trim()

        if (participants <= 0) {
            participantsEditText.error = "Please enter a valid number of participants"
            return
        }

        if (customerName.isEmpty()) {
            nameEditText.error = "Name is required"
            return
        }

        if (customerPhone.isEmpty()) {
            phoneEditText.error = "Phone number is required"
            return
        }

        // Create booking data
        val userId = auth.currentUser?.uid ?: ""

        val bookingData = hashMapOf(
            "eventId" to eventId,
            "shopId" to shopId,
            "shopName" to shopName,
            "eventName" to eventName,
            "eventDate" to eventDate,
            "eventTimeString" to eventTimeString, // Add the time string to Firestore
            "status" to "confirmed",
            "customerName" to customerName,
            "customerPhone" to customerPhone,
            "customerEmail" to customerEmail,
            "participants" to participants,
            "notes" to notes,
            "createdAt" to Date(),
            "createdBy" to (if (userId.isNotEmpty()) userId else "customer"),
            "userId" to userId
        )

        // Show loading state
        showLoading(true)

        // Save to Firestore
        db.collection("EventBookings")
            .add(bookingData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Event booking created with ID: ${documentReference.id}")

                // Save user information if logged in
                if (userId.isNotEmpty()) {
                    val userUpdates = hashMapOf<String, Any>()
                    userUpdates["name"] = customerName
                    userUpdates["phoneNumber"] = customerPhone

                    db.collection("Users").document(userId)
                        .update(userUpdates)
                        .addOnSuccessListener {
                            Log.d(TAG, "User information updated")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error updating user information", e)
                        }
                }

                // Show success message and finish
                showLoading(false)
                Toast.makeText(this, "Event booking confirmed!", Toast.LENGTH_LONG).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error creating event booking", e)
                Toast.makeText(this, "Error creating booking: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        submitButton.isEnabled = !isLoading
        submitButton.text = if (isLoading) "Booking Event..." else "Book Event"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}