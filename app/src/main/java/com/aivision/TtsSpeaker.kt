package com.aivision

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsSpeaker(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context, this)
    private val pending = mutableListOf<String>()
    private var ready = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("ru", "RU")
            ready = true
            pending.forEach { tts.speak(it, TextToSpeech.QUEUE_ADD, null, null) }
            pending.clear()
        }
    }

    fun speak(text: String) {
        if (ready) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            pending.add(text)
        }
    }

    fun shutdown() = tts.shutdown()
}
