package rip.failed.iris.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import rip.failed.iris.core.Entry
import rip.failed.iris.core.PartsOfSpeech

/**
 * Headword, parts of speech, then glosses. Shared by the overlay and the app so
 * the two never drift.
 *
 * The headword shown is the entry's, not the word that was looked up. Selecting
 * "running" and seeing "run" is correct and intentional -- the inflection path
 * is an implementation detail, and echoing the selection back as a heading
 * would just be noise above the line that matters.
 */
@Composable
fun EntryView(entry: Entry, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = entry.headword,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = entry.partsOfSpeech.joinToString(", ") { it.shortLabel() },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        }
        itemsIndexed(entry.glosses) { index, gloss ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = "${index + 1}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Text(text = gloss, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/**
 * Single-line preview for the search result list. First gloss only: the list is
 * for recognising the word you want, not for reading the dictionary.
 */
@Composable
fun EntryPreview(entry: Entry, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(entry.headword, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text(
                text = entry.partsOfSpeech.joinToString(", ") { it.shortLabel() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        entry.glosses.firstOrNull()?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Spacer(Modifier.height(2.dp))
    }
}

private fun PartsOfSpeech.shortLabel(): String = when (this) {
    PartsOfSpeech.NOUN -> "n."
    PartsOfSpeech.VERB -> "v."
    PartsOfSpeech.ADJECTIVE -> "adj."
    PartsOfSpeech.ADVERB -> "adv."
}
