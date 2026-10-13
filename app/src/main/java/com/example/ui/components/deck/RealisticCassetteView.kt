package com.example.ui.components.deck

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.audio.RetroSoundEffects
import kotlin.math.cos
import kotlin.math.sin

/**
 * Authentic Cassette Editions directly referencing user provided images:
 * - CASSETTE_90_GOLD: Reference to Cassette-1.jpeg (Amber/Yellow label, "cassette 90", "Made in Germany", window gauge "100 50 0")
 * - GREATEST_HITS_CYAN: Reference to Cassette-2.jpeg (Turquoise micro-grid, embossed tape "90s Music GREATEST HITS", "SIDE A")
 * - CHROME_TYPE_II: Authentic 90s studio CrO2 high-bias cassette with gold foil
 */
enum class CassetteEdition(val displayName: String) {
    CASSETTE_90_GOLD("Cassette 90 (Made in Germany)"),
    GREATEST_HITS_CYAN("90s Greatest Hits (Cyan Grid)"),
    CHROME_TYPE_II("Type II Chrome High-Bias")
}

/**
 * Ultra-realistic, high-resolution Compact Cassette Tape visualization.
 */
@Composable
fun RealisticCassetteView(
    song: Song?,
    rotation: Float,
    isPlaying: Boolean,
    playbackProgress: Float = 0f,
    deckHeight: Dp = 230.dp,
    modifier: Modifier = Modifier
) {
    var edition by remember { mutableStateOf(CassetteEdition.CASSETTE_90_GOLD) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        // Calculate authentic cassette 1.58:1 aspect ratio
        val shellHeight = totalHeight.coerceAtMost(totalWidth * 0.63f).coerceIn(140.dp, 240.dp)
        val shellWidth = (shellHeight * 1.56f).coerceAtMost(totalWidth - 8.dp)

        // Main Polycarbonate Cassette Shell
        Box(
            modifier = Modifier
                .size(width = shellWidth, height = shellHeight)
                .shadow(10.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF262B34),
                            Color(0xFF181B22),
                            Color(0xFF101217),
                            Color(0xFF1E222A)
                        )
                    )
                )
                .border(2.dp, Color(0xFF475569), RoundedCornerShape(8.dp))
                .clickable {
                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                    edition = when (edition) {
                        CassetteEdition.CASSETTE_90_GOLD -> CassetteEdition.GREATEST_HITS_CYAN
                        CassetteEdition.GREATEST_HITS_CYAN -> CassetteEdition.CHROME_TYPE_II
                        CassetteEdition.CHROME_TYPE_II -> CassetteEdition.CASSETTE_90_GOLD
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // 1. Cassette Label (Dynamically adapts to selected reference edition)
            CassetteLabel(
                song = song,
                edition = edition,
                shellWidth = shellWidth,
                shellHeight = shellHeight,
                rotation = rotation,
                isPlaying = isPlaying,
                playbackProgress = playbackProgress
            )

            // 2. Shell Structural Details: 5 Phillips-Head Screws, Guide Ribs & Read Head Chamber
            CassetteShellHardwareOverlay(
                shellWidth = shellWidth,
                shellHeight = shellHeight,
                playbackProgress = playbackProgress
            )

            // 3. Clear Acrylic Glass Reflection across the cassette face
            Canvas(modifier = Modifier.fillMaxSize()) {
                val glossPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width * 0.45f, 0f)
                    lineTo(size.width * 0.15f, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = glossPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0x28FFFFFF),
                            Color(0x0AFFFFFF),
                            Color(0x00FFFFFF)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.4f, size.height * 0.5f)
                    )
                )

                // Outer perimeter highlight
                drawRoundRect(
                    color = Color(0x33FFFFFF),
                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                    size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                    cornerRadius = CornerRadius(7.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
    }
}

/**
 * Authentic Vintage Cassette Label adapting to Reference Images:
 * - Cassette 90 Gold (Cassette-1.jpeg)
 * - 90s Greatest Hits Cyan (Cassette-2.jpeg)
 * - Chrome Type II
 */
