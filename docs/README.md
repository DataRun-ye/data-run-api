# Datarun Platform — Documentation Index

> **Ground Truth:** The `datarunapi` codebase is the single source of truth.
> All documentation below is **living, disposable guidance** — it can be rewritten when the code reality changes.

## How to Use These Docs

Each folder corresponds to a concern area or governance process. When working with AI, reference **only** the relevant folder to keep context windows small.

> [!CAUTION]
> **Do NOT load docs from `_ideas/` or `_deprecated/` unless explicitly asked.** Those contain conceptual explorations and outdated material that could mislead AI agents into enforcing non-existent constraints.

---

## Quick Reference

| Working On | Load These Docs |
|------------|----------------|
| **Strategic direction / North Star** | `architecture/strategic-blueprint.md` |
| **System diagrams (C4)** | `architecture/c4-model.md` |
| **Process rules, templates** | `governance/` |
| **Why a decision was made** | `adrs/` |
| **Proposing a big change** | `rfcs/` + `governance/rfc-process.md` |
| **DatarunAPI integration / V1/V2 contract** | `architecture/integration-contract-datarunapi.md` |
| **Authentication / SSO** | `architecture/auth-and-authorization.md` |
| **DatarunAPI frontend architecture** | `datarunapi/datarunapi-frontend/` |

---

## Folder Structure

### [`governance/`](governance/) — Documentation Governance
- [Living Architecture Charter](governance/README.md) — Principles, fitness functions, ADR/RFC process rules
- [ADR Template](governance/adr-template.md) — Standardized format for decision records
- [RFC Template](governance/rfc-template.md) — Format for architectural proposals
- [RFC Process](governance/rfc-process.md) — RFC lifecycle and when to use RFCs vs ADRs

### [`architecture/`](architecture/) — System Architecture
- [Strategic Blueprint](architecture/strategic-blueprint.md) — North Star vision, phased transition, business capability map
- [C4 Model](architecture/c4-model.md) — System Context, Container, and Component diagrams
- [System Overview](architecture/system-overview.md) — What DatarunAPI is, core entities, external systems
- [Context Map](architecture/context-map.md) — DDD strategic relationships (operational + directional)
- [Integration Contract — DatarunAPI](architecture/integration-contract-datarunapi.md) — V1/V2 API boundary, auth channels, versioning
- [Auth & Authorization](architecture/auth-and-authorization.md) — JWKS, JWT, SSO strategy

### [`adrs/`](adrs/) — Architectural Decision Records

> [!NOTE]
> ADRs 001–008 were written for a planned LMIS downstream platform (Python/FastAPI) that has not been implemented. They may inform future architecture but do **not** constrain the current `datarunapi` codebase. ADR-009+ apply to the current system.

- [ADR-001: Modular Monolith](adrs/001-modular-monolith.md) *(LMIS platform)*
- [ADR-002: Event Sourcing](adrs/002-event-sourcing.md) *(LMIS platform)*
- [ADR-003: CQRS Projections](adrs/003-cqrs-projections.md) *(LMIS platform)*
- [ADR-004: Sync Execution (No Kafka)](adrs/004-sync-execution-no-kafka.md) *(LMIS platform)*
- [ADR-005: Async Workers via Lifespan](adrs/005-async-workers-lifespan.md) *(LMIS platform)*
- [ADR-006: 3-Layer Adapter Pipeline](adrs/006-three-layer-adapter-pipeline.md) *(LMIS platform)*
- [ADR-007: API Composition Strategy](adrs/007-api-composition-strategy.md) *(LMIS platform)*
- [ADR-008: Auth Phased Strategy](adrs/008-auth-phased-strategy.md) *(LMIS platform)*
- [ADR-009: Feature Flags — Deferred](adrs/009-feature-flags-deferred.md)
- [ADR-010: Evolutionary Architecture](adrs/010-evolutionary-architecture.md)

### [`rfcs/`](rfcs/) — Requests for Comments
- No RFCs yet. See [RFC Process](governance/rfc-process.md).

### [`datarunapi/`](datarunapi/) — DatarunAPI-Specific Docs
- [DatarunAPI V2 Strategy](datarunapi/README.md) — V2 vision, ACL strategy, domain alignment
- **Web Frontend (exploratory):**
  - [Overview](datarunapi/datarunapi-frontend/overview.md) — Architecture, modules, layers
  - [Form Engine Contract](datarunapi/datarunapi-frontend/form-engine.md) — Headless engine API
  - [Architecture Decisions](datarunapi/datarunapi-frontend/architecture-decisions.md)

---

## Archived Docs

### [`_ideas/`](_ideas/) — Conceptual Explorations (Not Implemented)
Ideas and architectural explorations for a downstream LMIS platform. **Not operational. Not constraints.** See [_ideas/README.md](_ideas/README.md).

### [`_deprecated/`](_deprecated/) — Superseded Documentation
Old documentation preserved for historical reference only. **Do not use as authoritative sources.**
