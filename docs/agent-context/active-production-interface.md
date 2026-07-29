# Active Production Interface

Role: released mobile-to-server HTTP contract and current-branch server
ownership map

Status: LIVING CANDIDATE - transition owners are not deployed

The deployed server remains `v6.4.1`. This document preserves the released
mobile wire contract while mapping its owner on the transition candidate
branch. Use `initial-event-transition-boundary.md` for the split between
deployed authority, candidate authority, compatibility projections, and
bootstrap requirements.

This map follows the strict comment-out test: `CORE-ACTIVE` means removing the
path without replacement breaks a released mobile capability. Reachable
administration, maintenance, compatibility, and gated code is classified
separately rather than being treated as equally active.

## Authentication And HTTP Gate

| Status | Route | Contract |
| --- | --- | --- |
| CORE-ACTIVE | `POST /api/v1/authenticate` | released mobile login; returns access and refresh tokens |
| CORE-ACTIVE | `POST /api/v1/refresh` | released mobile refresh-token rotation; invalid, expired, unavailable, or inactive users receive `401` |
| CORE-ACTIVE | `GET /api/v1/myDetails` | authenticated released-mobile profile |
| PUBLIC STATUS | `GET /api/v1/authenticate`, `GET /api/authenticate`, health and info management routes | no user data; retained compatibility/status surface |
| ADMIN | `/api/v1/admin/**`, `/api/admin/**`, `/api/custom/admin/**`, and both account-creation aliases | current database `ROLE_ADMIN` is required |
| DEPRECATED COMPATIBILITY | `/api/custom/**` | authenticated or administrator-gated alias; not used by the released mobile and not a target API |

JWT claims identify the subject, but request authority comes from the current
activated database user and current database authorities. A stale token claim
cannot preserve administrator access after the database role changes.
Deactivated users cannot receive or rotate tokens. All routes outside the
explicit allowlist fail closed.

HTTP Basic and public Prometheus access remain baseline operational
compatibility pending evidence from the administrator and monitoring smoke.
They are not canonicalized by this transition.

## Assignment

| Status | Released mobile request | Server owner | Required downstream path |
| --- | --- | --- | --- |
| CORE-ACTIVE | `GET /api/v1/assignments?paged=false` | `AssignmentV1Resource` | `ReleasedWorkReadAuthority` -> `LatestAssignmentGrantReader` -> validated assignment projection |
| CORE-ACTIVE | `GET /api/v1/assignments/forms?paged=false&referenceVersion=1` | `AssignmentV1Resource.getAllDto` | the same released work scope -> event-grant form UIDs -> `ReferenceAssignmentFormGate` |
| CORE-ACTIVE | `GET /api/v1/orgUnits?paged=false` | `OrgUnitV1Resource` | the same released work scope -> direct grant org units -> existing ancestor projection |
| GATED | `GET /api/v1/assignments/{uid}/referenceEntries` | `ReferenceEntryResource` | the same released work scope -> active grant Reference capability -> grant org-unit catalog; deployed but unused until a Reference form is assigned |
| SUPPORTING-REACHABLE | `GET /api/v1/formPermissions` | `UserFormPermissionsResource` | the mobile synchronizes this into `user_form_permissions`, but no active mobile behavior reads that table; retire mobile registration/table first, then this endpoint |

The released mobile owners are `AssignmentDatasource` and
`ReferenceEntryDatasource`. Assignment synchronization persists assignments
and replaces the local assignment-form projection only after its secondary
request succeeds.

## Authorization Ownership

- `User.authorities` supplies Spring authentication authorities and the
  administrator flag.
- `CurrentUserDetailsService` rebuilds one request-scoped authorization
  snapshot from enabled direct teams, their enabled managed teams, activities,
  and team-scoped form grants. It is intentionally not cached across requests;
  access changes must not depend on four independently invalidated caches.
- `CurrentUserProfileV1` adapts that principal to the released
  `/api/v1/myDetails` response. Legacy count and `userGroupsUIDs` fields are
  `/api/v1` wire compatibility only and retire when older supported mobile
  clients no longer require that profile shape.
- `ResourceApiAuthorization` preserves the coarse inherited-resource gate:
  administrators or users with a team may read; only administrators may use
  generic writes. It does not decide entity visibility or form permissions and
  retires as the inherited routes receive domain owners or are removed.
- `UserAccessService` and registered access filters constrain entity reads.
  Its creator-only fallback is a compatibility policy for unclassified generic
  resources. Static service/filter comparison currently limits that fallback
  to data-element group/set and org-unit group/set generic surfaces; each must
  exit through an explicit domain filter or route removal.
- `ReleasedWorkReadAuthority` is the field-user authority for released V1
  assignment, assignment-form, organization-unit, and Reference reads. It
  consumes one request-current highest-generation event snapshot and validates
  the required baseline wire projection. Baseline filters cannot widen it.
- `AssignmentFormAccessService` remains the baseline owner for separately
  preserved custom/generic reads. On versioned upload it is used only by
  `BaselineVersionedUploadCompatibilityAdapter` to preserve released denial
  codes and narrow the explicit retired-assignment compatibility case.
