package com.example

import com.example.audio.AutoMixPlanner
import com.example.audio.MixRenderer
import com.example.audio.OneShotSynth
import com.example.audio.StudioDsp
import com.example.audio.StudioJobs
import com.example.audio.StudioPreviewBuffer
import com.example.audio.TrackAnalysis
import com.example.audio.TrackAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/** Pure-JVM checks of the DJ Studio audio processing using synthetic signals. */
class StudioDspTest {
    private val sr = 44100

    private fun gen(seconds: Double, f: (Int) -> Pair<Float, Float>): FloatArray {
        val n = (seconds * sr).toInt()
        val a = FloatArray(n * 2)
        for (i in 0 until n) { val (l, r) = f(i); a[i * 2] = l; a[i * 2 + 1] = r }
        return a
    }

    private fun drain(src: MixRenderer.PcmSource): FloatArray {
        val out = ArrayList<Float>()
        val b = FloatArray(4096 * 2)
        while (true) { val n = src.read(b, 0, 4096); if (n <= 0) break; for (i in 0 until n * 2) out.add(b[i]) }
        return out.toFloatArray()
    }

    private fun seg(a: FloatArray, s: Double, e: Double) = a.copyOfRange((s * sr).toInt() * 2, (e * sr).toInt() * 2)
    private fun rms(a: FloatArray): Double { var s = 0.0; for (v in a) s += v * v; return sqrt(s / a.size) }
    private fun tone(a: FloatArray, freq: Double): Double {
        val n = a.size / 2; val k = 2 * cos(2 * PI * freq / sr); var s1 = 0.0; var s2 = 0.0
        for (i in 0 until n) { val s = a[i * 2] + k * s1 - s2; s2 = s1; s1 = s }
        return sqrt(s1 * s1 + s2 * s2 - k * s1 * s2) / n
    }

    @Test fun stftReconstructsExactly() {
        val x = gen(2.0) { i -> (sin(2 * PI * 440 * i / sr) * 0.5).toFloat() to (sin(2 * PI * 660 * i / sr) * 0.4).toFloat() }
        val y = drain(StudioDsp.StftSource(StudioDsp.ArraySource(x), { _, _, _, _ -> }))
        assertEquals(x.size, y.size)
        var err = 0.0; for (i in x.indices) err = maxOf(err, abs((x[i] - y[i]).toDouble()))
        assertTrue("max error $err", err < 1e-3)
    }

    @Test fun noiseRemoverCutsHissKeepsMusic() {
        val rnd = java.util.Random(1)
        val noisy = gen(8.0) { i -> val t = i.toDouble() / sr; val m = if ((t % 2) < 1) sin(2 * PI * 330 * t) * 0.4 else 0.0; val n = rnd.nextGaussian() * 0.03; (m + n).toFloat() to (m + n).toFloat() }
        val profile = StudioDsp.learnNoiseProfile(StudioDsp.ArraySource(noisy))
        val clean = drain(StudioDsp.StftSource(StudioDsp.ArraySource(noisy), StudioDsp.NoiseReducer(profile, 0.8f)))
        assertTrue(rms(seg(clean, 1.2, 1.8)) < rms(seg(noisy, 1.2, 1.8)) * 0.5)
        assertTrue(tone(seg(clean, 0.2, 0.8), 330.0) > tone(seg(noisy, 0.2, 0.8), 330.0) * 0.9)
    }

    @Test fun vocalSeparatorRemovesCentreKeepsSidesAndBass() {
        val mix = gen(3.0) { i -> val t = i.toDouble() / sr; val v = sin(2 * PI * 1000 * t) * 0.3; val g = sin(2 * PI * 500 * t) * 0.3; val b = sin(2 * PI * 80 * t) * 0.3; (v + g + b).toFloat() to (v + b).toFloat() }
        val inst = seg(drain(StudioDsp.StftSource(StudioDsp.ArraySource(mix), StudioDsp.VocalSeparator(StudioDsp.VocalMode.INSTRUMENTAL, 1f))), 1.0, 2.5)
        val m = seg(mix, 1.0, 2.5)
        assertTrue(tone(inst, 1000.0) < tone(m, 1000.0) * 0.1)
        assertTrue(tone(inst, 500.0) > tone(m, 500.0) * 0.9)
        assertTrue(tone(inst, 80.0) > tone(m, 80.0) * 0.9)
    }

