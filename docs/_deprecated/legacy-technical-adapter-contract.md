# Integration Contract — DatarunAPI

> **Status:** Draft — Living Document
> **Ground Truth:** This document describes the actual DatarunAPI API surface. The `datarunapi` codebase is the sole authority.

## Purpose

This document defines what DatarunAPI **is** as an integration target — its identity, published language, authentication model, and versioning strategy. Any system consuming DatarunAPI's APIs or events must respect the contracts described here.

---

## DatarunAPI Identity

| Property | Value |
|---|---|
| **DDD Role** | Open-Host Service (OHS) + Published Language (PL) |
| **Codebase** | [DataRun-ye/data-run-api](https://github.com/DataRun-ye/data-run-api) |
| **Tech Stack** | Java, Spring Boot, PostgreSQL |
| **Mobile Client** | [DataRun-ye/data-run-mobile](https://github.com/DataRun-ye/data-run-mobile) (Flutter/Dart) |
| **Deployment** | Independent server, separate DB, independent release cycle |
| **Nature** | General-purpose data collection platform. Domain-agnostic. |

---

## Published Language (API Surface)

### V1 REST API (Stable — Mobile Contract)

The V1 API is the canonical contract consumed by the Flutter mobile app. It uses a section-wrapped JSON shape for submissions and flat arrays for templates.

> [!IMPORTANT]
> V1 is replaceable if it becomes a blocking constraint. A replacement strategy can be designed to enable faster forward progress. See [Strategic Blueprint](strategic-blueprint.md) §2.

### V2 REST API (Exploratory — Not Operational)

V2 was an exploratory effort to create normalized contracts decoupling data from UI layout. It is **not yet operational** and does not bind architectural decisions. It may inform the final API surface.

### Core Entities in DatarunAPI's Model

| Entity | Purpose |
|---|---|
| `data_template` | Form definition (fields, repeat blocks) |
| `submission` | Collected data instance |
| `activity` | Collection context (e.g., a campaign or programme round) |
| `party` | Actor/location registry |
| `assignment` | Links template + team + org_unit + period |
| `org_unit` | Organizational hierarchy |

> [!NOTE]
> DatarunAPI's entities are **generic**. They carry no domain-specific meaning (no inventory, stock, or case vocabulary). Business meaning is assigned by downstream consumers.

---

## Authentication

### Identity Provider: DatarunAPI

DatarunAPI is the **single identity provider** for all systems. It issues RS256-signed JWTs and publishes its public key at `/.well-known/jwks.json`.

| Channel | Mechanism |
|---|---|
| **Mobile App → DatarunAPI** | Direct login. RS256 JWT. |
| **Web Frontend → DatarunAPI** | SSO via JWKS. User authenticates once, receives JWT. |
| **Service-to-Service** | Service account authenticates with dedicated credential, receives JWT. |

### Identity vs. Authorization Split

DatarunAPI's JWT contains **identity only** (`sub`, `name`, generic roles). Domain-specific authorization (e.g., per-node access, specific functional roles) must live in the consuming system's own authorization layer — not in DatarunAPI's JWT.

### Phase 2 (Future)

Keycloak (or equivalent) replaces DatarunAPI as identity provider. Downstream authorization layers stay unchanged. See [ADR-008](../adrs/008-auth-phased-strategy.md).

---

## Versioning & Evolution

| Expectation | Rule |
|---|---|
| **API versioning** | DatarunAPI versions its REST API (e.g., `/api/v1/`). Consumers pin to a specific version. |
| **Non-breaking additions** | New fields in JSON responses are safe — consumers ignore unknown fields. |
| **Breaking changes** | Require a new API version. Old versions remain available until all consumers have migrated. |
| **Schema discovery** | DatarunAPI should provide an OpenAPI spec. |

---

## Constraints & Known Limitations

1. **No real-time push (yet).** Consumers currently pull data via polling or scheduled sync. Webhooks / transactional outbox are planned.
2. **Party Access Model** is ~80% implemented. Consumers should gracefully handle missing `party` fields.
3. **DatarunAPI has known internal tech debt.** This does not affect the integration contract as long as the API surface remains stable.

## Related Docs

- [System Overview](system-overview.md)
- [Context Map](context-map.md)
- [Auth & Authorization](auth-and-authorization.md)
- [ADR-008: Auth Strategy](../adrs/008-auth-phased-strategy.md)
