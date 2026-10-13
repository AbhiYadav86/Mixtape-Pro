package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.*
import com.example.data.model.RetroSkin
import kotlin.math.*

@Composable
fun SurgicalParametricEqView(
    equalizerController: RetroEqualizerController,
    skin: RetroSkin,
    modifier: Modifier = Modifier
) {
    var bands by remember { mutableStateOf(equalizerController.getParametricBands()) }
    var selectedBandId by remember { mutableIntStateOf(bands.firstOrNull()?.id ?: 1) }
    var currentPreset by remember { mutableStateOf(equalizerController.getCurrentParametricPreset()) }
    var preampGainDb by remember { mutableFloatStateOf(equalizerController.getParametricPreampGainDb()) }
    var isAbBypass by remember { mutableStateOf(equalizerController.isAbBypassActive()) }

    val selectedBand = bands.find { it.id == selectedBandId } ?: bands.firstOrNull()

    // Pulse animation for selected band node
    val infiniteTransition = rememberInfiniteTransition(label = "peqNodePulse")
    val pulseRingRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseRadius"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- 1. Top Bar: Header, A/B Audition & Presets ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Parametric EQ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = skin.accentColor,
                    maxLines = 1
                )
                Text(
                    text = "Drag the dots on the curve",
                    fontSize = 11.sp,
                    color = Color(0xFFB4BFCE),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // A/B Audition Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isAbBypass) Color(0xFFFFB800) else Color(0xFF1E2430))
                    .border(1.dp, if (isAbBypass) Color.White else Color(0xFF333E52), RoundedCornerShape(6.dp))
                    .clickable {
                        isAbBypass = !isAbBypass
                        equalizerController.setAbBypass(isAbBypass)
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isAbBypass) "BYPASSED" else "A/B",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAbBypass) Color.Black else Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // --- 2. Audiophile Target Curves / Presets Carousel ---
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ParametricPresets.ALL_PRESETS) { preset ->
                val isSelected = preset.id == currentPreset.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) skin.accentColor else Color(0xFF161B26))
                        .border(1.dp, if (isSelected) Color.White else Color(0xFF263045), RoundedCornerShape(6.dp))
                        .clickable {
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                            currentPreset = preset
                            equalizerController.applyParametricPreset(preset)
                            bands = equalizerController.getParametricBands()
                            preampGainDb = preset.preampGainDb
                            isAbBypass = false
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = preset.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // --- 3. Interactive Frequency Response Bode Plot Canvas ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF06080F))
                .border(1.dp, Color(0xFF222B3D), RoundedCornerShape(10.dp))
        ) {
            var activeDragBandId by remember { mutableStateOf<Int?>(null) }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(bands) {
                        detectTapGestures { tapOffset ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            // Find closest band node within touch radius (36dp)
                            val closest = bands.minByOrNull { band ->
                                val bx = freqToX(band.frequencyHz, w)
                                val by = gainToY(band.gainDb, h)
                                val dx = bx - tapOffset.x
                                val dy = by - tapOffset.y
                                dx * dx + dy * dy
                            }
                            if (closest != null) {
                                val bx = freqToX(closest.frequencyHz, w)
                                val by = gainToY(closest.gainDb, h)
                                val dist = sqrt((bx - tapOffset.x).pow(2) + (by - tapOffset.y).pow(2))
                                if (dist < 40.dp.toPx()) {
                                    selectedBandId = closest.id
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                }
                            }
                        }
                    }
                    .pointerInput(bands) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val closest = bands.minByOrNull { band ->
                                    val bx = freqToX(band.frequencyHz, w)
                                    val by = gainToY(band.gainDb, h)
                                    val dx = bx - startOffset.x
                                    val dy = by - startOffset.y
                                    dx * dx + dy * dy
                                }
                                if (closest != null) {
                                    val bx = freqToX(closest.frequencyHz, w)
                                    val by = gainToY(closest.gainDb, h)
                                    val dist = sqrt((bx - startOffset.x).pow(2) + (by - startOffset.y).pow(2))
                                    if (dist < 48.dp.toPx()) {
                                        activeDragBandId = closest.id
                                        selectedBandId = closest.id
                                    }
                                }
                            },
                            onDragEnd = {
                                activeDragBandId = null
                            },
                            onDragCancel = {
                                activeDragBandId = null
                            },
                            onDrag = { change, _ ->
                                activeDragBandId?.let { bandId ->
                                    change.consume()
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    val newFreq = xToFreq(change.position.x, w)
                                    val newGain = yToGain(change.position.y, h)

                                    equalizerController.setParametricBandFreq(bandId, newFreq)
                                    equalizerController.setParametricBandGain(bandId, newGain)
                                    bands = equalizerController.getParametricBands()
                                }
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height

                // A. Grid: Frequency Decades & Decibels
                val gridFreqs = listOf(20f, 50f, 100f, 200f, 500f, 1000f, 2000f, 5000f, 10000f, 20000f)
                gridFreqs.forEach { f ->
                    val x = freqToX(f, w)
                    val isMajor = f in listOf(100f, 1000f, 10000f)
                    drawLine(
                        color = if (isMajor) Color(0xFF2A3447) else Color(0xFF161E2E),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = if (isMajor) 1.2f else 0.8f
                    )
                }

                // Horizontal Decibel Grid (-12, -6, 0, +6, +12 dB)
                val gridDbs = listOf(-12f, -6f, 0f, 6f, 12f)
                gridDbs.forEach { db ->
                    val y = gainToY(db, h)
                    val isZero = db == 0f
                    drawLine(
                        color = if (isZero) Color(0xFF64748B) else Color(0xFF1E283B),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = if (isZero) 1.5f else 0.8f
                    )
                }

                // B. Composite Frequency Response Curve
                val curvePath = Path()
                val fillPath = Path()
                val zeroY = gainToY(0f, h)
                val numSamples = 120

                fillPath.moveTo(0f, zeroY)

                for (i in 0..numSamples) {
                    val px = (i.toFloat() / numSamples) * w
                    val freq = xToFreq(px, w)
                    val totalDb = equalizerController.getCompositeParametricGainDb(freq)
                    val py = gainToY(totalDb, h)

                    if (i == 0) {
                        curvePath.moveTo(px, py)
                        fillPath.lineTo(px, py)
                    } else {
                        curvePath.lineTo(px, py)
                        fillPath.lineTo(px, py)
                    }
                }

                fillPath.lineTo(w, zeroY)
                fillPath.close()

                // Draw Gradient Underfill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            skin.accentColor.copy(alpha = 0.28f),
                            skin.accentColor.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )

                // Draw Glowing Composite Curve
                drawPath(
                    path = curvePath,
                    color = skin.accentColor,
                    style = Stroke(width = 2.8f)
                )

                // C. Draw Parametric Band Nodes
                bands.forEach { band ->
                    val bx = freqToX(band.frequencyHz, w)
                    val by = gainToY(band.gainDb, h)
                    val isBandSelected = band.id == selectedBandId
                    val nodeColor = Color(band.colorHex)

                    if (isBandSelected) {
                        // Animated pulsing halo
                        drawCircle(
                            color = nodeColor.copy(alpha = 0.35f),
                            radius = pulseRingRadius,
                            center = Offset(bx, by)
                        )
                    }

                    // Node Outer Ring
                    drawCircle(
                        color = if (isBandSelected) Color.White else Color(0xFF0F172A),
                        radius = if (isBandSelected) 9.5f else 7.5f,
                        center = Offset(bx, by)
                    )

                    // Node Inner Core
                    drawCircle(
                        color = if (band.isEnabled) nodeColor else Color(0xFF64748B),
                        radius = if (isBandSelected) 7.5f else 5.5f,
                        center = Offset(bx, by)
                    )
                }
            }

            // Grid Labels Overlaid (Top Left & Top Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("+18dB", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8592A6), maxLines = 1, softWrap = false)
                Text("20kHz", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF8592A6), maxLines = 1, softWrap = false)
            }
        }

        // Selected band read-out (below the graph so it never collides with the axis labels)
        selectedBand?.let { sb ->
            Text(
                text = "B${sb.id}  ${formatFreq(sb.frequencyHz)}  ${formatGain(sb.gainDb)}  Q ${String.format("%.2f", sb.q)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(sb.colorHex),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // --- 4. Band Selection Strip (B1 .. B6) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            bands.forEach { b ->
                val isSelected = b.id == selectedBandId
                val bandColor = Color(b.colorHex)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) bandColor.copy(alpha = 0.25f) else Color(0xFF141923))
                        .border(
                            1.2.dp,
                            if (isSelected) bandColor else Color(0xFF263045),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            selectedBandId = b.id
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                        }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (b.isEnabled) bandColor else Color(0xFF64748B))
                            )
                            Text(
                                text = "B${b.id}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                            )
                        }
                        Text(
                            text = formatFreq(b.frequencyHz),
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) bandColor else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // --- 5. Selected Band Precision Editing Deck ---
        selectedBand?.let { band ->
            val bandColor = Color(band.colorHex)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121620)),
                border = androidx.compose.foundation.BorderStroke(1.dp, bandColor.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Band Header: Title, Bypass Switch & Filter Type Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(bandColor)
                            )
                            Text(
                                text = "Band ${band.id}: ${band.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Band Bypass Toggle
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (band.isEnabled) "ACTIVE" else "MUTED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (band.isEnabled) Color(0xFF4ADE80) else Color(0xFF94A3B8)
                            )
                            Switch(
                                checked = band.isEnabled,
                                onCheckedChange = {
                                    equalizerController.setParametricBandEnabled(band.id, it)
                                    bands = equalizerController.getParametricBands()
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                                },
                                modifier = Modifier.scale(0.7f)
                            )
                        }
                    }

                    // Filter Shape Selector (PEAK, LOW SHELF, HIGH SHELF, NOTCH, LOW CUT, HIGH CUT)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(FilterType.values()) { fType ->
                            val isCurrentType = band.filterType == fType
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isCurrentType) bandColor else Color(0xFF1E2636))
                                    .border(1.dp, if (isCurrentType) Color.White else Color(0xFF334155), RoundedCornerShape(4.dp))
                                    .clickable {
                                        equalizerController.setParametricBandType(band.id, fType)
                                        bands = equalizerController.getParametricBands()
                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = fType.shortName,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentType) Color.Black else Color.White
                                )
                            }
                        }
                    }

                    // --- Variable Q (Bandwidth) Precision Section ---
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Quality Factor (Q):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = String.format("%.2f", band.q),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = bandColor
                                )
                                Text(
                                    text = "(${String.format("%.2f", band.getOctaveBandwidth())} oct)",
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            // Q Character descriptor
                            val qDescriptor = when {
                                band.q > 8.0f -> "SURGICAL NOTCH"
                                band.q > 3.0f -> "NARROW CORRECTION"
                                band.q > 1.2f -> "MASTERING BELL"
                                else -> "WIDE MUSICAL"
                            }
                            Text(
                                text = qDescriptor,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (band.q > 5.0f) Color(0xFFFF5252) else bandColor
                            )
                        }

                        // Q Slider (Log-proportional response from 0.25 to 20.0)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Nudge Q down
                            IconButton(
                                onClick = {
                                    val newQ = (band.q * 0.85f).coerceIn(0.25f, 20.0f)
                                    equalizerController.setParametricBandQ(band.id, newQ)
                                    bands = equalizerController.getParametricBands()
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Narrow Q", tint = Color.White, modifier = Modifier.size(14.dp))
                            }

                            Slider(
                                value = log10(band.q.coerceIn(0.25f, 20f)),
                                onValueChange = {
                                    val newQ = 10.0.pow(it.toDouble()).toFloat().coerceIn(0.25f, 20.0f)
                                    equalizerController.setParametricBandQ(band.id, newQ)
                                    bands = equalizerController.getParametricBands()
                                },
                                valueRange = log10(0.25f)..log10(20.0f),
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = bandColor,
                                    activeTrackColor = bandColor,
                                    inactiveTrackColor = Color(0xFF263045)
                                )
                            )

                            // Nudge Q up
                            IconButton(
                                onClick = {
                                    val newQ = (band.q * 1.18f).coerceIn(0.25f, 20.0f)
                                    equalizerController.setParametricBandQ(band.id, newQ)
                                    bands = equalizerController.getParametricBands()
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Widen Q", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }

                        // Quick Q Presets row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(
                                Pair("Wide (0.7)", 0.707f),
                                Pair("Musical (1.4)", 1.414f),
                                Pair("Precision (3.0)", 3.0f),
                                Pair("Surgical (8.0)", 8.0f),
                                Pair("Notch (14.0)", 14.0f)
                            ).forEach { (label, qVal) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFF1B2230))
                                        .clickable {
                                            equalizerController.setParametricBandQ(band.id, qVal)
                                            bands = equalizerController.getParametricBands()
                                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(label, fontSize = 8.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }

                    // --- Frequency Precision Slider Section ---
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Center Frequency:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                text = "${formatFreq(band.frequencyHz)} (${band.frequencyHz.toInt()} Hz)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = bandColor
                            )
                        }

                        Slider(
                            value = log10(band.frequencyHz.coerceIn(20f, 20000f)),
                            onValueChange = {
                                val newFreq = 10.0.pow(it.toDouble()).toFloat().coerceIn(20f, 20000f)
                                equalizerController.setParametricBandFreq(band.id, newFreq)
                                bands = equalizerController.getParametricBands()
                            },
                            valueRange = log10(20f)..log10(20000f),
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = bandColor,
                                activeTrackColor = bandColor,
                                inactiveTrackColor = Color(0xFF263045)
                            )
                        )
                    }

                    // --- Gain Precision Slider Section ---
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gain (dB):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = formatGain(band.gainDb),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (band.gainDb > 0) Color(0xFFFF5252) else if (band.gainDb == 0f) Color.White else Color(0xFF4ADE80)
                                )
                                TextButton(
                                    onClick = {
                                        equalizerController.setParametricBandGain(band.id, 0f)
                                        bands = equalizerController.getParametricBands()
                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text("Reset 0dB", fontSize = 9.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        }

                        Slider(
                            value = band.gainDb,
                            onValueChange = {
                                equalizerController.setParametricBandGain(band.id, it)
                                bands = equalizerController.getParametricBands()
                            },
                            valueRange = -15.0f..15.0f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = bandColor,
                                activeTrackColor = bandColor,
                                inactiveTrackColor = Color(0xFF263045)
                            )
                        )
                    }
                }
            }
        }

        // --- 6. Preamp Headroom Gain Slider ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141923), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF263045), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("PEQ Headroom Guard:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = "${String.format("%+.1f", preampGainDb)} dB",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (preampGainDb <= 0) Color(0xFF4ADE80) else Color(0xFFFF5252)
                    )
                }
                Text("Preamp attenuation to prevent digital clipping", fontSize = 8.5.sp, color = Color(0xFF94A3B8))
            }

            Slider(
                value = preampGainDb,
                onValueChange = {
                    preampGainDb = it
                    equalizerController.setParametricPreampGainDb(it)
                },
                valueRange = -12.0f..6.0f,
                modifier = Modifier.width(130.dp),
                colors = SliderDefaults.colors(
                    thumbColor = skin.accentColor,
                    activeTrackColor = skin.accentColor,
                    inactiveTrackColor = Color(0xFF263045)
                )
            )
        }
    }
}

