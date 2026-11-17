#!/usr/bin/env python3
"""Extract one household sheet of a planning workbook into a CSV that
load-households.sh imports as Reference entries.

Standard library only; run with `python3 -I`. The sheet is streamed twice
(header, then the needed columns), so large workbooks stay cheap in memory.
Counts are printed; household names are written only to files under
target/, never to the terminal.

Columns are found by header: `ppcc` (village code), `hname` (household
name), `house_uuid` (stable source key) and every header containing
"exclu". A row is excluded when any exclusion column holds a number above
zero, or unexpected text. Rows repeating a house_uuid with the same village
and name are kept once; a house_uuid on different rows skips all of them.
"""

import argparse
import csv
import hashlib
import re
import secrets
import string
import sys
import time
import xml.etree.ElementTree as ET
import zipfile
from collections import Counter
from decimal import Decimal, InvalidOperation
from pathlib import Path

NS = "{http://schemas.openxmlformats.org/spreadsheetml/2006/main}"
RNS = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}"
PNS = "{http://schemas.openxmlformats.org/package/2006/relationships}"

CODE_HEADER = "ppcc"
NAME_HEADER = "hname"
KEY_HEADER = "house_uuid"
EXCLUDE_MARK = "exclu"
MAX_NAME_LENGTH = 255

UID_SALT = b"datarun-reference-entry:"
UID_PATTERN = re.compile(r"^[a-zA-Z][a-zA-Z0-9]{10}$")
UID_FIRST = string.ascii_letters
UID_REST = string.ascii_letters + string.digits
CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
WHITESPACE = re.compile(r"\s+")
LABEL_PATTERN = re.compile(r"^[A-Za-z0-9._-]+$")


def column_of(reference):
    return re.match(r"[A-Z]+", reference).group(0)


def sheet_path(workbook, sheet_name):
    book = ET.fromstring(workbook.read("xl/workbook.xml"))
    rels = ET.fromstring(workbook.read("xl/_rels/workbook.xml.rels"))
    targets = {rel.get("Id"): rel.get("Target") for rel in rels.iter(PNS + "Relationship")}
    for sheet in book.iter(NS + "sheet"):
        if sheet.get("name") == sheet_name:
            target = targets[sheet.get(RNS + "id")].lstrip("/")
            return target if target.startswith("xl/") else "xl/" + target
    names = [sheet.get("name") for sheet in book.iter(NS + "sheet")]
    raise SystemExit(f"Sheet not found: {sheet_name!r}. Sheets: {names}")


def iter_rows(workbook, path, keep=None):
    """Yield (row number, {column: (kind, raw)}) for each row."""
    with workbook.open(path) as handle:
        for _, element in ET.iterparse(handle, events=("end",)):
            if element.tag != NS + "row":
                continue
            cells = {}
            for cell in element.iter(NS + "c"):
                column = column_of(cell.get("r"))
                if keep is not None and column not in keep:
                    continue
                kind = cell.get("t")
                if kind == "inlineStr":
                    cells[column] = ("text", "".join(t.text or "" for t in cell.iter(NS + "t")))
                    continue
                value = cell.find(NS + "v")
                if value is not None:
                    cells[column] = ("shared" if kind == "s" else "text", value.text or "")
            yield int(element.get("r", "0")), cells
            element.clear()


def shared_strings(workbook, wanted):
    found = {}
    if not wanted or "xl/sharedStrings.xml" not in workbook.namelist():
        return found
    last = max(wanted)
    index = -1
    with workbook.open("xl/sharedStrings.xml") as handle:
        for _, element in ET.iterparse(handle, events=("end",)):
            if element.tag != NS + "si":
                continue
            index += 1
            if index in wanted:
                found[index] = "".join(t.text or "" for t in element.iter(NS + "t"))
            element.clear()
            if index >= last:
                break
    return found


def cell_text(cell, strings):
    if cell is None:
        return None
    kind, raw = cell
    return strings.get(int(raw)) if kind == "shared" else raw


def village_code(text):
    if text is None:
        return ""
    text = text.strip()
    try:
        number = Decimal(text)
        if number == number.to_integral_value():
            return str(int(number))
    except InvalidOperation:
        pass
    return text


def exclusion(text):
    """Return None when the row is kept, else the reason it is excluded."""
    if text is None or text.strip() in ("", "(blank)"):
        return None
    try:
        return "value" if Decimal(text.strip()) > 0 else None
    except InvalidOperation:
        return "text"


def stable_uid(source_key):
    number = int.from_bytes(hashlib.sha256(UID_SALT + source_key.encode("utf-8")).digest(), "big")
    number, first = divmod(number, len(UID_FIRST))
    characters = [UID_FIRST[first]]
    for _ in range(10):
        number, rest = divmod(number, len(UID_REST))
        characters.append(UID_REST[rest])
    uid = "".join(characters)
    assert UID_PATTERN.match(uid)
    return uid


