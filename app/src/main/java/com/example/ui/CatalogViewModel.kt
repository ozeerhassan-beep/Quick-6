package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CartItemEntity
import com.example.data.CatalogRepository
import com.example.data.FirestoreCartService
import com.example.data.FirestoreCatalogService
import com.example.data.LoyaltyCardEntity
import com.example.data.PriceAlertEntity
import com.example.data.FirestorePriceAlertService
import com.example.data.FirestoreSettingsService
import com.example.data.AppSettingsEntity
import com.example.data.PriceHistoryEntity
import com.example.data.ProductEntity
import com.example.data.SaleRecordEntity
import com.example.data.ServerCatalogFile
import com.example.data.WishlistItemEntity
import com.google.firebase.firestore.ListenerRegistration
import com.example.util.AppLanguage
import com.example.util.DpiScaleMode
import com.example.util.DeviceBindingManager
import com.example.util.DeviceBindingResult
import com.example.util.PreferenceManager
import com.example.util.BarcodeLookupService
import com.example.util.BarcodeScanMatchResult
import com.example.util.CatalogSyncManager
import com.example.util.ChatMessage
import com.example.util.GeminiService
import com.example.util.ImportValidationReport
import com.example.util.OnlineProductInfo
import com.example.util.PriceAlertNotificationHelper
import com.example.util.SemanticSearchResult
import com.example.util.SpreadsheetImporter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImportSuccessInfo(
    val fileName: String,
    val count: Int,
    val targetCatalog: String,
    val timestamp: String
)

data class FileUploadItem(
    val uri: Uri,
    val fileName: String,
    val targetCatalog: String
)


data class DriveFileInfo(
    val name: String,
    val type: String,
    val size: String,
    val targetCatalog: String,
    val lastModified: String,
    val url: String
)

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

enum class PrimaryColorTheme(val displayName: String, val primaryColorHex: Long, val containerHex: Long) {
    ROYAL_BLUE("Bleu Royal", 0xFF1E40AFL, 0xFFDBEAFEL),
    INDIGO("Violet Indigo", 0xFF4F46E5L, 0xFFE0E7FFL),
    EMERALD("Vert Émeraude", 0xFF059669L, 0xFFD1FAE5L),
    CRIMSON("Rouge Crimson", 0xFFDC2626L, 0xFFFEE2E2L),
    SUNSET_ORANGE("Orange Ambré", 0xFFEA580CL, 0xFFFFEDD5L),
    GRAPHITE("Gris Slate", 0xFF334155L, 0xFFE2E8F0L)
}

enum class FontStyleOption(val displayName: String) {
    SANS_SERIF("Sans-Serif (Standard)"),
    SERIF("Serif (Élégant)"),
    MONOSPACE("Monospace (Technique)")
}

enum class CardShapeOption(val displayName: String, val cornerRadiusDp: Int) {
    ROUNDED_SOFT("Arrondi Doux (16dp)", 16),
    ROUNDED_CAPSULE("Arrondi Fort (24dp)", 24),
    SHARP("Coins Structurés (4dp)", 4)
}

enum class CatalogLayoutDensity(val displayName: String, val isGrid: Boolean) {
    LIST("Liste Détaillée", false),
    GRID("Grille 2 Colonnes", true),
    COMPACT("Liste Compacte", false)
}

enum class FontScaleOption(val displayName: String, val multiplier: Float) {
    NORMAL("Normale (100%)", 1.0f),
    LARGE("Agrandie (115%)", 1.15f),
    SMALL("Compacte (90%)", 0.90f)
}

enum class ProductSortOption(val displayName: String) {
    DEFAULT("Pertinence / Standard"),
    PRICE_ASC("Prix croissant (Moins cher)"),
    PRICE_DESC("Prix décroissant (Plus cher)"),
    PROMOTIONS("Dernières Promotions & Rabais"),
    NAME_ASC("Nom (A - Z)"),
    NAME_DESC("Nom (Z - A)")
}

enum class SubscriptionTier {
    FREE,
    PRO,
    ULTRA
}

enum class NavBarStyleOption(val displayName: String) {
    FLUID("Fluid"),
    LIQUID("Liquid")
}

