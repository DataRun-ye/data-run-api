# Bounded Reference Server Boundary

Role: implemented server contract and activation boundary

Status: ACTIVE IN PRODUCTION FOR ITN 2026 (server 6.5.0, app 6.1.0); CATALOG
LOADED 2026-11-19 THROUGH `scripts/reference`

The durable end-to-end product contract is owned by the mobile repository's
[bounded Reference plan](https://github.com/DataRun-ye/data-run-mobile/blob/develop/docs/agent-context/10-bounded-reference-field-plan.md).
Mutable cross-repository status belongs in
[DataRun API #34](https://github.com/DataRun-ye/data-run-api/issues/34).
The server foundation was deployed in `v6.4.0`. Activation status remains in
[DataRun API #34](https://github.com/DataRun-ye/data-run-api/issues/34).

## Implemented Server Surface

- `09b9b27d` adds the neutral Reference catalog, assignment/form compatibility
  gate, access-scoped paginated reads, and additive Liquibase changeset.
- `8fa1d7ac` adds template-aware Reference extraction and resolution to the V1
  submission upload boundary.
- `reference_entry` stores canonical UID, display name, organization unit,
  optional first-registration activity, and standard audit fields.
- Existing non-Reference assignment responses and submission payloads remain
  on their existing paths.

Active implementation evidence:

```text
assignment forms request
-> AssignmentResource
-> DefaultAssignmentService
-> ReferenceAssignmentFormGate

Reference catalog request
-> ReferenceEntryResource
-> ReferenceEntryV1ServiceImpl
-> ReferenceEntryRepository

Reference submission upload
-> DataSubmissionResource
-> SubmissionUploadService
-> ReferenceValueExtractor / ReferenceSubmissionResolver
-> existing DataSubmissionService transaction
```

The migration is
`src/main/resources/config/liquibase/changelog/reference-entry/20260725-create-reference-entry.xml`
and is included by `master.xml`.

Since `v6.5.0` the versioned route (`referenceVersion=1`, apps 6.0.2+) uses
`SubmissionUploadService`, the legacy `/bulk` route keeps its own validation in
`DataSubmissionResource`, catalog access is scoped to the assignment, and both
routes save each submission in its own transaction, reporting failures per row
(api #37). The versioned route also applies go-live rules (api #42): an upload
held before a go-live, or sent from a practice session, creates no Reference
entries until an admin releases it. The legacy route is never held.

## Compatibility Boundary

- A missing `referenceVersion` is version 0 and excludes assignment forms
  whose latest template contains a Reference element.
- `referenceVersion=1` enables those forms only when ordinary assignment and
  access checks also pass.
- The Reference bulk upload path is selected explicitly with
  `referenceVersion=1`; ordinary V1 uploads retain the existing path.
- Reference definitions are request-only data. A selected field value remains
  one UID in submission `formData`.
- Assignment activity and organization-unit mutation is rejected when it
  would invalidate a Reference-enabled assignment scope.

Production state, measured on the 2026-11-16 production clone: activity
`xuWrotiHywD` (ITN 2026) has 13,861 village assignments created 2026-11-10/11,
all carrying `Tcf3Ks9ZRpB`, whose versions 28 and 29 contain one Reference
field. They were assigned without the import and test-assignment steps of #34.
Consequences: clients that do not send `referenceVersion=1` (mobile below
6.0.2) do not receive the form, and those assignments now reject scope changes
(`E1123`).

## Catalog Import

- `scripts/reference/extract-households.py` turns one household sheet into a
  CSV; `scripts/reference/load-households.sh` loads it for one activity's
  villages. Dry run by default, staging unless the production flag is set. See
  `scripts/reference/README.md`.
- Loaded rows carry `created_by = 'reference-import'` and no first-registration
  activity. Their uids derive from the source `house_uuid`, so re-runs add only
  new households.
- Never move an entry to another organization unit: phones that hold it fail
  their sync with a scope conflict. Never delete an entry phones may hold:
  phones resend its definition, and the server recreates it or rejects the
  upload. Renames are safe and reach phones on their next sync.

## Verification Evidence

Focused tests cover:

- old/new client assignment-form gating and ordinary-form preservation;
- assignment scope mutation rejection;
- access-scoped catalog paging;
- known, new, retry, wrong-scope, stale-name, malformed, and missing
  definitions;
- legacy payload mapping and the explicit Reference route;
- complete rollback of catalog/submission work on failure.

Production `v6.4.0` passed health, release-identity, login, and configuration
smoke checks after the additive migration.

Staging, 2026-11-17 (production image, 2026-11-16 production clone): the import
loaded 162,610 households on 9,287 of the 13,860 ITN 2026 villages; re-runs
inserted nothing. As a linked field user, a `referenceVersion=1` client saw the
form on its 20 team assignments and an old client on none, and the catalog
endpoint returned each village's full list.

Production, 2026-11-19 (after `v6.5.0`): the same import loaded 162,610
households on 9,287 villages.
