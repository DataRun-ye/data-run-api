# Strategic Blueprint — Datarun Platform

> **Status:** Draft — Living Document
> **Ground Truth:** The `datarunapi` codebase (Java/Spring Boot) is the only operational system.
> **Scope:** Cross-cutting — covers the entire Datarun ecosystem vision

---

## What This Document Is

This is the **North Star** — a strategic alignment document that connects business capabilities to technical architecture. It describes where we are, where we're heading, and the principles that guide the journey.

This document is **disposable**. It will be rewritten as the system evolves. What matters is the *fitness functions* and *ADRs* that codify actual decisions.

---

## 1. Current Baseline

### What Exists Today (Operational)

| Component | Stack | Status |
|-----------|-------|--------|
| **DatarunAPI** | Java, Spring Boot, PostgreSQL | ✅ Operational |
| **Flutter Mobile App** | Dart | ✅ Operational (V1 consumer) |
| **V1 REST API** | Spring Boot | ✅ Stable — mobile app contract (replaceable if blocking) |
| **V2 REST API** | Spring Boot | 🔬 Exploratory — not yet operational |
| **Event Gateway (Outbox)** | Java (in DatarunAPI) | 📋 Designed, not yet implemented |
| **LMIS Platform** | Python, FastAPI, PostgreSQL | 💡 Explored in docs — ideas only, not architectural decisions |

### What DatarunAPI Is Today

A **general-purpose data-collection platform**. It knows about templates, submissions, assignments, parties, and org units. It does not know about inventory, stock, malaria, or any domain-specific concept.

It is:
- A **single-tenant** system serving one health programme per deployment
- An **Open-Host Service** with a Published Language (V1 API; V2 is exploratory)
- The **identity provider** (RS256 JWTs via JWKS)
- **Domain-agnostic** — business meaning is assigned downstream

> [!IMPORTANT]
> **V1 is not sacred.** If V1 becomes a blocking constraint, a replacement strategy can be designed to move forward fast. V2 was *exploratory* — it may inform the final API surface but does not bind it.

> [!WARNING]
> **LMIS docs (Ledger, Kernel, Adapter, Composition) are ideas, not decisions.** Those documents explored concepts and patterns for a downstream platform. They do not determine the architectural direction. The actual architecture will be decided through ADRs and RFCs as implementation begins.

---

## 2. North Star Vision

### Configuration-Isolated Orchestration Platform

The system evolves toward a platform where:
- Every **business process** (Team A submitting to Team B, stock reporting, case data collection) is a **metadata configuration** — not a hard-coded feature
- Multiple independent **scenarios** (programmes, domains, workflows) can coexist on the same engine without code changes
- The `data_template` + `assignment` + policy configuration define *what* happens — not Java code

### What This Is NOT

- **Not multi-tenant** in the traditional SaaS sense (shared infrastructure with data isolation between paying customers)
- **Not a BPMN engine** — orchestration complexity is deferred until domain requirements demand it
- **Not a rewrite** — the current codebase evolves incrementally

### The Core Insight

The danger is the **"not multi-tenant" trap**: ending up running different versions of the same app for different teams because configurations are too hard-coded to coexist. The goal is **configuration isolation** — where different scenarios' configurations can live side-by-side in the same deployment without interfering with each other.

---

## 2b. Architectural Direction (Emerging — Not Final)

The following concepts are actively being considered for the system's future. They are **not decisions** — they are the mental model being refined.

### Multi-Schema Tenancy

A single organizational entity orchestrates multiple **Functional Domains** (e.g., LMIS and Case Management) while maintaining **logical data partitioning**. This is "Vertical Multi-Tenancy" — different groups doing completely different things on one backbone, as opposed to "Horizontal Multi-Tenancy" where different customers do the same thing in isolated compartments.

### Architectural Neighbors (50–70% Match)

These are related patterns and concepts that inform our thinking:

| Concept | Relevance | Example |
|---------|-----------|--------|
| **Vertical Multi-Tenancy** | High — our primary model. Different functional domains (LMIS, Case Mgmt) share one engine | A pharmacist in LMIS and a doctor in Case Mgmt both use the same DatarunAPI backbone |
| **Namespace Isolation** | High — technical mechanism for data partitioning. Prefixing/scoping data so different domains never collide | LMIS data and Field Registry data in the same table, partitioned by scenario/domain key |
| **Cross-Tenant Data Sharing** | Medium — controlled interface for domains to see each other's data | A national health registry syncing read-only views with local clinic data |
| **Domain Convergence** | Medium — when two roles in different domains need to interact with the same entity | A doctor (Case Mgmt) and pharmacist (LMIS) both referencing the same "Patient" |

> [!NOTE]
> These concepts will be formalized into ADRs and RFCs as the implementation matures. For now, they serve as directional guidance for AI agents and human reviewers.

---

## 3. Evolutionary Principles

### First Principle: Protect Fitness Functions, Allow Everything Else to Evolve

| Principle | Meaning |
|-----------|---------|
| **Modular Core** | Strict interface contracts between modules. No leaking internals. |
| **Configuration over Code** | New scenarios are deployed by adding configuration, not by writing new classes. |
| **Forward Compatibility** | Interfaces are designed so that future extensions don't break existing consumers. |
| **Deferred Orchestration** | No BPMN engine until domain complexity genuinely requires it. Simple state machines suffice until then. |
| **Incremental Migration** | Old contracts are deprecated, never deleted overnight. V1 can be replaced if blocking. |
| **Disposable Documentation** | Docs describe intent. Code describes reality. ADRs capture why. |

