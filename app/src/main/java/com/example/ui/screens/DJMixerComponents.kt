package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.*

enum class CrossfaderBus(val label: String) {
    LEFT("L"),
    THRU("THRU"),
    RIGHT("R")
}

data class DJDeckData(
    val deckId: String, // "A", "B", "C", "D"
    val song: Song?,
    val isPlaying: Boolean,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val playProgress: Float = 0.0f, // 0.0 to 1.0
    val cueProgress: Float = 0.0f,
    val crossfaderBus: CrossfaderBus = if (deckId == "A" || deckId == "C") CrossfaderBus.LEFT else CrossfaderBus.RIGHT,
    val isLowKill: Boolean = false,
    val isMidKill: Boolean = false,
    val isHighKill: Boolean = false,
    val isLoopActive: Boolean = false,
    val loopLengthBeats: Int = 4
)

/**
 * Interactive scrolling & scrubbing DJ Waveform Display.
 * Displays dynamic frequency-colored amplitude peaks, beat grid lines, cue markers,
 * and allows direct tactile touch seeking/scrubbing across the track.
 */
@Composable
fun DJWaveformView(
    progress: Float,
    cuePoint: Float,
    isPlaying: Boolean,
    accentColor: Color,
    skin: RetroSkin,
    trackDurationSec: Int = 180,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp
) {
    // Generate deterministic pseudo-random waveform peaks based on duration
    val numBars = 72
    val barAmplitudes = remember(numBars) {
        FloatArray(numBars) { i ->
            val phase = i * 0.28f
            val harmonic1 = sin(phase) * 0.4f
            val harmonic2 = cos(phase * 2.3f) * 0.3f
            val harmonic3 = sin(phase * 0.7f) * 0.2f
            (abs(harmonic1 + harmonic2 + harmonic3) + 0.15f).coerceIn(0.12f, 0.95f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF07090F))
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(newProgress)
                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.NEEDLE_DROP)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h * 0.5f

            // 1. Beat Grid lines (every 8th step)
            val beatInterval = w / 8f
            for (b in 0..8) {
                val bx = b * beatInterval
                drawLine(
                    color = Color(0xFF1E283A),
                    start = Offset(bx, 0f),
                    end = Offset(bx, h),
                    strokeWidth = 1f
                )
            }

            // 2. Waveform Bars
            val barWidth = w / numBars
            for (i in 0 until numBars) {
                val barProgress = i.toFloat() / numBars
                val isPlayed = barProgress <= progress
                val amp = barAmplitudes[i] * (h * 0.45f)
                val bx = i * barWidth + barWidth * 0.5f

                // Three-band spectral coloration:
                // Lows (bass) -> Deep Accent / Red, Mids -> Amber, Highs -> Cyan / Light Accent
                val spectralColor = when {
                    i % 3 == 0 -> accentColor
                    i % 3 == 1 -> Color(0xFF38BDF8)
                    else -> Color(0xFFA78BFA)
                }

                val barColor = if (isPlayed) {
                    spectralColor
                } else {
                    spectralColor.copy(alpha = 0.28f)
                }

                drawLine(
                    color = barColor,
                    start = Offset(bx, midY - amp),
                    end = Offset(bx, midY + amp),
                    strokeWidth = barWidth * 0.75f
                )
            }

            // 3. Cue Marker
            if (cuePoint in 0.01f..0.99f) {
                val cueX = cuePoint * w
                drawLine(
                    color = Color(0xFFFFB800),
                    start = Offset(cueX, 0f),
                    end = Offset(cueX, h),
                    strokeWidth = 2f
                )
                // Triangle flag at top
                val cueFlag = Path().apply {
                    moveTo(cueX, 0f)
                    lineTo(cueX + 8f, 5f)
                    lineTo(cueX, 10f)
                    close()
                }
                drawPath(cueFlag, color = Color(0xFFFFB800))
            }

            // 4. Active Playhead Line
            val playheadX = progress.coerceIn(0f, 1f) * w
            drawLine(
                color = Color.White,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, h),
                strokeWidth = 2.5f
            )
            // Playhead indicator diamond
            val diamond = Path().apply {
                moveTo(playheadX, 0f)
                lineTo(playheadX + 5f, 5f)
                lineTo(playheadX, 10f)
                lineTo(playheadX - 5f, 5f)
                close()
            }
            drawPath(diamond, color = Color.White)
        }

        // Overlay: Time readout (Elapsed / Remaining)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val elapsedSec = (progress * trackDurationSec).toInt()
            val remainSec = max(0, trackDurationSec - elapsedSec)
            Text(
                text = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = String.format("-%02d:%02d", remainSec / 60, remainSec % 60),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

/**
 * 4-Channel Professional Club Turntable Deck.
 * Encapsulates vinyl platter, interactive waveform, speed/tempo control,
 * 3-band kill EQ, cue/loop transport, and crossfader bus assignment.
 */
@Composable
fun DJTurntableDeck4Channel(
    deckId: String,
    deckName: String,
    song: Song?,
    isPlaying: Boolean,
    speed: Float,
    volume: Float,
    progress: Float,
    cuePoint: Float,
    crossfaderBus: CrossfaderBus,
    isLowKill: Boolean,
    isMidKill: Boolean,
    isHighKill: Boolean,
    isLoopActive: Boolean,
    accentColor: Color,
    skin: RetroSkin,
    onTogglePlay: () -> Unit,
    onSelectTrack: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSeek: (Float) -> Unit,
    onCue: () -> Unit,
    onSync: () -> Unit,
    onToggleLowKill: () -> Unit,
    onToggleMidKill: () -> Unit,
    onToggleHighKill: () -> Unit,
    onToggleLoop: () -> Unit,
    onBusChange: (CrossfaderBus) -> Unit,
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin_$deckId")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (2000 / speed.coerceAtLeast(0.1f)).toInt().coerceAtLeast(350), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_$deckId"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = skin.lcdBackground.copy(alpha = 0.90f)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor.copy(alpha = 0.75f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Deck Header: Name, Bus Badge, and Load Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = deckName,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = accentColor
                    )
                    // Bus Assignment Selector (L / THRU / R)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF0F1420))
                            .border(0.5.dp, Color(0xFF263045), RoundedCornerShape(3.dp))
                            .padding(1.dp)
                    ) {
                        CrossfaderBus.values().forEach { bus ->
                            val isSelected = crossfaderBus == bus
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isSelected) accentColor else Color.Transparent)
                                    .clickable {
                                        onBusChange(bus)
                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = bus.label,
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onSelectTrack, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = "Load Track", tint = accentColor, modifier = Modifier.size(15.dp))
                }
            }

            // Track Title & Artist
            Text(
                text = song?.title ?: "No Track Loaded",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song?.artist ?: "Tap music icon to load",
                fontSize = 8.5.sp,
                color = skin.lcdTextColor.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Waveform Scrubbing View for this Deck
            DJWaveformView(
                progress = progress,
                cuePoint = cuePoint,
                isPlaying = isPlaying,
                accentColor = accentColor,
                skin = skin,
                trackDurationSec = if (song != null) (song.durationMs / 1000).toInt().coerceAtLeast(30) else 180,
                onSeek = onSeek,
                height = if (compactMode) 32.dp else 40.dp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Vinyl Turntable & Level Center Rack
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Vinyl Platter
                val platterSize = if (compactMode) 60.dp else 72.dp
                Box(
                    modifier = Modifier
                        .size(platterSize)
                        .clip(CircleShape)
                        .background(Color(0xFF090A0E))
                        .border(2.dp, skin.chassisBorderColor, CircleShape)
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(if (isPlaying) rotationAngle else 0f)
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)
                        drawCircle(color = Color(0xFF141720), radius = size.width * 0.46f, center = center)
                        drawCircle(color = Color(0xFF0A0B0E), radius = size.width * 0.38f, center = center)
                        drawCircle(color = Color(0xFF181B26), radius = size.width * 0.28f, center = center)
                        drawCircle(color = accentColor, radius = size.width * 0.18f, center = center)
                        drawCircle(color = Color.Black, radius = size.width * 0.06f, center = center)
                    }

                    if (!isPlaying) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                // 3-Band Kill EQ Buttons (LOW / MID / HIGH)
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("KILL EQ", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    listOf(
                        Triple("HI", isHighKill, onToggleHighKill),
                        Triple("MID", isMidKill, onToggleMidKill),
                        Triple("LOW", isLowKill, onToggleLowKill)
                    ).forEach { (label, isKilled, onClick) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isKilled) Color(0xFFFF3D00) else Color(0xFF1A2130))
                                .border(0.5.dp, if (isKilled) Color.White else Color(0xFF334155), RoundedCornerShape(2.dp))
                                .clickable {
                                    onClick()
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isKilled) Color.White else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                // Volume Fader & Pitch Indicator
                Column(
                    modifier = Modifier.width(60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "VOL ${(volume * 100).toInt()}%",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..1f,
                        modifier = Modifier.height(26.dp),
                        colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                    )
                }
            }

            // Speed & Pitch Slider with Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pitchPercent = ((speed - 1.0f) * 100).toInt()
                Text(
                    text = "PITCH: ${if (pitchPercent >= 0) "+$pitchPercent%" else "$pitchPercent%"}",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "0% RESET",
                    fontSize = 7.5.sp,
                    color = Color(0xFF94A3B8),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable {
                        onSpeedChange(1.0f)
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                    }
                )
            }
            Slider(
                value = speed,
                onValueChange = onSpeedChange,
                valueRange = 0.84f..1.16f,
                modifier = Modifier.height(24.dp),
                colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Transport Row: PLAY/PAUSE, CUE, SYNC, LOOP
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                StudioHardwareButton(
                    text = "",
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    showLed = false,
                    onClick = onTogglePlay,
                    skin = skin,
                    theme = if (isPlaying) StudioButtonTheme.RED_REC else StudioButtonTheme.GREEN_PLAY,
                    isActive = isPlaying,
                    height = 34.dp,
                    fontSize = 8.sp,
                    modifier = Modifier.weight(1f)
                )

                StudioHardwareButton(
                    text = "",
                    icon = Icons.Default.FirstPage,
                    showLed = false,
                    onClick = onCue,
                    skin = skin,
                    theme = StudioButtonTheme.AMBER_CUE,
                    height = 34.dp,
                    fontSize = 8.sp,
                    modifier = Modifier.weight(1f)
                )

                StudioHardwareButton(
                    text = "",
                    icon = Icons.Default.Sync,
                    showLed = false,
                    onClick = onSync,
                    skin = skin,
                    theme = StudioButtonTheme.CYAN_SYNC,
                    height = 34.dp,
                    fontSize = 8.sp,
                    modifier = Modifier.weight(1f)
                )

                StudioHardwareButton(
                    text = "",
                    icon = Icons.Default.Repeat,
                    showLed = false,
                    onClick = onToggleLoop,
                    skin = skin,
                    theme = StudioButtonTheme.CHAMPAGNE_GOLD,
                    isActive = isLoopActive,
                    height = 34.dp,
                    fontSize = 8.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
