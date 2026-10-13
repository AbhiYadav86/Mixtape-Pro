package com.example.ui.screens

import android.media.MediaRecorder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RetroSoundEffects
import com.example.data.model.RetroSkin
import com.example.data.model.Song
import java.util.UUID
import kotlin.math.*

enum class ClipSourceType(val label: String, val badgeColor: Long) {
    LIBRARY_TRACK("TRACK", 0xFF0284C7),
    MIC_RECORDING("MIC VOCAL", 0xFFEF4444),
    SOUND_FX("STUDIO SFX", 0xFFF59E0B)
}

data class ClipSegment(
    val startPercent: Float, // 0.0 to 1.0
    val endPercent: Float
)

data class SplicerClip(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val artist: String,
    val durationSec: Int = 180,
    val uri: String? = null,
    val sourceType: ClipSourceType = ClipSourceType.LIBRARY_TRACK,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val colorHex: Long = 0xFF00E5FF,
    // Portion slicing & active segments
    val activeSegments: List<ClipSegment> = listOf(ClipSegment(0.0f, 1.0f)),
    val selectionStart: Float = 0.15f,
    val selectionEnd: Float = 0.45f,
    // Background Noise Removal DSP settings
    val isDenoiseEnabled: Boolean = false,
    val noiseGateThresholdDb: Float = -40.0f, // -60dB to -12dB
    val noiseReductionAmount: Float = 0.75f, // 0.0 to 1.0 (75% background noise reduction)
    val hissCutoffKhz: Float = 8.5f // 4kHz to 18kHz
)

/**
 * Interactive Multi-Segment Waveform & Portion Slicer.
 * Visualizes active audio segments, allows marking an in-out portion,
 * scrubbing the playhead, and executing precise cuts or deletions.
 */
@Composable
fun SplicerWaveformSegmentView(
    clip: SplicerClip,
    playheadProgress: Float,
    skin: RetroSkin,
    onSelectionChange: (Float, Float) -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 80.dp
) {
    val clipColor = Color(clip.colorHex)
    val numBars = 80
    val amplitudes = remember(clip.id, numBars) {
        FloatArray(numBars) { i ->
            val phase = i * 0.35 + (clip.id.hashCode() % 100) * 0.1
            val wave = abs(sin(phase) * 0.5 + cos(phase * 1.8) * 0.3 + sin(phase * 0.4) * 0.2).toFloat()
            wave.coerceIn(0.12f, 0.95f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF070911))
            .border(1.dp, clipColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .pointerInput(clip) {
                detectTapGestures { offset ->
                    val normX = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(normX)
                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.NEEDLE_DROP)
                }
            }
            .pointerInput(clip) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val normX = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek(normX)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h * 0.5f

            // 1. Draw Inactive / Deleted regions background
            drawRect(color = Color(0xFF0E121C), size = Size(w, h))

            // 2. Highlight Active Segments
            for (seg in clip.activeSegments) {
                val segX = seg.startPercent * w
                val segW = (seg.endPercent - seg.startPercent) * w
                drawRect(
                    color = Color(0xFF141926),
                    topLeft = Offset(segX, 0f),
                    size = Size(segW, h)
                )
            }

            // 3. Highlight Selected Portion to Remove (in red/amber)
            val selX = clip.selectionStart.coerceAtMost(clip.selectionEnd) * w
            val selW = abs(clip.selectionEnd - clip.selectionStart) * w
            drawRect(
                color = Color(0xFFFF5252).copy(alpha = 0.22f),
                topLeft = Offset(selX, 0f),
                size = Size(selW, h)
            )
            // Hatch borders for selected portion
            drawLine(Color(0xFFFF5252), Offset(selX, 0f), Offset(selX, h), strokeWidth = 2f)
            drawLine(Color(0xFFFF5252), Offset(selX + selW, 0f), Offset(selX + selW, h), strokeWidth = 2f)

            // 4. Draw Waveform Bars
            val barW = w / numBars
            for (i in 0 until numBars) {
                val barProg = i.toFloat() / numBars
                val inActiveSegment = clip.activeSegments.any { seg -> barProg in seg.startPercent..seg.endPercent }
                val inSelection = barProg in min(clip.selectionStart, clip.selectionEnd)..max(clip.selectionStart, clip.selectionEnd)

                val amp = amplitudes[i] * (h * 0.42f)
                val bx = i * barW + barW * 0.5f

                val barColor = when {
                    inSelection -> Color(0xFFFF5252)
                    inActiveSegment -> clipColor
                    else -> Color(0xFF263045).copy(alpha = 0.4f)
                }

                drawLine(
                    color = barColor,
                    start = Offset(bx, midY - amp),
                    end = Offset(bx, midY + amp),
                    strokeWidth = barW * 0.72f
                )
            }

            // 5. Playhead indicator
            val px = playheadProgress.coerceIn(0f, 1f) * w
            drawLine(
                color = Color.White,
                start = Offset(px, 0f),
                end = Offset(px, h),
                strokeWidth = 2.5f
            )
            // Playhead handle
            val arrow = Path().apply {
                moveTo(px, 0f)
                lineTo(px + 6f, 6f)
                lineTo(px, 12f)
                lineTo(px - 6f, 6f)
                close()
            }
            drawPath(arrow, color = Color.White)
        }

        // Overlay: In/Out Selection Readout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val startSec = (clip.selectionStart * clip.durationSec).toInt()
            val endSec = (clip.selectionEnd * clip.durationSec).toInt()
            Text(
                text = "PORTION IN: ${String.format("%02d:%02d", startSec / 60, startSec % 60)}",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF5252)
            )
            Text(
                text = "SELECTED: ${abs(endSec - startSec)}s",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "PORTION OUT: ${String.format("%02d:%02d", endSec / 60, endSec % 60)}",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF5252)
            )
        }
    }
}

