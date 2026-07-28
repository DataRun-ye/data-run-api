# Current Work

Updated: 2026-07-28

Status: ACCEPTED FOR IMPLEMENTATION

## Assignment Authority Command Ownership

### Outcome

Route every registered mutation that can change assignment-derived capture
authority through one transactional command owner. The command writes the
current assignment/team/activity tables and the assignment event shadow in the
same transaction.

Current tables and DTOs remain the read and wire authority in this slice. No
mobile, configuration-sync, submission-upload, or authorization read changes
are included.

### Authority Boundary

Before this slice, authority-changing writes are split across generic
assignment, team, and activity services. Team membership, form permissions,
disabled state, assignment scope/forms, and activity disabled state can all
change the same effective grant without one owner.

After this slice:

- `AssignmentAuthorityCommandService` is the only mutation facade for
  assignments, teams, and activities;
- a small baseline mutation adapter owns JPA relation resolution and current
  row persistence;
- a canonical snapshot component derives assignment/actor capture intents;
- one grant-lifecycle component owns identity links, role resolution, journal
  append, generations, and grant projection updates;
- existing services remain route/read adapters and do not write their
  repositories directly.

Do not create separate command owners for assignments, memberships, or status
changes. They are inputs to one authority projection.

User-account activation is authentication eligibility, not assignment
authority. A direct team membership may exist before an account can log in and
must not generate a new assignment lifecycle when the account is activated.
`UserService` therefore remains the owner of account activation and user data;
the assignment snapshot does not filter direct members by `app_user.activated`.
The isolated production clone contains no otherwise-eligible assignment rows
for inactive direct members, so this correction does not alter its observed
capture-authority tuple count.

### Registered Write Roots

The owner must cover both `/api/custom` and `/api/v1` aliases for:

- assignment `POST`, `POST /return`, `POST /bulk`, `PUT`, and soft `DELETE`;
- team `POST`, `POST /return`, `POST /bulk`, `PUT`, partial `PATCH`, and hard
  `DELETE`;
- activity `POST`, `POST /return`, `POST /bulk`, `PUT`, and hard `DELETE`.

The current team PATCH route is the duplicated path
`/api/{custom,v1}/teams/teams/{uid}`. Add the canonical
`/api/{custom,v1}/teams/{uid}` PATCH mapping to the same secured handler and
retain the old path only as a compatibility alias. Its retirement criterion is
one deployed release with no access-log or operator-tool use of the old path.

The mobile application does not call these writes. They are operational admin
surfaces, but they are registered and therefore cannot bypass the owner.

Keep `/bulk` compatibility: the resource loops over entities and each item is
its own command transaction. Do not turn a partial bulk success into one large
transaction in this slice.

### Canonical Capture Intent

For each affected baseline assignment, derive zero or more immutable intent
values keyed by:

```text
(baseline assignment UID, direct team-user UID)
```

An intent exists only when:

- assignment activity, assignment team, and team activity are enabled;
- the assignment is not soft-deleted;
- the actor is a direct team member, independently of account activation;
- organization-unit, assignment, activity, team, and user UIDs are valid;
- the sorted distinct intersection of `assignment.forms` and same-team
  `ADD_SUBMISSIONS`/`EDIT_SUBMISSIONS` permissions is non-empty.

The value contains only:

```text
activity UID
organization-unit UID
canonical capture-form UID list
```

Managed teams, user groups, administrator bypass, view/delete-only
permissions, names, codes, and compatibility action flags do not enter the
intent.

Extract one pure canonical capture-form resolver and use it from both the
runtime command and bootstrap parsing. Do not leave two implementations of
the role intersection rule.

Match the bootstrap validation order exactly: every source row requires valid
assignment/activity/team identity and parseable assignment-form and team-
permission JSON; a direct actor row also requires a valid user identity.
Disabled, no-actor, and empty-form rows then produce no intent. Only an
otherwise eligible row requires a valid non-null organization-unit scope.
Invalid required data aborts the command.

### Command Algorithm

Every command performs these steps in one transaction:

1. Acquire one transaction-scoped PostgreSQL advisory lock shared by all
   assignment-authority commands. These writes are low-volume admin work;
   serialization is preferred to generation races.
2. Identify all affected baseline assignment UIDs and snapshot their canonical
   intents before mutation.
