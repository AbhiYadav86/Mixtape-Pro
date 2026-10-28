package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.R
import com.example.data.local.AppDao
import com.example.data.local.AppPreferenceEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongCrossRef
import com.example.data.local.SongEntity
import com.example.data.model.AudioSource
import com.example.data.model.BuiltInArt
import com.example.data.model.Playlist
import com.example.data.model.RadioStationStatus
import com.example.data.model.Song
import com.example.data.model.StationLiveState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MusicRepository(
    private val context: Context,
    private val appDao: AppDao
) {
    companion object {
        private const val PREF_HIDDEN = "library_hidden_ids"
    }

    private val metadataService = RetroMetadataService(context)

    // Room DB streams
    val allSongs: Flow<List<Song>> = appDao.getAllSongs().map { list ->
        list.map { it.toSong() }
    }

    val favoriteSongs: Flow<List<Song>> = appDao.getFavoriteSongs().map { list ->
        list.map { it.toSong() }
    }

    val allPlaylists: Flow<List<Playlist>> = appDao.getAllPlaylists().map { list ->
        list.map { entity ->
            Playlist(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                coverRes = BuiltInArt.cover(entity.coverRes),
                createdAt = entity.createdAt,
                iconName = entity.iconName
            )
        }
    }

    fun getSongsForPlaylist(playlistId: String): Flow<List<Song>> {
        return appDao.getSongsForPlaylist(playlistId).map { list ->
            list.map { it.toSong() }
        }
    }

    suspend fun toggleFavorite(song: Song) {
        val newFav = !song.isFavorite
        appDao.setFavorite(song.id, newFav)
    }

    suspend fun incrementPlayCount(songId: String) {
        appDao.incrementPlayCount(songId)
    }

    suspend fun updateSongMetadata(songId: String, title: String, artist: String, album: String, year: String, genre: String) {
        appDao.updateSongMetadata(songId, title, artist, album, year, genre)
    }

    suspend fun updateSongLyrics(songId: String, lyrics: String) {
        appDao.updateSongLyrics(songId, lyrics)
    }

    suspend fun updateSongAlbumArt(songId: String, albumArtUri: String) {
        appDao.updateSongAlbumArt(songId, albumArtUri)
    }

    /** True when a song has no real artwork (none, or one of the old stock-photo placeholders). */
    fun needsArtwork(song: Song): Boolean {
        val art = song.albumArtUri
        if (art.isNullOrBlank()) return true
        if (art.contains("images.unsplash.com", ignoreCase = true)) return true
        if (art.startsWith("file://")) {
            val path = Uri.parse(art).path
            if (path != null && !File(path).exists()) return true
        }
        return false
    }

    /** Only real music (device files, imports, cloud, saved streams) gets online lookups. */
    fun isEnrichable(song: Song): Boolean =
        song.source != AudioSource.DEMO_JUKEBOX && song.source != AudioSource.RADIO_STREAM &&
            !song.id.startsWith("retro_demo_") && !song.id.startsWith("free_")

    suspend fun getFreshSong(songId: String): Song? = withContext(Dispatchers.IO) {
        appDao.getSongById(songId)?.toSong()
    }

    suspend fun fetchAndCacheItunesArt(song: Song): String? = withContext(Dispatchers.IO) {
        val artUri = metadataService.fetchAlbumArtwork(song.artist, song.title, song.album, song.id)
        if (artUri != null) {
            appDao.updateSongAlbumArt(song.id, artUri)
        }
        artUri
    }

    suspend fun fetchAndCacheLrclibLyrics(song: Song): String? = withContext(Dispatchers.IO) {
        val lyrics = metadataService.fetchLrclibLyrics(song.artist, song.title, song.album, song.durationMs)
        if (!lyrics.isNullOrBlank()) {
            appDao.updateSongLyrics(song.id, lyrics)
        }
        lyrics
    }

    suspend fun enrichSongMetadata(song: Song): Pair<String?, String?> = withContext(Dispatchers.IO) {
        if (!metadataService.isOnline()) return@withContext null to null
        val newArt = if (needsArtwork(song)) fetchAndCacheItunesArt(song) else null
        val newLyrics = if (song.lyrics.isNullOrBlank()) fetchAndCacheLrclibLyrics(song) else null
        newArt to newLyrics
    }

    /**
     * Walk the whole library in the background and fill in missing album art and lyrics.
     * Each song is attempted at most once every [retryAfterMs] so we don't hammer the
     * free APIs (or the battery) on every launch. Calls [onProgress] after each song.
     */
    suspend fun enrichLibraryInBackground(
        retryAfterMs: Long = 3L * 24 * 60 * 60 * 1000,
        onProgress: suspend (done: Int, total: Int, updated: Song?) -> Unit = { _, _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        if (!metadataService.isOnline()) return@withContext 0
        val now = System.currentTimeMillis()
        val candidates = appDao.getAllSongsDirect().map { it.toSong() }
            .filter { isEnrichable(it) && (needsArtwork(it) || it.lyrics.isNullOrBlank()) }
            .filter { song ->
                val last = appDao.getPreference("meta_try_${song.id}")?.toLongOrNull() ?: 0L
                now - last > retryAfterMs
            }

        var updatedCount = 0
        candidates.forEachIndexed { index, song ->
            if (!metadataService.isOnline()) return@withContext updatedCount
            val (art, lyrics) = try {
                enrichSongMetadata(song)
            } catch (e: Exception) {
                null to null
            }
            appDao.setPreference(AppPreferenceEntity("meta_try_${song.id}", now.toString()))
            val changed = art != null || lyrics != null
            if (changed) updatedCount++
            onProgress(index + 1, candidates.size, if (changed) appDao.getSongById(song.id)?.toSong() else null)
            kotlinx.coroutines.delay(350) // be polite to the free APIs
        }
        updatedCount
    }

    suspend fun createPlaylist(name: String, description: String = "", coverRes: Int? = null, iconName: String = "cassette"): String {
        val id = UUID.randomUUID().toString()
        val entity = PlaylistEntity(
            id = id,
            name = name,
            description = description,
            coverRes = coverRes ?: R.drawable.img_cassette_mixtape,
            createdAt = System.currentTimeMillis(),
            iconName = iconName
        )
        appDao.insertPlaylist(entity)
        return id
    }

    suspend fun deletePlaylist(playlistId: String) {
        appDao.clearPlaylistSongs(playlistId)
        appDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: String, song: Song, orderIndex: Int = 0) {
        // Ensure song is saved in songs table
        appDao.insertSong(SongEntity.fromSong(song))
        appDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, song.id, orderIndex))
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        appDao.removeSongFromPlaylist(playlistId, songId)
    }

    /**
     * Scan the device's MediaStore for music and merge it into the library.
     *
     *  - Existing songs keep favorites, play counts, edited tags, fetched lyrics and artwork.
     *  - Songs whose files were deleted from the phone are removed from the library.
     *  - Embedded (ID3) cover art is extracted once, the first time a file is seen.
     *
     * Returns the number of audio files found on the device.
     */
    // ---------------------------------------------------------------------------------------------
    // Removing tracks
    // ---------------------------------------------------------------------------------------------

    private suspend fun hiddenIds(): MutableSet<String> =
        appDao.getPreference(PREF_HIDDEN)?.split('\n')?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()

    /**
     * Remove songs from the Library (and every mixtape). With [keepHidden], device songs stay
     * out of the Library on the next scan even though their files still exist.
     */
    suspend fun removeFromLibrary(songIds: List<String>, keepHidden: Boolean) = withContext(Dispatchers.IO) {
        if (songIds.isEmpty()) return@withContext
        songIds.chunked(500).forEach { chunk ->
            appDao.removeSongsFromAllPlaylists(chunk)
            appDao.deleteSongs(chunk)
        }
        if (keepHidden) {
            val hidden = hiddenIds()
            hidden.addAll(songIds.filter { it.startsWith("local_") })
            appDao.setPreference(AppPreferenceEntity(PREF_HIDDEN, hidden.joinToString("\n")))
        }
    }

    suspend fun hiddenCount(): Int = hiddenIds().size

    /** Let tracks removed with "Remove from Library" come back on the next scan. */
    suspend fun clearHiddenTracks() = withContext(Dispatchers.IO) {
        appDao.setPreference(AppPreferenceEntity(PREF_HIDDEN, ""))
    }

    suspend fun scanLocalAudioFiles(): Int = withContext(Dispatchers.IO) {
        // One-time cleanup of stock-photo placeholders that older builds stored as "album art"
        try { appDao.clearStockPhotoArtOnLocalSongs() } catch (_: Exception) {}

        val existing = appDao.getAllSongsDirect().associateBy { it.id }
        val hidden = hiddenIds()
        val merged = mutableListOf<SongEntity>()
        val seenIds = HashSet<String>()

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projection.add(MediaStore.Audio.Media.GENRE)
            projection.add(MediaStore.Audio.Media.BITRATE)
        }

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor: Cursor? = try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                selection,
                null,
                sortOrder
            )
        } catch (e: Exception) {
            null // Permission not granted yet
        }

        if (cursor == null) return@withContext 0

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val durationCol = it.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val dataCol = it.getColumnIndex(MediaStore.Audio.Media.DATA)
            val albumIdCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val yearCol = it.getColumnIndex(MediaStore.Audio.Media.YEAR)
            val dateAddedCol = it.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
            val nameCol = it.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) it.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1
            val bitrateCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) it.getColumnIndex(MediaStore.Audio.Media.BITRATE) else -1

            while (it.moveToNext()) {
                val mediaId = it.getLong(idCol)
                val songId = "local_$mediaId"
                seenIds.add(songId)
                if (songId in hidden) continue

                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId).toString()
                val prev = existing[songId]
                if (prev != null) {
                    // Keep everything the user (or a previous lookup) already set; only refresh the file location.
                    merged.add(prev.copy(uri = contentUri))
                    continue
                }

                val fileName = if (nameCol >= 0) it.getString(nameCol) ?: "" else ""
                val rawTitle = (if (titleCol >= 0) it.getString(titleCol) else null)
                    ?.takeIf { t -> t.isNotBlank() } ?: fileName.substringBeforeLast(".").ifBlank { "Unknown Track" }
                val rawArtist = (if (artistCol >= 0) it.getString(artistCol) else null) ?: ""
                val rawAlbum = (if (albumCol >= 0) it.getString(albumCol) else null) ?: ""
                val artist = if (rawArtist.isBlank() || rawArtist.contains("<unknown>")) "Unknown Artist" else rawArtist
                val album = if (rawAlbum.isBlank() || rawAlbum.contains("<unknown>")) "" else rawAlbum
                val duration = if (durationCol >= 0) it.getLong(durationCol) else 0L
                val path = if (dataCol >= 0) it.getString(dataCol) ?: "" else ""
                val folderName = if (path.isNotEmpty()) File(path).parentFile?.name ?: "Music" else "Music"
                val year = if (yearCol >= 0) it.getInt(yearCol).takeIf { y -> y in 1900..2100 }?.toString() ?: "" else ""
                val genre = if (genreCol >= 0) it.getString(genreCol)?.takeIf { g -> g.isNotBlank() } ?: "" else ""
                val bitrateKbps = if (bitrateCol >= 0) (it.getLong(bitrateCol) / 1000L).toInt().takeIf { b -> b > 0 } ?: 192 else 192
                val addedAt = if (dateAddedCol >= 0) it.getLong(dateAddedCol) * 1000L else System.currentTimeMillis()

                merged.add(
                    SongEntity(
                        id = songId,
                        title = rawTitle,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        uri = contentUri,
                        albumArtUri = extractLocalArtwork(songId, contentUri, if (albumIdCol >= 0) it.getLong(albumIdCol) else -1L),
                        albumArtRes = null,
                        source = AudioSource.LOCAL.name,
                        genre = genre,
                        year = year,
                        bitrateKbps = bitrateKbps,
                        sampleRateKhz = 44,
                        isFavorite = false,
                        folderPath = folderName,
                        addedTimestamp = addedAt
                    )
                )
            }
        }

        if (merged.isNotEmpty()) {
            appDao.insertSongs(merged)
        }

        // Remove library entries for files that no longer exist on the device.
        val removed = existing.keys.filter { id -> id.startsWith("local_") && id !in seenIds }
        if (removed.isNotEmpty()) {
            removed.chunked(500).forEach { chunk ->
                appDao.removeSongsFromAllPlaylists(chunk)
                appDao.deleteSongs(chunk)
            }
        }

        seenIds.size
    }

    /** Embedded ID3 art first (most reliable), then the legacy MediaStore album-art provider. */
    private fun extractLocalArtwork(songId: String, contentUri: String, albumId: Long): String? {
        metadataService.cachedArtFor(songId)?.let { return it }

        var retriever: android.media.MediaMetadataRetriever? = null
        try {
            retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(contentUri))
            val picture = retriever.embeddedPicture
            if (picture != null && picture.isNotEmpty()) {
                metadataService.saveEmbeddedArt(songId, picture)?.let { return it }
            }
        } catch (_: Exception) {
        } finally {
            try { retriever?.release() } catch (_: Exception) {}
        }

        if (albumId > 0) {
            val artUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
            try {
                // Only accept the legacy album-art provider if it really returns a decodable image
                context.contentResolver.openInputStream(artUri)?.use { stream ->
                    val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeStream(stream, null, opts)
                    if (opts.outWidth > 0 && opts.outHeight > 0) return artUri.toString()
                }
            } catch (_: Exception) {}
        }
        return null
    }

    suspend fun populateInitialRetroLibraryIfEmpty() = withContext(Dispatchers.IO) {
        val demoTracks = getVintageRetroTracks()
        appDao.insertSongs(demoTracks.map { SongEntity.fromSong(it) })
        seedAllCuratedMixtapes(force = false)
    }

    suspend fun seedAllCuratedMixtapes(force: Boolean = false) = withContext(Dispatchers.IO) {
        val existingPlaylists = appDao.getAllPlaylistsDirect()
        val existingNames = existingPlaylists.map { it.name }.toSet()
        val demoTracks = getVintageRetroTracks()

        // Insert tracks into songs database if not present
        appDao.insertSongs(demoTracks.map { SongEntity.fromSong(it) })

        val curatedMixtapes = listOf(
            Triple("Summer '99 Mixtape", "The best nostalgic eurodance, pop and dance hits", listOf("retro_demo_01", "retro_demo_04", "retro_demo_13", "retro_demo_14")),
            Triple("Y2K Millennium Party", "High-energy millennium rave, trance, and dance anthems", listOf("retro_demo_04", "retro_demo_06", "retro_demo_12", "retro_demo_16")),
            Triple("Late Night Neon Cruise", "Chilled 90s synthwave, lofi & atmospheric 808s", listOf("retro_demo_02", "retro_demo_07", "retro_demo_10", "retro_demo_15")),
            Triple("Seattle Grunge & Alt '94", "Raw distortion, flannel vibes & alternative rock", listOf("retro_demo_03", "retro_demo_08", "free_grunge_rebellion")),
            Triple("Golden Era Boom Bap 1996", "East Coast breakbeats, cassette tape dubs & 90s rap", listOf("retro_demo_05", "retro_demo_09", "free_90s_hiphop_groove")),
            Triple("Cyberpunk 1999 Hacker Deck", "IDM, 16-bit chiptunes, glitch & industrial beats", listOf("retro_demo_10", "retro_demo_11", "retro_demo_06")),
            Triple("Tokyo Midnight City Pop & Wave", "Smooth nostalgic city pop, night drive & chillwave", listOf("retro_demo_07", "retro_demo_02", "retro_demo_15")),
            Triple("Goa & Warehouse Rave Anthems", "Hypnotic 303 acid lines & sun-drenched beach raves", listOf("retro_demo_12", "retro_demo_06", "retro_demo_16", "free_rave_acid_frenzy"))
        )

        curatedMixtapes.forEach { (name, desc, songIds) ->
            if (force || !existingNames.contains(name)) {
                val icon = when {
                    name.contains("Rave") || name.contains("Party") -> "disc"
                    name.contains("Cyberpunk") || name.contains("Neon") -> "radio"
                    else -> "cassette"
                }
                val coverRes = when {
                    name.contains("Grunge") || name.contains("Party") -> R.drawable.img_cd_jewel_case
                    name.contains("Cyberpunk") || name.contains("Neon") -> R.drawable.img_app_icon
                    else -> R.drawable.img_cassette_mixtape
                }
                val playlistId = createPlaylist(name, desc, coverRes, icon)
                val songsToAdd = songIds.mapNotNull { id ->
                    demoTracks.find { it.id == id } ?: searchOnlineRetroSongs("").find { it.id == id }
                }
                songsToAdd.forEachIndexed { index, song ->
                    addSongToPlaylist(playlistId, song, index)
                }
            }
        }
    }

    fun getFrequencyForStation(stationId: String): String {
        return when (stationId) {
            "radio_90s_eurodance" -> "104.5 FM"
            "radio_90s_hits" -> "98.7 FM"
            "radio_2000s_pop" -> "99.5 FM"
            "radio_90s_grunge" -> "92.9 FM"
            "radio_groove_salad" -> "88.5 FM"
            "radio_secret_agent" -> "90.3 FM"
            "radio_drone_zone" -> "89.7 FM"
            "radio_underground_80s" -> "95.3 FM"
            "radio_beat_blender" -> "102.3 FM"
            "radio_defcon" -> "101.1 FM"
            "radio_chiptune_game" -> "89.1 FM"
            "radio_suburbs_goa" -> "107.9 FM"
            "radio_poptron_2000s" -> "103.1 FM"
            "radio_lush_2000s" -> "96.7 FM"
            "radio_spacestation_2000s" -> "105.7 FM"
            "radio_cliqhop_2000s" -> "94.1 FM"
            "radio_fluid_2000s" -> "97.3 FM"
            "radio_mission_control_2000s" -> "91.5 FM"
            "radio_pirate_cassette_standby" -> "106.9 FM"
            else -> "98.5 FM"
        }
    }

    fun getEstimatedListenersForStation(stationId: String): Int {
        val hash = Math.abs(stationId.hashCode())
        return 450 + (hash % 2800)
    }

    suspend fun checkStationLiveStatus(station: Song): RadioStationStatus = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        var isLive = false
        var responseLatency = 0L

        if (station.id == "radio_pirate_cassette_standby") {
            // Simulated offline standby archive station for demonstration
            return@withContext RadioStationStatus(
                stationId = station.id,
                state = StationLiveState.OFFLINE,
                pingMs = 0L,
                signalPercent = 0,
                listenerCount = 0,
                frequencyMhz = getFrequencyForStation(station.id),
                bitrateStr = "OFFLINE • 106.9 MHz",
                isLiveStream = false,
                lastChecked = System.currentTimeMillis()
            )
        }

        // A station counts as live only if it really streams audio the player can open:
        // first try, then one retry so a slow mobile network doesn't mark it dead.
        isLive = probeStream(station.uri) || probeStream(station.uri)
        responseLatency = (System.currentTimeMillis() - t0).coerceAtLeast(18L)

        val state = if (isLive) StationLiveState.LIVE else StationLiveState.OFFLINE
        val signalPercent = if (isLive) {
            (99 - (responseLatency / 35).toInt()).coerceIn(68, 99)
        } else {
            0
        }
        val listeners = if (isLive) getEstimatedListenersForStation(station.id) else 0
        val freq = getFrequencyForStation(station.id)
        val bitrate = "${station.bitrateKbps} kbps MP3"

        RadioStationStatus(
            stationId = station.id,
            state = state,
            pingMs = responseLatency,
            signalPercent = signalPercent,
            listenerCount = listeners,
            frequencyMhz = freq,
            bitrateStr = bitrate,
            isLiveStream = isLive,
            lastChecked = System.currentTimeMillis()
        )
    }

    /**
     * Opens the stream the way the player will and reads the first few KB of audio.
     * Redirects are followed by hand so an https -> http hop (which Android's MediaPlayer
     * refuses) counts as dead instead of looking fine.
     */
    private fun probeStream(startUrl: String): Boolean {
        if (!startUrl.startsWith("http")) return false
        var url = startUrl
        repeat(5) {
            var conn: java.net.HttpURLConnection? = null
            try {
                conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = false
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "MixtapePro-RadioTuner/3.0 (Android)")
                    setRequestProperty("Icy-MetaData", "0")
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val next = conn.getHeaderField("Location") ?: return false
                    val resolved = java.net.URL(java.net.URL(url), next).toString()
                    if (url.startsWith("https:") && resolved.startsWith("http:")) return false
                    url = resolved
                    return@repeat
                }
                if (code != 200) return false
                val type = (conn.contentType ?: "").lowercase()
                if (type.startsWith("text/html") || type.startsWith("application/json")) return false
                // Must actually deliver audio bytes within the timeout
                val buf = ByteArray(4096)
                var total = 0
                val deadline = System.currentTimeMillis() + 6000
                conn.inputStream.use { input ->
                    while (total < 8 * 1024 && System.currentTimeMillis() < deadline) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        total += n
                    }
                }
                return total >= 8 * 1024
            } catch (e: Exception) {
                return false
            } finally {
                conn?.disconnect()
            }
        }
        return false
    }

    // 90s and 2000s Live Retro Radio Stations
    fun getRetroRadioStations(): List<Song> {
        return listOf(
            Song(
                id = "radio_90s_eurodance",
                title = "90s Eurodance FM",
                artist = "Eurodance Non-Stop Radio",
                album = "Live 90s Dance Radio",
                durationMs = 0L,
                uri = "https://streams.90s90s.de/eurodance/mp3-192/",
                albumArtUri = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cassette_mixtape,
                source = AudioSource.RADIO_STREAM,
                genre = "Eurodance / Pop",
                year = "1998",
                bitrateKbps = 192,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_90s_hits",
                title = "1990s Mega Hits Live",
                artist = "WALM 90s Retro Radio",
                album = "Billboard 90s Top 100",
                durationMs = 0L,
                uri = "https://icecast.walmradio.com:8443/classic",
                albumArtUri = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cd_jewel_case,
                source = AudioSource.RADIO_STREAM,
                genre = "90s Pop / Rock",
                year = "1999",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_2000s_pop",
                title = "Y2K 2000s Millennium Pop",
                artist = "NTS Live Underground",
                album = "Total Request Live 2000",
                durationMs = 0L,
                uri = "https://stream-relay-geo.ntslive.net/stream",
                albumArtUri = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_app_icon,
                source = AudioSource.RADIO_STREAM,
                genre = "2000s Pop / R&B",
                year = "2001",
                bitrateKbps = 160,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_90s_grunge",
                title = "Seattle Grunge & Alternative",
                artist = "SomaFM Indie Pop Rocks",
                album = "Flannel & Distortion 1994",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/indiepop-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cassette_mixtape,
                source = AudioSource.RADIO_STREAM,
                genre = "Grunge / Alt Rock",
                year = "1994",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_groove_salad",
                title = "Groove Salad 90s Ambient",
                artist = "SomaFM Ambient Lounge",
                album = "Downtempo Chillout 1999",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/groovesalad-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_app_icon,
                source = AudioSource.RADIO_STREAM,
                genre = "Ambient / Trip-Hop",
                year = "1999",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_secret_agent",
                title = "Secret Agent 007 Lounge",
                artist = "SomaFM Spy & Lounge",
                album = "Vintage 60s/90s Spy Themes",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/secretagent-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1485579149621-3123dd979885?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cd_jewel_case,
                source = AudioSource.RADIO_STREAM,
                genre = "Spy Lounge / Acid Jazz",
                year = "1997",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_drone_zone",
                title = "Drone Zone Deep Space",
                artist = "SomaFM Atmospheric Waves",
                album = "Interstellar Soundscapes",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/dronezone-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_app_icon,
                source = AudioSource.RADIO_STREAM,
                genre = "Deep Ambient",
                year = "1998",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_underground_80s",
                title = "Underground Wave & Synth",
                artist = "SomaFM Underground",
                album = "Post-Punk & New Wave Hits",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/u80s-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cassette_mixtape,
                source = AudioSource.RADIO_STREAM,
                genre = "New Wave / Synth",
                year = "1989",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_beat_blender",
                title = "Beat Blender Filter House",
                artist = "SomaFM French Touch",
                album = "Club House & Filter Disco",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/beatblender-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cd_jewel_case,
                source = AudioSource.RADIO_STREAM,
                genre = "House / Disco",
                year = "1999",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_defcon",
                title = "DEF CON Hacker Radio",
                artist = "SomaFM Cyberpunk Live",
                album = "Music for Hackers & Geeks",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/defcon-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_app_icon,
                source = AudioSource.RADIO_STREAM,
                genre = "Cyberpunk / Industrial",
                year = "1996",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_chiptune_game",
                title = "16-Bit Super Chiptune Radio",
                artist = "Retro VGM Legends",
                album = "SNES & MegaDrive Classics",
                durationMs = 0L,
                uri = "https://relay.rainwave.cc/chiptune.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cd_jewel_case,
                source = AudioSource.RADIO_STREAM,
                genre = "Chiptune / 16-Bit",
                year = "1993",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_suburbs_goa",
                title = "Suburbs of Goa Psy-Trance",
                artist = "SomaFM Goa Express",
                album = "90s Psychedelic Trance",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/suburbsofgoa-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_app_icon,
                source = AudioSource.RADIO_STREAM,
                genre = "Goa Trance / Psy",
                year = "1997",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            // Dedicated Early 2000s Radio Stations
            Song(
                id = "radio_poptron_2000s",
                title = "PopTron 2000s Electropop",
                artist = "SomaFM PopTron Live",
                album = "Electropop & Y2K Indie Dance",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/poptron-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cd_jewel_case,
                source = AudioSource.RADIO_STREAM,
                genre = "2000s Electropop / Indie",
                year = "2003",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_lush_2000s",
                title = "Lush 2000s Downtempo & Chill",
                artist = "SomaFM Lush Sensuous",
                album = "2000s Female Vocals & Chillout",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/lush-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cassette_mixtape,
                source = AudioSource.RADIO_STREAM,
                genre = "2000s Downtempo / Dream",
                year = "2002",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_spacestation_2000s",
                title = "Space Station Soma 2000s",
                artist = "SomaFM Spacewave",
                album = "Mid-Tempo Electronica 2000",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/spacestation-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_app_icon,
                source = AudioSource.RADIO_STREAM,
                genre = "2000s Ambient Electronic",
                year = "2000",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_cliqhop_2000s",
                title = "Cliqhop 2000s IDM & Glitch",
                artist = "SomaFM Cliqhop Beats",
                album = "Blips, Clicks & Y2K IDM",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/cliqhop-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cd_jewel_case,
                source = AudioSource.RADIO_STREAM,
                genre = "2000s IDM / Glitch",
                year = "2001",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_covers_2000s",
                title = "Covers 2000s All Cover Songs",
                artist = "SomaFM Covers Station",
                album = "2000s Alternative & Indie Covers",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/covers-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cassette_mixtape,
                source = AudioSource.RADIO_STREAM,
                genre = "2000s Indie / Rock Covers",
                year = "2004",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_bootliquor_2000s",
                title = "Boot Liquor 2000s Roots",
                artist = "SomaFM Americana Lounge",
                album = "Americana & Alt-Country 2002",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/bootliquor-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cd_jewel_case,
                source = AudioSource.RADIO_STREAM,
                genre = "2000s Alt-Country / Roots",
                year = "2002",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_illstreet_2000s",
                title = "Illinois Street 2000s Lounge",
                artist = "SomaFM Vintage Exotica",
                album = "Bachelor Pad & Y2K Lounge",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/illstreet-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1485579149621-3123dd979885?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_app_icon,
                source = AudioSource.RADIO_STREAM,
                genre = "2000s Exotica / Lounge",
                year = "2001",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_fluid_2000s",
                title = "Fluid 2000s Future Hip-Hop",
                artist = "SomaFM Fluid Beats",
                album = "Downtempo & 2000s Instrumental",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/fluid-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cassette_mixtape,
                source = AudioSource.RADIO_STREAM,
                genre = "2000s Hip-Hop / Instrumental",
                year = "2004",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_mission_control_2000s",
                title = "Mission Control 2000s NASA",
                artist = "SomaFM Space Audio",
                album = "Live NASA Comms & Ambient 2003",
                durationMs = 0L,
                uri = "https://ice5.somafm.com/missioncontrol-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_app_icon,
                source = AudioSource.RADIO_STREAM,
                genre = "2000s Ambient / Space",
                year = "2003",
                bitrateKbps = 128,
                sampleRateKhz = 44
            ),
            Song(
                id = "radio_pirate_cassette_standby",
                title = "106.9 FM Pirate Radio (Midnight Tape)",
                artist = "Underground Tape Dubbers",
                album = "Midnight Broadcast (Off-Air Archive)",
                durationMs = 0L,
                uri = "synth://pirate_tape_archive",
                albumArtUri = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cassette_mixtape,
                source = AudioSource.RADIO_STREAM,
                genre = "Pirate Radio / Archive",
                year = "1997",
                bitrateKbps = 96,
                sampleRateKhz = 32
            )
        )
    }

    // Free downloadable & streamable public 90s/00s songs catalog
    fun searchOnlineRetroSongs(query: String): List<Song> {
        val catalog = listOf(
            Song(
                id = "free_euro_rhythm",
                title = "Rhythm of the Millennium (Club Mix)",
                artist = "DJ Flashback '99",
                album = "Eurodance Superstars Vol. 1",
                durationMs = 214000L,
                uri = "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=retro-eurodance-112194.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cassette_mixtape,
                source = AudioSource.FREE_ARCHIVE,
                genre = "Eurodance",
                year = "1999",
                bitrateKbps = 320,
                sampleRateKhz = 44
            ),
            Song(
                id = "free_synthwave_drive",
                title = "Sunset Boulevard '98",
                artist = "Neon Drive Project",
                album = "Vapor City Lights",
                durationMs = 198000L,
                uri = "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=synthwave-80s-90s-110045.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_app_icon,
                source = AudioSource.FREE_ARCHIVE,
                genre = "Synthwave",
                year = "1998",
                bitrateKbps = 256,
                sampleRateKhz = 44
            ),
            Song(
                id = "free_grunge_rebellion",
                title = "Smells Like Cassette Tape",
                artist = "The Flannel Kids",
                album = "Garage Anthems 1994",
                durationMs = 245000L,
                uri = "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8bbf8e5c1.mp3?filename=grunge-rock-riff-108233.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cd_jewel_case,
                source = AudioSource.FREE_ARCHIVE,
                genre = "Grunge / Rock",
                year = "1994",
                bitrateKbps = 192,
                sampleRateKhz = 44
            ),
            Song(
                id = "free_y2k_pop_anthem",
                title = "Call Me On My Beeper",
                artist = "Britney & The Boyz",
                album = "Millennium Pop Sensation",
                durationMs = 186000L,
                uri = "https://cdn.pixabay.com/download/audio/2022/10/14/audio_9939f792cb.mp3?filename=pop-summer-y2k-123791.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cd_jewel_case,
                source = AudioSource.FREE_ARCHIVE,
                genre = "Y2K Pop",
                year = "2000",
                bitrateKbps = 320,
                sampleRateKhz = 44
            ),
            Song(
                id = "free_90s_hiphop_groove",
                title = "Boom Bap Street Corner",
                artist = "MC Cassette Deck",
                album = "Golden Era Chronicles",
                durationMs = 205000L,
                uri = "https://cdn.pixabay.com/download/audio/2021/08/09/audio_88447e769f.mp3?filename=old-school-hiphop-90s-14304.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cassette_mixtape,
                source = AudioSource.FREE_ARCHIVE,
                genre = "90s Hip-Hop",
                year = "1996",
                bitrateKbps = 256,
                sampleRateKhz = 44
            ),
            Song(
                id = "free_rave_acid_frenzy",
                title = "Acid Overdrive (TB-303 Mix)",
                artist = "The Cyber Ravers",
                album = "Warehouse Rave UK 1995",
                durationMs = 260000L,
                uri = "https://cdn.pixabay.com/download/audio/2022/02/07/audio_64771239c0.mp3?filename=trance-rave-techno-105128.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_app_icon,
                source = AudioSource.FREE_ARCHIVE,
                genre = "Rave / Acid",
                year = "1995",
                bitrateKbps = 320,
                sampleRateKhz = 44
            )
        )

        if (query.isBlank()) return catalog
        return catalog.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.genre.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }
    }

    // Google Drive audio file finder & streamer
    suspend fun importGoogleDriveFile(fileUrlOrId: String, title: String = "Drive MP3 Track", artist: String = "Google Drive"): Song = withContext(Dispatchers.IO) {
        val cleanUrl = if (fileUrlOrId.contains("drive.google.com")) {
            val fileId = extractDriveId(fileUrlOrId)
            "https://docs.google.com/uc?export=download&id=$fileId"
        } else {
            fileUrlOrId
        }

        val song = Song(
            id = "drive_${UUID.randomUUID()}",
            title = title.ifBlank { "Drive Audio Track" },
            artist = artist.ifBlank { "Google Drive" },
            album = "Google Drive Cloud Vault",
            durationMs = 180000L,
            uri = cleanUrl,
            albumArtRes = R.drawable.img_cd_jewel_case,
            source = AudioSource.GOOGLE_DRIVE,
            genre = "Cloud MP3",
            year = "2000",
            bitrateKbps = 256,
            sampleRateKhz = 44
        )

        appDao.insertSong(SongEntity.fromSong(song))
        song
    }

    // Cloud storage audio import (Dropbox, OneDrive, WebDAV, Direct HTTP/S Streams)
    suspend fun importCloudStorageFile(
        url: String,
        title: String,
        artist: String,
        genre: String = "Cloud Stream",
        album: String = "Cloud Vault"
    ): Song = withContext(Dispatchers.IO) {
        // Handle Dropbox direct download conversion
        var streamUrl = url.trim()
        if (streamUrl.contains("dropbox.com") && streamUrl.contains("dl=0")) {
            streamUrl = streamUrl.replace("dl=0", "dl=1")
        }

        val cleanTitle = title.ifBlank {
            // Extract filename from URL
            streamUrl.substringAfterLast("/").substringBefore("?").ifBlank { "Cloud Audio Stream" }
        }

        val song = Song(
            id = "cloud_${UUID.randomUUID()}",
            title = cleanTitle,
            artist = artist.ifBlank { "Cloud Storage" },
            album = album.ifBlank { "Cloud & Web Vault" },
            durationMs = 0L,
            uri = streamUrl,
            albumArtRes = R.drawable.img_app_icon,
            source = AudioSource.CLOUD_STORAGE,
            genre = genre.ifBlank { "Cloud Audio" },
            year = "2000",
            bitrateKbps = 256,
            sampleRateKhz = 44
        )

        appDao.insertSong(SongEntity.fromSong(song))
        song
    }

    // Local device audio import from Document / File Picker Uri
    suspend fun importLocalAudioUri(uri: Uri, displayName: String? = null): Song = withContext(Dispatchers.IO) {
        // Keep read access to the picked file across reboots
        try {
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {}

        val songId = "picked_${UUID.randomUUID()}"
        val fallbackName = displayName ?: queryDisplayName(uri) ?: "Local Audio Track"
        var trackTitle = fallbackName.substringBeforeLast(".")
        var trackArtist = "Unknown Artist"
        var trackAlbum = ""
        var trackYear = ""
        var duration = 0L
        var genre = ""
        var albumArtUri: String? = null

        var retriever: android.media.MediaMetadataRetriever? = null
        try {
            retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                if (it.isNotBlank()) trackTitle = it
            }
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                if (it.isNotBlank()) trackArtist = it
            }
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM)?.let {
                if (it.isNotBlank()) trackAlbum = it
            }
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_GENRE)?.let {
                if (it.isNotBlank()) genre = it
            }
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_YEAR)?.let {
                if (it.isNotBlank()) trackYear = it.take(4)
            }
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                duration = it
            }
            val picture = retriever.embeddedPicture
            if (picture != null && picture.isNotEmpty()) {
                albumArtUri = metadataService.saveEmbeddedArt(songId, picture)
            }
        } catch (e: Exception) {
            // Use defaults if retriever fails
        } finally {
            try { retriever?.release() } catch (_: Exception) {}
        }

        val song = Song(
            id = songId,
            title = trackTitle,
            artist = trackArtist,
            album = trackAlbum,
            durationMs = duration,
            uri = uri.toString(),
            albumArtUri = albumArtUri,
            albumArtRes = null,
            source = AudioSource.LOCAL,
            genre = genre,
            year = trackYear,
            bitrateKbps = 320,
            sampleRateKhz = 44,
            lyrics = null,
            folderPath = "Imported Files"
        )

        appDao.insertSong(SongEntity.fromSong(song))
        // Art & lyrics are filled in by the background enrichment pass (or when the song plays).
        song
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getRoyaltyFreeArtForArtist(artist: String, genre: String): String {
        val artList = listOf(
            "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1485579149621-3123dd979885?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=600&auto=format&fit=crop&q=80"
        )
        val hash = (artist + genre).hashCode()
        val index = Math.abs(hash) % artList.size
        return artList[index]
    }

    private fun extractDriveId(url: String): String {
        val pattern1 = "/d/([a-zA-Z0-9_-]+)".toRegex()
        val pattern2 = "id=([a-zA-Z0-9_-]+)".toRegex()
        return pattern1.find(url)?.groupValues?.get(1)
            ?: pattern2.find(url)?.groupValues?.get(1)
            ?: url
    }

    private fun getVintageRetroTracks(): List<Song> {
        return listOf(
            Song(
                id = "retro_demo_01",
                title = "Turn It Up Tonight (1998 Extended Mix)",
                artist = "DJ Pixel & The Bassline",
                album = "Bass In Your Face '98",
                durationMs = 218000L,
                uri = "https://streams.90s90s.de/eurodance/mp3-192/",
                albumArtUri = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cassette_mixtape,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "Eurodance",
                year = "1998",
                bitrateKbps = 192,
                sampleRateKhz = 44,
                folderPath = "Demo Tapes",
                isFavorite = true
            ),
            Song(
                id = "retro_demo_02",
                title = "Late Night Neon Cruise (Synthwave)",
                artist = "Neon Highway Wave",
                album = "Y2K Synth Odyssey",
                durationMs = 242000L,
                uri = "https://ice5.somafm.com/groovesalad-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_app_icon,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "Synthwave",
                year = "1999",
                bitrateKbps = 320,
                sampleRateKhz = 44,
                folderPath = "Synth Vault",
                isFavorite = true
            ),
            Song(
                id = "retro_demo_03",
                title = "Teenage Garage Riot '95",
                artist = "The Flannel Generation",
                album = "Dial-Up Distortion",
                durationMs = 195000L,
                uri = "https://ice5.somafm.com/indiepop-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cd_jewel_case,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "Grunge / Rock",
                year = "1995",
                bitrateKbps = 160,
                sampleRateKhz = 44,
                folderPath = "Rock Mixtapes"
            ),
            Song(
                id = "retro_demo_04",
                title = "Download Complete (Y2K Pop)",
                artist = "Millennium Pop Queens",
                album = "Countdown Top 20 1999",
                durationMs = 208000L,
                uri = "https://ice5.somafm.com/poptron-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cd_jewel_case,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "Pop",
                year = "1999",
                bitrateKbps = 128,
                sampleRateKhz = 44,
                folderPath = "P2P Downloads",
                isFavorite = true
            ),
            Song(
                id = "retro_demo_05",
                title = "Brooklyn Subwoofer 808s",
                artist = "Cassette Deck Masters",
                album = "Boombox Chronicles",
                durationMs = 230000L,
                uri = "https://ice5.somafm.com/beatblender-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cassette_mixtape,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "Hip-Hop",
                year = "1996",
                bitrateKbps = 256,
                sampleRateKhz = 44,
                folderPath = "Boombox"
            ),
            Song(
                id = "retro_demo_06",
                title = "Warehouse Acid Rave 303",
                artist = "London Underground 96",
                album = "Warehouse Nostalgia",
                durationMs = 254000L,
                uri = "https://relay.rainwave.cc/chiptune.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_app_icon,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "Acid Techno",
                year = "1996",
                bitrateKbps = 320,
                sampleRateKhz = 44,
                folderPath = "Rave Tapes"
            ),
            Song(
                id = "retro_demo_07",
                title = "Tokyo Midnight Wangan Drift",
                artist = "City Pop Wave 98",
                album = "Shinjuku Highway Drive",
                durationMs = 238000L,
                uri = "https://ice5.somafm.com/groovesalad-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cassette_mixtape,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "City Pop / Synth",
                year = "1998",
                bitrateKbps = 320,
                sampleRateKhz = 44,
                folderPath = "Tokyo Wave"
            ),
            Song(
                id = "retro_demo_08",
                title = "Seattle Coffeehouse Flannel Jam",
                artist = "Grunge Revolution 94",
                album = "Pine & Rain Anthology",
                durationMs = 212000L,
                uri = "https://ice5.somafm.com/indiepop-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cd_jewel_case,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "Alternative Rock",
                year = "1994",
                bitrateKbps = 192,
                sampleRateKhz = 44,
                folderPath = "Rock Mixtapes"
            ),
            Song(
                id = "retro_demo_09",
                title = "Golden Era East Coast Boom Bap",
                artist = "East Coast Lyricists",
                album = "Block Party Dubs",
                durationMs = 224000L,
                uri = "https://ice5.somafm.com/beatblender-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cassette_mixtape,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "90s Hip-Hop",
                year = "1995",
                bitrateKbps = 256,
                sampleRateKhz = 44,
                folderPath = "Boombox"
            ),
            Song(
                id = "retro_demo_10",
                title = "Cyberpunk 1999 Matrix Terminal",
                artist = "Netrunner Zero",
                album = "Cybernetic Overdrive",
                durationMs = 264000L,
                uri = "https://ice5.somafm.com/defcon-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_app_icon,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "IDM / Glitch",
                year = "1999",
                bitrateKbps = 320,
                sampleRateKhz = 44,
                folderPath = "Cyberpunk"
            ),
            Song(
                id = "retro_demo_11",
                title = "16-Bit Super Arcade Battle",
                artist = "Pixel Master 93",
                album = "Quarter Muncher OST",
                durationMs = 188000L,
                uri = "https://relay.rainwave.cc/chiptune.mp3",
                albumArtUri = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cd_jewel_case,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "Chiptune / 16-Bit",
                year = "1993",
                bitrateKbps = 160,
                sampleRateKhz = 44,
                folderPath = "Arcade"
            ),
            Song(
                id = "retro_demo_12",
                title = "Goa Beach Sunrise Trance",
                artist = "Astral Tribe 97",
                album = "Psychedelic Anjuna",
                durationMs = 278000L,
                uri = "https://ice5.somafm.com/suburbsofgoa-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_app_icon,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "Goa Trance",
                year = "1997",
                bitrateKbps = 320,
                sampleRateKhz = 44,
                folderPath = "Rave Tapes"
            ),
            Song(
                id = "retro_demo_13",
                title = "French Touch Filter House 1998",
                artist = "Modjo & Stardust Knights",
                album = "Parisian Disco Filters",
                durationMs = 232000L,
                uri = "https://ice5.somafm.com/beatblender-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cd_jewel_case,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "Filter House",
                year = "1998",
                bitrateKbps = 256,
                sampleRateKhz = 44,
                folderPath = "Club Tapes"
            ),
            Song(
                id = "retro_demo_14",
                title = "High School Prom 1999 Slow Jam",
                artist = "R&B Millennium Harmony",
                album = "Class of '99 Anthems",
                durationMs = 248000L,
                uri = "https://ice5.somafm.com/lush-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cassette_mixtape,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "90s R&B / Slow Jam",
                year = "1999",
                bitrateKbps = 192,
                sampleRateKhz = 44,
                folderPath = "Slow Jams"
            ),
            Song(
                id = "retro_demo_15",
                title = "Ibiza Balearic Sunset Chill",
                artist = "Cafe Del Retro",
                album = "Sunset Strip Chillout",
                durationMs = 259000L,
                uri = "https://ice5.somafm.com/groovesalad-128-mp3",
                albumArtUri = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_app_icon,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "Balearic Chillout",
                year = "1999",
                bitrateKbps = 320,
                sampleRateKhz = 44,
                folderPath = "Chillout"
            ),
            Song(
                id = "retro_demo_16",
                title = "Y2K Trance Energy Millennium",
                artist = "DJ Gatecrasher 2000",
                album = "Superclub Euphoria",
                durationMs = 285000L,
                uri = "https://streams.90s90s.de/eurodance/mp3-192/",
                albumArtUri = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
                albumArtRes = R.drawable.img_cd_jewel_case,
                source = AudioSource.DEMO_JUKEBOX,
                genre = "Trance Energy",
                year = "2000",
                bitrateKbps = 320,
                sampleRateKhz = 44,
                folderPath = "Rave Tapes"
            )
        )
    }
}
