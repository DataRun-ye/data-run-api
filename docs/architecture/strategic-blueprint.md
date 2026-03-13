# Strategic Blueprint — Datarun Platform

> **Status:** Active — Living Document
> **Scope:** Strategic Alignment (L1)
> **Ground Truth:** The `datarunapi` codebase is the source of truth for current state.

## Quick Reference
| Target | Vision | Metric |
| :--- | :--- | :--- |
| **Domain Isolation** | Zero domain-specific code in the core engine. | FF-02 |
| **Config Over Code** | New scenarios deployed via metadata. | FF-06 |
| **Evolution Path** | Incremental "Strangle" of legacy via ACLs. | FF-03 |

---

## 1. The North Star: Contextual Orchestration

The platform's goal is to move from **Static Record Management** to **Contextual Orchestration**. 

In the North Star, the Datarun Platform is a generic backbone where:
1.  **Business Logic is Metadata:** Every process is a configuration, not a feature.
2.  **Identities are Fluid:** A user's role and behavior are "projected" based on the scenario, not hard-coded in the database.
3.  **Domains are Isolated:** Multiple functional programs (LMIS, Malaria, Surveys) coexist seamlessly on a single engine.

---

## 2. The 80/20 Rule of Materialization

Architecture efficiency is governed by the boundary between Generic and Functional logic:
- **80% (Generic Engine):** Structure, Registry, Capture, and Policy. Fully configuration-driven.
- **20% (Functional Adapter):** Vertical accounting, domain-specific math, and external ledger sync. Code-driven downstream.

---

## 3. Evolutionary Principles

| Principle | Meaning |
| :--- | :--- |
| **Modular Core** | Strict BC boundaries (FF-02). No leaking internals. |
| **Vocabulary Isolation** | The engine never contains "Medicine", "Doctor", or "Stock". |
| **Disposable Documentation** | Docs define intent; code defines reality. |
| **Continuous Integration** | System is production-ready at every small refactoring step. |

---

## 4. The Trajectory (The Evolutionary Step)

We do not perform "Big Bang" rewrites. We follow the **Strangler Pattern**:
1.  **Wrap:** Create a clean North Star API/Model (The Projection Layer).
2.  **Bridge:** Map the clean model to the legacy "Mud" via an internal ACL.
3.  **Refactor:** Progressively swap legacy implementation for Pure Vision registries behind the ACL.

---

## 5. Fitness Functions (Invariants)

These invariants must hold across all evolutionary changes.

- **FF-01:** UID Stability (11-char UIDs).
- **FF-02:** Domain Isolation (Zero BC cross-imports).
- **FF-03:** Contract Backward Compatibility (V1 Mobile stays alive).
- **FF-06:** Configuration Isolation (Zero scenario bleed).

---

## 6. AI Agent Instructions

When an AI agent loads this document:

1. **Strategic Context Only:** This document defines the *Trajectory* (The What/Why). Implementation details for "Legacy Adapters" are in `_deprecated/`.
2. **Prioritize the Metaphor:** Always interpret requirements through the **Contextual Projection** lens. Do not add fields to legacy tables; define new Registry attributes.
3. **Respect the 80/20 Boundary:** If asked to implement a feature, identify if it is "Engine Configuration" (80%) or "Vertical Accounting" (20%). Never leak 20% logic into the core engine.
4. **Guard the Fitness Functions:** Flag any proposal that violates **FF-02** (Domain Isolation) or **FF-06** (Configuration Isolation).
5. **Evolutionary Thinking:** Always propose the "Hollow Shell" first. Never suggest a "Big Bang" rewrite of legacy services.

---

## Related Docs
- [System Overview](system-overview.md) (L2 Component Model)
- [Context Map](context-map.md) (DDD Relationships)
- [Living Architecture Charter](../governance/README.md) (Governance)
