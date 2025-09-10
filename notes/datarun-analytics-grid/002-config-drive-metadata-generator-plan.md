# Design goals for the Analytics Metadata Generator

1. **Non-invasive / pluggable** — generate metadata and register MVs without forcing schema changes in core tables.
2. **Idempotent & repeatable** — runs may be re-executed safely; generator produces stable identifiers and names.
3. **Schema-agnostic but auditable** — supports multiple templates and versions, records provenance for each metadata artifact.
4. **Extensible** — supports simple defaults now and richer config-driven behavior later (grain components, custom mappings).
5. **Minimal runtime coupling** — analytics queries / UI use metadata service, not ad-hoc naming heuristics.
6. **Backwards-compatible** — handle existing `semantic_path`/`semantic_repeat_path` and ETL outputs.

---

# Core analytics metadata model (conceptual entities)

These are the metadata primitives the generator should maintain. Keep them small, explicit and versioned.

1. **AnalyticsEntity**

    * Represents a logical analytical grain (table/MV/entity). Examples: `submission` (root grain), `household.member` (repeat grain), `inventory.line`.
    * Key fields: `entity_uid` (analytics), `template_uid`, `template_version_uid`, `semantic_repeat_path` (if repeat), `entity_kind` (root|repeat|derived), `mv_name` (registered MV), `grain_domain_id_components` (optional JSONB), `created_at`, `version`.

2. **AnalyticsAttribute**

    * Column-level metadata mapping a DataElement / ElementTemplateConfig to an attribute in the entity/MV.
    * Key fields: `attribute_uid`, `entity_uid` (FK), `element_template_config_uid`, `data_element_uid`, `attribute_name` (column alias), `value_type`, `is_dimension`, `is_measure`, `is_reference` (ref table), `option_set_uid` (if any), `display_label`, `map_expression` (how to compute/from which source column), `created_at`.

3. **AnalyticsRelationship** (optional but valuable)

    * Declares joins between entities (e.g., repeat entity -> submission root). Useful for query engine planning.
    * Key: `from_entity`, `to_entity`, `join_expr`, `cardinality`.

4. **MV Registry / Deployment**

    * Tracks generated MV names, their owning entity, `ddl`, `last_refresh`, `status` (deployed / stale / failed), and `mv_version`.
    * Avoids name collisions and records sanitization steps.

5. **GrainConfig** (optional config artifact)

    * Declares how to compute `grain_domain_id` and which canonical components to use for that entity. Config stored as ordered list of `component_paths` (references to canonical uids). Example: `["org_unit_uid","submission_external_id","household_id","member_national_id"]`.

6. **Metadata Change Log / Provenance**

    * Records generator runs, input template_version uid, diffs of generated artifacts (attributes added/removed), who triggered it, and migration/backfill suggestions.

(These are logical table types that the generator manages; actual DDL can be decided later.)

---

# Key mappings and naming conventions (stable, deterministic rules)

**Identifiers**

* `analytics_entity_uid`: `an_{template_uid}_{semantic_repeat_path_hash}` (shorten/encode when too long). Use deterministic hashing for long semantic paths.
* `analytics_attribute_uid`: `aa_{element_template_config_uid}` or `aa_{entity_uid}_{de_uid}`.

**MV names**

* Use a sanitized deterministic name: `mv_{template_uid}_v{version_no}_{semantic_normalized}`.

    * `semantic_normalized`: lowercased, non-alphanumeric replaced with `_`, consecutive `_` collapsed, truncated to safe length (e.g., 64 chars).
    * Persist mapping in `mv_registry` to avoid collisions and to allow changes later without breaking consumers.

**Column names (attribute_name)**

* Prefer readable column aliases built from canonical `de_uid` and friendly suffix: `de_{de_uid}`, or `de_{de_uid}_num/text/ts` when needed (e.g., `de_age_num`).
* If UI labels required, keep `display_label` in metadata and separate from physical column names.

**Grain domain id**

* `grain_domain_id` creation strategy: deterministic hash (SHA256/UUIDv5) of ordered canonical UID components. Store both the hash and the ordered components array in JSONB for traceability. Do not use timestamps in the deterministic key. (Generator must optionally populate `grain_domain_id_components` in `AnalyticsEntity` metadata.)

**Versioning**

* Every generated entity and attribute must carry `metadata_version` and `generated_at`. When input template version changes, generator records a new metadata version; MVs may be regenerated or altered accordingly.

---

# Analytics Metadata Generator: responsibilities & components