def new_ulid():
    value = (time.time_ns() // 1_000_000) << 80 | int.from_bytes(secrets.token_bytes(10), "big")
    characters = []
    for _ in range(26):
        value, digit = divmod(value, 32)
        characters.append(CROCKFORD[digit])
    return "".join(reversed(characters))


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--xlsx", required=True, help="workbook path")
    parser.add_argument("--sheet", required=True, help="sheet name")
    parser.add_argument("--label", required=True, help="output name, e.g. amran-hajjah-2026")
    parser.add_argument("--out-dir", default=str(Path(__file__).resolve().parents[2] / "target" / "reference"))
    args = parser.parse_args()
    if not LABEL_PATTERN.match(args.label):
        raise SystemExit("--label may contain only letters, digits, dot, dash and underscore.")

    with zipfile.ZipFile(args.xlsx) as workbook:
        path = sheet_path(workbook, args.sheet)

        header_row, header_cells = next(iter_rows(workbook, path))
        header_strings = shared_strings(
            workbook, {int(raw) for kind, raw in header_cells.values() if kind == "shared"})
        headers = {column: (cell_text(cell, header_strings) or "").strip()
                   for column, cell in header_cells.items()}

        def column_named(name):
            matches = [column for column, header in headers.items() if header.lower() == name]
            if len(matches) != 1:
                raise SystemExit(f"Expected exactly one {name!r} column, found {len(matches)}.")
            return matches[0]

        code_column = column_named(CODE_HEADER)
        name_column = column_named(NAME_HEADER)
        key_column = column_named(KEY_HEADER)
        exclude_columns = sorted(column for column, header in headers.items()
                                 if EXCLUDE_MARK in header.lower())
        if not exclude_columns:
            raise SystemExit("No exclusion column found (no header contains 'exclu').")

        keep = {code_column, name_column, key_column, *exclude_columns}
        rows = [(number, cells) for number, cells in iter_rows(workbook, path, keep) if number > header_row]
        strings = shared_strings(
            workbook, {int(raw) for _, cells in rows for kind, raw in cells.values() if kind == "shared"})

    excluded_by_column = Counter()
    excluded_rows = 0
    skipped = []
    by_key = {}
    for number, cells in rows:
        reasons = {column: exclusion(cell_text(cells.get(column), strings)) for column in exclude_columns}
        flagged = [column for column, reason in reasons.items() if reason]
        if flagged:
            excluded_rows += 1
            for column in flagged:
                excluded_by_column[(headers[column], reasons[column])] += 1
            continue

        code = village_code(cell_text(cells.get(code_column), strings))
        name = WHITESPACE.sub(" ", cell_text(cells.get(name_column), strings) or "").strip()
        key = (cell_text(cells.get(key_column), strings) or "").strip()
        reason = ("no house_uuid" if not key else "no village code" if not code
                  else "empty name" if not name else "name too long" if len(name) > MAX_NAME_LENGTH else None)
        if reason:
            skipped.append((number, reason, code, key, name))
            continue
        by_key.setdefault(key, []).append((number, code, name))

    duplicate_rows = 0
    written = []
    seen_uids = {}
    for key, group in by_key.items():
        if len({(code, name) for _, code, name in group}) > 1:
            skipped.extend((number, "house_uuid on different rows", code, key, name)
                           for number, code, name in group)
            continue
        duplicate_rows += len(group) - 1
        number, code, name = group[0]
        uid = stable_uid(key)
        if uid in seen_uids:
            raise SystemExit(f"uid collision between rows {seen_uids[uid]} and {number}; change UID_SALT.")
        seen_uids[uid] = number
        written.append((new_ulid(), uid, code, name, key))

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    output = out_dir / f"{args.label}.csv"
    skipped_output = out_dir / f"{args.label}.skipped.csv"
    with output.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(["id", "uid", "org_unit_code", "display_name", "source_key"])
        writer.writerows(written)
    with skipped_output.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(["sheet_row", "reason", "org_unit_code", "source_key", "display_name"])
        writer.writerows(skipped)

    print(f"Sheet: {args.sheet}  data rows: {len(rows)}")
    print("Exclusion columns: " + ", ".join(headers[column] for column in exclude_columns))
    print(f"Excluded rows: {excluded_rows}")
    for (header, reason), count in sorted(excluded_by_column.items()):
        print(f"  {header} ({reason}): {count}")
    print(f"Duplicate rows dropped (same house_uuid, village and name): {duplicate_rows}")
    print(f"Skipped rows: {len(skipped)}")
    for reason, count in sorted(Counter(row[1] for row in skipped).items()):
        print(f"  {reason}: {count}")
    print(f"Written: {len(written)} households, {len({row[2] for row in written})} village codes")
    print(f"Output: {output}")
    print(f"Skipped list: {skipped_output}")


if __name__ == "__main__":
    sys.exit(main())
