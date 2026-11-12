package app.wortlinge.core

import app.wortlinge.core.pets.PetGenes
import app.wortlinge.core.pets.Seeds
import app.wortlinge.core.pets.Species
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PetGenesTest {
    @Test fun `same id always gives the same pet`() {
        assertEquals(PetGenes.forWord("brot", Species.DAS), PetGenes.forWord("brot", Species.DAS))
    }

    @Test fun `seed is a fixed value so pets never change between app versions`() {
        // FNV-1a 64 of "a" is 0xaf63dc4c8601ec8c
        assertEquals(0xaf63dc4c8601ec8cuL.toLong(), Seeds.of("a"))
    }

    @Test fun `different ids give different pets`() {
        assertNotEquals(PetGenes.forWord("brot", Species.DAS), PetGenes.forWord("wasser", Species.DAS))
    }

    @Test fun `hue stays inside the species colour family`() {
        val ids = (1..500).map { "word$it" }
        Species.entries.forEach { s ->
            ids.forEach { id ->
                val h = PetGenes.forWord(id, s).hue
                val unwrapped = if (h < s.hueFrom % 360f && s.hueTo > 360f) h + 360f else h
                assertTrue(unwrapped >= s.hueFrom && unwrapped <= s.hueTo, "$s $id hue $h")
            }
        }
    }

    @Test fun `colour families never overlap`() {
        val ranges = Species.entries.map { it.hueFrom..it.hueTo }
        for (i in ranges.indices) for (j in i + 1 until ranges.size) {
            val a = ranges[i]; val b = ranges[j]
            val overlap = a.start <= b.endInclusive && b.start <= a.endInclusive
            assertTrue(!overlap, "${Species.entries[i]} overlaps ${Species.entries[j]}")
        }
    }
}
