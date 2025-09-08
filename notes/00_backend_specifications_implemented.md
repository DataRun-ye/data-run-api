# Datarun — Backend Data Model & ETL (IMPLEMENTED REFERENCE)

> **Status:** All items described below are implemented and present in the repository (DDL, ETL, materialized views, and operational jobs). This document is a concise, developer-focused reference describing the canonical data model and normalization logic used in production.

---

## Important implementation note (explicit)
**`DataSubmission`** and **`DataTemplateVersion`** are canonical database-backed entities (tables). They are **not** stored as opaque rows only — rather, certain properties are stored as JSONB payload snapshots inside their respective tables:
- `DataSubmission.form_data` (JSONB) — the immutable submission payload snapshot stored on `data_submission`.
- `DataTemplateVersion.fields` (JSONB) — snapshot of the template's element definitions (`FormDataElementConf`).
- `DataTemplateVersion.sections` (JSONB) — snapshot of the template's section configurations (`FormSectionConf`).

The tables `data_submission` and `data_template_version` are canonical rows with typed columns; the JSONB payloads above are snapshots used by ETL and for reproducible re-processing.

---

## 1 — Purpose & implemented design goals
- **Goal (implemented):** Accept nested JSON submissions from clients, retain an immutable raw ledger, and run an idempotent, re-runnable ETL that normalizes submissions into typed, analytics-ready fact rows.
- **Principles applied:** immutability (template versions), reproducibility (submissions reference exact template version), idempotence (unique dedupe keys), and analytics performance (typed fact columns + materialized views).

---

## 2 — Platform & tech stack (implemented)
- Java 17+, Spring Boot 3.4.x; Maven; Liquibase migrations; jOOQ available; Spring `JdbcTemplate`/`NamedParameterJdbcTemplate` used in ETL.
- PostgreSQL (JSONB), Testcontainers + JUnit5 used for tests.
- Caching: Ehcache; Codegen: Lombok, MapStruct.
- Clients: Angular (web) and Flutter (mobile).
- IDs: internal `id` = ULID (26 chars), business `uid` ~11 chars.

---

## 3 — Identifiers & versioning (implemented)
- `id` (VARCHAR(26)) — ULID primary key, immutable.
- `uid` (VARCHAR(11)) — business key, unique, stable across systems.
- Template versions are immutable rows in `data_template_version`; submissions reference `template_id` + `template_version_id` to guarantee reproducibility.

---

## 4 — Canonical dimension model (implemented)
Canonical dimension tables are authoritative for joins and analytics:
- `data_element` — canonical definitions (value type, optionSet ref, aggregation hints).
- `option_set`, `option_value` — select lists and values.
- `org_unit`, `team`, `activity`, etc. — context/dimension tables used in analytics.

---

## 5 — Template & submission model (implemented)
- `data_template` + `data_template_version` persist template header and versioned payloads.
- `data_template_version.fields` and `.sections` are JSONB snapshots that hold per-version element and section configuration (e.g., `FormDataElementConf` → `data_element` reference, `path`, `mandatory`, `repeatable`, `rules`).
- `data_submission` stores the original client payload in `form_data` JSONB and exposes typed columns for top-level metadata (org unit, team, activity, timestamps).

---

## 6 — ETL design & execution (implemented)
- ETL is idempotent and implemented as a sweep-update transaction per submission:
  1. Load `data_submission.form_data` and the referenced `data_template_version.fields/sections`.
  2. Insert or upsert `repeat_instance` rows representing repeats and their hierarchy.
  3. Normalize values into `element_data_value` typed rows (one atomic value per row; select-multi expands to multiple rows).
  4. Soft-mark stale rows from previous runs (if any) and insert current rows.
  5. Record ETL run metadata (`etl_version`, `run_ts`, `checksum`) for traceability.
- Deduplication is enforced by unique constraints using stable composite keys (e.g., `submission_uid + element_uid + repeat_instance_key + selection_key`).

---

## 7 — Fact storage (implemented)
- `element_data_value` stores normalized atomic values with typed columns:
  - `value_num`, `value_bool`, `value_ref_uid`, `option_uid`, `value_ts`, `value_text`.
- Context columns include `submission_uid`, `assignment_uid`, `team_uid`, `org_unit_uid`, `activity_uid`, `element_uid`, `element_template_config_uid`, `repeat_instance_id`.
- Unique index `ux_element_value_unique` enforces idempotence for re-run ETL.

---

## 8 — Repeat groups (implemented)
- Repeat groups are modeled as rows in `repeat_instance`:
  - Each row captures `id`, `parent_repeat_instance_id`, `submission_uid`, `repeat_path`, `repeat_index`, and `repeat_section_label`.
