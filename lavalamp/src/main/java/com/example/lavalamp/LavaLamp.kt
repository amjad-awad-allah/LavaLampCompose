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
    ),
    CLASSIC_70S(
        colors = listOf(Color(0xFFFFEE58), Color(0xFFFF9800), Color(0xFFFF5722), Color(0xFFE64A19)),
        backgroundColor = Color(0xFF1A0A00),
        chamberBackdrop = Color(0xFF331400) // Warm orange/red glow
    ),
    DEEP_OCEAN(
        colors = listOf(Color(0xFF00BFFF), Color(0xFF1E90FF), Color(0xFF4169E1), Color(0xFF00008B)),
        backgroundColor = Color(0xFF000511),
        chamberBackdrop = Color(0xFF001133) // Deep blue bioluminescence
    ),
    COTTON_CANDY(
        colors = listOf(Color(0xFFFFB6C1), Color(0xFFFF69B4), Color(0xFF87CEFA), Color(0xFFE0FFFF)),
        backgroundColor = Color(0xFF1F1A24),
        chamberBackdrop = Color(0xFF2D2338) // Soft pastel violet glow
    )
}

/**
 * Physics-Based Blob representing a real lava lamp bubble inside the glass chamber.
 */
class LavaBlob(
    var color: Color, // Mutable color to update styles instantly in place
    var baseRadius: Float,
    var x: Float = -1f,
    var y: Float = -1f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val phaseX: Float = Random.nextFloat() * 2f * Math.PI.toFloat(),
    val scalePhase: Float = Random.nextFloat() * 2f * Math.PI.toFloat(),
    val scaleSpeed: Float = Random.nextFloat() * 0.3f + 0.1f,
    var imageIndex: Int = 0, // Identifies which PNG asset texture from the custom image list this blob represents
    val id: Int = Random.nextInt(),
    var connectedBlobId: Int = -1, // ID of elastically bound blob, -1 if none
    var isAttachedToFinger: Boolean = false, // If dragged directly by the finger
    var snapRecoilTime: Float = -1f, // Time counter for post-split surface tension springy recoil
    
    // Grid/Slice attributes for Fluid Image support
    var originalX: Float = -1f,
    var originalY: Float = -1f,
    var srcX: Float = 0f,
    var srcY: Float = 0f,
    var srcW: Float = 0f,
    var srcH: Float = 0f
)

/**
 * Represents an ambient micro-particle (dust) drifting inside the lava lamp fluid.
 */
class LavaParticle(
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val radius: Float = Random.nextFloat() * 1.5f + 1f,
    val alpha: Float = Random.nextFloat() * 0.4f + 0.15f,
    val speedPhase: Float = Random.nextFloat() * 2f * Math.PI.toFloat()
)

/**
 * A lightweight, high-performance physical audio analysis helper for driving the Lava Lamp component.
 * It monitors the device microphone and splits the incoming sound frequencies into three primary bands:
 * Bass, Midrange, and Treble/Highs, returning smoothed intensities in [0..1] range.
 */
class LavaAudioProcessor(private val context: Context) {
    private var audioRecord: android.media.AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    
    // Smooth frequency bands (Bass, Mids, Highs)
    var bass = 0f
        private set
    var mids = 0f
        private set
    var highs = 0f
        private set
    
    var overallAmplitude = 0f
        private set
    
    // Callback for when spectrum bands are updated
    var onSpectrumUpdated: ((bass: Float, mids: Float, highs: Float, overall: Float) -> Unit)? = null

    private val sampleRate = 44100
    private val channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO
    private val audioEncoding = android.media.AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = android.media.AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding).coerceAtLeast(1024)

    // Low-pass state
    private var lpBass = 0f
    // Band-pass states
    private var bpMidPrev = 0f
    // High-pass state
    private var hpTreblePrev = 0f
    private var hpTreblePrevX = 0f

    fun start() {
        if (isRecording) return
        
        // Double-check permission
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            audioRecord = android.media.AudioRecord(
                android.media.MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioEncoding,
                bufferSize
            )
        } catch (e: SecurityException) {
            return
        } catch (e: Exception) {
            return
        }

        if (audioRecord?.state != android.media.AudioRecord.STATE_INITIALIZED) {
            audioRecord = null
            return
        }

        isRecording = true
        audioRecord?.startRecording()

        recordingThread = Thread({
            val buffer = ShortArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    analyzeAudio(buffer, read)
                }
            }
        }, "LavaAudioProcessor-Thread")
        recordingThread?.start()
    }

    fun stop() {
        isRecording = false
        try {
            recordingThread?.join(500)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        recordingThread = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null
    }

    private fun analyzeAudio(buffer: ShortArray, readSize: Int) {
        var totalBass = 0f
        var totalMids = 0f
        var totalHighs = 0f
        var totalAbs = 0f

        // Simple infinite impulse response (IIR) filtering for 3-band separation
        // Bass Low-Pass Filter coefficient (~150Hz cutoff)
        val alphaBass = 0.05f 
        // Mids Band-Pass filter resonant poles (~1kHz center)
        val alphaMidHigh = 0.35f
        // Highs High-Pass Filter coefficient (~4kHz cutoff)
        val alphaHigh = 0.45f

        for (i in 0 until readSize) {
            val sample = buffer[i].toFloat() / 32768f
            val absSample = kotlin.math.abs(sample)
            totalAbs += absSample

            // 1. Bass filter (Low-pass)
            lpBass = lpBass * (1f - alphaBass) + sample * alphaBass
            totalBass += kotlin.math.abs(lpBass)

            // 2. Mids filter (Band-pass estimated by subtracting lowpass and highpass)
            val midRes = sample - lpBass
            val bpMid = midRes * alphaMidHigh + bpMidPrev * (1f - alphaMidHigh)
            bpMidPrev = bpMid
            totalMids += kotlin.math.abs(bpMid)

            // 3. Highs filter (High-pass approximation)
            val hpTreble = alphaHigh * (hpTreblePrev + sample - hpTreblePrevX)
            hpTreblePrev = hpTreble
            hpTreblePrevX = sample
            totalHighs += kotlin.math.abs(hpTreble)
        }

        val avgOverall = (totalAbs / readSize) * 6f // Amplification factor
        val avgBass = (totalBass / readSize) * 12f
        val avgMids = (totalMids / readSize) * 12f
        val avgHighs = (totalHighs / readSize) * 15f

        // Temporal Smoothing (exponential moving average) to prevent high-frequency jitter
        val smoothing = 0.18f
        bass = (bass * (1f - smoothing) + avgBass.coerceIn(0f, 1f) * smoothing)
        mids = (mids * (1f - smoothing) + avgMids.coerceIn(0f, 1f) * smoothing)
        highs = (highs * (1f - smoothing) + avgHighs.coerceIn(0f, 1f) * smoothing)
        overallAmplitude = (overallAmplitude * (1f - smoothing) + avgOverall.coerceIn(0f, 1f) * smoothing)

        onSpectrumUpdated?.invoke(bass, mids, highs, overallAmplitude)
    }
}

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
 * Defines whether the fluid is displayed inside a classic glass bottle (GLASS_BOTTLE)
 * or floats freely across the full screen as a background layer (AMBIENT_BACKGROUND).
 *
 * Usage:
 *   containerMode = LavaContainerMode.GLASS_BOTTLE      // Classic lava lamp (default)
 *   containerMode = LavaContainerMode.AMBIENT_BACKGROUND // Free-flowing background fluid
 */
