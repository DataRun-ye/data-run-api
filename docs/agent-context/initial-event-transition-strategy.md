# Initial Event Transition Strategy

Role: architect-owned execution map for the first production-compatible event
transition

Status: PROPOSED FOR REVIEW

This document defines the bounded, executable transition decisions currently
proposed for review from the production architecture to event-backed
assignment, authorization, and capture ownership.

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
- accepted submission state changes append immutable capture facts while exact
  retries remain idempotent;
- existing mobile requests and configuration responses remain compatible;
- `data_submission` remains available as the baseline-compatible current-state
  projection;
- the current outbox, ETL, tall-table, pivot, and export paths remain
  downstream projections rather than event authority.

No current form JSON, repeat, template-version, submission-pull, synchronized
edit/delete, or review behavior changes in this transition.

The existing analytics `events` table is an ETL projection. It is not the
assignment or capture event journal introduced by this transition.

## Production Evidence

The released path is:

- assignments: `AssignmentResource` -> `DefaultAssignmentService` ->
  `AssignmentFilter` and `AssignmentWithAccessMapper`;
- form access: `CurrentUserDetailsService` ->
  `AssignmentFormAccessService`;
- capture: versioned `DataSubmissionResource` ->
  `SubmissionUploadService` -> `DefaultDataSubmissionService`;
- projection: `data_submission` and `outbox` in the same upload transaction.

The 2026-07-25 production clone establishes the migration shape. In these
counts, an enabled assignment is not soft-deleted and has an enabled activity
and team, matching the active assignment filters:

- all 105,080 enabled assignment rows have an organization-unit scope;
- direct team membership expands to 105,078 actor-assignment rows; canonical
  non-empty capture-form sets reduce these to 105,073 distinct actor, activity,
  role, and organization-unit grants;
- current capture access reduces to 31 non-empty activity-specific
  capture-form sets across 11 activities; operational activities generally use
  one to three sets, while the `test2025` activity accounts for 12 sets held
  by single assignments;
- one additional empty capture-form set belongs to an example warehouse test
  activity. It is reported as compatibility residue and is not promoted into
  a role or capture grant;
- no activity/form-set has more than one current `ADD_SUBMISSIONS` versus
  `EDIT_SUBMISSIONS` permission signature, so collapsing teams to these sets
  loses no current capture-authority distinction;
- two enabled assignments have no direct team user and no submissions;
- every one of the 52,535 submissions has assignment, organization-unit,
  activity, team, and pinned template-version context;
- every stored submission organization unit, team, and activity matches its
  referenced assignment;
- submission UID is unique and remains the DataRun Baseline idempotency
  identity;
- historical same-UID writes exist, so the compatibility path cannot assume
  that every reachable baseline write was an identical retry: outbox history
  contains 2,372 update writes across 655 submission UIDs, including 1,087
  writes with changed form JSON;
- 44,517 submissions have creators that resolve to a current user and 8,018
  retain non-null creator values that no longer resolve;
- assignment retirement is soft state. Active reads exclude `deleted=true`.
  All 90 retired assignments in the clone lack `deleted_at`, although the
  explicit DELETE path now sets it. Two retired assignments retain three
  submissions, so bootstrap treats the flag as authoritative and the timestamp
  as optional evidence.
- organization-unit synchronization currently omits the soft-delete predicate
  and exposes 78 scopes held only by retired assignments to eight users. This
  contradicts assignment retirement and is a baseline defect, not a
  compatibility contract.

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

Each command commits its event append and required baseline compatibility
projection in one database transaction. Capture commands also enqueue the
current outbox in that transaction. ETL, tall-table, pivot, and export
processing remain asynchronous downstream work. There is no permanent
dual-write arrangement and no phase in which two independent stores are both
authoritative.

All schema changes remain additive until event authority, replay, projection
equivalence, and rollback have passed a production-style release cycle.

## Assignment Lane

### Proposed Mapping

- The assignment actor is each enabled direct user of the baseline assignment
  team.
- `Team` is a baseline grouping and compatibility identifier. It is not the
  event role.
