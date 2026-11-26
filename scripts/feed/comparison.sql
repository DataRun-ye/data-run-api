-- The feed's comparison queries (build step F10, contract Rollout, phase B): counts that set the feed beside what
-- downstream reads today, so that feed versions of existing models can be checked against the server. Counts and ids
-- only, never a value; dated by the run.
--
-- Self-contained: it opens its own read-only, repeatable-read transaction (one snapshot), with the integrity check's
-- limits, and rolls back. Run it on the database host, detached (owner, 2026-11-26: anything that runs against the lab
-- runs on a lab host), as postgres (temp_file_limit needs a superuser; the statements then run as pg_read_all_data):
--
--   setsid -f sudo -n -u postgres psql -X -q -d <database> -f comparison.sql > feed-comparison.txt 2> feed-comparison.log < /dev/null
--
-- then fetch feed-comparison.txt. Every line is "kind|...|counts", in this order:
--   run|<time of the snapshot, UTC>
--   submissions|<form id>|<day>|<stored live>|<in the feed, live>|<with values in tall>
--       live submissions per form and day of creation (data_submission.created_date): stored, as feed records not
--       deleted, and with any row in analytics.tall_canonical; a submission whose stored form is unknown has form id -
--   rows|<form id>|<section id>|<in the feed, live>|<repeat events, live>
--       live rows per form and repeating section: feed row records not deleted, of feed records not deleted, against
--       analytics.events of type repeat not deleted, of live submissions. An event's section is that of its repeating
--       canonical element's data paths, by the feed's section id recipe (contract 5); an element with several paths
--       counts under each
--   values|<form id>|<field id>|<in the feed, live>|<in tall, through the crosswalk>
--       values per field: keys of field_values in live feed records and their live rows, against rows of
--       analytics.tall_canonical of live submissions whose canonical element maps to the field (crosswalk.sql)
--   crosswalk|<what>|<count>
--       canonical elements of a field mapping to no field, and to more than one (expected 0), and fields with no element
-- The contract's fourth count, the first-time values of section 14, comes with build step F12.
--
-- The section id recipe needs pgcrypto's digest, which a changeset of the schema installs.
\set ON_ERROR_STOP 1
\set QUIET on
\pset format unaligned
\pset tuples_only on
\pset footer off
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SET LOCAL work_mem = '8MB';
SET LOCAL max_parallel_workers_per_gather = 0;
SET LOCAL statement_timeout = '5min';
SET LOCAL lock_timeout = '5s';
SET LOCAL idle_in_transaction_session_timeout = '2min';
SET LOCAL jit = off;
SET LOCAL temp_file_limit = '1GB';
SET LOCAL ROLE pg_read_all_data;

