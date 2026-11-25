-- The feed's oracle: every value of a batch of journal heads, and the field each one is expected under. The walk is
-- the oracle of the feed spike's measure 1 (branch spike/d-feed, sql/60_agreement.sql), with the ids of contract
-- version 1 (section 5); the field of each value is resolved here, in SQL, by contract section 4's rule, apart from
-- the normalizer's code. Run by FeedAgreement (src/test/java/org/nmcpye/datarun/submission/feed/agreement),
-- read-only, one batch at a time. It needs pgcrypto's digest, which a changeset of the schema installs.
--
-- Parameters, in order: the batch's submission ids (text[]) and head versions (bigint[]), aligned.
--
-- The walk, from each head's form_data, depth first, with no form definition:
--   - an object's members are walked, except _status at the root and a row's _id, _index, _parentId and
--     _submissionUid, which are structure;
--   - the elements of a non-empty array whose elements are all objects are rows;
--   - anything else that is not the JSON null is one value, whole: a scalar, an array that is not of objects, an
--     empty array (zero rows or an empty multi-select, which the harness counts apart);
--   - a document that is not an object is one value at the empty path (contract 4).
-- A value's path is the stored keys from the root, joined by dots; arrays add nothing. A row's id is its stored _id
-- as text (a string's text, any other JSON as PostgreSQL prints it), or, when it has none or an empty one, the derived
-- id: the lower-case text of the version 5 UUID, in namespace 4b1aa021-0abd-532b-88b8-f71a9078093d, of
-- "<submission id>|<section path>|<parent row id, or nothing>|<row index>". A row id used twice in one submission is
-- left as it is: the contract's later attempts need the walk order, which this walk does not keep, so the harness
-- compares such a submission without row ids.
--
-- The field, from the stored definitions of the head's form (data_template_version.fields and .sections):
--   - a field's data path is its section's path (its stored path without the last key) and its name, or its name
--     alone outside every section; a field without a data element uid or a name defines nothing;
--   - a value at a path takes the field defined there by the pinned version (the head's template_version_uid), else
--     by the form's newest other version that defines one; the first in stored order when a version defines the
--     path twice. An empty array, which any definition holds, takes the pinned version's definition there before
--     another version's, the newest first, a field before a section. A field whose data element is gone keys
--     nothing;
--   - one value per field in each row, or outside every row: the one resolved through the pinned version, else the
--     first in walk order (keys in code point order, depth first); the others are expected unmapped. A row is told
--     apart by its place (container), not its id, which two rows may share.
--
-- Output: one line per value (is_row false; value is its jsonb text; field_id the field it is expected under, or
-- null when it is expected unmapped) and one per row (is_row true; parent_row_id the row it is nested in).
WITH RECURSIVE
batch AS (
    SELECT b.submission_id, r.form_data, s.template_uid AS form_uid, r.metadata ->> 'template_version_uid' AS pin
    FROM unnest(?::text[], ?::bigint[]) AS b(submission_id, version)
    JOIN journal.submission_revision r ON r.submission_id = b.submission_id AND r.version = b.version
    JOIN data_submission s ON s.id = b.submission_id
),
definition AS (
    SELECT t.uid AS form_uid, v.uid AS version_uid, v.version_number, 0 AS kind, f.ord, de.id AS field_id,
           CASE WHEN f.field ->> 'path' IS NULL OR f.field ->> 'path' = f.field ->> 'id'
                     OR strpos(f.field ->> 'path', '.') = 0
                THEN f.field ->> 'name'
                ELSE substring(f.field ->> 'path' FROM '^(.*)[.][^.]*$') || '.' || (f.field ->> 'name')
           END AS path
    FROM data_template t
    JOIN data_template_version v ON v.data_template_id = t.id
    CROSS JOIN LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(v.fields) = 'array' THEN v.fields END)
        WITH ORDINALITY AS f(field, ord)
    LEFT JOIN data_element de ON de.uid = f.field ->> 'id'
    WHERE t.uid IN (SELECT form_uid FROM batch)
      AND f.field ->> 'id' IS NOT NULL AND f.field ->> 'name' IS NOT NULL
    UNION ALL
    SELECT t.uid, v.uid, v.version_number, 1, s.ord, NULL, s.section ->> 'path'
    FROM data_template t
    JOIN data_template_version v ON v.data_template_id = t.id
    CROSS JOIN LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(v.sections) = 'array' THEN v.sections END)
        WITH ORDINALITY AS s(section, ord)
    WHERE t.uid IN (SELECT form_uid FROM batch) AND s.section ->> 'path' IS NOT NULL
),
w(submission_id, path, v, row_id, parent_row_id, container, is_row, depth) AS (
    SELECT b.submission_id, e.key, e.value, NULL::text, NULL::text, '', false, 1
    FROM batch b
    CROSS JOIN LATERAL jsonb_each(CASE WHEN jsonb_typeof(b.form_data) = 'object' THEN b.form_data END) e
    WHERE e.key <> '_status'
    UNION ALL
    SELECT w.submission_id, c.path, c.v, c.row_id, c.parent_row_id, c.container, c.is_row, w.depth + 1
    FROM w
    CROSS JOIN LATERAL (
        SELECT w.path || '.' || e.key AS path, e.value AS v, w.row_id, NULL::text AS parent_row_id, w.container,
               false AS is_row
        FROM jsonb_each(CASE WHEN jsonb_typeof(w.v) = 'object' THEN w.v END) e
        WHERE NOT (w.is_row AND e.key IN ('_id', '_index', '_parentId', '_submissionUid'))
        UNION ALL
        SELECT w.path, a.value,
               CASE WHEN coalesce(k.stored, '') = '' THEN (
                   SELECT encode(set_byte(set_byte(substring(d.h FROM 1 FOR 16),
                                                   6, (get_byte(d.h, 6) & 15) | 80),
                                          8, (get_byte(d.h, 8) & 63) | 128), 'hex')::uuid::text
                   FROM (SELECT digest(decode('4b1aa0210abd532b88b8f71a9078093d', 'hex')
                                       || convert_to(w.submission_id || '|' || w.path || '|'
                                                     || coalesce(w.row_id, '') || '|' || a.ord, 'UTF8'),
                                       'sha1') AS h) d)
                    ELSE k.stored
               END,
               w.row_id,
               w.container || '/' || w.path || '#' || a.ord,
               true
        FROM jsonb_array_elements(CASE WHEN jsonb_typeof(w.v) = 'array' AND jsonb_array_length(w.v) > 0
                                            AND NOT jsonb_path_exists(w.v, '$[*] ? (@.type() != "object")')
                                       THEN w.v END) WITH ORDINALITY a(value, ord)
        CROSS JOIN LATERAL (SELECT CASE jsonb_typeof(a.value -> '_id')
                                       WHEN 'string' THEN a.value ->> '_id'
                                       WHEN 'null' THEN NULL
                                       ELSE (a.value -> '_id')::text
                                   END AS stored) k
    ) c
    WHERE w.depth < 64 AND jsonb_typeof(w.v) IN ('object', 'array')
),
node AS (
    SELECT submission_id, row_id, parent_row_id, container, path, v, is_row
    FROM w
    WHERE is_row
       OR (jsonb_typeof(v) NOT IN ('object', 'null')
           AND NOT (jsonb_typeof(v) = 'array' AND jsonb_array_length(v) > 0
                    AND NOT jsonb_path_exists(v, '$[*] ? (@.type() != "object")')))
    UNION ALL
    SELECT b.submission_id, NULL, NULL, '', '', b.form_data, false
    FROM batch b
    WHERE jsonb_typeof(b.form_data) NOT IN ('object', 'null')
),
resolved AS (
    SELECT n.submission_id, n.row_id, n.parent_row_id, n.container, n.path, n.v, n.is_row, d.field_id,
           coalesce(d.version_uid = b.pin, false) AS from_pin
    FROM node n
    JOIN batch b ON b.submission_id = n.submission_id
    LEFT JOIN LATERAL (
        SELECT x.field_id, x.version_uid
        FROM definition x
        WHERE NOT n.is_row AND x.form_uid = b.form_uid AND x.path = n.path
          AND (x.kind = 0 OR n.v = '[]'::jsonb)
        ORDER BY x.version_uid = b.pin DESC, x.version_number DESC, x.kind, x.ord
        LIMIT 1) d ON true
)
SELECT submission_id, row_id, parent_row_id, path, CASE WHEN is_row THEN NULL ELSE v::text END AS value, is_row,
       CASE WHEN field_id IS NOT NULL
                 AND row_number() OVER (PARTITION BY submission_id, container, field_id
                                        ORDER BY from_pin DESC, string_to_array(path, '.') COLLATE "C") = 1
            THEN field_id END AS field_id
FROM resolved
