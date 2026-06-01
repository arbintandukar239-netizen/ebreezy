package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.viewmodel.JarvisMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactor(
    mode: JarvisMode,
    volumeIntensity: Float,
    modifier: Modifier = Modifier
) {
    // 1. Core rotate animation based on State speed
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor")
    val rotateDuration = when (mode) {
        JarvisMode.THINKING -> 1200
        JarvisMode.SPEAKING -> 2500
        JarvisMode.LISTENING -> 4500
        else -> 6000
    }
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(rotateDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // 2. Respiratory pulse animation
    val pulseDuration = if (mode == JarvisMode.LISTENING) 800 else 1800
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Reactive boost with vocal signals
    val reactiveAmplifier = if (mode == JarvisMode.LISTENING) {
        pulseScale * (1f + (volumeIntensity * 0.4f))
    } else if (mode == JarvisMode.SPEAKING) {
        pulseScale * 1.02f
    } else {
        pulseScale
    }

    // Color definitions
    val neonCyan = Color(0xFF00FFCC)
    val glowingBlue = Color(0xFF0099FF)
    val darkAtmosphere = Color(0xFF003366)
    val alertOrange = Color(0xFFFF5500)

    val reactorColor = when (mode) {
        JarvisMode.LISTENING -> alertOrange
        JarvisMode.THINKING -> neonCyan
        JarvisMode.SPEAKING -> glowingBlue
        else -> glowingBlue
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .graphicsLayer {
                scaleX = reactiveAmplifier
                scaleY = reactiveAmplifier
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val outerRadius = size.width / 2.2f
            val innerRadius = outerRadius * 0.70f
            val coreRadius = outerRadius * 0.35f

            // 1. Draw outer glowing blueprint circle
            drawCircle(
                color = reactorColor.copy(alpha = 0.15f),
                radius = outerRadius + 15f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Draw ticks on the outer blueprint
            val ticks = 36
            for (i in 0 until ticks) {
                val angle = (i * (360f / ticks)) * (PI / 180f)
                val startX = center.x + (outerRadius + 8f) * cos(angle).toFloat()
                val startY = center.y + (outerRadius + 8f) * sin(angle).toFloat()
                val endX = center.x + (outerRadius + 18f) * cos(angle).toFloat()
                val endY = center.y + (outerRadius + 18f) * sin(angle).toFloat()
                drawLine(
                    color = reactorColor.copy(alpha = 0.3f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 2. Draw outer solid support frame
            drawCircle(
                color = reactorColor.copy(alpha = 0.4f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 3. Draw middle segmented mechanical coils (spinning ring)
            rotate(rotation, center) {
                val coilCount = 10
                val sweepAngle = 26f
                val gapAngle = 10f
                for (i in 0 until coilCount) {
                    val startAngle = i * (sweepAngle + gapAngle)
                    
                    // Draw outer arc segmented line
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(reactorColor.copy(alpha = 0.2f), reactorColor, reactorColor.copy(alpha = 0.2f)),
                            center = center
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                        size = Size(innerRadius * 2, innerRadius * 2),
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw spokes connecting outer core
                    val rad = (startAngle + sweepAngle / 2f) * (PI / 180f)
                    val spokeStartX = center.x + (coreRadius + 10f) * cos(rad).toFloat()
                    val spokeStartY = center.y + (coreRadius + 10f) * sin(rad).toFloat()
                    val spokeEndX = center.x + (innerRadius - 10f) * cos(rad).toFloat()
                    val spokeEndY = center.y + (innerRadius - 10f) * sin(rad).toFloat()
                    drawLine(
                        color = reactorColor.copy(alpha = 0.4f),
                        start = Offset(spokeStartX, spokeStartY),
                        end = Offset(spokeEndX, spokeEndY),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            // 4. Draw inner blueprint grid rings
            drawCircle(
                color = reactorColor.copy(alpha = 0.25f),
                radius = innerRadius - 15f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 5. Draw glowing Center Core
            // Radial core blur imitation
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(reactorColor, reactorColor.copy(alpha = 0.3f), Color.Transparent),
                    center = center,
                    radius = coreRadius * 1.5f
                ),
                radius = coreRadius * 1.5f,
                center = center
            )

            // Central solid core ring
            drawCircle(
                color = Color.White,
                radius = coreRadius,
                center = center
            )

            drawCircle(
                color = reactorColor.copy(alpha = 0.8f),
                radius = coreRadius - 4f,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // 6. Draw central mechanical triangle
            rotate(-rotation * 1.5f, center) {
                val path = Path().apply {
                    val triRadius = coreRadius * 0.7f
                    val angle1 = 0 * (PI / 180) - (PI / 2)
                    val angle2 = 120 * (PI / 180) - (PI / 2)
                    val angle3 = 240 * (PI / 180) - (PI / 2)

                    moveTo(
                        center.x + triRadius * cos(angle1).toFloat(),
                        center.y + triRadius * sin(angle1).toFloat()
                    )
                    lineTo(
                        center.x + triRadius * cos(angle2).toFloat(),
                        center.y + triRadius * sin(angle2).toFloat()
                    )
                    lineTo(
                        center.x + triRadius * cos(angle3).toFloat(),
                        center.y + triRadius * sin(angle3).toFloat()
                    )
                    close()
                }
                drawPath(
                    path = path,
                    color = reactorColor,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
