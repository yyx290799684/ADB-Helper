package com.yangyx.adbhelper.touch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot

/**
 * Visual feedback HUD & overlay for remote multi-touch interaction.
 * Renders glowing touch points, pointer ID badges, coordinates, connection vectors, and touch trails.
 */
@Composable
fun MultiTouchVisualOverlay(
    controller: MultiTouchController,
    modifier: Modifier = Modifier
) {
    val activePoints by controller.activePoints.collectAsState()
    val trailsMap by controller.trailsMap.collectAsState()
    val stats by controller.stats.collectAsState()

    if (!stats.isOverlayVisible) return

    Box(modifier = modifier.fillMaxSize()) {
        // Multi-touch canvas rendering
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pointsList = activePoints.values.toList()

            // 1. Draw motion trails if enabled
            if (stats.isTrailsEnabled) {
                trailsMap.forEach { (pointerId, trail) ->
                    if (trail.size >= 2) {
                        val color = TouchColors.getColorForPointer(pointerId)
                        val path = Path().apply {
                            moveTo(trail.first().first, trail.first().second)
                            for (i in 1 until trail.size) {
                                lineTo(trail[i].first, trail[i].second)
                            }
                        }
                        drawPath(
                            path = path,
                            color = color.copy(alpha = 0.45f),
                            style = Stroke(
                                width = 4.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
            }

            // 2. Draw connecting lines and distance indicators between multiple fingers
            if (pointsList.size >= 2) {
                for (i in 0 until pointsList.size) {
                    for (j in i + 1 until pointsList.size) {
                        val p1 = pointsList[i]
                        val p2 = pointsList[j]
                        val offset1 = Offset(p1.localX, p1.localY)
                        val offset2 = Offset(p2.localX, p2.localY)
                        val distancePx = hypot(p1.localX - p2.localX, p1.localY - p2.localY)

                        // Connecting dashed line
                        drawLine(
                            color = Color.White.copy(alpha = 0.5f),
                            start = offset1,
                            end = offset2,
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                        )

                        // Center point indicator with distance
                        val midX = (p1.localX + p2.localX) / 2f
                        val midY = (p1.localY + p2.localY) / 2f
                        drawCircle(
                            color = Color(0xFF64FFDA).copy(alpha = 0.7f),
                            radius = 6.dp.toPx(),
                            center = Offset(midX, midY)
                        )

                        // Distance label
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            "${distancePx.toInt()} px",
                            midX,
                            midY - 12f,
                            textPaint
                        )
                    }
                }
            }

            // 3. Draw active touch points
            pointsList.forEach { pt ->
                val center = Offset(pt.localX, pt.localY)
                val baseColor = TouchColors.getColorForPointer(pt.pointerId)
                val pressureRadius = (32.dp.toPx() * pt.pressure).coerceIn(24.dp.toPx(), 48.dp.toPx())

                // Outer glowing halo
                drawCircle(
                    color = baseColor.copy(alpha = 0.25f),
                    radius = pressureRadius * 1.5f,
                    center = center
                )

                // Outer ring
                drawCircle(
                    color = baseColor.copy(alpha = 0.85f),
                    radius = pressureRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Inner filled core
                drawCircle(
                    color = baseColor,
                    radius = 8.dp.toPx(),
                    center = center
                )

                // Native canvas text for pointer ID badge and remote coordinates
                if (stats.isCoordinatesVisible) {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 30f
                        isFakeBoldText = true
                        setShadowLayer(5f, 0f, 3f, android.graphics.Color.BLACK)
                        isAntiAlias = true
                    }

                    val badgeText = "P#${pt.pointerId} (${pt.remoteX}, ${pt.remoteY})"
                    drawContext.canvas.nativeCanvas.drawText(
                        badgeText,
                        pt.localX + pressureRadius + 8f,
                        pt.localY - 8f,
                        paint
                    )

                    val pressurePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.LTGRAY
                        textSize = 22f
                        setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "压力: ${(pt.pressure * 100).toInt()}%",
                        pt.localX + pressureRadius + 8f,
                        pt.localY + 22f,
                        pressurePaint
                    )
                }
            }
        }

        // 4. Live Multi-Touch HUD Bar (Top Right)
        AnimatedVisibility(
            visible = activePoints.isNotEmpty() || stats.eventsPerSecond > 0f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (activePoints.isNotEmpty()) Color(0xFF00E676) else Color.Gray
                            )
                    )
                    Text(
                        text = " 触点: ${activePoints.size} | 发送: ${stats.eventsPerSecond.toInt()} msg/s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
