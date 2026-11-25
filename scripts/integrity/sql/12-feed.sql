-- Check 12: the submission feed (schema feed, contract version 1) against the journal and the stored submissions.
--
-- The publisher writes each submission's current record from its head (its newest revision in the journal), with one
-- record per row, positions from one sequence under one lock. feed_ops holds its pending changes (a feed part that
-- failed, retried; dead past the attempt cap, with a reason code), its republish marks and the normalizer's flag
-- counts of published records. The publication lines must read 0 once the feed is on (contract 8, item 4):
--   12a heads not published at their version, other than those waiting in the backlog or as pending changes;
--   12b submission records without their submission; 12c row records without their submission's record;
--   12d live row records of a deleted submission's record; 12e positions used twice, across both streams;
--   12f records at their head's version whose live row records differ in number from the head's rows;
--   12g pending changes older than the freshness target (two minutes);
--   12h the feed's dead changes (pending changes past the cap), by reason code, as freshness.dead counts them.
-- For reference only: 12i the backlog (open outbox changes whose version the feed lacks, as freshness.backlog counts
-- them), 12j the normalizer's flags in published records, by flag, and 12k republish marks waiting. Freshness, and so
-- these lines, count the feed's own state only: what tall and events did with a change does not count.
--
-- 12f finds a head's rows the way the submission normalizer does, from the stored form definitions, apart from its
-- code: walking the document from its root, an object is walked when no version defines its path or when its path
-- resolves to a plain section; a non-empty array of objects is rows when its path resolves to a repeating section, and
-- each row's keys are walked, its _id, _index, _parentId and _submissionUid aside; the root's _status is skipped; a JSON
-- null is nothing; nothing deeper than 32 objects and rows is walked. A path resolves, for the value's shape, to the
-- first of its definitions across the form's versions that can hold the shape (a field holds anything, a plain section
-- an object, a repeating section rows), the pinned version's before another's, then the newest version first, a field
-- before a section, then the version's stored order. A definition's path: a field's is its stored path without the
-- last key, then its name (its name alone outside every section); a section's is its stored path.
--
-- On a database without the feed's tables every line reads -1.
--
-- Cost: one pass over data_submission with probes into the journal, the feed and the outbox's unfinished rows; one
-- walk of the head of every published submission, joined to its form's definitions by data path; one aggregate over
-- both streams' positions.
SELECT to_regclass('feed.submission') IS NOT NULL AND to_regclass('feed_ops.pending') IS NOT NULL
       AND to_regclass('journal.submission_revision') IS NOT NULL AS has_feed \gset
