package com.example.lavacomponent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Data class representing an individual floating blob in the lava lamp.
 */
data class LavaBlob(
    val color: Color,
    val baseRadius: Float,
    val centerX: Float,
    val centerY: Float,
    val phaseX: Float,
    val phaseY: Float,
    val speedX: Float,
    val speedY: Float,
    val scalePhase: Float,
    val scaleSpeed: Float
)

/**
 * A reusable Jetpack Compose component that renders a premium, high-end 
 * animated background resembling a lava lamp or soft fluid motion.
 * 
 * @param modifier Modifier for styling and layout.
 * @param blobCount The number of animated glowing blobs to render.
 * @param colors The color palette to use for the blobs.
 * @param speed Multiplier for the animation speed.
 * @param interactive If true, blobs will organically react to user touch.
 */
@Composable
fun LavaLampComponent(
    modifier: Modifier = Modifier,
    blobCount: Int = 6,
    colors: List<Color> = listOf(
        Color(0xFF8A2BE2), // Deep Purple
        Color(0xFF0000FF), // Bright Blue
        Color(0xFFFF1493), // Neon Pink
        Color(0xFF00FFFF)  // Cyan
    ),
    speed: Float = 1.0f,
    interactive: Boolean = true
) {
    var time by remember { mutableFloatStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }

    // Animation loop for fluid, continuous motion (tied to frame rate)
    LaunchedEffect(speed) {
        var lastTime = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frameTime ->
                val delta = (frameTime - lastTime) / 1_000_000_000f
                time += delta * speed
                lastTime = frameTime
            }
        }
    }

    // Initialize random soft blobs (remembered across recompositions)
    val blobs = remember(blobCount, colors) {
        List(blobCount) {
            LavaBlob(
                color = colors[Random.nextInt(colors.size)],
                baseRadius = Random.nextFloat() * 150f + 250f, // Large glowing blobs
                centerX = Random.nextFloat(), // 0.0 to 1.0 relative to width
                centerY = Random.nextFloat(), // 0.0 to 1.0 relative to height
                phaseX = Random.nextFloat() * 2f * Math.PI.toFloat(),
                phaseY = Random.nextFloat() * 2f * Math.PI.toFloat(),
                speedX = Random.nextFloat() * 0.3f + 0.1f, // Slow, hypnotic speed
                speedY = Random.nextFloat() * 0.3f + 0.1f,
                scalePhase = Random.nextFloat() * 2f * Math.PI.toFloat(),
                scaleSpeed = Random.nextFloat() * 0.4f + 0.1f
            )
        }
    }

    // Optional interactivity modifier
    val pointerModifier = if (interactive) {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown()
                    touchPosition = down.position
                    do {
                        val event = awaitPointerEvent()
                        touchPosition = event.changes.firstOrNull()?.position
                    } while (event.changes.any { it.pressed })
                    touchPosition = null
                }
            }
        }
    } else Modifier

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080812)) // Deep dark background for high contrast neon glow
            .onSizeChanged { size = it }
            .then(pointerModifier)
    ) {
        val width = size.width.toFloat()
        val height = size.height.toFloat()
        if (width == 0f || height == 0f) return@Canvas

        blobs.forEach { blob ->
            // Base movement using sine/cosine for fluid natural motion.
            // Expanding the bounds slightly outside the screen allows blobs to drift in and out smoothly.
            val cx = width * blob.centerX + sin(time * blob.speedX + blob.phaseX) * (width * 0.5f)
            val cy = height * blob.centerY + cos(time * blob.speedY + blob.phaseY) * (height * 0.5f)

            // Dynamic "breathing" size changes over time
            val radius = blob.baseRadius * (1f + 0.2f * sin(time * blob.scaleSpeed + blob.scalePhase))

            // Smoothly interpolate towards touch position if interactive
            var renderCx = cx
            var renderCy = cy

            touchPosition?.let { touch ->
                val dx = touch.x - cx
                val dy = touch.y - cy
                val dist = hypot(dx, dy)
                val influenceRadius = width * 0.7f

                if (dist < influenceRadius) {
                    // Soft, delayed organic pull towards the finger
                    val pullStrength = ((1f - dist / influenceRadius) * 0.15f) 
                    renderCx += dx * pullStrength
                    renderCy += dy * pullStrength
                }
            }

            // Draw the soft glowing blob using a radial gradient and Screen blend mode.
            // Screen blend mode acts like light accumulation, giving the metaball illusion
            // when multiple colors overlap smoothly.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        blob.color.copy(alpha = 0.8f),
                        blob.color.copy(alpha = 0.4f),
                        blob.color.copy(alpha = 0.0f)
                    ),
                    center = Offset(renderCx, renderCy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(renderCx, renderCy),
                blendMode = BlendMode.Screen
            )
        }
    }
}
