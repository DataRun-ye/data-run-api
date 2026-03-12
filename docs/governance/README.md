# Living Architecture Charter

> **Status:** Active
> **Ground Truth:** The `datarunapi` codebase is the single source of truth.
> All documentation is **disposable guidance** — a living draft that can be rewritten when the code reality changes.

---

## Purpose

This charter governs how architectural knowledge is captured, communicated, and evolved in the Datarun ecosystem. It is designed for an **AI-assisted, agentic workflow** where the primary consumers of documentation are:

1. **AI coding agents** (context-window-limited, need focused, self-contained files)
2. **The human architect/developer** (needs strategic coherence and decision traceability)

---

## Documentation Principles

### 1. Code Is Ground Truth

Documentation describes *intent and rationale*. The codebase describes *reality*. When they conflict, the code wins and the doc gets updated — never the other way around.

### 2. Context-Window Friendly

Every document should be loadable into an AI context window **independently**. Rules:
- **One concern per file** — no 500-line omnibus documents
- **Front-matter metadata** — every doc starts with a YAML-style block or markdown header stating: Status, Scope (which BC or cross-cutting), Date
- **Explicit cross-references** — use relative links, never assume the reader has other docs loaded
- **Quick Reference table** at the top of index files — so an agent can decide which file to load

### 3. Disposable Drafts

No document is sacred. Any doc can be rewritten or deleted if the code reality has moved on. The governance process (ADRs) exists to capture *why* a change was made, not to prevent change.

### 4. Tiered Documentation

| Tier | Purpose | Location | Volatility |
|------|---------|----------|------------|
| **Governance** | Process rules, templates, fitness functions | `docs/governance/` | Low — changes rarely |
| **Architecture** | Strategic vision, context map, C4, cross-cutting | `docs/architecture/` | Medium — evolves with major pivots |
| **ADRs** | Point-in-time decision records | `docs/adrs/` | Immutable once accepted (superseded, never edited) |
| **RFCs** | Proposed changes under review | `docs/rfcs/` | High — living during review, frozen after decision |
| **BC-Specific** | Per-bounded-context schemas, contracts, edge cases | `docs/{adapter,ledger,kernel,...}/` | High — tracks code changes |

---

## Fitness Functions

Fitness functions are **invariants that must hold** across all evolutionary changes. Every ADR and RFC must declare which fitness functions it affects.

| ID | Fitness Function | Verification |
|----|-----------------|--------------|
| **FF-01** | **UID Stability** — 11-char identifiers never change meaning or format | Unit test on `CodeGenerator`; migration guardrails |
| **FF-02** | **Domain Isolation** — BCs cannot import each other's classes | Package structure; ArchUnit tests (future) |
| **FF-03** | **Contract Backward Compat** — V1 consumers never break silently | Integration tests; V1 round-trip tests |
| **FF-04** | **Event Immutability** — Persisted events are append-only, never mutated | DB constraints; code review |
| **FF-05** | **Idempotency** — Reprocessing the same submission produces the same result | Idempotency guard tests |
| **FF-06** | **Configuration Isolation** — Per-scenario config never bleeds across boundaries | Policy resolver tests; schema constraints |
| **FF-07** | **Audit Traceability** — Every state change has a correlation chain | Provenance field checks |

> [!TIP]
> When writing an ADR or RFC, reference fitness functions by ID (e.g., "This change preserves **FF-03** and strengthens **FF-06**").

---

## ADR Process

**Architecture Decision Records** capture *why* a technical choice was made at a specific point in time.

### When to Write an ADR
- Choosing between two or more viable approaches
- Introducing a new pattern or dependency
- Superseding a previous decision
- Deferring a capability to a future phase

### Lifecycle
`PROPOSED → ACCEPTED | REJECTED | DEFERRED → SUPERSEDED (by newer ADR)`

### Rules
1. ADRs are **immutable** once accepted. To change a decision, write a new ADR that supersedes.
2. Number sequentially: `001`, `002`, ..., `010`, etc.
3. Store in `docs/adrs/NNN-short-title.md`
4. Use the [ADR Template](adr-template.md)

---

## RFC Process

**Requests for Comments** are for larger proposals that require discussion before becoming ADRs.

### When to Write an RFC
- Cross-BC architectural changes
- New Bounded Context introduction
- Changes that affect multiple fitness functions
- Multi-phase migration strategies

### Lifecycle
`DRAFT → UNDER REVIEW → ACCEPTED | REJECTED → IMPLEMENTED → SUPERSEDED`

See [RFC Process](rfc-process.md) for full details and [RFC Template](rfc-template.md).

---

## Review Cadence

| Trigger | Action |
|---------|--------|
| **New feature/sprint** | Check if affected docs still match code reality |
| **Major pivot** | Write an ADR. Update strategic blueprint if needed. |
| **Post-implementation** | Update BC-specific docs. Close any open RFCs. |
| **Quarterly** | Audit fitness function table — add/retire as needed |

---

## Related Docs

- [ADR Template](adr-template.md)
- [RFC Template](rfc-template.md)
- [RFC Process](rfc-process.md)
- [Strategic Blueprint](../architecture/strategic-blueprint.md)
- [Documentation Index](../README.md)
