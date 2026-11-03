package com.example.audio

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.MainActivity
import com.example.R
import com.example.data.model.AudioSource
import com.example.data.model.BuiltInArt
import com.example.data.model.DiscDisplayMode
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RetroAudioPlaybackService : Service() {

    companion object {
        const val CHANNEL_ID = "retro_playback_channel"
        const val NOTIFICATION_ID = 1999

        const val ACTION_START_OR_UPDATE = "com.example.action.START_OR_UPDATE"
        const val ACTION_PLAY_PAUSE = "com.example.action.PLAY_PAUSE"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PREV = "com.example.action.PREV"
        const val ACTION_PREVIOUS = "com.example.action.PREV"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_VOLUME_UP = "com.example.action.VOLUME_UP"
        const val ACTION_VOLUME_DOWN = "com.example.action.VOLUME_DOWN"
        const val ACTION_REFRESH_ART = "com.example.action.REFRESH_ART"

        var activeEngine: RetroAudioPlayerEngine? = null

        /** What the player's deck is showing; the lock screen / media controls show the same picture. */
        @Volatile var artMode: DiscDisplayMode = DiscDisplayMode.CRT_MONITOR
        @Volatile private var running = false

        /** Called when the deck view changes, so the media controls follow it. */
        fun setArtMode(context: Context, mode: DiscDisplayMode) {
            if (artMode == mode) return
            artMode = mode
            if (!running) return
            try {
                context.startService(Intent(context, RetroAudioPlaybackService::class.java).apply { action = ACTION_REFRESH_ART })
            } catch (ignored: Exception) {}
        }

        fun startOrUpdateService(context: Context, song: Song?, isPlaying: Boolean) {
            try {
                val intent = Intent(context, RetroAudioPlaybackService::class.java).apply {
                    action = ACTION_START_OR_UPDATE
                    putExtra("title", song?.title ?: "Mixtape Pro")
                    putExtra("artist", song?.artist ?: "Ready to Play")
                    putExtra("isPlaying", isPlaying)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (ignored: Exception) {}
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, RetroAudioPlaybackService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (ignored: Exception) {}
        }
    }

    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var artJob: Job? = null
    private var artKey: String? = null
    private var artBitmap: Bitmap? = null
    private var lastTitle = "Mixtape Pro"
    private var lastArtist = "Retro Audio"
    private var lastPlaying = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        createNotificationChannel()
        setupMediaSession()
    }

    private fun setupMediaSession() {
        try {
            mediaSession = MediaSession(this, "Flashback99MediaSession").apply {
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        activeEngine?.togglePlayPause()
                    }

                    override fun onPause() {
                        activeEngine?.togglePlayPause()
                    }

                    override fun onSkipToNext() {
                        activeEngine?.nextTrack()
                    }

                    override fun onSkipToPrevious() {
                        activeEngine?.previousTrack()
                    }

                    override fun onStop() {
                        activeEngine?.stop()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }

                    override fun onSeekTo(pos: Long) {
                        activeEngine?.seekTo(pos)
                    }
                })
                isActive = true
            }
        } catch (ignored: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_OR_UPDATE -> {
                val title = intent.getStringExtra("title") ?: "Mixtape Pro"
                val artist = intent.getStringExtra("artist") ?: "Retro Audio"
                val isPlaying = intent.getBooleanExtra("isPlaying", false)
                updateMediaSessionState(title, artist, isPlaying)
                startForegroundWithNotification(title, artist, isPlaying)
            }
            ACTION_PLAY_PAUSE -> {
                activeEngine?.togglePlayPause()
                val currentSong = activeEngine?.currentSong?.value
                val isPlaying = activeEngine?.isPlaying?.value ?: false
                val title = currentSong?.title ?: "Mixtape Pro"
                val artist = currentSong?.artist ?: "Retro Audio"
                updateMediaSessionState(title, artist, isPlaying)
                startForegroundWithNotification(title, artist, isPlaying)
            }
            ACTION_REFRESH_ART -> {
                updateMediaSessionState(lastTitle, lastArtist, lastPlaying)
                startForegroundWithNotification(lastTitle, lastArtist, lastPlaying)
            }
            ACTION_NEXT -> {
                activeEngine?.nextTrack()
            }
            ACTION_PREV -> {
                activeEngine?.previousTrack()
            }
            ACTION_VOLUME_UP -> {
                try {
                    audioManager?.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE,
                        AudioManager.FLAG_SHOW_UI
                    )
                } catch (ignored: Exception) {}
            }
            ACTION_VOLUME_DOWN -> {
                try {
                    audioManager?.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER,
                        AudioManager.FLAG_SHOW_UI
                    )
                } catch (ignored: Exception) {}
            }
            ACTION_STOP -> {
                mediaSession?.isActive = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun updateMediaSessionState(title: String, artist: String, isPlaying: Boolean) {
        lastTitle = title; lastArtist = artist; lastPlaying = isPlaying
        try {
            val session = mediaSession ?: return
            val song = activeEngine?.currentSong?.value
            val isLive = song?.source == AudioSource.RADIO_STREAM
            val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            val position = if (isLive) PlaybackState.PLAYBACK_POSITION_UNKNOWN else activeEngine?.currentPositionMs?.value ?: 0L
            val duration = if (isLive) 0L else activeEngine?.durationMs?.value ?: 0L

            var actions = PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_STOP
            // A live station can't be seeked: no progress bar on the lock screen
            if (!isLive) actions = actions or PlaybackState.ACTION_SEEK_TO

            session.setPlaybackState(
                PlaybackState.Builder()
                    .setActions(actions)
                    .setState(state, position, if (isPlaying) 1.0f else 0f)
                    .build()
            )

            val key = artKeyFor(song)
            val metadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, (song?.album?.takeIf { it.isNotBlank() } ?: "Mixtape Pro"))
            if (!isLive && duration > 0) metadata.putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
            if (key == artKey && artBitmap != null) {
                metadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artBitmap)
                metadata.putBitmap(MediaMetadata.METADATA_KEY_ART, artBitmap)
            }
            session.setMetadata(metadata.build())

            if (key != artKey) loadArtwork(song, key)
        } catch (ignored: Exception) {}
    }

    private fun artKeyFor(song: Song?) = "${song?.id}|${song?.displayArtUri}|${song?.albumArtRes}|$artMode"

    /**
     * The picture for the lock screen and media controls: the same thing the player's deck shows.
     * CD, cassette and vinyl use their desk photos; "Album art" uses the cover (or the radio
     * station's photo), falling back to the song's built-in art and finally the cassette photo.
     */
    private fun loadArtwork(song: Song?, key: String) {
        artJob?.cancel()
        artJob = scope.launch {
            val bmp = withContext(Dispatchers.IO) {
                try {
                    when (artMode) {
                        DiscDisplayMode.CD_JEWEL_CASE -> decodeRes(R.drawable.deck_cd_photo)
                        DiscDisplayMode.VINYL_RECORD -> decodeRes(R.drawable.deck_vinyl_photo)
                        DiscDisplayMode.CASSETTE_TAPE -> decodeRes(R.drawable.img_cassette_mixtape)
                        DiscDisplayMode.CRT_MONITOR -> loadUri(song?.displayArtUri)
                            ?: BuiltInArt.songArt(song?.albumArtRes)?.takeIf { it != R.drawable.img_cd_jewel_case }?.let { decodeRes(it) }
                            ?: decodeRes(BuiltInArt.fallbackPhoto(song?.id))
                    }
                } catch (e: Exception) { null }
            }
            artKey = key
            artBitmap = bmp
            updateMediaSessionState(lastTitle, lastArtist, lastPlaying)
            startForegroundWithNotification(lastTitle, lastArtist, lastPlaying)
        }
    }

    private fun decodeRes(res: Int): Bitmap? =
        BitmapFactory.decodeResource(resources, res, BitmapFactory.Options().apply { inSampleSize = 2 })

    private suspend fun loadUri(uri: String?): Bitmap? {
        if (uri.isNullOrBlank()) return null
        val request = coil.request.ImageRequest.Builder(this)
            .data(uri)
            .size(512)
            .allowHardware(false)
            .build()
        val result = coil.Coil.imageLoader(this).execute(request)
        return (result as? coil.request.SuccessResult)?.drawable?.toBitmap(512, 512)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Flashback'99 Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background audio playback and notification volume & playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification(title: String, artist: String, isPlaying: Boolean) {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPausePendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, RetroAudioPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, RetroAudioPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPendingIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, RetroAudioPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val volUpPendingIntent = PendingIntent.getService(
            this,
            5,
            Intent(this, RetroAudioPlaybackService::class.java).apply { action = ACTION_VOLUME_UP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val volDownPendingIntent = PendingIntent.getService(
            this,
            6,
            Intent(this, RetroAudioPlaybackService::class.java).apply { action = ACTION_VOLUME_DOWN },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            7,
            Intent(this, RetroAudioPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("Mixtape Pro")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply { if (artBitmap != null) setLargeIcon(artBitmap) }
            .addAction(android.R.drawable.ic_media_previous, "Prev", prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setDeleteIntent(stopPendingIntent)

        // A proper media notification tied to the session: the phone's media panel and lock
        // screen then show the artwork, the controls and (for tracks) the progress bar.
        mediaSession?.sessionToken?.let { token ->
            builder.setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(android.support.v4.media.session.MediaSessionCompat.Token.fromToken(token))
                    .setShowActionsInCompactView(0, 1, 2)
            )
        }

        val notification = builder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            com.example.widget.RetroMusicWidgetProvider.sendUpdate(this, title, artist, isPlaying)
        } catch (ignored: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        scope.cancel()
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
            mediaSession = null
        } catch (ignored: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
