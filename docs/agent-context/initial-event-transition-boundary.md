# Initial Event Transition Boundary

Role: current implementation, compatibility, and cutover map for the initial
event transition

Status: CANDIDATE - NOT DEPLOYED

## Current Standing

- The deployed server is `v6.4.1` from `main`.
- The released mobile compatibility baseline is `6.0.3+54`.
- The transition implementation is on
  `architecture/initial-event-transition`; it is not merged into `develop` or
  deployed.
- Production has not received the transition migrations, bootstrap facts, or
  projections.
- `datarun.transition.capture-live-shadow-enabled` is `false` in every
  committed profile.

This branch is a credible first transition candidate, not a completed event
architecture or a production release. Released assignment reads and
versioned-upload authorization use event-backed grants while the released HTTP
and database contracts remain intact. Assignment projections are now exactly
rebuildable from immutable facts. Capture events remain optional shadow state;
`data_submission` and the current `outbox` are still capture authority.

## Boundary Map

| Boundary | Candidate authority | Compatibility surface | Current transition state |
| --- | --- | --- | --- |
| Assignment mutation | `AssignmentAuthorityCommandService` owns assignment, team, and activity mutations that affect capture authority | Existing assignment, team, activity, membership, and form-permission rows and DTOs remain written | The command mutates the baseline model and reconciles lifecycle events/projections in one transaction. The journal is not yet the sole mutation input. |
| Assignment lifecycle | Immutable `assignment_changed` facts plus `assignment_grant_projection` | Baseline assignment UID, team grouping, activity, org-unit, and form rows remain resolvable | Bootstrapped and maintained for active command routes; isolated validate/repair replay derives the projection without baseline authority reads. |
| Released work reads | `ReleasedWorkReadAuthority` reads highest-generation event grants | `/api/v1` assignment, assignment-form, org-unit, and Reference response shapes are unchanged | Cut over on the candidate branch. Missing or inconsistent event authority fails closed. |
| Versioned upload authorization | `VersionedUploadEventAuthorizer` uses the accepted assignment grant | Baseline logic only preserves released denial codes, administrator acceptance, and eligible late upload against an ended assignment | Cut over on the candidate branch. |
| Submission mutation | `DefaultDataSubmissionService` classifies create, update, delete, and unchanged retry | `data_submission`, response summaries, and current `outbox` remain authoritative | Same-UID exact retry is a successful write/outbox no-op. |
| Capture facts | `VersionedCaptureCommand` can append immutable facts and advance `capture_current_projection` | The same versioned upload request and response are retained | Shadow only, disabled by default, and executed after baseline persistence in the same transaction. |
| Capture current state | `data_submission` | Bootstrap/live facts and `capture_current_projection` are comparison and replay state | No authority cutover yet. |
| Forms and repeats | Stored template versions and whole `formData` JSON | Existing form DTOs, repeat metadata, Reference behavior, and mobile form engine | Unchanged by this transition. |
| Downstream projection | Current `outbox`, ETL, tall tables, pivots, and exports | Existing consumers continue unchanged | They do not read the transition journal yet and are not event authority. |

## Transition Persistence

The additive transition schema contains:

- `event_journal`: immutable assignment and capture facts only;
- `transition_checkpoint`: immutable operational migration/bootstrap
  completion metadata, deliberately outside the event journal;
- `actor_identity_link`, `org_unit_identity_link`, and
  `assignment_identity_link`: immutable aliases from baseline identities;
- `assignment_role_definition`: one canonical activity/form-set capture role;
- `assignment_grant_projection` and `assignment_access_projection`: rebuildable
  current assignment authority;
- `capture_identity_link`: immutable submission-to-capture identity;
- `capture_current_projection`: rebuildable pointer to the current capture
  fact.

Bootstrap facts reconstruct the state observable in the production clone.
They are not invented historical assignment or submission timelines.

## Transferable Event Contract

The candidate mechanically protects the accepted parts that are usable now:

- `event_type` is the closed structural vocabulary `capture`, `review`,
  `alert`, `task_created`, `task_completed`, and `assignment_changed`;
- `shape_ref` follows `{shape_name}/v{version}` and remains distinct from
  structural event type;
- event subjects use the typed categories `subject`, `actor`, `assignment`,
  and `process`;
- `assignment_created/v1` and `assignment_ended/v1` are
  `assignment_changed` facts about one assignment identity and emit the
  accepted payload fields;