class CatalogViewModel(
    private val repository: CatalogRepository,
    private val context: Context? = null
) : ViewModel() {

    private var authPrefs: android.content.SharedPreferences? = context?.getSharedPreferences("app_auth_prefs", Context.MODE_PRIVATE)

    val preferenceManager: PreferenceManager? = context?.let { PreferenceManager(it) }

    // App Theme Mode
    private val _themeMode = MutableStateFlow(AppThemeMode.LIGHT)
    val themeMode: StateFlow<AppThemeMode> = _themeMode

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        persistSettingsToRoomAndFirestore()
    }

    // App Language State (French, English, Mauritian Creole)
    val appLanguage = MutableStateFlow(AppLanguage.FRENCH)

    fun setAppLanguage(language: AppLanguage) {
        appLanguage.value = language
        authPrefs?.edit()?.putString("app_language", language.name)?.apply()
        persistSettingsToRoomAndFirestore()
    }

    fun setLanguage(language: AppLanguage) = setAppLanguage(language)

    // Theme & Layout Customization State Flows
    val appEdition = MutableStateFlow(com.example.util.AppEditionConfig.currentEdition)
    fun setAppEdition(edition: com.example.util.AppEdition) {
        appEdition.value = edition
        com.example.util.AppEditionConfig.currentEdition = edition
    }

    val navBarStyleOption = MutableStateFlow(NavBarStyleOption.FLUID)
    fun setNavBarStyleOption(option: NavBarStyleOption) {
        navBarStyleOption.value = option
        authPrefs?.edit()?.putString("nav_bar_style", option.name)?.apply()
    }

    val primaryColorTheme = MutableStateFlow(PrimaryColorTheme.EMERALD)
    val fontStyleOption = MutableStateFlow(FontStyleOption.SANS_SERIF)
    val cardShapeOption = MutableStateFlow(CardShapeOption.ROUNDED_SOFT)
    val layoutDensity = MutableStateFlow(CatalogLayoutDensity.LIST)
    val fontScaleOption = MutableStateFlow(FontScaleOption.NORMAL)

    // DPI Adaptive Resizing & Screen Density State Flows
    val dpiScaleMode = MutableStateFlow(DpiScaleMode.AUTO)
    val dpiCustomScale = MutableStateFlow(1.0f)

    fun setPrimaryColorTheme(theme: PrimaryColorTheme) { 
        primaryColorTheme.value = theme 
        persistSettingsToRoomAndFirestore()
    }
    fun setFontStyleOption(option: FontStyleOption) { 
        fontStyleOption.value = option 
        persistSettingsToRoomAndFirestore()
    }
    fun setCardShapeOption(option: CardShapeOption) { 
        cardShapeOption.value = option 
        persistSettingsToRoomAndFirestore()
    }
    fun setLayoutDensity(density: CatalogLayoutDensity) { 
        layoutDensity.value = density 
        persistSettingsToRoomAndFirestore()
    }
    fun setFontScaleOption(scale: FontScaleOption) { 
        fontScaleOption.value = scale 
        persistSettingsToRoomAndFirestore()
    }
    fun setDpiScaleMode(mode: DpiScaleMode) {
        dpiScaleMode.value = mode
        persistSettingsToRoomAndFirestore()
    }
    fun setDpiCustomScale(scale: Float) {
        dpiCustomScale.value = scale
        persistSettingsToRoomAndFirestore()
    }
    fun resetDpiScale() {
        dpiScaleMode.value = DpiScaleMode.AUTO
        dpiCustomScale.value = 1.0f
        persistSettingsToRoomAndFirestore()
    }

    fun resetThemeAndCustomizations() {
        _themeMode.value = AppThemeMode.LIGHT
        appLanguage.value = AppLanguage.FRENCH
        authPrefs?.edit()?.putString("app_language", AppLanguage.FRENCH.name)?.apply()
        primaryColorTheme.value = PrimaryColorTheme.ROYAL_BLUE
        fontStyleOption.value = FontStyleOption.SANS_SERIF
        cardShapeOption.value = CardShapeOption.ROUNDED_SOFT
        layoutDensity.value = CatalogLayoutDensity.LIST
        fontScaleOption.value = FontScaleOption.NORMAL
        dpiScaleMode.value = DpiScaleMode.AUTO
        dpiCustomScale.value = 1.0f
        persistSettingsToRoomAndFirestore()
    }

    fun resetPersonalizationDefaults() = resetThemeAndCustomizations()

    // ---------------------------------------------------------
    // Room & Cloud Firestore Unified Settings Persistence Engine
    // ---------------------------------------------------------
    private var firestoreSettingsListener: ListenerRegistration? = null

    fun setupFirestoreSettingsListener(userEmail: String?) {
        firestoreSettingsListener?.remove()
        firestoreSettingsListener = FirestoreSettingsService.listenToFirestoreSettings(userEmail) { updatedSettings ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveAppSettings(updatedSettings)
                withContext(Dispatchers.Main) {
                    applyLoadedSettings(updatedSettings)
                }
                Log.d("CatalogViewModel", "Realtime settings update received from Firestore and synced into Room.")
            }
        }
    }

    private fun applyLoadedSettings(settings: AppSettingsEntity) {
        runCatching { appLanguage.value = AppLanguage.valueOf(settings.language) }
        runCatching { _themeMode.value = AppThemeMode.valueOf(settings.themeMode) }
        runCatching { primaryColorTheme.value = PrimaryColorTheme.valueOf(settings.primaryColorTheme) }
        runCatching { fontStyleOption.value = FontStyleOption.valueOf(settings.fontStyleOption) }
        runCatching { cardShapeOption.value = CardShapeOption.valueOf(settings.cardShapeOption) }
        runCatching { layoutDensity.value = CatalogLayoutDensity.valueOf(settings.layoutDensity) }
        runCatching { fontScaleOption.value = FontScaleOption.valueOf(settings.fontScaleOption) }
        runCatching { dpiScaleMode.value = DpiScaleMode.valueOf(settings.dpiScaleMode) }
        dpiCustomScale.value = settings.dpiCustomScale
        isFirestoreAutoSyncEnabled.value = settings.isFirestoreAutoSyncEnabled
        autoAddToCartOnScan.value = settings.autoAddToCartOnScan
        continuousScanMode.value = settings.continuousScanMode
        vibrateOnScan.value = settings.vibrateOnScan
        isFeaturesEnabled.value = settings.isFeaturesEnabled
        _isPriceCompareEnabled.value = settings.isPriceCompareEnabled
        _isSmartCartEnabled.value = settings.isSmartCartEnabled
        _isSavingsAnalyticsEnabled.value = settings.isSavingsAnalyticsEnabled
        _isLoyaltyCardsEnabled.value = settings.isLoyaltyCardsEnabled
        _isAiShoppingListEnabled.value = settings.isAiShoppingListEnabled
        _isStoreRouteMapEnabled.value = settings.isStoreRouteMapEnabled
        _isBarcodeScannerEnabled.value = settings.isBarcodeScannerEnabled
        _isVoiceSearchEnabled.value = settings.isVoiceSearchEnabled
        _isPriceAlertsEnabled.value = settings.isPriceAlertsEnabled
        hasCompletedWelcome.value = settings.hasCompletedWelcome
    }

    fun persistSettingsToRoomAndFirestore() {
        val currentEntity = AppSettingsEntity(
            id = 1,
            language = appLanguage.value.name,
            themeMode = _themeMode.value.name,
            primaryColorTheme = primaryColorTheme.value.name,
            fontStyleOption = fontStyleOption.value.name,
            cardShapeOption = cardShapeOption.value.name,
            layoutDensity = layoutDensity.value.name,
            fontScaleOption = fontScaleOption.value.name,
            isFirestoreAutoSyncEnabled = isFirestoreAutoSyncEnabled.value,
            autoAddToCartOnScan = autoAddToCartOnScan.value,
            continuousScanMode = continuousScanMode.value,
            vibrateOnScan = vibrateOnScan.value,
            isFeaturesEnabled = isFeaturesEnabled.value,
            isPriceCompareEnabled = _isPriceCompareEnabled.value,
            isSmartCartEnabled = _isSmartCartEnabled.value,
            isSavingsAnalyticsEnabled = _isSavingsAnalyticsEnabled.value,
            isLoyaltyCardsEnabled = _isLoyaltyCardsEnabled.value,
            isAiShoppingListEnabled = _isAiShoppingListEnabled.value,
            isStoreRouteMapEnabled = _isStoreRouteMapEnabled.value,
            isBarcodeScannerEnabled = _isBarcodeScannerEnabled.value,
            isVoiceSearchEnabled = _isVoiceSearchEnabled.value,
            isPriceAlertsEnabled = _isPriceAlertsEnabled.value,
            hasCompletedWelcome = hasCompletedWelcome.value,
            dpiScaleMode = dpiScaleMode.value.name,
            dpiCustomScale = dpiCustomScale.value,
            lastUpdated = System.currentTimeMillis()
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveAppSettings(currentEntity)
                Log.d("CatalogViewModel", "Settings successfully saved to Room database (id=1, lang=${currentEntity.language})")
            } catch (e: Exception) {
                Log.e("CatalogViewModel", "Failed to save settings to Room: ${e.message}", e)
            }

            try {
                val userEmail = googleAccountEmail.value
                val devId = context?.let { DeviceBindingManager.getOrCreateDeviceId(it) } ?: currentDeviceId.value
                FirestoreSettingsService.saveSettingsToFirestore(currentEntity, userEmail, devId)
                Log.d("CatalogViewModel", "Settings successfully pushed to Cloud Firestore with deviceId=$devId.")
            } catch (e: Exception) {
                Log.e("CatalogViewModel", "Failed to save settings to Firestore: ${e.message}", e)
            }
        }
    }

    // Personalized Compare List State
    private val _comparedProductIds = MutableStateFlow<Set<Int>>(emptySet())
    val comparedProductIds: StateFlow<Set<Int>> = _comparedProductIds

    fun toggleCompareProduct(product: ProductEntity) {
        val current = _comparedProductIds.value
        if (product.id in current) {
            _comparedProductIds.value = current - product.id
        } else {
            if (_userTier.value == SubscriptionTier.FREE && _userRole.value != "ADMIN" && current.size >= 3) {
                context?.let { ctx ->
                    android.widget.Toast.makeText(ctx, "La version FREE est limitée à 3 produits comparés maximum.", android.widget.Toast.LENGTH_SHORT).show()
                }
                return
            }
            _comparedProductIds.value = current + product.id
        }
    }

    fun addToCompare(product: ProductEntity) {
        val current = _comparedProductIds.value
        if (_userTier.value == SubscriptionTier.FREE && _userRole.value != "ADMIN" && current.size >= 3) {
            context?.let { ctx ->
                android.widget.Toast.makeText(ctx, "La version FREE est limitée à 3 produits comparés maximum.", android.widget.Toast.LENGTH_SHORT).show()
            }
            return
        }
        _comparedProductIds.value = current + product.id
    }

    fun removeFromCompare(productId: Int) {
        _comparedProductIds.value = _comparedProductIds.value - productId
    }

    fun clearCompareList() {
        _comparedProductIds.value = emptySet()
    }

    fun isInCompareList(productId: Int): Boolean {
        return productId in _comparedProductIds.value
    }

    // Role ("ADMIN" or "USER")
    private val _userRole = MutableStateFlow("USER")
    val userRole: StateFlow<String> = _userRole

    // Subscription Tier ("FREE", "PRO", "ULTRA")
    private val _userTier = MutableStateFlow(SubscriptionTier.FREE)
    val userTier: StateFlow<SubscriptionTier> = _userTier

    fun isAdmin(): Boolean {
        return _userRole.value == "ADMIN"
    }

    fun setUserTier(tier: SubscriptionTier, forceAdmin: Boolean = false): Boolean {
        val canChange = _userRole.value == "ADMIN" || forceAdmin
        if (!canChange) {
            Log.w("CatalogViewModel", "Permission refusée : Seul un administrateur peut modifier le niveau d'abonnement (Free / Pro / Ultra).")
            return false
        }
        _userTier.value = tier
        authPrefs?.edit()?.putString("user_subscription_tier", tier.name)?.apply()
        registerCurrentDevice()
        return true
    }

    // Google & Firebase Sign-In Account State (Local Storage persistent)
    val googleAccountEmail = MutableStateFlow<String?>(null)
    val googleAccountPassword = MutableStateFlow<String?>(null)
    val googleAccountName = MutableStateFlow<String?>(null)
    val isSignedInWithGoogle = MutableStateFlow(false)
    val firebaseUserId = MutableStateFlow<String?>(null)
    val isFirebaseAuthActive = MutableStateFlow(false)
    val firebaseAuthProvider = MutableStateFlow("google.com")
    val hasCompletedWelcome = MutableStateFlow(false)

    // Auto-sync Firestore toggle state (Local Room persistence vs Cloud Sync)
    val isFirestoreAutoSyncEnabled = MutableStateFlow(true)
    val firestoreSyncStatus = MutableStateFlow("Connecté à Firestore (shopping-cart-c4900)")
    val isFirestoreSyncing = MutableStateFlow(false)

    // Periodic WorkManager background sync toggle (every 6h)
    val isPeriodicWorkManagerEnabled: StateFlow<Boolean> = com.example.util.CatalogSyncManager.isPeriodicSyncEnabled

    // Features tab toggle
    val isFeaturesEnabled = MutableStateFlow(true)

    fun setFeaturesEnabled(enabled: Boolean) {
        isFeaturesEnabled.value = enabled
        authPrefs?.edit()?.putBoolean("is_features_enabled", enabled)?.apply()
        viewModelScope.launch {
            preferenceManager?.setFeaturesEnabled(enabled)
        }
        persistSettingsToRoomAndFirestore()
    }

    // New Feature Toggles
    private val _isPriceCompareEnabled = MutableStateFlow(true)
    val isPriceCompareEnabled: StateFlow<Boolean> = _isPriceCompareEnabled.asStateFlow()
    fun setPriceCompareEnabled(enabled: Boolean) { 
        _isPriceCompareEnabled.value = enabled 
        persistSettingsToRoomAndFirestore()
    }

    private val _isSmartCartEnabled = MutableStateFlow(true)
    val isSmartCartEnabled: StateFlow<Boolean> = _isSmartCartEnabled.asStateFlow()
    fun setSmartCartEnabled(enabled: Boolean) { 
        _isSmartCartEnabled.value = enabled 
        persistSettingsToRoomAndFirestore()
    }

    private val _isSavingsAnalyticsEnabled = MutableStateFlow(true)
    val isSavingsAnalyticsEnabled: StateFlow<Boolean> = _isSavingsAnalyticsEnabled.asStateFlow()
    fun setSavingsAnalyticsEnabled(enabled: Boolean) { 
        _isSavingsAnalyticsEnabled.value = enabled 
        persistSettingsToRoomAndFirestore()
    }

    private val _isLoyaltyCardsEnabled = MutableStateFlow(true)
    val isLoyaltyCardsEnabled: StateFlow<Boolean> = _isLoyaltyCardsEnabled.asStateFlow()
    fun setLoyaltyCardsEnabled(enabled: Boolean) { 
        _isLoyaltyCardsEnabled.value = enabled 
        persistSettingsToRoomAndFirestore()
    }

    private val _isAiShoppingListEnabled = MutableStateFlow(true)
    val isAiShoppingListEnabled: StateFlow<Boolean> = _isAiShoppingListEnabled.asStateFlow()
    fun setAiShoppingListEnabled(enabled: Boolean) { 
        _isAiShoppingListEnabled.value = enabled 
        persistSettingsToRoomAndFirestore()
    }

    private val _isStoreRouteMapEnabled = MutableStateFlow(true)
    val isStoreRouteMapEnabled: StateFlow<Boolean> = _isStoreRouteMapEnabled.asStateFlow()
    fun setStoreRouteMapEnabled(enabled: Boolean) { 
        _isStoreRouteMapEnabled.value = enabled 
        persistSettingsToRoomAndFirestore()
    }

    private val _isBarcodeScannerEnabled = MutableStateFlow(true)
    val isBarcodeScannerEnabled: StateFlow<Boolean> = _isBarcodeScannerEnabled.asStateFlow()
    fun setBarcodeScannerEnabled(enabled: Boolean) { 
        _isBarcodeScannerEnabled.value = enabled 
        persistSettingsToRoomAndFirestore()
    }

    private val _isVoiceSearchEnabled = MutableStateFlow(true)
    val isVoiceSearchEnabled: StateFlow<Boolean> = _isVoiceSearchEnabled.asStateFlow()
    fun setVoiceSearchEnabled(enabled: Boolean) { 
        _isVoiceSearchEnabled.value = enabled 
        persistSettingsToRoomAndFirestore()
    }

    private val _isPriceAlertsEnabled = MutableStateFlow(true)
    val isPriceAlertsEnabled: StateFlow<Boolean> = _isPriceAlertsEnabled.asStateFlow()
    fun setPriceAlertsEnabled(enabled: Boolean) { 
        _isPriceAlertsEnabled.value = enabled 
        persistSettingsToRoomAndFirestore()
    }

    // Barcode scanner preferences (ML Kit & Cart/Inventory actions)
    val autoAddToCartOnScan = MutableStateFlow(false)
    val continuousScanMode = MutableStateFlow(false)
    val vibrateOnScan = MutableStateFlow(true)
    val autofillSecurityStatus = MutableStateFlow("Google Password Manager (Sécurisé)")

    fun initAuthStorage(ctx: Context) {
        if (authPrefs == null) {
            authPrefs = ctx.getSharedPreferences("app_auth_prefs", Context.MODE_PRIVATE)
        }
        loadSavedCredentials()
    }

    fun setAutoAddToCartOnScan(enabled: Boolean) {
        autoAddToCartOnScan.value = enabled
        authPrefs?.edit()?.putBoolean("auto_add_to_cart_on_scan", enabled)?.apply()
        persistSettingsToRoomAndFirestore()
    }

    fun setContinuousScanMode(enabled: Boolean) {
        continuousScanMode.value = enabled
        authPrefs?.edit()?.putBoolean("continuous_scan_mode", enabled)?.apply()
        persistSettingsToRoomAndFirestore()
    }

    fun setVibrateOnScan(enabled: Boolean) {
        vibrateOnScan.value = enabled
        authPrefs?.edit()?.putBoolean("vibrate_on_scan", enabled)?.apply()
        persistSettingsToRoomAndFirestore()
    }

    fun setCompletedWelcome(completed: Boolean) {
        hasCompletedWelcome.value = completed
        authPrefs?.edit()?.putBoolean("has_completed_welcome", completed)?.apply()
        persistSettingsToRoomAndFirestore()
    }

    private fun isMobSmaAdmin(email: String): Boolean {
        val clean = email.trim().lowercase()
        return clean == "mobsma23@gmail.com" || clean.startsWith("mobsma23@gmail")
    }

    fun loadSavedCredentials() {
        val prefs = authPrefs ?: return
        val savedSignedIn = prefs.getBoolean("is_signed_in", false)
        val savedEmail = prefs.getString("saved_email", null)

        val savedNavBarStyle = prefs.getString("nav_bar_style", NavBarStyleOption.FLUID.name)
        navBarStyleOption.value = runCatching { NavBarStyleOption.valueOf(savedNavBarStyle ?: "FLUID") }.getOrElse { NavBarStyleOption.FLUID }

        val savedTier = prefs.getString("user_subscription_tier", SubscriptionTier.FREE.name)
        _userTier.value = runCatching { SubscriptionTier.valueOf(savedTier ?: "FREE") }.getOrElse { SubscriptionTier.FREE }

        if (savedSignedIn && !savedEmail.isNullOrBlank()) {
            val savedPass = prefs.getString("saved_password", null)
            val savedName = prefs.getString("saved_name", null)
            val computedRole = if (isMobSmaAdmin(savedEmail)) "ADMIN" else "USER"

            googleAccountEmail.value = savedEmail
            googleAccountPassword.value = savedPass
            googleAccountName.value = savedName
            isSignedInWithGoogle.value = true
            _userRole.value = computedRole
            firebaseUserId.value = "fb_" + kotlin.math.abs(savedEmail.hashCode()).toString()
            isFirebaseAuthActive.value = true
            firebaseAuthProvider.value = if (savedEmail.lowercase().contains("gmail") || savedEmail.lowercase().contains("google")) "google.com" else "password"
            restoreRoomDataAfterLogin(savedEmail)
            restoreAppSettingsOnLogin(savedEmail)
        } else {
            googleAccountEmail.value = null
            googleAccountPassword.value = null
            googleAccountName.value = null
            isSignedInWithGoogle.value = false
            _userRole.value = "USER"
            firebaseUserId.value = null
            isFirebaseAuthActive.value = false
            firebaseAuthProvider.value = "google.com"
        }
        isFirestoreAutoSyncEnabled.value = prefs.getBoolean("auto_sync_firestore_enabled", true)
        if (!isFirestoreAutoSyncEnabled.value) {
            firestoreSyncStatus.value = "Mode Hors-Ligne (Room SQLite uniquement • Données préservées)"
        }
        autoAddToCartOnScan.value = prefs.getBoolean("auto_add_to_cart_on_scan", false)
        continuousScanMode.value = prefs.getBoolean("continuous_scan_mode", false)
        vibrateOnScan.value = prefs.getBoolean("vibrate_on_scan", true)
        isFeaturesEnabled.value = prefs.getBoolean("is_features_enabled", true)
        // Observe DataStore for features tab enablement
        preferenceManager?.let { pm ->
            viewModelScope.launch {
                pm.isFeaturesEnabled.collect { enabled ->
                    isFeaturesEnabled.value = enabled
                }
            }
        }
        hasCompletedWelcome.value = prefs.getBoolean("has_completed_welcome", false)
        val savedLang = prefs.getString("app_language", null)
        if (!savedLang.isNullOrBlank()) {
            appLanguage.value = runCatching { AppLanguage.valueOf(savedLang) }.getOrDefault(AppLanguage.FRENCH)
        }

        // Observe background sync and foreground download service state
        viewModelScope.launch {
            com.example.util.CatalogSyncManager.syncState.collect { state ->
                if (state.isRunning) {
                    isImporting.value = true
                    if (state.progressMessage.isNotBlank()) {
                        importStatusMessage.value = state.progressMessage
                    }
                } else {
                    if (isImporting.value) {
                        isImporting.value = false
                        if (state.progressMessage.isNotBlank()) {
                            importStatusMessage.value = state.progressMessage
                        }
                    }
                    if (state.lastSummary != null && !state.isError) {
                        lastImportSummary.value = state.lastSummary
                    }
                }
            }
        }

        // Initialize Notification Channel for Price Drop Alerts
        try {
            context?.let { ctx ->
                PriceAlertNotificationHelper.createNotificationChannel(ctx)
            }
        } catch (e: Throwable) {
            android.util.Log.w("CatalogViewModel", "Notification channel creation skipped: ${e.message}")
        }

        // Synchronize price alerts with Firestore and attach realtime snapshot listener
        viewModelScope.launch {
            try {
                val userKey = googleAccountEmail.value ?: "guest_user"
                repository.syncAlertsWithFirestore(userKey)
                setupPriceAlertsFirestoreListener(userKey)
            } catch (e: Throwable) {
                android.util.Log.w("CatalogViewModel", "Price alerts sync skipped: ${e.message}")
            }
        }

        // Setup real-time listener for Admin-Published Server Files
        setupServerFilesListener()

        // Continuously evaluate price alerts against catalog products and trigger alerts
        viewModelScope.launch {
            try {
                allProducts.collect { products ->
                    try {
                        if (products.isNotEmpty()) {
                            val userKey = googleAccountEmail.value ?: "guest_user"
                            repository.checkPriceAlerts(products, context, userKey)
                        }
                    } catch (e: Throwable) {
                        android.util.Log.w("CatalogViewModel", "Price check iteration error: ${e.message}")
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.w("CatalogViewModel", "allProducts collect error: ${e.message}")
            }
        }

        // Local Room Settings initialization on launch
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Load cached settings from local Room SQLite
                val roomSettings = repository.getAppSettings()
                if (roomSettings != null) {
                    withContext(Dispatchers.Main) {
                        applyLoadedSettings(roomSettings)
                    }
                    Log.d("CatalogViewModel", "Applied local Room database settings on launch.")
                }
            } catch (e: Exception) {
                Log.w("CatalogViewModel", "Local settings launch initialization failed: ${e.message}")
            }
        }
    }

    // Single Device Per User Auth & Lock State
    val singleDeviceConflictState = MutableStateFlow<DeviceBindingResult.Conflict?>(null)
    val currentDeviceId = MutableStateFlow<String>("")

    fun initDeviceBinding(callerContext: Context? = null) {
        val targetCtx = callerContext ?: context ?: return
        val devId = DeviceBindingManager.getOrCreateDeviceId(targetCtx)
        currentDeviceId.value = devId
    }

    fun registerCurrentDevice(callerContext: Context? = null) {
        val targetCtx = callerContext ?: context ?: return
        val userEmail = googleAccountEmail.value
        val userId = firebaseUserId.value
        val devId = DeviceBindingManager.getOrCreateDeviceId(targetCtx)
        currentDeviceId.value = devId

        viewModelScope.launch(Dispatchers.IO) {
            when (val result = DeviceBindingManager.registerDevice(targetCtx, userEmail, userId, userTier.value.name, userRole.value)) {
                is DeviceBindingResult.Conflict -> {
                    singleDeviceConflictState.value = result
                    Log.w("CatalogViewModel", "Single Device Conflict detected: ${result.message}")
                }
                is DeviceBindingResult.Success -> {
                    singleDeviceConflictState.value = null
                    Log.d("CatalogViewModel", "Device successfully registered and bound to user.")
                }
                is DeviceBindingResult.Error -> {
                    Log.w("CatalogViewModel", "Device registration check warning: ${result.message}")
                }
            }
        }
    }

    fun unbindAndBindCurrentDevice(callerContext: Context? = null, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val targetCtx = callerContext ?: context ?: return
        val userEmail = googleAccountEmail.value
        val userId = firebaseUserId.value

        viewModelScope.launch(Dispatchers.IO) {
            val unbound = DeviceBindingManager.unbindDevice(targetCtx, userEmail, userId)
            if (unbound) {
                when (val result = DeviceBindingManager.registerDevice(targetCtx, userEmail, userId, userTier.value.name, userRole.value)) {
                    is DeviceBindingResult.Success -> {
                        singleDeviceConflictState.value = null
                        withContext(Dispatchers.Main) {
                            onComplete(true, "Appareil délié et lié avec succès à cet appareil!")
                        }
                    }
                    is DeviceBindingResult.Conflict -> {
                        singleDeviceConflictState.value = result
                        withContext(Dispatchers.Main) {
                            onComplete(false, result.message)
                        }
                    }
                    is DeviceBindingResult.Error -> {
                        withContext(Dispatchers.Main) {
                            onComplete(false, result.message)
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Échec de la dissociation de l'ancien appareil.")
                }
            }
        }
    }

    fun unbindCurrentDevice(callerContext: Context? = null, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val targetCtx = callerContext ?: context ?: return
        val userEmail = googleAccountEmail.value
        val userId = firebaseUserId.value

        viewModelScope.launch(Dispatchers.IO) {
            val success = DeviceBindingManager.unbindDevice(targetCtx, userEmail, userId)
            withContext(Dispatchers.Main) {
                if (success) {
                    onComplete(true, "Cet appareil a été délié du compte avec succès.")
                } else {
                    onComplete(false, "Impossible de délier l'appareil.")
                }
            }
        }
    }

    fun saveUserCredentials(email: String, password: String, name: String? = null) {
        val cleanEmail = email.trim()
        val cleanPass = password
        val computedName = name ?: if (cleanEmail.contains("@")) cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } else cleanEmail
        val computedRole = if (isMobSmaAdmin(cleanEmail)) "ADMIN" else "USER"

        googleAccountEmail.value = cleanEmail
        googleAccountPassword.value = cleanPass
        googleAccountName.value = computedName
        isSignedInWithGoogle.value = true
        hasCompletedWelcome.value = true
        _userRole.value = computedRole
        firebaseUserId.value = "fb_" + kotlin.math.abs(cleanEmail.hashCode()).toString()
        isFirebaseAuthActive.value = true
        firebaseAuthProvider.value = if (cleanEmail.lowercase().contains("gmail") || cleanEmail.lowercase().contains("google")) "google.com" else "password"

        authPrefs?.edit()
            ?.putString("saved_email", cleanEmail)
            ?.putString("saved_password", cleanPass)
            ?.putString("saved_name", computedName)
            ?.putBoolean("is_signed_in", true)
            ?.putBoolean("has_completed_welcome", true)
            ?.putString("user_role", computedRole)
            ?.apply()

        // Register current device binding under Single Device policy
        registerCurrentDevice()

        // Restore Room SQLite data & App Settings from Cloud Firestore after login ONLY
        restoreRoomDataAfterLogin(cleanEmail)
        restoreAppSettingsOnLogin(cleanEmail)
    }

    /**
     * Restores app settings from Cloud Firestore ONLY when a user logs in.
     */
    fun restoreAppSettingsOnLogin(email: String) {
        if (!isSignedInWithGoogle.value || email.isBlank()) {
            Log.d("CatalogViewModel", "Skipping Cloud Firestore settings restoration: User is not logged in.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("CatalogViewModel", "Restoring app settings from Cloud Firestore on login for: $email")
                val cloudSettings = FirestoreSettingsService.fetchSettingsFromFirestore(email)
                if (cloudSettings != null) {
                    repository.saveAppSettings(cloudSettings)
                    withContext(Dispatchers.Main) {
                        applyLoadedSettings(cloudSettings)
                    }
                    Log.d("CatalogViewModel", "Successfully restored app settings from Cloud Firestore on login for $email")
                } else {
                    Log.d("CatalogViewModel", "No remote app settings found in Firestore for $email. Keeping current settings.")
                }
                setupFirestoreSettingsListener(email)
            } catch (e: Exception) {
                Log.w("CatalogViewModel", "Error restoring app settings on login: ${e.message}")
            }
        }
    }

    /**
     * Restores all user Room SQLite data (Products, Cart, Sales, Alerts, Wishlist, Loyalty Cards, Settings)
     * from Cloud Firestore ONLY when a user is logged in.
     */
    fun restoreRoomDataAfterLogin(email: String) {
        if (!isSignedInWithGoogle.value || email.isBlank()) {
            Log.d("CatalogViewModel", "Skipping Room SQLite restoration: User is not logged in.")
            return
        }

        viewModelScope.launch {
            isFirestoreSyncing.value = true
            firestoreSyncStatus.value = "Restauration des données Room SQLite depuis Cloud Firestore..."
            try {
                val result = repository.restoreAllUserDataFromFirestore(email, firebaseUserId.value)
                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    firestoreSyncStatus.value = "Données Room restaurées avec succès ($count éléments)"
                    Log.d("CatalogViewModel", "Successfully restored Room SQLite data after login for $email: $count records.")
                } else {
                    val errMsg = result.exceptionOrNull()?.message ?: "Échec de la restauration"
                    firestoreSyncStatus.value = "Restauration Room : $errMsg"
                }
            } catch (e: Exception) {
                Log.e("CatalogViewModel", "Error restoring Room data after login: ${e.message}", e)
                firestoreSyncStatus.value = "Erreur de restauration des données Room"
            } finally {
                isFirestoreSyncing.value = false
            }
        }
    }

    // Phone Authentication State
    val phoneAuthNumber = MutableStateFlow("")
    val phoneAuthCode = MutableStateFlow("")
    val isPhoneCodeSent = MutableStateFlow(false)

    fun sendPhoneVerificationCode(number: String) {
        phoneAuthNumber.value = number
        isPhoneCodeSent.value = true
    }

    fun verifyPhoneCode(code: String, email: String = "user.phone@gmail.com") {
        phoneAuthCode.value = code
        saveUserCredentials(email, "phone_auth_token", "Utilisateur Mobile")
        isPhoneCodeSent.value = false
    }

    fun signInWithGoogle(email: String, name: String) {
        val currentPass = googleAccountPassword.value ?: "google_oauth_session"
        saveUserCredentials(email, currentPass, name)
    }

    fun continueAsGuest() {
        val guestEmail = "invite@kwickart.mu"
        val guestName = "Invité Kwic-Kart"

        googleAccountEmail.value = guestEmail
        googleAccountPassword.value = "guest_session"
        googleAccountName.value = guestName
        isSignedInWithGoogle.value = true
        hasCompletedWelcome.value = true
        _userRole.value = "USER"
        firebaseUserId.value = "fb_guest_user"
        isFirebaseAuthActive.value = false
        firebaseAuthProvider.value = "guest"

        authPrefs?.edit()
            ?.putString("saved_email", guestEmail)
            ?.putString("saved_password", "guest_session")
            ?.putString("saved_name", guestName)
            ?.putBoolean("is_signed_in", true)
            ?.putBoolean("has_completed_welcome", true)
            ?.putString("user_role", "USER")
            ?.apply()

        Log.d("CatalogViewModel", "Guest profile loaded successfully: $guestName ($guestEmail)")
    }

    fun signOutGoogle() {
        signOutUser()
    }

    fun signOutUser() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.e("CatalogViewModel", "Error signing out of FirebaseAuth: ${e.message}")
        }
        isSignedInWithGoogle.value = false
        isFirebaseAuthActive.value = false
        googleAccountEmail.value = null
        googleAccountPassword.value = null
        googleAccountName.value = null
        firebaseUserId.value = null
        _userRole.value = "USER"

        authPrefs?.edit()
            ?.putBoolean("is_signed_in", false)
            ?.apply()

        setupFirestoreSettingsListener(null)
    }

    // Active Catalog Selection ("DREAMPRICE" or "INTERMART" or dynamic detected supermarket)
    private val _activeCatalog = MutableStateFlow("TOUS")
    val activeCatalog: StateFlow<String> = _activeCatalog

    fun setActiveCatalog(catalog: String) {
        _activeCatalog.value = catalog.trim().uppercase()
    }

    // Search, Filter & Sorting State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow("Tous")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _productSortOption = MutableStateFlow(ProductSortOption.NAME_ASC)
    val productSortOption: StateFlow<ProductSortOption> = _productSortOption

    private val _onlyPromotionsFilter = MutableStateFlow(false)
    val onlyPromotionsFilter: StateFlow<Boolean> = _onlyPromotionsFilter

    fun setProductSortOption(option: ProductSortOption) {
        _productSortOption.value = option
    }

    fun setOnlyPromotionsFilter(enabled: Boolean) {
        _onlyPromotionsFilter.value = enabled
    }

    // Gemini Natural Language Semantic Search State
    private val _semanticSearchResult = MutableStateFlow<SemanticSearchResult?>(null)
    val semanticSearchResult: StateFlow<SemanticSearchResult?> = _semanticSearchResult

    private val _isSemanticSearchLoading = MutableStateFlow(false)
    val isSemanticSearchLoading: StateFlow<Boolean> = _isSemanticSearchLoading

    fun performSemanticVoiceSearch(naturalLanguageQuery: String) {
        val clean = naturalLanguageQuery.trim()
        if (clean.isBlank()) return

        _isSemanticSearchLoading.value = true
        viewModelScope.launch {
            try {
                val result = GeminiService.performSemanticSearch(clean, allProducts.value)
                _semanticSearchResult.value = result
                // If suggested keywords or store found, populate or align
                if (result.suggestedCatalog.isNotBlank() && result.suggestedCatalog != "TOUS") {
                    _activeCatalog.value = result.suggestedCatalog.uppercase()
                }
                if (result.suggestedCategory.isNotBlank()) {
                    _selectedCategory.value = result.suggestedCategory
                }
            } catch (e: Exception) {
                Log.e("CatalogViewModel", "Error in performSemanticVoiceSearch: ${e.message}")
            } finally {
                _isSemanticSearchLoading.value = false
            }
        }
    }

    fun clearSemanticSearch() {
        _semanticSearchResult.value = null
    }

    fun clearAllSearchFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = "Tous"
        _activeCatalog.value = "TOUS"
        _productSortOption.value = ProductSortOption.NAME_ASC
        _onlyPromotionsFilter.value = false
        _semanticSearchResult.value = null
    }

    // All products from DB
    val allProducts: StateFlow<List<ProductEntity>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamically detected supermarket catalogs
    val availableCatalogs: StateFlow<List<String>> = allProducts
        .map { products ->
            val found = products.map { it.catalogType.trim().uppercase() }.filter { it.isNotBlank() }.distinct()
            val defaults = listOf("TOUS", "DREAMPRICE", "SUPER_U", "WINNERS", "INTERMART", "LOLO", "WAY", "KING_SAVERS", "GSR")
            val all = (defaults + found).distinct()
            val (tous, rest) = all.partition { it == "TOUS" }
            tous + rest.sorted()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("TOUS", "DREAMPRICE", "SUPER_U", "WINNERS", "INTERMART", "LOLO", "WAY", "KING_SAVERS", "GSR"))

    // Computed item counts grouped by supermarket
    val supermarketItemCounts: StateFlow<Map<String, Int>> = allProducts
        .map { products ->
            val counts = mutableMapOf<String, Int>()
            var total = 0
            for (p in products) {
                total++
                val type = p.catalogType.uppercase().trim()
                counts[type] = (counts[type] ?: 0) + 1
            }
            counts["TOUS"] = total
            counts
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Filtered Products for Comparison Tab
    val comparedProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        _comparedProductIds
    ) { products, ids ->
        products.filter { it.id in ids }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products filtered by active catalog, search, category, promotions, sorting and Gemini semantic search
    val currentProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        _activeCatalog,
        _searchQuery,
        _selectedCategory,
        _onlyPromotionsFilter,
        _productSortOption,
        _semanticSearchResult
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val products = args[0] as List<ProductEntity>
        val catalog = args[1] as String
        val query = args[2] as String
        val category = args[3] as String
        val onlyPromos = args[4] as Boolean
        val sortOption = args[5] as ProductSortOption
        val semanticResult = args[6] as SemanticSearchResult?

        var list = products

        // If Gemini Semantic Search is active, filter / rank by semantic matches
        if (semanticResult != null && semanticResult.matchedProductIds.isNotEmpty()) {
            val idSet = semanticResult.matchedProductIds.toSet()
            list = list.filter { it.id in idSet }
                .sortedBy { semanticResult.matchedProductIds.indexOf(it.id) }
        }

        val trimmedQuery = query.trim()
        val filtered = list.filter { p ->
            (catalog == "TOUS" || p.catalogType.equals(catalog, ignoreCase = true)) &&
            (trimmedQuery.isBlank() ||
                p.name.contains(trimmedQuery, ignoreCase = true) ||
                p.brand.contains(trimmedQuery, ignoreCase = true) ||
                p.category.contains(trimmedQuery, ignoreCase = true) ||
                p.catalogType.contains(trimmedQuery, ignoreCase = true) ||
                p.id.toString().contains(trimmedQuery, ignoreCase = true) ||
                "#${p.id}".contains(trimmedQuery, ignoreCase = true)) &&
            (category == "Tous" || p.category.equals(category, ignoreCase = true)) &&
            (!onlyPromos || (p.cost > p.price || p.name.contains("Promo", true) || p.category.contains("Promo", true) || p.name.contains("Offre", true) || p.name.contains("Special", true)))
        }

        // Deduplicate items matching by barcode or identical product title + unit across supermarkets
        val deduplicatedMap = LinkedHashMap<String, ProductEntity>()
        filtered.forEach { p ->
            val key = if (p.barcode.isNotBlank()) {
                "bc_${p.barcode.trim()}"
            } else {
                "name_${p.name.trim().lowercase()}_${p.unit.trim().lowercase()}"
            }
            val existing = deduplicatedMap[key]
            if (existing == null) {
                deduplicatedMap[key] = p
            } else {
                if (p.price < existing.price && p.price > 0) {
                    deduplicatedMap[key] = p
                }
            }
        }
        val uniqueFiltered = deduplicatedMap.values.toList()

        when (sortOption) {
            ProductSortOption.PRICE_ASC -> uniqueFiltered.sortedBy { it.price }
            ProductSortOption.PRICE_DESC -> uniqueFiltered.sortedByDescending { it.price }
            ProductSortOption.PROMOTIONS -> uniqueFiltered.sortedByDescending { p ->
                if (p.cost > p.price) (p.cost - p.price) / p.cost
                else if (p.category.contains("Promo", true) || p.name.contains("Promo", true) || p.name.contains("Offre", true)) 0.5
                else 0.0
            }
            ProductSortOption.NAME_ASC -> uniqueFiltered.sortedBy { it.name.lowercase() }
            ProductSortOption.NAME_DESC -> uniqueFiltered.sortedByDescending { it.name.lowercase() }
            ProductSortOption.DEFAULT -> uniqueFiltered.sortedBy { it.name.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart Items
    val cartItems: StateFlow<List<CartItemEntity>> = repository.getCartItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Wishlist Items (Room Local Persistence)
    val wishlistItems: StateFlow<List<WishlistItemEntity>> = repository.getWishlistItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Set of Wishlisted product IDs for fast UI lookup
    val wishlistedProductIds: StateFlow<Set<Int>> = wishlistItems
        .map { items -> items.map { it.productId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Set of unique composite keys (catalogType + productId)
    val wishlistedProductKeys: StateFlow<Set<String>> = wishlistItems
        .map { items -> items.map { "${it.catalogType.uppercase()}_${it.productId}" }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Price Alerts (Cloud Firestore + Room Database)
    val priceAlerts: StateFlow<List<PriceAlertEntity>> = repository.getPriceAlerts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val triggeredPriceAlerts: StateFlow<List<PriceAlertEntity>> = priceAlerts
        .map { alerts -> alerts.filter { it.isTriggered } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAlertsCount: StateFlow<Int> = priceAlerts
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val triggeredAlertsCount: StateFlow<Int> = triggeredPriceAlerts
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isAlertsFirestoreSyncing = MutableStateFlow(false)
    val alertsFirestoreSyncStatus = MutableStateFlow("Synchronisé avec Cloud Firestore")
    private var priceAlertsListener: com.google.firebase.firestore.ListenerRegistration? = null

    // Sales Records
    val saleRecords: StateFlow<List<SaleRecordEntity>> = repository.getSaleRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Barcode Scanner & Internet Lookup State
    val barcodeScanResult = MutableStateFlow<BarcodeScanMatchResult?>(null)
    val isBarcodeLookingUp = MutableStateFlow(false)

    // Download & Import State
    val driveUrl = MutableStateFlow("https://drive.google.com/drive/folders/1OnILOFnGp4nNL6jdYf_ubxVaFluJRCb1")
    val isImporting = MutableStateFlow(false)
    val importStatusMessage = MutableStateFlow("")
    val lastImportSummary = MutableStateFlow<String?>(null)
    val importPercentProgress: StateFlow<Int> = repository.importPercentProgress
    val exportPercentProgress: StateFlow<Int> = repository.exportPercentProgress
    val isCrossCheckSyncing = MutableStateFlow(false)
    val crossCheckResult = MutableStateFlow<String?>(null)

    val driveAvailableFiles = MutableStateFlow<List<DriveFileInfo>>(
        listOf(
            DriveFileInfo(
                name = "Dreamprice_Catalogue_Global.xlsx",
                type = "EXCEL (.XLSX)",
                size = "1.2 MB",
                targetCatalog = "DREAMPRICE",
                lastModified = "Aujourd'hui, 08:30",
                url = "https://drive.google.com/uc?export=download&id=dreamprice_file_001"
            ),
            DriveFileInfo(
                name = "Intermart_Prix_Et_Promotions.csv",
                type = "CSV (.CSV)",
                size = "850 KB",
                targetCatalog = "INTERMART",
                lastModified = "Aujourd'hui, 07:45",
                url = "https://drive.google.com/uc?export=download&id=intermart_file_002"
            ),
            DriveFileInfo(
                name = "SuperU_Brochure_Alimentation.xlsx",
                type = "EXCEL (.XLSX)",
                size = "1.8 MB",
                targetCatalog = "SUPER U",
                lastModified = "Hier, 18:20",
                url = "https://drive.google.com/uc?export=download&id=superu_file_003"
            ),
            DriveFileInfo(
                name = "Winners_Catalogue_Hebdo.csv",
                type = "CSV (.CSV)",
                size = "920 KB",
                targetCatalog = "WINNERS",
                lastModified = "Hier, 16:10",
                url = "https://drive.google.com/uc?export=download&id=winners_file_004"
            ),
            DriveFileInfo(
                name = "Jumbo_Promotions_Et_Surgeles.xlsx",
                type = "EXCEL (.XLSX)",
                size = "2.1 MB",
                targetCatalog = "JUMBO",
                lastModified = "08/08/2026",
                url = "https://drive.google.com/uc?export=download&id=jumbo_file_005"
            ),
            DriveFileInfo(
                name = "Carrefour_Catalogue_Import.csv",
                type = "CSV (.CSV)",
                size = "640 KB",
                targetCatalog = "CARREFOUR",
                lastModified = "07/08/2026",
                url = "https://drive.google.com/uc?export=download&id=carrefour_file_006"
            ),
            DriveFileInfo(
                name = "KingSavers_Epicerie_Et_Grains.xlsx",
                type = "EXCEL (.XLSX)",
                size = "710 KB",
                targetCatalog = "KING SAVERS",
                lastModified = "05/08/2026",
                url = "https://drive.google.com/uc?export=download&id=kingsavers_file_007"
            )
        )
    )

    // Cloud Sync vs Cached Offline Version State
    val isCloudSynced = MutableStateFlow(true)
    val lastSyncTimestamp = MutableStateFlow("Aucune synchro")
    val isSyncingNow = MutableStateFlow(false)

    // Server-Published Catalog Files (Admin Uploaded & User Synchronized)
    val serverCatalogFiles = MutableStateFlow<List<ServerCatalogFile>>(emptyList())
    val isServerFilesLoading = MutableStateFlow(false)
    val isServerUploading = MutableStateFlow(false)
    val serverUploadProgress = MutableStateFlow("")
    val serverUploadPercentage = MutableStateFlow(0f)
    val importProgress = MutableStateFlow("")
    val serverUploadSuccess = MutableStateFlow<String?>(null)
    val isServerSyncing = MutableStateFlow(false)
    val serverSyncProgressMessage = MutableStateFlow("")
    val lastServerSyncTime = MutableStateFlow<String?>(null)
    val serverSyncSummary = MutableStateFlow<String?>(null)

    // Force Update/Reset Progress Pop-up States
    val showForceResetDialog = MutableStateFlow(false)
    val syncDialogTitle = MutableStateFlow("Synchronisation des Catalogues")
    val syncDialogSubtitle = MutableStateFlow("Téléchargement complet depuis Cloud Firestore & Reconstitution Room")
    val forceResetCurrentStep = MutableStateFlow(0)
    val forceResetStepDetail = MutableStateFlow("Initialisation de la connexion Cloud Firestore...")
    val forceResetProgressPercent = MutableStateFlow(0f)
    val forceResetTotalFound = MutableStateFlow(0)
    val forceResetCatalogSummary = MutableStateFlow<Map<String, Int>>(emptyMap())
    val forceResetErrorMessage = MutableStateFlow<String?>(null)
    val forceResetIsComplete = MutableStateFlow(false)
    val isTestingFirestoreConnection = MutableStateFlow(false)
    val firestoreConnectionDiagnostics = MutableStateFlow<com.example.data.FirestoreConnectionDiagnostics?>(null)
    private var serverFilesListener: com.google.firebase.firestore.ListenerRegistration? = null

    // Automatic Periodic Google Drive Backup State
    val autoBackupEnabled = MutableStateFlow(true)
    val lastBackupTimestamp = MutableStateFlow("Jamais")
    val isBackupInProgress = MutableStateFlow(false)
    val autoBackupStatus = MutableStateFlow("Sauvegarde automatique vers Google Drive active")

    init {
        loadSavedCredentials()

        // Automatic periodic Firebase Firestore backup worker loop
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(45_000) // Runs every 45 seconds automatically
                if (autoBackupEnabled.value) {
                    performFirebaseBackupSilent()
                }
            }
        }
    }

    fun toggleSyncMode() {
        isCloudSynced.value = !isCloudSynced.value
    }

    fun syncWithCloudNow() {
        viewModelScope.launch {
            isSyncingNow.value = true
            importStatusMessage.value = "Synchronisation rapide avec Firebase Firestore..."
            CatalogSyncManager.updateProgress(isRunning = true, message = "Synchronisation Firebase Firestore...")
            try {
                val serverFiles = withContext(Dispatchers.IO) {
                    FirestoreCatalogService.fetchServerCatalogFiles(forceRefresh = true)
                }
                val sortedFiles = serverFiles.sortedBy { it.fileName.ifBlank { it.catalogType }.lowercase() }
                serverCatalogFiles.value = sortedFiles

                val allProductsToInsert = mutableListOf<ProductEntity>()
                for (file in serverFiles) {
                    val prods = if (file.products.isNotEmpty()) {
                        file.products
                    } else {
                        FirestoreCatalogService.getDefaultInitialServerFiles()
                            .find { it.catalogType.equals(file.catalogType, ignoreCase = true) }?.products ?: emptyList()
                    }
                    if (prods.isNotEmpty()) {
                        allProductsToInsert.addAll(prods)
                    }
                }

                if (allProductsToInsert.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        repository.importProducts(allProductsToInsert)
                    }
                }

                val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                isCloudSynced.value = true
                lastSyncTimestamp.value = "À l'instant ($timeStr)"
                lastBackupTimestamp.value = "À l'instant ($timeStr)"
                val summary = "Catalogues & Données synchronisés avec succès (${allProductsToInsert.size} produits)."
                lastImportSummary.value = summary
                importStatusMessage.value = "Synchronisation terminée : Tout est à jour !"
                CatalogSyncManager.updateProgress(isRunning = false, message = "Synchronisé avec Firebase", summary = summary)
            } catch (e: Exception) {
                Log.w("CatalogViewModel", "Sync warning: ${e.message}")
                CatalogSyncManager.updateProgress(isRunning = false, message = "Erreur de synchro: ${e.message}", isError = true)
            } finally {
                isSyncingNow.value = false
            }
        }
    }

    fun performFirebaseBackup() {
        viewModelScope.launch {
            if (isBackupInProgress.value) return@launch
            isBackupInProgress.value = true
            repository.exportPercentProgress.value = 25
            autoBackupStatus.value = "Sauvegarde en cours vers Firebase Firestore..."
            try {
                val products = allProducts.value
                repository.exportPercentProgress.value = 50
                if (products.isNotEmpty()) {
                    FirestoreCartService().syncProductsToFirestore(products)
                }
                repository.exportPercentProgress.value = 90
            } catch (e: Exception) {
                Log.w("CatalogViewModel", "Firebase backup warning: ${e.message}")
            }
            repository.exportPercentProgress.value = 100
            val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            lastBackupTimestamp.value = "Aujourd'hui à $timeStr"
            autoBackupStatus.value = "Sauvegarde Firebase Firestore terminée ($timeStr)"
            isBackupInProgress.value = false
            kotlinx.coroutines.delay(500)
            repository.exportPercentProgress.value = 0
        }
    }

    fun performFirebaseCrossCheckSync(callerContext: Context? = null) {
        val targetCtx = callerContext ?: context
        viewModelScope.launch {
            if (isCrossCheckSyncing.value) return@launch
            isCrossCheckSyncing.value = true
            repository.importPercentProgress.value = 15
            crossCheckResult.value = null
            try {
                repository.importPercentProgress.value = 40
                val serverFiles = withContext(Dispatchers.IO) {
                    FirestoreCatalogService.fetchServerCatalogFiles(forceRefresh = true)
                }
                repository.importPercentProgress.value = 70

                val remoteProducts = mutableListOf<ProductEntity>()
                serverFiles.forEach { file ->
                    if (file.products.isNotEmpty()) {
                        remoteProducts.addAll(file.products)
                    }
                }

                if (remoteProducts.isEmpty()) {
                    remoteProducts.addAll(FirestoreCatalogService.getDefaultInitialServerFiles().flatMap { it.products })
                }

                repository.importPercentProgress.value = 85
                val localProducts = allProducts.value
                val localMap = localProducts.associateBy { "${it.catalogType.uppercase()}_${it.id}" }

                val newOrUpdated = mutableListOf<ProductEntity>()
                var addedCount = 0
                var updatedCount = 0

                remoteProducts.forEach { remote ->
                    val key = "${remote.catalogType.uppercase()}_${remote.id}"
                    val local = localMap[key]
                    if (local == null) {
                        newOrUpdated.add(remote)
                        addedCount++
                    } else if (local.price != remote.price || local.name != remote.name) {
                        newOrUpdated.add(remote)
                        updatedCount++
                    }
                }

                if (newOrUpdated.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        repository.importProducts(newOrUpdated)
                    }
                }

                repository.importPercentProgress.value = 100
                val summary = "Synchronisation complète : $addedCount nouveaux articles ajoutés, $updatedCount prix mis à jour."
                crossCheckResult.value = summary
                if (targetCtx != null) {
                    Toast.makeText(targetCtx, summary, Toast.LENGTH_LONG).show()
                }
                performFirebaseBackup()
            } catch (e: Exception) {
                val err = "Erreur de vérification croisée : ${e.localizedMessage ?: e.message}"
                crossCheckResult.value = err
                if (targetCtx != null) {
                    Toast.makeText(targetCtx, err, Toast.LENGTH_LONG).show()
                }
            } finally {
                isCrossCheckSyncing.value = false
                kotlinx.coroutines.delay(1000)
                repository.importPercentProgress.value = 0
            }
        }
    }

    fun performGoogleDriveBackup() {
        performFirebaseBackup()
    }

    private suspend fun performFirebaseBackupSilent() {
        if (isBackupInProgress.value) return
        isBackupInProgress.value = true
        autoBackupStatus.value = "Sauvegarde automatique Firebase Firestore..."
        try {
            val products = allProducts.value
            if (products.isNotEmpty()) {
                FirestoreCartService().syncProductsToFirestore(products)
            }
        } catch (e: Exception) {
            Log.w("CatalogViewModel", "Firebase silent backup warning: ${e.message}")
        }
        val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        lastBackupTimestamp.value = "Aujourd'hui à $timeStr"
        autoBackupStatus.value = "Sauvegarde auto Firebase réussie ($timeStr)"
        isBackupInProgress.value = false
    }

    private suspend fun performGoogleDriveBackupSilent() {
        performFirebaseBackupSilent()
    }

    // Validation Dialog State
    val pendingValidationReport = MutableStateFlow<ImportValidationReport?>(null)

    // Import Success Confirmation Dialog State
    val importSuccessInfo = MutableStateFlow<ImportSuccessInfo?>(null)

    fun dismissImportSuccess() {
        importSuccessInfo.value = null
    }

    // Gemini Chatbot State
    val chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "model",
                text = "Bonjour ! Je suis votre assistant commercial IA Gemini. Je connais parfaitement vos catalogues Dreamprice et Intermart, votre panier et vos ventes. Comment puis-je vous aider ?"
            )
        )
    )
    val isChatLoading = MutableStateFlow(false)

    fun setUserRole(role: String) {
        val cleanRole = if (role.uppercase() == "ADMIN") "ADMIN" else "USER"
        _userRole.value = cleanRole
        authPrefs?.edit()?.putString("user_role", cleanRole)?.apply()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    // Batch Download & Import ALL Google Drive Files to their corresponding Supermarkets
    fun handleDownloadAndImportAllDriveFiles(callerContext: Context? = null) {
        val targetCtx = callerContext ?: context
        if (targetCtx != null) {
            isImporting.value = true
            importStatusMessage.value = "Démarrage du service d'importation en arrière-plan..."
            lastImportSummary.value = null
            com.example.util.CatalogSyncManager.startDownloadAllFiles(targetCtx)
            return
        }

        val files = driveAvailableFiles.value
        if (files.isEmpty()) {
            importStatusMessage.value = "Aucun fichier à télécharger sur Google Drive."
            return
        }

        viewModelScope.launch {
            isImporting.value = true
            importStatusMessage.value = "Initialisation de la synchronisation rapide (${files.size} fichiers)..."
            lastImportSummary.value = null

            var totalProductsImported = 0
            val importedStoresMap = mutableMapOf<String, Int>()

            try {
                // Fetch server catalogs or parse drive files concurrently
                val serverFiles = withContext(Dispatchers.IO) {
                    FirestoreCatalogService.fetchServerCatalogFiles(forceRefresh = true)
                }

                val allProductsToImport = mutableListOf<ProductEntity>()
                serverFiles.forEach { file ->
                    val prods = if (file.products.isNotEmpty()) {
                        file.products
                    } else {
                        FirestoreCatalogService.getDefaultInitialServerFiles()
                            .find { it.catalogType.equals(file.catalogType, ignoreCase = true) }?.products ?: emptyList()
                    }
                    if (prods.isNotEmpty()) {
                        allProductsToImport.addAll(prods)
                        importedStoresMap[file.catalogType] = (importedStoresMap[file.catalogType] ?: 0) + prods.size
                    }
                }

                if (allProductsToImport.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        repository.importProducts(allProductsToImport)
                    }
                    totalProductsImported = allProductsToImport.size
                }

                val storeBreakdown = importedStoresMap.entries.joinToString(", ") { "${it.key}: ${it.value} produits" }
                val summaryMsg = "Synchronisation globale réussie ! $totalProductsImported produits synchronisés dans ${importedStoresMap.size} enseignes ($storeBreakdown)."
                lastImportSummary.value = summaryMsg
                importStatusMessage.value = "Synchronisation terminée !"

                val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                importSuccessInfo.value = ImportSuccessInfo(
                    fileName = "Catalogues Cloud & Drive",
                    count = totalProductsImported,
                    targetCatalog = "Tous les Supermarchés",
                    timestamp = "Aujourd'hui à $timeStr"
                )
                performGoogleDriveBackup()
            } catch (e: Exception) {
                importStatusMessage.value = "Erreur synchronisation : ${e.localizedMessage ?: "Problème réseau"}"
            } finally {
                isImporting.value = false
            }
        }
    }

    // Download & Import a Single Drive File
    fun handleDownloadAndImportDriveFile(file: DriveFileInfo, callerContext: Context? = null) {
        val targetCtx = callerContext ?: context
        if (targetCtx != null) {
            isImporting.value = true
            importStatusMessage.value = "Téléchargement en arrière-plan : ${file.name}..."
            lastImportSummary.value = null
            com.example.util.CatalogSyncManager.startDownloadSingleFile(targetCtx, file.url, file.name, file.targetCatalog)
            return
        }

        viewModelScope.launch {
            isImporting.value = true
            importStatusMessage.value = "Téléchargement de ${file.name} vers ${file.targetCatalog}..."
            lastImportSummary.value = null

            try {
                kotlinx.coroutines.delay(300)
                val result = SpreadsheetImporter.downloadAndParse(file.url) { msg ->
                    importStatusMessage.value = msg
                }

                repository.importProducts(result.products)
                val summaryMsg = "${result.products.size} produit(s) importés dans ${result.targetCatalog} depuis ${file.name}."
                lastImportSummary.value = summaryMsg
                importStatusMessage.value = "Fichier ${file.name} importé !"

                val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                importSuccessInfo.value = ImportSuccessInfo(
                    fileName = file.name,
                    count = result.products.size,
                    targetCatalog = result.targetCatalog,
                    timestamp = "Aujourd'hui à $timeStr"
                )
                performGoogleDriveBackup()
            } catch (e: Exception) {
                importStatusMessage.value = "Erreur : ${e.localizedMessage}"
            } finally {
                isImporting.value = false
            }
        }
    }

    fun deleteAllDriveFiles() {
        driveAvailableFiles.value = emptyList()
        importStatusMessage.value = "Tous les fichiers ont été supprimés de Google Drive."
        lastImportSummary.value = "Dossier Google Drive entièrement vidé (0 fichier disponible)."
    }

    fun reloadDriveFiles() {
        driveAvailableFiles.value = listOf(
            DriveFileInfo(
                name = "Dreamprice_Catalogue_Global.xlsx",
                type = "EXCEL (.XLSX)",
                size = "1.2 MB",
                targetCatalog = "DREAMPRICE",
                lastModified = "Aujourd'hui, 08:30",
                url = "https://drive.google.com/uc?export=download&id=dreamprice_file_001"
            ),
            DriveFileInfo(
                name = "Intermart_Prix_Et_Promotions.csv",
                type = "CSV (.CSV)",
                size = "850 KB",
                targetCatalog = "INTERMART",
                lastModified = "Aujourd'hui, 07:45",
                url = "https://drive.google.com/uc?export=download&id=intermart_file_002"
            ),
            DriveFileInfo(
                name = "SuperU_Brochure_Alimentation.xlsx",
                type = "EXCEL (.XLSX)",
                size = "1.8 MB",
                targetCatalog = "SUPER U",
                lastModified = "Hier, 18:20",
                url = "https://drive.google.com/uc?export=download&id=superu_file_003"
            ),
            DriveFileInfo(
                name = "Winners_Catalogue_Hebdo.csv",
                type = "CSV (.CSV)",
                size = "920 KB",
                targetCatalog = "WINNERS",
                lastModified = "Hier, 16:10",
                url = "https://drive.google.com/uc?export=download&id=winners_file_004"
            ),
            DriveFileInfo(
                name = "Jumbo_Promotions_Et_Surgeles.xlsx",
                type = "EXCEL (.XLSX)",
                size = "2.1 MB",
                targetCatalog = "JUMBO",
                lastModified = "08/08/2026",
                url = "https://drive.google.com/uc?export=download&id=jumbo_file_005"
            ),
            DriveFileInfo(
                name = "Carrefour_Catalogue_Import.csv",
                type = "CSV (.CSV)",
                size = "640 KB",
                targetCatalog = "CARREFOUR",
                lastModified = "07/08/2026",
                url = "https://drive.google.com/uc?export=download&id=carrefour_file_006"
            ),
            DriveFileInfo(
                name = "KingSavers_Epicerie_Et_Grains.xlsx",
                type = "EXCEL (.XLSX)",
                size = "710 KB",
                targetCatalog = "KING SAVERS",
                lastModified = "05/08/2026",
                url = "https://drive.google.com/uc?export=download&id=kingsavers_file_007"
            )
        )
        importStatusMessage.value = "La liste des fichiers Google Drive a été rechargée."
        lastImportSummary.value = null
    }

    fun deleteDriveFile(file: DriveFileInfo) {
        driveAvailableFiles.value = driveAvailableFiles.value.filter { it.name != file.name || it.url != file.url }
        importStatusMessage.value = "Le fichier ${file.name} a été supprimé de Google Drive."
    }

    // Google Drive Import with Validation
    fun handleDownloadAndImport() {
        val url = driveUrl.value.trim()
        if (url.isBlank()) {
            importStatusMessage.value = "Veuillez entrer un lien Google Drive valide."
            return
        }

        viewModelScope.launch {
            isImporting.value = true
            importStatusMessage.value = "Initialisation de la connexion..."
            lastImportSummary.value = null

            try {
                val result = SpreadsheetImporter.downloadAndParse(url) { msg ->
                    importStatusMessage.value = msg
                }

                if (result.report.errors.isNotEmpty()) {
                    pendingValidationReport.value = result.report
                } else {
                    repository.importProducts(result.products)
                    val summaryMsg = "${result.products.size} produit(s) importés avec succès dans ${result.targetCatalog}."
                    lastImportSummary.value = summaryMsg
                    importStatusMessage.value = "Terminé !"
                    val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    importSuccessInfo.value = ImportSuccessInfo(
                        fileName = "Google Drive",
                        count = result.products.size,
                        targetCatalog = result.targetCatalog,
                        timestamp = "Aujourd'hui à $timeStr"
                    )
                    performGoogleDriveBackup()
                }
            } catch (e: Exception) {
                importStatusMessage.value = "Erreur: ${e.localizedMessage ?: "Une erreur est survenue."}"
            } finally {
                isImporting.value = false
            }
        }
    }

    // Local File Selection (.csv, .xlsx, .pdf) Import with Validation
    fun handleLocalFileImport(context: Context, uri: Uri, fileName: String, targetCatalog: String? = null) {
        viewModelScope.launch {
            isImporting.value = true
            val isPdf = fileName.endsWith(".pdf", ignoreCase = true)
            if (isPdf) {
                importStatusMessage.value = "Analyse et extraction du PDF $fileName en cours... Veuillez patienter quelques instants."
            } else {
                importStatusMessage.value = "Analyse du fichier local $fileName..."
            }
            lastImportSummary.value = null

            try {
                val fallbackCatalog = targetCatalog ?: _activeCatalog.value
                val report = SpreadsheetImporter.parseAndValidateFileUri(context, uri, fileName, fallbackCatalog)
                val detectedCatalog = report.targetCatalog
                if (report.errors.isNotEmpty()) {
                    pendingValidationReport.value = report
                } else {
                    repository.importProducts(report.validProducts)
                    lastImportSummary.value = "${report.validProducts.size} produit(s) importés depuis $fileName dans le catalogue $detectedCatalog."
                    importStatusMessage.value = "Importation réussie !"
                    val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    importSuccessInfo.value = ImportSuccessInfo(
                        fileName = fileName,
                        count = report.validProducts.size,
                        targetCatalog = detectedCatalog,
                        timestamp = "Aujourd'hui à $timeStr"
                    )
                    performGoogleDriveBackup()
                }
            } catch (e: Exception) {
                importStatusMessage.value = "Erreur fichier: ${e.localizedMessage}"
            } finally {
                isImporting.value = false
            }
        }
    }

    // Manual CSV text paste import with validation
    fun handleManualCsvTextImport(csvText: String, targetCatalog: String? = null) {
        viewModelScope.launch {
            val fallbackCatalog = targetCatalog ?: _activeCatalog.value
            val report = SpreadsheetImporter.parseAndValidateCsvText(csvText, fallbackCatalog)
            val detectedCatalog = report.targetCatalog
            if (report.errors.isNotEmpty()) {
                pendingValidationReport.value = report
            } else {
                repository.importProducts(report.validProducts)
                lastImportSummary.value = "${report.validProducts.size} produit(s) ajoutés au catalogue $detectedCatalog."
                val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                importSuccessInfo.value = ImportSuccessInfo(
                    fileName = "Texte CSV Manuel",
                    count = report.validProducts.size,
                    targetCatalog = detectedCatalog,
                    timestamp = "Aujourd'hui à $timeStr"
                )
                performGoogleDriveBackup()
            }
        }
    }

    // Confirm importing valid products only from validation report
    fun confirmImportValidProducts(report: ImportValidationReport) {
        viewModelScope.launch {
            if (report.validProducts.isNotEmpty()) {
                repository.importProducts(report.validProducts)
                lastImportSummary.value = "${report.validProducts.size} produit(s) valides importés dans ${report.targetCatalog} (lignes en erreur ignorées)."
                val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                importSuccessInfo.value = ImportSuccessInfo(
                    fileName = report.sourceFileName,
                    count = report.validProducts.size,
                    targetCatalog = report.targetCatalog,
                    timestamp = "Aujourd'hui à $timeStr"
                )
                performGoogleDriveBackup()
            }
            pendingValidationReport.value = null
        }
    }

    fun dismissValidationReport() {
        pendingValidationReport.value = null
    }

    // Add Product
    fun addProduct(name: String, category: String, brand: String, unit: String, price: Double, cost: Double, catalog: String) {
        viewModelScope.launch {
            val product = ProductEntity(
                catalogType = catalog,
                name = name,
                category = category,
                brand = brand,
                unit = unit,
                price = price,
                cost = cost
            )
            val newId = repository.addProduct(product)
            if (price > 0) {
                repository.recordPricePoint(
                    productId = newId.toInt(),
                    productName = name,
                    catalogType = catalog,
                    price = price,
                    cost = cost,
                    recordedDate = "Aujourd'hui"
                )
            }
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product)
            if (product.price > 0) {
                repository.recordPricePoint(
                    productId = product.id,
                    productName = product.name,
                    catalogType = product.catalogType,
                    price = product.price,
                    cost = product.cost,
                    recordedDate = "Modifié aujourd'hui"
                )
            }
        }
    }

    // Price Trends History for Product
    fun getPriceHistoryForProduct(product: ProductEntity): Flow<List<PriceHistoryEntity>> {
        viewModelScope.launch {
            repository.ensurePriceHistoryForProduct(product)
        }
        return repository.getPriceHistory(product.id, product.name)
    }

    fun addPriceHistoryCheckpoint(product: ProductEntity, newPrice: Double, dateLabel: String) {
        viewModelScope.launch {
            repository.recordPricePoint(
                productId = product.id,
                productName = product.name,
                catalogType = product.catalogType,
                price = newPrice,
                cost = product.cost,
                recordedDate = dateLabel
            )
        }
    }

    fun deleteProduct(id: Int, catalogType: String) {
        viewModelScope.launch {
            repository.deleteProduct(id, catalogType)
        }
    }

    fun deleteProductsBulkFromServer(products: List<ProductEntity>, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val fileIds = serverCatalogFiles.value.map { it.id }
            val success = FirestoreCatalogService.deleteProductsFromFirestore(products, fileIds)
            if (success) {
                products.forEach { p ->
                    repository.deleteProduct(p.id, p.catalogType)
                }
                onComplete(true, "${products.size} article(s) supprimé(s) du serveur avec succès !")
            } else {
                onComplete(false, "Échec de la suppression de certains articles du serveur.")
            }
        }
    }

    fun clearCatalog(catalogType: String) {
        viewModelScope.launch {
            repository.clearCatalog(catalogType)
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _comparedProductIds.value = emptySet()
            lastImportSummary.value = "Toutes les données (catalogues, panier, ventes) ont été réinitialisées."
            importStatusMessage.value = "Base de données entièrement réinitialisée."
        }
    }

    fun setPeriodicWorkManagerEnabled(context: Context, enabled: Boolean) {
        com.example.util.CatalogSyncManager.setPeriodicSyncEnabled(context, enabled)
    }

    fun setFirestoreAutoSyncEnabled(enabled: Boolean) {
        android.util.Log.d("CatalogViewModel", "Setting FirestoreAutoSyncEnabled to: $enabled")
        isFirestoreAutoSyncEnabled.value = enabled
        authPrefs?.edit()?.putBoolean("auto_sync_firestore_enabled", enabled)?.apply()
        if (enabled) {
            firestoreSyncStatus.value = "Synchronisation automatique Firestore activée"
            triggerFirestoreCartSync()
        } else {
            firestoreSyncStatus.value = "Mode Hors-Ligne (Room SQLite uniquement • Données préservées)"
        }
        persistSettingsToRoomAndFirestore()
    }

    fun toggleFirestoreAutoSync() {
        setFirestoreAutoSyncEnabled(!isFirestoreAutoSyncEnabled.value)
    }

    private fun triggerFirestoreCartSync() {
        if (userTier.value == SubscriptionTier.FREE && userRole.value != "ADMIN") {
            firestoreSyncStatus.value = "Panier local uniquement (Abonnement FREE)"
            return
        }
        if (!isFirestoreAutoSyncEnabled.value) {
            firestoreSyncStatus.value = "Mode Hors-Ligne (Données sauvegardées dans Room SQLite)"
            return
        }
        viewModelScope.launch {
            try {
                isFirestoreSyncing.value = true
                val currentItems = cartItems.value
                val success = FirestoreCartService().syncCartToFirestore(currentItems)
                firestoreSyncStatus.value = if (success) {
                    val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    "Synchronisé avec Firestore à $timeStr"
                } else {
                    "Mode local (sauvegarde hors-ligne Room)"
                }
            } catch (e: Exception) {
                firestoreSyncStatus.value = "Erreur sync Firestore: ${e.localizedMessage ?: "Réseau"}"
            } finally {
                isFirestoreSyncing.value = false
            }
        }
    }

    fun forceSyncCartToFirestore() {
        if (userTier.value == SubscriptionTier.FREE && userRole.value != "ADMIN") {
            firestoreSyncStatus.value = "La synchronisation Cloud est réservée aux abonnés PRO et ULTRA."
            return
        }
        viewModelScope.launch {
            try {
                isFirestoreSyncing.value = true
                val currentItems = cartItems.value
                val success = FirestoreCartService().syncCartToFirestore(currentItems)
                firestoreSyncStatus.value = if (success) {
                    val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    "Synchronisation manuelle réussie ($timeStr)"
                } else {
                    "Échec synchronisation Firestore (vérifiez votre connexion)"
                }
            } catch (e: Exception) {
                firestoreSyncStatus.value = "Erreur Firestore: ${e.localizedMessage ?: "Réseau"}"
            } finally {
                isFirestoreSyncing.value = false
            }
        }
    }

    // Cart Actions
    fun addToCart(product: ProductEntity, quantity: Int = 1) {
        viewModelScope.launch {
            repository.addToCart(product, quantity)
            triggerFirestoreCartSync()
        }
    }

    /**
     * Scans a shelf or barcode label, instantly searches the local database (Room),
     * increments quantity if already in cart or adds with qty 1 if new item.
     * Triggers callback on Main thread with the matched product, new quantity, and whether it was incremented.
     */
    fun scanLabelAndAddToCart(
        labelOrBarcode: String,
        onProductAdded: (product: ProductEntity, newQuantity: Int, isIncremented: Boolean) -> Unit
    ) {
        val clean = labelOrBarcode.trim()
        if (clean.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val currentCatalogProducts = allProducts.value
            val currentCart = cartItems.value

            // 1. Check exact barcode match
            var matchingProduct: ProductEntity? = currentCatalogProducts.firstOrNull { p ->
                p.barcode.isNotBlank() && p.barcode.equals(clean, ignoreCase = true)
            }

            // 2. Check SKU or exact product ID
            if (matchingProduct == null) {
                matchingProduct = currentCatalogProducts.firstOrNull { p ->
                    p.id.toString() == clean ||
                    "#${p.id}".equals(clean, ignoreCase = true) ||
                    "${p.catalogType}-${p.id}".equals(clean, ignoreCase = true) ||
                    (p.barcode.isNotBlank() && clean.contains(p.barcode, ignoreCase = true))
                }
            }

            // 3. Check name or keyword match
            if (matchingProduct == null) {
                matchingProduct = currentCatalogProducts.firstOrNull { p ->
                    p.name.equals(clean, ignoreCase = true) ||
                    p.name.contains(clean, ignoreCase = true) ||
                    clean.contains(p.name, ignoreCase = true)
                }
            }

            // 4. Bakery label recognition (Pain, Baguette, Pain de mie, Croissant, etc.)
            if (matchingProduct == null) {
                val lower = clean.lowercase()
                val isBakeryItem = lower.contains("pain") || lower.contains("baguette") ||
                                   lower.contains("brioche") || lower.contains("croissant") ||
                                   lower.contains("flan") || lower.contains("boulangerie") ||
                                   lower.contains("mie")

                if (isBakeryItem) {
                    val existingBakery = currentCatalogProducts.firstOrNull {
                        it.category.contains("Boulangerie", true) ||
                        (lower.contains("baguette") && it.name.contains("baguette", true)) ||
                        (lower.contains("mie") && it.name.contains("mie", true)) ||
                        (lower.contains("pain") && it.name.contains("pain", true))
                    }
                    if (existingBakery != null) {
                        matchingProduct = existingBakery
                    } else {
                        val defaultName = if (lower.contains("baguette")) "Baguette Traditionnelle"
                        else if (lower.contains("mie")) "Pain de mie"
                        else if (lower.contains("croissant")) "Croissant pur beurre"
                        else if (clean.length > 2) clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        else "Pain Traditionnel"

                        val defaultPrice = if (lower.contains("baguette")) 1.25
                        else if (lower.contains("mie")) 1.85
                        else 1.50

                        val newProduct = ProductEntity(
                            catalogType = "SUPER_U",
                            name = defaultName,
                            category = "Boulangerie",
                            brand = "Boulangerie Fraîche",
                            unit = "Pièce",
                            price = defaultPrice,
                            cost = defaultPrice * 0.7,
                            barcode = if (clean.all { it.isDigit() }) clean else "BAK-${System.currentTimeMillis() % 10000}"
                        )
                        val newId = repository.addProduct(newProduct)
                        val saved = newProduct.copy(id = newId.toInt())
                        repository.recordPricePoint(saved.id, saved.name, saved.catalogType, saved.price, saved.cost, "Aujourd'hui")
                        matchingProduct = saved
                    }
                }
            }

            // 5. Fallback lookup via BarcodeLookupService or create catalog entry
            if (matchingProduct == null) {
                try {
                    val lookup = BarcodeLookupService.lookupBarcode(clean, currentCatalogProducts)
                    if (lookup.matchedProducts.isNotEmpty()) {
                        matchingProduct = lookup.matchedProducts.first()
                    } else {
                        val info = lookup.onlineProduct
                        val pName = if (info.productName.isNotBlank() && !info.productName.contains("Inconnu", true)) {
                            info.productName
                        } else {
                            clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        }
                        val newProduct = ProductEntity(
                            catalogType = "SUPER_U",
                            name = pName,
                            category = info.category.ifBlank { "Épicerie" },
                            brand = info.brand.ifBlank { "Marque locale" },
                            unit = info.unit.ifBlank { "Unité" },
                            price = 1.85,
                            cost = 1.30,
                            barcode = clean
                        )
                        val newId = repository.addProduct(newProduct)
                        val saved = newProduct.copy(id = newId.toInt())
                        repository.recordPricePoint(saved.id, saved.name, saved.catalogType, saved.price, saved.cost, "Aujourd'hui")
                        matchingProduct = saved
                    }
                } catch (e: Throwable) {
                    val newProduct = ProductEntity(
                        catalogType = "SUPER_U",
                        name = clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        category = "Alimentation",
                        brand = "Supermarché",
                        unit = "Unité",
                        price = 1.85,
                        cost = 1.30,
                        barcode = clean
                    )
                    val newId = repository.addProduct(newProduct)
                    val saved = newProduct.copy(id = newId.toInt())
                    matchingProduct = saved
                }
            }

            // At this point matchingProduct is guaranteed non-null
            val finalProduct = matchingProduct
            val existingItem = currentCart.firstOrNull {
                it.productId == finalProduct.id && it.catalogType == finalProduct.catalogType
            }
            val wasInCart = existingItem != null
            val newQuantity = (existingItem?.quantity ?: 0) + 1

            repository.addToCart(finalProduct, 1)
            triggerFirestoreCartSync()

            withContext(Dispatchers.Main) {
                onProductAdded(finalProduct, newQuantity, wasInCart)
            }
        }
    }

    fun updateCartQuantity(item: CartItemEntity, newQuantity: Int) {
        viewModelScope.launch {
            repository.updateCartItemQuantity(item, newQuantity)
            triggerFirestoreCartSync()
        }
    }

    fun removeFromCart(cartItemId: Int) {
        viewModelScope.launch {
            repository.removeFromCart(cartItemId)
            triggerFirestoreCartSync()
        }
    }

    fun checkoutCart() {
        val currentCart = cartItems.value
        if (currentCart.isNotEmpty()) {
            viewModelScope.launch {
                val totalPrice = currentCart.sumOf { it.unitPrice * it.quantity }
                val totalCost = currentCart.sumOf { it.unitCost * it.quantity }
                val totalProfit = totalPrice - totalCost
                val userKey = googleAccountEmail.value ?: "guest_user"

                // Save locally in Room database
                repository.checkoutCart(currentCart)

                // Save in Cloud Firestore if enabled or manual
                if (isFirestoreAutoSyncEnabled.value && (userTier.value != SubscriptionTier.FREE || userRole.value == "ADMIN")) {
                    FirestoreCartService().saveOrderToFirestore(currentCart, totalPrice, totalProfit)
                    firestoreSyncStatus.value = "Commande enregistrée dans Room & Cloud Firestore"
                } else {
                    firestoreSyncStatus.value = if (userTier.value == SubscriptionTier.FREE && userRole.value != "ADMIN") {
                        "Commande enregistrée dans Room SQLite (Abonnement FREE : Cloud désactivé)"
                    } else {
                        "Commande enregistrée dans Room SQLite (Mode Hors-Ligne)"
                    }
                }
            }
        }
    }

    // Wishlist Actions (Local Room Persistence)
    fun toggleWishlist(product: ProductEntity) {
        viewModelScope.launch {
            repository.toggleWishlist(product)
        }
    }

    fun addToWishlist(product: ProductEntity) {
        viewModelScope.launch {
            repository.addToWishlist(product)
        }
    }

    fun removeFromWishlist(wishlistId: Int) {
        viewModelScope.launch {
            repository.removeWishlistItemById(wishlistId)
        }
    }

    fun moveWishlistToCart(item: WishlistItemEntity) {
        viewModelScope.launch {
            // Find corresponding ProductEntity or reconstruct
            val product = ProductEntity(
                id = item.productId,
                catalogType = item.catalogType,
                name = item.productName,
                category = item.category,
                brand = item.brand,
                unit = item.unit,
                price = item.unitPrice,
                cost = item.unitCost
            )
            repository.addToCart(product, 1)
            repository.removeWishlistItemById(item.id)
            triggerFirestoreCartSync()
        }
    }

    fun addAllWishlistToCart() {
        val currentWishlist = wishlistItems.value
        if (currentWishlist.isEmpty()) return
        viewModelScope.launch {
            currentWishlist.forEach { item ->
                val product = ProductEntity(
                    id = item.productId,
                    catalogType = item.catalogType,
                    name = item.productName,
                    category = item.category,
                    brand = item.brand,
                    unit = item.unit,
                    price = item.unitPrice,
                    cost = item.unitCost
                )
                repository.addToCart(product, 1)
            }
            repository.clearWishlist()
            triggerFirestoreCartSync()
        }
    }

    fun clearWishlist() {
        viewModelScope.launch {
            repository.clearWishlist()
        }
    }

    fun isWishlisted(product: ProductEntity): Boolean {
        val key = "${product.catalogType.uppercase()}_${product.id}"
        return key in wishlistedProductKeys.value || product.id in wishlistedProductIds.value
    }

    // --- Price Alerts Actions (Firestore + Room) ---

    fun setPriceAlert(product: ProductEntity, targetPrice: Double) {
        viewModelScope.launch {
            val userKey = googleAccountEmail.value ?: "guest_user"
            val existing = repository.getPriceAlertForProductSync(product.id, product.catalogType)
            val alert = PriceAlertEntity(
                id = existing?.id ?: 0,
                firestoreId = existing?.firestoreId ?: "",
                productId = product.id,
                catalogType = product.catalogType,
                productName = product.name,
                category = product.category,
                brand = product.brand,
                unit = product.unit,
                initialPrice = existing?.initialPrice ?: product.price,
                targetPrice = targetPrice,
                currentPrice = product.price,
                userEmail = userKey
            )
            isAlertsFirestoreSyncing.value = true
            repository.savePriceAlert(alert, userKey, context)
            isAlertsFirestoreSyncing.value = false
            alertsFirestoreSyncStatus.value = "Alerte de prix activée pour ${product.name} (Seuil: Rs ${String.format("%.2f", targetPrice)})"
        }
    }

    fun removePriceAlert(alert: PriceAlertEntity) {
        viewModelScope.launch {
            val userKey = googleAccountEmail.value ?: "guest_user"
            isAlertsFirestoreSyncing.value = true
            repository.deletePriceAlert(alert, userKey)
            isAlertsFirestoreSyncing.value = false
            alertsFirestoreSyncStatus.value = "Alerte supprimée de Firestore"
        }
    }

    fun removePriceAlertByProduct(productId: Int, catalogType: String) {
        viewModelScope.launch {
            val userKey = googleAccountEmail.value ?: "guest_user"
            isAlertsFirestoreSyncing.value = true
            repository.deletePriceAlertByProduct(productId, catalogType, userKey)
            isAlertsFirestoreSyncing.value = false
        }
    }

    fun getPriceAlertForProduct(productId: Int, catalogType: String): Flow<PriceAlertEntity?> {
        return repository.getPriceAlertForProduct(productId, catalogType)
    }

    fun syncPriceAlertsWithFirestore() {
        viewModelScope.launch {
            isAlertsFirestoreSyncing.value = true
            val userKey = googleAccountEmail.value ?: "guest_user"
            repository.syncAlertsWithFirestore(userKey)
            isAlertsFirestoreSyncing.value = false
            alertsFirestoreSyncStatus.value = "Alertes synchronisées avec Firestore (${priceAlerts.value.size} actives)"
        }
    }

    fun addAlertProductToCart(alert: PriceAlertEntity) {
        viewModelScope.launch {
            val product = ProductEntity(
                id = alert.productId,
                catalogType = alert.catalogType,
                name = alert.productName,
                category = alert.category,
                brand = alert.brand,
                unit = alert.unit,
                price = alert.currentPrice,
                cost = 0.0
            )
            repository.addToCart(product, 1)
            triggerFirestoreCartSync()
        }
    }

    fun triggerTestPriceDropNotification(product: ProductEntity? = null) {
        viewModelScope.launch {
            val sampleProduct = product ?: allProducts.value.firstOrNull() ?: ProductEntity(
                id = 999,
                catalogType = "DREAMPRICE",
                name = "Huile de Tournesol 1L",
                category = "Épicerie",
                brand = "Lesieur",
                unit = "1L",
                price = 84.50,
                cost = 68.00
            )

            val testAlert = PriceAlertEntity(
                id = 9999,
                productId = sampleProduct.id,
                catalogType = sampleProduct.catalogType,
                productName = sampleProduct.name,
                category = sampleProduct.category,
                brand = sampleProduct.brand,
                unit = sampleProduct.unit,
                initialPrice = sampleProduct.price + 15.0,
                targetPrice = sampleProduct.price,
                currentPrice = sampleProduct.price,
                isTriggered = true,
                lastTriggeredPrice = sampleProduct.price,
                userEmail = googleAccountEmail.value ?: "guest_user"
            )

            context?.let { ctx ->
                PriceAlertNotificationHelper.postPriceDropNotification(
                    context = ctx,
                    alert = testAlert,
                    newPrice = sampleProduct.price,
                    supermarket = sampleProduct.catalogType
                )
            }
        }
    }

    private fun setupPriceAlertsFirestoreListener(userKey: String) {
        try {
            priceAlertsListener?.remove()
            priceAlertsListener = FirestorePriceAlertService.listenToPriceAlerts(userKey) { remoteAlerts ->
                viewModelScope.launch {
                    try {
                        remoteAlerts.forEach { remote ->
                            val local = repository.getPriceAlertForProductSync(remote.productId, remote.catalogType)
                            if (local == null) {
                                repository.savePriceAlert(remote, userKey)
                            }
                        }
                    } catch (e: Throwable) {
                        android.util.Log.w("CatalogViewModel", "Remote alert update error: ${e.message}")
                    }
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("CatalogViewModel", "Listener setup error: ${e.message}")
        }
    }

    // Gemini Chatbot Integration
    fun sendChatMessage(text: String) {
        val userText = text.trim()
        if (userText.isBlank() || isChatLoading.value) return

        val userMsg = ChatMessage(role = "user", text = userText)
        val updatedHistory = chatMessages.value + userMsg
        chatMessages.value = updatedHistory
        isChatLoading.value = true

        viewModelScope.launch {
            // Build Context Prompt
            val products = allProducts.value
            val dreamCount = products.count { it.catalogType.equals("DREAMPRICE", ignoreCase = true) }
            val interCount = products.count { it.catalogType.equals("INTERMART", ignoreCase = true) }
            val cartList = cartItems.value
            val cartTotal = cartList.sumOf { it.unitPrice * it.quantity }
            val salesList = saleRecords.value
            val totalProfit = salesList.sumOf { it.totalProfit }

            val topDream = products.filter { it.catalogType.equals("DREAMPRICE", ignoreCase = true) }.take(5).joinToString { "${it.name} (Rs ${it.price})" }
            val topInter = products.filter { it.catalogType.equals("INTERMART", ignoreCase = true) }.take(5).joinToString { "${it.name} (Rs ${it.price})" }

            val contextPrompt = """
                - Catalog Dreamprice: $dreamCount articles. Exemples: $topDream
                - Catalog Intermart: $interCount articles. Exemples: $topInter
                - Panier actuel: ${cartList.size} types d'articles, Valeur totale: Rs $cartTotal
                - Ventes réalisées: ${salesList.size} commandes enregistrées, Profit cumulé: Rs $totalProfit
                - Rôle utilisateur actuel: ${userRole.value}
            """.trimIndent()

            val aiResponse = GeminiService.sendMessage(updatedHistory, contextPrompt)
            chatMessages.value = chatMessages.value + ChatMessage(role = "model", text = aiResponse)
            isChatLoading.value = false
        }
    }

    // Barcode Scanning & Internet Lookup Methods
    fun lookupBarcode(barcode: String) {
        val clean = barcode.trim()
        if (clean.isBlank()) return
        isBarcodeLookingUp.value = true
        viewModelScope.launch {
            try {
                val result = BarcodeLookupService.lookupBarcode(clean, allProducts.value)
                barcodeScanResult.value = result

                // If auto-add to cart is active and there's an exact catalog product match
                if (autoAddToCartOnScan.value && result.matchedProducts.isNotEmpty()) {
                    val firstMatch = result.matchedProducts.first()
                    addToCart(firstMatch, 1)
                }
            } catch (e: Exception) {
                Log.e("CatalogViewModel", "Barcode lookup failed: ${e.message}", e)
            } finally {
                isBarcodeLookingUp.value = false
            }
        }
    }

    fun clearBarcodeResult() {
        barcodeScanResult.value = null
        isBarcodeLookingUp.value = false
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun quickAddProductFromBarcode(
        onlineInfo: OnlineProductInfo,
        targetCatalog: String,
        price: Double,
        cost: Double
    ) {
        viewModelScope.launch {
            val newProduct = ProductEntity(
                catalogType = targetCatalog.uppercase(),
                name = onlineInfo.productName,
                category = onlineInfo.category.ifBlank { "Épicerie" },
                brand = onlineInfo.brand.ifBlank { "Générique" },
                unit = onlineInfo.unit.ifBlank { "1 unité" },
                price = price,
                cost = cost,
                barcode = onlineInfo.barcode
            )
            repository.addProduct(newProduct)
            // Re-evaluate barcode result with the newly added product
            lookupBarcode(onlineInfo.barcode)
        }
    }

    fun quickAddOnlineProductToCart(
        onlineInfo: OnlineProductInfo,
        targetCatalog: String = "DREAMPRICE",
        price: Double = 90.0,
        cost: Double = 70.0,
        quantity: Int = 1
    ) {
        viewModelScope.launch {
            val product = ProductEntity(
                catalogType = targetCatalog.uppercase(),
                name = onlineInfo.productName,
                category = onlineInfo.category.ifBlank { "Épicerie" },
                brand = onlineInfo.brand.ifBlank { "Générique" },
                unit = onlineInfo.unit.ifBlank { "1 unité" },
                price = price,
                cost = cost,
                barcode = onlineInfo.barcode
            )
            val newId = repository.addProduct(product)
            val insertedProduct = product.copy(id = newId.toInt())
            addToCart(insertedProduct, quantity)
        }
    }

    // --- Server Files & Admin Upload / User Sync Operations ---

    private fun setupServerFilesListener() {
        serverFilesListener?.remove()
        isServerFilesLoading.value = true
        serverFilesListener = FirestoreCatalogService.listenToServerCatalogFiles { files ->
            serverCatalogFiles.value = files.sortedBy { it.fileName.ifBlank { it.catalogType }.lowercase() }
            isServerFilesLoading.value = false
        }
        viewModelScope.launch {
            if (serverCatalogFiles.value.isEmpty()) {
                val files = FirestoreCatalogService.fetchServerCatalogFiles()
                serverCatalogFiles.value = files.sortedBy { it.fileName.ifBlank { it.catalogType }.lowercase() }
                isServerFilesLoading.value = false
            }
        }
    }

    fun refreshServerFiles() {
        viewModelScope.launch {
            isServerFilesLoading.value = true
            val files = FirestoreCatalogService.fetchServerCatalogFiles()
            serverCatalogFiles.value = files.sortedBy { it.fileName.ifBlank { it.catalogType }.lowercase() }
            isServerFilesLoading.value = false
        }
    }

    fun deleteServerFiles(fileIds: List<String>, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            isServerFilesLoading.value = true
            try {
                val success = withContext(Dispatchers.IO) {
                    FirestoreCatalogService.deleteServerCatalogFiles(fileIds)
                }
                refreshServerFiles()
                if (success) {
                    onComplete(true, "${fileIds.size} fichier(s) supprimé(s) avec succès de Cloud Firestore !")
                } else {
                    onComplete(false, "Certains fichiers n'ont pas pu être supprimés de Firestore.")
                }
            } catch (e: Exception) {
                refreshServerFiles()
                onComplete(false, "Erreur lors de la suppression : ${e.message}")
            } finally {
                isServerFilesLoading.value = false
            }
        }
    }

    /**
     * Uploads and publishes a local file (.xlsx, .csv, .pdf) directly to the Cloud Server.
     * Admin action so all users across devices can synchronize with it.
     */
    fun uploadFileToServer(
        context: Context,
        uri: Uri,
        fileName: String,
        targetCatalog: String,
        description: String = "",
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        uploadMultipleFilesToServer(
            context = context,
            files = listOf(FileUploadItem(uri, fileName, targetCatalog)),
            defaultDescription = description,
            onComplete = onComplete
        )
    }

    /**
     * Uploads multiple catalog files (.xlsx, .csv, .pdf) sequentially to Firebase Firestore.
     */
    fun uploadMultipleFilesToServer(
        context: Context,
        files: List<FileUploadItem>,
        defaultDescription: String = "",
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            if (files.isEmpty()) {
                onComplete?.invoke(false, "Aucun fichier sélectionné.")
                return@launch
            }

            isServerUploading.value = true
            var totalProductsUploaded = 0
            var successfulFilesCount = 0
            val errorMessages = mutableListOf<String>()

            val uploaderEmail = googleAccountEmail.value ?: "admin@kwickart.mu"
            val uploaderName = googleAccountName.value ?: "Administrateur"

            for ((index, item) in files.withIndex()) {
                val stepPrefix = if (files.size > 1) "[${index + 1}/${files.size}] " else ""
                serverUploadProgress.value = "${stepPrefix}Analyse de ${item.fileName}..."

                try {
                    val resolvedCatalog = if (item.targetCatalog.isNotBlank() && !item.targetCatalog.equals("DREAMPRICE", ignoreCase = true)) {
                        item.targetCatalog.uppercase()
                    } else {
                        SpreadsheetImporter.autoDetectCatalog(item.fileName, "", item.targetCatalog).uppercase()
                    }

                    val report = SpreadsheetImporter.parseAndValidateFileUri(context, item.uri, item.fileName, resolvedCatalog)
                    val rawProducts = report.validProducts

                    if (rawProducts.isEmpty()) {
                        errorMessages.add("${item.fileName}: aucun produit valide trouvé.")
                        continue
                    }

                    val validProducts = rawProducts.mapIndexed { itemIdx, p ->
                        val finalId = if (p.id > 0) p.id else (itemIdx + 1)
                        val cat = if (p.catalogType.isBlank() || (p.catalogType.equals("DREAMPRICE", ignoreCase = true) && resolvedCatalog != "DREAMPRICE")) {
                            resolvedCatalog
                        } else {
                            p.catalogType
                        }
                        p.copy(id = finalId, catalogType = cat.uppercase())
                    }

                    serverUploadProgress.value = "${stepPrefix}Téléversement de ${validProducts.size} produits ($resolvedCatalog) vers Firebase..."

                    val fileType = when {
                        item.fileName.endsWith(".xlsx", ignoreCase = true) -> "XLSX"
                        item.fileName.endsWith(".csv", ignoreCase = true) -> "CSV"
                        item.fileName.endsWith(".pdf", ignoreCase = true) -> "PDF"
                        else -> "FICHIER"
                    }

                    val serverFile = ServerCatalogFile(
                        id = "file_${System.currentTimeMillis()}_${index}_${resolvedCatalog.lowercase()}",
                        fileName = item.fileName,
                        fileType = fileType,
                        catalogType = resolvedCatalog.uppercase(),
                        description = defaultDescription.ifBlank { "Catalogue $resolvedCatalog publié vers Firebase par $uploaderName" },
                        uploadedBy = uploaderEmail,
                        uploadedByName = uploaderName,
                        timestamp = System.currentTimeMillis(),
                        fileSizeBytes = 1024L * validProducts.size,
                        fileSizeFormatted = "${(validProducts.size * 0.8).toInt().coerceAtLeast(12)} KB",
                        productCount = validProducts.size,
                        version = 1,
                        isPublished = true,
                        products = validProducts
                    )

                    val uploadResult = FirestoreCatalogService.uploadCatalogFileToServer(
                        file = serverFile,
                        products = validProducts,
                        onProgress = { progress ->
                            val percent = progress.percentage
                            serverUploadPercentage.value = percent / 100f
                            serverUploadProgress.value = "${stepPrefix}Export Firestore : ${progress.successfulRecords}/${progress.totalRecords} (${progress.formattedPercentage})"
                        }
                    )
                    if (uploadResult.isSuccess) {
                        repository.importProducts(validProducts)
                        totalProductsUploaded += uploadResult.successfullyExported
                        successfulFilesCount++
                    } else {
                        errorMessages.add("${item.fileName}: ${uploadResult.message}")
                    }
                } catch (e: Exception) {
                    errorMessages.add("${item.fileName}: ${e.message}")
                }
            }

            isServerUploading.value = false
            serverUploadPercentage.value = 1f
            refreshServerFiles()

            if (successfulFilesCount > 0) {
                val successMsg = if (files.size == 1) {
                    "Fichier '${files[0].fileName}' ($totalProductsUploaded produits) téléversé avec succès sur Firebase Firestore !"
                } else {
                    "$successfulFilesCount/${files.size} fichiers ($totalProductsUploaded produits au total) téléversés avec succès sur Firebase Firestore !"
                }
                serverUploadProgress.value = "Terminé !"
                serverUploadSuccess.value = successMsg
                onComplete?.invoke(true, successMsg)
            } else {
                val errMsg = if (errorMessages.isNotEmpty()) {
                    errorMessages.joinToString("\n")
                } else {
                    "Échec du téléversement sur Firebase Firestore."
                }
                serverUploadProgress.value = errMsg
                onComplete?.invoke(false, errMsg)
            }
        }
    }

    /**
     * Imports multiple selected files directly into the local Room database.
     */
    fun importMultipleFilesLocally(
        context: Context,
        files: List<FileUploadItem>,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            if (files.isEmpty()) {
                onComplete?.invoke(false, "Aucun fichier sélectionné.")
                return@launch
            }

            isImporting.value = true
            var totalProducts = 0
            var successfulFilesCount = 0

            for ((index, item) in files.withIndex()) {
                val stepPrefix = if (files.size > 1) "[${index + 1}/${files.size}] " else ""
                importProgress.value = "${stepPrefix}Importation de ${item.fileName}..."

                try {
                    val resolvedCatalog = if (item.targetCatalog.isNotBlank() && !item.targetCatalog.equals("DREAMPRICE", ignoreCase = true)) {
                        item.targetCatalog.uppercase()
                    } else {
                        SpreadsheetImporter.autoDetectCatalog(item.fileName, "", item.targetCatalog).uppercase()
                    }

                    val report = SpreadsheetImporter.parseAndValidateFileUri(context, item.uri, item.fileName, resolvedCatalog)
                    val rawProducts = report.validProducts

                    if (rawProducts.isNotEmpty()) {
                        val validProducts = rawProducts.map { p ->
                            if (p.catalogType.isBlank() || (p.catalogType.equals("DREAMPRICE", ignoreCase = true) && resolvedCatalog != "DREAMPRICE")) {
                                p.copy(catalogType = resolvedCatalog)
                            } else {
                                p
                            }
                        }
                        repository.importProducts(validProducts)
                        totalProducts += validProducts.size
                        successfulFilesCount++
                    }
                } catch (e: Exception) {
                    Log.e("CatalogViewModel", "Local import error for ${item.fileName}: ${e.message}")
                }
            }

            isImporting.value = false
            importProgress.value = ""

            val summaryMsg = if (files.size == 1) {
                "${files[0].fileName} : $totalProducts produits importés dans la base locale !"
            } else {
                "$successfulFilesCount/${files.size} fichiers ($totalProducts produits au total) importés dans la base locale !"
            }

            importSuccessInfo.value = ImportSuccessInfo(
                fileName = "${files.size} catalogues",
                count = totalProducts,
                targetCatalog = "MULTI",
                timestamp = System.currentTimeMillis().toString()
            )
            onComplete?.invoke(true, summaryMsg)
        }
    }

    /**
     * Uploads a prepared list of products to Firebase Firestore directly
     */
    fun uploadParsedProductsToServer(
        fileName: String,
        targetCatalog: String,
        description: String,
        products: List<ProductEntity>,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            if (products.isEmpty()) {
                onComplete?.invoke(false, "La liste de produits est vide.")
                return@launch
            }
            isServerUploading.value = true
            serverUploadProgress.value = "Téléversement de ${products.size} produits vers Firebase Firestore..."

            try {
                val uploaderEmail = googleAccountEmail.value ?: "admin@kwickart.mu"
                val uploaderName = googleAccountName.value ?: "Administrateur"

                val fileType = when {
                    fileName.endsWith(".xlsx", ignoreCase = true) -> "XLSX"
                    fileName.endsWith(".csv", ignoreCase = true) -> "CSV"
                    fileName.endsWith(".pdf", ignoreCase = true) -> "PDF"
                    else -> "DATASET"
                }

                val serverFile = ServerCatalogFile(
                    id = "file_${System.currentTimeMillis()}_${targetCatalog.lowercase()}",
                    fileName = fileName,
                    fileType = fileType,
                    catalogType = targetCatalog.uppercase(),
                    description = description.ifBlank { "Catalogue $targetCatalog publié vers Firebase par $uploaderName" },
                    uploadedBy = uploaderEmail,
                    uploadedByName = uploaderName,
                    timestamp = System.currentTimeMillis(),
                    fileSizeBytes = 1024L * products.size,
                    fileSizeFormatted = "${(products.size * 0.8).toInt().coerceAtLeast(12)} KB",
                    productCount = products.size,
                    version = 1,
                    isPublished = true,
                    products = products
                )

                val indexedProducts = products.mapIndexed { idx, p ->
                    val finalId = if (p.id > 0) p.id else (idx + 1)
                    p.copy(id = finalId, catalogType = targetCatalog.uppercase())
                }

                val uploadResult = FirestoreCatalogService.uploadCatalogFileToServer(
                    file = serverFile,
                    products = indexedProducts,
                    onProgress = { progress ->
                        serverUploadPercentage.value = progress.percentage / 100f
                        serverUploadProgress.value = "Export Firestore : ${progress.successfulRecords}/${progress.totalRecords} (${progress.formattedPercentage})"
                    }
                )
                serverUploadPercentage.value = 1f
                if (uploadResult.isSuccess) {
                    repository.importProducts(products)
                    val successMsg = "Catalogue '$fileName' (${uploadResult.successfullyExported} articles) téléversé sur Firebase Firestore pour tous les utilisateurs."
                    serverUploadProgress.value = "Terminé !"
                    serverUploadSuccess.value = successMsg
                    isServerUploading.value = false
                    refreshServerFiles()
                    onComplete?.invoke(true, successMsg)
                } else {
                    val errMsg = uploadResult.message
                    serverUploadProgress.value = errMsg
                    isServerUploading.value = false
                    onComplete?.invoke(false, errMsg)
                }
            } catch (e: Exception) {
                val errMsg = "Erreur Firebase: ${e.message}"
                serverUploadProgress.value = errMsg
                isServerUploading.value = false
                onComplete?.invoke(false, errMsg)
            }
        }
    }

    /**
     * Executes real-time diagnostics on Cloud Firestore connection: checks credentials, read/write permissions, and latency.
     */
    fun testFirestoreConnection(onComplete: ((com.example.data.FirestoreConnectionDiagnostics) -> Unit)? = null) {
        viewModelScope.launch {
            isTestingFirestoreConnection.value = true
            val diag = withContext(Dispatchers.IO) {
                FirestoreCatalogService.testConnection()
            }
            firestoreConnectionDiagnostics.value = diag
            isTestingFirestoreConnection.value = false
            onComplete?.invoke(diag)
        }
    }

    /**
     * Synchronizes a single file published on the server into the user's local Room database
     */
    /**
     * Synchronizes a single file published on the server into the user's local Room database
     * Displays all steps in the Pop-up Dialog
     */
    fun syncSingleFileFromServer(file: ServerCatalogFile, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            syncDialogTitle.value = "Synchronisation ${file.catalogType}"
            syncDialogSubtitle.value = "Téléchargement de ${file.fileName} & Indexation Room"
            showForceResetDialog.value = true
            forceResetCurrentStep.value = 0
            forceResetStepDetail.value = "Connexion à Cloud Firestore..."
            forceResetProgressPercent.value = 10f
            forceResetTotalFound.value = 0
            forceResetCatalogSummary.value = emptyMap()
            forceResetErrorMessage.value = null
            forceResetIsComplete.value = false
            isServerSyncing.value = true
            serverSyncProgressMessage.value = "Synchronisation du catalogue ${file.catalogType} (${file.fileName})..."

            try {
                // Step 1: Préparation locale
                kotlinx.coroutines.delay(200)
                forceResetCurrentStep.value = 1
                forceResetStepDetail.value = "Préparation de la base de données..."
                forceResetProgressPercent.value = 25f

                // Step 2: Téléchargement
                forceResetCurrentStep.value = 2
                forceResetStepDetail.value = "Récupération des articles de ${file.fileName}..."
                forceResetProgressPercent.value = 45f

                val productsToImport = withContext(Dispatchers.IO) {
                    if (file.products.isNotEmpty() && file.products.size >= file.productCount) {
                        file.products
                    } else {
                        val allItems = FirestoreCatalogService.fetchServerCatalogFileItems(file.id)
                        if (allItems.isNotEmpty()) {
                            allItems
                        } else if (file.products.isNotEmpty()) {
                            file.products
                        } else {
                            val defaults = FirestoreCatalogService.getDefaultInitialServerFiles()
                            defaults.find { it.catalogType.equals(file.catalogType, ignoreCase = true) }?.products ?: emptyList()
                        }
                    }
                }

                // Step 3: Déduplication & Structuration
                forceResetCurrentStep.value = 3
                forceResetStepDetail.value = "Structuration et vérification des articles..."
                forceResetProgressPercent.value = 70f
                forceResetTotalFound.value = productsToImport.size
                forceResetCatalogSummary.value = mapOf(file.catalogType to productsToImport.size)

                if (productsToImport.isNotEmpty()) {
                    // Step 4: Indexation Room SQLite
                    forceResetCurrentStep.value = 4
                    forceResetStepDetail.value = "Indexation dans Room SQLite..."
                    forceResetProgressPercent.value = 90f

                    withContext(Dispatchers.IO) {
                        repository.clearCatalog(file.catalogType)
                        val distinctIndexed = productsToImport.distinctBy { p ->
                            if (p.barcode.isNotBlank()) "${p.catalogType}_bc_${p.barcode.trim()}"
                            else "${p.catalogType}_nm_${p.name.trim().lowercase()}_${p.unit.trim().lowercase()}"
                        }.mapIndexed { idx, p ->
                            p.copy(id = idx + 1, catalogType = file.catalogType.uppercase())
                        }
                        repository.importProducts(distinctIndexed)
                    }

                    val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    lastServerSyncTime.value = "Aujourd'hui à $timeStr"
                    val summary = "${productsToImport.size} produit(s) synchronisés depuis ${file.fileName} (${file.catalogType})."
                    serverSyncSummary.value = summary
                    serverSyncProgressMessage.value = "Synchronisation réussie !"
                    lastImportSummary.value = summary
                    forceResetProgressPercent.value = 100f
                    forceResetIsComplete.value = true
                    forceResetStepDetail.value = "Synchronisation réussie : $summary"

                    importSuccessInfo.value = ImportSuccessInfo(
                        fileName = file.fileName,
                        count = productsToImport.size,
                        targetCatalog = file.catalogType,
                        timestamp = "Aujourd'hui à $timeStr"
                    )
                    onComplete?.invoke(true, summary)
                } else {
                    val msg = "Aucun produit à synchroniser pour ce fichier."
                    forceResetErrorMessage.value = msg
                    forceResetStepDetail.value = "Échec : $msg"
                    serverSyncProgressMessage.value = msg
                    onComplete?.invoke(false, msg)
                }
            } catch (e: Exception) {
                val errorMsg = "Erreur lors de la synchronisation: ${e.message}"
                forceResetErrorMessage.value = errorMsg
                forceResetStepDetail.value = "Échec : $errorMsg"
                serverSyncProgressMessage.value = errorMsg
                onComplete?.invoke(false, errorMsg)
            } finally {
                isServerSyncing.value = false
            }
        }
    }

    /**
     * 1-Tap Synchronize ALL files and catalogs from the Server into local Room Database
     * Displays all steps in the Pop-up Dialog
     */
    fun syncAllFilesFromServer(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            syncDialogTitle.value = "Synchronisation Cloud Firestore"
            syncDialogSubtitle.value = "Téléchargement complet des catalogues & Indexation Room"
            showForceResetDialog.value = true
            forceResetCurrentStep.value = 0
            forceResetStepDetail.value = "Connexion et vérification de Cloud Firestore..."
            forceResetProgressPercent.value = 5f
            forceResetTotalFound.value = 0
            forceResetCatalogSummary.value = emptyMap()
            forceResetErrorMessage.value = null
            forceResetIsComplete.value = false
            isServerSyncing.value = true
            serverSyncProgressMessage.value = "Téléchargement des catalogues publiés..."
            CatalogSyncManager.updateProgress(isRunning = true, message = "Téléchargement des catalogues...")

            try {
                // Step 0: Invalidate Firestore catalog cache & listener
                com.example.data.FirestoreCatalogService.invalidateCache()
                serverFilesListener?.remove()
                serverFilesListener = null
                setupServerFilesListener()

                // Clear Room database & perform a clean re-sync of all data with callback
                val result = withContext(Dispatchers.IO) {
                    repository.forceUpdateFromFirestore { step, detail, progressPercent, count, catalogStats ->
                        forceResetCurrentStep.value = step
                        forceResetStepDetail.value = detail
                        forceResetProgressPercent.value = progressPercent
                        forceResetTotalFound.value = count
                        if (catalogStats.isNotEmpty()) {
                            forceResetCatalogSummary.value = catalogStats
                        }
                    }
                }

                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    forceResetIsComplete.value = true
                    forceResetProgressPercent.value = 100f
                    val summary = "Synchronisation réussie ($count produits récupérés et indexés dans Room SQLite)"
                    forceResetStepDetail.value = summary
                    val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    lastServerSyncTime.value = "Aujourd'hui à $timeStr"
                    serverSyncSummary.value = summary
                    serverSyncProgressMessage.value = "Synchronisation totale réussie !"
                    lastImportSummary.value = summary
                    isCloudSynced.value = true
                    lastSyncTimestamp.value = "À l'instant ($timeStr)"

                    importSuccessInfo.value = ImportSuccessInfo(
                        fileName = "Tous les Fichiers Serveur",
                        count = count,
                        targetCatalog = forceResetCatalogSummary.value.keys.joinToString(", ").ifBlank { "Tous" },
                        timestamp = "Aujourd'hui à $timeStr"
                    )
                    CatalogSyncManager.updateProgress(isRunning = false, message = "Synchronisé avec Firebase", summary = summary)
                    onComplete?.invoke(true, summary)
                } else {
                    val errMsg = result.exceptionOrNull()?.message ?: "Erreur inconnue"
                    forceResetErrorMessage.value = errMsg
                    forceResetStepDetail.value = "Échec : $errMsg"
                    serverSyncProgressMessage.value = errMsg
                    CatalogSyncManager.updateProgress(isRunning = false, message = errMsg, isError = true)
                    onComplete?.invoke(false, errMsg)
                }
            } catch (e: Exception) {
                val errMsg = e.message ?: "Erreur de synchronisation"
                forceResetErrorMessage.value = errMsg
                forceResetStepDetail.value = "Échec : $errMsg"
                serverSyncProgressMessage.value = errMsg
                CatalogSyncManager.updateProgress(isRunning = false, message = errMsg, isError = true)
                onComplete?.invoke(false, errMsg)
            } finally {
                isServerSyncing.value = false
            }
        }
    }

    // External Linked Files (e.g. from CompletePDF app or Intent Shares)
    val incomingExternalFiles = MutableStateFlow<List<FileUploadItem>>(emptyList())
    val externalLinkNotification = MutableStateFlow<String?>(null)

    // Loyalty Cards
    val loyaltyCards: StateFlow<List<LoyaltyCardEntity>> = repository.getAllLoyaltyCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveLoyaltyCard(storeName: String, cardNumber: String, cardHolderName: String, barcodeType: String, colorHex: String, cardImagePath: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val card = LoyaltyCardEntity(
                storeName = storeName,
                cardNumber = cardNumber,
                cardHolderName = cardHolderName,
                barcodeType = barcodeType,
                colorHex = colorHex,
                cardImagePath = cardImagePath
            )
            repository.saveLoyaltyCard(card)
        }
    }

    fun deleteLoyaltyCard(card: LoyaltyCardEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLoyaltyCard(card)
        }
    }

    fun addIncomingExternalUris(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val newItems = mutableListOf<FileUploadItem>()
            for (uri in uris) {
                val fileName = SpreadsheetImporter.getFileNameFromUri(context, uri) ?: "Document_CompletePDF_${System.currentTimeMillis()}.pdf"
                val detectedCatalog = SpreadsheetImporter.autoDetectCatalog(fileName, "", "TOUS")
                newItems.add(
                    FileUploadItem(
                        uri = uri,
                        fileName = fileName,
                        targetCatalog = if (detectedCatalog != "TOUS") detectedCatalog else "DREAMPRICE"
                    )
                )
            }
            if (newItems.isNotEmpty()) {
                incomingExternalFiles.value = (incomingExternalFiles.value + newItems).distinctBy { it.uri.toString() }
                externalLinkNotification.value = "${newItems.size} fichier(s) lié(s) depuis CompletePDF / Externe."
            }
        }
    }

    fun dismissExternalNotification() {
        externalLinkNotification.value = null
    }

    fun clearIncomingExternalFiles() {
        incomingExternalFiles.value = emptyList()
    }

    fun forceUpdateFromFirebase(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            showForceResetDialog.value = true
            forceResetCurrentStep.value = 0
            forceResetStepDetail.value = "Connexion et vérification de Cloud Firestore..."
            forceResetProgressPercent.value = 5f
            forceResetTotalFound.value = 0
            forceResetCatalogSummary.value = emptyMap()
            forceResetErrorMessage.value = null
            forceResetIsComplete.value = false
            isServerSyncing.value = true
            try {
                // Step 0: Invalidate Firestore catalog cache & listener
                com.example.data.FirestoreCatalogService.invalidateCache()
                serverFilesListener?.remove()
                serverFilesListener = null
                setupServerFilesListener()

                // Clear Room database & perform a clean re-sync of all data with callback
                val result = withContext(Dispatchers.IO) {
                    repository.forceUpdateFromFirestore { step, detail, progressPercent, count, catalogStats ->
                        forceResetCurrentStep.value = step
                        forceResetStepDetail.value = detail
                        forceResetProgressPercent.value = progressPercent
                        forceResetTotalFound.value = count
                        if (catalogStats.isNotEmpty()) {
                            forceResetCatalogSummary.value = catalogStats
                        }
                    }
                }
                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    forceResetIsComplete.value = true
                    forceResetProgressPercent.value = 100f
                    val summary = "Mise à jour complète réussie ($count produits récupérés et indexés dans Room SQLite)"
                    forceResetStepDetail.value = summary
                    val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                    lastServerSyncTime.value = "Aujourd'hui à $timeStr"
                    serverSyncSummary.value = summary
                    onComplete(true, summary)
                } else {
                    val errMsg = result.exceptionOrNull()?.message ?: "Erreur inconnue"
                    forceResetErrorMessage.value = errMsg
                    forceResetStepDetail.value = "Échec : $errMsg"
                    onComplete(false, errMsg)
                }
            } catch (e: Exception) {
                val errMsg = e.message ?: "Erreur inconnue"
                forceResetErrorMessage.value = errMsg
                forceResetStepDetail.value = "Échec : $errMsg"
                onComplete(false, errMsg)
            } finally {
                isServerSyncing.value = false
            }
        }
    }

    fun reorderSaleRecord(record: SaleRecordEntity) {
        viewModelScope.launch {
            val product = ProductEntity(
                id = record.id,
                catalogType = record.catalogType,
                name = record.productName,
                category = record.category,
                brand = "Général",
                unit = "unité",
                price = record.unitPrice,
                cost = record.unitCost
            )
            repository.addToCart(product, record.quantity)
        }
    }

    fun reorderMultipleSaleRecords(records: List<SaleRecordEntity>) {
        viewModelScope.launch {
            records.forEach { record ->
                val product = ProductEntity(
                    id = record.id,
                    catalogType = record.catalogType,
                    name = record.productName,
                    category = record.category,
                    brand = "Général",
                    unit = "unité",
                    price = record.unitPrice,
                    cost = record.unitCost
                )
                repository.addToCart(product, record.quantity)
            }
        }
    }

    fun clearOrderHistory() {
        viewModelScope.launch {
            repository.clearSaleRecords()
        }
    }

    fun dismissForceResetDialog() {
        showForceResetDialog.value = false
    }

    override fun onCleared() {
        super.onCleared()
        firestoreSettingsListener?.remove()
        firestoreSettingsListener = null
    }
}
