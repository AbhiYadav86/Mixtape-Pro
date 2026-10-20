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
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A preview being rendered to a 16-bit stereo 44.1 kHz WAV in the cache, which can be played
 * while it is still being written. The renderer calls [write]; the player reads whatever has
 * been written so far. Cancelling it makes the next [write] throw, which stops the renderer.
 */
class StudioPreviewBuffer(val file: File, @Volatile var expectedFrames: Long) {
    class Cancelled : RuntimeException("preview replaced")

    private val writer = RandomAccessFile(file, "rw").apply { setLength(0); write(ByteArray(44)) }
    private var reader: RandomAccessFile? = null
    private val bytes = ByteBuffer.allocate(4096 * 4).order(ByteOrder.LITTLE_ENDIAN)

    @Volatile var framesWritten = 0L
        private set
    @Volatile var complete = false
        private set
    @Volatile var cancelled = false
        private set

    /** Write interleaved stereo float frames. */
    fun write(data: FloatArray, frames: Int) {
        if (cancelled) throw Cancelled()
        var f = 0
        while (f < frames) {
            val n = min(frames - f, 4096)
            bytes.clear()
            for (i in 0 until n * 2) {
                val v = (data[(f * 2) + i].coerceIn(-1f, 1f) * 32767f).toInt()
                bytes.putShort(v.toShort())
            }
            synchronized(writer) {
                writer.seek(44 + framesWritten * 4)
                writer.write(bytes.array(), 0, n * 4)
            }
            framesWritten += n
            f += n
        }
    }

    /** Mark the render finished and write a proper WAV header. */
    fun finish() {
        if (complete) return
        complete = true
        try {
            val dataLen = framesWritten * 4
            val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            h.put("RIFF".toByteArray(Charsets.US_ASCII)); h.putInt((36 + dataLen).toInt())
            h.put("WAVE".toByteArray(Charsets.US_ASCII)); h.put("fmt ".toByteArray(Charsets.US_ASCII))
            h.putInt(16); h.putShort(1); h.putShort(2); h.putInt(44100); h.putInt(44100 * 4); h.putShort(4); h.putShort(16)
            h.put("data".toByteArray(Charsets.US_ASCII)); h.putInt(dataLen.toInt())
            synchronized(writer) { writer.seek(0); writer.write(h.array()); writer.close() }
        } catch (_: Exception) {}
    }

    /** Read up to [frames] frames of 16-bit PCM starting at [frame]. Returns frames read. */
    fun read(frame: Long, out: ByteArray, frames: Int): Int {
        val avail = framesWritten - frame
        if (avail <= 0) return 0
        val n = min(frames.toLong(), avail).toInt()
        val r = reader ?: RandomAccessFile(file, "r").also { reader = it }
        r.seek(44 + frame * 4)
        r.readFully(out, 0, n * 4)
        return n
    }

    fun cancelAndDelete() {
        cancelled = true
        complete = true
        try { reader?.close() } catch (_: Exception) {}
        reader = null
        try { synchronized(writer) { writer.close() } } catch (_: Exception) {}
        file.delete()
    }

    companion object {
        private var counter = 0
        fun create(cacheDir: File, expectedFrames: Long): StudioPreviewBuffer {
            val dir = File(cacheDir, "studio_preview").apply { mkdirs() }
            // clear leftovers from earlier sessions
            dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 60_000 }?.forEach { it.delete() }
            return StudioPreviewBuffer(File(dir, "preview_${System.currentTimeMillis()}_${counter++}.wav"), expectedFrames)
        }
    }
}

/**
 * The single audio output used by every DJ Studio section for previews.
 *
 * A preview is a timeline (e.g. a whole song with FX applied) of which [StudioPreviewBuffer]
 * holds the part starting at `bufferStartMs`. The player plays it with pause/resume and
 * seeking, and publishes a live spectrum for the studio monitor. Seeking outside the rendered
 * part asks the owner (via `restartAt`) to render again from there.
 *
 * Whenever it starts or resumes, [onStart] is invoked — the app uses it to pause the main
 * player (song or radio) so the two never play over each other.
 */
