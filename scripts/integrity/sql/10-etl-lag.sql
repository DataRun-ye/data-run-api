-- Check 10: how far the ETL is behind.
--
-- Outbox status: 'pending' waits for its first try; 'success' is projected.
-- The ETL fix (branch fix/etl-runtime) adds 'failed', a row that failed and
-- will be tried again after a backoff, and 'dead', a row given up on. Today's
-- ETL never writes either, so on today's schema 10c and 10d stay 0. A row not
-- projected yet is pending or failed; leased rows keep their status.
-- outbox.created_at is written in UTC by the application.
-- The filter matches the branch's partial index on unfinished rows.
WITH counts AS (
    SELECT
        count(*) FILTER (WHERE o.status IN ('pending', 'failed')) AS backlog,
        coalesce(extract(epoch FROM (now() AT TIME ZONE 'UTC')
                 - min(o.created_at) FILTER (WHERE o.status IN ('pending', 'failed')))::bigint, 0) AS oldest_waiting_s,
        count(*) FILTER (WHERE o.status = 'failed') AS retrying,
        count(*) FILTER (WHERE o.status = 'dead') AS dead
    FROM outbox o
    WHERE o.status IN ('pending', 'failed', 'dead')
)
SELECT kv.key, kv.n, kv.label
FROM counts c
CROSS JOIN LATERAL (VALUES
    ('10a', c.backlog, 'outbox rows not projected yet: waiting, or failed and due to retry (backlog)'),
    ('10b', c.oldest_waiting_s, 'age of the oldest of them, in seconds (0 when none wait)'),
    ('10c', c.retrying, 'of 10a: rows that failed and wait to be tried again'),
    ('10d', c.dead, 'dead outbox rows: given up after failing, never projected')
) AS kv(key, n, label);
