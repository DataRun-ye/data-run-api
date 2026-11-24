-- Check 8: pivot tables against active events, and the pivot manifest.
--
-- Pivot tables (pivot.fact_<template code>) are snapshots, built only when
-- someone asks for them (one template or a list, each rebuilt whole). A build
-- writes <name>_new, renames the current table to <name>_old and <name>_new to
-- <name>. The name follows the template's code at build time (Naming.java),
-- but the manifest keeps the name of the first build.
-- "Active events" are analytics.events rows with deleted_at null.
-- data_template.uid and code are nullable, so names are compared with EXISTS.

WITH tmpl AS (
    SELECT t.uid, 'fact_' || lower(regexp_replace(coalesce(t.code, t.uid), '[^A-Za-z0-9]', '_', 'g')) AS name
    FROM data_template t
),
pivots AS (
    SELECT c.oid, c.relname
    FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'pivot' AND c.relkind IN ('r', 'v', 'm')
),
current_pivots AS (
    -- pivot tables under a template's current name, and the columns the checks read
    SELECT p.relname,
           bool_or(a.attname = 'event_id') AND bool_or(a.attname = 'event_type')
           AND bool_or(a.attname = 'submission_uid') AND bool_or(a.attname = 'deleted_at') AS has_columns
    FROM pivots p
    JOIN pg_attribute a ON a.attrelid = p.oid AND a.attnum > 0 AND NOT a.attisdropped
    WHERE EXISTS (SELECT 1 FROM tmpl t WHERE t.name = p.relname)
    GROUP BY p.relname
),
active AS (
    SELECT DISTINCT e.template_uid FROM analytics.events e WHERE e.deleted_at IS NULL
),
counts AS (
    SELECT
        (SELECT count(*) FROM active a JOIN tmpl t ON t.uid = a.template_uid
          WHERE NOT EXISTS (SELECT 1 FROM pivots p WHERE p.relname = t.name)) AS no_pivot,
        (SELECT count(*) FROM pivots p
          WHERE NOT EXISTS (SELECT 1 FROM tmpl t WHERE t.name = p.relname)
            AND p.relname NOT LIKE '%\_old' AND p.relname NOT LIKE '%\_new') AS former_names,
        (SELECT count(*) FROM analytics.pivot_manifest m
          WHERE m.status = 'success' AND to_regclass(m.view_name) IS NULL) AS manifest_missing,
        (SELECT count(*) FROM analytics.pivot_manifest m JOIN tmpl t ON t.uid = m.template_uid
          WHERE m.view_name IS DISTINCT FROM 'pivot.' || t.name) AS manifest_drift,
        (SELECT count(*) FROM current_pivots WHERE NOT has_columns) AS unchecked
)
SELECT kv.key, kv.n, kv.label
FROM counts c
CROSS JOIN LATERAL (VALUES
    ('8a', 0::bigint, 'active events missing from their template''s pivot table'),
    ('8b', 0::bigint, 'pivot rows that are not active events (voided, or gone from events)'),
    ('8c', c.no_pivot, 'templates with active events and no pivot table under the current name (built on request; not a fault by itself)'),
    ('8d', c.former_names, 'pivot tables left under a name no template has now'),
    ('8e', c.manifest_missing, 'manifest entries marked success whose table does not exist'),
    ('8f', c.manifest_drift, 'manifest entries whose name differs from the current naming'),
    ('8g', 0::bigint, 'active pivot rows of repeat rows that were removed from their submission'),
    ('8h', c.unchecked, 'pivot tables under a current name lacking a column 8a, 8b or 8g read (not fully checked)')
) AS kv(key, n, label);

-- 8a, 8b and 8g: one statement per pivot table under its template's current
-- name. 8g reads the template's payloads (their repeat row _ids), one
-- template at a time. A table without event_id cannot be compared (8h).
WITH tmpl AS (
    SELECT t.uid, 'fact_' || lower(regexp_replace(coalesce(t.code, t.uid), '[^A-Za-z0-9]', '_', 'g')) AS name
    FROM data_template t
),
pivot_table AS (
    SELECT t.uid, t.name,
           bool_or(a.attname = 'deleted_at') AS has_deleted_at,
           bool_or(a.attname = 'event_type') AND bool_or(a.attname = 'submission_uid') AS has_row_columns
    FROM tmpl t
    JOIN pg_class c ON c.relname = t.name AND c.relkind IN ('r', 'v', 'm')
    JOIN pg_namespace n ON n.oid = c.relnamespace AND n.nspname = 'pivot'
    JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum > 0 AND NOT a.attisdropped
    GROUP BY t.uid, t.name
    HAVING bool_or(a.attname = 'event_id')
)
SELECT format($stmt$
    SELECT '8a', count(*), 'active events missing from their template''s pivot table', %1$L
    FROM analytics.events e
    WHERE e.template_uid = %2$L AND e.deleted_at IS NULL
      AND NOT EXISTS (SELECT 1 FROM pivot.%1$I p WHERE p.event_id = e.event_id)
    UNION ALL
    SELECT '8b', count(*), 'pivot rows that are not active events (voided, or gone from events)', %1$L
    FROM pivot.%1$I p
    WHERE %3$s NOT EXISTS (SELECT 1 FROM analytics.events e WHERE e.event_id = p.event_id AND e.deleted_at IS NULL)
    UNION ALL
    %4$s
    $stmt$,
    p.name, p.uid,
    CASE WHEN p.has_deleted_at THEN 'p.deleted_at IS NOT NULL OR' ELSE '' END,
    CASE WHEN NOT p.has_row_columns THEN
        format($none$SELECT '8g', 0::bigint, 'active pivot rows of repeat rows that were removed from their submission', %1$L$none$, p.name)
    ELSE format($branch$
    SELECT '8g', count(*), 'active pivot rows of repeat rows that were removed from their submission', %1$L
    FROM (
        WITH row_ids AS MATERIALIZED (
            SELECT DISTINCT s.uid AS sub_uid, x #>> '{}' AS row_id
            FROM data_submission s CROSS JOIN LATERAL jsonb_path_query(s.form_data, 'lax $.**._id') x
            WHERE s.template_uid = %2$L AND NOT coalesce(s.deleted, false)
        )
        SELECT 1
        FROM pivot.%1$I p
        JOIN data_submission s ON s.uid = p.submission_uid AND NOT coalesce(s.deleted, false)
        WHERE p.event_type = 'repeat' %3$s
          AND NOT EXISTS (SELECT 1 FROM row_ids r WHERE r.sub_uid = p.submission_uid AND r.row_id = p.event_id)
    ) removed
    $branch$, p.name, p.uid, CASE WHEN p.has_deleted_at THEN 'AND p.deleted_at IS NULL' ELSE '' END)
    END)
FROM pivot_table p
ORDER BY p.name
\gexec
