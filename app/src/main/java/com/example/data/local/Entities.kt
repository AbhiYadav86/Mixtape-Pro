package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AudioSource
import com.example.data.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val albumArtUri: String?,
    val albumArtRes: Int?,
    val source: String,
    val genre: String,
    val year: String,
    val bitrateKbps: Int,
    val sampleRateKhz: Int,
    val isFavorite: Boolean,
    val folderPath: String,
    val addedTimestamp: Long,
    val playCount: Int = 0,
    val lyrics: String? = null
) {
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        uri = uri,
        albumArtUri = albumArtUri,
        albumArtRes = albumArtRes,
        source = runCatching { AudioSource.valueOf(source) }.getOrDefault(AudioSource.LOCAL),
        genre = genre,
        year = year,
        bitrateKbps = bitrateKbps,
        sampleRateKhz = sampleRateKhz,
        isFavorite = isFavorite,
        folderPath = folderPath,
        addedTimestamp = addedTimestamp,
        playCount = playCount,
        lyrics = lyrics
    )

    companion object {
        fun fromSong(song: Song): SongEntity = SongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.durationMs,
            uri = song.uri,
            albumArtUri = song.albumArtUri,
            albumArtRes = song.albumArtRes,
            source = song.source.name,
            genre = song.genre,
            year = song.year,
            bitrateKbps = song.bitrateKbps,
            sampleRateKhz = song.sampleRateKhz,
            isFavorite = song.isFavorite,
            folderPath = song.folderPath,
            addedTimestamp = song.addedTimestamp,
            playCount = song.playCount,
            lyrics = song.lyrics
        )
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val coverRes: Int?,
    val createdAt: Long,
    val iconName: String
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: String,
    val orderIndex: Int
)

@Entity(tableName = "app_preferences")
data class AppPreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)
