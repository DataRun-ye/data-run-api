# Current Work

Updated: 2026-07-28

Purpose: accepted server work only. This file is not code, API, architecture,
or deployment authority.

## Now

- Continue the assignment, synchronization, and access pass. Authentication
  authority and the coarse inherited-resource gate now have explicit owners.
  Reconcile the remaining session work-scope snapshot, entity read filters,
  assignment/form projection, and form-action authorization without turning
  one service into a universal access owner.
- Treat `ResourceApiAuthorization` as a compatibility boundary for inherited
  generic routes. Retire it route by route as those endpoints are removed or
  receive domain-specific authorization; reachability is not its permanent
  architecture.
- Physical role, privilege, and ACL tables remain a later Liquibase
  contraction. Endpoint/task and physical-schema removal remain separate
  decisions.

Reference activation remains parked in
[DataRun API #34](https://github.com/DataRun-ye/data-run-api/issues/34) while
the server runtime surface is cleaned.

Endpoint removal candidates are assessed in
`active-production-interface.md` and remain registered until the user
explicitly confirms them.

## Next

- Fix certificate-renewal Compose ownership in one bounded slice. Normal
  `docker compose up -d` currently starts a one-shot `letsencrypt` service that
  exits because its DNS credential path is not part of the normal deployment
  boundary. Routine API deployment must not invoke certificate issuance.
- After source ownership settles, reconcile the full Liquibase chain in one
  bounded pass before schema contraction. Prove clean replay and
  production-clone upgrade, then classify obsolete analytics, ETL, option, and
  projection changelogs without mixing in table drops.
- Determine whether the production `pg_idkit` PostgreSQL image provides any
  active extension before replacing it with the stock PostgreSQL 16 image.
  The active `generate_uid()` function is application-owned.
- After the production path and staging boundary are stable, keep one concise
  deployment playbook covering verification, image publication, staging smoke,
  production promotion, health checks, and rollback.
- After Reference activation, characterize and consolidate JWT/token ownership
  across server login, refresh, and mobile offline re-entry. Keep one active
  signing-secret owner and one intentional lifetime policy.

## Maintenance

- Keep only accepted open work.
- Remove closed work and add one factual line to `completed-work.md` when
  useful.
- Do not copy issue checklists or speculative backlog into this file.
