package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object RetroSoundEffects {
    var isSoundEnabled: Boolean = true
    private val scope = CoroutineScope(Dispatchers.Default)

    enum class SoundType {
        RETRO_CLICK,
        CASSETTE_CLACK,
        CD_SPIN,
        SWITCH_TOGGLE,
        SLIDER_TICK,
        NEEDLE_DROP,
        AIR_HORN,
        VINYL_SCRATCH,
        BEAT_DROP,
        LASER_ZAP,
        TAPE_REWIND,
        CLUB_SIREN,
        CROWD_APPLAUSE,
        SNARE_ROLL,
        RADIO_TUNER_TICK;

        companion object {
            @JvmField
            val WINAMP_CLICK = RETRO_CLICK
        }
    }

    fun playClick(type: SoundType = SoundType.RETRO_CLICK) {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                when (type) {
                    SoundType.RETRO_CLICK -> playRetroClick()
                    SoundType.CASSETTE_CLACK -> playCassetteClack()
                    SoundType.CD_SPIN -> playCdSpin()
                    SoundType.SWITCH_TOGGLE -> playSwitchToggle()
                    SoundType.SLIDER_TICK -> playSliderTick()
                    SoundType.NEEDLE_DROP -> playNeedleDrop()
                    SoundType.AIR_HORN -> playAirHorn()
                    SoundType.VINYL_SCRATCH -> playVinylScratch()
                    SoundType.BEAT_DROP -> playBeatDrop()
                    SoundType.LASER_ZAP -> playLaserZap()
                    SoundType.TAPE_REWIND -> playTapeRewind()
                    SoundType.CLUB_SIREN -> playClubSiren()
                    SoundType.CROWD_APPLAUSE -> playCrowdApplause()
                    SoundType.SNARE_ROLL -> playSnareRoll()
                    SoundType.RADIO_TUNER_TICK -> playRadioTunerTick()
                }
            } catch (e: Exception) {
                // Ignore audio synthesis errors gracefully
            }
        }
    }

    private fun playRetroClick() {
        val sampleRate = 22050
        val durationMs = 28
        val numSamples = (sampleRate * durationMs / 1000)
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * 220.0)
            // Two high-frequency pulses for mechanical switch tactile click
            val wave = sin(2.0 * PI * 1800.0 * t) * 0.7 + sin(2.0 * PI * 3400.0 * t) * 0.3
            pcm[i] = (wave * envelope * Short.MAX_VALUE * 0.7).toInt().toShort()
        }
        playPcm(pcm, sampleRate)
    }

    private fun playCassetteClack() {
        val sampleRate = 22050
        val durationMs = 60
        val numSamples = (sampleRate * durationMs / 1000)
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * 80.0)
            // Lower punchy thump of a spring-loaded mechanical cassette head
            val wave = sin(2.0 * PI * 160.0 * t) * 0.8 + sin(2.0 * PI * 650.0 * t) * 0.4
            pcm[i] = (wave * envelope * Short.MAX_VALUE * 0.85).toInt().toShort()
        }
        playPcm(pcm, sampleRate)
    }

    private fun playCdSpin() {
        val sampleRate = 22050
        val durationMs = 90
        val numSamples = (sampleRate * durationMs / 1000)
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * 40.0)
            // Optical laser servo chirp
            val freq = 2400.0 + (t * 8000.0)
            val wave = sin(2.0 * PI * freq * t)
            pcm[i] = (wave * envelope * Short.MAX_VALUE * 0.4).toInt().toShort()
        }
        playPcm(pcm, sampleRate)
    }

    private fun playSwitchToggle() {
        val sampleRate = 22050
        val durationMs = 35
        val numSamples = (sampleRate * durationMs / 1000)
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * 140.0)
            val wave = sin(2.0 * PI * 980.0 * t) * 0.6 + sin(2.0 * PI * 2200.0 * t) * 0.4
            pcm[i] = (wave * envelope * Short.MAX_VALUE * 0.6).toInt().toShort()
        }
        playPcm(pcm, sampleRate)
    }

    private fun playSliderTick() {
        val sampleRate = 22050
        val durationMs = 12
        val numSamples = (sampleRate * durationMs / 1000)
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * 450.0)
            val wave = sin(2.0 * PI * 2600.0 * t)
            pcm[i] = (wave * envelope * Short.MAX_VALUE * 0.35).toInt().toShort()
        }
        playPcm(pcm, sampleRate)
    }

    private fun playNeedleDrop() {
        val sampleRate = 22050
        val durationMs = 120
        val numSamples = (sampleRate * durationMs / 1000)
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * 30.0)
            // Vinyl surface scratch noise & gentle thud
            val noise = (Math.random() * 2.0 - 1.0) * 0.5
            val thud = sin(2.0 * PI * 95.0 * t) * 0.5
            pcm[i] = ((noise + thud) * envelope * Short.MAX_VALUE * 0.5).toInt().toShort()
        }
        playPcm(pcm, sampleRate)
    }

    private fun playAirHorn() {
        val sampleRate = 22050
        val durationMs = 380
        val numSamples = (sampleRate * durationMs / 1000)
        val pcm = ShortArray(numSamples)

        // Classic Reggae / Hip-Hop DJ multi-tone brass Airhorn
        val f1 = 466.16 // Bb4
        val f2 = 587.33 // D5
        val f3 = 698.46 // F5
        val f4 = 932.33 // Bb5

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = if (t < 0.05) t / 0.05 else if (t > 0.30) 1.0 - (t - 0.30) / 0.08 else 1.0
            val wave = (sin(2.0 * PI * f1 * t) * 0.35 +
                    sin(2.0 * PI * f2 * t) * 0.30 +
                    sin(2.0 * PI * f3 * t) * 0.25 +
                    sin(2.0 * PI * f4 * t) * 0.15)
            // Stutter pulses
            val pulse = (sin(2.0 * PI * 18.0 * t) * 0.15 + 0.85)
            pcm[i] = (wave * envelope * pulse * Short.MAX_VALUE * 0.85).toInt().toShort()
        }
        playPcm(pcm, sampleRate)
    }

    private fun playVinylScratch() {
        val sampleRate = 22050
        val durationMs = 260
        val numSamples = (sampleRate * durationMs / 1000)
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = sin(PI * (t / (durationMs / 1000.0)))
            val modFreq = 400.0 + 800.0 * sin(2.0 * PI * 12.0 * t)
            val noise = (Math.random() * 2.0 - 1.0) * 0.4
            val tone = sin(2.0 * PI * modFreq * t) * 0.6
            pcm[i] = ((tone + noise) * envelope * Short.MAX_VALUE * 0.75).toInt().toShort()
        }
        playPcm(pcm, sampleRate)
    }

    private fun playBeatDrop() {
        val sampleRate = 22050
        val durationMs = 450
        val numSamples = (sampleRate * durationMs / 1000)
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * 6.0)
            // Pitch sweeps down from 160Hz to 40Hz sub bass
            val freq = 40.0 + 120.0 * exp(-t * 15.0)
            val wave = sin(2.0 * PI * freq * t)
            pcm[i] = (wave * envelope * Short.MAX_VALUE * 0.95).toInt().toShort()
        }
        playPcm(pcm, sampleRate)
    }

    private fun playLaserZap() {
        val sampleRate = 22050
        val durationMs = 180
        val numSamples = (sampleRate * durationMs / 1000)
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * 18.0)
            val freq = 200.0 + 3200.0 * exp(-t * 22.0)
            val wave = sin(2.0 * PI * freq * t)
            pcm[i] = (wave * envelope * Short.MAX_VALUE * 0.75).toInt().toShort()
        }
        playPcm(pcm, sampleRate)
    }

    private fun playTapeRewind() {
        val sampleRate = 22050
        val durationMs = 320
        val numSamples = (sampleRate * durationMs / 1000)
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = if (t < 0.1) t / 0.1 else exp(-(t - 0.1) * 8.0)
            val freq = 1200.0 + 1800.0 * sin(2.0 * PI * 28.0 * t)
            val noise = (Math.random() * 2.0 - 1.0) * 0.3
            val wave = sin(2.0 * PI * freq * t) * 0.7 + noise
            pcm[i] = (wave * envelope * Short.MAX_VALUE * 0.7).toInt().toShort()
        }
        playPcm(pcm, sampleRate)
    }

    private fun playClubSiren() {
        val sampleRate = 22050
        val durationMs = 350
        val numSamples = (sampleRate * durationMs / 1000)
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = 0.9
            val freq = 700.0 + 400.0 * sin(2.0 * PI * 6.0 * t)
            val wave = sin(2.0 * PI * freq * t)
            pcm[i] = (wave * envelope * Short.MAX_VALUE * 0.7).toInt().toShort()
        }
        playPcm(pcm, sampleRate)
    }

    private fun playCrowdApplause() {
        val sampleRate = 22050
        val durationMs = 400
        val numSamples = (sampleRate * durationMs / 1000)
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = sin(PI * (t / 0.40))
            // Layered applause claps
            val noise = (Math.random() * 2.0 - 1.0) * 0.6
            val clapBurst = if (i % 600 < 50) 0.4 else 0.0
            pcm[i] = ((noise + clapBurst) * envelope * Short.MAX_VALUE * 0.65).toInt().toShort()
        }
        playPcm(pcm, sampleRate)
    }

    private fun playSnareRoll() {
        val sampleRate = 22050
        val durationMs = 300
        val numSamples = (sampleRate * durationMs / 1000)
        val pcm = ShortArray(numSamples)

        val hits = 6
        val samplesPerHit = numSamples / hits

        for (i in 0 until numSamples) {
            val hitIndex = i / samplesPerHit
            val subSample = i % samplesPerHit
            val tSub = subSample.toDouble() / sampleRate
            val hitVolume = 0.4 + (hitIndex.toDouble() / hits) * 0.6
            val envelope = exp(-tSub * 60.0) * hitVolume
            val noise = (Math.random() * 2.0 - 1.0) * 0.7
            val tone = sin(2.0 * PI * 220.0 * tSub) * 0.3
            pcm[i] = ((noise + tone) * envelope * Short.MAX_VALUE * 0.75).toInt().toShort()
        }
        playPcm(pcm, sampleRate)
    }

    private fun playRadioTunerTick() {
        val sampleRate = 44100
        val durationSec = 0.08
        val numSamples = (sampleRate * durationSec).toInt()
        val pcm = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * 45.0)
            val staticNoise = (Math.random() * 2.0 - 1.0) * 0.6
            val whine = sin(2.0 * PI * (800.0 + 400.0 * sin(t * 80.0)) * t) * 0.4
            pcm[i] = ((staticNoise + whine) * envelope * Short.MAX_VALUE * 0.5).toInt().toShort()
        }
        playPcm(pcm, sampleRate)
    }

    private fun playPcm(pcm: ShortArray, sampleRate: Int) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, pcm.size * 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcm, 0, pcm.size)
        track.play()
        
        // Release track after playback
        scope.launch {
            val waitTime = (pcm.size.toDouble() / sampleRate * 1000).toLong() + 50L
            kotlinx.coroutines.delay(waitTime)
            try {
                track.stop()
                track.release()
            } catch (ignored: Exception) {}
        }
    }
}
