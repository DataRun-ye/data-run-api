# Production Modernization Strategy

Role: durable execution strategy for reducing legacy server code and improving
production ownership without a rewrite

Status: LIVING

This document defines how the repository moves from its current production
state toward a smaller, safer, maintainable platform. It is not runtime, API,
schema, or product-policy authority. Current mobile/server contracts remain in
`active-production-interface.md`; accepted immediate work remains in
`current-work.md`.

## Intended Outcome

- No source-dead code remains in the reasoned production surface.
- Every active behavior has one authoritative owner and one intentional write
  path.
- Duplicate services, registrations, and state transitions are removed after
  their consumers move to the selected owner.
- Authentication, authority, assignment scope, synchronization, templates,
  submissions, and projections have explicit boundaries and contracts.
- Physical schema matches the surviving model through safe production
  migrations rather than preserving abandoned structures indefinitely.
- Mobile and server evolve through explicit compatible contracts.
- New features improve or use the relevant boundary instead of extending
  known debt.

This is an incremental modernization, not a ground-up redesign. A feature does
not need to wait for the entire repository to become clean. Its owning
boundary must be understood and brought to an acceptable state first.

## Evidence Before Risk

Names, comments, directories, `@Deprecated`, generated code, and old tests are
candidate evidence only.

Use this evidence ladder:

1. `E0 - CLUE`: name, comment, annotation, or zero obvious callers.
2. `E1 - STATIC`: imports, call graph, registrations, inherited methods,
   configuration, reflection, and data types have been traced.
3. `E2 - RUNTIME ROOT`: HTTP route, schedule, runner, listener, filter,
   startup hook, JPA callback, external process, or released mobile request is
   identified.
4. `E3 - CHARACTERIZED`: focused tests or a temporary comment-out/replacement
   test prove current behavior.
5. `E4 - INTEGRATED`: startup and relevant flows pass against an isolated
   production clone or staging environment.
6. `E5 - DEPLOYED`: image identity, production logs, database state, or field
   behavior confirms the contract.

Risk is raised only by evidence:

- `R0 - SOURCE ONLY`: no runtime root, inbound consumer, reflection, stored
  data, schema, or operator role. Remove directly after a comment-out test.
- `R1 - SUPPORTING`: reachable helper, read path, or duplicate with no unique
  external effect. Characterize and consolidate.
- `R2 - ACTIVE BOUNDARY`: API, authorization decision, synchronization,
  transaction, persistence, or scheduled behavior. Move consumers before
  removing an owner.
- `R3 - PERSISTED OR CROSS-CLIENT`: schema, stored JSON, authentication,
  deployed mobile payload, or offline migration. Use compatible staged
  migration.
- `R4 - PRODUCT POLICY`: authority, synced edit/delete, retention, and other
  user-visible policy. Require an explicit product decision before changing
  behavior.

## Dispositions

Every examined item ends in exactly one disposition:

- `KEEP-CORE`: required by an active production capability.
- `KEEP-PROJECTION`: active export, ETL, outbox, event, or analytics projection.
- `KEEP-OPERATIONAL`: intentional administration or maintenance behavior.
- `KEEP-COMPATIBILITY`: retained for a named deployed client or stored format.
- `REMOVE-SOURCE`: proven source-dead and safe to delete now.
- `CONSOLIDATE`: active or supporting behavior moves to one selected owner.
- `SEPARATE-API`: endpoint or task retirement needs its own compatibility or
  operator decision.
- `SEPARATE-SCHEMA`: physical persistence changes through its own Liquibase
  slice after source ownership is settled.
- `UNKNOWN`: one precise evidence gap remains, with the next check named.

Do not leave speculative deprecations in source as a substitute for one of
these outcomes.

## Modernization Sequence

### 1. Reduce The Source Surface

Scan the repository systematically:

1. Enumerate controllers and inherited routes.
2. Enumerate schedules, runners, listeners, filters, startup hooks, and
   callbacks.
3. Enumerate Spring registrations, JPA entities, Jackson contracts, Liquibase
   ownership, and configuration-driven loading.
4. Cross-check released mobile requests and known manual operations.
5. Trace each root to its services, repositories, data, and external effects.
6. Remove `R0` leaves and orphan graphs in bounded commits.
7. Record registered endpoints/tasks separately instead of calling them dead.

