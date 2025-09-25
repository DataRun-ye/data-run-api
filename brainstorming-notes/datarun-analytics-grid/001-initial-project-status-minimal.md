# Datarun: Key Architectural Principles & Diagrams

## Platform / Build dependencies

The current system is built upon:

* **Java 17+ (Spring Boot 3.4.2)**: A Maven-based project.
* **PostgresSQL (tested with v16.x)**: Utilizes a compatible PostgresSQL JDBC driver.
* **Liquibase (XML)**: Used for managing schema migrations.
* **Spring Security & Application-level ACLs**: Integrated for security.
* **`jOOQ` & `NamedParameterJdbcTemplate`/`JdbcTemplate`**: Available for analytical queries.
* **Caching**: Employs Ehcache and Hibernate 2nd-level cache annotations where appropriate.
* **Mapping and Codegen Tools**: Lombok and MapStruct are used.
* **Testing**: Testcontainers (Postgres), JUnit 5, and AssertJ are used for testing.
* **User authentication**:  sending basic user's credentials and receiving Access/Refresh tokens.

## Foundational Design Principles

The data platform already has a solid foundation for a **hybrid schema approach**, leveraging a **meta-data driven model
**. The key is to build a robust system on top of this foundation that can handle the raw data's flexibility while
providing a structured, queryable layer for business insights.

***

## Canonical ET TemplateElement

ET (which is an element's template-version-specific and immutable configuration, `Repeat` element, and scalar
`Elements`
element), maintain the canonical schema metadata of a template per template version.
**ET schema: see appendix**

### 1) ET paths Conceptual rules (short)

* **`namePath`** = *UI/physical path* showing where that element *is stored in the submission's JSON* (may
  include normal sections or UI grouping segments). Example: `mainsection.breedingsources.breeding_habitat_type` vs
  `breedingsources.breeding_habitat_type` (semantic). Use `namePath` when you must mirror the *exact* stored location.
* **Rule of thumb:** Use `namePath` when you traverse the exact stored location for a particular submission version.
* `name_path`/ each is unique per version.

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
    Service ->> DB: Generate TemplateElement records
    DB -->> Service: Configuration created
