package com.example.tabletopreserve

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.tabletopreserve.models.Shop

class ShopAdapter(
    private val context: Context,
    private val shops: List<Shop>,
    private val onShopClickListener: OnShopClickListener
) : RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    interface OnShopClickListener {
        fun onShopClick(shop: Shop)
        fun onBookClick(shop: Shop)
    }

    class ShopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val shopImage: ImageView = itemView.findViewById(R.id.shop_image)
        val shopName: TextView = itemView.findViewById(R.id.shop_name)
        val shopAddress: TextView = itemView.findViewById(R.id.shop_address)
        val availableTables: TextView = itemView.findViewById(R.id.available_tables)
        val bookButton: Button = itemView.findViewById(R.id.book_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop, parent, false)
        return ShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val shop = shops[position]

        // Debug log to trace shop information
        Log.d(TAG, "Binding shop: ${shop.id} - ${shop.storeName}")

        // Set shop data to views
        holder.shopName.text = shop.storeName

        // Format address from components
        val fullAddress = buildString {
            append(shop.address)
            if (shop.city.isNotEmpty()) {
                append(", ${shop.city}")
            }
            if (shop.postCode.isNotEmpty()) {
                append(", ${shop.postCode}")
            }
        }
        holder.shopAddress.text = fullAddress

        // For available tables, we'll need to load this from Tables collection
        // For now, set placeholder text
        holder.availableTables.text = "Tables available" // This will be updated later

        // Load shop image with Glide
        if (shop.logoUrl.isNotEmpty()) {
            // Log the URL being used to load the image
            Log.d(TAG, "Loading shop logo from URL: ${shop.logoUrl}")

            // If the shop has a logo URL, load it with improved error handling
            Glide.with(context)
                .load(shop.logoUrl)
                .placeholder(R.drawable.defaultstoreimage)
                .error(R.drawable.defaultstoreimage)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e(TAG, "Failed to load shop logo from ${shop.logoUrl}", e)
                        return false // Allow error placeholder to be set
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d(TAG, "Successfully loaded shop logo from ${shop.logoUrl}")
                        return false // Allow resource to be set on the target
                    }
                })
                .into(holder.shopImage)
        } else {
            // Set default shop image
            Log.d(TAG, "No logo URL available for shop ${shop.id}, using default image")
            Glide.with(context)
                .load(R.drawable.defaultstoreimage)
                .into(holder.shopImage)
        }

        // Set click listeners
        holder.itemView.setOnClickListener {
            Log.d(TAG, "Item click for shop: ${shop.id}")
            onShopClickListener.onShopClick(shop)
        }

        holder.bookButton.setOnClickListener {
            Log.d(TAG, "Book button click for shop: ${shop.id}")
            onShopClickListener.onBookClick(shop)
        }
    }

    override fun getItemCount(): Int = shops.size

    companion object {
        private const val TAG = "ShopAdapter"
    }
}