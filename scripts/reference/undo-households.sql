-- Undo the load of one extracted household CSV (read from psql's standard
-- input): delete the Reference entries whose uid is in the file, that sit on
-- the file row's own village (assigned in the activity) and that an import
-- created. Entries of other files and names teams added are never touched.
-- Use it only until a phone has synced those villages: phones keep entries
-- the server deletes.
-- Variables: activity_uid, apply (true commits, anything else rolls back).
\set ON_ERROR_STOP on
\pset footer off
\pset tuples_only on
\pset format unaligned
\pset fieldsep ': '

BEGIN;

CREATE TEMP TABLE import_household (
    id            varchar(26)  NOT NULL,
    uid           varchar(11)  NOT NULL,
    org_unit_code text         NOT NULL,
    display_name  varchar(255) NOT NULL,
    source_key    text         NOT NULL
) ON COMMIT DROP;

\copy import_household FROM pstdin WITH (FORMAT csv, HEADER true)

CREATE TEMP TABLE campaign_village ON COMMIT DROP AS
SELECT DISTINCT ou.id, ou.code
FROM assignment a
JOIN activity ac ON ac.id = a.activity_id
JOIN org_unit ou ON ou.id = a.org_unit_id
WHERE ac.uid = :'activity_uid'
  AND NOT coalesce(a.deleted, false);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM campaign_village) THEN
        RAISE EXCEPTION 'The activity has no assigned villages; check --activity.';
    END IF;
    IF EXISTS (SELECT 1 FROM import_household GROUP BY uid HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'The file repeats a uid; re-run the extractor.';
    END IF;
END $$;

CREATE TEMP TABLE file_entry ON COMMIT DROP AS
SELECT r.id
FROM import_household i
JOIN campaign_village v ON v.code = i.org_unit_code
JOIN reference_entry r ON r.uid = i.uid AND r.org_unit_id = v.id
WHERE r.created_by = 'reference-import';

SELECT 'rows in file', count(*) FROM import_household;
SELECT 'rows on campaign villages', count(*)
FROM import_household i JOIN campaign_village v ON v.code = i.org_unit_code;
SELECT 'file uids in the database', count(*)
FROM import_household i JOIN reference_entry r ON r.uid = i.uid;
SELECT '  on their own village and loaded by an import (to delete)', count(*) FROM file_entry;
SELECT '  on another village or added by a team (kept)', count(*)
FROM import_household i JOIN reference_entry r ON r.uid = i.uid
WHERE NOT EXISTS (SELECT 1 FROM file_entry f WHERE f.id = r.id);
SELECT 'entries loaded by an import, not from this file (kept)', count(*)
FROM reference_entry r
WHERE r.created_by = 'reference-import'
  AND NOT EXISTS (SELECT 1 FROM file_entry f WHERE f.id = r.id);

WITH deleted AS (
    DELETE FROM reference_entry r
    USING file_entry f
    WHERE r.id = f.id
    RETURNING r.org_unit_id
)
SELECT 'deleted', count(*) || ' households on ' || count(DISTINCT org_unit_id) || ' villages'
FROM deleted;

\if :apply
COMMIT;
\echo 'Committed.'
\else
ROLLBACK;
\echo 'Dry run: rolled back. Re-run with --apply to delete.'
\endif