@Composable
private fun CassetteLabel(
    song: Song?,
    edition: CassetteEdition,
    shellWidth: Dp,
    shellHeight: Dp,
    rotation: Float,
    isPlaying: Boolean,
    playbackProgress: Float
) {
    val labelWidth = shellWidth * 0.90f
    val labelHeight = shellHeight * 0.85f

    val labelBrush = when (edition) {
        CassetteEdition.CASSETTE_90_GOLD -> Brush.verticalGradient(
            listOf(
                Color(0xFFFBBF24), // Amber yellow base from Cassette-1.jpeg
                Color(0xFFF59E0B),
                Color(0xFFEAB308),
                Color(0xFFD97706)
            )
        )
        CassetteEdition.GREATEST_HITS_CYAN -> Brush.verticalGradient(
            listOf(
                Color(0xFF0891B2), // Vibrant retro cyan from Cassette-2.jpeg
                Color(0xFF06B6D4),
                Color(0xFF0891B2)
            )
        )
        CassetteEdition.CHROME_TYPE_II -> Brush.verticalGradient(
            listOf(
                Color(0xFF0F172A),
                Color(0xFF1E293B),
                Color(0xFFF1F5F9),
                Color(0xFFE2E8F0)
            ),
            startY = 0f,
            endY = 70f
        )
    }

    val labelBorder = when (edition) {
        CassetteEdition.CASSETTE_90_GOLD -> Color(0xFF78350F)
        CassetteEdition.GREATEST_HITS_CYAN -> Color(0xFF164E63)
        CassetteEdition.CHROME_TYPE_II -> Color(0xFFD97706)
    }

    Box(
        modifier = Modifier
            .size(width = labelWidth, height = labelHeight)
            .shadow(4.dp, RoundedCornerShape(5.dp))
            .clip(RoundedCornerShape(5.dp))
            .background(labelBrush)
            .border(1.dp, labelBorder, RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Subtle micro-grid pattern for cyan edition (as in Cassette-2.jpeg)
        if (edition == CassetteEdition.GREATEST_HITS_CYAN) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpacing = 8.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = Color(0x22FFFFFF),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 0.5.dp.toPx()
                    )
                    x += gridSpacing
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = Color(0x22FFFFFF),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.5.dp.toPx()
                    )
                    y += gridSpacing
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HEADER BAR: Directly referencing uploaded styles
            when (edition) {
                CassetteEdition.CASSETTE_90_GOLD -> {
                    // Reference Cassette-1.jpeg: "cassette 90" + "Made in Germany"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "cassette 90",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF111827),
                                letterSpacing = (-0.5).sp,
                                fontFamily = FontFamily.SansSerif
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFDC2626), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "EQ 70µs",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Made in Germany",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1F2937),
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "• C-90",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                CassetteEdition.GREATEST_HITS_CYAN -> {
                    // Reference Cassette-2.jpeg: Red "A" badge + "90s Music GREATEST HITS"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE11D48), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = "A",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "HIGH FIDELITY STEREO",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCFFAFE),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Box(
                            modifier = Modifier
                                .border(0.75.dp, Color(0xFFCFFAFE), RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "DOLBY B NR",
                                fontSize = 6.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                CassetteEdition.CHROME_TYPE_II -> {
                    // Type II Chrome high-bias
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFDC2626), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "SIDE A",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = "HIGH BIAS 70µs EQ",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE68A),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "TYPE II (CrO2)",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .border(0.75.dp, Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "DOLBY B•C NR",
                                    fontSize = 6.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // MIDDLE: Transparent Inspection Window with Dynamic Magnetic Tape Pack & Spools
            // Incorporates graduated scale [ 100 • 50 • 0 ] as seen in Cassette-1.jpeg
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(shellHeight * 0.44f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xEE0B0E14))
                    .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Dynamic Magnetic Tape & Spool Animation
                DynamicTapeWindow(
                    rotation = if (isPlaying) rotation else 0f,
                    playbackProgress = playbackProgress
                )

                // Embossed Measurement Scale Printed on the Window [ 100 • 50 • 0 ] from Cassette-1.jpeg
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.40f)
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("100", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color(0xBBFFFFFF), fontFamily = FontFamily.Monospace)
                    Text("•", fontSize = 8.sp, color = Color(0x99FFFFFF))
                    Text("50", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color(0xBBFFFFFF), fontFamily = FontFamily.Monospace)
                    Text("•", fontSize = 8.sp, color = Color(0x99FFFFFF))
                    Text("0", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color(0xBBFFFFFF), fontFamily = FontFamily.Monospace)
                }
            }

            // BOTTOM: Lined Handwritten/Typewriter Title Block
            when (edition) {
                CassetteEdition.CASSETTE_90_GOLD -> {
                    // Cassette 90 clean lined paper title block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(shellHeight * 0.22f)
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(3.dp))
                            .border(0.75.dp, Color(0xFFD97706), RoundedCornerShape(3.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song?.title ?: "STEREO RECORDING",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color(0xFF1C1917),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${song?.artist ?: "SIDE 1"} • [Tap Cassette to Switch Edition]",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF92400E),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "C-90",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }

                CassetteEdition.GREATEST_HITS_CYAN -> {
                    // Reference Cassette-2.jpeg: White embossed label tape strip: "90s Music GREATEST HITS"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(shellHeight * 0.22f)
                            .background(Color(0xFFECFEFF), RoundedCornerShape(3.dp))
                            .border(0.75.dp, Color(0xFF0891B2), RoundedCornerShape(3.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF0E7490), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "90s Music",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = "GREATEST HITS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF164E63),
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                                Text(
                                    text = song?.title ?: "SUMMER MIXTAPE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0891B2),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "SIDE A",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFE11D48)
                            )
                        }
                    }
                }

                CassetteEdition.CHROME_TYPE_II -> {
                    // Type II notebook lined paper
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(shellHeight * 0.22f)
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(3.dp))
                            .border(0.75.dp, Color(0xFFCBD5E1), RoundedCornerShape(3.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song?.title ?: "MIXTAPE 1998",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song?.artist ?: "CHROME STEREO",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "C-90",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Inspection window featuring dynamic magnetic tape packs that spool in real time,
 * 6-toothed drive cogs, and the authentic tape path.
 */
@Composable
private fun DynamicTapeWindow(
    rotation: Float,
    playbackProgress: Float
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth

        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftHubCenter = Offset(size.width * 0.23f, size.height * 0.50f)
            val rightHubCenter = Offset(size.width * 0.77f, size.height * 0.50f)

            val minTapeRadius = size.height * 0.28f
            val maxTapeRadius = size.height * 0.46f

            val clampedProgress = playbackProgress.coerceIn(0f, 1f)
            val leftTapeRadius = minTapeRadius + (maxTapeRadius - minTapeRadius) * (1f - clampedProgress)
            val rightTapeRadius = minTapeRadius + (maxTapeRadius - minTapeRadius) * clampedProgress

            // 1. Tape routing strip connecting the reels
            val tapePath = Path().apply {
                moveTo(leftHubCenter.x, leftHubCenter.y + leftTapeRadius)
                lineTo(rightHubCenter.x, rightHubCenter.y + rightTapeRadius)
            }
            drawPath(
                path = tapePath,
                color = Color(0xFF23120B),
                style = Stroke(width = 3.dp.toPx())
            )

            // 2. Left Reel: Magnetic Tape Pack
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3B1E12),
                        Color(0xFF26130B),
                        Color(0xFF170B06),
                        Color(0xFF2A150C)
                    ),
                    center = leftHubCenter,
                    radius = leftTapeRadius
                ),
                radius = leftTapeRadius,
                center = leftHubCenter
            )
            drawCircle(
                color = Color(0x33452215),
                radius = leftTapeRadius * 0.88f,
                center = leftHubCenter,
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Right Reel: Magnetic Tape Pack
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3B1E12),
                        Color(0xFF26130B),
                        Color(0xFF170B06),
                        Color(0xFF2A150C)
                    ),
                    center = rightHubCenter,
                    radius = rightTapeRadius
                ),
                radius = rightTapeRadius,
                center = rightHubCenter
            )
            drawCircle(
                color = Color(0x33452215),
                radius = rightTapeRadius * 0.88f,
                center = rightHubCenter,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // 4. Left White Nylon 6-Toothed Drive Hub (Spool)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = w * 0.14f)
        ) {
            CassetteCogSpool(rotation = rotation)
        }

        // 5. Right White Nylon 6-Toothed Drive Hub (Spool)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = w * 0.14f)
        ) {
            CassetteCogSpool(rotation = rotation)
        }
    }
}

