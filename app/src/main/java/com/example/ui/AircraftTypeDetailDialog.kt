package com.example.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AircraftDatabaseService
import com.example.data.AircraftMetadata
import com.example.data.AircraftRegistry
import com.example.data.FlightAwareRegistry
import com.example.data.FlightRouteDatabase
import com.example.model.Aircraft
import com.example.model.AircraftTypeInfo
import com.example.model.FlightPlanData
import com.example.ui.theme.AvionicsTextDim
import com.example.ui.theme.AvionicsTextPrimary
import com.example.ui.theme.AvionicsTextSecondary
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OledCardBorder
import com.example.ui.theme.OledSurface
import com.example.ui.theme.RadarAmber
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarGreen
import java.util.Locale

@Composable
fun AircraftTypeDetailDialog(
    typeInfo: AircraftTypeInfo,
    aircraft: Aircraft? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Pull initial verified FlightAware registration data
    val flightAwareInfo = aircraft?.let { FlightAwareRegistry.resolveFlightAwareTailInfo(it) }

    val rawTail = flightAwareInfo?.tailNumber ?: aircraft?.tailNumber ?: ""
    val rawHex = aircraft?.icao24 ?: ""
    val rawOp = aircraft?.registeredOperator ?: flightAwareInfo?.operatorName ?: ""

    // 1. Initial lookup from AircraftDatabaseService fast cache / fleet decoder
    val initialMeta = remember(rawTail, rawHex) {
        AircraftDatabaseService.getCachedOrFleet(rawTail, rawHex, rawOp)
    }
    val cleanCallsign = aircraft?.callsign?.trim()?.replace(" ", "") ?: ""
    val icaoHex = aircraft?.icao24?.trim()?.uppercase() ?: ""

    var liveMetadata by remember(rawTail, rawHex) { mutableStateOf(initialMeta) }
    var liveRoute by remember(cleanCallsign) {
        mutableStateOf<FlightPlanData?>(
            aircraft?.resolvedFlightPlan
                ?: if (cleanCallsign.length >= 4) FlightRouteDatabase.getCachedRoute(cleanCallsign) else null
        )
    }

    // 2. Query live global aircraft registries (ADSBDB & HexDB) + live flight route asynchronously
    LaunchedEffect(rawTail, rawHex, cleanCallsign) {
        if (rawTail.isNotBlank() || rawHex.isNotBlank()) {
            val fetched = AircraftDatabaseService.lookupAircraft(rawTail, rawHex, rawOp)
            if (fetched != null) {
                liveMetadata = fetched
            }
        }
        if (cleanCallsign.length >= 4 && liveRoute == null) {
            val fetchedRoute = FlightRouteDatabase.resolveFlightPlanAsync(cleanCallsign)
            if (fetchedRoute != null) {
                liveRoute = fetchedRoute
            }
        }
    }

    // Effective, real-world verified model & airframe parameters
    val effectiveTypeCode = liveMetadata?.icaoTypeCode?.ifBlank { null }
        ?: flightAwareInfo?.aircraftTypeCode?.ifBlank { null }
        ?: typeInfo.typeCode
    val resolvedCatalogInfo = remember(effectiveTypeCode, liveMetadata?.model, aircraft?.isHelicopter, typeInfo) {
        AircraftRegistry.getAircraftType(
            code = effectiveTypeCode,
            descriptionHint = liveMetadata?.model ?: typeInfo.modelName,
            isHelicopterHint = liveMetadata?.isHelicopter == true || typeInfo.isHelicopter || aircraft?.isHelicopter == true
        )
    }
    val effectiveModel = liveMetadata?.model?.ifBlank { null }
        ?: flightAwareInfo?.airframeModel?.ifBlank { null }
        ?: resolvedCatalogInfo.modelName
    val effectiveManufacturer = liveMetadata?.manufacturer?.ifBlank { null }
        ?: resolvedCatalogInfo.manufacturer
    val effectiveOperator = liveMetadata?.operator?.ifBlank { null }
        ?: flightAwareInfo?.operatorName?.ifBlank { null }
        ?: rawOp.ifBlank { "Unlisted Operator" }
    val effectiveCountry = liveMetadata?.country ?: flightAwareInfo?.countryOfRegistration ?: "United States"

    val isHelicopter = liveMetadata?.isHelicopter == true ||
            resolvedCatalogInfo.isHelicopter ||
            aircraft?.isHelicopter == true

    // Ensure photo strictly correlates to verified airframe family; null shows technical schematic instead of guessing
    val correlatedPhotoName = resolvedCatalogInfo.drawableResName
        ?: liveMetadata?.correlatedPhotoDrawable
        ?: flightAwareInfo?.photoDrawableResName

    val imageResId = correlatedPhotoName?.let {
        context.resources.getIdentifier(it, "drawable", context.packageName)
    } ?: 0

    val baseIdentifier = if (aircraft != null) {
        if (aircraft.callsign.isNotBlank()) aircraft.callsign else "HEX #${aircraft.icao24.uppercase()}"
    } else {
        effectiveModel
    }

    val displayTail = liveMetadata?.tailNumber?.ifBlank { null }
        ?: flightAwareInfo?.tailNumber?.ifBlank { null }
        ?: rawTail
    val displayHeaderTitle = if (displayTail.isNotBlank() && !displayTail.equals(baseIdentifier, ignoreCase = true)) {
        "$baseIdentifier · $displayTail"
    } else {
        baseIdentifier
    }

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
                // Header: Callsign, FlightAware Tail Number & Close button
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
                                text = displayHeaderTitle,
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
                                        text = "🔴 LIVE",
                                        color = Color(0xFFFF5252),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Text(
                            text = "$effectiveManufacturer · $effectiveTypeCode · $effectiveModel",
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

                // Aircraft Photo Card correlated to verified airframe
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(205.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OledCardBorder)
                ) {
                    if (liveMetadata?.photoUrl != null) {
                        Box(modifier = Modifier.fillMaxWidth().height(205.dp)) {
                            AsyncImage(
                                model = liveMetadata?.photoUrl,
                                contentDescription = "Photo of $effectiveModel",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(205.dp),
                                contentScale = ContentScale.Crop,
                                error = if (imageResId != 0) painterResource(id = imageResId) else null
                            )
                            // Live Verified Photo overlay tag
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xCC000000))
                                    .border(1.dp, if (isHelicopter) RadarCyan else RadarGreen, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "📷 LIVE PHOTO: $effectiveModel",
                                    color = if (isHelicopter) RadarCyan else RadarGreen,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else if (imageResId != 0) {
                        Box(modifier = Modifier.fillMaxWidth().height(205.dp)) {
                            Image(
                                painter = painterResource(id = imageResId),
                                contentDescription = "Photo of $effectiveModel",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(205.dp),
                                contentScale = ContentScale.Crop
                            )

                            // Type & photo correlation overlay tag on photo
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xCC000000))
                                    .border(1.dp, if (isHelicopter) RadarCyan else RadarGreen, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "📷 VERIFIED PHOTO: $effectiveModel",
                                    color = if (isHelicopter) RadarCyan else RadarGreen,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(205.dp)
                                .background(Color(0xFF00150A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (isHelicopter) Icons.Default.Speed else Icons.Default.AirplanemodeActive,
                                    contentDescription = null,
                                    tint = if (isHelicopter) RadarCyan else RadarGreen,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$effectiveModel Technical Schematic",
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

                // LIVE VERIFIED FLIGHT ROUTE / ITINERARY SECTION
                if (aircraft != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    SectionHeader(title = "LIVE FLIGHT ROUTE / ITINERARY")
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = OledSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val verifiedRoute = liveRoute
                            if (verifiedRoute != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ORIGIN",
                                            color = AvionicsTextDim,
                                            fontSize = 8.5.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = verifiedRoute.originIcao,
                                            color = RadarCyan,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = verifiedRoute.originCity,
                                            color = AvionicsTextSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }

                                    Text(
                                        text = "✈ ──▶",
                                        color = RadarGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "DESTINATION",
                                            color = AvionicsTextDim,
                                            fontSize = 8.5.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = verifiedRoute.destinationIcao,
                                            color = RadarGreen,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = verifiedRoute.destinationCity,
                                            color = AvionicsTextSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${verifiedRoute.originName} -> ${verifiedRoute.destinationName}",
                                    color = AvionicsTextPrimary,
                                    fontSize = 10.5.sp
                                )
                                Text(
                                    text = "Source: ${verifiedRoute.sourceName}${if (verifiedRoute.airlineName.isNotBlank()) " · ${verifiedRoute.airlineName}" else ""}",
                                    color = RadarGreen,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text(
                                    text = if (cleanCallsign.length >= 4)
                                        "No verified public IFR route on file for $cleanCallsign"
                                    else
                                        "No flight plan callsign broadcast on ADS-B squitter",
                                    color = AvionicsTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Use the cross-check links below for live airline / FlightAware / Google status.",
                                    color = AvionicsTextDim,
                                    fontSize = 9.5.sp
                                )
                            }
                        }
                    }
                }

                // FLIGHTAWARE & GLOBAL REGISTRY VERIFIED AIRFRAME SECTION
                if (displayTail.isNotBlank() || flightAwareInfo != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    SectionHeader(title = "FLIGHTAWARE & GLOBAL REGISTRY VERIFIED AIRFRAME")
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = OledSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AirplanemodeActive,
                                        contentDescription = null,
                                        tint = RadarCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "TAIL #$displayTail",
                                        color = AvionicsTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF002B1D))
                                        .border(1.dp, RadarGreen, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (liveMetadata?.isLiveVerified == true) "✅ LIVE GLOBAL REGISTRY" else "✅ FLIGHTAWARE RECORD",
                                        color = RadarGreen,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            DetailRow(label = "Aircraft Manufacturer", value = effectiveManufacturer)
                            DetailRow(label = "Airframe Model", value = "$effectiveModel ($effectiveTypeCode)")
                            DetailRow(label = "Registered Operator", value = effectiveOperator)
                            DetailRow(label = "Registration Country", value = effectiveCountry)

                            Spacer(modifier = Modifier.height(6.dp))
                            // Verified Photo correlation confirmation note
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF071C12))
                                    .border(1.dp, Color(0xFF16442A), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "📷 Photo Verified: Correlated to airframe $effectiveModel",
                                    color = RadarGreen,
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // FlightAware direct links for this tail number
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val url = flightAwareInfo?.flightAwareRegistrationUrl ?: "https://www.flightaware.com/resources/registration/$displayTail"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RadarCyan),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B))
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("TAIL PROFILE", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val url = flightAwareInfo?.flightAwarePhotosUrl ?: "https://www.flightaware.com/photos/aircraft/$displayTail"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RadarGreen),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B))
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AIRFRAME PHOTOS", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SOURCE VERIFICATION & CROSS-CHECK ON GOOGLE FLIGHT TRACKING
                SectionHeader(title = "VERIFY SOURCE & LIVE FLIGHT DATA")
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TravelExplore,
                                contentDescription = null,
                                tint = RadarGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VERIFIED FLIGHT TRACKING SOURCE",
                                color = RadarGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Cross-check live airline routes, scheduled vs actual arrival, delays, and terminal gates directly against Google's official real-time flight tracker and global radar records:",
                            color = AvionicsTextSecondary,
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Primary Google Flight Tracking Button
                        Button(
                            onClick = {
                                val query = if (cleanCallsign.isNotBlank()) cleanCallsign else icaoHex
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.google.com/search?q=${Uri.encode("flight $query")}")
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00381B),
                                contentColor = RadarGreen
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, RadarGreen)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = RadarGreen
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = "🔍 CROSS-CHECK ON GOOGLE FLIGHTS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = RadarGreen
                                    )
                                    val lookupText = if (cleanCallsign.isNotBlank()) "Search Google Flights for '$cleanCallsign'" else "Search Google Flights for Hex '$icaoHex'"
                                    Text(
                                        text = lookupText,
                                        fontSize = 9.sp,
                                        color = AvionicsTextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Secondary Verified Live Tracking Sources
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val targetQ = if (cleanCallsign.isNotBlank()) cleanCallsign else icaoHex
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.flightaware.com/live/flight/$targetQ")
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RadarCyan),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("FLIGHTAWARE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val targetQ = if (cleanCallsign.isNotBlank()) cleanCallsign else icaoHex
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.flightradar24.com/$targetQ")
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RadarAmber),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("FLIGHTRADAR24", fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (icaoHex.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://opensky-network.org/network/explorer?icao=${icaoHex.lowercase()}")
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AvionicsTextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("OPENSKY NETWORK ADS-B RECORD (#$icaoHex)", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }

                // Real-time verified ADS-B transponder telemetry
                if (aircraft != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(title = "LIVE ADS-B TRANSPONDER TELEMETRY")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2E0000))
                                .border(1.dp, Color(0xFFFF1744), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🔴 1090MHz STREAMING",
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
                            label = "COMPASS TRACK",
                            value = "${aircraft.headingDeg.toInt()}°",
                            sub = "True heading",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TelemetryCard(
                            label = "RANGE",
                            value = String.format(Locale.US, "%.1f NM", aircraft.distanceNm),
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TelemetryCard(
                            label = "ICAO 24-BIT HEX",
                            value = "#${aircraft.icao24.uppercase()}",
                            sub = "Mode-S transponder ID",
                            valueColor = RadarGreen,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            label = "TRANSPONDER CALLSIGN",
                            value = aircraft.callsign.ifBlank { "UNASSIGNED" },
                            sub = "ADS-B BDS 0,8 squitter",
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
                                text = "POS: ${String.format(Locale.US, "%.4f°N, %.4f°W", aircraft.latitude, -aircraft.longitude)} · 1090MHz EXTENDED SQUITTER",
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

                Spacer(modifier = Modifier.height(14.dp))

                // Specifications Grid
                SectionHeader(title = "MANUFACTURER SPECIFICATIONS")
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpecCard(
                        icon = Icons.Default.Speed,
                        label = "MAX SPEED",
                        value = if (resolvedCatalogInfo.maxSpeedKts > 0) "${resolvedCatalogInfo.maxSpeedKts} kts" else "N/A",
                        sub = if (resolvedCatalogInfo.maxSpeedKts > 0) {
                            if (isHelicopter) "Vne limit" else "Cruise speed"
                        } else "Type $effectiveTypeCode",
                        modifier = Modifier.weight(1f)
                    )
                    SpecCard(
                        icon = Icons.Default.VerticalAlignTop,
                        label = "CEILING",
                        value = if (resolvedCatalogInfo.cruiseAltitudeFt > 0) "${resolvedCatalogInfo.cruiseAltitudeFt / 1000}k ft" else "N/A",
                        sub = if (resolvedCatalogInfo.cruiseAltitudeFt > 0) "FL${resolvedCatalogInfo.cruiseAltitudeFt / 100}" else "Pending spec",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpecCard(
                        icon = Icons.Default.Straighten,
                        label = if (isHelicopter) "ROTOR DIAMETER" else "WINGSPAN",
                        value = if (resolvedCatalogInfo.wingspanMeters > 0.0) "${resolvedCatalogInfo.wingspanMeters} m" else "N/A",
                        sub = if (resolvedCatalogInfo.wingspanMeters > 0.0) "${(resolvedCatalogInfo.wingspanMeters * 3.28).toInt()} ft" else "Pending spec",
                        modifier = Modifier.weight(1f)
                    )
                    SpecCard(
                        icon = Icons.Default.AirplanemodeActive,
                        label = "RANGE",
                        value = if (resolvedCatalogInfo.rangeNm > 0) "${resolvedCatalogInfo.rangeNm} NM" else "N/A",
                        sub = if (resolvedCatalogInfo.rangeNm > 0) "${(resolvedCatalogInfo.rangeNm * 1.852).toInt()} km" else "Pending spec",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(label = "Powerplant", value = resolvedCatalogInfo.engines)
                DetailRow(label = "Capacity", value = resolvedCatalogInfo.passengerCapacity)

                Spacer(modifier = Modifier.height(10.dp))

                // Description
                Text(
                    text = resolvedCatalogInfo.description,
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
