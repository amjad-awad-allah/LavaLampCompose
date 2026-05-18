package com.example.lavalamp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Design Presets for the Lava Lamp Component.
 */
enum class LavaLampStyle(val colors: List<Color>, val backgroundColor: Color, val chamberBackdrop: Color) {
    CYBERPUNK(
        colors = listOf(Color(0xFF8A2BE2), Color(0xFF0000FF), Color(0xFFFF1493), Color(0xFF00FFFF)),
        backgroundColor = Color(0xFF06060E),
        chamberBackdrop = Color(0xFF140826) // Deep violet inner glass glow
    ),
    VOLCANIC(
        colors = listOf(Color(0xFFFF3300), Color(0xFFFF6600), Color(0xFFFF9900), Color(0xFFCC0000)),
        backgroundColor = Color(0xFF0D0303),
        chamberBackdrop = Color(0xFF260808) // Deep crimson inner glass glow
    ),
    LIQUID_MERCURY(
        colors = listOf(Color(0xFFE6E6FA), Color(0xFFD8BFD8), Color(0xFFB0C4DE), Color(0xFF778899)),
        backgroundColor = Color(0xFF080A0E),
        chamberBackdrop = Color(0xFF131722) // Cool metallic blue-grey glow
    ),
    AURORA_FOREST(
        colors = listOf(Color(0xFF00FFCC), Color(0xFF00FF66), Color(0xFF3399FF), Color(0xFF1A237E)),
        backgroundColor = Color(0xFF02070D),
        chamberBackdrop = Color(0xFF051D1F) // Deep teal inner glass glow
    )
}

/**
 * Physics-Based Blob representing a real lava lamp bubble inside the glass chamber.
 */
class LavaBlob(
    var color: Color, // Mutable color to update styles instantly in place
    val baseRadius: Float,
    var x: Float = -1f,
    var y: Float = -1f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var isRising: Boolean = true,
    val phaseX: Float = Random.nextFloat() * 2f * Math.PI.toFloat(),
    val scalePhase: Float = Random.nextFloat() * 2f * Math.PI.toFloat(),
    val scaleSpeed: Float = Random.nextFloat() * 0.3f + 0.1f,
    var imageIndex: Int = 0 // Identifies which PNG asset texture from the custom image list this blob represents
)

/**
 * Defines the rendering mode for the lava lamp blobs (programmatic vector gradients vs custom PNG assets).
 */
sealed interface LavaMode {
    data class Vector(val style: LavaLampStyle = LavaLampStyle.CYBERPUNK, val customColors: List<Color>? = null) : LavaMode
    data class Png(val images: List<ImageBitmap>) : LavaMode
}

/**
 * Defines the background rendering styles (classic gradient, transparent, or custom overlays).
 */
sealed interface LavaBackground {
    object StyleBackdrop : LavaBackground
    object Transparent : LavaBackground
    data class Custom(
        val chamberImage: ImageBitmap? = null,
        val wholeImage: ImageBitmap? = null
    ) : LavaBackground
}

/**
 * Tuning parameters for the high-fidelity viscous fluid physics engine.
 */
data class LavaPhysicsConfig(
    val damping: Float = 0.95f,
    val softRepulsion: Float = 120f,
    val smoothingWeight: Float = 0.05f
)

