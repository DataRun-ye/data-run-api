Good — here’s a focused, step-by-step backfill plan you can run in dev. Each step has an exact SQL template (adjust ULID
function to match your environment), short notes, and a final quick test checklist. Do this in a dev copy first.

---

## Recommendation first (one line)

Use **one assignment per activity** + **assignment_member** rows for teams + **principal-scoped bindings** per team —
avoids exploding assignments and fits your current resolver precedence.

---

## Preconditions

* `party` table already exists and contains parties for org_units and teams (if not, create them first mapping
  `source_type`/`source_id`).
* You already created `assignment_data_template` and `party_tag` tables.
* Use a transaction / run in dev first.

---

## Step 0 — helper: ULID placeholder

Replace `gen_random_uuid()` below with your ULID generator if needed.

---

## Step 1 — backfill `party_tag` from legacy relations

Tag parties by `activity`, `team`, and `org_unit` to make tag-based PartySets easy.

```sql
-- 1.A Tag party for its org_unit (if party.source_type = 'ORG_UNIT' and you have org_unit.code)
INSERT INTO party_tag (id, party_id, tag_key, tag_value, created_date, created_by)
SELECT gen_random_uuid() AS id, p.id, 'org_unit', ou.code, now(), 'admin'::varchar AS created_by
FROM party p
         JOIN org_unit ou ON p.source_type = 'ORG_UNIT' AND p.source_id = ou.id
WHERE NOT EXISTS(SELECT 1 FROM party_tag t WHERE t.party_id = p.id AND t.tag_key = 'org_unit');

-- 1.B Tag parties by team membership (team -> parties you consider relevant; here we tag team party)
INSERT INTO party_tag (party_id, tag_key, tag_value, created_date, created_by)
SELECT p.id, 'team', t.code, now(), 'admin'::varchar AS created_by
FROM party p
         JOIN team t ON p.source_type = 'TEAM' AND p.source_id = t.id
WHERE NOT EXISTS(SELECT 1 FROM party_tag t2 WHERE t2.party_id = p.id AND t2.tag_key = 'team');

-- 1.C Tag by activity code (if you want activity-scoped party sets)
INSERT INTO party_tag (party_id, tag_key, tag_value, created_date, created_by)
SELECT p.id, 'activity', a.code, now(), 'admin'::varchar AS created_by
FROM party p
         JOIN assignment ou ON p.source_type = 'ORG_UNIT' AND p.source_id = ou.id
         JOIN activity a ON a.id = ou.activity_id -- OR cut direct mapping; adjust to your model
WHERE a.code IS NOT NULL
  AND NOT EXISTS(SELECT 1 FROM party_tag t WHERE t.party_id = p.id AND t.tag_key = 'activity');
```

Notes: adapt joins to your actual schema: you might tag HFs by their MU/activity relationship.

---

```sql
INSERT INTO party_set (id, uid, name, kind, spec, created_date, created_by, last_modified_date, last_modified_by)
SELECT gen_random_uuid(),
       substring(md5(t.uid) for 11) as uid,
       ('TeamQuery:' || t.uid)      as name,
       'QUERY'                      as kind,
       jsonb_build_object(
           'sqlKey', 'party_by_tag',
           'params', jsonb_build_object('tagKey', 'assigned_to_team', 'tagValue', t.uid)
       )::jsonb                     as spec,
       now(),
       'admin'::varchar             AS created_by,
       now(),
       'admin'::varchar             AS last_modified_by
FROM team t
WHERE NOT EXISTS (SELECT 1
                  FROM party_set ps
                  WHERE ps.name = ('TeamQuery:' || t.uid));

INSERT INTO assignment_member (assignment_id, member_type, member_id, role, created_date, created_by,
                               last_modified_date, last_modified_by)
SELECT a.id,
       'TEAM',
       a.team_id,
       'member',
       now(),
       'admin'::varchar AS created_by,
       now(),
       'admin'::varchar AS last_modified_by
FROM assignment a
WHERE NOT EXISTS (SELECT 1
                  FROM assignment_member am
                  WHERE am.assignment_id = a.id AND am.member_type = 'TEAM' AND am.member_id = a.team_id);
```
## Runner:


For each team, create a STATIC party_set (members = parties tagged with that team or specific parties):

