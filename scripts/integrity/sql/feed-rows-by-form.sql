-- Check 12f, one form at a time: records at their head's version whose live row records differ in number from the
-- head's rows (check 12, 12-feed.sql). The runner sets :tmpl to a form's uid (data_submission.template_uid) and runs
-- this once per form, as it runs the payload walks, so each walk holds one form's published heads, not the whole
-- feed's. 12-feed.sql counts the records whose submission has no form.
--
-- It finds a head's rows the way the submission normalizer does, from the stored form definitions, apart from its
-- code: walking the document from its root, an object is walked when no version defines its path or when its path
-- resolves to a plain section; a non-empty array of objects is rows when its path resolves to a repeating section, and
-- each row's keys are walked, its _id, _index, _parentId and _submissionUid aside; the root's _status is skipped; a JSON
-- null is nothing; nothing deeper than 32 objects and rows is walked. A path resolves, for the value's shape, to the
-- first of its definitions across the form's versions that can hold the shape (a field holds anything, a plain section
-- an object, a repeating section rows), the pinned version's before another's, then the newest version first, a field
-- before a section, then the version's stored order. A definition's path: a field's is its stored path without the
-- last key, then its name (its name alone outside every section); a section's is its stored path.
--
-- On a database without the feed's tables it prints nothing: 12-feed.sql prints 12f as -1.
--
-- Each head is walked on its own, beside its record (a lateral walk), so the statement holds one document's walk at a
-- time and only its time grows with the form: the walk of the whole form at once wrote past the check's 1 GB of
-- temporary files at 50,000 published heads of invented documents (130 nodes, 24 rows each). Only rows count, so only
-- objects and arrays are looked at (a scalar or a null is never walked into, nor rows), and leaves are not kept.
--
-- Cost: one pass over the form's published records, with probes into data_submission, the journal and the row
-- records, and one walk per head, joined to the form's definitions by data path. On those invented documents, about
-- 0.8 ms a head and no temporary files (50,000 heads in 39 s, 200,000 in 165 s, on a laptop, 2026-11-26): one form's run
-- meets the 5-minute statement limit near 350,000 heads.
SELECT to_regclass('feed.submission') IS NOT NULL AND to_regclass('feed_ops.pending') IS NOT NULL
       AND to_regclass('journal.submission_revision') IS NOT NULL AS has_feed \gset
