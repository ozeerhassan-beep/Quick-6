package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.FirestoreUserService
import com.example.data.UserProfile
import com.example.ui.CatalogViewModel
import com.example.ui.SubscriptionTier
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import kotlinx.coroutines.launch

private enum class UserFilterCategory {
    ALL, FREE, PRO, ULTRA, ADMINS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: CatalogViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userRole by viewModel.userRole.collectAsState()
    val currentAdminEmail by viewModel.googleAccountEmail.collectAsState()

    var usersList by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(UserFilterCategory.ALL) }

    var userToEdit by remember { mutableStateOf<UserProfile?>(null) }
    var isUpdatingTier by remember { mutableStateOf(false) }

    fun refreshUsers() {
        scope.launch {
            isLoading = true
            val fetched = FirestoreUserService.fetchAllUsers()
            // If empty (e.g. initial offline setup), ensure active accounts / admin are displayed
            if (fetched.isEmpty()) {
                val fallbackList = listOf(
                    UserProfile(
                        uid = "admin-primary",
                        email = currentAdminEmail ?: "admin@kwickart.mu",
                        displayName = "Super Administrateur",
                        isAdmin = true,
                        userRole = "ADMIN",
                        subscriptionTier = "ULTRA",
                        userTier = "ULTRA"
                    )
                )
                usersList = fallbackList
            } else {
                usersList = fetched
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshUsers()
    }

    val filteredUsers = remember(usersList, searchQuery, selectedFilter) {
        usersList.filter { user ->
            val matchesQuery = searchQuery.isBlank() ||
                    user.effectiveEmail.contains(searchQuery, ignoreCase = true) ||
                    user.displayName.contains(searchQuery, ignoreCase = true) ||
                    user.uid.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                UserFilterCategory.ALL -> true
                UserFilterCategory.FREE -> user.effectiveTier == "FREE"
                UserFilterCategory.PRO -> user.effectiveTier == "PRO"
                UserFilterCategory.ULTRA -> user.effectiveTier == "ULTRA"
                UserFilterCategory.ADMINS -> user.effectiveRole == "ADMIN"
            }

            matchesQuery && matchesFilter
        }
    }

    val totalCount = usersList.size
    val freeCount = usersList.count { it.effectiveTier == "FREE" }
    val proCount = usersList.count { it.effectiveTier == "PRO" }
    val ultraCount = usersList.count { it.effectiveTier == "ULTRA" }
    val adminCount = usersList.count { it.effectiveRole == "ADMIN" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SupervisorAccount,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tableau de Bord Admin",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Gestion des Utilisateurs & Abonnements",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_dashboard_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshUsers() },
                        modifier = Modifier.testTag("admin_refresh_users_btn")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
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
            // Stats Overview Carousel Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminStatMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Total",
                    count = totalCount,
                    icon = Icons.Default.Group,
                    color = MaterialTheme.colorScheme.primary
                )
                AdminStatMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Free",
                    count = freeCount,
                    icon = Icons.Default.PhoneAndroid,
                    color = Color(0xFF64748B)
                )
                AdminStatMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Pro",
                    count = proCount,
                    icon = Icons.Default.Devices,
                    color = BluePrimary
                )
                AdminStatMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "Ultra",
                    count = ultraCount,
                    icon = Icons.Default.Star,
                    color = Color(0xFF7C3AED)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .testTag("admin_user_search_field"),
                placeholder = { Text("Rechercher par email, nom ou UID...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(UserFilterCategory.values()) { category ->
                    val isSelected = selectedFilter == category
                    val label = when (category) {
                        UserFilterCategory.ALL -> "Tous ($totalCount)"
                        UserFilterCategory.FREE -> "Free ($freeCount)"
                        UserFilterCategory.PRO -> "Pro ($proCount)"
                        UserFilterCategory.ULTRA -> "Ultra ($ultraCount)"
                        UserFilterCategory.ADMINS -> "Admins ($adminCount)"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = category },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            // User List / Loading / Empty
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chargement des utilisateurs Firestore...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Aucun utilisateur ne correspond à \"$searchQuery\"" else "Aucun utilisateur trouvé.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredUsers, key = { it.uid.ifBlank { it.effectiveEmail } }) { user ->
                        AdminUserCard(
                            user = user,
                            onEditTier = { userToEdit = user }
                        )
                    }
                }
            }
        }
    }

    // Edit Tier Modal Dialog
    if (userToEdit != null) {
        val targetUser = userToEdit!!
        EditSubscriptionTierDialog(
            user = targetUser,
            isUpdating = isUpdatingTier,
            onDismiss = { if (!isUpdatingTier) userToEdit = null },
            onConfirmTierChange = { newTier, newRole ->
                scope.launch {
                    isUpdatingTier = true
                    val success = FirestoreUserService.updateUserSubscriptionTier(
                        targetDocId = targetUser.uid.ifBlank { targetUser.effectiveEmail },
                        newTier = newTier.name,
                        newRole = newRole
                    )

                    if (success) {
                        Toast.makeText(
                            context,
                            "Abonnement mis à jour vers ${newTier.name} pour ${targetUser.effectiveEmail}",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Update in local state list
                        usersList = usersList.map { u ->
                            if (u.uid == targetUser.uid || u.effectiveEmail == targetUser.effectiveEmail) {
                                u.copy(
                                    subscriptionTier = newTier.name,
                                    userTier = newTier.name,
                                    userRole = newRole ?: u.userRole,
                                    isAdmin = newRole?.equals("ADMIN", ignoreCase = true) ?: u.isAdmin
                                )
                            } else u
                        }
                        userToEdit = null
                    } else {
                        Toast.makeText(
                            context,
                            "Échec de mise à jour Firestore. Vérifiez votre connexion et vos règles de sécurité.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    isUpdatingTier = false
                }
            }
        )
    }
}

@Composable
private fun AdminStatMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    icon: ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = count.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AdminUserCard(
    user: UserProfile,
    onEditTier: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val effectiveTier = user.effectiveTier
    val effectiveRole = user.effectiveRole

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_user_card_${user.uid}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // User Avatar and Names
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val avatarBg = when (effectiveTier) {
                        "ULTRA" -> Color(0xFF7C3AED)
                        "PRO" -> BluePrimary
                        else -> Color(0xFF64748B)
                    }

                    Surface(
                        shape = CircleShape,
                        color = avatarBg,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (user.displayName.ifBlank { user.effectiveEmail }).take(1).uppercase(),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.displayName.ifBlank { "Utilisateur" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = user.effectiveEmail,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Subscription Visual Badge with Color-Coded Icon
                SubscriptionVisualBadge(tier = effectiveTier)
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            Spacer(modifier = Modifier.height(8.dp))

            // Details and Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Roles & Device Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (effectiveRole == "ADMIN") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "ADMIN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                    }

                    // UID chip copyable
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(user.uid))
                            Toast.makeText(context, "UID copié : ${user.uid.take(10)}...", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(
                            text = "UID: ${user.uid.take(8)}...",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Edit Tier Action Button
                OutlinedButton(
                    onClick = onEditTier,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("edit_tier_btn_${user.uid}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modifier Tier", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * Visual badge to easily distinguish Free, Pro, and Ultra tiers with color-coded icons
 */
@Composable
fun SubscriptionVisualBadge(
    tier: String,
    modifier: Modifier = Modifier
) {
    val (badgeBg, contentColor, icon, label) = when (tier.uppercase()) {
        "ULTRA" -> Quadruple(
            Color(0xFF7C3AED).copy(alpha = 0.12f),
            Color(0xFF7C3AED),
            Icons.Default.Star,
            "ULTRA"
        )
        "PRO" -> Quadruple(
            BluePrimary.copy(alpha = 0.12f),
            BluePrimary,
            Icons.Default.Devices,
            "PRO"
        )
        else -> Quadruple(
            Color(0xFF64748B).copy(alpha = 0.12f),
            Color(0xFF64748B),
            Icons.Default.PhoneAndroid,
            "FREE"
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = badgeBg,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun EditSubscriptionTierDialog(
    user: UserProfile,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onConfirmTierChange: (newTier: SubscriptionTier, newRole: String?) -> Unit
) {
    var selectedTier by remember {
        mutableStateOf(
            when (user.effectiveTier.uppercase()) {
                "ULTRA" -> SubscriptionTier.ULTRA
                "PRO" -> SubscriptionTier.PRO
                else -> SubscriptionTier.FREE
            }
        )
    }
    var makeAdmin by remember { mutableStateOf(user.effectiveRole == "ADMIN") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("edit_subscription_tier_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Modifier l'Abonnement",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = user.effectiveEmail,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Choisir le niveau d'abonnement :",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tier Option Cards
                SubscriptionTier.values().forEach { tier ->
                    val isSelected = selectedTier == tier
                    val (tierColor, icon, title, desc) = when (tier) {
                        SubscriptionTier.FREE -> Quadruple(
                            Color(0xFF64748B),
                            Icons.Default.PhoneAndroid,
                            "FREE (Gratuit)",
                            "1 Appareil lié • Panier local • Fonctionnalités de base"
                        )
                        SubscriptionTier.PRO -> Quadruple(
                            BluePrimary,
                            Icons.Default.Devices,
                            "PRO (Professionnel)",
                            "3 Appareils • Sync Cloud multi-appareils • Bénéfices & Marges"
                        )
                        SubscriptionTier.ULTRA -> Quadruple(
                            Color(0xFF7C3AED),
                            Icons.Default.Star,
                            "ULTRA (Illimité)",
                            "Appareils illimités • Mode hors-ligne temps réel • Analytics"
                        )
                    }

                    val cardBg = if (isSelected) tierColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    val borderColor = if (isSelected) tierColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = cardBg,
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedTier = tier }
                            .testTag("tier_option_${tier.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedTier = tier }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = tierColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) tierColor else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Admin Role Switch
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Rôle Administrateur", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Accès complet de supervision", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = makeAdmin,
                            onCheckedChange = { makeAdmin = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFD97706)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isUpdating
                    ) {
                        Text("Annuler")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val role = if (makeAdmin) "ADMIN" else "USER"
                            onConfirmTierChange(selectedTier, role)
                        },
                        enabled = !isUpdating,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("confirm_tier_update_button")
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enregistrement...")
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Appliquer & Sync")
                        }
                    }
                }
            }
        }
    }
}
