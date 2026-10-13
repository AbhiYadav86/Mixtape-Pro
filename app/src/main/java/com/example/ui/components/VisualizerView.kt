package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.model.RetroSkin
import com.example.data.model.VisualizerType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VisualizerView(
    spectrumBands: FloatArray,
    peakHoldBands: FloatArray,
    vuLeft: Float,
    vuRight: Float,
    waveformPoints: FloatArray,
    visualizerType: VisualizerType,
    skin: RetroSkin,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (visualizerType) {
                VisualizerType.CLASSIC_BARS_PEAK -> {
                    drawEqualizerBars(spectrumBands, peakHoldBands, skin)
                }
                VisualizerType.ANALOG_VU_METERS -> {
                    drawAnalogVuMeters(vuLeft, vuRight, skin)
                }
                VisualizerType.OSCILLOSCOPE_CRT -> {
                    drawOscilloscope(waveformPoints, skin)
                }
                VisualizerType.CIRCULAR_VORTEX -> {
                    drawCircularVisualizer(spectrumBands, skin)
                }
                VisualizerType.MATRIX_RAIN -> {
                    drawMatrixSpectrum(spectrumBands, skin)
                }
                VisualizerType.LYRICS_TELEPROMPTER -> {
                    // Handled as specialized Compose layout
                }
            }
        }
    }
}

