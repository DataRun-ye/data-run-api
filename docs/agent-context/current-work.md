# Current Work

Updated: 2026-07-28

Status: ACCEPTED FOR IMPLEMENTATION

## Assignment Capture Authorization Shadow

### Outcome

Run one event-backed capture-authorization decision in shadow beside the
released baseline decision across the five active surfaces that consume
assignment scope:

- assignment list;
- assignment-form projection;
- organization-unit synchronization;
- Reference catalog access;
- versioned submission upload.

The released baseline remains response and upload authority in this slice.
No request, response, mobile, persistence, or authorization behavior changes.
No schema change, backfill, feature flag, or production activation is included.

### Selected Boundary

Introduce one read-only event-backed owner for actor capture scope. It reads
the assignment lifecycle projection created from the event journal; it does
not mutate that projection and does not create a second authority.

The normalized active value is:

```text
baseline assignment UID
target actor UUID
activity UID
organization-unit UUID
sorted distinct capture-form UIDs
```

Read it by joining the current active grant stream through:

```text
actor_identity_link
assignment_identity_link
assignment_grant_projection
assignment_role_definition
org_unit_identity_link
```

Do not use `assignment_access_projection` for per-assignment decisions: it is
the deduplicated effective-access projection and intentionally omits baseline
assignment identity. Extend the read port with one bounded batch query rather
than reconstructing identity in services.

Resolve the authenticated actor from `CurrentUserDetails.uid` through
`actor_identity_link`. Reads never mint a missing alias. No alias plus no
baseline capture scope is an equivalent empty result. A baseline capture scope
without its actor alias is an unexplained mismatch.

Administrator authority remains the existing explicit `isSuper()` decision.
Administrators are not represented as assignment grants and are not included
in field-user equivalence counts.

### Baseline Normalization

For current work and synchronized configuration, a baseline capture scope
exists only when:

- the assignment is not soft-deleted;
- assignment activity, assignment team, and team activity are enabled;
- the actor is a direct team member;
- assignment, actor, activity, organization-unit, team, and form identities
  required by the current path are valid;
- the sorted distinct intersection of assignment forms and same-team
  `ADD_SUBMISSIONS`/`EDIT_SUBMISSIONS` permissions is non-empty.

Use `CanonicalCaptureFormResolver` for the form intersection. Do not create a
second interpretation of role forms.

For versioned upload only, preserve the released exception: assignment
soft-deletion does not itself reject an already-created offline submission.
Current team membership and same-team capture permission still apply.

The shadow result for one assignment/form is one of:

```text
ACTIVE_GRANT
ENDED_RETIRED_ASSIGNMENT
REVOKED_HISTORY
NO_GRANT
SHADOW_UNAVAILABLE
```

`ENDED_RETIRED_ASSIGNMENT` requires a matching ended actor/assignment/form
generation, the current baseline assignment's `deleted=true` state, and the
same current membership and form-permission checks that make soft deletion the
only reason the active grant is absent. History ended or currently denied
because membership, permission, team, activity, role, or scope changed is
`REVOKED_HISTORY` and must not broaden released upload acceptance. This
distinction is comparison evidence only in this slice.

### Surface Integration

#### Assignment list

Keep the inherited baseline result unchanged. For a non-administrator request,
batch-compare the returned assignments with active actor grant streams.

- capture-bearing baseline assignments must have the same event-backed scope;
- event-backed assignment scopes absent from the baseline result are
  unexplained mismatches;
- a baseline-visible assignment with no capture-form intersection is
  `COMPATIBILITY_ONLY_EMPTY_CAPTURE`, not an event grant and not an unexplained
  mismatch.

Do not add one event query per assignment. A paged request compares the page
returned; the released mobile's `paged=false` request compares the complete
result.

#### Assignment-form projection

Keep `AssignmentWithAccessDto` and `AssignmentFormDto` unchanged. Compare only
forms for which the baseline currently permits capture through
`ADD_SUBMISSIONS` or `EDIT_SUBMISSIONS`.

`canAddSubmissions`, `canEditSubmissions`, `canDeleteSubmissions`, and forms
visible only through non-capture permissions remain baseline wire
compatibility fields. Do not infer those action distinctions from the initial
event role, which intentionally owns only the canonical capture-form set.

#### Organization-unit synchronization

Compare direct organization-unit scopes derived from baseline capture-bearing
assignments with direct scopes from active grants. Ancestors remain a hierarchy
navigation projection loaded from the baseline organization-unit tree; they
are not additional grants.

A direct organization unit supplied only by a baseline-visible empty-capture
assignment is compatibility-only. An event-backed direct scope with no
baseline capture scope is an unexplained mismatch.

#### Reference catalog