\if :has_feed
WITH RECURSIVE
backlog AS (
    SELECT DISTINCT o.submission_id FROM outbox o WHERE o.status IN ('pending', 'failed')
),
unpublished AS (
    SELECT count(*) AS n
    FROM data_submission s
    CROSS JOIN LATERAL (SELECT max(x.version) AS version FROM journal.submission_revision x
                        WHERE x.submission_id = s.id) h
    LEFT JOIN feed.submission f ON f.id = s.id
    WHERE h.version IS NOT NULL AND f.version IS DISTINCT FROM h.version
      AND NOT EXISTS (SELECT 1 FROM backlog b WHERE b.submission_id = s.id)
      AND NOT EXISTS (SELECT 1 FROM feed_ops.pending p WHERE p.submission_id = s.id)
),
orphans AS (
    SELECT (SELECT count(*) FROM feed.submission f
            WHERE NOT EXISTS (SELECT 1 FROM data_submission s WHERE s.id = f.id)) AS records,
           (SELECT count(*) FROM feed.submission_row r
            WHERE NOT EXISTS (SELECT 1 FROM feed.submission f WHERE f.id = r.submission_id)) AS rows,
           (SELECT count(*) FROM feed.submission_row r JOIN feed.submission f ON f.id = r.submission_id
            WHERE f.deleted AND NOT r.deleted) AS live_rows_of_deleted
),
positions AS (
    SELECT count(*) AS twice
    FROM (SELECT x.position
          FROM (SELECT position FROM feed.submission UNION ALL SELECT position FROM feed.submission_row) x
          GROUP BY x.position
          HAVING count(*) > 1) d
),
published AS (
    SELECT f.id, s.template_uid AS form_uid, r.metadata ->> 'template_version_uid' AS pin, r.form_data
    FROM feed.submission f
    JOIN data_submission s ON s.id = f.id
    JOIN journal.submission_revision r ON r.submission_id = f.id AND r.version = f.version
    WHERE NOT f.deleted
      AND f.version = (SELECT max(x.version) FROM journal.submission_revision x WHERE x.submission_id = f.id)
),
candidate AS (
    SELECT t.uid AS form_uid, v.uid AS version_uid, v.version_number, true AS is_field, false AS repeating, e.ord,
           CASE WHEN e.item ->> 'path' IS NULL OR e.item ->> 'path' = e.item ->> 'id'
                     OR strpos(e.item ->> 'path', '.') = 0
                THEN e.item ->> 'name'
                ELSE substring(e.item ->> 'path' FROM '^(.*)[.][^.]*$') || '.' || (e.item ->> 'name')
           END AS path
    FROM data_template t
    JOIN data_template_version v ON v.data_template_id = t.id
    CROSS JOIN LATERAL jsonb_array_elements(CASE WHEN jsonb_typeof(v.fields) = 'array' THEN v.fields END)
        WITH ORDINALITY AS e(item, ord)
    WHERE t.uid IN (SELECT DISTINCT p.form_uid FROM published p)
      AND e.item ->> 'id' IS NOT NULL AND e.item ->> 'name' IS NOT NULL
    UNION ALL
    SELECT t.uid, v.uid, v.version_number, false,
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
    WHERE t.uid IN (SELECT DISTINCT p.form_uid FROM published p) AND e.item ->> 'path' IS NOT NULL
),
by_path AS (
    SELECT c.form_uid, c.path,
           array_agg(c.version_uid ORDER BY c.version_number DESC, c.is_field DESC, c.ord) AS version_uids,
           array_agg(c.is_field ORDER BY c.version_number DESC, c.is_field DESC, c.ord) AS fields,
           array_agg(c.repeating ORDER BY c.version_number DESC, c.is_field DESC, c.ord) AS repeatings
    FROM candidate c
    GROUP BY c.form_uid, c.path
),
walk(id, form_uid, pin, path, value, depth, kind) AS (
    SELECT p.id, p.form_uid, p.pin, e.key, e.value, 0, k.kind
    FROM published p
    CROSS JOIN LATERAL jsonb_each(CASE WHEN jsonb_typeof(p.form_data) = 'object' THEN p.form_data END) AS e(key, value)
    LEFT JOIN by_path b ON b.form_uid = p.form_uid AND b.path = e.key
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
                     + (c.version_uid IS NOT DISTINCT FROM p.pin)::int DESC,
                     c.n
            LIMIT 1) ch ON true
    ) AS k(kind)
    WHERE e.key <> '_status' AND jsonb_typeof(e.value) <> 'null'
    UNION ALL
    SELECT w.id, w.form_uid, w.pin, m.path, m.value, w.depth + 1, k.kind
    FROM walk w
    CROSS JOIN LATERAL (
        SELECT w.path || '.' || e.key AS path, e.value
        FROM jsonb_each(CASE WHEN w.kind = 'container' THEN w.value END) AS e(key, value)
        UNION ALL
        SELECT w.path || '.' || e.key, e.value
        FROM jsonb_array_elements(CASE WHEN w.kind = 'rows' THEN w.value END) AS r(element)
        CROSS JOIN LATERAL jsonb_each(r.element) AS e(key, value)
        WHERE e.key NOT IN ('_id', '_index', '_parentId', '_submissionUid')
    ) m
    LEFT JOIN by_path b ON b.form_uid = w.form_uid AND b.path = m.path
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
                     + (c.version_uid IS NOT DISTINCT FROM w.pin)::int DESC,
                     c.n
            LIMIT 1) ch ON true
    ) AS k(kind)
    WHERE jsonb_typeof(m.value) <> 'null'
),
head_rows AS (
    SELECT p.id, coalesce(sum(jsonb_array_length(w.value)) FILTER (WHERE w.kind = 'rows'), 0) AS n
    FROM published p
    LEFT JOIN walk w ON w.id = p.id
    GROUP BY p.id
),
row_records AS (
    SELECT r.submission_id AS id, count(*) FILTER (WHERE NOT r.deleted) AS live
    FROM feed.submission_row r
    WHERE r.submission_id IN (SELECT p.id FROM published p)
    GROUP BY r.submission_id
),
rows_differ AS (
    SELECT count(*) AS n
    FROM head_rows h
    LEFT JOIN row_records r ON r.id = h.id
    WHERE h.n <> coalesce(r.live, 0)
),
pending AS (
    SELECT count(*) FILTER (WHERE p.dead_at IS NULL AND p.first_failed_at < now() - interval '2 minutes') AS old
    FROM feed_ops.pending p
),
totals AS (
    SELECT (SELECT count(*) FROM outbox o
            LEFT JOIN feed.submission f ON f.id = o.submission_id
            WHERE o.status IN ('pending', 'failed', 'dead') AND o.submission_version IS NOT NULL
              AND (f.version IS NULL OR f.version < o.submission_version)) AS backlog,
           (SELECT count(*) FROM feed_ops.republish) AS marks
),
dead AS (
    SELECT p.reason AS scope, count(*) AS n FROM feed_ops.pending p WHERE p.dead_at IS NOT NULL GROUP BY p.reason
),
flags AS (
    SELECT f.key AS scope, sum(f.value::bigint) AS n
    FROM feed_ops.normalization_flags x
    CROSS JOIN LATERAL jsonb_each_text(x.flags) AS f(key, value)
    GROUP BY f.key
)
SELECT kv.key, kv.n, kv.label, ''
FROM unpublished u CROSS JOIN orphans o CROSS JOIN positions p CROSS JOIN rows_differ r CROSS JOIN pending q
CROSS JOIN totals t
CROSS JOIN LATERAL (VALUES
    ('12a', u.n, 'heads not published at their version, other than those in the backlog or pending'),
    ('12b', o.records, 'submission records without their submission'),
    ('12c', o.rows, 'row records without their submission''s record'),
    ('12d', o.live_rows_of_deleted, 'live row records of a deleted submission''s record'),
    ('12e', p.twice, 'positions used twice, across both streams'),
    ('12f', r.n, 'records at their head''s version whose live rows differ in number from the head''s rows'),
    ('12g', q.old, 'pending changes older than the freshness target (2 minutes)'),
    ('12i', t.backlog, 'backlog: open changes whose version the feed lacks (reference)'),
    ('12k', t.marks, 'republish marks waiting (reference)')
) AS kv(key, n, label)
UNION ALL
SELECT '12h', d.n, 'the feed''s dead changes: pending changes past the attempt cap, by reason code', d.scope
FROM dead d
UNION ALL
SELECT '12h', 0, 'the feed''s dead changes: pending changes past the attempt cap, by reason code', ''
WHERE NOT EXISTS (SELECT 1 FROM dead)
UNION ALL
SELECT '12j', f.n, 'normalization flags in published records, by flag (reference)', f.scope
FROM flags f
UNION ALL
SELECT '12j', 0, 'normalization flags in published records, by flag (reference)', ''
WHERE NOT EXISTS (SELECT 1 FROM flags);
\else
SELECT kv.key, kv.n, kv.label
FROM (VALUES
    ('12a', -1, 'heads not published at their version, other than those in the backlog or pending'),
    ('12b', -1, 'submission records without their submission'),
    ('12c', -1, 'row records without their submission''s record'),
    ('12d', -1, 'live row records of a deleted submission''s record'),
    ('12e', -1, 'positions used twice, across both streams'),
    ('12f', -1, 'records at their head''s version whose live rows differ in number from the head''s rows'),
    ('12g', -1, 'pending changes older than the freshness target (2 minutes)'),
    ('12h', -1, 'the feed''s dead changes: pending changes past the attempt cap, by reason code'),
    ('12i', -1, 'backlog: open changes whose version the feed lacks (reference)'),
    ('12j', -1, 'normalization flags in published records, by flag (reference)'),
    ('12k', -1, 'republish marks waiting (reference)')
) AS kv(key, n, label);
\endif
