# Bounded Reference Server Boundary

Role: implemented server contract and activation boundary

Status: IMPLEMENTED ON `develop`; PRODUCTION DEPLOYMENT AND ACTIVATION PENDING

Validated: 2026-07-25 at `0d83c580`

The durable end-to-end product contract is owned by the mobile repository's
[bounded Reference plan](https://github.com/DataRun-ye/data-run-mobile/blob/develop/docs/agent-context/10-bounded-reference-field-plan.md).
Mutable cross-repository status belongs in
[DataRun API #34](https://github.com/DataRun-ye/data-run-api/issues/34).
The server-owned deployment task is
[DataRun API #35](https://github.com/DataRun-ye/data-run-api/issues/35).

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
-> ReferenceSubmissionUploadService
-> ReferenceValueExtractor / ReferenceSubmissionResolver
-> existing DataSubmissionService transaction
```

The migration is
`src/main/resources/config/liquibase/changelog/reference-entry/20260725-create-reference-entry.xml`
and is included by `master.xml`.

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

No Reference form is assigned in production, and the server commits above are
not deployed. Do not infer activation from source reachability.

## Verification Evidence

Focused tests cover:

- old/new client assignment-form gating and ordinary-form preservation;
- assignment scope mutation rejection;
- access-scoped catalog paging;
- known, new, retry, wrong-scope, stale-name, malformed, and missing
  definitions;
- legacy payload mapping and the explicit Reference route;
- complete rollback of catalog/submission work on failure.

Production closure remains the work in issue #35: validate the migration
against the production clone, deploy while unassigned, and smoke existing
client/configuration/submission behavior. Catalog import and test-assignment
activation happen only after that closes.
