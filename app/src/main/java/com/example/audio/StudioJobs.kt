package com.example.audio

import android.content.Context
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * High-level DJ Studio operations used by the ViewModel. Each operation can render either a
 * short preview (WAV in the cache, played by [StudioPreviewPlayer]) or the full result
 * (saved to Music/Mixtape Pro via [StudioRenderEngine]).
 */
object StudioJobs {
    private const val SR = StudioRenderEngine.SAMPLE_RATE

    private fun decoder(context: Context, uri: String, inMs: Long, outMs: Long): MixRenderer.PcmSource =
        MixRenderer.decoderFactory(context)(MixRenderer.MixTrack(uri, inMs, outMs))

    /**
     * Stream [source] into a preview buffer as fast as it decodes (the studio monitor plays it
     * while it is being written). Stops after [maxFrames] (if > 0) or when the buffer is replaced.
     */
    fun streamInto(source: MixRenderer.PcmSource, sink: StudioPreviewBuffer, maxFrames: Long = -1L) {
        val buf = FloatArray(4096 * 2)
        var done = 0L
        try {
            while (maxFrames <= 0 || done < maxFrames) {
                val want = if (maxFrames > 0) min(4096L, maxFrames - done).toInt() else 4096
                val n = source.read(buf, 0, want)
                if (n <= 0) break
                StudioDsp.softClip(buf, n)
                sink.write(buf, n)
                done += n
            }
        } finally {
            source.release()
        }
        if (done == 0L && !sink.cancelled) throw StudioRenderEngine.RenderException("Nothing to preview — the audio couldn't be decoded.")
    }

    /** Render a whole source into shared storage. */
    fun renderFull(
        context: Context,
        source: MixRenderer.PcmSource,
        expectedFrames: Long,
        title: String,
        format: StudioRenderEngine.OutputFormat,
        onProgress: (Float) -> Unit
    ): StudioRenderEngine.RenderResult = StudioRenderEngine.writeOutput(context, title, format, StudioRenderEngine.Destination.MUSIC) { sink ->
        val buf = FloatArray(4096 * 2)
        var done = 0L
        try {
            while (true) {
                val n = source.read(buf, 0, 4096)
                if (n <= 0) break
                StudioDsp.softClip(buf, n)
                sink.write(buf, n)
                done += n
                onProgress((done.toFloat() / expectedFrames.coerceAtLeast(1)).coerceIn(0f, 0.99f))
            }
        } finally {
            source.release()
        }
        onProgress(1f)
    }

    fun framesBetween(context: Context, uri: String, inMs: Long, outMs: Long): Long {
        val end = if (outMs > 0) outMs else StudioRenderEngine.durationMs(context, uri)
        return ((end - inMs).coerceAtLeast(0L)) * SR / 1000L
    }

    // =============================================================================================
    // Clean-Up Studio
    // =============================================================================================

    fun noiseRemovalSource(context: Context, uri: String, profile: StudioDsp.NoiseProfile, strength: Float, inMs: Long = 0L, outMs: Long = -1L) =
        StudioDsp.StftSource(decoder(context, uri, inMs, outMs), StudioDsp.NoiseReducer(profile, strength))

    fun learnNoise(context: Context, uri: String): StudioDsp.NoiseProfile {
        val src = decoder(context, uri, 0L, -1L)
        return try { StudioDsp.learnNoiseProfile(src) } finally { src.release() }
    }

    fun vocalSource(context: Context, uri: String, mode: StudioDsp.VocalMode, strength: Float, inMs: Long = 0L, outMs: Long = -1L) =
        StudioDsp.StftSource(decoder(context, uri, inMs, outMs), StudioDsp.VocalSeparator(mode, strength))

