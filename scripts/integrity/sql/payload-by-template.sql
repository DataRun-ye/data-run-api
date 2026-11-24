-- Checks 2, 3, 5, 6 and 7: one template's stored payloads against tall and events.
--
-- The runner sets :tmpl and runs this once per template, so each payload walk
-- stays small. Active submissions only (deleted ones are check 4).
--
-- The walk mirrors the ETL's path resolution (TransformServiceRobust):
--   * a path is section and field names joined by dots; repeat rows add no segment;
--   * an array is a repeat when one of its first 10 items is an object, and each
--     item is then a row; any other array (a multi-select, or an empty array) is
--     one node;
--   * JSON null and "" are skipped; any other node at a field definition's path
--     is projected, even an empty array or a plain section object;
--   * the projection key is the nearest repeat row's _id, or the submission uid
--     outside rows or when the nearest row has no _id;
--   * when several nodes share a key and a field, the ETL keeps the last one.
-- A "value" is a non-empty string, a number, a boolean or a non-empty
-- multi-select. Keys starting with "_" (_id, _index, _parentId, _submissionUid,
-- _status) are system keys, not values.
--
-- Row estimates for these CTEs are poor, so the "projected" flag is carried
-- along instead of joining back to the submissions.
WITH RECURSIVE
sub AS MATERIALIZED (
    SELECT s.uid, s.form_data,
           EXISTS (SELECT 1 FROM analytics.events e WHERE e.event_id = s.uid) AS projected
    FROM data_submission s
    WHERE s.template_uid = :'tmpl' AND NOT coalesce(s.deleted, false)
      AND jsonb_typeof(s.form_data) = 'object'
),
ce AS MATERIALIZED (
    SELECT c.id AS ce_id, p.path
    FROM canonical_element c
    CROSS JOIN LATERAL jsonb_array_elements_text(c.json_data_paths) AS p(path)
    WHERE c.template_uid = :'tmpl'
),
walk(sub_uid, projected, path, node, row_id, in_row, is_row, has_rows, ord) AS (
    SELECT s.uid, s.projected, m.key, m.value, NULL::text, false, false,
           CASE WHEN jsonb_typeof(m.value) = 'array' THEN
               (SELECT coalesce(bool_or(jsonb_typeof(x.v) = 'object'), false)
                FROM jsonb_array_elements(m.value) WITH ORDINALITY x(v, i) WHERE x.i <= 10) END,
           '{}'::int[]
    FROM sub s CROSS JOIN LATERAL jsonb_each(s.form_data) m
  UNION ALL
    SELECT w.sub_uid, w.projected, c.path, c.node, c.row_id, c.in_row, c.is_row,
           CASE WHEN jsonb_typeof(c.node) = 'array' THEN
               (SELECT coalesce(bool_or(jsonb_typeof(x.v) = 'object'), false)
                FROM jsonb_array_elements(c.node) WITH ORDINALITY x(v, i) WHERE x.i <= 10) END,
           c.ord
    FROM walk w
    CROSS JOIN LATERAL (
        -- the members of a section or of a repeat row
        SELECT w.path || '.' || m.key AS path, m.value AS node, w.row_id, w.in_row, false AS is_row, w.ord
        FROM jsonb_each(CASE WHEN jsonb_typeof(w.node) = 'object' THEN w.node END) m
      UNION ALL
        -- the rows of a repeat
        SELECT w.path, a.value, a.value ->> '_id', true, true, w.ord || a.n::int
        FROM jsonb_array_elements(CASE WHEN w.has_rows THEN w.node END) WITH ORDINALITY a(value, n)
    ) c
),
node AS MATERIALIZED (
    SELECT w.sub_uid, w.projected, w.path, coalesce(w.row_id, w.sub_uid) AS instance_key, w.node,
           w.is_row, w.ord,
           coalesce(w.has_rows, false) AS is_repeat,
           w.in_row AND w.row_id IS NULL AS in_row_without_id,
           w.node IN ('null'::jsonb, '""'::jsonb) AS is_empty,
           regexp_replace(w.path, '^.*\.', '') LIKE '\_%' AS is_system,
           NOT w.is_row AND (jsonb_typeof(w.node) IN ('string', 'number', 'boolean')
               OR (jsonb_typeof(w.node) = 'array' AND NOT w.has_rows AND w.node <> '[]'::jsonb)) AS is_leaf
    FROM walk w
),
val AS MATERIALIZED (
    -- values, and whether a field definition covers them
    SELECT n.sub_uid, n.projected, n.in_row_without_id, d.path IS NOT NULL AS has_def
    FROM node n
    LEFT JOIN (SELECT DISTINCT path FROM ce) d ON d.path = n.path
    WHERE n.is_leaf AND NOT n.is_empty AND NOT n.is_system
),
expected AS MATERIALIZED (
    -- what the ETL would write for the current payload: one row per key and field
    SELECT DISTINCT ON (n.instance_key, ce.ce_id)
           n.sub_uid, n.projected, n.instance_key, ce.ce_id, n.node, n.is_leaf
    FROM node n JOIN ce ON ce.path = n.path
    WHERE NOT n.is_empty AND NOT n.is_repeat
    ORDER BY n.instance_key, ce.ce_id, n.ord DESC
),
tall AS MATERIALIZED (
    SELECT t.submission_uid, t.instance_key, t.canonical_element_id AS ce_id,
           t.value_text, t.value_number, t.value_bool, t.value_json,
           (t.org_unit_uid, t.assignment_uid, t.team_uid, t.activity_uid)
               IS DISTINCT FROM (s.org_unit_uid, s.assignment_uid, s.team_uid, s.activity_uid) AS context_differs
    FROM analytics.tall_canonical t
    JOIN data_submission s ON s.uid = t.submission_uid
    WHERE t.template_uid = :'tmpl' AND s.template_uid = :'tmpl'
      AND NOT coalesce(s.deleted, false) AND jsonb_typeof(s.form_data) = 'object'
),
cur_rows AS MATERIALIZED (
    SELECT DISTINCT n.sub_uid, n.projected, n.instance_key AS row_id
    FROM node n WHERE n.is_row AND NOT n.in_row_without_id
),
ev AS MATERIALIZED (
    SELECT e.event_id, e.submission_uid, e.event_type, e.deleted_at,
           (e.org_unit_uid, e.assignment_uid, e.team_uid, e.activity_uid)
               IS DISTINCT FROM (s.org_unit_uid, s.assignment_uid, s.team_uid, s.activity_uid) AS context_differs,
           e.last_modified_by IS DISTINCT FROM s.last_modified_by AS modifier_differs
    FROM analytics.events e
    JOIN data_submission s ON s.uid = e.submission_uid
    WHERE e.template_uid = :'tmpl' AND s.template_uid = :'tmpl'
      AND NOT coalesce(s.deleted, false) AND jsonb_typeof(s.form_data) = 'object'
),
cmp AS MATERIALIZED (
    SELECT e.sub_uid, e.projected, e.instance_key AS e_key, e.node, e.is_leaf,
           t.submission_uid AS t_sub, t.instance_key AS t_key, t.ce_id AS t_ce,
           t.value_text, t.value_number, t.value_bool, t.value_json
    FROM expected e
    FULL JOIN tall t ON t.instance_key = e.instance_key AND t.ce_id = e.ce_id
),
leftover AS MATERIALIZED (
    -- tall rows the current payload no longer carries
    SELECT c.t_sub, c.t_key, c.t_ce,
           CASE WHEN c.t_key = c.t_sub OR r.row_id IS NOT NULL THEN 'stale' ELSE 'removed_row' END AS kind
    FROM cmp c
    LEFT JOIN cur_rows r ON r.sub_uid = c.t_sub AND r.row_id = c.t_key
    WHERE c.e_key IS NULL
),
stale_cleared AS (
    SELECT DISTINCT l.t_key, l.t_ce
    FROM leftover l
    JOIN ce ON ce.ce_id = l.t_ce
    JOIN node n ON n.sub_uid = l.t_sub AND n.instance_key = l.t_key AND n.path = ce.path AND n.is_empty
    WHERE l.kind = 'stale'
),
removed_events AS MATERIALIZED (
    SELECT ev.submission_uid
    FROM ev LEFT JOIN cur_rows r ON r.sub_uid = ev.submission_uid AND r.row_id = ev.event_id
    WHERE ev.event_type = 'repeat' AND ev.deleted_at IS NULL AND r.row_id IS NULL
),
counts AS (
    SELECT
        (SELECT count(*) FROM removed_events) AS removed_row_events,
        (SELECT count(*) FROM leftover WHERE kind = 'removed_row') AS removed_row_tall,
        (SELECT count(*) FROM (SELECT submission_uid FROM removed_events
                               UNION SELECT t_sub FROM leftover WHERE kind = 'removed_row') x) AS removed_row_subs,
        (SELECT count(*) FROM leftover WHERE kind = 'stale') AS stale,
        (SELECT count(*) FROM stale_cleared) AS stale_cleared,
        (SELECT count(*) FROM tall WHERE context_differs) AS tall_context_differs,
        (SELECT count(*) FROM ev WHERE context_differs) AS events_context_differs,
        (SELECT count(*) FROM ev WHERE modifier_differs) AS events_modifier_differs,
        (SELECT count(*) FROM (SELECT sub_uid FROM val GROUP BY sub_uid HAVING NOT bool_or(has_def)) x)
            AS subs_without_any_def,
        (SELECT count(*) FROM sub WHERE NOT projected) AS unprojected_subs,
        (SELECT count(*) FROM val WHERE NOT projected AND has_def) AS unprojected_values,
        (SELECT count(*) FROM val WHERE NOT has_def) AS values_no_def,
        (SELECT count(*) FROM cur_rows r LEFT JOIN ev ON ev.event_id = r.row_id
            WHERE r.projected AND ev.event_id IS NULL) AS rows_without_event,
        (SELECT count(*) FROM cmp WHERE cmp.projected AND cmp.t_key IS NULL) AS missing,
        (SELECT count(*) FROM val WHERE has_def AND in_row_without_id) AS collapsed_values,
        (SELECT count(DISTINCT sub_uid) FROM val WHERE has_def AND in_row_without_id) AS collapsed_subs,
        (SELECT count(*) FROM cmp WHERE cmp.e_key IS NOT NULL AND cmp.t_key IS NOT NULL AND cmp.is_leaf AND
            CASE jsonb_typeof(cmp.node)
              WHEN 'string' THEN cmp.value_text IS DISTINCT FROM
                   CASE WHEN length(cmp.node #>> '{}') > 4000 THEN left(cmp.node #>> '{}', 4000) || '...(truncated)'
                        ELSE cmp.node #>> '{}' END
              WHEN 'number' THEN cmp.value_number IS DISTINCT FROM (cmp.node #>> '{}')::numeric
              WHEN 'boolean' THEN cmp.value_bool IS DISTINCT FROM (cmp.node #>> '{}')::boolean
              WHEN 'array' THEN cmp.value_json IS DISTINCT FROM cmp.node
              ELSE true
            END) AS values_differ,
        (SELECT count(*) FROM val WHERE has_def) AS values_with_def
)
SELECT kv.key, kv.n, kv.label
FROM counts c
CROSS JOIN LATERAL (VALUES
    ('2a', c.removed_row_events, 'repeat events still active although their row was removed from the submission'),
    ('2b', c.removed_row_tall, 'tall rows still present although their repeat row was removed'),
    ('2c', c.removed_row_subs, 'submissions with removed repeat rows still projected'),
    ('2d', (c.removed_row_events + c.removed_row_tall > 0)::int, 'templates with removed repeat rows still projected'),
    ('3a', c.stale, 'tall values the current submission no longer carries (field removed or cleared)'),
    ('3b', c.stale_cleared, 'of 3a: the field is still there but now null or empty'),
    ('3c', c.tall_context_differs, 'tall rows whose village, assignment, team or activity differs from the submission'),
    ('3d', c.events_context_differs, 'events whose village, assignment, team or activity differs from the submission'),
    ('3e', c.events_modifier_differs, 'events whose last modifier differs from the submission''s (kept from the first projection)'),
    ('5a', c.unprojected_subs, 'submissions with data that were never projected'),
    ('5b', c.unprojected_values, 'values with a field definition in those submissions'),
    ('6a', c.values_no_def, 'values with no field definition (never projected)'),
    ('6b', c.rows_without_event, 'repeat rows with no event (their section has no field definition)'),
    ('6c', c.subs_without_any_def, 'submissions whose data matches none of their form''s field definitions'),
    ('7a', c.missing, 'values or rows with a field definition that tall lacks (projected submissions)'),
    ('7b', c.collapsed_values, 'values in repeat rows without an _id, collapsed onto the submission key'),
    ('7c', c.collapsed_subs, 'submissions with such collapsed values'),
    ('7d', c.values_differ, 'values whose tall copy differs from the submission'),
    ('7e', c.values_with_def, 'reference: values with a field definition')
) AS kv(key, n, label);
