package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Songs
    @Query("SELECT * FROM songs ORDER BY addedTimestamp DESC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET isFavorite = :isFav WHERE id = :songId")
    suspend fun setFavorite(songId: String, isFav: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: String)

    @Query("UPDATE songs SET title = :title, artist = :artist, album = :album, year = :year, genre = :genre WHERE id = :songId")
    suspend fun updateSongMetadata(songId: String, title: String, artist: String, album: String, year: String, genre: String)

    @Query("UPDATE songs SET lyrics = :lyrics WHERE id = :songId")
    suspend fun updateSongLyrics(songId: String, lyrics: String)

    @Query("UPDATE songs SET albumArtUri = :albumArtUri WHERE id = :songId")
    suspend fun updateSongAlbumArt(songId: String, albumArtUri: String)

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSong(songId: String)

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsDirect(): List<SongEntity>

    @Query("DELETE FROM songs WHERE id IN (:songIds)")
    suspend fun deleteSongs(songIds: List<String>)

    @Query("DELETE FROM playlist_songs WHERE songId IN (:songIds)")
    suspend fun removeSongsFromAllPlaylists(songIds: List<String>)

    @Query("UPDATE songs SET albumArtUri = NULL WHERE albumArtUri LIKE '%images.unsplash.com%' AND (id LIKE 'local_%' OR id LIKE 'picked_%')")
    suspend fun clearStockPhotoArtOnLocalSongs()

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getPlaylistSongCount(playlistId: String): Int

    @Query("DELETE FROM songs WHERE source = :source")
    suspend fun deleteSongsBySource(source: String)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun getAllPlaylistsDirect(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    // Playlist songs
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.orderIndex ASC
    """)
    fun getSongsForPlaylist(playlistId: String): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: String)

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    // App Preferences
    @Query("SELECT value FROM app_preferences WHERE `key` = :key")
    suspend fun getPreference(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setPreference(pref: AppPreferenceEntity)
}