    /** Remove [cutStartMs, cutEndMs] from a track with a short crossfade over the join. */
    fun cutSectionParts(uri: String, cutStartMs: Long, cutEndMs: Long): List<StudioRenderEngine.RenderPart> = listOfNotNull(
        if (cutStartMs > 200) StudioRenderEngine.RenderPart(uri, 0L, cutStartMs) else null,
        StudioRenderEngine.RenderPart(uri, cutEndMs, -1L)
    )

    // =============================================================================================
    // FX
    // =============================================================================================

    fun fxSource(context: Context, uri: String, preset: StudioDsp.RetroPreset, intensity: Float, inMs: Long = 0L, outMs: Long = -1L) =
        StudioDsp.retroChain(decoder(context, uri, inMs, outMs), preset, intensity)

    // =============================================================================================
    // Mashup Studio
    // =============================================================================================

    enum class LayerRole { MUSIC, VOICE }
    enum class LayerExtract(val label: String) { FULL("Full"), INSTRUMENTAL("No vocals"), VOCALS("Vocals only") }

    data class MashupLayer(
        val uri: String,
        val title: String,
        val durationMs: Long,
        val offsetMs: Long = 0L,
        val inMs: Long = 0L,
        val outMs: Long = -1L,
        val gain: Float = 1f,
        val pan: Float = 0f,
        val fadeIn: Boolean = true,
        val fadeOut: Boolean = true,
        val role: LayerRole = LayerRole.MUSIC,
        val extract: LayerExtract = LayerExtract.FULL,
        val tempo: Double = 1.0        // filled in by tempo matching
    ) {
        val playMs: Long get() = ((if (outMs > 0) outMs else durationMs) - inMs).coerceAtLeast(0L)
        val endOnTimelineMs: Long get() = offsetMs + (playMs / tempo).roundToLong()
    }

    /**
     * Tempo-match every layer to layer 1 and shift their start so their beats land on layer 1's grid.
     * Voice layers and layers without a detectable beat are left untouched.
     */
    fun alignLayers(
        layers: List<MashupLayer>,
        analyses: Map<String, TrackAnalysis>,
        matchTempo: Boolean,
        alignBeats: Boolean
    ): List<MashupLayer> {
        if (layers.isEmpty()) return layers
        val ref = layers.first()
        val refA = analyses[ref.uri]
        if (refA == null || !refA.hasBeat) return layers
        val refBeat = refA.beatMs
        // first beat of the reference on the output timeline
        val refFirstBeatOnTimeline = ref.offsetMs + floorMod(refA.firstBeatMs - ref.inMs, refBeat)

        return layers.mapIndexed { i, layer ->
            if (i == 0 || layer.role == LayerRole.VOICE) return@mapIndexed layer.copy(tempo = if (i == 0) 1.0 else layer.tempo)
            val a = analyses[layer.uri] ?: return@mapIndexed layer
            if (!a.hasBeat) return@mapIndexed layer
            var tempo = 1.0
            if (matchTempo) {
                var r = refA.bpm / a.bpm
                if (r > 1.6) r /= 2 else if (r < 0.62) r *= 2      // half/double-time
                tempo = if (r in 0.84..1.19) r else 1.0             // beyond ±16 % sounds unnatural
            }
            var offset = layer.offsetMs
            if (alignBeats) {
                val layerBeatOut = a.beatMs / tempo
                val firstBeatAfterIn = floorMod(a.firstBeatMs - layer.inMs, a.beatMs) / tempo
                val beatOnTimeline = layer.offsetMs + firstBeatAfterIn
                // move the layer by the smallest amount that puts its beat on the reference grid
                val grid = if (matchTempo && tempo != 1.0) refBeat else layerBeatOut
                val delta = floorMod(refFirstBeatOnTimeline - beatOnTimeline, grid)
                offset = layer.offsetMs + (if (delta > grid / 2) delta - grid else delta).roundToLong()
                if (offset < 0) offset += grid.roundToLong()
            }
            layer.copy(tempo = tempo, offsetMs = offset)
        }
    }

