package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firestoreId: String = "",
    val productId: Int,
    val catalogType: String,
    val productName: String,
    val category: String = "",
    val brand: String = "",
    val unit: String = "",
    val initialPrice: Double,
    val targetPrice: Double,
    val currentPrice: Double,
    val isTriggered: Boolean = false,
    val lastTriggeredPrice: Double = 0.0,
    val lastNotifiedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val userEmail: String = "",
    val isSynced: Boolean = false
)
