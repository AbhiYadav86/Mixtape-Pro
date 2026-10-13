package com.example.audio

import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import java.util.UUID

data class EqPreset(
    val id: String,
    val name: String,
    val description: String,
    val bandLevels: List<Float>, // 10 bands in range [-1.0f .. 1.0f] (-12dB to +12dB)
    val preampGain: Float = 0f, // [-1.0f .. 1.0f] (-12dB to +12dB)
    val bassBoost: Float = 0f, // [0.0f .. 1.0f]
    val tubeWarmth: Float = 0f, // [0.0f .. 1.0f]
    val stereoCrossfeed: Float = 0f // [0.0f .. 1.0f]
)

data class ReverbPreset(
    val id: String,
    val name: String,
    val description: String,
    val presetCode: Short // Maps to PresetReverb PRESET_*
)

enum class ReplayGainMode(val label: String, val description: String) {
    OFF("OFF", "Direct bitstream with no loudness adjustment"),
    TRACK_GAIN("TRACK (-14 LUFS)", "Equal loudness matching across individual tracks"),
    ALBUM_GAIN("ALBUM DYNAMICS", "Preserves master album dynamic range transitions"),
    AUDIOPHILE_LIMITER("BIT-PERFECT LIMITER", "Zero-clipping analog soft-knee peak protection")
}

enum class FilterType(val label: String, val shortName: String, val description: String) {
    PEAK("Bell / Peak", "PEAK", "Standard parametric bell filter with variable Q and gain"),
    LOW_SHELF("Low Shelf", "L-SHELF", "Smooth bass shelf boost or attenuation"),
    HIGH_SHELF("High Shelf", "H-SHELF", "Silky treble air shelf boost or attenuation"),
    NOTCH("Surgical Notch", "NOTCH", "Ultra-narrow Q filter to eliminate resonances or hum"),
    LOW_CUT("Low Cut (HPF)", "LOW-CUT", "High-pass filter removing sub-rumble below cutoff"),
    HIGH_CUT("High Cut (LPF)", "HIGH-CUT", "Low-pass filter removing ultrasonic frequencies above cutoff")
}

data class ParametricBand(
    val id: Int,
    val name: String,
    val frequencyHz: Float,
    val gainDb: Float,
    val q: Float,
    val filterType: FilterType = FilterType.PEAK,
    val isEnabled: Boolean = true,
    val colorHex: Long = 0xFF00E5FF
) {
    fun calculateBandGainDb(freq: Float): Float {
        if (!isEnabled) return 0f
        val f = freq.coerceIn(10f, 24000f)
        val f0 = frequencyHz.coerceIn(20f, 20000f)
        val qVal = q.coerceIn(0.25f, 25.0f)

        return when (filterType) {
            FilterType.PEAK -> {
                val ratio = f / f0
                val detuning = ratio - (1f / ratio)
                gainDb / (1f + (qVal * qVal * detuning * detuning))
            }
            FilterType.NOTCH -> {
                val ratio = f / f0
                val detuning = ratio - (1f / ratio)
                -24f / (1f + (qVal * qVal * detuning * detuning))
            }
            FilterType.LOW_SHELF -> {
                val ratio = (f / f0).toDouble()
                val power = (2.0 * qVal.coerceIn(0.5f, 3.0f)).toDouble()
                val denominator = 1.0 + Math.pow(ratio, power)
                (gainDb / denominator).toFloat()
            }
            FilterType.HIGH_SHELF -> {
                val ratio = (f0 / f).toDouble()
                val power = (2.0 * qVal.coerceIn(0.5f, 3.0f)).toDouble()
                val denominator = 1.0 + Math.pow(ratio, power)
                (gainDb / denominator).toFloat()
            }
            FilterType.LOW_CUT -> {
                if (f < f0) {
                    val octavesBelow = (Math.log((f0 / f).toDouble()) / Math.log(2.0)).toFloat()
                    -(octavesBelow * 12f * qVal.coerceIn(0.5f, 2.0f)).coerceAtMost(36f)
                } else {
                    0f
                }
            }
            FilterType.HIGH_CUT -> {
                if (f > f0) {
                    val octavesAbove = (Math.log((f / f0).toDouble()) / Math.log(2.0)).toFloat()
                    -(octavesAbove * 12f * qVal.coerceIn(0.5f, 2.0f)).coerceAtMost(36f)
                } else {
                    0f
                }
            }
        }
    }

    /**
     * Calculates the equivalent bandwidth in octaves:
     * N = 2 * asinh(1 / (2*Q)) / ln(2)
     */
    fun getOctaveBandwidth(): Float {
        val qVal = q.coerceIn(0.25f, 30.0f).toDouble()
        val arg = 1.0 / (2.0 * qVal)
        val asinhVal = Math.log(arg + Math.sqrt(arg * arg + 1.0))
        return ((2.0 / Math.log(2.0)) * asinhVal).toFloat()
    }
}

