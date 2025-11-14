let's pause for a second man, now that's everything is mostly working, let's summarize what we have, some you might
already know, and some might be new info for you:

## **we have proxmox server (current setup, some i have changed from what you previously known)**:

- iomete on a cluster: `k8s-control` (2 CPUs, 5.00 GiB Mem, 270 GiB boot disk), and
  `k8s-worker-1` [8 CPUs, 30.00 GiB Mem, scsi0: 100 GiB, scsi1 (shuffle): 200 GiB].
- postgresql on vm 210: `192.168.1.210`, [2 CPUs, 4.00 GiB Mem, 100 GiB boot disk].
- minio on vm 211: `192.168.1.211`, [2 CPUs, 4.00 GiB Mem, 300 GiB boot disk].
- integrate vm 212: `192.168.1.212`, [4 CPUs, 15 GiB Mem, 300.00 boot disk], containing `Airbyte` installed using
  `abctl local install --insecure-cookies --low-resource-mode`, metabase (on docker).

---

## Datarun Cloud Backend details:

We have datarun backend and its postgresql db instance at the cloud, here are some details about it, so we can be on the
same page of what's we already having, which could help us reach our final goals:

### 1. Platform / Build dependencies

* **Java 17+ (Spring Boot 3.4.2)**: A Maven-based project.
* **PostgresSQL (tested with v16.x)**: Utilizes a compatible PostgresSQL JDBC driver.
* **Liquibase (XML)**: Used for managing schema migrations.
* **`jOOQ` & `NamedParameterJdbcTemplate`/`JdbcTemplate`**: Available for analytical queries.
* **Caching**: Employs Ehcache and Hibernate 2nd-level cache annotations where appropriate.
* **Mapping and Codegen Tools**: Lombok (preferred for compactness and brevity) and MapStruct are used.
* **Testing**: Testcontainers (Postgres), JUnit 5, and AssertJ are used for testing.
* **User authentication**:  sending basic user's credentials and receiving Access/Refresh tokens.
* **Spring Security & Application-level ACLs**: Integrated for security.

### 2. Some of datarun entities and operations:

Note: each entity have it's own db table, and in service entity, service, repository and its rest crud api end point.

### 3. Foundational Design Principles

1. **IDs, UIDs and business keys**
    * **`id`**: internal primary key (`VARCHAR(26)`) ULID format. Immutable, never recycled. Used for all foreign-key
      relationships.
    * **`uid`**: short system generated business key (`VARCHAR(11)`), globally unique, stable across environments, used
      extensively in api client's requests and analytics for human-friendly references.

### 4. Immutability as the Bedrock of Integrity

**Principle:** Critical entities are immutable once published to prevent canonical drift

- **DataTemplateVersion:** Schema is locked upon publication.
- **DataSubmission context:** `template_uid` and `template_version_uid` are immutable after creation

### 5. Entities mostly related to our downstream processing and analytics:**

Include: (`DataTemplate` (dt), `DataTemplateVersion` (dtv), `DataSubmission` (ds), `OrgUnit` (ou), `OuLevel` (names of
the orgUnit
hierarchies levels), `OrgUnitGroup` (oug), `OrgUnitGroupSet` (ougs), `Team`, `Activity` (act), `Assignment` (asnmnt),
`User`, `OptionSet` (ops), `Option` (opv),
`OptionSetGroup`).

* including flat `template_element` (te) and `canonical_element` (ce) all created, generated and managed by the backend.
* template's element's (te) naming have rules checked at application before persisting a template
* element name is sensitized before saving, and is unique in the level it exist in, there are two levels types:
  submission level (i.e root), and repeat instance
  level, the system also allow data_templates with nested repeats, each repeat instance is a level.
* `data_submission`: ds also store multiple metadata properties from the collector env along the `formData` jsonb
  snapshot. such as `orgUnit`, `activity`,`team`, `user`,`time`,
  `template_uid`, `template_version_uid`, `created_date` (created at server), `last_modified_date`, `start_entry_time` (
  at client open form time), `finish_entry_time` (at client form marked complete).

```json lines
// [Checkout samples file of a data template]
```

#### **a data template push operation (create/update):**

