# Form Template & Submission V2 Contract — Source of Truth

> **Status:** DRAFT — under review  
> **Owner:** Hamza Assada  
> **Last updated:** 2026-03-05  
> **Audience:** Backend, Frontend, Mobile, QA

---

## Table of Contents

1. [Guiding Question](#1-guiding-question)
2. [Current State (V1) — What We Have](#2-current-state-v1--what-we-have)
3. [Design Principles & Non-Negotiables](#3-design-principles--non-negotiables)
4. [V2 Submission Contract](#4-v2-submission-contract)
5. [V2 Template Tree Contract](#5-v2-template-tree-contract)
6. [Behavior Engine (AST \ Logic Broker)](#6-behavior-engine-ast--logic-broker)
7. [Anti-Corruption Layer (ACL)](#7-anti-corruption-layer-acl)
8. [Migration Strategy — Phases](#8-migration-strategy--phases)
9. [Open Questions & Decision Log](#9-open-questions--decision-log)
10. [Appendix — Reference Files](#10-appendix--reference-files)

---

## 1. Guiding Question

> How do we build a **web frontend API** using a **future-proof contract**, while keeping the mobile app running unchanged, so that:
>
> - The mobile app migrates **gradually** and **non-disruptively**.
> - Any **new UI** never knows about legacy shapes — it only talks to V2.
> - The **translating layer** that bridges legacy ↔ V2 is hidden behind an internal boundary, not exposed to any outside consumer.
> - The design is **resilient to future evolution**, so we don't build another brittle contract.

---

## 2. Current State (V1) — What We Have

### 2.1 Domain Entities (JPA)

| Entity | Table | Role |
|---|---|---|
| [`DataTemplate`](..\..\src\main\java\org\nmcpye\datarun\jpa\datatemplate\DataTemplate.java) | `data_template` | The form "type" — has a `uid`, `name`, and tracks the latest version number + uid. |
| [`TemplateVersion`](..\..\src\main\java\org\nmcpye\datarun\jpa\datatemplate\TemplateVersion.java) | `data_template_version` | An immutable snapshot of a template at a version. Stores `fields` (JSONB `List<FormDataElementConf>`) and `sections` (JSONB `List<FormSectionConf>`), plus `options`. |
| [`TemplateElement`](..\..\src\main\java\org\nmcpye\datarun\jpa\datatemplate\TemplateElement.java) | `template_element` | The **Canonical Element Registry** — one row per field per version. Carries `canonicalPath`, `canonicalElementId`, `jsonDataPath`, `dataType`, `semanticType`. Sections are **NOT** stored here; they are visual-only and live in the `TemplateVersion.sections` JSONB. |
| [`DataSubmission`](..\..\src\main\java\org\nmcpye\datarun\jpa\datasubmission\DataSubmission.java) | `data_submission` | A single submission instance. Stores `formData` as an opaque JSONB blob, plus metadata (`form`, `formVersion`, `version`, `team`, `orgUnit`, `assignment`, etc.). Uses optimistic locking (`lockVersion`). |

### 2.2 Template Version Snapshot POJOs (stored inside `TemplateVersion` JSONB)

| POJO | Key properties |
|---|---|
| [`FormSectionConf`](..\..\src\main\java\org\nmcpye\datarun\datatemplateelement\FormSectionConf.java) | `name`, `path`, `order`, `label`, `rules[]`, `repeatable`, `categoryId`. **Note:** `getId()` returns `getName()`. |
| [`FormDataElementConf`](..\..\src\main\java\org\nmcpye\datarun\datatemplateelement\FormDataElementConf.java) | `id` (unique element uid), `parent` (section name), `name`, `path`, `code`, `type`, `mandatory`, `optionSet`, `rules[]`, `validationRule`, etc. Has legacy compatibility shims for `constraint`, `constraintMessage`, `mainField`. |
| [`AbstractElement`](..\..\src\main\java\org\nmcpye\datarun\datatemplateelement\AbstractElement.java) | Shared base: `parent`, `name`, `path`, `code`, `description`, `order`, `label`, `rules`, `properties`. |

### 2.3 V1 REST Endpoints

| Endpoint | Path | Purpose |
|---|---|---|
| [FormTemplateMergeResource](..\..\src\main\java\org\nmcpye\datarun\web\rest\v1\formtemplate\FormTemplateMergeResource.java) | `/api/v1/dataFormTemplates` | GET list/by-id, POST/PUT template+version as a merged DTO (`DataTemplateInstanceDto`). |
| [DataSubmissionResource](..\..\src\main\java\org\nmcpye\datarun\web\rest\v1\datasubmission\DataSubmissionResource.java) | `/api/v1/dataSubmission` | GET/POST/DELETE submissions. Supports `?flatten=true`. Pre-processes by generating missing repeat row `_id`s via `MigrationRepeatIdGenerator`. |

### 2.4 V1 Data Shapes (Actual Samples)

**V1 Submission `formData`:**

```json
{
  "main": { "visitdate": "2025-09-27", "NotificationNumber": 2 },
  "patients": { "age": "2017-09-26", "gender": "MALE", "PatientName": "..." },
  "medicines": [
    { "_id": "01K693...", "_index": 1, "amd": "act40_tape", "prescribed_quantity": 1 }
  ],
  "referrals": { "cm_measures": "treatment" }
}
```

**V1 Template Version `sections`:**

```json
[
  { "name": "main", "repeatable": false, "order": 100 },
  { "name": "patients", "repeatable": false, "order": 200 },
  { "name": "medicines", "repeatable": true, "order": 300 },
  { "name": "referrals", "repeatable": false, "order": 400 }
]
```

**V1 Template Version `fields`:** Each field has `parent` (section name), `name`, `path` (`<parent>.<elementId>`), `code`, `id`, `type`, `rules[]`.

### 2.5 What's Wrong with V1 (why we're doing V2)

| Problem | Detail |
|---|---|
| **UI structure leaks into data** | Section names (`main`, `patients`) are embedded as JSON keys in `formData`. Moving a field to a different tab changes the data contract. |
| **Arrays for repeaters** | `medicines` is a JSON `[]`. Array merging in offline sync is non-deterministic; requires scanning for `_id` matches. UPSERT is impossible without full-array comparison. |
| **Rules reference UI paths** | Rule expressions like `#{gender}` rely on the field `name` being resolvable in the current section's scope. There's no formal namespace for intra-row vs. cross-collection vs. global aggregation. |
| **No canonical identity in data** | The submission `formData` uses `name`/`code` keys that can collide across sections. There's no guaranteed stable identity for analytics over version changes. |

---

## 3. Design Principles & Non-Negotiables

These are the invariants that **must hold** across all V2 contracts. Any future proposal that violates these must be flagged and re-evaluated.

### P1. Data Shape Is Independent of UI Layout

> Moving a field between UI sections/tabs **MUST NOT** change any API payload key or database column.

**Consequence:** Section names (`main`, `patients`, etc.) do not appear as keys in V2 submission data.

### P2. Repeater Data Uses Identity-Keyed Maps, Not Arrays

> Every repeating collection **MUST** be an `Object<row_id, row_data>`, never a JSON `[]`.

**Consequence:** Offline sync becomes UPSERT-by-key. No array merge conflicts. `O(1)` row access.

### P3. Stable Canonical Identity Across Versions

> Each field has a `canonicalElementId` and `canonicalPath` (from `TemplateElement`) that survive section reorganizations. Only repeater-boundary changes break canonical identity.

### P4. Rules Express Intent, Not Data Shape

> Rule expressions use **logical namespaces** (`_row`, `$rel`, `$global`) that are resolved at evaluation time by a frontend state manager. The rule JSON itself doesn't embed specific data paths.

### P5. Anti-Corruption Layer Hides Legacy

> A backend **ACL service** (in the DDD sense) translates between V1-shape and the normalized internal model. No external consumer (V2 web, analytics, future mobile) ever sees V1 shapes — they only see V2.

### P6. Backward Compatibility via Dual-Write, Not Feature Flags

> V1 endpoints remain operational and unchanged. V2 endpoints run alongside. Both read/write through the **same canonical internal model** (normalized submission store). The ACL translates on ingest and on egress, in both directions.

---

## 4. V2 Submission Contract

### 4.1 Normalized Submission Shape

```json
{
  "submission_uid": "z3Ye07TDj7a",
  "template_uid": "ck2pHW93sk2",
  "version_number": 2,

  "values": {
    "visitdate": "2025-09-27",
    "NotificationNumber": 2,
    "emergency_team_type": "malaria_unit",
    "age": "2017-09-26T21:00:00.000Z",
    "gender": "MALE",
    "ispregnant": false,
    "PatientName": "محمد فيصل كامل مشعل",
    "serialNumber": 4,
    "diagnosed_disease_type": "malaria",
    "cm_measures": "treatment"
  },

  "collections": {
    "medicines": {
      "01K693VTPPWQR1M23AN06B6N0D": {
        "_index": 1,
        "amd": "act40_tape",
        "druguom": "tablet",
        "prescribeddrug": "PMQ",
        "prescribed_quantity": 1
      }
    }
  }
}
```

### 4.2 Rules for `values`

| Rule | Rationale |
|---|---|
| **Flat map** — no nesting, no section wrappers. | P1: data is independent of layout. |
| **Keys are field `name`** (stable within a version). For cross-version analytics, use `canonicalElementId`. | The `name` is the human-readable binding. |
| Multi-select values remain arrays (`["option1", "option2"]`) — the value itself is an array, not the field structure. | This is value-level typing, not structural nesting. |

### 4.3 Rules for `collections`

| Rule | Rationale |
|---|---|
| **Top-level key** is the repeater's section `name` (e.g., `"medicines"`). | This is the collection's namespace — stable as long as the repeat itself exists. |
| **Row key** is the row's `_id` — a client-generated ULID/UUID. | P2: identity-keyed map. |
| **Row object** contains only the field `name` → value pairs for that row. No `_parentId` or `_submissionUid` at this level unless nested (see §4.4). | Keep the simple case simple. |
| `_index` is **optional, client-hint-only** — never used for identity or merge logic. | Sort order for display; the server ignores it for merge. |

### 4.4 Nested Repeaters (Repeat-in-Repeat)

All collections are **flattened at the root** of `collections`. Parent-child linkage uses `_parent_id`.

```json
{
  "values": { "interviewer_name": "Dr. Smith" },
  "collections": {
    "households": {
      "hh_001": { "address": "123 Main St", "roof_type": "Tin" },
      "hh_002": { "address": "456 Side St" }
    },
    "family_members": {
      "mem_001": { "_parent_id": "hh_001", "name": "John", "age": 45 },
      "mem_002": { "_parent_id": "hh_001", "name": "Jane", "age": 42 },
      "mem_003": { "_parent_id": "hh_002", "name": "Bob", "age": 20 }
    }
  }
}
```

| Rule | Rationale |
|---|---|
| Child rows include `_parent_id` referencing the parent row's `_id`. | Relational normalization. O(1) lookup. |
| Collections are **never** nested inside other collections' row objects. | P2 + keeps JSON depth ≤ 3 always. Directly maps to relational tables. |
| A `vaccinations` repeater inside `family_members` would just be another root-level key in `collections`, with `_parent_id` pointing to `mem_*` IDs. | Infinite nesting depth, zero JSON depth increase. |

### 4.5 Sections Inside Repeaters

Sections (visual groupings) **do not appear** in the data payload. If a "Dosage Details" section wraps `prescribed_quantity` inside the `medicines` repeater, the data shape is unchanged:

```json
"medicines": {
  "row_123": {
    "amd": "Aspirin",
    "prescribed_quantity": 1
  }
}
```

> The section is purely a UI rendering concern. The Template Tree (§5) carries section hierarchy for rendering; the data layer ignores it.

---

## 5. V2 Template Tree Contract

### 5.1 Goal

The web frontend needs a **nested tree** of UI nodes to render the form. The backend builds this tree from the legacy flat `sections` + `fields` arrays stored in `TemplateVersion`, using an **O(N) HashMap Registry** algorithm in memory.

### 5.2 Node Schema

Every node in the tree has the following shape:

```json
{
  "node_id": "pHjMRAL4glF",
  "type": "SelectOne",
  "binding": "amd",
  "label": { "en": "amd", "ar": "الصنف" },
  "mandatory": true,
  "option_set": "sYiS5D2qeG8",
  "rules": [],
  "validation": null,
  "children": []
}
```

| Property | Source | Meaning |
|---|---|---|
| `node_id` | `FormDataElementConf.id` or `FormSectionConf.name` | Unique within the tree. |
| `type` | For fields: `FormDataElementConf.type`. For sections: `"section"`. For repeaters: `"repeater"`. | Determines the UI widget or container. |
| `binding` | For fields: `FormDataElementConf.name` (the key used in `values` or collection-row). For repeaters: section `name` (the key used in `collections`). | The data-binding key — links tree node → submission data. |
| `label` | `AbstractElement.label` | Localized display label. |
| `children` | Computed by the HashMap algorithm. | Ordered list of child nodes. |
| `rules` | `AbstractElement.rules` | Behavior rules (see §6) — carried as-is from legacy for now. |

### 5.3 HashMap Registry Algorithm (Template Transformer)

```
Input:  TemplateVersion.sections[] + TemplateVersion.fields[]
Output: V2 Tree (root node with nested children)

1. Create rootNode = { node_id: "root", children: [] }
2. Create registry = HashMap<String, Node>

3. FOR EACH section in sections (sorted by order):
     node = mapSectionToNode(section)
     registry[section.name] = node
     rootNode.children.add(node)

4. FOR EACH field in fields (sorted by order):
     node = mapFieldToNode(field)
     parentNode = registry[field.parent]
     parentNode.children.add(node)

5. RETURN rootNode
```

**Complexity:** O(N) time, O(N) space. No recursive DB queries. Entirely in-memory.

### 5.4 Example Transformation

**Legacy Input:**
```json
{
  "sections": [{ "name": "medicines", "repeatable": true, "order": 300 }],
  "fields": [{ "id": "pHjMRAL4glF", "parent": "medicines", "code": "amd", "type": "SelectOne" }]
}
```

**V2 Tree Output:**
```json
{
  "node_id": "root",
  "children": [
    {
      "node_id": "medicines",
      "type": "repeater",
      "binding": "medicines",
      "children": [
        {
          "node_id": "pHjMRAL4glF",
          "type": "SelectOne",
          "binding": "amd"
        }
      ]
    }
  ]
}
```

---

## 6. Behavior Engine (AST / Logic Broker)

### 6.1 The Problem

V1 rules reference flat field names scoped to their section (e.g., `#{gender} == 'FEMALE'`). In the normalized V2 data model, collections are identity-keyed maps. The logic engine needs to know:
- Which **specific row** it's evaluating (intra-row).
- Which **child rows** belong to the current parent (relational).
- The **entire collection** for global aggregations.

### 6.2 Namespace Resolvers

We introduce three **contextual namespaces** that the frontend state manager resolves before passing data into the rule evaluator:

#### `_row` — Intra-Row (Local Context)

```json
{
  "rule_id": "rule_medicine_other",
  "scope": "medicines",
  "triggers": ["collections.medicines.amd"],
  "condition": {
    "==": [{ "var": "_row.amd" }, "other"]
  },
  "effects": [
    { "target_node": "MYZOyP37ilc", "action": "SHOW" }
  ]
}
```

**Resolution:** The UI component rendering row `uuid-123` passes `collections.medicines["uuid-123"]` as `_row`. Fast, O(1).

#### `$rel` — Relational Aggregation (Parent-Scoped)

```json
{
  "rule_id": "rule_milk_supplement",
  "scope": "households",
  "triggers": ["collections.family_members.age"],
  "condition": {
    "some": [
      { "var": "$rel.family_members" },
      { "<": [{ "var": "age" }, 5] }
    ]
  },
  "effects": [
    { "target_node": "node_milk_field", "action": "SHOW" }
  ]
}
```

**Resolution:**
1. See `$rel.family_members` in a rule scoped to `households`.
2. Get the current household (e.g., `hh_001`).
3. Filter: `Object.values(collections.family_members).filter(m => m._parent_id === 'hh_001')`
4. Feed the resulting array to JsonLogic `some`.

#### `$global` — Global Aggregation (All Rows)

```json
{
  "rule_id": "rule_global_narcotic",
  "scope": "global",
  "triggers": ["collections.medicines.drug_type"],
  "condition": {
    "some": [
      { "var": "$global.medicines" },
      { "==": [{ "var": "drug_type" }, "Narcotic"] }
    ]
  },
  "effects": [
    { "target_node": "node_signature_pad", "action": "SET_REQUIRED", "value": true }
  ]
}
```

**Resolution:** Ignore `_parent_id`. Return `Object.values(collections.medicines)` as a flat array.

### 6.3 Architecture Win

| Concern | Owner | Stability |
|---|---|---|
| **Rule definitions** (JSON AST in template) | Template author | Stable unless logic changes |
| **Data shape** (normalized submission model) | Backend contract | Stable — P1 through P6 |
| **Namespace resolvers** (`_row`, `$rel`, `$global`) | Frontend state manager (memoized selectors) | Implementation detail — can be optimized without contract change |

> If you ever change how the database stores data, you don't rewrite a single rule. You update the frontend's selector that resolves `$rel`.

---

## 7. Anti-Corruption Layer (ACL)

### 7.1 Purpose

The ACL sits in the backend and **translates** between V1 shapes and the canonical normalized model. No external consumer ever sees the other side of the boundary.

```
Mobile (V1) ──► V1 REST ──► ACL ──► Canonical Model ──► ACL ──► V2 REST ──► Web (V2)
                                         ▲
                                    (single store)
```

### 7.2 Two Translation Directions

| Direction | When | What It Does |
|---|---|---|
| **V1 → Canonical** (ingest) | V1 `POST /api/v1/dataSubmission` | Takes V1 `formData` (`{ main: {…}, medicines: [{…}] }`) and normalizes into `{ values: {…}, collections: { medicines: { id: {…} } } }`. Stores the normalized form. |
| **Canonical → V1** (egress) | V1 `GET /api/v1/dataSubmission` | Reads normalized store, denormalizes back into V1 shape for mobile consumption. Restores section wrappers, re-arrays repeaters. |
| **V2 passthrough** (both) | V2 endpoints | V2 endpoints speak the canonical shape natively — no translation needed. |

### 7.3 Key Design Constraint

> [!IMPORTANT]
> The ACL is an **internal service** — it is never exposed as an API. The V1 and V2 REST controllers call it internally. From the outside, each API version simply "speaks its own dialect."

### 7.4 Storage Model Decision

> [!WARNING]
> **Open decision — must resolve before coding.**

**Option A: Single canonical store, translate on read for V1.**
- Pro: Single source of truth, no dual-write.
- Con: V1 reads require denormalization.

**Option B: Dual-write (store both shapes).**
- Pro: V1 reads are zero-cost.
- Con: Two copies to keep in sync, risk of drift.

**Option C: Store canonical + cache V1 shape (materialized view / computed column).**
- Pro: Best of both — single source of truth + fast V1 reads.
- Con: Slightly more complex.

**Current recommendation:** Option A for simplicity. V1 read traffic is bounded (mobile offline sync is periodic), and the denormalization is O(N) in the number of fields — well within acceptable latency. Reassess if profiling shows otherwise.

---

## 8. Migration Strategy — Phases

### Phase 0: Foundation (No User-Facing Change)
- [ ] Write the V1→Canonical and Canonical→V1 translators as a pure service (no REST changes).
- [ ] Cover with unit tests: round-trip `v1_input → normalize → denormalize → v1_output` must be identity.
- [ ] Handle edge cases: empty repeaters, null values, multi-select arrays.

### Phase 1: V2 Template Tree Endpoint
- [ ] Build the HashMap Registry Template Transformer (§5.3).
- [ ] Expose `GET /api/v2/formTemplates/{uid}` returning the V2 tree.
- [ ] V1 template endpoint remains unchanged.
- [ ] Web frontend starts consuming V2 tree for rendering.

### Phase 2: V2 Submission Endpoints
- [ ] Expose `POST /api/v2/dataSubmission` accepting V2 normalized shape.
- [ ] Expose `GET /api/v2/dataSubmission/{uid}` returning V2 normalized shape.
- [ ] V1 endpoint ingest now goes through ACL: `v1_input → normalize → store`.
- [ ] V1 endpoint reads now go through ACL: `read_normalized → denormalize → v1_output`.
- [ ] Web frontend uses V2 endpoints exclusively.

### Phase 3: Historical Data Migration
- [ ] Batch-migrate existing `form_data` JSONB from V1 shape to canonical normalized shape.
- [ ] Migration must be idempotent (re-runnable).
- [ ] Log every migration transformation for auditability.

### Phase 4: Mobile Gradual Migration
- [ ] Mobile client begins adopting V2 endpoints (coordinated release).
- [ ] V1 endpoints receive deprecation notices in response headers.
- [ ] Once V1 traffic drops to zero, sunset V1 endpoints.

---

## 9. Open Questions & Decision Log

| # | Question | Status | Decision |
|---|---|---|---|
| Q1 | Should `values` keys be `name` or `code`? Currently `name` and `code` can differ (e.g., `name: "ispregnant"`, `code: "de_ispregnant"`). | **OPEN** | Leaning toward `name` — it's what the mobile uses today. But `code` is the semantic identifier. Need to decide. |
| Q2 | Storage model: Option A, B, or C? (§7.4) | **OPEN** | Leaning A. |
| Q3 | Should the V2 tree include the full `rules[]` objects, or should rules be delivered separately? | **OPEN** | Keeping them inline for now (simpler). May extract to a separate endpoint if tree payloads become too large. |
| Q4 | Rule expression format: keep `#{fieldName}` for V1 compat, or migrate to JsonLogic AST in V2? | **OPEN** | V2 template tree should deliver rules in JsonLogic AST format. V1 legacy expression format stays in the raw `TemplateVersion.fields[].rules[]` JSONB as-is. The Template Transformer converts legacy → AST. |
| Q5 | How to handle `_parentId` and `_submissionUid` that V1 mobile injects into repeat rows? | **OPEN** | ACL strips them on ingest. V2 shape uses `_parent_id` (for nested repeats) and never includes `_submissionUid` (it's metadata on the parent submission, not on each row). |
| Q6 | V2 REST path prefix: `/api/v2/…` or `/api/web/…`? | **OPEN** | `/api/v2/` — it's a version, not a client type. |
| Q7 | Validation errors: should V2 return structured field-level errors keyed by `binding`? | **OPEN** | Yes, proposed shape: `{ "errors": { "visitdate": ["required"], "collections.medicines.row_123.amd": ["invalid_option"] } }`. |

---

## 10. Appendix — Reference Files

### Entities & POJOs
- [DataTemplate.java](..\..\src\main\java\org\nmcpye\datarun\jpa\datatemplate\DataTemplate.java)
- [TemplateVersion.java](..\..\src\main\java\org\nmcpye\datarun\jpa\datatemplate\TemplateVersion.java)
- [TemplateElement.java](..\..\src\main\java\org\nmcpye\datarun\jpa\datatemplate\TemplateElement.java)
- [DataSubmission.java](..\..\src\main\java\org\nmcpye\datarun\jpa\datasubmission\DataSubmission.java)
- [FormDataElementConf.java](..\..\src\main\java\org\nmcpye\datarun\datatemplateelement\FormDataElementConf.java)
- [FormSectionConf.java](..\..\src\main\java\org\nmcpye\datarun\datatemplateelement\FormSectionConf.java)
- [AbstractElement.java](..\..\src\main\java\org\nmcpye\datarun\datatemplateelement\AbstractElement.java)

### V1 REST Resources
- [DataSubmissionResource.java](..\..\src\main\java\org\nmcpye\datarun\web\rest\v1\datasubmission\DataSubmissionResource.java)
- [FormTemplateMergeResource.java](..\..\src\main\java\org\nmcpye\datarun\web\rest\v1\formtemplate\FormTemplateMergeResource.java)

### Sample Data
- [V1 Template Version Sample](\sample_data\template_version_v1_sample.json)
- [V1 Submission Sample](\sample_data\data_submission_v1_sample.json)
