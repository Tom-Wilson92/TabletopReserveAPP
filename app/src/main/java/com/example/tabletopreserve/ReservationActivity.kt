package com.example.tabletopreserve

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tabletopreserve.models.Shop
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReservationActivity : AppCompatActivity() {

    private lateinit var shopNameTextView: TextView
    private lateinit var tableInfoTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var partyEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var notesEditText: EditText
    private lateinit var selectDateButton: Button
    private lateinit var selectTimeButton: Button
    private lateinit var submitButton: Button

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var shop: Shop
    private var tableId: String = ""
    private var tableNumber: Int = 0
    private var tableType: String = ""

    private val calendar = Calendar.getInstance()
    private var duration: Int = 2 // Default 2 hours

    companion object {
        private const val TAG = "ReservationActivity"
        const val EXTRA_SHOP = "extra_shop"
        const val EXTRA_TABLE_ID = "extra_table_id"
        const val EXTRA_TABLE_NUMBER = "extra_table_number"
        const val EXTRA_TABLE_TYPE = "extra_table_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation)

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Get extras from intent
        val shopExtra = intent.getSerializableExtra(EXTRA_SHOP)
        if (shopExtra is Shop) {
            shop = shopExtra
        } else {
            // Handle the error, perhaps finish the activity or show a toast
            Toast.makeText(this, "Shop data is missing or invalid", Toast.LENGTH_SHORT).show()
            finish() // Optionally, close the activity if the Shop object is critical
            return
        }

        tableId = intent.getStringExtra(EXTRA_TABLE_ID) ?: ""
        tableNumber = intent.getIntExtra(EXTRA_TABLE_NUMBER, 0)
        tableType = intent.getStringExtra(EXTRA_TABLE_TYPE) ?: ""

        // Initialize views
        shopNameTextView = findViewById(R.id.shop_name)
        tableInfoTextView = findViewById(R.id.table_info)
        dateTextView = findViewById(R.id.date_text)
        timeTextView = findViewById(R.id.time_text)
        partyEditText = findViewById(R.id.party_size)
        nameEditText = findViewById(R.id.customer_name)
        phoneEditText = findViewById(R.id.customer_phone)
        emailEditText = findViewById(R.id.customer_email)
        notesEditText = findViewById(R.id.notes)
        selectDateButton = findViewById(R.id.select_date_button)
        selectTimeButton = findViewById(R.id.select_time_button)
        submitButton = findViewById(R.id.submit_button)

        // Set shop and table info
        shopNameTextView.text = shop.storeName
        tableInfoTextView.text = "Table $tableNumber - $tableType"

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

                        document.getString("phone")?.let { phone ->
                            if (phone.isNotEmpty()) {
                                phoneEditText.setText(phone)
                            }
                        }
                    }
                }
        }

        // Set up action bar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Book a Table"
        }

        // Set default date (tomorrow)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        updateDateText()

        // Set default time (6:00 PM)
        calendar.set(Calendar.HOUR_OF_DAY, 18)
        calendar.set(Calendar.MINUTE, 0)
        updateTimeText()

        // Set click listeners
        selectDateButton.setOnClickListener { showDatePicker() }
        selectTimeButton.setOnClickListener { showTimePicker() }
        submitButton.setOnClickListener { submitReservation() }
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                calendar.set(Calendar.YEAR, selectedYear)
                calendar.set(Calendar.MONTH, selectedMonth)
                calendar.set(Calendar.DAY_OF_MONTH, selectedDayOfMonth)
                updateDateText()
            }, year, month, day)

        // Set min date to today
        val today = Calendar.getInstance()
        datePickerDialog.datePicker.minDate = today.timeInMillis

        // Set max date to 3 months from now
        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.MONTH, 3)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this,
            { _, selectedHour, selectedMinute ->
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)
                updateTimeText()
            }, hour, minute, false)

        timePickerDialog.show()
    }

    private fun updateDateText() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        dateTextView.text = dateFormat.format(calendar.time)
    }

    private fun updateTimeText() {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        timeTextView.text = timeFormat.format(calendar.time)
    }

    private fun submitReservation() {
        // Validate inputs
        val partySize = partyEditText.text.toString().toIntOrNull() ?: 0
        val customerName = nameEditText.text.toString().trim()
        val customerPhone = phoneEditText.text.toString().trim()
        val customerEmail = emailEditText.text.toString().trim()
        val notes = notesEditText.text.toString().trim()

        if (partySize <= 0) {
            partyEditText.error = "Please enter a valid party size"
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

        // Create reservation data
        val reservationTime = calendar.time
        val userId = auth.currentUser?.uid ?: ""

        val reservationData = hashMapOf(
            "shopId" to shop.id,
            "tableId" to tableId,
            "tableNumber" to tableNumber,
            "reservationTime" to reservationTime,
            "duration" to duration,
            "status" to "pending",
            "customerName" to customerName,
            "customerPhone" to customerPhone,
            "customerEmail" to customerEmail,
            "partySize" to partySize,
            "notes" to notes,
            "createdAt" to Date(),
            "createdBy" to (if (userId.isNotEmpty()) userId else "customer"),
            "userId" to userId
        )

        // Show loading state
        submitButton.isEnabled = false
        submitButton.text = "Creating Reservation..."

        // Save to Firestore
        db.collection("Reservations")
            .add(reservationData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Reservation created with ID: ${documentReference.id}")

                // Save user information if logged in
                if (userId.isNotEmpty()) {
                    val userUpdates = hashMapOf<String, Any>()
                    userUpdates["name"] = customerName
                    userUpdates["phone"] = customerPhone

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
                Toast.makeText(this, "Reservation created successfully!", Toast.LENGTH_LONG).show()
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error creating reservation", e)
                Toast.makeText(this, "Error creating reservation: ${e.message}", Toast.LENGTH_SHORT).show()

                // Reset button state
                submitButton.isEnabled = true
                submitButton.text = "Book Table"
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
