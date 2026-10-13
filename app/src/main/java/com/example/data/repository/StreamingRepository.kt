package com.example.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.data.model.AudioSource
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One archive.org item (usually an album / live show / netlabel release). */
data class ArchiveItem(
    val identifier: String,
    val title: String,
    val creator: String,
    val year: String,
    val thumbnailUrl: String
)

/**
 * Free, legal online music sources for the Streaming tab:
 *  - Audius (open decentralized music platform, no API key required)
 *  - Internet Archive (archive.org audio collections, netlabels, live recordings)
 *
 * Tracks are returned as [Song] objects with source = FREE_ARCHIVE so the normal player,
 * mixtapes and DJ Studio can use them. [downloadForOffline] saves a copy to the phone's
 * Music folder so it becomes a regular offline track after the next library scan.
 */
class StreamingRepository(private val context: Context) {

    companion object {
        private const val TAG = "MixtapeStreaming"
        private const val APP_NAME = "MixtapePro"
        private const val TIMEOUT_MS = 10000
        private const val AUDIUS_FALLBACK_HOST = "https://discoveryprovider.audius.co"

        val AUDIUS_GENRES = listOf("All", "Electronic", "Hip-Hop/Rap", "Alternative", "Rock", "Pop", "Trance", "House", "Techno", "Lo-Fi", "R&B/Soul", "Dubstep", "Drum & Bass")

        /** Preset archive.org searches with a 90s flavour. */
        val ARCHIVE_PRESETS = linkedMapOf(
            "90s Netlabels" to "collection:(netlabels) AND year:[1990 TO 2005]",
            "Live 90s Shows" to "collection:(etree) AND year:[1990 TO 1999]",
            "Old Time Radio" to "collection:(oldtimeradio)",
            "78 RPM Classics" to "collection:(georgeblood)",
            "Chiptunes" to "subject:(chiptune) AND mediatype:(audio)",
            "Rave / Techno" to "subject:(techno OR rave OR acid) AND mediatype:(audio) AND year:[1988 TO 2002]"
        )
    }

