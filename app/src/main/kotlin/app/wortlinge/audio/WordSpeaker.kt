package app.wortlinge.audio

import kotlinx.coroutines.flow.StateFlow

enum class SpeakerState { STARTING, READY, MISSING_VOICE, UNAVAILABLE }

/**
 * Plays German audio. The TTS version speaks [text]; a future bundled-clip version
 * can look up a recording by [clipId] instead and ignore [text].
 */
interface WordSpeaker {
    val state: StateFlow<SpeakerState>

    /** The clipId currently playing, or null when silent. Handy for animating the pet. */
    val playing: StateFlow<String?>

    fun speak(clipId: String, text: String, slow: Boolean = false)

    fun stop()

    /** Checks again whether audio is available, e.g. after a voice was installed. */
    fun refresh() {}
}
