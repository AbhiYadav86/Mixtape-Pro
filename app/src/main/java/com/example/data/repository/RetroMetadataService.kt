package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer

/**
 * Online metadata lookups for Mixtape Pro.
 *
 *  - Album art: iTunes Search API (song + album search, artist-verified), falling back to Deezer.
 *  - Lyrics:    LRCLIB (synced .lrc preferred, plain text as fallback).
 *
 * Everything that is found is cached on the device (files dir, not cache dir, so Android
 * never evicts it) which means art and lyrics keep working offline after the first lookup.
 */
class RetroMetadataService(private val context: Context) {

    companion object {
        private const val TAG = "MixtapeMetadata"
        private const val USER_AGENT = "MixtapePro/3.0 (Android offline music player; https://lrclib.net)"
        private const val TIMEOUT_MS = 8000

        /** Generic values written by scanners / importers that are not real metadata. */
        private val PLACEHOLDER_ARTISTS = setOf(
            "", "unknown artist", "<unknown>", "unknown", "local artist", "various artists", "various"
        )
        private val PLACEHOLDER_ALBUMS = setOf(
            "", "unknown album", "<unknown>", "flashback album", "device storage", "music", "download",
            "downloads", "local audio", "imported files"
        )
    }

    private val artDir: File by lazy { File(context.filesDir, "artwork").apply { mkdirs() } }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true // If we cannot tell, just try the request.
        }
    }

    fun isPlaceholderArtist(artist: String?): Boolean =
        artist == null || artist.trim().lowercase() in PLACEHOLDER_ARTISTS

    fun isPlaceholderAlbum(album: String?): Boolean =
        album == null || album.trim().lowercase() in PLACEHOLDER_ALBUMS

    /**
     * Clean messy filenames, iTunes suffix tags, file extensions, and extra qualifiers
     * to ensure optimal hit-rate on the iTunes Search API and LRCLIB.
     */
    fun cleanSearchTerm(input: String): String {
        return input
            .replace(Regex("""\.(m4a|mp3|flac|wav|ogg|aac|alac|wma|opus)$""", RegexOption.IGNORE_CASE), "")
            .replace('_', ' ')
            .replace(Regex("""\s*[\(\[][^\)\]]*(remaster|deluxe|bonus|explicit|album version|single version|radio edit|mono|stereo|re-recorded|live|audio|official|lyric|clean|video|hd|hq|320|kbps)[^\)\]]*[\)\]]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*[\(\[]\s*(feat|ft)\.?\s.*?[\)\]]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+(feat|ft)\.?\s.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^\d{1,3}\s*[-–.)]\s*"""), "") // Remove track numbering like "01 - "
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
    }

    /**
     * Many downloaded MP3s have no ID3 tags, so the title is the filename, e.g.
     * "Chris Isaak - Wicked Game". If the artist is unknown, split it out.
     * Returns (artist, title).
     */
    fun splitArtistTitle(artist: String, title: String): Pair<String, String> {
        if (!isPlaceholderArtist(artist)) return artist to title
        val cleaned = cleanSearchTerm(title)
        val parts = cleaned.split(Regex("""\s+[-–—]\s+"""), limit = 2)
        return if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            parts[0].trim() to parts[1].trim()
        } else {
            "" to cleaned
        }
    }

    private fun normalize(s: String): String {
        val noAccents = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("""\p{Mn}+"""), "")
        return noAccents.lowercase()
            .replace("&", "and")
            .replace(Regex("""^the\s+"""), "")
            .replace(Regex("""[^a-z0-9ऀ-ॿ]+"""), " ")
            .trim()
    }

    /** Loose "same thing" match: one normalized string contains the other. */
    private fun looseMatch(a: String, b: String): Boolean {
        val na = normalize(a)
        val nb = normalize(b)
        if (na.isBlank() || nb.isBlank()) return false
        return na == nb || na.contains(nb) || nb.contains(na)
    }

    private fun httpGet(urlStr: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                Log.d(TAG, "HTTP ${conn.responseCode} for $urlStr")
                null
            }
        } catch (e: Exception) {
            Log.d(TAG, "Request failed for $urlStr: ${e.message}")
            null
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }

    /** org.json's optString() returns the literal "null" for JSON null — this does not. */
    private fun JSONObject.stringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key, "")
        return value.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    // ---------------------------------------------------------------------------------------------
    // Album art
    // ---------------------------------------------------------------------------------------------

    /** Returns a cached local file URI for this song's art if we already downloaded it. */
    fun cachedArtFor(songId: String): String? {
        val file = File(artDir, "art_${safeId(songId)}.jpg")
        return if (file.exists() && file.length() > 0) Uri.fromFile(file).toString() else null
    }

    private fun safeId(songId: String) = songId.replace(Regex("""[^a-zA-Z0-9_-]"""), "_")

    /**
     * Save embedded (ID3) picture bytes as this song's artwork.
     */
    fun saveEmbeddedArt(songId: String, bytes: ByteArray): String? {
        return try {
            val file = File(artDir, "art_${safeId(songId)}.jpg")
            file.writeBytes(bytes)
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Find high-resolution artwork online. Tries, in order:
     *  1. iTunes song search, verified against the artist name
     *  2. iTunes album search (if we know a real album name)
     *  3. Deezer track search
     * The image is downloaded into the app's files dir so it survives offline.
     */
    suspend fun fetchAlbumArtwork(
        artist: String,
        title: String,
        album: String?,
        songId: String
    ): String? = withContext(Dispatchers.IO) {
        val (searchArtist, rawTitle) = splitArtistTitle(artist, title)
        val searchTitle = cleanSearchTerm(rawTitle)
        if (searchTitle.isBlank()) return@withContext null

        val imageUrl = itunesSongArt(searchArtist, searchTitle)
            ?: (if (!isPlaceholderAlbum(album) && searchArtist.isNotBlank()) itunesAlbumArt(searchArtist, cleanSearchTerm(album!!)) else null)
            ?: deezerArt(searchArtist, searchTitle)
            ?: return@withContext null

        val cached = downloadImage(imageUrl, File(artDir, "art_${safeId(songId)}.jpg"))
        if (cached != null) Uri.fromFile(cached).toString() else imageUrl
    }

    /** Backwards-compatible name used by older call sites. */
    suspend fun fetchItunesArtwork(artist: String, title: String, songId: String): String? =
        fetchAlbumArtwork(artist, title, null, songId)

    private fun upgradeItunesArt(url: String): String =
        url.replace(Regex("""/\d+x\d+bb\.(jpg|png)$"""), "/600x600bb.jpg")

    private fun itunesSongArt(artist: String, title: String): String? {
        val term = "$artist $title".trim()
        val json = httpGet("https://itunes.apple.com/search?term=${enc(term)}&media=music&entity=song&limit=15") ?: return null
        val results = runCatching { JSONObject(json).optJSONArray("results") }.getOrNull() ?: return null

        var best: JSONObject? = null
        var bestScore = 0
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val itemArtist = item.stringOrNull("artistName") ?: ""
            val itemTitle = item.stringOrNull("trackName") ?: ""
            var score = 0
            if (artist.isNotBlank()) {
                if (!looseMatch(itemArtist, artist)) continue // wrong artist = wrong art
                score += 2
            }
            if (looseMatch(itemTitle, title)) score += 2 else if (artist.isBlank()) continue
            // Prefer original albums over compilations / karaoke
            val collection = (item.stringOrNull("collectionName") ?: "").lowercase()
            if (collection.contains("karaoke") || collection.contains("tribute") || collection.contains("instrumental")) score -= 2
            if (score > bestScore) {
                bestScore = score
                best = item
            }
        }
        val art = best?.stringOrNull("artworkUrl100") ?: return null
        return upgradeItunesArt(art)
    }

    private fun itunesAlbumArt(artist: String, album: String): String? {
        if (album.isBlank()) return null
        val json = httpGet("https://itunes.apple.com/search?term=${enc("$artist $album")}&media=music&entity=album&limit=10") ?: return null
        val results = runCatching { JSONObject(json).optJSONArray("results") }.getOrNull() ?: return null
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            if (looseMatch(item.stringOrNull("artistName") ?: "", artist) &&
                looseMatch(item.stringOrNull("collectionName") ?: "", album)
            ) {
                return item.stringOrNull("artworkUrl100")?.let { upgradeItunesArt(it) }
            }
        }
        return null
    }

    private fun deezerArt(artist: String, title: String): String? {
        val q = if (artist.isNotBlank()) "artist:\"$artist\" track:\"$title\"" else title
        val json = httpGet("https://api.deezer.com/search?q=${enc(q)}&limit=10") ?: return null
        val data = runCatching { JSONObject(json).optJSONArray("data") }.getOrNull() ?: return null
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val itemArtist = item.optJSONObject("artist")?.stringOrNull("name") ?: ""
            val itemTitle = item.stringOrNull("title") ?: ""
            if (artist.isNotBlank() && !looseMatch(itemArtist, artist)) continue
            if (!looseMatch(itemTitle, title)) continue
            val albumObj = item.optJSONObject("album") ?: continue
            return albumObj.stringOrNull("cover_xl") ?: albumObj.stringOrNull("cover_big")
        }
        return null
    }

    private fun downloadImage(imageUrl: String, target: File): File? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", USER_AGENT)
            }
            if (conn.responseCode == 200) {
                val tmp = File(target.parentFile, target.name + ".part")
                conn.inputStream.use { input -> FileOutputStream(tmp).use { input.copyTo(it) } }
                if (tmp.length() > 0 && tmp.renameTo(target)) target else null
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed downloading artwork from $imageUrl", e)
            null
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Lyrics
    // ---------------------------------------------------------------------------------------------

    /**
     * Queries LRCLIB (free, no API key) for synchronized (.lrc) or plain text lyrics.
     */
    suspend fun fetchLrclibLyrics(
        artist: String,
        title: String,
        album: String? = null,
        durationMs: Long = 0L
    ): String? = withContext(Dispatchers.IO) {
        val (searchArtist, rawTitle) = splitArtistTitle(artist, title)
        val cleanArtist = cleanSearchTerm(searchArtist)
        val cleanTitle = cleanSearchTerm(rawTitle)
        if (cleanTitle.isBlank()) return@withContext null

        val durationSec = if (durationMs > 0) durationMs / 1000 else 0

        // Attempt 1: exact signature match via /api/get (needs artist)
        if (cleanArtist.isNotBlank()) {
            var getUrl = "https://lrclib.net/api/get?artist_name=${enc(cleanArtist)}&track_name=${enc(cleanTitle)}"
            if (!isPlaceholderAlbum(album)) getUrl += "&album_name=${enc(cleanSearchTerm(album!!))}"
            if (durationSec > 0) getUrl += "&duration=$durationSec"
            httpGet(getUrl)?.let { body ->
                runCatching { extractLyricsFromObject(JSONObject(body)) }.getOrNull()?.let { return@withContext it }
            }
        }

        // Attempt 2: structured search by fields
        val fieldSearch = buildString {
            append("https://lrclib.net/api/search?track_name=${enc(cleanTitle)}")
            if (cleanArtist.isNotBlank()) append("&artist_name=${enc(cleanArtist)}")
        }
        pickBestSearchResult(httpGet(fieldSearch), cleanArtist, durationSec)?.let { return@withContext it }

        // Attempt 3: free-text search
        val q = "$cleanArtist $cleanTitle".trim()
        pickBestSearchResult(httpGet("https://lrclib.net/api/search?q=${enc(q)}"), cleanArtist, durationSec)
    }

    private fun pickBestSearchResult(body: String?, artist: String, durationSec: Long): String? {
        if (body.isNullOrBlank()) return null
        val array = runCatching { JSONArray(body) }.getOrNull() ?: return null
        if (array.length() == 0) return null

        val candidates = (0 until array.length()).mapNotNull { array.optJSONObject(it) }
            .filter { artist.isBlank() || looseMatch(it.stringOrNull("artistName") ?: "", artist) }
        if (candidates.isEmpty()) return null

        fun durationOk(o: JSONObject): Boolean {
            if (durationSec <= 0) return true
            val d = o.optDouble("duration", 0.0)
            return d <= 0.0 || kotlin.math.abs(d - durationSec) <= 6
        }

        // Prefer: synced + duration match > synced > plain + duration match > anything
        return candidates.firstOrNull { durationOk(it) && it.stringOrNull("syncedLyrics") != null }?.let { extractLyricsFromObject(it) }
            ?: candidates.firstOrNull { it.stringOrNull("syncedLyrics") != null }?.let { extractLyricsFromObject(it) }
            ?: candidates.firstOrNull { durationOk(it) }?.let { extractLyricsFromObject(it) }
            ?: extractLyricsFromObject(candidates.first())
    }

    private fun extractLyricsFromObject(obj: JSONObject): String? {
        obj.stringOrNull("syncedLyrics")?.let { return it }
        obj.stringOrNull("plainLyrics")?.let { return it }
        if (obj.optBoolean("instrumental", false)) return "[00:00.00] ♫ Instrumental ♫"
        return null
    }
}
