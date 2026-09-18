package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing all application preferences and customizations.
 * Persisted locally in Room and synced to Cloud Firestore.
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val language: String = "FRENCH",
    val themeMode: String = "LIGHT",
    val primaryColorTheme: String = "ROYAL_BLUE",
    val fontStyleOption: String = "SANS_SERIF",
    val cardShapeOption: String = "ROUNDED_SOFT",
    val layoutDensity: String = "LIST",
    val fontScaleOption: String = "NORMAL",
    val isFirestoreAutoSyncEnabled: Boolean = true,
    val autoAddToCartOnScan: Boolean = false,
    val continuousScanMode: Boolean = false,
    val vibrateOnScan: Boolean = true,
    val isFeaturesEnabled: Boolean = true,
    val isPriceCompareEnabled: Boolean = true,
    val isSmartCartEnabled: Boolean = true,
    val isSavingsAnalyticsEnabled: Boolean = true,
    val isLoyaltyCardsEnabled: Boolean = true,
    val isAiShoppingListEnabled: Boolean = true,
    val isStoreRouteMapEnabled: Boolean = true,
    val isBarcodeScannerEnabled: Boolean = true,
    val isVoiceSearchEnabled: Boolean = true,
    val isPriceAlertsEnabled: Boolean = true,
    val hasCompletedWelcome: Boolean = false,
    val dpiScaleMode: String = "AUTO",
    val dpiCustomScale: Float = 1.0f,
    val lastUpdated: Long = System.currentTimeMillis()
)
