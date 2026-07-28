# Current Work

Updated: 2026-07-28

Status: ACCEPTED FOR IMPLEMENTATION

## Capture Current Projection And Replay

### Outcome

Add the smallest replayable current-state projection for capture facts and
prove that it can be reconstructed from the immutable bootstrap facts while
remaining exactly equivalent to `data_submission`.

This slice completes the missing projection part of the accepted capture
foundation. It does not append live capture facts and does not change upload,
authorization, outbox, ETL, HTTP, or mobile behavior.

### Evidence

The capture bootstrap currently creates one immutable `capture_identity_link`
and one immutable `baseline_submission_captured/v1` journal fact for every
`data_submission` row. The production-clone gate reproduced all 52,535 rows
exactly and a second run wrote nothing.

There is no current capture projection. Consequently, a later accepted state
change has no authoritative pointer to its predecessor, and the current state
cannot yet be rebuilt as a projection from the journal. The accepted
transition requires this pointer before live event shadowing.

Exact same-UID retries are already successful persistence/outbox no-ops at
`9e5185ce`, so the projection does not need a retry counter, version, or other
duplicate-suppression state.

### Persisted Shape

Add exactly one table:

```text
capture_current_projection
  capture_id       UUID primary key -> capture_identity_link.capture_id
  source_event_id  UUID unique, not null -> event_journal.event_id
```

The table is a mutable, rebuildable projection. It stores no copied submission
JSON, status, timestamps, generation, actor, authority, or baseline IDs.
Those values remain in immutable journal facts and existing compatibility
projections.

Add a dedicated Liquibase changeset after the existing capture foundation.
Its rollback drops only this table. Do not edit an already-recorded changeset
or add a database trigger that makes the projection immutable.

### Ownership

Introduce one capture-current-projection port and one JDBC implementation with
only these operations:

- strictly insert the bootstrap pointer;
- read the pointer by capture ID.

Do not add a generic pointer-advance operation. The two foreign keys cannot
prove that an arbitrary journal event belongs to a capture. Pointer advance is
owned by the later live-event command, which must define that association and
append the fact plus advance its pointer atomically.

The JDBC owner participates in its caller's transaction and must not open a
`REQUIRES_NEW` transaction. In the existing 250-row bootstrap batch, identity,
event, and pointer work therefore commits or rolls back together.

`capture_identity_link` remains the immutable compatibility identity map.
`event_journal` remains the immutable fact owner. `data_submission` remains
the active production authority in this slice.

### Bootstrap And Replay Behavior

Extend the existing capture bootstrap rather than add a second command:

1. After proving the exact identity and bootstrap event for a source row,
   read the pointer by capture ID.
2. An existing exact pointer is counted as existing and performs no write.
3. An existing different pointer fails immediately with the capture ID and
   expected/actual event IDs; it is never silently overwritten.
4. An absent pointer uses a strict insert without `ON CONFLICT DO NOTHING`.
   Primary-key, unique-event, and foreign-key violations become one sanitized
   `CaptureShadowBootstrapConflictException`.
5. If projection rows are removed while immutable identities and bootstrap
   facts remain, rerunning the bootstrap reconstructs only the missing
   pointers. It creates no replacement identities, events, or checkpoint.
6. The final comparison includes missing, differing, and extra projection
   rows and verifies that every pointer selects the exact canonical submission
   fact expected from the source row.

Add `ItemCount currentPointers` to batch progress and the operator report. Add
`missingCurrentPointerCount`, `currentPointerDifferenceCount`, and
`extraCurrentPointerCount` to comparison/report results. Include pointer
creation in `createdCount()` and all three differences in `differenceCount()`.
Operator output names are:

```text
current_pointers_created
current_pointers_existing
missing_current_pointers
current_pointer_differences
extra_current_pointers
```

A differing pointer is an expected capture ID pointing to an event other than
its deterministic bootstrap event. Malformed or unequal journal content stays
an event difference. Pointer plus event equality proves the reached journal
fact. Compute extras with an anti-join against exact source-backed identities,
not `table count - source count`, so one missing and one unrelated extra row
cannot cancel each other.

The existing immutable v1 checkpoint continues to certify only identity and
bootstrap-event parity for its source fingerprint. Projection differences
make the command exit non-zero but do not rewrite or supersede an existing
checkpoint. No new checkpoint shape is added in this slice, and an existing
checkpoint must not be interpreted as projection readiness.

Preserve the existing `baseline_submission_captured/v1` event bytes and
deterministic IDs. Do not introduce the future live-event shape in this slice.

### Tests

Focused PostgreSQL tests must prove:

- the table has exactly the two specified columns, primary key, unique event
  pointer, and both foreign keys;
- one capture cannot have two current rows and one event cannot be current for
  two captures;
- strict insert/read work and a conflicting pointer cannot be overwritten;
- rollback of an outer transaction removes a newly inserted pointer;
- first bootstrap creates one pointer per source row;
- the completed rerun writes no identity, event, pointer, or checkpoint;
- deleting only projection rows and rerunning reconstructs them from existing
  immutable facts without new facts;
- a wrong pointer fails fast and rolls back its complete 250-row batch; final
  bounded diagnostics remain for missing/extra set mismatches;
- one missing and one unrelated extra pointer are both reported even when
  total projection cardinality equals source cardinality;
- interrupted batch processing resumes without duplicate pointers;
- journal payloads reached through current pointers remain exact for null and
  object form JSON, soft-deleted rows, pinned versions, context, and audit
  timestamps;
- a pre-existing exact v1 checkpoint remains byte-identical when projection
  validation fails and no replacement checkpoint is written.

Run focused tests, then `scripts/release/verify.sh`.

Against the isolated production clone:

1. restore
   `/mnt/windows-csystem-disk/datarun-production-clones/nmcpdb-production-20260725.dump`;
2. build the reviewed candidate and explicitly apply all migrations, including
   the new projection changeset, because the isolated bootstrap command runs
   with Liquibase disabled;
3. run assignment bootstrap and require its existing exact tuple/checkpoint
   result, then run capture bootstrap;
4. require 52,535 capture identities, bootstrap facts, and current pointers;
5. record the clean run's 64-character source SHA-256 as the pinned acceptance
   value, and require zero source/projection differences;
6. rerun and require every created counter to be zero and the same source
   fingerprint;
7. record journal row count and a deterministic journal-content checksum;
8. remove only the disposable clone's current pointers, rerun, and require
   `current_pointers_created=52535`, all identity/event/checkpoint created
   counters zero, and unchanged journal count/checksum;
9. rerun once more and require every created counter to be zero and the same
   pinned source fingerprint;
10. restore the clone from the untouched dump afterward.

Do not connect to or deploy production.

### Definition Of Done

- Every bootstrapped capture has exactly one current source-event pointer.
- The projection can be reconstructed from immutable capture facts.
- Comparison proves the pointed facts equal the current baseline source.
- No submission or upload behavior changes.
- Focused and full release gates pass.
- Production-clone replay and idempotency pass, the clone is restored, and
  production remains untouched.
