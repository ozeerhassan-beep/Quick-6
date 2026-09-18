package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import android.widget.Toast
import com.example.data.PriceAlertEntity
import com.example.data.ProductEntity
import com.example.ui.CatalogViewModel
import com.example.ui.SubscriptionTier
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.DreampriceColor
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IntermartColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductDetailsDialog(
    product: ProductEntity,
    allProducts: List<ProductEntity>,
    viewModel: CatalogViewModel,
    isWishlisted: Boolean,
    isInCompare: Boolean,
    onDismiss: () -> Unit,
    onToggleWishlist: () -> Unit,
    onToggleCompare: () -> Unit,
    onAddToCart: () -> Unit
) {
    val priceHistory by viewModel.getPriceHistoryForProduct(product).collectAsState(initial = emptyList())
    val existingAlert by viewModel.getPriceAlertForProduct(product.id, product.catalogType).collectAsState(initial = null)
    val userTier by viewModel.userTier.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val context = LocalContext.current

    val normUnit = product.unit.trim().lowercase()
    val sameNameProducts = remember(allProducts, product) {
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

    val margin = product.price - product.cost
    val marginPct = if (product.price > 0) (margin / product.price) * 100 else 0.0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BluePrimary.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Détails du Produit",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val hideId = userTier == SubscriptionTier.FREE && userRole != "ADMIN"
                                Text(
                                    text = if (hideId) product.catalogType else "#${product.id} • ${product.catalogType}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Product Title & Badges
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
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
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = product.brand,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = product.name,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    IconButton(
                                        onClick = onToggleWishlist,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favori",
                                            tint = if (isWishlisted) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(14.dp))

                                // Current Price & Margin Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Prix de vente actuel",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = "Rs ${String.format("%.2f", product.price)}",
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Black,
                                                color = BluePrimary
                                            )
                                            Text(
                                                text = " / ${product.unit}",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                modifier = Modifier.padding(bottom = 3.dp)
                                            )
                                        }
                                    }

                                    if (product.cost > 0) {
                                        val hasMarginAccess = userTier != SubscriptionTier.FREE || userRole == "ADMIN"
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Marge bénéficiaire",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            if (hasMarginAccess) {
                                                Text(
                                                    text = "+Rs ${String.format("%.2f", margin)} (${String.format("%.1f", marginPct)}%)",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldSuccess
                                                )
                                            } else {
                                                Text(
                                                    text = "🔒 Réservé PRO",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Price Trend Chart (Leveraging Room storage) - Locked for non-ULTRA / non-ADMIN
                    item {
                        if (userTier == SubscriptionTier.ULTRA || userRole == "ADMIN") {
                            PriceTrendChart(
                                history = priceHistory,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = "Verrouillé",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Analyses Avancées de Prix",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Déverrouillez le graphique d'évolution et l'historique des tendances de prix avec l'abonnement ULTRA.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Price Alert Tracking Card (Cloud Firestore + Room Database)
                    item {
                        PriceAlertProductSection(
                            product = product,
                            alert = existingAlert,
                            onSetAlert = { targetPrice ->
                                viewModel.setPriceAlert(product, targetPrice)
                                Toast.makeText(
                                    context,
                                    "Alerte activée pour ${product.name} (Seuil : Rs ${String.format("%.2f", targetPrice)})",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onRemoveAlert = {
                                if (existingAlert != null) {
                                    viewModel.removePriceAlert(existingAlert!!)
                                    Toast.makeText(context, "Alerte de prix désactivée", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    // Supermarket Comparison (If available in multiple supermarkets)
                    if (sameNameProducts.size > 1) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Store,
                                            contentDescription = null,
                                            tint = BluePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Comparatif en Supermarché",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    val lowestStorePrice = sameNameProducts.minOfOrNull { it.price } ?: 0.0

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        sameNameProducts.forEach { p ->
                                            val isCheapest = p.price == lowestStorePrice && sameNameProducts.size > 1
                                            val col = when (p.catalogType.uppercase()) {
                                                "DREAMPRICE" -> DreampriceColor
                                                "INTERMART" -> IntermartColor
                                                "SUPER U" -> Color(0xFFE53935)
                                                "JUMBO" -> Color(0xFFFF9800)
                                                "WINNERS" -> Color(0xFF4CAF50)
                                                else -> BluePrimary
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = col.copy(alpha = 0.08f),
                                                border = BorderStroke(if (isCheapest) 1.5.dp else 1.dp, if (isCheapest) EmeraldSuccess else col.copy(alpha = 0.3f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = p.catalogType,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = col
                                                        )
                                                        if (isCheapest) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = EmeraldSuccess
                                                            ) {
                                                                Text(
                                                                    text = "MOINS CHER",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color.White,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Rs ${String.format("%.2f", p.price)}",
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Room Historical Records Table
                    if (priceHistory.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = BluePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Relevés Stockés dans Room",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    priceHistory.takeLast(6).reversed().forEach { record ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = BluePrimary.copy(alpha = 0.12f),
                                                    modifier = Modifier.size(8.dp)
                                                ) {}
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = record.recordedDate,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Text(
                                                text = "Rs ${String.format("%.2f", record.price)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BluePrimary
                                            )
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Buttons
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onToggleCompare,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isInCompare) BluePrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, if (isInCompare) BluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isInCompare) "Comparé" else "Comparer",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = onAddToCart,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ajouter au Panier",
                                fontSize = 13.sp,
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
fun PriceAlertProductSection(
    product: ProductEntity,
    alert: PriceAlertEntity?,
    onSetAlert: (Double) -> Unit,
    onRemoveAlert: () -> Unit
) {
    val isAlertActive = alert != null
    var isEditingThreshold by remember { mutableStateOf(false) }
    var inputThreshold by remember(product.price, alert?.targetPrice) {
        mutableStateOf(
            if (alert != null) String.format("%.2f", alert.targetPrice)
            else String.format("%.2f", (product.price * 0.90).coerceAtLeast(1.0))
        )
    }

    val isTriggered = alert?.let { it.isTriggered || (it.currentPrice > 0 && it.currentPrice <= it.targetPrice) } ?: false

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTriggered) EmeraldSuccess.copy(alpha = 0.08f)
            else if (isAlertActive) Color(0xFFEA580C).copy(alpha = 0.05f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isTriggered) EmeraldSuccess.copy(alpha = 0.6f)
            else if (isAlertActive) Color(0xFFEA580C).copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section Title & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isAlertActive) Color(0xFFEA580C).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isAlertActive) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = if (isAlertActive) Color(0xFFEA580C) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Alerte Baisse de Prix",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAlertActive) "Surveillance active (Cloud Firestore)" else "Soyez notifié dès que le prix baisse",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                if (isAlertActive) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isTriggered) EmeraldSuccess else Color(0xFFEA580C).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isTriggered) "🎉 Baisse atteinte !" else "Actif",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTriggered) Color.White else Color(0xFFEA580C),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!isAlertActive && !isEditingThreshold) {
                // Not active state: Quick enable prompt
                Text(
                    text = "Définissez un prix cible. Lorsque ${product.catalogType} baisse le prix sous votre seuil, l'application vous envoie une notification push instantanée !",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { isEditingThreshold = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Activer une alerte pour ce produit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Active or Configuration State
                if (isAlertActive && !isEditingThreshold) {
                    // Summary of configured alert
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Seuil configuré :",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "<= Rs ${String.format("%.2f", alert!!.targetPrice)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFEA580C)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { isEditingThreshold = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Ajuster", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = onRemoveAlert,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Supprimer", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    // Interactive threshold editor
                    Column {
                        Text(
                            text = "Prix actuel : Rs ${String.format("%.2f", product.price)}. Entrez votre seuil d'alerte :",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = inputThreshold,
                            onValueChange = { inputThreshold = it },
                            label = { Text("Seuil d'alerte (Rs)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Percentage shortcut chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(5, 10, 15, 20).forEach { pct ->
                                val priceAtDiscount = product.price * (1.0 - pct / 100.0)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEA580C).copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            inputThreshold = String.format("%.2f", priceAtDiscount)
                                        }
                                ) {
                                    Text(
                                        text = "-$pct%\nRs ${String.format("%.0f", priceAtDiscount)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEA580C),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val parsedVal = inputThreshold.replace(",", ".").toDoubleOrNull()
                        val isValid = parsedVal != null && parsedVal > 0.0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isAlertActive) {
                                OutlinedButton(
                                    onClick = { isEditingThreshold = false },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Annuler", fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    if (isValid) {
                                        onSetAlert(parsedVal!!)
                                        isEditingThreshold = false
                                    }
                                },
                                enabled = isValid,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAlertActive) "Mettre à jour" else "Enregistrer l'alerte",
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
}

