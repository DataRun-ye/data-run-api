# Authoritative Model Description

This document is the authoritative source for the model used by the ETL/analytics pipeline. It describes table schemas,
key types, service responsibilities, and deterministic behaviors used by transforms, resolution, and event anchoring.

---

## Platform / Build dependencies

* **Java 17+ (Spring Boot 3.4.2)**: A Maven-based project.
* **PostgresSQL (tested with v16.x)**: Utilizes a compatible PostgresSQL JDBC driver.
* **Liquibase (XML)**: Used for managing schema migrations.
* **`jOOQ` & `NamedParameterJdbcTemplate`/`JdbcTemplate`**: Available for analytical queries.
* **Caching**: Employs Ehcache and Hibernate 2nd-level cache annotations where appropriate.
* **Mapping and Codegen Tools**: Lombok (preferred for compactness and brevity) and MapStruct are used.

## Key primitive types and lengths

* `submission_uid`: `varchar(11)` (external short id used in submissions)
* `submission_id`: `varchar(26)` (internal ULID for submission records)
* `instance_key`: `varchar(26)` (ULID; equals `submission_uid` for root, `repeat_instance_id` for repeats)
* `value_ref_uid`: canonical 11-char UIDs for values of reference type i.e referencing a canonical entity in the system
  `ce.semantic_type` = one of `Option`, `OrgUnit`, `Team`, `Activity`, it would have the resolved `option_uid`,
  `org_unit_uid`, `team_uid`, etc. already resolved upstream.
* `canonical_element_id`: `uuid` (deterministic id derived from template_uid + canonical_path + data_type +
  semantic_type)

---

## Tables (concise spec)

### analytics.submission_keys

* Purpose: small denorm one-row-per-submission for fast grouping/filters.
* PK: `submission_uid (varchar11)`.
* Columns: `submission_id (varchar26)`, `assignment_uid (varchar11)`, `activity_uid (varchar11)`,
  `org_unit_uid (varchar11)`, `team_uid (varchar11)`, `template_uid (varchar11)`, `last_seen`, `created_at`,
  `updated_at`.
* Indexes: `activity_uid`, `org_unit_uid`, `team_uid`.

### analytics.events

* Purpose: canonical instance registry (root or repeat) with anchors.
* PK: `event_uid (varchar26)`, unique `instance_key (varchar26)`.
* Columns: `instance_key`, `event_type` (`root`|`repeat`), `submission_uid`, `submission_id`, `assignment_uid`,
  `activity_uid`, `org_unit_uid`, `team_uid`, `template_uid`, `submission_creation_time`, `start_time`, `last_seen`,
  `created_at`, `updated_at`.
* Anchor columns: `anchor_ce_id uuid`, `anchor_ref_uid (varchar11)`, `anchor_value_text (text)`,
  `anchor_confidence (numeric(5,4))`, `anchor_resolved_at (timetamp)`.
* Indexes: unique(instance_key), `(anchor_ce_id, anchor_ref_uid)`, `submission_uid`.

### analytics.tall_canonical

* Purpose: EAV tall attribute store (one row per attribute instance).
* Uniqueness: unique constraint on `(instance_key, canonical_element_id)` for idempotent upserts.
* Important columns: `instance_key (varchar26)`, `submission_uid (varchar11)`, `submission_id (varchar26)`,
  `canonical_element_id (uuid)`, `element_path`, `repeat_instance_id`, `parent_instance_id`, `repeat_index`, typed
  values `value_text` (store all type of values), `value_number` (values numeric types also available here),
  `value_json` (values of List<String> i.e multi-select choices, or other json type values are stored only here),
  `value_ref_type` (if value of ref type i.e ce.semantic_type=Option, OrgUnit, Team, etc, in addition to storing the raw
  value in value_text,  `value_ref_uid` will store the resolved uid of the entity),
  `is_deleted`, provenance (`outbox_id, ingest_id, created_at, updated_at`, `submission_creation_time`, `start_time`).
* Joins: `tall_canonical.instance_key -> events.instance_key` for instance joins;
  `tall_canonical.value_ref_uid -> dim_*` for resolved refs.

### analytics.ref_resolution

* Purpose: authoritative audit and cache of raw token → canonical uid resolutions.
* PK: `value_ref_uid (varchar)`.
* Columns: `raw_value (text)`, `raw_source (varchar100)`, `ref_type (varchar50)` (e.g., `option`, `orgunit`, `team`,
  `activity`, `assignment`), `resolved_uid (varchar11)`, `confidence numeric(5,4)`, `resolved_at (timetamp)`,
  `replaced_by (varchar26)`, `notes`, `created_at`, `updated_at`.
* Indexes: `(raw_value, ref_type)`, `(resolved_uid)`.

### public.canonical_element (ce)

* Purpose: canonical metadata for elements.
* PK: `canonical_element_id (uuid)` deterministic from template+path+types.
* Key attrs: `semantic_type` (e.g., `option`, `orgunit`, `team`, `activity`, `assignment`), `option_set_uid`,
  `canonical_path`, `data_type`.
* Treated as stable/immutable; used to map element → semantic handling.

