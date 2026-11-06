package rip.failed.iris.core

/**
 * Read-only view over the baked dictionary file.
 *
 * The file is loaded whole into a ByteArray at startup. At 17 MB that is a few
 * milliseconds and a fixed, knowable cost, versus an index structure that would
 * be more code doing less. Binary search over the sorted headword table is the
 * index; nothing else is built.
 *
 * All offsets are into [buf]. Entries are laid out consecutively starting at
 * [entriesBase], each [ENTRY_SIZE] bytes:
 *
 *   head  u32  offset into the string blob
 *   hlen  u16  headword length in bytes
 *   pos   u8   bitmask: 1 noun, 2 verb, 4 adj, 8 adv
 *   gcnt  u16  number of glosses
 *   goff  u32  offset into the string blob of the packed gloss block
 *   glen  u32  packed gloss block length
 *
 * The packed gloss block is gcnt repetitions of (u16 length, UTF-8 bytes).
 */
class Dictionary(private val buf: ByteArray, private val forms: FormIndex?) {

    init {
        require(buf.size >= HEADER_SIZE) { "dictionary file is empty" }
        require(String(buf, 0, 8, Charsets.US_ASCII) == MAGIC) {
            "not an iris dictionary"
        }
        val version = readU16(8)
        require(version == FORMAT_VERSION) { "format $version, expected $FORMAT_VERSION" }
        entryCount = readU32(10).toInt()
        stringsBase = HEADER_SIZE + entryCount * ENTRY_SIZE
        require(buf.size >= stringsBase) { "dictionary file is truncated" }
    }

    /** Number of distinct headwords. */
    val size: Int get() = entryCount

    private val entryCount: Int
    private val stringsBase: Int

    /** Entries in ascending byte order, which for UTF-8 is ascending codepoint order. */
    fun headwordAt(index: Int): String {
        val p = entryBase(index)
        val off = readU32(p)
        val len = readU16(p + 4)
        return String(buf, stringsBase + off, len, Charsets.UTF_8)
    }

    /**
     * Exact-match lookup. The only search this app does: the overlay is handed
     * one highlighted word and returns that word or nothing.
     */
    fun lookup(headword: String): Entry? {
        val i = indexOf(headword)
        return if (i < 0) null else entryAt(i)
    }

    /**
     * Resolve any surface form to an entry.
     *
     *   1. exact headword
     *   2. exception list (ran -> run, mice -> mouse, better -> good)
     *   3. suffix rules (running -> run, dogs -> dog), followed transitively
     *
     * Step 3 walks the chain rather than stopping at the first hit. A rule
     * candidate that is itself inflected is a waypoint, not an answer:
     * "runningly" -> "running" is a headword, so a single-pass resolve would
     * return the adverb's adjective and stop. Following it one more step
     * reaches "run", which is what the reader wants.
     *
     * The walk is bounded at [MAX_DEPTH] because the rules can in principle
     * cycle, and every candidate is verified against the dictionary so a bad
     * guess costs one failed lookup and never reaches the screen.
     *
     * Not attempted: deciding whether a form that is both a headword and an
     * inflection should show itself or its base. "happily" and "interesting"
     * are genuine words; "running" and "flies" are inflections carrying a
     * specialised sense, and no lexical test separates the two groups. The
     * direct entry wins, which is right for the first pair and acceptable for
     * the second.
     */
    fun resolve(form: String): Resolution {
        val direct = lookup(form)
        if (direct != null) return Resolution(direct, form, matchedByRule = false)

        val viaRules = resolveInflection(form, depth = 0)
        return Resolution(viaRules, form, matchedByRule = viaRules != null)
    }

    /**
     * Walk from an inflected form toward a base entry.
     *
     * An exception-list hit is taken immediately: irregular forms have no
     * productive rule behind them, so there is nothing further down to reach.
     * A rule hit is followed one more step when it still looks inflected,
     * otherwise it stands on its own.
     */
    private fun resolveInflection(form: String, depth: Int): Entry? {
        if (depth >= MAX_DEPTH) return null

        forms?.base(form)?.let { base -> lookup(base)?.let { return it } }

        for (candidate in lemmaCandidates(form)) {
            if (candidate == form) continue
            val hit = lookup(candidate)
            if (hit != null) {
                if (!looksInflected(candidate)) return hit
                resolveInflection(candidate, depth + 1)?.let { return it }
                return hit
            }
            resolveInflection(candidate, depth + 1)?.let { return it }
        }
        return null
    }

    /**
     * Whether a headword is shaped like an inflection worth walking past.
     * Only ever decides "keep looking", never rejects a word outright.
     */
    private fun looksInflected(word: String): Boolean {
        val w = word.lowercase()
        return w.length > 3 && (w.endsWith("ing") || w.endsWith("ed") || w.endsWith("ly"))
    }

