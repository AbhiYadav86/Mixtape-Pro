package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RetroSoundEffects
import com.example.data.model.PlayerUiScale
import com.example.data.model.RetroSkin

@Composable
fun RetroSeekBar(
    currentPositionMs: Long,
    durationMs: Long,
    skin: RetroSkin,
    onSeek: (Long) -> Unit,
    scale: PlayerUiScale = PlayerUiScale.STANDARD,
    scaleFactor: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var isRemainingTimeMode by remember { mutableStateOf(false) }
    var sliderWidthPx by remember { mutableIntStateOf(1) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val progressFraction = if (isDragging) {
        dragFraction
    } else if (durationMs <= 0L) {
        0f // live stream: no length, so the bar stays at the start
    } else {
        (currentPositionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
    }

    val formatTime: (Long) -> String = { ms ->
        val totalSecs = (ms / 1000).coerceAtLeast(0)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        String.format("%02d:%02d", mins, secs)
    }

    val formatRemainingTime: (Long) -> String = { ms ->
        val remaining = (durationMs - ms).coerceAtLeast(0)
        val totalSecs = remaining / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        String.format("-%02d:%02d", mins, secs)
    }

    val activeTimeMs = if (isDragging) (dragFraction * safeDuration).toLong() else currentPositionMs
    val displayLeftTime = if (isRemainingTimeMode) formatRemainingTime(activeTimeMs) else formatTime(activeTimeMs)

    val timeFontSize = (scale.timeLabelFontSize * (scaleFactor / scale.factor)).coerceIn(13f, 22f).sp
    val sliderHeight = (scale.seekBarHeight * (scaleFactor / scale.factor)).coerceIn(24.dp, 40.dp)

    val tooltipAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 120),
        label = "tooltip_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 14.dp,
                vertical = if (scale == PlayerUiScale.COMPACT) 1.dp else 3.dp
            )
    ) {
        // Time Readout Header (Tap left time to toggle Elapsed vs Remaining)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                        isRemainingTimeMode = !isRemainingTimeMode
                    }
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = displayLeftTime,
                    fontSize = timeFontSize,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = skin.lcdTextColor,
                    softWrap = false,
                    maxLines = 1
                )
                Text(
                    text = if (isRemainingTimeMode) "REMAIN" else "ELAPSED",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = skin.lcdTextColor.copy(alpha = 0.6f)
                )
            }

            Text(
                text = if (durationMs <= 0L) "LIVE" else formatTime(durationMs),
                fontSize = timeFontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = skin.accentColor
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Custom Beveled Slider Bar with Floating Tooltip & Analog Calibration Ticks
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sliderHeight + 10.dp)
                .onSizeChanged { sliderWidthPx = it.width }
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        val newFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                        onSeek((newFraction * safeDuration).toLong())
                    }
                }
                .pointerInput(safeDuration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek((dragFraction * safeDuration).toLong())
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // 1. Floating Tactile Scrub Tooltip Badge
            if (tooltipAlpha > 0.01f) {
                val thumbWidthDp = 18.dp
                val tooltipWidthPx = 74.dp
                val currentThumbXPx = progressFraction * (sliderWidthPx - 18 * 3) // approximate
                val tooltipOffsetXPx = (progressFraction * sliderWidthPx - 110).coerceIn(
                    0f,
                    (sliderWidthPx - 230).coerceAtLeast(0).toFloat()
                )

                Box(
                    modifier = Modifier
                        .offset { IntOffset(tooltipOffsetXPx.toInt(), -16.dp.roundToPx()) }
                        .graphicsLayer { alpha = tooltipAlpha }
                        .shadow(6.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.5.dp, skin.accentColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "SEEK",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = skin.accentColor
                        )
                        Text(
                            text = formatTime((dragFraction * safeDuration).toLong()),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = skin.lcdTextColor
                        )
                    }
                }
            }

            // 2. Analog Slider Track Canvas with Tick Hash Marks & Beveled Knurled Thumb
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackHeight = 10.dp.toPx()
                val trackY = (size.height - trackHeight) / 2f + 4.dp.toPx()
                val thumbWidth = 18.dp.toPx()
                val thumbHeight = 28.dp.toPx()
                val thumbY = (size.height - thumbHeight) / 2f + 4.dp.toPx()
                val thumbX = (progressFraction * (size.width - thumbWidth)).coerceIn(0f, size.width - thumbWidth)
                val thumbCenterX = thumbX + thumbWidth / 2f
                val thumbCenterY = thumbY + thumbHeight / 2f

                // A. Analog Studio Calibration Hash Ticks along track
                val numTicks = 10
                for (i in 0..numTicks) {
                    val fraction = i.toFloat() / numTicks
                    val tickX = fraction * size.width
                    val isMajor = i == 0 || i == 5 || i == 10
                    val tickLen = if (isMajor) 6.dp.toPx() else 3.5.dp.toPx()
                    drawLine(
                        color = skin.lcdDimColor.copy(alpha = if (isMajor) 0.6f else 0.35f),
                        start = Offset(tickX, trackY - tickLen - 3.dp.toPx()),
                        end = Offset(tickX, trackY - 3.dp.toPx()),
                        strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                }

                // B. Track Background (Recessed Slot)
                drawRoundRect(
                    color = skin.sliderTrackColor,
                    topLeft = Offset(0f, trackY),
                    size = Size(size.width, trackHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = skin.chassisBevelShadow,
                    topLeft = Offset(0f, trackY),
                    size = Size(size.width, trackHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // C. Active Progress Fill with Gradient
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(skin.accentColor.copy(alpha = 0.75f), skin.accentColor),
                        startX = 0f,
                        endX = (thumbX + thumbWidth / 2f).coerceAtLeast(1f)
                    ),
                    topLeft = Offset(0f, trackY),
                    size = Size(thumbX + thumbWidth / 2f, trackHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // D. Thumb Grab Soft Halo (Pulsing / Glowing when scrubbing)
                if (isDragging) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(skin.accentColor.copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(thumbCenterX, thumbCenterY),
                            radius = thumbWidth * 1.6f
                        ),
                        center = Offset(thumbCenterX, thumbCenterY),
                        radius = thumbWidth * 1.6f
                    )
                }

                // E. Beveled Rectangular Aluminum Slider Thumb
                drawRoundRect(
                    color = skin.sliderThumbColor,
                    topLeft = Offset(thumbX, thumbY),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx())
                )

                // Thumb Bevel Border
                drawRoundRect(
                    color = if (isDragging) skin.accentColor else skin.sliderThumbBorder,
                    topLeft = Offset(thumbX, thumbY),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx()),
                    style = Stroke(width = if (isDragging) 2.dp.toPx() else 1.5.dp.toPx())
                )

                // F. Triple Knurled Grip Ridges on Thumb
                val gripOffsets = listOf(-3.5.dp.toPx(), 0f, 3.5.dp.toPx())
                for (offset in gripOffsets) {
                    val lineX = thumbCenterX + offset
                    // Shadow groove
                    drawLine(
                        color = Color(0x66000000),
                        start = Offset(lineX, thumbY + 5.dp.toPx()),
                        end = Offset(lineX, thumbY + thumbHeight - 5.dp.toPx()),
                        strokeWidth = 1.2.dp.toPx()
                    )
                    // Highlight groove
                    drawLine(
                        color = Color(0x55FFFFFF),
                        start = Offset(lineX + 1.dp.toPx(), thumbY + 5.dp.toPx()),
                        end = Offset(lineX + 1.dp.toPx(), thumbY + thumbHeight - 5.dp.toPx()),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
            }
        }
    }
}
