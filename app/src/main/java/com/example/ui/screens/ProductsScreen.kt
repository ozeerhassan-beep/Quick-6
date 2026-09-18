package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.ArrowUpward
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import kotlin.math.sin
import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.CatalogLayoutDensity
import com.example.ui.components.PullToRefreshContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.example.data.ProductEntity
import com.example.ui.CatalogViewModel
import com.example.ui.ProductSortOption
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.CloudSyncStatusCard
import com.example.ui.components.PriceAlertsDialog
import com.example.ui.components.SmartShoppingListDialog
import com.example.ui.components.VoiceSemanticSearchModal
import com.example.ui.components.WishlistDialog
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.DreampriceColor
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IntermartColor
import com.example.ui.theme.LoloColor
import com.example.ui.theme.SuperUColor
import com.example.ui.theme.WinnersColor
import com.example.ui.theme.SlateBorder
import com.example.util.AppLanguage
import com.example.util.AppStrings

@Composable
fun ProductsScreen(viewModel: CatalogViewModel) {
    val context = LocalContext.current
    val currentLang by viewModel.appLanguage.collectAsState()
    val activeCatalog by viewModel.activeCatalog.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val userRole by viewModel.userRole.collectAsState()

    val currentProducts by viewModel.currentProducts.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val availableCatalogs by viewModel.availableCatalogs.collectAsState()
    val comparedIds by viewModel.comparedProductIds.collectAsState()
    val wishlistItems by viewModel.wishlistItems.collectAsState()
    val wishlistedIds by viewModel.wishlistedProductIds.collectAsState()
    val priceAlerts by viewModel.priceAlerts.collectAsState()
    val triggeredAlerts by viewModel.triggeredPriceAlerts.collectAsState()
    val layoutDensity by viewModel.layoutDensity.collectAsState()
    val semanticResult by viewModel.semanticSearchResult.collectAsState()
    val productSortOption by viewModel.productSortOption.collectAsState()
    val onlyPromotionsFilter by viewModel.onlyPromotionsFilter.collectAsState()
    val isServerSyncing by viewModel.isServerSyncing.collectAsState()
    val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsState()
    val supermarketItemCounts by viewModel.supermarketItemCounts.collectAsState()
    val isVoiceSearchEnabled by viewModel.isVoiceSearchEnabled.collectAsState(initial = true)
    val isBarcodeScannerEnabled by viewModel.isBarcodeScannerEnabled.collectAsState(initial = true)
    val isAiShoppingListEnabled by viewModel.isAiShoppingListEnabled.collectAsState(initial = true)
    val isPriceAlertsEnabled by viewModel.isPriceAlertsEnabled.collectAsState(initial = true)

    val categories = remember(allProducts) {
        val cats = allProducts
            .map { it.category }
            .distinct()
            .sorted()
        listOf("Tous") + cats
    }

    var showSortDropdown by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showVoiceSearchModal by remember { mutableStateOf(false) }
    var showBarcodeScannerModal by remember { mutableStateOf(false) }
    var showWishlistModal by remember { mutableStateOf(false) }
    var showSmartShoppingListModal by remember { mutableStateOf(false) }
    var showSyncDashboardModal by remember { mutableStateOf(false) }
    var showPriceAlertsModal by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var isAlertBannerDismissed by remember { mutableStateOf(false) }
    var selectedProducts by remember { mutableStateOf(setOf<ProductEntity>()) }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    var animatingItemKey by remember { mutableStateOf<Int?>(null) }
    val animProgressX = remember { Animatable(0f) }
    val animProgressY = remember { Animatable(0f) }
    val animAlpha = remember { Animatable(1f) }
    val jumpProgress = remember { Animatable(0f) }

    LaunchedEffect(animatingItemKey) {
        if (animatingItemKey != null) {
            jumpProgress.snapTo(0f)
            jumpProgress.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            jumpProgress.stop()
        }
    }

    val isHeaderExpanded by remember {
        derivedStateOf {
            if (layoutDensity.isGrid) {
                gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset < 100
            } else {
                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 100
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search and Filter Bar
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_search_bar"),
                    placeholder = { Text(AppStrings.searchProductsPlaceholder(currentLang), fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BluePrimary) },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.clearAllSearchFilters() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Effacer",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (isPriceAlertsEnabled) {
                                IconButton(
                                    onClick = { showPriceAlertsModal = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("price_alerts_header_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (priceAlerts.isNotEmpty()) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                            contentDescription = "Alertes Baisse de Prix (Firestore)",
                                            tint = if (triggeredAlerts.isNotEmpty()) EmeraldSuccess else if (priceAlerts.isNotEmpty()) Color(0xFFEA580C) else BluePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        if (priceAlerts.isNotEmpty()) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (triggeredAlerts.isNotEmpty()) EmeraldSuccess else Color(0xFFEA580C),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .align(Alignment.TopEnd)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = if (triggeredAlerts.isNotEmpty()) "!" else "${priceAlerts.size}",
                                                        color = Color.White,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (isVoiceSearchEnabled) {
                                IconButton(
                                    onClick = { showVoiceSearchModal = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("mic_search_button")
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (semanticResult != null) BluePrimary.copy(alpha = 0.15f) else Color.Transparent,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Recherche vocale & IA Gemini",
                                                tint = if (semanticResult != null) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (isBarcodeScannerEnabled) {
                                // Barcode Reader in Catalogue Search Bar
                                IconButton(
                                    onClick = { showBarcodeScannerModal = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("search_barcode_scanner_button")
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = BluePrimary.copy(alpha = 0.12f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner,
                                                contentDescription = "Scanner un code-barres pour rechercher dans le catalogue",
                                                tint = BluePrimary,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Active AI Semantic Search Banner
                AnimatedVisibility(visible = semanticResult != null) {
                    if (semanticResult != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BluePrimary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Recherche IA : \"${semanticResult!!.query}\"",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BluePrimary
                                        )
                                        Text(
                                            text = semanticResult!!.explanation,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.clearSemanticSearch() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Annuler la recherche sémantique",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Triggered Price Drops Banner (Firestore Price Alert System)
                AnimatedVisibility(visible = isPriceAlertsEnabled && triggeredAlerts.isNotEmpty() && !isAlertBannerDismissed) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldSuccess.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable { showPriceAlertsModal = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = EmeraldSuccess,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "🚨 Baisse de prix détectée (${triggeredAlerts.size} article${if (triggeredAlerts.size > 1) "s" else ""}) !",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldSuccess
                                    )
                                    Text(
                                        text = "Des produits surveillés sont passés sous votre seuil. Appuyez pour voir.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { showPriceAlertsModal = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Voir",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldSuccess
                                    )
                                }

                                IconButton(
                                    onClick = { isAlertBannerDismissed = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Masquer le bandeau",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = isHeaderExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Supermarket Brand Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Enseignes & Magasins :",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    if (activeCatalog != "TOUS" || selectedCategory != "Tous" || onlyPromotionsFilter || productSortOption != ProductSortOption.DEFAULT) {
                        Text(
                            text = "Réinitialiser",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary,
                            modifier = Modifier.clickable { viewModel.clearAllSearchFilters() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    val supermarketOptions = listOf(
                        "TOUS" to ("Tous les magasins" to BluePrimary),
                        "DREAMPRICE" to ("Dreamprice" to DreampriceColor),
                        "SUPER_U" to ("Super U" to SuperUColor),
                        "WINNERS" to ("Winners" to WinnersColor),
                        "INTERMART" to ("Intermart" to IntermartColor),
                        "WAY" to ("Way Supermarket" to Color(0xFF0D9488)),
                        "LOLO" to ("Lolo" to LoloColor)
                    )

                    items(supermarketOptions, key = { it.first }) { (catKey, pair) ->
                        val (label, brandColor) = pair
                        val isSelected = activeCatalog.equals(catKey, ignoreCase = true)
                        val count = when (catKey) {
                            "TOUS" -> supermarketItemCounts["TOUS"] ?: allProducts.size
                            else -> supermarketItemCounts[catKey] ?: 0
                        }

                        Surface(
                            onClick = { viewModel.setActiveCatalog(catKey) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) brandColor else brandColor.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, if (isSelected) brandColor else brandColor.copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("filter_brand_$catKey")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else brandColor
                                    )
                                }
                                Text(
                                    text = "($count articles)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else brandColor.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sorting & Quick Action Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sort Dropdown Box
                    Box {
                        Surface(
                            onClick = { showSortDropdown = true },
                            shape = RoundedCornerShape(14.dp),
                            color = if (productSortOption != ProductSortOption.DEFAULT) BluePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (productSortOption != ProductSortOption.DEFAULT) BluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier.testTag("sort_dropdown_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Trier",
                                    tint = if (productSortOption != ProductSortOption.DEFAULT) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (productSortOption) {
                                        ProductSortOption.PRICE_ASC -> "Prix: Moins cher"
                                        ProductSortOption.PRICE_DESC -> "Prix: Plus cher"
                                        ProductSortOption.PROMOTIONS -> "🔥 Promotions"
                                        ProductSortOption.NAME_ASC -> "Nom A-Z"
                                        ProductSortOption.NAME_DESC -> "Nom Z-A"
                                        ProductSortOption.DEFAULT -> "Trier par"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (productSortOption != ProductSortOption.DEFAULT) FontWeight.Bold else FontWeight.Medium,
                                    color = if (productSortOption != ProductSortOption.DEFAULT) BluePrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = if (productSortOption != ProductSortOption.DEFAULT) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSortDropdown,
                            onDismissRequest = { showSortDropdown = false }
                        ) {
                            ProductSortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.displayName,
                                            fontWeight = if (option == productSortOption) FontWeight.Bold else FontWeight.Normal,
                                            color = if (option == productSortOption) BluePrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        if (option == productSortOption) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = BluePrimary)
                                        }
                                    },
                                    onClick = {
                                        viewModel.setProductSortOption(option)
                                        showSortDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Promotion Only Filter Chip
                    Surface(
                        onClick = { viewModel.setOnlyPromotionsFilter(!onlyPromotionsFilter) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (onlyPromotionsFilter) Color(0xFFEA580C) else Color(0xFFEA580C).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, if (onlyPromotionsFilter) Color(0xFFEA580C) else Color(0xFFEA580C).copy(alpha = 0.35f)),
                        modifier = Modifier.testTag("filter_promotions_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = if (onlyPromotionsFilter) Color.White else Color(0xFFEA580C),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Rabais & Promos",
                                fontSize = 12.sp,
                                fontWeight = if (onlyPromotionsFilter) FontWeight.Bold else FontWeight.Medium,
                                color = if (onlyPromotionsFilter) Color.White else Color(0xFFEA580C)
                            )
                        }
                    }

                    if (isAiShoppingListEnabled) {
                        // Smart Shopping List Shortcut Chip (with lowest price optimizer)
                        Surface(
                            onClick = { showSmartShoppingListModal = true },
                            shape = RoundedCornerShape(14.dp),
                            color = EmeraldSuccess.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("smart_list_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Liste Intelligente",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Categories Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = category == selectedCategory
                        Surface(
                            onClick = { viewModel.setSelectedCategory(category) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) BluePrimary else MaterialTheme.colorScheme.surface,
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Text(
                                text = category,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                }
                }
            }

            if (showBulkDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showBulkDeleteConfirmDialog = false },
                    title = {
                        Text(
                            text = "Confirmer la suppression en masse",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Text(
                            text = "Êtes-vous sûr de vouloir supprimer définitivement les ${selectedProducts.size} article(s) sélectionnés du serveur Firestore et de votre base locale ? Cette action est irréversible.",
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showBulkDeleteConfirmDialog = false
                                val itemsToDelete = selectedProducts.toList()
                                selectedProducts = emptySet()
                                viewModel.deleteProductsBulkFromServer(itemsToDelete) { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Oui, Supprimer", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showBulkDeleteConfirmDialog = false }
                        ) {
                            Text("Annuler")
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Admin Bulk Deletion Controls
            if (userRole == "ADMIN" && currentProducts.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedProducts.size} article(s) sélectionné(s)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (selectedProducts.isNotEmpty()) {
                                Button(
                                    onClick = { showBulkDeleteConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Supprimer du Serveur",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    selectedProducts = currentProducts.toSet()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.5f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Tout Sélectionner (${currentProducts.size})", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    selectedProducts = emptySet()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Tout Désélectionner", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Products List with Pull-to-Refresh Gesture for Room ↔ Firebase Sync
            PullToRefreshContainer(
                isRefreshing = isServerSyncing || isFirestoreSyncing,
                onRefresh = {
                    viewModel.syncAllFilesFromServer { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (currentProducts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = AppStrings.emptyCatalogTitle(currentLang),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = AppStrings.emptyCatalogSubtitle(currentLang),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (layoutDensity.isGrid) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(currentProducts, key = { "${it.catalogType}_${it.id}_${it.name}_${it.barcode}" }) { product ->
                            val isInCompare = product.id in comparedIds
                            val isWishlisted = product.id in wishlistedIds
                            ProductItemCard(
                                product = product,
                                allProducts = allProducts,
                                userRole = userRole,
                                isInCompare = isInCompare,
                                isWishlisted = isWishlisted,
                                isSelected = selectedProducts.contains(product),
                                onSelectedChange = { isChecked ->
                                    selectedProducts = if (isChecked) {
                                        selectedProducts + product
                                    } else {
                                        selectedProducts - product
                                    }
                                },
                                onToggleWishlist = {
                                    viewModel.toggleWishlist(product)
                                    val msg = if (isWishlisted)
                                        "${product.name} retiré de la liste d'envies"
                                    else
                                        "${product.name} sauvegardé dans la liste d'envies"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onToggleCompare = {
                                    viewModel.toggleCompareProduct(product)
                                    val msg = if (isInCompare)
                                        "${product.name} retiré du comparateur"
                                    else
                                        "${product.name} ajouté au comparateur"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onAddToCart = {
                                    viewModel.addToCart(product)
                                    Toast.makeText(context, "${product.name} ajouté au panier", Toast.LENGTH_SHORT).show()
                                    coroutineScope.launch {
                                        animatingItemKey = product.id
                                        animProgressX.snapTo(-100f)
                                        animProgressY.snapTo(-800f)
                                        animAlpha.snapTo(1f)
                                        
                                        val jobX = launch {
                                            animProgressX.animateTo(
                                                targetValue = 0f,
                                                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                                            )
                                        }
                                        val jobY = launch {
                                            animProgressY.animateTo(
                                                targetValue = 0f,
                                                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                                            )
                                        }
                                        val jobAlpha = launch {
                                            animAlpha.animateTo(
                                                targetValue = 0f,
                                                animationSpec = tween(durationMillis = 200, delayMillis = 450)
                                            )
                                        }
                                        joinAll(jobX, jobY, jobAlpha)
                                        animatingItemKey = null
                                    }
                                },
                                onEdit = { editingProduct = product },
                                onDelete = { viewModel.deleteProduct(product.id, product.catalogType) },
                                isGrid = true
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(if (layoutDensity == CatalogLayoutDensity.COMPACT) 6.dp else 12.dp)
                    ) {
                        items(currentProducts, key = { "${it.catalogType}_${it.id}_${it.name}_${it.barcode}" }) { product ->
                            val isInCompare = product.id in comparedIds
                            val isWishlisted = product.id in wishlistedIds
                            ProductItemCard(
                                product = product,
                                allProducts = allProducts,
                                userRole = userRole,
                                isInCompare = isInCompare,
                                isWishlisted = isWishlisted,
                                isSelected = selectedProducts.contains(product),
                                onSelectedChange = { isChecked ->
                                    selectedProducts = if (isChecked) {
                                        selectedProducts + product
                                    } else {
                                        selectedProducts - product
                                    }
                                },
                                onToggleWishlist = {
                                    viewModel.toggleWishlist(product)
                                    val msg = if (isWishlisted)
                                        "${product.name} retiré de la liste d'envies"
                                    else
                                        "${product.name} sauvegardé dans la liste d'envies"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onToggleCompare = {
                                    viewModel.toggleCompareProduct(product)
                                    val msg = if (isInCompare)
                                        "${product.name} retiré du comparateur"
                                    else
                                        "${product.name} ajouté au comparateur"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                onAddToCart = {
                                    viewModel.addToCart(product)
                                    Toast.makeText(context, "${product.name} ajouté au panier", Toast.LENGTH_SHORT).show()
                                    coroutineScope.launch {
                                        animatingItemKey = product.id
                                        animProgressX.snapTo(-300f)
                                        animProgressY.snapTo(-800f)
                                        animAlpha.snapTo(1f)
                                        
                                        val jobX = launch {
                                            animProgressX.animateTo(
                                                targetValue = 0f,
                                                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                                            )
                                        }
                                        val jobY = launch {
                                            animProgressY.animateTo(
                                                targetValue = 0f,
                                                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                                            )
                                        }
                                        val jobAlpha = launch {
                                            animAlpha.animateTo(
                                                targetValue = 0f,
                                                animationSpec = tween(durationMillis = 200, delayMillis = 450)
                                            )
                                        }
                                        joinAll(jobX, jobY, jobAlpha)
                                        animatingItemKey = null
                                    }
                                 },
                                 onEdit = { editingProduct = product },
                                 onDelete = { viewModel.deleteProduct(product.id, product.catalogType) },
                                 isGrid = false
                            )
                        }
                    }
                }
            }
        }

        val showGoToTop by remember {
            derivedStateOf {
                if (layoutDensity.isGrid) {
                    gridState.firstVisibleItemIndex > 0
                } else {
                    listState.firstVisibleItemIndex > 0
                }
            }
        }

        if (showGoToTop) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        if (layoutDensity.isGrid) {
                            gridState.animateScrollToItem(0)
                        } else {
                            listState.animateScrollToItem(0)
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
                    .testTag("goto_top_button")
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Retourner en haut")
            }
        }

        // Floating Action Button for Admin
        if (userRole == "ADMIN") {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BluePrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter Produit")
            }
        }

        // Add Product Dialog
        if (showAddDialog) {
            AddProductDialog(
                activeCatalog = activeCatalog,
                onDismiss = { showAddDialog = false },
                onAdd = { name, cat, brand, unit, price, cost ->
                    viewModel.addProduct(name, cat, brand, unit, price, cost, activeCatalog)
                    showAddDialog = false
                    Toast.makeText(context, "Produit ajouté à $activeCatalog", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Edit Product Dialog
        editingProduct?.let { prod ->
            EditProductDialog(
                product = prod,
                onDismiss = { editingProduct = null },
                onSave = { updated ->
                    viewModel.updateProduct(updated)
                    editingProduct = null
                    Toast.makeText(context, "Produit modifié avec succès", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Voice & Semantic AI Search Modal
        if (showVoiceSearchModal) {
            VoiceSemanticSearchModal(
                viewModel = viewModel,
                onDismiss = { showVoiceSearchModal = false }
            )
        }

        // Barcode Camera Scanner Dialog
        if (showBarcodeScannerModal) {
            BarcodeScannerDialog(
                viewModel = viewModel,
                onDismiss = { showBarcodeScannerModal = false },
                onBarcodeScanned = { scannedBarcode ->
                    viewModel.setSearchQuery(scannedBarcode)
                }
            )
        }

        // Wishlist Saved Products Dialog
        if (showWishlistModal) {
            WishlistDialog(
                viewModel = viewModel,
                onDismiss = { showWishlistModal = false }
            )
        }

        // Price Drop Alerts Dialog (Firestore + Room)
        if (showPriceAlertsModal) {
            PriceAlertsDialog(
                viewModel = viewModel,
                onDismiss = { showPriceAlertsModal = false },
                onNavigateToProduct = { productId ->
                    // Optionally scroll to or highlight product
                }
            )
        }

        // Smart Shopping List Modal Dialog (Best Store Prices & Optimization)
        if (showSmartShoppingListModal) {
            SmartShoppingListDialog(
                viewModel = viewModel,
                onDismiss = { showSmartShoppingListModal = false }
            )
        }

        // Sync Dashboard Screen Modal Dialog (WorkManager Status & Trigger)
        if (showSyncDashboardModal) {
            Dialog(
                onDismissRequest = { showSyncDashboardModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SyncDashboardScreen(
                        viewModel = viewModel,
                        onBack = { showSyncDashboardModal = false }
                    )
                }
            }
        }

        if (animatingItemKey != null) {
            // Parabolic arc jump effect using sine wave interpolation
            val parabolicArc = -sin(jumpProgress.value * Math.PI).toFloat() * 35f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Surface(
                    shape = CircleShape,
                    color = BluePrimary,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(36.dp)
                        .offset {
                            IntOffset(
                                animProgressX.value.roundToInt(),
                                (animProgressY.value + parabolicArc).roundToInt()
                            )
                        }
                        .alpha(animAlpha.value)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductItemCard(
    product: ProductEntity,
    allProducts: List<ProductEntity>,
    userRole: String,
    isInCompare: Boolean,
    isWishlisted: Boolean,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onToggleWishlist: () -> Unit,
    onToggleCompare: () -> Unit,
    onAddToCart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isGrid: Boolean = false
) {
    val margin = product.price - product.cost
    val marginPct = if (product.price > 0) (margin / product.price) * 100 else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (userRole == "ADMIN") {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = onSelectedChange,
                                colors = CheckboxDefaults.colors(checkedColor = BluePrimary),
                                modifier = Modifier.size(24.dp).padding(end = 4.dp).testTag("product_checkbox_${product.catalogType}_${product.id}")
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BluePrimary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = product.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (product.brand.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${product.brand}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = product.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false),
                            softWrap = true
                        )
                        if (product.unit.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BluePrimary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = product.unit,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Supermarkets badges & respective prices under each item
                    val normName = product.name.trim().lowercase()
                    val normUnit = product.unit.trim().lowercase()

                    val matchingCompetitors = remember(allProducts, product) {
                        allProducts
                            .filter { other ->
                                val sameName = other.name.trim().equals(product.name.trim(), ignoreCase = true)
                                val sameUnit = if (normUnit.isNotBlank() && other.unit.trim().isNotBlank()) {
                                    other.unit.trim().equals(normUnit, ignoreCase = true)
                                } else {
                                    true
                                }
                                sameName && sameUnit && other.price > 0
                            }
                            .groupBy { it.catalogType.uppercase() }
                            .mapValues { (_, list) ->
                                list.find { it.id == product.id } ?: list.first()
                            }
                            .values
                            .toList()
                    }

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        matchingCompetitors.forEach { p ->
                            if (p.price > 0) {
                                val color = when (p.catalogType.uppercase()) {
                                    "DREAMPRICE" -> DreampriceColor
                                    "INTERMART" -> IntermartColor
                                    "SUPER U", "SUPER_U" -> SuperUColor
                                    "JUMBO" -> Color(0xFFFF9800)
                                    "WINNERS" -> WinnersColor
                                    else -> BluePrimary
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = color.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = color
                                        ) {
                                            Text(
                                                text = p.catalogType.take(1).uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "${p.catalogType}: Rs ${String.format("%.2f", p.price)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color,
                                            softWrap = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (userRole == "ADMIN") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = BluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            if (isGrid) {
                // Compact Grid Layout
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Rs ${String.format("%.2f", product.price)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BluePrimary
                        )
                        if (product.unit.isNotBlank()) {
                            Text(
                                text = "/ ${product.unit}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleWishlist,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("wishlist_toggle_${product.id}")
                        ) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isWishlisted) "Retirer" else "Sauvegarder",
                                tint = if (isWishlisted) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleCompare,
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = if (isInCompare) BluePrimary.copy(alpha = 0.12f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isInCompare) BluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = if (isInCompare) Icons.Default.Check else Icons.Default.CompareArrows,
                                contentDescription = "Compare",
                                tint = if (isInCompare) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Button(
                            onClick = onAddToCart,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = "Add",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ajouter",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            } else {
                // Responsive List Layout with FlowRow to prevent wrapping text vertically
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "Rs ${String.format("%.2f", product.price)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BluePrimary
                            )
                            if (product.unit.isNotBlank()) {
                                Text(
                                    text = " / ${product.unit}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleWishlist,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("wishlist_toggle_${product.id}")
                        ) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isWishlisted) "Retirer" else "Sauvegarder",
                                tint = if (isWishlisted) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = onToggleCompare,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isInCompare) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                containerColor = if (isInCompare) BluePrimary.copy(alpha = 0.12f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (isInCompare) BluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isInCompare) Icons.Default.Check else Icons.Default.CompareArrows,
                                contentDescription = "Compare",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isInCompare) "Comparé" else "Comparer",
                                fontSize = 12.sp,
                                fontWeight = if (isInCompare) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = onAddToCart,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = "Add",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ajouter",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddProductDialog(
    activeCatalog: String,
    onDismiss: () -> Unit,
    onAdd: (name: String, cat: String, brand: String, unit: String, price: Double, cost: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Alimentaire") }
    var brand by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("PCS") }
    var priceText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau Produit ($activeCatalog)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du Produit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Catégorie") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Marque") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unité") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Prix Vente (Rs)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Prix Achat / Coût (Rs)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
                    val cost = costText.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onAdd(name, category, brand, unit, price, cost)
                    }
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun EditProductDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var category by remember { mutableStateOf(product.category) }
    var brand by remember { mutableStateOf(product.brand) }
    var unit by remember { mutableStateOf(product.unit) }
    var priceText by remember { mutableStateOf(if (product.price > 0) product.price.toString() else "") }
    var costText by remember { mutableStateOf(if (product.cost > 0) product.cost.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier Produit #${product.id}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du Produit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Catégorie") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Marque") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unité") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Prix Vente (Rs)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Prix Achat / Coût (Rs)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPrice = priceText.replace(',', '.').trim().toDoubleOrNull() ?: product.price
                    val parsedCost = costText.replace(',', '.').trim().toDoubleOrNull() ?: product.cost
                    if (name.isNotBlank()) {
                        onSave(
                            product.copy(
                                name = name.trim(),
                                category = category.trim(),
                                brand = brand.trim(),
                                unit = unit.trim(),
                                price = parsedPrice,
                                cost = parsedCost
                            )
                        )
                    }
                }
            ) {
                Text("Enregistrer les modifications")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
