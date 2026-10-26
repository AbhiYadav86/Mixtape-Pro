package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.RetroSoundEffects
import com.example.data.model.Playlist
import com.example.data.model.RetroSkin
import com.example.data.model.Song

@Composable
fun PlaylistScreen(
    playlists: List<Playlist>,
    selectedPlaylist: Playlist?,
    playlistSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    skin: RetroSkin,
    onSelectPlaylist: (Playlist) -> Unit,
    onPlayPlaylist: (Playlist) -> Unit,
    onCreatePlaylist: (name: String, desc: String) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onPlaySong: (Song) -> Unit,
    onRemoveSongFromPlaylist: (playlistId: String, songId: String) -> Unit,
    onRestoreCuratedMixtapes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistSearchQuery by remember { mutableStateOf("") }

    val filteredPlaylists = remember(playlists, playlistSearchQuery) {
        if (playlistSearchQuery.isBlank()) {
            playlists
        } else {
            playlists.filter {
                it.name.contains(playlistSearchQuery, ignoreCase = true) ||
                it.description.contains(playlistSearchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(skin.chassisBackground)
            .padding(10.dp)
    ) {
        // Title
        Text(
            text = "MIXTAPES (${filteredPlaylists.size})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = skin.lcdTextColor,
            maxLines = 1
        )
        Text(
            text = "Cassette C-90 & Compact Disc Vault",
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            color = Color(0xFFB4BFCE),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Actions: full-width buttons so labels are always readable
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // New Mixtape Button
            Button(
                onClick = {
                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                    showCreateDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .testTag("create_playlist_button"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "NEW MIXTAPE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Restore Curated 90s Mixtapes
            OutlinedButton(
                onClick = {
                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                    onRestoreCuratedMixtapes()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = skin.accentColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, skin.accentColor.copy(alpha = 0.7f)),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = skin.accentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "90s PRESETS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mixtape / Playlist Search Bar
        OutlinedTextField(
            value = playlistSearchQuery,
            onValueChange = { playlistSearchQuery = it },
            placeholder = {
                Text(
                    text = "Search mixtapes, cassettes, playlists...",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(0xFF94A3B8)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Mixtapes",
                    tint = skin.accentColor
                )
            },
            trailingIcon = {
                if (playlistSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { playlistSearchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("mixtape_search_input"),
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

        Spacer(modifier = Modifier.height(10.dp))

        // Playlists horizontal preview list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("playlists_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredPlaylists) { playlist ->
                val isSelected = selectedPlaylist?.id == playlist.id
                PlaylistCard(
                    playlist = playlist,
                    isSelected = isSelected,
                    skin = skin,
                    onSelect = {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                        onSelectPlaylist(playlist)
                    },
                    onPlayAll = {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                        onPlayPlaylist(playlist)
                    },
                    onDelete = {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                        onDeletePlaylist(playlist)
                    }
                )

                // If selected, show songs inside this playlist
                if (isSelected) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                            .background(Color(0xFF0F1219), RoundedCornerShape(8.dp))
                            .border(1.dp, skin.accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRACKLIST • ${playlistSongs.size}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )

                            if (playlistSongs.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                                        onPlayPlaylist(playlist)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("PLAY MIXTAPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (playlistSongs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Blank magnetic tape. Add songs from your Library!",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color(0xFF64748B)
                                )
                            }
                        } else {
                            playlistSongs.forEachIndexed { index, song ->
                                val isCur = currentSong?.id == song.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable {
                                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                            onPlaySong(song)
                                        },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCur) Color(0xFF1E283D) else Color(0xFF161B24)
                                    ),
                                    border = if (isCur) androidx.compose.foundation.BorderStroke(1.dp, skin.accentColor) else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "${(index + 1).toString().padStart(2, '0')}  ",
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCur) Color.White else Color(0xFF94A3B8)
                                            )
                                            Column {
                                                Text(
                                                    text = song.title,
                                                    fontSize = 13.sp,
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${song.artist} • ${song.genre}",
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.SansSerif,
                                                    color = Color(0xFF94A3B8),
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                                                onRemoveSongFromPlaylist(playlist.id, song.id)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            skin = skin,
            onCreate = { name, desc ->
                onCreatePlaylist(name, desc)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    isSelected: Boolean,
    skin: RetroSkin,
    onSelect: () -> Unit,
    onPlayAll: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) skin.accentColor else skin.chassisBorderColor,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF1B212D) else Color(0xFF171A23))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Cassette or CD art badge
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1115))
                    .border(1.dp, skin.chassisBorderColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val coverResId: Int = com.example.data.model.BuiltInArt.cover(playlist.coverRes)
                Image(
                    painter = painterResource(id = coverResId),
                    contentDescription = playlist.name,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = if (isSelected) skin.accentColor else Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = playlist.description.ifEmpty { "Curated Mixtape Pro tape" },
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(0xFFAAB6C6),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = onPlayAll,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = "Play Mixtape",
                        tint = skin.accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Mixtape",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    skin: RetroSkin,
    onCreate: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    val presetTemplates = listOf(
        "Eurodance '99 Club Hits" to "High-energy 90s eurodance & club party",
        "Late Night Synth Cruise" to "Chilled nostalgic synthwave & lofi beats",
        "Seattle Grunge Flannel" to "Raw 90s alternative rock & garage guitars",
        "Cyberpunk 1999 Hacker Deck" to "Chiptunes, IDM & dark cyber grooves",
        "Tokyo City Pop Highway" to "Smooth 90s Japanese city pop & jazz fusion",
        "Y2K Rave Millennium" to "Trance energy & millennium dance anthems"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Album, contentDescription = null, tint = skin.accentColor)
                Text(
                    text = "Record New Mixtape",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "QUICK 90s TEMPLATES:",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = skin.accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Quick preset pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetTemplates.take(3).forEach { (tName, tDesc) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF242A38))
                                .border(1.dp, Color(0xFF3B4861), RoundedCornerShape(4.dp))
                                .clickable {
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                    name = tName
                                    desc = tDesc
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tName.split(" ").first(),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Mixtape Title", fontFamily = FontFamily.SansSerif, fontSize = 14.sp) },
                    placeholder = { Text("e.g. Summer '99 Eurodance", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Liner Notes / Description", fontFamily = FontFamily.SansSerif, fontSize = 14.sp) },
                    placeholder = { Text("e.g. Recorded live from FM radio", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                        onCreate(name, desc)
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor),
                modifier = Modifier
                    .height(46.dp)
                    .testTag("confirm_create_playlist")
            ) {
                Text("RECORD TAPE", color = Color.Black, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(46.dp)
            ) {
                Text("Cancel", color = Color.White, fontFamily = FontFamily.SansSerif, fontSize = 13.sp)
            }
        },
        containerColor = Color(0xFF181A22)
    )
}
