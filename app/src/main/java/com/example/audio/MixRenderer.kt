package com.example.audio

import android.content.Context
import android.net.Uri
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Mixtape Builder renderer: plays a list of tracks back-to-back and blends each pair with a
 * DJ transition (crossfade, bass swap, filter sweep, echo out, reverb wash, flanger, brake,
 * loop roll or hard cut). Optional one-shot "drops" (vinyl scratch, rewind, 808 kick, clap)
 * are layered on top of a transition.
 *
 * Everything streams, so a 1-hour mix never needs more than a few seconds of audio in memory.
 */
object MixRenderer {

    private const val SR = StudioRenderEngine.SAMPLE_RATE

    enum class Transition(val label: String, val defaultSeconds: Int, val usesLength: Boolean) {
        CROSSFADE("Crossfade", 6, true),
        BASS_SWAP("Bass Swap", 8, true),
        FILTER_SWEEP("Filter Sweep", 8, true),
        ECHO_OUT("Echo Out", 6, true),
        REVERB_WASH("Reverb Wash", 6, true),
        FLANGER("Flanger", 6, true),
        BRAKE("Brake", 2, false),
        LOOP_ROLL("Loop Roll", 2, false),
        CUT("Hard Cut", 0, false)
    }

    enum class OneShot(val label: String) {
        NONE("None"),
        VINYL_SCRATCH("Scratch"),
        REWIND("Rewind"),
        KICK_808("808 Kick"),
        CLAP_808("808 Clap")
    }

    /** One track in the mix. Times are positions in the source file (ms). outMs <= 0 = end of file. */
    data class MixTrack(
        val uri: String,
        val inMs: Long = 0L,
        val outMs: Long = -1L,
        val gain: Float = 1f,
        /** How this track blends into the NEXT one (ignored on the last track). */
        val transition: Transition = Transition.CROSSFADE,
        val transitionSeconds: Float = Transition.CROSSFADE.defaultSeconds.toFloat(),
        val drop: OneShot = OneShot.NONE
    )

    /** Anything that can produce interleaved stereo float frames at 44.1 kHz. */
    interface PcmSource {
        fun read(out: FloatArray, offsetFrames: Int, maxFrames: Int): Int
        fun release()
    }

    private class DecoderAdapter(private val d: StudioRenderEngine.DecodedPcmSource) : PcmSource {
        override fun read(out: FloatArray, offsetFrames: Int, maxFrames: Int) = d.read(out, offsetFrames, maxFrames)
        override fun release() = d.release()
    }

    fun decoderFactory(context: Context): (MixTrack) -> PcmSource = { t ->
        DecoderAdapter(StudioRenderEngine.DecodedPcmSource(context, Uri.parse(t.uri), t.inMs, t.outMs))
    }

    // =============================================================================================
    // Public entry points
    // =============================================================================================

    /** Render the full mix into Music/Mixtape Pro. */
    fun exportMix(
        context: Context,
        tracks: List<MixTrack>,
        title: String,
        format: StudioRenderEngine.OutputFormat,
        onProgress: (Float) -> Unit
    ): StudioRenderEngine.RenderResult {
        if (tracks.isEmpty()) throw StudioRenderEngine.RenderException("Add at least one track to the mixtape.")
        val expected = tracks.sumOf { t ->
            val total = StudioRenderEngine.durationMs(context, t.uri)
            val end = if (t.outMs > 0) t.outMs else total
            ((end - t.inMs).coerceAtLeast(0L)) * SR / 1000L
        }.coerceAtLeast(1L)
        return StudioRenderEngine.writeOutput(context, title, format, StudioRenderEngine.Destination.MUSIC) { sink ->
            render(tracks, decoderFactory(context), { data, n -> sink.write(data, n) }, expected, onProgress)
        }
    }

