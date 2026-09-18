package com.example.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.screens.GoogleBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SlateTextSecondary
import com.example.util.HeatLevel
import com.example.util.RegionDensityInfo
import com.example.util.UserDensityManager

/**
 * Interactive Counter Badge / Chip displayed in the Catalogue Top Bar or Header.
 * Displays live count: "📍 14 actifs à proximité" with a pulsing live beacon.
 */
@Composable
fun ActiveUserDensityChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val count by UserDensityManager.activeNearbyUsers.collectAsState()
    val heatLevel by UserDensityManager.currentHeatLevel.collectAsState()

    // Smooth pulsing animation for live status beacon
    val infiniteTransition = rememberInfiniteTransition(label = "beacon_pulse")
    val beaconScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_scale"
    )

    val beaconColor = Color(heatLevel.colorHex)

    Surface(
        modifier = modifier
            .testTag("active_user_density_chip")
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = beaconColor.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, beaconColor.copy(alpha = 0.35f)),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Pulsing live activity indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(beaconScale)
                    .clip(CircleShape)
                    .background(beaconColor)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = beaconColor,
                modifier = Modifier.size(13.dp)
            )

            Spacer(modifier = Modifier.width(3.dp))

            Text(
                text = "$count actifs",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Detailed Pop-Up Dialog showing localized breakdown and heat indicator
 * without revealing individual identities or raw GPS coordinates.
 */
@Composable
fun ActiveUserDensityDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val nearbyUsers by UserDensityManager.activeNearbyUsers.collectAsState()
    val currentRegion by UserDensityManager.currentRegionName.collectAsState()
    val currentGridCell by UserDensityManager.currentGridCell.collectAsState()
    val heatLevel by UserDensityManager.currentHeatLevel.collectAsState()
    val regionalBreakdown by UserDensityManager.regionalBreakdown.collectAsState()
    val hasPermission by UserDensityManager.hasLocationPermission.collectAsState()
    val isTrackingActive by UserDensityManager.isTrackingActive.collectAsState()

    var showPrivacyDetails by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            UserDensityManager.startTracking(context)
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(heatLevel.colorHex).copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = Color(heatLevel.colorHex),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Affluence & Activité",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Densité en direct à proximité",
                                fontSize = 12.sp,
                                color = SlateTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = SlateTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Heat & Density Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(heatLevel.colorHex).copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = GoogleBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentRegion,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Heat Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(heatLevel.colorHex).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Affluence ${heatLevel.label}",
                                    color = Color(heatLevel.colorHex),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Large count number with description
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$nearbyUsers",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(heatLevel.colorHex)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "acheteurs actifs dans ce rayon (dernières 2 min)",
                                fontSize = 12.sp,
                                color = SlateTextSecondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Visual Activity Intensity Bar
                        val progress = (nearbyUsers / 30f).coerceIn(0.1f, 1.0f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color(heatLevel.colorHex),
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Grid-cell privacy indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Cellule : $currentGridCell (~1.2 km²)",
                                fontSize = 11.sp,
                                color = SlateTextSecondary
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isTrackingActive) EmeraldSuccess else SlateTextSecondary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isTrackingActive) "Actif (Premier plan)" else "En pause",
                                    fontSize = 11.sp,
                                    color = if (isTrackingActive) EmeraldSuccess else SlateTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Location Permission banner if missing
                if (!hasPermission) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = GoogleBlue.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoogleBlue.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = GoogleBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Activer la localisation pour détecter votre zone locale",
                                fontSize = 11.sp,
                                color = GoogleBlue,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Regional breakdown section
                Text(
                    text = "Répartition par zone commerciale",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(regionalBreakdown.take(6), key = { it.regionName }) { region ->
                        RegionalBreakdownItem(region)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))

                // Privacy-first banner (Expandable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPrivacyDetails = !showPrivacyDetails }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Garantie Confidentialité & Vie Privée",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }
                    Text(
                        text = if (showPrivacyDetails) "Masquer" else "En savoir plus",
                        fontSize = 11.sp,
                        color = GoogleBlue,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (showPrivacyDetails) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "• Coordonnées agrégées par cellules kilométriques (aucun point GPS brut stocké).",
                                fontSize = 10.5.sp,
                                color = SlateTextSecondary,
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "• Sessions éphémères anonymes expirant après 2 minutes d'inactivité (TTL).",
                                fontSize = 10.5.sp,
                                color = SlateTextSecondary,
                                lineHeight = 14.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "• Arrêt automatique de la localisation dès mise en arrière-plan (onPause/onStop).",
                                fontSize = 10.5.sp,
                                color = SlateTextSecondary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dismiss action button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = "Fermer",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionalBreakdownItem(info: RegionDensityInfo) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.regionName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = info.landmarkDescription,
                    fontSize = 10.sp,
                    color = SlateTextSecondary,
                    maxLines = 1
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(info.heatLevel.colorHex).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = Color(info.heatLevel.colorHex),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${info.count}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(info.heatLevel.colorHex)
                        )
                    }
                }
            }
        }
    }
}
