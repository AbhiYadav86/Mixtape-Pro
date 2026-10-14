package com.example.audio

import android.content.Context
import android.net.Uri
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sqrt

/** What the analyzer learns about a track. Times are positions in the source file (ms). */
data class TrackAnalysis(
    val durationMs: Long,
    val bpm: Double,              // 0 = no reliable beat (ballad / speech)
    val beatConfidence: Double,   // 0..1
    val firstBeatMs: Long,        // phase of the beat grid
    val audioStartMs: Long,       // first non-silent moment
    val audioEndMs: Long,         // last non-silent moment (before a long fade-out)
    val loudnessDb: Double        // RMS level of the main body (dBFS)
) {
    val hasBeat get() = bpm > 0 && beatConfidence >= 0.25
    val beatMs get() = if (bpm > 0) 60000.0 / bpm else 500.0

    /** Nearest beat-grid position to [ms]. */
    fun snapToBeat(ms: Long): Long {
        if (!hasBeat) return ms
        val k = Math.round((ms - firstBeatMs) / beatMs)
        return (firstBeatMs + k * beatMs).roundToLong()
    }
}

/**
 * Lightweight music analysis using the same decoder as the renderer.
 * Tempo comes from the autocorrelation of a spectral-flux onset envelope (works well on
 * beat-driven music; ballads and speech get bpm = 0 and simply crossfade).
 */
object TrackAnalyzer {
    private const val SR = StudioRenderEngine.SAMPLE_RATE
    private const val HOP = 512
    private const val N = 1024

    private val cache = HashMap<String, TrackAnalysis>()

    @Synchronized
    fun cached(uri: String) = cache[uri]

    fun analyze(context: Context, uri: String): TrackAnalysis {
        synchronized(this) { cache[uri]?.let { return it } }
        val duration = StudioRenderEngine.durationMs(context, uri)
        fun open(startMs: Long, endMs: Long): MixRenderer.PcmSource =
            MixRenderer.decoderFactory(context)(MixRenderer.MixTrack(uri, startMs, endMs))

        // Tempo from the body of the song (skip the intro, up to 60 s)
        val bodyStart = if (duration > 100_000) 30_000L else 0L
        val bodyEnd = min(duration, bodyStart + 60_000L).takeIf { it > bodyStart } ?: -1L
        val body = open(bodyStart, bodyEnd)
        val envs = try { onsetEnvelope(body) } finally { body.release() }
        val rmsDb = envs.db
        val (bpm, conf, phaseFrames) = estimateTempo(envs)
        val firstBeat = bodyStart + ((phaseFrames * HOP + N / 2) * 1000.0 / SR).roundToLong()

        // Silence trimming at both ends
        val head = open(0L, min(duration, 15_000L).takeIf { it > 0 } ?: -1L)
        val startMs = try { firstLoudMs(head, -42.0) } finally { head.release() }
        val tailStart = (duration - 20_000L).coerceAtLeast(0L)
        val tail = open(tailStart, -1L)
        val endMs = try { tailStart + lastLoudMs(tail, rmsDb - 24.0) } finally { tail.release() }

        val a = TrackAnalysis(
            durationMs = duration,
            bpm = bpm,
            beatConfidence = conf,
            firstBeatMs = firstBeat,
            audioStartMs = startMs.coerceIn(0L, max(0L, duration - 5000)),
            audioEndMs = endMs.coerceIn(startMs + 5000, if (duration > 0) duration else Long.MAX_VALUE),
            loudnessDb = rmsDb
        )
        synchronized(this) { cache[uri] = a }
        return a
    }

    /** Pure-DSP analysis of an in-memory source (used by tests). */
    fun analyzeSource(src: MixRenderer.PcmSource): Triple<Double, Double, Long> {
        val (bpm, conf, phase) = estimateTempo(onsetEnvelope(src))
        return Triple(bpm, conf, ((phase * HOP + N / 2) * 1000.0 / SR).roundToLong())
    }

    private class Envelopes(val full: DoubleArray, val low: DoubleArray, val db: Double)

