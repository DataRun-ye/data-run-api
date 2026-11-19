# Current Work

Updated: 2026-11-19

Purpose: accepted server work only. This file is not code, API, architecture,
or deployment authority.

## Now

- ITN 2026 campaign (from the week of 2026-11-23) on server 6.5.0 and app
  6.1.0, with the household lists loaded in production (see
  `reference-boundary.md`). A campaign-wide go-live rule holds every upload
  until operations set the real start; operations own the campaign's dates,
  districts and forms. Field fixes may still ship during the campaign, each
  through the staging rehearsal first. Shared status:
  [DataRun API #34](https://github.com/DataRun-ye/data-run-api/issues/34).
- Go-live and held uploads (#42): the first slice, admin API only, shipped in
  6.5.0. Next: an admin screen, a phone indicator, a closing time, and holding
  data stored before a go-live existed.
- Server additions, scheduled by need rather than by the campaign: retire
  Reference entries instead of deleting them (the phone list serves active
  entries only); a per-field "allow Add new" setting next to
  `ScannedCodeProperties`. Backlog: #38, #39.
- Define the product-policy cutover for same-UID submission mutation,
  synchronized edit/delete, and conflict behavior. The released app currently
  creates and idempotently retries submissions; compatibility reachability is
  not authority to invent a broader lifecycle.

Endpoint removal candidates are assessed in
`active-production-interface.md` and remain registered until the user
explicitly confirms them.

## Next

- Close the named access compatibility exits after their endpoint/client
  decisions: inherited-route gating, the four generic group/set filters,
  duplicated mobile `formPermissions`, V1 profile legacy fields, user-group
  CRUD/schema, and role/privilege/ACL schema residue.
- After source ownership settles, reconcile the full Liquibase chain in one
  bounded pass before schema contraction. Prove clean replay and
  production-clone upgrade, then classify obsolete analytics, ETL, option, and
  projection changelogs without mixing in table drops.
- After the production path and staging boundary are stable, keep one concise
  deployment playbook covering verification, image publication, staging smoke,
  production promotion, health checks, and rollback.
- Finish consolidating JWT/token ownership. Refresh-token rotation now has one
  owner in `TokenService` and one failure status (`401`), proven by
  `TokenServiceTest`, `CustomJwtAuthenticationConverterTest`, and
  `ExceptionTranslatorIT`. Server login and mobile offline re-entry are still
  unconsolidated; keep one active signing-secret owner and one intentional
  lifetime policy.

## Maintenance

- Keep only accepted open work.
- Remove closed work and add one factual line to `completed-work.md` when
  useful.
- Do not copy issue checklists or speculative backlog into this file.
