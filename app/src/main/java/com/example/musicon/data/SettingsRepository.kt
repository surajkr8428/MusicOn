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
    LIST, COMPACT_LIST, GRID, COMPACT_GRID
}

enum class PlayerImageMode {
    SQUARE, FULL_SCREEN, ROTATION
}

class SettingsRepository(val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LIBRARY_VIEW_MODE = stringPreferencesKey("library_view_mode")
        val PLAYER_IMAGE_MODE = stringPreferencesKey("player_image_mode")
        val PAUSE_ON_DETACH = booleanPreferencesKey("pause_on_detach")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        val SIMULTANEOUS_PLAYBACK = booleanPreferencesKey("simultaneous_playback")
        val CROSSFADE = booleanPreferencesKey("crossfade")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val ACCENT_COLOR = intPreferencesKey("accent_color")
        
        // Equalizer & FX
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_BANDS = stringPreferencesKey("eq_bands") // e.g. "0,0,0,0,0"
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val VIRTUALIZER = intPreferencesKey("virtualizer")
        
        // Utilities
        val SHAKE_TO_SKIP = booleanPreferencesKey("shake_to_skip")
        val CUSTOM_BG_URI = stringPreferencesKey("custom_bg_uri")
        val AUTO_THEME = booleanPreferencesKey("auto_theme")
        
        // Sorting
        val SONG_SORT_ORDER = stringPreferencesKey("song_sort_order")
        
        // Last Playback State
        val LAST_TRACK_ID = stringPreferencesKey("last_track_id")
        val LAST_POSITION = longPreferencesKey("last_position")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val themeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SPOTIFY_DARK.name
        ThemeMode.valueOf(themeName)
    }

    val libraryViewModeFlow: Flow<LibraryViewMode> = context.dataStore.data.map { preferences ->
        val modeName = preferences[PreferencesKeys.LIBRARY_VIEW_MODE] ?: LibraryViewMode.LIST.name
        LibraryViewMode.valueOf(modeName)
    }

    val playerImageModeFlow: Flow<PlayerImageMode> = context.dataStore.data.map { preferences ->
        val modeName = preferences[PreferencesKeys.PLAYER_IMAGE_MODE] ?: PlayerImageMode.SQUARE.name
        PlayerImageMode.valueOf(modeName)
    }

    val pauseOnDetachFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.PAUSE_ON_DETACH] ?: true }
    val keepScreenOnFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.KEEP_SCREEN_ON] ?: false }
    val showNotificationsFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.SHOW_NOTIFICATIONS] ?: true }
    val simultaneousPlaybackFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.SIMULTANEOUS_PLAYBACK] ?: false }
    val crossfadeFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.CROSSFADE] ?: false }
    val gaplessPlaybackFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.GAPLESS_PLAYBACK] ?: true }
    val accentColorFlow: Flow<Int> = context.dataStore.data.map { it[PreferencesKeys.ACCENT_COLOR] ?: 0xFFBB86FC.toInt() }

    val eqEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.EQ_ENABLED] ?: false }
    val eqBandsFlow: Flow<String> = context.dataStore.data.map { it[PreferencesKeys.EQ_BANDS] ?: "0,0,0,0,0" }
    val bassBoostFlow: Flow<Int> = context.dataStore.data.map { it[PreferencesKeys.BASS_BOOST] ?: 0 }
    val virtualizerFlow: Flow<Int> = context.dataStore.data.map { it[PreferencesKeys.VIRTUALIZER] ?: 0 }
    val shakeToSkipFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.SHAKE_TO_SKIP] ?: false }
    val customBgUriFlow: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.CUSTOM_BG_URI] }
    val autoThemeFlow: Flow<Boolean> = context.dataStore.data.map { it[PreferencesKeys.AUTO_THEME] ?: true }
    
    val songSortOrderFlow: Flow<String> = context.dataStore.data.map { it[PreferencesKeys.SONG_SORT_ORDER] ?: "NAME" }

    val lastTrackIdFlow: Flow<String?> = context.dataStore.data.map { it[PreferencesKeys.LAST_TRACK_ID] }
    val lastPositionFlow: Flow<Long> = context.dataStore.data.map { it[PreferencesKeys.LAST_POSITION] ?: 0L }

    suspend fun updateThemeMode(themeMode: ThemeMode) { context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = themeMode.name } }
    suspend fun updateLibraryViewMode(mode: LibraryViewMode) { context.dataStore.edit { it[PreferencesKeys.LIBRARY_VIEW_MODE] = mode.name } }
    suspend fun updatePlayerImageMode(mode: PlayerImageMode) { context.dataStore.edit { it[PreferencesKeys.PLAYER_IMAGE_MODE] = mode.name } }
    suspend fun updatePauseOnDetach(enabled: Boolean) { context.dataStore.edit { it[PreferencesKeys.PAUSE_ON_DETACH] = enabled } }
    suspend fun updateKeepScreenOn(enabled: Boolean) { context.dataStore.edit { it[PreferencesKeys.KEEP_SCREEN_ON] = enabled } }
    suspend fun updateShowNotifications(enabled: Boolean) { context.dataStore.edit { it[PreferencesKeys.SHOW_NOTIFICATIONS] = enabled } }
    suspend fun updateSimultaneousPlayback(enabled: Boolean) { context.dataStore.edit { it[PreferencesKeys.SIMULTANEOUS_PLAYBACK] = enabled } }
    suspend fun updateCrossfade(enabled: Boolean) { context.dataStore.edit { it[PreferencesKeys.CROSSFADE] = enabled } }
    suspend fun updateGaplessPlayback(enabled: Boolean) { context.dataStore.edit { it[PreferencesKeys.GAPLESS_PLAYBACK] = enabled } }
    suspend fun updateAccentColor(color: Int) { context.dataStore.edit { it[PreferencesKeys.ACCENT_COLOR] = color } }

    suspend fun updateEqEnabled(enabled: Boolean) { context.dataStore.edit { it[PreferencesKeys.EQ_ENABLED] = enabled } }
    suspend fun updateEqBands(bands: String) { context.dataStore.edit { it[PreferencesKeys.EQ_BANDS] = bands } }
    suspend fun updateBassBoost(level: Int) { context.dataStore.edit { it[PreferencesKeys.BASS_BOOST] = level } }
    suspend fun updateVirtualizer(level: Int) { context.dataStore.edit { it[PreferencesKeys.VIRTUALIZER] = level } }
    suspend fun updateShakeToSkip(enabled: Boolean) { context.dataStore.edit { it[PreferencesKeys.SHAKE_TO_SKIP] = enabled } }
    suspend fun updateCustomBgUri(uri: String?) { context.dataStore.edit { if (uri == null) it.remove(PreferencesKeys.CUSTOM_BG_URI) else it[PreferencesKeys.CUSTOM_BG_URI] = uri } }
    suspend fun updateAutoTheme(enabled: Boolean) { context.dataStore.edit { it[PreferencesKeys.AUTO_THEME] = enabled } }
    suspend fun updateSongSortOrder(order: String) { context.dataStore.edit { it[PreferencesKeys.SONG_SORT_ORDER] = order } }
    
    suspend fun updateLastPlaybackState(trackId: String?, position: Long) {
        context.dataStore.edit {
            if (trackId == null) it.remove(PreferencesKeys.LAST_TRACK_ID) else it[PreferencesKeys.LAST_TRACK_ID] = trackId
            it[PreferencesKeys.LAST_POSITION] = position
        }
    }
}
