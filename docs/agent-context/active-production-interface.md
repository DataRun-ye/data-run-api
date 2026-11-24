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

JWT claims identify the subject, but request authority comes from the current
activated database user and current database authorities. A stale token claim
cannot preserve administrator access after the database role changes.
Deactivated users cannot receive or rotate tokens. All routes outside the
explicit allowlist fail closed.

HTTP Basic and public Prometheus access remain baseline operational
compatibility pending evidence from the administrator and monitoring smoke.
They are not canonicalized by this transition.

## Assignment

| Status | Released mobile request | Server owner | Required downstream path |
| --- | --- | --- | --- |
| CORE-ACTIVE | `GET /api/v1/assignments?paged=false` | `AssignmentResource` inherited read route | `DefaultAssignmentService.findAllByUser` -> `AssignmentFilter` -> `AssignmentRepository` |
| CORE-ACTIVE | `GET /api/v1/assignments/forms?paged=false&referenceVersion=1` | `AssignmentResource.getAllDto` | access-filtered assignments -> `AssignmentWithAccessMapper` -> `AssignmentFormAccessService` -> `ReferenceAssignmentFormGate` |
| GATED | `GET /api/v1/assignments/{uid}/referenceEntries` | `ReferenceEntryResource` | `AssignmentService.findAccessibleByIdOrUid` and assignment/form access checks; deployed but unused until a Reference form is assigned |
| SUPPORTING-REACHABLE | `GET /api/v1/formPermissions` | `UserFormPermissionsResource` | the mobile synchronizes this into `user_form_permissions`, but no active mobile behavior reads that table; retire mobile registration/table first, then this endpoint |

The released mobile owners are `AssignmentDatasource` and
`ReferenceEntryDatasource`. Assignment synchronization persists assignments
and replaces the local assignment-form projection only after its secondary
request succeeds.

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
| CORE-ACTIVE | `POST /api/v1/dataSubmission/bulk?referenceVersion=1` | `DataSubmissionResource.saveVersionedUpload` -> `SubmissionCommandService.upload` -> `SubmissionUploads` -> `DefaultDataSubmissionService.upsertAll` |
| CORE-ACTIVE (app 6.1.0+) | `POST /api/v1/dataSubmission/bulk/practice?referenceVersion=1` | `DataSubmissionResource.savePracticeUpload` -> `SubmissionCommandService.uploadPractice` (practice: always held) |
| ADMIN | `GET`/`PUT /api/v1/admin/goLiveRules`, `DELETE /{uid}`, `GET /effective` | `GoLiveRuleResource` -> `GoLiveRuleService` -> `GoLiveRuleRepository` (`go_live_rule`) |
| ADMIN | `GET /api/v1/admin/heldSubmissions`, `GET /summary`, `GET /{uid}` | `HeldSubmissionResource` -> `HeldSubmissionRepository` (`held_submission`, reads) |
| ADMIN | `POST /api/v1/admin/heldSubmissions/release`, `POST /discard` | `HeldSubmissionResource` -> `SubmissionCommandService.releaseHeld` / `discardHeld` -> `HeldSubmissionDecisions` |
| LEGACY-RISK / UNKNOWN | `GET /api/v1/dataSubmission`, `GET /byLastModified`, `POST /query`, `GET /{id}` | inherited generic read surface |
| LEGACY-RISK / UNKNOWN | `POST /api/v1/dataSubmission/bulk` without the version parameter | `DataSubmissionResource.saveAll` -> `SubmissionCommandService.uploadLegacy` |
| LEGACY-RISK / UNKNOWN | `POST /api/v1/dataSubmission`, `POST /return` | inherited routes; bean validation answers 400 to every body (the id cannot be bound); their body would go through `uploadLegacy` |
| LEGACY-RISK / UNKNOWN | `GET|POST /api/v1/dataSubmission/objects` | deprecated flattened read surface |
| ADMIN / UNKNOWN | `DELETE /api/v1/dataSubmission/{uid}` | `DataSubmissionResource.deleteByIdUid` -> `SubmissionCommandService.delete` |
| ADMIN / UNKNOWN | `PUT /api/v1/dataSubmission/{uid}` | inherited generic update; bean validation answers 400 before its body runs |

