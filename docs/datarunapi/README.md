# DataRun API V2 Strategy & Architecture Overview

> **Status:** Draft  
> **Audience:** Backend, Frontend, and Mobile Teams

## 1. The V2 Vision: A Primary "Clean Room" API Surface

The existing V1 API and its underlying service layer have accumulated technical debt, particularly through UI-coupled logic and data structures. **V2 is designed as a "Clean Room" contract** to explicitly avoid inheriting these legacy patterns. 

Currently, the Flutter mobile app is the only client consuming the V1 API. Once the mobile app migrates to V2, V1 will be deprecated, establishing V2 as the system’s sole, canonical API surface.

## 2. Stabilizing the Boundary via Anti-Corruption Layer (ACL)

The immediate goal is to stabilize the backend–frontend API boundary. To achieve this without breaking the existing database schema or the V1 mobile app, V2 employs an **Anti-Corruption Layer (ACL)**.

The ACL acts as a translator, sitting between the V2 endpoints and the legacy internal services. This pattern effectively isolates the modern web frontend from legacy technical debt, allowing the internal backend architecture and database persistence to evolve independently *behind* the V2 boundary without ever impacting V2 clients.

## 3. Domain-Aligned Bounded Contexts

V2 is not just a REST endpoint refactoring; it represents a strategic shift toward Domain-Driven Design (DDD). The V2 API is being organized into distinct functional areas that map cleanly to the business domain:

- `/api/v2/form-config/` — Template design, authoring, and rule configuration.
- `/api/v2/data-capture/` — Form rendering, data collection, and processing submissions.
- `/api/v2/operations/` — Managing users, teams, organizational units, and assignments.
- `/api/v2/gateway/` — **The Central Transformation & Delivery Engine.** Dedicated integration context for delivering data products (via Outbox events) to external consumers.

## 4. Current Implementation Focus: `data-capture`

At present, our implementation focus is squarely on the **`data-capture`** domain—specifically the frontend form renderer and submission flow—as this represents the immediate dependency for the new web client.

**Important Note on Persistence:**
Database-level schema changes are intentionally postponed. We will first define and solidify the V2 contracts and prove them in production via the ACL. We will only tackle deep database schema migrations when existing structures become undeniable blocking bottlenecks. This mitigates the risk of "big bang" rewrites.

## 5. Event Boundaries & The Gateway

The architecture defines strict event boundaries to dispatch data to other systems (e.g., analytics aggregators, central ledgers) without coupling the core application logic.
- **`data-capture`** must remain completely unaware of downstream consumers. Its only responsibility is to securely record the submission and insert a "Fat Event" into the Transactional Outbox.
- **The Application Gateway** acts as the Integration Bounded Context. It reads the Outbox, handles Configurable Idempotency, performs JSON payload mapping, and guarantees delivery (with exponential backoff) of versioned Data Contracts to the outside world.

---

### In Summary
**V2 defines the long-term, stable external contract.** It serves as a protective shell isolating API consumers from legacy structures. Moving forward, the internal implementation (services, persistence, eventing) will evolve iteratively, comfortably hidden behind that pristine boundary.
