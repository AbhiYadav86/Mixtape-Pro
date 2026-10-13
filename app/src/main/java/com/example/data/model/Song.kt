package com.example.data.model

import java.io.Serializable

enum class AudioSource {
    LOCAL,
    DEMO_JUKEBOX,
    RADIO_STREAM,
    FREE_ARCHIVE,
    GOOGLE_DRIVE,
    CLOUD_STORAGE
}

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Flashback Mixtape",
    val durationMs: Long = 0L,
    val uri: String,
    val albumArtUri: String? = null,
    val albumArtRes: Int? = null,
    val source: AudioSource = AudioSource.LOCAL,
    val genre: String = "90s / 2000s",
    val year: String = "1999",
    val bitrateKbps: Int = 128,
    val sampleRateKhz: Int = 44,
    val isFavorite: Boolean = false,
    val folderPath: String = "Default",
    val addedTimestamp: Long = System.currentTimeMillis(),
    val playCount: Int = 0,
    val lyrics: String? = null
) : Serializable

data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val coverRes: Int? = null,
    val songCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val iconName: String = "cassette"
)

enum class VisualizerType {
    CLASSIC_BARS_PEAK,
    ANALOG_VU_METERS,
    OSCILLOSCOPE_CRT,
    CIRCULAR_VORTEX,
    MATRIX_RAIN,
    LYRICS_TELEPROMPTER
}

enum class DiscDisplayMode {
    CD_JEWEL_CASE,
    CASSETTE_TAPE,
    VINYL_RECORD,
    CRT_MONITOR
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class StationLiveState {
    LIVE,
    CHECKING,
    OFFLINE,
    ARCHIVE
}

data class RadioStationStatus(
    val stationId: String,
    val state: StationLiveState = StationLiveState.CHECKING,
    val pingMs: Long = 0L,
    val signalPercent: Int = 95,
    val listenerCount: Int = 1200,
    val frequencyMhz: String = "98.5 FM",
    val bitrateStr: String = "128 kbps MP3",
    val isLiveStream: Boolean = true,
    val lastChecked: Long = System.currentTimeMillis()
)
