-- Check 9: form-version labels on submissions.
--
-- A submission is labelled with the form version it was filled in
-- (template_version_uid, not updatable). Before server 6.5.0, uploads stored
-- the form's current version instead (fixed in e033dcd5), so some labels are
-- newer than the version that existed when the entry started. The phone's
-- start_entry_time is compared with the version's created_date (both UTC).
WITH lab AS (
    SELECT s.template_uid, s.start_entry_time, v.created_date AS label_created,
           v.uid IS NULL AS no_version,
           EXISTS (SELECT 1 FROM data_template_version o
                   WHERE o.template_uid = s.template_uid AND o.created_date <= s.start_entry_time) AS older_existed
    FROM data_submission s
    LEFT JOIN data_template_version v ON v.uid = s.template_version_uid
),
counts AS (
    SELECT
        count(*) FILTER (WHERE label_created > start_entry_time AND older_existed) AS provably_wrong,
        count(*) FILTER (WHERE label_created > start_entry_time AND NOT older_existed) AS unverifiable,
        count(*) FILTER (WHERE no_version) AS no_version,
        count(*) FILTER (WHERE NOT no_version AND start_entry_time IS NULL) AS no_start_time
    FROM lab
)
SELECT kv.key, kv.n, kv.label
FROM counts c
CROSS JOIN LATERAL (VALUES
    ('9a', c.provably_wrong, 'labels newer than a version that existed when the entry started (provably wrong)'),
    ('9b', c.unverifiable, 'labels newer than the entry, which predates any stored version (cannot be checked)'),
    ('9c', c.no_version, 'submissions whose label matches no stored form version'),
    ('9d', c.no_start_time, 'submissions with no entry start time (label cannot be checked)')
) AS kv(key, n, label);
