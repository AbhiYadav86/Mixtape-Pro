package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.StudioPreviewPlayer
import com.example.data.model.RetroSkin
import com.example.ui.viewmodel.MainViewModel

/*
 * Shared, consistently sized building blocks for every DJ Studio section, so buttons,
 * chips and tiles line up the same way everywhere:
 *   - all buttons are 48 dp tall, full width or split in equal halves
 *   - chips wrap onto aligned rows instead of scrolling off-screen
 *   - every section starts with its own monitor: spectrum, seek bar, timer and transport
 */

internal val StudioCardBg = Color(0xFF10141C)
internal val StudioSubtle = Color(0xFFB4BFCE)
internal val StudioButtonHeight = 48.dp

@Composable
fun StudioCard(
    skin: RetroSkin,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accent: Color = skin.accentColor,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioCardBg)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) {
                        Text(subtitle, color = StudioSubtle, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        content()
    }
}

@Composable
fun StudioButton(
    text: String,
    icon: ImageVector?,
    skin: RetroSkin,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg = if (primary) skin.accentColor else Color(0xFF1E2430)
    val fg = if (primary) Color.Black else Color.White
    Row(
        modifier = modifier
            .heightIn(min = StudioButtonHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.35f))
            .border(1.dp, if (primary) bg else Color(0x40FFFFFF), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Two equal-width buttons side by side. */
@Composable
fun StudioButtonPair(
    skin: RetroSkin,
    left: Triple<String, ImageVector, () -> Unit>,
    right: Triple<String, ImageVector, () -> Unit>,
    rightPrimary: Boolean = true,
    enabled: Boolean = true
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StudioButton(left.first, left.second, skin, Modifier.weight(1f), primary = false, enabled = enabled, onClick = left.third)
        StudioButton(right.first, right.second, skin, Modifier.weight(1f), primary = rightPrimary, enabled = enabled, onClick = right.third)
    }
}

/** Equal-width segmented selector (2–4 options). */
@Composable
fun <T> StudioSegmented(options: List<Pair<T, String>>, selected: T, skin: RetroSkin, onSelect: (T) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0B0E14))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEach { (value, label) ->
            val sel = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (sel) skin.accentColor else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (sel) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Chips laid out in an aligned grid of [columns] equal-width cells. */
@Composable
fun <T> StudioChipGrid(
    options: List<Pair<T, String>>,
    selected: T,
    skin: RetroSkin,
    columns: Int = 3,
    accent: Color = skin.accentColor,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (value, label) ->
                    val sel = value == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (sel) accent else Color(0xFF1E222D))
                            .border(1.dp, if (sel) accent else Color(0x40FFFFFF), RoundedCornerShape(20.dp))
                            .clickable { onSelect(value) }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (sel) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    }
                }
                repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun StudioSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    skin: RetroSkin,
    steps: Int = 0,
    onChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(valueText, color = skin.accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(thumbColor = skin.accentColor, activeTrackColor = skin.accentColor, inactiveTrackColor = Color(0x40FFFFFF))
        )
    }
}

@Composable
fun StudioToggle(label: String, sub: String?, checked: Boolean, skin: RetroSkin, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (sub != null) Text(sub, color = StudioSubtle, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = skin.accentColor, checkedThumbColor = Color.Black)
        )
    }
}

internal fun fmtMmSs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return String.format("%d:%02d", s / 60, s % 60)
}

/**
 * Each DJ Studio section's own monitor: live spectrum, a seek bar (showing how much of the
 * preview has been rendered, plus transition markers), a timer and transport controls.
 * It only shows audio that was started from [section]; everything plays here, never in the
 * main player.
 */
