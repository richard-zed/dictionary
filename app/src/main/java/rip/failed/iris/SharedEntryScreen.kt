package rip.failed.iris

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
 * The entry views are LazyColumns and own their own scrolling, so this screen
 * must not scroll. Wrapping them in verticalScroll measured them with an
 * unbounded height and crashed the frame -- a lazy list given infinite space has
 * nothing to page against. They are handed the full size instead and scroll
 * individually.
 */
@Composable
fun SharedEntryScreen(selection: String) {
    val context = LocalContext.current
    val results = remember(selection) {
        if (selection.isBlank()) emptyList()
        else resolveSelection(Assets.dictionary(context), selection)
    }

    val modifier = Modifier
        .fillMaxSize()
        .safeDrawingPadding()

    when {
        selection.isBlank() -> CenteredMessage("Nothing shared", modifier)
        results.isEmpty() -> CenteredMessage(
            stringResource(R.string.not_found, selection),
            modifier,
        )
        results.size == 1 -> EntryView(results.first().entry!!, modifier)
        else -> MultiEntryView(results.mapNotNull { it.entry }, modifier)
    }
}

/** The empty and not-found cases have no list to scroll, so they centre. */
@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier.padding(20.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
