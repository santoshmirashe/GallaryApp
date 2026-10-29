package com.gallery.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.app.data.model.Album
import com.gallery.app.data.model.MediaItem
import com.gallery.app.data.repository.FavoritesRepository
import com.gallery.app.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GalleryUiState(
    val isLoading: Boolean = true,
    val allMedia: List<MediaItem> = emptyList(),
    val albums: List<Album> = emptyList(),
    val filteredMedia: List<MediaItem> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val error: String? = null
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaRepository = MediaRepository(application)
    private val favoritesRepository = FavoritesRepository(application)

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState

    private val _searchQuery = MutableStateFlow("")
    private val _allMedia = MutableStateFlow<List<MediaItem>>(emptyList())

    val filteredMedia: StateFlow<List<MediaItem>> = combine(
        _allMedia, _searchQuery
    ) { media, query ->
        if (query.isBlank()) media
        else media.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<Set<String>> = favoritesRepository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val media = mediaRepository.getAllMedia()
                val albums = mediaRepository.getAlbums()
                _allMedia.value = media
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allMedia = media,
                    albums = albums
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load media: ${e.message}"
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleFavorite(mediaId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(mediaId, isFavorite)
        }
    }

    fun deleteMedia(item: MediaItem, onRequirePermission: (Uri) -> Unit) {
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+: need user permission via system dialog
                onRequirePermission(Uri.parse(item.uri))
            } else {
                mediaRepository.deleteMedia(item)
                loadMedia()
            }
        }
    }

    fun getAlbumMedia(bucketId: Long): List<MediaItem> {
        return _allMedia.value.filter { it.bucketId == bucketId }
    }

    fun getFavoriteMedia(): List<MediaItem> {
        return _allMedia.value.filter {
            favoriteIds.value.contains(it.id.toString())
        }
    }
}
