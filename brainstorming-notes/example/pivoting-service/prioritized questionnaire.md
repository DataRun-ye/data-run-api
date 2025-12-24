# Prioritized stakeholder questionnaire (copy/paste-ready)

**Instructions for stakeholders:** mark required fields, pick from options or give short free-text. If unsure, choose the answer. Return this form as JSON or simple key: value pairs.

---

## Priority 1 — Business/usage scope

1. **Top 10 dimensions we must support for MVP** (ranked: 1 = highest).
   Example answer: `["org_unit_uid","team_uid","template_uid","activity_uid","assignment_uid","submission_creation_time","anchor_ref_uid","instance_key","submission_id","user_id"]`
   *Answer:* `org_unit_uid, team_uid, template_uid, activity_uid, submission_creation_time, submission_uid, submission_id, anchor_ref_uid, assignment_uid, template_uid`
   *Why:* drives exposed API fields, indexes, and pre-aggregates. **(Required)**

2. **Top 10 metrics for MVP** (ranked). For each note whether **exact** or **approx OK**.
   Example: `[{name:"count_submissions",type:"count",exact:true},{name:"avg_score",type:"avg",exact:true},{name:"unique_users",type:"count_distinct",exact:false}]`
   *Answer:* count_submissions (exact), count_instances (exact), avg(value_number) (exact), count_distinct(value_ref_uid) (approx OK). **(Required)**

3. **Is anchor-based grouping (anchor_ref_uid) critical for MVP dashboards?**
   Options: `Critical` | `Nice-to-have` | `No`
   *Answer:* `Nice-to-have`. **(Required)**

---

## Priority 2 — Freshness, scale & SLAs

4. **Acceptable staleness for pre-aggregates / dashboard data**
   Options: `Near real-time (seconds-minutes)` | `Hourly` | `Daily` | `Weekly`
   *Answer:* `Hourly`. **(Required)**

5. **Interactive query latency target (95th percentile)**
   Options: `<2s`, `<5s`, `<15s`, `<30s`, `>30s`
   *Answer:* `<20s`. **(Required)**

6. **Expected query volume / concurrency** (typical and peak per minute)
   Answer: `typical: 5/min, peak: 200/min`
   *Why:* influences caching & autoscaling. **(Required)**

---

## Priority 3 — Accuracy & heavy ops

7. **Count-distinct policy** — choose preferred default for `count_distinct` on high-cardinality fields.
   Options: `Exact only`, `Approx by default (hll)`, `API opt-in approx`
   *Answer:* `API opt-in approx`. **(Optional)**

8. **Large exports / async jobs required?**
   Options: `Yes (CSV/Parquet)`, `CSV only`, `No`
   *Answer:* `Yes (CSV)`. **(Optional)**

9. **Tolerance for high-cardinality group-bys in interactive UI**
   Options: `Block by default`, `Warn & require explicit allow`, `Allow freely`
   *Answer:* `Warn & require explicit allow`. **(Required)**

---

## Priority 4 — Security, tenancy, RBAC

10. **Multi-tenancy model**
    Options: `Tenant-per-org_unit`, `Tenant-per-customer (multi-org)`, `Single-tenant`
    *Answer:* `Tenant-per-org_unit`. **(Required)**

11. **RBAC needs** — who can run what (choose one to start):
    Options: `All analysts have same access`, `Role-based: analyst / admin / viewer`, `Fine-grained: per-dimension/anchor`
    *Answer:* `Role-based`. **(Required)**

12. **Allow SQL preview only or full SQL Lab (run arbitrary SQL)?**
    Options: `SQL preview only`, `SQL Lab (restricted execution)`, `Full SQL allowed`
    *Answer:* `SQL preview only`. **(Required)**

---

## Priority 5 — Operations & monitoring

13. **Monitoring & alerts: which metrics matter most?** (pick top 5)
    Examples: `query_latency`, `error_rate`, `cache_hit_ratio`, `preagg_staleness`, `job_queue_depth`
    *Answer:* `query_latency, error_rate, cache_hit_ratio, preagg_staleness, slow_queries`. **(Optional)**

14. **Backfill policy for historical changes / ref_resolution updates**
    Options: `On-demand/manual backfill`, `Automatic backfill nightly`, `Stream incremental backfill`
    *Answer:* `Automatic backfill nightly`. **(Optional)**

15. **Data retention / archiving policy for tall rows & events**
    Options: `Keep all (no retention)`, `1 year`, `3 years`, `Custom (specify)`
    *Answer:* `3 years`. **(Optional)**

---

## Priority 6 — UX / Superset integration

16. **Superset integration preference**
    Options: `Lightweight plugin mapping to API (recommended)`, `Virtual table in Superset`, `Direct SQL datasource`
    *Answer:* `Lightweight plugin mapping to API`. **(Required)**

17. **Saved queries / templates needed in MVP?**
    Options: `Yes`, `No`
    *Answer:* `Yes (basic catalog)`. **(Optional)**

18. **Export formats required**
    Options: `CSV`, `Parquet`, `JSON`, `All`
    *Answer:* `CSV`. **(Optional)**

---

## Priority 7 — Acceptance & priorities

19. **What are the top 3 dashboards or reports that must work on day-1?**
    Provide names and a sentence describing required groupings/metrics and dims. **(Required)**

20. **Who are project stakeholders / owners and decision makers?** (name & contact)
    *Why:* helps resolve ambiguity quickly. **(Required)**

---

## Optional / Extra context

21. **Any existing cardinality pain points or known heavy CEs?** (list CE paths or UUIDs)
22. **Any regulatory / privacy constraints (e.g., PII masking, data location)?**
23. **Preferred export delivery (S3, internal share, email)?**

---

When you get answers back, paste them here. I’ll:

* convert responses into an MVP scope with explicit acceptance tests,
* produce the OpenAPI for the MVP endpoints tailored to your selected dims/metrics,
* generate the initial jOOQ query templates and sample Liquibase migrations for required pre-aggregates,
* and propose a prioritized dev backlog with effort-levels per task.

Want me to convert the form into a short Google Form / JSON schema next?
