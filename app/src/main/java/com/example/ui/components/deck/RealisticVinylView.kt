package com.example.ui.components.deck

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Song
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ultra-realistic, high-resolution Vinyl LP Record and Turntable visualization.
 * Features:
 * - Machined aluminum turntable platter with authentic stroboscopic speed markings.
 * - Deep black 12" vinyl LP with 4 recorded music bands and dead-wax gap bands.
 * - Iconic anisotropic "butterfly" dual specular light reflections across microgrooves.
 * - Run-out dead wax groove with matrix etching and lead-out spiral.
 * - Vintage circular paper label with album art & golden typography.
 * - Authentic phonograph S-shaped tonearm with counterweight, headshell & stylus
 *   that dynamically tracks across the record based on playback progress.
 */
@Composable
fun RealisticVinylView(
    song: Song?,
    rotation: Float,
    isPlaying: Boolean,
    playbackProgress: Float = 0f,
    deckHeight: Dp = 230.dp,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val platterSize = minOf(totalHeight, totalWidth * 0.92f).coerceIn(160.dp, 280.dp)
        val vinylSize = platterSize * 0.92f

        // Turntable Chassis Well Background
        Box(
            modifier = Modifier
                .size(platterSize * 1.08f)
                .shadow(12.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF1F242D), Color(0xFF14171E), Color(0xFF0D0F14))
                    )
                )
                .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            // 1. Heavy Machined Turntable Platter with Stroboscopic Rim Dots
            TurntablePlatter(platterSize = platterSize, isPlaying = isPlaying, rotation = rotation)

            // 2. High-Resolution Vinyl Record (Rotates when playing)
            VinylRecordDisc(
                song = song,
                vinylSize = vinylSize,
                rotation = if (isPlaying) rotation else 0f
            )

            // 3. Phonograph Tonearm with Headshell & Stylus Needle
            PhonographTonearm(
                platterSize = platterSize,
                vinylSize = vinylSize,
                isPlaying = isPlaying,
                playbackProgress = playbackProgress
            )
        }
    }
}

/**
 * Heavy die-cast aluminum turntable platter with stroboscopic speed dots.
 */
@Composable
private fun TurntablePlatter(
    platterSize: Dp,
    isPlaying: Boolean,
    rotation: Float
) {
    Canvas(
        modifier = Modifier
            .size(platterSize)
            .shadow(8.dp, CircleShape)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f

        // Cast aluminum rim with radial gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1E242B),
                    Color(0xFF2C3440),
                    Color(0xFF475569),
                    Color(0xFF64748B),
                    Color(0xFF334155),
                    Color(0xFF1E293B)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Machined strobe dots along the platter rim (33 & 45 RPM marks)
        val dotCount = 64
        val strobeRadius1 = radius * 0.965f
        val strobeRadius2 = radius * 0.935f
        val baseAngle = if (isPlaying) rotation else 0f

        for (i in 0 until dotCount) {
            val angleRad = Math.toRadians((baseAngle + (i * 360f / dotCount)).toDouble())
            val x1 = center.x + (strobeRadius1 * cos(angleRad)).toFloat()
            val y1 = center.y + (strobeRadius1 * sin(angleRad)).toFloat()
            val x2 = center.x + (strobeRadius2 * cos(angleRad + 0.05)).toFloat()
            val y2 = center.y + (strobeRadius2 * sin(angleRad + 0.05)).toFloat()

            drawCircle(
                color = Color(0xFFF1F5F9),
                radius = 1.1.dp.toPx(),
                center = Offset(x1, y1)
            )
            drawCircle(
                color = Color(0xFFCBD5E1),
                radius = 0.9.dp.toPx(),
                center = Offset(x2, y2)
            )
        }

        // Felt slipmat outer ring
        drawCircle(
            color = Color(0xFF11141A),
            radius = radius * 0.91f,
            center = center
        )
    }
}

/**
 * Ultra-realistic Vinyl LP Disc with distinct music bands, microgrooves,
 * anisotropic specular light sheen, dead wax run-out zone, and vintage center label.
 */