- `VersionedUploadEventAuthorizer` is the acceptance owner for released
  versioned field-user uploads. It batch-reads request-current event grants,
  accepts an exact active or eligible ended generation, and fails closed when
  event authority is unavailable or contradicts baseline compatibility.
- Assignment authority is derived from immutable assignment facts. The
  isolated assignment replay validates or rebuilds
  `assignment_grant_projection` using only the journal, immutable identity
  links, and immutable role definitions; baseline assignment/team/permission
  rows are not replay inputs.
- Organization-unit sync is scoped to direct-team assignments and their
  ancestors. Managed teams remain active mobile selector/summary data, but
  managed-team assignments are not synchronized and do not expand org-unit
  visibility.
- Two production-clone assignments with no canonical capture forms remain
  visible only through `EmptyCaptureReadCompatibilityAdapter`. They expose no
  forms and grant no Reference or upload authority. This adapter retires when
  those source rows are removed/corrected or their product behavior is
  explicitly defined.
- `/api/custom` and inherited generic assignment/org-unit/Reference reads
  remain baseline compatibility surfaces. They are not a second owner for the
  released V1 work graph.
- Legacy role, privilege, and Spring ACL tables have no source policy owner and
  remain schema-only until a bounded Liquibase contraction.
- User groups do not participate in the active authentication or work-scope
  decision. Their CRUD and schema surface remains a separate removal decision.

## Form Templates

| Status | Released mobile request | Server owner | Required downstream path |
| --- | --- | --- | --- |
| CORE-ACTIVE | `GET /api/v1/formTemplates?paged=false` | `FormTemplateResource` inherited read route | `DefaultDataTemplateService.findAllByUser` -> `FormTemplateFilter` -> `DataTemplateRepository` |
| CORE-ACTIVE | `GET /api/v1/formTemplateVersions?paged=false` | `TemplateVersionResource` inherited read route | `DefaultTemplateVersionService.findAllByUser` -> access-filtered template masters -> `TemplateVersionRepository` |

The released mobile owner is `DataFormTemplateDatasource`. The form-template
cleanup pass must trace the filters, services, repositories, template
processing, and pinned-version resolution behind these two reads before
removing similar-looking form APIs.

`/api/v1/dataFormTemplates` is the separate operational authoring boundary.
`FormTemplateAuthoringResource` validates and processes the complete template,
then `DataTemplateInstanceService.publishVersion` creates one immutable version,
updates the template's latest-version pointer, and invokes downstream
canonical projection generation exactly once. The stored template
version remains the product/form-runtime contract; generated element metadata
supports extraction and is not a second template authority.

`canonical_element` is the projection metadata consumed by the active ETL and
pivot/export path, including nested-repeat ancestry and option-set identity.
The duplicate `template_element` writer, JPA entity ownership, and cache were
removed after confirming there was no source, mobile, SQL, or declared external
reader. Its physical table remains schema-only until a separately tested
Liquibase contraction.

`DataElement` remains operational authoring input for stable field identity,
name/code, and value type. Option-set UID is a template-field property and is
resolved into canonical projection metadata downstream; template processing
does not copy it from `DataElement`. Data-element groups and group sets have no
product consumer and their source routes, services, repositories, and JPA
relations are removed. Their physical tables are schema-only pending a separate
Liquibase contraction.

## Submission

| Status | Route | Server owner |
| --- | --- | --- |
| CORE-ACTIVE | `POST /api/v1/dataSubmission/bulk?referenceVersion=1` | `DataSubmissionResource.saveVersionedUpload` -> `SubmissionUploadService` -> `VersionedUploadEventAuthorizer` -> `DefaultDataSubmissionService.upsertAll` |
| LEGACY-RISK / UNKNOWN | `GET /api/v1/dataSubmission`, `GET /byLastModified`, `POST /query`, `GET /{id}` | inherited generic read surface |
| LEGACY-RISK / UNKNOWN | `POST /api/v1/dataSubmission/bulk` without the version parameter, `POST /`, `POST /return` | inherited/overridden compatibility write surface |
| LEGACY-RISK / UNKNOWN | `GET|POST /api/v1/dataSubmission/objects` | deprecated flattened read surface |
| ADMIN / UNKNOWN | `DELETE /api/v1/dataSubmission/{id}`, `PUT /{uid}` | inherited/overridden admin surface |

The released mobile owner is `SubmissionUploadService`. Ordinary and
Reference-capable submissions currently share this versioned upload boundary.
The active upload maps the versioned DTO; resolves assignment and pinned
template once through `TemplateVersionResolver`; canonicalizes server-owned
assignment/template context;
authorizes the canonical assignment/form pair from the actor's latest
event-backed grant; generates missing repeat metadata for compatibility;
resolves Reference definitions; upserts whole submission JSON; and writes the
current `outbox` row in the same transaction. One immutable event snapshot is
read per bulk request. Baseline access cannot turn an event denial into
acceptance; it only preserves the released `E4114`/`E1112` distinction and the
exact soft-deleted-assignment late-upload case.
`DataSubmissionService` receives canonical submissions and a result summary,
not a security principal; authorization must complete before persistence.