    /**
     * Render a short audition of the transition between [a] and [b]:
     * the last [leadSeconds] of A, the transition, and the first [leadSeconds] of B.
     * Written as a WAV into the cache dir; returns the file.
     */
    fun renderTransitionPreview(
        context: Context,
        a: MixTrack,
        aDurationMs: Long,
        b: MixTrack,
        leadSeconds: Int = 8
    ): File {
        val aEnd = if (a.outMs > 0) a.outMs else aDurationMs
        val tLen = (a.transitionSeconds * 1000).toLong()
        val aStart = (aEnd - tLen - leadSeconds * 1000L).coerceAtLeast(a.inMs)
        val bEnd = b.inMs + tLen + leadSeconds * 1000L
        val previewA = a.copy(inMs = aStart, outMs = aEnd)
        val previewB = b.copy(inMs = b.inMs, outMs = bEnd, transition = Transition.CUT, drop = OneShot.NONE)

        val out = File(context.cacheDir, "mix_preview.wav")
        StudioRenderEngine.WavFileSink(out).use { sink ->
            render(listOf(previewA, previewB), decoderFactory(context), { d, n -> sink.write(d, n) }, 1L) {}
        }
        return out
    }

    // =============================================================================================
    // Core streaming renderer (pure DSP — unit-testable with synthetic sources)
    // =============================================================================================

    fun render(
        tracks: List<MixTrack>,
        sourceFor: (MixTrack) -> PcmSource,
        write: (FloatArray, Int) -> Unit,
        expectedFrames: Long,
        onProgress: (Float) -> Unit
    ) {
        val overlay = Overlay()
        var written = 0L
        val emit: (FloatArray, Int) -> Unit = { data, n ->
            overlay.mixInto(data, n)
            softClip(data, n)
            write(data, n)
            written += n
            onProgress((written.toFloat() / expectedFrames).coerceIn(0f, 0.99f))
        }

        var carriedSource: PcmSource? = null // next track's source, already partly consumed by a transition
        val chunk = FloatArray(4096 * 2)

        for ((index, track) in tracks.withIndex()) {
            val source = carriedSource ?: sourceFor(track)
            carriedSource = null
            val isLast = index == tracks.lastIndex
            val tailFrames = if (isLast) 0 else tailFramesFor(track)
            val ring = TailRing(tailFrames)

            try {
                while (true) {
                    val n = source.read(chunk, 0, chunk.size / 2)
                    if (n <= 0) break
                    if (track.gain != 1f) for (i in 0 until n * 2) chunk[i] *= track.gain
                    if (tailFrames > 0) ring.push(chunk, n, emit) else emit(chunk, n)
                }
            } finally {
                if (isLast) source.release()
            }
            if (isLast) break

            // ---- Transition into the next track ----
            source.release()
            val next = tracks[index + 1]
            val nextSource = sourceFor(next)
            val tail = ring.drain()
            if (track.drop != OneShot.NONE) overlay.add(OneShotSynth.render(track.drop), 0.85f)
            val blended = applyTransition(track, tail, nextSource, next.gain)
            if (blended.isNotEmpty()) emit(blended, blended.size / 2)
            carriedSource = nextSource
        }
        // Let a final one-shot ring out
        val rest = overlay.remainingFrames()
        if (rest > 0) {
            val silence = FloatArray(rest * 2)
            emit(silence, rest)
        }
        onProgress(1f)
    }

    /** Transparent below -2 dB, then rounds peaks off smoothly so overlaps never hard-clip. */
    private fun softClip(data: FloatArray, frames: Int) {
        val knee = 0.8f
        for (i in 0 until frames * 2) {
            val x = data[i]
            val ax = abs(x)
            if (ax > knee) {
                val y = knee + (1f - knee) * kotlin.math.tanh((ax - knee) / (1f - knee))
                data[i] = if (x < 0) -y else y
            }
        }
    }

    private fun tailFramesFor(t: MixTrack): Int = when (t.transition) {
        Transition.CUT -> 0
        Transition.BRAKE -> (SR * 1.6f).toInt()
        Transition.LOOP_ROLL -> (SR * 2.0f).toInt()
        else -> (t.transitionSeconds.coerceIn(1f, 20f) * SR).toInt()
    }

    /** Read exactly [frames] frames (or fewer at end of file) from [src], applying [gain]. */
    private fun readHead(src: PcmSource, frames: Int, gain: Float): FloatArray {
        val buf = FloatArray(frames * 2)
        var got = 0
        while (got < frames) {
            val n = src.read(buf, got, frames - got)
            if (n <= 0) break
            got += n
        }
        if (gain != 1f) for (i in 0 until got * 2) buf[i] *= gain
        return buf // zero-padded if the next track is shorter than the transition
    }

