package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Real offline audio rendering for DJ Studio:
 *  - decodes any audio the phone can play (MP3, AAC, FLAC, OGG, WAV…) with MediaCodec
 *  - trims, splices with crossfades, applies gain / fades / noise gate / vocal removal
 *  - writes a real file (WAV master or AAC .m4a) into the phone's Music or Ringtones folder
 *
 * All work is blocking; call it from a background dispatcher.
 */
object StudioRenderEngine {

    private const val TAG = "StudioRender"
    const val SAMPLE_RATE = 44100
    private const val CHANNELS = 2

    enum class OutputFormat(val extension: String, val mime: String) {
        WAV("wav", "audio/x-wav"),
        AAC("m4a", "audio/mp4")
    }

    enum class Destination { MUSIC, RINGTONES }

    /** One piece of source audio to render. Times are in milliseconds of the source file. */
    data class RenderPart(
        val uri: String,
        val startMs: Long = 0L,
        val endMs: Long = -1L, // -1 = until the end of the file
        val gain: Float = 1.0f,
        val noiseGateDb: Float? = null
    )

    data class RenderResult(
        val uri: Uri,
        val displayName: String,
        val durationMs: Long,
        val format: OutputFormat
    )

    class RenderException(message: String, cause: Throwable? = null) : Exception(message, cause)

    // =============================================================================================
    // Public operations
    // =============================================================================================

    /** Splice parts end-to-end with an equal-power crossfade between them (0 = hard cut). */
    fun renderSplice(
        context: Context,
        parts: List<RenderPart>,
        crossfadeMs: Long,
        title: String,
        format: OutputFormat = OutputFormat.WAV,
        onProgress: (Float) -> Unit = {}
    ): RenderResult {
        if (parts.isEmpty()) throw RenderException("Nothing to export — add at least one track with audio.")
        val expectedFrames = parts.sumOf { estimateFrames(context, it) }.coerceAtLeast(1L)
        val xfadeFrames = (crossfadeMs * SAMPLE_RATE / 1000L).toInt().coerceAtLeast(0)

        return writeOutput(context, title, format, Destination.MUSIC) { sink ->
            var written = 0L
            var pendingTail = FloatArray(0) // stereo interleaved tail kept back for the next crossfade
            val chunk = FloatArray(4096 * CHANNELS)

            parts.forEachIndexed { index, part ->
                val source = DecodedPcmSource(context, Uri.parse(part.uri), part.startMs, part.endMs)
                try {
                    val gate = part.noiseGateDb?.let { NoiseGate(it) }
                    val isLast = index == parts.lastIndex
                    val keepFrames = if (isLast) 0 else xfadeFrames
                    val tailRing = FrameRing(keepFrames)

                    // 1. Crossfade the previous part's tail with this part's head
                    if (pendingTail.isNotEmpty()) {
                        val tailFrames = pendingTail.size / CHANNELS
                        val head = FloatArray(pendingTail.size)
                        var got = 0
                        while (got < tailFrames) {
                            val n = source.read(head, got, tailFrames - got)
                            if (n <= 0) break
                            got += n
                        }
                        applyGain(head, got, part.gain)
                        gate?.process(head, got)
                        for (f in 0 until tailFrames) {
                            val t = (f + 0.5f) / tailFrames
                            val fadeOut = cos(t * PI / 2).toFloat()
                            val fadeIn = sin(t * PI / 2).toFloat()
                            for (c in 0 until CHANNELS) {
                                val i = f * CHANNELS + c
                                val incoming = if (f < got) head[i] else 0f
                                head[i] = pendingTail[i] * fadeOut + incoming * fadeIn
                            }
                        }
                        // If this part is shorter than the crossfade, treat the blend as its tail.
                        if (got < tailFrames && !isLast) {
                            tailRing.push(head, tailFrames) { out, n -> sink.write(out, n); written += n }
                        } else {
                            sink.write(head, tailFrames)
                            written += tailFrames
                        }
                        pendingTail = FloatArray(0)
                    }

                    // 2. Stream the rest of the part, holding back the last [keepFrames] for the next crossfade
                    while (true) {
                        val n = source.read(chunk, 0, chunk.size / CHANNELS)
                        if (n <= 0) break
                        applyGain(chunk, n, part.gain)
                        gate?.process(chunk, n)
                        if (keepFrames > 0) {
                            tailRing.push(chunk, n) { out, m -> sink.write(out, m); written += m }
                        } else {
                            sink.write(chunk, n)
                            written += n
                        }
                        onProgress((written.toFloat() / expectedFrames).coerceIn(0f, 0.99f))
                    }
                    if (keepFrames > 0) pendingTail = tailRing.drain()
                } finally {
                    source.release()
                }
            }
            if (pendingTail.isNotEmpty()) {
                sink.write(pendingTail, pendingTail.size / CHANNELS)
            }
            onProgress(1f)
        }
    }

