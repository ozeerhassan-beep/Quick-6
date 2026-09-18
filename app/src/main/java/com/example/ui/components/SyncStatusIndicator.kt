package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CatalogViewModel
import com.example.util.AppLanguage
import com.example.util.CatalogSyncManager
import com.example.util.WorkSyncStatus

/**
 * Persistent UI element for the TopAppBar using an Icon with dynamic content description
 * and colors based on the WorkManager synchronization state:
 * - Green (0xFF10B981) for 'Synced'
 * - Yellow (0xFFF59E0B) for 'Syncing...'
 * - Grey (0xFF9CA3AF) for 'Offline'
 * - Red (0xFFEF4444) for 'Sync Error'
 */
@Composable
fun SyncStatusIndicator(
    viewModel: CatalogViewModel? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val workSyncStatus by CatalogSyncManager.workSyncStatus.collectAsState()
    val isOnline by CatalogSyncManager.isOnline.collectAsState()
    val syncState by CatalogSyncManager.syncState.collectAsState()

    val isServerSyncing = viewModel?.isServerSyncing?.collectAsState()?.value ?: false
    val isFirestoreSyncing = viewModel?.isFirestoreSyncing?.collectAsState()?.value ?: false
    val currentLang = viewModel?.appLanguage?.collectAsState()?.value ?: AppLanguage.FRENCH

    val isActivelySyncing = syncState.isRunning || isServerSyncing || isFirestoreSyncing || workSyncStatus == WorkSyncStatus.SYNCING
    val isError = syncState.isError || workSyncStatus == WorkSyncStatus.ERROR

    // Effective state resolution
    val effectiveStatus = when {
        isActivelySyncing -> WorkSyncStatus.SYNCING
        !isOnline -> WorkSyncStatus.OFFLINE
        isError -> WorkSyncStatus.ERROR
        else -> WorkSyncStatus.SYNCED
    }

    // Dynamic color setup: Green for 'Synced', Yellow for 'Syncing...', Grey for 'Offline', Red for 'Error'
    val targetColor = when (effectiveStatus) {
        WorkSyncStatus.SYNCED -> Color(0xFF10B981)   // Green
        WorkSyncStatus.SYNCING -> Color(0xFFF59E0B)  // Yellow / Amber
        WorkSyncStatus.OFFLINE -> Color(0xFF9CA3AF)  // Grey
        WorkSyncStatus.ERROR -> Color(0xFFEF4444)    // Red
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(350),
        label = "sync_icon_color"
    )

    val iconVector: ImageVector = when (effectiveStatus) {
        WorkSyncStatus.SYNCED -> Icons.Default.CloudDone
        WorkSyncStatus.SYNCING -> Icons.Default.Sync
        WorkSyncStatus.OFFLINE -> Icons.Default.CloudOff
        WorkSyncStatus.ERROR -> Icons.Default.SyncProblem
    }

    val contentDesc = when (effectiveStatus) {
        WorkSyncStatus.SYNCED -> if (currentLang == AppLanguage.FRENCH) "Synchronisé (WorkManager)" else "Synced"
        WorkSyncStatus.SYNCING -> if (currentLang == AppLanguage.FRENCH) "Synchronisation en cours..." else "Syncing..."
        WorkSyncStatus.OFFLINE -> if (currentLang == AppLanguage.FRENCH) "Hors-ligne (Room local)" else "Offline"
        WorkSyncStatus.ERROR -> if (currentLang == AppLanguage.FRENCH) "Erreur de synchronisation" else "Sync Error"
    }

    val displayLabel = when (effectiveStatus) {
        WorkSyncStatus.SYNCED -> "Synced"
        WorkSyncStatus.SYNCING -> "Syncing..."
        WorkSyncStatus.OFFLINE -> "Offline"
        WorkSyncStatus.ERROR -> "Error"
    }

    val spinTransition = rememberInfiniteTransition(label = "sync_rotation")
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .testTag("persistent_topbar_sync_status_indicator"),
        shape = RoundedCornerShape(16.dp),
        color = animatedColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, animatedColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = contentDesc,
                tint = animatedColor,
                modifier = Modifier
                    .size(15.dp)
                    .then(if (effectiveStatus == WorkSyncStatus.SYNCING) Modifier.rotate(spinAngle) else Modifier)
                    .testTag("workmanager_sync_status_icon")
            )

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = displayLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = animatedColor,
                maxLines = 1
            )
        }
    }
}
