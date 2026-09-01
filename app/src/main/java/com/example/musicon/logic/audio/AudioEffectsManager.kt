package com.example.musicon.logic.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log

class AudioEffectsManager(audioSessionId: Int) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    init {
        try {
            if (audioSessionId != 0) {
                equalizer = Equalizer(0, audioSessionId)
                bassBoost = BassBoost(0, audioSessionId)
                virtualizer = Virtualizer(0, audioSessionId)
                Log.d("AudioEffectsManager", "Effects initialized for session: $audioSessionId")
            }
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Failed to initialize audio effects", e)
        }
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
    }

    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Error setting band level", e)
        }
    }

    fun setBassBoost(level: Int) {
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(level.toShort())
            }
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Error setting bass boost", e)
        }
    }

    fun setVirtualizer(level: Int) {
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(level.toShort())
            }
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Error setting virtualizer", e)
        }
    }

    fun getBandCount(): Short = equalizer?.numberOfBands ?: 0
    fun getBandLevelRange(): ShortArray = equalizer?.bandLevelRange ?: shortArrayOf(0, 0)
    fun getCenterFreq(band: Short): Int = equalizer?.getCenterFreq(band) ?: 0

    fun release() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}
