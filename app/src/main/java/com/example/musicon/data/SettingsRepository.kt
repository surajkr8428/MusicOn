package com.example.musicon.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.musicon.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class LibraryViewMode {
    LIST,
    COMPACT_LIST,
    GRID,
    COMPACT_GRID
}

class SettingsRepository(val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LIBRARY_VIEW_MODE = stringPreferencesKey("library_view_mode")
        val PAUSE_ON_DETACH = booleanPreferencesKey("pause_on_detach")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        val SIMULTANEOUS_PLAYBACK = booleanPreferencesKey("simultaneous_playback")
        val CROSSFADE = booleanPreferencesKey("crossfade")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SPOTIFY_DARK.name
            ThemeMode.valueOf(themeName)
        }

    val libraryViewModeFlow: Flow<LibraryViewMode> = context.dataStore.data
        .map { preferences ->
            val modeName = preferences[PreferencesKeys.LIBRARY_VIEW_MODE] ?: LibraryViewMode.LIST.name
            LibraryViewMode.valueOf(modeName)
        }

    val pauseOnDetachFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.PAUSE_ON_DETACH] ?: true }
    val keepScreenOnFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.KEEP_SCREEN_ON] ?: false }
    val showNotificationsFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.SHOW_NOTIFICATIONS] ?: true }
    val simultaneousPlaybackFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.SIMULTANEOUS_PLAYBACK] ?: false }
    val crossfadeFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.CROSSFADE] ?: false }
    val gaplessPlaybackFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.GAPLESS_PLAYBACK] ?: true }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateLibraryViewMode(mode: LibraryViewMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LIBRARY_VIEW_MODE] = mode.name
        }
    }

    suspend fun updatePauseOnDetach(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.PAUSE_ON_DETACH] = enabled }
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun updateShowNotifications(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_NOTIFICATIONS] = enabled }
    }

    suspend fun updateSimultaneousPlayback(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SIMULTANEOUS_PLAYBACK] = enabled }
    }

    suspend fun updateCrossfade(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.CROSSFADE] = enabled }
    }

    suspend fun updateGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.GAPLESS_PLAYBACK] = enabled }
    }
}
