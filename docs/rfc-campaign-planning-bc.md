# RFC: Campaign & Assignment Planning Bounded Context

## Status
Proposed - Initial Draft

## Context
Assignments link activities, teams, and org units, but are tightly coupled with submissions. The vision needs planning decoupled from execution for flexible campaigns.

## Problem
- Planning and execution bleed together, causing side effects.
- Hard to adapt assignments for complex flows.

## Proposal
Own campaign planning and assignments. Consumes events from other BCs; publishes "assignment created".

### Core Responsibilities
- Plan data collection campaigns and assign resources.

### Key Entities/Models
- `activity`, `assignment`.

### Boundaries & Dependencies
- **Inbound**: Events from Org, Team, Template BCs.
- **Outbound**: Events for assignments.
- **Flexibility**: Meta-config for assignment rules.

### Integration with Dynamic Entities
- Assignments can reference dynamic entities via rules.

## Benefits
- Separates planning from execution.
- Enables dynamic re-assignments.

## Risks
- Eventual consistency in planning.

## Open Questions
- Handling multi-party assignments?

## Next Steps
- Design assignment APIs.</content>
<parameter name="filePath">/home/hamza/data-run-api/docs/rfc-campaign-planning-bc.md