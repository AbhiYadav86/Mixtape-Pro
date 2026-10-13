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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.audio.RetroSoundEffects

/**
 * Authentic mechanical retro audio hardware button with realistic 3D molded cap,
 * chamfered specular bevels, physical bottom extrusion ledge, recessed chassis socket,
 * and tactile mechanical sound feedback.
 */
@Composable
fun RetroTactileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(5.dp),
    backgroundColor: Color = Color(0xFF232A38),
    backgroundBrush: Brush? = null,
    borderUnpressed: Color = Color(0xFF4B5563),
    borderPressed: Color = Color(0xFF374151),
    accentHighlight: Color? = null,
    elevation: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 9.dp, vertical = 5.dp),
    soundType: RetroSoundEffects.SoundType = RetroSoundEffects.SoundType.WINAMP_CLICK,
    content: @Composable BoxScope.(isPressed: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) 2.2.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tactile_press_y"
    )
    val pressElevation by animateDpAsState(
        targetValue = if (isPressed) 0.5.dp else elevation,
        animationSpec = tween(durationMillis = 50),
        label = "tactile_press_elevation"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tactile_press_scale"
    )

    // 1. Deep Recessed Chassis Socket Frame (Chassis Well)
    Box(
        modifier = modifier
            .background(Color(0xFF090C12), shape)
            .border(1.dp, Color(0x40000000), shape)
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        // 2. Physical 3D Moving Button Cap
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = pressOffsetY.toPx()
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .shadow(pressElevation, shape)
                .clip(shape)
                .background(
                    backgroundBrush ?: if (isPressed) {
                        Brush.verticalGradient(
                            listOf(
                                backgroundColor.copy(alpha = 0.85f),
                                Color(0xFF10141D)
                            )
                        )
                    } else {
                        // Multi-stop brushed satin specular gradient
                        Brush.verticalGradient(
                            listOf(
                                backgroundColor.copy(alpha = 1.0f),
                                backgroundColor,
                                backgroundColor.copy(alpha = 0.88f),
                                Color(0xFF131722)
                            )
                        )
                    }
                )
                .border(
                    width = if (isPressed) 1.dp else 1.2.dp,
                    color = accentHighlight ?: if (isPressed) borderPressed else borderUnpressed,
                    shape = shape
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    RetroSoundEffects.playClick(soundType)
                    onClick()
                }
        ) {
            // Realistic 3D Chamfer Highlights & Extrusion Layers
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                if (!isPressed) {
                    // Top Chamfer Specular Reflection (Overhead studio lighting)
                    drawLine(
                        color = Color.White.copy(alpha = 0.40f),
                        start = Offset(2f, 1f),
                        end = Offset(w - 2f, 1f),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Bottom 3D Extrusion Lip / Physical Thickness Shadow
                    drawLine(
                        color = Color.Black.copy(alpha = 0.55f),
                        start = Offset(2f, h - 1.5f),
                        end = Offset(w - 2f, h - 1.5f),
                        strokeWidth = 3.dp.toPx()
                    )
                } else {
                    // Pressed: Top Inset Recess Shadow
                    drawLine(
                        color = Color.Black.copy(alpha = 0.65f),
                        start = Offset(1f, 1f),
                        end = Offset(w - 1f, 1f),
                        strokeWidth = 3.dp.toPx()
                    )
                    // Bottom caught light
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(1f, h - 1f),
                        end = Offset(w - 1f, h - 1f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(contentPadding)
                    .padding(bottom = if (isPressed) 0.dp else 1.5.dp),
                contentAlignment = Alignment.Center
            ) {
                content(isPressed)
            }
        }
    }
}
