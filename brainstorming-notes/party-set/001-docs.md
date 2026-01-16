# Clean, minimal canonical spec (built *on top of my existing system*)

## Assumptions

### Dev Stack and Platform / Build dependencies

The current system is built upon:

* **Java 17+ (Spring Boot 3.4.2)**: A Maven-based project.
* **PostgresSQL (tested with v16.x)**: Utilizes a compatible PostgresSQL JDBC driver.
* **Liquibase (XML)**: Used for managing schema migrations.
* **`NamedParameterJdbcTemplate`/`JdbcTemplate`**: Available for analytical queries.
* **Caching**: Employs Ehcache and Hibernate 2nd-level cache annotations where appropriate.
* **Mapping and Codegen Tools**: Lombok (preferred for compactness and brevity) and MapStruct are used.

* **id**: internal primary key (VARCHAR(26)) ULID format. Immutable, never recycled. Used for foreign-key
  relationships.
* **uid**: short system generated business key (VARCHAR(11)), globally unique, stable across environments, used
  extensively in api client's requests and analytics for human-friendly references.

## 1. Current running entities we will enhance (short)

* `org_unit` (id ULID, code, name, path, level, created_date, ...)
* `org_unit_group` (id ULID, code, name, created_date, ...)
* `org_unit_group_members` (group_id, org_unit_id)
* `org_unit_groupset` (id ULID, uid, code, name, created_date, ...)
* `org_unit_groupset_org_unit_group` (groupset_id, org_unit_group_id)
* `team` (id ULID, uid, code, name, description, form_permissions, created_date, ...)
* `team_user` (team_id, user_id)
* `team_managed_teams` (team_id, managed_team_id)
* `user` (id, uid, code, login, firstname, created_date, ...)
* `user_group` (id, uid, code, name, created_date, ...)
* `user_group_users` (group_id, user_id)
* `user_group_managed_groups` (user_group_id, managed_group_id)
* `option_set` (id, uid, code, name, created_date, ...)
* `option_value` (id, uid, code, name, option_set_id, option_set_uid, created_date, ...)
* `data_template` (id, uid, code, name, fields (`id`, `name`, `parent` section, `type`, etc), sections (id, name,
  parent) (normal and repeatable), created_date, ...)
* `assignment` (id, uid, team_id, activity_id, org_unit_id, forms (i.e templates i.e vocabularies) jsonb array of
  template uids, created_date, ...)

Example `team.form_permissions`:

```json-
[{"form":"MI8KQFsxGFc","permissions":["ADD_SUBMISSIONS"]}]
````

### difficulties in the existing model

The current system relies on implicit relationships between activities, assignments, teams, org units, and templates.
While this works for basic scenarios, it introduces several practical difficulties:

* **Selectable entities are implicit.** What a user can select in a form (org units, teams, users, options) is derived
  indirectly from team membership, assignments, and org-unit relations rather than being explicitly modeled.
* **Configuration logic is scattered.** Access rules and filtering logic live across multiple places (assignment, team,
  org unit hierarchy, template design), making it hard to reason about or explain to administrators.
* **Limited support for complex flows.** Scenarios such as multi-party transactions, conditional source/destination
  selection, cross-team or cross-org flows, and curated lists require custom logic instead of configuration.
* **Tight coupling between planning and execution.** Assignments are designed around teams and org units upfront, which
  makes it difficult to adapt when users participate in multiple contexts or roles.
* **Hard to evolve without side effects.** Small changes in assignments, team membership, or org-unit structure can
  unintentionally affect what users see or can submit.
* **Offline sync assumes a fixed context.** The mobile client syncs configuration per team/assignment, which becomes
  inefficient or ambiguous for users with many assignments or overlapping scopes.

These issues make the system harder to extend, reason about, and safely configure as requirements evolve.

### What we aim to add / enable (concept notes)

* **Collect first, interpret later.** Clients capture structured "statements" using configurable **vocabularies** inside
  a scoped **context window** (assignment). Captured data is plain data — interpretation or workflows are applied later
  server-side.
* **Polymorphic parties & roles.** Parties are simple pointers (`{type, id, label}`) and roles are labels (e.g., `from`,
  `to`). This reuses the same UI primitives across domains.
* **Decoupling = flexibility.** Keep client contract minimal and stable so new server-side workflows or business logic
  can be added without changing the client.

* **Statement** — a user-submitted fact about the world (atomic unit of client collection).
* **Vocabulary** — a `dataTemplate` designed for collecting one kind of Statement (what fields/facts can be stated).
* **Context Window** — an `assignment` (where this Vocabulary appears and which Parties are visible).
* **Party** — any thing a user can point at (`{type, id, label}`); no behavior, only reference.
* **Role** — a label a Vocabulary assigns to a PartyRef (e.g., `from`, `to`, `reported_by`).

### 1) One-line mapping

* **Activity** → **Context tag** (What kind of statement this is usually about)
* **Assignment** → **Context window** (Where you configure *which* vocabularies and parties are usable)
* **dataTemplate** → **Vocabulary** (What facts can be stated — the reusable form the user will fill)
* **orgUnit / team / user / user_group / external** → **Party** (Things you can point at)
* **Fields inside dataTemplate** → **Facts** (the bite-sized things the user states)
* **Header / line groups** → **Shared defaults / Fact lines** (convenience: header is default; lines override)
* **Team membership / assignment binding** → **Authorisation lens** (who sees which context window
  and party lists).

---

## One-line summary

Assignment = context; `party` is the single lookup; `party_set` expresses *how* to pick parties;
`assignment_party_binding` maps (assignment, vocabulary, role) → `party_set`. Permissions are materialized per user for
fast, authoritative filtering.

---

## Core data model (only fields needed to understand flow)

* **party**:
  `id, uid, type, code, name, parent_id, tags(jsonb), properties(jsonb), source_type, source_id, created_at, updated_at`
* **party_set**:
  `id, uid, name, kind(enum: STATIC, ORG_TREE, TAG_FILTER, QUERY, EXTERNAL), spec (typed JSON) , created_at`
* **party_set_member**: `id, party_set_id, party_id, created_at` (STATIC members — relational)
* **assignment**: `id, uid, name, status (Draft|Active|Closed), visibility (PUBLIC|PRIVATE), default_party_set_id`
* **assignment_member**: `assignment_id, member_type (USER|TEAM|USER_GROUP), member_id, role`
* **assignment_party_binding**:
  `id, assignment_id, vocabulary_id (nullable), role_name, party_set_id, principal_type (nullable), principal_id (nullable), combine_mode (UNION|INTERSECT)`
* **user_allowed_party** (derived): `user_id, party_id, permission_mask, provenance(jsonb), last_updated`
* **data_template**: existing `data_template` with per-field `partySetRef` and `dependsOn`

---

## Binding precedence (exact, single pass)

1. Check principal-scoped bindings in this order: for each principal of user (USER, TEAMs, USER_GROUPs)
   a. `(assignmentId, vocabularyId, role, principal_type, principal_id)`
   b. `(assignmentId, null (vocab), role, principal_type, principal_id)`
2. If none matched, check assignment-level:
   a. `(assignmentId, vocabularyId, role_name, null, null)`
   b. `(assignmentId, null, role_name, null, null)`
3. If still none, use template field `partySetRef`.
4. If still none, use `assignment.default_party_set_id` or `assignment_scoped` fallback.

* If multiple bindings result, combine according to `combine_mode` (default = UNION).

---

## PartySet execution (kinds — exactly what engine must run)

* **STATIC**: SELECT `party` via `party_set_member`. (members in relational table)
* **ORG_TREE**: recursive CTE from `spec.rootId` down to `spec.depth`.
* **TAG_FILTER**: `WHERE tags @> spec.tags` or suitable jsonb operator.
* **QUERY**: execute named safe server-side query with bound params from `spec`.
* **EXTERNAL**: call external API; do NOT auto-insert into `party` unless promoted manually; snapshot selection in
  submission.

---

## Core runtime flow (exact steps)

Input: `(assignmentId, vocabularyId, role, userId, contextValues, q, limit, offset)`

1. **Membership gate**

    * If `assignment.visibility == PRIVATE` → require membership via `assignment_member` (user direct or via
      team/user_group). If not member → deny/empty.
2. **Resolve effective binding(s)** using the precedence above (must return list of `party_set_id` + provenance).
3. **For each party_set**: expand according to kind (STATIC / ORG_TREE / TAG_FILTER / QUERY / EXTERNAL). Use SQL where
   possible.
4. **Combine results** (apply combine_mode). Default behavior: UNION.
5. **Permission filter**: join `user_allowed_party` (or evaluate ACL) to remove unauthorized parties for caller.
   Superusers bypass. recompute `user_allowed_party` entries (incremental updates). Use event-driven updates to keep
   materialized tables current.
6. **Apply search `q` and pagination** (fetch `limit+1` to detect more).

---

## Permissions: materialization & maintenance (concise)

* `user_allowed_party` is the performance surface used by resolver. It is computed from: ACL ACEs + assignment-derived
  grants (team→assignment→party_set expansions) + explicit grants.
* Updates: initial full rebuild (backfill), incremental event-driven updates on changes to assignment_member,
  assignment_party_binding, party_set_member, team membership, ACL, and periodic reconcile job. Store `provenance` for
  audit.

---

## APIs (minimal, final)

* `GET /context/manifest` — returns the user’s active assignments, vocabulary list, and binding IDs (manifest only;
  small).
* `POST /parties/resolve` — body: `{assignmentId, vocabularyId, role, userId, contextValues, q, limit, offset}` →
  `PartiesResponse`.
* Admin CRUD: `/api/admin/party-sets`, `/api/admin/party-set-members`, `/api/admin/assignments`,
  `/api/admin/assignment-members`, `/api/admin/assignment-bindings`.
* Sync (mobile): manifest + per-resource paged endpoints (manifest → bindings per assignment → partyset parties paged →
  user_allowed_parties paged).

---

## Samples

# 1) Cross-border Aid Shipment (multi-party, multi-line, roles)

Use-case: many SKUs move between warehouses, trucks, and partner warehouses; lines may have different sources.

**Vocabulary (client template — client-facing)**

```json-
{
  "id":"v-aid-shipment",
  "label":"Aid Shipment",
  "fields":[
    {"name":"shipment_date","type":"datetime","ui":{"autoFill":"now"}},
    {"name":"shipment_ref","type":"string"},
    {"name":"sender","type":"partyRef","role":"from","partySetRef":"partySetSenders"},
    {"name":"carrier","type":"partyRef","role":"carrier","partySetRef":"partySetSenders"},
    {"name":"receiver","type":"partyRef","role":"to","partySetRef":"partySetReceivers"},
    {"name":"lines","type":"line","repeatable":true,"template":{
      "fields":[
        {"name":"item","type":"itemRef","required":true},
        {"name":"qty","type":"number","required":true},
        {"name":"uom","type":"uomRef"},
        {"name":"source_party","type":"partyRef","partySetRef":"partySetSources", "ui":{"label":"Line source (optional)"}},
        {"name":"dest_party","type":"partyRef","partySetRef":"partySetDests", "ui":{"label":"Line dest (optional)"}},
        {"name":"notes","type":"string"}
      ]
    }}
  ]
}
```

**Submission example**

```json-
{
  "template_id":"v-aid-shipment",
  "assignment_id":"assign-border-ops",
  "user_id":"user-7",
  "values":{
    "shipment_date":"2025-12-22T08:00:00Z",
    "shipment_ref":"SHP-2025-221",
    "sender":{"type":"orgUnit","id":"ou-warehouse-ams"},
    "carrier":{"type":"external","id":"carrier-x"},
    "receiver":{"type":"orgUnit","id":"ou-groundhub-ct"},
    "lines":[
      {"item":{"id":"itm-water-20l"},"qty":200,"uom":{"id":"uom-pallet"}},
      {"item":{"id":"itm-medkit"},"qty":50,"uom":{"id":"uom-box"},"source_party":{"type":"orgUnit","id":"ou-medstore-1"}}
    ]
  }
}
```

**Configurator/UI notes**

* PartyPicker prefilters per assignment: only allowed warehouses and partner carriers appear.
* Lines inherit header sender/receiver unless per-line override set.
* Quick-add: CSV/paste to bulk add lines; barcode scan to add items.

---

# 2) Anonymous Community Sentiment Survey (party-less, aggregate)

Use-case: collect quick anonymous scores and comments from community meetings.

**Vocabulary**

```json-
{
  "id":"v-community-survey",
  "label":"Community Sentiment Survey",
  "fields":[
    {"name":"survey_date","type":"date","ui":{"autoFill":"today"}},
    {"name":"topic","type":"string"},
    {"name":"rating","type":"number","ui":{"min":1,"max":10},"required":true},
    {"name":"comments","type":"string","ui":{"multiline":true}}
  ]
}
```

**Submission example**

```json-
{
  "template_id":"v-community-survey",
  "assignment_id":"assign-community-engage",
  "user_id":"user-42",
  "values":{"survey_date":"2025-12-22","topic":"Market access","rating":8,"comments":"Mostly positive but need smaller packaging"}
}
```

**Configurator/UI notes**

* No PartyPicker shown.
* Include simple analytics hooks later (avg rating) but collection is purely facts.

---

## 5 — `spec` JSON shapes (examples)

* **org_tree**

  ```json
  { "rootId": "ou-5", "depth": 5, "include_self": true }
  ```
* **tag_filter**

  ```json
  { "tags":["cold_chain","primary"], "types":["orgUnit"] }
  ```
* **query**

  ```json
  { "sql_key":"active_warehouses_by_program", "params": {"program_id":"prog-9"} }
  ```
* **assignment_scoped**

  ```json
  { "use_assignment_allowed_parties": true }
  ```
* **external**

  ```json
  { "provider":"partner_api", "endpoint":"/partners/{org}/sites" }
  ```

```java

@Data
public static class PartySetSpec {
    private String rootId;              // For ORG_TREE
    private Integer depth;              // nullable For ORG_TREE
    private Boolean includeSelf;        // For ORG_TREE

    private List<String> tags;          // For TAG_FILTER
    private List<String> types;         // ORG_UNIT, TEAM, USER. Nullable For Specific TAG_FILTER

    private String sqlKey;              // For QUERY
    private Map<String, Object> params; // For QUERY
}
```

---

### The End-to-End Logic Flow (The "Happy Path")

1. **Identity:** User logs in; Backend identifies their `Team_IDs`/`USER_GROUP_IDs`/`USER`.
2. **Context:** Backend returns all `Active Assignments` where those `Team_IDs`/`USER_GROUP_IDs`/`USER` are members.
3. **Instruction:** User selects Assignment; Backend sends the "Manifest" (Vocabularies + Role Bindings).
4. **Assistance:** User clicks a field; Backend **Resolves** the PartySet list (applying any search/filter queries).
5. **Submission:** User submits; Backend **Validates** the selection, **Snapshots** the party labels, and **Saves**
   the "Fact."