enum class LavaContainerMode {
    /** Classic lava lamp with glass chamber, chrome caps, and base. (Default) */
    GLASS_BOTTLE,
    /** Blobs float freely across the full screen with no container. Use as UI background. */
    AMBIENT_BACKGROUND
}

/**
 * Defines the direction of physics gravity inside the chamber.
 */
enum class LavaGravity {
    /** Buoyancy drives blobs up (Classic lava lamp behavior). */
    UP,
    /** Gravity pulls blobs down (Like raindrops or heavy falling wax). */
    DOWN,
    /** Zero gravity, blobs just drift and bounce slowly in space. */
    ZERO_GRAVITY
}

/**
 * Defines the visual rendering style of the glass bottle (if containerMode is GLASS_BOTTLE).
 */
enum class LavaGlassStyle {
    /** Highly reflective, skeuomorphic glass and metallic caps (Default). */
    REALISTIC_3D,
    /** Flat, minimal silhouette vector style without gloss or shadows. */
    FLAT_2D
}

/**
 * Defines the viscosity of the fluid, altering how blobs merge and separate.
 */
enum class LavaViscosity {
    /** Thin, fast-separating droplets. */
    WATER,
    /** Default lava lamp wax behavior. */
    STANDARD,
    /** Thick, sticky blobs that merge from far away and stretch heavily. */
    THICK_HONEY
}

/**
 * Tuning parameters for the high-fidelity viscous fluid physics engine.
 */
data class LavaPhysicsConfig(
    val damping: Float = 0.95f,
    val softRepulsion: Float = 120f,
    val smoothingWeight: Float = 0.05f,
    val touchInfluence: Float = 1.0f,
    val shakeInfluence: Float = 1.0f
)

/**
 * Tuning parameters for the 3D AGSL Refraction shader.
 */
data class LavaShaderConfig(
    val enabled: Boolean = true,
    val refractionStrength: Float = 12f,
    val specularIntensity: Float = 0.75f,
    val specularPower: Float = 25f,
    val lightDirectionX: Float = 0.5f,
    val lightDirectionY: Float = -0.5f
)

/**
 * A highly customizable, high-fidelity physics-based Lava Lamp component for Jetpack Compose.
 *
 * It features dynamic viscous fluid metaballs that stretch, merge, and split organically, 3D refraction
 * shaders, hardware accelerometer sloshing, physical obstacle deflection, and liquid image warp
 * with spring-based shape restoration.
 *
 * @param modifier Standard layout modifier to size and position the component.
 * @param blobCount The total number of dynamic liquid blobs to simulate. Must be greater than 0.
 * @param blobScale Multiplier to globally scale up or down the size of all metaballs. Must be positive.
 * @param speed Physics simulation time delta multiplier to speed up or slow down movement. Cannot be negative.
 * @param flowIntensity Adjusts internal buoyancy and random sine drift forces. Cannot be negative.
 * @param interactive When true, lets users touch, drag, fling, and stretch metaballs.
 * @param sensorReactive When true, uses device accelerometer sensor data to slosh liquid dynamically.
 * @param noiseOverlay Adds a premium cinematic analog micro-grain matte overlay.
 * @param lampRotation Rotational tilt offset in degrees for the entire visual canvas.
 * @param gravityMode Direction of gravity/buoyancy: [LavaGravity.UP] (Classic), [LavaGravity.DOWN], or [LavaGravity.ZERO_GRAVITY].
 * @param containerMode Renders either inside a classic skeuomorphic [LavaContainerMode.GLASS_BOTTLE] or full-screen [LavaContainerMode.AMBIENT_BACKGROUND].
 * @param glassStyle Style of glass/reflections when using [LavaContainerMode.GLASS_BOTTLE]: [LavaGlassStyle.REALISTIC_3D] or flat [LavaGlassStyle.FLAT_2D].
 * @param viscosity Liquid cohesion thickness and threshold mapping: [LavaViscosity.WATER], [LavaViscosity.STANDARD], or [LavaViscosity.THICK_HONEY].
 * @param pulseSpeed Rhythmical breathing breathing speed in Hz (0f to disable). Cannot be negative.
 * @param mode Renders either procedural color-gradient vectors ([LavaMode.Vector]) or custom floating PNG assets ([LavaMode.Png]).
 * @param background Renders styled gradient backdrops ([LavaBackground.StyleBackdrop]), transparent ([LavaBackground.Transparent]), or custom textures ([LavaBackground.Custom]).
 * @param physicsConfig Advanced parameters to fine-tune damping, touch influence, and soft repulsion weights.
 * @param shaderConfig Parameters to customize the 3D AGSL refraction strength, specular shininess, and light vectors.
 * @param obstacles Interactive bounding rectangles (e.g. from UI buttons/cards) that metaballs physically deflect around.
 * @param fluidImage A source bitmap image to liquefy, slice, and warp under physical forces before smoothly restoring back to its original layout.
 * @param imageRestorationStrength Proportional spring stiffness pulling image slices back to their original rest coordinate (0f to disable). Cannot be negative.
 * @param enableTiltDeformation Allows device accelerometer tilt sloshing when in fluid image mode.
 */