/**
 * DSP Background Noise Reduction Panel.
 * Provides real-time noise gate threshold, background noise reduction percentage,
 * high-frequency hiss attenuation, and A/B auditioning.
 */
@Composable
fun NoiseReductionPanel(
    clip: SplicerClip,
    skin: RetroSkin,
    onUpdateDenoise: (
        enabled: Boolean,
        thresholdDb: Float,
        reductionAmount: Float,
        hissKhz: Float
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C101A)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (clip.isDenoiseEnabled) Color(0xFF10B981) else Color(0xFF263045)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Title and Denoise Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (clip.isDenoiseEnabled) Color(0xFF10B981) else Color(0xFF64748B))
                    )
                    Text(
                        text = "DSP BACKGROUND NOISE REMOVER",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (clip.isDenoiseEnabled) Color(0xFF10B981) else Color.White
                    )
                }

                // Denoise Power Switch
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (clip.isDenoiseEnabled) "CLEANED" else "RAW NOISY",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (clip.isDenoiseEnabled) Color(0xFF10B981) else Color(0xFF94A3B8)
                    )
                    Switch(
                        checked = clip.isDenoiseEnabled,
                        onCheckedChange = {
                            onUpdateDenoise(it, clip.noiseGateThresholdDb, clip.noiseReductionAmount, clip.hissCutoffKhz)
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                        },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            // Quick Preset Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Pair("MILD ROOM", -48f to 0.50f),
                    Pair("FAN / HVAC", -38f to 0.75f),
                    Pair("VOICE ISOLATE", -32f to 0.90f),
                    Pair("AGGRESSIVE GATE", -24f to 1.00f)
                ).forEach { (name, params) ->
                    val (thresh, red) = params
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF161E2E))
                            .clickable {
                                onUpdateDenoise(true, thresh, red, clip.hissCutoffKhz)
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                            }
                            .padding(vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(name, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    }
                }
            }

            // Noise Gate Threshold Slider
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Noise Floor Threshold:", fontSize = 9.sp, color = Color(0xFFCBD5E1))
                    Text(
                        text = "${clip.noiseGateThresholdDb.toInt()} dB",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
                Slider(
                    value = clip.noiseGateThresholdDb,
                    onValueChange = { onUpdateDenoise(clip.isDenoiseEnabled, it, clip.noiseReductionAmount, clip.hissCutoffKhz) },
                    valueRange = -60f..-12f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF10B981), activeTrackColor = Color(0xFF10B981))
                )
            }

            // Noise Reduction Amount & Hiss Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reduction Percentage
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Suppression:", fontSize = 8.5.sp, color = Color(0xFFCBD5E1))
                        Text(
                            text = "${(clip.noiseReductionAmount * 100).toInt()}%",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Slider(
                        value = clip.noiseReductionAmount,
                        onValueChange = { onUpdateDenoise(clip.isDenoiseEnabled, clip.noiseGateThresholdDb, it, clip.hissCutoffKhz) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                    )
                }

                // High-Frequency Hiss Roll-Off
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Hiss Cutoff:", fontSize = 8.5.sp, color = Color(0xFFCBD5E1))
                        Text(
                            text = "${String.format("%.1f", clip.hissCutoffKhz)} kHz",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA78BFA)
                        )
                    }
                    Slider(
                        value = clip.hissCutoffKhz,
                        onValueChange = { onUpdateDenoise(clip.isDenoiseEnabled, clip.noiseGateThresholdDb, clip.noiseReductionAmount, it) },
                        valueRange = 4f..18f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFA78BFA), activeTrackColor = Color(0xFFA78BFA))
                    )
                }
            }
        }
    }
}

