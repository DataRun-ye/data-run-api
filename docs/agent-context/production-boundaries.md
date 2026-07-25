# Production Boundaries

Role: current server production and working boundary

Status: LIVING

Validated: 2026-07-25

## Repository Baselines

- Production `main`: `e8bee6b4`, reconstructed from the deployed server image
  and validated against its packaged Liquibase history.
- Current `develop`: `0d83c580`.
- `develop` contains the bounded Reference implementation and source-only dead
  surface cleanup. Those commits are not yet production-deployed.
- Shared Reference activation status:
  [DataRun API #34](https://github.com/DataRun-ye/data-run-api/issues/34).

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
- PostgreSQL/JPA/Liquibase are active production boundaries. Mongo remains a
  separate legacy surface to investigate and retire in its own bounded work;
  do not remove it as incidental cleanup.
- Source-dead analytics, pivot, jOOQ, Party, and assignment-member
  implementations were removed from `develop`. Their historical physical
  tables remain until a separately approved Liquibase removal proves
  production counts, dependencies, upgrade behavior, and rollback.

## Migration Rules

- Never edit or reorder a changeset already applied to production.
- Add a new changeset for every schema transition and include an intentional
  rollback when safe.
- Before deployment, inspect production `databasechangelog`, restore or use
  the production clone, and prove the migration from that exact state.
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

No deployment is implied by merging to `develop`.

Before changing production:

1. review the exact diff from the deployed commit;
2. validate migrations against the production clone;
3. prove old-client compatibility and existing payload behavior;
4. build from a clean, identifiable commit;
5. separate additive deployment from feature/configuration activation;
6. record the deployed revision and rollback point;
7. run focused authenticated smoke checks after deployment.

Changes affecting both repositories use one GitHub parent issue for shared
sequence and compatibility state, with one repository-owned task per repo.
Do not duplicate that mutable checklist in both codebases.
