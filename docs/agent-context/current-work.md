# Current Work

Updated: 2026-07-28

Status: ACCEPTED FOR IMPLEMENTATION

## Live Capture Facts In Shadow

### Outcome

Append one immutable capture fact for each actual state mutation accepted
through the released versioned submission upload, and atomically point the
capture-current projection at that fact.

This is still a shadow slice:

- `data_submission` remains the active current-state authority;
- the current `outbox` remains the active downstream write;
- HTTP requests, responses, authorization outcomes, form JSON, repeat
  behavior, and ETL behavior remain unchanged;
- exact same-UID retries append no event;
- live shadowing is disabled by default and is not enabled in production in
  this slice.

### Authority Before And After

Before this slice, immutable capture facts and current pointers exist only for
the production-clone bootstrap boundary. Released uploads do not append or
advance them.

After this slice, the released versioned upload remains the baseline command
authority, but every actual `CREATE`, `UPDATE`, or `DELETE` mutation can also
append an equivalent immutable fact in the same transaction. `UNCHANGED`
remains a successful persistence, outbox, and event no-op.

No second mutable capture authority is introduced. The current pointer is a
rebuildable projection over immutable facts.

### Active Entry And Compatibility Boundary

The owned entry is:

```text
POST /api/v1/dataSubmission/bulk?referenceVersion=1
  -> DataSubmissionResource.saveVersionedUpload
  -> SubmissionUploadService
  -> VersionedUploadEventAuthorizer
  -> capture command
  -> DefaultDataSubmissionService
  -> data_submission + outbox + capture fact + current pointer
```

The `/api/custom` alias reaches the same handler and is covered by the same
command.

Do not shadow or alter these separately registered compatibility surfaces:

- unversioned `POST /api/{v1,custom}/dataSubmission/bulk`;
- inherited single and `/return` submission writes;
- inherited administrator `PUT` and `DELETE`.

They are not used by the released mobile, but their reachability makes them a
capture-authority cutover blocker. Before event append becomes authoritative,
each must be retired or routed through an explicit command adapter. They must
not be hidden behind a generic repository listener.

### Classified Baseline Mutation

The current summary cannot drive event append because `updated` combines real
updates, deletes, and unchanged retries. Add an internal mutation result:

```text
SubmissionMutationKind = CREATE | UPDATE | DELETE | UNCHANGED

SubmissionMutationResult
  kind
  persisted submission
```

`DefaultDataSubmissionService` remains the only classifier. Classification
must follow its actual persistence branches:

- absent UID: `CREATE`, including a newly inserted soft-deleted row;
- existing active row first receiving `deleted=true`: `DELETE`;
- existing row with changed mutable persisted state: `UPDATE`;
- exact mutable-state retry or repeated delete: `UNCHANGED`.

Return post-flush entities so generated physical ID, serial number, audit
fields, server deletion time, and optimistic-lock state are final.

Keep the existing `upsert` and `upsertAll` API as a compatibility adapter over
the classified method. Preserve its returned entities, summary contents,
outbox events, and transaction behavior. The released upload uses the
classified method through the capture command.

One versioned bulk request may contain each submission UID at most once.
Reject a duplicate UID before authorization or persistence so one command
cannot carry two competing final states or authority receipts for one capture.
The released mobile does not intentionally send duplicate UIDs.

### Accepted Authority Receipt

`VersionedUploadEventAuthorizer.Session.authorize` must return the exact
accepted basis instead of only returning `void`.

For field-user acceptance, return:

- the authenticated actor UUID already resolved through
  `actor_identity_link`;
- the source event UUID of the accepted assignment-grant projection.

That one grant event resolves the grant-generation identity and whether it was
active or ended. Do not copy role, scope, generation number, or lifecycle
state into the capture fact.

For administrator acceptance, return:

- the deterministic authenticated actor UUID;
- an explicit administrator basis with no invented assignment grant.

Extend the internal assignment-grant read model only with its existing
`assignment_id` and `source_event_id` columns as needed. Denial behavior and
released error codes remain unchanged.

Extract one small transition-identity resolver from the existing assignment
lifecycle logic so assignment and capture commands enforce the same
deterministic user-UID actor alias and baseline organization-unit alias. Do not
create second actor or organization-unit identity strategies.
The authority receipt computes or carries identity; it does not persist an
alias. When shadowing is enabled and an actual mutation exists, the capture
command transactionally requires or creates the exact actor alias and
organization-unit alias before append. When shadowing is disabled or the
mutation is `UNCHANGED`, it creates no transition alias.

### Canonical Live Fact

