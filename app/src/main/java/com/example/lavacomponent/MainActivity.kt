package com.example.lavacomponent
 
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
 
import com.example.lavalamp.LavaLamp
import com.example.lavalamp.LavaLampStyle
import com.example.lavalamp.LavaMode
import com.example.lavalamp.LavaBackground
import com.example.lavalamp.LavaPhysicsConfig
// Utility 1: Programmatically draw beautiful text/emojis on a transparent Bitmap as PNG assets
fun createEmojiBitmap(text: String, size: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint().apply {
        textSize = size * 0.72f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }
    // Perform precise vertical typography centering
    val y = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f)
    canvas.drawText(text, size / 2f, y, paint)
    return bitmap.asImageBitmap()
}

// Utility 2: Programmatically render a gorgeous neon grid backdrop for the glass chamber
fun createChamberBackgroundBitmap(width: Int, height: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    // Draw deep neon night chamber background
    canvas.drawColor(android.graphics.Color.parseColor("#0F0A24"))
    
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#341C61")
        strokeWidth = 3f
    }
    
    // Draw horizontal grid lines
    for (i in 0..height step 40) {
        canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), paint)
    }
    // Draw vertical lines
    for (i in 0..width step 40) {
        canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paint)
    }
    
    // Draw a gorgeous glowing cyber pink sun in the center of the bottle
    val glowPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#FF007F")
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 10f
    }
    canvas.drawCircle(width / 2f, height / 2f, 100f, glowPaint)
    
    return bitmap.asImageBitmap()
}