```java
package org.nmcpye.datarun.backfill;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Idempotent backfill runner for party_tag, party_sets, assignment_member,
 * assignment_party_binding, and assignment_data_template.
 *
 * Run in dev first. SQL uses NOT EXISTS guards to be safe on reruns.
 */
@Component
public class BackfillRunner {

    private final NamedParameterJdbcTemplate jdbc;

    public BackfillRunner(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Run all steps in order.
     */
    @Transactional
    public void runAll() {
        backfillPartyTag_orgUnit();
        backfillPartyTag_teamParties();
        backfillPartyTag_teamActivity();
        backfillPartyTag_managedBy();
        backfillPartyTag_assignedToTeam();
        createTeamQueryPartySets();
        backfillAssignmentMembersFromLegacyTeam();
        backfillAssignmentPartyBindingsForIssue();
        backfillAssignmentDataTemplateFromTeamFormPermissions();
    }

    // ---------- Step 1: party_tag backfills ----------

    public void backfillPartyTag_orgUnit() {
        String sql = """
            INSERT INTO party_tag (party_id, tag_key, tag_value, created_date, created_by)
            SELECT p.id, 'org_unit', ou.code, now(), 'admin'
            FROM party p
            JOIN org_unit ou ON p.source_type = 'ORG_UNIT' AND p.source_id = ou.id
            WHERE NOT EXISTS(
              SELECT 1 FROM party_tag t WHERE t.party_id = p.id AND t.tag_key = 'org_unit'
            );
            """;
        jdbc.getJdbcOperations().execute(sql);
    }

    public void backfillPartyTag_teamParties() {
        String sql = """
            INSERT INTO party_tag (party_id, tag_key, tag_value, created_date, created_by)
            SELECT p.id, 'team', t.uid, now(), 'admin'
            FROM party p
            JOIN team t ON p.source_type = 'TEAM' AND p.source_id = t.id
            WHERE NOT EXISTS(
              SELECT 1 FROM party_tag t2 WHERE t2.party_id = p.id AND t2.tag_key = 'team'
            );
            """;
        jdbc.getJdbcOperations().execute(sql);
    }

    public void backfillPartyTag_teamActivity() {
        String sql = """
            INSERT INTO party_tag (party_id, tag_key, tag_value, created_date, created_by)
            SELECT p.id, 'activity', act.uid, now(), 'admin'
            FROM party p
            JOIN team t ON p.source_type = 'TEAM' AND p.source_id = t.id
            JOIN activity act ON act.id = t.activity_id
            WHERE NOT EXISTS(
              SELECT 1 FROM party_tag t2 WHERE t2.party_id = p.id AND t2.tag_key = 'activity'
            );
            """;
        jdbc.getJdbcOperations().execute(sql);
    }

    public void backfillPartyTag_managedBy() {
        String sql = """
            INSERT INTO party_tag (party_id, tag_key, tag_value, created_date, created_by)
            SELECT p.id, 'managed_by', mt.uid, now(), 'admin'
            FROM party p
            JOIN team t ON p.source_type = 'TEAM' AND p.source_id = t.id
            JOIN team_managed_teams tmt ON t.id = tmt.managed_team_id
            JOIN team mt ON tmt.team_id = mt.id
            WHERE NOT EXISTS(
              SELECT 1 FROM party_tag t2 WHERE t2.party_id = p.id AND t2.tag_key = 'managed_by'
            );
            """;
        jdbc.getJdbcOperations().execute(sql);
    }

    public void backfillPartyTag_assignedToTeam() {
        String sql = """
            INSERT INTO party_tag (party_id, tag_key, tag_value, created_date, created_by)
            SELECT p.id, 'assigned_to_team', t.uid, now(), 'admin'
            FROM party p
            JOIN assignment a ON p.source_type = 'ORG_UNIT' AND p.source_id = a.org_unit_id
            JOIN team t ON t.id = a.team_id
            WHERE NOT EXISTS(
              SELECT 1 FROM party_tag t2 WHERE t2.party_id = p.id AND t2.tag_key = 'assigned_to_team'
            );
            """;
        jdbc.getJdbcOperations().execute(sql);
    }

    // ---------- Step 2: create team-scoped QUERY party_sets ----------

    /**
     * Create a Query party_set per team that can be executed with param {assigned_to_team: team_uid}
     * The spec uses a server-side sqlKey that your resolver can implement (e.g., 'party_by_tag').
     */
    public void createTeamQueryPartySets() {
        String sql = """
            INSERT INTO party_set (uid, name, kind, spec, created_date)
            SELECT substring(md5(t.uid) for 11) as uid,
                   ('TeamQuery:' || t.uid) as name,
                   'QUERY' as kind,
                   jsonb_build_object(
                     'sqlKey', 'party_by_tag',
                     'params', jsonb_build_object('tagKey', 'assigned_to_team', 'tagValue', t.uid)
                   )::jsonb as spec,
                   now()
            FROM team t
            WHERE NOT EXISTS (
              SELECT 1 FROM party_set ps WHERE ps.name = ('TeamQuery:' || t.uid)
            );
            """;
        jdbc.getJdbcOperations().execute(sql);
    }

    // ---------- Step 3: assignment_member from legacy assignment.team_id ----------

    public void backfillAssignmentMembersFromLegacyTeam() {
        String sql = """
            INSERT INTO assignment_member (assignment_id, member_type, member_id, role, created_date)
            SELECT a.id, 'TEAM', a.team_id, 'member', now()
            FROM assignment a
            WHERE a.team_id IS NOT NULL
              AND NOT EXISTS (
                SELECT 1 FROM assignment_member am WHERE am.assignment_id = a.id AND am.member_type='TEAM' AND am.member_id = a.team_id
              );
            """;
        jdbc.getJdbcOperations().execute(sql);
    }

    // ---------- Step 4: assignment_party_binding for Issue (team-scoped from / assignment-scoped to) ----------

    /**
     * Create binding: for Issue.vocabulary:
     *  - 'from' binding scoped to TEAM -> TeamQuery party_set
     *  - 'to' binding assignment-scoped -> party_set built from assigned_to_team tag or org_unit tag
     *
     * Adjust data_template code lookup as required in your environment.
     */
    public void backfillAssignmentPartyBindingsForIssue() {
        // From-binding (team-scoped) -> TeamQuery party_set
        String sqlFrom = """
            INSERT INTO assignment_party_binding (assignment_id, vocabulary_id, name, party_set_id, principal_type, principal_id, combine_mode, created_date)
            SELECT a.id, dt.id, 'from', ps.id, 'TEAM', t.uid, 'UNION', now()
            FROM assignment a
            JOIN team t ON t.id = a.team_id
            JOIN data_template dt ON dt.code = 'Issue' -- adjust if you use uid
            JOIN party_set ps ON ps.name = ('TeamQuery:' || t.uid)
            WHERE NOT EXISTS (
              SELECT 1 FROM assignment_party_binding b
              WHERE b.assignment_id = a.id AND b.vocabulary_id = dt.id AND b.name = 'from' AND b.principal_type='TEAM' AND b.principal_id = t.uid
            );
            """;
        jdbc.getJdbcOperations().execute(sqlFrom);

        // To-binding (assignment-scoped) -> use party_set by org_unit tag (create one per assignment if not exists)
        String createOrgUnitPS = """
            INSERT INTO party_set (uid, name, kind, spec, created_date)
            SELECT substring(md5(a.id::text) for 11) as uid,
                   ('AssignmentOrg:' || a.id) as name,
                   'QUERY' as kind,
                   jsonb_build_object(
                     'sqlKey', 'party_by_tag',
                     'params', jsonb_build_object('tagKey', 'org_unit', 'tagValue', (SELECT ou.code FROM org_unit ou WHERE ou.id = a.org_unit_id))
                   )::jsonb as spec,
                   now()
            FROM assignment a
            WHERE a.org_unit_id IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM party_set ps WHERE ps.name = ('AssignmentOrg:' || a.id));
            """;
        jdbc.getJdbcOperations().execute(createOrgUnitPS);

        String sqlTo = """
            INSERT INTO assignment_party_binding (assignment_id, vocabulary_id, name, party_set_id, combine_mode, created_date)
            SELECT a.id, dt.id, 'to', ps.id, 'UNION', now()
            FROM assignment a
            JOIN data_template dt ON dt.code = 'Issue'
            JOIN party_set ps ON ps.name = ('AssignmentOrg:' || a.id)
            WHERE NOT EXISTS (
              SELECT 1 FROM assignment_party_binding b
              WHERE b.assignment_id = a.id AND b.vocabulary_id = dt.id AND b.name = 'to' AND b.principal_id IS NULL
            );
            """;
        jdbc.getJdbcOperations().execute(sqlTo);
    }

    // ---------- Step 5: assignment_data_template from team.form_permissions ----------

    /**
     * Insert team-scoped assignment_data_template rows for forms declared in team.form_permissions JSON.
     * Assumes team.form_permissions is JSON array: [{"form":"<uid>","permissions":[...]}]
     */
    public void backfillAssignmentDataTemplateFromTeamFormPermissions() {
        String sql = """
            WITH team_forms AS (
              SELECT t.id AS team_id, a.id AS assignment_id, (elem->>'form') AS form_uid
              FROM team t
              JOIN assignment a ON a.team_id = t.id
              CROSS JOIN LATERAL jsonb_array_elements(t.form_permissions::jsonb) elem
            )
            INSERT INTO assignment_data_template (assignment_id, data_template_id, principal_type, principal_id, created_at)
            SELECT tf.assignment_id, dt.id, 'TEAM', t.uid, now()
            FROM team_forms tf
            JOIN team t ON t.id = tf.team_id
            JOIN data_template dt ON dt.uid = tf.form_uid
            WHERE NOT EXISTS (
              SELECT 1 FROM assignment_data_template adt
              WHERE adt.assignment_id = tf.assignment_id AND adt.data_template_id = dt.id AND adt.principal_type='TEAM' AND adt.principal_id = t.uid
            );
            """;
        jdbc.getJdbcOperations().execute(sql);
    }
}

```

