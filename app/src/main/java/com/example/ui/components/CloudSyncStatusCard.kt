package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CatalogViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SlateTextSecondary

@Composable
fun CloudSyncStatusCard(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val isCloudSynced by viewModel.isCloudSynced.collectAsState()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()
    val isSyncingNow by viewModel.isSyncingNow.collectAsState()
    val activeCatalog by viewModel.activeCatalog.collectAsState()
    val currentProducts by viewModel.currentProducts.collectAsState()

    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()
    val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsState()
    val isBackupInProgress by viewModel.isBackupInProgress.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }

    val statusColor by animateColorAsState(
        targetValue = if (isCloudSynced) EmeraldSuccess else Color(0xFFE65100),
        label = "statusColor"
    )

    val containerBg by animateColorAsState(
        targetValue = if (isCloudSynced) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
        label = "containerBg"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = statusColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Pulsing status indicator dot with Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSyncingNow) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = statusColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isCloudSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Indicator dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCloudSynced) "Synchronisé Firebase Firestore" else "Version Locale (Room SQLite)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = if (isCloudSynced)
                                "Catalogue $activeCatalog synchronisé avec Firebase • $lastSyncTimestamp"
                            else
                                "Mode Hors Ligne • ${currentProducts.size} articles dans Room SQLite",
                            fontSize = 12.sp,
                            color = SlateTextSecondary,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Action button to trigger resync or toggle mode
                    IconButton(
                        onClick = { viewModel.syncWithCloudNow() },
                        enabled = !isSyncingNow,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Synchroniser avec Firebase",
                            tint = BluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Surface(
                        onClick = { isExpanded = !isExpanded },
                        shape = CircleShape,
                        color = Color.Transparent
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Détails",
                            tint = SlateTextSecondary,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(18.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Statut de la source de données :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isCloudSynced) {
                        Text(
                            text = "• Connexion active à Firebase Firestore (Temps Réel)\n" +
                                    "• Sauvegarde automatique périodique : ${if (autoBackupEnabled) "Activée" else "Désactivée"}\n" +
                                    "• Dernière sauvegarde Firebase : $lastBackupTimestamp\n" +
                                    "• Catalogue actif : $activeCatalog (${currentProducts.size} articles synchronisés)",
                            fontSize = 11.sp,
                            color = SlateTextSecondary,
                            lineHeight = 16.sp
                        )
                    } else {
                        Text(
                            text = "• Utilisation de la base SQLite locale (mode hors ligne Room)\n" +
                                    "• Copie de secours Firebase conservée : $lastBackupTimestamp\n" +
                                    "• Vous pouvez effectuer des ventes et recherches sans connexion",
                            fontSize = 11.sp,
                            color = SlateTextSecondary,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.performFirebaseBackup() },
                            enabled = !isBackupInProgress,
                            modifier = Modifier.height(32.dp)
                        ) {
                            if (isBackupInProgress) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sauvegarde Firebase...", fontSize = 11.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sauvegarder sur Firebase",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        TextButton(
                            onClick = { viewModel.toggleSyncMode() },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isCloudSynced) Icons.Default.Storage else Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCloudSynced) "Mode Room Local" else "Reconnecter Firebase",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