// Utility 3: Programmatically render a gorgeous starfield sky background for the whole component
fun createWholeBackgroundBitmap(width: Int, height: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
    }
    
    // Smooth deep vertical night-sky space gradient
    val gradient = android.graphics.LinearGradient(
        0f, 0f, 0f, height.toFloat(),
        android.graphics.Color.parseColor("#080512"),
        android.graphics.Color.parseColor("#020104"),
        android.graphics.Shader.TileMode.CLAMP
    )
    paint.shader = gradient
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    
    // Draw sparkling tiny space stars
    val starPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = true
    }
    val random = java.util.Random()
    repeat(50) {
        val rx = random.nextFloat() * width
        val ry = random.nextFloat() * height
        val rRadius = random.nextFloat() * 2.5f + 1f
        starPaint.alpha = random.nextInt(160) + 95
        canvas.drawCircle(rx, ry, rRadius, starPaint)
    }
    
    return bitmap.asImageBitmap()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var selectedStyle by remember { mutableStateOf(LavaLampStyle.CYBERPUNK) }
            var flowIntensity by remember { mutableFloatStateOf(0.4f) }
            var blobCount by remember { mutableIntStateOf(5) }
            var isInteractive by remember { mutableStateOf(true) }
            var isSensorReactive by remember { mutableStateOf(false) }
            var hasNoiseOverlay by remember { mutableStateOf(true) }
            
            // Custom asset and background states
            var useCustomPNGs by remember { mutableStateOf(false) }
            var useCustomChamberBg by remember { mutableStateOf(false) }
            var useCustomWholeBg by remember { mutableStateOf(false) }
            
            // Collapsible State (true = expanded, false = collapsed)
            var isExpanded by remember { mutableStateOf(true) }

            // Pre-generate and cache custom transparent PNG-like emoji bitmaps
            val customEmojiBitmaps = remember {
                listOf(
                    createEmojiBitmap("💜", 160),
                    createEmojiBitmap("🔥", 160),
                    createEmojiBitmap("👾", 160),
                    createEmojiBitmap("⭐", 160),
                    createEmojiBitmap("🔮", 160),
                    createEmojiBitmap("🦄", 160)
                )
            }

            // Pre-generate and cache cyber neon grid backdrop for the bottle
            val customChamberBgBitmap = remember {
                createChamberBackgroundBitmap(400, 850)
            }

            // Pre-generate and cache deep space starfield sky background
            val customWholeBgBitmap = remember {
                createWholeBackgroundBitmap(800, 1600)
            }
 
            // Safe list of styles to avoid any array retrieval bugs
            val stylesList = remember {
                listOf(
                    LavaLampStyle.CYBERPUNK,
                    LavaLampStyle.VOLCANIC,
                    LavaLampStyle.LIQUID_MERCURY,
                    LavaLampStyle.AURORA_FOREST
                )
            }
 
            Box(modifier = Modifier.fillMaxSize()) {
                // 1. The Dynamic Lava Lamp Background (Perfect liquid metaballs & clean API)
                val currentMode = if (useCustomPNGs) {
                    LavaMode.Png(customEmojiBitmaps)
                } else {
                    LavaMode.Vector(selectedStyle)
                }
 
                val currentBackground = when {
                    useCustomChamberBg || useCustomWholeBg -> {
                        LavaBackground.Custom(
                            chamberImage = if (useCustomChamberBg) customChamberBgBitmap else null,
                            wholeImage = if (useCustomWholeBg) customWholeBgBitmap else null
                        )
                    }
                    else -> LavaBackground.StyleBackdrop
                }
 
                LavaLamp(
                    modifier = Modifier.fillMaxSize(),
                    blobCount = blobCount,
                    speed = 1.0f,
                    flowIntensity = flowIntensity,
                    interactive = isInteractive,
                    sensorReactive = isSensorReactive,
                    noiseOverlay = hasNoiseOverlay,
                    mode = currentMode,
                    background = currentBackground,
                    physicsConfig = LavaPhysicsConfig()
                )

                // 2. High-Contrast Overlay Card with absolute input protection and COLLAPSE support
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF12121E) // Premium deep high-contrast navy/black
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), // Modern glowing outline
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 40.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        // INTERCEPT ALL TOUCHES so clicking control sliders/switches never propagates to background Canvas
                        .pointerInput(Unit) {} 
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header Row (Always visible) with Collapse Toggle Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LavaLamp Sandbox",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            
                            // Expand/Collapse Button using standard elegant text arrows
                            IconButton(
                                onClick = { isExpanded = !isExpanded },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    text = if (isExpanded) "▼" else "▲", 
                                    color = Color(0xFF00FFFF), 
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        // Expandable Content with smooth default fade/slide animation
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                                // A. Curved Style Selector Buttons
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "LAVA STYLE PRESET", 
                                        color = Color.White.copy(alpha = 0.6f), 
                                        fontSize = 10.sp, 
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        stylesList.forEach { styleOption ->
                                            val isSelected = selectedStyle == styleOption
                                            Button(
                                                onClick = { selectedStyle = styleOption },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) Color(0xFF8A2BE2) else Color(0xFF232336),
                                                    contentColor = Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = styleOption.name.replace("_", " "),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }

                                // B. Flow Energy/Intensity Slider
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "FLOW INTENSITY", 
                                            color = Color.White.copy(alpha = 0.6f), 
                                            fontSize = 10.sp, 
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "${(flowIntensity * 100).toInt()}%", 
                                            color = Color(0xFF00FFFF), 
                                            fontSize = 12.sp, 
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Slider(
                                        value = flowIntensity,
                                        onValueChange = { flowIntensity = it },
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF8A2BE2),
                                            activeTrackColor = Color(0xFF8A2BE2).copy(alpha = 0.6f),
                                            inactiveTrackColor = Color(0xFF232336)
                                        )
                                    )
                                }

                                // C. Blob Count Slider
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "BLOB DENSITY (COUNT)", 
                                            color = Color.White.copy(alpha = 0.6f), 
                                            fontSize = 10.sp, 
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "$blobCount Blobs", 
                                            color = Color(0xFF00FFFF), 
                                            fontSize = 12.sp, 
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Slider(
                                        value = blobCount.toFloat(),
                                        onValueChange = { blobCount = it.toInt() },
                                        valueRange = 2f..10f,
                                        steps = 7, // Even integer steps for 2, 3, 4, 5, 6, 7, 8, 9, 10
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF8A2BE2),
                                            activeTrackColor = Color(0xFF8A2BE2).copy(alpha = 0.6f),
                                            inactiveTrackColor = Color(0xFF232336)
                                        )
                                    )
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                // C. Functional Toggle Controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Toggle 1
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("TOUCH EFFECT", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = isInteractive,
                                            onCheckedChange = { isInteractive = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF00FFFF),
                                                checkedTrackColor = Color(0xFF8A2BE2),
                                                uncheckedThumbColor = Color.Gray,
                                                uncheckedTrackColor = Color(0xFF232336)
                                            )
                                        )
                                    }

                                    // Toggle 2
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("ACCEL TILT", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = isSensorReactive,
                                            onCheckedChange = { isSensorReactive = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF00FFFF),
                                                checkedTrackColor = Color(0xFF8A2BE2),
                                                uncheckedThumbColor = Color.Gray,
                                                uncheckedTrackColor = Color(0xFF232336)
                                            )
                                        )
                                    }

                                    // Toggle 3
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("FILM GRAIN", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = hasNoiseOverlay,
                                            onCheckedChange = { hasNoiseOverlay = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF00FFFF),
                                                checkedTrackColor = Color(0xFF8A2BE2),
                                                uncheckedThumbColor = Color.Gray,
                                                uncheckedTrackColor = Color(0xFF232336)
                                            )
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                // D. Custom Assets & Background Toggles
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Toggle 4: Custom Emoji PNG Blobs
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("EMOJI BLOBS", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = useCustomPNGs,
                                            onCheckedChange = { useCustomPNGs = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF00FFFF),
                                                checkedTrackColor = Color(0xFF8A2BE2),
                                                uncheckedThumbColor = Color.Gray,
                                                uncheckedTrackColor = Color(0xFF232336)
                                            )
                                        )
                                    }

                                    // Toggle 5: Custom Cyber Grid Glass
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("CYBER GLASS", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = useCustomChamberBg,
                                            onCheckedChange = { useCustomChamberBg = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF00FFFF),
                                                checkedTrackColor = Color(0xFF8A2BE2),
                                                uncheckedThumbColor = Color.Gray,
                                                uncheckedTrackColor = Color(0xFF232336)
                                            )
                                        )
                                    }

                                    // Toggle 6: Custom Starfield Whole Background
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("STARFIELD BG", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = useCustomWholeBg,
                                            onCheckedChange = { useCustomWholeBg = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color(0xFF00FFFF),
                                                checkedTrackColor = Color(0xFF8A2BE2),
                                                uncheckedThumbColor = Color.Gray,
                                                uncheckedTrackColor = Color(0xFF232336)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
