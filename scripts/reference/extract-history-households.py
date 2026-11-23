#!/usr/bin/env python3
"""Extract one campaign's conformed household history (households.tsv) into
a CSV that load-households.sh imports as Reference entries.

Standard library only; run with `python3 -I`. Counts are printed; household
names are written only to files under target/, never to the terminal.

The name, village code, uid and output rules are imported from
extract-households.py, so the CSV and the skipped list have the same columns
and limits. The source key of a row is its lineage, `_src_file|_src_object|
_src_row`: unique, but only stable while it is computed from the same
history files, so the input's sha256 is printed.

A row is left out when the review removed it (`removed_by_review` = yes),
when any column whose header contains "exclu" holds a number above zero or
unexpected text, when its name contains a placeholder word as a whole word
(ة and ه count as one letter), or when it repeats another row of its
`household_row_id` with the same village and name (the first row is kept).
"""

import argparse
import csv
import hashlib
import importlib.util
import re
import sys
from collections import Counter
from pathlib import Path

sys.dont_write_bytecode = True  # importing the loader must leave no __pycache__ behind


def _load_loader():
    path = Path(__file__).resolve().with_name("extract-households.py")
    spec = importlib.util.spec_from_file_location("extract_households", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


households = _load_loader()

KEY_COLUMNS = ("_src_file", "_src_object", "_src_row")
KEY_SEPARATOR = "|"
REQUIRED_COLUMNS = ("org_unit_code", "household_name", "household_row_id", "removed_by_review", *KEY_COLUMNS)
PLACEHOLDER_WORDS = ("مجهول", "فارغ", "مغترب", "نازح", "تجريبي", "خاليه")
SPELLING = str.maketrans({"ة": "ه"})


def placeholder_patterns(words):
    return [(word, re.compile(r"(?<!\w)" + re.escape(word.translate(SPELLING)) + r"(?!\w)")) for word in words]


def placeholder_in(name, patterns):
    folded = name.translate(SPELLING)
    return next((word for word, pattern in patterns if pattern.search(folded)), None)


def sha256_of(path):
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for block in iter(lambda: handle.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--tsv", required=True, help="households.tsv of one campaign's history")
    parser.add_argument("--label", required=True, help="output name, e.g. history-dec2021")
    parser.add_argument("--out-dir", default=str(Path(__file__).resolve().parents[2] / "target" / "reference"))
    parser.add_argument("--villages", metavar="FILE",
                        help="write only rows whose village code is listed in FILE, one per line")
    parser.add_argument("--skip-key", action="append", default=[], metavar="KEY",
                        help="leave out the row with this source key (repeatable)")
    parser.add_argument("--placeholder", action="append", metavar="WORD",
                        help="placeholder word (repeatable; default: " + ", ".join(PLACEHOLDER_WORDS) + ")")
    args = parser.parse_args()
    if not households.LABEL_PATTERN.match(args.label):
        raise SystemExit("--label may contain only letters, digits, dot, dash and underscore.")
    skip_keys = {key.strip() for key in args.skip_key} - {""}
    villages = households.read_villages(args.villages) if args.villages else None
    patterns = placeholder_patterns(args.placeholder or PLACEHOLDER_WORDS)

    with open(args.tsv, newline="", encoding="utf-8") as handle:
        reader = csv.reader(handle, delimiter="\t", quoting=csv.QUOTE_NONE)
        header = next(reader)
        missing = [column for column in REQUIRED_COLUMNS if column not in header]
        if missing:
            raise SystemExit(f"Missing columns: {', '.join(missing)}")
        rows = []
        for number, values in enumerate(reader, start=2):
            if len(values) != len(header):
                raise SystemExit(f"Line {number} has {len(values)} fields, the header {len(header)}.")
            rows.append((number, dict(zip(header, values))))
    exclude_columns = sorted(column for column in header if households.EXCLUDE_MARK in column.lower())

    removed_rows = 0
    excluded_rows = 0
    excluded_by_column = Counter()
    skip_key_rows = Counter()
    outside_rows = 0
    outside_codes = set()
    placeholders = Counter()
    skipped = []
    kept = []
    keys = set()
    for number, row in rows:
        parts = [row[column].strip() for column in KEY_COLUMNS]
        if not all(parts) or any(KEY_SEPARATOR in part for part in parts):
            raise SystemExit(f"Line {number}: the lineage columns must be non-empty and free of {KEY_SEPARATOR!r}.")
        key = KEY_SEPARATOR.join(parts)
        if key in keys:
            raise SystemExit(f"Line {number} repeats a source key; the lineage must be unique.")
        keys.add(key)

        if row["removed_by_review"].strip().lower() == "yes":
            removed_rows += 1
            continue
        reasons = {column: households.exclusion(row[column]) for column in exclude_columns}
        flagged = [column for column, reason in reasons.items() if reason]
        if flagged:
            excluded_rows += 1
            for column in flagged:
                excluded_by_column[(column, reasons[column])] += 1
            continue

        code = households.village_code(row["org_unit_code"])
        name = households.display_name(row["household_name"])
        if key in skip_keys:
            skip_key_rows[key] += 1
        if villages is not None and code and code not in villages:
            outside_rows += 1
            outside_codes.add(code)
            continue
        reason = "listed by --skip-key" if key in skip_keys else households.skip_reason(key, code, name)
        if not reason:
            word = placeholder_in(name, patterns)
            if word:
                placeholders[word] += 1
                reason = "placeholder name"
        if reason:
            skipped.append((number, reason, code, key, name))
            continue
        kept.append((number, code, name, key, row["household_row_id"].strip()))

    unmatched = sorted(skip_keys - set(skip_key_rows))
    if unmatched:
        raise SystemExit("These --skip-key values match no row outside the removed or excluded ones: "
                         + ", ".join(unmatched))

    repeats = 0
    seen = set()
    by_key = {}
    for number, code, name, key, row_id in kept:
        if row_id and (row_id, code, name) in seen:
            repeats += 1
            skipped.append((number, "repeat in household_row_id group", code, key, name))
            continue
        seen.add((row_id, code, name))
        by_key[key] = [(number, code, name)]

    written, _ = households.entries(by_key, skipped)
    skipped.sort()
    output, skipped_output = households.write_outputs(args.out_dir, args.label, written, skipped)

    print(f"Input: {args.tsv}")
    print(f"Input sha256: {sha256_of(args.tsv)}")
    print(f"Rows: {len(rows)}")
    print(f"Removed by review: {removed_rows}")
    print("Exclusion columns: " + (", ".join(exclude_columns) or "none"))
    print(f"Excluded rows: {excluded_rows}")
    for (column, reason), count in sorted(excluded_by_column.items()):
        print(f"  {column} ({reason}): {count}")
    if villages is not None:
        print(f"Outside --villages (not written): {outside_rows} rows on {len(outside_codes)} village codes")
    print(f"Skipped rows: {len(skipped)}")
    for reason, count in sorted(Counter(row[1] for row in skipped).items()):
        print(f"  {reason}: {count}")
    if placeholders:
        print("Placeholder words matched: " + ", ".join(f"{word} {count}" for word, count in placeholders.items()))
    print(f"Written: {len(written)} households, {len({row[2] for row in written})} village codes")
    print(f"Output: {output}")
    print(f"Skipped list: {skipped_output}")


if __name__ == "__main__":
    sys.exit(main())
