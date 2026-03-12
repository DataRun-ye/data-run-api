# ADR-010: Evolutionary Architecture — Configuration Isolation Strategy

> **Status:** Accepted
> **Date:** 2026-03-12
> **Scope:** Cross-cutting — all Bounded Contexts
> **Supersedes:** The "Not multi-tenant" constraint in `system-overview.md` (line 64)

## Context

The system was originally designed as a single-programme deployment: *"Not multi-tenant. Single deployment per health programme."* This constraint served us well during initial development — it kept scope manageable.

However, we've identified a strategic risk: the **"not multi-tenant" trap**. Without deliberate configuration isolation, the system accumulates hard-coded assumptions about a single programme's workflow. When a second programme needs to be supported, the only option becomes duplicating the deployment — leading to version drift, maintenance burden, and divergent codebases.

The solution is not to implement full SaaS multi-tenancy (which would be premature). Instead, the system should adopt **Evolutionary Architecture** principles that make configuration isolation a first-class concern, allowing the implementation to evolve toward multi-scenario support incrementally.

## Decision

1. **Adopt Evolutionary Architecture** as the primary design strategy. Protect fitness functions (see [Governance Charter](../governance/README.md)); allow everything else to change.

2. **Reframe the multi-tenant constraint**: The system is **not multi-tenant**. It is **configuration-isolation-ready**. This means:
   - Configurations (templates, assignments, policies) should be able to coexist without interfering
   - No hard-coded assumptions about "the one programme" in domain logic
   - The Policy Engine's resolution hierarchy (global → node type → specific node) already supports this pattern

3. **Defer orchestration complexity**. Simple conditional logic and the existing PolicyResolver are sufficient until domain requirements genuinely demand a formal workflow engine.

4. **Establish fitness functions** as the guardrails for all future changes. Every ADR and RFC must declare which fitness functions it affects.

5. **Documentation is disposable**. The codebase is ground truth. Docs describe intent and rationale. When they conflict with code, update the docs.

## Fitness Functions Affected

| ID | Function | Impact |
|----|----------|--------|
| FF-06 | Configuration Isolation | **Established** — this ADR creates this fitness function |
| FF-01 | UID Stability | Protected — UIDs remain programme-agnostic |
| FF-02 | Domain Isolation | Reinforced — BCs must not contain programme-specific assumptions |
| FF-03 | Contract Backward Compat | Protected — V1/V2 contracts remain scenario-agnostic |

## Consequences

### Positive
- Prevents the "not multi-tenant" trap without premature complexity
- Provides clear decision framework (fitness functions + ADRs)
- Enables incremental evolution — each phase is production-usable
- Documentation governance prevents knowledge loss during AI-assisted development

### Negative
- Requires discipline: every change must be evaluated against fitness functions
- "Configuration-isolation-ready" is harder to explain than "just multi-tenant" or "just single-tenant"
- The strategic blueprint is aspirational — the current codebase is Phase 0

## Notes

- See [Strategic Blueprint](../architecture/strategic-blueprint.md) for the full phased transition strategy
- The `system-overview.md` "Not multi-tenant" line should be updated to reference this ADR
- ADR-009 (Feature Flags Deferred) already mentions "revisit when the system moves to multi-tenant" — this ADR provides that context
