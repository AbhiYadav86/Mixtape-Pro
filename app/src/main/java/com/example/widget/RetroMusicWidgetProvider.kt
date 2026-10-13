package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.audio.RetroAudioPlaybackService

class RetroMusicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, null, null, false)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_UPDATE_WIDGET -> {
                val title = intent.getStringExtra(EXTRA_TITLE)
                val artist = intent.getStringExtra(EXTRA_ARTIST)
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, RetroMusicWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, title, artist, isPlaying)
                }
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.example.widget.ACTION_UPDATE_WIDGET"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        fun sendUpdate(context: Context, title: String?, artist: String?, isPlaying: Boolean) {
            val intent = Intent(context, RetroMusicWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
            }
            context.sendBroadcast(intent)
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            title: String?,
            artist: String?,
            isPlaying: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.retro_widget_layout)

            // Track title & artist
            views.setTextViewText(R.id.widget_track_title, title ?: "Mixtape Pro")
            views.setTextViewText(R.id.widget_track_artist, artist ?: "Tap to Open Vault")
            views.setTextViewText(R.id.widget_status_text, if (isPlaying) "PLAYING" else "PAUSED")

            // Play/Pause icon
            val playIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            views.setImageViewResource(R.id.widget_btn_play_pause, playIcon)

            // Click root to open app
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent)

            // Prev Button intent
            val prevIntent = Intent(context, RetroAudioPlaybackService::class.java).apply {
                action = RetroAudioPlaybackService.ACTION_PREVIOUS
            }
            val prevPendingIntent = PendingIntent.getService(
                context,
                1,
                prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPendingIntent)

            // Play/Pause Button intent
            val playPauseIntent = Intent(context, RetroAudioPlaybackService::class.java).apply {
                action = RetroAudioPlaybackService.ACTION_TOGGLE_PLAY_PAUSE
            }
            val playPausePendingIntent = PendingIntent.getService(
                context,
                2,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePendingIntent)

            // Next Button intent
            val nextIntent = Intent(context, RetroAudioPlaybackService::class.java).apply {
                action = RetroAudioPlaybackService.ACTION_NEXT
            }
            val nextPendingIntent = PendingIntent.getService(
                context,
                3,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