---

## Step 4 — create `assignment_member` rows from legacy `assignment.team_id`

For each assignment that has `team_id` set, create a corresponding `assignment_member` entry of type TEAM.

```sql
INSERT INTO assignment_member (id, assignment_id, member_type, member_id, role, valid_from, valid_to, created_date)
SELECT gen_random_uuid()::text,
       a.id,
       'TEAM',
       a.team_id,
       'member',
       NULL,
       NULL,
       now()
FROM assignment a
WHERE a.team_id IS NOT NULL
  AND NOT EXISTS (SELECT 1
                  FROM assignment_member am
                  WHERE am.assignment_id = a.id AND am.member_type = 'TEAM' AND am.member_id = a.team_id);
```

Note: role = 'member' by default. You can afterwards edit roles for grouping access.

---

## Step 5 — create principal-scoped `assignment_party_binding` entries

For each assignment and its team (created in step 4) bind the `from` / `to` roles to the party_sets. Use team-scoped
binding so each team sees only its party_set.

Example: bind Issue.from -> TeamSet, Issue.to -> OrgTree for assignment org_unit

```sql
-- 5.A find party_set ids
-- assume ps_team is the TeamSet name, and ps_org is the OrgTree created earlier for assignment.org_unit_id
INSERT INTO assignment_party_binding (id, assignment_id, vocabulary_id, name, party_set_id, principal_type,
                                      principal_id, combine_mode, created_at)
SELECT gen_random_uuid()::text,
       a.id,
       dt.id, -- data_template id for Issue; filter by code/name
       'from',
       ps_team.id,
       'TEAM',
       a.team_id,
       'UNION',
       now()
FROM assignment a
         JOIN data_template dt ON dt.code = 'Issue' -- adjust filter condition per your template ids
         JOIN party_set ps_team ON ps_team.name = 'TeamSet:' || (SELECT code from team t where t.id = a.team_id)
         JOIN party_set ps_org
              ON ps_org.kind = 'ORG_TREE' AND (ps_org.spec ->> 'rootId') IS NOT NULL AND (ps_org.spec ->> 'rootId') =
                                                                                         (SELECT p.id
                                                                                          FROM party p
                                                                                          WHERE p.source_type = 'ORG_UNIT'
                                                                                            AND p.source_id = a.org_unit_id
                                                                                          LIMIT 1)
WHERE NOT EXISTS (SELECT 1
                  FROM assignment_party_binding b
                  WHERE b.assignment_id = a.id
                    AND b.vocabulary_id = dt.id
                    AND b.name = 'from'
                    AND b.principal_id = a.team_id);

-- 5.B bind 'to' role to org_tree (no principal override)
INSERT INTO assignment_party_binding (id, assignment_id, vocabulary_id, name, party_set_id, principal_type,
                                      principal_id, combine_mode, created_at)
SELECT gen_random_uuid()::text,
       a.id,
       dt.id,
       'to',
       ps_org.id,
       NULL,
       NULL,
       'UNION',
       now()
FROM assignment a
         JOIN data_template dt ON dt.code = 'Issue'
         JOIN party_set ps_org ON ps_org.kind = 'ORG_TREE' AND (ps_org.spec ->> 'rootId') =
                                                               (SELECT p.id
                                                                FROM party p
                                                                WHERE p.source_type = 'ORG_UNIT'
                                                                  AND p.source_id = a.org_unit_id
                                                                LIMIT 1)
WHERE NOT EXISTS (SELECT 1
                  FROM assignment_party_binding b
                  WHERE b.assignment_id = a.id AND b.vocabulary_id = dt.id AND b.name = 'to');
```

