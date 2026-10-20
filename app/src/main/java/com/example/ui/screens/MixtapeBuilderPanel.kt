package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.MixRenderer
import com.example.audio.OneShotSynth
import com.example.audio.RetroSoundEffects
import com.example.audio.StudioRenderEngine
import com.example.data.model.RetroSkin
import com.example.data.model.Song
import com.example.ui.components.SongArtThumbnail
import com.example.ui.viewmodel.MainViewModel

private fun fmtClock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return if (s >= 3600) String.format("%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
    else String.format("%d:%02d", s / 60, s % 60)
}

private val transitionIcons: Map<MixRenderer.Transition, ImageVector> = mapOf(
    MixRenderer.Transition.CROSSFADE to Icons.Default.SwapHoriz,
    MixRenderer.Transition.BASS_SWAP to Icons.Default.GraphicEq,
    MixRenderer.Transition.FILTER_SWEEP to Icons.Default.Tune,
    MixRenderer.Transition.ECHO_OUT to Icons.Default.SettingsInputAntenna,
    MixRenderer.Transition.REVERB_WASH to Icons.Default.Waves,
    MixRenderer.Transition.FLANGER to Icons.Default.Air,
    MixRenderer.Transition.BRAKE to Icons.Default.PowerSettingsNew,
    MixRenderer.Transition.LOOP_ROLL to Icons.Default.Loop,
    MixRenderer.Transition.CUT to Icons.Default.ContentCut
)

private val transitionHints: Map<MixRenderer.Transition, String> = mapOf(
    MixRenderer.Transition.CROSSFADE to "Smooth equal-power blend",
    MixRenderer.Transition.BASS_SWAP to "Highs blend, bass line hands over at the midpoint",
    MixRenderer.Transition.FILTER_SWEEP to "Outgoing track thins out, next one opens up",
    MixRenderer.Transition.ECHO_OUT to "Track fades into repeating echoes",
    MixRenderer.Transition.REVERB_WASH to "Track dissolves into a big hall reverb",
    MixRenderer.Transition.FLANGER to "Jet-plane whoosh while blending",
    MixRenderer.Transition.BRAKE to "Turntable power-off, then the next track drops",
    MixRenderer.Transition.LOOP_ROLL to "Last beat stutters ½ → ¼ → ⅛, then drops",
    MixRenderer.Transition.CUT to "Straight cut, no overlap"
)

