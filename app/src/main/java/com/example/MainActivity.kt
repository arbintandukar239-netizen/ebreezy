package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ArcReactor
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.JarvisMode
import com.example.viewmodel.JarvisViewModel
import com.example.data.MessageEntity
import androidx.compose.ui.graphics.graphicsLayer
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        // Diagnostic status
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ask for standard permissions initially so voice and GPS can run immediately
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    JarvisAppContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun JarvisAppContent(
    modifier: Modifier = Modifier,
    viewModel: JarvisViewModel = viewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val userTitle by viewModel.userTitle.collectAsState()

    // 1. Check if User name is registered.
    Crossfade(
        targetState = userName.isEmpty(),
        animationSpec = tween(600),
        label = "login_screen_crossfade"
    ) { isEmpty ->
        if (isEmpty) {
            TerminalSetupScreen(
                onRegister = { name, title ->
                    viewModel.setUserNameAndTitle(name, title)
                },
                modifier = modifier
            )
        } else {
            HolographicCommandCenter(
                viewModel = viewModel,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalSetupScreen(
    onRegister: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var nameInput by remember { mutableStateOf("") }
    var selectedTitle by remember { mutableStateOf("Sir") }
    val titles = listOf("Sir", "Ma'am", "Commander", "Agent", "Specialist")

    // Sci-Fi sweep animation background
    val infiniteTransition = rememberInfiniteTransition(label = "setup_scan")
    val sweepY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .drawBehind {
                // Futuristic cyber grid lines mapping to our Vibrant Theme
                val gridGap = 40.dp.toPx()
                for (x in 0..size.width.toInt() step gridGap.toInt()) {
                    drawLine(
                        color = Color(0xFF22D3EE).copy(alpha = 0.05f),
                        start = Offset(x.toFloat(), 0f),
                        end = Offset(x.toFloat(), size.height),
                        strokeWidth = 1f
                    )
                }
                for (y in 0..size.height.toInt() step gridGap.toInt()) {
                    drawLine(
                        color = Color(0xFF22D3EE).copy(alpha = 0.05f),
                        start = Offset(0f, y.toFloat()),
                        end = Offset(size.width, y.toFloat()),
                        strokeWidth = 1f
                    )
                }
                // Simulated scanning HUD sweeping line
                val lineY = sweepY * size.height
                drawLine(
                    color = Color(0xFF22D3EE).copy(alpha = 0.25f),
                    start = Offset(0f, lineY),
                    end = Offset(size.width, lineY),
                    strokeWidth = 2.dp.toPx()
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .drawBehind {
                        drawCircle(
                            color = Color(0xFF22D3EE).copy(alpha = 0.2f),
                            radius = size.width / 2f + 10f,
                            style = Stroke(width = 2f)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Mainframe System Security Access",
                    tint = Color(0xFF22D3EE),
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "INITIALIZING COGNITIVE CORE",
                color = Color(0xFF22D3EE),
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "SYSTEM REGISTRATION AND PRIVILEGE DEPLOYMENT",
                color = Color(0xFF06B6D4).copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Text input custom styled for sci-fi HUD
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = {
                    Text(
                        "ENTER MAIN USER IDENTIFIER",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF22D3EE).copy(alpha = 0.8f)
                    )
                },
                placeholder = {
                    Text(
                        "e.g. Tony Stark",
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.3f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF22D3EE),
                    unfocusedBorderColor = Color(0xFF06B6D4).copy(alpha = 0.4f),
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF020617)
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_name_input")
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "CHOOSE PRIVILEGE PROTOCOL INDEX",
                color = Color(0xFF06B6D4),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            // Dynamic flow layout for Titles selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                titles.take(3).forEach { title ->
                    val isSelected = selectedTitle == title
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF22D3EE) else Color(0xFF06B6D4).copy(alpha = 0.3f),
                                RoundedCornerShape(4.dp)
                            )
                            .background(
                                if (isSelected) Color(0xFF06B6D4).copy(alpha = 0.15f) else Color.Transparent
                            )
                            .clickable { selectedTitle = title }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                titles.drop(3).forEach { title ->
                    val isSelected = selectedTitle == title
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF22D3EE) else Color(0xFF06B6D4).copy(alpha = 0.3f),
                                RoundedCornerShape(4.dp)
                            )
                            .background(
                                if (isSelected) Color(0xFF06B6D4).copy(alpha = 0.15f) else Color.Transparent
                            )
                            .clickable { selectedTitle = title }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            val isReady = nameInput.trim().isNotEmpty()
            Button(
                onClick = {
                    if (isReady) {
                        onRegister(nameInput.trim(), selectedTitle)
                    }
                },
                enabled = isReady,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22D3EE),
                    contentColor = Color(0xFF020617),
                    disabledContainerColor = Color(0xFF0F172A).copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("setup_sync_button")
            ) {
                Text(
                    text = "BOOT CENTRAL COMM PROTOCOL",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun HolographicCommandCenter(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val jarvisMode by viewModel.jarvisMode.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val textInput by viewModel.textInput.collectAsState()
    val volumeIntensity by viewModel.volumeIntensity.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userTitle by viewModel.userTitle.collectAsState()

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Active Dashboard Tab
    var activeTab by remember { mutableStateOf("matrix") } // "matrix" or "terminal"

    // Auto-scroll conversational terminal on new updates
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .drawBehind {
                // Background radial-gradient dot layout representing Tailwind's dot grid
                val dotRadius = 1.2f
                val spacing = 20.dp.toPx()
                val dotColor = Color(0xFF22D3EE).copy(alpha = 0.08f)
                var x = 0f
                while (x < size.width) {
                    var y = 0f
                    while (y < size.height) {
                        drawCircle(
                            color = dotColor,
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                        y += spacing
                    }
                    x += spacing
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Futuristic Header (System Status & Running Clock)
            SystemHeader(isOnline = isOnline)

            // Divider system trace
            HorizontalTraceLine()

            // Tab bar switcher (Sleek sci-fi borders)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TabSelectorButton(
                    text = "SYSTEM MATRIX",
                    isSelected = activeTab == "matrix",
                    onClick = { activeTab = "matrix" },
                    modifier = Modifier.weight(1f)
                )
                TabSelectorButton(
                    text = "TERMINAL CONSOLE",
                    isSelected = activeTab == "terminal",
                    onClick = { activeTab = "terminal" },
                    modifier = Modifier.weight(1f)
                )
            }

            // Central Space Content Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (activeTab == "matrix") {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 2. Personalized Hello Greeting Block
                        GreetingStatsHeader(userName = userName, userTitle = userTitle, isOnline = isOnline)

                        // 3. Central Core Arc Reactor with concentric border rings & badges
                        Box(
                            modifier = Modifier
                                .size(260.dp)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Floating concentric rings representing the scifi CSS layers
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = Color(0xFF22D3EE).copy(alpha = 0.05f),
                                    radius = size.width / 2.1f,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                                drawCircle(
                                    color = Color(0xFF22D3EE).copy(alpha = 0.1f),
                                    radius = size.width / 2.3f,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }

                            ArcReactor(
                                mode = jarvisMode,
                                volumeIntensity = volumeIntensity,
                                modifier = Modifier.testTag("arc_reactor_visualizer")
                            )

                            // Running state HUD badge on side
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "POWER CORE",
                                        color = Color(0xFF22D3EE),
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${telemetry.batteryPct}%",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Status banner message
                        Text(
                            text = when (jarvisMode) {
                                JarvisMode.LISTENING -> "listening... rms amplitude: ${(volumeIntensity * 100).toInt()}"
                                JarvisMode.THINKING -> "analyzing vocal arrays..."
                                JarvisMode.SPEAKING -> "vocal speech synthesis active..."
                                else -> "mainframe links nominal"
                            }.uppercase(Locale.getDefault()),
                            color = when (jarvisMode) {
                                JarvisMode.LISTENING -> Color(0xFFFF5500)
                                JarvisMode.THINKING -> Color(0xFF22D3EE)
                                JarvisMode.SPEAKING -> Color(0xFF06B6D4)
                                else -> Color(0xFF64748B)
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // 4. Weather & Location Rounded Cards Row Grid (Extreme roundness 32.dp)
                        WeatherLocationGrid(telemetry = telemetry)
                    }
                } else {
                    // Logs Terminal screen
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (messages.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "system logs purged. status ready.\ntype instructions or speak to begin.",
                                    color = Color(0xFF22D3EE).copy(alpha = 0.4f),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(messages) { msg ->
                                    TerminalMessageBubble(msg = msg)
                                }
                            }
                        }

                        // Purge clear history control action button
                        IconButton(
                            onClick = { viewModel.clearLogs() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(36.dp)
                                .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                                .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                                .testTag("clear_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset local terminal memory",
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Divider system trace line above bottom toolbar
            HorizontalTraceLine()

            // 5. Footer Command input toolbar (Glow effects, Map Navigation action, and dynamic Mic button)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0F172A).copy(alpha = 0.95f),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Quick Radar Map Navigation HUD Launcher
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF020617))
                                .border(1.dp, Color(0xFF22D3EE).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                                .clickable {
                                    scope.launch {
                                        viewModel.processQuery("Launch topographic navigation map.", isVoice = false)
                                    }
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Active GPS Mapping Grid",
                                tint = Color(0xFF22D3EE),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // SciFI customized input command entry field
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { viewModel.updateTextInput(it) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            ),
                            placeholder = {
                                Text(
                                    "Transmit instruction code...",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF22D3EE),
                                unfocusedBorderColor = Color(0xFF1E293B),
                                focusedContainerColor = Color(0xFF020617),
                                unfocusedContainerColor = Color(0xFF020617)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    viewModel.submitTextQuery()
                                    keyboardController?.hide()
                                }
                            ),
                            trailingIcon = {
                                if (textInput.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            viewModel.submitTextQuery()
                                            keyboardController?.hide()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Send text instruction",
                                            tint = Color(0xFF22D3EE)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("terminal_text_field")
                        )

                        // Elegant feedback-responsive Glowing vocal mic button with circular shadow background
                        val scaleFactor by animateFloatAsState(
                            targetValue = if (jarvisMode == JarvisMode.LISTENING) 1.12f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
                            label = "vocal_mic_breath"
                        )

                        val micContainerColor = if (jarvisMode == JarvisMode.LISTENING) {
                            Color(0xFFFF5500)
                        } else {
                            Color(0xFF22D3EE)
                        }

                        val micStrokeColor = if (jarvisMode == JarvisMode.LISTENING) {
                            Color(0xFFFFCC00)
                        } else {
                            Color.White.copy(alpha = 0.5f)
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer {
                                    scaleX = scaleFactor
                                    scaleY = scaleFactor
                                }
                                .clip(RoundedCornerShape(24.dp))
                                .background(micContainerColor)
                                .border(1.dp, micStrokeColor, RoundedCornerShape(24.dp))
                                .clickable {
                                    if (jarvisMode == JarvisMode.LISTENING) {
                                        viewModel.stopListening()
                                    } else {
                                        viewModel.startListening()
                                    }
                                }
                                .testTag("vocal_receiver_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            // Render active pulsing indicator or mic icon
                            if (jarvisMode == JarvisMode.LISTENING) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(Color.White, RoundedCornerShape(2.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Trigger vocal input detector",
                                    tint = Color(0xFF020617),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isOnline) "Encrypted Online Grid Active" else "Encrypted Offline Mode Active",
                        color = Color(0xFF475569),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun SystemHeader(isOnline: Boolean) {
    var currentTime by remember { mutableStateOf("09:41") }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            kotlinx.coroutines.delay(1000)
        }
    }

    val pulseAnim = rememberInfiniteTransition(label = "header_breath")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_light"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "SYSTEM STATUS",
                color = Color(0xFF22D3EE),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .graphicsLayer { alpha = pulseAlpha }
                        .background(Color(0xFF22D3EE), RoundedCornerShape(3.dp))
                )
                Text(
                    text = if (isOnline) "J.A.R.V.I.S. ONLINE" else "J.A.R.V.I.S. OFFLINE_REPLICA",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Text(
            text = currentTime,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-0.5).sp,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
fun GreetingStatsHeader(userName: String, userTitle: String, isOnline: Boolean) {
    val timeOfDay = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = "$timeOfDay,",
            color = Color(0xFFECFDF5).copy(alpha = 0.95f),
            fontSize = 30.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "$userTitle $userName",
            color = Color(0xFF22D3EE),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp
        )
        Text(
            text = if (isOnline) "All systems are currently nominal." else "Cloud decoupled. Local telemetry matrix is active.",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun TabSelectorButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) Color(0xFF22D3EE).copy(alpha = 0.15f) else Color.Transparent
    val borderCol = if (isSelected) Color(0xFF22D3EE) else Color(0xFF1E293B)
    val textCol = if (isSelected) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, borderCol, RoundedCornerShape(4.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textCol,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun WeatherLocationGrid(
    telemetry: com.example.viewmodel.TelemetryState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Weather Card (rounded-[2rem])
        Column(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFF0F172A).copy(alpha = 0.6f), RoundedCornerShape(32.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(32.dp))
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF22D3EE).copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Weather sensor node",
                    tint = Color(0xFF22D3EE),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "WEATHER",
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = telemetry.weatherTemp,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = telemetry.weatherDesc,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }

        // Location Card (rounded-[2rem])
        Column(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFF0F172A).copy(alpha = 0.6f), RoundedCornerShape(32.dp))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(32.dp))
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF22D3EE).copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location navigation node",
                    tint = Color(0xFF22D3EE),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "LOCATION",
                color = Color(0xFF64748B),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = telemetry.locationName,
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun HorizontalTraceLine() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = Color(0xFF22D3EE).copy(alpha = 0.2f),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@Composable
fun TopSensorStrip(
    telemetry: com.example.viewmodel.TelemetryState,
    isOnline: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF020617))
            .padding(vertical = 10.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TelemetryBox(
            title = "REACTOR TEMP",
            value = String.format("%.1f°C", telemetry.coreTemp),
            color = if (telemetry.coreTemp > 35) Color(0xFFFF5500) else Color(0xFF22D3EE)
        )
        TelemetryBox(
            title = "MAINFRAME CPU",
            value = String.format("%.1f%%", telemetry.cpuUsage),
            color = Color(0xFF06B6D4)
        )
        val battColor = when {
            telemetry.batteryPct < 25 -> Color(0xFFFF3333)
            telemetry.isCharging -> Color(0xFF33FF33)
            else -> Color(0xFF22D3EE)
        }
        TelemetryBox(
            title = "POWER RESERVE",
            value = "${telemetry.batteryPct}%" + (if (telemetry.isCharging) " (⚡)" else ""),
            color = battColor
        )
        TelemetryBox(
            title = "SYNAPTIC LINK",
            value = if (isOnline) "ONLINE" else "LOCAL ONLY",
            color = if (isOnline) Color(0xFF33FF33) else Color(0xFFFF9900)
        )
    }
}

@Composable
fun TelemetryBox(
    title: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(82.dp)
    ) {
        Text(
            text = title,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.White.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun TerminalMessageBubble(msg: MessageEntity) {
    val isJarvis = msg.role == "jarvis"

    val alignment = if (isJarvis) Alignment.Start else Alignment.End
    val borderStrokeCol = if (isJarvis) {
        if (msg.isOffline) Color(0xFFFF9900).copy(alpha = 0.5f) else Color(0xFF06B6D4).copy(alpha = 0.5f)
    } else {
        Color(0xFF22D3EE).copy(alpha = 0.5f)
    }

    val bubbleBg = if (isJarvis) {
        Color(0xFF0F172A).copy(alpha = 0.7f)
    } else {
        Color(0xFF020617).copy(alpha = 0.5f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = (if (isJarvis) "JARVIS MAINFRAME" else "USER INDEX").uppercase(Locale.getDefault()),
                color = if (isJarvis) Color(0xFF06B6D4) else Color(0xFF22D3EE),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            if (msg.isOffline) {
                Text(
                    text = "[OFFLINE ADAPTER]",
                    color = Color(0xFFFF9900),
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Light
                )
            }
            if (msg.isVoice) {
                Text(
                    text = "🎤",
                    fontSize = 8.sp
                )
            }
        }

        val msgShape = if (isJarvis) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp)
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .widthIn(max = 290.dp)
                .clip(msgShape)
                .border(1.dp, borderStrokeCol, msgShape)
                .background(bubbleBg)
                .padding(12.dp)
        ) {
            Text(
                text = msg.text,
                color = if (isJarvis) Color.White else Color(0xFFE2F0FE),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp
            )
        }
    }
}

// Helper wrapper functions
@Composable
fun <T> rememberStateFlowOf(initial: T): MutableState<T> {
    return remember { mutableStateOf(initial) }
}
fun <T> mutableStateFlowOf(value: T): T {
    var state by mutableStateOf(value)
    return state
}
