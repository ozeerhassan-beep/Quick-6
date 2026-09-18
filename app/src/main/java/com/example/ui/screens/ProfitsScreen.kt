package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.data.SaleRecordEntity
import com.example.ui.CatalogViewModel
import com.example.ui.theme.BlueLight
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.DreampriceColor
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IntermartColor
import com.example.ui.theme.OnBlueContainer
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SideBySideComparisonItem(
    val productNameKey: String,
    val displayName: String,
    val category: String,
    val brand: String,
    val unit: String,
    val dreamProduct: ProductEntity?,
    val interProduct: ProductEntity?
) {
    val dreamPrice = dreamProduct?.price
    val dreamCost = dreamProduct?.cost
    val dreamProfit = if (dreamProduct != null) dreamProduct.price - dreamProduct.cost else null
    val dreamMarginPct = if (dreamProduct != null && dreamProduct.price > 0)
        ((dreamProduct.price - dreamProduct.cost) / dreamProduct.price) * 100 else null

    val interPrice = interProduct?.price
    val interCost = interProduct?.cost
    val interProfit = if (interProduct != null) interProduct.price - interProduct.cost else null
    val interMarginPct = if (interProduct != null && interProduct.price > 0)
        ((interProduct.price - interProduct.cost) / interProduct.price) * 100 else null

    val priceDifference = if (dreamPrice != null && interPrice != null) dreamPrice - interPrice else null
    val marginPctDifference = if (dreamMarginPct != null && interMarginPct != null) dreamMarginPct - interMarginPct else null

    val betterMarginStore: String?
        get() {
            if (dreamMarginPct == null || interMarginPct == null) return null
            return when {
                dreamMarginPct > interMarginPct + 0.1 -> "DREAMPRICE"
                interMarginPct > dreamMarginPct + 0.1 -> "INTERMART"
                else -> "EQUAL"
            }
        }

    val lowerPriceStore: String?
        get() {
            if (dreamPrice == null || interPrice == null) return null
            return when {
                dreamPrice < interPrice -> "DREAMPRICE"
                interPrice < dreamPrice -> "INTERMART"
                else -> "EQUAL"
            }
        }
}

