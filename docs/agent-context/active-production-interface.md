# Active Production Interface

Role: released mobile-to-server HTTP contract and active server ownership map

Status: LIVING

This map follows the strict comment-out test: `CORE-ACTIVE` means removing the
path without replacement breaks a released mobile capability. Reachable
administration, maintenance, compatibility, and gated code is classified
separately rather than being treated as equally active.

## Authentication And HTTP Gate

| Status | Route | Contract |
| --- | --- | --- |
| CORE-ACTIVE | `POST /api/v1/authenticate` | released mobile login; returns access and refresh tokens |
| CORE-ACTIVE | `POST /api/v1/refresh` | released mobile refresh-token rotation; invalid, expired, unavailable, or inactive users receive `401` |
| CORE-ACTIVE | `GET /api/v1/myDetails` | authenticated released-mobile profile |
| PUBLIC STATUS | `GET /api/v1/authenticate`, `GET /api/authenticate`, health and info management routes | no user data; retained compatibility/status surface |
| ADMIN | `/api/v1/admin/**`, `/api/admin/**`, `/api/custom/admin/**`, and both account-creation aliases | current database `ROLE_ADMIN` is required |
| DEPRECATED COMPATIBILITY | `/api/custom/**` | authenticated or administrator-gated alias; not used by the released mobile and not a target API |
| NO ROUTES YET | `/api/v2/**` | its own chain: bearer JWT only (no HTTP Basic), every route authenticated, RFC 9457 problems; conventions in `api-v2.md` |

JWT claims identify the subject, but request authority comes from the current
activated database user and current database authorities. A stale token claim
cannot preserve administrator access after the database role changes.
Deactivated users cannot receive or rotate tokens. All routes outside the
explicit allowlist fail closed.

A failed login is logged at WARN without the login it tried, a personal
identifier and often a phone number (`AuthenticationFailureListener`, since
6.5.5): `Login failed (BadCredentialsException): request <request id>, login
tag <8 hex digits>, account <user id or unknown>`. The tag is an HMAC under a
key drawn at each start and never stored: a login repeats its tag until the
next restart, so repeated failures stand out, and the tag cannot be turned back
into the login. The account's durable id is named when the account exists; it
is read from the user cache that the login's own lookup has just filled, never
by a query. Only a username-and-password failure is logged: Spring Security
reports a rejected bearer token as bad credentials too, with the token as the
principal, and the listener ignores it. An expired, badly signed or malformed
token is counted by the invalid-token meter
(`security.authentication.invalid-tokens`, by cause) and logged nowhere; one
the decoder rejects for another reason (an unexpected algorithm, say) logs
`Unknown JWT error <the decoder's message>` at WARN (`SecurityJwtConfiguration`).
A failure inside the security chain (HTTP Basic) runs before `RequestIdFilter`,
so its line names no request id.

HTTP Basic remains baseline operational compatibility pending evidence from
the administrator and monitoring smoke. It is not canonicalized by this
transition. Under `/management/`, only `health` (with its probes) and `info`
are public; everything else needs ADMIN, `/management/prometheus` included
since 6.5.5 (it was public, and it names every route pattern, repository
timings, exception types and error rates).

## Assignment

| Status | Released mobile request | Server owner | Required downstream path |
| --- | --- | --- | --- |
| CORE-ACTIVE | `GET /api/v1/assignments?paged=false` | `AssignmentResource` inherited read route | `DefaultAssignmentService.findAllByUser` -> `AssignmentFilter` -> `AssignmentRepository` |
| CORE-ACTIVE | `GET /api/v1/assignments/forms?paged=false&referenceVersion=1` | `AssignmentResource.getAllDto` | `DefaultAssignmentService.getAllUserAccessibleDto`: access-filtered assignments -> `AssignmentWithAccessMapper` (forms from `AssignmentFormAccessService`), then `ReferenceAssignmentFormGate.filterUnsupportedForms` on the result |
| GATED | `GET /api/v1/assignments/{uid}/referenceEntries` | `ReferenceEntryResource` | `AssignmentService.findAccessibleByIdOrUid` and assignment/form access checks; deployed but unused until a Reference form is assigned |
| SUPPORTING-REACHABLE | `GET /api/v1/formPermissions` | `UserFormPermissionsResource` | the mobile synchronizes this into `user_form_permissions`, but no active mobile behavior reads that table; retire mobile registration/table first, then this endpoint |

The released mobile owners are `AssignmentDatasource` and
`ReferenceEntryDatasource`. Assignment synchronization persists assignments
and replaces the local assignment-form projection only after its secondary
request succeeds.

