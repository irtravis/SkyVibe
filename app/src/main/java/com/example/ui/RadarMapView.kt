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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AircraftRegistry
import com.example.model.Aircraft
import com.example.model.AircraftTypeInfo
import com.example.model.MapLayerType
import com.example.model.RadarCenter
import com.example.model.StormCell
import com.example.model.StormSeverity
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
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun RadarMapView(
    aircraftList: List<Aircraft>,
    radarCenter: RadarCenter,
    scanRadiusNm: Double,
    isPaused: Boolean,
    mapLayerType: MapLayerType = MapLayerType.RADAR,
    stormCells: List<StormCell> = emptyList(),
    isStormRadarEnabled: Boolean = true,
    onMapLayerChanged: (MapLayerType) -> Unit = {},
    onToggleStormRadar: () -> Unit = {},
    onOpenLocationPicker: () -> Unit = {},
    onSnapGps: () -> Unit = {},
    onAircraftSelected: (Aircraft) -> Unit,
    onViewTypeDetails: (AircraftTypeInfo) -> Unit,
    onToggleWatchlist: (String) -> Unit
) {
    var selectedAircraft by remember { mutableStateOf<Aircraft?>(null) }
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
        val maxRadiusPx = minOf(centerX, centerY) * 0.88f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(aircraftList, scanRadiusNm) {
                    detectTapGestures { tapOffset ->
                        var closest: Aircraft? = null
                        var minDistancePx = 50f

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
                            val dist = sqrt((tapOffset.x - blipX) * (tapOffset.x - blipX) + (tapOffset.y - blipY) * (tapOffset.y - blipY))
                            if (dist < minDistancePx) {
                                minDistancePx = dist
                                closest = ac
                            }
                        }

                        if (closest != null) {
                            selectedAircraft = closest
                            onAircraftSelected(closest!!)
                        } else {
                            selectedAircraft = null
                        }
                    }
                }
        ) {
            // 1. Draw Background Layer (Radar Grid / Satellite Nocturnal Earth / Terrain Sectional)
            when (mapLayerType) {
                MapLayerType.RADAR -> {
                    drawRadarGrid(
                        centerX = centerX,
                        centerY = centerY,
                        maxRadiusPx = maxRadiusPx,
                        maxRangeNm = scanRadiusNm,
                        textMeasurer = textMeasurer
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
                        textMeasurer = textMeasurer
                    )
                }
                MapLayerType.TERRAIN -> {
                    drawTerrainLayer(
                        centerX = centerX,
                        centerY = centerY,
                        maxRadiusPx = maxRadiusPx,
                        maxRangeNm = scanRadiusNm,
                        textMeasurer = textMeasurer
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
                    selectedAircraft = selectedAircraft,
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
                    textMeasurer = textMeasurer
                )
            }
        }

        // Top Status HUD with Location Selector, GPS Snap, Storm Toggle, and Map Layer Switcher
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
                        text = if (isStormRadarEnabled) "⛈ WX:ON" else "⛈ WX:OFF",
                        color = if (isStormRadarEnabled) Color(0xFFEA80FC) else AvionicsTextDim,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Map Layer Switcher Pill: [RADAR | SATELLITE | TERRAIN]
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(OledSurface)
                        .border(1.dp, OledCardBorder, RoundedCornerShape(20.dp))
                        .padding(2.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
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
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                    .testTag("layer_${layer.name.lowercase()}")
                            ) {
                                Text(
                                    text = layer.displayName,
                                    color = if (isSelected) {
                                        when (layer) {
                                            MapLayerType.RADAR -> RadarGreen
                                            MapLayerType.SATELLITE -> RadarCyan
                                            MapLayerType.TERRAIN -> RadarAmber
                                        }
                                    } else AvionicsTextDim,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // NEXRAD Doppler Weather Reflectivity Scale Legend
        if (isStormRadarEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = if (selectedAircraft != null) 200.dp else 12.dp)
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

        // Target Inspection Bottom Card (when an aircraft is tapped)
        selectedAircraft?.let { target ->
            val typeInfo = AircraftRegistry.getAircraftType(target.aircraftTypeCode)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, if (target.isWatchlisted) RadarAmber else RadarGreen, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = OledBlack)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = target.displayId,
                                    color = AvionicsTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (target.isWatchlisted) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF331400))
                                            .border(1.dp, RadarAmber, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "🚨 WATCHLIST",
                                            color = RadarAmber,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = target.registeredOperator,
                                color = RadarCyan,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = String.format(Locale.US, "%.1f NM", target.distanceNm),
                            color = RadarGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Storm cell proximity telemetry warning if active
                    if (isStormRadarEnabled && target.nearestStormName != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (target.isInsideStorm) Color(0xFF3B0B0B) else Color(0xFF261D00))
                                .border(1.dp, if (target.isInsideStorm) Color(0xFFFF1744) else RadarAmber, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (target.isInsideStorm) "⚠️ IN STORM CELL: " else "⚡ STORM PROX: ",
                                        color = if (target.isInsideStorm) Color(0xFFFF1744) else RadarAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = target.nearestStormName ?: "",
                                        color = AvionicsTextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = if (target.isInsideStorm) "HAIL CORE" else "${String.format(Locale.US, "%.1f", target.nearestStormDistanceNm ?: 0.0)} NM (${target.nearestStormDbz} dBZ)",
                                    color = if (target.isInsideStorm) Color(0xFFFF1744) else RadarAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ALTITUDE", color = AvionicsTextDim, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            Text(target.formattedAltitude, color = AvionicsTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("GROUND SPEED", color = AvionicsTextDim, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            Text("${target.velocityKts} kts", color = AvionicsTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("TRACK", color = AvionicsTextDim, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            Text("${target.headingDeg.toInt()}°", color = AvionicsTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("SQUAWK", color = AvionicsTextDim, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            Text(target.squawk, color = RadarCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onViewTypeDetails(typeInfo) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OledSurface,
                                contentColor = RadarCyan
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RadarCyan)
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "✈ ${target.aircraftTypeCode} PHOTO/SPECS",
                                fontSize = 10.5.sp,
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
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (target.isWatchlisted) "UNWATCH" else "WATCH",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
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
    textMeasurer: TextMeasurer
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
        drawText(
            textLayoutResult = textLayoutResult,
            topLeft = Offset(centerX + 6.dp.toPx(), centerY - radiusPx + 2.dp.toPx())
        )
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
        drawText(result, topLeft = pos)
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

// 2. SATELLITE LAYER DRAWING (Nocturnal Earth Orbit View)
private fun DrawScope.drawSatelliteLayer(
    centerX: Float,
    centerY: Float,
    maxRadiusPx: Float,
    maxRangeNm: Double,
    textMeasurer: TextMeasurer
) {
    // Deep nocturnal oceanic gradient
    val oceanBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF05131E), Color(0xFF02070D), Color(0xFF000306)),
        center = Offset(centerX, centerY),
        radius = maxRadiusPx * 1.2f
    )
    drawCircle(brush = oceanBrush, radius = maxRadiusPx, center = Offset(centerX, centerY))

    // Atmospheric limb ring
    drawCircle(
        color = Color(0x3300E5FF),
        radius = maxRadiusPx,
        center = Offset(centerX, centerY),
        style = Stroke(width = 2.dp.toPx())
    )

    // Orbital latitude & longitude grid
    val gridColor = Color(0x1A00E5FF)
    listOf(0.33f, 0.66f, 1.0f).forEach { r ->
        drawCircle(
            color = gridColor,
            radius = maxRadiusPx * r,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.dp.toPx())
        )
    }

    // Simulated nocturnal city cluster lights & coastal landmass silhouettes
    val cityGlowColor = Color(0x55FFD580)
    val clusterOffsets = listOf(
        Offset(centerX - maxRadiusPx * 0.4f, centerY - maxRadiusPx * 0.3f),
        Offset(centerX + maxRadiusPx * 0.35f, centerY + maxRadiusPx * 0.25f),
        Offset(centerX - maxRadiusPx * 0.15f, centerY + maxRadiusPx * 0.5f),
        Offset(centerX + maxRadiusPx * 0.5f, centerY - maxRadiusPx * 0.45f)
    )

    clusterOffsets.forEach { pos ->
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(cityGlowColor, Color(0x11FFD580), Color.Transparent),
                center = pos,
                radius = 45.dp.toPx()
            ),
            radius = 45.dp.toPx(),
            center = pos
        )
    }

    // Satellite Telemetry Label
    val satResult = textMeasurer.measure(
        text = "ORBITAL SATELLITE COMPOSITE · IR NIGHT SENSOR",
        style = TextStyle(color = Color(0x8800E5FF), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
    )
    drawText(satResult, topLeft = Offset(centerX - maxRadiusPx * 0.65f, centerY + maxRadiusPx * 0.85f))
}

