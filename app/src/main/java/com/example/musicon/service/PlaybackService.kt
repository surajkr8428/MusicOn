package com.example.musicon.service

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.musicon.data.SettingsRepository
import com.example.musicon.data.remote.CloudStorageManager
import com.example.musicon.logic.audio.AudioEffectsManager
import com.example.musicon.widget.MusicWidgetProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var effectsManager: AudioEffectsManager? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var cloudManager: CloudStorageManager
    private var positionSavingJob: Job? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val settingsRepository = SettingsRepository(applicationContext)
        cloudManager = CloudStorageManager(applicationContext)
        
        val httpDataSourceFactory = object : HttpDataSource.Factory {
            private val defaultFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)

            override fun createDataSource(): HttpDataSource {
                val dataSource = defaultFactory.createDataSource()
                // Inject token for Google Drive requests
                try {
                    runBlocking {
                        val token = cloudManager.getAccessTokenAsync()
                        if (token != null) {
                            dataSource.setRequestProperty("Authorization", "Bearer $token")
                            android.util.Log.d("MusicOnService", "Injected token into request")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MusicOnService", "Failed to inject token", e)
                }
                return dataSource
            }

            override fun setDefaultRequestProperties(defaultRequestProperties: MutableMap<String, String>): HttpDataSource.Factory {
                defaultFactory.setDefaultRequestProperties(defaultRequestProperties)
                return this
            }
        }
        
        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)
        
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory))
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.pauseAtEndOfMediaItems = false // Gapless
        
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MusicOnService", "ExoPlayer Error: ${error.errorCodeName}", error)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                android.util.Log.d("MusicOnService", "Playback State: $playbackState")
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
                    // Save state on any significant event
                    val trackId = player.currentMediaItem?.mediaId
                    if (trackId != null) {
                        serviceScope.launch {
                            settingsRepository.updateLastPlaybackState(trackId, player.currentPosition)
                        }
                    }

                    if (effectsManager == null) {
                        val sessionId = (player as? ExoPlayer)?.audioSessionId ?: 0
                        if (sessionId != 0) {
                            effectsManager = AudioEffectsManager(sessionId)
                            observeEffectsSettings(settingsRepository)
                        }
                    }
                    MusicWidgetProvider.updateAllWidgets(applicationContext)
                }
            }
        })
        
        val intent = Intent(this, com.example.musicon.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
            
        startPositionSaving(player, settingsRepository)
    }

    private fun startPositionSaving(player: Player, settings: SettingsRepository) {
        positionSavingJob?.cancel()
        positionSavingJob = serviceScope.launch {
            while (isActive) {
                val trackId = player.currentMediaItem?.mediaId
                if (trackId != null) {
                    settings.updateLastPlaybackState(trackId, player.currentPosition)
                }
                delay(2000) // Save every 2 seconds
            }
        }
    }

    private fun observeEffectsSettings(settings: SettingsRepository) {
        serviceScope.launch {
            combine(
                settings.eqEnabledFlow,
                settings.eqBandsFlow,
                settings.bassBoostFlow,
                settings.virtualizerFlow
            ) { enabled, bands, bass, virt ->
                Triple(enabled, bands, bass to virt)
            }.collect { (enabled, bandsStr, fx) ->
                effectsManager?.let { manager ->
                    manager.setEnabled(enabled)
                    if (enabled) {
                        val bands = bandsStr.split(",").mapNotNull { it.toShortOrNull() }
                        bands.forEachIndexed { index, level ->
                            manager.setBandLevel(index.toShort(), level)
                        }
                        manager.setBassBoost(fx.first)
                        manager.setVirtualizer(fx.second)
                    }
                }
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        effectsManager?.release()
        effectsManager = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Do not stopSelf() here to keep the service alive in the background
        super.onTaskRemoved(rootIntent)
    }
}
