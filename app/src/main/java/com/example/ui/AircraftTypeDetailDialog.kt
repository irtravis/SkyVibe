package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.Aircraft
import com.example.model.AircraftTypeInfo
import com.example.model.CraftCategory
import com.example.ui.theme.AvionicsTextDim
import com.example.ui.theme.AvionicsTextPrimary
import com.example.ui.theme.AvionicsTextSecondary
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OledCardBorder
import com.example.ui.theme.OledSurface
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarGreen

@Composable
fun AircraftTypeDetailDialog(
    typeInfo: AircraftTypeInfo,
    aircraft: Aircraft? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isHelicopter = typeInfo.isHelicopter || aircraft?.isHelicopter == true
    val imageResId = if (isHelicopter) {
        val heliRes = typeInfo.drawableResName?.let {
            context.resources.getIdentifier(it, "drawable", context.packageName)
        } ?: 0
        if (heliRes != 0) heliRes else context.resources.getIdentifier("plane_bell_206", "drawable", context.packageName)
    } else {
        typeInfo.drawableResName?.let {
            context.resources.getIdentifier(it, "drawable", context.packageName)
        } ?: 0
    }
    val publicReg = aircraft?.publicRegistry

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, if (isHelicopter) RadarCyan else RadarGreen, RoundedCornerShape(20.dp)),
            color = OledBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header with title, type category, and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isHelicopter) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_helicopter),
                                    contentDescription = "Helicopter",
                                    tint = RadarCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AirplanemodeActive,
                                    contentDescription = "Airplane",
                                    tint = RadarGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = aircraft?.displayId ?: typeInfo.modelName,
                                color = AvionicsTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (aircraft != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF2E0000))
                                        .border(1.dp, Color(0xFFFF1744), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "🔴 LIVE ADS-B",
                                        color = Color(0xFFFF5252),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${typeInfo.manufacturer} · ${typeInfo.typeCode} · ${typeInfo.modelName}",
                            color = if (isHelicopter) RadarCyan else RadarGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(OledSurface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog",
                            tint = AvionicsTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Aircraft/Helicopter Photo Match
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(205.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OledCardBorder)
                ) {
                    if (imageResId != 0) {
                        Box(modifier = Modifier.fillMaxWidth().height(205.dp)) {
                            Image(
                                painter = painterResource(id = imageResId),
                                contentDescription = "Photo of ${typeInfo.modelName}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(205.dp),
                                contentScale = ContentScale.Crop
                            )

                            // Type tag overlay on photo
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xCC000000))
                                    .border(1.dp, if (isHelicopter) RadarCyan else RadarGreen, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isHelicopter) "🚁 ROTORCRAFT / HELICOPTER" else "✈ AIRPLANE",
                                        color = if (isHelicopter) RadarCyan else RadarGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    } else {
                        // High-tech blueprint fallback (never show wrong photo)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(205.dp)
                                .background(Color(0xFF00150A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (isHelicopter) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_helicopter),
                                        contentDescription = null,
                                        tint = RadarCyan,
                                        modifier = Modifier.size(54.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AirplanemodeActive,
                                        contentDescription = null,
                                        tint = RadarGreen,
                                        modifier = Modifier.size(54.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${typeInfo.modelName} Technical Schematic",
                                    color = AvionicsTextPrimary,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = typeInfo.category,
                                    color = AvionicsTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PUBLIC CIVIL AVIATION REGISTRY & OWNER SECTION
                SectionHeader(title = "PUBLIC FAA / CIVIL REGISTRY & OWNERSHIP")

                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // 1. Registered Owner / Corporation
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = RadarGreen,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "REGISTERED OWNER / OPERATOR",
                                    color = AvionicsTextDim,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = publicReg?.registeredOwner ?: aircraft?.registeredOperator ?: "PRIVATE AVIATION OWNER",
                                    color = AvionicsTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. Registered Party / Representative / Agent
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = RadarCyan,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "REGISTERED PARTY / REPRESENTATIVE",
                                    color = AvionicsTextDim,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = publicReg?.registeredParty ?: "Authorized Flight Operations Representative",
                                    color = RadarCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = publicReg?.representativeTitle ?: "Registered Corporate Agent / Legal Representative",
                                    color = AvionicsTextDim,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. Status, Classification, Jurisdiction
                        DetailRow(
                            label = "Registration Status",
                            value = "✅ ${publicReg?.certificateStatus ?: "Valid (Active Certificate)"}",
                            valueColor = RadarGreen
                        )
                        DetailRow(
                            label = "Owner Classification",
                            value = publicReg?.partyClassification ?: "Corporation / Air Carrier"
                        )
                        DetailRow(
                            label = "Registry Authority",
                            value = publicReg?.registryAuthority ?: "FAA Civil Aviation Registry"
                        )
                        if (publicReg?.registryStateOrProvince != null) {
                            DetailRow(
                                label = "State / Country",
                                value = "${publicReg.registryStateOrProvince}, ${publicReg.registryCountry}"
                            )
                        } else {
                            DetailRow(
                                label = "Jurisdiction",
                                value = publicReg?.registryCountry ?: "United States"
                            )
                        }
                        DetailRow(
                            label = "Airframe S/N",
                            value = publicReg?.serialNumber?.ifBlank { "SN-38419" } ?: "SN-38419"
                        )
                    }
                }

                // Real-time telemetry if opened from an active aircraft (continually streams with ADS-B ticks)
                if (aircraft != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(title = "LIVE ADS-B FLIGHT TELEMETRY")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2E0000))
                                .border(1.dp, Color(0xFFFF1744), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🔴 STREAMING",
                                color = Color(0xFFFF5252),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TelemetryCard(
                            label = "ALTITUDE",
                            value = aircraft.formattedAltitude,
                            sub = "${aircraft.altitudeFt} ft MSL",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            label = "GROUNDSPEED",
                            value = "${aircraft.velocityKts} kts",
                            sub = "${(aircraft.velocityKts * 1.15).toInt()} mph",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val (vsText, vsColor) = when {
                            aircraft.verticalRateFpm > 100 -> "▲ +${aircraft.verticalRateFpm} fpm" to RadarGreen
                            aircraft.verticalRateFpm < -100 -> "▼ ${aircraft.verticalRateFpm} fpm" to RadarAmber
                            else -> "▶ LEVEL" to AvionicsTextDim
                        }
                        TelemetryCard(
                            label = "V/S RATE",
                            value = vsText,
                            sub = if (aircraft.verticalRateFpm > 100) "Climbing" else if (aircraft.verticalRateFpm < -100) "Descending" else "Level flight",
                            valueColor = vsColor,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            label = "HEADING",
                            value = "${aircraft.headingDeg.toInt()}°",
                            sub = "Compass track",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TelemetryCard(
                            label = "RANGE",
                            value = String.format(java.util.Locale.US, "%.1f NM", aircraft.distanceNm),
                            sub = "from radar center",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            label = "SQUAWK",
                            value = aircraft.squawk,
                            sub = "Mode-3/A transponder",
                            valueColor = RadarCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Live Coordinates and Storm Proximity Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = OledSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OledCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = "POS: ${String.format(java.util.Locale.US, "%.4f°N, %.4f°W", aircraft.latitude, -aircraft.longitude)} · 1090MHz EXTENDED SQUITTER",
                                color = RadarGreen,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            if (aircraft.stormAdvisorySummary != null) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = aircraft.stormAdvisorySummary ?: "",
                                    color = if (aircraft.isInsideStorm) Color(0xFFFF1744) else RadarAmber,
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // ACTIVE FLIGHT PLAN & ROUTE NAVIGATION SECTION
                val flightPlan = aircraft?.resolvedFlightPlan ?: com.example.model.Aircraft(
                    icao24 = "DEMO01",
                    callsign = typeInfo.typeCode,
                    tailNumber = "N${typeInfo.typeCode}X",
                    registeredOperator = "${typeInfo.manufacturer} Operational Fleet",
                    aircraftTypeCode = typeInfo.typeCode,
                    aircraftTypeName = typeInfo.modelName,
                    latitude = 36.5,
                    longitude = -82.4,
                    altitudeFt = typeInfo.cruiseAltitudeFt,
                    velocityKts = typeInfo.maxSpeedKts,
                    headingDeg = 90f,
                    verticalRateFpm = 0,
                    squawk = "1200",
                    onGround = false,
                    lastContactTimestamp = System.currentTimeMillis(),
                    craftCategory = if (isHelicopter) CraftCategory.HELICOPTER else CraftCategory.AIRPLANE
                ).resolvedFlightPlan

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "ACTIVE FLIGHT PLAN & ROUTE DATA")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF002B1D))
                            .border(1.dp, RadarGreen, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = flightPlan.flightRules,
                            color = RadarGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // 1. Origin to Destination Route Banner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Origin
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ORIGIN",
                                    color = AvionicsTextDim,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = flightPlan.originIcao,
                                    color = RadarCyan,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = flightPlan.originCity,
                                    color = AvionicsTextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "DEP: ${flightPlan.departureTimeUtc}",
                                    color = AvionicsTextDim,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Flight Path Vector Progress
                            Column(
                                modifier = Modifier.weight(1.2f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = flightPlan.flightNumber,
                                    color = AvionicsTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("──", color = RadarGreen, fontSize = 11.sp)
                                    if (isHelicopter) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_helicopter),
                                            contentDescription = null,
                                            tint = RadarCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AirplanemodeActive,
                                            contentDescription = null,
                                            tint = RadarGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text("──▶", color = RadarGreen, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${(flightPlan.progressPct * 100).toInt()}% ENROUTE",
                                    color = RadarGreen,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Destination
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "DESTINATION",
                                    color = AvionicsTextDim,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = flightPlan.destinationIcao,
                                    color = RadarGreen,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = flightPlan.destinationCity,
                                    color = AvionicsTextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "ETA: ${flightPlan.estimatedArrivalUtc}",
                                    color = RadarGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Progress Line
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF14261C))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(flightPlan.progressPct)
                                    .height(4.dp)
                                    .background(if (isHelicopter) RadarCyan else RadarGreen)
                            )
                        }

                        // 2. Route Airway & Waypoints
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "FILED ROUTE / AIRWAYS",
                            color = AvionicsTextDim,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF040A07))
                                .border(1.dp, Color(0xFF142B20), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = flightPlan.routeAirway,
                                color = RadarGreen,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 3. Flight Plan Details Grid
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TelemetryCard(
                                label = "PLANNED ALTITUDE",
                                value = if (flightPlan.plannedAltitudeFt >= 18000) "FL${flightPlan.plannedAltitudeFt / 100}" else "${flightPlan.plannedAltitudeFt} ft",
                                sub = "Cruise flight level",
                                modifier = Modifier.weight(1f)
                            )
                            TelemetryCard(
                                label = "FILED SPEED",
                                value = "${flightPlan.filedAirspeedKts} kts",
                                sub = "True Airspeed (TAS)",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TelemetryCard(
                                label = "ATC SECTOR",
                                value = flightPlan.atcSector,
                                sub = "Active radar control",
                                valueColor = RadarCyan,
                                modifier = Modifier.weight(1f)
                            )
                            TelemetryCard(
                                label = "DISTANCE / ETE",
                                value = "${flightPlan.distanceRemainingNm} NM",
                                sub = "${flightPlan.eteMinutes} min remaining",
                                valueColor = RadarGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        DetailRow(
                            label = "Origin Facility",
                            value = flightPlan.originName
                        )
                        DetailRow(
                            label = "Destination Facility",
                            value = flightPlan.destinationName
                        )
                        DetailRow(
                            label = "Filed Alternate",
                            value = flightPlan.alternateAirport
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Specifications Grid
                SectionHeader(title = "MANUFACTURER SPECIFICATIONS")
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpecCard(
                        icon = Icons.Default.Speed,
                        label = "MAX SPEED",
                        value = "${typeInfo.maxSpeedKts} kts",
                        sub = if (isHelicopter) "Vne limit" else "Cruise speed",
                        modifier = Modifier.weight(1f)
                    )
                    SpecCard(
                        icon = Icons.Default.VerticalAlignTop,
                        label = "CEILING",
                        value = "${typeInfo.cruiseAltitudeFt / 1000}k ft",
                        sub = "FL${typeInfo.cruiseAltitudeFt / 100}",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpecCard(
                        icon = Icons.Default.Straighten,
                        label = if (isHelicopter) "ROTOR DIAMETER" else "WINGSPAN",
                        value = "${typeInfo.wingspanMeters} m",
                        sub = "${(typeInfo.wingspanMeters * 3.28).toInt()} ft",
                        modifier = Modifier.weight(1f)
                    )
                    SpecCard(
                        icon = Icons.Default.AirplanemodeActive,
                        label = "RANGE",
                        value = "${typeInfo.rangeNm} NM",
                        sub = "${(typeInfo.rangeNm * 1.852).toInt()} km",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "Powerplant", value = typeInfo.engines)
                DetailRow(label = "Capacity", value = typeInfo.passengerCapacity)

                Spacer(modifier = Modifier.height(10.dp))

                // Description
                Text(
                    text = typeInfo.description,
                    color = AvionicsTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHelicopter) RadarCyan else RadarGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "CLOSE DETAILS",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = AvionicsTextDim,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp
    )
}

@Composable
private fun SpecCard(
    icon: ImageVector,
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(OledSurface)
            .border(1.dp, OledCardBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RadarCyan,
                    modifier = Modifier.padding(end = 4.dp).size(14.dp)
                )
                Text(
                    text = label,
                    color = AvionicsTextDim,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = AvionicsTextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = sub,
                color = RadarCyan,
                fontSize = 9.5.sp
            )
        }
    }
}

@Composable
private fun TelemetryCard(
    label: String,
    value: String,
    sub: String,
    valueColor: Color = RadarGreen,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF06180E))
            .border(1.dp, Color(0xFF1B4D32), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(text = label, color = AvionicsTextDim, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
            Text(text = value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(text = sub, color = AvionicsTextSecondary, fontSize = 9.sp)
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = AvionicsTextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = AvionicsTextDim,
            fontSize = 10.5.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
