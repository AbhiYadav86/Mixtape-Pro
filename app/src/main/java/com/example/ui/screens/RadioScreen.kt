package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.RetroSoundEffects
import com.example.data.model.RadioStationStatus
import com.example.data.model.RetroSkin
import com.example.data.model.Song
import com.example.data.model.StationLiveState

@Composable
fun RadioScreen(
    radioStations: List<Song>,
    stationStatusMap: Map<String, RadioStationStatus> = emptyMap(),
    isScanningFrequencies: Boolean = false,
    onlineSearchResults: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    skin: RetroSkin,
    onPlaySong: (Song) -> Unit,
    onSearchOnline: (String) -> Unit,
    onSaveToLibrary: (Song) -> Unit,
    onScanFrequencies: () -> Unit = {},
    onRecheckStation: (Song) -> Unit = {},
    onNavigateToPlayer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Live Radio FM, 1: 90s/00s Free Music Search
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // ALL, LIVE, OFFLINE, 90s, 2000s
    var searchQuery by remember { mutableStateOf("") }

    // Pulsing animation for live beacon indicators
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Calculate station live and offline counts
    val liveCount = remember(radioStations, stationStatusMap) {
        radioStations.count { station ->
            val status = stationStatusMap[station.id]
            status?.state == StationLiveState.LIVE || (status == null && station.id != "radio_pirate_cassette_standby" && !station.uri.contains("offline-archive.invalid"))
        }
    }
    val offlineCount = remember(radioStations, stationStatusMap) {
        radioStations.count { station ->
            val status = stationStatusMap[station.id]
            status?.state == StationLiveState.OFFLINE || (status == null && (station.id == "radio_pirate_cassette_standby" || station.uri.contains("offline-archive.invalid")))
        }
    }

    val filteredRadioStations = remember(radioStations, stationStatusMap, selectedStatusFilter) {
        when (selectedStatusFilter) {
            "LIVE" -> radioStations.filter { station ->
                val status = stationStatusMap[station.id]
                status?.state == StationLiveState.LIVE || (status == null && station.id != "radio_pirate_cassette_standby" && !station.uri.contains("offline-archive.invalid"))
            }
            "OFFLINE" -> radioStations.filter { station ->
                val status = stationStatusMap[station.id]
                status?.state == StationLiveState.OFFLINE || (status == null && (station.id == "radio_pirate_cassette_standby" || station.uri.contains("offline-archive.invalid")))
            }
            "2000s" -> radioStations.filter { (it.year?.toIntOrNull() ?: 0) >= 2000 || it.genre.contains("2000") || it.title.contains("2000") }
            "90s" -> radioStations.filter { (it.year?.toIntOrNull() ?: 0) < 2000 || it.genre.contains("90s") || it.title.contains("90s") }
            else -> radioStations
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(skin.chassisBackground)
            .padding(10.dp)
    ) {
        // Section Selector: Live Radio Stations vs Online Free 90s Vault
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF181B24))
                .border(1.dp, skin.chassisBorderColor, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val sections = listOf("📻 LIVE RADIO", "🌐 90s/00s VAULT")
            sections.forEachIndexed { index, title ->
                val isSelected = selectedSection == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) skin.accentColor else Color.Transparent)
                        .clickable {
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                            selectedSection = index
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = if (isSelected) Color.Black else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedSection == 0) {
            // Vintage Tuner Frequency Banner & Live Diagnostic Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D121B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A364F))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Digital LCD tuner readout with Live and Offline totals
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isScanningFrequencies) Color(0xFFFBBF24)
                                        else Color(0xFF10B981).copy(alpha = pulseAlpha)
                                    )
                            )
                            Column {
                                Text(
                                    text = if (isScanningFrequencies) "SCANNING…" else "$liveCount LIVE • $offlineCount OFF AIR",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isScanningFrequencies) Color(0xFFFBBF24) else Color(0xFF34D399)
                                )
                                Text(
                                    text = "Green = on air • Red = off air",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Rescan live frequencies button
                        Button(
                            onClick = {
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.RADIO_TUNER_TICK)
                                onScanFrequencies()
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan",
                                tint = skin.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isScanningFrequencies) "…" else "SCAN",
                                maxLines = 1,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = skin.accentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Vintage frequency dial visual tape
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF05080E))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("88.0", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B))
                            Text("92.5", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B))
                            Text("98.0 FM STEREO", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = skin.accentColor)
                            Text("104.5", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B))
                            Text("108.0", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status Tabs (ALL, LIVE, OFFLINE) with clean spacing and clear bounds
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "ALL" to "All (${radioStations.size})",
                    "LIVE" to "Live ($liveCount)",
                    "OFFLINE" to "Off air ($offlineCount)"
                ).forEach { (filterKey, label) ->
                    val isSelected = selectedStatusFilter == filterKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) {
                                    when (filterKey) {
                                        "LIVE" -> Color(0xFF064E3B)
                                        "OFFLINE" -> Color(0xFF450A0A)
                                        else -> skin.accentColor.copy(alpha = 0.35f)
                                    }
                                } else Color(0xFF161A22)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) {
                                    when (filterKey) {
                                        "LIVE" -> Color(0xFF10B981)
                                        "OFFLINE" -> Color(0xFFEF4444)
                                        else -> skin.accentColor
                                    }
                                } else Color(0xFF2A3140),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                selectedStatusFilter = filterKey
                            }
                            .padding(vertical = 7.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Indented Row below Live and Offline for 90's and 2000's Radio Stations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "90s" to "90s stations",
                    "2000s" to "2000s stations"
                ).forEach { (eraKey, label) ->
                    val isSelected = selectedStatusFilter == eraKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) skin.accentColor.copy(alpha = 0.28f)
                                else Color(0xFF181C26)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) skin.accentColor else Color(0xFF333D52),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                selectedStatusFilter = if (selectedStatusFilter == eraKey) "ALL" else eraKey
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredRadioStations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No stations match \"$selectedStatusFilter\"",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try selecting ALL to view all available frequencies",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("radio_stations_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredRadioStations) { station ->
                        val isCurrent = currentSong?.id == station.id
                        val status = stationStatusMap[station.id]
                        val isStationLive = status?.state == StationLiveState.LIVE ||
                                (status == null && station.id != "radio_pirate_cassette_standby" && !station.uri.contains("offline-archive.invalid"))
                        val isStationChecking = status?.state == StationLiveState.CHECKING

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                    onPlaySong(station)
                                    onNavigateToPlayer()
                                }
                                .border(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) skin.accentColor
                                    else if (isStationLive) Color(0xFF059669)
                                    else Color(0xFFDC2626),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) Color(0xFF1E2638)
                                else if (isStationLive) Color(0xFF141822)
                                else Color(0xFF1B1416)
                            )
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Left colored status bar strip for instant visual scanning
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .fillMaxHeight()
                                        .background(
                                            if (isStationChecking) Color(0xFFFBBF24)
                                            else if (isStationLive) Color(0xFF10B981)
                                            else Color(0xFFEF4444)
                                        )
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(10.dp)
                                ) {
                                    // Top Row: Album/Icon + Titles + Badges + Tune Button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Station Icon / Art with Live Indicator
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF242B38))
                                                .border(
                                                    1.dp,
                                                    if (isStationLive) Color(0xFF059669) else Color(0xFF7F1D1D),
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!station.albumArtUri.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = station.albumArtUri,
                                                    contentDescription = station.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (isCurrent && isPlaying) Icons.Default.GraphicEq else Icons.Default.Radio,
                                                    contentDescription = "Radio",
                                                    tint = if (isStationLive) skin.accentColor else Color(0xFFEF4444),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }

                                            if (isCurrent && isPlaying) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color(0x66000000)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.GraphicEq,
                                                        contentDescription = "Playing",
                                                        tint = Color(0xFF34D399),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            // Live Status Badge & Frequency Header
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // Live vs Offline prominent badge
                                                if (isStationChecking) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0x33FBBF24))
                                                            .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "🟡 PINGING...",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = Color(0xFFFBBF24)
                                                        )
                                                    }
                                                } else if (isStationLive) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFF064E3B))
                                                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(7.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(0xFF34D399).copy(alpha = pulseAlpha))
                                                            )
                                                            Text(
                                                                text = "LIVE ON AIR",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = Color(0xFF34D399)
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFF450A0A))
                                                            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "🔴 OFFLINE",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = Color(0xFFFCA5A5)
                                                        )
                                                    }
                                                }

                                                // Frequency tag
                                                val freqStr = status?.frequencyMhz ?: "98.5 FM"
                                                Text(
                                                    text = freqStr,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = if (isStationLive) skin.accentColor else Color(0xFFF87171)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                text = station.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.SansSerif,
                                                color = if (isCurrent) skin.accentColor else Color.White,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Text(
                                                text = "${station.genre} • ${station.artist}",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.SansSerif,
                                                color = if (isStationLive) Color(0xFFAAB6C6) else Color(0xFFFCA5A5),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                    }

                                    // Actions on their own row, so the station name and badges keep their width
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (isStationLive) {
                                        Button(
                                            onClick = {
                                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                                onPlaySong(station)
                                                onNavigateToPlayer()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isCurrent) skin.secondaryAccentColor else Color(0xFF10B981)
                                            ),
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isCurrent && isPlaying) "ON AIR" else "TUNE IN",
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.SansSerif,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black,
                                                maxLines = 1
                                            )
                                        }
                                    } else {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick = {
                                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.RADIO_TUNER_TICK)
                                                    onRecheckStation(station)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("RETEST", fontSize = 12.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = Color(0xFFFCA5A5), maxLines = 1)
                                            }
                                            Button(
                                                onClick = {
                                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                                    onPlaySong(station)
                                                    onNavigateToPlayer()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374151)),
                                                modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("PLAY SYNTH", fontSize = 12.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                            }
                                        }
                                    }

                                    // Signal line
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val pingText = if (status != null && status.pingMs > 0) "${status.pingMs} ms" else "24 ms"
                                    val signal = status?.signalPercent ?: 95
                                    val listeners = status?.listenerCount ?: 1240
                                    Text(
                                        text = if (isStationLive) "📶 $signal% • $pingText • ${station.bitrateKbps} kbps • 👥 $listeners" else "📶 No signal • off the air",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isStationLive) Color(0xFF34D399) else Color(0xFFFCA5A5),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isStationLive) Color(0xFF11141C) else Color(0xFF281518), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Free 90s/00s Music Search Section
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    onSearchOnline(it)
                },
                placeholder = {
                    Text("Search 90s eurodance, synthwave, trance...", fontSize = 14.sp, fontFamily = FontFamily.SansSerif, color = Color(0xFF94A3B8))
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = skin.accentColor)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("online_search_input"),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "DISCOVERED TRACKS (${onlineSearchResults.size})",
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = skin.lcdTextColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(onlineSearchResults) { song ->
                    val isCurrent = currentSong?.id == song.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                onPlaySong(song)
                                onNavigateToPlayer()
                            }
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) skin.accentColor else skin.chassisBorderColor,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D26))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = if (isCurrent) skin.accentColor else Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${song.artist} • ${song.genre} [${song.year}]",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                        onPlaySong(song)
                                        onNavigateToPlayer()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor),
                                    modifier = Modifier.height(42.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Stream",
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play", fontSize = 13.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = Color.Black)
                                }

                                IconButton(
                                    onClick = {
                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CD_SPIN)
                                        onSaveToLibrary(song)
                                    },
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download & Save",
                                        tint = skin.secondaryAccentColor,
                                        modifier = Modifier.size(24.dp)
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
