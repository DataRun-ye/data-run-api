## Current system overview

The current system is built upon:

* **Java 17+ (Spring Boot 3.4.2)**: A Maven-based project.
* **PostgresSQL (tested with v16.x)**: Utilizes a compatible PostgresSQL JDBC driver.
* **Liquibase (XML)**: Used for managing schema migrations.
* **`NamedParameterJdbcTemplate`/`JdbcTemplate`**: Available for analytical queries.
* **Mapping and Codegen Tools**: Lombok (preferred for compactness and brevity).
* **id**: internal primary key (VARCHAR(26)) ULID format. Immutable, never recycled. Used for foreign-key
  relationships.
* **uid**: short system generated business key (VARCHAR(11)), globally unique, stable across environments, used extensively for external referencing, like in api client's requests and analytics for human-friendly references.

---

### Current System Architecture overview

Expressed via [Mermaid diagram](https://github.com/DataRun-ye/data-run-api/blob/main/README.md):

- **1. Canonical Dimension Tables**: Core reference data (elements, org units, activities, teams, etc.)
- **2. Configuration & Staging**: DataTemplates and versions `TemplateVersion` for custom dataset configs
- **3. Template Configuration**: Field config mapping via TemplateElement and CanonicalElement
- **4. Operational Data**: Submission entities tracking data collection
- **5. : Simple Pivot tables/materialized views for reporting: Pivoting data submission per template, per user requests (load and replace strategy).

### Current running core entities we will enhance (short)

* `org_unit` (id ULID, uid, code, name, path, level, created_date, parent_id ...)
* `org_unit_group` (id ULID, uid, code, name, created_date, ...)
* `org_unit_group_members` (group_id, org_unit_id)
* `org_unit_groupset` (id ULID, uid, code, name, created_date, ...)
* `org_unit_groupset_org_unit_group` (groupset_id, org_unit_group_id)
* `team` (id ULID, uid, code, name, description, form_permissions, created_date, ...)
* `team_user` (team_id, user_id)
* `team_managed_teams` (team_id, managed_team_id)
* `user` (id, uid, code, login, firstname, created_date, ...)
* `user_group` (id, uid, code, name, created_date, ...)
* `user_group_users` (group_id, user_id)
* `option_set` (id, uid, code, name, created_date, ...)
* `option_value` (id, uid, code, name, option_set_id, option_set_uid, created_date, ...)
* `data_template` (id, uid, code, name, fields (`id`, `name`, `parent` section, `type`, etc), sections (id, name,  parent) (normal and repeatable), created_date, ...)
* `assignment` (id, uid, team_id, activity_id, org_unit_id, forms (i.e templates, a jsonb array of  template uids), created_date, ...).
* `DataSubmission` (id, uid, template_uid, template_version_uid, serial_number, deleted_at, form_data (JSONB payload), start_entry_time (client's opening form time), finished_entry_time (client's complete form time), assignment_uid, and denormalized from assignment: activity_uid, team_uid, and org_unit_uid, created_date (server timestamp)).
* `TemplateElement`(id, uid, template_uid, template_version_uid, )

### Forms sachems and DataSubmission
Here’s the same **tight structure + mental model** for your new classes:

#### **DataTemplate (root definition / lifecycle owner)**

- **Identity:** `uid`, `code`, `name`
- **Version tracking:** `versionUid`, `versionNumber`
- **Lifecycle:**  `deletedAt`
- **Metadata:** `description`
    
👉 **Core idea:** _The container and lifecycle owner of a form/template across versions._

---

#### **TemplateVersion (versioned structure snapshot)**

- **Identity:** `uid`, `versionNumber`, `templateUid`
- **Structure:** `fields`, `sections`
- **Options/config:** `options`
- **Context:** `dataTemplate`

👉 **Currently:** Elements structural tree are materialized through parent, each element (field/section) has `parent` property pointing to the parent sections, composing the tree is done at the client.

---

#### **DataSubmission (runtime data instance)**

- **Identity:** `uid`, `serialNumber`
- **Data payload:** `formData`
- **Template reference:** `form (templateUid)`, `formVersion`, `version`
- **Status:** `status`
- **Context:** `team`, `orgUnit`, `activity`, `assignment`
- **Timing:** `startEntryTime`, `finishedEntryTime`
- **Lifecycle:** `deletedAt`
- **Concurrency:** `lockVersion`
    

---

#### **Final compressed mental model (all together)**

- **DataTemplate:** _“The form as a long-lived entity.”_
- **TemplateVersion:** _“What the form looked like at a point in time.”_
- **DataSubmission:** _“Data filled into that form at that time.”_
    
👉 **End-to-end:**  
_“Template defines → Version shapes → Submission captures.”_

---

#### DataTemplate Access config:

Through `team.form_permissions`:

```json-
[{"form":"MI8KQFsxGFc","permissions":["ADD_SUBMISSIONS"]}]
````

---

### Functional Capabilities

each part is opened for suggestion of refactored or fully redesigning

* Manages templates and versions them 
* Manages users, user groups, and teams for specific data collection tasks
* Has an `activity` table describing collection contexts (e.g., Case Registration, Malaria unit flows, Mass distribution 2024, etc.)
* Implements organizational hierarchies (`org_unit`, `org_unit_group`, `org_unit_group_set`) similar in structure to DHIS2, but it is not DHIS2
* Supports `assignment`, linking metadata (activity, team, org_unit, period, etc.)
* Uses teams as principals for configuring template/assignment access (this can be improved but reflects the current state)

---

### Architectural Capability Score

Measured purely as a **general-purpose data collection architecture**, independent of any specific domain.

Note: Datarun is a solo-built system. These scores reflect current capability and are evolving as features mature.

| Capability                      | Score |
| ------------------------------- | ----- |
| Structured Reporting            | 9/10  |
| Hierarchical Governance         | 9/10  |
| Campaign Control                | 9/10  |
| Inventory Modeling              | 8/10  |
| Longitudinal Tracking           | 3/10  |
| Peer-to-Peer Workflows          | 4/10  |
| Case Management                 | 2/10  |
| Graph Relationships             | 3/10  |
| Ad-hoc / Exploratory Collection | 5/10  |

---

### difficulties in the existing model

The current system relies on implicit relationships between activities, assignments, teams, org units, and templates.
The system is using the same relational model to _write_ configuration (admins assigning teams) and to _read_ configuration (mobile apps downloading what they are allowed to see).

* Right now, "Assignments" and "Form Submissions" sit next to each other in logic, sharing services or bleeding DTOs.
* If I try to rewrite the entire mobile sync engine overnight, you will break the business.
While this works for basic scenarios, it introduces several practical difficulties:
* **A form referenced entities are implicit.** if a form defines a reference to system canonical entity the user can select in a form (org units, teams, users, options), it is derived
  indirectly from team membership, assignments, and org-unit relations rather than being explicitly modeled, and still limited to those that have a path to be identified in this way.
* **Configuration logic is scattered.** Access rules and filtering logic live across multiple places (assignment, team,
  org unit hierarchy, template design), making it hard to reason about or explain to administrators.
* **Limited support for complex flows.** Scenarios such as multi-party transactions, conditional source/destination
  selection, cross-team or cross-org flows, and curated lists require custom logic instead of configuration.
* **Tight coupling between planning and execution.** Assignments are designed around teams and org units upfront, which
  makes it difficult to adapt when users participate in multiple contexts or roles.
* **Hard to evolve without side effects.** Small changes in assignments, team membership, or org-unit structure can
  unintentionally affect what users see or can submit.
* **Offline sync assumes a fixed context.** The mobile client syncs configuration per team/assignment, which becomes  inefficient or ambiguous for users with many assignments or overlapping scopes.

These issues make the system harder to extend, reason about, and safely configure as requirements evolve.

---

## Future Vision for the platform

### What I want (high level — the product intent)

* A single platform owned by our organization that can deliver both:

  1. a stable set of **core tools** that cover ~70–80% of common needs across services, and
  2. **complete experiences** for specific domains or capabilities built on top of those core tools (overlays or modules).
* The platform must enable **incremental, evidence-driven evolution**: start small, learn from real usage, and extend the platform in controlled ways.
* The platform must keep **data integrity, identity stability, and traceability** as first principles across evolution.
* Ownership remains internal: eventual orchestration, low-code UIs, and advanced domain features are part of the platform vision — not outsourced or delegated to external products by default.

---

## Current Direction

The goal now is to re-design or refactore the system to be highly maintainable, following best practices and best battle tested architecture strategies, so the future vision could be easily materialized.