@Composable
fun LavaLamp(
    modifier: Modifier = Modifier,
    blobCount: Int = 10,
    blobScale: Float = 1.0f,
    speed: Float = 1.0f,
    flowIntensity: Float = 0.5f,
    interactive: Boolean = true,
    sensorReactive: Boolean = true,
    noiseOverlay: Boolean = true,
    lampRotation: Float = 0f,
    gravityMode: LavaGravity = LavaGravity.UP,
    containerMode: LavaContainerMode = LavaContainerMode.GLASS_BOTTLE,
    glassStyle: LavaGlassStyle = LavaGlassStyle.REALISTIC_3D,
    viscosity: LavaViscosity = LavaViscosity.STANDARD,
    pulseSpeed: Float = 0f,
    mode: LavaMode = LavaMode.Vector(LavaLampStyle.CYBERPUNK),
    background: LavaBackground = LavaBackground.StyleBackdrop,
    physicsConfig: LavaPhysicsConfig = LavaPhysicsConfig(),
    shaderConfig: LavaShaderConfig = LavaShaderConfig(),
    obstacles: List<androidx.compose.ui.geometry.Rect> = emptyList(),
    fluidImage: ImageBitmap? = null,
    imageRestorationStrength: Float = 0.05f,
    enableTiltDeformation: Boolean = true,
    audioInfluence: Float = 0f,
    audioFrequencyBands: List<Float> = emptyList(),
    enableParticles: Boolean = false,
    particleCount: Int = 80,
    enableColorMixing: Boolean = false,
    colorMixingRate: Float = 0.5f
) {
    require(blobCount > 0) { "LavaLamp: blobCount must be greater than 0 (provided: $blobCount)" }
    require(blobScale > 0f) { "LavaLamp: blobScale must be positive (provided: $blobScale)" }
    require(speed >= 0f) { "LavaLamp: speed cannot be negative (provided: $speed)" }
    require(flowIntensity >= 0f) { "LavaLamp: flowIntensity cannot be negative (provided: $flowIntensity)" }
    require(pulseSpeed >= 0f) { "LavaLamp: pulseSpeed cannot be negative (provided: $pulseSpeed)" }
    require(imageRestorationStrength >= 0f) { "LavaLamp: imageRestorationStrength cannot be negative (provided: $imageRestorationStrength)" }
    require(audioInfluence >= 0f) { "LavaLamp: audioInfluence cannot be negative (provided: $audioInfluence)" }
    require(particleCount >= 0) { "LavaLamp: particleCount cannot be negative (provided: $particleCount)" }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleResumed by remember { mutableStateOf(true) }

    val currentObstacles by rememberUpdatedState(obstacles)

    val runtimeShader = remember(shaderConfig.enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shaderConfig.enabled) {
            android.graphics.RuntimeShader(AGSL_SHADER)
        } else {
            null
        }
    }

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

    // Initialize micro-particles list
    val particles = remember { mutableStateListOf<LavaParticle>() }

    // Initialize/Re-populate micro-particles when particleCount or enableParticles or size changes
    LaunchedEffect(enableParticles, particleCount, size) {
        if (enableParticles && size.width > 0 && size.height > 0) {
            particles.clear()
            val width = size.width.toFloat()
            val height = size.height.toFloat()
            val lampWidth = width * 0.55f
            val lampHeight = height * 0.75f
            val centerX = width / 2f
            val centerY = height * 0.45f
            val glassTop = centerY - lampHeight / 2f
            val glassBottom = centerY + lampHeight / 2f
            
            repeat(particleCount) {
                particles.add(
                    LavaParticle(
                        x = centerX + (Random.nextFloat() - 0.5f) * lampWidth * 0.7f,
                        y = glassTop + Random.nextFloat() * lampHeight
                    )
                )
            }
        } else {
            particles.clear()
        }
    }

    // 3. Physics engine loop inside LaunchedEffect (uses self-sustaining delay to bypass Choreographer idle sleeps)
    LaunchedEffect(blobs, size, speed, flowIntensity, enableParticles, audioInfluence, audioFrequencyBands) {
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
            val delta = realDelta * speed * (0.4f + flowIntensity * 1.6f) * (1f + audioInfluence * 1.5f)
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

            // Initialize/update grid positions for fluidImage mode
            if (fluidImage != null) {
                val cols = kotlin.math.sqrt(blobs.size.toDouble()).toInt().coerceAtLeast(1)
                val rows = (blobs.size + cols - 1) / cols
                
                val imageRatio = fluidImage.width.toFloat() / fluidImage.height.toFloat()
                val containerRatio = width / height
                val imgTargetWidth = if (containerMode == LavaContainerMode.GLASS_BOTTLE) lampWidth * 0.8f else {
                    if (imageRatio > containerRatio) width else height * imageRatio
                }
                val imgTargetHeight = if (containerMode == LavaContainerMode.GLASS_BOTTLE) lampHeight * 0.7f else {
                    if (imageRatio > containerRatio) width / imageRatio else height
                }
                
                val imgTargetLeft = centerX - imgTargetWidth / 2f
                val imgTargetTop = if (containerMode == LavaContainerMode.GLASS_BOTTLE) glassTop + lampHeight * 0.15f else centerY - imgTargetHeight / 2f
                
                val cellW = imgTargetWidth / cols
                val cellH = imgTargetHeight / rows
                val avgCellSize = (cellW + cellH) / 2f
                val suggestedRadius = avgCellSize * 0.95f
                
                val srcCellW = fluidImage.width.toFloat() / cols
                val srcCellH = fluidImage.height.toFloat() / rows
                
                blobs.forEachIndexed { index, b ->
                    val col = index % cols
                    val row = index / cols
                    
                    b.originalX = imgTargetLeft + (col + 0.5f) * cellW
                    b.originalY = imgTargetTop + (row + 0.5f) * cellH
                    
                    b.srcX = col * srcCellW
                    b.srcY = row * srcCellH
                    b.srcW = srcCellW
                    b.srcH = srcCellH
                    
                    b.baseRadius = suggestedRadius
                    
                    if (b.x == -1f) {
                        b.x = b.originalX
                        b.y = b.originalY
                    }
                }
            }

            // C. Viscous Shake sloshing & bubble splitting (oil and water emulsification)
            if (shakeTriggered) {
                shakeTriggered = false // Reset trigger instantly
                
                if (fluidImage == null) {
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
                                        vx = (-180f - Random.nextFloat() * 120f) * physicsConfig.shakeInfluence,
                                        vy = blob.vy + ((Random.nextFloat() - 0.5f) * 120f) * physicsConfig.shakeInfluence,
                                        imageIndex = blob.imageIndex
                                    )
                                )
                                newBlobs.add(
                                    LavaBlob(
                                        color = blob.color,
                                        baseRadius = newRadius,
                                        x = blob.x,
                                        y = blob.y,
                                        vx = (180f + Random.nextFloat() * 120f) * physicsConfig.shakeInfluence,
                                        vy = blob.vy + ((Random.nextFloat() - 0.5f) * 120f) * physicsConfig.shakeInfluence,
                                        imageIndex = blob.imageIndex
                                    )
                                )
                            } else {
                                // Shake up small blobs with random sloshing velocities!
                                blob.vx += ((Random.nextFloat() - 0.5f) * 450f) * physicsConfig.shakeInfluence
                                blob.vy += ((Random.nextFloat() - 0.5f) * 450f) * physicsConfig.shakeInfluence
                            }
                        }
                    }
                    
                    if (blobsToRemove.isNotEmpty()) {
                        blobs.removeAll(blobsToRemove)
                        blobs.addAll(newBlobs)
                    }
                } else {
                    // Shake sloshes image grid nodes vigorously without splitting them
                    blobs.forEach { blob ->
                        blob.vx += ((Random.nextFloat() - 0.5f) * 350f) * physicsConfig.shakeInfluence
                        blob.vy += ((Random.nextFloat() - 0.5f) * 350f) * physicsConfig.shakeInfluence
                    }
                }
            }

            // D. Viscous Fluid Necking, Pinch-Off, and Re-Merging (Liquid physics stretching)
            if (interactive) {
                val currentTouch = touchPosition
                if (currentTouch != null) {
                    if (fluidImage != null) {
                        // In fluid image mode: directly attach the closest node to the finger
                        val hasAttachedFinger = blobs.any { it.isAttachedToFinger }
                        if (!hasAttachedFinger) {
                            var closestBlob: LavaBlob? = null
                            var minDist = Float.MAX_VALUE
                            blobs.forEach { b ->
                                if (b.x != -1f) {
                                    val dx = currentTouch.x - b.x
                                    val dy = currentTouch.y - b.y
                                    val dist = hypot(dx, dy)
                                    if (dist < minDist) {
                                        minDist = dist
                                        closestBlob = b
                                    }
                                }
                            }
                            closestBlob?.let { b ->
                                if (minDist < b.baseRadius * 2.5f) {
                                    b.isAttachedToFinger = true
                                }
                            }
                        }
                    } else {
                        // Standard lava lamp: elastic daughter blob spawning
                        val listForStretch = blobs.toList()
                        val hasAttachedFinger = listForStretch.any { it.isAttachedToFinger }
                        if (!hasAttachedFinger) {
                            for (parent in listForStretch) {
                                if (parent.x != -1f && parent.connectedBlobId == -1 && !parent.isAttachedToFinger) {
                                    val dx = currentTouch.x - parent.x
                                    val dy = currentTouch.y - parent.y
                                    val dist = hypot(dx, dy)
                                    val stretchThreshold = parent.baseRadius * 1.4f
                                    if (dist < stretchThreshold) {
                                        // Spawn daughter liquid droplet with 0.65f radius for thicker gooey necking!
                                        val daughterRadius = parent.baseRadius * 0.65f
                                        val daughterBlob = LavaBlob(
                                            color = parent.color,
                                            baseRadius = daughterRadius,
                                            x = currentTouch.x,
                                            y = currentTouch.y,
                                            vx = touchVelocity.x * 0.2f * physicsConfig.touchInfluence,
                                            vy = touchVelocity.y * 0.2f * physicsConfig.touchInfluence,
                                            imageIndex = parent.imageIndex,
                                            connectedBlobId = parent.id,
                                            isAttachedToFinger = true
                                        )
                                        parent.connectedBlobId = daughterBlob.id
                                        blobs.add(daughterBlob)
                                        break // Initiate only one stretching droplet per touch frame
                                    }
                                }
                            }
                        }
                    }

                    // Move all attached blobs directly
                    blobs.forEach { b ->
                        if (b.isAttachedToFinger) {
                            b.x = b.x * 0.72f + currentTouch.x * 0.28f
                            b.y = b.y * 0.72f + currentTouch.y * 0.28f
                            b.vx = touchVelocity.x * physicsConfig.touchInfluence
                            b.vy = touchVelocity.y * physicsConfig.touchInfluence
                        }
                    }
                } else {
                    // If touch is released, clear all finger attachments
                    blobs.forEach { it.isAttachedToFinger = false }
                }

                // Process spring dynamics (only for standard lava lamp mode)
                if (fluidImage == null) {
                    val listForSprings = blobs.toList()
                    val daughtersToRemove = mutableListOf<LavaBlob>()
                    val parentConnectionsToClear = mutableListOf<LavaBlob>()

                    for (blobA in listForSprings) {
                        if (blobA.connectedBlobId != -1) {
                            val blobB = listForSprings.find { it.id == blobA.connectedBlobId }
                            if (blobB != null) {
                                // Compute liquid neck length (distance)
                                val dx = blobB.x - blobA.x
                                val dy = blobB.y - blobA.y
                                val dist = hypot(dx, dy)

                                if (dist > 5f) {
                                    // Apply viscous spring force between parent and daughter (liquid neck surface tension)
                                    val springStrength = 0.09f
                                    val restLength = blobA.baseRadius * 0.15f
                                    val displacement = dist - restLength
                                    if (displacement > 0) {
                                        val fx = (dx / dist) * displacement * springStrength
                                        val fy = (dy / dist) * displacement * springStrength

                                        // Pull them back together like thick elastic mercury
                                        blobA.vx += fx * 15f
                                        blobA.vy += fy * 15f
                                        blobB.vx -= fx * 15f
                                        blobB.vy -= fy * 15f
                                    }
                                }

                                // A. Pinch-Off Trigger: If neck stretches beyond threshold, snap the neck and release droplet!
                                val pinchOffLimit = blobA.baseRadius * 2.3f
                                if (dist > pinchOffLimit) {
                                    blobA.connectedBlobId = -1
                                    blobB.connectedBlobId = -1
                                    blobB.isAttachedToFinger = false
                                    // Trigger surface tension snap vibration recoil on both parent and daughter!
                                    blobA.snapRecoilTime = 0f
                                    blobB.snapRecoilTime = 0f
                                    // Give the daughter droplet a physical fling matching finger velocity
                                    blobB.vx = touchVelocity.x * 0.7f * physicsConfig.touchInfluence
                                    blobB.vy = touchVelocity.y * 0.7f * physicsConfig.touchInfluence
                                }

                                // B. Re-Merge Snap-Back: If touch released and they snap back together, merge into one!
                                if (currentTouch == null && dist < blobA.baseRadius * 0.65f) {
                                    daughtersToRemove.add(blobB)
                                    parentConnectionsToClear.add(blobA)
                                }
                            }
                        }
                    }

                    if (daughtersToRemove.isNotEmpty()) {
                        blobs.removeAll(daughtersToRemove)
                        parentConnectionsToClear.forEach { it.connectedBlobId = -1 }
                    }
                }
            }
            // 2.5 Calculate SPH-like Volumetric Interaction (Repulsion for volume + Attraction for cohesion/surface tension)
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
                    val interactionRadius = minDist * 1.6f // Surface tension reach
                    
                    if (dist < interactionRadius && dist > 1f) {
                        if (dist < minDist) {
                            // Volumetric Overlap Repulsion (prevent collapse)
                            val overlapRatio = (minDist - dist) / minDist
                            val pushForce = overlapRatio * physicsConfig.softRepulsion * 4f
                            val fx = (dx / dist) * pushForce
                            val fy = (dy / dist) * pushForce
                            
                            blobI.vx -= fx * physicsConfig.smoothingWeight
                            blobI.vy -= fy * physicsConfig.smoothingWeight
                            blobJ.vx += fx * physicsConfig.smoothingWeight
                            blobJ.vy += fy * physicsConfig.smoothingWeight
                        } else {
                            // Surface Tension / Cohesion Attraction
                            val gap = dist - minDist
                            val maxGap = interactionRadius - minDist
                            val pullFactor = 1f - (gap / maxGap)
                            val pullForce = pullFactor * 350f // Strong smooth attraction
                            
                            val fx = (dx / dist) * pullForce
                            val fy = (dy / dist) * pullForce
                            
                            blobI.vx += fx * physicsConfig.smoothingWeight
                            blobI.vy += fy * physicsConfig.smoothingWeight
                            blobJ.vx -= fx * physicsConfig.smoothingWeight
                            blobJ.vy -= fy * physicsConfig.smoothingWeight
                        }

                        // Physical Color Mixing
                        if (enableColorMixing && dist < interactionRadius) {
                            val mixRate = (colorMixingRate * delta * 2f).coerceIn(0f, 1f)
                            val mixedColor = blendColorsHSV(blobI.color, blobJ.color, 0.5f)
                            blobI.color = lerpColorHSV(blobI.color, mixedColor, mixRate)
                            blobJ.color = lerpColorHSV(blobJ.color, mixedColor, mixRate)
                        }
                    }
                }
            }

            blobs.forEach { blob ->
                // Progress snap recoil oscillation time
                if (blob.snapRecoilTime != -1f) {
                    blob.snapRecoilTime += delta
                    if (blob.snapRecoilTime > 1.0f) {
                        blob.snapRecoilTime = -1f
                    }
                }

                // Initialize inside the bottle coordinates
                if (blob.x == -1f) {
                    // Fully random position across entire glass width and height
                    blob.x = centerX + (Random.nextFloat() - 0.5f) * (lampWidth * 0.7f)
                    blob.y = glassTop + Random.nextFloat() * lampHeight
                }

                // A. Vertical Buoyancy / Gravity force
                var verticalBuoyancy = when (gravityMode) {
                    LavaGravity.UP -> -110f
                    LavaGravity.DOWN -> 110f
                    LavaGravity.ZERO_GRAVITY -> 0f
                }
                
                // Gentle horizontal sin wave drift
                var horizontalDrift = sin(time * 0.5f + blob.phaseX) * 20f

                // In Liquid Image restoration mode, scale down drift & gravity so they don't combat restoration,
                // while still letting them play if restoration strength is set to 0.
                if (fluidImage != null && imageRestorationStrength > 0f) {
                    val suppression = (1f - (imageRestorationStrength * 5f).coerceIn(0f, 1f))
                    verticalBuoyancy *= suppression
                    horizontalDrift *= suppression
                }

                // B. Viscous Accelerometer Tilt Gravity sliding (Gentle horizontal drift only)
                var tiltForceX = tiltOffset.x * 12f
                var tiltForceY = tiltOffset.y * 4f
                if (fluidImage != null && !enableTiltDeformation) {
                    tiltForceX = 0f
                    tiltForceY = 0f
                }

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
                            val strength = pullFactor * 160f * physicsConfig.touchInfluence
                            touchForceX = (dx / dist) * strength
                            touchForceY = (dy / dist) * strength
                        }
                        
                        // 2. Strong Swipe Momentum (Fluid flow created by finger drag)
                        val dragRadius = lampWidth * 1.2f
                        if (dist < dragRadius && touchVelocity != Offset.Zero) {
                            val dragFactor = (1f - dist / dragRadius)
                            dragForceX = touchVelocity.x * dragFactor * 0.45f * physicsConfig.touchInfluence
                            dragForceY = touchVelocity.y * dragFactor * 0.45f * physicsConfig.touchInfluence
                        }
                    }
                }

                // D. Apply high-viscosity fluid drag (adjustable damping and smoothing weights from physicsConfig)
                var vxInput = horizontalDrift + tiltForceX + touchForceX + dragForceX
                var vyInput = verticalBuoyancy + tiltForceY + touchForceY + dragForceY

                // Organic Spring Restoration Force for Fluid Images
                if (fluidImage != null && imageRestorationStrength > 0f && blob.originalX != -1f) {
                    val rx = blob.originalX - blob.x
                    val ry = blob.originalY - blob.y
                    
                    vxInput += rx * imageRestorationStrength * 160f
                    vyInput += ry * imageRestorationStrength * 160f
                }

                blob.vx = blob.vx * physicsConfig.damping + vxInput * physicsConfig.smoothingWeight
                blob.vy = blob.vy * physicsConfig.damping + vyInput * physicsConfig.smoothingWeight

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

                // F. Dynamic breathing size (scaled by blobScale)
                val currentRadius = blob.baseRadius * blobScale * (1f + 0.12f * sin(time * blob.scaleSpeed + blob.scalePhase))

                // G. Obstacle Collision Deflection (Slide and deflect smoothly around rectangular UI obstacles)
                currentObstacles.forEach { rect ->
                    val closestX = blob.x.coerceIn(rect.left, rect.right)
                    val closestY = blob.y.coerceIn(rect.top, rect.bottom)
                    val dx = blob.x - closestX
                    val dy = blob.y - closestY
                    val dist = hypot(dx, dy)

                    if (dist > 0.1f) {
                        val contactRadius = currentRadius * 1.25f
                        if (dist < contactRadius) {
                            val overlap = contactRadius - dist
                            // Push position slightly outside
                            blob.x += (dx / dist) * overlap * 0.15f
                            blob.y += (dy / dist) * overlap * 0.15f
                            
                            // Apply deflection velocity normal to the obstacle surface
                            val pushForce = (overlap / contactRadius) * 550f
                            blob.vx += (dx / dist) * pushForce * physicsConfig.smoothingWeight
                            blob.vy += (dy / dist) * pushForce * physicsConfig.smoothingWeight
                        }
                    } else {
                        // Blob center is inside the rectangle; eject it to the closest edge
                        val dl = blob.x - rect.left
                        val dr = rect.right - blob.x
                        val dt = blob.y - rect.top
                        val db = rect.bottom - blob.y
                        val minDistance = minOf(dl, dr, dt, db)
                        when (minDistance) {
                            dl -> {
                                blob.x = rect.left - currentRadius
                                blob.vx = -kotlin.math.abs(blob.vx) * 0.5f
                            }
                            dr -> {
                                blob.x = rect.right + currentRadius
                                blob.vx = kotlin.math.abs(blob.vx) * 0.5f
                            }
                            dt -> {
                                blob.y = rect.top - currentRadius
                                blob.vy = -kotlin.math.abs(blob.vy) * 0.5f
                            }
                            db -> {
                                blob.y = rect.bottom + currentRadius
                                blob.vy = kotlin.math.abs(blob.vy) * 0.5f
                            }
                        }
                    }
                }

                if (containerMode == LavaContainerMode.GLASS_BOTTLE) {
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
                    if (blob.y < glassTop + currentRadius * 0.5f) {
                        blob.y = glassTop + currentRadius * 0.5f
                        blob.vy *= -0.2f
                    } else if (blob.y > glassBottom - currentRadius * 0.5f) {
                        blob.y = glassBottom - currentRadius * 0.5f
                        blob.vy *= -0.2f
                    }
                } else {
                    // AMBIENT_BACKGROUND Bounds (Fullscreen wrap)
                    if (blob.x < -currentRadius) blob.x = width + currentRadius
                    else if (blob.x > width + currentRadius) blob.x = -currentRadius
                    
                    if (blob.y < -currentRadius * 2f) blob.y = height + currentRadius * 2f
                    else if (blob.y > height + currentRadius * 2f) blob.y = -currentRadius * 2f
                }
            }

            // D. Update ambient micro-particles
            if (enableParticles && particles.isNotEmpty()) {
                val particleList = particles.toList()
                val lampWidth = width * 0.55f
                val lampHeight = height * 0.75f
                val glassTop = centerY - lampHeight / 2f
                val glassBottom = centerY + lampHeight / 2f
                val activePhysicsBlobs = blobs.toList()
                
                for (p in particleList) {
                    // Convection flow (drifts up or down depending on gravityMode)
                    val convectionForce = when (gravityMode) {
                        LavaGravity.UP -> -40f
                        LavaGravity.DOWN -> 40f
                        LavaGravity.ZERO_GRAVITY -> 0f
                    }
                    
                    // Device tilt sloshing force on micro-dust
                    val tiltForceX = tiltOffset.x * 2.5f
                    val tiltForceY = tiltOffset.y * 1.0f
                    
                    p.vx += tiltForceX * delta
                    p.vy += (convectionForce + tiltForceY) * delta
                    
                    // Radial repulsion displacement from all floating lava blobs
                    for (blob in activePhysicsBlobs) {
                        if (blob.x == -1f) continue
                        val bRadius = blob.baseRadius * blobScale * (1f + 0.12f * sin(time * blob.scaleSpeed + blob.scalePhase))
                        val dx = p.x - blob.x
                        val dy = p.y - blob.y
                        val dist = hypot(dx, dy)
                        val influenceRadius = bRadius * 1.4f
                        
                        if (dist < influenceRadius && dist > 1f) {
                            val forceFactor = (1f - dist / influenceRadius)
                            // Soft physical displacement push normal to blob surface
                            val pushForce = forceFactor * 160f
                            p.vx += (dx / dist) * pushForce * delta
                            p.vy += (dy / dist) * pushForce * delta
                        }
                    }
                    
                    // Apply liquid viscosity drag on particles
                    p.vx *= 0.90f
                    p.vy *= 0.90f
                    
                    // Apply position updates
                    p.x += p.vx * delta
                    p.y += p.vy * delta
                    
                    // Boundary wrapping for beautiful continuous particle drift
                    if (containerMode == LavaContainerMode.GLASS_BOTTLE) {
                        val relativeHeightProgress = (p.y - glassTop) / lampHeight
                        val activeGlassWidth = lampWidth * (0.6f + relativeHeightProgress * 0.4f)
                        val minX = centerX - activeGlassWidth / 2f
                        val maxX = centerX + activeGlassWidth / 2f
                        
                        if (p.x < minX) {
                            p.x = maxX - 2f
                            p.vx = -kotlin.math.abs(p.vx) * 0.1f
                        } else if (p.x > maxX) {
                            p.x = minX + 2f
                            p.vx = kotlin.math.abs(p.vx) * 0.1f
                        }
                        
                        if (p.y < glassTop) {
                            p.y = glassBottom - 5f
                            p.x = centerX + (Random.nextFloat() - 0.5f) * lampWidth * 0.7f
                        } else if (p.y > glassBottom) {
                            p.y = glassTop + 5f
                            p.x = centerX + (Random.nextFloat() - 0.5f) * lampWidth * 0.5f
                        }
                    } else {
                        // Fullscreen wrapping
                        if (p.x < 0f) p.x = width
                        else if (p.x > width) p.x = 0f
                        
                        if (p.y < 0f) p.y = height
                        else if (p.y > height) p.y = 0f
                    }
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
            .graphicsLayer { rotationZ = lampRotation }
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
            if (containerMode == LavaContainerMode.GLASS_BOTTLE) {
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
            } // end showGlassBottle Layer 1

            // Helper function for graphicsLayer modifier to avoid repeating the RenderEffect code
            val metaballGraphicsLayer: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {
                val isEmulator = Build.FINGERPRINT.contains("generic") ||
                               Build.FINGERPRINT.contains("unknown") ||
                               Build.MODEL.contains("google_sdk") ||
                               Build.MODEL.contains("Emulator") ||
                               Build.MODEL.contains("Android SDK built for x86") ||
                               Build.BRAND.contains("generic") ||
                               Build.DEVICE.contains("generic")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isEmulator && fluidImage == null) {
                    val (blurRadius, alphaMul, alphaAdd) = when (viscosity) {
                        LavaViscosity.WATER -> Triple(18f, 70f, -3500f)
                        LavaViscosity.STANDARD -> Triple(32f, 45f, -2250f)
                        LavaViscosity.THICK_HONEY -> Triple(50f, 30f, -1500f)
                    }

                    val blur = android.graphics.RenderEffect.createBlurEffect(
                        blurRadius, blurRadius, android.graphics.Shader.TileMode.CLAMP
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shaderConfig.enabled && runtimeShader != null) {
                        val thresholdVal = when (viscosity) {
                            LavaViscosity.WATER -> 0.21f
                            LavaViscosity.STANDARD -> 0.20f
                            LavaViscosity.THICK_HONEY -> 0.18f
                        }
                        runtimeShader.setFloatUniform("threshold", thresholdVal)
                        runtimeShader.setFloatUniform("refractionStrength", shaderConfig.refractionStrength)
                        runtimeShader.setFloatUniform("lightDir", shaderConfig.lightDirectionX, shaderConfig.lightDirectionY, 0.7f)
                        runtimeShader.setFloatUniform("lightColor", 1.0f, 1.0f, 1.0f)
                        runtimeShader.setFloatUniform("specularIntensity", shaderConfig.specularIntensity)
                        runtimeShader.setFloatUniform("specularPower", shaderConfig.specularPower)

                        val shaderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(
                            runtimeShader, "inputShader"
                        )
                        renderEffect = android.graphics.RenderEffect.createChainEffect(shaderEffect, blur)
                            .asComposeRenderEffect()
                    } else {
                        val matrix = floatArrayOf(
                            1f, 0f, 0f, 0f, 0f,
                            0f, 1f, 0f, 0f, 0f,
                            0f, 0f, 1f, 0f, 0f,
                            0f, 0f, 0f, alphaMul, alphaAdd
                        )
                        val colorFilter = android.graphics.RenderEffect.createColorFilterEffect(
                            ColorMatrixColorFilter(matrix)
                        )
                        renderEffect = android.graphics.RenderEffect.createChainEffect(colorFilter, blur)
                            .asComposeRenderEffect()
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(pointerModifier)
                    .graphicsLayer(block = metaballGraphicsLayer)
            ) {
                val drawTime = time

                // Conditionally clip blobs to the glass path or draw freely as background
                val doDrawBlobs: () -> Unit = {
                    // A. Draw Tapered Glowing Bottom Reservoir (only inside bottle)
                    if (containerMode == LavaContainerMode.GLASS_BOTTLE) {
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
                    }

                    // B. Draw Tapered Glowing Top Reservoir (only inside bottle)
                    if (containerMode == LavaContainerMode.GLASS_BOTTLE) {
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
                    }

                    // C. Draw Floating Dynamic Physics-Based Blobs
                    val cols = if (fluidImage != null) kotlin.math.sqrt(blobs.size.toDouble()).toInt().coerceAtLeast(1) else 1
                    val rows = if (fluidImage != null) (blobs.size + cols - 1) / cols else 1
                    
                    val imgTargetWidth = if (fluidImage != null) {
                        val imageRatio = fluidImage.width.toFloat() / fluidImage.height.toFloat()
                        val containerRatio = width / height
                        if (containerMode == LavaContainerMode.GLASS_BOTTLE) lampWidth * 0.8f else {
                            if (imageRatio > containerRatio) width else height * imageRatio
                        }
                    } else if (containerMode == LavaContainerMode.GLASS_BOTTLE) lampWidth * 0.8f else width * 0.8f

                    val imgTargetHeight = if (fluidImage != null) {
                        val imageRatio = fluidImage.width.toFloat() / fluidImage.height.toFloat()
                        val containerRatio = width / height
                        if (containerMode == LavaContainerMode.GLASS_BOTTLE) lampHeight * 0.7f else {
                            if (imageRatio > containerRatio) width / imageRatio else height
                        }
                    } else if (containerMode == LavaContainerMode.GLASS_BOTTLE) lampHeight * 0.7f else height * 0.7f
                    
                    val cellW = imgTargetWidth / cols
                    val cellH = imgTargetHeight / rows

                    blobs.forEachIndexed { index, blob ->
                        if (blob.x == -1f) return@forEachIndexed

                        val pulseFactor = if (pulseSpeed > 0f) (1f + 0.15f * sin(time * pulseSpeed)) else 1f
                        
                        // Dynamic frequency scale factor mapping
                        val bandValue = if (audioFrequencyBands.isNotEmpty()) {
                            audioFrequencyBands[index % audioFrequencyBands.size].coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        
                        val audioScaleFactor = 1f + bandValue * 1.5f
                        var radius = blob.baseRadius * blobScale * pulseFactor * audioScaleFactor * (1f + 0.12f * sin(drawTime * blob.scaleSpeed + blob.scalePhase))

                        if (blob.connectedBlobId != -1) {
                            val sibling = blobs.find { it.id == blob.connectedBlobId }
                            if (sibling != null) {
                                val dx = sibling.x - blob.x
                                val dy = sibling.y - blob.y
                                val dist = hypot(dx, dy)
                                val limit = blob.baseRadius * 2.3f
                                val stretchRatio = (dist / limit).coerceIn(0f, 1f)
                                radius *= (1.0f - stretchRatio * 0.28f)
                            }
                        }

                        if (blob.snapRecoilTime != -1f) {
                            val t = blob.snapRecoilTime
                            val recoilScale = 1f - 0.24f * kotlin.math.cos(t * 16f) * kotlin.math.exp(-t * 4.5f)
                            radius *= recoilScale
                        }

                        // Thermal & Kinetic Color Shift
                        val verticalRatio = ((blob.y - glassTop) / lampHeight).coerceIn(0f, 1f)
                        val speedFactor = hypot(blob.vx, blob.vy) / 380f // 380 is maxSpeed
                        
                        // Warmer colors at bottom, slightly cooler/darker at top
                        // Higher speed = brighter glow
                        val rFactor = (0.8f + 0.2f * verticalRatio + 0.1f * speedFactor).coerceIn(0f, 1f)
                        val gFactor = (0.9f + 0.1f * speedFactor).coerceIn(0f, 1f)
                        val bFactor = (1.0f - 0.2f * verticalRatio + 0.1f * speedFactor).coerceIn(0f, 1f)
                        
                        val thermalColor = Color(
                            red = blob.color.red * rFactor,
                            green = blob.color.green * gFactor,
                            blue = blob.color.blue * bFactor,
                            alpha = blob.color.alpha
                        )

                        // Audio dynamic white blend glow color shift
                        val resolvedColor = if (bandValue > 0.05f) {
                            val blendFactor = bandValue * 0.85f
                            Color(
                                red = thermalColor.red * (1f - blendFactor) + 1.0f * blendFactor,
                                green = thermalColor.green * (1f - blendFactor) + 1.0f * blendFactor,
                                blue = thermalColor.blue * (1f - blendFactor) + 1.0f * blendFactor,
                                alpha = thermalColor.alpha
                            )
                        } else {
                            thermalColor
                        }

                        if (fluidImage != null && blob.originalX != -1f) {
                            val overlap = 1.01f
                            val drawCellW = cellW * overlap
                            val drawCellH = cellH * overlap
                            drawImage(
                                image = fluidImage,
                                srcOffset = IntOffset(blob.srcX.toInt(), blob.srcY.toInt()),
                                srcSize = IntSize(blob.srcW.toInt(), blob.srcH.toInt()),
                                dstOffset = IntOffset((blob.x - drawCellW / 2f).toInt(), (blob.y - drawCellH / 2f).toInt()),
                                dstSize = IntSize(drawCellW.toInt(), drawCellH.toInt())
                            )
                        } else if (blobImages != null && blobImages.isNotEmpty()) {
                            val bitmap = blobImages[blob.imageIndex % blobImages.size]
                            drawImage(
                                image = bitmap,
                                dstOffset = IntOffset((blob.x - radius).toInt(), (blob.y - radius).toInt()),
                                dstSize = IntSize((radius * 2).toInt(), (radius * 2).toInt())
                            )
                        } else {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0.0f to resolvedColor,
                                    0.45f to resolvedColor,
                                    0.75f to resolvedColor.copy(alpha = 0.7f),
                                    1.0f to Color.Transparent,
                                    center = Offset(blob.x, blob.y),
                                    radius = radius
                                ),
                                radius = radius,
                                center = Offset(blob.x, blob.y),
                                blendMode = BlendMode.Plus
                            )
                        }
                    }

                    // D. Draw Floating Ambient Micro-Particles (glowing neon dust floating through the viscous chamber)
                    if (enableParticles && particles.isNotEmpty()) {
                        particles.forEach { p ->
                            val colorIndex = (p.speedPhase * 5f).toInt().coerceAtLeast(0)
                            val baseParticleColor = activeColors.getOrNull(colorIndex % activeColors.size) ?: Color.White
                            val particleColor = Color(
                                red = baseParticleColor.red * 0.4f + 0.6f,
                                green = baseParticleColor.green * 0.4f + 0.6f,
                                blue = baseParticleColor.blue * 0.4f + 0.6f,
                                alpha = p.alpha
                            )
                            
                            drawCircle(
                                color = particleColor,
                                radius = p.radius,
                                center = Offset(p.x, p.y)
                            )
                        }
                    }
                }

                if (containerMode == LavaContainerMode.GLASS_BOTTLE) {
                    clipPath(glassPath) { doDrawBlobs() }
                } else {
                    doDrawBlobs()
                }
            }

            // LAYER 3: Sharp Gloss reflections, metallic base and cap (only when bottle is shown and 3D style requested)
            if (containerMode == LavaContainerMode.GLASS_BOTTLE && glassStyle == LavaGlassStyle.REALISTIC_3D) {
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
                        Rect(0, 0, size.width, size.height),
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
            } // end showGlassBottle Layer 3
        }
    }
}

