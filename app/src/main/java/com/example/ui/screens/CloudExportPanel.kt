package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioSource
import com.example.data.model.RetroSkin
import com.example.data.model.Song
import com.example.ui.components.SongArtThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Cloud → EXPORT tab.
 * Upload DJ Studio exports (or any track on the phone) to Google Drive, Dropbox, OneDrive, email…
 *  - "Upload" opens Android's share sheet (any installed cloud app).
 *  - "Save to…" opens the system file picker, where Google Drive / Dropbox folders can be chosen directly.
 */
@Composable
fun CloudExportPanel(
    songs: List<Song>,
    skin: RetroSkin,
    onPlaySong: (Song) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var pendingSave by remember { mutableStateOf<Song?>(null) }

    val studioExports = remember(songs) {
        songs.filter { it.album == "Mixtape Pro Studio" || it.folderPath == "Mixtape Pro" }
    }
    val otherLocal = remember(songs, query) {
        songs.filter { it.source == AudioSource.LOCAL && it !in studioExports }
            .filter { query.isBlank() || it.title.contains(query, true) || it.artist.contains(query, true) }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/*")
    ) { target: Uri? ->
        val song = pendingSave
        pendingSave = null
        if (target == null || song == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    val resolver = context.contentResolver
                    resolver.openInputStream(Uri.parse(song.uri))?.use { input ->
                        resolver.openOutputStream(target)?.use { output -> input.copyTo(output) }
                    } != null
                } catch (e: Exception) {
                    false
                }
            }
            Toast.makeText(
                context,
                if (ok) "Saved \"${song.title}\"" else "Couldn't copy \"${song.title}\"",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun fileNameFor(song: Song): String {
        val ext = when {
            song.uri.endsWith(".wav", true) || song.bitrateKbps >= 1411 -> "wav"
            song.uri.endsWith(".m4a", true) -> "m4a"
            else -> "mp3"
        }
        return "${song.artist} - ${song.title}".replace(Regex("""[\\/:*?"<>|]"""), "_") + ".$ext"
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF10141C))
                    .border(1.5.dp, skin.accentColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = skin.accentColor, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXPORT TO THE CLOUD", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Upload sends the file to any cloud app on your phone (Google Drive, Dropbox, OneDrive, email). " +
                        "Save to… lets you pick a Drive or Dropbox folder in the file picker.",
                    color = Color(0xFFB4BFCE),
                    fontSize = 12.sp
                )
            }
        }

        item { SectionHeader("DJ STUDIO EXPORTS (${studioExports.size})", skin) }
        if (studioExports.isEmpty()) {
            item {
                Text(
                    "Nothing exported yet. Make a mix, splice, ringtone or instrumental in DJ Studio and it will appear here.",
                    color = Color(0xFFB4BFCE),
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
        items(studioExports, key = { "x_" + it.id }) { song ->
            ExportRow(song, skin,
                onPlay = { onPlaySong(song) },
                onUpload = { shareAudioFile(context, song.uri, song.title) },
                onSaveTo = { pendingSave = song; saveLauncher.launch(fileNameFor(song)) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader("BACK UP ANY TRACK", skin)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search your phone's music…", color = Color(0xFF94A3B8)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = skin.accentColor) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = skin.accentColor,
                    unfocusedBorderColor = skin.chassisBorderColor
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(otherLocal.take(200), key = { "l_" + it.id }) { song ->
            ExportRow(song, skin,
                onPlay = { onPlaySong(song) },
                onUpload = { shareAudioFile(context, song.uri, song.title) },
                onSaveTo = { pendingSave = song; saveLauncher.launch(fileNameFor(song)) }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionHeader(text: String, skin: RetroSkin) {
    Text(
        text,
        color = skin.accentColor,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun ExportRow(
    song: Song,
    skin: RetroSkin,
    onPlay: () -> Unit,
    onUpload: () -> Unit,
    onSaveTo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xDD181B22))
            .border(1.dp, skin.chassisBorderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onPlay)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongArtThumbnail(song = song, size = 44.dp, accent = skin.accentColor)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = Color(0xFFB4BFCE), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onSaveTo) {
            Icon(Icons.Default.SaveAlt, contentDescription = "Save to…", tint = Color.White)
        }
        IconButton(onClick = onUpload) {
            Icon(Icons.Default.CloudUpload, contentDescription = "Upload", tint = skin.accentColor)
        }
    }
}
