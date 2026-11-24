-- Check 11: the change record (journal.submission_revision) against the stored submissions.
--
-- Every submission has a head revision (its highest version) at its lock_version whose state is the row's:
-- form_data, and the other columns as journal.submission_revision_metadata() renders them. Every revision the application
-- wrote (CREATE, UPDATE, DELETE) has exactly one outbox row naming it, of the type its state calls for (DELETE when
-- deleted). BASELINE revisions come from Liquibase: MIGRATION when the journal started, GAP_REPAIR for changes a
-- rolled-back release left unrecorded.
--
-- On a database without the journal (before step C) every line reads -1.
--
-- Cost: one pass over data_submission with two index probes per submission (no sort, no hash of form data), one
-- pass over the journal for the application's revisions, and probes for the outbox rows that carry a version.
SELECT to_regclass('journal.submission_revision') IS NOT NULL AS has_journal \gset
\if :has_journal
WITH heads AS (
    SELECT count(*) FILTER (WHERE h.version IS NULL) AS no_revision,
           count(*) FILTER (WHERE h.version < s.lock_version) AS behind,
           count(*) FILTER (WHERE h.version > s.lock_version) AS ahead,
           count(*) FILTER (WHERE h.version = s.lock_version
                              AND (r.form_data IS DISTINCT FROM s.form_data
                                   OR r.metadata IS DISTINCT FROM journal.submission_revision_metadata(s))) AS differs
    FROM data_submission s
    LEFT JOIN LATERAL (SELECT max(x.version) AS version FROM journal.submission_revision x
                       WHERE x.submission_id = s.id) h ON true
    LEFT JOIN journal.submission_revision r ON r.submission_id = s.id AND r.version = h.version
),
announced AS (
    SELECT count(*) FILTER (WHERE coalesce(o.n, 0) <> 1) AS not_once
    FROM journal.submission_revision r
    LEFT JOIN (SELECT submission_id, submission_version, count(*) AS n
               FROM outbox WHERE submission_version IS NOT NULL
               GROUP BY submission_id, submission_version) o
           ON o.submission_id = r.submission_id AND o.submission_version = r.version
    WHERE r.kind <> 'BASELINE'
),
typed AS (
    SELECT count(*) AS mistyped
    FROM outbox o
    JOIN journal.submission_revision r ON r.submission_id = o.submission_id AND r.version = o.submission_version
    WHERE (o.event_type = 'DELETE') IS DISTINCT FROM (r.metadata ->> 'deleted')::boolean
),
totals AS (
    SELECT count(*) FILTER (WHERE command = 'GAP_REPAIR') AS gaps, count(*) AS revisions
    FROM journal.submission_revision
)
SELECT kv.key, kv.n, kv.label
FROM heads h CROSS JOIN announced a CROSS JOIN typed t CROSS JOIN totals x
CROSS JOIN LATERAL (VALUES
    ('11a', h.no_revision, 'submissions with no revision'),
    ('11b', h.behind, 'submissions whose head revision is behind their version (a write without a revision)'),
    ('11c', h.ahead, 'submissions whose head revision is ahead of their version'),
    ('11d', h.differs, 'submissions whose head revision, at their version, is not their stored state'),
    ('11e', a.not_once, 'revisions written by the application without exactly one outbox row'),
    ('11f', t.mistyped, 'outbox rows whose event type does not follow their revision''s state'),
    ('11g', x.gaps, 'gap revisions: changes a rolled-back release left unrecorded (reference)'),
    ('11h', x.revisions, 'revisions in all (reference)')
) AS kv(key, n, label);
\else
SELECT kv.key, kv.n, kv.label
FROM (VALUES
    ('11a', -1, 'submissions with no revision'),
    ('11b', -1, 'submissions whose head revision is behind their version (a write without a revision)'),
    ('11c', -1, 'submissions whose head revision is ahead of their version'),
    ('11d', -1, 'submissions whose head revision, at their version, is not their stored state'),
    ('11e', -1, 'revisions written by the application without exactly one outbox row'),
    ('11f', -1, 'outbox rows whose event type does not follow their revision''s state'),
    ('11g', -1, 'gap revisions: changes a rolled-back release left unrecorded (reference)'),
    ('11h', -1, 'revisions in all (reference)')
) AS kv(key, n, label);
\endif
