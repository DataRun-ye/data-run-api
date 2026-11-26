# Legacy Seams

Role: the map of the old structures new code meets: their kinds, the seams it uses, the exceptions, and the gaps

Status: LIVING

[`NewCodeSeamsTest`](../../src/test/java/org/nmcpye/datarun/NewCodeSeamsTest.java) holds the same lists and
enforces them; change both together. Facts and line numbers were read from `develop` at `844d91b9` on 2026-11-26.
Package names are relative to `org.nmcpye.datarun`.

## Kinds

| Kind | What it is | New code |
| --- | --- | --- |
| Retiring derived | derived from submissions, and replaced by the feed | no dependency, either way: a legacy output's failure never aborts new work, and delays it by at most one retry |
| Transitional | the outbox: stays for now, as an edge integration artifact | only through its seam |
| System of record | data and rules old code still owns | only through its seam |
| Client contract | what installed phones and the admin's tools rely on | none: adapted at the edge |
| Neutral infrastructure | configuration and shared plumbing | any use |

## New Code

| Package | What it is |
| --- | --- |
| [`submission.normalized`](../../src/main/java/org/nmcpye/datarun/submission/normalized/) | the submission normalizer: a stored submission and its form's definitions in, values by field id and row id out; pure (`NormalizedSubmissionPurityTest`) |
| [`submission.normalized.jdbc`](../../src/main/java/org/nmcpye/datarun/submission/normalized/jdbc/FormDefinitionJdbcReader.java) | `FormDefinitionJdbcReader`: every version of a form with its data element and option set ids, in one statement |
| [`submission.feed`](../../src/main/java/org/nmcpye/datarun/submission/feed/) | the feed's switch and reader role; `publisher` writes the form dictionary and the submission streams |
| [`web.v2`](../../src/main/java/org/nmcpye/datarun/web/v2/) | v2's conventions (`api-v2.md`): problems, its security chain, `ApiCaller`, ids, paging, idempotent commands |
| [`jpa.datasubmission.revision`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/revision/) | the journal: `SubmissionRevisionWriter`, and `SubmissionRevisionReader` for conflict detection |
| [`jpa.datasubmission.conflict`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/conflict/) | conflict detection, called by `SubmissionUploads` for each upload item, which gives it plain values: the stored state, the phone copy with its row id tag, the uploader's user id, and a neutral refusal kind for the move rule |

## Retiring Derived