/**
 * White injection-molded nylon cassette spool with 6 authentic drive teeth.
 */
@Composable
private fun CassetteCogSpool(rotation: Float) {
    Canvas(
        modifier = Modifier
            .size(34.dp)
            .rotate(rotation)
            .shadow(3.dp, CircleShape)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerR = size.width / 2f
        val innerR = outerR * 0.58f

        // Outer white ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFFCBD5E1)),
                center = center,
                radius = outerR
            ),
            radius = outerR,
            center = center
        )

        // 6 Drive teeth
        for (i in 0 until 6) {
            val angleDeg = i * 60.0
            val rad = Math.toRadians(angleDeg)
            val toothW = 3.dp.toPx()
            val toothL = 4.dp.toPx()

            val toothX = center.x + ((outerR - toothL / 2f) * cos(rad)).toFloat()
            val toothY = center.y + ((outerR - toothL / 2f) * sin(rad)).toFloat()

            drawCircle(
                color = Color(0xFF94A3B8),
                radius = 1.5.dp.toPx(),
                center = Offset(toothX, toothY)
            )
        }

        // Center drive hole
        drawCircle(
            color = Color(0xFF0F1218),
            radius = innerR,
            center = center
        )
        drawCircle(
            color = Color(0xFF475569),
            radius = innerR,
            center = center,
            style = Stroke(1.dp.toPx())
        )
    }
}

