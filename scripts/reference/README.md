# Household list import (Reference entries)

The server has no API to load a Reference list: entries are otherwise
created only by phone uploads. These scripts load a campaign's household
list into `reference_entry`, scoped to the villages of one activity.

## Steps

1. Extract one sheet to a CSV (standard library only; names go to
   `target/reference/`, which is gitignored, never to the terminal):

   ```bash
   python3 -I scripts/reference/extract-households.py \
       --xlsx "/path/to/planning.xlsx" \
       --sheet "على مستوى المنزل" \
       --label amran-hajjah-2026
   ```

   Columns are found by header: `ppcc` (village code), `hname` (name),
   `house_uuid` (stable source key) and every header containing `exclu`.
   A row is excluded when any exclusion column holds a number above zero.
   Review `target/reference/<label>.skipped.csv` (rows with no key, no code,
   no name, or a name over 255 characters).

   Options, all off by default:

   - `--code-header NAME` reads the village code from another column, for
     example `ppcupdated`.
   - `--skip-key KEY` leaves out the row whose `house_uuid` is KEY, for a row
     a review deleted that no exclusion column marks (repeatable).
   - `--recode OLD=NEW` writes village code NEW where the sheet has OLD
     (repeatable).
   - `--villages FILE` writes only rows whose village code, after `--recode`,
     is listed in FILE, one code per line. The load adds rows to a village
     that already has a list, so use it to load only villages that have none.
     It applies after every other rule: a row is written or skipped exactly
     as without it, so a `house_uuid` shared with another village is still
     skipped.
   - `--drop-placeholders` leaves out a row whose name contains one of these
     words as a whole word: `نازح`, `مغترب`, `فارغ`, `خالية`, `تجريبي`,
     `مجهول`. `ة`/`ه` and `ى`/`ي` count as one letter, so `خاليه` matches
     too. Such a row is no household's name. The history extractor always
     applies the same list (`PLACEHOLDER_WORDS` in `extract-households.py`).

   A `--skip-key` or `--recode` OLD that matches no row outside the excluded
   ones stops the run, so a typo cannot pass unnoticed.

   A campaign's conformed household history (`households.tsv`) is extracted
   into the same CSV format:

   ```bash
   python3 -I scripts/reference/extract-history-households.py \
       --tsv "/path/to/history/dec2021/households.tsv" \
       --villages target/reference/villages-without-list.txt \
       --label history-dec2021
   ```

   It leaves out rows the review removed (`removed_by_review` = yes) or that
   a column whose header contains `exclu` marks (Dec 2023's
   `planning_exclude`), names containing a placeholder word (the
   `--drop-placeholders` list above, always applied here), and rows
   repeating another row of their `household_row_id` with the same
   village and name. Names, limits, uids and skip reasons follow
   `extract-households.py`. The source key, and so the uid, is the row's
   lineage `_src_file|_src_object|_src_row`; `--skip-key` takes one.
   `--recode OLD=NEW` and `--villages` work as in the loader. Always
   extract from the same history files: rebuilt files may number rows
   differently, and their rows would load as new households. The input's
   sha256 is printed for that check.

2. Dry run on staging (loads inside a transaction, prints counts, rolls back):

   ```bash
   scripts/reference/load-households.sh \
       --csv target/reference/amran-hajjah-2026.csv --activity xuWrotiHywD
   ```

3. Load on staging: right before, repeat with `--check` (below), then with
   `--apply`.

4. Production, only with explicit approval and **before users are linked to
   the campaign teams**:

   ```bash
   DATARUN_REFERENCE_PRODUCTION=true scripts/reference/load-households.sh \
       --target production \
       --csv target/reference/amran-hajjah-2026.csv --activity xuWrotiHywD
   ```

   Then, right before applying, repeat with `--check`, then with `--apply`.

   `--check` changes nothing. It counts the entries the database already
   holds on the file's villages, split into earlier imports and names teams
   added. For a file meant for villages without a list, the line "entries
   already on those villages" must read 0 right before `--apply`; otherwise
   the load adds a second list there. The dry run does not show this.

## Behaviour

- Only rows whose code matches a village assigned in the activity are loaded;
  the rest are counted as "other villages".
