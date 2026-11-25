# Submission feed: contract, version 1

Status: PROPOSED AND REVIEWED (2026-11-25), NOT BUILT. The words are those of [CONTEXT.md](CONTEXT.md).

The feed publishes the current state of every submission. Downstream wants the current state, and nobody reads who
made a change (owner, 2026-11-25), so the feed publishes no history, no actor and no command; history stays in
DataRun's revision journal. DataRun owns its model, and downstream follows the published feed (owner, 2026-11-25).
Decisions are engineering (2026-11-25) unless marked. Counts come from a throwaway spike on a production clone
(November 2026).

## 1. Streams

| Stream | One record per | Primary key | Cursor field | Airbyte sync mode |
| --- | --- | --- | --- | --- |
| `feed.submission` | submission, live or deleted | `id` | `position` | Incremental, Append + Deduped |
| `feed.submission_row` | row ever published in a submission's current record | `submission_id`, `id` | `position` | Incremental, Append + Deduped |
| `feed.form_field` | field placement per form version | unique (`form_version_id`, `section_id`, `field_id`), for reference | — | Full Refresh, Overwrite |
| `feed.form_section` | section per form version | (`form_version_id`, `section_id`) | — | Full Refresh, Overwrite |
| `feed.freshness` | feed (one record: `submissions`) | `feed` | — | Full Refresh, Overwrite |

## 2. `feed.submission`

| Column | Type (Airbyte) | Null | Meaning |
| --- | --- | --- | --- |
| `position` | bigint (number) | no | The feed's order; the cursor field |
| `id` | text (string) | no | The submission's id, `public.data_submission.id` |
| `version` | bigint (number) | no | The submission's version (revision number) this record shows |
| `deleted` | boolean | no | True when the submission is deleted; the record keeps its last values |
| `form_id` | text | no | `public.data_template.id` |
| `form_version_id` | text | yes | The pinned form version, `public.data_template_version.id`; never null today |
| `assignment_id`, `team_id`, `org_unit_id`, `activity_id` | text | no | The placement: the ids of `public.assignment`, `team`, `org_unit`, `activity` |
| `created_at` | timestamptz (string, ISO 8601 with zone) | no | When DataRun stored the submission (`created_date`, UTC): an upload's receipt, a go-live release's time, a split-off's time. For the last two it is not the first receipt |
| `entered_start_at`, `entered_end_at` | timestamptz | yes | The phone's start and end of filling the form |
| `changed_at` | timestamptz | no | When the change this record shows was recorded. Never a cursor: it follows the writer's start, not commit order, and a cursor on it missed 54 of 521 changed submissions in the spike |
| `published_at` | timestamptz | no | When this record was written |
| `field_values` | jsonb (string) | no | Top-level values: `{"<field id>": value}` |
| `unmapped_values` | jsonb (string) | yes | Top-level values no field keys: `{"<data path>": value}` |

## 3. `feed.submission_row`

| Column | Type (Airbyte) | Null | Meaning |
| --- | --- | --- | --- |
| `position` | bigint | no | The feed's order, from the same sequence as `submission` |
| `submission_id` | text | no | The submission |
| `id` | text | no | The row id: its stored id as text, or a derived id (§5). Unique within its submission only: a split moves rows to a new submission with their ids, and a phone can copy rows |
| `id_origin` | text | no | `STORED` or `DERIVED` (an open list). Derived when the row has no stored id or an empty one, or when its id, stored or derived, is already taken by a row earlier in walk order (`DUPLICATE_ID`, §5). A derived id follows the row's place, so it changes when an earlier sibling is removed, until a change of submission review stores it (§5) |
| `parent_row_id` | text | yes | The parent row, for a nested row; null at the first level |
| `section_id` | text | no | The repeating section (`form_section.section_id`) |
| `row_index` | integer | no | The row's 1-based place among its siblings |
| `deleted` | boolean | no | True when the row is no longer in its submission's current state (removed by an edit, split into another submission, or replaced by a re-send that changed row ids), or when its submission is deleted |
| `changed_in_version` | bigint | no | The submission's version when this record last changed |
| `published_at` | timestamptz | no | When this record was written |
| `field_values` | jsonb (string) | no | `{"<field id>": value}` |
| `unmapped_values` | jsonb (string) | yes | `{"<path inside the row>": value}` |

## 4. Values

