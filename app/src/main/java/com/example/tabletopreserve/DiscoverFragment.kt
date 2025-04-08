package com.example.tabletopreserve

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tabletopreserve.models.Shop
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class DiscoverFragment : Fragment(), ShopAdapter.OnShopClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var searchEditText: EditText
    private lateinit var db: FirebaseFirestore

    private val allShops = mutableListOf<Shop>()
    private val filteredShops = mutableListOf<Shop>()
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
        searchEditText = view.findViewById(R.id.search_edit_text)

        // Set up RecyclerView
        adapter = ShopAdapter(requireContext(), filteredShops, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Set up search functionality
        setupSearch()

        // Load shops
        loadShops()

        return view
    }

    private fun setupSearch() {
        // Listen for text changes in the search field
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                filterShops(s.toString())
            }
        })

        // Handle search action on keyboard
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Hide keyboard if needed
                val query = searchEditText.text.toString()
                filterShops(query)
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun filterShops(query: String) {
        filteredShops.clear()

        if (query.isEmpty()) {
            // If search query is empty, show all shops
            filteredShops.addAll(allShops)
        } else {
            // Filter shops based on name containing the search query (case insensitive)
            val lowercaseQuery = query.lowercase(Locale.getDefault())

            for (shop in allShops) {
                if (shop.storeName.lowercase(Locale.getDefault()).contains(lowercaseQuery)) {
                    filteredShops.add(shop)
                }
            }
        }

        // Update RecyclerView
        adapter.notifyDataSetChanged()

        // Show/hide empty view if no results
        if (filteredShops.isEmpty()) {
            if (query.isNotEmpty()) {
                emptyView.text = "No shops found matching \"$query\""
            } else {
                emptyView.text = getString(R.string.no_shops_found)
            }
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun loadShops() {
        progressBar.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        recyclerView.visibility = View.GONE

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
                    // Clear existing data
                    allShops.clear()
                    filteredShops.clear()

                    // Parse shops data from Firestore
                    for (document in documents) {
                        try {
                            val shop = document.toObject(Shop::class.java).copy(id = document.id)
                            allShops.add(shop)
                            filteredShops.add(shop)
                            Log.d(TAG, "Shop loaded: ${shop.storeName}, Logo URL: ${shop.logoUrl}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing shop data: ${e.message}")
                        }
                    }

                    // Sort shops alphabetically by name
                    allShops.sortBy { it.storeName }
                    filteredShops.sortBy { it.storeName }

                    // Notify adapter of data change
                    adapter.notifyDataSetChanged()

                    // Show shops
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE

                    // Apply any existing search filter
                    if (searchEditText.text.isNotEmpty()) {
                        filterShops(searchEditText.text.toString())
                    }
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
        // FIXED: Now properly navigate to shop detail activity
        Log.d(TAG, "Shop clicked: ${shop.id} - ${shop.storeName}")
        val intent = Intent(requireContext(), ShopDetailActivity::class.java)
        intent.putExtra("shop_id", shop.id)
        startActivity(intent)
    }

    override fun onBookClick(shop: Shop) {
        // Navigate to table booking activity
        val intent = Intent(context, TableListActivity::class.java)
        intent.putExtra(TableListActivity.EXTRA_SHOP, shop)
        startActivity(intent)
    }
}