    // =============================================================================================
    // Transitions — each returns the audio that replaces A's tail (and possibly B's head)
    // =============================================================================================

    private fun applyTransition(a: MixTrack, tail: FloatArray, next: PcmSource, nextGain: Float): FloatArray {
        val n = tail.size / 2
        if (n == 0) return FloatArray(0)
        return when (a.transition) {
            Transition.CUT -> tail
            Transition.CROSSFADE -> crossfade(tail, readHead(next, n, nextGain))
            Transition.BASS_SWAP -> bassSwap(tail, readHead(next, n, nextGain))
            Transition.FILTER_SWEEP -> filterSweep(tail, readHead(next, n, nextGain))
            Transition.ECHO_OUT -> echoOut(tail, next, nextGain)
            Transition.REVERB_WASH -> reverbWash(tail, next, nextGain)
            Transition.FLANGER -> flangerOut(tail, readHead(next, n, nextGain))
            Transition.BRAKE -> brake(tail)
            Transition.LOOP_ROLL -> loopRoll(tail)
        }
    }

    private fun crossfade(a: FloatArray, b: FloatArray): FloatArray {
        val n = a.size / 2
        val out = FloatArray(a.size)
        for (f in 0 until n) {
            val t = (f + 0.5) / n
            val ga = cos(t * PI / 2).toFloat()
            val gb = sin(t * PI / 2).toFloat()
            out[f * 2] = a[f * 2] * ga + b[f * 2] * gb
            out[f * 2 + 1] = a[f * 2 + 1] * ga + b[f * 2 + 1] * gb
        }
        return out
    }

    /** Highs crossfade smoothly; the bass line hands over from A to B at the midpoint. */
    private fun bassSwap(a: FloatArray, b: FloatArray): FloatArray {
        val n = a.size / 2
        val out = FloatArray(a.size)
        val lpA = arrayOf(Biquad(), Biquad())
        val lpB = arrayOf(Biquad(), Biquad())
        for (c in 0..1) { lpA[c].lowPass(180.0, 0.707); lpB[c].lowPass(180.0, 0.707) }
        val swap = n / 2
        val ramp = (SR * 0.08).toInt() // 80 ms bass handover
        for (f in 0 until n) {
            val t = (f + 0.5) / n
            val hiA = cos(t * PI / 2).toFloat()
            val hiB = sin(t * PI / 2).toFloat()
            val bassMix = ((f - (swap - ramp / 2)).toFloat() / ramp).coerceIn(0f, 1f)
            for (c in 0..1) {
                val i = f * 2 + c
                val aLow = lpA[c].process(a[i])
                val bLow = lpB[c].process(b[i])
                val aHigh = a[i] - aLow
                val bHigh = b[i] - bLow
                out[i] = aHigh * hiA + bHigh * hiB + aLow * (1f - bassMix) + bLow * bassMix
            }
        }
        return out
    }

    /** A is swept out through a rising high-pass; B is swept in through an opening low-pass. */
    private fun filterSweep(a: FloatArray, b: FloatArray): FloatArray {
        val n = a.size / 2
        val out = FloatArray(a.size)
        val hp = arrayOf(Biquad(), Biquad())
        val lp = arrayOf(Biquad(), Biquad())
        for (f in 0 until n) {
            val t = (f + 0.5) / n
            if (f % 32 == 0) {
                val hpCut = 30.0 * (2500.0 / 30.0).pow(t)          // 30 Hz -> 2.5 kHz
                val lpCut = 250.0 * (18000.0 / 250.0).pow(t)       // 250 Hz -> 18 kHz
                for (c in 0..1) { hp[c].highPass(hpCut, 1.2); lp[c].lowPass(lpCut, 1.2) }
            }
            val ga = if (t < 0.7) 1f else (1f - ((t - 0.7) / 0.3).toFloat()).coerceIn(0f, 1f)
            val gb = (t / 0.35).toFloat().coerceIn(0f, 1f)
            for (c in 0..1) {
                val i = f * 2 + c
                out[i] = hp[c].process(a[i]) * ga + lp[c].process(b[i]) * gb
            }
        }
        return out
    }

