# Current Work

Updated: 2026-07-28

Status: ACCEPTED FOR IMPLEMENTATION

## Released Work-Read Event Authority Cutover

### Outcome

Make current event-backed assignment grants the authorization authority for
the released field-user work reads:

```text
GET /api/v1/assignments?paged=false
GET /api/v1/assignments/forms?paged=false&referenceVersion=1
GET /api/v1/orgUnits?paged=false
GET /api/v1/assignments/{assignmentUid}/referenceEntries
```

These reads form one mobile offline-configuration graph. Cut them over
together so assignment rows, eligible forms, organization-unit scope, and
Reference catalog access cannot disagree.

Keep the released response DTOs, paging/query behavior, administrator access,
Reference capability gate, local replacement semantics, and persistence
unchanged. This slice has no mobile, schema, bootstrap, or production
deployment change.

### Evidence Gate Already Closed

- Assignment bootstrap and command ownership are complete.
- All five assignment-scope consumers passed shadow comparison on the
  isolated production clone.
- Versioned upload is already event-authorized.
- The release gate passes with 188 unit/contract and 38 integration tests.
- The restored clone contains 263,423 exact baseline/event authority tuples
  with no unexplained differences.
- Two baseline-visible assignments for one actor have no canonical capture
  forms. They are not grants and are the only verified read compatibility
  case.
- Production has not been connected or changed by this transition work.

### Selected Owners

Introduce one shared current-grant reader over
`AssignmentCaptureEventReadPort`. It must:

- read one immutable event snapshot for the request;
- require the exact completed bootstrap checkpoint;
- select only the highest generation for each assignment;
- reject duplicate or contradictory latest generations as authority
  unavailable;
- distinguish an absent actor alias from unavailable authority;
- expose current active grants separately from ended history.

`VersionedUploadEventAuthorizer` must use this shared latest-generation owner
without changing its accepted active/retired behavior. Do not leave a second
generation-selection implementation in upload.

Introduce one released work-read owner that consumes the shared snapshot:

- administrators retain the released bypass and perform no event read;
- a field user sees only latest `ACTIVE` grants;
- an absent actor alias produces an empty field-user result;
- missing checkpoint, failed reads, or invalid event state fail closed as
  service unavailable;
- baseline assignment/team/form permission logic cannot widen an event result.

The owner is request-current. Do not put grants in JWT claims, cross-request
caches, or another persistent projection.

### Query And Paging

For assignment and assignment-form reads, calculate the authorized assignment
UIDs before the repository query and intersect them with the existing
`QueryRequest`/JSON filters before paging. Filtering a page after retrieval is
not acceptable because it changes page size, totals, and next links.

Client filters may narrow the authorized set. They must never widen it.

### Surface Behavior

#### Assignment list

Return projection rows for the latest active event grants. The assignment JPA
row remains the released wire projection; event state decides whether the row
is visible.

#### Assignment forms

Return the same authorized assignments. Form UIDs come from the latest active
grant, then the existing `referenceVersion` capability gate may narrow them.
Baseline team/form permission checks do not add forms.

#### Organization units

Derive direct organization-unit UIDs from latest active grants, then include
their existing ancestor chain. Managed-team assignments do not expand scope.
`includeDisabled` must not reactivate ended or revoked grants.

#### Reference catalog

Require a latest active grant for the requested assignment containing at least
one Reference-capable form after `referenceVersion=1` capability resolution.
Read catalog rows only for that grant's organization unit. A baseline-visible
assignment or permission cannot authorize the catalog.

### Empty-Capture Read Compatibility

Preserve the two verified baseline-visible assignments with no canonical
capture forms behind one named read-only compatibility adapter:

- they may remain in assignment list and assignment-form wire projections;
- their direct organization units and ancestors may remain in org-unit sync;
- they expose no eligible forms;
- they grant no Reference catalog access;
- they grant no upload authority;
- any assignment with a canonical capture form is ineligible for this adapter.

This is display compatibility, not a mutable grant or second authority.
Retire it when those source rows are corrected/removed or a separately accepted
product decision defines their intended behavior.

### Ownership Cleanup

- Remove `AssignmentCaptureShadowComparator` from the four read paths.
- Remove comparison-only reports, categories, surface enums, metrics, and
  tests after no active consumer remains.
- Replace broad `BaselineAssignmentCaptureAdapter` read use with the narrow
  empty-capture compatibility adapter; retain only independently active
  compatibility required by upload.
- Remove baseline assignment-form and Reference authorization from these four
  released reads once the event owner supplies their scope.
- Keep `AssignmentFilter` and generic inherited read behavior only for
  separately classified admin/legacy routes. Do not silently cut over or
  remove those routes here.
- Do not create another work-scope table, cache, grant projection, or DTO
  authority.

### Tests

Focused tests must prove:

- highest generation alone decides each assignment;
- duplicate/contradictory latest state fails closed;
- administrator reads preserve released behavior without event reads;
- absent actor alias returns empty field-user reads;
- missing checkpoint and reader failure return service unavailable;
- query filters narrow authorized assignment UIDs before paging;
- assignment list and forms expose the same assignment set;
- forms are exactly the active grant forms after capability narrowing;
- ended/revoked grants disappear from assignments, forms, org units, and
  Reference access;
- org units contain active direct scope plus ancestors only;
- managed-team scope and `includeDisabled` cannot widen event authority;
- Reference uses the event-authorized assignment, forms, and org unit;
- the empty-capture adapter cannot grant forms, Reference, or upload;
- upload retains its existing active and eligible-retired decisions through
  the shared latest-grant reader;
- no comparison-only shadow owner remains on a released read path.

Run focused tests, then `scripts/release/verify.sh`.

Against the isolated production clone:

1. require exact 263,423 baseline/event tuple comparison before starting;
2. exercise active assignment/form/org-unit/Reference reads through HTTP;
3. exercise membership, permission, form-scope, status, assignment-retire,
   restore, and latest-generation changes;
4. prove all four read surfaces change together and upload behavior is
   unchanged;
5. prove the two empty-capture rows remain display-only;
6. restore the disposable clone afterward.

Do not connect to or deploy production.

### Slice Gate

- **Authority before:** baseline filters decide all four reads; event state is
  comparison-only.
- **Authority after:** latest active event grants decide all four field-user
  reads; baseline retains only explicit empty-capture display compatibility.
- **Persistence/wire/mobile:** unchanged.
- **Schema/bootstrap:** unchanged; exact checkpoint remains mandatory.
- **Rollback:** revert the cutover to restore baseline read authority; no data
  rollback exists.
- **Retirement:** empty-capture compatibility has the explicit source-data or
  product-decision exit above. It must not become permanent grant semantics.

### Definition Of Done

- One shared owner resolves latest assignment grant generations.
- All four released field-user reads use one event-backed work scope.
- Baseline logic cannot widen event-authorized assignments, forms, org units,
  or Reference access.
- Empty-capture compatibility is isolated and incapable of authorization.
- Upload behavior remains unchanged and uses the shared generation owner.
- Comparison-only shadow code is removed.
- Focused, full release, and isolated-clone gates pass.
- Production remains untouched.
