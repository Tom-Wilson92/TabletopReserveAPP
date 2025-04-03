package com.example.tabletopreserve

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TableAdapter(private val tables: List<Map<String, Any>>,
                   private val onTableClicked: (Map<String, Any>) -> Unit)
    : RecyclerView.Adapter<TableAdapter.TableViewHolder>() {

    class TableViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(android.R.id.text1)
        val subtitleText: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return TableViewHolder(view)
    }

    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        val table = tables[position]

        // Display table number and type
        val tableNumber = table["tableNumber"] as? Long ?: 0
        val tableType = table["tableType"] as? String ?: "Standard"
        holder.titleText.text = "Table $tableNumber - $tableType"

        // Display capacity
        val capacity = table["capacity"] as? Long ?: 0
        holder.subtitleText.text = "Capacity: $capacity people"

        // Set click listener
        holder.itemView.setOnClickListener {
            onTableClicked(table)
        }
    }

    override fun getItemCount() = tables.size
}