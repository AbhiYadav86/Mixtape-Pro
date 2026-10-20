package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RetroSoundEffects
import com.example.data.model.Playlist
import com.example.data.model.RetroSkin
import com.example.data.model.Song
import com.example.ui.components.SongArtThumbnail

@Composable
fun LibraryScreen(
    songs: List<Song>,
    playlists: List<Playlist>,
    currentSong: Song?,
    isPlaying: Boolean,
    skin: RetroSkin,
    onSongSelected: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onSyncLocalFolders: () -> Unit,
    onImportLocalUris: (List<Uri>) -> Unit = {},
    onAddSongToPlaylist: (playlistId: String, song: Song) -> Unit,
    enrichmentProgress: Pair<Int, Int>? = null,
    isShuffle: Boolean = false,
    onShuffleAll: (List<Song>) -> Unit = {},
    onRemoveFromLibrary: (List<Song>) -> Unit = {},
    onDeletedFromDevice: (List<Song>) -> Unit = {},
    onRestoreRemoved: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All Tracks, 1: Artist, 2: Genre, 3: Album, 4: Folders, 5: Favorites
    var songForPlaylistPicker by remember { mutableStateOf<Song?>(null) }

    // Selection & delete
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var songsToDelete by remember { mutableStateOf<List<Song>?>(null) }
    var showLibraryMenu by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val deleteFromDevice = rememberDeviceDeleter { deleted, failed ->
        val removedOnly = failed.filter { !canDeleteFromDevice(it) }
        if (deleted.isNotEmpty()) onDeletedFromDevice(deleted)
        if (removedOnly.isNotEmpty()) onRemoveFromLibrary(removedOnly)
        val notAllowed = failed.size - removedOnly.size
        if (notAllowed > 0) {
            android.widget.Toast.makeText(
                context,
                if (deleted.isEmpty()) "Nothing deleted — permission wasn't given" else "$notAllowed file(s) couldn't be deleted",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        selectionMode = false
        selectedIds.clear()
    }
    LaunchedEffect(songs) {
        // forget selections of songs that are gone
        val ids = songs.map { it.id }.toHashSet()
        selectedIds.removeAll { it !in ids }
    }

    // SAF Document Picker for local phone audio files
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty()) {
            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CD_SPIN)
            onImportLocalUris(uris)
        }
    }

    val filteredSongs = remember(songs, searchQuery, selectedTab) {
        val baseList = when (selectedTab) {
            1 -> songs.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            2 -> songs.sortedWith(compareBy({ it.genre.lowercase() }, { it.title.lowercase() }))
            3 -> songs.sortedWith(compareBy({ it.album.lowercase() }, { it.title.lowercase() }))
            4 -> songs.sortedBy { it.folderPath }
            5 -> songs.filter { it.isFavorite }
            else -> songs
        }
        if (searchQuery.isBlank()) baseList
        else baseList.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.album.contains(searchQuery, ignoreCase = true) ||
            it.genre.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(skin.chassisBackground)
            .padding(10.dp)
    ) {
        // Search bar & Import / Sync Buttons in Modern Typography
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search library…",
                        fontSize = 14.sp,
                        maxLines = 1,
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFF94A3B8)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = skin.accentColor
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("library_search_input"),
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

            // Pick Audio Files Button
            Button(
                onClick = {
                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                    documentPickerLauncher.launch(
                        arrayOf(
                            "audio/*",
                            "application/ogg",
                            "application/x-ogg"
                        )
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("pick_local_audio_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Files",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Modern Category Filter Tabs (All Tracks, Artist, Genre, Album, Folders, Favorites)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf("All", "Artist", "Genre", "Album", "Folders", "Favorites")
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) skin.accentColor else Color(0xFF1E222D))
                        .border(1.dp, if (isSelected) Color.White else skin.chassisBorderColor, RoundedCornerShape(6.dp))
                        .clickable {
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                            selectedTab = index
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
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

        // Shuffle / selection actions
        if (selectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2A1518))
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectionMode = false; selectedIds.clear() }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel selection", tint = Color.White)
                }
                Text("${selectedIds.size} selected", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val all = filteredSongs.map { it.id }
                    if (selectedIds.containsAll(all)) selectedIds.clear() else { selectedIds.clear(); selectedIds.addAll(all) }
                }) {
                    Text(if (filteredSongs.isNotEmpty() && selectedIds.containsAll(filteredSongs.map { it.id })) "None" else "All", color = skin.accentColor, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { songsToDelete = songs.filter { it.id in selectedIds } },
                    enabled = selectedIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                        onShuffleAll(filteredSongs)
                    },
                    enabled = filteredSongs.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isShuffle) skin.accentColor else Color(0xFF1E2430)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, skin.accentColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 46.dp)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, tint = if (isShuffle) Color.Black else skin.accentColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (selectedTab == 0 && searchQuery.isBlank()) "Shuffle all" else "Shuffle these",
                        color = if (isShuffle) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1
                    )
                }
                OutlinedButton(
                    onClick = { selectionMode = true },
                    enabled = songs.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, skin.chassisBorderColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.heightIn(min = 46.dp)
                ) {
                    Icon(Icons.Default.Checklist, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Box {
                    IconButton(onClick = { showLibraryMenu = true }, modifier = Modifier.size(46.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                    DropdownMenu(expanded = showLibraryMenu, onDismissRequest = { showLibraryMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Restore removed tracks") },
                            leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                            onClick = { showLibraryMenu = false; onRestoreRemoved() }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Track Count & Format Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (filteredSongs.size == songs.size) "${songs.size} TRACKS" else "${filteredSongs.size} OF ${songs.size} TRACKS",
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = skin.lcdTextColor
            )
            Text(
                text = enrichmentProgress?.let { (done, total) -> "FINDING ART & LYRICS $done/$total" } ?: "MP3 / FLAC / WAV / OGG",
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = skin.accentColor,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Song List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("songs_list"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredSongs, key = { it.id }) { song ->
                val isCurrent = currentSong?.id == song.id
                SongRowItem(
                    song = song,
                    isCurrent = isCurrent,
                    isPlaying = isPlaying,
                    skin = skin,
                    selectionMode = selectionMode,
                    isSelected = song.id in selectedIds,
                    onLongPress = {
                        if (!selectionMode) { selectionMode = true; selectedIds.clear() }
                        if (song.id !in selectedIds) selectedIds.add(song.id)
                    },
                    onDelete = { songsToDelete = listOf(song) },
                    onPlay = {
                        if (selectionMode) {
                            if (song.id in selectedIds) selectedIds.removeAll { it == song.id } else selectedIds.add(song.id)
                        } else {
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                            onSongSelected(song)
                        }
                    },
                    onToggleFavorite = {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                        onToggleFavorite(song)
                    },
                    onAddToPlaylist = {
                        songForPlaylistPicker = song
                    }
                )
            }
        }
    }

    songsToDelete?.let { targets ->
        DeleteTracksDialog(
            songs = targets,
            skin = skin,
            onRemoveFromLibrary = {
                songsToDelete = null
                onRemoveFromLibrary(targets)
                selectionMode = false
                selectedIds.clear()
            },
            onDeleteFromDevice = {
                songsToDelete = null
                deleteFromDevice(targets)
            },
            onDismiss = { songsToDelete = null }
        )
    }

    // Playlist Picker Dialog
    songForPlaylistPicker?.let { targetSong ->
        AddToPlaylistDialog(
            song = targetSong,
            playlists = playlists,
            skin = skin,
            onPlaylistSelected = { playlistId ->
                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                onAddSongToPlaylist(playlistId, targetSong)
                songForPlaylistPicker = null
            },
            onDismiss = { songForPlaylistPicker = null }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SongRowItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    skin: RetroSkin,
    selectionMode: Boolean,
    isSelected: Boolean,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onPlay, onLongClick = onLongPress)
            .border(
                width = if (isCurrent || isSelected) 2.dp else 1.dp,
                color = when {
                    isSelected -> Color(0xFFEF4444)
                    isCurrent -> skin.accentColor
                    else -> skin.chassisBorderColor.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> Color(0xFF2A1518)
                isCurrent -> Color(0xFF1E2638)
                else -> Color(0xDD181B22)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onPlay() },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444))
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            // Album art thumbnail (real cover, or a colourful per-artist tile)
            SongArtThumbnail(
                song = song,
                size = 48.dp,
                isPlayingNow = isCurrent && isPlaying,
                accent = skin.accentColor
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title, Artist, Album in Modern Sans-Serif Typography
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = if (isCurrent) skin.accentColor else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = listOf(song.artist, song.album, song.year).filter { it.isNotBlank() }.joinToString(" • "),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(0xFFB4BFCE),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = listOfNotNull(
                        song.folderPath.takeIf { it.isNotBlank() },
                        "${song.bitrateKbps} kbps",
                        if (song.playCount > 0) "▶ ${song.playCount}x" else null,
                        if (!song.lyrics.isNullOrBlank()) "♫ lyrics" else null
                    ).joinToString(" • "),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(0xFF8592A6),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Favorite button & Add to Playlist Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) Color(0xFFFF3366) else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onAddToPlaylist,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = skin.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (!selectionMode) {
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Select") },
                                leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                                onClick = { showMenu = false; onLongPress() }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete…", color = Color(0xFFEF4444)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                                onClick = { showMenu = false; onDelete() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddToPlaylistDialog(
    song: Song,
    playlists: List<Playlist>,
    skin: RetroSkin,
    onPlaylistSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add to Mixtape / Playlist",
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select playlist for \"${song.title}\":",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(playlists) { playlist ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF222632))
                                .border(1.dp, skin.chassisBorderColor, RoundedCornerShape(6.dp))
                                .clickable { onPlaylistSelected(playlist.id) }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "📼 ${playlist.name}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = skin.accentColor
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White, fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
            }
        },
        containerColor = Color(0xFF16181E)
    )
}
