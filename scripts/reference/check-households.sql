-- Before loading one extracted household CSV (read from psql's standard
-- input), count what the database already holds on the file's villages
-- (those assigned in the activity). A file for villages without a list must
-- find no entries there. Read-only: always rolls back.
-- Variables: activity_uid.
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
END $$;

CREATE TEMP TABLE file_village ON COMMIT DROP AS
SELECT DISTINCT v.id
FROM import_household i
JOIN campaign_village v ON v.code = i.org_unit_code;

SELECT 'villages in file', count(DISTINCT org_unit_code) FROM import_household;
SELECT 'villages in file assigned in the activity', count(*) FROM file_village;
SELECT 'entries already on those villages (0 before loading villages without a list)', count(*)
FROM reference_entry r JOIN file_village f ON f.id = r.org_unit_id;
SELECT '  loaded by an import', count(*)
FROM reference_entry r JOIN file_village f ON f.id = r.org_unit_id
WHERE r.created_by = 'reference-import';
SELECT '  added by teams', count(*)
FROM reference_entry r JOIN file_village f ON f.id = r.org_unit_id
WHERE r.created_by <> 'reference-import';
SELECT '  villages that have entries', count(DISTINCT r.org_unit_id)
FROM reference_entry r JOIN file_village f ON f.id = r.org_unit_id;
SELECT 'file uids already in the database', count(*)
FROM import_household i JOIN reference_entry r ON r.uid = i.uid;

ROLLBACK;
\echo 'Read-only check: nothing was changed.'