- **As stored, whole, never cut.** Numbers stay numbers, exact; multi-selects stay arrays of option codes; `""` and
  `[]` stay as sent. An absent key means no value: nulls are not published.
- **Keyed by field id** inside each container (the top level, or one row), at most once per container. The server
  should refuse a form version that places one data element twice in one container.
- **Normalized** by the submission normalizer: resolved against the submission's pinned form version, then against the
  form's other versions by the shape of the stored value. A value no field keys goes to `unmapped_values`, whole, at
  its path, and is never dropped (engineering default, open to the owner): mostly values at paths no version defines
  (`PATH_NOT_IN_FORM`; about 5,900 values in two forms today), also a second value for a field that already has one
  (`FIELD_ALREADY_HAS_VALUE`). A stored document that is not a JSON object is kept whole under the empty path `""`.
- **Never a failure on data.** The normalizer flags what it meets and counts the flags per submission; the publisher
  never fails a change because of the data. The flag counts are server monitoring, reported for reference by
  integrity check 12 (§8) and as a metric, not part of the feed: downstream already sees `unmapped_values`. A count
  column would be an additive change if downstream ever needs one (engineering, 2026-11-25).
- **Types are the phone's:** the server does not check uploaded values (submission review checks its own edits).
  Cast by the dictionary's `value_type`, guarding against a stray type (none today).
- **Labels are not in the feed.** Option labels, names and translations come through their own tables (owner,
  2026-11-25).

## 5. Ids

- **Opaque text: never parse them.** Formats vary: ULIDs, 24-character hexadecimal legacy ids on some forms, UUIDv7
  text later. Every id equals the `id` column of the public table it names, so downstream joins its existing copies
  by text.
- **No uid.** A model that still needs a submission's uid during the transition joins `public.data_submission (id,
  uid)`, which Airbyte already copies.
- **Derived ids** are the lower-case text of a UUID version 5 in the namespace `4b1aa021-0abd-532b-88b8-f71a9078093d`
  (the version 5 UUID of the URL `https://datarun/feed/v1`), over a name that joins stored id texts and paths with
  `|`:
  - a section: `<form id>|section|<section path>`. Form definitions carry no usable section id (145 of 717 section
    objects have none, and 21 of 62 repeating-section paths changed id across versions), so a renamed section gets a
    new id;
  - a row with no stored id, or an empty one: `<submission id>|<section path>|<parent row id, or nothing>|<row index>`;
  - a row whose id, stored or derived, is already taken by an earlier row of the submission (`DUPLICATE_ID`): first
    the name above, then the same name followed by `|<attempt>`, from 2 up, until the id is free.

  A section path is the stored keys from the root to the section, joined by dots. The parent row id is that row's own
  id, stored or derived. The row index is the row's 1-based place in its array.

  **Row ids are stored by submission review's changes that write the document** (engineering, 2026-11-25). An edit,
  a split or an accept stores, in the same revision, the id the feed already shows in each row lacking a unique
  stored id, the `|<attempt>` ids included, and `id_origin` turns `STORED` with the same value. A server-side change
  never changes a row id. Move, delete and restore write no document and store nothing; derived ids depend on
  neither placement nor deletion. A phone upload that applies still replaces the document, as today: apps from 6.0
  send their rows' ids, and the legacy route mints ids for rows sent without them (§9).

  **Earlier** means walk order: depth first from the document's root; each object's keys in `String.compareTo`
  order (by UTF-16 code unit); each array in stored order; a row before its nested rows. The walk reads the stored
  document as PostgreSQL prints it: arrays keep their stored order, and object keys, which come in jsonb's own
  order, are sorted again by the walk, so jsonb's key order never changes an id.

## 6. The dictionary

### `feed.form_field`

One record per field placement per form version. Unique on (`form_version_id`, `section_id`, `field_id`), nulls not
distinct, which matters only for `section_id`.

| Column | Type (Airbyte) | Null | Meaning |
| --- | --- | --- | --- |
| `form_id` | text (string) | no | `public.data_template.id` |
| `form_version_id` | text | no | `public.data_template_version.id` |
| `form_version_number` | integer (number) | no | The version's number |
| `field_id` | text | no | `public.data_element.id` |
| `section_id` | text | yes | The section that holds the field (`form_section.section_id`); null for a field outside every section |
| `repeat_section_id` | text | yes | The repeating section whose rows hold the field's values; null when they are in `submission.field_values` |
| `name` | text | no | The field's name in this version |
| `label` | jsonb (string) | yes | Labels by locale |
| `value_type` | text | yes | The value type as defined; null when the definition gives none |
| `option_set_id` | text | yes | `public.option_set.id` |
| `mandatory` | boolean | yes | As defined; null when the definition does not say |
| `sort_order` | integer | yes | Its place among its siblings, as defined |
| `published_at` | timestamptz (string, ISO 8601 with zone) | no | When this record was written |

