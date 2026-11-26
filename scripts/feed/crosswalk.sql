-- The crosswalk (build step F10, contract 14): analytics' canonical_element_id to the feed's field_id, for re-pointing
-- downstream's existing models. One CSV line per canonical element of a field and field it maps to: the element's id,
-- the form's id, the field's id (its data element's id) and the ids of the form versions where the field sits at one of
-- the element's data paths. Several elements can map to one field (renames split today's keys); comparison.sql counts
-- any element that maps to more than one. Ids only, never a value.
--
-- An element maps to a field when one of its data paths (json_data_paths) is the field's data path in a version of the
-- same form, computed as the submission normalizer computes it: the field's section path (its stored path without the
-- last key) and its name, or its name alone outside every section. Repeating sections' elements map to no field.
--
-- That data path rule restates the Java code's (FormVersionDefinition.parse) in SQL. The copy is accepted because this
-- file is a one-time migration aid and a test pins the rule to the Java code (engineering, 2026-11-26):
-- FeedComparisonIT maps through this file a form with a field on each branch of the rule, against the data paths that
-- FormVersionDefinition.parse gives those fields, and holds comparison.sql's two copies of the rule to this file's
-- text. Change the rule in Java, and that test fails until both files follow.
--
-- Self-contained: its own read-only, repeatable-read transaction, with the integrity check's limits, rolled back. Run
-- it on the database host, detached (owner, 2026-11-26), as postgres, and fetch the file:
--
--   setsid -f sudo -n -u postgres psql -X -q -d <database> -f crosswalk.sql > feed-crosswalk.csv 2> feed-crosswalk.log < /dev/null
\set ON_ERROR_STOP 1
\set QUIET on
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SET LOCAL work_mem = '8MB';
SET LOCAL max_parallel_workers_per_gather = 0;
SET LOCAL statement_timeout = '5min';
SET LOCAL lock_timeout = '5s';
SET LOCAL idle_in_transaction_session_timeout = '2min';
SET LOCAL jit = off;
SET LOCAL temp_file_limit = '1GB';
SET LOCAL ROLE pg_read_all_data;

COPY (
    WITH field_path AS (
        SELECT t.id AS form_id, t.uid AS template_uid, v.id AS form_version_id, de.id AS field_id,
               CASE WHEN f.field ->> 'path' IS NULL OR f.field ->> 'path' = f.field ->> 'id'
                         OR strpos(f.field ->> 'path', '.') = 0
                    THEN f.field ->> 'name'
                    ELSE substring(f.field ->> 'path' FROM '^(.*)[.][^.]*$') || '.' || (f.field ->> 'name')
               END AS path
        FROM data_template t
        JOIN data_template_version v ON v.data_template_id = t.id
        CROSS JOIN LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(v.fields) = 'array' THEN v.fields END)
            AS f(field)
        JOIN data_element de ON de.uid = f.field ->> 'id'
        WHERE f.field ->> 'name' IS NOT NULL
    )
    SELECT ce.id::text AS canonical_element_id, fp.form_id, fp.field_id,
           string_agg(DISTINCT fp.form_version_id, ' ' ORDER BY fp.form_version_id) AS form_version_ids
    FROM canonical_element ce
    CROSS JOIN LATERAL jsonb_array_elements_text(
        CASE WHEN jsonb_typeof(ce.json_data_paths) = 'array' THEN ce.json_data_paths END) AS p(path)
    JOIN field_path fp ON fp.template_uid = ce.template_uid AND fp.path = p.path
    WHERE coalesce(ce.semantic_type, '') <> 'Repeat'
    GROUP BY ce.id, fp.form_id, fp.field_id
    ORDER BY fp.form_id, fp.field_id, ce.id::text
) TO STDOUT WITH (FORMAT csv, HEADER true);

ROLLBACK;
