package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioSource
import com.example.data.model.Song
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun GenerativeAlbumArt(
    song: Song?,
    modifier: Modifier = Modifier
) {
    val isRadio = song?.source == AudioSource.RADIO_STREAM
    val title = song?.title ?: (if (isRadio) "LIVE RETRO RADIO" else "RETRO TRACK")
    val artist = song?.artist ?: (if (isRadio) "90s FM Broadcast" else "FLASHBACK '99")
    val genre = song?.genre ?: (if (isRadio) "LIVE STREAM" else "90s ELECTRONIC")
    val hash = remember(title, artist) { abs((title + artist).hashCode()) }

    // Derive deterministic retro theme color palettes based on hash
    val colorPalettes = remember {
        listOf(
            // Synthwave Sunset
            listOf(Color(0xFFFF007F), Color(0xFF7928CA), Color(0xFF00DFD8), Color(0xFFFFBE0B)),
            // Cyberpunk Neon
            listOf(Color(0xFF00F5D4), Color(0xFF7B2CBF), Color(0xFFF72585), Color(0xFF4CC9F0)),
            // 90s Eurodance Rave
            listOf(Color(0xFFFFE600), Color(0xFFFF0055), Color(0xFF0038FF), Color(0xFF00FF66)),
            // Acid House & Grunge
            listOf(Color(0xFFCCFF00), Color(0xFF1E293B), Color(0xFFFF3366), Color(0xFF38BDF8)),
            // Y2K Millennium Ice
            listOf(Color(0xFF38BDF8), Color(0xFF818CF8), Color(0xFFC084FC), Color(0xFF0EA5E9)),
            // Vaporwave Sunset
            listOf(Color(0xFFFF70A6), Color(0xFFFF9770), Color(0xFFFFD670), Color(0xFF70D6FF)),
            // Cassette Gold & Amber
            listOf(Color(0xFFF59E0B), Color(0xFFB45309), Color(0xFF1E1B4B), Color(0xFFFBBF24))
        )
    }

    val palette = colorPalettes[hash % colorPalettes.size]
    val c1 = palette[0]
    val c2 = palette[1]
    val c3 = palette[2]
    val c4 = palette[3]

    val patternType = hash % 4

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0B0D13)),
        contentAlignment = Alignment.Center
    ) {
        // Generative Background Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Base gradient
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(c1.copy(alpha = 0.85f), c2, Color(0xFF090A0F)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )

            when (patternType) {
                0 -> {
                    // Synthwave Grid & Sun
                    val horizon = h * 0.58f
                    // Draw Sun
                    drawCircle(
                        brush = Brush.verticalGradient(
                            colors = listOf(c4, c1),
                            startY = horizon - w * 0.35f,
                            endY = horizon
                        ),
                        radius = w * 0.28f,
                        center = Offset(w * 0.5f, horizon)
                    )

                    // Perspective Grid Lines
                    for (i in 0..10) {
                        val y = horizon + (h - horizon) * (i.toFloat() / 10f) * (i.toFloat() / 10f)
                        drawLine(
                            color = c3.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.5f
                        )
                    }
                    for (i in -5..5) {
                        val xBottom = w * 0.5f + i * (w * 0.15f)
                        drawLine(
                            color = c3.copy(alpha = 0.35f),
                            start = Offset(w * 0.5f, horizon),
                            end = Offset(xBottom, h),
                            strokeWidth = 1.5f
                        )
                    }
                }
                1 -> {
                    // Concentric Waveform / Vinyl Rings
                    for (i in 1..8) {
                        val r = (w * 0.08f) * i
                        drawCircle(
                            color = if (i % 2 == 0) c3.copy(alpha = 0.35f) else c4.copy(alpha = 0.25f),
                            radius = r,
                            center = Offset(w * 0.5f, h * 0.45f),
                            style = Stroke(width = 2.5f)
                        )
                    }
                    // Sound waves
                    val centerY = h * 0.45f
                    for (x in 0..w.toInt() step 6) {
                        val wave = (sin(x * 0.04f + hash) * 20f).toFloat()
                        drawLine(
                            color = c1.copy(alpha = 0.5f),
                            start = Offset(x.toFloat(), centerY - wave),
                            end = Offset(x.toFloat(), centerY + wave),
                            strokeWidth = 3f
                        )
                    }
                }
                2 -> {
                    // Geometric Bauhaus / 90s CD Blocks
                    drawRect(
                        brush = Brush.radialGradient(
                            listOf(c3.copy(alpha = 0.6f), Color.Transparent),
                            center = Offset(w * 0.8f, h * 0.2f),
                            radius = w * 0.6f
                        )
                    )
                    drawCircle(
                        color = c4.copy(alpha = 0.5f),
                        radius = w * 0.22f,
                        center = Offset(w * 0.25f, h * 0.35f)
                    )
                    drawRect(
                        color = c1.copy(alpha = 0.4f),
                        topLeft = Offset(w * 0.4f, h * 0.3f),
                        size = Size(w * 0.45f, h * 0.35f)
                    )
                }
                else -> {
                    // Cyberpunk Matrix diagonal beams
                    for (i in -4..8) {
                        val startX = i * (w * 0.25f)
                        drawLine(
                            brush = Brush.linearGradient(
                                listOf(c1.copy(alpha = 0f), c3.copy(alpha = 0.5f), c4.copy(alpha = 0f))
                            ),
                            start = Offset(startX, 0f),
                            end = Offset(startX + w * 0.6f, h),
                            strokeWidth = 8f
                        )
                    }
                }
            }
        }

        // Overlay Typography & Badges
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Genre Badge & Stereo Audio Mark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xCC000000))
                        .border(1.dp, c3.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = genre.uppercase(),
                        color = c3,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Text(
                    text = if (isRadio) "LIVE ON-AIR FM STREAM" else "COMPACT DISC DIGITAL AUDIO",
                    color = if (isRadio) Color(0xFFFF3333) else Color.White.copy(alpha = 0.7f),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            // Center / Lower Title & Artist Info in High-Legibility Modern Typography
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC080A10))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title.uppercase(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = artist,
                    color = c4,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom Footer Watermark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "STEREO • 44.1kHz",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "FLASHBACK '99",
                    color = c1,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // Gloss sheen reflection layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0x33FFFFFF),
                            Color(0x00FFFFFF),
                            Color(0x00FFFFFF),
                            Color(0x20FFFFFF)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset.Infinite
                    )
                )
        )
    }
}
