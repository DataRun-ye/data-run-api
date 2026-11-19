# Server Staging

This environment proves a candidate against a sanitized clone of current
production before that candidate is considered for production. Staging runs an
image, never a branch, and the rehearsal re-clones the database on every run,
so an experiment cannot contaminate a release verdict.

## What To Run

Run all commands from the canonical operator checkout on the development
machine:

```bash
cd /home/hamza/datarun/data-run-api
```

Do not clone the repository onto either staging VM. The scripts copy only the
required runtime files and operate through SSH.

When server code changed, run these two commands:

```bash
scripts/staging/publish-candidate.sh
```

```bash
DATARUN_STAGING_REHEARSAL=true scripts/staging/rehearse.sh
```

When reusing the same candidate, run only the second command.

On success, staging remains running for mobile and administrator testing until
the next explicit rehearsal. On failure, stop and report the final error; do
not continue with later commands or diagnose database/container internals.

The rehearsal replaces staging from current production, applies the candidate,
runs its automated checks, starts the API, and checks the public address. It
stops at the first failed step.

## Individual Recovery Commands

Agents may use these only to diagnose a failed rehearsal.

To refresh only the database while diagnosing connectivity:

```bash
DATARUN_STAGING_REFRESH=true scripts/staging/refresh-from-production.sh
```

To prepare/start an already published candidate without another refresh:

```bash
scripts/staging/deploy-candidate.sh
```

Do not use that last command after a failed or partially isolated preparation;
run the full rehearsal so it begins from a clean production snapshot.

## Environment

- API host: `nmcp@product-staging.lab` (`192.168.1.220`)
- Database host: `nmcp@product-staging-db.lab` (`192.168.1.221`)
- Public API: `https://staging.nmcpye.org`
- TLS proxy: `root@192.168.10.10`; the staging virtual host terminates TLS
  and proxies to `http://192.168.1.220:8080`
- Runtime directory: `/home/nmcp/datarun-staging`
- Database: `datarun_staging`

The API uses an immutable image. Secrets live only in
`/home/nmcp/datarun-staging/.env` with mode `0600`. The staging JWT key is not
the production key. PostgreSQL accepts the runtime role only from the API
host.

The database VM stores the generated runtime password at
`/etc/datarun-staging/runtime-password`, readable only by root and the
PostgreSQL service account. `provision-database.sql` creates or updates the
least-privileged runtime role from that file without putting the password in a
command argument or log.

`pg_hba-staging.conf` allows only the staging API host to reach
`datarun_staging` and rejects other remote database access. Local PostgreSQL
administration remains available through the existing Unix-socket rules.

Schedules and live capture shadowing are disabled. Staging may exercise normal
API writes during smoke tests, but it cannot run background production work.

## Refresh And Prepare

Publish a clean, fully verified candidate:

```bash
scripts/staging/publish-candidate.sh
```

The complete Maven output remains under `target/staging/`; the terminal shows
only progress or a short failure tail. The image tag includes the exact commit
and is separate from production release tags. Publication first runs
`scripts/staging/verify-config.sh`, which mechanically checks the disabled
schedulers/shadow writers, database target, public bind, and script syntax.

Then refresh the database:

```bash
DATARUN_STAGING_REFRESH=true scripts/staging/refresh-from-production.sh
```

The refresh streams a new custom-format dump directly from the current
production PostgreSQL service to the staging database VM. Dump bytes do not
pass through the operator machine. The dedicated
`datarun-production-refresh` SSH alias and private key exist only on the
staging database VM; its public key is authorized for user `nmcp` on the
production database VM `datarun-prod-db.lab` (192.168.1.111). That
`authorized_keys` entry pins a forced command, so the key can only ever emit

```
sudo -n -u postgres pg_dump -d nmcpdb -Fc --no-owner --no-privileges
```

and cannot open a shell, forward a port, or run any other command. Strict
host-key checking is required. The flow never stores production database
credentials. Production is read only; only `datarun_staging` is replaced.

Before the 2026-08-29 Proxmox move this streamed from a containerized
production database via `docker exec nmcp-db`, authorized for user `hamza` in
the Compute Engine instance's SSH metadata. Neither that container nor that
instance is in service, so the pre-move flow cannot reach production.

The dump is written to a partial file and validated before it atomically
replaces the reusable dump. Staging remains running until that transfer
succeeds. A failed transfer therefore neither replaces the database nor stops
the staging API. During transfer, the command reports compressed MiB and
elapsed time. During restore, it reports elapsed time; a percentage would be
misleading because custom-format compression and restore work are not linear.

Verify the host-to-host connection. The forced command ignores any command
argument, so passing `true` still dumps production. Read the first bytes and
stop instead; `PGDMP` confirms authentication, `sudo`, and `pg_dump` all work:

```bash
ssh nmcp@product-staging-db.lab \
  "ssh datarun-production-refresh | head -c 5"
```

If restore fails after transfer, the compressed dump remains on the DB host.
Retry with `DATARUN_STAGING_REUSE_DUMP=true` to avoid another production read.

The production database still declares the retired `pg_idkit` extension for
defaults in the inactive `supply_temp` schema. Current runtime UID generation
is owned by `public.generate_uid()`. Refresh omits the extension declaration
and installs the staging-only function in `restore-prerequisites.sql` so the
legacy table definitions and all data still restore. The function is not an
application authority and must not be copied into production migrations.

Deploy and prepare the published candidate:

```bash
scripts/staging/deploy-candidate.sh
```

Candidate preparation applies Liquibase, runs the exact assignment bootstrap,
and validates that the assignment projection rebuilds from immutable facts.
The API remains stopped during this sequence. Before startup, staging replaces
all copied password hashes, reset credentials, email logins, and refresh
tokens. It provisions only `staging-admin` and the field account, whose login
is digits only (`9000000001`) because the app logs it in; the field
identity retains the selected source user's UID and team memberships so mobile
access remains realistic. Both logins are read from the user file below. Passwords are generated once and retained only in
the staging DB host's mode-`0600` user file. Deployment pulls the digest-pinned
candidate image, stops any previous staging API before replay, rotates the
staging JWT key, and starts it only after preparation and identity sanitization
pass. Existing staging sessions never survive a candidate replacement.

The first setup must identify one production-clone field user whose access
scope is suitable for mobile smoke:

```bash
DATARUN_STAGING_FIELD_TEMPLATE_LOGIN=<source-login> \
  scripts/staging/deploy-candidate.sh
```

Later refreshes and deployments reuse the host-only selection and generated
credentials. Automated smoke reads them directly over SSH without printing
them:

```bash
DATARUN_STAGING_IDENTITY=all scripts/staging/smoke.sh
```

This checks login, current-user lookup, and refresh-token rotation for both
staging identities. To enter credentials in a mobile or administrator client,
inspect the host-only file in your own terminal. Never put its values in chat,
repository files, or command history. Explicit
`DATARUN_STAGING_LOGIN`/`DATARUN_STAGING_PASSWORD` variables remain available
for one-off smoke of another staging identity.

## Compatibility Gate

After the automated gate passes:

1. Point a non-production mobile configuration to the public staging URL.
2. Smoke installed mobile `6.0.3+54`: login, configuration sync, ordinary
   draft/save/upload/retry, and Reference select/create/reopen/upload.
3. Smoke only the administrator operations actually used against staging.
4. Record the tested image identity and outcomes in
   `docs/agent-context/initial-event-transition-boundary.md`.

Passing this gate makes the branch a production-release candidate. It does not
deploy, enable capture shadowing, or change production.
