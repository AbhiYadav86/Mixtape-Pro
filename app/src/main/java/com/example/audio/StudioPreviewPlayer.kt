package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * The single audio output used by every DJ Studio section for previews, auditions and one-shots.
 * It plays 44.1 kHz stereo PCM (from a WAV we rendered, or an in-memory buffer) through an
 * AudioTrack and publishes a live spectrum for the studio's analyzer display.
 *
 * Whenever it starts, [onStart] is invoked — the app uses it to pause the main player
 * (song or radio) so the two never play over each other.
 */
object StudioPreviewPlayer {
    private const val SR = StudioRenderEngine.SAMPLE_RATE
    const val BANDS = 24

    data class State(
        val isPlaying: Boolean = false,
        val label: String = "",
        val positionMs: Long = 0L,
        val durationMs: Long = 0L
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _bands = MutableStateFlow(FloatArray(BANDS))
    val bands: StateFlow<FloatArray> = _bands.asStateFlow()

    @Volatile var onStart: (() -> Unit)? = null

    @Volatile private var generation = 0
    private var thread: Thread? = null

    fun playWav(file: File, label: String) {
        val data = readWav(file) ?: return
        playPcm(data, label)
    }

    fun playPcm(pcm: FloatArray, label: String) {
        stop()
        onStart?.invoke()
        val gen = ++generation
        val totalFrames = pcm.size / 2
        _state.value = State(true, label, 0L, totalFrames * 1000L / SR)
        thread = Thread({ run(pcm, gen, label) }, "StudioPreview").apply { start() }
    }

    fun stop() {
        generation++
        thread = null
        _state.value = _state.value.copy(isPlaying = false)
        _bands.value = FloatArray(BANDS)
    }

    private fun run(pcm: FloatArray, gen: Int, label: String) {
        val totalFrames = pcm.size / 2
        val block = 1024
        val minBuf = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SR)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(max(minBuf, block * 4 * 4))
                .build()
        } catch (e: Exception) {
            _state.value = State()
            return
        }

        val shorts = ShortArray(block * 2)
        val re = DoubleArray(2048); val im = DoubleArray(2048)
        val win = StudioDsp.hann(2048)
        val smooth = FloatArray(BANDS)
        val edges = IntArray(BANDS + 1) { b ->
            // log-spaced band edges 40 Hz .. 16 kHz in FFT bins
            val f = 40.0 * (16000.0 / 40.0).pow(b.toDouble() / BANDS)
            (f * 2048 / SR).toInt().coerceIn(1, 1023)
        }
        try {
            track.play()
            var pos = 0
            while (pos < totalFrames && gen == generation) {
                val n = min(block, totalFrames - pos)
                for (i in 0 until n * 2) shorts[i] = (pcm[pos * 2 + i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()

                // Spectrum of the block about to play (2048-point window centred on it)
                val start = (pos - 512).coerceAtLeast(0)
                for (k in 0 until 2048) {
                    val idx = start + k
                    val v = if (idx < totalFrames) (pcm[idx * 2] + pcm[idx * 2 + 1]) * 0.5 else 0f
                    re[k] = v.toDouble() * win[k]; im[k] = 0.0
                }
                StudioDsp.fft(re, im)
                val out = FloatArray(BANDS)
                for (b in 0 until BANDS) {
                    var peak = 0.0
                    for (k in edges[b]..max(edges[b], edges[b + 1] - 1)) {
                        val m = sqrt(re[k] * re[k] + im[k] * im[k])
                        if (m > peak) peak = m
                    }
                    val db = 20 * log10(peak / 512.0 + 1e-6)          // ~ dBFS
                    val level = ((db + 60) / 60).toFloat().coerceIn(0f, 1f)
                    smooth[b] = if (level > smooth[b]) level else smooth[b] * 0.82f + level * 0.18f
                    out[b] = smooth[b]
                }
                _bands.value = out

                track.write(shorts, 0, n * 2)
                pos += n
                _state.value = State(true, label, pos * 1000L / SR, totalFrames * 1000L / SR)
            }
            if (gen == generation) {
                // let the buffer drain
                Thread.sleep(150)
            }
        } catch (_: Exception) {
        } finally {
            try { track.stop() } catch (_: Exception) {}
            track.release()
            if (gen == generation) {
                _state.value = State(false, label, 0L, totalFrames * 1000L / SR)
                _bands.value = FloatArray(BANDS)
            }
        }
    }

    /** Reads the 16-bit stereo 44.1 kHz WAVs written by [StudioRenderEngine.WavFileSink]. */
    fun readWav(file: File): FloatArray? {
        return try {
            val bytes = file.readBytes()
            if (bytes.size <= 44) return null
            val bb = ByteBuffer.wrap(bytes, 44, bytes.size - 44).order(ByteOrder.LITTLE_ENDIAN)
            val count = (bytes.size - 44) / 2
            FloatArray(count) { bb.short / 32768f }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Records the microphone to a mono 16-bit 44.1 kHz WAV (for Mashup voice-over layers).
 * Requires the RECORD_AUDIO permission.
 */
object MicRecorder {
    private const val SR = StudioRenderEngine.SAMPLE_RATE

    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level.asStateFlow()

    private val _recordingMs = MutableStateFlow<Long?>(null)
    val recordingMs: StateFlow<Long?> = _recordingMs.asStateFlow()

    @Volatile private var active = false
    private var thread: Thread? = null

    @android.annotation.SuppressLint("MissingPermission")
    fun start(target: File): Boolean {
        if (active) return false
        val minBuf = AudioRecord.getMinBufferSize(SR, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, SR, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(minBuf, 8192))
        } catch (e: Exception) {
            return false
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return false }
        active = true
        _recordingMs.value = 0L
        thread = Thread({
            val buf = ShortArray(2048)
            var total = 0L
            FileOutputStream(target).use { out ->
                out.write(ByteArray(44))
                val bb = ByteBuffer.allocate(buf.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                rec.startRecording()
                while (active) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n <= 0) continue
                    bb.clear()
                    var peak = 0
                    for (i in 0 until n) { bb.putShort(buf[i]); peak = max(peak, kotlin.math.abs(buf[i].toInt())) }
                    out.write(bb.array(), 0, n * 2)
                    total += n
                    _level.value = peak / 32768f
                    _recordingMs.value = total * 1000 / SR
                }
                try { rec.stop() } catch (_: Exception) {}
                rec.release()
            }
            writeHeader(target, total)
            _level.value = 0f
            _recordingMs.value = null
        }, "MicRecorder").apply { start() }
        return true
    }

    fun stop() {
        active = false
        try { thread?.join(2000) } catch (_: Exception) {}
        thread = null
    }

    private fun writeHeader(file: File, frames: Long) {
        val dataLen = frames * 2
        RandomAccessFile(file, "rw").use { raf ->
            val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            h.put("RIFF".toByteArray(Charsets.US_ASCII)); h.putInt((36 + dataLen).toInt())
            h.put("WAVE".toByteArray(Charsets.US_ASCII)); h.put("fmt ".toByteArray(Charsets.US_ASCII))
            h.putInt(16); h.putShort(1); h.putShort(1); h.putInt(SR); h.putInt(SR * 2); h.putShort(2); h.putShort(16)
            h.put("data".toByteArray(Charsets.US_ASCII)); h.putInt(dataLen.toInt())
            raf.seek(0); raf.write(h.array())
        }
    }
}