Keep the current accessible-assignment and `ADD_SUBMISSIONS` requirement.
Whenever that baseline check permits Reference catalog access, the event
shadow must contain the same active assignment/form scope.

The reverse is not sufficient: an event role may contain a form through
`EDIT_SUBMISSIONS`, while Reference creation currently requires
`ADD_SUBMISSIONS`. Keep that action-specific narrowing in the existing
baseline adapter. It retires only with a separately accepted action-capability
cutover; do not invent one here.

#### Versioned submission upload

Refactor the current field-user upload check into a value decision so both
baseline and event results are available before the existing error is thrown.
Return the baseline decision to `SubmissionUploadService`; retain the exact
released error codes and administrator bypass.

Expected comparisons:

- accepted active assignment/form -> `ACTIVE_GRANT`;
- accepted soft-deleted assignment/form ->
  `ENDED_RETIRED_ASSIGNMENT`;
- baseline denial after membership/permission/status/scope change ->
  `REVOKED_HISTORY` or `NO_GRANT`, both denied;
- an event active grant rejected by the baseline -> unexplained mismatch;
- baseline acceptance with no matching active or retired-assignment history ->
  unexplained mismatch.

Only `POST /api/v1/dataSubmission/bulk?referenceVersion=1` is in scope. The
unversioned validator pipeline remains a separately classified legacy-risk
surface and must not acquire another authorization implementation in this
slice.

### Comparison Ownership

Use one comparator and one normalized event read model across all surfaces.
Do not scatter equality logic through controllers, mappers, filters, and upload
services.

Comparison must:

- return baseline behavior without throwing merely because shadow data is
  unavailable or mismatched;
- emit bounded structured diagnostics and Micrometer counters by surface and
  result category;
- never log form data, names, tokens, passwords, or request bodies;
- avoid persistent comparison tables;
- avoid cross-request caches and token claims;
- batch event reads once per actor/request or upload batch;
- treat a missing bootstrap checkpoint as `SHADOW_UNAVAILABLE`, not as
  permission and not as a reason to block the released baseline path.

Tests must be able to assert comparison outcomes directly. Do not make log
parsing the only executable contract.

### Tests

Focused unit and PostgreSQL tests must prove:

- active projection lookup preserves baseline assignment identity and returns
  canonical role forms and aliased organization-unit scope;
- actor alias absence is equivalent only when baseline capture scope is also
  empty;
- assignment list, capture-form, direct organization-unit, and Reference
  comparisons classify exact, compatibility-only, unavailable, and
  unexplained outcomes correctly;
- assignment list and bulk upload perform bounded event queries, not per-row
  lookups;
- administrator behavior is unchanged and excluded from assignment-grant
  comparison;
- active versioned upload is classified `ACTIVE_GRANT` and retains current
  success/error behavior;
- an accepted upload against a soft-deleted assignment is classified
  `ENDED_RETIRED_ASSIGNMENT`;
- membership removal, permission removal, team/activity disablement, role/form
  change, and scope change leave history but do not become late-upload
  permission;
- missing checkpoint or contrary shadow data changes diagnostics only, never
  the released response in this slice;
- assignment and assignment-form wire JSON, organization-unit results,
  Reference paging, and versioned upload summaries remain unchanged.

Run focused tests, then `scripts/release/verify.sh`. Against the isolated
production clone:

1. run the generation-aware assignment comparison and require exact tuple
   equality;
2. exercise the five surfaces for clone-only field-user fixtures through the
   real HTTP routes;
3. cover active, retired-assignment, membership/permission revoked, disabled,
   and restored states;
4. require zero unexplained comparison outcomes and unchanged HTTP payloads;
5. restore the disposable clone after the scenarios.

Do not connect to or deploy production.

### Slice Gate

- **Authority before:** released baseline filters and
  `AssignmentFormAccessService`.
- **Authority after:** unchanged; one event-backed decision runs in shadow and
  owns comparison evidence.
- **Baseline compatibility owner:** existing assignment filters,
  action-specific form flags, and versioned upload errors.
- **Schema/backfill:** none; requires the completed assignment bootstrap and
  checkpoint.
- **Shadow comparison:** all five active surfaces use one normalized event read
  model and one comparator.
- **Activation:** none in this slice.
- **Rollback:** removing the shadow reader/comparator restores the identical
  baseline behavior; no data rollback exists.
- **Retirement:** the next cutover handoff may select event authority only
  after clone and controlled runtime evidence contain no unexplained mismatch.

### Definition Of Done

- The five active surfaces produce explicit event-shadow evidence without
  changing released behavior.
- Event decisions are request-current, batched, and assignment-specific.
- Retired-assignment upload compatibility is distinguished from revoked
  history and does not broaden acceptance.
- Action-specific compatibility fields remain in one named baseline adapter.
- Focused, full release, and isolated-clone gates pass.
- Production remains untouched.
