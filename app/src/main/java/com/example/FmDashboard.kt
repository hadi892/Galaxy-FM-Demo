package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.launch

// High-fidelity Tech Theme Color Constants
val TechDarkBackground = Color(0xFF0C0F16)
val TechCardBackground = Color(0xFF161B26)
val TechBlue = Color(0xFF00D2FF)
val TechGreen = Color(0xFF00E676)
val TechRed = Color(0xFFE63946)
val TechYellow = Color(0xFFFFD600)
val TechWhite = Color(0xFFF1F5F9)
val TechGray = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FmDashboard(viewModel: FmViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val logs by viewModel.logs.collectAsState()
    val permissions by viewModel.permissionsStatus.collectAsState()
    val deviceNodes by viewModel.deviceNodesStatus.collectAsState()
    val libraries by viewModel.librariesStatus.collectAsState()
    val halStatus by viewModel.halStatus.collectAsState()
    val classes by viewModel.classesStatus.collectAsState()
    val report by viewModel.diagnosticReport.collectAsState()

    val isInitialized by viewModel.isInitialized.collectAsState()
    val isPoweredOn by viewModel.isPoweredOn.collectAsState()
    val currentFrequency by viewModel.currentFrequency.collectAsState()
    val rssi by viewModel.rssi.collectAsState()
    val rdsText by viewModel.rdsText.collectAsState()

    // Run dynamic diagnostics instantly when app starts
    LaunchedEffect(Unit) {
        viewModel.runFullDiagnostic(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "GALAXY FM DEMO",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TechBlue,
                                letterSpacing = 1.5.sp
                            )
                        )
                        Text(
                            "Snapdragon 695 Diagnostic Engine",
                            style = MaterialTheme.typography.bodySmall.copy(color = TechGray)
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TechCardBackground)
                            .border(1.dp, TechBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "API 36",
                            style = TextStyle(
                                color = TechBlue,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TechDarkBackground,
                    titleContentColor = TechWhite
                )
            )
        },
        containerColor = TechDarkBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Tab Row Configuration
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = TechDarkBackground,
                contentColor = TechBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TechBlue
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("HARDWARE AUDIT", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Dns, contentDescription = "Hardware Tab") },
                    selectedContentColor = TechBlue,
                    unselectedContentColor = TechGray
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("CONTROL CENTER", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Controls Tab") },
                    selectedContentColor = TechBlue,
                    unselectedContentColor = TechGray
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("SYSTEM REPORT", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Terminal, contentDescription = "Report Tab") },
                    selectedContentColor = TechBlue,
                    unselectedContentColor = TechGray
                )
            }

            // Main Tab Content Router
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> HardwareAuditTab(
                        permissions = permissions,
                        deviceNodes = deviceNodes,
                        libraries = libraries,
                        halStatus = halStatus,
                        classes = classes,
                        onScanClick = { viewModel.runFullDiagnostic(context) }
                    )
                    1 -> ControlCenterTab(
                        isInitialized = isInitialized,
                        isPoweredOn = isPoweredOn,
                        currentFrequency = currentFrequency,
                        rssi = rssi,
                        rdsText = rdsText,
                        onInitialize = { viewModel.initializeFmReceiver() },
                        onPowerOn = { viewModel.powerOnFm() },
                        onPowerOff = { viewModel.powerOffFm() },
                        onTune = { freq -> viewModel.tuneFrequency(freq) },
                        onSeekUp = { viewModel.seek(true) },
                        onSeekDown = { viewModel.seek(false) },
                        onScan = { viewModel.scanStations() },
                        onReadRssi = { viewModel.readRssi() },
                        onReadRds = { viewModel.readRds() }
                    )
                    2 -> SystemReportTab(
                        report = report,
                        logs = logs,
                        onExportClick = { viewModel.exportDiagnosticReport(context) }
                    )
                }
            }
        }
    }
}

