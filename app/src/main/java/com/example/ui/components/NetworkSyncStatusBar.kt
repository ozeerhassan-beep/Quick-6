package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CatalogViewModel
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess

@Composable
fun NetworkSyncStatusBar(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier
) {
    val isAutoSyncEnabled by viewModel.isFirestoreAutoSyncEnabled.collectAsState()
    val isSyncing by viewModel.isFirestoreSyncing.collectAsState()
    val firestoreStatus by viewModel.firestoreSyncStatus.collectAsState()

    var showDetailExpanded by remember { mutableStateOf(false) }

    val barBgColor by animateColorAsState(
        targetValue = if (isAutoSyncEnabled) EmeraldSuccess.copy(alpha = 0.08f) else Color(0xFFF59E0B).copy(alpha = 0.10f),
        animationSpec = tween(400),
        label = "barBg"
    )

    val barBorderColor by animateColorAsState(
        targetValue = if (isAutoSyncEnabled) EmeraldSuccess.copy(alpha = 0.25f) else Color(0xFFF59E0B).copy(alpha = 0.35f),
        animationSpec = tween(400),
        label = "barBorder"
    )

    val indicatorColor = if (isAutoSyncEnabled) EmeraldSuccess else Color(0xFFD97706)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("sync_status_bar"),
        shape = RoundedCornerShape(12.dp),
        color = barBgColor,
        border = BorderStroke(1.dp, barBorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDetailExpanded = !showDetailExpanded }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Status Dot + Mode Label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Pulsing/Solid Indicator Dot or Spinner
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = BluePrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = indicatorColor,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isAutoSyncEnabled) "MODE EN LIGNE" else "MODE HORS-LIGNE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = indicatorColor,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAutoSyncEnabled) "• Room + Firestore" else "• Room SQLite",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Text(
                            text = if (isSyncing) "Synchronisation en cours..." else if (isAutoSyncEnabled) "Auto-synchro Cloud active" else "Économie de données (Local seul)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }

                // Right side: Quick Toggle Switch & Manual Sync Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isAutoSyncEnabled) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = indicatorColor.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { viewModel.forceSyncCartToFirestore() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync manuelle",
                                    tint = indicatorColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Synchro",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = indicatorColor
                                )
                            }
                        }
                    }

                    Switch(
                        checked = isAutoSyncEnabled,
                        onCheckedChange = { viewModel.setFirestoreAutoSyncEnabled(it) },
                        modifier = Modifier
                            .size(width = 38.dp, height = 24.dp)
                            .testTag("auto_sync_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldSuccess,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFD1D5DB)
                        )
                    )
                }
            }

            // Expanded Detail Card
            AnimatedVisibility(visible = showDetailExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp, top = 2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isAutoSyncEnabled) Icons.Default.CloudDone else Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = indicatorColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAutoSyncEnabled) "Synchronisation Firestore Active" else "Mode Hors-Ligne Room SQLite",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = if (isAutoSyncEnabled) "Cloud Actif" else "Hors-Ligne",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = indicatorColor
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isAutoSyncEnabled)
                                    "Toutes les modifications du panier et commandes sont synchronisées en direct avec Cloud Firestore et sauvegardées dans la base locale Room SQLite."
                                else
                                    "L'application travaille exclusivement avec la base de données locale Room SQLite pour économiser les données mobiles. Utilisez le bouton 'Synchro' pour envoyer manuellement.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 14.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = firestoreStatus,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = { viewModel.forceSyncCartToFirestore() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Rafraîchir",
                                        tint = BluePrimary,
                                        modifier = Modifier.size(14.dp)
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
