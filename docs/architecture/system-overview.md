# System Overview — Datarun Platform

> **Status:** Draft — Living Document
> **Ground Truth:** The `datarunapi` codebase (Java/Spring Boot) is the single operational system.

## Vision

**DatarunAPI** is a general-purpose data-collection platform. It manages templates, submissions, assignments, parties, and organizational hierarchies. It is **domain-agnostic** — it does not know about inventory, stock, malaria, case management, or any specific business domain. Business meaning is assigned externally through configuration and downstream interpretation.

The platform is evolving toward a **Configuration-Isolated Orchestration Platform** — where multiple functional domains can coexist on the same engine via metadata configuration rather than code changes. See [Strategic Blueprint](strategic-blueprint.md) and [ADR-010](../adrs/010-evolutionary-architecture.md).

## Architecture Pattern

DatarunAPI operates as an **Open-Host Service (OHS)** with a **Published Language** (V1 REST API; V2 is exploratory):

```
  Flutter Mobile App ──► V1 REST API ──► Canonical Store (PostgreSQL)
                                                 ▲
  Web Frontend (future) ──► V2 REST API ─────────┘
                                                 ▼
                                         Event Gateway (future)
                                                 ▼
                                        Downstream Consumers
```

Key patterns in use:
- **API versioning** (V1 stable for mobile, V2 exploratory)
- **Internal ACL** translator for V1↔V2 canonical store bridging
- **Transactional Outbox** (planned) for reliable event delivery to downstream systems

## Core Entities

| Entity | Purpose |
|--------|---------|
| `data_template` | Form definition — fields, repeat blocks, validation rules |
| `submission` | Collected data instance (a filled form) |
| `activity` | Collection context (e.g., a campaign or programme round) |
| `assignment` | Links template + team + org_unit + period |
| `party` | Actor/location registry |
| `org_unit` | Organizational hierarchy |

## External Systems

| System | Relationship | Status |
|--------|-------------|--------|
| **Flutter Mobile App** (Dart) | V1 API consumer, offline-first | ✅ Operational |
| **Web Frontend** (Angular) | V2 API consumer | 📋 Architecture defined, not yet built |
| **Downstream Consumers** (LMIS, Analytics, etc.) | Receive events via Gateway | 💡 Conceptual — see [_ideas/](_ideas/) |

## What This System Is NOT

- **Not a domain-specific system.** DatarunAPI does not contain business logic for inventory, stock accounting, case management, or any vertical domain. It is a generic data-collection backbone.
- **Not multi-tenant (yet).** Currently single deployment per health programme. Evolving toward configuration isolation. See [ADR-010](../adrs/010-evolutionary-architecture.md).

## Related Docs

| Topic | Document |
|-------|----------|
| North Star Vision | [Strategic Blueprint](strategic-blueprint.md) |
| C4 Diagrams | [C4 Model](c4-model.md) |
| DatarunAPI V2 Strategy | [DatarunAPI README](../datarunapi/README.md) |
| Integration Contract | [Integration Contract](integration-contract-datarunapi.md) |
| Authentication & SSO | [Auth & Authorization](auth-and-authorization.md) |
| ADRs | [All ADRs](../adrs/) |
| Governance | [Living Architecture Charter](../governance/README.md) |
| Conceptual Ideas (non-operational) | [_ideas/](../_ideas/) |
