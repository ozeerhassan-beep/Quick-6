package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.ProductEntity
import com.example.ui.CatalogViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.DreampriceColor
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IntermartColor
import com.example.util.BarcodeAnalyzer
import com.example.util.BarcodeScanMatchResult
import com.example.util.OnlineProductInfo
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerDialog(
    viewModel: CatalogViewModel,
    onDismiss: () -> Unit,
    onBarcodeScanned: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val allProducts by viewModel.allProducts.collectAsState()
    val barcodeResult by viewModel.barcodeScanResult.collectAsState()
    val isLookingUp by viewModel.isBarcodeLookingUp.collectAsState()
    val wishlistedIds by viewModel.wishlistedProductIds.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var manualBarcodeInput by remember { mutableStateOf("") }
    var lastScannedCode by remember { mutableStateOf("") }
    var cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Quick Add Form state
    var showQuickAddForm by remember { mutableStateOf(false) }
    var quickAddCatalog by remember { mutableStateOf("DREAMPRICE") }
    var quickAddPrice by remember { mutableStateOf("95.00") }
    var quickAddCost by remember { mutableStateOf("75.00") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Permission caméra requise pour scanner. Utilisez la saisie de code ci-dessous.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraExecutor.shutdown()
            } catch (e: Exception) {
                // ignore
            }
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun handleBarcodeScanned(code: String) {
        val clean = code.trim()
        if (clean.isNotBlank() && clean != lastScannedCode) {
            lastScannedCode = clean
            manualBarcodeInput = clean
            viewModel.lookupBarcode(clean)
            onBarcodeScanned?.invoke(clean)
            Toast.makeText(context, "Code détecté : $clean", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = {
            viewModel.clearBarcodeResult()
            onDismiss()
        },
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BluePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Scanner de Code-Barres",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Reconnaissance Internet & Vérification Catalogue",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.clearBarcodeResult()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("close_scanner_modal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Google Code Scanner API (Zéro-Permission) button
                    item {
                        Button(
                            onClick = {
                                try {
                                    val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context)
                                    scanner.startScan()
                                        .addOnSuccessListener { barcode ->
                                            val rawValue = barcode.rawValue
                                            if (!rawValue.isNullOrBlank()) {
                                                handleBarcodeScanned(rawValue)
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(context, "Erreur du scanner Google Code: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "API Google Code Scanner non disponible: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanner via l'API Google Code (Instantané)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // 1. Camera Viewfinder with ML Kit Analyzer
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (hasCameraPermission) {
                                AndroidView(
                                    factory = { ctx ->
                                        val previewView = PreviewView(ctx)
                                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                                        cameraProviderFuture.addListener({
                                            try {
                                                val cameraProvider = cameraProviderFuture.get()
                                                val preview = Preview.Builder().build().also {
                                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                                }

                                                val imageAnalysis = ImageAnalysis.Builder()
                                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                                    .build()
                                                    .also { analysis ->
                                                        analysis.setAnalyzer(
                                                            cameraExecutor,
                                                            BarcodeAnalyzer { detectedCode ->
                                                                // Switch to main thread for UI/ViewModel update
                                                                ContextCompat.getMainExecutor(ctx).execute {
                                                                    handleBarcodeScanned(detectedCode)
                                                                }
                                                            }
                                                        )
                                                    }

                                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                                cameraProvider.unbindAll()
                                                cameraProvider.bindToLifecycle(
                                                    lifecycleOwner,
                                                    cameraSelector,
                                                    preview,
                                                    imageAnalysis
                                                )
                                            } catch (e: Exception) {
                                                Log.e("BarcodeScanner", "Camera & Analyzer bind failure", e)
                                            }
                                        }, ContextCompat.getMainExecutor(ctx))

                                        previewView
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Scanning Reticle & Animated Laser Indicator
                                Box(
                                    modifier = Modifier
                                        .size(width = 250.dp, height = 120.dp)
                                        .border(
                                            BorderStroke(2.dp, if (lastScannedCode.isNotBlank()) EmeraldSuccess else BluePrimary),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.92f)
                                            .height(2.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color.Transparent, Color(0xFFEF4444), Color.Transparent)
                                                )
                                            )
                                    )
                                }

                                // Active Scanner Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Pointez vers un code-barres (EAN, UPC, QR)",
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            } else {
                                // Camera Permission Required Card
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Accès caméra requis pour le scan en direct",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Autoriser la Caméra", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Manual Barcode Search & Test Barcodes
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = manualBarcodeInput,
                                    onValueChange = { manualBarcodeInput = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("manual_barcode_input"),
                                    placeholder = {
                                        Text("Code-barres / EAN (ex: 3017620422003)", fontSize = 12.sp)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = BluePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Search
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            keyboardController?.hide()
                                            if (manualBarcodeInput.isNotBlank()) {
                                                viewModel.lookupBarcode(manualBarcodeInput)
                                                onBarcodeScanned?.invoke(manualBarcodeInput)
                                            }
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BluePrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        keyboardController?.hide()
                                        if (manualBarcodeInput.isNotBlank()) {
                                            viewModel.lookupBarcode(manualBarcodeInput)
                                            onBarcodeScanned?.invoke(manualBarcodeInput)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("search_barcode_button")
                                ) {
                                    if (isLookingUp) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Chercher", fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Quick Real-world & Catalog Samples
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Exemples :",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                val sampleCodes = listOf(
                                    "609101234001" to "Lait Red Cow",
                                    "609102345002" to "Riz Orient",
                                    "609103456003" to "Huile Rani",
                                    "8076809513753" to "Spaghetti Barilla",
                                    "3265471410101" to "Huile Lesieur",
                                    "3451790401124" to "Beurre Elle&Vire"
                                )

                                sampleCodes.forEach { (code, label) ->
                                    Surface(
                                        onClick = {
                                            manualBarcodeInput = code
                                            viewModel.lookupBarcode(code)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            color = BluePrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Loading Indicator for Internet Lookup
                    if (isLookingUp) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = BluePrimary,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Recherche de l'article sur Internet...",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Interrogation d'Open Food Facts & Base mondiale",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Scan & Internet Matching Result
                    if (barcodeResult != null && !isLookingUp) {
                        val result = barcodeResult!!
                        val online = result.onlineProduct

                        item {
                            // Section: Online Item Identification Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Language,
                                                contentDescription = null,
                                                tint = BluePrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Article Identifié sur Internet",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BluePrimary
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = online.source,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = online.productName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (online.brand.isNotBlank() || online.category.isNotBlank() || online.unit.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = listOf(online.brand, online.category, online.unit)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" • "),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Code-barres EAN : ${result.barcode}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        // Section: Catalog Presence Banner & Action
                        item {
                            if (result.isListedInCatalog) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = EmeraldSuccess.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Article listé dans votre catalogue !",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldSuccess
                                            )
                                            Text(
                                                text = "${result.matchedProducts.size} correspondance(s) trouvée(s) dans vos supermarchés",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.WarningAmber,
                                                contentDescription = null,
                                                tint = Color(0xFFF59E0B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Non listé dans vos catalogues de prix",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB45309)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Ce produit a été identifié en ligne mais n'est pas encore enregistré dans votre base de données locale.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { showQuickAddForm = !showQuickAddForm },
                                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("quick_add_scanned_product_button")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                if (showQuickAddForm) "Masquer le formulaire" else "➕ Ajouter au catalogue local",
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Section: Quick Add Form (if opened for unlisted product)
                        if (showQuickAddForm && !result.isListedInCatalog) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "Ajout rapide au catalogue",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Store selector
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf("DREAMPRICE", "INTERMART", "SUPER U").forEach { store ->
                                                val isSelected = quickAddCatalog == store
                                                Surface(
                                                    onClick = { quickAddCatalog = store },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) BluePrimary else MaterialTheme.colorScheme.surface,
                                                    border = BorderStroke(1.dp, if (isSelected) BluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = store,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.padding(vertical = 8.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Price & Cost Inputs
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = quickAddPrice,
                                                onValueChange = { quickAddPrice = it },
                                                label = { Text("Prix de vente (Rs)", fontSize = 11.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            OutlinedTextField(
                                                value = quickAddCost,
                                                onValueChange = { quickAddCost = it },
                                                label = { Text("Prix d'achat (Rs)", fontSize = 11.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    val priceVal = quickAddPrice.toDoubleOrNull() ?: 90.0
                                                    val costVal = quickAddCost.toDoubleOrNull() ?: 70.0
                                                    viewModel.quickAddProductFromBarcode(
                                                        onlineInfo = online,
                                                        targetCatalog = quickAddCatalog,
                                                        price = priceVal,
                                                        cost = costVal
                                                    )
                                                    showQuickAddForm = false
                                                    Toast.makeText(context, "${online.productName} ajouté à $quickAddCatalog (Room)", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("save_scanned_to_catalog_button")
                                            ) {
                                                Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Ajouter au Catalogue", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    val priceVal = quickAddPrice.toDoubleOrNull() ?: 90.0
                                                    val costVal = quickAddCost.toDoubleOrNull() ?: 70.0
                                                    viewModel.quickAddOnlineProductToCart(
                                                        onlineInfo = online,
                                                        targetCatalog = quickAddCatalog,
                                                        price = priceVal,
                                                        cost = costVal,
                                                        quantity = 1
                                                    )
                                                    showQuickAddForm = false
                                                    Toast.makeText(context, "${online.productName} ajouté au panier !", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("save_scanned_to_cart_button")
                                            ) {
                                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Ajouter au Panier", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section: Matched Catalog Products List
                        if (result.matchedProducts.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Prix & Disponibilité en Rayon (${result.matchedProducts.size}) :",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            items(result.matchedProducts, key = { "${it.catalogType}_${it.id}_${it.name}" }) { product ->
                                val storeColor = when (product.catalogType.uppercase()) {
                                    "DREAMPRICE" -> DreampriceColor
                                    "INTERMART" -> IntermartColor
                                    "SUPER U" -> Color(0xFFE53935)
                                    "JUMBO" -> Color(0xFFFF9800)
                                    "WINNERS" -> Color(0xFF4CAF50)
                                    else -> BluePrimary
                                }
                                val isWishlisted = product.id in wishlistedIds

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = storeColor
                                                ) {
                                                    Text(
                                                        text = product.catalogType,
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "#${product.id} • ${product.category}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = product.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (product.brand.isNotBlank() || product.unit.isNotBlank()) {
                                                Text(
                                                    text = "${product.brand} • ${product.unit}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = "Rs ${String.format("%.2f", product.price)}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = EmeraldSuccess
                                            )
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.addToCart(product)
                                                    Toast.makeText(context, "${product.name} ajouté au panier", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("barcode_add_to_cart_${product.id}")
                                            ) {
                                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Panier", fontSize = 11.sp)
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.toggleWishlist(product)
                                                        Toast.makeText(context, "Favoris mis à jour", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                        contentDescription = "Favoris",
                                                        tint = if (isWishlisted) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.setSearchQuery(product.name)
                                                        viewModel.clearBarcodeResult()
                                                        onDismiss()
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Afficher", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Section: Related Products (if not directly listed)
                        if (result.relatedProducts.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Articles similaires dans votre catalogue :",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            items(result.relatedProducts, key = { "rel_${it.catalogType}_${it.id}_${it.name}" }) { rel ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${rel.name} (${rel.catalogType})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Rs ${String.format("%.2f", rel.price)} • ${rel.category}",
                                                fontSize = 11.sp,
                                                color = EmeraldSuccess
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.addToCart(rel)
                                                Toast.makeText(context, "${rel.name} ajouté au panier", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Ajouter", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
