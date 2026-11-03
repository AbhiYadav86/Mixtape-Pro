package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RetroSoundEffects
import com.example.data.model.Playlist
import com.example.data.model.RetroSkin
import com.example.data.model.Song

/**
 * A mixtape row that opens like a drop-down to list its songs. Tap the row to open or close it,
 * PLAY to play the whole mixtape, or a song to play the mixtape from that song.
 */
@Composable
fun MixtapeDropdownRow(
    playlist: Playlist,
    skin: RetroSkin,
    currentSong: Song?,
    isPlaying: Boolean,
    loadSongs: suspend (Playlist) -> List<Song>,
    onPlayMixtape: () -> Unit,
    onPlaySong: (songs: List<Song>, index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember(playlist.id) { mutableStateOf(false) }
    var songs by remember(playlist.id) { mutableStateOf<List<Song>?>(null) }

    LaunchedEffect(expanded, playlist.id, playlist.songCount) {
        if (expanded) songs = try { loadSongs(playlist) } catch (e: Exception) { emptyList() }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, if (expanded) skin.accentColor else skin.chassisBorderColor, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B24))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                        expanded = !expanded
                    }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF232B3A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = skin.accentColor, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif,
                        color = Color.White
                    )
                    val count = songs?.size ?: playlist.songCount
                    Text(
                        text = if (count > 0) "$count tracks" + (if (playlist.description.isNotBlank()) " • ${playlist.description}" else "")
                               else playlist.description.ifBlank { "Tap to see the songs" },
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp, fontFamily = FontFamily.SansSerif,
                        color = Color(0xFF94A3B8)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Hide songs" else "Show songs",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                        onPlayMixtape()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = skin.accentColor),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play mixtape", tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("PLAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF10141B))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val list = songs
                    when {
                        list == null -> Text("Loading…", color = Color(0xFF94A3B8), fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                        list.isEmpty() -> Text("No songs in this mixtape yet", color = Color(0xFF94A3B8), fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                        else -> list.forEachIndexed { i, song ->
                            val isCurrent = currentSong?.id == song.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isCurrent) skin.accentColor.copy(alpha = 0.18f) else Color.Transparent)
                                    .clickable {
                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                        onPlaySong(list, i)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${i + 1}".padStart(2, '0'),
                                    fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                    color = if (isCurrent) skin.accentColor else Color(0xFF64748B),
                                    modifier = Modifier.width(26.dp)
                                )
                                SongArtThumbnail(song = song, size = 34.dp, isPlayingNow = isCurrent && isPlaying, accent = skin.accentColor)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isCurrent) skin.accentColor else Color.White)
                                    Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                if (isCurrent && isPlaying) {
                                    Icon(Icons.Default.GraphicEq, contentDescription = "Playing", tint = skin.accentColor, modifier = Modifier.size(18.dp))
                                } else if (song.durationMs > 0) {
                                    val s = song.durationMs / 1000
                                    Text("%d:%02d".format(s / 60, s % 60), fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF94A3B8))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
