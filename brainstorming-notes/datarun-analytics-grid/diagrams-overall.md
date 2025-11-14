# Datarun: Key Architectural Principles & Diagrams

## Platform / Build dependencies

The current system is built upon:

* **Java 17+ (Spring Boot 3.4.2)**: A Maven-based project, initially generated with JHipster and extended.
* **PostgresSQL (tested with v16.x)**: Utilizes a compatible PostgreSQL JDBC driver.
* **Liquibase (XML)**: Used for managing schema migrations.
* **Spring Security & Application-level ACLs**: Integrated for security.
* **`jOOQ` & `NamedParameterJdbcTemplate`/`JdbcTemplate`**: Available for analytical queries.
* **Caching**: Employs Ehcache and Hibernate 2nd-level cache annotations where appropriate.
* **Mapping and Codegen Tools**: Lombok and MapStruct are used.
* **Testing**: Testcontainers (Postgres), JUnit 5, and AssertJ are used for testing.
* **User authentication**:  sending basic user's credentials and receiving Access/Refresh tokens.

## Foundational Design Principles

### 1. IDs, UIDs and business keys

* **id**: internal primary key (VARCHAR(26)) ULID format. Immutable, never recycled. Used for all foreign-key
  relationships.
* **uid**: short system generated business key (VARCHAR(11)), globally unique, stable across environments, used
  extensively in api client's requests and analytics for human-friendly references.

### 2. Immutability as the Bedrock of Integrity

**Principle:** Critical entities are immutable once published to prevent canonical drift

- **DataTemplateVersion:** Schema is locked upon publication
- **DataElement.valueType:** Semantic definition cannot change once in use
- **DataSubmission context:** template_uid and template_version_uid are immutable after creation

## System Architecture System Overview (Concept)

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

    subgraph L7 [Layer 7: Analytics Metadata]
        direction TB
        AnalyticsMetadata[Analytics Metadata Service]
        AnalyticsEntity[AnalyticsEntity]
        AnalyticsAttribute[AnalyticsAttribute]
        AnalyticsRelationship[AnalyticsRelationship]
        AnalyticsMetadata --> AnalyticsEntity
        AnalyticsMetadata --> AnalyticsAttribute
        AnalyticsMetadata --> AnalyticsRelationship
    end

%% Relationships between layers
    L1 -- referenced by --> L2
    L2 -- configures --> L3
    L3 -- defines structure --> L4
    L4 -- processed by --> L5
    L5 -- feeds --> L6
    L6 -- source for --> L7
%% Specific relationships
    DataElement -- canonical definition --> ElementTemplateConfig
    OptionSet -- options reference --> ElementTemplateConfig
    Team -- context --> DataSubmission
    OrgUnit -- context --> DataSubmission
    Activity -- context --> DataSubmission
```

**Note:** Layer 8 API & Query Layer (metadata based, generation), is not fully implemented yet.

### Template Publishing Flow

```mermaid
sequenceDiagram
    participant Admin as Administrator
    participant Service as TemplateService
    participant DB as Database
    Admin ->> Service: Publish DataTemplateVersion (vX)
    activate Service
    Service ->> DB: Validate and save DataTemplateVersion
    DB -->> Service: Success response
    Service ->> DB: Generate ElementTemplateConfig records
    DB -->> Service: Configuration created