Only fields the submission normalizer resolves are published (engineering, 2026-11-25). A field whose data element
no longer exists, or a definition without a data element or a name, has no record; its values are already published
as unmapped values under their data path.

### `feed.form_section`

One record per section per form version; key (`form_version_id`, `section_id`).

| Column | Type (Airbyte) | Null | Meaning |
| --- | --- | --- | --- |
| `form_id` | text (string) | no | `public.data_template.id` |
| `form_version_id` | text | no | `public.data_template_version.id` |
| `form_version_number` | integer (number) | no | The version's number |
| `section_id` | text | no | The section's derived id (§5) |
| `parent_section_id` | text | yes | The section that holds it; null for a section outside every other |
| `name` | text | yes | The section's name in this version |
| `label` | jsonb (string) | yes | Labels by locale |
| `repeatable` | boolean | no | Whether its data are rows; false when the definition does not say |
| `sort_order` | integer | yes | Its place among its siblings, as defined |
| `published_at` | timestamptz | no | When this record was written |

- **Small** (about 5,000 placements and sections), so read with a full refresh: a rewritten or deleted form version
  needs no deleted records. The server rewrites the dictionary in place, in one transaction, at start and whenever a
  version is published or changed. A full-refresh read that overlaps a rewrite can miss records until the next sync.
- **What it answers:** a field's name, type and option set in every version, and where its values sit. A field keeps
  its id through renames, type changes and moves (no field split by id in the spike, where tall splits 83 of 1,138).
  A field moved between the top level and a repeating section, or sitting in a renamed repeating section, changes
  wide table; the dictionary shows each case.

## 7. `feed.freshness`

One record, key `feed` = `submissions`, read with a full refresh at every sync.

| Column | Type (Airbyte) | Null | Meaning |
| --- | --- | --- | --- |
| `feed` | text (string) | no | The feed's name and key: `submissions` (room for other feeds in the schema) |
| `last_position` | bigint (number) | yes | The highest position published; null until the first record |
| `last_published_at` | timestamptz (string) | yes | When the publisher last wrote a record; null until then |
| `last_changed_at` | timestamptz | yes | The newest `changed_at` published ("data as of", for dashboards); null until the first record |
| `backlog` | bigint | no | Outbox changes not yet processed; 0 when none |
| `oldest_unpublished_at` | timestamptz | yes | When the oldest of them was recorded; null when none |
| `pending` | bigint | no | Changes whose feed part failed and wait for a retry; 0 when none |
| `oldest_pending_at` | timestamptz | yes | When the oldest pending change first failed; null when none |
| `dead` | bigint | no | Changes given up on: outbox rows marked dead, and pending changes past the attempt cap, each kept with its reason code; 0 when none. Reconcile still publishes their submissions once the cause is fixed |
| `unpublished` | bigint | yes | Submissions whose published version differed from the journal's head at the last reconcile; null until the first reconcile |
| `last_reconciled_at` | timestamptz | yes | When reconcile last ran; null until then |
| `first_publication_completed_at` | timestamptz | yes | Null until the first publication has written every submission; models built earlier are partial |
| `checked_at` | timestamptz | no | When the publisher last wrote this record, also when idle |

Normalization flag counts are not in `freshness`; they are server monitoring (§4).

## 8. Order and delivery

1. **Ordering is enforced inside every feed-writing transaction.** Each takes `pg_advisory_xact_lock(<feed key>)`
   first, then reads the submission's head in the journal and the version the feed holds, then takes the next value
   of `feed.position_seq` (`CACHE 1`, so that two sessions cannot interleave cached values). Commits therefore happen
   in position order, even if two publishers ever run: the second waits for the first's commit and gets higher
   positions. A reader that has read up to position _p_ and next reads `position >= p` misses nothing (the spike: 0
   missed of 15,317 changed keys under concurrent late commits). Live changes, pending retries, reconcile batches and
   republication all take the lock. The ETL's run lock is not relied on: it is held on a separate connection, which
   can be lost while the run continues.
