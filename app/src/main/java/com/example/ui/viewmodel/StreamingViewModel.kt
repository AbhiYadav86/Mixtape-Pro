package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Song
import com.example.data.repository.ArchiveItem
import com.example.data.repository.StreamingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** State for the Streaming tab (Audius + Internet Archive). Radio state lives in MainViewModel. */
class StreamingViewModel(application: Application) : AndroidViewModel(application) {

    val repository = StreamingRepository(application)

    // ---- Audius ----
    private val _audiusTracks = MutableStateFlow<List<Song>>(emptyList())
    val audiusTracks: StateFlow<List<Song>> = _audiusTracks.asStateFlow()

    private val _audiusLoading = MutableStateFlow(false)
    val audiusLoading: StateFlow<Boolean> = _audiusLoading.asStateFlow()

    private val _audiusGenre = MutableStateFlow("All")
    val audiusGenre: StateFlow<String> = _audiusGenre.asStateFlow()

    private val _audiusError = MutableStateFlow<String?>(null)
    val audiusError: StateFlow<String?> = _audiusError.asStateFlow()

    // ---- Internet Archive ----
    private val _archiveItems = MutableStateFlow<List<ArchiveItem>>(emptyList())
    val archiveItems: StateFlow<List<ArchiveItem>> = _archiveItems.asStateFlow()

    private val _archiveLoading = MutableStateFlow(false)
    val archiveLoading: StateFlow<Boolean> = _archiveLoading.asStateFlow()

    private val _archivePreset = MutableStateFlow<String?>(StreamingRepository.ARCHIVE_PRESETS.keys.first())
    val archivePreset: StateFlow<String?> = _archivePreset.asStateFlow()

    private val _openArchiveItem = MutableStateFlow<ArchiveItem?>(null)
    val openArchiveItem: StateFlow<ArchiveItem?> = _openArchiveItem.asStateFlow()

    private val _archiveTracks = MutableStateFlow<List<Song>>(emptyList())
    val archiveTracks: StateFlow<List<Song>> = _archiveTracks.asStateFlow()

    private val _archiveError = MutableStateFlow<String?>(null)
    val archiveError: StateFlow<String?> = _archiveError.asStateFlow()

    private var audiusJob: Job? = null
    private var archiveJob: Job? = null

    fun loadAudiusIfEmpty() {
        if (_audiusTracks.value.isEmpty() && !_audiusLoading.value) loadAudiusTrending(_audiusGenre.value)
    }

    fun loadAudiusTrending(genre: String) {
        _audiusGenre.value = genre
        audiusJob?.cancel()
        audiusJob = viewModelScope.launch {
            _audiusLoading.value = true
            _audiusError.value = null
            val result = repository.audiusTrending(genre)
            _audiusTracks.value = result
            if (result.isEmpty()) _audiusError.value = "Couldn't reach Audius. Check your internet connection."
            _audiusLoading.value = false
        }
    }

    fun searchAudius(query: String) {
        audiusJob?.cancel()
        audiusJob = viewModelScope.launch {
            _audiusLoading.value = true
            _audiusError.value = null
            val result = repository.audiusSearch(query)
            _audiusTracks.value = result
            if (result.isEmpty()) _audiusError.value = "No Audius tracks found for \"$query\"."
            _audiusLoading.value = false
        }
    }

    fun loadArchiveIfEmpty() {
        if (_archiveItems.value.isEmpty() && !_archiveLoading.value) {
            selectArchivePreset(_archivePreset.value ?: StreamingRepository.ARCHIVE_PRESETS.keys.first())
        }
    }

    fun selectArchivePreset(name: String) {
        val query = StreamingRepository.ARCHIVE_PRESETS[name] ?: return
        _archivePreset.value = name
        runArchiveSearch(query, isPreset = true)
    }

    fun searchArchive(text: String) {
        _archivePreset.value = null
        runArchiveSearch(text, isPreset = false)
    }

    private fun runArchiveSearch(query: String, isPreset: Boolean) {
        _openArchiveItem.value = null
        archiveJob?.cancel()
        archiveJob = viewModelScope.launch {
            _archiveLoading.value = true
            _archiveError.value = null
            val items = repository.archiveSearch(query, isPreset)
            _archiveItems.value = items
            if (items.isEmpty()) _archiveError.value = "Nothing found on archive.org (or no connection)."
            _archiveLoading.value = false
        }
    }

    fun openArchiveItem(item: ArchiveItem) {
        _openArchiveItem.value = item
        _archiveTracks.value = emptyList()
        archiveJob?.cancel()
        archiveJob = viewModelScope.launch {
            _archiveLoading.value = true
            _archiveError.value = null
            val tracks = repository.archiveTracks(item)
            _archiveTracks.value = tracks
            if (tracks.isEmpty()) _archiveError.value = "This item has no playable MP3 files."
            _archiveLoading.value = false
        }
    }

    fun closeArchiveItem() {
        _openArchiveItem.value = null
        _archiveTracks.value = emptyList()
    }

    fun download(song: Song): Boolean = repository.downloadForOffline(song)
}
