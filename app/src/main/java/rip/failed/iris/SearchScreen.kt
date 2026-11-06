package rip.failed.iris

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import rip.failed.iris.core.Assets
import rip.failed.iris.core.Dictionary
import rip.failed.iris.core.Entry
import rip.failed.iris.ui.EntryPreview
import rip.failed.iris.ui.EntryView

/**
 * The app itself. A search field at the bottom, results above it, updating as
 * you type. Tapping a result opens the full entry.
 *
 * This is the bonus -- the overlay is the point -- so it does the obvious thing
 * and nothing more.
 */
@Composable
fun SearchScreen() {
    val context = LocalContext.current
    val dict = remember { Assets.dictionary(context) }

    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<Entry?>(null) }

    // Prefix results, computed on each keystroke. A binary search to the first
    // matching headword then a linear run forward: the cost is proportional to
    // how many results are shown, not to the size of the dictionary.
    val results = remember(query) { search(dict, query, limit = 40) }

    // While an entry is open, back returns to the results rather than closing
    // the app. Without this the system back dispatches to the activity and the
    // whole thing exits, losing the query.
    BackHandler(enabled = selected != null) { selected = null }

    // Explicit surface colour rather than inherited: the activity window is
    // whatever Theme.Iris paints, and a transparent Compose root over it is how
    // the search field ended up invisible.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        // safeDrawingPadding() already covers the system bars AND the keyboard
        // -- it is the union of systemBars and ime. Adding a separate ime
        // padding on top of it double-counts, which is why the field snapped up
        // roughly twice as far as the keyboard.
        //
        // It is used alone here, before fillMaxSize so the box measures against
        // the space that is actually left. Previously the order was fill then
        // pad, which placed the content against a full-screen measure and made
        // the overshoot vary between runs.
        Box(
            Modifier
                .safeDrawingPadding()
                .fillMaxSize(),
        ) {
            selected?.let { entry ->
                Column(Modifier.fillMaxSize()) {
                    TextButton(onClick = { selected = null }, modifier = Modifier.padding(4.dp)) {
                        Text("Back", color = MaterialTheme.colorScheme.primary)
                    }
                    EntryView(entry)
                }
            } ?: Column(Modifier.fillMaxSize()) {
                if (query.isBlank()) {
                    Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            "Type to search",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (results.isEmpty()) {
                    Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.not_found, query),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        items(results, key = { it.headword }) { entry ->
                            EntryPreview(
                                entry = entry,
                                modifier = Modifier.clickable { selected = entry },
                            )
                            HorizontalDivider(
                                Modifier.padding(horizontal = 20.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }

                // Every colour is stated. Material3's defaults for an outlined
                // field resolved against a scheme whose surface matched the
                // window, leaving the border and hint invisible.
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            stringResource(R.string.search_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

/**
 * Prefix search over the sorted headword table.
 *
 * One binary search to find where the prefix run starts, then a walk forward
 * while the prefix still holds. The cost is proportional to the results shown,
 * not to the size of the dictionary.
 *
 * The query is lowercased before the search because the table is lowercased
 * throughout. Searching for "Dog" with case intact would binary-search past
 * every real entry -- uppercase letters sort below lowercase -- land at the
 * first headword, and match nothing.
 */
fun search(dict: Dictionary, query: String, limit: Int): List<Entry> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()

    val first = dict.firstIndexAtOrAfter(q)

    val out = ArrayList<Entry>(minOf(limit, 16))
    var i = first
    while (i < dict.size && out.size < limit) {
        if (!dict.headwordAt(i).startsWith(q)) break
        out += dict.entryAt(i)
        i++
    }
    return out
}