2. **At least once.** Airbyte's cursor is inclusive (`>=`), so the record at the last position is read again, and a
   record changed again comes back with a new position. Consumers deduplicate by primary key, keeping the highest
   position, which is what Append + Deduped does.
3. **The newest record is current.** A key appears once in the server's tables, rewritten with a new position at each
   change. Versions never go backwards: a revision at or below the version the feed holds is skipped. Republication
   writes the same version again, and skips a submission whose held version is newer than the head it read.
4. **Nothing stays unpublished.** The feed's writes share the ETL's per-row transaction, inside a savepoint: a feed
   failure rolls back only the feed part, tall and events still commit, and the submission waits in a retry list; a
   tall or events failure never holds back the feed, which commits whatever they do. Retries are capped; past the cap the change is marked dead with its reason code, counted in `freshness` and by
   integrity check 12, and the heartbeat stops (§12). Reconcile publishes every head the feed lacks or holds at another
   version: at start, at switch-on and nightly. That covers periods with the feed off, a rollback to a release without
   it, dead outbox rows and anything else. **Integrity check 12** holds the feed to this. Its publication lines must
   read 0: a journal head not published at its version once the backlog is 0; a feed record without its submission;
   a live row of a deleted submission; a position used twice; a submission whose row records differ from its head;
   a pending change older than the freshness target; a dead change. The backlog and the normalization flag counts
   are reported for reference only.
5. **A split lands at once.** Submission review writes a split as one transaction with two revisions that name each
   other. When the publisher reaches either revision, it writes both submissions' heads in one feed transaction; the
   partner's own outbox row then finds its version held and is skipped. Because of the feed, no consumer sees the moved
   rows under both submissions, or under neither.
6. **Not guaranteed across the two streams within one sync.** Airbyte reads `submission` and `submission_row` at
   different moments, so for one sync interval a submission's record and its row records may differ. Consumers
   join rows to live submissions (`NOT r.deleted AND NOT s.deleted`). After a split, a read caught between the two
   streams then shows the moved rows for one interval either hidden or still under the source submission; never
   under both, and the next sync converges.
7. **Not guaranteed:** every intermediate version (a lagging publisher coalesces changes); a record for a row added and
   removed between two publications.

## 9. Deletes

- **A deleted submission:** its record and every row record are rewritten with `deleted = true`, keeping the last
  values. A restore rewrites them with `deleted = false`.
- **A removed row,** or a row split into another submission: its record is rewritten with `deleted = true`. A re-send
  that replaces row ids (apps 5.2 and 5.3 send rows without ids, and the legacy route mints new ones) shows as old
  rows deleted and new rows added.
- **Nothing is physically deleted in version 1.** A later version may compact deleted records older than a stated
  window, announced first; never the newest record of a live key.
- **Airbyte's cursor-based syncs carry no delete events:** `deleted` is an ordinary column, which the consumer filters.

## 10. Evolution

- **Additive within version 1:** new nullable columns, new streams, new values in open lists (`id_origin`), new JSON
  keys (a new field simply appears). Consumers ignore what they do not know.
- **A new column reaches existing records only when they are rewritten.** If it must be filled for every record, the
  release republishes everything at a quiet hour (positions move, so incremental syncs carry the column; about 3 GB of
  WAL per million submissions); otherwise old records hold null until they next change. The release notes say which.
  Either way downstream refreshes its schema in Airbyte to see the column.
- **New streams** are readable at once: at every start the server grants the configured reader role
  (`datarun.feed.reader-role`) `SELECT` on the tables of schema `feed`, and on future ones (`ALTER DEFAULT
  PRIVILEGES`), and revokes every other non-owner grantee. When the setting is empty or names no existing role, it
  changes nothing and logs a warning.
- **Widening a type** is allowed only when Airbyte's type stays the same: integer to bigint (number), bounded text to
  unbounded (string). Any change of Airbyte type is breaking.
- **Deprecating a column:** announced with a date and kept populated until a `feed_v2` replaces it; a name is never
  reused for another meaning.
- **Breaking:** renaming, removing or retyping a column; changing a key, the cursor's meaning or an id recipe. A
  breaking change publishes a schema `feed_v2` beside `feed` for one campaign or 90 days, whichever is longer, with a
  dated notice; adding it to Airbyte is a deliberate step.
- **Form changes are not contract changes.** A column for the true first receipt can be added once every path stores
  it.

## 11. Replay and republication

