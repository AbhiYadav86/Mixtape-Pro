package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.LrcParser
import com.example.data.repository.RetroMetadataService
import com.example.data.repository.StreamingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Calls the real online services the app uses and writes a report to build/api-check.txt.
 * Only runs when LIVE_API_CHECK=1 (set by the CI "api-check" job), so normal builds stay offline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LiveApiCheckTest {

    private val report = StringBuilder()
    private fun log(line: String) {
        println(line)
        report.appendLine(line)
    }

    @Test
    fun liveApis() {
        assumeTrue(System.getenv("LIVE_API_CHECK") == "1")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val meta = RetroMetadataService(context)
        val streaming = StreamingRepository(context)

        val songs = listOf(
            Triple("Nickelback", "Burn It to the Ground", "Dark Horse"),
            Triple("Nickelback", "When We Stand Together", "Here and Now"),
            Triple("Chris Isaak", "Wicked Game", "Heart Shaped World"),
            Triple("The Righteous Brothers", "You've Lost That Lovin' Feelin'", ""),
            Triple("Unknown Artist", "Kenny Loggins - Playing With the Boys", ""),
            Triple("RAJIV DIXIT", "Viruddh Bhojan kabhi na", "")
        )
        runBlocking {
            for ((artist, title, album) in songs) {
                val art = meta.fetchAlbumArtwork(artist, title, album, "test_${title.hashCode()}")
                val lyrics = meta.fetchLrclibLyrics(artist, title, album, 0L)
                val lines = lyrics?.let { LrcParser.parse(it, 200_000L).size } ?: 0
                log("ART    [$artist - $title] -> ${art ?: "NONE"}")
                log("LYRICS [$artist - $title] -> ${if (lyrics == null) "NONE" else "$lines lines"}")
            }

            val trending = streaming.audiusTrending("All")
            log("AUDIUS trending -> ${trending.size} tracks; first=${trending.firstOrNull()?.let { it.artist + " - " + it.title + " | " + it.uri }}")
            val search = streaming.audiusSearch("synthwave")
            log("AUDIUS search synthwave -> ${search.size} tracks")

            val items = streaming.archiveSearch(StreamingRepository.ARCHIVE_PRESETS.values.first(), isPreset = true)
            log("ARCHIVE preset -> ${items.size} items; first=${items.firstOrNull()?.identifier}")
            items.firstOrNull()?.let { item ->
                val tracks = streaming.archiveTracks(item)
                log("ARCHIVE tracks for ${item.identifier} -> ${tracks.size}; first=${tracks.firstOrNull()?.uri}")
            }
        }
        File("build").mkdirs()
        File("build/api-check.txt").writeText(report.toString())
    }
}
