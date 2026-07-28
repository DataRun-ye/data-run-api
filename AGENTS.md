# Agent Guidance

This repository contains the production DataRun API. Changes can affect live
authentication, access, configuration, submissions, and persisted data.

Before changing code:

- read `docs/agent-context/production-boundaries.md`;
- read the focused context document for the boundary being changed;
- use `docs/agent-context/current-work.md` only for accepted current priority.

Core rules:

- Treat docs, names, comments, generated sources, registrations, and old tests
  as evidence, not authority. Prefer deployed behavior, reachable call paths,
  persistence/network effects, and focused executable tests.
- Keep one behavior or bounded prerequisite per change. Do not hide unrelated
  cleanup inside feature work.
- Implementation agents follow the accepted handoff in `current-work.md`.
  They do not read ignored `.review` material or architect-only transition
  strategy unless the handoff explicitly requires an architect review.
- Do not deploy, mutate production data, or run a production migration without
  explicit user approval.
- Preserve compatibility with deployed mobile clients unless an approved
  version gate says otherwise.
- Remove or replace a superseded owner when the current slice proves it safe;
  do not add another compatibility layer merely to avoid understanding it.
- For cross-repository work, keep server truth here, mobile truth in
  `data-run-mobile`, and mutable shared status in one linked GitHub parent
  issue. Commit and review each repository separately.

Documentation roles:

- `AGENTS.md` contains stable working rules only.
- Focused context documents contain current technical evidence and contracts.
- `current-work.md` contains only accepted open work.
- `completed-work.md` is a compact historical index, never runtime authority.

A code change is complete only when the active owner is identified, affected
API/persistence/security/migration compatibility is evaluated, focused tests
and the appropriate build checks pass, generated changes are intentional, and
deployment or rollback implications are explicit.