3. Require the immutable bootstrap-completed checkpoint described below, then
   verify that every authority-bearing pre-state has one matching active shadow
   generation. A missing checkpoint or missing/contrary pre-state aborts before
   the baseline mutation.
4. Apply the baseline mutation, including relation resolution and current
   validation, then flush it.
5. Snapshot the affected intents after mutation and diff by assignment UID and
   actor UID.
6. Apply the lifecycle changes below.
7. Compare affected baseline capture tuples with affected shadow tuples. Any
   difference rolls back the baseline and shadow changes together.

Diff behavior:

- absent -> present: create the next generation and active grant;
- present -> absent: end the current generation;
- present -> changed role/activity/scope: end the current generation, then
  create the successor generation;
- unchanged: append nothing, including stable-UID update retries and
  non-authority field updates. A create request without a UID is not an
  idempotent retry; preserve the current behavior that assigns a new baseline
  identity.

Equivalent authority from another assignment remains active because streams
are reconciled independently and only the access projection deduplicates them.

### Event And Identity Contract

Reuse the existing journal, aliases, role definitions, assignment identity
links, and grant projection. No Liquibase change is allowed in this slice.

Extend the bootstrap transaction to append one deterministic checkpoint only
after exact tuple comparison succeeds:

```text
event_id: UUID.nameUUIDFromBytes("datarun-baseline/assignment-shadow/bootstrap-completed/v1")
event_type: transition_checkpoint
shape_ref: assignment_shadow_bootstrap_completed/v1
activity_ref: null
subject_type: transition
subject_id: UUID.nameUUIDFromBytes("datarun-baseline/assignment-shadow/v1")
actor_id: system:migration/datarun-baseline-assignment-bootstrap
payload: {}
```

Before live command activation, an exact bootstrap rerun verifies and reuses
that event. A conflicting event is a hard failure. After live generations
exist, do not rerun the generation-`0` bootstrap; use generation-aware
baseline-versus-current-projection comparison instead. The command owner checks
the checkpoint while holding the same transaction advisory lock, so a new
authority-bearing row can be distinguished from a deployment that has never
completed bootstrap.

Update bootstrap actor selection to include every direct team member rather
than only activated accounts, and keep the comparison on that same grant
definition. This is an assignment-authority correction, not an authentication
change.

Extract and reuse the deterministic baseline identity functions already used
by bootstrap:

```text
datarun-baseline/actor/{userUid}
datarun-baseline/org-unit/{orgUnitUid}
datarun-baseline/assignment/{assignmentUid}/actor/{userUid}/generation/{n}
```

The first generation is always `0`, whether first observed by bootstrap or
created later by a live command. A restore, re-addition, or changed role/scope
uses `max(existing generation) + 1`. Concurrent commands cannot create
duplicate generations.

Live lifecycle facts use:

```text
event_type: assignment_changed
subject_type: assignment
subject_id: assignment generation UUID
activity_ref: baseline activity UID
actor_id: UUID alias of the authenticated command actor, serialized as text
recorded_at: server Clock instant
```

Start fact:

```text
shape_ref: assignment_created/v1
payload: { role, org_unit_id }
```

End fact:

```text
shape_ref: assignment_ended/v1
payload: {}
```

The immutable identity link already owns target actor and baseline assignment
UID. The envelope owns activity and command actor. Do not duplicate team,
names, codes, target actor, baseline UID, or timestamps in payloads.

Ending updates the current grant row to `ENDED` with the end event as its
source while retaining its role and scope. Starting inserts a new identity and
`ACTIVE` grant. Event IDs are new UUIDs; idempotency comes from transactional
state comparison, not deterministic reuse of a request with no command ID.

When an activity changes, the end fact retains the old generation's activity
UID and the start fact uses the successor generation's new activity UID.

The command actor comes from the authenticated server context. Request-body
actor or audit fields never authorize or author lifecycle facts.

### Baseline Mutation Compatibility

- Unify assignment create and update relation resolution. Both must resolve
  team, activity, organization unit, and parent and run the existing Reference
  scope guard before persistence; create must no longer bypass this path.
- Preserve assignment soft-delete and restore behavior, including
  `deletedAt` handling.
- Preserve full team update and partial-patch field semantics. Centralize user
  resolution so existing ID/UID/login representations are accepted, and reject
  a supplied user that cannot be resolved instead of silently dropping it.
- Keep user-account activation outside this command owner. Team membership,
  not the account's current login eligibility, starts or ends a grant.
