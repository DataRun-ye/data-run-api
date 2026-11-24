# Data-chain reconciliation check

A read-only check of the data chain: stored submissions, the outbox, the tall
table and events, and the pivot tables. It counts every known inconsistency
along that chain. It is step A1 of the data-chain plan and the gate for the
later repair steps: each step must show its own numbers fall as expected and
nothing else rise.

It prints counts only. No names or other values leave the database; the only
identifiers it prints are template uids and pivot table names (with
`--detail`).

## Running it

Staging (the default):

```bash
scripts/integrity/check.sh            # one line per check
scripts/integrity/check.sh --detail   # also each count split by template or pivot table
```

To gate a repair step, save a run before the step and one after, then compare:

```bash
scripts/integrity/check.sh > before.txt
# ... deploy the step to staging, re-project, etc.
scripts/integrity/check.sh > after.txt
diff before.txt after.txt
```

The summary goes to stdout and is stable between runs on unchanged data.
Timings and the target go to stderr.

Production only on the owner's go:

```bash
DATARUN_INTEGRITY_PRODUCTION=true scripts/integrity/check.sh --target production
```

Hosts and databases follow the reference scripts: `DATARUN_STAGING_DB_SSH`
(default `nmcp@product-staging-db.lab`), `DATARUN_STAGING_DATABASE`
(`datarun_staging`), `DATARUN_PRODUCTION_DB_SSH` (`nmcp@datarun-prod-db.lab`)
and `DATARUN_PRODUCTION_DATABASE` (`nmcpdb`).

## How it stays safe

- It first lists the templates in a short read-only transaction. All checks
  then run in one `BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY`
  that ends in `ROLLBACK`, so all counts come from the same snapshot and nothing
  can be written.
- One heavy query once stopped the whole staging database (2026-11-23), so the
  transaction sets `work_mem = '8MB'`, `max_parallel_workers_per_gather = 0`,
  `statement_timeout = '5min'`, `jit = off`, `lock_timeout = '10s'` and
  `idle_in_transaction_session_timeout = '2min'`. Statements run one at a time.
- The payload walks run one template at a time (`payload-by-template.sql`, once
  per template), and the pivot checks one pivot table at a time.
- Before any query, it reads the database host's available memory and stops if
  it is below 1024 MB (`DATARUN_INTEGRITY_MIN_AVAILABLE_MB`).
- The SQL is copied to the database host first and run there with
  `sudo -n -u postgres psql`, as the reference scripts do, so a stalled tunnel
  cannot leave the transaction waiting for input. The copy is removed afterwards.

On staging (2026-11-24, about 66,000 submissions) a full run takes about a
minute, almost all of it in the payload walks.

## Files

| File | Checks |
| --- | --- |
| `check.sh` | the runner |
| `sql/01-outbox.sql` | 1 |
| `sql/04-voided.sql` | 4 |
| `sql/05-empty-submissions.sql` | 5c |
| `sql/08-pivots.sql` | 8 |
| `sql/09-version-labels.sql` | 9 |
| `sql/10-etl-lag.sql` | 10 |
| `sql/payload-by-template.sql` | 2, 3, 5a–5b, 6, 7 |

Each SQL statement returns rows of `key|count|description`, optionally with a
fourth column saying where (a pivot table); the runner adds the rows up per key.

## The checks

"Active" means a submission with `deleted = false`. "Should stay 0" means any
rise is a new problem.

### 1. The outbox against the stored submissions

Every change to a submission should leave an outbox event in the same
transaction. The ETL learns about changes only through these events.

- **1a** Submissions with no outbox event at all. They were stored before the
  outbox existed (April to September 2025) and were never backfilled, so tall
  and events do not have them either (see 5a).
- **1b** Deleted submissions whose latest event is not a DELETE. Today these are
  the 48 deleted by hand-written SQL on 2025-11-27; their latest event is the
  later BACKFILL. Falls when events carry a type that matches the state (plan
  step C1).
- **1c** Active submissions whose latest event is a DELETE. Should stay 0.
- **1d** Submissions changed after their latest event: the stored payload
  differs from the latest event's payload, or `last_modified_date` is later.
  Should stay 0; a rise means some write skipped the outbox (the generic PUT, or
  SQL).
- **1e, 1f** UPDATE events that changed nothing (same payload as the event
  before), and their submissions. Noise from identical retries, which stopped on
  2026-11-18. Should not rise.

### 2. Removed repeat rows still projected

When a re-send drops a repeat row, the ETL only inserts and updates, so the
removed row's event stays active and its tall rows stay.

