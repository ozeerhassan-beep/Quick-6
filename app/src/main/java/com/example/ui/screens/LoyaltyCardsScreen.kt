package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.LoyaltyCardEntity
import com.example.ui.CatalogViewModel
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyCardsScreen(viewModel: CatalogViewModel) {
    val loyaltyCards by viewModel.loyaltyCards.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCardForFullscreen by remember { mutableStateOf<LoyaltyCardEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
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
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cartes de Fidélité",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Stockez vos cartes pour un passage rapide en caisse",
                        fontSize = 12.sp,
                        color = SlateTextSecondary
                    )
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_loyalty_card_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (loyaltyCards.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = SlateTextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Aucune carte de fidélité",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ajoutez vos cartes Lolo, Intermart, Dreamprice, etc.",
                            fontSize = 13.sp,
                            color = SlateTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(loyaltyCards, key = { "${it.id}_${it.storeName}_${it.cardNumber}" }) { card ->
                        LoyaltyCardItemCard(
                            card = card,
                            onClick = { selectedCardForFullscreen = card },
                            onDelete = { viewModel.deleteLoyaltyCard(card) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLoyaltyCardDialog(
            onDismiss = { showAddDialog = false },
            onSave = { store, number, holder, type, color, imagePath ->
                viewModel.saveLoyaltyCard(store, number, holder, type, color, imagePath)
                showAddDialog = false
            }
        )
    }

    selectedCardForFullscreen?.let { card ->
        FullscreenLoyaltyCardDialog(
            card = card,
            onDismiss = { selectedCardForFullscreen = null }
        )
    }
}

@Composable
fun LoyaltyCardItemCard(
    card: LoyaltyCardEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(card.colorHex))
    } catch (e: Exception) {
        BluePrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable(onClick = onClick)
            .testTag("loyalty_card_${card.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            cardColor,
                            cardColor.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = card.storeName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Column {
                    Text(
                        text = card.cardNumber,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = card.cardHolderName.ifBlank { "Client Kwic-Kart" },
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = card.barcodeType,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddLoyaltyCardDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String?) -> Unit
) {
    var storeName by remember { mutableStateOf("Lolo") }
    var cardNumber by remember { mutableStateOf("") }
    var cardHolderName by remember { mutableStateOf("") }
    var barcodeType by remember { mutableStateOf("CODE_128") }
    var selectedColorHex by remember { mutableStateOf("#1E3A8A") }
    var cardImagePath by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            cardImagePath = it.toString()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            try {
                val file = File(context.cacheDir, "loyalty_${System.currentTimeMillis()}.jpg")
                val os = FileOutputStream(file)
                it.compress(Bitmap.CompressFormat.JPEG, 90, os)
                os.flush()
                os.close()
                cardImagePath = file.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val presetStores = listOf("Lolo", "Intermart", "Dreamprice", "Super U", "Winners", "Way", "Jumbo")
    val storeColors = mapOf(
        "Lolo" to "#16A34A",
        "Intermart" to "#DC2626",
        "Dreamprice" to "#D97706",
        "Super U" to "#1E3A8A",
        "Winners" to "#7C3AED",
        "Way" to "#0284C7",
        "Jumbo" to "#4F46E5"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une carte de fidélité") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = storeName,
                    onValueChange = {
                        storeName = it
                        selectedColorHex = storeColors[it] ?: "#1E3A8A"
                    },
                    label = { Text("Nom du Supermarché") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetStores.forEach { store ->
                        FilterChip(
                            selected = storeName.equals(store, ignoreCase = true),
                            onClick = {
                                storeName = store
                                selectedColorHex = storeColors[store] ?: "#1E3A8A"
                            },
                            label = { Text(store, fontSize = 11.sp, maxLines = 1) }
                        )
                    }
                }

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it },
                    label = { Text("Numéro de carte / Code-barres") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = cardHolderName,
                    onValueChange = { cardHolderName = it },
                    label = { Text("Nom du titulaire (Optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("CODE_128", "QR_CODE", "EAN_13").forEach { type ->
                        FilterChip(
                            selected = barcodeType == type,
                            onClick = { barcodeType = type },
                            label = { Text(type, fontSize = 11.sp, maxLines = 1) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text("Photo de la carte (Optionnel)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Prendre photo", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importer", fontSize = 12.sp)
                    }
                }

                if (cardImagePath != null) {
                    Text(
                        text = "✓ Photo de carte ajoutée",
                        fontSize = 12.sp,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (storeName.isNotBlank() && cardNumber.isNotBlank()) {
                        onSave(storeName, cardNumber, cardHolderName, barcodeType, selectedColorHex, cardImagePath)
                    }
                },
                enabled = storeName.isNotBlank() && cardNumber.isNotBlank()
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
fun FullscreenLoyaltyCardDialog(
    card: LoyaltyCardEntity,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.storeName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Fermer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Canvas-based Barcode / QR Code rendering
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            ) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.BLACK
                                    style = android.graphics.Paint.Style.FILL
                                }

                                if (card.barcodeType == "QR_CODE") {
                                    // Draw QR-like matrix placeholder based on card number hash
                                    val seed = card.cardNumber.hashCode()
                                    val gridSize = 21
                                    val cellSize = canvasWidth / (gridSize + 4)
                                    val startX = (canvasWidth - (gridSize * cellSize)) / 2
                                    val startY = (canvasHeight - (gridSize * cellSize)) / 2

                                    for (i in 0 until gridSize) {
                                        for (j in 0 until gridSize) {
                                            val isDark = ((i * j + seed) % 3 == 0) || (i == 0 || i == gridSize - 1 || j == 0 || j == gridSize - 1)
                                            if (isDark) {
                                                drawContext.canvas.nativeCanvas.drawRect(
                                                    startX + i * cellSize,
                                                    startY + j * cellSize,
                                                    startX + (i + 1) * cellSize,
                                                    startY + (j + 1) * cellSize,
                                                    paint
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Draw Barcode lines based on characters
                                    val charList = card.cardNumber.toCharArray()
                                    val totalBars = charList.size * 6 + 15
                                    val barWidth = canvasWidth / totalBars.toFloat()
                                    var currentX = barWidth * 3

                                    for (c in charList) {
                                        val code = c.code
                                        for (b in 0..5) {
                                            val isDark = (code + b) % 2 == 0
                                            if (isDark) {
                                                drawContext.canvas.nativeCanvas.drawRect(
                                                    currentX,
                                                    10f,
                                                    currentX + barWidth * (if (b % 3 == 0) 2f else 1f),
                                                    canvasHeight - 20f,
                                                    paint
                                                )
                                            }
                                            currentX += barWidth * 1.5f
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = card.cardNumber,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                letterSpacing = 3.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = card.cardHolderName.ifBlank { "Client Kwic-Kart" },
                                fontSize = 14.sp,
                                color = SlateTextSecondary
                            )
                        }
                    }
                }

                Text(
                    text = "Présentez ce code en caisse pour scanner votre carte de fidélité",
                    fontSize = 13.sp,
                    color = SlateTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
