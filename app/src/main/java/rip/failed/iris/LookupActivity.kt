package rip.failed.iris

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import rip.failed.iris.core.Assets
import rip.failed.iris.core.Dictionary
import rip.failed.iris.core.Resolution
import rip.failed.iris.core.TextNormalizer
import rip.failed.iris.ui.EntryView
import rip.failed.iris.ui.MultiEntryView

/**
 * The point of the app: a highlighted word becomes a definition.
 *
 * Launched by ACTION_PROCESS_TEXT from any app that offers text selection. The
 * platform gives no popover contract -- this is an activity over the source app,
 * and the user backs out to return. The translucent theme and the card below are
 * an attempt to read as an overlay rather than a takeover; that is the whole
 * extent of what Android allows here.
 */
class LookupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val selection = intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty()

        setContent {
            IrisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Transparent,
                ) {
                    LookupOverlay(selection)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTop keeps a second selection from stacking activities, but the
        // composition above read the first intent. Recreating is honest and
        // cheap; the dictionary is already resident.
        recreate()
    }
}

@Composable
private fun LookupOverlay(selection: String) {
    val context = LocalContext.current
    val results = remember(selection) {
        if (selection.isBlank()) emptyList()
        else resolveSelection(Assets.dictionary(context), selection)
    }

    // Tap anywhere outside the card to dismiss, which is what a popover does and
    // is the behaviour a reader expects. The card itself consumes clicks so
    // tapping the definition does not close it.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { (context as? Activity)?.finish() },
            )
            .navigationBarsPadding()
            .padding(16.dp),
        // Bottom, not top. Coming up from the bottom keeps the card near the
        // back gesture and the keyboard, and leaves the text being read visible
        // above it.
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            when {
                selection.isBlank() -> Message("Nothing selected")
                results.isEmpty() -> Message(stringResource(R.string.not_found, selection))
                results.size == 1 -> EntryView(results.first().entry!!)
                else -> MultiEntryView(results.mapNotNull { it.entry })
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Resolve a selection to the entries worth showing, deduplicated by headword.
 *
 * Selection text arrives with punctuation attached often enough that a single
 * raw lookup would produce spurious "not found" results, so each cleaned-up
 * variant is tried. For a multi-word selection this now collects every word
 * that resolves rather than stopping at the first: showing only "brown" for
 * "the quick brown fox" is arbitrary, and the whole point of the overlay is
 * not requiring a second trip.
 */
fun resolveSelection(dict: Dictionary, selection: String): List<Resolution> {
    val out = ArrayList<Resolution>(4)
    val seen = HashSet<String>(4)
    for (candidate in TextNormalizer.candidates(selection)) {
        val r = dict.resolve(candidate)
        val entry = r.entry ?: continue
        if (seen.add(entry.headword)) out += r
    }
    return out
}
