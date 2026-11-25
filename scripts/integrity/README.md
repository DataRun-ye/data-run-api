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

**Never run it during a deploy or a pivot build.** Its read locks last until
it finishes, so a migration's DDL or a pivot swap would wait behind it, and the
application behind them; and a pivot rebuilt after it started reads as empty.
It refuses to start while an exclusive lock is held or waited for, or while
Liquibase holds its changelog lock, but it cannot stop one from starting
during the run.

To gate a repair step, save a run before the step and one after, then compare:

```bash
scripts/integrity/check.sh > before.txt
# ... deploy the step to staging, re-project, etc.
scripts/integrity/check.sh > after.txt
diff before.txt after.txt
```

The summary goes to stdout and is stable between runs on unchanged data.
Timings, the guards' readings and the temporary files written go to stderr.

Production only on the owner's go:

```bash
DATARUN_INTEGRITY_PRODUCTION=true scripts/integrity/check.sh --target production
```

Hosts and databases follow the reference scripts: `DATARUN_STAGING_DB_SSH`
(default `nmcp@product-staging-db.lab`), `DATARUN_STAGING_DATABASE`
(`datarun_staging`), `DATARUN_PRODUCTION_DB_SSH` (`nmcp@datarun-prod-db.lab`)
and `DATARUN_PRODUCTION_DATABASE` (`nmcpdb`).

Exit codes: 0 done, 2 usage or settings, 3 a guard refused to start, 4 psql
failed (a query error, a timeout or a lost connection), 5 a line is missing or
the output was not understood.

## How it stays safe

- Before any query it reads the database host and stops (exit 3) when:
  - less than 1024 MB of memory is available
    (`DATARUN_INTEGRITY_MIN_AVAILABLE_MB`); one heavy query once stopped the
    whole staging database (2026-11-23);
  - the disk holding the data directory has less than 10 GB free
    (`DATARUN_INTEGRITY_MIN_FREE_DISK_MB`, default 10240); sorts and hashes
    that pass `work_mem` spill to temporary files there;
  - an `AccessExclusiveLock` is held or waited for on a table in `public`,
    `analytics`, `pivot`, `journal`, `feed` or `feed_ops`, or on a table not
    committed yet (the long `CREATE TABLE ..._new AS` of a pivot build), the
    sign of a migration or a pivot build;
  - `databasechangeloglock.locked` is true: a migration is running, or stopped
    between changesets.
- It lists the templates in a short read-only transaction (1-minute statement
  timeout, 5-second lock timeout), and stops if there are none.
- All checks then run in one `BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY`
  that ends in `ROLLBACK`, so all counts come from the same snapshot and nothing
  can be written. The transaction sets `work_mem = '8MB'`,
  `max_parallel_workers_per_gather = 0`, `statement_timeout = '5min'`,
  `lock_timeout = '5s'`, `idle_in_transaction_session_timeout = '2min'`,
  `jit = off` and `temp_file_limit = '1GB'` (superuser-only, so set first), then
  `SET LOCAL ROLE pg_read_all_data`, so the statements run with read rights
  only. Statements run one at a time.
- The payload walks run one template at a time (`payload-by-template.sql`, once
  per template), and the pivot checks one pivot table at a time.
- The SQL is copied to the database host first and run there with
  `sudo -n -u postgres psql`, as the reference scripts do, so a stalled tunnel
  cannot leave the transaction waiting for input. The copy is removed
  afterwards.
- psql runs under `timeout` inside `sudo`, as postgres, so the stop reaches
  it (from outside `sudo` it would not). After 15 minutes
  (`DATARUN_INTEGRITY_TIMEOUT_S`, default 900) an interrupt cancels the running
  query and ends psql; a kill follows 30 seconds later if needed, and
  `client_connection_check_interval = '10s'` makes the server notice a killed
  client within seconds. Each session is named `datarun-integrity-check` in
  `pg_stat_activity`, so it can be found and cancelled by name. Tested on
  staging (2026-11-24) with `DATARUN_INTEGRITY_TIMEOUT_S=5`: psql sent its
  cancel and stopped, the runner exited 4 saying psql was stopped, and no
  psql process and no `datarun-integrity-check` session remained.

