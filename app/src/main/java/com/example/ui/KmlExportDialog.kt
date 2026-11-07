package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.FlightTrackPointEntity
import com.example.ui.theme.AvionicsTextDim
import com.example.ui.theme.AvionicsTextPrimary
import com.example.ui.theme.AvionicsTextSecondary
import com.example.ui.theme.OledBlack
import com.example.ui.theme.OledCardBorder
import com.example.ui.theme.OledSurface
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RadarGreen
import com.example.util.KmlExportManager

@Composable
fun KmlExportDialog(
    tracks: List<FlightTrackPointEntity>,
    selectedTail: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var exportMode by remember { mutableStateOf("CURRENT") } // "CURRENT", "ALL", "STREAMING"

    val effectiveTail = selectedTail ?: tracks.firstOrNull()?.tailNumber ?: "N856UA"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.5.dp, RadarCyan, RoundedCornerShape(18.dp)),
            color = OledBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = RadarCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "GOOGLE EARTH .KML EXPORT",
                                color = AvionicsTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "3D Flight Tracks & Live Streaming Layer",
                                color = RadarCyan,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(OledSurface)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AvionicsTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SELECT EXPORT FORMAT",
                    color = AvionicsTextDim,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 1: Current Selected Tail Track
                KmlOptionCard(
                    title = "Selected Craft Flight Path ($effectiveTail)",
                    description = "3D extruded route profile with altitude curtains and speed metrics for $effectiveTail.",
                    icon = Icons.Default.Download,
                    isSelected = exportMode == "CURRENT",
                    onClick = { exportMode = "CURRENT" }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: All Watchlist Tracks
                KmlOptionCard(
                    title = "All Watchlist Recorded Paths",
                    description = "Consolidated multi-aircraft flight tracks and point fixes across your entire watchlist.",
                    icon = Icons.Default.Public,
                    isSelected = exportMode == "ALL",
                    onClick = { exportMode = "ALL" }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 3: Live Data Streaming Layer (NetworkLink)
                KmlOptionCard(
                    title = "Google Earth Live Streaming Layer",
                    description = "NetworkLink KML layer that automatically streams live ADS-B 1090MHz targets every 5 seconds.",
                    icon = Icons.Default.Stream,
                    isSelected = exportMode == "STREAMING",
                    onClick = { exportMode = "STREAMING" }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Share / Export and Copy
                Button(
                    onClick = {
                        when (exportMode) {
                            "CURRENT" -> {
                                val kml = KmlExportManager.generateWatchlistTracksKml(tracks, effectiveTail)
                                KmlExportManager.exportAndShareKml(context, kml, "$effectiveTail-track.kml")
                            }
                            "ALL" -> {
                                val kml = KmlExportManager.generateWatchlistTracksKml(tracks)
                                KmlExportManager.exportAndShareKml(context, kml, "SkyWatch-Watchlist-Tracks.kml")
                            }
                            "STREAMING" -> {
                                val kml = KmlExportManager.generateLiveStreamingNetworkLinkKml()
                                KmlExportManager.exportAndShareKml(context, kml, "SkyWatch-Live-Stream.kml")
                            }
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RadarGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "OPEN IN GOOGLE EARTH / SHARE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val kmlContent = when (exportMode) {
                            "CURRENT" -> KmlExportManager.generateWatchlistTracksKml(tracks, effectiveTail)
                            "ALL" -> KmlExportManager.generateWatchlistTracksKml(tracks)
                            "STREAMING" -> KmlExportManager.generateLiveStreamingNetworkLinkKml()
                            else -> ""
                        }
                        KmlExportManager.copyKmlToClipboard(context, kmlContent, "SkyWatch-$exportMode")
                    },
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RadarCyan
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RadarCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "COPY .KML XML TO CLIPBOARD",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun KmlOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFF002919) else OledSurface)
            .border(1.dp, if (isSelected) RadarGreen else OledCardBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) RadarGreen else AvionicsTextDim,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) AvionicsTextPrimary else AvionicsTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = description,
                    color = AvionicsTextDim,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