@Composable
fun HardwareAuditTab(
    permissions: List<ComponentStatus>,
    deviceNodes: List<ComponentStatus>,
    libraries: List<ComponentStatus>,
    halStatus: ComponentStatus,
    classes: List<ComponentStatus>,
    onScanClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Qualcomm hardware and subsystem audit status. Checks represent direct file system, dynamic linker, and framework reflection values.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TechWhite)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onScanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TechBlue, contentColor = TechDarkBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("run_diagnostic_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TRIGGER DYNAMIC SYSTEM AUDIT", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }

        // 1. Android Permission Status Group
        StatusSection(title = "App Permissions", items = permissions)

        // 2. Kernel Device Node Group
        StatusSection(title = "Kernel Driver Nodes (/dev)", items = deviceNodes)

        // 3. Proprietary Dynamic Libraries Group
        StatusSection(title = "Proprietary Shared Libraries (.so)", items = libraries)

        // 4. AOSP HIDL HAL Service Group
        StatusSection(title = "HIDL Binder Services", items = listOf(halStatus))

        // 5. Framework Classes Group
        StatusSection(title = "OEM Framework Classpath", items = classes)
    }
}

@Composable
fun StatusSection(title: String, items: List<ComponentStatus>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TechCardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TechBlue,
                    letterSpacing = 1.sp
                )
            )
            HorizontalDivider(color = TechGray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { status ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                status.name,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TechWhite
                                )
                            )
                            if (status.path != "N/A" && status.path.isNotBlank()) {
                                Text(
                                    "Path: ${status.path}",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = TechGray
                                    )
                                )
                            }
                            if (status.error != null) {
                                Text(
                                    status.error,
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = TechRed,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Colored status badge
                        val badgeColor = when {
                            status.isLoaded -> TechGreen
                            status.isPresent -> TechYellow
                            else -> TechRed
                        }

                        val badgeText = when {
                            status.isLoaded -> "ACTIVE / OK"
                            status.isPresent -> "PRESENT"
                            else -> "MISSING / FAILED"
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                badgeText,
                                style = TextStyle(
                                    color = badgeColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ControlCenterTab(
    isInitialized: Boolean,
    isPoweredOn: Boolean,
    currentFrequency: Double,
    rssi: Int?,
    rdsText: String?,
    onInitialize: () -> Unit,
    onPowerOn: () -> Unit,
    onPowerOff: () -> Unit,
    onTune: (Double) -> Unit,
    onSeekUp: () -> Unit,
    onSeekDown: () -> Unit,
    onScan: () -> Unit,
    onReadRssi: () -> Unit,
    onReadRds: () -> Unit
) {
    var frequencyInputText by remember { mutableStateOf("98.1") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hardware Monitor HUD
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "FM RECEIVER LIVE STATUS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TechBlue,
                        letterSpacing = 1.sp
                    )
                )
                HorizontalDivider(color = TechGray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left HUD block (State check)
                    Column(modifier = Modifier.weight(1f)) {
                        HUDItem(label = "Receiver Class", state = if (isInitialized) "INITIALIZED" else "NOT INITIALIZED", color = if (isInitialized) TechGreen else TechRed)
                        Spacer(modifier = Modifier.height(8.dp))
                        HUDItem(label = "Power Stage", state = if (isPoweredOn) "POWER ON" else "POWER OFF", color = if (isPoweredOn) TechGreen else TechRed)
                    }

                    // Right HUD block (Frequency readout)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TechDarkBackground)
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("FREQUENCY READOUT", fontSize = 10.sp, color = TechGray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${String.format("%.1f", currentFrequency)} MHz",
                            style = TextStyle(
                                color = if (isPoweredOn) TechGreen else TechGray,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }

                if (isPoweredOn) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(TechDarkBackground)
                                .padding(8.dp)
                        ) {
                            Text(
                                "RSSI: ${rssi?.toString() ?: "N/A"} dBm",
                                style = TextStyle(
                                    color = TechBlue,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(TechDarkBackground)
                                .padding(8.dp)
                        ) {
                            Text(
                                "RDS: ${rdsText ?: "N/A"}",
                                style = TextStyle(
                                    color = TechYellow,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Initialize & Power controls
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "PRIMARY HAL CONTROLS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TechBlue,
                        letterSpacing = 1.sp
                    )
                )
                HorizontalDivider(color = TechGray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onInitialize,
                        colors = ButtonDefaults.buttonColors(containerColor = TechBlue, contentColor = TechDarkBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("initialize_fm_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("INITIALIZE FM RECEIVER CLASS", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onPowerOn,
                            colors = ButtonDefaults.buttonColors(containerColor = TechGreen, contentColor = TechDarkBackground),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("power_on_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("POWER ON", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onPowerOff,
                            colors = ButtonDefaults.buttonColors(containerColor = TechRed, contentColor = TechWhite),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("power_off_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("POWER OFF", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Tuning & Scanning controls
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "FM BAND OPERATIONS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TechBlue,
                        letterSpacing = 1.sp
                    )
                )
                HorizontalDivider(color = TechGray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextField(
                        value = frequencyInputText,
                        onValueChange = { frequencyInputText = it },
                        label = { Text("Frequency (87.5 - 108.0)") },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("frequency_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = TechDarkBackground,
                            unfocusedContainerColor = TechDarkBackground,
                            focusedTextColor = TechWhite,
                            unfocusedTextColor = TechWhite,
                            focusedLabelColor = TechBlue,
                            unfocusedLabelColor = TechGray,
                            focusedIndicatorColor = TechBlue
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val freq = frequencyInputText.toDoubleOrNull()
                            if (freq != null && freq in 87.5..108.0) {
                                onTune(freq)
                            } else {
                                onTune(98.1)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TechBlue, contentColor = TechDarkBackground),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("tune_button")
                    ) {
                        Text("TUNE", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onSeekDown,
                        colors = ButtonDefaults.buttonColors(containerColor = TechCardBackground, contentColor = TechBlue),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(1.dp, TechBlue.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .testTag("seek_down_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Seek Down")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SEEK DN")
                    }

                    Button(
                        onClick = onSeekUp,
                        colors = ButtonDefaults.buttonColors(containerColor = TechCardBackground, contentColor = TechBlue),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(1.dp, TechBlue.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .testTag("seek_up_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Seek Up")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SEEK UP")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onScan,
                    colors = ButtonDefaults.buttonColors(containerColor = TechCardBackground, contentColor = TechWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, TechGray.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .testTag("scan_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SCAN ENTIRE FM BAND")
                }
            }
        }

        // Telemetry readers
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardBackground),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "HARDWARE TELEMETRY & DATA",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TechBlue,
                        letterSpacing = 1.sp
                    )
                )
                HorizontalDivider(color = TechGray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onReadRssi,
                        colors = ButtonDefaults.buttonColors(containerColor = TechCardBackground, contentColor = TechBlue),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(1.dp, TechBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .testTag("read_rssi_button")
                    ) {
                        Text("READ RSSI", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onReadRds,
                        colors = ButtonDefaults.buttonColors(containerColor = TechCardBackground, contentColor = TechYellow),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .border(1.dp, TechYellow.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .testTag("read_rds_button")
                    ) {
                        Text("READ RDS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun HUDItem(label: String, state: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = TechGray, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.15f))
                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(state, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun SystemReportTab(
    report: String,
    logs: List<String>,
    onExportClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Log console view
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "LOG CONSOLE STACK",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TechBlue,
                        letterSpacing = 1.sp
                    )
                )
                HorizontalDivider(color = TechGray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TechDarkBackground)
                        .padding(12.dp)
                ) {
                    val logScrollState = rememberScrollState()
                    // Auto-scroll to end when logs size changes
                    LaunchedEffect(logs.size) {
                        logScrollState.animateScrollTo(logScrollState.maxValue)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(logScrollState),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (logs.isEmpty()) {
                            Text("Console is idle.", color = TechGray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        } else {
                            logs.forEach { logItem ->
                                Text(
                                    logItem,
                                    color = if (logItem.contains("Failed") || logItem.contains("Error") || logItem.contains("Exception")) TechRed else if (logItem.contains("Success")) TechGreen else TechWhite,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Formatted raw diagnostics display
        Card(
            colors = CardDefaults.cardColors(containerColor = TechCardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.5f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "TECHNICAL REPORT PREVIEW",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TechBlue,
                            letterSpacing = 1.sp
                        )
                    )

                    Button(
                        onClick = onExportClick,
                        colors = ButtonDefaults.buttonColors(containerColor = TechBlue, contentColor = TechDarkBackground),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("export_report_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SHARE REPORT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = TechGray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TechDarkBackground)
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = report.ifBlank { "Audit report has not been compiled yet. Click 'Trigger Dynamic System Audit' in the Hardware tab." },
                            color = TechWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
