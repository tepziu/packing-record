package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.LinkedList
import java.util.Locale

class VoiceFeedbackManager(context: Context) {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var toneGenerator: ToneGenerator? = null
    private val pendingQueue = LinkedList<String>()

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            } catch (e2: Exception) {
                Log.e("VoiceFeedback", "ToneGenerator init failed", e2)
            }
        }

        initializeTts()
    }

    private fun initializeTts() {
        try {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val viLocale = Locale("vi", "VN")
                    val result = tts?.setLanguage(viLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        val viGeneral = Locale("vi")
                        val resultGeneral = tts?.setLanguage(viGeneral)
                        if (resultGeneral == TextToSpeech.LANG_MISSING_DATA || resultGeneral == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.w("VoiceFeedback", "Vietnamese TTS voice not supported, fallback to system default voice")
                            tts?.language = Locale.getDefault()
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        tts?.setAudioAttributes(audioAttributes)
                    }

                    tts?.setPitch(1.0f)
                    tts?.setSpeechRate(1.05f)

                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {}
                        override fun onError(utteranceId: String?) {}
                    })

                    isTtsReady = true
                    Log.d("VoiceFeedback", "TTS Initialized successfully.")

                    // Flush any queued speech requests
                    synchronized(pendingQueue) {
                        while (pendingQueue.isNotEmpty()) {
                            val text = pendingQueue.poll()
                            if (text != null) {
                                executeSpeak(text)
                            }
                        }
                    }
                } else {
                    Log.w("VoiceFeedback", "TTS initialization returned status: $status")
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceFeedback", "TTS init exception", e)
        }
    }

    fun speakStartRecording() {
        vibrate(80)
        playBeepTone(ToneGenerator.TONE_PROP_ACK, 250)
        speak("Bắt đầu quay")
    }

    fun speakStopRecording() {
        vibrate(150)
        playDoubleBeep()
        speak("Kết thúc")
    }

    fun speakWrongOrderCode() {
        vibratePattern(longArrayOf(0, 100, 80, 200))
        playBeepTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 350)
        speak("Sai mã đơn hàng")
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.e("VoiceFeedback", "Vibration error", e)
        }
    }

    private fun vibratePattern(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.e("VoiceFeedback", "Vibration pattern error", e)
        }
    }

    private fun playDoubleBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
        } catch (e: Exception) {
            Log.e("VoiceFeedback", "Beep error", e)
        }
    }

    private fun playBeepTone(tone: Int, durationMs: Int) {
        try {
            toneGenerator?.startTone(tone, durationMs)
        } catch (e: Exception) {
            Log.e("VoiceFeedback", "Tone error", e)
        }
    }

    private fun speak(text: String) {
        if (!isTtsReady || tts == null) {
            synchronized(pendingQueue) {
                pendingQueue.add(text)
            }
            return
        }

        executeSpeak(text)
    }

    private fun executeSpeak(text: String) {
        try {
            val utteranceId = "VOICE_${System.currentTimeMillis()}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val params = Bundle().apply {
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            } else {
                @Suppress("DEPRECATION")
                val params = HashMap<String, String>().apply {
                    put(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC.toString())
                    put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }
                @Suppress("DEPRECATION")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
            }
        } catch (e: Exception) {
            Log.e("VoiceFeedback", "Execute speak failed", e)
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            toneGenerator?.release()
        } catch (e: Exception) {
            Log.e("VoiceFeedback", "Release error", e)
        }
    }
}
