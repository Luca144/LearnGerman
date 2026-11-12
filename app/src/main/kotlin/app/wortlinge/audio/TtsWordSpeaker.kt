package app.wortlinge.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsWordSpeaker(context: Context) : WordSpeaker {
    private val _state = MutableStateFlow(SpeakerState.STARTING)
    override val state: StateFlow<SpeakerState> = _state.asStateFlow()

    private val _playing = MutableStateFlow<String?>(null)
    override val playing: StateFlow<String?> = _playing.asStateFlow()

    private var pending: Triple<String, String, Boolean>? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        onInit(status)
    }

    private fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            _state.value = SpeakerState.UNAVAILABLE
            return
        }
        val result = tts.setLanguage(Locale.GERMANY)
        _state.value = when (result) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> SpeakerState.MISSING_VOICE
            else -> SpeakerState.READY
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) { _playing.value = utteranceId.substringBefore('#') }
            override fun onDone(utteranceId: String) { _playing.value = null }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) { _playing.value = null }
            override fun onError(utteranceId: String, errorCode: Int) { _playing.value = null }
        })
        pending?.let { (id, text, slow) -> speak(id, text, slow) }
        pending = null
    }

    /** Re-checks the voice, e.g. after the player returns from installing it. */
    override fun refresh() {
        if (_state.value == SpeakerState.MISSING_VOICE) {
            val result = tts.setLanguage(Locale.GERMANY)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                _state.value = SpeakerState.READY
            }
        }
    }

    override fun speak(clipId: String, text: String, slow: Boolean) {
        when (_state.value) {
            SpeakerState.STARTING -> { pending = Triple(clipId, text, slow); return }
            SpeakerState.UNAVAILABLE, SpeakerState.MISSING_VOICE -> return
            SpeakerState.READY -> Unit
        }
        tts.setSpeechRate(if (slow) SLOW_RATE else NORMAL_RATE)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "$clipId#${System.nanoTime()}")
    }

    override fun stop() {
        tts.stop()
        _playing.value = null
    }

    private companion object {
        const val NORMAL_RATE = 0.9f
        const val SLOW_RATE = 0.5f
    }
}