- `element_data_value` rows link to the corresponding `repeat_instance_id` so the hierarchy is preserved for analytics joins.

---

## 9 — Analytics & materialized views (implemented)
- Materialized views (e.g., `pivot_grid_facts`) flatten `element_data_value` with submission, template metadata, option labels, and dimension joins for efficient reporting.
- MV refresh jobs are scheduled and `etl_version` is recorded for traceability of which ETL run produced the derived rows.

---

## 10 — Operational guarantees (in place)
- **Reproducibility:** enforced by immutable `data_template_version` and `data_submission` payload snapshots.
- **Idempotence:** enforced via dedupe keys and unique indexes in `element_data_value`.
- **Auditability:** raw `data_submission.form_data` retained; ETL run metadata recorded.
- **Performance:** typed columns and MVs reduce runtime JSONB parsing for analytics.

---

## 11 — Known risks & applied mitigations
- **Template drift:** canonical `data_element` metadata is authoritative; template-scoped copies are maintained in `data_template_version` only when necessary.
- **Select-multi explosion:** modeled as one row per option to simplify analytics; selection-level dedupe keys included.
- **MV refresh & ETL versioning:** implemented to avoid stale analytics results after ETL/schema changes.

---

## 12 — Acceptance checklist (implemented / verified)
- [x] `data_template_version` is immutable and `data_submission` references exact version.  
- [x] `data_submission.form_data` is stored unchanged as JSONB.  
- [x] ETL writes typed columns into `element_data_value` and enforces dedupe.  
- [x] `repeat_instance` hierarchy is preserved and linked.  
- [x] Unique indexes enforce idempotence.  
- [x] ETL run metadata (`etl_version`, `run_ts`, `checksum`) recorded.  
- [x] Materialized views for analytics exist and are refreshable.

---

## Appendix — DDL (shortened for brevity)

### 2.1 `org_unit` (Dimension)
**Purpose:** Geographic or organizational hierarchies.

```sql
CREATE TABLE org_unit (
  id        VARCHAR(26) PRIMARY KEY,
  uid       VARCHAR(11) NOT NULL UNIQUE,
  parent_id VARCHAR(26) REFERENCES org_unit(id),
  path      TEXT NOT NULL,
  level     INTEGER NOT NULL,
  code      VARCHAR(50) UNIQUE,
  name      VARCHAR(255) NOT NULL,
  label     JSONB,
  disabled  BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_org_unit_parent_id ON org_unit(parent_id);
```

### 2.3 `team` (Dimension)
**Purpose:** Teams used as context for submissions.

```sql
CREATE TABLE team (
  id        VARCHAR(26) PRIMARY KEY,
  uid       VARCHAR(11) NOT NULL UNIQUE,
  name      VARCHAR(255) NOT NULL UNIQUE,
  label     JSONB,
  disabled  BOOLEAN DEFAULT FALSE
);
CREATE UNIQUE INDEX ux_team_name ON team(name);
```

### 2.4 `activity` (Dimension)
**Purpose:** Activities used as context for submissions.

```sql
CREATE TABLE activity (
  id        VARCHAR(26) PRIMARY KEY,
  uid       VARCHAR(11) NOT NULL UNIQUE,
  name      VARCHAR(255) NOT NULL UNIQUE,
  label     JSONB,
  "from"    TIMESTAMP WITH TIME ZONE,
  "to"      TIMESTAMP WITH TIME ZONE,
  disabled  BOOLEAN DEFAULT FALSE
);
CREATE UNIQUE INDEX ux_activity_name ON activity(name);
```

### 2.6 `data_element` (Dimension)
**Purpose:** Canonical list of data elements that can be collected.

```sql
CREATE TABLE data_element (
  id           VARCHAR(26) PRIMARY KEY,
  uid          VARCHAR(11) NOT NULL UNIQUE,
  name         VARCHAR(255) NOT NULL UNIQUE,
  label        JSONB,
  value_type   VARCHAR(50) NOT NULL,
  description  VARCHAR(500),
  disabled     BOOLEAN DEFAULT FALSE,
  option_set_id VARCHAR(26) REFERENCES option_set(id),
  is_dimension BOOLEAN NOT NULL DEFAULT TRUE,
  is_measure   BOOLEAN NOT NULL,
  aggregation_type VARCHAR(50)
);
```

**Notes:** `Date`/`DateTime`/`Time` values are normalized to UTC `element_data_value.value_ts` during ETL. Age is derived at normalization time when DOB is provided.

### 2.7 `option_set`
**Purpose:** Groups of selectable options.

```sql
CREATE TABLE option_set (
  id          VARCHAR(26) PRIMARY KEY,
  uid         VARCHAR(11) NOT NULL UNIQUE,
  name        VARCHAR(255) NOT NULL UNIQUE,
  description VARCHAR(500),
  disabled    BOOLEAN DEFAULT FALSE,
  label       JSONB
);
```

