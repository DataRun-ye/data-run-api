# Production Boundaries

Role: current server production and working boundary

Status: LIVING

## Repository State

- `main` is the production branch; `develop` is integration.
- Merging to either branch does not deploy the server.
- Record exact deployed commits and image digests in the corresponding release,
  not in this living document.

Commit dates and branch names alone are not production evidence. Before a
deployment, identify the exact source commit, built image, database migration
state, and configuration being promoted.

## Authority Order

When evidence conflicts:

1. deployed behavior and production data;
2. reachable runtime paths and persistence/network effects;
3. focused characterization and integration tests;
4. this current boundary document;
5. names, comments, generated-only code, old tests, and historical docs.

## Runtime And Ownership

- This repository owns server API behavior, authentication and access
  enforcement, server persistence, and Liquibase migrations.
- The mobile repository owns offline storage, mobile sync orchestration, form
  UI/state, and client payload construction.
- HTTP payload authority belongs to server DTOs, request handling, and
  contract tests. Mobile assumptions must be characterized in mobile tests
  rather than copied here.
- PostgreSQL/JPA/Liquibase are active production boundaries. Mongo persistence
  and runtime ownership were removed after all eight production collections
  were confirmed empty and the candidate passed against the production clone
  with Mongo stopped. Production no longer contains the Mongo service, volumes,
  or image.
- The old jOOQ/analytics-query, Party, and assignment-member implementations
  were removed from `develop`. The restored 2026-07-25 production clone
  confirmed that the legacy Party/assignment tables left by those attempts
  exist but are empty. They are inert schema residue, not an active surface or
  current task; remove them only in a separately approved Liquibase slice.
- This classification does not apply to the separate analytics ETL/ledger
  tables still referenced by active source.

## Migration Rules

- Never edit or reorder a changeset already applied to production.
- Add a new changeset for every schema transition and include an intentional
  rollback when safe.
- `public.generate_uid()` is an application-owned 11-character UID function,
  now defined by Liquibase. It is not supplied by PostgreSQL or `pg_idkit`.
- Before deployment, inspect production `databasechangelog`, restore or use
  the production clone, and prove the migration from that exact state.
- Treat database access through `docker exec` and database access through
  Maven/Liquibase over TCP as separate credential paths. An existing
  PostgreSQL volume can retain role passwords that do not match the
  container's current `POSTGRES_PASSWORD`; never infer usable Liquibase
  credentials from that environment variable.
- For migration proof, use an isolated restored clone and a disposable,
  clone-only database role. Generate its secret outside the repository, pass
  it through a temporary environment/settings file without printing it, and
  remove the role, secret, settings file, and disposable database after the
  evidence is captured. Never substitute production credentials.
- Source removal and table removal are separate slices. A dormant table is not
  permission to drop production data.
- Never point tests, local startup, or migration tooling at production
  credentials or production hosts.

## Validation

The Maven wrapper is authoritative for project checks:

```bash
./mvnw -Dtest=<FocusedTest> test
./mvnw test
./mvnw -Pprod clean verify
```

Run the smallest focused tests first. Use the full test/build gate before
deployment. If a check depends on local services or a cloned database, record
that environment explicitly; do not describe an unavailable check as passed.

## Deployment Gate

Use `release-boundary.md` for the executable build, image, deployment, and
rollback path. Feature deployment and feature activation remain separate.

Changes affecting both repositories use one GitHub parent issue for shared
sequence and compatibility state, with one repository-owned task per repo.
Do not duplicate that mutable checklist in both codebases.
