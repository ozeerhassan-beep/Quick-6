package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProductEntity
import com.example.ui.CatalogViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.DreampriceColor
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IntermartColor
import com.example.ui.theme.SlateTextSecondary
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CompareScreen(
    viewModel: CatalogViewModel,
    onNavigateToProducts: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val comparedProducts by viewModel.comparedProducts.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var animatingItemKey by remember { mutableStateOf<String?>(null) }
    
    // Exact button center tracking coordinates
    var buttonCenterX by remember { mutableStateOf(0f) }
    var buttonCenterY by remember { mutableStateOf(0f) }

    val animProgressX = remember { Animatable(0f) }
    val animProgressY = remember { Animatable(0f) }
    val animAlpha = remember { Animatable(1f) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CompareArrows,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Comparateur de Prix",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Liste personnalisée de comparaison",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    if (comparedProducts.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.clearCompareList()
                                Toast.makeText(context, "Liste de comparaison vidée", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "Vider",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Vider",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (comparedProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BluePrimary.copy(alpha = 0.1f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CompareArrows,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = BluePrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Liste de comparaison vide",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Sélectionnez des articles dans l'onglet 'Produits' en cliquant sur le bouton 'Comparer' pour mesurer les prix entre supermarchés.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onNavigateToProducts,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(text = "Parcourir les produits", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        GlobalBasketComparisonCard(
                            comparedProducts = comparedProducts,
                            allProducts = allProducts
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Articles Sélectionnés (${comparedProducts.size})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            TextButton(onClick = onNavigateToProducts) {
                                Text("+ Ajouter d'autres", fontSize = 12.sp, color = BluePrimary)
                            }
                        }
                    }

                    items(comparedProducts, key = { "${it.catalogType}_${it.id}_${it.name}" }) { product ->
                        val itemKey = "${product.catalogType}_${product.id}_${product.name}"
                        ComparedProductCard(
                            product = product,
                            allProducts = allProducts,
                            onAddToCart = { layoutCoordinates ->
                                viewModel.addToCart(product)
                                Toast.makeText(context, "${product.name} ajouté au panier", Toast.LENGTH_SHORT).show()
                                
                                // Capture exact root position and calculate center of the button
                                val position = layoutCoordinates.positionInRoot()
                                buttonCenterX = position.x + (layoutCoordinates.size.width / 2f)
                                buttonCenterY = position.y + (layoutCoordinates.size.height / 2f)

                                animatingItemKey = itemKey
                                coroutineScope.launch {
                                    animProgressX.snapTo(0f)
                                    animProgressY.snapTo(0f)
                                    animAlpha.snapTo(1f)
                                    
                                    val screenWidth = context.resources.displayMetrics.widthPixels.toFloat()
                                    val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()
                                    
                                    // Target 3rd tab coordinates relative to button center
                                    val targetTabX = (screenWidth * 0.5f) - buttonCenterX
                                    val targetTabY = (screenHeight - 70f) - buttonCenterY
                                    
                                    val jobX = launch {
                                        animProgressX.animateTo(
                                            targetValue = targetTabX,
                                            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                                        )
                                    }
                                    val jobY = launch {
                                        animProgressY.animateTo(
                                            targetValue = targetTabY,
                                            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                                        )
                                    }
                                    val jobAlpha = launch {
                                        animAlpha.animateTo(
                                            targetValue = 0f,
                                            animationSpec = tween(durationMillis = 500, delayMillis = 150)
                                        )
                                    }
                                    joinAll(jobX, jobY, jobAlpha)
                                    animatingItemKey = null
                                }
                            },
                            onRemove = {
                                viewModel.removeFromCompare(product.id)
                                Toast.makeText(context, "${product.name} retiré", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // Virtual String and Dragged Item Animation Overlay matching actual center positions
        if (animatingItemKey != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val screenWidth = size.width
                val screenHeight = size.height
                
                val thirdTabX = screenWidth * 0.5f
                val thirdTabY = screenHeight - 35.dp.toPx()
                
                val currentX = buttonCenterX + animProgressX.value
                val currentY = buttonCenterY + animProgressY.value

                drawLine(
                    color = BluePrimary.copy(alpha = animAlpha.value * 0.85f),
                    start = Offset(thirdTabX, thirdTabY),
                    end = Offset(currentX, currentY),
                    strokeWidth = 2.5.dp.toPx()
                )
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val iconSizePx = with(density) { 42.dp.toPx() }
                Surface(
                    shape = CircleShape,
                    color = BluePrimary,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(42.dp)
                        .offset {
                            IntOffset(
                                (buttonCenterX - (iconSizePx / 2f) + animProgressX.value).roundToInt(),
                                (buttonCenterY - (iconSizePx / 2f) + animProgressY.value).roundToInt()
                            )
                        }
                        .alpha(animAlpha.value)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalBasketComparisonCard(
    comparedProducts: List<ProductEntity>,
    allProducts: List<ProductEntity>
) {
    var totalDream = 0.0
    var totalInter = 0.0
    var dreamMatchCount = 0
    var interMatchCount = 0

    comparedProducts.forEach { p ->
        val normName = p.name.trim().lowercase()
        val normUnit = p.unit.trim().lowercase()
        val dream = allProducts.find {
            it.name.trim().lowercase() == normName &&
            it.catalogType.equals("DREAMPRICE", ignoreCase = true) &&
            (normUnit.isBlank() || it.unit.trim().isBlank() || it.unit.trim().lowercase() == normUnit)
        }
        val inter = allProducts.find {
            it.name.trim().lowercase() == normName &&
            it.catalogType.equals("INTERMART", ignoreCase = true) &&
            (normUnit.isBlank() || it.unit.trim().isBlank() || it.unit.trim().lowercase() == normUnit)
        }

        val pDream = dream?.price ?: if (p.catalogType.equals("DREAMPRICE", ignoreCase = true)) p.price else null
        val pInter = inter?.price ?: if (p.catalogType.equals("INTERMART", ignoreCase = true)) p.price else null

        if (pDream != null) {
            totalDream += pDream
            dreamMatchCount++
        }
        if (pInter != null) {
            totalInter += pInter
            interMatchCount++
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = BluePrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bilan Comparatif de votre Sélection",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = DreampriceColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, DreampriceColor.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Dreamprice",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DreampriceColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rs ${String.format("%.2f", totalDream)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DreampriceColor
                        )
                        Text(
                            text = "$dreamMatchCount / ${comparedProducts.size} articles",
                            fontSize = 10.sp,
                            color = SlateTextSecondary
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = IntermartColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, IntermartColor.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Intermart",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IntermartColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rs ${String.format("%.2f", totalInter)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = IntermartColor
                        )
                        Text(
                            text = "$interMatchCount / ${comparedProducts.size} articles",
                            fontSize = 10.sp,
                            color = SlateTextSecondary
                        )
                    }
                }
            }

            if (totalDream > 0 && totalInter > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                val diff = kotlin.math.abs(totalDream - totalInter)
                val cheaperStore = if (totalDream < totalInter) "Dreamprice" else if (totalInter < totalDream) "Intermart" else "Équivalent"
                val cheaperColor = if (totalDream < totalInter) DreampriceColor else if (totalInter < totalDream) IntermartColor else EmeraldSuccess

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = cheaperColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = cheaperColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (cheaperStore == "Équivalent") {
                            Text(
                                text = "Les deux enseignes proposent le même total global.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = cheaperColor
                            )
                        } else {
                            Text(
                                text = "$cheaperStore est moins cher de Rs ${String.format("%.2f", diff)} sur votre liste !",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = cheaperColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparedProductCard(
    product: ProductEntity,
    allProducts: List<ProductEntity>,
    onAddToCart: (LayoutCoordinates) -> Unit,
    onRemove: () -> Unit
) {
    val normName = product.name.trim().lowercase()
    val normUnit = product.unit.trim().lowercase()
    val dreamItem = allProducts.find {
        it.name.trim().lowercase() == normName &&
        it.catalogType.equals("DREAMPRICE", ignoreCase = true) &&
        (normUnit.isBlank() || it.unit.trim().isBlank() || it.unit.trim().lowercase() == normUnit)
    }
    val interItem = allProducts.find {
        it.name.trim().lowercase() == normName &&
        it.catalogType.equals("INTERMART", ignoreCase = true) &&
        (normUnit.isBlank() || it.unit.trim().isBlank() || it.unit.trim().lowercase() == normUnit)
    }

    val dreamPrice = dreamItem?.price ?: if (product.catalogType.equals("DREAMPRICE", ignoreCase = true)) product.price else null
    val interPrice = interItem?.price ?: if (product.catalogType.equals("INTERMART", ignoreCase = true)) product.price else null

    var buttonCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Retirer",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = product.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = DreampriceColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, DreampriceColor.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = DreampriceColor) {
                                Text(
                                    text = "D",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dreamprice", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DreampriceColor)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (dreamPrice != null) {
                            Text(
                                text = "Rs ${String.format("%.2f", dreamPrice)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DreampriceColor
                            )
                        } else {
                            Text("Non répertorié", fontSize = 11.sp, color = SlateTextSecondary)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = IntermartColor.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, IntermartColor.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = IntermartColor) {
                                Text(
                                    text = "I",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Intermart", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IntermartColor)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (interPrice != null) {
                            Text(
                                text = "Rs ${String.format("%.2f", interPrice)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = IntermartColor
                            )
                        } else {
                            Text("Non répertorié", fontSize = 11.sp, color = SlateTextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dreamPrice != null && interPrice != null) {
                    val diff = kotlin.math.abs(dreamPrice - interPrice)
                    if (dreamPrice < interPrice) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DreampriceColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Moins cher à Dreamprice (-Rs ${String.format("%.2f", diff)})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DreampriceColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (interPrice < dreamPrice) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = IntermartColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Moins cher à Intermart (-Rs ${String.format("%.2f", diff)})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = IntermartColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Prix identique",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        buttonCoordinates?.let { coords -> onAddToCart(coords) }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .onGloballyPositioned { coordinates ->
                            buttonCoordinates = coordinates
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "Ajouter au panier",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Au panier",
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}