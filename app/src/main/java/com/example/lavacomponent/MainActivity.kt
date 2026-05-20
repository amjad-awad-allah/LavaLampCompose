package com.example.lavacomponent

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.lavalamp.LavaLamp
import com.example.lavalamp.LavaLampStyle
import com.example.lavalamp.LavaShaderConfig
import com.example.lavalamp.LavaMode
import com.example.lavalamp.LavaBackground
import com.example.lavalamp.LavaPhysicsConfig
import com.example.lavalamp.LavaContainerMode
import com.example.lavalamp.LavaGravity
import com.example.lavalamp.LavaGlassStyle
import com.example.lavalamp.LavaViscosity
import com.example.lavalamp.LavaAudioProcessor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

enum class Screen { HOME, SPLASH, LOGIN, CUSTOM_OBJECTS, SANDBOX, OBSTACLE_DEMO, FLUID_IMAGE_DEMO, AUDIO_REACTIVE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf(Screen.HOME) }
            MaterialTheme {
                Crossfade(targetState = currentScreen, label = "nav") { screen ->
                    when (screen) {
                        Screen.HOME          -> HomeScreen         { currentScreen = it }
                        Screen.SPLASH        -> SplashDemoScreen   { currentScreen = Screen.HOME }
                        Screen.LOGIN         -> LoginDemoScreen     { currentScreen = Screen.HOME }
                        Screen.CUSTOM_OBJECTS-> CustomObjectsScreen { currentScreen = Screen.HOME }
                        Screen.SANDBOX       -> SandboxScreen       { currentScreen = Screen.HOME }
                        Screen.OBSTACLE_DEMO -> ObstacleDemoScreen  { currentScreen = Screen.HOME }
                        Screen.FLUID_IMAGE_DEMO -> FluidImageDemoScreen { currentScreen = Screen.HOME }
                        Screen.AUDIO_REACTIVE -> AudioReactiveDemoScreen { currentScreen = Screen.HOME }
                    }
                }
            }
        }
    }
}

