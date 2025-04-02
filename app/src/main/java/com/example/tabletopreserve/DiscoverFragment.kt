package com.example.tabletopreserve

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class DiscoverFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var db: FirebaseFirestore

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
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Load shops
        loadShops()

        return view
    }

    private fun loadShops() {
        progressBar.visibility = View.VISIBLE

        // Query approved shops from Firestore
        db.collection("Stores")
            .whereEqualTo("isApproved", true)
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

                    // TODO: Parse shops and set adapter
                    // This will be implemented when you create the ShopAdapter

                    // For now, just show a placeholder
                    emptyView.text = "Found ${documents.size()} shops"
                    emptyView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                emptyView.text = "Error loading shops: ${exception.message}"
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
    }
}