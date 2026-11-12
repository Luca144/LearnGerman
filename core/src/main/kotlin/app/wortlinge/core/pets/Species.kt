package app.wortlinge.core.pets

import app.wortlinge.core.content.Article
import app.wortlinge.core.content.Word

/**
 * One species per grammatical gender, plus wisps for words without an article.
 * Hue ranges are in degrees on the colour wheel; every pet's hue stays inside its family.
 */
enum class Species(val hueFrom: Float, val hueTo: Float) {
    /** der: steel-blue family, armoured four-legged drake with horns. */
    DER(198f, 226f),
    /** die: crimson family, two-legged wyvern with wings. Wraps past 360. */
    DIE(334f, 362f),
    /** das: moss-green family, shell-backed beast with fronds. */
    DAS(92f, 150f),
    /** No article: golden ember spirit. */
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