The assignment and org-unit syncs and the supervisor scope read assignments by
team, through `idx_assignment_team_id` (changeset `20261125-02`, 6.5.5).
Before it no index started with `team_id`, so each such read walked the whole
table or the whole `uc_assignment_activity_id` index (on staging, the busiest
supervisor's scope went from 3.3 s to 28 ms).

## Authorization Ownership

- `User.authorities` supplies Spring authentication authorities and the
  administrator flag.
- `CurrentUserDetailsService` rebuilds one request-scoped authorization
  snapshot from enabled direct teams, their enabled managed teams, activities,
  and team-scoped form grants. A team's grants also apply to the enabled teams
  it manages (also when the user belongs to both), so a supervisor's team
  grants reach his teams' assignments. It is
  intentionally not cached across requests; access changes must not depend on
  four independently invalidated caches.
- `CurrentUserProfileV1` adapts that principal to the released
  `/api/v1/myDetails` response. Legacy count and `userGroupsUIDs` fields are
  V1 wire compatibility only and retire when older supported mobile clients no
  longer require that profile shape.
- `ResourceApiAuthorization` preserves the coarse inherited-resource gate:
  administrators or users with a team may read; only administrators may use
  generic writes. It does not decide entity visibility or form permissions and
  retires as the inherited routes receive domain owners or are removed.
- `UserAccessService` and registered access filters constrain entity reads.
  Its creator-only fallback is a compatibility policy for unclassified generic
  resources. Static service/filter comparison currently limits that fallback
  to data-element group/set and org-unit group/set generic surfaces; each must
  exit through an explicit domain filter or route removal.
- `AssignmentFormAccessService` is the shared authorization owner for the
  actor, assignment team, assigned form, and requested form action.
- Assignment and organization-unit sync cover direct-team assignments and,
  for app 6.1.2 and later (`X-App-Version`), the managed teams' assignments
  that carry a form the user may submit there (a grant with add or edit;
  `ManagedAssignmentScope`: one query per group of managed teams with the same
  forms, returning ids only), with org-unit ancestors. Older
  apps receive none: one such assignment stops them listing the activity.
  Other managed-team assignments are not synchronized. Managed teams also
  remain mobile selector/summary data.
- Legacy role, privilege, and Spring ACL tables have no source policy owner and
  remain schema-only until a bounded Liquibase contraction.
- User groups do not participate in the active authentication or work-scope
  decision. Their CRUD and schema surface remains a separate removal decision.

## Form Templates

| Status | Released mobile request | Server owner | Required downstream path |
| --- | --- | --- | --- |
| CORE-ACTIVE | `GET /api/v1/formTemplates?paged=false` | `FormTemplateResource` inherited read route | `DefaultDataTemplateService.findAllByUser` -> `FormTemplateFilter` -> `DataTemplateRepository` |
| CORE-ACTIVE | `GET /api/v1/formTemplateVersions?paged=false` | `TemplateVersionResource` inherited read route | `DefaultTemplateVersionService.findAllByUser` -> access-filtered template masters -> `TemplateVersionRepository` |

The released mobile owner is `DataFormTemplateDatasource`. The form-template
cleanup pass must trace the filters, services, repositories, template
processing, and pinned-version resolution behind these two reads before
removing similar-looking form APIs.

`/api/v1/dataFormTemplates` is the separate operational authoring boundary.
`FormTemplateAuthoringResource` validates and processes the complete template,
then `DataTemplateInstanceService.publishVersion` creates one immutable version,
updates the template's latest-version pointer, and invokes downstream
canonical projection generation exactly once. The stored template
version remains the product/form-runtime contract; generated element metadata
supports extraction and is not a second template authority.

`canonical_element` is the projection metadata consumed by the active ETL and
pivot/export path, including nested-repeat ancestry and option-set identity.
The duplicate `template_element` writer, JPA entity ownership, and cache were
removed after confirming there was no source, mobile, SQL, or declared external
reader. Its physical table remains schema-only until a separately tested
Liquibase contraction.

`DataElement` remains operational authoring input for stable field identity,
name/code, and value type. Option-set UID is a template-field property and is
resolved into canonical projection metadata downstream; template processing
does not copy it from `DataElement`. Data-element groups and group sets have no
product consumer and their source routes, services, repositories, and JPA
relations are removed. Their physical tables are schema-only pending a separate
Liquibase contraction.

## Submission

| Status | Route | Server owner |
| --- | --- | --- |
| CORE-ACTIVE | `POST /api/v1/dataSubmission/bulk?referenceVersion=1` | `DataSubmissionResource.saveVersionedUpload` -> `SubmissionCommandService.upload` -> `SubmissionUploads` -> `DefaultDataSubmissionService.upsert` |
| CORE-ACTIVE (app 6.1.0+) | `POST /api/v1/dataSubmission/bulk/practice?referenceVersion=1` | `DataSubmissionResource.savePracticeUpload` -> `SubmissionCommandService.uploadPractice` (practice: always held) |
| ADMIN | `GET`/`PUT /api/v1/admin/goLiveRules`, `DELETE /{uid}`, `GET /effective` | `GoLiveRuleResource` -> `GoLiveRuleService` -> `GoLiveRuleRepository` (`go_live_rule`) |
| ADMIN | `GET /api/v1/admin/heldSubmissions`, `GET /summary`, `GET /{uid}` | `HeldSubmissionResource` -> `HeldSubmissionRepository` (`held_submission`, reads) |
| ADMIN | `POST /api/v1/admin/heldSubmissions/release`, `POST /discard` | `HeldSubmissionResource` -> `SubmissionCommandService.releaseHeld` / `discardHeld` -> `HeldSubmissionDecisions` |
| LEGACY-RISK / UNKNOWN | `GET /api/v1/dataSubmission`, `GET /byLastModified`, `POST /query` | inherited generic read surface; an admin's list is bounded (6.5.5): at most 1,000 per page, starting within the first 10,000, never unpaged, else 400 `E2060` before any query. `POST /query` answers that refusal, like any failure, with its own empty 400: no body, no error code (`BaseReadResource.unifiedMongoLikeQuerying` catches every exception) |
| LEGACY-RISK / UNKNOWN | `POST /api/v1/dataSubmission/bulk` without the version parameter | `DataSubmissionResource.saveAll` -> `SubmissionCommandService.uploadLegacy` |
| RETIRED (6.5.5) | `POST /api/v1/dataSubmission`, `POST /return` | not routes: 405 to every caller (B2). They never saved (bean validation answered 400, the id cannot be bound), and production's access logs of 2026-11-10 to 24 hold no call |
| LEGACY-RISK / UNKNOWN | `GET|POST /api/v1/dataSubmission/objects` | deprecated flattened read surface, bounded for admins as the reads above; the admin tool's pages 61 to 66 of 1,000 are refused (the tool no longer depends on them, 2026-11-25), G0's unpaged pull as a field user is unchanged |
| ADMIN / UNKNOWN (6.5.5) | `GET /api/{v1,custom}/dataSubmission/{id}` | `DataSubmissionResource.getById` -> `findByIdOrUid` (no row filter: deleted submissions too). Admin only since 6.5.5: any other user gets 403 (`error.http.403`), as on `DELETE`; before, any team member read any submission. Production's access logs of 2026-11-10 to 25 hold no call, and no phone release calls it |
| ADMIN / UNKNOWN | `DELETE /api/v1/dataSubmission/{uid}` | `DataSubmissionResource.deleteByIdUid` -> `SubmissionCommandService.delete` |
| RETIRED (6.5.5) | `PUT /api/v1/dataSubmission/{uid}` | not a route: 405 to every caller (B2, owner decision 4). It never saved (bean validation answered 400), and the same logs hold no call |

Every submission write goes through `SubmissionCommandService`
(`jpa.datasubmission.command`): controllers adapt each route to a
`SubmissionCommand` (upload, practice upload, legacy upload, admin delete,
held release, held discard) naming the acting `SubmissionActor`: the durable
user id, plus the login that today's audit columns store. Each submission is
written in a transaction of its own, with the revision that records each
change and the `outbox` row that announces it (Change Record below), and
`SubmissionWriteOwnershipTest` (ArchUnit) fails the build when any other class
calls the write methods of the persistence layer (`DataSubmissionService`, the
submission repository, `SubmissionIntake.hold`, `HeldSubmissionRepository`,
`OutboxWriter`, `SubmissionRevisionWriter`); it accepts no violation. The rule cannot see writes through
the generic `IdentifiableObjectService` types; `DataSubmissionResource`
inherits none of the generic writes since 6.5.5 (it extends the read-only
`BaseReadResource` and declares its own routes), so no route reaches one. Log
lines on these paths carry identifiers and shapes only: submission and form
uids, the user id, counts, error codes and exception types, never a login, a
submitted value or an exception message.

The submission lock first: each transaction that writes a stored submission
(both upload routes, a held release's replay, the admin delete) takes the
submission lock as its first read, through
`DataSubmissionRepository.findByUidForWrite`: a pessimistic write lock that
Hibernate 6.6 issues on PostgreSQL as `SELECT ... FOR NO KEY UPDATE`, the lock
every `UPDATE` of a submission takes. The stored-record guard and the save use
the submission it locked, and another change of the same submission waits
until the transaction ends instead of committing between its reads, where it
used to fail the later write's version check (the upload item as
`UNEXPECTED_FAILURE`, the delete as a 409). `SubmissionLockIT` shows it with
real transactions: it holds an upload on either route, or an admin delete,
between its reads, and the other change waits on the submission lock until it
ends. The wait is bounded: the transaction's first statement sets
`lock_timeout` for itself (`datarun.submissions.lock-timeout`, 5 s, as v2's
`datarun.v2.commands.lock-timeout`; Hibernate ignores a numeric lock timeout
on PostgreSQL). When another change holds the submission longer, as a stalled
holder would (a dropped database link, an `UPDATE` left open in psql), the
waiting upload item fails with `UNEXPECTED_FAILURE` and the phone sends it
again at its next sync, the admin delete answers 409
`error.concurrencyFailure`, and one WARN line names the submission's uid with
SQLState 55P03; the connection returns to the pool. PostgreSQL bounds each
lock wait on its own, so a writer queued behind another waiter can wait up to
about twice the bound (measured on a container: 5.1 to 5.2 s alone, 10.1 s
queued). A new submission has nothing to lock: a second save of its uid still
fails on the uid's unique constraint (below). `SubmissionWriteOwnershipTest`
keeps the submission lock to the command service and its persistence layer.

Conflict detection (submission review, step S2; `jpa.datasubmission.conflict`)
is behind the activation switch `conflict-detection`, off by default, and off
both upload routes run exactly as described here: the A2 goldens prove it. On,
`ConflictDetection` reads each live upload item of a stored submission right
after the submission lock, before the upload checks and, on the legacy route,
before row ids are minted. It tags a phone copy whose rows lack ids, finds the
uploader's base version in the journal, and compares the copy structurally
(`ConflictComparison`: rows' structure keys left out, numbers by value, with
the deleted flag, assignment and status; not the form version, which an update
never writes). While the uploader has
an open conflict on the submission, their copy only counts there or replaces
its copy, and never writes the submission. Otherwise an unchanged re-send, or a
copy equal to the current state, writes nothing. After a server-side change since
the base version, a changed copy is kept aside in `submission_review.conflict`
(one open conflict per submission and uploader), and so is any changed copy of
a submission that is deleted now, whatever the base; after a move, so is a
changed copy the checks refuse for access (E4124, E4114, E4122). An uploader without a
base of their own is checked first and refused as today. Each such upload item
is answered `updated`, and detection never fails one: an error in it keeps the
copy aside with cause `DETECTION_ERROR`. Practice uploads and held releases are
not detected. The journal accepts submission review's commands since changeset
`20261125-s2-01`, but no release writes them yet, so today's server-side
changes are admin deletes and deleted baselines. A deleted baseline counts for
every uploader, also as a submission's first revision: a copy of a submission
deleted before the journal began is kept aside, as after an admin delete.
`ConflictDetectionRehearsalIT` rehearses detection, counts only, on a clone the
operator names: its changed-copy pass writes and rolls back, so it refuses any
database but the one named, and production's host and database outright.

`data_submission`'s indexes: the primary key, the unique keys on `uid` and
`serial_number`, `idx_ds_form_assignment`, and two that only the generic read
surface would use, when an admin filters `/objects` or `/query` by org unit
(`idx_ds_orgunit`) or by form (`idx_ds_active_form_assignment`). Changeset
`20261125-03` (6.5.5) drops the GIN on `form_data`, which no query can use (no
jsonb operator it serves appears anywhere), and `idx_ds_serial_number`, a
duplicate of the unique key.

Ordinary and Reference-capable submissions share the versioned upload
(`SubmissionUploads`, the released mobile's owner). It maps the versioned
DTO; resolves assignment and pinned template once through
`TemplateVersionResolver`; canonicalizes server-owned assignment/template
context; authorizes the canonical assignment/form pair; generates missing repeat
metadata for compatibility; resolves Reference definitions; upserts whole
submission JSON; and writes the current `outbox` row in the same transaction.
Submission access and assignment-form projection use the same
`AssignmentFormAccessService`; permissions from another team and forms absent
from the assignment are rejected. A manager may submit on a managed team's
assignment with his own team's grant; the submission keeps the assignment's
team. An upload that names an existing submission is also checked against the
stored record (`StoredSubmissionGuard`, both upload routes): a record never
changes form (E4123), the user must be allowed on the stored assignment and
form as well (E4124; a move needs both), and deleting needs a delete
permission there (E4125). A record another user made (its creator is the
login that first sent it) needs `EDIT_SUBMISSIONS_FROM_USERS` to change
(E4126) or `DELETE_SUBMISSIONS_FROM_USERS` to delete; no active team holds
either (2026-11-20). Administrators are not checked.
`DataSubmissionService` receives canonical submissions and a result summary,
not a security principal; authorization must complete before persistence.

The legacy route (apps before 6.0.2) runs the same pipeline steps in its own
order: the form version first, by uid only; repeat ids; for a non-admin, the
assignment (missing or unknown are both "Assignment is required"), E4114
naming the team the upload claims, E4122 and the stored-record guard; then
the context. It resolves no Reference values and is never held. The A2
goldens pin each difference (W3-*); they end when the route retires (B2).

After authorization, `SubmissionIntake` decides per submission whether it is
saved or held (DataRun-ye/data-run-api#42): practice uploads are always held,
and a new submission started before the go-live rule that applies to it
(`GoLivePolicy`: the nearest org unit first, a form rule before that level's
rule for every form; the earlier of phone start and server receipt) is held.
A held submission runs the same checks, Reference resolution and upsert, then
its transaction is rolled back and the upload is stored as received in
`held_submission`, so nothing reaches `data_submission`, the outbox or the
Reference lists. An admin release replays it through this same path on
behalf of its original submitter (the checks read that user's access, the
audit columns name them, and `decided_by` names the admin), saving it in the
same transaction as the decision that releases it: if a re-send changed the
held copy after the release read it, nothing is saved and the upload is
reported as changed. A held copy of a uid already in the real data is marked
superseded. A release by filter takes only uploads still held before a
go-live; discarded and practice uploads are released only by uid. Releases
and discards work in batches (at most 500 per call, reporting what remains),
and every decision applies only if the upload is unchanged since it was read;
released and superseded uploads are final. An upload reported `changed` was
left as it was, with nothing saved.

Releasing a campaign's held uploads:
- A release is finished when `GET /api/v1/admin/heldSubmissions/summary` for
  the campaign shows no `HELD` `BEFORE_GO_LIVE` rows.
- Repeat the same call while it answers `runAgain` true. Each such call made
  progress (it decided an upload, or tried one for the first time), so the
  repetition ends. A by-uid call takes only uploads still undecided, and
  uploads already tried and failed wait behind untried ones.
- If a call makes no progress, it answers `stopReason` (for example "No
  progress: 2 failed again"): stop and investigate before calling again.
  The exception: if the only thing left is `changed` (phones re-sent those
  uploads during the call), call once more. The next call releases their
  newer copies.
- Re-run once before treating a failed upload as needing a fix: a live upload
  of the same uid that was not yet committed can make the replay fail on the
  unique uid, and the next release marks it superseded. An upload that keeps
  failing stays held, with `last_release_error` (an error code or exception
  type, and the uploader's user id), until its cause is fixed or it is
  discarded.
- That unique-uid failure is expected, as is a phone's re-send that loses the
  same race to its own first request (the phone is told it failed and sends
  again). Since 6.5.5 it is logged at WARN, `not saved: another save of the
  same uid came first`, with the ids and SQLState 23505, and nothing is
  reported to GlitchTip: Hibernate's ERROR line for the same insert stays in
  the container log only. Any other failure is still logged at ERROR.

The area chain is read by walking
parents, never through `OrgUnit.getPath()`, which writes the org unit when
called in a committing transaction. The unversioned legacy writes are never
held. The connection pool does not auto-commit, so JDBC writes outside a
transaction are lost: intake writes are `@Transactional`. Never run the
Liquibase rollback of `held_submission` once uploads were held: it would
delete uploads that phones report as sent.

The admin delete marks the submission deleted, flushes it, and then writes its
`DELETE` revision and event at the new version, in one transaction. Deleting a
deleted submission writes nothing, as an upload's `deleted=true` does, and an
unknown uid answers 404.

The unversioned writes are not used by the released mobile and remain
`LEGACY-RISK` pending the recorded endpoint-use decision; their reachability
does not make them a second product authority.

The disabled submission-history listener and its zero-caller processor/model
alternatives are source-dead. Their physical table remains a schema concern.
The legacy `outbox_event` table has no source left: no class reads or writes
it, and only its changelog names it. The current `outbox` is inserted only by
`OutboxWriter`; claims update its status (`OutboxClaimReader`).

Mixed update/delete batches persist their classified collections separately;
each submission whose version moved then gets its revision and its outbox
row, `DELETE` whenever it is deleted afterwards; this boundary has a focused
regression test. Submission pulling, inherited CRUD/read routes,
deprecated `objects`, and the admin delete route still require independent
API-use classification.

The upload-time `MigrationRepeatIdGenerator` is core-active. The separate
migration-error entity, repository, service, and skip/listener chain had no
registered batch job or caller and is source-dead; its physical table remains
a later schema-migration concern.

### Change Record (journal.submission_revision)

Every committed change of a submission is one row of
`journal.submission_revision`, an append-only journal keyed by
`(submission_id, version)`: the submission's internal id
(`data_submission.id`) and its `lock_version` after the change. The journal
has its own schema, `journal`, because downstream analytics reads the current
state only and its copy syncs the `public` and `analytics` schemas: a table
in `journal` is never copied unless someone adds the schema to that source.
Its foreign keys to `public` tables, and the outbox's key to it, cross
schemas; every reference names the schema.
A revision is written if and only if the version moves: creation stores
version 0 and each flushed change raises it by one (JPA `@Version`, which
counts changes only because `SubmissionCommandService` is the only writer).
Whatever a write persists is a change, copied labels included (a re-send
after a village rename is an `UPDATE` with the new name); an unchanged
re-send writes nothing. With `conflict-detection` on, a copy equal to its base
version's state or to the current state writes nothing at all, so it no longer
refreshes copied labels. `DefaultDataSubmissionService` reads the versions
back after each flush and writes, in the same transaction, the revision and
then one outbox row naming it (`SubmissionRevisionWriter`, the only writer).

A revision holds `kind` (`BASELINE`, `CREATE`, `UPDATE`, `DELETE`),
`command`, `previous_version`, exactly one of `actor_user_id` (an
`app_user.id`) and `actor_system` (`MIGRATION`), `on_behalf_of_user_id`,
`reason`, `recorded_at` (the database's transaction time, `timestamptz`),
and the state after the change: `form_data` and `metadata`, the row's other
columns as `journal.submission_revision_metadata()` renders them (form version label,
placement uids and the labels copied with them, status, deleted state, and
`last_modified_date`; times as UTC instants). The writer, the baseline, the
gap repair and the reconciliation all use that one function. Keys are ids,
never uids; a reader joins `data_submission` for the uid and for what never
changes after creation (form, entry times, creation time).

| Command and case | Revision | Actor | On behalf of | Reason | Outbox event |
| --- | --- | --- | --- | --- | --- |
| Upload or legacy upload, new uid | `CREATE` at 0, `UPLOAD` or `LEGACY_UPLOAD` | uploader | | | `SAVE` 0, or `DELETE` 0 if sent deleted |
| Re-send that changes anything persisted | `UPDATE` v+1 | uploader | | | `UPDATE`, or `DELETE` if the record stays deleted |
| `deleted=true` on a live record | `DELETE` v+1 | uploader | | | `DELETE` |
| Unchanged re-send, or `deleted=true` on a deleted record | none | | | | none |
| `conflict-detection` on: an upload item that changes nothing, or a phone copy kept aside as a conflict | none | | | | none |
| Admin delete | `DELETE` v+1, `ADMIN_DELETE` | admin | | | `DELETE` |
| Held release | `CREATE` at 0, `HELD_RELEASE` | admin | uploader | release note | `SAVE` 0 |
| Held or practice upload, discard, superseded or changed release | none | | | | none |
| Baseline (Liquibase, once) | `BASELINE` at `lock_version`, `MIGRATION` | system `MIGRATION` | | | none |
| Gap repair (Liquibase, each start) | `BASELINE` jumping to `lock_version`, `GAP_REPAIR` | system `MIGRATION` | | | none |

The outbox contract:
- `submission_version`: the version after the change. `(submission_id,
  submission_version)` references the revision (`fk_outbox_submission_revision`);
  rows written before step C, or by 6.5.5 (the release before it) after a
  rollback, keep NULL and are not checked.
- `created_at`: the database's transaction time in UTC (column default);
  readers treat it as UTC (`EtlMetrics`, integrity checks 1 and 10).
- `idx_outbox_unversioned`: the rows without a version (those written before
  step C, and any a release without the journal writes), which the gap
  repair probes for its cue. The application's rows are never in it.
- `event_type` follows the state after the change: `DELETE` whenever the
  submission is deleted afterwards, else `SAVE` for a creation and `UPDATE`.
- Actor and kind are not copied into the outbox: the key names the revision.
  The row still carries `payload`, a copy of the submission's `form_data`
  written with each revision, which no code reads (the ETL re-reads the
  submission). Per-consumer progress is not built; the ETL keeps its claim
  state on the row.

The version is what submission review's `ETag` will carry: `ETag:
"<version>"` on a view, `If-Match: "<version>"` on an edit or other change (412 when
stale), for a submission's `lock_version`, which its head revision equals. A
delete, and later a restore, is a version too. Never the form version
(`template_version_uid`, `template_version_no`, the outbox `topic`) and never
the unmapped `data_submission.submission_version` column (1 on every row).
Phones see no version; the v1 shapes are unchanged.

Guards: the primary key; foreign keys to `data_submission` and `app_user`;
checks on each kind's versions and commands, one actor of the right kind, and
a release's uploader; triggers that refuse `UPDATE`, `DELETE` and `TRUNCATE`
of revisions (`submission_revision_append_only`) and any revision that does
not name the submission's head as `previous_version`
(`submission_revision_no_gap`); ArchUnit and the SQL tripwire for the one
writer; failure-injection tests for the single transaction; and
`JournalReconciliation` after every A2 golden. In production, integrity check
11 compares the journal with the rows (counts only). Two kinds of session get
past the triggers: a superuser, by setting `session_replication_role =
replica` (the tests' purge, `SubmissionTestPurge`), and the journal's owner,
by `ALTER TABLE ... DISABLE TRIGGER`. The owner is the application's database
role, which runs the migrations; it is not a superuser on staging
(2026-11-25). No main code or changelog does either; the SQL tripwire checks
both. A database trigger requiring a revision for every write is deferred: it
would stop 6.5.5 from writing after a rollback.

Deploy, rollback and history:
- History starts at the baseline: one `BASELINE` per submission stored before
  step C (deleted ones and the 4,628 never sent to the outbox included), with
  no outbox rows. Changes before it are not reconstructed.
- Rehearsed on 67,176 synthetic submissions sized like staging's (form data
  59 MB stored; staging holds 50 MB), on a container with default settings,
  on a busy workstation (2026-11-25): the baseline took 4.8 to 6.8 s and
  wrote 173 MB of WAL; the journal is 105 MB with its key; the outbox column,
  default and partial index about 0.1 s, the triggers about 20 ms each, the
  outbox key about 80 ms. Check 11 took 3.5 to 6 s on that dataset.
- The gap repair runs at every start, last. It reads every submission only on
  a cue: an outbox row without a version above `journal.outbox_watermark`
  (a release without the journal wrote; the newest such row is one probe of
  `idx_outbox_unversioned`), or a changeset run since it last ran (a deploy).
  Without a cue its statement takes 0.5 ms and the changeset 13 to 19 ms; on
  a cue it took 0.4 s on the first deploy and 0.33 s after a simulated window
  of the previous release with 1,001 gaps. It moves the watermark to the newest outbox row each time. JIT
  is off for it: the estimated cost of the full read would otherwise make
  every start compile it (about 40 ms).
- 6.5.5, the release before step C, runs on the migrated schema (its outbox
  writer is 6.5.4's): it names its outbox columns (the version
  stays NULL, `created_at` is its JVM's UTC), never touches the journal, and
  ignores applied changesets it does not know. Its writes raise versions
  without revisions, and its outbox rows, without a version, are the cue: the
  next start of a release with the journal records one `GAP_REPAIR` per
  submission it wrote (check 11g counts them).
- Never run the Liquibase rollback of the journal once the application wrote
  a revision: it is their only record, and its rollback refuses.
- The analytics copy does not see the journal: its schema is not one the copy
  syncs. Nothing in `data_submission`, tall or events changed.
- Production's migration role must be able to create a schema. On staging
  (2026-11-25) it owns the database and is not a superuser, so it can.

Data repairs and operations SQL (a rule, not yet a guard):
- A data repair changes submissions only through revisions. In the
  transaction of each row it changes, it raises `lock_version` by one and
  inserts an `UPDATE` revision at the new version, naming the head as
  `previous_version`, with `actor_system` and a `reason` naming the repair,
  under a command its own changeset first adds to the journal's checks (for
  example `DATA_REPAIR`: today a system actor may only write baselines). If
  downstream must see the change, it also writes the outbox row naming that
  revision, as the application does.
- Operations SQL never changes `lock_version`, nor a submission's state
  outside that pattern.
- The known case: the repair of the 402 form-version labels that integrity
  check 9a counts.
- Why: a version raised without a revision makes every re-send of that
  submission fail at `submission_revision_no_gap`, and its change is recorded
  without who or why. A changeset that breaks the rule is still recorded at
  its own deploy, as a gap revision without its reason, because the gap
  repair runs after every changeset (`runOrder="last"`) and a deploy is its
  cue. Direct SQL outside a deploy leaves no cue: it is recorded by nothing
  until the gap repair's `INSERT` (changeset c08) is run by hand without its
  cue condition, and check 11b counts the submissions it left behind.

### Outbox Consumer (ETL)

The ETL (`etl/`) is the only consumer of the current `outbox`. For each row it
re-reads the submission and projects it into `analytics.tall_canonical` and
`analytics.events`; a `DELETE` row marks the submission's events deleted. That
projection does not read `submission_version` or the journal yet: the
projector step moves it onto the revision each row names. While the submission
feed is on, the same runtime also hands each row's submission id and
`submission_version` to the feed, which publishes the submission's head from
the journal (`docs/features/submission-feed/contract.md`). What a projection writes or leaves behind (removed rows,
voids, delete markers) belongs to the projector work, not to this runtime.

- **Polling.** `ScheduledOrchestratorRunner` starts a poll
  `datarun.etl.poll-delay` after the previous one ends. When a row is due, it
  takes a transaction-scoped advisory lock on one connection kept for the whole
  run, then claims batches of `datarun.etl.batch-size` while they come back
  full, for at most `datarun.etl.max-poll-duration`. A poll that stops or fails
  is not a success. The pause before the next poll then doubles, up to
  `datarun.etl.max-poll-delay`, until a poll succeeds.
  `datarun.etl.enabled=false` stops an instance from polling. Scheduling is off
  in the test profiles. While the submission feed is on (switch
  `submission-feed`), every poll takes the lock, and once no row is due it runs
  the feed's own work (its retries, republication, first publication and
  reconcile, in that order) one batch at a time, claiming due rows first again
  after each batch, for as long as the poll may run. A reconcile batch that
  finds nothing to publish reads for at most
  `datarun.feed.reconcile-scan-budget` (1 s), which must be shorter than
  `datarun.etl.max-poll-duration` or the application does not start.
- **Claims.** A claim takes `pending` or `failed` rows whose `next_attempt_at`
  is empty or past, in submission-serial then outbox-id order. It leases the
  row by moving `next_attempt_at` forward by `datarun.etl.claim-lease`, so a run
  that dies frees its rows when the lease ends. A claim is not an attempt.
- **Outcomes.** When a row's processing starts, its `attempt` is counted in a
  transaction of its own. The row is then applied, and its outcome recorded,
  in one transaction, which gives up after `datarun.etl.row-timeout`: to the
  submission feed first while it is on, then to tall and events, each part in a
  savepoint of its own, so a part that fails rolls back only its own writes.
  The feed keeps its own failures, its lock wait included, as pending changes
  that it retries itself (`feed_ops.pending`): the row is then `success`, with
  tall and events written. A failure of tall or events, or a feed failure that
  could not be recorded as a pending change, is recorded on the row in the same
  transaction, beside what the other parts wrote. When the transaction cannot
  commit (its time ran out), the other parts apply the row again in a new
  transaction with that failure, and only if that fails too is the failure
  recorded after a rollback, in a transaction of its own:
  - before the last attempt, the row becomes `failed`. It keeps a
    `last_error`: the database's first error line or the root cause, never
    bound values. It is retried after `datarun.etl.retry-delay`, doubling up
    to `datarun.etl.max-retry-delay`.
  - It becomes `dead` on attempt `datarun.etl.max-attempts`, when its
    submission is gone, or when it was started more often than it may be
    tried (a row that kills its batch).

  A lost connection, a database shutdown or an interrupt also stops the batch.
  The row that met it is recorded as failed with the usual retry wait, so
  neither an outage nor a poison row can hold the head of the queue. A row dies
  only on the normal schedule. The rows not started are released for the next
  run without losing an attempt, and `etl_runs` records the run as `stopped`.

  `outbox_processing` keeps one audit row per run and row: `success`,
  `failure` or `dead`.
- **The feed's admin route.** `/management/feedpublication` starts the feed's
  first publication and republishes a scope. Both are operational steps, run
  on the owner's go: the first publication at a chosen hour once the feed is
  switched on (phase B of the feed's rollout), a republication after a fix.
  The admin's v1 tools do not call it. Only an admin reaches it, as every
  management route but health and info (exposed in application.yml), and both
  are refused (409) while the feed is off. `GET` shows where the first
  publication stands and how many republish marks wait.
  `POST {"action": "first-publication"}` starts the first
  publication: every submission's head, in id order, in batches of
  `datarun.feed.batch-size` (200 by default) behind the live backlog. It is
  resumable: its mark, in `feed_ops.first_publication`, lets a restart go on
  from where it stopped, and a second start while one is under way is refused
  (409). `POST {"action": "republication"}`, with `formId` or `submissionIds`
  (comma-separated) or neither for every form, marks the records the feed holds
  in that scope; each is written again at its held version with new positions,
  skipping a record whose held version is newer than its head. Neither writes
  an outbox row. The first publication writes every head, and reconcile
  repairs after it, never in its place (engineering, 2026-11-26). Until the
  first publication has completed, reconcile publishes no head the feed lacks
  and counts them in `unpublished`, so switching the feed on publishes only the
  live changes and the records the feed holds at another version. Only a first
  publication that wrote every head it passed sets
  `first_publication_completed_at` in `feed.freshness`. One that ends with
  failures leaves them as pending changes; start it again once their cause is
  fixed, and it skips the heads already held. From then on reconcile publishes
  every head the feed lacks or holds at another version, at start, at switch-on
  and nightly.
- **Dead rows.** A dead row is not claimed again. Put one back only after its
  cause is fixed, and reset its attempts, or the next claim marks it dead again:
  `UPDATE outbox SET status = 'pending', attempt = 0, next_attempt_at = NULL
  WHERE outbox_id IN (...)`.
- **Metrics.** Micrometer meters are served at `/management/prometheus` where
  Prometheus export is on. Production and staging turn it on in their compose
  files (`MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED=true`), which outranks
  `application-prod.yml`'s `false`:
  - `datarun_etl_outbox_rows{status}`: the backlog by status;
  - `datarun_etl_outbox_oldest_age_seconds`: the age of the oldest change not
    yet projected;
  - `datarun_etl_last_success_seconds`: when a poll last ended without error or
    stop;
  - `datarun_etl_polls_total{outcome}`: polls that succeeded, stopped or failed;
  - `datarun_etl_outbox_processed_total{outcome}`: rows processed, by outcome;
  - `datarun_etl_outbox_processing_seconds`: the time per row.

  A scheduled task reads the backlog every `datarun.etl.backlog-refresh`
  (15 s), so a scrape never waits on the database, even when the pool is
  exhausted. Since 6.5.5 a scrape needs ADMIN (HTTP Basic or a bearer token),
  so a scraper needs credentials or an internal-only path.
- **Monitoring.** GlitchTip watches production. Each part is off, and sends
  nothing, while its `.env` value is empty.
  - Errors (`SENTRY_DSN`, `SENTRY_ENVIRONMENT`): every ERROR log line and
    uncaught exception of the application becomes an event in the GlitchTip
    project `datarun-api` (Sentry Java SDK, `ErrorReportingConfiguration`).
    An event carries the logger, exception types, stack frames, the matched
    route (none before routing, never the raw path), the request id, the
    release and the environment, and for the application's own lines
    (`org.nmcpye.datarun`) the line's pattern, so those lines pass values only
    as `{}` arguments. Another library's line arrives as its logger's name:
    Hibernate and Undertow format values into the text (Undertow's UT005023
    names the request path; Hibernate logs the database's message). An event
    never carries the line's arguments, exception messages, the request, the
    user or breadcrumbs, and nothing is traced. Request errors that the REST
    layer answers are log lines (Request errors, below); only a 5xx's is
    reported. Nor is anything before the application context is up: the SDK
    starts during the context's refresh and attaches to the log at its end,
    so a failed startup and the Liquibase migrations, which production runs
    during the refresh, show in the container log only. A malformed DSN leaves
    reporting off, with one WARN at startup that does not repeat it.
    The container log keeps every line in full, in at most 5 files of 20 MB
    (json-file rotation in `deploy/production/compose.yml` since 6.5.5; before
    that it never rotated). Database errors there leave
    out the failing row only because the pool and both JDBC URLs set
    `logServerErrorDetail=false`; PostgreSQL's own message can still quote an
    input value (`invalid input syntax for type integer: "…"`).
  - Request errors: `ExceptionTranslator` logs one line for each error it
    answers (6.5.5). A server fault (5xx) is an ERROR line, so GlitchTip gets
    its pattern, the route and the request id, one issue per method and route,
    outermost exception type and SQLState (a fingerprint taken from the line's
    arguments before they are dropped): `Request GET /api/v1/... failed
    with 500 (error.http.500): DataIntegrityViolationException <
    PSQLException, SQLState 23505, at ..., request <id>`. Any other error
    (4xx), an unreadable body included, is an INFO line: `Request POST
    /api/v1/dataSubmission/bulk answered 400 (error.http.400):
    HttpMessageNotReadableException < JsonParseException, request <id>`. Both
    pass every value as an argument and name the method, the route pattern
    (never the raw path), the status, the error code and the exception types,
    never a message, a body or a value. A failed login's WARN line
    (Authentication above) is its only record. Rates come from the
    `http.server.requests` metrics, where Prometheus export is on.
  - `error_event` is no longer written or read (6.5.5). The table stays for
    one release, because 6.5.4, the rollback target, still writes it; a later
    release drops it. The 6.5.5 deploy empties it once (changeset
    `20261125-04`, owner's decision of 2026-11-25). That is irreversible: the
    purged rows survive only in the nightly Proxmox backups (7 to 14 days).
  - Heartbeat (`DATARUN_ETL_HEARTBEAT_URL`, `datarun.etl.heartbeat-url`):
    after a poll that succeeded, the ETL sends a POST to the monitor, at most
    once a minute, and gives up after 5 s. A failure is logged at DEBUG and
    never touches the poll. A healthy ETL pings about every 60 to 90 s. While
    the submission feed is on, a poll pings only while the feed is within its
    freshness target (`datarun.feed.freshness-target`, 2 minutes): nothing
    dead, and the oldest change it has not published and its oldest pending
    change both younger than the target (`feed.freshness`). Until tall and
    events retire this is the ETL's heartbeat, which the feed's freshness also
    gates; once they retire, it is the feed's heartbeat.
  - One-time setup in GlitchTip (owner): create the project `datarun-api`
    (platform Java) and copy its DSN. Under Uptime Monitors, add a monitor of
    type Heartbeat with an interval of 300 s and copy its URL, of the form
    `/api/0/organizations/<org>/heartbeat_check/<uuid>/`. GlitchTip 6.0.7 has
    no separate grace setting: the monitor is down when no ping arrived within
    the last interval, so the 300 s are the grace for a few missed pings, and
    it turns down 5 to 10 minutes after the last one. An alert channel is
    optional; without one, issues and the monitor's state show in GlitchTip.
  - Values on the API VM: put the DSN and the heartbeat URL into the
    production `.env` exactly as GlitchTip shows them, with
    `SENTRY_ENVIRONMENT=production` (`deploy/production/.env.example`). The
    API VM must resolve GlitchTip's public name to the internal reverse proxy
    (split-horizon DNS on the lab router); without that, the name does not
    resolve from inside the lab and nothing arrives. Check from the container
    without printing the URL; GlitchTip answers 200:
    `docker exec datarunapi sh -c 'curl -sS -o /dev/null -w "%{http_code}\n"
    -X POST "$DATARUN_ETL_HEARTBEAT_URL"'`. Staging's deploy script writes
    its own `.env` without these values, so staging reports nothing.
  - What GlitchTip shows, and what to do:
    - An issue `Outbox row {} is dead after attempt {}: {}`: rows were set
      aside as dead. List them with `SELECT outbox_id, attempt, last_error
      FROM outbox WHERE status = 'dead';`, fix the cause, then put them back
      (Dead rows above).
    - The heartbeat monitor is down: no poll has succeeded within the
      interval. The application is down or restarting (a long deploy shows
      this too), the ETL is switched off, its polls keep failing (the pause
      grows to five minutes), or the API VM cannot reach GlitchTip. With the
      submission feed on, also when the feed is behind its freshness target or
      has a dead change: the log says `The feed is behind its freshness
      target`, and `feed.freshness` and `feed_ops.pending` (with its reason
      codes) show what waits. Check that `datarunapi` runs, its log
      (`docker logs --since 30m datarunapi`), then the outbox. The next
      successful poll within the target brings the monitor back up.
    - Any other issue: an ERROR line or uncaught exception. The full line,
      with its values, is in the application's log at that time; an event
      raised during a request carries the request id the log line shows.
      The one ERROR line kept back on purpose is Hibernate's for the expected
      duplicate-uid race of submissions (a unique violation of
      `uk_data_submission_uid`); every other database error is reported.
- **Index.** The partial index `idx_outbox_unfinished` (pending, failed and
  dead rows, in claim order) keeps the due check, the claim and the backlog
  gauges off the processed history. Liquibase builds it at startup with a plain
  `CREATE INDEX` and `lock_timeout` 30s (Liquibase parameter
  `outboxIndexLockTimeout`), so a held write lock fails the deploy instead of
  stalling it.
- **Tall's indexes.** `analytics.tall_canonical` keeps its primary key, the
  upsert's key `uq_tall_instance_canonical`, and the indexes on
  `submission_uid` and on (`template_uid`, `canonical_element_id`). Changeset
  `20261125-01` (6.5.5) drops five that production never scanned in 21.6 days
  (repeat and parent instance, template version and element, `value_ref_uid`,
  `updated_at`; 81 MB). Its `DROP INDEX` waits at most 30 s for its lock
  (Liquibase parameter `tallIndexLockTimeout`), so check that no dump or
  Airbyte read holds `analytics.tall_canonical` right before deploying.
- **Before a deploy that changes the ETL.** Update the production host's
  `compose.yml` from `deploy/production/compose.yml` (`release-boundary.md`,
  step 3). Both JDBC URLs there carry `logServerErrorDetail=false`. The pool
  also gets it from the application's configuration, but Liquibase's connection
  gets it only from its URL. Check the outbox right before deploying, and
  compare with the release notes:
  `SELECT status, attempt, next_attempt_at IS NOT NULL AS leased, count(*)
  FROM outbox GROUP BY 1, 2, 3 ORDER BY 1, 2, 3;`.
  - Under the release before this runtime, every row is `success` with attempt
    0, and nothing is pending, failed or leased.
  - Rows pending at deploy time are projected by the first poll.
  - Anything else (failed, dead, attempt above 0) was changed by hand: find
    out why before deploying.
- **Known residual (projector step).** An array or object value longer than
  10,000 characters is cut into invalid JSON (`TransformServiceRobust`,
  `TallCanonicalJdbcRepository`). Its row fails on every attempt with
  `invalid input syntax for type json` and is dead after about four hours.
  This is latent in production today; its fix belongs to the projector step,
  not to this runtime.
- **Migrations at deploy time.**
  - Never restart or stop the application while it applies migrations.
  - If a crash left Liquibase's lock held, startup logs "Waiting for changelog
    lock" and gives up after five minutes. Check
    `SELECT locked, lockgranted, lockedby FROM databasechangeloglock;`. Confirm
    no session is still migrating:
    `SELECT pid, application_name, state, now() - query_start AS running,
    left(query, 60) FROM pg_stat_activity WHERE datname = current_database() AND
    state <> 'idle';`. Only then release it:
    `UPDATE databasechangeloglock SET locked = false, lockgranted = NULL,
    lockedby = NULL WHERE id = 1;`.
  - To see what a migration waits on:
    `SELECT pid, wait_event_type, wait_event, pg_blocking_pids(pid) AS blocked_by,
    now() - query_start AS waiting, left(query, 60) FROM pg_stat_activity WHERE
    datname = current_database() AND wait_event_type = 'Lock';`. The blocking
    pids are the sessions holding the lock.

## Cleanup Cycle

For each domain: map contract, remove source-dead alternatives, reconcile
duplicated active ownership, assess unused APIs, then handle schema residue in
a separate Liquibase migration. Close each step with focused tests, the full
test gate, and the smallest applicable mobile smoke.

## Pending Endpoint Removal Decisions

Status: **WAITING FOR USER CONFIRMATION**. These routes remain registered.
Absence from the released mobile and this repository is evidence, not proof
that no external operator or older client uses them.

| Candidate | Proposed action | Evidence | Confidence / compatibility risk |
| --- | --- | --- | --- |
| `POST /api/{v1,custom}/assignments/forms` | retain GET; remove POST method registration | released mobile uses GET; no in-repo POST caller; both methods currently run the same read handler | high / older external client unknown |
| inherited assignment writes: `POST`, `POST /bulk`, `POST /return`, `PUT /{uid}`, `DELETE /{id}` | remove write surface | assignment synchronization is read-only; no in-repo caller | medium / possible manual admin use |
| `GET /api/{v1,custom}/assignments/updatePaths` | assess manual use, then remove or restrict to the maintenance owner | no mobile/in-repo caller; path maintenance also has a service/scheduled owner | medium / operator use unknown |
| `GET /api/{v1,custom}/teams/managed` | remove after external-client confirmation; then retire `managedTeamsUIDs` from the security principal/V1 profile | released mobile gets managed teams embedded in direct-team sync and never calls this route | high / external admin client unknown |
| inherited `/api/{v1,custom}/userGroups` CRUD | remove source, then handle tables in a separate Liquibase cutover | no released-mobile call or active authentication/access decision uses user groups | high / external admin client unknown |
| inherited `formTemplates` writes | remove; retain `dataFormTemplates` as the operational authoring boundary | mobile reads only; full-template authoring has a separate validated/versioned endpoint | high / external direct writer unknown |
| inherited `formTemplateVersions` writes | remove | mobile reads only; controller overrides save with a no-op, so POST routes misleadingly report without persisting | high / clients may rely on broken behavior |
| generic assignment/form reads `/byLastModified`, `/query`, and `/{id}` | remove only after access-log/operator confirmation | no released-mobile or in-repo caller | medium / external reads unknown |
| `GET|POST /api/{v1,custom}/dataSubmission/objects` | remove | deprecated flattened-read endpoint; no released-mobile or in-repo caller | high / older reporting client unknown |
| generic submission reads `GET /`, `/byLastModified`, `POST /query`, `GET /{id}` | remove only after access-log/operator confirmation | submission pull is disabled; no current mobile caller | medium / older pull/reporting client unknown |
| unversioned submission upload `POST /bulk` (`POST /` and `POST /return` answer 405 since 6.5.5) | retain until old-client compatibility is explicitly retired | current mobile uses `bulk?referenceVersion=1`; production's access logs of 2026-11-10 to 24 hold 214 unversioned `POST /bulk` (192 answered 200) | low removal confidence / highest client risk |
| submission `DELETE /{id}` (`PUT /{uid}` answers 405 since 6.5.5) | defer to synced edit/delete policy | no current mobile caller; admin-only route exists, but lifecycle policy is incomplete | low removal confidence / product-policy risk |
| `/api/custom` aliases | replace remaining administrator/operational callers with `/api/v1`, then remove | released mobile uses `/api/v1`; recent production access logs still contain successful custom calls | medium / caller must be identified during administrator smoke |