    /** Cut a ringtone: [startMs, endMs] with fade in/out, saved as AAC into Ringtones/Mixtape Pro. */
    fun renderRingtone(
        context: Context,
        sourceUri: String,
        startMs: Long,
        endMs: Long,
        fadeInMs: Long,
        fadeOutMs: Long,
        title: String,
        onProgress: (Float) -> Unit = {}
    ): RenderResult {
        if (endMs <= startMs) throw RenderException("Pick an end point after the start point.")
        val totalFrames = ((endMs - startMs) * SAMPLE_RATE / 1000L).coerceAtLeast(1L)
        val fadeInFrames = (fadeInMs * SAMPLE_RATE / 1000L)
        val fadeOutFrames = (fadeOutMs * SAMPLE_RATE / 1000L)

        return writeOutput(context, title, OutputFormat.AAC, Destination.RINGTONES) { sink ->
            val source = DecodedPcmSource(context, Uri.parse(sourceUri), startMs, endMs)
            try {
                val chunk = FloatArray(4096 * CHANNELS)
                var pos = 0L
                while (true) {
                    val n = source.read(chunk, 0, chunk.size / CHANNELS)
                    if (n <= 0) break
                    for (f in 0 until n) {
                        val frameIndex = pos + f
                        var g = 1f
                        if (fadeInFrames > 0 && frameIndex < fadeInFrames) g *= frameIndex.toFloat() / fadeInFrames
                        val fromEnd = totalFrames - frameIndex
                        if (fadeOutFrames > 0 && fromEnd < fadeOutFrames) g *= (fromEnd.toFloat() / fadeOutFrames).coerceIn(0f, 1f)
                        chunk[f * 2] *= g
                        chunk[f * 2 + 1] *= g
                    }
                    sink.write(chunk, n)
                    pos += n
                    onProgress((pos.toFloat() / totalFrames).coerceIn(0f, 0.99f))
                }
            } finally {
                source.release()
            }
            onProgress(1f)
        }
    }

    /**
     * Karaoke-style vocal remover. Lead vocals are usually mixed dead-centre, so removing the
     * centre (mid) channel removes most of the voice. Bass and kick drum are also centred, so the
     * mid channel below ~[bassKeepHz] is kept to avoid a thin, bass-less result.
     * [strength] 0..1 controls how much of the centre is removed.
     */
    fun renderVocalRemoval(
        context: Context,
        sourceUri: String,
        strength: Float,
        bassKeepHz: Float,
        title: String,
        format: OutputFormat = OutputFormat.WAV,
        onProgress: (Float) -> Unit = {}
    ): RenderResult {
        val expected = estimateFrames(context, RenderPart(sourceUri)).coerceAtLeast(1L)
        val s = strength.coerceIn(0f, 1f)
        // One-pole low-pass coefficient for the kept bass band
        val alpha = (1.0 - exp(-2.0 * PI * bassKeepHz / SAMPLE_RATE)).toFloat()

        return writeOutput(context, title, format, Destination.MUSIC) { sink ->
            val source = DecodedPcmSource(context, Uri.parse(sourceUri), 0L, -1L)
            try {
                val chunk = FloatArray(4096 * CHANNELS)
                var lp = 0f
                var done = 0L
                var monoWarned = false
                while (true) {
                    val n = source.read(chunk, 0, chunk.size / CHANNELS)
                    if (n <= 0) break
                    if (source.sourceChannels < 2 && !monoWarned) {
                        Log.w(TAG, "Mono source: vocal removal cannot separate the centre channel")
                        monoWarned = true
                    }
                    for (f in 0 until n) {
                        val l = chunk[f * 2]
                        val r = chunk[f * 2 + 1]
                        val mid = (l + r) * 0.5f
                        val side = (l - r) * 0.5f
                        lp += alpha * (mid - lp)
                        val keptMid = lp + (1f - s) * (mid - lp)
                        // Side carries the stereo instruments; boost slightly to compensate loudness
                        val sideGain = 1f + 0.4f * s
                        chunk[f * 2] = keptMid + side * sideGain
                        chunk[f * 2 + 1] = keptMid - side * sideGain
                    }
                    sink.write(chunk, n)
                    done += n
                    onProgress((done.toFloat() / expected).coerceIn(0f, 0.99f))
                }
            } finally {
                source.release()
            }
            onProgress(1f)
        }
    }

