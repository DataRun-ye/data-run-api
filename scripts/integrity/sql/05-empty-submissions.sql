-- Check 5 (part): active submissions with no data at all.
--
-- The payload walk (payload-by-template.sql) counts never-projected
-- submissions that hold data (5a, 5b). Submissions whose form_data is null
-- have nothing to project; they are counted here so they are not mistaken for
-- a projection gap.
SELECT '5c' AS key, count(*) AS n, 'active submissions with no data (form_data null), so nothing to project' AS label
FROM data_submission s
WHERE NOT coalesce(s.deleted, false) AND jsonb_typeof(s.form_data) IS DISTINCT FROM 'object';
