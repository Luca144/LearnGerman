package app.wortlinge.core.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class Article(val text: String) {
    @SerialName("der") DER("der"),
    @SerialName("die") DIE("die"),
    @SerialName("das") DAS("das"),
}

@Serializable
enum class WordKind {
    @SerialName("noun") NOUN,
    @SerialName("phrase") PHRASE,
}

@Serializable
enum class Rarity {
    @SerialName("common") COMMON,
    @SerialName("rare") RARE,
    @SerialName("legendary") LEGENDARY,
}

@Serializable
data class Area(
    val id: String,
    val order: Int,
    val nameEn: String,
    val nameDe: String,
    val built: Boolean,
    val width: Int,
    val height: Int,
)

@Serializable
data class Word(
    val id: String,
    val german: String,
    val article: Article? = null,
    val english: String,
    val kind: WordKind,
    val area: String,
    val rarity: Rarity,
    val exampleDe: String,
    val exampleEn: String,
    val x: Int,
    val y: Int,
    val hidden: Boolean = false,
    val accept: List<String> = emptyList(),
) {
    /** "das Brot" for nouns, "Hallo" for phrases. */
    val withArticle: String get() = if (article != null) "${article.text} $german" else german
}

@Serializable
data class WordCatalog(
    val version: Int,
    val areas: List<Area>,
    val words: List<Word>,
) {
    fun area(id: String): Area? = areas.firstOrNull { it.id == id }
    fun wordsIn(areaId: String): List<Word> = words.filter { it.area == areaId }
    fun word(id: String): Word? = words.firstOrNull { it.id == id }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String): WordCatalog = json.decodeFromString(serializer(), text)
    }
}
