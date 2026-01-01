# Clean, minimal canonical spec (built *on top of my existing system*)

## Assumptions

### Dev Stack and Platform / Build dependencies

The current system is built upon:

* **Java 17+ (Spring Boot 3.4.2)**: A Maven-based project.
* **PostgresSQL (tested with v16.x)**: Utilizes a compatible PostgresSQL JDBC driver.
* **Liquibase (XML)**: Used for managing schema migrations.
* **`jOOQ` & `NamedParameterJdbcTemplate`/`JdbcTemplate`**: Available for analytical queries.
* **Caching**: Employs Ehcache and Hibernate 2nd-level cache annotations where appropriate.
* **Mapping and Codegen Tools**: Lombok (preferred for compactness and brevity) and MapStruct are used.

* **id**: internal primary key (VARCHAR(26)) ULID format. Immutable, never recycled. Used for foreign-key
  relationships.
* **uid**: short system generated business key (VARCHAR(11)), globally unique, stable across environments, used
  extensively in api client's requests and analytics for human-friendly references.

## 1. Current running entities we will enhance (short)

* `org_unit` (id ULID, code, name, path, level, created_date, ...)
* `org_unit_closure` (ancestor_id, ancestor_uid, descendant_id, descendant_uid, level_diff) maintained and updated from
  the `org_unit` table data.
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
   Superusers bypass.
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


## Appendix (codes)

```java
@Entity
@Table(name = "assignment_member")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssignmentMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "assignment_id", length = 26, nullable = false)
    private String assignmentId;
    @Column(name = "member_type", nullable = false, length = 50)
    private String memberType; // USER | TEAM | USER_GROUP
    @Column(name = "member_id", length = 26, nullable = false)
    private String memberId;
    @Column(name = "role")
    private String role;
    @Column(name = "created_by", nullable = false, updatable = false)
    private Instant createdBy;
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;
}

//
@Entity
@Table(name = "assignment_party_binding")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssignmentPartyBinding {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(length = 11, unique = true, nullable = false, updatable = false)
    private String uid;
    
    /// role Name
    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "assignment_id", length = 26, nullable = false)
    private String assignmentId;

    private String vocabularyId; // dataTemplate id (nullable)

    @Column(name = "party_set_id", nullable = false)
    private UUID partySetId;

    @Column(name = "principal_type", length = 64)
    private String principalType;

    @Column(name = "principal_id", length = 26)
    private String principalId;

    @Column(name = "combine_mode", length = 32)
    private CombineMode combineMode;
}

//

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Party extends NamedObject {
    public enum SourceType {INTERNAL, EXTERNAL}

    public enum PartyType {ORG_UNIT, TEAM, USER, STATIC, EXTERNAL}

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /// 11-char Business Key
    @Column(name = "uid", length = 11, unique = true, nullable = false, updatable = false)
    private String uid;

    @Column(name = "code", length = 32)
    protected String code;

    @Column(name = "name", nullable = false)
    private String name;

    /// ORG_UNIT, TEAM, USER, STATIC, EXTERNAL Types
    @Column(name = "type", nullable = false, length = 32)
    private String type;

    /// INTERNAL, EXTERNAL
    @Column(name = "source_type", nullable = false, length = 32)
    private SourceType sourceType;

    @Column(name = "source_id", length = 64, nullable = false)
    private String sourceId;

    @Column(name = "parent_id", length = 32)
    private UUID parentId;

    @Column(name = "tags", columnDefinition = "jsonb default '[]'::jsonb")
    @Type(JsonType.class)
    private List<String> tags;

    /// small JSON containing data from domain entity (do not dump domain model here)
    @Type(JsonType.class)
    @Column(name = "properties_map", columnDefinition = "jsonb")
    @JsonProperty
    private Map<String, Object> properties;
}
//

@Entity
@Table(name = "party_set")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PartySet extends NamedObject {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(length = 11, unique = true, nullable = false)
    private String uid; // 11-char Business Key

    @Column(name = "code", unique = true, length = 32)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    private PartySetKind kind;

    @Column(columnDefinition = "jsonb default '{}'::jsonb", nullable = false)
    @Type(JsonType.class)
    private PartySetSpec spec;

    @Column(name = "is_materialized")
    @Builder.Default
    protected Boolean isMaterialized = Boolean.TRUE;

    @Data
    public static class PartySetSpec {
        private UUID rootId;              // For ORG_TREE
        private Integer depth;              // nullable For ORG_TREE
        private Boolean includeSelf;        // For ORG_TREE

        private List<String> tags;          // For TAG_FILTER
        private List<String> types;         // ORG_UNIT, TEAM, USER. Nullable For Specific TAG_FILTER

        private String sqlKey;              // For QUERY
        private Map<String, Object> params; // For QUERY
    }
}

@Entity
@Table(name = "party_set_member")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PartySetMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "party_set_id", length = 26, nullable = false)
    private UUID partySetId;

    @Column(name = "party_id", length = 26, nullable = false)
    private UUID partyId;
}
```