@Composable
fun StudioMonitor(
    viewModel: MainViewModel,
    skin: RetroSkin,
    section: String,
    title: String,
    height: Dp = 84.dp
) {
    val raw by StudioPreviewPlayer.state.collectAsState()
    val rawBands by StudioPreviewPlayer.bands.collectAsState()
    val task by viewModel.studioTask.collectAsState()
    val mine = raw.loaded && raw.section == section
    val state = if (mine) raw else StudioPreviewPlayer.State()
    val bands = if (mine) rawBands else FloatArray(StudioPreviewPlayer.BANDS)
    var scrubMs by remember { mutableStateOf<Long?>(null) }
    val busy = task != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF05070B))
            .border(1.5.dp, skin.accentColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ---- header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        when {
                            state.isPlaying && !state.isBuffering -> Color(0xFF00FF66)
                            busy || state.isBuffering -> Color(0xFFFFB800)
                            state.loaded -> skin.accentColor
                            else -> Color(0xFF475569)
                        }
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = skin.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, maxLines = 1)
                Text(
                    text = when {
                        busy -> task!!.label
                        state.loaded -> state.label
                        else -> "Tap a Preview button to listen here"
                    },
                    color = if (state.loaded || busy) Color.White else StudioSubtle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ---- versions of the same audio (e.g. Original / Cleaned): switch instantly
        if (state.stems.size > 1) {
            StudioSegmented(
                options = state.stems.mapIndexed { i, name -> i to name },
                selected = state.activeStem,
                skin = skin
            ) { i -> StudioPreviewPlayer.setStem(i) }
        }

        // ---- spectrum
        Box(modifier = Modifier.fillMaxWidth().height(height)) {
            val peakColors = skin.eqBarGradient.ifEmpty { listOf(skin.accentColor, skin.accentColor) }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val n = bands.size
                val gap = 3.dp.toPx()
                val w = (size.width - gap * (n - 1)) / n
                val brush = Brush.verticalGradient(peakColors.reversed(), startY = 0f, endY = size.height)
                for (i in 0 until n) {
                    val level = bands[i].coerceIn(0f, 1f)
                    val h = (size.height * level).coerceAtLeast(2.dp.toPx())
                    val x = i * (w + gap)
                    drawRoundRect(color = Color(0x14FFFFFF), topLeft = Offset(x, 0f), size = Size(w, size.height), cornerRadius = CornerRadius(2.dp.toPx()))
                    drawRoundRect(brush = brush, topLeft = Offset(x, size.height - h), size = Size(w, h), cornerRadius = CornerRadius(2.dp.toPx()))
                }
            }
            if (busy && !state.loaded) {
                val p = task?.progress
                Column(modifier = Modifier.fillMaxSize().background(Color(0xAA05070B)), verticalArrangement = Arrangement.Center) {
                    if (p != null) {
                        LinearProgressIndicator(progress = { p }, color = skin.accentColor, trackColor = Color(0x33FFFFFF), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    } else {
                        LinearProgressIndicator(color = skin.accentColor, trackColor = Color(0x33FFFFFF), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    }
                }
            }
        }

        // ---- seek bar
        StudioSeekBar(
            state = state,
            skin = skin,
            scrubMs = scrubMs,
            onScrub = { scrubMs = it },
            onSeek = { ms -> scrubMs = null; StudioPreviewPlayer.seekTo(ms) }
        )

        // ---- timer
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                fmtMmSs(scrubMs ?: state.positionMs),
                color = if (scrubMs != null) Color.White else skin.accentColor,
                fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
            )
            Text(
                text = when {
                    !state.loaded -> ""
                    state.isBuffering -> "Preparing…"
                    !state.renderComplete && state.canSeekAnywhere -> "Processing ahead • drag to jump"
                    !state.renderComplete -> "Processing ${fmtMmSs(state.bufferEndMs)} ready"
                    else -> ""
                },
                color = StudioSubtle, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
            )
            Text(
                if (state.loaded) "-${fmtMmSs((state.durationMs - (scrubMs ?: state.positionMs)).coerceAtLeast(0L))} / ${fmtMmSs(state.durationMs)}" else "0:00 / 0:00",
                color = StudioSubtle, fontSize = 13.sp, fontFamily = FontFamily.Monospace
            )
        }

        // ---- transport
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            val enabled = state.loaded
            TransportKey(Icons.Default.SkipPrevious, "Back to start", skin, enabled, Modifier.weight(1f)) { StudioPreviewPlayer.seekTo(0L) }
            TransportKey(Icons.Default.Replay10, "Back 10 seconds", skin, enabled, Modifier.weight(1f)) { StudioPreviewPlayer.seekBy(-10_000L) }
            TransportKey(
                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (state.isPlaying) "Pause" else "Play",
                skin, enabled, Modifier.weight(1.4f), primary = true
            ) { StudioPreviewPlayer.togglePlayPause() }
            TransportKey(Icons.Default.Forward10, "Forward 10 seconds", skin, enabled, Modifier.weight(1f)) { StudioPreviewPlayer.seekBy(10_000L) }
            TransportKey(Icons.Default.Stop, "Stop", skin, enabled || busy, Modifier.weight(1f), danger = true) { viewModel.stopStudioAudio() }
        }
    }
}