@Composable
private fun VinylRecordDisc(
    song: Song?,
    vinylSize: Dp,
    rotation: Float
) {
    Box(
        modifier = Modifier
            .size(vinylSize)
            .shadow(10.dp, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Rotating Vinyl Layer: Vinyl Base + Grooves + Track Gaps + Run-out Groove
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f
            val leadInRadius = outerRadius * 0.97f
            val labelRadius = outerRadius * 0.35f
            val deadWaxRadius = outerRadius * 0.44f

            // 1. Deep Obsidian Black Vinyl Resin Body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF14171E),
                        Color(0xFF0B0C0E),
                        Color(0xFF181B22),
                        Color(0xFF090A0D)
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // Outer glossy lead-in lip
            drawCircle(
                color = Color(0x55475569),
                radius = leadInRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 2. Music Track Bands (4 recorded song sections with dead-wax track transition gaps)
            // Real vinyl LPs have 3 to 5 visible bands where songs are cut
            val trackBands = listOf(
                Pair(outerRadius * 0.95f, outerRadius * 0.83f), // Track 1
                Pair(outerRadius * 0.82f, outerRadius * 0.70f), // Track 2
                Pair(outerRadius * 0.69f, outerRadius * 0.57f), // Track 3
                Pair(outerRadius * 0.56f, outerRadius * 0.45f)  // Track 4
            )

            for ((outerR, innerR) in trackBands) {
                val grooveSteps = 22
                for (g in 0..grooveSteps) {
                    val r = innerR + (outerR - innerR) * (g.toFloat() / grooveSteps)
                    val alpha = if (g % 3 == 0) 0x33 else if (g % 2 == 0) 0x1A else 0x10
                    drawCircle(
                        color = Color(0xFF94A3B8).copy(alpha = alpha / 255f),
                        radius = r,
                        center = center,
                        style = Stroke(width = 0.6.dp.toPx())
                    )
                }

                // Dark smooth gap band between songs
                drawCircle(
                    color = Color(0xFF050608),
                    radius = innerR,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 3. Dead-Wax Run-Out Zone (Lead-out groove area before label)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF161A22), Color(0xFF0C0E12)),
                    center = center,
                    radius = deadWaxRadius
                ),
                radius = deadWaxRadius,
                center = center
            )

            // Spiral lead-out groove tracing into the label
            drawCircle(
                color = Color(0x4464748B),
                radius = deadWaxRadius * 0.92f,
                center = center,
                style = Stroke(width = 0.75.dp.toPx())
            )
            drawCircle(
                color = Color(0x33475569),
                radius = deadWaxRadius * 0.85f,
                center = center,
                style = Stroke(width = 0.75.dp.toPx())
            )
        }

        // Fixed Anisotropic Specular Light Cones ("Butterfly" Light Sheen)
        // Light striking radial microgrooves creates two opposing triangular reflection cones
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f
            val labelRadius = outerRadius * 0.35f

            // Butterfly reflection cones at 40° and 220°
            val butterflyPath = Path().apply {
                // Top-right cone
                moveTo(
                    center.x + (labelRadius * cos(Math.toRadians(35.0))).toFloat(),
                    center.y + (labelRadius * sin(Math.toRadians(35.0))).toFloat()
                )
                lineTo(
                    center.x + (outerRadius * cos(Math.toRadians(25.0))).toFloat(),
                    center.y + (outerRadius * sin(Math.toRadians(25.0))).toFloat()
                )
                lineTo(
                    center.x + (outerRadius * cos(Math.toRadians(65.0))).toFloat(),
                    center.y + (outerRadius * sin(Math.toRadians(65.0))).toFloat()
                )
                lineTo(
                    center.x + (labelRadius * cos(Math.toRadians(55.0))).toFloat(),
                    center.y + (labelRadius * sin(Math.toRadians(55.0))).toFloat()
                )
                close()

                // Bottom-left opposing cone
                moveTo(
                    center.x + (labelRadius * cos(Math.toRadians(215.0))).toFloat(),
                    center.y + (labelRadius * sin(Math.toRadians(215.0))).toFloat()
                )
                lineTo(
                    center.x + (outerRadius * cos(Math.toRadians(205.0))).toFloat(),
                    center.y + (outerRadius * sin(Math.toRadians(205.0))).toFloat()
                )
                lineTo(
                    center.x + (outerRadius * cos(Math.toRadians(245.0))).toFloat(),
                    center.y + (outerRadius * sin(Math.toRadians(245.0))).toFloat()
                )
                lineTo(
                    center.x + (labelRadius * cos(Math.toRadians(235.0))).toFloat(),
                    center.y + (labelRadius * sin(Math.toRadians(235.0))).toFloat()
                )
                close()
            }

            drawPath(
                path = butterflyPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x35E2E8F0),
                        Color(0x18CBD5E1),
                        Color(0x00000000)
                    ),
                    center = center,
                    radius = outerRadius
                )
            )

            // Subtle rainbow edge fringing along the sheen
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        Color(0x15FF0055),
                        Color(0x1500FFCC),
                        Color.Transparent,
                        Color(0x15FFCC00),
                        Color(0x150099FF),
                        Color.Transparent
                    ),
                    center = center
                ),
                radius = outerRadius * 0.95f,
                center = center
            )
        }

        // Center Vintage Paper Record Label
        val labelDiameter = vinylSize * 0.35f
        Box(
            modifier = Modifier
                .size(labelDiameter)
                .rotate(rotation)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF991B1B), Color(0xFF7F1D1D), Color(0xFF450A0A))
                    )
                )
                .border(2.dp, Color(0xFFFBBF24), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Label artwork / Circular Album Art Frame
            val artSize = labelDiameter * 0.58f
            Box(
                modifier = Modifier
                    .size(artSize)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFFFDE68A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!song?.albumArtUri.isNullOrBlank()) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RETRO",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFBBF24),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Authentic Vintage Record Label Typography
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "33 ⅓ RPM • STEREO",
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFEF3C7),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "SIDE 1 • HI-FI",
                    fontSize = 5.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFDE68A),
                    fontFamily = FontFamily.Monospace
                )
            }

            // Solid Polished Brass Center Spindle Pin & Hole
            Box(
                modifier = Modifier
                    .size(labelDiameter * 0.12f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFDE68A), Color(0xFFD97706), Color(0xFF0F172A))
                        )
                    )
                    .border(1.dp, Color(0xFFB45309), CircleShape)
            )
        }
    }
}

