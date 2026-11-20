# Active Production Interface

Role: released mobile-to-server HTTP contract and active server ownership map

Status: LIVING

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
| CORE-ACTIVE | `GET /api/v1/assignments?paged=false` | `AssignmentResource` inherited read route | `DefaultAssignmentService.findAllByUser` -> `AssignmentFilter` -> `AssignmentRepository` |
| CORE-ACTIVE | `GET /api/v1/assignments/forms?paged=false&referenceVersion=1` | `AssignmentResource.getAllDto` | access-filtered assignments -> `AssignmentWithAccessMapper` -> `AssignmentFormAccessService` -> `ReferenceAssignmentFormGate` |
| GATED | `GET /api/v1/assignments/{uid}/referenceEntries` | `ReferenceEntryResource` | `AssignmentService.findAccessibleByIdOrUid` and assignment/form access checks; deployed but unused until a Reference form is assigned |
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
  and team-scoped form grants. A team's grants also apply to the enabled teams
  it manages (also when the user belongs to both), so a supervisor's team
  grants reach his teams' assignments. It is
  intentionally not cached across requests; access changes must not depend on
  four independently invalidated caches.
- `CurrentUserProfileV1` adapts that principal to the released
  `/api/v1/myDetails` response. Legacy count and `userGroupsUIDs` fields are
  V1 wire compatibility only and retire when older supported mobile clients no
  longer require that profile shape.
- `ResourceApiAuthorization` preserves the coarse inherited-resource gate:
  administrators or users with a team may read; only administrators may use
  generic writes. It does not decide entity visibility or form permissions and
  retires as the inherited routes receive domain owners or are removed.
- `UserAccessService` and registered access filters constrain entity reads.
  Its creator-only fallback is a compatibility policy for unclassified generic
  resources. Static service/filter comparison currently limits that fallback
  to data-element group/set and org-unit group/set generic surfaces; each must
  exit through an explicit domain filter or route removal.
- `AssignmentFormAccessService` is the shared authorization owner for the
  actor, assignment team, assigned form, and requested form action.
- Assignment and organization-unit sync cover direct-team assignments and,
  for app 6.1.2 and later (`X-App-Version`), the managed teams' assignments
  that carry a form the user may submit there (a grant with add or edit;
  `ManagedAssignmentScope`, one id-only query), with org-unit ancestors. Older
  apps receive none: one such assignment stops them listing the activity.
  Other managed-team assignments are not synchronized. Managed teams also
  remain mobile selector/summary data.
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
| CORE-ACTIVE | `POST /api/v1/dataSubmission/bulk?referenceVersion=1` | `DataSubmissionResource.saveVersionedUpload` -> `SubmissionUploadService` -> `DefaultDataSubmissionService.upsertAll` |
| CORE-ACTIVE (app 6.1.0+) | `POST /api/v1/dataSubmission/bulk/practice?referenceVersion=1` | `DataSubmissionResource.savePracticeUpload` -> `SubmissionUploadService` (practice: always held) |
| ADMIN | `GET`/`PUT /api/v1/admin/goLiveRules`, `DELETE /{uid}`, `GET /effective` | `GoLiveRuleResource` -> `GoLiveRuleService` -> `GoLiveRuleRepository` (`go_live_rule`) |
| ADMIN | `GET /api/v1/admin/heldSubmissions`, `GET /summary`, `GET /{uid}`, `POST /release`, `POST /discard` | `HeldSubmissionResource` -> `HeldSubmissionReview` / `HeldSubmissionRepository` (`held_submission`) |
| LEGACY-RISK / UNKNOWN | `GET /api/v1/dataSubmission`, `GET /byLastModified`, `POST /query`, `GET /{id}` | inherited generic read surface |
| LEGACY-RISK / UNKNOWN | `POST /api/v1/dataSubmission/bulk` without the version parameter, `POST /`, `POST /return` | inherited/overridden compatibility write surface |
| LEGACY-RISK / UNKNOWN | `GET|POST /api/v1/dataSubmission/objects` | deprecated flattened read surface |
| ADMIN / UNKNOWN | `DELETE /api/v1/dataSubmission/{id}`, `PUT /{uid}` | inherited/overridden admin surface |