Design the generator as a modular pipeline composed of these components:

1. **Discovery / Input Scanner**

    * Reads published `DataTemplateVersion` and associated `ElementTemplateConfig` rows (including `semantic_repeat_path`, `is_dimension`, `is_measure`, `option_set_uid`, `ancestor_repeat_semantic_path`).&#x20;

2. **Normalizer**

    * Normalize semantic paths, names, UIDs. Produce sanitized identifiers, canonical value types, and resolved references (option sets, ref types).

3. **Planner / Mapper**

    * Decide what AnalyticsEntities to produce: which `semantic_repeat_path`s become entities/MVs; which attributes map to which entity.
    * Apply `grain_config` if present to compute `grain_domain_id_components`.

4. **NameGenerator**

    * Deterministically produce `mv_name`, `entity_uid`, `attribute_name`. Make sure to use the MV Registry to check collisions.

5. **Metadata Upserter / Registry**

    * Persist `AnalyticsEntity`, `AnalyticsAttribute`, `AnalyticsRelationship`, `MVRegistry`, `GrainConfig` entries. This write is idempotent and versioned.

6. **DDL/Deployment Planner (optional plugin)**

    * Generate the MV DDL (or update plan) for the MV manager to execute. Keep this optional—some teams prefer manual review before applying DDL.

7. **Validator & Sanity-checker**

    * Validate: no duplicate attribute_names, required dimensions measured properly, valueType compatibility with DataElement valueType, reference keys resolved, and that unique constraints for ETL idempotence are present (e.g., `grain_domain_id` exists where needed).

8. **Change Detector / Migration Advisor**

    * Compare new metadata with prior versions, produce a changelog/diff, and recommend whether to re-create MV, alter it, or produce a compatibility layer.

9. **Auditor / Provenance Recorder**

    * Store run metadata (source template_version, run_ts, author), diffs, and backfill instructions.

10. **API/Service Layer**

    * Expose metadata via the Analytics Metadata Service (Layer 8 in your diagram) for the Dynamic Query Engine and UIs.

---

# Generation workflow (sequence for a template publish event)

1. **Event**: `NewTemplateVersionPublishedEvent` (already present in your flow).&#x20;
2. **Discovery**: generator reads `ElementTemplateConfig` rows for the new version.
3. **Normalization**: normalize names, compute `semantic_repeat_path` normal form.
4. **Plan**: decide which entities to create:

    * always create a root `submission` entity for the template version;
    * create a repeat entity for each distinct `semantic_repeat_path` found (use `SELECT DISTINCT semantic_repeat_path ...`).&#x20;
5. **Map attributes**: group `ElementTemplateConfig` rows by `semantic_repeat_path` into `AnalyticsAttribute` mapping (respect `is_dimension/is_measure` flags).
6. **Name & register**: generate deterministic `entity_uid`, `mv_name`, `attribute_name` and upsert into metadata tables. Create `mv_registry` entry.
7. **Validate**: run sanity checks (valueType conflicts, missing ref types).
8. **(Optional) DDL plan**: produce MV DDL and store in registry for operator review.
9. **Notify**: emit an event `AnalyticsMetadataGenerated(template_version_uid, metadata_version)`.

Idempotence: if the same publish event is re-processed, metadata upsert is a no-op or produces the same deterministic artifacts.

---

# Four practical design choices (patterns) — pick one as the main strategy

### Option A — **Conservative (Template-local, simple)** — *fast to implement*

* **What:** One AnalyticsEntity per `semantic_repeat_path` per template_version. Attributes mapped directly from `ElementTemplateConfig`. Minimal config. `grain_domain_id` optional.
* **Pros:** Simple, deterministic, minimal new config, maps directly to current ETL. Fast to deploy.
* **Cons:** Cross-template joins and unifying same domain concept across multiple templates is harder. Less flexible for domain-based grain merging.

### Option B — **Config-driven (Recommended)** — *extensible & future-proof*

* **What:** Like Option A but supports explicit `GrainConfig` and `AnalyticsEntity` aliases to express domain equivalences (e.g., map `household.members` in template A and `family.members` in template B to the same AnalyticsEntity). Generator consults `grain_config` for `grain_domain_id_components`.
* **Pros:** Enables cross-template identity, de-duplication, and migration, while still deterministic. Metadata-driven, so minimal code changes later.
* **Cons:** Requires some admin UI/authoring of `grain_config` entries (but defaults apply).

### Option C — **Schema-first (strong typing / canonical model)** — *strict & governed*