### 2.8 `option_value` (Dimension)
**Purpose:** Options belonging to a set.

```sql
CREATE TABLE option_value (
  id          VARCHAR(26) PRIMARY KEY,
  uid         VARCHAR(11) NOT NULL UNIQUE,
  sort_order  INTEGER NOT NULL,
  code        VARCHAR(255) NOT NULL,
  name        VARCHAR(255) NOT NULL,
  description VARCHAR(500),
  label       JSONB,
  deleted     BOOLEAN DEFAULT FALSE,
  option_set_id VARCHAR(26) REFERENCES option_set(id)
);
CREATE UNIQUE INDEX ux_option_value_code ON option_value(option_set_id, code);
CREATE UNIQUE INDEX ux_option_value_name ON option_value(option_set_id, uid);
CREATE UNIQUE INDEX ux_option_value_name ON option_value(option_set_id, name);
```

### 2.10 `data_template` & `data_template_version` (Form templates)
**Purpose:** Store templates and immutable versioned payloads.

```sql
CREATE TABLE data_template (
  id          VARCHAR(26) PRIMARY KEY,
  uid         VARCHAR(11) NOT NULL UNIQUE,
  name        VARCHAR(255) NOT NULL UNIQUE,
  label       JSONB,
  version_uid VARCHAR(11),
  deleted     BOOLEAN DEFAULT FALSE
);

CREATE TABLE data_template_version (
  id              VARCHAR(26) PRIMARY KEY,
  template_id     VARCHAR(26) NOT NULL REFERENCES data_template(id),
  version_number  INTEGER NOT NULL,
  fields          JSONB,
  sections        JSONB,
  release_notes   TEXT
);
```

**Notes:** `fields` / `sections` are JSONB snapshots of the template-specific configuration (element references to `data_element.id`, `path`, `repeatable`, `validators`, etc.). Runtime DTOs are produced by merging `data_template` + chosen `data_template_version`.

---

## 3 — Fact tables (canonical write model)

### 3.1 `data_submission`
**Purpose:** Stores the original submission and top-level metadata (source of truth for ETL).

```sql
CREATE TABLE data_submission (
  id                   VARCHAR(26) PRIMARY KEY,
  uid                  VARCHAR(11) NOT NULL UNIQUE,
  org_unit_id          VARCHAR(26) NOT NULL REFERENCES org_unit(id),
  team_id              VARCHAR(26) NOT NULL REFERENCES team(id),
  activity_id          VARCHAR(26) NOT NULL REFERENCES activity(id),
  template_id          VARCHAR(26) NOT NULL REFERENCES data_template(id),
  template_version_id  VARCHAR(26) NOT NULL REFERENCES data_template_version(id),
  finished_entry_time  TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted               BOOLEAN DEFAULT FALSE,
  deleted_at            TIMESTAMP WITH TIME ZONE,
  form_data             JSONB,
  created_date          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
```

### 3.1 `data_submission_history`
**Purpose:** Keeps snapshots of updates to `data_submission` up to a configured `MAX_INSTANCES`.

```sql
CREATE TABLE data_submission_history (
  id                   VARCHAR(26) PRIMARY KEY,
  uid                  VARCHAR(11) NOT NULL UNIQUE,
  org_unit_id          VARCHAR(26) NOT NULL REFERENCES org_unit(id),
  team_id              VARCHAR(26) NOT NULL REFERENCES team(id),
  activity_id          VARCHAR(26) NOT NULL REFERENCES activity(id),
  template_id          VARCHAR(26) NOT NULL REFERENCES data_template(id),
  template_version_id  VARCHAR(26) NOT NULL REFERENCES data_template_version(id),
  finished_entry_time  TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted               BOOLEAN DEFAULT FALSE,
  deleted_at            TIMESTAMP WITH TIME ZONE,
  form_data             JSONB,
  created_date          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
```

---

## 4 — Operational notes & recommended references
- ETL versioning: `etl_version` is recorded on each run to allow tracing derived facts back to the ETL code that produced them.
- Tests: fixtures-driven tests exist covering canonical submission examples → expected `element_data_value` rows.
- Re-processing playbook: documented scripts and steps exist for re-running ETL after schema or logic changes (see repo scripts).
- Monitoring: ETL job durations, MV refresh durations and failure alerts are instrumented.

---

**If you'd like:** I can also:
- produce a one-page PDF export of this Markdown, or
- commit this file into a chosen repo branch (I can prepare a patch file),
- or expand the appendix with the `element_data_value` and `repeat_instance` DDL and example ETL SQL.

