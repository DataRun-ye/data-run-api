# Current Work

Updated: 2026-11-17

Purpose: accepted server work only. This file is not code, API, architecture,
or deployment authority.

## Now

- ITN 2026 household lists (campaign from the week of 2026-11-23; Reference
  form already assigned in production, see `reference-boundary.md`). Load the
  lists with `scripts/reference`: staging first, then production only with
  explicit approval and before campaign users are linked to teams. Server and
  mobile releases may ship before or during the campaign when they fix real
  field problems; each goes through the staging rehearsal first. Shared status:
  [DataRun API #34](https://github.com/DataRun-ye/data-run-api/issues/34).
- Server additions, scheduled by need rather than by the campaign: record the
  client app version with last-seen; retire Reference entries instead of
  deleting them (the phone list serves active entries only); a per-field
  "allow Add new" setting next to `ScannedCodeProperties`. Backlog: #37, #38,
  #39; mobile #34, #35.
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