- Each entry's uid is derived from its source key (`house_uuid`, or a
  history row's lineage), so re-running loads only new households and later
  district lists can be added the same way. The `id` column is a new ULID on
  every extraction; the other columns repeat exactly. The loaded entries keep
  the file's ids, so only the file that was loaded can undo it (below).
- If a row on one of the activity's villages has a uid that already exists
  with a different village or name, nothing is loaded. Rows on other villages
  are neither loaded nor checked.
- Loaded entries have `created_by = 'reference-import'` and no
  `first_registered_activity_id`; entries added by teams carry the user and the
  campaign activity, so the two stay distinguishable.

## Never do these

- **Never move an entry to another village.** Phones that hold it fail their
  list sync with a scope conflict.
- **Never delete an entry phones may hold.** The next upload that uses it sends
  it back: the server recreates it as team-added, or rejects the whole upload
  when the name fails the 4-part rule.

Renaming is safe: `UPDATE reference_entry SET display_name = …` reaches phones
on their next sync. Undo a load only with `--undo` (below), never with a
`DELETE` on `created_by`, which would also remove every earlier load.

## Undo a load

Only until a phone has synced the file's villages, and never after: phones
keep the entries the server deletes (see above).

```bash
scripts/reference/load-households.sh --undo \
    --csv target/reference/<label>.csv --activity xuWrotiHywD
```

It works only with the exact file that was loaded. The load stores the
file's `id` column as each entry's id, and the undo deletes only entries that
match a file row on both id and uid, still sit on that row's village and were
created by an import (`created_by = 'reference-import'`). An earlier load's
entry with the same uid, entries moved since and names teams added therefore
stay, and the counts show them. Extracting the file again gives every row a
new id, so an undo with the new copy deletes nothing: keep the loaded file
unchanged until the undo window closes, when phones sync those villages. Dry
run unless `--apply`; production needs `DATARUN_REFERENCE_PRODUCTION=true`
and `--target production`, as for the load.

## Daily review during the campaign

Campaign users whose phone cannot show the Reference form (server 6.5.0+).
`reference_version` comes from the user's last assignments sync: 0 means an app
before 6.0.2; null means no assignments sync since the release. `app_version` is
reported by app 6.1.0+.

```sql
SELECT u.login, t.code AS team, ls.app_version, ls.reference_version, ls.last_seen
FROM team_user tu
JOIN team t ON t.id = tu.team_id
JOIN activity ac ON ac.id = t.activity_id
JOIN app_user u ON u.id = tu.user_id
LEFT JOIN user_last_seen ls ON ls.user_id = u.id
WHERE ac.uid = 'xuWrotiHywD'
  AND coalesce(ls.reference_version, 0) < 1
ORDER BY ls.last_seen DESC NULLS LAST;
```

Names added by teams, per user per day (Yemen time):

```sql
SELECT r.created_by AS login,
       ((r.created_date AT TIME ZONE 'UTC') AT TIME ZONE 'Asia/Aden')::date AS day,
       count(*) AS names_added
FROM reference_entry r
JOIN activity ac ON ac.id = r.first_registered_activity_id
WHERE ac.uid = 'xuWrotiHywD'
GROUP BY 1, 2
ORDER BY 2 DESC, 3 DESC;
```

The same household picked, or the same card number recorded, more than once in
a village:

```sql
SELECT s.org_unit_code AS village, 'household' AS kind,
       hh->>'householdreference' AS value, count(*) AS times
FROM data_submission s
CROSS JOIN LATERAL jsonb_array_elements(
    coalesce(s.form_data->'households_information'->'householdnames', '[]'::jsonb)) hh
WHERE s.activity_uid = 'xuWrotiHywD' AND NOT coalesce(s.deleted, false)
  AND hh->>'householdreference' IS NOT NULL
GROUP BY 1, 3 HAVING count(*) > 1
UNION ALL
SELECT s.org_unit_code, 'card number', hh->>'householdheadserialnumber', count(*)
FROM data_submission s
CROSS JOIN LATERAL jsonb_array_elements(
    coalesce(s.form_data->'households_information'->'householdnames', '[]'::jsonb)) hh
WHERE s.activity_uid = 'xuWrotiHywD' AND NOT coalesce(s.deleted, false)
  AND hh->>'householdheadserialnumber' IS NOT NULL
GROUP BY 1, 3 HAVING count(*) > 1
ORDER BY times DESC;
```
