package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    primaryKeys = ["id", "catalogType"],
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["catalogType"]),
        Index(value = ["name"])
    ]
)
data class ProductEntity(
    val id: Int = 0,
    val catalogType: String, // "DREAMPRICE", "INTERMART", "SUPER U", "WINNERS", etc.
    val name: String,
    val category: String,
    val brand: String,
    val unit: String,
    val price: Double,
    val cost: Double,
    val barcode: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false
)
