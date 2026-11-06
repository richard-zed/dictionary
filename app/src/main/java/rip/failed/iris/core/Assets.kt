package rip.failed.iris.core

import android.content.Context

/**
 * Loads the baked dictionary once per process.
 *
 * 17 MB read into a ByteArray is a fixed, small cost and it makes every later
 * lookup a pure array read with no IO and no allocation beyond the strings
 * returned. There is deliberately no lazy or partial loading: a dictionary
 * that is only sometimes resident would make the overlay's first lookup slow,
 * and the overlay is the part that must feel instant.
 */
object Assets {

    @Volatile
    private var loaded: Dictionary? = null

    fun dictionary(context: Context): Dictionary =
        loaded ?: synchronized(this) {
            loaded ?: build(context.applicationContext).also { loaded = it }
        }

    private fun build(context: Context): Dictionary {
        val data = context.assets.open("iris.dat").use { it.readBytes() }
        val forms = context.assets.open("iris.frm").use { FormIndex.parse(it.readBytes()) }
        return Dictionary(data, forms)
    }
}
