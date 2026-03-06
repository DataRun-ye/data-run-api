# DatarunAPI Frontend — Architecture Decisions & Implementation Plan

> **Status:** Final Plan (Pre-Implementation)  
> **Audience:** Frontend, Backend, Architecture  
> **Date:** 2026-03-06

This document captures the final agreed-upon architecture for the **DatarunAPI Web Frontend** before any scaffolding or code is written.

## 1. What Are We Building?

We are building a standalone **Angular 19+ Single Page Application (SPA)** that serves as the official web frontend for the `datarunapi` Bounded Context. 

**Core Domains Handled by this UI:**
1. **Data Capture:** Rendering dynamic forms based on the V2 Template Tree and capturing user input into normalized V2 Submissions.
2. **Review & Analytics:** Browsing, searching, and pivoting on submitted data.
3. **Administration:** Designing form templates, managing users, teams, and assignments.

**What this app explicitly excludes:**
- **Ledger/Inventory:** Handled by the LMIS SPA.
- **Adapter Ingestion:** Handled by the LMIS SPA.
- **Micro-Frontend/Portal:** This is a standalone app linked over SSO, not a micro-frontend shell.

## 2. Technical Stack & Paradigms

| Concern | Decision | Rationale |
|---|---|---|
| **Framework** | Angular 19+ (Standalone Components) | Strict DI enforces architectural boundaries smoothly. |
| **State Management** | Native Angular Signals | Eliminates NgRx/RxJS boilerplate for synchronous UI state. Native reactivity. |
| **API Integration** | Standard `HttpClient` | Typed DTO interfaces strictly enforcing OpenAPI specs. No `any` types. |
| **CSS Methodology** | Component-Scoped CSS | No global utility frameworks unless explicitly requested. Each component encapsulates its own styles. |
| **Authentication** | RS256 JWT via SSO | DatarunAPI backend issues JWT via cookie/header. UI forwards it. No heavy Auth0/OIDC UI libraries needed. |

## 3. The "Clean Architecture" Layering Model

The project structure will strictly follow Clean Architecture principles to ensure the UI is purely a reflection of the domain logic, decoupled from the framework where possible.

### Dependency Flow:
`Presentation (Components)` → `Application (Services/State)` → `Infrastructure (API)` → `Domain (Types)`

### Folder Structure Blueprint

```text
src/
├── app/
│   ├── core/                  # Singletons: Auth guards, HTTP Interceptors (JWT injection, Error handling), App Layout
│   ├── shared/                # "Dumb" highly reusable UI: Buttons, Data Tables, Typography, Pipes
│   ├── domain/                # Pure TypeScript: `V2SubmissionDto`, `TemplateTreeDto`, `CanonicalElementId` (Zero Angular imports)
│   ├── infrastructure/        # Implementations: `V2DataSubmissionApiClient` (implements domain interfaces using `HttpClient`)
│   │
│   └── features/              # The distinct bounded modules
│       ├── admin/             # Template designer, User roles
│       ├── data-capture/      # Smart Container `FormFillPage`, Headless Form Engine, Field Renderers
│       └── review/            # Submission grids, search filters
```

## 4. The Form Engine Architecture

The most complex feature is `data-capture`. It relies on a custom **Headless Form Engine**.

- **Why Headless?** The engine is a pure state machine (written in vanilla TypeScript or holding Signals) that manages the V2 Submission Shape (`values`, `collections`), rules, and validations. It has zero DOM dependencies.
- **How works:** 
  - The UI (Angular) passes the `TemplateTree` to the Engine.
  - The Engine exposes a `state$` (Signal) representing the normalized submission.
  - Angular dumb components (e.g., `TextFieldComponent`, `RepeaterComponent`) bind recursively to the Tree nodes, reading their value and `uiState` (hidden, required, disabled) from the Engine, and writing back via `engine.setValue(node.binding, val)`.
  - **Rules:** The engine resolves V1 logical rules using Namespaces (`_row`, `$rel`, `$global`) so rules don't bind strictly to UI layout.

## 5. Execution Plan (The Roadmap)

We will execute the frontend build in vertical, testable slices.

**Phase 1: Project Bootstrap & Core Layers**
- Scaffold Angular 19+ standalone app (`datarun-web`).
- Setup Clean Architecture directories (`core`, `domain`, `infrastructure`, `shared`, `features`).
- Define the Domain Data Contracts (`domain/models/`, `domain/interfaces/`) based on Phase 1 and 2 backend implementations (`V2SubmissionDto`, `TemplateTreeDto`).

**Phase 2: The Infrastructure Layer**
- Implement `infrastructure/api/V2TemplateApiService`.
- Implement `infrastructure/api/V2SubmissionApiService`.
- Add Mock implementations or integrate with the running local backend.

**Phase 3: The Data Capture Feature (The Core)**
- Build the `FormEngineService` state machine (Signals based).
- Build the recursive Field Renderer components in `features/data-capture/components`.
- Build the Smart Container `FormFillPage` to tie it all together.
- Test End-to-End: Load a template → Render form → Fill data → Serialize to Canonical V2 JSON → Submit to backend.

**Phase 4: Admin & Review Module Skeletons**
- Scaffold the basic routing and smart containers for Template Design and Submission Search.
