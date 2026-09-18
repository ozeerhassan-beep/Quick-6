package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Palette
import com.example.util.AppLanguage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.CatalogViewModel
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateTextSecondary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.IconButton

data class AppFunction(
    val id: String,
    val title: String,
    val description: String,
    val isEnabled: Boolean
)

@Composable
fun PersonaliseScreen(
    functionsList: List<AppFunction>,
    lang: AppLanguage,
    onToggleChanged: (AppFunction, Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (lang) {
                        AppLanguage.FRENCH -> "Configuration des Fonctions"
                        AppLanguage.ENGLISH -> "Function Configuration"
                        AppLanguage.CREOLE -> "Konfigirasion bann Fonksion"
                        else -> "Configuration des Fonctions"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Réduire" else "Développer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    functionsList.forEach { functionItem ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = functionItem.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = functionItem.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SlateTextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Switch(
                                    checked = functionItem.isEnabled,
                                    onCheckedChange = { isChecked ->
                                        onToggleChanged(functionItem, isChecked)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalizeScreen(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Safely observe feature toggle state with fallback flow
    val featuresFlow = remember(viewModel) {
        viewModel.preferenceManager?.isFeaturesEnabled ?: viewModel.isFeaturesEnabled
    }
    val isFeaturesEnabled by featuresFlow.collectAsState(initial = true)

    // Collect states from ViewModel for all functions
    val isPriceCompareEnabled by viewModel.isPriceCompareEnabled.collectAsState(initial = true)
    val isSmartCartEnabled by viewModel.isSmartCartEnabled.collectAsState(initial = true)
    val isSavingsAnalyticsEnabled by viewModel.isSavingsAnalyticsEnabled.collectAsState(initial = true)
    val isLoyaltyCardsEnabled by viewModel.isLoyaltyCardsEnabled.collectAsState(initial = true)
    val isAiShoppingListEnabled by viewModel.isAiShoppingListEnabled.collectAsState(initial = true)
    val isStoreRouteMapEnabled by viewModel.isStoreRouteMapEnabled.collectAsState(initial = true)
    val isBarcodeScannerEnabled by viewModel.isBarcodeScannerEnabled.collectAsState(initial = true)
    val isVoiceSearchEnabled by viewModel.isVoiceSearchEnabled.collectAsState(initial = true)
    val isPriceAlertsEnabled by viewModel.isPriceAlertsEnabled.collectAsState(initial = true)

    // Scanner / System state flows
    val isFirestoreSyncEnabled by viewModel.isFirestoreAutoSyncEnabled.collectAsState()
    val isAutoAddToCartOnScan by viewModel.autoAddToCartOnScan.collectAsState()
    val isContinuousScanMode by viewModel.continuousScanMode.collectAsState()
    val isVibrateOnScan by viewModel.vibrateOnScan.collectAsState()

    // Dynamic state-backed list of all toggleable app functions
    val appFunctions = remember(
        isPriceCompareEnabled,
        isSmartCartEnabled,
        isSavingsAnalyticsEnabled,
        isLoyaltyCardsEnabled,
        isAiShoppingListEnabled,
        isStoreRouteMapEnabled,
        isBarcodeScannerEnabled,
        isVoiceSearchEnabled,
        isPriceAlertsEnabled,
        isFirestoreSyncEnabled,
        isAutoAddToCartOnScan,
        isContinuousScanMode,
        isVibrateOnScan,
        isFeaturesEnabled
    ) {
        listOf(
            AppFunction("price_compare", "Comparateur de Prix", "Comparer les prix des articles entre Carrefour, Intermarché, SuperU et Lidl", isPriceCompareEnabled),
            AppFunction("smart_cart", "Panier Intelligent", "Gérer les articles du panier, quantités et répartition par magasin", isSmartCartEnabled),
            AppFunction("savings_analytics", "Analyses & Économies (Profits)", "Suivre l'historique des économies, bénéfices et graphiques de budget", isSavingsAnalyticsEnabled),
            AppFunction("loyalty_cards", "Cartes de Fidélité", "Stocker les cartes de fidélité numériques des supermarchés avec codes-barres", isLoyaltyCardsEnabled),
            AppFunction("ai_shopping_list", "Optimiseur de Liste IA", "Optimisation intelligente du panier au meilleur prix multi-magasins", isAiShoppingListEnabled),
            AppFunction("store_route_map", "Plan & Itinéraire Magasin (TSP)", "Plan de magasin 2D interactif et itinéraire d'achat optimisé", isStoreRouteMapEnabled),
            AppFunction("barcode_scanner", "Scanner Code-barres ML Kit", "Scanner instantané par caméra pour recherche de prix produits", isBarcodeScannerEnabled),
            AppFunction("voice_search", "Recherche Vocale Sémantique", "Rechercher des produits du catalogue par commandes vocales", isVoiceSearchEnabled),
            AppFunction("price_alerts", "Alertes & Notifications de Prix", "Surveiller les baisses de prix et recevoir des notifications", isPriceAlertsEnabled),
            AppFunction("firestore_sync", "Auto-Sync Firestore", "Synchronisation automatique avec Firebase", isFirestoreSyncEnabled),
            AppFunction("auto_add_to_cart", "Auto-ajout au panier", "Ajout automatique au panier après scan", isAutoAddToCartOnScan),
            AppFunction("continuous_scan", "Scan continu", "Scanner plusieurs articles en continu", isContinuousScanMode),
            AppFunction("vibrate_on_scan", "Vibration au scan", "Vibration lors de la validation du scan", isVibrateOnScan),
            AppFunction("features_screen", "Écran Fonctionnalités", "Afficher ou masquer l'onglet Fonctionnalités dans la barre de navigation", isFeaturesEnabled)
        )
    }

    val scrollState = rememberScrollState()
    val currentLang by viewModel.appLanguage.collectAsState(initial = AppLanguage.FRENCH)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        PersonaliseScreen(
            functionsList = appFunctions,
            lang = currentLang,
            onToggleChanged = { item, newState ->
                when (item.id) {
                    "price_compare" -> viewModel.setPriceCompareEnabled(newState)
                    "smart_cart" -> viewModel.setSmartCartEnabled(newState)
                    "savings_analytics" -> viewModel.setSavingsAnalyticsEnabled(newState)
                    "loyalty_cards" -> viewModel.setLoyaltyCardsEnabled(newState)
                    "ai_shopping_list" -> viewModel.setAiShoppingListEnabled(newState)
                    "store_route_map" -> viewModel.setStoreRouteMapEnabled(newState)
                    "barcode_scanner" -> viewModel.setBarcodeScannerEnabled(newState)
                    "voice_search" -> viewModel.setVoiceSearchEnabled(newState)
                    "price_alerts" -> viewModel.setPriceAlertsEnabled(newState)
                    "firestore_sync" -> viewModel.setFirestoreAutoSyncEnabled(newState)
                    "auto_add_to_cart" -> viewModel.setAutoAddToCartOnScan(newState)
                    "continuous_scan" -> viewModel.setContinuousScanMode(newState)
                    "vibrate_on_scan" -> viewModel.setVibrateOnScan(newState)
                    "features_screen" -> viewModel.setFeaturesEnabled(newState)
                }

                if (item.id != "firestore_sync") {
                    Toast.makeText(context, "${item.title} ${if (newState) "activé" else "désactivé"}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun PersonalizeDialog(
    viewModel: CatalogViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        },
        title = { Text("Personnalisation") },
        text = {
            PersonalizeScreen(
                viewModel = viewModel,
                modifier = Modifier.heightIn(max = 450.dp)
            )
        }
    )
}