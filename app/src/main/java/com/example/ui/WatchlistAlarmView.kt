package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.model.Aircraft
import com.example.model.AlertLogEntity
import com.example.model.RadarCenter
import com.example.model.WatchlistEntity
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
fun WatchlistAlarmView(
    watchlist: List<WatchlistEntity>,
    alertLogs: List<AlertLogEntity>,
    currentAircraft: List<Aircraft>,
    radarCenter: RadarCenter,
    onAddWatchlist: (tailNumber: String, label: String, radiusNm: Float) -> Unit,
    onToggleActive: (WatchlistEntity) -> Unit,
    onDeleteWatchlist: (WatchlistEntity) -> Unit,
    onClearLogs: () -> Unit,
    onTriggerTestAlarm: () -> Unit
) {
    var inputTailNumber by remember { mutableStateOf("") }
    var inputLabel by remember { mutableStateOf("") }
    var inputRadius by remember { mutableFloatStateOf(25.0f) }

    val timeFormatter = remember {
        SimpleDateFormat("MMM dd, HH:mm:ss 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .testTag("watchlist_alarm_view"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Add New Watched Tail Number Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = OledSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, RadarAmber)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddAlert,
                            contentDescription = null,
                            tint = RadarAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ARM PROXIMITY WATCHLIST ALARM",
                            color = RadarAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Enter a tail number (N-number / registration or callsign). An alarm will trigger when the aircraft breaches your defined radius around ${radarCenter.name}.",
                        color = AvionicsTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputTailNumber,
                        onValueChange = { inputTailNumber = it.uppercase() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_tail_number"),
                        label = { Text("AIRCRAFT TAIL # / REGISTRATION", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        placeholder = { Text("e.g. N856UA, N12345, DAL1420", color = AvionicsTextDim) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadarAmber,
                            unfocusedBorderColor = OledCardBorder,
                            focusedTextColor = AvionicsTextPrimary,
                            unfocusedTextColor = AvionicsTextPrimary,
                            focusedLabelColor = RadarAmber,
                            unfocusedLabelColor = AvionicsTextDim,
                            focusedContainerColor = OledBlack,
                            unfocusedContainerColor = OledBlack
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputLabel,
                        onValueChange = { inputLabel = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_label"),
                        label = { Text("OPTIONAL LABEL / NOTES", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        placeholder = { Text("e.g. VIP Transport, Family Flight", color = AvionicsTextDim) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RadarCyan,
                            unfocusedBorderColor = OledCardBorder,
                            focusedTextColor = AvionicsTextPrimary,
                            unfocusedTextColor = AvionicsTextPrimary,
                            focusedLabelColor = RadarCyan,
                            unfocusedLabelColor = AvionicsTextDim,
                            focusedContainerColor = OledBlack,
                            unfocusedContainerColor = OledBlack
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Perimeter Radius Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRIGGER RADIUS:",
                            color = AvionicsTextDim,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${inputRadius.toInt()} NM (${(inputRadius * 1.852).toInt()} km)",
                            color = RadarAmber,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = inputRadius,
                        onValueChange = { inputRadius = it },
                        valueRange = 5f..100f,
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = RadarAmber,
                            activeTrackColor = RadarAmber,
                            inactiveTrackColor = OledCardBorder
                        ),
                        modifier = Modifier.testTag("radius_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (inputTailNumber.isNotBlank()) {
                                    onAddWatchlist(inputTailNumber.trim(), inputLabel.trim(), inputRadius)
                                    inputTailNumber = ""
                                    inputLabel = ""
                                }
                            },
                            enabled = inputTailNumber.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_arm_watchlist"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RadarAmber,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ARM WATCHLIST TARGET", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.5.sp)
                        }

                        Button(
                            onClick = onTriggerTestAlarm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF261400),
                                contentColor = RadarAmber
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RadarAmber),
                            modifier = Modifier.testTag("btn_test_alarm")
                        ) {
                            Text("TEST ALARM", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section 2: Active Watchlist Entries
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE MONITORED TARGETS (${watchlist.size})",
                    color = RadarGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        if (watchlist.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(OledSurface)
                        .border(1.dp, OledCardBorder, RoundedCornerShape(10.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tail numbers in watchlist.\nEnter a tail number above or tap + on any aircraft card in the list.",
                        color = AvionicsTextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(watchlist, key = { it.tailNumber }) { item ->
                // Check if currently detected in range
                val activeHit = currentAircraft.find {
                    it.tailNumber.equals(item.tailNumber, ignoreCase = true) ||
                            it.callsign.equals(item.tailNumber, ignoreCase = true)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = OledSurface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (activeHit != null) RadarAmber else OledCardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.tailNumber,
                                    color = AvionicsTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (activeHit != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF331400))
                                            .border(1.dp, RadarAmber, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "IN RADAR: ${String.format(Locale.US, "%.1f NM", activeHit.distanceNm)}",
                                            color = RadarAmber,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            if (item.label.isNotBlank()) {
                                Text(
                                    text = item.label,
                                    color = RadarCyan,
                                    fontSize = 11.sp
                                )
                            }

                            Text(
                                text = "Alert Trigger: Within ${item.alertRadiusNm.toInt()} NM of radar center",
                                color = AvionicsTextDim,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = item.isActive,
                                onCheckedChange = { onToggleActive(item) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = RadarGreen,
                                    checkedTrackColor = Color(0xFF00381B),
                                    uncheckedThumbColor = AvionicsTextDim,
                                    uncheckedTrackColor = OledBlack
                                )
                            )

                            IconButton(onClick = { onDeleteWatchlist(item) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete watchlist entry",
                                    tint = RadarRedAlert,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Intrusion Alarm Log History
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = RadarCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BREACH HISTORY / ALARM LOG (${alertLogs.size})",
                        color = RadarCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                if (alertLogs.isNotEmpty()) {
                    Button(
                        onClick = onClearLogs,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = AvionicsTextDim)
                    ) {
                        Text("Clear Log", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        if (alertLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(OledSurface)
                        .border(1.dp, OledCardBorder, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No perimeter breach alarms recorded yet.",
                        color = AvionicsTextDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            items(alertLogs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0803)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF331C00))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🚨 ${log.tailNumber} (${log.callsign})",
                                color = RadarAmber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = timeFormatter.format(Date(log.timestamp)),
                                color = AvionicsTextDim,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Detected at ${String.format(Locale.US, "%.1f NM", log.distanceNm)} from ${log.radarLocationName} · FL${log.altitudeFt / 100} (${log.altitudeFt} ft)",
                            color = AvionicsTextPrimary,
                            fontSize = 11.sp
                        )

                        Text(
                            text = "Operator: ${log.registeredOperator} · Type: ${log.aircraftType}",
                            color = AvionicsTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
