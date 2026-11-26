-- The feed's oracle: every value of a batch of journal heads, and the field each one is expected under. The walk is
-- the oracle of the feed spike's measure 1 (branch spike/d-feed, sql/60_agreement.sql), with the ids of contract
-- version 1 (section 5); the field of each value is resolved here, in SQL, by contract section 4's rule, apart from
-- the normalizer's code. Run by FeedAgreement (src/test/java/org/nmcpye/datarun/submission/feed/agreement),
-- read-only, one batch at a time. It needs pgcrypto's digest, which a changeset of the schema installs.
--
-- Parameters, in order: the batch's submission ids (text[]) and head versions (bigint[]), aligned.
--
-- The walk, from each head's form_data, depth first:
--   - an object's members are walked, except _status at the root and a row's _id, _index, _parentId and
--     _submissionUid, which are structure;
--   - the elements of a non-empty array whose elements are all objects are rows;
--   - anything else that is not the JSON null is one value, whole: a scalar, an array that is not of objects, an
--     empty array (zero rows or an empty multi-select, which the harness counts apart);
--   - a document that is not an object is one value at the empty path (contract 4).
-- What the walk keeps whole, it decides here, from the stored definitions, by contract section 4's resolution, apart
-- from the normalizer's code: an object or rows at a path resolves, for its shape, to the first of its definitions
-- across the form's versions that can hold the shape (a field holds anything, a plain section an object, a repeating
-- section rows), the pinned version's before another's, then the newest version first, a field before a section, then
-- the version's stored order. An object is walked into only when no version defines its path or it resolves to a plain
-- section, rows only when they resolve to a repeating section, and neither when 32 objects and rows already enclose
-- it. Anything else stays one value, whole (is_whole): an object at a field, rows at a path no version defines, and
-- the like; the walk never looks inside it, under a field when its resolution is a field, unmapped otherwise.
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
-- null when it is expected unmapped; is_whole for a value kept whole) and one per row (is_row true; parent_row_id the
-- row it is nested in).
WITH RECURSIVE
batch AS (
    SELECT b.submission_id, r.form_data, s.template_uid AS form_uid, r.metadata ->> 'template_version_uid' AS pin
    FROM unnest(?::text[], ?::bigint[]) AS b(submission_id, version)
    JOIN journal.submission_revision r ON r.submission_id = b.submission_id AND r.version = b.version
    JOIN data_submission s ON s.id = b.submission_id
),
definition AS (
    SELECT t.uid AS form_uid, v.uid AS version_uid, v.version_number, 0 AS kind, f.ord, de.id AS field_id,
           NULL::boolean AS repeating,
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
    SELECT t.uid, v.uid, v.version_number, 1, s.ord, NULL,
           CASE jsonb_typeof(s.section -> 'repeatable')
               WHEN 'boolean' THEN (s.section ->> 'repeatable')::boolean
               WHEN 'string' THEN btrim(s.section ->> 'repeatable') = 'true'
               WHEN 'number' THEN (s.section ->> 'repeatable')::numeric <> 0
               ELSE false
           END,
           s.section ->> 'path'
    FROM data_template t
    JOIN data_template_version v ON v.data_template_id = t.id
    CROSS JOIN LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(v.sections) = 'array' THEN v.sections END)
        WITH ORDINALITY AS s(section, ord)
    WHERE t.uid IN (SELECT form_uid FROM batch) AND s.section ->> 'path' IS NOT NULL
),
by_path AS (
    SELECT d.form_uid, d.path,
           array_agg(d.version_uid ORDER BY d.version_number DESC, d.kind, d.ord) AS version_uids,
           array_agg(d.kind = 0 ORDER BY d.version_number DESC, d.kind, d.ord) AS fields,
           array_agg(d.repeating ORDER BY d.version_number DESC, d.kind, d.ord) AS repeatings,
           array_agg(d.field_id ORDER BY d.version_number DESC, d.kind, d.ord) AS field_ids
    FROM definition d
    GROUP BY d.form_uid, d.path
),
-- nd: how many objects and rows enclose a value, as the normalizer counts them (a row's members at its rows' depth);
-- descend: whether the walk goes into an object or rows, by the definitions; a row is always walked into
w(submission_id, form_uid, pin, path, v, row_id, parent_row_id, container, is_row, depth, nd, descend, shaped,
  whole_field_id, whole_from_pin) AS (
    SELECT b.submission_id, b.form_uid, b.pin, e.key, e.value, NULL::text, NULL::text, '', false, 1, 0,
           k.descend, k.shaped, k.whole_field_id, k.whole_from_pin
    FROM batch b
    CROSS JOIN LATERAL jsonb_each(CASE WHEN jsonb_typeof(b.form_data) = 'object' THEN b.form_data END) e
    CROSS JOIN LATERAL (
        SELECT CASE
                   WHEN false THEN true
                   WHEN sh.shape = 'OBJECT' THEN 0 < 32
                       AND (p.path IS NULL OR coalesce(NOT ch.is_field AND NOT ch.repeating, false))
                   WHEN sh.shape = 'ROWS' THEN 0 < 32 AND coalesce(NOT ch.is_field AND ch.repeating, false)
                   ELSE false
               END AS descend,
               sh.shape IN ('OBJECT', 'ROWS') AS shaped,
               CASE WHEN ch.is_field THEN ch.field_id END AS whole_field_id,
               coalesce(ch.version_uid = b.pin, false) AS whole_from_pin
        FROM (SELECT CASE WHEN jsonb_typeof(e.value) = 'object' THEN 'OBJECT'
                          WHEN jsonb_typeof(e.value) = 'array' AND jsonb_array_length(e.value) > 0
                               AND NOT jsonb_path_exists(e.value, '$[*] ? (@.type() != "object")') THEN 'ROWS'
                          ELSE 'LEAF' END AS shape) sh
        LEFT JOIN by_path p ON p.form_uid = b.form_uid AND p.path = e.key
        LEFT JOIN LATERAL (
            SELECT c.version_uid, c.is_field, c.repeating, c.field_id
            FROM unnest(p.version_uids, p.fields, p.repeatings, p.field_ids)
                WITH ORDINALITY AS c(version_uid, is_field, repeating, field_id, n)
            ORDER BY (CASE sh.shape WHEN 'OBJECT' THEN c.is_field OR NOT c.repeating
                                    ELSE c.is_field OR c.repeating END)::int * 2
                     + (c.version_uid IS NOT DISTINCT FROM b.pin)::int DESC,
                     c.n
            LIMIT 1) ch ON true
    ) k
    WHERE e.key <> '_status'
    UNION ALL
    SELECT w.submission_id, w.form_uid, w.pin, c.path, c.v, c.row_id, c.parent_row_id, c.container, c.is_row,
           w.depth + 1, c.nd, k.descend, k.shaped, k.whole_field_id, k.whole_from_pin
    FROM w
    CROSS JOIN LATERAL (
        SELECT w.path || '.' || e.key AS path, e.value AS v, w.row_id, NULL::text AS parent_row_id, w.container,
               false AS is_row, CASE WHEN w.is_row THEN w.nd ELSE w.nd + 1 END AS nd
        FROM jsonb_each(CASE WHEN jsonb_typeof(w.v) = 'object' AND w.descend THEN w.v END) e
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
               true,
               w.nd + 1
        FROM jsonb_array_elements(CASE WHEN jsonb_typeof(w.v) = 'array' AND jsonb_array_length(w.v) > 0
                                            AND NOT jsonb_path_exists(w.v, '$[*] ? (@.type() != "object")')
                                            AND w.descend
                                       THEN w.v END) WITH ORDINALITY a(value, ord)
        CROSS JOIN LATERAL (SELECT CASE jsonb_typeof(a.value -> '_id')
                                       WHEN 'string' THEN a.value ->> '_id'
                                       WHEN 'null' THEN NULL
                                       ELSE (a.value -> '_id')::text
                                   END AS stored) k
    ) c
    CROSS JOIN LATERAL (
        SELECT CASE
                   WHEN c.is_row THEN true
                   WHEN sh.shape = 'OBJECT' THEN c.nd < 32
                       AND (p.path IS NULL OR coalesce(NOT ch.is_field AND NOT ch.repeating, false))
                   WHEN sh.shape = 'ROWS' THEN c.nd < 32 AND coalesce(NOT ch.is_field AND ch.repeating, false)
                   ELSE false
               END AS descend,
               sh.shape IN ('OBJECT', 'ROWS') AS shaped,
               CASE WHEN ch.is_field THEN ch.field_id END AS whole_field_id,
               coalesce(ch.version_uid = w.pin, false) AS whole_from_pin
        FROM (SELECT CASE WHEN jsonb_typeof(c.v) = 'object' THEN 'OBJECT'
                          WHEN jsonb_typeof(c.v) = 'array' AND jsonb_array_length(c.v) > 0
                               AND NOT jsonb_path_exists(c.v, '$[*] ? (@.type() != "object")') THEN 'ROWS'
                          ELSE 'LEAF' END AS shape) sh
        LEFT JOIN by_path p ON p.form_uid = w.form_uid AND p.path = c.path
        LEFT JOIN LATERAL (
            SELECT c.version_uid, c.is_field, c.repeating, c.field_id
            FROM unnest(p.version_uids, p.fields, p.repeatings, p.field_ids)
                WITH ORDINALITY AS c(version_uid, is_field, repeating, field_id, n)
            ORDER BY (CASE sh.shape WHEN 'OBJECT' THEN c.is_field OR NOT c.repeating
                                    ELSE c.is_field OR c.repeating END)::int * 2
                     + (c.version_uid IS NOT DISTINCT FROM w.pin)::int DESC,
                     c.n
            LIMIT 1) ch ON true
    ) k
    WHERE w.depth < 64 AND jsonb_typeof(w.v) IN ('object', 'array')
),
node AS (
    SELECT submission_id, row_id, parent_row_id, container, path, v, is_row, shaped AND NOT descend AS is_whole,
           whole_field_id, whole_from_pin
    FROM w
    WHERE is_row OR (jsonb_typeof(v) <> 'null' AND NOT (shaped AND descend))
    UNION ALL
    SELECT b.submission_id, NULL, NULL, '', '', b.form_data, false, false, NULL, false
    FROM batch b
    WHERE jsonb_typeof(b.form_data) NOT IN ('object', 'null')
),
resolved AS (
    SELECT n.submission_id, n.row_id, n.parent_row_id, n.container, n.path, n.v, n.is_row, n.is_whole,
           CASE WHEN n.is_whole THEN n.whole_field_id ELSE d.field_id END AS field_id,
           CASE WHEN n.is_whole THEN n.whole_from_pin ELSE coalesce(d.version_uid = b.pin, false) END AS from_pin
    FROM node n
    JOIN batch b ON b.submission_id = n.submission_id
    LEFT JOIN LATERAL (
        SELECT x.field_id, x.version_uid
        FROM definition x
        WHERE NOT n.is_row AND NOT n.is_whole AND x.form_uid = b.form_uid AND x.path = n.path
          AND (x.kind = 0 OR n.v = '[]'::jsonb)
        ORDER BY x.version_uid = b.pin DESC, x.version_number DESC, x.kind, x.ord
        LIMIT 1) d ON true
)
SELECT submission_id, row_id, parent_row_id, path, CASE WHEN is_row THEN NULL ELSE v::text END AS value, is_row,
       is_whole,
       CASE WHEN field_id IS NOT NULL
                 AND row_number() OVER (PARTITION BY submission_id, container, field_id
                                        ORDER BY from_pin DESC, string_to_array(path, '.') COLLATE "C") = 1
            THEN field_id END AS field_id
FROM resolved
