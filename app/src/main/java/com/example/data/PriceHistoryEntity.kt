package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_history")
data class PriceHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val productId: Int,
    val productName: String,
    val catalogType: String,
    val price: Double,
    val cost: Double = 0.0,
    val recordedDate: String, // e.g. "Mars 2026"
    val timestamp: Long = System.currentTimeMillis()
)
