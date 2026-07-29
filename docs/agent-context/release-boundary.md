# Server Release Boundary

Role: durable server build, image, promotion, and rollback contract

Status: ACTIVE

## Stable Boundary

- `pom.xml` is the sole release-version owner.
- A release commit on `main` has tag `v<project.version>`.
- The application image is:
  `kaswarah/datarunapi:<version>-<12-character-commit>`.
- Never build or deploy the application as `latest`.
- `/management/info` must report the Maven version and release commit after
  deployment.
- `deploy/production/compose.yml` owns the sanitized production topology.
- Its project name is `datarun`; changing it can detach the existing named
  volumes.
- The production project directory is `/home/hamza/datarun`, where the
  relative init and nginx mounts live.
- Production secrets remain in the host environment. They are not committed or
  printed.

## Gate

```bash
scripts/release/verify.sh
scripts/release/build-image.sh --tar
```

The scripts require a clean tree. The gate runs tests, production build checks,
JAR version/commit verification, and a local image-tar build.

## Staging

`deploy/staging/compose.yml` runs only the candidate API against a dedicated
local clone database on an external Docker network. It has its own JWT secret,
Compose project, and port; its ignored `.env` must never contain production
credentials.

The Compose file does not create, restore, migrate, or bootstrap that
database. Keep these environment roles distinct:

- an archived production dump is immutable input;
- a disposable clone proves migrations and comparisons, then is reset;
- a staging clone is separately restored, migrated, bootstrapped, and retained
  for installed-client smoke;
- a development database uses small synthetic data for fast loops;
- production is never a local test target.

Restoring a disposable clone after a gate preserves the reusable source state;
it does not undo candidate code. Never use the archived dump or the disposable
comparison database itself as the staging database.

After the dedicated staging database is prepared:

```bash
cp deploy/staging/.env.example deploy/staging/.env
chmod 600 deploy/staging/.env
docker load -i target/jib-image.tar
docker compose --env-file deploy/staging/.env -f deploy/staging/compose.yml up -d
```

Before promotion, verify health and `/management/info`, then smoke login,
configuration reads, and one ordinary idempotent submission upload.

For the initial event-transition candidate, ordinary Liquibase startup is not
enough. Apply migrations and complete exact assignment bootstrap before the
candidate serves field-user assignment/configuration requests. Capture
bootstrap/replay is required only before capture shadow is enabled. The
authoritative sequence and compatibility gate are in
`initial-event-transition-boundary.md`.

The isolated assignment commands are:

```bash
scripts/transition/bootstrap-assignment-shadow.sh
scripts/transition/replay-assignment-projection.sh
```

Both require explicit opt-in and clone-only credentials. Replay additionally
requires assignment writers to be stopped and an explicit `validate` or
`repair` mode. `validate` must report `status=EXACT` before candidate startup;
`repair` is an operator recovery action, not routine application startup.

## Release

1. Merge the verified candidate to `main` and tag `v<version>`.
2. Run `scripts/release/build-image.sh --push`.
3. Record the pushed image tag/digest and the currently running rollback image
   in the GitHub release or deployment record.
4. Back up PostgreSQL and set `DATARUN_API_IMAGE` in the production host
   environment.
5. Validate Compose, then pull and replace only `app`.
6. Verify health and `/management/info`; smoke login, configuration sync, and
   one ordinary submission.

Required host values are documented in `deploy/production/.env.example`.
Keep the existing `DATARUN_JWT_BASE64_SECRET` unchanged unless key rotation and
reauthentication are the explicit purpose of a separate release.

## Rollback

Restore the recorded previous image and replace only `app`:

```bash
docker compose pull app
docker compose up -d --no-deps app
```

An incompatible migration requires its own rollback plan before release.