data class ParametricPreset(
    val id: String,
    val name: String,
    val description: String,
    val bands: List<ParametricBand>,
    val preampGainDb: Float = 0f
)

enum class EqualizerMode(val label: String, val subtitle: String) {
    GRAPHIC_10_BAND("10-Band Graphic EQ", "Classic Fixed Frequencies"),
    SURGICAL_PARAMETRIC("Surgical Parametric (PEQ)", "Continuous Freq & Variable Q")
}

object ParametricPresets {
    val FLAT_REFERENCE = ParametricPreset(
        id = "peq_flat",
        name = "Flat Direct (Reference)",
        description = "Uncolored bit-perfect reference curve",
        bands = listOf(
            ParametricBand(1, "Sub", 35f, 0f, 0.7f, FilterType.LOW_SHELF, true, 0xFFEF4444),
            ParametricBand(2, "Bass", 120f, 0f, 1.0f, FilterType.PEAK, true, 0xFFF97316),
            ParametricBand(3, "Low-Mid", 500f, 0f, 1.2f, FilterType.PEAK, true, 0xFFFACC15),
            ParametricBand(4, "High-Mid", 2500f, 0f, 1.5f, FilterType.PEAK, true, 0xFF22C55E),
            ParametricBand(5, "Presence", 6000f, 0f, 2.0f, FilterType.PEAK, true, 0xFF06B6D4),
            ParametricBand(6, "Air", 14000f, 0f, 0.7f, FilterType.HIGH_SHELF, true, 0xFFA855F7)
        )
    )

    val HARMAN_AUDIOPHILE = ParametricPreset(
        id = "peq_harman",
        name = "Harman Target 2019v2",
        description = "Acoustically validated audiophile headphone target curve",
        bands = listOf(
            ParametricBand(1, "Sub Excursion", 45f, 5.5f, 0.65f, FilterType.LOW_SHELF, true, 0xFFEF4444),
            ParametricBand(2, "Mid-Bass Tamer", 200f, -1.5f, 1.2f, FilterType.PEAK, true, 0xFFF97316),
            ParametricBand(3, "Vocal Clarity", 1000f, 1.0f, 1.0f, FilterType.PEAK, true, 0xFFFACC15),
            ParametricBand(4, "Ear Gain Peak", 3200f, 6.0f, 1.8f, FilterType.PEAK, true, 0xFF22C55E),
            ParametricBand(5, "Concha Notch", 6800f, -2.5f, 4.0f, FilterType.PEAK, true, 0xFF06B6D4),
            ParametricBand(6, "Air Treble Sparkle", 12500f, 3.0f, 0.75f, FilterType.HIGH_SHELF, true, 0xFFA855F7)
        ),
        preampGainDb = -4.0f
    )

    val SENNHEISER_HD650 = ParametricPreset(
        id = "peq_hd650",
        name = "Sennheiser HD600/650 Target",
        description = "Sub-bass extension with 3.5kHz smoothing for open-back dynamic drivers",
        bands = listOf(
            ParametricBand(1, "Sub Lift", 35f, 6.5f, 0.7f, FilterType.LOW_SHELF, true, 0xFFEF4444),
            ParametricBand(2, "Punch", 105f, 1.5f, 1.4f, FilterType.PEAK, true, 0xFFF97316),
            ParametricBand(3, "Mid Clean", 300f, -1.2f, 1.0f, FilterType.PEAK, true, 0xFFFACC15),
            ParametricBand(4, "Upper-Mid Smooth", 3400f, -2.0f, 2.5f, FilterType.PEAK, true, 0xFF22C55E),
            ParametricBand(5, "Treble Presence", 7500f, 1.5f, 3.0f, FilterType.PEAK, true, 0xFF06B6D4),
            ParametricBand(6, "Air Extension", 15000f, 3.5f, 0.6f, FilterType.HIGH_SHELF, true, 0xFFA855F7)
        ),
        preampGainDb = -4.5f
    )

