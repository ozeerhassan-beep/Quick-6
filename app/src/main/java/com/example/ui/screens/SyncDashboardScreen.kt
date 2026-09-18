package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CatalogViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.DreampriceColor
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IntermartColor
import com.example.ui.theme.SuperUColor
import com.example.ui.theme.WinnersColor
import com.example.util.CatalogSyncManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDashboardScreen(
    viewModel: CatalogViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val syncState by CatalogSyncManager.syncState.collectAsState()
    val isPeriodicEnabled by CatalogSyncManager.isPeriodicSyncEnabled.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState(initial = emptyList())
    val lastSummary by viewModel.lastImportSummary.collectAsState()
    val serverFiles by viewModel.serverCatalogFiles.collectAsState()
    val isServerSyncing by viewModel.isServerSyncing.collectAsState()
    val lastServerSyncTime by viewModel.lastServerSyncTime.collectAsState()

    val totalCatalogProducts = allProducts.size
    val dreampriceCount = allProducts.count { it.catalogType.equals("DREAMPRICE", ignoreCase = true) }
    val superUCount = allProducts.count { it.catalogType.equals("SUPER_U", ignoreCase = true) || it.catalogType.equals("SUPERU", ignoreCase = true) }
    val winnersCount = allProducts.count { it.catalogType.equals("WINNERS", ignoreCase = true) }
    val intermartCount = allProducts.count { it.catalogType.equals("INTERMART", ignoreCase = true) }
    val wayCount = allProducts.count { it.catalogType.equals("WAY", ignoreCase = true) }
    val loloCount = allProducts.count { it.catalogType.equals("LOLO", ignoreCase = true) }
    val kingSaversCount = allProducts.count { it.catalogType.equals("KING_SAVERS", ignoreCase = true) || it.catalogType.equals("KING_SAVER", ignoreCase = true) || it.catalogType.equals("KING SAVERS", ignoreCase = true) }
    val gsrCount = allProducts.count { it.catalogType.equals("GSR", ignoreCase = true) }

    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

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
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tableau de Synchronisation",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "WorkManager & Services d'arrière-plan",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            CatalogSyncManager.startDownloadAllFiles(context)
                            Toast.makeText(context, "Synchronisation immédiate lancée...", Toast.LENGTH_SHORT).show()
                        },
                        enabled = !syncState.isRunning
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rafraîchir",
                            tint = if (syncState.isRunning) Color.Gray else BluePrimary,
                            modifier = if (syncState.isRunning) Modifier.rotate(rotationAngle) else Modifier
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Hero Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (syncState.isRunning) BluePrimary.copy(alpha = 0.08f)
                        else if (syncState.isError) Color(0xFFDC2626).copy(alpha = 0.08f)
                        else EmeraldSuccess.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (syncState.isRunning) BluePrimary.copy(alpha = 0.4f)
                        else if (syncState.isError) Color(0xFFDC2626).copy(alpha = 0.4f)
                        else EmeraldSuccess.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (syncState.isRunning) BluePrimary
                                    else if (syncState.isError) Color(0xFFDC2626)
                                    else EmeraldSuccess,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (syncState.isRunning) {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .rotate(rotationAngle)
                                            )
                                        } else if (syncState.isError) {
                                            Icon(
                                                imageVector = Icons.Default.SyncProblem,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.CloudDone,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (syncState.isRunning) "Synchronisation en cours"
                                        else if (syncState.isError) "Erreur de synchronisation"
                                        else "Catalogues à jour",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (syncState.isRunning) "Téléchargement & intégration..."
                                        else "Persistance SQLite Room active",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (syncState.isRunning) BluePrimary.copy(alpha = 0.15f)
                                else if (syncState.isError) Color(0xFFDC2626).copy(alpha = 0.15f)
                                else EmeraldSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (syncState.isRunning) "ACTIF" else if (syncState.isError) "ERREUR" else "OPTIMAL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (syncState.isRunning) BluePrimary else if (syncState.isError) Color(0xFFDC2626) else EmeraldSuccess,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Progress Bar if running
                        if (syncState.isRunning) {
                            Column {
                                LinearProgressIndicator(
                                    progress = {
                                        if (syncState.totalSteps > 0) syncState.currentStep.toFloat() / syncState.totalSteps.toFloat()
                                        else 0.5f
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = BluePrimary,
                                    trackColor = BluePrimary.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = syncState.progressMessage.ifBlank { "Mise à jour des catalogues..." },
                                    fontSize = 12.sp,
                                    color = BluePrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Last Sync Time Info Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Dernière synchronisation réussie",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = if (syncState.lastSyncTimeFormatted.isNotBlank()) "Aujourd'hui à ${syncState.lastSyncTimeFormatted}"
                                        else if (syncState.lastSyncTimestamp > 0) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(syncState.lastSyncTimestamp))
                                        else "Effectuée à l'ouverture",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "$totalCatalogProducts articles",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Manual Sync Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    CatalogSyncManager.startDownloadAllFiles(context)
                                    Toast.makeText(context, "Synchronisation manuelle lancée...", Toast.LENGTH_SHORT).show()
                                },
                                enabled = !syncState.isRunning,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sync_now_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (syncState.isRunning) "Synchronisation..." else "Synchroniser Maintenant",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (syncState.isRunning) {
                                OutlinedButton(
                                    onClick = {
                                        CatalogSyncManager.cancelSync(context)
                                        Toast.makeText(context, "Annulation demandée", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Annuler", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // WorkManager Periodic Configuration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Synchronisation Périodique (WorkManager)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tâche d'arrière-plan toutes les 6 heures avec connexion réseau",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Switch(
                                checked = isPeriodicEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setPeriodicWorkManagerEnabled(context, enabled)
                                    Toast.makeText(
                                        context,
                                        if (enabled) "Synchronisation WorkManager activée (6h)" else "Synchronisation en arrière-plan désactivée",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BluePrimary
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )

                        // WorkManager Constraints Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NetworkCheck,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Réseau requis :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Text("Connecté (Wi-Fi / 4G)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = BluePrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Intervalle :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Text("6 heures", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Statut Job :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Text(if (isPeriodicEnabled) "Planifié" else "Inactif", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isPeriodicEnabled) EmeraldSuccess else Color.Gray)
                            }
                        }
                    }
                }
            }

            // Supermarket Catalog Breakdown
            item {
                Text(
                    text = "État des Catalogues Supermarchés",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            data class StoreStatusEntry(
                val code: String,
                val name: String,
                val brandColor: Color,
                val count: Int
            )

            val supermarkets = listOf(
                StoreStatusEntry("DREAMPRICE", "Dreamprice", DreampriceColor, dreampriceCount),
                StoreStatusEntry("SUPER_U", "Super U", SuperUColor, superUCount),
                StoreStatusEntry("WINNERS", "Winners", WinnersColor, winnersCount),
                StoreStatusEntry("INTERMART", "Intermart", IntermartColor, intermartCount),
                StoreStatusEntry("WAY", "Way Supermarket", Color(0xFF0D9488), wayCount),
                StoreStatusEntry("LOLO", "Lolo", Color(0xFF9333EA), loloCount),
                StoreStatusEntry("KING_SAVERS", "King Savers", Color(0xFFEA580C), kingSaversCount),
                StoreStatusEntry("GSR", "GSR", Color(0xFF2563EB), gsrCount)
            ).sortedBy { it.name }

            items(supermarkets, key = { it.code }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = item.brandColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = item.brandColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = item.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (item.count > 0) "${item.count} produits indexés • Prêt" else "En attente de téléchargement",
                                    fontSize = 11.sp,
                                    color = if (item.count > 0) EmeraldSuccess else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (item.count > 0) EmeraldSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (item.count > 0) "✓ Synchronisé" else "Non chargé",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.count > 0) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Summary / Architecture Info Box
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = BluePrimary.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Architecture de synchronisation hybride",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Les fichiers Drive sont téléchargés via un service de premier plan persistant.\n2. Les données sont insérées dans la base SQLite locale Room avec détection d'alertes de prix.\n3. WorkManager exécute des rafraîchissements périodiques automatiques en tâche de fond.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // App Identity & Signatures (Package ID, SHA-1, SHA-256)
            item {
                val (pkgId, sha1Cert, sha256Cert) = remember(context) {
                    var pkg = context.packageName
                    var s1 = "N/A"
                    var s256 = "N/A"
                    try {
                        val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            val pkgInfo = context.packageManager.getPackageInfo(pkg, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                            pkgInfo.signingInfo?.let {
                                if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val pkgInfo = context.packageManager.getPackageInfo(pkg, android.content.pm.PackageManager.GET_SIGNATURES)
                            @Suppress("DEPRECATION")
                            pkgInfo.signatures
                        }

                        if (signatures != null && signatures.isNotEmpty()) {
                            val cert = signatures[0].toByteArray()
                            s1 = java.security.MessageDigest.getInstance("SHA-1").digest(cert).joinToString(":") { "%02X".format(it) }
                            s256 = java.security.MessageDigest.getInstance("SHA-256").digest(cert).joinToString(":") { "%02X".format(it) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    Triple(pkg, s1, s256)
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Informations de Signature & Package",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Package ID:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = pkgId,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "SHA-1 Certificate:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = sha1Cert,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = "SHA-256 Certificate (SHA-25):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = sha256Cert,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