// =======================================================================
// HOME SCREEN
// =======================================================================
@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF08050F))) {

        // Full screen background fluid — no bottle
        LavaLamp(
            modifier = Modifier.fillMaxSize().blur(32.dp),
            blobCount = 5,
            speed = 0.35f,
            flowIntensity = 0.15f,
            containerMode = LavaContainerMode.AMBIENT_BACKGROUND,
            interactive = false,
            mode = LavaMode.Vector(LavaLampStyle.CYBERPUNK),
            physicsConfig = LavaPhysicsConfig(touchInfluence = 0f)
        )

        // Dark gradient overlay to improve text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(0.55f), Color.Black.copy(0.2f), Color.Black.copy(0.7f))
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 48.dp, bottom = 52.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "LavaLamp",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color(0xFF00FFCC).copy(0.6f),
                                blurRadius = 40f
                            )
                        )
                    )
                    Text(
                        text = "Jetpack Compose Fluid Physics",
                        fontSize = 13.sp,
                        color = Color.White.copy(0.75f),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Text("LIVE EXAMPLES", color = Color.White.copy(0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Start)
            }

            // Mini Showcase: Thick Honey
            item {
                MiniShowcaseCard(title = "Thick Honey Viscosity", desc = "High blur and sticky threshold") {
                    LavaLamp(
                        modifier = Modifier.fillMaxSize(),
                        blobCount = 4,
                        speed = 0.5f,
                        viscosity = LavaViscosity.THICK_HONEY,
                        mode = LavaMode.Vector(LavaLampStyle.CLASSIC_70S),
                        containerMode = LavaContainerMode.AMBIENT_BACKGROUND
                    )
                }
            }

            // Mini Showcase: Water
            item {
                MiniShowcaseCard(title = "Water Viscosity", desc = "Thin droplets, fast splitting") {
                    LavaLamp(
                        modifier = Modifier.fillMaxSize(),
                        blobCount = 15,
                        blobScale = 0.6f,
                        speed = 1.5f,
                        viscosity = LavaViscosity.WATER,
                        mode = LavaMode.Vector(LavaLampStyle.DEEP_OCEAN),
                        containerMode = LavaContainerMode.AMBIENT_BACKGROUND
                    )
                }
            }

            // Mini Showcase: Pulse Effect
            item {
                MiniShowcaseCard(title = "Pulsing Effect", desc = "Blobs rhythmically breathe") {
                    LavaLamp(
                        modifier = Modifier.fillMaxSize(),
                        blobCount = 6,
                        pulseSpeed = 2f,
                        mode = LavaMode.Vector(LavaLampStyle.COTTON_CANDY),
                        containerMode = LavaContainerMode.AMBIENT_BACKGROUND
                    )
                }
            }

            // Mini Showcase: Reverse Gravity
            item {
                MiniShowcaseCard(title = "Reverse Gravity (Wax Rain)", desc = "Gravity pulls blobs downward") {
                    LavaLamp(
                        modifier = Modifier.fillMaxSize(),
                        blobCount = 8,
                        speed = 0.8f,
                        gravityMode = LavaGravity.DOWN,
                        mode = LavaMode.Vector(LavaLampStyle.VOLCANIC),
                        containerMode = LavaContainerMode.AMBIENT_BACKGROUND
                    )
                }
            }

            // Mini Showcase: Zero Gravity
            item {
                MiniShowcaseCard(title = "Zero Gravity Space", desc = "Blobs drift peacefully in place") {
                    LavaLamp(
                        modifier = Modifier.fillMaxSize(),
                        blobCount = 5,
                        speed = 0.2f,
                        gravityMode = LavaGravity.ZERO_GRAVITY,
                        mode = LavaMode.Vector(LavaLampStyle.AURORA_FOREST),
                        containerMode = LavaContainerMode.AMBIENT_BACKGROUND
                    )
                }
            }

            // Mini Showcase: Flat 2D Style
            item {
                MiniShowcaseCard(title = "Flat Minimalist Bottle", desc = "Vector style without 3D glass reflections") {
                    LavaLamp(
                        modifier = Modifier.fillMaxSize(),
                        blobCount = 6,
                        speed = 0.6f,
                        glassStyle = LavaGlassStyle.FLAT_2D,
                        containerMode = LavaContainerMode.GLASS_BOTTLE,
                        mode = LavaMode.Vector(LavaLampStyle.CYBERPUNK)
                    )
                }
            }

            item {
                Text("INTERACTIVE DEMOS", color = Color.White.copy(0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), textAlign = TextAlign.Start)
            }

            // Feature cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    NavCard(
                        emoji = "🎬", title = "Splash Screen",
                        desc = "Fluid logo animation as app loading screen",
                        accent = Color(0xFF8A2BE2)
                    ) { onNavigate(Screen.SPLASH) }

                    NavCard(
                        emoji = "🏠", title = "Background Mode",
                        desc = "Calm fluid running behind real UI components",
                        accent = Color(0xFF00BCD4)
                    ) { onNavigate(Screen.LOGIN) }

                    NavCard(
                        emoji = "🔮", title = "Custom PNG Objects",
                        desc = "Your own images melting & floating as fluid",
                        accent = Color(0xFFE91E63)
                    ) { onNavigate(Screen.CUSTOM_OBJECTS) }

                    NavCard(
                        emoji = "⚙️", title = "Physics Sandbox",
                        desc = "Live-tune every physics parameter in real time",
                        accent = Color(0xFF4CAF50)
                    ) { onNavigate(Screen.SANDBOX) }

                    NavCard(
                        emoji = "🚧", title = "Obstacle Deflection",
                        desc = "Metaballs dynamically flow around real interactive UI buttons",
                        accent = Color(0xFFFF9800)
                    ) { onNavigate(Screen.OBSTACLE_DEMO) }

                    NavCard(
                        emoji = "🎨", title = "Liquid Image Warp",
                        desc = "Melt any image like a fluid jigsaw and restore it organically",
                        accent = Color(0xFF00FFCC)
                    ) { onNavigate(Screen.FLUID_IMAGE_DEMO) }

                    NavCard(
                        emoji = "🎙️", title = "Audio-Reactive & Particles",
                        desc = "Metaballs dance and dust shifts to sound frequencies in real time",
                        accent = Color(0xFFFFCC00)
                    ) { onNavigate(Screen.AUDIO_REACTIVE) }
                }
            }
        }
    }
}