    /** Reads the duration of an audio file in ms (0 if unknown). */
    fun durationMs(context: Context, uri: String): Long {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, Uri.parse(uri), null)
            val track = findAudioTrack(extractor)
            if (track < 0) 0L else {
                val fmt = extractor.getTrackFormat(track)
                if (fmt.containsKey(MediaFormat.KEY_DURATION)) fmt.getLong(MediaFormat.KEY_DURATION) / 1000L else 0L
            }
        } catch (e: Exception) {
            0L
        } finally {
            extractor.release()
        }
    }

    // =============================================================================================
    // Output
    // =============================================================================================

    private interface PcmSink {
        /** Write [frames] interleaved stereo float frames from [data]. */
        fun write(data: FloatArray, frames: Int)
    }

    private fun writeOutput(
        context: Context,
        title: String,
        format: OutputFormat,
        destination: Destination,
        render: (PcmSink) -> Unit
    ): RenderResult {
        val safeTitle = title.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { "Mixtape Pro Export" }.take(80)
        val displayName = "$safeTitle.${format.extension}"
        val temp = File(context.cacheDir, "render_${System.currentTimeMillis()}.${format.extension}")

        val frames: Long
        try {
            frames = when (format) {
                OutputFormat.WAV -> WavFileSink(temp).use { sink -> render(sink); sink.framesWritten }
                OutputFormat.AAC -> AacFileSink(temp).use { sink -> render(sink); sink.framesWritten }
            }
            if (frames <= 0L) throw RenderException("The source audio could not be decoded.")
            val uri = publish(context, temp, displayName, format, destination, safeTitle)
            return RenderResult(uri, displayName, frames * 1000L / SAMPLE_RATE, format)
        } catch (e: RenderException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Render failed", e)
            throw RenderException("Export failed: ${e.message ?: e.javaClass.simpleName}", e)
        } finally {
            temp.delete()
        }
    }

    /** Copy the finished temp file into shared storage so other apps (and the Library scan) can see it. */
    private fun publish(
        context: Context,
        temp: File,
        displayName: String,
        format: OutputFormat,
        destination: Destination,
        title: String
    ): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val relative = when (destination) {
                Destination.MUSIC -> Environment.DIRECTORY_MUSIC + "/Mixtape Pro"
                Destination.RINGTONES -> Environment.DIRECTORY_RINGTONES + "/Mixtape Pro"
            }
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, "Mixtape Pro Studio")
                put(MediaStore.Audio.Media.MIME_TYPE, format.mime)
                put(MediaStore.Audio.Media.RELATIVE_PATH, relative)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
                if (destination == Destination.RINGTONES) {
                    put(MediaStore.Audio.Media.IS_RINGTONE, 1)
                    put(MediaStore.Audio.Media.IS_NOTIFICATION, 1)
                    put(MediaStore.Audio.Media.IS_MUSIC, 0)
                } else {
                    put(MediaStore.Audio.Media.IS_MUSIC, 1)
                }
            }
            val resolver = context.contentResolver
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values) ?: throw RenderException("Could not create the output file.")
            resolver.openOutputStream(uri)?.use { out -> temp.inputStream().use { it.copyTo(out) } }
                ?: throw RenderException("Could not write the output file.")
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } else {
            // Android 7–9: app-specific music folder (no storage permission needed)
            val dirType = if (destination == Destination.RINGTONES) Environment.DIRECTORY_RINGTONES else Environment.DIRECTORY_MUSIC
            val dir = File(context.getExternalFilesDir(dirType), "Mixtape Pro").apply { mkdirs() }
            var target = File(dir, displayName)
            var i = 1
            while (target.exists()) {
                target = File(dir, displayName.substringBeforeLast('.') + " ($i)." + format.extension)
                i++
            }
            temp.copyTo(target)
            android.media.MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(format.mime), null)
            return Uri.fromFile(target)
        }
    }

    private class WavFileSink(private val file: File) : PcmSink, AutoCloseable {
        private val out = FileOutputStream(file).buffered(1 shl 16)
        private val bytes = ByteBuffer.allocate(4096 * CHANNELS * 2).order(ByteOrder.LITTLE_ENDIAN)
        var framesWritten = 0L
            private set

        init {
            out.write(ByteArray(44)) // header placeholder
        }

        override fun write(data: FloatArray, frames: Int) {
            var offset = 0
            val total = frames * CHANNELS
            while (offset < total) {
                bytes.clear()
                val n = min(total - offset, bytes.capacity() / 2)
                for (i in 0 until n) {
                    bytes.putShort(floatToPcm16(data[offset + i]))
                }
                out.write(bytes.array(), 0, n * 2)
                offset += n
            }
            framesWritten += frames
        }

        override fun close() {
            out.flush()
            out.close()
            val dataLen = framesWritten * CHANNELS * 2
            RandomAccessFile(file, "rw").use { raf ->
                val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                h.put("RIFF".toByteArray(Charsets.US_ASCII))
                h.putInt((36 + dataLen).toInt())
                h.put("WAVE".toByteArray(Charsets.US_ASCII))
                h.put("fmt ".toByteArray(Charsets.US_ASCII))
                h.putInt(16)
                h.putShort(1) // PCM
                h.putShort(CHANNELS.toShort())
                h.putInt(SAMPLE_RATE)
                h.putInt(SAMPLE_RATE * CHANNELS * 2)
                h.putShort((CHANNELS * 2).toShort())
                h.putShort(16)
                h.put("data".toByteArray(Charsets.US_ASCII))
                h.putInt(dataLen.toInt())
                raf.seek(0)
                raf.write(h.array())
            }
        }
    }

    /** AAC-LC encoder into an .m4a container. */
    private class AacFileSink(file: File) : PcmSink, AutoCloseable {
        private val codec: MediaCodec
        private val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        private var trackIndex = -1
        private var muxerStarted = false
        private var presentationFrames = 0L
        private val info = MediaCodec.BufferInfo()
        var framesWritten = 0L
            private set

        init {
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNELS).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 192_000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
        }

        override fun write(data: FloatArray, frames: Int) {
            var offsetFrames = 0
            while (offsetFrames < frames) {
                val inIndex = codec.dequeueInputBuffer(10_000)
                if (inIndex >= 0) {
                    val buf = codec.getInputBuffer(inIndex)!!
                    buf.clear()
                    buf.order(ByteOrder.LITTLE_ENDIAN)
                    val capFrames = buf.capacity() / (CHANNELS * 2)
                    val n = min(capFrames, frames - offsetFrames)
                    for (i in 0 until n * CHANNELS) {
                        buf.putShort(floatToPcm16(data[(offsetFrames * CHANNELS) + i]))
                    }
                    val ptsUs = presentationFrames * 1_000_000L / SAMPLE_RATE
                    codec.queueInputBuffer(inIndex, 0, n * CHANNELS * 2, ptsUs, 0)
                    presentationFrames += n
                    offsetFrames += n
                }
                drain(false)
            }
            framesWritten += frames
        }

        private fun drain(endOfStream: Boolean) {
            var idleTries = 0
            while (true) {
                val outIndex = codec.dequeueOutputBuffer(info, if (endOfStream) 10_000 else 0)
                when {
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (!endOfStream) return
                        if (++idleTries > 300) return // encoder never signalled EOS; give up after ~3s
                    }
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outIndex >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIndex)!!
                        if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) info.size = 0
                        if (info.size > 0 && muxerStarted) {
                            outBuf.position(info.offset)
                            outBuf.limit(info.offset + info.size)
                            muxer.writeSampleData(trackIndex, outBuf, info)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return
                    }
                }
            }
        }

        override fun close() {
            try {
                var queued = false
                var attempts = 0
                while (!queued && attempts < 100) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val ptsUs = presentationFrames * 1_000_000L / SAMPLE_RATE
                        codec.queueInputBuffer(inIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        queued = true
                    } else {
                        drain(false)
                    }
                    attempts++
                }
                drain(true)
            } finally {
                try { codec.stop() } catch (_: Exception) {}
                codec.release()
                try { if (muxerStarted) muxer.stop() } catch (_: Exception) {}
                muxer.release()
            }
        }
    }

    // =============================================================================================
    // Decoding
    // =============================================================================================

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return -1
    }

    private fun estimateFrames(context: Context, part: RenderPart): Long {
        val total = durationMs(context, part.uri)
        val end = if (part.endMs > 0) part.endMs else total
        return ((end - part.startMs).coerceAtLeast(0L)) * SAMPLE_RATE / 1000L
    }

    /**
     * Pull-based decoder: produces interleaved stereo float frames at 44.1 kHz for the
     * range [startMs, endMs] of any file MediaExtractor understands. Mono is duplicated to
     * both channels, multichannel is down-mixed, other sample rates are linearly resampled.
     */
    private class DecodedPcmSource(
        context: Context,
        uri: Uri,
        startMs: Long,
        endMs: Long
    ) {
        private val extractor = MediaExtractor()
        private val codec: MediaCodec
        private val srcRate: Int
        var sourceChannels: Int
            private set
        private var pcmFloat = false
        private val startUs = startMs.coerceAtLeast(0L) * 1000L
        private val maxOutFrames: Long
        private var outFrames = 0L
        private var inputDone = false
        private var outputDone = false
        private val info = MediaCodec.BufferInfo()

        // Decoded frames at the source rate, stereo interleaved
        private var srcBuf = FloatArray(16384)
        private var srcFrames = 0
        private var srcPos = 0.0 // fractional read position (frames) into srcBuf
        private val step: Double

        init {
            try {
                extractor.setDataSource(context, uri, null)
            } catch (e: Exception) {
                extractor.release()
                throw RenderException("Can't open this audio file (is it still on the phone?)", e)
            }
            val track = findAudioTrack(extractor)
            if (track < 0) {
                extractor.release()
                throw RenderException("No audio track found in this file.")
            }
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            srcRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            sourceChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()
            if (startUs > 0) extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            step = srcRate.toDouble() / SAMPLE_RATE
            maxOutFrames = if (endMs > 0 && endMs > startMs) (endMs - startMs) * SAMPLE_RATE / 1000L else Long.MAX_VALUE
        }

        /** Read up to [maxFrames] frames into [out] starting at frame [offsetFrames]. Returns frames read, 0 at end. */
        fun read(out: FloatArray, offsetFrames: Int, maxFrames: Int): Int {
            var produced = 0
            val limit = min(maxFrames.toLong(), maxOutFrames - outFrames).toInt()
            while (produced < limit) {
                val idx = srcPos.toInt()
                if (idx + 1 >= srcFrames) {
                    if (!decodeMore()) {
                        // Flush the very last frame without interpolation
                        if (idx < srcFrames) {
                            val o = (offsetFrames + produced) * 2
                            out[o] = srcBuf[idx * 2]
                            out[o + 1] = srcBuf[idx * 2 + 1]
                            produced++
                            srcPos += step
                        }
                        break
                    }
                    continue
                }
                val frac = (srcPos - idx).toFloat()
                val a = idx * 2
                val b = a + 2
                val o = (offsetFrames + produced) * 2
                out[o] = srcBuf[a] + (srcBuf[b] - srcBuf[a]) * frac
                out[o + 1] = srcBuf[a + 1] + (srcBuf[b + 1] - srcBuf[a + 1]) * frac
                produced++
                srcPos += step
            }
            outFrames += produced
            return produced
        }

        /** Decode the next chunk into srcBuf (compacting consumed frames). Returns false at end of stream. */
        private fun decodeMore(): Boolean {
            // Compact: keep from floor(srcPos) onward
            val keepFrom = srcPos.toInt().coerceAtMost(srcFrames)
            if (keepFrom > 0) {
                val remaining = srcFrames - keepFrom
                System.arraycopy(srcBuf, keepFrom * 2, srcBuf, 0, remaining * 2)
                srcFrames = remaining
                srcPos -= keepFrom
            }

            var guard = 0
            while (!outputDone && guard < 200) {
                guard++
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(5_000)
                    if (inIndex >= 0) {
                        val buf = codec.getInputBuffer(inIndex)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(info, 5_000)
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val f = codec.outputFormat
                    if (f.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) sourceChannels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && f.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        pcmFloat = f.getInteger(MediaFormat.KEY_PCM_ENCODING) == android.media.AudioFormat.ENCODING_PCM_FLOAT
                    }
                    continue
                }
                if (outIndex < 0) continue

                val outBuf = codec.getOutputBuffer(outIndex)
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
                var appended = 0
                if (outBuf != null && info.size > 0) {
                    outBuf.position(info.offset)
                    outBuf.limit(info.offset + info.size)
                    appended = appendPcm(outBuf.slice().order(ByteOrder.nativeOrder()), info.presentationTimeUs)
                }
                codec.releaseOutputBuffer(outIndex, false)
                if (appended > 0) return true
            }
            return srcFrames - srcPos.toInt() > 1 && !outputDone
        }

        private fun appendPcm(buf: ByteBuffer, ptsUs: Long): Int {
            val ch = sourceChannels.coerceAtLeast(1)
            val bytesPerSample = if (pcmFloat) 4 else 2
            val frames = buf.remaining() / (bytesPerSample * ch)
            if (frames <= 0) return 0

            // Skip decoded audio that lies before the requested start time
            var skip = 0
            if (ptsUs < startUs) {
                skip = ((startUs - ptsUs) * srcRate / 1_000_000L).toInt().coerceIn(0, frames)
            }
            val keep = frames - skip
            if (keep <= 0) return 0

            ensureCapacity(srcFrames + keep)
            val readSample: () -> Float = if (pcmFloat) {
                { buf.getFloat() }
            } else {
                { buf.getShort() / 32768f }
            }
            // advance past skipped frames
            repeat(skip * ch) { readSample() }
            for (f in 0 until keep) {
                var l: Float
                var r: Float
                if (ch == 1) {
                    l = readSample(); r = l
                } else {
                    l = readSample(); r = readSample()
                    if (ch > 2) {
                        // Simple down-mix: fold extra channels in at half level
                        var extra = 0f
                        repeat(ch - 2) { extra += readSample() }
                        val e = extra / (ch - 2) * 0.5f
                        l += e; r += e
                    }
                }
                val o = (srcFrames + f) * 2
                srcBuf[o] = l
                srcBuf[o + 1] = r
            }
            srcFrames += keep
            return keep
        }

        private fun ensureCapacity(frames: Int) {
            if (frames * 2 > srcBuf.size) {
                srcBuf = srcBuf.copyOf(max(frames * 2, srcBuf.size * 2))
            }
        }

        fun release() {
            try { codec.stop() } catch (_: Exception) {}
            try { codec.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }

    // =============================================================================================
    // DSP helpers
    // =============================================================================================

    private fun floatToPcm16(v: Float): Short {
        val clamped = when {
            v > 1f -> 1f
            v < -1f -> -1f
            else -> v
        }
        return (clamped * 32767f).toInt().toShort()
    }

    private fun applyGain(data: FloatArray, frames: Int, gain: Float) {
        if (gain == 1f) return
        for (i in 0 until frames * CHANNELS) data[i] *= gain
    }

    /** Simple downward noise gate with smoothing — removes hiss/room noise between phrases. */
    private class NoiseGate(thresholdDb: Float) {
        private val threshold = Math.pow(10.0, thresholdDb / 20.0).toFloat()
        private var env = 0f
        private var gain = 1f

        fun process(data: FloatArray, frames: Int) {
            for (f in 0 until frames) {
                val l = data[f * 2]
                val r = data[f * 2 + 1]
                val level = max(abs(l), abs(r))
                env = if (level > env) level else env * 0.9995f
                val target = if (env >= threshold) 1f else 0.05f
                gain += (target - gain) * (if (target > gain) 0.01f else 0.0005f)
                data[f * 2] = l * gain
                data[f * 2 + 1] = r * gain
            }
        }
    }

    /** Holds back the most recent N frames (for crossfading the tail into the next part). */
    private class FrameRing(private val capacityFrames: Int) {
        private var buf = FloatArray(0)
        private var frames = 0

        /** Push [n] frames; frames that fall out of the ring are emitted to [emit]. */
        fun push(data: FloatArray, n: Int, emit: (FloatArray, Int) -> Unit) {
            if (capacityFrames <= 0) {
                emit(data, n)
                return
            }
            val combined = FloatArray((frames + n) * CHANNELS)
            System.arraycopy(buf, 0, combined, 0, frames * CHANNELS)
            System.arraycopy(data, 0, combined, frames * CHANNELS, n * CHANNELS)
            val total = frames + n
            val overflow = total - capacityFrames
            if (overflow > 0) {
                emit(combined, overflow)
                buf = combined.copyOfRange(overflow * CHANNELS, total * CHANNELS)
                frames = capacityFrames
            } else {
                buf = combined
                frames = total
            }
        }

        fun drain(): FloatArray {
            val out = buf.copyOf(frames * CHANNELS)
            buf = FloatArray(0)
            frames = 0
            return out
        }
    }
}
