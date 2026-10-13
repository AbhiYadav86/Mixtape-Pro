package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RetroSoundEffects
import com.example.data.model.PlayerUiScale
import com.example.data.model.RetroSkin

@Composable
fun RetroControlBar(
    isPlaying: Boolean,
    skin: RetroSkin,
    scale: PlayerUiScale = PlayerUiScale.STANDARD,
    scaleFactor: Float = 1.0f,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onNext: () -> Unit,
    onOpenTrackMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 4.dp,
                vertical = if (scale == PlayerUiScale.COMPACT) 2.dp else 4.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        val totalWidth = maxWidth
        // 5 buttons: PREV, PLAY/PAUSE, STOP, NEXT, OPEN
        // Standard buttons share available space, Play/Pause is hero with ~1.22x width
        val interButtonGap = if (totalWidth < 340.dp) 2.dp else 4.dp
        val availableWidth = (totalWidth - (interButtonGap * 4) - 6.dp).coerceAtLeast(180.dp)
        val standardWidth = (availableWidth / 5.25f).coerceIn(36.dp, 56.dp)
        val heroWidth = (standardWidth * 1.25f).coerceIn(44.dp, 68.dp)

        val standardHeight = (standardWidth * 0.88f).coerceIn(34.dp, 48.dp)
        val heroHeight = (heroWidth * 0.88f).coerceIn(38.dp, 52.dp)

        val standardIcon = (standardWidth * 0.48f).coerceIn(18.dp, 26.dp)
        val heroIcon = (heroWidth * 0.52f).coerceIn(22.dp, 32.dp)

        val labelFontSize = if (totalWidth < 340.dp) 8.5.sp else 9.5.sp

        // Physical Master Transport Controls Row (PREV, PLAY/PAUSE, STOP, NEXT, OPEN)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Track Button
            RetroBeveledButton(
                icon = Icons.Default.SkipPrevious,
                label = "PREV",
                skin = skin,
                scale = scale,
                scaleFactor = scaleFactor,
                customWidth = standardWidth,
                customHeight = standardHeight,
                customIconSize = standardIcon,
                customLabelSize = labelFontSize,
                testTag = "prev_button",
                onClick = onPrevious
            )

            // Play / Pause Button (Hero button - Resized & Prominent)
            RetroBeveledButton(
                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                label = if (isPlaying) "PAUSE" else "PLAY",
                isHero = true,
                skin = skin,
                scale = scale,
                scaleFactor = scaleFactor,
                customWidth = heroWidth,
                customHeight = heroHeight,
                customIconSize = heroIcon,
                customLabelSize = labelFontSize,
                testTag = "play_pause_button",
                onClick = onPlayPause
            )

            // Stop Button
            RetroBeveledButton(
                icon = Icons.Default.Stop,
                label = "STOP",
                skin = skin,
                scale = scale,
                scaleFactor = scaleFactor,
                customWidth = standardWidth,
                customHeight = standardHeight,
                customIconSize = standardIcon,
                customLabelSize = labelFontSize,
                testTag = "stop_button",
                onClick = onStop
            )

            // Next Track Button
            RetroBeveledButton(
                icon = Icons.Default.SkipNext,
                label = "NEXT",
                skin = skin,
                scale = scale,
                scaleFactor = scaleFactor,
                customWidth = standardWidth,
                customHeight = standardHeight,
                customIconSize = standardIcon,
                customLabelSize = labelFontSize,
                testTag = "next_button",
                onClick = onNext
            )

            // Open / Eject Track Menu Button (Directly beside NEXT)
            RetroBeveledButton(
                icon = Icons.Default.FolderOpen,
                label = "OPEN",
                skin = skin,
                scale = scale,
                scaleFactor = scaleFactor,
                customWidth = standardWidth,
                customHeight = standardHeight,
                customIconSize = standardIcon,
                customLabelSize = labelFontSize,
                testTag = "open_track_menu_button",
                onClick = onOpenTrackMenu
            )
        }
    }
}

