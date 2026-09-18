package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.Screen

@Composable
fun LiquidNavigationBar(
    screens: List<Screen>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    badgeCounts: Map<Screen, Int> = emptyMap(),
    titleProvider: (Screen) -> String = { it.title }
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    // Determine whether current background is dark or light
    val isDarkTheme = colorScheme.background.red < 0.5f && colorScheme.background.green < 0.5f && colorScheme.background.blue < 0.5f

    // Glassy background and border styling
    val barBgColor = if (isDarkTheme) {
        colorScheme.surface.copy(alpha = 0.85f)
    } else {
        colorScheme.surface.copy(alpha = 0.95f)
    }

    val activeContentColor = Color.White
    val inactiveContentColor = if (isDarkTheme) {
        colorScheme.onSurface.copy(alpha = 0.6f)
    } else {
        colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    val badgeBgColor = colorScheme.tertiary
    val badgeTextColor = Color.White

    // Safe coordinates mapping for animating the morphing liquid indicator
    val itemWidths = remember { mutableStateMapOf<Int, Float>() }
    val itemOffsets = remember { mutableStateMapOf<Int, Float>() }

    // Safe indices mapping
    val currentLeft = itemOffsets[selectedIndex] ?: 0f
    val currentWidth = itemWidths[selectedIndex] ?: 0f

    var hasMeasured by remember { mutableStateOf(false) }
    if (currentWidth > 0f) {
        hasMeasured = true
    }

    // Snappy sliding animations for position and width
    val animatedLeft by animateFloatAsState(
        targetValue = currentLeft,
        animationSpec = if (hasMeasured) {
            spring(dampingRatio = 0.72f, stiffness = 380f)
        } else {
            snap()
        },
        label = "LiquidIndicatorLeft"
    )

    val animatedWidth by animateFloatAsState(
        targetValue = currentWidth,
        animationSpec = if (hasMeasured) {
            spring(dampingRatio = 0.72f, stiffness = 380f)
        } else {
            snap()
        },
        label = "LiquidIndicatorWidth"
    )

    val isFillSpace = screens.size < 4

    // Reset measured coordinates whenever the screens list changes (e.g. tabs added/removed in settings)
    LaunchedEffect(screens) {
        itemOffsets.clear()
        itemWidths.clear()
        hasMeasured = false
    }

    // Center scroll the newly selected item dynamically (only when scrollable)
    if (!isFillSpace) {
        LaunchedEffect(selectedIndex, itemOffsets.toMap(), itemWidths.toMap()) {
            val left = itemOffsets[selectedIndex]
            val width = itemWidths[selectedIndex]

            if (left != null && width != null) {
                // Calculate center position in the viewport
                val targetScroll = left - 150f // fallback centering factor
                scrollState.animateScrollTo(targetScroll.toInt().coerceIn(0, scrollState.maxValue))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp) // Strictly limit the height of the navigation bar container
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(9999.dp),
                clip = false
            )
            .background(
                color = barBgColor,
                shape = RoundedCornerShape(9999.dp)
            )
            .border(
                width = 1.dp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                shape = RoundedCornerShape(9999.dp)
            )
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val innerBoxModifier = if (isFillSpace) {
            Modifier.fillMaxWidth()
        } else {
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        }

        Box(
            modifier = innerBoxModifier,
            contentAlignment = Alignment.CenterStart
        ) {
            // 1. Snappy Liquid Indicator background pill
            if (currentWidth > 0f) {
                Box(
                    modifier = Modifier
                        .offset(x = with(LocalDensity.current) { animatedLeft.toDp() })
                        .width(with(LocalDensity.current) { animatedWidth.toDp() })
                        .height(38.dp)
                        .align(Alignment.CenterStart)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(9999.dp),
                            ambientColor = Color(0xFF34D399),
                            spotColor = Color(0xFF0F5132)
                        )
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF34D399),
                                    Color(0xFF0F5132)
                                )
                            ),
                            shape = RoundedCornerShape(9999.dp)
                        )
                )
            }

            // 2. Row of clickable navigation items
            val rowModifier = if (isFillSpace) {
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(vertical = 2.dp)
            } else {
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(vertical = 2.dp)
            }

            Row(
                modifier = rowModifier,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEachIndexed { index, screen ->
                    val isSelected = index == selectedIndex
                    val badgeCount = badgeCounts[screen] ?: 0
                    val screenDisplayTitle = titleProvider(screen)

                    val itemModifier = if (isFillSpace) {
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9999.dp))
                            .clickable { onItemSelected(index) }
                            .onGloballyPositioned { itemCoords ->
                                // Calculate position relative to the Row parent container directly
                                itemCoords.parentLayoutCoordinates?.let { parent ->
                                    itemOffsets[index] = parent.localPositionOf(itemCoords, Offset.Zero).x
                                    itemWidths[index] = itemCoords.size.width.toFloat()
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 8.dp)
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .clickable { onItemSelected(index) }
                            .onGloballyPositioned { itemCoords ->
                                // Calculate position relative to the Row parent container directly
                                itemCoords.parentLayoutCoordinates?.let { parent ->
                                    itemOffsets[index] = parent.localPositionOf(itemCoords, Offset.Zero).x
                                    itemWidths[index] = itemCoords.size.width.toFloat()
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = itemModifier
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screenDisplayTitle,
                                tint = if (isSelected) activeContentColor else inactiveContentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = screenDisplayTitle,
                                color = if (isSelected) activeContentColor else inactiveContentColor,
                                fontSize = if (isFillSpace && screens.size == 3) 12.sp else 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (badgeCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(badgeBgColor, CircleShape)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (badgeCount > 99) "99+" else "$badgeCount",
                                        color = badgeTextColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Right edge gradient visual cue to indicate overflow (only if scrollable)
        if (!isFillSpace && scrollState.value < scrollState.maxValue) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(28.dp)
                    .align(Alignment.CenterEnd)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, barBgColor.copy(alpha = 0.95f))
                        ),
                        shape = RoundedCornerShape(topEnd = 9999.dp, bottomEnd = 9999.dp)
                    )
            )
        }
    }
}