On staging (2026-11-24, about 66,000 submissions) a full run takes about a
minute, almost all of it in the payload walks. It wrote 132 temporary files,
about 3 GB in all; the most on disk at one time, sampled every 5 seconds, was
about 0.5 GB, under the 1 GB limit.

## Files

| File | Checks |
| --- | --- |
| `check.sh` | the runner |
| `sql/01-outbox.sql` | 1 |
| `sql/04-voided.sql` | 4 |
| `sql/05-skipped-submissions.sql` | 5c, 5d |
| `sql/08-pivots.sql` | 8 |
| `sql/09-version-labels.sql` | 9 |
| `sql/10-etl-lag.sql` | 10 |
| `sql/11-revisions.sql` | 11 |
| `sql/12-feed.sql` | 12 |
| `sql/payload-by-template.sql` | 2, 3, 5a–5b, 6, 7 |

Each SQL statement returns rows of `key|count|description`, optionally with a
fourth column saying where (a pivot table, or for check 12 a reason code or a
normalization flag); the runner adds the rows up per key and fails (exit 5) if
any expected key is missing.

## The checks

"Active" means a submission with `deleted = false`. "Should stay 0" means any
rise is a new problem.

### 1. The outbox against the stored submissions

Every change to a submission should leave an outbox event in the same
transaction. The ETL learns about changes only through these events.

- **1a** Submissions with no outbox event at all. They were stored before the
  outbox existed (April to September 2025) and were never backfilled, so tall
  and events do not have them either (see 5a).
- **1b** Deleted submissions that have events, whose latest event is not a
  DELETE (submissions with no event at all are in 1a). Today these are the 48
  deleted by hand-written SQL on 2025-11-27; their latest event is the later
  BACKFILL. Falls when events carry a type that matches the state (plan step
  C1).
- **1c** Active submissions whose latest event is a DELETE. Should stay 0.
- **1d** Submissions changed after their latest event: the stored payload
  differs from the latest event's payload, or the record moved on. Since step
  C an event names the version it announces (`outbox.submission_version`),
  and the record must still be at that version; the event's time now comes
  from the database and the record's from the application, so their times are
  not compared. Events without a version (written before step C, or by a
  rolled-back 6.5.5) are compared by time, as before: the upload saved the
  record before it stamped the event, so its event was never older; the admin
  delete stamped the DELETE event first, so a deleted record up to a minute
  newer than its DELETE event is not counted. On a database before step C
  every event is compared by time. Should stay 0; a rise means some write
  skipped the outbox (the generic PUT, or SQL).
- **1e, 1f** UPDATE events whose form data equals the previous event's, and
  their submissions. An UPDATE is also written when only the status, team,
  village or assignment changes (since `a7c7e008`), so this is not a fault count
  by itself. The ones from identical retries stopped on 2026-11-18.

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

A submission counts as projected once it has any tall row or any event. The
ETL writes events only for the keys of the tall rows it writes, so a
submission whose values all sit in repeat rows has no root event; a root event
alone is not the test.

- **5a** Active submissions holding at least one value with a field definition
  but no tall row and no event.
- **5b** The values with a field definition that those submissions hold: what a
  re-projection would add to tall.
- **5c** Active submissions whose form data is not a JSON object. There is
  nothing to project, so they are not a projection gap.
- **5d** Active submissions with no `template_uid`. The payload checks read one
  template at a time, so they cannot see them. Should stay 0.

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

Pivot tables (`pivot.fact_<template code>`) are snapshots, built only when
someone asks for them (one template or a list, each rebuilt whole). Each build
renames the previous table to `<name>_old`; those copies are not counted.
"Active events" are events with `deleted_at` null.

- **8a** Active events missing from their template's pivot table: events created
  after its last build. `--detail` shows which tables are old.
- **8b** Pivot rows that are not active events: voided rows a build kept, or
  events that are gone.
- **8c** Templates with active events and no pivot table under their current
  name. Pivots are built on request, so this is not a fault by itself.
- **8d** Pivot tables under a name no template has now: the template's code
  changed after the build.
- **8e** Manifest entries marked success whose table does not exist. A fault.
- **8f** Manifest entries whose name differs from the current naming. The
  manifest keeps the name of the first build.
