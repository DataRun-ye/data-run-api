Nice — here’s the **full canonical inventory** you asked for: every layer / entity / element / step / logic item given a conventional name, short responsibility, minimal schema (SQL you can paste/run for the pilot), a JSON schema for `projection_config`, two concrete `projection_config` seeds (supply and breedingsources), the evaluator token language, upsert template for semantic tables, and a short next-step checklist.

I distilled our previous discussions and your platform constraints (Postgres ≥v15, ULID as `varchar(26)`, payload.\_id is stable, `ElementTemplateConfig` exists, etc.). These artifacts are directly actionable — run the SQL, load the seeds, wire the runner to read `projection_config` rows, and the rest follows.

# 1) Conventions & small glossary (quick)

* ULID PKs: `varchar(26)` for domain rows (`id` in business tables).
* Business UIDs: `uid` (11-char) for top-level entities (you already have them).
* Repeat canonical id: `repeat_uid` format `RPT_xxx` (text).
* `template_version_uid` is recorded everywhere for provenance.
* `payload_id` = `payload->_id` (client \_id), stable and required for raw repeat extraction.
* `extra` / `payload` fields are JSONB. Always keep them.

---

# 2) DDLs (SQL) — paste & run for pilot

Run these as plain SQL (I kept them simple, you can convert to Liquibase if you prefer later).

```sql
-- 2.1 repeat_definition (canonical repeat registry)
CREATE TABLE IF NOT EXISTS repeat_definition (
  repeat_uid VARCHAR(64) PRIMARY KEY,             -- e.g. RPT_xxx
  canonical_name VARCHAR(500) NOT NULL,            -- human friendly name
  default_semantic_paths JSONB DEFAULT '[]'::jsonb, -- known semantic_path examples per template/version
  is_entity_candidate BOOLEAN DEFAULT false,
  natural_key_strategy JSONB DEFAULT '{}'::jsonb,  -- e.g. {"type":"payload_id"} or {"type":"composite","fields":["amd","month_name"]}
  notes VARCHAR(2500),
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);
```

```sql
-- 2.2 raw_repeat_payload (staging area for repeat occurrences)
CREATE TABLE IF NOT EXISTS raw_repeat_payload (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  repeat_uid VARCHAR(64) NOT NULL REFERENCES repeat_definition(repeat_uid),
  submission_uid VARCHAR(11) NOT NULL,  --new         -- business submission uid
--   submission_id VARCHAR(26),    --new          -- DataSubmission.id (ULID) if you want internal PK
  template_uid VARCHAR(11),     --new                  -- template id if known
  template_version_uid VARCHAR(11) NOT NULL,      -- essential for provenance
  semantic_path VARCHAR(3000) NOT NULL,  --repeat_path           -- canonical semantic path used for extraction
  payload_id VARCHAR(64) NOT NULL UNIQUE,                -- payload->_id (natural key candidate)
  payload JSONB NOT NULL,                  -- the repeat instance payload
  occurrence_index INTEGER DEFAULT 0,
  payload_checksum TEXT,                   -- sha256(payload::text)
  extracted_at TIMESTAMP DEFAULT now(),
  extracted_by VARCHAR(64),            --new           -- service id
  projection_run_id UUID,       --new           -- last projection run that touched it (nullable)
  UNIQUE (submission_uid, repeat_uid, payload_id)
);

CREATE INDEX IF NOT EXISTS idx_raw_repeat_repeat_uid ON raw_repeat_payload(repeat_uid);
CREATE INDEX IF NOT EXISTS idx_raw_repeat_template_version ON raw_repeat_payload(template_version_uid);
CREATE INDEX IF NOT EXISTS idx_raw_repeat_submission ON raw_repeat_payload(submission_uid);
```

```sql
-- add versioning, activation, extracted convenience columns, author, checksum, and timestamps
ALTER TABLE projection_config
    ADD COLUMN version INTEGER DEFAULT 1,
    ADD COLUMN active BOOLEAN DEFAULT TRUE,
    ADD COLUMN target_table VARCHAR(128),
    ADD COLUMN natural_key_expr TEXT,
    ADD COLUMN mappings JSONB,
    ADD COLUMN post_processing JSONB,
    ADD COLUMN dedupe JSONB,
    ADD COLUMN provenance JSONB,
    ADD COLUMN author VARCHAR(128),
    ADD COLUMN config_checksum TEXT,
    ADD COLUMN updated_at TIMESTAMPTZ DEFAULT now();

-- indexes for commonly queried columns
CREATE INDEX IF NOT EXISTS idx_projection_config_active ON projection_config(active);
CREATE INDEX IF NOT EXISTS idx_projection_config_target_table ON projection_config(target_table);
```

