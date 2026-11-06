package rip.failed.iris.core

/**
 * Turn a raw selection into something worth looking up.
 *
 * Selections arrive with punctuation, stray whitespace, and line wrapping from
 * the source app. Trailing periods and commas are the common case (selecting a
 * word at the end of a sentence); quotes and brackets appear when the source
 * included them. Possessives are stripped because "runner's" should find
 * "runner" and the apostrophe-s is not in any dictionary.
 *
 * Deliberately conservative: it never lowercases a word it cannot confirm, and
 * the caller tries the original first.
 */
object TextNormalizer {

    private const val MAX_LEN = 64

    /** Variants to try, most likely first, deduplicated. */
    fun candidates(selection: String): List<String> {
        val trimmed = selection.trim()
        if (trimmed.isEmpty()) return emptyList()

        val out = LinkedHashSet<String>(4)
        out += trimmed

        var s = trimmed
        // Strip surrounding punctuation and brackets.
        s = s.trim { it.isWhitespace() || it in LEADING }
        s = s.trimEnd { it.isWhitespace() || it in TRAILING }
        if (s.isNotEmpty()) out += s

        // Possessive: runner's / runners'
        if (s.length > 2 && (s.endsWith("'s") || s.endsWith("\u2019s"))) {
            out += s.dropLast(2)
        } else if (s.length > 1 && (s.endsWith("'") || s.endsWith("\u2019"))) {
            out += s.dropLast(1)
        }

        // A selection can be several words. The whole string is tried first (it
        // may be a phrase like "run of the mill"), but every individual word is
        // offered as well, in order and deduplicated.
        //
        // This used to keep only the longest word, which silently discarded the
        // rest: selecting "the quick brown fox" looked up "brown" and nothing
        // else. The caller resolves candidates in order and shows the first hit,
        // so listing them all is what makes a multi-word selection useful.
        if (s.any { it.isWhitespace() }) {
            s.split(WHITESPACE)
                .filter { it.any(Char::isLetter) }
                .forEach { out += it }
        }

        return out.filter { it.isNotEmpty() && it.length <= MAX_LEN }
    }

    private val LEADING = charArrayOf('"', '\'', '\u201C', '\u2018', '(', '[', '{', '<', '\u00AB')
    private val TRAILING = charArrayOf(
        '.', ',', ';', ':', '!', '?', '"', '\'', '\u201D', '\u2019',
        ')', ']', '}', '>', '\u00BB', '\u2014', '\u2013',
    )
    private val WHITESPACE = Regex("\\s+")
}
