# Architecture Overview

## Overview
This document outlines the proposed Bounded Contexts (BCs) for refactoring the data collection platform into a modular, maintainable architecture. Based on DDD principles, it separates concerns into 9 BCs, enabling flexibility, scalability, and incremental evolution toward the future vision of core tools + domain overlays.

## Key Principles
- **Modularity**: Each BC is independently deployable, with clear boundaries.
- **Event-Driven Coupling**: Loose coupling via events/APIs; no shared databases.
- **Meta-Based Flexibility**: JSON configs for dynamic rules and relationships.
- **Scalability**: Supports microservices evolution; CQRS for read/write separation.

## Proposed Bounded Contexts
1. **IAM BC**: User identity and access.
2. **Org Structure BC**: Hierarchical governance.
3. **Team & Collaboration BC**: Team management.
4. **Form Template Design BC**: Schema definitions.
5. **Campaign & Assignment Planning BC**: Planning logic.
6. **Data Collection & Submission BC**: Runtime execution.
7. **Reference Data & Options BC**: Canonical data.
8. **Analytics & Reporting BC**: Pivots and reports.
9. **Domain Entity Registry BC**: Dynamic entity management.

## Benefits
- Addresses current difficulties (e.g., scattered logic, tight coupling).
- Enables dynamic entities as subjects in data.
- Scales with future requirements via overlays.

## Next Steps
- Review RFCs with experts.
- Prototype integrations.</content>
<parameter name="filePath">/home/hamza/data-run-api/docs/architecture-overview.md