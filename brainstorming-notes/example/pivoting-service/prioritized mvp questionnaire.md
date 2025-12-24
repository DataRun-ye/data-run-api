# Quick checklist of what I will assume going forward (so we don't argue later)

* Analytics will join `tall_canonical.instance_key = events.instance_key` as strings.
* Grouping by anchors uses `events.anchor_ref_uid`; grouping by CE uses `tall_canonical.canonical_element_id` (uuid) +
  typed value columns.
* `tall.value_ref_uid` is materialized and authoritative for joins to `dim_*`.
* Submission-level filters should default to `submission_keys` where possible.
* events.anchor_ce_id is guaranteed to be CE UUID but string, i already fixed that, man. it's a deterministically a 1->1 relation.
* anchors values are mostly of ref type (ou, option, team, assignment, activity, etc)


# Prioritized, practical questionnaire (strict, minimal, no fluff)


Answer these (copy/paste back). Mark required fields; if unsure, pick suggested default.

**A. Core scope (MVP) — REQUIRED**

1. `top_dims` (ranked list, top 8):
Answer: `org_unit_uid`,`team_uid`,`template_uid`,`activity_uid`,`assignment_uid`,`submission_creation_time`,`submission_uid`,`anchor_ref_uid`

2. `top_metrics` (ranked, include exact/approx):
   Answer: `[{"id":"count_submissions","agg":"count","exact":true},{"id":"avg_value","agg":"avg","ce_id":"<uuid>","exact":true},{"id":"unique_users","agg":"count_distinct","approx":true}]` —
   **Provide list.**
3. Is `anchor_ref_uid` grouping **required** for MVP? (`yes`)

**B. Freshness & performance — REQUIRED**

4. `staleness_target` for dashboards (choose one):  *Answer:* `Hourly`
5. `interactive_latency_target_95` (choose): `<20s`.
6. `typical_and_peak_queries_per_min`: `typical: 10/min, peak: 200/min`.

**C. Accuracy & heavy ops — REQUIRED**

7. `count_distinct_policy`: `API opt-in approx`.
8. `allow_large_groupbys`: `warn_and_require_allow`

**D. Security & tenancy — REQUIRED**

9. `multi_tenancy_model`: `per_org_unit`
10. `rbac_model`: `role_based(analyst/viewer/admin)`
11. `exports_needed`: `CSV`.
12. `superset_integration`: `plugin_mapping_api`.

---

Paste answers back exactly as JSON or plain key: value pairs. I will immediately (in the next reply) convert them
into: (1) a locked MVP scope with explicit acceptance tests, (2) the minimal OpenAPI spec plus jOOQ query templates
needed, and (3) a prioritized dev backlog. No fluff.
