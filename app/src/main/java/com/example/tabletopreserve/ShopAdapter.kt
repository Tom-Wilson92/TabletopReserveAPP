package com.example.tabletopreserve

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
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
            // If the shop has a logo URL, load it
            Glide.with(context)
                .load(shop.logoUrl)
                .apply(RequestOptions()
                    .placeholder(R.drawable.default_shop_logo)
                    .error(R.drawable.default_shop_logo)
                    .centerCrop())
                .into(holder.shopImage)
        } else {
            // Set default shop image based on shop type
            val defaultImageResId = getDefaultImageForShopType(shop.shopType)
            Glide.with(context)
                .load(defaultImageResId)
                .centerCrop()
                .into(holder.shopImage)
        }

        // Set click listeners
        holder.itemView.setOnClickListener {
            onShopClickListener.onShopClick(shop)
        }

        holder.bookButton.setOnClickListener {
            onShopClickListener.onBookClick(shop)
        }
    }

    override fun getItemCount(): Int = shops.size

    /**
     * Get the appropriate default image resource based on shop type
     */
    private fun getDefaultImageForShopType(shopType: String): Int {
        return when (shopType) {
            "game-store" -> R.drawable.default_game_store
            "dedicated-tables" -> R.drawable.default_gaming_tables
            "cafe" -> R.drawable.default_gaming_cafe
            "restaurant" -> R.drawable.default_restaurant
            else -> R.drawable.default_shop_logo
        }
    }
}