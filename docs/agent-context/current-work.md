# Current Work

Updated: 2026-11-26

Purpose: accepted server work only. This file is not code, API, architecture,
or deployment authority.

## Now

- ITN 2026 campaign (from the week of 2026-11-23) on server 6.5.8 and app
  6.1.2, with the household lists loaded in production (see
  `reference-boundary.md`). A campaign-wide go-live rule holds every upload
  until operations set the real start; operations own the campaign's dates,
  districts and forms. Field fixes may still ship during the campaign, each
  through the staging rehearsal first. Shared status:
  [DataRun API #34](https://github.com/DataRun-ye/data-run-api/issues/34).
- Data chain, then submission review. Done:
  - `scripts/integrity/check.sh` measures the gaps between submissions, the outbox, tall rows, events and pivots;
  - characterization goldens pin today's submission behaviour for each installed phone contract;
  - one submission write path: `SubmissionCommandService`, in 6.5.4;
  - privacy and logging hardening in 6.5.5: request errors are log lines and `error_event` is retired; the metrics
    endpoint and the single-submission read are admin-only; admin generic submission lists are bounded;
  - the revision journal in 6.5.6: every change to a submission is recorded in `journal.submission_revision`, from a
    baseline of every submission, with each change tied to one outbox event;
  - the submission lock in 6.5.7: every upload item and the admin delete lock the submission first, waiting at most
    5 s. Conflict detection, the feed's schema and dictionary, and the v2 foundation shipped switched off;
  - in 6.5.8, the feed's publisher and its first-publication tooling, switched off, and tall and events applied
    through the `outbox.processing` seam, each in its own savepoint. Conflict detection's rehearsal passed on a
    production clone; switching it on is a separate decision.

  Next, sequenced by engineering, with no delivery deadline:
  - a published feed of current state for downstream analytics, in its own schema, which replaces tall, events and
    pivots as the analytics interface. They retire once downstream has switched. Before its first publication: the
    placement columns, the creator as a user id, and reference values as ids, in one contract revision;
  - submission review: operations find and open any form's submissions and change them through edits with a reason,
    moves, splits, and delete and restore, over a v2 API. The admin tools use it first, then a screen for the
    operations room.

  Pivots stay admin-only and on request until downstream no longer reads them.
- Held-upload releases follow the runbook in `active-production-interface.md`. Repeat while `runAgain`; call once
  more when only `changed` is left. Use the default limit of 200 per call.
- Access (#46) and the submission lifecycle (#47) are framed for the
  access-scope work. 6.5.5 bounded the admin's generic submission lists and
  made the single-submission read admin-only; submission review's scoped read
  replaces the remaining generic submission reads.
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