// Coordinate conversions between frequency/dB and screen pixels
private fun freqToX(freq: Float, width: Float): Float {
    val fClamped = freq.coerceIn(20f, 20000f)
    val logMin = log10(20.0)
    val logMax = log10(20000.0)
    val logVal = log10(fClamped.toDouble())
    return (((logVal - logMin) / (logMax - logMin)) * width).toFloat().coerceIn(0f, width)
}

private fun xToFreq(x: Float, width: Float): Float {
    val fraction = (x / width).coerceIn(0f, 1f).toDouble()
    val logMin = log10(20.0)
    val logMax = log10(20000.0)
    val logVal = logMin + fraction * (logMax - logMin)
    return 10.0.pow(logVal).toFloat().coerceIn(20f, 20000f)
}

private fun gainToY(gainDb: Float, height: Float): Float {
    // Range: -18 dB (bottom) to +18 dB (top)
    val gClamped = gainDb.coerceIn(-18f, 18f)
    val fraction = (0.5f - (gClamped / 36.0f)).coerceIn(0f, 1f)
    return fraction * height
}

private fun yToGain(y: Float, height: Float): Float {
    val fraction = (y / height).coerceIn(0f, 1f)
    return ((0.5f - fraction) * 36.0f).coerceIn(-15.0f, 15.0f)
}

private fun formatFreq(freq: Float): String {
    return if (freq >= 1000f) {
        String.format("%.1fkHz", freq / 1000f)
    } else {
        "${freq.toInt()}Hz"
    }
}

private fun formatGain(gainDb: Float): String {
    return String.format("%+.1f dB", gainDb)
}
