# Initial Event Transition Strategy

Role: architect-owned execution map for the first production-compatible event
transition

Status: PROPOSED FOR REVIEW

This document is not default implementation-agent context. It contains only
the accepted, executable transition from the current production architecture
to event-backed assignment, authorization, and capture ownership. It does not
import unfinished platform specifications or reserve abstractions for
unaccepted capabilities.

`DataRun Baseline` is the identity name for the architecture currently used
in production. It is not an API namespace, Maven version, or application
release number. `Event architecture` means only the three bounded ownership
changes defined here.

## Outcome

The initial transition is complete when:

- assignment authority is reconstructed from immutable assignment lifecycle
  facts;
- capture authorization is decided from an actor's activity role and
  organization-unit scope;
- accepted submissions append immutable capture facts;
- existing mobile requests and configuration responses remain compatible;
- `data_submission` remains available as the current compatibility and
  projection read model;
- the current outbox, ETL, tall-table, pivot, and export paths remain
  downstream projections rather than event authority.

No current form JSON, repeat, template-version, submission-pull, synchronized
edit/delete, or review behavior changes in this transition.

## Production Evidence

The released path is:

- assignments: `AssignmentResource` -> `DefaultAssignmentService` ->
  `AssignmentFilter` and `AssignmentWithAccessMapper`;
- form access: `CurrentUserDetailsService` ->
  `AssignmentFormAccessService`;
- capture: versioned `DataSubmissionResource` ->
  `SubmissionUploadService` -> `DefaultDataSubmissionService`;
- projection: `data_submission` and `outbox` in the same upload transaction.

The 2026-07-25 production clone establishes the migration shape:

- all 105,080 enabled assignment rows have an organization-unit scope;
- direct team membership expands to 105,078 actor grants and 105,075 distinct
  actor, activity, role, and organization-unit grants;
- current capture access reduces to 32 activity-specific effective role
  profiles across 12 enabled activities;
- every one of the 52,535 submissions has assignment, organization-unit,
  activity, team, and pinned template-version context;
- every stored submission organization unit, team, and activity matches its
  referenced assignment;
- submission UID is unique and remains the DataRun Baseline idempotency
  identity;
- historical same-UID writes exist, so the compatibility path cannot assume
  that every reachable baseline write was an identical retry.

Empty legacy role, assignment-member, assignment-template, and party-binding
tables are not migration inputs.

## Authority Migration

Each boundary moves through one unambiguous sequence:

```text
DataRun Baseline authority
  -> baseline authority with event shadow and comparison
  -> event authority with baseline compatibility projection
  -> retired baseline write owner
```

The event journal and required projections use one database transaction.
There is no permanent dual-write arrangement and no phase in which two
independent stores are both authoritative.

All schema changes remain additive until event authority, replay, projection
equivalence, and rollback have passed a production-style release cycle.

## Assignment Lane

### Accepted Mapping

- The assignment actor is each enabled direct user of the baseline assignment
  team.
- `Team` is a baseline grouping and compatibility identifier. It is not the
  event role.
- An initial role is the activity-specific effective set of forms for which
  the baseline assignment and team permissions currently permit capture.
- The assignment geographic scope is its organization unit.
- Existing assignment and team UIDs remain in a baseline compatibility link;
  they are not reused as event identities.
- Event and assignment-stream identities are UUIDs. The compatibility link
  makes bootstrap and repeated reconciliation idempotent.

An enabled baseline assignment with no direct actor produces no active event
grant and is reported by the bootstrap result. It is not assigned to an
invented actor.

### Shadow

1. Append one explicit bootstrap assignment fact for each distinct enabled
   actor, activity, effective role, and organization-unit grant.
2. Do not fabricate historical assignment changes.
3. Build a current-assignment projection from those facts.
4. Compare baseline and event projections for each actor:
   assignment availability, organization-unit scope, eligible capture forms,
   and duplicate suppression.
5. Keep all released reads and authorization decisions on the baseline while
   any unexplained mismatch remains.

