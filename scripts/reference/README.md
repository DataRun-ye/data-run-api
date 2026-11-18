# Household list import (Reference entries)

The server has no API to load a Reference list: entries are otherwise
created only by phone uploads. These two scripts load a campaign's household
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

2. Dry run on staging (loads inside a transaction, prints counts, rolls back):

   ```bash
   scripts/reference/load-households.sh \
       --csv target/reference/amran-hajjah-2026.csv --activity xuWrotiHywD
   ```

3. Load on staging: repeat with `--apply`.

4. Production, only with explicit approval and **before users are linked to
   the campaign teams**:

   ```bash
   DATARUN_REFERENCE_PRODUCTION=true scripts/reference/load-households.sh \
       --target production \
       --csv target/reference/amran-hajjah-2026.csv --activity xuWrotiHywD
   ```

   Then repeat with `--apply`.

## Behaviour

- Only rows whose code matches a village assigned in the activity are loaded;
  the rest are counted as "other villages".
- Each entry's uid is derived from `house_uuid`, so re-running loads only new
  households and later district lists can be added the same way.
- If a uid already exists with a different village or name, nothing is loaded.
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
on their next sync. Undo a load only before any phone has synced:
`DELETE FROM reference_entry WHERE created_by = 'reference-import'`.

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
