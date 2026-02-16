package com.irah.galleria.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.util.lerp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AnimatedBlob(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_bg")
    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(13000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "t1"
    )
    val t2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(17000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "t2"
    )
    val t3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(23000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "t3"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val baseColors = if (isDark) {
            listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B))
        } else {
            listOf(Color(0xFFF0F9FF), Color(0xFFE0F2FE), Color(0xFFBAE6FD))
        }
        drawRect(brush = Brush.verticalGradient(baseColors))

        val blobColors = if (isDark) {
            listOf(
                Color(0xFF4F46E5).copy(alpha = 0.5f),
                Color(0xFFEC4899).copy(alpha = 0.4f),
                Color(0xFF06B6D4).copy(alpha = 0.4f)
            )
        } else {
            listOf(
                Color(0xFFA5F3FC).copy(alpha = 0.7f),
                Color(0xFFFBCFE8).copy(alpha = 0.6f),
                Color(0xFFDDD6FE).copy(alpha = 0.7f)
            )
        }

        drawCircle(
            brush = Brush.radialGradient(colors = listOf(blobColors[0], Color.Transparent)),
            radius = w * 0.9f,
            center = Offset(
                x = w * -0.3f + (w * 1.6f * t1),
                y = h * -0.2f + (h * 1.4f * t2)
            )
        )
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(blobColors[1], Color.Transparent)),
            radius = w * 0.85f,
            center = Offset(
                x = w * 1.3f - (w * 1.6f * t2),
                y = h * 1.2f - (h * 1.4f * t3)
            )
        )
        drawCircle(
            brush = Brush.radialGradient(colors = listOf(blobColors[2], Color.Transparent)),
            radius = w * 0.8f,
            center = Offset(
                x = w * -0.2f + (w * 1.5f * t3),
                y = h * 1.2f - (h * 1.4f * t1)
            )
        )
    }
}

@Composable
fun AnimatedWave(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_bg")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val baseColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF0F9FF)
        drawRect(color = baseColor)

        val waveColors = if (isDark) {
            listOf(
                Color(0xFF3B82F6).copy(alpha = 0.1f),
                Color(0xFF8B5CF6).copy(alpha = 0.1f),
                Color(0xFFEC4899).copy(alpha = 0.1f)
            )
        } else {
            listOf(
                Color(0xFF60A5FA).copy(alpha = 0.2f),
                Color(0xFFA78BFA).copy(alpha = 0.2f),
                Color(0xFFF472B6).copy(alpha = 0.2f)
            )
        }

        waveColors.forEachIndexed { index, color ->
            val path = Path()
            val amplitude = h * 0.1f
            val frequency = 2 * Math.PI / w
            val yOffset = h * (0.3f + index * 0.2f)
            val currentPhase = phase + index * (Math.PI / 2)

            path.moveTo(0f, h)
            path.lineTo(0f, yOffset)

            for (x in 0..w.toInt() step 10) {
                val y = yOffset + amplitude * sin(x * frequency + currentPhase).toFloat()
                path.lineTo(x.toFloat(), y)
            }
            path.lineTo(w, h)
            path.close()

            drawPath(path = path, color = color)
        }
    }
}

@Composable
fun AnimatedGradient(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient_bg")
    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "t1"
    )
    val t2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "t2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        
        // Base background
        val baseColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
        drawRect(color = baseColor)

        val c1 = if (isDark) Color(0xFF4C1D95).copy(alpha=0.4f) else Color(0xFFDDD6FE).copy(alpha=0.6f)
        val c2 = if (isDark) Color(0xFF1E40AF).copy(alpha=0.4f) else Color(0xFFBFDBFE).copy(alpha=0.6f)
        val c3 = if (isDark) Color(0xFFBE185D).copy(alpha=0.3f) else Color(0xFFFBCFE8).copy(alpha=0.5f)

        // Moving gradient orbs
        drawCircle(
            brush = Brush.radialGradient(listOf(c1, Color.Transparent)),
            radius = w * 0.8f,
            center = Offset(w * t1, h * 0.2f)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(c2, Color.Transparent)),
            radius = w * 0.7f,
            center = Offset(w * (1-t2), h * 0.8f)
        )
         drawCircle(
            brush = Brush.radialGradient(listOf(c3, Color.Transparent)),
            radius = w * 0.9f,
            center = Offset(w * 0.5f, h * 0.5f + (h*0.3f * sin(t1 * Math.PI).toFloat()))
        )
        // Noise overlay
        drawRect(
             brush = Brush.radialGradient(
                 colors = listOf(Color.Transparent, if(isDark) Color.Black.copy(alpha=0.4f) else Color.White.copy(alpha=0.4f)),
                 radius = size.maxDimension,
                 center = center
             )
         )
    }
}

@Composable
fun AnimatedParticles(isDark: Boolean) {
    val particles = remember { List(40) { Particle() } }
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val baseColor = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)
        drawRect(color = baseColor)

        val particleColor = if (isDark) Color(0xFF6366F1) else Color(0xFF818CF8)

        particles.forEachIndexed { index, particle ->
            // Floating motion using sine waves
            val x = (particle.initialX * w + 50 * sin(time + index)) % w
            val y = (particle.initialY * h + 50 * cos(time + index * 0.5f)) % h

            val safeX = x.coerceIn(0f, w)
            val safeY = y.coerceIn(0f, h)
            
            val alpha = 0.2f + 0.3f * sin(time * 2 + index)
            
            drawCircle(
                color = particleColor.copy(alpha = alpha.coerceIn(0.1f, 0.5f)),
                radius = particle.radius * (0.8f + 0.2f * sin(time * 3 + index)),
                center = Offset(safeX, safeY)
            )
        }
    }
}

private data class Particle(
    val initialX: Float = Random.nextFloat(),
    val initialY: Float = Random.nextFloat(),
    val radius: Float = Random.nextFloat() * 6f + 2f
)

@Composable
fun AnimatedMesh(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val t by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "t"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val baseColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
        drawRect(color = baseColor)

        val lineColor = if (isDark) Color(0xFF38BDF8).copy(alpha = 0.1f) else Color(0xFF0EA5E9).copy(alpha = 0.1f)
        val cols = 8
        val rows = 12
        val cellW = w / cols
        val cellH = h / rows

        for (i in 0..cols) {
            for (j in 0..rows) {
                val x = i * cellW
                val y = j * cellH
                
                // Distort based on sine wave
                val distortionX = 20f * sin(t + y / 100f)
                val distortionY = 20f * cos(t + x / 100f)
                
                val pX = x + distortionX
                val pY = y + distortionY
                
                // Draw horizontal lines
                if (i < cols) {
                    val nextX = (i + 1) * cellW + 20f * sin(t + y / 100f) // Simplified next point calc
                    val nextY = j * cellH + 20f * cos(t + (i+1)*cellW / 100f)
                    drawLine(lineColor, Offset(pX, pY), Offset(nextX, nextY), strokeWidth = 2f)
                }
                
                // Draw vertical lines
                if (j < rows) {
                     val nextX = i * cellW + 20f * sin(t + (j+1)*cellH / 100f)
                     val nextY = (j + 1) * cellH + 20f * cos(t + x / 100f)
                     drawLine(lineColor, Offset(pX, pY), Offset(nextX, nextY), strokeWidth = 2f)
                }
            }
        }
    }
}
