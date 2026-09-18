package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val catalogType: String,
    val productId: Int,
    val productName: String,
    val category: String,
    val unit: String,
    val unitPrice: Double,
    val unitCost: Double,
    val quantity: Int
)