/**
 * Phonograph S-shaped Tonearm with counterweight, headshell, and stylus needle.
 * Dynamically tracks inward across the record grooves from outer rim to inner label
 * based on playback progress.
 */
@Composable
private fun PhonographTonearm(
    platterSize: Dp,
    vinylSize: Dp,
    isPlaying: Boolean,
    playbackProgress: Float
) {
    // Dynamic tracking angle:
    // When paused, stays parked at rest angle (~ -18°).
    // When playing, tracks smoothly from lead-in groove (~ -8°) to lead-out groove (~ 16°).
    val targetAngle by animateFloatAsState(
        targetValue = if (isPlaying) {
            -8f + (24f * playbackProgress.coerceIn(0f, 1f))
        } else {
            -20f // Parked off-record
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "tonearm_angle"
    )

    Box(
        modifier = Modifier
            .size(platterSize)
            .padding(top = 4.dp, end = 4.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Canvas(
            modifier = Modifier
                .width(platterSize * 0.52f)
                .height(platterSize * 0.86f)
        ) {
            val pivotX = size.width - 24.dp.toPx()
            val pivotY = 24.dp.toPx()

            // 1. Chrome Turntable Pivot Assembly (Gimbal base)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE2E8F0), Color(0xFF64748B), Color(0xFF1E293B)),
                    center = Offset(pivotX, pivotY),
                    radius = 16.dp.toPx()
                ),
                radius = 16.dp.toPx(),
                center = Offset(pivotX, pivotY)
            )
            drawCircle(
                color = Color(0xFF94A3B8),
                radius = 8.dp.toPx(),
                center = Offset(pivotX, pivotY)
            )

            // 2. Counterweight Cylinder (Behind pivot)
            val weightRad = Math.toRadians((targetAngle - 180.0))
            val weightDist = 18.dp.toPx()
            val weightX = pivotX + (weightDist * cos(weightRad)).toFloat()
            val weightY = pivotY + (weightDist * sin(weightRad)).toFloat()

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFCBD5E1), Color(0xFF475569), Color(0xFF0F172A)),
                    center = Offset(weightX, weightY),
                    radius = 9.dp.toPx()
                ),
                radius = 9.dp.toPx(),
                center = Offset(weightX, weightY)
            )
            // Weight knurled ring
            drawCircle(
                color = Color(0xFF000000),
                radius = 5.dp.toPx(),
                center = Offset(weightX, weightY)
            )

            // 3. S-Shaped Brushed Chrome Aluminum Tonearm Wand
            val angleRad = Math.toRadians((targetAngle + 65.0))
            val armLength = size.height * 0.72f

            val midX = pivotX - (armLength * 0.45f * sin(angleRad)).toFloat()
            val midY = pivotY + (armLength * 0.45f * cos(angleRad)).toFloat()

            val needleX = pivotX - (armLength * sin(angleRad)).toFloat()
            val needleY = pivotY + (armLength * cos(angleRad)).toFloat()

            val armPath = Path().apply {
                moveTo(pivotX, pivotY)
                cubicTo(
                    pivotX - 10.dp.toPx(), pivotY + armLength * 0.25f,
                    midX + 15.dp.toPx(), midY,
                    needleX, needleY
                )
            }

            // Drop shadow under arm
            drawPath(
                path = armPath,
                color = Color(0x66000000),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Chrome arm tube
            drawPath(
                path = armPath,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFF1F5F9), Color(0xFFCBD5E1), Color(0xFF64748B))
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // 4. Angled Headshell & Cartridge Body
            val headshellLength = 16.dp.toPx()
            val headshellAngleRad = angleRad + Math.toRadians(18.0)
            val headshellTipX = needleX - (headshellLength * sin(headshellAngleRad)).toFloat()
            val headshellTipY = needleY + (headshellLength * cos(headshellAngleRad)).toFloat()

            drawLine(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF334155), Color(0xFF0F172A))
                ),
                start = Offset(needleX, needleY),
                end = Offset(headshellTipX, headshellTipY),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Square
            )

            // Cartridge body (Audio-Technica style red accent)
            drawCircle(
                color = if (isPlaying) Color(0xFFEF4444) else Color(0xFF991B1B),
                radius = 2.5.dp.toPx(),
                center = Offset(
                    needleX - (headshellLength * 0.5f * sin(headshellAngleRad)).toFloat(),
                    needleY + (headshellLength * 0.5f * cos(headshellAngleRad)).toFloat()
                )
            )

            // Glowing tiny diamond stylus needle tip
            drawCircle(
                color = if (isPlaying) Color(0xFFFEF08A) else Color(0xFFE2E8F0),
                radius = 1.5.dp.toPx(),
                center = Offset(headshellTipX, headshellTipY)
            )
        }
    }
}
