# Datarun ERD (Mermaid) + Fixes

A concise reference for the Datarun data-collection platform.

## 1.1 Platform / Build Assumptions

The current system is built upon:

* **Java 17+ (Spring Boot 3.4.2)**: A Maven-based project, initially generated with JHipster and extended.
* **PostgreSQL (tested with v16.x)**: Utilizes a compatible PostgreSQL JDBC driver.
* **Liquibase (XML)**: Used for managing schema migrations.
* **Spring Security & Application-level ACLs**: Integrated for security.
* **`jOOQ` & `NamedParameterJdbcTemplate`/`JdbcTemplate`**: Available for analytical queries.
* **Caching**: Employs Ehcache and Hibernate 2nd-level cache annotations where appropriate.
* **Mapping and Codegen Tools**: Lombok and MapStruct are used.
* **Testing**: Testcontainers (Postgres), JUnit 5, and AssertJ are used for testing.
* **User authentication**:  sending basic user's credentials and receiving Access/Refresh tokens.

---

## Key model clarifications

### 1. IDs, UIDs and business keys

* **id**: internal primary key (VARCHAR(26)) ULID format. Immutable, never recycled. Used for all foreign-key
  relationships.
* **uid**: short system generated business key (VARCHAR(11)), globally unique, stable across environments, used
  extensively in api client's requests and analytics for human-friendly references.

## Overview — design choices already applied to Datarun

Datarun is following strong, production-grade design choices that improve data integrity and long-term
maintainability:

### 1. **Canonical, immutable entities.** Most system entities referenced in submissions (e.g., `Activity`, `Team`,

`OrgUnit`, `DataElement`, `OptionSet`, `Option`) are canonical, identified by UID, and managed with an explicit
lifecycle (publish, deprecate/soft-delete). Once published and used, these entities are *not* mutated in-place (no
changing `valueType` or semantics).
*Benefit:* prevents semantic drift and keeps analysis reproducible.

### 2. **Immutability as the bedrock of integrity**

**Principle:** once an entity is published and used by submissions, its semantic contract is immutable. Practical
rules implemented so far:

    * `DataTemplateVersion` (the form schema) is locked on publication — schema changes create a new version.
    * `DataElement.valueType` (semantic type: number/string/date/…) cannot change once the element has been used in
      submissions; changes produce a new element/version.
    * `DataSubmission` references (`template_id`, `template_version_id`) are immutable after creation.

### 3. Strict Separation of Concerns

**Principle:** Clear distinction between timeless concepts and contextual applications

- **Canonical (The "What"):** DataElement represents pure abstract definition
- **Contextual (The "How"):** ElementTemplateConfig represents specific usage in a template

### 4. Idempotent, Transactional Processes

**Principle:** All data processing operations are designed to be safely repeatable

- **ETL Process:** Uses "sweep-and-update" pattern within transactions.
- **Metadata Generation:** Event-driven with proper transaction boundaries

### 5. Repeats Grain canonical, stable identifier

we introduced a canonical identifier for repeat groups that decouples analytical grain semantics from
presentation/layout. Key changes:

1. Add `semantic_repeat_path VARCHAR(3000)` to `ElementTemplateConfig` — a canonical, stable identifier for the grain (
   repeat group) within a template.
   *Purpose:* a stable semantic key that survives cosmetic renames or UI reordering.

## High level view of System layers

**Flowchart — illustrates a high level view of the data and processing layers, showing how data flows from configuration
to analytics.**
This diagram captures the layered architecture. It shows how canonical dimension tables relate to configuration, which
feeds submissions and is ETL-processed into analytics-ready facts.

## 1. Complete System Architecture with Analytics Layer

illustrates a high level view of the data and processing layers, showing how data flows from configuration
to analytics. This diagram captures the layered architecture. It shows how canonical dimension tables relate to
configuration, which
feeds submissions and is ETL-processed into analytics-ready facts.