The unversioned generic writes still use the separate validator/enrichment
pipeline in `DataSubmissionResource.preProcess`. They are not used by the
released mobile and remain `LEGACY-RISK` pending the recorded endpoint-use
decision; their reachability does not make them a second product authority.

The disabled submission-history listener and its zero-caller processor/model
alternatives are source-dead. Their physical table remains a schema concern.
The separate `jpa/datasubmissionoutbox` tree targets the legacy
`outbox_event` table; it has no active producer or enabled worker and is not
the current `outbox` owner.

Mixed update/delete batches persist their classified collections separately
and emit `UPDATE` and `DELETE` through the current outbox; this boundary has
a focused regression test. An accepted same-UID request whose mutable
persisted state is already exact returns success in `updated` without a
repository write, audit/version change, or outbox row. Real changes and first
delete transitions retain their existing writes. Capture bootstrap facts,
identity links, and the reconstructible current-event projection are
transition-only comparison/replay state. When
`datarun.transition.capture-live-shadow-enabled=false`, the released upload
does not read or append capture facts. When separately enabled after
bootstrap/replay, actual mutations append one fact and advance one pointer in
the same baseline transaction. They are still not released production
authority.
Submission pulling, inherited CRUD/read routes,
deprecated `objects`, and the admin delete route still require independent
API-use classification.

The upload-time `MigrationRepeatIdGenerator` is core-active. The separate
migration-error entity, repository, service, and skip/listener chain had no
registered batch job or caller and is source-dead; its physical table remains
a later schema-migration concern.

## Cleanup Cycle

For each domain: map contract, remove source-dead alternatives, reconcile
duplicated active ownership, assess unused APIs, then handle schema residue in
a separate Liquibase migration. Close each step with focused tests, the full
test gate, and the smallest applicable mobile smoke.

## Pending Endpoint Removal Decisions

Status: **WAITING FOR USER CONFIRMATION**. These routes remain registered.
Absence from the released mobile and this repository is evidence, not proof
that no external operator or older client uses them.

| Candidate | Proposed action | Evidence | Confidence / compatibility risk |
| --- | --- | --- | --- |
| `POST /api/{v1,custom}/assignments/forms` | retain GET; remove POST method registration | released mobile uses GET; no in-repo POST caller; both methods currently run the same read handler | high / older external client unknown |
| inherited assignment writes: `POST`, `POST /bulk`, `POST /return`, `PUT /{uid}`, `DELETE /{id}` | remove write surface | assignment synchronization is read-only; no in-repo caller | medium / possible manual admin use |
| `GET /api/{v1,custom}/assignments/updatePaths` | assess manual use, then remove or restrict to the maintenance owner | no mobile/in-repo caller; path maintenance also has a service/scheduled owner | medium / operator use unknown |
| `GET /api/{v1,custom}/teams/managed` | remove after external-client confirmation; then retire `managedTeamsUIDs` from the security principal and `/api/v1` profile | released mobile gets managed teams embedded in direct-team sync and never calls this route | high / external admin client unknown |
| inherited `/api/{v1,custom}/userGroups` CRUD | remove source, then handle tables in a separate Liquibase cutover | no released-mobile call or active authentication/access decision uses user groups | high / external admin client unknown |
| inherited `formTemplates` writes | remove; retain `dataFormTemplates` as the operational authoring boundary | mobile reads only; full-template authoring has a separate validated/versioned endpoint | high / external direct writer unknown |
| inherited `formTemplateVersions` writes | remove | mobile reads only; controller overrides save with a no-op, so POST routes misleadingly report without persisting | high / clients may rely on broken behavior |
| generic assignment/form reads `/byLastModified`, `/query`, and `/{id}` | remove only after access-log/operator confirmation | no released-mobile or in-repo caller | medium / external reads unknown |
| `GET|POST /api/{v1,custom}/dataSubmission/objects` | remove | deprecated flattened-read endpoint; no released-mobile or in-repo caller | high / older reporting client unknown |
| generic submission reads `GET /`, `/byLastModified`, `POST /query`, `GET /{id}` | remove only after access-log/operator confirmation | submission pull is disabled; no current mobile caller | medium / older pull/reporting client unknown |
| unversioned submission writes `POST /bulk`, `POST /`, `POST /return` | retain until old-client compatibility is explicitly retired | current mobile uses `bulk?referenceVersion=1`; older clients may use the unversioned payload | low removal confidence / highest client risk |
| submission `PUT /{uid}` and `DELETE /{id}` | defer to synced edit/delete policy | no current mobile caller; admin-only route exists, but lifecycle policy is incomplete | low removal confidence / product-policy risk |
| `/api/custom` aliases | replace remaining administrator/operational callers with `/api/v1`, then remove | released mobile uses `/api/v1`; recent production access logs still contain successful custom calls | medium / caller must be identified during administrator smoke |
