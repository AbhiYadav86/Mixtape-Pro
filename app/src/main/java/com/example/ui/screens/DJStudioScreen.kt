package com.example.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RetroEqualizerController
import com.example.audio.RetroSoundEffects
import com.example.data.model.RetroSkin
import com.example.data.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

enum class StudioButtonTheme {
    GREEN_PLAY,     // 90s emerald transport play
    RED_REC,        // 90s crimson record/cut
    AMBER_CUE,      // 90s amber cue/solo
    CYAN_SYNC,      // 90s cyan sync/fx
    SLATE_UTILITY,  // 90s brushed slate/console
    CHAMPAGNE_GOLD  // 90s luxury gold master export
}

/**
 * Authentic Retro Hardware Console Button adapted to the active RetroSkin.
 * Built with realistic 3D molded cap, recessed chassis socket, chamfered specular highlights,
 * physical bottom extrusion ledge, domed LED lens, and high-contrast legible typography.
 */
@Composable
fun StudioHardwareButton(
    text: String,
    onClick: () -> Unit,
    skin: RetroSkin,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    theme: StudioButtonTheme = StudioButtonTheme.SLATE_UTILITY,
    isActive: Boolean = false,
    height: Dp = 38.dp,
    subText: String? = null,
    fontSize: TextUnit = 10.5.sp,
    showLed: Boolean = true,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "studio_btn_press_y"
    )

    val ledColor = when (theme) {
        StudioButtonTheme.GREEN_PLAY -> Color(0xFF00E676)
        StudioButtonTheme.RED_REC -> Color(0xFFFF3D00)
        StudioButtonTheme.AMBER_CUE -> Color(0xFFFFC107)
        StudioButtonTheme.CYAN_SYNC -> skin.accentColor
        StudioButtonTheme.SLATE_UTILITY -> skin.secondaryAccentColor
        StudioButtonTheme.CHAMPAGNE_GOLD -> Color(0xFFFFD54F)
    }

    if (skin.isWin98Style) {
        val winFace = if (isPressed || isActive) Color(0xFFB0B0B0) else Color(0xFFC0C0C0)
        val borderTopLeft = if (isPressed || isActive) Color(0xFF404040) else Color.White
        val borderBottomRight = if (isPressed || isActive) Color.White else Color(0xFF404040)

        Box(
            modifier = modifier
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .height(height)
                .background(winFace)
                .border(1.dp, Color.Black)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.RETRO_CLICK)
                    onClick()
                }
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawLine(borderTopLeft, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 2f)
                drawLine(borderTopLeft, Offset(0f, 0f), Offset(0f, h), strokeWidth = 2f)
                drawLine(borderBottomRight, Offset(0f, h), Offset(w, h), strokeWidth = 2f)
                drawLine(borderBottomRight, Offset(w, 0f), Offset(w, h), strokeWidth = 2f)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                if (showLed) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF202020))
                            .border(0.5.dp, Color(0xFF909090), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isActive) ledColor else Color(0xFF505050))
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size((height.value * 0.38f).dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = text,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subText != null) {
                        Text(
                            text = subText,
                            fontSize = (fontSize.value * 0.78f).sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(0xFF333333),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    } else {
        val baseGradient = when (theme) {
            StudioButtonTheme.GREEN_PLAY -> if (isActive) listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)) else listOf(Color(0xFF162B1A), Color(0xFF1F3D25))
            StudioButtonTheme.RED_REC -> if (isActive) listOf(Color(0xFFB71C1C), Color(0xFFD32F2F)) else listOf(Color(0xFF331515), Color(0xFF451C1C))
            StudioButtonTheme.AMBER_CUE -> if (isActive) listOf(Color(0xFFE65100), Color(0xFFF57C00)) else listOf(Color(0xFF38230D), Color(0xFF4E3012))
            StudioButtonTheme.CYAN_SYNC -> if (isActive) listOf(skin.accentColor.copy(alpha = 0.6f), skin.accentColor) else listOf(Color(0xFF102533), Color(0xFF163445))
            StudioButtonTheme.SLATE_UTILITY -> if (isActive) listOf(Color(0xFF334155), Color(0xFF475569)) else listOf(Color(0xFF242C3E), Color(0xFF161E2D))
            StudioButtonTheme.CHAMPAGNE_GOLD -> if (isActive) listOf(Color(0xFFFFD54F), Color(0xFFFFCA28)) else listOf(Color(0xFF3B331A), Color(0xFF524724))
        }

        // High-contrast, crystal-clear typography colors
        val contentColor = when (theme) {
            StudioButtonTheme.GREEN_PLAY -> if (isActive) Color(0xFFE8F5E9) else Color(0xFFA5D6A7)
            StudioButtonTheme.RED_REC -> if (isActive) Color(0xFFFFEBEE) else Color(0xFFFFCDD2)
            StudioButtonTheme.AMBER_CUE -> if (isActive) Color(0xFFFFF8E1) else Color(0xFFFFE082)
            StudioButtonTheme.CYAN_SYNC -> if (isActive) Color.White else skin.accentColor
            StudioButtonTheme.SLATE_UTILITY -> if (isActive) Color.White else Color(0xFFF1F5F9)
            StudioButtonTheme.CHAMPAGNE_GOLD -> if (isActive) Color(0xFF1A1A1A) else Color(0xFFFFF9C4)
        }

        val cornerRadius = skin.buttonShapeRounded.coerceIn(4, 8).dp

        // 1. Recessed Console Housing Well
        Box(
            modifier = modifier
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .height(height)
                .background(Color(0xFF07090F), RoundedCornerShape(cornerRadius))
                .border(1.2.dp, Color(0x60000000), RoundedCornerShape(cornerRadius))
                .padding(1.dp),
            contentAlignment = Alignment.Center
        ) {
            // 2. Physical 3D Molded Moving Button Cap
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = pressOffsetY.toPx()
                        scaleX = if (isPressed) 0.98f else 1.0f
                        scaleY = if (isPressed) 0.98f else 1.0f
                    }
                    .shadow(if (isPressed) 0.5.dp else 3.5.dp, RoundedCornerShape((cornerRadius - 1.dp).coerceAtLeast(2.dp)))
                    .clip(RoundedCornerShape((cornerRadius - 1.dp).coerceAtLeast(2.dp)))
                    .background(brush = Brush.verticalGradient(baseGradient))
                    .border(
                        width = if (isPressed || isActive) 1.2.dp else 1.dp,
                        color = if (isActive) ledColor.copy(alpha = 0.85f)
                                else if (isPressed) skin.chassisBevelHighlight.copy(alpha = 0.7f)
                                else Color(0x35FFFFFF),
                        shape = RoundedCornerShape((cornerRadius - 1.dp).coerceAtLeast(2.dp))
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.RETRO_CLICK)
                        onClick()
                    }
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                // 3. Realistic 3D Chamfers & Extrusion Lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    if (!isPressed) {
                        // Top specular chamfer highlight
                        drawLine(
                            color = Color.White.copy(alpha = if (isActive) 0.50f else 0.32f),
                            start = Offset(2f, 1f),
                            end = Offset(w - 2f, 1f),
                            strokeWidth = 1.8.dp.toPx()
                        )
                        // Bottom 3D physical extrusion ledge
                        drawLine(
                            color = Color.Black.copy(alpha = 0.60f),
                            start = Offset(2f, h - 1.5f),
                            end = Offset(w - 2f, h - 1.5f),
                            strokeWidth = 2.5.dp.toPx()
                        )
                    } else {
                        // Pressed inset shadow
                        drawLine(
                            color = Color.Black.copy(alpha = 0.65f),
                            start = Offset(1f, 1f),
                            end = Offset(w - 1f, 1f),
                            strokeWidth = 2.5.dp.toPx()
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Realistic Domed Studio LED Lens with Metallic Bezel
                    if (showLed) {
                        Box(
                            modifier = Modifier
                                .size(7.5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A))
                                .border(0.7.dp, Color(0xFF94A3B8), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.5.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) ledColor else ledColor.copy(alpha = 0.25f))
                            ) {
                                // Specular lens reflection dot
                                Box(
                                    modifier = Modifier
                                        .size(2.dp)
                                        .offset(x = 1.dp, y = 1.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = if (isActive) 0.85f else 0.40f))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                    }

                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size((height.value * 0.42f).dp)
                        )
                        if (text.isNotEmpty() || subText != null) Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (text.isNotEmpty() || subText != null) Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = text,
                            fontSize = fontSize,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.2.sp,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (subText != null) {
                            Text(
                                text = subText,
                                fontSize = (fontSize.value * 0.78f).sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = if (isActive) contentColor.copy(alpha = 0.92f) else Color(0xFFCBD5E1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Real-time Retro 16-Band Stereo Visualizer / Spectrum Analyzer.
 */
@Composable
fun RetroSpectrumAnalyzer(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    skin: RetroSkin,
    bandsCount: Int = 16,
    speedMultiplier: Float = 1.0f,
    leftVolume: Float = 1.0f,
    rightVolume: Float = 1.0f,
    label: String = "REAL-TIME SPECTRUM ANALYZER"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrum_anim")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (1200 / speedMultiplier.coerceAtLeast(0.1f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anim_phase"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("retro_spectrum_analyzer"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = skin.lcdBackground),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, skin.chassisBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isActive) Color(0xFF00FF66) else Color(0xFF64748B))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (isActive) Color(0xFF00FF66) else skin.lcdDimColor
                    )
                }
                Text(
                    text = if (isActive) "LIVE 16-BAND DSP" else "STANDBY",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = skin.lcdDimColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF05070B))
                    .border(0.5.dp, skin.chassisBorderColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val barSpacing = 2.dp.toPx()
                    val totalSpacing = barSpacing * (bandsCount - 1)
                    val barWidth = (w - totalSpacing) / bandsCount

                    for (i in 0 until bandsCount) {
                        val x = i * (barWidth + barSpacing)
                        val channelVolume = if (i < bandsCount / 2) leftVolume else rightVolume
                        val heightFraction = if (isActive) {
                            val wave1 = sin(animProgress + i * 0.45f) * 0.4f
                            val wave2 = cos(animProgress * 1.5f + i * 0.7f) * 0.3f
                            val wave3 = sin(animProgress * 0.7f - i * 0.3f) * 0.2f
                            val harmonic = (wave1 + wave2 + wave3 + 0.55f).coerceIn(0.08f, 0.98f)
                            (harmonic * channelVolume).coerceIn(0.05f, 1f)
                        } else {
                            0.04f
                        }

                        val barH = h * heightFraction
                        val y = h - barH

                        val barColor = when {
                            heightFraction > 0.82f -> Color(0xFFFF3366)
                            heightFraction > 0.55f -> Color(0xFFFFB800)
                            else -> skin.accentColor
                        }

                        drawRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barH)
                        )

                        // Peak hold dot
                        if (isActive && heightFraction > 0.15f) {
                            val peakY = (y - 3.dp.toPx()).coerceAtLeast(0f)
                            drawRect(
                                color = Color.White,
                                topLeft = Offset(x, peakY),
                                size = Size(barWidth, 1.5.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Akai MPC-style Velocity Sound Pad.
 */
@Composable
private fun DJSoundPad(
    padNumber: String,
    label: String,
    color: Color,
    skin: RetroSkin,
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .heightIn(min = 58.dp)
            .background(Color(0xFF0D0F16), RoundedCornerShape(6.dp))
            .border(1.5.dp, skin.chassisBorderColor, RoundedCornerShape(6.dp))
            .padding(2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isPressed) color else Color(0xFF181D2A))
            .border(
                1.dp,
                if (isPressed) Color.White.copy(alpha = 0.9f) else color.copy(alpha = 0.6f),
                RoundedCornerShape(4.dp)
            )
            .clickable {
                isPressed = true
                onTrigger()
            }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(padNumber, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = if (isPressed) Color.Black else Color(0xFF94A3B8), maxLines = 1)
            Text(
                label,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = if (isPressed) Color.Black else Color.White,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(150)
            isPressed = false
        }
    }
}

/**
 * Complete DJ Studio Screen with 4-Track Mixing, Audio Waveforms,
 * Multi-Clip Sound Splicer, Portion Removal, and Background Noise Remover.
 */
@Composable
fun DJStudioScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    allSongs: List<Song>,
    skin: RetroSkin,
    equalizerController: RetroEqualizerController,
    onTurnPlayerOff: () -> Unit = {},
    onPlaySong: (Song) -> Unit = {},
    onSaveMixedTrack: (
        title: String,
        artist: String,
        genre: String,
        durationSec: Int,
        sourceUri: String?,
        albumArtUri: String?,
        formatName: String,
        targetPlaylist: String,
        onComplete: ((Song) -> Unit)?
    ) -> Unit,
    onOpenEqualizerDialog: () -> Unit,
    onNavigateToTab: ((Int) -> Unit)? = null,
    exportState: com.example.ui.viewmodel.MainViewModel.StudioExportState = com.example.ui.viewmodel.MainViewModel.StudioExportState(),
    onExportMix: (
        parts: List<com.example.audio.StudioRenderEngine.RenderPart>,
        crossfadeMs: Long,
        title: String,
        artist: String,
        format: com.example.audio.StudioRenderEngine.OutputFormat,
        targetPlaylist: String?,
        artUri: String?
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onExportRingtone: (Song, Long, Long, Long, Long, String) -> Unit = { _, _, _, _, _, _ -> },
    onDismissExport: () -> Unit = {},
    mixPlan: List<com.example.ui.viewmodel.MainViewModel.MixPlanItem> = emptyList(),
    mixPreview: com.example.ui.viewmodel.MainViewModel.MixPreviewState = com.example.ui.viewmodel.MainViewModel.MixPreviewState(),
    onMixAdd: (Song) -> Unit = {},
    onMixRemove: (String) -> Unit = {},
    onMixMove: (String, Int) -> Unit = { _, _ -> },
    onMixUpdate: (com.example.ui.viewmodel.MainViewModel.MixPlanItem) -> Unit = {},
    onMixClear: () -> Unit = {},
    onMixPreview: (String) -> Unit = {},
    onMixPreviewDone: () -> Unit = {},
    onMixExport: (String, String, com.example.audio.StudioRenderEngine.OutputFormat) -> Unit = { _, _, _ -> },
    studioVm: com.example.ui.viewmodel.MainViewModel? = null,
    studioPlaylists: List<com.example.data.model.Playlist> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var studioMode by remember { mutableIntStateOf(0) } // 0: 4-Deck DJ Live Mixer, 1: Multi-Clip Splicer

    // ==========================================
    // 4-DECK DJ MIXER STATE
    // ==========================================
    var djDeckLayoutMode by remember { mutableIntStateOf(0) } // 0: 2-Deck Focus (A/B or C/D), 1: 4-Deck Quad Mixer
    var activeFocusPair by remember { mutableIntStateOf(0) } // 0: Decks A & B, 1: Decks C & D

    // Deck A (Left bus, Cyan accent)
    var deckASong by remember { mutableStateOf(currentSong ?: allSongs.firstOrNull()) }
    var deckAPlaying by remember { mutableStateOf(false) }
    var deckASpeed by remember { mutableFloatStateOf(1.0f) }
    var deckAVolume by remember { mutableFloatStateOf(1.0f) }
    var deckAProgress by remember { mutableFloatStateOf(0.0f) }
    var deckACue by remember { mutableFloatStateOf(0.0f) }
    var deckABus by remember { mutableStateOf(CrossfaderBus.LEFT) }
    var deckALowKill by remember { mutableStateOf(false) }
    var deckAMidKill by remember { mutableStateOf(false) }
    var deckAHighKill by remember { mutableStateOf(false) }
    var deckALoop by remember { mutableStateOf(false) }

    // Deck B (Right bus, Secondary accent / Magenta)
    var deckBSong by remember { mutableStateOf(allSongs.getOrNull(1) ?: allSongs.firstOrNull()) }
    var deckBPlaying by remember { mutableStateOf(false) }
    var deckBSpeed by remember { mutableFloatStateOf(1.0f) }
    var deckBVolume by remember { mutableFloatStateOf(1.0f) }
    var deckBProgress by remember { mutableFloatStateOf(0.0f) }
    var deckBCue by remember { mutableFloatStateOf(0.0f) }
    var deckBBus by remember { mutableStateOf(CrossfaderBus.RIGHT) }
    var deckBLowKill by remember { mutableStateOf(false) }
    var deckBMidKill by remember { mutableStateOf(false) }
    var deckBHighKill by remember { mutableStateOf(false) }
    var deckBLoop by remember { mutableStateOf(false) }

    // Deck C (Aux Left bus, Emerald Green accent)
    var deckCSong by remember { mutableStateOf(allSongs.getOrNull(2) ?: allSongs.firstOrNull()) }
    var deckCPlaying by remember { mutableStateOf(false) }
    var deckCSpeed by remember { mutableFloatStateOf(1.0f) }
    var deckCVolume by remember { mutableFloatStateOf(0.85f) }
    var deckCProgress by remember { mutableFloatStateOf(0.0f) }
    var deckCCue by remember { mutableFloatStateOf(0.0f) }
    var deckCBus by remember { mutableStateOf(CrossfaderBus.LEFT) }
    var deckCLowKill by remember { mutableStateOf(false) }
    var deckCMidKill by remember { mutableStateOf(false) }
    var deckCHighKill by remember { mutableStateOf(false) }
    var deckCLoop by remember { mutableStateOf(false) }

    // Deck D (Aux Right bus, Amber Gold accent)
    var deckDSong by remember { mutableStateOf(allSongs.getOrNull(3) ?: allSongs.firstOrNull()) }
    var deckDPlaying by remember { mutableStateOf(false) }
    var deckDSpeed by remember { mutableFloatStateOf(1.0f) }
    var deckDVolume by remember { mutableFloatStateOf(0.85f) }
    var deckDProgress by remember { mutableFloatStateOf(0.0f) }
    var deckDCue by remember { mutableFloatStateOf(0.0f) }
    var deckDBus by remember { mutableStateOf(CrossfaderBus.RIGHT) }
    var deckDLowKill by remember { mutableStateOf(false) }
    var deckDMidKill by remember { mutableStateOf(false) }
    var deckDHighKill by remember { mutableStateOf(false) }
    var deckDLoop by remember { mutableStateOf(false) }

    var masterCrossfader by remember { mutableFloatStateOf(0.5f) } // 0.0 (Left 100%) .. 1.0 (Right 100%)
    var showTrackPickerForDeck by remember { mutableStateOf<String?>(null) } // "A", "B", "C", "D"

    // DJ Live Recording State
    var isRecordingDJSession by remember { mutableStateOf(false) }
    var recordedSessionSeconds by remember { mutableIntStateOf(0) }
    var hasRecordedDJSession by remember { mutableStateOf(false) }
    var isPreviewingRecordedSession by remember { mutableStateOf(false) }
    var djRecordTitle by remember { mutableStateOf("Live DJ Quad Set '99") }
    var djRecordArtist by remember { mutableStateOf("DJ Studio Live") }
    var djRecordFormat by remember { mutableStateOf("Studio WAV (1411k / 16-bit)") }
    var djRecordTargetPlaylist by remember { mutableStateOf("DJ Studio Mixtapes '99") }
    var recentlyExtractedDjSong by remember { mutableStateOf<Song?>(null) }
    var showDjExtractSuccessBanner by remember { mutableStateOf(false) }

    // MediaPlayers for 4 DJ Decks
    var deckAPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var deckBPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var deckCPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var deckDPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // ==========================================
    // SOUND SPLICER STATE (Multi-Clip, Portion Removal, Noise Reduction)
    // ==========================================
    var splicerClips by remember {
        val initialList = mutableListOf<SplicerClip>()
        val track1 = deckASong ?: allSongs.firstOrNull()
        val track2 = deckBSong ?: allSongs.getOrNull(1) ?: allSongs.firstOrNull()
        if (track1 != null) {
            initialList.add(
                SplicerClip(
                    title = track1.title,
                    artist = track1.artist,
                    durationSec = (track1.durationMs / 1000).toInt().coerceAtLeast(30),
                    uri = track1.uri,
                    sourceType = ClipSourceType.LIBRARY_TRACK,
                    colorHex = 0xFF00E5FF
                )
            )
        }
        if (track2 != null) {
            initialList.add(
                SplicerClip(
                    title = track2.title,
                    artist = track2.artist,
                    durationSec = (track2.durationMs / 1000).toInt().coerceAtLeast(30),
                    uri = track2.uri,
                    sourceType = ClipSourceType.LIBRARY_TRACK,
                    colorHex = 0xFFFF4081
                )
            )
        }
        // Add a 3rd starter studio sound FX clip to demonstrate >2 clips loaded and spliced together
        initialList.add(
            SplicerClip(
                title = "808 Boom & Vinyl Scratch",
                artist = "Studio FX",
                durationSec = 6,
                sourceType = ClipSourceType.SOUND_FX,
                colorHex = 0xFFFFB800
            )
        )
        mutableStateOf<List<SplicerClip>>(initialList)
    }

    var selectedClipIndex by remember { mutableIntStateOf(0) }
    var showAddSoundDialog by remember { mutableStateOf(false) }
    var isSplicerPreviewPlaying by remember { mutableStateOf(false) }
    var splicerPlayheadProgress by remember { mutableFloatStateOf(0.0f) }
    var spliceTransitionMode by remember { mutableStateOf("CROSSFADE (1.5s)") }

    // Splicer Export State
    var mixExportTitle by remember { mutableStateOf("Multi-Clip Spliced Master") }
    var mixExportArtist by remember { mutableStateOf("DJ Sound Splicer") }
    var exportFormat by remember { mutableStateOf("Studio WAV (44.1kHz / 16-bit)") }
    var exportTargetPlaylist by remember { mutableStateOf("DJ Studio Mixtapes '99") }
    var recentlyExportedSong by remember { mutableStateOf<Song?>(null) }
    var showExportSuccessBanner by remember { mutableStateOf(false) }

    var splicerPreviewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun notifyEditingOrStudioPlayback() {
        if (isPlaying) {
            onTurnPlayerOff()
        }
    }

    // Safely cleanup all MediaPlayers
    DisposableEffect(Unit) {
        onDispose {
            try { deckAPlayer?.release() } catch (ignored: Exception) {}
            try { deckBPlayer?.release() } catch (ignored: Exception) {}
            try { deckCPlayer?.release() } catch (ignored: Exception) {}
            try { deckDPlayer?.release() } catch (ignored: Exception) {}
            try { splicerPreviewPlayer?.release() } catch (ignored: Exception) {}
            deckAPlayer = null
            deckBPlayer = null
            deckCPlayer = null
            deckDPlayer = null
            splicerPreviewPlayer = null
        }
    }

    // Helper: Calculate effective volume considering crossfader bus assignment and kill EQ
    fun calcDeckVolume(deckVol: Float, bus: CrossfaderBus, isLow: Boolean, isMid: Boolean, isHigh: Boolean): Float {
        if (isLow && isMid && isHigh) return 0f
        val busMultiplier = when (bus) {
            CrossfaderBus.LEFT -> (1.0f - masterCrossfader).coerceIn(0f, 1f)
            CrossfaderBus.RIGHT -> masterCrossfader.coerceIn(0f, 1f)
            CrossfaderBus.THRU -> 1.0f
        }
        val killReduction = (if (isLow) 0.35f else 0f) + (if (isMid) 0.35f else 0f) + (if (isHigh) 0.30f else 0f)
        return (deckVol * busMultiplier * (1f - killReduction)).coerceIn(0f, 1f)
    }

    // Toggle Deck Playback
    fun toggleDeck(deckLetter: String) {
        notifyEditingOrStudioPlayback()
        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.NEEDLE_DROP)
        when (deckLetter) {
            "A" -> {
                val next = !deckAPlaying
                deckAPlaying = next
                if (next) {
                    deckASong?.let { song ->
                        try {
                            deckAPlayer?.release()
                            deckAPlayer = MediaPlayer().apply {
                                if (song.uri.startsWith("http://") || song.uri.startsWith("https://")) {
                                    setDataSource(song.uri)
                                } else {
                                    setDataSource(context, Uri.parse(song.uri))
                                }
                                isLooping = true
                                val vol = calcDeckVolume(deckAVolume, deckABus, deckALowKill, deckAMidKill, deckAHighKill)
                                setVolume(vol, vol)
                                setOnPreparedListener { it.start() }
                                prepareAsync()
                            }
                        } catch (ignored: Exception) {}
                    }
                } else {
                    try { deckAPlayer?.pause() } catch (ignored: Exception) {}
                }
            }
            "B" -> {
                val next = !deckBPlaying
                deckBPlaying = next
                if (next) {
                    deckBSong?.let { song ->
                        try {
                            deckBPlayer?.release()
                            deckBPlayer = MediaPlayer().apply {
                                if (song.uri.startsWith("http://") || song.uri.startsWith("https://")) {
                                    setDataSource(song.uri)
                                } else {
                                    setDataSource(context, Uri.parse(song.uri))
                                }
                                isLooping = true
                                val vol = calcDeckVolume(deckBVolume, deckBBus, deckBLowKill, deckBMidKill, deckBHighKill)
                                setVolume(vol, vol)
                                setOnPreparedListener { it.start() }
                                prepareAsync()
                            }
                        } catch (ignored: Exception) {}
                    }
                } else {
                    try { deckBPlayer?.pause() } catch (ignored: Exception) {}
                }
            }
            "C" -> {
                val next = !deckCPlaying
                deckCPlaying = next
                if (next) {
                    deckCSong?.let { song ->
                        try {
                            deckCPlayer?.release()
                            deckCPlayer = MediaPlayer().apply {
                                if (song.uri.startsWith("http://") || song.uri.startsWith("https://")) {
                                    setDataSource(song.uri)
                                } else {
                                    setDataSource(context, Uri.parse(song.uri))
                                }
                                isLooping = true
                                val vol = calcDeckVolume(deckCVolume, deckCBus, deckCLowKill, deckCMidKill, deckCHighKill)
                                setVolume(vol, vol)
                                setOnPreparedListener { it.start() }
                                prepareAsync()
                            }
                        } catch (ignored: Exception) {}
                    }
                } else {
                    try { deckCPlayer?.pause() } catch (ignored: Exception) {}
                }
            }
            "D" -> {
                val next = !deckDPlaying
                deckDPlaying = next
                if (next) {
                    deckDSong?.let { song ->
                        try {
                            deckDPlayer?.release()
                            deckDPlayer = MediaPlayer().apply {
                                if (song.uri.startsWith("http://") || song.uri.startsWith("https://")) {
                                    setDataSource(song.uri)
                                } else {
                                    setDataSource(context, Uri.parse(song.uri))
                                }
                                isLooping = true
                                val vol = calcDeckVolume(deckDVolume, deckDBus, deckDLowKill, deckDMidKill, deckDHighKill)
                                setVolume(vol, vol)
                                setOnPreparedListener { it.start() }
                                prepareAsync()
                            }
                        } catch (ignored: Exception) {}
                    }
                } else {
                    try { deckDPlayer?.pause() } catch (ignored: Exception) {}
                }
            }
        }
    }

    // Dynamic Volume sync across 4 decks when crossfader, volume, or bus changes
    LaunchedEffect(masterCrossfader, deckAVolume, deckBVolume, deckCVolume, deckDVolume, deckABus, deckBBus, deckCBus, deckDBus, deckALowKill, deckBLowKill, deckCLowKill, deckDLowKill) {
        val volA = calcDeckVolume(deckAVolume, deckABus, deckALowKill, deckAMidKill, deckAHighKill)
        val volB = calcDeckVolume(deckBVolume, deckBBus, deckBLowKill, deckBMidKill, deckBHighKill)
        val volC = calcDeckVolume(deckCVolume, deckCBus, deckCLowKill, deckCMidKill, deckCHighKill)
        val volD = calcDeckVolume(deckDVolume, deckDBus, deckDLowKill, deckDMidKill, deckDHighKill)
        try { deckAPlayer?.setVolume(volA, volA) } catch (ignored: Exception) {}
        try { deckBPlayer?.setVolume(volB, volB) } catch (ignored: Exception) {}
        try { deckCPlayer?.setVolume(volC, volC) } catch (ignored: Exception) {}
        try { deckDPlayer?.setVolume(volD, volD) } catch (ignored: Exception) {}
    }

    // Continuous simulated progress and waveform scrolling for playing decks
    LaunchedEffect(deckAPlaying, deckBPlaying, deckCPlaying, deckDPlaying) {
        while (deckAPlaying || deckBPlaying || deckCPlaying || deckDPlaying) {
            delay(150)
            if (deckAPlaying) {
                deckAProgress = (deckAProgress + 0.005f * deckASpeed) % 1.0f
            }
            if (deckBPlaying) {
                deckBProgress = (deckBProgress + 0.005f * deckBSpeed) % 1.0f
            }
            if (deckCPlaying) {
                deckCProgress = (deckCProgress + 0.005f * deckCSpeed) % 1.0f
            }
            if (deckDPlaying) {
                deckDProgress = (deckDProgress + 0.005f * deckDSpeed) % 1.0f
            }
        }
    }

    // Recording session timer
    LaunchedEffect(isRecordingDJSession) {
        if (isRecordingDJSession) {
            while (isRecordingDJSession) {
                delay(1000)
                recordedSessionSeconds++
            }
        }
    }

    // Splicer Preview Playback Loop
    LaunchedEffect(isSplicerPreviewPlaying) {
        if (isSplicerPreviewPlaying) {
            while (isSplicerPreviewPlaying) {
                delay(100)
                splicerPlayheadProgress += 0.015f
                if (splicerPlayheadProgress > 1f) {
                    splicerPlayheadProgress = 0f
                }
            }
        } else {
            splicerPlayheadProgress = 0f
        }
    }

    fun toggleSplicerPreview() {
        notifyEditingOrStudioPlayback()
        val next = !isSplicerPreviewPlaying
        isSplicerPreviewPlaying = next
        if (next) {
            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
            val clipToPreview = splicerClips.getOrNull(selectedClipIndex) ?: splicerClips.firstOrNull()
            clipToPreview?.uri?.let { uriStr ->
                try {
                    splicerPreviewPlayer?.release()
                    splicerPreviewPlayer = MediaPlayer().apply {
                        if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                            setDataSource(uriStr)
                        } else {
                            setDataSource(context, Uri.parse(uriStr))
                        }
                        isLooping = true
                        setOnPreparedListener { it.start() }
                        prepareAsync()
                    }
                } catch (ignored: Exception) {}
            }
        } else {
            try { splicerPreviewPlayer?.pause() } catch (ignored: Exception) {}
        }
    }

    // ==========================================
    // MAIN LAYOUT
    // ==========================================
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(skin.chassisBackground)
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP CONSOLE MODE SWITCHER: Mixtape Builder • Clean-Up • FX & Mashup • Ringtone Maker
        val studioModes = listOf(
            Triple("MIXTAPE BUILDER", "AUTO MIX • TRANSITIONS", Icons.Default.QueueMusic),
            Triple("CLEAN-UP STUDIO", "NOISE • CUT", Icons.Default.CleaningServices),
            Triple("FX & MASHUP", "RETRO FX • LAYERS", Icons.Default.AutoFixHigh),
            Triple("RINGTONE MAKER", "CUT • FADE • SET", Icons.Default.RingVolume)
        )
        val modeThemes = listOf(StudioButtonTheme.CYAN_SYNC, StudioButtonTheme.AMBER_CUE, StudioButtonTheme.CHAMPAGNE_GOLD, StudioButtonTheme.CYAN_SYNC)
        for (rowIndex in 0 until 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (col in 0 until 2) {
                    val modeIndex = rowIndex * 2 + col
                    val (modeTitle, modeSub, modeIcon) = studioModes[modeIndex]
                    StudioHardwareButton(
                        text = modeTitle,
                        subText = modeSub,
                        onClick = {
                            studioVm?.stopStudioAudio()
                            studioMode = modeIndex
                        },
                        skin = skin,
                        theme = if (studioMode == modeIndex) modeThemes[modeIndex] else StudioButtonTheme.SLATE_UTILITY,
                        isActive = studioMode == modeIndex,
                        icon = modeIcon,
                        height = 50.dp,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val studioInitialSong = currentSong?.takeIf { it.source == com.example.data.model.AudioSource.LOCAL } ?: allSongs.firstOrNull()
        if (studioVm != null) {
            // Each section owns its monitor; switching sections or leaving the studio stops studio audio.
            LaunchedEffect(studioMode) {
                studioVm.setStudioSection(
                    when (studioMode) {
                        0 -> com.example.ui.viewmodel.MainViewModel.SECTION_MIX
                        1 -> com.example.ui.viewmodel.MainViewModel.SECTION_CLEAN
                        2 -> com.example.ui.viewmodel.MainViewModel.SECTION_FX
                        else -> com.example.ui.viewmodel.MainViewModel.SECTION_RINGTONE
                    }
                )
            }
            DisposableEffect(studioVm) { onDispose { studioVm.stopStudioAudio() } }
            // Finished exports play in the studio monitor, not the main player
            val playResult: (Song) -> Unit = { song -> studioVm.playStudioResult(song) }
            when (studioMode) {
                0 -> MixtapeBuilderPanel(
                    viewModel = studioVm,
                    librarySongs = allSongs,
                    playlists = studioPlaylists,
                    skin = skin,
                    exportState = exportState,
                    onPlayExport = playResult
                )
                1 -> CleanUpStudioPanel(studioVm, allSongs, studioInitialSong, skin, exportState, playResult)
                2 -> FxMashupPanel(studioVm, allSongs, studioInitialSong, skin, exportState, playResult)
                else -> RingtoneMakerPanel(
                    songs = allSongs,
                    initialSong = studioInitialSong,
                    skin = skin,
                    exportState = exportState,
                    onBeforePreview = { notifyEditingOrStudioPlayback() },
                    onExport = onExportRingtone,
                    onPlayExport = playResult,
                    onDismissExport = onDismissExport,
                    viewModel = studioVm
                )
            }
        }
    }

    // Song Selection Dialog for Decks
    showTrackPickerForDeck?.let { deckTarget ->
        AlertDialog(
            onDismissRequest = { showTrackPickerForDeck = null },
            title = {
                Text(
                    text = "SELECT TRACK FOR DECK $deckTarget",
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = skin.titleBarTextColor
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allSongs) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(skin.lcdBackground)
                                .border(1.dp, skin.chassisBorderColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .clickable {
                                    notifyEditingOrStudioPlayback()
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                    when (deckTarget) {
                                        "A" -> deckASong = song
                                        "B" -> deckBSong = song
                                        "C" -> deckCSong = song
                                        "D" -> deckDSong = song
                                    }
                                    showTrackPickerForDeck = null
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = skin.accentColor, modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                Text(song.artist, fontSize = 10.5.sp, color = skin.lcdTextColor.copy(alpha = 0.6f), maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrackPickerForDeck = null }) {
                    Text("CLOSE", color = skin.accentColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = skin.lcdBackground
        )
    }

    // Add Sound / Audio Clip Dialog (Library Track, Record Mic, Studio SFX)
    if (showAddSoundDialog) {
        AddSoundDialog(
            allSongs = allSongs,
            onAddTrack = { song ->
                val newClip = SplicerClip(
                    title = song.title,
                    artist = song.artist,
                    durationSec = (song.durationMs / 1000).toInt().coerceAtLeast(30),
                    uri = song.uri,
                    sourceType = ClipSourceType.LIBRARY_TRACK,
                    colorHex = when (splicerClips.size % 4) {
                        0 -> 0xFF00E5FF
                        1 -> 0xFFFF4081
                        2 -> 0xFF00FF66
                        else -> 0xFFFFD54F
                    }
                )
                splicerClips = splicerClips + newClip
                selectedClipIndex = splicerClips.size - 1
            },
            onAddRecordedVocal = { title, durationSec ->
                val newClip = SplicerClip(
                    title = title,
                    artist = "Recorded Vocal",
                    durationSec = durationSec,
                    sourceType = ClipSourceType.MIC_RECORDING,
                    colorHex = 0xFFEF4444,
                    isDenoiseEnabled = true // Auto-enable denoiser for recorded mic vocal clips!
                )
                splicerClips = splicerClips + newClip
                selectedClipIndex = splicerClips.size - 1
            },
            onAddSoundFx = { sfxName, durationSec, colorHex ->
                val newClip = SplicerClip(
                    title = sfxName,
                    artist = "Studio FX",
                    durationSec = durationSec,
                    sourceType = ClipSourceType.SOUND_FX,
                    colorHex = colorHex
                )
                splicerClips = splicerClips + newClip
                selectedClipIndex = splicerClips.size - 1
            },
            onDismiss = { showAddSoundDialog = false }
        )
    }
}