Adjust `data_template` selection to your template UIDs or codes.

---

## Step 6 — create `assignment_data_template` rows from team.form_permissions

If `team.form_permissions` JSON contains forms allowed for the team, translate them into assignment-scoped template
grants.

```sql
-- Example: team.form_permissions JSON is array; extract form uids and insert assignment_data_template per assignment/team
WITH team_forms AS (SELECT t.id                                                AS team_id,
                           a.id                                                AS assignment_id,
                           jsonb_array_elements(t.form_permissions) ->> 'form' AS form_uid
                    FROM team t
                             JOIN assignment a ON a.team_id = t.id)
INSERT
INTO assignment_data_template (id, assignment_id, data_template_id, principal_type, principal_id, principal_role,
                               created_at)
SELECT gen_random_uuid()::text, tf.assignment_id, dt.id, 'TEAM', tf.team_id, NULL, now()
FROM team_forms tf
         JOIN data_template dt ON dt.uid = tf.form_uid
WHERE NOT EXISTS (SELECT 1
                  FROM assignment_data_template adt
                  WHERE adt.assignment_id = tf.assignment_id
                    AND adt.data_template_id = dt.id
                    AND adt.principal_type = 'TEAM'
                    AND adt.principal_id = tf.team_id);
```

Notes:

* If `team.form_permissions` is nested JSON, adjust accordingly (extract by key).
* This gives team-scoped template visibility.