    val SURGICAL_NOTCH_RESONANCE = ParametricPreset(
        id = "peq_surgical_notch",
        name = "Surgical Room & Hum Notch",
        description = "Ultra-narrow Q=8-15 cuts for 60Hz ground hum & room resonance peaks",
        bands = listOf(
            ParametricBand(1, "60Hz Ground Hum", 60f, -15.0f, 14.0f, FilterType.NOTCH, true, 0xFFEF4444),
            ParametricBand(2, "Room Mode 125Hz", 125f, -6.5f, 8.0f, FilterType.NOTCH, true, 0xFFF97316),
            ParametricBand(3, "Mud Resonance", 420f, -3.5f, 6.0f, FilterType.PEAK, true, 0xFFFACC15),
            ParametricBand(4, "Nasal 1.8kHz", 1850f, -4.0f, 7.5f, FilterType.PEAK, true, 0xFF22C55E),
            ParametricBand(5, "Harsh Sibilance", 6200f, -5.5f, 9.0f, FilterType.NOTCH, true, 0xFF06B6D4),
            ParametricBand(6, "Air Shelf", 13000f, 1.5f, 0.8f, FilterType.HIGH_SHELF, true, 0xFFA855F7)
        )
    )

    val VOCAL_DE_ESSER = ParametricPreset(
        id = "peq_de_esser",
        name = "Surgical De-Esser (Vocal)",
        description = "Pinpoint surgical notch at 6.4kHz (Q=11) to eliminate harsh sibilance",
        bands = listOf(
            ParametricBand(1, "Sub Rumble Cut", 30f, 0f, 1.0f, FilterType.LOW_CUT, true, 0xFFEF4444),
            ParametricBand(2, "Chest Warmth", 220f, 1.5f, 1.2f, FilterType.PEAK, true, 0xFFF97316),
            ParametricBand(3, "Vocal Body", 900f, 2.0f, 1.4f, FilterType.PEAK, true, 0xFFFACC15),
            ParametricBand(4, "Intelligibility", 3500f, 2.5f, 2.0f, FilterType.PEAK, true, 0xFF22C55E),
            ParametricBand(5, "Surgical De-Ess", 6400f, -7.5f, 11.0f, FilterType.NOTCH, true, 0xFF06B6D4),
            ParametricBand(6, "Air Sparkle", 14000f, 2.5f, 0.7f, FilterType.HIGH_SHELF, true, 0xFFA855F7)
        )
    )

    val MASTERING_AIR_LIFT = ParametricPreset(
        id = "peq_air_lift",
        name = "Audiophile Mastering Air Lift",
        description = "Boutique high-shelf air bloom with tight sub-bass foundation",
        bands = listOf(
            ParametricBand(1, "Sub Foundation", 40f, 2.5f, 0.7f, FilterType.LOW_SHELF, true, 0xFFEF4444),
            ParametricBand(2, "Tight Kick", 90f, 1.0f, 1.8f, FilterType.PEAK, true, 0xFFF97316),
            ParametricBand(3, "Boxiness Cut", 350f, -1.8f, 2.2f, FilterType.PEAK, true, 0xFFFACC15),
            ParametricBand(4, "Presence Sweetener", 4200f, 1.5f, 1.5f, FilterType.PEAK, true, 0xFF22C55E),
            ParametricBand(5, "Smooth Treble", 8500f, 1.0f, 1.2f, FilterType.PEAK, true, 0xFF06B6D4),
            ParametricBand(6, "Silky Air 16k", 16000f, 4.5f, 0.55f, FilterType.HIGH_SHELF, true, 0xFFA855F7)
        ),
        preampGainDb = -2.0f
    )

    val ALL_PRESETS = listOf(
        FLAT_REFERENCE,
        HARMAN_AUDIOPHILE,
        SENNHEISER_HD650,
        SURGICAL_NOTCH_RESONANCE,
        VOCAL_DE_ESSER,
        MASTERING_AIR_LIFT
    )
}

