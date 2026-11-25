-- The feed's oracle: every value of a batch of journal heads, read with no form definition. It is the oracle of the
-- feed spike's measure 1 (branch spike/d-feed, sql/60_agreement.sql), with the ids of contract version 1 (section 5)
-- instead of the spike's uuids. Run by FeedAgreement (src/test/java/org/nmcpye/datarun/submission/feed/agreement),
-- read-only, one batch at a time. It needs pgcrypto's digest, which a changeset of the schema installs.
--
-- Parameters, in order: the batch's submission ids (text[]) and head versions (bigint[]), aligned.
--
-- The walk, from each head's form_data, depth first:
--   - an object's members are walked, except _status at the root and a row's _id, _index, _parentId and
--     _submissionUid, which are structure;
--   - the elements of a non-empty array whose elements are all objects are rows;
--   - anything else that is not the JSON null is one value, whole: a scalar, an array that is not of objects, an
--     empty array (counted apart: zero rows or an empty multi-select, which only a definition can tell);
--   - a document that is not an object is one value at the empty path (contract 4).
-- A value's path is the stored keys from the root, joined by dots; arrays add nothing. A row's id is its stored _id
-- as text (a string's text, any other JSON as PostgreSQL prints it), or, when it has none or an empty one, the derived
-- id: the lower-case text of the version 5 UUID, in namespace 4b1aa021-0abd-532b-88b8-f71a9078093d, of
-- "<submission id>|<section path>|<parent row id, or nothing>|<row index>". A row id used twice in one submission is
-- left as it is: the contract's later attempts need the walk order, which this walk does not keep, so the harness
-- compares such a submission without row ids.
--
-- Output: one line per value (is_row false; value is its jsonb text) and one per row (is_row true; no value).
WITH RECURSIVE
batch AS (
    SELECT b.submission_id, r.form_data
    FROM unnest(?::text[], ?::bigint[]) AS b(submission_id, version)
    JOIN journal.submission_revision r ON r.submission_id = b.submission_id AND r.version = b.version
),
w(submission_id, path, v, row_id, is_row, depth) AS (
    SELECT b.submission_id, e.key, e.value, NULL::text, false, 1
    FROM batch b
    CROSS JOIN LATERAL jsonb_each(CASE WHEN jsonb_typeof(b.form_data) = 'object' THEN b.form_data END) e
    WHERE e.key <> '_status'
    UNION ALL
    SELECT w.submission_id, c.path, c.v, c.row_id, c.is_row, w.depth + 1
    FROM w
    CROSS JOIN LATERAL (
        SELECT w.path || '.' || e.key AS path, e.value AS v, w.row_id, false AS is_row
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
)
SELECT submission_id, row_id, path, CASE WHEN is_row THEN NULL ELSE v::text END AS value, is_row
FROM w
WHERE is_row
   OR (jsonb_typeof(v) NOT IN ('object', 'null')
       AND NOT (jsonb_typeof(v) = 'array' AND jsonb_array_length(v) > 0
                AND NOT jsonb_path_exists(v, '$[*] ? (@.type() != "object")')))
UNION ALL
SELECT b.submission_id, NULL, '', b.form_data::text, false
FROM batch b
WHERE jsonb_typeof(b.form_data) NOT IN ('object', 'null')
