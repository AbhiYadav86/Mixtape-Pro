package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.RetroSoundEffects
import com.example.data.model.RetroSkin
import com.example.data.model.Song
import com.example.data.repository.ArchiveItem
import com.example.data.repository.StreamingRepository
import com.example.ui.components.SongArtThumbnail
import com.example.ui.viewmodel.StreamingViewModel

/**
 * Streaming tab: 90s Radio + Audius + Internet Archive.
 * Every online track can be played, saved to the Library, or downloaded for offline play.
 */
@Composable
fun StreamingScreen(
    streamingViewModel: StreamingViewModel,
    skin: RetroSkin,
    currentSong: Song?,
    isPlaying: Boolean,
    onPlayTrack: (song: Song, queue: List<Song>) -> Unit,
    onSaveToLibrary: (Song) -> Unit,
    onStatus: (String) -> Unit,
    radioContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var section by rememberSaveable { mutableIntStateOf(0) } // 0 Radio, 1 Audius, 2 Archive

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(skin.chassisBackground)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Source selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF12151C))
                .border(1.dp, skin.chassisBorderColor, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val sources = listOf(
                Triple("RADIO", Icons.Default.Radio, 0),
                Triple("AUDIUS", Icons.Default.Bolt, 1),
                Triple("ARCHIVE", Icons.Default.AccountBalance, 2)
            )
            sources.forEach { (label, icon, idx) ->
                SourceTab(
                    label = label,
                    icon = icon,
                    selected = section == idx,
                    skin = skin,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                        section = idx
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when (section) {
            0 -> Box(modifier = Modifier.fillMaxSize()) { radioContent() }
            1 -> AudiusPanel(streamingViewModel, skin, currentSong, isPlaying, onPlayTrack, onSaveToLibrary, onStatus)
            else -> ArchivePanel(streamingViewModel, skin, currentSong, isPlaying, onPlayTrack, onSaveToLibrary, onStatus)
        }
    }
}

@Composable
private fun SourceTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    skin: RetroSkin,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) skin.accentColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color.Black else Color(0xFFE2E8F0),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            color = if (selected) Color.Black else Color(0xFFE2E8F0),
            maxLines = 1
        )
    }
}

@Composable
private fun SearchField(
    hint: String,
    skin: RetroSkin,
    onSearch: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        placeholder = { Text(hint, color = Color(0xFF94A3B8), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = skin.accentColor) },
        trailingIcon = {
            if (text.isNotEmpty()) {
                IconButton(onClick = { text = ""; onSearch("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            focus.clearFocus()
            onSearch(text)
        }),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = skin.accentColor,
            unfocusedBorderColor = skin.chassisBorderColor,
            focusedContainerColor = Color(0xFF14171F),
            unfocusedContainerColor = Color(0xFF14171F),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = skin.accentColor
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ChipRow(
    options: List<String>,
    selected: String?,
    skin: RetroSkin,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            val isSel = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSel) skin.accentColor else Color(0xFF1E222D))
                    .border(1.dp, if (isSel) skin.accentColor else Color(0x40FFFFFF), RoundedCornerShape(16.dp))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = option,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSel) Color.Black else Color(0xFFE2E8F0),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StatusLine(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AudiusPanel(
    vm: StreamingViewModel,
    skin: RetroSkin,
    currentSong: Song?,
    isPlaying: Boolean,
    onPlayTrack: (Song, List<Song>) -> Unit,
    onSaveToLibrary: (Song) -> Unit,
    onStatus: (String) -> Unit
) {
    val tracks by vm.audiusTracks.collectAsState()
    val loading by vm.audiusLoading.collectAsState()
    val genre by vm.audiusGenre.collectAsState()
    val error by vm.audiusError.collectAsState()

    LaunchedEffect(Unit) { vm.loadAudiusIfEmpty() }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchField(hint = "Search Audius artists & tracks", skin = skin, onSearch = { q ->
            if (q.isBlank()) vm.loadAudiusTrending(genre) else vm.searchAudius(q)
        })
        Spacer(modifier = Modifier.height(6.dp))
        ChipRow(StreamingRepository.AUDIUS_GENRES, genre, skin) { vm.loadAudiusTrending(it) }
        StatusLine(
            text = when {
                loading -> "TUNING IN TO AUDIUS…"
                error != null -> error ?: ""
                else -> "AUDIUS OPEN STREAM • ${tracks.size} TRACKS"
            },
            color = if (error != null && !loading) Color(0xFFFBBF24) else skin.accentColor
        )
        if (loading && tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = skin.accentColor)
            }
        } else {
            TrackList(tracks, skin, currentSong, isPlaying, onPlayTrack, onSaveToLibrary, vm, onStatus)
        }
    }
}