SELECT 'run|' || to_char(now() AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"');

-- Live submissions per form and day: stored, in the feed, with values in tall
WITH stored AS (
    SELECT s.id, coalesce(t.id, '-') AS form_id, s.created_date::date AS day
    FROM data_submission s
    LEFT JOIN data_template t ON t.uid = s.template_uid
    WHERE NOT coalesce(s.deleted, false)
),
in_tall AS (
    SELECT DISTINCT x.submission_id AS id FROM analytics.tall_canonical x WHERE x.submission_id IS NOT NULL
)
SELECT 'submissions|' || st.form_id || '|' || st.day || '|' || count(*)
       || '|' || count(*) FILTER (WHERE f.id IS NOT NULL AND NOT f.deleted)
       || '|' || count(*) FILTER (WHERE tl.id IS NOT NULL)
FROM stored st
LEFT JOIN feed.submission f ON f.id = st.id
LEFT JOIN in_tall tl ON tl.id = st.id
GROUP BY st.form_id, st.day
ORDER BY st.form_id, st.day;

-- Live rows per form and repeating section: the feed against repeat events
WITH forms AS (
    SELECT t.id AS form_id, t.uid AS template_uid FROM data_template t
),
event_rows AS (
    SELECT fm.form_id, p.path, count(*) AS n
    FROM analytics.events e
    JOIN data_submission s ON s.id = e.submission_id AND NOT coalesce(s.deleted, false)
    JOIN forms fm ON fm.template_uid = e.template_uid
    JOIN canonical_element ce ON ce.id::text = e.event_ce_id::text
    CROSS JOIN LATERAL jsonb_array_elements_text(
        CASE WHEN jsonb_typeof(ce.json_data_paths) = 'array' THEN ce.json_data_paths END) AS p(path)
    WHERE e.event_type = 'repeat' AND e.deleted_at IS NULL
    GROUP BY fm.form_id, p.path
),
event_sections AS (
    SELECT r.form_id, r.n,
           encode(set_byte(set_byte(substring(d.h FROM 1 FOR 16), 6, (get_byte(d.h, 6) & 15) | 80),
                           8, (get_byte(d.h, 8) & 63) | 128), 'hex')::uuid::text AS section_id
    FROM event_rows r
    CROSS JOIN LATERAL (SELECT digest(decode('4b1aa0210abd532b88b8f71a9078093d', 'hex')
                                      || convert_to(r.form_id || '|section|' || r.path, 'UTF8'), 'sha1') AS h) d
),
feed_rows AS (
    SELECT f.form_id, x.section_id, count(*) AS n
    FROM feed.submission_row x
    JOIN feed.submission f ON f.id = x.submission_id
    WHERE NOT x.deleted AND NOT f.deleted
    GROUP BY f.form_id, x.section_id
)
SELECT 'rows|' || coalesce(fr.form_id, ev.form_id) || '|' || coalesce(fr.section_id, ev.section_id)
       || '|' || coalesce(fr.n, 0) || '|' || coalesce(ev.n, 0)
FROM feed_rows fr
FULL JOIN (SELECT form_id, section_id, sum(n) AS n FROM event_sections GROUP BY form_id, section_id) ev
    ON ev.form_id = fr.form_id AND ev.section_id = fr.section_id
ORDER BY 1;

-- Values per field: the feed against tall, through the crosswalk
WITH field_path AS (
    SELECT t.id AS form_id, t.uid AS template_uid, de.id AS field_id,
           CASE WHEN f.field ->> 'path' IS NULL OR f.field ->> 'path' = f.field ->> 'id'
                     OR strpos(f.field ->> 'path', '.') = 0
                THEN f.field ->> 'name'
                ELSE substring(f.field ->> 'path' FROM '^(.*)[.][^.]*$') || '.' || (f.field ->> 'name')
           END AS path
    FROM data_template t
    JOIN data_template_version v ON v.data_template_id = t.id
    CROSS JOIN LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(v.fields) = 'array' THEN v.fields END) AS f(field)
    JOIN data_element de ON de.uid = f.field ->> 'id'
    WHERE f.field ->> 'name' IS NOT NULL
),
crosswalk AS (
    SELECT DISTINCT ce.id::text AS canonical_element_id, fp.form_id, fp.field_id
    FROM canonical_element ce
    CROSS JOIN LATERAL jsonb_array_elements_text(
        CASE WHEN jsonb_typeof(ce.json_data_paths) = 'array' THEN ce.json_data_paths END) AS p(path)
    JOIN field_path fp ON fp.template_uid = ce.template_uid AND fp.path = p.path
    WHERE coalesce(ce.semantic_type, '') <> 'Repeat'
),
feed_values AS (
    SELECT f.form_id, k.field_id, count(*) AS n
    FROM feed.submission f
    CROSS JOIN LATERAL jsonb_object_keys(f.field_values) AS k(field_id)
    WHERE NOT f.deleted
    GROUP BY f.form_id, k.field_id
    UNION ALL
    SELECT f.form_id, k.field_id, count(*)
    FROM feed.submission_row x
    JOIN feed.submission f ON f.id = x.submission_id
    CROSS JOIN LATERAL jsonb_object_keys(x.field_values) AS k(field_id)
    WHERE NOT x.deleted AND NOT f.deleted
    GROUP BY f.form_id, k.field_id
),
tall_values AS (
    SELECT c.form_id, c.field_id, count(*) AS n
    FROM analytics.tall_canonical x
    JOIN data_submission s ON s.id = x.submission_id AND NOT coalesce(s.deleted, false)
    JOIN crosswalk c ON c.canonical_element_id = x.canonical_element_id::text
    GROUP BY c.form_id, c.field_id
),
feed_totals AS (
    SELECT form_id, field_id, sum(n) AS n FROM feed_values GROUP BY form_id, field_id
)
SELECT 'values|' || coalesce(fv.form_id, tv.form_id) || '|' || coalesce(fv.field_id, tv.field_id)
       || '|' || coalesce(fv.n, 0) || '|' || coalesce(tv.n, 0)
FROM feed_totals fv
FULL JOIN tall_values tv ON tv.form_id = fv.form_id AND tv.field_id = fv.field_id
ORDER BY 1;

-- The crosswalk's own counts
WITH field_path AS (
    SELECT t.id AS form_id, t.uid AS template_uid, de.id AS field_id,
           CASE WHEN f.field ->> 'path' IS NULL OR f.field ->> 'path' = f.field ->> 'id'
                     OR strpos(f.field ->> 'path', '.') = 0
                THEN f.field ->> 'name'
                ELSE substring(f.field ->> 'path' FROM '^(.*)[.][^.]*$') || '.' || (f.field ->> 'name')
           END AS path
    FROM data_template t
    JOIN data_template_version v ON v.data_template_id = t.id
    CROSS JOIN LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(v.fields) = 'array' THEN v.fields END) AS f(field)
    JOIN data_element de ON de.uid = f.field ->> 'id'
    WHERE f.field ->> 'name' IS NOT NULL
),
element AS (
    SELECT ce.id::text AS canonical_element_id, ce.template_uid,
           CASE WHEN jsonb_typeof(ce.json_data_paths) = 'array' THEN ce.json_data_paths END AS paths
    FROM canonical_element ce
    WHERE coalesce(ce.semantic_type, '') <> 'Repeat'
),
mapped AS (
    SELECT e.canonical_element_id, count(DISTINCT fp.field_id) AS fields
    FROM element e
    LEFT JOIN LATERAL jsonb_array_elements_text(e.paths) AS p(path) ON true
    LEFT JOIN field_path fp ON fp.template_uid = e.template_uid AND fp.path = p.path
    GROUP BY e.canonical_element_id
),
unmatched_fields AS (
    SELECT count(*) AS n
    FROM (SELECT DISTINCT fp.form_id, fp.field_id FROM field_path fp) f
    WHERE NOT EXISTS (
        SELECT 1 FROM field_path fp
        JOIN element e ON e.template_uid = fp.template_uid
        CROSS JOIN LATERAL jsonb_array_elements_text(e.paths) AS p(path)
        WHERE fp.form_id = f.form_id AND fp.field_id = f.field_id AND p.path = fp.path)
)
SELECT 'crosswalk|elements of a field mapping to no field|' || count(*) FILTER (WHERE m.fields = 0) FROM mapped m
UNION ALL
SELECT 'crosswalk|elements of a field mapping to more than one field (expected 0)|'
       || count(*) FILTER (WHERE m.fields > 1) FROM mapped m
UNION ALL
SELECT 'crosswalk|fields no element maps to|' || n FROM unmatched_fields;

ROLLBACK;
