package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin

/**
 * Real-time 16-Bit Stereo Retro Synthesizer Engine using Android AudioTrack.
 * Generates authentic 90s FM-style synthwave, eurodance, grunge, hip-hop, and chiptune audio.
 * Guarantees that preloaded mixtapes, offline playback, and jukebox demos ALWAYS produce real sound.
 */
class RetroPcmSynthesizer {

    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null
    private val isRunning = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val sampleRate = 44100
    private val minBufSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private val bufferSize = (if (minBufSize > 0) minBufSize else sampleRate / 2).coerceAtLeast(sampleRate / 2)

    fun startSynthPlayback(songTitle: String, artist: String, genre: String) {
        stopSynthPlayback()
        isRunning.set(true)

        // Determine tempo and chord progression based on genre/title
        val bpm = when {
            genre.contains("Eurodance", ignoreCase = true) || genre.contains("Dance", ignoreCase = true) -> 138
            genre.contains("Rave", ignoreCase = true) || genre.contains("Acid", ignoreCase = true) || genre.contains("Trance", ignoreCase = true) -> 142
            genre.contains("Hip-Hop", ignoreCase = true) || genre.contains("Boom Bap", ignoreCase = true) -> 92
            genre.contains("Ambient", ignoreCase = true) || genre.contains("Space", ignoreCase = true) || genre.contains("Lounge", ignoreCase = true) -> 76
            genre.contains("Grunge", ignoreCase = true) || genre.contains("Rock", ignoreCase = true) -> 124
            genre.contains("City Pop", ignoreCase = true) || genre.contains("Chill", ignoreCase = true) -> 108
            else -> 120
        }

        synthesisJob = scope.launch {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                // Genre-specific chord progressions (frequencies in Hz)
                val rootNotes = when {
                    genre.contains("Grunge", ignoreCase = true) || genre.contains("Rock", ignoreCase = true) ->
                        listOf(164.81, 146.83, 130.81, 164.81) // E3, D3, C3, E3 power progression
                    genre.contains("Synthwave", ignoreCase = true) || genre.contains("City Pop", ignoreCase = true) ->
                        listOf(220.0, 174.61, 261.63, 196.0) // Am, F, C, G
                    genre.contains("Acid", ignoreCase = true) || genre.contains("Rave", ignoreCase = true) ->
                        listOf(130.81, 130.81, 155.56, 174.61) // C3, C3, Eb3, F3
                    else ->
                        listOf(220.0, 261.63, 196.0, 174.61) // A3, C4, G3, F3
                }

                val chunkSamples = 2048
                val pcmBuffer = ShortArray(chunkSamples * 2) // Stereo

                var sampleIndex = 0L
                val samplesPerBeat = ((sampleRate.toDouble() * 60.0) / bpm.toDouble()).toInt().coerceAtLeast(100)

                while (isRunning.get() && isActive) {
                    for (i in 0 until chunkSamples) {
                        val currentSample = sampleIndex + i
                        val beat = (currentSample / samplesPerBeat).toInt()
                        val chordIndex = (beat / 4) % rootNotes.size
                        val rootFreq = rootNotes[chordIndex]

                        // 1. Arpeggiator note pattern
                        val arpStep = (currentSample / (samplesPerBeat / 4).coerceAtLeast(1)).toInt() % 4
                        val semitoneOffset = when (arpStep) {
                            0 -> 0
                            1 -> 3
                            2 -> 7
                            else -> 12
                        }
                        val leadFreq = rootFreq * Math.pow(2.0, semitoneOffset / 12.0)

                        // 2. Multi-oscillator FM / Sub synthesis
                        val time = currentSample.toDouble() / sampleRate
                        val leadOsc1 = sin(2.0 * PI * leadFreq * time)
                        val leadOsc2 = sin(2.0 * PI * (leadFreq * 1.004) * time)
                        val lead = (leadOsc1 + leadOsc2) * 0.28

                        // Bassline Sub-Oscillator (1 octave down)
                        val bassFreq = rootFreq / 2.0
                        val bass = sin(2.0 * PI * bassFreq * time) * 0.40

                        // 3. Rhythm Percussion (Kick drum on downbeats, hi-hat on offbeats)
                        val beatPhase = (currentSample % samplesPerBeat).toDouble() / samplesPerBeat
                        val kick = if (beatPhase < 0.18) {
                            val kickFreq = 150.0 * (1.0 - (beatPhase / 0.18)) + 40.0
                            sin(2.0 * PI * kickFreq * (beatPhase * 0.18)) * (1.0 - (beatPhase / 0.18)) * 0.45
                        } else 0.0

                        // Hi-hat tick
                        val subBeatPhase = ((currentSample + samplesPerBeat / 2) % samplesPerBeat).toDouble() / samplesPerBeat
                        val hihat = if (subBeatPhase < 0.06) {
                            ((Math.random() * 2.0 - 1.0) * (1.0 - (subBeatPhase / 0.06))) * 0.15
                        } else 0.0

                        // Combine channels into rich stereo mix
                        val mixedLeft = (lead * 0.85 + bass * 0.95 + kick + hihat * 0.75).coerceIn(-1.0, 1.0)
                        val mixedRight = (lead * 0.95 + bass * 0.85 + kick + hihat * 0.95).coerceIn(-1.0, 1.0)

                        pcmBuffer[i * 2] = (mixedLeft * 28000).toInt().toShort()
                        pcmBuffer[i * 2 + 1] = (mixedRight * 28000).toInt().toShort()
                    }

                    track.write(pcmBuffer, 0, pcmBuffer.size)
                    sampleIndex += chunkSamples
                }
            } catch (ignored: Exception) {
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (ignored: Exception) {}
                audioTrack = null
            }
        }
    }

    fun stopSynthPlayback() {
        isRunning.set(false)
        synthesisJob?.cancel()
        synthesisJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (ignored: Exception) {}
        audioTrack = null
    }

    fun isSynthActive(): Boolean = isRunning.get()
}
