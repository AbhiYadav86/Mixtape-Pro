package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DiscDisplayMode
import com.example.data.model.RetroSkin
import com.example.data.model.Song
import com.example.ui.components.deck.RealisticCassetteView
import com.example.ui.components.deck.RealisticCdView
import com.example.ui.components.deck.RealisticVinylView

@Composable
fun AlbumArtDisplay(
    song: Song?,
    isPlaying: Boolean,
    displayMode: DiscDisplayMode,
    skin: RetroSkin,
    deckHeight: Dp = Dp.Unspecified,
    playbackProgress: Float = 0f,
    modifier: Modifier = Modifier,
    topControlHeader: @Composable () -> Unit = {},
    visualizerContent: @Composable () -> Unit = {},
    onArtworkMissing: (Song) -> Unit = {}
) {
    // Rotation animation for spinning disc / spools
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 6000 else 10000000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_spin"
    )

    val heightMod = if (deckHeight != Dp.Unspecified && deckHeight > 0.dp) {
        Modifier.height(deckHeight)
    } else {
        Modifier.fillMaxHeight()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .then(heightMod)
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .shadow(6.dp, RoundedCornerShape(10.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF0F1115), Color(0xFF181B22), Color(0xFF0A0C10))
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .border(2.dp, skin.chassisBorderColor, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        val availableDeckHeight = maxHeight

        // 1. BASE LAYER: Ultra-Realistic Media Deck (CD Jewel Case / Cassette / Vinyl / CRT)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (displayMode) {
                DiscDisplayMode.CD_JEWEL_CASE -> {
                    RealisticCdView(
                        song = song,
                        rotation = rotation,
                        isPlaying = isPlaying,
                        deckHeight = availableDeckHeight
                    )
                }
                DiscDisplayMode.CASSETTE_TAPE -> {
                    RealisticCassetteView(
                        song = song,
                        rotation = rotation,
                        isPlaying = isPlaying,
                        playbackProgress = playbackProgress,
                        deckHeight = availableDeckHeight
                    )
                }
                DiscDisplayMode.VINYL_RECORD -> {
                    RealisticVinylView(
                        song = song,
                        rotation = rotation,
                        isPlaying = isPlaying,
                        playbackProgress = playbackProgress,
                        deckHeight = availableDeckHeight
                    )
                }
                DiscDisplayMode.CRT_MONITOR -> {
                    CrtMonitorView(song = song, onArtworkMissing = onArtworkMissing)
                }
            }
        }

        // 2. OVERLAY LAYER: Live Dancing Equalizer Visualizer over Album Art (Enlarged by 60%)
        val visualizerOverlayH = (availableDeckHeight * 0.662f).coerceIn(77.dp, 192.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(visualizerOverlayH)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0x00000000),
                            Color(0x20000000),
                            Color(0x60000000),
                            Color(0xBB000000),
                            Color(0xEE000000)
                        )
                    )
                )
                .padding(bottom = 6.dp, start = 12.dp, end = 12.dp, top = 4.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            visualizerContent()
        }

        // 3. TOP CONTROLS: Mode Selectors floating cleanly at the top of the deck
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xEE0F1115), Color(0x990F1115), Color.Transparent)
                    )
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            topControlHeader()
        }
    }
}

@Composable
private fun CrtMonitorView(song: Song?, onArtworkMissing: (Song) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
    ) {
        RenderAlbumImage(song = song, onArtworkMissing = onArtworkMissing)

        // CRT Scanline raster effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0x3300FF66), Color(0x00000000), Color(0x3300FF66))
                    )
                )
        )
    }
}

@Composable
private fun RenderAlbumImage(song: Song?, onArtworkMissing: (Song) -> Unit = {}) {
    val rawArtUri = song?.albumArtUri
    val hasValidArtUri = !rawArtUri.isNullOrBlank() && !rawArtUri.contains("unsplash", ignoreCase = true)

    var loadFailed by remember(song?.id, rawArtUri) { mutableStateOf(!hasValidArtUri) }

    // No art, or the stored art can't be loaded: ask for an online lookup
    LaunchedEffect(song?.id, rawArtUri, loadFailed) {
        if (song != null && loadFailed) onArtworkMissing(song)
    }

    if (hasValidArtUri && !loadFailed) {
        AsyncImage(
            model = rawArtUri,
            contentDescription = song?.title ?: "Album Art",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onError = {
                loadFailed = true
            }
        )
    } else if (com.example.data.model.BuiltInArt.songArt(song?.albumArtRes)?.let { it != com.example.R.drawable.img_cd_jewel_case } == true) {
        Image(
            painter = painterResource(id = com.example.data.model.BuiltInArt.songArt(song?.albumArtRes)!!),
            contentDescription = song.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        // Authentic procedural vintage cassette/CD label styling
        GenerativeAlbumArt(
            song = song,
            modifier = Modifier.fillMaxSize()
        )
    }
}