### public.canonical_element_anchor (cea)

* Purpose: 1:1 auxiliary config for ce used as anchors.
* PK/FK: `canonical_element_id` → `canonical_element.canonical_element_id`.
* Columns: `anchor_allowed boolean`, `anchor_priority int` (smaller = higher priority), `updated_by`, `updated_at`.

### analytics.dim_* (dim_option, dim_org_unit, dim_team, ...)

* Purpose: canonical dimension/dict tables. Keys are `*_uid (varchar11)` and include codes / names / option_set links.
* Used by `RefResolutionService` for deterministic lookup and by analysts via joins.

---

## Service responsibilities & deterministic behaviors

### TransformServiceRobust

* Pure JSON traversal: accepts `JsonNode root` and a **List<TemplateElement>** (template element descriptions with
  `jsonDataPath`) and produces `List<TallCanonicalRow>` without side effects. This component strictly expects
  `TemplateElement` objects (not CE).

### TransformServiceV2

* Calls `TransformServiceRobust` (with `TemplateElement` list), then:

    1. Upserts `submission_keys` for the submission root (idempotent).
    2. For each tall row: determines CE id, inspects CE metadata and `cea` to decide if the element is a ref/anchor
       candidate.
    3. For ref-type elements (`option`, `orgunit`, `team`, `activity`, `assignment`) calls
       `RefResolutionService.resolve(...)` to get `(resolvedUid, confidence, resolvedAt)` and sets
       `tall.value_ref_uid` + `tall.value_ref_type`.
    4. Collects anchor candidates per `instance_key` (repeat or root) when `anchor_allowed=true` for the CE.
    5. Writes an `events` row for every observed `instance_key` (root + repeats). If candidates exist, chooses best
       candidate and sets anchor fields; otherwise upserts a row with null anchors (instance registry).
    6. Returns enriched tall rows for persistence to `tall_canonical` (caller persists with idempotent upsert).

### RefResolutionService (deterministic v1 behavior)

* Behavior: (1) check in-process cache, (2) lookup `ref_resolution` latest row for `(raw_value, ref_type)`, (3) if
  absent perform deterministic dim lookup via `dim_*` repo methods (optionSet-aware for options), (4) persist a
  `ref_resolution` row (including misses with `resolved_uid=NULL` and confidence=0), (5) return
  `Resolution(resolvedUid, confidence, resolvedAt)`.

---

## Relational model and join patterns (concise)

* `instance_key` is the canonical join column linking `tall_canonical` <-> `events`. For submission root
  `instance_key == submission_uid`. Repeats have `repeat_instance_id` stored in tall rows and used as `instance_key`.
* For submission-level dashboards use `submission_keys` (one-row) for filters and joins. For attribute-level or
  repeat-aware queries join `tall_canonical` → `events` by `instance_key`.
* Use `tall.value_ref_uid` to join to `dim_*` for analyses that require canonical dimensions instead of fragile text
  matching.

---

## Appendix

### 2. **Core Database Schema Relationships**

Visualizes how main tables relate:

```mermaid
erDiagram
    submission_keys {
        varchar11 submission_uid PK
        varchar11 assignment_uid
        varchar11 activity_uid
        varchar11 org_unit_uid
        varchar11 team_uid
        timestamp last_seen
    }

    events {
        varchar26 event_uid PK
        varchar26 instance_key UK
        varchar11 submission_uid FK
        uuid anchor_ce_id
        varchar11 anchor_ref_uid
    }

    tall_canonical {
        varchar26 instance_key FK
        varchar11 submission_uid FK
        uuid canonical_element_id FK
        varchar26 repeat_instance_id
        varchar26 parent_instance_id
        varchar11 value_ref_uid
        text value_text
        numeric value_number
    }

    canonical_element {
        uuid canonical_element_id PK
        varchar50 semantic_type
        varchar11 option_set_uid
        text canonical_path
    }

    canonical_element_anchor {
        uuid canonical_element_id PK, FK
        boolean anchor_allowed
        int anchor_priority
    }

    ref_resolution {
        varchar26 value_ref_uid PK
        text raw_value
        varchar50 ref_type
        varchar11 resolved_uid FK
        numeric confidence
    }

    dim_option ||--o{ ref_resolution: resolved_uid
    dim_org_unit ||--o{ ref_resolution: resolved_uid
    dim_team ||--o{ ref_resolution: resolved_uid
    submission_keys ||--o{ events: "submission_uid"
    events ||--o{ tall_canonical: "instance_key"
    canonical_element ||--o{ tall_canonical: "canonical_element_id"
    canonical_element ||--|| canonical_element_anchor: "canonical_element_id"
    ref_resolution }o--|| tall_canonical: "value_ref_uid"
```

### 2. **Instance Identity Model**

Clarifies the `instance_key` concept and relationships:

