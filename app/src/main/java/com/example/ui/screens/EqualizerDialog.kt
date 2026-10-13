package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.*
import com.example.data.model.DiscDisplayMode
import com.example.data.model.RetroSkin
import com.example.data.model.VisualizerType

@Composable
fun EqualizerDialog(
    equalizerController: RetroEqualizerController,
    currentVisualizerType: VisualizerType,
    currentDiscDisplayMode: DiscDisplayMode,
    skin: RetroSkin,
    onVisualizerTypeSelected: (VisualizerType) -> Unit,
    onDiscDisplayModeSelected: (DiscDisplayMode) -> Unit,
    onDismiss: () -> Unit
) {
    var bands by remember { mutableStateOf(equalizerController.getCurrentBands()) }
    var preampGain by remember { mutableFloatStateOf(equalizerController.getPreampGain()) }
    var bassBoost by remember { mutableFloatStateOf(equalizerController.getBassBoostLevel()) }
    var selectedPreset by remember { mutableStateOf(equalizerController.getCurrentPreset()) }
    var selectedReverb by remember { mutableStateOf(equalizerController.getCurrentReverbPreset()) }
    var isEqEnabled by remember { mutableStateOf(equalizerController.isEnabled()) }
    var isAbBypass by remember { mutableStateOf(equalizerController.isAbBypassActive()) }
    var eqMode by remember { mutableStateOf(equalizerController.getEqualizerMode()) }

    // DSP Audiophile modules
    var isTubeWarmthEnabled by remember { mutableStateOf(equalizerController.isTubeWarmthEnabled()) }
    var tubeWarmthDrive by remember { mutableFloatStateOf(equalizerController.getTubeWarmthDrive()) }
    var isCrossfeedEnabled by remember { mutableStateOf(equalizerController.isStereoCrossfeedEnabled()) }
    var crossfeedWidth by remember { mutableFloatStateOf(equalizerController.getStereoCrossfeedWidth()) }
    var replayGainMode by remember { mutableStateOf(equalizerController.getReplayGainMode()) }

    var isSoundFXEnabled by remember { mutableStateOf(RetroSoundEffects.isSoundEnabled) }

    // 10 Audiophile Frequency Bands
    val bandLabels = listOf(
        Pair("31Hz", "SUB"),
        Pair("62Hz", "DEEP"),
        Pair("125Hz", "PUNCH"),
        Pair("250Hz", "WARM"),
        Pair("500Hz", "BODY"),
        Pair("1kHz", "MID"),
        Pair("2kHz", "PRES"),
        Pair("4kHz", "CLAR"),
        Pair("8kHz", "SPRK"),
        Pair("16kHz", "AIR")
    )

    // Vacuum tube pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "tubeGlow")
    val tubeGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tubeAlpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.95f)
                .testTag("equalizer_dialog")
                .shadow(20.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10131A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, skin.chassisBorderColor, RoundedCornerShape(16.dp))
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with icon, title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Audio DSP",
                        tint = skin.accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Equalizer & DSP",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "Graphic & parametric EQ • Tube warmth • Crossfeed",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(0xFFB4BFCE),
                            maxLines = 2
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Master EQ on/off + A/B audition, stacked so nothing gets clipped
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161B26), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF263045), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "EQ Master",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (!isEqEnabled) "Off (flat)" else if (isAbBypass) "A/B: hearing original" else "On",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isEqEnabled) Color(0xFF94A3B8) else if (isAbBypass) Color(0xFFFFB800) else Color(0xFF00FF66),
                                maxLines = 1
                            )
                        }
                        Switch(
                            checked = isEqEnabled,
                            onCheckedChange = {
                                isEqEnabled = it
                                equalizerController.setEnabled(it)
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = skin.accentColor, checkedThumbColor = Color.Black)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Quick A/B Audition Button (full width)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isAbBypass) Color(0xFFFFB800) else Color(0xFF252D3D))
                            .border(1.dp, if (isAbBypass) Color.White else Color(0xFF4A5568), RoundedCornerShape(6.dp))
                            .clickable {
                                isAbBypass = !isAbBypass
                                equalizerController.setAbBypass(isAbBypass)
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAbBypass) "A/B: ORIGINAL (tap to hear EQ)" else "A/B: COMPARE WITH ORIGINAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAbBypass) Color.Black else Color(0xFFE2E8F0),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Equalizer Mode Switcher (10-Band Graphic vs Surgical Parametric PEQ) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141923))
                        .border(1.dp, Color(0xFF263045), RoundedCornerShape(8.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EqualizerMode.values().forEach { mode ->
                        val isSelected = eqMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) skin.accentColor else Color.Transparent)
                                .clickable {
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                    eqMode = mode
                                    equalizerController.setEqualizerMode(mode)
                                }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (mode == EqualizerMode.SURGICAL_PARAMETRIC) Icons.Default.Tune else Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.Black else Color(0xFF94A3B8),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (mode == EqualizerMode.SURGICAL_PARAMETRIC) "Parametric" else "10-Band",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (eqMode == EqualizerMode.SURGICAL_PARAMETRIC) {
                    // --- Surgical Parametric EQ Engine with Variable Q ---
                    SurgicalParametricEqView(
                        equalizerController = equalizerController,
                        skin = skin
                    )
                } else {
                    // --- 10-Band Graphic Equalizer Section ---
                    // Audiophile & Retro EQ Presets Carousel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Acoustic Target Curves & Presets",
                            fontSize = 12.5.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = skin.accentColor
                        )

                        TextButton(
                            onClick = {
                                selectedPreset = EqualizerPresets.FLAT
                                equalizerController.applyPreset(EqualizerPresets.FLAT)
                                bands = EqualizerPresets.FLAT.bandLevels
                                preampGain = 0f
                                bassBoost = 0f
                                tubeWarmthDrive = 0f
                                crossfeedWidth = 0.2f
                                isAbBypass = false
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF94A3B8))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Reset Direct", fontSize = 11.sp, fontFamily = FontFamily.SansSerif, color = Color(0xFF94A3B8))
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(EqualizerPresets.ALL_PRESETS) { preset ->
                            val isSelected = preset.id == selectedPreset.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) skin.accentColor else Color(0xFF1E2430))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.White else Color(0xFF333E52),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                                        selectedPreset = preset
                                        equalizerController.applyPreset(preset)
                                        bands = equalizerController.getCurrentBands()
                                        preampGain = preset.preampGain
                                        bassBoost = preset.bassBoost
                                        tubeWarmthDrive = preset.tubeWarmth
                                        crossfeedWidth = preset.stereoCrossfeed
                                        isAbBypass = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Column {
                                    Text(
                                        text = preset.name,
                                        fontSize = 11.5.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preamp Gain & Headroom Slider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161B26), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Preamp Headroom Gain",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                val preampDb = (preampGain * 12).toInt()
                                Text(
                                    text = if (preampDb > 0) "+$preampDb dB" else "$preampDb dB",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (preampDb < 0) Color(0xFF4ADE80) else if (preampDb == 0) Color.White else Color(0xFFFF5252)
                                )
                            }
                            Text(
                                text = "Prevents inter-sample clipping when boosting EQ bands",
                                fontSize = 9.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Slider(
                            value = preampGain,
                            onValueChange = {
                                preampGain = it
                                equalizerController.setPreampGain(it)
                            },
                            valueRange = -1.0f..1.0f,
                            modifier = Modifier.width(130.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = skin.accentColor,
                                activeTrackColor = skin.accentColor,
                                inactiveTrackColor = Color(0xFF263045)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 10-Band Graphic Equalizer Slider Rack
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "10-Band Precision Graphic Filter (-12dB .. +12dB)",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = skin.lcdTextColor
                        )
                        Text(
                            text = "Touch / Drag Bars",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF090B10)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, skin.chassisBorderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bandLabels.forEachIndexed { index, (freqLabel, subLabel) ->
                                val level = bands.getOrElse(index) { 0f }
                                InteractiveEqBandColumn(
                                    freqLabel = freqLabel,
                                    subLabel = subLabel,
                                    level = level,
                                    accentColor = skin.accentColor,
                                    onLevelChanged = { newLvl ->
                                        val updated = bands.toMutableList()
                                        while (updated.size <= index) {
                                            updated.add(0f)
                                        }
                                        updated[index] = newLvl.coerceIn(-1.0f, 1.0f)
                                        bands = updated
                                        equalizerController.setBandLevel(index, newLvl)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Audiophile DSP Modules: Analog Tube Warmth & Stereo Crossfeed ---
                Text(
                    text = "Audiophile DSP Modules",
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 1. Analog Vacuum Tube & Tape Warmth Module
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151922)),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isTubeWarmthEnabled) Color(0xFFFF9900).copy(alpha = 0.6f) else Color(0xFF263045)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Glowing Tube Indicator Light
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isTubeWarmthEnabled) Color(0xFFFF7700).copy(alpha = tubeGlowAlpha)
                                            else Color(0xFF4A3020)
                                        )
                                        .border(1.dp, Color(0xFFFFB366), CircleShape)
                                )
                                Column {
                                    Text(
                                        text = "Analog Tube & Tape Warmth Simulator",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTubeWarmthEnabled) Color(0xFFFFB84D) else Color.White
                                    )
                                    Text(
                                        text = "2nd-Order harmonic saturation & analog tape compression",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Switch(
                                checked = isTubeWarmthEnabled,
                                onCheckedChange = {
                                    isTubeWarmthEnabled = it
                                    equalizerController.setTubeWarmthEnabled(it)
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                                },
                                modifier = Modifier.scale(0.8f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFFF9900),
                                    checkedTrackColor = Color(0xFF663300)
                                )
                            )
                        }

                        if (isTubeWarmthEnabled) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Warmth Drive: ${(tubeWarmthDrive * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB84D)
                                )
                                Text(
                                    text = if (tubeWarmthDrive > 0.6f) "RICH TUBE GLOW" else if (tubeWarmthDrive > 0.2f) "SUBTLE HARMONICS" else "WARM FLAT",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9900)
                                )
                            }
                            Slider(
                                value = tubeWarmthDrive,
                                onValueChange = {
                                    tubeWarmthDrive = it
                                    equalizerController.setTubeWarmthDrive(it)
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFF9900),
                                    activeTrackColor = Color(0xFFFF7700),
                                    inactiveTrackColor = Color(0xFF332014)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Headphone Stereo Crossfeed & Soundstage Expander (Bauer Binaural)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151922)),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCrossfeedEnabled) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color(0xFF263045)
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Headphones,
                                    contentDescription = "Crossfeed",
                                    tint = if (isCrossfeedEnabled) Color(0xFF38BDF8) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Bauer Stereo Crossfeed (Headphone 3D)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCrossfeedEnabled) Color(0xFF38BDF8) else Color.White
                                    )
                                    Text(
                                        text = "Loudspeaker cross-talk simulation to eliminate listening fatigue",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Switch(
                                checked = isCrossfeedEnabled,
                                onCheckedChange = {
                                    isCrossfeedEnabled = it
                                    equalizerController.setStereoCrossfeedEnabled(it)
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                                },
                                modifier = Modifier.scale(0.8f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF38BDF8),
                                    checkedTrackColor = Color(0xFF0C4A6E)
                                )
                            )
                        }

                        if (isCrossfeedEnabled) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Soundstage Width: ${(crossfeedWidth * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                                Text(
                                    text = if (crossfeedWidth > 0.6f) "WIDE STAGE" else "NATURAL BINAURAL",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7)
                                )
                            }
                            Slider(
                                value = crossfeedWidth,
                                onValueChange = {
                                    crossfeedWidth = it
                                    equalizerController.setStereoCrossfeedWidth(it)
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF38BDF8),
                                    activeTrackColor = Color(0xFF0284C7),
                                    inactiveTrackColor = Color(0xFF1E293B)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. ReplayGain & Dynamic Limiter Selector
                Text(
                    text = "ReplayGain & Dynamic Loudness Normalization",
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReplayGainMode.values().forEach { mode ->
                        val isSelected = mode == replayGainMode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) skin.accentColor else Color(0xFF1B202C))
                                .border(1.dp, if (isSelected) Color.White else Color(0xFF333E52), RoundedCornerShape(6.dp))
                                .clickable {
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                    replayGainMode = mode
                                    equalizerController.setReplayGainMode(mode)
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mega Bass Subwoofer Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mega Bass Subwoofer Hardware Boost",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = skin.secondaryAccentColor
                    )
                    Text(
                        text = "${(bassBoost * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = skin.secondaryAccentColor
                    )
                }

                Slider(
                    value = bassBoost,
                    onValueChange = {
                        bassBoost = it
                        equalizerController.setBassBoost(it)
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = skin.secondaryAccentColor,
                        activeTrackColor = skin.secondaryAccentColor,
                        inactiveTrackColor = Color(0xFF263045)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Spatial Reverb Room Acoustics
                Text(
                    text = "Acoustic Reverb Chamber",
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SpatialReverbPresets.ALL_REVERBS) { rev ->
                        val isSelected = rev.id == selectedReverb.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) skin.secondaryAccentColor else Color(0xFF1E2430))
                                .border(
                                    1.dp,
                                    if (isSelected) Color.White else Color(0xFF333E52),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                                    selectedReverb = rev
                                    equalizerController.applyReverbPreset(rev)
                                }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = rev.name,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Technical Stream Telemetry Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0E14)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222B3D))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STREAM & DSP TELEMETRY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "BIT-PERFECT DIRECT",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF66)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Format: FLAC 24-bit / 96kHz", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                Text("Dynamic Range: 114 dB", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF94A3B8))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("DSP Latency: 1.2 ms", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                                Text("Bitrate: 1411 kbps", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = skin.accentColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Spectrum Visualizer Mode Selector
                Text(
                    text = "Spectrum Visualizer Mode",
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = skin.lcdTextColor
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(VisualizerType.values()) { visType ->
                        val isSelected = visType == currentVisualizerType
                        val label = when (visType) {
                            VisualizerType.CLASSIC_BARS_PEAK -> "Bars & Peaks"
                            VisualizerType.ANALOG_VU_METERS -> "VU Meters"
                            VisualizerType.OSCILLOSCOPE_CRT -> "Oscilloscope"
                            VisualizerType.CIRCULAR_VORTEX -> "Vortex"
                            VisualizerType.MATRIX_RAIN -> "Matrix Rain"
                            VisualizerType.LYRICS_TELEPROMPTER -> "Lyrics Prompter"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) skin.lcdTextColor else Color(0xFF1E2430))
                                .border(
                                    1.dp,
                                    if (isSelected) Color.White else Color(0xFF333E52),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                    onVisualizerTypeSelected(visType)
                                }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Disc Display Style Selector
                Text(
                    text = "Disc Jewel Case & Spin Style",
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(DiscDisplayMode.values()) { discMode ->
                        val isSelected = discMode == currentDiscDisplayMode
                        val label = when (discMode) {
                            DiscDisplayMode.CD_JEWEL_CASE -> "CD Jewel Case"
                            DiscDisplayMode.CASSETTE_TAPE -> "Cassette Tape"
                            DiscDisplayMode.VINYL_RECORD -> "Vinyl Record"
                            DiscDisplayMode.CRT_MONITOR -> "CRT Monitor"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) skin.accentColor else Color(0xFF1E2430))
                                .border(
                                    1.dp,
                                    if (isSelected) Color.White else Color(0xFF333E52),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                    onDiscDisplayModeSelected(discMode)
                                }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sound FX Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161B26), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Sound FX",
                            tint = skin.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "90s Mechanical Click Audio Feedback",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Switch(
                        checked = isSoundFXEnabled,
                        onCheckedChange = {
                            isSoundFXEnabled = it
                            RetroSoundEffects.isSoundEnabled = it
                            if (it) RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                        },
                        modifier = Modifier.scale(0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Apply & Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor)
                ) {
                    Text(
                        text = "Apply DSP & Return to Player",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveEqBandColumn(
    freqLabel: String,
    subLabel: String,
    level: Float, // -1f .. 1f
    accentColor: Color,
    onLevelChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSegments = 14
    val currentDb = (level * 12).toInt()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 1.dp)
    ) {
        // Nudge Up (+) button
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E2430))
                .clickable {
                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                    onLevelChanged((level + 0.166f).coerceIn(-1f, 1f))
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Boost", tint = Color.White, modifier = Modifier.size(12.dp))
        }

        Spacer(modifier = Modifier.height(2.dp))

        // dB Readout
        Text(
            text = if (currentDb > 0) "+$currentDb" else "$currentDb",
            fontSize = 8.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = if (currentDb > 0) Color(0xFFFF5252) else if (currentDb == 0) Color.White else Color(0xFF4ADE80)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Interactive LED Bar Column with full tap + drag hit area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF06070B))
                .border(1.dp, Color(0xFF222838), RoundedCornerShape(3.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val fraction = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        val newLevel = (fraction * 2f - 1f).coerceIn(-1f, 1f)
                        onLevelChanged(newLevel)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val fraction = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        val newLevel = (fraction * 2f - 1f).coerceIn(-1f, 1f)
                        onLevelChanged(newLevel)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 2.dp)) {
                val w = size.width
                val h = size.height
                val segHeight = (h - (totalSegments - 1) * 1.5f) / totalSegments
                val activeCount = (((level + 1f) * 0.5f) * totalSegments).toInt().coerceIn(1, totalSegments)

                // 0dB Center reference line
                val zeroY = h * 0.5f
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(0f, zeroY),
                    end = Offset(w, zeroY),
                    strokeWidth = 1f
                )

                for (i in 0 until totalSegments) {
                    val segIndexFromBottom = totalSegments - 1 - i
                    val segY = i * (segHeight + 1.5f)
                    val isLit = segIndexFromBottom < activeCount

                    val segColor = when {
                        segIndexFromBottom >= 11 -> Color(0xFFFF2255) // Red Peak (+8dB to +12dB)
                        segIndexFromBottom >= 7 -> Color(0xFFFFB800)  // Amber (+2dB to +7dB)
                        else -> Color(0xFF00E676)                     // Green Clear (-12dB to +1dB)
                    }

                    drawRoundRect(
                        color = if (isLit) segColor else segColor.copy(alpha = 0.12f),
                        topLeft = Offset(0f, segY),
                        size = Size(w, segHeight),
                        cornerRadius = CornerRadius(1.2f, 1.2f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Nudge Down (-) button
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E2430))
                .clickable {
                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                    onLevelChanged((level - 0.166f).coerceIn(-1f, 1f))
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Cut", tint = Color.White, modifier = Modifier.size(12.dp))
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Frequency Label & Band Name
        Text(
            text = freqLabel,
            fontSize = 8.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )
        Text(
            text = subLabel,
            fontSize = 6.5.sp,
            fontFamily = FontFamily.SansSerif,
            color = Color(0xFF94A3B8),
            maxLines = 1
        )
    }
}
