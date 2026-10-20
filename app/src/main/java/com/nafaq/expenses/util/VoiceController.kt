package com.nafaq.expenses.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data class Partial(val text: String) : VoiceState
    data class Final(val text: String) : VoiceState
    data class Error(val message: String) : VoiceState
}

class VoiceController(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    var state: VoiceState by mutableStateOf(VoiceState.Idle)
        private set

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            state = VoiceState.Listening
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            state = VoiceState.Error(errorMessage(error))
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            state = if (text.isBlank()) VoiceState.Error("ما فهمت شيئاً، حاول مرة أخرى") else VoiceState.Final(text)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) state = VoiceState.Partial(text)
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        if (!isAvailable) {
            state = VoiceState.Error("التعرف على الصوت غير متوفر على هذا الجهاز")
            return
        }
        val active = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(listener)
            recognizer = it
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        state = VoiceState.Listening
        try {
            active.startListening(intent)
        } catch (error: Throwable) {
            state = VoiceState.Error("تعذّر بدء التسجيل الصوتي")
        }
    }

    fun stop() {
        recognizer?.stopListening()
        state = VoiceState.Idle
    }

    fun reset() {
        state = VoiceState.Idle
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH -> "ما فهمت شيئاً، حاول مرة أخرى"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "انتهت مهلة الكلام"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يلزم إذن الميكروفون"
        SpeechRecognizer.ERROR_NETWORK -> "مشكلة في الاتصال بالشبكة"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "انتهت مهلة الشبكة"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "محرك التعرف مشغول، حاول بعد قليل"
        SpeechRecognizer.ERROR_SERVER -> "خطأ في خادم التعرف على الصوت"
        SpeechRecognizer.ERROR_CLIENT -> "خطأ في محرك التعرف على الصوت"
        else -> "تعذّر التعرف على الصوت"
    }
}
