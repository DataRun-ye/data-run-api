# Submission feed

Status: DESIGNED, NOT BUILT. Contract version 1 is proposed and reviewed (2026-11-25). The feed is built after the
submission normalizer it shares with submission review (on `develop`), in a release after 6.5.6, and ships switched
off.

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
