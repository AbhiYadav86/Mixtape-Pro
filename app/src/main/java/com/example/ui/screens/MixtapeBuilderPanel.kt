package com.example.ui.screens

import android.media.MediaPlayer
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
 * DJ Studio → MIXTAPE BUILDER.
 * Line up tracks, trim their in/out points, pick a DJ transition and an optional one-shot drop
 * between each pair, audition transitions, then render the whole mixtape to a real audio file.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixtapeBuilderPanel(
    librarySongs: List<Song>,
    plan: List<MainViewModel.MixPlanItem>,
    preview: MainViewModel.MixPreviewState,
    exportState: MainViewModel.StudioExportState,
    skin: RetroSkin,
    onAdd: (Song) -> Unit,
    onRemove: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onUpdate: (MainViewModel.MixPlanItem) -> Unit,
    onClear: () -> Unit,
    onPreview: (String) -> Unit,
    onPreviewDone: () -> Unit,
    onExport: (title: String, artist: String, format: StudioRenderEngine.OutputFormat) -> Unit,
    onPlayExport: (Song) -> Unit,
    onDismissExport: () -> Unit,
    onBeforeAudition: () -> Unit
) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    var expandedKey by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(StudioRenderEngine.OutputFormat.AAC) }

    // ---- Transition preview playback ----
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingKey by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) { onDispose { try { player?.release() } catch (_: Exception) {} } }
    LaunchedEffect(preview.readyKey, preview.file) {
        val file = preview.file
        val key = preview.readyKey
        if (file != null && key != null) {
            try {
                player?.release()
                val mp = MediaPlayer()
                mp.setDataSource(file)
                mp.prepare()
                mp.setOnCompletionListener { playingKey = null }
                mp.start()
                player = mp
                playingKey = key
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't play the preview", Toast.LENGTH_SHORT).show()
            }
            onPreviewDone()
        }
    }
    fun stopPreview() {
        try { player?.pause() } catch (_: Exception) {}
        playingKey = null
    }

    // Total running time (transitions overlap, brake/roll/cut don't)
    val totalMs = remember(plan) {
        var total = 0L
        plan.forEachIndexed { i, item ->
            total += (item.outMs - item.inMs).coerceAtLeast(0L)
            if (i < plan.lastIndex && item.transition.usesLength) {
                total -= (item.transitionSeconds * 1000).toLong()
            }
        }
        total.coerceAtLeast(0L)
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // ===== Header =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF10141C))
                .border(1.5.dp, skin.accentColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = skin.accentColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("MIXTAPE BUILDER", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1)
                    Text(
                        if (plan.isEmpty()) "Line up songs, pick DJ transitions, render one mix"
                        else "${plan.size} tracks • ${fmtClock(totalMs)}",
                        color = Color(0xFFB4BFCE), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (plan.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear mix", tint = Color(0xFF94A3B8))
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { showPicker = true },
                colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ADD TRACK", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        if (plan.isEmpty()) {
            Text(
                "Tip: add 2 or more songs. Between each pair you can choose a DJ transition — crossfade, " +
                    "bass swap, filter sweep, echo out, reverb wash, flanger, brake or loop roll — and drop " +
                    "in a scratch, rewind or 808 hit.",
                color = Color(0xFFB4BFCE),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // ===== Track list with transitions in between =====
        plan.forEachIndexed { index, item ->
            MixTrackCard(
                index = index,
                item = item,
                isFirst = index == 0,
                isLast = index == plan.lastIndex,
                expanded = expandedKey == item.key,
                skin = skin,
                onToggleExpand = { expandedKey = if (expandedKey == item.key) null else item.key },
                onMove = { d -> onMove(item.key, d) },
                onRemove = { onRemove(item.key) },
                onUpdate = onUpdate
            )
            if (index < plan.lastIndex) {
                TransitionCard(
                    item = item,
                    skin = skin,
                    isRendering = preview.renderingKey == item.key,
                    isPlaying = playingKey == item.key,
                    onUpdate = onUpdate,
                    onPreview = {
                        if (playingKey == item.key) stopPreview() else {
                            onBeforeAudition()
                            onPreview(item.key)
                        }
                    }
                )
            }
        }

        // ===== One-shot audition pads =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF10141C))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Text("ONE-SHOT DROPS", color = skin.accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Tap to hear • add them to a transition above", color = Color(0xFFB4BFCE), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            val pads = listOf(
                MixRenderer.OneShot.VINYL_SCRATCH to Color(0xFF00E5FF),
                MixRenderer.OneShot.REWIND to Color(0xFFB388FF),
                MixRenderer.OneShot.KICK_808 to Color(0xFFFF9100),
                MixRenderer.OneShot.CLAP_808 to Color(0xFFFF4081)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                pads.forEach { (shot, color) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color.copy(alpha = 0.18f))
                            .border(1.5.dp, color, RoundedCornerShape(8.dp))
                            .clickable {
                                onBeforeAudition()
                                OneShotSynth.play(shot)
                            }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            shot.label,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // ===== Export =====
        if (plan.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF10141C))
                    .border(1.5.dp, skin.accentColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text("RENDER MIXTAPE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text("Mixtape title") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = skin.accentColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    singleLine = true,
                    label = { Text("DJ name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = skin.accentColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        StudioRenderEngine.OutputFormat.AAC to "M4A (small)",
                        StudioRenderEngine.OutputFormat.WAV to "WAV (lossless)"
                    ).forEach { (fmt, label) ->
                        val sel = format == fmt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (sel) skin.accentColor else Color(0xFF1E222D))
                                .clickable { format = fmt }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (sel) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        stopPreview()
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CD_SPIN)
                        onExport(title, artist, format)
                    },
                    enabled = !exportState.isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Album, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RENDER & SAVE (${fmtClock(totalMs)})", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                }
                StudioExportStatusCard(exportState, skin, onPlayExport, onDismissExport)
            }
        }
    }

    if (showPicker) {
        StudioTrackPickerDialog(
            songs = librarySongs.filter { it.durationMs > 5000 },
            skin = skin,
            onPick = { song -> onAdd(song); showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MixTrackCard(
    index: Int,
    item: MainViewModel.MixPlanItem,
    isFirst: Boolean,
    isLast: Boolean,
    expanded: Boolean,
    skin: RetroSkin,
    onToggleExpand: () -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onUpdate: (MainViewModel.MixPlanItem) -> Unit
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
                    "${item.song.artist} • ${fmtClock(item.inMs)}–${fmtClock(item.outMs)}",
                    color = Color(0xFFB4BFCE), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
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
    isPlaying: Boolean,
    onUpdate: (MainViewModel.MixPlanItem) -> Unit,
    onPreview: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0B0E14))
            .border(1.dp, skin.secondaryAccentColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SouthEast, contentDescription = null, tint = skin.secondaryAccentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("TRANSITION", color = skin.secondaryAccentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = onPreview,
                enabled = !isRendering,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.heightIn(min = 34.dp)
            ) {
                if (isRendering) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                    Text(" Preparing…", color = Color.White, fontSize = 12.sp)
                } else {
                    Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.Headphones, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(if (isPlaying) " Stop" else " Preview", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MixRenderer.Transition.values().forEach { t ->
                val sel = item.transition == t
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (sel) skin.secondaryAccentColor else Color(0xFF1E222D))
                        .border(1.dp, if (sel) skin.secondaryAccentColor else Color(0x40FFFFFF), RoundedCornerShape(16.dp))
                        .clickable {
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                            onUpdate(item.copy(transition = t, transitionSeconds = if (t.usesLength) item.transitionSeconds.takeIf { item.transition.usesLength } ?: t.defaultSeconds.toFloat() else t.defaultSeconds.toFloat()))
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(transitionIcons[t] ?: Icons.Default.SwapHoriz, contentDescription = null, tint = if (sel) Color.Black else Color.White, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(t.label, color = if (sel) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
        Text(transitionHints[item.transition] ?: "", color = Color(0xFFB4BFCE), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        if (item.transition.usesLength) {
            Text("Length ${item.transitionSeconds.toInt()} s", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            Slider(
                value = item.transitionSeconds,
                onValueChange = { onUpdate(item.copy(transitionSeconds = it.coerceIn(2f, 16f))) },
                valueRange = 2f..16f,
                steps = 13,
                colors = SliderDefaults.colors(thumbColor = skin.secondaryAccentColor, activeTrackColor = skin.secondaryAccentColor)
            )
        }
        Text("Drop", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MixRenderer.OneShot.values().forEach { shot ->
                val sel = item.drop == shot
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (sel) skin.accentColor else Color(0xFF1E222D))
                        .clickable { onUpdate(item.copy(drop = shot)) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(shot.label, color = if (sel) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}