data class AudiophileTelemetry(
    val format: String = "FLAC 24-bit Hi-Res",
    val sampleRateKhz: Double = 96.0,
    val bitDepth: Int = 24,
    val bitrateKbps: Int = 1411,
    val isHiRes: Boolean = true,
    val dspLatencyMs: Double = 1.2,
    val dynamicRangeDb: Double = 114.0
)

object SpatialReverbPresets {
    val NONE = ReverbPreset("none", "Studio Direct (Bypass)", "Pure dry acoustic signal with zero artificial reflections", PresetReverb.PRESET_NONE)
    val LIVING_ROOM = ReverbPreset("living_room", "90s Hi-Fi Listening Room", "Warm acoustic wood & fabric reflections", PresetReverb.PRESET_SMALLROOM)
    val MEDIUM_ROOM = ReverbPreset("medium_room", "Acoustic Control Room", "Studio baffle dampening and focused stereo imaging", PresetReverb.PRESET_MEDIUMROOM)
    val CONCERT_HALL = ReverbPreset("concert_hall", "Philharmonic Arena", "Expansive 3D orchestral soundstage depth", PresetReverb.PRESET_LARGEHALL)
    val CATHEDRAL = ReverbPreset("cathedral", "Cathedral Acoustics", "Long ambient decay with harmonic richness", PresetReverb.PRESET_LARGEROOM)
    val SUBWAY_PLATE = ReverbPreset("plate", "Vintage EMT Plate Reverb", "Warm metallic analog vintage plate decay", PresetReverb.PRESET_PLATE)

    val ALL_REVERBS = listOf(
        NONE,
        LIVING_ROOM,
        MEDIUM_ROOM,
        CONCERT_HALL,
        CATHEDRAL,
        SUBWAY_PLATE
    )
}

object EqualizerPresets {
    val FLAT = EqPreset(
        id = "flat",
        name = "Flat (Audiophile Direct)",
        description = "Uncolored bit-perfect reference curve",
        bandLevels = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        preampGain = 0f,
        bassBoost = 0f,
        tubeWarmth = 0f,
        stereoCrossfeed = 0.2f
    )

    val HARMAN_TARGET = EqPreset(
        id = "harman_target",
        name = "Harman IEM Target '99",
        description = "Scientifically tuned audiophile in-ear reference curve",
        bandLevels = listOf(0.6f, 0.45f, 0.2f, 0.0f, -0.1f, 0.1f, 0.4f, 0.6f, 0.35f, 0.2f),
        preampGain = -0.15f,
        bassBoost = 0.3f,
        tubeWarmth = 0.15f,
        stereoCrossfeed = 0.45f
    )

    val ANALOG_TUBE_VINYL = EqPreset(
        id = "vinyl_warmth",
        name = "Analog Tube & Vinyl Warmth",
        description = "Rich 2nd-order harmonics with soft treble roll-off",
        bandLevels = listOf(0.4f, 0.5f, 0.4f, 0.3f, 0.25f, 0.1f, 0.0f, -0.1f, -0.25f, -0.4f),
        preampGain = 0.0f,
        bassBoost = 0.4f,
        tubeWarmth = 0.85f,
        stereoCrossfeed = 0.5f
    )

    val MEGA_BASS = EqPreset(
        id = "bass_boost",
        name = "Mega Bass Sub-Extender '99",
        description = "Deep 31Hz/62Hz excursion with tight punchy sub bass",
        bandLevels = listOf(0.9f, 0.75f, 0.5f, 0.2f, 0.0f, -0.1f, 0.0f, 0.1f, 0.2f, 0.3f),
        preampGain = -0.2f,
        bassBoost = 0.85f,
        tubeWarmth = 0.3f,
        stereoCrossfeed = 0.1f
    )

    val EURODANCE_90S = EqPreset(
        id = "eurodance",
        name = "Eurodance & Trance Club",
        description = "Tight punchy transient kicks with sparkling air synths",
        bandLevels = listOf(0.7f, 0.6f, 0.3f, 0.0f, 0.1f, 0.3f, 0.5f, 0.7f, 0.8f, 0.85f),
        preampGain = -0.1f,
        bassBoost = 0.6f,
        tubeWarmth = 0.2f,
        stereoCrossfeed = 0.35f
    )