@Composable
private fun TransportKey(
    icon: ImageVector,
    description: String,
    skin: RetroSkin,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val bg = when {
        primary -> skin.accentColor
        danger -> Color(0x33EF4444)
        else -> Color(0xFF1E2430)
    }
    val fg = when {
        primary -> Color.Black
        danger -> Color(0xFFFCA5A5)
        else -> Color.White
    }
    Box(
        modifier = modifier
            .height(StudioButtonHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.3f))
            .border(1.dp, if (primary) bg else Color(0x30FFFFFF), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (enabled) fg else fg.copy(alpha = 0.4f), modifier = Modifier.size(if (primary) 28.dp else 22.dp))
    }
}

/**
 * Seek bar: played part in the skin colour, the rendered part lighter, transition markers as
 * ticks. Tap or drag to jump.
 */
@Composable
fun StudioSeekBar(
    state: StudioPreviewPlayer.State,
    skin: RetroSkin,
    scrubMs: Long?,
    onScrub: (Long?) -> Unit,
    onSeek: (Long) -> Unit
) {
    val duration = state.durationMs.coerceAtLeast(1L)
    val enabled = state.loaded && state.durationMs > 0
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentOnScrub by rememberUpdatedState(onScrub)
    val currentDuration by rememberUpdatedState(duration)
    val currentEnabled by rememberUpdatedState(enabled)
    val markerColor = skin.secondaryAccentColor

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .pointerInput(Unit) {
                detectTapGestures { off ->
                    if (currentEnabled) currentOnSeek((off.x / size.width).coerceIn(0f, 1f).let { (it * currentDuration).toLong() })
                }
            }
            .pointerInput(Unit) {
                var last = 0L
                detectHorizontalDragGestures(
                    onDragStart = { off ->
                        if (currentEnabled) { last = ((off.x / size.width).coerceIn(0f, 1f) * currentDuration).toLong(); currentOnScrub(last) }
                    },
                    onDragEnd = { if (currentEnabled) currentOnSeek(last) else currentOnScrub(null) },
                    onDragCancel = { currentOnScrub(null) }
                ) { change, _ ->
                    if (currentEnabled) {
                        last = ((change.position.x / size.width).coerceIn(0f, 1f) * currentDuration).toLong()
                        currentOnScrub(last)
                    }
                }
            }
    ) {
        val cy = size.height / 2
        val trackH = 6.dp.toPx()
        val r = CornerRadius(trackH / 2)
        fun x(ms: Long) = (ms.toFloat() / duration).coerceIn(0f, 1f) * size.width
        drawRoundRect(color = Color(0x26FFFFFF), topLeft = Offset(0f, cy - trackH / 2), size = Size(size.width, trackH), cornerRadius = r)
        if (enabled) {
            // rendered (ready to play) region
            val bs = x(state.bufferStartMs)
            val be = x(if (state.renderComplete && !state.canSeekAnywhere) duration else state.bufferEndMs)
            if (be > bs) drawRoundRect(color = Color(0x55FFFFFF), topLeft = Offset(bs, cy - trackH / 2), size = Size(be - bs, trackH), cornerRadius = r)
            // played
            val px = x(scrubMs ?: state.positionMs)
            drawRoundRect(color = skin.accentColor, topLeft = Offset(0f, cy - trackH / 2), size = Size(px, trackH), cornerRadius = r)
            // markers
            state.markers.forEach { m ->
                val mx = x(m)
                drawRect(color = markerColor, topLeft = Offset(mx - 1.dp.toPx(), cy - 9.dp.toPx()), size = Size(2.dp.toPx(), 18.dp.toPx()))
            }
            // thumb
            drawCircle(color = Color.White, radius = (if (scrubMs != null) 10 else 8).dp.toPx(), center = Offset(px, cy))
            drawCircle(color = skin.accentColor, radius = (if (scrubMs != null) 6 else 5).dp.toPx(), center = Offset(px, cy))
        }
    }
}
