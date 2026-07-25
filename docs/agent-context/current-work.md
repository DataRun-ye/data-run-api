# Current Work

Updated: 2026-07-26

Purpose: accepted server work only. This file is not code, API, architecture,
or deployment authority.

## Now

- Retire MongoDB through one bounded slice. Production inventory found all
  eight collections empty; separate active template/query contracts from the
  misleading `mongo` package, then remove Mongo persistence, configuration,
  dependencies, and Compose ownership. Prove the candidate locally against
  the production clone.
- Before promoting the Mongo-retired image, establish one isolated staging
  environment with its own database, secrets, Compose project, and URL. Smoke
  the same immutable image there, then remove the empty production Mongo
  container and volume only after production verification.

Reference activation remains parked in
[DataRun API #34](https://github.com/DataRun-ye/data-run-api/issues/34) while
the server runtime surface is cleaned.

## Next

- After MongoDB retirement, assess the remaining server runtime and
  development surface for active ownership, obsolete registrations,
  duplicated persistence/configuration, and deployment smells. Convert only
  proven findings into focused removal or consolidation work.
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
