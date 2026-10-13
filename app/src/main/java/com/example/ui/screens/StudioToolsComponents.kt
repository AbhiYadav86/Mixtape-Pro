package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.StudioRenderEngine
import com.example.data.model.RetroSkin
import com.example.data.model.Song
import com.example.ui.components.SongArtThumbnail
import com.example.ui.viewmodel.MainViewModel

// =================================================================================================
// Shared helpers
// =================================================================================================

private fun fmtMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val tenths = (ms % 1000) / 100
    return String.format("%d:%02d.%d", totalSec / 60, totalSec % 60, tenths)
}

/** Open Android's share sheet (Google Drive, Dropbox, Gmail, WhatsApp, Bluetooth…) for an exported file. */
fun shareAudioFile(context: Context, uriString: String, title: String) {
    val uri = Uri.parse(uriString)
    if (uri.scheme != "content") {
        Toast.makeText(context, "Sharing needs Android 10+ (file saved in the app's Music folder)", Toast.LENGTH_LONG).show()
        return
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(send, "Upload / share \"$title\"").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app available to share audio", Toast.LENGTH_SHORT).show()
    }
}

/** Set a saved ringtone as the phone's default ringtone (asks for the "modify system settings" permission first). */
fun setAsPhoneRingtone(context: Context, uriString: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
        Toast.makeText(context, "Allow \"Modify system settings\" for Mixtape Pro, then tap again", Toast.LENGTH_LONG).show()
        try {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {}
        return
    }
    try {
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, Uri.parse(uriString))
        Toast.makeText(context, "Ringtone set!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Couldn't set ringtone: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun PanelCard(skin: RetroSkin, title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF10141C))
            .border(1.5.dp, skin.accentColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = skin.accentColor, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, fontFamily = FontFamily.SansSerif)
                Text(subtitle, color = Color(0xFFB4BFCE), fontSize = 11.5.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SelectedTrackRow(song: Song?, skin: RetroSkin, onPick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1F2A))
            .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(8.dp))
            .clickable(onClick = onPick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (song != null) {
            SongArtThumbnail(song = song, size = 44.dp, accent = skin.accentColor)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = Color(0xFFB4BFCE), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = skin.accentColor)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Choose a track from your library", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        }
        Text("CHANGE", color = skin.accentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun StudioTrackPickerDialog(
    songs: List<Song>,
    skin: RetroSkin,
    onPick: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(songs, query) {
        if (query.isBlank()) songs else songs.filter {
            it.title.contains(query, true) || it.artist.contains(query, true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16181E),
        title = { Text("Pick a track", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search…", color = Color(0xFF94A3B8)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = skin.accentColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Text("No tracks in your library yet.", color = Color(0xFFB4BFCE))
                }
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(filtered, key = { it.id }) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onPick(song) }
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SongArtThumbnail(song = song, size = 36.dp, accent = skin.accentColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = Color.White) } }
    )
}

/** Progress / result card shown under every DJ Studio export button. */
@Composable
fun StudioExportStatusCard(
    state: MainViewModel.StudioExportState,
    skin: RetroSkin,
    onPlay: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    if (!state.isRunning && state.lastExportUri == null && state.error == null) return

    Spacer(modifier = Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (state.error != null) Color(0xFF2A1215) else Color(0xFF0F2615))
            .border(1.5.dp, if (state.error != null) Color(0xFFEF4444) else Color(0xFF00FF66), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        when {
            state.isRunning -> {
                Text("${state.label}… ${(state.progress * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { state.progress },
                    color = skin.accentColor,
                    trackColor = Color(0x33FFFFFF),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            state.error != null -> {
                Text("Export failed", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(state.error, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                TextButton(onClick = onDismiss) { Text("OK", color = Color.White) }
            }
            else -> {
                Text("✓ Saved: ${state.lastExportName ?: ""}", color = Color(0xFF00FF66), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    if (state.isRingtone) "Saved in Ringtones/Mixtape Pro" else "Saved in Music/Mixtape Pro and added to your Library",
                    color = Color(0xFFE2E8F0), fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    val song = state.lastExportSong
                    if (song != null) {
                        OutlinedButton(onClick = { onPlay(song) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(" Play", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    if (state.isRingtone) {
                        OutlinedButton(onClick = { setAsPhoneRingtone(context, state.lastExportUri ?: "") }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
                            Icon(Icons.Default.RingVolume, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(" Set", color = Color.White, fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = { shareAudioFile(context, state.lastExportUri ?: "", state.lastExportName ?: "Mixtape Pro export") },
                        colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Text(" Cloud", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =================================================================================================
// Ringtone Maker
// =================================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneMakerPanel(
    songs: List<Song>,
    initialSong: Song?,
    skin: RetroSkin,
    exportState: MainViewModel.StudioExportState,
    onBeforePreview: () -> Unit,
    onExport: (song: Song, startMs: Long, endMs: Long, fadeInMs: Long, fadeOutMs: Long, title: String) -> Unit,
    onPlayExport: (Song) -> Unit,
    onDismissExport: () -> Unit
) {
    var song by remember { mutableStateOf(initialSong ?: songs.firstOrNull()) }
    var showPicker by remember { mutableStateOf(false) }
    val durationMs = (song?.durationMs ?: 0L).coerceAtLeast(1000L)
    var range by remember(song?.id) { mutableStateOf(0f..(30_000f / durationMs).coerceAtMost(1f)) }
    var fadeIn by remember { mutableStateOf(true) }
    var fadeOut by remember { mutableStateOf(true) }
    var title by remember(song?.id) { mutableStateOf(song?.let { "${it.title} (Ringtone)" } ?: "") }

    // Preview player
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var previewing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    DisposableEffect(Unit) {
        onDispose {
            try { previewPlayer?.release() } catch (_: Exception) {}
        }
    }
    val startMs = (range.start * durationMs).toLong()
    val endMs = (range.endInclusive * durationMs).toLong()
    LaunchedEffect(previewing, startMs, endMs) {
        if (previewing) {
            while (previewing) {
                kotlinx.coroutines.delay(200)
                val p = previewPlayer ?: break
                val pos = try { p.currentPosition.toLong() } catch (_: Exception) { break }
                if (pos >= endMs) {
                    try { p.pause() } catch (_: Exception) {}
                    previewing = false
                }
            }
        }
    }

    PanelCard(skin, "RINGTONE MAKER", "Cut any song into a ringtone or alert sound", Icons.Default.RingVolume) {
        SelectedTrackRow(song, skin) { showPicker = true }
        Spacer(modifier = Modifier.height(12.dp))

        if (song != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("START ${fmtMs(startMs)}", color = skin.accentColor, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("LENGTH ${fmtMs(endMs - startMs)}", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text("END ${fmtMs(endMs)}", color = skin.accentColor, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            RangeSlider(
                value = range,
                onValueChange = { r ->
                    // Keep ringtones between 1 and 60 seconds
                    val minLen = 1000f / durationMs
                    val maxLen = 60_000f / durationMs
                    var s = r.start
                    var e = r.endInclusive
                    if (e - s < minLen) e = (s + minLen).coerceAtMost(1f)
                    if (e - s > maxLen) {
                        if (s != range.start) e = s + maxLen else s = e - maxLen
                    }
                    range = s.coerceIn(0f, 1f)..e.coerceIn(0f, 1f)
                },
                colors = SliderDefaults.colors(
                    thumbColor = skin.accentColor,
                    activeTrackColor = skin.accentColor,
                    inactiveTrackColor = Color(0x40FFFFFF)
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = fadeIn, onCheckedChange = { fadeIn = it }, colors = CheckboxDefaults.colors(checkedColor = skin.accentColor))
                Text("Fade in", color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Checkbox(checked = fadeOut, onCheckedChange = { fadeOut = it }, colors = CheckboxDefaults.colors(checkedColor = skin.accentColor))
                Text("Fade out", color = Color.White, fontSize = 13.sp)
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                label = { Text("Ringtone name") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = skin.accentColor),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        val s = song ?: return@OutlinedButton
                        if (previewing) {
                            try { previewPlayer?.pause() } catch (_: Exception) {}
                            previewing = false
                        } else {
                            onBeforePreview()
                            try {
                                previewPlayer?.release()
                                val mp = MediaPlayer()
                                mp.setDataSource(context, Uri.parse(s.uri))
                                mp.prepare()
                                mp.seekTo(startMs.toInt())
                                mp.start()
                                previewPlayer = mp
                                previewing = true
                            } catch (e: Exception) {
                                Toast.makeText(context, "Can't preview this file", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(if (previewing) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Text(if (previewing) " Stop" else " Preview", color = Color.White)
                }
                Button(
                    onClick = {
                        val s = song ?: return@Button
                        try { previewPlayer?.pause() } catch (_: Exception) {}
                        previewing = false
                        onExport(s, startMs, endMs, if (fadeIn) 600L else 0L, if (fadeOut) 1500L else 0L, title)
                    },
                    enabled = !exportState.isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                    Text(" Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        StudioExportStatusCard(exportState, skin, onPlayExport, onDismissExport)
    }

    if (showPicker) {
        StudioTrackPickerDialog(songs, skin, onPick = { song = it; showPicker = false }, onDismiss = { showPicker = false })
    }
}

// =================================================================================================
// Vocal Remover
// =================================================================================================

@Composable
fun VocalRemoverPanel(
    songs: List<Song>,
    initialSong: Song?,
    skin: RetroSkin,
    exportState: MainViewModel.StudioExportState,
    onExport: (song: Song, strength: Float, bassKeepHz: Float, format: StudioRenderEngine.OutputFormat) -> Unit,
    onPlayExport: (Song) -> Unit,
    onDismissExport: () -> Unit
) {
    var song by remember { mutableStateOf(initialSong ?: songs.firstOrNull()) }
    var showPicker by remember { mutableStateOf(false) }
    var strength by remember { mutableFloatStateOf(0.9f) }
    var bassKeep by remember { mutableFloatStateOf(160f) }
    var format by remember { mutableStateOf(StudioRenderEngine.OutputFormat.WAV) }

    PanelCard(skin, "VOCAL REMOVER", "Strip lead vocals for karaoke & DJ instrumentals", Icons.Default.MicOff) {
        SelectedTrackRow(song, skin) { showPicker = true }
        Spacer(modifier = Modifier.height(12.dp))

        Text("Vocal removal: ${(strength * 100).toInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Slider(
            value = strength,
            onValueChange = { strength = it },
            colors = SliderDefaults.colors(thumbColor = skin.accentColor, activeTrackColor = skin.accentColor)
        )
        Text("Keep bass below ${bassKeep.toInt()} Hz", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Slider(
            value = bassKeep,
            onValueChange = { bassKeep = it },
            valueRange = 60f..300f,
            colors = SliderDefaults.colors(thumbColor = skin.accentColor, activeTrackColor = skin.accentColor)
        )
        Text(
            "Works by cancelling sound panned to the centre of a stereo mix, where lead vocals usually sit. " +
                "Results vary by song; mono recordings can't be separated.",
            color = Color(0xFFB4BFCE),
            fontSize = 11.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(StudioRenderEngine.OutputFormat.WAV to "WAV (lossless)", StudioRenderEngine.OutputFormat.AAC to "M4A (small)").forEach { (fmt, label) ->
                val sel = format == fmt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (sel) skin.accentColor else Color(0xFF1E222D))
                        .clickable { format = fmt }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (sel) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = { song?.let { onExport(it, strength, bassKeep, format) } },
            enabled = song != null && !exportState.isRunning,
            colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.MicOff, contentDescription = null, tint = Color.Black)
            Text(" Remove vocals & save", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        StudioExportStatusCard(exportState, skin, onPlayExport, onDismissExport)
    }

    if (showPicker) {
        StudioTrackPickerDialog(songs, skin, onPick = { song = it; showPicker = false }, onDismiss = { showPicker = false })
    }
}
