package com.example.ui.components

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PriceAlertEntity
import com.example.data.ProductEntity
import com.example.ui.CatalogViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.DreampriceColor
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IntermartColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceAlertsDialog(
    viewModel: CatalogViewModel,
    onDismiss: () -> Unit,
    onNavigateToProduct: ((Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val allAlerts by viewModel.priceAlerts.collectAsState()
    val triggeredAlerts by viewModel.triggeredPriceAlerts.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val isSyncing by viewModel.isAlertsFirestoreSyncing.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(if (triggeredAlerts.isNotEmpty()) 1 else 0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingAlert by remember { mutableStateOf<PriceAlertEntity?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top App Bar / Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEA580C).copy(alpha = 0.12f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFEA580C),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Alertes Baisse de Prix",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (triggeredAlerts.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = EmeraldSuccess
                                    ) {
                                        Text(
                                            text = "${triggeredAlerts.size} baisse(s) !",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = if (isSyncing) BluePrimary else EmeraldSuccess,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSyncing) "Synchronisation Firestore..." else "Cloud Firestore & Room SQLite",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                viewModel.syncPriceAlertsWithFirestore()
                                Toast.makeText(context, "Synchronisation avec Firestore effectuée", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Sync Cloud Firestore",
                                tint = BluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("close_price_alerts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions & Tabs Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter Tabs
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTabIndex == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (selectedTabIndex == 0) 2.dp else 0.dp,
                            modifier = Modifier.clickable { selectedTabIndex = 0 }
                        ) {
                            Text(
                                text = "Toutes (${allAlerts.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTabIndex == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTabIndex == 1) EmeraldSuccess.copy(alpha = 0.15f) else Color.Transparent,
                            border = if (selectedTabIndex == 1) BorderStroke(1.dp, EmeraldSuccess) else null,
                            modifier = Modifier.clickable { selectedTabIndex = 1 }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Déclenchées (${triggeredAlerts.size})",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTabIndex == 1) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Test Alert Button
                    OutlinedButton(
                        onClick = {
                            viewModel.triggerTestPriceDropNotification()
                            Toast.makeText(context, "Notification envoyée ! Vérifiez vos notifications Android.", Toast.LENGTH_LONG).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEA580C)),
                        modifier = Modifier.testTag("test_price_alert_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Tester Notification", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val displayedAlerts = if (selectedTabIndex == 1) triggeredAlerts else allAlerts

                if (displayedAlerts.isEmpty()) {
                    // Empty State
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
                                color = Color(0xFFEA580C).copy(alpha = 0.08f),
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsNone,
                                        contentDescription = null,
                                        tint = Color(0xFFEA580C).copy(alpha = 0.6f),
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (selectedTabIndex == 1) "Aucune baisse de prix déclenchée pour l'instant" else "Aucune alerte configurée",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (selectedTabIndex == 1)
                                    "Dès qu'un supermarché baisse le prix sous votre seuil, vous recevrez une notification instantanée et l'article apparaîtra ici !"
                                else
                                    "Ouvrez la fiche de n'importe quel produit et définissez votre prix cible pour être notifié en temps réel lors d'une promotion ou baisse de prix.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { showCreateDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("create_new_alert_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Créer une alerte produit", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayedAlerts, key = { "${it.id}_${it.catalogType}_${it.productId}_${it.targetPrice}" }) { alert ->
                            PriceAlertCard(
                                alert = alert,
                                onAddToCart = {
                                    viewModel.addAlertProductToCart(alert)
                                    Toast.makeText(context, "${alert.productName} ajouté au panier !", Toast.LENGTH_SHORT).show()
                                },
                                onEdit = { editingAlert = alert },
                                onDelete = {
                                    viewModel.removePriceAlert(alert)
                                    Toast.makeText(context, "Alerte supprimée", Toast.LENGTH_SHORT).show()
                                },
                                onClickProduct = {
                                    if (alert.productId > 0) {
                                        onNavigateToProduct?.invoke(alert.productId)
                                        onDismiss()
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Floating Action Button to Add Alert
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_create_alert_dialog_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Ajouter une alerte de prix", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Modal to create or edit an alert
    if (showCreateDialog || editingAlert != null) {
        CreateOrEditAlertDialog(
            existingAlert = editingAlert,
            allProducts = allProducts,
            onDismiss = {
                showCreateDialog = false
                editingAlert = null
            },
            onSave = { product, targetPrice ->
                viewModel.setPriceAlert(product, targetPrice)
                Toast.makeText(context, "Alerte activée pour ${product.name} à Rs ${String.format("%.2f", targetPrice)}", Toast.LENGTH_SHORT).show()
                showCreateDialog = false
                editingAlert = null
            }
        )
    }
}

@Composable
fun PriceAlertCard(
    alert: PriceAlertEntity,
    onAddToCart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClickProduct: () -> Unit
) {
    val storeColor = when (alert.catalogType.uppercase()) {
        "DREAMPRICE" -> DreampriceColor
        "INTERMART" -> IntermartColor
        "SUPER U" -> Color(0xFFE53935)
        "JUMBO" -> Color(0xFFFF9800)
        "WINNERS" -> Color(0xFF4CAF50)
        else -> BluePrimary
    }

    val isTriggered = alert.isTriggered || (alert.currentPrice > 0 && alert.currentPrice <= alert.targetPrice)
    val savings = if (alert.initialPrice > alert.currentPrice) alert.initialPrice - alert.currentPrice else 0.0
    val discountPct = if (alert.initialPrice > 0) ((alert.initialPrice - alert.targetPrice) / alert.initialPrice) * 100 else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickProduct() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTriggered) EmeraldSuccess.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            if (isTriggered) EmeraldSuccess.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Triggered banner if threshold reached
            if (isTriggered) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldSuccess,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🎉 PRIX SOUS LE SEUIL ! (Rs ${String.format("%.2f", alert.currentPrice)} <= Rs ${String.format("%.2f", alert.targetPrice)})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        if (savings > 0) {
                            Text(
                                text = "-Rs ${String.format("%.2f", savings)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Top Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = storeColor
                    ) {
                        Text(
                            text = alert.catalogType,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (alert.category.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${alert.category}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Firestore",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Firestore",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Product Name
            Text(
                text = alert.productName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (alert.brand.isNotBlank() || alert.unit.isNotBlank()) {
                Text(
                    text = listOf(alert.brand, alert.unit).filter { it.isNotBlank() }.joinToString(" • "),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(10.dp))

            // Prices Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Target Threshold Price
                Column {
                    Text(
                        text = "Seuil d'alerte défini",
                        fontSize = 11.sp,
                        color = Color(0xFFEA580C),
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "<= Rs ${String.format("%.2f", alert.targetPrice)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEA580C)
                        )
                    }
                }

                // Current Price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Prix actuel en magasin",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Rs ${String.format("%.2f", alert.currentPrice)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isTriggered) EmeraldSuccess else BluePrimary
                    )
                }
            }

            // If triggered, Action Button to Buy
            if (isTriggered) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAddToCart,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ajouter au panier au prix réduit (Rs ${String.format("%.2f", alert.currentPrice)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrEditAlertDialog(
    existingAlert: PriceAlertEntity?,
    allProducts: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, Double) -> Unit
) {
    var searchQuery by remember { mutableStateOf(existingAlert?.productName ?: "") }
    var selectedProduct by remember {
        mutableStateOf<ProductEntity?>(
            if (existingAlert != null) {
                allProducts.firstOrNull { it.id == existingAlert.productId } ?: ProductEntity(
                    id = existingAlert.productId,
                    catalogType = existingAlert.catalogType,
                    name = existingAlert.productName,
                    category = existingAlert.category,
                    brand = existingAlert.brand,
                    unit = existingAlert.unit,
                    price = existingAlert.currentPrice,
                    cost = 0.0
                )
            } else null
        )
    }

    var targetPriceText by remember {
        mutableStateOf(
            if (existingAlert != null) String.format("%.2f", existingAlert.targetPrice)
            else if (selectedProduct != null) String.format("%.2f", (selectedProduct!!.price * 0.90).coerceAtLeast(1.0))
            else ""
        )
    }

    var isDropdownExpanded by remember { mutableStateOf(false) }

    val filteredProducts = remember(searchQuery, allProducts) {
        if (searchQuery.isBlank()) allProducts.take(15)
        else allProducts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.brand.contains(searchQuery, ignoreCase = true)
        }.take(20)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEA580C).copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = Color(0xFFEA580C),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (existingAlert != null) "Modifier l'alerte de prix" else "Nouvelle alerte de prix",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Product Selection
                if (existingAlert == null) {
                    Text(
                        text = "1. Sélectionnez le produit à surveiller",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedProduct?.let { "${it.name} (${it.catalogType} - Rs ${String.format("%.2f", it.price)})" } ?: searchQuery,
                            onValueChange = {
                                searchQuery = it
                                selectedProduct = null
                                isDropdownExpanded = true
                            },
                            placeholder = { Text("Rechercher un produit...", fontSize = 13.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded && filteredProducts.isNotEmpty(),
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            filteredProducts.forEach { product ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(text = product.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "${product.catalogType} • Rs ${String.format("%.2f", product.price)} • ${product.category}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedProduct = product
                                        searchQuery = product.name
                                        targetPriceText = String.format("%.2f", (product.price * 0.90).coerceAtLeast(1.0))
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Current Price Overview
                selectedProduct?.let { prod ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = prod.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Supermarché : ${prod.catalogType}", fontSize = 11.sp, color = BluePrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Prix actuel", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "Rs ${String.format("%.2f", prod.price)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Threshold Input
                Text(
                    text = "2. Seuil de prix souhaité (Notification si prix <= seuil)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = targetPriceText,
                    onValueChange = { targetPriceText = it },
                    label = { Text("Seuil en Roupies (Rs)") },
                    placeholder = { Text("Ex: 75.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Quick percentage reduction shortcuts
                selectedProduct?.let { prod ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Raccourcis de réduction :",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 15, 20).forEach { pct ->
                            val calculated = prod.price * (1.0 - pct / 100.0)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BluePrimary.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        targetPriceText = String.format("%.2f", calculated)
                                    }
                            ) {
                                Text(
                                    text = "-$pct%\nRs ${String.format("%.0f", calculated)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Save Button
                val parsedTarget = targetPriceText.replace(",", ".").toDoubleOrNull()
                val isValid = selectedProduct != null && parsedTarget != null && parsedTarget > 0.0

                Button(
                    onClick = {
                        if (isValid) {
                            onSave(selectedProduct!!, parsedTarget!!)
                        }
                    },
                    enabled = isValid,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Enregistrer l'alerte", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
