package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CartItemEntity
import com.example.data.LoyaltyCardEntity
import com.example.ui.CatalogViewModel
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowBack
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.CartConsolidatedBarcodeDialog
import com.example.ui.components.CartQuickLabelScannerDialog
import com.example.ui.components.StoreRouteMapDialog
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.DreampriceColor
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IntermartColor
import com.example.ui.theme.SlateTextSecondary
import com.example.util.AppLanguage
import com.example.util.AppStrings

@Composable
fun CartScreen(viewModel: CatalogViewModel) {
    val context = LocalContext.current
    val currentLang by viewModel.appLanguage.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val activeCatalog by viewModel.activeCatalog.collectAsState()
    val availableCatalogs by viewModel.availableCatalogs.collectAsState()
    val loyaltyCards by viewModel.loyaltyCards.collectAsState()

    val isStoreRouteMapEnabled by viewModel.isStoreRouteMapEnabled.collectAsState(initial = true)
    val isBarcodeScannerEnabled by viewModel.isBarcodeScannerEnabled.collectAsState(initial = true)
    val isLoyaltyCardsEnabled by viewModel.isLoyaltyCardsEnabled.collectAsState(initial = true)

    val featuresFlow = remember(viewModel) {
        viewModel.preferenceManager?.isFeaturesEnabled ?: viewModel.isFeaturesEnabled
    }
    val isFeaturesEnabled by featuresFlow.collectAsState(initial = true)

    val totalItems = cartItems.sumOf { it.quantity }
    val totalPrice = cartItems.sumOf { it.unitPrice * it.quantity }
    val totalCost = cartItems.sumOf { it.unitCost * it.quantity }
    val totalProfit = totalPrice - totalCost
    val totalSavings = if (totalProfit > 0) totalProfit else (totalPrice * 0.08)

    val firestoreSyncStatus by viewModel.firestoreSyncStatus.collectAsState()
    val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsState()
    val isAutoSyncEnabled by viewModel.isFirestoreAutoSyncEnabled.collectAsState()

    var showBarcodeScannerModal by remember { mutableStateOf(false) }
    var showQuickLabelScannerModal by remember { mutableStateOf(false) }
    var showRouteMapModal by remember { mutableStateOf(false) }
    var showFidelityPopup by remember { mutableStateOf(false) }
    var showConsolidatedBarcodeModal by remember { mutableStateOf(false) }
    var showStoreSelectionModal by remember { mutableStateOf(false) }
    var showFeaturesModal by remember { mutableStateOf(false) }

    // Active loyalty card matching the active store or first available card
    val activeLoyaltyCard = remember(loyaltyCards, activeCatalog) {
        loyaltyCards.find { it.storeName.equals(activeCatalog, ignoreCase = true) }
            ?: loyaltyCards.firstOrNull()
    }

    // Group items by supermarket section / category
    val groupedItems = remember(cartItems) {
        cartItems.groupBy { item ->
            item.category.ifBlank { "Épicerie & Divers" }
        }.toList().sortedBy { it.first }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ================= TOP HEADER =================
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Title Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Mon Panier",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    // Route Map / Store Floorplan Button
                    if (isStoreRouteMapEnabled) {
                        IconButton(
                            onClick = { showRouteMapModal = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("cart_open_route_map_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = "Plan & Itinéraire Magasin",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    if (isFeaturesEnabled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { showFeaturesModal = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("cart_open_features_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Fonctionnalités",
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = BluePrimary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "$totalItems article(s)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Supermarket Selection Trigger & Active Fidelity Card Chip Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Supermarket Selection Trigger
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showStoreSelectionModal = true }
                            .testTag("cart_select_supermarket_trigger")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Supermarché : ${if (activeCatalog == "TOUS") "Tous" else activeCatalog}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = SlateTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Active Fidelity Card Chip / Badge
                    if (isLoyaltyCardsEnabled) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (activeLoyaltyCard != null) Color(0xFF10B981).copy(alpha = 0.12f) else BluePrimary.copy(alpha = 0.08f),
                            border = BorderStroke(
                                1.dp,
                                if (activeLoyaltyCard != null) Color(0xFF10B981).copy(alpha = 0.4f) else BluePrimary.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showFidelityPopup = true }
                                .testTag("cart_fidelity_card_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = "Carte de Fidélité",
                                    tint = if (activeLoyaltyCard != null) Color(0xFF10B981) else BluePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (activeLoyaltyCard != null) {
                                        "Fidélité : ${activeLoyaltyCard.storeName}"
                                    } else {
                                        "+ Fidélité"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeLoyaltyCard != null) Color(0xFF047857) else BluePrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Prominent 'Scanner une étiquette' Quick-Scan Action Button
                if (isBarcodeScannerEnabled) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showQuickLabelScannerModal = true }
                            .testTag("cart_scan_label_button"),
                        color = BluePrimary.copy(alpha = 0.08f),
                        border = BorderStroke(1.2.dp, BluePrimary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = BluePrimary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Scanner une étiquette",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Scanner une étiquette",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Rayons, boulangerie (Pain, Baguette), codes-barres...",
                                        fontSize = 11.sp,
                                        color = SlateTextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BluePrimary
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddShoppingCart,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Scanner",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cloud Firestore Synchronization Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (!isAutoSyncEnabled) Icons.Default.CloudOff else if (isFirestoreSyncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                        contentDescription = "Cloud Firestore",
                        tint = if (!isAutoSyncEnabled) Color(0xFFF59E0B) else if (isFirestoreSyncing) BluePrimary else EmeraldSuccess,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = firestoreSyncStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.forceSyncCartToFirestore()
                        Toast.makeText(context, "Synchronisation avec Firestore lancée...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Synchroniser avec Firestore",
                        tint = BluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // ================= MAIN CART BODY =================
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Votre panier est vide",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Explorez les catalogues et ajoutez des articles.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Smart Shopping Route Map Promo Card
                if (isStoreRouteMapEnabled) {
                    item(key = "route_map_banner") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRouteMapModal = true }
                                .testTag("cart_smart_route_map_banner"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Route,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Itinéraire de Courses Optimisé",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "-30% temps",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF047857),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Plan 2D interactif & tracé sans détour dans les rayons.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.DirectionsWalk,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Grouped Cart Items by Section/Category
                groupedItems.forEach { (category, itemsInCategory) ->
                    val categoryTotal = itemsInCategory.sumOf { it.unitPrice * it.quantity }
                    val categoryQuantity = itemsInCategory.sumOf { it.quantity }

                    // Category Section Header
                    item(key = "section_header_$category") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = category,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = BluePrimary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "$categoryQuantity",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Rs ${String.format("%.2f", categoryTotal)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Items within this section
                    items(itemsInCategory, key = { "${it.id}_${it.catalogType}_${it.productId}_${it.productName}" }) { item ->
                        CartItemRow(
                            item = item,
                            onIncrement = { viewModel.updateCartQuantity(item, item.quantity + 1) },
                            onDecrement = { viewModel.updateCartQuantity(item, item.quantity - 1) },
                            onRemove = { viewModel.removeFromCart(item.id) }
                        )
                    }
                }
            }
        }

        // ================= DYNAMIC ACTIONS BAR (Cart Items < 10) =================
        // Rendered dynamically when cartItems.size in 1..9 to open the consolidated barcode modal
        if (isBarcodeScannerEnabled && cartItems.size in 1..9) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = BluePrimary.copy(alpha = 0.08f),
                border = BorderStroke(1.2.dp, BluePrimary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BluePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Passage caisse rapide (${cartItems.size}/9)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Code-barres consolidé pour tout le panier",
                                fontSize = 10.sp,
                                color = SlateTextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = { showConsolidatedBarcodeModal = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("generate_consolidated_cart_barcode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Générer Code-Barres Unique",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ================= BOTTOM SUMMARY FOOTER =================
        // Sticky bottom checkout bar featuring total item count, total price, savings summary, and 'Valider le panier'
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Summary Metrics Row: Items count, Savings summary, Total price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$totalItems article(s)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = SlateTextSecondary
                        )
                        if (totalSavings > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldSuccess.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Économies : Rs ${String.format("%.2f", totalSavings)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                }
                            }
                        }
                    }

                    // Total Price
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "Total : ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = SlateTextSecondary,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                        Text(
                            text = "Rs ${String.format("%.2f", totalPrice)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BluePrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Primary 'Valider le panier' Button
                Button(
                    onClick = {
                        viewModel.checkoutCart()
                        Toast.makeText(context, "Panier validé et enregistré !", Toast.LENGTH_LONG).show()
                    },
                    enabled = cartItems.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("validate_cart_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Valider le panier",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // ================= MODALS & DIALOGS =================

    // Supermarket Selection Modal Dialog
    if (showStoreSelectionModal) {
        val storeList = remember(availableCatalogs) {
            val others = listOf("DREAMPRICE", "INTERMART", "SUPER_U", "WINNERS", "WINNER'S", "WAY", "LOLO", "KING_SAVERS", "GSR")
            val combined = (availableCatalogs + others).map { it.uppercase().trim() }.filter { it != "TOUS" }.distinct().sorted()
            listOf("TOUS") + combined
        }

        AlertDialog(
            onDismissRequest = { showStoreSelectionModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = BluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choisir le Supermarché", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sélectionnez le magasin où vous faites vos courses :",
                        fontSize = 13.sp,
                        color = SlateTextSecondary
                    )
                    storeList.forEach { store ->
                        val isSelected = activeCatalog.equals(store, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) BluePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) BorderStroke(1.5.dp, BluePrimary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setActiveCatalog(store)
                                    showStoreSelectionModal = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = if (isSelected) BluePrimary else SlateTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (store == "TOUS") "Tous les magasins" else store,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BluePrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStoreSelectionModal = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Advanced Features Dialog in Cart
    if (showFeaturesModal) {
        Dialog(
            onDismissRequest = { showFeaturesModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                FeaturesScreen(
                    viewModel = viewModel,
                    onBack = { showFeaturesModal = false }
                )
            }
        }
    }

    // Smart Store Route Map Dialog
    if (showRouteMapModal) {
        StoreRouteMapDialog(
            viewModel = viewModel,
            onDismiss = { showRouteMapModal = false }
        )
    }

    // Barcode Scanner Dialog in Cart
    if (showBarcodeScannerModal) {
        BarcodeScannerDialog(
            viewModel = viewModel,
            onDismiss = { showBarcodeScannerModal = false }
        )
    }

    // Quick Continuous Label Scanner Dialog in Cart (Bakery, Shelves, SKUs)
    if (showQuickLabelScannerModal) {
        CartQuickLabelScannerDialog(
            viewModel = viewModel,
            onDismissRequest = { showQuickLabelScannerModal = false }
        )
    }

    // Consolidated Cart Barcode Dialog (POS checkout modal)
    if (showConsolidatedBarcodeModal) {
        CartConsolidatedBarcodeDialog(
            cartItems = cartItems,
            onDismissRequest = { showConsolidatedBarcodeModal = false }
        )
    }

    // Fidelity / Loyalty Cards Popup Dialog
    if (showFidelityPopup) {
        Dialog(
            onDismissRequest = { showFidelityPopup = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showFidelityPopup = false }) {
                            Text("Fermer", fontWeight = FontWeight.Bold)
                        }
                    }
                    LoyaltyCardsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItemEntity,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    val storeColor = when (item.catalogType.uppercase()) {
        "INTERMART" -> IntermartColor
        "DREAMPRICE" -> DreampriceColor
        "SUPER U" -> Color(0xFFE11D48)
        "WINNER'S" -> Color(0xFFF97316)
        else -> BluePrimary
    }
    val itemTotalPrice = item.unitPrice * item.quantity

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = storeColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = item.catalogType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = storeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.productName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Prix : Rs ${String.format("%.2f", item.unitPrice)}${if (item.unit.isNotBlank()) " / " + item.unit else ""}",
                    fontSize = 12.sp,
                    color = SlateTextSecondary
                )

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total : ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateTextSecondary
                    )
                    Text(
                        text = "Rs ${String.format("%.2f", itemTotalPrice)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }
            }

            // Quantity Controls (- / + / Delete)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Diminuer",
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "${item.quantity}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Augmenter",
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = if (isBold) 15.sp else 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}
