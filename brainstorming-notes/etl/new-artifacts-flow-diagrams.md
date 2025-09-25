Nice — below are clear, production-grade lifecycle diagrams (Mermaid) for each artifact and the key processes that
produce/update them. I include a short narrative for each diagram that states ownership, lifecycle events, invariants
and important implementation notes you’ll need while wiring this into your services.

You can paste these Mermaid blocks into any Markdown preview that supports Mermaid (or into diagrams.net / Mermaid Live
Editor) to visualize the flows.

# TemplateElementInstance → CanonicalElement lifecycle

```mermaid
flowchart TD
A["Template ingestion triggered\n(new DATA_TEMPLATE_VERSION)"] --> B[Create TemplateElementInstance rows]
B --> C[Compute schema_fingerprint]
C --> D{Lookup CanonicalElement by fingerprint}
D -- found --> E[Link TemplateElementInstance.canonical_element_uid -> CanonicalElement]
D -- not found --> F["Create CanonicalElement(row)"]
F --> E
E --> G["Emit event: canonical-linked (log/notify)"]
G --> H[Mapping UI picks CanonicalElement as stable target]

subgraph Notes["f"]
direction TB
N1[Owner: template ingestion service]
N2[Invariant: TemplateElementInstance is immutable, canonical link added only]
N3["Fingerprint: SHA256(control_type|isMultiSelect|cardinality|parent_repeat_path|option_set|constraints)"]
end
```

**Narrative**

* **Owner**: Template ingestion service (the code that runs when a `DATA_TEMPLATE_VERSION` is persisted).
* **When produced**: `TemplateElementInstance` rows are created on template version publish; `schema_fingerprint`
  computed immediately; `CanonicalElement` may be created if fingerprint unseen.
* **Invariants**: `TemplateElementInstance` is a per-template-version immutable record (for provenance).
  `CanonicalElement.schema_fingerprint` is unique. `TemplateElementInstance.canonical_element_uid` links to canonical.
* **Idempotency**: lookup-or-create must be atomic (`INSERT ... ON CONFLICT` or transaction with `SELECT FOR UPDATE`) to
  avoid duplicate canonicals.

---

# Structural ETL → ExtractionManifest → ElementDataValue & RepeatInstance

```mermaid
flowchart LR
    S["Client submits JSON → DATA_SUBMISSION"] --> ETL_START[Structural ETL Runner]
    ETL_START --> EXTRACT[Extract values from JSON\nfor each TemplateElementInstance]
    EXTRACT --> REPEAT_SUMMARY[Create RepeatInstance rows]
    EXTRACT --> EVAL_TYPES["Normalize simple types\n(date/number/trim)"]
    EVAL_TYPES --> INSERT_EDV[Insert ElementDataValue rows]
    INSERT_EDV --> BUILD_MANIFEST["Build ExtractionManifest DTO\n(collect snippets, repeat_summaries)"]
    BUILD_MANIFEST --> PERSIST_MANIFEST[Persist extraction_manifest JSONB]
    PERSIST_MANIFEST --> LINK_EDV[Update ElementDataValue.manifest_uid and canonical_element_uid]
    LINK_EDV --> COMPLETE[ETL Run Complete - manifest_uid returned]

    subgraph Notes2["f"]
        direction TB
        M1[Owner: Structural ETL service]
        M2[ElementDataValue typed columns for fast batch reads]
        M3[Manifest stored immutable, new manifest row for re-extraction runs]
        M4[ExtractionConfidence heuristics computed at extraction time]
    end
```

**Narrative**

* **Owner**: Structural ETL (extraction) service.
* **When produced**: For each `DATA_SUBMISSION` processed, ETL produces `RepeatInstance` entries, `ElementDataValue`
  typed rows, and a single `ExtractionManifest` JSONB row (timestamped).
* **Invariants**:

    * Each `ElementDataValue` links to `template_element_uid` and (after link) `canonical_element_uid`.
    * Manifest contains `repeat_summaries` with `repeat_instance_id` values that correspond to
      `ElementDataValue.repeat_instance_id`.
    * `value_normalized` and typed value columns obey type consistency (if `is_null` true then typed values null).
* **Implementation note**: Persist manifest first (or within same transaction) and write `manifest_uid` into EDV rows;
  for large EDV batches write in chunks.

---

# CanonicalElement / TemplateElement change detection & mapping review flow

```mermaid
flowchart TD
    TPL_CHANGE[New TemplateVersion ingested] --> TE_CREATE["Create TemplateElementInstance(s) + compute fingerprint"]
    TE_CREATE --> FP_COMPARE{Fingerprint matches existing\nCanonicalElement?}
    FP_COMPARE -- yes --> LINK1[Link instances to CanonicalElement]
    FP_COMPARE -- no --> CREATE_CANON[Create new CanonicalElement]
    CREATE_CANON --> NOTIFY[Emit 'new canonical' event]
    NOTIFY --> MAP_REVIEW[Mapping authors notified to review affected mappings]
    MAP_REVIEW --> APPROVE[Author approves or updates mappings]
    APPROVE --> READY[Mappings ready for next ETL runs]

    subgraph Notes3["d"]
        direction TB
        P1[Why: prevents semantic drift and avoids reauthoring mappings]
        P2[Tradeoff: requires occasional mapping review when new canonicals appear]
    end
```

**Narrative**