/**
 * DJ Studio → MIXTAPE BUILDER + AUTO MIX.
 * Auto Mix analyses songs (tempo, beats, intro/outro, loudness) and fills the builder;
 * every transition can then be tweaked, previewed and rendered to one audio file.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixtapeBuilderPanel(
    viewModel: MainViewModel,
    librarySongs: List<Song>,
    playlists: List<com.example.data.model.Playlist>,
    skin: RetroSkin,
    exportState: MainViewModel.StudioExportState,
    onPlayExport: (Song) -> Unit
) {
    val plan by viewModel.mixPlan.collectAsState()
    val preview by viewModel.mixPreview.collectAsState()
    val task by viewModel.studioTask.collectAsState()

    var showPicker by remember { mutableStateOf(false) }
    var showMultiPicker by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var expandedKey by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(StudioRenderEngine.OutputFormat.AAC) }
    var style by remember { mutableStateOf(com.example.audio.AutoMixPlanner.Style.SMOOTH) }
    var reorder by remember { mutableStateOf(true) }

    val totalMs = remember(plan) {
        var total = 0L
        plan.forEachIndexed { i, item ->
            total += (item.outMs - item.inMs).coerceAtLeast(0L)
            if (i < plan.lastIndex && item.transition.usesLength) total -= (item.transitionSeconds * 1000).toLong()
        }
        total.coerceAtLeast(0L)
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StudioMonitor(viewModel, skin, MainViewModel.SECTION_MIX, "MIXTAPE MONITOR")

        // ===== AUTO MIX =====
        StudioCard(skin, title = "AUTO MIX", subtitle = "Pick songs and a style — tempo, beats, order and transitions are done for you", icon = Icons.Default.AutoAwesome) {
            Text("Style", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            StudioChipGrid(
                options = com.example.audio.AutoMixPlanner.Style.values().map { it to it.label },
                selected = style,
                skin = skin,
                columns = 2
            ) { style = it }
            Text(style.description, color = StudioSubtle, fontSize = 12.sp)
            StudioToggle("Best order", "Arrange songs by tempo for a smooth energy flow", reorder, skin) { reorder = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StudioButton("Pick songs", Icons.Default.LibraryMusic, skin, Modifier.weight(1f), primary = false, enabled = task == null) { showMultiPicker = true }
                StudioButton("A mixtape", Icons.Default.QueueMusic, skin, Modifier.weight(1f), primary = false, enabled = task == null) { showPlaylistPicker = true }
            }
            if (plan.size >= 2) {
                StudioButton("Auto Mix these ${plan.size} tracks", Icons.Default.AutoAwesome, skin, Modifier.fillMaxWidth(), enabled = task == null) {
                    viewModel.autoMix(plan.map { it.song }, style, reorder)
                }
            }
        }

        // ===== BUILDER =====
        StudioCard(
            skin,
            title = "MIXTAPE BUILDER",
            subtitle = if (plan.isEmpty()) "Or line up songs yourself and choose each transition" else "${plan.size} tracks • ${fmtClock(totalMs)}",
            icon = Icons.Default.QueueMusic
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StudioButton("Add track", Icons.Default.Add, skin, Modifier.weight(1f)) { showPicker = true }
                StudioButton("Clear", Icons.Default.DeleteSweep, skin, Modifier.weight(1f), primary = false, enabled = plan.isNotEmpty()) { viewModel.stopStudioAudio(); viewModel.clearMix() }
            }
            if (plan.isNotEmpty()) {
                StudioButton(
                    if (plan.size == 1) "Preview the track" else "Preview whole mix (${fmtClock(totalMs)})",
                    Icons.Default.Headphones, skin, Modifier.fillMaxWidth(), primary = false, enabled = task == null
                ) { viewModel.previewFullMix() }
                Text("Markers on the monitor's seek bar show where each transition starts.", color = StudioSubtle, fontSize = 12.sp)
            }
        }

        plan.forEachIndexed { index, item ->
            MixTrackCard(
                index = index,
                item = item,
                bpm = viewModel.cachedAnalysis(item.song.uri)?.takeIf { it.hasBeat }?.bpm,
                isFirst = index == 0,
                isLast = index == plan.lastIndex,
                expanded = expandedKey == item.key,
                skin = skin,
                onToggleExpand = { expandedKey = if (expandedKey == item.key) null else item.key },
                onMove = { d -> viewModel.moveInMix(item.key, d) },
                onRemove = { viewModel.removeFromMix(item.key) },
                onUpdate = { viewModel.updateMixItem(it) },
                onListen = { viewModel.previewOriginal(item.song, item.inMs) }
            )
            if (index < plan.lastIndex) {
                TransitionCard(
                    item = item,
                    skin = skin,
                    isRendering = false,
                    onUpdate = { viewModel.updateMixItem(it) },
                    onPreview = { viewModel.previewTransition(item.key) }
                )
            }
        }

        // ===== ONE-SHOT PADS =====
        StudioCard(skin, title = "ONE-SHOT DROPS", subtitle = "Tap to hear • add them to any transition above", icon = Icons.Default.GraphicEq) {
            val pads = listOf(
                MixRenderer.OneShot.VINYL_SCRATCH to Color(0xFF00E5FF),
                MixRenderer.OneShot.REWIND to Color(0xFFB388FF),
                MixRenderer.OneShot.KICK_808 to Color(0xFFFF9100),
                MixRenderer.OneShot.CLAP_808 to Color(0xFFFF4081)
            )
            pads.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { (shot, color) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(color.copy(alpha = 0.18f))
                                .border(1.5.dp, color, RoundedCornerShape(10.dp))
                                .clickable { viewModel.playOneShot(shot) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(shot.label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        // ===== EXPORT =====
        if (plan.isNotEmpty()) {
            StudioCard(skin, title = "RENDER MIXTAPE", subtitle = "Saved to Music/Mixtape Pro and your Library", icon = Icons.Default.Album) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, singleLine = true, label = { Text("Mixtape title") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = skin.accentColor),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist, onValueChange = { artist = it }, singleLine = true, label = { Text("DJ name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = skin.accentColor),
                    modifier = Modifier.fillMaxWidth()
                )
                StudioSegmented(
                    options = listOf(StudioRenderEngine.OutputFormat.AAC to "M4A (small)", StudioRenderEngine.OutputFormat.WAV to "WAV (lossless)"),
                    selected = format, skin = skin
                ) { format = it }
                StudioButton("Render & save (${fmtClock(totalMs)})", Icons.Default.Album, skin, Modifier.fillMaxWidth(), enabled = !exportState.isRunning) {
                    viewModel.stopStudioAudio()
                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CD_SPIN)
                    viewModel.exportMixtape(title, artist, format, "DJ Studio Mixtapes")
                }
                StudioExportStatusCard(exportState, skin, onPlayExport) { viewModel.clearStudioExportMessage() }
            }
        }
    }

    if (showPicker) {
        StudioTrackPickerDialog(
            songs = librarySongs.filter { it.durationMs > 5000 },
            skin = skin,
            onPick = { song -> viewModel.addToMix(song); showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
    if (showMultiPicker) {
        MultiSongPickerDialog(
            songs = librarySongs.filter { it.durationMs > 20_000 },
            skin = skin,
            confirmLabel = "Auto Mix",
            onConfirm = { picked -> showMultiPicker = false; viewModel.autoMix(picked, style, reorder) },
            onDismiss = { showMultiPicker = false }
        )
    }
    if (showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
            containerColor = Color(0xFF16181E),
            title = { Text("Auto Mix a mixtape", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(playlists.size) { i ->
                        val pl = playlists[i]
                        Text(
                            "📼 ${pl.name}",
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF222632))
                                .clickable { showPlaylistPicker = false; viewModel.autoMixPlaylist(pl, style, reorder) }
                                .padding(14.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPlaylistPicker = false }) { Text("Close", color = Color.White) } }
        )
    }
}

/** Pick several songs at once (Auto Mix, Mashup). */
@Composable
fun MultiSongPickerDialog(
    songs: List<Song>,
    skin: RetroSkin,
    confirmLabel: String,
    max: Int = 30,
    onConfirm: (List<Song>) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val picked = remember { mutableStateListOf<String>() }
    val filtered = remember(songs, query) {
        if (query.isBlank()) songs else songs.filter { it.title.contains(query, true) || it.artist.contains(query, true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16181E),
        title = { Text("Pick songs (${picked.size})", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text("Search…", color = Color(0xFF94A3B8)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = skin.accentColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(filtered.size) { i ->
                        val song = filtered[i]
                        val on = song.id in picked
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { if (on) picked.removeAll { it == song.id } else if (picked.size < max) picked.add(song.id) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(checked = on, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = skin.accentColor))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = picked.size >= 2, onClick = { onConfirm(songs.filter { it.id in picked }) }) {
                Text(confirmLabel, color = if (picked.size >= 2) skin.accentColor else Color(0xFF64748B), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MixTrackCard(
    index: Int,
    item: MainViewModel.MixPlanItem,
    bpm: Double?,
    isFirst: Boolean,
    isLast: Boolean,
    expanded: Boolean,
    skin: RetroSkin,
    onToggleExpand: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onUpdate: (MainViewModel.MixPlanItem) -> Unit,
    onListen: () -> Unit
) {
    val dur = item.song.durationMs.coerceAtLeast(1000L)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xDD181B22))
            .border(1.dp, skin.chassisBorderColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).background(skin.accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text("${index + 1}", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            SongArtThumbnail(song = item.song, size = 44.dp, accent = skin.accentColor)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f).clickable(onClick = onToggleExpand)) {
                Text(item.song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(item.song.artist, bpm?.let { "${it.toInt()} BPM" }, "${fmtClock(item.inMs)}–${fmtClock(item.outMs)}").joinToString(" • "),
                    color = Color(0xFFB4BFCE), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onListen, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Headphones, contentDescription = "Listen in the monitor", tint = skin.accentColor)
            }
            IconButton(onClick = { onMove(-1) }, enabled = !isFirst, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", tint = if (isFirst) Color(0xFF475569) else Color.White)
            }
            IconButton(onClick = { onMove(1) }, enabled = !isLast, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", tint = if (isLast) Color(0xFF475569) else Color.White)
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFFCA5A5))
            }
        }

        // Trim / gain editor
        TextButton(onClick = onToggleExpand, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ContentCut, contentDescription = null, tint = skin.accentColor, modifier = Modifier.size(16.dp))
            Text(if (expanded) " Done" else " Trim & level", color = skin.accentColor, fontSize = 12.sp)
        }
        if (expanded) {
            val range = (item.inMs.toFloat() / dur)..(item.outMs.toFloat() / dur).coerceAtMost(1f)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("IN ${fmtClock(item.inMs)}", color = skin.accentColor, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text("OUT ${fmtClock(item.outMs)}", color = skin.accentColor, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            RangeSlider(
                value = range,
                onValueChange = { r ->
                    val minLen = 10_000f / dur // keep at least 10 s
                    var s = r.start
                    var e = r.endInclusive
                    if (e - s < minLen) { if (s != range.start) s = (e - minLen).coerceAtLeast(0f) else e = (s + minLen).coerceAtMost(1f) }
                    onUpdate(item.copy(inMs = (s * dur).toLong(), outMs = (e * dur).toLong()))
                },
                colors = SliderDefaults.colors(thumbColor = skin.accentColor, activeTrackColor = skin.accentColor, inactiveTrackColor = Color(0x40FFFFFF))
            )
            Text("Level ${(item.gain * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
            Slider(
                value = item.gain,
                onValueChange = { onUpdate(item.copy(gain = it)) },
                valueRange = 0.3f..1.5f,
                colors = SliderDefaults.colors(thumbColor = skin.accentColor, activeTrackColor = skin.accentColor)
            )
        }
    }
}

@Composable
private fun TransitionCard(
    item: MainViewModel.MixPlanItem,
    skin: RetroSkin,
    isRendering: Boolean,
    onUpdate: (MainViewModel.MixPlanItem) -> Unit,
    onPreview: () -> Unit
) {
    StudioCard(skin, modifier = Modifier.padding(horizontal = 12.dp), accent = skin.secondaryAccentColor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(transitionIcons[item.transition] ?: Icons.Default.SwapHoriz, contentDescription = null, tint = skin.secondaryAccentColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("TRANSITION • ${item.transition.label}", color = skin.secondaryAccentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Text(transitionHints[item.transition] ?: "", color = StudioSubtle, fontSize = 12.sp, maxLines = 2)
            }
        }
        StudioChipGrid(
            options = MixRenderer.Transition.values().map { it to it.label },
            selected = item.transition,
            skin = skin,
            columns = 3,
            accent = skin.secondaryAccentColor
        ) { t ->
            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
            onUpdate(item.copy(transition = t, transitionSeconds = if (t.usesLength && item.transition.usesLength) item.transitionSeconds else t.defaultSeconds.toFloat()))
        }
        if (item.transition.usesLength) {
            StudioSlider("Length", "${item.transitionSeconds.toInt()} s", item.transitionSeconds, 2f..16f, skin, steps = 13) {
                onUpdate(item.copy(transitionSeconds = it.coerceIn(2f, 16f)))
            }
        }
        Text("Drop at the change", color = Color.White, fontSize = 13.sp)
        StudioChipGrid(
            options = MixRenderer.OneShot.values().map { it to it.label },
            selected = item.drop,
            skin = skin,
            columns = 3
        ) { onUpdate(item.copy(drop = it)) }
        StudioButton(
            if (isRendering) "Preparing preview…" else "Preview transition",
            Icons.Default.Headphones, skin, Modifier.fillMaxWidth(), primary = false, enabled = !isRendering, onClick = onPreview
        )
    }
}
