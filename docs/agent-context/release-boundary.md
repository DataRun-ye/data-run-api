# Server Release Boundary

Role: durable server build, image, promotion, and rollback contract

Status: ACTIVE

## Stable Boundary

- `pom.xml` is the sole release-version owner.
- A release commit on `main` has tag `v<project.version>`.
- The application image is built once, as the staging candidate
  `kaswarah/datarunapi:<version>-staging-<12-character-commit>`. Release adds
  the tag `kaswarah/datarunapi:<version>-<12-character-commit>` to the same
  digest, and production pins that digest.
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
    services, and no named volumes. `datarunapi` is the only container on the
    VM: the leftovers inherited when it was cloned from the staging VM
    (local app, reference server, monitoring, local registry) were removed on
    2026-11-19. Their volumes, images and directories remain on disk.
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

The scripts require a clean tree. The gate runs the unit tests, production
build checks, JAR version/commit verification, and a local image-tar build.

Integration tests are deliberately outside the gate. Surefire excludes
`**/*IT*` and the gate's `verify` runs with `-DskipTests`, so no `*IT` class is
executed by `verify.sh`. Run them on demand with `./mvnw verify`. A coherent
test strategy is to be planned and built against the server surface that
exists at that time; until then the gate does not depend on integration
coverage.

## Staging

`deploy/staging/compose.yml` runs only the candidate API on
`product-staging.lab` (192.168.1.220) against `datarun_staging` on
`product-staging-db.lab` (192.168.1.221). It has its own JWT secret, Compose
project, and port; its ignored `.env` must never contain production
credentials.

Staging deploys an image, never a branch: the image staging validates is the
image production promotes. `deploy/staging/README.md` is the operator
procedure.

```bash
scripts/staging/publish-candidate.sh
DATARUN_STAGING_REHEARSAL=true scripts/staging/rehearse.sh
```

Use the second command alone when the image has not changed. To baseline
staging against production without building, set `DATARUN_API_IMAGE` to the
image production is running and rehearse; Liquibase then has nothing to apply.

Before promotion, verify health and `/management/info`, then smoke login,
configuration reads, and one ordinary idempotent submission upload.

## Release

Promote the rehearsed staging image by digest; never rebuild it for release.
The image carries its build time (`/management/info` reports it as `built`),
so a rebuild of the same commit is a different, untested image.

1. Fast-forward `main` to the candidate's commit (named in its tag and in
   `target/staging/candidate.env`) and tag `v<version>` there.
2. Add the release tag to the candidate's digest, then check that the tag
   resolves to that digest:

   ```bash
   docker buildx imagetools create --prefer-index=false \
     --tag kaswarah/datarunapi:<version>-<commit> kaswarah/datarunapi@<digest>
   docker buildx imagetools inspect kaswarah/datarunapi:<version>-<commit>
   ```

   Without `--prefer-index=false`, buildx wraps the image in a new index with
   a different digest.
3. Back up PostgreSQL. On the production host, keep the current `.env` as the
   rollback copy and set `DATARUN_API_IMAGE` to
   `kaswarah/datarunapi:<version>-<commit>@<digest>`. If the release changes
   `deploy/production/compose.yml`, also keep the host's current `compose.yml`
   as the rollback copy and replace it with the release's; the host copy is not
   updated otherwise.
4. Validate Compose, then pull and replace only `app`.
5. Verify health and `/management/info`; smoke login, configuration sync, and
   one ordinary submission.
6. Publish the GitHub release with the digest and the rollback image.

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
