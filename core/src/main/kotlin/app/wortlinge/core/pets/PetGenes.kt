package app.wortlinge.core.pets

/** Deterministic random numbers so a pet looks the same on every device and every launch. */
class SeededRandom(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        // SplitMix64
        state += -0x61c8864680b583ebL
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    /** Uniform in [0, 1). */
    fun nextFloat(): Float = ((nextLong() ushr 40).toFloat() / (1L shl 24).toFloat())

    fun nextInt(bound: Int): Int = ((nextLong() ushr 1) % bound).toInt()

    fun range(from: Float, to: Float): Float = from + (to - from) * nextFloat()

    fun chance(p: Float): Boolean = nextFloat() < p
}

object Seeds {
    /** FNV-1a 64-bit hash of the word id. Stable across JVMs, unlike relying on library internals. */
    fun of(id: String): Long {
        var hash = -0x340d631b7bdddcdbL
        for (b in id.encodeToByteArray()) {
            hash = hash xor (b.toLong() and 0xff)
            hash *= 0x100000001b3L
        }
        return hash
    }
}

enum class EyeStyle { ROUND, SPARKLY, SLEEPY_LIDS, WIDE }
enum class Pattern { NONE, SPOTS, STRIPES, BELLY }
enum class Mouth { SMILE, OPEN, CAT, TOOTH }
enum class Tuft { NONE, CURL, SPIKES, BOW }

/**
 * Everything that makes one pet look different from its siblings.
 * Values are normalised so the drawing code can scale them freely.
 */
data class PetGenes(
    val species: Species,
    /** Hue in degrees, 0..360. */
    val hue: Float,
    /** Saturation 0..1. */
    val saturation: Float,
    /** Lightness 0..1. */
    val lightness: Float,
    /** Hue offset for markings, relative to the body hue. */
    val accentHueShift: Float,
    /** Width/height multipliers around 1.0. */
    val widthFactor: Float,
    val heightFactor: Float,
    val eyeStyle: EyeStyle,
    val eyeSpacing: Float,
    val pattern: Pattern,
    val patternSeed: Long,
    val mouth: Mouth,
    val tuft: Tuft,
    val blush: Boolean,
    /** Horn length, ear length, leaf size or tail length, depending on species. */
    val featureSize: Float,
) {
    companion object {
        fun forWord(id: String, species: Species): PetGenes {
            val r = SeededRandom(Seeds.of(id))
            val hue = r.range(species.hueFrom, species.hueTo) % 360f
            return PetGenes(
                species = species,
                hue = hue,
                saturation = r.range(0.55f, 0.8f),
                lightness = r.range(0.5f, 0.66f),
                accentHueShift = r.range(-25f, 25f),
                widthFactor = r.range(0.88f, 1.12f),
                heightFactor = r.range(0.9f, 1.1f),
                eyeStyle = EyeStyle.entries[r.nextInt(EyeStyle.entries.size)],
                eyeSpacing = r.range(0.85f, 1.15f),
                pattern = Pattern.entries[r.nextInt(Pattern.entries.size)],
                patternSeed = r.nextLong(),
                mouth = Mouth.entries[r.nextInt(Mouth.entries.size)],
                tuft = Tuft.entries[r.nextInt(Tuft.entries.size)],
                blush = r.chance(0.5f),
                featureSize = r.range(0.8f, 1.25f),
            )
        }
    }
}
