package rip.failed.iris.core

/**
 * Map from inflected surface form to base form, built from WordNet's exception
 * lists (ran -> run, mice -> mouse, better -> good). Only forms whose base is
 * actually in the dictionary are here, so a hit is always resolvable.
 *
 * Small enough (a few thousand pairs) that a HashMap built at load time is
 * cheaper to write and read than any on-disk structure.
 */
class FormIndex(private val map: Map<String, String>) {

    fun base(form: String): String? = map[form.lowercase()]

    val size: Int get() = map.size

    companion object {
        const val MAGIC = "IRISFORM"
        const val HEADER_SIZE = 12

        /**
         * Parse the form file. Format: magic, u32 count, then count repetitions
         * of (u16 len + UTF-8 form, u16 len + UTF-8 base).
         */
        fun parse(buf: ByteArray): FormIndex {
            require(buf.size >= HEADER_SIZE) { "form file is empty" }
            require(String(buf, 0, 8, Charsets.US_ASCII) == MAGIC) {
                "not an iris form index"
            }
            val count = (buf[8].toInt() and 0xFF) or
                ((buf[9].toInt() and 0xFF) shl 8) or
                ((buf[10].toInt() and 0xFF) shl 16) or
                ((buf[11].toInt() and 0xFF) shl 24)

            val map = HashMap<String, String>(count * 2)
            var at = HEADER_SIZE
            repeat(count) {
                val flen = (buf[at].toInt() and 0xFF) or ((buf[at + 1].toInt() and 0xFF) shl 8)
                at += 2
                val form = String(buf, at, flen, Charsets.UTF_8)
                at += flen
                val blen = (buf[at].toInt() and 0xFF) or ((buf[at + 1].toInt() and 0xFF) shl 8)
                at += 2
                val base = String(buf, at, blen, Charsets.UTF_8)
                at += blen
                map[form] = base
            }
            return FormIndex(map)
        }
    }
}
