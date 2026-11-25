-- Check 1: the outbox against the stored submissions.
--
-- The outbox is the only way a change reaches tall and events. Every write to a
-- submission should leave an event, and the latest event should match the
-- record: its type (DELETE for a deleted record) and its payload (the stored
-- form_data). "Latest" means the highest outbox_id, the order rows were written.
--
-- 1d also flags a record changed after its latest event. Since step C an event
-- names the version it announces (outbox.submission_version), and the record
-- must still be at that version. Events written before step C, or by a
-- rolled-back 6.5.5, carry no version and are compared by time, as before: the
-- upload flushed the record before it stamped the event, so its event was never
-- older than the record, and the admin delete stamped its DELETE event first and
-- saved the record afterwards, so a deleted record up to a minute newer than its
-- DELETE event is taken as that same transaction. (Since step C the event's time
-- comes from the database and the record's from the application, so times are
-- not compared for versioned events.) A changed payload is flagged in both cases.
--
-- On a database before step C the outbox has no submission_version: the
-- version is read as NULL there, and every event is compared by time.
SELECT CASE WHEN EXISTS (SELECT 1 FROM information_schema.columns
                         WHERE table_schema = 'public' AND table_name = 'outbox'
                           AND column_name = 'submission_version')
            THEN 'o.submission_version' ELSE 'NULL::bigint' END AS outbox_version \gset
WITH latest AS MATERIALIZED (
    SELECT DISTINCT ON (o.submission_id) o.submission_id, o.outbox_id, o.event_type, o.created_at,
           :outbox_version AS submission_version
    FROM outbox o
    ORDER BY o.submission_id, o.outbox_id DESC
),
per_submission AS (
    SELECT coalesce(s.deleted, false) AS deleted, l.event_type,
           l.outbox_id IS NULL AS no_event,
           l.outbox_id IS NOT NULL
             AND (coalesce(s.form_data, 'null'::jsonb) IS DISTINCT FROM coalesce(o.payload, 'null'::jsonb)
                  OR CASE WHEN l.submission_version IS NOT NULL THEN l.submission_version <> s.lock_version
                          ELSE s.last_modified_date > l.created_at
                               AND NOT (l.event_type = 'DELETE' AND coalesce(s.deleted, false)
                                        AND s.last_modified_date <= l.created_at + interval '1 minute')
                     END) AS changed_after_event
    FROM data_submission s
    LEFT JOIN latest l ON l.submission_id = s.id
    LEFT JOIN outbox o ON o.outbox_id = l.outbox_id
),
same_payload_updates AS (
    SELECT count(*) AS events, count(DISTINCT submission_id) AS submissions
    FROM (
        SELECT o.submission_id, o.event_type,
               o.payload = lag(o.payload) OVER (PARTITION BY o.submission_id ORDER BY o.outbox_id) AS same_as_previous
        FROM outbox o
    ) x
    WHERE x.event_type = 'UPDATE' AND x.same_as_previous
),
counts AS (
    SELECT count(*) FILTER (WHERE no_event) AS no_event,
           count(*) FILTER (WHERE deleted AND NOT no_event AND event_type IS DISTINCT FROM 'DELETE') AS deleted_not_delete,
           count(*) FILTER (WHERE NOT deleted AND event_type = 'DELETE') AS active_delete,
           count(*) FILTER (WHERE changed_after_event) AS changed_after_event
    FROM per_submission
)
SELECT kv.key, kv.n, kv.label
FROM counts c CROSS JOIN same_payload_updates u
CROSS JOIN LATERAL (VALUES
    ('1a', c.no_event, 'submissions with no outbox event at all'),
    ('1b', c.deleted_not_delete, 'deleted submissions with events, whose latest event is not DELETE'),
    ('1c', c.active_delete, 'active submissions whose latest outbox event is DELETE'),
    ('1d', c.changed_after_event, 'submissions changed after their latest outbox event (payload or time)'),
    ('1e', u.events, 'UPDATE events whose form data equals the previous event''s (includes metadata-only changes, so not a fault count by itself)'),
    ('1f', u.submissions, 'submissions with such UPDATE events')
) AS kv(key, n, label);