    @Test fun timeStretchChangesLengthNotPitch() {
        val x = gen(6.0) { i -> (sin(2 * PI * 440 * i / sr) * 0.5).toFloat().let { it to it } }
        val y = drain(StudioDsp.TimeStretchSource(StudioDsp.ArraySource(x), 1.12))
        assertEquals(6.0 / 1.12, y.size / 2.0 / sr, 0.1)
        val s = seg(y, 1.0, 4.0)
        assertTrue(tone(s, 440.0) > tone(s, 493.0) * 5)
    }

    @Test fun retroPresetsAreStable() {
        val x = gen(4.0) { i -> (sin(2 * PI * 440 * i / sr) * 0.5).toFloat().let { it to it } }
        for (p in StudioDsp.RetroPreset.values()) {
            val y = drain(StudioDsp.retroChain(StudioDsp.ArraySource(x), p, 1f))
            assertFalse("$p produced NaN", y.any { it.isNaN() || it.isInfinite() })
            assertTrue("$p silent", rms(y) > 0.05)
        }
    }

    @Test fun mixTransitionsRenderWithoutGapsOrClipping() {
        for (tr in MixRenderer.Transition.values()) {
            val tracks = listOf(
                MixRenderer.MixTrack("a", transition = tr, transitionSeconds = 4f, drop = MixRenderer.OneShot.KICK_808),
                MixRenderer.MixTrack("b")
            )
            val srcs = mutableListOf(
                StudioDsp.ArraySource(gen(12.0) { i -> (sin(2 * PI * 220 * i / sr) * 0.5).toFloat().let { it to it } }),
                StudioDsp.ArraySource(gen(10.0) { i -> (sin(2 * PI * 330 * i / sr) * 0.5).toFloat().let { it to it } })
            )
            var peak = 0f; var frames = 0L
            MixRenderer.render(tracks, { srcs.removeAt(0) }, { d, n -> frames += n; for (k in 0 until n * 2) peak = maxOf(peak, abs(d[k])) }, 1L) {}
            assertTrue("$tr peak $peak", peak <= 1.0001f)
            assertTrue("$tr too short", frames > 15 * sr)
        }
    }

    @Test fun oneShotsAreNormalised() {
        for (s in MixRenderer.OneShot.values()) {
            if (s == MixRenderer.OneShot.NONE) continue
            val pcm = OneShotSynth.render(s)
            assertEquals(0.9f, pcm.maxOf { abs(it) }, 0.01f)
        }
    }

    @Test fun tempoAndBeatDetection() {
        for (bpm in listOf(95.0, 124.0, 140.0)) {
            val beat = 60.0 / bpm
            val x = gen(30.0) { i ->
                val t = i.toDouble() / sr - 0.137
                val v = if (t >= 0) { val tb = t % beat; sin(2 * PI * (50 + 80 * exp(-tb * 30)) * tb) * exp(-tb * 8) * 0.6 } else 0.0
                v.toFloat() to v.toFloat()
            }
            val (detected, conf, phaseMs) = TrackAnalyzer.analyzeSource(StudioDsp.ArraySource(x))
            assertEquals(bpm, detected, 0.5)
            assertTrue(conf > 0.25)
            val err = abs(((phaseMs - 137) % (beat * 1000) + beat * 1000) % (beat * 1000))
            assertTrue("phase error $err", err < 40 || err > beat * 1000 - 40)
        }
    }

    @Test fun mashupLayersAlignToReferenceBeat() {
        val a1 = TrackAnalysis(200000, 120.0, 0.9, 100, 0, 199000, -14.0)
        val a2 = TrackAnalysis(180000, 125.0, 0.9, 300, 0, 179000, -12.0)
        val layers = listOf(
            StudioJobs.MashupLayer("a", "A", 200000),
            StudioJobs.MashupLayer("b", "B", 180000, offsetMs = 5000, inMs = 1000)
        )
        val r = StudioJobs.alignLayers(layers, mapOf("a" to a1, "b" to a2), matchTempo = true, alignBeats = true)
        assertEquals(0.96, r[1].tempo, 0.001)
        val beatAfterIn = (((a2.firstBeatMs - r[1].inMs) % a2.beatMs) + a2.beatMs) % a2.beatMs / r[1].tempo
        val phase = ((r[1].offsetMs + beatAfterIn - 100) % 500 + 500) % 500
        assertTrue("phase $phase", phase < 5 || phase > 495)
    }

