# Completed Work

Updated: 2026-11-19

Purpose: compact historical outcomes only. This file is not current runtime or
deployment authority.

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
  JWT tests were aligned with the active V1 endpoint.
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
  scope; `/myDetails` has an explicit V1 compatibility adapter, organization
  units follow direct assignments only, and the remaining access fallbacks
  have named retirement paths.
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

- `43a3d52d`: `deploy/production/compose.yml` reconciled with the topology
  deployed since the 2026-08-29 Google Cloud to Proxmox cutover. The API is the
  only service it owns; PostgreSQL 16 is a managed VM service on
  `datarun-prod-db.lab` reached over TLS, and nginx/TLS is external. This also
  removed the one-shot `letsencrypt` service that exited on every
  `docker compose up -d`, and promoted the database VM spec out of the
  gitignored `.review` directory. Verified against the live container.
- The production `pg_idkit` question is closed by that cutover: production runs
  stock PostgreSQL 16, with `idkit_ulid_generate()` supplied by
  `deploy/production/restore-prerequisites.sql` for inactive `supply_temp`
  defaults only. `public.generate_uid()` remains application-owned.
- `3c797950`, `09efc954`, `44b5c52a`, `c29aa11c`, `f9eb2e1f`, `b895e270`: the
  staging rehearsal kit moved onto `develop` and was repaired for the current
  environment. The refresh now dumps from the Proxmox production database
  through a forced-command key instead of a retired containerized cloud
  database; the restore list skips the `idkit_ulid_generate()` function that
  production now carries; the assignment-shadow phases are opt-in; and user
  sanitization no longer aborts under `set -u` on repeat runs. Staging was then
  baselined to the exact production artifact and smoked.
- `650a198f`, `a7c7e008`, `b1f403e9`, `1f2f9633`, `559b3cb0`, `f0f04a1e`,
  `9c69b0eb`: bounded fixes taken from `architecture/initial-event-transition`
  without any event-transition code. `/api/v1/admin/**` now requires ADMIN;
  identical submission retries no longer rewrite `formData`; deleted
  assignments no longer grant org-unit access; entity audit is decoupled from
  web conversion; `UserDTO` sits at its declared package path; the logback
  `conversionRule` uses the supported attribute; and refresh-token rotation has
  one owner in `TokenService` returning `401`. Released mobile `6.0.3` is
  unaffected: `AuthApi` uses an interceptor-free Dio and
  `isCredentialRejection()` already treated 401 and 403 identically.
- `a9e988bc`: the five generated user and account integration tests were
  removed. They held every failing test in the suite (48 across 53 runs) and
  cost over five minutes. The remaining suite is green: 127 unit and 41
  integration tests.
- `v6.5.0` / `a9609da0`, deployed 2026-11-19 (Yemen time): every form version
  synced, per-submission saves, Arabic-Indic digit logins, client app version
  in last-seen, kept form versions and publish-time cache eviction, and go-live
  rules with held uploads (#42). Promoted by digest from the rehearsed staging
  candidate; the ITN 2026 household lists (162,610 entries) were loaded in
  production the same day.

Use commits, tests, focused context, and deployed evidence to determine current
behavior.
