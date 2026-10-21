package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.StudioDsp
import com.example.audio.StudioJobs
import com.example.data.model.AudioSource
import com.example.data.model.RetroSkin
import com.example.data.model.Song
import com.example.ui.viewmodel.MainViewModel

// =================================================================================================
// 2. CLEAN-UP STUDIO
// =================================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanUpStudioPanel(
    viewModel: MainViewModel,
    librarySongs: List<Song>,
    initialSong: Song?,
    skin: RetroSkin,
    exportState: MainViewModel.StudioExportState,
    onPlayExport: (Song) -> Unit
) {
    var song by remember { mutableStateOf(initialSong ?: librarySongs.firstOrNull()) }
    var showPicker by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(MainViewModel.CleanSettings()) }
    val task by viewModel.studioTask.collectAsState()
    val monitor by com.example.audio.StudioPreviewPlayer.state.collectAsState()
    val dur = (song?.durationMs ?: 0L).coerceAtLeast(1000L)
    // which version the monitor is playing, if it holds this song's Clean-Up stems
    val liveStem = song?.let { if (monitor.loaded && viewModel.isCleanPreviewLoaded(it, settings)) monitor.activeStem else null }

    // Moving a strength slider while listening re-renders from the same moment (no Preview tap)
    LaunchedEffect(settings.noiseStrength) {
        kotlinx.coroutines.delay(450)
        song?.let { if (settings.tool != MainViewModel.CleanTool.CUT) viewModel.refreshCleanUpPreview(it, settings) }
    }

    // default cut selection: 10 s in the middle
    LaunchedEffect(song?.id) {
        val mid = dur / 2
        settings = settings.copy(cutStartMs = (mid - 5000).coerceAtLeast(0), cutEndMs = (mid + 5000).coerceAtMost(dur))
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StudioMonitor(viewModel, skin, MainViewModel.SECTION_CLEAN, "CLEAN-UP MONITOR")

        StudioCard(skin, title = "CLEAN-UP STUDIO", subtitle = "Remove hiss & hum, or cut out a section", icon = Icons.Default.CleaningServices) {
            SelectedTrackRow(song, skin) { showPicker = true }
            StudioSegmented(
                options = listOf(
                    MainViewModel.CleanTool.NOISE to "Noise",
                    MainViewModel.CleanTool.CUT to "Cut"
                ),
                selected = settings.tool,
                skin = skin
            ) { settings = settings.copy(tool = it) }
        }

        val s = song
        if (s != null) {
            when (settings.tool) {
                MainViewModel.CleanTool.NOISE -> StudioCard(skin, title = "Noise Remover", subtitle = "Learns the background noise from the quietest moments, then removes it everywhere", icon = Icons.Default.Hearing) {
                    Text("Listen", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    StudioSegmented(
                        options = listOf(0 to "Original", 1 to "Cleaned"),
                        selected = liveStem ?: -1,
                        skin = skin
                    ) { i -> viewModel.listenCleanUp(s, settings, i) }
                    StudioSlider("Strength", "${(settings.noiseStrength * 100).toInt()}%", settings.noiseStrength, 0f..1f, skin) {
                        settings = settings.copy(noiseStrength = it)
                    }
                    Text("Tip: start around 50–60 %. Very high settings can sound watery.", color = StudioSubtle, fontSize = 12.sp)
                }
                MainViewModel.CleanTool.CUT -> StudioCard(skin, title = "Cut a Section", subtitle = "Remove the highlighted part; the two sides are joined with a tiny crossfade", icon = Icons.Default.ContentCut) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("REMOVE ${fmtMmSs(settings.cutStartMs)} – ${fmtMmSs(settings.cutEndMs)}", color = skin.accentColor, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("−${fmtMmSs(settings.cutEndMs - settings.cutStartMs)}", color = Color(0xFFFCA5A5), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                    RangeSlider(
                        value = (settings.cutStartMs.toFloat() / dur)..(settings.cutEndMs.toFloat() / dur),
                        onValueChange = { r ->
                            settings = settings.copy(cutStartMs = (r.start * dur).toLong(), cutEndMs = (r.endInclusive * dur).toLong().coerceAtLeast((r.start * dur).toLong() + 500))
                        },
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFCA5A5), activeTrackColor = Color(0xFFEF4444), inactiveTrackColor = Color(0x40FFFFFF))
                    )
                }
            }

            StudioCard(skin, title = "Listen & save", subtitle = "Switch between Original and Result — playback stays at the same moment", icon = Icons.Default.Headphones) {
                val isCut = settings.tool == MainViewModel.CleanTool.CUT
                StudioButtonPair(
                    skin,
                    left = Triple("Original", Icons.Default.PlayArrow) {
                        if (isCut) viewModel.previewOriginal(s, (settings.cutStartMs - 6000).coerceAtLeast(0)) else viewModel.listenCleanUp(s, settings, 0)
                    },
                    right = Triple("Preview result", Icons.Default.Headphones) { viewModel.previewCleanUp(s, settings) },
                    enabled = task == null
                )
                val saveLabel = when (settings.tool) {
                    MainViewModel.CleanTool.NOISE -> "Save cleaned track"
                    MainViewModel.CleanTool.CUT -> "Save edited track"
                }
                StudioButton(saveLabel, Icons.Default.Save, skin, Modifier.fillMaxWidth(), enabled = !exportState.isRunning && task == null) {
                    viewModel.stopStudioAudio()
                    viewModel.exportCleanUp(s, settings)
                }
                StudioExportStatusCard(exportState, skin, onPlayExport) { viewModel.clearStudioExportMessage() }
            }
        }
    }

    if (showPicker) {
        StudioTrackPickerDialog(librarySongs, skin, onPick = { song = it; showPicker = false }, onDismiss = { showPicker = false })
    }
}

