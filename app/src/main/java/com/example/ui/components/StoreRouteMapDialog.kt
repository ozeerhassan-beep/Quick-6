package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CartItemEntity
import com.example.ui.CatalogViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.util.AisleDefinition
import com.example.util.OptimizedShoppingRoute
import com.example.util.RouteStop
import com.example.util.StoreFloorPlan
import com.example.util.StoreRouteOptimizer

@Composable
fun StoreRouteMapDialog(
    viewModel: CatalogViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cartItems by viewModel.cartItems.collectAsState()

    // Determine initial store based on cart items
    val dominantCatalog = remember(cartItems) {
        cartItems.groupBy { it.catalogType }.maxByOrNull { it.value.size }?.key ?: "WINNERS"
    }

    var selectedStore by remember {
        mutableStateOf(StoreRouteOptimizer.getStoreByCatalogType(dominantCatalog))
    }

    // Set of checked-off / completed cart item IDs
    val completedItemIds = remember { mutableStateListOf<Int>() }

    // Selected aisle for inspection on map
    var selectedAisleInspection by remember { mutableStateOf<AisleDefinition?>(null) }

    // View tab: 0 = "Plan 2D & Navigation", 1 = "Feuille de Route (Étapes)"
    var activeTab by remember { mutableStateOf(0) }

    // Calculated optimized route
    val route = remember(selectedStore, cartItems, completedItemIds.toList()) {
        StoreRouteOptimizer.buildOptimizedRoute(
            store = selectedStore,
            cartItems = cartItems,
            completedItemIds = completedItemIds.toSet()
        )
    }

    val currentStop = remember(route) {
        if (route.stops.isNotEmpty() && route.currentStopIndex < route.stops.size) {
            route.stops[route.currentStopIndex]
        } else null
    }

    val isAllCompleted = route.stops.isNotEmpty() && route.stops.all { it.isCompleted }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Header Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Route,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Plan & Itinéraire Magasin",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Optimisation du parcours sans retour en arrière",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("close_route_map_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fermer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Supermarket Floor Plan Selector Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(StoreRouteOptimizer.STORE_PLANS) { plan ->
                                val isSelected = selectedStore.id == plan.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedStore = plan },
                                    label = {
                                        Text(
                                            text = plan.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Storefront,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BluePrimary.copy(alpha = 0.15f),
                                        selectedLabelColor = BluePrimary,
                                        selectedLeadingIconColor = BluePrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // Stats Banner: Distance, Time, Savings & Progress
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatPill(
                                icon = Icons.Default.DirectionsWalk,
                                label = "${route.totalDistanceMeters} m",
                                subtitle = "Parcours total",
                                color = BluePrimary
                            )
                            StatPill(
                                icon = Icons.Default.Timer,
                                label = "~${route.estimatedMinutes} min",
                                subtitle = "Temps estimé",
                                color = Color(0xFF8B5CF6)
                            )
                            StatPill(
                                icon = Icons.Default.Speed,
                                label = "-${route.minutesSaved} min",
                                subtitle = "Temps économisé",
                                color = Color(0xFF10B981)
                            )
                            StatPill(
                                icon = Icons.Default.DoneAll,
                                label = "${route.completedItemCount}/${route.totalItemCount}",
                                subtitle = "Articles pris",
                                color = if (isAllCompleted) EmeraldSuccess else Color(0xFFF59E0B)
                            )
                        }

                        if (route.totalItemCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val progress = (route.completedItemCount.toFloat() / route.totalItemCount.toFloat()).coerceIn(0f, 1f)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (isAllCompleted) EmeraldSuccess else BluePrimary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAllCompleted) EmeraldSuccess else BluePrimary
                                )
                            }
                        }
                    }
                }

                // Active Stop Hero Card (Turn-by-turn guidance)
                if (cartItems.isNotEmpty()) {
                    ActiveStopBanner(
                        currentStop = currentStop,
                        isAllCompleted = isAllCompleted,
                        completedItemIds = completedItemIds,
                        onToggleItem = { itemId ->
                            if (completedItemIds.contains(itemId)) {
                                completedItemIds.remove(itemId)
                            } else {
                                completedItemIds.add(itemId)
                            }
                        }
                    )
                }

                // Tab Switcher
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = BluePrimary,
                    modifier = Modifier.height(44.dp)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Plan 2D & Tracé", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Étapes (${route.stops.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                // Main Content View
                if (cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Votre panier est vide",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ajoutez des articles à votre panier pour calculer l'itinéraire de courses le plus court.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when (activeTab) {
                            0 -> {
                                // 2D Floor Plan Canvas
                                SupermarketFloorCanvasView(
                                    store = selectedStore,
                                    route = route,
                                    selectedAisle = selectedAisleInspection,
                                    onAisleTapped = { aisle ->
                                        selectedAisleInspection = if (selectedAisleInspection?.id == aisle.id) null else aisle
                                    }
                                )
                            }
                            1 -> {
                                // Turn-by-turn stops list
                                TurnByTurnWaypointsList(
                                    stops = route.stops,
                                    currentStopIndex = route.currentStopIndex,
                                    completedItemIds = completedItemIds,
                                    onToggleItem = { itemId ->
                                        if (completedItemIds.contains(itemId)) {
                                            completedItemIds.remove(itemId)
                                        } else {
                                            completedItemIds.add(itemId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Bottom Action Footer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                completedItemIds.clear()
                                Toast.makeText(context, "Itinéraire réinitialisé", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Réinitialiser", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (!isAllCompleted) {
                                    cartItems.forEach { item ->
                                        if (!completedItemIds.contains(item.id)) completedItemIds.add(item.id)
                                    }
                                    Toast.makeText(context, "Tous les articles sont cochés !", Toast.LENGTH_SHORT).show()
                                } else {
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAllCompleted) EmeraldSuccess else BluePrimary
                            ),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isAllCompleted) Icons.Default.CheckCircle else Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAllCompleted) "Terminer & Fermer" else "Tout Cocher",
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
private fun ActiveStopBanner(
    currentStop: RouteStop?,
    isAllCompleted: Boolean,
    completedItemIds: List<Int>,
    onToggleItem: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isAllCompleted) EmeraldSuccess.copy(alpha = 0.1f) else BluePrimary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, if (isAllCompleted) EmeraldSuccess.copy(alpha = 0.3f) else BluePrimary.copy(alpha = 0.2f))
    ) {
        if (isAllCompleted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldSuccess,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "🎉 Tous vos articles sont ramassés !",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                    Text(
                        text = "Dirigez-vous vers les caisses pour finaliser votre commande.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (currentStop != null) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BluePrimary,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${currentStop.stepIndex}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PROCHAIN ARRÊT : ${currentStop.aisle.label} (${currentStop.aisle.code})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(currentStop.aisle.sectionColor).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = currentStop.aisle.categoryName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(currentStop.aisle.sectionColor),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Distance depuis point précédent : ~${currentStop.distanceMetersFromPrev} mètres",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Items in this current stop
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    currentStop.items.forEach { item ->
                        val isChecked = completedItemIds.contains(item.id)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleItem(item.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleItem(item.id) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = EmeraldSuccess,
                                        uncheckedColor = BluePrimary
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.productName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isChecked) FontWeight.Normal else FontWeight.SemiBold,
                                    color = if (isChecked) Color.Gray else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = BluePrimary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "Qté: ${item.quantity}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupermarketFloorCanvasView(
    store: StoreFloorPlan,
    route: OptimizedShoppingRoute,
    selectedAisle: AisleDefinition?,
    onAisleTapped: (AisleDefinition) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(store) {
                            detectTapGestures { tapOffset ->
                                val normX = tapOffset.x / size.width
                                val normY = tapOffset.y / size.height

                                // Check if tapped inside any aisle bounding box
                                val tapped = store.aisles.firstOrNull { aisle ->
                                    val left = aisle.normX - aisle.widthNorm / 2
                                    val right = aisle.normX + aisle.widthNorm / 2
                                    val top = aisle.normY - aisle.heightNorm / 2
                                    val bottom = aisle.normY + aisle.heightNorm / 2
                                    normX in left..right && normY in top..bottom
                                }
                                if (tapped != null) {
                                    onAisleTapped(tapped)
                                }
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // 1. Draw floor plan perimeter and subtle tile grid
                    drawRoundRect(
                        color = Color(0xFFF1F5F9),
                        size = size,
                        cornerRadius = CornerRadius(24f, 24f)
                    )
                    drawRoundRect(
                        color = Color(0xFFCBD5E1),
                        size = size,
                        cornerRadius = CornerRadius(24f, 24f),
                        style = Stroke(width = 2f)
                    )

                    // Draw subtle grid lines
                    val gridSteps = 8
                    for (i in 1 until gridSteps) {
                        val y = (canvasHeight / gridSteps) * i
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1f
                        )
                    }

                    // 2. Draw Entrance Marker
                    val entranceX = store.entrance.x * canvasWidth
                    val entranceY = store.entrance.y * canvasHeight
                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = 0.25f),
                        radius = 24f,
                        center = Offset(entranceX, entranceY)
                    )
                    drawCircle(
                        color = Color(0xFF10B981),
                        radius = 12f,
                        center = Offset(entranceX, entranceY)
                    )

                    // 3. Draw Checkouts / Cashier Area
                    val checkX = store.checkouts.x * canvasWidth
                    val checkY = store.checkouts.y * canvasHeight
                    drawRoundRect(
                        color = Color(0xFFF59E0B).copy(alpha = 0.25f),
                        topLeft = Offset(checkX - 35f, checkY - 18f),
                        size = Size(70f, 36f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    drawRoundRect(
                        color = Color(0xFFF59E0B),
                        topLeft = Offset(checkX - 35f, checkY - 18f),
                        size = Size(70f, 36f),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = Stroke(width = 2f)
                    )

                    // 4. Draw Supermarket Aisle Blocks
                    store.aisles.forEach { aisle ->
                        val aisleW = aisle.widthNorm * canvasWidth
                        val aisleH = aisle.heightNorm * canvasHeight
                        val left = (aisle.normX * canvasWidth) - (aisleW / 2)
                        val top = (aisle.normY * canvasHeight) - (aisleH / 2)

                        // Check if this aisle is part of the shopping route
                        val stopForAisle = route.stops.find { it.aisle.id == aisle.id }
                        val isTargeted = stopForAisle != null
                        val isCompleted = stopForAisle?.isCompleted == true
                        val isCurrent = route.stops.getOrNull(route.currentStopIndex)?.aisle?.id == aisle.id
                        val isInspected = selectedAisle?.id == aisle.id

                        val blockColor = when {
                            isCurrent -> Color(0xFF2563EB).copy(alpha = 0.25f)
                            isCompleted -> Color(0xFF10B981).copy(alpha = 0.20f)
                            isTargeted -> Color(aisle.sectionColor).copy(alpha = 0.25f)
                            else -> Color(0xFFE2E8F0)
                        }

                        val borderColor = when {
                            isCurrent -> Color(0xFF2563EB)
                            isCompleted -> Color(0xFF10B981)
                            isTargeted -> Color(aisle.sectionColor)
                            isInspected -> Color(0xFF8B5CF6)
                            else -> Color(0xFF94A3B8)
                        }

                        // Aisle shelf box
                        drawRoundRect(
                            color = blockColor,
                            topLeft = Offset(left, top),
                            size = Size(aisleW, aisleH),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                        drawRoundRect(
                            color = borderColor,
                            topLeft = Offset(left, top),
                            size = Size(aisleW, aisleH),
                            cornerRadius = CornerRadius(12f, 12f),
                            style = Stroke(width = if (isCurrent || isInspected) 3.5f else 1.5f)
                        )

                        // Draw Aisle Code Pill on Top
                        val pillColor = if (isCurrent) Color(0xFF2563EB) else if (isCompleted) Color(0xFF10B981) else Color(aisle.sectionColor)
                        drawCircle(
                            color = pillColor,
                            radius = 12f,
                            center = Offset(left + aisleW / 2, top + 16f)
                        )

                        // Target indicator or Stop Number
                        if (stopForAisle != null) {
                            drawCircle(
                                color = if (isCompleted) Color(0xFF10B981) else Color(0xFF2563EB),
                                radius = 10f,
                                center = Offset(left + aisleW - 10f, top + 10f)
                            )
                        }
                    }

                    // 5. Draw Optimized Path Polyline connecting Waypoints
                    if (route.pathPoints.size >= 2) {
                        val path = Path()
                        val first = route.pathPoints.first()
                        path.moveTo(first.x * canvasWidth, first.y * canvasHeight)

                        for (i in 1 until route.pathPoints.size) {
                            val pt = route.pathPoints[i]
                            path.lineTo(pt.x * canvasWidth, pt.y * canvasHeight)
                        }

                        // Background glowing path stroke
                        drawPath(
                            path = path,
                            color = Color(0xFF2563EB).copy(alpha = 0.25f),
                            style = Stroke(
                                width = 12f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Foreground dashed navigation line
                        drawPath(
                            path = path,
                            color = Color(0xFF2563EB),
                            style = Stroke(
                                width = 4f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
                            )
                        )
                    }

                    // 6. Draw Animated Pulsing Target on Current Stop
                    val activeStop = route.stops.getOrNull(route.currentStopIndex)
                    if (activeStop != null) {
                        val currX = activeStop.aisle.normX * canvasWidth
                        val currY = activeStop.aisle.normY * canvasHeight

                        drawCircle(
                            color = Color(0xFF2563EB).copy(alpha = 0.35f),
                            radius = pulseRadius * 1.6f,
                            center = Offset(currX, currY)
                        )
                        drawCircle(
                            color = Color(0xFF2563EB),
                            radius = 9f,
                            center = Offset(currX, currY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 4f,
                            center = Offset(currX, currY)
                        )
                    }
                }

                // Map Legend Overlay
                MapLegendOverlay(modifier = Modifier.align(Alignment.TopEnd))

                // Entrance / Exit Labels
                Text(
                    text = "🟢 ENTRÉE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF10B981),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 8.dp)
                )

                Text(
                    text = "🏁 CAISSES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MapLegendOverlay(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(6.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            LegendItem(color = Color(0xFF2563EB), label = "Étape active")
            LegendItem(color = Color(0xFF10B981), label = "Articles pris")
            LegendItem(color = Color(0xFF94A3B8), label = "Autres rayons")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TurnByTurnWaypointsList(
    stops: List<RouteStop>,
    currentStopIndex: Int,
    completedItemIds: List<Int>,
    onToggleItem: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Entrance Stop Header
        item {
            WaypointHeaderCard(
                icon = Icons.Default.DirectionsWalk,
                title = "Point de Départ : Entrée Principale",
                subtitle = "Prenez un chariot et suivez le tracé optimisé",
                color = Color(0xFF10B981)
            )
        }

        // Ordered Aisle Stops
        itemsIndexed(stops, key = { index, stop -> "${index}_${stop.aisle.id}_${stop.aisle.label}" }) { index, stop ->
            val isCurrent = index == currentStopIndex
            val isCompleted = stop.isCompleted

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isCurrent -> BluePrimary.copy(alpha = 0.06f)
                        isCompleted -> EmeraldSuccess.copy(alpha = 0.04f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    width = if (isCurrent) 1.5.dp else 1.dp,
                    color = when {
                        isCurrent -> BluePrimary
                        isCompleted -> EmeraldSuccess.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 2.dp else 0.5.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    isCompleted -> EmeraldSuccess
                                    isCurrent -> BluePrimary
                                    else -> Color(stop.aisle.sectionColor)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isCompleted) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    } else {
                                        Text(
                                            text = "${stop.stepIndex}",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "Arrêt ${stop.stepIndex} : ${stop.aisle.label} (${stop.aisle.code})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) BluePrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stop.aisle.categoryName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(stop.aisle.sectionColor)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "~${stop.distanceMetersFromPrev}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Items list inside this stop
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        stop.items.forEach { item ->
                            val isChecked = completedItemIds.contains(item.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleItem(item.id) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleItem(item.id) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = EmeraldSuccess,
                                        uncheckedColor = BluePrimary
                                    ),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium,
                                        color = if (isChecked) Color.Gray else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Rs ${String.format("%.2f", item.unitPrice)} / ${item.unit}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = BluePrimary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        text = "Qté: ${item.quantity}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Checkout Finish Card
        item {
            WaypointHeaderCard(
                icon = Icons.Default.CheckCircle,
                title = "Destination Finale : Caisses & Paiement",
                subtitle = "Toutes les étapes sont complétées ! Passez en caisse.",
                color = Color(0xFFF59E0B)
            )
        }
    }
}

@Composable
private fun WaypointHeaderCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