    val GRUNGE_ROCK = EqPreset(
        id = "grunge_rock",
        name = "Grunge & Heavy Guitar Alt-Rock",
        description = "Thick distorted 500Hz-2kHz crunch with solid low-end body",
        bandLevels = listOf(0.4f, 0.5f, 0.6f, 0.5f, 0.4f, 0.3f, 0.5f, 0.4f, 0.3f, 0.2f),
        preampGain = 0.0f,
        bassBoost = 0.35f,
        tubeWarmth = 0.5f,
        stereoCrossfeed = 0.25f
    )

    val GOLDEN_HIPHOP = EqPreset(
        id = "hip_hop",
        name = "Golden Era 90s Hip-Hop",
        description = "Deep 808 sub bass foundation with crisp vocal intelligibility",
        bandLevels = listOf(0.85f, 0.7f, 0.4f, 0.0f, -0.2f, 0.1f, 0.3f, 0.4f, 0.5f, 0.4f),
        preampGain = -0.15f,
        bassBoost = 0.7f,
        tubeWarmth = 0.4f,
        stereoCrossfeed = 0.2f
    )

    val VOCAL_ACOUSTIC = EqPreset(
        id = "vocal_acoustic",
        name = "Vocal Intimacy & Acoustic Jazz",
        description = "Sweet, detailed midrange vocal projection with wide acoustic staging",
        bandLevels = listOf(-0.2f, 0.0f, 0.2f, 0.4f, 0.6f, 0.7f, 0.55f, 0.4f, 0.3f, 0.35f),
        preampGain = 0.0f,
        bassBoost = 0.1f,
        tubeWarmth = 0.4f,
        stereoCrossfeed = 0.6f
    )

    val CLASSICAL_SYMPHONY = EqPreset(
        id = "classical",
        name = "Classical & Symphony Hall",
        description = "Expansive acoustic headroom with balanced dynamic staging",
        bandLevels = listOf(0.2f, 0.25f, 0.1f, 0.0f, 0.0f, 0.1f, 0.2f, 0.35f, 0.5f, 0.6f),
        preampGain = 0.1f,
        bassBoost = 0.15f,
        tubeWarmth = 0.2f,
        stereoCrossfeed = 0.75f
    )

    val LOUDNESS_CONTOUR = EqPreset(
        id = "loudness",
        name = "Loudness Dynamic Contour",
        description = "Fletcher-Munson compensation for full richness at low volume",
        bandLevels = listOf(0.75f, 0.6f, 0.35f, 0.0f, -0.1f, -0.1f, 0.1f, 0.35f, 0.65f, 0.8f),
        preampGain = 0.0f,
        bassBoost = 0.5f,
        tubeWarmth = 0.3f,
        stereoCrossfeed = 0.3f
    )

    val ALL_PRESETS = listOf(
        FLAT,
        HARMAN_TARGET,
        ANALOG_TUBE_VINYL,
        MEGA_BASS,
        EURODANCE_90S,
        GRUNGE_ROCK,
        GOLDEN_HIPHOP,
        VOCAL_ACOUSTIC,
        CLASSICAL_SYMPHONY,
        LOUDNESS_CONTOUR
    )
}

class RetroEqualizerController {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var presetReverb: PresetReverb? = null

    private var isEqEnabled: Boolean = true
    private var isAbBypassActive: Boolean = false // A/B auditioning: when true, temporarily bypasses DSP for quick comparison
    private var eqMode: EqualizerMode = EqualizerMode.GRAPHIC_10_BAND

    // 10-Band levels in range [-1.0f .. 1.0f] (-12dB to +12dB)
    // 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
    private var customBands = mutableListOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
    private var preampGain = 0.0f // [-1.0f .. +1.0f] (-12dB .. +12dB)
    private var bassBoostStrength = 0.4f // [0.0f .. 1.0f]

    // Surgical Parametric EQ (PEQ) Engine
    private var parametricBands = ParametricPresets.HARMAN_AUDIOPHILE.bands.map { it.copy() }.toMutableList()
    private var parametricPreampGainDb = -3.0f
    private var currentParametricPreset: ParametricPreset = ParametricPresets.HARMAN_AUDIOPHILE

    // DSP Audiophile modules
    private var isTubeWarmthEnabled = true
    private var tubeWarmthDrive = 0.45f // [0.0f .. 1.0f]
    private var isStereoCrossfeedEnabled = true
    private var stereoCrossfeedWidth = 0.4f // [0.0f .. 1.0f]
    private var replayGainMode = ReplayGainMode.AUDIOPHILE_LIMITER

