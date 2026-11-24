-- Check 4: deleted (voided) submissions downstream.
--
-- A deleted submission stays in data_submission with deleted = true. Its events
-- should carry deleted_at, and its tall rows a delete marker. Tall has no
-- marker today (is_deleted was dropped on 2026-02-11), so every tall row of a
-- deleted submission counts as unmarked. When a marker is added (plan step D1),
-- change 4a to count only the rows it does not mark.
-- No application path removes a submission row; 4e and 4f catch rows removed
-- some other way, which would leave tall rows and events behind.
WITH del AS MATERIALIZED (
    SELECT s.uid FROM data_submission s WHERE coalesce(s.deleted, false)
),
counts AS (
    SELECT
        (SELECT count(*) FROM analytics.tall_canonical t WHERE t.submission_uid IN (SELECT uid FROM del)) AS tall_rows,
        (SELECT count(DISTINCT t.submission_uid) FROM analytics.tall_canonical t
          WHERE t.submission_uid IN (SELECT uid FROM del)) AS tall_subs,
        (SELECT count(*) FROM analytics.events e
          WHERE e.submission_uid IN (SELECT uid FROM del) AND e.deleted_at IS NULL) AS events_unmarked,
        (SELECT count(*) FROM analytics.events e
          JOIN data_submission s ON s.uid = e.submission_uid AND NOT coalesce(s.deleted, false)
          WHERE e.deleted_at IS NOT NULL) AS events_marked_on_active,
        (SELECT count(*) FROM analytics.tall_canonical t
          WHERE NOT EXISTS (SELECT 1 FROM data_submission s WHERE s.uid = t.submission_uid)) AS tall_orphans,
        (SELECT count(*) FROM analytics.events e
          WHERE NOT EXISTS (SELECT 1 FROM data_submission s WHERE s.uid = e.submission_uid)) AS event_orphans
)
SELECT kv.key, kv.n, kv.label
FROM counts c
CROSS JOIN LATERAL (VALUES
    ('4a', c.tall_rows, 'tall rows of deleted submissions, not marked (tall has no delete marker)'),
    ('4b', c.tall_subs, 'deleted submissions that still have tall rows'),
    ('4c', c.events_unmarked, 'events of deleted submissions not marked deleted'),
    ('4d', c.events_marked_on_active, 'events marked deleted although their submission is active'),
    ('4e', c.tall_orphans, 'tall rows whose submission no longer exists (removed outside the application)'),
    ('4f', c.event_orphans, 'events whose submission no longer exists (removed outside the application)')
) AS kv(key, n, label);
