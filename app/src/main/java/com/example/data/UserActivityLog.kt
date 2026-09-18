package com.example.data

data class UserActivityLog(
    val userId: String,
    val userEmail: String,
    val timestamp: Long,
    val downloadType: String,
    val itemCount: Int,
    val downloadedItemIds: List<Int>
)