@Composable
fun RetroBeveledButton(
    icon: ImageVector,
    label: String,
    skin: RetroSkin,
    testTag: String,
    isHero: Boolean = false,
    customWidth: Dp? = null,
    customHeight: Dp? = null,
    customIconSize: Dp? = null,
    customLabelSize: TextUnit? = null,
    scale: PlayerUiScale = PlayerUiScale.STANDARD,
    scaleFactor: Float = 1.0f,
    onClick: () -> Unit
) {
    val multiplier = scaleFactor / scale.factor
    val btnWidth = customWidth ?: run {
        val baseSize = if (isHero) scale.heroButtonSize else scale.normalButtonSize
        (baseSize * multiplier).coerceIn(38.dp, 70.dp)
    }
    val btnHeight = customHeight ?: run {
        val baseSize = if (isHero) scale.heroButtonSize else scale.normalButtonSize
        (baseSize * multiplier).coerceIn(38.dp, 70.dp)
    }
    val iconSize = customIconSize ?: run {
        val baseIconSize = if (isHero) scale.heroIconSize else scale.normalIconSize
        (baseIconSize * multiplier).coerceIn(18.dp, 36.dp)
    }
    val labelSize = customLabelSize ?: (scale.buttonLabelSize * multiplier).coerceIn(8.5f, 13f).sp
    val horizontalPad = 1.dp

    val shape = RoundedCornerShape(skin.buttonShapeRounded.coerceAtLeast(6).dp)

    // Interaction Source for authentic 3D mechanical depression
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) 2.5.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btn_press_y"
    )
    val pressElevation by animateDpAsState(
        targetValue = if (isPressed) 0.5.dp else 5.dp,
        animationSpec = tween(durationMillis = 50),
        label = "btn_press_elevation"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.955f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_press_scale"
    )

    // High contrast label color: ensure text on colored/dark chassis has crisp contrast
    val labelColor = when {
        skin.id == "acid_techno" -> Color(0xFFFACC15) // Bright toxic yellow on black
        skin.id == "y2k_bubblegum" -> Color(0xFFFFFFFF) // Crisp white on hot pink
        skin.id == "winamp_bento" -> Color(0xFF38BDF8) // Electric cyan on dark slate
        skin.id == "imac_bondi" -> Color(0xFFE0FBFC) // High-contrast ice-cyan white on Bondi Blue chassis
        skin.isWin98Style -> Color(0xFF0F172A)
        skin.id == "discman_esp" -> Color(0xFF0F172A)
        skin.id == "cyber_matrix" -> Color(0xFF4ADE80)
        skin.id == "hacker_terminal" -> Color(0xFFFBBF24)
        skin.id == "hot_rod_flame" -> Color(0xFFFEF08A)
        skin.id in listOf("grape_imac", "lime_imac", "ruby_imac") -> Color(0xFFFFFFFF)
        else -> skin.buttonIconColor
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = horizontalPad, vertical = 1.dp)
    ) {
        // 1. Recessed Chassis Socket Well (Deep Cutout Frame)
        Box(
            modifier = Modifier
                .width(btnWidth)
                .height(btnHeight)
                .background(Color(0xFF07090F), shape)
                .border(1.2.dp, Color(0x60000000), shape)
                .padding(1.dp),
            contentAlignment = Alignment.Center
        ) {
            // 2. Realistic 3D Molded Moving Button Body
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = pressOffsetY.toPx()
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .testTag(testTag)
                    .shadow(pressElevation, shape)
                    .clip(shape)
                    .background(
                        if (isHero) {
                            if (isPressed) {
                                Brush.verticalGradient(
                                    listOf(
                                        skin.accentColor.copy(alpha = 0.60f),
                                        skin.accentColor.copy(alpha = 0.32f),
                                        Color(0xFF0D121B)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        skin.accentColor.copy(alpha = 0.48f),
                                        skin.accentColor.copy(alpha = 0.22f),
                                        skin.accentColor.copy(alpha = 0.38f),
                                        Color(0xFF0A0D15)
                                    )
                                )
                            }
                        } else {
                            if (isPressed) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF141A25),
                                        Color(0xFF0C1018)
                                    )
                                )
                            } else skin.buttonBackground
                        }
                    )
                    .border(
                        width = if (isPressed) 1.2.dp else 1.8.dp,
                        color = if (isHero) skin.accentColor
                                else if (isPressed) skin.buttonBorderColor.copy(alpha = 0.70f)
                                else skin.buttonBorderColor,
                        shape = shape
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                        onClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                // 3. Realistic 3D Chamfer, Specular Sheen, Bottom Extrusion & Tactile Grip Ridges
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    if (!isPressed) {
                        // Top specular chamfer line (illumination from studio light)
                        drawLine(
                            color = Color.White.copy(alpha = if (isHero) 0.65f else 0.42f),
                            start = Offset(2f, 1.2f),
                            end = Offset(w - 2f, 1.2f),
                            strokeWidth = 2.dp.toPx()
                        )
                        // Bottom 3D physical extrusion ledge (giving realistic button thickness)
                        drawLine(
                            color = Color.Black.copy(alpha = 0.60f),
                            start = Offset(2f, h - 1.5f),
                            end = Offset(w - 2f, h - 1.5f),
                            strokeWidth = 3.dp.toPx()
                        )

                        // Subtle horizontal tactile grip ridges for authentic Walkman/Hi-Fi feel (non-hero buttons)
                        if (!isHero) {
                            val midY = h * 0.48f
                            // Ridge 1
                            drawLine(
                                color = Color.White.copy(alpha = 0.12f),
                                start = Offset(w * 0.22f, midY - 2.5f),
                                end = Offset(w * 0.78f, midY - 2.5f),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color.Black.copy(alpha = 0.25f),
                                start = Offset(w * 0.22f, midY - 1.5f),
                                end = Offset(w * 0.78f, midY - 1.5f),
                                strokeWidth = 1f
                            )
                            // Ridge 2
                            drawLine(
                                color = Color.White.copy(alpha = 0.12f),
                                start = Offset(w * 0.22f, midY + 3.5f),
                                end = Offset(w * 0.78f, midY + 3.5f),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color.Black.copy(alpha = 0.25f),
                                start = Offset(w * 0.22f, midY + 4.5f),
                                end = Offset(w * 0.78f, midY + 4.5f),
                                strokeWidth = 1f
                            )
                        }
                    } else {
                        // Pressed: Top inset shadow
                        drawLine(
                            color = Color.Black.copy(alpha = 0.70f),
                            start = Offset(1f, 1f),
                            end = Offset(w - 1f, 1f),
                            strokeWidth = 3.dp.toPx()
                        )
                        // Caught bottom rim light
                        drawLine(
                            color = Color.White.copy(alpha = 0.18f),
                            start = Offset(1f, h - 1f),
                            end = Offset(w - 1f, h - 1f),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }

                // Ambient specular highlight layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                if (isPressed) {
                                    listOf(Color(0x45000000), Color.Transparent, Color(0x10FFFFFF))
                                } else {
                                    listOf(Color(0x35FFFFFF), Color.Transparent, Color(0x28000000))
                                }
                            )
                        )
                )

                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isHero) (if (isPressed) Color.White else skin.accentColor) else skin.buttonIconColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = labelSize,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 0.4.sp,
            color = labelColor
        )
    }
}
