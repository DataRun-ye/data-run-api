-- Import one extracted household CSV (read from psql's standard input) as
-- Reference entries for the villages of one activity.
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

SELECT 'rows in file', count(*) FROM import_household;
SELECT 'rows on campaign villages', count(*)
FROM import_household i JOIN campaign_village v ON v.code = i.org_unit_code;
SELECT 'rows on other villages (not loaded)', count(*)
FROM import_household i LEFT JOIN campaign_village v ON v.code = i.org_unit_code
WHERE v.id IS NULL;
SELECT 'campaign villages covered by file', count(DISTINCT v.id)
FROM import_household i JOIN campaign_village v ON v.code = i.org_unit_code;
SELECT 'already loaded (same village and name)', count(*)
FROM import_household i
JOIN campaign_village v ON v.code = i.org_unit_code
JOIN reference_entry r ON r.uid = i.uid
WHERE r.org_unit_id = v.id AND r.display_name = i.display_name;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM import_household i
        JOIN campaign_village v ON v.code = i.org_unit_code
        JOIN reference_entry r ON r.uid = i.uid
        WHERE r.org_unit_id <> v.id OR r.display_name <> i.display_name
    ) THEN
        RAISE EXCEPTION 'Some uids already exist with a different village or name; nothing was loaded.';
    END IF;
END $$;

WITH inserted AS (
    INSERT INTO reference_entry (
        id, uid, display_name, org_unit_id, first_registered_activity_id,
        created_by, last_modified_by, created_date, last_modified_date)
    SELECT i.id, i.uid, i.display_name, v.id, NULL,
           'reference-import', 'reference-import',
           now() AT TIME ZONE 'UTC', now() AT TIME ZONE 'UTC'
    FROM import_household i
    JOIN campaign_village v ON v.code = i.org_unit_code
    ON CONFLICT (uid) DO NOTHING
    RETURNING org_unit_id
)
SELECT 'inserted', count(*) || ' households on ' || count(DISTINCT org_unit_id) || ' villages'
FROM inserted;

\if :apply
COMMIT;
\echo 'Committed.'
\else
ROLLBACK;
\echo 'Dry run: rolled back. Re-run with --apply to load.'
\endif