    @Test fun autoMixPlansBeatMatchedTransitions() {
        val inputs = listOf(
            AutoMixPlanner.Input("1", "u1", 200000, TrackAnalysis(200000, 124.0, 0.9, 200, 1500, 195000, -10.0)),
            AutoMixPlanner.Input("2", "u2", 210000, TrackAnalysis(210000, 126.0, 0.9, 50, 300, 205000, -16.0)),
            AutoMixPlanner.Input("3", "u3", 180000, TrackAnalysis(180000, 90.0, 0.9, 10, 0, 176000, -12.0))
        )
        val plan = AutoMixPlanner.plan(inputs, AutoMixPlanner.Style.CLUB, reorder = false)
        assertEquals(3, plan.size)
        assertEquals(MixRenderer.Transition.BASS_SWAP, plan[0].transition)   // 124 -> 126: close tempos
        assertEquals(MixRenderer.Transition.ECHO_OUT, plan[1].transition)    // 126 -> 90: tempo clash
        assertTrue(plan[1].gain > plan[0].gain)                               // quieter track is turned up
    }

    @Test fun previewBufferPlaysWhileWritingAndStopsWhenReplaced() {
        val dir = java.nio.file.Files.createTempDirectory("prev").toFile()
        val buf = StudioPreviewBuffer.create(dir, 44100)
        val x = gen(1.0) { i -> (sin(2 * PI * 440 * i / sr) * 0.5).toFloat().let { it to -it } }
        buf.write(x, 10000)
        // readable before the render has finished
        val bytes = ByteArray(4 * 512)
        assertEquals(512, buf.read(100, bytes, 512))
        val l = ((bytes[0].toInt() and 0xFF) or (bytes[1].toInt() shl 8)).toShort() / 32767f
        val r = ((bytes[2].toInt() and 0xFF) or (bytes[3].toInt() shl 8)).toShort() / 32767f
        assertEquals(x[200], l, 1e-3f)
        assertEquals(x[201], r, 1e-3f)
        assertEquals(0, buf.read(10000, bytes, 512))           // nothing past what's written
        buf.write(x.copyOfRange(20000, x.size), 34100)
        buf.finish()
        assertTrue(buf.complete)
        assertEquals(44100L, buf.framesWritten)
        val header = buf.file.readBytes().copyOfRange(0, 4).toString(Charsets.US_ASCII)
        assertEquals("RIFF", header)

        val buf2 = StudioPreviewBuffer.create(dir, 44100)
        buf2.cancelAndDelete()
        var threw = false
        try { buf2.write(x, 100) } catch (e: StudioPreviewBuffer.Cancelled) { threw = true }
        assertTrue("write after replace must stop the renderer", threw)
        assertFalse(buf2.file.exists())
    }

    @Test fun stoppedMixRenderReleasesEveryDecoder() {
        val released = mutableListOf<String>()
        class Src(val name: String, val a: FloatArray) : MixRenderer.PcmSource {
            private val inner = StudioDsp.ArraySource(a)
            override fun read(out: FloatArray, offsetFrames: Int, maxFrames: Int) = inner.read(out, offsetFrames, maxFrames)
            override fun release() { released.add(name) }
        }
        val tracks = listOf(MixRenderer.MixTrack("a", transitionSeconds = 4f), MixRenderer.MixTrack("b"), MixRenderer.MixTrack("c"))
        val tone = gen(10.0) { i -> (sin(2 * PI * 220 * i / sr) * 0.5).toFloat().let { it to it } }
        var frames = 0L
        try {
            MixRenderer.render(tracks, { t -> Src(t.uri, tone) }, { _, n -> frames += n; if (frames > 12 * sr) throw IllegalStateException("stop") }, 1L) {}
        } catch (e: IllegalStateException) {}
        assertTrue("released $released", released.containsAll(listOf("a", "b")))
        assertEquals(released.size, released.toSet().size)  // each released once
    }
}
