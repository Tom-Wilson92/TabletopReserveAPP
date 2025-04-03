package com.example.tabletopreserve

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tabletopreserve.models.Shop
import com.google.firebase.firestore.FirebaseFirestore

class TableListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var shopNameTextView: TextView
    private lateinit var shopAddressTextView: TextView
    private lateinit var shop: Shop
    private val tablesList = mutableListOf<Map<String, Any>>()

    companion object {
        private const val TAG = "TableListActivity"
        const val EXTRA_SHOP = "extra_shop"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_table_list)

        // Get shop from intent
        shop = intent.getSerializableExtra(EXTRA_SHOP) as Shop

        // Initialize views
        recyclerView = findViewById(R.id.tables_recycler_view)
        progressBar = findViewById(R.id.progress_bar)
        emptyView = findViewById(R.id.empty_view)
        shopNameTextView = findViewById(R.id.shop_name)
        shopAddressTextView = findViewById(R.id.shop_address)

        // Set shop details
        shopNameTextView.text = shop.storeName
        val address = shop.address + (if (shop.city.isNotEmpty()) ", ${shop.city}" else "")
        shopAddressTextView.text = address

        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Available Tables"

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load tables
        loadTables()
    }

    private fun loadTables() {
        progressBar.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        recyclerView.visibility = View.GONE

        val db = FirebaseFirestore.getInstance()
        db.collection("Tables")
            .whereEqualTo("shopId", shop.id)
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE

                if (documents.isEmpty) {
                    // No tables found
                    emptyView.visibility = View.VISIBLE
                } else {
                    // Tables found
                    recyclerView.visibility = View.VISIBLE

                    // Convert documents to simple maps
                    tablesList.clear()
                    for (document in documents) {
                        val tableData = document.data
                        tableData["id"] = document.id  // Add document ID
                        tablesList.add(tableData)
                    }

                    // Set up adapter
                    val adapter = TableAdapter(tablesList) { table ->
                        // Get table details
                        val tableId = table["id"] as String
                        val tableNumber = table["tableNumber"] as? Long ?: 0
                        val tableType = table["tableType"] as? String ?: "Standard"

                        // Launch reservation activity
                        val intent = Intent(this, ReservationActivity::class.java)
                        intent.putExtra(ReservationActivity.EXTRA_SHOP, shop)
                        intent.putExtra(ReservationActivity.EXTRA_TABLE_ID, tableId)
                        intent.putExtra(ReservationActivity.EXTRA_TABLE_NUMBER, tableNumber.toInt())
                        intent.putExtra(ReservationActivity.EXTRA_TABLE_TYPE, tableType)
                        startActivity(intent)
                    }
                    recyclerView.adapter = adapter
                }
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                emptyView.text = "Error loading tables: ${exception.message}"
                Log.e(TAG, "Error loading tables", exception)
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}