#!/bin/sh
# Fetch the WordNet 3.1 data files that build_dict.py needs.
#
# Deliberately not vendored in git: the dumps are 27 MB, this script makes them
# reproducible, and a binary blob in a repo would be permanent weight for
# something regenerated per clone.
set -eu

BASE="https://raw.githubusercontent.com/delver/wordnet-db/master/dict"
DIR="$(dirname "$0")/wordnet"

mkdir -p "$DIR"
cd "$DIR"

for f in index.noun data.noun index.verb data.verb \
         index.adj data.adj index.adv data.adv \
         noun.exc verb.exc adj.exc adv.exc; do
    if [ -s "$f" ]; then
        printf 'have  %s\n' "$f"
    else
        printf 'fetch %s\n' "$f"
        curl -fsS -O "$BASE/$f"
    fi
done

printf '\nnow run: python3 %s/build_dict.py\n' "$(dirname "$0")"