```mermaid
flowchart TB
    subgraph L1 [Layer 1: Canonical Dimension Tables]
        direction LR
        DataElement[Data Element]
        OptionSet[OptionSet]
        OptionValue[OptionValue]
        Team[Team]
        OrgUnit[Org Unit]
        Activity[Activity]
        OrgUnitHierarchy[Org Unit Hierarchy]
    end

    subgraph L2 [Layer 2: Configuration & Staging]
        direction TB
        DataTemplate[DataTemplate]
        DataTemplateVersion[DataTemplateVersion]
        DataTemplate --> DataTemplateVersion
    end

    subgraph L3 [Layer 3: Template Configuration]
        direction TB
        ElementTemplateConfig[ElementTemplateConfig<br/>Field Configurations]
        ElementTemplateMap[ElementTemplateMap<br/>Mappings]
        DataTemplateVersion --> ElementTemplateConfig
        ElementTemplateConfig --> ElementTemplateMap
    end

    subgraph L4 [Layer 4: Operational Data]
        direction TB
        DataSubmission[DataSubmission]
        DataSubmissionHistory[DataSubmissionHistory]
        DataSubmission --> DataSubmissionHistory
        DataTemplateVersion --> DataSubmission
    end

    subgraph L5 [Layer 5: ETL Processing]
        direction TB
        RepeatInstance[Repeat Facts]
        ElementDataValue[Cell Value Facts]
        DataSubmission -- ETL --> RepeatInstance
        DataSubmission -- ETL --> ElementDataValue
        RepeatInstance --> ElementDataValue
    end

    subgraph L6 [Layer 6: Analytics Foundation]
        direction TB
        PivotGridFacts[Pivot Grid Facts<br/>Materialized View]
        ElementDataValue --> PivotGridFacts
        RepeatInstance --> PivotGridFacts
    end

    subgraph L7 [Layer 7: Analytics Metadata & Wide Models]
        direction TB
        AnalyticsMetadata[Analytics Metadata Service]
        WideModels[Template-Specific Wide Models]
        AnalyticsEntity[AnalyticsEntity]
        AnalyticsAttribute[AnalyticsAttribute]
        AnalyticsRelationship[AnalyticsRelationship]
        AnalyticsMetadata --> AnalyticsEntity
        AnalyticsMetadata --> AnalyticsAttribute
        AnalyticsMetadata --> AnalyticsRelationship
        AnalyticsMetadata -- configures --> WideModels
    end

    subgraph L8 [Layer 8: API & Query Layer]
        direction TB
        DynamicQueryEngine[Dynamic Query Engine]
        MetadataService[Analytics Metadata Service]
        MVManager[Materialized View Manager]
        DynamicQueryEngine -- uses --> MetadataService
        DynamicQueryEngine -- manages --> MVManager
    end

    subgraph L9 [Layer 9: Client Interface]
        direction TB
        ClientAPI[Client API]
        AdminUI[Admin UI]
        ReportUI[Report UI]
    end

%% Relationships between layers
    L1 -- referenced by --> L2
    L2 -- configures --> L3
    L3 -- defines structure --> L4
    L4 -- processed by --> L5
    L5 -- feeds --> L6
    L6 -- source for --> L7
    L7 -- drives --> L8
    L8 -- serves --> L9
%% Specific relationships
    DataElement -- canonical definition --> ElementTemplateConfig
    OptionSet -- options reference --> ElementTemplateConfig
    Team -- context --> DataSubmission
    OrgUnit -- context --> DataSubmission
    Activity -- context --> DataSubmission
    OrgUnitHierarchy -- hierarchy queries --> PivotGridFacts
    PivotGridFacts -- source --> WideModels
```

**Note:** Layer **7**, **8**, and **9** are still a "general idea" and not implemented yet.

## Detailed picture of the flow

### creat/update DataTemplateVersion flow

**Process & Event Flows A. Template Publishing & Analytics Model Generation**

```mermaid
sequenceDiagram
    autonumber
    participant Admin
    participant SpringBootApp as Spring Boot App
    participant EventBus as Event Bus
    participant Database
    Admin ->> SpringBootApp: Publish DataTemplateVersion (vX)
    SpringBootApp ->> Database: Save DataTemplateVersion
    SpringBootApp ->> EventBus: NewTemplateVersionPublishedEvent
    EventBus -->> SpringBootApp: Deliver event to listeners

    par On Event
        SpringBootApp ->> Database: Generate ElementTemplateConfig
    end
```

* **`ElementTemplateConfig` Key Points:**

1. **Core Identity**: Uses a sequential `id` as primary key and a unique 11-character `uid` as business key
2. **Template Context**: Links to template (`template_uid`) and specific version (`template_version_uid`, `version_no`)
3. **Element Reference**: Connects to canonical DataElement via `data_element_uid`
4. **Type Information**: Captures value type, aggregation type, and element kind (FIELD/REPEAT)
5. **Structural Metadata**: Contains multiple path fields (`id_path`, `name_path`, `semantic_path`) for hierarchical
   positioning
