package rip.failed.iris

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
    val result = remember(selection) {
        if (selection.isBlank()) null
        else resolveSelection(Assets.dictionary(context), selection)
    }

    Box(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            when {
                selection.isBlank() -> Message("Nothing selected")
                result?.entry == null -> Message(stringResource(R.string.not_found, selection))
                else -> EntryView(result.entry)
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
 * Try each cleaned-up variant of the selection in turn, stopping at the first
 * hit. Selection text arrives with punctuation attached often enough that
 * trying only the raw string would produce spurious "not found" results.
 */
fun resolveSelection(dict: Dictionary, selection: String): Resolution? {
    for (candidate in TextNormalizer.candidates(selection)) {
        val r = dict.resolve(candidate)
        if (r.entry != null) return r
    }
    return null
}
