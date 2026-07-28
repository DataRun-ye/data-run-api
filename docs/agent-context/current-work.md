# Current Work

Updated: 2026-07-28

Status: ACCEPTED FOR IMPLEMENTATION

## Capture Replay Foundation

### Outcome

Represent every current `data_submission` row as one deterministic immutable
bootstrap capture fact and prove that those facts reproduce the active
released submission projection exactly.

This is a persistence, bootstrap, and comparison slice only. It does not
change the released upload route, submission acceptance, same-UID upsert
behavior, outbox writes, ETL, mobile payloads, or production.

### Production-Clone Evidence

The restored 2026-07-25 clone contains:

- 52,535 submissions with distinct non-null UID, physical ID, and serial
  number;
- 48 soft-deleted rows;
- 38,707 null statuses and 13,828 `IN_PROGRESS` statuses;
- two SQL-null `form_data` values, zero JSON-literal-null values, and no
  non-object form bodies;
- complete assignment, team, organization-unit, and template-version
  references for every row;
- 44,517 rows whose `created_by` resolves to an existing user and 8,018 rows
  whose historical creator is `system`;
- all audit timestamps present;
- 688 referenced organization units without an event identity link.

Inherited/physical residue is not active capture state: all 52,535
`translations` values are empty arrays, `submission_version` is uniformly
`1`, `properties_map` has no active `DataSubmission` owner, and `lock_version`
is optimistic-lock machinery. These columns remain untouched in
`data_submission`; this slice does not copy them into events or remove them.

The current outbox is a delivery queue, not a complete submission history:
4,628 current submissions have no retained outbox row. Bootstrap must use
`data_submission` as the source and must not infer missing capture history
from outbox retention.

### Persisted Shape

Reuse the existing `event_journal` and `org_unit_identity_link`. Add only:

```text
capture_identity_link
  capture_id                 UUID primary key
  baseline_submission_uid    VARCHAR(11) unique
  baseline_submission_id     current ULID, unique
  baseline_serial_number     current BIGINT, unique
```

This immutable link preserves the released compatibility identities. A
current-event pointer is deliberately not added: this slice has exactly one
deterministic event per submission. A pointer is justified only when a later
accepted slice appends more than one event for the same submission.

Do not add a capture state table, submission history table, event journal,
device identity, workflow state, conflict model, review model, or generic
subject model.

### Bootstrap Capture Fact

Append one event per current submission:

```text
event_type     capture
shape_ref      baseline_submission_captured/v1
activity_ref   canonical activity UID
subject_type   org_unit
subject_id     event identity of the canonical organization unit
actor_id       system:migration/datarun-baseline-capture
recorded_at    stored last_modified_date
payload
  submission   active released submission state
```

This event is a system observation of the current baseline row. It does not
claim that `created_by`, `last_modified_by`, or the migration process authored
the historical user action. The stored audit values remain evidence inside
the submission state.

The submission state contains exactly:

```text
uid
deleted
deletedAt
formData
status
formUid
formVersionUid
formVersionNumber
assignmentUid
teamUid
teamCode
orgUnitUid
orgUnitCode
orgUnitName
activityUid
startEntryTime
finishedEntryTime
createdBy
createdDate
lastModifiedBy
lastModifiedDate
```

Preserve all listed nulls. The verified SQL-null form body is encoded as event
JSON null and replays as SQL `NULL`; bootstrap must reject a JSON-literal-null
or non-object source body instead of collapsing its meaning.

Map `deleted_at`, entry timestamps, and audit timestamps through the active
Hibernate UTC `Instant` policy and preserve database microsecond precision.
Physical submission ID and serial belong only to `capture_identity_link`.

The event ID and `capture_id` are deterministic namespaced UUIDs derived from
the baseline submission UID. Reuse the existing organization-unit identity
algorithm. Create a missing organization-unit link only when the referenced
current organization unit exists; otherwise fail. Adding those links must not
change existing assignment grants or released work reads. Do not create actor
links, users, assignments, grants, or historical changes.

### Outbox Boundary

Identity plus submission state preserves only the replayable inputs used to
construct a new outbox row:

```text
submission physical ID
submission UID
submission serial number
template-version topic
serialized form payload
```

The historical queue event type (`SAVE`, `UPDATE`, or `DELETE`), enqueue
timestamp, delivery status, attempts, claims, errors, and ingest identity
cannot be reconstructed from current submission state. They are not capture
facts and must not be added to the event.

### Bootstrap Execution

Implement an explicit non-web command with the same opt-in and isolated-clone
discipline as assignment bootstrap. No external writer may use the database
while it runs.

At command start, capture a stable source boundary:

```text
row count
maximum serial number
SHA-256 of canonical source rows ordered by serial number
```

The fingerprint covers compatibility ID/UID/serial plus every listed active
state field. Serialize fields in the declared order, timestamps as UTC ISO-8601
with stored precision, and each UTF-8 row as a length-prefixed value before
updating the digest.

Then:

1. read only rows inside that boundary by serial-number keyset in bounded
   batches;
2. atomically insert or verify each batch's exact identity links, organization
   unit links, and journal events;
