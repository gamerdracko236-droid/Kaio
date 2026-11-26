package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.data.model.CombatStyle
import com.example.data.model.SpeakerId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SfxType {
    SLASH,
    HEAVY_HIT,
    PARRY,
    DODGE,
    SKILL_CHAMA,
    SKILL_TEMPESTADE,
    SKILL_CAATINGA,
    SKILL_AGUAS,
    SKILL_LUA,
    HEAL,
    LEVEL_UP,
    UI_CLICK,
    ENEMY_HIT
}

class SertaoAudioEngine(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false

    @Volatile
    private var musicVolume: Float = 0.7f

    @Volatile
    private var sfxVolume: Float = 0.85f

    @Volatile
    private var voiceVolume: Float = 0.9f

    @Volatile
    private var currentTheme: String = "menu"

    private var musicJob: Job? = null

    init {
        try {
            tts = TextToSpeech(appContext, this)
        } catch (e: Exception) {
            Log.w("SertaoAudio", "TTS unavailable: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ptBr = Locale("pt", "BR")
            val result = tts?.setLanguage(ptBr)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ttsReady) {
                // Fallback to generic Portuguese if pt-BR pack not explicitly flagged
                val ptRes = tts?.setLanguage(Locale("pt"))
                ttsReady = ptRes != TextToSpeech.LANG_MISSING_DATA && ptRes != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun updateVolumes(music: Float, sfx: Float, voice: Float) {
        musicVolume = music.coerceIn(0f, 1f)
        sfxVolume = sfx.coerceIn(0f, 1f)
        voiceVolume = voice.coerceIn(0f, 1f)
    }

    fun startMusic(theme: String) {
        if (currentTheme == theme && musicJob?.isActive == true) return
        currentTheme = theme
        if (musicJob?.isActive == true) return

        musicJob = scope.launch {
            val sampleRate = 22050
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (e: Exception) {
                Log.w("SertaoAudio", "AudioTrack music init failed: ${e.message}")
                return@launch
            }

            try {
                track.play()
                val chunkSamples = 1024
                val buffer = ShortArray(chunkSamples)
                var sampleIndex = 0L

                // Escala Nordestina (D Mixolídio / Lídio b7: D4, E4, F#4, G4, A4, B4, C5, D5)
                val baiaoScale = doubleArrayOf(293.66, 329.63, 369.99, 392.00, 440.00, 493.88, 523.25, 587.33)
                val vilaPattern = intArrayOf(0, 2, 4, 5, 4, 2, 6, 4, 0, 4, 6, 7, 6, 4, 2, 0)
                val combatPattern = intArrayOf(0, 4, 6, 4, 7, 6, 4, 2, 0, 2, 4, 6, 7, 5, 4, 2)
                val menuPattern = intArrayOf(0, 4, 2, 5, 4, 6, 4, 2, 0, 3, 4, 5, 4, 2, 1, 0)

                while (isActive) {
                    val vol = musicVolume
                    if (vol <= 0.01f) {
                        buffer.fill(0)
                        track.write(buffer, 0, chunkSamples)
                        sampleIndex += chunkSamples
                        continue
                    }

                    val themeSnap = currentTheme
                    val isFast = themeSnap == "combate" || themeSnap == "chefe_final"
                    val isMenu = themeSnap == "menu" || themeSnap == "creditos"
                    val stepDurationSec = when {
                        themeSnap == "chefe_final" -> 0.16
                        isFast -> 0.19
                        isMenu -> 0.34
                        else -> 0.25
                    }
                    val pattern = when {
                        isFast -> combatPattern
                        isMenu -> menuPattern
                        else -> vilaPattern
                    }

                    for (i in 0 until chunkSamples) {
                        val t = (sampleIndex + i).toDouble() / sampleRate
                        val stepFloat = t / stepDurationSec
                        val stepIdx = stepFloat.toInt()
                        val localT = (stepFloat - stepIdx) * stepDurationSec

                        val noteFreq = baiaoScale[pattern[stepIdx % pattern.size]]
                        // Viola / Rabeca lead voice
                        val leadEnv = exp(-localT * if (isFast) 6.5 else 4.2)
                        val leadWave = if (isFast) {
                            // Rabeca expressiva (harmonicos)
                            (sin(2.0 * PI * noteFreq * t) * 0.6 +
                                sin(2.0 * PI * noteFreq * 2.0 * t) * 0.25 +
                                sin(2.0 * PI * noteFreq * 3.0 * t) * 0.12) * leadEnv
                        } else {
                            // Viola caipira / Sanfona suave
                            (sin(2.0 * PI * noteFreq * t) * 0.72 +
                                sin(2.0 * PI * noteFreq * 2.0 * t) * 0.22) * leadEnv
                        }

                        // Bordao de viola / Sanfona baixo (D3 = 146.83 Hz ou A2 = 110 Hz)
                        val bassFreq = if ((stepIdx / 4) % 2 == 0) 146.83 else 110.0
                        val bassWave = sin(2.0 * PI * bassFreq * t) * 0.22

                        // Zabumba (bumbo grave nas batidas 0 e 2)
                        var zabumba = 0.0
                        if (!isMenu && (stepIdx % 4 == 0 || stepIdx % 4 == 2)) {
                            val zEnv = exp(-localT * 24.0)
                            val zFreq = 58.0 + 42.0 * exp(-localT * 35.0)
                            zabumba = sin(2.0 * PI * zFreq * localT) * zEnv * 0.45
                        }

                        // Triangulo nordestino (brilho metalico nos contratempos)
                        var triangulo = 0.0
                        if (!isMenu && (stepIdx % 2 == 1)) {
                            val triEnv = exp(-localT * 28.0)
                            triangulo = (sin(2.0 * PI * 3200.0 * t) + sin(2.0 * PI * 4750.0 * t) * 0.6) * triEnv * 0.08
                        }

                        val mixed = (leadWave * 0.36 + bassWave + zabumba + triangulo) * vol * 0.55
                        buffer[i] = (mixed.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                    }
                    sampleIndex += chunkSamples
                    track.write(buffer, 0, chunkSamples)
                }
            } catch (e: Exception) {
                Log.w("SertaoAudio", "Music loop stopped: ${e.message}")
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    fun speakLine(speaker: SpeakerId, text: String) {
        if (!ttsReady || voiceVolume <= 0.02f) return
        try {
            tts?.setPitch(speaker.pitch)
            tts?.setSpeechRate(speaker.speechRate)
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceVolume)
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "line_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.w("SertaoAudio", "TTS speak failed: ${e.message}")
        }
    }

    fun stopVoice() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }

    fun playStyleSkillSfx(style: CombatStyle) {
        when (style) {
            CombatStyle.CHAMA -> playSfx(SfxType.SKILL_CHAMA)
            CombatStyle.TEMPESTADE -> playSfx(SfxType.SKILL_TEMPESTADE)
            CombatStyle.CAATINGA -> playSfx(SfxType.SKILL_CAATINGA)
            CombatStyle.AGUAS -> playSfx(SfxType.SKILL_AGUAS)
            CombatStyle.LUA -> playSfx(SfxType.SKILL_LUA)
        }
    }

    fun playSfx(type: SfxType) {
        val vol = sfxVolume
        if (vol <= 0.02f) return
        scope.launch {
            val sampleRate = 22050
            val durationSec = when (type) {
                SfxType.UI_CLICK -> 0.06
                SfxType.DODGE, SfxType.SLASH -> 0.14
                SfxType.PARRY -> 0.28
                SfxType.LEVEL_UP, SfxType.HEAL -> 0.35
                else -> 0.26
            }
            val count = (sampleRate * durationSec).toInt()
            val pcm = ShortArray(count)
            for (i in 0 until count) {
                val t = i.toDouble() / sampleRate
                val progress = i.toDouble() / count
                val env = (1.0 - progress) * exp(-progress * 3.0)
                val sample = when (type) {
                    SfxType.SLASH -> {
                        val f = 780.0 - 520.0 * progress
                        sin(2.0 * PI * f * t) * 0.5 + (Math.random() - 0.5) * 0.45
                    }
                    SfxType.HEAVY_HIT, SfxType.ENEMY_HIT -> {
                        val f = 150.0 - 95.0 * progress
                        sin(2.0 * PI * f * t) * 0.8 + (Math.random() - 0.5) * 0.25
                    }
                    SfxType.PARRY -> {
                        sin(2.0 * PI * 1480.0 * t) * 0.45 + sin(2.0 * PI * 2240.0 * t) * 0.35
                    }
                    SfxType.DODGE -> (Math.random() - 0.5) * 0.55 * sin(progress * PI)
                    SfxType.SKILL_CHAMA -> {
                        sin(2.0 * PI * (320.0 + 280.0 * progress) * t) * 0.5 + (Math.random() - 0.5) * 0.4
                    }
                    SfxType.SKILL_TEMPESTADE -> {
                        sin(2.0 * PI * (920.0 - 450.0 * progress) * t) * 0.65 + sin(2.0 * PI * 1840.0 * t) * 0.25
                    }
                    SfxType.SKILL_CAATINGA -> sin(2.0 * PI * (120.0 + 60.0 * (1 - progress)) * t) * 0.85
                    SfxType.SKILL_AGUAS -> sin(2.0 * PI * (440.0 + 180.0 * sin(progress * PI * 2)) * t) * 0.55
                    SfxType.SKILL_LUA -> sin(2.0 * PI * (660.0 + 320.0 * progress) * t) * 0.5
                    SfxType.HEAL -> sin(2.0 * PI * (523.25 + 260.0 * progress) * t) * 0.55
                    SfxType.LEVEL_UP -> {
                        val f = if (progress < 0.33) 523.25 else if (progress < 0.66) 659.25 else 783.99
                        sin(2.0 * PI * f * t) * 0.6
                    }
                    SfxType.UI_CLICK -> sin(2.0 * PI * 980.0 * t) * 0.35
                }
                pcm[i] = (sample * env * vol * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playOneShotPcm(sampleRate, pcm)
        }
    }

    private fun playOneShotPcm(sampleRate: Int, pcm: ShortArray) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.setNotificationMarkerPosition(pcm.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) {
                    try {
                        t?.release()
                    } catch (_: Exception) {
                    }
                }
                override fun onPeriodicNotification(t: AudioTrack?) {}
            })
            track.play()
        } catch (_: Exception) {
        }
    }

    fun release() {
        stopMusic()
        stopVoice()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }
        scope.cancel()
    }
}
