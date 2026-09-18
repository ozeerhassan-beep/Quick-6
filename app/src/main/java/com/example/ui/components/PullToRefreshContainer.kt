package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private const val PULL_TRIGGER_THRESHOLD_PX = 220f
private const val PULL_MAX_OFFSET_PX = 320f

/**
 * Modern Jetpack Compose Pull-to-Refresh container that integrates seamlessly
 * via NestedScrollConnection to intercept drag gestures on any scrollable child
 * (LazyColumn, LazyVerticalGrid, etc.) and triggers Firebase Firestore synchronization
 * to update the local Room database instance.
 */
@Composable
fun PullToRefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val pullOffset = remember { Animatable(0f) }
    var isTriggered by remember { mutableStateOf(false) }

    // When isRefreshing state updates from ViewModel, animate indicator
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            pullOffset.animateTo(
                targetValue = 140f,
                animationSpec = spring(stiffness = 400f)
            )
        } else {
            pullOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = 300f)
            )
            isTriggered = false
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // When user is dragging up and we have pull offset, consume the scroll
                if (available.y < 0 && pullOffset.value > 0 && !isRefreshing) {
                    val newOffset = (pullOffset.value + available.y).coerceAtLeast(0f)
                    coroutineScope.launch {
                        pullOffset.snapTo(newOffset)
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // When user drags down past the top of the scrollable list
                if (available.y > 0 && !isRefreshing && source == NestedScrollSource.UserInput) {
                    val dragResistance = 0.45f
                    val newOffset = (pullOffset.value + available.y * dragResistance)
                        .coerceAtMost(PULL_MAX_OFFSET_PX)
                    coroutineScope.launch {
                        pullOffset.snapTo(newOffset)
                    }
                    isTriggered = newOffset >= PULL_TRIGGER_THRESHOLD_PX
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!isRefreshing && pullOffset.value > 0) {
                    if (pullOffset.value >= PULL_TRIGGER_THRESHOLD_PX || isTriggered) {
                        isTriggered = true
                        onRefresh()
                    } else {
                        pullOffset.animateTo(0f, animationSpec = spring(stiffness = 400f))
                    }
                }
                return Velocity.Zero
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ptr_spinner")
    val spinningAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .testTag("pull_to_refresh_container")
    ) {
        // Main content with subtle offset during pull
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(0, (pullOffset.value * 0.4f).roundToInt())
                }
        ) {
            content()
        }

        // Pull to Refresh Floating Badge
        val currentOffset = pullOffset.value
        val isVisible = currentOffset > 10f || isRefreshing

        if (isVisible) {
            val progressFraction = min(1f, currentOffset / PULL_TRIGGER_THRESHOLD_PX)
            val badgeY = currentOffset * 0.65f + 16f

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, badgeY.roundToInt()) }
                    .zIndex(100f)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp))
                    .testTag("pull_to_refresh_indicator"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isRefreshing || isTriggered) BluePrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isRefreshing) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(spinningAngle)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mise à jour Room ↔ Firebase...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                    } else {
                        val rotation = progressFraction * 180f
                        Icon(
                            imageVector = if (progressFraction >= 1f) Icons.Default.CloudDownload else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (progressFraction >= 1f) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(if (progressFraction >= 1f) 0f else rotation)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (progressFraction >= 1f) "Relâchez pour actualiser" else "Tirez pour synchroniser",
                            fontSize = 12.sp,
                            fontWeight = if (progressFraction >= 1f) FontWeight.Bold else FontWeight.Medium,
                            color = if (progressFraction >= 1f) BluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
