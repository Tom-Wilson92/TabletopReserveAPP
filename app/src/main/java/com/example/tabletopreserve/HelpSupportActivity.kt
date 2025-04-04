package com.example.tabletopreserve

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class HelpSupportActivity : AppCompatActivity() {

    private lateinit var faqCard: CardView
    private lateinit var contactCard: CardView
    private lateinit var reportCard: CardView
    private lateinit var versionText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_support)

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Help & Support"

        // Initialize UI components
        faqCard = findViewById(R.id.faq_card)
        contactCard = findViewById(R.id.contact_card)
        reportCard = findViewById(R.id.report_card)
        versionText = findViewById(R.id.version_text)

        // Set version text
        val versionName = try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "1.0.0"
        }
        versionText.text = "Version $versionName"

        // Set up click listeners
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // FAQ card click
        faqCard.setOnClickListener {
            showFAQDialog()
        }

        // Contact card click
        contactCard.setOnClickListener {
            showContactOptions()
        }

        // Report issue card click
        reportCard.setOnClickListener {
            showReportDialog()
        }
    }

    private fun showFAQDialog() {
        val faqItems = arrayOf(
            "How do I make a reservation?",
            "Can I cancel my reservation?",
            "How far in advance can I book?",
            "What if I'm running late?",
            "Do I need to create an account?"
        )

        val faqAnswers = arrayOf(
            "To make a reservation, browse to a game shop, select an available table, choose your date and time, and confirm your booking.",
            "Yes, you can cancel a reservation up to 2 hours before your scheduled time without any penalty.",
            "Most shops allow bookings up to 2 weeks in advance. Some special events might have different booking windows.",
            "If you're running late, please contact the shop directly. They typically hold your reservation for 15 minutes past the start time.",
            "Yes, you need to create an account to make reservations, which helps track your bookings and preferences."
        )

        AlertDialog.Builder(this)
            .setTitle("Frequently Asked Questions")
            .setItems(faqItems) { _, which ->
                // Show the selected FAQ answer
                AlertDialog.Builder(this)
                    .setTitle(faqItems[which])
                    .setMessage(faqAnswers[which])
                    .setPositiveButton("Got it", null)
                    .show()
            }
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showContactOptions() {
        val options = arrayOf("Email Support", "Call Support")

        AlertDialog.Builder(this)
            .setTitle("Contact Support")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Email support
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@tabletopreserve.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Support Request")
                        }
                        startActivity(Intent.createChooser(intent, "Send Email"))
                    }
                    1 -> {
                        // Call support
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:0123456789")
                        }
                        startActivity(intent)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReportDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_report_issue, null)

        AlertDialog.Builder(this)
            .setTitle("Report an Issue")
            .setView(view)
            .setPositiveButton("Submit") { _, _ ->
                // In a real app, you would send this report to your backend
                // For now, just show a confirmation
                AlertDialog.Builder(this)
                    .setTitle("Thank You")
                    .setMessage("Your report has been submitted. We'll look into it as soon as possible.")
                    .setPositiveButton("OK", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed() // Handle the back button
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}