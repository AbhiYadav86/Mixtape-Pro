package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.LyricLine
import com.example.audio.RetroSoundEffects
import com.example.data.model.RetroSkin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RetroLyricsDisplay(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    skin: RetroSkin,
    isOverlay: Boolean = false,
    isFetching: Boolean = false,
    onSeekTo: (Long) -> Unit,
    onFetchOnline: () -> Unit,
    onFetchArt: () -> Unit,
    modifier: Modifier = Modifier,
    /** 1 = compact (inside the player); larger for the full-screen view. */
    textScale: Float = 1f
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var userInteracting by remember { mutableStateOf(false) }

    // Find current active line index
    val activeIndex = remember(lyrics, currentPositionMs) {
        if (lyrics.isEmpty()) -1
        else {
            val idx = lyrics.indexOfLast { it.timeMs <= currentPositionMs }
            if (idx == -1) 0 else idx
        }
    }

    // Auto-scroll when activeIndex changes and user is not manually scrolling
    LaunchedEffect(activeIndex, userInteracting) {
        if (!userInteracting && activeIndex >= 0 && activeIndex < lyrics.size) {
            val scrollTarget = (activeIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(scrollTarget)
        }
    }

    // Blinking cursor for retro HUD
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isOverlay) Color(0xDD0A0F1A)
                else Color(0xFF070B12)
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        userInteracting = true
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            delay(3500)
                            userInteracting = false
                        }
                    },
                    onDragCancel = {
                        userInteracting = false
                    },
                    onDrag = { _, _ -> }
                )
            }
    ) {
        if (lyrics.isEmpty()) {
            // Empty state HUD with quick fetch buttons
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isFetching) ">> QUERYING LRCLIB REPOSITORY..." else ">> NO SYNCED LYRICS FOUND",
                    color = skin.lcdTextColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isFetching) "Awaiting timestamped telemetry_" else "iTunes M4A metadata missing lyrics atom",
                    color = skin.lcdTextColor.copy(alpha = cursorAlpha),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(skin.accentColor.copy(alpha = 0.3f))
                            .border(1.dp, skin.accentColor, RoundedCornerShape(4.dp))
                            .clickable(enabled = !isFetching) {
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.RETRO_CLICK)
                                onFetchOnline()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isFetching) "FETCHING..." else "⚡ FETCH LRCLIB",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF222B3D))
                            .border(1.dp, Color(0xFF475569), RoundedCornerShape(4.dp))
                            .clickable {
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.RETRO_CLICK)
                                onFetchArt()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = " FETCH ART",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        } else {
            // Lyrics list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(lyrics) { index, line ->
                    val isActive = index == activeIndex
                    val isPast = index < activeIndex

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    isActive -> skin.accentColor.copy(alpha = if (isOverlay) 0.35f else 0.22f)
                                    else -> Color.Transparent
                                }
                            )
                            .clickable {
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                onSeekTo(line.timeMs)
                            }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Timestamp indicator
                        val minutes = (line.timeMs / 1000) / 60
                        val seconds = (line.timeMs / 1000) % 60
                        val timeStr = String.format("%02d:%02d", minutes, seconds)

                        Text(
                            text = timeStr,
                            color = when {
                                isActive -> skin.lcdTextColor
                                isPast -> Color(0xFF64748B)
                                else -> Color(0xFF475569)
                            },
                            fontSize = (9 * textScale).sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.width((36 * textScale).dp)
                        )

                        // Lyric text
                        Text(
                            text = if (isActive) "▶ ${line.text}" else "  ${line.text}",
                            color = when {
                                isActive -> Color.White
                                isPast -> Color(0xFF94A3B8)
                                else -> Color(0xFF64748B)
                            },
                            fontSize = ((if (isActive) 12 else 11) * textScale).sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = if (textScale > 1f) 4 else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Top edge gradient fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (isOverlay) Color(0xEE0A0F1A) else Color(0xFF070B12),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom edge gradient fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (isOverlay) Color(0xEE0A0F1A) else Color(0xFF070B12)
                        )
                    )
                )
        )
    }
}


/**
 * Lyrics over the visualiser, one line at a time: the current line large and centred, the next
 * line small underneath, in a band at the bottom so the visualiser stays visible. The expand
 * button opens the full-screen lyrics.
 */
@Composable
fun LyricsLineOverlay(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    skin: RetroSkin,
    isFetching: Boolean,
    onFetchOnline: () -> Unit,
    onFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeIndex = remember(lyrics, currentPositionMs) {
        if (lyrics.isEmpty()) -1 else lyrics.indexOfLast { it.timeMs <= currentPositionMs }
    }
    val current = lyrics.getOrNull(activeIndex)?.text?.takeIf { it.isNotBlank() }
        ?: if (activeIndex < 0 && lyrics.isNotEmpty()) "♪" else lyrics.getOrNull(activeIndex)?.let { "♪" }
    val next = lyrics.getOrNull(activeIndex + 1)?.text?.takeIf { it.isNotBlank() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC05070C), Color(0xEE05070C))))
                .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (lyrics.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isFetching) "Looking for lyrics…" else "No lyrics for this song",
                        color = Color(0xFFCBD5E1), fontSize = 12.sp, fontFamily = FontFamily.SansSerif
                    )
                    if (!isFetching) {
                        Text(
                            "FIND LYRICS",
                            color = skin.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, skin.accentColor, RoundedCornerShape(4.dp))
                                .clickable { onFetchOnline() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            } else {
                androidx.compose.animation.Crossfade(targetState = current ?: "", label = "lyricLine") { line ->
                    Text(
                        text = line,
                        color = Color.White,
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (next != null) {
                    Text(
                        text = next,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                    )
                }
            }
        }
        // Full-screen button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xAA05070C))
                .border(1.dp, skin.accentColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                .clickable { onFullScreen() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                androidx.compose.material.icons.Icons.Default.Fullscreen,
                contentDescription = "Full-screen lyrics",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Full-screen synced lyrics: scrolls with the song, tap a line to jump there. */
@Composable
fun FullScreenLyricsDialog(
    title: String,
    artist: String,
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    skin: RetroSkin,
    isFetching: Boolean,
    onSeekTo: (Long) -> Unit,
    onFetchOnline: () -> Unit,
    onFetchArt: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B12))
                .padding(top = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist, color = skin.accentColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2430))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            RetroLyricsDisplay(
                lyrics = lyrics,
                currentPositionMs = currentPositionMs,
                skin = skin,
                isOverlay = false,
                isFetching = isFetching,
                onSeekTo = onSeekTo,
                onFetchOnline = onFetchOnline,
                onFetchArt = onFetchArt,
                textScale = 1.5f,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