The brain:

```java
package org.nmcpye.datarun.party.resolution.strategies;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.nmcpye.datarun.party.dto.PartyResolutionRequest;
import org.nmcpye.datarun.party.dto.ResolvedParty;
import org.nmcpye.datarun.party.entities.PartySetKind;
import org.nmcpye.datarun.party.resolution.engine.PartySecurityFilter;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.nmcpye.datarun.jooq.public_.Tables.PARTY;
@Service
@RequiredArgsConstructor
public class BindingResolver {

    private final DSLContext dsl;

    /**
     * Resolves the list of PartySets to apply based on precedence.
     *
     * @param templateFallbackPartySetId The 'partySetRef' from the vocabulary/template (Level 3 fallback).
     */
    public List<BindingResult> resolveBindings(
        String assignmentId,
        String vocabularyId,
        String role,
        String userId,
        UUID templateFallbackPartySetId
    ) {
        // 1. Resolve User's Principals (User ID + Team IDs + Group IDs)
        // We do this first to check "Level 1" precedence.
        Set<String> principalIds = getUserPrincipals(userId);

        // 2. LEVEL 1: Check Principal-Scoped Bindings
        // (Assignment + Role + [Vocab?] + Principal)
        List<BindingResult> principalBindings = fetchBindings(assignmentId, vocabularyId, role, principalIds, true);
        if (!principalBindings.isEmpty()) {
            return principalBindings;
        }

        // 3. LEVEL 2: Check Assignment-Global Bindings
        // (Assignment + Role + [Vocab?] + No Principal)
        List<BindingResult> globalBindings = fetchBindings(assignmentId, vocabularyId, role, null, false);
        if (!globalBindings.isEmpty()) {
            return globalBindings;
        }

        // 4. LEVEL 3: Template Fallback
        if (templateFallbackPartySetId != null) {
            return List.of(new BindingResult(
                templateFallbackPartySetId,
                CombineMode.UNION,
                "Template Fallback (partySetRef)"
            ));
        }

        // 5. LEVEL 4: Assignment Default
        UUID assignmentDefaultId = dsl.select(ASSIGNMENT.DEFAULT_PARTY_SET_ID)
            .from(ASSIGNMENT)
            .where(ASSIGNMENT.ID.eq(assignmentId))
            .fetchOneInto(UUID.class);

        if (assignmentDefaultId != null) {
            return List.of(new BindingResult(
                assignmentDefaultId,
                CombineMode.UNION,
                "Assignment Default"
            ));
        }

        // 6. No bindings found (return empty list, effectively no parties)
        return Collections.emptyList();
    }

    /**
     * for superuser, fetch with no per principle filtering
     *
     * @param assignmentId assignment id
     * @param vocabularyId template id
     * @param role         binding role name
     * @return admin accessible binding, which is all
     */
    public List<BindingResult> resolveBindings(String assignmentId, String vocabularyId, String role) {
        return fetchBindings(assignmentId, vocabularyId, role, null, false);
    }

    /**
     * Fetches bindings from assignment_party_binding table.
     * Handles the precedence of "Specific Vocabulary" > "Null Vocabulary" implicitly via sorting.
     */
    private List<BindingResult> fetchBindings(
        String assignmentId,
        String vocabularyId,
        String role,
        Set<String> principalIds,
        boolean usePrincipals
    ) {
        var query = dsl.select(
                ASSIGNMENT_PARTY_BINDING.PARTY_SET_ID,
                ASSIGNMENT_PARTY_BINDING.COMBINE_MODE,
                ASSIGNMENT_PARTY_BINDING.VOCABULARY_ID,
                ASSIGNMENT_PARTY_BINDING.PRINCIPAL_ID
            )
            .from(ASSIGNMENT_PARTY_BINDING)
            .where(ASSIGNMENT_PARTY_BINDING.ASSIGNMENT_ID.eq(assignmentId))
            .and(ASSIGNMENT_PARTY_BINDING.NAME.eq(role));

        // Filter by Principals or NULL
        if (usePrincipals && principalIds != null && !principalIds.isEmpty()) {
            query = query.and(ASSIGNMENT_PARTY_BINDING.PRINCIPAL_ID.in(principalIds));
        } else {
            query = query.and(ASSIGNMENT_PARTY_BINDING.PRINCIPAL_ID.isNull());
        }

        // Handle Vocabulary Specificity
        // We want rows that match our Vocab OR are NULL.
        if (vocabularyId != null) {
            query = query.and(
                ASSIGNMENT_PARTY_BINDING.VOCABULARY_ID.eq(vocabularyId)
                    .or(ASSIGNMENT_PARTY_BINDING.VOCABULARY_ID.isNull())
            );
        } else {
            query = query.and(ASSIGNMENT_PARTY_BINDING.VOCABULARY_ID.isNull());
        }

        // Fetch all candidates
        var results = query.fetch();

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        // In-Memory Sorting for Specificity (avoiding complex SQL sorting for now)
        // Rule: Specific Vocab > Null Vocab.
        // If we have Mixed Specificity (some match vocab, some are null),
        // the "Specific Vocabulary" rule usually strictly wins in config systems.
        // Strategy: If *any* row matches the specific Vocabulary, discard the Null-Vocab rows.
        boolean hasSpecificVocabMatch = results.stream()
            .anyMatch(r -> vocabularyId != null &&
                vocabularyId.equals(r.get(ASSIGNMENT_PARTY_BINDING.VOCABULARY_ID)));

        return results.stream()
            .filter(r -> {
                if (!hasSpecificVocabMatch) return true; // Keep all if no specific winner
                return vocabularyId != null && vocabularyId.equals(r.get(ASSIGNMENT_PARTY_BINDING.VOCABULARY_ID));
            })
            .map(r -> new BindingResult(
                r.get(ASSIGNMENT_PARTY_BINDING.PARTY_SET_ID),
                // Ensure DB stores 'UNION'/'INTERSECT'
                CombineMode.valueOf(r.get(ASSIGNMENT_PARTY_BINDING.COMBINE_MODE) != null ?
                    r.get(ASSIGNMENT_PARTY_BINDING.COMBINE_MODE) : "UNION"),
                determineProvenance(r, usePrincipals)
            ))
            .collect(Collectors.toList());
    }

    private String determineProvenance(Record r, boolean isPrincipal) {
        if (isPrincipal) return "Principal Binding: " + r.get(ASSIGNMENT_PARTY_BINDING.PRINCIPAL_ID);
        return "Assignment Global Binding";
    }

    /**
     * Helper to gather all IDs representing the user (User ID + Team IDs + Group IDs).
     */
    private Set<String> getUserPrincipals(String userId) {
        Set<String> principals = new java.util.HashSet<>();
        principals.add(userId);

        final var p = dsl.select(TEAM_USER.TEAM_ID)
            .from(TEAM_USER)
            .join(TEAM).on(TEAM_USER.TEAM_ID.eq(TEAM.ID))
            .where(TEAM_USER.USER_ID.eq(userId))
            .fetchInto(String.class);
        // Add Teams
        principals.addAll(dsl.select(TEAM_USER.TEAM_ID)
            .from(TEAM_USER)
            .join(TEAM).on(TEAM_USER.TEAM_ID.eq(TEAM.ID))
            .where(TEAM_USER.USER_ID.eq(userId).and(TEAM.DISABLED.isFalse().or(TEAM.DISABLED.isNull())))
            .fetchInto(String.class));

        // Add User Groups
        principals.addAll(dsl.select(USER_GROUP_USERS.USER_GROUP_ID)
            .from(USER_GROUP_USERS)
            .join(USER_GROUP).on(USER_GROUP_USERS.USER_GROUP_ID.eq(USER_GROUP.ID))
            .where(USER_GROUP_USERS.USER_ID.eq(userId).and(USER_GROUP.DISABLED.isFalse()
                .or(USER_GROUP.DISABLED.isNull())))
            .fetchInto(String.class));

        return principals;
    }
}
//
@Service
public class PartyResolutionEngine {

    private final Map<PartySetKind, PartySetStrategy> strategies;

    public PartyResolutionEngine(List<PartySetStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(PartySetStrategy::getKind, Function.identity()));
    }

    public List<ResolvedParty> executeStrategy(PartySetKind kind,
                                               UUID partySetId,
                                               String spec,
                                               boolean isMaterialized,
                                               PartyResolutionRequest request) {
        PartySetStrategy strategy = strategies.get(kind);

        if (strategy == null) {
            throw new UnsupportedOperationException("No strategy found for kind: " + kind);
        }

        return strategy.resolve(partySetId, spec, isMaterialized, request);
    }
}

@Component
public class PartySecurityFilter {

    /**
     * Applies the permission filter to an ongoing query.
     *
     * @param query          The base query (selecting from PARTY or joining PARTY)
     * @param userId         The requesting user
     * @param isMaterialized If false, the set is Public/Unsecured, so we skip the check.
     * @return The query with the security JOIN/WHERE clause appended
     */
    public <R extends org.jooq.Record> SelectConditionStep<R> apply(
        SelectConditionStep<R> query,
        String userId,
        boolean isMaterialized
    ) {
        if (!isMaterialized) {
            // Public set (e.g., Generic Option List, Survey Locations) -> No security check needed
            return query;
        }

        // Secure set: strict INNER JOIN to user_allowed_party
        // This automatically filters out any party not present in the permission table for this user.
        return query
            .andExists(
                DSL.selectOne()
                    .from(USER_ALLOWED_PARTY)
                    .where(USER_ALLOWED_PARTY.PARTY_ID.eq(PARTY.ID))
                    .and(USER_ALLOWED_PARTY.USER_ID.eq(userId))
            );
    }
}
//
@Component
public class NamedQueryProvider {

    private final DSLContext dsl;
    private final Map<String, BiFunction<DSLContext, Map<String, Object>, SelectConditionStep<?>>> queries;

    public NamedQueryProvider(DSLContext dsl) {
        this.dsl = dsl;
        this.queries = Map.of(
            // Query 1: Find children of a specific Parent Org Unit
            // Spec: { "query": "FIND_ORG_CHILDREN", "params": { "parentId": "context_region_id" } }
            "FIND_ORG_CHILDREN", (ctx, params) -> {
                UUID parentId = getUuid(params, "parentId");
                if (parentId == null) return null; // Or throw, or return empty condition

                // We join Party to OrgUnit to filter by parent
                return ctx.select(PARTY.asterisk())
                    .from(PARTY)
                    .where(PARTY.PARENT_ID.eq(parentId));
            },

            // Query 2: Find Teams managing a specific Team (e.g. for escalation)
            "FIND_MANAGING_TEAMS", (ctx, params) -> {
                UUID teamId = getUuid(params, "targetTeamId");
                // ... logic ...
                return ctx.select(PARTY.asterisk())
                    .from(PARTY)
                    .where(DSL.falseCondition()); // stub
            }
        );
    }

    public SelectConditionStep<?> getQuery(String queryName, Map<String, Object> resolvedParams) {
        if (!queries.containsKey(queryName)) {
            throw new IllegalArgumentException("Unknown named query: " + queryName);
        }
        return queries.get(queryName).apply(dsl, resolvedParams);
    }

    private UUID getUuid(Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val instanceof String s) return UUID.fromString(s);
        if (val instanceof UUID u) return u;
        return null;
    }
}

/**
 * @author Hamza Assada 29/12/2025
 */
public interface PartySetStrategy {

    /**
     * @return The specific kind this strategy handles (STATIC, ORG_TREE, etc.)
     */
    PartySetKind getKind();

    /**
     * Resolves parties based on the party set specification.
     *
     * @param partySetId The ID of the configuration row (party_set)
     * @param spec       The JSONB spec payload (parsed or raw)
     * @param request    The user's request context
     * @return A list of resolved parties
     */
    List<ResolvedParty> resolve(UUID partySetId,
                                String spec,
                                boolean isMaterialized,
                                PartyResolutionRequest request);
}


@Component
public class QueryPartySetStrategy implements PartySetStrategy {

    private final NamedQueryProvider queryProvider;
    private final PartySecurityFilter securityFilter; // Injected
    private final ObjectMapper objectMapper;

    public QueryPartySetStrategy(NamedQueryProvider queryProvider, PartySecurityFilter securityFilter, ObjectMapper objectMapper) {
        this.queryProvider = queryProvider;
        this.securityFilter = securityFilter;
        this.objectMapper = objectMapper;
    }

    @Override
    public PartySetKind getKind() {
        return PartySetKind.QUERY;
    }

    @Override
    public List<ResolvedParty> resolve(UUID partySetId,
                                       String specJson,
                                       boolean isMaterialized,
                                       PartyResolutionRequest request) {
        try {
            // 1. Parse Spec
            // Expected Spec: { "query": "FIND_ORG_CHILDREN", "params": { "parentId": "region_id" } }
            JsonNode spec = objectMapper.readTree(specJson);
            String queryName = spec.path("sqlKey").asText();
            JsonNode paramMapping = spec.path("params");

            // 2. Resolve Parameters (Map Spec Param -> Context Value)
            Map<String, Object> resolvedParams = new HashMap<>();
            if (paramMapping.isObject()) {
                paramMapping.fields().forEachRemaining(entry -> {
                    String sqlParamName = entry.getKey();
                    String contextKey = entry.getValue().asText();

                    // Look up value in the Request Context (e.g. what the user selected in dropdown A)
                    Object contextValue = request.getContextValues().get(contextKey);
                    resolvedParams.put(sqlParamName, contextValue);
                });
            }

            // 3. Get Base Query
            SelectConditionStep<?> baseQuery = queryProvider.getQuery(queryName, resolvedParams);

            if (baseQuery == null) {
                // Pre-requisites not met (e.g. parent dropdown not selected yet)
                return Collections.emptyList();
            }

            // 4. Apply Common Filters (Search q)
            if (request.getSearchQuery() != null && !request.getSearchQuery().isBlank()) {
                String term = "%" + request.getSearchQuery().trim() + "%";
                baseQuery.and(
                    PARTY.NAME.likeIgnoreCase(term).or(PARTY.CODE.likeIgnoreCase(term))
                );
            }

            // --- APPLY SECURITY FILTER HERE ---
            // Note: The baseQuery MUST select from PARTY (or alias it correctly) for the filter to work.
            // The NamedQueryProvider should ensure queries are rooted in PARTY.
            var securedQuery = securityFilter.apply(baseQuery, request.getUserId(), isMaterialized);

            // 5. Execute & Map
            // Note: baseQuery is selecting *, so we can fetch into ResolvedParty manually
            return securedQuery
                .orderBy(PARTY.NAME.asc())
                .limit(request.getLimit())
                .offset(request.getOffset())
                .fetch(this::mapRecord);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Invalid JSON spec for PartySet " + partySetId, e);
        }
    }

    private ResolvedParty mapRecord(Record r) {
        // Safe mapping from the PARTY table columns in the result
        return new ResolvedParty(
            r.get(PARTY.ID),
            r.get(PARTY.UID),
            r.get(PARTY.TYPE),
            r.get(PARTY.NAME),
            r.get(PARTY.CODE),
            null, // json properties
            r.get(PARTY.SOURCE_TYPE)
        );
    }
}

@Component
public class StaticPartySetStrategy implements PartySetStrategy {

    private final DSLContext dsl;
    private final PartySecurityFilter securityFilter; // Injected

    public StaticPartySetStrategy(DSLContext dsl, PartySecurityFilter securityFilter) {
        this.dsl = dsl;
        this.securityFilter = securityFilter;
    }

    @Override
    public PartySetKind getKind() {
        return PartySetKind.STATIC;
    }

    @Override
    public List<ResolvedParty> resolve(UUID partySetId,
                                       String spec,
                                       boolean isMaterialized,
                                       PartyResolutionRequest request) {

        // 1. Base Condition: Must belong to this specific set
        Condition whereCondition = PARTY_SET_MEMBER.PARTY_SET_ID.eq(partySetId);

        // 2. Apply Search (if provided)
        // We check name or code via case-insensitive search
        if (request.getSearchQuery() != null && !request.getSearchQuery().isBlank()) {
            String term = "%" + request.getSearchQuery().trim() + "%";
            whereCondition = whereCondition.and(
                PARTY.NAME.likeIgnoreCase(term)
                    .or(PARTY.CODE.likeIgnoreCase(term))
            );
        }

        var query = dsl.select(
                PARTY.ID,
                PARTY.UID,
                PARTY.TYPE,
                PARTY.NAME,
                PARTY.CODE,
                PARTY.PROPERTIES_MAP,
                PARTY.SOURCE_TYPE // Assuming you added this to distinguish Internal vs External
            )
            .from(PARTY)
            .join(PARTY_SET_MEMBER).on(PARTY.ID.eq(PARTY_SET_MEMBER.PARTY_ID))
            .where(whereCondition);

        // --- APPLY SECURITY FILTER ---
        var securedQuery = securityFilter.apply(query, request.getUserId(), isMaterialized);

        // 3. Execute Query
        return securedQuery
            .orderBy(PARTY.NAME.asc())
            .limit(request.getLimit())
            .offset(request.getOffset())
            .fetch(record -> new ResolvedParty(
                record.get(PARTY.ID),
                record.get(PARTY.UID),
                record.get(PARTY.TYPE),
                record.get(PARTY.NAME), // Mapping 'name' to 'label'
                record.get(PARTY.CODE),
                // Safely handle JSONB -> Map conversion if needed, or pass null
                // jOOQ usually maps JSONB to a JSON object or String, depending on config.
                // Assuming implicit conversion or a utility method here:
                null,
                record.get(PARTY.SOURCE_TYPE)
            ));
    }
}
```

