# Completed Work

Updated: 2026-07-26

Purpose: compact historical outcomes only. This file is not current runtime or
deployment authority.

- `e8bee6b4`: `main` was aligned to the reconstructed deployed server baseline.
- `09b9b27d`: bounded Reference catalog, read boundary, and old-client gate
  landed on `develop`.
- `8fa1d7ac`: bounded Reference upload extraction and resolution landed on
  `develop`.
- `8695335a`: the old analytics-query/jOOQ, Party, and duplicate outbox source
  surfaces were removed without dropping inert legacy tables or active
  ETL/ledger storage.
- `0d83c580`: inactive assignment-member source was removed without dropping
  production tables.
- `v6.4.0`: the immutable server image, additive Reference migration, external
  JWT-key ownership, and existing login/configuration behavior were verified
  in production without activating the new Reference workflow.
- `caee970e`: inactive Mongo persistence, endpoints, dependencies,
  configuration, and Compose ownership were removed from `develop`; ordinary
  configuration reads and submission upload passed against the production
  clone with Mongo stopped.
- `90281457`: the test runtime was aligned with the local Docker API, the
  app-owned `generate_uid()` function entered Liquibase ownership, and focused
  JWT tests were aligned with the active V1 endpoint.
- The immutable `6.4.1-40f176dde05b` candidate passed the release gate and
  isolated staging smoke against the production clone with Mongo stopped:
  health, identity, login, configuration reads, and ordinary submission upload.
- `v6.4.1` is deployed in production. Public health/build identity and
  authenticated mobile configuration sync passed; the app-owned UID migration
  ran successfully. Empty Mongo persistence and its production Compose
  service, volumes, and images were removed after backup and verification.

Use commits, tests, focused context, and deployed evidence to determine current
behavior.