@Composable
fun MiniShowcaseCard(title: String, desc: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161622)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f))))
                    .padding(16.dp)
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(desc, color = Color.White.copy(0.6f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun NavCard(emoji: String, title: String, desc: String, accent: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(listOf(accent.copy(0.25f), Color(0xFF1A1A2E).copy(0.9f))),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(52.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 24.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(desc, color = Color.White.copy(0.55f), fontSize = 12.sp)
            }
            Text("›", color = accent, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

// =======================================================================
// SPLASH SCREEN DEMO
// =======================================================================
@Composable
fun SplashDemoScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val splashImages = remember {
        val opts = BitmapFactory.Options().apply { inScaled = false }
        val b1 = BitmapFactory.decodeResource(context.resources, R.drawable.img1, opts)
        if (b1 != null) listOf(b1.asImageBitmap()) else emptyList()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF060410))) {

        // The image IS the fluid — large blobs, no bottle
        if (splashImages.isNotEmpty()) {
            LavaLamp(
                modifier = Modifier.fillMaxSize(),
                blobCount = 3,        // Few large blobs so image is clearly visible
                blobScale = 2.0f,
                speed = 0.7f,
                flowIntensity = 0.5f,
                containerMode = LavaContainerMode.AMBIENT_BACKGROUND,
                mode = LavaMode.Png(splashImages),
                physicsConfig = LavaPhysicsConfig(
                    softRepulsion = 80f,
                    touchInfluence = 1.5f,
                    shakeInfluence = 1.5f
                )
            )
        }

        ShowcaseHeader(
            title = "Splash Screen",
            subtitle = "Logo becomes the fluid itself",
            label = "USE CASE 1",
            color = Color(0xFF8A2BE2),
            onBack = onBack
        )

        // Bottom hint
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFF8A2BE2),
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp
            )
            Text("Touch to interact • Shake to split", color = Color.White.copy(0.6f), fontSize = 12.sp)
        }


    }
}

// =======================================================================
// LOGIN / BACKGROUND DEMO
// =======================================================================
@Composable
fun LoginDemoScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    Box(modifier = Modifier.fillMaxSize()) {

        // The fluid is the background — fully visible, no bottle
        LavaLamp(
            modifier = Modifier.fillMaxSize(),
            blobCount = 6,
            speed = 0.18f,
            flowIntensity = 0.08f,
            containerMode = LavaContainerMode.AMBIENT_BACKGROUND,
            mode = LavaMode.Vector(LavaLampStyle.AURORA_FOREST),
            interactive = false,
            physicsConfig = LavaPhysicsConfig(touchInfluence = 0f, shakeInfluence = 0f)
        )

        // Frosted dark overlay
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.38f))
        )

        ShowcaseHeader(
            title = "Background Mode",
            subtitle = "Calm fluid behind real UI",
            label = "USE CASE 2",
            color = Color(0xFF00BCD4),
            onBack = onBack
        )

        // Glassmorphism login card
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D1A).copy(alpha = 0.78f)),
            border = BorderStroke(1.dp, Color.White.copy(0.12f)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Welcome Back", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = "user@example.com", onValueChange = {},
                    label = { Text("Email", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00BCD4),
                        unfocusedBorderColor = Color.White.copy(0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = "••••••••", onValueChange = {},
                    label = { Text("Password", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00BCD4),
                        unfocusedBorderColor = Color.White.copy(0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                TextButton(onClick = {}) {
                    Text("Don't have an account? Sign Up", color = Color.White.copy(0.5f), fontSize = 12.sp)
                }
            }
        }


    }
}

// =======================================================================
// CUSTOM PNG OBJECTS DEMO
// =======================================================================
@Composable
fun CustomObjectsScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val customImages = remember {
        val opts = BitmapFactory.Options().apply { inScaled = false }

        val b2 = BitmapFactory.decodeResource(context.resources, R.drawable.img2, opts)
        listOfNotNull(
            b2?.asImageBitmap(),
            b2?.asImageBitmap()
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF080512))) {

        if (customImages.isNotEmpty()) {
            LavaLamp(
                modifier = Modifier.fillMaxSize(),
                blobCount = 1,
                blobScale = 4f,
                speed = 0.2f,
                flowIntensity = 0.55f,
                containerMode = LavaContainerMode.AMBIENT_BACKGROUND,
                mode = LavaMode.Png(customImages),
                physicsConfig = LavaPhysicsConfig(
                    softRepulsion = 140f,
                    touchInfluence = 2.0f,
                    shakeInfluence = 2.0f
                )
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Could not load images", color = Color.Red)
            }
        }

        ShowcaseHeader(
            title = "Custom PNG Objects",
            subtitle = "Your images become the fluid",
            label = "USE CASE 3",
            color = Color(0xFFE91E63),
            onBack = onBack
        )

        // Bottom tip
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(0.5f))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                "👆 Drag to separate  •  📱 Shake to explode",
                color = Color.White.copy(0.75f), fontSize = 13.sp, textAlign = TextAlign.Center
            )
        }


    }
}

