package com.example.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Streaming DSP building blocks for DJ Studio. Every processor is a [MixRenderer.PcmSource]
 * that wraps another source (interleaved stereo float, 44.1 kHz), so they can be chained:
 *   Decoder -> TimeStretch -> Spectral (noise) -> RetroFx -> output
 * None of them hold more than a few thousand frames in memory.
 */
object StudioDsp {
    const val SR = StudioRenderEngine.SAMPLE_RATE

    // =============================================================================================
    // FFT
    // =============================================================================================

    /** In-place iterative radix-2 complex FFT. [inverse] = true computes the unscaled inverse. */
    fun fft(re: DoubleArray, im: DoubleArray, inverse: Boolean = false) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        var len = 2
        while (len <= n) {
            val ang = 2 * PI / len * (if (inverse) 1 else -1)
            val wRe = cos(ang); val wIm = sin(ang)
            var i = 0
            while (i < n) {
                var curRe = 1.0; var curIm = 0.0
                for (k in 0 until len / 2) {
                    val aRe = re[i + k]; val aIm = im[i + k]
                    val bRe = re[i + k + len / 2] * curRe - im[i + k + len / 2] * curIm
                    val bIm = re[i + k + len / 2] * curIm + im[i + k + len / 2] * curRe
                    re[i + k] = aRe + bRe; im[i + k] = aIm + bIm
                    re[i + k + len / 2] = aRe - bRe; im[i + k + len / 2] = aIm - bIm
                    val nRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = nRe
                }
                i += len
            }
            len = len shl 1
        }
    }

    fun hann(n: Int) = DoubleArray(n) { 0.5 - 0.5 * cos(2 * PI * it / n) }

    // =============================================================================================
    // Generic helpers
    // =============================================================================================

    /** Pull-based helper: fills [n] frames, returns frames read (less only at end of stream). */
    fun readFully(src: MixRenderer.PcmSource, buf: FloatArray, offset: Int, n: Int): Int {
        var got = 0
        while (got < n) {
            val r = src.read(buf, offset + got, n - got)
            if (r <= 0) break
            got += r
        }
        return got
    }

    /** A source made of an in-memory buffer (one-shots, tests). */
    class ArraySource(private val data: FloatArray) : MixRenderer.PcmSource {
        private var pos = 0
        override fun read(out: FloatArray, offsetFrames: Int, maxFrames: Int): Int {
            val n = min(maxFrames, data.size / 2 - pos)
            if (n <= 0) return 0
            System.arraycopy(data, pos * 2, out, offsetFrames * 2, n * 2)
            pos += n
            return n
        }
        override fun release() {}
    }

    /** Leading silence then the upstream (used to offset mashup layers). */
    class DelaySource(private val up: MixRenderer.PcmSource, private var silenceFrames: Long) : MixRenderer.PcmSource {
        override fun read(out: FloatArray, offsetFrames: Int, maxFrames: Int): Int {
            if (silenceFrames > 0) {
                val n = min(maxFrames.toLong(), silenceFrames).toInt()
                java.util.Arrays.fill(out, offsetFrames * 2, (offsetFrames + n) * 2, 0f)
                silenceFrames -= n
                return n
            }
            return up.read(out, offsetFrames, maxFrames)
        }
        override fun release() = up.release()
    }

    // =============================================================================================
    // Speed change (pitch + tempo together, like a turntable)
    // =============================================================================================

    class SpeedSource(private val up: MixRenderer.PcmSource, private val rate: Double) : MixRenderer.PcmSource {
        private var buf = FloatArray(8192 * 2)
        private var frames = 0
        private var pos = 0.0
        private var ended = false

        override fun read(out: FloatArray, offsetFrames: Int, maxFrames: Int): Int {
            var produced = 0
            while (produced < maxFrames) {
                val i = pos.toInt()
                if (i + 1 >= frames) {
                    if (ended || !refill()) break
                    continue
                }
                val f = (pos - i).toFloat()
                val o = (offsetFrames + produced) * 2
                out[o] = buf[i * 2] + (buf[i * 2 + 2] - buf[i * 2]) * f
                out[o + 1] = buf[i * 2 + 1] + (buf[i * 2 + 3] - buf[i * 2 + 1]) * f
                produced++
                pos += rate
            }
            return produced
        }

        private fun refill(): Boolean {
            val keep = pos.toInt().coerceAtMost(frames)
            val remain = frames - keep
            System.arraycopy(buf, keep * 2, buf, 0, remain * 2)
            frames = remain
            pos -= keep
            val n = up.read(buf, frames, buf.size / 2 - frames)
            if (n <= 0) { ended = true; return false }
            frames += n
            return true
        }

        override fun release() = up.release()
    }

    // =============================================================================================
    // WSOLA time-stretch (change tempo, keep pitch)
    // =============================================================================================

    /**
     * Waveform-similarity overlap-add. [tempo] > 1 plays faster, < 1 slower, pitch unchanged.
     * Good for the ±15 % range used for beat-matching.
     */
    class TimeStretchSource(private val up: MixRenderer.PcmSource, private val tempo: Double) : MixRenderer.PcmSource {
        private val n = 2048
        private val hs = n / 2
        private val tol = 512
        private val win = hann(n).map { it.toFloat() }.toFloatArray()

        private var inBuf = FloatArray((n * 4 + tol * 2) * 2)
        private var inFrames = 0
        private var inBase = 0L            // absolute index of inBuf[0]
        private var upEnded = false

        private var analysisPos = 0.0      // ideal absolute start of next segment
        private var prevStart = -1L
        private val olaTail = FloatArray(hs * 2)
        private val pending = FloatArray(hs * 2)
        private var pendingCount = 0
        private var pendingPos = 0
        private var done = false

        private fun ensure(absEnd: Long): Boolean {
            while (inBase + inFrames < absEnd && !upEnded) {
                if (inFrames * 2 + 4096 * 2 > inBuf.size) inBuf = inBuf.copyOf(inBuf.size * 2)
                val r = up.read(inBuf, inFrames, min(4096, inBuf.size / 2 - inFrames))
                if (r <= 0) upEnded = true else inFrames += r
            }
            return inBase + inFrames >= absEnd
        }

        private fun discardBefore(abs: Long) {
            val drop = (abs - inBase).toInt().coerceIn(0, inFrames)
            if (drop <= 0) return
            System.arraycopy(inBuf, drop * 2, inBuf, 0, (inFrames - drop) * 2)
            inFrames -= drop
            inBase += drop
        }

        private fun sample(abs: Long, c: Int): Float {
            val i = (abs - inBase).toInt()
            return if (i in 0 until inFrames) inBuf[i * 2 + c] else 0f
        }

        private fun step(): Boolean {
            val ideal = analysisPos.toLong()
            var start = ideal
            if (prevStart >= 0) {
                val natural = prevStart + hs
                // Search window around the ideal position for best match with the natural continuation
                val lo = max(inBase, ideal - tol)
                val hi = ideal + tol
                if (!ensure(max(hi, natural) + n)) {
                    if (inBase + inFrames <= lo + hs) return false
                }
                var best = Double.NEGATIVE_INFINITY
                var cand = lo
                while (cand <= hi) {
                    var s = 0.0
                    var k = 0
                    while (k < hs) {
                        val a = sample(cand + k, 0) + sample(cand + k, 1)
                        val b = sample(natural + k, 0) + sample(natural + k, 1)
                        s += a * b
                        k += 8
                    }
                    if (s > best) { best = s; start = cand }
                    cand += 4
                }
            } else {
                ensure(start + n)
            }
            if (start >= inBase + inFrames) return false

            // Overlap-add: first half completes the previous tail, second half becomes the new tail
            for (k in 0 until hs) {
                for (c in 0..1) {
                    pending[k * 2 + c] = olaTail[k * 2 + c] + sample(start + k, c) * win[k]
                    olaTail[k * 2 + c] = sample(start + hs + k, c) * win[hs + k]
                }
            }
            pendingCount = hs
            pendingPos = 0
            prevStart = start
            analysisPos += hs * tempo
            discardBefore(min(prevStart, analysisPos.toLong()) - tol - 8)
            return true
        }

        override fun read(out: FloatArray, offsetFrames: Int, maxFrames: Int): Int {
            var produced = 0
            while (produced < maxFrames) {
                if (pendingPos >= pendingCount) {
                    if (done) break
                    if (!step()) {
                        // flush the remaining tail once
                        System.arraycopy(olaTail, 0, pending, 0, hs * 2)
                        pendingCount = hs; pendingPos = 0; done = true
                    }
                    // the very first step only primes the tail with half a window of silence-overlap
                }
                val m = min(maxFrames - produced, pendingCount - pendingPos)
                System.arraycopy(pending, pendingPos * 2, out, (offsetFrames + produced) * 2, m * 2)
                pendingPos += m
                produced += m
            }
            return produced
        }

        override fun release() = up.release()
    }

    // =============================================================================================
    // STFT framework (2048-point, 75 % overlap, Hann/Hann, exact reconstruction when untouched)
    // =============================================================================================

    fun interface SpectralFrame {
        /** Modify the stereo spectra in place. Arrays hold [n/2 + 1] meaningful bins. */
        fun process(lRe: DoubleArray, lIm: DoubleArray, rRe: DoubleArray, rIm: DoubleArray)
    }

    class StftSource(
        private val up: MixRenderer.PcmSource,
        private val processor: SpectralFrame,
        private val n: Int = 2048
    ) : MixRenderer.PcmSource {
        private val hop = n / 4
        private val win = hann(n)
        private val norm = 1.0 / 1.5 // sum of hann^2 at 75 % overlap
        private val inL = DoubleArray(n); private val inR = DoubleArray(n)
        private val accL = DoubleArray(n); private val accR = DoubleArray(n)
        private val lRe = DoubleArray(n); private val lIm = DoubleArray(n)
        private val rRe = DoubleArray(n); private val rIm = DoubleArray(n)
        private val hopBuf = FloatArray(hop * 2)
        private val outQ = FloatArray(hop * 2)
        private var outCount = 0
        private var outPos = 0
        private var latencyToDrop = n - hop
        private var inputFrames = 0L
        private var emitted = 0L
        private var upEnded = false
        private var flushHops = 0

        private fun step(): Boolean {
            var got = 0
            if (!upEnded) {
                got = readFully(up, hopBuf, 0, hop)
                if (got < hop) upEnded = true
                inputFrames += got
            }
            if (upEnded) {
                if (got == 0) {
                    if (flushHops >= n / hop) return false
                    flushHops++
                }
                java.util.Arrays.fill(hopBuf, got * 2, hop * 2, 0f)
            }
            // shift input window
            System.arraycopy(inL, hop, inL, 0, n - hop); System.arraycopy(inR, hop, inR, 0, n - hop)
            for (k in 0 until hop) { inL[n - hop + k] = hopBuf[k * 2].toDouble(); inR[n - hop + k] = hopBuf[k * 2 + 1].toDouble() }
            for (k in 0 until n) {
                lRe[k] = inL[k] * win[k]; lIm[k] = 0.0
                rRe[k] = inR[k] * win[k]; rIm[k] = 0.0
            }
            fft(lRe, lIm); fft(rRe, rIm)
            processor.process(lRe, lIm, rRe, rIm)
            // enforce conjugate symmetry so the inverse is real
            for (k in 1 until n / 2) {
                lRe[n - k] = lRe[k]; lIm[n - k] = -lIm[k]
                rRe[n - k] = rRe[k]; rIm[n - k] = -rIm[k]
            }
            lIm[0] = 0.0; rIm[0] = 0.0; lIm[n / 2] = 0.0; rIm[n / 2] = 0.0
            fft(lRe, lIm, inverse = true); fft(rRe, rIm, inverse = true)
            for (k in 0 until n) {
                accL[k] += lRe[k] / n * win[k] * norm
                accR[k] += rRe[k] / n * win[k] * norm
            }
            // first hop of the accumulator is now complete
            outCount = 0; outPos = 0
            for (k in 0 until hop) {
                if (latencyToDrop > 0) { latencyToDrop--; continue }
                if (emitted >= inputFrames && upEnded) break
                outQ[outCount * 2] = accL[k].toFloat()
                outQ[outCount * 2 + 1] = accR[k].toFloat()
                outCount++
                emitted++
            }
            System.arraycopy(accL, hop, accL, 0, n - hop); java.util.Arrays.fill(accL, n - hop, n, 0.0)
            System.arraycopy(accR, hop, accR, 0, n - hop); java.util.Arrays.fill(accR, n - hop, n, 0.0)
            return true
        }

        override fun read(out: FloatArray, offsetFrames: Int, maxFrames: Int): Int {
            var produced = 0
            while (produced < maxFrames) {
                if (outPos >= outCount) {
                    if (upEnded && emitted >= inputFrames) break
                    if (!step()) break
                    continue
                }
                val m = min(maxFrames - produced, outCount - outPos)
                System.arraycopy(outQ, outPos * 2, out, (offsetFrames + produced) * 2, m * 2)
                outPos += m
                produced += m
            }
            return produced
        }

        override fun release() = up.release()
    }

    /**
     * One STFT analysis feeding several outputs ("stems"): each output gets its own copy of the
     * spectrum, its own processor (null = untouched) and its own overlap-add. All outputs stay
     * sample-aligned, so a player can switch between them instantly.
     */
    class MultiStft(
        private val up: MixRenderer.PcmSource,
        private val processors: List<SpectralFrame?>,
        private val n: Int = 2048
    ) {
        val count = processors.size
        private val hop = n / 4
        private val win = hann(n)
        private val norm = 1.0 / 1.5
        private val inL = DoubleArray(n); private val inR = DoubleArray(n)
        private val bRe = DoubleArray(n); private val bIm = DoubleArray(n)
        private val cRe = DoubleArray(n); private val cIm = DoubleArray(n)
        private val lRe = DoubleArray(n); private val lIm = DoubleArray(n)
        private val rRe = DoubleArray(n); private val rIm = DoubleArray(n)
        private val accL = Array(count) { DoubleArray(n) }
        private val accR = Array(count) { DoubleArray(n) }
        private val hopBuf = FloatArray(hop * 2)
        private val outQ = Array(count) { FloatArray(hop * 2) }
        private var outCount = 0
        private var outPos = 0
        private var latencyToDrop = n - hop
        private var inputFrames = 0L
        private var emitted = 0L
        private var upEnded = false
        private var flushHops = 0

        private fun step(): Boolean {
            var got = 0
            if (!upEnded) {
                got = readFully(up, hopBuf, 0, hop)
                if (got < hop) upEnded = true
                inputFrames += got
            }
            if (upEnded) {
                if (got == 0) {
                    if (flushHops >= n / hop) return false
                    flushHops++
                }
                java.util.Arrays.fill(hopBuf, got * 2, hop * 2, 0f)
            }
            System.arraycopy(inL, hop, inL, 0, n - hop); System.arraycopy(inR, hop, inR, 0, n - hop)
            for (k in 0 until hop) { inL[n - hop + k] = hopBuf[k * 2].toDouble(); inR[n - hop + k] = hopBuf[k * 2 + 1].toDouble() }
            for (k in 0 until n) {
                bRe[k] = inL[k] * win[k]; bIm[k] = 0.0
                cRe[k] = inR[k] * win[k]; cIm[k] = 0.0
            }
            fft(bRe, bIm); fft(cRe, cIm)
            for (o in 0 until count) {
                System.arraycopy(bRe, 0, lRe, 0, n); System.arraycopy(bIm, 0, lIm, 0, n)
                System.arraycopy(cRe, 0, rRe, 0, n); System.arraycopy(cIm, 0, rIm, 0, n)
                processors[o]?.process(lRe, lIm, rRe, rIm)
                for (k in 1 until n / 2) {
                    lRe[n - k] = lRe[k]; lIm[n - k] = -lIm[k]
                    rRe[n - k] = rRe[k]; rIm[n - k] = -rIm[k]
                }
                lIm[0] = 0.0; rIm[0] = 0.0; lIm[n / 2] = 0.0; rIm[n / 2] = 0.0
                fft(lRe, lIm, inverse = true); fft(rRe, rIm, inverse = true)
                val aL = accL[o]; val aR = accR[o]
                for (k in 0 until n) {
                    aL[k] += lRe[k] / n * win[k] * norm
                    aR[k] += rRe[k] / n * win[k] * norm
                }
            }
            outCount = 0; outPos = 0
            for (k in 0 until hop) {
                if (latencyToDrop > 0) { latencyToDrop--; continue }
                if (emitted >= inputFrames && upEnded) break
                for (o in 0 until count) {
                    outQ[o][outCount * 2] = accL[o][k].toFloat()
                    outQ[o][outCount * 2 + 1] = accR[o][k].toFloat()
                }
                outCount++
                emitted++
            }
            for (o in 0 until count) {
                System.arraycopy(accL[o], hop, accL[o], 0, n - hop); java.util.Arrays.fill(accL[o], n - hop, n, 0.0)
                System.arraycopy(accR[o], hop, accR[o], 0, n - hop); java.util.Arrays.fill(accR[o], n - hop, n, 0.0)
            }
            return true
        }

        /** Fill each of [outs] (interleaved stereo) with the same span of frames. Returns frames. */
        fun read(outs: Array<FloatArray>, maxFrames: Int): Int {
            var produced = 0
            while (produced < maxFrames) {
                if (outPos >= outCount) {
                    if (upEnded && emitted >= inputFrames) break
                    if (!step()) break
                    continue
                }
                val m = min(maxFrames - produced, outCount - outPos)
                for (o in 0 until count) System.arraycopy(outQ[o], outPos * 2, outs[o], produced * 2, m * 2)
                outPos += m
                produced += m
            }
            return produced
        }

        fun release() = up.release()
    }

    // =============================================================================================
    // Noise remover (spectral subtraction with a learned noise profile)
    // =============================================================================================

    /** Per-bin noise magnitude, learned from the quietest moments of the track. */
    class NoiseProfile(val mag: DoubleArray)

    /**
     * Scan a source and estimate the noise floor per frequency bin as a low percentile
     * of the magnitude spectrum (noise is always there; music comes and goes).
     */
    fun learnNoiseProfile(src: MixRenderer.PcmSource, maxSeconds: Int = 240, n: Int = 2048): NoiseProfile {
        val hop = n
        val win = hann(n)
        val bins = n / 2 + 1
        val frames = ArrayList<FloatArray>()
        val buf = FloatArray(hop * 2)
        val re = DoubleArray(n); val im = DoubleArray(n)
        var total = 0L
        val limit = maxSeconds.toLong() * SR
        while (total < limit) {
            val got = readFully(src, buf, 0, hop)
            if (got < hop) break
            total += got
            for (k in 0 until n) { re[k] = (buf[k * 2] + buf[k * 2 + 1]) * 0.5 * win[k]; im[k] = 0.0 }
            fft(re, im)
            val mags = FloatArray(bins) { sqrt(re[it] * re[it] + im[it] * im[it]).toFloat() }
            frames.add(mags)
        }
        val profile = DoubleArray(bins)
        if (frames.isEmpty()) return NoiseProfile(profile)
        // Ignore digital silence frames (fade-ins) when estimating noise
        val energies = frames.map { f -> f.sum() }
        val active = frames.filterIndexed { i, _ -> energies[i] > 1e-3f }.ifEmpty { frames }
        val col = FloatArray(active.size)
        for (b in 0 until bins) {
            for (i in active.indices) col[i] = active[i][b]
            col.sort()
            profile[b] = col[(col.size * 0.12).toInt().coerceIn(0, col.size - 1)].toDouble()
        }
        // The STFT in StftSource uses the same window but hop n/4; magnitudes are comparable.
        return NoiseProfile(profile)
    }

    /** Wiener-style gain with temporal smoothing to avoid "musical noise". */
    class NoiseReducer(private val profile: NoiseProfile, strength: Float) : SpectralFrame {
        private val alpha = 1.0 + 2.0 * strength          // over-subtraction 1..3
        private val floor = 0.30 - 0.24 * strength        // residual 0.30..0.06
        private val prevGain = DoubleArray(profile.mag.size) { 1.0 }

        override fun process(lRe: DoubleArray, lIm: DoubleArray, rRe: DoubleArray, rIm: DoubleArray) {
            for (k in profile.mag.indices) {
                val mRe = (lRe[k] + rRe[k]) * 0.5
                val mIm = (lIm[k] + rIm[k]) * 0.5
                val mag = sqrt(mRe * mRe + mIm * mIm) + 1e-9
                var g = 1.0 - alpha * profile.mag[k] / mag
                if (g < floor) g = floor
                // fast attack, slow release
                g = if (g > prevGain[k]) 0.5 * prevGain[k] + 0.5 * g else 0.85 * prevGain[k] + 0.15 * g
                prevGain[k] = g
                lRe[k] *= g; lIm[k] *= g; rRe[k] *= g; rIm[k] *= g
            }
        }
    }

    // =============================================================================================
    // Vocal remover / isolator (frequency-domain centre extraction)
    // =============================================================================================

    // =============================================================================================
    // Retro FX ("Time Machine" + "Remix Lab")
    // =============================================================================================

    enum class RetroPreset(val label: String, val emoji: String, val description: String) {
        CASSETTE("Cassette", "📼", "Tape hiss, wow & flutter, warm saturation"),
        VINYL("Vinyl", "💿", "Crackle, pops and low rumble"),
        AM_RADIO("AM Radio", "📻", "Narrow band, static and fading"),
        TELEPHONE("Telephone", "📞", "Thin answering-machine voice"),
        GAME_BOY("8-Bit", "🎮", "Crunchy bit-crushed video-game sound"),
        LOFI("Lo-Fi Chill", "🌙", "Muffled, mellow with soft crackle"),
        SLOWED_REVERB("Slowed + Reverb", "🐌", "Slower, deeper, dreamy reverb"),
        NIGHTCORE("Nightcore", "⚡", "Faster and higher"),
        CHOPPED_SCREWED("Chopped & Screwed", "🔊", "Houston style: slow with stutters"),
        EIGHT_D("8D Audio", "🎧", "Sound circles around your head");

        /** Speed factor applied before the effect (1 = unchanged). */
        val speed: Double
            get() = when (this) {
                SLOWED_REVERB -> 0.85
                NIGHTCORE -> 1.25
                CHOPPED_SCREWED -> 0.72
                else -> 1.0
            }
    }

    /** Builds the full processing chain for a preset around a decoded source. */
    fun retroChain(src: MixRenderer.PcmSource, preset: RetroPreset, intensity: Float): MixRenderer.PcmSource {
        val sped = if (preset.speed != 1.0) {
            // intensity also scales how far the speed moves away from 1.0
            val rate = 1.0 + (preset.speed - 1.0) * (0.4 + 0.6 * intensity)
            SpeedSource(src, rate)
        } else src
        return RetroFxSource(sped, preset, intensity)
    }

    class RetroFxSource(
        private val up: MixRenderer.PcmSource,
        private val preset: RetroPreset,
        private val intensity: Float
    ) : MixRenderer.PcmSource {
        private val rnd = java.util.Random(1999)
        private var t = 0L
        private val lpL = MixRenderer.Biquad(); private val lpR = MixRenderer.Biquad()
        private val hpL = MixRenderer.Biquad(); private val hpR = MixRenderer.Biquad()
        private val rumble = MixRenderer.Biquad().apply { lowPass(40.0, 0.7) }
        private val crackleLp = MixRenderer.Biquad().apply { lowPass(5000.0, 0.7) }
        private val reverbL = SimpleReverb(0); private val reverbR = SimpleReverb(37)
        // modulated delay for wow/flutter
        private val dlen = 2048
        private val dl = FloatArray(dlen * 2)
        private var dw = 0
        // 8-bit sample & hold
        private var holdL = 0f; private var holdR = 0f
        // chopped & screwed stutter
        private val chopLen = (SR * 0.42).toInt()
        private val chopBuf = FloatArray(chopLen * 2)
        private var chopCounter = 0L
        private var chopPlay = -1

        init {
            when (preset) {
                RetroPreset.CASSETTE -> { lp(11000.0); hp(45.0) }
                RetroPreset.VINYL -> { lp(12500.0); hp(25.0) }
                RetroPreset.AM_RADIO -> { lp(4200.0); hp(320.0) }
                RetroPreset.TELEPHONE -> { lp(3300.0); hp(380.0) }
                RetroPreset.GAME_BOY -> { lp(9000.0); hp(60.0) }
                RetroPreset.LOFI -> { lp(3600.0); hp(60.0) }
                RetroPreset.SLOWED_REVERB, RetroPreset.CHOPPED_SCREWED -> { lp(16000.0); hp(25.0) }
                else -> { lp(19000.0); hp(20.0) }
            }
        }

        private fun lp(f: Double) { lpL.lowPass(f, 0.707); lpR.lowPass(f, 0.707) }
        private fun hp(f: Double) { hpL.highPass(f, 0.707); hpR.highPass(f, 0.707) }

        override fun read(out: FloatArray, offsetFrames: Int, maxFrames: Int): Int {
            val n = up.read(out, offsetFrames, maxFrames)
            if (n <= 0) return n
            val mix = intensity.coerceIn(0f, 1f)
            for (f in 0 until n) {
                val i = (offsetFrames + f) * 2
                val dryL = out[i]; val dryR = out[i + 1]
                var l = dryL; var r = dryR
                val time = t.toDouble() / SR
                when (preset) {
                    RetroPreset.CASSETTE -> {
                        val wow = 0.0012 * sin(2 * PI * 0.55 * time) + 0.00025 * sin(2 * PI * 6.5 * time)
                        val (wl, wr) = modDelay(l, r, (0.0025 + wow * mix) * SR)
                        l = sat(lpL.process(hpL.process(wl)), 1.6f)
                        r = sat(lpR.process(hpR.process(wr)), 1.6f)
                        val hiss = (rnd.nextGaussian() * 0.006 * mix).toFloat()
                        l += hiss; r += hiss
                    }
                    RetroPreset.VINYL -> {
                        l = lpL.process(hpL.process(l)); r = lpR.process(hpR.process(r))
                        val mono = (l + r) * 0.5f
                        l = l * 0.8f + mono * 0.2f; r = r * 0.8f + mono * 0.2f
                        val crackle = crackleSample(0.00035 * mix, 0.35f)
                        val rum = rumble.process((rnd.nextGaussian() * 0.25).toFloat()) * 0.15f * mix
                        l += crackle + rum; r += crackle + rum
                    }
                    RetroPreset.AM_RADIO -> {
                        val mono = (l + r) * 0.5f
                        var s = sat(lpL.process(hpL.process(mono)) * 1.4f, 2.2f)
                        val fade = 1f - 0.25f * mix * (0.5f + 0.5f * sin(2 * PI * 0.17 * time).toFloat())
                        s = s * fade + (rnd.nextGaussian() * 0.018 * mix).toFloat()
                        l = s; r = s
                    }
                    RetroPreset.TELEPHONE -> {
                        val mono = (l + r) * 0.5f
                        var s = lpL.process(hpL.process(mono)) * 1.6f
                        s = quantize(sat(s, 2.5f), 7)
                        l = s; r = s
                    }
                    RetroPreset.GAME_BOY -> {
                        // sample-rate reduce to ~11 kHz and crush to 5 bits
                        if (t % 4L == 0L) { holdL = l; holdR = r }
                        l = lpL.process(quantize(holdL, 5)); r = lpR.process(quantize(holdR, 5))
                    }
                    RetroPreset.LOFI -> {
                        val wow = 0.0008 * sin(2 * PI * 0.4 * time)
                        val (wl, wr) = modDelay(l, r, (0.002 + wow) * SR)
                        l = lpL.process(hpL.process(wl)); r = lpR.process(hpR.process(wr))
                        val mono = (l + r) * 0.5f
                        l = l * 0.6f + mono * 0.4f; r = r * 0.6f + mono * 0.4f
                        val crackle = crackleSample(0.00018, 0.18f)
                        l += crackle; r += crackle
                    }
                    RetroPreset.SLOWED_REVERB -> {
                        l = lpL.process(l); r = lpR.process(r)
                        l += reverbL.process(l) * 0.55f; r += reverbR.process(r) * 0.55f
                    }
                    RetroPreset.NIGHTCORE -> { /* speed change already applied */ }
                    RetroPreset.CHOPPED_SCREWED -> {
                        l = lpL.process(l); r = lpR.process(r)
                        // keep the last 0.42 s; every ~8 s replay it twice ("chop")
                        val ci = (chopCounter % chopLen).toInt()
                        if (chopPlay >= 0) {
                            l = chopBuf[chopPlay * 2]; r = chopBuf[chopPlay * 2 + 1]
                            chopPlay++
                            if (chopPlay >= chopLen) chopPlay = -1
                        } else {
                            chopBuf[ci * 2] = l; chopBuf[ci * 2 + 1] = r
                            if (chopCounter > 0 && chopCounter % (SR * 8L) == 0L) chopPlay = 0
                        }
                        chopCounter++
                    }
                    RetroPreset.EIGHT_D -> {
                        val angle = 2 * PI * 0.12 * time
                        val pan = sin(angle).toFloat()          // -1..1
                        val mono = (l + r) * 0.5f
                        val gl = sqrt((1f - pan) * 0.5f); val gr = sqrt((1f + pan) * 0.5f)
                        l = mono * gl + reverbL.process(mono) * 0.18f
                        r = mono * gr + reverbR.process(mono) * 0.18f
                    }
                }
                out[i] = dryL + (l - dryL) * mix
                out[i + 1] = dryR + (r - dryR) * mix
                t++
            }
            return n
        }

        private fun modDelay(l: Float, r: Float, delayFrames: Double): Pair<Float, Float> {
            dl[dw * 2] = l; dl[dw * 2 + 1] = r
            val rp = (dw - delayFrames + dlen) % dlen
            val i0 = floor(rp).toInt(); val fr = (rp - i0).toFloat()
            val i1 = (i0 + 1) % dlen
            val ol = dl[i0 * 2] * (1 - fr) + dl[i1 * 2] * fr
            val orr = dl[i0 * 2 + 1] * (1 - fr) + dl[i1 * 2 + 1] * fr
            dw = (dw + 1) % dlen
            return ol to orr
        }

        private var crackleEnv = 0f
        private fun crackleSample(prob: Double, level: Float): Float {
            if (rnd.nextDouble() < prob) crackleEnv = level * (0.4f + rnd.nextFloat())
            val s = crackleLp.process((rnd.nextGaussian() * crackleEnv).toFloat())
            crackleEnv *= 0.82f
            return s
        }

        override fun release() = up.release()
    }

    private fun sat(x: Float, drive: Float): Float = (tanh((x * drive).toDouble()) / tanh(drive.toDouble())).toFloat()

    private fun quantize(x: Float, bits: Int): Float {
        val q = (1 shl (bits - 1)).toFloat()
        return (Math.round(x * q) / q).coerceIn(-1f, 1f)
    }

    /** Compact Schroeder reverb used by the FX presets. */
    class SimpleReverb(spread: Int) {
        private val combs = intArrayOf(1557, 1617, 1491, 1422).map { Comb(it + spread) }
        private val aps = intArrayOf(225, 556).map { AllPass(it + spread) }
        fun process(x: Float): Float {
            var s = 0f
            for (c in combs) s += c.process(x * 0.22f)
            var y = s
            for (a in aps) y = a.process(y)
            return y
        }
        private class Comb(size: Int) {
            private val b = FloatArray(size); private var i = 0; private var st = 0f
            fun process(x: Float): Float { val o = b[i]; st = o * 0.75f + st * 0.25f; b[i] = x + st * 0.84f; i = (i + 1) % b.size; return o }
        }
        private class AllPass(size: Int) {
            private val b = FloatArray(size); private var i = 0
            fun process(x: Float): Float { val bo = b[i]; b[i] = x + bo * 0.5f; i = (i + 1) % b.size; return bo - x }
        }
    }

    // =============================================================================================
    // Small utilities shared by mashup / cut tools
    // =============================================================================================

    /** Per-sample gain envelope (fade in/out over [totalFrames]) + constant-power pan. */
    class GainPanSource(
        private val up: MixRenderer.PcmSource,
        private val gain: Float,
        private val pan: Float,           // -1 left .. +1 right
        private val fadeInFrames: Long,
        private val fadeOutFrames: Long,
        private val totalFrames: Long     // length of the audible part (for fade-out), <=0 unknown
    ) : MixRenderer.PcmSource {
        private var pos = 0L
        private val gl = sqrt((1f - pan.coerceIn(-1f, 1f)) * 0.5f) * 1.4142f
        private val gr = sqrt((1f + pan.coerceIn(-1f, 1f)) * 0.5f) * 1.4142f
        override fun read(out: FloatArray, offsetFrames: Int, maxFrames: Int): Int {
            val n = up.read(out, offsetFrames, maxFrames)
            for (f in 0 until max(n, 0)) {
                var g = gain
                if (fadeInFrames > 0 && pos < fadeInFrames) g *= pos.toFloat() / fadeInFrames
                if (fadeOutFrames > 0 && totalFrames > 0) {
                    val left = totalFrames - pos
                    if (left < fadeOutFrames) g *= (left.toFloat() / fadeOutFrames).coerceIn(0f, 1f)
                }
                val i = (offsetFrames + f) * 2
                out[i] *= g * gl; out[i + 1] *= g * gr
                pos++
            }
            return n
        }
        override fun release() = up.release()
    }

    fun softClip(data: FloatArray, frames: Int) {
        val knee = 0.8f
        for (i in 0 until frames * 2) {
            val x = data[i]; val ax = abs(x)
            if (ax > knee) {
                val y = knee + (1f - knee) * tanh(((ax - knee) / (1f - knee)).toDouble()).toFloat()
                data[i] = if (x < 0) -y else y
            }
        }
    }

    fun dbToGain(db: Double) = Math.pow(10.0, db / 20.0)
    fun expDecay(t: Double, k: Double) = exp(-t * k)
}