    private fun onsetEnvelope(src: MixRenderer.PcmSource): Envelopes {
        val win = StudioDsp.hann(N)
        val frame = FloatArray(N * 2)
        val hopBuf = FloatArray(HOP * 2)
        val mono = DoubleArray(N)
        val re = DoubleArray(N); val im = DoubleArray(N)
        var prev = DoubleArray(N / 2)
        val env = ArrayList<Double>()
        val lowEnv = ArrayList<Double>()
        var sumSq = 0.0; var count = 0L
        // prime first window
        var filled = StudioDsp.readFully(src, frame, 0, N)
        if (filled < N) return Envelopes(DoubleArray(0), DoubleArray(0), -60.0)
        while (true) {
            for (k in 0 until N) mono[k] = (frame[k * 2] + frame[k * 2 + 1]) * 0.5
            for (k in 0 until N) { re[k] = mono[k] * win[k]; im[k] = 0.0; sumSq += mono[k] * mono[k] }
            count += N
            StudioDsp.fft(re, im)
            val mag = DoubleArray(N / 2) { ln(1.0 + 1000.0 * sqrt(re[it] * re[it] + im[it] * im[it])) }
            var flux = 0.0
            var lowFlux = 0.0
            for (k in 1 until N / 2) {
                val d = mag[k] - prev[k]
                if (d > 0) {
                    flux += d
                    if (k <= 10) lowFlux += d   // < ~430 Hz: kick drum & bass, used for beat phase
                }
            }
            env.add(flux)
            lowEnv.add(lowFlux)
            prev = mag
            // advance one hop
            System.arraycopy(frame, HOP * 2, frame, 0, (N - HOP) * 2)
            val got = StudioDsp.readFully(src, hopBuf, 0, HOP)
            if (got < HOP) break
            System.arraycopy(hopBuf, 0, frame, (N - HOP) * 2, HOP * 2)
        }
        val rms = sqrt(sumSq / max(1L, count))
        val db = if (rms > 1e-6) 20 * log10(rms) else -60.0
        return Envelopes(detrend(env.toDoubleArray()), detrend(lowEnv.toDoubleArray()), db)
    }

    /** Remove the slow trend and half-wave rectify. */
    private fun detrend(e: DoubleArray): DoubleArray {
        val out = DoubleArray(e.size)
        val w = 16
        for (i in e.indices) {
            var s = 0.0; var c = 0
            for (j in max(0, i - w)..min(e.size - 1, i + w)) { s += e[j]; c++ }
            out[i] = max(0.0, e[i] - s / c)
        }
        return out
    }

