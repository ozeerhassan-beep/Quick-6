package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wishlist_items")
data class WishlistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val catalogType: String,
    val productName: String,
    val category: String,
    val brand: String = "",
    val unit: String = "",
    val unitPrice: Double = 0.0,
    val unitCost: Double = 0.0,
    val addedAt: Long = System.currentTimeMillis()
)