- **2a** Repeat events still active although their row is gone from the
  submission.
- **2b** Tall rows still present although their repeat row is gone.
- **2c, 2d** The submissions and templates concerned.

All fall to 0 when the projector replaces a submission's whole projection
(plan step D1).

### 3. Stale values left by an edit

- **3a** Tall values, on the submission itself or on a repeat row that still
  exists, that the current submission no longer carries: a field removed or
  cleared in an edit, or a repeat stored empty (`[]` on the submission) that
  later got rows.
- **3b** Of 3a, the field is still there but now null or empty. The ETL skips
  null and empty values, so a cleared field keeps its old value in tall.
- **3c, 3d** Tall rows and events whose village, assignment, team or activity
  differs from the submission's. A re-projection gives the rows it rewrites the
  new context; rows left behind keep the old one.
- **3e** Events whose `last_modified_by` differs from the submission's. Events
  keep the author of their first projection, so whoever changed a submission
  later never shows downstream.

All fall to 0 with plan step D1, which records who made each change.

### 4. Voided (deleted) submissions

- **4a** Tall rows of deleted submissions. Tall has no delete marker (its
  `is_deleted` column was dropped on 2026-02-11), so every such row counts and
  readers must join back to the submission to exclude it. Falls when D1 marks
  voids in tall; at that point change 4a to count only rows the marker misses.
- **4b** Deleted submissions that still have tall rows.
- **4c** Events of deleted submissions not marked deleted. Should stay 0.
- **4d** Events marked deleted although their submission is active (a restore
  that never reached events). Should stay 0.
- **4e, 4f** Tall rows and events whose submission no longer exists. No
  application path removes a submission row (deletes are soft), so these catch
  rows removed some other way, such as hand-written SQL. Should stay 0.

### 5. Never projected

- **5a** Active submissions that hold data but have no projection (no root
  event).
- **5b** The values with a field definition that those submissions hold: what a
  re-projection would add to tall.
- **5c** Active submissions with no data at all (`form_data` null). There is
  nothing to project, so they are not a projection gap.

5a and 5b fall to 0 with the supported re-projection (plan step D2).

### 6. Field coverage

The ETL projects only the paths listed in `canonical_element.json_data_paths`
for the submission's template.

- **6a** Values with no field definition. They never reach tall, events or
  pivots. Falls with plan step D3.
- **6b** Repeat rows with no event, because nothing in their section has a field
  definition.
- **6c** Submissions whose data matches none of their form's field definitions.
  The one case on staging (2026-11-24) holds another form's data since a re-send
  in July 2026: before the guard in `6e4d43b3` (2026-11-20), an upload could
  overwrite a submission of another form that had the same uid. Should not rise.

### 7. Values that did not reach tall as they are

- **7a** Values or repeat rows with a field definition that tall lacks, in
  submissions that were projected. This happens when a definition is published
  after a submission was projected, because nothing re-projects. Should stay 0
  once D2 re-projects on definition changes.
- **7b** Values in repeat rows that have no `_id`. The ETL keys a row's values by
  the row's `_id`; without one they land on the submission's own key, so only
  the last value of each field survives, and it looks like a value of the
  submission itself.
- **7c** The submissions concerned.
- **7d** Values whose tall copy differs from the submission. Should stay 0: what
  reaches the ETL is projected correctly.
- **7e** Reference, not a problem: all values with a field definition in active
  submissions. It is the denominator for 6a and 7a–7d.

### 8. Pivot tables

Pivot tables (`pivot.fact_<template code>`) are snapshots built by hand from
events. Each build renames the previous table to `<name>_old`; those copies are
not counted. "Active events" are events with `deleted_at` null.

- **8a** Active events missing from their template's pivot table: events created
  after its last build. `--detail` shows which tables are old.
- **8b** Pivot rows that are not active events: voided rows a build kept, or
  events that are gone.
- **8c** Templates with active events but no pivot table under their current
  name (never built, or the table is missing).
- **8d** Pivot tables under a name no template has now: the template's code
  changed after the build.
- **8e** Manifest entries marked success whose table does not exist.
- **8f** Manifest entries whose name differs from the current naming. The
  manifest keeps the name of the first build.
- **8g** Active pivot rows of repeat rows removed from their submission: 2a
  carried into the pivots. Note that once D1 marks removed rows in events, these
  rows move to 8b until the pivots are rebuilt.

### 9. Form-version labels

