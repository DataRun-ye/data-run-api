# Datarun platform
a data collection platform built with a **Spring Boot** backend and a **PostgreSQL >= v15** database. The platform functions similarly to ODK (Open Data Kit) but collects data as structured JSON. Our system includes several **immutable, canonical dimension tables** that are used for reference.

## **Source/Operational Data Layer**
* **Source Tables:** These are the raw, transactional tables/Jpa/JdbcTemplate such as `user`, `project` (prj), `activity` (act), `assignment` (asi), `team` (tm), `orgUnit` (ou) (Administrative level Hierarchy), `option_set` & `option_value` (ops, ov), `data_element` (de), `data_template` (dt), `data_template_version` (dtv), the mapping of a `data_element` to a template field `element_template_config`, and `data_submission` (ds).
* **Fact Tables:** `repeat_instance`, and tall `element_data_value` fact tables, store the "facts", and they are the result of an ETL pipe.
* **Dimension Tables:** These tables contain the descriptive attributes that we use to slice, dice, and filter our facts. They provide context to the measures. for simplicity, currently the system is directly using the source tables needed as the dim_ tables e.g team, assignment, project, activity, etc.

### These dimensions include:
* **Data Templates & Versions:**.
* **DataElement:** Specific fields within a form.
* **Option & Option Set:** Predefined choices for form fields.
* **OrgUnit, Team, Activity, Project:** Hierarchical and categorical metadata.
* **User:** Data collectors or system users.
* **DomainEntity:** Uniquiely identified, Dynamically created, and tracked domain entities referenced in the data `household`, `person`, etc.
* **DataTemplate & DataTemplateVersion:** Definitions of data collection forms and their versioned payloads, reference the data elements configure where their path, place, rules per the form.
* **ElementTemplateConfig**: mapping between a `DataElement` and `DataTemplateVersion` it stores only meaningful within the context of a specific template/version attribute (`template_uid`, `template_version_uid`, `idPath`, `data_element_uid`, ..).
* **DataSubmission:** entity storing metadata info, also point to its root dims (`OrgUnit`, `User`, `Time`, `Team`, `Activity`, ...), and have `formData` (JSONB) which preserves the original payload.
* All of the source entities apply a **soft delete**/**enable/disable** instead of permanent deletion.

### 1.2 Identifiers

- Primary identifiers across tables are **ULIDs** (26-character Base32-encoded strings), stored as `VARCHAR(26)`: Only for internal use i.e db transaction and relationships, etc.
- Additionally, a stable, immutable, non-nullable business key named `uid` (11-character long of random `chars and number`, system generated and verified, unique across the system), used externally, for client requests and queries.


* Note: currently Repeatable Sections: Nested, repeating data groups are stored as json in data template version but their instances ensures `_id` (ulid) is generated at client, `repeatIndex`, `parent_repeat_uid`.

## Compact Summary

Build a **robust, performant, and flexible analytics back-end components** that serves a production-grade pivot experience in the front end (Angular + pivot grid), by normalizing submission data into queryable facts, exposing rich metadata, validating & translating measure requests, and producing fast, correctly-typed aggregated results in multiple output formats for different consumers.

## 1.1 Platform / Build Assumptions

The current system is built upon:

*   **Java 17+ (Spring Boot 3.4.2)**: A Maven-based project, initially generated with JHipster and extended.
*   **PostgreSQL (tested with v16.x)**: Utilizes a compatible PostgreSQL JDBC driver.
*   **Liquibase (XML)**: Used for managing schema migrations.
*   **Spring Security & Application-level ACLs**: Integrated for security.
*   **`jOOQ` & `NamedParameterJdbcTemplate`/`JdbcTemplate`**: Available for analytical queries.
*   **Caching**: Employs Ehcache and Hibernate 2nd-level cache annotations where appropriate.
*   **Mapping and Codegen Tools**: Lombok and MapStruct are used.
*   **Testing**: Testcontainers (Postgres), JUnit 5, and AssertJ are used for testing.
*   **User authentication**: JWT, and Basic
*   **Client, web (Angular >= v20.0.0), and mobile (Flutter >= 3.35)**.

---

## Data Model, ETL, and Basic Analytics MVs (Schema View)

Overview of the core data entities, the Extract, Transform, Load (ETL) process, and the resulting analytical materialized views (MVs). The primary focus is on how data is structured and transformed to support efficient analytics and pivot queries.


### Core Source Table Attributes

This section outlines the core entities and their key attributes. For brevity, some properties and related services have been omitted. All entities have a primary key (`id`), a 26-character ULID that is immutable and guarantees uniqueness. They also have a unique business key (`uid`), an 11-character, system-generated identifier used extensively in analytics and MV queries. 

**Note:** Core source tables At Service have JPA entities, Jpa repositories, services, and rest end points listing, crud, fluent filtering, etc, ...).

1.  **`activity`**: Stores activities. Key attributes: `id`, `uid`, `name`.
2.  **`team`**: Stores teams. Key attributes: `id`, `uid`, `name`.
3.  **`org_unit`**: Stores organizational units. Key attributes: `id`, `uid`, `name`.
4.  **`assignment`**: Links `orgUnit`, `team`, and `activity` with other properties.
5.  **`option_set` & `option_value`**: Manages selectable options. `OptionSet` has attributes `id`, `uid`, `name`; `Option` has `id`, `uid`, `code`, `name`.
6.  **`data_element`**: Defines data fields. Attributes include `id`, `uid`, `name`, `valueType`, `is_measure`, `is_dimension`, and `aggregation_type`.
7.  **`element_template_config`**: A queryable catalog of fields per template. It holds attributes like `id`, `uid`, `template_uid`, `template_version_uid`, and `data_element_uid`.
8.  **`data_template`** & **`data_template_version`**: Defines data templates. `DataTemplate` has `id`, `uid`, `name`, and lists of field->data_element mapping config dto, and list of sections, each stored in a column in db as JSONB. Each field in a version is used to create or update an entry in `element_template_config`.
9.  **`data_submission`**: Contains the source data in a JSONB column (`formData`), which is the origin for all ETL processes. It links to `data_template`, `data_template_version`, `team`, `activity`, and `org_unit` via their UIDs.

***

### ETL Process Generated Facts (UID-Native)

The ETL process transforms the `DataSubmission.formData` JSONB into a "tall" fact table structure, primarily using UIDs. This results in the `element_data_value` and `repeat_instance` tables.

* **`element_template_config`** a physical table that mostly used by the query builder, projects some of the attributes of `data_element` and attributes inferred from the context for pivot-specific flags (value Type, is Dimension, is Measure, repeat Path, paths, is it a Category, etc),
* **`repeat_instance`** is a fact table that captures the hierarchical structure of repeated sections within a data submission. It includes columns for a unique ID, a parent ID for nested repeats, and UIDs for the submission and categories to which the repeat belongs.
* **`element_data_value`** stores the fact rows, with one row per data element value. It links to the submission, assignment, team, org unit, and activity UIDs. It also includes the data element UID, element template configuration UID, and option UID. Data values are stored in dedicated columns (`value_num`, `value_text`, etc.) based on their type, and generated columns (`repeat_instance_key`, `selection_key`) are used for creating unique indexes.

**ETL Facts Generation Flow (High level View):**
(`DataSubmission.formData` JSONB → (Spring ETL services and components) → `ElementDataValue` (tall fact rows) + `RepeatInstance`).

#### element_template_config ddl (in service has JPA entity, Jpa repository, and service):
```sql
CREATE TABLE element_template_config (
  id int8 NOT NULL,
  uid varchar(11) NOT NULL UNIQUE,
  template_uid varchar(11) NOT NULL,            -- DataTemplate.uid
  template_version_uid varchar(11) NOT NULL,    -- DataTemplateVersion.uid
  template_version_no int4 NOT NULL,            -- DataTemplateVersion.versionNumber
  data_element_uid varchar(11) NOT NULL,        -- DataElement.uid
-----------------------------
-- changing per Field in DataTemplateVersion.fields
-----------------------------   
  template_order int4,
  id_path text NOT NULL,                   
  name_path text NOT NULL,                 -- canonical path, e.g. household.children.age
  is_repeatable bool NOT NULL,             -- decided in service during data_template_version.field save/update
  repeat_path text,                        -- if field belongs to a repeat section
  is_category bool,
  category_for_repeat varchar(26),         -- element_id of category in same repeat, if any
  display_label jsonb,
  definition_json jsonb,
-----------------------------
-- projected in service based on the immutable data_element.value_type ()
-----------------------------
  is_reference bool NOT NULL,
  reference_table varchar(100),
-----------------------------
-- A Copy/or projection to the same DataElement's immutable properties (for easier reach for the planner)
-----------------------------
  is_multi bool NOT NULL,   -- multi-select
  option_set_uid varchar(11), 
  is_measure bool NOT NULL,
  is_dimension bool NOT NULL,
  created_at timestamp(6) NOT NULL,
  name text NOT NULL,
  value_type varchar(255) NOT NULL,             -- Text, Integer, Date, SelectOne, Team, OrgUnit, etc.
  aggregation_type varchar(32) NOT NULL
);

ALTER TABLE element_template_config ADD CONSTRAINT uk_element_template_config_uid UNIQUE (uid);
ALTER TABLE element_template_config ADD CONSTRAINT ux_element_template_config_tpl_ver_idpath UNIQUE (template_uid, template_version_uid, id_path);
```

#### Facts DDLs**:

**`repeat_instance`:**

```sql
-- etl result facts: 
-- repeat_instance table
CREATE TABLE IF NOT EXISTS repeat_instance (
    id varchar(26) PRIMARY KEY,
    parent_repeat_instance_id varchar(26),
    repeat_section_label jsonb DEFAULT '{}'::jsonb,
    submission_uid varchar(11) NOT NULL,
    category_uid varchar(11),
    category_kind varchar(200),
    category_name varchar(200),
    category_label jsonb DEFAULT '{}'::jsonb,
    repeat_path varchar(3000) NOT NULL,
    repeat_index bigint,
    client_updated_at timestamp,
    deleted_at timestamp,
    submission_completed_at timestamp,
    created_date timestamp NOT NULL DEFAULT now(),
    last_modified_date timestamp,
    last_modified_by varchar(100),
    created_by varchar(100)
);
CREATE INDEX IF NOT EXISTS idx_repeat_instance_submission_path ON repeat_instance(submission_uid, repeat_path);
    CREATE INDEX IF NOT EXISTS idx_repeat_instance_parent_id ON repeat_instance(parent_repeat_instance_id);
    ALTER TABLE repeat_instance ADD CONSTRAINT fk_repeat_instance_parent
        FOREIGN KEY(parent_repeat_instance_id) REFERENCES repeat_instance(id);
```

**`element_data_value` DDL:**
```sql
  --------------------------------
  -- etl result facts
  -- element_data_value table
  CREATE TABLE IF NOT EXISTS element_data_value (
      id bigserial PRIMARY KEY,
      repeat_instance_id varchar(26),
      submission_uid varchar(11) NOT NULL,
      assignment_uid varchar(11),
      team_uid varchar(11),
      org_unit_uid varchar(11),
      activity_uid varchar(11),
      element_uid varchar(11) NOT NULL,
      element_template_config_uid varchar(11) NOT NULL,
      option_uid varchar(11), -- only for multi select or null
      value_text text,
      value_num numeric,
      value_bool boolean,
      value_ref_uid varchar(11),
      value_ts timestamp,
      deleted_at timestamp,
      created_date timestamp NOT NULL DEFAULT now(),
      last_modified_date timestamp,
      repeat_instance_key text GENERATED ALWAYS AS (COALESCE(repeat_instance_id, '')) STORED,
      selection_key text GENERATED ALWAYS AS (COALESCE(option_uid, '')) STORED,
      row_type char(1) NOT NULL DEFAULT 'S'
  );
  CREATE UNIQUE INDEX IF NOT EXISTS ux_element_value_unique
      ON element_data_value (
      submission_uid,
      element_uid,
      repeat_instance_key,
      row_type,
      selection_key
  );

  -- other indexes omitted for brevity
```

### Materialized Views (MVs)

The `pivot_grid_facts` MV is a UID-native view optimized for analytics. It flattens normalized `element_data_value` rows with template and repeat hierarchy context, allowing the analytics stack to perform efficient grouping, filtering, and aggregation.

**`pivot_grid_facts` (MV) Postgres:**
* **Purpose:** This MV denormalizes key attributes from various source tables (`data_submission`, `data_element`, `element_template_config`, etc.) into a single, wide table. It joins `element_data_value` with `repeat_instance` and other dimension tables to include contextual information like `submission_uid`, `form_template_uid`, and `repeat_path`, among others. This structure is designed to minimize joins during analytical queries.

```sql
CREATE MATERIALIZED VIEW pivot_grid_facts AS SELECT
ev.ID AS value_id,
ev.submission_uid AS submission_uid,
-------------------------------------
-- (specific template filtering/grouping mode)
--------------------------------------
sub.form AS form_template_uid,-- (template mode filtering)
sub.form_version AS form_version_uid,
etc.uid AS etc_uid,-- (template mode filtering)
-- Template metadata (per-template overrides from element_template_config)
etc.repeat_path AS template_repeat_path,
etc.name_Path AS template_name_path,-- element Path built with element names (ends with name)
etc.id_Path AS template_id_path,-- element Path built with section names, (ends with element uid)
-------------------------------------
-- REPEAT CONTEXT
-- HIERARCHICAL_CONTEXT (specific template filtering/grouping mode)
--------------------------------------
child_ri.ID AS repeat_instance_id,--  ULID PK is used only for repeat instance, rest is uid-native
parent_ri.ID AS parent_repeat_instance_id,
-- Repeat hierarchy
child_ri.repeat_path,
child_ri.repeat_section_label,-- json e.g. {"en": "...", "ar": "..."}
parent_ri.repeat_section_label AS parent_repeat_section_label,
-------------------------------------
-- Submission / Assignment context
-- CORE_DIMENSION
--------------------------------------
ev.assignment_uid AS assignment_uid,
ev.team_uid AS team_uid,
tm.code AS team_code,
ev.org_unit_uid AS org_unit_uid,
ou.NAME AS org_unit_name,
ev.activity_uid AS activity_uid,
act.NAME AS activity_name,
sub.finished_entry_time AS submission_completed_at,
-------------------------------------
-- REPEAT INSTANCE canonical CATEGORY
-- HIERARCHICAL_CONTEXT (across templates filtering/grouping mode)
--------------------------------------
child_ri.category_uid AS child_category_uid,
parent_ri.category_uid AS parent_category_uid,
child_ri.category_kind AS child_category_kind,
parent_ri.category_kind AS parent_category_kind,
etc.display_label,-- json e.g. {"en": "...", "ar": "..."}
etc.category_for_repeat,-- the element pointed to in this row is part of (if configured)
-------------------------------------
-- data_element (canonical) (Global i.e across templates filtering/grouping mode)
--------------------------------------
-- Global data_element metadata (join)
de.uid AS de_uid,
de.NAME AS de_name,
de.TYPE AS de_value_type,
-- Option metadata (selects)
ops.uid AS de_option_set_uid,
ev.option_uid AS option_uid,
ov.uid AS option_value_uid,
ov.NAME AS option_name,
ov.code AS option_code,

--------------------------------------
-- Category names resolved live by UID + kind
--------------------------------------
CASE
  WHEN child_ri.category_kind = 'team'     THEN child_team.name
  WHEN child_ri.category_kind = 'org_unit' THEN child_ou.name
  WHEN child_ri.category_kind = 'activity' THEN child_activity.name
  WHEN child_ri.category_kind = 'option'   THEN child_opt.name
END AS child_category_name,

CASE
  WHEN parent_ri.category_kind = 'team'     THEN parent_team.name
  WHEN parent_ri.category_kind = 'org_unit' THEN parent_ou.name
  WHEN parent_ri.category_kind = 'activity' THEN parent_activity.name
  WHEN parent_ri.category_kind = 'option'   THEN parent_opt.name
END AS parent_category_name,

--------------------------------------
-- Measures
--------------------------------------
  ev.value_num,
  ev.value_text,
  ev.value_bool,
  ev.value_ts,
  ev.value_ref_uid,
  ev.deleted_at 
FROM
  element_data_value ev
  JOIN data_submission sub ON ev.submission_uid = sub.uid
LEFT JOIN data_element de ON ev.element_uid = de.uid
LEFT JOIN element_template_config etc ON ev.element_template_config_uid::text = etc.uid::text
LEFT JOIN option_value ov ON ev.option_uid = ov.uid
LEFT JOIN option_set ops ON de.option_set_id = ops.id
LEFT JOIN team tm ON ev.team_uid = tm.uid
LEFT JOIN org_unit ou ON ev.org_unit_uid = ou.uid
LEFT JOIN activity act ON ev.activity_uid = act.uid

LEFT JOIN repeat_instance child_ri ON ev.repeat_instance_id = child_ri.id
LEFT JOIN repeat_instance parent_ri ON child_ri.parent_repeat_instance_id = parent_ri.id

-- Live category joins (child)
LEFT JOIN team child_team ON child_ri.category_uid = child_team.uid AND child_ri.category_kind = 'team'
LEFT JOIN org_unit child_ou ON child_ri.category_uid = child_ou.uid AND child_ri.category_kind = 'org_unit'
LEFT JOIN activity child_activity ON child_ri.category_uid = child_activity.uid AND child_ri.category_kind = 'activity'
LEFT JOIN option_value child_opt ON child_ri.category_uid = child_opt.uid AND child_ri.category_kind = 'option'

-- Live category joins (parent)
LEFT JOIN team parent_team ON parent_ri.category_uid = parent_team.uid AND parent_ri.category_kind = 'team'
LEFT JOIN org_unit parent_ou ON parent_ri.category_uid = parent_ou.uid AND parent_ri.category_kind = 'org_unit'
LEFT JOIN activity parent_activity ON parent_ri.category_uid = parent_activity.uid AND parent_ri.category_kind = 'activity'
LEFT JOIN option_value parent_opt ON parent_ri.category_uid = parent_opt.uid AND parent_ri.category_kind = 'option';

-- ... some other indexes omitted for brevity
```

### How to Consume the Materialized View

#### 1. What Comes from `data_element` vs. `element_template_config`

* **Immutable/Canonical Attributes (`data_element`)**: Core, authoritative attributes that are globally consistent, such as `data_element.name`, `valueType`, `aggregation_type`, and `is_measure`/`is_dimension`, should be sourced from the **`data_element`** table.
* **Template-Specific Attributes (`element_template_config`)**: Attributes only meaningful within a specific template version should be sourced from the **`element_template_config`** table. Examples include `display_label`, `repeat_path`, `template_name_path`, and `is_category`.

#### 2. `valueType` Mapping

The `element_data_value` table uses different columns to store data based on its `valueType`, optimizing storage and retrieval.

* **Numerical values** (`Number`, `Integer`, etc.) are stored in `value_num`.
* **Boolean values** (`Boolean`, `TrueOnly`) are stored in `value_bool`.
* **Select-Multi** options are stored as separate rows, with each selected option's UID in the `option_uid` column.
* **Reference types** (`SelectOne`, `Activity`, etc.) are stored in `value_ref_uid`.
* **Date/Time** values are stored in `value_ts`.
* All other types are stored in `value_text`.

For querying repeat categories, the `category_uid` column is used for reference types that have been configured as categories, enabling fast hierarchical joins.

Mapping to jOOQ Fields, `PG` is `...jooq.Tables.PIVOT_GRID_FACTS` codegen class:

* `value_num` -> `PG.VALUE_NUM`;   (Field<BigDecimal>)
* `value_text` -> `PG.VALUE_TEXT;` (Field<String>)
* `value_bool` -> `PG.VALUE_BOOL`; (Field<Boolean>)
* `value_ts` -> `PG.VALUE_TS`;     (Field<LocalDateTime>)
* `value_ref_uid` -> `PG.VALUE_REF_UID`; (Field<String>)

// Option handling
* `option_uid` -> `PG.OPTION_UID`;  (Field<String>)
* `option_value_uid` -> `PG.OPTION_VALUE_UID`;  (Field<String>)
* `option_name` -> `PG.OPTION_NAME`;  (Field<String>)
* `option_code` -> `PG.OPTION_CODE`;  (Field<String>)

// De / element
* `de_uid` -> `PG.DE_UID`; (Field<String>)
* `de_name` -> `PG.DE_NAME`;
* `de_value_type` -> `PG.DE_VALUE_TYPE`;
* `de_option_set_uid` -> `PG.DE_OPTION_SET_UID`; (Field<String>)

// etc template uids
* `etc_uid` -> `PG.ETC_UID`; (Field<String>)
* `template_repeat_path` -> `PG.TEMPLATE_REPEAT_PATH`;
* `template_id_path` -> `PG.TEMPLATE_ID_PATH`;
* `template_name_path` -> `PG.TEMPLATE_NAME_PATH`;
* `display_label` -> `PG.DISPLAY_LABEL`;

// Context / Dimensions
* `team_uid` -> `PG.TEAM_UID`; (Field<String>)
* `team_code` -> `PG.TEAM_CODE`; (Field<String>)
* `org_unit_uid` -> `PG.ORG_UNIT_UID`; (Field<String>)
* `org_unit_name` -> `PG.ORG_UNIT_NAME`; (Field<String>)
* `activity_uid` -> `PG.ACTIVITY_UID`;
* `activity_name` -> `PG.ACTIVITY_NAME`;
* `assignment_uid` -> `PG.ASSIGNMENT_UID`;
* `submission_uid` -> `PG.SUBMISSION_UID`;

// Repeat / category uids
* `child_category_uid` -> `PG.CHILD_CATEGORY_UID`; (Field<String>)
* `parent_category_uid` -> `PG.PARENT_CATEGORY_UID`; (Field<String>)
* `child_category_kind` -> `PG.CHILD_CATEGORY_KIND`;
* `parent_category_kind` -> `PG.PARENT_CATEGORY_KIND`;

// Repeat instance ids (we keep ULIDs here)
* `repeat_instance_id` -> `PG.REPEAT_INSTANCE_ID`;
* `parent_repeat_instance_id` -> `PG.PARENT_REPEAT_INSTANCE_ID`;
* `repeat_path` -> `PG.REPEAT_PATH`;
* `repeat_section_label` -> `PG.REPEAT_SECTION_LABEL`;

// submission time (timestamp)
* `submission_completed_at` -> `PG.SUBMISSION_COMPLETED_AT`;

// fallback to other known columns present in MV
* `value_id` -> `PG.VALUE_ID`;
* `deleted_at` -> `PG.DELETED_AT`;

---

### Analytics flow
... omitted for brevity (see contracts and basic flow below)
#### Metadata services and components
... omitted for brevity (see contracts and basic flow below)
#### Query Builder, and Query services and components
... omitted for brevity (see contracts and basic flow below)
#### Analytics entry point (rest controllers)
... omitted for brevity (see contracts and basic flow below)

---

# Analytics API for Pivot & Aggregation Queries (A Front-end **developer-facing guide**)

This section explains how the frontend interacts with the analytics backend to render pivot tables, charts, and aggregated summaries. It complements the data model/ETL overview by describing the API contracts and request/response flows.

---

## 1. Metadata Discovery

Before a client can build a query, it needs to know what **fields/dimensions/measures** are available. This is exposed via metadata endpoints.

### Endpoint: Get template metadata

```
GET /api/v1/analytics/pivot/metadata?templateId={uid}&templateVersionId={uid}
```

**Response:**

```json
{
  "templateUid": "dt123abc456",
  "templateVersionUid": "dtv987zyx321",
  "fields": [
    {
      "uid": "etcAbc12345", 
      "dataElementUid": "deXYZ67890",
      "factColumn": "etc_uid", 
      "name": "Age of Household Head",
      "dataType": "NUMERIC",
      "aggregationModes": ["SUM","AVG","MIN","MAX","COUNT"],
      "isDimension": true,
      "isMeasure": true,
      "extras": {
        "optionSetUid": null,
        "isMulti": false,
        "referenceTable": null
      }
    },
    {
      "uid": "etcDef98765",
      "dataElementUid": "deLMN22222",
      "factColumn": "etc_uid", 
      "name": "Household Category",
      "dataType": "OPTION",
      "aggregationModes": ["COUNT","COUNT_DISTINCT"],
      "isDimension": true,
      "isMeasure": false,
      "extras": {
        "optionSetUid": "opsQWERTY11",
        "isMulti": false
      }
    }
  ],
  "coreDimensions": [
    { "factColumn": "team_uid", "name": "Team", "dataType": "UID" },
    { "factColumn": "org_unit_uid", "name": "Org Unit", "dataType": "UID" },
    { "factColumn": "activity_uid", "name": "Activity", "dataType": "UID" },
    { "factColumn": "submission_completed_at", "name": "Submission Date", "dataType": "TIMESTAMP" }
  ]
}
```

👉 **Frontend usage:**

* Populate dropdowns for measures and dimensions.
* Constrain aggregation operators by field `dataType` and `aggregationModes`.
* Show option values via `/api/v1/option-sets/{uid}/values`.

---

## 2. Pivot Query Execution

Once the user selects measures, dimensions, filters, etc., the frontend posts a query request.

### Endpoint: Execute pivot query

```
POST /api/v1/analytics/pivot/query?format=TABLE_ROWS
POST /api/v1/analytics/pivot/query?format=PIVOT_MATRIX
```

**Request (example for TABLE_ROWS):**

```json
{
  "templateId": "dt123abc456",
  "templateVersionId": "dtv987zyx321",
  "dimensions": ["team_uid", "org_unit_uid", "org_unit_name"],
  "measures": [
    { "elementIdOrUid": "etc:etcAbc12345", "aggregation": "SUM", "alias": "total_age" },
    { "elementIdOrUid": "etc:etcDef98765", "aggregation": "COUNT", "alias": "household_count" }
  ],
  "filters": [
    { "field": "submission_completed_at", "op": ">=", "value": "2025-01-01T00:00:00Z" },
    { "field": "team_uid", "op": "IN", "value": ["tm12345abc", "tm67890xyz"] }
  ],
  "sorts": [
    { "field": "total_age", "direction": "DESC" }
  ],
  "limit": 50,
  "offset": 0,
  "autoRenameAliases": false
}
```

**Response (TABLE\_ROWS):**

```json
{
  "columns": [
    { "id": "team_uid", "label": "Team", "dataType": "UID" },
    { "id": "org_unit_uid", "label": "Org Unit UID", "dataType": "UID" },
    { "id": "org_unit_name", "label": "Org Unit Name", "dataType": "TEXT" },
    { "id": "total_age", "label": "Total Age", "dataType": "NUMERIC" },
    { "id": "household_count", "label": "Households", "dataType": "NUMERIC" }
  ],
  "rows": [
    { "team_uid": "tm12345abc", "org_unit_uid": "ou77777", "org_unit_name": "Org Unit X1", "total_age": 245, "household_count": 12 },
    { "team_uid": "tm67890xyz", "org_unit_uid": "ou88888", "org_unit_name": "Org Unit X2", "total_age": 300, "household_count": 15 }
  ],
  "total": 2
}
```

**Request (PIVOT\_MATRIX):**
```json
{
  "templateId":"Tcf3Ks9ZRpB",
  "templateVersionId":"fb2GC7FInSu",
	"autoRenameAliases": true,
	"rowDimensions": ["team_uid", "team_code"],
	"columnDimensions": ["activity_name"],
  "measures": [
    { "elementIdOrUid": "etc:CiEZemZ7mlg", "aggregation": "SUM", "alias": "total" },
    { "elementIdOrUid": "etc:CiEZemZ7mlg", "aggregation": "COUNT", "alias": "total" }
  ]
}
```

**Response (PIVOT\_MATRIX):**
```json lines
{
    "total": 61,
    "meta": { "format": "PIVOT_MATRIX", "templateId": "Tcf3Ks9ZRpB", "templateVersionId": "fb2GC7FInSu"},
    "matrix": {
        "rowDimensionNames": ["team_uid", "team_code"],
        "columnDimensionNames": ["activity_name"],
        "measureAliases": ["total", "total_1"],
        "rowHeaders": [ [ "A0zx1Phnfua", "442" ], ["a1fXKhbyq7q", "57" ] ],
        "columnHeaders": [
            [ "ITNs Distribution 2022"],
            [ "ITNs Distribution 2023" ]
        ],
        "cells": [
            [{"total": 200, "total_1": 2}, {"total": 321, "total_1": 14}]
        ]
    }
}
```

👉 **Frontend usage:**

* For `TABLE_ROWS`: feed directly into grid components (`ag-Grid`, `igniteui-angular`).
* For `PIVOT_MATRIX`: map to pivot grid or charting components with explicit row/column headers.

---

## 3. Auxiliary Endpoints (already assumed in your system)

* **Option values lookup**

  ```
  GET /api/v1/option-sets/{uid}/values
  ```

  → returns list of `{uid, code, name, sortOrder}`.

* **OrgUnit hierarchy**

  ```
  GET /api/v1/org-units/tree?root={uid}
  ```

  → returns hierarchical tree for filters.

* **Teams, Activities, Projects**

  ```
  GET /api/v1/teams?filter=...
  GET /api/v1/activities?filter=...
  GET /api/v1/projects?filter=...
  ```

  → used to populate dropdowns in filter builders.

* **Data element lookup (global)**

  ```
  GET /api/v1/data-elements/{uid}
  ```

  → metadata when building global (cross-template) queries using `de:<uid>`.

---

## 4. Typical Frontend User Flow

1. **Select Template & Version** → fetch metadata.
2. **Pick Measures & Dimensions** from metadata.
3. **Build Filters** using option pickers, date ranges, or entity selectors.
4. **Configure Sorting & Pagination.**
5. **Choose Output Format** (table vs pivot).
6. **Run Query** → POST to pivot endpoint.
7. **Render Results** in grid or pivot component.
8. **Optional Drilldown**: click cell → re-run query with extra filter.
9. **Save / Export**: save request JSON as “view” or export result (CSV/Excel).

---

### Analysis of Missing Information

a front-end developer would still have questions about the *how* in a few key areas:

1.  **UID-to-Name Resolution:** The UI needs to display human-readable names (e.g., "Team Alpha") instead of UIDs (e.g., `tm12345abc`). While the `pivot_grid_facts` view pre-joins some names (`org_unit_name`, `activity_name`), this doesn't cover all cases, especially for dimensions selected by the user. A mechanism for resolving any given UID to its corresponding name is needed to avoid showing raw identifiers in tables and filters.
2.  **Standardized Error Handling:** The UX document mentions specific error messages, but there's no defined API contract for how errors are returned. A front-end developer needs a predictable error structure to parse and display messages correctly.
3.  **API Preliminaries:** Basic information like the API base URL, versioning, and, most importantly, the authentication scheme (e.g., JWT Bearer token) is assumed but not explicitly stated for the developer.
4.  **Concrete Drill-Down Implementation:** The concept of a "drill-down" is explained, but a concrete example showing the transformation from a clicked table row into a *new* `PivotQueryRequest` would be invaluable.
5.  **Handling Hierarchical Data in Filters:** The document mentions an `org-units/tree` endpoint, but doesn't explicitly connect it to the filter-building process in the pivot UI.

To address these points, below is an enhancement for the "Analytics API for Pivot & Aggregation Queries" section above.

***

### Enhanced: Analytics API for Pivot & Aggregation Queries (A Complete Front-end Developer's Guide)

This guide provides front-end engineers with all the necessary information to build a rich, interactive pivot table experience using the Datarun analytics API. It is designed to be used with modern web frameworks like Angular and UI component libraries such as `ag-Grid`.

---

### 1. Getting Started: API Fundamentals

Before making any calls, please be aware of the following:

*   **Authentication**: All requests to the `/api/v1/analytics/` endpoints must be authenticated. Include an `Authorization` header with a valid JWT token.
    ```
    Authorization: Bearer <your_jwt_token>
    ```
*   **Base URL**: All API paths in this guide are relative to your deployed instance's base URL (e.g., `https://your-datarun-instance.com`).
*   **Identifiers**: The API exclusively uses the 11-character `uid` for all entities in requests and responses. The internal 26-character ULID is never exposed to the client.

---

### 2. The Core Workflow: A Three-Step Process

Building a pivot table follows a simple, stateful flow:

1.  **Discover**: Fetch metadata for a selected form template. This tells the UI what fields, dimensions, and operations are available.
2.  **Query**: Construct and execute a query based on user selections. The backend processes the request and returns a structured data set.
3.  **Render & Interact**: Display the data. Handle interactions like sorting, pagination, or drilling down by re-querying with modified parameters.

---

### 3. Step 1: Metadata Discovery

To build the UI controls for the pivot table, you first need to know what data is available for a specific form.

#### **Endpoint: Get Template Metadata**

This is the starting point of the entire process. Call this endpoint whenever the user selects a form template and version.

```
GET /api/v1/analytics/pivot/metadata?templateId={uid}&templateVersionId={uid}
```

**Response Payload (`PivotMetadataResponse`)**

The response contains two primary lists: `fields` (data collected in the form) and `coreDimensions` (system-level data like team, org unit, etc.).

```json
{
  "templateUid": "dt123abc456",
  "templateVersionUid": "dtv987zyx321",
  "fields": [
    {
      "uid": "etcAbc12345", 
      "dataElementUid": "deXYZ67890",
      "factColumn": "etc_uid", 
      "name": "Age of Household Head",
      "dataType": "NUMERIC",
      "aggregationModes": ["SUM","AVG","MIN","MAX","COUNT"],
      "isDimension": true,
      "isMeasure": true,
      "extras": {
        "optionSetUid": null,
        "isMulti": false,
        "referenceTable": null
      }
    }
  ],
  "coreDimensions": [
    { "factColumn": "team_uid", "name": "Team", "dataType": "UID" },
    { "factColumn": "org_unit_uid", "name": "Org Unit", "dataType": "UID" },
    { "factColumn": "submission_completed_at", "name": "Submission Date", "dataType": "TIMESTAMP" }
  ]
}
```

**How the Frontend Uses This Metadata:**

*   **`fields` Array**: Use this to populate the "Measures" and template-specific "Dimensions" pickers.
    *   `name`: The display label for the field in the UI.
    *   `dataType`: Drives which filter operators (`=`, `>`, `IN`) and input controls (date picker, number input, dropdown) to show.
    *   `aggregationModes`: The list of allowed aggregations for a measure. Disable or hide any unsupported options.
    *   `uid`: The identifier for this template field. When creating a `MeasureRequest`, prefix this with `etc:` (e.g., `"elementIdOrUid": "etc:etcAbc12345"`).
    *   `extras.optionSetUid`: If present, use the `/api/v1/option-sets/{uid}/values` endpoint to fetch the available options for dropdowns.
*   **`coreDimensions` Array**: Use this to populate the "Dimensions" picker for system-level groupings.
    *   `factColumn`: The identifier to be used in the `dimensions`, `rowDimensions`, `columnDimensions`, and `filters` arrays of your query.
    *   `name`: The display label for the dimension.

---

### 4. Step 2: Pivot Query Execution

Once the user has configured their report, you will construct and send a `PivotQueryRequest`.

#### **Endpoint: Execute Pivot Query**

```
POST /api/v1/analytics/pivot/query?format={TABLE_ROWS | PIVOT_MATRIX}
```

**Request Body (`PivotQueryRequest`)**

This is the main object you will build from the UI state.

```json
{
  "templateId": "dt123abc456",
  "templateVersionId": "dtv987zyx321",
  "dimensions": ["team_uid"],
  "measures": [
    { "elementIdOrUid": "etc:etcAbc12345", "aggregation": "SUM", "alias": "total_age" }
  ],
  "filters": [
    { "field": "submission_completed_at", "op": ">=", "value": "2025-01-01T00:00:00Z" }
  ],
  "sorts": [ { "fieldOrAlias": "total_age", "desc": true } ],
  "limit": 50,
  "offset": 0
}```

**Response (`format=TABLE_ROWS`)**

This format is ideal for standard data grids.

```json
{
  "columns": [
    { "id": "team_uid", "label": "Team", "dataType": "UID" },
    { "id": "total_age", "label": "Total Age", "dataType": "NUMERIC" }
  ],
  "rows": [
    { "team_uid": "tm12345abc", "total_age": 2450 },
    { "team_uid": "tm67890xyz", "total_age": 3123 }
  ],
  "total": 24 
}
```

*   **Frontend Action**:
    1.  Map the `columns` array to your grid's column definitions. The `id` field corresponds to the key in each object in the `rows` array.
    2.  Use the `rows` array as the data source for the grid.
    3.  Use `total`, `limit`, and `offset` to configure your pagination controls. Re-query with a new `offset` when the user changes pages.

---

### 5. Step 3: Interactions and Follow-up Actions

A static table is good, but an interactive one is better. Here’s how to handle common user actions.

#### **Sorting and Pagination**

When a user clicks a column header to sort or navigates to a new page:

1.  Modify the `sorts` or `offset` properties in your stored `PivotQueryRequest` object.
2.  Resubmit the request to the `POST /query` endpoint.
3.  Update the grid with the new response.

#### **Drill-Down: From Aggregation to Detail**

When a user clicks a cell or row, they often want to see the underlying data that produced that result.

**Example Scenario:** The table shows `team_uid: "tm12345abc"` has a `total_age: 2450`. The user clicks this row to investigate.

1.  **Get Context from the Clicked Row**: The data for the clicked row provides the context for the drill-down. In this case, the context is `team_uid = "tm12345abc"`.
2.  **Construct a New Query**: Create a new `PivotQueryRequest` by cloning the original one and adding the context as a new filter. You might also want to change the dimensions to see more detail.

    *   **Original Request:**
        ```json
        {
          "templateId": "dt123abc456",
          "dimensions": ["team_uid"],
          "measures": [{"elementIdOrUid": "etc:etcAbc12345", "aggregation": "SUM", "alias": "total_age"}]
        }
        ```
    *   **New Drill-Down Request:** (Notice the added filter and new dimension)
        ```json
        {
          "templateId": "dt123abc456",
          "dimensions": ["team_uid", "submission_uid"], // Show individual submissions
          "measures": [{"elementIdOrUid": "etc:etcAbc12345", "aggregation": "SUM", "alias": "total_age"}],
          "filters": [
            { "field": "team_uid", "op": "=", "value": "tm12345abc" } // The new drill-down filter
          ]
        }
        ```
3.  **Execute and Render**: Execute this new query and display the results in a new tab, a modal, or by replacing the current view.

---

### 6. Auxiliary Endpoints and UI Helpers

#### **Resolving UIDs to Readable Names**

Your grid will often receive UIDs (e.g., `team_uid`, `org_unit_uid`). To display friendly names, use this efficient batch endpoint.

**Endpoint: Batch Resolve UIDs**

```
POST /api/v1/resolve-uids
```

**Request Body**
```json
{
  "uids": ["tm12345abc", "ou77777", "tm67890xyz"]
}
```

**Response**
```json
{
  "tm12345abc": { "uid": "tm12345abc", "name": "Team Alpha", "type": "Team" },
  "ou77777": { "uid": "ou77777", "name": "District West", "type": "OrgUnit" },
  "tm67890xyz": { "uid": "tm67890xyz", "name": "Team Bravo", "type": "Team" }
}
```
*   **Frontend Action**: After receiving a `TABLE_ROWS` response, collect all unique UIDs from the result set. Make a single call to this endpoint and use the returned map to display the names in your grid, for example by using an `ag-Grid` value formatter.

#### **Building a Hierarchy Filter (e.g., for Org Units)**

To let users select from a tree of organizational units:
1.  Fetch the hierarchy using `GET /api/v1/org-units/tree`.
2.  Render this data using a tree component in your UI.
3.  When the user selects one or more org units, collect their UIDs.
4.  Construct a filter in your `PivotQueryRequest`:
    ```json
    { "field": "org_unit_uid", "op": "IN", "value": ["ouChild123", "ouChild456"] }
    ```

---

### 7. Handling Errors

The API returns errors in a standardized format. Your HTTP client interceptor should be configured to handle `4xx` and `5xx` status codes and parse this body.

**Example Error Response (`400 Bad Request`)**
```json
{
  "timestamp": "2025-09-01T22:00:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "The specified aggregation is not supported for the given field.",
  "details": [
    {
      "field": "measures[0].aggregation",
      "value": "SUM",
      "issue": "Aggregation 'SUM' is not allowed on data type 'OPTION'. Allowed aggregations are: [COUNT, COUNT_DISTINCT]."
    }
  ]
}
```
*   **Frontend Action**:
    1.  Display the top-level `message` to the user in a notification.
    2.  If the `details` array is present, iterate through it to highlight the specific UI controls that are causing the validation error (e.g., put a red border around the invalid measure configuration).