- **8g** Active pivot rows of repeat rows removed from their submission: 2a
  carried into the pivots. Note that once D1 marks removed rows in events, these
  rows move to 8b until the pivots are rebuilt.
- **8h** Pivot tables under a current name that lack a column 8a, 8b or 8g
  read (`event_id`, `event_type`, `submission_uid`, `deleted_at`), so those
  checks are incomplete there. Today: three pivots built in December 2025 and
  January 2026 that have no `deleted_at`, so their own void flag cannot be read.

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

An outbox row is `pending` until the ETL first tries it and `success` once
projected. The ETL fix (branch `fix/etl-runtime`) adds `failed`, a row that
failed and will be tried again after a backoff, and `dead`, a row given up on.
Today's ETL writes neither, so 10c and 10d stay 0 until that fix ships; the
check reads only the `status` and `created_at` columns that exist today.

- **10a** Outbox rows not projected yet: pending, or failed and waiting to be
  tried again.
- **10b** The age of the oldest of them, in seconds (0 when none wait).
- **10c** Of 10a, the rows that failed and wait to be tried again.
- **10d** Dead rows: given up after failing, never projected. Each one is a
  change analytics will never see until someone acts.

### 11. The change record (journal.submission_revision)

Since step C every committed change of a submission is a revision in
`journal.submission_revision`, written in the same transaction as the change, and
every revision the application writes has one outbox row naming it by
`(submission_id, submission_version)`. Liquibase wrote one `BASELINE`
revision per submission when the journal started (`MIGRATION`), and writes
one at each start for every submission whose version rose without a revision
(`GAP_REPAIR`, after a rollback to a release without the journal). A
revision's state is `form_data` and the row's other columns as
`journal.submission_revision_metadata()` renders them. On a database without
the journal every line reads -1.

- **11a** Submissions with no revision. Should stay 0.
- **11b** Submissions whose head revision (highest version) is behind their
  `lock_version`: a write that left no revision. 6.5.5 running after a
  rollback does this until the next start of a release with the journal
  records a gap; anything else (SQL raising the version) is a fault, and the
  gap repair does not see it, because it leaves no cue (see
  `active-production-interface.md`). Should stay 0.
- **11c** Submissions whose head revision is ahead of their `lock_version`.
  Cannot happen through the application. Should stay 0.
- **11d** Submissions whose head revision, at their version, is not their
  stored state: a change made without raising the version (hand-written SQL).
  The gap repair cannot record these. Should stay 0.
- **11e** Revisions the application wrote (`CREATE`, `UPDATE`, `DELETE`)
  without exactly one outbox row naming them. Should stay 0 until outbox
  retention prunes delivered rows; then this check must change with it.
- **11f** Outbox rows whose event type does not follow their revision's
  state: `DELETE` whenever the submission is deleted after the change.
  Should stay 0.
- **11g** Reference: gap revisions. Rises only after a rollback window, by
  the number of submissions written meanwhile.
- **11h** Reference: revisions in all.

### 12. The submission feed (schema `feed`, contract version 1)

The feed publishes each submission's current record from its head (its newest
revision in the journal), with one record per row, positions from one
sequence under one lock (`docs/features/submission-feed/contract.md`). The
publisher keeps its pending changes (a feed part that failed, retried, and
dead past the attempt cap, with a reason code), its republish marks and the
normalizer's flag counts in `feed_ops`. The publication lines (12a to 12h)
must read 0 once the feed is on and caught up (contract 8, item 4); with the
feed off, 12a counts every head. On a database without the feed's tables
every line reads -1.

- **12a** Heads not published at their version, other than those waiting in
  the backlog (an outbox row not processed yet) or as a pending change.
- **12b** Submission records without their submission.
- **12c** Row records without their submission's record.
- **12d** Live row records of a submission whose record is deleted.
- **12e** Positions used twice, across `submission` and `submission_row`.
- **12f** Records at their head's version whose live row records differ in
  number from the head's rows. The check finds a head's rows the way the
  submission normalizer does, apart from its code: walking the document, an
  object is walked when no form version defines its path or when its path
  resolves to a plain section, and a non-empty array of objects is rows when
  its path resolves to a repeating section; a path resolves, for the value's
  shape, to the first definition that can hold it, the pinned version's first,
  then the newest version's, a field before a section (the file states the
  rule in full).
