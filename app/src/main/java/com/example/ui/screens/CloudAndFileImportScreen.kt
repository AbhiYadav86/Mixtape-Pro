package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.audio.RetroSoundEffects
import com.example.data.model.AudioSource
import com.example.data.model.RetroSkin
import com.example.data.model.Song

@Composable
fun CloudAndFileImportScreen(
    allSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    skin: RetroSkin,
    onImportLocalUris: (List<Uri>) -> Unit,
    onSyncLocalFolders: () -> Unit,
    onImportDriveLink: (link: String, title: String, artist: String) -> Unit,
    onImportCloudUrl: (url: String, title: String, artist: String, genre: String, album: String) -> Unit,
    onPlaySong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSourceTab by remember { mutableIntStateOf(0) } // 0: Phone / Local, 1: Google Drive, 2: Cloud / Web (Dropbox, OneDrive, URLs)

    // Form states
    var driveLinkInput by remember { mutableStateOf("") }
    var driveTitleInput by remember { mutableStateOf("") }
    var driveArtistInput by remember { mutableStateOf("") }

    var cloudUrlInput by remember { mutableStateOf("") }
    var cloudTitleInput by remember { mutableStateOf("") }
    var cloudArtistInput by remember { mutableStateOf("") }
    var cloudGenreInput by remember { mutableStateOf("") }

    // SAF Document Picker for local phone files
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty()) {
            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CD_SPIN)
            onImportLocalUris(uris)
        }
    }

    val driveSongs = remember(allSongs) { allSongs.filter { it.source == AudioSource.GOOGLE_DRIVE } }
    val cloudSongs = remember(allSongs) { allSongs.filter { it.source == AudioSource.CLOUD_STORAGE } }
    val localSongs = remember(allSongs) { allSongs.filter { it.source == AudioSource.LOCAL } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(skin.chassisBackground)
            .padding(10.dp)
    ) {
        // Modern Source Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF181B24))
                .border(1.dp, skin.chassisBorderColor, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val tabs = listOf(
                Triple(0, "PHONE", Icons.Default.PhoneAndroid),
                Triple(1, "DRIVE", Icons.Default.CloudQueue),
                Triple(2, "WEB URL", Icons.Default.Link),
                Triple(3, "EXPORT", Icons.Default.CloudUpload)
            )

            tabs.forEach { (index, title, icon) ->
                val isSelected = selectedSourceTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) skin.accentColor else Color.Transparent)
                        .clickable {
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SWITCH_TOGGLE)
                            selectedSourceTab = index
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedSourceTab) {
            3 -> CloudExportPanel(songs = allSongs, skin = skin, onPlaySong = onPlaySong)
            0 -> {
                // PHONE / LOCAL STORAGE TAB
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222D))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "IMPORT FROM LOCAL DEVICE STORAGE",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Select MP3, WAV, FLAC, AAC, or M4A audio files from your Downloads, SD Card, or Music folders.",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color(0xFF94A3B8)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Primary File Picker Button
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
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .height(52.dp)
                                            .testTag("pick_audio_files_button"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor)
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Black)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Pick Files...",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.SansSerif,
                                            color = Color.Black
                                        )
                                    }

                                    // Device MediaStore Scan Button
                                    OutlinedButton(
                                        onClick = {
                                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CD_SPIN)
                                            onSyncLocalFolders()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .testTag("scan_device_audio_button"),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A5568))
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Scan All",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.SansSerif,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "LOCAL AUDIO TRACKS (${localSongs.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = skin.accentColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (localSongs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No local tracks imported yet. Tap \"Pick Files...\" above to load your audio!",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        items(localSongs) { song ->
                            SongItemRow(
                                song = song,
                                isCurrent = currentSong?.id == song.id,
                                isPlaying = isPlaying,
                                skin = skin,
                                onPlay = { onPlaySong(song) }
                            )
                        }
                    }
                }
            }

            1 -> {
                // GOOGLE DRIVE CLOUD SYNC TAB
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222D))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "GOOGLE DRIVE SHARE LINK IMPORTER",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Paste a Google Drive share link (e.g. https://drive.google.com/file/d/.../view) to stream and cache tracks directly.",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color(0xFF94A3B8)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = driveLinkInput,
                                    onValueChange = { driveLinkInput = it },
                                    label = { Text("Google Drive Share Link or File ID", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) },
                                    placeholder = { Text("https://drive.google.com/file/d/...", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("drive_link_input"),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = driveTitleInput,
                                        onValueChange = { driveTitleInput = it },
                                        label = { Text("Track Title", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = driveArtistInput,
                                        onValueChange = { driveArtistInput = it },
                                        label = { Text("Artist", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        if (driveLinkInput.isNotBlank()) {
                                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                                            onImportDriveLink(
                                                driveLinkInput,
                                                driveTitleInput.ifBlank { "Drive Audio Track" },
                                                driveArtistInput.ifBlank { "Google Drive" }
                                            )
                                            driveLinkInput = ""
                                            driveTitleInput = ""
                                            driveArtistInput = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("submit_drive_import_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Import & Play from Google Drive",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "GOOGLE DRIVE TRACKS (${driveSongs.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = skin.accentColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (driveSongs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No Google Drive tracks synced yet. Paste a link above to import!",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        items(driveSongs) { song ->
                            SongItemRow(
                                song = song,
                                isCurrent = currentSong?.id == song.id,
                                isPlaying = isPlaying,
                                skin = skin,
                                onPlay = { onPlaySong(song) }
                            )
                        }
                    }
                }
            }

            2 -> {
                // OTHER CLOUD STORAGE & WEB STREAMS (Dropbox, OneDrive, WebDAV, Direct URL)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222D))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "DROPBOX, ONEDRIVE & CLOUD STREAM IMPORTER",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Stream any audio link from Dropbox, Microsoft OneDrive, WebDAV, Icecast, or direct HTTP/S MP3/WAV/FLAC streams.",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color(0xFF94A3B8)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = cloudUrlInput,
                                    onValueChange = { cloudUrlInput = it },
                                    label = { Text("Cloud Audio Stream URL / Dropbox / OneDrive Link", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) },
                                    placeholder = { Text("https://www.dropbox.com/s/.../track.mp3", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("cloud_url_input"),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = cloudTitleInput,
                                        onValueChange = { cloudTitleInput = it },
                                        label = { Text("Track Title", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = cloudArtistInput,
                                        onValueChange = { cloudArtistInput = it },
                                        label = { Text("Artist", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = cloudGenreInput,
                                    onValueChange = { cloudGenreInput = it },
                                    label = { Text("Genre (Optional, e.g. Synthwave, Trance, Rock)", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        if (cloudUrlInput.isNotBlank()) {
                                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                            onImportCloudUrl(
                                                cloudUrlInput,
                                                cloudTitleInput.ifBlank { "Cloud Audio Stream" },
                                                cloudArtistInput.ifBlank { "Cloud Storage" },
                                                cloudGenreInput.ifBlank { "Cloud Stream" },
                                                "Cloud Storage"
                                            )
                                            cloudUrlInput = ""
                                            cloudTitleInput = ""
                                            cloudArtistInput = ""
                                            cloudGenreInput = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("submit_cloud_import_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Import & Play Cloud Track",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "CLOUD & WEB STREAMS (${cloudSongs.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = skin.accentColor,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (cloudSongs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No custom cloud streams imported yet. Enter a URL above to connect!",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        items(cloudSongs) { song ->
                            SongItemRow(
                                song = song,
                                isCurrent = currentSong?.id == song.id,
                                isPlaying = isPlaying,
                                skin = skin,
                                onPlay = { onPlaySong(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongItemRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    skin: RetroSkin,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                onPlay()
            }
            .border(
                width = if (isCurrent) 2.dp else 1.dp,
                color = if (isCurrent) skin.accentColor else skin.chassisBorderColor,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = if (isCurrent) Color(0xFF1E2638) else Color(0xFF161922))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCurrent) skin.accentColor else Color(0xFF2B3242)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (song.source) {
                        AudioSource.GOOGLE_DRIVE -> Icons.Default.CloudQueue
                        AudioSource.CLOUD_STORAGE -> Icons.Default.Cloud
                        else -> Icons.Default.MusicNote
                    },
                    contentDescription = null,
                    tint = if (isCurrent) Color.Black else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = if (isCurrent) skin.accentColor else Color.White,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${song.artist} • ${song.genre}",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (isCurrent && isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = "Play",
                    tint = if (isCurrent) skin.accentColor else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