- **Downstream:** "Refresh and Retain Records" on a stream re-reads every current record and keeps what the destination
  already holds; Airbyte calls it the best way to recover with a deduplicated sync mode.
- **Server:** an admin-only action republishes a scope (all, one form, listed submissions) after a fix: the same
  versions, new positions; consumers do nothing.

## 12. Freshness

- **Target:** a committed change published within 2 minutes at p95 during a campaign peak. Basis: the ETL's lag is
  28 s at p95, and the publisher adds 1.4–5.7 ms of CPU per change.
- **Alerting:** the heartbeat is sent only while the oldest unpublished outbox change and the oldest pending change are
  both younger than the target, and nothing is dead. A stall, a failure that recurs on every retry, or a change past
  the attempt cap stops it, and GlitchTip alerts.
- **End to end** adds Airbyte's cadence and downstream's model runs, which downstream sets and shows. Only a need under
  about 5 minutes would change the design (engineering default, open to the owner).

## 13. Airbyte settings (downstream's connection)

| Setting | Value |
| --- | --- |
| Source | The existing Postgres source on production |
| Schemas | Add `feed` to `analytics`, `pivot` and `public`, which it reads today (owner, 2026-11-25). Never `journal`, `api`, `feed_ops` or `submission_review` |
| Replication method | Standard (user-defined cursor), as today; not xmin (a whole-table read at each sync), and not CDC (`wal_level` is `replica`) |
| `feed.submission` | Incremental, Append + Deduped; cursor field `position`; primary key `id` |
| `feed.submission_row` | Incremental, Append + Deduped; cursor field `position`; primary key (`submission_id`, `id`) |
| `feed.form_field`, `feed.form_section`, `feed.freshness` | Full Refresh, Overwrite |
| First sync | Covered by the cursor watermark Airbyte takes at the start of its scan. As cheap insurance, run it with the feed paused at a quiet hour (switch-on reconciles), then check counts against `freshness` and the server's counts |
| Cadence | Downstream's choice; schedules are per connection |
| Permissions | `USAGE` on schema `feed` and `SELECT` on its tables, plus default privileges for later streams |

