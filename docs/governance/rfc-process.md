# RFC Process

> **Scope:** Cross-cutting
> **Status:** Active

## What Is an RFC?

A **Request for Comments** is a proposal for a significant architectural change that affects multiple Bounded Contexts or multiple fitness functions. It is reviewed before any code is written.

## When to Use an RFC (vs. an ADR)

| Use an **ADR** when... | Use an **RFC** when... |
|------------------------|------------------------|
| The decision is scoped to one BC | The change crosses BC boundaries |
| There is a clear winner among options | Multiple viable approaches need discussion |
| The change is small/reversible | The change requires a multi-step migration |
| You can decide and implement in one sprint | The proposal needs input before committing |

## Lifecycle

```
DRAFT → UNDER REVIEW → ACCEPTED → IMPLEMENTED
                     ↘ REJECTED
                     
IMPLEMENTED → SUPERSEDED (by newer RFC/ADR)
```

### States

| State | Meaning | Who Acts |
|-------|---------|----------|
| **Draft** | Author is still writing. Not ready for review. | Author |
| **Under Review** | Open for comments. Can be loaded by AI agents for context. | Reviewers |
| **Accepted** | Proposal approved. Creates one or more ADRs. | Author → ADR |
| **Rejected** | Proposal declined. Rationale is documented in the RFC. | Reviewer |
| **Implemented** | Code changes are complete. RFC is now historical. | Author |
| **Superseded** | A newer RFC or ADR has replaced this proposal. | Author |

## File Location

Store RFCs in `docs/rfcs/NNN-short-title.md`. Number sequentially.

## AI Agent Usage

When an AI agent encounters an RFC reference:
1. Load the RFC file to understand the proposed change
2. Check RFC status — only `ACCEPTED` or `IMPLEMENTED` RFCs should influence code decisions
3. `DRAFT` and `UNDER REVIEW` RFCs are context-only — do not implement them without explicit instruction

## Related Docs

- [RFC Template](rfc-template.md)
- [ADR Template](adr-template.md)
- [Living Architecture Charter](README.md)
