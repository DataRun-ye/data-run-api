-- Check 1: the outbox against the stored submissions.
--
-- The outbox is the only way a change reaches tall and events. Every write to a
-- submission should leave an event, and the latest event should match the
-- record: its type (DELETE for a deleted record) and its payload (the stored
-- form_data). "Latest" means the highest outbox_id, the order rows were written.
WITH latest AS MATERIALIZED (
    SELECT DISTINCT ON (o.submission_id) o.submission_id, o.outbox_id, o.event_type, o.created_at
    FROM outbox o
    ORDER BY o.submission_id, o.outbox_id DESC
),
per_submission AS (
    SELECT s.deleted, l.event_type,
           l.outbox_id IS NULL AS no_event,
           l.outbox_id IS NOT NULL
             AND (coalesce(s.form_data, 'null'::jsonb) IS DISTINCT FROM coalesce(o.payload, 'null'::jsonb)
                  OR s.last_modified_date > l.created_at) AS changed_after_event
    FROM data_submission s
    LEFT JOIN latest l ON l.submission_id = s.id
    LEFT JOIN outbox o ON o.outbox_id = l.outbox_id
),
empty_updates AS (
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
           count(*) FILTER (WHERE coalesce(deleted, false) AND coalesce(event_type, '') <> 'DELETE') AS deleted_not_delete,
           count(*) FILTER (WHERE NOT coalesce(deleted, false) AND event_type = 'DELETE') AS active_delete,
           count(*) FILTER (WHERE changed_after_event) AS changed_after_event
    FROM per_submission
)
SELECT kv.key, kv.n, kv.label
FROM counts c CROSS JOIN empty_updates u
CROSS JOIN LATERAL (VALUES
    ('1a', c.no_event, 'submissions with no outbox event at all'),
    ('1b', c.deleted_not_delete, 'deleted submissions whose latest outbox event is not DELETE'),
    ('1c', c.active_delete, 'active submissions whose latest outbox event is DELETE'),
    ('1d', c.changed_after_event, 'submissions changed after their latest outbox event (payload or time)'),
    ('1e', u.events, 'UPDATE events that changed nothing (same payload as the event before)'),
    ('1f', u.submissions, 'submissions with such empty UPDATE events')
) AS kv(key, n, label);
