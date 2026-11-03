package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FlightTrackPointEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun FlightHistoryView(
    tracks: List<FlightTrackPointEntity>,
    trackedTails: List<String>,
    selectedTail: String?,
    onSelectTail: (String?) -> Unit,
    onClearTracks: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val timeFormatter = remember {
        SimpleDateFormat("HH:mm:ss 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    val dateFormatter = remember {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    // Filter points by chosen tail number
    val effectiveTail = selectedTail ?: trackedTails.firstOrNull() ?: "N856UA"
    val tailPoints = tracks.filter { it.tailNumber.equals(effectiveTail, ignoreCase = true) }
        .sortedBy { it.timestamp }

    // Summary statistics
    val maxAlt = tailPoints.maxOfOrNull { it.altitudeFt } ?: 0
    val maxSpd = tailPoints.maxOfOrNull { it.velocityKts } ?: 0
    val avgSpd = if (tailPoints.isNotEmpty()) tailPoints.map { it.velocityKts }.average().toInt() else 0
    val firstPoint = tailPoints.firstOrNull()
    val lastPoint = tailPoints.lastOrNull()
    val operatorName = lastPoint?.registeredOperator ?: firstPoint?.registeredOperator ?: "Unknown Operator"
    val craftType = lastPoint?.aircraftType ?: firstPoint?.aircraftType ?: "Jet Transport"
    val callsign = lastPoint?.callsign ?: firstPoint?.callsign ?: effectiveTail

    var scrubIndex by remember(effectiveTail, tailPoints.size) {
        mutableIntStateOf((tailPoints.size - 1).coerceAtLeast(0))
    }
    val activeScrubPoint = tailPoints.getOrNull(scrubIndex) ?: lastPoint

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("flight_history_view"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Watched Aircraft Selector Chips
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = RadarCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RECORDED WATCHLIST FLIGHT TRACKS",
                            color = RadarCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    if (tracks.isNotEmpty()) {
                        IconButton(onClick = onClearTracks, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Flight Tracks",
                                tint = AvionicsTextDim,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tails = if (trackedTails.isNotEmpty()) trackedTails else listOf("N856UA")
                    items(tails) { tail ->
                        val isSelected = tail.equals(effectiveTail, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectTail(tail) },
                            label = {
                                Text(
                                    text = "✈ $tail",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00381B),
                                selectedLabelColor = RadarGreen,
                                containerColor = OledSurface,
                                labelColor = AvionicsTextSecondary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) RadarGreen else OledCardBorder
                            )
                        )
                    }
                }
            }
        }

        if (tailPoints.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(OledSurface)
                        .border(1.dp, OledCardBorder, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Route, contentDescription = null, tint = AvionicsTextDim, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No flight breadcrumbs recorded for $effectiveTail yet.",
                            color = AvionicsTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Flight paths are automatically recorded whenever watchlisted aircraft enter radar range.",
                            color = AvionicsTextDim,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        } else {
            // 2. Flight Session Profile Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RadarCyan)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$effectiveTail · $callsign",
                                        color = AvionicsTextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF003544))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "TRACKED ROUTE",
                                            color = RadarCyan,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "$operatorName · $craftType",
                                    color = RadarGreen,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${tailPoints.size} PTS",
                                    color = RadarCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (firstPoint != null) dateFormatter.format(Date(firstPoint.timestamp)) else "",
                                    color = AvionicsTextDim,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Metric Stat Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HistoryStatPill(
                                label = "PEAK CEILING",
                                value = if (maxAlt >= 18000) "FL${maxAlt / 100}" else "$maxAlt ft",
                                sub = "$maxAlt ft",
                                color = RadarGreen,
                                modifier = Modifier.weight(1f)
                            )
                            HistoryStatPill(
                                label = "MAX AIRSPEED",
                                value = "$maxSpd kts",
                                sub = "~${(maxSpd * 1.15).toInt()} mph",
                                color = RadarCyan,
                                modifier = Modifier.weight(1f)
                            )
                            HistoryStatPill(
                                label = "AVG SPEED",
                                value = "$avgSpd kts",
                                sub = "Cruise phase",
                                color = RadarAmber,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. Geographic Route Breadcrumb Map
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = OledBlack),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OledCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GEOGRAPHIC ROUTE PATH (BREADCRUMB TRACK)",
                                color = RadarCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "ENTRY > EXIT",
                                color = AvionicsTextDim,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Flight Path Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF03070A))
                                .border(1.dp, Color(0xFF0F1E16), RoundedCornerShape(10.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawFlightRouteMap(
                                    points = tailPoints,
                                    scrubbedIndex = scrubIndex,
                                    textMeasurer = textMeasurer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Timeline Scrubber Slider
                        if (tailPoints.size > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SCRUB ROUTE TIMELINE:",
                                    color = AvionicsTextDim,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                activeScrubPoint?.let { pt ->
                                    Text(
                                        text = "${timeFormatter.format(Date(pt.timestamp))} · ${pt.altitudeFt} ft · ${pt.velocityKts} kts",
                                        color = RadarGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Slider(
                                value = scrubIndex.toFloat(),
                                onValueChange = { scrubIndex = it.toInt().coerceIn(0, tailPoints.size - 1) },
                                valueRange = 0f..(tailPoints.size - 1).toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = RadarGreen,
                                    activeTrackColor = RadarGreen,
                                    inactiveTrackColor = OledCardBorder
                                )
                            )
                        }
                    }
                }
            }

            // 4. Altitude Profile Elevation Chart (FT / FL)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = OledBlack),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OledCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.VerticalAlignTop, contentDescription = null, tint = RadarGreen, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ALTITUDE ELEVATION PROFILE",
                                    color = RadarGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "CEILING: $maxAlt FT",
                                color = RadarGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF030805))
                                .border(1.dp, Color(0xFF0F1E16), RoundedCornerShape(8.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawAltitudeChart(
                                    points = tailPoints,
                                    scrubbedIndex = scrubIndex,
                                    maxAlt = maxAlt,
                                    textMeasurer = textMeasurer
                                )
                            }
                        }
                    }
                }
            }

            // 5. Airspeed Profile Chart (KTS)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = OledBlack),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OledCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = RadarCyan, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AIRSPEED PROFILE (GROUND SPEED)",
                                    color = RadarCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "TOP: $maxSpd KTS",
                                color = RadarCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF03070A))
                                .border(1.dp, Color(0xFF0F1E16), RoundedCornerShape(8.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawSpeedChart(
                                    points = tailPoints,
                                    scrubbedIndex = scrubIndex,
                                    maxSpd = maxSpd,
                                    textMeasurer = textMeasurer
                                )
                            }
                        }
                    }
                }
            }

            // 6. Time-stamped Route Waypoint Log Table
            item {
                Text(
                    text = "RECORDED ROUTE WAYPOINTS (${tailPoints.size})",
                    color = AvionicsTextDim,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            items(tailPoints.reversed()) { pt ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OledCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (pt.verticalRateFpm > 100) RadarGreen else if (pt.verticalRateFpm < -100) RadarAmber else RadarCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = timeFormatter.format(Date(pt.timestamp)),
                                    color = AvionicsTextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "LAT: %.4f°  LON: %.4f°", pt.latitude, pt.longitude),
                                color = AvionicsTextDim,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (pt.altitudeFt >= 18000) "FL${pt.altitudeFt / 100} (${pt.altitudeFt} ft)" else "${pt.altitudeFt} ft",
                                color = RadarGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${pt.velocityKts} kts · ${pt.headingDeg.toInt()}° · ${if (pt.verticalRateFpm > 0) "+${pt.verticalRateFpm} fpm" else "${pt.verticalRateFpm} fpm"}",
                                color = AvionicsTextSecondary,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryStatPill(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(OledBlack)
            .border(1.dp, OledCardBorder, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(label, color = AvionicsTextDim, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(sub, color = AvionicsTextSecondary, fontSize = 9.sp)
        }
    }
}

// Draw Breadcrumb Geographic Map
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFlightRouteMap(
    points: List<FlightTrackPointEntity>,
    scrubbedIndex: Int,
    textMeasurer: TextMeasurer
) {
    if (points.isEmpty()) return

    val padding = 28.dp.toPx()
    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }

    val latSpan = (maxLat - minLat).coerceAtLeast(0.02)
    val lonSpan = (maxLon - minLon).coerceAtLeast(0.02)

    val mapWidth = size.width - padding * 2
    val mapHeight = size.height - padding * 2

    // Background Lat/Lon grid
    val gridColor = Color(0x1800E5FF)
    drawLine(gridColor, Offset(padding, size.height / 2), Offset(size.width - padding, size.height / 2), 1f)
    drawLine(gridColor, Offset(size.width / 2, padding), Offset(size.width / 2, size.height - padding), 1f)

    // Convert lat/lon to Canvas X/Y coordinates
    fun toCanvasCoords(lat: Double, lon: Double): Offset {
        val x = padding + ((lon - minLon) / lonSpan * mapWidth).toFloat()
        val y = size.height - padding - ((lat - minLat) / latSpan * mapHeight).toFloat()
        return Offset(x, y)
    }

    // Draw connecting route trajectory path
    val routePath = Path()
    points.forEachIndexed { i, pt ->
        val pos = toCanvasCoords(pt.latitude, pt.longitude)
        if (i == 0) routePath.moveTo(pos.x, pos.y)
        else routePath.lineTo(pos.x, pos.y)
    }

    drawPath(
        path = routePath,
        color = Color(0x8800E5FF),
        style = Stroke(width = 2.5.dp.toPx())
    )

    // Draw Waypoint dots
    points.forEachIndexed { i, pt ->
        val pos = toCanvasCoords(pt.latitude, pt.longitude)
        val isFirst = i == 0
        val isLast = i == points.size - 1
        val isScrubbed = i == scrubbedIndex

        val dotColor = when {
            isScrubbed -> Color.White
            isLast -> RadarGreen
            isFirst -> RadarCyan
            pt.altitudeFt > 30000 -> Color(0xFF00E5FF)
            else -> Color(0xFF00E676)
        }

        drawCircle(
            color = dotColor,
            radius = if (isScrubbed) 6.dp.toPx() else if (isFirst || isLast) 4.5.dp.toPx() else 2.5.dp.toPx(),
            center = pos
        )

        if (isFirst) {
            val res = textMeasurer.measure(
                text = "DEP",
                style = TextStyle(color = RadarCyan, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            )
            drawText(res, topLeft = Offset(pos.x + 8f, pos.y - 12f))
        }

        if (isLast) {
            val res = textMeasurer.measure(
                text = "CUR",
                style = TextStyle(color = RadarGreen, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            )
            drawText(res, topLeft = Offset(pos.x + 8f, pos.y - 12f))
        }
    }
}

// Draw Altitude Profile Chart
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAltitudeChart(
    points: List<FlightTrackPointEntity>,
    scrubbedIndex: Int,
    maxAlt: Int,
    textMeasurer: TextMeasurer
) {
    if (points.isEmpty()) return

    val padLeft = 40.dp.toPx()
    val padRight = 14.dp.toPx()
    val padTop = 14.dp.toPx()
    val padBottom = 20.dp.toPx()

    val chartWidth = size.width - padLeft - padRight
    val chartHeight = size.height - padTop - padBottom
    val ceil = (maxAlt * 1.15f).coerceAtLeast(10000f)

    // Grid lines for FL100, FL200, FL300, FL400
    val flSteps = listOf(10000, 20000, 30000, 40000).filter { it <= ceil }
    flSteps.forEach { fl ->
        val y = padTop + chartHeight * (1f - fl / ceil)
        drawLine(Color(0x2200E676), Offset(padLeft, y), Offset(size.width - padRight, y), 1f)
        val res = textMeasurer.measure(
            text = "FL${fl / 100}",
            style = TextStyle(color = Color(0x6600E676), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
        )
        drawText(res, topLeft = Offset(4.dp.toPx(), y - 8f))
    }

    // Altitude Area & Line
    val altPath = Path()
    val altArea = Path()

    points.forEachIndexed { i, pt ->
        val x = padLeft + (i.toFloat() / (points.size - 1).coerceAtLeast(1)) * chartWidth
        val y = padTop + chartHeight * (1f - (pt.altitudeFt / ceil))

        if (i == 0) {
            altPath.moveTo(x, y)
            altArea.moveTo(x, padTop + chartHeight)
            altArea.lineTo(x, y)
        } else {
            altPath.lineTo(x, y)
            altArea.lineTo(x, y)
        }

        if (i == points.size - 1) {
            altArea.lineTo(x, padTop + chartHeight)
            altArea.close()
        }
    }

    // Fill under elevation curve
    drawPath(
        path = altArea,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0x3300E676), Color(0x0500E676)),
            startY = padTop,
            endY = padTop + chartHeight
        )
    )

    // Elevation stroke line
    drawPath(
        path = altPath,
        color = RadarGreen,
        style = Stroke(width = 2.dp.toPx())
    )

    // Scrubbed indicator line
    val scrubX = padLeft + (scrubbedIndex.toFloat() / (points.size - 1).coerceAtLeast(1)) * chartWidth
    drawLine(Color.White, Offset(scrubX, padTop), Offset(scrubX, padTop + chartHeight), 1.5f)
}

// Draw Ground Speed Chart
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpeedChart(
    points: List<FlightTrackPointEntity>,
    scrubbedIndex: Int,
    maxSpd: Int,
    textMeasurer: TextMeasurer
) {
    if (points.isEmpty()) return

    val padLeft = 40.dp.toPx()
    val padRight = 14.dp.toPx()
    val padTop = 14.dp.toPx()
    val padBottom = 16.dp.toPx()

    val chartWidth = size.width - padLeft - padRight
    val chartHeight = size.height - padTop - padBottom
    val topSpd = (maxSpd * 1.15f).coerceAtLeast(200f)

    // Grid lines for 200kts, 400kts, 600kts
    listOf(200, 400, 600).filter { it <= topSpd }.forEach { spd ->
        val y = padTop + chartHeight * (1f - spd / topSpd)
        drawLine(Color(0x2200E5FF), Offset(padLeft, y), Offset(size.width - padRight, y), 1f)
        val res = textMeasurer.measure(
            text = "${spd}k",
            style = TextStyle(color = Color(0x6600E5FF), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
        )
        drawText(res, topLeft = Offset(4.dp.toPx(), y - 8f))
    }

    val spdPath = Path()
    points.forEachIndexed { i, pt ->
        val x = padLeft + (i.toFloat() / (points.size - 1).coerceAtLeast(1)) * chartWidth
        val y = padTop + chartHeight * (1f - (pt.velocityKts / topSpd))

        if (i == 0) spdPath.moveTo(x, y)
        else spdPath.lineTo(x, y)
    }

    drawPath(
        path = spdPath,
        color = RadarCyan,
        style = Stroke(width = 2.dp.toPx())
    )

    val scrubX = padLeft + (scrubbedIndex.toFloat() / (points.size - 1).coerceAtLeast(1)) * chartWidth
    drawLine(Color.White, Offset(scrubX, padTop), Offset(scrubX, padTop + chartHeight), 1.5f)
}