Before server 6.5.0, uploads were labelled with the form's current version
instead of the version filled in (fixed in `e033dcd5`). The entry's start time
from the phone is compared with each version's creation time.

- **9a** Provably wrong: the label is newer than a version that already existed
  when the entry started.
- **9b** Cannot be checked: the label is newer than the entry, but the entry
  started before the form had any stored version (the first versions were
  recorded on 2025-06-24). This is not the same as wrong.
- **9c** Labels that match no stored version. **9d** Entries with no start time.

Only a deliberate label repair (the owner's call) changes 9a and 9b. New
uploads should never add to them.

### 10. ETL lag

- **10a** Outbox rows waiting for the ETL.
- **10b** The age of the oldest waiting row, in seconds (0 when none wait).
- **10c** Outbox rows marked failed. The ETL never marks failures today (a
  failing row stays pending and is retried every minute), so watch 10a and 10b.

## How the payload checks read a submission

`payload-by-template.sql` mirrors the ETL's path resolution
(`TransformServiceRobust`), so its expected projection can be compared with
tall row by row:

- a path is section and field names joined by dots; repeat rows add no segment;
- an array is a repeat when one of its first 10 items is an object, and each
  item is then a row; any other array (a multi-select, or an empty array) is one
  node;
- JSON null and `""` are skipped; any other node at a field definition's path is
  projected, even an empty array or a plain section object;
- the projection key is the nearest repeat row's `_id`, or the submission uid
  outside rows or when the nearest row has no `_id`;
- when several nodes share a key and a field, the last one is kept, as in the
  ETL.

A **value** is a non-empty string, a number, a boolean or a non-empty
multi-select. Keys starting with `_` (`_id`, `_index`, `_parentId`,
`_submissionUid`, `_status`) are system keys, not values.

If the ETL's resolution rules change, change this file with them.

## Baseline: staging, 2026-11-24

Staging held the production clone of 2026-11-20 plus the household lists
loaded on 2026-11-23. The investigation numbers are from the three read-only
data-chain investigations of the same day, on the same data.

| Key | Staging | Investigation | Note |
| --- | ---: | ---: | --- |
| 1a | 4,628 | 4,628 | |
| 1b | 48 | 48 | |
| 1c | 0 | 0 | |
| 1d | 0 | 0 | |
| 1e, 1f | 1,573, 588 | 1,573, 588 | |
| 2a, 2b | 429, 1,845 | 429, 1,845 | |
| 2c, 2d | 71, 5 | 71, 5 | |
| 3a | 3 | 2 | the investigation counted fields on the submission itself; the third is a field removed inside a repeat row that still exists |
| 3b | 0 | 0 | |
| 3c, 3d | 19, 7 | 19, 7 | one submission, all inside 2b and 3a except its root event |
| 3e | 7 | 7 | the same submission's events: it was re-sent by another user |
| 4a, 4b | 460, 48 | 460, 48 | |
| 4c | 0 | 0 | (70 events marked) |
| 4d, 4e, 4f | 0, 0, 0 | | not measured before |
| 5a, 5b | 4,628, 56,925 | 4,628, 56,925 | |
| 5c | 2 | | not measured before |
| 6a | 18,485 | 18,485 | |
| 6b | 699 | 699 | |
| 6c | 1 | | not measured before: a line-items submission holding another form's data since a July 2026 re-send |
| 7a | 0 | 3,682 | the investigation's 3,682 are the values counted in 7b, not values lost to a definition change |
| 7b, 7c | 3,682, 71 | | |
| 7d | 0 | 0 | |
| 7e | 1,585,896 | 1,585,896 | |
| 8a | 6,646 | | per table as the investigation: 3,312, 412, 64 and 2,239 in the four old pivots; the pivots rebuilt on 2026-11-19 miss only later events |
| 8b | 70 | 70 | voided rows in `fact_lsm` |
| 8c | 3 | | not measured before |
| 8d, 8e, 8f | 3, 2, 3 | 3, 2, 3 | |
| 8g | 409 | 409 | |
| 9a, 9b | 402, 13,814 | 402, 13,814 | |
| 9c, 9d | 0, 0 | | |
| 10a, 10b, 10c | 0, 0, 0 | 0 pending or failed | |

## Not included

- How stale the materialized views `events_enriched` and `event_ancestors` are:
  restoring a dump recomputes them, so staging cannot show it.
- The Airbyte copy (`datarun_meta`) and the analytics schemas downstream of it.
- Option values with no reference uid (measured in the investigations): the
  stored code matches no option, which is a question for server-side
  validation, not for the chain.