    /** Returns (bpm, confidence 0..1, phase in envelope frames). */
    private fun estimateTempo(envs: Envelopes): Triple<Double, Double, Double> {
        val env = envs.full
        if (env.size < 200) return Triple(0.0, 0.0, 0.0)
        val fps = SR.toDouble() / HOP
        val minLag = (fps * 60 / 180).toInt()   // 180 BPM
        val maxLag = (fps * 60 / 70).toInt()    // 70 BPM
        val mean = env.average()
        val x = DoubleArray(env.size) { env[it] - mean }
        var energy = 0.0
        for (v in x) energy += v * v
        if (energy <= 1e-9) return Triple(0.0, 0.0, 0.0)

        var bestLag = 0.0; var bestScore = Double.NEGATIVE_INFINITY; var bestRaw = 0.0
        val scores = DoubleArray(maxLag + 2)
        for (lag in minLag..maxLag) {
            var s = 0.0
            for (i in 0 until x.size - lag) s += x[i] * x[i + lag]
            scores[lag] = s
        }
        for (lag in minLag..maxLag) {
            val bpm = 60 * fps / lag
            // mild preference for 90–140 BPM (log-gaussian around 118)
            val prior = exp(-0.5 * Math.pow(ln(bpm / 118.0) / 0.35, 2.0))
            // reward lags whose double also correlates (beat + bar structure)
            val dbl = if (lag * 2 <= maxLag) scores[lag * 2] else 0.0
            val s = (scores[lag] + 0.5 * dbl) * (0.6 + 0.4 * prior)
            if (s > bestScore) { bestScore = s; bestLag = lag.toDouble(); bestRaw = scores[lag] }
        }
        // parabolic refinement for a fractional lag
        val l = bestLag.toInt()
        if (l in (minLag + 1) until maxLag) {
            val a = scores[l - 1]; val b = scores[l]; val c = scores[l + 1]
            val d = (a - 2 * b + c)
            if (d != 0.0) bestLag = l + 0.5 * (a - c) / d
        }
        // confidence: how much the winning lag stands out from the average periodicity
        var meanR = 0.0
        for (lag in minLag..maxLag) meanR += scores[lag] / energy
        meanR /= (maxLag - minLag + 1)
        val bestR = bestRaw / energy
        val confidence = ((bestR - meanR) / (1.0 - meanR).coerceAtLeast(1e-6) * 2.5)

        // Joint fine search of tempo and phase: a tiny tempo error drifts the grid by
        // hundreds of ms over a few minutes, so refine the lag to 0.01-frame precision.
        val phaseEnv = if (envs.low.sum() > envs.full.sum() * 0.02) envs.low else env
        fun interp(a: DoubleArray, x: Double): Double {
            val i = x.toInt(); if (i + 1 >= a.size) return 0.0
            val f = x - i; return a[i] * (1 - f) + a[i + 1] * f
        }
        var bestPhase = 0.0; var bestSum = -1.0; var refinedLag = bestLag
        var lagC = bestLag * 0.985
        while (lagC <= bestLag * 1.015) {
            var p = 0.0
            while (p < lagC) {
                var s = 0.0
                var pos = p
                while (pos < phaseEnv.size - 1) { s += interp(phaseEnv, pos); pos += lagC }
                if (s > bestSum) { bestSum = s; bestPhase = p; refinedLag = lagC }
                p += 0.5
            }
            lagC += 0.02
        }
        val refinedBpm = 60 * fps / refinedLag

        // Reject signals with no real onsets (pure tones, speech-like or near-silent audio)
        val meanFlux = env.average()
        val peaky = env.sortedDescending().take(max(1, env.size / 20)).average() / (meanFlux + 1e-9)
        val conf = if (meanFlux < 0.05 || peaky < 3.0) 0.0 else confidence.coerceIn(0.0, 1.0)
        return Triple(if (conf > 0) refinedBpm else 0.0, conf, bestPhase)
    }

    private fun blockDb(buf: FloatArray, n: Int): Double {
        var s = 0.0
        for (i in 0 until n * 2) s += buf[i] * buf[i]
        val rms = sqrt(s / max(1, n * 2))
        return if (rms > 1e-6) 20 * log10(rms) else -90.0
    }

    private fun firstLoudMs(src: MixRenderer.PcmSource, thresholdDb: Double): Long {
        val block = SR / 20 // 50 ms
        val buf = FloatArray(block * 2)
        var pos = 0L
        while (true) {
            val n = StudioDsp.readFully(src, buf, 0, block)
            if (n <= 0) return 0L
            if (blockDb(buf, n) > thresholdDb) return pos * 1000 / SR
            pos += n
        }
    }

    /** End of the musical content inside the tail window (cuts long fade-outs and silence). */
    private fun lastLoudMs(src: MixRenderer.PcmSource, thresholdDb: Double): Long {
        val block = SR / 20
        val buf = FloatArray(block * 2)
        var pos = 0L
        var lastLoud = 0L
        while (true) {
            val n = StudioDsp.readFully(src, buf, 0, block)
            if (n <= 0) break
            pos += n
            if (blockDb(buf, n) > thresholdDb) lastLoud = pos
        }
        return lastLoud * 1000 / SR
    }
}

/**
 * Turns a set of analysed songs into a ready-to-render mix plan:
 * order, in/out points on the beat, loudness matching and a transition per change.
 */
object AutoMixPlanner {

    enum class Style(val label: String, val description: String) {
        SMOOTH("Smooth", "Long, gentle blends"),
        CLUB("Club", "Bass swaps on the beat"),
        RADIO("Radio", "Short and punchy"),
        PARTY_90S("90s Party", "Filter sweeps with scratches & rewinds")
    }

    data class Input(val key: String, val uri: String, val durationMs: Long, val analysis: TrackAnalysis)

