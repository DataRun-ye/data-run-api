# Current Work

Updated: 2026-07-26

Purpose: accepted server work only. This file is not code, API, architecture,
or deployment authority.

## Now

- Deploy and verify the bounded Reference foundation without activation:
  [DataRun API #35](https://github.com/DataRun-ye/data-run-api/issues/35).
- Shared sequence and compatibility state:
  [DataRun API #34](https://github.com/DataRun-ye/data-run-api/issues/34).

The server task closes before catalog import or a Reference form is assigned.
Detailed implementation evidence belongs in `reference-boundary.md`; mutable
status belongs in the issues.

## Next

- Retire MongoDB through one bounded production slice: prove the remaining
  runtime reads and writes, preserve or migrate anything still active, then
  remove its application configuration, repositories, Compose service, and
  production container.
- After MongoDB retirement, assess the remaining server runtime and
  development surface for active ownership, obsolete registrations,
  duplicated persistence/configuration, and deployment smells. Convert only
  proven findings into focused removal or consolidation work.
- After Reference activation, characterize and consolidate JWT/token ownership
  across server login, refresh, and mobile offline re-entry. Keep one active
  signing-secret owner and one intentional lifetime policy.

## Maintenance

- Keep only accepted open work.
- Remove closed work and add one factual line to `completed-work.md` when
  useful.
- Do not copy issue checklists or speculative backlog into this file.