| Structure | Code | Note |
| --- | --- | --- |
| `analytics.tall_canonical`, `analytics.events`, `analytics.dim_*`, `etl_runs` | [`etl`](../../src/main/java/org/nmcpye/datarun/etl/): the tall and events writers, their transformers, the dimensions | `etl` is also, for now, the home and name of the outbox's claim loop (Transitional) |
| schema `pivot`, `analytics.pivot_manifest`, `analytics.canonical_element_config` | [`etl/pivot`](../../src/main/java/org/nmcpye/datarun/etl/pivot/), [`PivotController`](../../src/main/java/org/nmcpye/datarun/web/rest/v1/PivotController.java) | per-form views over tall, admin route |
| the outbox's `payload`, a copy of `form_data` | written by [`DefaultDataSubmissionService.java:215`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/service/DefaultDataSubmissionService.java#L215) | no code reads it: the claim loads it ([`OutboxClaimReader.java:105`](../../src/main/java/org/nmcpye/datarun/outbox/repository/OutboxClaimReader.java#L105)), and the ETL re-reads the submission ([`EventProcessorServiceImpl.java:63`](../../src/main/java/org/nmcpye/datarun/etl/service/impl/EventProcessorServiceImpl.java#L63)) |
| `canonical_element` | [`CanonicalElement`](../../src/main/java/org/nmcpye/datarun/jpa/datatemplate/CanonicalElement.java), [`CanonicalElementRepository`](../../src/main/java/org/nmcpye/datarun/jpa/datatemplate/repository/CanonicalElementRepository.java), [`MetadataUpsertService`](../../src/main/java/org/nmcpye/datarun/jpa/datatemplate/repository/MetadataUpsertService.java), [`jpa/datatemplategenerator`](../../src/main/java/org/nmcpye/datarun/jpa/datatemplategenerator/) with `TemplateElement`, `SemanticType`, `DataType` | only the ETL and the pivots read it; publishing a form version writes it in the same call ([`DataTemplateInstanceServiceImpl.java:144`](../../src/main/java/org/nmcpye/datarun/jpa/datatemplate/service/DataTemplateInstanceServiceImpl.java#L144)) |
| `error_event` | none since 6.5.5 | table only |
| `canonical_element_anchor`, `outbox_event` | none | tables only |
| `jpa.etl` | [`package-info.java`](../../src/main/java/org/nmcpye/datarun/jpa/etl/package-info.java) | an empty package (debt: dead) |

## Transitional: The Outbox

| Structure | Code | Seam for new code | Never touch |
| --- | --- | --- | --- |
| `outbox`, `outbox_processing` | [`outbox`](../../src/main/java/org/nmcpye/datarun/outbox/); its claim loop still lives in [`etl/orchestrator`](../../src/main/java/org/nmcpye/datarun/etl/orchestrator/) and [`etl/admin`](../../src/main/java/org/nmcpye/datarun/etl/admin/) (debt: move it to `outbox.processing` and rename it) | `outbox.processing`: `ChangeOutput` and `ChangeReference` (a change as a submission id and version), `DeferredWork` (an output's work behind the live changes), `ChangeBacklog` (the changes recorded and not yet processed) | the table, `OutboxDto`, the claim and processing services |

The outbox is an edge integration artifact, and the core must not depend on it. The write path inserts every row,
beside the revision it announces ([`DefaultDataSubmissionService.java:146`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/service/DefaultDataSubmissionService.java#L146)):
the pattern's emission point and the outbox's only insert, which goes if the journal becomes the single change
record. Each row references its revision
([`20261124-outbox-revision-key.xml:32`](../../src/main/resources/config/liquibase/changelog/etl/20261124-outbox-revision-key.xml#L32)).
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
| Accounts, access scope | `app_user`, `app_authority`, `app_user_authority`, `refresh_token`, `user_group`; the access snapshot built per request | [`jpa/user`](../../src/main/java/org/nmcpye/datarun/jpa/user/), `jpa/userauthority`, `jpa/userrefreshtoken`, `jpa/usegroup`, [`security`](../../src/main/java/org/nmcpye/datarun/security/), [`jpa/accessfilter`](../../src/main/java/org/nmcpye/datarun/jpa/accessfilter/), `userdetail`, `jpa/migration` | identity: [`ApiCaller`](../../src/main/java/org/nmcpye/datarun/web/v2/caller/ApiCaller.java), through its seam (conflict detection also reads `CurrentUserDetails`, listed under Exceptions); the v2 chain resolves the token with `DomainUserDetailsService`. Scope: none (gap 4) | `CurrentUserDetails`, `SecurityUtils`, `AssignmentFormAccessService`, `UserAccessService` and its filters |
| Generic internals | none of their own | [`jpa/common`](../../src/main/java/org/nmcpye/datarun/jpa/common/), `common` (entity bases, `IdentifiableObjectService` family), `common.enumeration`, `common.repository`, `common.translation`, [`jpa/auditing`](../../src/main/java/org/nmcpye/datarun/jpa/auditing/), `service` | none | all |

Where today's seams serve new code poorly:

- Write path: every [`SubmissionCommand`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/command/SubmissionCommand.java)
  variant carries a client contract or a uid: the phone payload (`:18`, `:25`), the entity (`:32`), a uid
  (`:36`), the v1 held-decision request (`:43`, `:51`). The upload answers `EntitySaveSummaryVM`, its failures
  as `ErrorCode` text ([`SubmissionUploads.java:495`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/command/SubmissionUploads.java#L495)).
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
| Phone uploads: `bulk?referenceVersion=1`, `bulk/practice`, legacy `bulk` | [`web/rest/v1/datasubmission`](../../src/main/java/org/nmcpye/datarun/web/rest/v1/datasubmission/): resource, `DataSubmissionUploadV1Dto`, `DataSubmissionUploadV1Mapper` | the DTO is the command item, mapped inside the write path ([`SubmissionUploads.java:272`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/command/SubmissionUploads.java#L272)); held uploads are replayed from it ([`HeldSubmissionDecisions.java:85`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/command/HeldSubmissionDecisions.java#L85)) |
| Held-upload and go-live admin routes | [`web/rest/v1/intake`](../../src/main/java/org/nmcpye/datarun/web/rest/v1/intake/) | `HeldDecisionRequest` is the command's selection |
| Sync: assignments, forms, form templates and versions | [`web/rest/postgres/assignment`](../../src/main/java/org/nmcpye/datarun/web/rest/postgres/assignment/), [`web/rest/v1/formtemplate`](../../src/main/java/org/nmcpye/datarun/web/rest/v1/formtemplate/); DTOs and mappers in `jpa/assignment/{dto,mapper}`, `jpa/datatemplate/{dto,mapper}` | services return v1 DTOs ([`AssignmentService.java:24`](../../src/main/java/org/nmcpye/datarun/jpa/assignment/service/AssignmentService.java#L24)); `DataTemplateInstanceDto` is the upload's template model ([`TemplateVersionContext.java:14`](../../src/main/java/org/nmcpye/datarun/jpa/datatemplate/TemplateVersionContext.java#L14)) |
| Login, refresh, profile | [`web/rest/v1/authenticate`](../../src/main/java/org/nmcpye/datarun/web/rest/v1/authenticate/), [`web/rest/v1/account`](../../src/main/java/org/nmcpye/datarun/web/rest/v1/account/) (`CurrentUserProfileV1`) | the token's subject is the login, resolved by `DomainUserDetailsService` for v1 and v2; `ApiCaller` keeps the user id |
| Reference lists | [`web/rest/v1/referenceentry`](../../src/main/java/org/nmcpye/datarun/web/rest/v1/referenceentry/) | none |
| Inherited generic routes, query language | [`web/rest/common`](../../src/main/java/org/nmcpye/datarun/web/rest/common/), [`web/rest/postgres`](../../src/main/java/org/nmcpye/datarun/web/rest/postgres/), [`apiquery`](../../src/main/java/org/nmcpye/datarun/apiquery/) | the routes serialize entities directly as their payload ([`BaseReadResource.java:69`](../../src/main/java/org/nmcpye/datarun/web/rest/common/BaseReadResource.java#L69) to `:76`) |
| The app version, and the account's last visit | [`LastSeenFilter`](../../src/main/java/org/nmcpye/datarun/web/filter/LastSeenFilter.java) reads the phones' `X-App-Version` header ([`:25`](../../src/main/java/org/nmcpye/datarun/web/filter/LastSeenFilter.java#L25)) | it writes account data from it ([`:62`](../../src/main/java/org/nmcpye/datarun/web/filter/LastSeenFilter.java#L62)) |
| v1 errors | [`ExceptionTranslator`](../../src/main/java/org/nmcpye/datarun/web/rest/errors/ExceptionTranslator.java); codes in [`common/feedback`](../../src/main/java/org/nmcpye/datarun/common/feedback/), thrown as `ErrorCodeException` | codes are raised deep in old domain code |
| uids | every configuration table's `uid`; phones name records by uid | stored references and the access snapshot, above |
| Login as identity | `created_by`, `last_modified_by` ([`SpringSecurityAuditorAware.java:16`](../../src/main/java/org/nmcpye/datarun/security/SpringSecurityAuditorAware.java#L16)), `held_submission.submitted_by` and `decided_by` | `SubmissionActor.login`; a submission's creator is compared by login ([`StoredSubmissionGuard.java:71`](../../src/main/java/org/nmcpye/datarun/jpa/datasubmission/validation/StoredSubmissionGuard.java#L71)) |

## Neutral Infrastructure

`config` (activation switches, the JWT converter, properties), `web.filter` (request ids), `aop`,
`management`, the application class, and `FailureShape`, `DatabaseFailures`, `CodeGenerator` (new ids with
`nextUlid`; `generateUid` mints phone uids), `UuidUtils` and `LatinDigits`.

## Seams

| Seam | Used from | For |
| --- | --- | --- |
| `SubmissionCommandService`, `SubmissionActor` | any new code | the single write path; no command variant is a seam until one carries no client contract and no uid |
| `ValueType` | any new code | the stored value-type vocabulary in the normalizer's definitions |
| `CurrentUserDetails` | `web.v2.caller` | `ApiCaller` keeps the user id and authorities |
| `DomainUserDetailsService` | `web.v2.config` | the v2 chain authenticates as v1 does |
| `outbox.processing` | any new code | the outputs' seam: `ChangeOutput`, `ChangeReference`, `DeferredWork`, `ChangeBacklog` |
| SQL: `data_template`, `data_template_version`, `data_element`, `option_set` | `FormDefinitionJdbcReader` | the normalizer's form definitions, and a form's and a version's ids by uid |
| SQL: `data_submission` | the journal package | the journal copies the row it records |
| SQL: `data_submission` | `submission.normalized.jdbc.StoredSubmissionJdbcReader` | stored submissions by id and in batches |
| SQL: `assignment`, `team`, `org_unit`, `activity` | `submission.normalized.jdbc.PlacementJdbcResolver` | one placement resolver, turning placement uids into ids |

Engineering planned the last two (2026-11-26); the feed built them.

## Exceptions

| Class | Depends on | Until |
| --- | --- | --- |
| [`OutboxClaimServiceImpl`](../../src/main/java/org/nmcpye/datarun/outbox/service/impl/OutboxClaimServiceImpl.java) | `etl.config.EtlProperties` (its claim lease) | the claim loop leaves `etl` |
| [`OutboxProcessingServiceImpl`](../../src/main/java/org/nmcpye/datarun/outbox/service/impl/OutboxProcessingServiceImpl.java) | `etl.service.EtlRunService` (outcomes counted on the ETL's run) | the claim loop leaves `etl` |
| [`DataTemplateInstanceServiceImpl`](../../src/main/java/org/nmcpye/datarun/jpa/datatemplate/service/DataTemplateInstanceServiceImpl.java) | `TemplateElementGeneratorService`: publishing a form version writes `canonical_element` in the same call ([`:144`](../../src/main/java/org/nmcpye/datarun/jpa/datatemplate/service/DataTemplateInstanceServiceImpl.java#L144)) | `canonical_element` goes |

Known and unguarded: [`CacheConfiguration.java:139`](../../src/main/java/org/nmcpye/datarun/config/CacheConfiguration.java#L139) names
`RefTypeValueResolutionService.REF_RESOLUTION_CACHE_NAME`, a constant the compiler inlines, so no rule sees this use
of `etl`.

Uids in new code and `outbox.processing` (owner, 2026-11-26: new work keys by ids, and the 11-character uid is an
alias for installed phones only). An edge adapter turns the stored uids into ids; a breach keys by uid today.

| Class | Members | Kind | Why |
| --- | --- | --- | --- |
| `PlacementJdbcResolver.StoredPlacement` | `assignmentUid`, `teamUid`, `orgUnitUid`, `activityUid` | edge | the placement uids a revision stores, which the resolver turns into ids |
| `FormDefinitionJdbcReader` | `ids(formUid, versionUid)` | edge | turns the form and version uids a stored submission names into ids |
| `FormVersionDefinition` | `parse(fieldIdForUid, optionSetIdForUid)` | edge | turn the data element and option set uids a template's fields name into ids |
| `StoredSubmission` | `formUid`, `formVersionUid` | breach | the stored form and pinned version reach the normalizer as uids, though the reader can resolve their ids |
| `FormVersionDefinition` | `uid`, `parse(uid)` | breach | a version is known by its uid, to match the stored pin |
| `FormDefinition` | `versionsByUid`, `resolutionsByVersionUid`, `versionByUid(uid)` | breach | versions are looked up by uid |
| `FormDefinitionJdbcReader` | `read(formUid)`, `read(formUids)`, `changeTokens(formUids)`, SQL `form_uid`, `version_uid` | breach | definitions and change tokens are keyed by form uid, and versions by uid |
| `StoredSubmissionJdbcReader` | SQL `template_uid`, `form_uid`, `template_version_uid`, `form_version_uid`; `HeadVersion.formUid` | breach | hands the stored form and version uids on untranslated |
| `FormDefinitions` (the feed) | `forHead(formUid, pinnedVersionUid)` | breach | the feed keeps form definitions by form uid |
| `SubmissionRevisionReader` | SQL `assignment_uid`, `template_version_uid` | breach | conflict detection compares the journal's assignment and form version by uid |

## Gaps

Submission review (`docs/features/submission-review/spec.md`) needs these; the feed built parts of 1 and 4, the rest is not built.

| # | Need | Systems of record | Seam to build | Today |
| --- | --- | --- | --- | --- |
| 1 | Search, view and history: submissions by filters and by id | `data_submission`, journal | `StoredSubmissionJdbcReader` (built: by id and in batches); search by filters under the search guard still to add. It reads `journal.submission_revision` too, and `SubmissionWriteOwnershipTest`'s `NAMED_IN` ([`:124`](../../src/test/java/org/nmcpye/datarun/SubmissionWriteOwnershipTest.java#L124)) names it | the journal's `SubmissionRevisionReader` reads `data_submission` on its own |
| 2 | Values typed for a form named by id; edits written back | form definitions | a read by form id, and the write direction (field id and row id to the stored place), in `submission.normalized` | uid-keyed reader; no write direction |
| 3 | Edits, moves, splits, deletes and restores | `data_submission`, journal | write-path commands for new code: `SubmissionCommand` variants keyed by submission id, with domain types and the journal's reason and detail, run in v2's idempotent transaction, answering a result of their own | every variant carries a client contract or a uid |
| 4 | A move's destination rules; scope by activity, team, area | configuration, access scope | `PlacementJdbcResolver` (built: uids to ids); an assignment's forms and deleted state, and org unit ancestry, still to add. Review's own grants are new code | entities and v1 DTOs keyed by uid; the snapshot in uids |
| 5 | View's entries; history's actor names | `reference_entry`, `app_user` | small readers by id | none |
