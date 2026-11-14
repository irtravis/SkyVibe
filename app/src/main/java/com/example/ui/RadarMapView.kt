package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AircraftDatabaseService
import com.example.data.AircraftRegistry
import com.example.model.Aircraft
import com.example.model.AircraftTypeInfo
import com.example.model.MapLayerType
import com.example.model.MapOrientationMode
import com.example.model.RadarCenter
import com.example.model.StormCell
import com.example.model.StormSeverity
import com.example.util.rememberDeviceHeading
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
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun RadarMapView(
    aircraftList: List<Aircraft>,
    radarCenter: RadarCenter,
    scanRadiusNm: Double,
    isPaused: Boolean,
    feedStatus: String = "",
    mapLayerType: MapLayerType = MapLayerType.RADAR,
    stormCells: List<StormCell> = emptyList(),
    isStormRadarEnabled: Boolean = true,
    selectedAircraft: Aircraft? = null,
    isMaximized: Boolean = false,
    onToggleMaximize: () -> Unit = {},
    onMapLayerChanged: (MapLayerType) -> Unit = {},
    onToggleStormRadar: () -> Unit = {},
    onOpenLocationPicker: () -> Unit = {},
    onSnapGps: () -> Unit = {},
    onAircraftSelected: (Aircraft) -> Unit,
    onDeselectAircraft: () -> Unit = {},
    onViewTypeDetails: (AircraftTypeInfo, Aircraft?) -> Unit,
    onToggleWatchlist: (String) -> Unit
) {
    // Continuously resolve the live updating instance of the selected aircraft from the latest telemetry scan
    val liveSelectedAircraft = remember(aircraftList, selectedAircraft) {
        if (selectedAircraft == null) null
        else aircraftList.find {
            it.tailNumber.equals(selectedAircraft.tailNumber, ignoreCase = true) ||
            it.icao24.equals(selectedAircraft.icao24, ignoreCase = true)
        } ?: selectedAircraft
    }

    // Zoom & Pan state: zoom out is bounded strictly to 1.0f (selected range), zoom in up to 5.0f
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Selected aircraft bottom detail card: state for sliding down so just tail number is visible
    var isDetailMinimized by remember(liveSelectedAircraft?.tailNumber) { mutableStateOf(false) }

    // Maximize mode sliding control & info panel state (variable sliding width with thin edge bar)
    var rightPanelWidthDp by remember { mutableFloatStateOf(260f) }
    var isRightPanelClosed by remember { mutableStateOf(false) }
    val animatedPanelWidthDp by animateDpAsState(
        targetValue = if (isMaximized && !isRightPanelClosed) rightPanelWidthDp.coerceIn(160f, 340f).dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "RightPanelWidthAnim"
    )

    // Map Orientation Mode: REGULAR (North Up), FOLLOW_NORTH (Phone Compass Gyro), FOLLOW_AIRCRAFT (Track Up)
    var orientationMode by remember { mutableStateOf(MapOrientationMode.REGULAR) }
    val deviceHeading by rememberDeviceHeading()

    val targetRotationDeg by remember(orientationMode, deviceHeading, liveSelectedAircraft) {
        derivedStateOf {
            when (orientationMode) {
                MapOrientationMode.REGULAR -> 0f
                MapOrientationMode.FOLLOW_NORTH -> -deviceHeading
                MapOrientationMode.FOLLOW_AIRCRAFT -> {
                    val targetHeading = liveSelectedAircraft?.headingDeg
                        ?: aircraftList.firstOrNull()?.headingDeg
                        ?: 0f
                    -targetHeading
                }
            }
        }
    }

    val animatedRotationDeg by animateFloatAsState(
        targetValue = targetRotationDeg,
        animationSpec = tween(durationMillis = 140),
        label = "MapRotationAngle"
    )

    val textMeasurer = rememberTextMeasurer()

    // Smooth Radar Sweep animation (stops when paused to save battery/cycles)
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                when (mapLayerType) {
                    MapLayerType.RADAR -> OledBlack
                    MapLayerType.SATELLITE -> Color(0xFF03070E)
                    MapLayerType.TERRAIN -> Color(0xFF0A0F0D)
                }
            )
            .testTag("radar_map_view")
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val centerX = width / 2f
        val centerY = height / 2f
        val maxRadiusPx = minOf(centerX, centerY) * (if (isMaximized) 0.94f else 0.88f)

        // Real Satellite & Terrain Bounding Box Map Imagery (ArcGIS Global World Imagery & Topo Map)
        val latDelta = scanRadiusNm / 60.0
        val lonDelta = scanRadiusNm / (60.0 * cos(Math.toRadians(radarCenter.latitude)).coerceAtLeast(0.2))
        val minLat = radarCenter.latitude - latDelta
        val maxLat = radarCenter.latitude + latDelta
        val minLon = radarCenter.longitude - lonDelta
        val maxLon = radarCenter.longitude + lonDelta

        val mapExportUrl = remember(radarCenter.latitude, radarCenter.longitude, scanRadiusNm, mapLayerType) {
            when (mapLayerType) {
                MapLayerType.SATELLITE ->
                    "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/export?bbox=$minLon,$minLat,$maxLon,$maxLat&bboxSR=4326&imageSR=4326&size=1024,1024&format=jpg&f=image"
                MapLayerType.TERRAIN ->
                    "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/export?bbox=$minLon,$minLat,$maxLon,$maxLat&bboxSR=4326&imageSR=4326&size=1024,1024&format=jpg&f=image"
                MapLayerType.RADAR -> null
            }
        }

        // Real Satellite Aerial Imagery or Topographic Elevation Layer (rendered inside the radar circle)
        if (mapExportUrl != null) {
            val diameterDp = with(LocalDensity.current) { (maxRadiusPx * 2).toDp() }
            Box(
                modifier = Modifier
                    .size(diameterDp)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                        rotationZ = animatedRotationDeg
                    }
                    .clip(CircleShape)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(mapExportUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${mapLayerType.displayName} Map Layer",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(aircraftList, scanRadiusNm, maxRadiusPx, centerX, centerY, animatedRotationDeg) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var pastTouchSlop = false
                        val touchSlop = viewConfiguration.touchSlop

                        do {
                            val event = awaitPointerEvent()
                            val canceled = event.changes.any { it.isConsumed }
                            if (canceled) break

                            if (event.changes.size > 1) {
                                // Multi-touch pinch zoom
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val newScale = (zoomScale * zoomChange).coerceIn(1.0f, 5.0f)
                                zoomScale = newScale
                                if (newScale <= 1.0f) {
                                    panOffset = Offset.Zero
                                } else {
                                    val maxPan = maxRadiusPx * (newScale - 1f)
                                    panOffset = Offset(
                                        x = (panOffset.x + panChange.x).coerceIn(-maxPan, maxPan),
                                        y = (panOffset.y + panChange.y).coerceIn(-maxPan, maxPan)
                                    )
                                }
                                event.changes.forEach { it.consume() }
                                pastTouchSlop = true
                            } else if (event.changes.size == 1) {
                                val change = event.changes[0]
                                val dragDistance = (change.position - down.position).getDistance()
                                if (dragDistance > touchSlop) {
                                    pastTouchSlop = true
                                    if (zoomScale > 1.0f) {
                                        val panChange = change.position - change.previousPosition
                                        val maxPan = maxRadiusPx * (zoomScale - 1f)
                                        panOffset = Offset(
                                            x = (panOffset.x + panChange.x).coerceIn(-maxPan, maxPan),
                                            y = (panOffset.y + panChange.y).coerceIn(-maxPan, maxPan)
                                        )
                                        change.consume()
                                    }
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        if (!pastTouchSlop) {
                            val tapOffset = down.position
                            var closest: Aircraft? = null
                            var minDistancePx = 52f

                            aircraftList.forEach { ac ->
                                val (baseX, baseY) = calculateBlipPosition(
                                    centerLat = radarCenter.latitude,
                                    centerLon = radarCenter.longitude,
                                    targetLat = ac.latitude,
                                    targetLon = ac.longitude,
                                    radarCenterX = centerX,
                                    radarCenterY = centerY,
                                    maxRadiusPx = maxRadiusPx,
                                    maxRangeNm = scanRadiusNm
                                )
                                val screenPos = calculateScreenPosition(
                                    baseX = baseX,
                                    baseY = baseY,
                                    centerX = centerX,
                                    centerY = centerY,
                                    rotationDeg = animatedRotationDeg,
                                    zoomScale = zoomScale,
                                    panOffset = panOffset
                                )
                                val dist = hypot((tapOffset.x - screenPos.x).toDouble(), (tapOffset.y - screenPos.y).toDouble()).toFloat()
                                if (dist < minDistancePx) {
                                    minDistancePx = dist
                                    closest = ac
                                }
                            }

                            if (closest != null) {
                                onAircraftSelected(closest!!)
                            } else {
                                onDeselectAircraft()
                            }
                        }
                    }
                }
        ) {
            withTransform({
                translate(panOffset.x, panOffset.y)
                scale(zoomScale, zoomScale, pivot = Offset(centerX, centerY))
                rotate(animatedRotationDeg, pivot = Offset(centerX, centerY))
            }) {
                // 1. Draw Background Layer (Radar Grid / Satellite Nocturnal Earth / Terrain Sectional)
                when (mapLayerType) {
                    MapLayerType.RADAR -> {
                        drawRadarGrid(
                            centerX = centerX,
                            centerY = centerY,
                            maxRadiusPx = maxRadiusPx,
                            maxRangeNm = scanRadiusNm,
                            textMeasurer = textMeasurer,
                            mapRotationDeg = animatedRotationDeg
                        )
                        if (!isPaused) {
                            drawRadarSweep(
                                centerX = centerX,
                                centerY = centerY,
                                maxRadiusPx = maxRadiusPx,
                                currentAngle = sweepAngle
                            )
                        }
                    }
                    MapLayerType.SATELLITE -> {
                        drawSatelliteLayer(
                            centerX = centerX,
                            centerY = centerY,
                            maxRadiusPx = maxRadiusPx,
                            maxRangeNm = scanRadiusNm,
                            textMeasurer = textMeasurer,
                            mapRotationDeg = animatedRotationDeg
                        )
                    }
                    MapLayerType.TERRAIN -> {
                        drawTerrainLayer(
                            centerX = centerX,
                            centerY = centerY,
                            maxRadiusPx = maxRadiusPx,
                            maxRangeNm = scanRadiusNm,
                            textMeasurer = textMeasurer,
                            mapRotationDeg = animatedRotationDeg
                        )
                    }
                }

                // 2. Draw Storm Weather Radar Overlay (Doppler Reflectivity & Storm Proximity Vectors)
                if (isStormRadarEnabled && stormCells.isNotEmpty()) {
                    drawStormRadarOverlay(
                        centerX = centerX,
                        centerY = centerY,
                        maxRadiusPx = maxRadiusPx,
                        scanRadiusNm = scanRadiusNm,
                        radarCenter = radarCenter,
                        stormCells = stormCells,
                        textMeasurer = textMeasurer
                    )

                    drawStormProximityVectors(
                        aircraftList = aircraftList,
                        stormCells = stormCells,
                        radarCenter = radarCenter,
                        centerX = centerX,
                        centerY = centerY,
                        maxRadiusPx = maxRadiusPx,
                        scanRadiusNm = scanRadiusNm,
                        selectedAircraft = liveSelectedAircraft,
                        textMeasurer = textMeasurer
                    )
                }

                // 3. Draw Aircraft Blips, Heading Vectors, and Identification Tags
                aircraftList.forEach { ac ->
                    val (blipX, blipY) = calculateBlipPosition(
                        centerLat = radarCenter.latitude,
                        centerLon = radarCenter.longitude,
                        targetLat = ac.latitude,
                        targetLon = ac.longitude,
                        radarCenterX = centerX,
                        radarCenterY = centerY,
                        maxRadiusPx = maxRadiusPx,
                        maxRangeNm = scanRadiusNm
                    )

                    val isSelected = selectedAircraft?.icao24 == ac.icao24

                    drawAircraftTarget(
                        x = blipX,
                        y = blipY,
                        aircraft = ac,
                        isSelected = isSelected,
                        mapLayerType = mapLayerType,
                        textMeasurer = textMeasurer,
                        mapRotationDeg = animatedRotationDeg
                    )
                }
            }
        }

        // Honest empty/offline live feed indicator when no airborne targets are in range
        if (aircraftList.isEmpty() && !isMaximized) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xD8040B07))
                    .border(1.dp, OledCardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = feedStatus.ifBlank { "NO AIRBORNE ADS-B TARGETS IN ${scanRadiusNm.toInt()} NM" },
                    color = if (feedStatus.startsWith("SIGNAL LOST")) RadarAmber else AvionicsTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Top Status HUD & Map Controls (Hidden when isMaximized to provide pure full-scope view)
        if (!isMaximized) {
            // 1. Top Status HUD with Location Selector, GPS Snap, Storm Toggle, and Map Layer Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Controls: Airport / GPS Selector & Quick GPS Snap
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Airport / Center & Radius Info Tag (Clickable)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(OledSurface)
                            .border(1.dp, if (radarCenter.isGps) RadarGreen else RadarCyan, RoundedCornerShape(8.dp))
                            .clickable { onOpenLocationPicker() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("radar_hud_location_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (radarCenter.isGps) Icons.Default.LocationOn else Icons.Default.AirplanemodeActive,
                                contentDescription = "Center Location",
                                tint = if (radarCenter.isGps) RadarGreen else RadarCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${radarCenter.id.uppercase()} · ${scanRadiusNm.toInt()}NM ▾",
                                color = if (radarCenter.isGps) RadarGreen else RadarCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Quick GPS Snap Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (radarCenter.isGps) Color(0xFF00381B) else OledSurface)
                            .border(1.dp, RadarGreen, RoundedCornerShape(8.dp))
                            .clickable { onSnapGps() }
                            .padding(horizontal = 6.dp, vertical = 5.dp)
                            .testTag("radar_hud_gps_snap")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Snap to GPS",
                                tint = RadarGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (radarCenter.isGps) "GPS" else "⌖ GPS",
                                color = RadarGreen,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Right Controls: Storm Radar Toggle + Map Layer Switcher
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Storm Radar Toggle Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isStormRadarEnabled) Color(0xFF330030) else OledSurface)
                            .border(1.dp, if (isStormRadarEnabled) Color(0xFFD500F9) else OledCardBorder, RoundedCornerShape(8.dp))
                            .clickable { onToggleStormRadar() }
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                            .testTag("radar_hud_storm_toggle")
                    ) {
                        Text(
                            text = if (isStormRadarEnabled) {
                                if (com.example.data.StormRadarManager.hasActivePrecipitation) "⛈ WX: STORM"
                                else "☀️ WX: CLEAR"
                            } else "⛈ WX: OFF",
                            color = if (isStormRadarEnabled) {
                                if (com.example.data.StormRadarManager.hasActivePrecipitation) Color(0xFFEA80FC)
                                else RadarGreen
                            } else AvionicsTextDim,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Map Layer Switcher Pill: [RADAR (green scanning screen icon) | SATELLITE (🛰️) | TERRAIN (⛰️)]
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(OledSurface)
                            .border(1.dp, OledCardBorder, RoundedCornerShape(20.dp))
                            .padding(2.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MapLayerType.values().forEach { layer ->
                                val isSelected = layer == mapLayerType
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) {
                                                when (layer) {
                                                    MapLayerType.RADAR -> Color(0xFF00381B)
                                                    MapLayerType.SATELLITE -> Color(0xFF003544)
                                                    MapLayerType.TERRAIN -> Color(0xFF332200)
                                                }
                                            } else Color.Transparent
                                        )
                                        .clickable { onMapLayerChanged(layer) }
                                        .padding(horizontal = 7.dp, vertical = 4.dp)
                                        .testTag("layer_${layer.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (layer) {
                                        MapLayerType.RADAR -> MiniRadarScreenIcon(isSelected = isSelected)
                                        MapLayerType.SATELLITE -> Text("🛰️", fontSize = 13.sp)
                                        MapLayerType.TERRAIN -> Text("⛰️", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Map Orientation Mode Toggle (Top-Left of Map)
            // One button with three options: REGULAR (North-Up) -> FOLLOW NORTH (Compass/GPS) -> FOLLOW AIRCRAFT (Track-Up)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 10.dp, top = 50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(OledSurface.copy(alpha = 0.94f))
                    .border(
                        width = 1.2.dp,
                        color = when (orientationMode) {
                            MapOrientationMode.REGULAR -> OledCardBorder
                            MapOrientationMode.FOLLOW_NORTH -> RadarCyan
                            MapOrientationMode.FOLLOW_AIRCRAFT -> RadarAmber
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        orientationMode = when (orientationMode) {
                            MapOrientationMode.REGULAR -> MapOrientationMode.FOLLOW_NORTH
                            MapOrientationMode.FOLLOW_NORTH -> MapOrientationMode.FOLLOW_AIRCRAFT
                            MapOrientationMode.FOLLOW_AIRCRAFT -> MapOrientationMode.REGULAR
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("btn_map_orientation_toggle")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = when (orientationMode) {
                            MapOrientationMode.REGULAR -> Icons.Default.Explore
                            MapOrientationMode.FOLLOW_NORTH -> Icons.Default.Navigation
                            MapOrientationMode.FOLLOW_AIRCRAFT -> Icons.Default.AirplanemodeActive
                        },
                        contentDescription = orientationMode.title,
                        tint = when (orientationMode) {
                            MapOrientationMode.REGULAR -> RadarGreen
                            MapOrientationMode.FOLLOW_NORTH -> RadarCyan
                            MapOrientationMode.FOLLOW_AIRCRAFT -> RadarAmber
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = when (orientationMode) {
                                MapOrientationMode.REGULAR -> "REGULAR (N-UP)"
                                MapOrientationMode.FOLLOW_NORTH -> "FOLLOW NORTH"
                                MapOrientationMode.FOLLOW_AIRCRAFT -> "FOLLOW AIRCRAFT"
                            },
                            color = when (orientationMode) {
                                MapOrientationMode.REGULAR -> AvionicsTextPrimary
                                MapOrientationMode.FOLLOW_NORTH -> RadarCyan
                                MapOrientationMode.FOLLOW_AIRCRAFT -> RadarAmber
                            },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (orientationMode) {
                                MapOrientationMode.REGULAR -> "000° FIXED"
                                MapOrientationMode.FOLLOW_NORTH -> "${deviceHeading.toInt()}° COMPASS"
                                MapOrientationMode.FOLLOW_AIRCRAFT -> {
                                    val hdg = liveSelectedAircraft?.headingDeg ?: 0f
                                    "${hdg.toInt()}° TRACK"
                                }
                            },
                            color = AvionicsTextDim,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 3. Floating Zoom In / Out / Reset Controls (Right Edge of Map)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = if (isMaximized && !isRightPanelClosed) (animatedPanelWidthDp + 10.dp) else 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zoom In (+)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(OledSurface.copy(alpha = 0.92f))
                        .border(1.2.dp, if (zoomScale < 5.0f) RadarGreen else OledCardBorder, CircleShape)
                        .clickable(enabled = zoomScale < 5.0f) {
                            zoomScale = (zoomScale + 0.5f).coerceAtMost(5.0f)
                        }
                        .testTag("btn_radar_zoom_in"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = if (zoomScale < 5.0f) RadarGreen else AvionicsTextDim,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Zoom Level Reset Pill (Shows "1.5x", tapping resets to 1.0x & center)
                if (zoomScale > 1.0f || panOffset != Offset.Zero) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF00381B))
                            .border(1.dp, RadarGreen, RoundedCornerShape(12.dp))
                            .clickable {
                                zoomScale = 1.0f
                                panOffset = Offset.Zero
                            }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                            .testTag("btn_radar_zoom_reset")
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1fx", zoomScale),
                            color = RadarGreen,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Zoom Out (-) strictly bounded to 1.0f (selected range, not a global zoom)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(OledSurface.copy(alpha = 0.92f))
                        .border(1.2.dp, if (zoomScale > 1.0f) RadarGreen else OledCardBorder, CircleShape)
                        .clickable(enabled = zoomScale > 1.0f) {
                            val newScale = (zoomScale - 0.5f).coerceAtLeast(1.0f)
                            zoomScale = newScale
                            if (newScale <= 1.0f) panOffset = Offset.Zero
                        }
                        .testTag("btn_radar_zoom_out"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = if (zoomScale > 1.0f) RadarGreen else AvionicsTextDim,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 4. NEXRAD Doppler Weather Reflectivity Scale Legend
            if (isStormRadarEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = 12.dp,
                            bottom = if (liveSelectedAircraft != null) (if (isDetailMinimized) 64.dp else 230.dp) else 12.dp
                        )
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xD0000000))
                        .border(1.dp, OledCardBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Column {
                        Text(
                            text = "NEXRAD DOPPLER dBZ",
                            color = AvionicsTextDim,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            DbzBlock(color = Color(0xFF00E676), label = "20-35")
                            DbzBlock(color = Color(0xFFFFD600), label = "35-45")
                            DbzBlock(color = Color(0xFFFF1744), label = "45-55")
                            DbzBlock(color = Color(0xFFD500F9), label = "55+")
                        }
                    }
                }
            }
        }

        // Target Inspection Bottom Card (continually streams live data for selected plane)
        // Can be slid down so just the plane tail number is visible, allowing the radar screen to be seen while plane is still selected
        liveSelectedAircraft?.let { target ->
            val liveMeta = AircraftDatabaseService.getCachedOrFleet(target.tailNumber, target.icao24, target.registeredOperator)
            val effectiveTypeCode = liveMeta?.icaoTypeCode?.ifBlank { null } ?: target.aircraftTypeCode
            val effectiveModelName = liveMeta?.model?.ifBlank { null } ?: target.aircraftTypeName
            val typeInfo = AircraftRegistry.getAircraftType(
                code = effectiveTypeCode,
                descriptionHint = effectiveModelName,
                isHelicopterHint = target.isHelicopter || liveMeta?.isHelicopter == true
            )
            val isHeli = target.isHelicopter || typeInfo.isHelicopter || liveMeta?.isHelicopter == true
            val pubReg = target.publicRegistry

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 10.dp,
                        end = if (isMaximized && !isRightPanelClosed) (animatedPanelWidthDp + 10.dp) else 10.dp,
                        bottom = 10.dp
                    )
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        1.5.dp,
                        if (target.isWatchlisted) RadarAmber else if (isHeli) RadarCyan else RadarGreen,
                        RoundedCornerShape(14.dp)
                    )
                    .animateContentSize()
                    .pointerInput(target.tailNumber) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 12) {
                                isDetailMinimized = true // Swiped down
                            } else if (dragAmount < -12) {
                                isDetailMinimized = false // Swiped up
                            }
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = OledBlack.copy(alpha = 0.96f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Thin Top Drag Handle to indicate the window can be slid down or expanded
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF334A3E))
                            .clickable { isDetailMinimized = !isDetailMinimized }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Top header: Icon, Identification, Watchlist/Live stream badge, Distance, Slide Down / Expand, Close
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDetailMinimized = !isDetailMinimized },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isHeli) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_helicopter),
                                    contentDescription = "Helicopter",
                                    tint = RadarCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AirplanemodeActive,
                                    contentDescription = "Airplane",
                                    tint = RadarGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = target.displayId,
                                color = AvionicsTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF2E0000))
                                    .border(1.dp, Color(0xFFFF1744), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "🔴 LIVE",
                                    color = Color(0xFFFF5252),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (target.isWatchlisted) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF331400))
                                        .border(1.dp, RadarAmber, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "🚨 WATCH",
                                        color = RadarAmber,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale.US, "%.1f NM", target.distanceNm),
                                color = RadarGreen,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(4.dp))

                            // Slide Down (Minimize) / Expand Toggle Button
                            IconButton(
                                onClick = { isDetailMinimized = !isDetailMinimized },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDetailMinimized) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isDetailMinimized) "Expand Details" else "Slide Down to Tail Number",
                                    tint = RadarCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(2.dp))

                            IconButton(
                                onClick = onDeselectAircraft,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Target Panel",
                                    tint = AvionicsTextDim,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (isDetailMinimized) {
                        // MINIMIZED STATE: Just the aircraft callsign / hex & concise stats visible, allowing full radar visibility
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${target.displayId} · $effectiveModelName · ${target.formattedAltitude} · ${target.velocityKts} kts · ${target.headingDeg.toInt()}°",
                                color = AvionicsTextSecondary,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "▲ SLIDE UP",
                                color = RadarCyan,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        // EXPANDED STATE: Full avionics details, aircraft type, telemetry grid, and action buttons
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$effectiveModelName · HEX #${target.icao24.uppercase()}",
                            color = RadarCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        Text(
                            text = "${liveMeta?.manufacturer ?: typeInfo.manufacturer} · SQUAWK ${target.squawk} · ADS-B 1090MHz",
                            color = AvionicsTextDim,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )

                        // Storm cell proximity warning banner
                        if (isStormRadarEnabled && target.nearestStormName != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (target.isInsideStorm) Color(0xFF3B0B0B) else Color(0xFF261D00))
                                    .border(1.dp, if (target.isInsideStorm) Color(0xFFFF1744) else RadarAmber, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (target.isInsideStorm) "⚠️ IN STORM CELL: ${target.nearestStormName}" else "⚡ PROX: ${target.nearestStormName}",
                                        color = if (target.isInsideStorm) Color(0xFFFF1744) else RadarAmber,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (target.isInsideStorm) "HAIL CORE" else "${String.format(Locale.US, "%.1f", target.nearestStormDistanceNm ?: 0.0)} NM (${target.nearestStormDbz} dBZ)",
                                        color = if (target.isInsideStorm) Color(0xFFFF1744) else RadarAmber,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Live Telemetry Grid (continually updates with ADS-B scans)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("ALTITUDE", color = AvionicsTextDim, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text(target.formattedAltitude, color = AvionicsTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("${target.altitudeFt} ft", color = AvionicsTextDim, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            }
                            Column {
                                Text("GROUND SPEED", color = AvionicsTextDim, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text("${target.velocityKts} kts", color = AvionicsTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("${(target.velocityKts * 1.15).toInt()} mph", color = AvionicsTextDim, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            }
                            Column {
                                Text("V/S RATE", color = AvionicsTextDim, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                val (vsText, vsColor) = when {
                                    target.verticalRateFpm > 100 -> "▲ +${target.verticalRateFpm}" to RadarGreen
                                    target.verticalRateFpm < -100 -> "▼ ${target.verticalRateFpm}" to RadarAmber
                                    else -> "▶ LEVEL" to AvionicsTextDim
                                }
                                Text(vsText, color = vsColor, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("fpm", color = AvionicsTextDim, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            }
                            Column {
                                Text("TRACK", color = AvionicsTextDim, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text("${target.headingDeg.toInt()}°", color = AvionicsTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(target.squawk, color = RadarCyan, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "POS: ${String.format(Locale.US, "%.4f°N, %.4f°W", target.latitude, -target.longitude)} · ADS-B 1090MHz LIVE",
                            color = AvionicsTextDim,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onViewTypeDetails(typeInfo, target) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OledSurface,
                                    contentColor = if (isHeli) RadarCyan else RadarGreen
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isHeli) RadarCyan else RadarGreen)
                            ) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${if (isHeli) "🚁" else "✈"} $effectiveTypeCode SPECS & VERIFY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Button(
                                onClick = { onToggleWatchlist(target.tailNumber) },
                                modifier = Modifier.weight(0.7f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (target.isWatchlisted) Color(0xFF331400) else Color(0xFF00381B),
                                    contentColor = if (target.isWatchlisted) RadarAmber else RadarGreen
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (target.isWatchlisted) RadarAmber else RadarGreen)
                            ) {
                                Icon(
                                    imageVector = if (target.isWatchlisted) Icons.Default.Warning else Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (target.isWatchlisted) "UNWATCH" else "WATCH",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. MAXIMIZE MODE: Right Control and Info Panel
        // Can be slid closed variably with a small thin vertical bar on the edge to make more space for the radar screen
        if (isMaximized) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Small Thin Vertical Bar on the edge of the panel to indicate it can be slid to the right
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .fillMaxHeight()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (rightPanelWidthDp < 90f) {
                                        isRightPanelClosed = true
                                    }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    // Dragging right (dragAmount > 0) shrinks/closes panel to the right
                                    // Dragging left (dragAmount < 0) widens panel
                                    val newWidth = (rightPanelWidthDp - dragAmount).coerceIn(0f, 360f)
                                    rightPanelWidthDp = newWidth
                                    isRightPanelClosed = newWidth <= 50f
                                }
                            )
                        }
                        .clickable {
                            isRightPanelClosed = !isRightPanelClosed
                            if (!isRightPanelClosed && rightPanelWidthDp < 160f) {
                                rightPanelWidthDp = 260f
                            }
                        }
                        .testTag("radar_panel_slider_bar"),
                    contentAlignment = Alignment.Center
                ) {
                    // Thin vertical indicator line
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isRightPanelClosed) RadarCyan else RadarGreen)
                    )

                    // Small directional tab badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(6.dp))
                            .background(OledSurface)
                            .border(1.dp, if (isRightPanelClosed) RadarCyan else RadarGreen, RoundedCornerShape(6.dp))
                            .padding(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isRightPanelClosed) "‹" else "›",
                            color = if (isRightPanelClosed) RadarCyan else RadarGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Variable Sliding Right Control & Info Panel Content
                if (animatedPanelWidthDp > 4.dp) {
                    Surface(
                        modifier = Modifier
                            .width(animatedPanelWidthDp)
                            .fillMaxHeight()
                            .border(1.dp, OledCardBorder),
                        color = OledBlack.copy(alpha = 0.95f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Panel Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = RadarGreen,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "CONTROLS & INFO",
                                        color = AvionicsTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(OledSurface)
                                        .clickable { isRightPanelClosed = true }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "SLIDE ›",
                                        color = AvionicsTextDim,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Selected Target Info (if any)
                            if (liveSelectedAircraft != null) {
                                val target = liveSelectedAircraft
                                val typeInfo = AircraftRegistry.getAircraftType(
                                    code = target.aircraftTypeCode,
                                    descriptionHint = target.aircraftTypeName,
                                    isHelicopterHint = target.isHelicopter
                                )
                                val isHeli = target.isHelicopter || typeInfo.isHelicopter
                                val pubReg = target.publicRegistry

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isHeli) RadarCyan else RadarGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = target.displayId,
                                                color = AvionicsTextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = String.format(Locale.US, "%.1f NM", target.distanceNm),
                                                color = RadarGreen,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Text(
                                            text = "${target.aircraftTypeName} · HEX #${target.icao24.uppercase()}",
                                            color = RadarCyan,
                                            fontSize = 9.5.sp,
                                            maxLines = 1,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "${target.formattedAltitude} · ${target.velocityKts} kts · ${target.headingDeg.toInt()}° · SQWK ${target.squawk}",
                                            color = RadarGreen,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = { onViewTypeDetails(typeInfo, target) },
                                            modifier = Modifier.fillMaxWidth().height(28.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00381B)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = "SPECS & GOOGLE VERIFY",
                                                fontSize = 8.5.sp,
                                                color = RadarGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Layer Switcher
                            Text("MAP LAYER", color = AvionicsTextDim, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                MapLayerType.values().forEach { layer ->
                                    val isSelected = layer == mapLayerType
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) {
                                                    when (layer) {
                                                        MapLayerType.RADAR -> Color(0xFF00381B)
                                                        MapLayerType.SATELLITE -> Color(0xFF003544)
                                                        MapLayerType.TERRAIN -> Color(0xFF332200)
                                                    }
                                                } else OledSurface
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) {
                                                    when (layer) {
                                                        MapLayerType.RADAR -> RadarGreen
                                                        MapLayerType.SATELLITE -> RadarCyan
                                                        MapLayerType.TERRAIN -> RadarAmber
                                                    }
                                                } else OledCardBorder,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { onMapLayerChanged(layer) }
                                            .padding(vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (layer) {
                                            MapLayerType.RADAR -> MiniRadarScreenIcon(isSelected = isSelected)
                                            MapLayerType.SATELLITE -> Text("🛰️", fontSize = 13.sp)
                                            MapLayerType.TERRAIN -> Text("⛰️", fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Storm Radar Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isStormRadarEnabled) Color(0xFF330030) else OledSurface)
                                    .border(1.dp, if (isStormRadarEnabled) Color(0xFFD500F9) else OledCardBorder, RoundedCornerShape(6.dp))
                                    .clickable { onToggleStormRadar() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⛈ NEXRAD STORM RADAR",
                                    color = if (isStormRadarEnabled) Color(0xFFEA80FC) else AvionicsTextDim,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isStormRadarEnabled) "ON" else "OFF",
                                    color = if (isStormRadarEnabled) Color(0xFFEA80FC) else AvionicsTextDim,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Center & GPS Snap
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(OledSurface)
                                        .border(1.dp, RadarCyan, RoundedCornerShape(6.dp))
                                        .clickable { onOpenLocationPicker() }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "📍 ${radarCenter.id.uppercase()}",
                                        color = RadarCyan,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (radarCenter.isGps) Color(0xFF00381B) else OledSurface)
                                        .border(1.dp, if (radarCenter.isGps) RadarGreen else OledCardBorder, RoundedCornerShape(6.dp))
                                        .clickable { onSnapGps() }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "⌖ GPS SNAP",
                                        color = if (radarCenter.isGps) RadarGreen else AvionicsTextDim,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Live Contacts in Radar Airspace
                            Text(
                                text = "CONTACTS IN RANGE (${aircraftList.size})",
                                color = AvionicsTextDim,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            aircraftList.take(15).forEach { ac ->
                                val isAcSelected = liveSelectedAircraft?.icao24 == ac.icao24
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isAcSelected) Color(0xFF00381B) else Color(0xFF080F0C))
                                        .border(1.dp, if (isAcSelected) RadarGreen else Color(0xFF14241B), RoundedCornerShape(4.dp))
                                        .clickable { onAircraftSelected(ac) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ac.displayId,
                                            color = if (isAcSelected) RadarGreen else AvionicsTextPrimary,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "${ac.formattedAltitude} · ${ac.velocityKts}k · ${ac.aircraftTypeCode}",
                                            color = AvionicsTextSecondary,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = String.format(Locale.US, "%.1f NM", ac.distanceNm),
                                        color = if (isAcSelected) RadarGreen else RadarCyan,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                            }
                        }
                    }
                }
            }
        }

        // Maximize Screen / Return Button in Lower-Right
        // In normal view: allows user to maximize radar screen to fill entire phone screen with no controls
        // In maximized view: allows user to restore/return to normal view
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = if (isMaximized && !isRightPanelClosed) (animatedPanelWidthDp + 12.dp) else 12.dp,
                    bottom = if (liveSelectedAircraft != null) (if (isDetailMinimized) 64.dp else 230.dp) else (if (isMaximized) 18.dp else 12.dp)
                )
                .clip(RoundedCornerShape(10.dp))
                .background(if (isMaximized) Color(0xFF00381B) else OledSurface.copy(alpha = 0.94f))
                .border(1.2.dp, if (isMaximized) RadarGreen else RadarCyan, RoundedCornerShape(10.dp))
                .clickable { onToggleMaximize() }
                .padding(horizontal = 10.dp, vertical = 7.dp)
                .testTag("btn_radar_fullscreen_toggle")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isMaximized) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isMaximized) "Return to Normal View" else "Maximize Radar Screen",
                    tint = if (isMaximized) RadarGreen else RadarCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isMaximized) "RESTORE" else "MAXIMIZE",
                    color = if (isMaximized) RadarGreen else RadarCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 1. RADAR LAYER DRAWING
private fun DrawScope.drawRadarGrid(
    centerX: Float,
    centerY: Float,
    maxRadiusPx: Float,
    maxRangeNm: Double,
    textMeasurer: TextMeasurer,
    mapRotationDeg: Float = 0f
) {
    val ringRatios = listOf(0.25f, 0.50f, 0.75f, 1.0f)
    val ringColor = Color(0x3300E676)
    val crosshairColor = Color(0x2200E676)

    drawLine(crosshairColor, Offset(centerX - maxRadiusPx, centerY), Offset(centerX + maxRadiusPx, centerY), 1.dp.toPx())
    drawLine(crosshairColor, Offset(centerX, centerY - maxRadiusPx), Offset(centerX, centerY + maxRadiusPx), 1.dp.toPx())

    ringRatios.forEach { ratio ->
        val radiusPx = maxRadiusPx * ratio
        drawCircle(
            color = ringColor,
            radius = radiusPx,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.dp.toPx())
        )

        val ringNm = (maxRangeNm * ratio).toInt()
        val textLayoutResult = textMeasurer.measure(
            text = "$ringNm NM",
            style = TextStyle(
                color = Color(0x6600E676),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )
        val textPos = Offset(centerX + 6.dp.toPx(), centerY - radiusPx + 2.dp.toPx())
        if (mapRotationDeg != 0f) {
            rotate(degrees = -mapRotationDeg, pivot = textPos) {
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = textPos
                )
            }
        } else {
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = textPos
            )
        }
    }

    val cardinals = listOf(
        "000° N" to Offset(centerX - 14.dp.toPx(), centerY - maxRadiusPx - 16.dp.toPx()),
        "090° E" to Offset(centerX + maxRadiusPx + 4.dp.toPx(), centerY - 6.dp.toPx()),
        "180° S" to Offset(centerX - 14.dp.toPx(), centerY + maxRadiusPx + 4.dp.toPx()),
        "270° W" to Offset(centerX - maxRadiusPx - 38.dp.toPx(), centerY - 6.dp.toPx())
    )

    cardinals.forEach { (label, pos) ->
        val result = textMeasurer.measure(
            text = label,
            style = TextStyle(color = RadarGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        )
        if (mapRotationDeg != 0f) {
            rotate(degrees = -mapRotationDeg, pivot = pos) {
                drawText(result, topLeft = pos)
            }
        } else {
            drawText(result, topLeft = pos)
        }
    }
}

private fun DrawScope.drawRadarSweep(
    centerX: Float,
    centerY: Float,
    maxRadiusPx: Float,
    currentAngle: Float
) {
    rotate(degrees = currentAngle, pivot = Offset(centerX, centerY)) {
        drawLine(
            color = RadarGreen,
            start = Offset(centerX, centerY),
            end = Offset(centerX + maxRadiusPx, centerY),
            strokeWidth = 2.dp.toPx()
        )

        val sweepBrush = Brush.radialGradient(
            colors = listOf(Color(0x4400E676), Color(0x1100E676), Color.Transparent),
            center = Offset(centerX, centerY),
            radius = maxRadiusPx
        )
        drawCircle(
            brush = sweepBrush,
            radius = maxRadiusPx,
            center = Offset(centerX, centerY)
        )
    }
}

// 2. SATELLITE LAYER OVERLAY (Over Real Orbital Satellite Imagery)
private fun DrawScope.drawSatelliteLayer(
    centerX: Float,
    centerY: Float,
    maxRadiusPx: Float,
    maxRangeNm: Double,
    textMeasurer: TextMeasurer,
    mapRotationDeg: Float = 0f
) {
    // Outer atmospheric cyan perimeter ring
    drawCircle(
        color = Color(0x9900E5FF),
        radius = maxRadiusPx,
        center = Offset(centerX, centerY),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Orbital distance range rings (25%, 50%, 75%, 100%)
    val ringRatios = listOf(0.25f, 0.50f, 0.75f, 1.0f)
    val gridColor = Color(0x4400E5FF)
    ringRatios.forEach { ratio ->
        val radiusPx = maxRadiusPx * ratio
        drawCircle(
            color = gridColor,
            radius = radiusPx,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.dp.toPx())
        )

        val ringNm = (maxRangeNm * ratio).toInt()
        val textLayoutResult = textMeasurer.measure(
            text = "$ringNm NM",
            style = TextStyle(
                color = Color(0xDD00E5FF),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )
        val textPos = Offset(centerX + 6.dp.toPx(), centerY - radiusPx + 2.dp.toPx())
        if (mapRotationDeg != 0f) {
            rotate(degrees = -mapRotationDeg, pivot = textPos) {
                drawText(textLayoutResult = textLayoutResult, topLeft = textPos)
            }
        } else {
            drawText(textLayoutResult = textLayoutResult, topLeft = textPos)
        }
    }

    // Origin Crosshair at Airport / GPS Center
    val crossColor = Color(0x6600E5FF)
    drawLine(crossColor, Offset(centerX - 16.dp.toPx(), centerY), Offset(centerX + 16.dp.toPx(), centerY), 1.dp.toPx())
    drawLine(crossColor, Offset(centerX, centerY - 16.dp.toPx()), Offset(centerX, centerY + 16.dp.toPx()), 1.dp.toPx())

    // Cardinal Points
    val cardinals = listOf(
        "000° N" to Offset(centerX - 14.dp.toPx(), centerY - maxRadiusPx - 16.dp.toPx()),
        "090° E" to Offset(centerX + maxRadiusPx + 4.dp.toPx(), centerY - 6.dp.toPx()),
        "180° S" to Offset(centerX - 14.dp.toPx(), centerY + maxRadiusPx + 4.dp.toPx()),
        "270° W" to Offset(centerX - maxRadiusPx - 38.dp.toPx(), centerY - 6.dp.toPx())
    )
    cardinals.forEach { (label, pos) ->
        val result = textMeasurer.measure(
            text = label,
            style = TextStyle(color = RadarCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        )
        if (mapRotationDeg != 0f) {
            rotate(degrees = -mapRotationDeg, pivot = pos) {
                drawText(result, topLeft = pos)
            }
        } else {
            drawText(result, topLeft = pos)
        }
    }

    // Satellite Attribution Label
    val satResult = textMeasurer.measure(
        text = "SATELLITE · DIGITALGLOBE / EARTHSTAR / USGS",
        style = TextStyle(color = Color(0xDD00E5FF), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    )
    val labelPos = Offset(centerX - maxRadiusPx * 0.55f, centerY + maxRadiusPx * 0.88f)
    if (mapRotationDeg != 0f) {
        rotate(degrees = -mapRotationDeg, pivot = labelPos) {
            drawText(satResult, topLeft = labelPos)
        }
    } else {
        drawText(satResult, topLeft = labelPos)
    }
}

// 3. TERRAIN LAYER OVERLAY (Over Real Topographic Elevation Map)
private fun DrawScope.drawTerrainLayer(
    centerX: Float,
    centerY: Float,
    maxRadiusPx: Float,
    maxRangeNm: Double,
    textMeasurer: TextMeasurer,
    mapRotationDeg: Float = 0f
) {
    // Outer topographic amber perimeter ring
    drawCircle(
        color = Color(0x99FFA000),
        radius = maxRadiusPx,
        center = Offset(centerX, centerY),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Topographic range rings
    val ringRatios = listOf(0.25f, 0.50f, 0.75f, 1.0f)
    val ringColor = Color(0x44FFA000)
    ringRatios.forEach { ratio ->
        val radiusPx = maxRadiusPx * ratio
        drawCircle(
            color = ringColor,
            radius = radiusPx,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.dp.toPx())
        )

        val ringNm = (maxRangeNm * ratio).toInt()
        val textLayoutResult = textMeasurer.measure(
            text = "$ringNm NM",
            style = TextStyle(
                color = Color(0xDDFFA000),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )
        val textPos = Offset(centerX + 6.dp.toPx(), centerY - radiusPx + 2.dp.toPx())
        if (mapRotationDeg != 0f) {
            rotate(degrees = -mapRotationDeg, pivot = textPos) {
                drawText(textLayoutResult = textLayoutResult, topLeft = textPos)
            }
        } else {
            drawText(textLayoutResult = textLayoutResult, topLeft = textPos)
        }
    }

    // Airport Runway Alignment Crosshair at Center
    val runwayColor = Color(0xFFFFFFFF)
    drawLine(runwayColor, Offset(centerX - 16.dp.toPx(), centerY - 10.dp.toPx()), Offset(centerX + 16.dp.toPx(), centerY + 10.dp.toPx()), 2.5.dp.toPx())
    drawLine(runwayColor, Offset(centerX - 8.dp.toPx(), centerY + 14.dp.toPx()), Offset(centerX + 8.dp.toPx(), centerY - 14.dp.toPx()), 2.dp.toPx())

    // Cardinal Points
    val cardinals = listOf(
        "000° N" to Offset(centerX - 14.dp.toPx(), centerY - maxRadiusPx - 16.dp.toPx()),
        "090° E" to Offset(centerX + maxRadiusPx + 4.dp.toPx(), centerY - 6.dp.toPx()),
        "180° S" to Offset(centerX - 14.dp.toPx(), centerY + maxRadiusPx + 4.dp.toPx()),
        "270° W" to Offset(centerX - maxRadiusPx - 38.dp.toPx(), centerY - 6.dp.toPx())
    )
    cardinals.forEach { (label, pos) ->
        val result = textMeasurer.measure(
            text = label,
            style = TextStyle(color = RadarAmber, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        )
        if (mapRotationDeg != 0f) {
            rotate(degrees = -mapRotationDeg, pivot = pos) {
                drawText(result, topLeft = pos)
            }
        } else {
            drawText(result, topLeft = pos)
        }
    }

    // Topography Attribution Label
    val terrainTag = textMeasurer.measure(
        text = "TOPOGRAPHY · USGS ELEVATION & CONTOURS MSL",
        style = TextStyle(color = Color(0xDDFFA000), fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    )
    val labelPos = Offset(centerX - maxRadiusPx * 0.55f, centerY + maxRadiusPx * 0.88f)
    if (mapRotationDeg != 0f) {
        rotate(degrees = -mapRotationDeg, pivot = labelPos) {
            drawText(terrainTag, topLeft = labelPos)
        }
    } else {
        drawText(terrainTag, topLeft = labelPos)
    }
}

private fun DrawScope.drawAircraftTarget(
    x: Float,
    y: Float,
    aircraft: Aircraft,
    isSelected: Boolean,
    mapLayerType: MapLayerType,
    textMeasurer: TextMeasurer,
    mapRotationDeg: Float = 0f
) {
    val blipColor = when {
        aircraft.isWatchlisted -> RadarAmber
        isSelected -> RadarCyan
        aircraft.isHelicopter -> Color(0xFF00E5FF) // High-visibility Cyan for Helicopter
        mapLayerType == MapLayerType.SATELLITE -> Color(0xFF00F0FF)
        mapLayerType == MapLayerType.TERRAIN -> Color(0xFFE0E0E0)
        else -> RadarGreen
    }

    if (aircraft.isHelicopter) {
        // ROTORCRAFT HELICOPTER BLIP: Fuselage oval + Tail boom + Tail rotor + Main Rotor Crossbar
        rotate(degrees = aircraft.headingDeg, pivot = Offset(x, y)) {
            // Fuselage oval
            drawOval(
                color = blipColor,
                topLeft = Offset(x - 3.5.dp.toPx(), y - 7.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(7.dp.toPx(), 13.dp.toPx())
            )
            // Tail boom extending rearward
            drawLine(
                color = blipColor,
                start = Offset(x, y + 6.dp.toPx()),
                end = Offset(x, y + 14.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
            // Tail rotor cross
            drawLine(
                color = blipColor,
                start = Offset(x - 3.dp.toPx(), y + 14.dp.toPx()),
                end = Offset(x + 3.dp.toPx(), y + 14.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )
            // Main Rotor Disc Blades (wide crossbar perpendicular to fuselage)
            drawLine(
                color = blipColor,
                start = Offset(x - 12.dp.toPx(), y - 1.dp.toPx()),
                end = Offset(x + 12.dp.toPx(), y - 1.dp.toPx()),
                strokeWidth = 2.2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            // Rotor hub center
            drawCircle(
                color = Color.Black,
                radius = 1.8.dp.toPx(),
                center = Offset(x, y - 1.dp.toPx())
            )

            // Forward speed vector
            val vectorLen = (aircraft.velocityKts / 20f).coerceIn(10f, 30f)
            drawLine(
                color = blipColor.copy(alpha = 0.8f),
                start = Offset(x, y - 7.dp.toPx()),
                end = Offset(x, y - 7.dp.toPx() - vectorLen),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    } else {
        // FIXED-WING AIRPLANE BLIP: Directional swept delta arrow
        rotate(degrees = aircraft.headingDeg, pivot = Offset(x, y)) {
            val path = Path().apply {
                moveTo(x, y - 10.dp.toPx())
                lineTo(x + 8.dp.toPx(), y + 8.dp.toPx())
                lineTo(x, y + 4.dp.toPx())
                lineTo(x - 8.dp.toPx(), y + 8.dp.toPx())
                close()
            }
            drawPath(path = path, color = blipColor)

            val vectorLen = (aircraft.velocityKts / 20f).coerceIn(12f, 35f)
            drawLine(
                color = blipColor.copy(alpha = 0.7f),
                start = Offset(x, y - 10.dp.toPx()),
                end = Offset(x, y - 10.dp.toPx() - vectorLen),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }

    if (isSelected || aircraft.isWatchlisted) {
        drawCircle(
            color = if (aircraft.isWatchlisted) RadarAmber else RadarCyan,
            radius = 16.dp.toPx(),
            center = Offset(x, y),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }

    val glyph = if (aircraft.isHelicopter) "🚁" else "✈"
    val label = "$glyph ${aircraft.callsign.ifBlank { aircraft.tailNumber }}\n${aircraft.formattedAltitude}"
    val result = textMeasurer.measure(
        text = label,
        style = TextStyle(
            color = blipColor,
            fontSize = 8.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    )
    if (mapRotationDeg != 0f) {
        rotate(degrees = -mapRotationDeg, pivot = Offset(x, y)) {
            drawText(result, topLeft = Offset(x + 12.dp.toPx(), y - 12.dp.toPx()))
        }
    } else {
        drawText(result, topLeft = Offset(x + 12.dp.toPx(), y - 12.dp.toPx()))
    }
}

private fun calculateBlipPosition(
    centerLat: Double,
    centerLon: Double,
    targetLat: Double,
    targetLon: Double,
    radarCenterX: Float,
    radarCenterY: Float,
    maxRadiusPx: Float,
    maxRangeNm: Double
): Pair<Float, Float> {
    val r = 6371.0
    val dLat = Math.toRadians(targetLat - centerLat)
    val dLon = Math.toRadians(targetLon - centerLon)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(centerLat)) * cos(Math.toRadians(targetLat)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    val distNm = (r * c) * 0.539957

    val phi1 = Math.toRadians(centerLat)
    val phi2 = Math.toRadians(targetLat)
    val deltaLambda = Math.toRadians(targetLon - centerLon)
    val y = sin(deltaLambda) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
    val bearingRad = atan2(y, x)

    val scale = (distNm / maxRangeNm).toFloat().coerceAtMost(1.05f)
    val blipRadiusPx = maxRadiusPx * scale

    val posX = radarCenterX + blipRadiusPx * sin(bearingRad).toFloat()
    val posY = radarCenterY - blipRadiusPx * cos(bearingRad).toFloat()

    return Pair(posX, posY)
}

private fun calculateScreenPosition(
    baseX: Float,
    baseY: Float,
    centerX: Float,
    centerY: Float,
    rotationDeg: Float,
    zoomScale: Float,
    panOffset: Offset
): Offset {
    val rad = Math.toRadians(rotationDeg.toDouble())
    val cosR = cos(rad)
    val sinR = sin(rad)
    val dx = baseX - centerX
    val dy = baseY - centerY
    val rotX = (dx * cosR - dy * sinR).toFloat()
    val rotY = (dx * sinR + dy * cosR).toFloat()
    val screenX = centerX + rotX * zoomScale + panOffset.x
    val screenY = centerY + rotY * zoomScale + panOffset.y
    return Offset(screenX, screenY)
}

private fun DrawScope.drawStormRadarOverlay(
    centerX: Float,
    centerY: Float,
    maxRadiusPx: Float,
    scanRadiusNm: Double,
    radarCenter: RadarCenter,
    stormCells: List<StormCell>,
    textMeasurer: TextMeasurer
) {
    val nmToPx = maxRadiusPx / scanRadiusNm.toFloat()

    stormCells.forEach { cell ->
        val (cellX, cellY) = calculateBlipPosition(
            centerLat = radarCenter.latitude,
            centerLon = radarCenter.longitude,
            targetLat = cell.latitude,
            targetLon = cell.longitude,
            radarCenterX = centerX,
            radarCenterY = centerY,
            maxRadiusPx = maxRadiusPx,
            maxRangeNm = scanRadiusNm
        )

        val cellRadiusPx = (cell.radiusNm * nmToPx).toFloat()

        // Level 1: Light to moderate precipitation (20-35 dBZ)
        drawCircle(
            color = Color(0x3500E676),
            radius = cellRadiusPx,
            center = Offset(cellX, cellY)
        )
        drawCircle(
            color = Color(0x6500E676),
            radius = cellRadiusPx,
            center = Offset(cellX, cellY),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Level 2: Convective rain band (35-45 dBZ)
        val l2Radius = cellRadiusPx * 0.72f
        drawCircle(
            color = Color(0x55FFD600),
            radius = l2Radius,
            center = Offset(cellX, cellY)
        )
        drawCircle(
            color = Color(0x80FFD600),
            radius = l2Radius,
            center = Offset(cellX, cellY),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Level 3: Severe thunderstorm core (45-55 dBZ)
        if (cell.peakDbz >= 45) {
            val l3Radius = cellRadiusPx * 0.46f
            drawCircle(
                color = Color(0x75FF1744),
                radius = l3Radius,
                center = Offset(cellX, cellY)
            )
            drawCircle(
                color = Color(0xB5FF1744),
                radius = l3Radius,
                center = Offset(cellX, cellY),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Level 4: Extreme hail / tornadic core (55-65+ dBZ)
        if (cell.peakDbz >= 55) {
            val l4Radius = cellRadiusPx * 0.24f
            drawCircle(
                color = Color(0x95D500F9),
                radius = l4Radius,
                center = Offset(cellX, cellY)
            )
        }

        // Drift vector indicator
        val headingRad = Math.toRadians(cell.headingDeg.toDouble() - 90.0)
        val arrowLen = (cell.speedKts * 0.9f).coerceIn(16f, 36f)
        val endX = cellX + (arrowLen * cos(headingRad)).toFloat()
        val endY = cellY + (arrowLen * sin(headingRad)).toFloat()
        drawLine(
            color = cell.severity.color.copy(alpha = 0.85f),
            start = Offset(cellX, cellY),
            end = Offset(endX, endY),
            strokeWidth = 1.5.dp.toPx()
        )

        // Storm Cell identification label & echo top
        val lightningIcon = if (cell.hasLightning) "⚡ " else ""
        val cellTag = "$lightningIcon${cell.name}\n${cell.peakDbz} dBZ · FL${cell.topAltitudeFt / 100} · ↗${cell.speedKts}KT"
        val textResult = textMeasurer.measure(
            text = cellTag,
            style = TextStyle(
                color = cell.severity.color,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        )
        drawText(textResult, topLeft = Offset(cellX - cellRadiusPx * 0.6f, cellY + cellRadiusPx * 0.3f))
    }
}

private fun DrawScope.drawStormProximityVectors(
    aircraftList: List<Aircraft>,
    stormCells: List<StormCell>,
    radarCenter: RadarCenter,
    centerX: Float,
    centerY: Float,
    maxRadiusPx: Float,
    scanRadiusNm: Double,
    selectedAircraft: Aircraft?,
    textMeasurer: TextMeasurer
) {
    if (stormCells.isEmpty()) return

    aircraftList.forEach { ac ->
        val isSelected = selectedAircraft?.icao24 == ac.icao24
        if (ac.isNearStorm || isSelected) {
            val (acX, acY) = calculateBlipPosition(
                centerLat = radarCenter.latitude,
                centerLon = radarCenter.longitude,
                targetLat = ac.latitude,
                targetLon = ac.longitude,
                radarCenterX = centerX,
                radarCenterY = centerY,
                maxRadiusPx = maxRadiusPx,
                maxRangeNm = scanRadiusNm
            )

            val nearestCell = stormCells.minByOrNull { it.distanceToCenterNm(ac.latitude, ac.longitude) }
            if (nearestCell != null) {
                val (cellX, cellY) = calculateBlipPosition(
                    centerLat = radarCenter.latitude,
                    centerLon = radarCenter.longitude,
                    targetLat = nearestCell.latitude,
                    targetLon = nearestCell.longitude,
                    radarCenterX = centerX,
                    radarCenterY = centerY,
                    maxRadiusPx = maxRadiusPx,
                    maxRangeNm = scanRadiusNm
                )

                val lineColor = if (ac.isInsideStorm) Color(0xFFFF1744) else Color(0xFFFFD600)

                drawLine(
                    color = lineColor.copy(alpha = if (isSelected) 0.95f else 0.6f),
                    start = Offset(acX, acY),
                    end = Offset(cellX, cellY),
                    strokeWidth = if (isSelected) 2.dp.toPx() else 1.2.dp.toPx()
                )

                val proxText = if (ac.isInsideStorm) "⚠️ IN CORE" else "⚡ ${String.format(Locale.US, "%.1f", ac.nearestStormDistanceNm ?: 0.0)} NM"
                val textLayout = textMeasurer.measure(
                    text = proxText,
                    style = TextStyle(
                        color = lineColor,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                val midX = (acX + cellX) / 2f
                val midY = (acY + cellY) / 2f
                drawText(textLayout, topLeft = Offset(midX, midY))
            }
        }
    }
}

@Composable
private fun DbzBlock(color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 24.dp, height = 8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            color = AvionicsTextDim,
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Compact animated CRT radar screen icon showing green concentric range rings, crosshairs,
 * and an active rotating sweeping radar beam. Used in the map layer switcher to maximize space.
 */
@Composable
fun MiniRadarScreenIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "miniRadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "miniSweepAngle"
    )

    Canvas(
        modifier = modifier.size(16.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 0.8f

        // 1. CRT Radar Scope Background
        drawCircle(
            color = if (isSelected) Color(0xFF00220F) else Color(0xFF051208),
            radius = radius,
            center = center
        )

        // 2. Concentric Range Rings
        drawCircle(
            color = if (isSelected) RadarGreen else RadarGreen.copy(alpha = 0.5f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.2f)
        )
        drawCircle(
            color = if (isSelected) RadarGreen.copy(alpha = 0.6f) else RadarGreen.copy(alpha = 0.3f),
            radius = radius * 0.52f,
            center = center,
            style = Stroke(width = 0.8f)
        )

        // 3. Radar Crosshairs
        val crossAlpha = if (isSelected) 0.5f else 0.25f
        drawLine(
            color = RadarGreen.copy(alpha = crossAlpha),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 0.7f
        )
        drawLine(
            color = RadarGreen.copy(alpha = crossAlpha),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 0.7f
        )

        // 4. Scanning Beam & Trailing Phosphor Glow
        val rad = Math.toRadians(sweepAngle.toDouble())
        val sweepEnd = Offset(
            x = (center.x + radius * Math.cos(rad)).toFloat(),
            y = (center.y + radius * Math.sin(rad)).toFloat()
        )

        // Trailing sweep sector arc
        drawArc(
            color = RadarGreen.copy(alpha = if (isSelected) 0.35f else 0.18f),
            startAngle = sweepAngle - 40f,
            sweepAngle = 40f,
            useCenter = true
        )

        // Sharp leading sweep line
        drawLine(
            color = if (isSelected) RadarGreen else RadarGreen.copy(alpha = 0.7f),
            start = center,
            end = sweepEnd,
            strokeWidth = 1.2f
        )

        // Center transponder dot
        drawCircle(
            color = if (isSelected) RadarGreen else RadarGreen.copy(alpha = 0.8f),
            radius = 1.2f,
            center = center
        )
    }
}


