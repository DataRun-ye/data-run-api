# Legacy Seams

Role: the map of the old structures new code meets: their kinds, the seams it uses, the exceptions, and the gaps

Status: LIVING

[`NewCodeSeamsTest`](../../src/test/java/org/nmcpye/datarun/NewCodeSeamsTest.java) holds the same lists and
enforces them; change both together. Facts were read from `develop` on 2026-11-26. Package names are relative to
`org.nmcpye.datarun`.

## Kinds

| Kind | What it is | New code |
| --- | --- | --- |
| Retiring derived | derived from submissions, and replaced by the feed | no dependency, either way |
| Transitional | the outbox: stays for now, as an edge integration artifact | only through its seam |
| System of record | data and rules old code still owns | only through its seam |
| Client contract | what installed phones and the admin's tools rely on | none: adapted at the edge |
| Neutral infrastructure | configuration and shared plumbing | any use |

## New Code

| Package | What it is | Branch |
| --- | --- | --- |
| [`submission.normalized`](../../src/main/java/org/nmcpye/datarun/submission/normalized/) | the submission normalizer: a stored submission and its form's definitions in, values by field id and row id out; pure (`NormalizedSubmissionPurityTest`) | develop |
| [`submission.normalized.jdbc`](../../src/main/java/org/nmcpye/datarun/submission/normalized/jdbc/FormDefinitionJdbcReader.java) | `FormDefinitionJdbcReader`: every version of a form with its data element and option set ids, in one statement | develop |
| [`submission.feed`](../../src/main/java/org/nmcpye/datarun/submission/feed/) | the feed's switch and reader role; `publisher` writes the form dictionary, and the submission streams on `feat/feed-v1` | develop |
| [`web.v2`](../../src/main/java/org/nmcpye/datarun/web/v2/) | v2's conventions (`api-v2.md`): problems, its security chain, `ApiCaller`, ids, paging, idempotent commands | develop |
| [`jpa.datasubmission.revision`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/revision/) | the journal: `SubmissionRevisionWriter`; `SubmissionRevisionReader` on `feat/review-s2` | develop |
| `jpa.datasubmission.conflict` | conflict detection, called by `SubmissionUploads` for each upload item; it takes the values the write path computes, and its breaches are listed (Exceptions) until its seam refactor | feat/review-s2 |

## Retiring Derived

