package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PriceHistoryEntity
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess

@Composable
fun PriceTrendChart(
    history: List<PriceHistoryEntity>,
    modifier: Modifier = Modifier
) {
    if (history.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aucun historique de prix disponible",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Les variations de prix apparaîtront au fil des mises à jour.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    val prices = history.map { it.price }
    val minPrice = prices.minOrNull() ?: 0.0
    val maxPrice = prices.maxOrNull() ?: 0.0
    val avgPrice = if (prices.isNotEmpty()) prices.average() else 0.0
    val firstPrice = prices.firstOrNull() ?: 0.0
    val lastPrice = prices.lastOrNull() ?: 0.0
    val priceDiff = lastPrice - firstPrice
    val pctDiff = if (firstPrice > 0) (priceDiff / firstPrice) * 100 else 0.0

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(history.size) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = BluePrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = "Tendance des prix",
                                tint = BluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Tendance Historique des Prix",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${history.size} relevés enregistrés dans Room",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Trend badge
                val isDrop = pctDiff < 0
                val isSame = kotlin.math.abs(pctDiff) < 0.01
                val badgeColor = when {
                    isDrop -> EmeraldSuccess
                    isSame -> BluePrimary
                    else -> Color(0xFFE53935)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDrop) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSame) "Stable" else "${if (pctDiff > 0) "+" else ""}${String.format("%.1f", pctDiff)}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stat Summary Cards (Min, Max, Avg, Current)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill(title = "Actuel", value = "Rs ${String.format("%.2f", lastPrice)}", color = BluePrimary, modifier = Modifier.weight(1f))
                StatPill(title = "Moyenne", value = "Rs ${String.format("%.2f", avgPrice)}", color = Color(0xFF6B7280), modifier = Modifier.weight(1f))
                StatPill(title = "Min", value = "Rs ${String.format("%.2f", minPrice)}", color = EmeraldSuccess, modifier = Modifier.weight(1f))
                StatPill(title = "Max", value = "Rs ${String.format("%.2f", maxPrice)}", color = Color(0xFFE53935), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected point info if tapped
            selectedPointIndex?.let { idx ->
                val point = history.getOrNull(idx)
                if (point != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BluePrimary.copy(alpha = 0.08f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📅 ${point.recordedDate} (${point.catalogType})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BluePrimary
                            )
                            Text(
                                text = "Rs ${String.format("%.2f", point.price)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BluePrimary
                            )
                        }
                    }
                }
            }

            // Interactive Canvas Chart
            val chartPrimaryColor = BluePrimary
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .pointerInput(history) {
                        detectTapGestures { offset ->
                            val width = size.width
                            val paddingLeft = 30f
                            val paddingRight = 30f
                            val usableWidth = width - paddingLeft - paddingRight
                            if (history.size > 1 && usableWidth > 0) {
                                val stepX = usableWidth / (history.size - 1)
                                val touchX = offset.x - paddingLeft
                                val closestIndex = (touchX / stepX).toInt().coerceIn(0, history.size - 1)
                                selectedPointIndex = closestIndex
                            } else if (history.size == 1) {
                                selectedPointIndex = 0
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(170.dp)) {
                    val width = size.width
                    val height = size.height

                    val paddingLeft = 40f
                    val paddingRight = 40f
                    val paddingTop = 25f
                    val paddingBottom = 40f

                    val usableWidth = width - paddingLeft - paddingRight
                    val usableHeight = height - paddingTop - paddingBottom

                    val range = if (maxPrice > minPrice) (maxPrice - minPrice) else 1.0
                    val baselineMin = (minPrice - (range * 0.15)).coerceAtLeast(0.0)
                    val baselineMax = maxPrice + (range * 0.15)
                    val effectiveRange = if (baselineMax > baselineMin) (baselineMax - baselineMin) else 1.0

                    // Horizontal grid lines
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = paddingTop + (usableHeight / gridLines) * i
                        drawLine(
                            color = onSurfaceColor.copy(alpha = 0.08f),
                            start = Offset(paddingLeft, y),
                            end = Offset(width - paddingRight, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    if (history.size == 1) {
                        // Single point
                        val cx = width / 2f
                        val cy = paddingTop + usableHeight / 2f
                        drawCircle(
                            color = chartPrimaryColor,
                            radius = 6.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    } else {
                        val points = history.mapIndexed { index, item ->
                            val x = paddingLeft + (usableWidth / (history.size - 1)) * index
                            val normY = (item.price - baselineMin) / effectiveRange
                            val y = paddingTop + usableHeight - (normY * usableHeight).toFloat()
                            Offset(x, y)
                        }

                        val progress = animationProgress.value
                        val visiblePoints = points.take((points.size * progress).toInt().coerceAtLeast(2).coerceAtMost(points.size))

                        if (visiblePoints.size >= 2) {
                            val linePath = Path().apply {
                                moveTo(visiblePoints[0].x, visiblePoints[0].y)
                                for (i in 1 until visiblePoints.size) {
                                    val p0 = visiblePoints[i - 1]
                                    val p1 = visiblePoints[i]
                                    val midX = (p0.x + p1.x) / 2f
                                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                                }
                            }

                            // Fill gradient under curve
                            val fillPath = Path().apply {
                                addPath(linePath)
                                lineTo(visiblePoints.last().x, height - paddingBottom)
                                lineTo(visiblePoints.first().x, height - paddingBottom)
                                close()
                            }

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        chartPrimaryColor.copy(alpha = 0.25f),
                                        chartPrimaryColor.copy(alpha = 0.0f)
                                    ),
                                    startY = paddingTop,
                                    endY = height - paddingBottom
                                )
                            )

                            // Main stroke line
                            drawPath(
                                path = linePath,
                                color = chartPrimaryColor,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            // Draw data point circles
                            visiblePoints.forEachIndexed { idx, point ->
                                val isSelected = selectedPointIndex == idx
                                drawCircle(
                                    color = Color.White,
                                    radius = if (isSelected) 7.dp.toPx() else 4.5.dp.toPx(),
                                    center = point
                                )
                                drawCircle(
                                    color = if (isSelected) Color(0xFFE53935) else chartPrimaryColor,
                                    radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                                    center = point
                                )
                            }
                        }
                    }
                }
            }

            // X-Axis Date Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (history.size > 0) {
                    Text(
                        text = history.first().recordedDate.take(8),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (history.size > 2) {
                        val midIdx = history.size / 2
                        Text(
                            text = history[midIdx].recordedDate.take(8),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        text = history.last().recordedDate.take(8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Touchez les points du graphique pour inspecter chaque relevé de prix.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun StatPill(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = color.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1
            )
        }
    }
}
