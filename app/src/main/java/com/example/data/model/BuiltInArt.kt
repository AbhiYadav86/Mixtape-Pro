package com.example.data.model

import com.example.R

/**
 * Built-in artwork is saved in the database as a drawable resource id. Those ids change
 * whenever the app's images change between versions, so an old saved id can point at the wrong
 * resource (or none) and crash the screen that draws it. Only ids of the current built-in
 * images are trusted; anything else falls back to the default.
 */
object BuiltInArt {
    private val valid: Set<Int> by lazy {
        setOf(R.drawable.img_cassette_mixtape, R.drawable.img_cd_jewel_case, R.drawable.img_app_icon)
    }

    /** A saved song-art id, or null when it isn't one of today's built-in images. */
    fun songArt(id: Int?): Int? = id?.takeIf { it in valid }

    /**
     * Picture for a track with no cover: one of two 90s desk photos (neon Eurodance CD or a
     * grunge record), picked per song so the same track always gets the same one.
     */
    fun fallbackPhoto(songId: String?): Int =
        if (songId == null || (songId.hashCode() and 1) == 0) R.drawable.art_fallback_neon_cd
        else R.drawable.art_fallback_grunge_vinyl

    /** A saved mixtape-cover id, or the cassette cover when it isn't valid any more. */
    fun cover(id: Int?): Int = id?.takeIf { it in valid } ?: R.drawable.img_cassette_mixtape
}
