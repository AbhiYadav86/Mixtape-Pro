package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.RetroSoundEffects
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.StreamingViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PlayerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val skin by viewModel.currentSkin.collectAsState()
    val visualizerType by viewModel.visualizerType.collectAsState()
    val discDisplayMode by viewModel.discDisplayMode.collectAsState()

    val currentSong by viewModel.playerEngine.currentSong.collectAsState()
    val isPlaying by viewModel.playerEngine.isPlaying.collectAsState()
    val isBuffering by viewModel.playerEngine.isBuffering.collectAsState()
    val currentPositionMs by viewModel.playerEngine.currentPositionMs.collectAsState()
    val durationMs by viewModel.playerEngine.durationMs.collectAsState()
    val repeatMode by viewModel.playerEngine.repeatMode.collectAsState()
    val isShuffle by viewModel.playerEngine.isShuffle.collectAsState()

    val currentLyrics by viewModel.currentLyrics.collectAsState()
    val isLyricsOverlayActive by viewModel.isLyricsOverlayActive.collectAsState()
    val isFetchingLyrics by viewModel.isFetchingLyrics.collectAsState()
    val autoShowLyrics by viewModel.autoShowLyrics.collectAsState()

    // Visualizer streams
    val spectrumBands by viewModel.visualizerEngine.spectrumBands.collectAsState()
    val peakHoldBands by viewModel.visualizerEngine.peakHoldBands.collectAsState()
    val vuLeft by viewModel.visualizerEngine.vuLeft.collectAsState()
    val vuRight by viewModel.visualizerEngine.vuRight.collectAsState()
    val waveformPoints by viewModel.visualizerEngine.waveformPoints.collectAsState()

    // Library & Playlists data
    val allSongs by viewModel.allSongs.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()
    val enrichmentProgress by viewModel.enrichmentProgress.collectAsState()
    val studioExport by viewModel.studioExport.collectAsState()
    val mixPlan by viewModel.mixPlan.collectAsState()
    val mixPreview by viewModel.mixPreview.collectAsState()
    val allPlaylists by viewModel.allPlaylists.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val playlistSongs by viewModel.playlistSongs.collectAsState()
    val radioStations by viewModel.radioStations.collectAsState()
    val stationStatusMap by viewModel.stationStatusMap.collectAsState()
    val isScanningFrequencies by viewModel.isScanningFrequencies.collectAsState()
    val onlineSearchResults by viewModel.onlineSearchResults.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Player Deck, 1: Library, 2: Playlists, 3: Radio & Web, 4: Cloud
    val playerUiScale by viewModel.playerUiScale.collectAsState()
    val playerScaleFactor by viewModel.playerScaleFactor.collectAsState()
    var showSkinDialog by remember { mutableStateOf(false) }
    var showEqDialog by remember { mutableStateOf(false) }
    var showQuitConfirmDialog by remember { mutableStateOf(false) }
    var showTrackMenuDialog by remember { mutableStateOf(false) }
    var showFullLyrics by remember { mutableStateOf(false) }
    var showVisualMenu by remember { mutableStateOf(false) }

    val driveSongs = remember(allSongs) {
        allSongs.filter { it.source == AudioSource.GOOGLE_DRIVE }
    }

    // Status banner auto-dismiss
    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            kotlinx.coroutines.delay(3500)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("player_screen_root"),
        containerColor = Color(0xFF0F1115),
        bottomBar = {
            RetroNavigationBar(
                activeTab = activeTab,
                skin = skin,
                onTabSelected = { tab ->
                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                    activeTab = tab
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            // Main Hardware Chassis Container
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(8.dp, RoundedCornerShape(skin.buttonShapeRounded.coerceAtLeast(6).dp))
                    .border(2.dp, skin.chassisBorderColor, RoundedCornerShape(skin.buttonShapeRounded.coerceAtLeast(6).dp)),
                shape = RoundedCornerShape(skin.buttonShapeRounded.coerceAtLeast(6).dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(skin.chassisBackground)
                ) {
                    // Winamp / Retro Hardware Title Bar with Quit, Eq and Skin Buttons
                    RetroTitleBar(
                        skin = skin,
                        onSkinClick = {
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                            showSkinDialog = true
                        },
                        onEqClick = {
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                            showEqDialog = true
                        },
                        onQuitClick = {
                            RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                            showQuitConfirmDialog = true
                        }
                    )

                    // Status Toast Banner (if any)
                    statusMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(skin.accentColor)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = msg,
                                color = Color.Black,
                                maxLines = 2,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Content switcher based on navigation tabs
                    when (activeTab) {
                        0 -> {
                            // MASTER RETRO PLAYER DECK - Dynamically sized to viewport
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val availableHeight = maxHeight

                                // Dynamically scale player elements based on available viewport height
                                val (dynamicScale, dynamicScaleFactor) = when {
                                    availableHeight < 540.dp -> PlayerUiScale.COMPACT to 0.85f
                                    availableHeight < 660.dp -> PlayerUiScale.COMPACT to 0.94f
                                    availableHeight < 780.dp -> PlayerUiScale.STANDARD to 1.0f
                                    availableHeight < 900.dp -> PlayerUiScale.STANDARD to 1.08f
                                    else -> PlayerUiScale.EXPANDED to 1.15f
                                }

                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // 1. TOP: Track Title & Mins/Secs Timer Digital LCD Display
                                    RetroDigitalDisplay(
                                        song = currentSong,
                                        isPlaying = isPlaying,
                                        isBuffering = isBuffering,
                                        currentPositionMs = currentPositionMs,
                                        durationMs = durationMs,
                                        repeatMode = repeatMode,
                                        isShuffle = isShuffle,
                                        skin = skin,
                                        scale = dynamicScale,
                                        scaleFactor = dynamicScaleFactor * 1.10f,
                                        peakLevel = maxOf(vuLeft, vuRight),
                                        isEqActive = true,
                                        isDspActive = true,
                                        onToggleRepeat = { viewModel.toggleRepeat() },
                                        onToggleShuffle = { viewModel.toggleShuffle() }
                                    )

                                    // 2. MIDDLE: Overlaid Equalizer on Album Art Deck (fills available viewport height dynamically)
                                    val playbackProgress = remember(currentPositionMs, durationMs) {
                                        if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                                    }
                                    AlbumArtDisplay(
                                        song = currentSong,
                                        isPlaying = isPlaying,
                                        displayMode = discDisplayMode,
                                        skin = skin,
                                        playbackProgress = playbackProgress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f, fill = true),
                                        onArtworkMissing = { song -> viewModel.requestArtworkIfMissing(song) },
                                        topControlHeader = {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // 1. CONSOLIDATED VISUALIZATION BUTTON (CD, Tape, Vinyl, Bars)
                                                val currentDeckLabel = when (discDisplayMode) {
                                                    DiscDisplayMode.CD_JEWEL_CASE -> "CD"
                                                    DiscDisplayMode.CASSETTE_TAPE -> "Tape"
                                                    DiscDisplayMode.VINYL_RECORD -> "Vinyl"
                                                    DiscDisplayMode.CRT_MONITOR -> "Album Art"
                                                }
                                                val currentSpecLabel = when (visualizerType) {
                                                    VisualizerType.CLASSIC_BARS_PEAK -> "Bars"
                                                    VisualizerType.ANALOG_VU_METERS -> "VU"
                                                    VisualizerType.OSCILLOSCOPE_CRT -> "Scope"
                                                    VisualizerType.CIRCULAR_VORTEX -> "Vortex"
                                                    VisualizerType.MATRIX_RAIN -> "Matrix"
                                                    VisualizerType.LYRICS_TELEPROMPTER -> "Lyrics"
                                                }

                                                RetroTactileButton(
                                                    onClick = {
                                                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                                        showVisualMenu = !showVisualMenu
                                                    },
                                                    shape = RoundedCornerShape(5.dp),
                                                    backgroundColor = if (showVisualMenu) skin.accentColor.copy(alpha = 0.30f) else Color(0xFF1B202A),
                                                    borderUnpressed = if (showVisualMenu) skin.accentColor else skin.accentColor.copy(alpha = 0.8f),
                                                    borderPressed = skin.accentColor,
                                                    accentHighlight = skin.accentColor,
                                                    elevation = 4.dp,
                                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp),
                                                    soundType = RetroSoundEffects.SoundType.SLIDER_TICK
                                                ) { isPressed ->
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.GraphicEq,
                                                            contentDescription = "Visualization",
                                                            tint = skin.accentColor,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = "VISUAL: $currentDeckLabel",
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.SansSerif,
                                                            color = Color.White
                                                        )
                                                        Icon(
                                                            imageVector = if (showVisualMenu) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                            contentDescription = "Menu",
                                                            tint = Color.White.copy(alpha = 0.85f),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }

                                                // 2. RETAINED LYRICS BUTTON WITH TACTILE 3D FEEL
                                                RetroTactileButton(
                                                    onClick = {
                                                        viewModel.toggleLyricsOverlay()
                                                    },
                                                    shape = RoundedCornerShape(5.dp),
                                                    backgroundColor = if (isLyricsOverlayActive) skin.accentColor.copy(alpha = 0.32f) else Color(0xFF1B202A),
                                                    borderUnpressed = if (isLyricsOverlayActive) skin.accentColor else Color(0x55FFFFFF),
                                                    borderPressed = skin.accentColor,
                                                    accentHighlight = if (isLyricsOverlayActive) skin.accentColor else null,
                                                    elevation = 4.dp,
                                                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                                                    soundType = RetroSoundEffects.SoundType.RETRO_CLICK
                                                ) { isPressed ->
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.MusicNote,
                                                            contentDescription = "Lyrics",
                                                            tint = if (isLyricsOverlayActive) skin.accentColor else Color(0xFFCBD5E1),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Text(
                                                            text = if (isLyricsOverlayActive) "♫ LYRICS ON" else "♫ LYRICS",
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.SansSerif,
                                                            color = if (isLyricsOverlayActive) skin.accentColor else Color(0xFFCBD5E1)
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        visualizerContent = {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                if (visualizerType == VisualizerType.LYRICS_TELEPROMPTER) {
                                                    // Separate dedicated visualization in the equalizer area
                                                    RetroLyricsDisplay(
                                                        lyrics = currentLyrics,
                                                        currentPositionMs = currentPositionMs,
                                                        skin = skin,
                                                        isOverlay = false,
                                                        isFetching = isFetchingLyrics,
                                                        onSeekTo = { viewModel.seekToPosition(it) },
                                                        onFetchOnline = { viewModel.fetchOnlineLyricsForCurrentSong() },
                                                        onFetchArt = { viewModel.fetchOnlineArtworkForCurrentSong() },
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    // Equalizer Visualizer (Bars, VU, CRT, Vortex, Matrix)
                                                    VisualizerView(
                                                        spectrumBands = spectrumBands,
                                                        peakHoldBands = peakHoldBands,
                                                        vuLeft = vuLeft,
                                                        vuRight = vuRight,
                                                        waveformPoints = waveformPoints,
                                                        visualizerType = visualizerType,
                                                        skin = skin,
                                                        modifier = Modifier.fillMaxSize()
                                                    )

                                                    // Optional synced lyrics over the visualiser: one line at a time,
                                                    // with a button for the full-screen lyrics
                                                    if (isLyricsOverlayActive) {
                                                        com.example.ui.components.LyricsLineOverlay(
                                                            lyrics = currentLyrics,
                                                            currentPositionMs = currentPositionMs,
                                                            skin = skin,
                                                            isFetching = isFetchingLyrics,
                                                            onFetchOnline = { viewModel.fetchOnlineLyricsForCurrentSong() },
                                                            onFullScreen = { showFullLyrics = true },
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 2.dp),
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        // 3. 90s Scrub Seek Bar
                                        RetroSeekBar(
                                            currentPositionMs = currentPositionMs,
                                            durationMs = durationMs,
                                            skin = skin,
                                            scale = dynamicScale,
                                            scaleFactor = dynamicScaleFactor,
                                            onSeek = { viewModel.seekTo(it) }
                                        )

                                        // 4. Beveled Physical Transport Controls
                                        RetroControlBar(
                                            isPlaying = isPlaying,
                                            skin = skin,
                                            scale = dynamicScale,
                                            scaleFactor = dynamicScaleFactor,
                                            onPrevious = { viewModel.previousTrack() },
                                            onPlayPause = { viewModel.togglePlayPause() },
                                            onStop = { viewModel.stopPlayback() },
                                            onNext = { viewModel.nextTrack() },
                                            onOpenTrackMenu = {
                                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                                                showTrackMenuDialog = true
                                            }
                                        )
                                    }

                                } // end player column

                                    // In-Tree Custom Retro Pop-Up Menu Overlay
                                    // Eliminates native WindowManager PopupWindow to resolve BLASTBufferQueue dtor desync
                                    if (showVisualMenu) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0x99000000))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    showVisualMenu = false
                                                }
                                                .padding(horizontal = 16.dp, vertical = 20.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.95f)
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) { /* Consume click inside popup */ }
                                                    .shadow(16.dp, RoundedCornerShape(10.dp))
                                                    .border(2.dp, skin.accentColor, RoundedCornerShape(10.dp)),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141822))
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp)
                                                ) {
                                                    // Header Bar
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.GraphicEq,
                                                                contentDescription = null,
                                                                tint = skin.accentColor,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Text(
                                                                text = "VISUALIZATION SELECTOR",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Black,
                                                                fontFamily = FontFamily.SansSerif,
                                                                color = Color.White,
                                                                letterSpacing = 0.5.sp
                                                            )
                                                        }

                                                        RetroTactileButton(
                                                            onClick = {
                                                                RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                                                                showVisualMenu = false
                                                            },
                                                            shape = RoundedCornerShape(4.dp),
                                                            backgroundColor = Color(0x33EF4444),
                                                            borderUnpressed = Color(0xFFEF4444).copy(alpha = 0.7f),
                                                            elevation = 2.dp,
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            soundType = RetroSoundEffects.SoundType.WINAMP_CLICK
                                                        ) {
                                                            Text(
                                                                text = "✕",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(10.dp))

                                                    // Section 1: Media Deck Styles
                                                    Text(
                                                        text = "── MEDIA DECK (CD / TAPE / VINYL / ART) ──",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = skin.accentColor,
                                                        modifier = Modifier.padding(bottom = 6.dp)
                                                    )

                                                    listOf(
                                                        DiscDisplayMode.CD_JEWEL_CASE to "CD (spinning disc)",
                                                        DiscDisplayMode.CASSETTE_TAPE to "Cassette tape (close-up)",
                                                        DiscDisplayMode.VINYL_RECORD to "Vinyl record (turntable)",
                                                        DiscDisplayMode.CRT_MONITOR to "Album art"
                                                    ).forEach { (mode, label) ->
                                                        val isSelected = discDisplayMode == mode
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (isSelected) skin.accentColor.copy(alpha = 0.25f) else Color(0x18FFFFFF))
                                                                .border(1.dp, if (isSelected) skin.accentColor else Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                                                                .clickable {
                                                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.CASSETTE_CLACK)
                                                                    viewModel.setDiscDisplayMode(mode)
                                                                    showVisualMenu = false
                                                                }
                                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                                contentDescription = null,
                                                                tint = if (isSelected) skin.accentColor else Color(0xFF64748B),
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                            Text(
                                                                text = label,
                                                                fontSize = 11.5.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    // Section 2: Audio Equalizer Styles
                                                    Text(
                                                        text = "── AUDIO EQUALIZER (BARS / VU / SCOPE) ──",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = skin.accentColor,
                                                        modifier = Modifier.padding(bottom = 6.dp)
                                                    )

                                                    listOf(
                                                        VisualizerType.CLASSIC_BARS_PEAK to "Classic Peak Spectrum Bars",
                                                        VisualizerType.ANALOG_VU_METERS to "Dual Needle Analog VU Meters",
                                                        VisualizerType.OSCILLOSCOPE_CRT to "CRT Phosphor Oscilloscope",
                                                        VisualizerType.CIRCULAR_VORTEX to "Radial Vortex Scope",
                                                        VisualizerType.MATRIX_RAIN to "Matrix Digital Rain",
                                                        VisualizerType.LYRICS_TELEPROMPTER to "Lyrics Teleprompter (full panel)"
                                                    ).forEach { (type, label) ->
                                                        val isSelected = visualizerType == type
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (isSelected) skin.accentColor.copy(alpha = 0.25f) else Color(0x18FFFFFF))
                                                                .border(1.dp, if (isSelected) skin.accentColor else Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                                                                .clickable {
                                                                    RetroSoundEffects.playClick(RetroSoundEffects.SoundType.SLIDER_TICK)
                                                                    viewModel.setVisualizerType(type)
                                                                    showVisualMenu = false
                                                                }
                                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                                contentDescription = null,
                                                                tint = if (isSelected) skin.accentColor else Color(0xFF64748B),
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                            Text(
                                                                text = label,
                                                                fontSize = 11.5.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    // Section 3: Lyrics behaviour
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0x18FFFFFF))
                                                            .clickable { viewModel.setAutoShowLyrics(!autoShowLyrics) }
                                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "Auto-show lyrics",
                                                                fontSize = 11.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                            Text(
                                                                text = "Replace the spectrum with lyrics when a song has them",
                                                                fontSize = 10.sp,
                                                                color = Color(0xFFB4BFCE)
                                                            )
                                                        }
                                                        Switch(
                                                            checked = autoShowLyrics,
                                                            onCheckedChange = { viewModel.setAutoShowLyrics(it) },
                                                            colors = SwitchDefaults.colors(
                                                                checkedThumbColor = Color.Black,
                                                                checkedTrackColor = skin.accentColor
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                        1 -> {
                            // LIBRARY SCREEN
                            LibraryScreen(
                                songs = librarySongs,
                                enrichmentProgress = enrichmentProgress,
                                playlists = allPlaylists,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                skin = skin,
                                onSongSelected = { song -> viewModel.playSong(song) },
                                onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                                onSyncLocalFolders = { viewModel.syncLocalFolders() },
                                onImportLocalUris = { uris -> viewModel.importMultipleLocalAudioUris(uris) },
                                onAddSongToPlaylist = { pid, song -> viewModel.addSongToPlaylist(pid, song) },
                                isShuffle = isShuffle,
                                onShuffleAll = { list -> viewModel.shuffleAll(list) },
                                onRemoveFromLibrary = { list -> viewModel.removeSongsFromLibrary(list, deletedFromDevice = false) },
                                onDeletedFromDevice = { list -> viewModel.removeSongsFromLibrary(list, deletedFromDevice = true) },
                                onRestoreRemoved = { viewModel.restoreRemovedTracks() }
                            )
                        }
                        2 -> {
                            // PLAYLISTS / MIXTAPES SCREEN
                            PlaylistScreen(
                                playlists = allPlaylists,
                                selectedPlaylist = selectedPlaylist,
                                playlistSongs = playlistSongs,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                skin = skin,
                                onSelectPlaylist = { playlist -> viewModel.selectPlaylist(playlist) },
                                onPlayPlaylist = { playlist -> viewModel.playPlaylist(playlist) },
                                onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
                                onDeletePlaylist = { playlist -> viewModel.deletePlaylist(playlist) },
                                onPlaySong = { song -> viewModel.playSong(song) },
                                onRemoveSongFromPlaylist = { pid, sid -> viewModel.removeSongFromPlaylist(pid, sid) },
                                onRestoreCuratedMixtapes = { viewModel.restoreCuratedMixtapes() }
                            )
                        }
                        3 -> {
                            // STREAMING: 90s Radio + Audius + Internet Archive
                            val streamingViewModel: StreamingViewModel = viewModel()
                            StreamingScreen(
                                streamingViewModel = streamingViewModel,
                                skin = skin,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                onPlayTrack = { song, queue ->
                                    viewModel.playQueue(queue, queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0))
                                },
                                onSaveToLibrary = { song -> viewModel.saveOnlineSongToLibrary(song) },
                                onStatus = { msg -> viewModel.showStatus(msg) },
                                radioContent = {
                                    RadioScreen(
                                        radioStations = radioStations,
                                        stationStatusMap = stationStatusMap,
                                        isScanningFrequencies = isScanningFrequencies,
                                        onlineSearchResults = onlineSearchResults,
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        skin = skin,
                                        onPlaySong = { song -> if (song.source == com.example.data.model.AudioSource.RADIO_STREAM) viewModel.playRadioStation(song) else viewModel.playSong(song) },
                                        onSearchOnline = { query -> viewModel.searchOnline(query) },
                                        onSaveToLibrary = { song -> viewModel.saveOnlineSongToLibrary(song) },
                                        onScanFrequencies = { viewModel.scanRadioStationsLiveStatus(force = true) },
                                        onRecheckStation = { station -> viewModel.recheckSingleStation(station) },
                                        onNavigateToPlayer = { activeTab = 0 }
                                    )
                                }
                            )
                        }
                        4 -> {
                            // DJ TOOLS & MULTI-TRACK SOUND EDITOR
                            DJStudioScreen(
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                allSongs = librarySongs,
                                skin = skin,
                                equalizerController = viewModel.equalizerController,
                                onTurnPlayerOff = {
                                    if (isPlaying) {
                                        viewModel.pausePlayback()
                                    }
                                },
                                onPlaySong = { song -> viewModel.playSong(song) },
                                onSaveMixedTrack = { title, artist, genre, durationSec, sourceUri, albumArtUri, formatName, targetPlaylist, onComplete ->
                                    viewModel.saveMixedMixtapeTrack(
                                        title = title,
                                        artist = artist,
                                        genre = genre,
                                        durationSec = durationSec,
                                        sourceUri = sourceUri,
                                        albumArtUri = albumArtUri,
                                        formatName = formatName,
                                        targetPlaylist = targetPlaylist,
                                        onExportComplete = onComplete
                                    )
                                },
                                onOpenEqualizerDialog = { showEqDialog = true },
                                onNavigateToTab = { activeTab = it },
                                exportState = studioExport,
                                onExportMix = { parts, xfade, title, artist, format, playlist, art ->
                                    viewModel.exportStudioMix(parts, xfade, title, artist, format, playlist, art)
                                },
                                onExportRingtone = { song, start, end, fadeIn, fadeOut, title ->
                                    viewModel.exportRingtone(song, start, end, fadeIn, fadeOut, title)
                                },
                                onDismissExport = { viewModel.clearStudioExportMessage() },
                                mixPlan = mixPlan,
                                mixPreview = mixPreview,
                                onMixAdd = { viewModel.addToMix(it) },
                                onMixRemove = { viewModel.removeFromMix(it) },
                                onMixMove = { key, d -> viewModel.moveInMix(key, d) },
                                onMixUpdate = { viewModel.updateMixItem(it) },
                                onMixClear = { viewModel.clearMix() },
                                onMixPreview = { viewModel.previewTransition(it) },
                                onMixPreviewDone = { viewModel.clearMixPreview() },
                                onMixExport = { title, artist, format ->
                                    viewModel.exportMixtape(title, artist, format, "DJ Studio Mixtapes")
                                },
                                studioVm = viewModel,
                                studioPlaylists = allPlaylists
                            )
                        }
                        5 -> {
                            // CLOUD STORAGE & FILE IMPORT SCREEN (Google Drive, Phone Storage, Dropbox, Web URLs)
                            CloudAndFileImportScreen(
                                allSongs = allSongs,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                skin = skin,
                                onImportLocalUris = { uris -> viewModel.importMultipleLocalAudioUris(uris) },
                                onSyncLocalFolders = { viewModel.syncLocalFolders() },
                                onImportDriveLink = { link, title, artist ->
                                    viewModel.importGoogleDriveLink(link, title, artist)
                                    activeTab = 0
                                },
                                onImportCloudUrl = { url, title, artist, genre, album ->
                                    viewModel.importCloudStorageFile(url, title, artist, genre, album)
                                    activeTab = 0
                                },
                                onPlaySong = { song ->
                                    viewModel.playSong(song)
                                    activeTab = 0
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Quick Open Track & Mixtape Menu Dialog
    if (showFullLyrics) {
        com.example.ui.components.FullScreenLyricsDialog(
            title = currentSong?.title ?: "",
            artist = currentSong?.artist ?: "",
            lyrics = currentLyrics,
            currentPositionMs = currentPositionMs,
            skin = skin,
            isFetching = isFetchingLyrics,
            onSeekTo = { viewModel.seekToPosition(it) },
            onFetchOnline = { viewModel.fetchOnlineLyricsForCurrentSong() },
            onFetchArt = { viewModel.fetchOnlineArtworkForCurrentSong() },
            onDismiss = { showFullLyrics = false }
        )
    }

    if (showTrackMenuDialog) {
        QuickOpenTrackMenuDialog(
            allSongs = allSongs,
            allPlaylists = allPlaylists,
            currentSong = currentSong,
            isPlaying = isPlaying,
            skin = skin,
            onSongSelected = { song ->
                viewModel.playSong(song)
                showTrackMenuDialog = false
            },
            onPlaylistSelected = { playlist ->
                viewModel.playPlaylist(playlist)
                showTrackMenuDialog = false
            },
            onEnrichMetadata = { song ->
                viewModel.enrichSongMetadata(song)
            },
            onDismiss = { showTrackMenuDialog = false }
        )
    }

    // Skins Dialog
    if (showSkinDialog) {
        SkinSelectorDialog(
            currentSkin = skin,
            onSkinSelected = { selected ->
                viewModel.setSkin(selected)
                showSkinDialog = false
            },
            onDismiss = { showSkinDialog = false }
        )
    }

    // Equalizer & DSP Dialog
    if (showEqDialog) {
        EqualizerDialog(
            equalizerController = viewModel.equalizerController,
            currentVisualizerType = visualizerType,
            currentDiscDisplayMode = discDisplayMode,
            skin = skin,
            onVisualizerTypeSelected = { viewModel.setVisualizerType(it) },
            onDiscDisplayModeSelected = { viewModel.setDiscDisplayMode(it) },
            onDismiss = { showEqDialog = false }
        )
    }

    // Quit & Close Confirmation Dialog
    if (showQuitConfirmDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { showQuitConfirmDialog = false },
            modifier = Modifier.testTag("quit_confirmation_dialog"),
            containerColor = Color(0xFF1E2430),
            icon = {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Power Off",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "QUIT MIXTAPE PRO?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to close the player? This will stop background music playback and exit the app.",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                        viewModel.playerEngine.stop()
                        com.example.audio.RetroAudioPlaybackService.stopService(context)
                        showQuitConfirmDialog = false
                        (context as? android.app.Activity)?.finishAffinity()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_quit_button")
                ) {
                    Text(
                        text = "CONFIRM QUIT & CLOSE",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        RetroSoundEffects.playClick(RetroSoundEffects.SoundType.WINAMP_CLICK)
                        showQuitConfirmDialog = false
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("cancel_quit_button")
                ) {
                    Text(
                        text = "CANCEL",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun RetroTitleBar(
    skin: RetroSkin,
    onSkinClick: () -> Unit,
    onEqClick: () -> Unit,
    onQuitClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(skin.titleBarBackground)
            .border(1.dp, skin.chassisBorderColor)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title & Skin identifier with Retro Note Logo (skin name on its own line so it is never cut)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ra98_note_logo_1787601220471),
                    contentDescription = "Mixtape Pro logo",
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MIXTAPE PRO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = skin.titleBarTextColor,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = skin.name,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        color = skin.accentColor,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 1500, repeatDelayMillis = 2500)
                    )
                }
            }

            // Title bar quick buttons: Skins, Eq, and Quit with tactile feel and drop shadows
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RetroTactileButton(
                    onClick = { onSkinClick() },
                    shape = RoundedCornerShape(3.dp),
                    backgroundBrush = skin.buttonBackground,
                    borderUnpressed = skin.buttonBorderColor,
                    elevation = 3.dp,
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 3.dp),
                    soundType = RetroSoundEffects.SoundType.WINAMP_CLICK
                ) {
                    Text(
                        text = "Skins",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = skin.buttonIconColor
                    )
                }

                RetroTactileButton(
                    onClick = { onEqClick() },
                    shape = RoundedCornerShape(3.dp),
                    backgroundBrush = skin.buttonBackground,
                    borderUnpressed = skin.buttonBorderColor,
                    elevation = 3.dp,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                    soundType = RetroSoundEffects.SoundType.WINAMP_CLICK
                ) {
                    Text(
                        text = "Eq",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = skin.buttonIconColor
                    )
                }

                RetroTactileButton(
                    onClick = { onQuitClick() },
                    shape = RoundedCornerShape(3.dp),
                    backgroundColor = Color(0x33EF4444),
                    borderUnpressed = Color(0xFFEF4444).copy(alpha = 0.7f),
                    borderPressed = Color(0xFFEF4444),
                    accentHighlight = Color(0xFFEF4444),
                    elevation = 3.dp,
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 3.dp),
                    soundType = RetroSoundEffects.SoundType.WINAMP_CLICK,
                    modifier = Modifier.testTag("quit_close_player_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Quit App",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "QUIT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RetroNavigationBar(
    activeTab: Int,
    skin: RetroSkin,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        Triple(0, "Player", Icons.Default.PlayCircleFilled),
        Triple(1, "Library", Icons.Default.LibraryMusic),
        Triple(2, "Mixtapes", Icons.Default.QueueMusic),
        Triple(3, "Streaming", Icons.Default.SettingsInputAntenna),
        Triple(4, "DJ Studio", Icons.Default.Tune),
        Triple(5, "Cloud", Icons.Default.CloudUpload)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp),
        color = Color(0xFF141720),
        border = androidx.compose.foundation.BorderStroke(1.dp, skin.chassisBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(skin.chassisBackground)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (index, title, icon) ->
                val isSelected = activeTab == index

                // High-contrast button background, border, and text colors per skin
                val (btnBg, btnBorder, contentColor) = when {
                    // 1. OS 8 / OS '98 Style
                    skin.isWin98Style || skin.id == "win98_player" -> {
                        if (isSelected) {
                            Triple(Color(0xFF000080), Color(0xFF1084D0), Color(0xFFFFFFFF))
                        } else {
                            Triple(Color(0xFFD4D0C8), Color(0xFF808080), Color(0xFF000000))
                        }
                    }
                    // 2. Y2K Bubblegum Skin
                    skin.id == "y2k_bubblegum" -> {
                        if (isSelected) {
                            Triple(Color(0xFF700353), Color(0xFF70D6FF), Color(0xFFFFFFFF))
                        } else {
                            Triple(Color(0x40FFFFFF), Color(0xFFFFB3D1), Color(0xFFFFFFFF))
                        }
                    }
                    // 3. Bento Modern Skin
                    skin.id == "winamp_bento" -> {
                        if (isSelected) {
                            Triple(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFFFFFFFF))
                        } else {
                            Triple(Color(0xFF1E293B), Color(0xFF475569), Color(0xFFE2E8F0))
                        }
                    }
                    // 4. Acid Techno Skin
                    skin.id == "acid_techno" -> {
                        if (isSelected) {
                            Triple(Color(0xFFFACC15), Color(0xFFFDE047), Color(0xFF000000))
                        } else {
                            Triple(Color(0xFF1C1917), Color(0xFFCA8A04), Color(0xFFFDE047))
                        }
                    }
                    // 5. Cyber Matrix
                    skin.id == "cyber_matrix" -> {
                        if (isSelected) {
                            Triple(Color(0xFF14532D), Color(0xFF4ADE80), Color(0xFF86EFAC))
                        } else {
                            Triple(Color(0xFF0A140A), Color(0xFF166534), Color(0xFF4ADE80))
                        }
                    }
                    // 6. Hacker Terminal
                    skin.id == "hacker_terminal" -> {
                        if (isSelected) {
                            Triple(Color(0xFFB45309), Color(0xFFFDE68A), Color(0xFF000000))
                        } else {
                            Triple(Color(0xFF1A1208), Color(0xFF78350F), Color(0xFFFBBF24))
                        }
                    }
                    // 7. Hot Rod Flame
                    skin.id == "hot_rod_flame" -> {
                        if (isSelected) {
                            Triple(Color(0xFF991B1B), Color(0xFFF87171), Color(0xFFFEF08A))
                        } else {
                            Triple(Color(0xFF1C1917), Color(0xFF7F1D1D), Color(0xFFF1F5F9))
                        }
                    }
                    // 8. Discman ESP
                    skin.id == "discman_esp" -> {
                        if (isSelected) {
                            Triple(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFFFFFFFF))
                        } else {
                            Triple(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF0F172A))
                        }
                    }
                    // 9. Translucent iMacs
                    skin.id in listOf("imac_bondi", "lime_imac", "ruby_imac", "grape_imac") -> {
                        if (isSelected) {
                            Triple(Color(0x80000000), Color(0xFFFFFFFF), Color(0xFFFFFFFF))
                        } else {
                            Triple(Color(0x33000000), Color(0x66FFFFFF), Color(0xFFFFFFFF))
                        }
                    }
                    // General / Default skins
                    else -> {
                        if (isSelected) {
                            Triple(skin.accentColor.copy(alpha = 0.40f), skin.accentColor, Color(0xFFFFFFFF))
                        } else {
                            Triple(Color(0xFF1E2430), Color(0x35FFFFFF), Color(0xFFE2E8F0))
                        }
                    }
                }

                RetroTactileButton(
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    backgroundColor = btnBg,
                    borderUnpressed = btnBorder,
                    borderPressed = if (isSelected) btnBorder else skin.accentColor,
                    accentHighlight = if (isSelected) btnBorder else null,
                    elevation = if (isSelected) 1.5.dp else 3.5.dp,
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 5.dp),
                    soundType = RetroSoundEffects.SoundType.WINAMP_CLICK
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = title,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            color = contentColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                            letterSpacing = (-0.2).sp
                        )
                    }
                }
            }
        }
    }
}