1. after passing validation, the template is stored as a new DataTemplateVersion in db table
   `data_template_verison`, snapshotting its `fields` and `sections` in same table as jsonb.
2. optionSets and optionValue, are two canonical entities in the system, with stable globally unique business `uid`.
3. normal sections i.e `repeatable=false`, are just ui and doesn't hold a semantical value.
4. `fields` and `sections` are stored as flat lists, materialized hierarchically through each element's `path`,
   paths are determined by the system from the field to section `parent` property, and populated during a template
   version update/create.
5. fields and sections that are `repeatable=true` are flattened into `template_element`, and `canonical_element`.
6. **`template_element` and `canonical_element`** samples:
   `template_element` is the different versions of an element, and `canonical_element` is the canonical
   representation of an element across different versions (checkout samples files):
    * a flattened representation of a template (fields + repeatable sections), turning each into a TemplateElement (and
      a corresponding CanonicalElement), and persisting those metadata rows into PostgreSQL so later ETL / Spark / BI
      can rely on stable canonical IDs and the “tall table” mapping.
    * `jsonDataPath` (db: `json_data_path`): Path built with element names (ends with name). mirroring the path of
      an element's value in: `DataSubmission.formData`.
    * `canonicalPath`: canonically represent a grain of data (name of element if submission-level). e.g. The full
      `repeat_path` (e.g., `root.householdinfo.children`) mixes two different concepts:
        1. **Structural Grouping:** The `householdinfo` part is just a visual grouping (i.e non repeatable `section`
           name) on the form. An admin could rename it to `household_details` tomorrow, and it would mean the exact
           same thing to the user.
        2. **Data Grain:** The `children` part, because it is `repeatable: true`, defines a fundamental change in
           the data's structure. It means "one or more children related to one parent submission." This is the true
           canonical grain.
    * deterministic canonical IDs/fingerprints, canonical_element_uid a deterministic `UUID` from (canonicalPath,
      dataType, semanticType, optionSetUid, cardinality).
      **`template_element` and `canonical_element`** samples of two of the above forms, exported from the system db:

```json lines
// [Checkout samples file of a template element, and canonical element]
```

#### Some of the Operations at the backend:

* when a submission arrives/created/updated to the backend:
    * an `outbox_event`: when a submission arrives/created/updated and validated, in same transaction, it gets persisted
      into `data_submission`
      and an outbox event is persisted into `outbox_event` table. below is an `outbox_event` sample records showing its
      schema on cloud pg instance:

      | id    | aggregate_type | aggregate_id               | event_type         | payload                                                                  | status  | attempts | last_error | created_at | available_at | processing_owner | processing_started_at | processed_at |
                |-------|----------------|----------------------------|--------------------|--------------------------------------------------------------------------|---------|----------|------------|------------|--------------|------------------|-----------------------|--------------|
      | 15653 | DataSubmission | 01K6EGW4X4ZRCFM2GYA5KN28S0 | submission.saved   | {"submissionId":   "01K6EGW4X4ZRCFM2GYA5KN28S0", "submissionVersion": 0} | PENDING | 0        |            | 56:09.5    | 56:09.5      |                  |                       |              |
      | 15405 | DataSubmission | 01K6781VXDMB8H4W97ABSFBE3J | submission.updated | {"submissionId":   "01K6781VXDMB8H4W97ABSFBE3J", "submissionVersion": 1} | PENDING | 0        |            | 07:44.5    | 07:44.5      |                  |                       |              |

### **other canonical entities in the system at the cloud instance**:

* `OptionSet` entity: db table name: `db.option_set`.
* `Option` entity: table name: `db.option_value`
* `User`: `db.app_user`.
* `OrgUnit`: `db.org_unit`
* `Team`: db.team.
* `Activity`: db.activity.
* `Assignment`: db.assignment, assignment is a relation linking: `orgUnit`, `team`, `activity`,
  `list of data templates` to submit progress, and other attributes like: `day`, `status`, `allocatedResource`, etc.
* **Schemas of option_set, option_values, org_unit:**
    ```json lines
    // [Checkout `canonical_entities_sample.md` samples file]
    ```

