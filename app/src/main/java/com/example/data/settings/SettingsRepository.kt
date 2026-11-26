package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.QualityLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "sertao_settings")

data class GameSettings(
    val musicVolume: Float = 0.7f,
    val sfxVolume: Float = 0.85f,
    val voiceVolume: Float = 0.9f,
    val qualityLevel: QualityLevel = QualityLevel.MEDIA,
    val subtitlesEnabled: Boolean = true,
    val showMinimap: Boolean = true,
    val cameraSensitivity: Float = 1.0f
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val SFX_VOLUME = floatPreferencesKey("sfx_volume")
        val VOICE_VOLUME = floatPreferencesKey("voice_volume")
        val QUALITY = stringPreferencesKey("quality_level")
        val SUBTITLES = booleanPreferencesKey("subtitles_enabled")
        val MINIMAP = booleanPreferencesKey("show_minimap")
        val SENSITIVITY = floatPreferencesKey("camera_sensitivity")
    }

    val settingsFlow: Flow<GameSettings> = context.settingsDataStore.data.map { prefs ->
        GameSettings(
            musicVolume = prefs[Keys.MUSIC_VOLUME] ?: 0.7f,
            sfxVolume = prefs[Keys.SFX_VOLUME] ?: 0.85f,
            voiceVolume = prefs[Keys.VOICE_VOLUME] ?: 0.9f,
            qualityLevel = QualityLevel.entries.firstOrNull { it.name == prefs[Keys.QUALITY] }
                ?: QualityLevel.MEDIA,
            subtitlesEnabled = prefs[Keys.SUBTITLES] ?: true,
            showMinimap = prefs[Keys.MINIMAP] ?: true,
            cameraSensitivity = prefs[Keys.SENSITIVITY] ?: 1.0f
        )
    }

    suspend fun updateSettings(transform: (GameSettings) -> GameSettings) {
        context.settingsDataStore.edit { prefs ->
            val current = GameSettings(
                musicVolume = prefs[Keys.MUSIC_VOLUME] ?: 0.7f,
                sfxVolume = prefs[Keys.SFX_VOLUME] ?: 0.85f,
                voiceVolume = prefs[Keys.VOICE_VOLUME] ?: 0.9f,
                qualityLevel = QualityLevel.entries.firstOrNull { it.name == prefs[Keys.QUALITY] }
                    ?: QualityLevel.MEDIA,
                subtitlesEnabled = prefs[Keys.SUBTITLES] ?: true,
                showMinimap = prefs[Keys.MINIMAP] ?: true,
                cameraSensitivity = prefs[Keys.SENSITIVITY] ?: 1.0f
            )
            val updated = transform(current)
            prefs[Keys.MUSIC_VOLUME] = updated.musicVolume.coerceIn(0f, 1f)
            prefs[Keys.SFX_VOLUME] = updated.sfxVolume.coerceIn(0f, 1f)
            prefs[Keys.VOICE_VOLUME] = updated.voiceVolume.coerceIn(0f, 1f)
            prefs[Keys.QUALITY] = updated.qualityLevel.name
            prefs[Keys.SUBTITLES] = updated.subtitlesEnabled
            prefs[Keys.MINIMAP] = updated.showMinimap
            prefs[Keys.SENSITIVITY] = updated.cameraSensitivity.coerceIn(0.4f, 2.0f)
        }
    }
}