// =======================================================================
// PHYSICS SANDBOX
// =======================================================================
@Composable
fun SandboxScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    var selectedStyle by remember { mutableStateOf(LavaLampStyle.CYBERPUNK) }
    var flowIntensity by remember { mutableFloatStateOf(0.4f) }
    var blobCount by remember { mutableIntStateOf(5) }
    var touchInfluence by remember { mutableFloatStateOf(1.0f) }
    var shakeInfluence by remember { mutableFloatStateOf(1.0f) }
    var lampRotation by remember { mutableFloatStateOf(0f) }
    var blobScale by remember { mutableFloatStateOf(1.0f) }
    var isInteractive by remember { mutableStateOf(true) }
    var isSensorReactive by remember { mutableStateOf(false) }
    var hasNoiseOverlay by remember { mutableStateOf(true) }
    var gravityMode by remember { mutableStateOf(LavaGravity.UP) }
    var glassStyle by remember { mutableStateOf(LavaGlassStyle.REALISTIC_3D) }
    var viscosity by remember { mutableStateOf(LavaViscosity.STANDARD) }
    var pulseSpeed by remember { mutableFloatStateOf(0f) }
    var isExpanded by remember { mutableStateOf(true) }

    // 3D Shader states
    var isShaderEnabled by remember { mutableStateOf(true) }
    var refractionStrength by remember { mutableFloatStateOf(12f) }
    var specularIntensity by remember { mutableFloatStateOf(0.75f) }
    var specularPower by remember { mutableFloatStateOf(25f) }
    var lightDirectionX by remember { mutableFloatStateOf(0.5f) }
    var lightDirectionY by remember { mutableFloatStateOf(-0.5f) }

    val stylesList = remember {
        LavaLampStyle.values().toList()
    }
    val viscosityList = remember {
        LavaViscosity.values().toList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LavaLamp(
            modifier = Modifier.fillMaxSize(),
            blobCount = blobCount,
            blobScale = blobScale,
            speed = 1.0f,
            flowIntensity = flowIntensity,
            interactive = isInteractive,
            sensorReactive = isSensorReactive,
            noiseOverlay = hasNoiseOverlay,
            lampRotation = lampRotation,
            gravityMode = gravityMode,
            containerMode = LavaContainerMode.GLASS_BOTTLE,
            glassStyle = glassStyle,
            viscosity = viscosity,
            pulseSpeed = pulseSpeed,
            mode = LavaMode.Vector(selectedStyle),
            physicsConfig = LavaPhysicsConfig(
                touchInfluence = touchInfluence,
                shakeInfluence = shakeInfluence
            ),
            shaderConfig = LavaShaderConfig(
                enabled = isShaderEnabled,
                refractionStrength = refractionStrength,
                specularIntensity = specularIntensity,
                specularPower = specularPower,
                lightDirectionX = lightDirectionX,
                lightDirectionY = lightDirectionY
            )
        )

        ShowcaseHeader(
            title = "Physics Sandbox",
            subtitle = "Live-tune physics parameters",
            label = "USE CASE 4",
            color = Color(0xFF4CAF50),
            onBack = onBack
        )



        // Controls card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF12121E)),
            border = BorderStroke(1.dp, Color.White.copy(0.12f)),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 32.dp)
                .fillMaxWidth()
                .heightIn(max = 380.dp)
                .pointerInput(Unit) {}
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Controls", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(36.dp)) {
                        Text(if (isExpanded) "▼" else "▲", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
                AnimatedVisibility(visible = isExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        HorizontalDivider(color = Color.White.copy(0.1f))

                        // Style selector
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            items(stylesList) { s ->
                                Button(
                                    onClick = { selectedStyle = s },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedStyle == s) Color(0xFF4CAF50) else Color(0xFF1E1E2E)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(s.name.replace("_", " "), fontSize = 10.sp, textAlign = TextAlign.Center, color = Color.White)
                                }
                            }
                        }

                        // Viscosity selector
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            viscosityList.forEach { v ->
                                Button(
                                    onClick = { viscosity = v },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (viscosity == v) Color(0xFFE91E63) else Color(0xFF1E1E2E)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(v.name.replace("_", " "), fontSize = 9.sp, textAlign = TextAlign.Center, color = Color.White)
                                }
                            }
                        }

                        SandboxSlider("BLOBS", "$blobCount", blobCount.toFloat(), 2f..20f) { blobCount = it.toInt() }
                        SandboxSlider("BLOB SCALE", "%.1fx".format(blobScale), blobScale, 0.5f..2.5f) { blobScale = it }
                        SandboxSlider("ROTATION", "${lampRotation.toInt()}°", lampRotation, 0f..180f) { lampRotation = it }
                        SandboxSlider("PULSE SPEED", "%.1fx".format(pulseSpeed), pulseSpeed, 0f..5f) { pulseSpeed = it }

                        HorizontalDivider(color = Color.White.copy(0.08f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            SandboxToggle("GRAVITY DOWN", gravityMode == LavaGravity.DOWN) { 
                                gravityMode = if (it) LavaGravity.DOWN else LavaGravity.UP 
                            }
                            SandboxToggle("FLAT 2D", glassStyle == LavaGlassStyle.FLAT_2D) { 
                                glassStyle = if (it) LavaGlassStyle.FLAT_2D else LavaGlassStyle.REALISTIC_3D 
                            }
                            SandboxToggle("GRAIN", hasNoiseOverlay) { hasNoiseOverlay = it }
                            SandboxToggle("3D SHADER", isShaderEnabled) { isShaderEnabled = it }
                        }

                        if (isShaderEnabled) {
                            HorizontalDivider(color = Color.White.copy(0.08f))
                            Text("3D Shader Tuning", color = Color.White.copy(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            SandboxSlider("REFRACTION", "%.1f".format(refractionStrength), refractionStrength, 0f..30f) { refractionStrength = it }
                            SandboxSlider("SPECULAR LIGHT", "%.2f".format(specularIntensity), specularIntensity, 0f..2f) { specularIntensity = it }
                            SandboxSlider("SPECULAR POWER", "${specularPower.toInt()}", specularPower, 5f..100f) { specularPower = it }
                            SandboxSlider("LIGHT DIR X", "%.2f".format(lightDirectionX), lightDirectionX, -2.0f..2.0f) { lightDirectionX = it }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SandboxSlider(label: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>, onChanged: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White.copy(0.55f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(valueText, color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
        Slider(
            value = value, onValueChange = onChanged, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF4CAF50), activeTrackColor = Color(0xFF4CAF50).copy(0.5f), inactiveTrackColor = Color(0xFF1E1E2E))
        )
    }
}

@Composable
fun SandboxToggle(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Color.White.copy(0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Switch(
            checked = checked, onCheckedChange = onChanged,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF4CAF50), checkedTrackColor = Color(0xFF1E3A1E), uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color(0xFF1E1E2E))
        )
    }
}

// =======================================================================
// SHARED: Unified Header & Back Button
// =======================================================================
@Composable
fun BoxScope.ShowcaseHeader(title: String, subtitle: String, label: String, color: Color, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.5f)),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Text("←", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = Color.White.copy(0.6f), fontSize = 11.sp)
        }
        
        Spacer(modifier = Modifier.size(44.dp)) // Balances the back button width to keep text perfectly centered
    }
}

