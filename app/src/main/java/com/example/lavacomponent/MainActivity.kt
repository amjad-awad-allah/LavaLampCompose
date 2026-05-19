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

import com.example.lavalamp.LavaLamp
import com.example.lavalamp.LavaLampStyle
import com.example.lavalamp.LavaMode
import com.example.lavalamp.LavaBackground
import com.example.lavalamp.LavaPhysicsConfig
import com.example.lavalamp.LavaContainerMode

enum class Screen { HOME, SPLASH, LOGIN, CUSTOM_OBJECTS, SANDBOX }

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
            modifier = Modifier.fillMaxSize(),
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

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 72.dp)
            ) {
                Text(
                    "🌋", fontSize = 52.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "LavaLamp", color = Color.White, fontSize = 36.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 1.sp
                )
                Text(
                    "Jetpack Compose Physics Library",
                    color = Color(0xFF00FFFF).copy(alpha = 0.85f),
                    fontSize = 13.sp, fontWeight = FontWeight.Medium
                )
            }

            // Feature cards
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(bottom = 52.dp)) {
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

        // Top label
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "USE CASE 1", color = Color(0xFF8A2BE2),
                fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Splash Screen", color = Color.White,
                fontSize = 26.sp, fontWeight = FontWeight.Black
            )
            Text(
                "Logo becomes the fluid itself",
                color = Color.White.copy(0.55f), fontSize = 13.sp
            )
        }

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

        BackBtn(onBack)
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

        // USE CASE label
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "USE CASE 2", color = Color(0xFF00BCD4),
                fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text("Background Mode", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("Calm fluid behind real UI", color = Color.White.copy(0.55f), fontSize = 13.sp)
        }

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

        BackBtn(onBack)
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
        val b1 = BitmapFactory.decodeResource(context.resources, R.drawable.img1, opts)
        val b2 = BitmapFactory.decodeResource(context.resources, R.drawable.img2, opts)
        listOfNotNull(
            b1?.asImageBitmap(),
            b2?.asImageBitmap()
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF080512))) {

        if (customImages.isNotEmpty()) {
            LavaLamp(
                modifier = Modifier.fillMaxSize(),
                blobCount = 6,
                speed = 0.9f,
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

        // Top label
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "USE CASE 3", color = Color(0xFFE91E63),
                fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text("Custom PNG Objects", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("Your images become the fluid", color = Color.White.copy(0.55f), fontSize = 13.sp)
        }

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

        BackBtn(onBack)
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
    var isInteractive by remember { mutableStateOf(true) }
    var isSensorReactive by remember { mutableStateOf(false) }
    var hasNoiseOverlay by remember { mutableStateOf(true) }
    var isExpanded by remember { mutableStateOf(true) }

    val stylesList = remember {
        listOf(LavaLampStyle.CYBERPUNK, LavaLampStyle.VOLCANIC, LavaLampStyle.LIQUID_MERCURY, LavaLampStyle.AURORA_FOREST)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LavaLamp(
            modifier = Modifier.fillMaxSize(),
            blobCount = blobCount,
            speed = 1.0f,
            flowIntensity = flowIntensity,
            interactive = isInteractive,
            sensorReactive = isSensorReactive,
            noiseOverlay = hasNoiseOverlay,
            lampRotation = lampRotation,
            containerMode = LavaContainerMode.GLASS_BOTTLE,
            mode = LavaMode.Vector(selectedStyle),
            physicsConfig = LavaPhysicsConfig(
                touchInfluence = touchInfluence,
                shakeInfluence = shakeInfluence
            )
        )

        // USE CASE label
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "USE CASE 4", color = Color(0xFF4CAF50),
                fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
            )
            Text("Physics Sandbox", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }

        BackBtn(onBack)

        // Controls card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF12121E)),
            border = BorderStroke(1.dp, Color.White.copy(0.12f)),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 32.dp)
                .fillMaxWidth()
                .wrapContentHeight()
                .pointerInput(Unit) {}
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            stylesList.forEach { s ->
                                Button(
                                    onClick = { selectedStyle = s },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selectedStyle == s) Color(0xFF4CAF50) else Color(0xFF1E1E2E)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(s.name.replace("_", "\n"), fontSize = 8.sp, textAlign = TextAlign.Center, color = Color.White)
                                }
                            }
                        }

                        SandboxSlider("BLOBS", "$blobCount", blobCount.toFloat(), 2f..10f) { blobCount = it.toInt() }
                        SandboxSlider("TOUCH INFLUENCE", "${(touchInfluence * 100).toInt()}%", touchInfluence, 0f..3f) { touchInfluence = it }
                        SandboxSlider("SHAKE INFLUENCE", "${(shakeInfluence * 100).toInt()}%", shakeInfluence, 0f..3f) { shakeInfluence = it }
                        SandboxSlider("ROTATION", "${lampRotation.toInt()}°", lampRotation, 0f..180f) { lampRotation = it }

                        HorizontalDivider(color = Color.White.copy(0.08f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            SandboxToggle("TOUCH", isInteractive) { isInteractive = it }
                            SandboxToggle("SENSOR", isSensorReactive) { isSensorReactive = it }
                            SandboxToggle("GRAIN", hasNoiseOverlay) { hasNoiseOverlay = it }
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
// SHARED: Back Button
// =======================================================================
@Composable
fun BoxScope.BackBtn(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.55f)),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.align(Alignment.TopStart).padding(top = 16.dp, start = 16.dp)
    ) {
        Text("← Back", color = Color.White, fontSize = 13.sp)
    }
}