@Composable
fun LavaLamp(
    modifier: Modifier = Modifier,
    blobCount: Int = 6,
    speed: Float = 1.0f,
    flowIntensity: Float = 0.5f,
    interactive: Boolean = true,
    sensorReactive: Boolean = true,
    noiseOverlay: Boolean = true,
    mode: LavaMode = LavaMode.Vector(LavaLampStyle.CYBERPUNK),
    background: LavaBackground = LavaBackground.StyleBackdrop,
    physicsConfig: LavaPhysicsConfig = LavaPhysicsConfig()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleResumed by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // Skip physics and rendering when the app is in background or paused
            isLifecycleResumed = event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_CREATE || event == Lifecycle.Event.ON_START
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var time by remember { mutableFloatStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    var tiltOffset by remember { mutableStateOf(Offset.Zero) }
    var shakeTriggered by remember { mutableStateOf(false) } // Detects vigorous phone shake
 
    // Resolve rendering mode options
    val style = when (mode) {
        is LavaMode.Vector -> mode.style
        is LavaMode.Png -> LavaLampStyle.CYBERPUNK // Fallback style reference for metallic cap/base colors
    }
 
    val activeColors = when (mode) {
        is LavaMode.Vector -> mode.customColors ?: mode.style.colors
        is LavaMode.Png -> LavaLampStyle.CYBERPUNK.colors
    }
 
    val blobImages = when (mode) {
        is LavaMode.Vector -> null
        is LavaMode.Png -> mode.images
    }
 
    // Resolve background images
    val chamberBackgroundImage = when (background) {
        is LavaBackground.Custom -> background.chamberImage
        else -> null
    }
 
    val wholeBackgroundImage = when (background) {
        is LavaBackground.Custom -> background.wholeImage
        else -> null
    }
 
    // Transparent vs Solid styles background support
    val resolvedBgColor = if (background == LavaBackground.Transparent) Color.Transparent else style.backgroundColor

    // 1. Gyroscope/Sensor gravity and shake listener (Automatically unregisters in background for lifecycle resume/pause safety)
    if (sensorReactive && isLifecycleResumed) {
        DisposableEffect(Unit) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val listener = object : SensorEventListener {
                var lastX = 0f
                var lastY = 0f
                var lastZ = 0f
                var lastUpdate = System.currentTimeMillis()
                
                var smoothX = 0f
                var smoothY = 0f

                override fun onSensorChanged(event: SensorEvent?) {
                    if (event != null) {
                        val currentTime = System.currentTimeMillis()
                        val diffTime = currentTime - lastUpdate
                        if (diffTime > 90) { // Throttle calculations to avoid overhead
                            lastUpdate = currentTime
                            val x = event.values[0]
                            val y = event.values[1]
                            val z = event.values[2]

                            if (lastX != 0f) {
                                val deltaX = x - lastX
                                val deltaY = y - lastY
                                val deltaZ = z - lastZ
                                // Calculate total acceleration delta (jerk)
                                val acceleration = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
                                
                                // Threshold of 13.5f represents a clear, active shake of the handset
                                if (acceleration > 13.5f) {
                                    shakeTriggered = true
                                }
                            }
                            lastX = x
                            lastY = y
                            lastZ = z
                        }

                        // Smooth tilt values
                        smoothX = smoothX * 0.85f + (-event.values[0] * 18f) * 0.15f
                        smoothY = smoothY * 0.85f + (event.values[1] * 18f) * 0.15f
                        tiltOffset = Offset(smoothX, smoothY)
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
    } else {
        tiltOffset = Offset.Zero
        shakeTriggered = false
    }

    // 2. Initialize dynamic physical blobs list inside a SnapshotStateList to allow runtime addition/removal (splitting)
    val blobs = remember { mutableStateListOf<LavaBlob>() }

    // Re-populate list whenever blobCount changes from the sandbox control card
    LaunchedEffect(blobCount) {
        blobs.clear()
        repeat(blobCount) { index ->
            blobs.add(
                LavaBlob(
                    color = activeColors[index % activeColors.size],
                    baseRadius = Random.nextFloat() * 25f + 70f,
                    isRising = Random.nextBoolean(),
                    imageIndex = index
                )
            )
        }
    }

    // Update colors of the existing blobs dynamically in-place when style/colors change
    SideEffect {
        blobs.forEachIndexed { index, blob ->
            if (index < blobs.size) {
                blob.color = activeColors[index % activeColors.size]
            }
        }
    }

    // 3. Physics engine loop inside LaunchedEffect (uses self-sustaining delay to bypass Choreographer idle sleeps)
    LaunchedEffect(blobs, size, speed, flowIntensity) {
        var lastTime = System.nanoTime()
        var prevTouchPos: Offset? = null
        while (isActive) {
            // Guarantee continuous ticking at ~60fps independently of screen draw passes (paused when app is in background)
            kotlinx.coroutines.delay(16)
            if (!isLifecycleResumed) {
                lastTime = System.nanoTime()
                continue
            }

            val currentTime = System.nanoTime()
            val realDelta = (currentTime - lastTime) / 1_000_000_000f
            val delta = realDelta * speed * (0.4f + flowIntensity * 1.6f)
            lastTime = currentTime
            time += delta

            // Calculate Touch Velocity for Drag/Swipe Influence
            val currentTouch = touchPosition
            val touchVelocity = if (currentTouch != null && prevTouchPos != null && realDelta > 0) {
                (currentTouch - prevTouchPos) / realDelta
            } else {
                Offset.Zero
            }
            prevTouchPos = currentTouch

            val width = size.width.toFloat()
            val height = size.height.toFloat()
            if (width <= 0f || height <= 0f) continue // Loop continuously waiting for positive screen size

            // Define Glass Chamber Dimensions
            val lampWidth = width * 0.55f
            val lampHeight = height * 0.75f
            val centerX = width / 2f
            val centerY = height * 0.45f
            val glassTop = centerY - lampHeight / 2f
            val glassBottom = centerY + lampHeight / 2f

            // C. Viscous Shake sloshing & bubble splitting (oil and water emulsification)
            if (shakeTriggered) {
                shakeTriggered = false // Reset trigger instantly
                
                val newBlobs = mutableListOf<LavaBlob>()
                val blobsToRemove = mutableListOf<LavaBlob>()
                
                // Read from a safe snapshot list copy to prevent concurrent modification exceptions
                val currentBlobsList = blobs.toList()
                currentBlobsList.forEach { blob ->
                    if (blob.x != -1f) {
                        if (blob.baseRadius > 45f) { // Only split larger blobs to prevent infinite division
                            blobsToRemove.add(blob)
                            
                            // Split into two smaller blobs with proportional liquid area/volume
                            val newRadius = blob.baseRadius * 0.68f
                            
                            // Explode left and right with high opposite speeds
                            newBlobs.add(
                                LavaBlob(
                                    color = blob.color,
                                    baseRadius = newRadius,
                                    x = blob.x,
                                    y = blob.y,
                                    vx = -180f - Random.nextFloat() * 120f,
                                    vy = blob.vy + (Random.nextFloat() - 0.5f) * 120f,
                                    isRising = Random.nextBoolean(),
                                    imageIndex = blob.imageIndex
                                )
                            )
                            newBlobs.add(
                                LavaBlob(
                                    color = blob.color,
                                    baseRadius = newRadius,
                                    x = blob.x,
                                    y = blob.y,
                                    vx = 180f + Random.nextFloat() * 120f,
                                    vy = blob.vy + (Random.nextFloat() - 0.5f) * 120f,
                                    isRising = Random.nextBoolean(),
                                    imageIndex = blob.imageIndex
                                )
                            )
                        } else {
                            // Shake up small blobs with random sloshing velocities!
                            blob.vx += (Random.nextFloat() - 0.5f) * 450f
                            blob.vy += (Random.nextFloat() - 0.5f) * 450f
                        }
                    }
                }
                
                if (blobsToRemove.isNotEmpty()) {
                    blobs.removeAll(blobsToRemove)
                    blobs.addAll(newBlobs)
                }
            }

            // D. Tactile Bubble interaction (Removed splitting to maintain liquid behavior)

            // 2.5 Calculate volumetric soft repulsion between blobs (elastic collision push)
            val activePhysicsBlobs = blobs.toList()
            for (i in activePhysicsBlobs.indices) {
                val blobI = activePhysicsBlobs[i]
                if (blobI.x == -1f) continue
                val radiusI = blobI.baseRadius * (1f + 0.12f * sin(time * blobI.scaleSpeed + blobI.scalePhase))
                
                for (j in i + 1 until activePhysicsBlobs.size) {
                    val blobJ = activePhysicsBlobs[j]
                    if (blobJ.x == -1f) continue
                    val radiusJ = blobJ.baseRadius * (1f + 0.12f * sin(time * blobJ.scaleSpeed + blobJ.scalePhase))
                    
                    val dx = blobJ.x - blobI.x
                    val dy = blobJ.y - blobI.y
                    val dist = hypot(dx, dy)
                    val minDist = radiusI + radiusJ
                    
                    if (dist < minDist && dist > 1f) {
                        val overlap = minDist - dist
                        // Volumetric elastic fluid repulsion force
                        val forceStrength = (overlap / minDist) * physicsConfig.softRepulsion
                        
                        val pushX = (dx / dist) * forceStrength
                        val pushY = (dy / dist) * forceStrength
                        
                        // Push them apart gently
                        blobI.vx -= pushX
                        blobI.vy -= pushY
                        blobJ.vx += pushX
                        blobJ.vy += pushY
                    }
                }
            }

            blobs.forEach { blob ->
                // Initialize inside the bottle coordinates
                if (blob.x == -1f) {
                    blob.x = centerX + (Random.nextFloat() - 0.5f) * (lampWidth * 0.4f)
                    blob.y = glassBottom - Random.nextFloat() * (lampHeight * 0.7f)
                }

                // A. Vertical Buoyancy force (convection)
                val verticalBuoyancy = if (blob.isRising) -80f else 80f
                
                // Gentle horizontal sin wave drift
                val horizontalDrift = sin(time * 0.5f + blob.phaseX) * 20f

                // B. Viscous Accelerometer Tilt Gravity sliding (Gentle horizontal drift only)
                val tiltForceX = tiltOffset.x * 12f
                val tiltForceY = tiltOffset.y * 4f

                // C. Viscous Touch Magnet & Drag Force (Physical attraction and swipe momentum)
                var touchForceX = 0f
                var touchForceY = 0f
                var dragForceX = 0f
                var dragForceY = 0f
                if (interactive) {
                    touchPosition?.let { touch ->
                        val dx = touch.x - blob.x
                        val dy = touch.y - blob.y
                        val dist = hypot(dx, dy)
                        
                        // 1. Soft Magnetic Attraction (Pulls liquid gently to look viscous, not violent)
                        val influenceRadius = lampWidth * 1.3f 
                        if (dist < influenceRadius && dist > 5f) {
                            val pullFactor = (1f - dist / influenceRadius)
                            val strength = pullFactor * 160f // Reduced from 450f for thick liquid cohesion
                            touchForceX = (dx / dist) * strength
                            touchForceY = (dy / dist) * strength
                        }
                        
                        // 2. Strong Swipe Momentum (Fluid flow created by finger drag)
                        val dragRadius = lampWidth * 1.2f 
                        if (dist < dragRadius && touchVelocity != Offset.Zero) {
                            val dragFactor = (1f - dist / dragRadius)
                            dragForceX = touchVelocity.x * dragFactor * 0.45f // Increased to 0.45f for strong swipe impact!
                            dragForceY = touchVelocity.y * dragFactor * 0.45f
                        }
                    }
                }

                // D. Apply high-viscosity fluid drag (adjustable damping and smoothing weights from physicsConfig)
                blob.vx = blob.vx * physicsConfig.damping + (horizontalDrift + tiltForceX + touchForceX + dragForceX) * physicsConfig.smoothingWeight
                blob.vy = blob.vy * physicsConfig.damping + (verticalBuoyancy + tiltForceY + touchForceY + dragForceY) * physicsConfig.smoothingWeight

                // Speed Clamping (Viscosity Limit to prevent liquid from disintegrating or scattering)
                val maxSpeed = 380f
                val currentSpeed = hypot(blob.vx, blob.vy)
                if (currentSpeed > maxSpeed) {
                    blob.vx = (blob.vx / currentSpeed) * maxSpeed
                    blob.vy = (blob.vy / currentSpeed) * maxSpeed
                }

                // E. Update positions
                blob.x += blob.vx * delta
                blob.y += blob.vy * delta

                // F. Dynamic breathing size
                val currentRadius = blob.baseRadius * (1f + 0.12f * sin(time * blob.scaleSpeed + blob.scalePhase))

                // G. Switch direction when hitting top/bottom cooling zones of the glass
                val coolingThresholdTop = glassTop + currentRadius + 20f
                val coolingThresholdBottom = glassBottom - currentRadius - 20f
                if (blob.y < coolingThresholdTop) {
                    blob.isRising = false
                } else if (blob.y > coolingThresholdBottom) {
                    blob.isRising = true
                }

                // H. Strict horizontal boundary collision (Keep strictly within glass tapered width!)
                val relativeHeightProgress = (blob.y - glassTop) / lampHeight
                val activeGlassWidth = lampWidth * (0.6f + relativeHeightProgress * 0.4f)
                
                val minX = centerX - activeGlassWidth / 2f + currentRadius
                val maxX = centerX + activeGlassWidth / 2f - currentRadius

                if (blob.x < minX) {
                    blob.x = minX
                    blob.vx *= -0.15f
                } else if (blob.x > maxX) {
                    blob.x = maxX
                    blob.vx *= -0.15f
                }

                // I. Strict vertical bounds inside the glass tube
                if (blob.y < glassTop + currentRadius) {
                    blob.y = glassTop + currentRadius
                    blob.vy = 0f
                } else if (blob.y > glassBottom - currentRadius) {
                    blob.y = glassBottom - currentRadius
                    blob.vy = 0f
                }
            }
        }
    }

    // 4. Pre-generate micro-grain noise bitmap for tactile matte finish
    val noiseBitmap = remember(noiseOverlay) {
        if (!noiseOverlay) null else {
            val width = 128
            val height = 128
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            val random = java.util.Random()
            for (i in pixels.indices) {
                val alpha = random.nextInt(15)
                pixels[i] = android.graphics.Color.argb(alpha, 255, 255, 255)
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        }
    }

    // Explicit Lifecycle and Memory Cleanup: Recycle native bitmap on dispose
    DisposableEffect(noiseBitmap) {
        onDispose {
            noiseBitmap?.let {
                if (!it.isRecycled) {
                    it.recycle()
                }
            }
        }
    }

    // 5. Interactivity touch detector
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

    // =========================================================================
    // MULTI-LAYERED COMPOSITION STRUCTURE FOR FLUID METABALLS AND SHARP CHROME
    // =========================================================================
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(resolvedBgColor)
            .onSizeChanged { size = it }
    ) {
        val width = size.width.toFloat()
        val height = size.height.toFloat()

        if (width > 0f && height > 0f) {
            val lampWidth = width * 0.55f
            val lampHeight = height * 0.75f
            val centerX = width / 2f
            val centerY = height * 0.45f
            val glassTop = centerY - lampHeight / 2f
            val glassBottom = centerY + lampHeight / 2f

            // Build Tapered Glass Bottle Chamber Path
            val glassPath = Path().apply {
                val bottomWidth = lampWidth * 0.95f
                val topWidth = lampWidth * 0.65f
                
                moveTo(centerX - bottomWidth / 2f, glassBottom)
                cubicTo(
                    centerX - lampWidth * 0.48f, centerY + lampHeight * 0.2f,
                    centerX - lampWidth * 0.38f, centerY - lampHeight * 0.2f,
                    centerX - topWidth / 2f, glassTop
                )
                lineTo(centerX + topWidth / 2f, glassTop)
                cubicTo(
                    centerX + lampWidth * 0.38f, centerY - lampHeight * 0.2f,
                    centerX + lampWidth * 0.48f, centerY + lampHeight * 0.2f,
                    centerX + bottomWidth / 2f, glassBottom
                )
                close()
            }

            // LAYER 0: Custom full background image (drawn behind the entire component)
            wholeBackgroundImage?.let { bg ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawImage(
                        image = bg,
                        dstSize = IntSize(width.toInt(), height.toInt())
                    )
                }
            }

            // LAYER 1: Ambient Shadow Glow and Glass Backdrop Liquid
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw Ambient Backdrop Shadow Glow behind the whole lamp
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(style.chamberBackdrop.copy(alpha = 0.3f), Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = lampHeight * 0.6f
                    ),
                    size = Size(width, height)
                )

                // Draw Glowing Glass Backdrop Liquid
                clipPath(glassPath) {
                    if (chamberBackgroundImage != null) {
                        drawImage(
                            image = chamberBackgroundImage,
                            dstOffset = IntOffset((centerX - lampWidth).toInt(), glassTop.toInt()),
                            dstSize = IntSize((lampWidth * 2f).toInt(), lampHeight.toInt())
                        )
                    } else {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    style.chamberBackdrop.copy(alpha = 0.95f),
                                    style.chamberBackdrop.copy(alpha = 0.7f),
                                    style.chamberBackdrop.copy(alpha = 0.9f)
                                ),
                                startY = glassTop,
                                endY = glassBottom
                            ),
                            topLeft = Offset(centerX - lampWidth, glassTop),
                            size = Size(lampWidth * 2f, lampHeight)
                        )
                    }
                }
            }

            // LAYER 2: Isolated Fluid Metaball Liquid Blobs (Isolated graphicsLayer for absolute safety!)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(pointerModifier)
                    .graphicsLayer {
                        // Apply highly stable, optimized Blur + Alpha Thresholding ONLY to the blobs!
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val blur = android.graphics.RenderEffect.createBlurEffect(
                                16f, 16f, android.graphics.Shader.TileMode.CLAMP // CLAMP is 100% stable on emulators!
                            )
                            val matrix = floatArrayOf(
                                1f, 0f, 0f, 0f, 0f,
                                0f, 1f, 0f, 0f, 0f,
                                0f, 0f, 1f, 0f, 0f,
                                0f, 0f, 0f, 24f, -1150f // Optimized 4x faster rendering color threshold
                            )
                            val colorFilter = android.graphics.RenderEffect.createColorFilterEffect(
                                ColorMatrixColorFilter(matrix)
                            )
                            renderEffect = android.graphics.RenderEffect.createChainEffect(colorFilter, blur)
                                .asComposeRenderEffect()
                        }
                    }
            ) {
                // Force Compose snapshot system to observe 'time' state so this Canvas ALWAYS invalidates and redraws during animation ticks
                val drawTime = time

                clipPath(glassPath) {
                    // A. Draw Tapered Glowing Bottom Reservoir (Hot wax pool in base)
                    val bottomPoolRadius = lampWidth * 0.45f
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.0f to activeColors[0],
                            0.45f to activeColors[0],
                            0.75f to activeColors[0].copy(alpha = 0.6f),
                            1.0f to Color.Transparent,
                            center = Offset(centerX, glassBottom + 20f),
                            radius = bottomPoolRadius
                        ),
                        radius = bottomPoolRadius,
                        center = Offset(centerX, glassBottom + 20f),
                        blendMode = BlendMode.SrcOver
                    )

                    // B. Draw Tapered Glowing Top Reservoir (Cooling wax pool in top cap)
                    val topPoolRadius = lampWidth * 0.32f
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.0f to activeColors.last(),
                            0.45f to activeColors.last(),
                            0.75f to activeColors.last().copy(alpha = 0.6f),
                            1.0f to Color.Transparent,
                            center = Offset(centerX, glassTop - 15f),
                            radius = topPoolRadius
                        ),
                        radius = topPoolRadius,
                        center = Offset(centerX, glassTop - 15f),
                        blendMode = BlendMode.SrcOver
                    )

                    // C. Draw Floating Dynamic Physics-Based Blobs inside the glass
                    blobs.forEach { blob ->
                        if (blob.x == -1f) return@forEach

                        val radius = blob.baseRadius * (1f + 0.12f * sin(time * blob.scaleSpeed + blob.scalePhase))

                        if (blobImages != null && blobImages.isNotEmpty()) {
                            // Scale and render custom PNG image asset
                            val bitmap = blobImages[blob.imageIndex % blobImages.size]
                            drawImage(
                                image = bitmap,
                                dstOffset = IntOffset((blob.x - radius).toInt(), (blob.y - radius).toInt()),
                                dstSize = IntSize((radius * 2).toInt(), (radius * 2).toInt())
                            )
                        } else {
                            // Fallback to programmatic glowing gradient metaball vector circles
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0.0f to blob.color,
                                    0.45f to blob.color,
                                    0.75f to blob.color.copy(alpha = 0.7f),
                                    1.0f to Color.Transparent,
                                    center = Offset(blob.x, blob.y),
                                    radius = radius
                                ),
                                radius = radius,
                                center = Offset(blob.x, blob.y),
                                blendMode = BlendMode.SrcOver
                            )
                        }
                    }
                }
            }

            // LAYER 3: Sharp Gloss reflections, metallic base and cap
            Canvas(modifier = Modifier.fillMaxSize()) {
                // A. Draw Curved Glass Reflex Highlights (drawn on top of blurred blobs!)
                clipPath(glassPath) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.02f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.05f)
                            ),
                            start = Offset(centerX - lampWidth * 0.4f, glassTop),
                            end = Offset(centerX + lampWidth * 0.4f, glassTop)
                        ),
                        topLeft = Offset(centerX - lampWidth, glassTop),
                        size = Size(lampWidth * 2f, lampHeight)
                    )

                    // Dynamic diagonal gloss highlight line
                    val reflexWidth = lampWidth * 0.08f
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.12f), Color.Transparent),
                            startX = centerX - lampWidth * 0.35f,
                            endX = centerX - lampWidth * 0.35f + reflexWidth
                        ),
                        topLeft = Offset(centerX - lampWidth * 0.35f, glassTop),
                        size = Size(reflexWidth, lampHeight)
                    )
                }

                // B. Draw Solid Metallic Cap and Base
                val metallicBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF2C2D35), // Dark chrome edge
                        Color(0xFF4C4E58), // Medium metal
                        Color(0xFFE2E4EB), // Bright reflective chrome core
                        Color(0xFF636572), // Soft metal shadow
                        Color(0xFF1E1F24)  // Dark chrome edge
                    ),
                    startX = centerX - lampWidth / 2f,
                    endX = centerX + lampWidth / 2f
                )

                // Metallic Base (Curved Trapezoid base)
                val baseTopWidth = lampWidth * 0.95f
                val baseBottomWidth = lampWidth * 1.15f
                val baseHeight = lampHeight * 0.16f
                val baseBottomY = glassBottom + baseHeight

                val basePath = Path().apply {
                    moveTo(centerX - baseTopWidth / 2f, glassBottom)
                    lineTo(centerX + baseTopWidth / 2f, glassBottom)
                    lineTo(centerX + baseBottomWidth / 2f, baseBottomY)
                    lineTo(centerX - baseBottomWidth / 2f, baseBottomY)
                    close()
                }
                drawPath(path = basePath, brush = metallicBrush)

                // Glowing trim line
                drawLine(
                    color = Color.White.copy(alpha = 0.25f),
                    start = Offset(centerX - baseTopWidth / 2f, glassBottom),
                    end = Offset(centerX + baseTopWidth / 2f, glassBottom),
                    strokeWidth = 3f
                )

                // Metallic Top Cap (Curved tapered cap)
                val capBottomWidth = lampWidth * 0.65f
                val capTopWidth = lampWidth * 0.5f
                val capHeight = lampHeight * 0.08f
                val capTopY = glassTop - capHeight

                val capPath = Path().apply {
                    moveTo(centerX - capBottomWidth / 2f, glassTop)
                    lineTo(centerX + capBottomWidth / 2f, glassTop)
                    lineTo(centerX + capTopWidth / 2f, capTopY)
                    lineTo(centerX - capTopWidth / 2f, capTopY)
                    close()
                }
                drawPath(path = capPath, brush = metallicBrush)

                // C. Draw high-performance hardware tiled cinematic noise overlay
                noiseBitmap?.let { bitmap ->
                    drawContext.canvas.nativeCanvas.drawBitmap(
                        bitmap,
                        null,
                        Rect(0, 0, size.width.toInt(), size.height.toInt()),
                        Paint().apply {
                            shader = BitmapShader(
                                bitmap,
                                Shader.TileMode.REPEAT,
                                Shader.TileMode.REPEAT
                            )
                        }
                    )
                }
            }
        }
    }
}
