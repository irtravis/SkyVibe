package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.MapLayerType
import com.example.ui.theme.AvionicsTextDim
import com.example.ui.theme.AvionicsTextPrimary
import com.example.ui.theme.AvionicsTextSecondary
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OledCardBorder
import com.example.ui.theme.OledSurface
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarGreen
import com.example.ui.theme.RadarRedAlert

@Composable
fun SkyWatchScreen(
    viewModel: SkyWatchViewModel,
    targetTailFromIntent: String? = null
) {
    val context = LocalContext.current
    val aircraftList by viewModel.aircraftList.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val feedStatus by viewModel.feedStatus.collectAsState()
    val radarCenter by viewModel.radarCenter.collectAsState()
    val scanRadiusNm by viewModel.scanRadiusNm.collectAsState()
    val telemetryFeed by viewModel.telemetryFeed.collectAsState()
    val activeAlertsCount by viewModel.activeAlertsCount.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val alertLogs by viewModel.alertLogs.collectAsState()
    val currentViewMode by viewModel.currentViewMode.collectAsState()
    val mapLayerType by viewModel.mapLayerType.collectAsState()
    val isCasting by viewModel.isCasting.collectAsState()
    val connectedCastDevice by viewModel.connectedCastDevice.collectAsState()
    val availableCastDevices by viewModel.availableCastDevices.collectAsState()
    val isScanningCast by viewModel.isScanningCast.collectAsState()
    val selectedTypeForInfo by viewModel.selectedTypeForInfo.collectAsState()
    val selectedAircraftForInfo by viewModel.selectedAircraftForInfo.collectAsState()
    val selectedAircraft by viewModel.selectedAircraft.collectAsState()
    val isLocatingGps by viewModel.isLocatingGps.collectAsState()
    val locationMessage by viewModel.locationMessage.collectAsState()
    val flightTracks by viewModel.flightTracks.collectAsState()
    val trackedTails by viewModel.trackedTails.collectAsState()
    val selectedTailForHistory by viewModel.selectedTailForHistory.collectAsState()
    val stormCells by viewModel.stormCells.collectAsState()
    val isStormRadarEnabled by viewModel.isStormRadarEnabled.collectAsState()

    var showLocationRadiusDialog by remember { mutableStateOf(false) }
    var showCastDialog by remember { mutableStateOf(false) }
    var tabletRightTab by remember { mutableStateOf("LIST") } // "LIST" or "WATCHLIST" for dual-pane
    var isRadarMaximized by remember { mutableStateOf(false) }

    // Intercept back button when radar is maximized to restore normal view
    BackHandler(enabled = isRadarMaximized) {
        isRadarMaximized = false
    }

    // Request Notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(targetTailFromIntent) {
        if (!targetTailFromIntent.isNullOrBlank()) {
            val target = aircraftList.find { it.tailNumber.equals(targetTailFromIntent, ignoreCase = true) }
            if (target != null) {
                viewModel.selectAircraft(target)
                viewModel.setViewMode("MAP")
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = OledBlack
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(OledBlack)
        ) {
            // Adaptive screen size detection: Tablets and Foldables (Fold8 Ultra unfolded)
            val isExpandedScreen = maxWidth >= 700.dp

            Column(modifier = Modifier.fillMaxSize()) {
                if (!isRadarMaximized || currentViewMode != "MAP") {
                    // 1. OLED Top Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OledBlack)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Logo, Status & Layer
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF002914))
                                .border(1.dp, RadarGreen, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = "Radar Scope",
                                tint = RadarGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SKYWATCH",
                                    color = AvionicsTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isPaused) Color(0xFF261A00) else Color(0xFF00381B))
                                        .border(1.dp, if (isPaused) RadarAmber else RadarGreen, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (isPaused) "PAUSED" else "1090MHz",
                                        color = if (isPaused) RadarAmber else RadarGreen,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                if (isExpandedScreen) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF002233))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "DUAL-PANE TABLET/FOLD",
                                            color = RadarCyan,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "$feedStatus · ${scanRadiusNm.toInt()} NM · ${mapLayerType.displayName}",
                                color = if (feedStatus.startsWith("SIGNAL LOST")) RadarAmber else AvionicsTextSecondary,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }

                    // Controls: Cast Screen & Pause/Resume Toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Cast Button
                        Button(
                            onClick = { showCastDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCasting) Color(0xFF00381B) else OledSurface,
                                contentColor = if (isCasting) RadarGreen else RadarCyan
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCasting) RadarGreen else OledCardBorder
                            ),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("btn_cast_screen")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = "Cast Screen",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCasting) "CASTING" else "CAST",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Pause / Resume Toggle
                        Button(
                            onClick = { viewModel.togglePaused() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPaused) Color(0xFF261A00) else Color(0xFF00381B),
                                contentColor = if (isPaused) RadarAmber else RadarGreen
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isPaused) RadarAmber else RadarGreen
                            ),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("btn_pause_toggle")
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume Radar" else "Pause Radar",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isPaused) "RESUME" else "PAUSE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // 1b. Prominent Airspace & Weather Command Bar (Airport Code, GPS Snap, Radius, Storm Radar)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    color = OledSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OledCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Airport Code Selector Button (Prominent display of current code like TRI)
                        Box(
                            modifier = Modifier
                                .weight(1.15f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!radarCenter.isGps) Color(0xFF002B36) else Color(0xFF141E1C))
                                .border(1.dp, if (!radarCenter.isGps) RadarCyan else OledCardBorder, RoundedCornerShape(8.dp))
                                .clickable { showLocationRadiusDialog = true }
                                .padding(horizontal = 6.dp)
                                .testTag("btn_quick_airport_select"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AirplanemodeActive,
                                    contentDescription = "Airport Code",
                                    tint = if (!radarCenter.isGps) RadarCyan else AvionicsTextDim,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (radarCenter.isGps) "AIRPORT ▾" else "${radarCenter.id.uppercase()} ▾",
                                    color = if (!radarCenter.isGps) RadarCyan else AvionicsTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }

                        // 2. GPS Location Snap Button
                        Box(
                            modifier = Modifier
                                .weight(1.15f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (radarCenter.isGps) Color(0xFF00381B) else Color(0xFF141E1C))
                                .border(1.dp, if (radarCenter.isGps) RadarGreen else OledCardBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.acquireGpsLocation() }
                                .padding(horizontal = 6.dp)
                                .testTag("btn_quick_gps_snap"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isLocatingGps) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = RadarGreen,
                                        strokeWidth = 1.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "FIXING...",
                                        color = RadarGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "GPS Location",
                                        tint = if (radarCenter.isGps) RadarGreen else AvionicsTextDim,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (radarCenter.isGps) "GPS LOCKED" else "GPS SNAP",
                                        color = if (radarCenter.isGps) RadarGreen else AvionicsTextSecondary,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // 3. Scan Radius Selector
                        Box(
                            modifier = Modifier
                                .weight(0.85f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF211700))
                                .border(1.dp, RadarAmber, RoundedCornerShape(8.dp))
                                .clickable { showLocationRadiusDialog = true }
                                .padding(horizontal = 4.dp)
                                .testTag("btn_quick_radius_select"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${scanRadiusNm.toInt()} NM ▾",
                                color = RadarAmber,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }

                        // 4. Storm Radar / Weather Overlay Toggle
                        Box(
                            modifier = Modifier
                                .weight(1.05f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isStormRadarEnabled) Color(0xFF330030) else Color(0xFF141E1C))
                                .border(1.dp, if (isStormRadarEnabled) Color(0xFFD500F9) else OledCardBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.toggleStormRadar() }
                                .padding(horizontal = 4.dp)
                                .testTag("btn_quick_weather_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isStormRadarEnabled) "⛈ WX:ON" else "⛈ WX:OFF",
                                color = if (isStormRadarEnabled) Color(0xFFEA80FC) else AvionicsTextDim,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Location feedback notification banner
                locationMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF00291B))
                            .border(1.dp, RadarGreen)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "📡 $msg",
                            color = RadarGreen,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 2. Active Watchlist Alarm Intrusion Banner
                AnimatedVisibility(visible = activeAlertsCount > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF3B1500))
                            .border(1.dp, RadarAmber)
                            .clickable {
                                if (isExpandedScreen) tabletRightTab = "WATCHLIST"
                                else viewModel.setViewMode("WATCHLIST")
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alarm Alert",
                                    tint = RadarAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🚨 PERIMETER BREACH: $activeAlertsCount watched plane(s) in radius!",
                                    color = RadarAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Text(
                                text = "VIEW ALARMS >",
                                color = AvionicsTextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                }

                // 3. Main Content: Adaptive Dual-Pane (Tablets/Foldables) vs Single-Pane (Phones)
                if (isExpandedScreen) {
                    if (isRadarMaximized) {
                        RadarMapView(
                            aircraftList = aircraftList,
                            radarCenter = radarCenter,
                            scanRadiusNm = scanRadiusNm,
                            isPaused = isPaused,
                            feedStatus = feedStatus,
                            mapLayerType = mapLayerType,
                            stormCells = stormCells,
                            isStormRadarEnabled = isStormRadarEnabled,
                            selectedAircraft = selectedAircraft,
                            isMaximized = isRadarMaximized,
                            onToggleMaximize = { isRadarMaximized = !isRadarMaximized },
                            onMapLayerChanged = { viewModel.setMapLayerType(it) },
                            onToggleStormRadar = { viewModel.toggleStormRadar() },
                            onOpenLocationPicker = { showLocationRadiusDialog = true },
                            onSnapGps = { viewModel.acquireGpsLocation() },
                            onAircraftSelected = { viewModel.selectAircraft(it) },
                            onDeselectAircraft = { viewModel.clearSelectedAircraft() },
                            onViewTypeDetails = { typeInfo, ac -> viewModel.showTypeDetails(typeInfo, ac) },
                            onToggleWatchlist = { viewModel.toggleWatchlist(it) }
                        )
                    } else {
                        // WIDE SCREEN / TABLET / FOLD8 ULTRA CANONICAL LAYOUT
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left Pane (58% width): Tactical Map (Radar / Satellite / Terrain)
                            Box(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .fillMaxHeight()
                                    .border(1.dp, OledCardBorder)
                            ) {
                                RadarMapView(
                                    aircraftList = aircraftList,
                                    radarCenter = radarCenter,
                                    scanRadiusNm = scanRadiusNm,
                                    isPaused = isPaused,
                                    feedStatus = feedStatus,
                                    mapLayerType = mapLayerType,
                                stormCells = stormCells,
                                isStormRadarEnabled = isStormRadarEnabled,
                                selectedAircraft = selectedAircraft,
                                isMaximized = isRadarMaximized,
                                onToggleMaximize = { isRadarMaximized = !isRadarMaximized },
                                onMapLayerChanged = { viewModel.setMapLayerType(it) },
                                onToggleStormRadar = { viewModel.toggleStormRadar() },
                                onOpenLocationPicker = { showLocationRadiusDialog = true },
                                onSnapGps = { viewModel.acquireGpsLocation() },
                                onAircraftSelected = { viewModel.selectAircraft(it) },
                                onDeselectAircraft = { viewModel.clearSelectedAircraft() },
                                onViewTypeDetails = { typeInfo, ac -> viewModel.showTypeDetails(typeInfo, ac) },
                                onToggleWatchlist = { viewModel.toggleWatchlist(it) }
                            )
                        }

                        // Right Pane (42% width): Live Aircraft List / Watchlist
                        Column(
                            modifier = Modifier
                                .weight(1.0f)
                                .fillMaxHeight()
                                .background(OledBlack)
                        ) {
                            // Sub-tabs for tablet right pane
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ModeTabButton(
                                    label = "AIRCRAFT DATA",
                                    icon = Icons.Default.FormatListBulleted,
                                    isSelected = tabletRightTab == "LIST",
                                    onClick = { tabletRightTab = "LIST" },
                                    modifier = Modifier.weight(1f)
                                )
                                ModeTabButton(
                                    label = "WATCHLIST",
                                    icon = Icons.Default.NotificationsActive,
                                    badgeCount = watchlist.size,
                                    isSelected = tabletRightTab == "WATCHLIST",
                                    onClick = { tabletRightTab = "WATCHLIST" },
                                    modifier = Modifier.weight(1f)
                                )
                                ModeTabButton(
                                    label = "HISTORY",
                                    icon = Icons.Default.History,
                                    badgeCount = flightTracks.size,
                                    isSelected = tabletRightTab == "HISTORY",
                                    onClick = { tabletRightTab = "HISTORY" },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                when (tabletRightTab) {
                                    "LIST" -> AircraftListView(
                                        aircraftList = aircraftList,
                                        telemetryFeed = telemetryFeed,
                                        isPaused = isPaused,
                                        feedStatus = feedStatus,
                                        onViewTypeDetails = { typeInfo, ac -> viewModel.showTypeDetails(typeInfo, ac) },
                                        onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                                        onAircraftSelected = { viewModel.selectAircraft(it) }
                                    )
                                    "WATCHLIST" -> WatchlistAlarmView(
                                        watchlist = watchlist,
                                        alertLogs = alertLogs,
                                        currentAircraft = aircraftList,
                                        radarCenter = radarCenter,
                                        onAddWatchlist = { tail, label, radius ->
                                            viewModel.addWatchlistEntry(tail, label, radius)
                                        },
                                        onToggleActive = { viewModel.toggleWatchlistActive(it) },
                                        onDeleteWatchlist = { viewModel.deleteWatchlistEntry(it) },
                                        onClearLogs = { viewModel.clearAlertLogs() },
                                        onTriggerTestAlarm = { viewModel.triggerTestAlarm() }
                                    )
                                    "HISTORY" -> FlightHistoryView(
                                        tracks = flightTracks,
                                        trackedTails = trackedTails,
                                        selectedTail = selectedTailForHistory,
                                        onSelectTail = { viewModel.selectTailForHistory(it) },
                                        onClearTracks = { viewModel.clearFlightTracks() }
                                    )
                                }
                            }
                        }
                    }
                }
                } else {
                    // COMPACT SCREEN (Phones & Folded Cover)
                    // View Mode Switcher with Map Layer Toggling (hidden when radar is maximized to fill entire screen)
                    if (!isRadarMaximized || currentViewMode != "MAP") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // MAP VIEW: Clicking when already selected cycles [RADAR -> SATELLITE -> TERRAIN]
                            ModeTabButton(
                                label = "${mapLayerType.displayName} ⟳",
                                icon = Icons.Default.Radar,
                                isSelected = currentViewMode == "MAP",
                                onClick = {
                                    if (currentViewMode == "MAP") {
                                        viewModel.cycleMapLayerType()
                                    } else {
                                        viewModel.setViewMode("MAP")
                                    }
                                },
                                modifier = Modifier.weight(1.05f)
                            )
                            ModeTabButton(
                                label = "LIST",
                                icon = Icons.Default.FormatListBulleted,
                                isSelected = currentViewMode == "LIST",
                                onClick = { viewModel.setViewMode("LIST") },
                                modifier = Modifier.weight(0.85f)
                            )
                            ModeTabButton(
                                label = "WATCH",
                                icon = Icons.Default.NotificationsActive,
                                badgeCount = watchlist.size,
                                isSelected = currentViewMode == "WATCHLIST",
                                onClick = { viewModel.setViewMode("WATCHLIST") },
                                modifier = Modifier.weight(0.95f)
                            )
                            ModeTabButton(
                                label = "HISTORY",
                                icon = Icons.Default.History,
                                badgeCount = flightTracks.size,
                                isSelected = currentViewMode == "HISTORY",
                                onClick = { viewModel.setViewMode("HISTORY") },
                                modifier = Modifier.weight(1.05f)
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Crossfade(targetState = currentViewMode, label = "ViewModeCrossfade") { mode ->
                            when (mode) {
                                "MAP" -> RadarMapView(
                                    aircraftList = aircraftList,
                                    radarCenter = radarCenter,
                                    scanRadiusNm = scanRadiusNm,
                                    isPaused = isPaused,
                                    feedStatus = feedStatus,
                                    mapLayerType = mapLayerType,
                                    stormCells = stormCells,
                                    isStormRadarEnabled = isStormRadarEnabled,
                                    selectedAircraft = selectedAircraft,
                                    isMaximized = isRadarMaximized,
                                    onToggleMaximize = { isRadarMaximized = !isRadarMaximized },
                                    onMapLayerChanged = { viewModel.setMapLayerType(it) },
                                    onToggleStormRadar = { viewModel.toggleStormRadar() },
                                    onOpenLocationPicker = { showLocationRadiusDialog = true },
                                    onSnapGps = { viewModel.acquireGpsLocation() },
                                    onAircraftSelected = { viewModel.selectAircraft(it) },
                                    onDeselectAircraft = { viewModel.clearSelectedAircraft() },
                                    onViewTypeDetails = { typeInfo, ac -> viewModel.showTypeDetails(typeInfo, ac) },
                                    onToggleWatchlist = { viewModel.toggleWatchlist(it) }
                                )
                                "LIST" -> AircraftListView(
                                    aircraftList = aircraftList,
                                    telemetryFeed = telemetryFeed,
                                    isPaused = isPaused,
                                    feedStatus = feedStatus,
                                    onViewTypeDetails = { typeInfo, ac -> viewModel.showTypeDetails(typeInfo, ac) },
                                    onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                                    onAircraftSelected = {
                                        viewModel.selectAircraft(it)
                                        viewModel.setViewMode("MAP")
                                    }
                                )
                                "WATCHLIST" -> WatchlistAlarmView(
                                    watchlist = watchlist,
                                    alertLogs = alertLogs,
                                    currentAircraft = aircraftList,
                                    radarCenter = radarCenter,
                                    onAddWatchlist = { tail, label, radius ->
                                        viewModel.addWatchlistEntry(tail, label, radius)
                                    },
                                    onToggleActive = { viewModel.toggleWatchlistActive(it) },
                                    onDeleteWatchlist = { viewModel.deleteWatchlistEntry(it) },
                                    onClearLogs = { viewModel.clearAlertLogs() },
                                    onTriggerTestAlarm = { viewModel.triggerTestAlarm() }
                                )
                                "HISTORY" -> FlightHistoryView(
                                    tracks = flightTracks,
                                    trackedTails = trackedTails,
                                    selectedTail = selectedTailForHistory,
                                    onSelectTail = { viewModel.selectTailForHistory(it) },
                                    onClearTracks = { viewModel.clearFlightTracks() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Aircraft Type Specs & Photo Modal Dialog
        selectedTypeForInfo?.let { typeInfo ->
            AircraftTypeDetailDialog(
                typeInfo = typeInfo,
                aircraft = selectedAircraftForInfo,
                onDismiss = { viewModel.dismissTypeDetails() }
            )
        }

        // Airport Code (e.g. TRI), GPS Location & Radius Setup Dialog
        if (showLocationRadiusDialog) {
            RadarLocationRadiusDialog(
                currentCenter = radarCenter,
                currentRadiusNm = scanRadiusNm,
                isLocatingGps = isLocatingGps,
                statusMessage = locationMessage,
                onAirportSelected = { center ->
                    viewModel.setRadarCenter(center)
                },
                onGpsRequested = {
                    viewModel.acquireGpsLocation()
                },
                onRadiusChanged = { newRadius ->
                    viewModel.setScanRadius(newRadius)
                },
                onDismiss = {
                    showLocationRadiusDialog = false
                }
            )
        }

        // Wireless Screen Cast Modal
        if (showCastDialog) {
            CastScreenDialog(
                isCasting = isCasting,
                connectedDevice = connectedCastDevice,
                availableDevices = availableCastDevices,
                isScanning = isScanningCast,
                onConnectDevice = { device ->
                    viewModel.connectCastDevice(device)
                },
                onDisconnectCast = {
                    viewModel.disconnectCast()
                },
                onRefreshScan = {
                    viewModel.refreshCastScan()
                },
                onLaunchSystemCast = {
                    viewModel.launchSystemCast()
                },
                onDismiss = {
                    showCastDialog = false
                }
            )
        }
    }
}

@Composable
private fun ModeTabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeCount: Int = 0,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF00381B) else OledSurface,
            contentColor = if (isSelected) RadarGreen else AvionicsTextSecondary
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) RadarGreen else OledCardBorder
        )
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (badgeCount > 0) "$label ($badgeCount)" else label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}
