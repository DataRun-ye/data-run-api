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
- Its project name is `datarun` and its container name is `datarunapi`;
  changing either detaches the deployed container identity.
- Since the 2026-08-29 move from Google Cloud to Proxmox, production is two
  VMs on `pve.lab`:
  - API: `datarun-prod.lab` (192.168.1.110), project directory
    `/home/nmcp/datarun`. This Compose project owns exactly one container,
    `app`/`datarunapi`. There are no bundled database, nginx, or certificate
    services, and no named volumes. The VM additionally runs containers that
    this repository does not own; verified 2026-11-16: datarun-local-app-*,
    datarun-reference-server-1, datarun-prometheus, datarun-node-exporter,
    datarun-blackbox-exporter.
  - Database: `datarun-prod-db.lab` (192.168.1.111), PostgreSQL 16 as a
    managed VM service with database and role `nmcpdb`, reached over TLS
    (`sslmode=require`). It is not containerized and `compose.yml` does not
    own it.
  - nginx and TLS termination are external to this repository.
  - `pg_hba-production.conf`, `provision-production-role.sql`, and
    `restore-prerequisites.sql` in `deploy/production/` are the DB VM's
    spec. The runtime role password lives only in
    `/etc/datarun-production/runtime-password` on that VM.
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

```bash
cp deploy/staging/.env.example deploy/staging/.env
chmod 600 deploy/staging/.env
docker load -i target/jib-image.tar
docker compose --env-file deploy/staging/.env -f deploy/staging/compose.yml up -d
```

Before promotion, verify health and `/management/info`, then smoke login,
configuration reads, and one ordinary idempotent submission upload.

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
