#!/usr/bin/env python3
"""
Build iris.dat from WordNet 3.1 data files.

Reads index.POS + data.POS + POS.exc and writes a single sorted binary file
that the Android app loads entirely into memory. No index structure: at this
size a binary search over the headword table is the index.

Format (little-endian throughout):

  magic     8s   b"IRISDICT"
  version   u16  format version
  count     u32  number of entries
  strbytes  u32  total length of the string blob
  entries   count * Entry  (sorted by headword, byte order)
  strings   strbytes bytes of NUL-free UTF-8, offsets into it below

Entry (17 bytes, '<' = no padding):
  head  u32  offset into string blob
  hlen  u16  headword length
  pos   u8   bitmask: 1 noun, 2 verb, 4 adj, 8 adv
  gcnt  u16  number of glosses
  goff  u32  offset into string blob of the packed gloss block
  glen  u32  packed gloss block length

Gloss block: gcnt times (u16 le length, bytes). Definitions only; example
sentences are dropped because they are the bulk of the bytes and none of the
information wanted in a popover.

Inflected forms are NOT stored as entries. They go into a separate
form->lemma map file (iris.frm) that is consulted before any suffix rules.
"""

import struct
import sys
from pathlib import Path

POS_FILES = [("noun", 1), ("verb", 2), ("adj", 4), ("adv", 8)]
HERE = Path(__file__).parent / "wordnet"


def read_data(pos_name: str) -> dict[int, tuple[str, list[str]]]:
    """synset_offset -> (ss_type, [definitions]). Skips WordNet's header lines."""
    out: dict[int, tuple[str, list[str]]] = {}
    with open(HERE / f"data.{pos_name}", encoding="utf-8", errors="replace") as f:
        for line in f:
            if "|" not in line:
                continue
            left, gloss = line.split("|", 1)
            fields = left.split()
            if len(fields) < 6:
                continue
            try:
                offset = int(fields[0])
            except ValueError:
                continue
            ss_type = fields[2]
            # Definition is everything before the first example, which WordNet
            # delimits with '; "'. Some glosses have no examples at all.
            definition = gloss.split('; "', 1)[0].strip()
            if not definition:
                continue
            out[offset] = (ss_type, [definition])
    return out


def read_index(pos_name: str) -> list[tuple[str, int, list[int]]]:
    """[(lemma, ss_type_bit, [synset_offset...])] with offsets in sense order."""
    rows = []
    bit = dict(POS_FILES)[pos_name]
    with open(HERE / f"index.{pos_name}", encoding="utf-8", errors="replace") as f:
        for line in f:
            if line.startswith("  "):
                continue  # license header
            fields = line.split()
            if len(fields) < 6:
                continue
            lemma = fields[0]
            synset_cnt = int(fields[2])
            p_cnt = int(fields[3])
            start = 6 + p_cnt
            offsets = [int(x) for x in fields[start : start + synset_cnt]]
            if offsets:
                rows.append((lemma, bit, offsets))
    return rows


def read_exceptions() -> dict[str, str]:
    """inflected form -> base form. First base listed wins."""
    out: dict[str, str] = {}
    for pos_name, _ in POS_FILES:
        path = HERE / f"{pos_name}.exc"
        if not path.exists():
            continue
        with open(path, encoding="utf-8", errors="replace") as f:
            for line in f:
                parts = line.split()
                if len(parts) >= 2:
                    out.setdefault(parts[0], parts[1])
    return out


def main() -> int:
    # headword -> {pos_bit: [definitions]}
    entries: dict[str, dict[int, list[str]]] = {}

    for pos_name, _bit in POS_FILES:
        data = read_data(pos_name)
        for lemma, bit, offsets in read_index(pos_name):
            defs = entries.setdefault(lemma, {}).setdefault(bit, [])
            for off in offsets:
                synset = data.get(off)
                if synset:
                    defs.extend(synset[1])

    strings = bytearray()
    entries_blob = bytearray()

    def add_str(s: str) -> tuple[int, int]:
        raw = s.encode("utf-8")
        off = len(strings)
        strings.extend(raw)
        return off, len(raw)

    for head in sorted(entries):
        posmask = 0
        glosses: list[str] = []
        for bit in (1, 2, 4, 8):
            if bit in entries[head]:
                posmask |= bit
                glosses.extend(entries[head][bit])
        head_off, head_len = add_str(head)
        packed = bytearray()
        for g in glosses:
            raw = g.encode("utf-8")
            packed += struct.pack("<H", min(len(raw), 0xFFFF)) + raw[:0xFFFF]
        g_off = len(strings)
        strings.extend(packed)
        entries_blob += struct.pack(
            "<IHB HII", head_off, head_len, posmask, len(glosses), g_off, len(packed)
        )

    out = HERE.parent / "iris.dat"
    with open(out, "wb") as f:
        f.write(b"IRISDICT")
        f.write(struct.pack("<HIII", 1, len(entries), len(strings), 0))
        f.write(entries_blob)
        f.write(strings)

    exc = read_exceptions()
    known = set(entries)
    forms = {f: b for f, b in exc.items() if b in known}
    frm = HERE.parent / "iris.frm"
    with open(frm, "wb") as f:
        f.write(b"IRISFORM")
        f.write(struct.pack("<I", len(forms)))
        for form in sorted(forms):
            fo, fl = len(strings), 0
            fb, lb = form.encode(), forms[form].encode()
            f.write(struct.pack("<H", len(fb)) + fb + struct.pack("<H", len(lb)) + lb)

    print(f"entries  {len(entries)}")
    print(f"glosses  {sum(len(g) for e in entries.values() for g in e.values())}")
    print(f"forms    {len(forms)} of {len(exc)} exceptions (rest point outside WordNet)")
    print(f"iris.dat {out.stat().st_size / 1e6:.1f} MB")
    print(f"iris.frm {frm.stat().st_size / 1e6:.1f} MB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
