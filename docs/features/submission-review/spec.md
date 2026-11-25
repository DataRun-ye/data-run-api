# Submission review: specification

Status: DESIGNED, NOT BUILT (2026-11-25). The words are those of [CONTEXT.md](CONTEXT.md). The v2 conventions
this feature uses are built on `develop` and described in [api-v2.md](../../agent-context/api-v2.md); this
document adds what is particular to the feature.

**Origins.** Each decision is labelled **owner** (the owner's answer or direction, dated: evidence of intent),
**operations** (a fact of operations' work) or **engineering** (practice, measured debt, safety). Counts come from
read-only measurements on a production clone holding 67,176 submissions (November 2026).

## 1. The problem

Operations review submissions every day, and need to change the submissions of any form, not only those that use
lists (owner, 2026-11-25). Today only the admin can change a submission, through v1 routes that skip the upload
checks, record no reason, and are undone by the next phone re-send. Nothing moves or splits a submission. The
admin's tools use this API first; a screen for operations follows (owner, 2026-11-25).

### 1.1 The review cases (owner, 2026-11-25)

| Case | What a user needs | Why it is hard today | Capability |
| --- | --- | --- | --- |
| Correcting household data, in any row of any form | Change a value; pick the right list entry; remove a row recorded twice; undo | Only an admin re-send of the whole submission. A row can be found only in raw JSON | Search, view, edit |
| Changing campaign days | Select the wanted day option, like any other value | As above | Edit (`setValue`) |
| Moving wrongly recorded households between villages | Move a whole submission, or split rows into a new one | No route. Operations delete the submission and the team leader re-enters up to 250 names on a phone: new ids, new list entries (now listed twice), lost history, and rows recorded twice while both copies exist | Move, split |
| Resolving what was recorded twice | Decide recorded twice or served twice; remove the extra row or delete the extra submission; keep the decision | Admin delete only: whole submissions, no reason, phones never told, and a later re-send still changes the deleted submission. Past campaigns kept the decisions in spreadsheet columns | Search by entry; `removeRow` or delete with a reason; a note; history |
| Reconciling warehouse quantities | Fix a typo; record a difference that happened or was found later | No server-side edit, and no running balance anywhere | Edit for a typo; an adjustment for a real difference |

Decisions on the cases (2026-11-25):

- Changing campaign days is a plain `setValue`, like any other value (owner).
- Rows go into another submission only by a split into a new one. Moving rows into an existing submission is not
  built: no case needs it, an org unit can hold several submissions, and analytics aggregates by user, team,
  assigned village and houses (owner).
- A warehouse typo is an edit, and history keeps the original. A difference that happened or was found later (a
  loss, damage, a recount) is an adjustment: a new submission with a signed quantity and a reason, in the
  activity's stocktake form or an adjustment row type that operations configure. Balances stay downstream sums; no
  stock engine is built (engineering, on the owner's answer). A field that must take negative numbers needs a
  signed value type: a form-configuration change by operations, with no code here.

### 1.2 Constraints

**C1. Phones work offline.** They push submissions and never download them, so a server-side change never reaches
a phone and the team keeps seeing its own copy. A re-send carries the whole document and no version. After any
failure a phone forgets that the server holds the submission. Apps 5.2 and 5.3, on the legacy route, send rows
without ids, and the server mints new ids at every re-send. Today a move or a handover can make the original team's
copies fail (E4124, E4114, E4122). Most later copies are retries: 96.6% of re-sends arrive within an hour of the
previous change.
So: the server answers `updated` to every upload item and decides alone, from the journal, whether a copy is an
unchanged re-send or a conflict; nothing a server-side change does may fail an upload item; a copy without row ids is
never matched row by row; teams learn of server-side changes by other channels until an app release can show them.

**C2. List entries stay where they are.** Phones pick entries from an org unit's list. An upload's entry must belong
to the submission's org unit (E4119) and appear at most once per field in a submission (E4118); a new name becomes
an entry in the assignment's org unit (E4120). Entries never move or retire: a phone's catalog sync fails when an
entry it holds comes back under another org unit, and a team holds a median of 21 villages' catalogs. Nothing
checks a household served in two submissions; listed twice, served twice and links belong to the lists feature.
So: no change re-homes an entry; only accept creates entries, in the copy's own org unit, as the phone's upload
would have; rows keep their entries through moves and splits, and entries outside placement are reported and
tracked; `setEntry` picks an entry of the submission's current org unit.

**C3. Placement is an assignment.** It binds a team, an org unit and an activity, and an org unit can have two
teams' assignments. Phones authorize by team scope and team form grants.
So: a move stays inside one activity, as access grants are per activity, and its destination is an assignment
that carries the form.

**C4. Downstream reads tall and events until it reads the feed.** For each change the ETL upserts every present
value with the current placement, marks events deleted on a delete and unmarks them on the next update, and never
removes a value or a row. On a row it already holds, tall keeps the old `submission_id`, `submission_uid` and
serial, while events updates them.
So: the ETL's split re-attribution makes tall's update set those three columns too (one statement, with a test);
row ids are unique across live submissions, so only split rows move. Clears and row removals wait for the feed.

**C5. One ordered journal.** Since 6.5.6 every change is one revision, written with one outbox row in the change's
transaction. So a split writes a revision and an outbox row for each of its two submissions in one transaction,
history shows lineage, and the base version comes from the journal.

**C6. Permissions.** Today only the admin role changes submissions, and it also changes users, teams and forms;
team form grants drive phones. So: one access grant per part of the work, scoped by activity, apart from team form
grants; admins grant themselves what they use.

## 2. Requirements

| # | Requirement | Origin |
| --- | --- | --- |
| 1 | Search any form's submissions by activity, form, team, assignment, org unit, area, created and entered times, deleted, open conflict and any field's value; keyset paging, no totals | owner, 2026-11-25; engineering |
| 2 | View one submission: current state, version as `ETag`, history, notes and conflicts | owner, 2026-11-25 |
| 3 | Change any element: values, rows, the picked entry, the placement (move), rows into a new submission (split), delete and restore | owner, 2026-11-24 and 2026-11-25 |
| 4 | Record every change with who (a durable user id), when, why (a required reason) and its version | owner, November 2026; engineering (a required reason, as in clinical data capture) |
| 5 | Keep a phone's changed copy aside after a server-side change, for a person to resolve; an unchanged re-send changes nothing; the phone is answered `updated` | owner, 2026-11-24; refined by engineering, 2026-11-25 |
| 6 | Run no form rules on the server; validate value types, option codes, mandatory fields, row shape and list entries | owner, 2026-11-24 |
| 7 | One permission per part of the work, scoped by activity; no hardcoded approval workflow | owner, 2026-11-25 |
| 8 | Move and split for any form; no moving rows into an existing submission | owner, 2026-11-25 |
| 9 | Downstream reads the current state through the feed, deleted records included | owner, 2026-11-25 |
| 10 | Ids are opaque strings, never the uid | owner, 2026-11-24; engineering, 2026-11-25 |
| 11 | The wire format is not the storage format; ordinary change stays inside v2 | owner, 2026-11-24 |
| 12 | Installed phones never break, and no upload item fails because of a server-side change | engineering |
| 13 | Changes are reversible; resolving a conflict never drops server-side edits silently; a split never duplicates a row or a value | engineering |
| 14 | Search never scans without bound | engineering |
| 15 | Deployment and activation are separate | engineering |
| 16 | No values, reasons, notes or logins in logs; this feature's tables stay out of the analytics copy | engineering; owner, 2026-11-25 |
| 17 | List entries are never re-homed | engineering |
| 18 | Things that happen later are new submissions; the past is never rewritten | engineering, on the owner's answer, 2026-11-25 |

## 3. The capability set

Every changing method takes `If-Match`, an `Idempotency-Key`, a required `reason` and an optional `dryRun` (§9,
R4). Every method has one activation switch, and a switched-off method answers 422 `method-not-enabled`. Inside
`:edit`, each edit operation has its own switch too, checked after the method's: a request naming a switched-off
operation is refused whole with 422 `operation-not-enabled`, pointing at that operation (engineering, 2026-11-25).
Conflict detection is a requirement of every change switch, so no change can be turned on before it.

| Capability | Method | Permission | Revision: kind / command | Gate |
| --- | --- | --- | --- | --- |
| Search | `GET /api/v2/submissions`; `POST /api/v2/submissions:search` | view | — | A |
| View | `GET /api/v2/submissions/{id}` | view | — | A |
| Form dictionary | `GET /api/v2/forms`; `GET /api/v2/forms/{id}/versions/{number}` | view | — | A |
| History | `GET /api/v2/submissions/{id}/revisions`; `…/revisions/{version}` | viewHistory | — | A |
| Notes | `GET`, `POST /api/v2/submissions/{id}/notes` | view to read; addNotes to add | none | A |
| Edit | `POST /api/v2/submissions/{id}:edit` | change | `UPDATE` / `EDIT` | per operation |
| Move | `POST …/{id}:move` | change | `UPDATE` / `MOVE` | A |
| Split | `POST …/{id}:split` | change | source `UPDATE` / `SPLIT`; new `CREATE` / `SPLIT` | A, once the split re-attribution is in production |
| Delete | `POST …/{id}:delete` | change | `DELETE` / `DELETE` | A |
| Restore | `POST …/{id}:restore` | change | `RESTORE` / `RESTORE` | A |
| Conflicts | `GET /api/v2/conflicts`; `GET …/{id}/conflicts/{conflictId}`; `POST …:keep`; `POST …:accept` | read: change or viewHistory; decide: change | accept: `UPDATE` / `CONFLICT_ACCEPT`; keep: none | A |
| Access grants | `GET`, `POST /api/v2/access-grants`; `DELETE /api/v2/access-grants/{id}` | admin | — | — |

| Edit operation | What it does | Gate |
| --- | --- | --- |
| `setValue` | Sets a field's value, at the top level or in a row | A |
| `setEntry` | Picks another list entry in a row's field | A |
| `restoreRow` | Puts a removed row back from a named revision | A |
| `clearValue` | Clears a field's value | B |
| `removeRow` | Takes a row out of the current state | B |
| `addRow` | Adds a row; later, by need | — |

**Validation** (owner, 2026-11-24: no form rules run on the server; each edit records `formRules: NOT_RUN`):

- **`setValue`, `clearValue`.** The value type and option code of the field in the pinned form version are checked;
  option values are stored as codes. The field must exist in that version, or its value must already sit at a path
  another version of the form defines. Clearing is `clearValue`, never `setValue` with null. Clearing a mandatory
  field answers 422 `mandatory-field-empty` unless `overrideMandatory` is set; the override is recorded with the
  reason.
- **`setEntry`.** The entry exists, belongs to the submission's current org unit, and is not in another row of the
  submission at that field (E4118); otherwise 422 `entry-outside-placement` or `entry-already-in-submission`. A
  household used in another submission of the activity is reported, not refused: whether it was served twice is a
  person's decision. It never creates an entry.
- **`removeRow`, `restoreRow`.** A removed row leaves the current state and stays in history. `restoreRow` puts it
  back from a named revision with the same id. It is refused with 409 `row-live-elsewhere` while that row id is live
  in any submission of the lineage (I5), and with 422 `entry-already-in-submission` while its entry now sits in
  another row at that field.
- **`addRow`** (later): a new row id, every mandatory field of the new row, and entries that exist in the org unit.
- **Reported, not enforced:** issues already elsewhere in the document.
- **Row ids are stored by the changes that write the document** (engineering, 2026-11-25). Every server-side change
  that writes the document, an edit, a split or an accept, stores in the same revision the shown id of each row
  lacking a unique stored id: its derived id, or the `|<attempt>` id of a row whose id an earlier row already has.
  So a server-side change never changes a row id, rows without ids that an accept brings in included. Move, delete
  and restore never write the document and store nothing; derived ids depend on neither placement nor deletion.
  The feed's `id_origin` turns `STORED`, with the same value, at the first change that stores the id.
- **What still changes row ids:** a phone upload that applies replaces the document, as today. Apps from 6.0 send
  their rows' ids; for rows sent without ids (apps 5.2 and 5.3), the legacy route mints new ones. With conflict
  detection on, such a copy is kept aside whenever a server-side change exists after its base version, so this
  applies only to uploads with no server-side change since their base.

### 3.1 Search

**Contract** (engineering, 2026-11-25; one set of names for both forms):

- `GET /api/v2/submissions` takes `form` (required); `activity`, `team`, `assignment`; `orgUnit` (one org unit) or
  `area` (an org unit and everything under it); `createdFrom`, `createdTo`; `enteredFrom`, `enteredTo`;
  `showDeleted`; `hasOpenConflict`; `fields`; `limit`, `cursor`. No bare `status`.
- **Deleted submissions** are left out unless `showDeleted` is `true` (a boolean, `false` by default); then they
  are included, each marked `deleted: true`. There is no three-value `deleted` parameter (engineering,
  2026-11-25, following AIP-164).
- `POST /api/v2/submissions:search` takes the same names as JSON members, plus `where`: conditions
  `{"field", "op", "value"}` with `op` one of `eq`, `ne`, `lt`, `le`, `gt`, `ge`, `in`, `isNull`, `notNull`,
  `contains`. Values are typed JSON, compared by the field's value type. It is a safe read.
- **Row semantics:** conditions on one repeating section hold in the same row; a condition on a nested section holds
  in a row under that row; conditions on unrelated sections each need some row. Matching row ids come back with each
  submission.
- **Values out** are typed by the pinned form version. A stored value that does not parse is returned raw and listed
  in `valueIssues`.
- **`created`** is `data_submission.created_date`, when the server stored the submission: an upload's receipt, a
  go-live release's time (the phone's earlier receipt stays with the held upload), or a split's time for a
  split-off. **`entered`** is the phone's start and end times (engineering, 2026-11-25).

**The guard** (engineering, 2026-11-25). A field filter or an entered-time filter needs one indexed narrowing: a
team, an assignment or an org unit; an `area` of at most a set number of org units; or a created range of at most a
set number of days, which maps to a `serial_number` range through the serial index. Otherwise the answer is 422
`search-too-broad`. A form listed newest first needs no narrowing.
Every search runs under a timeout (proposed 5 s) and answers 503 `search-timeout` when it fires. An entry search is
narrowed by the entry's org unit and also includes the submissions tracked as holding the entry outside their
placement (§4.3), so it finds rows moved or split elsewhere.

**Why the guard:** `data_submission` has no index on team, activity or dates. A field filter across one form
visited every row (209 ms, about 3 µs a row); narrowed to one village by an index it took about 1 ms.

**How** (engineering, 2026-11-25). Now: SQL on `data_submission` under the guard, with B-tree indexes on (form,
serial), (form, team, serial) and (form, org unit, serial), the assignment index, and a pattern index on
`org_unit.path`; field filters are predicates on the data paths each form version gives a field. The scaled
measurement (build step 6) decides whether a `jsonb_path_ops` GIN is added for form-wide equality. Later: a
server-owned read model in schema `submission_review`, written by the feed's publisher from the same normalized
submissions. Rejected: a read model written in the upload transaction (load on the phones' path), and the feed's
tables (they would tie this feature's indexes to downstream's additive-only contract). A test proves that the SQL
filters and the submission normalizer select the same submissions and rows.

### 3.2 View, history, notes and the form dictionary

- **View** answers the current state with values typed by field id, rows with their ids, entries expanded with name
  and org unit, `version` (also the `ETag`), `hasOpenConflict` and the note count.
- **History** lists revisions with their command, actor, on-behalf-of user, reason and time, and the differences
  between consecutive states by field id and row id; a split shows in both submissions' histories. Submissions
  stored before the journal started begin with one baseline revision.
- **Notes** are plain remarks on a submission or one of its rows (`row` optional), with author and time. They make
  no revision and never cause a conflict. There is no question-and-answer workflow (owner, 2026-11-25).
- **The form dictionary** gives each form version's field ids, names, labels, value types, option codes and sections.

### 3.3 Delete and restore

- `:delete` marks a live submission deleted, with a reason. It stays with its history, searches leave it out
  unless `showDeleted` is set, and the feed publishes it as deleted. On a deleted submission: 409 `already-deleted`.
- `:restore` makes it live again. On a live submission: 409 `not-deleted`. While one of its rows is live in another
  submission of its lineage: 409 `row-live-elsewhere` (I5).
- The v1 admin delete (`DELETE /api/v1/dataSubmission/{uid}`) records no reason and acts outside the access grants.
  It retires once `:delete` is on and the logs show its callers have moved (engineering, 2026-11-25).

### 3.4 Not built, by decision (owner, 2026-11-25)

- **Review status or approval.** Operations do not mark their reviews, and an approval process must not be hardcoded
  unless it is data. If marks are wanted later, they are added as data (engineering, 2026-11-25): defined per
  activity like option sets (code, label, order); applied by `POST …/{id}:mark` with a mark's code and a reason;
  stored in one small table in `submission_review` and shown in history beside revisions; one more permission (for
  example `submissions.mark`); an additive search filter. A mark changes no version, so it causes no conflict and
  stays out of the feed. Nothing else depends on marks, so adding them is additive.
- **Moving rows into an existing submission.** Split covers the need.
- **A feedback workflow** with recipients and states. Feedback is plain notes.

### 3.5 From spreadsheet columns to actions and history

Past campaigns kept their review decisions in spreadsheets, with ad-hoc columns such as `deleted`, `duplicate_1` and
`duplicate_2`, added by different people in different passes (owner, 2026-11-25). Here a `deleted` column is
`:delete` with a reason, undone by `:restore`. A recorded-twice decision is an action, `removeRow` or `:delete` with
that reason; a household served twice stays, with a note. Free text is a note. Columns recording who did what are
history, kept for every change. A value typed over a wrong one is an edit, and history keeps the original. A pass
marking rows as checked is not built (§3.4).

## 4. Move and split

### 4.1 Move

```http
POST /api/v2/submissions/{id}:move
If-Match: "4"
Idempotency-Key: 3c51…
{"reason": "village handed to team T415", "destination": {"assignment": "…a15"}}
```

**Rules.** The destination is an assignment of the same activity that carries the form and is not deleted;
otherwise 422 `destination-other-activity`, `form-not-on-destination` or `destination-deleted`. The current
assignment answers 409 `already-at-destination`, and a deleted submission 409 `already-deleted`.

**Effects, in one transaction**, after the key claim, the submission lock and `If-Match`: the placement columns and
their copied labels are set from the destination, the stored document is untouched, and the version rises by one;
the journal records `UPDATE`, command `MOVE`, with detail `{from, to, entriesOutsidePlacement}`; one outbox row; the
tracking of entries outside placement is updated (§4.3). Downstream, the ETL re-projects every value with the new
placement and the feed carries it: gate A.

**Phones holding the original** (engineering, 2026-11-25). These rules apply only to an uploader with a base version
of their own (§7); everyone else's refusals stay as today.

1. An unchanged re-send, or a copy equal to the current state, changes nothing, opens no conflict and is answered
   `updated`, even where the upload checks would refuse it, since nothing is written. Repeated re-sends after a move
   therefore never reopen a kept conflict.
2. Once a `MOVE` revision exists after the uploader's base version, a changed copy that fails authorization is kept
   aside as a conflict (cause `SERVER_SIDE_CHANGE`) and answered `updated`: E4124 (the stored assignment is outside
   the uploader's scope, or the phone moves the submission itself), or E4114 and E4122 (the copy's own assignment was
   handed over).
3. Otherwise a changed copy that passes the upload checks goes on to conflict detection.
4. Without a `MOVE` revision, today's refusals of changed copies stand. A handover made only in configuration, with
   no move, is an existing gap outside this feature.

These rules ship in build step 2, inside the never-fail handling, and take effect only once moves exist.

**Conflicts afterwards.** An open conflict survives a move. Accept never changes placement: it validates the copy,
and creates any new names as entries, against the copy's own assignment; those rows are then tracked as outside
placement.

**Undo:** move it back, with a reason.

### 4.2 Split

```http
POST /api/v2/submissions/{id}:split
If-Match: "0"
Idempotency-Key: 6f1d…
{"reason": "23 households of V9 recorded under V7",
 "rows": ["…k1", "…k2", "…"],
 "destination": {"assignment": "…a9"},
 "topLevel": [{"field": "…d1", "op": "copyValue"},
              {"field": "…p2", "op": "setValue", "value": 23},
              {"field": "…n3", "op": "leaveEmpty"}]}
```

The answer is `201 Created`, with `Location: /api/v2/submissions/{newId}` and `ETag: "0"` for the new submission;
the body names the source's new version.

**Rules.**

- **Rows:** top-level rows of one repeating section only; nested rows travel with their parent. A nested row alone
  answers 422 `row-not-top-level`; taking every row answers 422 `split-takes-all-rows` (that is a move).
- **Top-level values** (engineering, 2026-11-25). Every top-level field holding a value in the source is named once
  in `topLevel`: copied (`copyValue`), set (`setValue`, validated like an edit) or left empty (`leaveEmpty`). A
  field not named answers 422 `top-level-field-unstated`, listing the fields. Nothing is copied implicitly, because
  a copied number adds to downstream totals: 29 of the 45 forms with a repeating section hold numeric fields outside
  every repeat. A mandatory field left empty needs `overrideMandatory`. The dry run lists the numeric fields marked
  to be copied.
- **Unmapped top-level values** have no field id, so they cannot be named: they stay in the source, and the dry run
  lists them by path. Unmapped values inside a split row travel with the row.
- **The source** keeps its own top-level values. It must not be deleted.
- **The destination** follows the move's rules. It may be the source's own assignment, to separate two visits
  recorded as one.

**Effects, in one transaction:**

1. the key claim, the source's submission lock, `If-Match`;
2. the new submission: a new id and a server-made uid (the v1 alias, known to no phone); the source's form, pinned
   form version, entered times and creator (its stored `created_by`, the person who entered the rows); the
   destination's placement; exactly the named top-level values, and the selected rows with their ids and nested
   rows; version 0, recorded as `CREATE`, command `SPLIT`, with detail `{splitFrom: {submission, version}, rows,
   topLevel, entriesOutsidePlacement}`; an outbox `SAVE` row;
3. the source: the rows removed and its version raised by one, recorded as `UPDATE`, command `SPLIT`, with detail
   `{splitTo, rows}`; an outbox `UPDATE` row;
4. the tracking of entries outside placement, for both submissions;
5. the idempotency record, naming the new submission; a replay finds the source's version through the journal.

**Downstream.** With the split re-attribution in production, the new submission's projection moves the split rows
in tall and events, so a split reaches downstream before the feed (gate A). The feed's publisher writes both
submissions in one feed transaction: the source's records of the split rows show `deleted = true`, and the new
submission's records carry them.

**Phones holding the original.** No phone holds the new submission. The source's phone holds all the rows: an
unchanged re-send matches its base version and changes nothing, and a changed copy is kept aside as a conflict.
Plain accept is refused with 409 `row-live-elsewhere` while the copy holds rows live in another submission of the
lineage; accept and re-apply re-applies the split. A copy whose rows lack ids is detected before any id is minted.
Its rows cannot be matched with the split rows, so both kinds of accept answer 409 `phone-copy-rows-without-ids`
once any row-level server-side change exists after its base version, and keep is the only resolution (engineering,
2026-11-25).

**Undo:** delete the new submission, then `restoreRow` the rows in the source from the revision before the split.
I5 refuses the opposite order, and refuses to restore the new submission while its rows are live in the source.

### 4.3 List entries

- **No change re-homes an entry:** a changed org unit would fail the catalog sync of every phone holding the entry.
  Only accept creates entries, for new names in a phone copy, in the copy's own org unit, as the phone's upload
  would have. E4118 holds in each submission after every change.
- **`entriesOutsidePlacement`** lists the rows whose entry belongs to another org unit than the submission's, in
  answers, dry runs and history. A small table, `submission_review.entry_outside_placement` (submission, row, field,
  entry), tracks them: maintained in the transaction of every move, split and accept that leaves such rows, and
  cleared by the `setEntry`, removal or move back that resolves them. Entry searches include the submissions it
  lists (engineering, 2026-11-25).
- **After a move or split,** `setEntry` re-points rows to the destination's entries where they exist. Entries left
  without rows stay on phones, as after today's re-entry workaround; retiring and linking them is the lists
  feature's work.
- **The view marks entries added on a phone** rather than loaded from a list. That helps choose the rows to split:
  households recorded under the wrong village were most likely typed in as new names.

## 5. Invariants, and how they are tested

| # | Invariant | Test |
| --- | --- | --- |
| I1 | One writer: every changed submission gets exactly one revision and one outbox row, in one transaction, both of a split's submissions included | Failure injection after each insert of a split leaves nothing; ArchUnit and the SQL tripwire |
| I2 | No upload item of an uploader with a base version of their own fails because of a server-side change. Unchanged re-sends and copies equal to the current state change nothing and open nothing, checked first; changed copies, and changed copies refused after a server-side move, are kept aside. Other uploaders are refused exactly as today | Characterization goldens for each installed phone contract after edit, move, split, delete and restore, copies without row ids included: never a refusal or an unexpected failure. An unchanged re-send after a move, repeated, opens no conflict; a kept conflict stays kept under repeated unchanged re-sends; another user's copy of a submission older than the journal is refused as today and never enters the queue |
| I3 | A move changes placement only, to an assignment of the same activity that carries the form; accept never changes placement | The stored document and row ids compared byte for byte, before and after; refusal tests |
| I4 | Conservation: a split neither loses nor duplicates a row; the new submission holds exactly the named top-level values; unmapped top-level values stay in the source | On a clone, split a random half of every multi-row submission with random top-level choices, then compare rows and top-level totals with the request, through the submission normalizer |
| I5 | A row id is live in at most one submission of a lineage, splits of splits included; `:restore`, `restoreRow` and accept refuse to break it | Lineage checks after every step of the randomized sequences |
| I6 | No change re-homes an entry; only accept creates entries, in the copy's org unit; E4118 holds; the tracking table matches the data | The entry table compared before and after each scenario; the tracking compared with an independent scan |
| I7 | Lineage is recorded both ways | The two split revisions name each other; history shows both |
| I8 | A switched-off method or edit operation is refused whole | 422 `method-not-enabled` and `operation-not-enabled` tests |
| I9 | Once the ETL or the publisher has caught up, each row counts once, under its current submission and placement | ETL tests: a move updates tall and events; a split re-attributes them. A feed test against the journal |
| I10 | A replay returns the first outcome, a split's included | Replay tests, including a reused key with another body |

**Randomized sequence tests** on clone copies interleave edit, move, split, delete, restore, `restoreRow`, keep and
accept with phone re-sends, unchanged and changed, with and without row ids, and check I1–I10 after every step
(engineering, 2026-11-25). Move and split get no spike: their risks are these invariants.

## 6. Concurrency and idempotency

The v2 foundation's rules apply as built ([api-v2.md](../../agent-context/api-v2.md), Concurrency and Changing
Requests). A submission's `ETag` is its version (`"4"`, also shown as `version`; a conflict's is its own, §7), and
`If-Match` is required on every changing method (428 when missing, 412 when stale). Each changing transaction
claims the `Idempotency-Key`, takes the submission lock (`SELECT … FOR NO KEY UPDATE`, the first read), checks `If-Match`,
validates and applies, records the outcome on the key, and commits; a rolled-back attempt takes its claim with it.

This feature adds (engineering, 2026-11-25): the upload routes and the v1 admin delete take the same submission lock
first, and the stored-submission guard, conflict detection and the save read the submission under it. A latch test
proves that no upload item fails when a change commits between two reads.

## 7. Conflicts

Phones send no version and must be answered `updated`; most re-sends are quick retries, and most later ones are
identical. **The base version** of an uploader's copy is the newest revision whose state equals what they last sent:
their `UPLOAD` or `LEGACY_UPLOAD` revisions; a `HELD_RELEASE` on their behalf; a plain `CONFLICT_ACCEPT` of their
copy; or a live baseline, only when the uploader is the submission's creator: its stored `created_by`, compared as
`StoredSubmissionGuard` compares it (engineering, 2026-11-25). Any other uploader has no base version of their own:
they never uploaded the submission, and it is older than the journal or someone else made it.

**First,** a copy is tagged if any of its rows lacks an id, before any id is minted, on both upload routes.

**The decision,** for each upload item of an existing submission, under the submission lock, in this order
(engineering, 2026-11-25):

- **For an uploader with a base version of their own:**
  1. A copy equal to the base version's state, or to the uploader's last decided conflict copy, is an unchanged
     re-send: it changes nothing and opens nothing, so a kept conflict stays kept.
  2. A copy equal to the current state changes nothing and opens nothing.
  3. After a server-side `MOVE` later than the base version, a changed copy that fails authorization (E4124, E4114,
     E4122) is kept aside as a conflict. Any other refusal stays a refusal.
  4. A server-side change after the base version keeps the copy aside as a conflict. Server-side changes are `EDIT`,
     `MOVE`, `SPLIT`, `DELETE`, `RESTORE`, another uploader's accept, `ADMIN_DELETE`, and a deleted baseline.
  5. Otherwise the upload applies as today.

  Steps 1 and 2 come before the upload checks. Nothing is written, so a copy the checks would refuse after a move is
  still answered `updated`.
- **For any other uploader,** the upload checks run first, exactly as today, and their refusals stay refusals, so
  these copies never enter the conflict queue. An authorized copy takes the submission's first revision as its base,
  so any server-side change since then keeps it aside; otherwise it applies as today.

**The comparison is structural** (engineering, 2026-11-25): two documents without row metadata, together with their
deleted flag, assignment and form version. It does not go through the submission normalizer.

- **Row metadata** is exactly the normalizer's row structure keys: `_id`, `_index`, `_parentId` and
  `_submissionUid`. Everything else compares, the top-level `_status` included, which only phones write. Build
  step 2's measurement (every submission re-sent unchanged gives 0 conflicts) checks that phones do not change
  `_status` between re-sends; if they do, it joins the ignored keys on that evidence.
- **The deleted flag counts:** a `deleted: true` copy against a live state is a change (golden W1-14).

**It never fails an upload item:** an exception in detection keeps the copy aside with cause `DETECTION_ERROR`.
Only a failure to store the conflict can fail the upload item.

**Storage:** `submission_review.conflict` holds one open conflict per submission and uploader; its state (`OPEN`,
`KEPT`, `ACCEPTED`) and `cause`; the base version and whether the copy's rows lack ids; the copy as it arrived, its
first and last receipt times, a re-send count and the conflict's own version; and the decision. The held-upload table
is not reused: it allows one row per uid and treats released rows as final.

**A re-send while the conflict is `OPEN`** (engineering, 2026-11-25): a copy equal to the conflict's copy changes
only its re-send count and last receipt time. A different copy replaces the conflict's copy, keeps the first
receipt time and raises the conflict's own version, which changes its `ETag`: a decision made on the old view
answers 412 `precondition-failed`.

**Resolution** (decided with `submissions.change`; read with it or `submissions.viewHistory`):

- **The view** shows three columns, the base version, the current state and the phone copy, with each value and row
  marked as changed on the phone, on the server, or both.
- **The conflict is a versioned resource** (engineering, 2026-11-25). Its strong `ETag`, opaque to clients, covers
  what the conflict view shows: it changes when the phone copy changes (the conflict's own version) or when the
  submission's version does. `:keep` and `:accept` send `If-Match` with that `ETag`; a stale tag answers 412
  `precondition-failed`, and the resolver views the conflict again. So every decision is made on the three-way view
  the resolver actually saw. The representation has no `version` member, since R9 keeps `version` for the
  submission; the items of `GET /api/v2/conflicts` carry each conflict's `ETag` as `etag`.
- **Keep** closes the conflict with a reason. The submission is unchanged, and no revision is written.
- **Accept** makes the copy's values and rows the state, by the resolver on behalf of the uploader. Placement never
  changes; validation and new entries use the copy's own assignment. It is also refused with 409
  `row-live-elsewhere` while the copy holds rows live elsewhere in the lineage, and with 409 `already-deleted` on a
  deleted submission.
- **Accept and re-apply** (`reapplyEdits`) also replays, in order and in the same revision, the edit operations of
  the server-side edits since the base version, with splits replayed as row removals.
- **A copy whose rows lack ids:** both kinds of accept answer 409 `phone-copy-rows-without-ids` once any row-level
  server-side change exists after its base version (an operation addressed to a row, a removal, a restore or a
  split). Keep is the only resolution.

## 8. Permissions

| Part of the work (owner, 2026-11-25) | Permission | Allows |
| --- | --- | --- |
| Monitoring | `submissions.view` | Search, view (deleted submissions included), the form dictionary, reading notes |
| Auditing and reviewing | `submissions.viewHistory` | History, conflicts, actors' names |
| Making changes | `submissions.change` | Edit, move, split, delete, restore; keep and accept conflicts |
| Feedback | `submissions.addNotes` | Adding notes |

Engineering, 2026-11-25:

- **Access grants** live in `submission_review.access_grant`, unique while live on (user, permission, activity), and
  admins manage them through `/api/v2/access-grants`. A revoked grant is kept.
- **One predicate per permission:** the activity is among the caller's live grants of it. Holding `change` or
  `addNotes` implies `view` on the same scope.
- **Admins** manage grants and hold no permission they have not granted themselves.
- **Team form grants** stay separate: they drive phones.
- **No approval workflow** (owner, 2026-11-25). Marks, if ever wanted, add one permission (§3.4).
- **Later scopes:** an area (by ancestor paths, with a pattern index on `org_unit.path`), a form, a team. A move or
  split then needs `change` on both placements.
- **Everything stored** names the caller by user id, never by login.

## 9. API conventions

The v2 conventions apply as built ([api-v2.md](../../agent-context/api-v2.md)). This feature's naming rules come
from a study of operations' words, field data platforms and API design guides (engineering, 2026-11-25):

- **R1. Nouns come from the glossary.** Collections are plural, path segments kebab-case, JSON members and query
  parameters camelCase. Resources: `submissions`; `submissions/{id}/revisions/{version}`; `submissions/{id}/notes`;
  `submissions/{id}/conflicts/{conflictId}`; `conflicts` (the open conflicts across submissions);
  `forms/{id}/versions/{number}`; `access-grants`.
- **R2. Standard methods where they fit.** List and get read; create makes notes and access grants; delete revokes an
  access grant, which stays recorded. Submissions get no PUT and no PATCH: every change is a named method with its
  own validation, permission, switch and journal command. Rejected: JSON Patch on the representation (it cannot
  express a split, and leaves intent to be inferred from paths) and a full replace (history must diff, and dropped
  fields are deleted). A JSON Patch media type can be added to `:edit` later.
- **R3. Custom methods carry intent:** `POST /api/v2/{resource}:{verb}`, verbs from the glossary in camelCase, with
  no prepositions. Submissions: `:edit`, `:move`, `:split`, `:delete`, `:restore`. Conflicts: `:keep`, `:accept`.
  `submissions:search` is a safe read sent as a POST because its filter body is large. Each changing method is one
  permission check, one switch (plus one per edit operation inside `:edit`) and one journal command of the same
  name: `EDIT`, `MOVE`, `SPLIT`, `DELETE`, `RESTORE`, `CONFLICT_ACCEPT`. Keep writes no revision. `:delete` is the
  one deliberate reuse of a standard verb: it needs a required reason in a body, and content in a DELETE request has
  no defined meaning (RFC 9110). The colon keeps actions apart from sub-collections; ids never contain one.
- **R4. Every changing method takes the same four things:** `If-Match`, an `Idempotency-Key` (a UUID), a required
  `reason`, and an optional `dryRun`.
- **R5. An edit's list is `operations`, and each item has an `op`** (JSON Patch's names): a camelCase verb and noun.
  Operations apply in order, all or none, against the `If-Match` version. `op`, not `type`, because `type` is the
  problem type in errors that point into the same body.
- **R6. Nothing is meant by null or by omission.** Clearing is `clearValue`, never `setValue` with null. Whatever a
  request leaves out is untouched.
- **R7. Targets by id only:** `field`, `row`, `entry`, `assignment`, `submission`; never names, section paths or
  array indexes. Move and split name their target `destination`. Responses expand references as `{id, code, name}`.
- **R8. No write masks; reads use `fields`** to limit which fields' values come back.
- **R9. One name per state, and never a bare `status`.** States: `deleted` (a boolean, as in the table, the journal
  and the feed) and `hasOpenConflict`. `version` is the submission's revision number and `formVersion` the form's;
  the API's own version appears only in the path. Times: `created…` for when the server stored the submission,
  `entered…` for the phone's times. `orgUnit` filters one org unit, `area` an org unit and everything under it.
- **R10. Spelling follows the word's role:** enumeration values UPPER_SNAKE; verbs and ops camelCase; problem codes
  kebab-case.
- **R11. Errors are RFC 9457 problems with a `code` from one registry.** A code names the problem, not the remedy.
  Values and reasons are never repeated in an error. A 409 never carries the bare code `conflict`, so that it cannot
  be mistaken for a submission's conflict. Published codes are contract.
- **R12. `reason` is only ever a person's text;** codes the server assigns are `cause`.
- **R13. Paging** is `limit` and `cursor`, answered with `{items, next}` and `Link: rel="next"`, without totals.
- **R14. Batches, if the tools need them,** are `:batchEdit`, `:batchMove` and so on, all or none like single
  requests. v2 never says "bulk".
- **R15. Long-running work is a job,** never an operation.

**Problem codes this feature adds** (engineering, 2026-11-25):

| Area | Codes |
| --- | --- |
| Search | `search-too-broad` (422); `search-timeout` (503) |
| Validation (422) | `mandatory-field-empty`; `not-an-integer` and the other value-type codes; `not-an-option-code`; `field-not-in-form-version` |
| Entries (422) | `entry-outside-placement` (`setEntry`); `entry-already-in-submission` (`setEntry`, `restoreRow`) |
| Rows | `row-not-top-level`, `split-takes-all-rows`, `top-level-field-unstated` (422); `row-live-elsewhere` (409) |
| Destinations | `destination-other-activity`, `form-not-on-destination`, `destination-deleted` (422); `already-at-destination` (409) |
| State (409) | `already-deleted` (delete, move, split or accept on a deleted submission); `not-deleted` (restore) |
| Phone copies (409) | `phone-copy-rows-without-ids` |

The switches' codes, `method-not-enabled` and `operation-not-enabled`, are already in the registry.

## 10. The journal and the feed

| Piece | From the journal | From the submission normalizer, the ETL and the feed |
| --- | --- | --- |
| View, `ETag` | The version equals the head's | The state by field id and row id, typed |
| History | Revisions: kind, command, actor, on behalf of, reason, time; split lineage in `detail` | Differences between consecutive states |
| Changes | One writer: the submission, its revision and its outbox row in one transaction (two of each for a split) | The write direction: from field id and row id to the stored place |
| Conflicts | Base versions, server-side changes, the operations to re-apply | None: the comparison is structural |
| Search | — | The data paths each form version gives a field now; a read model later |
| Downstream | Each change is revisions and outbox rows | The ETL's split re-attribution now; the feed later |

**The journal grows additively** in this feature's changeset (engineering, 2026-11-25): the kind `RESTORE`; the
commands `EDIT`, `MOVE`, `SPLIT`, `DELETE`, `RESTORE` and `CONFLICT_ACCEPT` (the v1 admin delete keeps
`ADMIN_DELETE` until it retires); `reason` required for those commands; `on_behalf_of_user_id` allowed for
`CONFLICT_ACCEPT`; a nullable `detail jsonb` holding the edit operations, top-level choices, overrides,
`formRules: NOT_RUN`, lineage, `entriesOutsidePlacement` and the conflict id. The checks are re-created in one
changeset, whose rollback refuses once such revisions exist.

**The submission normalizer** (`SubmissionNormalizer.normalize`, on `develop`) turns a stored submission into values
keyed by row id and field id, typed by its form version. It never fails on data: it flags what it meets (normalization
flags) and counts them per submission. Search, view, history and the feed's publisher read through it; edits also need
the write direction, from field id and row id back to the stored place.

**The feed** ([submission feed](../submission-feed/README.md)) publishes each submission's current state.
Downstream wants the current state and no attribution (owner, 2026-11-25), so the feed carries no actor, no reason
and no command; history stays in the journal. A split's two submissions are published in one feed transaction. A
deleted submission, a removed row and a row split away all show `deleted = true`.

**This feature's tables** live in schema `submission_review`: conflicts with their phone copies, notes, access
grants and entry tracking. The analytics copy never reads it: Airbyte reads `analytics`, `pivot` and `public`
(owner, 2026-11-25).

## 11. Effects

- **Phones.** No route, DTO or response shape changes; what the server does with some copies changes, and the answer
  stays `updated`. New goldens per phone contract cover copies after each server-side action, copies refused after a
  move, copies without row ids, detection errors, and held and practice uploads (unaffected).
- **Downstream.** The load is small: about 800 changes a day, against the ETL's capacity of about 40,000 an hour.
- **Privacy.** Logs carry ids, versions, commands and counts only; problems never repeat a value or a reason.
- **v1.** The v1 admin delete retires as its own step (§12). `/objects` keeps its answer to old apps until they
  retire.

## 12. Build order and activation

There is no delivery deadline, and no search-only release: searching is of no use without acting on the results
(owner, 2026-11-25). Each step ends reviewed and rehearsed, and users get one coherent capability.

| Step | Contents | Proves |
| --- | --- | --- |
| 0 | The revision journal in production (6.5.6); the v2 foundation on `develop` | — |
| 1 | The submission normalizer (reading by row id and field id, typed values: on `develop`) and the write direction back to stored places | Agreement with the feed spike on every submission |
| 2 | Its own release, reviewed: the submission lock first on the upload paths; conflict detection shipped switched off, with the tagging of copies without row ids and the never-fail handling of copies refused after a move; the ETL's split re-attribution | Goldens unchanged with the switch off. With it on, on a clone: every submission re-sent unchanged, with and without row ids, gives 0 conflicts and 0 failures, and changed copies are kept aside. ETL test: split rows re-attributed |
| 3 | Access grants; search (both forms, the guard, the timeout, row semantics, entry tracking, `created`); view; history; the form dictionary; notes | Plans use the indexes; refusals for missing grants and other activities; the test of SQL against the normalizer; entry searches return split rows |
| 4 | Edits; delete and restore with lineage checks; the conflict view, keep, accept, accept and re-apply; validation; dry run; the journal changeset | I1, I2, I5, I8, I10; 428, 412, 422 and replays; integrity check 11 reads 0 |
| 5 | Move and split: destination rules, top-level choices, entry tracking, the two-submission transaction, lineage, accept's split rules; the randomized sequences | I2–I7 and I9 on the clone; rows and top-level values conserved |
| 6 | The scaled measurement: about 30,000 village-shaped submissions of about 70 rows, and about a million submissions without rows | p50 and p95, warm and cold, for every search shape the guard allows; whether the GIN is added |
| 7 | After activation: the v1 admin delete retired | Logs show no callers; goldens adjusted with intent |

**Activation in production,** each gate on the owner's go:

| Gate | Turns on | When |
| --- | --- | --- |
| First | Conflict detection | Before any change; users see nothing new |
| A | Search, view, history, notes; `setValue`, `setEntry`, `restoreRow`, move, split, delete, restore, conflict decisions | With the capability; split once the split re-attribution is in production |
| B | `clearValue`, `removeRow` | Server-wide, in one step: once downstream confirms in writing that no model reads tall and events, for any activity |
| Then | The v1 admin delete retired | Once `:delete` is on and the logs show its callers have moved |

Gate B is server-wide, never per activity (engineering, 2026-11-25): downstream re-points all its models to the feed,
and tall and events then retire. Switching it on per activity would need a per-activity setting that this plan does
not use; revisit only if downstream switches activity by activity.

**Later, by need:** marks as data; `addRow`; creating a submission through the API; area, form and team scopes; the
server-owned read model for search; the operations screen, with its browser login.

## 13. Example, end to end

Invented ids and values. Team T312 has assignment A7 in village V7 and assignment A9 in village V9. Submission S
(`…7f3a`) was sent for A7 with 64 rows; 23 of them are households of V9.

1. **Search.** `GET /api/v2/submissions?form=…a1&team=…t3&orgUnit=…v7` returns S at version 0.
2. **View.** `GET /api/v2/submissions/…7f3a` answers with `ETag: "0"`.
3. **Split.** The request of §4.2, with the 23 rows and the destination A9. The answer is `201 Created` with
   `Location: /api/v2/submissions/…9c2e`. S is at version 1 with 41 rows; the new submission is at version 0 with
   the 23 rows, split from S.
4. **History of S** shows "Split by ‹user› · 4 Dec 10:12 · 23 households of V9 recorded under V7 · 23 rows to
   …9c2e (V9)". In the journal this is S's revision 1, kind `UPDATE`, and the new submission's revision 0, kind
   `CREATE`, both with command `SPLIT`.
5. **The feed** publishes, in one feed transaction, S at version 1, its 23 row records now `deleted = true`, and the
   new submission at version 0 with the 23 rows; no command and no actor. Once downstream reads them, V9 no longer
   shows as uncovered.
6. **An edit.**
   ```http
   POST /api/v2/submissions/…7f3a:edit
   If-Match: "1"
   Idempotency-Key: 9b4e…
   {"reason": "population typed 70 for 7; household recorded twice",
    "operations": [{"op": "setValue", "row": "…k4", "field": "…p1", "value": 7},
                   {"op": "removeRow", "row": "…k9"}]}
   ```
   Once gate B is switched on, it makes revision 2, shown as "Edited by ‹user› · row …k4: population 70 → 7;
   row …k9 removed". Before that, the whole request is refused with 422 `operation-not-enabled` at
   `#/operations/1`.
7. **The phone re-sends S unchanged.** That is an unchanged re-send: nothing changes, and the phone is answered
   `updated`.
8. **The team leader edits S on the phone and re-sends it.** Its base version is 0, and server-side changes came
   after it, so a conflict `…c1` opens (`OPEN`); the phone is answered `updated`, and S shows
   `"hasOpenConflict": true`. A user with `submissions.change` views `GET /api/v2/submissions/…7f3a/conflicts/…c1`
   and sees the base version, the current state and the phone copy side by side. The copy still holds the 23 rows
   now in …9c2e, so a plain accept would answer 409 `row-live-elsewhere`. Seeing nothing in the copy to take, the
   user sends `POST …/conflicts/…c1:keep` with `If-Match` set to the conflict's `ETag` from that view, and a
   reason: the conflict is `KEPT`, and S stays at version 2.
9. **A refusal.** Clearing a mandatory field without `"overrideMandatory": true` answers 422 with
   `"code": "mandatory-field-empty"` and `errors[0].pointer = "#/operations/0"`. The value is never repeated.
10. **A handover.** When V7 is handed to team T415, `POST /api/v2/submissions/…7f3a:move` with
    `If-Match: "2"` and `{"reason": "…", "destination": {"assignment": "…a15"}}` makes one revision with command
    `MOVE`. T312's phone may still re-send S: unchanged, it changes nothing; changed, it is refused today (E4124),
    and is now kept aside as a conflict, since a move came after its base version.

## 14. Open

- **Switch names** are kebab-case, from the glossary, chosen with each build step.
- **The Arabic words** are to confirm with operations ([CONTEXT.md](CONTEXT.md)).
