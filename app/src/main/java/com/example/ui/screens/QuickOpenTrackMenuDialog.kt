package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.RetroSoundEffects
import com.example.data.model.Playlist
import com.example.data.model.RetroSkin
import com.example.data.model.Song

@Composable
fun QuickOpenTrackMenuDialog(
    allSongs: List<Song>,
    allPlaylists: List<Playlist>,
    currentSong: Song?,
    isPlaying: Boolean,
    skin: RetroSkin,
    onSongSelected: (Song) -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onEnrichMetadata: ((Song) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Library Tracks, 1: Mixtapes / Playlists
    var libraryGrouping by remember { mutableIntStateOf(0) } // 0: All Tracks, 1: Artist, 2: Genre, 3: Album
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlaylistForDetail by remember { mutableStateOf<Playlist?>(null) }

    // Filter library tracks according to search and grouping
    val filteredSongs = remember(allSongs, searchQuery, libraryGrouping) {
        val searched = if (searchQuery.isBlank()) {
            allSongs
        } else {
            allSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true) ||
                it.genre.contains(searchQuery, ignoreCase = true)
            }
        }

        when (libraryGrouping) {
            1 -> searched.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            2 -> searched.sortedWith(compareBy({ it.genre.lowercase() }, { it.title.lowercase() }))
            3 -> searched.sortedWith(compareBy({ it.album.lowercase() }, { it.title.lowercase() }))
            else -> searched
        }
    }

    // Filter playlists according to search
    val filteredPlaylists = remember(allPlaylists, searchQuery) {
        if (searchQuery.isBlank()) {
            allPlaylists
        } else {
            allPlaylists.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("quick_open_track_menu_dialog")
                .border(2.dp, skin.accentColor, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131720))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(skin.chassisBackground)
                    .padding(14.dp)
            ) {
                // Header Bar with Eject / Open Icon & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(skin.accentColor.copy(alpha = 0.2f))
                                .border(1.dp, skin.accentColor, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Open Tracks",
                                tint = skin.accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OPEN AUDIO SOURCE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = skin.lcdTextColor
                            )
                            Text(
                                text = "Select tracks from Library or Mixtapes",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                            onDismiss()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Menu",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section Tabs: LIBRARY vs. MIXTAPES / PLAYLISTS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        0 to "🎵 LIBRARY TRACKS (${allSongs.size})",
                        1 to "📼 MIXTAPES & PLAYLISTS (${allPlaylists.size})"
                    ).forEach { (sectionIdx, title) ->
                        val isSelected = selectedSection == sectionIdx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) skin.accentColor else Color(0xFF1E222D))
                                .border(1.dp, if (isSelected) Color.White else skin.chassisBorderColor, RoundedCornerShape(8.dp))
                                .clickable {
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                    selectedSection = sectionIdx
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = if (isSelected) Color.Black else Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar (Available in both Library and Mixtapes section)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (selectedSection == 0) "Search tracks, artists, genres, albums..." else "Search mixtapes, playlists...",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = skin.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_open_search_bar"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = skin.accentColor,
                        unfocusedBorderColor = skin.chassisBorderColor,
                        focusedContainerColor = Color(0xAA181A20),
                        unfocusedContainerColor = Color(0xAA181A20),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // If in Library section: Grouping Filter Options (All Tracks | Artist | Genre | Album)
                if (selectedSection == 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            0 to "All Tracks",
                            1 to "Artist",
                            2 to "Genre",
                            3 to "Album"
                        ).forEach { (idx, label) ->
                            val isSelected = libraryGrouping == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) skin.accentColor.copy(alpha = 0.35f)
                                        else Color(0xFF1B202B)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) skin.accentColor else Color(0x44FFFFFF),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                        libraryGrouping = idx
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.SansSerif,
                                    color = if (isSelected) skin.accentColor else Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Section Content List
                if (selectedSection == 0) {
                    // Library Track List
                    if (filteredSongs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tracks found matching \"$searchQuery\"",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = Color(0xFF64748B)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("quick_open_songs_list"),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredSongs, key = { it.id }) { song ->
                                val isCurrent = currentSong?.id == song.id
                                QuickSongRow(
                                    song = song,
                                    isCurrent = isCurrent,
                                    isPlaying = isPlaying,
                                    skin = skin,
                                    groupingMode = libraryGrouping,
                                    onEnrich = if (onEnrichMetadata != null) { { onEnrichMetadata(song) } } else null,
                                    onClick = {
                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                        onSongSelected(song)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Mixtapes / Playlists Section
                    if (filteredPlaylists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No mixtapes or playlists found matching \"$searchQuery\"",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = Color(0xFF64748B)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("quick_open_playlists_list"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredPlaylists, key = { it.id }) { playlist ->
                                QuickPlaylistRow(
                                    playlist = playlist,
                                    skin = skin,
                                    onPlayPlaylist = {
                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                                        onPlaylistSelected(playlist)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    skin: RetroSkin,
    groupingMode: Int,
    onEnrich: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isCurrent) 2.dp else 1.dp,
                color = if (isCurrent) skin.accentColor else Color(0x33FFFFFF),
                shape = RoundedCornerShape(6.dp)
            ),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFF1E2638) else Color(0xFF141720)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isCurrent) skin.accentColor else Color(0xFF262B38)),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent && isPlaying) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = if (isCurrent) Color.Black else skin.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isCurrent) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = if (isCurrent) skin.accentColor else Color.White,
                    maxLines = 1
                )
                val subtitle = when (groupingMode) {
                    1 -> "👤 ${song.artist} • ${song.album}"
                    2 -> "🏷️ [${song.genre}] • ${song.artist}"
                    3 -> "💿 ${song.album} • ${song.artist}"
                    else -> "${song.artist} • ${song.genre}"
                }
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )

                // Metadata status badges (Lyrics / Album Art)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (!song.lyrics.isNullOrBlank()) {
                        Text(
                            text = "♫ LRC",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF10B981)
                        )
                    }
                    val hasArt = !song.albumArtUri.isNullOrBlank() && !song.albumArtUri.contains("unsplash")
                    if (hasArt) {
                        Text(
                            text = " ART",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    if (onEnrich != null && (song.lyrics.isNullOrBlank() || !hasArt)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0x33F59E0B))
                                .border(0.5.dp, Color(0xFFF59E0B), RoundedCornerShape(3.dp))
                                .clickable { onEnrich() }
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "⚡ FETCH",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFFBBF24)
                            )
                        }
                    }
                }
            }

            Text(
                text = String.format("%02d:%02d", (song.durationMs / 1000) / 60, (song.durationMs / 1000) % 60),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = skin.lcdTextColor
            )
        }
    }
}

@Composable
private fun QuickPlaylistRow(
    playlist: Playlist,
    skin: RetroSkin,
    onPlayPlaylist: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayPlaylist() }
            .border(1.dp, skin.chassisBorderColor, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B24))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF232B3A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "Mixtape",
                        tint = skin.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = playlist.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.White
                    )
                    Text(
                        text = if (playlist.description.isNotBlank()) playlist.description else "${playlist.songCount} Tracks",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFF94A3B8),
                        maxLines = 1
                    )
                }
            }

            Button(
                onClick = onPlayPlaylist,
                colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play Mixtape", tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("PLAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}
