package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loyalty_cards")
data class LoyaltyCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val storeName: String,       // e.g. "Lolo", "Intermart", "Dreamprice", "Super U", "Winners"
    val cardNumber: String,      // e.g. "1234567890"
    val cardHolderName: String,  // e.g. "Hassan Ozeer"
    val barcodeType: String,     // "QR_CODE", "CODE_128", "EAN_13"
    val colorHex: String,        // Hex color string for card background
    val cardImagePath: String? = null, // Optional image path of the fidelity card
    val createdAt: Long = System.currentTimeMillis()
)