/**
 * Overlay rendering the 5 Phillips-head metal screws, guide roller pins,
 * capstan drive holes, and read-head felt pressure pad at the bottom.
 */
@Composable
private fun CassetteShellHardwareOverlay(
    shellWidth: Dp,
    shellHeight: Dp,
    playbackProgress: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Draw 5 Precision Phillips-Head Screws
        val screwRadius = 3.2.dp.toPx()
        val screwLocations = listOf(
            Offset(10.dp.toPx(), 10.dp.toPx()),          // Top-Left
            Offset(w - 10.dp.toPx(), 10.dp.toPx()),      // Top-Right
            Offset(10.dp.toPx(), h - 10.dp.toPx()),      // Bottom-Left
            Offset(w - 10.dp.toPx(), h - 10.dp.toPx()),  // Bottom-Right
            Offset(w / 2f, h - 8.dp.toPx())              // Bottom-Center
        )

        for (screwPos in screwLocations) {
            // Beveled counterbore hole
            drawCircle(
                color = Color(0xFF0F1116),
                radius = screwRadius + 1.2.dp.toPx(),
                center = screwPos
            )
            // Metallic screw body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF475569)),
                    center = screwPos,
                    radius = screwRadius
                ),
                radius = screwRadius,
                center = screwPos
            )
            // Cross (+) slot
            val slotLen = screwRadius * 0.65f
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(screwPos.x - slotLen, screwPos.y),
                end = Offset(screwPos.x + slotLen, screwPos.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(screwPos.x, screwPos.y - slotLen),
                end = Offset(screwPos.x, screwPos.y + slotLen),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 2. Bottom Read-Head Cavity Area
        val capstanLeft = Offset(w * 0.30f, h - 12.dp.toPx())
        val capstanRight = Offset(w * 0.70f, h - 12.dp.toPx())
        val capstanR = 3.5.dp.toPx()

        drawCircle(color = Color(0xFF0F1218), radius = capstanR, center = capstanLeft)
        drawCircle(color = Color(0xFF475569), radius = capstanR, center = capstanLeft, style = Stroke(0.75.dp.toPx()))
        drawCircle(color = Color(0xFF0F1218), radius = capstanR, center = capstanRight)
        drawCircle(color = Color(0xFF475569), radius = capstanR, center = capstanRight, style = Stroke(0.75.dp.toPx()))

        // Center Read Head Opening with Bronze Spring & Felt Pressure Pad
        val feltRectW = 16.dp.toPx()
        val feltRectH = 6.dp.toPx()
        val feltCenter = Offset(w / 2f, h - 13.dp.toPx())

        drawRoundRect(
            color = Color(0xFFB45309),
            topLeft = Offset(feltCenter.x - feltRectW / 2f, feltCenter.y - feltRectH / 2f),
            size = Size(feltRectW, feltRectH),
            cornerRadius = CornerRadius(1.dp.toPx())
        )
        val padW = 8.dp.toPx()
        val padH = 4.dp.toPx()
        drawRoundRect(
            color = Color(0xFFF1F5F9),
            topLeft = Offset(feltCenter.x - padW / 2f, feltCenter.y - padH / 2f),
            size = Size(padW, padH),
            cornerRadius = CornerRadius(1.dp.toPx())
        )

        // Brass Guide Roller Pins
        val rollerLeft = Offset(w * 0.16f, h - 11.dp.toPx())
        val rollerRight = Offset(w * 0.84f, h - 11.dp.toPx())
        val rollerR = 3.dp.toPx()

        drawCircle(color = Color(0xFFD97706), radius = rollerR, center = rollerLeft)
        drawCircle(color = Color(0xFFFDE68A), radius = 1.2.dp.toPx(), center = rollerLeft)
        drawCircle(color = Color(0xFFD97706), radius = rollerR, center = rollerRight)
        drawCircle(color = Color(0xFFFDE68A), radius = 1.2.dp.toPx(), center = rollerRight)
    }
}