6. **Analytics Configuration**: Flags for dimension/measure categorization and reference types
7. **Localization Support**: `display_label` stores multilingual labels as JSON
8. **Immutability**: `created_at` tracks creation time; most fields are not updatable after creation

**See Appendix** detailed diagrams

---

#### ETL model & execution (uid-native)

The ETL result is a generalized star schema tables, it is for all templates. It provides a single, unified, and scalable
way to query any piece of data. it maintains a "tall" fact table, but surrounds it with rich dimension tables for
context (for start our existing tables (`Team`, `OrgUnit`, `Activity`, `DataElement`, `OptionSet`, `Option`, etc) are
already perfect dimension tables.)

**A. Submission ETL Flow ("Sweep and Update")**: Visualizes the idempotent ETL process on new/updated submissions.

```mermaid

sequenceDiagram
    autonumber
    participant Client as Client
    participant SpringBootApp as Spring Boot App
    participant EventBus as Event Bus
    participant Database
    Client ->> SpringBootApp: POST DataSubmission
    SpringBootApp ->> Database: Save DataSubmission
    SpringBootApp ->> EventBus: SubmissionUpdatedEvent
    EventBus -->> SpringBootApp: Deliver event to ETL listener

    rect rgb(13,43,139)
        SpringBootApp ->> Database: BEGIN TRANSACTION
        SpringBootApp ->> Database: Soft-delete ETL rows for this submission
        SpringBootApp ->> Database: Insert repeat_instance rows
        SpringBootApp ->> Database: Insert element_data_value rows
        SpringBootApp ->> Database: COMMIT
    end

    Note right of SpringBootApp: Later, scheduled job runs<br/>REFRESH MATERIALIZED VIEW
```

- ETL is idempotent and implemented as a sweep-update transaction per submission:
    1. Load `data_submission.form_data` and the referenced `data_template_version.fields/sections`.
    2. Insert or upsert `repeat_instance` rows representing repeats and their hierarchy.
    3. Normalize values into `element_data_value` typed rows (one atomic value per row; select-multi expands to multiple
       rows).
    4. Multi-select are expanded to multi `element_data_value` rows
    5. Soft-mark stale rows from previous runs (if any) and insert current rows.
    6. Record ETL run metadata (`etl_version`, `run_ts`, `checksum`) for traceability.
- Deduplication is enforced by unique constraints using stable composite keys (e.g.,
  `submission_uid` + `element_uid` + `repeat_instance_id` + `option_uid` (for MultiSelect)).

**See Appendix** detailed diagrams

---

##### 1.2 — Fact storage

- `element_data_value` stores normalized atomic values with typed columns:
    - `value_num`, `value_bool`, `value_ref_uid`, `option_uid`, `value_ts`, `value_text`.
- Context columns include `submission_uid`, `assignment_uid`, `team_uid`, `org_unit_uid`, `activity_uid`, `element_uid`,
  `element_template_config_uid`, `repeat_instance_id`.
- Unique index `ux_element_value_unique` enforces idempotence for re-run ETL.

---

##### 1.3 — Repeat groups

- Repeat groups are modeled as rows in `repeat_instance`:
    - Each row captures `id`, `parent_repeat_instance_id`, `submission_uid`, `repeat_path`, `semantic_path`,
      `repeat_index`, and
      `repeat_section_label`.
- `element_data_value` rows link to the corresponding `repeat_instance_id` so the hierarchy is preserved for analytics
  joins.

---

## materialized view

- catch all `PIVOT_GRID_FACTS` Materialized view, flatten `element_data_value` with submission, template metadata,
  option
  labels, and dimension joins for efficient reporting.
- MV refresh jobs are scheduled.

---

## Auxiliary Dimension Tables

* **`org_unit_hierarchy` (Closure Table)** generated from canonical `OrgUnit` for analytics
  **Purpose:** Provides an efficient way to query for all descendants or ancestors of an organizational unit, regardless
  of depth.

* **`ou_level`**
  **Purpose:** Provides human-readable names and descriptions for organizational hierarchy levels.

---

## Appendix

### 1. System Minimal ERD Diagrams (Conceptual, With Configuration layer and ETL Entities)

