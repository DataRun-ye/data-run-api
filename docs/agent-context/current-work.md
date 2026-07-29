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

- Fresh production snapshot restored directly from production to
  `datarun_staging`: 254 production Liquibase records, 210,447 assignments,
  and 53,448 submissions.
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
- Production refresh SSH is owned by the staging DB VM through a dedicated,
  Google instance-metadata-managed key and strict host verification. Dump data
  travels directly from production to staging DB and no longer traverses the
  operator machine.
- The clean-clone candidate gate passed: migrations completed, assignment
  bootstrap reported `SUCCESS` with 263,622 tuples, and event-only replay
  reported `EXACT`.
- The digest-pinned API is healthy on the staging LAN endpoint. The nginx
  upstream was corrected from HTTPS port 80 to HTTP port 8080; public health
  and exact build-identity smoke now pass.
- A normalized 30-day production access-log check confirms active
  administrator use of assignment query/create/bulk, team query/create,
  organization-unit query/create/bulk, activity, data-element, option-set,
  form-template publication, submission query, and pivot routes. Registered
  routes outside this list are not made canonical by this evidence.
- Authenticated mobile and administrator workflow smoke remain open.

Resume with the released-client and actually-used administrator compatibility
checks. Do not refresh or prepare the database again.
