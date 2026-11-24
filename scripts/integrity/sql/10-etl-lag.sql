-- Check 10: how far the ETL is behind.
--
-- The ETL claims pending outbox rows every minute and marks them success.
-- outbox.created_at is written in UTC by the application.
WITH counts AS (
    SELECT
        count(*) FILTER (WHERE o.status = 'pending') AS backlog,
        coalesce(extract(epoch FROM (now() AT TIME ZONE 'UTC')
                 - min(o.created_at) FILTER (WHERE o.status = 'pending'))::bigint, 0) AS oldest_pending_s,
        count(*) FILTER (WHERE o.status = 'failed') AS failed
    FROM outbox o
)
SELECT kv.key, kv.n, kv.label
FROM counts c
CROSS JOIN LATERAL (VALUES
    ('10a', c.backlog, 'outbox rows waiting for the ETL (backlog)'),
    ('10b', c.oldest_pending_s, 'age of the oldest waiting outbox row, in seconds (0 when none wait)'),
    ('10c', c.failed, 'outbox rows marked failed')
) AS kv(key, n, label);
