# Submission review

Status: DESIGNED, NOT BUILT (2026-11-25). The v2 conventions it uses are on `develop`
([api-v2.md](../../agent-context/api-v2.md)), and the revision journal that records its changes is in production
since 6.5.6.

Submission review is how operations search, check and change the submissions of any form after phones send them:
search, view, history and notes; edits with a required reason; moves and splits; delete and restore; and the
conflicts that arise when a phone sends a changed copy after a change on the server. Every change becomes a new
revision with who, when and why. The admin's tools use the v2 API first; a screen for operations follows (owner,
2026-11-25).

## Files

| File | Role |
| --- | --- |
| [CONTEXT.md](CONTEXT.md) | The glossary: the words this feature uses, their API spellings, and the words it avoids. New code and docs use these words. |
| [spec.md](spec.md) | The specification: review cases, constraints, requirements, capabilities, move and split, invariants, concurrency, conflicts, permissions, API conventions, and the use of the journal and the feed. |

Related: the [submission feed](../submission-feed/README.md), through which changes reach downstream analytics, and
the revision journal in [active-production-interface.md](../../agent-context/active-production-interface.md).

There are no ADRs. An ADR records only what a spike settled (engineering rule), and this feature has no spike: its
risks are tested as invariants (spec §5), and search is measured at scale in build step 6.

## Build order

What each step proves is in [spec.md §12](spec.md#12-build-order-and-activation).

0. The revision journal in production (6.5.6) and the v2 foundation (done).
1. The submission normalizer, which reads stored submissions by row id and field id with typed values (on
   `develop`), and the write direction back to stored places.
2. Upload-path safety, in its own release: the submission lock first; conflict detection shipped switched off; the
   ETL's split re-attribution.
3. Reading: access grants, search, view, history, the form dictionary, notes.
4. Changes inside one submission: edits, delete and restore, conflict decisions, validation, dry run, the journal
   changeset.
5. Move and split.
6. A scaled search measurement.
7. After activation: the v1 admin delete retired.

## Activation

Deployment and activation are separate. Nothing is on by default, and each gate is switched on at the owner's go.

| Gate | Turns on | When |
| --- | --- | --- |
| First | Conflict detection | Before any change; users see nothing new |
| A | Search, view, history, notes; `setValue`, `setEntry`, `restoreRow`, move, split, delete, restore, conflict decisions | With the capability; split once the ETL's split re-attribution is in production |
| B | `clearValue`, `removeRow` | Server-wide, once downstream confirms in writing that no model reads tall and events; the ETL never removes a value or a row |
| Then | The v1 admin delete retired | Once `:delete` is on and the logs show its callers have moved |

## Not built, by decision (owner, 2026-11-25)

- Review status, or any approval workflow. Marks can be added later as data (spec §3.4).
- Moving rows into an existing submission. A split makes a new one.
- A feedback workflow. Feedback is plain notes.

## Later, by need

Marks as data; `addRow`; creating a submission through the API; area, form and team scopes; a server-owned read
model for search; the operations screen, with its browser login.
