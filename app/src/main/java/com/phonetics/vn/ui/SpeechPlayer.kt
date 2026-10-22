package com.phonetics.vn.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Thin wrapper over Android's on-device [TextToSpeech], used to play the target utterance
 * for AUDIO/ORAL questions. Speech stays on the device (no network). If no Vietnamese voice
 * is installed, [available] is false and the UI falls back to self-grading.
 */
class SpeechPlayer(context: Context) : TextToSpeech.OnInitListener {

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            val r = tts.setLanguage(Locale("vi", "VN"))
            ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    fun available(): Boolean = ready

    fun speak(text: String) {
        if (ready && text.isNotBlank()) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun shutdown() = tts.shutdown()
}
