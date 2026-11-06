# Iris

A dictionary for Android. Highlight a word anywhere and get its definition.

There are two surfaces, and their importance is not equal:

- **The overlay** — the point. Registers `ACTION_PROCESS_TEXT`, so it appears
  in the text-selection menu of any app that offers one.
- **The app** — the bonus. A search field at the bottom, results above it,
  updating as you type.

English only for now. The dictionary is baked in; the app has no network
permission and never fetches anything.

## The overlay is not a popover

iOS gives you a system-wide selection menu you can extend, and it can present a
popover. Android has no equivalent. `ACTION_PROCESS_TEXT` launches an activity
over the source app, hands over the selected string, and the user backs out to
return. This app uses a translucent window and a card to read as close to an
overlay as the platform allows, but it is genuinely a takeover.

The other options are a `SYSTEM_ALERT_WINDOW` floating window (needs a
permission grant, and still needs process-text to obtain the word) or an
accessibility service (works everywhere, but is a much larger permission
conversation). Neither was worth it for a personal app.

## Dictionary data

WordNet 3.1, stripped to headwords, parts of speech and definition glosses.

- 147k entries, 17.3 MB, in `app/src/main/assets/iris.dat`
- Inflected forms in `iris.frm`, built from WordNet's exception lists

WordNet's licence permits use, modification and distribution for any purpose
without fee, provided the copyright notice and disclaimer accompany all copies.
That notice ships as `app/src/main/assets/WORDNET-LICENSE.txt`. Unlike a
Wiktionary extract there is no share-alike obligation.

### Regenerating

```
tools/fetch-wordnet.sh
python3 tools/build_dict.py
cp tools/iris.dat tools/iris.frm app/src/main/assets/
```

The generated files are gitignored: they are build outputs and 17 MB of binary
in git would be permanent weight for something a script reproduces.

### Why the resolver is dumb on purpose

Lookup order is exact headword, then the exception list, then one pass of suffix
rules, each candidate verified against the dictionary.

A form that is both a headword and an inflection returns itself, so "runningly"
lands on "running" rather than walking on to "run". Making that smarter needs
either unbounded recursion or a sense heuristic that the data does not support:
"happily" and "interesting" are genuine independent headwords, while "running"
and "flies" are inflections carrying a specialised sense, and nothing lexical
separates the two groups. Not worth it for words nobody highlights.

## Building

Same toolchain as the other Android projects here: Gradle 9.8.2, AGP 9.0.1,
compileSdk 37, minSdk 24. AGP 9 compiles Kotlin itself, so there is no Kotlin
plugin — but the Compose compiler plugin is still required.

```
./gradlew :app:assembleDebug
```

Release is signed with the debug key on purpose. This is a personal app with no
distribution plan; if that changes, a real `signingConfig` has to be added, and
that swap forces one uninstall.
