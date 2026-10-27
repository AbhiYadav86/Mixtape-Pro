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
        checkRadioStreams()
        File("build").mkdirs()
        File("build/api-check.txt").writeText(report.toString())
    }

    /** Connects to every radio station in the app and checks real audio comes back. */
    private fun checkRadioStreams() {
        val src = File("src/main/java/com/example/data/repository/MusicRepository.kt").readText()
        val urls = Regex("""uri = "(https?://[^"]+)"""").findAll(src).map { it.groupValues[1] }
            .filter { !it.contains("pixabay") }.distinct().toList()
        val candidates = listOf(
            "http://stream.dancewave.online:8080/dance.mp3",
            "https://stream.laut.fm/eurodance",
            "https://stream.laut.fm/90s-eurodance",
            "https://streams.90s90s.de/eurodance/mp3-192/",
            "https://streams.90s90s.de/main/mp3-192/",
            "https://0n-90s.radionetz.de/0n-90s.mp3",
            "http://icecast.walmradio.com:8000/classic",
            "https://icecast.walmradio.com:8443/classic",
            "https://relay.rainwave.cc/chiptune.mp3",
            "https://scenestream.io/necta128.mp3",
            "https://radio.plaza.one/mp3"
        )
        for (u in urls + candidates.map { "CANDIDATE $it" }) {
            val line = try {
                val target = u.removePrefix("CANDIDATE ")
                // follow redirects by hand (also https -> http), like the phone's player does
                var url = java.net.URL(target)
                var conn = url.openConnection() as java.net.HttpURLConnection
                repeat(5) {
                    conn.instanceFollowRedirects = false
                    conn.connectTimeout = 8000; conn.readTimeout = 8000
                    conn.setRequestProperty("User-Agent", "MixtapePro-RadioTuner/3.0 (Android)")
                    val c = conn.responseCode
                    if (c in 300..399) {
                        val loc = conn.getHeaderField("Location") ?: return@repeat
                        url = java.net.URL(url, loc); conn.disconnect()
                        conn = url.openConnection() as java.net.HttpURLConnection
                    }
                }
                if (url.toString() != target) log("REDIRECT $target -> $url")
                val code = conn.responseCode
                val type = conn.contentType ?: "?"
                var bytes = 0
                val t0 = System.currentTimeMillis()
                if (code in 200..299) conn.inputStream.use { ins ->
                    val buf = ByteArray(8192)
                    while (bytes < 64_000 && System.currentTimeMillis() - t0 < 6000) {
                        val n = ins.read(buf); if (n < 0) break; bytes += n
                    }
                }
                conn.disconnect()
                val ok = code in 200..299 && type.startsWith("audio") && bytes >= 16_000
                "${if (ok) "OK  " else "DEAD"} $code $type ${bytes / 1024}KB ${System.currentTimeMillis() - t0}ms"
            } catch (e: Exception) {
                "DEAD ${e.javaClass.simpleName}: ${e.message}"
            }
            log("RADIO  $line  $u")
            if (u.startsWith("CANDIDATE")) Unit
        }
    }
}