3. commit completed batches so interruption is safely resumable;
4. reject any deterministic identity or event whose stored content differs;
5. in one final repeatable-read transaction, recompute the source boundary,
   require it unchanged, run complete set comparison, and append the completion
   checkpoint;
6. report source rows, created/existing rows, source fingerprint, missing
   identities, difference counts, and bounded diagnostic samples;
7. exit non-zero on conflict, source movement, or mismatch.

Do not load all form JSON into memory and do not use page offsets. The
PostgreSQL advisory lock prevents two bootstrap commands from running
together; the explicit isolated/no-writer gate protects against ordinary
submission writes that do not acquire that lock.

### Completion Checkpoint

Append exactly one checkpoint after final comparison:

```text
event_id       namespaced UUID for
               datarun-baseline/capture-shadow/bootstrap-completed/v1
event_type     transition_checkpoint
shape_ref      capture_shadow_bootstrap_completed/v1
activity_ref   null
subject_type   transition
subject_id     namespaced UUID for datarun-baseline/capture-shadow/v1
actor_id       system:migration/datarun-baseline-capture
recorded_at    maximum source last_modified_date, or Unix epoch when empty
payload
  sourceCount
  sourceMaxSerial
  sourceSha256
```

A completed rerun must still execute full set comparison, verify the exact
checkpoint and source fingerprint, perform no writes, and report `created=0`.
Fingerprinting, comparison, and diagnostic collection must remain bounded in
memory.

### Replay Comparison

For every source row, reconstruct the active submission projection from
`capture_identity_link` plus its deterministic journal event. Require:

- exact set equality between source UIDs, identity links, and all
  `capture`/`baseline_submission_captured/v1` events;
- exactly one identity and one bootstrap event per submission;
- no orphan, duplicate, or extra bootstrap capture event;
- exact compatibility ID, UID, and serial-number equality;
- exact equality for every listed active state field;
- exact accepted envelope, subject, migration actor, and checkpoint;
- availability of the replayable outbox inputs listed above.

The comparison target is the listed active submission projection, not every
physical column in `data_submission`. It does not compare outbox history,
queue metadata, `translations`, `submission_version`, `properties_map`, or
`lock_version`.

### Ownership Boundaries

- `data_submission` remains the released write and read authority.
- `DefaultDataSubmissionService`, `SubmissionUploadService`, and current
  outbox ownership are unchanged.
- Capture-owned links and bootstrap events are comparison-only.
- `org_unit_identity_link` remains shared active identity infrastructure;
  tests must prove new aliases do not alter assignment authority or V1 reads.
- Assignment grants are existing context only; bootstrap does not authorize,
  create, or reinterpret them.
- Historical facts preserve current accepted state only. They do not claim to
  reconstruct prior edits or deletion history.
- No live shadow append is added here. That is a separately accepted slice
  after bootstrap/replay equivalence closes.

### Tests

Focused tests must prove:

- schema constraints, foreign keys, and capture-link immutability;
- deterministic IDs and exact conflict detection;
- SQL-null form data, null status, soft deletion, pinned template, and audit
  values round-trip exactly;
- JSON-literal-null and non-object form bodies fail explicitly;
- all timestamp fields use UTC and preserve microsecond precision;
- missing organization-unit aliases are deterministic and do not change
  assignment authority or released reads;
- an unresolved organization unit fails without fabricated identity;
- interrupted multi-batch bootstrap resumes without duplicate events;
- source movement prevents checkpoint creation;
- comparison and checkpoint share one repeatable-read final transaction;
- a completed rerun writes nothing;
- changing any active state, alias, event, envelope, or checkpoint value is
  reported as a mismatch;
- extra bootstrap capture events are rejected;
- submission upload, outbox, ETL, and released HTTP behavior remain untouched.

Run focused tests, then `scripts/release/verify.sh`.

Against the isolated production clone:

1. restore the untouched dump and apply current migrations;
2. restore the exact 263,423 assignment-authority checkpoint;
3. run capture bootstrap and require 52,535 exact events;
4. run it again and require zero created rows;
5. inspect bounded-batch memory behavior and elapsed time;
6. rerun assignment tuple comparison and released-read checks unchanged;
7. restore the disposable clone afterward.

Do not connect to or deploy production.

### Slice Gate

- **Authority before/after:** `data_submission`.
- **Compatibility owner:** existing submission service, outbox, and released
  DTOs, unchanged.
- **Schema:** one additive immutable capture identity link beside the existing
  journal.
- **Activation:** explicit isolated bootstrap command only.
- **Rollback:** code rollback leaves additive, unread capture rows inert; no
  baseline data rollback is required.
- **Retirement:** none in this slice. Live shadow append is not permitted until
  exact replay equivalence is closed.

### Definition Of Done

- The model contains only the accepted capture link and bootstrap fact.
- Bootstrap is bounded, deterministic, resumable, and conflict-detecting.
- Replay reproduces all 52,535 active clone projections exactly.
- The second clone run creates nothing.
- Shared organization-unit identity and assignment reads remain unchanged.
- Focused and full release gates pass.
- Released upload/outbox/ETL behavior and production remain unchanged.
