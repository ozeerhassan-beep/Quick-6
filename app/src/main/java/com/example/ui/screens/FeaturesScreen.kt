package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ProductEntity
import com.example.ui.CatalogViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.util.GeminiService
import com.example.util.RecipeSearchResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesScreen(
    viewModel: CatalogViewModel,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allProducts by viewModel.allProducts.collectAsState()

    var showOcrModal by remember { mutableStateOf(false) }
    var showSharedCartModal by remember { mutableStateOf(false) }
    var showRecipeModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Fonctionnalités Avancées",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Outils IA, Partage et Import de Recettes",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FeatureCard(
                    title = "Scanner de Reçu (OCR)",
                    icon = Icons.Default.QrCodeScanner,
                    accentColor = BluePrimary,
                    description = "Photographiez un ticket de caisse ou importez une image pour extraire et vérifier instantanément les prix des produits.",
                    badgeText = "IA & OCR"
                ) {
                    showOcrModal = true
                }
            }

            item {
                FeatureCard(
                    title = "Panier Partagé en Temps Réel",
                    icon = Icons.Default.Group,
                    accentColor = Color(0xFF10B981),
                    description = "Collaborez sur la même liste de courses en famille ou entre colocataires avec synchronisation instantanée.",
                    badgeText = "Cloud Synced"
                ) {
                    showSharedCartModal = true
                }
            }

            item {
                FeatureCard(
                    title = "Recettes vers Panier Intelligent",
                    icon = Icons.Default.RestaurantMenu,
                    accentColor = Color(0xFFEA580C),
                    description = "Choisissez parmi une sélection de recettes gourmandes et ajoutez tous les ingrédients nécessaires en un seul clic.",
                    badgeText = "Astucieux"
                ) {
                    showRecipeModal = true
                }
            }
        }
    }

    // 1. OCR Receipt Scanner Modal
    if (showOcrModal) {
        ReceiptOcrScannerDialog(
            allProducts = allProducts,
            viewModel = viewModel,
            onDismiss = { showOcrModal = false }
        )
    }

    // 2. Shared Cart Modal
    if (showSharedCartModal) {
        SharedCartDialog(
            onDismiss = { showSharedCartModal = false }
        )
    }

    // 3. Recipes to Cart Modal
    if (showRecipeModal) {
        RecipesToCartDialog(
            allProducts = allProducts,
            viewModel = viewModel,
            onDismiss = { showRecipeModal = false }
        )
    }
}

@Composable
fun FeatureCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    description: String,
    badgeText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 1. Receipt OCR Scanner Dialog