Source removal does not drop tables or change an API. A dormant table and an
unused endpoint are different decisions.

### 2. Establish One Owner Per Boundary

For each boundary, build a compact owner map:

- entrypoints;
- decisions it owns;
- canonical state and tables;
- writes and transaction boundary;
- reads and projections;
- external/mobile contract;
- duplicate or misplaced behavior;
- checks that prove equivalence.

Select the owner from production behavior and responsibility, not from class or
package names. Add an interface only where it creates a real boundary: an
external system, a replacement seam, multiple legitimate implementations, or
a useful test seam. Do not create one interface per service as cleanup theater.

Use branch-by-abstraction when replacing active behavior:

1. characterize the current owner;
2. introduce the smallest stable seam;
3. route one consumer at a time to the selected owner;
4. compare results when risk warrants shadow reads or dual calculation;
5. stop old writes;
6. remove the old implementation and temporary seam.

Compatibility adapters must have a named consumer and retirement condition.
Reachability is not a permanent exemption from cleanup. A mixed or misplaced
active owner closes only when it is consolidated, isolated behind a named
temporary compatibility boundary, or assigned a production-safe cutover with
an executable exit gate.

### 3. Clean The High-Risk Boundaries

Work in this order because later work depends on the earlier contracts.

#### Assignment, Access, And Configuration Sync

Target separation:

- authentication establishes identity and session validity;
- authority policy decides whether an actor may perform an operation;
- assignment/form access produces the actor's current work scope;
- configuration sync transports that accessible state to the mobile client;
- offline mobile policy decides how cached authorized work remains usable.

The pass must identify and converge generic ACL checks, user/team/activity
filters, assignment projections, form access, dependency injection, and sync
registration. It must not redesign authority while merely deleting dead code.

#### Form Templates And Versions

Target separation:

- template authoring validates and creates immutable versions;
- released reads expose accessible templates and versions;
- submission validation resolves the pinned version;
- element metadata used by ETL/export is a downstream projection;
- persisted form JSON remains a data contract until migration evidence says
  otherwise.

Data-element, generated-element, old reference, rule, and value-type surfaces
are classified by stored-template evidence before removal.

#### Submissions

Target separation:

- the versioned upload application service owns request mapping, access checks,
  repeat/reference resolution, validation, whole-JSON persistence, result
  classification, and the current outbox write transaction;
- controllers translate HTTP only;
- repositories persist, rather than decide product policy;
- ETL, tall tables, events, and analytics remain downstream projections and
  never become submission authority;
- pull, synced edit/delete, retention, and conflict behavior remain explicit
  product-policy work.

#### Projection And Export

The current outbox, ETL, events, tall tables, exports, analytics, and MVs may
remain active even though core features do not read them. Classify each as a
projection producer, consumer, operator endpoint, or dead alternative.
Consolidate duplicate pipelines only after proving which one receives current
submission writes and which outputs are still used.

### 4. Align Persistence

After source ownership is settled and before schema contraction, run one
bounded Liquibase reconciliation pass. Prove both clean-database replay and
upgrade from an isolated production clone; correct include ordering; and
classify duplicate or abandoned analytics, ETL, option, and projection
changelogs. Do not restore dead source merely to satisfy an obsolete
changelog, and do not mix table drops into replay repair.

Use expand-and-contract for `R3` changes:

1. add compatible schema or fields;
2. deploy code that can read old and new forms;
3. backfill idempotently with observable counts;
4. switch the authoritative read/write path;
5. verify mobile upgrades, retries, and old records;
6. stop old writes;
7. remove old source;
8. drop obsolete schema in a later release when rollback no longer needs it.

Every Liquibase slice starts from the observed production changelog and an
isolated production clone. It includes preservation assertions, an intentional
rollback position, and no unrelated source cleanup.

### 5. Evolve Mobile And Server Together

For cross-repository contracts:

- define the request, response, identity, retry, and compatibility behavior
  before implementation;
- add server support before exposing client behavior when old clients could
  misinterpret it;
- use additive fields or an explicit capability/version gate only when a real
  compatibility risk exists;
- characterize server DTOs and mobile parsing/upload independently;
- migrate mobile offline data before relying on new fields;
- smoke an installed production upgrade against staging or an isolated clone;
- remove compatibility paths only when their deployed consumer is retired.