Every submission write goes through `SubmissionCommandService`
(`jpa.datasubmission.command`): controllers adapt each route to a
`SubmissionCommand` (upload, practice upload, legacy upload, admin delete,
held release, held discard) naming the acting `SubmissionActor`: the durable
user id, plus the login that today's audit columns store. Each submission is
written in a transaction of its own, with its `outbox` row, and
`SubmissionWriteOwnershipTest` (ArchUnit) fails the build when any other class
calls the write methods of the persistence layer (`DataSubmissionService`, the
submission repository, `SubmissionIntake.hold`, `HeldSubmissionRepository`,
`OutboxWriter`); it accepts no violation. The rule cannot see writes through
the generic `IdentifiableObjectService` types; the inherited `PUT` body is the
one such path, and it answers 400 before it runs. Log lines on these paths carry
identifiers and shapes only: submission and form uids, the user id, counts,
error codes and exception types, never a login, a submitted value or an
exception message.

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

The area chain is read by walking
parents, never through `OrgUnit.getPath()`, which writes the org unit when
called in a committing transaction. The unversioned legacy writes are never
held. The connection pool does not auto-commit, so JDBC writes outside a
transaction are lost: intake writes are `@Transactional`. Never run the
Liquibase rollback of `held_submission` once uploads were held: it would
delete uploads that phones report as sent.

The admin delete marks the submission deleted and writes its `DELETE`
event in one transaction. Deleting a deleted submission writes nothing, as an
upload's `deleted=true` does, and an unknown uid answers 404.

The unversioned writes are not used by the released mobile and remain
`LEGACY-RISK` pending the recorded endpoint-use decision; their reachability
does not make them a second product authority.

The disabled submission-history listener and its zero-caller processor/model
alternatives are source-dead. Their physical table remains a schema concern.
The separate `jpa/datasubmissionoutbox` tree targets the legacy
`outbox_event` table; it has no active producer or enabled worker and is not
the current `outbox` owner.

Mixed update/delete batches persist their classified collections separately
and emit `UPDATE` and `DELETE` through the current outbox; this boundary has
a focused regression test. Submission pulling, inherited CRUD/read routes,
deprecated `objects`, and the admin delete route still require independent
API-use classification.

The upload-time `MigrationRepeatIdGenerator` is core-active. The separate
migration-error entity, repository, service, and skip/listener chain had no
registered batch job or caller and is source-dead; its physical table remains
a later schema-migration concern.

### Outbox Consumer (ETL)

The ETL (`etl/`) is the only consumer of the current `outbox`. For each row it
re-reads the submission and projects it into `analytics.tall_canonical` and
`analytics.events`. What a projection writes or leaves behind (removed rows,
voids, delete markers) belongs to the projector work, not to this runtime.

- **Polling.** `ScheduledOrchestratorRunner` starts a poll
  `datarun.etl.poll-delay` after the previous one ends. When a row is due, it
  takes a transaction-scoped advisory lock on one connection kept for the whole
  run, then claims batches of `datarun.etl.batch-size` while they come back
  full, for at most `datarun.etl.max-poll-duration`. A poll that stops or fails
  is not a success. The pause before the next poll then doubles, up to
  `datarun.etl.max-poll-delay`, until a poll succeeds.
  `datarun.etl.enabled=false` stops an instance from polling. Scheduling is off
  in the test profiles.
- **Claims.** A claim takes `pending` or `failed` rows whose `next_attempt_at`
  is empty or past, in submission-serial then outbox-id order. It leases the
  row by moving `next_attempt_at` forward by `datarun.etl.claim-lease`, so a run
  that dies frees its rows when the lease ends. A claim is not an attempt.