Use this event envelope:

```text
event_id       random UUID generated once by the server command
event_type     capture
shape_ref      capture_state_accepted/v1
activity_ref   canonical persisted activity UID
subject_type   org_unit
subject_id     existing aliased organization-unit UUID
actor_id       authenticated actor UUID string
recorded_at    server clock
payload        versioned JSON below
```

The payload is:

```json
{
  "captureId": "<UUID>",
  "previousEventId": "<UUID or null>",
  "acceptance": {
    "kind": "assignment",
    "grantEventId": "<UUID>"
  },
  "submission": {}
}
```

Administrator acceptance uses only:

```json
{
  "kind": "administrator"
}
```

`submission` has exactly the canonical fields already emitted by
`baseline_submission_captured/v1`. Extract the existing canonical submission
builder from the bootstrap-only class and reuse it for both source rows and
post-flush entities. Existing bootstrap payload bytes and source fingerprints
must remain unchanged.

The acceptance object is payload provenance, not an authorization decision
stored in the event envelope. For assignment acceptance, the referenced grant
event must exist and remain the exact event used by the authorizer. Its
assignment subject must resolve through `assignment_identity_link` to the same
baseline assignment UID and actor carried by the capture. Replay validates
that relationship. Administrator acceptance has no grant event.

Do not add copied role/scope fields, workflow state, flags, client DTO values,
lock version, outbox values, or speculative event-envelope fields.

### Identity And Lineage

The capture ID remains the deterministic UUID alias of the baseline submission
UID.

Extract the exact capture identity-link read/insert rules from bootstrap into
one shared port. Bootstrap and live append must not maintain separate SQL or
conflict semantics for the same immutable alias.

For `CREATE`:

1. require the post-flush physical submission ID and serial number;
2. insert one exact `capture_identity_link`;
3. require the actor and organization-unit aliases;
4. append the live fact with `previousEventId=null`;
5. strictly insert its initial current pointer.

For `UPDATE` or `DELETE`:

1. require the existing capture identity to match UID, physical ID, and serial;
2. lock/read its current pointer;
3. require the pointed event to belong to this capture:
   - the deterministic bootstrap event for this capture, or
   - a live event whose payload has this exact `captureId`;
4. append the live fact with that pointer as `previousEventId`;
5. compare-and-swap the pointer from the expected event to the new event.

The compare-and-swap updates exactly one row:

```text
capture_id = expected capture
source_event_id = expected predecessor
```

Zero affected rows is a conflict. Event append, pointer movement,
`data_submission`, and outbox must then roll back together. The current
projection port exposes strict initial insert, read, and compare-and-swap; it
does not expose an unconditional update.

`UNCHANGED` performs none of the identity, alias, event, or pointer writes.
For a multi-capture batch, lock and append actual mutations in deterministic
capture-ID order so reversed request order cannot create avoidable pointer
deadlocks.

### Command And Transaction Ownership

Introduce one versioned capture command between `SubmissionUploadService` and
baseline persistence.

`SubmissionUploadService` remains a wire/canonicalization adapter. It resolves
assignment and pinned template, canonicalizes context, obtains the accepted
authority receipt, generates repeat metadata, resolves Reference definitions,
then gives the accepted commands to the capture command.

The capture command:

1. invokes classified baseline persistence;
2. if live shadowing is disabled, returns the unchanged summary;
3. if enabled, appends only actual mutation facts and advances their pointers.

Keep the existing outer transaction. Do not use after-commit append,
`REQUIRES_NEW`, an entity listener, or an asynchronous worker. Shadow failure
must roll back baseline state and outbox rather than create divergence.

Add one typed setting under the existing `datarun` properties:

```text
datarun.transition.capture-live-shadow-enabled=false
```

The default is false in every profile. Enabling requires exact capture
bootstrap and current-projection replay first. This slice does not enable it
in production.

### Replay And Comparison

Live append must not make the current projection unrebuildable.

Extend capture replay to understand both accepted shapes:

- `baseline_submission_captured/v1` is the optional first fact for rows inside
  the pinned bootstrap boundary;
- `capture_state_accepted/v1` carries its capture ID and predecessor;
- a post-bootstrap created capture starts with a live fact whose predecessor
  is null;
- every later live fact points to exactly one earlier fact of the same capture.

For every identity, replay must reject:

- an unknown event shape;
- a missing or cross-capture predecessor;
- a fork, cycle, or more than one head;
- a present current pointer that does not equal the derived head;
- a derived head whose canonical submission differs from current
  `data_submission`;
- conflicting or unrelated identity/current rows.