    /** A fades out into a trail of repeating echoes; B comes in over the echo tail. */
    private fun echoOut(a: FloatArray, next: PcmSource, nextGain: Float): FloatArray {
        val n = a.size / 2
        val half = n / 2
        val b = readHead(next, n - half, nextGain)
        val out = FloatArray(a.size)
        val delayFrames = (SR * 0.375).toInt() // dotted-eighth at ~120 BPM
        val dl = FloatArray(delayFrames * 2)
        var dp = 0
        val feedback = 0.6f
        for (f in 0 until n) {
            val t = (f + 0.5f) / n
            val dryGain = (1f - t / 0.5f).coerceIn(0f, 1f)
            for (c in 0..1) {
                val i = f * 2 + c
                val dry = a[i] * dryGain
                val delayed = dl[dp * 2 + c]
                dl[dp * 2 + c] = dry + delayed * feedback
                val bIdx = (f - half) * 2 + c
                val bs = if (f >= half) b[bIdx] * ((f - half).toFloat() / (n - half)).coerceIn(0f, 1f) else 0f
                out[i] = dry + delayed * 0.8f + bs
            }
            dp = (dp + 1) % delayFrames
        }
        return out
    }

    /** A dissolves into a big hall reverb; B fades in over the reverb tail. */
    private fun reverbWash(a: FloatArray, next: PcmSource, nextGain: Float): FloatArray {
        val n = a.size / 2
        val half = n / 2
        val b = readHead(next, n - half, nextGain)
        val out = FloatArray(a.size)
        val rev = arrayOf(Reverb(0), Reverb(23))
        for (f in 0 until n) {
            val t = (f + 0.5f) / n
            val dry = (1f - t / 0.6f).coerceIn(0f, 1f)
            val send = (t / 0.4f).coerceIn(0f, 1f) * (if (t < 0.6f) 1f else 0f)
            for (c in 0..1) {
                val i = f * 2 + c
                val wet = rev[c].process(a[i] * send)
                val bIdx = (f - half) * 2 + c
                val bs = if (f >= half) b[bIdx] * sin(((f - half).toDouble() / (n - half)) * PI / 2).toFloat() else 0f
                out[i] = a[i] * dry + wet * 0.9f + bs
            }
        }
        return out
    }

    /** Jet-plane flanger swells on A while crossfading to B. */
    private fun flangerOut(a: FloatArray, b: FloatArray): FloatArray {
        val n = a.size / 2
        val out = FloatArray(a.size)
        val maxDelay = (SR * 0.006).toInt() + 2
        val buf = Array(2) { FloatArray(maxDelay) }
        var wp = 0
        for (f in 0 until n) {
            val t = (f + 0.5) / n
            val lfo = (1 - cos(2 * PI * 0.35 * f / SR)) / 2 // 0..1 at 0.35 Hz
            val delay = 0.001 * SR + lfo * 0.004 * SR
            val depth = min(1.0, t * 1.6).toFloat()
            val ga = cos(t * PI / 2).toFloat()
            val gb = sin(t * PI / 2).toFloat()
            for (c in 0..1) {
                val i = f * 2 + c
                val rp = (wp - delay + maxDelay) % maxDelay
                val i0 = rp.toInt()
                val frac = (rp - i0).toFloat()
                val d = buf[c][i0] * (1 - frac) + buf[c][(i0 + 1) % maxDelay] * frac
                buf[c][wp] = a[i] + d * 0.55f
                val fl = a[i] + d * depth
                out[i] = fl * 0.75f * ga + b[i] * gb
            }
            wp = (wp + 1) % maxDelay
        }
        return out
    }