@Composable
fun ProfitsScreen(viewModel: CatalogViewModel) {
    val allProducts by viewModel.allProducts.collectAsState()
    val saleRecords by viewModel.saleRecords.collectAsState()

    var hasError by remember { mutableStateOf(false) }

    if (hasError || allProducts == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = SlateTextSecondary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Aucune donnée disponible",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Veuillez importer des catalogues ou vérifier les données pour afficher les bénéfices.",
                    fontSize = 13.sp,
                    color = SlateTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Purchased Items, 1: Side-by-Side Comparison, 2: Single Model Calculator
    var purchasedSearchQuery by remember { mutableStateOf("") }
    var purchasedSupermarketFilter by remember { mutableStateOf("Tous") } // "Tous", "DREAMPRICE", "INTERMART", "LOLO", "SUPER U"

    var selectedPricingModel by remember { mutableStateOf("DREAMPRICE") }
    var modelSearchQuery by remember { mutableStateOf("") }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tous") }
    var marginFilter by remember { mutableStateOf("Tous") }

    val comparisonList = remember(allProducts) {
        try {
            val groups = allProducts.groupBy { it.name.trim().lowercase() }
            groups.mapNotNull { (key, products) ->
                if (products.isEmpty()) return@mapNotNull null
                val dream = products.find { it.catalogType.equals("DREAMPRICE", ignoreCase = true) }
                val inter = products.find { it.catalogType.equals("INTERMART", ignoreCase = true) }
                val sample = dream ?: inter ?: products.firstOrNull() ?: return@mapNotNull null
                SideBySideComparisonItem(
                    productNameKey = key,
                    displayName = sample.name,
                    category = sample.category,
                    brand = sample.brand,
                    unit = sample.unit,
                    dreamProduct = dream,
                    interProduct = inter
                )
            }.sortedBy { it.displayName }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val categories = remember(comparisonList) {
        listOf("Tous") + comparisonList.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredComparisons = comparisonList.filter { item ->
        val matchesSearch = searchQuery.isBlank() ||
                item.displayName.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true) ||
                item.brand.contains(searchQuery, ignoreCase = true)

        val matchesCategory = selectedCategory == "Tous" || item.category.equals(selectedCategory, ignoreCase = true)

        val matchesMargin = when (marginFilter) {
            "Meilleure Marge Dreamprice" -> item.betterMarginStore == "DREAMPRICE"
            "Meilleure Marge Intermart" -> item.betterMarginStore == "INTERMART"
            else -> true
        }

        matchesSearch && matchesCategory && matchesMargin
    }

    val filteredSaleRecords = remember(saleRecords, purchasedSearchQuery, purchasedSupermarketFilter) {
        saleRecords.filter { record ->
            val matchesStore = when (purchasedSupermarketFilter) {
                "DREAMPRICE" -> record.catalogType.equals("DREAMPRICE", ignoreCase = true)
                "INTERMART" -> record.catalogType.equals("INTERMART", ignoreCase = true)
                "LOLO" -> record.catalogType.equals("LOLO", ignoreCase = true)
                "SUPER U" -> record.catalogType.equals("SUPER U", ignoreCase = true)
                else -> true
            }
            val q = purchasedSearchQuery.trim()
            val matchesQuery = q.isBlank() ||
                    record.productName.contains(q, ignoreCase = true) ||
                    record.category.contains(q, ignoreCase = true) ||
                    record.catalogType.contains(q, ignoreCase = true)

            matchesStore && matchesQuery
        }
    }

    val totalPurchasedQty = saleRecords.sumOf { it.quantity }
    val totalPurchasedRevenue = saleRecords.sumOf { it.totalPrice }
    val totalPurchasedProfit = saleRecords.sumOf { it.totalProfit }
    val realizedProfit = totalPurchasedProfit

    val dreamLeadCount = comparisonList.count { it.betterMarginStore == "DREAMPRICE" }
    val interLeadCount = comparisonList.count { it.betterMarginStore == "INTERMART" }

    val totalRevenue = allProducts.sumOf { it.price }
    val totalCost = allProducts.sumOf { it.cost }
    val totalMargin = totalRevenue - totalCost
    val avgMarginPct = if (totalRevenue > 0.0) (totalMargin / totalRevenue) * 100 else 0.0

    val dreamProducts = allProducts.filter { it.catalogType.equals("DREAMPRICE", ignoreCase = true) }
    val interProducts = allProducts.filter { it.catalogType.equals("INTERMART", ignoreCase = true) }

    val dreamProfit = dreamProducts.sumOf { it.price - it.cost }
    val interProfit = interProducts.sumOf { it.price - it.cost }

    val modelProducts = remember(allProducts, selectedPricingModel) {
        allProducts.filter { it.catalogType.equals(selectedPricingModel, ignoreCase = true) }
    }
    val modelRevenue = modelProducts.sumOf { it.price }
    val modelCost = modelProducts.sumOf { it.cost }
    val modelProfit = modelRevenue - modelCost
    val modelAvgMarginPct = if (modelRevenue > 0.0) (modelProfit / modelRevenue) * 100 else 0.0

    val filteredModelProducts = remember(modelProducts, modelSearchQuery) {
        val q = modelSearchQuery.trim()
        if (q.isBlank()) modelProducts else modelProducts.filter { p ->
            p.name.contains(q, ignoreCase = true) ||
            p.brand.contains(q, ignoreCase = true) ||
            p.category.contains(q, ignoreCase = true) ||
            p.id.toString().contains(q, ignoreCase = true) ||
            "#${p.id}".contains(q, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Screen Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = BlueLight
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Articles Achetés & Bénéfices",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Aperçu des articles achetés (tous supermarchés ensemble) & marges",
                        fontSize = 12.sp,
                        color = SlateTextSecondary
                    )
                }
            }
        }

        // Tab Navigation Switcher
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = BluePrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = BluePrimary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Text(
                                text = "Articles Achetés",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Text(
                                text = "Comparatif Côte à Côte",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = {
                            Text(
                                text = "Modèles de Prix",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
        }

        if (selectedTabIndex == 0) {
            // TAB 0: PURCHASED ITEMS VIEW (ALL SUPERMARKETS TOGETHER)

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Profit Réalisé Total",
                        value = "Rs ${String.format("%.2f", totalPurchasedProfit)}",
                        subtitle = "${saleRecords.size} achats (${totalPurchasedQty} articles)",
                        accentColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )

                    KpiCard(
                        title = "Achats / C.A. Cumulé",
                        value = "Rs ${String.format("%.2f", totalPurchasedRevenue)}",
                        subtitle = "Tous supermarchés réunis",
                        accentColor = BluePrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Supermarket Filter Chips Row
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Supermarchés (Tous Ensemble)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val dreamCount = saleRecords.count { it.catalogType.equals("DREAMPRICE", ignoreCase = true) }
                        val interCount = saleRecords.count { it.catalogType.equals("INTERMART", ignoreCase = true) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = purchasedSupermarketFilter == "Tous",
                                onClick = { purchasedSupermarketFilter = "Tous" },
                                label = { Text("Tous (${saleRecords.size})", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BluePrimary,
                                    selectedLabelColor = Color.White
                                )
                            )

                            FilterChip(
                                selected = purchasedSupermarketFilter == "DREAMPRICE",
                                onClick = { purchasedSupermarketFilter = "DREAMPRICE" },
                                label = { Text("[D] Dreamprice ($dreamCount)", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DreampriceColor,
                                    selectedLabelColor = Color.White
                                )
                            )

                            FilterChip(
                                selected = purchasedSupermarketFilter == "INTERMART",
                                onClick = { purchasedSupermarketFilter = "INTERMART" },
                                label = { Text("[I] Intermart ($interCount)", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = IntermartColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Search Bar for Purchased Items
            item {
                OutlinedTextField(
                    value = purchasedSearchQuery,
                    onValueChange = { purchasedSearchQuery = it },
                    placeholder = { Text("Rechercher un article acheté...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BluePrimary) },
                    trailingIcon = {
                        if (purchasedSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { purchasedSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = SlateBorder,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )
            }

            // List of Purchased Items
            if (filteredSaleRecords.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = SlateTextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aucun article acheté trouvé",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Effectuez un achat depuis l'onglet Panier pour suivre vos bénéfices.",
                                fontSize = 12.sp,
                                color = SlateTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredSaleRecords, key = { "${it.id}_${it.timestamp}_${it.productName}" }) { record ->
                    SaleRecordCard(record = record)
                }
            }

        } else if (selectedTabIndex == 1) {
            // SINGLE MODEL PRICING & PROFIT MARGIN CALCULATOR VIEW

            // Segmented Pricing Model Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Modèle de Tarification Actif",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val isDreamSelected = selectedPricingModel == "DREAMPRICE"
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedPricingModel = "DREAMPRICE" },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDreamSelected) DreampriceColor else DreampriceColor.copy(alpha = 0.08f),
                                border = if (isDreamSelected) null else BorderStroke(1.dp, DreampriceColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = if (isDreamSelected) Color.White else DreampriceColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Modèle Dreamprice",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDreamSelected) Color.White else DreampriceColor
                                    )
                                }
                            }

                            val isInterSelected = selectedPricingModel == "INTERMART"
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedPricingModel = "INTERMART" },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isInterSelected) IntermartColor else IntermartColor.copy(alpha = 0.08f),
                                border = if (isInterSelected) null else BorderStroke(1.dp, IntermartColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = if (isInterSelected) Color.White else IntermartColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Modèle Intermart",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isInterSelected) Color.White else IntermartColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Summary KPIs for Selected Pricing Model
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Profit Brute Total",
                        value = "Rs ${String.format("%.2f", modelProfit)}",
                        subtitle = "${modelProducts.size} articles ($selectedPricingModel)",
                        accentColor = if (selectedPricingModel == "DREAMPRICE") DreampriceColor else IntermartColor,
                        modifier = Modifier.weight(1f)
                    )

                    KpiCard(
                        title = "Marge Moyenne",
                        value = "${String.format("%.1f", modelAvgMarginPct)}%",
                        subtitle = "Sur C.A. de Rs ${String.format("%.0f", modelRevenue)}",
                        accentColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search Bar for Model Products
            item {
                OutlinedTextField(
                    value = modelSearchQuery,
                    onValueChange = { modelSearchQuery = it },
                    placeholder = { Text("Rechercher un produit, ID (#), marque...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BluePrimary) },
                    trailingIcon = {
                        if (modelSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { modelSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BluePrimary,
                        unfocusedBorderColor = SlateBorder,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )
            }

            // List of Single Model Product Cards
            if (filteredModelProducts.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Aucun produit dans le modèle $selectedPricingModel",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Essayez un autre mot-clé ou basculez de modèle.",
                                fontSize = 12.sp,
                                color = SlateTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredModelProducts, key = { "${it.catalogType}_${it.id}_${it.name}" }) { product ->
                    SingleModelProfitCard(product = product)
                }
            }

        } else if (selectedTabIndex == 1) {
            // SIDE-BY-SIDE COMPARISON DASHBOARD VIEW

            // Summary Lead Indicators
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DreampriceColor.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Avantage Marge",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DreampriceColor
                            )
                            Text(
                                text = "Dreamprice",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DreampriceColor
                            )
                            Text(
                                text = "$dreamLeadCount articles",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = IntermartColor.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Avantage Marge",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IntermartColor
                            )
                            Text(
                                text = "Intermart",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = IntermartColor
                            )
                            Text(
                                text = "$interLeadCount articles",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Search Bar & Filter Controls
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Rechercher un produit à comparer...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = SlateBorder,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = true
                    )

                    // Categories Scrollable Row
                    if (categories.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BlueLight,
                                        selectedLabelColor = OnBlueContainer
                                    )
                                )
                            }
                        }
                    }

                    // Margin Filter Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = marginFilter == "Tous",
                            onClick = { marginFilter = "Tous" },
                            label = { Text("Tous (${comparisonList.size})") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = marginFilter == "Meilleure Marge Dreamprice",
                            onClick = { marginFilter = "Meilleure Marge Dreamprice" },
                            label = { Text("Dreamprice ↑") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DreampriceColor.copy(alpha = 0.2f),
                                selectedLabelColor = DreampriceColor
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = marginFilter == "Meilleure Marge Intermart",
                            onClick = { marginFilter = "Meilleure Marge Intermart" },
                            label = { Text("Intermart ↑") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IntermartColor.copy(alpha = 0.2f),
                                selectedLabelColor = IntermartColor
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Results List
            if (filteredComparisons.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Aucun produit trouvé",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Essayez de modifier votre recherche ou vos filtres.",
                                fontSize = 12.sp,
                                color = SlateTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredComparisons, key = { it.productNameKey }) { item ->
                    SideBySideCard(item = item)
                }
            }

        } else {
            // OVERVIEW & SALES LOG VIEW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Profit Réalisé",
                        value = "Rs ${String.format("%.2f", realizedProfit)}",
                        subtitle = "Sur ${saleRecords.size} ventes",
                        accentColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )

                    KpiCard(
                        title = "Marge Potentielle",
                        value = "Rs ${String.format("%.2f", totalMargin)}",
                        subtitle = "${String.format("%.1f", avgMarginPct)}% marge moy.",
                        accentColor = BluePrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Répartition du Profit Potentiel",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val combinedProfit = (dreamProfit + interProfit).coerceAtLeast(1.0)
                        val dreamRatio = (dreamProfit / combinedProfit).toFloat()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { dreamRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp),
                                color = DreampriceColor,
                                trackColor = IntermartColor,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StoreProfitDetail(
                                name = "Dreamprice",
                                profit = "Rs ${String.format("%.2f", dreamProfit)}",
                                count = "${dreamProducts.size} prods",
                                color = DreampriceColor
                            )

                            StoreProfitDetail(
                                name = "Intermart",
                                profit = "Rs ${String.format("%.2f", interProfit)}",
                                count = "${interProducts.size} prods",
                                color = IntermartColor
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Historique des Ventes Enregistrées",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (saleRecords.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "Aucune vente enregistrée pour l'instant. Effectuez une commande depuis le panier.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            } else {
                items(saleRecords, key = { "${it.id}_${it.timestamp}_${it.productName}" }) { record ->
                    SaleRecordCard(record = record)
                }
            }
        }
    }
}

@Composable
private fun SideBySideCard(item: SideBySideComparisonItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Product Name & Meta Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.category} • ${item.brand} (${item.unit})",
                        fontSize = 12.sp,
                        color = SlateTextSecondary
                    )
                }

                // Strategy Badge
                when (item.betterMarginStore) {
                    "DREAMPRICE" -> {
                        Surface(
                            shape = CircleShape,
                            color = DreampriceColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Marge ↑ Dreamprice",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DreampriceColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    "INTERMART" -> {
                        Surface(
                            shape = CircleShape,
                            color = IntermartColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Marge ↑ Intermart",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IntermartColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    "EQUAL" -> {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldSuccess.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Marges Égales",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Side by Side Store Comparison Columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Dreamprice Column
                StoreComparisonColumn(
                    storeName = "Dreamprice",
                    color = DreampriceColor,
                    price = item.dreamPrice,
                    cost = item.dreamCost,
                    marginPct = item.dreamMarginPct,
                    isBetterMargin = item.betterMarginStore == "DREAMPRICE",
                    isLowerPrice = item.lowerPriceStore == "DREAMPRICE",
                    modifier = Modifier.weight(1f)
                )

                // Intermart Column
                StoreComparisonColumn(
                    storeName = "Intermart",
                    color = IntermartColor,
                    price = item.interPrice,
                    cost = item.interCost,
                    marginPct = item.interMarginPct,
                    isBetterMargin = item.betterMarginStore == "INTERMART",
                    isLowerPrice = item.lowerPriceStore == "INTERMART",
                    modifier = Modifier.weight(1f)
                )
            }

            // Margin & Price Delta Insight Banner
            if (item.dreamProduct != null && item.interProduct != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = BlueLight
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        val marginDiffText = if (item.marginPctDifference != null) {
                            val absDiff = String.format("%.1f", Math.abs(item.marginPctDifference))
                            if (item.marginPctDifference > 0)
                                "Dreamprice offre +$absDiff% de marge."
                            else if (item.marginPctDifference < 0)
                                "Intermart offre +$absDiff% de marge."
                            else
                                "Rentabilité identique."
                        } else ""

                        val priceDiffText = if (item.priceDifference != null) {
                            val absPrice = String.format("%.2f", Math.abs(item.priceDifference))
                            if (item.priceDifference < 0)
                                " Dreamprice est Rs $absPrice moins cher."
                            else if (item.priceDifference > 0)
                                " Intermart est Rs $absPrice moins cher."
                            else
                                " Prix de vente identique."
                        } else ""

                        Text(
                            text = "$marginDiffText$priceDiffText",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnBlueContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreComparisonColumn(
    storeName: String,
    color: Color,
    price: Double?,
    cost: Double?,
    marginPct: Double?,
    isBetterMargin: Boolean,
    isLowerPrice: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.06f),
        border = if (isBetterMargin) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = storeName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )

                if (isBetterMargin) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Meilleure Marge",
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (price == null || cost == null) {
                Text(
                    text = "Non disponible dans ce catalogue",
                    fontSize = 11.sp,
                    color = SlateTextSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Text(
                    text = "Prix : Rs ${String.format("%.2f", price)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                val profit = price - cost
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isBetterMargin) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Marge : ${String.format("%.1f", marginPct)}% (+Rs ${String.format("%.1f", profit)})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBetterMargin) color else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun StoreProfitDetail(
    name: String,
    profit: String,
    count: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = RoundedCornerShape(2.dp),
            color = color
        ) {}
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = profit, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = count, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun SaleRecordCard(record: SaleRecordEntity) {
    val isIntermart = record.catalogType.equals("INTERMART", ignoreCase = true)
    val isDreamprice = record.catalogType.equals("DREAMPRICE", ignoreCase = true)
    val storeInitial = when {
        isDreamprice -> "D"
        isIntermart -> "I"
        else -> record.catalogType.take(1).uppercase()
    }
    val storeName = when {
        isDreamprice -> "Dreamprice"
        isIntermart -> "Intermart"
        else -> record.catalogType
    }
    val storeColor = when {
        isDreamprice -> DreampriceColor
        isIntermart -> IntermartColor
        else -> BluePrimary
    }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = storeColor
                    ) {
                        Text(
                            text = storeInitial,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = storeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = storeName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = storeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = dateFormat.format(Date(record.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${record.productName} (x${record.quantity})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Prix unitaire: Rs ${String.format("%.2f", record.unitPrice)}",
                    fontSize = 11.sp,
                    color = SlateTextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Rs ${String.format("%.2f", record.totalPrice)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BluePrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldSuccess.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "+Rs ${String.format("%.2f", record.totalProfit)} profit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleModelProfitCard(product: ProductEntity) {
    val profit = product.price - product.cost
    val marginPct = if (product.price > 0) (profit / product.price) * 100 else 0.0
    val isDreamprice = product.catalogType.equals("DREAMPRICE", ignoreCase = true)
    val accentColor = if (isDreamprice) DreampriceColor else IntermartColor

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "#${product.id}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = product.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        if (product.brand.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${product.brand}",
                                fontSize = 12.sp,
                                color = SlateTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = product.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = product.catalogType,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pricing & Profit Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Prix Vente", fontSize = 11.sp, color = SlateTextSecondary)
                    Text(
                        text = "Rs ${String.format("%.2f", product.price)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text(text = "Prix Achat", fontSize = 11.sp, color = SlateTextSecondary)
                    Text(
                        text = "Rs ${String.format("%.2f", product.cost)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Marge Unitaire", fontSize = 11.sp, color = SlateTextSecondary)
                    Text(
                        text = "+Rs ${String.format("%.2f", profit)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Profit Margin Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Taux de Marge : ${String.format("%.1f", marginPct)}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (marginPct >= 25.0) EmeraldSuccess else if (marginPct >= 15.0) BluePrimary else Color(0xFFD97706)
                )

                Text(
                    text = if (marginPct >= 25.0) "Marge Élevée" else if (marginPct >= 15.0) "Marge Moyenne" else "Marge Faible",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { (marginPct / 50.0).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (marginPct >= 25.0) EmeraldSuccess else if (marginPct >= 15.0) BluePrimary else Color(0xFFD97706),
                trackColor = SlateBorder.copy(alpha = 0.4f)
            )
        }
    }
}
