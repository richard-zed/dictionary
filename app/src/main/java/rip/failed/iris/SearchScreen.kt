package rip.failed.iris

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import rip.failed.iris.core.Entry
import rip.failed.iris.core.TextNormalizer
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

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        selected?.let { entry ->
            Column(Modifier.fillMaxSize()) {
                TextButton(onClick = { selected = null }, modifier = Modifier.padding(4.dp)) {
                    Text("Back")
                }
                EntryView(entry)
            }
        } ?: Column(Modifier.fillMaxSize()) {
            if (query.isBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Type to search",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp))
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

/**
 * Prefix search over the sorted headword table.
 *
 * Two lookups, not a scan: binary search for the query itself, and binary
 * search for the query with a high sentinel appended. Everything between the
 * two indices shares the prefix. The sentinel is U+10FFFF, which sorts above
 * every real character in UTF-8 byte order, so the upper bound is exact.
 */
fun search(dict: rip.failed.iris.core.Dictionary, query: String, limit: Int): List<Entry> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()

    val first = dict.firstIndexAtOrAfter(q)
    if (first < 0) return emptyList()

    val out = ArrayList<Entry>(minOf(limit, 16))
    var i = first
    while (i < dict.size && out.size < limit) {
        val head = dict.headwordAt(i)
        if (!head.startsWith(q, ignoreCase = true)) break
        out += dict.entryAt(i)
        i++
    }
    return out
}

/** Kept for the typeahead path; the overlay uses [TextNormalizer] instead. */
@Suppress("unused")
private fun normalise(query: String) = TextNormalizer.candidates(query)
