# Completed Work

Updated: 2026-07-28

Purpose: compact historical outcomes only. This file is not current runtime or
deployment authority.

- `252c38e0`: production-profile verification now runs the real unit and
  integration gate; 113 unit and 37 current-contract integration tests pass,
  and release artifact version/commit identity is verified.
- `d1b87dc9`: additive assignment-event persistence landed without changing
  active reads or writes; PostgreSQL tests prove immutability, identity,
  concurrency, rollback, and effective-access projection behavior.
- `5d0f5688`: `/api/v1/admin/**` now requires `ROLE_ADMIN`; integration tests
  prove ordinary-user denial and administrator access.
- `3559cfe9`: organization-unit access now excludes soft-deleted assignments
  in both normal and include-disabled reads; focused and full tests pass.
- `e8bee6b4`: `main` was aligned to the reconstructed deployed server baseline.
- `09b9b27d`: bounded Reference catalog, read boundary, and old-client gate
  landed on `develop`.
- `8fa1d7ac`: bounded Reference upload extraction and resolution landed on
  `develop`.
- `8695335a`: the old analytics-query/jOOQ, Party, and duplicate outbox source
  surfaces were removed without dropping inert legacy tables or active
  ETL/ledger storage.
- `0d83c580`: inactive assignment-member source was removed without dropping
  production tables.
- `v6.4.0`: the immutable server image, additive Reference migration, external
  JWT-key ownership, and existing login/configuration behavior were verified
  in production without activating the new Reference workflow.
- `caee970e`: inactive Mongo persistence, endpoints, dependencies,
  configuration, and Compose ownership were removed from `develop`; ordinary
  configuration reads and submission upload passed against the production
  clone with Mongo stopped.
- `90281457`: the test runtime was aligned with the local Docker API, the
  app-owned `generate_uid()` function entered Liquibase ownership, and focused
  JWT tests were aligned with the active `/api/v1` endpoint.
- The immutable `6.4.1-40f176dde05b` candidate passed the release gate and
  isolated staging smoke against the production clone with Mongo stopped:
  health, identity, login, configuration reads, and ordinary submission upload.
- `v6.4.1` is deployed in production. Public health/build identity and
  authenticated mobile configuration sync passed; the app-owned UID migration
  ran successfully. Empty Mongo persistence and its production Compose
  service, volumes, and images were removed after backup and verification.
- The assignment source-dead pass removed disabled listeners and migrations,
  unreferenced services, superseded repository extensions, and zero-caller
  queries while preserving the released assignment routes and schema.
- The form-template source-dead pass removed commented models/repositories, a
  disabled hardcoded migration runner, unused service/query alternatives, and
  false controller dependencies while preserving released reads, operational
  version authoring, submission template lookup, and schema.
- The first submission source-dead pass removed the disabled history/event
  chain and zero-caller form-data/reference helpers while preserving the
  versioned upload, whole-JSON persistence, and current outbox write.
- The abandoned `outbox_event` source, disabled workers, unused executor
  configuration, and misleading admin endpoint were removed. The active
  `outbox` writer/orchestrator and both physical tables were unchanged.
- The submission bulk soft-delete collection bug was recovered from the
  archived branch as a one-line correction with a mixed update/delete outbox
  regression test; no delete policy or endpoint was activated.
- The submission source pass removed unregistered migration-error listeners
  and zero-caller repository queries while retaining the active repeat-ID
  generator, compatibility routes, and physical tables.
- Authentication remains owned by `User.authorities`; the unused
  role/privilege source model was removed without changing its physical tables.
- The unused Spring ACL engine and dependency were removed after preserving
  its two live coarse rules in `ResourceApiAuthorization`. Entity and form
  scope owners were unchanged; ACL tables remain for the schema pass.
- Assignment-form projection, Reference reads, and submission validation now
  share one team-and-assignment-scoped authorization owner. The unused generic
  reverse mapper was removed and the projection exposes external UIDs.
- Authentication now builds one uncached, request-current team/activity/form
  scope; `/myDetails` has an explicit `/api/v1` compatibility adapter,
  organization units follow direct assignments only, and the remaining access
  fallbacks have named retirement paths.
- `c9f614d9`: the released versioned submission route now has one general
  upload owner that resolves assignment and pinned template once before
  canonical authorization, repeat handling, Reference resolution, whole-JSON
  persistence, and the current outbox transaction.
- `4ad077f2`: submission persistence no longer accepts a security principal;
  authorization completes before the canonical persistence boundary.
- Template publication now has one immutable-version owner and one active
  `canonical_element` projection. The duplicate `template_element` writer,
  entity/cache ownership, and persistence-only fields were removed without a
  schema change; nested-repeat ancestry and option-set projection are covered.
- Assignment-shadow bootstrap closed through `d3476ab7`. The explicit non-web
  command created 105,153 deterministic assignment streams on the isolated
  production clone; 263,423 baseline and shadow capture-authority tuples
  matched with zero differences. A second run created nothing and reproduced
  the same result. Active reads, writes, authorization, and production were
  unchanged.
- `c83ec9d7`: assignment, team, and activity authority-changing writes now use
  one transactional command owner. The full 156-unit/contract and
  37-integration release gate passed; real HTTP mutation scenarios on the
  isolated production clone preserved exact baseline/shadow equality through
  scope/form, membership, permission, status, delete/restore, and
  non-authority changes. The clone was restored and production was untouched.
- `0b1861bd`: all five released assignment-scope consumers now run one batched,
  fail-open event authorization shadow without changing responses or upload
  authority. The 174-unit/contract and 37-integration release gate passed;
  clone-only HTTP scenarios covered active, revoked, disabled, retired,
  accepted late-upload, and restored states with exact tuple equality. The
  clone was restored to 263,423 exact tuples and production was untouched.
- `61f8da6c`: event-backed assignment grants now authorize the released
  versioned submission upload. Baseline logic remains only for exact
  retired-assignment compatibility and released denial-code mapping. The
  188-unit/contract and 38-integration release gate passed; clone-only HTTP
  scenarios proved ordinary and Reference persistence, repeat metadata,
  outbox writes, revoked-state rollback, accepted late upload, restoration,
  and exact tuple equality. The clone was restored to 263,423 tuples and
  production was untouched.
- `d1b02b7b`: one highest-generation event reader and one released work-read
  owner now authorize the V1 assignment, assignment-form, organization-unit,
  and Reference graph. Custom/generic routes retain baseline compatibility,
  and the two empty-capture assignments are isolated as display-only. The
  209-unit/contract and 38-integration release gate passed. Clone HTTP reads
  matched all 5,002 assignments/forms and 5,086 organization units for the
  largest actor; a disposable Reference fixture matched across V1/custom
  routes. The untouched dump was restored, migrations reapplied, and bootstrap
  reruns reproduced 263,423 exact tuples with a zero-write second pass.

Use commits, tests, focused context, and deployed evidence to determine current
behavior.
