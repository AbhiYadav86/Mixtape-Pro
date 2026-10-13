package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.LyricLine
import com.example.audio.RetroAudioPlayerEngine
import com.example.audio.RetroEqualizerController
import com.example.audio.StudioRenderEngine
import com.example.audio.VisualizerEngine
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferenceEntity
import com.example.data.model.*
import com.example.data.repository.MusicRepository
import com.example.ui.skins.RetroSkinDefinitions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val appDao = db.appDao()
    val repository = MusicRepository(application, appDao)

    val visualizerEngine = VisualizerEngine()
    val equalizerController = RetroEqualizerController()
    val playerEngine = RetroAudioPlayerEngine(application, visualizerEngine, equalizerController)

    // Current Retro Skin Theme
    private val _currentSkin = MutableStateFlow<RetroSkin>(RetroSkinDefinitions.WINAMP_CLASSIC)
    val currentSkin: StateFlow<RetroSkin> = _currentSkin.asStateFlow()

    // Visualizer and Media Display mode
    private val _visualizerType = MutableStateFlow(VisualizerType.CLASSIC_BARS_PEAK)
    val visualizerType: StateFlow<VisualizerType> = _visualizerType.asStateFlow()

    // Lyrics overlay over live visualizer
    private val _isLyricsOverlayActive = MutableStateFlow(false)
    val isLyricsOverlayActive: StateFlow<Boolean> = _isLyricsOverlayActive.asStateFlow()

    private val _isFetchingLyrics = MutableStateFlow(false)
    val isFetchingLyrics: StateFlow<Boolean> = _isFetchingLyrics.asStateFlow()

    val currentLyrics: StateFlow<List<LyricLine>> = playerEngine.currentLyrics

    private val _discDisplayMode = MutableStateFlow(DiscDisplayMode.CRT_MONITOR)
    val discDisplayMode: StateFlow<DiscDisplayMode> = _discDisplayMode.asStateFlow()
    private var userExplicitlySelectedDiscMode = false

    private fun hasValidAlbumArt(song: Song?): Boolean {
        if (song == null) return false
        val hasUri = !song.albumArtUri.isNullOrBlank() && !song.albumArtUri.contains("unsplash", ignoreCase = true)
        val hasRes = song.albumArtRes != null && song.albumArtRes != 0 && song.albumArtRes != com.example.R.drawable.img_cd_jewel_case
        return hasUri || hasRes
    }

    // Player UI Screen Fit & Button Scaling
    private val _playerUiScale = MutableStateFlow(PlayerUiScale.STANDARD)
    val playerUiScale: StateFlow<PlayerUiScale> = _playerUiScale.asStateFlow()

    private val _playerScaleFactor = MutableStateFlow(1.0f)
    val playerScaleFactor: StateFlow<Float> = _playerScaleFactor.asStateFlow()

    // Database Songs & Playlists
    val allSongs: StateFlow<List<Song>> = repository.allSongs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    /** The user's own music: device files, imports, cloud tracks and saved streams (no built-in demo tracks). */
    val librarySongs: StateFlow<List<Song>> = repository.allSongs
        .map { list -> list.filter { repository.isEnrichable(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Automatically show lyrics in the spectrum area whenever a track has them
    private val _autoShowLyrics = MutableStateFlow(true)
    val autoShowLyrics: StateFlow<Boolean> = _autoShowLyrics.asStateFlow()

    // Background library art/lyrics enrichment progress (null when idle)
    private val _enrichmentProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val enrichmentProgress: StateFlow<Pair<Int, Int>?> = _enrichmentProgress.asStateFlow()
    private var enrichmentJob: Job? = null
    private var scanJob: Job? = null

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylists.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Selected Playlist & Playlist songs
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _playlistSongs = MutableStateFlow<List<Song>>(emptyList())
    val playlistSongs: StateFlow<List<Song>> = _playlistSongs.asStateFlow()

    // Online Radio & Search
    private val _radioStations = MutableStateFlow<List<Song>>(emptyList())
    val radioStations: StateFlow<List<Song>> = _radioStations.asStateFlow()

    // Radio Station Live Status map (stationId -> RadioStationStatus)
    private val _stationStatusMap = MutableStateFlow<Map<String, RadioStationStatus>>(emptyMap())
    val stationStatusMap: StateFlow<Map<String, RadioStationStatus>> = _stationStatusMap.asStateFlow()

    private val _isScanningFrequencies = MutableStateFlow(false)
    val isScanningFrequencies: StateFlow<Boolean> = _isScanningFrequencies.asStateFlow()

    private val _onlineSearchResults = MutableStateFlow<List<Song>>(emptyList())
    val onlineSearchResults: StateFlow<List<Song>> = _onlineSearchResults.asStateFlow()

    // Status message toast banner
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        playerEngine.onSongPlayed = { song ->
            viewModelScope.launch {
                repository.incrementPlayCount(song.id)

                // The queue may hold an old copy of the song; reload art/lyrics from the database.
                val fresh = repository.getFreshSong(song.id) ?: song
                if (fresh != song) playerEngine.refreshSongData(fresh)

                if (repository.isEnrichable(fresh) && (repository.needsArtwork(fresh) || fresh.lyrics.isNullOrBlank())) {
                    if (fresh.lyrics.isNullOrBlank()) _isFetchingLyrics.value = true
                    val (newArt, newLyrics) = withContext(Dispatchers.IO) { repository.enrichSongMetadata(fresh) }
                    _isFetchingLyrics.value = false
                    if (!newLyrics.isNullOrBlank()) {
                        playerEngine.updateSongLyrics(fresh.id, newLyrics)
                    }
                    if (!newArt.isNullOrBlank()) {
                        playerEngine.updateCurrentSongArtwork(fresh.id, newArt)
                    }
                }
            }
        }

        // Pick the best media deck for each track (album art when we have it, CD otherwise)
        viewModelScope.launch {
            playerEngine.currentSong
                .map { it?.id to hasValidAlbumArt(it) }
                .distinctUntilChanged()
                .collect { (id, hasArt) ->
                    if (id != null && !userExplicitlySelectedDiscMode) {
                        _discDisplayMode.value = if (hasArt) DiscDisplayMode.CRT_MONITOR else DiscDisplayMode.CD_JEWEL_CASE
                    }
                }
        }

        // Auto-show lyrics where the spectrum analyzer is, as soon as a track has them
        viewModelScope.launch {
            combine(
                playerEngine.currentSong.map { it?.id }.distinctUntilChanged(),
                playerEngine.currentLyrics.map { it.isNotEmpty() }.distinctUntilChanged(),
                _autoShowLyrics
            ) { id, hasLyrics, auto -> Triple(id, hasLyrics, auto) }
                .collect { (id, hasLyrics, auto) ->
                    if (id != null && auto) {
                        _isLyricsOverlayActive.value = hasLyrics
                    }
                }
        }

        // Load initial data and preferences
        viewModelScope.launch {
            val stations = repository.getRetroRadioStations()
            _radioStations.value = stations
            _onlineSearchResults.value = repository.searchOnlineRetroSongs("")

            // Initialize station status map with frequency mapping
            val initialStatusMap = stations.associate { station ->
                val isOfflineDummy = station.id == "radio_pirate_cassette_standby" || station.uri.contains("offline-archive.invalid") || station.uri.startsWith("synth://")
                station.id to RadioStationStatus(
                    stationId = station.id,
                    state = if (isOfflineDummy) StationLiveState.OFFLINE else StationLiveState.LIVE,
                    pingMs = if (isOfflineDummy) 0L else 42L,
                    signalPercent = if (isOfflineDummy) 0 else 96,
                    listenerCount = if (isOfflineDummy) 0 else repository.getEstimatedListenersForStation(station.id),
                    frequencyMhz = repository.getFrequencyForStation(station.id),
                    bitrateStr = if (isOfflineDummy) "OFFLINE ARCHIVE" else "${station.bitrateKbps} kbps MP3",
                    isLiveStream = !isOfflineDummy,
                    lastChecked = System.currentTimeMillis()
                )
            }
            _stationStatusMap.value = initialStatusMap

            // Load saved skin preference
            appDao.getPreference("selected_skin_id")?.let { skinId ->
                _currentSkin.value = RetroSkinDefinitions.getSkinById(skinId)
            }

            appDao.getPreference("auto_show_lyrics")?.let { _autoShowLyrics.value = it != "false" }

            // Load saved player UI scale preference
            appDao.getPreference("player_ui_scale_id")?.let { scaleId ->
                val scale = PlayerUiScale.fromId(scaleId)
                _playerUiScale.value = scale
            }
            appDao.getPreference("player_ui_scale_factor")?.toFloatOrNull()?.let { factor ->
                _playerScaleFactor.value = factor.coerceIn(0.75f, 1.25f)
            }

            // Ensure curated vintage mixtapes are seeded
            repository.seedAllCuratedMixtapes(force = false)

            // Start live station ping scan
            scanRadioStationsLiveStatus()
        }
    }

    fun scanRadioStationsLiveStatus(force: Boolean = false) {
        if (_isScanningFrequencies.value && !force) return
        viewModelScope.launch {
            _isScanningFrequencies.value = true
            val stations = _radioStations.value

            // Set all to checking state
            val currentMap = _stationStatusMap.value.toMutableMap()
            stations.forEach { station ->
                val prev = currentMap[station.id]
                if (prev != null) {
                    currentMap[station.id] = prev.copy(state = StationLiveState.CHECKING)
                }
            }
            _stationStatusMap.value = currentMap

            // Parallel async checks with coroutines
            kotlinx.coroutines.coroutineScope {
                stations.map { station ->
                    async(kotlinx.coroutines.Dispatchers.IO) {
                        val status = repository.checkStationLiveStatus(station)
                        _stationStatusMap.update { map ->
                            map + (station.id to status)
                        }
                    }
                }.forEach { it.await() }
            }

            _isScanningFrequencies.value = false
        }
    }

    fun recheckSingleStation(station: Song) {
        viewModelScope.launch {
            _stationStatusMap.update { map ->
                val prev = map[station.id]
                if (prev != null) {
                    map + (station.id to prev.copy(state = StationLiveState.CHECKING))
                } else map
            }
            val status = repository.checkStationLiveStatus(station)
            _stationStatusMap.update { map ->
                map + (station.id to status)
            }
        }
    }

    fun restoreCuratedMixtapes() {
        viewModelScope.launch {
            repository.seedAllCuratedMixtapes(force = true)
            _statusMessage.value = "Restored all 8 Curated Vintage 90s Mixtapes!"
        }
    }

    fun setSkin(skin: RetroSkin) {
        _currentSkin.value = skin
        viewModelScope.launch {
            appDao.setPreference(AppPreferenceEntity("selected_skin_id", skin.id))
        }
    }

    fun setPlayerUiScale(scale: PlayerUiScale) {
        _playerUiScale.value = scale
        _playerScaleFactor.value = scale.factor
        viewModelScope.launch {
            appDao.setPreference(AppPreferenceEntity("player_ui_scale_id", scale.id))
            appDao.setPreference(AppPreferenceEntity("player_ui_scale_factor", scale.factor.toString()))
        }
    }

    fun setPlayerScaleFactor(factor: Float) {
        val clamped = factor.coerceIn(0.75f, 1.25f)
        _playerScaleFactor.value = clamped
        viewModelScope.launch {
            appDao.setPreference(AppPreferenceEntity("player_ui_scale_factor", clamped.toString()))
        }
    }

    fun setVisualizerType(type: VisualizerType) {
        _visualizerType.value = type
    }

    fun toggleLyricsOverlay() {
        _isLyricsOverlayActive.value = !_isLyricsOverlayActive.value
    }

    fun setAutoShowLyrics(enabled: Boolean) {
        _autoShowLyrics.value = enabled
        viewModelScope.launch { appDao.setPreference(AppPreferenceEntity("auto_show_lyrics", enabled.toString())) }
    }

    /** Fill in missing album art & lyrics for the whole library (runs quietly in the background). */
    fun startLibraryEnrichment() {
        if (enrichmentJob?.isActive == true) return
        enrichmentJob = viewModelScope.launch {
            val updated = repository.enrichLibraryInBackground { done, total, updatedSong ->
                _enrichmentProgress.value = done to total
                if (updatedSong != null) {
                    playerEngine.refreshSongData(updatedSong)
                }
            }
            _enrichmentProgress.value = null
            if (updated > 0) {
                _statusMessage.value = "Found album art / lyrics for $updated track(s)"
            }
        }
    }

    fun setLyricsOverlayActive(active: Boolean) {
        _isLyricsOverlayActive.value = active
    }

    private val artRequestedIds = mutableSetOf<String>()

    /** Called by the player when it has no usable artwork to show: look it up online once per session. */
    fun requestArtworkIfMissing(song: Song) {
        if (!repository.isEnrichable(song) || !artRequestedIds.add(song.id)) return
        viewModelScope.launch {
            val art = withContext(Dispatchers.IO) { repository.fetchAndCacheItunesArt(song) }
            if (art != null) {
                playerEngine.updateCurrentSongArtwork(song.id, art)
            } else {
                _statusMessage.value = "No album art found online for \"${song.title}\""
            }
        }
    }

    fun fetchOnlineArtworkForCurrentSong() {
        val song = playerEngine.currentSong.value ?: return
        viewModelScope.launch {
            _statusMessage.value = "Searching for album art..."
            val art = repository.fetchAndCacheItunesArt(song)
            if (art != null) {
                playerEngine.updateCurrentSongArtwork(song.id, art)
                _statusMessage.value = "Album art updated!"
            } else {
                _statusMessage.value = "No album art found for \"${song.title}\""
            }
        }
    }

    fun fetchOnlineLyricsForCurrentSong() {
        val song = playerEngine.currentSong.value ?: return
        viewModelScope.launch {
            _isFetchingLyrics.value = true
            _statusMessage.value = "Searching for lyrics..."
            val lyrics = repository.fetchAndCacheLrclibLyrics(song)
            _isFetchingLyrics.value = false
            if (!lyrics.isNullOrBlank()) {
                playerEngine.updateSongLyrics(song.id, lyrics)
                _isLyricsOverlayActive.value = true
                _statusMessage.value = "Lyrics loaded for \"${song.title}\""
            } else {
                _statusMessage.value = "No lyrics found for \"${song.title}\""
            }
        }
    }

    fun enrichSongMetadata(song: Song) {
        viewModelScope.launch {
            _statusMessage.value = "Enriching metadata for \"${song.title}\"..."
            val (art, lyrics) = repository.enrichSongMetadata(song)
            if (art != null) {
                playerEngine.updateCurrentSongArtwork(song.id, art)
            }
            if (!lyrics.isNullOrBlank()) {
                playerEngine.updateSongLyrics(song.id, lyrics)
            }
            if (art != null || lyrics != null) {
                _statusMessage.value = "Updated artwork/lyrics for \"${song.title}\"!"
            } else {
                _statusMessage.value = "No online metadata found for \"${song.title}\""
            }
        }
    }

    fun seekToPosition(positionMs: Long) {
        playerEngine.seekTo(positionMs)
    }

    fun setDiscDisplayMode(mode: DiscDisplayMode) {
        userExplicitlySelectedDiscMode = true
        _discDisplayMode.value = mode
    }

    fun setPlaybackSpeed(speed: Float) {
        playerEngine.setPlaybackSpeed(speed)
    }

    fun setPlaybackPitch(pitch: Float) {
        playerEngine.setPlaybackPitch(pitch)
    }

    fun resetPitchAndSpeed() {
        playerEngine.resetPitchAndSpeed()
    }

    fun setCrossfadeSeconds(seconds: Int) {
        playerEngine.setCrossfadeSeconds(seconds)
    }

    fun setSleepTimer(minutes: Int) {
        playerEngine.setSleepTimer(minutes)
        if (minutes > 0) {
            _statusMessage.value = "Sleep timer set for $minutes min (Auto-stops with cassette clack)"
        } else {
            _statusMessage.value = "Sleep timer cancelled"
        }
    }

    fun cancelSleepTimer() {
        playerEngine.cancelSleepTimer()
        _statusMessage.value = "Sleep timer cancelled"
    }

    fun updateSongMetadata(songId: String, title: String, artist: String, album: String, year: String, genre: String) {
        viewModelScope.launch {
            repository.updateSongMetadata(songId, title, artist, album, year, genre)
            _statusMessage.value = "Updated ID3 tags for \"$title\""
        }
    }

    fun updateSongLyrics(songId: String, lyrics: String) {
        viewModelScope.launch {
            repository.updateSongLyrics(songId, lyrics)
            playerEngine.updateSongLyrics(songId, lyrics)
            _statusMessage.value = "Updated lyrics!"
        }
    }

    fun playSmartPlaylist(type: String) {
        val songs = allSongs.value
        if (songs.isEmpty()) return

        val smartQueue = when (type) {
            "TOP_RATED" -> songs.filter { it.isFavorite }.ifEmpty { songs.take(10) }
            "SYNTH_90S" -> songs.filter { 
                it.genre.contains("Synth", ignoreCase = true) ||
                it.genre.contains("Eurodance", ignoreCase = true) ||
                it.genre.contains("Electronic", ignoreCase = true) ||
                it.genre.contains("Techno", ignoreCase = true)
            }.ifEmpty { songs }
            "MOST_PLAYED" -> songs.sortedByDescending { it.playCount }.take(15)
            "RECENT_IMPORTS" -> songs.sortedByDescending { it.addedTimestamp }.take(15)
            "SHUFFLE_ALL" -> songs.shuffled()
            else -> songs
        }

        if (smartQueue.isNotEmpty()) {
            playerEngine.setQueue(smartQueue, 0)
            _statusMessage.value = "Loaded Smart Playlist (${smartQueue.size} tracks)"
        }
    }

    fun playSong(song: Song) {
        playerEngine.playSong(song)
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        playerEngine.setQueue(songs, startIndex)
    }

    fun togglePlayPause() {
        playerEngine.togglePlayPause()
    }

    fun nextTrack() {
        playerEngine.nextTrack()
    }

    fun previousTrack() {
        playerEngine.previousTrack()
    }

    fun stopPlayback() {
        playerEngine.stop()
    }

    fun seekTo(positionMs: Long) {
        playerEngine.seekTo(positionMs)
    }

    fun toggleRepeat() {
        playerEngine.toggleRepeat()
    }

    fun toggleShuffle() {
        playerEngine.toggleShuffle()
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song)
        }
    }

    fun syncLocalFolders() {
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            _statusMessage.value = "Scanning this device for music..."
            val count = repository.scanLocalAudioFiles()
            _statusMessage.value = if (count > 0) "Found $count songs on this device" else "No music files found on this device"
            startLibraryEnrichment()
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        _selectedPlaylist.value = playlist
        viewModelScope.launch {
            repository.getSongsForPlaylist(playlist.id).collect {
                _playlistSongs.value = it
            }
        }
    }

    fun playPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.getSongsForPlaylist(playlist.id).firstOrNull()?.let { songs ->
                if (songs.isNotEmpty()) {
                    playerEngine.setQueue(songs, 0)
                }
            }
        }
    }

    fun createPlaylist(name: String, description: String) {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
            _statusMessage.value = "Mixtape \"$name\" recorded!"
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist.id)
            if (_selectedPlaylist.value?.id == playlist.id) {
                _selectedPlaylist.value = null
                _playlistSongs.value = emptyList()
            }
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, song)
            _statusMessage.value = "Added \"${song.title}\" to playlist!"
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun searchOnline(query: String) {
        _onlineSearchResults.value = repository.searchOnlineRetroSongs(query)
    }

    fun saveOnlineSongToLibrary(song: Song) {
        viewModelScope.launch {
            appDao.insertSong(com.example.data.local.SongEntity.fromSong(song))
            _statusMessage.value = "Saved \"${song.title}\" to Library!"
        }
    }

    fun pausePlayback() {
        playerEngine.pause()
    }

    fun saveMixedMixtapeTrack(
        title: String,
        artist: String,
        genre: String,
        durationSec: Int,
        sourceUri: String? = null,
        albumArtUri: String? = null,
        formatName: String = "Studio Master WAV",
        targetPlaylist: String = "DJ Studio Mixtapes '99",
        onExportComplete: ((Song) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val finalUri = if (!sourceUri.isNullOrBlank()) {
                sourceUri
            } else {
                "https://archive.org/download/rave_90s_anthems/dj_mix_preview.mp3"
            }

            val mixedSong = Song(
                id = "mix_${System.currentTimeMillis()}",
                title = title.ifBlank { "Untitled Studio Mix" },
                artist = artist.ifBlank { "DJ Studio" },
                album = targetPlaylist,
                genre = genre,
                year = "1999",
                durationMs = (durationSec * 1000L).coerceAtLeast(30000L),
                source = AudioSource.LOCAL,
                uri = finalUri,
                albumArtUri = albumArtUri,
                isFavorite = true
            )
            appDao.insertSong(com.example.data.local.SongEntity.fromSong(mixedSong))

            try {
                val playlists = appDao.getAllPlaylistsDirect()
                var playlist = playlists.find { it.name.equals(targetPlaylist, ignoreCase = true) }
                if (playlist == null) {
                    val newPlaylist = com.example.data.local.PlaylistEntity(
                        id = "playlist_dj_mixtapes",
                        name = targetPlaylist,
                        description = "Studio spliced and mixed master tracks",
                        coverRes = null,
                        createdAt = System.currentTimeMillis(),
                        iconName = "cassette"
                    )
                    appDao.insertPlaylist(newPlaylist)
                    playlist = newPlaylist
                }
                appDao.addSongToPlaylist(
                    com.example.data.local.PlaylistSongCrossRef(
                        playlistId = playlist.id,
                        songId = mixedSong.id,
                        orderIndex = 999
                    )
                )
            } catch (ignored: Exception) {}

            _statusMessage.value = "Exported \"${mixedSong.title}\" to Library & $targetPlaylist!"
            onExportComplete?.invoke(mixedSong)
        }
    }

    // =============================================================================================
    // DJ STUDIO — real audio rendering & export
    // =============================================================================================

    data class StudioExportState(
        val isRunning: Boolean = false,
        val label: String = "",
        val progress: Float = 0f,
        val lastExportUri: String? = null,
        val lastExportName: String? = null,
        val lastExportSong: Song? = null,
        val isRingtone: Boolean = false,
        val error: String? = null
    )

    private val _studioExport = MutableStateFlow(StudioExportState())
    val studioExport: StateFlow<StudioExportState> = _studioExport.asStateFlow()
    private var studioJob: Job? = null

    fun clearStudioExportMessage() {
        _studioExport.update { it.copy(error = null, lastExportUri = null, lastExportName = null, lastExportSong = null) }
    }

    private fun runStudioRender(
        label: String,
        title: String,
        artist: String,
        artUri: String?,
        targetPlaylist: String?,
        isRingtone: Boolean,
        render: ((Float) -> Unit) -> StudioRenderEngine.RenderResult
    ) {
        if (studioJob?.isActive == true) {
            _statusMessage.value = "Studio is still exporting — please wait"
            return
        }
        studioJob = viewModelScope.launch {
            _studioExport.value = StudioExportState(isRunning = true, label = label, isRingtone = isRingtone)
            try {
                val result = withContext(Dispatchers.IO) {
                    render { p -> _studioExport.update { it.copy(progress = p) } }
                }
                var savedSong: Song? = null
                if (!isRingtone) {
                    val mediaId = runCatching { android.content.ContentUris.parseId(result.uri) }.getOrDefault(-1L)
                    val song = Song(
                        // Same id the device scan will give it, so the file is never listed twice
                        id = if (mediaId > 0 && result.uri.scheme == "content") "local_$mediaId" else "mix_${System.currentTimeMillis()}",
                        title = title,
                        artist = artist,
                        album = "Mixtape Pro Studio",
                        genre = "Studio Mix",
                        year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString(),
                        durationMs = result.durationMs,
                        source = AudioSource.LOCAL,
                        uri = result.uri.toString(),
                        albumArtUri = artUri,
                        bitrateKbps = if (result.format == StudioRenderEngine.OutputFormat.WAV) 1411 else 192,
                        folderPath = "Mixtape Pro",
                        isFavorite = true
                    )
                    appDao.insertSong(com.example.data.local.SongEntity.fromSong(song))
                    if (!targetPlaylist.isNullOrBlank()) addToNamedPlaylist(targetPlaylist, song)
                    savedSong = song
                }
                _studioExport.value = StudioExportState(
                    isRunning = false,
                    label = label,
                    progress = 1f,
                    lastExportUri = result.uri.toString(),
                    lastExportName = result.displayName,
                    lastExportSong = savedSong,
                    isRingtone = isRingtone
                )
                _statusMessage.value = if (isRingtone) {
                    "Ringtone saved to Ringtones/Mixtape Pro"
                } else {
                    "Exported \"${result.displayName}\" to Music/Mixtape Pro"
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Export failed"
                _studioExport.value = StudioExportState(isRunning = false, label = label, error = msg, isRingtone = isRingtone)
                _statusMessage.value = msg
            }
        }
    }

    private suspend fun addToNamedPlaylist(name: String, song: Song) {
        try {
            val playlists = appDao.getAllPlaylistsDirect()
            var playlist = playlists.find { it.name.equals(name, ignoreCase = true) }
            if (playlist == null) {
                val created = com.example.data.local.PlaylistEntity(
                    id = "playlist_" + name.lowercase().replace(Regex("[^a-z0-9]+"), "_"),
                    name = name,
                    description = "Mixes and edits made in DJ Studio",
                    coverRes = com.example.R.drawable.img_cassette_mixtape,
                    createdAt = System.currentTimeMillis(),
                    iconName = "cassette"
                )
                appDao.insertPlaylist(created)
                playlist = created
            }
            val order = appDao.getPlaylistSongCount(playlist.id)
            appDao.addSongToPlaylist(com.example.data.local.PlaylistSongCrossRef(playlist.id, song.id, order))
        } catch (_: Exception) {}
    }

    /** Splice / DJ-mix several sources into one real audio file. */
    fun exportStudioMix(
        parts: List<StudioRenderEngine.RenderPart>,
        crossfadeMs: Long,
        title: String,
        artist: String,
        format: StudioRenderEngine.OutputFormat,
        targetPlaylist: String?,
        artUri: String?
    ) {
        val context = getApplication<Application>()
        val finalTitle = title.ifBlank { "Studio Mix" }
        runStudioRender("Rendering mix", finalTitle, artist.ifBlank { "DJ Studio" }, artUri, targetPlaylist, isRingtone = false) { progress ->
            StudioRenderEngine.renderSplice(context, parts, crossfadeMs, finalTitle, format, progress)
        }
    }

    fun exportRingtone(song: Song, startMs: Long, endMs: Long, fadeInMs: Long, fadeOutMs: Long, title: String) {
        val context = getApplication<Application>()
        val finalTitle = title.ifBlank { "${song.title} (Ringtone)" }
        runStudioRender("Cutting ringtone", finalTitle, song.artist, song.albumArtUri, null, isRingtone = true) { progress ->
            StudioRenderEngine.renderRingtone(context, song.uri, startMs, endMs, fadeInMs, fadeOutMs, finalTitle, progress)
        }
    }

    fun exportVocalRemoved(song: Song, strength: Float, bassKeepHz: Float, format: StudioRenderEngine.OutputFormat) {
        val context = getApplication<Application>()
        val finalTitle = "${song.title} (Instrumental)"
        runStudioRender("Removing vocals", finalTitle, song.artist, song.albumArtUri, "DJ Studio Mixtapes", isRingtone = false) { progress ->
            StudioRenderEngine.renderVocalRemoval(context, song.uri, strength, bassKeepHz, finalTitle, format, progress)
        }
    }

    fun importGoogleDriveLink(link: String, title: String, artist: String) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Syncing Google Drive link..."
                val song = repository.importGoogleDriveFile(link, title, artist)
                _statusMessage.value = "Imported \"${song.title}\" from Google Drive!"
                playerEngine.playSong(song)
            } catch (e: Exception) {
                _statusMessage.value = "Failed to sync Drive link: ${e.localizedMessage}"
            }
        }
    }

    fun importCloudStorageFile(url: String, title: String, artist: String, genre: String = "Cloud Stream", album: String = "Cloud Storage") {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Connecting to Cloud Audio Stream..."
                val song = repository.importCloudStorageFile(url, title, artist, genre, album)
                _statusMessage.value = "Added \"${song.title}\" from Cloud Storage!"
                playerEngine.playSong(song)
            } catch (e: Exception) {
                _statusMessage.value = "Failed to import Cloud URL: ${e.localizedMessage}"
            }
        }
    }

    fun importLocalAudioUri(uri: android.net.Uri, displayName: String? = null) {
        viewModelScope.launch {
            try {
                val song = repository.importLocalAudioUri(uri, displayName)
                _statusMessage.value = "Imported local audio: \"${song.title}\""
                playerEngine.playSong(song)
            } catch (e: Exception) {
                _statusMessage.value = "Failed to import file: ${e.localizedMessage}"
            }
        }
    }

    fun importMultipleLocalAudioUris(uris: List<android.net.Uri>) {
        viewModelScope.launch {
            var count = 0
            for (uri in uris) {
                try {
                    repository.importLocalAudioUri(uri)
                    count++
                } catch (_: Exception) {}
            }
            _statusMessage.value = "Imported $count local audio file(s) to Library!"
            startLibraryEnrichment()
        }
    }

    fun showStatus(message: String) {
        _statusMessage.value = message
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        playerEngine.release()
    }
}