    private var currentPreset: EqPreset = EqualizerPresets.HARMAN_TARGET
    private var currentReverb: ReverbPreset = SpatialReverbPresets.NONE

    init {
        // Initialize with default preset
        applyPreset(EqualizerPresets.HARMAN_TARGET)
    }

    private fun isEffectSupported(type: UUID): Boolean {
        return try {
            val effects = AudioEffect.queryEffects() ?: return false
            effects.any { it.type == type }
        } catch (e: Throwable) {
            false
        }
    }

    fun attachToSession(sessionId: Int) {
        release()
        try {
            if (sessionId > 0) {
                if (isEffectSupported(AudioEffect.EFFECT_TYPE_EQUALIZER)) {
                    equalizer = Equalizer(0, sessionId).apply {
                        enabled = isEqEnabled && !isAbBypassActive
                    }
                }
                if (isEffectSupported(AudioEffect.EFFECT_TYPE_BASS_BOOST)) {
                    bassBoost = BassBoost(0, sessionId).apply {
                        enabled = isEqEnabled && !isAbBypassActive
                    }
                }
                if (isEffectSupported(AudioEffect.EFFECT_TYPE_PRESET_REVERB)) {
                    presetReverb = PresetReverb(0, sessionId).apply {
                        preset = currentReverb.presetCode
                        enabled = isEqEnabled && !isAbBypassActive && currentReverb.presetCode != PresetReverb.PRESET_NONE
                    }
                }
                applySettings()
            }
        } catch (e: Throwable) {
            // Audio FX fallback
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEqEnabled = enabled
        updateHardwareEnabled()
    }

    fun isEnabled(): Boolean = isEqEnabled

    fun setAbBypass(bypass: Boolean) {
        isAbBypassActive = bypass
        updateHardwareEnabled()
        applySettings()
    }

    fun isAbBypassActive(): Boolean = isAbBypassActive

    private fun updateHardwareEnabled() {
        val active = isEqEnabled && !isAbBypassActive
        try {
            equalizer?.enabled = active
            bassBoost?.enabled = active
            presetReverb?.enabled = active && currentReverb.presetCode != PresetReverb.PRESET_NONE
        } catch (ignored: Exception) {}
    }

    fun applyPreset(preset: EqPreset) {
        currentPreset = preset
        customBands = if (preset.bandLevels.size == 10) {
            preset.bandLevels.toMutableList()
        } else {
            // Expand or adapt if less
            List(10) { i -> preset.bandLevels.getOrElse(i / 2) { 0f } }.toMutableList()
        }
        preampGain = preset.preampGain
        bassBoostStrength = preset.bassBoost
        tubeWarmthDrive = preset.tubeWarmth
        stereoCrossfeedWidth = preset.stereoCrossfeed
        applySettings()
    }

    fun applyReverbPreset(reverb: ReverbPreset) {
        currentReverb = reverb
        try {
            presetReverb?.let { pr ->
                pr.preset = reverb.presetCode
                pr.enabled = isEqEnabled && !isAbBypassActive && reverb.presetCode != PresetReverb.PRESET_NONE
            }
        } catch (ignored: Exception) {}
    }

    fun setBandLevel(bandIndex: Int, levelNormalized: Float) {
        if (bandIndex in customBands.indices) {
            customBands[bandIndex] = levelNormalized.coerceIn(-1.0f, 1.0f)
            applySettings()
        }
    }

    fun setPreampGain(gainNormalized: Float) {
        preampGain = gainNormalized.coerceIn(-1.0f, 1.0f)
        applySettings()
    }

    fun getPreampGain(): Float = preampGain

    fun setBassBoost(level: Float) {
        bassBoostStrength = level.coerceIn(0f, 1f)
        try {
            val effStrength = if (isAbBypassActive || !isEqEnabled) 0 else (bassBoostStrength * 1000).toInt()
            bassBoost?.setStrength(effStrength.toShort())
        } catch (ignored: Exception) {}
    }

    fun getBassBoostLevel(): Float = bassBoostStrength

    // --- Tube Warmth Simulator ---
    fun setTubeWarmthEnabled(enabled: Boolean) {
        isTubeWarmthEnabled = enabled
        applySettings()
    }

    fun isTubeWarmthEnabled(): Boolean = isTubeWarmthEnabled

    fun setTubeWarmthDrive(drive: Float) {
        tubeWarmthDrive = drive.coerceIn(0.0f, 1.0f)
        applySettings()
    }

    fun getTubeWarmthDrive(): Float = tubeWarmthDrive

    // --- Stereo Crossfeed & Soundstage ---
    fun setStereoCrossfeedEnabled(enabled: Boolean) {
        isStereoCrossfeedEnabled = enabled
        applySettings()
    }

    fun isStereoCrossfeedEnabled(): Boolean = isStereoCrossfeedEnabled

    fun setStereoCrossfeedWidth(width: Float) {
        stereoCrossfeedWidth = width.coerceIn(0.0f, 1.0f)
        applySettings()
    }

    fun getStereoCrossfeedWidth(): Float = stereoCrossfeedWidth

    // --- ReplayGain Mode ---
    fun setReplayGainMode(mode: ReplayGainMode) {
        replayGainMode = mode
    }

    fun getReplayGainMode(): ReplayGainMode = replayGainMode

    fun getCurrentBands(): List<Float> = customBands.toList()
    fun getCurrentPreset(): EqPreset = currentPreset
    fun getCurrentReverbPreset(): ReverbPreset = currentReverb

    // --- Equalizer Mode (Graphic vs Surgical Parametric) ---
    fun getEqualizerMode(): EqualizerMode = eqMode

    fun setEqualizerMode(mode: EqualizerMode) {
        eqMode = mode
        applySettings()
    }

    // --- Surgical Parametric EQ (PEQ) Controls ---
    fun getParametricBands(): List<ParametricBand> = parametricBands.map { it.copy() }

    fun getCurrentParametricPreset(): ParametricPreset = currentParametricPreset

    fun getParametricPreampGainDb(): Float = parametricPreampGainDb

    fun setParametricPreampGainDb(gainDb: Float) {
        parametricPreampGainDb = gainDb.coerceIn(-15.0f, 15.0f)
        applySettings()
    }

    fun applyParametricPreset(preset: ParametricPreset) {
        currentParametricPreset = preset
        parametricBands = preset.bands.map { it.copy() }.toMutableList()
        parametricPreampGainDb = preset.preampGainDb
        applySettings()
    }

    fun updateParametricBand(band: ParametricBand) {
        val index = parametricBands.indexOfFirst { it.id == band.id }
        if (index >= 0) {
            parametricBands[index] = band.copy(
                frequencyHz = band.frequencyHz.coerceIn(20f, 20000f),
                gainDb = band.gainDb.coerceIn(-18f, 18f),
                q = band.q.coerceIn(0.25f, 25f)
            )
            applySettings()
        }
    }

    fun setParametricBandFreq(bandId: Int, freq: Float) {
        val index = parametricBands.indexOfFirst { it.id == bandId }
        if (index >= 0) {
            parametricBands[index] = parametricBands[index].copy(frequencyHz = freq.coerceIn(20f, 20000f))
            applySettings()
        }
    }

    fun setParametricBandGain(bandId: Int, gainDb: Float) {
        val index = parametricBands.indexOfFirst { it.id == bandId }
        if (index >= 0) {
            parametricBands[index] = parametricBands[index].copy(gainDb = gainDb.coerceIn(-18f, 18f))
            applySettings()
        }
    }

    fun setParametricBandQ(bandId: Int, q: Float) {
        val index = parametricBands.indexOfFirst { it.id == bandId }
        if (index >= 0) {
            parametricBands[index] = parametricBands[index].copy(q = q.coerceIn(0.25f, 25f))
            applySettings()
        }
    }

    fun setParametricBandType(bandId: Int, type: FilterType) {
        val index = parametricBands.indexOfFirst { it.id == bandId }
        if (index >= 0) {
            parametricBands[index] = parametricBands[index].copy(filterType = type)
            applySettings()
        }
    }

    fun setParametricBandEnabled(bandId: Int, isEnabled: Boolean) {
        val index = parametricBands.indexOfFirst { it.id == bandId }
        if (index >= 0) {
            parametricBands[index] = parametricBands[index].copy(isEnabled = isEnabled)
            applySettings()
        }
    }

    /**
     * Computes the composite frequency response in dB at any arbitrary continuous frequency.
     * Incorporates the summation of all enabled parametric filter bands plus preamp headroom gain.
     */
    fun getCompositeParametricGainDb(frequencyHz: Float): Float {
        var totalDb = parametricPreampGainDb
        for (band in parametricBands) {
            totalDb += band.calculateBandGainDb(frequencyHz)
        }
        return totalDb
    }

    private fun applySettings() {
        try {
            equalizer?.let { eq ->
                val numHardwareBands = eq.numberOfBands.toInt()
                val minRange = eq.bandLevelRange[0]
                val maxRange = eq.bandLevelRange[1]
                val range = maxRange - minRange

                if (isAbBypassActive || !isEqEnabled) {
                    // Set all flat 0dB
                    for (i in 0 until numHardwareBands) {
                        val flatLevel = (minRange + 0.5f * range).toInt().toShort()
                        eq.setBandLevel(i.toShort(), flatLevel)
                    }
                } else if (eqMode == EqualizerMode.SURGICAL_PARAMETRIC) {
                    // Sample continuous parametric curve at hardware band center frequencies
                    for (i in 0 until numHardwareBands) {
                        val centerFreqHz = try {
                            val cf = eq.getCenterFreq(i.toShort()) / 1000f
                            if (cf > 10f) cf else 1000f
                        } catch (e: Throwable) {
                            val fNorm = if (numHardwareBands > 1) i.toFloat() / (numHardwareBands - 1) else 0.5f
                            (20.0 * Math.pow(1000.0, fNorm.toDouble())).toFloat()
                        }

                        // Evaluate continuous parametric transfer function in dB
                        val responseDb = getCompositeParametricGainDb(centerFreqHz)
                        // Normalized factor in [-1.0 .. 1.0] where 1.0 corresponds to +12dB
                        var bandFactor = (responseDb / 12f).coerceIn(-1.0f, 1.0f)

                        // Analog tube warmth coloration
                        if (isTubeWarmthEnabled && tubeWarmthDrive > 0f) {
                            if (centerFreqHz < 500f) {
                                bandFactor += tubeWarmthDrive * 0.18f
                            } else if (centerFreqHz > 8000f) {
                                bandFactor -= tubeWarmthDrive * 0.08f
                            }
                        }

                        bandFactor = bandFactor.coerceIn(-1.0f, 1.0f)
                        val level = (minRange + (bandFactor + 1f) * 0.5f * range).toInt().toShort()
                        eq.setBandLevel(i.toShort(), level)
                    }
                } else {
                    // Map 10 custom graphic bands to the device's hardware bands + preamp gain factor
                    for (i in 0 until numHardwareBands) {
                        val mapped10Index = if (numHardwareBands > 1) {
                            ((i.toFloat() / (numHardwareBands - 1)) * (customBands.size - 1)).toInt()
                                .coerceIn(0, customBands.size - 1)
                        } else {
                            0
                        }

                        var bandFactor = customBands[mapped10Index]

                        // Apply Tube Warmth analog boost in low-mids and soft-clipping if active
                        if (isTubeWarmthEnabled && tubeWarmthDrive > 0f) {
                            if (mapped10Index in 0..4) {
                                bandFactor += tubeWarmthDrive * 0.18f // Warm harmonic lift
                            } else if (mapped10Index >= 8) {
                                bandFactor -= tubeWarmthDrive * 0.08f // Soft analog tape roll-off
                            }
                        }

                        // Apply Preamp Gain offset
                        bandFactor = (bandFactor + (preampGain * 0.5f)).coerceIn(-1.0f, 1.0f)

                        val level = (minRange + (bandFactor + 1f) * 0.5f * range).toInt().toShort()
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
            }

            val effBass = if (isAbBypassActive || !isEqEnabled) 0 else {
                var b = bassBoostStrength
                if (isTubeWarmthEnabled) b = (b + tubeWarmthDrive * 0.15f).coerceIn(0f, 1f)
                (b * 1000).toInt()
            }
            bassBoost?.setStrength(effBass.toShort())

            presetReverb?.let { pr ->
                if (isAbBypassActive || !isEqEnabled || currentReverb.presetCode == PresetReverb.PRESET_NONE) {
                    pr.enabled = false
                } else {
                    pr.preset = currentReverb.presetCode
                    pr.enabled = true
                }
            }
        } catch (ignored: Exception) {}
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            presetReverb?.release()
        } catch (ignored: Exception) {}
        equalizer = null
        bassBoost = null
        presetReverb = null
    }
}