```mermaid
erDiagram
    DATA_TEMPLATE {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(255) name
        JSONB description
        TIMESTAMP created_date
        TIMESTAMP last_modified_date
    }

    DATA_TEMPLATE_VERSION {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(11) template_uid
        INTEGER version
        JSONB fields "flat list with materialized paths field configs snapshot"
        JSONB sections "flat list with materialized paths section configs snapshot"
        VARCHAR(20) status
        TIMESTAMP created_date
    }

    DATA_ELEMENT {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(255) name
        VARCHAR(50) value_type
        BOOLEAN is_dimension
        BOOLEAN is_measure
        VARCHAR(50) aggregation_type
        VARCHAR(26) option_set_id FK
        TIMESTAMP created_date
    }

    OPTION_SET {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(255) name
        TIMESTAMP created_date
    }

    OPTION_VALUE {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(26) option_set_id FK
        VARCHAR(100) code
        VARCHAR(255) name
        INTEGER sort_order
    }

    DATA_SUBMISSION {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(11) template_uid FK
        VARCHAR(11) template_version_uid FK
        VARCHAR(11) org_unit_uid FK
        VARCHAR(11) team_uid FK
        VARCHAR(11) activity_uid FK
        JSONB form_data
        TIMESTAMP completed_at
        TIMESTAMP created_date
    }

    DATA_SUBMISSION_HISTORY {
        VARCHAR(26) id PK
        VARCHAR(11) submission_uid FK
        INTEGER version
        JSONB form_data
        TIMESTAMP created_date
    }

    ORG_UNIT {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(255) name
        VARCHAR(11) parent_uid FK
        TEXT path
        INTEGER level
        TIMESTAMP created_date
    }

    TEAM {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(255) name
        VARCHAR(100) code
        TIMESTAMP created_date
    }

    ACTIVITY {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(255) name
        TIMESTAMP created_date
    }

    ELEMENT_TEMPLATE_CONFIG {
        bigint id PK
        varchar(11) uid UK "Unique business key (11 chars, immutable)"
        varchar(16) element_kind "FIELD or REPEAT section type"
        varchar(11) template_uid "Parent DataTemplate reference"
        varchar(11) template_version_uid "Specific TemplateVersion reference"
        integer version_no
        varchar(100) data_element_uid "Canonical DataElement reference"
        text name "Element name (copied from DataElement)"
        varchar(32) value_type "Data type (copied from DataElement)"
        varchar(32) aggregation_type
        boolean is_reference "Marks reference types (Team/OrgUnit/Option)"
        varchar(11) option_set_uid "Associated OptionSet for select fields"
        boolean is_multi "Multi-select field flag"
        boolean is_measure
        boolean is_dimension
        integer sort_order
        varchar(3000) id_path "Structural, ui, using element IDs"
        varchar(3000) name_path "using element names, used in etl data extraction (Structural)"
        varchar(3000) semantic_path "True semantic path (excluding structural groupings)"
        boolean has_repeat_ancestor "In repeatable section flag"
        varchar(3000) ancestor_repeat_semantic_path "Nearest repeatable ancestor path"
        varchar(3000) ancestor_repeat_path "Nearest repeatable ancestor path (Structural)"
        jsonb display_label "Localized labels (JSON)"
    }

    REPEAT_INSTANCE {
        VARCHAR(26) id PK
        VARCHAR(26) parent_repeat_instance_id FK
        JSONB repeat_section_label
        VARCHAR(11) submission_uid FK
        VARCHAR(3000) repeat_path
        VARCHAR(3000) semantic_path
        BIGINT repeat_index
        TIMESTAMP client_updated_at
        TIMESTAMP deleted_at
        TIMESTAMP submission_completed_at
        TIMESTAMP created_date
    }

    ELEMENT_DATA_VALUE {
        BIGINT id PK
        VARCHAR(26) repeat_instance_id FK
        VARCHAR(11) submission_uid FK
        VARCHAR(11) assignment_uid
        VARCHAR(11) team_uid FK
        VARCHAR(11) org_unit_uid FK
        VARCHAR(11) activity_uid FK
        VARCHAR(11) element_uid FK
        VARCHAR(11) element_template_config_uid FK
        VARCHAR(11) option_uid FK
        TEXT value_text
        NUMERIC value_num
        BOOLEAN value_bool
        VARCHAR(11) value_ref_uid
        TIMESTAMP value_ts
        TIMESTAMP deleted_at
        TIMESTAMP created_date
        TEXT repeat_instance_key
        TEXT selection_key
        CHAR(1) row_type
    }

    ORG_UNIT_HIERARCHY {
        VARCHAR(11) ancestor_uid PK, FK
        VARCHAR(11) descendant_uid PK, FK
        INTEGER depth
    }

    OU_LEVEL {
        INTEGER level PK
        VARCHAR(255) name UK
        TEXT description
    }

    DATA_TEMPLATE ||--o{ DATA_TEMPLATE_VERSION: has
    DATA_TEMPLATE_VERSION }o--o{ DATA_ELEMENT: references
    DATA_TEMPLATE_VERSION }|--|{ DATA_SUBMISSION: used_by
    DATA_ELEMENT }o--|| OPTION_SET: has
    OPTION_SET ||--o{ OPTION_VALUE: contains
    DATA_SUBMISSION ||--o{ DATA_SUBMISSION_HISTORY: has_history
    DATA_SUBMISSION }o--|| ORG_UNIT: belongs_to
    DATA_SUBMISSION }o--|| TEAM: belongs_to
    DATA_SUBMISSION }o--|| ACTIVITY: belongs_to
    DATA_TEMPLATE_VERSION ||--o{ ELEMENT_TEMPLATE_CONFIG: generates
    DATA_ELEMENT ||--o{ ELEMENT_TEMPLATE_CONFIG: configures
    DATA_SUBMISSION ||--o{ REPEAT_INSTANCE: contains
    REPEAT_INSTANCE }o--o{ REPEAT_INSTANCE: parent_child
    DATA_SUBMISSION ||--o{ ELEMENT_DATA_VALUE: contains_data
    REPEAT_INSTANCE ||--o{ ELEMENT_DATA_VALUE: contextualizes
    ELEMENT_TEMPLATE_CONFIG ||--o{ ELEMENT_DATA_VALUE: defines
    ORG_UNIT ||--o{ ORG_UNIT_HIERARCHY: hierarchy
```