```java
package org.nmcpye.datarun.party.resolution;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.nmcpye.datarun.party.dto.BindingResult;
import org.nmcpye.datarun.party.dto.CombineMode;
import org.nmcpye.datarun.party.dto.PartyResolutionRequest;
import org.nmcpye.datarun.party.dto.ResolvedParty;
import org.nmcpye.datarun.party.entities.PartySetKind;
import org.nmcpye.datarun.party.resolution.engine.PartyResolutionEngine;
import org.nmcpye.datarun.party.resolution.engine.BindingResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.nmcpye.datarun.jooq.public_.tables.PartySet.PARTY_SET;

@Service
@RequiredArgsConstructor
public class PartyResolutionService {

    private final BindingResolver bindingResolver;
    private final PartyResolutionEngine engine;
    private final DSLContext dsl; // Used for the simple config lookup

    @Transactional(readOnly = true)
    public List<ResolvedParty> resolveParties(PartyResolutionRequest request) {

        // 1. Resolve Bindings (The "Brain")
        // Note: You might need to look up templateFallbackPartySetId from the Vocabulary ID first.
        // For MVP, passing null for fallback.
        List<BindingResult> bindings = bindingResolver.resolveBindings(
            request.getAssignmentId(),
            request.getVocabularyId(),
            request.getRole(),
            request.getUserId(),
            null // TODO: Look up data_template.party_set_ref if needed later
        );

        if (bindings.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Execute Strategies & Combine Results
        // We start with the results of the first binding, then merge subsequent ones.
        List<ResolvedParty> accumulatedParties = new ArrayList<>();
        boolean firstPass = true;

        for (BindingResult binding : bindings) {
            // A. Fetch Config (Kind + Spec)
            // In a real app, cache this lookup.
            var config = dsl.select(PARTY_SET.KIND, PARTY_SET.SPEC, PARTY_SET.IS_MATERIALIZED)
                .from(PARTY_SET)
                .where(PARTY_SET.ID.eq(binding.getPartySetId()))
                .fetchOne();

            if (config == null) continue; // Should not happen given referential integrity

            PartySetKind kind = PartySetKind.valueOf(config.value1()); // Enum mapping
            String spec = config.value2().data(); // JSONB string

            // Default to TRUE (secure) if null, for safety
            boolean isMaterialized = config.value3() == null || config.value3();

            // B. Execute Strategy
            List<ResolvedParty> currentSetParties = engine.executeStrategy(
                kind,
                binding.getPartySetId(),
                spec,
                isMaterialized,
                request
            );

            // C. Combine (Union / Intersect)
            if (firstPass) {
                accumulatedParties.addAll(currentSetParties);
                firstPass = false;
            } else {
                accumulatedParties = applyCombination(
                    accumulatedParties,
                    currentSetParties,
                    binding.getCombineMode()
                );
            }
        }

        // 3. Final Polish (Deduplication)
        // UNION usually implies uniqueness. INTERSECT definitely does.
        // We assume ResolvedParty.equals/hashCode relies on ID.
        return accumulatedParties.stream().distinct().collect(Collectors.toList());
    }

    private List<ResolvedParty> applyCombination(
        List<ResolvedParty> existing,
        List<ResolvedParty> newParties,
        CombineMode mode
    ) {
        if (mode == CombineMode.INTERSECT) {
            // Only keep parties present in both lists
            // O(N*M) naive, but lists are usually page-sized (small).
            // Optimizable with Sets if lists are large.
            Set<UUID> newIds = newParties.stream()
                .map(ResolvedParty::getId)
                .collect(Collectors.toSet());

            return existing.stream()
                .filter(p -> newIds.contains(p.getId()))
                .collect(Collectors.toList());
        }

        // Default: UNION
        List<ResolvedParty> result = new ArrayList<>(existing);
        result.addAll(newParties);
        return result;
    }
}

//
@Service
@RequiredArgsConstructor
public class ManifestService {

    private final AssignmentRepository assignmentRepo;
    private final AssignmentMemberRepository memberRepo;
    /**
     * vocabRepo
     */
    private final DataTemplateRepository templateRepository;
    private final AssignmentPartyBindingRepository bindingRepo;

    // In a real app, you'd inject a "UserContext" to get the current user's ID
    public List<AssignmentManifestDto> buildManifest(String userUid, List<String> teamUids) {

        // 1. Find active Assignment IDs for this user/team
        List<String> assignmentIds = List.of(); // not implemented `memberRepo.findActiveAssignmentIds(userUid, teamUids)`;

        if (assignmentIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Fetch the Assignment Entities
        List<Assignment> assignments = assignmentRepo.findAllById(assignmentIds);

        // 3. Build DTOs
        return assignments.stream().map(assign -> {

            // Fetch linked Vocabularies (Form Templates)
            List<String> vocabUids = assignmentRepo.findById(assign.getId())
                .map(Assignment::getForms)
                .stream()
                .flatMap(Collection::stream).toList();

            // Fetch Bindings (Rules)
            // We expose these so the client can cache requests by 'partySetUid'
            List<AssignmentPartyBinding> bindings = bindingRepo.findByAssignmentId(assign.getId());

            return AssignmentManifestDto.builder()
                .assignmentUid(assign.getUid())
                .label(assign.getOrgUnit().getName())
                .status(AssignmentStatus.getAssignmentStatus(assign.getStatus()))
                .templateUids(vocabUids)
                .bindings(mapBindings(bindings))
                .build();

        }).toList();
    }

    private List<AssignmentManifestDto.BindingDto> mapBindings(List<AssignmentPartyBinding> source) {
        return source.stream().map(b -> AssignmentManifestDto.BindingDto.builder()
                .roleName(b.getName())
                .templateUid(templateRepository.findById(b.getVocabularyId())
                    .orElseThrow(() -> new NotFoundException("no template with id" + b.getVocabularyId()))
                    .getUid())
                .partySetId(b.getPartySetId()) // Exposing ID allows client to use it as a cache key
                .build())
            .toList();
    }
}
```