- Preserve team managed-team compatibility data, but it does not affect the
  authority intent.
- Preserve activity and team hard-delete behavior. Referenced rows remain
  protected by current foreign keys; unreferenced deletion has no grant.
- Keep current response JSON and save summaries unchanged.

### Bypass And Security Closure

- Remove `GET /teams/migrate`, `runFormPermissionsMigration`, and
  `TeamFormPermissionsMigration`. The registered operation is a semantic no-op
  and direct `TeamRepository.saveAll` bypass; no replacement endpoint is
  required.
- Replace assignment path maintenance's whole-entity `saveAll` with targeted
  repository updates of derived `path` and hierarchy-level columns only. Keep
  the schedule and force/missing modes, but do not allow maintenance to write
  authority fields. Missing-path pagination must not skip rows as the result
  set shrinks.
- Require the same super-user manage decision used by inherited writes for
  team partial PATCH and the manual assignment path-maintenance endpoint.
  Ordinary `ROLE_USER` access is denied.
- No production code outside the selected command/baseline adapter may call
  assignment, team, or activity repository `save`, `saveAll`, or delete for
  these registered mutations. Add a focused structural check where practical.

### Tests

Focused route/unit/PostgreSQL tests must prove:

- every route alias and bulk/partial path delegates to the command owner;
- assignment create, delete, restore, role/form, team, activity, and scope
  changes produce the expected generations;
- direct membership add/remove and team permission changes reconcile every
  affected assignment;
- team/activity disable and re-enable end and recreate affected grants;
- empty-form/no-actor/disabled behavior matches bootstrap;
- overlapping assignments remain independent while effective access remains
  deduplicated;
- stable-UID update retries and non-authority updates emit no event;
- a missing or contrary pre-existing shadow blocks baseline mutation;
- baseline failure, journal failure, projection failure, or parity mismatch
  rolls back both sides;
- concurrent commands cannot create duplicate generations;
- bootstrap includes inactive direct members, records its completion checkpoint
  only after exact equality, and commands reject a missing checkpoint;
- a first live assignment/actor stream uses generation `0` and a later
  restoration uses the next generation;
- an activity change records the old activity on the end fact and the new
  activity on the start fact;
- bulk remains one transaction per item;
- both team PATCH aliases delegate to one handler, and team PATCH and manual
  maintenance reject an ordinary user;
- path maintenance changes only derived columns and does not skip rows;
- assignment/team/activity wire JSON remains compatible.

Run focused tests, then `scripts/release/verify.sh`. On an isolated clone whose
bootstrap has completed, exercise assignment, membership, permission,
team/activity status, delete/restore, and non-authority changes and prove full
tuple equality after each scenario. Roll back or recreate the disposable clone
after the scenario. Do not connect to production.

### Slice Gate

- **Authority before:** split baseline JPA services and repositories.
- **Authority after:** one assignment-authority command; baseline remains read
  authority and compatibility projection.
- **Schema/backfill:** existing additive schema and completed bootstrap only.
- **Shadow comparison:** affected-tuple parity inside every command plus clone
  scenarios.
- **Activation:** future deployment first stops the old API from accepting
  assignment/team/activity admin writes, then runs migration and the
  checkpoint-producing bootstrap. Start the new application, run the
  generation-aware exact comparison, and only then reopen those admin writes.
  This prevents an old writer from racing the bootstrap snapshot.
- **Rollback:** the old application ignores additive shadow data and continues
  serving released reads/mobile behavior. Assignment/team/activity admin writes
  are paused while the old writer is restored. If an old-writer authority
  change occurs, exact comparison blocks redeployment; recovery is an
  authority-scoped reconciliation that appends observed lifecycle facts before
  command writes reopen. Never restore the whole production database or
  overwrite shadow rows, because submissions may have continued during the
  rollback window.
- **Retirement:** baseline-only mutation paths and direct repository bypasses
  are removed in this slice; baseline read authority retires only in the later
  authorization cutover.

### Definition Of Done

- All registered authority-changing writes have one owner and one transaction.
- Baseline rows, lifecycle facts, and the grant projection cannot diverge on a
  successful command.
- The two known repository bypasses and the team PATCH security gap are gone.
- Current mobile/API reads and submission behavior are unchanged.
- Focused, full release, and isolated-clone gates pass.
- Production remains untouched.
