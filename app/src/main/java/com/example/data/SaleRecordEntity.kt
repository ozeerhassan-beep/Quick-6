package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_records")
data class SaleRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val catalogType: String,
    val productName: String,
    val category: String,
    val quantity: Int,
    val unitPrice: Double,
    val unitCost: Double,
    val totalPrice: Double,
    val totalProfit: Double,
    val timestamp: Long = System.currentTimeMillis()
)