```sql
-- 2.4 projection_run (audit of runs)
CREATE TABLE IF NOT EXISTS projection_run (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  projection_config_id UUID NOT NULL REFERENCES projection_config(id),
  repeat_uid VARCHAR(64) NOT NULL,
  semantic_path VARCHAR(3000),
  template_version_uid VARCHAR(11),
  started_at TIMESTAMP DEFAULT now(),
  ended_at TIMESTAMP,
  status VARCHAR(64),           -- PENDING | SUCCESS | FAILED
  rows_processed INTEGER DEFAULT 0,
  error_msg TEXT,
  created_at TIMESTAMP DEFAULT now()
);
```

```sql
-- 2.5 analytics metadata catalog (small)
CREATE TABLE IF NOT EXISTS analytics_entity (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL,       -- business name e.g. breeding_source_event
  table_name VARCHAR(255) NOT NULL, -- physical table
  entity_type VARCHAR(64) NOT NULL, -- EVENT | ENTITY | RELATION
  projection_config_id UUID, -- origin
  natural_key_expr TEXT,
  notes TEXT,
  created_at TIMESTAMP DEFAULT now()
);
```

```sql
CREATE TABLE IF NOT EXISTS analytics_attribute (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  entity_id UUID NOT NULL REFERENCES analytics_entity(id),
  name VARCHAR(255) NOT NULL,         -- business attribute e.g. breeding_habitat_type
  column_name VARCHAR(64) NOT NULL,  -- physical column
  data_type VARCHAR(64) NOT NULL,    -- STRING|INT|NUM|DATE|BOOL|JSONB
  is_dimension BOOLEAN DEFAULT true,
  is_measure BOOLEAN DEFAULT false,
  aggregation_type VARCHAR(64) DEFAULT 'DEFAULT',
  option_set_uid VARCHAR(11),        -- if relevant
  label JSONB,
  created_at TIMESTAMP DEFAULT now()
);
```

```sql
CREATE TABLE IF NOT EXISTS analytics_relationship (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  from_entity_id UUID NOT NULL REFERENCES analytics_entity(id),
  to_entity_id UUID NOT NULL REFERENCES analytics_entity(id),
  from_column VARCHAR(64) NOT NULL,
  to_column VARCHAR(64) NOT NULL,
  relationship_type VARCHAR(64),     -- e.g. MANY_TO_ONE
  notes TEXT,
  created_at TIMESTAMP DEFAULT now()
);
```

```sql
-- 2.6 example exploded child table for LSM types
CREATE TABLE IF NOT EXISTS breeding_lsm_type (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  breeding_event_id UUID NOT NULL,   -- FK to breeding_source_event.id (not enforce here, pilot)
  lsm_type VARCHAR(64) NOT NULL,
  created_at TIMESTAMP DEFAULT now(),
  UNIQUE (breeding_event_id, lsm_type)
);
CREATE INDEX IF NOT EXISTS idx_breeding_event_id ON breeding_lsm_type(breeding_event_id);
```

```sql
-- 2.7 sample semantic tables (pilot)
CREATE TABLE IF NOT EXISTS breeding_source_event (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  natural_key TEXT UNIQUE,        -- e.g. payload_id
  submission_uid VARCHAR(11),
  template_uid VARCHAR(11),
  template_version_uid VARCHAR(11),
  projection_run_id UUID,
  occurrence_index INT,
  household_name VARCHAR(500),
  breeding_habitat_type_uid VARCHAR(255),
  breeding_habitat_type_label JSONB,
  larval_stage_presence BOOLEAN,
  growth_regulator_grams INT,
  breeding_habitat_description TEXT,
  breeding_habitats_count INT,
  breeding_habitat_length_uid VARCHAR(255),
  breeding_habitat_width_uid VARCHAR(255),
  breeding_habitat_depth_uid VARCHAR(255),
  temphos_ml NUMERIC,
  lsm_types JSONB,
  extra JSONB,
  payload_checksum TEXT,
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);
```