* When templates change, fingerprints detect semantic changes (e.g., moved into/out of repeat, multi-select changed,
  control type changed).
* New canonicals prompt mapping authors to verify mappings (or mapping UI can auto-suggest the prior canonical if
  semantic match near).

---

# Mapping runner & downstream usage (preview & batch apply)

```mermaid
flowchart LR
    Author[Mapping Author] --> UI[Mapping Authoring UI]
    UI --> Save["Save MappingRule (draft) -> mapping_rule table"]
    Save --> Publish[Publish mapping_version]
    Publish --> MR[Mapping Runner Service: load published mapping_version]

    subgraph Preview["Preview flow"]
        MR --> QueryManifest["Query sample extraction_manifest(s)"]
        QueryManifest --> ExecuteAST[Evaluate Transform AST against manifests]
        ExecuteAST --> ReturnPreview[Return proposed UES rows to UI]
    end

    subgraph Batch["Batch apply flow"]
        MR --> QueryManifests["Query extraction_manifest rows (batch/range)"]
        QueryManifests --> ExecuteAST2[Evaluate AST per manifest / per repeat]
        ExecuteAST2 --> UpsertUES[Upsert into Unified Entity Store / Relationship table]
        UpsertUES --> Materialize[Optionally materialize AttributeIndex or materialized views]
    end
```

**Narrative**

* Preview uses `extraction_manifest` DTOs so authors get fast dry-runs with provenance. No raw JSONB scan needed.
* Batch apply consumes `extraction_manifest` + `ElementDataValue` as input (for historical loads or incremental).
* Mapping Runner must record `mapping_version_uid` in `attribute_provenance` when writing UES rows.

---

# Backfill / Reprocessing flow (mapping change impact)

```mermaid
flowchart TD
    MappingChange[Mapping version published/updated] --> Decision{Requires backfill?}
    Decision -- No --> Done["No backfill required (stateless mapping)"]
    Decision -- Yes --> BackfillJob[Backfill Orchestrator]
    BackfillJob --> SelectManifests["Select manifests to reprocess (range / list)"]
    SelectManifests --> DryRun[Run preview on selected manifests -> diff report]
    DryRun --> Review[Operator reviews diff]
    Review -- approve --> ExecuteBackfill[Apply mapping to manifests -> UES upserts]
    ExecuteBackfill --> Audit[Write backfill audit log & metrics]
```

**Narrative**

* Mapping changes that affect DDE semantics should trigger a backfill with dry-run reports. Use `manifest_uid` lists to
  scope backfills and run incremental batches.

---

# Artifact state & lifecycle quick-reference table

| Artifact                |                         Owned By | Created When                 | Updated When                         |                  Immutable? | Key Invariants                                          |
|-------------------------|---------------------------------:|------------------------------|--------------------------------------|----------------------------:|---------------------------------------------------------|
| TemplateElementInstance |               Template ingestion | new template version         | never (except set canonical link)    | Yes (except canonical link) | `uid`, `template_version_uid`, `name_path` stable       |
| CanonicalElement        | Template ingestion/canonicalizer | first time fingerprint seen  | metadata (notes, candidates)         |                      Mostly | `schema_fingerprint` unique                             |
| schema\_fingerprint     |               Template ingestion | computed on create           | recompute on reingest only           |                         N/A | Deterministic from canonicalized fields                 |
| ExtractionManifest      |                   Structural ETL | every ETL run per submission | new manifest on re-extract           |                         Yes | `elements[]` reference existing TemplateElementInstance |
| RepeatInstance          |                   Structural ETL | per extraction run           | new on re-extract                    |          Yes (per manifest) | ordinal\_index >=0; parent links valid                  |
| ElementDataValue        |                   Structural ETL | per extracted value          | new on re-extract or additional runs |               No (new rows) | typed value columns consistent; linked to manifest      |
| ElementStats            |                        Stats job | periodic                     | periodic                             |      Overwritten per window | sample\_count > 0 when present                          |
| CodeList                |                            Admin | on code-list creation        | on code-list update (new version)    |                   Versioned | version immutability                                    |

---

# Important invariants & engineering rules (summary)

* **Separation of concerns**: Extraction = structural parsing + deterministic normalization + provenance. No business
  mapping, identity merging, or dedup here.
* **Provenance everywhere**: Every EDV and manifest includes `submission_uid`, `template_version_uid`, and
  `template_element_uid` (and `manifest_uid`) so higher layers can always trace to original source.
* **Canonicalization**: Fingerprint formula must be identical between the DB backfill SQL and the Java compute function.
  Keep unit tests to enforce parity.
* **Idempotency**: Manifests are immutable rows; re-extraction produces a new manifest row. TemplateElement
  canonicalization must be idempotent (lookup-or-create).
* **Repeat handling**: RepeatInstance ids are stable per manifest; mapping runner uses `repeat_instance_id` to apply
  repeat-scoped rules.
* **Performance**: For bulk loads use COPY for EDV inserts; GLUE JPA for metadata only.

---

If you want I can now:

* produce PNG/SVG exports of these Mermaid diagrams (if your tool requires), or
* produce a single combined Mermaid diagram that shows *end-to-end* (template ingestion → extraction → mapping → UES) in
  one view (might be very wide), or
* convert the diagrams into developer-facing sequence diagrams (Mermaid sequence) for each process for implementation
  tasks.

Which one next?