    /** Turntable power-off: A slows to a stop, then B drops in on the next beat. */
    private fun brake(tail: FloatArray): FloatArray {
        val n = tail.size / 2
        if (n < 4) return tail
        val outFrames = n // output length; consumes ~n/2 source frames
        val out = FloatArray(outFrames * 2)
        var pos = 0.0
        for (f in 0 until outFrames) {
            val rate = 1.0 - f.toDouble() / outFrames
            val i0 = pos.toInt().coerceAtMost(n - 2)
            val frac = (pos - i0).toFloat()
            val g = (1.0 - (f.toDouble() / outFrames).pow(3)).toFloat()
            for (c in 0..1) {
                out[f * 2 + c] = (tail[i0 * 2 + c] * (1 - frac) + tail[(i0 + 1) * 2 + c] * frac) * g
            }
            pos += rate
        }
        return out
    }

    /** Beat-repeat: the last slice of A stutters in 1/2, 1/4 then 1/8-beat rolls before B. */
    private fun loopRoll(tail: FloatArray): FloatArray {
        val n = tail.size / 2
        val beat = (SR * 0.5).toInt() // 120 BPM
        val sliceStart = max(0, n - beat)
        val pattern = listOf(beat / 2 to 2, beat / 4 to 4, beat / 8 to 8)
        val total = pattern.sumOf { it.first * it.second }
        val head = sliceStart
        val out = FloatArray((head + total) * 2)
        System.arraycopy(tail, 0, out, 0, head * 2)
        var w = head
        var step = 0
        val steps = pattern.sumOf { it.second }
        for ((len, reps) in pattern) {
            repeat(reps) {
                val fadeLen = min(64, len / 4)
                val g = 1f - 0.4f * step / steps
                for (f in 0 until len) {
                    val src = (sliceStart + f).coerceAtMost(n - 1)
                    val env = when {
                        f < fadeLen -> f.toFloat() / fadeLen
                        f > len - fadeLen -> (len - f).toFloat() / fadeLen
                        else -> 1f
                    }
                    out[(w + f) * 2] = tail[src * 2] * env * g
                    out[(w + f) * 2 + 1] = tail[src * 2 + 1] * env * g
                }
                w += len
                step++
            }
        }
        return out
    }

    // =============================================================================================
    // DSP building blocks
    // =============================================================================================

    /** RBJ biquad (Direct Form I). */
    class Biquad {
        private var b0 = 1.0; private var b1 = 0.0; private var b2 = 0.0
        private var a1 = 0.0; private var a2 = 0.0
        private var x1 = 0.0; private var x2 = 0.0; private var y1 = 0.0; private var y2 = 0.0

        fun lowPass(freq: Double, q: Double) = set(freq, q, false)
        fun highPass(freq: Double, q: Double) = set(freq, q, true)

        private fun set(freq: Double, q: Double, high: Boolean) {
            val f = freq.coerceIn(10.0, SR * 0.45)
            val w0 = 2 * PI * f / SR
            val alpha = sin(w0) / (2 * q)
            val cw = cos(w0)
            val a0 = 1 + alpha
            if (high) {
                b0 = (1 + cw) / 2 / a0; b1 = -(1 + cw) / a0; b2 = (1 + cw) / 2 / a0
            } else {
                b0 = (1 - cw) / 2 / a0; b1 = (1 - cw) / a0; b2 = (1 - cw) / 2 / a0
            }
            a1 = -2 * cw / a0; a2 = (1 - alpha) / a0
        }

        fun process(x: Float): Float {
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x.toDouble(); y2 = y1; y1 = y
            return y.toFloat()
        }
    }

    /** Small Schroeder/Freeverb-style reverb (4 combs + 2 all-pass). [spread] detunes the channels. */
    private class Reverb(spread: Int) {
        private val combs = intArrayOf(1116, 1188, 1277, 1356).map { Comb(it + spread) }
        private val allpasses = intArrayOf(556, 441).map { AllPass(it + spread) }
        fun process(x: Float): Float {
            var s = 0f
            for (c in combs) s += c.process(x * 0.25f)
            var y = s
            for (a in allpasses) y = a.process(y)
            return y
        }

        private class Comb(size: Int) {
            private val buf = FloatArray(size); private var i = 0; private var store = 0f
            fun process(x: Float): Float {
                val out = buf[i]
                store = out * 0.8f + store * 0.2f // damping
                buf[i] = x + store * 0.86f
                i = (i + 1) % buf.size
                return out
            }
        }