    /** Entry at a sorted index, with glosses decoded. */
    fun entryAt(index: Int): Entry {
        val p = entryBase(index)
        val headOff = readU32(p)
        val headLen = readU16(p + 4)
        val posMask = buf[p + 6].toInt() and 0xFF
        val gcnt = readU16(p + 7)
        val goff = readU32(p + 9)
        val glen = readU32(p + 13)

        val glosses = ArrayList<String>(gcnt)
        var o = 0
        while (o < glen && glosses.size < gcnt) {
            val len = readU16(stringsBase + goff + o)
            o += 2
            glosses += String(buf, stringsBase + goff + o, len, Charsets.UTF_8)
            o += len
        }
        return Entry(
            headword = String(buf, stringsBase + headOff, headLen, Charsets.UTF_8),
            partsOfSpeech = PartsOfSpeech.fromMask(posMask),
            glosses = glosses,
        )
    }

    private fun indexOf(headword: String): Int {
        val target = headword.toByteArray(Charsets.UTF_8)
        var lo = 0
        var hi = entryCount - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val cmp = compareAt(mid, target)
            when {
                cmp == 0 -> return mid
                cmp < 0 -> lo = mid + 1
                else -> hi = mid - 1
            }
        }
        return -1
    }

    /** Byte-compare the headword at [index] against [target], without allocating. */
    private fun compareAt(index: Int, target: ByteArray): Int {
        val p = entryBase(index)
        val off = stringsBase + readU32(p)
        val len = readU16(p + 4)
        val n = minOf(len, target.size)
        for (i in 0 until n) {
            val a = buf[off + i].toInt() and 0xFF
            val b = target[i].toInt() and 0xFF
            if (a != b) return a - b
        }
        return len - target.size
    }

    private fun entryBase(index: Int): Int {
        require(index in 0 until entryCount) { "entry $index out of range 0..${entryCount - 1}" }
        return HEADER_SIZE + index * ENTRY_SIZE
    }

    private fun readU16(at: Int): Int =
        (buf[at].toInt() and 0xFF) or ((buf[at + 1].toInt() and 0xFF) shl 8)

    private fun readU32(at: Int): Int =
        (buf[at].toInt() and 0xFF) or
            ((buf[at + 1].toInt() and 0xFF) shl 8) or
            ((buf[at + 2].toInt() and 0xFF) shl 16) or
            ((buf[at + 3].toInt() and 0xFF) shl 24)

    companion object {
        const val MAGIC = "IRISDICT"
        const val FORMAT_VERSION = 1
        const val HEADER_SIZE = 22
        const val ENTRY_SIZE = 17

        /** Longest inflection chain to follow before giving up. */
        private const val MAX_DEPTH = 3

        /**
         * Candidate base forms for a regular inflection, most specific first.
         *
         * These are Morphy's rules in miniature. They over-generate on purpose:
         * the caller verifies each candidate against the dictionary, so a wrong
         * guess costs one failed lookup and never reaches the screen.
         */
        fun lemmaCandidates(form: String): List<String> {
            if (form.length < 3) return emptyList()
            val out = ArrayList<String>(6)
            val lower = form.lowercase()
            fun add(s: String) {
                if (s.length >= 2 && s != lower) out += s
            }

            // plural / third person:  boxes -> box,  dogs -> dog
            if (lower.endsWith("es")) add(lower.dropLast(2))
            if (lower.endsWith("s")) add(lower.dropLast(1))
            // ies -> y:  puppies -> puppy
            if (lower.endsWith("ies")) add(lower.dropLast(3) + "y")

            // past / participle:  jumped -> jump,  hated -> hate,  stopped -> stop
            if (lower.endsWith("ed")) {
                val stem = lower.dropLast(2)
                add(stem)
                add(stem + "e")
                if (stem.length >= 2 && stem[stem.length - 1] == stem[stem.length - 2]) {
                    add(stem.dropLast(1))
                }
            }
            // progressive:  running -> run,  making -> make
            if (lower.endsWith("ing")) {
                val stem = lower.dropLast(3)
                add(stem)
                add(stem + "e")
                if (stem.length >= 2 && stem[stem.length - 1] == stem[stem.length - 2]) {
                    add(stem.dropLast(1))
                }
            }
            // comparative / adverb:  quickly -> quick
            if (lower.endsWith("ly")) add(lower.dropLast(2))
            if (lower.endsWith("ier")) add(lower.dropLast(3) + "y")
            if (lower.endsWith("iest")) add(lower.dropLast(4) + "y")
            return out
        }
    }
}

/** A resolved entry plus how it was reached, so the UI can stay silent about the path. */
data class Resolution(
    val entry: Entry?,
    val form: String,
    val matchedByRule: Boolean,
)

data class Entry(
    val headword: String,
    val partsOfSpeech: List<PartsOfSpeech>,
    val glosses: List<String>,
)

enum class PartsOfSpeech(val bit: Int) {
    NOUN(1),
    VERB(2),
    ADJECTIVE(4),
    ADVERB(8);

    companion object {
        fun fromMask(mask: Int): List<PartsOfSpeech> = entries.filter { mask and it.bit != 0 }
    }
}