// =======================================================================
// OBSTACLE COLLISION SHOWCASE SCREEN
// =======================================================================
@Composable
fun ObstacleDemoScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    
    // Position offset for the draggable card
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    // Bounds of obstacles
    var cardObstacleRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var backButtonObstacleRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var floatingButtonObstacleRect by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    
    val obstacles = remember(cardObstacleRect, backButtonObstacleRect, floatingButtonObstacleRect) {
        listOfNotNull(cardObstacleRect, backButtonObstacleRect, floatingButtonObstacleRect)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF06060E))) {
        // LavaLamp background
        LavaLamp(
            modifier = Modifier.fillMaxSize(),
            blobCount = 8,
            blobScale = 1.1f,
            speed = 0.8f,
            flowIntensity = 0.4f,
            containerMode = LavaContainerMode.AMBIENT_BACKGROUND,
            mode = LavaMode.Vector(LavaLampStyle.VOLCANIC),
            interactive = true,
            obstacles = obstacles
        )

        // Title and Back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.Black.copy(0.4f), CircleShape)
                    .border(1.dp, Color.White.copy(0.15f), CircleShape)
                    .onGloballyPositioned { backButtonObstacleRect = it.boundsInParent() }
            ) {
                Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Obstacle Deflection", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("UI elements physically push & deflect metaballs", color = Color.White.copy(0.5f), fontSize = 12.sp)
            }
        }

        // Draggable Glassmorphic Card (Obstacle 1)
        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                .align(Alignment.Center)
                .padding(horizontal = 40.dp)
                .fillMaxWidth()
                .onGloballyPositioned { cardObstacleRect = it.boundsInParent() }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E).copy(alpha = 0.82f)),
            border = BorderStroke(1.dp, Color(0xFFFF9800).copy(0.25f)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🚧 Interactive Card", color = Color(0xFFFF9800), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Drag this card around the screen! The metaballs will dynamically slide and deflect around its borders in real time.",
                    color = Color.White.copy(0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Interactive Static Circle Button (Obstacle 2)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
                .size(72.dp)
                .background(Color(0xFFE91E63), CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .onGloballyPositioned { floatingButtonObstacleRect = it.boundsInParent() }
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("💥", color = Color.White, fontSize = 24.sp)
        }
    }
}

