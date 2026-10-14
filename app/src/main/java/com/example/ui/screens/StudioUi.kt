package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 *   - every section starts with the same live spectrum monitor
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
 * Live spectrum analyser + transport for everything the studio plays. Shows render progress
 * while a preview is being prepared, then the audio's spectrum while it plays.
 */
@Composable
fun StudioMonitor(viewModel: MainViewModel, skin: RetroSkin, height: Dp = 92.dp) {
    val state by StudioPreviewPlayer.state.collectAsState()
    val bands by StudioPreviewPlayer.bands.collectAsState()
    val task by viewModel.studioTask.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF05070B))
            .border(1.5.dp, skin.accentColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (state.isPlaying) Color(0xFF00FF66) else if (task != null) Color(0xFFFFB800) else Color(0xFF475569))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when {
                    task != null -> task!!.label
                    state.isPlaying -> state.label
                    else -> "STUDIO MONITOR • tap Preview to listen"
                },
                color = if (state.isPlaying || task != null) Color.White else StudioSubtle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (state.isPlaying) {
                Text(
                    "${fmtMmSs(state.positionMs)} / ${fmtMmSs(state.durationMs)}",
                    color = skin.accentColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace
                )
            }
            if (state.isPlaying || task != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33EF4444))
                        .clickable { viewModel.stopStudioAudio() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color(0xFFFCA5A5), modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        val t = task
        if (t != null && !state.isPlaying) {
            Box(modifier = Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
                if (t.progress != null) {
                    LinearProgressIndicator(
                        progress = { t.progress ?: 0f },
                        color = skin.accentColor,
                        trackColor = Color(0x33FFFFFF),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(color = skin.accentColor, trackColor = Color(0x33FFFFFF), modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
            val peakColors = skin.eqBarGradient.ifEmpty { listOf(skin.accentColor, skin.accentColor) }
            Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
                val n = bands.size
                val gap = 3.dp.toPx()
                val w = (size.width - gap * (n - 1)) / n
                val brush = Brush.verticalGradient(peakColors.reversed(), startY = 0f, endY = size.height)
                for (i in 0 until n) {
                    val level = bands[i].coerceIn(0f, 1f)
                    val h = (size.height * level).coerceAtLeast(2.dp.toPx())
                    val x = i * (w + gap)
                    // dim "unlit" segment column
                    drawRoundRect(
                        color = Color(0x14FFFFFF),
                        topLeft = Offset(x, 0f),
                        size = Size(w, size.height),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(x, size.height - h),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
            }
        }
    }
}
