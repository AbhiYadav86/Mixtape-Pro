package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.audio.LyricLine
import com.example.data.model.AudioSource
import com.example.data.model.Playlist
import com.example.data.model.RadioStationStatus
import com.example.data.model.Song
import com.example.data.model.StationLiveState
import com.example.ui.components.LyricsLineOverlay
import com.example.ui.screens.PlaylistScreen
import com.example.ui.screens.QuickOpenTrackMenuDialog
import com.example.ui.screens.RadioScreen
import com.example.ui.screens.SkinSelectorDialog
import com.example.ui.skins.RetroSkinDefinitions
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the screens that had layout problems at a large system text size (1.3x, like the
 * phone they were reported on) and saves screenshots to app/build/ui-shots for review.
 * Also proves the Mixtapes screen opens with stale saved cover ids (the crash in v3.0.11).
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class UiScreensTest {
    @get:Rule val compose = createComposeRule()

    private val skin = RetroSkinDefinitions.ALL_SKINS.first { it.name.startsWith("Scroll Wheel") }
    private val dir = "build/ui-shots"

    private fun show(content: @Composable () -> Unit) {
        // Some screens run endless animations (blinking LEDs); step the clock by hand instead
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 1.3f)) { content() }
        }
        compose.mainClock.advanceTimeBy(1_500L)
    }

    private fun song(i: Int, title: String, artist: String, genre: String = "Eurodance") =
        Song(id = "s$i", title = title, artist = artist, genre = genre, durationMs = 218_000L + i * 7_000L, uri = "content://media/external/audio/media/$i")

    private val songs = listOf(
        song(1, "Turn It Up Tonight (1998 Extended Mix)", "DJ Pixel & The Bassline"),
        song(2, "Late Night Neon Cruise (Synthwave)", "Neon Highway Wave", "Synthwave"),
        song(3, "Teenage Garage Riot '95", "The Flannel Generation", "Grunge / Rock"),
        song(4, "Download Complete (Y2K Pop)", "Millennium Pop Queens", "Pop")
    )

    private val playlists = listOf(
        Playlist("p1", "Summer '99 Mixtape", "The best nostalgic eurodance, pop and dance hits", coverRes = 0x7f0800ff),
        Playlist("p2", "Y2K Millennium Party", "High-energy millennium rave, trance, and dance anthems", coverRes = 12345),
        Playlist("p3", "Late Night Neon Cruise", "Chilled 90s synthwave, lofi & atmospheric 808s", coverRes = null),
        Playlist("p4", "Seattle Grunge & Alt '94", "Raw distortion, flannel vibes & alternative rock", coverRes = 0)
    )

    @Test fun mixtapesScreenOpensWithStaleCovers() {
        show {
            PlaylistScreen(
                playlists = playlists, selectedPlaylist = playlists[0], playlistSongs = songs,
                currentSong = songs[0], isPlaying = true, skin = skin,
                onSelectPlaylist = {}, onPlayPlaylist = {}, onCreatePlaylist = { _, _ -> }, onDeletePlaylist = {},
                onPlaySong = {}, onRemoveSongFromPlaylist = { _, _ -> }
            )
        }
        compose.onRoot().captureRoboImage("$dir/mixtapes.png")
    }

    @Test fun skinsGallery() {
        show { SkinSelectorDialog(currentSkin = skin, onSkinSelected = {}, onDismiss = {}) }
        // the dialog is its own window: capture its root directly
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("$dir/skins.png")
    }

    @Test fun trackMenu() {
        show {
            QuickOpenTrackMenuDialog(
                allSongs = songs, allPlaylists = playlists, currentSong = songs[0], isPlaying = true, skin = skin,
                onSongSelected = {}, onPlaylistSelected = {}, onEnrichMetadata = {}, onDismiss = {}
            )
        }
        compose.onAllNodes(isRoot()).onLast().captureRoboImage("$dir/track-menu.png")
    }

    @Test fun radioStations() {
        val stations = listOf(
            Song(id = "r1", title = "1990s Mega", artist = "Retro FM", genre = "90s Pop / Rock", uri = "https://offline-archive.invalid/1", source = AudioSource.RADIO_STREAM),
            Song(id = "r2", title = "Groove Salad Classic Ambient Downtempo", artist = "Internet radio", genre = "Ambient", uri = "https://example.com/s", source = AudioSource.RADIO_STREAM)
        )
        val status = mapOf(
            "r1" to RadioStationStatus("r1", StationLiveState.OFFLINE, 0, 0, 0, "98.7 FM", "OFFLINE ARCHIVE", false),
            "r2" to RadioStationStatus("r2", StationLiveState.LIVE, 42, 96, 1240, "104.3 FM")
        )
        show {
            RadioScreen(
                radioStations = stations, stationStatusMap = status, onlineSearchResults = emptyList(),
                currentSong = null, isPlaying = false, skin = skin,
                onPlaySong = {}, onSearchOnline = {}, onSaveToLibrary = {}
            )
        }
        compose.onRoot().captureRoboImage("$dir/radio.png")
    }

    @Test fun lyricsOneLineAtATime() {
        val lyrics = listOf(
            LyricLine(0L, "Neon lights over the highway"),
            LyricLine(4_000L, "We were young and the night was ours"),
            LyricLine(8_000L, "Rewind the tape, play it again")
        )
        show {
            Box(Modifier.fillMaxWidth().height(170.dp)) {
                LyricsLineOverlay(
                    lyrics = lyrics, currentPositionMs = 5_000L, skin = skin, isFetching = false,
                    onFetchOnline = {}, onFullScreen = {}
                )
            }
        }
        compose.onRoot().captureRoboImage("$dir/lyrics-overlay.png")
    }
}