The released mobile owner is `SubmissionUploadService`. Ordinary and
Reference-capable submissions currently share this versioned upload boundary.
The active upload maps the versioned DTO; resolves assignment and pinned
template once through `TemplateVersionResolver`; canonicalizes server-owned
assignment/template context;
authorizes the canonical assignment/form pair; generates missing repeat
metadata for compatibility; resolves Reference definitions; upserts whole
submission JSON; and writes the current `outbox` row in the same transaction.
Submission access and assignment-form projection use the same
`AssignmentFormAccessService`; permissions from another team and forms absent
from the assignment are rejected. A manager may submit on a managed team's
assignment with his own team's grant; the submission keeps the assignment's
team.
`DataSubmissionService` receives canonical submissions and a result summary,
not a security principal; authorization must complete before persistence.

After authorization, `SubmissionIntake` decides per submission whether it is
saved or held (DataRun-ye/data-run-api#42): practice uploads are always held,
and a new submission started before the go-live rule that applies to it
(`GoLivePolicy`: the nearest org unit first, a form rule before that level's
rule for every form; the earlier of phone start and server receipt) is held.
A held submission runs the same checks, Reference resolution and upsert, then
its transaction is rolled back and the upload is stored as received in
`held_submission`, so nothing reaches `data_submission`, the outbox or the
Reference lists. An admin release replays it through this same path as its
original submitter; a held copy of a uid already in the real data is marked
superseded. A release by filter takes only uploads still held before a
go-live; discarded and practice uploads are released only by uid. Releases
and discards work in batches (at most 500 per call, reporting what remains),
and every decision applies only if the upload is unchanged since it was read;
released and superseded uploads are final. The area chain is read by walking
parents, never through `OrgUnit.getPath()`, which writes the org unit when
called in a committing transaction. The unversioned legacy writes are never
held. The connection pool does not auto-commit, so JDBC writes outside a
transaction are lost: intake writes are `@Transactional`. Never run the
Liquibase rollback of `held_submission` once uploads were held: it would
delete uploads that phones report as sent.

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
a focused regression test. Submission pulling, inherited CRUD/read routes,
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
| `GET /api/{v1,custom}/teams/managed` | remove after external-client confirmation; then retire `managedTeamsUIDs` from the security principal/V1 profile | released mobile gets managed teams embedded in direct-team sync and never calls this route | high / external admin client unknown |
| inherited `/api/{v1,custom}/userGroups` CRUD | remove source, then handle tables in a separate Liquibase cutover | no released-mobile call or active authentication/access decision uses user groups | high / external admin client unknown |
| inherited `formTemplates` writes | remove; retain `dataFormTemplates` as the operational authoring boundary | mobile reads only; full-template authoring has a separate validated/versioned endpoint | high / external direct writer unknown |
| inherited `formTemplateVersions` writes | remove | mobile reads only; controller overrides save with a no-op, so POST routes misleadingly report without persisting | high / clients may rely on broken behavior |
| generic assignment/form reads `/byLastModified`, `/query`, and `/{id}` | remove only after access-log/operator confirmation | no released-mobile or in-repo caller | medium / external reads unknown |
| `GET|POST /api/{v1,custom}/dataSubmission/objects` | remove | deprecated flattened-read endpoint; no released-mobile or in-repo caller | high / older reporting client unknown |
| generic submission reads `GET /`, `/byLastModified`, `POST /query`, `GET /{id}` | remove only after access-log/operator confirmation | submission pull is disabled; no current mobile caller | medium / older pull/reporting client unknown |
| unversioned submission writes `POST /bulk`, `POST /`, `POST /return` | retain until old-client compatibility is explicitly retired | current mobile uses `bulk?referenceVersion=1`; older clients may use the unversioned payload | low removal confidence / highest client risk |
| submission `PUT /{uid}` and `DELETE /{id}` | defer to synced edit/delete policy | no current mobile caller; admin-only route exists, but lifecycle policy is incomplete | low removal confidence / product-policy risk |
| `/api/custom` aliases | replace remaining administrator/operational callers with `/api/v1`, then remove | released mobile uses `/api/v1`; recent production access logs still contain successful custom calls | medium / caller must be identified during administrator smoke |