```java

package org.nmcpye.datarun.party.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.party.entities.Party;
import org.nmcpye.datarun.party.entities.Party.SourceType;
import org.nmcpye.datarun.party.repository.PartyRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartySyncService {

    private final PartyRepository partyRepo;
    private final OrgUnitRepository orgUnitRepository;

    /**
     * Generic upsert method.
     * We use the ID as the source of truth. If it exists, we update label/meta.
     */
    @Transactional
    public void syncParty(String id, String uid, String code, String type, String name, Map<String, String> label, String parentId,
                          List<String> tags, Map<String, Object> meta) {

        final var partyParent = partyRepo.findBySourceId(parentId)
            .orElse(null);
        final UUID partyParentId = partyParent != null ? partyParent.getId() : null;

        partyRepo.findBySourceId(id).ifPresentOrElse(
            // Case A: Update existing
            existing -> {
                boolean changed = !existing.getLabel().equals(label)
                    || !Objects.equals(existing.getParentId(), parentId);
                if (changed) {
                    existing.setName(name);
                    existing.setCode(code);
                    existing.setLabel(label);
                    existing.setType(type);
                    existing.setSourceType(SourceType.INTERNAL);
                    existing.setParentId(partyParentId);
                    existing.setProperties(meta);
                    existing.setTags(tags);
                    existing.setLastModifiedDate(Instant.now());
                    log.debug("Updated Party index: {} ({})", label, uid);
                }
            },
            // Case B: Create new
            () -> {
                Party newParty = Party.builder()
                    .id(UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)))      // Keeps the same ULID as the source
                    .uid(uid)    // Keeps the same UID as the source
                    .code(code)
                    .type(type)
                    .name(name)
                    .parentId(partyParentId)
                    .sourceType(SourceType.INTERNAL)
                    .sourceId(id)
                    .properties(meta)
                    .tags(tags) // Default empty
                    .createdDate(Instant.now())
                    .build();
                newParty.setLabel(label);

                partyRepo.persist(newParty);
                log.info("Indexed new Party: {} ({})", name, uid);
            }
        );
    }
}
```