---

## Step 7 — (Optional) create global assignment_data_template baseline

If not already done (you said you created table and perhaps backfilled), ensure every assignment.form gets a global row
first so behavior unchanged:

```sql
INSERT INTO assignment_data_template (id, assignment_id, data_template_id, principal_type, principal_id, principal_role,
                                      created_at)
SELECT gen_random_uuid()::text, a.id, dt.id, NULL, NULL, NULL, now()
FROM assignment a
         JOIN LATERAL jsonb_array_elements_text(a.forms) AS f(uid) ON true
         JOIN data_template dt ON dt.uid = f.uid
WHERE NOT EXISTS (SELECT 1
                  FROM assignment_data_template adt
                  WHERE adt.assignment_id = a.id AND adt.data_template_id = dt.id);
```

---

## Step 8 — final consistency and index fixes

* Add indexes: `assignment_member(assignment_id)`, `assignment_party_binding(assignment_id)`,
  `party_tag(tag_key, tag_value)`, `party_set(kind)` etc.
* Ensure `assignment_member` rows have `created_date`, set roles if you want grouping.

---

## Quick test checklist (run after backfill)

1. For one assignment A and team T:

    * Verify `assignment_member` exists linking T to A.
    * Verify `party_tag` rows exist for parties under that org_unit and for team party.
    * Verify `party_set` for org_unit exists and `party_set_member`/ORG_TREE returns expected parties.
    * Verify `assignment_party_binding` has team-scoped binding for `from` and org-scoped for `to`.
    * Verify `assignment_data_template` contains both global and team-scoped rows per forms.
2. Call `GET /api/context/manifest` as a user in team T and confirm:

    * Manifest lists assignment A.
    * Manifest templates for assignment A only include templates allowed by team.form_permissions and global items.
3. Call `POST /api/parties/resolve` for assignment A, template Issue, role 'from' as user in T — ensure returned parties
   are team-set (campaign warehouse or team-specific).
4. Try as HF user (not in team) — ensure they do not see `Issue` if team-scoped access forbids it.

---

## Caveats & notes (short)

* Your idea is valid and practical: **one assignment per activity** plus principal-scoped bindings is efficient.
* Careful with tag derivation logic — ensure tags reflect the truth (which party belongs to which MU/team).
* If you need per-team entirely disparate configs (many differences), consider per (activity, team) assignments but only
  if config complexity warrants it.
* Run backfill in dev and verify before running in prod.

---

If you want, I can now:

* produce **a single combined SQL script** (ordered) with these inserts (ready to run in dev), or
* generate a small **jOOQ Java runner** that performs the backfill steps idempotently.

Which one do you want?
