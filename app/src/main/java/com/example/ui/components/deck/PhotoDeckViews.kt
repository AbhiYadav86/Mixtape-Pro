package com.example.ui.components.deck

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.model.Song

/*
 * Photo-style deck views: a 90s desk photo of a cassette, a vinyl record and a burned CD.
 * While music plays, each photo drifts slowly like a camera moving over the desk;
 * when paused it holds still.
 */

@Composable
private fun DriftingPhoto(@DrawableRes res: Int, isPlaying: Boolean, modifier: Modifier, label: String) {
    val drift = rememberInfiniteTransition(label = label)
    val t by drift.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "${label}T"
    )
    val k = if (isPlaying) t else 0.5f
    Image(
        painter = painterResource(res),
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

/** "Rave '95" white-label record half out of its dance sleeve. */
@Composable
fun PhotoVinylView(song: Song?, isPlaying: Boolean, modifier: Modifier = Modifier) =
    DriftingPhoto(R.drawable.deck_vinyl_photo, isPlaying, modifier, "vinylDrift")

/** Burned "Rock Mix '98" CD-R next to its case with a handwritten tracklist. */
@Composable
fun PhotoCdView(song: Song?, isPlaying: Boolean, modifier: Modifier = Modifier) =
    DriftingPhoto(R.drawable.deck_cd_photo, isPlaying, modifier, "cdDrift")

/** Close-up of the mixtape cassette. */
@Composable
fun PhotoTapeView(isPlaying: Boolean, modifier: Modifier = Modifier) =
    DriftingPhoto(R.drawable.deck_tape_closeup, isPlaying, modifier, "tapeDrift")
