package com.example.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AircraftRegistry
import com.example.model.Aircraft
import com.example.model.AircraftTypeInfo
import com.example.ui.theme.AvionicsTextDim
import com.example.ui.theme.AvionicsTextPrimary
import com.example.ui.theme.AvionicsTextSecondary
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OledCardBorder
import com.example.ui.theme.OledSurface
import com.example.ui.theme.RadarAmber
import com.example.R
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarGreen
import androidx.compose.ui.res.painterResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun AircraftListView(
    aircraftList: List<Aircraft>,
    telemetryFeed: List<String>,
    isPaused: Boolean,
    feedStatus: String = "SCANNING LIVE 1090MHz...",
    onViewTypeDetails: (AircraftTypeInfo, Aircraft?) -> Unit,
    onToggleWatchlist: (String) -> Unit,
    onAircraftSelected: (Aircraft) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterWatchlistOnly by remember { mutableStateOf(false) }
    var filterProximityOnly by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf("DISTANCE") } // DISTANCE, ALTITUDE, SPEED

    val timeFormatter = remember {
        SimpleDateFormat("HH:mm:ss.SSS 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    val filteredList = aircraftList.filter { ac ->
        val query = searchQuery.trim().lowercase()
        val matchesSearch = query.isEmpty() ||
                ac.tailNumber.lowercase().contains(query) ||
                ac.callsign.lowercase().contains(query) ||
                ac.registeredOperator.lowercase().contains(query) ||
                ac.aircraftTypeName.lowercase().contains(query)

        val matchesWatch = !filterWatchlistOnly || ac.isWatchlisted
        val matchesProx = !filterProximityOnly || ac.distanceNm <= 25.0

        matchesSearch && matchesWatch && matchesProx
    }.let { list ->
        when (sortOrder) {
            "ALTITUDE" -> list.sortedByDescending { it.altitudeFt }
            "SPEED" -> list.sortedByDescending { it.velocityKts }
            else -> list.sortedBy { it.distanceNm }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("aircraft_list_view")
    ) {
        // 1. Live ADS-B Transponder Ticker Bar (Scrolling with timestamps)
        if (telemetryFeed.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF030805))
                    .border(1.dp, Color(0xFF0D2418))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isPaused) RadarAmber else RadarGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = telemetryFeed.firstOrNull() ?: "Scanning 1090MHz ADS-B frequencies...",
                        color = if (isPaused) RadarAmber else RadarGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }

        // 2. Search & Filter Bar
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_aircraft_input"),
                placeholder = {
                    Text(
                        "Search callsign, ICAO hex, or aircraft type...",
                        color = AvionicsTextDim,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = RadarCyan
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = AvionicsTextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RadarGreen,
                    unfocusedBorderColor = OledCardBorder,
                    focusedTextColor = AvionicsTextPrimary,
                    unfocusedTextColor = AvionicsTextPrimary,
                    focusedContainerColor = OledSurface,
                    unfocusedContainerColor = OledSurface
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = !filterWatchlistOnly && !filterProximityOnly,
                        onClick = {
                            filterWatchlistOnly = false
                            filterProximityOnly = false
                        },
                        label = { Text("ALL (${aircraftList.size})", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00381B),
                            selectedLabelColor = RadarGreen
                        )
                    )
                }
                item {
                    val watchCount = aircraftList.count { it.isWatchlisted }
                    FilterChip(
                        selected = filterWatchlistOnly,
                        onClick = { filterWatchlistOnly = !filterWatchlistOnly },
                        label = { Text("🚨 WATCHLIST ($watchCount)", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF331400),
                            selectedLabelColor = RadarAmber
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = filterProximityOnly,
                        onClick = { filterProximityOnly = !filterProximityOnly },
                        label = { Text("PROXIMITY (<25 NM)", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF003544),
                            selectedLabelColor = RadarCyan
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = sortOrder == "SPEED",
                        onClick = { sortOrder = if (sortOrder == "SPEED") "DISTANCE" else "SPEED" },
                        label = { Text("SORT: SPEED", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OledSurface,
                            selectedLabelColor = RadarGreen
                        )
                    )
                }
            }
        }

        // 3. Aircraft Data Cards List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = AvionicsTextDim,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when {
                            isPaused -> "ADS-B Scanner is Paused"
                            aircraftList.isEmpty() -> feedStatus
                            else -> "No aircraft found matching filter"
                        },
                        color = AvionicsTextSecondary,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = when {
                            isPaused -> "Tap 'RESUME' to restart live transponder tracking"
                            aircraftList.isEmpty() -> "Try expanding the radar radius or selecting a busier hub airport"
                            else -> "Try clearing search filters or broadening radar radius"
                        },
                        color = AvionicsTextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.icao24 }) { aircraft ->
                    val typeInfo = AircraftRegistry.getAircraftType(
                        code = aircraft.aircraftTypeCode,
                        descriptionHint = aircraft.aircraftTypeName,
                        isHelicopterHint = aircraft.isHelicopter
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAircraftSelected(aircraft)
                                onViewTypeDetails(typeInfo, aircraft)
                            }
                            .testTag("aircraft_card_${aircraft.tailNumber}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = OledSurface),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (aircraft.isWatchlisted) RadarAmber else if (aircraft.isHelicopter) Color(0xFF0D3B42) else OledCardBorder
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Top Row: Callsign, Tail #, Helicopter vs Plane Icon, Watchlist Tag & Distance
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Differentiated Craft Icon: Helicopter vs Airplane
                                    if (aircraft.isHelicopter) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_helicopter),
                                            contentDescription = "Helicopter",
                                            tint = RadarCyan,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AirplanemodeActive,
                                            contentDescription = "Airplane",
                                            tint = RadarGreen,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = aircraft.displayId,
                                        color = AvionicsTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• #${aircraft.icao24.uppercase()}",
                                        color = if (aircraft.isHelicopter) RadarCyan else RadarGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    if (aircraft.isWatchlisted) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF331400))
                                                .border(1.dp, RadarAmber, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "🚨 WATCHLIST",
                                                color = RadarAmber,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = String.format(Locale.US, "%.1f NM", aircraft.distanceNm),
                                    color = RadarCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Aircraft Model & Squawk
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${typeInfo.modelName} · ${typeInfo.category}",
                                    color = RadarCyan,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(
                                    text = "SQWK ${aircraft.squawk}",
                                    color = AvionicsTextDim,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Storm Weather Cell Proximity Advisory
                            if (aircraft.nearestStormName != null) {
                                Spacer(modifier = Modifier.height(5.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (aircraft.isInsideStorm) Color(0xFF380808) else Color(0xFF221A05))
                                        .border(1.dp, if (aircraft.isInsideStorm) Color(0xFFFF1744) else RadarAmber, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (aircraft.isInsideStorm)
                                                "⚠️ INSIDE ${aircraft.nearestStormName}"
                                            else
                                                "⛈ WX: ${String.format(Locale.US, "%.1f", aircraft.nearestStormDistanceNm ?: 0.0)} NM to ${aircraft.nearestStormName}",
                                            color = if (aircraft.isInsideStorm) Color(0xFFFF1744) else RadarAmber,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "${aircraft.nearestStormDbz} dBZ",
                                            color = if (aircraft.isInsideStorm) Color(0xFFFF1744) else RadarAmber,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Telemetry Metrics Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TelemetryMetric(label = "ALTITUDE", value = aircraft.formattedAltitude)
                                TelemetryMetric(label = "SPD", value = "${aircraft.velocityKts} kts")
                                TelemetryMetric(label = "TRACK", value = "${aircraft.headingDeg.toInt()}°")
                                TelemetryMetric(label = "SQUAWK", value = aircraft.squawk)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom Row: Clickable Aircraft Type Chip + Timestamp + Watchlist Action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Aircraft/Helicopter Type Chip: TAPPING POPS UP PICTURE OF CRAFT & PUBLIC REGISTRY DETAILS
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (aircraft.isHelicopter) Color(0xFF041E26) else Color(0xFF071B11))
                                        .border(1.dp, if (aircraft.isHelicopter) RadarCyan else RadarGreen, RoundedCornerShape(6.dp))
                                        .clickable { onViewTypeDetails(typeInfo, aircraft) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("type_chip_${aircraft.tailNumber}")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (aircraft.isHelicopter) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_helicopter),
                                                contentDescription = "View Helicopter Photo & Registry",
                                                tint = RadarCyan,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AirplanemodeActive,
                                                contentDescription = "View Aircraft Photo & Registry",
                                                tint = RadarGreen,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${if (aircraft.isHelicopter) "🚁" else "✈"} ${aircraft.aircraftTypeName} (${aircraft.aircraftTypeCode}) 📷",
                                            color = if (aircraft.isHelicopter) RadarCyan else RadarGreen,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Timestamp
                                    Text(
                                        text = timeFormatter.format(Date(aircraft.lastContactTimestamp)),
                                        color = AvionicsTextDim,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Quick Watchlist Toggle Icon
                                    IconButton(
                                        onClick = { onToggleWatchlist(aircraft.tailNumber) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (aircraft.isWatchlisted) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = if (aircraft.isWatchlisted) "Remove from Watchlist" else "Add to Watchlist",
                                            tint = if (aircraft.isWatchlisted) RadarAmber else AvionicsTextDim,
                                            modifier = Modifier.size(18.dp)
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

@Composable
private fun TelemetryMetric(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = AvionicsTextDim,
            fontSize = 8.5.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = AvionicsTextPrimary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
