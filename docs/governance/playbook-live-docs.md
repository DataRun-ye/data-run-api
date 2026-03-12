# Playbook: Working with Live Documents

> **Scope:** Cross-cutting Governance
> **Status:** Active
> **Living Document:** Updated via the "Two-Step Waltz"

This playbook defines how agents and developers interact with the `docs/` directory. It ensures that documentation remains a **lightweight, living guide** rather than a stale burden.

---

## The Workflow Integration

Documentation lifecycle is strictly tied to the **Two-Step Waltz** execution strategy.

### Step 1: Creative Phase (Propose & Build)
*   **Action:** If the task involves a significant architectural pivot or a new pattern, draft an **ADR** (Architecture Decision Record) or **RFC** (Request for Comments).
*   **Location:** 
    *   `docs/adrs/NNN-title.md` (Scoped decisions)
    *   `docs/rfcs/NNN-title.md` (Cross-BC/strategic changes)
*   **Status:** Set to `PROPOSED` or `DRIVING`.
*   **Verification:** Ensure no **Fitness Functions (FF-01 to FF-07)** are violated.

### Step 2: Clerical Phase (Document & Commit)
*   **Action:** Sync the "Living Reality" with the code.
*   **Tasks:**
    1.  **Update ADRs:** Move `PROPOSED` ADRs to `ACCEPTED`.
    2.  **Sync BC-Specific Docs:** If a Bounded Context schema or contract changed, update the corresponding file in `docs/{bc-name}/`.
    3.  **Supersede Old Docs:** If a doc is now incorrect, do not "patch" it indefinitely. If it's fundamentally wrong, either delete it or rewrite it. If it was an ADR, create a new one that `Supersedes: [ADR-XXX]`.
    4.  **Atomic Commit:** The documentation updates **must** be part of the same commit as the code and tests.

---

## Rules for AI Agents

1.  **Context-First:** Before starting research, load the relevant index files (e.g., `docs/README.md` and `docs/governance/README.md`).
2.  **Code Over Docs:** If a document says `X` but the code does `Y`, **the code is right**. Your job is to flag the discrepancy and update the document in Step 2.
3.  **Small Chunks:** Avoid creating large new documentation files. Prefer many small, focused files that fit in a 4k-8k token context window easily.
4.  **Identify "Dead" Docs:** If you find a doc in a main folder that is outdated, proactively propose moving it to `_deprecated/`.

---

## When to Create a New Doc?

| Scenario | Tool |
|----------|------|
| "I'm changing how X works across 3 modules" | **RFC** |
| "I'm choosing Library A over Library B" | **ADR** |
| "I'm adding a new Bounded Context" | **Folder + README in `docs/`** |
| "I'm fixing a bug" | **No Doc (Update existing if needed)** |

---

## Maintenance & Pruning

*   **The "Living" Rule:** If a document hasn't been touched in 3 months and the code has evolved, it is suspect.
*   **Pruning:** During "Milestone Closeout", perform a quick audit of the docs folder related to that milestone. Delete anything that was "temporary guidance" that is now fully captured by code/tests.