private const val AGSL_SHADER = """
    uniform shader inputShader;
    uniform float threshold;
    uniform float refractionStrength;
    uniform float3 lightDir;
    uniform float3 lightColor;
    uniform float specularIntensity;
    uniform float specularPower;

    half4 main(float2 coord) {
        half4 color = inputShader.eval(coord);
        
        if (color.a < threshold) {
            return half4(0.0);
        }
        
        // Liquid Color Normalization (Metaball Color Blending)
        // Decode pre-multiplied additive colors resulting from BlendMode.Plus
        float3 fluidColor = color.rgb / max(color.a, 0.0001);
        color.rgb = clamp(fluidColor, 0.0, 1.0);
        
        float delta = 2.0; 
        float a_left  = inputShader.eval(coord + float2(-delta, 0.0)).a;
        float a_right = inputShader.eval(coord + float2(delta, 0.0)).a;
        float a_up    = inputShader.eval(coord + float2(0.0, -delta)).a;
        float a_down  = inputShader.eval(coord + float2(0.0, delta)).a;
        
        float3 normal = normalize(float3(a_left - a_right, a_up - a_down, 0.15));
        
        float2 refractedCoord = coord + normal.xy * refractionStrength;
        half4 refractedColor = inputShader.eval(refractedCoord);
        
        float3 N = normal;
        float3 L = normalize(lightDir);
        float3 V = float3(0.0, 0.0, 1.0);
        
        float diffuse = max(dot(N, L), 0.0);
        
        float3 H = normalize(L + V);
        float specular = pow(max(dot(N, H), 0.0), specularPower) * specularIntensity;
        
        float edgeGlow = smoothstep(threshold + 0.12, threshold, color.a);
        
        float3 baseColor = mix(color.rgb, refractedColor.rgb, 0.25);
        
        float3 finalColor = baseColor * (0.8 + 0.45 * diffuse) + lightColor * specular + baseColor * edgeGlow * 0.35;
        
        float finalAlpha = smoothstep(threshold, threshold + 0.02, color.a);
        
        return half4(finalColor, finalAlpha);
    }
"""

private fun blendColorsHSV(color1: Color, color2: Color, fraction: Float): Color {
    return lerpColorHSV(color1, color2, fraction)
}

private fun lerpColorHSV(color1: Color, color2: Color, fraction: Float): Color {
    val hsv1 = FloatArray(3)
    val hsv2 = FloatArray(3)
    android.graphics.Color.colorToHSV(color1.toArgb(), hsv1)
    android.graphics.Color.colorToHSV(color2.toArgb(), hsv2)
    
    var h1 = hsv1[0]
    var h2 = hsv2[0]
    val diff = h2 - h1
    if (diff > 180f) {
        h1 += 360f
    } else if (diff < -180f) {
        h2 += 360f
    }
    
    val h = ((h1 + (h2 - h1) * fraction) % 360f + 360f) % 360f
    val s = hsv1[1] + (hsv2[1] - hsv1[1]) * fraction
    val v = hsv1[2] + (hsv2[2] - hsv1[2]) * fraction
    val a = color1.alpha + (color2.alpha - color1.alpha) * fraction
    
    val blendedInt = android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))
    return Color(blendedInt)
}
