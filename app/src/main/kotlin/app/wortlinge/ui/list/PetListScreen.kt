package app.wortlinge.ui.list

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.wortlinge.R
import app.wortlinge.audio.SpeakerState
import app.wortlinge.core.content.Rarity
import app.wortlinge.core.content.Word
import app.wortlinge.core.pets.Stage
import app.wortlinge.ui.pets.PetLook
import app.wortlinge.ui.pets.PetView
import app.wortlinge.ui.pictures.WordPicture
import app.wortlinge.ui.theme.Palette
import app.wortlinge.ui.theme.labelColour

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetListScreen(vm: PetListViewModel = viewModel(factory = PetListViewModel.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<PetCard?>(null) }

    LifecycleResumeEffect(Unit) {
        vm.refreshVoice()
        onPauseOrDispose { }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Header(state.areaName, state.pets.size)
            }
            if (state.speaker == SpeakerState.MISSING_VOICE || state.speaker == SpeakerState.UNAVAILABLE) {
                item(span = { GridItemSpan(maxLineSpan) }) { VoiceBanner(state.speaker) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                PreviewControls(state.preview, vm::setStage, vm::toggleShiny, vm::toggleAsleep)
            }
            items(state.pets, key = { it.word.id }) { pet ->
                PetCardView(
                    pet = pet,
                    preview = state.preview,
                    talking = state.playing == pet.word.id,
                    onPlay = { slow -> vm.playWord(pet.word, slow) },
                    onOpen = { selected = pet },
                )
            }
        }
    }

    selected?.let { pet ->
        ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = MaterialTheme.colorScheme.surface) {
            PetDetail(
                pet = pet,
                preview = state.preview,
                playing = state.playing,
                onPlayWord = { slow -> vm.playWord(pet.word, slow) },
                onPlayExample = { slow -> vm.playExample(pet.word, slow) },
            )
        }
    }
}

@Composable
private fun Header(areaName: String, count: Int) {
    Column(Modifier.padding(bottom = 4.dp)) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, color = Palette.HoneyDeep)
        Text(
            stringResource(R.string.area_pets, areaName, count),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VoiceBanner(speaker: SpeakerState) {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE8E0))) {
        Column(Modifier.padding(16.dp)) {
            if (speaker == SpeakerState.MISSING_VOICE) {
                Text(stringResource(R.string.voice_missing_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.voice_missing_body), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                Button(onClick = {
                    try {
                        context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
                    } catch (_: ActivityNotFoundException) { }
                }) { Text(stringResource(R.string.voice_install)) }
            } else {
                Text(stringResource(R.string.voice_unavailable), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewControls(
    preview: PreviewOptions,
    onStage: (Stage) -> Unit,
    onShiny: () -> Unit,
    onAsleep: () -> Unit,
) {
    Column {
        Text(
            stringResource(R.string.preview),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        val stages = listOf(Stage.BABY to R.string.baby, Stage.TEEN to R.string.teen, Stage.ADULT to R.string.adult)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            stages.forEachIndexed { i, (stage, label) ->
                SegmentedButton(
                    selected = preview.stage == stage,
                    onClick = { onStage(stage) },
                    shape = SegmentedButtonDefaults.itemShape(i, stages.size),
                ) { Text(stringResource(label)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = preview.shiny, onClick = onShiny, label = { Text(stringResource(R.string.shiny)) })
            FilterChip(selected = preview.asleep, onClick = onAsleep, label = { Text(stringResource(R.string.asleep)) })
        }
    }
}

@Composable
private fun PetCardView(
    pet: PetCard,
    preview: PreviewOptions,
    talking: Boolean,
    onPlay: (Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(130.dp)) {
                PetView(
                    look = PetLook(pet.genes, preview.stage, preview.shiny, preview.asleep),
                    talking = talking,
                    modifier = Modifier.fillMaxSize(),
                )
                PictureBadge(pet.word.id, Modifier.align(Alignment.TopEnd))
                RarityDot(pet.word.rarity, Modifier.align(Alignment.TopStart))
            }
            WordLabel(pet, preview.stage, Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(8.dp))
            PlayButtons(onPlay)
        }
    }
}

@Composable
private fun PictureBadge(wordId: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(44.dp)
            .background(Color(0xFFFFF4E0), CircleShape)
            .padding(6.dp),
    ) { WordPicture(wordId, Modifier.fillMaxSize()) }
}

@Composable
private fun RarityDot(rarity: Rarity, modifier: Modifier = Modifier) {
    val (colour, label) = when (rarity) {
        Rarity.COMMON -> return
        Rarity.RARE -> Palette.Rare to R.string.rarity_rare
        Rarity.LEGENDARY -> Palette.Legendary to R.string.rarity_legendary
    }
    Text(
        stringResource(label),
        style = MaterialTheme.typography.labelLarge,
        color = Color.White,
        modifier = modifier.background(colour, RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Baby: no text at all, only sound and picture. Teen: article + spelling. Adult: same, plus English. */
@Composable
private fun WordLabel(pet: PetCard, stage: Stage, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (stage == Stage.BABY) {
            Text("· · ·", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        val colour = pet.species.labelColour()
        Text(
            buildAnnotatedString {
                pet.word.article?.let {
                    withStyle(SpanStyle(color = colour, fontWeight = FontWeight.ExtraBold)) { append(it.text) }
                    append(" ")
                }
                append(pet.word.german)
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            pet.word.english,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlayButtons(onPlay: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = { onPlay(false) },
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color(0xFFFFE3A8)),
        ) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = stringResource(R.string.play)) }
        OutlinedIconButton(onClick = { onPlay(true) }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.SlowMotionVideo, contentDescription = stringResource(R.string.play_slowly))
        }
    }
}

@Composable
private fun PetDetail(
    pet: PetCard,
    preview: PreviewOptions,
    playing: String?,
    onPlayWord: (Boolean) -> Unit,
    onPlayExample: (Boolean) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(240.dp)) {
            PetView(
                look = PetLook(pet.genes, preview.stage, preview.shiny, preview.asleep),
                talking = playing == pet.word.id || playing == "${pet.word.id}:example",
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(Modifier.size(96.dp).background(Color(0xFFFFF4E0), CircleShape).padding(14.dp)) {
            WordPicture(pet.word.id, Modifier.fillMaxSize())
        }
        WordLabel(pet, preview.stage, Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(12.dp))
        PlayButtons(onPlayWord)

        if (preview.stage == Stage.ADULT) {
            Spacer(Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.example),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(pet.word.exampleDe, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            pet.word.exampleEn,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(onClick = { onPlayExample(false) }) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = stringResource(R.string.play))
                        }
                        OutlinedIconButton(onClick = { onPlayExample(true) }) {
                            Icon(Icons.Rounded.SlowMotionVideo, contentDescription = stringResource(R.string.play_slowly))
                        }
                    }
                }
            }
        }
    }
}