- **Outcomes.** When a row's processing starts, its `attempt` is counted in a
  transaction of its own. The row is then projected and marked `success` in
  one transaction, which gives up after `datarun.etl.row-timeout`. A failure of
  any kind, errors included, is recorded after that transaction has rolled
  back:
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
- **Dead rows.** A dead row is not claimed again. Put one back only after its
  cause is fixed, and reset its attempts, or the next claim marks it dead again:
  `UPDATE outbox SET status = 'pending', attempt = 0, next_attempt_at = NULL
  WHERE outbox_id IN (...)`.
- **Metrics.** Micrometer meters are served at `/management/prometheus` where
  Prometheus export is on:
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
  exhausted. The application serves `/management/prometheus` without a login.
  Operations should limit `/management/` at nginx to the monitoring host.
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
    layer answers stay in `error_event` (Error records, below) and are not
    reported. Nor is anything before the application context is up: the SDK
    starts during the context's refresh and attaches to the log at its end,
    so a failed startup and the Liquibase migrations, which production runs
    during the refresh, show in the container log only. A malformed DSN leaves
    reporting off, with one WARN at startup that does not repeat it.
    The container log keeps every line in full. Database errors there leave
    out the failing row only because the pool and both JDBC URLs set
    `logServerErrorDetail=false`; PostgreSQL's own message can still quote an
    input value (`invalid input syntax for type integer: "…"`).
  - Error records (`error_event`): one row per request error that the REST
    layer answers. Since 6.5.5 a row holds no exception message, which
    quoted submitted values, request bodies and database rows: `message`
    holds the error's code (the answer's message key, such as
    `error.validation` or `error.http.401`), `exception` its type, and
    `stacktrace` the exception types and frames (cut at 4,000 characters).
    `context_json` holds identifiers: the request id of the log lines and of
    the `X-Request-ID` answer, the user id, the causes' exception types, and
    for a database error the SQLState and the constraint. Older rows keep
    their messages. `username` is unchanged: the login a failed login
    attempted, otherwise mostly `anonymous`, because production writes the
    row on another thread. Nothing in code reads the table.
  - Heartbeat (`DATARUN_ETL_HEARTBEAT_URL`, `datarun.etl.heartbeat-url`):
    after a poll that succeeded, the ETL sends a POST to the monitor, at most
    once a minute, and gives up after 5 s. A failure is logged at DEBUG and
    never touches the poll. A healthy ETL pings about every 60 to 90 s.
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
      grows to five minutes), or the API VM cannot reach GlitchTip. Check that
      `datarunapi` runs, its log (`docker logs --since 30m datarunapi`), then
      the outbox. The next successful poll brings the monitor back up.
    - Any other issue: an ERROR line or uncaught exception. The full line,
      with its values, is in the application's log at that time; an event
      raised during a request carries the request id the log line shows.
- **Index.** The partial index `idx_outbox_unfinished` (pending, failed and
  dead rows, in claim order) keeps the due check, the claim and the backlog
  gauges off the processed history. Liquibase builds it at startup with a plain
  `CREATE INDEX` and `lock_timeout` 30s (Liquibase parameter
  `outboxIndexLockTimeout`), so a held write lock fails the deploy instead of
  stalling it.
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
| unversioned submission writes `POST /bulk`, `POST /`, `POST /return` | retain until old-client compatibility is explicitly retired | current mobile uses `bulk?referenceVersion=1`; older clients may use the unversioned payload | low removal confidence / highest client risk |
| submission `PUT /{uid}` and `DELETE /{id}` | defer to synced edit/delete policy | no current mobile caller; admin-only route exists, but lifecycle policy is incomplete | low removal confidence / product-policy risk |
| `/api/custom` aliases | replace remaining administrator/operational callers with `/api/v1`, then remove | released mobile uses `/api/v1`; recent production access logs still contain successful custom calls | medium / caller must be identified during administrator smoke |
