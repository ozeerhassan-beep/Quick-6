package com.example.util

/**
 * App Edition Configuration
 * 
 * Allows compiling or running either:
 * - CLIENT: The customer-facing shopping & scanning application.
 * - ADMIN: The administration, catalog upload, and user tier management console.
 */
enum class AppEdition {
    CLIENT,
    ADMIN;

    val isClient: Boolean get() = this == CLIENT
    val isAdmin: Boolean get() = this == ADMIN
}

object AppEditionConfig {
    /**
     * Change this constant to compile either the CLIENT APK or the ADMIN APK:
     * - AppEdition.CLIENT -> Consumer shopping app (Scanner, Cart, Catalog, Price Alerts)
     * - AppEdition.ADMIN  -> Store management app (User Tier Manager, Data Sync, Analytics, Catalog Upload)
     */
    var currentEdition: AppEdition = try {
        AppEdition.valueOf(com.example.BuildConfig.APP_EDITION)
    } catch (e: Exception) {
        AppEdition.CLIENT
    }

    val appName: String
        get() = when (currentEdition) {
            AppEdition.CLIENT -> "KwickArt Client"
            AppEdition.ADMIN -> "KwickArt Admin Console"
        }

    val defaultPackageId: String
        get() = when (currentEdition) {
            AppEdition.CLIENT -> "com.kwickart.shop"
            AppEdition.ADMIN -> "com.kwickart.admin"
        }
}