```

## Layer — Capture

## Purpose

Durably accept client submissions with *minimum* runtime semantics, preserve raw payload for replay/audit, and record
template/version references.

## Core artifacts

* `DATA_SUBMISSION` (DS): `form_data` JSONB, `template_uid`, `template_version_uid`, simple domain context refs e.g
  `orgUnitUid`, `teamUid`,
  activityUid)
* `DATA_TEMPLATE` (DT)/`DATA_TEMPLATE_VERSION` (DTV) (fields, sections config snapshot)
* `TEMPLATE_ELEMENT` (ET)

## Responsibilities & rules

* Only basic validation on arrival (required meta like `template_uid`, `template_version_uid`, `submission_uid`).
* Always store the raw JSON payload and submission metadata immutable in `data_submission`.

---

## Appendix

### 1. Canonical References Layer, Minimal ERD Diagrams

```mermaid
erDiagram
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

    OPTION_SET ||--o{ OPTION_VALUE: owns
    ACTIVITY |o--o{ TEAM: contains
```

### 1. Data Capture Layer, Minimal ERD Diagrams

#### Capture Templates Register (minimal)

mostly uid native.

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

    TemplateElement {
        bigint id PK
        varchar(11) uid UK "Immutable ET's Unique business key"
        varchar(16) element_kind "FIELD or REPEAT"
        varchar(11) template_uid "Parent DataTemplate reference"
        varchar(11) template_version_uid "Specific TemplateVersion reference"
        integer version_no "Template Version no"
        varchar(255) name "Element name sensitized to be used in path"
        varchar(50) value_type "Data typ"
        boolean is_reference "Marks reference types (Team/OrgUnit/Option)"
        varchar(11) option_set_uid "Associated OptionSet for select fields"
        boolean is_multi "Multi-select field flag"
        integer sort_order "ordinal in template ui"
        varchar(11) repeat_uid "ET.uid of the nearest Repeat this te belongs to"
        varchar(3000) name_path "structural path for extraction, using element names, used in etl data extraction (Structural)"
        boolean has_repeat_ancestor "In repeatable section flag"
        varchar(3000) ancestor_repeat_path "Nearest repeatable ancestor's structural extraction path"
        jsonb display_label "Simple Localized labels (JSON) e.g {'en': '', 'ar': ''}"
        TIMESTAMP created_date
    }

    DATA_SUBMISSION {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(11) template_uid
        VARCHAR(11) template_version_uid
        VARCHAR(11) org_unit_uid "reference uid of canonical ou"
        VARCHAR(11) team_uid "reference uid of canonical team"
        VARCHAR(11) activity_uid "reference uid of canonical activity"
        JSONB form_data "template's data (structured) snapshot"
        TIMESTAMP completed_at
        TIMESTAMP created_date
    }

    DATA_TEMPLATE ||--o{ DATA_TEMPLATE_VERSION: has
    DATA_TEMPLATE_VERSION ||--o{ DATA_SUBMISSION: referenced_by
    DATA_TEMPLATE_VERSION ||--|{ TemplateElement: generates
    TemplateElement }o--o{ OPTION_SET: has
    DATA_SUBMISSION }o--|| ORG_UNIT: belongs_to
    DATA_SUBMISSION }o--|| TEAM: belongs_to
    DATA_SUBMISSION }o--|| ACTIVITY: belongs_to
```

### 3. Ingestion stage 1 transformation (Structural):

queryable structural normalized data with minimal domain semantics present at ingestion time (org_unit_uid, team_uid,
etc)

```mermaid
erDiagram
    REPEAT_INSTANCE {
        VARCHAR(26) id PK
        VARCHAR(11) template_uid
        VARCHAR(11) template_version_uid
        VARCHAR(11) submission_uid
        VARCHAR(11) org_unit_uid
        VARCHAR(11) team_uid
        VARCHAR(11) activity_uid
        VARCHAR(26) client_id "client generated _id, globally unique per repeat"
        VARCHAR(26) parent_repeat_instance_id "client generated parent _id, hierarchical context for nested repeats at client submission"
        VARCHAR(3000) name_path "structural dot-delimited path of element's value in formData"
        BIGINT repeat_index "client generated simple ordinal sequential number (Occurrence index)"
        TIMESTAMP client_updated_at
        TIMESTAMP deleted_at
        TIMESTAMP submission_completed_at
        TIMESTAMP created_date
    }

    ELEMENT_DATA_VALUE {
        BIGINT id PK
        VARCHAR(11) template_uid
        VARCHAR(11) template_version_uid
        VARCHAR(11) submission_uid
        VARCHAR(11) org_unit_uid
        VARCHAR(11) team_uid
        VARCHAR(11) activity_uid
        VARCHAR(26) repeat_instance_id FK "client generated parent's repeat._id, if the element is part of a repeat"
        VARCHAR(3000) name_path "structural dot-delimited path of element's value in formData"
        TEXT value_text
        NUMERIC value_num
        BOOLEAN value_bool
        TIMESTAMP value_ts "date/timestamp values"
        JSONB value_array "multi-select options array"
        VARCHAR(11) value_ref_uid "uid ref value of a canonical system entity e.g Team.uid, OrgUnit.uid, Option.uid (single select)"
        TIMESTAMP deleted_at
        TIMESTAMP created_date
    }

    REPEAT_INSTANCE |o--o{ REPEAT_INSTANCE: parent_child
    REPEAT_INSTANCE ||--o{ ELEMENT_DATA_VALUE: contextualizes
    TemplateElement ||--o{ ELEMENT_DATA_VALUE: structures
    TemplateElement ||--o{ REPEAT_INSTANCE: structures
```

referential Join: (template_version_uid, name_path) -> etc.(template_version_uid, name_path) to get the template
descriptor metadata
including name and display labels, etc.

## current JPA entity/dto elements:

all with repository, service, and basic endpoints controllers:

`DATA_TEMPLATE`/`DATA_TEMPLATE_VERSION`/`TemplateElement` management.
`DATA_SUBMISSION` ingestion.

Domain references management: `ORG_UNIT`, `TEAM`, `ACTIVITY`, `OptionSet`, `Option`, each is a jpa entity and have
proper repositories, services, and crud rest endpoints

### Common Abbreviations Used Throughout The System

* `act`: Activity.
* `ds`: DataSubmission.
* `dt`: DataTemplate.
* `dtv`: DataTemplateVersion.
* `te`: TemplateElement (repeat or scalar)
* `ops`: OptionSet.
* `ou`: OrgUnit.
* `ov`: OptionValue.
