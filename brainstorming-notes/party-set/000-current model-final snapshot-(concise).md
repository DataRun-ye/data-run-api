# Current model — final snapshot (concise)

## Entities & purpose

* **party** — registry of selectable actors/locations (types: ORG_UNIT, TEAM, USER, EXTERNAL, etc.). Minimal canonical
  fields: `id, uid, type, code, name, parent_id`.
* **party_tag** — normalized tags for `party` (`party_id, tag_key, tag_value`) used for admin-friendly grouping and
  dynamic queries.
* **party_set** — named set of parties used by bindings. Kinds:

    * `STATIC` — explicit `party_set_member` rows.
    * `ORG_TREE` — recursive subtree (rare for our usage).
    * `QUERY` — dynamic; `spec.sqlKey='party_by_tag'` or other server-side safe queries; accepts runtime params.
* **assignment** — context window for data collection (activity + scope). Holds `forms` (template UIDs) as base list.
* **assignment_member** — membership rows for assignment (who can act). Columns:
  `assignment_id, member_type (USER|TEAM|USER_GROUP), member_id, role, valid_from, valid_to`. This is the first gate: if
  no member row → stop.
* **assignment_data_template** — controls which `data_template` (vocabulary/form) is visible/usable inside an assignment
  for specific principals. Scoped by `principal_type/principal_id` or `principal_role`. Global rows (no principal
  fields) mean visible to all assignment members.
* **assignment_party_binding** — maps `(assignment, data_template, role_name)` → `party_set`. Can be principal-scoped (
  principal_type/principal_id) for overrides. Also has `combine_mode` (UNION / INTERSECT).
* **data_template** — form definition (fields, repeat blocks). Fields referencing parties use role labels (e.g., `from`,
  `to`) and rely on bindings to resolve allowed parties.

## Key config patterns

* Use **one assignment per activity** (per MU) where possible; add `assignment_member` rows for teams/users.
* Use `assignment_data_template` to hide templates per principal (team/user/role) — prefer using
  `assignment_member.role` values to group principals and reduce rows.
* Use a **single reusable QUERY `party_set`** (sqlKey = `party_by_tag`) that resolves parties by tags; pass runtime
  params (teamUid, activityUid) to get intersections (AND semantics). Use STATIC sets only for small curated lists (
  campaign warehouses).

## Resolution & runtime flow

1. **Client manifest** request: server builds manifest per user by:

    * collecting principals (user id, team ids, user_group ids),
    * collecting `assignment_member` roles for each assignment,
    * calling `AssignmentDataTemplateRepository.findAllowedTemplateUids(assignmentId, userId, principalIds, userRoles)`
      to compute visible templates (intersect with `assignment.forms`),
    * returning assignments + only allowed templates + bindings for those templates.
2. **Party resolution** for a role in a template:

    * Resolver loads matching `assignment_party_binding` rows (respecting principal precedence: explicit principal_id →
      principal_type+id → principal_role → global),
    * For each binding, expand `party_set` per its kind:

        * `STATIC`: read members,
        * `QUERY`: run server-side `party_by_tag` with runtime params (tags from spec + client context) to get parties,
        * `ORG_TREE`: expand subtree if used.
    * Combine fragments according to `combine_mode` (UNION/INTERSECT) and apply permission filter (user_allowed_party /
      ACL) if configured.
3. **Client behavior**:

    * Sync manifest → fetch templates → resolve required party_sets (paged) → cache locally.
    * Use role-dependent pickers; on `from` selection pass value as runtime param when resolving `to` if `to` uses QUERY
      cascade.
4. **Submission validation (server-side, mandatory)**:

    * Re-check `assignment_member` membership and `assignment_data_template` visibility for the principal.
    * Re-resolve `assignment_party_binding` for submitted `from`/`to` and confirm submitted party ids are allowed.
    * Persist submission snapshot; pass submission to ledger/processing pipeline if validated.

## Precedence & deduplication rules

* **Template visibility precedence:** explicit `principal_id` → `principal_type+id` → `principal_role` → global.
* **Binding precedence:** same ordering for matching binding rows; combine multiple matched bindings per `combine_mode`.
* **Role grouping:** use `assignment_member.role` to assign teams/users to logical roles (e.g., `issuing`, `reporter`)
  and map templates to roles in `assignment_data_template` to avoid many per-principal rows.

## Admin ergonomics

* Tag parties (`party_tag`) for flexible team/activity scoping; build `party_by_tag` QUERY party_set once and reuse with
  runtime params.
* Use STATIC sets for ephemeral campaign lists if admins want explicit curated members.
* Admin UI should allow: preview of party_set results, mapping roles → templates, and principal overrides for bindings.

---

## Diagrams

Below are four compact Mermaid diagrams:

- ER (data model).
- Manifest flow.
- Party resolution sequence.
- and Binding precedence.

Paste these ` ```mermaid ... ``` ` blocks where you need them.

