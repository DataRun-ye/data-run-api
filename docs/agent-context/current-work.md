# Current Work

Updated: 2026-07-28

Status: ACCEPTED FOR IMPLEMENTATION

## Baseline Assignment Eligibility Correction

### Outcome

Organization-unit access must not be derived from a soft-deleted assignment.
This aligns organization-unit synchronization with the released assignment
list before assignment event shadowing begins.

### Current Evidence

- `OrgUnitFilter.getDirectOrgUnits` loads assignments for the actor's direct
  teams and filters disabled teams and activities, but not
  `assignment.deleted`.
- `DefaultJpaSoftDeleteService` excludes soft-deleted assignments from normal
  assignment reads.
- The production clone contains 78 organization-unit scopes held only through
  retired assignments, exposed to eight users by the current filter.
- `includeDisabled` controls disabled team/activity visibility. It does not
  mean that retired assignments grant access.

### Required Change

In `OrgUnitFilter`, exclude every assignment where
`Boolean.TRUE.equals(assignment.getDeleted())` before mapping assignments to
organization units.

The exclusion applies when `includeDisabled` is both `false` and `true`.
Existing disabled-team and disabled-activity behavior remains unchanged.

Add focused characterization covering:

- a retired assignment is excluded in the normal path;
- a retired assignment is still excluded when disabled entities are included;
- enabled assignments remain included;
- disabled teams and activities retain their current `includeDisabled`
  behavior.

### Scope

- `src/main/java/org/nmcpye/datarun/jpa/accessfilter/OrgUnitFilter.java`
- `src/test/java/org/nmcpye/datarun/jpa/accessfilter/OrgUnitFilterTest.java`

### Production Boundary

- Authority before and after: `OrgUnitFilter` remains the organization-unit
  access owner for the released path.
- API and payloads: unchanged.
- Database schema and data: unchanged; no migration or backfill.
- Submission upload: unchanged, including current handling of retired
  assignment UIDs.
- Event transition: no event tables, facts, projections, or shadow reads are
  introduced in this slice.
- Activation: the correction takes effect only when a later server release is
  deployed.
- Rollback: revert this code change; no persisted state requires rollback.

### Excluded Work

- Assignment event modeling or bootstrap.
- Changes to assignment upload authorization.
- Managed-team organization-unit expansion.
- Endpoint removal, schema cleanup, or unrelated access refactoring.

### Verification

Run:

```bash
./mvnw -Dtest=OrgUnitFilterTest test
./mvnw test
git diff --check
```

### Definition Of Done

- The focused tests prove both `includeDisabled` modes.
- The full unit-test suite passes, or any unrelated baseline failure is
  reported without being hidden or fixed in this slice.
- The diff contains only the filter and its focused test.
- No production deployment is performed.
