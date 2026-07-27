# Runtime Surface Cleanup

Role: living evidence ledger for classifying and reducing the server source
surface before ownership refactoring

Status: LIVING

This document tracks scan coverage and disposition. It is not API, product,
schema, or deployment authority. Active mobile contracts remain in
`active-production-interface.md`.

## Outcome

Every examined source area must end in one of these dispositions:

- `KEEP-CORE`: required by an active production capability.
- `KEEP-PROJECTION`: active export, ETL, outbox, event, or analytics projection.
- `KEEP-OPERATIONAL`: active administration, maintenance, or startup behavior.
- `KEEP-COMPATIBILITY`: intentionally retained for deployed clients or data.
- `REMOVE-SOURCE`: no executable root, inbound consumer, data-contract role, or
  operator use; remove in a bounded source-only slice.
- `SEPARATE-API`: registered endpoint or task whose retirement requires an
  explicit compatibility or operator decision.
- `SEPARATE-SCHEMA`: source is inactive, but physical persistence is removed
  only through its own Liquibase slice.
- `UNKNOWN`: a specific runtime, data, or operator evidence gap remains.

`@Deprecated`, comments, names, package location, and zero direct Java callers
are candidate evidence only. They never determine disposition by themselves.

## Scan Cycle

Each pass:

1. Enumerates HTTP routes, inherited routes, schedules, runners, listeners,
   filters, startup hooks, Spring registrations, JPA callbacks, and entities.
2. Traces the root through services, repositories, persistence, and external
   effects.
3. Checks inbound references plus JSON/Jackson, JPA, Liquibase, reflection,
   configuration, and stored-data contracts.
4. Cross-checks released mobile calls and known manual operations.
5. Classifies every examined item using one disposition above.
6. Proves `REMOVE-SOURCE` candidates in a clean temporary snapshot before
   changing the real tree.
7. Closes one bounded removal or records one explicit later slice. Endpoint,
   scheduled-task, schema, and ownership changes are not hidden in source
   cleanup.

Removal verification requires compilation, focused tests, the full Maven test
gate, and Spring startup against the local production clone when registrations
or persistence mappings are affected. Production is not changed during this
cleanup sequence.

## Coverage Order

| Pass | Scope | Status |
| --- | --- | --- |
| 0 | Executable and data-contract roots | IN PROGRESS |
| 1 | Source-dead leaves and orphan graphs | IN PROGRESS |
| 2 | Assignment, synchronization, and access | PENDING |
| 3 | Form templates, versions, elements, and rules | PENDING |
| 4 | Submission upload, persistence, validation, and compatibility | PENDING |
| 5 | Outbox, ETL, events, tall tables, exports, analytics, and MVs | PENDING |
| 6 | Remaining operational, generic-resource, audit, import, cache, and utility surfaces | PENDING |
| 7 | Schema-only residue after source ownership is closed | PENDING |

## Current Evidence

| Candidate | Evidence | Disposition |
| --- | --- | --- |
| `domainmapping/` | No executable Spring or JPA root, no outside inbound reference, and no entity. Removing it with `importer/` and `importprocessor/` from clean `HEAD` compiled and passed all 75 tests. | `REMOVE-SOURCE` |
| `importprocessor/` | No executable Spring or JPA root, no outside inbound reference, and no entity. Passed the same temporary removal test. | `REMOVE-SOURCE` |
| `importer/` | Its active annotations create only an internal orphan bean graph. There is no endpoint, scheduler, service, outside consumer, or entity. Passed the same temporary removal test. | `REMOVE-SOURCE` |
| `acl/` | Mixed package. `AclConfig` and `DefaultAclService` supply calls made by generic resources. `AclBootstrap` is registered but its runner is disabled. | Pass 2; no package-wide deprecation |
| `etl/` | Active scheduled and manual projection/export behavior; not a core source of assignment, form, or submission truth. | `KEEP-PROJECTION`; inspect alternatives in pass 5 |
| `FormTemplateResource` | Released mobile fetches `GET /api/v1/formTemplates`; route contract test exists. | `KEEP-CORE` |
| `TemplateVersionResource` | Released mobile fetches `GET /api/v1/formTemplateVersions`; route contract test exists. | `KEEP-CORE` |
| `DataSubmissionResource` | Owns active versioned bulk submission upload. Its admin delete route is a separate product/API decision. | `KEEP-CORE`; delete route is `SEPARATE-API` |
| Assignment path maintenance | Clean `HEAD` registers a daily schedule and a callable endpoint. Necessity may be reviewed later, but it is not source-dead. | `KEEP-OPERATIONAL` for this pass |
| `ValueType`, `RuleAction`, and `FormDataElementConf` properties | Serialized template data can reach these without direct Java call sites. | Pass 3 data-contract check |
| Generic inherited writes and registered admin resources | Reachable routes are not source-dead merely because the released mobile does not call them. | `SEPARATE-API` unless operator use is proven |

## Dirty Candidate Patch

The current unstaged patch is an input list, not an accepted cleanup:

- Commented blocks, unused private fields, imports, and behavior-neutral method
  naming can be retained only in their owning pass.
- Removing assignment maintenance methods also removes a schedule and endpoint;
  keep that change parked.
- Class-level deprecations on active form-template resources conflict with
  released mobile evidence.
- Deprecations on ACL, ETL, audit, data-element, team migration, generic write,
  submission delete, and serialized enum/property surfaces require their
  owning pass or a separate API/data-contract decision.
- No class or package is removed merely because this patch labels it inactive.

## Next Slice

Close pass 1 by removing only the three proven source-dead packages, then run
the full verification gate against the actual combined worktree. After that,
continue the remaining leaf candidates before entering assignment, sync, and
access ownership.
