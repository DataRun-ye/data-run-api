# Current Work

Updated: 2026-07-28

Status: ACCEPTED FOR IMPLEMENTATION

## Production Verification Gate Repair

### Outcome

Make the server's production-profile verification command exercise the current
registered application instead of failing on obsolete generated endpoint tests
or skipping integration tests during release verification.

This is a test-infrastructure slice. It must not change runtime endpoints,
security policy, persistence, payloads, or production configuration.

### Evidence

- `./mvnw test` passes because Surefire intentionally excludes `*IT*` and
  `*IntTest*`.
- `./mvnw -Pprod clean verify` reaches Failsafe but currently fails in five
  generated suites that call endpoint shapes replaced in 2025.
- `IntegrationTest` hardcodes `testdev`, overriding Maven's `test,testprod`
  selection under the production profile.
- `scripts/release/verify.sh` runs unit tests, then invokes the production
  build with `-DskipTests`; it therefore does not prove the production-profile
  integration gate.
- Current authentication and versioned administrator-route security already
  have focused tests against their registered endpoints.

### Required Change

1. Make Maven own integration-test profile selection:
   - remove hardcoded `@ActiveProfiles("testdev")` from `IntegrationTest`;
   - remove the duplicate JavaDoc and commented-out historical annotation;
   - retain the existing `dev -> testdev` and `prod -> test,testprod` Maven
     profile mappings.
2. Remove these obsolete generated suites:
   - `AccountResourceIT`;
   - `AuthenticateControllerIT`;
   - `AuthorityResourceIT`;
   - `PublicUserResourceUpdateIT`;
   - `UserResourceUpdateIT`.
3. Do not restore their legacy routes or rewrite their generated CRUD
   expectations against unrelated current endpoints.
4. Preserve and run the focused current-contract tests, including
   `TokenAuthenticationIT`, `TokenAuthenticationSecurityMetersIT`, and
   `UserAdminRouteSecurityIT`.
5. Change `scripts/release/verify.sh` to run one real production-profile gate:

   ```bash
   ./mvnw -Pprod clean verify
   ```

   Remove the redundant standalone unit-test invocation and do not pass
   `-DskipTests`.

Removing the obsolete suites does not claim that endpoint coverage is
complete. New characterization belongs to the bounded owner of the active
endpoint, not to this infrastructure repair.

### Production Boundary

- Runtime source and resources: unchanged.
- Registered routes and authorization: unchanged.
- Database and Liquibase: unchanged.
- Build artifact identity checks: retained.
- Production deployment: none.

### Excluded Work

- Adding compatibility aliases for removed endpoints.
- Changing account registration, password, authority, or user-admin policy.
- Broad test modernization or coverage targets.
- Assignment bootstrap or event-transition runtime behavior.
- Dependency, Maven-version, or plugin upgrades.

### Verification

Before commit:

```bash
./mvnw -Pprod clean verify
git diff --check
```

After commit, with a clean worktree:

```bash
scripts/release/verify.sh
```

### Definition Of Done

- Production-profile verification runs unit and integration tests and passes.
- Maven, not a test annotation, selects `testdev` versus `test,testprod`.
- No test still depends on the five obsolete route contracts.
- Release verification no longer skips the integration gate.
- No runtime or production behavior changes.