- **12g** Pending changes older than the freshness target (two minutes).
- **12h** Dead changes: pending changes past the attempt cap, by reason code,
  and dead outbox rows (`OUTBOX_DEAD`), as `freshness.dead` counts them.
  `--detail` splits them.
- **12i** Reference: the backlog, outbox rows not processed yet.
- **12j** Reference: the normalizer's flags in published records, by flag
  (`PATH_NOT_IN_FORM` and the others). `--detail` splits them.
- **12k** Reference: republish marks waiting.

## How the payload checks read a submission

The ETL that runs is `TransformServiceV2` (`EventProcessorServiceImpl.java:35`,
`:99`). It takes its tall rows from `TransformServiceRobust.transform` over
every field definition of the template (`TransformServiceV2.java:34`,
`EventProcessorServiceImpl.java:85`) and writes one event per projection key of
those rows (`TransformServiceV2.java:51`, `:84`). `payload-by-template.sql`
mirrors that path resolution, so its expected projection can be compared with
tall row by row:

- a path is section and field names joined by dots; repeat rows add no segment;
- an array is a repeat when one of its first 10 items is an object, and each
  item is then a row (`TransformServiceRobust.java:171-180`); any other array (a
  multi-select, or an empty array) is one node;
- JSON null and `""` are skipped (`:163`, `:166`, `:102`); any other node at a
  field definition's path is projected, even an empty array or a plain section
  object;
- the projection key is the nearest repeat row's `_id` (`:47`), or the
  submission uid outside rows or when the nearest row has no `_id`
  (`InstanceKeyUtil.java:27`);
- when several nodes share a key and a field, the last one is kept, because
  tall is upserted on that key and field (`TallCanonicalJdbcRepository.java:39`).

On staging this mirror agrees with tall: every tall row of an active
submission is either an expected row or a counted leftover (2b, 3a), no
expected row of a projected submission is missing (7a = 0), and none of the
1,585,896 values differs (7d = 0).

A **value** is a non-empty string, a number, a boolean or a non-empty
multi-select. Keys starting with `_` (`_id`, `_index`, `_parentId`,
`_submissionUid`, `_status`) are system keys, not values.

An active submission whose form data is not a JSON object (5c) yields no
values, so any tall rows and events it still has count as left behind: in 2a,
2b and 3a, and its pivot repeat rows in 8g. Today the two such submissions have
none.

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
| 1e, 1f | 1,573, 588 | 1,573, 588 | equal form data; identical retries and metadata-only changes |
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
| 5c, 5d | 2, 0 | | not measured before |
| 6a | 18,485 | 18,485 | |
| 6b | 699 | 699 | |
| 6c | 1 | | not measured before: a line-items submission holding another form's data since a July 2026 re-send |
| 7a | 0 | 3,682 | the investigation's 3,682 are the values counted in 7b, not values lost to a definition change |
| 7b, 7c | 3,682, 71 | | |
| 7d | 0 | 0 | |
| 7e | 1,585,896 | 1,585,896 | |
| 8a | 6,646 | | per table as the investigation: 3,312, 412, 64 and 2,239 in the four old pivots; the pivots rebuilt on 2026-11-19 miss only later events |
| 8b | 70 | 70 | voided rows in `fact_lsm` |
| 8c | 3 | | not measured before; two templates never pivoted, and the one whose table is missing (8e) |
| 8d, 8e, 8f | 3, 2, 3 | 3, 2, 3 | |
| 8g | 409 | 409 | |
| 8h | 3 | | not measured before: three older pivots without `deleted_at` |
| 9a, 9b | 402, 13,814 | 402, 13,814 | |
| 9c, 9d | 0, 0 | | |
| 10a–10d | 0, 0, 0, 0 | 0 pending or failed | |

## Not included

- How stale the materialized views `events_enriched` and `event_ancestors` are:
  restoring a dump recomputes them, so staging cannot show it.
- The Airbyte copy (`datarun_meta`) and the analytics schemas downstream of it.
- Option values with no reference uid (measured in the investigations): the
  stored code matches no option, which is a question for server-side
  validation, not for the chain.
