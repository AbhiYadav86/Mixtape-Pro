package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.PowerManager
import com.example.data.model.AudioSource
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RetroAudioPlayerEngine(
    private val context: Context,
    val visualizerEngine: VisualizerEngine,
    val equalizerController: RetroEqualizerController
) {
    private enum class PlayerState {
        IDLE,
        PREPARING,
        PREPARED,
        PLAYING,
        PAUSED,
        COMPLETED,
        ERROR
    }

    private var mediaPlayer: MediaPlayer? = null
    private var playerState = PlayerState.IDLE
    private var autoPlayWhenPrepared = true
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var prepareJob: Job? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val pcmSynthesizer = RetroPcmSynthesizer()

    init {
        RetroAudioPlaybackService.activeEngine = this
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RetroAmp99:BackgroundAudioPlayback")
        } catch (ignored: Exception) {}
    }

    // Playback state
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    /** Set when a stream or file can't be played (shown to the user, then cleared). */
    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()
    fun clearPlaybackError() { _playbackError.value = null }

    /** Stop and say so, instead of faking playback with the synth. */
    private fun failPlayback(song: Song, reason: String) {
        val old = mediaPlayer
        mediaPlayer = null
        safelyReleasePlayer(old)
        progressJob?.cancel()
        playerState = PlayerState.ERROR
        _isBuffering.value = false
        _isPlaying.value = false
        _currentPositionMs.value = 0L
        visualizerEngine.setPlaybackActive(false)
        releaseWakeLock()
        RetroAudioPlaybackService.startOrUpdateService(context, song, false)
        _playbackError.value = reason
    }

    private fun failureText(song: Song) = when (song.source) {
        AudioSource.RADIO_STREAM -> "\"${song.title}\" isn't broadcasting right now. Try another station."
        AudioSource.LOCAL -> "Couldn't play \"${song.title}\". The file may be missing or in an unsupported format."
        else -> "Couldn't connect to \"${song.title}\". Check your internet connection and try again."
    }

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    // 1. DJ Crossfade Settings (0s .. 12s)
    private val _crossfadeSeconds = MutableStateFlow(0)
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    // 2. Turntable Playback Speed & Pitch (0.5x .. 2.0x)
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playbackPitch = MutableStateFlow(1.0f)
    val playbackPitch: StateFlow<Float> = _playbackPitch.asStateFlow()

    // 3. Sleep Timer (Remaining seconds, null if disabled)
    private val _sleepTimerRemainingSecs = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSecs: StateFlow<Int?> = _sleepTimerRemainingSecs.asStateFlow()

    // 4. Real-time Lyrics
    private val _currentLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val currentLyrics: StateFlow<List<LyricLine>> = _currentLyrics.asStateFlow()

    private var currentIndex = -1

    // Shuffle: a random order over the queue; every track plays once before it reshuffles.
    private var shuffleOrder: MutableList<Int> = mutableListOf()
    private var shufflePos = 0

    private fun rebuildShuffle(first: Int) {
        val n = _queue.value.size
        val order = (0 until n).shuffled().toMutableList()
        if (first in 0 until n) { order.remove(first); order.add(0, first) }
        shuffleOrder = order
        shufflePos = 0
    }

    private fun shuffleValid() = shuffleOrder.size == _queue.value.size && shuffleOrder.isNotEmpty()
    var onSongPlayed: ((Song) -> Unit)? = null

    fun setCrossfadeSeconds(seconds: Int) {
        _crossfadeSeconds.value = seconds.coerceIn(0, 12)
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        _playbackSpeed.value = clamped
        applyPlaybackParams()
    }

    fun setPlaybackPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        _playbackPitch.value = clamped
        applyPlaybackParams()
    }

    fun resetPitchAndSpeed() {
        _playbackSpeed.value = 1.0f
        _playbackPitch.value = 1.0f
        applyPlaybackParams()
    }

    private fun applyPlaybackParams() {
        try {
            val mp = mediaPlayer
            if (mp != null && (playerState == PlayerState.PLAYING || playerState == PlayerState.PAUSED || playerState == PlayerState.PREPARED)) {
                val params = PlaybackParams()
                    .setSpeed(_playbackSpeed.value)
                    .setPitch(_playbackPitch.value)
                mp.playbackParams = params
            }
        } catch (ignored: Exception) {}
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingSecs.value = null
            return
        }

        val totalSecs = minutes * 60
        _sleepTimerRemainingSecs.value = totalSecs

        sleepTimerJob = scope.launch {
            var remaining = totalSecs
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                _sleepTimerRemainingSecs.value = remaining

                // Smooth fade out in the last 15 seconds
                if (remaining <= 15) {
                    val fadeRatio = (remaining.toFloat() / 15f).coerceIn(0f, 1f)
                    try {
                        mediaPlayer?.setVolume(fadeRatio, fadeRatio)
                    } catch (ignored: Exception) {}
                }
            }

            if (isActive) {
                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                stop()
                _sleepTimerRemainingSecs.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSecs.value = null
        try {
            mediaPlayer?.setVolume(1.0f, 1.0f)
        } catch (ignored: Exception) {}
    }

    fun setCustomLyrics(lrcText: String) {
        val duration = _durationMs.value.takeIf { it > 0 } ?: (_currentSong.value?.durationMs ?: 0L)
        _currentLyrics.value = LrcParser.parse(lrcText, duration)
    }

    /** Apply freshly fetched lyrics if [songId] is still the active track, and cache them in the queue. */
    fun updateSongLyrics(songId: String, lrcText: String) {
        _queue.value = _queue.value.map { if (it.id == songId) it.copy(lyrics = lrcText) else it }
        val current = _currentSong.value
        if (current != null && current.id == songId) {
            _currentSong.value = current.copy(lyrics = lrcText)
            setCustomLyrics(lrcText)
        }
    }

    /** Replace stale queue copies of a song with the latest database version (art, lyrics, tags). */
    fun refreshSongData(updated: Song) {
        _queue.value = _queue.value.map { if (it.id == updated.id) updated else it }
        val current = _currentSong.value
        if (current != null && current.id == updated.id) {
            _currentSong.value = updated
            if (!updated.lyrics.isNullOrBlank() && updated.lyrics != current.lyrics) {
                setCustomLyrics(updated.lyrics)
            }
        }
    }

    fun updateCurrentSongArtwork(songId: String, newArtUri: String) {
        val current = _currentSong.value
        if (current != null && current.id == songId) {
            _currentSong.value = current.copy(albumArtUri = newArtUri)
        }
        _queue.value = _queue.value.map {
            if (it.id == songId) it.copy(albumArtUri = newArtUri) else it
        }
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        _queue.value = songs
        if (_isShuffle.value) rebuildShuffle(startIndex)
        if (startIndex in songs.indices) {
            playSongAtIndex(startIndex)
        }
    }

    fun playSong(song: Song) {
        val currentQueue = _queue.value
        val index = currentQueue.indexOfFirst { it.id == song.id }
        if (index >= 0) {
            playSongAtIndex(index)
        } else {
            val newQueue = currentQueue + song
            _queue.value = newQueue
            playSongAtIndex(newQueue.size - 1)
        }
    }

    /**
     * Shuffle the whole [songs] list and keep playing tracks automatically in random order
     * (each once, then a new random order).
     */
    fun shufflePlay(songs: List<Song>) {
        if (songs.isEmpty()) return
        _isShuffle.value = true
        if (_repeatMode.value == RepeatMode.ONE) _repeatMode.value = RepeatMode.ALL
        _queue.value = songs
        val start = songs.indices.random()
        rebuildShuffle(start)
        playSongAtIndex(start)
    }

    /** Drop tracks (e.g. deleted ones) from the queue; skips ahead if the current one went. */
    fun removeFromQueue(ids: Set<String>) {
        val list = _queue.value
        if (list.none { it.id in ids }) return
        val currentId = _currentSong.value?.id
        val remaining = list.filter { it.id !in ids }
        _queue.value = remaining
        if (currentId != null && currentId in ids) {
            val oldIndex = currentIndex
            val wasPlaying = _isPlaying.value
            stop()
            _currentSong.value = null
            currentIndex = -1
            if (remaining.isNotEmpty() && wasPlaying) {
                val next = oldIndex.coerceIn(0, remaining.lastIndex)
                if (_isShuffle.value) rebuildShuffle(-1)
                playSongAtIndex(if (_isShuffle.value) shuffleOrder.first() else next)
            }
        } else {
            currentIndex = remaining.indexOfFirst { it.id == currentId }
            if (_isShuffle.value) rebuildShuffle(currentIndex)
        }
    }

    fun playSongAtIndex(index: Int) {
        val list = _queue.value
        if (index !in list.indices) return
        currentIndex = index
        if (_isShuffle.value) {
            if (!shuffleValid()) rebuildShuffle(index)
            val p = shuffleOrder.indexOf(index)
            if (p >= 0) shufflePos = p
        }
        val song = list[index]
        _currentSong.value = song
        _currentLyrics.value = when {
            !song.lyrics.isNullOrBlank() -> LrcParser.parse(song.lyrics, song.durationMs)
            // Built-in demo / synth tracks keep their novelty teleprompter text.
            song.source == AudioSource.DEMO_JUKEBOX || song.uri.startsWith("synth://") ->
                LrcParser.parse(LrcParser.getDemoLyricsForTitle(song.title, song.artist), song.durationMs)
            // Real songs: show nothing until real lyrics are found (never fake lyrics).
            else -> emptyList()
        }
        onSongPlayed?.invoke(song)

        // Cancel previous loading, synth, and progress tracking
        prepareJob?.cancel()
        progressJob?.cancel()
        pcmSynthesizer.stopSynthPlayback()

        // Safely dispose active player
        val oldPlayer = mediaPlayer
        mediaPlayer = null
        safelyReleasePlayer(oldPlayer)

        playerState = PlayerState.PREPARING
        autoPlayWhenPrepared = true
        _isBuffering.value = true
        _isPlaying.value = false
        _playbackError.value = null
        _currentPositionMs.value = 0L
        // Live radio has no length; showing a made-up 3:30 made stations look like songs
        _durationMs.value = if (song.durationMs > 0 && song.source != AudioSource.RADIO_STREAM) song.durationMs else 0L

        // If track is an offline demo, synthesizer URI, or invalid URL, fallback to real-time retro PCM synth immediately
        if (song.uri.isBlank() || song.uri.contains(".invalid") || song.uri.startsWith("synth://") || song.uri.startsWith("offline://")) {
            startSyntheticPlayback(song)
            return
        }

        prepareJob = scope.launch(Dispatchers.IO) {
            var mp: MediaPlayer? = null
            try {
                mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    try {
                        setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                    } catch (ignored: Exception) {}

                    if (song.uri.startsWith("http://") || song.uri.startsWith("https://")) {
                        setDataSource(song.uri)
                    } else {
                        setDataSource(context, Uri.parse(song.uri))
                    }

                    setOnPreparedListener { player ->
                        scope.launch(Dispatchers.Main) {
                            if (playerState != PlayerState.PREPARING || (mediaPlayer != null && mediaPlayer != player)) {
                                safelyReleasePlayer(player)
                                return@launch
                            }
                            playerState = PlayerState.PREPARED
                            _isBuffering.value = false
                            val dur = try { player.duration.toLong() } catch (e: Exception) { 0L }
                            _durationMs.value = when {
                                dur > 0 -> dur
                                song.source == AudioSource.RADIO_STREAM -> 0L
                                else -> song.durationMs
                            }

                            applyPlaybackParams()

                            if (autoPlayWhenPrepared) {
                                try {
                                    player.start()
                                    playerState = PlayerState.PLAYING
                                    _isPlaying.value = true
                                    acquireWakeLock()
                                    visualizerEngine.setPlaybackActive(true)
                                    RetroAudioPlaybackService.startOrUpdateService(context, song, true)
                                    startProgressTracker()
                                } catch (e: Exception) {
                                    safelyReleasePlayer(player)
                                    mediaPlayer = null
                                    failPlayback(song, failureText(song))
                                }
                            } else {
                                playerState = PlayerState.PAUSED
                                _isPlaying.value = false
                            }
                        }
                    }

                    setOnCompletionListener {
                        scope.launch(Dispatchers.Main) {
                            playerState = PlayerState.COMPLETED
                            handleSongCompletion()
                        }
                    }

                    setOnErrorListener { player, what, extra ->
                        scope.launch(Dispatchers.Main) {
                            safelyReleasePlayer(player)
                            if (mediaPlayer == player) {
                                mediaPlayer = null
                            }
                            failPlayback(song, failureText(song))
                        }
                        true
                    }

                    // A stream that stalls mid-play: show buffering and freeze the visualiser until audio returns
                    setOnInfoListener { _, what, _ ->
                        scope.launch(Dispatchers.Main) {
                            when (what) {
                                MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                                    _isBuffering.value = true
                                    visualizerEngine.setPlaybackActive(false)
                                }
                                MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                                    _isBuffering.value = false
                                    if (_isPlaying.value) visualizerEngine.setPlaybackActive(true)
                                }
                            }
                        }
                        true
                    }

                    prepareAsync()
                }

                withContext(Dispatchers.Main) {
                    if (playerState == PlayerState.PREPARING) {
                        mediaPlayer = mp
                        try {
                            visualizerEngine.attachToAudioSession(mp.audioSessionId, context)
                            equalizerController.attachToSession(mp.audioSessionId)
                        } catch (ignored: Exception) {}
                    } else {
                        safelyReleasePlayer(mp)
                    }
                }

                // Radio stations can take a while to connect: wait up to 20 s ("Tuning in…"), then give up
                // honestly. (Older builds switched to the built-in synth after 2.5 s, which looked like
                // playback with no sound.)
                delay(20_000L)
                if (isActive && playerState == PlayerState.PREPARING) {
                    withContext(Dispatchers.Main) {
                        if (playerState == PlayerState.PREPARING) {
                            failPlayback(song, failureText(song))
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    failPlayback(song, failureText(song))
                }
            }
        }
    }

    private fun startSyntheticPlayback(song: Song) {
        val oldPlayer = mediaPlayer
        mediaPlayer = null
        safelyReleasePlayer(oldPlayer)
        _durationMs.value = if (song.durationMs > 0) song.durationMs else 210000L
        _isPlaying.value = true
        _isBuffering.value = false
        playerState = PlayerState.PLAYING
        acquireWakeLock()
        pcmSynthesizer.startSynthPlayback(song.title, song.artist, song.genre)
        visualizerEngine.setPlaybackActive(true)
        RetroAudioPlaybackService.startOrUpdateService(context, song, true)
        startProgressTracker()
    }

    fun pause() {
        if (_isPlaying.value) {
            val song = _currentSong.value
            val mp = mediaPlayer
            if (mp != null) {
                try {
                    mp.pause()
                    playerState = PlayerState.PAUSED
                } catch (ignored: Exception) {}
            }
            pcmSynthesizer.stopSynthPlayback()
            _isPlaying.value = false
            visualizerEngine.setPlaybackActive(false)
            if (song != null) {
                RetroAudioPlaybackService.startOrUpdateService(context, song, false)
            }
        }
    }

    fun togglePlayPause() {
        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
        val song = _currentSong.value ?: run {
            if (_queue.value.isNotEmpty()) playSongAtIndex(0)
            return
        }

        when (playerState) {
            PlayerState.PREPARING -> {
                autoPlayWhenPrepared = !autoPlayWhenPrepared
                _isPlaying.value = autoPlayWhenPrepared
                visualizerEngine.setPlaybackActive(autoPlayWhenPrepared)
            }
            PlayerState.PLAYING -> {
                val mp = mediaPlayer
                if (mp != null) {
                    try {
                        mp.pause()
                        playerState = PlayerState.PAUSED
                    } catch (ignored: Exception) {}
                }
                pcmSynthesizer.stopSynthPlayback()
                _isPlaying.value = false
                visualizerEngine.setPlaybackActive(false)
                RetroAudioPlaybackService.startOrUpdateService(context, song, false)
            }
            PlayerState.PREPARED, PlayerState.PAUSED, PlayerState.COMPLETED -> {
                val mp = mediaPlayer
                if (mp != null) {
                    try {
                        mp.start()
                        playerState = PlayerState.PLAYING
                        _isPlaying.value = true
                        acquireWakeLock()
                        visualizerEngine.setPlaybackActive(true)
                        RetroAudioPlaybackService.startOrUpdateService(context, song, true)
                        startProgressTracker()
                    } catch (e: Exception) {
                        playerState = PlayerState.ERROR
                        startSyntheticPlayback(song)
                    }
                } else {
                    _isPlaying.value = true
                    playerState = PlayerState.PLAYING
                    acquireWakeLock()
                    pcmSynthesizer.startSynthPlayback(song.title, song.artist, song.genre)
                    visualizerEngine.setPlaybackActive(true)
                    RetroAudioPlaybackService.startOrUpdateService(context, song, true)
                    startProgressTracker()
                }
            }
            PlayerState.IDLE, PlayerState.ERROR -> {
                if (_isPlaying.value) {
                    _isPlaying.value = false
                    pcmSynthesizer.stopSynthPlayback()
                    visualizerEngine.setPlaybackActive(false)
                    RetroAudioPlaybackService.startOrUpdateService(context, song, false)
                } else {
                    if (mediaPlayer == null && currentIndex in _queue.value.indices) {
                        playSongAtIndex(currentIndex)
                    } else {
                        startSyntheticPlayback(song)
                    }
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs
        val mp = mediaPlayer
        if (mp != null && (playerState == PlayerState.PREPARED || playerState == PlayerState.PLAYING || playerState == PlayerState.PAUSED || playerState == PlayerState.COMPLETED)) {
            try {
                mp.seekTo(positionMs.toInt())
            } catch (ignored: Exception) {}
        }
    }

    fun nextTrack() {
        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
        val list = _queue.value
        if (list.isEmpty()) return

        if (_isShuffle.value && list.size > 1) {
            if (!shuffleValid()) rebuildShuffle(currentIndex)
            if (shufflePos + 1 >= shuffleOrder.size) {
                // every track has played: new random order, not starting with the one just heard
                val last = currentIndex
                rebuildShuffle(-1)
                if (shuffleOrder.first() == last && shuffleOrder.size > 1) {
                    shuffleOrder.removeAt(0); shuffleOrder.add(last)
                }
            } else {
                shufflePos++
            }
            playSongAtIndex(shuffleOrder[shufflePos])
        } else {
            val nextIndex = (currentIndex + 1) % list.size
            playSongAtIndex(nextIndex)
        }
    }

    fun previousTrack() {
        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
        val list = _queue.value
        if (list.isEmpty()) return

        if (_currentPositionMs.value > 3000L) {
            seekTo(0)
            return
        }

        if (_isShuffle.value && shuffleValid() && shufflePos > 0) {
            shufflePos--
            playSongAtIndex(shuffleOrder[shufflePos])
            return
        }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        playSongAtIndex(prevIndex)
    }

    fun stop() {
        prepareJob?.cancel()
        prepareJob = null
        progressJob?.cancel()
        progressJob = null
        pcmSynthesizer.stopSynthPlayback()

        val mp = mediaPlayer
        mediaPlayer = null
        playerState = PlayerState.IDLE

        safelyReleasePlayer(mp)

        _isPlaying.value = false
        _isBuffering.value = false
        _currentPositionMs.value = 0L
        visualizerEngine.setPlaybackActive(false)
        releaseWakeLock()
        RetroAudioPlaybackService.stopService(context)
    }

    fun toggleRepeat() {
        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
        _isShuffle.value = !_isShuffle.value
        if (_isShuffle.value) rebuildShuffle(currentIndex)
    }

    private fun handleSongCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                val mp = mediaPlayer
                if (mp != null && (playerState == PlayerState.COMPLETED || playerState == PlayerState.PLAYING || playerState == PlayerState.PAUSED)) {
                    try {
                        mp.seekTo(0)
                        mp.start()
                        playerState = PlayerState.PLAYING
                        _isPlaying.value = true
                        visualizerEngine.setPlaybackActive(true)
                        startProgressTracker()
                    } catch (e: Exception) {
                        playSongAtIndex(currentIndex)
                    }
                } else {
                    _currentPositionMs.value = 0L
                    _isPlaying.value = true
                    visualizerEngine.setPlaybackActive(true)
                    startProgressTracker()
                }
            }
            RepeatMode.ALL -> {
                nextTrack()
            }
            RepeatMode.OFF -> {
                val more = if (_isShuffle.value && shuffleValid()) shufflePos < shuffleOrder.size - 1 else currentIndex < _queue.value.size - 1
                if (more) {
                    nextTrack()
                } else {
                    stop()
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                val mp = mediaPlayer
                val cfSecs = _crossfadeSeconds.value
                if (mp != null && playerState == PlayerState.PLAYING) {
                    try {
                        if (mp.isPlaying) {
                            val pos = mp.currentPosition.toLong()
                            val dur = mp.duration.toLong()
                            _currentPositionMs.value = pos
                            if (dur > 0) _durationMs.value = dur

                            // Handle DJ crossfade volume automation
                            if (cfSecs > 0 && dur > 10000L) {
                                val remainingMs = dur - pos
                                val cfMs = cfSecs * 1000L
                                if (remainingMs in 0..cfMs) {
                                    val vol = (remainingMs.toFloat() / cfMs.toFloat()).coerceIn(0.05f, 1f)
                                    mp.setVolume(vol, vol)
                                } else if (pos < cfMs) {
                                    val vol = (pos.toFloat() / cfMs.toFloat()).coerceIn(0.05f, 1f)
                                    mp.setVolume(vol, vol)
                                } else {
                                    mp.setVolume(1.0f, 1.0f)
                                }
                            } else {
                                mp.setVolume(1.0f, 1.0f)
                            }
                        }
                    } catch (ignored: Exception) {}
                } else if (mediaPlayer == null) {
                    // Synthetic progress increment
                    val newPos = _currentPositionMs.value + 500L
                    if (_durationMs.value > 0 && newPos >= _durationMs.value) {
                        handleSongCompletion()
                    } else {
                        _currentPositionMs.value = newPos
                    }
                }
                delay(400)
            }
        }
    }

    private fun safelyReleasePlayer(player: MediaPlayer?) {
        if (player == null) return
        try {
            player.setOnPreparedListener(null)
            player.setOnCompletionListener(null)
            player.setOnErrorListener(null)
            player.setOnBufferingUpdateListener(null)
            player.reset()
            player.release()
        } catch (ignored: Exception) {}
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == false) wakeLock?.acquire(120 * 60 * 1000L)
        } catch (ignored: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (ignored: Exception) {}
    }

    fun release() {
        stop()
        pcmSynthesizer.stopSynthPlayback()
        visualizerEngine.release()
        equalizerController.release()
    }
}