\if :has_feed
WITH
candidate AS (
    SELECT v.uid AS version_uid, v.version_number, true AS is_field, false AS repeating, e.ord,
           CASE WHEN e.item ->> 'path' IS NULL OR e.item ->> 'path' = e.item ->> 'id'
                     OR strpos(e.item ->> 'path', '.') = 0
                THEN e.item ->> 'name'
                ELSE substring(e.item ->> 'path' FROM '^(.*)[.][^.]*$') || '.' || (e.item ->> 'name')
           END AS path
    FROM data_template t
    JOIN data_template_version v ON v.data_template_id = t.id
    CROSS JOIN LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(v.fields) = 'array' THEN v.fields END)
        WITH ORDINALITY AS e(item, ord)
    WHERE t.uid = :'tmpl'
      AND e.item ->> 'id' IS NOT NULL AND e.item ->> 'name' IS NOT NULL
    UNION ALL
    SELECT v.uid, v.version_number, false,
           CASE jsonb_typeof(e.item -> 'repeatable')
               WHEN 'boolean' THEN (e.item ->> 'repeatable')::boolean
               WHEN 'string' THEN btrim(e.item ->> 'repeatable') = 'true'
               WHEN 'number' THEN (e.item ->> 'repeatable')::numeric <> 0
               ELSE false
           END,
           e.ord, e.item ->> 'path'
    FROM data_template t
    JOIN data_template_version v ON v.data_template_id = t.id
    CROSS JOIN LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(v.sections) = 'array' THEN v.sections END)
        WITH ORDINALITY AS e(item, ord)
    WHERE t.uid = :'tmpl' AND e.item ->> 'path' IS NOT NULL
),
by_path AS MATERIALIZED (
    SELECT c.path,
           array_agg(c.version_uid ORDER BY c.version_number DESC, c.is_field DESC, c.ord) AS version_uids,
           array_agg(c.is_field ORDER BY c.version_number DESC, c.is_field DESC, c.ord) AS fields,
           array_agg(c.repeating ORDER BY c.version_number DESC, c.is_field DESC, c.ord) AS repeatings
    FROM candidate c
    GROUP BY c.path
)
SELECT '12f', count(*), 'records at their head''s version whose live rows differ in number from the head''s rows', ''
FROM feed.submission f
JOIN data_submission ds ON ds.id = f.id
JOIN journal.submission_revision r ON r.submission_id = f.id AND r.version = f.version
CROSS JOIN LATERAL (
    WITH RECURSIVE
    walk(path, value, depth, kind) AS (
        SELECT e.key, e.value, 0, k.kind
        FROM jsonb_each(CASE WHEN jsonb_typeof(r.form_data) = 'object' THEN r.form_data END) AS e(key, value)
        LEFT JOIN by_path b ON b.path = e.key
        CROSS JOIN LATERAL (
            SELECT CASE
                       WHEN s.shape = 'OBJECT' AND (b.path IS NULL OR (NOT ch.is_field AND NOT ch.repeating))
                           THEN 'container'
                       WHEN s.shape = 'ROWS' AND NOT ch.is_field AND ch.repeating THEN 'rows'
                       ELSE 'leaf'
                   END
            FROM (SELECT CASE WHEN jsonb_typeof(e.value) = 'object' THEN 'OBJECT'
                              WHEN jsonb_typeof(e.value) = 'array' AND jsonb_array_length(e.value) > 0
                                   AND NOT EXISTS (SELECT 1 FROM jsonb_array_elements(e.value) AS x(element)
                                                   WHERE jsonb_typeof(x.element) <> 'object')
                                  THEN 'ROWS'
                              ELSE 'LEAF' END AS shape) s
            LEFT JOIN LATERAL (
                SELECT c.is_field, c.repeating
                FROM unnest(b.version_uids, b.fields, b.repeatings) WITH ORDINALITY AS c(version_uid, is_field, repeating, n)
                ORDER BY (CASE s.shape WHEN 'OBJECT' THEN c.is_field OR NOT c.repeating
                                       WHEN 'ROWS' THEN c.is_field OR c.repeating
                                       ELSE c.is_field END)::int * 2
                         + (c.version_uid IS NOT DISTINCT FROM r.metadata ->> 'template_version_uid')::int DESC,
                         c.n
                LIMIT 1) ch ON true
        ) AS k(kind)
        WHERE e.key <> '_status' AND jsonb_typeof(e.value) IN ('object', 'array') AND k.kind <> 'leaf'
        UNION ALL
        SELECT m.path, m.value, w.depth + 1, k.kind
        FROM walk w
        CROSS JOIN LATERAL (
            SELECT w.path || '.' || e.key AS path, e.value
            FROM jsonb_each(CASE WHEN w.kind = 'container' THEN w.value END) AS e(key, value)
            UNION ALL
            SELECT w.path || '.' || e.key, e.value
            FROM jsonb_array_elements(CASE WHEN w.kind = 'rows' THEN w.value END) AS rw(element)
            CROSS JOIN LATERAL jsonb_each(rw.element) AS e(key, value)
            WHERE e.key NOT IN ('_id', '_index', '_parentId', '_submissionUid')
        ) m
        LEFT JOIN by_path b ON b.path = m.path
        CROSS JOIN LATERAL (
            SELECT CASE
                       WHEN w.depth + 1 >= 32 THEN 'leaf'
                       WHEN s.shape = 'OBJECT' AND (b.path IS NULL OR (NOT ch.is_field AND NOT ch.repeating))
                           THEN 'container'
                       WHEN s.shape = 'ROWS' AND NOT ch.is_field AND ch.repeating THEN 'rows'
                       ELSE 'leaf'
                   END
            FROM (SELECT CASE WHEN jsonb_typeof(m.value) = 'object' THEN 'OBJECT'
                              WHEN jsonb_typeof(m.value) = 'array' AND jsonb_array_length(m.value) > 0
                                   AND NOT EXISTS (SELECT 1 FROM jsonb_array_elements(m.value) AS x(element)
                                                   WHERE jsonb_typeof(x.element) <> 'object')
                                  THEN 'ROWS'
                              ELSE 'LEAF' END AS shape) s
            LEFT JOIN LATERAL (
                SELECT c.is_field, c.repeating
                FROM unnest(b.version_uids, b.fields, b.repeatings) WITH ORDINALITY AS c(version_uid, is_field, repeating, n)
                ORDER BY (CASE s.shape WHEN 'OBJECT' THEN c.is_field OR NOT c.repeating
                                       WHEN 'ROWS' THEN c.is_field OR c.repeating
                                       ELSE c.is_field END)::int * 2
                         + (c.version_uid IS NOT DISTINCT FROM r.metadata ->> 'template_version_uid')::int DESC,
                         c.n
                LIMIT 1) ch ON true
        ) AS k(kind)
        WHERE jsonb_typeof(m.value) IN ('object', 'array') AND k.kind <> 'leaf'
    )
    SELECT coalesce(sum(jsonb_array_length(w.value)) FILTER (WHERE w.kind = 'rows'), 0) AS n
    FROM walk w
) h
WHERE ds.template_uid = :'tmpl' AND NOT f.deleted
  AND f.version = (SELECT max(x.version) FROM journal.submission_revision x WHERE x.submission_id = f.id)
  AND h.n <> (SELECT count(*) FROM feed.submission_row x WHERE x.submission_id = f.id AND NOT x.deleted);
\endif
