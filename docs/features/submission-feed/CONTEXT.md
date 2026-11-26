# Submission feed

The words of the submission feed: what DataRun publishes for downstream analytics, how a consumer reads it, and how
the publisher keeps it complete. Status: LIVING. Contract version 1 is proposed and reviewed (2026-11-25), and the
feed is built to it and switched off (2026-11-26); its first publication waits for phase B and for a contract revision.

- These are engineering and downstream words; operations see none of them.
- Words shared with [submission review](../submission-review/CONTEXT.md) mean the same in both: form, activity,
  team, org unit, submission, row, entry (a list entry only), version, deleted, created, entered, placement,
  split-off, position, field id, data path, unmapped value, head, outbox, ETL.
- Airbyte's own words name what Airbyte does: stream, record, cursor field, sync mode, initial load, refresh.

## Language

### The feed and how it is read

**Feed**:
The change feed of submissions' current state, in schema `feed`. A consumer reads what changed since its last read
and keeps the newest record of each key.
_Avoid_: change log, history (the journal's), export

**Stream**:
One table of the feed, as Airbyte reads it: `submission`, `submission_row`, `form_field`, `form_section` and
`freshness`.

**Record**:
One row of a stream, in Airbyte's sense. Not a submission; in the lists feature a record is a source line.
_Avoid_: row (a submission's row), entry, event

**Position**:
The feed's total order: one number per record, from one sequence for both submission streams, assigned by the
publisher in commit order. It is Airbyte's cursor field.
_Avoid_: offset, seq, sequence number; position for a row's place among its siblings (its row index)

**Current record**:
The record with the highest position for its key: the only one a deduplicating consumer keeps.
_Avoid_: state (Airbyte's saved cursor), snapshot (the journal's revisions hold snapshots)

**Deleted record**:
A record with `deleted = true`: a deleted submission, or a row no longer in its submission's current state. It keeps
its last values. _Tombstone_ in prose only.
_Avoid_: tombstone as a name, or in Kafka's sense (a record with a null value); void; removed record

**Reader role**:
Downstream's database role, which Airbyte reads schema `feed` as, named by the setting `datarun.feed.reader-role`. At
every start the grants on `feed` converge to it: USAGE on the schema, SELECT on its tables and on tables added later,
and nothing for any other role. With no role named, no grant is made or revoked.
_Avoid_: Airbyte role (the role is downstream's to name), reading role, feed user

### What a record holds

**Submission**:
One filled form sent from a phone, as in submission review: one record of `submission`, live or deleted.

**Row**:
One repetition of a repeating section in a submission, as in submission review: one record of `submission_row`,
keyed by its submission and its row id.
_Avoid_: repeat row, event, child record

**Row id origin**:
Whether a row id is the row's own stored id (`STORED`) or derived from its place (`DERIVED`), because none is stored,
or an empty one, or one an earlier row of the submission already has. A derived id changes when an earlier sibling is
removed.

**Row index**:
A row's 1-based place among its siblings.

**Section**:
A group of fields in a form version; a repeating section holds rows. Known by a derived section id, since form
definitions carry none that is usable.

**Version**:
The submission's revision number that a record shows, as in submission review. A row's **changed in version** is
the submission's version when the row's record last changed.
_Avoid_: `submission_version` (a dead column's name), version alone for a form version

**Placement**:
The assignment, team, org unit and activity a submission belongs to.

**Created**:
When DataRun stored the submission (`data_submission.created_date`): an upload's receipt, a go-live release's time,
or a split-off's split time.
_Avoid_: received (it is not the first receipt for releases and split-offs)

**Entered**:
The phone's start and end times for filling the form.

**Changed at**:
When the change a record shows was recorded. Never a cursor: it does not follow commit order.

**Field id**:
The id of the data element a field carries, as in submission review: the same in every version of the form, through
renames, type changes and moves.
_Avoid_: element, canonical element (analytics' old key), path

**Field values**:
One container's values keyed by field id: a submission's top-level values, or one row's.

**Data path**:
Where a value sits in a stored document: the stored keys from the root, joined by dots. A section path is a data
path.

**Unmapped values**:
Stored values no field keys, kept whole at their data path and never dropped: mostly values at paths no version of
the form defines, also a second value for a field that already has one.

### The dictionary and freshness

**Dictionary**:
The streams `form_field` and `form_section`: every form version's fields and sections, with names, labels, value
types and option sets. It is submission review's form dictionary, as streams.
_Avoid_: metadata, element

**Freshness**:
The one-record stream `freshness`: how far the feed has got, and what waits.
_Avoid_: status

**Backlog**:
Recorded changes, still being processed, whose version the feed does not hold yet. It counts the feed's own state only,
whatever DataRun's other outputs did with a change.

**Pending**:
A change whose feed part failed and waits for a retry.

**Dead**:
A change of the feed's given up on: a pending change past the attempt cap, kept with its reason code until a fix lets
reconcile publish its submission.

**Heartbeat**:
The signal sent after each successful poll of the runtime that applies the outbox's changes, only while the feed is
within its freshness target and nothing is dead; its absence raises an alert. Until tall and events retire it is that
runtime's heartbeat, which the feed's freshness also gates; once they retire, it is the feed's.

### Keeping it complete

**Publisher**:
The feed's single writer, inside DataRun. Internal.

**Submission normalizer**:
The module that turns a stored submission into values keyed by row id and field id, typed by the form version, for
the publisher and for submission review (`SubmissionNormalizer.normalize`). It never throws on data: it flags what it
meets instead, and counts each normalization flag per submission (such as `PATH_NOT_IN_FORM` or `DUPLICATE_ID`).
_Avoid_: translation (in DataRun, label translations), translator

**Head**:
A submission's newest revision in the journal.

**First publication**:
The server's write of every submission's head when the feed starts. An admin starts it (`/management/feedpublication`);
the publisher writes the heads in id order, in batches behind the live backlog, and keeps its mark, the last submission
it has passed, so a restart goes on from there.
_Avoid_: initial load (Airbyte's first read), backfill, snapshot

**Republish**:
Writing a scope's current records again: the same versions, new positions. An admin marks the scope (every form, one
form, or listed submissions; `/management/feedpublication`), and the publisher writes the marks behind the live
backlog, skipping a record whose held version is newer than its head.

**Reconcile**:
Finding and publishing every submission whose published version differs from its head: at start, at switch-on and
nightly. Not an activity's reconciliation form.

**Refresh**:
Downstream's re-read of a stream in Airbyte ("Refresh and Retain Records").

**Crosswalk**:
The published map from analytics' `canonical_element_id` to `field_id`, for re-pointing existing models
(`scripts/feed/crosswalk.sql`).

**Agreement harness**:
The check, counts only, that the submission normalizer agrees with the feed spike's oracle on every head of the
journal: mapped values by field id, unmapped values by data path, rows by id, and the form and form version. It runs
read-only on a restored clone the operator names, on a lab host from its bundle (`scripts/feed/agreement.sh`),
before the feed is switched on.
_Avoid_: reconcile (the publisher's), integrity check (`scripts/integrity`), verification

## Words to avoid

| Avoid | Why | Say instead |
| --- | --- | --- |
| state | Airbyte's saved cursor | current record |
| status (bare) | Several meanings | freshness; deleted |
| snapshot | The journal's revisions hold snapshots | current record; first publication |
| element, offset, seq | Vague, or another system's word | field id; position |
| entry, for anything but a list entry | It means a list entry, as in submission review | record; pending change |
| received | Not the first receipt for releases and split-offs | created |
| `submission_version` | A dead column's name | version; changed in version |
| void | The stored state is deleted | deleted |
| translation (for reading stored submissions) | Means label translations in DataRun | submission normalizer; normalize, normalized |
| command, actor | The journal's columns; the feed publishes neither | — |
