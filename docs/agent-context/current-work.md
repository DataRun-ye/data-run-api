# Current Work

Updated: 2026-07-28

Status: ACCEPTED FOR IMPLEMENTATION

## Versioned Upload Event Authorization Cutover

### Outcome

Make the event-backed assignment grant the authorization authority for the
released versioned submission upload:

```text
POST /api/v1/dataSubmission/bulk?referenceVersion=1
```

Keep the request DTO, response summary, administrator bypass, validation
order, error codes, Reference resolution, repeat metadata, whole-JSON
persistence, and outbox transaction unchanged.

This slice cuts over only versioned upload. Assignment list,
assignment-form, organization-unit, and Reference catalog reads continue to
use their released owners with event comparison. No mobile, schema,
bootstrap, configuration, or production deployment change is included.

### Evidence Gate Already Closed

The preceding shadow stage is complete:

- focused tests and the full release gate passed;
- 174 unit/contract and 37 integration tests passed;
- active, membership-revoked, permission-revoked, disabled,
  assignment-retired, late-upload, and restored clone-only states were
  exercised through real HTTP routes;
- all five active surfaces produced no unexplained shadow result;
- the lifecycle fixture ended with 263,425 baseline and event tuples and zero
  differences;
- the disposable clone was restored and re-bootstrapped to its original
  263,423 tuples with zero differences;
- production was not connected or changed.

### Selected Authority

Introduce one request-scoped event authorization owner for field-user capture.
It reads the current actor's assignment grant generations through the existing
`AssignmentCaptureEventReadPort` and decides, for one baseline assignment UID
and form UID:

```text
ACTIVE_GRANT
ENDED_RETIRED_ASSIGNMENT
REVOKED_HISTORY
NO_GRANT
AUTHORITY_UNAVAILABLE
```

The event decision owns acceptance after this slice:

- an active matching grant accepts an upload against an active assignment;
- a matching ended generation may accept only the existing retired-assignment
  compatibility case described below;
- revoked history or no grant denies;
- missing exact bootstrap checkpoint or failed event reads fail closed as
  service unavailable; they never fall back to baseline acceptance;
- administrators retain the current explicit bypass without an invented
  assignment grant.

The authority is request-current. Do not put grants in JWT claims,
cross-request caches, or persistent decision tables.

### Bulk And Validation Order

Preserve the released per-request order:

```text
map request
resolve assignment
resolve pinned template version
canonicalize submission context
authorize
generate missing repeat metadata
resolve Reference values/definitions
persist batch and outbox
```

Create one request-scoped authorization session for the bulk request. On the
first authorization decision, batch-read event history for the distinct,
non-null raw assignment UIDs in the request. Reuse that immutable snapshot for
the remaining requests.

This must not prepare, validate, or resolve later submissions before an earlier
submission reaches its authorization point. The first failing request and its
existing error therefore remain authoritative. Do not introduce one event
query per submission.

### Retired Assignment Compatibility

The released server accepts an already-created offline upload referencing a
soft-deleted assignment when current direct team membership and the same-team
capture permission still hold. Preserve only that behavior:

- the submitted assignment row is `deleted=true`;
- event history contains the matching ended actor/assignment/form generation;
- current direct membership still holds;
- current `ADD_SUBMISSIONS` or `EDIT_SUBMISSIONS` permission still holds for
  the submitted form;
- team and activity status and assignment scope still match the ended
  generation.

Use the existing baseline adapter only to narrow and map this explicit
compatibility case. It must not reactivate the assignment, expose it for new
work, or turn membership, permission, team/activity, role/form, or scope
revocation into late-upload permission.

### Released Error Compatibility

When the event authority denies, preserve the existing field-user errors:

- no current direct team membership -> `E4114`;
- no current same-team capture permission -> `E1112`.

The baseline adapter may classify the denial solely to preserve these wire
errors. It must never change an event denial into acceptance.

If the baseline classifier says `ALLOWED` while event authority has no active
or eligible retired generation, treat that as an event/projection invariant
failure and fail closed. Emit one bounded diagnostic without user, form-data,
token, or request-body content. Do not silently use baseline acceptance.

An active event grant is accepted without a baseline authorization veto. That
is the ownership cutover. Existing assignment rows remain required as the
compatibility projection supplying canonical assignment/team/activity/org-unit
context.

### Ownership Cleanup

- Move upload decision semantics out of
  `AssignmentCaptureShadowComparator` into the selected authorization owner.
- Remove the upload-specific shadow integration once the event decision is
  authoritative; retain shadow comparison only for the four read surfaces.
- Keep baseline upload logic behind one named compatibility adapter for
  retired-assignment narrowing and released denial-code mapping.
- Do not duplicate grant matching in `SubmissionUploadService`.
- Do not add another authorization check to the unversioned generic write
  routes; they remain a separately classified legacy-risk surface.
- Transitional names may remain only where the four read shadows still use
  them. Remove upload-only dead methods, records, and tests in this slice.

### Tests

Focused tests must prove:

- one event read per bulk request, including many submissions sharing or
  mixing assignments;
- administrator upload performs no event read and preserves success behavior;
- active matching grant accepts with the unchanged summary;
- active event grant acceptance does not depend on baseline authorization
  acceptance;
- no grant and revoked history deny without baseline fallback;
- denial preserves `E4114` versus `E1112`;
- baseline `ALLOWED` plus missing/contrary event state fails closed;
- missing checkpoint and event-read failure fail closed and persist nothing;
- a matching ended generation accepts only the retired-assignment compatibility
  case;
- membership, permission, team/activity status, form-role, and scope changes
  deny even when matching history exists;
- an active grant cannot authorize a soft-deleted assignment;
- an earlier authorization failure still wins over malformed later requests;
- repeat metadata, Reference creation, whole-JSON persistence, outbox writes,
  retries, and transaction rollback retain their current behavior.

Run focused tests, then `scripts/release/verify.sh`.

Against the isolated production clone:

1. require exact baseline/event tuple comparison before starting;
2. use clone-only actor, assignment, and submission fixtures;
3. exercise active acceptance, membership and permission denial, team disable,
   assignment retirement with accepted late upload, and restoration;
4. prove ordinary and Reference versioned uploads preserve status, summary,
   persisted canonical context, repeat metadata, and outbox behavior;
5. require no unexplained state and no per-submission event query growth;
6. restore the disposable clone afterward.

Do not connect to or deploy production.

### Slice Gate

- **Authority before:** `BaselineAssignmentCaptureAdapter` decides versioned
  field-user upload; event state is comparison-only.
- **Authority after:** the event authorization owner decides versioned
  field-user upload; baseline logic only narrows retired-assignment
  compatibility and maps released denial codes.
- **Persistence:** unchanged `data_submission` and outbox transaction.
- **Wire/mobile:** unchanged.
- **Schema/bootstrap:** unchanged; exact completed checkpoint is mandatory.
- **Rollback:** revert this cutover to restore baseline upload authority; no
  data rollback exists.
- **Retirement:** the baseline upload compatibility adapter retires only after
  action-specific error compatibility and retired-assignment policy receive a
  separately accepted replacement. It is not a second acceptance authority.

### Definition Of Done

- Versioned upload has one event-backed acceptance owner.
- Baseline acceptance cannot override event denial.
- Existing administrator, error, validation-order, Reference, repeat,
  persistence, retry, and outbox behavior remains characterized and passing.
- Bulk event reads are bounded once per request.
- Upload-only shadow/dead paths are removed.
- Focused, full release, and isolated-clone gates pass.
- Production remains untouched.
