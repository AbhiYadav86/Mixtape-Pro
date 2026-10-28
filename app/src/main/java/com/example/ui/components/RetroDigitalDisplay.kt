package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RetroSoundEffects
import com.example.data.model.AudioSource
import com.example.data.model.PlayerUiScale
import com.example.data.model.RepeatMode
import com.example.data.model.RetroSkin
import com.example.data.model.Song

@Composable
fun RetroDigitalDisplay(
    song: Song?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    repeatMode: RepeatMode,
    isShuffle: Boolean,
    skin: RetroSkin,
    scale: PlayerUiScale = PlayerUiScale.STANDARD,
    scaleFactor: Float = 1.0f,
    peakLevel: Float = 0f,
    isEqActive: Boolean = true,
    isDspActive: Boolean = true,
    onToggleRepeat: (() -> Unit)? = null,
    onToggleShuffle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showRemainingTime by remember { mutableStateOf(false) }

    // Formatted time digits without milliseconds (MM:SS)
    val digitsFormatted = remember(currentPositionMs, durationMs, showRemainingTime) {
        val targetMs = if (showRemainingTime && durationMs > 0) {
            (durationMs - currentPositionMs).coerceAtLeast(0)
        } else {
            currentPositionMs
        }
        val totalSecs = (targetMs / 1000).coerceAtLeast(0)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        String.format("%02d:%02d", mins, secs)
    }

    val totalDurationFormatted = remember(durationMs) {
        if (durationMs <= 0) "LIVE"
        else {
            val totalSecs = (durationMs / 1000).coerceAtLeast(0)
            val mins = totalSecs / 60
            val secs = totalSecs % 60
            String.format("%02d:%02d", mins, secs)
        }
    }

    val sourceLabel = when (song?.source) {
        AudioSource.LOCAL -> "LOCAL STORAGE"
        AudioSource.RADIO_STREAM -> "FM 90s STREAM"
        AudioSource.FREE_ARCHIVE -> "WEB ARCHIVE"
        AudioSource.GOOGLE_DRIVE -> "G-DRIVE CLOUD"
        AudioSource.CLOUD_STORAGE -> "CLOUD VAULT"
        else -> "JUKEBOX DECK"
    }

    val dynamicPaddingH = (11 * (scaleFactor / scale.factor).coerceIn(0.85f, 1.25f)).dp
    val dynamicPaddingV = if (scale == PlayerUiScale.COMPACT) 4.dp else (6.5f * (scaleFactor / scale.factor).coerceIn(0.85f, 1.2f)).dp


    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .shadow(6.dp, RoundedCornerShape(10.dp))
            .background(skin.lcdBackground, RoundedCornerShape(10.dp))
            .border(2.dp, skin.lcdBorderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = dynamicPaddingH, vertical = dynamicPaddingV)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // 1. TOP ILLUMINATED STATUS LED CHASSIS ROW (Authentic Vintage Hi-Fi Indicator Lamps)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0x35000000))
                    .border(1.dp, skin.lcdDimColor.copy(alpha = 0.25f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 8.dp, vertical = 3.5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left LED Array: PLAY, STEREO, PEAK
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RetroLedLamp(
                        label = "PLAY",
                        isOn = isPlaying,
                        isPulsing = isBuffering,
                        ledColor = Color(0xFF22C55E), // Emerald Green
                        labelColor = skin.lcdTextColor
                    )
                    RetroLedLamp(
                        label = "STEREO",
                        isOn = isPlaying,
                        ledColor = Color(0xFFEF4444), // Classic Ruby Red
                        labelColor = skin.lcdTextColor
                    )
                    RetroLedLamp(
                        label = "PEAK",
                        isOn = peakLevel > 0.82f,
                        ledColor = Color(0xFFFF0055), // Hot Clip Red
                        labelColor = Color(0xFFFF4D6D)
                    )
                }

                // Right Lcd Badges: Bitrate, Repeat, Shuffle (Tap to Toggle)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LcdBadge(
                        text = "${song?.bitrateKbps ?: 320}K",
                        isActive = isPlaying,
                        activeColor = skin.accentColor,
                        dimColor = skin.lcdDimColor
                    )
                    LcdBadge(
                        text = if (repeatMode == RepeatMode.ONE) "RPT 1" else if (repeatMode == RepeatMode.ALL) "RPT ALL" else "RPT",
                        isActive = repeatMode != RepeatMode.OFF,
                        activeColor = skin.secondaryAccentColor,
                        dimColor = skin.lcdDimColor,
                        onClick = onToggleRepeat
                    )
                    LcdBadge(
                        text = if (isShuffle) "SHUF ON" else "SHUF",
                        isActive = isShuffle,
                        activeColor = skin.secondaryAccentColor,
                        dimColor = skin.lcdDimColor,
                        onClick = onToggleShuffle
                    )
                }
            }

            // 2. FULLY VISIBLE & PROMINENT TRACK TITLE & METADATA LCD DISPLAY BOX
            // Enhanced with Smooth Authentic Pause-and-Scroll Marquee (Enlarged by 10%)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x35000000))
                    .border(1.dp, skin.lcdDimColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                // Smooth Authentic Marquee with Pause at Beginning and Loop
                Text(
                    text = song?.title ?: "Mixtape Pro",
                    color = skin.lcdTextColor,
                    fontSize = (19f * (scaleFactor / scale.factor)).coerceIn(16.5f, 23f).sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                            repeatDelayMillis = 1800,
                            initialDelayMillis = 1800,
                            velocity = 35.dp
                        )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Artist on its own line (scrolls if long) — never overlaps the album text.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = skin.accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = song?.artist ?: "Ready to Play",
                        color = skin.accentColor,
                        fontSize = (15f * (scaleFactor / scale.factor)).coerceIn(13.5f, 18f).sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                repeatDelayMillis = 2200,
                                initialDelayMillis = 2200,
                                velocity = 28.dp
                            )
                    )
                }

                // Album • Year (only real values, no fake "1999")
                val albumLine = if (song == null) {
                    "Mixtape Pro"
                } else {
                    listOf(song.album, song.year).filter { it.isNotBlank() }.joinToString(" • ")
                }
                if (albumLine.isNotBlank()) {
                    Text(
                        text = albumLine,
                        color = skin.lcdTextColor.copy(alpha = 0.7f),
                        fontSize = (13f * (scaleFactor / scale.factor)).coerceIn(11.5f, 15f).sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, top = 1.dp)
                    )
                }
            }

            // 3. MIDDLE SECTION: Large LCD Timer (MM:SS) + Format Badges (Safely bounded for any screen width)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x33000000), RoundedCornerShape(8.dp))
                    .border(1.dp, skin.lcdDimColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Digital LCD Timer Block (Strictly MM:SS, safely scaled to prevent truncation on high-res displays)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x55000000))
                        .border(1.dp, skin.lcdDimColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                        .clickable {
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                            showRemainingTime = !showRemainingTime
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    // The digits ignore the phone's text-size setting so MM:SS always fits
                    val fontScaleNow = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
                    val timerFontSize = ((scale.timerFontSize * 0.74f * (scaleFactor / scale.factor)).coerceIn(22f, 30f) / fontScaleNow).sp
                    val totalLabelSize = if (scale == PlayerUiScale.COMPACT) 10.5.sp else 12.sp
                    val trackTimeLabelSize = if (scale == PlayerUiScale.COMPACT) 10.sp else 11.5.sp

                    // Labels take what's left after the digits (weight), so they shrink instead of the time
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (showRemainingTime) "REMAINING" else "TRACK TIME",
                            overflow = TextOverflow.Ellipsis,
                            color = skin.accentColor,
                            fontSize = trackTimeLabelSize,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        Text(
                            text = "TOTAL $totalDurationFormatted",
                            overflow = TextOverflow.Ellipsis,
                            color = skin.lcdTextColor.copy(alpha = 0.65f),
                            fontSize = totalLabelSize,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        // Dedicated Illuminated Minus Segment (Authentic Hi-Fi LCD Remaining Indicator)
                        Box(
                            modifier = Modifier.width((timerFontSize.value * 0.38f).dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (showRemainingTime) {
                                Text(
                                    text = "-",
                                    color = skin.lcdTextColor,
                                    fontSize = timerFontSize,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }

                        // Fixed MM:SS Digits Block (Never shifts or truncates)
                        Box(contentAlignment = Alignment.CenterStart) {
                            Text(
                                text = digitsFormatted,
                                color = skin.lcdTextColor,
                                fontSize = timerFontSize,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Format & Genre Badges
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    // Badges get a fixed maximum width so long genres can never squeeze the timer
                    modifier = Modifier.widthIn(max = 84.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(skin.accentColor.copy(alpha = 0.2f))
                            .border(1.dp, skin.accentColor.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = song?.genre
                                ?.split('/', ';', ',')?.firstOrNull()?.trim()
                                ?.takeIf { it.isNotBlank() } ?: "Stereo",
                            color = skin.accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x33000000))
                            .border(1.dp, skin.lcdDimColor.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${song?.bitrateKbps ?: 320} kbps",
                            color = skin.lcdTextColor.copy(alpha = 0.7f),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Authentic Vintage Hi-Fi Jewel LED Indicator with Soft Radial Halo & Specular Reflection
 */
@Composable
fun RetroLedLamp(
    label: String,
    isOn: Boolean,
    ledColor: Color,
    isPulsing: Boolean = false,
    labelColor: Color = Color(0xFFCBD5E1),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "led_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val glowAlpha = if (!isOn) 0.04f else if (isPulsing) pulseAlpha else 0.85f
    val coreAlpha = if (!isOn) 0.25f else if (isPulsing) pulseAlpha.coerceAtLeast(0.6f) else 1.0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        modifier = modifier
    ) {
        // Physical circular jewel LED assembly with soft halo
        Box(
            modifier = Modifier.size(15.dp),
            contentAlignment = Alignment.Center
        ) {
            // 1. Soft radial luminous halo (spreads outward when lit)
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .graphicsLayer { alpha = glowAlpha }
                    .background(
                        Brush.radialGradient(
                            listOf(
                                ledColor.copy(alpha = 0.9f),
                                ledColor.copy(alpha = 0.45f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // 2. Beveled metal bezel socket
            Box(
                modifier = Modifier
                    .size(9.5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A))
                    .border(0.75.dp, Color(0xFF64748B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // 3. Inner glass lens & emitter dome
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (isOn) {
                                Brush.radialGradient(
                                    listOf(
                                        Color.White.copy(alpha = coreAlpha),
                                        ledColor.copy(alpha = coreAlpha),
                                        ledColor.copy(alpha = coreAlpha * 0.85f)
                                    )
                                )
                            } else {
                                Brush.radialGradient(
                                    listOf(
                                        ledColor.copy(alpha = 0.25f),
                                        Color(0xFF1E293B)
                                    )
                                )
                            }
                        )
                ) {
                    // Specular micro reflection dot on glass lens
                    if (isOn) {
                        Box(
                            modifier = Modifier
                                .offset(x = 1.2.dp, y = 0.8.dp)
                                .size(1.5.dp)
                                .background(Color.White.copy(alpha = 0.95f), CircleShape)
                        )
                    }
                }
            }
        }

        // Crisp engraved label beside the LED
        Text(
            text = label,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isOn) labelColor else labelColor.copy(alpha = 0.4f),
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun LcdBadge(
    text: String,
    isActive: Boolean,
    activeColor: Color,
    dimColor: Color,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.22f) else Color(0x22000000))
            .border(
                1.dp,
                if (isActive) activeColor.copy(alpha = 0.65f) else dimColor.copy(alpha = 0.25f),
                RoundedCornerShape(3.dp)
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                        onClick()
                    }
                } else Modifier
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isActive) activeColor else dimColor.copy(alpha = 0.5f)
        )
    }
}