// =======================================================================
// LIQUID IMAGE WARP & ORGANIC RESTORATION SHOWCASE
// =======================================================================
@Composable
fun FluidImageDemoScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    
    var blobCount by remember { mutableIntStateOf(4) } //
    var imageRestorationStrength by remember { mutableFloatStateOf(0.04f) }
    var enableTiltDeformation by remember { mutableStateOf(true) }
    // showChamber removed per user request
    var isExpanded by remember { mutableStateOf(true) }
    
    var selectedImageIndex by remember { mutableIntStateOf(0) }
    
    val imageBitmaps = remember {
        val opts = BitmapFactory.Options().apply { inScaled = false }
        val b1 = BitmapFactory.decodeResource(context.resources, R.drawable.img1, opts)
        val b2 = BitmapFactory.decodeResource(context.resources, R.drawable.img2, opts)
        listOfNotNull(b1?.asImageBitmap(), b2?.asImageBitmap())
    }

    val activeImage = if (imageBitmaps.isNotEmpty()) imageBitmaps[selectedImageIndex % imageBitmaps.size] else null

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040209))) {
        if (activeImage != null) {
            LavaLamp(
                modifier = Modifier.fillMaxSize(),
                blobCount = blobCount,
                blobScale = 1.2f,
                speed = 1.0f,
                flowIntensity = 0.5f,
                interactive = true,
                sensorReactive = enableTiltDeformation,
                containerMode = LavaContainerMode.AMBIENT_BACKGROUND,
                fluidImage = activeImage,
                imageRestorationStrength = imageRestorationStrength,
                enableTiltDeformation = enableTiltDeformation,
                viscosity = LavaViscosity.STANDARD
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: Loading Image Asset", color = Color.Red, fontSize = 16.sp)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.Black.copy(0.5f), CircleShape)
                    .border(1.dp, Color.White.copy(0.15f), CircleShape)
            ) {
                Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Liquid Image Warp", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Smear & distort the image with touch, watch it restore organically", color = Color.White.copy(0.55f), fontSize = 12.sp)
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 105.dp, end = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(0.6f))
                    .border(1.dp, Color(0xFF00FFCC).copy(0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("👉 Swipe image to melt it!\n📱 Tilt device to slosh!", color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF100C1A).copy(alpha = 0.88f)),
            border = BorderStroke(1.dp, Color.White.copy(0.12f)),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth()
                .heightIn(max = 380.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Liquid Configuration", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(36.dp)) {
                        Text(if (isExpanded) "▼" else "▲", color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        HorizontalDivider(color = Color.White.copy(0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Select Image:", color = Color.White.copy(0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Button(
                                onClick = { selectedImageIndex = 0 },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedImageIndex == 0) Color(0xFF00FFCC) else Color(0xFF1E1E2E)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Logo", fontSize = 10.sp, color = if (selectedImageIndex == 0) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { selectedImageIndex = 1 },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedImageIndex == 1) Color(0xFF00FFCC) else Color(0xFF1E1E2E)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Art Pattern", fontSize = 10.sp, color = if (selectedImageIndex == 1) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("RESTORATION SPEED (FLUIDITY)", color = Color.White.copy(0.55f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (imageRestorationStrength == 0f) "DISABLED (MELTS FOREVER)" else "%.2f".format(imageRestorationStrength),
                                    color = Color(0xFF00FFCC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Slider(
                                value = imageRestorationStrength,
                                onValueChange = { imageRestorationStrength = it },
                                valueRange = 0f..0.2f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00FFCC),
                                    activeTrackColor = Color(0xFF00FFCC).copy(0.5f),
                                    inactiveTrackColor = Color(0xFF1E1E2E)
                                )
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("GRID RESOLUTION (METABALLS COUNT)", color = Color.White.copy(0.55f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("$blobCount blobs", color = Color(0xFF00FFCC), fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                            Slider(
                                value = blobCount.toFloat(),
                                onValueChange = { blobCount = it.toInt() },
                                valueRange = 9f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00FFCC),
                                    activeTrackColor = Color(0xFF00FFCC).copy(0.5f),
                                    inactiveTrackColor = Color(0xFF1E1E2E)
                                )
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(0.08f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("TILT WARP", color = Color.White.copy(0.5f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = enableTiltDeformation,
                                    onCheckedChange = { enableTiltDeformation = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00FFCC),
                                        checkedTrackColor = Color(0xFF004433),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color(0xFF1E1E2E)
                                    )
                                )
                            }

                            // Glass bottle toggle removed

                            var triggerReset by remember { mutableStateOf(false) }
                            LaunchedEffect(triggerReset) {
                                if (triggerReset) {
                                    val previousStrength = imageRestorationStrength
                                    imageRestorationStrength = 0.2f
                                    kotlinx.coroutines.delay(1000)
                                    imageRestorationStrength = previousStrength
                                    triggerReset = false
                                }
                            }

                            Button(
                                onClick = { triggerReset = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text("⚡ Restore Image", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =======================================================================
// AUDIO-REACTIVE & AMBIENT MICRO-PARTICLES SHOWCASE
// =======================================================================
@Composable
fun AudioReactiveDemoScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current

    // State for dynamic audio permission check
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher contract
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    // Interactive custom state sliders
    var enableParticles by remember { mutableStateOf(true) }
    var particleCount by remember { mutableFloatStateOf(60f) }
    var audioInfluence by remember { mutableFloatStateOf(1.0f) }
    var selectedStyle by remember { mutableStateOf(LavaLampStyle.CYBERPUNK) }
    var isExpanded by remember { mutableStateOf(true) }

    // Frequency bands driving physics and rendering
    var bassState by remember { mutableFloatStateOf(0f) }
    var midsState by remember { mutableFloatStateOf(0f) }
    var highsState by remember { mutableFloatStateOf(0f) }
    var overallAmplitudeState by remember { mutableFloatStateOf(0f) }

    // Start audio record processor loop when permission is available
    if (hasPermission) {
        DisposableEffect(Unit) {
            val processor = LavaAudioProcessor(context).apply {
                onSpectrumUpdated = { b, m, h, overall ->
                    bassState = b
                    midsState = m
                    highsState = h
                    overallAmplitudeState = overall
                }
            }
            processor.start()
            onDispose {
                processor.stop()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF07040D))) {
        if (hasPermission) {
            LavaLamp(
                modifier = Modifier.fillMaxSize(),
                blobCount = 8,
                blobScale = 1.1f + bassState * 0.35f, // Grow blobs physically on deep bass hits
                speed = 0.8f,
                flowIntensity = 0.45f,
                containerMode = LavaContainerMode.GLASS_BOTTLE,
                mode = LavaMode.Vector(selectedStyle),
                enableParticles = enableParticles,
                particleCount = particleCount.toInt(),
                audioInfluence = audioInfluence,
                audioFrequencyBands = listOf(bassState, midsState, highsState),
                interactive = true
            )
        }

        // Skeuomorphic glass header
        ShowcaseHeader(
            title = "Audio & Micro-Particles",
            subtitle = "Fluid metaballs and dust reactive to sound",
            label = "USE CASE 5",
            color = Color(0xFFFFCC00),
            onBack = onBack
        )

        if (!hasPermission) {
            // High fidelity dark frosted glass card prompting for microphone permission
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13101E).copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, Color.White.copy(0.12f)),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🎙️ Microphone Access Required",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "This premium visualizer performs real-time audio separation (Bass, Mids, Highs) through your microphone to dynamically slosh liquid metaballs and float ambient neon dust particles.",
                        color = Color.White.copy(0.6f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFCC00)),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Grant Record Permission",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            // Elegant glassmorphic floating controller
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF100C1B).copy(alpha = 0.88f)),
                border = BorderStroke(1.dp, Color.White.copy(0.12f)),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Audio Controls & Spectrum", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(36.dp)) {
                            Text(if (isExpanded) "▼" else "▲", color = Color(0xFFFFCC00), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            HorizontalDivider(color = Color.White.copy(0.1f))

                            // Dynamic Style Palette Selector
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                items(LavaLampStyle.values().toList()) { style ->
                                    Button(
                                        onClick = { selectedStyle = style },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedStyle == style) Color(0xFFFFCC00) else Color(0xFF1E1E2E)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = style.name.replace("_", " "),
                                            fontSize = 10.sp,
                                            color = if (selectedStyle == style) Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Dynamic Live Real-Time Spectrum Visualizer
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("REAL-TIME FREQUENCY SPECTRUM", color = Color.White.copy(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .background(Color.Black.copy(0.3f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // BASS BAR (Deep Violet / Pink Glow)
                                    SpectrumBar(
                                        label = "BASS",
                                        value = bassState,
                                        color = Color(0xFFFF1493),
                                        modifier = Modifier.weight(1f)
                                    )
                                    // MIDS BAR (Electric Blue Glow)
                                    SpectrumBar(
                                        label = "MIDS",
                                        value = midsState,
                                        color = Color(0xFF00BFFF),
                                        modifier = Modifier.weight(1f)
                                    )
                                    // HIGHS BAR (Neon Green / Orange Glow)
                                    SpectrumBar(
                                        label = "HIGHS",
                                        value = highsState,
                                        color = Color(0xFF00FF66),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(0.08f))

                            // Audio Influence Sensitivity Tuning
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("AUDIO SENSITIVITY (DANCE SPEED)", color = Color.White.copy(0.55f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "%.1fx".format(audioInfluence),
                                        color = Color(0xFFFFCC00),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Slider(
                                    value = audioInfluence,
                                    onValueChange = { audioInfluence = it },
                                    valueRange = 0f..2.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFCC00),
                                        activeTrackColor = Color(0xFFFFCC00).copy(0.5f),
                                        inactiveTrackColor = Color(0xFF1E1E2E)
                                    )
                                )
                            }

                            // Dynamic Ambient Micro-Particles Density Control
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("PARTICLE DENSITY (NEON DUST)", color = Color.White.copy(0.55f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (enableParticles) "${particleCount.toInt()} particles" else "DISABLED",
                                        color = Color(0xFFFFCC00),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Slider(
                                    value = particleCount,
                                    onValueChange = { particleCount = it },
                                    valueRange = 10f..120f,
                                    enabled = enableParticles,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFCC00),
                                        activeTrackColor = Color(0xFFFFCC00).copy(0.5f),
                                        inactiveTrackColor = Color(0xFF1E1E2E)
                                    )
                                )
                            }

                            HorizontalDivider(color = Color.White.copy(0.08f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✨ Enable Ambient Particles Layer", color = Color.White.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Switch(
                                    checked = enableParticles,
                                    onCheckedChange = { enableParticles = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFFFCC00),
                                        checkedTrackColor = Color(0xFF664400),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color(0xFF1E1E2E)
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

@Composable
fun SpectrumBar(label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(0.65f),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Background slot
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(0.06f))
            )
            // Animated level height
            Box(
                modifier = Modifier
                    .fillMaxHeight(value.coerceIn(0.01f, 1f))
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(color, color.copy(alpha = 0.5f))
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color.White.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}