    data class Planned(
        val key: String,
        val inMs: Long,
        val outMs: Long,
        val gain: Float,
        val transition: MixRenderer.Transition,
        val transitionSeconds: Float,
        val drop: MixRenderer.OneShot
    )

    fun plan(inputs: List<Input>, style: Style, reorder: Boolean, targetDb: Double = -14.0): List<Planned> {
        if (inputs.isEmpty()) return emptyList()
        val ordered = if (reorder) orderForFlow(inputs) else inputs

        return ordered.mapIndexed { idx, item ->
            val a = item.analysis
            val next = ordered.getOrNull(idx + 1)?.analysis
            val isLast = idx == ordered.lastIndex

            // Loudness match, clamped so quiet acoustic songs don't get blasted
            val gain = StudioDsp.dbToGain(targetDb - a.loudnessDb).coerceIn(0.5, 1.8).toFloat()

            // Transition choice
            val (transition, beats) = when {
                isLast || next == null -> MixRenderer.Transition.CROSSFADE to 0
                !a.hasBeat || !next.hasBeat -> MixRenderer.Transition.CROSSFADE to 0
                else -> {
                    val diff = abs(a.bpm - next.bpm) / a.bpm
                    if (diff <= 0.06) when (style) {
                        Style.SMOOTH -> MixRenderer.Transition.CROSSFADE to 32
                        Style.CLUB -> MixRenderer.Transition.BASS_SWAP to 32
                        Style.RADIO -> MixRenderer.Transition.CROSSFADE to 8
                        Style.PARTY_90S -> (if (idx % 2 == 0) MixRenderer.Transition.FILTER_SWEEP else MixRenderer.Transition.BASS_SWAP) to 16
                    } else when (style) {
                        Style.SMOOTH -> MixRenderer.Transition.REVERB_WASH to 16
                        Style.CLUB -> MixRenderer.Transition.ECHO_OUT to 16
                        Style.RADIO -> MixRenderer.Transition.ECHO_OUT to 8
                        Style.PARTY_90S -> (if (idx % 2 == 0) MixRenderer.Transition.BRAKE else MixRenderer.Transition.LOOP_ROLL) to 0
                    }
                }
            }
            val seconds = when {
                beats > 0 -> (beats * a.beatMs / 1000.0).toFloat().coerceIn(2f, 16f)
                transition == MixRenderer.Transition.CROSSFADE -> when (style) {
                    Style.SMOOTH -> 10f; Style.CLUB -> 8f; Style.RADIO -> 4f; Style.PARTY_90S -> 6f
                }
                else -> transition.defaultSeconds.toFloat()
            }
            val drop = if (style == Style.PARTY_90S && !isLast) {
                when (idx % 3) { 0 -> MixRenderer.OneShot.VINYL_SCRATCH; 1 -> MixRenderer.OneShot.REWIND; else -> MixRenderer.OneShot.NONE }
            } else MixRenderer.OneShot.NONE

            // In point: first downbeat after the intro silence; out point: on the beat before the fade-out
            val inMs = if (idx == 0) a.audioStartMs else a.snapToBeat(a.audioStartMs).coerceAtLeast(a.audioStartMs)
            val rawOut = if (isLast) a.audioEndMs else a.snapToBeat(a.audioEndMs - 500)
            val outMs = rawOut.coerceIn(inMs + 10_000, max(inMs + 10_000, a.durationMs))

            Planned(item.key, inMs, outMs, gain, transition, seconds, drop)
        }.let { planned ->
            // keep the planner's order
            planned
        }
    }

    /** Order by tempo in a gentle rise-and-fall energy arc (slower songs at both ends). */
    private fun orderForFlow(inputs: List<Input>): List<Input> {
        val withBeat = inputs.filter { it.analysis.hasBeat }.sortedBy { it.analysis.bpm }
        val noBeat = inputs.filterNot { it.analysis.hasBeat }
        if (withBeat.size < 3) return withBeat + noBeat
        val rising = withBeat.filterIndexed { i, _ -> i % 2 == 0 }
        val falling = withBeat.filterIndexed { i, _ -> i % 2 == 1 }.reversed()
        return rising + falling + noBeat
    }
}