        private class AllPass(size: Int) {
            private val buf = FloatArray(size); private var i = 0
            fun process(x: Float): Float {
                val b = buf[i]
                buf[i] = x + b * 0.5f
                i = (i + 1) % buf.size
                return b - x
            }
        }
    }

    /** Holds the most recent N frames of a track so they can be blended into the next one. */
    private class TailRing(private val capacity: Int) {
        private val buf = FloatArray(capacity * 2)
        private var start = 0
        private var size = 0
        private val spill = FloatArray(4096 * 2)

        fun push(data: FloatArray, n: Int, emit: (FloatArray, Int) -> Unit) {
            var spillCount = 0
            for (f in 0 until n) {
                if (size == capacity) {
                    // Oldest frame leaves the ring and goes to the output
                    spill[spillCount * 2] = buf[start * 2]
                    spill[spillCount * 2 + 1] = buf[start * 2 + 1]
                    spillCount++
                    if (spillCount * 2 == spill.size) { emit(spill.copyOf(), spillCount); spillCount = 0 }
                    buf[start * 2] = data[f * 2]
                    buf[start * 2 + 1] = data[f * 2 + 1]
                    start = (start + 1) % capacity
                } else {
                    val w = (start + size) % capacity
                    buf[w * 2] = data[f * 2]
                    buf[w * 2 + 1] = data[f * 2 + 1]
                    size++
                }
            }
            if (spillCount > 0) emit(spill.copyOf(spillCount * 2), spillCount)
        }

        fun drain(): FloatArray {
            val out = FloatArray(size * 2)
            for (k in 0 until size) {
                val r = (start + k) % capacity
                out[k * 2] = buf[r * 2]
                out[k * 2 + 1] = buf[r * 2 + 1]
            }
            start = 0; size = 0
            return out
        }
    }

    /** One-shot samples layered over whatever is being written next. */
    private class Overlay {
        private val voices = mutableListOf<Pair<FloatArray, IntArray>>() // samples, [position]

        fun add(samples: FloatArray, gain: Float) {
            if (gain != 1f) for (i in samples.indices) samples[i] *= gain
            voices.add(samples to intArrayOf(0))
        }

        fun mixInto(data: FloatArray, frames: Int) {
            if (voices.isEmpty()) return
            val it = voices.iterator()
            while (it.hasNext()) {
                val (s, posArr) = it.next()
                var p = posArr[0]
                var f = 0
                while (f < frames && p * 2 < s.size) {
                    data[f * 2] += s[p * 2]
                    data[f * 2 + 1] += s[p * 2 + 1]
                    f++; p++
                }
                posArr[0] = p
                if (p * 2 >= s.size) it.remove()
            }
        }

        fun remainingFrames(): Int = voices.maxOfOrNull { (s, p) -> s.size / 2 - p[0] } ?: 0
    }
}

/**
 * Synthesised 90s one-shot sounds (no sample files needed): vinyl scratch, tape rewind,
 * TR-808 kick and clap. Returns interleaved stereo float PCM at 44.1 kHz.
 */
object OneShotSynth {
    private const val SR = StudioRenderEngine.SAMPLE_RATE
    private val cache = HashMap<MixRenderer.OneShot, FloatArray>()

    @Synchronized
    fun render(type: MixRenderer.OneShot): FloatArray =
        (cache.getOrPut(type) { normalize(synth(type), 0.9f) }).copyOf()

    private fun normalize(pcm: FloatArray, target: Float): FloatArray {
        val peak = pcm.maxOfOrNull { abs(it) } ?: 0f
        if (peak > 0f) { val g = target / peak; for (i in pcm.indices) pcm[i] *= g }
        return pcm
    }

    private fun synth(type: MixRenderer.OneShot): FloatArray = when (type) {
        MixRenderer.OneShot.NONE -> FloatArray(0)
        MixRenderer.OneShot.KICK_808 -> kick()
        MixRenderer.OneShot.CLAP_808 -> clap()
        MixRenderer.OneShot.VINYL_SCRATCH -> scratch()
        MixRenderer.OneShot.REWIND -> rewind()
    }