### Cutover

One assignment command owner must handle baseline assignment writes and
baseline team-membership changes:

- creation or membership addition appends the required actor grants;
- removal, disabling, or authority-bearing scope/role changes end affected
  grants;
- an authority-bearing replacement ends the previous grant and creates its
  successor;
- Baseline rows and DTOs become compatibility projections in the same
  transaction.

The released mobile assignment and assignment-form DTOs remain unchanged.
Their legacy assignment and team identifiers come from the compatibility
link.

## Authorization Lane

The selected authorization owner answers one question:

> Does this authenticated actor have an active assignment whose activity,
> organization-unit scope, and role permit capture with this pinned form?

The event-backed decision first runs in shadow beside
`AssignmentFormAccessService`. Comparison covers:

- assignment list inclusion;
- assignment-form projection;
- organization-unit synchronization scope;
- Reference catalog scope;
- versioned submission upload.

The event decision becomes authoritative only after these results are
equivalent on production-clone fixtures and a controlled runtime scope.
Authentication, administrator authority, and the released mobile profile
remain separate compatibility concerns.

## Capture Lane

### Accepted Mapping

- The baseline submission UID remains the external idempotency alias.
- Every immutable capture fact has its own UUID.
- The authenticated user is the actor for live capture.
- Historical bootstrap resolves the stored creator to the existing user when
  possible; otherwise it creates a stable migration-only actor identity from
  the stored creator value rather than inventing a current user.
- The subject for the baseline compatibility path is the assignment
  organization unit.
- Activity, role/scope grant, pinned template version, entry timestamps, and
  whole form JSON are recorded with the capture fact.
- Repeat rows remain embedded in form JSON.

### Same-UID Compatibility

- An exact retry of the current canonical payload is idempotent and appends no
  event.
- A different payload accepted by an existing baseline compatibility route
  appends a new immutable capture fact linked as the successor of the prior
  fact.
- The current projection points to the latest accepted fact.
- This preserves reachable baseline behavior without defining a new edit,
  delete, review, or conflict product policy.

### Shadow And Cutover

1. Backfill one explicit bootstrap capture fact from each current
   `data_submission` row. Do not reconstruct history that the database cannot
   prove.
2. Append shadow facts for accepted versioned uploads.
3. Compare the event projection with `data_submission`, including retries,
   changed same-UID compatibility writes, soft-delete state, pinned versions,
   and outbox payload inputs.
4. Cut capture authority to event append only after replay produces the
   baseline current projection exactly.
5. Continue writing `data_submission` and the current outbox as compatibility
   and downstream projections in the event transaction.

## Delivery Sequence

1. **Assignment shadow foundation**
   Add the append-only assignment facts, baseline identity links, bootstrap,
   current projection, and deterministic comparison report. No active read or
   authorization change.
2. **Assignment command ownership**
   Route baseline assignment and team-membership changes through one command
   owner while the baseline remains authoritative.
3. **Authorization shadow and cutover**
   Compare all five active decision surfaces, then enable event-backed capture
   authorization in a controlled scope.
4. **Capture shadow foundation**
   Add immutable capture facts, baseline submission aliases, current
   projection, bootstrap, and replay comparison.
5. **Capture authority cutover**
   Make event append authoritative while preserving baseline upload and
   downstream projections.
6. **Compatibility contraction**
   Remove baseline write owners only after supported clients and operational
   tools use the event-backed boundaries. Physical table contraction remains
   a later, separately verified migration.

## Slice Gate

Every implementation handoff must state:

```text
Authority before
Authority after
Baseline compatibility owner
Schema and backfill
Shadow comparison
Activation boundary
Rollback position
Retirement criterion
```

The implementation agent receives only the current handoff and the current
production boundary it touches. It does not receive this complete strategy or
the temporary source material unless the architect explicitly requests a
review.

Discoveries outside the handoff either block the architect's accepted design
or remain outside the slice. They do not become speculative hooks, TODO
models, or addenda in implementation code.
