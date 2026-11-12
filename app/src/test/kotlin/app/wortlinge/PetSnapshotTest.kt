package app.wortlinge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.wortlinge.core.content.WordCatalog
import app.wortlinge.core.pets.PetGenes
import app.wortlinge.core.pets.Species
import app.wortlinge.core.pets.Stage
import app.wortlinge.ui.pets.PetLook
import app.wortlinge.ui.pets.drawPet
import app.wortlinge.ui.pictures.WordPicture
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders every pet in every stage to a PNG so the drawings can be checked without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w600dp-h1000dp-hdpi")
class PetSnapshotTest {
    private val words = WordCatalog.parse(File("src/main/assets/words.json").readText()).words

    @Test fun closeUps() {
        val picks = listOf("hallo", "brot", "brezel", "kaffee")
        captureRoboImage("build/outputs/roborazzi/closeups.png") {
            Column(Modifier.background(Color(0xFFFFF8EC))) {
                picks.forEach { id ->
                    val w = words.first { it.id == id }
                    val genes = PetGenes.forWord(w.id, Species.of(w))
                    Row {
                        listOf(Stage.BABY, Stage.ADULT).forEach { st ->
                            Canvas(Modifier.size(280.dp, 220.dp)) { drawPet(PetLook(genes, st)) }
                        }
                    }
                }
            }
        }
    }

    @Test fun allPets() {
        captureRoboImage("build/outputs/roborazzi/pets.png") {
            Column(Modifier.background(Color(0xFFFFF8EC)).padding(8.dp)) {
                words.forEach { w ->
                    val genes = PetGenes.forWord(w.id, Species.of(w))
                    Row {
                        WordPicture(w.id, Modifier.size(80.dp).padding(10.dp))
                        listOf(
                            PetLook(genes, Stage.BABY),
                            PetLook(genes, Stage.TEEN),
                            PetLook(genes, Stage.ADULT),
                            PetLook(genes, Stage.ADULT, shiny = true),
                            PetLook(genes, Stage.TEEN, asleep = true),
                            PetLook(genes, Stage.ADULT, silhouette = true),
                        ).forEach { look -> Canvas(Modifier.size(80.dp)) { drawPet(look) } }
                    }
                }
            }
        }
    }
}
