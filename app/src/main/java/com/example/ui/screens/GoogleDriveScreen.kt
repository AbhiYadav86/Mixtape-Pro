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
import com.example.audio.RetroSoundEffects
import com.example.data.model.AudioSource
import com.example.data.model.RetroSkin
import com.example.data.model.Song

@Composable
fun GoogleDriveScreen(
    driveSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    skin: RetroSkin,
    onImportDriveLink: (link: String, title: String, artist: String) -> Unit,
    onPlaySong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var driveLinkInput by remember { mutableStateOf("") }
    var trackTitleInput by remember { mutableStateOf("") }
    var artistInput by remember { mutableStateOf("") }
    var showImportForm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(skin.chassisBackground)
            .padding(8.dp)
    ) {
        // Cloud Drive Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222D))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudQueue, contentDescription = "Google Drive", tint = skin.accentColor, modifier = Modifier.size(24.dp))
                        Text(
                            text = "Google Drive Audio Sync",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = {
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                            showImportForm = !showImportForm
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("toggle_drive_import_button")
                    ) {
                        Icon(if (showImportForm) Icons.Default.Close else Icons.Default.Add, contentDescription = "Import", tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showImportForm) "Close" else "Import Link", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, color = Color.Black)
                    }
                }

                Text(
                    text = "Stream and categorize MP3 / WAV audio files directly from Google Drive shareable links or folders.",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (showImportForm) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = driveLinkInput,
                        onValueChange = { driveLinkInput = it },
                        label = { Text("Google Drive Share Link / File ID", fontFamily = FontFamily.SansSerif, fontSize = 13.sp) },
                        placeholder = { Text("https://drive.google.com/file/d/...", fontSize = 13.sp, fontFamily = FontFamily.SansSerif) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drive_link_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = trackTitleInput,
                            onValueChange = { trackTitleInput = it },
                            label = { Text("Track Title", fontFamily = FontFamily.SansSerif, fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = artistInput,
                            onValueChange = { artistInput = it },
                            label = { Text("Artist", fontFamily = FontFamily.SansSerif, fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (driveLinkInput.isNotBlank()) {
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                                onImportDriveLink(
                                    driveLinkInput,
                                    trackTitleInput.ifBlank { "Drive Audio File" },
                                    artistInput.ifBlank { "Google Drive" }
                                )
                                driveLinkInput = ""
                                trackTitleInput = ""
                                artistInput = ""
                                showImportForm = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = skin.secondaryAccentColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("confirm_drive_import_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Import & Sync Drive Track", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Drive Songs List
        Text(
            text = "GOOGLE DRIVE TRACKS (${driveSongs.size})",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = skin.lcdTextColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (driveSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF14161C), RoundedCornerShape(8.dp))
                    .border(1.dp, skin.chassisBorderColor, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Google Drive songs synced yet.",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap 'Import Link' to add Google Drive shared MP3s!",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(driveSongs) { song ->
                    val isCurrent = currentSong?.id == song.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                onPlaySong(song)
                            }
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) skin.accentColor else skin.chassisBorderColor,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF191D26))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif,
                                    color = if (isCurrent) skin.accentColor else Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${song.artist} • Cloud MP3",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            IconButton(
                                onClick = {
                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                    onPlaySong(song)
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCurrent && isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = skin.accentColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
