package com.example.tabletopreserve

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShopDetailActivity : AppCompatActivity() {

    private lateinit var shopNameTextView: TextView
    private lateinit var shopAddressTextView: TextView
    private lateinit var shopDescriptionTextView: TextView
    private lateinit var shopImageView: ImageView
    private lateinit var followButton: Button
    private lateinit var tabLayout: TabLayout
    private lateinit var contentContainer: View
    private lateinit var loadingView: View

    // Events tab views
    private lateinit var eventsContainer: View
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var noEventsView: TextView
    private lateinit var eventsProgressBar: ProgressBar
    private val shopEvents = mutableListOf<Map<String, Any>>()
    private lateinit var eventsAdapter: EventsAdapter

    // Tables tab views
    private lateinit var tablesContainer: View
    private lateinit var tablesRecyclerView: RecyclerView
    private lateinit var noTablesView: TextView
    private lateinit var tablesProgressBar: ProgressBar
    private val shopTables = mutableListOf<Map<String, Any>>()
    private lateinit var tablesAdapter: TablesAdapter

    private var shopId: String? = null
    private var isFollowing = false

    companion object {
        private const val TAG = "ShopDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop_detail)

        // Initialize basic views
        shopNameTextView = findViewById(R.id.shop_name)
        shopAddressTextView = findViewById(R.id.shop_address)
        shopDescriptionTextView = findViewById(R.id.shop_description)
        shopImageView = findViewById(R.id.shop_image)
        followButton = findViewById(R.id.follow_button)
        tabLayout = findViewById(R.id.tab_layout)
        contentContainer = findViewById(R.id.content_container)
        loadingView = findViewById(R.id.loading_view)

        // Initialize events and tables views
        initializeEventsViews()
        initializeTablesViews()

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get shop ID from intent
        shopId = intent.getStringExtra("shop_id")
        Log.d(TAG, "Received shop_id: $shopId")

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
            Log.d(TAG, "Follow button clicked for shop: $shopId")
            toggleFollowStatus()
        }

        // Set up tab listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Handle tab selection
                when (tab?.position) {
                    0 -> {
                        // Show shop info tab
                        contentContainer.visibility = View.VISIBLE
                        eventsContainer.visibility = View.GONE
                        tablesContainer.visibility = View.GONE
                        Log.d(TAG, "Info tab selected")
                    }
                    1 -> {
                        // Show events tab
                        contentContainer.visibility = View.GONE
                        eventsContainer.visibility = View.VISIBLE
                        tablesContainer.visibility = View.GONE
                        loadShopEvents()
                        Log.d(TAG, "Events tab selected")
                    }
                    2 -> {
                        // Show tables tab
                        contentContainer.visibility = View.GONE
                        eventsContainer.visibility = View.GONE
                        tablesContainer.visibility = View.VISIBLE
                        loadShopTables()
                        Log.d(TAG, "Tables tab selected")
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

    private fun initializeEventsViews() {
        eventsContainer = findViewById(R.id.events_container)
        eventsRecyclerView = findViewById(R.id.events_recycler_view)
        noEventsView = findViewById(R.id.no_events_view)
        eventsProgressBar = findViewById(R.id.events_progress_bar)

        // Set up RecyclerView
        eventsAdapter = EventsAdapter(shopEvents)
        eventsRecyclerView.layoutManager = LinearLayoutManager(this)
        eventsRecyclerView.adapter = eventsAdapter

        // Initially hide the events container
        eventsContainer.visibility = View.GONE
    }

    private fun initializeTablesViews() {
        tablesContainer = findViewById(R.id.tables_container)
        tablesRecyclerView = findViewById(R.id.tables_recycler_view)
        noTablesView = findViewById(R.id.no_tables_view)
        tablesProgressBar = findViewById(R.id.tables_progress_bar)

        // Set up RecyclerView
        tablesAdapter = TablesAdapter(shopTables) { tableId ->
            // Handle table selection - navigate to reservation screen
            val selectedTable = shopTables.find { it["id"] == tableId }
            if (selectedTable != null) {
                val intent = Intent(this, ReservationActivity::class.java).apply {
                    putExtra("shop_id", shopId)
                    putExtra("table_id", tableId)
                    putExtra("table_number", (selectedTable["tableNumber"] as? Long)?.toInt() ?: 0)
                    putExtra("table_type", selectedTable["tableType"] as? String ?: "")
                }
                startActivity(intent)
            }
        }
        tablesRecyclerView.layoutManager = LinearLayoutManager(this)
        tablesRecyclerView.adapter = tablesAdapter

        // Initially hide the tables container
        tablesContainer.visibility = View.GONE
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
                Log.d(TAG, "Loading data for shop: $shopId")
                val shopDoc = FirebaseFirestore.getInstance()
                    .collection("Stores")
                    .document(shopId!!)
                    .get()
                    .await()

                if (!shopDoc.exists()) {
                    Log.e(TAG, "Shop document doesn't exist for ID: $shopId")
                    Toast.makeText(this@ShopDetailActivity, "Shop not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                // Update UI with shop data
                val shopData = shopDoc.data
                if (shopData != null) {
                    Log.d(TAG, "Shop data loaded: ${shopData["storeName"]}")
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

                    // Load shop image using Glide (same as in ShopAdapter)
                    val logoUrl = shopData["logoUrl"] as? String
                    if (!logoUrl.isNullOrEmpty()) {
                        // If the shop has a logo URL, load it
                        Glide.with(this@ShopDetailActivity)
                            .load(logoUrl)
                            .placeholder(R.drawable.defaultstoreimage)
                            .error(R.drawable.defaultstoreimage)
                            .into(shopImageView)
                    } else {
                        // Set default shop image
                        Glide.with(this@ShopDetailActivity)
                            .load(R.drawable.defaultstoreimage)
                            .into(shopImageView)
                    }
                }

                // Show content
                contentContainer.visibility = View.VISIBLE
                loadingView.visibility = View.GONE

            } catch (e: Exception) {
                // Handle error
                Log.e(TAG, "Error loading shop data", e)
                Toast.makeText(this@ShopDetailActivity, "Error loading shop data: ${e.message}", Toast.LENGTH_SHORT).show()

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
                Log.d(TAG, "Follow status checked: $isFollowing")
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
                    Log.d(TAG, "Attempting to unfollow shop: $shopId")
                    NotificationManager.unfollowShop(shopId!!)
                } else {
                    Log.d(TAG, "Attempting to follow shop: $shopId")
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
                    "Error updating follow status: ${e.message}",
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

    private fun loadShopEvents() {
        if (shopId == null) return

        // Show loading state
        eventsProgressBar.visibility = View.VISIBLE
        eventsRecyclerView.visibility = View.GONE
        noEventsView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading events for shop: $shopId")

                // Get current date to filter past events
                val currentDate = Date()

                // Query Firestore for shop events
                val eventsSnapshot = FirebaseFirestore.getInstance()
                    .collection("Events")
                    .whereEqualTo("shopId", shopId)
                    .whereGreaterThan("date", currentDate) // Only future events
                    .orderBy("date", Query.Direction.ASCENDING)
                    .get()
                    .await()

                Log.d(TAG, "Found ${eventsSnapshot.size()} events")

                // Clear existing events
                shopEvents.clear()

                if (eventsSnapshot.isEmpty) {
                    // No events found
                    eventsProgressBar.visibility = View.GONE
                    eventsRecyclerView.visibility = View.GONE
                    noEventsView.visibility = View.VISIBLE
                    noEventsView.text = "No upcoming events for this shop"
                    return@launch
                }

                // Process events
                for (doc in eventsSnapshot.documents) {
                    val eventData = doc.data?.toMutableMap() ?: mutableMapOf()
                    eventData["id"] = doc.id
                    shopEvents.add(eventData)
                }

                // Update UI
                eventsProgressBar.visibility = View.GONE
                eventsRecyclerView.visibility = View.VISIBLE
                noEventsView.visibility = View.GONE
                eventsAdapter.notifyDataSetChanged()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading shop events", e)
                eventsProgressBar.visibility = View.GONE
                eventsRecyclerView.visibility = View.GONE
                noEventsView.visibility = View.VISIBLE
                noEventsView.text = "Error loading events: ${e.message}"
            }
        }
    }

    private fun loadShopTables() {
        if (shopId == null) return

        // Show loading state
        tablesProgressBar.visibility = View.VISIBLE
        tablesRecyclerView.visibility = View.GONE
        noTablesView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Loading tables for shop: $shopId")

                // Query Firestore for shop tables
                val tablesSnapshot = FirebaseFirestore.getInstance()
                    .collection("Tables")
                    .whereEqualTo("shopId", shopId)
                    .whereEqualTo("isActive", true) // Only active tables
                    .orderBy("tableNumber", Query.Direction.ASCENDING)
                    .get()
                    .await()

                Log.d(TAG, "Found ${tablesSnapshot.size()} tables")

                // Clear existing tables
                shopTables.clear()

                if (tablesSnapshot.isEmpty) {
                    // No tables found
                    tablesProgressBar.visibility = View.GONE
                    tablesRecyclerView.visibility = View.GONE
                    noTablesView.visibility = View.VISIBLE
                    noTablesView.text = "No tables available for this shop"
                    return@launch
                }

                // Process tables
                for (doc in tablesSnapshot.documents) {
                    val tableData = doc.data?.toMutableMap() ?: mutableMapOf()
                    tableData["id"] = doc.id
                    shopTables.add(tableData)
                }

                // Update UI
                tablesProgressBar.visibility = View.GONE
                tablesRecyclerView.visibility = View.VISIBLE
                noTablesView.visibility = View.GONE
                tablesAdapter.notifyDataSetChanged()

            } catch (e: Exception) {
                Log.e(TAG, "Error loading shop tables", e)
                tablesProgressBar.visibility = View.GONE
                tablesRecyclerView.visibility = View.GONE
                noTablesView.visibility = View.VISIBLE
                noTablesView.text = "Error loading tables: ${e.message}"
            }
        }
    }

    // Events adapter class
    private class EventsAdapter(private val events: List<Map<String, Any>>) :
        RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

        class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val eventName: TextView = view.findViewById(R.id.event_name)
            val eventDate: TextView = view.findViewById(R.id.event_date)
            val eventTime: TextView = view.findViewById(R.id.event_time)
            val eventDescription: TextView = view.findViewById(R.id.event_description)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_event, parent, false)
            return EventViewHolder(view)
        }

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            val event = events[position]

            // Set event data to views
            holder.eventName.text = event["name"] as? String ?: "Unknown Event"

            // Format date
            val timestamp = event["date"] as? com.google.firebase.Timestamp
            if (timestamp != null) {
                val eventDate = timestamp.toDate()
                val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                holder.eventDate.text = dateFormat.format(eventDate)
            } else {
                holder.eventDate.text = "Date not available"
            }

            // Set time
            val startTime = event["startTime"] as? String ?: ""
            val endTime = event["endTime"] as? String ?: ""

            if (startTime.isNotEmpty() && endTime.isNotEmpty()) {
                holder.eventTime.text = "$startTime - $endTime"
            } else {
                holder.eventTime.text = "Time not specified"
            }

            // Set description
            holder.eventDescription.text = event["description"] as? String ?: ""

            // If description is empty, hide the TextView
            if (holder.eventDescription.text.isBlank()) {
                holder.eventDescription.visibility = View.GONE
            } else {
                holder.eventDescription.visibility = View.VISIBLE
            }
        }

        override fun getItemCount() = events.size
    }

    // Tables adapter class
    private class TablesAdapter(
        private val tables: List<Map<String, Any>>,
        private val onTableClick: (String) -> Unit
    ) : RecyclerView.Adapter<TablesAdapter.TableViewHolder>() {

        class TableViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tableNumber: TextView = view.findViewById(R.id.table_number)
            val tableType: TextView = view.findViewById(R.id.table_type)
            val tableCapacity: TextView = view.findViewById(R.id.table_capacity)
            val tableDescription: TextView = view.findViewById(R.id.table_description)
            val bookButton: Button = view.findViewById(R.id.book_button)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_table, parent, false)
            return TableViewHolder(view)
        }

        override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
            val table = tables[position]

            // Set table data to views
            holder.tableNumber.text = "Table ${table["tableNumber"]}"

            // Format table type for display
            var tableTypeDisplay = "Standard"
            when (table["tableType"] as? String) {
                "standard" -> tableTypeDisplay = "Standard Gaming Table"
                "large" -> tableTypeDisplay = "Large Gaming Table"
                "rpg" -> tableTypeDisplay = "RPG/D&D Table"
                "miniatures" -> tableTypeDisplay = "Miniatures/Wargaming Table"
                "card" -> tableTypeDisplay = "Card Game Table"
                "private" -> tableTypeDisplay = "Private Room"
                "warhammer" -> tableTypeDisplay = "Warhammer/40K Table"
                "pokemon" -> tableTypeDisplay = "Pokémon TCG Table"
                "magic" -> tableTypeDisplay = "Magic: The Gathering Table"
                "boardgame" -> tableTypeDisplay = "Board Game Table"
            }
            holder.tableType.text = tableTypeDisplay

            // Set capacity
            val capacity = table["capacity"] as? Long ?: 0
            holder.tableCapacity.text = "Capacity: $capacity people"

            // Set description
            val description = table["description"] as? String ?: ""
            if (description.isNotEmpty()) {
                holder.tableDescription.text = description
                holder.tableDescription.visibility = View.VISIBLE
            } else {
                holder.tableDescription.visibility = View.GONE
            }

            // Set click listener for book button
            holder.bookButton.setOnClickListener {
                val tableId = table["id"] as? String
                if (tableId != null) {
                    onTableClick(tableId)
                }
            }

            // Set click listener for the whole item
            holder.itemView.setOnClickListener {
                val tableId = table["id"] as? String
                if (tableId != null) {
                    onTableClick(tableId)
                }
            }
        }

        override fun getItemCount() = tables.size
    }
}