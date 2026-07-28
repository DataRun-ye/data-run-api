# Current Work

Updated: 2026-07-28

Status: ACCEPTED FOR IMPLEMENTATION

## Same-UID Submission Retry Idempotency

### Outcome

Make an accepted same-UID submission retry that has no effective persisted
change a successful no-op. Preserve the released response contract while
avoiding a redundant database update, audit/version change, and outbox row.

This is a correction inside the current `data_submission` authority. It is a
prerequisite for live capture-event shadowing, not an event implementation.

### Evidence

`DefaultDataSubmissionService.upsertAll` currently sends every existing,
non-delete submission through `updateAllAndFlush` and emits an `UPDATE` outbox
row. An already-deleted retry is also written again and emits another `DELETE`.
The production-clone capture gate reproduced this directly: the first upload
of one UID emitted `SAVE`; an unchanged retry returned `updated` and emitted
`UPDATE`.

The released mobile treats a UID listed in either `created` or `updated` as a
successful upload. Therefore an unchanged retry can remain in `updated`
without changing the HTTP payload or mobile synchronization behavior.

### Required Behavior

For an existing UID, compare only the state the current persistence owner can
actually change:

```text
formData
activity
assignment
team
orgUnit
status
orgUnitCode
orgUnitName
teamCode
deleted transition
```

Use null-safe value equality and deep JSON value equality. Do not compare or
start mutating immutable or currently ignored fields such as physical ID,
serial number, form/template identity, pinned version, entry timestamps, or
audit fields.

Classify each existing submission as exactly one of:

- unchanged normal retry: return the existing entity and add its UID to
  `summary.updated`, but perform no repository update and no outbox write;
- changed normal update: preserve the current field mutation, repository
  update, `UPDATE` outbox write, and `summary.updated` result;
- first delete transition: preserve server-owned deletion time, repository
  update, `DELETE` outbox write, and `summary.updated` result;
- already-deleted retry with `deleted=true`: return the existing entity and
  add its UID to `summary.updated`, but perform no repository update and no
  outbox write. Preserve current delete precedence: ignore other mutable values
  in this request;
- already-deleted row with incoming `deleted=false` or omitted: never clear
  `deleted` or `deletedAt`; classify matching mutable state as unchanged, or
  preserve the current normal `UPDATE` behavior when mutable state differs.

For a first delete transition, preserve the current precedence exactly: set
only `deleted=true` and a server-owned deletion timestamp. Do not copy mutable
payload/context changes or a client-provided `deletedAt` from the same request.

New submissions retain the current `SAVE` behavior. Do not redesign
undelete/edit policy, immutable-field conflict policy, duplicate UIDs inside
one request, unversioned compatibility routes, or synced mobile editing in
this slice.

### Ownership And Compatibility

- `DefaultDataSubmissionService` remains the single persistence and outbox
  classification owner after authorization and canonicalization.
- `SubmissionUploadService` and all released request/response DTOs remain
  unchanged.
- Both versioned and compatibility callers receive the existing
  `EntitySaveSummaryVM` shape.
- `data_submission` remains authoritative; capture bootstrap rows remain
  inert comparison material.
- No schema, endpoint, mobile, ETL, or production change is permitted.

### Tests

Focused service tests must prove:

- an unchanged normal retry returns the existing entity in `updated` without
  repository update or outbox write;
- a changed mutable value produces one `UPDATE` and one outbox row;
- an already-deleted retry returns success without another write or outbox
  row;
- an already-deleted row with `deleted=false` or omitted remains deleted and
  is either an unchanged no-op or a normal update according to mutable state;
- a first delete transition still produces exactly one `DELETE` outbox row;
- first-delete precedence ignores accompanying mutable changes and a supplied
  deletion timestamp;
- a mixed batch of new, changed, unchanged, first-delete, and deleted-retry
  inputs classifies every UID once and writes only the required batches;
- each listed mutable field independently triggers an update when changed;
- `null` to JSON, JSON to `null`, and `null` to `null` form-data transitions
  are safe, and structurally equal JSON trees are unchanged.

Run focused tests, then `scripts/release/verify.sh`.

Against the isolated production clone:

1. upload one ordinary new submission and record its stored audit/version and
   outbox state;
2. retry the identical canonical payload and require the same successful
   summary with no stored or outbox change;
3. change one mutable value and require exactly one update and outbox row;
4. retry that changed payload and require no further write;
5. perform one delete transition, retry it, and require only one delete write;
6. restore the disposable clone afterward.

Do not connect to or deploy production.

### Definition Of Done

- Exact semantic retries are successful no-ops at the existing persistence
  owner.
- Real updates, creates, and first deletes preserve released behavior.
- HTTP payloads and mobile success classification are unchanged.
- Focused and full release gates pass.
- Clone evidence proves audit/version and outbox stability on retries.
- The clone is restored and production remains untouched.
