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
- The staging VM's external route reset Docker Hub downloads and then became
  unreachable during the resumable archive fallback. Candidate migrations,
  assignment bootstrap/replay, and API startup have not run.

Resume by completing the same image delivery on the staging LAN, then run the
existing digest-pinned deployment gate. Do not refresh the database again.