// =================================================================================================
// 3. FX & MASHUP STUDIO
// =================================================================================================

@Composable
fun FxMashupPanel(
    viewModel: MainViewModel,
    librarySongs: List<Song>,
    initialSong: Song?,
    skin: RetroSkin,
    exportState: MainViewModel.StudioExportState,
    onPlayExport: (Song) -> Unit
) {
    var tab by rememberSaveableInt(0)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StudioMonitor(viewModel, skin, MainViewModel.SECTION_FX, "FX & MASHUP MONITOR")
        StudioSegmented(options = listOf(0 to "Retro FX", 1 to "Mashup"), selected = tab, skin = skin) { viewModel.stopStudioAudio(); tab = it }
        if (tab == 0) {
            RetroFxSection(viewModel, librarySongs, initialSong, skin, exportState, onPlayExport)
        } else {
            MashupSection(viewModel, librarySongs, skin, exportState, onPlayExport)
        }
    }
}

@Composable
private fun rememberSaveableInt(initial: Int): MutableState<Int> =
    androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initial) }

@Composable
private fun RetroFxSection(
    viewModel: MainViewModel,
    librarySongs: List<Song>,
    initialSong: Song?,
    skin: RetroSkin,
    exportState: MainViewModel.StudioExportState,
    onPlayExport: (Song) -> Unit
) {
    var song by remember { mutableStateOf(initialSong ?: librarySongs.firstOrNull()) }
    var showPicker by remember { mutableStateOf(false) }
    var preset by remember { mutableStateOf(StudioDsp.RetroPreset.CASSETTE) }
    var intensity by remember { mutableStateOf(0.8f) }
    val task by viewModel.studioTask.collectAsState()

    StudioCard(skin, title = "RETRO FX", subtitle = "Time Machine & Remix Lab presets — makes a new version of a song", icon = Icons.Default.AutoFixHigh) {
        SelectedTrackRow(song, skin) { showPicker = true }
        // 2-column grid of equal tiles
        StudioDsp.RetroPreset.values().toList().chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { p ->
                    val sel = p == preset
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) skin.accentColor.copy(alpha = 0.22f) else Color(0xFF1A1F2A))
                            .border(if (sel) 2.dp else 1.dp, if (sel) skin.accentColor else Color(0x30FFFFFF), RoundedCornerShape(10.dp))
                            .clickable { preset = p }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.emoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(p.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
        Text(preset.description, color = StudioSubtle, fontSize = 12.sp)
        StudioSlider("Intensity", "${(intensity * 100).toInt()}%", intensity, 0.1f..1f, skin) { intensity = it }
    }

    val s = song
    if (s != null) {
        StudioCard(skin, title = "Listen & save", subtitle = "Switch between Original and FX — playback stays at the same moment", icon = Icons.Default.Headphones) {
            StudioButtonPair(
                skin,
                left = Triple("Original", Icons.Default.PlayArrow) { viewModel.previewOriginal(s) },
                right = Triple("Preview FX", Icons.Default.Headphones) { viewModel.previewFx(s, preset, intensity) },
                enabled = task == null
            )
            StudioButton("Save ${preset.label} version", Icons.Default.Save, skin, Modifier.fillMaxWidth(), enabled = !exportState.isRunning && task == null) {
                viewModel.stopStudioAudio()
                viewModel.exportFx(s, preset, intensity)
            }
            StudioExportStatusCard(exportState, skin, onPlayExport) { viewModel.clearStudioExportMessage() }
        }
    }

    if (showPicker) {
        StudioTrackPickerDialog(librarySongs, skin, onPick = { song = it; showPicker = false }, onDismiss = { showPicker = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MashupSection(
    viewModel: MainViewModel,
    librarySongs: List<Song>,
    skin: RetroSkin,
    exportState: MainViewModel.StudioExportState,
    onPlayExport: (Song) -> Unit
) {
    val context = LocalContext.current
    val layers by viewModel.mashupLayers.collectAsState()
    val settings by viewModel.mashupSettings.collectAsState()
    val task by viewModel.studioTask.collectAsState()
    val recordingMs by viewModel.micRecordingMs.collectAsState()
    val micLevel by viewModel.micLevel.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startMicRecording()
    }

    StudioCard(skin, title = "MASHUP STUDIO", subtitle = "Layer up to 4 tracks at the same time — beats, voice-overs, mashups", icon = Icons.Default.Layers) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StudioButton("Add track", Icons.Default.Add, skin, Modifier.weight(1f), enabled = layers.size < 4 && recordingMs == null) { showPicker = true }
            if (recordingMs == null) {
                StudioButton("Record mic", Icons.Default.Mic, skin, Modifier.weight(1f), primary = false, enabled = layers.size < 4) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.startMicRecording()
                    } else {
                        micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            } else {
                StudioButton("Stop ${fmtMmSs(recordingMs ?: 0L)}", Icons.Default.Stop, skin, Modifier.weight(1f)) { viewModel.stopMicRecording() }
            }
        }
        if (recordingMs != null) {
            LinearProgressIndicator(
                progress = { micLevel.coerceIn(0f, 1f) },
                color = Color(0xFFEF4444),
                trackColor = Color(0x33FFFFFF),
                modifier = Modifier.fillMaxWidth()
            )
            Text("Recording… use headphones so the mic doesn't pick up the music", color = StudioSubtle, fontSize = 12.sp)
        }
        if (layers.isEmpty()) {
            Text("Layer 1 sets the tempo. Add a beat, another song, or record your own voice-over.", color = StudioSubtle, fontSize = 12.sp)
        }
    }

    layers.forEachIndexed { index, layer ->
        MashupLayerCard(
            index, layer, skin,
            onUpdate = { viewModel.updateMashupLayer(index, it) },
            onRemove = { viewModel.removeMashupLayer(index) },
            onListen = { viewModel.previewSource(layer.uri, "Layer ${index + 1}: ${layer.title}", layer.durationMs, layer.inMs) }
        )
    }

    if (layers.isNotEmpty()) {
        StudioCard(skin, title = "Mix settings", icon = Icons.Default.Tune) {
            StudioToggle("Match tempo", "Stretch layers to layer 1's tempo (pitch stays the same)", settings.matchTempo, skin) { viewModel.updateMashupSettings(settings.copy(matchTempo = it)) }
            StudioToggle("Align beats", "Nudge each layer so its beats land on layer 1's beats", settings.alignBeats, skin) { viewModel.updateMashupSettings(settings.copy(alignBeats = it)) }
            StudioToggle("Auto-duck music", "Music dips while a Voice layer is talking", settings.autoDuck, skin) { viewModel.updateMashupSettings(settings.copy(autoDuck = it)) }
            if (settings.autoDuck) {
                StudioSlider("Duck amount", "${(settings.duckAmount * 100).toInt()}%", settings.duckAmount, 0.2f..1f, skin) { viewModel.updateMashupSettings(settings.copy(duckAmount = it)) }
            }
        }
        StudioCard(skin, title = "Listen & save", icon = Icons.Default.Headphones) {
            OutlinedTextField(
                value = title, onValueChange = { title = it }, singleLine = true, label = { Text("Mashup title") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = skin.accentColor),
                modifier = Modifier.fillMaxWidth()
            )
            StudioButtonPair(
                skin,
                left = Triple("Preview", Icons.Default.Headphones) { viewModel.previewMashup() },
                right = Triple("Save mashup", Icons.Default.Save) { viewModel.stopStudioAudio(); viewModel.exportMashup(title) },
                enabled = task == null && !exportState.isRunning && recordingMs == null
            )
            StudioExportStatusCard(exportState, skin, onPlayExport) { viewModel.clearStudioExportMessage() }
        }
    }

    if (showPicker) {
        StudioTrackPickerDialog(librarySongs.filter { it.source == AudioSource.LOCAL || it.uri.startsWith("content") }, skin, onPick = { viewModel.addMashupLayer(it); showPicker = false }, onDismiss = { showPicker = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MashupLayerCard(
    index: Int,
    layer: StudioJobs.MashupLayer,
    skin: RetroSkin,
    onUpdate: (StudioJobs.MashupLayer) -> Unit,
    onRemove: () -> Unit,
    onListen: () -> Unit
) {
    var expanded by remember { mutableStateOf(index == 0) }
    val colors = listOf(Color(0xFF00E5FF), Color(0xFFFF4081), Color(0xFFFFB800), Color(0xFFB388FF))
    val c = colors[index % colors.size]
    val dur = layer.durationMs.coerceAtLeast(1000L)
    StudioCard(skin, accent = c) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(14.dp)).background(c),
                contentAlignment = Alignment.Center
            ) { Text("${index + 1}", color = Color.Black, fontWeight = FontWeight.Black) }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f).clickable { expanded = !expanded }) {
                Text(layer.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        if (layer.role == StudioJobs.LayerRole.VOICE) "Voice" else "Music",
                        "starts ${fmtMmSs(layer.offsetMs)}",
                        if (layer.tempo != 1.0) "tempo ×${String.format("%.2f", layer.tempo)}" else null
                    ).joinToString(" • "),
                    color = StudioSubtle, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onListen) {
                Icon(Icons.Default.Headphones, contentDescription = "Listen in the monitor", tint = c)
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Edit", tint = Color.White)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFFCA5A5))
            }
        }
        if (expanded) {
            StudioSegmented(
                options = listOf(StudioJobs.LayerRole.MUSIC to "Music", StudioJobs.LayerRole.VOICE to "Voice"),
                selected = layer.role, skin = skin
            ) { onUpdate(layer.copy(role = it)) }
            StudioSlider("Starts at", fmtMmSs(layer.offsetMs), layer.offsetMs / 1000f, 0f..180f, skin) {
                onUpdate(layer.copy(offsetMs = (it * 1000).toLong()))
            }
            Text("Use ${fmtMmSs(layer.inMs)} – ${fmtMmSs(if (layer.outMs > 0) layer.outMs else dur)} of the track", color = Color.White, fontSize = 13.sp)
            RangeSlider(
                value = (layer.inMs.toFloat() / dur)..((if (layer.outMs > 0) layer.outMs else dur).toFloat() / dur).coerceAtMost(1f),
                onValueChange = { r -> onUpdate(layer.copy(inMs = (r.start * dur).toLong(), outMs = (r.endInclusive * dur).toLong().coerceAtLeast((r.start * dur).toLong() + 1000))) },
                colors = SliderDefaults.colors(thumbColor = c, activeTrackColor = c, inactiveTrackColor = Color(0x40FFFFFF))
            )
            StudioSlider("Volume", "${(layer.gain * 100).toInt()}%", layer.gain, 0f..1.5f, skin) { onUpdate(layer.copy(gain = it)) }
            StudioSlider("Balance", when { layer.pan < -0.05f -> "L ${(-layer.pan * 100).toInt()}"; layer.pan > 0.05f -> "R ${(layer.pan * 100).toInt()}"; else -> "Centre" }, layer.pan, -1f..1f, skin) {
                onUpdate(layer.copy(pan = if (kotlin.math.abs(it) < 0.05f) 0f else it))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) { StudioToggle("Fade in", null, layer.fadeIn, skin) { onUpdate(layer.copy(fadeIn = it)) } }
                Box(modifier = Modifier.weight(1f)) { StudioToggle("Fade out", null, layer.fadeOut, skin) { onUpdate(layer.copy(fadeOut = it)) } }
            }
        }
    }
}
