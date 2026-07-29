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
