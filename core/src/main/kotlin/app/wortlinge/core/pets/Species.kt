package app.wortlinge.core.pets

import app.wortlinge.core.content.Article
import app.wortlinge.core.content.Word

/**
 * One species per grammatical gender, plus wisps for words without an article.
 * Hue ranges are in degrees on the colour wheel; every pet's hue stays inside its family.
 */
enum class Species(val hueFrom: Float, val hueTo: Float) {
    /** der: blue family, boxy body with horns. */
    DER(198f, 226f),
    /** die: red/pink family, tall teardrop with long ears. Wraps past 360. */
    DIE(334f, 362f),
    /** das: green family, wide blob with a leaf sprout. */
    DAS(92f, 150f),
    /** No article: golden floating wisp. */
    WISP(38f, 54f);

    companion object {
        fun of(word: Word): Species = when (word.article) {
            Article.DER -> DER
            Article.DIE -> DIE
            Article.DAS -> DAS
            null -> WISP
        }
    }
}

enum class Stage { BABY, TEEN, ADULT }
