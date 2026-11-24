-- Check 5 (part): active submissions the payload walk cannot read.
--
-- payload-by-template.sql counts never-projected submissions that hold values
-- (5a, 5b). It reads each template's submissions, so it never sees a
-- submission without a template_uid, and a form_data that is not a JSON object
-- yields no values. Both are counted here, so they are not mistaken for a
-- projection gap and do not drop out of the payload checks unseen.
WITH counts AS (
    SELECT
        count(*) FILTER (WHERE jsonb_typeof(s.form_data) IS DISTINCT FROM 'object') AS not_object,
        count(*) FILTER (WHERE s.template_uid IS NULL) AS no_template
    FROM data_submission s
    WHERE NOT coalesce(s.deleted, false)
)
SELECT kv.key, kv.n, kv.label
FROM counts c
CROSS JOIN LATERAL (VALUES
    ('5c', c.not_object, 'active submissions whose form data is not a JSON object (nothing to project)'),
    ('5d', c.no_template, 'active submissions with no template_uid (the payload checks cannot read them)')
) AS kv(key, n, label);
