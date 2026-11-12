package app.wortlinge

import android.app.Application
import app.wortlinge.audio.TtsWordSpeaker
import app.wortlinge.audio.WordSpeaker
import app.wortlinge.core.content.WordCatalog
import app.wortlinge.data.ContentRepository

/** Tiny manual dependency container. One instance per process. */
class AppContainer(app: Application) {
    val catalog: WordCatalog by lazy { ContentRepository(app).load() }
    val speaker: WordSpeaker by lazy { TtsWordSpeaker(app) }
}

class WortlingeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.speaker // start the TTS engine early; it takes a moment to warm up
    }
}