### Fitness Functions (Invariants)

These must hold across **all** evolutionary changes. See [Governance Charter](../governance/README.md) for the definitive list.

| ID | Invariant |
|----|-----------|
| FF-01 | UID stability — 11-char identifiers never change meaning |
| FF-02 | Domain isolation — BCs cannot import each other's code |
| FF-03 | Contract backward compatibility — V1 consumers never break |
| FF-04 | Event immutability — persisted events are append-only |
| FF-05 | Idempotency — reprocessing produces the same result |
| FF-06 | Configuration isolation — per-scenario config never bleeds |
| FF-07 | Audit traceability — every state change has a correlation chain |

---

## 4. Business Capability Map

High-level alignment of business capabilities to architectural components.

```
┌─────────────────────────────────────────────────────────────────┐
│                    DATARUN PLATFORM                             │
│                                                                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │   DATA CAPTURE    │  │  FORM CONFIG      │  │  OPERATIONS   │  │
│  │                  │  │                  │  │              │  │
│  │ • Submit data    │  │ • Design templates│  │ • Teams      │  │
│  │ • Offline sync   │  │ • Define rules   │  │ • Assignments│  │
│  │ • V1/V2 API     │  │ • Field types    │  │ • Org units  │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
│                                                                 │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │  IDENTITY (SSO)   │  │  EVENT GATEWAY    │  │  WEB FRONTEND │  │
│  │                  │  │                  │  │              │  │
│  │ • JWKS / RS256   │  │ • Outbox relay   │  │ • Admin UI   │  │
│  │ • User registry  │  │ • Mapping engine │  │ • Data review│  │
│  │ • Auth tokens    │  │ • Reliable deliv.│  │ • V2 consumer│  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓ Events / API
┌─────────────────────────────────────────────────────────────────┐
│                 DOWNSTREAM DOMAIN PLATFORM (LMIS)               │
│                                                                 │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────┐ ┌──────────┐ │
│  │  ADAPTER/ACL  │ │   LEDGER     │ │  KERNEL    │ │   BFF    │ │
│  │              │ │              │ │            │ │          │ │
│  │ • Translate  │ │ • Accounting │ │ • Nodes    │ │ • Compose│ │
│  │ • Validate   │ │ • Events     │ │ • Policies │ │ • Reads  │ │
│  │ • Route      │ │ • Approvals  │ │ • Items    │ │          │ │
│  └──────────────┘ └──────────────┘ └────────────┘ └──────────┘ │
│                                                                 │
│  ┌──────────────┐ ┌──────────────┐                              │
│  │  CASE MGMT    │ │  ANALYTICS   │   ← Future BCs             │
│  │  (Future)    │ │  (Future)    │                              │
│  └──────────────┘ └──────────────┘                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. Phased Transition Strategy

### Phase 0 — Current: Stabilize the API Surface
- **What:** Freeze V1 for mobile. Build V2 Clean Room API. Establish governance (ADRs, fitness functions).
- **Outcome:** Stable external contract. Internal refactoring can happen freely behind V2.
- **Key ADRs:** ADR-001 through ADR-010

### Phase 1 — Configuration-Driven Domain Overlays
- **What:** Ensure that `data_template` + `assignment` configurations define complete business scenarios. Different programmes' configurations coexist without code changes.
- **Outcome:** New use cases (programmes, workflows) are deployed via metadata, not code.
- **Key Concern:** FF-06 (Configuration Isolation)

### Phase 2 — Scenario Isolation
- **What:** Introduce scenario-scoped data boundaries. Policies, registries, and templates are scoped to scenarios. The Kernel's Policy Engine resolves per-scenario overrides.
- **Outcome:** Multiple independent scenarios run on a single deployment. Configuration never bleeds.
- **Prerequisite:** Phase 1 complete. Policy Engine supports scenario-level resolution.

### Phase 3 — Orchestration Layer (If Needed)
- **What:** Introduce a formal orchestration mechanism (state machine, workflow engine, or eventually BPMN) only when domain complexity demands it.
- **Outcome:** Complex multi-step business processes are managed declaratively.
- **Decision Trigger:** When simple if/then configuration can no longer express the business rules.

> [!IMPORTANT]
> Each phase is a checkpoint, not a deadline. The system should be **production-usable at every phase boundary**. There is no "it works when Phase 3 is done."

---

## 6. AI Agent Instructions

When an AI agent loads this document:

1. **Use it for strategic context only** — it explains the *direction*, not the *implementation*
2. **Check ADRs** for actual decisions that constrain code changes
3. **Check BC-specific docs** for implementation details
4. **Never implement Phase 1/2/3 features** unless explicitly instructed — the system operates in Phase 0
5. **Flag** any code change that would violate the fitness functions listed above

---

## Related Docs

| Topic | Document |
|-------|----------|
| System Overview | [system-overview.md](system-overview.md) |
| Context Map | [context-map.md](context-map.md) |
| C4 Model | [c4-model.md](c4-model.md) |
| Governance | [Living Architecture Charter](../governance/README.md) |
| ADRs | [All ADRs](../adrs/) |
| Integration Contract | [DatarunAPI Contract](integration-contract-datarunapi.md) |
