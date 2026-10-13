package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.Song
import kotlin.math.abs

/** True when the URI points at real artwork (not missing, not an old stock-photo placeholder). */
fun isRealArtworkUri(uri: String?): Boolean =
    !uri.isNullOrBlank() && !uri.contains("images.unsplash.com", ignoreCase = true)

private val thumbPalettes = listOf(
    Color(0xFFFF007F) to Color(0xFF7928CA),
    Color(0xFF00F5D4) to Color(0xFF7B2CBF),
    Color(0xFFFFE600) to Color(0xFFFF0055),
    Color(0xFFCCFF00) to Color(0xFF1E293B),
    Color(0xFF38BDF8) to Color(0xFF818CF8),
    Color(0xFFFF70A6) to Color(0xFFFF9770),
    Color(0xFFF59E0B) to Color(0xFF1E1B4B)
)

/**
 * Square album-art thumbnail for lists. Shows the real cover when available, otherwise a
 * colourful per-artist gradient tile (stable for every song by the same artist).
 * When [isPlayingNow] is true, a translucent equalizer badge is drawn on top.
 */
@Composable
fun SongArtThumbnail(
    song: Song,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isPlayingNow: Boolean = false,
    accent: Color = Color(0xFF00FF66)
) {
    val shape = RoundedCornerShape(8.dp)
    var failed by remember(song.id, song.albumArtUri) { mutableStateOf(false) }
    val hasArt = isRealArtworkUri(song.albumArtUri) && !failed

    Box(
        modifier = modifier
            .size(size)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        if (hasArt) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = "${song.title} album art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = { failed = true }
            )
        } else {
            val (c1, c2) = thumbPalettes[abs(song.artist.lowercase().hashCode()) % thumbPalettes.size]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(c1.copy(alpha = 0.85f), c2))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(size * 0.45f)
                )
            }
        }

        if (isPlayingNow) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Now playing",
                    tint = accent,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }
    }
}
