package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import com.example.data.SaleRecordEntity
import com.example.ui.CatalogViewModel
import com.example.ui.SubscriptionTier
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SlateTextSecondary
import com.example.util.AppFingerprintUtils
import com.example.util.AppLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: CatalogViewModel,
    onBack: (() -> Unit)? = null,
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToAdminDashboard: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentLang by viewModel.appLanguage.collectAsState()

    // Auth & Account State
    val isSignedIn by viewModel.isSignedInWithGoogle.collectAsState()
    val savedEmail by viewModel.googleAccountEmail.collectAsState()
    val savedName by viewModel.googleAccountName.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val userTier by viewModel.userTier.collectAsState()
    val firebaseUserId by viewModel.firebaseUserId.collectAsState()
    val firebaseAuthProvider by viewModel.firebaseAuthProvider.collectAsState()

    // Order History State
    val saleRecords by viewModel.saleRecords.collectAsState()

    // Screen Sub-tab State (0: Commandes / Order History, 1: Détails du Compte / Account Details)
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStoreFilter by remember { mutableStateOf("TOUS") }

    // Dialogs
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val currentUserName = remember(isSignedIn, savedName, savedEmail) {
        if (isSignedIn && !savedName.isNullOrBlank()) {
            savedName!!
        } else if (isSignedIn && !savedEmail.isNullOrBlank()) {
            savedEmail!!.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            "Invité Kwic-Kart"
        }
    }

    val displayEmail = remember(isSignedIn, savedEmail) {
        if (isSignedIn && !savedEmail.isNullOrBlank()) savedEmail!! else "Non connecté"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BluePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Mon Profil Utilisateur",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isSignedIn) "Compte & Historique d'achats" else "Mode Invité",
                                fontSize = 11.sp,
                                color = SlateTextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("user_profile_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Retour",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    if (isSignedIn) {
                        IconButton(
                            onClick = {
                                viewModel.forceSyncCartToFirestore()
                                Toast.makeText(context, "Profil et données synchronisés avec Firestore", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("user_profile_sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Synchroniser avec Firestore",
                                tint = BluePrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // If not signed in, display Sign-In Prompt Hero Card
            if (!isSignedIn) {
                SignedOutProfileBanner(
                    onLoginClick = {
                        if (onNavigateToLogin != null) {
                            onNavigateToLogin()
                        }
                    }
                )
            } else {
                // ================= USER HEADER CARD =================
                UserProfileHeaderCard(
                    userName = currentUserName,
                    userEmail = displayEmail,
                    userRole = userRole,
                    userTier = userTier.name,
                    onEditProfile = { showEditProfileDialog = true },
                    onSignOut = {
                        viewModel.signOutUser()
                        Toast.makeText(context, "Déconnexion réussie", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Tabs between Order History and Account Details
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = BluePrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Historique (${saleRecords.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_order_history")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Détails du Compte",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_account_details")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // TAB 0: ORDER HISTORY
                if (selectedTab == 0) {
                    OrderHistorySection(
                        saleRecords = saleRecords,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedStoreFilter = selectedStoreFilter,
                        onStoreFilterSelect = { selectedStoreFilter = it },
                        onReorderItem = { record ->
                            viewModel.reorderSaleRecord(record)
                            Toast.makeText(context, "${record.productName} ajouté au panier !", Toast.LENGTH_SHORT).show()
                        },
                        onReorderAll = { records ->
                            viewModel.reorderMultipleSaleRecords(records)
                            Toast.makeText(context, "${records.size} article(s) réajouté(s) au panier !", Toast.LENGTH_SHORT).show()
                        },
                        onClearAllHistory = { showClearHistoryConfirm = true }
                    )
                }
                // TAB 1: ACCOUNT DETAILS
                else {
                    AccountDetailsSection(
                        viewModel = viewModel,
                        userEmail = displayEmail,
                        userName = currentUserName,
                        userRole = userRole,
                        firebaseUid = firebaseUserId ?: "fb_local_${currentUserName.lowercase()}",
                        authProvider = firebaseAuthProvider,
                        onEditProfile = { showEditProfileDialog = true },
                        onNavigateToAdminDashboard = onNavigateToAdminDashboard
                    )
                }
            }
        }
    }

    // Confirm Clear Order History Dialog
    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Effacer l'historique ?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Text(
                    text = "Voulez-vous supprimer tout l'historique de vos achats enregistrés ? Cette action est irréversible.",
                    fontSize = 13.sp,
                    color = SlateTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearOrderHistory()
                        showClearHistoryConfirm = false
                        Toast.makeText(context, "Historique des commandes effacé.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Effacer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Edit Profile Info Dialog
    if (showEditProfileDialog) {
        var newDisplayName by remember { mutableStateOf(currentUserName) }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = BluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Modifier le Profil", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Mettez à jour le nom affiché sur votre compte :",
                        fontSize = 13.sp,
                        color = SlateTextSecondary
                    )
                    OutlinedTextField(
                        value = newDisplayName,
                        onValueChange = { newDisplayName = it },
                        label = { Text("Nom complet") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Remarque : Les modifications du document Firestore sont soumises aux permissions d'Administration.",
                                fontSize = 11.sp,
                                color = SlateTextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveUserCredentials(displayEmail, "oauth_session", newDisplayName)
                        showEditProfileDialog = false
                        Toast.makeText(context, "Profil mis à jour !", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Text("Enregistrer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

// ================= HERO HEADER CARD =================
@Composable
fun UserProfileHeaderCard(
    userName: String,
    userEmail: String,
    userRole: String,
    userTier: String,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = BluePrimary,
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF0F5132), // Deep Luxury Emerald
                            Color(0xFF10B981), // Vibrant Mint/Emerald
                            Color(0xFF047857)  // Rich deep forest green
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(54.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BluePrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = "Vérifié",
                                    tint = Color(0xFF6EE7B7),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = userEmail,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    // Logout Button
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .testTag("user_profile_signout_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Déconnexion",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Roles & Tier Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Admin or User Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (userRole == "ADMIN") Color(0xFFF59E0B) else Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (userRole == "ADMIN") Icons.Default.Star else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (userRole == "ADMIN") Color.Black else Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (userRole == "ADMIN") "ADMINISTRATEUR" else "MEMBRE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (userRole == "ADMIN") Color.Black else Color.White
                            )
                        }
                    }

                    // Subscription Tier Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFCD34D),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = userTier.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = onEditProfile,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.testTag("edit_profile_trigger")
                    ) {
                        Text(
                            text = "Éditer le nom",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ================= ORDER HISTORY SECTION =================
@Composable
fun OrderHistorySection(
    saleRecords: List<SaleRecordEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedStoreFilter: String,
    onStoreFilterSelect: (String) -> Unit,
    onReorderItem: (SaleRecordEntity) -> Unit,
    onReorderAll: (List<SaleRecordEntity>) -> Unit,
    onClearAllHistory: () -> Unit
) {
    // Filtered records based on store selection and search query
    val filteredRecords = remember(saleRecords, searchQuery, selectedStoreFilter) {
        saleRecords.filter { record ->
            val matchesStore = selectedStoreFilter == "TOUS" || record.catalogType.equals(selectedStoreFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    record.productName.contains(searchQuery, ignoreCase = true) ||
                    record.category.contains(searchQuery, ignoreCase = true) ||
                    record.catalogType.contains(searchQuery, ignoreCase = true)
            matchesStore && matchesSearch
        }
    }

    val totalSpent = remember(filteredRecords) { filteredRecords.sumOf { it.totalPrice } }
    val totalSavings = remember(filteredRecords) { filteredRecords.sumOf { it.totalProfit } }
    val totalArticles = remember(filteredRecords) { filteredRecords.sumOf { it.quantity } }

    val storeList = remember(saleRecords) {
        val stores = saleRecords.map { it.catalogType.uppercase().trim() }.distinct()
        listOf("TOUS") + (if (stores.isEmpty()) listOf("WINNERS", "SUPER_U", "INTERMART", "DREAMPRICE") else stores)
    }

    // Group records by Date string (e.g. "12 Septembre 2026")
    val groupedByDate = remember(filteredRecords) {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH)
        filteredRecords.groupBy { record ->
            sdf.format(Date(record.timestamp))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search & Filter Header
        item(key = "order_search_bar") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_order_history_input"),
                    placeholder = { Text("Rechercher dans vos achats...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateTextSecondary) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Store Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(storeList) { store ->
                        val isSelected = selectedStoreFilter == store
                        FilterChip(
                            selected = isSelected,
                            onClick = { onStoreFilterSelect(store) },
                            label = {
                                Text(
                                    text = if (store == "TOUS") "Tous" else store,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BluePrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Summary Metrics Card
                if (saleRecords.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Achats", fontSize = 11.sp, color = SlateTextSecondary)
                                Text("${filteredRecords.size} ($totalArticles art.)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Divider(modifier = Modifier.height(24.dp).width(1.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Dépensé", fontSize = 11.sp, color = SlateTextSecondary)
                                Text("Rs ${String.format("%.2f", totalSpent)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                            }
                            Divider(modifier = Modifier.height(24.dp).width(1.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Économies", fontSize = 11.sp, color = SlateTextSecondary)
                                Text("Rs ${String.format("%.2f", totalSavings)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                            }
                        }
                    }
                }
            }
        }

        // Action Row: Reorder All & Clear History
        if (filteredRecords.isNotEmpty()) {
            item(key = "order_action_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onReorderAll(filteredRecords) },
                        modifier = Modifier.testTag("reorder_all_purchases_button")
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp), tint = BluePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Réajouter tout au panier", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                    }

                    TextButton(
                        onClick = onClearAllHistory,
                        modifier = Modifier.testTag("clear_order_history_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Effacer", fontSize = 12.sp, color = Color(0xFFEF4444))
                    }
                }
            }
        }

        // Empty state when no records exist
        if (saleRecords.isEmpty()) {
            item(key = "empty_order_history") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(70.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = SlateTextSecondary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Aucune commande dans l'historique",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Lorsque vous validez votre panier d'achats, vos commandes s'afficheront automatiquement ici avec le détail des économies réalisées.",
                            fontSize = 12.sp,
                            color = SlateTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        } else if (filteredRecords.isEmpty()) {
            item(key = "empty_filtered_orders") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune commande ne correspond aux filtres sélectés.",
                        fontSize = 13.sp,
                        color = SlateTextSecondary
                    )
                }
            }
        } else {
            // Render Grouped Orders by Date
            groupedByDate.forEach { (dateStr, recordsOnDate) ->
                item(key = "date_header_$dateStr") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BluePrimary.copy(alpha = 0.08f),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dateStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                            Text(
                                text = "${recordsOnDate.size} article(s)",
                                fontSize = 11.sp,
                                color = SlateTextSecondary
                            )
                        }
                    }
                }

                items(recordsOnDate, key = { "sale_${it.id}_${it.timestamp}_${it.productName}" }) { record ->
                    OrderRecordCard(
                        record = record,
                        onReorder = { onReorderItem(record) }
                    )
                }
            }
        }
    }
}

// Single Order Record Item Card
@Composable
fun OrderRecordCard(
    record: SaleRecordEntity,
    onReorder: () -> Unit
) {
    val sdfTime = remember { SimpleDateFormat("HH:mm", Locale.FRENCH) }
    val timeStr = remember(record.timestamp) { sdfTime.format(Date(record.timestamp)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = BluePrimary.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.productName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = record.catalogType,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Quantité : ${record.quantity}  •  À $timeStr",
                        fontSize = 11.sp,
                        color = SlateTextSecondary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Rs ${String.format("%.2f", record.totalPrice)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BluePrimary
                    )
                }

                if (record.totalProfit > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Économisé: Rs ${String.format("%.2f", record.totalProfit)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onReorder,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BluePrimary.copy(alpha = 0.1f))
                    .testTag("reorder_item_${record.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.AddShoppingCart,
                    contentDescription = "Réajouter",
                    tint = BluePrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ================= ACCOUNT DETAILS SECTION =================
@Composable
fun AccountDetailsSection(
    viewModel: CatalogViewModel,
    userEmail: String,
    userName: String,
    userRole: String,
    firebaseUid: String,
    authProvider: String,
    onEditProfile: () -> Unit,
    onNavigateToAdminDashboard: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentDeviceId = remember(context) { AppFingerprintUtils.getSha1Fingerprint(context) }
    val currentLang by viewModel.appLanguage.collectAsState()
    val isAutoSyncEnabled by viewModel.isFirestoreAutoSyncEnabled.collectAsState()
    val userTier by viewModel.userTier.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Subscription Tier & Plan Card (Admin-only modification)
        item(key = "acc_subscription_tier") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = when (userTier) {
                                    SubscriptionTier.FREE -> Color(0xFF64748B)
                                    SubscriptionTier.PRO -> BluePrimary
                                    SubscriptionTier.ULTRA -> Color(0xFF7C3AED)
                                },
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Abonnement du Profil", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (userRole == "ADMIN") "Gestion des profils d'accès" else "Géré par l'administrateur",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Badge
                        val badgeBg = when (userTier) {
                            SubscriptionTier.FREE -> Color(0xFF64748B)
                            SubscriptionTier.PRO -> BluePrimary
                            SubscriptionTier.ULTRA -> Color(0xFF7C3AED)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, badgeBg.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = userTier.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = badgeBg,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Admin vs User Status Banner
                    if (userRole == "ADMIN") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Mode Administrateur : Vous avez l'autorisation de changer le niveau de ce profil.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF92400E)
                                    )
                                }
                                if (onNavigateToAdminDashboard != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = onNavigateToAdminDashboard,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("open_admin_dashboard_from_profile_btn")
                                    ) {
                                        Icon(Icons.Default.SupervisorAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Tableau de Bord Admin (Tous les Utilisateurs)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Seul un administrateur peut modifier le niveau d'abonnement (Free / Pro / Ultra) de votre profil.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (userRole == "ADMIN") "Sélectionner un plan pour ce compte :" else "Niveaux d'accès au système :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SubscriptionTier.values().forEach { tier ->
                            val isSelected = userTier == tier
                            val tierColor = when (tier) {
                                SubscriptionTier.FREE -> Color(0xFF64748B)
                                SubscriptionTier.PRO -> BluePrimary
                                SubscriptionTier.ULTRA -> Color(0xFF7C3AED)
                            }
                            val cardBg = if (isSelected) tierColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            val borderColor = if (isSelected) tierColor else MaterialTheme.colorScheme.outlineVariant

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(cardBg, RoundedCornerShape(12.dp))
                                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (userRole == "ADMIN") {
                                            viewModel.setUserTier(tier, forceAdmin = true)
                                            Toast.makeText(context, "Profil mis à jour vers : ${tier.name} (Admin)", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Action refusée : Seul un administrateur peut modifier le niveau d'abonnement (Free / Pro / Ultra).",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tier.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) tierColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (userRole != "ADMIN" && !isSelected) {
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (tier) {
                                        SubscriptionTier.FREE -> "Gratuit\n1 Appareil"
                                        SubscriptionTier.PRO -> "Pro\n3 Appareils"
                                        SubscriptionTier.ULTRA -> "Ultra\nIllimité"
                                    },
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    AccountDetailRow(
                        label = "Statut Privilège",
                        value = if (userRole == "ADMIN") "Superviseur Admin (Modification autorisée)" else "Verrouillé (Admin uniquement)"
                    )
                }
            }
        }
        // Account Identity Card
        item(key = "acc_identity") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Informations d'Identité", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AccountDetailRow(label = "Nom Complet", value = userName)
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    AccountDetailRow(label = "Adresse Email", value = userEmail)
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    AccountDetailRow(label = "Rôle Firestore", value = if (userRole == "ADMIN") "Administrateur (Accès complet)" else "Utilisateur Standard")
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    AccountDetailRow(label = "Fournisseur Auth", value = authProvider)
                }
            }
        }

        // Security & Device Card
        item(key = "acc_security") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhonelinkLock, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sécurité & Appareil Bindé", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AccountDetailRow(label = "Identifiant Appareil (UUID)", value = currentDeviceId)
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    AccountDetailRow(label = "Firebase UID", value = firebaseUid)
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    AccountDetailRow(label = "Synchronisation Firestore", value = if (isAutoSyncEnabled) "Automatique (Temps Réel)" else "Manuelle")
                    Divider(modifier = Modifier.padding(vertical = 6.dp))
                    AccountDetailRow(label = "Langue de l'application", value = currentLang.displayName)
                }
            }
        }

        // Action Buttons Card
        item(key = "acc_actions") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onEditProfile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("account_edit_profile_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Modifier mes informations", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.forceSyncCartToFirestore()
                        Toast.makeText(context, "Données de compte synchronisées avec Firestore", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("account_sync_now_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp), tint = BluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Synchroniser avec le Cloud", fontWeight = FontWeight.Bold, color = BluePrimary)
                }
            }
        }
    }
}

@Composable
fun AccountDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = SlateTextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

// Signed-out Banner Component
@Composable
fun SignedOutProfileBanner(onLoginClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_kwic_kart_logo_1789205313486),
                            contentDescription = "Kwic-Kart Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Connectez-vous à votre Compte",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Pour consulter votre historique de commandes, vos économies réalisées et vos informations personnelles de compte, connectez-vous dès maintenant.",
                    fontSize = 13.sp,
                    color = SlateTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("signed_out_profile_login_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Se Connecter / S'inscrire", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
