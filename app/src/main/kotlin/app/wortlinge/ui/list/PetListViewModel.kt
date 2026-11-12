package app.wortlinge.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.wortlinge.WortlingeApp
import app.wortlinge.audio.SpeakerState
import app.wortlinge.audio.WordSpeaker
import app.wortlinge.core.content.Word
import app.wortlinge.core.content.WordCatalog
import app.wortlinge.core.pets.PetGenes
import app.wortlinge.core.pets.Species
import app.wortlinge.core.pets.Stage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class PetCard(val word: Word, val species: Species, val genes: PetGenes)

data class PreviewOptions(val stage: Stage = Stage.ADULT, val shiny: Boolean = false, val asleep: Boolean = false)

data class PetListState(
    val areaName: String,
    val pets: List<PetCard>,
    val preview: PreviewOptions,
    val speaker: SpeakerState,
    val playing: String?,
)

class PetListViewModel(catalog: WordCatalog, private val speaker: WordSpeaker) : ViewModel() {
    private val area = catalog.areas.first { it.built }
    private val pets = catalog.wordsIn(area.id).map { w ->
        val species = Species.of(w)
        PetCard(w, species, PetGenes.forWord(w.id, species))
    }
    private val preview = MutableStateFlow(PreviewOptions())

    val state: StateFlow<PetListState> = combine(preview, speaker.state, speaker.playing) { p, s, playing ->
        PetListState(area.nameEn, pets, p, s, playing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PetListState(area.nameEn, pets, preview.value, speaker.state.value, null))

    fun setStage(stage: Stage) = preview.update { it.copy(stage = stage) }
    fun toggleShiny() = preview.update { it.copy(shiny = !it.shiny) }
    fun toggleAsleep() = preview.update { it.copy(asleep = !it.asleep) }

    fun playWord(word: Word, slow: Boolean) = speaker.speak(word.id, word.german, slow)
    fun playExample(word: Word, slow: Boolean) = speaker.speak("${word.id}:example", word.exampleDe, slow)
    fun refreshVoice() = speaker.refresh()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as WortlingeApp
                PetListViewModel(app.container.catalog, app.container.speaker)
            }
        }
    }
}