```

### Ingestion ETL Process Flow

```mermaid
flowchart TB
    Start([New/Updated Submission]) --> LoadData[Load Submission Data]

    subgraph ETLProcess [ETL Processing Transaction]
        direction TB
        LoadData --> ParseJSON[Parse Form JSON]
        ParseJSON --> ExtractRepeats[Extract Repeat Instances]
        ExtractRepeats --> ValidateData[Validate Data Elements]
        ValidateData --> NormalizeValues[Normalize Values]
        NormalizeValues --> PrepareInserts[Prepare Database Inserts]
    end

    PrepareInserts --> BeginTxn[BEGIN TRANSACTION]

    subgraph InTransaction [Database Operations]
        direction TB
        BeginTxn --> SoftDelete[Soft-delete previous ETL rows]
        SoftDelete --> InsertRepeats[Insert repeat_instance rows]
        InsertRepeats --> InsertValues[Insert element_data_value rows]
        InsertValues --> RecordMetadata[Record ETL metadata]
        RecordMetadata --> Commit[COMMIT TRANSACTION]
    end

    Commit --> Success{Success?}
    Success -->|Yes| TriggerRefresh[Trigger MV Refresh Job]
    Success -->|No| Rollback[ROLLBACK]
    Rollback --> LogError[Log Error & Alert]
    TriggerRefresh --> ScheduleRefresh[Schedule Materialized View Refresh]
    ScheduleRefresh --> RefreshMV[REFRESH MATERIALIZED VIEW]
    RefreshMV --> UpdateMetadata[Update ETL Version Metadata]
    UpdateMetadata --> Finish([ETL Complete])
    LogError --> Finish
