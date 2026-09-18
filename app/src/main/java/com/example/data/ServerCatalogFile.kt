package com.example.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Represents a catalog file uploaded to the Cloud Server by an Administrator,
 * which standard users can discover and synchronize with.
 */
data class ServerCatalogFile(
    val id: String = "",
    val fileName: String = "",
    val fileType: String = "XLSX", // XLSX, CSV, PDF, JSON
    val catalogType: String = "DREAMPRICE", // DREAMPRICE, INTERMART, SUPER_U, WINNERS, WAY, etc.
    val description: String = "",
    val uploadedBy: String = "", // Admin email, e.g. ozeerhassan@gmail.com
    val uploadedByName: String = "Administrateur",
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
    val fileSizeBytes: Long = 0L,
    val fileSizeFormatted: String = "0 KB",
    val productCount: Int = 0,
    val version: Int = 1,
    val isPublished: Boolean = true,
    val fileUrl: String? = null,
    val sampleProductsPreview: List<String> = emptyList(),
    val products: List<ProductEntity> = emptyList()
)
