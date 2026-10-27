package com.example.ui.components.deck

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Song
import kotlinx.coroutines.isActive

/*
 * Photo-style deck views (CD, vinyl, tape), in the same look as the album-art photos.
 * Each is built from layers: a lit desk background, the media (which spins while music plays)
 * and a fixed light reflection on top, so the shine stays put while the disc turns.
 */

/** Rotation in degrees that advances every frame while [isPlaying] and holds when paused. */
@Composable
private fun spinAngle(isPlaying: Boolean, secondsPerTurn: Float): Float {
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying, secondsPerTurn) {
        if (isPlaying) {
            var last = 0L
            while (isActive) {
                withFrameNanos { now ->
                    if (last != 0L) angle = (angle + (now - last) / 1_000_000_000f * 360f / secondsPerTurn) % 360f
                    last = now
                }
            }
        }
    }
    return angle
}

@Composable
private fun DeckBackground() {
    Image(
        painter = painterResource(R.drawable.deck_bg),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

/** Turntable seen from above: the record turns at 33⅓ rpm; the song's cover becomes the label. */
@Composable
fun PhotoVinylView(song: Song?, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val angle = spinAngle(isPlaying, 1.8f)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        DeckBackground()
        Box(modifier = Modifier.fillMaxHeight(0.94f).aspectRatio(1f), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.deck_vinyl_platter), null, Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = angle }, contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.deck_vinyl_disc), null, Modifier.fillMaxSize())
                val art = song?.albumArtUri?.takeIf { it.isNotBlank() && !it.contains("unsplash") }
                if (art != null) {
                    // label = 32.7 % of the square
                    AsyncImage(
                        model = art,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth(0.30f).aspectRatio(1f).clip(CircleShape)
                    )
                }
            }
            Image(painterResource(R.drawable.deck_vinyl_sheen), null, Modifier.fillMaxSize())
            Image(painterResource(R.drawable.deck_vinyl_tonearm), null, Modifier.fillMaxSize())
        }
    }
}

/** Compact disc, data side up, with the rainbow sheen a CD throws under light. */
@Composable
fun PhotoCdView(song: Song?, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val angle = spinAngle(isPlaying, 2.4f)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        DeckBackground()
        Box(modifier = Modifier.fillMaxHeight(0.92f).aspectRatio(1f), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.deck_cd_shadow), null, Modifier.fillMaxSize())
            Image(
                painterResource(R.drawable.deck_cd_disc), null,
                Modifier.fillMaxSize().graphicsLayer { rotationZ = angle }
            )
            Image(painterResource(R.drawable.deck_cd_sheen), null, Modifier.fillMaxSize())
        }
    }
}

/** Close-up of the mixtape cassette; drifts slowly while playing, like a camera on the deck. */
@Composable
fun PhotoTapeView(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val drift = rememberInfiniteTransition(label = "tapeDrift")
    val t by drift.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "tapeT"
    )
    val k = if (isPlaying) t else 0.5f
    Image(
        painter = painterResource(R.drawable.deck_tape_closeup),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val s = 1.06f + 0.05f * k
                scaleX = s; scaleY = s
                translationX = (k - 0.5f) * size.width * 0.05f
            }
    )
}