- a capture remains `event_type=capture`; its current subject is the assigned
  organization unit represented as a typed `subject`, while its separate
  capture ID is only the immutable stream/projection key;
- current assignment authority is a rebuildable projection, not a mutable
  second source of truth.

The transition journal is intentionally a reduced internal server envelope.
It maps event ID, structural type, shape, activity, typed subject, actor,
recorded time, and payload without inventing final device ID, device sequence,
or sync-watermark values that released clients do not provide. A future native
event transport must add those semantics at its own boundary; it must not
reinterpret this internal journal as the complete wire envelope.

Temporary transition adapters are explicit:

- `baseline_assignment_observed/v1` records migration-observed assignment
  state, not fabricated history;
- `baseline_submission_captured/v1` and `capture_state_accepted/v1` preserve
  the baseline submission snapshot, pinned template version, and accepted
  upload provenance until capture authority is cut over;
- immutable identity links map baseline 11-character external UIDs to typed
  event identities;
- baseline mutation commands still write baseline rows and event facts in one
  transaction until event append becomes the accepted command.

Baseline-only authority remains equally explicit: `data_submission` and the
current `outbox` still own capture state and downstream delivery. ETL, tall
tables, pivots, and exports remain downstream projections and never authorize
work.

## Released Mobile Contract

The candidate must preserve the requests, payloads, responses, error codes,
and offline behavior used by mobile `6.0.3+54`.

The transition directly touches these released requests:

| Released request | Candidate behavior |
| --- | --- |
| `GET /api/v1/assignments?paged=false` | Same DTOs, filtered by event-backed assignment grants |
| `GET /api/v1/assignments/forms?paged=false&referenceVersion=1` | Same DTOs, form set derived from the same grants |
| `GET /api/v1/orgUnits?paged=false` | Same DTOs, direct assignment scopes plus required ancestors |
| `GET /api/v1/assignments/{uid}/referenceEntries` | Same paginated catalog contract, authorized by the same grant |
| `POST /api/v1/dataSubmission/bulk?referenceVersion=1` | Same versioned DTO, summary, errors, whole-JSON persistence, repeat compatibility, Reference handling, and outbox behavior |

Authentication, `/api/v1/myDetails`, projects, activities, levels, option
sets, teams, form templates, template versions, and form permissions retain
their released wire contracts. Submission pull remains inactive.

Compatibility is not proved merely by unit tests. Before integration or
deployment, an installed production mobile `6.0.3+54` must complete login,
configuration sync, ordinary form open/save/reopen/upload, and Reference
selection/create/reopen/upload against the prepared candidate environment.

## Administrator And Operator Contract

There is no administrator frontend in this repository, so a complete
administrator compatibility claim cannot be derived from source reachability
alone.

Known candidate behavior:

- registered assignment, team, and activity authority-changing writes route
  through `AssignmentAuthorityCommandService`;
- form authoring and immutable template publication remain on their existing
  owner;
- user/authentication administration remains outside the transition;
- generic/custom assignment reads retain baseline compatibility;
- unversioned submission writes and administrator submission `PUT`/`DELETE`
  do not append capture facts.

Before capture events become authority, every actually used administrator or
external submission-write route must either:

1. route through the capture command;
2. remain behind a named compatibility adapter with retirement evidence; or
3. be removed after caller evidence confirms it is unused.

The evidence task is an endpoint inventory from the actual operator workflow
and, where available, production access logs. It is not permission to redesign
administrator behavior.

## Bootstrap And Runtime Requirement

The candidate cannot be pointed at an untouched production clone and treated
as ready after ordinary startup alone.

Required order for event-authorized assignment reads:

```text
restore isolated clone
  -> apply candidate Liquibase migrations
  -> run assignment bootstrap and require exact comparison
  -> start candidate API
  -> run client compatibility smoke
```

Liquibase creates empty transition tables. Until assignment bootstrap writes
its checkpoint, the candidate fails closed for field-user assignment,
assignment-form, org-unit, Reference, and versioned-upload authorization.

Capture bootstrap and replay are a separate activation prerequisite. They are
not required while live capture shadowing remains disabled:

```text
assignment-compatible candidate
  -> isolate writers
  -> capture bootstrap
  -> capture replay/validation
  -> enable live capture shadow in that environment
```

## Database Environment Roles

- **Archived dump**: immutable input artifact. The
  `nmcpdb-production-20260725.dump` file is never migrated or used as a live
  database.
- **Disposable compatibility clone**: restored from the archived dump,
  migrated, bootstrapped, compared, exercised, then discarded or restored.
  This proves production-data compatibility.
