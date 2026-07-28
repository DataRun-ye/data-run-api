# Current Work

Updated: 2026-07-28

Status: ACCEPTED FOR IMPLEMENTATION

## Assignment Shadow Persistence Foundation

### Outcome

Create the smallest additive persistence boundary needed to shadow assignment
lifecycle events and effective access. This slice defines storage and tested
ports only. It does not bootstrap data, intercept writes, change active reads,
or expose an endpoint, runner, schedule, or feature flag.

This foundation is consumed by the next bootstrap/comparison slice and must
not be deployed or merged to production alone.

### Evidence

- Mutable `assignment`, direct `team_user` membership, team form permissions,
  and organization-unit scope are the DataRun Baseline authorities.
- `analytics.events` is a mutable ETL projection and is not an event journal.
- Both outbox tables and asynchronous entity audit are unsuitable for
  transactional assignment history.
- The accepted assignment contract uses UUID actor, assignment, and geographic
  scope identities. Current 11-character user and organization-unit UIDs
  remain compatibility aliases.

### Storage Contract

Add one Liquibase changeset at the end of `master.xml`, with explicit rollback.
All new tables use the public schema.

#### `event_journal`

Append-only journal shared only by the accepted assignment and capture
transition lanes:

- `journal_position BIGSERIAL` primary key;
- `event_id UUID` unique and not null;
- `event_type VARCHAR(64)` not null;
- `shape_ref VARCHAR(128)` not null;
- `activity_ref VARCHAR(11)` nullable;
- `subject_type VARCHAR(32)` not null;
- `subject_id UUID` not null;
- `actor_id VARCHAR(128)` not null;
- `recorded_at TIMESTAMP WITH TIME ZONE` not null;
- `payload JSONB` not null and constrained to an object.

Index `(subject_type, subject_id, journal_position)` and
`(event_type, journal_position)`.

These are the minimum immutable identity, classification, authorship, ordering,
and payload fields needed by both accepted lanes. Do not add subject versions,
device/sync fields, review fields, future event types, or submission columns.

#### `actor_identity_link`

Immutable alias between the DataRun Baseline user and event actor:

- `actor_id UUID` primary key;
- `baseline_user_uid VARCHAR(11)` unique and not null.

Do not copy login, name, authority, team membership, or other mutable user
properties.

#### `org_unit_identity_link`

Immutable alias between a DataRun Baseline organization unit and the UUID
scope identity used by assignment events:

- `org_unit_id UUID` primary key;
- `baseline_org_unit_uid VARCHAR(11)` unique and not null.

Do not copy names, codes, paths, parents, or hierarchy state.

#### `assignment_role_definition`

Immutable role derived later from an activity and canonical capture-form set:

- `role_key VARCHAR(128)` primary key;
- `activity_uid VARCHAR(11)` not null;
- `form_uids JSONB` not null and constrained to an array.

The role resolver canonicalizes the form UID list and derives `role_key` from
the activity and that list. It rejects an empty set. Require unique
`(activity_uid, form_uids)` after canonicalization so the same role content
cannot be stored under another key. Do not store the derivable hash or a
second creation timestamp. Do not foreign-key baseline UIDs.

#### `assignment_identity_link`

Immutable mapping from one baseline assignment row and actor to an independently
endable generation:

- `assignment_id UUID` primary key;
- `baseline_assignment_uid VARCHAR(11)` not null;
- `target_actor_id UUID` not null and foreign-keyed to
  `actor_identity_link`;
- `generation INTEGER` not null and non-negative;
- unique `(baseline_assignment_uid, target_actor_id, generation)`.

The prior generation is derivable and is not stored. Team remains baseline
compatibility context; it is not duplicated into event identity, role, or
scope. Equivalent grants from different baseline assignment rows remain
distinct here.

#### `assignment_grant_projection`

Replaceable current-state read model for one assignment stream:

- `assignment_id UUID` primary key and foreign-keyed to the identity link;
- `source_event_id UUID` unique, not null, and foreign-keyed to the journal;
- `role_key VARCHAR(128)` not null and foreign-keyed to the role definition;
- `org_unit_id UUID` not null and foreign-keyed to
  `org_unit_identity_link`;
- `lifecycle_state VARCHAR(16)` constrained to `ACTIVE` or `ENDED`.

Create `assignment_access_projection` as a view joining identity, role, and
grant rows and selecting distinct active `target_actor_id`, `activity_uid`,
`org_unit_id`, and `role_key`. Deduplication belongs only in this effective
access view. Index the identity link by `target_actor_id`, which is the active
access lookup key.

A grant update requires the caller's expected current `source_event_id`.
Concurrent commands therefore cannot both advance the same projection; a
losing transaction also rolls back its journal append.

This table is never an independent source of truth. Before cutover, the
DataRun Baseline remains authoritative. After cutover, the journal is
authoritative. Only the assignment projector may advance this table from an
already accepted journal event, and replay must be able to rebuild it exactly.
There is no administrator CRUD or independent business mutation surface.

One shared database trigger function rejects UPDATE and DELETE on
`event_journal`, `actor_identity_link`, `assignment_role_definition`, and
`assignment_identity_link`, plus `org_unit_identity_link`. The grant
projection is the only mutable table.

### Persistence Ports

Create typed JDBC-backed ports under `eventjournal` and `assignmentshadow`:

- append/read journal events without a stream-version input;
- insert/read immutable actor and organization-unit aliases;
- resolve or insert canonical immutable role definitions;
- insert/read identity generations;
- internally apply/read grant state from accepted journal events, with
  expected-source-event concurrency;
- read deduplicated active access.

Immutable ports expose no update/delete methods. Do not expose JDBC maps or JPA
entities. Duplicate event IDs, aliases, generations, and conflicting role
definitions fail instead of overwriting facts.

Keep the grant write port package-owned by `assignmentshadow`; later command
owners call a projector, not the projection store directly.

### Required Tests

PostgreSQL integration tests prove:

- Liquibase creates every table, constraint, index, trigger, and view;
- native UUID/JSONB journal round-trip and duplicate event rejection;
- direct UPDATE/DELETE rejection for every immutable table;
- actor and organization-unit alias uniqueness;
- transaction rollback removes an appended event;
- independent baseline assignment/actor generations remain distinct;
- overlapping grants deduplicate only in the access view;
- ending a grant preserves history but removes effective access;
- conflicting role content cannot reuse a role identity;
- a stale expected source event cannot advance a grant, and the same
  transaction rolls back its journal append.

### Scope

- one new changeset under
  `src/main/resources/config/liquibase/changelog/event-transition/`;
- `src/main/resources/config/liquibase/master.xml`;
- new records, ports, and JDBC implementations only under `eventjournal` and
  `assignmentshadow`;
- focused integration tests for these owners.

### Production Boundary

- Active authority before and after: unchanged DataRun Baseline services.
- API, mobile payloads, existing tables, and existing data: unchanged.
- Shadow population and comparison: next slice.
- Activation: none.
- Rollback: drop only the new view, triggers/function, and six new tables.
- Deployment: prohibited until bootstrap/comparison and production-clone
  migration evidence are complete.

### Excluded Work

- Bootstrap or reconciliation.
- Assignment command interception or dual writes.
- Authorization or capture cutover.
- Administrator endpoints, schedules, startup runners, or flags.
- Analytics, outbox, audit, or legacy-table cleanup.

### Verification

Run focused integration tests, then:

```bash
./mvnw test
./mvnw -Pprod clean verify
git diff --check
```

### Definition Of Done

- The schema and ports contain only the properties above.
- Immutability, concurrency, and rollback are proven against PostgreSQL.
- No existing runtime owner reads or writes the new structures.
- Full gates pass, or an unrelated baseline failure is reported without
  expanding the slice.
- Nothing is deployed.
