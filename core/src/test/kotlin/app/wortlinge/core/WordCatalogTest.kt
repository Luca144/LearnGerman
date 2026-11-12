package app.wortlinge.core

import app.wortlinge.core.content.Rarity
import app.wortlinge.core.content.WordCatalog
import app.wortlinge.core.content.WordKind
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Checks the real words.json shipped in the app, so a typo in the data fails the build. */
class WordCatalogTest {
    private val catalog: WordCatalog by lazy {
        val file = listOf("../app/src/main/assets/words.json", "app/src/main/assets/words.json")
            .map(::File).first { it.exists() }
        WordCatalog.parse(file.readText(Charsets.UTF_8))
    }

    @Test fun `town square has twelve pets and one legendary`() {
        val words = catalog.wordsIn("town_square")
        assertEquals(12, words.size)
        assertEquals(1, words.count { it.rarity == Rarity.LEGENDARY })
    }

    @Test fun `ids are unique`() {
        val ids = catalog.words.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun `nouns have articles and phrases do not`() {
        catalog.words.forEach { w ->
            when (w.kind) {
                WordKind.NOUN -> assertNotNull(w.article, "${w.id} needs an article")
                WordKind.PHRASE -> assertNull(w.article, "${w.id} should not have an article")
            }
        }
    }

    @Test fun `every word belongs to a known area and sits inside the map`() {
        catalog.words.forEach { w ->
            val area = assertNotNull(catalog.area(w.area), "${w.id} has unknown area ${w.area}")
            assertTrue(w.x in 0 until area.width && w.y in 0 until area.height, "${w.id} is off the map")
        }
    }

    @Test fun `no two eggs share a tile`() {
        val tiles = catalog.words.map { Triple(it.area, it.x, it.y) }
        assertEquals(tiles.size, tiles.toSet().size)
    }

    @Test fun `example sentence uses the word`() {
        catalog.words.forEach { w ->
            assertTrue(w.exampleDe.contains(w.german, ignoreCase = true), "${w.id}: '${w.exampleDe}'")
        }
    }

    @Test fun `noun with article reads naturally`() {
        assertEquals("das Brot", catalog.word("brot")!!.withArticle)
        assertEquals("Hallo", catalog.word("hallo")!!.withArticle)
    }

    @Test fun `only the town square is built in version one`() {
        assertEquals(listOf("town_square"), catalog.areas.filter { it.built }.map { it.id })
        assertEquals(4, catalog.areas.size)
    }
}
