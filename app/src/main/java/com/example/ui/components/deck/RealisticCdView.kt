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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
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
import com.example.audio.RetroSoundEffects
import kotlin.math.cos
import kotlin.math.sin

/**
 * Authentic CD Editions directly referencing user provided images:
 * - SOUNDS_OF_THE_90S: Reference to Cds Mix 95.jpeg (Orange & Silver Silk-Screen Disc, "SOUNDS OF THE '90s", 1992, green jewel case booklet)
 * - BURNED_CDR_SHARPIE: Reference to CDs.webp (Holographic Rainbow CD-R with Sharpie marker handwriting, doodles, CD-R 80m 700MB)
 * - ALBUM_ARTWORK_DISC: Custom album cover silk-screen printed CD
 */
enum class CdEdition(val displayName: String) {
    SOUNDS_OF_THE_90S("Sounds of the '90s (1992 Orange Disc)"),
    BURNED_CDR_SHARPIE("Burned CD-R (Handwritten Sharpie Mix)"),
    ALBUM_ARTWORK_DISC("Album Artwork Disc")
}

/**
 * Ultra-realistic, high-resolution Compact Disc (CD) and Jewel Case visualization.
 */
@Composable
fun RealisticCdView(
    song: Song?,
    rotation: Float,
    isPlaying: Boolean,
    deckHeight: Dp = 230.dp,
    modifier: Modifier = Modifier
) {
    var edition by remember { mutableStateOf(CdEdition.SOUNDS_OF_THE_90S) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val caseHeight = totalHeight.coerceAtMost(totalWidth * 0.88f)
        val caseWidth = (caseHeight * 1.14f).coerceAtMost(totalWidth - 8.dp)
        val discDiameter = caseHeight * 0.88f

        // Polycarbonate Jewel Case Outer Chassis
        Box(
            modifier = Modifier
                .size(width = caseWidth, height = caseHeight)
                .shadow(10.dp, RoundedCornerShape(5.dp))
                .clip(RoundedCornerShape(5.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF282D36),
                            Color(0xFF191C22),
                            Color(0xFF101216)
                        )
                    )
                )
                .border(1.5.dp, Color(0x6694A3B8), RoundedCornerShape(5.dp))
                .clickable {
                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                    edition = when (edition) {
                        CdEdition.SOUNDS_OF_THE_90S -> CdEdition.BURNED_CDR_SHARPIE
                        CdEdition.BURNED_CDR_SHARPIE -> CdEdition.ALBUM_ARTWORK_DISC
                        CdEdition.ALBUM_ARTWORK_DISC -> CdEdition.SOUNDS_OF_THE_90S
                    }
                }
        ) {
            // Jewel Case Interior & Contents
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. LEFT: Black Ribbed Plastic Jewel Case Spine
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp))
                        .background(Color(0xFF15181E))
                        .border(0.75.dp, Color(0x44475569), RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val ribSpacing = 3.5.dp.toPx()
                        var y = ribSpacing
                        while (y < size.height - ribSpacing) {
                            drawLine(
                                color = Color(0x55FFFFFF),
                                start = Offset(2.dp.toPx(), y),
                                end = Offset(size.width - 2.dp.toPx(), y),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = Color(0x88000000),
                                start = Offset(2.dp.toPx(), y + 1.dp.toPx()),
                                end = Offset(size.width - 2.dp.toPx(), y + 1.dp.toPx()),
                                strokeWidth = 1.dp.toPx()
                            )
                            y += ribSpacing
                        }
                    }
                }

                Spacer(modifier = Modifier.width(3.dp))

                // 2. MIDDLE-LEFT: Album Cover / Vintage Booklet Insert inside the clear tray
                val bookletWidth = (caseWidth - 28.dp) * 0.44f
                Box(
                    modifier = Modifier
                        .width(bookletWidth)
                        .fillMaxHeight(0.96f)
                        .shadow(4.dp, RoundedCornerShape(2.dp))
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1E2430))
                        .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(2.dp))
                ) {
                    when (edition) {
                        CdEdition.SOUNDS_OF_THE_90S -> {
                            // Reference Cds Mix 95.jpeg: Retro 1992 green geometric graphic booklet
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF065F46))
                                        )
                                    )
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, Color(0xFFFDE047), RoundedCornerShape(2.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "1992 EDITION",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFDE047),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "SOUNDS OF\nTHE '90s",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 13.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = song?.title ?: "CLASSIC TRACKS",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6EE7B7),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "[Tap: Switch Disc]",
                                        fontSize = 6.5.sp,
                                        color = Color(0xAAFFFFFF),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        CdEdition.BURNED_CDR_SHARPIE -> {
                            // Reference CDs.webp: Folded lined notebook paper with handwritten notes
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFF8FAFC))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val lineSpacing = 12.dp.toPx()
                                    var y = lineSpacing
                                    while (y < size.height) {
                                        drawLine(
                                            color = Color(0xFFBAE6FD),
                                            start = Offset(0f, y),
                                            end = Offset(size.width, y),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                        y += lineSpacing
                                    }
                                    // Red margin line on left
                                    drawLine(
                                        color = Color(0xFFFCA5A5),
                                        start = Offset(10.dp.toPx(), 0f),
                                        end = Offset(10.dp.toPx(), size.height),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    modifier = Modifier.fillMaxSize().padding(start = 12.dp, top = 4.dp)
                                ) {
                                    Text(
                                        text = "SUMMER 09 ♥",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF1D4ED8),
                                        fontFamily = FontFamily.Cursive
                                    )
                                    Text(
                                        text = "July Jams Mix",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        fontFamily = FontFamily.SansSerif
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "1. ${song?.title ?: "My Fav Track"}",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF334155),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "2. Roadtrip Anthem",
                                        fontSize = 8.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "3. Late Night Drive",
                                        fontSize = 8.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        CdEdition.ALBUM_ARTWORK_DISC -> {
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
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                            )
                                        )
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "COMPACT DISC",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8),
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = song?.title ?: "AUDIO TRACK",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = song?.artist ?: "STEREO",
                                            fontSize = 9.sp,
                                            color = Color(0xFF38BDF8),
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Clear Plastic Booklet Holder Tabs
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val tabW = 12.dp.toPx()
                        val tabH = 4.dp.toPx()
                        drawRoundRect(
                            color = Color(0x77FFFFFF),
                            topLeft = Offset(size.width * 0.35f, 0f),
                            size = Size(tabW, tabH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                        )
                        drawRoundRect(
                            color = Color(0x77FFFFFF),
                            topLeft = Offset(size.width * 0.35f, size.height - tabH),
                            size = Size(tabW, tabH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                        )
                    }

                    // Paper booklet surface subtle sheen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0x22FFFFFF), Color.Transparent, Color(0x11000000))
                                )
                            )
                    )
                }

                // 3. RIGHT: The High-Resolution Compact Disc (Orange Silk-screen or Burned CD-R Sharpie)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    CdDisc(
                        song = song,
                        edition = edition,
                        discDiameter = discDiameter,
                        rotation = if (isPlaying) rotation else 0f
                    )
                }
            }

            // High-gloss diagonal acrylic glass reflection over the jewel case lid
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width * 0.65f, 0f)
                    lineTo(size.width * 0.25f, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0x30FFFFFF),
                            Color(0x10FFFFFF),
                            Color(0x00FFFFFF)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.5f, size.height * 0.5f)
                    )
                )

                drawLine(
                    color = Color(0x88FFFFFF),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

/**
 * Photorealistic Compact Disc rendering with authentic rainbow diffraction grating,
 * concentric laser tracks, and either 90s Orange Silk-Screen (Cds Mix 95.jpeg) or Burned CD-R Sharpie (CDs.webp).
 */
@Composable
private fun CdDisc(
    song: Song?,
    edition: CdEdition,
    discDiameter: Dp,
    rotation: Float
) {
    Box(
        modifier = Modifier
            .size(discDiameter)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Rotating layer: Aluminum substrate + Rainbow diffraction grating + concentric micro-tracks
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f
            val dataOuterR = outerRadius * 0.96f
            val dataInnerR = outerRadius * 0.40f
            val clampingR = outerRadius * 0.24f
            val holeR = outerRadius * 0.12f

            // 1. Polished Aluminum Mirror Base Substrate
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE2E8F0),
                        Color(0xFFCBD5E1),
                        Color(0xFF94A3B8),
                        Color(0xFFE2E8F0),
                        Color(0xFF64748B)
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // 2. Realistic Iridescent Rainbow Diffraction Spectral Sheen (Sweep Gradients)
            val spectralColors = listOf(
                Color(0x70FF2A6D), // Magenta
                Color(0x7505D9E8), // Cyan
                Color(0x7000FF9F), // Neon Green
                Color(0x75FFE600), // Yellow
                Color(0x70FF6C00), // Orange
                Color(0x759D00FF), // Violet
                Color(0x7005D9E8), // Cyan
                Color(0x75FF2A6D)  // Magenta
            )

            drawCircle(
                brush = Brush.sweepGradient(
                    colors = spectralColors,
                    center = center
                ),
                radius = dataOuterR,
                center = center
            )

            drawCircle(
                brush = Brush.sweepGradient(
                    colors = spectralColors.reversed(),
                    center = center
                ),
                radius = dataOuterR * 0.85f,
                center = center
            )

            // 3. Dense Laser Micro-tracks (concentric sound grooves)
            val stepCount = 28
            for (i in 0..stepCount) {
                val r = dataInnerR + (dataOuterR - dataInnerR) * (i.toFloat() / stepCount)
                drawCircle(
                    color = if (i % 2 == 0) Color(0x33FFFFFF) else Color(0x22000000),
                    radius = r,
                    center = center,
                    style = Stroke(width = 0.75.dp.toPx())
                )
            }

            // If burned CD-R edition (CDs.webp), draw authentic darker laser burned track zone!
            if (edition == CdEdition.BURNED_CDR_SHARPIE) {
                drawCircle(
                    color = Color(0x220284C7),
                    radius = dataOuterR * 0.88f,
                    center = center,
                    style = Stroke(width = (dataOuterR * 0.40f))
                )
            }

            // 4. Mirror Lead-in / Matrix Band
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8), Color(0xFFE2E8F0)),
                    center = center,
                    radius = dataInnerR
                ),
                radius = dataInnerR,
                center = center
            )

            drawCircle(
                color = Color(0x44FFFFFF),
                radius = dataInnerR * 0.92f,
                center = center,
                style = Stroke(width = 0.5.dp.toPx())
            )

            // 5. Transparent Polycarbonate Clamping Area
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x99A0AEC0),
                        Color(0x66CBD5E1),
                        Color(0x884A5568)
                    ),
                    center = center,
                    radius = clampingR
                ),
                radius = clampingR,
                center = center
            )

            drawCircle(
                color = Color(0xAAFFFFFF),
                radius = clampingR * 0.88f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 3 Molded injection gate dots inside clamping ring
            for (dotAngle in listOf(0.0, 120.0, 240.0)) {
                val rad = Math.toRadians(dotAngle)
                val dotDist = clampingR * 0.65f
                val dotX = center.x + (dotDist * cos(rad)).toFloat()
                val dotY = center.y + (dotDist * sin(rad)).toFloat()
                drawCircle(
                    color = Color(0xAAFFFFFF),
                    radius = 1.5.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }

            // 6. Center Spindle Hole
            drawCircle(
                color = Color(0xFF0A0D12),
                radius = holeR,
                center = center
            )
            drawCircle(
                color = Color(0x99475569),
                radius = holeR,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        // Center Silk-Screen Disc Label OR Sharpie Handwritten CD-R Disc Label
        when (edition) {
            CdEdition.SOUNDS_OF_THE_90S -> {
                // Reference Cds Mix 95.jpeg: Vibrant Orange & Silver Silk-Screen CD Disc
                val labelDiameter = discDiameter * 0.65f
                Box(
                    modifier = Modifier
                        .size(labelDiameter)
                        .rotate(rotation)
                        .clip(CircleShape)
                        .border(1.2.dp, Color(0xFFEA580C), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Two-tone Orange & Silver radial artwork
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFEA580C), // Vibrant 90s Orange from Cds Mix 95.jpeg
                                        Color(0xFFC2410C),
                                        Color(0xFF9A3412)
                                    )
                                )
                            )
                    )

                    // Track Number Ring around perimeter (1 to 16)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val r = size.width / 2f - 4.dp.toPx()
                        drawCircle(
                            color = Color(0x66FFFFFF),
                            radius = r,
                            center = c,
                            style = Stroke(width = 0.75.dp.toPx())
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SOUNDS OF THE '90s",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontFamily = FontFamily.SansSerif,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "1992 • DIGITAL STEREO",
                                fontSize = 5.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE047),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Center Spindle Hole cutout
                        Box(
                            modifier = Modifier
                                .size(discDiameter * 0.12f)
                                .clip(CircleShape)
                                .background(Color(0xFF0A0D12))
                                .border(1.dp, Color(0xFF94A3B8), CircleShape)
                        )

                        Text(
                            text = song?.title ?: "COMPACT DISC AUDIO",
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            CdEdition.BURNED_CDR_SHARPIE -> {
                // Reference CDs.webp: Holographic Burned CD-R with Sharpie Marker handwriting!
                val labelDiameter = discDiameter * 0.72f
                Box(
                    modifier = Modifier
                        .size(labelDiameter)
                        .rotate(rotation),
                    contentAlignment = Alignment.Center
                ) {
                    // Handwritten Sharpie text in authentic royal blue permanent marker ink
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        // Sharpie marker handwritten song title & doodles
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = song?.title ?: "SUMMER 09 JAMS",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1D4ED8), // Sharpie Blue Ink
                                fontFamily = FontFamily.Cursive,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "~ July Jams ♥ ~",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                fontFamily = FontFamily.Cursive
                            )
                        }

                        // Center Spindle Hole
                        Box(
                            modifier = Modifier
                                .size(discDiameter * 0.12f)
                                .clip(CircleShape)
                                .background(Color(0xFF0A0D12))
                                .border(1.dp, Color(0xFF94A3B8), CircleShape)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "CD-R 80min 700MB",
                                fontSize = 5.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xAA475569),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "★ BEST MIX EVER ★",
                                fontSize = 6.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFDC2626), // Sharpie Red Ink
                                fontFamily = FontFamily.Cursive
                            )
                        }
                    }
                }
            }

            CdEdition.ALBUM_ARTWORK_DISC -> {
                val labelDiameter = discDiameter * 0.46f
                Box(
                    modifier = Modifier
                        .size(labelDiameter)
                        .rotate(rotation)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFF64748B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!song?.albumArtUri.isNullOrBlank()) {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x33000000))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF0284C7), Color(0xFF0369A1), Color(0xFF075985))
                                    )
                                )
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "COMPACT DISC",
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "DIGITAL AUDIO",
                            fontSize = 5.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2E8F0),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(discDiameter * 0.12f)
                            .clip(CircleShape)
                            .background(Color(0xFF0A0D12))
                            .border(1.dp, Color(0xFF64748B), CircleShape)
                    )
                }
            }
        }

        // Fixed Anisotropic Specular Light Cones (Overhead Studio Lighting)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f

            val conePath = Path().apply {
                moveTo(center.x, center.y)
                val angle1 = Math.toRadians(125.0)
                val angle2 = Math.toRadians(155.0)
                lineTo(
                    center.x + (outerRadius * cos(angle1)).toFloat(),
                    center.y + (outerRadius * sin(angle1)).toFloat()
                )
                lineTo(
                    center.x + (outerRadius * cos(angle2)).toFloat(),
                    center.y + (outerRadius * sin(angle2)).toFloat()
                )
                close()

                moveTo(center.x, center.y)
                val angle3 = Math.toRadians(305.0)
                val angle4 = Math.toRadians(335.0)
                lineTo(
                    center.x + (outerRadius * cos(angle3)).toFloat(),
                    center.y + (outerRadius * sin(angle3)).toFloat()
                )
                lineTo(
                    center.x + (outerRadius * cos(angle4)).toFloat(),
                    center.y + (outerRadius * sin(angle4)).toFloat()
                )
                close()
            }

            drawPath(
                path = conePath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x50FFFFFF),
                        Color(0x25FFFFFF),
                        Color(0x00FFFFFF)
                    ),
                    center = center,
                    radius = outerRadius
                )
            )

            drawCircle(
                color = Color(0x66FFFFFF),
                radius = outerRadius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