object StudioPreviewPlayer {
    private const val SR = StudioRenderEngine.SAMPLE_RATE
    const val BANDS = 24
    private const val BLOCK = 1024

    data class State(
        val loaded: Boolean = false,
        val isPlaying: Boolean = false,
        val isBuffering: Boolean = false,
        val label: String = "",
        /** Which studio section started this preview. */
        val section: String = "",
        /** Identifies what is being previewed (e.g. a song uri) so A/B switches keep position. */
        val sourceKey: String = "",
        /** Timeline ms per source-file ms (≠ 1 when FX change the speed). */
        val timeScale: Double = 1.0,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L,
        val bufferStartMs: Long = 0L,
        val bufferEndMs: Long = 0L,
        val renderComplete: Boolean = false,
        val markers: List<Long> = emptyList(),
        val canSeekAnywhere: Boolean = false,
        /** Names of the aligned versions you can switch between instantly (e.g. Original / Instrumental / Vocals). */
        val stems: List<String> = emptyList(),
        val activeStem: Int = 0
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _bands = MutableStateFlow(FloatArray(BANDS))
    val bands: StateFlow<FloatArray> = _bands.asStateFlow()

    @Volatile var onStart: (() -> Unit)? = null

    private class Session(
        val buffers: List<StudioPreviewBuffer>,
        val stems: List<String>,
        val label: String,
        val section: String,
        val sourceKey: String,
        val timeScale: Double,
        val startMs: Long,
        val durationMs: Long,
        val markers: List<Long>,
        val restartAt: ((Long) -> Unit)?
    ) {
        @Volatile var playing = true
        @Volatile var ended = false
        @Volatile var active = 0
        /** Frames ready in every stem. */
        fun available(): Long = buffers.minOf { it.framesWritten }
        fun complete(): Boolean = buffers.all { it.complete }
        val expectedFrames: Long get() = buffers[0].expectedFrames
        fun cancel() = buffers.forEach { it.cancelAndDelete() }
        val seekFrame = AtomicLong(-1L)
        @Volatile var pos = 0L
    }

    @Volatile private var session: Session? = null
    @Volatile private var generation = 0

    /**
     * Start playing [buffer] (which is being rendered) as the part of a [durationMs]-long timeline
     * that starts at [startMs]. Replaces whatever was loaded.
     */
    fun load(
        buffer: StudioPreviewBuffer,
        label: String,
        section: String,
        startMs: Long,
        durationMs: Long,
        markers: List<Long> = emptyList(),
        sourceKey: String = "",
        timeScale: Double = 1.0,
        autoPlay: Boolean = true,
        restartAt: ((Long) -> Unit)? = null
    ) = loadStems(listOf(buffer), emptyList(), 0, label, section, startMs, durationMs, markers, sourceKey, timeScale, autoPlay, restartAt)

    /**
     * Like [load], with several sample-aligned versions of the same timeline (one buffer each);
     * [setStem] switches between them instantly while playing.
     */
    fun loadStems(
        buffers: List<StudioPreviewBuffer>,
        stems: List<String>,
        activeStem: Int,
        label: String,
        section: String,
        startMs: Long,
        durationMs: Long,
        markers: List<Long> = emptyList(),
        sourceKey: String = "",
        timeScale: Double = 1.0,
        autoPlay: Boolean = true,
        restartAt: ((Long) -> Unit)? = null
    ) {
        val old = session
        val s = Session(buffers, stems, label, section, sourceKey, timeScale, startMs, max(durationMs, startMs), markers, restartAt)
        s.playing = autoPlay
        s.active = activeStem.coerceIn(0, buffers.lastIndex)
        val gen = ++generation
        session = s
        old?.cancel()
        if (autoPlay) onStart?.invoke()
        publish(s, buffering = true)
        Thread({ run(s, gen) }, "StudioPreview").apply { priority = Thread.MAX_PRIORITY; start() }
    }

    /** Switch to another stem of the current preview at the same moment (no re-render). */
    fun setStem(index: Int) {
        val s = session ?: return
        if (index !in s.buffers.indices || index == s.active) return
        s.active = index
        publish(s)
    }

    fun stop() {
        generation++
        session?.cancel()
        session = null
        _state.value = State()
        _bands.value = FloatArray(BANDS)
    }

    /** Stop only if the current preview belongs to [section]. */
    fun stopIfSection(section: String) {
        if (session?.section == section) stop()
    }

    val isActive: Boolean get() = session?.playing == true

    fun togglePlayPause() {
        val s = session ?: return
        if (s.playing) pause() else resume()
    }

    fun pause() {
        val s = session ?: return
        s.playing = false
        publish(s)
    }

    fun resume() {
        val s = session ?: return
        if (s.ended) {
            s.ended = false
            seekTo(0L)
            s.playing = true
            onStart?.invoke()
            session?.let { it.playing = true; publish(it) }
            return
        }
        s.playing = true
        onStart?.invoke()
        publish(s)
    }

    /** Current position on the timeline. */
    fun positionMs(): Long = session?.let { it.startMs + it.pos * 1000 / SR } ?: 0L

    fun seekBy(deltaMs: Long) = seekTo(positionMs() + deltaMs)

    /** Seek to a position on the timeline (ms). */
    fun seekTo(targetMs: Long) {
        val s = session ?: return
        val t = targetMs.coerceIn(0L, max(0L, s.durationMs - 250))
        val bufferedEnd = s.startMs + s.available() * 1000 / SR
        val inside = t >= s.startMs && (t <= bufferedEnd || (!s.complete() && t <= bufferedEnd + 4000))
        when {
            inside -> {
                s.ended = false
                s.seekFrame.set((t - s.startMs) * SR / 1000)
            }
            s.restartAt != null -> s.restartAt.invoke(t)
            t < s.startMs -> s.seekFrame.set(0L)
            else -> {
                // can't render out of order: jump as far as has been rendered
                s.ended = false
                s.seekFrame.set(max(0L, s.available() - SR / 2))
            }
        }
    }

    private fun publish(s: Session, buffering: Boolean = false) {
        if (session !== s) return
        val written = s.available()
        val complete = s.complete()
        val durationMs = if (complete && s.restartAt == null && s.startMs == 0L) written * 1000 / SR else s.durationMs
        _state.value = State(
            loaded = true,
            isPlaying = s.playing,
            isBuffering = buffering && s.playing,
            label = s.label,
            section = s.section,
            sourceKey = s.sourceKey,
            timeScale = s.timeScale,
            positionMs = (s.startMs + s.pos * 1000 / SR).coerceAtMost(max(durationMs, 0L)),
            durationMs = durationMs,
            bufferStartMs = s.startMs,
            bufferEndMs = s.startMs + written * 1000 / SR,
            renderComplete = complete,
            markers = s.markers,
            canSeekAnywhere = s.restartAt != null,
            stems = s.stems,
            activeStem = s.active
        )
    }

    private fun run(s: Session, gen: Int) {
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
                .setBufferSizeInBytes(max(minBuf, BLOCK * 4 * 4))
                .build()
        } catch (e: Exception) {
            if (gen == generation) stop()
            return
        }

        val bytes = ByteArray(BLOCK * 4)
        val prevBytes = ByteArray(BLOCK * 4)
        var playedStem = s.active
        val mono = FloatArray(2048) // rolling window of the most recent samples
        val re = DoubleArray(2048); val im = DoubleArray(2048)
        val win = StudioDsp.hann(2048)
        val smooth = FloatArray(BANDS)
        val edges = IntArray(BANDS + 1) { b ->
            val f = 40.0 * (16000.0 / 40.0).pow(b.toDouble() / BANDS)
            (f * 2048 / SR).toInt().coerceIn(1, 1023)
        }
        var trackRunning = false
        var lastPublish = 0L
        try {
            while (gen == generation) {
                val seek = s.seekFrame.getAndSet(-1L)
                if (seek >= 0) {
                    s.pos = seek
                    if (trackRunning) { track.pause(); track.flush(); trackRunning = false }
                    java.util.Arrays.fill(mono, 0f)
                    publish(s)
                }
                if (!s.playing) {
                    if (trackRunning) { track.pause(); trackRunning = false }
                    var any = false
                    for (b in 0 until BANDS) { smooth[b] *= 0.8f; if (smooth[b] > 0.01f) any = true }
                    _bands.value = if (any) smooth.copyOf() else FloatArray(BANDS)
                    Thread.sleep(25)
                    continue
                }
                val stem = s.active
                val avail = s.available() - s.pos
                val n = if (avail <= 0) 0 else s.buffers[stem].read(s.pos, bytes, min(BLOCK.toLong(), avail).toInt())
                if (n > 0 && stem != playedStem) {
                    // switching stems: short crossfade from the old version so there's no click
                    val m = s.buffers[playedStem].read(s.pos, prevBytes, n)
                    for (i in 0 until m * 2) {
                        val g = (i / 2).toFloat() / n
                        val a = ((prevBytes[i * 2].toInt() and 0xFF) or (prevBytes[i * 2 + 1].toInt() shl 8)).toShort()
                        val b = ((bytes[i * 2].toInt() and 0xFF) or (bytes[i * 2 + 1].toInt() shl 8)).toShort()
                        val v = (a * (1f - g) + b * g).toInt()
                        bytes[i * 2] = (v and 0xFF).toByte(); bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
                    }
                }
                playedStem = stem
                if (n <= 0) {
                    if (s.complete()) {
                        // reached the end: let the hardware buffer drain, then rewind
                        Thread.sleep(120)
                        if (gen != generation) break
                        if (trackRunning) { track.pause(); track.flush(); trackRunning = false }
                        s.playing = false
                        s.ended = true
                        _bands.value = FloatArray(BANDS)
                        publish(s)
                    } else {
                        publish(s, buffering = true)
                        Thread.sleep(25)
                    }
                    continue
                }
                if (!trackRunning) { track.play(); trackRunning = true }

                // spectrum of the newest 2048 samples
                System.arraycopy(mono, n, mono, 0, 2048 - n)
                for (i in 0 until n) {
                    val l = ((bytes[i * 4].toInt() and 0xFF) or (bytes[i * 4 + 1].toInt() shl 8)).toShort()
                    val r = ((bytes[i * 4 + 2].toInt() and 0xFF) or (bytes[i * 4 + 3].toInt() shl 8)).toShort()
                    mono[2048 - n + i] = (l + r) / 65536f
                }
                for (k in 0 until 2048) { re[k] = mono[k] * win[k]; im[k] = 0.0 }
                StudioDsp.fft(re, im)
                val out = FloatArray(BANDS)
                for (b in 0 until BANDS) {
                    var peak = 0.0
                    for (k in edges[b]..max(edges[b], edges[b + 1] - 1)) {
                        val m = sqrt(re[k] * re[k] + im[k] * im[k])
                        if (m > peak) peak = m
                    }
                    val db = 20 * log10(peak / 512.0 + 1e-6)
                    val level = ((db + 60) / 60).toFloat().coerceIn(0f, 1f)
                    smooth[b] = if (level > smooth[b]) level else smooth[b] * 0.82f + level * 0.18f
                    out[b] = smooth[b]
                }
                _bands.value = out

                track.write(bytes, 0, n * 4)
                s.pos += n
                val now = System.currentTimeMillis()
                if (now - lastPublish > 80) { publish(s); lastPublish = now }
            }
        } catch (_: InterruptedException) {
        } catch (_: Exception) {
        } finally {
            try { track.stop() } catch (_: Exception) {}
            track.release()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // One-shot pads: fire-and-forget, so they can be played over a running preview.
    // ---------------------------------------------------------------------------------------------

    fun playOneShot(pcm: FloatArray) {
        if (pcm.isEmpty()) return
        if (session?.playing != true) onStart?.invoke()
        Thread({
            val shorts = ShortArray(pcm.size) { (pcm[it].coerceIn(-1f, 1f) * 32767f).toInt().toShort() }
            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SR).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(shorts.size * 2)
                    .build()
            } catch (e: Exception) { return@Thread }
            try {
                track.write(shorts, 0, shorts.size)
                track.play()
                Thread.sleep(shorts.size / 2 * 1000L / SR + 100)
            } catch (_: Exception) {
            } finally {
                try { track.stop() } catch (_: Exception) {}
                track.release()
            }
        }, "OneShot").start()
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
