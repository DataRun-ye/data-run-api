# Exploratory Ideas Archive

> **Status:** Archived — these documents describe **unimplemented systems** and exploratory concepts.
> **Do NOT treat these as architectural constraints.** They are preserved as reference material for future design decisions.

## What's Here

These documents were created during exploratory architecture sessions for a planned downstream platform (LMIS, Python/FastAPI). They describe systems, patterns, and components that **do not exist in code**.

## AI Agent Instructions

> [!CAUTION]
> **Never load documents from `_ideas/` unless explicitly instructed by the user.**
> These docs use present-tense, authoritative language that could be mistaken for descriptions of real systems. They are not. The only operational codebase is `datarunapi` (Java/Spring Boot).

## Contents

| Directory | Explored Concept |
|-----------|-----------------|
| `adapter/` | Anti-Corruption Layer pipeline (3-layer: Ingestion→Transform→Egress) |
| `ledger/` | Event-sourced inventory accounting (Idempotency Guard, Approval Gatekeeper, Event Store, In-Transit Registry) |
| `kernel/` | Shared Kernel registries (Nodes, Commodities, Policy Engine) |
| `composition/` | BFF composition layer for multi-BC read aggregation |
| `frontend/` | LMIS Angular 19+ SPA architecture |
| `gateway/` | Standalone data delivery product |
| `architecture/` | Cross-cutting concepts (configuration hierarchy, transaction types) |
| `datarunapi/` | V2 contract discussion, gateway integration contract |