```mermaid
graph TD
    subgraph "Root Instance (Submission)"
        A[submission_uid: abc123<br/>instance_key = abc123] --> B[tall_canonical row 1]
        A --> C[tall_canonical row 2]
        A --> D[tall_canonical row 3]
    end

    subgraph "Repeat Instance 1"
        E[repeat_instance_id: def456<br/>instance_key = def456] --> F[tall_canonical row 4]
        E --> G[tall_canonical row 5]
        A -.-> E
    end

    subgraph "Repeat Instance 2"
        H[repeat_instance_id: ghi789<br/>instance_key = ghi789] --> I[tall_canonical row 6]
        A -.-> H
    end

    J[events table row 1<br/>instance_key: abc123] --- A
    K[events table row 2<br/>instance_key: def456] --- E
    L[events table row 3<br/>instance_key: ghi789] --- H
    style A fill: #3856c9
    style E fill: #6083e0
    style H fill: #0883e0
```

### 3. **Analytics Query Pattern**

Shows how typical queries join tables:

```mermaid
graph LR
    subgraph "Filtering Layer"
        A[API Parameters] --> B(submission_keys)
        A --> C(events via anchors)
    end

    B --> D{tall_canonical}
    C --> D
    D --> E[dim_* tables]
    D --> F[canonical_element]
    E --> G[Aggregation]
    F --> G
    G --> H[Results]

    subgraph "Superset Filters → API Params"
        I[ou_uid] --> A
        J[ou_group_uid] --> A
        K[team_uid] --> A
        L[activity_uid] --> A
        M[assignment_uid] --> A
        N[time_range] --> A
        O[template_uid] --> A
        P[anchor_ref_uid] --> A
        Q[repeat_ce_id] --> A
    end

    style I fill: #6060fe
    style P fill: #3030e0
    style Q fill: #4040e0
```

### 4. **Anchor Concept: Within-Submission Grouping Elements**

```mermaid
graph TD
    subgraph "Submission Root"
        A[Root Instance] --> B[Element A: text]
        A --> C[Element B: number]
        A --> D[Anchor Element: org_unit_ref]
        A --> E[Element C: option_ref]
        A --> F[Element D: date]
    end

    subgraph "Repeat Group 1"
        G[Repeat Instance 1] --> H[Element A: text]
        G --> I[Anchor Element: activity_ref]
        G --> J[Element B: number]
        G --> K[Element C: option_ref]
    end

    subgraph "Repeat Group 2"
        L[Repeat Instance 2] --> M[Element A: text]
        L --> N[Anchor Element: activity_ref]
        L --> O[Element B: number]
        L --> P[Element C: option_ref]
    end

    A --> G
    A --> L
    D -.-> Q[Groups all root elements<br/>by org_unit_ref]
    I -.-> R[Groups repeat elements<br/>by activity_ref]
    N -.-> R
    style D fill: #5050e0
    style I fill: #5050e0
    style N fill: #5050e0
    style Q fill: #5050fe
    style R fill: #5050fe
```

### 2. **Anchor Configuration & Selection Flow**

```mermaid
flowchart TD
    subgraph "Template Configuration"
        A[Template Elements]
        A --> B[Element 1: text_field]
        A --> C[Element 2: org_unit_ref<br/>marked as anchor]
        A --> D[Element 3: activity_ref<br/>marked as anchor]
        A --> E[Element 4: option_ref]
    end

    subgraph "Data Submission"
        F[Submission JSON] --> G[TransformServiceV2]
        G --> H{Check canonical_element_anchor}
        H -- Anchor allowed --> I[Add to anchor candidates]
        H -- Not anchor --> J[Process normally]
        I --> K[Evaluate anchor_priority]
        K --> L[Select best anchor per instance]
    end

    subgraph "Persist Anchor Context"
        L --> M[Update events.anchor_ce_id]
        L --> N[Update events.anchor_ref_uid]
        M --> O[Anchor available for grouping]
        N --> P[Link to dim_* tables]
    end

    style C fill: #4040e0
    style D fill: #4040e0
    style O fill: #4040fe
```

### 4. **Hierarchical Anchor Grouping**

```mermaid
graph TB
    subgraph "Submission with Nested Repeats"
        R[Root Instance<br/>Anchor: org_unit_uid]
        R --> G1[Repeat Group A<br/>Anchor: team_uid]
        R --> G2[Repeat Group B<br/>Anchor: team_uid]
        G1 --> S1[Repeat Item A1<br/>Anchor: activity_uid]
        G1 --> S2[Repeat Item A2<br/>Anchor: activity_uid]
        G2 --> S3[Repeat Item B1<br/>Anchor: activity_uid]
        S1 --> T1[Values: q1, q2, q3]
        S2 --> T2[Values: q1, q2, q3]
        S3 --> T3[Values: q1, q2, q3]
    end

    subgraph "Analytics Grouping"
        U[Group by org_unit_uid] --> V[All root-level analysis]
        W[Group by team_uid] --> X[Within each team<br/>see nested repeats]
        Y[Group by activity_uid] --> Z[Drill down to<br/>specific activities]
    end

    R -.-> U
    G1 -.-> W
    G2 -.-> W
    S1 -.-> Y
    S2 -.-> Y
    S3 -.-> Y
    style R fill: #5050e0
    style G1 fill: #5050e0
    style G2 fill: #5050e0
    style S1 fill: #5050e0
    style S2 fill: #5050e0
    style S3 fill: #5050e0
```