    @Volatile
    private var audiusHost: String? = null

    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "MixtapePro/3.0 (Android)")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else null
        } catch (e: Exception) {
            Log.d(TAG, "GET failed $urlStr: ${e.message}")
            null
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun JSONObject.str(key: String): String =
        if (!has(key) || isNull(key)) "" else optString(key, "").let { if (it == "null") "" else it }

    // ---------------------------------------------------------------------------------------------
    // Audius
    // ---------------------------------------------------------------------------------------------

    private fun resolveAudiusHost(): String {
        audiusHost?.let { return it }
        val body = httpGet("https://api.audius.co")
        val host = runCatching {
            val arr = JSONObject(body ?: "{}").optJSONArray("data")
            if (arr != null && arr.length() > 0) arr.getString((0 until arr.length()).random()) else null
        }.getOrNull() ?: AUDIUS_FALLBACK_HOST
        audiusHost = host
        return host
    }

    suspend fun audiusTrending(genre: String = "All"): List<Song> = withContext(Dispatchers.IO) {
        val host = resolveAudiusHost()
        val genreParam = if (genre == "All") "" else "&genre=${enc(genre)}"
        val body = httpGet("$host/v1/tracks/trending?app_name=$APP_NAME&time=allTime$genreParam")
            ?: run { audiusHost = null; null }
        parseAudiusTracks(body, host)
    }

    suspend fun audiusSearch(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext audiusTrending()
        val host = resolveAudiusHost()
        val body = httpGet("$host/v1/tracks/search?query=${enc(query)}&app_name=$APP_NAME")
            ?: run { audiusHost = null; null }
        parseAudiusTracks(body, host)
    }

    private fun parseAudiusTracks(body: String?, host: String): List<Song> {
        if (body.isNullOrBlank()) return emptyList()
        val data = runCatching { JSONObject(body).optJSONArray("data") }.getOrNull() ?: return emptyList()
        val out = mutableListOf<Song>()
        for (i in 0 until data.length()) {
            val t = data.optJSONObject(i) ?: continue
            if (t.optBoolean("is_streamable", true).not()) continue
            val id = t.str("id")
            if (id.isBlank()) continue
            val artwork = t.optJSONObject("artwork")
            val art = artwork?.let { it.str("480x480").ifBlank { it.str("1000x1000").ifBlank { it.str("150x150") } } }
            out.add(
                Song(
                    id = "audius_$id",
                    title = t.str("title").ifBlank { "Untitled" },
                    artist = t.optJSONObject("user")?.str("name")?.ifBlank { null } ?: "Audius Artist",
                    album = "Audius",
                    durationMs = t.optLong("duration", 0L) * 1000L,
                    uri = "$host/v1/tracks/$id/stream?app_name=$APP_NAME",
                    albumArtUri = art?.ifBlank { null },
                    source = AudioSource.FREE_ARCHIVE,
                    genre = t.str("genre").ifBlank { "Audius" },
                    year = t.str("release_date").take(4),
                    bitrateKbps = 320,
                    sampleRateKhz = 44,
                    folderPath = "Audius"
                )
            )
        }
        return out
    }

    // ---------------------------------------------------------------------------------------------
    // Internet Archive
    // ---------------------------------------------------------------------------------------------

    suspend fun archiveSearch(queryOrPreset: String, isPreset: Boolean): List<ArchiveItem> = withContext(Dispatchers.IO) {
        val q = if (isPreset) {
            queryOrPreset
        } else {
            val text = queryOrPreset.trim().ifBlank { "90s" }
            "($text) AND mediatype:(audio)"
        }
        val url = "https://archive.org/advancedsearch.php?q=${enc(q)}" +
            "&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=creator&fl%5B%5D=year" +
            "&sort%5B%5D=${enc("downloads desc")}&rows=40&page=1&output=json"
        val body = httpGet(url) ?: return@withContext emptyList()
        val docs = runCatching { JSONObject(body).optJSONObject("response")?.optJSONArray("docs") }.getOrNull()
            ?: return@withContext emptyList()
        (0 until docs.length()).mapNotNull { i ->
            val d = docs.optJSONObject(i) ?: return@mapNotNull null
            val id = d.str("identifier").ifBlank { return@mapNotNull null }
            ArchiveItem(
                identifier = id,
                title = d.str("title").ifBlank { id },
                creator = flatten(d, "creator").ifBlank { "Internet Archive" },
                year = d.str("year").take(4),
                thumbnailUrl = "https://archive.org/services/img/$id"
            )
        }
    }

    /** creator can be a string or an array in archive.org JSON. */
    private fun flatten(obj: JSONObject, key: String): String {
        val v = obj.opt(key) ?: return ""
        return when (v) {
            is JSONArray -> (0 until v.length()).joinToString(", ") { v.optString(it) }
            JSONObject.NULL -> ""
            else -> v.toString()
        }
    }

    /** List the playable MP3 tracks inside an archive.org item. */
    suspend fun archiveTracks(item: ArchiveItem): List<Song> = withContext(Dispatchers.IO) {
        val body = httpGet("https://archive.org/metadata/${enc(item.identifier)}") ?: return@withContext emptyList()
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return@withContext emptyList()
        val files = root.optJSONArray("files") ?: return@withContext emptyList()

        val mp3s = (0 until files.length()).mapNotNull { files.optJSONObject(it) }
            .filter { f ->
                val name = f.str("name").lowercase()
                val format = f.str("format").lowercase()
                name.endsWith(".mp3") && (format.contains("mp3") || format.isBlank())
            }
        // Prefer one encoding (VBR over 64kbps previews) so tracks are not duplicated.
        val preferred = mp3s.filter { it.str("format").contains("VBR", ignoreCase = true) }
            .ifEmpty { mp3s.filter { !it.str("name").contains("_64kb", ignoreCase = true) } }
            .ifEmpty { mp3s }

        preferred.mapIndexed { index, f ->
            val name = f.str("name")
            val trackTitle = f.str("title").ifBlank { name.substringAfterLast('/').substringBeforeLast('.') }
            val lengthSec = f.str("length").let { len ->
                if (len.contains(":")) {
                    val parts = len.split(":").mapNotNull { it.toDoubleOrNull() }
                    parts.fold(0.0) { acc, p -> acc * 60 + p }
                } else len.toDoubleOrNull() ?: 0.0
            }
            val encodedPath = name.split("/").joinToString("/") { seg -> enc(seg).replace("+", "%20") }
            Song(
                id = "ia_${item.identifier}_$index",
                title = trackTitle,
                artist = f.str("creator").ifBlank { f.str("artist").ifBlank { item.creator } },
                album = f.str("album").ifBlank { item.title },
                durationMs = (lengthSec * 1000).toLong(),
                uri = "https://archive.org/download/${enc(item.identifier)}/$encodedPath",
                albumArtUri = item.thumbnailUrl,
                source = AudioSource.FREE_ARCHIVE,
                genre = "Internet Archive",
                year = item.year,
                bitrateKbps = 192,
                sampleRateKhz = 44,
                folderPath = "Archive.org"
            )
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Offline download
    // ---------------------------------------------------------------------------------------------

    /**
     * Download a streamed track into Music/Mixtape Pro/ using Android's DownloadManager.
     * Once finished, the media scanner indexes it and it appears in the Library as a local file.
     */
    fun downloadForOffline(song: Song): Boolean {
        return try {
            val safeName = "${song.artist} - ${song.title}".replace(Regex("""[\\/:*?"<>|]"""), "_").take(120) + ".mp3"
            val request = DownloadManager.Request(Uri.parse(song.uri))
                .setTitle(song.title)
                .setDescription("Mixtape Pro • ${song.artist}")
                .setMimeType("audio/mpeg")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "Mixtape Pro/$safeName")
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Download failed for ${song.uri}", e)
            false
        }
    }
}
