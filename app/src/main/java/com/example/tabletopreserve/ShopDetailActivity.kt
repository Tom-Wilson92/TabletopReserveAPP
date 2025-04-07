package com.example.tabletopreserve

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ShopDetailActivity : AppCompatActivity() {

    private lateinit var shopNameTextView: TextView
    private lateinit var shopAddressTextView: TextView
    private lateinit var shopDescriptionTextView: TextView
    private lateinit var followButton: Button
    private lateinit var tabLayout: TabLayout
    private lateinit var contentContainer: View
    private lateinit var loadingView: View

    private var shopId: String? = null
    private var isFollowing = false

    companion object {
        private const val TAG = "ShopDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop_detail)

        // Initialize views
        shopNameTextView = findViewById(R.id.shop_name)
        shopAddressTextView = findViewById(R.id.shop_address)
        shopDescriptionTextView = findViewById(R.id.shop_description)
        followButton = findViewById(R.id.follow_button)
        tabLayout = findViewById(R.id.tab_layout)
        contentContainer = findViewById(R.id.content_container)
        loadingView = findViewById(R.id.loading_view)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get shop ID from intent
        shopId = intent.getStringExtra("shop_id")
        if (shopId == null) {
            Toast.makeText(this, "Shop not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check if we should open a specific tab
        val openTab = intent.getStringExtra("open_tab")
        if (openTab != null) {
            when (openTab) {
                "events" -> tabLayout.getTabAt(1)?.select()
                "tables" -> tabLayout.getTabAt(2)?.select()
            }
        }

        // Set up follow button
        followButton.setOnClickListener {
            toggleFollowStatus()
        }

        // Set up tab listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Handle tab selection
                // In a real implementation, this would swap fragments or update the UI
                when (tab?.position) {
                    0 -> {
                        // Show shop info tab
                    }
                    1 -> {
                        // Show events tab
                    }
                    2 -> {
                        // Show tables tab
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Not used
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Not used
            }
        })

        // Load shop data
        loadShopData()

        // Check if user is following this shop
        checkFollowStatus()

        // Check if this is from a notification
        val notificationId = intent.getStringExtra("notification_id")
        if (notificationId != null) {
            // Track notification open
            lifecycleScope.launch {
                NotificationManager.trackNotificationOpen(notificationId)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadShopData() {
        // Show loading
        contentContainer.visibility = View.GONE
        loadingView.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val shopDoc = FirebaseFirestore.getInstance()
                    .collection("Stores")
                    .document(shopId!!)
                    .get()
                    .await()

                if (!shopDoc.exists()) {
                    Toast.makeText(this@ShopDetailActivity, "Shop not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                // Update UI with shop data
                val shopData = shopDoc.data
                if (shopData != null) {
                    shopNameTextView.text = shopData["storeName"] as? String ?: "Unknown Shop"

                    // Format address
                    val address = shopData["address"] as? String ?: ""
                    val city = shopData["city"] as? String ?: ""
                    val county = shopData["county"] as? String ?: ""
                    val postCode = shopData["postCode"] as? String ?: ""

                    val fullAddress = StringBuilder()
                    if (address.isNotEmpty()) fullAddress.append(address)
                    if (city.isNotEmpty()) {
                        if (fullAddress.isNotEmpty()) fullAddress.append(", ")
                        fullAddress.append(city)
                    }
                    if (county.isNotEmpty()) {
                        if (fullAddress.isNotEmpty()) fullAddress.append(", ")
                        fullAddress.append(county)
                    }
                    if (postCode.isNotEmpty()) {
                        if (fullAddress.isNotEmpty()) fullAddress.append(", ")
                        fullAddress.append(postCode)
                    }

                    shopAddressTextView.text = fullAddress.toString()

                    // Set description
                    shopDescriptionTextView.text = shopData["description"] as? String ?: "No description available"

                    // Set activity title
                    supportActionBar?.title = shopData["storeName"] as? String ?: "Shop Details"
                }

                // Show content
                contentContainer.visibility = View.VISIBLE
                loadingView.visibility = View.GONE

            } catch (e: Exception) {
                // Handle error
                Log.e(TAG, "Error loading shop data", e)
                Toast.makeText(this@ShopDetailActivity, "Error loading shop data", Toast.LENGTH_SHORT).show()

                // Show content anyway (it will be empty)
                contentContainer.visibility = View.VISIBLE
                loadingView.visibility = View.GONE
            }
        }
    }

    private fun checkFollowStatus() {
        lifecycleScope.launch {
            try {
                isFollowing = NotificationManager.isFollowingShop(shopId!!)
                updateFollowButton()
            } catch (e: Exception) {
                Log.e(TAG, "Error checking follow status", e)
            }
        }
    }

    private fun toggleFollowStatus() {
        lifecycleScope.launch {
            try {
                // Show loading state on button
                followButton.isEnabled = false
                followButton.text = if (isFollowing) "Unfollowing..." else "Following..."

                val success = if (isFollowing) {
                    NotificationManager.unfollowShop(shopId!!)
                } else {
                    NotificationManager.followShop(shopId!!)
                }

                if (success) {
                    // Update UI
                    isFollowing = !isFollowing
                    updateFollowButton()

                    // Show success message
                    val message = if (isFollowing) {
                        "You'll receive notifications from this shop"
                    } else {
                        "You won't receive notifications from this shop"
                    }
                    Toast.makeText(this@ShopDetailActivity, message, Toast.LENGTH_SHORT).show()
                } else {
                    // Show error and revert button state
                    Toast.makeText(
                        this@ShopDetailActivity,
                        "Failed to ${if (isFollowing) "unfollow" else "follow"} shop",
                        Toast.LENGTH_SHORT
                    ).show()
                    updateFollowButton()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error toggling follow status", e)
                Toast.makeText(
                    this@ShopDetailActivity,
                    "Error updating follow status",
                    Toast.LENGTH_SHORT
                ).show()

                // Re-enable button
                updateFollowButton()
            }
        }
    }

    private fun updateFollowButton() {
        followButton.isEnabled = true

        if (isFollowing) {
            followButton.text = "Unfollow"
            followButton.setBackgroundResource(android.R.color.darker_gray)
        } else {
            followButton.text = "Follow"
            followButton.setBackgroundResource(R.color.purple_500)
        }
    }
}