Historical facts before the derived head are validated for shape, capture
identity, predecessor linkage, envelope consistency, and canonical payload
structure. They are not compared to current `data_submission`, because a later
accepted fact legitimately supersedes their state.

Replay has two explicit modes:

- validation requires every identity to have the exact derived current pointer
  and fails on a missing, differing, or extra pointer;
- repair may insert a missing derived pointer, but still fails on a differing
  or extra pointer and never overwrites one.

Every journal row with `event_type=capture` and either accepted capture shape
must resolve to exactly one capture identity. An orphan live event, duplicate
ownership, or capture event with an unknown shape is a replay failure; replay
must not discover events only by walking outward from identities.

When current pointers alone are removed, replay reconstructs their derived
heads in repair mode without inserting, deleting, or changing journal facts or
identities.
Use bounded pages; do not load all submission JSON into one unbounded
collection.

The existing bootstrap checkpoint remains unchanged. It certifies only its
pinned bootstrap boundary and is not rewritten by live events.

### Tests

Use small deterministic PostgreSQL fixtures for the implementation loop.
Focused tests must prove:

- exact classification for create, update, delete, repeated delete, and
  unchanged retry while preserving summaries and outbox behavior;
- duplicate UIDs in one versioned request fail before any write;
- active-grant, ended-grant, and administrator receipts use exact actor/grant
  event identity; denials and error codes do not change;
- disabled shadowing performs no transition writes;
- create, update, delete, and exact retry produce event counts `1, 1, 1, 0`;
- ordinary and Reference-capable submissions use the same command;
- canonical live and bootstrap submission snapshots are field-identical;
- a field-user fact references the grant event actually used for acceptance;
- assignment acceptance cannot reference another actor's or another baseline
  assignment's grant event;
- administrator facts contain no invented grant;
- wrong identity, wrong pointer, cross-capture predecessor, stale
  compare-and-swap, or append failure rolls back submission, outbox, event,
  and pointer together;
- concurrent same-UID mutations cannot create two accepted heads;
- overlapping bulk mutations presented in opposite input order do not
  deadlock and still produce one chain per capture;
- bootstrap-only, mixed bootstrap/live, and live-only capture chains replay to
  one exact head;
- projection-only deletion and bounded replay restore all pointers without
  changing journal count or deterministic journal-content checksum;
- forks, cycles, missing/cross-capture predecessors, orphan live events,
  unknown capture shapes, extra identities/pointers, and current-head
  canonical-state differences are reported and fail;
- with live shadowing enabled, the `/api/custom` versioned alias appends through
  the same command;
- with live shadowing enabled, every unversioned bulk/single/return and
  inherited administrator PUT/DELETE compatibility route remains behaviorally
  unchanged and appends no live shadow fact.

Run focused tests and then `scripts/release/verify.sh`.

### Final Production-Clone Gate

Run the full clone only once after code review and the full release gate:

1. restore the untouched 2026-07-25 dump;
2. apply candidate migrations;
3. run assignment and capture bootstrap and require their pinned exact
   results;
4. enable live shadow only for the disposable candidate process;
5. exercise one ordinary and one Reference fixture through versioned HTTP
   create, actual update, exact retry, delete, and repeated delete;
6. require baseline rows, outbox operations, immutable facts, authority
   receipts, and current pointers to match the focused contract;
7. run full capture comparison across the 52,535 baseline rows plus disposable
   fixtures;
8. record journal count/checksum, remove only current pointers, replay once,
   and require exact reconstruction with unchanged journal count/checksum;
9. rerun comparison and require zero writes and zero differences;
10. remove disposable fixtures and restore the untouched dump.

Do not connect to or deploy production.

### Activation, Rollback, And Retirement

Activation boundary: code and schema may land with the setting false. A future
production enablement requires exact bootstrap/replay on that database and a
separately approved release gate.

Rollback position: with the setting false, the released baseline path behaves
as before. The additive event and pointer rows can remain unread. Once enabled,
roll back by disabling the setting; do not delete immutable facts.

Retirement criterion: this shadow slice closes only when event replay remains
exact. Event append cannot become capture authority until the unversioned and
generic write surfaces are retired or adapted and a later cutover slice proves
that `data_submission` and outbox can be derived compatibility projections.

### Definition Of Done

- Actual released versioned mutations append one exact immutable capture fact.
- Exact retries append nothing.
- Every live capture chain has one atomically advanced current pointer.
- Current pointers rebuild from immutable facts.
- Released behavior remains unchanged with shadowing disabled.
- Focused, full, and one final production-clone gate pass.
- Production remains untouched.