```mermaid
erDiagram
    PARTY {
        varchar id PK "ULID"
        varchar uid "11-char"
        varchar type
        varchar code
        varchar name
        varchar parent_id
    }
    PARTY_TAG {
        varchar id PK
        varchar party_id FK
        varchar tag_key
        varchar tag_value
    }
    PARTY_SET {
        varchar id PK
        varchar uid
        varchar name
        varchar kind
        jsonb spec
    }
    PARTY_SET_MEMBER {
        varchar id PK
        varchar party_set_id FK
        varchar party_id FK
    }
    DATA_TEMPLATE {
        varchar id PK
        varchar uid
        varchar code
        varchar name
    }
    ASSIGNMENT {
        varchar id PK
        varchar uid
        varchar team_id
        varchar activity_id
        varchar org_unit_id
        jsonb forms
    }
    ASSIGNMENT_MEMBER {
        bigint id PK
        varchar assignment_id FK
        varchar member_type
        varchar member_id
        varchar role
    }
    ASSIGNMENT_DATA_TEMPLATE {
        varchar id PK
        varchar assignment_id FK
        varchar data_template_id FK
        varchar principal_type
        varchar principal_id
        varchar principal_role
    }
    ASSIGNMENT_PARTY_BINDING {
        varchar id PK
        varchar assignment_id FK
        varchar vocabulary_id FK
        varchar name "role name (from/to)"
        varchar party_set_id FK
        varchar principal_type
        varchar principal_id
        varchar combine_mode
    }

    PARTY ||--o{ PARTY_TAG: "has"
    PARTY ||--o{ PARTY_SET_MEMBER: "is member of"
    PARTY_SET ||--o{ PARTY_SET_MEMBER: "includes"
    ASSIGNMENT ||--o{ ASSIGNMENT_MEMBER: "has"
    ASSIGNMENT ||--o{ ASSIGNMENT_PARTY_BINDING: "has"
    DATA_TEMPLATE ||--o{ ASSIGNMENT_PARTY_BINDING: "used in"
    ASSIGNMENT ||--o{ ASSIGNMENT_DATA_TEMPLATE: "has"
    DATA_TEMPLATE ||--o{ ASSIGNMENT_DATA_TEMPLATE: "maps to"
```

```mermaid
flowchart LR
    Client["Mobile Client"] -->|GET /manifest| Server["Server / ManifestService"]
    Server --> RepoA[AssignmentRepo / AssignmentMember]
    Server --> RepoB[AssignmentDataTemplateRepo]
    Server --> RepoC[AssignmentPartyBindingRepo]
    RepoA --> Server
    RepoB --> Server
    RepoC --> Server
    Server -->|"manifest (assignments + allowed templates + bindings )"|Client
Client -->|"resolve party_set ( paged )"|PartyResolver["Party Resolver"]
PartyResolver --> PartySetStore["party_set"]
PartyResolver --> PartyTagStore["party_tag"]
PartyResolver --> PartyStore["party"]
PartyResolver -->|returns parties| Client
```

```mermaid
sequenceDiagram
    participant C as Client
    participant M as ManifestService
    participant AM as AssignmentMemberRepo
    participant ADT as AssignmentDataTemplateRepo
    participant APB as AssignmentPartyBindingRepo
    participant PS as PartySetResolver
    participant ACL as ACL/PermissionFilter
    C ->> M: request /manifest (user)
    M ->> AM: fetch principals & assignment_member roles
    AM -->> M: principals + roles
    M ->> ADT: findAllowedTemplateUids(assignment, user, principals, roles)
    ADT -->> M: list of allowed templates
    M ->> APB: fetch bindings for allowed templates
    APB -->> M: binding rows
    M -->> C: manifest (assignments + allowed templates + bindings)
    C ->> PS: resolve party_set (send binding, runtime params)
    PS ->> PS: expand party_set (STATIC | QUERY → party_by_tag | ORG_TREE)
    PS ->> ACL: filter by user_allowed_party / ACL
    ACL -->> PS: filtered parties
    PS -->> C: parties (paged)
```

```mermaid
flowchart TD
    A[Start binding lookup]
    A --> B{binding rows exist for assignment+vocab+role?}
    B -->|no| C[No parties]
    B -->|yes| D[Filter bindings by principal precedence]
    D --> E{explicit principal_id present?}
    E -->|yes| F[use principal_id bindings]
    E -->|no| G{principal_type+principal_id?}
    G -->|yes| H[use principal_type bindings]
    G -->|no| I{principal_role matches assignment_member.role?}
    I -->|yes| J[use principal_role bindings]
    I -->|no| K[use global bindings]
    F --> L["expand party_set(s)"]
H --> L
J --> L
K --> L
L --> M["combine fragments per combine_mode (UNION/INTERSECT)"]
M --> N[apply ACL / permission filter]
N --> O[return final party list]
```