| Structure | Code | Note |
| --- | --- | --- |
| `analytics.tall_canonical`, `analytics.events`, `analytics.dim_*`, `etl_runs` | [`etl`](../../src/main/java/org/nmcpye/datarun/etl/): the tall and events writers, their transformers, the dimensions | `etl` is also, for now, the home and name of the outbox's claim loop (Transitional) |
| schema `pivot`, `analytics.pivot_manifest`, `analytics.canonical_element_config` | [`etl/pivot`](../../src/main/java/org/nmcpye/datarun/etl/pivot/), [`PivotController`](../../src/main/java/org/nmcpye/datarun/web/rest/v1/PivotController.java) | per-form views over tall, admin route |
| the outbox's `payload`, a copy of `form_data` | written by [`DefaultDataSubmissionService.java:215`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/service/DefaultDataSubmissionService.java#L215) | no code reads it: the claim loads it ([`OutboxClaimReader.java:105`](../../src/main/java/org/nmcpye/datarun/outbox/repository/OutboxClaimReader.java#L105)), and the ETL re-reads the submission ([`EventProcessorServiceImpl.java:63`](../../src/main/java/org/nmcpye/datarun/etl/service/impl/EventProcessorServiceImpl.java#L63)) |
| `canonical_element` | [`CanonicalElement`](../../src/main/java/org/nmcpye/datarun/jpa/datatemplate/CanonicalElement.java), [`CanonicalElementRepository`](../../src/main/java/org/nmcpye/datarun/jpa/datatemplate/repository/CanonicalElementRepository.java), [`MetadataUpsertService`](../../src/main/java/org/nmcpye/datarun/jpa/datatemplate/repository/MetadataUpsertService.java), [`jpa/datatemplategenerator`](../../src/main/java/org/nmcpye/datarun/jpa/datatemplategenerator/) with `TemplateElement`, `SemanticType`, `DataType` | only the ETL and the pivots read it; publishing a form version writes it in the same call ([`DataTemplateInstanceServiceImpl.java:144`](../../src/main/java/org/nmcpye/datarun/jpa/datatemplate/service/DataTemplateInstanceServiceImpl.java#L144)) |
| `error_event` | none since 6.5.5 | table only |
| `jpa.etl` | [`package-info.java`](../../src/main/java/org/nmcpye/datarun/jpa/etl/package-info.java) | an empty package (debt: dead) |

## Transitional: The Outbox

| Structure | Code | Seam for new code | Never touch |
| --- | --- | --- | --- |
| `outbox`, `outbox_processing` | [`outbox`](../../src/main/java/org/nmcpye/datarun/outbox/); its claim loop still lives in [`etl/orchestrator`](../../src/main/java/org/nmcpye/datarun/etl/orchestrator/) and [`etl/admin`](../../src/main/java/org/nmcpye/datarun/etl/admin/) (debt: move it to `outbox.processing` and rename it) | `outbox.processing` (`ChangeOutput`, on `feat/feed-v1`): a change as a submission id and version | the table, `OutboxDto`, the claim and processing services |

The outbox is an edge integration artifact, and the core must not depend on it. Each row references the revision it
announces ([`20261124-outbox-revision-key.xml:32`](../../src/main/resources/config/liquibase/changelog/etl/20261124-outbox-revision-key.xml#L32)).
Two links run the wrong way today: the journal's gap repair reads outbox rows without a version as its cue
([`20261124-submission-revision-gap-repair.xml:25`](../../src/main/resources/config/liquibase/changelog/data_submission/20261124-submission-revision-gap-repair.xml#L25), `:56`),
until no pre-journal image is a rollback target; and the write path copies `form_data` into `payload`, until the
retention step (C3). Whether the outbox should exist beside the journal at all is an open question, for a later spike.

## Systems Of Record

| Area | Tables | Code | Seam for new code | Never touch |
| --- | --- | --- | --- | --- |
| Submissions | `data_submission` with `form_data` | [`DataSubmission`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/DataSubmission.java), [`repository`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/repository/), [`service`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/service/) (persistence port), [`validation`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/validation/) | write: [`SubmissionCommandService`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/command/SubmissionCommandService.java) and [`SubmissionActor`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/command/SubmissionActor.java), with no command variant yet. Read: the normalizer's [`StoredSubmission`](../../src/main/java/org/nmcpye/datarun/submission/normalized/StoredSubmission.java) and [`StoredJson`](../../src/main/java/org/nmcpye/datarun/submission/normalized/StoredJson.java), which no reader fills (gap 1) | the entity, repository, port and `StoredSubmissionGuard`; the write path's internals `SubmissionUploads`, `SubmissionLock`, `HeldSubmissionDecisions` |
| Submissions | `journal.submission_revision` | [`jpa/datasubmission/revision`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/revision/) | the journal package; only `DefaultDataSubmissionService` appends | the table outside the package |
| Submissions | `held_submission`, `go_live_rule` | [`jpa/intake`](../../src/main/java/org/nmcpye/datarun/jpa/intake/) | `SubmissionCommandService` (release, discard) | repositories, `SubmissionIntake`, `GoLiveRuleService` |
| Submissions | repeat ids for old phones | [`MigrationRepeatIdGenerator`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmissionbatching/job/MigrationRepeatIdGenerator.java) | the write path, internally | the class |
| Reference lists | `reference_entry` | [`jpa/reference`](../../src/main/java/org/nmcpye/datarun/jpa/reference/) | none (gap 5) | all |
| Form definitions | `data_template`, `data_template_version` (`fields`, `sections`), `data_element`, `option_set`, `option_value` | [`jpa/datatemplate`](../../src/main/java/org/nmcpye/datarun/jpa/datatemplate/), [`datatemplateelement`](../../src/main/java/org/nmcpye/datarun/datatemplateelement/), [`datatemplateprocessor`](../../src/main/java/org/nmcpye/datarun/datatemplateprocessor/), [`jpa/dataelement`](../../src/main/java/org/nmcpye/datarun/jpa/dataelement/), [`jpa/option`](../../src/main/java/org/nmcpye/datarun/jpa/option/) | read: `FormDefinitionJdbcReader` and `SubmissionNormalizer`; vocabulary: [`ValueType`](../../src/main/java/org/nmcpye/datarun/datatemplateelement/enumeration/ValueType.java) | entities, repositories, `TemplateVersionResolver`, `TemplateVersionContext`, processors, element configuration |
| Configuration | `assignment`, `team` (team form grants in `form_permissions`), `team_user`, `team_managed_teams`, `org_unit`, `ou_level`, `org_unit_group*`, `activity`, `project` | [`jpa/assignment`](../../src/main/java/org/nmcpye/datarun/jpa/assignment/), [`jpa/team`](../../src/main/java/org/nmcpye/datarun/jpa/team/), [`jpa/orgunit`](../../src/main/java/org/nmcpye/datarun/jpa/orgunit/), `jpa/orgunitgroup`, `jpa/orgunitgroupset`, `jpa/oulevel`, [`jpa/activity`](../../src/main/java/org/nmcpye/datarun/jpa/activity/), `jpa/project` | none (gap 4) | all |
| Accounts, access scope | `app_user`, `app_authority`, `app_user_authority`, `refresh_token`, `user_group`; the access snapshot built per request | [`jpa/user`](../../src/main/java/org/nmcpye/datarun/jpa/user/), `jpa/userauthority`, `jpa/userrefreshtoken`, `jpa/usegroup`, [`security`](../../src/main/java/org/nmcpye/datarun/security/), [`jpa/accessfilter`](../../src/main/java/org/nmcpye/datarun/jpa/accessfilter/), `userdetail`, `jpa/migration` | identity: [`ApiCaller`](../../src/main/java/org/nmcpye/datarun/web/v2/caller/ApiCaller.java), the only new code that reads `CurrentUserDetails`; the v2 chain resolves the token with `DomainUserDetailsService`. Scope: none (gap 4) | `CurrentUserDetails`, `SecurityUtils`, `AssignmentFormAccessService`, `UserAccessService` and its filters |
| Generic internals | none of their own | [`jpa/common`](../../src/main/java/org/nmcpye/datarun/jpa/common/), `common` (entity bases, `IdentifiableObjectService` family), `common.enumeration`, `common.repository`, `common.translation`, [`jpa/auditing`](../../src/main/java/org/nmcpye/datarun/jpa/auditing/), `service` | none | all |

Where today's seams serve new code poorly:

- Write path: every [`SubmissionCommand`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/command/SubmissionCommand.java)
  variant carries a client contract or a uid: the phone payload (`:18`, `:25`), the entity (`:32`), a uid
  (`:36`), the v1 held-decision request (`:43`, `:51`). The upload answers `EntitySaveSummaryVM`, its failures
  as `ErrorCode` text ([`SubmissionUploads.java:401`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/command/SubmissionUploads.java#L401)).
  `SubmissionActor` carries the login and the whole access snapshot ([`SubmissionActor.java:20`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/command/SubmissionActor.java#L20)).
- Form definitions are read by form uid only ([`FormDefinitionJdbcReader.java:102`](../../src/main/java/org/nmcpye/datarun/submission/normalized/jdbc/FormDefinitionJdbcReader.java#L102), `:120`),
  and `StoredSubmission` takes the stored form and version uids.
- Stored references are uids: `data_submission` keeps form, version, assignment, team, org unit and activity
  by uid ([`DataSubmission.java:75`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/DataSubmission.java#L75) to `:103`),
  and the journal's metadata copies them ([`20261124-submission-revision.xml:142`](../../src/main/resources/config/liquibase/changelog/data_submission/20261124-submission-revision.xml#L142)).
- Access: the snapshot speaks team, activity and form uids ([`CurrentUserDetails.java:70`](../../src/main/java/org/nmcpye/datarun/security/CurrentUserDetails.java#L70) to `:78`),
  and v2's chain rebuilds all of it on every request, teams found by login
  ([`CurrentUserDetailsService.java:44`](../../src/main/java/org/nmcpye/datarun/security/CurrentUserDetailsService.java#L44)),
  though `ApiCaller` keeps only the id and authorities.

## Client Contracts

| Contract | Edge adapter | What travels inward today (debt) |
| --- | --- | --- |
| Phone uploads: `bulk?referenceVersion=1`, `bulk/practice`, legacy `bulk` | [`web/rest/v1/datasubmission`](../../src/main/java/org/nmcpye/datarun/web/rest/v1/datasubmission/): resource, `DataSubmissionUploadV1Dto`, `DataSubmissionUploadV1Mapper` | the DTO is the command item, mapped inside the write path ([`SubmissionUploads.java:234`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/command/SubmissionUploads.java#L234)); held uploads are replayed from it ([`HeldSubmissionDecisions.java:85`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/command/HeldSubmissionDecisions.java#L85)) |
| Held-upload and go-live admin routes | [`web/rest/v1/intake`](../../src/main/java/org/nmcpye/datarun/web/rest/v1/intake/) | `HeldDecisionRequest` is the command's selection |
| Sync: assignments, forms, form templates and versions | [`web/rest/postgres/assignment`](../../src/main/java/org/nmcpye/datarun/web/rest/postgres/assignment/), [`web/rest/v1/formtemplate`](../../src/main/java/org/nmcpye/datarun/web/rest/v1/formtemplate/); DTOs and mappers in `jpa/assignment/{dto,mapper}`, `jpa/datatemplate/{dto,mapper}` | services return v1 DTOs ([`AssignmentService.java:24`](../../src/main/java/org/nmcpye/datarun/jpa/assignment/service/AssignmentService.java#L24)); `DataTemplateInstanceDto` is the upload's template model ([`TemplateVersionContext.java:14`](../../src/main/java/org/nmcpye/datarun/jpa/datatemplate/TemplateVersionContext.java#L14)) |
| Login, refresh, profile | [`web/rest/v1/authenticate`](../../src/main/java/org/nmcpye/datarun/web/rest/v1/authenticate/), [`web/rest/v1/account`](../../src/main/java/org/nmcpye/datarun/web/rest/v1/account/) (`CurrentUserProfileV1`) | the token's subject is the login, resolved by `DomainUserDetailsService` for v1 and v2; `ApiCaller` keeps the user id |
| Reference lists | [`web/rest/v1/referenceentry`](../../src/main/java/org/nmcpye/datarun/web/rest/v1/referenceentry/) | none |
| Inherited generic routes, query language | [`web/rest/common`](../../src/main/java/org/nmcpye/datarun/web/rest/common/), [`web/rest/postgres`](../../src/main/java/org/nmcpye/datarun/web/rest/postgres/), [`apiquery`](../../src/main/java/org/nmcpye/datarun/apiquery/) | none |
| v1 errors | [`ExceptionTranslator`](../../src/main/java/org/nmcpye/datarun/web/rest/errors/ExceptionTranslator.java); codes in [`common/feedback`](../../src/main/java/org/nmcpye/datarun/common/feedback/), thrown as `ErrorCodeException` | codes are raised deep in old domain code |
| uids | every configuration table's `uid`; phones name records by uid | stored references and the access snapshot, above |
| Login as identity | `created_by`, `last_modified_by` ([`SpringSecurityAuditorAware.java:16`](../../src/main/java/org/nmcpye/datarun/security/SpringSecurityAuditorAware.java#L16)), `held_submission.submitted_by` and `decided_by` | `SubmissionActor.login`; a submission's creator is compared by login ([`StoredSubmissionGuard.java:68`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/validation/StoredSubmissionGuard.java#L68)) |

## Neutral Infrastructure

`config` (activation switches, the JWT converter, properties), `web.filter` (request ids), `aop`,
`management`, the application class, and `FailureShape`, `DatabaseFailures`, `CodeGenerator` (new ids with
`nextUlid`; `generateUid` mints phone uids) and `UuidUtils`.

## Seams

| Seam | Used from | For |
| --- | --- | --- |
| `SubmissionCommandService`, `SubmissionActor` | any new code | the single write path; no command variant is a seam until one carries no client contract and no uid |
| `ValueType` | any new code | the stored value-type vocabulary in the normalizer's definitions |
| `CurrentUserDetails` | `web.v2.caller` | `ApiCaller` keeps the user id and authorities |
| `DomainUserDetailsService` | `web.v2.config` | the v2 chain authenticates as v1 does |
| `outbox.processing` | any new code | the outputs' seam: a change as a submission id and version |
| SQL: `data_template`, `data_template_version`, `data_element`, `option_set` | `FormDefinitionJdbcReader` | the normalizer's form definitions |
| SQL: `data_submission` | the journal package | the journal copies the row it records |
| SQL: `data_submission` | `submission.normalized.jdbc.StoredSubmissionJdbcReader`, **planned** | stored submissions by id and in batches |
| SQL: `assignment`, `team`, `org_unit`, `activity` | `submission.normalized.jdbc.PlacementJdbcResolver`, **planned** | one placement resolver, turning placement uids into ids |

The two planned seams are being built for the feed (engineering, 2026-11-26). Until they exist where named, SQL in
new code that names their tables anywhere else fails the test; once built, the test wants them marked built. Outside
new code, these tables are named only by the write path.

## Exceptions

| Class | Depends on | Until |
| --- | --- | --- |
| [`OutboxClaimServiceImpl`](../../src/main/java/org/nmcpye/datarun/outbox/service/impl/OutboxClaimServiceImpl.java) | `etl.config.EtlProperties` (its claim lease) | the claim loop leaves `etl` |
| [`OutboxProcessingServiceImpl`](../../src/main/java/org/nmcpye/datarun/outbox/service/impl/OutboxProcessingServiceImpl.java) | `etl.service.EtlRunService` (outcomes counted on the ETL's run) | the claim loop leaves `etl` |
| `ConflictDetection` | `DataSubmission`: the stored entity from `SubmissionUploads`, whose uids it logs | removed by `refactor/conflict-detection-seam` |
| `ConflictDetection` | `TemplateVersionContext`: the copy's form version, for the row id check | removed by `refactor/conflict-detection-seam` |
| `ConflictDetection` | `MigrationRepeatIdGenerator`: whether the copy has rows without ids | removed by `refactor/conflict-detection-seam` |
| `ConflictDetection` | `StoredSubmissionGuard`: whether the uploader made the submission | removed by `refactor/conflict-detection-seam` |
| `ConflictDetection` | `CurrentUserDetails`: the uploader's access snapshot, for that question | removed by `refactor/conflict-detection-seam` |
| `ConflictCheck` | `ErrorCode`: the v1 refusal codes E4124, E4114 and E4122 that keep a copy aside after a move | removed by `refactor/conflict-detection-seam` |

The conflict detection rows apply from `feat/review-s2`, which lands before the guard (checked at `8649e9ef`). Each
exception must still occur while its class exists.

## Gaps

Submission review (`docs/features/submission-review/spec.md`) needs these; none is built yet.

| # | Need | Systems of record | Seam to build | Today |
| --- | --- | --- | --- | --- |
| 1 | Search, view and history: submissions by filters and by id | `data_submission`, journal | `StoredSubmissionJdbcReader` (planned: by id and in batches); search by filters under the search guard still to add | `feat/feed-v1`'s `FeedWriter` and `Reconcile`, and `feat/review-s2`'s `SubmissionRevisionReader`, each read `data_submission` on their own |
| 2 | Values typed for a form named by id; edits written back | form definitions | a read by form id, and the write direction (field id and row id to the stored place), in `submission.normalized` | uid-keyed reader; no write direction |
| 3 | Edits, moves, splits, deletes and restores | `data_submission`, journal | write-path commands for new code: `SubmissionCommand` variants keyed by submission id, with domain types and the journal's reason and detail, run in v2's idempotent transaction, answering a result of their own | every variant carries a client contract or a uid |
| 4 | A move's destination rules; scope by activity, team, area | configuration, access scope | `PlacementJdbcResolver` (planned: uids to ids); an assignment's forms and deleted state, and org unit ancestry, still to add. Review's own grants are new code | entities and v1 DTOs keyed by uid; the snapshot in uids; `feat/feed-v1`'s private `Placement` |
| 5 | View's entries; history's actor names | `reference_entry`, `app_user` | small readers by id | none |
