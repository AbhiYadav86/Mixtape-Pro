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
                            modifier = Modifier.size((height.value * 0.38f).dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Column(
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
            .height(46.dp)
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(padNumber, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF94A3B8))
            Text(
                label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (isPressed) Color.Black else Color.White,
                maxLines = 1
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
    onExportVocals: (Song, Float, Float, com.example.audio.StudioRenderEngine.OutputFormat) -> Unit = { _, _, _, _ -> },
    onDismissExport: () -> Unit = {},
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
        // TOP CONSOLE MODE SWITCHER: DJ Mixer • Sound Splicer • Ringtone Maker • Vocal Remover
        val studioModes = listOf(
            Triple("4-TRACK DJ MIXER", "4 DECKS & WAVEFORMS", Icons.Default.Tune),
            Triple("SOUND SPLICER", "MULTI-CLIP & DENOISE", Icons.Default.ContentCut),
            Triple("RINGTONE MAKER", "CUT • FADE • SET", Icons.Default.RingVolume),
            Triple("VOCAL REMOVER", "KARAOKE / INSTRUMENTAL", Icons.Default.MicOff)
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
                            if (modeIndex != 0) notifyEditingOrStudioPlayback()
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

        if (studioMode == 0) {
            // =========================================================================
            // 4-TRACK DJ MIXER CONSOLE
            // =========================================================================

            // Deck Layout Mode Switcher: 2-DECK FOCUS (A/B vs C/D) vs 4-DECK QUAD CLUB
            // Two full-width segmented rows so labels never get squeezed (works with large system fonts)
            @Composable
            fun SegmentRow(options: List<String>, selected: Int, selectedColor: Color, onSelect: (Int) -> Unit) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0D111A))
                        .border(1.dp, skin.chassisBorderColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    options.forEachIndexed { index, label ->
                        val isSel = selected == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSel) selectedColor else Color.Transparent)
                                .clickable { onSelect(index) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = if (isSel) Color.Black else Color(0xFFCBD5E1),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            SegmentRow(listOf("2-DECK FOCUS", "4-DECK QUAD"), djDeckLayoutMode, skin.accentColor) { index ->
                djDeckLayoutMode = index
                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
            }
            if (djDeckLayoutMode == 0) {
                Spacer(modifier = Modifier.height(4.dp))
                SegmentRow(listOf("DECKS A & B", "DECKS C & D"), activeFocusPair, Color(0xFFFFB800)) { pairIndex ->
                    activeFocusPair = pairIndex
                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // RENDER DECKS
            if (djDeckLayoutMode == 0) {
                // 2-DECK FOCUS (Expanded view with large waveforms)
                if (activeFocusPair == 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // DECK A
                        DJTurntableDeck4Channel(
                            deckId = "A",
                            deckName = "DECK A (CH-1)",
                            song = deckASong,
                            isPlaying = deckAPlaying,
                            speed = deckASpeed,
                            volume = deckAVolume,
                            progress = deckAProgress,
                            cuePoint = deckACue,
                            crossfaderBus = deckABus,
                            isLowKill = deckALowKill,
                            isMidKill = deckAMidKill,
                            isHighKill = deckAHighKill,
                            isLoopActive = deckALoop,
                            accentColor = skin.accentColor,
                            skin = skin,
                            onTogglePlay = { toggleDeck("A") },
                            onSelectTrack = {
                                notifyEditingOrStudioPlayback()
                                showTrackPickerForDeck = "A"
                            },
                            onSpeedChange = { deckASpeed = it },
                            onVolumeChange = { deckAVolume = it },
                            onSeek = { deckAProgress = it },
                            onCue = {
                                notifyEditingOrStudioPlayback()
                                if (!deckAPlaying) toggleDeck("A") else deckACue = deckAProgress
                            },
                            onSync = {
                                deckASpeed = deckBSpeed
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                            },
                            onToggleLowKill = { deckALowKill = !deckALowKill },
                            onToggleMidKill = { deckAMidKill = !deckAMidKill },
                            onToggleHighKill = { deckAHighKill = !deckAHighKill },
                            onToggleLoop = { deckALoop = !deckALoop },
                            onBusChange = { deckABus = it },
                            modifier = Modifier.weight(1f)
                        )

                        // DECK B
                        DJTurntableDeck4Channel(
                            deckId = "B",
                            deckName = "DECK B (CH-2)",
                            song = deckBSong,
                            isPlaying = deckBPlaying,
                            speed = deckBSpeed,
                            volume = deckBVolume,
                            progress = deckBProgress,
                            cuePoint = deckBCue,
                            crossfaderBus = deckBBus,
                            isLowKill = deckBLowKill,
                            isMidKill = deckBMidKill,
                            isHighKill = deckBHighKill,
                            isLoopActive = deckBLoop,
                            accentColor = skin.secondaryAccentColor,
                            skin = skin,
                            onTogglePlay = { toggleDeck("B") },
                            onSelectTrack = {
                                notifyEditingOrStudioPlayback()
                                showTrackPickerForDeck = "B"
                            },
                            onSpeedChange = { deckBSpeed = it },
                            onVolumeChange = { deckBVolume = it },
                            onSeek = { deckBProgress = it },
                            onCue = {
                                notifyEditingOrStudioPlayback()
                                if (!deckBPlaying) toggleDeck("B") else deckBCue = deckBProgress
                            },
                            onSync = {
                                deckBSpeed = deckASpeed
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                            },
                            onToggleLowKill = { deckBLowKill = !deckBLowKill },
                            onToggleMidKill = { deckBMidKill = !deckBMidKill },
                            onToggleHighKill = { deckBHighKill = !deckBHighKill },
                            onToggleLoop = { deckBLoop = !deckBLoop },
                            onBusChange = { deckBBus = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // DECK C
                        DJTurntableDeck4Channel(
                            deckId = "C",
                            deckName = "DECK C (CH-3)",
                            song = deckCSong,
                            isPlaying = deckCPlaying,
                            speed = deckCSpeed,
                            volume = deckCVolume,
                            progress = deckCProgress,
                            cuePoint = deckCCue,
                            crossfaderBus = deckCBus,
                            isLowKill = deckCLowKill,
                            isMidKill = deckCMidKill,
                            isHighKill = deckCHighKill,
                            isLoopActive = deckCLoop,
                            accentColor = Color(0xFF00FF66),
                            skin = skin,
                            onTogglePlay = { toggleDeck("C") },
                            onSelectTrack = {
                                notifyEditingOrStudioPlayback()
                                showTrackPickerForDeck = "C"
                            },
                            onSpeedChange = { deckCSpeed = it },
                            onVolumeChange = { deckCVolume = it },
                            onSeek = { deckCProgress = it },
                            onCue = {
                                notifyEditingOrStudioPlayback()
                                if (!deckCPlaying) toggleDeck("C") else deckCCue = deckCProgress
                            },
                            onSync = {
                                deckCSpeed = deckASpeed
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                            },
                            onToggleLowKill = { deckCLowKill = !deckCLowKill },
                            onToggleMidKill = { deckCMidKill = !deckCMidKill },
                            onToggleHighKill = { deckCHighKill = !deckCHighKill },
                            onToggleLoop = { deckCLoop = !deckCLoop },
                            onBusChange = { deckCBus = it },
                            modifier = Modifier.weight(1f)
                        )

                        // DECK D
                        DJTurntableDeck4Channel(
                            deckId = "D",
                            deckName = "DECK D (CH-4)",
                            song = deckDSong,
                            isPlaying = deckDPlaying,
                            speed = deckDSpeed,
                            volume = deckDVolume,
                            progress = deckDProgress,
                            cuePoint = deckDCue,
                            crossfaderBus = deckDBus,
                            isLowKill = deckDLowKill,
                            isMidKill = deckDMidKill,
                            isHighKill = deckDHighKill,
                            isLoopActive = deckDLoop,
                            accentColor = Color(0xFFFFB800),
                            skin = skin,
                            onTogglePlay = { toggleDeck("D") },
                            onSelectTrack = {
                                notifyEditingOrStudioPlayback()
                                showTrackPickerForDeck = "D"
                            },
                            onSpeedChange = { deckDSpeed = it },
                            onVolumeChange = { deckDVolume = it },
                            onSeek = { deckDProgress = it },
                            onCue = {
                                notifyEditingOrStudioPlayback()
                                if (!deckDPlaying) toggleDeck("D") else deckDCue = deckDProgress
                            },
                            onSync = {
                                deckDSpeed = deckASpeed
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                            },
                            onToggleLowKill = { deckDLowKill = !deckDLowKill },
                            onToggleMidKill = { deckDMidKill = !deckDMidKill },
                            onToggleHighKill = { deckDHighKill = !deckDHighKill },
                            onToggleLoop = { deckDLoop = !deckDLoop },
                            onBusChange = { deckDBus = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // 4-DECK QUAD CLUB LAYOUT (All 4 Decks visible side-by-side in 2x2 grid)
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Top Pair: Deck A & Deck B
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DJTurntableDeck4Channel(
                            deckId = "A",
                            deckName = "DECK A (CH-1)",
                            song = deckASong,
                            isPlaying = deckAPlaying,
                            speed = deckASpeed,
                            volume = deckAVolume,
                            progress = deckAProgress,
                            cuePoint = deckACue,
                            crossfaderBus = deckABus,
                            isLowKill = deckALowKill,
                            isMidKill = deckAMidKill,
                            isHighKill = deckAHighKill,
                            isLoopActive = deckALoop,
                            accentColor = skin.accentColor,
                            skin = skin,
                            compactMode = true,
                            onTogglePlay = { toggleDeck("A") },
                            onSelectTrack = { showTrackPickerForDeck = "A" },
                            onSpeedChange = { deckASpeed = it },
                            onVolumeChange = { deckAVolume = it },
                            onSeek = { deckAProgress = it },
                            onCue = { if (!deckAPlaying) toggleDeck("A") else deckACue = deckAProgress },
                            onSync = { deckASpeed = deckBSpeed },
                            onToggleLowKill = { deckALowKill = !deckALowKill },
                            onToggleMidKill = { deckAMidKill = !deckAMidKill },
                            onToggleHighKill = { deckAHighKill = !deckAHighKill },
                            onToggleLoop = { deckALoop = !deckALoop },
                            onBusChange = { deckABus = it },
                            modifier = Modifier.weight(1f)
                        )
                        DJTurntableDeck4Channel(
                            deckId = "B",
                            deckName = "DECK B (CH-2)",
                            song = deckBSong,
                            isPlaying = deckBPlaying,
                            speed = deckBSpeed,
                            volume = deckBVolume,
                            progress = deckBProgress,
                            cuePoint = deckBCue,
                            crossfaderBus = deckBBus,
                            isLowKill = deckBLowKill,
                            isMidKill = deckBMidKill,
                            isHighKill = deckBHighKill,
                            isLoopActive = deckBLoop,
                            accentColor = skin.secondaryAccentColor,
                            skin = skin,
                            compactMode = true,
                            onTogglePlay = { toggleDeck("B") },
                            onSelectTrack = { showTrackPickerForDeck = "B" },
                            onSpeedChange = { deckBSpeed = it },
                            onVolumeChange = { deckBVolume = it },
                            onSeek = { deckBProgress = it },
                            onCue = { if (!deckBPlaying) toggleDeck("B") else deckBCue = deckBProgress },
                            onSync = { deckBSpeed = deckASpeed },
                            onToggleLowKill = { deckBLowKill = !deckBLowKill },
                            onToggleMidKill = { deckBMidKill = !deckBMidKill },
                            onToggleHighKill = { deckBHighKill = !deckBHighKill },
                            onToggleLoop = { deckBLoop = !deckBLoop },
                            onBusChange = { deckBBus = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Bottom Pair: Deck C & Deck D
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DJTurntableDeck4Channel(
                            deckId = "C",
                            deckName = "DECK C (CH-3)",
                            song = deckCSong,
                            isPlaying = deckCPlaying,
                            speed = deckCSpeed,
                            volume = deckCVolume,
                            progress = deckCProgress,
                            cuePoint = deckCCue,
                            crossfaderBus = deckCBus,
                            isLowKill = deckCLowKill,
                            isMidKill = deckCMidKill,
                            isHighKill = deckCHighKill,
                            isLoopActive = deckCLoop,
                            accentColor = Color(0xFF00FF66),
                            skin = skin,
                            compactMode = true,
                            onTogglePlay = { toggleDeck("C") },
                            onSelectTrack = { showTrackPickerForDeck = "C" },
                            onSpeedChange = { deckCSpeed = it },
                            onVolumeChange = { deckCVolume = it },
                            onSeek = { deckCProgress = it },
                            onCue = { if (!deckCPlaying) toggleDeck("C") else deckCCue = deckCProgress },
                            onSync = { deckCSpeed = deckASpeed },
                            onToggleLowKill = { deckCLowKill = !deckCLowKill },
                            onToggleMidKill = { deckCMidKill = !deckCMidKill },
                            onToggleHighKill = { deckCHighKill = !deckCHighKill },
                            onToggleLoop = { deckCLoop = !deckCLoop },
                            onBusChange = { deckCBus = it },
                            modifier = Modifier.weight(1f)
                        )
                        DJTurntableDeck4Channel(
                            deckId = "D",
                            deckName = "DECK D (CH-4)",
                            song = deckDSong,
                            isPlaying = deckDPlaying,
                            speed = deckDSpeed,
                            volume = deckDVolume,
                            progress = deckDProgress,
                            cuePoint = deckDCue,
                            crossfaderBus = deckDBus,
                            isLowKill = deckDLowKill,
                            isMidKill = deckDMidKill,
                            isHighKill = deckDHighKill,
                            isLoopActive = deckDLoop,
                            accentColor = Color(0xFFFFB800),
                            skin = skin,
                            compactMode = true,
                            onTogglePlay = { toggleDeck("D") },
                            onSelectTrack = { showTrackPickerForDeck = "D" },
                            onSpeedChange = { deckDSpeed = it },
                            onVolumeChange = { deckDVolume = it },
                            onSeek = { deckDProgress = it },
                            onCue = { if (!deckDPlaying) toggleDeck("D") else deckDCue = deckDProgress },
                            onSync = { deckDSpeed = deckASpeed },
                            onToggleLowKill = { deckDLowKill = !deckDLowKill },
                            onToggleMidKill = { deckDMidKill = !deckDMidKill },
                            onToggleHighKill = { deckDHighKill = !deckDHighKill },
                            onToggleLoop = { deckDLoop = !deckDLoop },
                            onBusChange = { deckDBus = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 16-Band Visualizer for the DJ Console
            val isAnyDeckPlaying = deckAPlaying || deckBPlaying || deckCPlaying || deckDPlaying
            RetroSpectrumAnalyzer(
                isActive = isAnyDeckPlaying,
                skin = skin,
                speedMultiplier = if (deckAPlaying) deckASpeed else if (deckBPlaying) deckBSpeed else 1.0f,
                leftVolume = ((1f - masterCrossfader) * max(deckAVolume, deckCVolume)).coerceIn(0f, 1f),
                rightVolume = (masterCrossfader * max(deckBVolume, deckDVolume)).coerceIn(0f, 1f),
                label = "4-CHANNEL MASTER STEREO SPECTRUM"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Master DJ Crossfader Rack
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dj_crossfader_panel"),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = skin.lcdBackground.copy(alpha = 0.85f)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, skin.chassisBorderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("◀ BUS L [${((1f - masterCrossfader) * 100).toInt()}%]", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = skin.accentColor, fontFamily = FontFamily.Monospace)
                        Text("ASSIGNABLE CROSSFADER", fontSize = 11.sp, fontWeight = FontWeight.Black, color = skin.titleBarTextColor, fontFamily = FontFamily.Monospace)
                        Text("BUS R [${(masterCrossfader * 100).toInt()}%] ▶", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = skin.secondaryAccentColor, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = masterCrossfader,
                        onValueChange = {
                            notifyEditingOrStudioPlayback()
                            masterCrossfader = it
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dj_crossfader_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = skin.sliderThumbColor,
                            activeTrackColor = skin.secondaryAccentColor,
                            inactiveTrackColor = skin.accentColor
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StudioHardwareButton(
                            text = "CUT L",
                            subText = "100% L",
                            onClick = {
                                notifyEditingOrStudioPlayback()
                                masterCrossfader = 0f
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                            },
                            skin = skin,
                            theme = StudioButtonTheme.RED_REC,
                            isActive = masterCrossfader == 0f,
                            modifier = Modifier.weight(1f)
                        )

                        StudioHardwareButton(
                            text = "CENTER",
                            subText = "50/50",
                            onClick = {
                                notifyEditingOrStudioPlayback()
                                masterCrossfader = 0.5f
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                            },
                            skin = skin,
                            theme = StudioButtonTheme.SLATE_UTILITY,
                            isActive = masterCrossfader == 0.5f,
                            modifier = Modifier.weight(1f)
                        )

                        StudioHardwareButton(
                            text = "CUT R",
                            subText = "100% R",
                            onClick = {
                                notifyEditingOrStudioPlayback()
                                masterCrossfader = 1.0f
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                            },
                            skin = skin,
                            theme = StudioButtonTheme.RED_REC,
                            isActive = masterCrossfader == 1.0f,
                            modifier = Modifier.weight(1f)
                        )

                        StudioHardwareButton(
                            text = "EQ RACK",
                            subText = "DSP PEQ",
                            onClick = { onOpenEqualizerDialog() },
                            skin = skin,
                            theme = StudioButtonTheme.AMBER_CUE,
                            icon = Icons.Default.Equalizer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // DJ Live Session Recording & Preferred Format Extractor
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dj_live_recorder_card"),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = skin.lcdBackground.copy(alpha = 0.90f)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isRecordingDJSession) Color(0xFFFF3D00) else skin.chassisBorderColor)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecordingDJSession) Color(0xFFFF3D00) else Color(0xFF64748B))
                                    .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRecordingDJSession) "● RECORDING LIVE 4-TRACK DJ SET" else "🎛️ 4-TRACK DJ SESSION RECORDER",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isRecordingDJSession) Color(0xFFFF3D00) else skin.titleBarTextColor
                            )
                        }

                        val timeFormatted = String.format("%02d:%02d", recordedSessionSeconds / 60, recordedSessionSeconds % 60)
                        Text(
                            text = "[$timeFormatted]",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isRecordingDJSession) Color(0xFFFF3D00) else skin.lcdTextColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Record live crossfader cuts, transitions, and scratches across all 4 decks to extract into your preferred master audio format.",
                        fontSize = 10.sp,
                        color = skin.lcdTextColor.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!isRecordingDJSession) {
                            StudioHardwareButton(
                                text = "● START LIVE REC",
                                subText = "CAPTURE 4-DECK MIX",
                                onClick = {
                                    notifyEditingOrStudioPlayback()
                                    isRecordingDJSession = true
                                    hasRecordedDJSession = true
                                    recordedSessionSeconds = 0
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                                    if (!isAnyDeckPlaying) toggleDeck("A")
                                },
                                skin = skin,
                                theme = StudioButtonTheme.RED_REC,
                                icon = Icons.Default.FiberManualRecord,
                                height = 38.dp,
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            StudioHardwareButton(
                                text = "■ STOP & EXTRACT",
                                subText = "FINISH SESSION",
                                onClick = {
                                    isRecordingDJSession = false
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                                    djRecordTitle = "Quad DJ Set (${deckASong?.title?.take(8) ?: "A"} x ${deckBSong?.title?.take(8) ?: "B"})"
                                },
                                skin = skin,
                                theme = StudioButtonTheme.RED_REC,
                                isActive = true,
                                icon = Icons.Default.Stop,
                                height = 38.dp,
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (hasRecordedDJSession && !isRecordingDJSession) {
                            StudioHardwareButton(
                                text = if (isPreviewingRecordedSession) "PAUSE PREVIEW" else "▶ PREVIEW SET",
                                subText = "${recordedSessionSeconds}s CAPTURED",
                                onClick = {
                                    notifyEditingOrStudioPlayback()
                                    isPreviewingRecordedSession = !isPreviewingRecordedSession
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.RETRO_CLICK)
                                },
                                skin = skin,
                                theme = StudioButtonTheme.GREEN_PLAY,
                                isActive = isPreviewingRecordedSession,
                                height = 38.dp,
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (hasRecordedDJSession && !isRecordingDJSession) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(skin.lcdBackground)
                                .border(1.dp, skin.lcdBorderColor, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("EXTRACTION FORMAT:", fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = skin.lcdTextColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                val formats = listOf("WAV 1411k", "MP3 320k", "FLAC LOSSLESS", "TAPE DUB")
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    formats.forEach { fmt ->
                                        StudioHardwareButton(
                                            text = fmt,
                                            onClick = { djRecordFormat = fmt },
                                            skin = skin,
                                            theme = if (djRecordFormat == fmt) StudioButtonTheme.CHAMPAGNE_GOLD else StudioButtonTheme.SLATE_UTILITY,
                                            isActive = djRecordFormat == fmt,
                                            height = 30.dp,
                                            fontSize = 8.5.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = djRecordTitle,
                                    onValueChange = { djRecordTitle = it },
                                    label = { Text("Recorded DJ Set Title", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                StudioHardwareButton(
                                    text = "💾 EXTRACT & SAVE RECORDED SET",
                                    subText = "SAVE AS $djRecordFormat",
                                    onClick = {
                                        notifyEditingOrStudioPlayback()
                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CD_SPIN)
                                        val finalTitle = djRecordTitle.ifBlank { "Live Quad DJ Set" }
                                        val finalArtist = djRecordArtist.ifBlank { "DJ Studio Live" }
                                        // Render the loaded decks back-to-back from their cue points with 4s DJ crossfades
                                        val deckParts = listOf(
                                            Triple(deckASong, deckACue, deckAVolume),
                                            Triple(deckBSong, deckBCue, deckBVolume),
                                            Triple(deckCSong, deckCCue, deckCVolume),
                                            Triple(deckDSong, deckDCue, deckDVolume)
                                        ).mapNotNull { (deckSong, cue, vol) ->
                                            if (deckSong == null || deckSong.uri.isBlank() || deckSong.uri.startsWith("synth://")) null
                                            else com.example.audio.StudioRenderEngine.RenderPart(
                                                uri = deckSong.uri,
                                                startMs = (cue.coerceIn(0f, 0.95f) * deckSong.durationMs).toLong(),
                                                gain = vol.coerceIn(0f, 1.5f)
                                            )
                                        }.distinctBy { it.uri }
                                        onExportMix(
                                            deckParts,
                                            4000L,
                                            finalTitle,
                                            finalArtist,
                                            if (djRecordFormat.contains("WAV", ignoreCase = true)) com.example.audio.StudioRenderEngine.OutputFormat.WAV else com.example.audio.StudioRenderEngine.OutputFormat.AAC,
                                            djRecordTargetPlaylist,
                                            deckASong?.albumArtUri ?: deckBSong?.albumArtUri
                                        )
                                    },
                                    skin = skin,
                                    theme = StudioButtonTheme.CHAMPAGNE_GOLD,
                                    icon = Icons.Default.Save,
                                    height = 42.dp,
                                    fontSize = 11.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        StudioExportStatusCard(exportState, skin, onPlaySong, onDismissExport)
                        AnimatedVisibility(visible = showDjExtractSuccessBanner) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2615)),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00FF66))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("✓ DJ SET EXTRACTED TO LIBRARY!", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF00FF66))
                                    Text("\"$djRecordTitle\" saved to database.", fontSize = 9.5.sp, color = Color(0xFFE2E8F0))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // DJ Sound FX Velocity Pads
            Text("⚡ AKAI MPC-STYLE SOUND FX LAUNCHPAD", fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = skin.lcdTextColor, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(6.dp))

            val pads = listOf(
                Triple("01", "AIR HORN", RetroSoundEffects.SoundType.AIR_HORN to Color(0xFFFF3366)),
                Triple("02", "SCRATCH", RetroSoundEffects.SoundType.VINYL_SCRATCH to skin.accentColor),
                Triple("03", "808 DROP", RetroSoundEffects.SoundType.BEAT_DROP to Color(0xFFFF9100)),
                Triple("04", "LASER ZAP", RetroSoundEffects.SoundType.LASER_ZAP to Color(0xFF00FF66)),
                Triple("05", "REWIND", RetroSoundEffects.SoundType.TAPE_REWIND to Color(0xFFB388FF)),
                Triple("06", "SIREN", RetroSoundEffects.SoundType.CLUB_SIREN to Color(0xFFFF5252)),
                Triple("07", "APPLAUSE", RetroSoundEffects.SoundType.CROWD_APPLAUSE to Color(0xFFFFD700)),
                Triple("08", "SNARE ROLL", RetroSoundEffects.SoundType.SNARE_ROLL to skin.secondaryAccentColor)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pads.take(4).forEach { (num, label, data) ->
                    val (soundType, padColor) = data
                    DJSoundPad(
                        padNumber = num,
                        label = label,
                        color = padColor,
                        skin = skin,
                        onTrigger = {
                            notifyEditingOrStudioPlayback()
                            RetroSoundEffects.playClick(soundType)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pads.drop(4).forEach { (num, label, data) ->
                    val (soundType, padColor) = data
                    DJSoundPad(
                        padNumber = num,
                        label = label,
                        color = padColor,
                        skin = skin,
                        onTrigger = {
                            notifyEditingOrStudioPlayback()
                            RetroSoundEffects.playClick(soundType)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

        } else if (studioMode == 1) {
            // =========================================================================
            // MULTI-CLIP SOUND SPLICER, PORTION REMOVAL & NOISE REDUCTION
            // =========================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = skin.lcdBackground.copy(alpha = 0.90f)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, skin.chassisBorderColor)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🎚️ MULTI-CLIP SOUND SPLICER",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = skin.titleBarTextColor
                            )
                            Text(
                                text = "${splicerClips.size} CLIPS LOADED • SURGICAL PORTION CUTTER • DSP NOISE REMOVAL",
                                fontSize = 8.5.sp,
                                color = skin.lcdTextColor.copy(alpha = 0.6f)
                            )
                        }

                        // Button to Add Sound (Tracks, Mic, Studio SFX)
                        StudioHardwareButton(
                            text = "+ ADD SOUND",
                            subText = "TRACK/MIC/SFX",
                            onClick = {
                                notifyEditingOrStudioPlayback()
                                showAddSoundDialog = true
                            },
                            skin = skin,
                            theme = StudioButtonTheme.CYAN_SYNC,
                            icon = Icons.Default.Add,
                            height = 32.dp,
                            fontSize = 8.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // MULTI-CLIP LIST RACK (Supporting >2 clips loaded and spliced together)
                    Text("LOADED CLIPS TO SPLICE TOGETHER (${splicerClips.size} TOTAL):", fontSize = 8.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(splicerClips) { index, clip ->
                            val isSelected = selectedClipIndex == index
                            val clipColor = Color(clip.colorHex)

                            Box(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF1E283A) else Color(0xFF101420))
                                    .border(
                                        1.5.dp,
                                        if (isSelected) clipColor else Color(0xFF263045),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        selectedClipIndex = index
                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.RETRO_CLICK)
                                    }
                                    .padding(6.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = clipColor
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(clip.sourceType.badgeColor).copy(alpha = 0.2f))
                                                .padding(horizontal = 3.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = clip.sourceType.label,
                                                fontSize = 6.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(clip.sourceType.badgeColor)
                                            )
                                        }
                                    }

                                    Text(
                                        text = clip.title,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "${clip.durationSec}s • ${if (clip.isDenoiseEnabled) "DENOISED" else "RAW"}",
                                        fontSize = 7.5.sp,
                                        color = skin.lcdTextColor.copy(alpha = 0.6f)
                                    )

                                    // Quick Clip Actions (Move Left/Right, Delete)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            if (index > 0) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowBack,
                                                    contentDescription = "Move Left",
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            val updated = splicerClips.toMutableList()
                                                            val item = updated.removeAt(index)
                                                            updated.add(index - 1, item)
                                                            splicerClips = updated
                                                            selectedClipIndex = index - 1
                                                        }
                                                )
                                            }
                                            if (index < splicerClips.size - 1) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowForward,
                                                    contentDescription = "Move Right",
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            val updated = splicerClips.toMutableList()
                                                            val item = updated.removeAt(index)
                                                            updated.add(index + 1, item)
                                                            splicerClips = updated
                                                            selectedClipIndex = index + 1
                                                        }
                                                )
                                            }
                                        }

                                        if (splicerClips.size > 1) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete Clip",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable {
                                                        val updated = splicerClips.toMutableList()
                                                        updated.removeAt(index)
                                                        splicerClips = updated
                                                        selectedClipIndex = max(0, index - 1)
                                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // SELECTED CLIP SURGICAL WAVEFORM & PORTION REMOVAL RACK
                    val activeClip = splicerClips.getOrNull(selectedClipIndex)
                    if (activeClip != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CLIP #${selectedClipIndex + 1}: ${activeClip.title.uppercase()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(activeClip.colorHex)
                            )
                            Text(
                                text = "${activeClip.activeSegments.size} SEGMENTS",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Interactive Multi-Segment Waveform View
                        SplicerWaveformSegmentView(
                            clip = activeClip,
                            playheadProgress = splicerPlayheadProgress,
                            skin = skin,
                            onSelectionChange = { s, e ->
                                val updated = splicerClips.toMutableList()
                                updated[selectedClipIndex] = activeClip.copy(selectionStart = s, selectionEnd = e)
                                splicerClips = updated
                            },
                            onSeek = { splicerPlayheadProgress = it },
                            height = 76.dp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Selection In & Out Sliders
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CUT IN: ${(activeClip.selectionStart * 100).toInt()}%", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                                Slider(
                                    value = activeClip.selectionStart,
                                    onValueChange = {
                                        val updated = splicerClips.toMutableList()
                                        updated[selectedClipIndex] = activeClip.copy(selectionStart = it.coerceAtMost(activeClip.selectionEnd - 0.02f))
                                        splicerClips = updated
                                    },
                                    valueRange = 0f..0.98f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF5252), activeTrackColor = Color(0xFFFF5252))
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("CUT OUT: ${(activeClip.selectionEnd * 100).toInt()}%", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                                Slider(
                                    value = activeClip.selectionEnd,
                                    onValueChange = {
                                        val updated = splicerClips.toMutableList()
                                        updated[selectedClipIndex] = activeClip.copy(selectionEnd = it.coerceAtLeast(activeClip.selectionStart + 0.02f))
                                        splicerClips = updated
                                    },
                                    valueRange = 0.02f..1.0f,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF5252), activeTrackColor = Color(0xFFFF5252))
                                )
                            }
                        }

                        // PORTION REMOVAL TOOLBAR: Cut at Playhead, Remove Portion, Trim, Restore
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StudioHardwareButton(
                                text = "✂️ SPLIT PLAYHEAD",
                                subText = "DIVIDE CLIP",
                                onClick = {
                                    // Split clip at playhead position into 2 clips
                                    val splitPoint = splicerPlayheadProgress.coerceIn(0.1f, 0.9f)
                                    val part1 = activeClip.copy(
                                        id = java.util.UUID.randomUUID().toString(),
                                        title = "${activeClip.title} (Part 1)",
                                        durationSec = (activeClip.durationSec * splitPoint).toInt().coerceAtLeast(1),
                                        activeSegments = listOf(ClipSegment(0f, splitPoint))
                                    )
                                    val part2 = activeClip.copy(
                                        id = java.util.UUID.randomUUID().toString(),
                                        title = "${activeClip.title} (Part 2)",
                                        durationSec = (activeClip.durationSec * (1f - splitPoint)).toInt().coerceAtLeast(1),
                                        activeSegments = listOf(ClipSegment(splitPoint, 1f))
                                    )
                                    val updated = splicerClips.toMutableList()
                                    updated.removeAt(selectedClipIndex)
                                    updated.add(selectedClipIndex, part2)
                                    updated.add(selectedClipIndex, part1)
                                    splicerClips = updated
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.VINYL_SCRATCH)
                                },
                                skin = skin,
                                theme = StudioButtonTheme.CYAN_SYNC,
                                height = 34.dp,
                                fontSize = 8.5.sp,
                                modifier = Modifier.weight(1f)
                            )

                            StudioHardwareButton(
                                text = "🗑️ REMOVE PORTION",
                                subText = "CUT RANGE",
                                onClick = {
                                    // Remove the selected portion from active segments
                                    val selStart = min(activeClip.selectionStart, activeClip.selectionEnd)
                                    val selEnd = max(activeClip.selectionStart, activeClip.selectionEnd)
                                    val newSegments = mutableListOf<ClipSegment>()
                                    for (seg in activeClip.activeSegments) {
                                        if (selEnd <= seg.startPercent || selStart >= seg.endPercent) {
                                            // Completely outside
                                            newSegments.add(seg)
                                        } else {
                                            // Overlaps
                                            if (seg.startPercent < selStart) {
                                                newSegments.add(ClipSegment(seg.startPercent, selStart))
                                            }
                                            if (seg.endPercent > selEnd) {
                                                newSegments.add(ClipSegment(selEnd, seg.endPercent))
                                            }
                                        }
                                    }
                                    val updated = splicerClips.toMutableList()
                                    updated[selectedClipIndex] = activeClip.copy(activeSegments = if (newSegments.isEmpty()) listOf(ClipSegment(0f, 0.05f)) else newSegments)
                                    splicerClips = updated
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                                },
                                skin = skin,
                                theme = StudioButtonTheme.RED_REC,
                                height = 34.dp,
                                fontSize = 8.5.sp,
                                modifier = Modifier.weight(1f)
                            )

                            StudioHardwareButton(
                                text = "🎯 TRIM TO RANGE",
                                subText = "KEEP SELECTED",
                                onClick = {
                                    val selStart = min(activeClip.selectionStart, activeClip.selectionEnd)
                                    val selEnd = max(activeClip.selectionStart, activeClip.selectionEnd)
                                    val updated = splicerClips.toMutableList()
                                    updated[selectedClipIndex] = activeClip.copy(activeSegments = listOf(ClipSegment(selStart, selEnd)))
                                    splicerClips = updated
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                },
                                skin = skin,
                                theme = StudioButtonTheme.AMBER_CUE,
                                height = 34.dp,
                                fontSize = 8.5.sp,
                                modifier = Modifier.weight(1f)
                            )

                            StudioHardwareButton(
                                text = "↺ RESTORE",
                                subText = "CLEAR CUTS",
                                onClick = {
                                    val updated = splicerClips.toMutableList()
                                    updated[selectedClipIndex] = activeClip.copy(activeSegments = listOf(ClipSegment(0f, 1f)))
                                    splicerClips = updated
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.RETRO_CLICK)
                                },
                                skin = skin,
                                theme = StudioButtonTheme.SLATE_UTILITY,
                                height = 34.dp,
                                fontSize = 8.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // BACKGROUND NOISE REMOVER PANEL (For recorded clips or noisy stems)
                        NoiseReductionPanel(
                            clip = activeClip,
                            skin = skin,
                            onUpdateDenoise = { enabled, thresh, red, hiss ->
                                val updated = splicerClips.toMutableList()
                                updated[selectedClipIndex] = activeClip.copy(
                                    isDenoiseEnabled = enabled,
                                    noiseGateThresholdDb = thresh,
                                    noiseReductionAmount = red,
                                    hissCutoffKhz = hiss
                                )
                                splicerClips = updated
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // MASTER SPLICED SEQUENCE OVERVIEW
                    Text("MASTER SPLICED TIMELINE PREVIEW:", fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF070911))
                            .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val totalDuration = splicerClips.sumOf { it.durationSec }.coerceAtLeast(1)

                            var currentX = 0f
                            splicerClips.forEachIndexed { idx, clip ->
                                val clipW = (clip.durationSec.toFloat() / totalDuration) * w
                                val clipColor = Color(clip.colorHex)

                                drawRect(
                                    color = clipColor.copy(alpha = 0.25f),
                                    topLeft = Offset(currentX, 0f),
                                    size = Size(clipW, h)
                                )
                                drawLine(
                                    color = clipColor,
                                    start = Offset(currentX, 0f),
                                    end = Offset(currentX, h),
                                    strokeWidth = 2f
                                )
                                currentX += clipW
                            }

                            // Splicer Preview Playhead
                            if (isSplicerPreviewPlaying) {
                                val playheadX = splicerPlayheadProgress * w
                                drawLine(
                                    color = Color.White,
                                    start = Offset(playheadX, 0f),
                                    end = Offset(playheadX, h),
                                    strokeWidth = 2.5f
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TRANSITION: $spliceTransitionMode",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFFD54F)
                            )
                            val totalSec = splicerClips.sumOf { it.durationSec }
                            Text(
                                text = "TOTAL: ${String.format("%02d:%02d", totalSec / 60, totalSec % 60)}",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Splicer Transition Modes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("BUTT SPLICE", "CROSSFADE (1.5s)", "VINYL DROP", "TAPE STOP").forEach { mode ->
                            val isSel = spliceTransitionMode == mode
                            StudioHardwareButton(
                                text = mode,
                                onClick = { spliceTransitionMode = mode },
                                skin = skin,
                                theme = if (isSel) StudioButtonTheme.AMBER_CUE else StudioButtonTheme.SLATE_UTILITY,
                                isActive = isSel,
                                height = 28.dp,
                                fontSize = 7.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Splicer Preview Button
                    StudioHardwareButton(
                        text = if (isSplicerPreviewPlaying) "PAUSE SPLICED SEQUENCE" else "▶ PLAY SPLICED MASTER SEQUENCE",
                        subText = "AUDITION ${splicerClips.size} CLIPS JOINED",
                        onClick = { toggleSplicerPreview() },
                        skin = skin,
                        theme = if (isSplicerPreviewPlaying) StudioButtonTheme.RED_REC else StudioButtonTheme.GREEN_PLAY,
                        isActive = isSplicerPreviewPlaying,
                        icon = if (isSplicerPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        height = 40.dp,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Master Dubbing & Export Rack (Splicer)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = skin.lcdBackground.copy(alpha = 0.90f)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD54F).copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                    Text(
                        text = "💾 SPLICER MASTER EXPORT & DUBBING",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFFD54F)
                    )
                    Text(
                        text = "Export and save ${splicerClips.size} spliced clips directly as a single playable master track in your music library",
                        fontSize = 10.sp,
                        color = skin.lcdTextColor.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = mixExportTitle,
                        onValueChange = {
                            notifyEditingOrStudioPlayback()
                            mixExportTitle = it
                        },
                        label = { Text("Export Track Title", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = mixExportArtist,
                        onValueChange = {
                            notifyEditingOrStudioPlayback()
                            mixExportArtist = it
                        },
                        label = { Text("Artist / DJ Name", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("MASTER AUDIO FORMAT:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = skin.lcdTextColor, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val formats = listOf("WAV (44.1k)", "CASSETTE", "MP3 320k")
                        formats.forEach { fmt ->
                            StudioHardwareButton(
                                text = fmt,
                                onClick = {
                                    notifyEditingOrStudioPlayback()
                                    exportFormat = fmt
                                },
                                skin = skin,
                                theme = if (exportFormat == fmt) StudioButtonTheme.CHAMPAGNE_GOLD else StudioButtonTheme.SLATE_UTILITY,
                                isActive = exportFormat == fmt,
                                height = 30.dp,
                                fontSize = 8.5.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    StudioHardwareButton(
                        text = "💾 EXPORT & SAVE SPLICED MASTER",
                        subText = "SAVE TO ROOM DATABASE",
                        onClick = {
                            notifyEditingOrStudioPlayback()
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CD_SPIN)
                            val finalTitle = mixExportTitle.ifBlank { "Spliced Master Mix (${splicerClips.size} Clips)" }
                            val finalArtist = mixExportArtist.ifBlank { "DJ Sound Splicer" }
                            // Every active (non-deleted) segment of every un-muted clip, in timeline order
                            val spliceParts = splicerClips
                                .filter { !it.isMuted && !it.uri.isNullOrBlank() && !it.uri.startsWith("synth://") }
                                .flatMap { clip ->
                                    val clipMs = clip.durationSec * 1000L
                                    clip.activeSegments.sortedBy { it.startPercent }.map { seg ->
                                        com.example.audio.StudioRenderEngine.RenderPart(
                                            uri = clip.uri ?: "",
                                            startMs = (seg.startPercent.coerceIn(0f, 1f) * clipMs).toLong(),
                                            endMs = (seg.endPercent.coerceIn(0f, 1f) * clipMs).toLong(),
                                            gain = clip.volume,
                                            noiseGateDb = if (clip.isDenoiseEnabled) clip.noiseGateThresholdDb else null
                                        )
                                    }
                                }
                            val xfadeSeconds = Regex("""([0-9]+(\.[0-9]+)?)\s*s""").find(spliceTransitionMode)?.groupValues?.get(1)?.toFloatOrNull()
                            val xfadeMs = if (spliceTransitionMode.contains("CROSSFADE", ignoreCase = true)) ((xfadeSeconds ?: 1.5f) * 1000).toLong() else 0L
                            onExportMix(
                                spliceParts,
                                xfadeMs,
                                finalTitle,
                                finalArtist,
                                if (exportFormat.contains("WAV", ignoreCase = true)) com.example.audio.StudioRenderEngine.OutputFormat.WAV else com.example.audio.StudioRenderEngine.OutputFormat.AAC,
                                exportTargetPlaylist,
                                deckASong?.albumArtUri ?: deckBSong?.albumArtUri
                            )
                        },
                        skin = skin,
                        theme = StudioButtonTheme.CHAMPAGNE_GOLD,
                        icon = Icons.Default.Save,
                        height = 42.dp,
                        fontSize = 11.sp,
                        testTag = "export_save_track_button",
                        modifier = Modifier.fillMaxWidth()
                    )

                    StudioExportStatusCard(exportState, skin, onPlaySong, onDismissExport)
                    AnimatedVisibility(visible = showExportSuccessBanner) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2615)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00FF66))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00FF66), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "TRACK EXPORTED & SAVED TO LIBRARY!",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF00FF66)
                                    )
                                }
                                Text(
                                    text = "\"$mixExportTitle\" is now saved in Room database and accessible across all playlists.",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    StudioHardwareButton(
                                        text = "▶ PLAY NOW",
                                        onClick = { recentlyExportedSong?.let { onPlaySong(it) } },
                                        skin = skin,
                                        theme = StudioButtonTheme.GREEN_PLAY,
                                        height = 30.dp,
                                        fontSize = 8.5.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StudioHardwareButton(
                                        text = "LOAD DECK A",
                                        onClick = {
                                            recentlyExportedSong?.let { deckASong = it }
                                            studioMode = 0
                                        },
                                        skin = skin,
                                        theme = StudioButtonTheme.CYAN_SYNC,
                                        height = 30.dp,
                                        fontSize = 8.5.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StudioHardwareButton(
                                        text = "VIEW LIBRARY",
                                        onClick = { onNavigateToTab?.invoke(1) },
                                        skin = skin,
                                        theme = StudioButtonTheme.SLATE_UTILITY,
                                        height = 30.dp,
                                        fontSize = 8.5.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (studioMode == 2) {
            RingtoneMakerPanel(
                songs = allSongs,
                initialSong = currentSong?.takeIf { it.source == com.example.data.model.AudioSource.LOCAL } ?: allSongs.firstOrNull(),
                skin = skin,
                exportState = exportState,
                onBeforePreview = { notifyEditingOrStudioPlayback() },
                onExport = onExportRingtone,
                onPlayExport = onPlaySong,
                onDismissExport = onDismissExport
            )
        } else {
            VocalRemoverPanel(
                songs = allSongs,
                initialSong = currentSong?.takeIf { it.source == com.example.data.model.AudioSource.LOCAL } ?: allSongs.firstOrNull(),
                skin = skin,
                exportState = exportState,
                onExport = onExportVocals,
                onPlayExport = onPlaySong,
                onDismissExport = onDismissExport
            )
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