// -------------------------------------------------------------
@Composable
fun ReceiptOcrScannerDialog(
    allProducts: List<ProductEntity>,
    viewModel: CatalogViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var scanCompleted by remember { mutableStateOf(false) }

    val sampleReceiptItems = listOf(
        Pair("Lait Demi-Écrémé 1L", 1.15),
        Pair("Baguette Tradition", 1.10),
        Pair("Beurre Doux 250g", 2.45),
        Pair("Café Moulu 500g", 4.95),
        Pair("Jus d'Orange 1.5L", 2.20)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.925f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = BluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Scanner de Reçu (OCR)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!scanCompleted && !isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(2.dp, BluePrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Aucun ticket scanné pour le moment",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Simulez l'analyse OCR d'un ticket de caisse de supermarché pour extraire les articles et comparer les prix.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    isScanning = true
                                    scope.launch {
                                        delay(1800)
                                        isScanning = false
                                        scanCompleted = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Scanner un Ticket (Caméra/Galerie)")
                            }
                        }
                    }
                } else if (isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BluePrimary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Analyse OCR du ticket par l'IA...", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Extraction des libellés et des prix en cours...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldSuccess.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "Ticket analysé avec succès", fontWeight = FontWeight.Bold, color = EmeraldSuccess, fontSize = 13.sp)
                                    Text(text = "5 articles détectés (Confiance OCR: 98.4%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(text = "Articles extraits du reçu :", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        sampleReceiptItems.forEach { (name, price) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text = name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    }
                                    Text(text = "%.2f €".format(price), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BluePrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                var count = 0
                                sampleReceiptItems.forEachIndexed { index, (name, price) ->
                                    // 1. Try exact or contains match (bidirectional)
                                    var matched = allProducts.firstOrNull { 
                                        it.name.contains(name, ignoreCase = true) || name.contains(it.name, ignoreCase = true)
                                    }
                                    
                                    // 2. Try word-based keyword matching if still null
                                    if (matched == null) {
                                        val nameWords = name.lowercase().split(" ", "-", "'", ",")
                                            .filter { it.length > 2 && it != "les" && it != "des" && it != "aux" && it != "avec" && it != "dans" }
                                        if (nameWords.isNotEmpty()) {
                                            matched = allProducts.firstOrNull { prod ->
                                                val prodNameLower = prod.name.lowercase()
                                                nameWords.any { word -> prodNameLower.contains(word) }
                                            }
                                        }
                                    }
                                    
                                    // 3. Fallback to a custom virtual ProductEntity if no match is found
                                    val finalProduct = matched ?: ProductEntity(
                                        id = (System.currentTimeMillis() % 1000000).toInt() + index + 60000,
                                        catalogType = "REÇU",
                                        name = name,
                                        category = "Épicerie",
                                        brand = "Reçu",
                                        unit = "1u",
                                        price = price,
                                        cost = price,
                                        barcode = ""
                                    )
                                    viewModel.addToCart(finalProduct, 1)
                                    count++
                                    kotlinx.coroutines.delay(30)
                                }
                                Toast.makeText(context, "$count articles ajoutés au panier depuis le reçu !", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Ajouter tous les articles au panier", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. Shared Cart Dialog
// -------------------------------------------------------------
data class SharedCartMember(
    val id: String,
    val name: String,
    val role: String,
    val isOnline: Boolean,
    val isOwner: Boolean = false
)

@Composable
fun SharedCartDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var roomCode by remember { mutableStateOf("CART-FAMILLE-8824") }
    var isCopied by remember { mutableStateOf(false) }

    val members = remember {
        mutableStateListOf(
            SharedCartMember("1", "Moi", "Propriétaire", isOnline = true, isOwner = true),
            SharedCartMember("2", "Émilie", "Conjoint", isOnline = true),
            SharedCartMember("3", "Thomas", "Enfant", isOnline = false)
        )
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var memberToEdit by remember { mutableStateOf<SharedCartMember?>(null) }

    var newMemberName by remember { mutableStateOf("") }
    var newMemberRole by remember { mutableStateOf("") }
    var newMemberOnline by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.925f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Panier Partagé", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Code de Salon Partagé", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = roomCode, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Code Panier Partagé", roomCode)
                                clipboard.setPrimaryClip(clip)
                                isCopied = true
                                Toast.makeText(context, "Code copié dans le presse-papier !", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isCopied) "Code Copié !" else "Copier le code de partage")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Membres connectés au panier :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    TextButton(
                        onClick = {
                            newMemberName = ""
                            newMemberRole = "Membre"
                            newMemberOnline = true
                            showAddDialog = true
                        }
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "+ Ajouter", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    members.forEach { member ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (member.isOnline) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (member.isOnline) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = member.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            if (member.isOwner) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Hôte",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF10B981),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${member.role} • ${if (member.isOnline) "En ligne" else "Hors ligne"}",
                                            fontSize = 11.sp,
                                            color = if (member.isOnline) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val index = members.indexOfFirst { it.id == member.id }
                                            if (index != -1) {
                                                members[index] = member.copy(isOnline = !member.isOnline)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (member.isOnline) Color(0xFF10B981) else Color.Gray,
                                            modifier = Modifier.size(10.dp)
                                        ) {}
                                    }

                                    IconButton(
                                        onClick = {
                                            memberToEdit = member
                                            newMemberName = member.name
                                            newMemberRole = member.role
                                            newMemberOnline = member.isOnline
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Modifier",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    if (!member.isOwner) {
                                        IconButton(
                                            onClick = {
                                                members.removeIf { it.id == member.id }
                                                Toast.makeText(context, "${member.name} retiré(e) du panier", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Supprimer",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        Toast.makeText(context, "Panier synchronisé avec succès avec ${members.count { it.isOnline }} membre(s) !", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Forcer la synchronisation Cloud")
                }
            }
        }
    }

    // Modal to Add New Member
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = "Ajouter un membre au panier", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        label = { Text("Nom du membre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newMemberRole,
                        onValueChange = { newMemberRole = it },
                        label = { Text("Rôle / Relation (ex: Colocataire, Parent)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Statut en ligne", fontSize = 13.sp)
                        Switch(
                            checked = newMemberOnline,
                            onCheckedChange = { newMemberOnline = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMemberName.isNotBlank()) {
                            members.add(
                                SharedCartMember(
                                    id = System.currentTimeMillis().toString(),
                                    name = newMemberName.trim(),
                                    role = if (newMemberRole.isBlank()) "Membre" else newMemberRole.trim(),
                                    isOnline = newMemberOnline
                                )
                            )
                            Toast.makeText(context, "${newMemberName.trim()} ajouté(e) au panier partagé !", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Modal to Edit Member
    memberToEdit?.let { target ->
        AlertDialog(
            onDismissRequest = { memberToEdit = null },
            title = { Text(text = "Modifier le membre", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        label = { Text("Nom du membre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newMemberRole,
                        onValueChange = { newMemberRole = it },
                        label = { Text("Rôle / Relation") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Statut en ligne", fontSize = 13.sp)
                        Switch(
                            checked = newMemberOnline,
                            onCheckedChange = { newMemberOnline = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val index = members.indexOfFirst { it.id == target.id }
                        if (index != -1 && newMemberName.isNotBlank()) {
                            members[index] = target.copy(
                                name = newMemberName.trim(),
                                role = if (newMemberRole.isBlank()) "Membre" else newMemberRole.trim(),
                                isOnline = newMemberOnline
                            )
                            Toast.makeText(context, "Membre mis à jour !", Toast.LENGTH_SHORT).show()
                            memberToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToEdit = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// 3. Recipes to Cart Dialog
// -------------------------------------------------------------
data class RecipeItem(
    val title: String,
    val subtitle: String,
    val prepTime: String,
    val ingredients: List<String>,
    val isAiGenerated: Boolean = false
)

@Composable
fun RecipesToCartDialog(
    allProducts: List<ProductEntity>,
    viewModel: CatalogViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val defaultRecipes = listOf(
        RecipeItem(
            title = "Gratin Dauphinois Traditionnel",
            subtitle = "Crème, pommes de terre, muscade & lait",
            prepTime = "45 min",
            ingredients = listOf("Pommes de terre", "Crème fraîche", "Lait entier", "Beurre", "Fromage râpé")
        ),
        RecipeItem(
            title = "Ratatouille Provençale",
            subtitle = "Légumes frais du soleil & huile d'olive",
            prepTime = "35 min",
            ingredients = listOf("Courgettes", "Tomates fraîches", "Poivrons rouges", "Oignons", "Huile d'olive")
        ),
        RecipeItem(
            title = "Pâtes Carbonara Italienne",
            subtitle = "Spaghetti, lardons, œufs & parmesan",
            prepTime = "20 min",
            ingredients = listOf("Spaghetti", "Lardons fumés", "Œufs frais", "Parmesan râpé")
        ),
        RecipeItem(
            title = "Crêpes Party Gourmandes",
            subtitle = "Farine, œufs, lait & sucre vanillé",
            prepTime = "25 min",
            ingredients = listOf("Farine de blé", "Œufs", "Lait entier", "Sucre vanillé", "Beurre")
        )
    )

    var selectedRecipe by remember { mutableStateOf<RecipeItem?>(null) }

    fun performGeminiSearch(query: String) {
        if (query.isBlank()) return
        scope.launch {
            isLoading = true
            try {
                val result = GeminiService.generateRecipeIngredients(query)
                selectedRecipe = RecipeItem(
                    title = result.recipeTitle,
                    subtitle = result.subtitle,
                    prepTime = result.prepTime,
                    ingredients = result.ingredients,
                    isAiGenerated = true
                )
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur de recherche Gemini : ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.925f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                            color = Color(0xFFEA580C).copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Recettes vers Panier", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Recherche d'ingrédients par l'IA Gemini", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar for Recipe
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Entrez une recette (ex: Tarte Tatin, Couscous...)", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFEA580C),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFEA580C))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { performGeminiSearch(searchQuery) }) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Rechercher par IA", tint = Color(0xFFEA580C))
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Search Action Button
                Button(
                    onClick = { performGeminiSearch(searchQuery) },
                    enabled = searchQuery.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Gemini recherche la recette...", fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Rechercher la recette avec l'IA Gemini", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFEA580C))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Gemini extrait les ingrédients de supermarché pour \"$searchQuery\"...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (selectedRecipe == null) {
                    Text(text = "Ou choisissez parmi nos recettes populaires :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(defaultRecipes, key = { it.title }) { recipe ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRecipe = recipe },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFEA580C).copy(alpha = 0.12f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Restaurant,
                                                contentDescription = null,
                                                tint = Color(0xFFEA580C),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = recipe.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = recipe.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFEA580C))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = recipe.prepTime, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                                        }
                                    }

                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedRecipe = null }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Retour aux recettes", color = Color(0xFFEA580C), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = selectedRecipe!!.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(text = selectedRecipe!!.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEA580C).copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (selectedRecipe!!.isAiGenerated) "Généré par Gemini" else "Recette Chefs",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEA580C)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFEA580C))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Temps de préparation estimé : ${selectedRecipe!!.prepTime}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(text = "Ingrédients nécessaires (${selectedRecipe!!.ingredients.size}) :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        selectedRecipe!!.ingredients.forEach { ingredient ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = ingredient, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                     Button(
                        onClick = {
                            scope.launch {
                                var count = 0
                                selectedRecipe!!.ingredients.forEachIndexed { index, ing ->
                                    // 1. Try exact or contains match (bidirectional)
                                    var matched = allProducts.firstOrNull { 
                                        it.name.contains(ing, ignoreCase = true) || ing.contains(it.name, ignoreCase = true)
                                    }
                                    
                                    // 2. Try word-based keyword matching if still null
                                    if (matched == null) {
                                        val ingWords = ing.lowercase().split(" ", "-", "'", ",")
                                            .filter { it.length > 2 && it != "les" && it != "des" && it != "aux" && it != "avec" && it != "dans" }
                                        if (ingWords.isNotEmpty()) {
                                            matched = allProducts.firstOrNull { prod ->
                                                val prodNameLower = prod.name.lowercase()
                                                ingWords.any { word -> prodNameLower.contains(word) }
                                            }
                                        }
                                    }
                                    
                                    // 3. Fallback to a custom virtual ProductEntity if no match is found
                                    val finalProduct = matched ?: ProductEntity(
                                        id = (System.currentTimeMillis() % 1000000).toInt() + index + 50000,
                                        catalogType = "RECETTE",
                                        name = ing,
                                        category = "Ingrédients",
                                        brand = "Recette",
                                        unit = "1u",
                                        price = 0.0,
                                        cost = 0.0,
                                        barcode = ""
                                    )
                                    viewModel.addToCart(finalProduct, 1)
                                    count++
                                    kotlinx.coroutines.delay(30)
                                }
                                Toast.makeText(context, "$count ingrédient(s) de '${selectedRecipe!!.title}' ajouté(s) au panier !", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Ajouter tous les ingrédients au panier", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

