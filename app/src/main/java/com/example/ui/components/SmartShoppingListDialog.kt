package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ProductEntity
import com.example.data.WishlistItemEntity
import com.example.ui.CatalogViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.DreampriceColor
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IntermartColor
import com.example.ui.theme.SuperUColor
import com.example.ui.theme.WinnersColor

data class SmartItemComparison(
    val wishlistItem: WishlistItemEntity,
    val quantity: Int,
    val originalCatalog: String,
    val originalPrice: Double,
    val lowestPrice: Double,
    val lowestStore: String,
    val highestPrice: Double,
    val bestProduct: ProductEntity?,
    val storeOffers: List<ProductEntity>
)

@Composable
fun SmartShoppingListDialog(
    viewModel: CatalogViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val wishlistItems by viewModel.wishlistItems.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState(initial = emptyList())
    val quantities = remember { mutableStateMapOf<Int, Int>() }
    var quickSearchQuery by remember { mutableStateOf("") }
    var showStoreBreakdown by remember { mutableStateOf(false) }

    // Initialize quantities default to 1 if not set
    wishlistItems.forEach { item ->
        if (!quantities.containsKey(item.id)) {
            quantities[item.id] = 1
        }
    }

    // Smart Optimization Engine: Find lowest available price per item across all catalogs
    val smartComparisons = remember(wishlistItems, allProducts, quantities.toMap()) {
        wishlistItems.map { wishItem ->
            val qty = quantities[wishItem.id] ?: 1
            // Find all matching products in the catalog by exact or close name
            val matchingProducts = allProducts.filter { product ->
                product.name.trim().equals(wishItem.productName.trim(), ignoreCase = true) ||
                (product.name.length > 4 && wishItem.productName.contains(product.name.take(6), ignoreCase = true))
            }

            if (matchingProducts.isNotEmpty()) {
                val lowestProduct = matchingProducts.minByOrNull { it.price } ?: matchingProducts.first()
                val highestProduct = matchingProducts.maxByOrNull { it.price } ?: matchingProducts.first()
                SmartItemComparison(
                    wishlistItem = wishItem,
                    quantity = qty,
                    originalCatalog = wishItem.catalogType,
                    originalPrice = wishItem.unitPrice,
                    lowestPrice = lowestProduct.price,
                    lowestStore = lowestProduct.catalogType,
                    highestPrice = highestProduct.price,
                    bestProduct = lowestProduct,
                    storeOffers = matchingProducts.sortedBy { it.price }
                )
            } else {
                SmartItemComparison(
                    wishlistItem = wishItem,
                    quantity = qty,
                    originalCatalog = wishItem.catalogType,
                    originalPrice = wishItem.unitPrice,
                    lowestPrice = wishItem.unitPrice,
                    lowestStore = wishItem.catalogType,
                    highestPrice = wishItem.unitPrice,
                    bestProduct = null,
                    storeOffers = emptyList()
                )
            }
        }
    }

    // Financial Metrics based on lowest available store prices
    val totalLowestEstimatedCost = smartComparisons.sumOf { it.lowestPrice * it.quantity }
    val totalStandardEstimatedCost = smartComparisons.sumOf { it.highestPrice * it.quantity }
    val estimatedSavings = (totalStandardEstimatedCost - totalLowestEstimatedCost).coerceAtLeast(0.0)
    val savingsPercent = if (totalStandardEstimatedCost > 0) (estimatedSavings / totalStandardEstimatedCost * 100).toInt() else 0

    // Supermarket-by-Supermarket comparison for the whole basket
    val catalogTypes = listOf("DREAMPRICE", "SUPER_U", "WINNERS", "INTERMART", "WAY", "LOLO", "KING_SAVERS", "GSR").sorted()
    val storeTotals = remember(smartComparisons, allProducts) {
        catalogTypes.mapNotNull { cat ->
            var storeTotal = 0.0
            var availableItemsCount = 0
            smartComparisons.forEach { comp ->
                val offer = comp.storeOffers.firstOrNull { it.catalogType.equals(cat, ignoreCase = true) }
                if (offer != null) {
                    storeTotal += offer.price * comp.quantity
                    availableItemsCount++
                } else if (comp.originalCatalog.equals(cat, ignoreCase = true)) {
                    storeTotal += comp.originalPrice * comp.quantity
                    availableItemsCount++
                }
            }
            if (availableItemsCount > 0) {
                Triple(cat, storeTotal, availableItemsCount)
            } else null
        }.sortedBy { it.second }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldSuccess.copy(alpha = 0.12f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Liste de Courses Intelligente",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Optimisation au meilleur prix garanti",
                                fontSize = 11.sp,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (wishlistItems.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val shareText = buildString {
                                        append("🛒 MA LISTE DE COURSES INTELLIGENTE (Kwic-Kart)\n\n")
                                        smartComparisons.forEach { comp ->
                                            append("• ${comp.wishlistItem.productName} (x${comp.quantity})\n")
                                            append("  Meilleur prix : Rs ${String.format("%.2f", comp.lowestPrice)} chez ${comp.lowestStore}\n")
                                        }
                                        append("\n💰 Coût total estimé aux meilleurs prix : Rs ${String.format("%.2f", totalLowestEstimatedCost)}\n")
                                        if (estimatedSavings > 0) {
                                            append("🎉 Économies estimées : Rs ${String.format("%.2f", estimatedSavings)} (-$savingsPercent%)\n")
                                        }
                                    }
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Partager ma liste optimisée")
                                    context.startActivity(shareIntent)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Partager",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lowest Available Store Price Summary Banner
                if (wishlistItems.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Coût Estimé au Meilleur Prix :",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "Rs ${String.format("%.2f", totalLowestEstimatedCost)}",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black,
                                            color = EmeraldSuccess
                                        )
                                        if (estimatedSavings > 0) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "au lieu de Rs ${String.format("%.2f", totalStandardEstimatedCost)}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                            )
                                        }
                                    }
                                }

                                if (estimatedSavings > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = EmeraldSuccess
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "ÉCONOMIE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                            Text(
                                                text = "-$savingsPercent% (Rs ${String.format("%.0f", estimatedSavings)})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Store breakdown toggle
                            if (storeTotals.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = EmeraldSuccess.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showStoreBreakdown = !showStoreBreakdown },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val bestSingleStore = storeTotals.first()
                                    Text(
                                        text = "Magasin unique le moins cher : ${bestSingleStore.first} (Rs ${String.format("%.2f", bestSingleStore.second)})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = if (showStoreBreakdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                AnimatedVisibility(visible = showStoreBreakdown) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        storeTotals.forEach { (store, total, count) ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "• $store ($count/${wishlistItems.size} articles) :",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                                )
                                                Text(
                                                    text = "Rs ${String.format("%.2f", total)}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (store == storeTotals.first().first) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Quick Add to List Search Input
                OutlinedTextField(
                    value = quickSearchQuery,
                    onValueChange = { quickSearchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ajouter rapidement un produit du catalogue...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (quickSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { quickSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Quick Add Filter Results
                if (quickSearchQuery.isNotBlank()) {
                    val matchingQuickProducts = allProducts.filter {
                        it.name.contains(quickSearchQuery, ignoreCase = true) ||
                        it.category.contains(quickSearchQuery, ignoreCase = true)
                    }.take(4)

                    if (matchingQuickProducts.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Suggestions du catalogue :", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                                matchingQuickProducts.forEach { p ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.toggleWishlist(p)
                                                quickSearchQuery = ""
                                                Toast.makeText(context, "${p.name} ajouté à la liste", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(p.name, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Rs ${String.format("%.2f", p.price)} (${p.catalogType})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.Add, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Shopping List Items
                if (wishlistItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Votre Liste Intelligente est vide",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ajoutez des produits depuis le catalogue ou via la recherche ci-dessus pour comparer instantanément les prix des supermarchés !",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(smartComparisons, key = { "${it.wishlistItem.id}_${it.wishlistItem.productName}" }) { comp ->
                            SmartShoppingItemCard(
                                comparison = comp,
                                onQuantityChange = { newQty ->
                                    if (newQty > 0) {
                                        quantities[comp.wishlistItem.id] = newQty
                                    }
                                },
                                onRemove = {
                                    val p = allProducts.find { it.id == comp.wishlistItem.productId } ?: ProductEntity(
                                        id = comp.wishlistItem.productId,
                                        catalogType = comp.wishlistItem.catalogType,
                                        name = comp.wishlistItem.productName,
                                        category = comp.wishlistItem.category,
                                        brand = "",
                                        unit = "",
                                        price = comp.wishlistItem.unitPrice,
                                        cost = 0.0
                                    )
                                    viewModel.toggleWishlist(p)
                                },
                                onAddToCart = {
                                    val targetProd = comp.bestProduct ?: allProducts.find { it.id == comp.wishlistItem.productId }
                                    if (targetProd != null) {
                                        viewModel.addToCart(targetProd, comp.quantity)
                                        Toast.makeText(context, "${targetProd.name} (x${comp.quantity}) ajouté au panier !", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }

                // Bottom Actions: Batch Add to Cart
                if (wishlistItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                wishlistItems.forEach { item ->
                                    val p = allProducts.find { it.id == item.productId } ?: ProductEntity(
                                        id = item.productId,
                                        catalogType = item.catalogType,
                                        name = item.productName,
                                        category = item.category,
                                        brand = "",
                                        unit = "",
                                        price = item.unitPrice,
                                        cost = 0.0
                                    )
                                    viewModel.toggleWishlist(p)
                                }
                                Toast.makeText(context, "Liste vidée", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Vider la liste", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                smartComparisons.forEach { comp ->
                                    val targetProd = comp.bestProduct ?: allProducts.find { it.id == comp.wishlistItem.productId }
                                    if (targetProd != null) {
                                        viewModel.addToCart(targetProd, comp.quantity)
                                    }
                                }
                                Toast.makeText(context, "${wishlistItems.size} articles ajoutés au panier au meilleur prix !", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            modifier = Modifier.weight(2f)
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Transférer vers le Panier (Rs ${String.format("%.0f", totalLowestEstimatedCost)})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartShoppingItemCard(
    comparison: SmartItemComparison,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
    onAddToCart: () -> Unit
) {
    val storeBadgeColor = when (comparison.lowestStore.uppercase()) {
        "DREAMPRICE" -> DreampriceColor
        "SUPER_U", "SUPERU" -> SuperUColor
        "WINNERS" -> WinnersColor
        "INTERMART" -> IntermartColor
        else -> BluePrimary
    }

    val itemTotal = comparison.lowestPrice * comparison.quantity
    val isCheaperThanOriginal = comparison.lowestPrice < comparison.originalPrice

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comparison.wishlistItem.productName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = comparison.wishlistItem.category,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Best Store & Price Comparison Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(storeBadgeColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = storeBadgeColor
                    ) {
                        Text(
                            text = "Meilleur : ${comparison.lowestStore}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (isCheaperThanOriginal) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "-${String.format("%.0f", (comparison.originalPrice - comparison.lowestPrice))} Rs",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldSuccess
                        )
                    }
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Rs ${String.format("%.2f", comparison.lowestPrice)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = storeBadgeColor
                    )
                    Text(
                        text = " /u",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity Control & Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity Selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                if (comparison.quantity > 1) onQuantityChange(comparison.quantity - 1)
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Remove, contentDescription = "Diminuer", modifier = Modifier.size(14.dp))
                        }
                    }

                    Text(
                        text = "${comparison.quantity}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onQuantityChange(comparison.quantity + 1) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Augmenter", modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Subtotal and Direct Add to Cart
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total : Rs ${String.format("%.2f", itemTotal)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onAddToCart,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = "Ajouter",
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
