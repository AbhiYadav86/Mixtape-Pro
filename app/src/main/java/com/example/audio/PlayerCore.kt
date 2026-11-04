package com.example.audio

import android.content.Context

/**
 * One music player for the whole app. It used to belong to the screen's view model, so when
 * Android re-created the screen (or opened a second copy of it from the media panel) the new
 * screen got a new, empty player while the old one kept playing in the background: the app
 * looked frozen on "Ready to Play" and the media controls lost the song and its artwork.
 * Now every screen talks to the same player, which lives as long as the app does.
 */
object PlayerCore {
    @Volatile private var instance: RetroAudioPlayerEngine? = null

    fun engine(context: Context): RetroAudioPlayerEngine =
        instance ?: synchronized(this) {
            instance ?: RetroAudioPlayerEngine(
                context.applicationContext,
                VisualizerEngine(),
                RetroEqualizerController()
            ).also { instance = it }
        }
}