Server truth remains in this repository, mobile truth in the mobile
repository, and shared mutable sequence in one linked issue only when needed.

## Slice Protocol

Each bounded slice closes this cycle:

1. **Discover:** one ownership or removal question, with runtime and data roots.
2. **Plan:** selected owner, behavioral contract, risk class, and checks.
3. **Change:** smallest complete move, deletion, or migration.
4. **Verify:** focused characterization, full build gate, and proportional
   integration/device smoke.
5. **Close:** remove superseded source, update the owning map, and name any
   genuinely separate follow-up.

The existing test suite is sparse and mostly recent. `./mvnw test` explicitly
excludes `*IT*` and `*IntTest*`; a passing count proves only the unit and
contract behavior currently covered. It is not a coverage claim. Run focused
integration tests or `verify` when the slice affects Spring wiring, JPA,
Liquibase, security, or HTTP behavior, and add characterization where the
active contract is otherwise unprotected.

Adjacent debt cannot silently expand the slice. Debt required by the change is
either cleaned in a prerequisite slice or explicitly isolated with a reason.
Do not land a second owner merely because the first owner is difficult.

## Low-Ceremony Git And Documentation

- Use one working branch for a coherent scan/pass.
- Use small commits as rollback and comparison checkpoints.
- A normal PR against `develop` is only the final diff review surface; no draft
  lifecycle, issue hierarchy, or waiting ceremony is required for solo work.
- Keep the worktree clean. Candidate annotations or partial deletions are
  either committed in their validated slice or discarded.
- `current-work.md` names only the current pass and immediate next pass.
- This strategy owns the method and coverage order.
- `active-production-interface.md` owns released runtime contracts.
- Focused boundary documents own accepted technical findings.
- `completed-work.md` records compact outcomes, not old plans.

## Coverage Order

| Pass | Scope | Current state |
| --- | --- | --- |
| 0 | Executable and data-contract roots | IN PROGRESS |
| 1 | Source-dead leaves and orphan graphs | IN PROGRESS |
| 2 | Assignment, synchronization, and access | PENDING |
| 3 | Form templates, versions, elements, and rules | PENDING |
| 4 | Submission upload, persistence, validation, and compatibility | PENDING |
| 5 | Outbox, ETL, events, tall tables, exports, analytics, and MVs | PENDING |
| 6 | Remaining operational, generic-resource, audit, cache, and utility surfaces | PENDING |
| 7 | Source-owner consolidation within each surviving boundary | PENDING |
| 8 | Liquibase replay reconciliation after source ownership settles | PENDING |
| 9 | Schema contraction after source and compatibility closure | PENDING |

## Established Evidence

- `domainmapping/`, `importer/`, and `importprocessor/` had no external runtime
  or data-contract owner. Their temporary removal compiled and passed the 75
  currently discovered non-integration tests; the real combined tree passed
  the same limited gate. Static root and inbound-reference evidence is the
  primary removal proof.
- `acl/` is mixed: active generic-resource checks coexist with dormant
  bootstrap and permission alternatives. It requires the access-boundary pass,
  not package-wide deprecation.
- `etl/` is projection-active and is not a source of assignment, form, or
  submission truth.
- `FormTemplateResource` and `TemplateVersionResource` own released mobile
  reads.
- `DataSubmissionResource` owns active versioned upload; its admin delete and
  generic inherited routes are separate API/product decisions.
- Assignment path maintenance is currently registered as scheduled and
  operational behavior. Necessity may be reviewed, but it is not source-dead.
- `ValueType`, `RuleAction`, and `FormDataElementConf` are serialized template
  contracts and require stored-data evidence before contraction.

## Completion Conditions

The modernization is ready to shift its emphasis toward feature expansion when:

- every package has been scanned or belongs to an explicitly active boundary;
- no known source-dead or registered-dormant alternative remains;
- each high-risk boundary has one named authoritative owner and write path;
- projections are not used as core business authority;
- duplicate services have a named convergence slice rather than indefinite
  coexistence;
- schema residue has been removed or has a concrete compatibility reason and
  retirement condition;
- active mobile/server contracts have characterization and upgrade checks;
- new feature slices can identify their owner without rescanning the whole
  repository.

This does not require perfection. It requires that remaining complexity is
intentional, owned, testable, and no longer misleading.
