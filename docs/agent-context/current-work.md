# Current Work

Updated: 2026-07-28

Status: ACCEPTED FOR IMPLEMENTATION

## Secure The Versioned Administrator Boundary

### Outcome

Every `/api/v1/admin/**` request requires `ROLE_ADMIN`, matching the existing
`/api/custom/admin/**` and `/api/admin/**` boundaries.

### Current Evidence

- `UserResource` is registered at both `/api/custom/admin/users` and
  `/api/v1/admin/users`.
- `DataRunSecurityConfig` explicitly protects `/api/custom/admin/**` and
  `/api/admin/**`, but not `/api/v1/admin/**`.
- `/api/v1/admin/**` currently falls through to the general `/api/**`
  authenticated rule.
- `UserResource` custom GET, PUT, and DELETE handlers have no class-level or
  method-level administrator guard. Generic inherited writes perform
  additional checks, but those do not protect the custom handlers.

### Required Change

Add an MVC request matcher for `/api/v1/admin/**` requiring
`AuthoritiesConstants.ADMIN`. It must appear before the general `/api/**`
authenticated matcher.

Add an integration test against a real registered versioned administrator
route proving:

- an authenticated `ROLE_USER` receives HTTP 403;
- an authenticated `ROLE_ADMIN` is allowed through the security boundary.

Use `GET /api/v1/admin/users/all` unless runtime route evidence requires an
equivalent registered `/api/v1/admin/**` endpoint.

### Scope

- `src/main/java/org/nmcpye/datarun/config/datarun/DataRunSecurityConfig.java`
- one focused test under `src/test/java/org/nmcpye/datarun/security/`

### Production Boundary

- Authentication and token behavior: unchanged.
- Administrator APIs and payloads: unchanged for administrators.
- Normal authenticated users lose unintended access to versioned
  administrator handlers.
- Database schema and data: unchanged.
- Assignment, configuration, and submission behavior: unchanged.
- Activation: only with a later approved server deployment.
- Rollback: revert this code change; no persisted state requires rollback.

### Excluded Work

- User, role, privilege, or ACL redesign.
- Assignment event modeling.
- Changes to `/api/custom/admin/**`, `/api/admin/**`, or public authentication
  endpoints.
- Generic endpoint cleanup or user-service refactoring.

### Verification

Run:

```bash
./mvnw -Dtest=UserAdminRouteSecurityIT test
./mvnw test
git diff --check
```

### Definition Of Done

- The focused integration test proves both non-admin denial and admin access.
- The full test suite passes, or an unrelated baseline failure is reported
  without being hidden or fixed in this slice.
- The diff contains only the matcher and its focused test.
- No production deployment is performed.
