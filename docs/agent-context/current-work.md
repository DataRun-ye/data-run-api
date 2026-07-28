# Current Work

Updated: 2026-07-28

Status: ACCEPTED FOR IMPLEMENTATION

## Assignment Shadow Persistence Foundation

### Outcome

Create the additive, append-only persistence boundary required for assignment
event shadowing. This slice defines storage and tested persistence ports only;
it does not bootstrap data, intercept assignment writes, change active reads,
or expose a runtime endpoint.

This foundation is consumed by the next bootstrap/comparison slice on this
branch and must not be deployed or merged to production on its own.

### Current Evidence

- `assignment` is the mutable production authority and has no lifecycle
  sequence.
- `analytics.events` is a mutable ETL projection of submission/repeat rows. It
  is not an event journal and must not be reused.
- `outbox` is submission/ETL-specific. `outbox_event` is inactive schema
  residue. Neither owns command history.
- `app_entity_audit_event` is asynchronous and cannot guarantee atomic append
  or stream ordering.
- PostgreSQL `uuid`, JSONB, `NamedParameterJdbcTemplate`, and application-side
  UUID generation are established repository conventions.

### Storage Contract

Add one Liquibase changeset at the end of `master.xml` with explicit rollback.
Use these names so the new authority cannot be confused with ETL projections.

#### `event_journal`

Append-only server journal shared by the accepted assignment and capture
transition lanes:

- `journal_position BIGSERIAL` primary key;
- `event_id UUID` unique and not null;
- `event_type VARCHAR(64)` not null;
- `shape_ref VARCHAR(128)` not null;
- `activity_ref VARCHAR(11)` nullable;
- `subject_type VARCHAR(32)` not null;
- `subject_id UUID` not null;
- `subject_version BIGINT` not null and greater than zero;
- `actor_id VARCHAR(128)` not null;
- `recorded_at TIMESTAMP WITH TIME ZONE` not null;
- `payload JSONB` not null and constrained to a JSON object.

Require unique `(subject_type, subject_id, subject_version)`. Index
`(subject_type, subject_id, journal_position)` and
`(event_type, journal_position)`. Add a database trigger that rejects UPDATE
and DELETE. Rollback drops the trigger/function and table. The journal append
port must require the expected next subject version so concurrent lifecycle
appends cannot both succeed.

Do not add device IDs, device sequences, sync watermarks, review flags, future
event types, or submission fields in this slice.

#### `assignment_role_definition`

Immutable role definition derived later from one activity and one canonical
capture-form set:

- `role_key VARCHAR(128)` primary key;
- `activity_uid VARCHAR(11)` not null;
- `form_set_hash CHAR(64)` not null;
- `form_uids JSONB` not null and constrained to an array;
- `created_at TIMESTAMP WITH TIME ZONE` not null;
- unique `(activity_uid, form_set_hash)`.

Do not foreign-key baseline UIDs. Historical facts must survive later baseline
row contraction.

#### `assignment_identity_link`

Maps one baseline assignment row and direct actor to independently endable
grant generations:

- `baseline_assignment_uid VARCHAR(11)` not null;
- `target_actor_uid VARCHAR(11)` not null;
- `generation INTEGER` not null and non-negative;
- `assignment_id UUID` unique and not null;
- `baseline_team_uid VARCHAR(11)` not null;
- `predecessor_assignment_id UUID` nullable;
- primary key `(baseline_assignment_uid, target_actor_uid, generation)`;
- self foreign key from predecessor to `assignment_id`.

No actor is invented and equivalent grants from different baseline assignment
rows are not collapsed here.

#### `assignment_grant_projection`

Current state of each independently endable assignment stream:

- `assignment_id UUID` primary key and foreign key to the identity link;
- `source_event_id UUID` unique, not null, and foreign key to the journal;
- `role_key VARCHAR(128)` not null and foreign key to the role definition;
- `target_actor_uid VARCHAR(11)` not null;
- `activity_uid VARCHAR(11)` not null;
- `org_unit_uid VARCHAR(11)` not null;
- `lifecycle_state VARCHAR(16)` constrained to `ACTIVE` or `ENDED`;
- `valid_from TIMESTAMP WITH TIME ZONE` nullable;
- `valid_to TIMESTAMP WITH TIME ZONE` nullable;
- `updated_at TIMESTAMP WITH TIME ZONE` not null.

Create `assignment_access_projection` as a view selecting distinct active
`target_actor_uid`, `activity_uid`, `org_unit_uid`, and `role_key`. Deduplication
belongs in this effective-access projection, not the identity link or journal.

### Persistence Ports

Create small JDBC-backed ports under clear packages such as
`org.nmcpye.datarun.eventjournal` and
`org.nmcpye.datarun.assignmentshadow`:

- append and read journal events;
- insert/read immutable role definitions;
- insert/read identity generations;
- insert/update/read grant projection state;
- read the deduplicated active-access projection.

The journal port exposes no update or delete operation. Use typed Java records
for inputs/results; do not expose JDBC maps or JPA entities. Duplicate event
IDs, duplicate stream generations, and conflicting role definitions must fail
rather than silently overwrite existing facts.

### Required Tests

Add focused PostgreSQL integration tests proving:

- Liquibase creates every table, constraint, index, trigger, and view;
- a journal event round-trips with native UUID and JSONB values;
- duplicate event IDs fail;
- duplicate subject versions fail;
- UPDATE and DELETE of a journal row fail;
- transaction rollback removes an appended event;
- independent baseline assignment/actor generations remain distinct;
- duplicate effective active grants collapse only in
  `assignment_access_projection`;
- ended grants disappear from effective access while their identity, event,
  and grant state remain stored;
- conflicting role content cannot reuse an activity/form-set identity.

### Scope

- one new Liquibase changeset under
  `src/main/resources/config/liquibase/changelog/event-transition/`;
- `src/main/resources/config/liquibase/master.xml`;
- new persistence records/ports/implementations only under
  `eventjournal` and `assignmentshadow` packages;
- focused integration tests for those new owners.

### Production Boundary

- Authority before and after: mutable baseline assignment/access services.
- API and mobile payloads: unchanged.
- Existing tables and data: unchanged; all schema is additive.
- Shadow comparison: not active until the next slice populates these tables.
- Activation: none in this slice.
- Rollback: drop only the new view, trigger/function, and four new tables.
- Deployment: prohibited until bootstrap/comparison and production-clone
  migration evidence are complete.

### Excluded Work

- Bootstrap, reconciliation, or production-clone data population.
- Assignment command interception or dual writes.
- Authorization reads or cutover.
- Capture event persistence.
- Administrator endpoints, schedules, startup runners, or feature flags.
- Reuse or cleanup of analytics events, outbox, audit, or inert legacy tables.

### Verification

Run focused integration tests chosen for the new ports, then:

```bash
./mvnw test
./mvnw -Pprod clean verify
git diff --check
```

### Definition Of Done

- The schema and ports implement exactly this contract.
- Append-only and transaction behavior are proven against PostgreSQL.
- No existing runtime owner reads or writes the new structures.
- The full test/build gate passes, or unrelated baseline failures are reported
  without expanding this slice.
- No production deployment is performed.