- One initial role definition exists per activity and canonical distinct set
  of forms for which baseline assignment and team permissions permit capture.
  Capture permission here means `ADD_SUBMISSIONS` or `EDIT_SUBMISSIONS`,
  matching `AssignmentFormAccessService.canSubmitData`.
  Teams and assignments sharing that set reuse the role; they do not create
  additional role identities. An actor may receive more than one such role
  through assignments exposing different form sets.
- Role identity uses form UIDs, not mutable template names. Human-readable
  names are projection labels only.
- An empty capture-form set produces no role or capture grant. Any such
  baseline assignment remains visible only through the compatibility
  projection until the test residue is removed.
- The initial role owns capture eligibility only. The unchanged mobile
  `canAddSubmissions` and `canEditSubmissions` fields remain a baseline wire
  projection until that distinction receives its own cutover.
- The assignment access scope is its organization unit.
- Ancestor organization units synchronized for hierarchy display are a
  navigation projection, not additional assignment authority.
- Existing assignment and team UIDs remain in a baseline compatibility link;
  they are not reused as event identities.
- Event and assignment-stream identities are UUIDs. The compatibility link
  makes bootstrap and repeated reconciliation idempotent.

An enabled baseline assignment with no direct actor produces no active event
grant and is reported by the bootstrap result. It is not assigned to an
invented actor.

### Shadow

1. Create an idempotent baseline identity link for every assignment, including
   retired assignments retained by historical submissions.
2. Append one explicit bootstrap assignment fact for each distinct enabled
   actor, activity, effective role, and organization-unit grant.
3. Do not fabricate historical assignment changes.
4. Build a current-assignment projection from those facts.
5. Compare baseline and event projections for each actor:
   assignment availability, organization-unit scope, eligible capture forms,
   and duplicate suppression.
6. Keep all released reads and authorization decisions on the baseline while
   any unexplained mismatch remains.

### Cutover

One assignment command owner must handle baseline assignment writes and
baseline team-membership and form-permission changes:

- entering the baseline eligible state through assignment creation or
  restoration, activity/team enablement, or team-membership addition appends
  the required actor grants;
- leaving that state through assignment soft deletion, activity/team
  disablement, or team-membership removal ends affected grants;
- changing assignment forms, team form permissions, team, activity, or
  organization-unit scope ends the affected grant and creates its successor;
- Baseline rows and DTOs become compatibility projections in the same
  transaction.

While baseline assignment commands still carry form sets, the compatibility
adapter resolves or creates exactly one baseline-derived role definition for
each canonical activity/form set before changing grants. It must not create
team-specific duplicate roles.

Every registered baseline assignment, team-membership, team form-permission,
activity-enablement, and team-enablement write path must call this owner.
The owner emits lifecycle facts only for authority-bearing changes and retains
other baseline fields as compatibility projection data. Direct repository
writes that bypass the owner are not a compatibility mechanism.

New assignment endings record their event time and retain baseline soft-delete
projection behavior. Bootstrap does not invent deletion times for historical
rows that have only the `deleted` flag.

The released mobile assignment and assignment-form DTOs remain unchanged.
Their legacy assignment and team identifiers come from the compatibility
link.

## Authorization Lane

Before shadow comparison, organization-unit synchronization must apply the
same assignment soft-delete eligibility used by the released assignment list.
This is a bounded baseline correction; the event projection must not reproduce
the retired-scope leak.

For field-user capture, the selected authorization owner answers one question:

> Does this authenticated actor have an active assignment whose activity,
> organization-unit scope, and role permit capture using this form UID?

The submission owner separately verifies that the pinned template version
belongs to that form.

The released administrator bypass remains an explicit compatibility adapter.
It is compared separately and is not represented as an invented role grant.

The released versioned upload route also resolves an assignment by UID without
requiring that assignment to remain active. Changing that behavior could
reject currently accepted offline work. The initial transition therefore
keeps it behind an explicit baseline upload adapter, measures its use, and
does not treat it as evidence of an active grant. It retires only when
supported clients no longer depend on uploads against retired assignments.

