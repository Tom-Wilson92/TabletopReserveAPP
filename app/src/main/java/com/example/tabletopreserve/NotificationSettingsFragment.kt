package com.example.tabletopreserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationSettingsFragment : Fragment() {

    private lateinit var notificationsSwitch: Switch
    private lateinit var followedShopsRecyclerView: RecyclerView
    private lateinit var noShopsText: TextView
    private lateinit var followedShopsAdapter: FollowedShopsAdapter
    private val followedShops = mutableListOf<Map<String, Any>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notification_settings, container, false)

        // Initialize views
        notificationsSwitch = view.findViewById(R.id.notifications_switch)
        followedShopsRecyclerView = view.findViewById(R.id.followed_shops_recycler_view)
        noShopsText = view.findViewById(R.id.no_shops_text)

        // Set up RecyclerView
        followedShopsAdapter = FollowedShopsAdapter(followedShops) { shopId ->
            onUnfollowShop(shopId)
        }
        followedShopsRecyclerView.layoutManager = LinearLayoutManager(context)
        followedShopsRecyclerView.adapter = followedShopsAdapter

        // Set initial switch state
        notificationsSwitch.isChecked = context?.let { SharedPreferenceHelper.areNotificationsEnabled(it) } ?: true

        // Set up switch listener
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            onNotificationToggle(isChecked)
        }

        // Load followed shops
        loadFollowedShops()

        return view
    }

    private fun onNotificationToggle(enabled: Boolean) {
        context?.let { ctx ->
            SharedPreferenceHelper.setNotificationsEnabled(ctx, enabled)

            // Update server setting
            lifecycleScope.launch {
                val success = NotificationManager.setNotificationsEnabled(enabled)
                if (!success) {
                    // Show error and revert switch if server update failed
                    Toast.makeText(ctx, "Failed to update notification settings", Toast.LENGTH_SHORT).show()
                    notificationsSwitch.isChecked = !enabled
                }
            }
        }
    }

    private fun onUnfollowShop(shopId: String) {
        lifecycleScope.launch {
            val success = NotificationManager.unfollowShop(shopId)
            if (success) {
                // Remove from local list and update UI
                val indexToRemove = followedShops.indexOfFirst { it["id"] == shopId }
                if (indexToRemove >= 0) {
                    followedShops.removeAt(indexToRemove)
                    followedShopsAdapter.notifyItemRemoved(indexToRemove)

                    // Show empty state if needed
                    if (followedShops.isEmpty()) {
                        noShopsText.visibility = View.VISIBLE
                        followedShopsRecyclerView.visibility = View.GONE
                    }
                }

                Toast.makeText(context, "Shop unfollowed", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to unfollow shop", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadFollowedShops() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            noShopsText.visibility = View.VISIBLE
            followedShopsRecyclerView.visibility = View.GONE
            return
        }

        lifecycleScope.launch {
            try {
                // Get followed shop IDs
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(userId)
                    .get()
                    .await()

                val followedShopIds = userDoc.get("followedShops") as? List<String> ?: listOf()

                if (followedShopIds.isEmpty()) {
                    noShopsText.visibility = View.VISIBLE
                    followedShopsRecyclerView.visibility = View.GONE
                    return@launch
                }

                // Get shop details
                followedShops.clear()
                for (shopId in followedShopIds) {
                    val shopDoc = FirebaseFirestore.getInstance()
                        .collection("Stores")
                        .document(shopId)
                        .get()
                        .await()

                    if (shopDoc.exists()) {
                        val shopData = shopDoc.data?.toMutableMap() ?: mutableMapOf()
                        shopData["id"] = shopId
                        followedShops.add(shopData)
                    }
                }

                // Update UI
                if (followedShops.isEmpty()) {
                    noShopsText.visibility = View.VISIBLE
                    followedShopsRecyclerView.visibility = View.GONE
                } else {
                    noShopsText.visibility = View.GONE
                    followedShopsRecyclerView.visibility = View.VISIBLE
                    followedShopsAdapter.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                noShopsText.visibility = View.VISIBLE
                followedShopsRecyclerView.visibility = View.GONE
                Toast.makeText(context, "Error loading followed shops", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private class FollowedShopsAdapter(
        private val shops: List<Map<String, Any>>,
        private val onUnfollowClick: (String) -> Unit
    ) : RecyclerView.Adapter<FollowedShopsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val shopName: TextView = view.findViewById(R.id.shop_name)
            val shopAddress: TextView = view.findViewById(R.id.shop_address)
            val unfollowBtn: View = view.findViewById(R.id.unfollow_button)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_followed_shop, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val shop = shops[position]

            holder.shopName.text = shop["storeName"] as? String ?: "Unknown Shop"

            // Format address
            val city = shop["city"] as? String ?: ""
            val address = shop["address"] as? String ?: ""
            holder.shopAddress.text = if (address.isNotEmpty() && city.isNotEmpty()) {
                "$address, $city"
            } else {
                address.ifEmpty { city }
            }

            // Set unfollow button click listener
            holder.unfollowBtn.setOnClickListener {
                val shopId = shop["id"] as? String
                if (shopId != null) {
                    onUnfollowClick(shopId)
                }
            }
        }

        override fun getItemCount() = shops.size
    }
}