/**
 * Add Sound Dialog:
 * Allows user to import from Library, Record Mic Audio live, or pick Studio Sound FX stems!
 */
@Composable
fun AddSoundDialog(
    allSongs: List<Song>,
    onAddTrack: (Song) -> Unit,
    onAddRecordedVocal: (title: String, durationSec: Int) -> Unit,
    onAddSoundFx: (name: String, durationSec: Int, colorHex: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Library, 1: Record Mic, 2: Studio SFX
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var recTitle by remember { mutableStateOf("Mic Vocal Clip") }

    // Recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                recordingSeconds++
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ADD SOUND / AUDIO CLIP",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Source Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF141926))
                        .padding(2.dp)
                ) {
                    listOf("MUSIC TRACK", "🎙️ RECORD MIC", "⚡ STUDIO SFX").forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) Color(0xFF0284C7) else Color.Transparent)
                                .clickable {
                                    selectedTab = index
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        // Track Library
                        Text("Select a track to add to splice sequence:", fontSize = 9.5.sp, color = Color(0xFFCBD5E1))
                        LazyColumn(modifier = Modifier.height(180.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(allSongs) { song ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1E2638))
                                        .clickable {
                                            onAddTrack(song)
                                            onDismiss()
                                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.NEEDLE_DROP)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Column {
                                        Text(song.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(song.artist, fontSize = 9.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Microphone Recorder
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF141A28))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("RECORD AUDIO VIA MICROPHONE", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))

                            OutlinedTextField(
                                value = recTitle,
                                onValueChange = { recTitle = it },
                                label = { Text("Clip Name", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Timer & VU meter
                            Text(
                                text = String.format("%02d:%02d", recordingSeconds / 60, recordingSeconds % 60),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isRecording) Color(0xFFEF4444) else Color.White
                            )

                            Button(
                                onClick = {
                                    if (isRecording) {
                                        // Finish Recording
                                        isRecording = false
                                        val dur = max(3, recordingSeconds)
                                        onAddRecordedVocal(recTitle, dur)
                                        onDismiss()
                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                                    } else {
                                        // Start Recording
                                        recordingSeconds = 0
                                        isRecording = true
                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.VINYL_SCRATCH)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRecording) Color(0xFFDC2626) else Color(0xFF10B981)
                                )
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isRecording) "STOP & ADD CLIP" else "START RECORDING", fontSize = 10.5.sp)
                            }
                        }
                    }
                    2 -> {
                        // Studio Sound FX
                        Text("Add studio sound effects & stems:", fontSize = 9.5.sp, color = Color(0xFFCBD5E1))
                        val sfxList = listOf(
                            Triple("808 Sub Boom Kick", 4, 0xFFEF4444),
                            Triple("Vinyl Backspin Scratch", 3, 0xFFF59E0B),
                            Triple("Classic Air Horn Blast", 5, 0xFF10B981),
                            Triple("Sci-Fi Laser Sweep", 4, 0xFF06B6D4),
                            Triple("Tape Stop Rewind", 3, 0xFFA855F7),
                            Triple("Vocal Hook: 'Check It!'", 2, 0xFFEC4899),
                            Triple("Amen Break Snare Roll", 6, 0xFF3B82F6)
                        )
                        LazyColumn(modifier = Modifier.height(180.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(sfxList) { (sfxName, dur, color) ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF161C2C))
                                        .border(0.5.dp, Color(color).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .clickable {
                                            onAddSoundFx(sfxName, dur, color)
                                            onDismiss()
                                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.BEAT_DROP)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(sfxName, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("${dur}s", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(color))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color.White, fontSize = 10.sp)
            }
        }
    )
}
