# Current Work

Updated: 2026-07-29

Status: ACTIVE

## Transition Staging Closure

Prepare one durable staging environment for the accepted initial event
transition candidate:

- refresh `datarun_staging` directly from the current production database;
- apply candidate migrations, assignment bootstrap, and projection replay;
- run the candidate at `staging.nmcpye.org` with schedules and live capture
  shadowing disabled;
- preserve host-only staging credentials and an immutable candidate identity;
- smoke released mobile `6.0.3+54` and the administrator operations actually
  used.

This slice changes no product contract and does not deploy to production.
Completion evidence and the release-candidate assessment belong in
`initial-event-transition-boundary.md`.

## Checkpoint

- Fresh production snapshot restored as `datarun_staging`: 2.15 GB, 254
  production Liquibase records, 210,447 assignments, and 53,303 submissions.
- Staging PostgreSQL accepts the `datarun_staging` runtime role only from
  `192.168.1.220`; credentials and the staging JWT remain host-only.
- Full release verification passed for commit
  `fe0bc0690b292668c1fddf7b37b3e47b01b7f97e`.
- Candidate image `kaswarah/datarunapi:6.4.1-staging-fe0bc0690b29` is published
  at digest
  `sha256:2d255fc9809040ebeff725ca6968204d3953998a214b62aa20ef699f9154dc87`.
- The candidate archive was delivered over the staging LAN, matched the local
  Jib archive exactly, and loaded as the same immutable digest.
- The initial preparation exposed an entrypoint argument-forwarding defect;
  that container and API were stopped. The corrected gate now uses an explicit
  entrypoint, no host network, host-only temporary secret files, deterministic
  cleanup/timeouts, and a bounded synchronous migration phase.
- The corrected full gate passed on the disposable staging state: assignment
  bootstrap `SUCCESS` with 263,622 tuples and zero baseline differences;
  event-only replay `EXACT` with zero missing, unexpected, or differing rows.
- The final clean production refresh is blocked before dump creation because
  direct SSH to `api.nmcpye.org:22` times out. Staging API is stopped and the
  database remains disposable until that refresh completes.

Resume with one fresh production stream, then run the already-proven
digest-pinned deployment gate. No image transfer or further gate redesign is
required.