- **Staging clone**: a separate resettable database prepared from the same
  dump and retained long enough for installed mobile and administrator smoke.
  It is not the archived dump and does not use production credentials.
- **Development database**: small synthetic data for fast implementation
  loops. It does not prove production compatibility.
- **Production database**: never used by local tests. Migration, bootstrap,
  activation, and rollback require a separately approved release window.

Restoring the disposable clone to the untouched dump after earlier gates was
correct: the dump is reusable input, not a progressively migrated environment.
For this closure, the disposable clone is currently migrated and bootstrapped
only long enough to prove the exact replay gate; it is still not the retained
staging environment. What is missing is a separately prepared staging clone
that remains migrated and bootstrapped for client smoke.

`deploy/staging/compose.yml` currently starts only the candidate API. It assumes
that the database, Docker network, credentials, migrations, and required
bootstrap already exist; it does not prepare them.

## Executable Evidence

The completed branch work records:

- exact assignment bootstrap and retry comparisons against the production
  clone;
- event-authorized assignment/form/org-unit/Reference HTTP equivalence;
- ordinary and Reference versioned-upload authorization, denial rollback, and
  eligible ended-assignment upload;
- exact bootstrap of all 52,535 cloned submissions;
- capture pointer validation and reconstruction without changing journal
  content;
- live capture create/update/delete/retry transaction and replay tests;
- closed-vocabulary and known shape/envelope persistence constraints;
- exact assignment-created and assignment-ended payload fixtures;
- assignment replay that fails before mutation on malformed facts, detects
  missing/differing rows, repairs from facts, and leaves journal/access results
  unchanged;
- a clean production-profile gate of 296 unit/contract tests and 38
  integration tests with zero failures or errors;
- a production-profile JAR build;
- candidate Liquibase migration of the isolated production clone;
- first and idempotent assignment bootstrap runs producing 105,153 facts and
  105,153 grants, with 263,423 effective access tuples and zero baseline/event
  differences;
- production-clone assignment replay reporting zero missing, unexpected, or
  differing projection rows.

This proves the candidate is ready for installed-client compatibility smoke.
It does not yet prove every external administrator route or an installed
mobile against the final candidate, so it is not yet a production-release
candidate.

## Ordered Cutover

1. **Prepare one staging clone.** Stream a fresh compressed dump directly from
   the current production database without exposing credentials; replace only
   `datarun_staging`; apply candidate migrations, assignment bootstrap, exact
   comparison, and candidate startup.
2. **Prove released-client compatibility.** Smoke installed mobile
   `6.0.3+54`; inventory and smoke the administrator operations that are
   actually used.
3. **Integrate the first transition candidate.** Merge to `develop` only after
   the staging gate passes. Merging is not deployment.
4. **Deploy assignment authority separately.** In a later approved release
   window, stop assignment writers, apply migrations, run exact assignment
   bootstrap, then start the candidate with capture shadow disabled.
5. **Activate capture shadow separately.** Bootstrap and validate capture
   facts, enable shadow append, observe exact equivalence, and retain
   `data_submission` as authority.
6. **Eliminate capture-write bypasses.** Route every supported
   unversioned/custom/administrator mutation through the capture command, or
   remove it after caller evidence proves it unused. No route may write
   `data_submission` directly once capture events become authority.
7. **Cut capture authority.** Make immutable capture append the accepted
   command and produce `data_submission` plus `outbox` as compatibility
   projections in the same transaction.
8. **Retire baseline write owners.** Invert or remove baseline assignment and
   capture mutation owners only after event replay, compatibility, rollback,
   and one production-style cycle pass.

An event-native mobile or administrator endpoint is not part of the current
candidate. Design it only after server event authority is stable; until then,
the released endpoints are deliberate compatibility adapters, not a second
architecture.

## Next Bounded Slice

The event-contract and assignment-replay closure is complete. The active
implementation is **transition staging preparation**:

- refresh a named staging clone directly from current production;
- use a staging-only non-production database role;
- apply candidate migrations;
- run assignment bootstrap and exact comparison;
- start the candidate API with capture shadow disabled;
- provide one concise smoke command for health, build identity, login, and
  released configuration reads.

It changes no product behavior and closes the environment ambiguity that
currently blocks reliable mobile and administrator compatibility evidence.
The current branch justifies spending time on that smoke. It does not justify
production publication until the smoke and actually used administrator-route
inventory pass.