```sql
CREATE TABLE IF NOT EXISTS supply_event (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  natural_key TEXT UNIQUE,
  submission_uid VARCHAR(11),
  template_uid VARCHAR(11),
  template_version_uid VARCHAR(11),
  projection_run_id UUID,
  occurrence_index INT,
  payload_id VARCHAR(64),
  amd_uid VARCHAR(255),
  amd_label JSONB,
  month_name_uid VARCHAR(255),
  supply_date DATE,
  stockout_days INT,
  expired_quantity INT,
  consumed_quantity INT,
  received_quantity INT,
  available_quantity INT,
  redistributed_quantity INT,
  extra JSONB,
  payload_checksum TEXT,
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);
```

> Notes: adjust FK relationships to actual semantic table PKs if you want referential integrity. For the pilot keep FKs loose and rely on projection-run upserts to ensure consistency.

---

# 3) `projection_config` JSON schema (minimal, pragmatic)

Use this schema to validate projection\_config payloads before inserting. The runner uses it.

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ProjectionConfig",
  "type": "object",
  "required": ["source_repeat_uid", "target_table", "natural_key_expr", "mappings", "version"],
  "properties": {
    "source_repeat_uid": {"type": "string"},
    "version": {"type": "integer"},
    "semantic_paths": {"type": "array", "items": {"type": "string"}},
    "target_table": {"type": "string"},
    "natural_key_expr": {"type": "string"},
    "mappings": {
      "type": "object",
      "additionalProperties": {"type": "string"}
    },
    "post_processing": {
      "type": "object",
      "properties": {
        "explode": {
          "type": "array",
          "items": {
            "type": "object",
            "required": ["field", "target_table", "parent_key"],
            "properties": {
              "field": {"type": "string"},
              "target_table": {"type": "string"},
              "parent_key": {"type": "string"},
              "natural_key_expr": {"type": "string"}
            }
          }
        }
      }
    },
    "author": {"type": "string"},
    "notes": {"type": "string"},
    "active": {"type": "boolean"}
  }
}
```

---

# 4) Two concrete `projection_config` seeds (paste into `projection_config`):

### 4.1 Supply (RPT\_aaf9a7ade8ad8535)

```sql
INSERT INTO projection_config (
  id, source_repeat_uid, version, target_table, natural_key_expr, mappings, post_processing, author, active
) VALUES (
  gen_random_uuid(),
  'RPT_aaf9a7ade8ad8535',
  1,
  'supply_event',
  'payload->_id',   -- token expression evaluated by runner
  '{
    "natural_key": "payload->_id",
    "payload_id": "payload->_id",
    "submission_uid": "submission_uid",
    "occurrence_index": "occurrence_index",
    "amd_uid": "OPTION_RESOLVE(payload->>''amd'', ''sYiS5D2qeG8'')",
    "amd_label": "OPTION_LABEL(payload->>''amd'', ''sYiS5D2qeG8'')",
    "month_name_uid": "payload->>''month_name''",
    "supply_date": "PARSE_DATE(payload->>''supply_date'')",
    "stockout_days": "TO_INT(payload->>''stockout_days'')",
    "expired_quantity": "TO_INT(payload->>''expired_quantity'')",
    "consumed_quantity": "TO_INT(payload->>''consumed_quantity'')",
    "received_quantity": "TO_INT(payload->>''received_quantity'')",
    "available_quantity": "TO_INT(payload->>''available_quantity'')",
    "redistributed_quantity": "TO_INT(payload->>''redistributed_quantity'')",
    "extra": "payload"
  }'::jsonb,
  '{}'::jsonb,
  'pilot-seed',
  true
);
```

### 4.2 Breedingsources (RPT\_0cb69bd7ed22c6d5)

```sql
INSERT INTO projection_config (
  id, source_repeat_uid, version, target_table, natural_key_expr, mappings, post_processing, author, active
) VALUES (
  gen_random_uuid(),
  'RPT_0cb69bd7ed22c6d5',
  1,
  'breeding_source_event',
  'payload->_id',
  '{
    "natural_key": "payload->_id",
    "payload_id": "payload->_id",
    "submission_uid": "submission_uid",
    "occurrence_index": "occurrence_index",
    "household_name": "payload->>''householdname''",
    "breeding_habitat_type_uid": "payload->>''breeding_habitat_type''",
    "breeding_habitat_type_label": "OPTION_LABEL(payload->>''breeding_habitat_type'', ''okB4N6tZJZO'')",
    "larval_stage_presence": "TO_BOOL(payload->>''Larval_stage_presence'')",
    "growth_regulator_grams": "TO_INT(payload->>''growth_regulator_grams'')",
    "breeding_habitat_description": "payload->>''breeding_habitat_description''",
    "breeding_habitats_count": "TO_INT(payload->>''breeding_habitats_count'')",
    "breeding_habitat_length_uid": "payload->>''breeding_habitat_length_meter''",
    "breeding_habitat_width_uid": "payload->>''breeding_habitat_width_meter''",
    "breeding_habitat_depth_uid": "payload->>''breeding_habitat_depth_meter''",
    "temphos_ml": "TO_NUM(payload->>''temphos_ml'')",
    "lsm_types": "payload->''lsm_type''",
    "extra": "payload"
  }'::jsonb,
  '{
    "explode": [
      {
        "field": "lsm_types",
        "target_table": "breeding_lsm_type",
        "parent_key": "natural_key",
        "natural_key_expr": "concat(natural_key, ''|'', lsm_value)"
      }
    ]
  }'::jsonb,
  'pilot-seed',
  true
);
```

> Note: `OPTION_RESOLVE` and `OPTION_LABEL` are pseudo-functions used in mapping expressions — the runner must implement helpers to resolve option codes to `option_uid` and labels (JSON). See evaluator section below.

---

# 5) Token/expression language (safe, minimal, implementable)

Implement a small, whitelisted expression evaluator in Java (no full dynamic code eval). Supported tokens/functions:

* `submission_uid` — literal column from raw\_repeat\_payload row (string).
* `payload` — the JSON payload (JSON object). Use sub-tokens:

    * `payload->_id`  → payload.get("\_id")
    * `payload->>'fieldName'` → payload.get("fieldName").asText()
    * `payload->'fieldName'` → payload.get("fieldName") as JsonNode (for arrays)
* `occurrence_index` — integer from raw row
* CAST / helpers:

    * `TO_INT(<expr>)` — parse int or null
    * `TO_NUM(<expr>)` — parse numeric
    * `TO_BOOL(<expr>)` — parse boolean values
    * `PARSE_DATE(<expr>)` — parse to `YYYY-MM-DD` or Date
    * `CONCAT(a,b,...)` — string concat
* Option helpers (runner must call option service):

    * `OPTION_RESOLVE(<option_code_expr>, <optionSetUid>)` → returns option\_uid (string)
    * `OPTION_LABEL(<option_code_expr>, <optionSetUid>)` → returns JSON label object (jsonb)
* For `explode` postProcessing:

    * `field` must be path to JSON array inside payload (e.g. `payload->'lsm_type'`).
    * runner must iterate array values and insert into `target_table` child rows.

**Security rule**: evaluator must limit function set to the above. No arbitrary SQL or script evaluation at runtime.

---

# 6) Upsert template for semantic tables (example for supply\_event)

Runner should compute a `Map<String,Object>` params per row, then perform a batched upsert like:

```sql
INSERT INTO supply_event (
  id, natural_key, submission_uid, template_uid, template_version_uid,
  occurrence_index, payload_id, amd_uid, amd_label, month_name_uid,
  supply_date, stockout_days, expired_quantity, consumed_quantity,
  received_quantity, available_quantity, redistributed_quantity, extra, payload_checksum, created_at, updated_at
)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, now(), now())
ON CONFLICT (natural_key) DO UPDATE
  SET amd_uid = EXCLUDED.amd_uid,
      amd_label = EXCLUDED.amd_label,
      month_name_uid = EXCLUDED.month_name_uid,
      supply_date = EXCLUDED.supply_date,
      stockout_days = EXCLUDED.stockout_days,
      expired_quantity = EXCLUDED.expired_quantity,
      consumed_quantity = EXCLUDED.consumed_quantity,
      received_quantity = EXCLUDED.received_quantity,
      available_quantity = EXCLUDED.available_quantity,
      redistributed_quantity = EXCLUDED.redistributed_quantity,
      extra = EXCLUDED.extra,
      payload_checksum = EXCLUDED.payload_checksum,
      projection_run_id = EXCLUDED.projection_run_id,
      updated_at = now();
