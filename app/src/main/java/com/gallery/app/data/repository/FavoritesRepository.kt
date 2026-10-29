package com.gallery.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gallery_prefs")

/**
 * Repository for persisting user preferences (e.g., favorites).
 */
class FavoritesRepository(private val context: Context) {

    private val FAVORITES_KEY = stringSetPreferencesKey("favorites")

    val favoriteIds: Flow<Set<String>> = context.dataStore.data
        .map { prefs -> prefs[FAVORITES_KEY] ?: emptySet() }

    suspend fun addFavorite(mediaId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY] ?: emptySet()
            prefs[FAVORITES_KEY] = current + mediaId
        }
    }

    suspend fun removeFavorite(mediaId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITES_KEY] ?: emptySet()
            prefs[FAVORITES_KEY] = current - mediaId
        }
    }

    suspend fun toggleFavorite(mediaId: String, isFavorite: Boolean) {
        if (isFavorite) removeFavorite(mediaId) else addFavorite(mediaId)
    }
}