**JSON arrives as text** (Airbyte's Postgres source types `jsonb` as a string). Downstream parses it, so there is no
tall view (owner, 2026-11-25). Parse once per record in a staging model:

```sql
-- downstream, PostgreSQL destination: parse once, then read by field id and cast by the dictionary's value_type
SELECT s.id, s.version, s.team_id, s.org_unit_id, s.entered_end_at,
       (v.fv->>'<field id>')::numeric AS <field name>,    -- one line per field
       v.fv->'<multi-select field id>'  AS <field name>     -- arrays stay jsonb
FROM feed_submission s
CROSS JOIN LATERAL (SELECT s.field_values::jsonb AS fv) v
WHERE s.form_id = '<form id>' AND NOT s.deleted;
```

A repeating section's wide table reads `submission_row` filtered by `section_id`, joined to live submissions.

## 14. What downstream can drop, and the handoff

- **After its switch, downstream can drop** its streams of `analytics.tall_canonical`, `analytics.events` and
  `pivot.*`; field resolution through `canonical_element` (the dictionary replaces it); its removed-row filters and
  delete joins; its copy of `data_submission` for values and placement (keeping `(id, uid)` while a model still needs
  uids).
- **Re-pointing keys:** the server publishes a crosswalk from `canonical_element_id` to `field_id`, with the form and
  the versions where it applies. Several canonical ids can map to one field (renames split today's keys); none maps
  to two.
- **`value_ref_uid` has no feed equivalent.** The feed publishes the raw value, and downstream joins its copies with
  the rules tall uses today: options within the field's option set by code, then name, then English label, then
  Arabic label; org units by uid, then code, then id; teams by uid or id. Option values that match no option stay as
  published.
- **Values downstream sees for the first time:** unmapped values, submissions tall never projected, and values longer
  than 10,000 characters that tall cut; also removed rows gone, deletes marked, and fields no longer split by renames.
  The server provides a counts-only list before the switch.
- **The handoff** is a document in this repository: the streams, the settings, the staging recipe, the crosswalk, the
  reference rules, the switch steps and the first sync (owner, 2026-11-25).

## Rollout

### Before phase B: measure today's gap

- **Why:** `public.data_submission` is copied with an `updated_at` cursor. A change that commits late can fall behind
  the cursor and stay missing until its submission changes again. Uploads are short, and production's real gap is
  not measured.
- **Measure** (downstream, counts only): compare downstream's copy with the source, for changes stored before the
  copy's last completed sync started.

  ```sql
  -- s: the source's (id, lock_version, last_modified_date), read-only; c: downstream's copy of data_submission
  SELECT count(*) FILTER (WHERE c.id IS NULL)              AS missing,
         count(*) FILTER (WHERE c.lock_version < s.lock_version) AS behind
  FROM s LEFT JOIN c USING (id)
  WHERE s.last_modified_date < :last_sync_started_at;
  ```

- **If `missing` or `behind` is above 0:** in the Airbyte UI, run "Refresh and Retain Records" on the
  `data_submission` stream at a quiet hour (it pauses scheduled syncs until it finishes), then measure again. Never a
  second connection, and never Overwrite: full-refresh scans have been reported to drop table rows updated during the
  read. A refresh scans too, so a record updated during it can stay stale until its next change; measure again
  afterwards, and weekly until phase C.
- tall and events are written by one sequential writer, so their cursors have no such gap.

### Phase B: publish the feed beside tall and events

**When:** after the revision journal (6.5.6) is in production and the submission normalizer is merged, with the
feed's release rehearsed. Timing follows engineering readiness, not a campaign date (owner, 2026-11-25).

- **Server:** deploy with the feed switched off. At a quiet hour, switch it on: reconcile runs, then the first
  publication writes every submission (about half an hour of CPU and 3 GB of WAL per million submissions); the
  publication lines of integrity check 12 read 0; `first_publication_completed_at` is set. From then on the ETL
  writes tall, events and the feed for every change. The feed costs about 4.4 KB of WAL per change.
- **Downstream:** add `feed` to the source's schemas, refresh the schema, and enable the five streams (§13); run the
  first sync with the feed paused, then check counts. New models are built on the feed. Feed versions of existing
  models are built beside the old ones and compared through the server's comparison queries.
- **Keeps running:** tall, events, pivots and every existing stream.
- **Rollback:** before any model reads the feed, switch it off, and drop schema `feed` only after downstream has
  deselected its streams. Once models read it, switching it off stops their dashboards from updating and dropping it
  removes their source, so the rollback is a pause; reconcile at switch-on catches up.

**Comparison queries** (server-provided, dated, counts only): per form and day, live submissions in the feed against
tall and against `data_submission`; live rows per repeating section against events; values per field through the
crosswalk; the first-time values of §14, counted one by one.

### Phase C: downstream switches

- New models build on the feed from the start: nothing to re-point.
- Existing models are re-pointed one form at a time, with the crosswalk and the rules of §14, each once its counts
  agree; dashboards follow. Not during a campaign peak.
- Dimension tables stay as Airbyte copies them (daily) and are joined by text id. During a campaign, a new team or
  assignment joins nothing until the next copy; downstream may copy those two more often.
- Submission review's gate B (clears and row removals) is switched on server-wide, once downstream confirms in
  writing that no model reads tall and events, for any activity (engineering, 2026-11-25): downstream re-points all
  its models, and tall and events then retire. No removal fix for tall is built; the split re-attribution stays
  (owner, 2026-11-25).
- The server marks tall, events and pivots deprecated in its docs, and watches their scan counters until reads stop.
- **Rollback:** downstream points back at tall and events, which are still maintained. Before `clearValue` and
  `removeRow` are switched on, that loses nothing. After that, tall and events keep the removed rows and cleared
  values of the submissions those operations touched, which the journal lists; a rollback then accepts those
  differences (engineering, 2026-11-25); tall and events get no removal fix (owner, 2026-11-25).

### Phase D: retire tall, events and pivots

**When:** once downstream confirms its switch in writing (owner, 2026-11-25). The rollback margin is one campaign or
90 days (engineering default; open to the owner). Each step is its own release:

1. pivots: the feature, its manifest and its views;
2. the ETL stops writing tall and events, and the feed is its only output;
3. a final dump, then tall and events are dropped;
4. the outbox stops copying the submission's form data into its `payload`, and its retention is decided.

**Rollback:** none after step 3, except the dump.

## Open

- **Phase D (owner):** whether tall, events and pivots may retire without the rollback margin, and whether a final
  dump may be their only archive.
- **For downstream:** the gap measurement above; schema `feed` added at phase B, with the first sync as §13 says; the
  Airbyte role's name, for the grant.