    private fun stereo(mono: FloatArray): FloatArray {
        val out = FloatArray(mono.size * 2)
        for (i in mono.indices) { out[i * 2] = mono[i]; out[i * 2 + 1] = mono[i] }
        return out
    }

    private fun kick(): FloatArray {
        val n = (SR * 0.9).toInt()
        val m = FloatArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val freq = 45 + 110 * exp(-t * 28)
            phase += 2 * PI * freq / SR
            val amp = exp(-t * 3.2)
            val click = if (i < 120) (1 - i / 120.0) * 0.4 else 0.0
            m[i] = (sin(phase) * amp + click).toFloat() * 0.95f
        }
        return stereo(m)
    }

    private fun clap(): FloatArray {
        val n = (SR * 0.45).toInt()
        val m = FloatArray(n)
        val rnd = java.util.Random(808)
        val bp = MixRenderer.Biquad().apply { highPass(900.0, 0.9) }
        val lp = MixRenderer.Biquad().apply { lowPass(3500.0, 0.9) }
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            // three quick slaps then a decaying tail
            val burst = listOf(0.0, 0.011, 0.023).sumOf { s -> if (t >= s && t < s + 0.009) exp(-(t - s) * 300) else 0.0 }
            val tail = if (t >= 0.023) exp(-(t - 0.023) * 18) else 0.0
            val noise = rnd.nextGaussian() * 0.5
            m[i] = lp.process(bp.process((noise * (burst + tail * 0.8)).toFloat())) * 1.6f
        }
        return stereo(m)
    }

    private fun scratch(): FloatArray {
        val n = (SR * 0.7).toInt()
        val m = FloatArray(n)
        val rnd = java.util.Random(1999)
        var phase = 0.0
        val lp = MixRenderer.Biquad().apply { lowPass(2500.0, 2.0) }
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            // hand moves the record forward/back twice: speed follows |sin|
            val motion = sin(2 * PI * 2.9 * t)
            val speed = abs(motion)
            phase += 2 * PI * (90 + 520 * speed) / SR
            val saw = ((phase / (2 * PI)) % 1.0) * 2 - 1
            val grit = rnd.nextGaussian() * 0.15 * speed
            val env = min(1.0, t * 60) * (if (t > 0.6) (0.7 - t) / 0.1 else 1.0)
            m[i] = lp.process(((saw * 0.55 + grit) * speed * env).toFloat())
        }
        return stereo(m)
    }

    private fun rewind(): FloatArray {
        val n = (SR * 1.3).toInt()
        val m = FloatArray(n)
        val rnd = java.util.Random(95)
        var phase = 0.0
        val bp = MixRenderer.Biquad()
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val x = t / 1.3
            // tape spins up backwards: pitch & brightness rise quickly, then cut off
            val freq = 120 * (40.0).pow(x)
            phase += 2 * PI * freq / SR
            if (i % 32 == 0) bp.lowPass(400 + 9000 * x, 1.5)
            val warble = sin(phase) * 0.5 + sin(phase * 1.51) * 0.25
            val hiss = rnd.nextGaussian() * 0.25
            val env = min(1.0, t * 8) * (if (x > 0.92) (1 - x) / 0.08 else 1.0)
            m[i] = bp.process(((warble + hiss) * env * 0.8).toFloat())
        }
        return stereo(m)
    }

    /** Play a one-shot immediately through the speaker (for the audition pads). */
    fun play(type: MixRenderer.OneShot) {
        if (type == MixRenderer.OneShot.NONE) return
        val pcm = render(type)
        val shorts = ShortArray(pcm.size) { i -> (pcm[i].coerceIn(-1f, 1f) * 32767).toInt().toShort() }
        Thread {
            try {
                val track = android.media.AudioTrack.Builder()
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        android.media.AudioFormat.Builder()
                            .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SR)
                            .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setTransferMode(android.media.AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(shorts.size * 2)
                    .build()
                track.write(shorts, 0, shorts.size)
                track.play()
                Thread.sleep((shorts.size / 2 * 1000L / SR) + 150)
                track.release()
            } catch (_: Exception) {}
        }.start()
    }
}