```

For exploded child tables (e.g. `breeding_lsm_type`) use:

```sql
INSERT INTO breeding_lsm_type (id, breeding_event_id, lsm_type)
VALUES (?, ?, ?)
ON CONFLICT (breeding_event_id, lsm_type) DO NOTHING;
```

---

# 7) Option resolution service — minimal contract

Implement a small service with API (or in-process cache) used by the runner:

* `resolveOptionUid(optionSetUid, code) -> option_uid`
* `resolveOptionLabel(optionSetUid, code) -> jsonb label (e.g. {"en":"pond","ar":"..."})`

Cache optionSet -> (code -> uid,label) mapping in memory and refresh on template changes or via event hooks.

---

# 8) Analytics metadata registration rule (projection→metadata sync)

When a `projection_config` (version X) is activated (or `projection_run` completes first success), the runner should upsert rows into `analytics_entity` and `analytics_attribute`:

* `analytics_entity.name` = `target_table`
* `analytics_entity.table_name` = `target_table`
* `analytics_entity.entity_type` = `EVENT` (for now)
* For each mapping `target_column` create/ensure `analytics_attribute` row with `column_name`, `data_type` inferred from mapping (e.g. `PARSE_DATE` => DATE).

This makes the dynamic query layer safe to query these attributes.

---

# 9) Example: how the runner flow looks (high-level)

1. Start `projection_run` row (PENDING).
2. Query `raw_repeat_payload` pages for given `repeat_uid` (+ optional semantic\_path and template\_version filter).
3. For each page:

    * Evaluate mapping expressions for each payload → produce typed Map.
    * Batch upsert into `target_table` using prepared statements (JdbcTemplate or jOOQ).
    * For `post_processing.explode` entries, produce child rows and upsert (ON CONFLICT DO NOTHING).
4. Commit per-batch. Update `rows_processed`.
5. On success, set `projection_run.status=SUCCESS`, `ended_at`, rows\_processed. Link `projection_run_id` into updated rows if possible. On failure, record `error_msg` and status=FAILED.

Transactional note: do not rely on `@Transactional` for cross-method calls inside same class; ensure public transactional boundary on runner method or manage transactions programmatically.

---

# 10) Indexing & performance suggestions (pilot -> production)

* Index `raw_repeat_payload` on `(repeat_uid, template_version_uid, payload_id)` and on `submission_uid`.
* Semantic tables: index `natural_key` (unique), `template_version_uid`, `projection_run_id` and common filter columns (e.g., `breeding_habitat_type_uid`).
* For high volumes, partition `raw_repeat_payload` and `*_event` by `created_at` (monthly) or by `template_version_uid` if backfills often happen per-version.

---

# 11) Golden sample tests (CI)

* Keep a repo directory with `samples/<repeat_uid>/golden1.json` (raw payload) and an expected `expected_breeding_source_event_row.json`.
* CI runs extractor + projection in Testcontainers DB and asserts the upserted rows equal expected values (typed).
* Add tests for edge cases: missing optional fields, select-multi with empty array, invalid dates.

---

# 12) Short next-step checklist (practical order you can do right now)

1. Run the DDLs above (repeat\_definition, raw\_repeat\_payload, projection\_config, projection\_run, analytics tables, example semantic tables).
2. Insert the two `projection_config` seeds (supply & breedingsources).
3. Wire projection runner to read `projection_config` rows: implement expression evaluator and option resolver (small in-memory cache).
4. Run a projection run for the two repeats and check `projection_run`, `*_event` rows, and child exploded tables.
5. Implement `analytics_entity`/`analytics_attribute` upsert from projection\_config (basic inference of types).
6. Add golden sample test for both repeats and run in CI.
7. Add a preview endpoint: runner should support `dryRun=true` returning mapped rows but not writing to DB.

---

# 13) Closing / partner-mode notes

* This is intentionally conservative: defaults treat repeats as **events** and keep raw payloads immutable. Promotion to entities and fuzzy dedupe are separate flows (entity-resolution) and should be executed with manual review & audited merges.
* The `projection_config` is the single place to grow mapping functionality (type conversion, custom enrichers, post-processing). Keep it versioned; always create new versions rather than editing historic mappings in-place.
* If you want, I can now:

    * (A) produce a Java class skeleton for the expression evaluator + helper functions (OPTION\_RESOLVE, PARSE\_DATE, etc.), or
    * (B) produce Liquibase XML equivalents for the DDLs, or
    * (C) generate the preview endpoint shape and example HTTP request/response.

Tell me which one you want next and I’ll produce it ready-to-paste.