```

## Appendix

### 1. Initial Canonical Layer, Minimal ERD Diagrams

```mermaid
erDiagram
    DATA_ELEMENT {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(255) name
        VARCHAR(50) value_type
        BOOLEAN is_dimension
        BOOLEAN is_measure
        VARCHAR(50) aggregation_type
        VARCHAR(26) option_set_id FK
        JSONB translations "set or translated properties"
        TIMESTAMP created_date
    }

    OPTION_SET {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(255) name
        JSONB translations "set or translated properties"
        TIMESTAMP created_date
    }

    OPTION_VALUE {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(26) option_set_id FK
        VARCHAR(100) code
        VARCHAR(255) name
        INTEGER sort_order
        JSONB translations "set or translated properties"
        TIMESTAMP created_date
    }

    ORG_UNIT {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(255) name
        VARCHAR(26) parent_id FK
        VARCHAR(1000) path "root-to-self dot delimited uids"
        INTEGER level
        JSONB translations "set or translated properties"
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
        JSONB translations "set or translated properties"
        TIMESTAMP created_date
    }

    DATA_ELEMENT }o--|| OPTION_SET: has
    OPTION_SET ||--o{ OPTION_VALUE: owns
    ACTIVITY |o--o{ TEAM: contains
```

### 1. Data Capture Layer, Minimal ERD Diagrams

```mermaid
erDiagram
    DATA_TEMPLATE {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(11) versionUid UK "latest version uid"
        VARCHAR(11) versionNo "latest version number"
        VARCHAR(255) name
        VARCHAR(500) description
        TIMESTAMP created_date
        TIMESTAMP last_modified_date
        JSONB translations "set or translated properties"
    }

    DATA_TEMPLATE_VERSION {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(11) template_uid
        INTEGER versionNumber
        JSONB fields "flat list with materialized paths field snapshot"
        JSONB sections "flat list with materialized paths section snapshot (normal and repeatable)"
        VARCHAR(20) status
        TIMESTAMP created_date
    }

    DATA_SUBMISSION {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(11) template_uid
        VARCHAR(11) template_version_uid
        VARCHAR(11) org_unit_uid
        VARCHAR(11) team_uid
        VARCHAR(11) activity_uid
        JSONB form_data "data (structured) snapshot"
        TIMESTAMP completed_at
        TIMESTAMP created_date
    }

    ELEMENT_TEMPLATE_CONFIG {
        bigint id PK
        varchar(11) uid UK "Immutable Etc's Unique business key"
        varchar(16) element_kind "FIELD or REPEAT section type"
        varchar(11) template_uid "Parent DataTemplate reference"
        varchar(11) template_version_uid "Specific TemplateVersion reference"
        integer version_no  "Template Version no"
        varchar(11) data_element_uid "Canonical DataElement reference"
        varchar(255) name "Element name (resolved from immutable DataElement.name)"
        varchar(50) value_type "Data type (immutable in and resolved from DataElement)"
        varchar(50) aggregation_type    "resolved from DataElement"
        boolean is_reference "Marks reference types (Team/OrgUnit/Option)"
        varchar(11) option_set_uid "Associated OptionSet for select fields"
        boolean is_multi "Multi-select field flag"
        boolean is_measure  "resolved from DataElement"
        boolean is_dimension "resolved from DataElement"
        integer sort_order  "ordinal in template ui"
        varchar(11) repeat_uid "Etc.uid of the nearest Repeat this etc belongs to"
        varchar(3000) id_path "Structural, ui, using element IDs"
        varchar(3000) name_path "structural path for extraction, using element names, used in etl data extraction (Structural)"
        varchar(3000) semantic_path "True semantic path (excluding ui grouping segments)"
        boolean has_repeat_ancestor "In repeatable section flag"
        varchar(3000) ancestor_repeat_semantic_path "Nearest repeatable ancestor's semantic path"
        varchar(3000) ancestor_repeat_path "Nearest repeatable ancestor's structural `id_path` path"
        jsonb display_label "de overridden Simple Localized labels (JSON) e.g {'en': '', 'ar': ''}"
        TIMESTAMP created_date
    }

    DATA_TEMPLATE ||--o{ DATA_TEMPLATE_VERSION: has
    DATA_TEMPLATE_VERSION }|--|{ DATA_SUBMISSION: referenced_by
    DATA_TEMPLATE_VERSION ||--o{ ELEMENT_TEMPLATE_CONFIG: generates
    DATA_SUBMISSION }o--|| ORG_UNIT: belongs_to
    DATA_SUBMISSION }o--|| TEAM: belongs_to
    DATA_SUBMISSION }o--|| ACTIVITY: belongs_to
```

### 3. Ingestion stage 1 etl transformation (Structure, no semantics):

```mermaid
erDiagram
    REPEAT_INSTANCE {
        VARCHAR(26) id PK
        VARCHAR(26) parent_repeat_instance_id "hierarchical context for nested repeats"
        JSONB repeat_section_label
        VARCHAR(11) submission_uid
        VARCHAR(3000) semantic_path "repeat template semantic path"
        VARCHAR(11) te_uid "repeat's ElementTemplateConfig.uid"
        BIGINT repeat_index     "simple ordinal sequential number (Occurrence index)"
        TIMESTAMP client_updated_at
        TIMESTAMP deleted_at
        TIMESTAMP submission_completed_at
        TIMESTAMP created_date
    }

    ELEMENT_DATA_VALUE {
        BIGINT id PK
        VARCHAR(26) repeat_instance_id FK
        VARCHAR(3000) semantic_path
        VARCHAR(11) te_uid "element template's config uid"
        VARCHAR(11) element_uid "canonical data_element.uid"
        TEXT value_text
        NUMERIC value_num
        BOOLEAN value_bool
        TIMESTAMP value_ts
        JSONB value_array "multi-select options array"
        VARCHAR(11) value_ref_uid "ref uid value, references Team.uid, Ou.uid, etc."
        TIMESTAMP deleted_at
        TIMESTAMP created_date
    }
    
    REPEAT_INSTANCE |o--o{ REPEAT_INSTANCE: parent_child
    REPEAT_INSTANCE ||--o{ ELEMENT_DATA_VALUE: contextualizes
    ELEMENT_TEMPLATE_CONFIG ||--o{ ELEMENT_DATA_VALUE: structures
    ELEMENT_TEMPLATE_CONFIG ||--o{ REPEAT_INSTANCE: structures
```

### Common Abbreviations Used Throughout The System

* `act`: Activity.
* `de`: DataElement.
* `ds`: DataSubmission.
* `dt`: DataTemplate.
* `dtv`: DataTemplateVersion.
* `etc`: ElementTemplateConfig
* `ops`: OptionSet.
* `ou`: OrgUnit.
* `ov`: OptionValue.