private fun DrawScope.drawEqualizerBars(
    bands: FloatArray,
    peaks: FloatArray,
    skin: RetroSkin
) {
    val barCount = bands.size.coerceAtLeast(1)
    val totalWidth = size.width
    val totalHeight = size.height
    val spacing = 3.5.dp.toPx()
    val barWidth = (totalWidth - (spacing * (barCount - 1))) / barCount
    val segmentHeight = 6.dp.toPx()
    val segmentGap = 2.dp.toPx()
    val numSegments = (totalHeight / (segmentHeight + segmentGap)).toInt()

    // Draw subtle grid background
    for (s in 0 until numSegments) {
        val y = totalHeight - ((s + 1) * (segmentHeight + segmentGap))
        drawLine(
            color = skin.eqGridLineColor.copy(alpha = 0.35f),
            start = Offset(0f, y),
            end = Offset(totalWidth, y),
            strokeWidth = 1f
        )
    }

    // Draw bars
    for (i in 0 until barCount) {
        val x = i * (barWidth + spacing)
        val level = bands[i].coerceIn(0f, 1f)
        val activeSegments = (level * numSegments).toInt()

        for (s in 0 until numSegments) {
            val y = totalHeight - ((s + 1) * (segmentHeight + segmentGap))
            val isActive = s < activeSegments

            // Determine color from skin's EQ gradient based on segment height
            val fraction = s.toFloat() / numSegments
            val segColor = when {
                fraction > 0.8f -> skin.eqBarGradient.firstOrNull() ?: Color.Red
                fraction > 0.45f -> skin.eqBarGradient.getOrNull(1) ?: Color.Yellow
                else -> skin.eqBarGradient.lastOrNull() ?: Color.Green
            }

            if (isActive) {
                drawRoundRect(
                    color = segColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, segmentHeight),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
            } else {
                // Dim ghost segment
                drawRoundRect(
                    color = segColor.copy(alpha = 0.08f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, segmentHeight),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
            }
        }

        // Draw Peak Hold LED indicator
        val peakLevel = peaks.getOrElse(i) { 0f }.coerceIn(0f, 1f)
        val peakSegment = (peakLevel * (numSegments - 1)).toInt()
        val peakY = totalHeight - ((peakSegment + 1) * (segmentHeight + segmentGap))

        drawRoundRect(
            color = skin.eqPeakHoldColor,
            topLeft = Offset(x, peakY),
            size = Size(barWidth, segmentHeight),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )
    }
}

private fun DrawScope.drawAnalogVuMeters(
    vuLeft: Float,
    vuRight: Float,
    skin: RetroSkin
) {
    val meterWidth = size.width / 2.1f
    val meterHeight = size.height * 0.9f
    val gap = size.width * 0.05f

    // Draw Left VU Meter
    drawSingleVuMeter(
        offset = Offset((size.width - (meterWidth * 2 + gap)) / 2f, (size.height - meterHeight) / 2f),
        size = Size(meterWidth, meterHeight),
        value = vuLeft,
        channelName = "LEFT CH",
        skin = skin
    )

    // Draw Right VU Meter
    drawSingleVuMeter(
        offset = Offset((size.width - (meterWidth * 2 + gap)) / 2f + meterWidth + gap, (size.height - meterHeight) / 2f),
        size = Size(meterWidth, meterHeight),
        value = vuRight,
        channelName = "RIGHT CH",
        skin = skin
    )
}

private fun DrawScope.drawSingleVuMeter(
    offset: Offset,
    size: Size,
    value: Float,
    channelName: String,
    skin: RetroSkin
) {
    // Vintage meter faceplate
    drawRoundRect(
        color = skin.lcdBackground,
        topLeft = offset,
        size = size,
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )
    drawRoundRect(
        color = skin.chassisBorderColor,
        topLeft = offset,
        size = size,
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        style = Stroke(width = 2.dp.toPx())
    )

    val pivotX = offset.x + size.width / 2f
    val pivotY = offset.y + size.height * 0.95f
    val needleLength = size.height * 0.75f

    // Scale arc
    val startAngle = -135f
    val sweepAngle = 90f
    val angle = startAngle + (value.coerceIn(0f, 1f) * sweepAngle)
    val rad = Math.toRadians(angle.toDouble())

    val endX = pivotX + (needleLength * cos(rad)).toFloat()
    val endY = pivotY + (needleLength * sin(rad)).toFloat()

    // Meter scale tick marks
    for (step in 0..10) {
        val tickFrac = step / 10f
        val tickAngle = startAngle + (tickFrac * sweepAngle)
        val tickRad = Math.toRadians(tickAngle.toDouble())
        val r1 = needleLength * 0.85f
        val r2 = needleLength * 0.98f
        val t1X = pivotX + (r1 * cos(tickRad)).toFloat()
        val t1Y = pivotY + (r1 * sin(tickRad)).toFloat()
        val t2X = pivotX + (r2 * cos(tickRad)).toFloat()
        val t2Y = pivotY + (r2 * sin(tickRad)).toFloat()

        val tickColor = if (tickFrac > 0.75f) Color(0xFFFF3333) else skin.lcdTextColor
        drawLine(
            color = tickColor,
            start = Offset(t1X, t1Y),
            end = Offset(t2X, t2Y),
            strokeWidth = if (step % 5 == 0) 2.5f else 1.5f
        )
    }

    // Ballistic needle with shadow
    drawLine(
        color = Color(0x66000000),
        start = Offset(pivotX + 2f, pivotY + 2f),
        end = Offset(endX + 2f, endY + 2f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = if (value > 0.8f) Color(0xFFFF3333) else skin.accentColor,
        start = Offset(pivotX, pivotY),
        end = Offset(endX, endY),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )

    // Center pivot knob
    drawCircle(
        color = skin.chassisBevelHighlight,
        center = Offset(pivotX, pivotY),
        radius = 5.dp.toPx()
    )
}

private fun DrawScope.drawOscilloscope(
    waveform: FloatArray,
    skin: RetroSkin
) {
    if (waveform.isEmpty()) return

    val totalWidth = size.width
    val totalHeight = size.height
    val centerY = totalHeight / 2f

    // Draw CRT Green Grid
    val gridStep = 20.dp.toPx()
    var gx = 0f
    while (gx < totalWidth) {
        drawLine(
            color = skin.eqGridLineColor.copy(alpha = 0.35f),
            start = Offset(gx, 0f),
            end = Offset(gx, totalHeight),
            strokeWidth = 1f
        )
        gx += gridStep
    }
    var gy = 0f
    while (gy < totalHeight) {
        drawLine(
            color = skin.eqGridLineColor.copy(alpha = 0.35f),
            start = Offset(0f, gy),
            end = Offset(totalWidth, gy),
            strokeWidth = 1f
        )
        gy += gridStep
    }

    // Draw phosphor beam path
    val path = Path()
    val stepX = totalWidth / (waveform.size - 1)

    for (i in waveform.indices) {
        val x = i * stepX
        val y = centerY + (waveform[i] * centerY * 0.75f)
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    // Outer phosphor glow
    drawPath(
        path = path,
        color = skin.lcdGlowColor.copy(alpha = 0.45f),
        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    // Core sharp trace
    drawPath(
        path = path,
        color = skin.lcdTextColor,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawCircularVisualizer(
    bands: FloatArray,
    skin: RetroSkin
) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val baseRadius = (minOf(size.width, size.height) / 3f)

    val count = bands.size * 2
    for (i in 0 until count) {
        val bandIdx = if (i < bands.size) i else (count - 1 - i)
        val level = bands[bandIdx].coerceIn(0f, 1f)
        val angle = (i.toFloat() / count) * 360f
        val rad = Math.toRadians(angle.toDouble())

        val r1 = baseRadius
        val r2 = baseRadius + (level * baseRadius * 0.9f)

        val x1 = centerX + (r1 * cos(rad)).toFloat()
        val y1 = centerY + (r1 * sin(rad)).toFloat()
        val x2 = centerX + (r2 * cos(rad)).toFloat()
        val y2 = centerY + (r2 * sin(rad)).toFloat()

        drawLine(
            color = skin.accentColor.copy(alpha = 0.85f),
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    drawCircle(
        color = skin.lcdGlowColor.copy(alpha = 0.2f),
        center = Offset(centerX, centerY),
        radius = baseRadius
    )
}

private fun DrawScope.drawMatrixSpectrum(
    bands: FloatArray,
    skin: RetroSkin
) {
    drawEqualizerBars(bands, bands, skin)
}
