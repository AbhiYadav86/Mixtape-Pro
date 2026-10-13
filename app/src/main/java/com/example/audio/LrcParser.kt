package com.example.audio

data class LyricLine(
    val timeMs: Long,
    val text: String,
    /** True when the source lyrics had no timestamps and timing was estimated. */
    val isEstimated: Boolean = false
)

object LrcParser {

    private val timeRegex = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val metaTagRegex = Regex("""^\[(ar|ti|al|by|offset|length|re|ve|au|#):.*]$""", RegexOption.IGNORE_CASE)

    /**
     * Parse LRC (synced) or plain lyrics.
     * Plain lyrics have no timestamps; their lines are spread evenly across the track
     * so the teleprompter still scrolls along with the song.
     */
    fun parse(lrcContent: String, durationMs: Long = 0L): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        val plainLines = mutableListOf<String>()

        for (line in lrcContent.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (metaTagRegex.matches(trimmed)) continue

            val matches = timeRegex.findAll(trimmed).toList()
            if (matches.isNotEmpty() && matches.first().range.first == 0) {
                // A line can carry several timestamps: [00:12.00][01:02.00]Chorus line
                val text = trimmed.substring(matches.last().range.last + 1).trim()
                for (match in matches) {
                    val min = match.groupValues[1].toLongOrNull() ?: 0L
                    val sec = match.groupValues[2].toLongOrNull() ?: 0L
                    val fracStr = match.groupValues.getOrNull(3).orEmpty()
                    val millis = when (fracStr.length) {
                        0 -> 0L
                        1 -> fracStr.toLong() * 100
                        2 -> fracStr.toLong() * 10
                        else -> fracStr.take(3).toLong()
                    }
                    result.add(LyricLine((min * 60 + sec) * 1000 + millis, text.ifEmpty { "♪" }))
                }
            } else {
                plainLines.add(trimmed)
            }
        }

        if (result.isNotEmpty()) {
            return result.sortedBy { it.timeMs }
        }

        if (plainLines.isEmpty()) return emptyList()

        // Unsynced lyrics: estimate timing across the song (skip ~8% intro, ~5% outro)
        val total = if (durationMs > 0) durationMs else plainLines.size * 4000L
        val startMs = (total * 0.08).toLong()
        val span = (total * 0.87).toLong().coerceAtLeast(1L)
        val step = span / plainLines.size.coerceAtLeast(1)
        return plainLines.mapIndexed { i, text -> LyricLine(startMs + i * step, text, isEstimated = true) }
    }

    fun getDemoLyricsForTitle(title: String, artist: String): String {
        val lower = "$title $artist".lowercase()
        return when {
            lower.contains("eurodance") || lower.contains("synth") || lower.contains("rave") || lower.contains("midnight") -> """
[00:00.00] ♫ (90s Analog Synth Intro) ♫
[00:04.00] Pulsing neon through the midnight air
[00:08.50] Electric bassline taking you right there
[00:13.00] Feel the rhythm in the stereo sound
[00:17.50] Retro frequencies shake the ground
[00:22.00] (Chorus)
[00:24.00] Flashback ninety-nine, rewind the groove
[00:28.50] 16-bit melody making you move
[00:33.00] Turn up the tape deck, let the speakers glow
[00:37.50] Feel the digital audio flow
[00:43.00] ♫ (Guitar & Synthesizer Solo) ♫
[00:52.00] Cassette spools spinning round and round
[00:56.50] High definition stereo sound
[01:01.00] Never gonna let the retro magic fade
[01:05.50] Best mixtapes that we ever made!
[01:10.00] (Outro - Fade to Neon)
            """.trimIndent()
            lower.contains("grunge") || lower.contains("rock") || lower.contains("overdrive") -> """
[00:00.00] ♫ (Distorted Tube Amp Feedback) ♫
[00:05.00] Grunge guitars ripping through the garage door
[00:10.00] Heavy flannel shirts upon the basement floor
[00:15.00] Crank the preamp gain to maximum power
[00:20.00] Stomp the overdrive pedal every hour!
[00:25.00] (Chorus)
[00:26.50] 90s Alternative, loud and free
[00:31.00] Sonic rebellion on the CRT
[00:36.00] Screaming riffs and heavy drum beats
[00:41.00] Rocking out in the midnight streets!
[00:48.00] ♫ (Heavy Overdrive Breakdown) ♫
            """.trimIndent()
            lower.contains("hip-hop") || lower.contains("boom") || lower.contains("808") || lower.contains("groove") -> """
[00:00.00] ♫ (Vinyl Crackle & 808 Sub Kick) ♫
[00:04.50] Drop the needle on the direct drive deck
[00:08.00] Sampling the jazz breaks with utmost respect
[00:12.50] Boom-bap snare hitting crisp and tight
[00:16.50] SP-1200 swinging through the night
[00:21.00] (Hook)
[00:22.50] Street studio beats in thirty-two bit
[00:26.50] Pure golden era classic hit
[00:30.50] Tape cassette running side A to side B
[00:35.00] Hip-hop history for you and me!
            """.trimIndent()
            lower.contains("vaporwave") || lower.contains("chill") || lower.contains("mall") || lower.contains("dream") -> """
[00:00.00] ♫ (Slowed & Reverb VHS Nostalgia) ♫
[00:06.00] Walking through the empty 1995 mall
[00:12.00] Pastel neon shadows dancing on the wall
[00:18.00] Translucent aqua casing in the summer light
[00:24.00] Vaporwave echoes floating through the night
[00:32.00] (Chorus)
[00:34.00] Endless nostalgia, smooth and slow
[00:41.00] Ambient synthesizers in a CRT glow
[00:49.00] Time stands still in the cyber mall breeze
[00:57.00] Floating away on the digital seas...
            """.trimIndent()
            else -> """
[00:00.00] ♫ (Retro Audio Playback) ♫
[00:05.00] Now Playing: $title
[00:09.00] Artist: $artist
[00:14.00] 16-Band Realtime Spectrum Analysis Active
[00:20.00] Enhanced with DSP Equalizer & Dynamic Bass
[00:27.00] (Enjoy authentic high-fidelity retro audio)
            """.trimIndent()
        }
    }
}
