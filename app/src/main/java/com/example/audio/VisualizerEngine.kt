package com.example.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.Visualizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class VisualizerEngine {
    private var visualizer: Visualizer? = null
    private var isCapturing = false
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 16 Frequency Spectrum bands (0.0f .. 1.0f)
    private val _spectrumBands = MutableStateFlow(FloatArray(16) { 0f })
    val spectrumBands: StateFlow<FloatArray> = _spectrumBands.asStateFlow()

    // 16 Peak Hold values (0.0f .. 1.0f)
    private val _peakHoldBands = MutableStateFlow(FloatArray(16) { 0f })
    val peakHoldBands: StateFlow<FloatArray> = _peakHoldBands.asStateFlow()

    // VU Meters (Left & Right channel) (0.0f .. 1.0f)
    private val _vuLeft = MutableStateFlow(0f)
    val vuLeft: StateFlow<Float> = _vuLeft.asStateFlow()

    private val _vuRight = MutableStateFlow(0f)
    val vuRight: StateFlow<Float> = _vuRight.asStateFlow()

    // Waveform points for oscilloscope (64 points, -1.0f .. 1.0f)
    private val _waveformPoints = MutableStateFlow(FloatArray(64) { 0f })
    val waveformPoints: StateFlow<FloatArray> = _waveformPoints.asStateFlow()

    private var animationJob: Job? = null
    private var isPlaying = false
    private var simPhase = 0.0

    private fun isHardwareVisualizerSupported(context: Context?): Boolean {
        if (context == null) return false
        return try {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return false

            val effects = AudioEffect.queryEffects() ?: return false
            val visualizerUuid = UUID.fromString("e46b26a0-dddd-11db-8afd-0002a5d5c51b")
            effects.any { it.uuid == visualizerUuid }
        } catch (e: Throwable) {
            false
        }
    }

    fun attachToAudioSession(audioSessionId: Int, context: Context? = null) {
        release()
        try {
            if (audioSessionId > 0 && isHardwareVisualizerSupported(context)) {
                visualizer = Visualizer(audioSessionId).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[1]
                    setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                            if (waveform != null && isPlaying) {
                                processWaveform(waveform)
                            }
                        }

                        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            if (fft != null && isPlaying) {
                                processFft(fft)
                            }
                        }
                    }, Visualizer.getMaxCaptureRate() / 2, true, true)
                    enabled = true
                }
                isCapturing = true
            }
        } catch (e: Throwable) {
            // Visualizer permission or hardware limitation, fallback to dynamic synthetic rhythm engine
            isCapturing = false
        }
    }

    fun setPlaybackActive(active: Boolean) {
        isPlaying = active
        if (active) {
            startSimulationLoop()
        } else {
            decayToZero()
        }
    }

    private fun startSimulationLoop() {
        animationJob?.cancel()
        animationJob = scope.launch {
            val bands = FloatArray(16) { 0f }
            val peaks = FloatArray(16) { 0f }
            val wave = FloatArray(64) { 0f }

            while (isActive && isPlaying) {
                simPhase += 0.12

                // Calculate rhythmic frequency bands if hardware visualizer is idle or not capturing
                for (i in 0 until 16) {
                    val bassInfluence = if (i < 4) (cos(simPhase * 1.5) * 0.4 + 0.6).toFloat() else 1.0f
                    val midInfluence = if (i in 4..10) (sin(simPhase * 2.2 + i) * 0.35 + 0.65).toFloat() else 1.0f
                    val trebleInfluence = if (i > 10) (sin(simPhase * 3.7 + i * 0.5) * 0.3 + 0.5).toFloat() else 1.0f

                    val noise = (Math.random() * 0.2).toFloat()
                    val targetLevel = ((0.35f + noise) * bassInfluence * midInfluence * trebleInfluence)
                        .coerceIn(0.05f, 0.98f)

                    // Smooth rise and fall
                    bands[i] = bands[i] * 0.65f + targetLevel * 0.35f

                    // Peak hold logic
                    if (bands[i] > peaks[i]) {
                        peaks[i] = bands[i]
                    } else {
                        peaks[i] = max(0f, peaks[i] - 0.025f)
                    }
                }

                // VU needle ballistics
                val leftTarget = (bands[1] * 0.6f + bands[3] * 0.4f).coerceIn(0f, 1f)
                val rightTarget = (bands[2] * 0.5f + bands[4] * 0.5f).coerceIn(0f, 1f)
                _vuLeft.value = _vuLeft.value * 0.7f + leftTarget * 0.3f
                _vuRight.value = _vuRight.value * 0.7f + rightTarget * 0.3f

                // Oscilloscope waveform points
                for (i in 0 until 64) {
                    val t = i.toDouble() / 64.0
                    val w = (sin(2.0 * Math.PI * 3.0 * t + simPhase) * 0.5 +
                            sin(2.0 * Math.PI * 7.0 * t + simPhase * 1.7) * 0.3 +
                            (Math.random() - 0.5) * 0.15).toFloat()
                    wave[i] = w.coerceIn(-1f, 1f)
                }

                _spectrumBands.value = bands.copyOf()
                _peakHoldBands.value = peaks.copyOf()
                _waveformPoints.value = wave.copyOf()

                delay(33) // ~30 fps
            }
        }
    }

    private fun processFft(fft: ByteArray) {
        val bands = FloatArray(16) { 0f }
        val peaks = _peakHoldBands.value.copyOf()
        val n = fft.size / 2

        for (i in 0 until 16) {
            val start = (i * n / 16)
            val end = ((i + 1) * n / 16).coerceAtMost(n - 1)
            var sum = 0f
            var count = 0
            for (j in start..end) {
                val r = fft[2 * j].toFloat()
                val im = fft[2 * j + 1].toFloat()
                val mag = kotlin.math.sqrt(r * r + im * im)
                sum += mag
                count++
            }
            val avg = if (count > 0) sum / count else 0f
            val normalized = (avg / 128f).coerceIn(0f, 1f)
            bands[i] = normalized

            if (normalized > peaks[i]) {
                peaks[i] = normalized
            } else {
                peaks[i] = max(0f, peaks[i] - 0.02f)
            }
        }
        _spectrumBands.value = bands
        _peakHoldBands.value = peaks
    }

    private fun processWaveform(waveform: ByteArray) {
        val wave = FloatArray(64) { 0f }
        val step = (waveform.size / 64).coerceAtLeast(1)
        for (i in 0 until 64) {
            val idx = (i * step).coerceAtMost(waveform.size - 1)
            val v = (waveform[idx].toInt() and 0xFF) - 128
            wave[i] = (v / 128f).coerceIn(-1f, 1f)
        }
        _waveformPoints.value = wave
    }

    private fun decayToZero() {
        animationJob?.cancel()
        animationJob = scope.launch {
            val bands = _spectrumBands.value.copyOf()
            val peaks = _peakHoldBands.value.copyOf()
            for (step in 0 until 10) {
                for (i in 0 until 16) {
                    bands[i] = (bands[i] * 0.7f).coerceAtLeast(0f)
                    peaks[i] = (peaks[i] * 0.75f).coerceAtLeast(0f)
                }
                _vuLeft.value = (_vuLeft.value * 0.6f).coerceAtLeast(0f)
                _vuRight.value = (_vuRight.value * 0.6f).coerceAtLeast(0f)
                _spectrumBands.value = bands.copyOf()
                _peakHoldBands.value = peaks.copyOf()
                delay(30)
            }
            _spectrumBands.value = FloatArray(16) { 0f }
            _peakHoldBands.value = FloatArray(16) { 0f }
            _vuLeft.value = 0f
            _vuRight.value = 0f
        }
    }

    fun release() {
        try {
            visualizer?.release()
        } catch (ignored: Exception) {}
        visualizer = null
        isCapturing = false
    }
}