### 2. ANALYTICS CATCH ALL `PIVOT_GRID_FACTS` MV ERD

```mermaid
erDiagram
    PIVOT_GRID_FACTS {
        BIGINT value_id PK
        VARCHAR(11) submission_uid
        VARCHAR(11) template_uid
        VARCHAR(11) template_version_uid
        VARCHAR(11) etc_uid
        VARCHAR(26) repeat_instance_id
        VARCHAR(26) parent_repeat_instance_id
        VARCHAR(3000) repeat_path
        VARCHAR(3000) semantic_path
        JSONB repeat_section_label
        JSONB parent_repeat_section_label
        VARCHAR(11) assignment_uid
        VARCHAR(11) team_uid
        VARCHAR(100) team_code
        VARCHAR(11) org_unit_uid
        VARCHAR(255) org_unit_name
        VARCHAR(11) activity_uid
        VARCHAR(255) activity_name
        TIMESTAMP submission_completed_at
        JSONB display_label
        VARCHAR(11) de_uid
        VARCHAR(255) de_name
        VARCHAR(50) de_value_type
        VARCHAR(11) de_option_set_uid
        VARCHAR(11) option_uid
        VARCHAR(11) option_value_uid
        VARCHAR(255) option_name
        VARCHAR(100) option_code
        NUMERIC value_num
        TEXT value_text
        BOOLEAN value_bool
        TIMESTAMP value_ts
        VARCHAR(11) value_ref_uid
        TIMESTAMP deleted_at
    }

    ELEMENT_DATA_VALUE ||--|| PIVOT_GRID_FACTS: materializes
    DATA_SUBMISSION ||--o{ PIVOT_GRID_FACTS: contributes_to
    REPEAT_INSTANCE ||--o{ PIVOT_GRID_FACTS: contextualizes
    DATA_ELEMENT ||--o{ PIVOT_GRID_FACTS: defines
    ELEMENT_TEMPLATE_CONFIG ||--o{ PIVOT_GRID_FACTS: configures
    TEAM ||--o{ PIVOT_GRID_FACTS: describes
    ORG_UNIT ||--o{ PIVOT_GRID_FACTS: describes
    ACTIVITY ||--o{ PIVOT_GRID_FACTS: describes
    OPTION_VALUE ||--o{ PIVOT_GRID_FACTS: options
```

---

### Common Abbreviations Used Throughout The System

* `act`: Activity.
* `de`: DataElement.
* `dt`: DataTemplate.
* `dtv`: DataTemplateVersion.
* `etc`: ElementTemplateConfiguration
* `ops`: OptionSet.
* `ou`: OrgUnit.
* `ov`: OptionValue.
