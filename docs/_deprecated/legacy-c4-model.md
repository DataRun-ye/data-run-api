# C4 Model — Datarun Platform

> **Status:** Draft — Living Document
> **Scope:** Cross-cutting — full system context
> **Notation:** [C4 Model](https://c4model.com/) rendered as Mermaid diagrams

---

## Level 1 — System Context

Who uses the system and what external systems exist?

```mermaid
graph TB
    subgraph Users
        FW["Field Worker<br/>───<br/>Collects data via<br/>mobile app (offline-first)"]
        SUP["Supervisor<br/>───<br/>Reviews submissions,<br/>approves transactions"]
        ADMIN["Admin<br/>───<br/>Configures templates,<br/>assignments, policies"]
    end

    subgraph Systems
        DR["DatarunAPI Platform<br/>───<br/>Java · Spring Boot · PostgreSQL<br/>General-purpose data collection"]
        LMIS["LMIS Platform<br/>───<br/>Python · FastAPI · PostgreSQL<br/>Domain intelligence (future)"]
    end

    FW -->|"V1 REST API<br/>(offline sync)"| DR
    SUP -->|"V2 Web UI"| DR
    ADMIN -->|"V2 Web UI"| DR
    DR -->|"Events / API"| LMIS
    LMIS -->|"Dashboards"| SUP
```

> [!NOTE]
> The LMIS Platform is **documented but not yet implemented**. DatarunAPI is the single operational system.

---

## Level 2 — Container Diagram

Zoom into the DatarunAPI Platform. What are the major runtime containers?

```mermaid
graph TB
    subgraph DatarunPlatform["DatarunAPI Platform"]
        API["REST API Server<br/>───<br/>Spring Boot<br/>V1 + V2 endpoints"]
        DB[(PostgreSQL<br/>───<br/>Canonical Store<br/>Templates · Submissions<br/>Assignments · Parties)]
        WEB["Web Frontend<br/>───<br/>Angular SPA<br/>V2 API consumer"]
        GW["Event Gateway<br/>───<br/>Outbox Relay + Mapper<br/>(planned)"]
    end

    MOB["Flutter Mobile App<br/>───<br/>Offline-first<br/>V1 API consumer"]

    subgraph Downstream["Downstream Consumers (Future)"]
        LMIS_A["LMIS Adapter"]
        ANALYTICS["Analytics Engine"]
    end

    MOB -->|"V1 REST"| API
    WEB -->|"V2 REST"| API
    API <-->|"JDBC"| DB
    API -->|"Transactional<br/>Outbox"| GW
    GW -->|"HTTP POST"| LMIS_A
    GW -->|"HTTP POST"| ANALYTICS

    style GW stroke-dasharray: 5 5
    style LMIS_A stroke-dasharray: 5 5
    style ANALYTICS stroke-dasharray: 5 5
```

**Legend:** Dashed borders = planned / not yet implemented.

---

## Level 3 — Component Diagram: DatarunAPI

Zoom into the DatarunAPI REST API Server. What are the major internal components?

```mermaid
graph TB
    subgraph APIServer["DatarunAPI — API Server"]
        direction TB
        subgraph V1["V1 API Layer (Mobile)"]
            V1_RES["V1 Resources<br/>───<br/>JpaBaseResource wrappers<br/>DTO façade (stabilized)"]
        end
        subgraph V2["V2 API Layer (Web)"]
            V2_RES["V2 Resources<br/>───<br/>Clean Room endpoints<br/>Domain-aligned DTOs"]
        end
        subgraph ACL["Internal ACL"]
            TRANS["V1 ↔ V2 Translator<br/>───<br/>Bidirectional<br/>canonical store mapping"]
        end
        subgraph Services["Domain Services"]
            FORM["Form Config Service<br/>───<br/>Templates, Fields, Rules"]
            DATA["Data Capture Service<br/>───<br/>Submissions, Validation"]
            OPS["Operations Service<br/>───<br/>Assignments, Teams, OrgUnits"]
        end
        subgraph Infra["Infrastructure"]
            AUTH["Auth (JWKS)<br/>───<br/>RS256 JWT validation<br/>Identity provider"]
            OUTBOX["Outbox Publisher<br/>───<br/>Bi-temporal fat events"]
            UID["UID Generator<br/>───<br/>CodeGenerator<br/>11-char stable identifiers"]
        end
    end

    DB[(PostgreSQL)]

    V1_RES --> TRANS
    V2_RES --> TRANS
    TRANS --> Services
    Services <--> DB
    Services --> OUTBOX
    AUTH --> V1_RES
    AUTH --> V2_RES
```

---

## Level 3 — Component Diagram: LMIS Platform (Future)

For reference — the planned downstream platform.

```mermaid
graph TB
    subgraph LMISPlatform["LMIS Platform (Future — Python/FastAPI)"]
        subgraph Adapter["Adapter BC (ACL)"]
            ING["Ingestion Layer"]
            TRANSFORM["Transformation Layer"]
            EGRESS["Egress Layer"]
        end
        subgraph Ledger["Ledger BC"]
            IDEM["Idempotency Guard"]
            APPROVAL["Approval Gatekeeper"]
            EVENTS["Event Store"]
            TRANSIT["In-Transit Registry"]
        end
        subgraph Kernel["Shared Kernel"]
            NODES["Node Registry"]
            COMMODITIES["Commodity Registry"]
            POLICY["Policy Engine"]
        end
        subgraph BFF["Composition (BFF)"]
            AGG["API Aggregator"]
        end
    end

    ING --> TRANSFORM --> EGRESS
    EGRESS -->|"LedgerCommand"| IDEM
    IDEM --> APPROVAL --> EVENTS
    Kernel -.->|"DB reads"| Ledger
    Ledger -->|"Read Models"| AGG
    Kernel -->|"Read Models"| AGG

    style LMISPlatform stroke-dasharray: 5 5
```

> [!NOTE]
> **Level 4 (Code)** is intentionally omitted. During active refactoring, code-level diagrams become stale within days. Use `view_file_outline` and `view_code_item` tools to explore code structure on demand.

---

## AI Agent Instructions

When loading this document:
1. Use Level 2 to understand which containers exist and how they communicate
2. Use Level 3 to understand internal component structure when working on a specific area
3. **Dashed borders** mean "planned / not implemented" — do not assume these components exist in code
4. Cross-reference with [Context Map](context-map.md) for DDD relationship labels

---

## Related Docs

| Topic | Document |
|-------|----------|
| DDD Relationships | [Context Map](context-map.md) |
| Strategic Vision | [Strategic Blueprint](strategic-blueprint.md) |
| Integration Contract | [DatarunAPI Contract](integration-contract-datarunapi.md) |
| System Overview | [System Overview](system-overview.md) |
