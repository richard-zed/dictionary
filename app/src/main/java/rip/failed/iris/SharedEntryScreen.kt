package rip.failed.iris

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import rip.failed.iris.core.Assets
import rip.failed.iris.ui.EntryView
import rip.failed.iris.ui.MultiEntryView

/**
 * A shared selection, shown as a full screen.
 *
 * This is the overlay's card with the overlay taken off: same resolution, same
 * entry views, no card, no tap-outside-to-dismiss. It exists because a share
 * arrives as a normal activity launch and nothing about it should pretend to be
 * a popover.
 *
 * It scrolls as a whole rather than each entry scrolling independently, which
 * is what makes a multi-word selection readable instead of a set of small
 * scrollable boxes.
 */
@Composable
fun SharedEntryScreen(selection: String) {
    val context = LocalContext.current
    val results = remember(selection) {
        if (selection.isBlank()) emptyList()
        else resolveSelection(Assets.dictionary(context), selection)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        when {
            selection.isBlank() -> Message("Nothing shared")
            results.isEmpty() -> Message(stringResource(R.string.not_found, selection))
            results.size == 1 -> EntryView(results.first().entry!!)
            else -> MultiEntryView(results.mapNotNull { it.entry })
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyLarge,
    )
}
