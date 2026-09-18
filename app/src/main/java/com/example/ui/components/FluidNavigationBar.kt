package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class NavigationTabItem(
    val index: Int,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val testTag: String,
    val badgeCount: Int = 0
)

@Composable
fun FluidNavigationBar(
    items: List<NavigationTabItem>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 72.dp,
    fabSize: Dp = 48.dp,
    cutoutWidth: Dp = 80.dp,
    cutoutDepth: Dp = 40.dp
) {
    if (items.isEmpty()) return

    val safeSelectedIndex = selectedTabIndex.coerceIn(0, items.size - 1)
    val density = LocalDensity.current

    val barBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val tabWidthPx = totalWidthPx / items.size
        
        // Compute active center X offset
        val targetCenterX = tabWidthPx * safeSelectedIndex + tabWidthPx / 2f

        // Fluid spring glide animation position horizontally for the FAB
        val animatedCenterX by animateFloatAsState(
            targetValue = targetCenterX,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "cutout_glide"
        )

        // String bottom anchor X that lags behind, creating the tether pull effect
        val stringAnchorX = remember { Animatable(targetCenterX) }
        LaunchedEffect(targetCenterX) {
            delay(60)
            stringAnchorX.animateTo(
                targetValue = targetCenterX,
                animationSpec = spring(
                    dampingRatio = 0.5f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        // Visible jump-and-bounce animation triggered on tab change
        val verticalBounceAnim = remember { Animatable(0f) }
        LaunchedEffect(safeSelectedIndex) {
            val jumpHeightPx = with(density) { (-30).dp.toPx() }
            verticalBounceAnim.animateTo(
                targetValue = jumpHeightPx,
                animationSpec = spring(
                    dampingRatio = 0.4f,
                    stiffness = Spring.StiffnessMedium
                )
            )
            verticalBounceAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.2f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        val cutoutWidthPx = with(density) { cutoutWidth.toPx() }
        val cutoutDepthPx = with(density) { cutoutDepth.toPx() }
        val fabSizePx = with(density) { fabSize.toPx() }

        // 1. Fluid Background Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .align(Alignment.BottomCenter)
                .drawWithCache {
                    val path = Path()
                    val strokePath = Path()
                    val strokeWidthPx = 1.dp.toPx()

                    onDrawBehind {
                        val cx = animatedCenterX
                        val halfCutout = cutoutWidthPx / 2f
                        val startX = cx - halfCutout
                        val endX = cx + halfCutout

                        path.reset()
                        path.moveTo(0f, 0f)
                        path.lineTo(startX, 0f)

                        // Smooth Bezier Curve Down
                        path.cubicTo(
                            cx - cutoutWidthPx / 2.8f, 0f,
                            cx - cutoutWidthPx / 4f, cutoutDepthPx,
                            cx, cutoutDepthPx
                        )
                        // Smooth Bezier Curve Up
                        path.cubicTo(
                            cx + cutoutWidthPx / 4f, cutoutDepthPx,
                            cx + cutoutWidthPx / 2.8f, 0f,
                            endX, 0f
                        )

                        path.lineTo(size.width, 0f)
                        path.lineTo(size.width, size.height)
                        path.lineTo(0f, size.height)
                        path.close()

                        // Draw background fill
                        drawPath(path, color = barBgColor)

                        // Draw top border outline
                        strokePath.reset()
                        strokePath.moveTo(0f, 0f)
                        strokePath.lineTo(startX, 0f)
                        strokePath.cubicTo(
                            cx - cutoutWidthPx / 2.8f, 0f,
                            cx - cutoutWidthPx / 4f, cutoutDepthPx,
                            cx, cutoutDepthPx
                        )
                        strokePath.cubicTo(
                            cx + cutoutWidthPx / 4f, cutoutDepthPx,
                            cx + cutoutWidthPx / 2.8f, 0f,
                            endX, 0f
                        )
                        strokePath.lineTo(size.width, 0f)

                        drawPath(
                            path = strokePath,
                            color = outlineColor,
                            style = Stroke(width = strokeWidthPx)
                        )
                    }
                }
        ) {
            // Render non-selected tabs & labels
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom
            ) {
                items.forEach { item ->
                    val isSelected = item.index == safeSelectedIndex

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(item.index) }
                            )
                            .testTag(item.testTag),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            if (!isSelected) {
                                NavigationBadgeWrapper(badgeCount = item.badgeCount) {
                                    Icon(
                                        imageVector = item.inactiveIcon,
                                        contentDescription = item.label,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            } else {
                                // Active Label under the cutout
                                Text(
                                    text = item.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Tethered String (Connects the lagging bottom anchor to the moving FAB with increased thickness)
        Canvas(modifier = Modifier.matchParentSize()) {
            val startX = stringAnchorX.value
            val startY = cutoutDepthPx 
            val fabBottomX = animatedCenterX
            val fabBottomY = -10.dp.toPx() + verticalBounceAnim.value + (fabSizePx / 2f)

            drawLine(
                color = outlineColor,
                start = Offset(startX, startY),
                end = Offset(fabBottomX, fabBottomY),
                strokeWidth = 3.dp.toPx() // Increased thickness size here
            )
        }

        // 2. Floating Active Circular Bubble with Visible Jump & Bounce
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .graphicsLayer {
                    translationX = animatedCenterX - (fabSizePx / 2f)
                    translationY = -10.dp.toPx() + verticalBounceAnim.value
                }
                .size(width = fabSize, height = fabSize / 2)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .background(Color.Transparent, CircleShape)
                .clip(CircleShape)
                .clickable { onTabSelected(safeSelectedIndex) },
            contentAlignment = Alignment.Center
        ) {
            val activeItem = items.getOrNull(safeSelectedIndex) ?: items.firstOrNull()

            if (activeItem != null) {
                NavigationBadgeWrapper(badgeCount = activeItem.badgeCount) {
                    Icon(
                        imageVector = activeItem.activeIcon,
                        contentDescription = activeItem.label,
                        modifier = Modifier
                            .size(22.dp)
                            .drawWithCache {
                                val linearGradient = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF10B981), // Vibrant Mint/Emerald
                                        Color(0xFF0F5132)  // Deep Luxury Emerald Green
                                    )
                                )
                                onDrawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = linearGradient,
                                        blendMode = BlendMode.SrcAtop
                                    )
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationBadgeWrapper(
    badgeCount: Int,
    content: @Composable () -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        content()
        if (badgeCount > 0) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-4).dp)
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}