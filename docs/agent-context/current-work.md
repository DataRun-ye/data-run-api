# Current Work

Updated: 2026-07-28

Status: ACCEPTED FOR IMPLEMENTATION

## Assignment Shadow Bootstrap And Comparison

### Outcome

Create one explicit, idempotent operation that snapshots the DataRun Baseline
assignment authority into the additive event/shadow tables and proves exact
capture-authority equivalence. Active reads and writes remain on the baseline.

The operation must be safe to run repeatedly against an isolated production
clone. It is not an endpoint, schedule, startup migration, or automatic
production action.

### Baseline Selection

Read assignment/actor rows from the current persistence model using the same
eligibility as released request-time access:

- assignment team and activity are enabled;
- actor is an activated direct member of the assignment team;
- managed teams, user groups, and administrator bypass are excluded;
- organization unit, assignment/team/activity/user UIDs, and required JSON are
  present and valid;
- an active row has `assignment.deleted != true`;
- a retired row has `assignment.deleted == true`.

For each assignment, the canonical capture-form set is the sorted distinct
intersection of:

- `assignment.forms`; and
- same-team form permissions containing `ADD_SUBMISSIONS` or
  `EDIT_SUBMISSIONS`.

Do not treat an empty permission array or unrelated view/delete permissions as
capture authority. Do not apply the mobile Reference capability gate while
creating roles; that gate is wire compatibility, not role authority.

Rows with no direct actor or an empty capture-form set create no stream or
grant and are reported separately. Disabled team/activity rows are also
reported, not promoted. Null scope or malformed identity/permission data is a
hard failure.

### Deterministic Mapping

Use UTF-8 namespaced `UUID.nameUUIDFromBytes` values so repeated bootstrap runs
resolve the same immutable identities without a new dependency:

```text
datarun-baseline/actor/{userUid}
datarun-baseline/org-unit/{orgUnitUid}
datarun-baseline/assignment/{assignmentUid}/actor/{userUid}/generation/0
datarun-baseline/event/assignment-observed/{assignmentId}
```

For every eligible assignment/actor/form-set row:

1. resolve or verify the actor and organization-unit aliases;
2. resolve the existing canonical activity/form-set role owner;
3. resolve or verify generation `0` of the assignment identity;
4. append or verify one deterministic `assignment_changed` event;
5. insert or verify the grant projection as `ACTIVE` or `ENDED` from the
   baseline soft-delete flag.

The bootstrap event contract is:

```text
shape_ref: baseline_assignment_observed/v1
activity_ref: baseline activity UID
subject_type: assignment
subject_id: deterministic assignment UUID
actor_id: system:migration/datarun-baseline-assignment-bootstrap
recorded_at: actual first bootstrap recording time
payload:
  role: canonical role key
  org_unit_id: aliased organization-unit UUID
  lifecycle_state: ACTIVE | ENDED
```

This shape records an observed migration fact. It intentionally does not claim
historical creation or ending times that the baseline cannot prove. The target
actor is already immutable in `assignment_identity_link`; activity is in the
envelope/role; the baseline assignment UID is in the identity link. Do not
duplicate them in the payload.

On rerun, an existing deterministic event retains its original `recorded_at`;
all other immutable content must match. Any conflicting alias, role, identity,
event, or grant is a hard failure rather than an overwrite.

### Transaction And Throughput

- Run in one `REPEATABLE_READ` transaction.
- Acquire one transaction-scoped PostgreSQL advisory lock dedicated to
  `assignment-shadow-bootstrap/v1`.
- Read one stable baseline snapshot and process in deterministic assignment UID
  then actor UID order.
- Use bounded JDBC batches, no larger than 1,000 rows, for the high-cardinality
  identity/event/grant writes. Do not perform hundreds of thousands of
  one-row round trips or load unbounded result sets.
- Exact existing rows are no-ops; conflicts abort the operation.
- A failed comparison rolls back every insertion made by that run. Sequence
  gaps after rollback are acceptable.
- Do not lock baseline tables for the whole operation. Snapshot isolation is
  sufficient while baseline remains authoritative and avoids blocking
  operational writes.

Keep bootstrap SQL/storage under a named transition package. Runtime command
owners continue using the typed event and assignment-shadow ports; this
one-time backfill adapter is not a second permanent owner.

### Comparison Contract

Compare distinct capture-authority tuples:

```text
(baseline user UID, activity UID, organization-unit UID, form UID)
```

Baseline tuples use active assignments and the exact direct-user capture-form
selection above. Shadow tuples expand active role form UIDs from
`assignment_access_projection` and join immutable aliases back to baseline
UIDs.

The report contains:

- baseline and shadow tuple counts;
- `baseline EXCEPT shadow` and `shadow EXCEPT baseline` counts;
- at most ten deterministic samples from either mismatch;
- created/existing counts for aliases, roles, identities, events, active
  grants, and ended grants;
- excluded counts for retired, disabled, no-actor, empty-form-set, null-scope,
  and malformed rows;
- raw active-grant count, distinct effective-access count, and overlap count.

Success requires both tuple differences to be zero and every eligible retired
row to have an ended grant. Empty-form/no-actor compatibility residue is
reported but cannot be called event-backed authority. Administrator bypass and
legacy view/edit/delete wire flags are explicitly outside this equality claim.

### Invocation Boundary

Provide one property-gated, operator-only command path that runs with the web
application disabled, prints the bounded report, exits nonzero on conflict or
mismatch, and closes the application context after completion. It is disabled
by default and must never be exposed as an HTTP endpoint or scheduled task.

The checked-in wrapper command must require explicit database environment
values and the bootstrap opt-in. It must not contain, infer, or print secrets.

### Required Tests

PostgreSQL integration tests prove:

- active and retired rows map to active and ended grants respectively;
- disabled, no-actor, and empty-form rows follow the rules above;
- only add/edit permissions enter canonical capture roles;
- overlapping assignments remain separate streams but deduplicate in access;
- deterministic identities and an exact rerun are idempotent;
- an existing conflicting immutable row aborts without overwriting;
- comparison mismatch rolls back the entire run;
- a fixture larger than one batch completes and reruns exactly;
- the operator command is disabled by default and closes after an explicit run.

After focused tests and the full release gate, run the command against the
isolated production clone using a disposable clone-only database role. Record
the factual report outside this active handoff. Do not connect to or modify
production.

### Scope

- new bootstrap/snapshot/report/command classes under
  `assignmentshadow.bootstrap`;
- one secret-free operator wrapper under `scripts/transition/`;
- focused unit/PostgreSQL integration tests;
- no Liquibase changes unless a test exposes a defect in the already accepted
  persistence schema, in which case stop and return to architect review.

### Production Boundary

- Baseline assignment, access, configuration, and upload owners: unchanged.
- Existing API/mobile contracts: unchanged.
- Existing tables and data: unchanged.
- New shadow tables: populated only when the explicit command is invoked.
- Production deployment or invocation: prohibited in this slice.

### Definition Of Done

- The bootstrap and comparison rules above are implemented once in one owner.
- Synthetic integration tests and isolated production-clone comparison pass.
- Throughput is bounded and reruns are exact.
- Default application startup performs no bootstrap work.
- Full `scripts/release/verify.sh` passes from the committed clean tree.
- No active read, write, authorization, or deployment behavior changes.