// 3. TERRAIN LAYER DRAWING (Aeronautical Sectional Topography)
private fun DrawScope.drawTerrainLayer(
    centerX: Float,
    centerY: Float,
    maxRadiusPx: Float,
    maxRangeNm: Double,
    textMeasurer: TextMeasurer
) {
    // Topographic elevation contour bands (hypsometric shading)
    val terrainColors = listOf(
        Color(0xFF0F1E15) to 1.0f,
        Color(0xFF1B2A1E) to 0.75f,
        Color(0xFF2C3520) to 0.50f,
        Color(0xFF3E3622) to 0.28f,
        Color(0xFF4A3828) to 0.14f
    )

    terrainColors.forEach { (color, ratio) ->
        drawCircle(
            color = color,
            radius = maxRadiusPx * ratio,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color(0x33FFA000),
            radius = maxRadiusPx * ratio,
            center = Offset(centerX, centerY),
            style = Stroke(width = 0.8.dp.toPx())
        )
    }

    // Airport Runway Intersect at Radar Origin (Center)
    val runwayColor = Color(0xFFFFFFFF)
    drawLine(runwayColor, Offset(centerX - 16.dp.toPx(), centerY - 10.dp.toPx()), Offset(centerX + 16.dp.toPx(), centerY + 10.dp.toPx()), 2.5.dp.toPx())
    drawLine(runwayColor, Offset(centerX - 8.dp.toPx(), centerY + 14.dp.toPx()), Offset(centerX + 8.dp.toPx(), centerY - 14.dp.toPx()), 2.dp.toPx())

    // Elevation Spot Heights & Sectional Label
    val spotHeights = listOf(
        "^ 4,210 ft" to Offset(centerX - maxRadiusPx * 0.45f, centerY - maxRadiusPx * 0.2f),
        "^ 6,684 ft" to Offset(centerX + maxRadiusPx * 0.38f, centerY - maxRadiusPx * 0.35f),
        "^ 2,150 ft" to Offset(centerX - maxRadiusPx * 0.25f, centerY + maxRadiusPx * 0.4f)
    )

    spotHeights.forEach { (label, pos) ->
        val res = textMeasurer.measure(
            text = label,
            style = TextStyle(color = RadarAmber, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        )
        drawText(res, topLeft = pos)
    }

    val terrainTag = textMeasurer.measure(
        text = "SECTIONAL HYPSOMETRIC ELEVATION · CONTOURS MSL",
        style = TextStyle(color = Color(0x88FFA000), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
    )
    drawText(terrainTag, topLeft = Offset(centerX - maxRadiusPx * 0.65f, centerY + maxRadiusPx * 0.85f))
}

private fun DrawScope.drawAircraftTarget(
    x: Float,
    y: Float,
    aircraft: Aircraft,
    isSelected: Boolean,
    mapLayerType: MapLayerType,
    textMeasurer: TextMeasurer
) {
    val blipColor = when {
        aircraft.isWatchlisted -> RadarAmber
        isSelected -> RadarCyan
        mapLayerType == MapLayerType.SATELLITE -> Color(0xFF00F0FF)
        mapLayerType == MapLayerType.TERRAIN -> Color(0xFFE0E0E0)
        else -> RadarGreen
    }

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

    if (isSelected || aircraft.isWatchlisted) {
        drawCircle(
            color = if (aircraft.isWatchlisted) RadarAmber else RadarCyan,
            radius = 16.dp.toPx(),
            center = Offset(x, y),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }

    val label = "${aircraft.callsign.ifBlank { aircraft.tailNumber }}\n${aircraft.formattedAltitude}"
    val result = textMeasurer.measure(
        text = label,
        style = TextStyle(
            color = blipColor,
            fontSize = 8.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    )
    drawText(result, topLeft = Offset(x + 12.dp.toPx(), y - 12.dp.toPx()))
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