* **What:** An explicit canonical analytics schema is managed (AnalyticsEntity definitions authored by data modelers). Generator maps templates into the canonical schema using `ElementTemplateMap` mappings (existing table in doc). The generator can auto-suggest mappings but requires human review for final registration.
* **Pros:** Best long-term analytics quality, consistent column names across templates, easier BI.
* **Cons:** Heavy governance overhead and slower onboarding of new templates.

### Option D — **Hybrid (auto + manual review)** — *practical compromise*

* **What:** Generator auto-creates metadata (Option A) and produces auto-mapping suggestions into canonical entities (Option C). A light admin review step is required to confirm mapping before MVs are deployed to production analytics schema.
* **Pros:** Balance speed and long-term quality; human-in-the-loop avoids bad automated merges.
* **Cons:** Requires light process and tooling for review.

**Recommendation:** Start with **Option B (Config-driven)** or **Option D (Hybrid)**. That gives immediate automation but keeps the door open to canonical consolidation without large refactors.

---

# Important operational concerns & constraints

1. **Name collisions & sanitization**

    * Always maintain `mv_registry` with the canonical mapping from `entity_uid` -> `mv_name` and store original `semantic_repeat_path`. If a sanitized name collides, append a deterministic short hash.

2. **Length limits**

    * Keep sanitized MV names < 63 chars (Postgres identifier limit), use registry to map long semantic paths to safe MV names.

3. **Backfill and migrations**

    * When metadata changes (e.g., attribute removed/renamed), generator should: (a) produce a diff; (b) flag if ALTER MV vs RECREATE suggested; (c) create backfill job descriptors for transformation.

4. **Idempotent ETL & `grain_domain_id`**

    * Use `grain_domain_id` (deterministically derived from `GrainConfig` components) as the primary upsert key for MV incremental refreshes. Don’t include timestamps in the hash. Store components JSONB for audit.

5. **Indexing & partitioning guidance**

    * Generator can add index recommendations in metadata (e.g., index on `grain_domain_id`, `submission_completed_at`) so MV manager can apply them.

6. **Governance & validation policies**

    * Enforce rules: do not map a DataElement with used data to a different valueType; if DataElement must change, a new DataElement/version must be created and mapping updated.

7. **Audit trail**

    * Keep full provenance of generator runs and metadata versions. Allow queries such as “which metadata version generated MV X, and which template elements were mapped?”

---

# Example metadata outputs the generator should persist (conceptual)

* `analytics_entity` record for `templateA_v3_household_members` with `entity_uid`, `mv_name`, `semantic_repeat_path`, `grain_config` reference.
* `analytics_attribute` rows mapping each `element_template_config` to a column alias and indicating dimension/measure status.
* `mv_registry` entry with `mv_name`, `ddl_snippet`, `status`.
* `run_log` entry with `metadata_version`, `diff_summary`, `author`.

---

# How this design keeps future changes minimal

* **Separation**: metadata registry isolates query layer from raw ETL and template structure — renames or layout changes only require regenerating metadata, not reworking consumers.
* **Deterministic naming**: stable, hash-based naming reduces breakages when semantic paths are long or changed cosmetically.
* **Config-driven grain identity**: allowing `grain_config` to be authored after the fact means domain consolidation can be done gradually without reprocessing old ETL results immediately.
* **Optional DDL automation**: the generator can produce but not auto-apply DDL; operators can review, reducing risk.

---

# Short prioritized checklist to implement the generator (high-level, no code)

1. Create metadata tables (AnalyticsEntity, AnalyticsAttribute, AnalyticsRelationship, MVRegistry, GrainConfig, RunLog).
2. Implement generator pipeline modules (Discovery → Normalizer → Planner → NameGen → Upsert → Validator → RunLog).
3. Start with conservative default mapping (Option A) with hooks for `GrainConfig`.
4. Build simple UI/admin for reviewing generated metadata and authoring `GrainConfig`.
5. Implement `mv_registry` and safe MV name sanitizer.
6. Add a validation step that rejects valueType mismatches and recommends DataElement versioning when needed.
7. Implement provenance logging and a simple diff viewer for metadata changes.

---

# Final recommendation (one-liner)

Implement a **Config-driven, idempotent Analytics Metadata Generator** that auto-discovers entities from `semantic_repeat_path`, maps ElementTemplateConfig rows to attributes, stores deterministic names in an `mv_registry`, and supports explicit `GrainConfig` for domain-level `grain_domain_id` — start with automated defaults but require lightweight human review (hybrid flow) for production MV deployment.