    private fun floorMod(x: Double, m: Double): Double = ((x % m) + m) % m
    private fun floorMod(x: Long, m: Double): Double = floorMod(x.toDouble(), m)

    /** Build the layered mix as one streaming source. */
    fun mashupSource(context: Context, layers: List<MashupLayer>, autoDuck: Boolean, duckAmount: Float): MixRenderer.PcmSource {
        val built = layers.map { l ->
            var src: MixRenderer.PcmSource = decoder(context, l.uri, l.inMs, l.outMs)
            when (l.extract) {
                LayerExtract.INSTRUMENTAL -> src = StudioDsp.StftSource(src, StudioDsp.VocalSeparator(StudioDsp.VocalMode.INSTRUMENTAL, 1f))
                LayerExtract.VOCALS -> src = StudioDsp.StftSource(src, StudioDsp.VocalSeparator(StudioDsp.VocalMode.VOCALS_ONLY, 1f))
                LayerExtract.FULL -> {}
            }
            if (abs(l.tempo - 1.0) > 0.003) src = StudioDsp.TimeStretchSource(src, l.tempo)
            val playFrames = (l.playMs / l.tempo).roundToLong() * SR / 1000L
            src = StudioDsp.GainPanSource(
                src, l.gain, l.pan,
                if (l.fadeIn) SR * 2L else 0L,
                if (l.fadeOut) SR * 3L else 0L,
                playFrames
            )
            StudioDsp.DelaySource(src, l.offsetMs * SR / 1000L) to l.role
        }
        return LayerMixer(built, autoDuck, duckAmount)
    }

    fun mashupFrames(layers: List<MashupLayer>): Long = (layers.maxOfOrNull { it.endOnTimelineMs } ?: 0L) * SR / 1000L

    /** Sums layers; voice layers can push the music layers down ("ducking"). */
    private class LayerMixer(
        private val layers: List<Pair<MixRenderer.PcmSource, LayerRole>>,
        private val autoDuck: Boolean,
        duckAmount: Float
    ) : MixRenderer.PcmSource {
        private val ended = BooleanArray(layers.size)
        private val tmp = FloatArray(4096 * 2)
        private val voice = FloatArray(4096 * 2)
        private val music = FloatArray(4096 * 2)
        private var env = 0f
        private var duckGain = 1f
        private val duckTarget = 1f - 0.75f * duckAmount.coerceIn(0f, 1f)
        private val attack = 1f - kotlin.math.exp(-1.0 / (0.02 * SR)).toFloat()
        private val release = 1f - kotlin.math.exp(-1.0 / (0.45 * SR)).toFloat()

        override fun read(out: FloatArray, offsetFrames: Int, maxFrames: Int): Int {
            val n = min(maxFrames, 4096)
            java.util.Arrays.fill(voice, 0, n * 2, 0f)
            java.util.Arrays.fill(music, 0, n * 2, 0f)
            var longest = 0
            layers.forEachIndexed { i, (src, role) ->
                if (ended[i]) return@forEachIndexed
                val got = StudioDsp.readFully(src, tmp, 0, n)
                if (got < n) ended[i] = true
                longest = max(longest, got)
                val target = if (role == LayerRole.VOICE) voice else music
                for (k in 0 until got * 2) target[k] += tmp[k]
            }
            if (longest == 0) return 0
            for (f in 0 until longest) {
                val v = max(abs(voice[f * 2]), abs(voice[f * 2 + 1]))
                env = if (v > env) env + (v - env) * 0.2f else env * 0.9995f
                val want = if (autoDuck && env > 0.03f) duckTarget else 1f
                duckGain += (want - duckGain) * (if (want < duckGain) attack else release)
                val o = (offsetFrames + f) * 2
                out[o] = music[f * 2] * duckGain + voice[f * 2]
                out[o + 1] = music[f * 2 + 1] * duckGain + voice[f * 2 + 1]
            }
            return longest
        }

        override fun release() { layers.forEach { it.first.release() } }
    }
}