@Composable
private fun ArchivePanel(
    vm: StreamingViewModel,
    skin: RetroSkin,
    currentSong: Song?,
    isPlaying: Boolean,
    onPlayTrack: (Song, List<Song>) -> Unit,
    onSaveToLibrary: (Song) -> Unit,
    onStatus: (String) -> Unit
) {
    val items by vm.archiveItems.collectAsState()
    val loading by vm.archiveLoading.collectAsState()
    val preset by vm.archivePreset.collectAsState()
    val openItem by vm.openArchiveItem.collectAsState()
    val tracks by vm.archiveTracks.collectAsState()
    val error by vm.archiveError.collectAsState()

    LaunchedEffect(Unit) { vm.loadArchiveIfEmpty() }

    Column(modifier = Modifier.fillMaxSize()) {
        val item = openItem
        if (item == null) {
            SearchField(hint = "Search the Internet Archive", skin = skin, onSearch = { q -> vm.searchArchive(q) })
            Spacer(modifier = Modifier.height(6.dp))
            ChipRow(StreamingRepository.ARCHIVE_PRESETS.keys.toList(), preset, skin) { vm.selectArchivePreset(it) }
            StatusLine(
                text = when {
                    loading -> "SEARCHING ARCHIVE.ORG…"
                    error != null -> error ?: ""
                    else -> "INTERNET ARCHIVE • ${items.size} COLLECTIONS"
                },
                color = if (error != null && !loading) Color(0xFFFBBF24) else skin.accentColor
            )
            if (loading && items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = skin.accentColor)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                    items(items, key = { it.identifier }) { archiveItem ->
                        ArchiveItemRow(archiveItem, skin) { vm.openArchiveItem(archiveItem) }
                    }
                }
            }
        } else {
            // Album / show detail
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { vm.closeArchiveItem() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E222D))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(item.creator, item.year).filter { it.isNotBlank() }.joinToString(" • "),
                        color = Color(0xFFB4BFCE), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (tracks.isNotEmpty()) {
                    IconButton(onClick = { onPlayTrack(tracks.first(), tracks) }) {
                        Icon(Icons.Default.PlayCircleFilled, contentDescription = "Play all", tint = skin.accentColor, modifier = Modifier.size(32.dp))
                    }
                }
            }
            StatusLine(
                text = when {
                    loading -> "LOADING TRACK LIST…"
                    error != null -> error ?: ""
                    else -> "${tracks.size} TRACKS"
                },
                color = if (error != null && !loading) Color(0xFFFBBF24) else skin.accentColor
            )
            if (loading && tracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = skin.accentColor)
                }
            } else {
                TrackList(tracks, skin, currentSong, isPlaying, onPlayTrack, onSaveToLibrary, vm, onStatus)
            }
        }
    }
}

@Composable
private fun ArchiveItemRow(item: ArchiveItem, skin: RetroSkin, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xDD181B22))
            .border(1.dp, skin.chassisBorderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF262B38))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(item.creator, item.year).filter { it.isNotBlank() }.joinToString(" • "),
                color = Color(0xFFB4BFCE), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = skin.accentColor)
    }
}

@Composable
private fun TrackList(
    tracks: List<Song>,
    skin: RetroSkin,
    currentSong: Song?,
    isPlaying: Boolean,
    onPlayTrack: (Song, List<Song>) -> Unit,
    onSaveToLibrary: (Song) -> Unit,
    vm: StreamingViewModel,
    onStatus: (String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
        items(tracks, key = { it.id }) { song ->
            val isCurrent = currentSong?.id == song.id
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCurrent) Color(0xFF1E2638) else Color(0xDD181B22))
                    .border(
                        if (isCurrent) 2.dp else 1.dp,
                        if (isCurrent) skin.accentColor else skin.chassisBorderColor.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onPlayTrack(song, tracks) }
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SongArtThumbnail(song = song, size = 52.dp, isPlayingNow = isCurrent && isPlaying, accent = skin.accentColor)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            song.title,
                            color = if (isCurrent) skin.accentColor else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            listOf(song.artist, song.genre).filter { it.isNotBlank() }.joinToString(" • "),
                            color = Color(0xFFB4BFCE),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (song.durationMs > 0) {
                            val sec = song.durationMs / 1000
                            Text(
                                String.format("%d:%02d", sec / 60, sec % 60),
                                color = Color(0xFF8592A6),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionPill("Play", Icons.Default.PlayArrow, skin.accentColor, Color.Black, Modifier.weight(1f)) {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                        onPlayTrack(song, tracks)
                    }
                    ActionPill("Save", Icons.Default.LibraryAdd, Color(0xFF262B38), Color.White, Modifier.weight(1f)) {
                        onSaveToLibrary(song)
                    }
                    ActionPill("Offline", Icons.Default.Download, Color(0xFF262B38), Color.White, Modifier.weight(1f)) {
                        if (vm.download(song)) onStatus("Downloading \"${song.title}\" to Music/Mixtape Pro…")
                        else onStatus("Download failed for \"${song.title}\"")
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ActionPill(
    label: String,
    icon: ImageVector,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = content, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
