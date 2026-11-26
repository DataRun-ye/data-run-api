# Submission feed

Status (2026-11-26): BUILT, SWITCHED OFF. The feed is built to contract version 1, proposed and reviewed on
2026-11-25, and stays off (activation switch `submission-feed`) until phase B of its rollout, when it is switched on
and its first publication runs, on the owner's go. A revision of the contract comes before that first publication
(engineering, 2026-11-26): the creator's user id, placement columns, and reference values as ids. What is built:
[Built](#built).

The submission feed publishes every submission's current state for downstream analytics: one record per submission
and one per row, keyed by ids, in their own database schema, `feed`. One publisher inside DataRun writes it in commit
order, and downstream reads it with Airbyte's incremental, deduplicated sync. It replaces tall, events and pivots as
the analytics interface; they retire once downstream has switched (owner, 2026-11-25).

## Why

- Downstream copies `data_submission` with an `updated_at` cursor, which can miss a change that commits late; the
  feed's position cannot.
- tall and events never remove a value or a row, cut long values, and split a field's values when it is renamed. The
  feed shows removals as deleted records and keys values by field id.
- Downstream wants the current state, and nobody reads who made a change (owner, 2026-11-25). History stays in
  DataRun's journal, and the feed publishes no actor and no command.
- DataRun owns its model, and downstream follows the published feed rather than internal tables (owner, 2026-11-25).

## Built

Switched off until phase B. The code is in `src/main/java/org/nmcpye/datarun/submission/feed` and `scripts/feed`.

- Schemas `feed` and `feed_ops`, with the five streams of contract version 1 and one position sequence for both
  submission streams; the reader role's grants, which converge at every start.
- The dictionary, rewritten at start and whenever a form definition changes.
- The publisher. The ETL hands it each change, which it publishes in a savepoint of the transaction the ETL applies
  that change in, so a feed failure never rolls back tall or events. A failed change is pending and is retried after
  the live backlog, until the attempt cap leaves it dead. Reconcile runs at start, which is also switch-on, and
  nightly; until the first publication has completed, it publishes no head the feed lacks and only counts them.
  Freshness counts what waits, and the heartbeat stops while the feed is behind its target or has a dead change.
- First publication and republishing a scope, started at the admin route `/management/feedpublication`
  ([active-production-interface.md](../../agent-context/active-production-interface.md)).
- Integrity check 12, which holds the feed to its contract; the agreement harness (`scripts/feed/agreement.sh`); the
  comparison queries and the crosswalk (`scripts/feed/comparison.sql`, `scripts/feed/crosswalk.sql`).

Still to come: the contract revision above, then the first publication at phase B, and the comparison queries' count
of the values downstream sees for the first time, after the revision
([contract.md, 14](contract.md#14-what-downstream-can-drop-and-the-handoff)).

## Files

| File | Role |
| --- | --- |
| [CONTEXT.md](CONTEXT.md) | The glossary: the feed's words, consistent with submission review's where they share one, and the words it avoids. |
| [contract.md](contract.md) | The contract, version 1: streams, columns, values, ids, order and delivery, deletes, evolution, freshness, Airbyte settings, the handoff, and the rollout. |

Related: [submission review](../submission-review/README.md), whose changes reach downstream through the feed and
whose clears and row removals wait for it; the revision journal, the outbox and the ETL in
[active-production-interface.md](../../agent-context/active-production-interface.md).

There is no ADR yet: the feed's build adds one for its shape, from its spike's evidence.

## Rollout, in short

Before phase B, downstream measures the gap in today's copy of `data_submission`. Phase B publishes the feed beside
tall and events; phase C moves downstream onto it; phase D retires tall, events and pivots, once downstream confirms
in writing. Details: [contract.md, Rollout](contract.md#rollout).

## Open

- Phase D (owner): whether tall, events and pivots may retire without a rollback margin of one campaign or 90 days,
  and whether a final dump may be their only archive.
- For downstream: the gap measurement, schema `feed` at phase B, and the Airbyte role's name for the grant.