The event-backed decision first runs in shadow beside
`AssignmentFormAccessService`. Comparison covers:

- assignment list inclusion;
- capture-eligible assignment-form inclusion; legacy visibility and action
  flags remain compatibility output;
- organization-unit synchronization scope;
- Reference catalog scope;
- versioned submission upload.

Authorization reads the request-current grant projection. It does not place
scope authority in a cross-request cache or access token.

The event decision becomes authoritative only after these results are
equivalent on production-clone fixtures and a controlled runtime scope.
Authentication, administrator authority, and the released mobile profile
remain separate compatibility concerns.

## Capture Lane

### Proposed Mapping

- The baseline submission UID remains the external idempotency alias.
- Existing `data_submission.id` and `serial_number` values remain compatibility
  projection identities for the current outbox and ETL. They are not event
  identities.
- Every immutable capture fact has its own UUID.
- The authenticated user is the actor for live capture.
- Historical bootstrap resolves the stored creator to the existing user when
  possible; otherwise it creates a stable migration-only actor identity from
  the stored creator value rather than inventing a current user.
- The subject for the baseline compatibility path is the assignment
  organization unit.
- Every capture fact records the canonical baseline assignment identity; team
  identity/code; activity identity; organization-unit identity/code/name; form
  and pinned template-version identity/number; status and soft-delete state;
  entry timestamps; server actor/audit times; and whole form JSON.
- Field-user live captures also record the authorizing role/scope grant.
  Administrator compatibility writes record their explicit authorization
  basis instead of an invented grant. Historical bootstrap preserves the
  stored assignment context but does not fabricate a grant that the current
  database cannot prove.
- Repeat rows remain embedded in form JSON.

Capture facts are built from the accepted server-canonical state after
assignment context, pinned template version, repeat metadata, Reference
resolution, and baseline upsert semantics have been applied. Raw client DTOs
are not event facts.

### Same-UID Compatibility

- If applying the baseline upsert rules produces no change in the canonical
  current-state projection, the retry appends no event.
- If an accepted baseline compatibility write changes that projected state,
  it appends a new immutable capture fact linked as the successor of the prior
  fact.
- The current projection points to the latest accepted fact.
- This preserves reachable baseline behavior without defining a new edit,
  delete, review, or conflict product policy.

### Shadow And Cutover

1. Backfill one explicit bootstrap capture fact from each current
   `data_submission` row. Do not reconstruct history that the database cannot
   prove.
2. Append shadow facts for accepted projected-state changes from versioned
   uploads.
3. Compare the event projection with `data_submission`, including retries,
   changed same-UID compatibility writes, soft-delete state, pinned versions,
   and outbox payload inputs.
4. Cut capture authority to event append only after replay is equivalent
   across every baseline compatibility field. Physical optimistic-lock
   counters are projection mechanics, not event semantics.
5. Continue writing `data_submission` and the current outbox as compatibility
   and downstream projections in the event transaction.

## Delivery Sequence

1. **Baseline assignment eligibility correction**
   Exclude soft-deleted assignments from organization-unit synchronization and
   characterize the retained upload compatibility path. Do not change upload
   acceptance in this slice.
2. **Assignment shadow foundation**
   Add the append-only assignment facts, baseline identity links, bootstrap,
   current projection, and deterministic comparison report. No active read or
   authorization change.
3. **Assignment command ownership**
   Route baseline assignment and team-membership changes through one command
   owner while the baseline remains authoritative.
4. **Authorization shadow and cutover**
   Compare all five active decision surfaces, then enable event-backed capture
   authorization in a controlled scope.
5. **Capture shadow foundation**
   Add immutable capture facts, baseline submission aliases, current
   projection, bootstrap, and replay comparison.
6. **Capture authority cutover**
   Make event append authoritative while preserving baseline upload and
   downstream projections.
7. **Compatibility contraction**
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

Each accepted slice is published as a complete implementation blueprint before
work starts. The blueprint contains the evidence, resulting behavior,
compatibility boundary, verification, and closure conditions required for that
slice.
