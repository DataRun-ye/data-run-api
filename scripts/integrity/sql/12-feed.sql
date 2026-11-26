-- Check 12: the submission feed (schema feed, contract version 1) against the journal and the stored submissions.
--
-- The publisher writes each submission's current record from its head (its newest revision in the journal), with one
-- record per row, positions from one sequence under one lock. feed_ops holds its pending changes (a feed part that
-- failed, retried; dead past the attempt cap, with a reason code), its republish marks and the normalizer's flag
-- counts of published records. The publication lines must read 0 once the feed is on (contract 8, item 4):
--   12a heads not published at their version, other than those waiting in the backlog or as pending changes;
--   12b submission records without their submission; 12c row records without their submission's record;
--   12d live row records of a deleted submission's record; 12e positions used twice, across both streams;
--   12f records at their head's version whose live row records differ in number from the head's rows, run one form
--       at a time from feed-rows-by-form.sql; this file adds the records whose submission has no form, whose heads
--       have no rows, so any live row record of theirs differs;
--   12g pending changes older than the freshness target (two minutes);
--   12h the feed's dead changes (pending changes past the cap), by reason code, as freshness.dead counts them.
-- For reference only: 12i the backlog (open outbox changes whose version the feed lacks, as freshness.backlog counts
-- them), 12j the normalizer's flags in published records, by flag, and 12k republish marks waiting. Freshness, and so
-- these lines, count the feed's own state only: what tall and events did with a change does not count.
--
-- On a database without the feed's tables every line reads -1.
--
-- Cost: one pass over data_submission with probes into the journal, the feed and the outbox's unfinished rows; one
-- aggregate over both streams' positions. The walk of the heads (12f) runs per form, from feed-rows-by-form.sql.
SELECT to_regclass('feed.submission') IS NOT NULL AND to_regclass('feed_ops.pending') IS NOT NULL
       AND to_regclass('journal.submission_revision') IS NOT NULL AS has_feed \gset
\if :has_feed
WITH
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
formless AS (
    SELECT count(*) AS n
    FROM feed.submission f
    JOIN data_submission s ON s.id = f.id
    WHERE s.template_uid IS NULL AND NOT f.deleted
      AND f.version = (SELECT max(x.version) FROM journal.submission_revision x WHERE x.submission_id = f.id)
      AND EXISTS (SELECT 1 FROM feed.submission_row r WHERE r.submission_id = f.id AND NOT r.deleted)
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
FROM unpublished u CROSS JOIN orphans o CROSS JOIN positions p CROSS JOIN formless r CROSS JOIN pending q
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
