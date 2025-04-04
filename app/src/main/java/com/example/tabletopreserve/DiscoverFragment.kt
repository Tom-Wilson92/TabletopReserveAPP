package com.example.tabletopreserve

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tabletopreserve.models.Shop
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DiscoverFragment : Fragment(), ShopAdapter.OnShopClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var db: FirebaseFirestore
    private val shops = mutableListOf<Shop>()
    private lateinit var adapter: ShopAdapter

    companion object {
        private const val TAG = "DiscoverFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_discover, container, false)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize views
        recyclerView = view.findViewById(R.id.shops_recycler_view)
        progressBar = view.findViewById(R.id.progress_bar)
        emptyView = view.findViewById(R.id.empty_view)

        // Set up RecyclerView
        adapter = ShopAdapter(requireContext(), shops, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Load shops
        loadShops()

        return view
    }

    private fun loadShops() {
        progressBar.visibility = View.VISIBLE

        // Query approved shops from Firestore - using the schema's field names
        db.collection("Stores")
            .whereEqualTo("isApproved", true)
            // Optional: you can also filter by registrationStatus
            .whereEqualTo("registrationStatus", "approved")
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE

                if (documents.isEmpty) {
                    // No shops found
                    emptyView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    // Shops found
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE

                    // Clear existing data
                    shops.clear()

                    // Parse shops data from Firestore
                    for (document in documents) {
                        try {
                            val shop = document.toObject(Shop::class.java).copy(id = document.id)
                            shops.add(shop)
                            Log.d(TAG, "Shop loaded: ${shop.storeName}, Logo URL: ${shop.logoUrl}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing shop data: ${e.message}")
                        }
                    }

                    // Notify adapter of data change
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                emptyView.text = "Error loading shops: ${exception.message}"
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                Log.e(TAG, "Error loading shops", exception)
            }
    }

    // ShopAdapter.OnShopClickListener implementation
    override fun onShopClick(shop: Shop) {
        // Navigate to shop detail activity
        Toast.makeText(context, "Selected shop: ${shop.storeName}", Toast.LENGTH_SHORT).show()

        // Here you could implement a detailed shop view
        // val intent = Intent(context, ShopDetailActivity::class.java)
        // intent.putExtra("SHOP_ID", shop.id)
        // startActivity(intent)
    }

    override fun onBookClick(shop: Shop) {
        // Navigate to table booking activity
        val intent = Intent(context, TableListActivity::class.java)
        intent.putExtra(TableListActivity.EXTRA_SHOP, shop)
        startActivity(intent)
    }
}